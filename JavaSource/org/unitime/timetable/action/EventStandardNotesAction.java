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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.unitime.commons.web.Web;
import org.unitime.timetable.form.EventStandardNotesForm;

/**
 * @author Zuzana Mullerova
 */
public class EventStandardNotesAction extends Action {

	public ActionForward execute(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		EventStandardNotesForm myForm = (EventStandardNotesForm) form;
		String op = myForm.getOp();
		HttpSession session = request.getSession();
		
        if(!Web.isLoggedIn( session )) {
            throw new Exception ("Access Denied.");
        }
        
		if("Add Standard Note".equals(op)) {
			request.setAttribute("op", "add");
			return mapping.findForward("add");
		}
		
		return mapping.findForward("show");
	}
	
}
