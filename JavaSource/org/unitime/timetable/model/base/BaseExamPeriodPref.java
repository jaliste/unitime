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

import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.ExamPeriodPref;
import org.unitime.timetable.model.Preference;

public abstract class BaseExamPeriodPref extends Preference implements Serializable {
	private static final long serialVersionUID = 1L;

	private ExamPeriod iExamPeriod;


	public BaseExamPeriodPref() {
		initialize();
	}

	public BaseExamPeriodPref(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public ExamPeriod getExamPeriod() { return iExamPeriod; }
	public void setExamPeriod(ExamPeriod examPeriod) { iExamPeriod = examPeriod; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof ExamPeriodPref)) return false;
		if (getUniqueId() == null || ((ExamPeriodPref)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((ExamPeriodPref)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "ExamPeriodPref["+getUniqueId()+"]";
	}

	public String toDebugString() {
		return "ExamPeriodPref[" +
			"\n	ExamPeriod: " + getExamPeriod() +
			"\n	Owner: " + getOwner() +
			"\n	PrefLevel: " + getPrefLevel() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
