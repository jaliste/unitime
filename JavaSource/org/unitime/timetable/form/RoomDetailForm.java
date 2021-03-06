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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/** 
 * MyEclipse Struts
 * Creation date: 05-12-2006
 * 
 * XDoclet definition:
 * @struts.form name="roomDetailForm"
 */
public class RoomDetailForm extends ActionForm {

	// --------------------------------------------------------- Instance Variables
	private String id;
	private String doit;
	private String sharingTable;
	private String name;
    private String externalId;
	private Integer capacity;
	private Double coordinateX;
	private Double coordinateY;
	private Long type;
    private String typeName;
	private String patterns;
	private Collection groups;
	private Collection globalFeatures;	
	private Collection departmentFeatures;	
	private List roomPrefs;	
	private List depts;
	private boolean deleteFlag;
	private boolean owner;
	private boolean ignoreTooFar = false;
	private boolean ignoreRoomCheck = false;
	private String control = null;
	private boolean nonUniv;
	private boolean editable = false;
	private boolean examEnabled = false;
	private boolean examEEnabled = false;
	private boolean used = false;
	private Integer examCapacity;
	private String examPref;
	private String examEPref;
	
	private Long previos, next;

	// --------------------------------------------------------- Methods

	/**
	 * 
	 */
	private static final long serialVersionUID = -542603705961314236L;

	/** 
	 * Method validate
	 * @param mapping
	 * @param request
	 * @return ActionErrors
	 */
	public ActionErrors validate(
		ActionMapping mapping,
		HttpServletRequest request) {

		return null;
	}

	/** 
	 * Method reset
	 * @param mapping
	 * @param request
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		ignoreTooFar = false;
		ignoreRoomCheck = false;
		editable = false; used = false;
		control = null;
		examEnabled = false; examEEnabled = false;
		examPref = null; examEPref = null;
		previos = null; next = null;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	public Double getCoordinateX() {
		return coordinateX;
	}

	public void setCoordinateX(Double coordinateX) {
		this.coordinateX = coordinateX;
	}

	public Double getCoordinateY() {
		return coordinateY;
	}

	public void setCoordinateY(Double coordinateY) {
		this.coordinateY = coordinateY;
	}

	public Collection getGlobalFeatures() {
		return globalFeatures;
	}

	public void setGlobalFeatures(Collection globalFeatures) {
		this.globalFeatures = globalFeatures;
	}

	public Collection getDepartmentFeatures() {
		return departmentFeatures;
	}

	public void setDepartmentFeatures(Collection departmentFeatures) {
		this.departmentFeatures = departmentFeatures;
	}

	public Collection getGroups() {
		return groups;
	}

	public void setGroups(Collection groups) {
		this.groups = groups;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPatterns() {
		return patterns;
	}

	public void setPatterns(String patterns) {
		this.patterns = patterns;
	}

	public Long getType() {
		return type;
	}

	public void setType(Long type) {
		this.type = type;
	}

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public boolean isDeleteFlag() {
		return deleteFlag;
	}

	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSharingTable() {
		return sharingTable;
	}

	public void setSharingTable(String sharingTable) {
		this.sharingTable = sharingTable;
	}

	public String getDoit() {
		return doit;
	}

	public void setDoit(String doit) {
		this.doit = doit;
	}

	public boolean isOwner() {
		return owner;
	}

	public void setOwner(boolean owner) {
		this.owner = owner;
	}

	public void setIgnoreTooFar(boolean ignoreTooFar) {
		this.ignoreTooFar = ignoreTooFar;
	}
	
	public boolean getIgnoreTooFar() { return ignoreTooFar; }

	public boolean isIgnoreRoomCheck() {
		return ignoreRoomCheck;
	}

	public void setIgnoreRoomCheck(boolean ignoreRoomCheck) {
		this.ignoreRoomCheck = ignoreRoomCheck;
	}

	public String getControl() {
		return control;
	}

	public void setControl(String control) {
		this.control = control;
	}

	public List getRoomPrefs() {
		return roomPrefs;
	}

	public void setRoomPrefs(List roomPrefs) {
		this.roomPrefs = roomPrefs;
	}

	public List getDepts() {
		return depts;
	}

	public void setDepts(List depts) {
		this.depts = depts;
	}

	public boolean isNonUniv() {
		return nonUniv;
	}

	public void setNonUniv(boolean nonUniv) {
		this.nonUniv = nonUniv;
	}
	
	public boolean isEditable() { return editable; }
	public void setEditable(boolean editable) { this.editable = editable; }
	
	public Integer getExamCapacity() {
	    return examCapacity;
	}
	
	public void setExamCapacity(Integer examCapacity) {
	    this.examCapacity = examCapacity;
	}
	
	public boolean isExamEnabled() {
	    return examEnabled;
	}
	
	public void setExamEnabled(boolean examEnabled) {
	    this.examEnabled = examEnabled;
	}
	
    public boolean isExamEEnabled() {
        return examEEnabled;
    }
    
    public void setExamEEnabled(boolean examEEnabled) {
        this.examEEnabled = examEEnabled;
    }

    public void setExamPref(String examPref) {
	    this.examPref = examPref;
	}
	
	public String getExamPref() {
	    return examPref;
	}

    public void setExamEPref(String examEPref) {
        this.examEPref = examEPref;
    }
    
    public String getExamEPref() {
        return examEPref;
    }
    
    public Long getNext() {
    	return next;
    }
    
    public void setNext(Long next) {
    	this.next = next;
    }
    
    public Long getPrevious() {
    	return previos;
    }
    
    public void setPrevious(Long previous) {
    	this.previos = previous;
    }
    
	public boolean isUsed() {
	    return used;
	}
	
	public void setUsed(boolean used) {
	    this.used = used;
	}

}

