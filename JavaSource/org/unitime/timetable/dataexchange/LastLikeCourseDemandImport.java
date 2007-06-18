/*
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org, and individual contributors
 * as indicated by the @authors tag.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package org.unitime.timetable.dataexchange;

import java.util.Iterator;

import org.dom4j.Element;

import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.LastLikeCourseDemand;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.SubjectAreaDAO;

/**
 * 
 * @author Timothy Almon
 *
 */

public class LastLikeCourseDemandImport extends BaseImport {

	public LastLikeCourseDemandImport() {
		super();
	}

	public void loadXml(Element root) throws Exception {
		try {
	        String campus = root.attributeValue("campus");
	        String year   = root.attributeValue("year");
	        String term   = root.attributeValue("term");

	        Session session = Session.getSessionUsingInitiativeYearTerm(campus, year, term);
	        if(session == null) {
	           	throw new Exception("No session found for the given campus, year, and term.");
	        }
			beginTransaction();
	        for ( Iterator it = root.elementIterator(); it.hasNext(); ) {
	            Element element = (Element) it.next();
	            String externalId = element.attributeValue("externalId");
	            System.out.println("Loading " + externalId);
	            Student student = fetchStudent(externalId, session.getSessionId());
	            if(student == null) continue;
	            loadCourses(element, student, session);
	            flushIfNeeded(true);
	        }
		} catch (Exception e) {
			fatal("Exception: " + e.getMessage(), e);
			rollbackTransaction();
			throw e;
		}
		finally {
            flush(true);
		}
	}

	Student fetchStudent(String externalId, Long sessionId) {
		return (Student) this.
		getHibSession().
		createQuery("select distinct a from Student as a where a.externalUniqueId=:externalId and a.session.uniqueId=:sessionId").
		setLong("sessionId", sessionId.longValue()).
		setString("externalId", externalId).
		setCacheable(true).
		uniqueResult();
	}

	private void loadCourses(Element studentEl, Student student, Session session) throws Exception {
		for (Iterator it = studentEl.elementIterator(); it.hasNext();) {
			Element el = (Element) it.next();
			String subject = el.attributeValue("subject");
			if(subject == null) {
				throw new Exception("Subject is required.");
			}
			String courseNumber = el.attributeValue("courseNumber");
			if(courseNumber == null) {
				throw new Exception("Course Number is required.");
			}
			SubjectArea area = fetchSubjectArea(subject, session.getSessionId());
			if(area == null) {
				System.out.println("Subject area " + subject + " not found");
				continue;
			}

	        LastLikeCourseDemand demand = new LastLikeCourseDemand();

	        CourseOffering courseOffering = fetchCourseOffering(courseNumber, area.getUniqueId());
			if(courseOffering == null) {
		        demand.setCoursePermId(null);
			} else {
		        demand.setCoursePermId(courseOffering.getPermId());
			}
	        demand.setCourseNbr(courseNumber);
	        demand.setStudent(student);
	        demand.setSubjectArea(area);
	        demand.setPriority(Integer.decode(el.attributeValue("priority")));
	        getHibSession().save(demand);
		}
	}

	SubjectArea fetchSubjectArea(String subjectAreaAbbv, Long sessionId) {
		return (SubjectArea) new SubjectAreaDAO().
		getSession().
		createQuery("select distinct a from SubjectArea as a where a.session.uniqueId=:sessionId and a.subjectAreaAbbreviation=:subjectAreaAbbv").
		setLong("sessionId", sessionId.longValue()).
		setString("subjectAreaAbbv", subjectAreaAbbv).
		setCacheable(true).
		uniqueResult();
	}

	CourseOffering fetchCourseOffering(String courseNbr, Long subjectAreaId) {
		return (CourseOffering) new CourseOfferingDAO().
		getSession().
		createQuery("select distinct a from CourseOffering as a where a.courseNbr=:courseNbr and a.subjectArea=:subjectAreaId").
		setLong("subjectAreaId", subjectAreaId.longValue()).
		setString("courseNbr", courseNbr).
		setCacheable(true).
		uniqueResult();
	}
}