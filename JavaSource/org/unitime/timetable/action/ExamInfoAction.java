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
import org.unitime.commons.web.Web;
import org.unitime.timetable.form.ExamInfoForm;
import org.unitime.timetable.interfaces.RoomAvailabilityInterface;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.exam.ui.ExamInfoModel;
import org.unitime.timetable.util.RoomAvailability;

/**
 * @author Tomas Muller
 */
public class ExamInfoAction extends Action {
    
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ExamInfoForm myForm = (ExamInfoForm) form;
        
        // Check Access
        if (!Web.isLoggedIn( request.getSession() )) {
            throw new Exception ("Access Denied.");
        }
        
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));

        ExamInfoModel model = (ExamInfoModel)request.getSession().getAttribute("ExamInfo.model");
        if (model==null) {
            model = new ExamInfoModel();
            request.getSession().setAttribute("ExamInfo.model", model);
        }

        if (op==null && model.getExam()!=null && request.getParameter("examId")==null) {
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
            model.refreshSuggestions();
        } if ("Search Deeper".equals(op)) {
            myForm.setDepth(myForm.getDepth()+1);
            myForm.save(request.getSession());
            model.refreshSuggestions();
        } else if ("Search Longer".equals(op)) {
            myForm.setTimeout(2*myForm.getTimeout());
            myForm.save(request.getSession());
            model.refreshSuggestions();
        }
        
        model.setSolver(WebSolver.getExamSolver(request.getSession()));
        
        if (request.getParameter("examId")!=null) {
            model.setExam(new ExamDAO().get(Long.valueOf(request.getParameter("examId"))));
            myForm.save(request.getSession());
        }
        
        if (model.getExam()==null) throw new Exception("No exam given.");
        
        if (RoomAvailability.getInstance()!=null && op==null) {
            Session session = Session.getCurrentAcadSession(Web.getUser(request.getSession()));
            Date[] bounds = ExamPeriod.getBounds(session, model.getExam().getExamType());
            String exclude = (model.getExam().getExamType()==org.unitime.timetable.model.Exam.sExamTypeFinal?RoomAvailabilityInterface.sFinalExamType:RoomAvailabilityInterface.sMidtermExamType);
            RoomAvailability.getInstance().activate(session,bounds[0],bounds[1],exclude,false);
            RoomAvailability.setAvailabilityWarning(request, session, model.getExam().getExamType(), true, true);
        }
        
        if ("Select".equals(op)) {
            synchronized (model) {
                if (request.getParameter("period")!=null)
                    model.setPeriod(Long.valueOf(request.getParameter("period")));
                if (request.getParameter("room")!=null)
                    model.setRooms(request.getParameter("room"));
                if (request.getParameter("suggestion")!=null)
                    model.setSuggestion(Integer.parseInt(request.getParameter("suggestion")));
                if (request.getParameter("delete")!=null)
                    model.delete(Long.valueOf(request.getParameter("delete")));
            }
        }
        
        if ("Assign".equals(op)) {
            synchronized (model) {
                String message = model.assign();
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
        
        /*
        BackTracker.markForBack(
                request,
                "examInfo.do?examId=" + model.getExam().getExamId(),
                "Exam Info ("+ model.getExam().getExamName() +")",
                true, false);
        */
        
        return mapping.findForward("show");        
    }

}
