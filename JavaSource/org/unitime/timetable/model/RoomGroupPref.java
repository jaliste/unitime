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

import org.unitime.timetable.model.base.BaseRoomGroupPref;

import net.sf.cpsolver.ifs.util.ToolBox;



public class RoomGroupPref extends BaseRoomGroupPref {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public RoomGroupPref () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public RoomGroupPref (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public RoomGroupPref (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.PreferenceGroup owner,
		org.unitime.timetable.model.PreferenceLevel prefLevel) {

		super (
			uniqueId,
			owner,
			prefLevel);
	}

/*[CONSTRUCTOR MARKER END]*/
	
	public String preferenceText() { 
		return (this.getRoomGroup().getName());
    }

    public String preferenceAbbv() { 
        return (this.getRoomGroup().getAbbv());
    }

    public Object clone() {
    	RoomGroupPref pref = new RoomGroupPref();
    	pref.setPrefLevel(getPrefLevel());
    	pref.setRoomGroup(getRoomGroup());
    	return pref;
    }
    public boolean isSame(Preference other) {
    	if (other==null || !(other instanceof RoomGroupPref)) return false;
    	return ToolBox.equals(getRoomGroup(),((RoomGroupPref)other).getRoomGroup());
    }

	public String preferenceTitle() {
		return getPrefLevel().getPrefName()+" Room Group "+getRoomGroup().getNameWithTitle();
	}
}