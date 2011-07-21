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

import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.unitime.commons.Debug;
import org.unitime.commons.web.Web;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.form.SuggestionsForm;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.TimetableSolver.AssignmentRecord;
import org.unitime.timetable.solver.TimetableSolver.RecordedAssignment;
import org.unitime.timetable.solver.interactive.ClassAssignmentDetails;
import org.unitime.timetable.solver.interactive.Hint;
import org.unitime.timetable.solver.interactive.Suggestion;
import org.unitime.timetable.solver.interactive.SuggestionsModel;


/** 
 * @author Tomas Muller
 */
public class SuggestionsAction extends Action {

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		SuggestionsForm myForm = (SuggestionsForm) form;
		try {
		
        // Check Access
        if (!Web.isLoggedIn( request.getSession() )) {
            throw new Exception ("Access Denied.");
        }
        
        SuggestionsModel model = (SuggestionsModel)request.getSession().getAttribute("Suggestions.model");
        if (model==null) {
        	model = new SuggestionsModel();
        	model.load(request.getSession());
        	request.getSession().setAttribute("Suggestions.model", model);
        }

        // Read operation to be performed
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));
        
        /*
        if (op==null)
        	throw new Exception("No operation selected.");
        */
        
        if ("Reset".equals(op) || "Select".equals(op)) {
        	String id = request.getParameter("id");
        	if (id==null)
        		throw new Exception("No class selected.");
        	Long classId = Long.valueOf(id);
        	if ("Reset".equals(op))
        		model.reset(request.getSession());
        	model.setClassId(classId);
        }
        
        if ("Apply".equals(op)) {
        	myForm.save(model);
        	model.save(request.getSession());
        }
        
        if ("Search Deeper".equals(op)) {
        	model.incDepth();
        }
        
        if ("Search Longer".equals(op)) {
        	model.doubleTimeout();
        }
        
        if ("Remove".equals(op)) {
        	String id = request.getParameter("id");
        	if (id==null)
        		throw new Exception("No class selected.");
        	model.remHint(Long.valueOf(id));
        }

        if ("Try".equals(op)) {
        	if (request.getParameter("reset")!=null)
        		model.reset(request.getSession());
        	String id = request.getParameter("id");
        	Vector roomIds = new Vector();
        	for (int idx=0;true;idx++) {
        		String room = request.getParameter("room"+idx);
        		if (room!=null)
        			roomIds.addElement(Long.valueOf(room));
        		else
        			break;
        	}
        	String days = request.getParameter("days");
        	String slot = request.getParameter("slot");
        	String pattern = request.getParameter("pattern");
        	String dates = request.getParameter("dates");
        	if (id==null)
        		throw new Exception("No class selected.");
        	model.setClassId(new Long(id));
        	if (days==null || slot==null || pattern==null)
        		throw new Exception("No time selected.");
        	if (dates==null)
        		throw new Exception("No dates selected.");
        	SolverProxy solver = WebSolver.getSolver(request.getSession());
        	if (solver==null || solver.getInfo(new Hint(Long.valueOf(id),Integer.parseInt(days),Integer.parseInt(slot),roomIds,Long.valueOf(pattern),Long.valueOf(dates)))!=null) {
        		model.addHint(Long.valueOf(id),Integer.parseInt(days),Integer.parseInt(slot),roomIds,Long.valueOf(pattern),Long.valueOf(dates));
        	} else {
        		String message = solver.getNotValidReason(new Hint(Long.valueOf(id),Integer.parseInt(days),Integer.parseInt(slot),roomIds,Long.valueOf(pattern),Long.valueOf(dates)));
        		request.setAttribute("Suggestions.currentAssignmentMessage","<script language='JavaScript'>alert('"+(message==null?"Selected placement is not valid (room or instructor not avaiable).":message)+"');</script>");
        	}
        }

        if ("Assign".equals(op)) {
        	if (model.getSelectedSuggestion()!=null)
        		model.getSelectedSuggestion().assign(request.getSession());
        	else if (model.getCurrentSuggestion()!=null)
        		model.getCurrentSuggestion().assign(request.getSession());
        	else
        		throw new Exception("Nothing to assign.");
        	model.reset(request.getSession());
        	
        	myForm.setOp("close");
        	return mapping.findForward("showSuggestions");
        }
        
        if ("Suggestion".equals(op)) {
        	String id = (String)request.getParameter("id");
        	if (id==null)
        		throw new Exception("No suggestion selected.");
        	model.selectSuggestion(Integer.parseInt(id));
        }

        if ("Placement".equals(op)) {
        	String id = (String)request.getParameter("id");
        	if (id==null)
        		throw new Exception("No placement selected.");
        	model.selectPlacement(Integer.parseInt(id));
        }
        
        if ("ShowHistory".equals(op)) {
        	model.reset(request.getSession());
        	int idx = Integer.parseInt(request.getParameter("hist"));
        	SolverProxy solver = WebSolver.getSolver(request.getSession());
        	AssignmentRecord record = (AssignmentRecord)solver.getAssignmentRecords().elementAt(idx);
        	for (Enumeration e=record.getAssignments().elements();e.hasMoreElements();) {
        		RecordedAssignment assignment = (RecordedAssignment)e.nextElement();
        		if (myForm.getId()==null) myForm.setId(assignment.getAfter()==null?assignment.getBefore().getClassId():assignment.getAfter().getClassId());
        		if (assignment.getBefore()==null) continue;
        		model.addHint(assignment.getBefore().getClassId(),assignment.getBefore().getDays(),assignment.getBefore().getStartSlot(),assignment.getBefore().getRoomIds(),assignment.getBefore().getPatternId(),assignment.getBefore().getDatePatternId());
        	}
        }

        myForm.load(model);

        ClassAssignmentDetails ca = ClassAssignmentDetails.createClassAssignmentDetails(request.getSession(),myForm.getId(),true);
        Hint newAssignment = (ca==null?null:ca.getHint());

        if (model.compute(request.getSession())) {
        	myForm.load(model);
        	
        	Hashtable confInfo = new Hashtable();
        	if (model.getSelectedSuggestion()!=null)
        		confInfo.putAll(model.getSelectedSuggestion().conflictInfo(request.getSession()));
        	if (model.getCurrentSuggestion()!=null)
        		confInfo.putAll(model.getCurrentSuggestion().conflictInfo(request.getSession()));
        	
            String selectedAssignments = getHintTable(model.getSimpleMode(),request, "Selected Assignments", model.getHints(),null);
            if (model.getHints()!=null) {
            	SolverProxy solver = WebSolver.getSolver(request.getSession());
            	confInfo.putAll(solver.conflictInfo(model.getHints()));
            	for (Enumeration e=model.getHints().elements();e.hasMoreElements();) {
            		Hint h = (Hint)e.nextElement();
            		if (ca!=null && ca.getClazz().getClassId().equals(h.getClassId()))
            			newAssignment = h;
            	}
            }
            if (selectedAssignments!=null)
            	request.setAttribute("Suggestions.selectedAssignments",selectedAssignments);
            if (model.getSelectedSuggestion()!=null) {
            	Vector ass = new Vector(model.getSelectedSuggestion().getDifferentAssignments());
            	for (Iterator i=ass.iterator();i.hasNext();) {
            		Hint h = (Hint)i.next();
            		if (ca!=null && ca.getClazz().getClassId().equals(h.getClassId()))
            			newAssignment = h;
            		boolean contains = false;
            		for (Enumeration e=model.getHints().elements();!contains && e.hasMoreElements();) {
            			Hint x = (Hint)e.nextElement();
            			if (x.equals(h)) contains = true;
            		}
            		if (contains) i.remove();
            	}
            	String selectedSuggestion =  (model.getSelectedSuggestion()==null?null:getHintTable(model.getSimpleMode(),request, "Selected Suggestion", ass, confInfo));
            	if (selectedSuggestion!=null)
            		request.setAttribute("Suggestions.selectedSuggestion",selectedSuggestion);
            }
            Suggestion s = (model.getSelectedSuggestion()!=null?model.getSelectedSuggestion():model.getCurrentSuggestion()); 
            Set conf = (s==null?null:s.getUnresolvedConflicts());
            String conflictAssignments = getHintTable(model.getSimpleMode(),request, "Conflicting Assignments", conf, confInfo);
            if (conf!=null) {
            	for (Iterator i=conf.iterator();i.hasNext();) {
            		Hint h = (Hint)i.next();
            		if (ca!=null && ca.getClazz().getClassId().equals(h.getClassId()))
            			newAssignment = null;
            	}
            }
            
            if (conflictAssignments!=null)
            	request.setAttribute("Suggestions.conflictAssignments",conflictAssignments);
            String selectedInfo = getInfoTable(model.getSimpleMode(), request,model.getEmptySuggestion(),(model.getSelectedSuggestion()!=null?model.getSelectedSuggestion():model.getCurrentSuggestion()));
            if (selectedInfo!=null)
            	request.setAttribute("Suggestions.selectedInfo",selectedInfo);
            String suggestions = getSuggestionsTable(model.getSimpleMode(),request,"Suggestions","Suggestion",model,model.getSuggestions());
            if (suggestions!=null)
            	request.setAttribute("Suggestions.suggestions",suggestions);
            String message = null;
            if (model.getTimeoutReached()) {
            	message = "("+(model.getTimeout()/1000l)+"s timeout reached, "+model.getNrCombinationsConsidered()+" possibilities up to "+model.getDepth()+" changes were considered, ";
            } else {
            	message = "(all "+model.getNrCombinationsConsidered()+" possibilities up to "+model.getDepth()+" changes were considered, ";
            }
            if (model.getSuggestions().size()==0) {
            	message += "no suggestion found)";
            } else if (model.getNrSolutions()>model.getSuggestions().size()) {
            	message += "top "+model.getSuggestions().size()+" of "+model.getNrSolutions()+" suggestions displayed)";
            } else {
            	message += model.getSuggestions().size()+" suggestions displayed)";
            }
            if (model.getNrCombinationsConsidered()==0)
            	request.removeAttribute("Suggestions.suggestions");
            	
            request.setAttribute("Suggestions.suggestionsMessage",message);
            if (model.getTryAssignments()!=null) {
            	String placements = getSuggestionsTable(model.getSimpleMode(),request,"Placements","Placement",model,model.getTryAssignments());
            	if (placements!=null) 
            		request.setAttribute("Suggestions.placements",placements);
            	
            	if (model.getNrTries()>model.getTryAssignments().size()) {
            		message = "(top "+model.getTryAssignments().size()+" of "+model.getNrTries()+" placements displayed)";
            	} else {
            		message = "(all "+model.getTryAssignments().size()+" placements displayed)";
            	}
            	request.setAttribute("Suggestions.placementsMessage",message);
            }
            
            if (model.getNrTries()==0)
            	request.removeAttribute("Suggestions.placements");
            
            if (model.getConfTable()!=null && !model.getConfTable().isEmpty()) {
            	request.setAttribute("Suggestions.confTable", getConfTable(model.getSimpleMode(),request,model,model.getConfTable()));
            }
        }
		
        if (ca!=null) {
        	String assignment = getAssignmentTable(request,ca,true,newAssignment);
        	request.setAttribute("Suggestions.assignment",ca.getClazz().getName());
        	if (assignment!=null)
        		request.setAttribute("Suggestions.assignmentInfo", assignment);
        }

        if (myForm.getId()!=null)
        	request.setAttribute("Suggestions.id", myForm.getId());
		return mapping.findForward("showSuggestions");
	    } catch (Exception e) {
	    	Debug.error(e);
	    	throw e;
	    }
	}
	
    public String getHintTable(boolean simple, HttpServletRequest request, String name, Collection hints, Hashtable confInfo) {
    	if (hints==null || hints.isEmpty()) return null;
    	boolean hasConfInfo = false;
    	if (confInfo!=null) {
        	for (Iterator i=hints.iterator();i.hasNext();) {
        		Hint hint = (Hint)i.next();
        		if (confInfo.get(hint)!=null) {
        			hasConfInfo = true;
        			break;
        		}
        	}
    	}
    	boolean remove = "Selected Assignments".equals(name);
		WebTable.setOrder(request.getSession(),"suggestions.hints.ord",request.getParameter("hord"),1);
        WebTable webTable = (hasConfInfo?
        	(simple?
        		new WebTable(6,
            			name, "suggestions.do?hord=%%",
            			new String[] {"Class", "Date", "Time", "Room", "Students", "Constraint"},
            			new String[] {"left", "left", "left", "left", "left", "left"},
            			null )
            :	new WebTable(15,
            			name, "suggestions.do?hord=%%",
            			new String[] {"Class", "Date", "Time", "Room", "Std","Tm","Rm","Dist","Ins","Usl","Big","Dept","Subp","Pert", "Constraint"},
            			new String[] {"left", "left", "left", "left", "left","right","right","right","right","right","right","right","right","right","right", "left"},
            			null )
            ):(simple?
	        		new WebTable(6,
	            			name, "suggestions.do?hord=%%",
	            			new String[] {"Class", "Date", "Time", "Room", "Students"},
	            			new String[] {"left", "left", "left", "left", "left"},
	            			null )
	            :	new WebTable(15,
	            			name, "suggestions.do?hord=%%",
	            			new String[] {"Class", "Date", "Time", "Room", "Std","Tm","Rm","Dist","Ins","Usl","Big","Dept","Subp","Pert"},
	            			new String[] {"left", "left", "left", "left", "left","right","right","right","right","right","right","right","right","right","right"},
	            			null )));
        webTable.setRowStyle("white-space:nowrap");
        try {
        	int lines = 0;
        	for (Iterator i=hints.iterator();i.hasNext();) {
        		Hint hint = (Hint)i.next();
        		lines++;
        		String conf = (confInfo==null?null:(String)confInfo.get(hint));
        		if (conf==null) conf = "";
        		ClassAssignmentDetails ca = null;
        		if ("Conflicting Assignments".equals(name))
        			ca = hint.getDetailsUnassign(request.getSession(),false);
        		else
        			ca = hint.getDetails(request.getSession(),false);
        		String remLink = null;
        		if (remove)
        			remLink = "<a href='suggestions.do?id="+ca.getClazz().getClassId()+"&op=Remove'><img src='images/Delete16.gif' border='0'></a>&nbsp;";
        		
        		String line[] = new String[hasConfInfo?(simple?6:15):(simple?5:14)];
        		Comparable cmp[] = new Comparable[hasConfInfo?(simple?6:15):(simple?5:14)];
        		int i1 = 0, i2 = 0;
        		line[i1++] = (remLink==null?"":remLink)+ca.getClassHtml();
        		line[i1++] = ca.getDaysHtml();
        		line[i1++] = ca.getTimeHtml();
        		line[i1++] = ca.getRoomHtml();
        		line[i1++] = ca.getNrStudentConflicts();
        		cmp[i2++] = ca;
        		cmp[i2++] = ca.getDaysName();
        		cmp[i2++] = ca.getTimeName();
        		cmp[i2++] = ca.getRoomName();
        		cmp[i2++] = ca.getNrStudentConflictsCmp();
        		if (!simple) {
        			line[i1++] = ca.getTimePreference();
        			line[i1++] = ca.getRoomPreference();
        			line[i1++] = ca.getGroupConstraintPref();
        			line[i1++] = ca.getBtbInstructorPreference();
        			line[i1++] = ca.getUselessHalfHours();
        			line[i1++] = ca.getIsTooBig();
        			line[i1++] = ca.getDeptBalancPenalty();
        			line[i1++] = ca.getSpreadPenalty();
        			line[i1++] = ca.getPertPenalty();
        			cmp[i2++] = ca.getTimePreferenceCmp();
        			cmp[i2++] = ca.getRoomPreferenceCmp();
        			cmp[i2++] = ca.getGroupConstraintPrefCmp();
        			cmp[i2++] = ca.getBtbInstructorPreferenceCmp();
        			cmp[i2++] = ca.getUselessHalfHoursCmp();
        			cmp[i2++] = ca.getIsTooBigCmp();
        			cmp[i2++] = ca.getSpreadPenaltyCmp();
        			cmp[i2++] = ca.getDeptBalancPenaltyCmp();
        			cmp[i2++] = ca.getPertPenaltyCmp();
        		}
        		if (hasConfInfo) {
        			line[i1++] = conf;
        			cmp[i2++] = conf;
        		}
        		
        		webTable.addLine(null, line, cmp);
        	}
        	if (lines==0) return null;
        } catch (Exception e) {
        	Debug.error(e);
        	webTable.addLine(new String[] {"<font color='red'>ERROR:"+e.getMessage()+"</font>"},null);
        }
        return webTable.printTable(WebTable.getOrder(request.getSession(),"suggestions.hints.ord"));
    }	
    
    public String getInfoTable(boolean simple, HttpServletRequest request,Suggestion oldSuggestion, Suggestion newSuggestion) {
    	if (oldSuggestion==null || newSuggestion==null) return null;
    	StringBuffer sb = new StringBuffer();
    	sb.append("<TR><TD width='5%' nowrap>Not-assigned classes:</TD><TD width='2%' nowrap>"+
    			ClassAssignmentDetails.dispNumber(newSuggestion.getUnassignedVariables(),oldSuggestion.getUnassignedVariables())+
    			"</TD></TR>");
    	if (!simple) {
    		sb.append("<TR><TD>Student conflicts:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getViolatedStudentConflicts(),oldSuggestion.getViolatedStudentConflicts())+
    				"</TD></TR>");
    		if (newSuggestion.getCommitedStudentConflicts()!=0 || oldSuggestion.getCommitedStudentConflicts()!=0)
    			sb.append("<TR><TD>Commited student conflicts:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getCommitedStudentConflicts(),oldSuggestion.getCommitedStudentConflicts())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Distance student conflicts:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getDistanceStudentConflicts(),oldSuggestion.getDistanceStudentConflicts())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Hard student conflicts:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getHardStudentConflicts(),oldSuggestion.getHardStudentConflicts())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Time preferences:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getGlobalTimePreference(),oldSuggestion.getGlobalTimePreference())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Room preferences:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getGlobalRoomPreference(),oldSuggestion.getGlobalRoomPreference())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Distribution preferences:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getGlobalGroupConstraintPreference(),oldSuggestion.getGlobalGroupConstraintPreference())+
    				"</TD></TR>");
    		sb.append("<TR><TD nowrap>Back-to-back instructor preferences:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getInstructorDistancePreference(),oldSuggestion.getInstructorDistancePreference())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Too big rooms:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getTooBigRooms(),oldSuggestion.getTooBigRooms())+
					"</TD></TR>");
    		sb.append("<TR><TD>Useless half-hours:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getUselessSlots(),oldSuggestion.getUselessSlots())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Department balancing penalty:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getDepartmentSpreadPenalty(),oldSuggestion.getDepartmentSpreadPenalty())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Same subpart balancing penalty:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getSpreadPenalty(),oldSuggestion.getSpreadPenalty())+
    				"</TD></TR>");
    		sb.append("<TR><TD>Perturbation penalty:</TD><TD nowrap>"+
    				ClassAssignmentDetails.dispNumber(newSuggestion.getPerturbationPenalty(),oldSuggestion.getPerturbationPenalty())+
    				"</TD></TR>");
    	}
    	sb.append("<TR><TD nowrap>Overall solution value:</TD><TD nowrap>"+
    			ClassAssignmentDetails.dispNumber(newSuggestion.getValue(),oldSuggestion.getValue())+
    			"</TD></TR>");
    	if (newSuggestion.hasStudentConflictInfo())
    		sb.append("<TR><TD nowrap>Student conflicts:</TD><TD colspan='2' nowrap>"+newSuggestion.getStudentConflictInfosAsHtml(request.getSession(),true,0,3)+"</TD></TR>");
    	if (newSuggestion.hasDistributionConstraintInfo() || newSuggestion.hasBtbInstructorInfo()) {
    		sb.append("<TR><TD nowrap>Violated constraints:</TD><TD colspan='2' nowrap>");
    		if (newSuggestion.hasDistributionConstraintInfo())
    			sb.append(newSuggestion.getDistributionConstraintInfoAsHtml(request.getSession(),true,0,-1));
    		if (newSuggestion.hasDistributionConstraintInfo() && newSuggestion.hasBtbInstructorInfo())
    			sb.append("<BR>");
    		if (newSuggestion.hasBtbInstructorInfo())
    			sb.append(newSuggestion.getBtbInstructorInfosAsHtml(request.getSession(),true));
    		sb.append("</TD></TR>");
    	}
    	return sb.toString();
    }
    
    public String getSuggestionsTable(boolean simple, HttpServletRequest request, String name, String op, SuggestionsModel model, Collection suggestions) {
    	//if (suggestions.isEmpty()) return null;
		WebTable.setOrder(request.getSession(),"suggestions.suggestions."+op+".ord",request.getParameter(op+"_ord"),1);
	   	WebTable webTable =
        	(simple?
       			new WebTable( 6,
       	        	name, "suggestions.do?"+op+"_ord=%%",
       				new String[] {"Score", "Class", "Date", "Time", "Room", "Students"},
       				new String[] {"left", "left", "left", "left", "left", "left"},
       				null )
        	:	new WebTable( 16,
        			name, "suggestions.do?"+op+"_ord=%%",
        			new String[] {"Score", "Class", "Date", "Time", "Room", "Conf","Std","Tm","Rm","Dist","Ins","Usl","Big","Dept","Subp","Pert"},
        			new String[] {"left", "left", "left", "left", "left","right","right","right","right","right","right","right","right","right","right","right"},
        			null ));
        webTable.setRowStyle("white-space:nowrap");
        try {
        	Suggestion empty = model.getEmptySuggestion();
        	int idx = 0;
        	for (Iterator i=suggestions.iterator();i.hasNext();idx++) {
        		Suggestion s = (Suggestion)i.next();
        		StringBuffer classes = new StringBuffer("<table colspan='0' rowspan='0' border='0'>");
                StringBuffer classesSort = new StringBuffer();
        	    StringBuffer rooms = new StringBuffer("<table colspan='0' rowspan='0' border='0'>");
        	    StringBuffer times = new StringBuffer("<table colspan='0' rowspan='0' border='0'>");
        	    StringBuffer roomsSort = new StringBuffer();
        	    StringBuffer timesSort = new StringBuffer();
        	    StringBuffer dates = new StringBuffer("<table colspan='0' rowspan='0' border='0'>");
        	    StringBuffer datesSort = new StringBuffer();
        	    for (Iterator j=s.getDifferentAssignments().iterator();j.hasNext();) {
        	    	Hint hint = (Hint)j.next();
        	    	if (model.getHints().contains(hint)) { //do not list stuff from hint
        	    		if (!"Placement".equals(op) || !model.getClassId().equals(hint.getClassId()))
        	    			continue;
        	    	}
        	    	ClassAssignmentDetails ca = hint.getDetails(request.getSession(),false);
        	    	if (classesSort.length()>0) {
                        classesSort.append(":");
                        datesSort.append(":");
        	    		roomsSort.append(":");
        	    		timesSort.append(":");
        	    	}
        	    	classes.append("<tr valign='top' height='25'><td nowrap>");
        	        rooms.append("<tr valign='top' height='25'><td nowrap>");
        	        times.append("<tr valign='top' height='25'><td nowrap>");
        	        dates.append("<tr valign='top' height='25'><td nowrap>");
        	        classes.append(ca.getClassHtml());
                    classesSort.append(ca.getClassName());
        	        times.append(ca.getTimeHtml());
        	        rooms.append(ca.getRoomHtml());
        	        dates.append(ca.getDaysHtml());
        	        timesSort.append(ca.getTimeNoHtml());
        	        roomsSort.append(ca.getRoomNoHtml());
        	        datesSort.append(ca.getDaysName());
        	        classes.append("</td></tr>");
        	        rooms.append("</td></tr>");
        	        times.append("</td></tr>");
        	        dates.append("</td></tr>");
        	    }
        	    for (Iterator j=s.getUnresolvedConflicts().iterator();j.hasNext();) {
        	    	Hint hint = (Hint)j.next();
        	    	if (model.getHints().contains(hint)) continue; //do not list stuff from hint
        	    	ClassAssignmentDetails ca = hint.getDetailsUnassign(request.getSession(),false);
        	    	if (classesSort.length()>0) {
                        classesSort.append(":");
                        datesSort.append(":");
        	    		roomsSort.append(":");
        	    		timesSort.append(":");
        	    	}
        	    	classes.append("<tr valign='top' height='25'><td nowrap>");
                    classesSort.append(ca.getClassName());
        	        rooms.append("<tr valign='top' height='25'><td nowrap>");
        	        times.append("<tr valign='top' height='25'><td nowrap>");
        	        dates.append("<tr valign='top' height='25'><td nowrap>");
        	        classes.append(ca.getClassHtml());
        	        times.append(ca.getTimeHtml());
        	        rooms.append(ca.getRoomHtml());
        	        dates.append(ca.getDaysHtml());
        	        timesSort.append(ca.getTimeNoHtml());
        	        roomsSort.append(ca.getRoomNoHtml());
        	        datesSort.append(ca.getDaysName());
        	        classes.append("</td></tr>");
        	        rooms.append("</td></tr>");
        	        times.append("</td></tr>");
        	        dates.append("</td></tr>");
        	    }
        	    if (classesSort.length()==0) continue;
        	    classes.append("</table>");
        	    rooms.append("</table>");
        	    times.append("</table>");
        	    dates.append("</table>");
        	    
        	    StringBuffer sb = new StringBuffer();
        	    if (s.getCommitedStudentConflicts()-empty.getCommitedStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumber("c",s.getCommitedStudentConflicts()-empty.getCommitedStudentConflicts()));
        	    }
        	    if (s.getDistanceStudentConflicts()-empty.getDistanceStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumber("d",s.getDistanceStudentConflicts()-empty.getDistanceStudentConflicts()));
        	    }
        	    if (s.getHardStudentConflicts()-empty.getHardStudentConflicts()!=0) {
        	    	if (sb.length()==0) sb.append(" ("); else sb.append(",");
        	    	sb.append(ClassAssignmentDetails.dispNumber("h",s.getHardStudentConflicts()-empty.getHardStudentConflicts()));
        	    }
        	    if (sb.length()>0) sb.append(")");
        	    
        	    if (simple)
            	    webTable.addLine("onClick=\"document.location='suggestions.do?id="+idx+"&op="+op+"';\"",
            	    		new String[] {
            	    			ClassAssignmentDetails.dispNumber(s.getValue()-empty.getValue()),
            	    			classes.toString(),
            	    			dates.toString(),
            	    			times.toString(),
            	    			rooms.toString(),
            	    			ClassAssignmentDetails.dispNumber(s.getViolatedStudentConflicts()-empty.getViolatedStudentConflicts())+sb
            	    			},
            	             new Comparable[] {
            	                new Double(s.getValue()-empty.getValue()),
            	                classesSort.toString(),
            	                datesSort.toString(),
            	                timesSort.toString(),
            	                roomsSort.toString(),
            	                new Long(s.getViolatedStudentConflicts()-empty.getViolatedStudentConflicts())
            	             });
        	    else
            	    webTable.addLine("onClick=\"document.location='suggestions.do?id="+idx+"&op="+op+"';\"",
            	    		new String[] {
            	    			ClassAssignmentDetails.dispNumber(s.getValue()-empty.getValue()),
            	    			classes.toString(),
            	    			dates.toString(),
            	    			times.toString(),
            	    			rooms.toString(),
            	    			ClassAssignmentDetails.dispNumber(s.getUnassignedVariables()-empty.getUnassignedVariables()),
            	                ClassAssignmentDetails.dispNumber(s.getViolatedStudentConflicts()-empty.getViolatedStudentConflicts())+sb,
            	                ClassAssignmentDetails.dispNumber(s.getGlobalTimePreference()-empty.getGlobalTimePreference()),
            	                ClassAssignmentDetails.dispNumber(s.getGlobalRoomPreference()-empty.getGlobalRoomPreference()),
            	                ClassAssignmentDetails.dispNumber(s.getGlobalGroupConstraintPreference()-empty.getGlobalGroupConstraintPreference()),
            	                ClassAssignmentDetails.dispNumber(s.getInstructorDistancePreference()-empty.getInstructorDistancePreference()),
            	                ClassAssignmentDetails.dispNumber(s.getUselessSlots()-empty.getUselessSlots()),
            	                ClassAssignmentDetails.dispNumber(s.getTooBigRooms()-empty.getTooBigRooms()),
            	                ClassAssignmentDetails.dispNumber(s.getDepartmentSpreadPenalty()-empty.getDepartmentSpreadPenalty()),
            	                ClassAssignmentDetails.dispNumber(s.getSpreadPenalty()-empty.getSpreadPenalty()),
            	                ClassAssignmentDetails.dispNumber(s.getPerturbationPenalty()-empty.getPerturbationPenalty())
            	             },
            	             new Comparable[] {
            	                new Double(s.getValue()-empty.getValue()),
            	                classes.toString(),
            	                datesSort.toString(),
            	                timesSort.toString(),
            	                roomsSort.toString(),
            	                new Integer(s.getUnassignedVariables()-empty.getUnassignedVariables()),
            	                new Long(s.getViolatedStudentConflicts()-empty.getViolatedStudentConflicts()),
            	                new Double(s.getGlobalTimePreference()-empty.getGlobalTimePreference()),
            	                new Long(s.getGlobalRoomPreference()-empty.getGlobalRoomPreference()),
            	                new Long(s.getGlobalGroupConstraintPreference()-empty.getGlobalGroupConstraintPreference()),
            	                new Long(s.getInstructorDistancePreference()-empty.getInstructorDistancePreference()),
            	                new Long(s.getUselessSlots()-empty.getUselessSlots()),
            	                new Long(s.getTooBigRooms()-empty.getTooBigRooms()),
            	                new Double(s.getDepartmentSpreadPenalty()-empty.getDepartmentSpreadPenalty()),
            	                new Double(s.getSpreadPenalty()-empty.getSpreadPenalty()),
            	                new Double(s.getPerturbationPenalty()-empty.getPerturbationPenalty())
            	             });
        	}
        } catch (Exception e) {
        	Debug.error(e);
        	webTable.addLine(new String[] {"<font color='red'>ERROR:"+e.getMessage()+"</font>"},null);
        }
        return webTable.printTable(WebTable.getOrder(request.getSession(),"suggestions.suggestions."+op+".ord"));
    }
    
    public static String getAssignmentTable(HttpServletRequest request, ClassAssignmentDetails ca, boolean dispLinks) {
    	return getAssignmentTable(request, ca, dispLinks, null, true);
    }
    
    public static String getAssignmentTable(HttpServletRequest request, ClassAssignmentDetails ca, boolean dispLinks, Hint selection) {
    	return getAssignmentTable(request, ca, dispLinks, selection, true);
    }

    public static String getAssignmentTable(HttpServletRequest request, ClassAssignmentDetails ca, boolean dispLinks, Hint selection, boolean dispDate) {
    	StringBuffer sb = new StringBuffer();
		if (ca.getTime()==null) {
			sb.append("<TR><TD colspan='2'><I>Not assigned.</I></TD></TR>");
		} else {
			if (dispDate)
				sb.append("<TR><TD>Date:</TD><TD>"+ca.getAssignedTime().getDatePatternName()+"</TD></TR>");
			sb.append("<TR><TD>Time:</TD><TD>"+ca.getAssignedTime().toHtml(false,false,true,true)+"</TD></TR>");
			if (ca.getAssignedRoom()!=null) {
				sb.append("<TR><TD>Room:</TD><TD>");
				for (int i=0;i<ca.getAssignedRoom().length;i++) {
					if (i>0) sb.append(", ");
					sb.append(ca.getAssignedRoom()[i].toHtml(false,false,true));
				}
				sb.append("</TD></TR>");
			}
		}
		if (ca.getInstructor()!=null) {
			sb.append("<TR><TD>Instructor:</TD><TD>"+ca.getInstructorHtml()+"</TD></TR>");
			if (!ca.getBtbInstructors().isEmpty()) {
				sb.append("<TR><TD></TD><TD>");
				for (Enumeration e=ca.getBtbInstructors().elements();e.hasMoreElements();) {
					ClassAssignmentDetails.BtbInstructorInfo btb = (ClassAssignmentDetails.BtbInstructorInfo)e.nextElement();
					sb.append(btb.toHtml(request.getSession()));
				}
				sb.append("</TD></TR>");
			}
		}
		if (ca.getInitialTime()!=null) {
			sb.append("<TR><TD>Initial&nbsp;Assignment:</TD><TD>");
			if (ca.isInitial()) {
				sb.append("<I>this one</I>");
			} else {
				sb.append(ca.getInitialTime().toHtml(false,false,true,true)+" ");
				for (int i=0;i<ca.getInitialRoom().length;i++) {
					if (i>0) sb.append(", ");
					sb.append(ca.getInitialRoom()[i].toHtml(false,false,true));
				}
				sb.append("</TD></TR>");
			}
			sb.append("</TD></TR>");
		}
		if (!ca.getStudentConflicts().isEmpty()) {
			sb.append("<TR><TD>Student&nbsp;Conflicts:</TD><TD>");
			Collections.sort(ca.getStudentConflicts(), new ClassAssignmentDetails.StudentConflictInfoComparator(request.getSession()));
			for (Enumeration e=ca.getStudentConflicts().elements();e.hasMoreElements();) {
				ClassAssignmentDetails.StudentConflictInfo std = (ClassAssignmentDetails.StudentConflictInfo)e.nextElement();
				sb.append(std.toHtml(request.getSession(),dispLinks));
				if (e.hasMoreElements()) sb.append("<BR>");
			}
			sb.append("</TD></TR>");
		}
		if (ca.hasViolatedGroupConstraint()) {
			sb.append("<TR><TD>Violated&nbsp;Constraints:</TD><TD>");
			for (Enumeration e=ca.getGroupConstraints().elements();e.hasMoreElements();) {
				ClassAssignmentDetails.DistributionInfo gc = (ClassAssignmentDetails.DistributionInfo)e.nextElement();
				if (gc.getInfo().isSatisfied()) continue;
				sb.append(gc.toHtml(request.getSession(), dispLinks));
				if (e.hasMoreElements()) sb.append("<BR>");
			}
			sb.append("</TD></TR>");
		}
		if (dispLinks) {
			if (!ca.getRooms().isEmpty()) {
				sb.append("<TR><TD>Room&nbsp;Locations:</TD><TD>"+ca.getRooms().toHtml(true,true,selection)+"</TD></TR>");
			} else {
				sb.append("<input type='hidden' name='nrRooms' value='0'/>");
				sb.append("<input type='hidden' name='roomState' value='0'/>");
			}
			if (!ca.getTimes().isEmpty()) {
				sb.append("<TR><TD>Time&nbsp;Locations:</TD><TD>"+ca.getTimes().toHtml(true,true,selection)+"</TD></TR>");
				sb.append("<TR"+(ca.getTimes().getNrDates() <= 1 ? " style='display:none;'" : "")+"><TD>Date&nbsp;Patterns:</TD><TD>"+ca.getTimes().toDatesHtml(true,true,selection)+"</TD></TR>");
			}
		}
		if (dispLinks && ca.getClazz()!=null && ca.getClazz().getRoomCapacity()>=0 && ca.getClazz().getRoomCapacity()<Integer.MAX_VALUE && ca.getClazz().nrRooms()>0) {
			sb.append("<TR><TD>Minimal Room Size:</TD><TD>"+ca.getClazz().getRoomCapacity()+"</TD></TR>");
		}
		if (dispLinks && ca.getClazz()!=null && ca.getClazz().getNote()!=null) {
			sb.append("<TR><TD>Note:</TD><TD>"+ca.getClazz().getNote().replaceAll("\n","<BR>")+"</TD></TR>");
		}
    	return sb.toString();
    }
    
    public String getConfTable(boolean simple, HttpServletRequest request, SuggestionsModel model, Collection suggestions) {
		WebTable.setOrder(request.getSession(),"suggestions.suggestions.conf.ord",request.getParameter("conf_ord"),1);
		boolean hasViolDistConst = false;
    	for (Iterator i=suggestions.iterator();i.hasNext();) {
    		Suggestion s = (Suggestion)i.next();
    		if (s.getGlobalGroupConstraintPreference()>0) {
    			hasViolDistConst = true;
    			break;
    		}
    	}
		WebTable webTable = (hasViolDistConst?
				new WebTable( 5,
   	        	"Conflict Table", "suggestions.do?conf_ord=%%",
   				new String[] {"Time", null, "Student Conflicts", null, "Violated Distr. Constr."},
   				new String[] {"left", "right", "left","right","left"},
   				null):
		    new WebTable( 3,
   	        	"Conflict Table", "suggestions.do?conf_ord=%%",
   				new String[] {"Time", null, "Student Conflicts"},
   				new String[] {"left", "right", "left"},
   				null));
        webTable.setRowStyle("white-space:nowrap");
        int nrLines = 0;
        try {
        	int idx = 0;
        	for (Iterator i=suggestions.iterator();i.hasNext();idx++) {
        		Suggestion s = (Suggestion)i.next();
        		if (s.getDifferentAssignments()==null || s.getDifferentAssignments().isEmpty()) continue;
        		Hint h = (Hint)s.getDifferentAssignments().firstElement();
        		ClassAssignmentDetails ca = h.getDetails(request.getSession(),false);

        		StringBuffer sb = new StringBuffer();
        		if (s.getCommitedStudentConflicts()!=0) {
        			if (sb.length()==0) sb.append(" ("); else sb.append(",");
        			sb.append("c"+s.getCommitedStudentConflicts());
        		}
        		if (s.getDistanceStudentConflicts()!=0) {
        			if (sb.length()==0) sb.append(" ("); else sb.append(",");
        			sb.append("d"+s.getDistanceStudentConflicts());
        		}
        		if (s.getHardStudentConflicts()!=0) {
        			if (sb.length()==0) sb.append(" ("); else sb.append(",");
        			sb.append("h"+s.getHardStudentConflicts());
        		}
        		if (sb.length()>0) sb.append(")");
    	    
        		nrLines++;
        		if (hasViolDistConst) {
        			webTable.addLine(null,//"onClick=\"selectTime(event, '"+ca.getAssignedTime().getDays()+"', '"+ca.getAssignedTime().getStartSlot()+"', '"+ca.getAssignedTime().getPatternId()+"');\"",
        					new String[] {
        						ca.getAssignedTime().toHtml(true,true,true,false),
        						(s.getViolatedStudentConflicts()>0?"<font color='red'>":"")+
        						s.getViolatedStudentConflicts() + sb.toString()+
        						(s.getViolatedStudentConflicts()>0?"</font>":""),
        						s.getStudentConflictInfosAsHtml(request.getSession(), true,(idx+1), 0),
        						""+s.getGlobalGroupConstraintPreference(),
        						s.getDistributionConstraintInfoAsHtml(request.getSession(),true,(idx+1), 0) 
        	    				},
        	    				new Comparable[] {
        						new Integer(-ca.getAssignedTime().getDays()*1000+ca.getAssignedTime().getStartSlot()),
        						new Long(s.getViolatedStudentConflicts()),
        						new Long(s.getViolatedStudentConflicts()),
        						new Long(s.getGlobalGroupConstraintPreference()),
        						new Long(s.getGlobalGroupConstraintPreference())
        	             	});
        		} else {
        			webTable.addLine(null,//"onClick=\"selectTime(event, '"+ca.getAssignedTime().getDays()+"', '"+ca.getAssignedTime().getStartSlot()+"', '"+ca.getAssignedTime().getPatternId()+"');\"",
        					new String[] {
        						ca.getAssignedTime().toHtml(true,true,true,false),
        						(s.getViolatedStudentConflicts()>0?"<font color='red'>":"")+
        						s.getViolatedStudentConflicts() + sb.toString()+
        						(s.getViolatedStudentConflicts()>0?"</font>":""),
        						s.getStudentConflictInfosAsHtml(request.getSession(), true,(idx+1), 0)
        	    				},
        	    				new Comparable[] {
        						new Integer(-ca.getAssignedTime().getDays()*1000+ca.getAssignedTime().getStartSlot()),
        						new Long(s.getViolatedStudentConflicts()),
        						new Long(s.getViolatedStudentConflicts())
        	             	});
        		}
        	}
        } catch (Exception e) {
        	Debug.error(e);
        	webTable.addLine(new String[] {"<font color='red'>ERROR:"+e.getMessage()+"</font>"},null);
        }
        if (nrLines==0) return null;
        return webTable.printTable(WebTable.getOrder(request.getSession(),"suggestions.suggestions.conf.ord"));
    }    
}

