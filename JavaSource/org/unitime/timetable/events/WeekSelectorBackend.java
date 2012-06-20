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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.unitime.timetable.gwt.client.widgets.WeekSelector.WeekSelectorRequest;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.server.GwtRpcImplementation;
import org.unitime.timetable.gwt.shared.EventInterface.WeekInterface;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.spring.SessionContext;

@Service("org.unitime.timetable.gwt.client.widgets.WeekSelector$WeekSelectorRequest")
public class WeekSelectorBackend implements GwtRpcImplementation<WeekSelectorRequest, GwtRpcResponseList<WeekInterface>> {

	@Override
	public GwtRpcResponseList<WeekInterface> execute(WeekSelectorRequest command, SessionContext context) {
		GwtRpcResponseList<WeekInterface> ret = new GwtRpcResponseList<WeekInterface>();
		Session session = SessionDAO.getInstance().get(command.getSessionId());
		Calendar c = Calendar.getInstance(Locale.US);
		c.setTime(session.getEventBeginDate());
		while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
			c.add(Calendar.DAY_OF_YEAR, -1);
		}
		int sessionYear = session.getSessionStartYear();
		DateFormat df = new SimpleDateFormat("MM/dd");
		while (!c.getTime().after(session.getEventEndDate())) {
			int dayOfYear = c.get(Calendar.DAY_OF_YEAR);
			if (c.get(Calendar.YEAR) < sessionYear) {
				Calendar x = Calendar.getInstance(Locale.US);
			    x.set(c.get(Calendar.YEAR),11,31,0,0,0);
			    dayOfYear -= x.get(Calendar.DAY_OF_YEAR);
			} else if (c.get(Calendar.YEAR) > sessionYear) {
				Calendar x = Calendar.getInstance(Locale.US);
			    x.set(sessionYear,11,31,0,0,0);
			    dayOfYear += x.get(Calendar.DAY_OF_YEAR);
			}
			WeekInterface week = new WeekInterface();
			week.setDayOfYear(dayOfYear);
			for (int i = 0; i < 7; i++) {
				week.addDayName(df.format(c.getTime()));
				c.add(Calendar.DAY_OF_YEAR, 1);
			}
			ret.add(week);
		}
		return ret;
	}

}
