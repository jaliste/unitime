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
package org.unitime.timetable.gwt.client.events;

import org.unitime.timetable.gwt.shared.EventInterface;
import org.unitime.timetable.gwt.shared.EventInterface.EventType;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingConglictInterface;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;

public class EventComparator {
	public static enum EventMeetingSortBy {
		NAME, SECTION, TYPE, DATE, PUBLISHED_TIME, ALLOCATED_TIME, SETUP_TIME, TEARDOWN_TIME, LOCATION, CAPACITY, SPONSOR, MAIN_CONTACT, APPROVAL, LIMIT, ENROLLMENT
	}

	protected static int compareByName(EventInterface e1, EventInterface e2) {
		return compare(e1.getName(), e2.getName());
	}
	
	protected static int compareBySection(EventInterface e1, EventInterface e2) {
		if (e1.hasExternalIds()) {
			if (e2.hasExternalIds()) {
				int cmp = e1.getExternalIds().get(0).compareTo(e2.getExternalIds().get(0));
				if (cmp != 0) return cmp;
			} else return -1;
		} else if (e2.hasExternalIds()) return 1;
		return compare(e1.getSectionNumber(), e2.getSectionNumber());
	}
	
	protected static int compareByType(EventInterface e1, EventInterface e2) {
		int cmp = (e1.getType() == null ? e2.getType() == null ? 0 : -1 : e2.getType() == null ? 1 : e1.getType().compareTo(e2.getType()));
		if (cmp != 0) return cmp;
		return compare(e1.getInstructionType(), e2.getInstructionType());
	}
	
	protected static int compareBySponsor(EventInterface e1, EventInterface e2) {
		int cmp = compare(e1.getInstructorNames("|"), e2.getInstructorNames("|"));
		if (cmp != 0) return cmp;
		return compare(e1.hasSponsor() ? e1.getSponsor().getName() : null, e2.hasSponsor() ? e2.getSponsor().getName() : null);
	}
	
	protected static int compareByMainContact(EventInterface e1, EventInterface e2) {
		return compare(e1.hasContact() ? e1.getContact().getName() : null, e2.hasContact() ? e2.getContact().getName() : null);
	}
	
	protected static int compareByLimit(EventInterface e1, EventInterface e2) {
		return -(e1.hasMaxCapacity() ? e1.getMaxCapacity() : new Integer(0)).compareTo(e2.hasMaxCapacity() ? e2.getMaxCapacity() : new Integer(0));
	}
	
	protected static int compareByEnrollment(EventInterface e1, EventInterface e2) {
		return -(e1.hasEnrollment() ? e1.getEnrollment() : new Integer(0)).compareTo(e2.hasEnrollment() ? e2.getEnrollment() : new Integer(0));
	}
	
	public static int compareFallback(EventInterface e1, EventInterface e2) {
		int cmp = compareByName(e1, e2);
		if (cmp != 0) return cmp;
		cmp = compareBySection(e1, e2);
		if (cmp != 0) return cmp;
		cmp = compareByType(e1, e2);
		if (cmp != 0) return cmp;
		return e1.compareTo(e2);
	}
	
	public static int compareEvents(EventInterface e1, EventInterface e2, EventMeetingSortBy sortBy) {
		switch (sortBy) {
		case NAME:
			return compareByName(e1, e2);
		case SECTION:
			return compareBySection(e1, e2);
		case TYPE:
			return compareByType(e1, e2);
		case SPONSOR:
			return compareBySponsor(e1, e2);
		case MAIN_CONTACT:
			return compareByMainContact(e1, e2);
		case LIMIT:
			return compareByLimit(e1, e2);
		case ENROLLMENT:
			return compareByEnrollment(e1, e2);
		}
		return 0;
	}
	
	protected static int compateByApproval(MeetingInterface m1, MeetingInterface m2) {
		if (m1.getId() == null && m2.getId() != null) return -1;
		if (m1.getId() != null && m2.getId() == null) return 1;
		if (m1.getApprovalDate() == null) {
			return (m2.getApprovalDate() == null ? 0 : -1);
		} else {
			return (m2.getApprovalDate() == null ? 1 : m1.getApprovalDate().compareTo(m2.getApprovalDate()));
		}
	}
	
	protected static int compareByName(MeetingInterface m1, MeetingInterface m2) {
		return compare(m1 instanceof MeetingConglictInterface ? ((MeetingConglictInterface)m1).getName() : null,
				m2 instanceof MeetingConglictInterface ? ((MeetingConglictInterface)m2).getName() : null);
	}
	
	protected static int compareByType(MeetingInterface m1, MeetingInterface m2) {
		EventType t1 = (m1 instanceof MeetingConglictInterface ? ((MeetingConglictInterface)m1).getType() : null);
		EventType t2 = (m2 instanceof MeetingConglictInterface ? ((MeetingConglictInterface)m2).getType() : null);
		return (t1 == null ? t2 == null ? 0 : -1 : t2 == null ? 1 : t1.compareTo(t2));
	}

	protected static int compareByDate(MeetingInterface m1, MeetingInterface m2) {
		if (m1 instanceof MeetingConglictInterface && m2 instanceof MeetingConglictInterface) {
			int cmp = ((MeetingConglictInterface)m1).getName().compareTo(((MeetingConglictInterface)m2).getName());
			if (cmp != 0) return cmp;
		}
		return (m1.getMeetingDate() == null ? m2.getMeetingDate() == null ? 0 : 1 :m2.getMeetingDate() == null ? -1 :  m1.getMeetingDate().compareTo(m2.getMeetingDate()));
	}
	
	protected static int compareByAllocatedTime(MeetingInterface m1, MeetingInterface m2) {
		int cmp = new Integer(m1.getStartSlot()).compareTo(m2.getStartSlot());
		if (cmp != 0) return cmp;
		return new Integer(m1.getEndSlot()).compareTo(m2.getEndSlot());
	}
	
	protected static int compareByPublishedTime(MeetingInterface m1, MeetingInterface m2) {
		int cmp = new Integer((5 * m1.getStartSlot()) + m1.getStartOffset()).compareTo((5 * m2.getStartSlot()) + m2.getStartOffset());
		if (cmp != 0) return cmp;
		return new Integer((5 * m1.getEndSlot()) + m2.getEndOffset()).compareTo((5 * m2.getEndSlot()) + m2.getEndOffset());
	}

	protected static int compareBySetupTime(MeetingInterface m1, MeetingInterface m2) {
		return new Integer(m1.getStartOffset()).compareTo(m2.getStartOffset());
	}

	protected static int compareByTeardownTime(MeetingInterface m1, MeetingInterface m2) {
		return new Integer(m2.getEndOffset()).compareTo(m1.getEndOffset());
	}
	
	protected static int compareByLocation(MeetingInterface m1, MeetingInterface m2) {
		return m1.getLocationName().compareTo(m2.getLocationName());
	}
	
	protected static int compareByCapacity(MeetingInterface m1, MeetingInterface m2) {
		return (m1.getLocation() == null ? new Integer(-1) : m1.getLocation().getSize()).compareTo(m2.getLocation() == null ? new Integer(-1) : m2.getLocation().getSize());
	}

	public static int compareFallback(MeetingInterface m1, MeetingInterface m2) {
		int cmp = compareByDate(m1, m2);
		if (cmp != 0) return cmp;
		cmp = compareByPublishedTime(m1, m2);
		if (cmp != 0) return cmp;
		cmp = compareByLocation(m1, m2);
		if (cmp != 0) return cmp;
		return m1.compareTo(m2);
	}
	
	public static int compareMeetings(MeetingInterface m1, MeetingInterface m2, EventMeetingSortBy sortBy) {
		switch (sortBy) {
		case NAME:
			return compareByName(m1, m2);
		case TYPE:
			return compareByType(m1, m2);
		case APPROVAL:
			return compateByApproval(m1, m2);
		case DATE:
			return compareByDate(m1, m2);
		case SETUP_TIME:
			return compareBySetupTime(m1, m2);
		case TEARDOWN_TIME:
			return compareByTeardownTime(m1, m2);
		case PUBLISHED_TIME:
			return compareByPublishedTime(m1, m2);
		case ALLOCATED_TIME:
			return compareByAllocatedTime(m1, m2);
		case LOCATION:
			return compareByLocation(m1, m2);
		case CAPACITY:
			return compareByCapacity(m1, m2);
		}
		return 0;
	}
	
	protected static int compare(String s1, String s2) {
		if (s1 == null || s1.isEmpty()) {
			return (s2 == null || s2.isEmpty() ? 0 : 1);
		} else {
			return (s2 == null || s2.isEmpty() ? -1 : s1.compareToIgnoreCase(s2));
		}
	}
	
	protected static int compare(Number n1, Number n2) {
		return (n1 == null ? n2 == null ? 0 : -1 : n2 == null ? -1 : Double.compare(n1.doubleValue(), n2.doubleValue())); 
	}
}
