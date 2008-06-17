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
package org.unitime.timetable.model;

import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.unitime.timetable.model.base.BaseAcademicClassification;
import org.unitime.timetable.model.dao.AcademicClassificationDAO;




public class AcademicClassification extends BaseAcademicClassification {
	private static final long serialVersionUID = 1L;

	private static HashMap academicClassifications = new HashMap(40);

/*[CONSTRUCTOR MARKER BEGIN]*/
	public AcademicClassification () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public AcademicClassification (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public AcademicClassification (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.Session session,
		java.lang.String code,
		java.lang.String name) {

		super (
			uniqueId,
			session,
			code,
			name);
	}

/*[CONSTRUCTOR MARKER END]*/

	/** Request Attribute name for Academic Classification **/
    public static final String ACAD_CLASS_REQUEST_ATTR = "academicClassifications";

	/**
	 * Retrieves all academic classifications in the database for the academic session
	 * ordered by name
	 * @param sessionId academic session
	 * @return Vector of AcademicClassification objects
	 */
 	public static List getAcademicClassificationList(Long sessionId) 
 			throws HibernateException {
	    
 	    AcademicClassificationDAO adao = new AcademicClassificationDAO();
	    Session hibSession = adao.getSession();
	    List l = hibSession.createQuery(
	    		"select c from AcademicClassification as c where c.session.uniqueId=:sessionId " +
	    		"order by c.name").
	    	setLong("sessionId",sessionId.longValue()).setCacheable(true).list();
		return l;
	}

    /**
     * Creates label of the format Name - Code
     * @return
     */
    public String getLabelNameCode() {
        return this.getName() + " - " + this.getCode();
    }

    /**
     * Creates label of the format Code - Name
     * @return
     */
    public String getLabelCodeName() {
        return this.getCode() + " - " + this.getName();
    }

	/**
	 * Load Academic Classifications
	 */
	public static void loadAcademicClassifications(Long sessionId) {
		
		List acadClasses = getAcademicClassificationList(sessionId);
		
		for(int i = 0; i < acadClasses.size(); i++) {
			AcademicClassification acadClass = 
				(AcademicClassification)acadClasses.get(i);
			String code = acadClass.getCode();
			academicClassifications.put(code, acadClass);
		}
	}

	/**
	 * Get the Academic Classification
	 * @param academicClass
	 */
	public static AcademicClassification getAcademicClassification(
			String academicClass) {
		
		return (AcademicClassification)academicClassifications
					.get(academicClass);
	}
	
	public Long getSessionId(){
		if (getSession() != null){
			return(getSession().getUniqueId());
		} else {
			return(null);
		}
	}
    
    public static AcademicClassification findByCode(Long sessionId, String code) {
        return (AcademicClassification)new AcademicClassificationDAO().
        getSession().
        createQuery(
                "select a from AcademicClassification a where "+
                "a.session.uniqueId=:sessionId and "+
                "a.code=:code").
         setLong("sessionId", sessionId.longValue()).
         setString("code", code).
         setCacheable(true).
         uniqueResult(); 
    }
}
