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
import org.unitime.timetable.model.SponsoringOrganization;

/**
 * @author Zuzana Mullerova
 */
public class SponsoringOrgListForm extends ActionForm {

	private static final long serialVersionUID = 2371430972689067425L;

	private String iOp;
	
	public ActionErrors validate(
			ActionMapping mapping,
			HttpServletRequest request) {

			return null;
		}
	
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		
	}

	public String getTable() {

		// Create new table
		WebTable table = new WebTable( 2,
		    	    null, 
		    	    new String[] {"Name", "Email"}, 
		    	    new String[] {"left", "left"}, 
		    	    new boolean[] {true, true});    
		
	    for (Iterator i=SponsoringOrganization.findAll().iterator();i.hasNext();) {
	        SponsoringOrganization spor = (SponsoringOrganization) i.next();
			table.addLine(
		        "onclick=\"document.location='sponsoringOrgEdit.do?op=Edit&id="+spor.getUniqueId()+"';\"",
	        	new String[] {spor.getName(), spor.getEmail()},
	        	new Comparable[] {null, null});
		    }
		    
/*		    if ("Export PDF".equals(request.getParameter("op"))) {
		        File file = ApplicationProperties.getTempFile("itypes", "pdf");
		        webTable.exportPdf(file, PdfWebTable.getOrder(request.getSession(),"itypeDescList.ord"));
		        request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
		    }
		
		    String tblData = webTable.printTable(PdfWebTable.getOrder(request.getSession(),"itypeDescList.ord"));
*/
	    return (table.getLines().isEmpty()?"":table.printTable());
	}
   
    
	public String getOp() {return iOp;}
	public void setOp(String op) {iOp = op;}
	
}
