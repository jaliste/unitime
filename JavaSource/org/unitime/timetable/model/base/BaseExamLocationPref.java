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

import org.unitime.timetable.model.ExamLocationPref;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.PreferenceLevel;

public abstract class BaseExamLocationPref implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;

	private Location iLocation;
	private PreferenceLevel iPrefLevel;
	private ExamPeriod iExamPeriod;

	public static String PROP_UNIQUEID = "uniqueId";

	public BaseExamLocationPref() {
		initialize();
	}

	public BaseExamLocationPref(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public Location getLocation() { return iLocation; }
	public void setLocation(Location location) { iLocation = location; }

	public PreferenceLevel getPrefLevel() { return iPrefLevel; }
	public void setPrefLevel(PreferenceLevel prefLevel) { iPrefLevel = prefLevel; }

	public ExamPeriod getExamPeriod() { return iExamPeriod; }
	public void setExamPeriod(ExamPeriod examPeriod) { iExamPeriod = examPeriod; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof ExamLocationPref)) return false;
		if (getUniqueId() == null || ((ExamLocationPref)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((ExamLocationPref)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "ExamLocationPref["+getUniqueId()+"]";
	}

	public String toDebugString() {
		return "ExamLocationPref[" +
			"\n	ExamPeriod: " + getExamPeriod() +
			"\n	Location: " + getLocation() +
			"\n	PrefLevel: " + getPrefLevel() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
