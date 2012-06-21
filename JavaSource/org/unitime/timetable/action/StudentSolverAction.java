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

import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.springframework.stereotype.Service;
import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.form.StudentSolverForm;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SolverParameterGroup;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.remote.SolverRegisterService;
import org.unitime.timetable.solver.studentsct.StudentSolverProxy;


/** 
 * @author Tomas Muller
 */
@Service("/studentSolver")
public class StudentSolverAction extends Action {

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		StudentSolverForm myForm = (StudentSolverForm) form;
		
        // Check Access
        if (!Web.isLoggedIn( request.getSession() )) {
            throw new Exception ("Access Denied.");
        }
        
        try {
        	SolverRegisterService.setupLocalSolver(request.getRequestURL().substring(0,request.getRequestURL().lastIndexOf("/")),request.getServerName(),SolverRegisterService.getPort());
        } catch (Exception e) {
        	Debug.error(e);
        }

        // Read operation to be performed
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));

        StudentSolverProxy solver = WebSolver.getStudentSolver(request.getSession());
        User user = Web.getUser(request.getSession());
        TimetableManager manager = (user==null?null:TimetableManager.getManager(user));
        Session acadSession = (manager==null?null:Session.getCurrentAcadSession(user));

        if (op==null) {
        	myForm.init();
        	return mapping.findForward("showSolver");
        }

        if ("Restore From Best".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	solver.restoreBest();
        }
        
        if ("Store To Best".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	solver.saveBest();
        }
        
        if (op.startsWith("Save")) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	solver.save();
        	myForm.setChangeTab(true);
        }
        
        if ("Unload".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	WebSolver.removeStudentSolver(request.getSession());
        	myForm.reset(mapping, request);
        	myForm.init();
        }
        
        if ("Clear".equals(op)) {
            if (solver==null) throw new Exception("Solver is not started.");
            if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
            solver.clear();
            myForm.setChangeTab(true);
        }

        // Reload
        if ("Reload Input Data".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
            ActionMessages errors = myForm.validate(mapping, request);
            if(errors.size()>0) {
                saveErrors(request, errors);
                return mapping.findForward("showSolver");
            }
            Hashtable extra = new Hashtable(myForm.getParameterValues());
        	WebSolver.reloadStudentSolver(request.getSession(), myForm.getSetting(), extra);
        	myForm.setChangeTab(true);
        }
        
        if ("Start".equals(op) || "Load".equals(op)) {
        	boolean start = "Start".equals(op); 
        	if (solver!=null && solver.isWorking()) throw new Exception("Solver is working, stop it first.");
            ActionMessages errors = myForm.validate(mapping, request);
            if(errors.size()>0) {
                saveErrors(request, errors);
                return mapping.findForward("showSolver");
            }
            Long settingsId = myForm.getSetting();
        	Hashtable extra = new Hashtable(myForm.getParameterValues());
    	    if (solver == null) {
        		solver = WebSolver.createStudentSolver(acadSession.getUniqueId(),request.getSession(),settingsId,extra,start,myForm.getHost());
        	} else if (start) {
        		solver.setProperties(WebSolver.createProperties(settingsId, extra, SolverParameterGroup.sTypeStudent));
        		solver.start();
        	}
    	    myForm.setChangeTab(true);
        }
        
        if ("Stop".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isRunning()) solver.stopSolver();
        	myForm.reset(mapping, request);
        	myForm.init();
        }
        
        if ("Refresh".equals(op)) {
        	myForm.reset(mapping, request);
        	myForm.init();
        }
        
		return mapping.findForward("showSolver");
	}

}

