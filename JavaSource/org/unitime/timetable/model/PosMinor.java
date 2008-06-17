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

import org.unitime.timetable.model.base.BasePosMinor;
import org.unitime.timetable.model.dao.PosMinorDAO;



public class PosMinor extends BasePosMinor {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public PosMinor () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public PosMinor (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public PosMinor (
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

    public static PosMinor findByCode(Long sessionId, String code) {
        return (PosMinor)new PosMinorDAO().
        getSession().
        createQuery(
                "select a from PosMinor a where "+
                "a.session.uniqueId=:sessionId and "+
                "a.code=:code").
         setLong("sessionId", sessionId.longValue()).
         setString("code", code).
         setCacheable(true).
         uniqueResult(); 
    }
    
    public static PosMinor findByCodeAcadAreaId(Long sessionId, String code, Long areaId) {
        if (areaId==null) return findByCode(sessionId, code);
        return (PosMinor)new PosMinorDAO().
        getSession().
        createQuery(
                "select p from PosMinor p inner join p.academicAreas a where "+
                "p.session.uniqueId=:sessionId and "+
                "a.uniqueId=:areaId and p.code=:code").
         setLong("sessionId", sessionId.longValue()).
         setLong("areaId", areaId.longValue()).
         setString("code", code).
         setCacheable(true).
         uniqueResult(); 
    }

    public static PosMinor findByCodeAcadAreaAbbv(Long sessionId, String code, String areaAbbv) {
        if (areaAbbv==null || areaAbbv.trim().length()==0) return findByCode(sessionId, code);
        return (PosMinor)new PosMinorDAO().
        getSession().
        createQuery(
                "select p from PosMinor p inner join p.academicAreas a where "+
                "p.session.uniqueId=:sessionId and "+
                "a.academicAreaAbbreviation=:areaAbbv and p.code=:code").
         setLong("sessionId", sessionId.longValue()).
         setString("areaAbbv", areaAbbv).
         setString("code", code).
         setCacheable(true).
         uniqueResult(); 
    }

}
