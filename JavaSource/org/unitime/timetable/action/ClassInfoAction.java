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

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.form.ClassInfoForm;
import org.unitime.timetable.interfaces.RoomAvailabilityInterface;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.solver.course.ui.ClassInfoModel;
import org.unitime.timetable.util.DefaultRoomAvailabilityService;
import org.unitime.timetable.util.RoomAvailability;

/**
 * @author Tomas Muller
 */
public class ClassInfoAction extends Action {
    
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ClassInfoForm myForm = (ClassInfoForm) form;
        
        // Check Access
        if (!Web.isLoggedIn( request.getSession() )) {
            throw new Exception ("Access Denied.");
        }
        
        //FIXME: Only allow administrator for the time being (anything can be assigned anywhere).
        if (!Web.getUser(request.getSession()).isAdmin()) {
        	throw new Exception ("Access Denied.");
        }
        
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));
        if (request.getParameter("op2")!=null && request.getParameter("op2").length()>0) {
        	op = request.getParameter("op2");
        	myForm.setOp(op);
        }

        ClassInfoModel model = (ClassInfoModel)request.getSession().getAttribute("ClassInfo.model");
        if (model==null) {
            model = new ClassInfoModel();
            request.getSession().setAttribute("ClassInfo.model", model);
        }

        if (op==null && model.getClass()!=null && request.getParameter("classId")==null) {
            op="Apply";
        }

        if ("Apply".equals(op)) {
            myForm.save(request.getSession());
        } else if ("Refresh".equals(op)) {
            myForm.reset(mapping, request);
        }
        
        myForm.load(request.getSession());
        myForm.setModel(model);
        model.apply(request, myForm);
        
        if (op==null) {
            model.clear(TimetableManager.getManager(Web.getUser(request.getSession())));
        } else if ("Apply".equals(op)) {
            model.refreshRooms();
            if (model.isKeepConflictingAssignments()!=myForm.getKeepConflictingAssignments())
            	model.update();
        }
        
        if (request.getParameter("classId")!=null) {
            model.setClazz(new Class_DAO().get(Long.valueOf(request.getParameter("classId"))));
            if (model.getClassAssignment()!=null && (model.getChange()==null || model.getChange().getCurrent(model.getClazz().getClassId())==null))
            	model.setTime(model.getClassAssignment().getTimeId());
            myForm.save(request.getSession());
        }
        
        if (model.getClass()==null) throw new Exception("No class given.");
        
        if (RoomAvailability.getInstance()!=null && op==null && !(RoomAvailability.getInstance() instanceof DefaultRoomAvailabilityService)) {
            Session session = Session.getCurrentAcadSession(Web.getUser(request.getSession()));
            Date[] bounds = DatePattern.getBounds(session.getUniqueId());
            RoomAvailability.getInstance().activate(session,bounds[0],bounds[1],RoomAvailabilityInterface.sClassType, false);
            RoomAvailability.setAvailabilityWarning(request, session, true, true);
        }
        
        if ("Select".equals(op)) {
            synchronized (model) {
                if (request.getParameter("time")!=null)
                    model.setTime(request.getParameter("time"));
                if (request.getParameter("room")!=null)
                    model.setRooms(request.getParameter("room"));
                if (request.getParameter("delete")!=null)
                    model.delete(Long.valueOf(request.getParameter("delete")));
            }
        }
        
        if ("Assign".equals(op)) {
            synchronized (model) {
            	User user = Web.getUser(request.getSession());
                String message = model.assign(Session.getCurrentAcadSession(user).getUniqueId(), user);
                if (message==null || message.trim().length()==0) {
                    myForm.setOp("Close");
                } else {
                    myForm.setMessage(message);
                }
            }
        }

        if ("Close".equals(op)) {
            myForm.setOp("Close");
            
        }
        if (myForm.getOp() == null || myForm.getOp().equals("Close")){
        	myForm.setKeepConflictingAssignments(false);
            request.getSession().removeAttribute("ClassInfo.KeepConflictingAssignments");
        }
        
        /*
        BackTracker.markForBack(
                request,
                "ClassInfo.do?ClassId=" + model.getClass().getClassId(),
                "Class Info ("+ model.getClass().getClassName() +")",
                true, false);
        */
        
        return mapping.findForward("show");        
    }

}
