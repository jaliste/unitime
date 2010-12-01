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
package org.unitime.timetable.model;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.hibernate.criterion.Order;
import org.unitime.timetable.model.base.BaseCourseCreditUnitType;
import org.unitime.timetable.model.dao.CourseCreditUnitTypeDAO;




public class CourseCreditUnitType extends BaseCourseCreditUnitType {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public CourseCreditUnitType () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public CourseCreditUnitType (Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/

	public static String COURSE_CREDIT_UNIT_TYPE_ATTR_NAME = "courseCreditUnitTypeList";
	
	public static Vector courseCreditUnitTypeList = null;
	
	public static synchronized Vector getCourseCreditUnitTypeList(boolean refresh) {
		if (courseCreditUnitTypeList != null && !refresh){
			return(courseCreditUnitTypeList);
		}
		
		CourseCreditUnitTypeDAO cctDao = new CourseCreditUnitTypeDAO();
        
        List l = cctDao.findAll(Order.asc("label"));
        courseCreditUnitTypeList = new Vector(l);
        return(courseCreditUnitTypeList);
	}
	
	public static CourseCreditUnitType getCourseCreditUnitTypeForReference(String referenceString){
		if (referenceString == null || referenceString.length() == 0){
			return(null);
		}
		CourseCreditUnitType ccut = null;
		for(Iterator it = getCourseCreditUnitTypeList(false).iterator(); it.hasNext(); ){
			ccut = (CourseCreditUnitType) it.next();
			if (referenceString.equals(ccut.getReference())){
				return(ccut);
			}
		}
		return(null);
	}

	public static CourseCreditUnitType getCourseCreditUnitTypeForUniqueId(Long uniqueId){
		if (uniqueId == null){
			return(null);
		}
		CourseCreditUnitType ccut = null;
		for(Iterator it = getCourseCreditUnitTypeList(false).iterator(); it.hasNext(); ){
			ccut = (CourseCreditUnitType) it.next();
			if (uniqueId.equals(ccut.getUniqueId())){
				return(ccut);
			}
		}
		return(null);
	}
	
	public String getAbbv() {
		if (getAbbreviation()==null) return "";
		return getAbbreviation();
	}

}
