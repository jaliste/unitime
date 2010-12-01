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

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.timetable.model.SolverParameterGroup;
import org.unitime.timetable.util.IdValue;


/** 
 * @author Tomas Muller
 */
public class SolverParamGroupsForm extends ActionForm {
	private static final long serialVersionUID = 8095255248431482868L;
	private String op;
    private Long uniqueId;
	private String description;
	private String name;
	private int order;
	private int type;

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

		ActionErrors errors = new ActionErrors();
        
        if(name==null || name.trim().length()==0)
            errors.add("name", new ActionMessage("errors.required", ""));
        else {
        	if ("Save".equals(op)) {
        		SolverParameterGroup gr = SolverParameterGroup.findByName(name);
        		if (gr!=null)
        			errors.add("name", new ActionMessage("errors.exists", name));
        	}
        }
        
        if(description==null || description.trim().length()==0)
            errors.add("description", new ActionMessage("errors.required", ""));
        
        return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
        op = "List";
        uniqueId = null;
        name = "";
        description = "";
        order = -1;
        type = 0;
	}

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }
    public Long getUniqueId() { return uniqueId; }
    public void setUniqueId(Long uniqueId) { this.uniqueId = uniqueId; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
    public int getOrder() { return this.order; }
    public void setOrder(int order) { this.order = order; }
    public int getType() { return this.type; }
    public void setType(int type) { this.type = type; }
    public Vector getTypes() {
        Vector ret = new Vector(3);
        ret.add(new IdValue(new Long(SolverParameterGroup.sTypeCourse), "Course Timetabling"));
        ret.add(new IdValue(new Long(SolverParameterGroup.sTypeExam), "Examination Timetabling"));
        ret.add(new IdValue(new Long(SolverParameterGroup.sTypeStudent), "Student Sectioning"));
        return ret;
    }
}
