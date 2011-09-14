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
package org.unitime.timetable.gwt.client.sectioning;

import java.util.ArrayList;
import java.util.Iterator;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.widgets.ImageLink;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.resources.StudentSectioningResources;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class TimeGrid extends Composite {
	public static final StudentSectioningResources RESOURCES =  GWT.create(StudentSectioningResources.class);
	public static final StudentSectioningMessages MESSAGES = GWT.create(StudentSectioningMessages.class);
	public static final StudentSectioningConstants CONSTANTS = GWT.create(StudentSectioningConstants.class);

	private VerticalPanel iContainer;
	private SimplePanel iScrollPanel;
	private AbsolutePanel iPanel;
	private AbsolutePanel iGrid;
	private HorizontalPanel iHeader, iDock;
	private SimplePanel[] iSeparators = new SimplePanel[8];
	private SimplePanel iWorkingHours;
	private ImageLink iCalendar;
	
	private ArrayList<ArrayList<Meeting>> iMeetings = new ArrayList<ArrayList<Meeting>>();
	@SuppressWarnings("unchecked")
	private ArrayList<Meeting>[][] iMeetingTable = new ArrayList[7][24 * 60 / 5];
	private ArrayList<String[]> iColor = new ArrayList<String[]>();
	
	private int iCellWidth = 150;
	private int iNrDays = 5;
	private int iStart = 0;
	private int iEnd = 24;
	private boolean iPrint = false;
	
	private ArrayList<MeetingClickHandler> iMeetingClickHandlers = new ArrayList<MeetingClickHandler>();
	private ArrayList<PinClickHandler> iPinClickHandlers = new ArrayList<PinClickHandler>();
	private ArrayList<ClassAssignmentInterface.ClassAssignment> iClasses = new ArrayList<ClassAssignmentInterface.ClassAssignment>();
	private ArrayList<BusyPanel> iBusy = new ArrayList<BusyPanel>();
	
	public TimeGrid() {
		this(5, 900 / 5, false, 0, 24);
	}
	
	public TimeGrid(int nrDays, int cellWidth, boolean print, int start, int end) {
		iNrDays = nrDays;
		iCellWidth = cellWidth;
		iStart = start;
		iEnd = end;
		iPrint = print;
		
		iContainer = new VerticalPanel();
		iContainer.setStyleName("unitime-TimeGrid");
		iHeader = new HorizontalPanel();

		iDock = new HorizontalPanel();
		
		iPanel = new AbsolutePanel();
		AbsolutePanel times = new AbsolutePanel();
		
		for (int i = -1; i < iNrDays; i++) {
			SimplePanel sp = new SimplePanel();
			sp.setStyleName("header-time-interval");
			if (i < 0) {
				sp.setWidth("30px");
				iCalendar = new ImageLink();
				iCalendar.setImage(new Image(RESOURCES.calendar()));
				iCalendar.setTarget(null);
				iCalendar.setTitle(MESSAGES.exportICalendar());
				if (!iPrint)
					sp.setWidget(iCalendar);
			} else {
				sp.setWidth(String.valueOf(iCellWidth));
				sp.setWidget(new Label(CONSTANTS.longDays()[i]));
			}
			iHeader.add(sp);
		}
		
		iPanel.setSize(String.valueOf(iNrDays * iCellWidth + (iPrint ? 5 : 0)), String.valueOf(50 * (iEnd - iStart)));
		times.setSize("30", String.valueOf(50 * (iEnd - iStart)));
		iGrid = new AbsolutePanel();
		iGrid.setSize("100%", "100%");
        
		iPanel.setStyleName("bottom-line");
		
		iWorkingHours = new SimplePanel();
		iWorkingHours.setStyleName("working-hours");
		iWorkingHours.setSize(String.valueOf(2 + iCellWidth * 5), String.valueOf(500));
		iGrid.add(iWorkingHours, 0, 375 - (50 * iStart));
		
		for (int i = iStart; i < iEnd; i++) {
			
			//create major interval
			SimplePanel sp1 = new SimplePanel();
			sp1.setStyleName("major-time-interval");
			iGrid.add(sp1, 0, 50 * (i - iStart));

			SimplePanel sp3 = new SimplePanel();
			sp3.setStyleName("dummy-time-interval");
			Label title = new Label(CONSTANTS.useAmPm() ? (i == 0 ? "12am": i <= 11 ? i + "am" : i == 12 ? "12pm" : (i-12) + "pm") : String.valueOf(i));
			sp3.setWidget(title);
			times.add(sp3, 0, 50 * (i - iStart));

			SimplePanel sp2 = new SimplePanel();
			sp2.setStyleName("minor-time-interval");
			iGrid.add(sp2, 0, 50 * (i - iStart) + 25);
			
			SimplePanel sp4 = new SimplePanel();
			sp4.setStyleName("dummy-time-interval");
			times.add(sp4, 0, 50 * (i - iStart) + 25);			
		}

		for (int day = 0; day < iNrDays + (iPrint ? 1 : 0); day++) {
			iSeparators[day] = new SimplePanel();
			iSeparators[day].setStyleName("day-separator");
			iGrid.add(iSeparators[day], iCellWidth * day, 0);
		}
		
        iPanel.add(iGrid);
        
        iDock.add(times);
        iDock.add(iPanel);
        iContainer.add(iHeader);
        iHeader.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        
        if (!iPrint) {
    		iScrollPanel = new ScrollPanel();
        	iScrollPanel.setStyleName("calendar-scroll");
        } else {
        	iScrollPanel = new SimplePanel();
        }
        
    	iScrollPanel.add(iDock);
    	iContainer.add(iScrollPanel);
    	iScrollPanel.setWidth(String.valueOf(iNrDays * iCellWidth + 30 + (iPrint ? 5 : ToolBox.getScrollBarWidth())));

        initWidget(iContainer);
	}
	
	public void addFreeTime(CourseRequestInterface.FreeTime ft) {
		for (int day: ft.getDays())
			addBusy(CONSTANTS.freePrefix() + ft.toString(CONSTANTS.shortDays(), CONSTANTS.useAmPm()), day, ft.getStart(), ft.getLength());
	}
	
	private void addBusy(String text, int day, int start, int length) {
		iBusy.add(new BusyPanel(text, day, start, length));
	}
	
	public void setCalendarUrl(String url) {
		iCalendar.setUrl(url);
	}
	
	public Widget getPrintWidget() {
		int firstHour = firstSlot() / 12;
		int lastHour = 1 + lastSlot() / 12;
		TimeGrid tg = new TimeGrid(iNrDays, iCellWidth, true, (firstHour < 7 ? firstHour : 7), (lastHour > 18 ? lastHour : 18));
		int i = 0;
		for (ClassAssignmentInterface.ClassAssignment c: iClasses)
			for (Meeting m : tg.addClass(c, i++)) {
				m.hidePin();
				m.setDummy();
				m.addStyleName("meeting-selected-noshadow");
			}
		for (BusyPanel busy: iBusy)
			tg.addBusy(busy.getTitle(), busy.getDay(), busy.getStart(), busy.getLength());
		return tg;
	}
	
	public void setNrDays(int days) {
		if (iNrDays == days) return;
		iNrDays = days;
		iCellWidth = 900 / days;
		iPanel.setWidth(String.valueOf(iNrDays * iCellWidth));
		iWorkingHours.setWidth(String.valueOf(2 + iCellWidth * (iNrDays < 5 ? iNrDays : 5)));
        iScrollPanel.setWidth(String.valueOf(iNrDays * iCellWidth + 30 + ToolBox.getScrollBarWidth()));
		for (int day = 0; day < 7; day++) {
			if (day < days) {
				if (iSeparators[day] == null) {
					iSeparators[day] = new SimplePanel();
					iSeparators[day].setStyleName("day-separator");
					iGrid.add(iSeparators[day], iCellWidth * day, 0);
				} else {
					DOM.setStyleAttribute(iSeparators[day].getElement(), "left", String.valueOf(iCellWidth * day));
					iSeparators[day].setVisible(true);
				}
			} else {
				if (iSeparators[day] != null) 
					iSeparators[day].setVisible(false);
			}
		}
		iHeader.clear();
		for (int i = -1; i < iNrDays; i++) {
			SimplePanel sp = new SimplePanel();
			sp.setStyleName("header-time-interval");
			if (i < 0)
				sp.setWidth("30px");
			else {
				sp.setWidth(String.valueOf(iCellWidth));
				sp.setWidget(new Label(CONSTANTS.longDays()[i]));
			}
			iHeader.add(sp);
		}
		for (ArrayList<Meeting> meetings: iMeetings)
			for (Meeting meeting: meetings) 
				meeting.move();
		for (BusyPanel busy: iBusy)
			busy.move();
	}
	
	public int firstSlot() {
		for (int slot = 0; slot < 24 * 60 / 5; slot++) {
			for (int day = 0; day < 7; day++) {
				if (iMeetingTable[day][slot] != null && !iMeetingTable[day][slot].isEmpty())
					return slot;
			}
		}
		return 24 * 60 / 5 + 1;
	}
	
	public int lastSlot() {
		for (int slot = 24 * 60 / 5 - 1; slot >= 0; slot--) {
			for (int day = 0; day < 7; day++) {
				if (iMeetingTable[day][slot] != null && !iMeetingTable[day][slot].isEmpty())
					return slot;
			}
		}
		return 0;
	}

	public void shrink() {
		boolean hasSat = false, hasSun = false;
		for (int slot = 0; slot < 24 * 60 / 5; slot++) {
			if (iMeetingTable[5][slot] !=null && !iMeetingTable[5][slot].isEmpty()) hasSat = true;
			if (iMeetingTable[6][slot] !=null && !iMeetingTable[6][slot].isEmpty()) hasSun = true;
		}
		for (BusyPanel busy: iBusy) {
			if (busy.getDay() == 5) hasSat = true;
			if (busy.getDay() == 6) hasSun = true;
		}
		if (!hasSat && !hasSun) setNrDays(5);
		else if (!hasSun) setNrDays(6);
		else setNrDays(7);
	}
	
	public String getWidth() {
		return String.valueOf(iNrDays * iCellWidth + 30 + ToolBox.getScrollBarWidth());
	}
	
	public void scrollDown() {
        iScrollPanel.setWidth(String.valueOf(iNrDays * iCellWidth + 30 + ToolBox.getScrollBarWidth()));
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				if (iScrollPanel instanceof ScrollPanel)
					((ScrollPanel)iScrollPanel).setVerticalScrollPosition(350);
			}
		});
	}
	
	public void clear() {
		for (ArrayList<Meeting> meetings: iMeetings)
			for (Meeting meeting: meetings)
				iPanel.remove(meeting);
		iMeetings.clear();
		for (int i = 0; i < iMeetingTable.length; i++)
			for (int j = 0 ; j < iMeetingTable[i].length; j++)
				if (iMeetingTable[i][j] != null) iMeetingTable[i][j].clear();
		iColor.clear();
		iClasses.clear();
		for (BusyPanel busy: iBusy)
			busy.remove();
		iBusy.clear();
	}
	
	public String getColor(ClassAssignmentInterface.ClassAssignment clazz) {
		if (clazz.isFreeTime()) return CONSTANTS.freeTimeColor();
		for (String[] pair: iColor)
			if (pair[0].equals(clazz.getCourseId().toString())) return pair[1];
		String color = CONSTANTS.meetingColors()[iColor.size() % CONSTANTS.meetingColors().length];
		iColor.add(new String[] {clazz.getCourseId().toString(), color});
		return color;
	}
	
	protected Meeting addMeeting(int index, int day, int startSlot, int length, String name, ArrayList<String> note, String title, String color, boolean pinned, ArrayList<Meeting> meetings) {
		int col = -1;
		for (int i = 0; i < length; i++) {
			if (iMeetingTable[day][startSlot + i] != null) {
				for (Iterator<Meeting> j = iMeetingTable[day][startSlot + i].iterator(); j.hasNext(); ) {
					Meeting m = j.next();
					if (m.getColumn() > col) col = m.getColumn();
				}
			}
		}
		col += 1;
		int cols = col + 1;
		if (col > 0) {
			boolean used[] = new boolean[cols - 1];
			for (int i = 0; i < cols - 1; i++) used[i] = false;
			for (int i = 0; i < length; i++) {
				if (iMeetingTable[day][startSlot + i] != null) {
					for (Iterator<Meeting> j = iMeetingTable[day][startSlot + i].iterator(); j.hasNext(); ) {
						Meeting m = j.next();
						used[m.getColumn()] = true;
					}
				}
			}
			for (int i = 0; i < cols - 1; i++)
				if (!used[i]) {col = i; cols--; break; }
		}
        Meeting meeting = new Meeting(index, name, note, day, startSlot, length, col, cols, pinned, meetings);
        meeting.addStyleName(color);

        meeting.setTitle(title);
        iPanel.add(meeting);
		for (int i = 0; i < length; i++) {
			if (iMeetingTable[day][startSlot + i] == null) iMeetingTable[day][startSlot + i] = new ArrayList<Meeting>();
			for (Iterator<Meeting> j = iMeetingTable[day][startSlot + i].iterator(); j.hasNext(); ) {
				Meeting m = j.next();
				m.setNrColumns(cols);
			}
			iMeetingTable[day][startSlot + i].add(meeting);
		}
        return meeting;
	}
	
	public ArrayList<Meeting> getMeetings(int index) {
		for (ArrayList<Meeting> meetings: iMeetings)
			for (Meeting meeting: meetings)
				if (meeting.getIndex() == index) return meetings;
		return null;
	}
	
	public ArrayList<Meeting> addClass(ClassAssignmentInterface.ClassAssignment row, int index) {
		if (!row.isAssigned()) return null;
		iClasses.add(row);
		String name = (row.isFreeTime() ? MESSAGES.freeTimeCourse() + " " + MESSAGES.freeTimeSubject() :
			row.getSubject() + " " + row.getCourseNbr() + " " + row.getSubpart());
		String longName = name + (row.isFreeTime() ? "" : " " + row.getSection());
		String time = row.getDaysString(CONSTANTS.shortDays()) + " " + row.getStartString(CONSTANTS.useAmPm()) + " - " + row.getEndString(CONSTANTS.useAmPm());
		ArrayList<String> notes = new ArrayList<String>();
		ArrayList<String> notesNoHtml = new ArrayList<String>();
		if (row.hasRoom()) {
			notes.add(row.getRooms(", "));
			notesNoHtml.add(row.getRooms(", "));
		}
		String color = getColor(row);
		if (row.hasInstructors()) {
			if (!iPrint) {
				String instructors = "";
				for (int i = 0; i < row.getInstructors().size(); i++) {
					String email = (row.hasInstructorEmails() && i < row.getInstructorEmails().size() ? row.getInstructorEmails().get(i) : null);
					if (email != null && !email.isEmpty()) {
						ImageLink il = new ImageLink(new Image(RESOURCES.email()), "mailto:" + email);
						il.setTitle(MESSAGES.sendEmail(row.getInstructors().get(i)));
						il.setStyleName("A.unitime-SimpleLink");
						il.getElement().getStyle().setMarginRight(1, Unit.PX);
						instructors += DOM.toString(il.getElement()) + row.getInstructors().get(i) + (i + 1 < row.getInstructors().size() ? ", " : "");
					} else {
						instructors += row.getInstructors().get(i) + (i + 1 < row.getInstructors().size() ? ", " : "");
					}
				}
				notes.add(instructors);
				notesNoHtml.add(row.getInstructors(", "));
			} else {
				notes.add(row.getInstructors(", "));
				notesNoHtml.add(row.getInstructors(", "));
			}
		}
		if (row.hasDatePattern()) {
			notes.add(row.getDatePattern());
			notesNoHtml.add(row.getDatePattern());
		}
		String note = "";
		for (String n: notesNoHtml) {
			if (n == null || n.isEmpty()) continue;
			if (!note.isEmpty()) note += ", ";
			note += n;
		}
		String title = longName + " (" + time + ", " + note + ")";
		if (note.isEmpty()) {
			title = longName + " (" + time + ")";
			note = time;
		}
		final ArrayList<Meeting> meetings = new ArrayList<Meeting>();
		for (int day: row.getDays()) {
			meetings.add(addMeeting(index, day, row.getStart(), row.getLength(), name, notes, title, color, row.isPinned(), meetings));
		}
		if (row.isPinned())
			for (Meeting m: meetings) m.setPinned(true);
		if (row.hasDistanceConflict()) {
			for (Meeting m: meetings) {
				Widget dist = new Image(RESOURCES.distantConflict());
				if (iPrint) { 
					dist = new Label(MESSAGES.distanceConflict(row.getBackToBackDistance()));
					dist.setStyleName("label");
					// FIXME: Workaround about inability of image bundle images to be printed
					// dist = new Image(GWT.getHostPageBaseURL() + "roadrunner16.png");
				}
				dist.setTitle(MESSAGES.backToBackDistance(row.getBackToBackRooms(), row.getBackToBackDistance()));
				m.addIcon(dist);
			}
		}
		if (row.isOfHighDemand() && !iPrint) {
			for (Meeting m: meetings) {
				Image highDemand = new Image(RESOURCES.highDemand());
				highDemand.setTitle(MESSAGES.highDemand(row.getExpected(), row.getAvailableLimit()));
				m.addIcon(highDemand);
			}
		}
		if (row.isSaved() && !iPrint) {
			for (Meeting m: meetings) m.setSaved(row.isSaved());
		}
		iMeetings.add(meetings);
		return meetings;
	}
	
	public interface MeetingClickHandler {
		public void onMeetingClick(MeetingClickEvent event);
	}
	
	public class MeetingClickEvent {
		private Meeting iMeeting;
		MeetingClickEvent(Meeting meeting) {
			iMeeting = meeting;
		}
		public int getRowIndex() { return iMeeting.getIndex(); }
	}
	
	public void addMeetingClickHandler(MeetingClickHandler h) {
		iMeetingClickHandlers.add(h);
	}

	public interface PinClickHandler {
		public void onPinClick(PinClickEvent event);
	}
	
	public class PinClickEvent {
		private Meeting iMeeting;
		PinClickEvent(Meeting meeting) {
			iMeeting = meeting;
		}
		public int getRowIndex() { return iMeeting.getIndex(); }
		public boolean isPinChecked() { return iMeeting.getPinned(); }
	}
	
	public void addPinClickHandler(PinClickHandler h) {
		iPinClickHandlers.add(h);
	}

	public class Meeting extends AbsolutePanel {
		private int iIndex, iColumn, iDay, iNrColumns;
		private int iLeft, iWidth;
		private boolean iPinned = false;
		private Image iPin;
		private ArrayList<Meeting> iMeetings;
		private HorizontalPanel iHeaderPanel;
		private Image iSaved;
		private boolean iDummy = false;
		
		private Meeting(int index, String name, ArrayList<String> note, int day, int start, int length, int column, int nrColumns, boolean pinned, ArrayList<Meeting> meetings) {
			super();
			iMeetings = meetings;
			iIndex = index;
			iDay = day;
			iColumn = column;
			iNrColumns = nrColumns;
	    	iHeaderPanel = new HorizontalPanel();
	        iHeaderPanel.setStylePrimaryName("header");
	        iPinned = pinned;
	        iPin = new Image(pinned ? RESOURCES.locked() : RESOURCES.unlocked());
	        iHeaderPanel.add(iPin);
	        Label nameLabel = new Label(name);
	        nameLabel.setStyleName("label");
	        iHeaderPanel.add(nameLabel);
	        iHeaderPanel.setCellHorizontalAlignment(iPin, HasHorizontalAlignment.ALIGN_CENTER);
	        iSaved = new Image(RESOURCES.saved());
	        iSaved.setVisible(false);
	        iHeaderPanel.add(iSaved);
	        iPin.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					boolean pinned = !getPinned();
					for (Meeting m: iMeetings) m.setPinned(pinned);
					PinClickEvent e = new PinClickEvent(Meeting.this);
					for (PinClickHandler h: iPinClickHandlers)
						h.onPinClick(e);
					event.stopPropagation();
				}
			});
	        SimplePanel mbot = new SimplePanel();
	        mbot.setStylePrimaryName("footer");
	        String notes = "";
	        String delim = "<br>";
	        if (note.size() > 2 && length <= 14) delim = ", ";
	        if (note.size() > 3 && length <= 20) delim = ", ";
	        for (String n: note) {
	        	if (n == null || n.isEmpty()) continue;
	        	if (notes.length() > 0) notes += delim;
	        	notes += "<span  style=\"white-space: nowrap\">" + n + "</span>";
	        }
	        mbot.add(new HTML(notes));
	        setStylePrimaryName("meeting");
	        add(iHeaderPanel);
	        add(mbot);
	        iWidth = (iCellWidth - 6) / nrColumns + (column + 1 != nrColumns && nrColumns > 1 ? -3 : 0);
	        setSize(String.valueOf(iWidth), String.valueOf(125 * length / 30 - 3));
	        DOM.setStyleAttribute(getElement(), "position", "absolute");
	        iLeft = 4 + iCellWidth * day + column * (iCellWidth - 6) / nrColumns;
			DOM.setStyleAttribute(getElement(), "left", String.valueOf(iLeft));
			DOM.setStyleAttribute(getElement(), "top", String.valueOf(1 + 125 * start / 30 - 50 * iStart));

			sinkEvents(Event.ONCLICK);
			sinkEvents(Event.ONMOUSEOVER);
			sinkEvents(Event.ONMOUSEMOVE);
			sinkEvents(Event.ONMOUSEOUT);
		}
		
		public void addIcon(Widget image) {
			iHeaderPanel.add(image);
		}
		
		public void setSaved(boolean saved) {
			iSaved.setVisible(saved);
		}
		
		public void setPinned(boolean pinned) {
			iPinned = pinned;
			iPin.setResource(pinned ? RESOURCES.locked() : RESOURCES.unlocked());
		}
		
		public void hidePin() {
			iHeaderPanel.remove(iPin);
		}
		
		public boolean getPinned() {
			return iPinned;
		}
		
		public void onBrowserEvent(Event event) {
			if (iDummy) return;
		    com.google.gwt.user.client.Element target = DOM.eventGetTarget(event);
		    boolean anchor = false;
		    for (; target != null; target = DOM.getParent(target)) {
		    	String tag = DOM.getElementProperty(target, "tagName");
		    	if (tag.equalsIgnoreCase("a")) {
		    		anchor = true;
		    		break;
		    	} else if (tag.equalsIgnoreCase("div")) {
		    		break;
		    	}
		    }
			EventTarget related = event.getRelatedEventTarget();
		    switch (DOM.eventGetType(event)) {
			case Event.ONCLICK:
				setWidth(String.valueOf(iWidth));
				DOM.setStyleAttribute(getElement(), "zIndex", "");
				DOM.setStyleAttribute(getElement(), "left", String.valueOf(iLeft));
				for (Meeting meeting: iMeetings)
					meeting.removeStyleName("meeting-selected");
				if (!anchor) {
					MeetingClickEvent e = new MeetingClickEvent(Meeting.this);
					for (MeetingClickHandler h: iMeetingClickHandlers)
						h.onMeetingClick(e);
				}
				break;
			case Event.ONMOUSEOVER:
		        if (related == null || !getElement().isOrHasChild((Element)related.cast())) {
					setWidth(String.valueOf(iCellWidth - 6));
					getElement().getStyle().setZIndex(1000);
					DOM.setStyleAttribute(getElement(), "left", String.valueOf(4 + iCellWidth * iDay));
					for (Meeting meeting: iMeetings)
						meeting.addStyleName("meeting-selected");
		        }
				break;
			case Event.ONMOUSEOUT:
		        if (related == null || !getElement().isOrHasChild((Element)related.cast())) {
					setWidth(String.valueOf(iWidth));
					DOM.setStyleAttribute(getElement(), "zIndex", "");
					DOM.setStyleAttribute(getElement(), "left", String.valueOf(iLeft));
					for (Meeting meeting: iMeetings)
						meeting.removeStyleName("meeting-selected");
		        }
				break;
			case Event.ONMOUSEMOVE:
				int relativeX = event.getClientX() - getElement().getAbsoluteLeft() + getElement().getScrollLeft() + getElement().getOwnerDocument().getScrollLeft();
				if (relativeX < iLeft - 6 - iDay * iCellWidth || relativeX > iLeft - 2 - iDay * iCellWidth + iWidth) {
					setWidth(String.valueOf(iWidth));
					DOM.setStyleAttribute(getElement(), "zIndex", "");
					DOM.setStyleAttribute(getElement(), "left", String.valueOf(iLeft));
				}
				break;
			}
		    super.onBrowserEvent(event);
		}
		
		public int getIndex() {
			return iIndex;
		}
		
		public void setNrColumns(int nrColumns) {
			if (nrColumns == iNrColumns) return;
			iNrColumns = nrColumns;
			move();
		}
		
		public void move() {
	        iWidth = (iCellWidth - 6) / iNrColumns + (iColumn + 1 != iNrColumns && iNrColumns > 1 ? -3 : 0);
	        iLeft = 4 + iCellWidth * iDay + iColumn * (iCellWidth - 6) / iNrColumns;
			setWidth(String.valueOf(iWidth));
			DOM.setStyleAttribute(getElement(), "left", String.valueOf(iLeft));
		}
		
		public void setTitle(String title) {
			super.setTitle(title);
		}
		
		public int getColumn() {
			return iColumn;
		}
		
		public int getDay() {
			return iDay;
		}
		
		public int getNrColumns() {
			return iNrColumns;
		}
		
		public void add(Widget w) {
            super.add(w, getElement());
        }
		
		public void setDummy() {
			iDummy = true;
		}
	}
	
	private class BusyPanel extends SimplePanel {
		private int iDayOfWeek, iStartSlot, iLength;
		private String iText;
		
		public BusyPanel(String text, int dayOfWeek, int startSlot, int length) {
			super();
			iText = text;
			iDayOfWeek = dayOfWeek;
			iStartSlot = startSlot;
			iLength = length;
			if (iText != null || !iText.isEmpty()) {
				setTitle(iText);
				boolean empty = true;
				for (int i = 0; i < 3; i++) {
					if (iMeetingTable[iDayOfWeek].length <= iStartSlot + i) { empty = false; break; }
					if (iMeetingTable[iDayOfWeek][iStartSlot + i] != null && !iMeetingTable[iDayOfWeek][iStartSlot + i].isEmpty()) {
						empty = false; break;
					}
				}
				if (empty) {
					HTML widget = new HTML(text, false);
					widget.setStyleName("text");
					setWidget(widget);
				}
			}
			setStyleName("busy");
			setSize(String.valueOf(iCellWidth + (iPrint ? 3 : iDayOfWeek + 1 < iNrDays ? 3 : 0)), String.valueOf(125 * iLength / 30));
			iGrid.insert(this, iCellWidth * iDayOfWeek, 125 * iStartSlot / 30 - 50 * iStart, 1);
		}
		
		public void move() {
			setWidth(String.valueOf(iCellWidth + (iPrint ? 3 : iDayOfWeek + 1 < iNrDays ? 3 : 0)));
			DOM.setStyleAttribute(getElement(), "left", String.valueOf(iCellWidth * iDayOfWeek));
		}
		
		public int getDay() { return iDayOfWeek; }
		public int getStart() { return iStartSlot; }
		public int getLength() { return iLength; }
		
		public void remove() {
			iGrid.remove(this);
		}
		
	}
}
