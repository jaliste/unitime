/*
 * UniTime 3.3 (University Timetabling Application)
 * Copyright (C) 2011, UniTime LLC, and individual contributors
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
package org.unitime.timetable.gwt.resources;

import com.google.gwt.i18n.client.Constants;

public interface GwtConstants extends Constants {
	
	@DefaultStringValue("3.4")
	String version();

	@DefaultStringValue("&copy; 2008 - 2012 UniTime LLC")
	String copyright();
	
	@DefaultBooleanValue(true)
	boolean useAmPm();

	@DefaultStringArrayValue({"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"})
	String[] days();

	@DefaultStringValue("MM/dd/yyyy")
	String eventDateFormat();
	
	@DefaultStringValue("MM/dd hh:mmaa")
	String timeStampFormat();
	
	@DefaultIntValue(3)
	int eventSlotIncrement();
	
	@DefaultIntValue(90)
	int eventStartDefault();
	
	@DefaultIntValue(210)
	int eventStopDefault();
	
	@DefaultIntValue(12)
	int eventLengthDefault();
	
	@DefaultIntValue(10000)
	int maxMeetings();
	
	@DefaultStringArrayValue({"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"})
	String[] longDays();

	@DefaultStringArrayValue({
		"blue", "green", "orange", "yellow", "pink",
		"purple", "teal", "darkpurple", "steelblue", "lightblue",
		"lightgreen", "yellowgreen", "redorange", "lightbrown", "lightpurple",
		"grey", "bluegrey", "lightteal", "yellowgrey", "brown", "red"})
	String[] meetingColors();

}
