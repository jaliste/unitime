/*
 * UniTime 3.4 (University Timetabling Application)
 * Copyright (C) 2012, UniTime LLC, and individual contributors
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
package org.unitime.timetable.events;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.server.GwtRpcHelper;
import org.unitime.timetable.gwt.command.server.GwtRpcImplementation;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;
import org.unitime.timetable.gwt.shared.EventInterface.RelatedObjectInterface;
import org.unitime.timetable.gwt.shared.PageAccessException;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.Conflict;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.CourseAssignment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.Enrollment;
import org.unitime.timetable.gwt.shared.EventInterface.GetEnrollmentsFromRelatedObjectsRpcRequest;
import org.unitime.timetable.model.AcademicAreaClassification;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.CourseRequest;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Event;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.StudentGroup;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.Event.MultiMeeting;
import org.unitime.timetable.model.dao.EventDAO;

public class GetEnrollmentsFromRelatedObjectsBackend implements GwtRpcImplementation<GetEnrollmentsFromRelatedObjectsRpcRequest, GwtRpcResponseList<ClassAssignmentInterface.Enrollment>> {
	protected static GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MM/dd");
	
	@Override
	public GwtRpcResponseList<Enrollment> execute(GetEnrollmentsFromRelatedObjectsRpcRequest request, GwtRpcHelper helper) {
		checkAccess(helper);

		if (request.hasRelatedObjects()) {
			Map<Long, List<Meeting>> conflicts = null;
			HashSet<StudentClassEnrollment> enrollments = new HashSet<StudentClassEnrollment>();
			for (RelatedObjectInterface related: request.getRelatedObjects()) {
				enrollments.addAll(getStudentClassEnrollments(related));
				if (request.hasMeetings()) {
					conflicts = new HashMap<Long, List<Meeting>>();
					for (MeetingInterface meeting: request.getMeetings())
						computeConflicts(conflicts, meeting, related, request.getEventId());
				}
			}				

			return convert(enrollments, conflicts);
		} else {
			return null;
		}
	}
	
	
	public void checkAccess(GwtRpcHelper helper) throws PageAccessException {
		if (helper.getUser() == null) {
			throw new PageAccessException(helper.isHttpSessionNew() ? MESSAGES.authenticationExpired() : MESSAGES.authenticationRequired());
		}
		if (!Roles.ADMIN_ROLE.equals(helper.getUser().getRole()) && !Roles.EVENT_MGR_ROLE.equals(helper.getUser().getRole()))
			throw new PageAccessException(MESSAGES.authenticationInsufficient());
	}
	
	public static Collection<StudentClassEnrollment> getStudentClassEnrollments(RelatedObjectInterface relatedObject) {
        switch (relatedObject.getType()) {
        case Class : 
            return EventDAO.getInstance().getSession().createQuery(
            		"select distinct e from StudentClassEnrollment e, StudentClassEnrollment f where f.clazz.uniqueId = :classId" +
        			" and e.courseOffering.instructionalOffering = f.courseOffering.instructionalOffering and e.student = f.student")
                    .setLong("classId", relatedObject.getUniqueId())
                    .setCacheable(true)
                    .list();
        case Config : 
            return EventDAO.getInstance().getSession().createQuery(
            		"select distinct e from StudentClassEnrollment e, StudentClassEnrollment f where f.clazz.schedulingSubpart.instrOfferingConfig.uniqueId = :configId" +
            		" and e.courseOffering.instructionalOffering = f.courseOffering.instructionalOffering and e.student = f.student")
                    .setLong("configId", relatedObject.getUniqueId())
                    .setCacheable(true)
                    .list();
        case Course : 
            return EventDAO.getInstance().getSession().createQuery(
                    "select e from StudentClassEnrollment e where e.courseOffering.uniqueId = :courseId")
                    .setLong("courseId", relatedObject.getUniqueId())
                    .setCacheable(true)
                    .list();
        case Offering : 
            return EventDAO.getInstance().getSession().createQuery(
                    "select e from StudentClassEnrollment e where e.courseOffering.instructionalOffering.uniqueId = :offeringId")
                    .setLong("offeringId", relatedObject.getUniqueId())
                    .setCacheable(true)
                    .list();
        default : throw new RuntimeException("Unsupported related object type " + relatedObject.getType());
        }
    }
	
	
	private static String where(int type, int idx) {
		switch (type) {
		case ExamOwner.sOwnerTypeClass:
			return " and o" + idx + ".ownerType = " + type + " and o" + idx + ".ownerId = s" + idx + ".clazz.uniqueId";
		case ExamOwner.sOwnerTypeConfig:
			return " and o" + idx + ".ownerType = " + type + " and o" + idx + ".ownerId = s" + idx + ".clazz.schedulingSubpart.instrOfferingConfig.uniqueId";
		case ExamOwner.sOwnerTypeCourse:
			return " and o" + idx + ".ownerType = " + type + " and o" + idx + ".ownerId = s" + idx + ".courseOffering.uniqueId";
		case ExamOwner.sOwnerTypeOffering:
			return " and o" + idx + ".ownerType = " + type + " and o" + idx + ".ownerId = s" + idx + ".courseOffering.instructionalOffering.uniqueId";
		default:
			return "";
		}
	}
	
	public static void computeConflicts(Map<Long, List<Meeting>> conflicts, MeetingInterface meeting, RelatedObjectInterface relatedObject, Long eventId) {
        org.hibernate.Session hibSession = EventDAO.getInstance().getSession();
        
        switch (relatedObject.getType()) {
        case Class : 
        	// class events
        	for (Object[] o: (List<Object[]>)hibSession.createQuery(
        			"select s1.student.uniqueId, m1" +
        			" from StudentClassEnrollment s1, ClassEvent e1 inner join e1.meetings m1, StudentClassEnrollment s2" +
        			" where s2.clazz.uniqueId = :classId and e1.clazz = s1.clazz and s1.student = s2.student" +
        			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
        			.setDate("meetingDate", meeting.getMeetingDate())
        			.setInteger("startPeriod", meeting.getStartSlot())
        			.setInteger("stopPeriod", meeting.getEndSlot())
        			.setLong("classId", relatedObject.getUniqueId()).list()) {
	    		Long studentId = (Long)o[0];
	    		Meeting conflictingMeeting = (Meeting)o[1];
	    		List<Meeting> meetings = conflicts.get(studentId);
	    		if (meetings == null) {
	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
	    		}
	    		meetings.add(conflictingMeeting);
        	}
        	
        	// exam events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, ExamEvent e1 inner join e1.meetings m1 inner join e1.exam.owners o1, StudentClassEnrollment s2" +
            			" where s2.clazz.uniqueId = :classId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("classId", relatedObject.getUniqueId()).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	// course events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, CourseEvent e1 inner join e1.meetings m1 inner join e1.relatedCourses o1, StudentClassEnrollment s2" +
            			" where s2.clazz.uniqueId = :classId and e1.uniqueId != :eventId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("classId", relatedObject.getUniqueId())
            			.setLong("eventId", eventId == null ? -1 : eventId).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	break;
        	
        case Config : 
        	// class events
        	for (Object[] o: (List<Object[]>)hibSession.createQuery(
        			"select s1.student.uniqueId, m1" +
        			" from StudentClassEnrollment s1, ClassEvent e1 inner join e1.meetings m1, StudentClassEnrollment s2" +
        			" where s2.clazz.schedulingSubpart.instrOfferingConfig.uniqueId = :configId and e1.clazz = s1.clazz and s1.student = s2.student" +
        			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
        			.setDate("meetingDate", meeting.getMeetingDate())
        			.setInteger("startPeriod", meeting.getStartSlot())
        			.setInteger("stopPeriod", meeting.getEndSlot())
        			.setLong("configId", relatedObject.getUniqueId()).list()) {
	    		Long studentId = (Long)o[0];
	    		Meeting conflictingMeeting = (Meeting)o[1];
	    		List<Meeting> meetings = conflicts.get(studentId);
	    		if (meetings == null) {
	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
	    		}
	    		meetings.add(conflictingMeeting);
        	}
        	
        	// exam events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, ExamEvent e1 inner join e1.meetings m1 inner join e1.exam.owners o1, StudentClassEnrollment s2" +
            			" where s2.clazz.schedulingSubpart.instrOfferingConfig.uniqueId = :configId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("configId", relatedObject.getUniqueId()).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	// course events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, CourseEvent e1 inner join e1.meetings m1 inner join e1.relatedCourses o1, StudentClassEnrollment s2" +
            			" where s2.clazz.schedulingSubpart.instrOfferingConfig.uniqueId = :configId and e1.uniqueId != :eventId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("configId", relatedObject.getUniqueId())
            			.setLong("eventId", eventId == null ? -1 : eventId).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	break;

        case Course : 
        	// class events
        	for (Object[] o: (List<Object[]>)hibSession.createQuery(
        			"select s1.student.uniqueId, m1" +
        			" from StudentClassEnrollment s1, ClassEvent e1 inner join e1.meetings m1, StudentClassEnrollment s2" +
        			" where s2.courseOffering.uniqueId = :courseId and e1.clazz = s1.clazz and s1.student = s2.student" +
        			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
        			.setDate("meetingDate", meeting.getMeetingDate())
        			.setInteger("startPeriod", meeting.getStartSlot())
        			.setInteger("stopPeriod", meeting.getEndSlot())
        			.setLong("courseId", relatedObject.getUniqueId()).list()) {
	    		Long studentId = (Long)o[0];
	    		Meeting conflictingMeeting = (Meeting)o[1];
	    		List<Meeting> meetings = conflicts.get(studentId);
	    		if (meetings == null) {
	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
	    		}
	    		meetings.add(conflictingMeeting);
        	}
        	
        	// exam events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, ExamEvent e1 inner join e1.meetings m1 inner join e1.exam.owners o1, StudentClassEnrollment s2" +
            			" where s2.courseOffering.uniqueId = :courseId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("courseId", relatedObject.getUniqueId()).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	// course events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, CourseEvent e1 inner join e1.meetings m1 inner join e1.relatedCourses o1, StudentClassEnrollment s2" +
            			" where s2.courseOffering.uniqueId = :courseId and e1.uniqueId != :eventId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("courseId", relatedObject.getUniqueId())
            			.setLong("eventId", eventId == null ? -1 : eventId).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	break;

        case Offering : 
        	// class events
        	for (Object[] o: (List<Object[]>)hibSession.createQuery(
        			"select s1.student.uniqueId, m1" +
        			" from StudentClassEnrollment s1, ClassEvent e1 inner join e1.meetings m1, StudentClassEnrollment s2" +
        			" where s2.courseOffering.instructionalOffering.uniqueId = :offeringId and e1.clazz = s1.clazz and s1.student = s2.student" +
        			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
        			.setDate("meetingDate", meeting.getMeetingDate())
        			.setInteger("startPeriod", meeting.getStartSlot())
        			.setInteger("stopPeriod", meeting.getEndSlot())
        			.setLong("offeringId", relatedObject.getUniqueId()).list()) {
	    		Long studentId = (Long)o[0];
	    		Meeting conflictingMeeting = (Meeting)o[1];
	    		List<Meeting> meetings = conflicts.get(studentId);
	    		if (meetings == null) {
	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
	    		}
	    		meetings.add(conflictingMeeting);
        	}
        	
        	// exam events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, ExamEvent e1 inner join e1.meetings m1 inner join e1.exam.owners o1, StudentClassEnrollment s2" +
            			" where s2.courseOffering.instructionalOffering.uniqueId = :offeringId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("offeringId", relatedObject.getUniqueId()).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	// course events
        	for (int t1 = 0; t1 < ExamOwner.sOwnerTypes.length; t1++) {
            	for (Object[] o: (List<Object[]>)hibSession.createQuery(
            			"select s1.student.uniqueId, m1" +
            			" from StudentClassEnrollment s1, CourseEvent e1 inner join e1.meetings m1 inner join e1.relatedCourses o1, StudentClassEnrollment s2" +
            			" where s2.courseOffering.instructionalOffering.uniqueId = :offeringId and e1.uniqueId != :eventId and s1.student = s2.student" + where(t1, 1) +
            			" and m1.meetingDate = :meetingDate and m1.startPeriod < :stopPeriod and :startPeriod < m1.stopPeriod")
            			.setDate("meetingDate", meeting.getMeetingDate())
            			.setInteger("startPeriod", meeting.getStartSlot())
            			.setInteger("stopPeriod", meeting.getEndSlot())
            			.setLong("offeringId", relatedObject.getUniqueId())
            			.setLong("eventId", eventId == null ? -1 : eventId).list()) {
            		Long studentId = (Long)o[0];
    	    		Meeting conflictingMeeting = (Meeting)o[1];
    	    		List<Meeting> meetings = conflicts.get(studentId);
    	    		if (meetings == null) {
    	    			meetings = new ArrayList<Meeting>(); conflicts.put(studentId, meetings);
    	    		}
    	    		meetings.add(conflictingMeeting);
            	}
        	}
        	
        	break;
        }
	}

	
	public static GwtRpcResponseList<ClassAssignmentInterface.Enrollment> convert(Collection<StudentClassEnrollment> enrollments, Map<Long, List<Meeting>> conflicts) {
		GwtRpcResponseList<ClassAssignmentInterface.Enrollment> converted = new GwtRpcResponseList<ClassAssignmentInterface.Enrollment>();
		Map<String, String> approvedBy2name = new Hashtable<String, String>();
		Hashtable<Long, ClassAssignmentInterface.Enrollment> student2enrollment = new Hashtable<Long, ClassAssignmentInterface.Enrollment>();
    	for (StudentClassEnrollment enrollment: enrollments) {
    		ClassAssignmentInterface.Enrollment enrl = student2enrollment.get(enrollment.getStudent().getUniqueId());
    		if (enrl == null) {
    			ClassAssignmentInterface.Student st = new ClassAssignmentInterface.Student();
    			st.setId(enrollment.getStudent().getUniqueId());
    			st.setExternalId(enrollment.getStudent().getExternalUniqueId());
    			st.setName(enrollment.getStudent().getName(ApplicationProperties.getProperty("unitime.enrollment.student.name", DepartmentalInstructor.sNameFormatLastFirstMiddle)));
    			for (AcademicAreaClassification ac: enrollment.getStudent().getAcademicAreaClassifications()) {
    				st.addArea(ac.getAcademicArea().getAcademicAreaAbbreviation());
    				st.addClassification(ac.getAcademicClassification().getCode());
    			}
    			for (PosMajor m: enrollment.getStudent().getPosMajors()) {
    				st.addMajor(m.getCode());
    			}
    			for (StudentGroup g: enrollment.getStudent().getGroups()) {
    				st.addGroup(g.getGroupAbbreviation());
    			}
    			enrl = new ClassAssignmentInterface.Enrollment();
    			enrl.setStudent(st);
    			enrl.setEnrolledDate(enrollment.getTimestamp());
    			CourseAssignment c = new CourseAssignment();
    			c.setCourseId(enrollment.getCourseOffering().getUniqueId());
    			c.setSubject(enrollment.getCourseOffering().getSubjectAreaAbbv());
    			c.setCourseNbr(enrollment.getCourseOffering().getCourseNbr());
    			enrl.setCourse(c);
    			student2enrollment.put(enrollment.getStudent().getUniqueId(), enrl);
    			if (enrollment.getCourseRequest() != null) {
    				enrl.setPriority(1 + enrollment.getCourseRequest().getCourseDemand().getPriority());
    				if (enrollment.getCourseRequest().getCourseDemand().getCourseRequests().size() > 1) {
    					CourseRequest first = null;
    					for (CourseRequest r: enrollment.getCourseRequest().getCourseDemand().getCourseRequests()) {
    						if (first == null || r.getOrder().compareTo(first.getOrder()) < 0) first = r;
    					}
    					if (!first.equals(enrollment.getCourseRequest()))
    						enrl.setAlternative(first.getCourseOffering().getCourseName());
    				}
    				if (enrollment.getCourseRequest().getCourseDemand().isAlternative()) {
    					CourseDemand first = enrollment.getCourseRequest().getCourseDemand();
    					demands: for (CourseDemand cd: enrollment.getStudent().getCourseDemands()) {
    						if (!cd.isAlternative() && cd.getPriority().compareTo(first.getPriority()) < 0 && !cd.getCourseRequests().isEmpty()) {
    							for (CourseRequest cr: cd.getCourseRequests())
    								if (cr.getClassEnrollments().isEmpty()) continue demands;
    							first = cd;
    						}
    					}
    					CourseRequest alt = null;
    					for (CourseRequest r: first.getCourseRequests()) {
    						if (alt == null || r.getOrder().compareTo(alt.getOrder()) < 0) alt = r;
    					}
    					enrl.setAlternative(alt.getCourseOffering().getCourseName());
    				}
    				enrl.setRequestedDate(enrollment.getCourseRequest().getCourseDemand().getTimestamp());
    				enrl.setApprovedDate(enrollment.getApprovedDate());
    				if (enrollment.getApprovedBy() != null) {
    					String name = approvedBy2name.get(enrollment.getApprovedBy());
    					if (name == null) {
    						TimetableManager mgr = (TimetableManager)EventDAO.getInstance().getSession().createQuery(
    								"from TimetableManager where externalUniqueId = :externalId")
    								.setString("externalId", enrollment.getApprovedBy())
    								.setMaxResults(1).uniqueResult();
    						if (mgr != null) {
    							name = mgr.getName();
    						} else {
    							DepartmentalInstructor instr = (DepartmentalInstructor)EventDAO.getInstance().getSession().createQuery(
    									"from DepartmentalInstructor where externalUniqueId = :externalId and department.session.uniqueId = :sessionId")
    									.setString("externalId", enrollment.getApprovedBy())
    									.setLong("sessionId", enrollment.getStudent().getSession().getUniqueId())
    									.setMaxResults(1).uniqueResult();
    							if (instr != null)
    								name = instr.nameLastNameFirst();
    						}
    						if (name != null)
    							approvedBy2name.put(enrollment.getApprovedBy(), name);
    					}
    					enrl.setApprovedBy(name == null ? enrollment.getApprovedBy() : name);
    				}
    			} else {
    				enrl.setPriority(-1);
    			}
    			
				List<Meeting> conf = (conflicts == null ? null : conflicts.get(enrollment.getStudent().getUniqueId()));
				if (conf != null) {
					Map<Event, TreeSet<Meeting>> events = new HashMap<Event, TreeSet<Meeting>>();
					for (Meeting m: conf) {
						TreeSet<Meeting> ms = events.get(m.getEvent());
						if (ms == null) { ms = new TreeSet<Meeting>(); events.put(m.getEvent(), ms); }
						ms.add(m);
					}
					for (Event confEvent: new TreeSet<Event>(events.keySet())) {
						Conflict conflict = new Conflict();
						conflict.setName(confEvent.getEventName());
						conflict.setType(confEvent.getEventTypeAbbv());
						String lastDate = null, lastTime = null, lastRoom = null;
						for (MultiMeeting mm: Event.getMultiMeetings(events.get(confEvent))) {
							String date = sDateFormat.format(mm.getMeetings().first().getMeetingDate()) +
								(mm.getMeetings().size() == 1 ? "" : " - " + sDateFormat.format(mm.getMeetings().last().getMeetingDate()));
							if (lastDate == null) {
								conflict.setDate(date);
							} else if (lastDate.equals(date)) {
								conflict.setDate(conflict.getDate() + "<br>");
							} else {
								conflict.setDate(conflict.getDate() + "<br>" + date);
							}
							lastDate = date;
							
							String time = mm.getDays() + " " + (mm.getMeetings().first().isAllDay() ? "All Day" : mm.getMeetings().first().startTime() + " - " + mm.getMeetings().first().stopTime());
							if (lastTime == null) {
								conflict.setTime(time);
							} else if (lastTime.equals(time)) {
								conflict.setTime(conflict.getTime() + "<br>");
							} else {
								conflict.setTime(conflict.getTime() + "<br>" + time);
							}
							lastTime = time;
							
							String room = (mm.getMeetings().first().getLocation() == null ? "" : mm.getMeetings().first().getLocation().getLabel());
							if (lastRoom == null) {
								conflict.setRoom(room);
							} else if (lastRoom.equals(room)) {
								conflict.setRoom(conflict.getRoom() + "<br>");
							} else {
								conflict.setRoom(conflict.getRoom() + "<br>" + room);
							}
							lastRoom = room;
						}
						enrl.addConflict(conflict);
					}
				}
				
    			converted.add(enrl);
    		}
    		ClassAssignmentInterface.ClassAssignment c = enrl.getCourse().addClassAssignment();
    		c.setClassId(enrollment.getClazz().getUniqueId());
    		c.setSection(enrollment.getClazz().getClassSuffix(enrollment.getCourseOffering()));
    		if (c.getSection() == null)
    			c.setSection(enrollment.getClazz().getSectionNumberString());
    		c.setClassNumber(enrollment.getClazz().getSectionNumberString());
    		c.setSubpart(enrollment.getClazz().getSchedulingSubpart().getItypeDesc());
    	}
		return converted;
	}
}
