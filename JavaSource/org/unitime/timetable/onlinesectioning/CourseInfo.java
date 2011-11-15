/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.onlinesectioning;

import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.onlinesectioning.custom.CourseDetailsProvider;

/**
 * @author Tomas Muller
 */
public class CourseInfo implements Comparable<CourseInfo> {
	private static StudentSectioningMessages MSG = Localization.create(StudentSectioningMessages.class);
	private Long iUniqueId;
	private Long iAcademicSessionId;
	private String iSubjectArea;
	private String iDepartment;
	private String iCourseNbr;
	private String iTitle;
	private String iCourseNameLowerCase;
	private String iTitleLowerCase;
	private String iNote;
	private String iDetails = null;
	private boolean iHasUniqueName = true;
	private String iConsent = null, iConsentAbbv = null;
	private Integer iWkEnroll = null, iWkChange = null, iWkDrop = null;

	
	public CourseInfo(CourseOffering course)  throws SectioningException {
		iUniqueId = course.getUniqueId();
		iAcademicSessionId = course.getSubjectArea().getSession().getUniqueId();
		iSubjectArea = course.getSubjectArea().getSubjectAreaAbbreviation();
		iDepartment = (course.getSubjectArea().getDepartment().getDeptCode() == null ? course.getSubjectArea().getDepartment().getAbbreviation() : course.getSubjectArea().getDepartment().getDeptCode());
		iCourseNbr = course.getCourseNbr().trim();
		iTitle = (course.getTitle() == null ? null : course.getTitle().trim());
		iNote = course.getScheduleBookNote();
		if (course.getInstructionalOffering().getConsentType() != null) {
			iConsent = course.getInstructionalOffering().getConsentType().getLabel();
			iConsentAbbv = course.getInstructionalOffering().getConsentType().getAbbv();
		}
		iCourseNameLowerCase = (iSubjectArea + " " + iCourseNbr).toLowerCase();
		iTitleLowerCase = (iTitle == null ? null : iTitle.toLowerCase());
		iWkEnroll = course.getInstructionalOffering().getLastWeekToEnroll();
		iWkChange = course.getInstructionalOffering().getLastWeekToChange();
		iWkDrop = course.getInstructionalOffering().getLastWeekToDrop();
	}
	
	public CourseInfo(CourseOffering course, String courseNbr)  throws SectioningException {
		this(course);
		iCourseNbr = courseNbr;
		iCourseNameLowerCase = (iSubjectArea + " " + iCourseNbr).toLowerCase();
	}
	
	public Long getUniqueId() { return iUniqueId; }
	public Long getAcademicSessionId() { return iAcademicSessionId; }
	public String getSubjectArea() { return iSubjectArea; }
	public String getCourseNbr() { return iCourseNbr; }
	public String getDepartment() { return iDepartment; }
	public String getTitle() { return iTitle; }
	public String getNote() { return iNote; }
	public boolean hasUniqueName() { return iHasUniqueName; }
	public void setHasUniqueName(boolean hasUniqueName) { iHasUniqueName = hasUniqueName; }
	public String getConsent() { return iConsent; }
	public String getConsentAbbv() { return iConsentAbbv; }

	public int compareTo(CourseInfo c) {
		int cmp = getSubjectArea().compareToIgnoreCase(c.getSubjectArea());
		if (cmp!=0) return cmp;
		cmp = getCourseNbr().compareToIgnoreCase(c.getCourseNbr());
		if (cmp!=0) return cmp;
		cmp = (getTitle() == null ? "" : getTitle()).compareToIgnoreCase(c.getTitle() == null ? "" : c.getTitle());
		if (cmp!=0) return cmp;
		return getUniqueId().compareTo(c.getUniqueId());
	}
	
	public boolean matchCourseName(String queryInLowerCase) {
		if (iCourseNameLowerCase.startsWith(queryInLowerCase)) return true;
		if (iTitleLowerCase == null) return false;
		if ((iCourseNameLowerCase + " " + iTitleLowerCase).startsWith(queryInLowerCase)) return true;
		if ((iCourseNameLowerCase + " - " + iTitleLowerCase).toLowerCase().startsWith(queryInLowerCase)) return true;
		return false;
	}
	
	public boolean matchTitle(String queryInLowerCase) {
		if (iTitleLowerCase == null) return false;
		if (!matchCourseName(queryInLowerCase) && iTitleLowerCase.contains(queryInLowerCase)) return true;
		return false;
	}

	public String getDetails() throws SectioningException {
		if (iDetails == null) {
			CourseDetailsProvider provider = null;
			try {
				provider = (CourseDetailsProvider)Class.forName(ApplicationProperties.getProperty("unitime.custom.CourseDetailsProvider")).newInstance();
			} catch (Exception e) {
				throw new SectioningException(MSG.exceptionNoCustomCourseDetails());
			}
			iDetails = provider.getDetails(OnlineSectioningService.getInstance(iAcademicSessionId).getAcademicSession(), iSubjectArea, iCourseNbr);
		}
		return iDetails;
	}
	
	public String toString() {
		return (getSubjectArea() + " " + getCourseNbr()).toLowerCase();
	}
	
	public Integer getLastWeekToEnroll() { return iWkEnroll; }
	public Integer getLastWeekToChange() { return iWkChange; }
	public Integer getLastWeekToDrop() { return iWkDrop; }
}
