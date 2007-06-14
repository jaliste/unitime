/*
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org, and individual contributors
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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.criterion.Restrictions;
import org.unitime.timetable.model.base.BaseBuilding;
import org.unitime.timetable.model.dao.BuildingDAO;
import org.unitime.timetable.model.dao.RoomDAO;




public class Building extends BaseBuilding implements Comparable {

/**
	 * 
	 */
	private static final long serialVersionUID = 3256440313428981557L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public Building () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Building (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public Building (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.Session session,
		java.lang.String externalUniqueId,
		java.lang.String abbreviation,
		java.lang.String name,
		java.lang.Integer coordinateX,
		java.lang.Integer coordinateY) {

		super (
			uniqueId,
			session,
			externalUniqueId,
			abbreviation,
			name,
			coordinateX,
			coordinateY);
	}

/*[CONSTRUCTOR MARKER END]*/

	/** Request attribute name for available buildings **/
    public static String BLDG_LIST_ATTR_NAME = "bldgsList";

    /**
     * @return Building Identifier of the form {Abbr} - {Name}
     */
    public String getAbbrName() {
        return this.getAbbreviation() + " - " + this.getName();
    }

    /**
     * Dummy setter - does nothing (Do not use)
     */
    public void setAbbrName(String abbrName) {
        
    }
    
    /**
     * @return Building Identifier of the form {Abbr} - {Name}
     */
    public String toString() {
        return getAbbrName();        
    }
    
    public int compareTo(Object o) {
    	if (o==null || !(o instanceof Building)) return -1;
    	Building b = (Building)o;
    	int cmp = getAbbreviation().compareTo(b.getAbbreviation());
    	if (cmp!=0) return cmp;
    	return getUniqueId().compareTo(b.getUniqueId());
    }
    
    /**
     * 
     * @param bldgAbbv
     * @param sessionId
     * @return
     * @throws Exception
     */
	public static Building findByBldgAbbv(String bldgAbbv, Long sessionId) throws Exception {
		List bldgs = (new BuildingDAO()).getQuery(
				"SELECT distinct b FROM Building b "+ 
				"WHERE b.session.uniqueId=:sessionId AND b.abbreviation=:bldgAbbv").
				setLong("sessionId", sessionId.longValue()).
				setString("bldgAbbv", bldgAbbv).
				list();
		
		if (!bldgs.isEmpty()) return (Building)bldgs.get(0);

		return null;
	}
	
	/*
	 * Update building information using External Building
	 * @param sessionId
	 */
    public static void updateBuildings(Long sessionId) {
        
        Session currentSession = Session.getSessionById(sessionId);
        TreeSet currentBuildings = new TreeSet(currentSession.getBuildings());
        Hashtable updateBuildings = ExternalBuilding.getBuildings(sessionId);
        
        Iterator b = currentBuildings.iterator();
        BuildingDAO bldgDAO = new BuildingDAO();
        
        while(b.hasNext()) {
            Building bldg = (Building)b.next();
            ExternalBuilding extBldg = 
                (ExternalBuilding)updateBuildings.get(bldg.getExternalUniqueId());
            if(extBldg != null) {
                if(updateBldgInfo(bldg, extBldg)) {
                    bldgDAO.update(bldg);
                }
                b.remove();
                updateBuildings.remove(extBldg.getExternalUniqueId());
            }
        }
        
        b = currentBuildings.iterator();
        while(b.hasNext()) {
            Building bldg = (Building)b.next();
            if(checkBuildingDelete(bldg)) {
                currentSession.getBuildings().remove(bldg);
                bldgDAO.delete(bldg);
            }
        }
        
        Iterator eb = (updateBuildings.values()).iterator();
        while(eb.hasNext()) {
            ExternalBuilding extBldg = (ExternalBuilding)eb.next();
            Building newBldg = new Building();
            newBldg.setAbbreviation(extBldg.getAbbreviation());
            newBldg.setCoordinateX(extBldg.getCoordinateX());
            newBldg.setCoordinateY(extBldg.getCoordinateY());
            newBldg.setName(extBldg.getDisplayName());
            newBldg.setSession(currentSession);
            newBldg.setExternalUniqueId(extBldg.getExternalUniqueId());
            bldgDAO.save(newBldg);
        }
        
        return;
    }
    
	/*
	 * Update building information
	 * @param bldg (Building)
	 * @param extBldg (ExternalBuilding)
	 * @return update  (True if updates are made)
	 */
	private static boolean updateBldgInfo(Building bldg, ExternalBuilding extBldg) {
		
		boolean updated = false;
		
		if(!bldg.getAbbreviation().equals(extBldg.getAbbreviation())) {
			bldg.setAbbreviation(extBldg.getAbbreviation());
			updated = true;
		}
		if(!bldg.getName().equals(extBldg.getDisplayName())) {
			bldg.setName(extBldg.getDisplayName());
			updated = true;
		}
		if((bldg.getCoordinateX().compareTo(extBldg.getCoordinateX())) != 0) {
			bldg.setCoordinateX(extBldg.getCoordinateX());
			updated = true;
		}
		if((bldg.getCoordinateY().compareTo(extBldg.getCoordinateY())) != 0) {
			bldg.setCoordinateY(extBldg.getCoordinateY());
			updated = true;
		}
		
		return updated;
	}
	
	/*
	 * Check if building can be deleted
	 * @param bldg
	 * @return boolean  (True if building can be deleted)
	 */
	private static boolean checkBuildingDelete(Building bldg) {
		
		boolean result = false;;
		
		List rooms = (new RoomDAO()).getQuery(
				"from Room as rm " + 
				"where rm.building.uniqueId = " + (bldg.getUniqueId()).longValue()).
				list();
		
		if(rooms.isEmpty()) {
			result = true;
		}
		return result;
	}
	
	public Object clone() {
		Building b = new Building();
		b.setAbbreviation(getAbbreviation());
		b.setCoordinateX(getCoordinateX());
		b.setCoordinateY(getCoordinateY());
		b.setExternalUniqueId(getExternalUniqueId());
		b.setName(getName());
		b.setSession(getSession());
		return b;
	}

	public Building findSameBuildingInSession(Session newSession) throws Exception{
		if (newSession == null){
			return(null);
		}

		Building newBuilding = Building.findByBldgAbbv(this.getAbbreviation(), newSession.getUniqueId());
		if (newBuilding == null && this.getExternalUniqueId() != null){
			newBuilding = Building.findByExternalIdAndSession(getExternalUniqueId(), newSession);
		}
		return(newBuilding);

	}
	
	public static Building findByExternalIdAndSession(String externalId, Session session){
		BuildingDAO bDao = new BuildingDAO();
		List bldgs = bDao.getSession().createCriteria(Building.class)
			.add(Restrictions.eq("externalUniqueId", externalId))
			.add(Restrictions.eq("session.uniqueId", session.getUniqueId()))
			.setCacheable(true).list();
			
		if (bldgs != null && bldgs.size() == 1){
			return((Building) bldgs.get(0));
		}

		return(null);

	}

}