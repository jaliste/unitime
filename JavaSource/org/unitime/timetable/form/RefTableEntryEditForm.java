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

import org.apache.struts.action.ActionForm;
import org.unitime.timetable.model.RefTableEntry;


/** 
* MyEclipse Struts
* Creation date: 02-18-2005
* 
* XDoclet definition:
* @struts:form name="refTableEntryEditForm"
*/
public abstract class RefTableEntryEditForm extends ActionForm {

	/**
	 * 
	 */
	// --------------------------------------------------------- Instance Variables
	RefTableEntry refTableEntry;
	// --------------------------------------------------------- Methods

	/**
	 * @return Returns the refTableEntry.
	 */
	public RefTableEntry getRefTableEntry() {
		return refTableEntry;
	}
	/**
	 * @param refTableEntry The refTableEntry to set.
	 */
	public void setRefTableEntry(RefTableEntry refTableEntry) {
		this.refTableEntry = refTableEntry;
	}
	
	public boolean equals(Object arg0) {
		return refTableEntry.equals(arg0);
	}
	
	public String getReference() {
		return refTableEntry.getReference();
	}
	
	public String getLabel() {
		return refTableEntry.getLabel();
	}
	
	public int hashCode() {
		return refTableEntry.hashCode();
	}
	
	public void setReference(String reference) {
		refTableEntry.setReference(reference);
	}
	public void setLabel(String label) {
		refTableEntry.setLabel(label);
	}
	public String toString() {
		return refTableEntry.toString();
	}
	
	
	/**
	 * @return
	 */
	public Long getUniqueId() {
		return refTableEntry.getUniqueId();
	}
	/**
	 * @param refTableEntryId
	 */
	public void setUniqueId(Long uniqueId) {
		refTableEntry.setUniqueId(uniqueId);
	}
}
