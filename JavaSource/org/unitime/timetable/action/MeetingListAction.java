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

package org.unitime.timetable.action;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.springframework.stereotype.Service;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.form.MeetingListForm;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.webutil.BackTracker;
import org.unitime.timetable.webutil.CalendarEventTableBuilder;
import org.unitime.timetable.webutil.CsvEventTableBuilder;
import org.unitime.timetable.webutil.pdf.PdfEventTableBuilder;

/**
 * @author Tomas Muller
 */
@Service("/meetingList")
public class MeetingListAction extends Action {

	public ActionForward execute(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		MeetingListForm myForm = (MeetingListForm)form;
		
        User user = Web.getUser(request.getSession()); 
        if (user==null || !TimetableManager.canSeeEvents(user)) 
        	throw new Exception ("Access Denied.");

        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));
        if (!("Search".equals(op) || "Export PDF".equals(op)
				|| "Add Event".equals(op) || "iCalendar".equals(op) || "Export CSV".equals(op))){
			op = null;
		}

        if ("Search".equals(op) || "Export PDF".equals(op) || "Export CSV".equals(op)) {
        	ActionMessages errors = myForm.validate(mapping, request);
        	if (!errors.isEmpty()) saveErrors(request, errors);
        	else myForm.save(request.getSession());
        } else myForm.load(request.getSession());

        if ("Add Event".equals(op)) {
            return mapping.findForward("addEvent");
        }
        
        if ("Export PDF".equals(op)) {
            File pdfFile = new PdfEventTableBuilder().pdfTableForMeetings(myForm);
            if (pdfFile!=null) request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+pdfFile.getName());
        }

        if ("Export CSV".equals(op)) {
            File csvFile = new CsvEventTableBuilder().csvTableForMeetings(myForm);
            if (csvFile!=null) request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+csvFile.getName());
        }

        if ("iCalendar".equals(op)) {
            String url = new CalendarEventTableBuilder().calendarUrlForMeetings(myForm);
            if (url!=null) request.setAttribute(Constants.REQUEST_OPEN_URL, url);
            /*
            File pdfFile = new CalendarEventTableBuilder().calendarTableForMeetings(myForm);
            if (pdfFile!=null) request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+pdfFile.getName());
            */
        }

        if (request.getParameter("backId")!=null)
            request.setAttribute("hash", request.getParameter("backId"));

        BackTracker.markForBack(
                request, 
                "meetingList.do",
                "Meetings", 
                true, true);

        return mapping.findForward("show");

	}
}
