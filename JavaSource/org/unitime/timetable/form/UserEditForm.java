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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.timetable.authenticate.jaas.DbAuthenticateModule;
import org.unitime.timetable.model.User;

/** 
 * 
 * @author Tomas Muller
 * 
 */
public class UserEditForm extends ActionForm {
	private String iOp = null;
    private String iExternalId = null;
    private String iName = null;
    private String iPassword = null;

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        
        if (iExternalId ==null || iExternalId.trim().length()==0)
            errors.add("externalId", new ActionMessage("errors.required", ""));
        else if (!"Update".equals(getOp()) && User.findByExternalId(getExternalId())!=null) {
            errors.add("externalId", new ActionMessage("errors.exists", iExternalId));
        }

        if (iName==null || iName.trim().length()==0)
            errors.add("name", new ActionMessage("errors.required", ""));
        else {
            try {
                User user = User.findByUserName(iName);
                if (user!=null && !user.getExternalUniqueId().equals(iExternalId))
                    errors.add("name", new ActionMessage("errors.exists", iName));
            } catch (Exception e) {
                errors.add("name", new ActionMessage("errors.generic", e.getMessage()));
            }
        }

        if (iPassword==null || iPassword.trim().length()==0)
            errors.add("password", new ActionMessage("errors.required", ""));
        
        return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iOp = null; iExternalId = null; iName = null; iPassword = null;
	}
	
	public String getOp() { return iOp; }
	public void setOp(String op) { iOp = op; }
    public String getExternalId() { return iExternalId; }
    public void setExternalId(String externalId) { iExternalId = externalId; }
    public String getName() { return iName; }
    public void setName(String name) { iName = name; }
    public String getPassword() { return iPassword; }
    public void setPassword(String password) { iPassword = password; }
    
    public void load(User user) {
        setOp("Update");
        setExternalId(user.getExternalUniqueId());
        setName(user.getUsername());
        setPassword(user.getPassword());
    }
    
    public void saveOrUpdate(org.hibernate.Session hibSession) throws Exception {
        if ("Update".equals(getOp())) {
            User u = User.findByExternalId(getExternalId());
            u.setUsername(getName());
            if (!getPassword().equals(u.getPassword())) {
                u.setPassword(DbAuthenticateModule.getEncodedPassword(getPassword()));
                System.out.println("Password changed to "+u.getPassword());
            }
            hibSession.update(u);
        } else {
            User u = new User();
            u.setExternalUniqueId(getExternalId());
            u.setUsername(getName());
            u.setPassword(DbAuthenticateModule.getEncodedPassword(getPassword()));
            hibSession.save(u);
        }
    }
    
    public void delete(org.hibernate.Session hibSession) {
        User u = User.findByExternalId(getExternalId());
        if (u!=null) hibSession.delete(u);
    }
}

