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

import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.StudentAccomodation;

public abstract class BaseStudentAccomodation implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private String iName;
	private String iAbbreviation;
	private String iExternalUniqueId;

	private Session iSession;

	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_NAME = "name";
	public static String PROP_ABBREVIATION = "abbreviation";
	public static String PROP_EXTERNAL_UID = "externalUniqueId";

	public BaseStudentAccomodation() {
		initialize();
	}

	public BaseStudentAccomodation(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public String getName() { return iName; }
	public void setName(String name) { iName = name; }

	public String getAbbreviation() { return iAbbreviation; }
	public void setAbbreviation(String abbreviation) { iAbbreviation = abbreviation; }

	public String getExternalUniqueId() { return iExternalUniqueId; }
	public void setExternalUniqueId(String externalUniqueId) { iExternalUniqueId = externalUniqueId; }

	public Session getSession() { return iSession; }
	public void setSession(Session session) { iSession = session; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof StudentAccomodation)) return false;
		if (getUniqueId() == null || ((StudentAccomodation)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((StudentAccomodation)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "StudentAccomodation["+getUniqueId()+" "+getName()+"]";
	}

	public String toDebugString() {
		return "StudentAccomodation[" +
			"\n	Abbreviation: " + getAbbreviation() +
			"\n	ExternalUniqueId: " + getExternalUniqueId() +
			"\n	Name: " + getName() +
			"\n	Session: " + getSession() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
