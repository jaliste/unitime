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
package org.unitime.timetable.model;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.unitime.timetable.model.base.BaseExternalRoom;
import org.unitime.timetable.model.dao.ExternalRoomDAO;




public class ExternalRoom extends BaseExternalRoom {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public ExternalRoom () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public ExternalRoom (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public ExternalRoom (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.ExternalBuilding building,
		java.lang.String roomNumber,
		java.lang.Integer capacity,
		java.lang.String classification,
		java.lang.String scheduledRoomType,
		java.lang.Boolean isInstructional) {

		super (
			uniqueId,
			building,
			roomNumber,
			capacity,
			classification,
			scheduledRoomType,
			isInstructional);
	}

/*[CONSTRUCTOR MARKER END]*/

	public static ExternalRoom findExternalRoomForSession(String externalUniqueId, Session session){
		ExternalRoomDAO erDao = new ExternalRoomDAO();
		List rooms = erDao.getSession().createCriteria(ExternalRoom.class)
			.add(Restrictions.eq("externalUniqueId", externalUniqueId))
			.createCriteria("building")
				.add(Restrictions.eq("session.uniqueId", session.getUniqueId()))
			.setCacheable(true).list();

		if (rooms.size() == 1){
			return((ExternalRoom) rooms.get(0));
		}
		return(null);
	}
    
    public static List findAll(Long sessionId) {
        return new ExternalRoomDAO().getSession().createQuery(
                "select r from ExternalRoom r where r.building.session.uniqueId=:sessionId").
                setLong("sessionId", sessionId).
                setCacheable(true).
                list();
    }
    
    public static ExternalRoom findByBldgAbbvRoomNbr(Long sessionId, String bldgAbbv, String roomNbr) {
        return (ExternalRoom)new ExternalRoomDAO().getSession().createQuery(
                "select r from ExternalRoom r where r.building.session.uniqueId=:sessionId and " +
                "r.building.abbreviation=:bldgAbbv and r.roomNumber=:roomNbr").
                setLong("sessionId", sessionId).
                setString("bldgAbbv", bldgAbbv).
                setString("roomNbr", roomNbr).
                uniqueResult();
    }
	
}