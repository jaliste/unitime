/*
 * UniTime 3.4 (University Timetabling Application)
 * Copyright (C) 2012, UniTime LLC, and individual contributors
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
package org.unitime.timetable.export.events;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.export.ExportHelper;
import org.unitime.timetable.export.PDFPrinter;
import org.unitime.timetable.gwt.client.events.EventComparator.EventMeetingSortBy;
import org.unitime.timetable.gwt.shared.EventInterface;
import org.unitime.timetable.gwt.shared.EventInterface.EventFlag;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;

@Service("org.unitime.timetable.export.Exporter:meetings.pdf")
public class EventsExportMeetingsToPDF extends EventsExporter {

	@Override
	public String reference() {
		return "meetings.pdf";
	}

	@Override
	protected void print(ExportHelper helper, List<EventInterface> events, int eventCookieFlags, EventMeetingSortBy sort) throws IOException {
		Printer printer = new PDFPrinter(helper.getOutputStream(), true);
		helper.setup(printer.getContentType(), reference(), false);
		hideColumns(printer, events, eventCookieFlags);
		print(printer, meetings(events, sort));
	}

	@Override
	protected void hideColumn(Printer out, List<EventInterface> events, EventFlag flag) {
		switch (flag) {
		case SHOW_SECTION: out.hideColumn(1); break;
		case SHOW_TITLE: out.hideColumn(3); break;
		case SHOW_PUBLISHED_TIME: out.hideColumn(5); break;
		case SHOW_ALLOCATED_TIME: out.hideColumn(6); break;
		case SHOW_SETUP_TIME: out.hideColumn(7); break;
		case SHOW_TEARDOWN_TIME: out.hideColumn(8); break;
		case SHOW_CAPACITY: out.hideColumn(10); break;
		case SHOW_ENROLLMENT: out.hideColumn(11); break;
		case SHOW_LIMIT: out.hideColumn(12); break;
		case SHOW_SPONSOR: out.hideColumn(13); break;
		case SHOW_MAIN_CONTACT: out.hideColumn(14); break;
		case SHOW_APPROVAL: out.hideColumn(15); break;
		}
	}
	
	protected void print(Printer out, Set<EventMeeting> meetings) throws IOException {
		out.printHeader(
				/*  0 */ MESSAGES.colName(),
				/*  1 */ MESSAGES.colSection(),
				/*  2 */ MESSAGES.colType(),
				/*  3 */ MESSAGES.colTitle(),
				/*  4 */ MESSAGES.colDate(),
				/*  5 */ MESSAGES.colPublishedTime(),
				/*  6 */ MESSAGES.colAllocatedTime(),
				/*  7 */ MESSAGES.colSetupTimeShort(),
				/*  8 */ MESSAGES.colTeardownTimeShort(),
				/*  9 */ MESSAGES.colLocation(),
				/* 10 */ MESSAGES.colCapacity(),
				/* 11 */ MESSAGES.colEnrollment(),
				/* 12 */ MESSAGES.colLimit(),
				/* 13 */ MESSAGES.colSponsorOrInstructor(),
				/* 14 */ MESSAGES.colMainContact(),
				/* 15 */ MESSAGES.colApproval());
		
		DateFormat df = new SimpleDateFormat(CONSTANTS.eventDateFormat(), Localization.getJavaLocale());
		DateFormat dfMeeting = new SimpleDateFormat(CONSTANTS.meetingDateFormat(), Localization.getJavaLocale());
		EventInterface last = null;
		
		for (EventMeeting em: meetings) {
			EventInterface event = em.getEvent();
			MeetingInterface meeting = em.getMeeting();
			
			if (last == null || !last.equals(event)) {
				out.flush();
				last = event;
			}
			
			out.printLine(
					getName(event),
					getSection(event),
					event.hasInstruction() ? event.getInstruction() : event.getType().getAbbreviation(),
					getTitle(event),
					meeting.isArrangeHours() ? CONSTANTS.arrangeHours() : dfMeeting.format(meeting.getMeetingDate()),
					meeting.getMeetingTime(CONSTANTS),
					meeting.getAllocatedTime(CONSTANTS),
					String.valueOf(meeting.getStartOffset()),
					String.valueOf(-meeting.getEndOffset()),
					meeting.getLocationName(),
					meeting.hasLocation() && meeting.getLocation().hasSize() ? meeting.getLocation().getSize().toString() : null,
					event.hasEnrollment() ? event.getEnrollment().toString() : null,
					event.hasMaxCapacity() ? event.getMaxCapacity().toString() : null,
					event.hasInstructors() ? event.getInstructorNames("\n") : event.hasSponsor() ? event.getSponsor().getName() : null,
					event.hasContact() ? event.getContact().getName() : null,
					meeting.isArrangeHours() ? "" : meeting.isApproved() ? df.format(meeting.getApprovalDate()) : MESSAGES.approvalNotApproved()
					);
		}
		
		out.flush();
		out.close();
	}
}
