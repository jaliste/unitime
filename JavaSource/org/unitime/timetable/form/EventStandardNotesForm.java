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

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.model.StandardEventNote;

/**
 * @author Zuzana Mullerova
 */
public class EventStandardNotesForm extends ActionForm {

	private static final long serialVersionUID = 4764358302157565376L;
	private String iOp;
	
	public ActionErrors validate(
			ActionMapping mapping,
			HttpServletRequest request) {

			return null;
		}
	
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		
	}

	public String getTable() {

		WebTable table = new WebTable( 2,
					null, 
					new String[] {"Reference", "Note"}, 
		    	    new String[] {"left", "left"}, 
		    	    new boolean[] {true, true});    
		
	    for (Iterator i=StandardEventNote.findAll().iterator();i.hasNext();) {
	    	StandardEventNote sen = (StandardEventNote) i.next();
			table.addLine(
		        "onclick=\"document.location='eventStandardNoteEdit.do?op=Edit&id="+sen.getUniqueId()+"';\"",
	        	new String[] {sen.getReference(), sen.getNote().replaceAll("\\n", "<br>")},
	        	new Comparable[] {null, null});
		    }

	    return (table.getLines().isEmpty()?"":table.printTable());
	}
   
    
	public String getOp() {return iOp;}
	public void setOp(String op) {iOp = op;}
	
}
