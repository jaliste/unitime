/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2011, UniTime LLC, and individual contributors
 * as indicated by the @authors tag.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
*/
package org.unitime.timetable.onlinesectioning.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.constraint.LinkedSections;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.server.DayCode;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer.Lock;
import org.unitime.timetable.onlinesectioning.solver.multicriteria.MultiCriteriaBranchAndBoundSuggestions;

/**
 * @author Tomas Muller
 */
public class ComputeSuggestionsAction extends FindAssignmentAction {
	private static StudentSectioningMessages MSG = Localization.create(StudentSectioningMessages.class);
	private ClassAssignmentInterface.ClassAssignment iSelection;
	private double iValue = 0.0;
	private String iFilter = null;
	
	public ComputeSuggestionsAction(CourseRequestInterface request, Collection<ClassAssignmentInterface.ClassAssignment> currentAssignment, ClassAssignmentInterface.ClassAssignment selectedAssignment, String filter) throws SectioningException {
		super(request, currentAssignment);
		iSelection = selectedAssignment;
		iFilter = filter;
	}
	
	public ClassAssignmentInterface.ClassAssignment getSelection() { return iSelection; }
	
	public String getFilter() { return iFilter; }

	@Override
	public List<ClassAssignmentInterface> execute(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		long t0 = System.currentTimeMillis();
		StudentSectioningModel model = new StudentSectioningModel(server.getConfig());

		OnlineSectioningLog.Action.Builder action = helper.getAction();

		if (getRequest().getStudentId() != null)
			action.setStudent(
					OnlineSectioningLog.Entity.newBuilder()
					.setUniqueId(getRequest().getStudentId()));

		Student student = new Student(getRequest().getStudentId() == null ? -1l : getRequest().getStudentId());
		Set<Long> enrolled = null;

		Lock readLock = server.readLock();
		try {
			Student original = (getRequest().getStudentId() == null ? null : server.getStudent(getRequest().getStudentId()));
			if (original != null) {
				action.getStudentBuilder().setUniqueId(original.getId()).setExternalId(original.getExternalId());
				enrolled = new HashSet<Long>();
				for (Request r: original.getRequests())
					if (r.getInitialAssignment() != null && r.getInitialAssignment().isCourseRequest())
						for (Section s: r.getInitialAssignment().getSections())
							enrolled.add(s.getId());
				OnlineSectioningLog.Enrollment.Builder enrollment = OnlineSectioningLog.Enrollment.newBuilder();
				enrollment.setType(OnlineSectioningLog.Enrollment.EnrollmentType.STORED);
				for (Request oldRequest: original.getRequests()) {
					if (oldRequest.getInitialAssignment() != null && oldRequest.getInitialAssignment().isCourseRequest())
						for (Section section: oldRequest.getInitialAssignment().getSections())
							enrollment.addSection(OnlineSectioningHelper.toProto(section, oldRequest.getInitialAssignment()));
				}
				action.addEnrollment(enrollment);
			}
			Map<Long, Section> classTable = new HashMap<Long, Section>();
			Set<LinkedSections> linkedSections = new HashSet<LinkedSections>();
			for (CourseRequestInterface.Request c: getRequest().getCourses())
				addRequest(server, model, student, original, c, false, true, classTable, linkedSections);
			if (student.getRequests().isEmpty()) throw new SectioningException(MSG.exceptionNoCourse());
			for (CourseRequestInterface.Request c: getRequest().getAlternatives())
				addRequest(server, model, student, original, c, true, true, classTable, linkedSections);
			model.addStudent(student);
			model.setDistanceConflict(new DistanceConflict(null, model.getProperties()));
			model.setTimeOverlaps(new TimeOverlapsCounter(null, model.getProperties()));
			for (LinkedSections link: linkedSections) {
				List<Section> sections = new ArrayList<Section>();
				for (Offering offering: link.getOfferings())
					for (Subpart subpart: link.getSubparts(offering))
						for (Section section: link.getSections(subpart)) {
							Section x = classTable.get(section.getId());
							if (x != null) sections.add(x);
						}
				if (sections.size() > 2)
					model.addLinkedSections(sections);
			}
		} finally {
			readLock.release();
		}
		
		long t1 = System.currentTimeMillis();

		Hashtable<CourseRequest, Set<Section>> preferredSectionsForCourse = new Hashtable<CourseRequest, Set<Section>>();
		Hashtable<CourseRequest, Set<Section>> requiredSectionsForCourse = new Hashtable<CourseRequest, Set<Section>>();
		HashSet<FreeTimeRequest> requiredFreeTimes = new HashSet<FreeTimeRequest>();
        ArrayList<ClassAssignmentInterface> ret = new ArrayList<ClassAssignmentInterface>();
        ClassAssignmentInterface messages = new ClassAssignmentInterface();
        ret.add(messages);
        
		OnlineSectioningLog.Enrollment.Builder requested = OnlineSectioningLog.Enrollment.newBuilder();
		requested.setType(OnlineSectioningLog.Enrollment.EnrollmentType.PREVIOUS);
		for (ClassAssignmentInterface.ClassAssignment assignment: getAssignment())
			if (assignment != null)
				requested.addSection(OnlineSectioningHelper.toProto(assignment));
		action.addEnrollment(requested);

		Request selectedRequest = null;
		Section selectedSection = null;
		for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext();) {
			Request r = (Request)e.next();
			OnlineSectioningLog.Request.Builder rq = OnlineSectioningHelper.toProto(r); 
			if (r instanceof CourseRequest) {
				CourseRequest cr = (CourseRequest)r;
				if (!getSelection().isFreeTime() && cr.getCourse(getSelection().getCourseId()) != null) {
					selectedRequest = r;
					if (getSelection().getClassId() != null) {
						Section section = cr.getSection(getSelection().getClassId());
						if (section != null)
							selectedSection = section;
					}
				}
				HashSet<Section> preferredSections = new HashSet<Section>();
				HashSet<Section> requiredSections = new HashSet<Section>();
				a: for (ClassAssignmentInterface.ClassAssignment a: getAssignment()) {
					if (a != null && !a.isFreeTime() && cr.getCourse(a.getCourseId()) != null && a.getClassId() != null) {
						Section section = cr.getSection(a.getClassId());
						if (section == null || section.getLimit() == 0) {
							messages.addMessage((a.isSaved() ? "Enrolled class" : a.isPinned() ? "Required class" : "Previously selected class") + a.getSubject() + " " + a.getCourseNbr() + " " + a.getSubpart() + " " + a.getSection() + " is no longer available.");
							continue a;
						}
						if (a.isPinned() && !getSelection().equals(a)) 
							requiredSections.add(section);
						preferredSections.add(section);
						cr.getSelectedChoices().add(section.getChoice());
						rq.addSection(OnlineSectioningHelper.toProto(section, cr.getCourse(a.getCourseId())).setPreference(
								getSelection().equals(a) ? OnlineSectioningLog.Section.Preference.SELECTED :
								a.isPinned() ? OnlineSectioningLog.Section.Preference.REQUIRED : OnlineSectioningLog.Section.Preference.PREFERRED));
					}
				}
				preferredSectionsForCourse.put(cr, preferredSections);
				requiredSectionsForCourse.put(cr, requiredSections);
			} else {
				FreeTimeRequest ft = (FreeTimeRequest)r;
				if (getSelection().isFreeTime() && ft.getTime() != null &&
					ft.getTime().getStartSlot() == getSelection().getStart() &&
					ft.getTime().getLength() == getSelection().getLength() && 
					ft.getTime().getDayCode() == DayCode.toInt(DayCode.toDayCodes(getSelection().getDays()))) {
					selectedRequest = r;
					for (OnlineSectioningLog.Time.Builder ftb: rq.getFreeTimeBuilderList())
						ftb.setPreference(OnlineSectioningLog.Section.Preference.SELECTED);
				} else for (ClassAssignmentInterface.ClassAssignment a: getAssignment()) {
					if (a != null && a.isFreeTime() && a.isPinned() && ft.getTime() != null &&
						ft.getTime().getStartSlot() == a.getStart() &&
						ft.getTime().getLength() == a.getLength() && 
						ft.getTime().getDayCode() == DayCode.toInt(DayCode.toDayCodes(a.getDays()))) {
						requiredFreeTimes.add(ft);
						for (OnlineSectioningLog.Time.Builder ftb: rq.getFreeTimeBuilderList())
							ftb.setPreference(OnlineSectioningLog.Section.Preference.REQUIRED);
					}
				}
			}
			action.addRequest(rq);
		}
		
		long t2 = System.currentTimeMillis();
        
		SuggestionsBranchAndBound suggestionBaB = null;
		
		boolean avoidOverExpected = server.getAcademicSession().isSectioningEnabled();
		if (avoidOverExpected && helper.getUser() != null && helper.getUser().hasType() && helper.getUser().getType() != OnlineSectioningLog.Entity.EntityType.STUDENT)
			avoidOverExpected = false;
		if (avoidOverExpected && "true".equals(ApplicationProperties.getProperty("unitime.sectioning.allowOverExpected")))
			avoidOverExpected = false;
		if (server.getConfig().getPropertyBoolean("Suggestions.MultiCriteria", true)) {
			suggestionBaB = new MultiCriteriaBranchAndBoundSuggestions(
					model.getProperties(), student,
					requiredSectionsForCourse, requiredFreeTimes, preferredSectionsForCourse,
					selectedRequest, selectedSection,
					getFilter(), server.getAcademicSession().getDatePatternFirstDate(), avoidOverExpected);
		} else {
			suggestionBaB = new SuggestionsBranchAndBound(model.getProperties(), student,
					requiredSectionsForCourse, requiredFreeTimes, preferredSectionsForCourse,
					selectedRequest, selectedSection,
					getFilter(), server.getAcademicSession().getDatePatternFirstDate(), avoidOverExpected);
		}
		
        TreeSet<SuggestionsBranchAndBound.Suggestion> suggestions = suggestionBaB.computeSuggestions();
		iValue = (suggestions.isEmpty() ? 0.0 : - suggestions.first().getValue());
        
		long t3 = System.currentTimeMillis();
		helper.debug("  -- suggestion B&B took "+suggestionBaB.getTime()+"ms"+(suggestionBaB.isTimeoutReached()?", timeout reached":""));

		for (SuggestionsBranchAndBound.Suggestion suggestion : suggestions) {
        	ret.add(convert(server, suggestion.getEnrollments(), requiredSectionsForCourse, requiredFreeTimes, true, model.getDistanceConflict(), enrolled));
        	OnlineSectioningLog.Enrollment.Builder solution = OnlineSectioningLog.Enrollment.newBuilder();
        	solution.setType(OnlineSectioningLog.Enrollment.EnrollmentType.COMPUTED);
        	solution.setValue(- suggestion.getValue());
    		for (Enrollment e: suggestion.getEnrollments()) {
    			if (e != null && e.getAssignments() != null)
    				for (Assignment section: e.getAssignments())
    					solution.addSection(OnlineSectioningHelper.toProto(section, e));
    		}
			action.addEnrollment(solution);
        }
        
		long t4 = System.currentTimeMillis();
		helper.info("Sectioning took "+(t4-t0)+"ms (model "+(t1-t0)+"ms, solver init "+(t2-t1)+"ms, sectioning "+(t3-t2)+"ms, conversion "+(t4-t3)+"ms)");

		return ret;
	}

	@Override
	public String name() {
		return "suggestions";
	}
	
	public double value() {
		return iValue;
	}

}
