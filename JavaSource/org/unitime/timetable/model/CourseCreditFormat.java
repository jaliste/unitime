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
import org.unitime.timetable.model.base.BaseCourseCreditFormat;
import org.unitime.timetable.model.dao.CourseCreditFormatDAO;




public class CourseCreditFormat extends BaseCourseCreditFormat {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public CourseCreditFormat () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public CourseCreditFormat (Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/

	public static String COURSE_CREDIT_FORMAT_ATTR_NAME = "courseCreditFormatList";
	
	public static Vector courseCreditFormatList = null;
	
	public synchronized static Vector getCourseCreditFormatList(boolean refresh) {
		if (courseCreditFormatList != null && !refresh){
			return(courseCreditFormatList);
		}
		
		CourseCreditFormatDAO ccfDao = new CourseCreditFormatDAO();
		
        List l = ccfDao.findAll(Order.asc("label"));
		courseCreditFormatList = new Vector(l);
        return(courseCreditFormatList);
	}
	
	public static CourseCreditFormat getCourseCreditForReference(String referenceString){
		if (referenceString == null || referenceString.length() == 0){
			return(null);
		}
		CourseCreditFormat ccf = null;
		for(Iterator it = getCourseCreditFormatList(false).iterator(); it.hasNext(); ){
			ccf = (CourseCreditFormat) it.next();
			if (referenceString.equals(ccf.getReference())){
				return(ccf);
			}
		}
		return(null);
	}
	
	public static CourseCreditFormat getCourseCreditForUniqueId(Integer uniqueId){
		if (uniqueId == null){
			return(null);
		}
		CourseCreditFormat ccf = null;
		for(Iterator it = getCourseCreditFormatList(false).iterator(); it.hasNext(); ){
			ccf = (CourseCreditFormat) it.next();
			if (uniqueId.equals(ccf.getUniqueId())){
				return(ccf);
			}
		}
		return(null);
	}
	
	public String getAbbv() {
		if (getAbbreviation()==null) return "";
		return getAbbreviation();
	}
}
