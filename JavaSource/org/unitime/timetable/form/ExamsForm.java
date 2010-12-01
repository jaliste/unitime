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
package org.unitime.timetable.form;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.SubjectAreaDAO;
import org.unitime.timetable.util.ComboBoxLookup;


/** 
 * @author Tomas Muller
 */
public class ExamsForm extends ActionForm {
	private static final long serialVersionUID = 8434268097497866325L;
	private String iOp = null;
	private Long iSession = null;
	private String iSubjectArea = null;
	private Collection iSubjectAreas = null;
	private Vector iSessions = null;
	private String iTable = null;
	private int iNrColumns;
	private int iNrRows;
	private int iExamType;
	private String iMessage;
	private Boolean canRetrieveAllExamForAllSubjects;
	
	private String iUser, iPassword;

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        
        return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iOp = null;
		iTable = null;
		iNrRows = iNrColumns = 0;
		iExamType = Exam.sExamTypeFinal;
		iSession = null; 
		iUser = null;
		iPassword = null;
		iMessage = null;
		canRetrieveAllExamForAllSubjects = new Boolean(false);
	}
	
	public String getOp() { return iOp; }
	public void setOp(String op) { iOp = op; }
	
	public String getSubjectArea() { return iSubjectArea; }
	public void setSubjectArea(String subjectArea) { iSubjectArea = subjectArea; } 
	public Collection getSubjectAreas() { return iSubjectAreas; }
	
	public Long getSession() { return iSession; }
	public void setSession(Long session) { iSession = session; }
	public Collection getSessions() { return iSessions; }
	
	public Boolean canDisplayAllSubjectsAtOnce(){
		Boolean displayAll = new Boolean(false); 
		if (iSession != null){
			String queryStr = "select count(e) from Exam e where e.session.uniqueId = :sessionId";
			int count = ((Number)SessionDAO.getInstance().getQuery(queryStr).setLong("sessionId", iSession).setCacheable(true).uniqueResult()).intValue();
			if (count <= 300){
				displayAll = new Boolean(true);
			}
		}
		return(displayAll);
	}

	public void load(HttpSession session) {
	    setSubjectArea(session.getAttribute("Exams.subjectArea")==null?null:(String)session.getAttribute("Exams.subjectArea"));
	    iSessions = new Vector();
        setSession(session.getAttribute("Exams.session")==null?iSessions.isEmpty()?null:Long.valueOf(((ComboBoxLookup)iSessions.lastElement()).getValue()):(Long)session.getAttribute("Exams.session"));
        boolean hasSession = false;
	    for (Iterator i=Session.getAllSessions().iterator();i.hasNext();) {
	        Session s = (Session)i.next();
	        if (s.getStatusType()!=null && (s.getStatusType().canNoRoleReportExamFinal() || s.getStatusType().canNoRoleReportExamMidterm()) && Exam.hasTimetable(s.getUniqueId())) {
	            if (s.getUniqueId().equals(getSession())) hasSession = true;
	            iSessions.add(new ComboBoxLookup(s.getLabel(),s.getUniqueId().toString()));
	        }
	    }
	    if (!hasSession) { setSession(null); setSubjectArea(null); }
	    if (getSession()==null && !iSessions.isEmpty()) setSession(Long.valueOf(((ComboBoxLookup)iSessions.lastElement()).getValue()));
	    iSubjectAreas = new TreeSet(new SubjectAreaDAO().getSession().createQuery("select distinct sa.subjectAreaAbbreviation from SubjectArea sa").setCacheable(true).list());
	    setExamType(session.getAttribute("Exams.examType")==null?iExamType:(Integer)session.getAttribute("Exams.examType"));
	    setCanRetrieveAllExamForAllSubjects(canDisplayAllSubjectsAtOnce());
	}
	    
    public void save(HttpSession session) {
        if (getSubjectArea()==null)
            session.removeAttribute("Exams.subjectArea");
        else
            session.setAttribute("Exams.subjectArea", getSubjectArea());
        session.setAttribute("Exams.examType", getExamType());
        if (getSession()==null)
            session.removeAttribute("Exams.session");
        else
            session.setAttribute("Exams.session", getSession());
    }
    
    public void setTable(String table, int cols, int rows) {
        iTable = table; iNrColumns = cols; iNrRows = rows;
    }
    
    public String getTable() { return iTable; }
    public int getNrRows() { return iNrRows; }
    public int getNrColumns() { return iNrColumns; }
    public int getExamType() { return iExamType; }
    public void setExamType(int type) { iExamType = type; }
    public Collection getExamTypes() {
    	Vector ret = new Vector(Exam.sExamTypes.length);
        for (int i=0;i<Exam.sExamTypes.length;i++) {
            ret.add(new ComboBoxLookup(ApplicationProperties.getProperty("tmtbl.exam.name.type."+Exam.sExamTypes[i],Exam.sExamTypes[i]), String.valueOf(i)));
        }
    	return ret;
    }
    
    public String getUsername() { return iUser; }
    public void setUsername(String user) { iUser = user; }
    public String getPassword() { return iPassword; }
    public void setPassword(String password) { iPassword = password; }
    public String getMessage() { return iMessage; }
    public void setMessage(String message) { iMessage = message; }
    
    public String getExamTypeLabel() {
        for (int i=0;i<Exam.sExamTypes.length;i++) {
            if (i==getExamType()) return ApplicationProperties.getProperty("tmtbl.exam.name.type."+Exam.sExamTypes[i],Exam.sExamTypes[i]).toLowerCase();
        }
        return "";
    }
    
    public String getSessionLabel() {
        if (iSessions==null) return "";
        for (Enumeration e=iSessions.elements();e.hasMoreElements();) {
            ComboBoxLookup s = (ComboBoxLookup)e.nextElement();
            if (Long.valueOf(s.getValue()).equals(getSession())) return s.getLabel();
        }
        return "";
    }

	public Boolean getCanRetrieveAllExamForAllSubjects() {
		return canRetrieveAllExamForAllSubjects;
	}

	public void setCanRetrieveAllExamForAllSubjects(
			Boolean canRetrieveAllExamForAllSubjects) {
		this.canRetrieveAllExamForAllSubjects = canRetrieveAllExamForAllSubjects;
	}
}
