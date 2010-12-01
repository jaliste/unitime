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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.timetable.model.StudentGroupReservation;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.DynamicList;


/** 
 * MyEclipse Struts
 * Creation date: 09-01-2006
 * 
 * XDoclet definition:
 * @struts:form name="studentGroupReservationEditForm"
 */
public class StudentGroupReservationEditForm extends CharacteristicReservationForm {

	private static final long serialVersionUID = 6580680514337481241L;

	// --------------------------------------------------------- Instance Variables
    
	private List studentGroupId;

    // --------------------------------------------------------- Methods

    /** 
     * Method validate
     * @param mapping
     * @param request
     * @return ActionErrors
     */
    public ActionErrors validate(
        ActionMapping mapping,
        HttpServletRequest request) {

        ActionErrors errors = super.validate(mapping, request);
        
        if(!checkList(studentGroupId, true)) {
            errors.add("studentGroupId", 
                    new ActionMessage(
                            "errors.generic", 
                            "Invalid Student Group: Check for duplicate / blank values. ") );
        }        
        
        return errors;        
    }

    /** 
     * Method reset
     * @param mapping
     * @param request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        studentGroupId = DynamicList.getInstance(new ArrayList(), factoryResv);;
    }
    
    public List getStudentGroupId() {
        return studentGroupId;
    }
    public void setStudentGroupId(List studentGroupId) {
        this.studentGroupId = studentGroupId;
    }
    public String getStudentGroupId(int key) {
        return studentGroupId.get(key).toString();
    }
    public void setStudentGroupId(int key, Object value) {
        this.studentGroupId.set(key, value);
    }
    
    public void addToStudentGroupId(String studentGroupId) {
        this.studentGroupId.add(studentGroupId);
    }
    
    
   public void addBlankRows() {
       super.addBlankRows();
       for (int i=0; i<RESV_ROWS_ADDED; i++) {
           addToStudentGroupId(Constants.BLANK_OPTION_VALUE);
       }
    }
    
    public void clear() {
        super.clear();
        this.studentGroupId.clear();
    }

    public void addReservation(StudentGroupReservation resv) {
        super.addReservation(resv);
        addToStudentGroupId(resv.getStudentGroup().getUniqueId().toString());
    }
    
    public void removeRow(int rowNum) {
        if (rowNum>=0) {
            super.removeRow(rowNum);
            studentGroupId.remove(rowNum);
        }
    }
}
