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
package org.unitime.timetable.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.jsp.tagext.TagSupport;

import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.comparators.InstructionalOfferingComparator;
import org.unitime.timetable.model.dao.InstructionalOfferingDAO;


/**
 * @author Tomas Muller
 */
public class OfferingLocks extends TagSupport {
	private static CourseMessages MSG = Localization.create(CourseMessages.class);
	private static final long serialVersionUID = 7947787141769725429L;

	public String getOfferingLocksWarning(User user, Session session) {
		if (!session.getStatusType().canLockOfferings()) return null;
		List<InstructionalOffering> lockedOfferings = new ArrayList<InstructionalOffering>();
		if (session.getLockedOfferings() == null) return null;
		for (Long offeringId: session.getLockedOfferings()) {
			InstructionalOffering io = InstructionalOfferingDAO.getInstance().get(offeringId);
			if (io != null && io.isLockableBy(user))
				lockedOfferings.add(io);
		}
		if (lockedOfferings.isEmpty()) return null;
		Collections.sort(lockedOfferings, new InstructionalOfferingComparator(null));
		String course1 = null;
		String course2 = null;
		for (InstructionalOffering io: lockedOfferings) {
			if (course1 == null) {
				course1 = "<a href='instructionalOfferingDetail.do?io=" + io.getUniqueId() + "'>" + io.getCourseName() + "</a>";
			} else {
				if (course2 != null) course1 += ", " + course2;
				course2 = "<a href='instructionalOfferingDetail.do?io=" + io.getUniqueId() + "'>" + io.getCourseName() + "</a>";
			}
		}
		return (course2 == null ? MSG.lockedCourse(course1) : MSG.lockedCourses(course1, course2));
	}

	public int doStartTag() {
		try {
			User user = Web.getUser(pageContext.getSession());
			if (user==null) return SKIP_BODY;
			TimetableManager manager = TimetableManager.getManager(user);
			if (manager==null) return SKIP_BODY;
			Session acadSession = Session.getCurrentAcadSession(user);
			if (acadSession==null) return SKIP_BODY;
			String warns = getOfferingLocksWarning(user, acadSession);
			if (warns!=null) {
				pageContext.getOut().println("<table width='100%' border='0' cellpadding='3' cellspacing='0'><tr>");
				pageContext.getOut().println("<td class=\"unitime-MessageYellow\" width='5'>&nbsp;</td>");
				pageContext.getOut().println("<td class=\"unitime-MessageYellow\" >");
				pageContext.getOut().println(warns);
				pageContext.getOut().println("</td></tr></table>");
			}
			return SKIP_BODY;
		} catch (Exception e) {
			Debug.error(e);
			return SKIP_BODY;
		}
	}
	
	public int doEndTag() {
		return EVAL_PAGE;
	}


}
