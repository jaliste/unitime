/*
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org, and individual contributors
 * as indicated by the @authors tag.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package org.unitime.timetable.form;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.unitime.timetable.model.ExactTimeMins;
import org.unitime.timetable.model.dao.ExactTimeMinsDAO;


/** 
 * @author Tomas Muller
 */
public class ExactTimeEditForm extends ActionForm {
    private String iOp;
    private Vector iExactTimeMins = new Vector();

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();
        
		return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iOp = null; 
		iExactTimeMins.clear();
		iExactTimeMins.addAll((new ExactTimeMinsDAO()).findAll());
		Collections.sort(iExactTimeMins);
	}
	
	public void save() {
		org.hibernate.Session hibSession = (new ExactTimeMinsDAO()).getSession(); 
		for (Enumeration e=iExactTimeMins.elements();e.hasMoreElements();) {
			ExactTimeMins ex = (ExactTimeMins)e.nextElement();
			hibSession.saveOrUpdate(ex);
		}
		hibSession.flush();
	}

	public String getOp() { return iOp; }
	public void setOp(String op) { iOp = op; }
	public int getSize() { return iExactTimeMins.size(); }
	public Vector getExactTimeMins() {
		return iExactTimeMins;
	}
	public int getMinsPerMtgMin(int idx) {
		return ((ExactTimeMins)iExactTimeMins.elementAt(idx)).getMinsPerMtgMin().intValue();
	}
	public int getMinsPerMtgMax(int idx) {
		return ((ExactTimeMins)iExactTimeMins.elementAt(idx)).getMinsPerMtgMax().intValue();
	}
	public int getNrTimeSlots(int idx) {
		return ((ExactTimeMins)iExactTimeMins.elementAt(idx)).getNrSlots().intValue();
	}
	public int getBreakTime(int idx) {
		return ((ExactTimeMins)iExactTimeMins.elementAt(idx)).getBreakTime().intValue();
	}
	public void setMinsPerMtgMin(int idx, int minsPerMtgMin) {
		((ExactTimeMins)iExactTimeMins.elementAt(idx)).setMinsPerMtgMin(new Integer(minsPerMtgMin));
	}
	public void setMinsPerMtgMax(int idx, int minsPerMtgMax) {
		((ExactTimeMins)iExactTimeMins.elementAt(idx)).setMinsPerMtgMax(new Integer(minsPerMtgMax));
	}
	public void setNrTimeSlots(int idx, int nrTimeSlots) {
		((ExactTimeMins)iExactTimeMins.elementAt(idx)).setNrSlots(new Integer(nrTimeSlots));
	}
	public void setBreakTime(int idx, int breakTime) {
		((ExactTimeMins)iExactTimeMins.elementAt(idx)).setBreakTime(new Integer(breakTime));
	}
}

