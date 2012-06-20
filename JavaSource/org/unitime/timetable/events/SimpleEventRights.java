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
package org.unitime.timetable.events;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.EventInterface.EventType;
import org.unitime.timetable.gwt.shared.PageAccessException;
import org.unitime.timetable.model.Event;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.spring.SessionContext;
import org.unitime.timetable.spring.UserContext;

public class SimpleEventRights implements EventRights {
	private static final long serialVersionUID = 1L;
	
	protected static GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	private UserContext iUser;
	private boolean iHttpSessionNew = false;
	private Date iToday, iBegin, iEnd;
	private Long iSessionId;
	
	public SimpleEventRights(UserContext user, boolean isHttpSessionNew, Long sessionId) {
		iUser = user;
		iHttpSessionNew = isHttpSessionNew;
		iSessionId = sessionId;
		Calendar cal = Calendar.getInstance(Localization.getJavaLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		iToday = cal.getTime();
		
		Session session = SessionDAO.getInstance().get(iSessionId);
		if (session != null) {
			iBegin = session.getEventBeginDate();
			cal.setTime(session.getEventEndDate());
			cal.add(Calendar.DAY_OF_YEAR, 1);
			iEnd = cal.getTime();
		}
	}
	
	public SimpleEventRights(SessionContext context, Long sessionId) {
		this(context.getUser(), context.isHttpSessionNew(), (sessionId != null ? sessionId : context.isAuthenticated() ? context.getUser().getCurrentAcademicSessionId() : null));
	}
	
	protected UserContext getUser() {
		return iUser;
	}
	
	protected boolean isHttpSessionNew() {
		return iHttpSessionNew;
	}
	
	protected boolean isAdmin() {
		return getUser() != null && Roles.ADMIN_ROLE.equals(getUser().getCurrentRole());
	}
	
	protected boolean isAuthenticated() {
		return getUser() != null;
	}
	
	protected boolean hasRole() {
		return getUser() != null && getUser().getCurrentRole() != null && hasSession();
	}
	
	private Set<Long> iManagedSessions = null;
	protected boolean hasSession() {
		if (iManagedSessions == null)
			iManagedSessions = new HashSet<Long>(SessionDAO.getInstance().getSession().createQuery(
					"select distinct d.session.uniqueId from TimetableManager m inner join m.departments d where m.externalUniqueId = :userId")
					.setString("userId", getUserId()).setCacheable(true).list());
		return iManagedSessions.contains(iSessionId);
	}
	
	protected boolean isEventManager() {
		return getUser() != null && Roles.EVENT_MGR_ROLE.equals(getUser().getCurrentRole()) && hasSession();
	}
	
	protected boolean isStudentAdvisor() {
		return getUser() != null && Roles.STUDENT_ADVISOR.equals(getUser().getCurrentRole()) && hasSession();
	}

	protected boolean isScheduleManager() {
		return getUser() != null && Roles.DEPT_SCHED_MGR_ROLE.equals(getUser().getCurrentRole()) && hasSession();
	}

	protected String getUserId() {
		return getUser() == null ? null : getUser().getExternalUserId();
	}
	
	@Override
	public PageAccessException getException() {
		if (!isAuthenticated())
			return new PageAccessException(isHttpSessionNew() ? MESSAGES.authenticationExpired() : MESSAGES.authenticationRequired());
		return new PageAccessException(MESSAGES.authenticationInsufficient());
	}
	
	private Set<Long> iManagedRooms = null;
	protected boolean isLocationManager(Long locationId) {
		if (isAdmin()) {
			return true;
		} else if (isEventManager()) {
			if (iManagedRooms == null)
				iManagedRooms = new HashSet<Long>(SessionDAO.getInstance().getSession().createQuery(
						"select distinct l.uniqueId " +
						"from Location l inner join l.roomDepts rd inner join rd.department.timetableManagers m " +
						"where m.externalUniqueId = :userId and rd.control = true and l.session.uniqueId = :sessionId")
						.setString("userId", getUserId()).setLong("sessionId", iSessionId)
						.setCacheable(true).list());
			return (locationId == null ? !iManagedRooms.isEmpty() : iManagedRooms.contains(locationId));
		}
		return false;
	}
	
	private Set<Long> iEventRooms = null;
	@Override
	public boolean isEventLocation(Long locationId) {
		if (iEventRooms == null)
			iEventRooms = new HashSet<Long>(SessionDAO.getInstance().getSession().createQuery(
					"select l.uniqueId " +
					"from Location l inner join l.roomDepts rd inner join rd.department.timetableManagers m inner join m.managerRoles mr, RoomTypeOption o " +
					"where rd.control = true and mr.role.reference = :eventMgr and o.status = 1 and o.roomType = l.roomType and o.session = l.session and l.session.uniqueId = :sessionId")
					.setString("eventMgr", Roles.EVENT_MGR_ROLE)
					.setLong("sessionId", iSessionId).setCacheable(true).list());
		return (locationId == null ? !iEventRooms.isEmpty() : iEventRooms.contains(locationId));
	}
	
	@Override
	public boolean canOverbook(Long locationId) {
		return isLocationManager(locationId);
	}

	@Override
	public boolean canCreate(Long locationId) {
		return isAuthenticated() && isEventLocation(locationId);
	}

	@Override
	public boolean canApprove(Long locationId) {
		return isLocationManager(locationId);
	}

	@Override
	public void checkAccess() throws PageAccessException {
		if (!isAuthenticated() && "true".equals(ApplicationProperties.getProperty("unitime.event_timetable.requires_authentication", "true")))
			throw getException();
	}

	@Override
	public boolean canSee(Event event) {
		// Owner of the event can always see the details
		if (isAuthenticated() && event.getMainContact() != null && getUserId().equals(event.getMainContact().getExternalUniqueId()))
			return true;

		// Admin and event manager can see details of any event
		if (isAdmin() || isEventManager())
			return true;
		
		return false;
	}
	
	@Override
	public boolean isPastOrOutside(Date date) {
		if (date == null) return true;
		
		if (iBegin != null && date.before(iBegin)) return true;
		if (iEnd != null && !date.before(iEnd)) return true;
		
		return date == null || date.before(iToday);
	}
		
	public boolean canEdit(Event event) {
		// Examination and class events cannot be edited just yet
		switch (event.getEventType()) {
		case Event.sEventTypeClass:
		case Event.sEventTypeFinalExam:
		case Event.sEventTypeMidtermExam:
			return false;
		}

		// Otherwise, user can edit (e.g., add new meetings) if he/see can see the details
		return canSee(event);
	}

	@Override
	public boolean canEdit(Meeting meeting) {
		// Past meetings cannot be edited
		if (isPastOrOutside(meeting.getStartTime())) return false;
		
		// Examination and class events cannot be edited just yet
		switch (meeting.getEvent().getEventType()) {
		case Event.sEventTypeClass:
		case Event.sEventTypeFinalExam:
		case Event.sEventTypeMidtermExam:
			return false;
		}
		
		// Owner of the event can edit the meeting
		if (isAuthenticated() && meeting.getEvent().getMainContact() != null && getUserId().equals(meeting.getEvent().getMainContact().getExternalUniqueId()))
			return true;
		
		// Admin or event manager can edit if no location, or if the location is managed by the user
		if (isAdmin() || isEventManager()) {
			Location location = meeting.getLocation();
			return location == null || isEventLocation(location.getUniqueId());
		}
		
		return false;
	}
	
	@Override
	public boolean canApprove(Meeting meeting) {
		// Past meetings cannot be edited
		if (isPastOrOutside(meeting.getStartTime())) return false;
		
		// No approval for examination and class events
		switch (meeting.getEvent().getEventType()) {
		case Event.sEventTypeClass:
		case Event.sEventTypeFinalExam:
		case Event.sEventTypeMidtermExam:
			return false;
		}

		// Admin or event manager can approve if no location, or if the location is managed by the user
		if (isAdmin() || isEventManager()) {
			Location location = meeting.getLocation();
			return location == null || isEventLocation(location.getUniqueId());
		}
		
		return false;
	}

	@Override
	public boolean canAddEvent(EventType type, String userId) {
		// Not authenticated
		if (!isAuthenticated()) return false;
		
		// All dates are in the past
		if (iEnd == null || !iToday.before(iEnd)) return false;
		
		// No event room
		if (!isAdmin() && !isEventLocation(null)) return false;
		
		// Only admins and event managers can create an event on behalf of someone else
		if (userId != null && !isAdmin() && !isEventManager() && !userId.equals(getUserId())) return false;
		
		// Default event type to Special Event
		if (type == null) type = EventType.Special;
		
		switch (type) {
		case Special: return true;
		case Course: return isAdmin() || isEventManager();
		default: return false;
		}
	}

	@Override
	public boolean canLookupContacts() {
		return isAdmin() || isEventManager();
	}

	@Override
	public boolean canSeeSchedule(String userId) {
		return isAdmin() || isScheduleManager() || isStudentAdvisor() || (userId != null && userId.equals(getUserId()));
	}

}
