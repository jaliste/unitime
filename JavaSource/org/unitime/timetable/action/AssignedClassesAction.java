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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.commons.Debug;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.defaults.SessionAttribute;
import org.unitime.timetable.form.AssignedClassesForm;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.Solution;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.dao.SolutionDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.interactive.ClassAssignmentDetails;
import org.unitime.timetable.solver.interactive.SuggestionsModel;
import org.unitime.timetable.solver.service.SolverService;
import org.unitime.timetable.solver.ui.AssignmentPreferenceInfo;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.webutil.PdfWebTable;


/** 
 * @author Tomas Muller
 */
@Service("/assignedClasses")
public class AssignedClassesAction extends Action {
	
	@Autowired SessionContext sessionContext;
	
	@Autowired SolverService<SolverProxy> courseTimetablingSolverService;
	
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		AssignedClassesForm myForm = (AssignedClassesForm) form;

        // Check Access
		if (!sessionContext.hasPermission(null, "Department", Right.AssignedClasses))
			throw new Exception ("Access Denied.");
        
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));

        SuggestionsModel model = (SuggestionsModel)request.getSession().getAttribute("Suggestions.model");
        if (model==null) {
        	model = new SuggestionsModel();
        	model.load(request.getSession());
        	request.getSession().setAttribute("Suggestions.model", model);
        }

        if ("Apply".equals(op) || "Export PDF".equals(op)) {
        	myForm.save(model);
        	model.save(request.getSession());
        }
        if ("Refresh".equals(op)) {
        	myForm.reset(mapping, request);
        }
        
        myForm.load(model);
        try {
        	myForm.setSubjectAreas(new TreeSet(SubjectArea.getSubjectAreaList(sessionContext.getUser().getCurrentAcademicSessionId())));
        } catch (Exception e) {}
        
        if ("Apply".equals(op) || "Export PDF".equals(op)) {
        	if (myForm.getSubjectArea() == null)
        		sessionContext.removeAttribute(SessionAttribute.OfferingsSubjectArea);
        	else if (myForm.getSubjectArea() < 0)
        		sessionContext.setAttribute(SessionAttribute.OfferingsSubjectArea, Constants.ALL_OPTION_VALUE);
        	else
        		sessionContext.setAttribute(SessionAttribute.OfferingsSubjectArea, myForm.getSubjectArea().toString());
        } else {
        	try {
        		Object sa = sessionContext.getAttribute(SessionAttribute.OfferingsSubjectArea);
        		if (Constants.ALL_OPTION_VALUE.equals(sa))
        			myForm.setSubjectArea(-1l);
        		else if (sa != null)
        			myForm.setSubjectArea(Long.valueOf(sa.toString()));
        	} catch (Exception e) {}
        }
        if (myForm.getSubjectArea() == null && myForm.getSubjectAreas().size() == 1) {
        	myForm.setSubjectArea(((SubjectArea)myForm.getSubjectAreas().iterator().next()).getUniqueId());
        }
        
        Vector assignedClasses = null;
        if (myForm.getSubjectArea() != null && myForm.getSubjectArea() != 0) {
        	String prefix = myForm.getSubjectArea() > 0 ? myForm.getSubjectAreaAbbv() + " " : null;
            SolverProxy solver = courseTimetablingSolverService.getSolver();
            if (solver!=null) {
            	assignedClasses = solver.getAssignedClasses(prefix);
            } else {
            	String instructorNameFormat = sessionContext.getUser().getProperty(Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
            	String solutionIdsStr = (String)request.getSession().getAttribute("Solver.selectedSolutionId");
            	assignedClasses = new Vector();
    			if (solutionIdsStr!=null && solutionIdsStr.length()>0) {
    				SolutionDAO dao = new SolutionDAO();
    				org.hibernate.Session hibSession = dao.getSession();
    				for (StringTokenizer s=new StringTokenizer(solutionIdsStr,",");s.hasMoreTokens();) {
    					Long solutionId = Long.valueOf(s.nextToken());
    					Solution solution = dao.get(solutionId, hibSession);
    					try {
    						for (Iterator i=solution.getAssignments().iterator();i.hasNext();) {
    							Assignment a = (Assignment)i.next();
    							if (prefix != null && !a.getClassName().startsWith(prefix)) continue;
    							assignedClasses.add(new ClassAssignmentDetails(solution, a, false, hibSession, instructorNameFormat));
    						}
    					} catch (ObjectNotFoundException e) {
    						hibSession.refresh(solution);
    						for (Iterator i=solution.getAssignments().iterator();i.hasNext();) {
    							Assignment a = (Assignment)i.next();
    							assignedClasses.add(new ClassAssignmentDetails(solution, a, false, hibSession, instructorNameFormat));
    						}
    					}
    				}
    			} else {
        			request.setAttribute("AssignedClasses.message","Neither a solver is started nor solution is selected.");
        			return mapping.findForward("showAssignedClasses");
    			}
            }
        } else {
			request.setAttribute("AssignedClasses.message","No subject area is selected.");
			return mapping.findForward("showAssignedClasses");
        }
        	

        
        String assignedTable = getAssignmentTable(model.getSimpleMode(),request,"Assigned Classes",assignedClasses);
        if (assignedTable!=null) {
        	request.setAttribute("AssignedClasses.table",assignedTable);
        	request.setAttribute("AssignedClasses.table.colspan",new Integer(model.getSimpleMode()?6:15));
        } else
        	request.setAttribute("AssignedClasses.message","No assigned class.");
        
        if ("Export PDF".equals(op)) {
        	File f = exportPdf(model.getSimpleMode(),request,"Assigned Classes",assignedClasses);
        	if (f!=null)
        		request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+f.getName());
        		//response.sendRedirect("temp/"+f.getName());
        }
		
        return mapping.findForward("showAssignedClasses");
	}
	
	public File exportPdf(boolean simple, HttpServletRequest request, String name, Vector assignedClasses) {
    	if (assignedClasses==null || assignedClasses.isEmpty()) return null;
        PdfWebTable webTable =
        	(simple?
       			new PdfWebTable( 6,
       	        	name, null,
       				new String[] {"Class", "Date", "Time", "Room", "Instructor", "Students"},
       				new String[] {"left", "left", "left", "left", "left", "left"},
       				null )
        	:	new PdfWebTable( 15,
        			name, null,
        			new String[] {"Class", "Date", "Time", "Room", "Instructor", "Std","Tm","Rm","Gr","Ins","Usl","Big","Dept","Subp","Pert"},
        			new String[] {"left", "left", "left", "left", "left","right","right","right","right","right","right","right","right","right","right"},
        			null ));
        
        try {
        	for (Enumeration e=assignedClasses.elements();e.hasMoreElements();) {
        		ClassAssignmentDetails ca = (ClassAssignmentDetails)e.nextElement();
        		AssignmentPreferenceInfo ci = ca.getInfo(); 

        		StringBuffer sb = new StringBuffer();
        	    if (ci.getNrCommitedStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumberNoHtml("c",ci.getNrCommitedStudentConflicts()));
        	    }
        	    if (ci.getNrDistanceStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumberNoHtml("d",ci.getNrDistanceStudentConflicts()));
        	    }
        	    if (ci.getNrHardStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumberNoHtml("h",ci.getNrHardStudentConflicts()));
        	    }
        	    if (sb.length()>0) sb.append(")");
        	    
        	    String rooms = "";
        	    if (ca.getRoom()!=null)
        	    	for (int i=0;i<ca.getRoom().length;i++) {
        	    		if (i>0) rooms += ", ";
        	    		rooms += ca.getRoom()[i].getName();
        	    	}
        	    
        	    if (simple)
            	    webTable.addLine(null,
            	    		new String[] {
            	    			ca.getClazz().getName(),
            	    			(ca.getTime()==null?ca.getAssignedTime()==null?"":ca.getAssignedTime().getDatePatternName():ca.getTime().getDatePatternName()),
            	    			(ca.getTime()==null?ca.getAssignedTime()==null?"":ca.getAssignedTime().getDaysName()+" "+ca.getTime().getStartTime()+" - "+ca.getTime().getEndTime():ca.getTime().getDaysName()+" "+ca.getTime().getStartTime()+" - "+ca.getTime().getEndTime()),
            	    			rooms,
            	    			ca.getInstructorHtml(),
            	    			ClassAssignmentDetails.dispNumberNoHtml(ci.getNrStudentConflicts())+sb
            	    			},
            	             new Comparable[] {
            	             	ca,
            	                ca.getDaysName(),
            	                ca.getTimeName(),
            	                ca.getRoomName(),
            	                ca.getInstructorName(),
            	                new Long(ci.getNrStudentConflicts())
            	             });
        	    else
            	    webTable.addLine("onClick=\"showGwtDialog('Suggestions', 'suggestions.do?id="+ca.getClazz().getClassId()+"&op=Reset','900','90%');\"",
            	    		new String[] {
    	    					ca.getClazz().getName(),
    	    					(ca.getTime()==null?ca.getAssignedTime()==null?"":ca.getAssignedTime().getDatePatternName():ca.getTime().getDatePatternName()),
    	    					(ca.getTime()==null?ca.getAssignedTime()==null?"":ca.getAssignedTime().getDaysName()+" "+ca.getTime().getStartTime()+" - "+ca.getTime().getEndTime():ca.getTime().getDaysName()+" "+ca.getTime().getStartTime()+" - "+ca.getTime().getEndTime()),
    	    					rooms,
    	    					ca.getInstructorHtml(),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getNrStudentConflicts())+sb,
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getTimePreference()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.sumRoomPreference()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getGroupConstraintPref()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getBtbInstructorPreference()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getUselessHalfHours()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getTooBigRoomPreference()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getDeptBalancPenalty()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getSpreadPenalty()),
    	    					ClassAssignmentDetails.dispNumberNoHtml(ci.getPerturbationPenalty())
            	             },
            	             new Comparable[] {
    	             			ca,
    	             			ca.getDaysName(),
    	                		ca.getTimeName(),
    	                		ca.getRoomName(),
    	                		ca.getInstructorName(),
    	                		new Long(ci.getNrStudentConflicts()),
            	                new Double(ci.getTimePreference()),
            	                new Long(ci.sumRoomPreference()),
            	                new Long(ci.getGroupConstraintPref()),
            	                new Long(ci.getBtbInstructorPreference()),
            	                new Long(ci.getUselessHalfHours()),
            	                new Long(ci.getTooBigRoomPreference()),
            	                new Double(ci.getDeptBalancPenalty()),
            	                new Double(ci.getSpreadPenalty()),
            	                new Double(ci.getPerturbationPenalty())
            	             });
        	}
        	File file = ApplicationProperties.getTempFile("assigned", "pdf");
        	webTable.exportPdf(file, WebTable.getOrder(sessionContext,"assignedClasses.ord"));
        	return file;
        } catch (Exception e) {
        	Debug.error(e);
        }
        return null;
	}

    public String getAssignmentTable(boolean simple, HttpServletRequest request, String name, Vector assignedClasses) {
    	if (assignedClasses==null || assignedClasses.isEmpty()) return null;
		WebTable.setOrder(sessionContext,"assignedClasses.ord",request.getParameter("ord"),1);
        WebTable webTable =
        	(simple?
       			new WebTable( 6,
       	        	name, "assignedClasses.do?ord=%%",
       				new String[] {"Class", "Date", "Time", "Room", "Instructor", "Students"},
       				new String[] {"left", "left", "left", "left", "left", "left"},
       				null )
        	:	new WebTable( 15,
        			name, "assignedClasses.do?ord=%%",
        			new String[] {"Class", "Date", "Time", "Room", "Instructor", "Std","Tm","Rm","Gr","Ins","Usl","Big","Dept","Subp","Pert"},
        			new String[] {"left", "left", "left", "left", "left","right","right","right","right","right","right","right","right","right","right"},
        			null ));
        webTable.setRowStyle("white-space:nowrap");
        try {
        	for (Enumeration e=assignedClasses.elements();e.hasMoreElements();) {
        		ClassAssignmentDetails ca = (ClassAssignmentDetails)e.nextElement();
        		AssignmentPreferenceInfo ci = ca.getInfo(); 

        		StringBuffer sb = new StringBuffer();
        	    if (ci.getNrCommitedStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumber("c",ci.getNrCommitedStudentConflicts()));
        	    }
        	    if (ci.getNrDistanceStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumber("d",ci.getNrDistanceStudentConflicts()));
        	    }
        	    if (ci.getNrHardStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumber("h",ci.getNrHardStudentConflicts()));
        	    }
        	    if (sb.length()>0) sb.append(")");
        	    
        	    String rooms = "";
        	    if (ca.getRoom()!=null)
        	    	for (int i=0;i<ca.getRoom().length;i++) {
        	    		if (i>0) rooms += ", ";
        	    		rooms += ca.getRoom()[i].toHtml(false,false,true);
        	    	}
        	    
        	    if (simple)
            	    webTable.addLine("onClick=\"showGwtDialog('Suggestions', 'suggestions.do?id="+ca.getClazz().getClassId()+"&op=Reset','900','90%');\"",
            	    		new String[] {
            	    			ca.getClazz().toHtml(true,true),
            	    			ca.getDaysHtml(),
            	    			ca.getTime().toHtml(false,false,true,true),
            	    			rooms,
            	    			ca.getInstructorHtml(),
            	    			ClassAssignmentDetails.dispNumber(ci.getNrStudentConflicts())+sb
            	    			},
            	             new Comparable[] {
            	             	ca,
            	                ca.getDaysName(),
            	                ca.getTimeName(),
            	                ca.getRoomName(),
            	                ca.getInstructorName(),
            	                new Long(ci.getNrStudentConflicts())
            	             });
        	    else
            	    webTable.addLine("onClick=\"showGwtDialog('Suggestions', 'suggestions.do?id="+ca.getClazz().getClassId()+"&op=Reset','900','90%');\"",
            	    		new String[] {
    	    					ca.getClazz().toHtml(true,true),
    	    					ca.getDaysHtml(),
    	    					ca.getTime().toHtml(false,false,true,true),
    	    					rooms,
    	    					ca.getInstructorHtml(),
    	    					ClassAssignmentDetails.dispNumber(ci.getNrStudentConflicts())+sb,
            	                ClassAssignmentDetails.dispNumber(ci.getTimePreference()),
            	                ClassAssignmentDetails.dispNumber(ci.sumRoomPreference()),
            	                ClassAssignmentDetails.dispNumber(ci.getGroupConstraintPref()),
            	                ClassAssignmentDetails.dispNumber(ci.getBtbInstructorPreference()),
            	                ClassAssignmentDetails.dispNumber(ci.getUselessHalfHours()),
            	                ClassAssignmentDetails.dispNumber(ci.getTooBigRoomPreference()),
            	                ClassAssignmentDetails.dispNumber(ci.getDeptBalancPenalty()),
            	                ClassAssignmentDetails.dispNumber(ci.getSpreadPenalty()),
            	                ClassAssignmentDetails.dispNumber(ci.getPerturbationPenalty())
            	             },
            	             new Comparable[] {
    	             			ca,
    	             			ca.getDaysName(),
    	                		ca.getTimeName(),
    	                		ca.getRoomName(),
    	                		ca.getInstructorName(),
    	                		new Long(ci.getNrStudentConflicts()),
            	                new Double(ci.getTimePreference()),
            	                new Long(ci.sumRoomPreference()),
            	                new Long(ci.getGroupConstraintPref()),
            	                new Long(ci.getBtbInstructorPreference()),
            	                new Long(ci.getUselessHalfHours()),
            	                new Long(ci.getTooBigRoomPreference()),
            	                new Double(ci.getDeptBalancPenalty()),
            	                new Double(ci.getSpreadPenalty()),
            	                new Double(ci.getPerturbationPenalty())
            	             });
        	}
        } catch (Exception e) {
        	Debug.error(e);
        	webTable.addLine(new String[] {"<font color='red'>ERROR:"+e.getMessage()+"</font>"},null);
        }
        return webTable.printTable(WebTable.getOrder(sessionContext,"assignedClasses.ord"));
    }	
}

