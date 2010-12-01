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

import org.unitime.timetable.model.StudentGroupReservation;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.model.dao.StudentGroupReservationDAO;

public abstract class BaseStudentGroupReservationDAO extends _RootDAO<StudentGroupReservation,Long> {

	private static StudentGroupReservationDAO sInstance;

	public static StudentGroupReservationDAO getInstance() {
		if (sInstance == null) sInstance = new StudentGroupReservationDAO();
		return sInstance;
	}

	public Class<StudentGroupReservation> getReferenceClass() {
		return StudentGroupReservation.class;
	}

	@SuppressWarnings("unchecked")
	public List<StudentGroupReservation> findByStudentGroup(org.hibernate.Session hibSession, Long studentGroupId) {
		return hibSession.createQuery("from StudentGroupReservation x where x.studentGroup.uniqueId = :studentGroupId").setLong("studentGroupId", studentGroupId).list();
	}
}
