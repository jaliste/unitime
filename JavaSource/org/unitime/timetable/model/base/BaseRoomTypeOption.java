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

import org.unitime.timetable.model.RoomType;
import org.unitime.timetable.model.RoomTypeOption;
import org.unitime.timetable.model.Session;

public abstract class BaseRoomTypeOption implements Serializable {
	private static final long serialVersionUID = 1L;

	private RoomType iRoomType;
	private Session iSession;
	private Integer iStatus;
	private String iMessage;


	public static String PROP_STATUS = "status";
	public static String PROP_MESSAGE = "message";

	public BaseRoomTypeOption() {
		initialize();
	}

	protected void initialize() {}

	public RoomType getRoomType() { return iRoomType; }
	public void setRoomType(RoomType roomType) { iRoomType = roomType; }

	public Session getSession() { return iSession; }
	public void setSession(Session session) { iSession = session; }

	public Integer getStatus() { return iStatus; }
	public void setStatus(Integer status) { iStatus = status; }

	public String getMessage() { return iMessage; }
	public void setMessage(String message) { iMessage = message; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof RoomTypeOption)) return false;
		RoomTypeOption roomTypeOption = (RoomTypeOption)o;
		if (getRoomType() == null || roomTypeOption.getRoomType() == null || !getRoomType().equals(roomTypeOption.getRoomType())) return false;
		if (getSession() == null || roomTypeOption.getSession() == null || !getSession().equals(roomTypeOption.getSession())) return false;
		return true;
	}

	public int hashCode() {
		if (getRoomType() == null || getSession() == null) return super.hashCode();
		return getRoomType().hashCode() ^ getSession().hashCode();
	}

	public String toString() {
		return "RoomTypeOption[" + getRoomType() + ", " + getSession() + "]";
	}

	public String toDebugString() {
		return "RoomTypeOption[" +
			"\n	Message: " + getMessage() +
			"\n	RoomType: " + getRoomType() +
			"\n	Session: " + getSession() +
			"\n	Status: " + getStatus() +
			"]";
	}
}
