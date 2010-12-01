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

import org.unitime.timetable.model.CharacteristicReservation;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.CourseOfferingReservation;

public abstract class BaseCourseOfferingReservation extends CharacteristicReservation implements Serializable {
	private static final long serialVersionUID = 1L;

	private CourseOffering iCourseOffering;


	public BaseCourseOfferingReservation() {
		initialize();
	}

	public BaseCourseOfferingReservation(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public CourseOffering getCourseOffering() { return iCourseOffering; }
	public void setCourseOffering(CourseOffering courseOffering) { iCourseOffering = courseOffering; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof CourseOfferingReservation)) return false;
		if (getUniqueId() == null || ((CourseOfferingReservation)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((CourseOfferingReservation)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "CourseOfferingReservation["+getUniqueId()+"]";
	}

	public String toDebugString() {
		return "CourseOfferingReservation[" +
			"\n	CourseOffering: " + getCourseOffering() +
			"\n	Owner: " + getOwner() +
			"\n	OwnerClassId: " + getOwnerClassId() +
			"\n	PriorEnrollment: " + getPriorEnrollment() +
			"\n	Priority: " + getPriority() +
			"\n	ProjectedEnrollment: " + getProjectedEnrollment() +
			"\n	Requested: " + getRequested() +
			"\n	ReservationType: " + getReservationType() +
			"\n	Reserved: " + getReserved() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
