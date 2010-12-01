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

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.form.RoomFeatureListForm;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.LookupTables;


/** 
 * MyEclipse Struts
 * Creation date: 06-27-2006
 * 
 * XDoclet definition:
 * @struts.action path="/roomFeatureSearch" name="roomFeatureListForm" input="/admin/roomFeatureSearch.jsp" scope="request"
 * @struts.action-forward name="roomFeatureList" path="/roomFeatureList.do"
 * @struts.action-forward name="showRoomFeatureSearch" path="roomFeatureSearchTile"
 * @struts.action-forward name="showRoomFeatureList" path="roomFeatureListTile"
 */
public class RoomFeatureSearchAction extends Action {

	// --------------------------------------------------------- Instance Variables

	// --------------------------------------------------------- Methods

	/** 
	 * Method execute
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response) throws Exception {
		RoomFeatureListForm roomFeatureListForm = (RoomFeatureListForm) form;
		
		//Check permissions
		HttpSession httpSession = request.getSession();
		if (!Web.isLoggedIn(httpSession)) {
			throw new Exception("Access Denied.");
		}

		User user = Web.getUser(httpSession);
		Long sessionId = (Long) user
				.getAttribute(Constants.SESSION_ID_ATTR_NAME);
		
        Object dc = roomFeatureListForm.getDeptCodeX();
        if (dc==null)
            dc = httpSession.getAttribute(Constants.DEPT_CODE_ATTR_ROOM_NAME);
        if (dc==null) {
            dc = request.getParameter("default");
            if (dc!=null)
                httpSession.setAttribute(Constants.DEPT_CODE_ATTR_ROOM_NAME, dc);
        }
		String deptCode = "";
		
		// Dept code is saved to the session - go to room feature list
		if (dc != null && !Constants.BLANK_OPTION_VALUE.equals(dc)) {
			deptCode = dc.toString();
			roomFeatureListForm.setDeptCodeX(deptCode);

			if ("Export PDF".equals(request.getParameter("op"))) {
				RoomFeatureListAction.buildPdfFeatureTable(request, roomFeatureListForm);
			}

			return mapping.findForward("roomFeatureList");
		}
		
		// No session attribute found - Load dept code
		else {
			if (user.getRole().equals(Roles.ADMIN_ROLE) || user.getCurrentRole().equals(Roles.VIEW_ALL_ROLE) || user.getCurrentRole().equals(Roles.EXAM_MGR_ROLE)) {
				//set departments
				LookupTables.setupDeptsForUser(request, user, sessionId, true);
				return mapping.findForward("showRoomFeatureSearch");
			} else {
				//get user info
				TimetableManager mgr = TimetableManager.getManager(user);

				//get depts owned by user and forward to the appropriate page
				Set mgrDepts = Department.findAllOwned(sessionId, mgr, true); 
				if (mgrDepts.size() == 0) {
					throw new Exception(
							"You do not have any department to manage. ");
				} else if (mgrDepts.size() == 1) {
					Vector labelValueDepts = new Vector();
					for (Iterator it = mgrDepts.iterator(); it.hasNext();) {
						Department d = (Department) it.next();
						String code = d.getDeptCode().trim();
						String abbv = d.getName().trim();
						if (d.isExternalManager().booleanValue()) {
							labelValueDepts.add(new LabelValueBean(code + " - " + abbv + " ("+d.getExternalMgrLabel()+")", code));
						} else {
							labelValueDepts.add(new LabelValueBean(code + " - " + abbv, code));
						}
						request.setAttribute("deptCode", code);
						roomFeatureListForm.setDeptCodeX(code);
					}
					request.setAttribute(Department.DEPT_ATTR_NAME,
							labelValueDepts);
					
					if ("Export PDF".equals(request.getParameter("op"))) {
						RoomFeatureListAction.buildPdfFeatureTable(request, roomFeatureListForm);
					}

					return mapping.findForward("roomFeatureList");
				} else {
					Vector labelValueDepts = new Vector();
					for (Iterator it = mgrDepts.iterator(); it.hasNext();) {
						Department d = (Department) it.next();
						String code = d.getDeptCode().trim();
						String abbv = d.getName().trim();
						if (d.isExternalManager().booleanValue()) {
							labelValueDepts.add(new LabelValueBean(code + " - " + abbv + " ("+d.getExternalMgrLabel()+")", code));
						} else {
							labelValueDepts.add(new LabelValueBean(code + " - " + abbv, code));
						}
					}
					request.setAttribute(Department.DEPT_ATTR_NAME,
							labelValueDepts);
					return mapping.findForward("showRoomFeatureSearch");
				}
			}

		}

	}

}

