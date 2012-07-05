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
import java.io.FileOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.cpsolver.ifs.util.DataProperties;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.SolverForm;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.remote.SolverRegisterService;
import org.unitime.timetable.solver.service.SolverService;
import org.unitime.timetable.util.Constants;


/** 
 * @author Tomas Muller
 */
@Service("/solver")
public class SolverAction extends Action {
	
	@Autowired SolverService<SolverProxy> courseTimetablingSolverService;

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		SolverForm myForm = (SolverForm) form;
		
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

        if ("n".equals(request.getParameter("confirm")))
        	op = null;
        
        if (op==null) {
        	myForm.init();
        	return mapping.findForward("showSolver");
        }
        
        SolverProxy solver = courseTimetablingSolverService.getSolver();
        
        if ("Export XML".equals(op)) {
            if (solver==null) throw new Exception("Solver is not started.");
            if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
            solver.restoreBest();
            byte[] buf = solver.exportXml();
            File file = ApplicationProperties.getTempFile("solution", "xml");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buf);
            fos.flush();fos.close();
            request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
        }
        
        if ("Restore From Best".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	solver.restoreBest();
        }
        
        if ("Save To Best".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	solver.saveBest();
        }
        
        if (op.startsWith("Save") && !op.equals("Save To Best")) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	solver.restoreBest();
        	solver.save(op.indexOf("As New")>=0, op.indexOf("Commit")>=0);
        	myForm.setChangeTab(true);
        }
        
        if ("Unload".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	courseTimetablingSolverService.removeSolver();
        	myForm.reset(mapping, request);
        	myForm.init();
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
            courseTimetablingSolverService.reload(
            		courseTimetablingSolverService.createConfig(myForm.getSetting(), myForm.getParameterValues()));
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
            User user = Web.getUser(request.getSession());
            Long sessionId = Session.getCurrentAcadSession(user).getUniqueId();
            Long settingsId = myForm.getSetting();
        	Long[] ownerId = null;
        	String solutionId = (String)request.getSession().getAttribute("Solver.selectedSolutionId");
        	if (myForm.getSelectOwner())
        		ownerId = myForm.getOwner();
        	else if (!myForm.getOwners().isEmpty()) {
        		ownerId = new Long[myForm.getOwners().size()];
        		for (int i=0;i<myForm.getOwners().size();i++)
        			ownerId[i] = ((SolverForm.LongIdValue)myForm.getOwners().elementAt(i)).getId();
        	}
    	    DataProperties config = courseTimetablingSolverService.createConfig(settingsId, myForm.getParameterValues());
    	    if (solutionId != null)
    	    	config.setProperty("General.SolutionId", solutionId);
    	    if (myForm.getHost() != null)
    	    	config.setProperty("General.Host", myForm.getHost());
    	    config.setProperty("General.SolverGroupId", ownerId);
    	    config.setProperty("General.StartSolver", new Boolean(start).toString());
    	    if (solver == null) {
        	    solver = courseTimetablingSolverService.createSolver(config);
        	} else if (start) {
        		solver.setProperties(config);
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
        
        if ("Student Sectioning".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	solver.finalSectioning();
        	myForm.setChangeTab(true);
        }
        
        if ("Export Solution".equals(op)) {
        	if (solver==null) throw new Exception("Solver is not started.");
        	if (solver.isWorking()) throw new Exception("Solver is working, stop it first.");
        	File file = ApplicationProperties.getTempFile("solution", "csv");
       		solver.export().save(file);
       		request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
       		/*
       		response.sendRedirect("temp/"+file.getName());
       		response.setContentType("text/csv");
       		*/
        }

		return mapping.findForward("showSolver");
	}

}

