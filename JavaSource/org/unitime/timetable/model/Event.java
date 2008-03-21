/* 
 * UniTime 3.1 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2008, UniTime.org
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

import org.unitime.timetable.model.base.BaseEvent;



public class Event extends BaseEvent {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public Event () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Event (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public Event (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.EventType eventType,
		java.lang.Integer minCapacity,
		java.lang.Integer maxCapacity) {

		super (
			uniqueId,
			eventType,
			minCapacity,
			maxCapacity);
	}

/*[CONSTRUCTOR MARKER END]*/


}