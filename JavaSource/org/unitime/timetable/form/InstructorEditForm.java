/*
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime LLC, and individual contributors
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.MessageResources;
import org.unitime.commons.User;
import org.unitime.timetable.model.PositionType;


/** 
 * MyEclipse Struts
 * Creation date: 10-20-2005
 * 
 * XDoclet definition:
 * @struts:form name="instructorEditForm"
 */
public class InstructorEditForm extends PreferencesForm  {

	// --------------------------------------------------------- Instance Variables

	/**
	 * 
	 */
	private static final long serialVersionUID = 7234507709430023477L;

	/** deptCode prpoerty */
	private String deptCode;

	/** instructorId property */
	private String instructorId;

	/** puId property */
	private String puId;

	/** name property */
	private String name;
	
	private String careerAcct;
	private String posType;
	private String note;
	private boolean displayPrefs;
	private String lname;
	private String mname;
	private String fname;
	private String deptName;
	private String email;

	private String searchSelect;
	private User i2a2Match;
	private Collection staffMatch;
	private Boolean matchFound;
	
	private String screenName;

	private String prevId;
	private String nextId;
    
    private boolean ignoreDist;
	private Boolean lookupEnabled;
    protected boolean limitedEditable;
	
	// --------------------------------------------------------- Methods
    
    public boolean getIgnoreDist() {
        return ignoreDist;
    }
    
    public void setIgnoreDist(boolean ignoreDist) {
        this.ignoreDist = ignoreDist; 
    }

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
	
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getFname() {
		return fname;
	}

	public void setFname(String fname) {
		this.fname = fname;
	}

	public String getLname() {
		return lname;
	}

	public void setLname(String lname) {
		this.lname = lname;
	}

	public String getMname() {
		return mname;
	}

	public void setMname(String mname) {
		this.mname = mname;
	}

	public String getCareerAcct() {
		return careerAcct;
	}

	public void setCareerAcct(String careerAcct) {
		this.careerAcct = careerAcct;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getPosType() {
		return posType;
	}

	public void setPosType(String posCode) {
		this.posType = posCode;
	}

    public String getScreenName() {
        return screenName;
    }
    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
    
	/** 
	 * Method reset
	 * @param mapping
	 * @param request
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		instructorId = "";
		screenName = "instructor";
		super.reset(mapping, request);
		
        //Set request attributes
        setPosType(request);
        prevId = nextId = null;
        ignoreDist = false;
        limitedEditable = false;
        email = null;
	}
	
	/**
	 * 
	 * @param request
	 */
	private void setPosType(HttpServletRequest request) {
		TreeSet tset = PositionType.findAll();
		ArrayList list = new ArrayList();
		
		for (Iterator iter = tset.iterator(); iter.hasNext(); ) {
			PositionType pt = (PositionType) iter.next();
			list.add(new LabelValueBean(pt.getLabel().trim(), pt.getUniqueId().toString()));
		}
		
		request.setAttribute(PositionType.POSTYPE_ATTR_NAME, list);
		
	}

	/** 
	 * Method validate
	 * @param mapping
	 * @param request
	 * @return ActionErrors
	 */
	public ActionErrors validate(
		ActionMapping mapping,
		HttpServletRequest request) {
		
        ActionErrors errors = new ActionErrors();

        // Get Message Resources
        MessageResources rsc = 
            (MessageResources) super.getServlet()
            	.getServletContext().getAttribute(Globals.MESSAGES_KEY);
        

        if (op.equals(rsc.getMessage("button.checkPuId"))) {
    		if ( (fname==null || fname.trim().length()==0) 
    		        && (lname==null || lname.trim().length()==0) 
    		        && (careerAcct==null || careerAcct.trim().length()==0) ) {
				errors.add("fname", 
	                    new ActionMessage("errors.generic", "Supply one or more of the following information: Career Account / First Name / Last Name") );
    		}
    		
    		return errors;
        }
        
        if (!screenName.equalsIgnoreCase("instructorPref") ) {
					
			if (lname == null || lname.trim().equals("")) {
				errors.add("Last Name", 
	                    new ActionMessage("errors.required", "Last Name") );
			}
        }
        
		if (errors.size() == 0) {
			return super.validate(mapping, request); 
		} else {
			return errors;
		}
	}

	/** 
	 * Returns the instructorId.
	 * @return String
	 */
	public String getInstructorId() {
		return instructorId;
	}

	/** 
	 * Set the instructorId.
	 * @param instructorId The instructorId to set
	 */
	public void setInstructorId(String instructorId) {
		this.instructorId = instructorId;
	}

	/** 
	 * Returns the puId.
	 * @return String
	 */
	public String getPuId() {
		return puId;
	}

	/** 
	 * Set the puId.
	 * @param puId The puId to set
	 */
	public void setPuId(String puId) {
		this.puId = puId;
	}

	/** 
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/** 
	 * Set the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return
	 */
	public String getDeptCode() {
		return deptCode;
	}

	/**
	 * 
	 * @param deptCode
	 */
	public void setDeptCode(String deptCode) {
		this.deptCode = deptCode;
	}

	public boolean isDisplayPrefs() {
		return displayPrefs;
	}

	public void setDisplayPrefs(boolean displayPrefs) {
		this.displayPrefs = displayPrefs;
	}	
	
    public User getI2a2Match() {
        return i2a2Match;
    }
    public void setI2a2Match(User match) {
        i2a2Match = match;
    }
    
    public Collection getStaffMatch() {
        return staffMatch;
    }
    public void setStaffMatch(Collection staffMatch) {
        this.staffMatch = staffMatch;
    }
    
    public Boolean getMatchFound() {
        return matchFound;
    }
    public void setMatchFound(Boolean matchFound) {
        this.matchFound = matchFound;
    }
        
    public String getSearchSelect() {
        return searchSelect;
    }
    public void setSearchSelect(String searchSelect) {
        this.searchSelect = searchSelect;
    }
        
    public void setPreviousId(String prevId) {
    	this.prevId = prevId;
    }
    public String getPreviousId() {
    	return prevId;
    }
    
    public void setNextId(String nextId) {
    	this.nextId = nextId;
    }
    public String getNextId() {
    	return nextId;
    }

	public Boolean getLookupEnabled() {
		return lookupEnabled;
	}

	public void setLookupEnabled(Boolean lookupEnabled) {
		this.lookupEnabled = lookupEnabled;
	}
    
    public boolean isLimitedEditable() { return limitedEditable; }
    public void setLimitedEditable(boolean limitedEditable) { this.limitedEditable = limitedEditable; }
    
}

