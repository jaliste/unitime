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
import org.apache.struts.action.ActionMapping;
import org.unitime.timetable.ApplicationProperties;


/** 
 * MyEclipse Struts
 * Creation date: 07-26-2005
 * 
 * XDoclet definition:
 * @struts:form name="schedulingSubpartEditForm"
 */
public class SchedulingSubpartEditForm extends PreferencesForm {

    // --------------------------------------------------------- Instance Variables
    
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3256445806692087861L;
	
	private String schedulingSubpartId;
	private String subjectAreaId;
    private String subjectArea;
    private String courseNbr;
    private String courseTitle;
    private String parentSubpart;
    private String instructionalType;
    private String instructionalTypeLabel;
    private Long datePattern;
    private String instrOfferingId;
    private String parentSubpartId;
    private String parentSubpartLabel;
    private String managingDeptName;
    private String creditFormat;
    private Long creditType;
    private Long creditUnitType;
    private Float units;
    private Float maxUnits;
    private Boolean fractionalIncrementsAllowed;
    private String creditText;
    private Boolean sameItypeAsParent;
    private Boolean unlimitedEnroll;
    private Boolean autoSpreadInTime;
    private Boolean subpartCreditEditAllowed;
    private boolean itypeBasic;
    private Boolean studentAllowOverlap;
    
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
        return super.validate(mapping, request);
    }

    /** 
     * Method reset
     * @param mapping
     * @param request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {        
        schedulingSubpartId = "";
        datePattern = null;
        unlimitedEnroll = null;
        parentSubpartId = null; parentSubpartLabel = null; managingDeptName = null; sameItypeAsParent = null;
        creditFormat = null; creditType = null; creditUnitType = null; units = null; maxUnits = null; fractionalIncrementsAllowed = new Boolean(false); creditText = "";
        autoSpreadInTime = Boolean.FALSE;
        studentAllowOverlap = Boolean.FALSE;
        subpartCreditEditAllowed =new Boolean(ApplicationProperties.getProperty("tmtbl.subpart.credit.editable", "false").equals("true"));
        itypeBasic = false;
        instructionalType = null; instructionalTypeLabel = null;
        super.reset(mapping, request);
    }

    /**
     * @return Returns the schedulingSubpartId.
     */
    public String getSchedulingSubpartId() {
        return schedulingSubpartId;
    }
    /**
     * @param schedulingSubpartId The schedulingSubpartId to set.
     */
    public void setSchedulingSubpartId(String schedulingSubpartId) {
        this.schedulingSubpartId = schedulingSubpartId;
    }    
    
    /**
     * @return Returns the courseNbr.
     */
    public String getCourseNbr() {
        return courseNbr;
    }
    /**
     * @param courseNbr The courseNbr to set.
     */
    public void setCourseNbr(String courseNbr) {
        this.courseNbr = courseNbr;
    }
    
    public String getCourseTitle() {
    	return this.courseTitle;
    }
    
    public void setCourseTitle(String courseTitle) {
    	this.courseTitle = courseTitle;
    }
    
    /**
     * @return Returns the instructionalType.
     */
    public String getInstructionalType() {
        return instructionalType;
    }
    /**
     * @param instructionalType The instructionalType to set.
     */
    public void setInstructionalType(String instructionalType) {
        this.instructionalType = instructionalType;
    }
    
    /**
     * @return Returns the parentSubpart.
     */
    public String getParentSubpart() {
        return parentSubpart;
    }
    /**
     * @param parentSubpart The parentSubpart to set.
     */
    public void setParentSubpart(String parentSubpart) {
        this.parentSubpart = parentSubpart;
    }
    public String getParentSubpartId() {
        return parentSubpartId;
    }
    public void setParentSubpartId(String parentSubpartId) {
        this.parentSubpartId = parentSubpartId;
    }
    public String getParentSubpartLabel() {
        return parentSubpartLabel;
    }
    public void setParentSubpartLabel(String parentSubpartLabel) {
        this.parentSubpartLabel = parentSubpartLabel;
    }
    
    /**
     * @return Returns the subjectArea.
     */
    public String getSubjectArea() {
        return subjectArea;
    }
    /**
     * @param subjectArea The subjectArea to set.
     */
    public void setSubjectArea(String subjectArea) {
        this.subjectArea = subjectArea;
    }    
    
    /**
     * @return Returns the instructionalTypeLabel.
     */
    public String getInstructionalTypeLabel() {
        return instructionalTypeLabel;
    }
    /**
     * @param instructionalTypeLabel The instructionalTypeLabel to set.
     */
    public void setInstructionalTypeLabel(String instructionalTypeLabel) {
        this.instructionalTypeLabel = instructionalTypeLabel;
    }  
    
	public Long getDatePattern() {
        return datePattern;
    }
    public void setDatePattern(Long datePattern) {
        this.datePattern = datePattern;
    }
        
    public String getSubjectAreaId() {
        return subjectAreaId;
    }
    public void setSubjectAreaId(String subjectAreaId) {
        this.subjectAreaId = subjectAreaId;
    }
    
    public String getInstrOfferingId() {
        return instrOfferingId;
    }
    public void setInstrOfferingId(String instrOfferingId) {
        this.instrOfferingId = instrOfferingId;
    }
    public String getManagingDeptName() { return managingDeptName; }
    public void setManagingDeptName(String managingDeptName) { this.managingDeptName = managingDeptName; }

	public String getCreditFormat() {
		return creditFormat;
	}

	public void setCreditFormat(String creditFormat) {
		this.creditFormat = creditFormat;
	}

	public Long getCreditType() {
		return creditType;
	}

	public void setCreditType(Long creditType) {
		this.creditType = creditType;
	}

	public Long getCreditUnitType() {
		return creditUnitType;
	}

	public void setCreditUnitType(Long creditUnitType) {
		this.creditUnitType = creditUnitType;
	}

	public Boolean getFractionalIncrementsAllowed() {
		return fractionalIncrementsAllowed;
	}

	public void setFractionalIncrementsAllowed(Boolean fractionalIncrementsAllowed) {
		this.fractionalIncrementsAllowed = fractionalIncrementsAllowed;
	}

	public Float getMaxUnits() {
		return maxUnits;
	}

	public void setMaxUnits(Float maxUnits) {
		this.maxUnits = maxUnits;
	}

	public Float getUnits() {
		return units;
	}

	public void setUnits(Float units) {
		this.units = units;
	}

	public String getCreditText() {
		return creditText;
	}

	public void setCreditText(String creditText) {
		this.creditText = creditText;
	}

	public Boolean getSameItypeAsParent() {
		return sameItypeAsParent;
	}

	public void setSameItypeAsParent(Boolean sameItypeAsParent) {
		this.sameItypeAsParent = sameItypeAsParent;
	}

    public Boolean getUnlimitedEnroll() {
        return unlimitedEnroll;
    }
    public void setUnlimitedEnroll(Boolean unlimitedEnroll) {
        this.unlimitedEnroll = unlimitedEnroll;
    }

	public Boolean getAutoSpreadInTime() {
		return autoSpreadInTime;
	}
	
	public void setAutoSpreadInTime(Boolean autoSpreadInTime) {
		this.autoSpreadInTime = autoSpreadInTime;
	}

	public Boolean getSubpartCreditEditAllowed() {
		return subpartCreditEditAllowed;
	}
	
	public void setSubpartCreditEditAllowed(Boolean subpartCreditEditAllowed) {
		this.subpartCreditEditAllowed = subpartCreditEditAllowed;
	}
	
	public boolean getItypeBasic() { return itypeBasic; }
	public void setItypeBasic(boolean itypeBasic) { this.itypeBasic = itypeBasic; }
	
	public boolean getStudentAllowOverlap() { return studentAllowOverlap; }
	public void setStudentAllowOverlap(boolean studentAllowOverlap) { this.studentAllowOverlap = studentAllowOverlap; }
}
