/*
 * UniTime 3.0 (University Timetabling Application)
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
package org.unitime.timetable.gwt.client.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.unitime.timetable.gwt.client.widgets.ImageLink;
import org.unitime.timetable.gwt.client.widgets.P;
import org.unitime.timetable.gwt.resources.GwtConstants;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.resources.GwtResources;
import org.unitime.timetable.gwt.shared.EventInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ContactInterface;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ResourceInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ResourceType;
import org.unitime.timetable.gwt.shared.EventInterface.SelectionInterface;
import org.unitime.timetable.gwt.shared.EventInterface.WeekInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class TimeGrid extends Composite {
	public static final GwtResources RESOURCES =  GWT.create(GwtResources.class);
	public static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	public static final GwtConstants CONSTANTS = GWT.create(GwtConstants.class);
	private static DateTimeFormat sDateFormat = DateTimeFormat.getFormat(CONSTANTS.eventDateFormat());

	private P iContainer;
	private ScrollPanel iScrollPanel;
	private P iPanel;
	private P iGrid;
	private P iHeader, iDock;
	private P iVLines;
	private P iSelections;
	private P[] iSeparators = new P[7];
	private P iWorkingHours;
	private P iTimes;
	private ImageLink iCalendar;
	private SelectionLayer iSelectionLayer;
	private List<SelectionInterface> iAllSelections = new ArrayList<SelectionInterface>();
	private ResourceType iResourceType;
	private boolean iShowShadows;
	
	private ArrayList<ArrayList<Meeting>> iMeetings = new ArrayList<ArrayList<Meeting>>();
	@SuppressWarnings("unchecked")
	private ArrayList<Meeting>[][] iMeetingTable = new ArrayList[7][24 * 60 / 5];
	
	private int iCellWidth = 150;
	private int iCellHeight = 60;
	private int[] iDays = new int[] {0, 1, 2, 3, 4};
	private int iStart = 0;
	private int iEnd = 24;
	private int iTotalNrColumns = 0;
	
	private ArrayList<MeetingClickHandler> iMeetingClickHandlers = new ArrayList<MeetingClickHandler>();
	private HashMap<Long, String> iColors = new HashMap<Long, String>();
	
	private List<ResourceInterface> iRoomResources = null;
	private List<WeekInterface> iSelectedWeeks = null;
	
	private List<HandlerRegistration> iHandlerRegistrations = new ArrayList<HandlerRegistration>();
	
	public static enum Mode {
		FILLSPACE,
		PROPORTIONAL,
		OVERLAP
	}
	
	private Mode iMode = Mode.FILLSPACE;
	
	public TimeGrid() {
		this(new HashMap<Long, String>(), new int[] {0, 1, 2, 3, 4}, (int) (0.9 * Window.getClientWidth() / 5), false, false, 0, 24);
	}
	
	private List<P> iDayLabels = new ArrayList<P>();
	
	public TimeGrid(HashMap<Long, String> colors, int[] days, int cellWidth, boolean print, boolean scroll, int start, int end) {
		iColors = colors;
		iDays = days;
		iCellWidth = cellWidth;
		iStart = start;
		iEnd = end;
		iShowShadows = !print;
		
		iContainer = new P("unitime-TimeGrid");
		iContainer.setSize(40 + iDays.length * iCellWidth, scroll ? 575 : 25 + iCellHeight * (iEnd - iStart));
		
		iHeader = new P("calendar-header");
		iHeader.setWidth(iDays.length * iCellWidth);

		iDock = new P("calendar-dock");
		iDock.setHeight(iCellHeight * (iEnd - iStart) + 5);
		
		iPanel = new P("calendar-panel");
		iPanel.setSize(iDays.length * iCellWidth + 5, iCellHeight * (iEnd - iStart));
		iTimes = new P("calendar-times");
		iTimes.setHeight(iCellHeight * (iEnd - iStart));

		iCalendar = new ImageLink();
		iCalendar.setImage(new Image(RESOURCES.calendar()));
		iCalendar.setTarget(null);
		iCalendar.setTitle(MESSAGES.exportICalendar());
		iCalendar.addStyleName("calendar");
		if (!print)
			iContainer.add(iCalendar);
		
		for (int i = 0; i < iDays.length; i++) {
			P sp = new P("header-time-interval");
			sp.setWidth(iCellWidth);
			sp.setText(CONSTANTS.longDays()[iDays[i]]);
			iHeader.add(sp, i * iCellWidth, 0);
			iDayLabels.add(sp);
		}
		
		iGrid = new P("calendar-grid");
		
		iWorkingHours = new P("working-hours");
		iWorkingHours.setSize(iCellWidth * nrWorkDays(), iCellHeight * 10);
		iPanel.add(iWorkingHours, iCellWidth * firstWorkDay(), 15 * iCellHeight / 2 - (iCellHeight * iStart));
		
        for (int i = iStart; i < iEnd; i++) {
			
			//create major interval
			P sp1 = new P("major-time-interval");
			iGrid.add(sp1, 0, iCellHeight * (i - iStart));

			P sp3 = new P("dummy-time-interval");
			sp3.setText(CONSTANTS.useAmPm() ? (i == 0 ? "12am": i <= 11 ? i + "am" : i == 12 ? "12pm" : (i-12) + "pm") : String.valueOf(i));
			iTimes.add(sp3, 0, iCellHeight * (i - iStart));

			P sp2 = new P("minor-time-interval");
			iGrid.add(sp2, 0, iCellHeight * (i - iStart) + iCellHeight / 2);
			
			P sp4 = new P("dummy-time-interval");
			iTimes.add(sp4, 0, iCellHeight * (i - iStart) + iCellHeight / 2);			
		}

		for (int day = 0; day < iDays.length; day++) {
			iSeparators[day] = new P("day-separator");
			iGrid.add(iSeparators[day], iCellWidth * day, 0);
		}
		P lastSeparator = new P("day-separator");
		lastSeparator.getElement().getStyle().setRight(0, Unit.PX);
		lastSeparator.getElement().getStyle().setTop(0, Unit.PX);
		lastSeparator.getElement().getStyle().setPosition(Position.ABSOLUTE);
		iGrid.add(lastSeparator);
		
        iPanel.add(iGrid, 0, 0);
        
        iDock.add(iTimes, 0, 0);
        iDock.add(iPanel, 30, 0);
        
        iContainer.add(iHeader, 30, 0);
        
        iVLines = new P("calendar-grid");
        iPanel.add(iVLines, 0, 0);
        
        iSelections = new P("calendar-grid");
        iPanel.add(iSelections, 0, 0);
        
        iSelectionLayer = new SelectionLayer();
        iSelectionLayer.setVisible(false);
        iPanel.add(iSelectionLayer, 0, 0);
        
        if (scroll) {
    		iScrollPanel = new ScrollPanel(iDock);
        	iScrollPanel.setStyleName("calendar-scroll");
        	iContainer.add(iScrollPanel, 0, 20);
        } else {
        	iContainer.add(iDock, 0, 20);
        }

        initWidget(iContainer);
        
        if (!print) {
            iHandlerRegistrations.add(RootPanel.get().addDomHandler(new MouseUpHandler() {
    			@Override
    			public void onMouseUp(MouseUpEvent event) {
    				if (iSelectionLayer.isVisible())
    					iSelectionLayer.onMouseUp();
    			}
    		}, MouseUpEvent.getType()));
        }
	}
	
	public void destroy() {
		iMeetingClickHandlers.clear();
		for (HandlerRegistration reg: iHandlerRegistrations)
			reg.removeHandler();
	}
	
	public void setResourceType(ResourceType resourceType) { iResourceType = resourceType; }
	public ResourceType getResourceType() { return iResourceType; }
	
	public boolean isShowVerticalSplit() {
		return getMode() == Mode.OVERLAP && (getResourceType() != ResourceType.PERSON || !isSingleWeek());
	}

	public boolean isAllowSelection() {
		return getMode() == Mode.OVERLAP && (isSingleRoom() || isSingleWeek()) && (getResourceType() != ResourceType.PERSON);
	}
	
	private boolean isVerticalSplitByWeek() {
		return isSingleRoom() || getResourceType() == ResourceType.PERSON;
	}

	public List<WeekInterface> getSelectedWeeks() { return iSelectedWeeks; }
	public void setSelectedWeeks(List<WeekInterface> weeks) {
		iSelectedWeeks = weeks;
	}
	public boolean isSingleWeek() { return iSelectedWeeks != null && iSelectedWeeks.size() == 1; }
	
	public void setRoomResources(List<ResourceInterface> roomResources) {
		iRoomResources = roomResources;
	}
	public List<ResourceInterface> getRoomResources() { return iRoomResources; }
	public boolean isSingleRoom() { return iRoomResources != null && iRoomResources.size() == 1; }
	
	public void setMode(Mode mode) {
		iMode = mode;
		showSelections();
	}
	public Mode getMode() { return iMode; }
	
	public void setCalendarUrl(String url) {
		if (url == null) {
			iCalendar.setVisible(false);
		} else {
			iCalendar.setUrl(url);
			iCalendar.setVisible(true);
		}
	}
	
	public String getCalendarUrl() {
		return (iCalendar.isVisible() ? iCalendar.getUrl() : null);
	}
	
	public TimeGrid getPrintWidget() {
		int firstHour = firstSlot() / 12;
		int lastHour = (11 + lastSlot()) / 12;
		TimeGrid tg = new TimeGrid(iColors, iDays, (int) (1000 / iDays.length), true, false, (firstHour < 7 ? firstHour : 7), (lastHour > 18 ? lastHour : 18));
		tg.setSelectedWeeks(getSelectedWeeks());
		tg.setRoomResources(getRoomResources());
		tg.setResourceType(getResourceType());
		tg.setMode(getMode());
		return tg;
	}
	
	public void addPrintEvent(EventInterface event) {
		addPrintEvent(event, event.getMeetings());
	}
	
	public void addPrintEvent(EventInterface event, Collection<MeetingInterface> meetings) {
		for (Meeting m : addEvent(event, meetings)) {
			m.setDummy();
			m.addStyleName("meeting-selected-noshadow");
		}
	}
	
	public void labelDays(WeekInterface first, WeekInterface last) {
		for (int i = 0; i < iDays.length; i++) {
			iDayLabels.get(i).setText(CONSTANTS.longDays()[iDays[i]] +
					(first == null ? "" : " " + first.getDayNames().get(iDays[i])) +
					(last == null ? "" : " - " + last.getDayNames().get(iDays[i])));
		}
	}
	
	
	private int nrWorkDays() {
		int ret = 0;
		for (int i = 0; i < iDays.length; i++)
			if (iDays[i] < 5) ret++;
		return ret;
	}
	
	private int firstWorkDay() { 
		for (int i = 0; i < iDays.length; i++)
			if (iDays[i] < 5) return i;
		return iDays.length;
	}
	
	public void setDays(int[] days) {
		iDays = days;
		iCellWidth = (int)(0.9 * Window.getClientWidth() / iDays.length);
		iContainer.setWidth(40 + iDays.length * iCellWidth);
		iHeader.setWidth(iDays.length * iCellWidth);
		iPanel.setWidth(iDays.length * iCellWidth);
		iWorkingHours.setWidth(iCellWidth * nrWorkDays());
		iWorkingHours.getElement().getStyle().setLeft(iCellWidth * firstWorkDay(), Unit.PX);
		for (int day = 0; day < 7; day++) {
			if (day < iDays.length) {
				if (iSeparators[day] == null) {
					iSeparators[day] = new P("day-separator");
					iGrid.add(iSeparators[day], iCellWidth * day, 0);
				} else {
					iSeparators[day].getElement().getStyle().setLeft(iCellWidth * day, Unit.PX);
					iSeparators[day].setVisible(true);
				}
			} else {
				if (iSeparators[day] != null) 
					iSeparators[day].setVisible(false);
			}
		}
		iHeader.clear();
		iDayLabels.clear();
		for (int i = 0; i < iDays.length; i++) {
			P sp = new P("header-time-interval");
			sp.setWidth(iCellWidth);
			sp.setText(CONSTANTS.longDays()[iDays[i]]);
			iHeader.add(sp, i * iCellWidth, 0);
			iDayLabels.add(sp);
		}
		for (ArrayList<Meeting> meetings: iMeetings)
			for (Meeting meeting: meetings) 
				meeting.move();
		for (int i = 0; i < iSelections.getWidgetCount(); i++) {
			Widget w = iSelections.getWidget(i);
			if (w instanceof SelectionPanel)
				((SelectionPanel)w).move();
		}
		
		iVLines.clear();
		if (isShowVerticalSplit()) {
			if (isVerticalSplitByWeek()) {
				for (int d = 0; d < iDays.length; d++) {
					for (int w = 0; w < iSelectedWeeks.size(); w++) {
						if (w > 0)
							iVLines.add(new P("week-separator"), 3 + iCellWidth * d + w * (iCellWidth - 6) / iSelectedWeeks.size(), 0);
						P p = new P("week-title"); p.setHTML(iSelectedWeeks.get(w).getDayNames().get(iDays[d]).replaceAll("/", "<br>"));
						p.setWidth((iCellWidth - 6) / iSelectedWeeks.size());
						iVLines.add(p, 3 + iCellWidth * d + w * (iCellWidth - 6) / iSelectedWeeks.size(), 0);
					}
				}
			} else {
				for (int d = 0; d < iDays.length; d++) {
					for (int w = 0; w < iRoomResources.size(); w++) {
						if (w > 0)
							iVLines.add(new P("week-separator"), 3 + iCellWidth * d + w * (iCellWidth - 6) / iRoomResources.size(), 0);
						P p = new P("week-title"); p.setHTML(iRoomResources.get(w).getName().replaceAll(" ", "<br>"));
						p.setWidth((iCellWidth - 6) / iRoomResources.size());
						iVLines.add(p, 3 + iCellWidth * d + w * (iCellWidth - 6) / iRoomResources.size(), 0);
					}
				}
			}
		}
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
		boolean hasDay[] = new boolean[] { false, false, false, false, false, false, false };
		for (int slot = 0; slot < 24 * 60 / 5; slot++) {
			for (int i = 0; i < 7; i++)
				if (iMeetingTable[i][slot] != null && !iMeetingTable[i][slot].isEmpty()) hasDay[i] = true;
		}
		for (int i = 0; i < iSelections.getWidgetCount(); i++) {
			Widget w = iSelections.getWidget(i);
			if (w instanceof SelectionPanel) {
				hasDay[((SelectionPanel)w).getDayOfWeek()] = true;
			}
		}
		int nrDays = 0;
		for (boolean d: hasDay) if (d) nrDays++;
		int days[] = new int[nrDays];
		int d = 0;
		for (int i = 0; i < 7; i++)
			if (hasDay[i]) days[d++] = i;
		setDays(days);
	}
	
	public void clear() {
		for (ArrayList<Meeting> meetings: iMeetings)
			for (Meeting meeting: meetings) {
				if (meeting.hasShadow())
					iPanel.remove(meeting.getShadow());
				iPanel.remove(meeting);
			}
		iMeetings.clear();
		for (int i = 0; i < iMeetingTable.length; i++)
			for (int j = 0 ; j < iMeetingTable[i].length; j++)
				if (iMeetingTable[i][j] != null) iMeetingTable[i][j].clear();
		iTotalNrColumns = 0;
		iSelections.clear();
	}
	
	public void showSelections() {
		iSelectionLayer.setVisible(isAllowSelection());
		iSelections.clear();
		if (!isAllowSelection()) return;
		for (SelectionInterface selection: iAllSelections) {
			for (ResourceInterface location: iRoomResources) {
				if (selection.getLocations().contains(location)) {
					SelectionPanel panel = new SelectionPanel(selection);
					if (panel.isVisible()) iSelections.add(panel, panel.getLeft(), panel.getTop());
					break;
				}
			}
		}
	}
	
	public String getColor(EventInterface event) {
		String color = iColors.get(event.getId());
		if (color == null) {
			color = CONSTANTS.meetingColors()[iColors.size() % CONSTANTS.meetingColors().length];
			iColors.put(event.getId(), color);
		}
		return color;
	}
	
	protected Meeting addMeeting(EventInterface event, int day, int startSlot, int length, int startOffset, int endOffset, String name, ArrayList<String> note, String title, String color, int firstWeekIndex, int nrMeetings, ArrayList<Meeting> meetings) {
		switch (iMode) {
		case PROPORTIONAL: {
			boolean used[] = new boolean[iTotalNrColumns + nrMeetings];
			for (int i = 0; i < used.length; i++) used[i] = false;
			for (int i = 0; i < length; i++) {
				if (iMeetingTable[day][startSlot + i] != null) {
					for (Iterator<Meeting> j = iMeetingTable[day][startSlot + i].iterator(); j.hasNext(); ) {
						Meeting m = j.next();
						for (int k = 0; k < m.getNrMeetings(); k++)
							used[m.getColumn() + k] = true;
					}
				}
			}
			int col = 0;
			i: for (int i = 0; i <= iTotalNrColumns; i ++) {
				for (int j = 0; j < nrMeetings; j++)
					if (used[i + j]) continue i;
				col = i;
				break;
			}
			if (iTotalNrColumns < col + nrMeetings) {
				iTotalNrColumns = col + nrMeetings;
				for (int i = 0; i < iPanel.getWidgetCount(); i++) {
					Widget w = iPanel.getWidget(i);
					if (w instanceof Meeting) ((Meeting)w).move();
				}
			}
			Meeting meeting = new Meeting(event, name, note, day, startSlot, length, col, 1, nrMeetings, meetings, startOffset, endOffset);
	        meeting.setColor(color);
	        meeting.setTitle(title);
	        if (meeting.hasShadow()) iPanel.add(meeting.getShadow());
	        iPanel.add(meeting);
			for (int i = 0; i < length; i++) {
				if (iMeetingTable[day][startSlot + i] == null) iMeetingTable[day][startSlot + i] = new ArrayList<Meeting>();
				iMeetingTable[day][startSlot + i].add(meeting);
			}
	        return meeting;
		}
		case FILLSPACE: {
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
	        Meeting meeting = new Meeting(event, name, note, day, startSlot, length, col, cols, 1, meetings, startOffset, endOffset);
	        meeting.setColor(color);

	        meeting.setTitle(title);
	        if (meeting.hasShadow()) iPanel.add(meeting.getShadow());
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
		case OVERLAP: {
			int overlap = -1;
			for (int i = 0; i < length; i++) {
				if (iMeetingTable[day][startSlot + i] != null) {
					for (Iterator<Meeting> j = iMeetingTable[day][startSlot + i].iterator(); j.hasNext(); ) {
						Meeting m = j.next();
						if (m.getColumn() == firstWeekIndex && m.getNrColumns() > overlap) overlap = m.getNrColumns();
					}
				}
			}
			overlap += 1;
			int overlaps = overlap + 1;
			if (overlap > 0) {
				boolean used[] = new boolean[overlaps - 1];
				for (int i = 0; i < overlaps - 1; i++) used[i] = false;
				for (int i = 0; i < length; i++) {
					if (iMeetingTable[day][startSlot + i] != null) {
						for (Iterator<Meeting> j = iMeetingTable[day][startSlot + i].iterator(); j.hasNext(); ) {
							Meeting m = j.next();
							if (m.getColumn() == firstWeekIndex)
								used[m.getNrColumns()] = true;
						}
					}
				}
				for (int i = 0; i < overlaps - 1; i++)
					if (!used[i]) {overlap = i; overlaps--; break; }
			}
			
			Meeting meeting = new Meeting(event, name, note, day, startSlot, length, firstWeekIndex, overlap, nrMeetings, meetings, startOffset, endOffset);
			meeting.setColor(color);
	        meeting.setTitle(title);
	        if (meeting.hasShadow()) iPanel.add(meeting.getShadow());
	        iPanel.add(meeting);
			for (int i = 0; i < length; i++) {
				if (iMeetingTable[day][startSlot + i] == null) iMeetingTable[day][startSlot + i] = new ArrayList<Meeting>();
				iMeetingTable[day][startSlot + i].add(meeting);
			}
	        return meeting;
		}
		default:
			return null;
		}
	}
	
	private int weekIndex(MeetingInterface m) {
		if (isVerticalSplitByWeek()) {
			if (iSelectedWeeks == null) return -1;
			for (int i = 0; i < iSelectedWeeks.size(); i++) {
				if (iSelectedWeeks.get(i).getDayOfYear() <= m.getDayOfYear() && m.getDayOfYear() <= iSelectedWeeks.get(i).getDayOfYear() + 6)
					return i;
			}
			return -1;
		} else if (isSingleWeek()) {
			return (iRoomResources == null || m.getLocation() == null ? -1 : iRoomResources.indexOf(m.getLocation()));
		} else {
			return -1;
		}
	}
	
	public ArrayList<Meeting> addEvent(EventInterface event) {
		return addEvent(event, event.getMeetings());
	}
	
	public ArrayList<Meeting> addEvent(EventInterface event, Collection<MeetingInterface> eventMeetings) {
		String color = getColor(event);
		final ArrayList<Meeting> done = new ArrayList<Meeting>();
		ArrayList<MeetingInterface> meetings = new ArrayList<MeetingInterface>(eventMeetings);
		while (!meetings.isEmpty()) {
			MeetingInterface meeting = null;
			TreeSet<MeetingInterface> dates = new TreeSet<MeetingInterface>(new Comparator<MeetingInterface>() {
				public int compare(MeetingInterface m1, MeetingInterface m2) {
					if (isVerticalSplitByWeek()) {
						int cmp = new Integer(m1.getDayOfYear()).compareTo(new Integer(m2.getDayOfYear()));
						if (cmp != 0) return cmp;
					} else {
						int cmp = (m1.getLocationName() == null ? "" : m1.getLocationName()).compareTo(m2.getLocationName() == null ? "" : m2.getLocationName());
						if (cmp != 0) return cmp;
					}
					return m1.getId().compareTo(m2.getId());
				}
			});
			MeetingInterface prev = null;
			for (Iterator<MeetingInterface> i = meetings.iterator(); i.hasNext(); ) {
				MeetingInterface m = i.next();
				if (meeting == null) {
					meeting = m;
					prev = m;
					dates.add(m);
					i.remove();
				} else if (meeting.getStartSlot() == m.getStartSlot() && meeting.getEndSlot() == m.getEndSlot() &&
						meeting.getDayOfWeek() == m.getDayOfWeek() && meeting.getStartOffset() == m.getStartOffset() && meeting.getEndOffset() == m.getEndOffset()) {
					if (iMode == Mode.OVERLAP && (weekIndex(prev) != weekIndex(m) && weekIndex(prev) + 1 != weekIndex(m))) continue;
					dates.add(m);
					prev = m;
					i.remove();
				}
			}
			String dateString = null;
			TreeSet<String> rooms = new TreeSet<String>();
			int lastDay = 0;
			String endDate = null;
			TreeSet<Integer> days = new TreeSet<Integer>();
			for (MeetingInterface m: dates) {
				days.add(m.getDayOfYear());
				if (m.getLocation() != null) rooms.add(m.getLocation().getName());
				if (dateString == null) {
					dateString = sDateFormat.format(m.getMeetingDate());
					lastDay = m.getDayOfYear();
					endDate = null;
				} else if (lastDay == m.getDayOfYear()) {
				} else if (lastDay + 7 == m.getDayOfYear()) {
					endDate = sDateFormat.format(m.getMeetingDate());
					lastDay = m.getDayOfYear();
				} else {
					if (endDate != null) dateString += " - " + endDate;
					dateString += ", " + sDateFormat.format(m.getMeetingDate());
					lastDay = m.getDayOfYear();
					endDate = null;
				}
			}
			if (endDate != null) {
				dateString += " - " + endDate;
			}
			ArrayList<String> notes = new ArrayList<String>();
			notes.add(meeting.getMeetingTime());
			if (dates.size() != getSelectedWeeks().size())
				notes.add(dateString);
			String roomString = "";
			for (String room: rooms) {
				if (!roomString.isEmpty()) roomString += ", ";
				roomString += room;
			}
			if (!isSingleRoom() || getResourceType() != ResourceType.ROOM)
				notes.add(roomString);
			if (event.hasInstructors())
				for (ContactInterface instructor: event.getInstructors())
					notes.add(instructor.getName());
			if (event.hasSponsor())
				notes.add(event.getSponsor().getName());
			done.add(addMeeting(
					event,
					meeting.getDayOfWeek(), meeting.getStartSlot(), 
					meeting.getEndSlot() - meeting.getStartSlot(),
					meeting.getStartOffset(), meeting.getEndOffset(),
					(meeting.isApproved() ? "" : "<i>") + event.getName() + " (" + (event.hasInstruction() ? event.getInstruction() : event.getType()) + ")" + (meeting.isApproved() ? "" : " -- not approved</i>"), 
					notes, (event.hasInstruction() ? event.getInstruction() : event.getType()) + " " + event.getName() + ": " + 
					dateString + " " + meeting.getMeetingTime() + " " + roomString, color, weekIndex(meeting), days.size(), done));
		}
		iMeetings.add(done);
		return done;
	}
	
	public interface MeetingClickHandler {
		public void onMeetingClick(MeetingClickEvent event);
	}
	
	public class MeetingClickEvent {
		private Meeting iMeeting;
		MeetingClickEvent(Meeting meeting) {
			iMeeting = meeting;
		}
		public EventInterface getEvent() { return iMeeting.getEvent(); }
	}
	
	public void addMeetingClickHandler(MeetingClickHandler h) {
		iMeetingClickHandlers.add(h);
	}
	
	public void removeMeetingClickHandler(MeetingClickHandler h) {
		iMeetingClickHandlers.remove(h);
	}
	
	public class Meeting extends AbsolutePanel {
		private EventInterface iEvent;
		private int iColumn, iDayOfWeek, iNrColumns;
		private double iLeft, iWidth;
		private ArrayList<Meeting> iMeetings;
		private boolean iDummy = false;
		private int iNrMeetings;
		private P iShadow;
		
		private Meeting(EventInterface event, String name, ArrayList<String> note, int dayOfWeek, int start, int length, int column, int nrColumns, int nrMeetings, ArrayList<Meeting> meetings, int startOffset, int endOffset) {
			super();
			iEvent = event;
			iMeetings = meetings;
			iDayOfWeek = dayOfWeek;
			iColumn = column;
			iNrMeetings = nrMeetings;
			iNrColumns = nrColumns;
			
	        setStyleName("meeting");
	    	P header = new P("header", "label");
	    	header.setHTML(name);
	        add(header);

	        P footer = new P("footer");
	        String notes = "";
	        String delim = "<br>";
	        if (note.size() > 2 && length < 12) delim = ", ";
	        if (note.size() > 3 && length < 18) delim = ", ";
	        for (String n: note) {
	        	if (n == null || n.isEmpty()) continue;
	        	if (notes.length() > 0) notes += delim;
	        	notes += "<span  style=\"white-space: nowrap\">" + n + "</span>";
	        }
	        footer.setHTML(notes);
	        add(footer);
	        
			double totalHeight = iCellHeight * length / 12.0 - 3;
			double setupHeight = iCellHeight * startOffset / 60.0;
			double teardownHeight = - iCellHeight * endOffset / 60.0;
			
			int day = -1;
			for (int d = 0; d < iDays.length; d++)
				if (iDays[d] == iDayOfWeek) { day = d; break; }
	        switch (iMode) {
	        case PROPORTIONAL:
		        iWidth = Math.max(3.0, iNrMeetings * (iCellWidth - 6.0) / iTotalNrColumns + (iColumn + iNrMeetings != iTotalNrColumns && iTotalNrColumns > 1 ? -3 : 0));
		        iLeft = 4.0 + iCellWidth * day + iColumn * (iCellWidth - 6.0) / iTotalNrColumns;
		        break;
	        case FILLSPACE:
		        iWidth = Math.max(3.0, (iCellWidth - 6.0) / iNrColumns + (iColumn + 1 != iNrColumns && iNrColumns > 1 ? -3 : 0));
		        iLeft = 4.0 + iCellWidth * day + iColumn * (iCellWidth - 6) / iNrColumns;
		        break;
	        case OVERLAP:
	        	int weeks = (isVerticalSplitByWeek() ? iSelectedWeeks.size() : iRoomResources.size());
	        	iWidth = Math.max(3.0, iNrMeetings * (iCellWidth - 6.0) / weeks + (iColumn + iNrMeetings != weeks && weeks > 1 ? -3 : 0) - 5 * iNrColumns);
	        	iLeft = 4.0 + iCellWidth * day + iColumn * (iCellWidth - 6) / weeks + 5 * iNrColumns;
	        }
	        getElement().getStyle().setWidth(iWidth, Unit.PX);
	        getElement().getStyle().setHeight(totalHeight - setupHeight - teardownHeight, Unit.PX);
	        getElement().getStyle().setPosition(Position.ABSOLUTE);
			getElement().getStyle().setLeft(iLeft, Unit.PX);
			getElement().getStyle().setTop(1 + iCellHeight * start / 12 - iCellHeight * iStart + setupHeight, Unit.PX);
			
			if (iShowShadows && (startOffset != 0 || endOffset != 0)) {
		        iShadow = new P("meeting-shadow");
		        iShadow.getElement().getStyle().setHeight(totalHeight, Unit.PX);
		        iShadow.getElement().getStyle().setTop(1 + iCellHeight * start / 12 - iCellHeight * iStart, Unit.PX);
		        iShadow.getElement().getStyle().setLeft(iLeft, Unit.PX);
		        iShadow.getElement().getStyle().setWidth(iWidth, Unit.PX);
		        iShadow.getElement().getStyle().setPosition(Position.ABSOLUTE);
			}
	        
			sinkEvents(Event.ONCLICK);
			sinkEvents(Event.ONMOUSEOVER);
			sinkEvents(Event.ONMOUSEMOVE);
			sinkEvents(Event.ONMOUSEOUT);
		}
		
		public void setColor(String color) {
			addStyleName(color);
			if (hasShadow()) iShadow.addStyleName(color);
		}
		
		public EventInterface getEvent() {
			return iEvent;
		}
		
		public boolean hasShadow() {
			return iShadow != null;
		}
		
		public P getShadow() {
			return iShadow;
		}
		
		private void select(boolean selected) {
			if (selected) {
				getElement().getStyle().setWidth(iCellWidth - 6, Unit.PX);
				getElement().getStyle().setLeft(4 + iCellWidth * getDay(), Unit.PX);
				if (hasShadow()) {
					iShadow.getElement().getStyle().setWidth(iCellWidth - 6, Unit.PX);
					iShadow.getElement().getStyle().setLeft(4 + iCellWidth * getDay(), Unit.PX);
				}
				getElement().getStyle().setZIndex(1001);
				if (hasShadow()) iShadow.getElement().getStyle().setZIndex(1000);
				for (Meeting meeting: iMeetings) {
					meeting.addStyleName("meeting-selected");
					meeting.getElement().getStyle().setCursor(iEvent.isCanView() ? Cursor.POINTER : Cursor.AUTO);
				}
			} else {
				getElement().getStyle().setWidth(iWidth, Unit.PX);
				getElement().getStyle().setLeft(iLeft, Unit.PX);
				if (hasShadow()) {
					iShadow.getElement().getStyle().setWidth(iWidth, Unit.PX);
					iShadow.getElement().getStyle().setLeft(iLeft, Unit.PX);
				}
				getElement().getStyle().clearZIndex();
				if (hasShadow()) iShadow.getElement().getStyle().clearZIndex();
				for (Meeting meeting: iMeetings) {
					meeting.removeStyleName("meeting-selected");
					meeting.getElement().getStyle().clearCursor();
				}
			}
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
				select(false);
				if (!anchor) {
					MeetingClickEvent e = new MeetingClickEvent(Meeting.this);
					for (MeetingClickHandler h: iMeetingClickHandlers)
						h.onMeetingClick(e);
				}
				break;
			case Event.ONMOUSEOVER:
		        if (related == null || !getElement().isOrHasChild((Element)related.cast())) {
					select(true);
		        }
				break;
			case Event.ONMOUSEOUT:
		        if (related == null || !getElement().isOrHasChild((Element)related.cast())) {
		        	select(false);
		        }
				break;
			case Event.ONMOUSEMOVE:
				int relativeX = event.getClientX() - getElement().getAbsoluteLeft() + getElement().getScrollLeft() + getElement().getOwnerDocument().getScrollLeft();
				if (relativeX < iLeft - 6 - getDay() * iCellWidth || relativeX > iLeft - 2 - getDay() * iCellWidth + iWidth) {
					select(false);
				}
				break;
			}
		    super.onBrowserEvent(event);
		}
		
		public void setNrColumns(int nrColumns) {
			if (nrColumns == iNrColumns) return;
			iNrColumns = nrColumns;
			move();
		}
		
		public void move() {
	        switch (iMode) {
	        case PROPORTIONAL:
		        iWidth = Math.max(3, iNrMeetings * (iCellWidth - 6.0) / iTotalNrColumns + (iColumn + iNrMeetings != iTotalNrColumns && iTotalNrColumns > 1 ? -3 : 0));
		        iLeft = 4.0 + iCellWidth * getDay() + iColumn * (iCellWidth - 6.0) / iTotalNrColumns;
		        break;
	        case FILLSPACE:
		        iWidth = (iCellWidth - 6.0) / iNrColumns + (iColumn + 1 != iNrColumns && iNrColumns > 1 ? -3 : 0);
		        iLeft = 4.0 + iCellWidth * getDay() + iColumn * (iCellWidth - 6) / iNrColumns;
		        break;
	        case OVERLAP:
	        	int weeks = (isVerticalSplitByWeek() ? iSelectedWeeks.size() : iRoomResources.size());
	        	iWidth = iNrMeetings * (iCellWidth - 6.0) / weeks + (iColumn + iNrMeetings != weeks && weeks > 1 ? -3 : 0) - 5 * iNrColumns;
	        	iLeft = 4.0 + iCellWidth * getDay() + iColumn * (iCellWidth - 6) / weeks + 5 * iNrColumns;
	        }
			getElement().getStyle().setWidth(iWidth, Unit.PX);
			getElement().getStyle().setLeft(iLeft, Unit.PX);
			if (hasShadow()) {
				iShadow.getElement().getStyle().setWidth(iWidth, Unit.PX);
				iShadow.getElement().getStyle().setLeft(iLeft, Unit.PX);
			}
		}
		
		public void setTitle(String title) {
			super.setTitle(title);
		}
		
		public int getColumn() {
			return iColumn;
		}
		
		public int getDayOfWeek() {
			return iDayOfWeek;
		}
		
		public int getDay() {
			for (int d = 0; d < iDays.length; d++)
				if (iDays[d] == iDayOfWeek) return d;
			return -1;
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
		
		public int getNrMeetings() {
			return iNrMeetings;
		}
	}
	
	class SelectionLayer extends AbsolutePanel {
		private SelectionPanel iSelection;
		private P iHint;
		private PopupPanel iPopup;
		private SelectionPanel iMoving = null;
		
		public SelectionLayer() {
			setStyleName("selection-layer");
			
			iPopup = new PopupPanel();
			iPopup.setStyleName("unitime-TimeGridSelectionPopup");
			iHint = new P("content");
			iPopup.setWidget(iHint);
			
			iSelection = new SelectionPanel();
			iSelection.setVisible(false);
			add(iSelection, 0, 0);
			
			sinkEvents(Event.ONMOUSEDOWN);
			sinkEvents(Event.ONMOUSEUP);
			sinkEvents(Event.ONMOUSEMOVE);
			sinkEvents(Event.ONMOUSEOVER);
			sinkEvents(Event.ONMOUSEOUT);
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			if (Event.ONMOUSEMOVE == DOM.eventGetType(event) && !iSelection.isActive() && iMoving != null) {
				iMoving.onBrowserEvent(event);
				if (iMoving.iCursor != null)
					getElement().getStyle().setCursor(iMoving.iCursor);
				return;
			}

			double x = event.getClientX() - getAbsoluteLeft() + Window.getScrollLeft();
			double y = event.getClientY() - getAbsoluteTop() + Window.getScrollTop();

			int slot = 3 * Math.min(Math.max(0, (int)Math.round(4 * (y - 1 + iStart * iCellHeight) / iCellHeight)), 96);
			int day = Math.min(Math.max(0, (int)Math.floor((x - 2) / iCellWidth)), iDays.length - 1);
			int dayOfWeek = iDays[day];
			int weeks = (isSingleRoom() ? iSelectedWeeks.size() : iRoomResources.size());
			int week = Math.min(Math.max(0, (int)Math.floor(weeks * (x - 2 - iCellWidth * day) / (iCellWidth - 6))), weeks - 1);
			int h = slot / 12;
			int m = 5 * (slot % 12);
			String time = (CONSTANTS.useAmPm() ? (h == 0 ? "12": h <= 12 ? h : h-12) : h) + ":" + (m < 10 ? "0" : "") + m + (CONSTANTS.useAmPm() ? (h <= 11 ? "a" : "p") : "");
			
			String text = CONSTANTS.longDays()[dayOfWeek] + " " + (isSingleRoom() ? iSelectedWeeks.get(week) : iSelectedWeeks.get(0)).getDayNames().get(dayOfWeek) +
					" " + time + (isSingleRoom() ? "" : " " + iRoomResources.get(week).getName());
			iPopup.setPopupPosition(event.getClientX() + Window.getScrollLeft(), event.getClientY() + Window.getScrollTop());
			
			getElement().getStyle().setCursor(Cursor.CROSSHAIR);
			
			switch (DOM.eventGetType(event)) {
			case Event.ONMOUSEDOWN:
				iSelection.setStart(dayOfWeek, slot, week);
				iSelection.setEnd(dayOfWeek, slot, week);
				iSelection.setVisible(true);
				iSelection.setActive(true);
				break;
			case Event.ONMOUSEMOVE:
				iSelection.setEnd(dayOfWeek, slot, week);
				if (!iPopup.isShowing()) iPopup.show();
				break;
			case Event.ONMOUSEUP:
				onMouseUp();
				break;
			case Event.ONMOUSEOVER:
				if (!iPopup.isShowing() && (iSelection.isActive() || iMoving == null)) iPopup.show();
				if (iSelection.isActive() && !iSelection.isVisible()) {
					iSelection.setVisible(true);					
				}	
				break;
			case Event.ONMOUSEOUT:
				if (!DOM.isOrHasChild(getElement(), DOM.eventGetToElement(event))) {
					if (iPopup.isShowing()) iPopup.hide();
					iSelection.setVisible(false);
				}
				/*
				if (iSelection.isActive() && !DOM.isOrHasChild(TimeGrid.this.getElement(), DOM.eventGetToElement(event))) {
					iSelection.setActive(false);
				}
				*/
				break;
			}
			
			iHint.setText((iSelection.isVisible() && iSelection.isActive() ? iSelection.toString() : text));
			
			event.preventDefault();
			event.stopPropagation();
		}
		
		public void onMouseUp() {
			if (iSelection.isVisible() && iSelection.isActive()) {
				SelectionPanel s = new SelectionPanel(iSelection.getDayOfWeek(), iSelection.getStartSlot(), iSelection.getLength(), iSelection.getStartWeek(), iSelection.getNrWeeks());
				iSelections.add(s, s.getLeft(), s.getTop());
			}
			iSelection.setVisible(false);
			iSelection.setActive(false);
			for (int i = 0; i < iSelections.getWidgetCount(); i++) {
				Widget w = iSelections.getWidget(i);
				if (w instanceof SelectionPanel)
					((SelectionPanel)w).onMouseUp();
			}
		}
		
		@Override
		public void clear() {
			super.clear();
			iSelection.setVisible(false); iSelection.setActive(false);
			add(iSelection, 0, 0);
		}
	}
	
	public class SelectionPanel extends AbsolutePanel {
		private int iDayOfWeek = -1, iStartSlot, iEndSlot, iStartWeek, iEndWeek;
		private boolean iActive = false;
		private P iRemove = null, iText = null;
		private SelectionInterface iSelection = null;
		
		public SelectionPanel(boolean fixed) {
			setStyleName(fixed ? "selection" : "active-selection");
			if (fixed) {
				iRemove = new P("x"); iRemove.setHTML("&times;");
				iRemove.addMouseDownHandler(new MouseDownHandler() {
					@Override
					public void onMouseDown(MouseDownEvent event) {
						iSelections.remove(SelectionPanel.this);
						if (iSelection != null)
							iAllSelections.remove(iSelection);
					}
				});
				iRemove.getElement().getStyle().setRight(2, Unit.PX);
				iRemove.getElement().getStyle().setTop(2, Unit.PX);
				iRemove.getElement().getStyle().setPosition(Position.ABSOLUTE);
				add(iRemove);
				iText = new P("text");
				add(iText, 0, 0);
			}
			
			sinkEvents(Event.ONMOUSEDOWN);
			sinkEvents(Event.ONMOUSEUP);
			sinkEvents(Event.ONMOUSEMOVE);
		}
		
		private Cursor iCursor = null;
		private double iX, iY;
		private int iSS, iES, iSW, iEW;
		
		private Cursor cursor(double x, double y) {
			if (x <= 6) {
				if (y <= 6)
					return Cursor.NW_RESIZE;
				else if (y >= getHeight() - 6)
					return Cursor.SW_RESIZE;
				else
					return Cursor.W_RESIZE;
			} else if (x >= getWidth() - 6) {
				if (y <= 6)
					return Cursor.NE_RESIZE;
				else if (y >= getHeight() - 6)
					return Cursor.SE_RESIZE;
				else
					return Cursor.E_RESIZE;
			} else if (y <= 6) {
				return Cursor.N_RESIZE;
			} else if (y >= getHeight() - 6) {
				return Cursor.S_RESIZE;
			} else {
				return Cursor.MOVE;
			}
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			if (iText == null) return;
			
			double dx = event.getClientX() - getAbsoluteLeft() + Window.getScrollLeft();
			double dy = event.getClientY() - getAbsoluteTop() + Window.getScrollTop();
			
			double x = event.getClientX() - iSelectionLayer.getAbsoluteLeft() + Window.getScrollLeft();
			double y = event.getClientY() - iSelectionLayer.getAbsoluteTop() + Window.getScrollTop();
			
			switch (DOM.eventGetType(event)) {
			case Event.ONMOUSEMOVE:
				if (iCursor == null)
					getElement().getStyle().setCursor(cursor(dx, dy));
				else {
					getElement().getStyle().setCursor(iCursor);
					
					int dSlot = 3 * (int)Math.round(4 * (y - iY) / iCellHeight);
					int weeks = (isSingleRoom() ? iSelectedWeeks.size() : iRoomResources.size());
					int dWeek = (int)Math.round(weeks * (x - iX) / (iCellWidth - 6));
					
					switch (iCursor) {
					case MOVE:
						dSlot = Math.min(Math.max(dSlot, iStart * 12 - iSS), 12 * iEnd - iES);
						dWeek = Math.min(Math.max(dWeek, -iSW), weeks - iEW - 1);
						iStartSlot = iSS + dSlot; iEndSlot = iES + dSlot;
						iStartWeek = iSW + dWeek; iEndWeek = iEW + dWeek;
						break;
					case N_RESIZE:
						dSlot = Math.max(dSlot, iStart * 12 - iSS);
						if (iSS + dSlot < iES) iStartSlot = iSS + dSlot;
						break;
					case NW_RESIZE:
						dSlot = Math.max(dSlot, iStart * 12 - iSS);
						dWeek = Math.max(dWeek, -iSW);
						if (iSS + dSlot < iES) iStartSlot = iSS + dSlot;
						if (iSW + dWeek <= iEW) iStartWeek = iSW + dWeek;
						break;
					case NE_RESIZE:
						dSlot = Math.max(dSlot, iStart * 12 - iSS);
						dWeek = Math.min(dWeek, weeks - iEW - 1);
						if (iSS + dSlot < iES) iStartSlot = iSS + dSlot;
						if (iSW <= iEW + dWeek) iEndWeek = iEW + dWeek;
						break;
					case E_RESIZE:
						dWeek = Math.min(dWeek, weeks - iEW - 1);
						if (iSW <= iEW + dWeek) iEndWeek = iEW + dWeek;
						break;
					case SE_RESIZE:
						dWeek = Math.min(dWeek, weeks - iEW - 1);
						dSlot = Math.min(dSlot, 12 * iEnd - iES);
						if (iSW <= iEW + dWeek) iEndWeek = iEW + dWeek;
						if (iSS < iES + dSlot) iEndSlot = iES + dSlot;
						break;
					case S_RESIZE:
						dSlot = Math.min(dSlot, 12 * iEnd - iES);
						if (iSS < iES + dSlot) iEndSlot = iES + dSlot;
						break;
					case SW_RESIZE:
						dSlot = Math.min(dSlot, 12 * iEnd - iES);
						dWeek = Math.max(dWeek, -iSW);
						if (iSS < iES + dSlot) iEndSlot = iES + dSlot;
						if (iSW + dWeek <= iEW) iStartWeek = iSW + dWeek;
						break;
					case W_RESIZE:
						dWeek = Math.max(dWeek, -iSW);
						if (iSW + dWeek <= iEW) iStartWeek = iSW + dWeek;
						break;
					}
					move();
					if (iSS != iStartSlot || iES != iEndSlot) {
						iSelection.setStartSlot(iStartSlot);
						iSelection.setLength(getLength());
					}
					if (iSW != iStartWeek || iEW != iEndWeek) {
						if (isSingleRoom()) {
							iSelection.getDays().clear();
							for (int i = getStartWeek(); i <= getEndWeek(); i++)
								iSelection.getDays().add(iSelectedWeeks.get(i).getDayOfYear() + getDayOfWeek());
						} else {
							iSelection.getLocations().clear();
							for (int i = getStartWeek(); i <= getEndWeek(); i++)
								iSelection.addLocation(iRoomResources.get(i));
						}
					}
				}
				break;
			case Event.ONMOUSEDOWN:
				iCursor = cursor(dx, dy); iX = x; iY = y;
				iSelectionLayer.iMoving = this;
				iSS = iStartSlot; iES = iEndSlot;
				iSW = iStartWeek; iEW = iEndWeek;
				break;
			case Event.ONMOUSEUP:
				onMouseUp();
				break;
			}

			event.preventDefault();
			event.stopPropagation();
		}
		
		public SelectionPanel() {
			this(false);
		}
		
		public SelectionPanel(int dayOfWeek, int start, int length, int week, int nrWeeks) {
			this(true);
			setStart(dayOfWeek, start, week);
			setEnd(dayOfWeek, start + length, week + nrWeeks - 1);
			iSelection = new SelectionInterface();
			iSelection.setStartSlot(getStartSlot());
			iSelection.setLength(getLength());
			if (isSingleRoom()) {
				for (int i = getStartWeek(); i <= getEndWeek(); i++) {
					iSelection.addDay(iSelectedWeeks.get(i).getDayOfYear() + getDayOfWeek());
				}
				iSelection.addLocation(getRoomResources().get(0));
			} else {
				iSelection.addDay(iSelectedWeeks.get(0).getDayOfYear() + getDayOfWeek());
				for (int i = getStartWeek(); i <= getEndWeek(); i++) {
					iSelection.addLocation(iRoomResources.get(i));
				}
			}
			iAllSelections.add(iSelection);
		}
		
		public SelectionPanel(SelectionInterface selection) {
			this(true);
			iSelection = selection;
			int dayOfWeek = -1;
			int startWeek = iSelectedWeeks.size(), endWeek = -1;
			for (int d: selection.getDays()) {
				for (int i = 0; i < iSelectedWeeks.size(); i++) {
					WeekInterface w = iSelectedWeeks.get(i);
					if (w.getDayOfYear() <= d && d < w.getDayOfYear() + 7) {
						startWeek = Math.min(startWeek, i);
						endWeek = Math.max(endWeek, i);
						dayOfWeek = d - w.getDayOfYear();
					}
				}
			}
			int startRoom = iRoomResources.size(), endRoom = -1;
			for (ResourceInterface location: selection.getLocations()) {
				for (int i = 0; i < iRoomResources.size(); i++) {
					ResourceInterface r = iRoomResources.get(i);
					if (r.equals(location)) {
						startRoom = Math.min(startRoom, i);
						endRoom = Math.max(endRoom, i);
					}
				}
			}
			if (dayOfWeek >= 0) {
				setStart(dayOfWeek, selection.getStartSlot(), isSingleRoom() ? startWeek : startRoom);
				setEnd(dayOfWeek, selection.getStartSlot() + selection.getLength(), isSingleRoom() ? endWeek : endRoom);
			} else {
				setVisible(false);
			}
		}
		
		public void setStart(int day, int slot, int week) {
			iDayOfWeek = day; iStartSlot = slot; iStartWeek = week;
		}
		
		public void setEnd(int day, int slot, int week) {
			iEndSlot = slot;
			if (iDayOfWeek == day) {
				iEndWeek = week;
			} else if (day < iDayOfWeek) {
				iEndWeek = 0;
			} else {
				iEndWeek = (isSingleRoom() ? iSelectedWeeks.size() : iRoomResources.size()) - 1;
			}
			move();
		}
				
		public void setActive(boolean active) {
			iActive = active;
		}
		
		public boolean isActive() { return iActive; }
		
		private void move() {
	        getElement().getStyle().setWidth(getWidth(), Unit.PX);
	        getElement().getStyle().setLeft(getLeft(), Unit.PX);
	        getElement().getStyle().setTop(getTop(), Unit.PX);
	        getElement().getStyle().setHeight(getHeight(), Unit.PX);
	        if (iText != null)
	        	iText.setHTML(
	        		CONSTANTS.days()[getDayOfWeek()] + " " +
	        		(isSingleRoom() ?
	        			iSelectedWeeks.get(getStartWeek()).getDayNames().get(getDayOfWeek()) + (getNrWeeks() <= 1 ? "" : "&nbsp;&#8209;&nbsp;" + iSelectedWeeks.get(getEndWeek()).getDayNames().get(getDayOfWeek())) :
	        			iSelectedWeeks.get(0).getDayNames().get(getDayOfWeek())) + " " +
	        		getStartTime() + "&nbsp;&#8209;&nbsp;" + getEndTime() + " (" + (5 * getLength()) + "&nbsp;mins) " +
	        		(isSingleRoom() ? iRoomResources.get(0).getName().replace(" ", "&nbsp;") :
	        			iRoomResources.get(getStartWeek()).getName().replace(" ", "&nbsp;") + (getNrWeeks() <= 1 ? "" : "&nbsp;&#8209;&nbsp;" + iRoomResources.get(getEndWeek()).getName().replace(" ", "&nbsp;")))
	        		);
		}
		
		public int getLeft() {
			return 4 + iCellWidth * getDay() + getStartWeek() * (iCellWidth - 6) / (isSingleRoom() ? iSelectedWeeks.size() : iRoomResources.size());
		}
		
		public int getTop() {
			return 1 + iCellHeight * getStartSlot() / 12 - iCellHeight * iStart;
		}
		
		public int getHeight() {
			return iCellHeight * getLength() / 12;
		}
		
		public int getWidth() {
			int weeks = (isSingleRoom() ? iSelectedWeeks.size() : iRoomResources.size());
			return getNrWeeks() * (iCellWidth - 6) / weeks + (getStartWeek() + getNrWeeks() != weeks && weeks > 1 ? 0 : 3);
		}
		
		public int getDay() {
			for (int i = 0; i < iDays.length; i++)
				if (iDays[i] == iDayOfWeek) return i;
			return -1;
		}
		
		public int getDayOfWeek() { return iDayOfWeek; }
		
		public int getStartSlot() {
			return (iStartSlot <= iEndSlot ? iStartSlot : iEndSlot);
		}
		
		public int getEndSlot() {
			return getStartSlot() + getLength();
		}
		
		public int getLength() {
			return Math.max(3, iStartSlot < iEndSlot ? iEndSlot - iStartSlot : iStartSlot - iEndSlot);
		}
		
		public int getStartWeek() {
			return (iStartWeek <= iEndWeek ? iStartWeek : iEndWeek);
		}
		
		public int getEndWeek() {
			return (iStartWeek <= iEndWeek ? iEndWeek : iStartWeek);
		}
		
		public int getNrWeeks() {
			return iStartWeek < iEndWeek ? 1 + iEndWeek - iStartWeek : 1 + iStartWeek - iEndWeek;
		}
		
		public String getStartTime() {
			int h = getStartSlot() / 12;
			int m = 5 * (getStartSlot() % 12);
			return (CONSTANTS.useAmPm() ? (h == 0 ? "12": h <= 12 ? h : h-12) : h) + ":" + (m < 10 ? "0" : "") + m + (CONSTANTS.useAmPm() ? (h <= 11 ? "a" : "p") : "");
		}
		
		public String getEndTime() {
			int h = (getStartSlot() + getLength()) / 12;
			int m = 5 * ((getStartSlot() + getLength()) % 12);
			return (CONSTANTS.useAmPm() ? (h == 0 ? "12": h <= 12 ? h : h-12) : h) + ":" + (m < 10 ? "0" : "") + m + (CONSTANTS.useAmPm() ? (h <= 11 ? "a" : "p") : "");
		}
		
		public String toString() {
			return CONSTANTS.longDays()[getDayOfWeek()] + " " +
				(isSingleRoom() ?
        			iSelectedWeeks.get(getStartWeek()).getDayNames().get(getDayOfWeek()) + (getNrWeeks() <= 1 ? "" : " - " + iSelectedWeeks.get(getEndWeek()).getDayNames().get(getDayOfWeek())) :
        			iSelectedWeeks.get(0).getDayNames().get(getDayOfWeek())) + " " +
				getStartTime() + " - " + getEndTime() + " (" + (5 * getLength()) + " mins)" + " " +
        		(isSingleRoom() ? iRoomResources.get(0).getName() :
        			iRoomResources.get(getStartWeek()).getName() + (getNrWeeks() <= 1 ? "" : " - " + iRoomResources.get(getEndWeek()).getName()))
				;
		}
		
		public SelectionInterface getSelection() {
			return iSelection;
		}
		
		private void onMouseUp() {
			if (iSelectionLayer.iMoving != null) {
				iSelectionLayer.iMoving.iCursor = null;
				iSelectionLayer.iMoving = null;
			}
			iCursor = null;
		}
	}
	
	public List<SelectionInterface> getSelections() {
		return iAllSelections;
	}
}
