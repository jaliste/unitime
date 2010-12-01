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

import org.unitime.timetable.model.ExternalRoomDepartment;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.model.dao.ExternalRoomDepartmentDAO;

public abstract class BaseExternalRoomDepartmentDAO extends _RootDAO<ExternalRoomDepartment,Long> {

	private static ExternalRoomDepartmentDAO sInstance;

	public static ExternalRoomDepartmentDAO getInstance() {
		if (sInstance == null) sInstance = new ExternalRoomDepartmentDAO();
		return sInstance;
	}

	public Class<ExternalRoomDepartment> getReferenceClass() {
		return ExternalRoomDepartment.class;
	}

	@SuppressWarnings("unchecked")
	public List<ExternalRoomDepartment> findByRoom(org.hibernate.Session hibSession, Long roomId) {
		return hibSession.createQuery("from ExternalRoomDepartment x where x.room.uniqueId = :roomId").setLong("roomId", roomId).list();
	}
}
