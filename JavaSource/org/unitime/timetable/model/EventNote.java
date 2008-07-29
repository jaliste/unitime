/* 
 * UniTime 3.1 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2008, UniTime LLC
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

import java.text.SimpleDateFormat;
import java.util.Collection;

import org.unitime.timetable.model.Event.MultiMeeting;
import org.unitime.timetable.model.base.BaseEventNote;



public class EventNote extends BaseEventNote implements Comparable<EventNote> {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public EventNote () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public EventNote (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public EventNote (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.Event event,
		java.lang.Integer noteType,
		java.util.Date timeStamp) {

		super (
			uniqueId,
			event,
			noteType,
			timeStamp);
	}
	
/*[CONSTRUCTOR MARKER END]*/
	
	public static final int sEventNoteTypeCreateEvent = 0;
    public static final int sEventNoteTypeAddMeetings = 1;
	public static final int sEventNoteTypeApproval = 2;
	public static final int sEventNoteTypeRejection = 3;
	public static final int sEventNoteTypeDeletion = 4;
	
	public static final String[] sEventNoteTypeBgColor = new String[] {
	    "#FFFFFF", "#FFFFFF", "#D7FFD7", "#FFD7D7", "#FFFFFF" 
	};
	public static final String[] sEventNoteTypeName = new String[] {
	    "Create", "Update", "Approve", "Reject", "Delete"
	};

	public int compareTo(EventNote n) {
	    int cmp = getTimeStamp().compareTo(n.getTimeStamp());
	    if (cmp!=0) return cmp;
	    return getUniqueId().compareTo(n.getUniqueId());
	}
	
	public void setMeetingCollection(Collection meetings) {
        String meetingStr = "";
        for (MultiMeeting m : Event.getMultiMeetings(meetings)) {
            if (meetingStr.length()>0) meetingStr += "\n";
            meetingStr += m.toShortString();
        }
        setMeetings(meetingStr);
	}
	
	public String getMeetingsHtml() {
	    if (getMeetings()==null || getMeetings().length()==0) return "<i>N/A</i>";
	    return getMeetings().replaceAll("\n", "<br>");
	}
	
	public String toHtmlString() {
	    return "<tr style=\"background-color:"+sEventNoteTypeBgColor[getNoteType()]+";\" valign='top' " +
	            "onMouseOver=\"this.style.backgroundColor='rgb(223,231,242)';\" " +
	            "onMouseOut=\"this.style.backgroundColor='"+sEventNoteTypeBgColor[getNoteType()]+"';\">" +
	    		"<td>"+new SimpleDateFormat("MM/dd hh:mmaa").format(getTimeStamp())+"</td>" +
                "<td>"+(getUser()==null || getUser().length()==0?"<i>N/A</i>":getUser())+"</td>" +
	    		"<td>"+sEventNoteTypeName[getNoteType()]+"</td>" +
	    		"<td>"+getMeetingsHtml()+"</td>"+
	    		"<td>"+(getTextNote()==null?"":getTextNote().replaceAll("\n", "<br>"))+"</td>"+
	    		"</tr>";
	}

}
