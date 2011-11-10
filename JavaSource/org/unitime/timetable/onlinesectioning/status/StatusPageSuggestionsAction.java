/*
 * UniTime 3.3 (University Timetabling Application)
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
package org.unitime.timetable.onlinesectioning.status;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

import org.unitime.commons.User;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.server.DayCode;
import org.unitime.timetable.gwt.server.LookupServlet;
import org.unitime.timetable.gwt.server.Query.TermMatcher;
import org.unitime.timetable.gwt.shared.PersonInterface;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.model.AcademicArea;
import org.unitime.timetable.model.AcademicClassification;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.OfferingConsentType;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.StudentGroup;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.AcademicAreaDAO;
import org.unitime.timetable.model.dao.AcademicClassificationDAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.OfferingConsentTypeDAO;
import org.unitime.timetable.model.dao.StudentGroupDAO;
import org.unitime.timetable.model.dao.SubjectAreaDAO;
import org.unitime.timetable.model.dao.TimetableManagerDAO;
import org.unitime.timetable.onlinesectioning.CourseInfo;
import org.unitime.timetable.onlinesectioning.OnlineSectioningAction;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;

public class StatusPageSuggestionsAction implements OnlineSectioningAction<List<String[]>> {
	private static StudentSectioningMessages MSG = Localization.create(StudentSectioningMessages.class);
	private static StudentSectioningConstants CONSTANTS = Localization.create(StudentSectioningConstants.class);
	private String iQuery;
	private int iLimit;
	private User iUser;
	
	public StatusPageSuggestionsAction(User user, String query, int limit) {
		iUser = user;
		iQuery = (query == null ? "" : query);
		iLimit = limit;
	}

	@Override
	public List<String[]> execute(OnlineSectioningServer server, OnlineSectioningHelper helper) {
		try {
			helper.beginTransaction();

			Long sessionId = server.getAcademicSession().getUniqueId();

			List<String[]> ret = new ArrayList<String[]>();
			Matcher m = Pattern.compile("^(.*\\W?subject:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				for (SubjectArea subject: (List<SubjectArea>)SubjectAreaDAO.getInstance().getSession().createQuery(
						"select a from SubjectArea a where" +
						" (lower(a.subjectAreaAbbreviation) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(a.shortTitle) like '%' || :q || '%' or lower(a.longTitle) like '%' || :q || '%'") + ")" +
						" and a.session.uniqueId = :sessionId order by a.subjectAreaAbbreviation"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", server.getAcademicSession().getUniqueId()).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (subject.getSubjectAreaAbbreviation().indexOf(' ') >= 0 ? "\"" + subject.getSubjectAreaAbbreviation() + "\"" : subject.getSubjectAreaAbbreviation()),
							subject.getSubjectAreaAbbreviation() + " - " + (subject.getLongTitle() == null ? subject.getShortTitle() : subject.getLongTitle())
					});
				}
			}
			m = Pattern.compile("^(.*\\W?department:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				for (Department dept: (List<Department>)DepartmentDAO.getInstance().getSession().createQuery(
						"select a from Department a where" +
						" (lower(a.abbreviation) like :q || '%' or lower(a.deptCode) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(name) like '%' || :q || '%'") + ")" +
						" and a.session.uniqueId = :sessionId order by a.deptCode"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (dept.getDeptCode().indexOf(' ') >= 0 ? "\"" + dept.getDeptCode() + "\"" : dept.getDeptCode()),
							dept.getDeptCode() + " - " + dept.getName()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?area:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				for (AcademicArea area: (List<AcademicArea>)AcademicAreaDAO.getInstance().getSession().createQuery(
						"select a from AcademicArea a where " +
						" (lower(a.academicAreaAbbreviation) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(a.shortTitle) like '%' || :q || '%' or lower(a.longTitle) like '%' || :q || '%'") + ")" +
						" and a.session.uniqueId = :sessionId order by a.academicAreaAbbreviation"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (area.getAcademicAreaAbbreviation().indexOf(' ') >= 0 ? "\"" + area.getAcademicAreaAbbreviation() + "\"" : area.getAcademicAreaAbbreviation()),
							area.getAcademicAreaAbbreviation() + " - " + (area.getLongTitle() == null ? area.getShortTitle() : area.getLongTitle())
					});
				}
			}
			m = Pattern.compile("^(.*\\W?classification:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				for (AcademicClassification clasf: (List<AcademicClassification>)AcademicClassificationDAO.getInstance().getSession().createQuery(
						"select a from AcademicClassification a where " +
						" (lower(a.code) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(a.name) like '%' || :q || '%'") + ")" +
						" and a.session.uniqueId = :sessionId order by a.code"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (clasf.getCode().indexOf(' ') >= 0 ? "\"" + clasf.getCode() + "\"" : clasf.getCode()),
							clasf.getCode() + " - " + clasf.getName()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?clasf:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				for (AcademicClassification clasf: (List<AcademicClassification>)AcademicClassificationDAO.getInstance().getSession().createQuery(
						"select a from AcademicClassification a where " +
						" (lower(a.code) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(a.name) like '%' || :q || '%'") + ")" +
						" and a.session.uniqueId = :sessionId order by a.code"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (clasf.getCode().indexOf(' ') >= 0 ? "\"" + clasf.getCode() + "\"" : clasf.getCode()),
							clasf.getCode() + " - " + clasf.getName()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?major:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				String area = null;
				Matcher x = Pattern.compile("area:[ ]?\"([^\\\"]*)\"|area:[ ]?(\\w*)").matcher(iQuery);
				if (x.find()) area = (x.group(1) == null ? x.group(2) : x.group(1));
				for (PosMajor major: (List<PosMajor>)AcademicClassificationDAO.getInstance().getSession().createQuery(
						"select distinct a from PosMajor a " + (area == null ? "" : "inner join a.academicAreas x ") + "where " +
						" (lower(a.code) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(a.name) like '%' || :q || '%'") + ")" +
						(area == null ? "" : " and lower(x.academicAreaAbbreviation) = '" + area.toLowerCase() + "'") +
						" and a.session.uniqueId = :sessionId order by a.code"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (major.getCode().indexOf(' ') >= 0 ? "\"" + major.getCode() + "\"" : major.getCode()),
							major.getCode() + " - " + major.getName()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?course:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				String subject = null;
				Matcher x = Pattern.compile("subject:[ ]?\"([^\\\"]*)\"|subject:[ ]?(\\w*)").matcher(iQuery);
				if (x.find()) subject = (x.group(1) == null ? x.group(2) : x.group(1));
				for (CourseOffering course: (List<CourseOffering>)CourseOfferingDAO.getInstance().getSession().createQuery(
						"select c from CourseOffering c where " +
						" (lower(c.courseNbr) like :q || '%' or lower(c.subjectArea.subjectAreaAbbreviation) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(c.title) like '%' || :q || '%'") + ")" +
						(subject == null ? "" : " and lower(c.subjectArea.subjectAreaAbbreviation) = '" + subject.toLowerCase() + "'") +
						" and c.subjectArea.session.uniqueId = :sessionId order by c.subjectArea.subjectAreaAbbreviation, c.courseNbr"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + "\"" + course.getCourseName() + "\"",
							course.getCourseNameWithTitle()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?number:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				String subject = null;
				Matcher x = Pattern.compile("subject:[ ]?\"([^\\\"]*)\"|subject:[ ]?(\\w*)").matcher(iQuery);
				if (x.find()) subject = (x.group(1) == null ? x.group(2) : x.group(1));
				for (CourseOffering course: (List<CourseOffering>)CourseOfferingDAO.getInstance().getSession().createQuery(
						"select c from CourseOffering c where " +
						" (lower(c.courseNbr) like :q || '%' or lower(c.subjectArea.subjectAreaAbbreviation) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(c.title) like '%' || :q || '%'") + ")" +
						(subject == null ? "" : " and lower(c.subjectArea.subjectAreaAbbreviation) = '" + subject.toLowerCase() + "'") +
						" and c.subjectArea.session.uniqueId = :sessionId order by c.subjectArea.subjectAreaAbbreviation, c.courseNbr"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (course.getCourseNbr().indexOf(' ') >= 0 ? "\"" + course.getCourseNbr() + "\"" : course.getCourseNbr()) +
							(subject == null ? " subject: " + (course.getSubjectArea().getSubjectAreaAbbreviation().indexOf(' ') >= 0 ? "\"" + course.getSubjectArea().getSubjectAreaAbbreviation() + "\"" : course.getSubjectArea().getSubjectAreaAbbreviation()) : ""),
							course.getCourseNameWithTitle()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?group:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				for (StudentGroup group: (List<StudentGroup>)StudentGroupDAO.getInstance().getSession().createQuery(
						"select a from StudentGroup a where " +
						" (lower(a.groupAbbreviation) like :q || '%'" + (m.group(2).length() <= 2 ? "" : " or lower(a.groupName) like '%' || :q || '%'") + ")" +
						" and a.session.uniqueId = :sessionId order by a.groupAbbreviation"
						).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
					ret.add(new String[] {
							m.group(1) + (group.getGroupAbbreviation().indexOf(' ') >= 0 ? "\"" + group.getGroupAbbreviation() + "\"" : group.getGroupAbbreviation()),
							group.getGroupAbbreviation() + " - " + group.getGroupName()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?student:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches() && m.group(2).length() > 0) {
				for (PersonInterface person: new LookupServlet().lookupPeople(m.group(2), "mustHaveExternalId,source=students,session=" + sessionId + ",maxResults=" + iLimit)) {
					ret.add(new String[] {
							m.group(1) + (person.getId().indexOf(' ') >= 0 ? "\"" + person.getId() + "\"" : person.getId()),
							person.getName()
					});
				}
			}
			m = Pattern.compile("^(.*\\W?assigned:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("true".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "true",
							"true - Assigned enrollments"
					});
				if ("false".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "false",
							"false - Wait-listed course requests"
					});
			}
			m = Pattern.compile("^(.*\\W?scheduled:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("true".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "true",
							"true - Assigned enrollments"
					});
				if ("false".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "false",
							"false - Wait-listed course requests"
					});
			}
			m = Pattern.compile("^(.*\\W?waitlist:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("true".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "true",
							"true - Wait-listed course requests"
					});
				if ("false".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "false",
							"false - Assigned enrollments"
					});
			}
			m = Pattern.compile("^(.*\\W?waitlisted:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("true".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "true",
							"true - Wait-listed course requests"
					});
				if ("false".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "false",
							"false - Assigned enrollments"
					});
			}
			m = Pattern.compile("^(.*\\W?reservation:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("true".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "true",
							"true - Enrollments with a reservation"
					});
				if ("false".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "false",
							"false - Enrollments without a reservation"
					});
			}
			m = Pattern.compile("^(.*\\W?reserved:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("true".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "true",
							"true - Enrollments with a reservation"
					});
				if ("false".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "false",
							"false - Enrollments without a reservation"
					});
			}
			m = Pattern.compile("^(.*\\W?consent:[ ]?)(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("none".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "none",
							"none - Courses with no consent"
					});
				if ("required".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "required",
							"required - Courses requiring a consent"
					});
				for (OfferingConsentType consent: OfferingConsentTypeDAO.getInstance().findAll())
					if (consent.getAbbv().toLowerCase().startsWith(m.group(2).toLowerCase()))
						ret.add(new String[] {
								m.group(1) + (consent.getAbbv().indexOf(' ') >= 0 ? "\"" + consent.getAbbv() + "\"" : consent.getAbbv()).toLowerCase(),
								consent.getAbbv().toLowerCase() + " - " + consent.getLabel() + " required"
						});
				if ("waiting".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "waiting",
							"waiting - Enrollments waiting for a consent"
					});
				if ("todo".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "todo",
							"todo - Enrollments waiting for my consent"
					});
				if ("approved".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							m.group(1) + "approved",
							"approved - Enrollments with an approved consent"
					});
				if (m.group(2).length() > 0) {
					for (TimetableManager manager: (List<TimetableManager>)TimetableManagerDAO.getInstance().getSession().createQuery(
							"select distinct m from TimetableManager m inner join m.managerRoles r inner join m.departments d where " +
							" (lower(m.externalUniqueId) like :q || '%' or lower(m.emailAddress) like :q || '%' or lower(m.lastName) || ' ' || lower(m.firstName) like :q || '%')" +
							" and r.role.reference in ('Administrator', 'Dept Sched Mgr') and d.session.uniqueId = :sessionId order by m.lastName, m.firstName, m.middleName"
							).setString("q", m.group(2).toLowerCase()).setLong("sessionId", sessionId).setMaxResults(iLimit).list()) {
						ret.add(new String[] {
								m.group(1) + (manager.getExternalUniqueId().indexOf(' ') >= 0 ? "\"" + manager.getExternalUniqueId() + "\"" : manager.getExternalUniqueId()),
								manager.getLastName().toLowerCase() + " - Enrollments approved by " + manager.getName()
						});
					}
				} else {
					ret.add(new String[] {
							m.group(1) + iUser.getId(),
							(iUser.getName().contains(",") ? iUser.getName().substring(0, iUser.getName().indexOf(',')).toLowerCase() : iUser.getName().toLowerCase()) + " - " + "Enrollments approved by " + iUser.getName()
					});
				}
			}

			if (ret.isEmpty() && !iQuery.isEmpty()) {
				for (CourseInfo c: server.findCourses(iQuery, iLimit)) {
					ret.add(new String[] {
							c.getSubjectArea() + " " + c.getCourseNbr(),
							c.getSubjectArea() + " " + c.getCourseNbr() + (c.getTitle() == null ? "" : " - " + c.getTitle())
					});
				}
			}
			
			m = Pattern.compile("^(.*[^: ][ ]+)?(\\w*)$", Pattern.CASE_INSENSITIVE).matcher(iQuery);
			if (m.matches()) {
				if ("area".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "area:",
							"area: Academic Area"
					});
				if ("classification".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "classification:",
							"classification: Academic Classification"
					});
				if ("consent".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "consent:",
							"consent: Courses with consent"
					});
				if ("course".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "course:",
							"course: Course Offering"
					});
				if ("department".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "department:",
							"department: Department"
					});
				if ("group".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "group:",
							"group: Student Group"
					});
				if ("major".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "major:",
							"major: Major"
					});
				if ("reservation".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "reservation:",
							"reservation: Enrollments with a reservation"
					});
				if ("student".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "student:",
							"student: Student"
					});
				if ("subject".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "subject:",
							"subject: Subject Area"
					});
				if ("waitlist".startsWith(m.group(2).toLowerCase()))
					ret.add(new String[] {
							(m.group(1) == null ? "" : m.group(1)) + "waitlist:",
							"waitlist: Wait-Listed Course Requests"
					});
			}
			
			helper.commitTransaction();
			return ret;
		} catch (Exception e) {
			helper.rollbackTransaction();
			if (e instanceof SectioningException)
				throw (SectioningException)e;
			throw new SectioningException(MSG.exceptionUnknown(e.getMessage()), e);
		}
	}
	
	public static class CourseInfoMatcher implements TermMatcher {
		private CourseInfo iInfo;
		private OnlineSectioningHelper iHelper;
		private boolean iConsentToDoCourse;
		
		public CourseInfoMatcher(OnlineSectioningHelper helper, CourseInfo course, boolean isConsentToDoCourse) {
			iHelper = helper;
			iInfo = course;
			iConsentToDoCourse = isConsentToDoCourse;
		}
		
		public OnlineSectioningHelper helper() { return iHelper; }

		public CourseInfo info() { return iInfo; }
		
		public boolean isConsentToDoCourse() { return iConsentToDoCourse; }
		
		@Override
		public boolean match(String attr, String term) {
			if (term.isEmpty()) return true;
			if (attr == null || "name".equals(attr) || "course".equals(attr)) {
				return info().getSubjectArea().equalsIgnoreCase(term) || info().getCourseNbr().equalsIgnoreCase(term) || (info().getSubjectArea() + " " + info().getCourseNbr()).equalsIgnoreCase(term);
			}
			if ((attr == null && term.length() > 2) || "title".equals(attr)) {
				return info().getTitle().toLowerCase().contains(term.toLowerCase());
			}
			if (attr == null || "subject".equals(attr)) {
				return info().getSubjectArea().equalsIgnoreCase(term);
			}
			if (attr == null || "number".equals(attr)) {
				return info().getCourseNbr().equalsIgnoreCase(term);
			}
			if ("department".equals(attr)) {
				return info().getDepartment().equalsIgnoreCase(term);
				
			}
			if ("consent".equals(attr)) {
				if ("none".equalsIgnoreCase(term))
					return info().getConsent() == null;
				else if ("todo".equalsIgnoreCase(term))
					return isConsentToDoCourse();
				else
					return info().getConsent() != null;
			}
			if ("registered".equals(attr)) {
				if ("true".equalsIgnoreCase(term) || "1".equalsIgnoreCase(term))
					return true;
				else
					return false;
			}
			return attr != null; // pass unknown attributes lower
		}
	}
	
	public static class CourseRequestMatcher extends CourseInfoMatcher {
		private CourseRequest iRequest;
		private Date iFirstDate;
		
		public CourseRequestMatcher(OnlineSectioningHelper helper, OnlineSectioningServer server, CourseInfo info, CourseRequest request, boolean isConsentToDoCourse) {
			super(helper, info, isConsentToDoCourse);
			iFirstDate = server.getAcademicSession().getDatePatternFirstDate();
			iRequest = request;
		}
		
		public CourseRequest request() { return iRequest; }
		public Enrollment enrollment() { return iRequest.getAssignment(); }
		public Student student() { return iRequest.getStudent(); }
		public Course course() {
			if (enrollment() != null) return enrollment().getCourse();
			for (Course course: request().getCourses())
				if (course.getId() == info().getUniqueId()) return course;
			return request().getCourses().get(0);
		}

		@Override
		public boolean match(String attr, String term) {
			if (attr == null || "name".equals(attr) || "title".equals(attr) || "subject".equals(attr) || "number".equals(attr) || "course".equals(attr) || "department".equals(attr) || "registered".equals(attr))
				return super.match(attr, term);
			
			if ("area".equals(attr)) {
				for (AcademicAreaCode ac: student().getAcademicAreaClasiffications())
					if (eq(ac.getArea(), term)) return true;
			}
			
			if ("clasf".equals(attr) || "classification".equals(attr)) {
				for (AcademicAreaCode ac: student().getAcademicAreaClasiffications())
					if (eq(ac.getCode(), term)) return true;
			}
			
			if ("major".equals(attr)) {
				for (AcademicAreaCode ac: student().getMajors())
					if (eq(ac.getCode(), term)) return true;
			}
			
			if ("group".equals(attr)) {
				for (AcademicAreaCode ac: student().getMinors())
					if (eq(ac.getCode(), term)) return true;
			}

			
			if ("student".equals(attr)) {
				return has(student().getName(), term) || eq(student().getExternalId(), term) || eq(student().getName(), term);
			}
			
			if ("assigned".equals(attr) || "scheduled".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return enrollment() != null;
				else
					return enrollment() == null;
			}
			
			if ("waitlisted".equals(attr) || "waitlist".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return enrollment() == null;
				else
					return enrollment() != null;
			}
			
			if ("reservation".equals(attr) || "reserved".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return enrollment() != null && enrollment().getReservation() != null;
				else
					return enrollment() != null && enrollment().getReservation() == null;
			}
			
			if ("consent".equals(attr)) {
				if (eq("none", term)) {
					return info().getConsent() == null;
				} else if (eq("required", term)) {
					return info().getConsent() != null;
				} else if (eq("approved", term)) {
					return info().getConsent() != null && enrollment() != null && enrollment().getApproval() != null;
				} else if (eq("waiting", term)) {
					return info().getConsent() != null && enrollment() != null && enrollment().getApproval() == null;
				} else if (eq("todo", term)) {
					return isConsentToDoCourse() && enrollment() != null && enrollment().getApproval() == null;
				} else {
					return info().getConsent() != null && ((enrollment() != null && enrollment().getApproval() != null && (has(enrollment().getApproval().split(":")[2], term) || eq(enrollment().getApproval().split(":")[1], term))) || eq(info().getConsentAbbv(), term));
				}
			}
			
			if (enrollment() != null) {
				
				for (Section section: enrollment().getSections()) {
					if (attr == null || attr.equals("crn") || attr.equals("id") || attr.equals("externalId") || attr.equals("exid") || attr.equals("name")) {
						if (section.getName(info().getUniqueId()) != null && section.getName(info().getUniqueId()).toLowerCase().startsWith(term.toLowerCase()))
							return true;
					}
					if (attr == null || attr.equals("day")) {
						if (section.getTime() == null && term.equalsIgnoreCase("none")) return true;
						if (section.getTime() != null) {
							int day = parseDay(term);
							if (day > 0 && (section.getTime().getDayCode() & day) == day) return true;
						}
					}
					if (attr == null || attr.equals("time")) {
						if (section.getTime() == null && term.equalsIgnoreCase("none")) return true;
						if (section.getTime() != null) {
							int start = parseStart(term);
							if (start >= 0 && section.getTime().getStartSlot() == start) return true;
						}
					}
					if (attr != null && attr.equals("before")) {
						if (section.getTime() != null) {
							int end = parseStart(term);
							if (end >= 0 && section.getTime().getStartSlot() + section.getTime().getLength() - section.getTime().getBreakTime() / 5 <= end) return true;
						}
					}
					if (attr != null && attr.equals("after")) {
						if (section.getTime() != null) {
							int start = parseStart(term);
							if (start >= 0 && section.getTime().getStartSlot() >= start) return true;
						}
					}
					if (attr == null || attr.equals("date")) {
						if (section.getTime() == null && term.equalsIgnoreCase("none")) return true;
						if (section.getTime() != null && !section.getTime().getWeekCode().isEmpty()) {
							SimpleDateFormat df = new SimpleDateFormat(CONSTANTS.patternDateFormat());
					    	Calendar cal = Calendar.getInstance(Locale.US); cal.setLenient(true);
					    	cal.setTime(iFirstDate);
					    	for (int i = 0; i < section.getTime().getWeekCode().size(); i++) {
					    		if (section.getTime().getWeekCode().get(i)) {
					    			DayCode day = null;
					    			switch (cal.get(Calendar.DAY_OF_WEEK)) {
					    			case Calendar.MONDAY:
					    				day = DayCode.MON; break;
					    			case Calendar.TUESDAY:
					    				day = DayCode.TUE; break;
					    			case Calendar.WEDNESDAY:
					    				day = DayCode.WED; break;
					    			case Calendar.THURSDAY:
					    				day = DayCode.THU; break;
					    			case Calendar.FRIDAY:
					    				day = DayCode.FRI; break;
					    			case Calendar.SATURDAY:
					    				day = DayCode.SAT; break;
					    			case Calendar.SUNDAY:
					    				day = DayCode.SUN; break;
					    			}
					    			if ((section.getTime().getDayCode() & day.getCode()) == day.getCode()) {
						    			int d = cal.get(Calendar.DAY_OF_MONTH);
						    			int m = cal.get(Calendar.MONTH) + 1;
						    			if (df.format(cal.getTime()).equalsIgnoreCase(term) || eq(d + "." + m + ".",term) || eq(m + "/" + d, term)) return true;
					    			}
					    		}
					    		cal.add(Calendar.DAY_OF_YEAR, 1);
					    	}
						}
					}
					if (attr == null || attr.equals("room")) {
						if ((section.getRooms() == null || section.getRooms().isEmpty()) && term.equalsIgnoreCase("none")) return true;
						if (section.getRooms() != null) {
							for (RoomLocation r: section.getRooms()) {
								if (has(r.getName(), term)) return true;
							}
						}
					}
					if (attr == null || attr.equals("instr") || attr.equals("instructor")) {
						if (attr != null && (section.getChoice().getInstructorNames() == null || section.getChoice().getInstructorNames().isEmpty()) && term.equalsIgnoreCase("none")) return true;
						for (String instructor: section.getChoice().getInstructorNames().split(":")) {
							String[] nameEmail = instructor.split("\\|");
							if (has(nameEmail[0], term)) return true;
							if (nameEmail.length == 2) {
								String email = nameEmail[1];
								if (email.indexOf('@') >= 0) email = email.substring(0, email.indexOf('@'));
								if (eq(email, term)) return true;
							}
						}
					}
					if (attr != null && section.getTime() != null) {
						int start = parseStart(attr + ":" + term);
						if (start >= 0 && section.getTime().getStartSlot() == start) return true;
					}					
				}
			}
			
			return false;
		}

		private boolean eq(String name, String term) {
			if (name == null) return false;
			return name.equalsIgnoreCase(term);
		}

		private boolean has(String name, String term) {
			if (name == null) return false;
			if (eq(name, term)) return true;
			for (String t: name.split(" |,"))
				if (t.equalsIgnoreCase(term)) return true;
			return false;
		}
		
		private int parseDay(String token) {
			int days = 0;
			boolean found = false;
			do {
				found = false;
				for (int i=0; i<CONSTANTS.longDays().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.longDays()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.longDays()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.days().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.days()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.days()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.days().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.days()[i].substring(0,2).toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(2);
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.shortDays().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.shortDays()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.shortDays()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.freeTimeShortDays().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.freeTimeShortDays()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.freeTimeShortDays()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
			} while (found);
			return (token.isEmpty() ? days : 0);
		}
		
		private int parseStart(String token) {
			int startHour = 0, startMin = 0;
			String number = "";
			while (!token.isEmpty() && token.charAt(0) >= '0' && token.charAt(0) <= '9') { number += token.substring(0, 1); token = token.substring(1); }
			if (number.isEmpty()) return -1;
			if (number.length() > 2) {
				startHour = Integer.parseInt(number) / 100;
				startMin = Integer.parseInt(number) % 100;
			} else {
				startHour = Integer.parseInt(number);
			}
			while (token.startsWith(" ")) token = token.substring(1);
			if (token.startsWith(":")) {
				token = token.substring(1);
				while (token.startsWith(" ")) token = token.substring(1);
				number = "";
				while (!token.isEmpty() && token.charAt(0) >= '0' && token.charAt(0) <= '9') { number += token.substring(0, 1); token = token.substring(1); }
				if (number.isEmpty()) return -1;
				startMin = Integer.parseInt(number);
			}
			while (token.startsWith(" ")) token = token.substring(1);
			boolean hasAmOrPm = false;
			if (token.toLowerCase().startsWith("am")) { token = token.substring(2); hasAmOrPm = true; }
			if (token.toLowerCase().startsWith("a")) { token = token.substring(1); hasAmOrPm = true; }
			if (token.toLowerCase().startsWith("pm")) { token = token.substring(2); hasAmOrPm = true; if (startHour<12) startHour += 12; }
			if (token.toLowerCase().startsWith("p")) { token = token.substring(1); hasAmOrPm = true; if (startHour<12) startHour += 12; }
			if (startHour < 7 && !hasAmOrPm) startHour += 12;
			if (startMin % 5 != 0) startMin = 5 * ((startMin + 2)/ 5);
			if (startHour == 7 && startMin == 0 && !hasAmOrPm) startHour += 12;
			return (60 * startHour + startMin) / 5;
		}
	}
	
	public static class StudentMatcher implements TermMatcher {
		private Student iStudent;
		
		public StudentMatcher(Student student) {
			iStudent = student;
		}

		public Student student() { return iStudent; }
		
		@Override
		public boolean match(String attr, String term) {
			if (attr == null && term.isEmpty()) return true;
			if ("area".equals(attr)) {
				for (AcademicAreaCode ac: student().getAcademicAreaClasiffications())
					if (eq(ac.getArea(), term)) return true;
			} else if ("clasf".equals(attr) || "classification".equals(attr)) {
				for (AcademicAreaCode ac: student().getAcademicAreaClasiffications())
					if (eq(ac.getCode(), term)) return true;
			} else if ("major".equals(attr)) {
				for (AcademicAreaCode ac: student().getMajors())
					if (eq(ac.getCode(), term)) return true;
			} else if ("group".equals(attr)) {
				for (AcademicAreaCode ac: student().getMinors())
					if (eq(ac.getCode(), term)) return true;
			} else if  ("student".equals(attr)) {
				return has(student().getName(), term) || eq(student().getExternalId(), term) || eq(student().getName(), term);
			} else if ("registered".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return false;
				else
					return true;
			}
			return false;
		}
		
		private boolean eq(String name, String term) {
			if (name == null) return false;
			return name.equalsIgnoreCase(term);
		}

		private boolean has(String name, String term) {
			if (name == null) return false;
			if (eq(name, term)) return true;
			for (String t: name.split(" |,"))
				if (t.equalsIgnoreCase(term)) return true;
			return false;
		}
	}

	@Override
	public String name() {
		return "status-suggestions";
	}

}
