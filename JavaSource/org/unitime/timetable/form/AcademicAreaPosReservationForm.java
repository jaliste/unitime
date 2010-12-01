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
import org.unitime.timetable.model.AcadAreaPosReservation;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.DynamicList;


/**
 * Subclasses: AcademicAreaReservation, and PosReservation forms
 * 
 * @author Heston Fernandes
 */
public class AcademicAreaPosReservationForm extends CharacteristicReservationForm {
    
    // --------------------------------------------------------- Instance Variables
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 2482481549947035012L;
	private List academicClassificationId;    

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
        return errors;        
    }

    /** 
     * Method reset
     * @param mapping
     * @param request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        academicClassificationId = DynamicList.getInstance(new ArrayList(), factoryResv);
    }

    public List getAcademicClassificationId() {
        return academicClassificationId;
    }
    public void setAcademicClassificationId(List academicClassificationId) {
        this.academicClassificationId = academicClassificationId;
    }
    public String getAcademicClassificationId(int key) {
        return academicClassificationId.get(key).toString();
    }
    public void setAcademicClassificationId(int key, Object value) {
        this.academicClassificationId.set(key, value);
    }
    
    public void addToAcademicClassificationId(String academicClassificationId) {
        this.academicClassificationId.add(academicClassificationId);
    }
    
    
   public void addBlankRows() {
       super.addBlankRows();
       for (int i=0; i<RESV_ROWS_ADDED; i++) {
           addToAcademicClassificationId(Constants.BLANK_OPTION_VALUE);
       }
    }
    
    public void clear() {
        super.clear();
        this.academicClassificationId.clear();
    }

    public void addReservation(AcadAreaPosReservation resv) {
        super.addReservation(resv);
        
        if (resv.getAcademicClassification()!=null)
            addToAcademicClassificationId(resv.getAcademicClassification().getUniqueId().toString());
        else
            addToAcademicClassificationId("");
    }

    public void removeRow(int rowNum) {
        if (rowNum>=0) {
            super.removeRow(rowNum);
            academicClassificationId.remove(rowNum);
        }
    }
}
