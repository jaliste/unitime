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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.cpsolver.ifs.util.CSVFile;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.defaults.SessionAttribute;
import org.unitime.timetable.form.ClassAssignmentsReportForm;
import org.unitime.timetable.form.ClassListFormInterface;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.comparators.ClassCourseComparator;
import org.unitime.timetable.model.dao.SubjectAreaDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.solver.ClassAssignmentProxy;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.service.AssignmentService;
import org.unitime.timetable.solver.service.SolverService;
import org.unitime.timetable.spring.struts.SpringAwareLookupDispatchAction;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.webutil.BackTracker;
import org.unitime.timetable.webutil.CsvClassAssignmentExport;
import org.unitime.timetable.webutil.pdf.PdfClassAssignmentReportListTableBuilder;


/**
 * @author Stephanie Schluttenhofer
 */
@Service("/classAssignmentsReportSearch")
public class ClassAssignmentsReportSearchAction extends SpringAwareLookupDispatchAction {
	
	@Autowired SessionContext sessionContext;
	
	@Autowired AssignmentService<ClassAssignmentProxy> classAssignmentService;
	
	@Autowired SolverService<ExamSolverProxy> examinationSolverService;

	protected Map getKeyMethodMap() {
	      Map map = new HashMap();
	      map.put("button.searchClasses", "searchClasses");
	      map.put("button.cancel", "searchClasses");
	      map.put("button.exportPDF", "exportPdf");
	      map.put("button.exportCSV", "exportCsv");
	      return map;
	}
	
	private void initializeFilters(HttpServletRequest request, ClassAssignmentsReportForm classListForm){
	    if ("1".equals(request.getParameter("loadFilter"))) {
            ClassAssignmentsReportSearchAction.setupGeneralFormFilters(sessionContext.getUser(), classListForm);
	    } else {
	    	sessionContext.getUser().setProperty("ClassAssignments.sortByKeepSubparts", classListForm.getSortByKeepSubparts() ? "1" : "0");
	    	sessionContext.getUser().setProperty("ClassAssignments.sortBy", classListForm.getSortBy());
	    	sessionContext.getUser().setProperty("ClassAssignments.filterAssignedRoom", classListForm.getFilterAssignedRoom());		    	
	    	//sessionContext.getUser().setProperty("ClassAssignments.filterInstructor", classListForm.getFilterInstructor());		    	
	    	sessionContext.getUser().setProperty("ClassAssignments.filterManager", classListForm.getFilterManager());		
	    	sessionContext.getUser().setProperty("ClassAssignments.filterIType", classListForm.getFilterIType());
	    	sessionContext.getUser().setProperty("ClassAssignments.filterDayCode", String.valueOf(classListForm.getFilterDayCode()));
	    	sessionContext.getUser().setProperty("ClassAssignments.filterStartSlot", String.valueOf(classListForm.getFilterStartSlot()));
	    	sessionContext.getUser().setProperty("ClassAssignments.filterLength", String.valueOf(classListForm.getFilterLength()));
	    	sessionContext.getUser().setProperty("ClassAssignments.showCrossListedClasses", String.valueOf(classListForm.getShowCrossListedClasses()));
	    }

	}
	
    
    public static void setupGeneralFormFilters(UserContext user, ClassListFormInterface form){
        form.setSortBy(user.getProperty("ClassAssignments.sortBy", ClassCourseComparator.getName(ClassCourseComparator.SortBy.NAME)));
        form.setFilterAssignedRoom(user.getProperty("ClassAssignments.filterAssignedRoom", ""));
        form.setFilterManager(user.getProperty("ClassAssignments.filterManager", ""));
        form.setFilterIType(user.getProperty("ClassAssignments.filterIType", ""));
        form.setFilterDayCode(Integer.valueOf(user.getProperty("ClassAssignments.filterDayCode", "-1")));
        form.setFilterStartSlot(Integer.valueOf(user.getProperty("ClassAssignments.filterStartSlot", "-1")));
        form.setFilterLength(Integer.valueOf(user.getProperty("ClassAssignments.filterLength", "-1")));
        form.setSortByKeepSubparts("1".equals(user.getProperty("ClassAssignments.sortByKeepSubparts", "1")));
        form.setShowCrossListedClasses("1".equals(user.getProperty("ClassAssignments.showCrossListedClasses", "0")));
    
    }
    
	/** 
	 * Method execute
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 * @throws HibernateException
	 */

	public ActionForward searchClasses(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {	
		
		return(performAction(mapping, form, request, response, "search"));
		
	}

	public ActionForward exportPdf(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		return(performAction(mapping, form, request, response, "exportPdf"));
		
	}
	
	public ActionForward exportCsv(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		return(performAction(mapping, form, request, response, "exportCsv"));
		
	}
	
	public ActionForward performAction(
			ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response,
			String action) throws Exception{
		
		if (!sessionContext.hasPermission(Right.ClassAssignments))
			throw new Exception ("Access Denied.");
        
        if (!action.equals("search") && !action.equals("exportPdf") && !action.equals("exportCsv"))
        	throw new Exception ("Unrecognized Action");
        
        ClassAssignmentsReportForm classListForm = (ClassAssignmentsReportForm) form;
        
        request.setAttribute(Department.EXTERNAL_DEPT_ATTR_NAME, Department.findAllExternal(sessionContext.getUser().getCurrentAcademicSessionId()));
        
	    this.initializeFilters(request, classListForm);
	    
	    classListForm.setSubjectAreas(SubjectArea.getUserSubjectAreas(sessionContext.getUser()));
	    classListForm.setClasses(ClassSearchAction.getClasses(classListForm, classAssignmentService.getAssignment()));
	    
		Collection classes = classListForm.getClasses();
		if (classes.isEmpty()) {
		    ActionMessages errors = new ActionMessages();
		    errors.add("searchResult", new ActionMessage("errors.generic", "No records matching the search criteria were found."));
		    saveErrors(request, errors);
		    return mapping.findForward("showClassAssignmentsReportSearch");
		} else {
			StringBuffer subjIds = new StringBuffer();
			StringBuffer ids = new StringBuffer();
			StringBuffer names = new StringBuffer();
			for (int i=0;i<classListForm.getSubjectAreaIds().length;i++) {
				if (i>0) {
					names.append(","); 
					subjIds.append(",");
					}
				ids.append("&subjectAreaIds="+classListForm.getSubjectAreaIds()[i]);
				subjIds.append(classListForm.getSubjectAreaIds()[i]);
				names.append(((new SubjectAreaDAO()).get(new Long(classListForm.getSubjectAreaIds()[i]))).getSubjectAreaAbbreviation());
			}
			sessionContext.setAttribute(SessionAttribute.ClassAssignmentsSubjectAreas, subjIds);
			if("search".equals(action)){
				BackTracker.markForBack(
						request, 
						"classAssignmentsReportSearch.do?doit=Search&loadFilter=1"+ids, 
						"Class Assignments ("+names+")", 
						true, true);
			} else if ("exportPdf".equals(action)) {
				PdfClassAssignmentReportListTableBuilder tb = new PdfClassAssignmentReportListTableBuilder();
				File outFile = tb.pdfTableForClasses(classAssignmentService.getAssignment(), examinationSolverService.getSolver(), classListForm, sessionContext);
				//if (outFile!=null) response.sendRedirect("temp/"+outFile.getName());
				if (outFile!=null) request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+outFile.getName());
				BackTracker.markForBack(
						request, 
						"classAssignmentsReportSearch.do?doit=Search&loadFilter=1"+ids, 
						"Class Assignments ("+names+")", 
						true, true);
			} else if ("exportCsv".equals(action)) {
				CSVFile csvFile = CsvClassAssignmentExport.exportCsv(sessionContext.getUser(), classListForm.getClasses(), classAssignmentService.getAssignment());
				File file = ApplicationProperties.getTempFile("classassign", "csv");
	        	csvFile.save(file);
				request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
				BackTracker.markForBack(
						request, 
						"classAssignmentsReportSearch.do?doit=Search&loadFilter=1"+ids, 
						"Class Assignments ("+names+")", 
						true, true);
	        	/*
	        	response.sendRedirect("temp/"+file.getName());
	       		response.setContentType("text/csv");
	       		*/
			} 
			return mapping.findForward("showClassAssignmentsReportList");
		}

	}
}
