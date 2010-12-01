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

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.unitime.timetable.model.base.BasePosMajor;
import org.unitime.timetable.model.dao.PosMajorDAO;




public class PosMajor extends BasePosMajor {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public PosMajor () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public PosMajor (java.lang.Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/

    /** Request attribute name for available pos majors **/
    public static String POSMAJOR_ATTR_NAME = "posMajorList";  
    
	/**
	 * Retrieves all pos majors in the database for the academic session
	 * ordered by column name
	 * @param sessionId academic session
	 * @return Vector of PosMajor objects
	 */
    public static List getPosMajorList(Long sessionId) {
	    Session hibSession = new PosMajorDAO().getSession();
	    String query = "from PosMajor where academicArea.sessionId=:acadSessionId order by name";
	    Query q = hibSession.createQuery(query);
	    q.setLong("acadSessionId", sessionId.longValue());
		return q.list();
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

    public static PosMajor findByCode(Long sessionId, String code) {
        return (PosMajor)new PosMajorDAO().
        getSession().
        createQuery(
                "select a from PosMajor a where "+
                "a.session.uniqueId=:sessionId and "+
                "a.code=:code").
         setLong("sessionId", sessionId.longValue()).
         setString("code", code).
         setCacheable(true).
         uniqueResult(); 
    }

    public static PosMajor findByExternalId(Long sessionId, String externalId) {
        return (PosMajor)new PosMajorDAO().
        getSession().
        createQuery(
                "select a from PosMajor a where "+
                "a.session.uniqueId=:sessionId and "+
                "a.externalUniqueId=:externalUniqueId").
         setLong("sessionId", sessionId.longValue()).
         setString("externalUniqueId", externalId).
         setCacheable(true).
         uniqueResult(); 
    }

    public static PosMajor findByCodeAcadAreaId(Long sessionId, String code, Long areaId) {
        if (areaId==null) return findByCode(sessionId, code);
        return (PosMajor)new PosMajorDAO().
        getSession().
        createQuery(
                "select p from PosMajor p inner join p.academicAreas a where "+
                "p.session.uniqueId=:sessionId and "+
                "a.uniqueId=:areaId and p.code=:code").
         setLong("sessionId", sessionId.longValue()).
         setLong("areaId", areaId.longValue()).
         setString("code", code).
         setCacheable(true).
         uniqueResult(); 
    }

    public static PosMajor findByCodeAcadAreaAbbv(Long sessionId, String code, String areaAbbv) {
        if (areaAbbv==null || areaAbbv.trim().length()==0) return findByCode(sessionId, code);
        return (PosMajor)new PosMajorDAO().
        getSession().
        createQuery(
                "select p from PosMajor p inner join p.academicAreas a where "+
                "p.session.uniqueId=:sessionId and "+
                "a.academicAreaAbbreviation=:areaAbbv and p.code=:code").
         setLong("sessionId", sessionId.longValue()).
         setString("areaAbbv", areaAbbv).
         setString("code", code).
         setCacheable(true).
         uniqueResult(); 
    }
    
    public Object clone() {
    	PosMajor m = new PosMajor();
    	m.setExternalUniqueId(getExternalUniqueId());
    	m.setCode(getCode());
    	m.setName(getName());
    	return m;
    }
}
