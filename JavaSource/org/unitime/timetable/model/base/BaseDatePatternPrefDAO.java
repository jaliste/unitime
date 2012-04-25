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

import java.util.List;

import org.unitime.timetable.model.DatePatternPref;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.model.dao.DatePatternPrefDAO;

public abstract class BaseDatePatternPrefDAO extends _RootDAO<DatePatternPref,Long> {

	private static DatePatternPrefDAO sInstance;

	public static DatePatternPrefDAO getInstance() {
		if (sInstance == null) sInstance = new DatePatternPrefDAO();
		return sInstance;
	}

	public Class<DatePatternPref> getReferenceClass() {
		return DatePatternPref.class;
	}

	@SuppressWarnings("unchecked")
	public List<DatePatternPref> findByDatePattern(org.hibernate.Session hibSession, Long datePatternId) {
		return hibSession.createQuery("from DatePatternPref x where x.datePattern.uniqueId = :datePatternId").setLong("datePatternId", datePatternId).list();
	}
}
