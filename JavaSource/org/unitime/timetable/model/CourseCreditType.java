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
import org.unitime.timetable.model.base.BaseCourseCreditType;
import org.unitime.timetable.model.dao.CourseCreditTypeDAO;




public class CourseCreditType extends BaseCourseCreditType {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public CourseCreditType () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public CourseCreditType (Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/

	public static String COURSE_CREDIT_TYPE_ATTR_NAME = "courseCreditTypeList";
	
	public static Vector courseCreditTypeList = null;
	
	public static synchronized Vector getCourseCreditTypeList(boolean refresh) {
		if (courseCreditTypeList != null && !refresh){
			return(courseCreditTypeList);
		}
		
		CourseCreditTypeDAO cctDao = new CourseCreditTypeDAO();
        
        List l = cctDao.findAll(Order.asc("label"));
		courseCreditTypeList = new Vector(l);
        return(courseCreditTypeList);
	}
	
	public static CourseCreditType getCourseCreditTypeForReference(String referenceString){
		if (referenceString == null || referenceString.length() == 0){
			return(null);
		}
		CourseCreditType cct = null;
		for(Iterator it = getCourseCreditTypeList(false).iterator(); it.hasNext(); ){
			cct = (CourseCreditType) it.next();
			if (referenceString.equals(cct.getReference())){
				return(cct);
			}
		}
		return(null);
	}

	public static CourseCreditType getCourseCreditTypeForUniqueId(Long uniqueId){
		if (uniqueId == null){
			return(null);
		}
		CourseCreditType cct = null;
		for(Iterator it = getCourseCreditTypeList(false).iterator(); it.hasNext(); ){
			cct = (CourseCreditType) it.next();
			if (uniqueId.equals(cct.getUniqueId())){
				return(cct);
			}
		}
		return(null);
	}
	
	public String getAbbv() {
		if (getAbbreviation()==null) return "";
		return getAbbreviation();
	}	
}
