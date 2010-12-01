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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;
import org.unitime.commons.web.Web;
import org.unitime.timetable.model.TimetableManager;

/** 
 * MyEclipse Struts
 * Creation date: 01-24-2007
 * 
 * XDoclet definition:
 * @struts.form name="dataImportForm"
 */
public class DataImportForm extends ActionForm {
	private static final long serialVersionUID = 7165669008085313647L;
	private FormFile iFile;
	private String iOp;
	private boolean iExportCourses;
	private boolean iExportFinalExams;
	private boolean iExportMidtermExams;
	private boolean iExportTimetable;
	private boolean iExportCurricula;
    private boolean iEmail = false;
    private String iAddr = null;
	
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();
		
        if ("Import".equals(iOp) && (iFile == null || iFile.getFileSize()<=0)) {
        	errors.add("name", new ActionMessage("errors.required", "File") );
        }
        
        if ("Export".equals(iOp)) {
            if (!getExportCourses() && !getExportFinalExams() && !getExportMidtermExams() && !getExportTimetable() && !getExportCurricula()) {
                errors.add("export", new ActionMessage("errors.generic", "Nothing to export") );
            }
            
            if (getExportCurricula() && (getExportCourses() || getExportFinalExams() || getExportMidtermExams() || getExportTimetable())) {
            	errors.add("export", new ActionMessage("errors.generic", "Curricula need to be exported separately") );
            }
        }
        
        return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iFile = null;
		iExportCourses = false; iExportFinalExams = false; iExportMidtermExams = false; iExportTimetable = false; iExportCurricula = false;
		iEmail = false; iAddr = null;
        TimetableManager manager = TimetableManager.getManager(Web.getUser(request.getSession()));
        if (manager!=null && manager.getEmailAddress()!=null) setAddress(manager.getEmailAddress());
	}

	public FormFile getFile() { return iFile; }
	public void setFile(FormFile file) { iFile = file; }
	public String getOp() { return iOp; }
	public void setOp(String op) { iOp = op; }
	
    public boolean getExportCourses() { return iExportCourses; }
    public void setExportCourses(boolean exportCourses) { iExportCourses = exportCourses; }
    public boolean getExportFinalExams() { return iExportFinalExams; }
    public void setExportFinalExams(boolean exportFinalExams) { iExportFinalExams = exportFinalExams; }
    public boolean getExportMidtermExams() { return iExportMidtermExams; }
    public void setExportMidtermExams(boolean exportMidtermExams) { iExportMidtermExams = exportMidtermExams; }
    public boolean getExportTimetable() { return iExportTimetable; }
    public void setExportTimetable(boolean exportTimetable) { iExportTimetable = exportTimetable; }
    public boolean getExportCurricula() { return iExportCurricula; }
    public void setExportCurricula(boolean exportCurricula) { iExportCurricula = exportCurricula; }
    
    public boolean getEmail() { return iEmail; }
    public void setEmail(boolean email) { iEmail = email; }
    public String getAddress() { return iAddr; }
    public void setAddress(String addr) { iAddr = addr; }
    
    public Object clone() {
    	DataImportForm form = new DataImportForm();
    	form.iFile = iFile;
    	form.iOp = iOp;
    	form.iExportCourses= iExportCourses;
    	form.iExportFinalExams = iExportFinalExams;
    	form.iExportMidtermExams = iExportMidtermExams;
    	form.iExportTimetable = iExportTimetable;
    	form.iExportCurricula = iExportCurricula;
        form.iEmail = iEmail;
        form.iAddr = iAddr;
        return form;
    }
}

