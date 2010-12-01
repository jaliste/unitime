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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.timetable.model.SolverParameterDef;
import org.unitime.timetable.model.SolverPredefinedSetting;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.model.dao.SolverParameterDefDAO;
import org.unitime.timetable.webutil.RequiredTimeTable;


/** 
 * @author Tomas Muller
 */
public class SolverSettingsForm extends ActionForm {
	private static final long serialVersionUID = -9205033432561871308L;
	private String op;
	private Long uniqueId;
	private String name;
	private String description;
	private String appearance;
	private Map params;
	private Map useDefaults;
	private HashSet timePrefs;

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();
        
        if(name==null || name.trim().length()==0)
            errors.add("name", new ActionMessage("errors.required", ""));
        else {
        	if ("Add New".equals(op)) {
        		SolverPredefinedSetting set = SolverPredefinedSetting.findByName(name);
        		if (set!=null)
        			errors.add("name", new ActionMessage("errors.exists", name));
        	}
        }
        
        if(description==null || description.trim().length()==0)
            errors.add("description", new ActionMessage("errors.required", ""));
        
        if(appearance==null || appearance.trim().length()==0)
            errors.add("appearance", new ActionMessage("errors.required", ""));
        
        for (Iterator i=timePrefs.iterator();i.hasNext();) {
        	Long id = (Long)i.next();
        	RequiredTimeTable rtt = TimePattern.getDefaultRequiredTimeTable();
        	if (getUseDefault(id).booleanValue()) {
            	rtt.setName("tp_def_"+id);
        	} else {
        		rtt.setName("tp_"+id);
        	}
        	rtt.update(request);
        	setParameter(id, rtt.getModel().getPreferences());
        }

        for (Iterator i=params.entrySet().iterator();i.hasNext();) {
        	Map.Entry entry = (Map.Entry)i.next();
            Long parm = (Long)entry.getKey();
        	String val = (String)entry.getValue();
        	Boolean useDefault = (Boolean)useDefaults.get(parm);
        	if (!useDefault.booleanValue() && (val==null || val.trim().length()==0))
        		errors.add("parameter["+parm+"]", new ActionMessage("errors.required", ""));
        }

        return errors;
	}

	/** 
	 * Method reset
	 * @param mapping
	 * @param request
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		name=""; description="";
		op=null; uniqueId=null;
		params = new Hashtable();
		useDefaults = new Hashtable();
		timePrefs = new HashSet();
		for (Iterator i=(new SolverParameterDefDAO()).findAll().iterator();i.hasNext();) {
			SolverParameterDef def = (SolverParameterDef)i.next();
			if (!def.isVisible().booleanValue()) continue;
			if ("boolean".equals(def.getType()))
				params.put(def.getUniqueId(),"false");
			else
				params.put(def.getUniqueId(),"");
			if ("timepref".equals(def.getType()))
				timePrefs.add(def.getUniqueId());
			useDefaults.put(def.getUniqueId(),Boolean.FALSE);
		}
	}

	/** 
	 * Method reset
	 * @param mapping
	 * @param request
	 */
	public void loadDefaults() {
		for (Iterator i=(new SolverParameterDefDAO()).findAll().iterator();i.hasNext();) {
			SolverParameterDef def = (SolverParameterDef)i.next();
			if (!def.isVisible().booleanValue()) continue;
			params.put(def.getUniqueId(),def.getDefault());
			useDefaults.put(def.getUniqueId(),Boolean.TRUE);
		}
	}
    public void loadDefaults(HttpServletRequest request) {
        for (Iterator i=(new SolverParameterDefDAO()).findAll().iterator();i.hasNext();) {
            SolverParameterDef def = (SolverParameterDef)i.next();
            if (!def.isVisible().booleanValue()) continue;
            params.put(def.getUniqueId(),(request.getParameter("parameter["+def.getUniqueId()+"]")==null?def.getDefault():request.getParameter("parameter["+def.getUniqueId()+"]")));
            useDefaults.put(def.getUniqueId(),(request.getParameter("useDefault["+def.getUniqueId()+"]")==null || "false".equals(request.getParameter("useDefault["+def.getUniqueId()+"]"))?Boolean.FALSE:Boolean.TRUE));
        }
    }

	public String getOp() { return op; }
	public void setOp(String op) { this.op = op;}
	public Long getUniqueId() { return uniqueId;}
	public void setUniqueId(Long uniqueId) { this.uniqueId = uniqueId;}
	public String getDescription() { return description;}
	public void setDescription(String description) { this.description = description;}
	public String getAppearance() { return appearance;}
	public void setAppearance(String appearance) { this.appearance = appearance;}
	public int getAppearanceIdx() {
		for (int i=0;i<SolverPredefinedSetting.sAppearances.length;i++)
			if (SolverPredefinedSetting.sAppearances[i].equals(appearance)) return i;
		return -1;
	}
	public void setAppearanceIdx(int idx) {
		if (idx<0) appearance="";
		appearance=SolverPredefinedSetting.sAppearances[idx];
	}
	public String getName() { return name;}
	public void setName(String name) { this.name = name;}
	public Boolean getUseDefault(Long id) {
		return (Boolean)useDefaults.get(id); 
	}
	public void setUseDefault(Long id, Boolean useDefault) { useDefaults.put(id,useDefault); }
	public boolean getUseDefault(int id) { return getUseDefault(new Long(id)).booleanValue(); }
	public void setUseDefault(int id, boolean useDefault) { setUseDefault(new Long(id), new Boolean(useDefault)); }
	public String getParameter(Long id) { return (String)params.get(id); }
	public void setParameter(Long id, String value) { params.put(id, value); }
	public String getParameter(int id) { return getParameter(new Long(id)); }
	public void setParameter(int id, String value) { setParameter(new Long(id), value); }
	public String[] getAppearances() { return SolverPredefinedSetting.sAppearances; }
	public Collection getEnum(String type) {
		Vector options = new Vector();
		StringTokenizer stk = new StringTokenizer(type,",");
		while (stk.hasMoreTokens()) options.add(stk.nextToken());
		return options;
	}
}

