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
package org.unitime.timetable.model.base;

import java.io.Serializable;

import org.unitime.timetable.model.CourseCreditType;
import org.unitime.timetable.model.CourseCreditUnitConfig;
import org.unitime.timetable.model.CourseCreditUnitType;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.SchedulingSubpart;

public abstract class BaseCourseCreditUnitConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private String iCreditFormat;
	private Boolean iDefinesCreditAtCourseLevel;

	private CourseCreditType iCreditType;
	private CourseCreditUnitType iCreditUnitType;
	private SchedulingSubpart iSubpartOwner;
	private InstructionalOffering iInstructionalOfferingOwner;

	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_CREDIT_FORMAT = "creditFormat";
	public static String PROP_DEFINES_CREDIT_AT_COURSE_LEVEL = "definesCreditAtCourseLevel";

	public BaseCourseCreditUnitConfig() {
		initialize();
	}

	public BaseCourseCreditUnitConfig(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public String getCreditFormat() { return iCreditFormat; }
	public void setCreditFormat(String creditFormat) { iCreditFormat = creditFormat; }

	public Boolean isDefinesCreditAtCourseLevel() { return iDefinesCreditAtCourseLevel; }
	public Boolean getDefinesCreditAtCourseLevel() { return iDefinesCreditAtCourseLevel; }
	public void setDefinesCreditAtCourseLevel(Boolean definesCreditAtCourseLevel) { iDefinesCreditAtCourseLevel = definesCreditAtCourseLevel; }

	public CourseCreditType getCreditType() { return iCreditType; }
	public void setCreditType(CourseCreditType creditType) { iCreditType = creditType; }

	public CourseCreditUnitType getCreditUnitType() { return iCreditUnitType; }
	public void setCreditUnitType(CourseCreditUnitType creditUnitType) { iCreditUnitType = creditUnitType; }

	public SchedulingSubpart getSubpartOwner() { return iSubpartOwner; }
	public void setSubpartOwner(SchedulingSubpart subpartOwner) { iSubpartOwner = subpartOwner; }

	public InstructionalOffering getInstructionalOfferingOwner() { return iInstructionalOfferingOwner; }
	public void setInstructionalOfferingOwner(InstructionalOffering instructionalOfferingOwner) { iInstructionalOfferingOwner = instructionalOfferingOwner; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof CourseCreditUnitConfig)) return false;
		if (getUniqueId() == null || ((CourseCreditUnitConfig)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((CourseCreditUnitConfig)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "CourseCreditUnitConfig["+getUniqueId()+"]";
	}

	public String toDebugString() {
		return "CourseCreditUnitConfig[" +
			"\n	CreditFormat: " + getCreditFormat() +
			"\n	CreditType: " + getCreditType() +
			"\n	CreditUnitType: " + getCreditUnitType() +
			"\n	DefinesCreditAtCourseLevel: " + getDefinesCreditAtCourseLevel() +
			"\n	InstructionalOfferingOwner: " + getInstructionalOfferingOwner() +
			"\n	SubpartOwner: " + getSubpartOwner() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
