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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.stereotype.Service;
import org.unitime.commons.Debug;
import org.unitime.commons.web.Web;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.ExamReportForm;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.RoomAvailability;
import org.unitime.timetable.webutil.PdfWebTable;


/** 
 * @author Tomas Muller
 */
@Service("/assignedExams")
public class AssignedExamsAction extends Action {
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ExamReportForm myForm = (ExamReportForm) form;

        // Check Access
        if (!Web.isLoggedIn( request.getSession() )) {
            throw new Exception ("Access Denied.");
        }
        
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));

        if ("Export PDF".equals(op) || "Apply".equals(op)) {
            myForm.save(request.getSession());
        } else if ("Refresh".equals(op)) {
            myForm.reset(mapping, request);
        }
        
        myForm.load(request.getSession());
        
        Session session = Session.getCurrentAcadSession(Web.getUser(request.getSession()));
        RoomAvailability.setAvailabilityWarning(request, session, myForm.getExamType(), true, false);
        
        ExamSolverProxy solver = WebSolver.getExamSolver(request.getSession());
        Collection<ExamAssignmentInfo> assignedExams = null;
        if (myForm.getSubjectArea()!=null && myForm.getSubjectArea()!=0) {
            if (solver!=null && solver.getExamType()==myForm.getExamType())
                assignedExams = solver.getAssignedExams(myForm.getSubjectArea());
            else
                assignedExams = Exam.findAssignedExams(Session.getCurrentAcadSession(Web.getUser(request.getSession())).getUniqueId(),myForm.getSubjectArea(),myForm.getExamType());
        }
        
        WebTable.setOrder(request.getSession(),"assignedExams.ord",request.getParameter("ord"),1);
        
        WebTable table = getTable(Web.getUser(request.getSession()), true, myForm, assignedExams);
        
        if ("Export PDF".equals(op) && table!=null) {
            PdfWebTable pdfTable = getTable(Web.getUser(request.getSession()), false, myForm, assignedExams);
            File file = ApplicationProperties.getTempFile("assigned", "pdf");
            pdfTable.exportPdf(file, WebTable.getOrder(request.getSession(),"assignedExams.ord"));
        	request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
        }
        
        if (table!=null)
            myForm.setTable(table.printTable(WebTable.getOrder(request.getSession(),"assignedExams.ord")), 10, assignedExams.size());
		
        if (request.getParameter("backId")!=null)
            request.setAttribute("hash", request.getParameter("backId"));

        return mapping.findForward("showReport");
	}
	
    public PdfWebTable getTable(org.unitime.commons.User user, boolean html, ExamReportForm form, Collection<ExamAssignmentInfo> exams) {
        if (exams==null || exams.isEmpty()) return null;
        String nl = (html?"<br>":"\n");
		PdfWebTable table =
            new PdfWebTable( 11,
                    "Assigned Examinations", "assignedExams.do?ord=%%",
                    new String[] {(form.getShowSections()?"Classes / Courses":"Examination"), "Period", "Room", "Seating"+nl+"Type", "Size", "Instructor", "Violated"+nl+"Distributions", "Direct", "Student N/A", ">2 A Day", "Back-To-Back"},
       				new String[] {"left", "left", "left", "center", "right", "left", "left", "right", "right", "right", "right"},
       				new boolean[] {true, true, true, true, false, true, true, false, false, false, false} );
		table.setRowStyle("white-space:nowrap");
		
        try {
        	for (ExamAssignmentInfo exam : exams) {

        	    int dc = exam.getNrDirectConflicts();
                int edc = exam.getNrNotAvailableDirectConflicts(); dc -= edc;
                String dcStr = (dc<=0?"":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+dc+"</font>":String.valueOf(dc));
                String edcStr = (edc<=0?"":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+edc+"</font>":String.valueOf(edc));
                int m2d = exam.getNrMoreThanTwoConflicts();
                String m2dStr = (m2d<=0?"":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+m2d+"</font>":String.valueOf(m2d));
                int btb = exam.getNrBackToBackConflicts();
                int dbtb = exam.getNrDistanceBackToBackConflicts();
                String btbStr = (btb<=0 && dbtb<=0?"":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+btb+(dbtb>0?" (d:"+dbtb+")":"")+"</font>":btb+(dbtb>0?" (d:"+dbtb+")":""));
                
        	    table.addLine(
        	            "onClick=\"showGwtDialog('Examination Assignment', 'examInfo.do?examId="+exam.getExamId()+"','900','90%');\"",
                        new String[] {
                            (html?"<a name='"+exam.getExamId()+"'>":"")+(form.getShowSections()?exam.getSectionName(nl):exam.getExamName())+(html?"</a>":""),
                            (html?exam.getPeriodAbbreviationWithPref():exam.getPeriodAbbreviation()),
                            (html?exam.getRoomsNameWithPref(", "):exam.getRoomsName(", ")),
                            (Exam.sSeatingTypeNormal==exam.getSeatingType()?"Normal":"Exam"),
                            String.valueOf(exam.getNrStudents()),
                            exam.getInstructorName(", "),
                            (html?exam.getDistributionConflictsHtml(", "):exam.getDistributionConflictsList(", ")),
                            dcStr,
                            edcStr,
                            m2dStr,
                            btbStr
                        },
                        new Comparable[] {
                            exam,
                            exam.getPeriodOrd(),
                            exam.getRoomsName(":"),
                            exam.getSeatingType(),
                            exam.getNrStudents(),
                            exam.getInstructorName(":"),
                            exam.getDistributionConflictsList(", "),
                            dc,
                            edc,
                            m2d,
                            btb
                        },
                        exam.getExamId().toString());
        	}
        } catch (Exception e) {
        	Debug.error(e);
        	table.addLine(new String[] {"<font color='red'>ERROR:"+e.getMessage()+"</font>"},null);
        }
        return table;
    }	
}

