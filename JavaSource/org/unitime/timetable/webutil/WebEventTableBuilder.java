/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008 - 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.webutil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import org.hibernate.Query;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.commons.web.htmlgen.TableCell;
import org.unitime.commons.web.htmlgen.TableHeaderCell;
import org.unitime.commons.web.htmlgen.TableRow;
import org.unitime.commons.web.htmlgen.TableStream;
import org.unitime.timetable.form.EventListForm;
import org.unitime.timetable.form.MeetingListForm;
import org.unitime.timetable.model.ClassEvent;
import org.unitime.timetable.model.CourseEvent;
import org.unitime.timetable.model.Event;
import org.unitime.timetable.model.ExamEvent;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.RelatedCourseInfo;
import org.unitime.timetable.model.Event.MultiMeeting;
import org.unitime.timetable.model.dao.ClassEventDAO;
import org.unitime.timetable.model.dao.CourseEventDAO;
import org.unitime.timetable.model.dao.EventDAO;
import org.unitime.timetable.model.dao.ExamEventDAO;
import org.unitime.timetable.util.Constants;


public class WebEventTableBuilder {

	public static SimpleDateFormat sDateFormat = new SimpleDateFormat("EEE MM/dd, yyyy", Locale.US);
	//public static SimpleDateFormat sDateFormatDay = new SimpleDateFormat("EEE", Locale.US);	
	public static SimpleDateFormat sDateFormatM1 = new SimpleDateFormat("MM/dd", Locale.US);
	public static SimpleDateFormat sDateFormatM2 = new SimpleDateFormat("MM/dd, yyyy", Locale.US);
	
	//Colors
    protected static String indent = "&nbsp;&nbsp;&nbsp;&nbsp;";
    protected static String oddRowBGColor = "#f1f3f9";
    protected static String oddRowMouseOverBGColor = "#8EACD0";
    protected static String evenRowMouseOverBGColor = "#8EACD0";
    protected String disabledColor = "gray";

    protected static String formName = "eventListForm";
    
    protected static String LABEL = "Name";
    protected static String EMPTY = "&nbsp;";
    public static final String EVENT_CAPACITY = "Attend/Limit";
    public static final String ENROLLMENT = "Enrollment";
    public static final String EVENT_TYPE = "Type";
    public static final String MAIN_CONTACT = "Main Contact";
    public static final String SPONSORING_ORG = "Sponsoring Org";
    public static final String MEETING_DATE = "Date";
    public static final String MEETING_TIME = "Time";
    public static final String MEETING_LOCATION = "Location";
    public static final String APPROVED_DATE = "Approved";
    
    public int getMaxResults() {
    	return 100;
    }
    
    public WebEventTableBuilder() {
    	super();
    }
    
    protected String getRowMouseOver(boolean isHeaderRow, boolean isControl){
        return ("this.style.backgroundColor='" 
                + (isHeaderRow ?oddRowMouseOverBGColor:evenRowMouseOverBGColor) 
                + "';this.style.cursor='"
                + (isControl ? "hand" : "default")
                + "';this.style.cursor='"
                + (isControl ? "pointer" : "default")+ "';");
   	
    }
    
    protected String getRowMouseOut(String color){
        return ("this.style.backgroundColor='"+ (color==null?"transparent":color) +"';");   	
    }

    protected TableRow initRow(boolean isHeaderRow){
        TableRow row = new TableRow();
        if (isHeaderRow){
        	row.setBgColor(oddRowBGColor);
        }
        return (row);
    }
    
    protected TableHeaderCell headerCell(String content, int rowSpan, int colSpan){
    	TableHeaderCell cell = new TableHeaderCell();
    	cell.setRowSpan(rowSpan);
    	cell.setColSpan(colSpan);
    	cell.setAlign("left");
    	cell.setValign("bottom");
    	cell.addContent("<font size=\"-1\">");
    	cell.addContent(content);
    	cell.addContent("</font>");
		cell.setStyleClass("WebTableHeader");
    	return(cell);
     }

    
    protected TableHeaderCell headerCell(String content, int rowSpan, int colSpan, boolean firstRow){
    	TableHeaderCell cell = headerCell(content, rowSpan, colSpan);
		cell.setStyleClass("WebTableHeader" + (firstRow ? "FirstRow" : "SecondRow"));
    	return(cell);
     }
    
    private TableCell initCell(boolean isEditable, String onClick, int cols){
        return (initCell(isEditable, onClick, cols, false));
    }

    private TableCell initCell(boolean isEditable, String onClick, int cols, boolean nowrap){
        TableCell cell = new TableCell();
        cell.setValign("top");
        if (cols > 1){
            cell.setColSpan(cols);
        }
        if (nowrap){
            cell.setNoWrap(true);
        }
        if (onClick != null && onClick.length() > 0){
        	cell.setOnClick(onClick);
        }
        if (!isEditable){
        	cell.addContent("<font color=" + disabledColor + ">");
        }
        return (cell);
    }

    private void endCell(TableCell cell, boolean isEditable){
        if (!isEditable){
            cell.addContent("</font>");
        }   
    }
   
    protected TableCell initNormalCell(String text, boolean isEditable){
        return (initColSpanCell(text, isEditable, 1));
    }
    
    private TableCell initColSpanCell(String text, boolean isEditable, int cols){
        TableCell cell = initCell(isEditable, null, cols);
        cell.addContent(text);
        endCell(cell, isEditable);
        return (cell);
        
    }
    
    protected void buildTableHeader(TableStream table, boolean mainContact){  
    	TableRow row = new TableRow();
    	TableRow row2 = new TableRow();
     	TableHeaderCell cell = null;
        cell = this.headerCell(LABEL, 1, 1, true);
        row.addContent(cell);
        cell = this.headerCell(EMPTY, 1, 1, false);
        row2.addContent(cell);
        cell = this.headerCell(ENROLLMENT, 1, 1, true);
        cell.setAlign("right");
        row.addContent(cell);
        cell = this.headerCell(EVENT_CAPACITY + "&nbsp;", 1, 1, true);
        cell.setAlign("right");
        row.addContent(cell);
        cell = this.headerCell(MEETING_DATE, 1, 2, false);
        row2.addContent(cell);
        cell = this.headerCell(SPONSORING_ORG, 1, 1, true);
        row.addContent(cell);
        cell = this.headerCell(MEETING_TIME, 1, 1, false);
        row2.addContent(cell);
        cell = this.headerCell(EVENT_TYPE, 1, 1, true);
        row.addContent(cell);
        cell = this.headerCell(MEETING_LOCATION, 1, 1, false);
        row2.addContent(cell);
    	if (mainContact) {
    		cell = this.headerCell(MAIN_CONTACT, 1, 1, true);
    		row.addContent(cell);
    		cell = this.headerCell(APPROVED_DATE, 1, 1, false);
    		row2.addContent(cell);
    	}
    	table.addContent(row);
    	table.addContent(row2);
   }

    protected void buildMeetingTableHeader(TableStream table, boolean mainContact){  
        TableRow row = new TableRow();
        TableHeaderCell cell = null;
        cell = this.headerCell(LABEL, 1, 1);
        row.addContent(cell);
        cell = this.headerCell(EVENT_TYPE, 1, 1);
        row.addContent(cell);
        cell = this.headerCell(ENROLLMENT, 1, 1);
        cell.setAlign("right");
        row.addContent(cell);
        cell = this.headerCell(EVENT_CAPACITY, 1, 1);
        cell.setAlign("right");
        row.addContent(cell);
        cell = this.headerCell(SPONSORING_ORG, 1, 1);
        row.addContent(cell);
        cell = this.headerCell(MEETING_DATE, 1, 1);
        row.addContent(cell);
        cell = this.headerCell(MEETING_TIME, 1, 1);
        row.addContent(cell);
        cell = this.headerCell(MEETING_LOCATION, 1, 1);
        row.addContent(cell);
        if (mainContact) {
            cell = this.headerCell(MAIN_CONTACT, 1, 1);
            row.addContent(cell);
            cell = this.headerCell(APPROVED_DATE, 1, 1);
            row.addContent(cell);
        }
        table.addContent(row);
   }

    private String subjectOnClickAction(Long eventId){
        return("document.location='eventDetail.do?op=view&id=" + eventId + "';");
    }    
    
    private TableCell buildEventName(Event e) {
        TableCell cell = this.initCell(true, null, 1, true);    	
        cell.addContent("<a name='A"+e.getUniqueId()+"' style='color:black;'>"+(e.getEventName()==null?"&nbsp;":"<b>"+e.getEventName()+"</b>")+"</a>");
        this.endCell(cell, true);
        return (cell);
    }

    private TableCell buildEventCapacity(Event e) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	int minCap = (e.getMinCapacity()==null?-1:e.getMinCapacity());
    	int maxCap = (e.getMaxCapacity()==null?-1:e.getMaxCapacity());
    	if (minCap==-1){
    		cell.addContent("&nbsp;");
    	} else {
    		if (maxCap!=-1) {
    			if (maxCap!=minCap) {
        			cell.addContent(minCap+"-"+maxCap+"&nbsp;");    				
    			} else {cell.addContent(minCap+"&nbsp;");}
    		}
			cell.setAlign("right");
    	}
//    	this.endCell(cell, true);
    	return (cell);
    }

    private TableCell buildEventEnrollment(Event e) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	if (Event.sEventTypeClass == e.getEventType()) {
    		ClassEvent ce = new ClassEventDAO().get(Long.valueOf(e.getUniqueId()));
			if (ce.getClazz().getEnrollment() != null){
				cell.addContent(ce.getClazz().getEnrollment().toString());
			} else {
				cell.addContent("0");
			}
		} else if (Event.sEventTypeFinalExam == e.getEventType() || Event.sEventTypeMidtermExam == e.getEventType()) {
			ExamEvent ee = new ExamEventDAO().get(e.getUniqueId());
			cell.addContent(String.valueOf(ee.getExam().countStudents()));
		} else if (Event.sEventTypeCourse == e.getEventType()) {
			CourseEvent ce = new CourseEventDAO().get(e.getUniqueId());
			int enrl = 0;
			for (RelatedCourseInfo rci: ce.getRelatedCourses())
				enrl += rci.countStudents();
			cell.addContent(String.valueOf(enrl));
		} else {
			cell.addContent("&nbsp;");
		}
		cell.setAlign("right");
    	this.endCell(cell, true);
    	return (cell);
    }

    private TableCell buildSponsoringOrg(Event e) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	cell.addContent(e.getSponsoringOrganization()==null?"&nbsp;":e.getSponsoringOrganization().getName());
    	return(cell);
    }

    private TableCell buildEventType(Event e) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	cell.addContent(e.getEventTypeLabel());
    	return(cell);
    }
    
    private TableCell buildEventTypeAbbv(Event e) {
        TableCell cell = this.initCell(true, null, 1, true);
        cell.addContent(e.getEventTypeAbbv());
        return(cell);
    }
    
    private TableCell buildMainContactName(Event e) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	if (e.getMainContact()!=null)
    	    cell.addContent((e.getMainContact().getLastName()==null?"&nbsp;":(e.getMainContact().getLastName()+", "))+
    	    			(e.getMainContact().getFirstName()==null?"":e.getMainContact().getFirstName()));
    	else
    	    cell.addContent("&nbsp;");
    	return(cell);
    }
    
    private TableCell buildEmptyMeetingInfo() {
    	TableCell cell = this.initCell(true, null, 1, true);
    	cell.addContent("&nbsp;");
    	return(cell);
    }
 
    private TableCell buildDate (Meeting m) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	cell.addContent(sDateFormat.format(m.getMeetingDate()));
    	/*
    			+" &nbsp;&nbsp;<font color='gray'><i>("
    			+sDateFormatDay.format(m.getMeetingDate())
    			+")</i></font>"); //date cannot be null*/
    	return(cell);
    }
    
    private TableCell buildDate (MultiMeeting m) {
        TableCell cell = this.initCell(true, null, 1, true);
        Calendar c = Calendar.getInstance();
        c.setTime(m.getMeetings().first().getMeetingDate());
        int y1 = c.get(Calendar.YEAR);
        c.setTime(m.getMeetings().last().getMeetingDate());
        int y2 = c.get(Calendar.YEAR);
        cell.addContent(
                m.getDays()+" "+
                (y1==y2?sDateFormatM1:sDateFormatM2).format(m.getMeetings().first().getMeetingDate())
                +" - "+
                sDateFormatM2.format(m.getMeetings().last().getMeetingDate())
                );
        return(cell);
    }

    private TableCell buildTime (Meeting m) {
    	TableCell cell = this.initCell(true, null, 1, true);
		cell.addContent(
		        m.isAllDay()?"All Day":
		        Constants.toTime(Constants.SLOT_LENGTH_MIN*m.getStartPeriod()+Constants.FIRST_SLOT_TIME_MIN+(m.getStartOffset()==null?0:m.getStartOffset()))+" - "+
		        Constants.toTime(Constants.SLOT_LENGTH_MIN*m.getStopPeriod()+Constants.FIRST_SLOT_TIME_MIN+(m.getStopOffset()==null?0:m.getStopOffset())));
		return(cell);
    }
    
    private TableCell buildLocation (Meeting m) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	cell.addContent(m.getLocation()==null?"&nbsp;":m.getLocation().getLabelWithHint());
    	return(cell);
    }
    
    private TableCell buildApproved (Meeting m) {
    	TableCell cell = this.initCell(true, null, 1, true);
    	SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy", Locale.US);
    	cell.addContent(m.getApprovedDate()==null?"&nbsp;":df.format(m.getApprovedDate()));
    	return(cell);
    }
    
    private TableCell buildApproved (MultiMeeting mm) {
        TableCell cell = this.initCell(true, null, 1, true);
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy", Locale.US);
        Date approvalDate = null; //latest approval date
        for (Meeting m : mm.getMeetings())
            if (approvalDate==null || approvalDate.compareTo(m.getApprovedDate())<0) approvalDate = m.getApprovedDate();
        cell.addContent(approvalDate==null?"&nbsp;":df.format(approvalDate));
        return(cell);
    }

    private void addEventsRowsToTable(TableStream table, Event e, boolean mainContact, TreeSet<MultiMeeting> meetings) {
        TableRow row = (this.initRow(true));
        row.setOnMouseOver(this.getRowMouseOver(true, true));
        row.setOnMouseOut(this.getRowMouseOut(oddRowBGColor));
        row.setOnClick(subjectOnClickAction(e.getUniqueId()));
        
        row.addContent(buildEventName(e));
        row.addContent(buildEventEnrollment(e));
        row.addContent(buildEventCapacity(e));
        row.addContent(buildSponsoringOrg(e));
        row.addContent(buildEventType(e));
        if (mainContact)
            row.addContent(buildMainContactName(e));
        boolean allPast = true;
        for (MultiMeeting meeting : meetings) {
            if (!meeting.isPast()) { allPast = false; break; }
        }
        if (allPast) row.setStyle("font-style:italic;color:gray;");
        table.addContent(row);
    }
    
    private void addMeetingRowsToTable (TableStream table, MultiMeeting mm, boolean mainContact, boolean printOverlaps) {
        Meeting m = mm.getMeetings().first();
        TableRow row = (this.initRow(false));
        row.setOnMouseOver(this.getRowMouseOver(false, true));
        row.setOnClick(subjectOnClickAction(m.getEvent().getUniqueId()));
        row.addContent(buildEmptyMeetingInfo());
        TableCell cell = (mm.getMeetings().size()==1?buildDate(m):buildDate(mm));
        cell.setColSpan(2);
        row.addContent(cell);
        row.addContent(buildTime(m));
        row.addContent(buildLocation(m));
        String bgColor = null;
        if (mainContact)
            row.addContent(mm.getMeetings().size()==1?buildApproved(m):buildApproved(mm));
        if (mm.isPast()) {
            row.setStyle("font-style:italic;color:gray;");
        } else {
            if (m.isApproved()) {
                //bgColor = "#DDFFDD";
            } else {
                bgColor = "#FFFFDD";
            }
        }
        row.setBgColor(bgColor);
        row.setOnMouseOut(getRowMouseOut(bgColor));
        table.addContent(row);
        if (printOverlaps) {
            TreeSet<Meeting> overlaps = new TreeSet();
            for (Meeting mx: mm.getMeetings()) {
                overlaps.addAll(mx.getTimeRoomOverlaps());
            }
            if (!overlaps.isEmpty()) {
                for (MultiMeeting o: Event.getMultiMeetings(overlaps))
                    addOverlappingMeetingToTable(table, o, mainContact);
            }
        }
    }
    
    private void addOverlappingMeetingToTable(TableStream table, MultiMeeting mm, boolean mainContact) {
        Meeting m = mm.getMeetings().first();
        TableRow row = (this.initRow(false));
        row.setOnMouseOver(this.getRowMouseOver(false, true));
        row.setOnClick(subjectOnClickAction(m.getEvent().getUniqueId()));
        TableCell cell = this.initCell(true, null, 1, true);
        cell.addContent("&nbsp;&nbsp;&nbsp;Conf/w "+m.getEvent().getEventName()+" ("+m.getEvent().getEventTypeAbbv()+")");
        row.addContent(cell);
        TableCell dateCell = (mm.getMeetings().size()==1?buildDate(m):buildDate(mm));
        dateCell.setColSpan(2);
        row.addContent(dateCell);
        row.addContent(buildTime(m));
        row.addContent(buildLocation(m));
        if (mainContact)
            row.addContent(mm.getMeetings().size()==1?buildApproved(m):buildApproved(mm));
        if (mm.isPast()) {
            row.setStyle("font-style:italic;color:gray;");
        }        
        row.setBgColor("#FFD7D7");
        row.setOnMouseOut(getRowMouseOut("#FFD7D7"));
        table.addContent(row);
    }
    
    private void addMeetingRowsToTable (TableStream table, Meeting m, boolean mainContact, Event lastEvent, Date now, boolean line, boolean printOverlaps) {
        TableRow row = (this.initRow(false));
        row.setOnMouseOver(this.getRowMouseOver(false, true));
        row.setOnClick(subjectOnClickAction(m.getEvent().getUniqueId()));
        if (lastEvent!=null && lastEvent.getUniqueId().equals(m.getEvent().getUniqueId())) {
            TableCell cell = this.initCell(true, null, 1, true);
            cell.addContent("&nbsp;");
            row.addContent(cell);
            row.addContent(cell);
            row.addContent(cell);
            row.addContent(cell);
            row.addContent(cell);
        } else {
            row.addContent(buildEventName(m.getEvent()));
            row.addContent(buildEventTypeAbbv(m.getEvent()));
            row.addContent(buildEventEnrollment(m.getEvent()));
            row.addContent(buildEventCapacity(m.getEvent()));
            row.addContent(buildSponsoringOrg(m.getEvent()));
        }
        row.addContent(buildDate(m));
        row.addContent(buildTime(m));
        row.addContent(buildLocation(m));
        String bgColor = null;
        if (mainContact) {
            if (lastEvent!=null && lastEvent.getUniqueId().equals(m.getEvent().getUniqueId())) {
                TableCell cell = this.initCell(true, null, 1, true);
                cell.addContent("&nbsp;");
                row.addContent(cell);
            } else {
                row.addContent(buildMainContactName(m.getEvent()));
            }
            row.addContent(buildApproved(m));
        }
        if (m.getStartTime().before(now)) {
            row.setStyle("font-style:italic;color:gray;");
        } else {
            if (m.isApproved()) {
                //bgColor = "#DDFFDD";
            } else {
                bgColor = "#FFFFDD";
            }
        }
        row.setBgColor(bgColor);
        row.setOnMouseOut(getRowMouseOut(bgColor));
        if (line && lastEvent!=null && !lastEvent.getUniqueId().equals(m.getEvent().getUniqueId())) {
            for (Iterator i=row.getContents().iterator();i.hasNext();)
                ((TableCell)i.next()).setStyle("border-top: 1px dashed #9CB0CE;");
        }
        table.addContent(row);
        if (printOverlaps) {
            TreeSet<Meeting> overlaps = new TreeSet(m.getTimeRoomOverlaps());
            if (!overlaps.isEmpty()) {
                for (Meeting o: overlaps)
                    addOverlappingMeetingToTable(table, o, mainContact, now);
            }
        }
    }
    
    private void addOverlappingMeetingToTable(TableStream table, Meeting m, boolean mainContact, Date now) {
        TableRow row = (this.initRow(false));
        row.setOnMouseOver(this.getRowMouseOver(false, true));
        row.setOnClick(subjectOnClickAction(m.getEvent().getUniqueId()));
        TableCell cell = this.initCell(true, null, 2, true);        
        cell.addContent("&nbsp;&nbsp;&nbsp;Conf/w "+(m.getEvent().getEventName()==null?"":m.getEvent().getEventName())+" ("+m.getEvent().getEventTypeAbbv()+")");
        this.endCell(cell, true);
        row.addContent(cell);
        //row.addContent(buildEventTypeAbbv(m.getEvent()));
        row.addContent(buildEventEnrollment(m.getEvent()));
        row.addContent(buildEventCapacity(m.getEvent()));
        row.addContent(buildSponsoringOrg(m.getEvent()));
        row.addContent(buildDate(m));
        row.addContent(buildTime(m));
        row.addContent(buildLocation(m));
        if (mainContact) {
            row.addContent(buildMainContactName(m.getEvent()));
            row.addContent(buildApproved(m));
        }
        if (m.getStartTime().before(now)) {
            row.setStyle("font-style:italic;color:gray;");
        }
        row.setBgColor("#FFD7D7");
        row.setOnMouseOut(getRowMouseOut("#FFD7D7"));
        table.addContent(row);
    }
    
    protected List loadEvents(EventListForm form) {
        boolean conf = (form.getMode()==EventListForm.sModeAllConflictingEvents);
        
        
        String query = "select distinct e from Event e inner join e.meetings m where e.class in (";
        
        if (conf) {
            query = "select distinct e from Event e inner join e.meetings m, Meeting mx where "+
                    "mx.uniqueId!=m.uniqueId and m.meetingDate=mx.meetingDate and m.startPeriod < mx.stopPeriod and m.stopPeriod > mx.startPeriod and " +
                    "m.locationPermanentId = mx.locationPermanentId and e.class in (";
        }
        
        for (int i=0;i<form.getEventTypes().length;i++) {
            if (i>0) query+=",";
            switch (form.getEventTypes()[i].intValue()) {
            case Event.sEventTypeClass : query += "ClassEvent"; break;
            case Event.sEventTypeFinalExam : query += "FinalExamEvent"; break;
            case Event.sEventTypeMidtermExam : query += "MidtermExamEvent"; break;
            case Event.sEventTypeCourse : query += "CourseEvent"; break;
            case Event.sEventTypeSpecial : query += "SpecialEvent"; break;
            }
            //query += form.getEventTypes()[i];
        }
        query += ")";
        
        if (form.getEventNameSubstring()!=null && form.getEventNameSubstring().trim().length()>0) {
            query += " and upper(e.eventName) like :eventNameSubstring";
        }
        
        if (form.getEventDateFrom()!=null && form.getEventDateFrom().trim().length()>0) {
            query += " and m.meetingDate>=:eventDateFrom";
        }
        
        if (form.getEventDateTo()!=null && form.getEventDateTo().trim().length()>0) {
            query += " and m.meetingDate<=:eventDateTo";
        }
                
        if (form.getEventMainContactSubstring()!=null && form.getEventMainContactSubstring().trim().length()>0) {
            for (StringTokenizer s=new StringTokenizer(form.getEventMainContactSubstring().trim(),", ");s.hasMoreTokens();) {
                String token = s.nextToken().toUpperCase();
                query += " and (upper(e.mainContact.firstName) like '%"+token+"%' or upper(e.mainContact.middleName) like '%"+token+"%' or upper(e.mainContact.lastName) like '%"+token+"%')";
            }
        }
        
        switch (form.getMode()) {
            case EventListForm.sModeMyEvents :
                query += " and e.mainContact.externalUniqueId = :userId";
                break;
            case EventListForm.sModeAllApprovedEvents :
                query += " and m.approvedDate is not null";
                break;
            case EventListForm.sModeAllEventsWaitingApproval :
                query += " and m.approvedDate is null";
                break;
            case EventListForm.sModeEvents4Approval :
                query += " and m.approvedDate is null";
                break;
            case EventListForm.sModeAllEvents : 
                break;
        }
        
        if (form.getSponsoringOrganization()!=null && form.getSponsoringOrganization()>=0) {
            query += " and e.sponsoringOrganization.uniqueId=:sponsorOrgId";
        }
        
        if (form.getStartTime() >= 0) {
        	query += " and m.stopPeriod > " + form.getStartTime();
        }

        if (form.getStopTime() >= 0) {
        	query += " and m.startPeriod < " + form.getStopTime();
        }
        
        if (form.isDayMon() || form.isDayTue() || form.isDayWed() || form.isDayThu() || form.isDayFri() || form.isDaySat() || form.isDaySun()) {
        	String dow = "";
        	if (form.isDayMon()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "2";
        	}
        	if (form.isDayTue()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "3";
        	}
        	if (form.isDayWed()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "4";
        	}
        	if (form.isDayThu()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "5";
        	}
        	if (form.isDayFri()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "6";
        	}
        	if (form.isDaySat()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "7";
        	}
        	if (form.isDaySun()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "1";
        	}
        	if (dow.indexOf(',') >= 0)
        		query += " and " + HibernateUtil.dayOfWeek("m.meetingDate") + " in (" + dow + ")";
        	else
        		query += " and " + HibernateUtil.dayOfWeek("m.meetingDate") + " = " + dow;
        }
        
        query += " order by e.eventName, e.uniqueId";
        
        Query hibQuery = new EventDAO().getSession().createQuery(query);
        hibQuery.setFetchSize(getMaxResults() + 1);
        if (form.getMode() != EventListForm.sModeEvents4Approval)
        	hibQuery.setMaxResults(getMaxResults() + 1);
        
        if (form.getEventNameSubstring()!=null && form.getEventNameSubstring().trim().length()>0) {
            hibQuery.setString("eventNameSubstring", "%"+form.getEventNameSubstring().toUpperCase().trim()+"%");
        }
        
        if (form.getEventDateFrom()!=null && form.getEventDateFrom().trim().length()>0) {
            try {
                hibQuery.setDate("eventDateFrom", new SimpleDateFormat("MM/dd/yyyy").parse(form.getEventDateFrom()));
            } catch (ParseException ex) {
                hibQuery.setDate("eventDateFrom", new Date());
            }
        }
        
        if (form.getEventDateTo()!=null && form.getEventDateTo().trim().length()>0) {
            try {
                hibQuery.setDate("eventDateTo", new SimpleDateFormat("MM/dd/yyyy").parse(form.getEventDateTo()));
            } catch (ParseException ex) {
                hibQuery.setDate("eventDateTo", new Date());
            }
        }
        
        if (form.getSponsoringOrganization()!=null && form.getSponsoringOrganization()>=0) {
            hibQuery.setLong("sponsorOrgId", form.getSponsoringOrganization());
        }
        
        switch (form.getMode()) {
            case EventListForm.sModeMyEvents :
                hibQuery.setString("userId", form.getUserId());
                break;
            case EventListForm.sModeAllApprovedEvents :
            case EventListForm.sModeAllEventsWaitingApproval :
            case EventListForm.sModeEvents4Approval :
                break;
        }
        
        return hibQuery.setCacheable(true).list();        
    }
    
    public void htmlTableForEvents (HttpSession httpSession, EventListForm form, JspWriter outputStream){
        List events = loadEvents(form);
        int numberOfEvents = events.size();
        
        TableStream eventsTable = this.initTable(outputStream);
        if (numberOfEvents>getMaxResults() && form.getMode()!=EventListForm.sModeEvents4Approval) {
        	TableRow row = new TableRow();
        	TableCell cell = initCell(true, null, 5, false);
        	cell.addContent("Warning: There are more than " + getMaxResults() + " events matching your search criteria. Only the first " + getMaxResults() + " events are displayed. Please, redefine the search criteria in your filter.");
        	cell.setStyle("padding-bottom:10px;color:red;font-weight:bold;");
        	row.addContent(cell);
        	eventsTable.addContent(row);
        }
        if (numberOfEvents==0) {
        	TableRow row = new TableRow();
        	TableCell cell = initCell(true, null, 5, false);
        	cell.addContent("No events matching the search criteria were found.");
        	cell.setStyle("padding-bottom:10px;color:red;font-weight:bold;");
        	row.addContent(cell);
        	eventsTable.addContent(row);
        } else buildTableHeader(eventsTable, form.isAdmin() || form.isEventManager());

        ArrayList eventIds = new ArrayList();
        int idx = 0;
        for (Iterator it = events.iterator();it.hasNext();idx++){
            Event event = (Event) it.next();
            if (form.getMode()==EventListForm.sModeEvents4Approval) {
                boolean myApproval = false;
                for (Iterator j=event.getMeetings().iterator();j.hasNext();) {
                    Meeting m = (Meeting)j.next();
                    if (m.getApprovedDate()==null && m.getLocation()!=null && form.getManagingDepartments().contains(m.getLocation().getControllingDepartment())) {
                        myApproval = true; break;
                    }
                }
                if (!myApproval) continue;
            } else  if (idx==getMaxResults()) break;
            eventIds.add(event.getUniqueId());
            TreeSet<MultiMeeting> meetings = event.getMultiMeetings();
            addEventsRowsToTable(eventsTable, event, form.isAdmin() || form.isEventManager(), meetings);
            for (MultiMeeting meeting : meetings) 
                addMeetingRowsToTable(eventsTable, meeting, form.isAdmin() || form.isEventManager(), form.getDispConflicts());
        }

        eventsTable.tableComplete();
        Navigation.set(httpSession, Navigation.sInstructionalOfferingLevel, eventIds);
    }
    
    protected boolean match(String filter, String name) {
        if (filter==null || filter.trim().length()==0) return true;
        String n = (name==null?"":name).toUpperCase();
        StringTokenizer stk1 = new StringTokenizer(filter.toUpperCase(),";");
        while (stk1.hasMoreTokens()) {
            StringTokenizer stk2 = new StringTokenizer(stk1.nextToken()," ,");
            boolean match = true;
            while (match && stk2.hasMoreTokens()) {
                String token = stk2.nextToken().trim();
                if (token.length()==0) continue;
                if (token.indexOf('*')>=0 || token.indexOf('?')>=0) {
                    try {
                        String tokenRegExp = "\\s+"+token.replaceAll("\\.", "\\.").replaceAll("\\?", ".+").replaceAll("\\*", ".*")+"\\s";
                        if (!Pattern.compile(tokenRegExp).matcher(" "+n+" ").find()) match = false;
                    } catch (PatternSyntaxException e) { match = false; }
                } else if (n.indexOf(token)<0) match = false;
            }
            if (match) return true;
        }
        return false;
    }
    
    protected List<Meeting> loadMeetings(MeetingListForm form) {
        boolean conf = (form.getMode()==EventListForm.sModeAllConflictingEvents);
        
        String query = "select m from Event e inner join e.meetings m where e.class in (";
        
        if (conf) {
            query = "select m from Event e inner join e.meetings m, Meeting mx where "+
                    "mx.uniqueId!=m.uniqueId and m.meetingDate=mx.meetingDate and m.startPeriod < mx.stopPeriod and m.stopPeriod > mx.startPeriod and " +
                    "m.locationPermanentId = mx.locationPermanentId and e.class in (";
        }
        
        for (int i=0;i<form.getEventTypes().length;i++) {
            if (i>0) query+=",";
            switch (form.getEventTypes()[i].intValue()) {
            case Event.sEventTypeClass : query += "ClassEvent"; break;
            case Event.sEventTypeFinalExam : query += "FinalExamEvent"; break;
            case Event.sEventTypeMidtermExam : query += "MidtermExamEvent"; break;
            case Event.sEventTypeCourse : query += "CourseEvent"; break;
            case Event.sEventTypeSpecial : query += "SpecialEvent"; break;
            }
            //query += form.getEventTypes()[i];
        }
        query += ")";
        
        if (form.getEventNameSubstring()!=null && form.getEventNameSubstring().trim().length()>0) {
            query += " and upper(e.eventName) like :eventNameSubstring";
        }
        
        if (form.getEventDateFrom()!=null && form.getEventDateFrom().trim().length()>0) {
            query += " and m.meetingDate>=:eventDateFrom";
        }
        
        if (form.getEventDateTo()!=null && form.getEventDateTo().trim().length()>0) {
            query += " and m.meetingDate<=:eventDateTo";
        }
                
        if (form.getEventMainContactSubstring()!=null && form.getEventMainContactSubstring().trim().length()>0) {
            for (StringTokenizer s=new StringTokenizer(form.getEventMainContactSubstring().trim(),", ");s.hasMoreTokens();) {
                String token = s.nextToken().toUpperCase();
                query += " and (upper(e.mainContact.firstName) like '%"+token+"%' or upper(e.mainContact.middleName) like '%"+token+"%' or upper(e.mainContact.lastName) like '%"+token+"%')";
            }
        }
        
        if (form.getLocation()!=null && form.getLocation().trim().length() > 0){
        	query += " and ((select count(r) from Room as r where r.permanentId = m.locationPermanentId and upper(r.building.abbreviation) like :bldgSubstr and upper(r.roomNumber) like :roomSubstr) > 0 or (select count(nul) from NonUniversityLocation as nul where nul.permanentId = m.locationPermanentId and upper(nul.name) like :nameStr) > 0)) ";	       	
        }

        switch (form.getMode()) {
            case EventListForm.sModeMyEvents :
                query += " and e.mainContact.externalUniqueId = :userId";
                break;
            case EventListForm.sModeAllApprovedEvents :
                query += " and m.approvedDate is not null";
                break;
            case EventListForm.sModeAllEventsWaitingApproval :
                query += " and m.approvedDate is null";
                break;
            case EventListForm.sModeEvents4Approval :
                query += " and m.approvedDate is null";
                break;
            case EventListForm.sModeAllEvents : 
                break;
        }
        
        if (form.getSponsoringOrganization()!=null && form.getSponsoringOrganization()>=0) {
            query += " and e.sponsoringOrganization.uniqueId=:sponsorOrgId";
        }
        
        if (form.getStartTime() >= 0) {
        	query += " and m.stopPeriod > " + form.getStartTime();
        }

        if (form.getStopTime() >= 0) {
        	query += " and m.startPeriod < " + form.getStopTime();
        }
        
        if (form.isDayMon() || form.isDayTue() || form.isDayWed() || form.isDayThu() || form.isDayFri() || form.isDaySat() || form.isDaySun()) {
        	String dow = "";
        	if (form.isDayMon()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "2";
        	}
        	if (form.isDayTue()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "3";
        	}
        	if (form.isDayWed()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "4";
        	}
        	if (form.isDayThu()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "5";
        	}
        	if (form.isDayFri()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "6";
        	}
        	if (form.isDaySat()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "7";
        	}
        	if (form.isDaySun()) {
        		if (!dow.isEmpty()) dow += ",";
        		dow += "1";
        	}
        	if (dow.indexOf(',') >= 0)
        		query += " and " + HibernateUtil.dayOfWeek("m.meetingDate") + " in (" + dow + ")";
        	else
        		query += " and " + HibernateUtil.dayOfWeek("m.meetingDate") + " = " + dow;
        }
        
        Query hibQuery = new EventDAO().getSession().createQuery(query);
        hibQuery.setFetchSize(getMaxResults() + 1);
        if (form.getMode() != EventListForm.sModeEvents4Approval)
        	hibQuery.setMaxResults(getMaxResults() + 1);

        if (form.getEventNameSubstring()!=null && form.getEventNameSubstring().trim().length()>0) {
            hibQuery.setString("eventNameSubstring", "%"+form.getEventNameSubstring().toUpperCase().trim()+"%");
        }
        
        if (form.getEventDateFrom()!=null && form.getEventDateFrom().trim().length()>0) {
            try {
                hibQuery.setDate("eventDateFrom", new SimpleDateFormat("MM/dd/yyyy").parse(form.getEventDateFrom()));
            } catch (ParseException ex) {
                hibQuery.setDate("eventDateFrom", new Date());
            }
        }
        
        if (form.getEventDateTo()!=null && form.getEventDateTo().trim().length()>0) {
            try {
                hibQuery.setDate("eventDateTo", new SimpleDateFormat("MM/dd/yyyy").parse(form.getEventDateTo()));
            } catch (ParseException ex) {
                hibQuery.setDate("eventDateTo", new Date());
            }
        }
        
        if (form.getSponsoringOrganization()!=null && form.getSponsoringOrganization()>=0) {
            hibQuery.setLong("sponsorOrgId", form.getSponsoringOrganization());
        }
        
        if (form.getLocation()!=null && form.getLocation().trim().length() > 0){
	     	String bldgSubstr = null;
	     	String roomSubstr = null;
	     	String nameStr = "%" + form.getLocation().toUpperCase() + "%";
	     	int indexOfFirstSpace = form.getLocation().indexOf(' ');
	     	if(indexOfFirstSpace == 0){
	     		bldgSubstr = "%";
	     		roomSubstr = "%" + form.getLocation().substring(0, indexOfFirstSpace).toUpperCase() + "%";
	     	} else if (indexOfFirstSpace < 0) {
	     		bldgSubstr = "%" + form.getLocation().toUpperCase() + "%";
	     		roomSubstr = "%";
        	} else {
	     		bldgSubstr = "%" + form.getLocation().substring(0, indexOfFirstSpace).toUpperCase() + "%";
	     		if (indexOfFirstSpace == form.getLocation().length() - 1){
	     			roomSubstr = "%";
	     		} else {
	     			roomSubstr = "%" + form.getLocation().substring(indexOfFirstSpace + 1, form.getLocation().length()).toUpperCase() +"%";
	     		}
	     	}
        	hibQuery.setString("bldgSubstr", bldgSubstr);
        	hibQuery.setString("roomSubstr", roomSubstr);
        	hibQuery.setString("nameStr", nameStr);
        	
        }
        
        switch (form.getMode()) {
            case EventListForm.sModeMyEvents :
                hibQuery.setString("userId", form.getUserId());
                break;
            case EventListForm.sModeAllApprovedEvents :
            case EventListForm.sModeAllEventsWaitingApproval :
            case EventListForm.sModeEvents4Approval :
                break;
        }
        
        List meetings = hibQuery.setCacheable(true).list();
        if (form.getMode()==EventListForm.sModeEvents4Approval || (form.getLocation()!=null && form.getLocation().trim().length()>0)) {
            for (Iterator it = meetings.iterator();it.hasNext();){
                Meeting meeting = (Meeting) it.next();
                if (form.getMode()==EventListForm.sModeEvents4Approval) {
                    if (meeting.getApprovedDate()!=null || meeting.getLocation()==null || 
                            !form.getManagingDepartments().contains(meeting.getLocation().getControllingDepartment())) {
                        it.remove(); continue;
                    }
                }
                if (meeting.getLocation()==null || !match(form.getLocation(), meeting.getLocation().getLabel())) {
                    it.remove(); continue;
                }
            }
        }

        Comparator<Meeting> cmp = null;
        if (MeetingListForm.sOrderByName.equals(form.getOrderBy())) {
            cmp = new Comparator<Meeting>() {
                public int compare(Meeting m1, Meeting m2) {
                    int cmp = m1.getEvent().getEventName().compareToIgnoreCase(m2.getEvent().getEventName());
                    if (cmp!=0) return cmp;
                    cmp = m1.getEvent().getUniqueId().compareTo(m2.getEvent().getUniqueId());
                    if (cmp!=0) return cmp;
                    return m1.compareTo(m2);
                }
            };
        } else if (MeetingListForm.sOrderByLocation.equals(form.getOrderBy())) {
            cmp = new Comparator<Meeting>() {
                public int compare(Meeting m1, Meeting m2) {
                    String l1 = (m1.getLocation()==null?"":m1.getLocation().getLabel());
                    String l2 = (m2.getLocation()==null?"":m2.getLocation().getLabel());
                    int cmp = l1.compareToIgnoreCase(l2);
                    if (cmp!=0) return cmp;
                    return m1.compareTo(m2);
                }
            };
        } else if (MeetingListForm.sOrderByTime.equals(form.getOrderBy())) {
            cmp = new Comparator<Meeting>() {
                public int compare(Meeting m1, Meeting m2) {
                    return m1.compareTo(m2);
                }
            };
        }

        if (cmp!=null)
            Collections.sort(meetings,cmp);
        
        return meetings;
    }

    public void htmlTableForMeetings(HttpSession httpSession, MeetingListForm form, JspWriter outputStream){
        List meetings = loadMeetings(form);
        int numberOfMeetings = meetings.size();
        
        TableStream eventsTable = this.initTable(outputStream);
        if (numberOfMeetings>getMaxResults() && form.getMode()!=EventListForm.sModeEvents4Approval) {
            TableRow row = new TableRow();
            TableCell cell = initCell(true, null, 5, false);
            cell.addContent("Warning: There are more than " + getMaxResults() + " meetings matching your search criteria. Only the first " + getMaxResults() + " meetings are displayed. Please, redefine the search criteria in your filter.");
            cell.setStyle("padding-bottom:10px;color:red;font-weight:bold;");
            row.addContent(cell);
            eventsTable.addContent(row);
        }
        if (numberOfMeetings==0) {
            TableRow row = new TableRow();
            TableCell cell = initCell(true, null, 5, false);
            cell.addContent("No meetings matching the search criteria were found.");
            cell.setStyle("padding-bottom:10px;color:red;font-weight:bold;");
            row.addContent(cell);
            eventsTable.addContent(row);
        } else buildMeetingTableHeader(eventsTable, form.isAdmin() || form.isEventManager());

        int idx = 0;
        HashSet<Long> eventIdsHash = new HashSet();
        Event lastEvent = null;
        Date now = new Date();
        boolean line = MeetingListForm.sOrderByName.equals(form.getOrderBy());
        ArrayList eventIds = new ArrayList();
        
        for (Iterator it = meetings.iterator();it.hasNext();idx++){
            Meeting meeting = (Meeting) it.next();
            if (idx==getMaxResults()) break;
            if (eventIdsHash.add(meeting.getEvent().getUniqueId())) eventIds.add(meeting.getEvent().getUniqueId());
            addMeetingRowsToTable(eventsTable, meeting, form.isAdmin() || form.isEventManager(), lastEvent, now, line, form.getDispConflicts());
            lastEvent = meeting.getEvent();
        }

        eventsTable.tableComplete();
        Navigation.set(httpSession, Navigation.sInstructionalOfferingLevel, eventIds);
    }
        
    protected TableStream initTable(JspWriter outputStream){
    	TableStream table = new TableStream(outputStream);
        table.setWidth("100%");
        table.setBorder(0);
        table.setCellSpacing(0);
        table.setCellPadding(3);
        table.tableDefComplete();
        return(table);
    }            
   
}
