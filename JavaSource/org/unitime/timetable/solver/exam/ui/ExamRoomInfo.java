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
package org.unitime.timetable.solver.exam.ui;

import java.io.Serializable;

import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.dao.LocationDAO;

import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.ifs.util.DistanceMetric;

public class ExamRoomInfo implements Serializable, Comparable<ExamRoomInfo>{
	private static final long serialVersionUID = -5882156641099610154L;
	private Long iId = null;
    private String iName = null;
    private int iPreference = 0;
    private int iCapacity, iExamCapacity = 0;
    private Double iX = null, iY = null;
    private transient Location iLocation = null;
    private transient static DistanceMetric sDistanceMetric = null;
    
    public ExamRoomInfo(ExamRoom room, int preference) {
        iId = room.getId();
        iName = room.getName();
        iCapacity = room.getSize();
        iExamCapacity = room.getAltSize();
        iPreference = preference;
        iX = room.getCoordX(); iY = room.getCoordY();
    }
    
    public ExamRoomInfo(Location location, int preference) {
        iLocation = location;
        iId = location.getUniqueId();
        iName = location.getLabel();
        iCapacity = location.getCapacity();
        iExamCapacity = (location.getExamCapacity()==null?location.getCapacity()/2:location.getExamCapacity());
        iPreference = preference;
        iX = location.getCoordinateX();
        iY = location.getCoordinateY();
    }
    
    public Long getLocationId() { return iId; }
    public String getName() { return iName; }
    public int getPreference() { return iPreference; }
    public void setPreference(int preference) { iPreference = preference; }
    public int getCapacity() { return iCapacity; }
    public int getExamCapacity() { return iExamCapacity; }
    public int getCapacity(ExamInfo exam) { return (exam.getSeatingType()==Exam.sSeatingTypeExam?getExamCapacity():getCapacity());}
    public Location getLocation() {
        if (iLocation==null) iLocation = new LocationDAO().get(getLocationId());
        return iLocation;
    }
    public Location getLocation(org.hibernate.Session hibSession) {
        return new LocationDAO().get(getLocationId(), hibSession);
    }
    
    
    public String toString() {
    	return "<span style='color:"+PreferenceLevel.prolog2color(PreferenceLevel.int2prolog(getPreference()))+";' " +
    		"onmouseover=\"showGwtHint(this, '" + getLocation().getHtmlHint(PreferenceLevel.int2string(getPreference())) + "');\" onmouseout=\"hideGwtHint();\">" +
    		getName() + "</span>";
    }
    
    public String getNameWithHint(boolean pref) {
    	return "<span" + (pref? " style='color:"+PreferenceLevel.prolog2color(PreferenceLevel.int2prolog(getPreference()))+";'": "") +
    		" onmouseover=\"showGwtHint(this, '" + getLocation().getHtmlHint(PreferenceLevel.int2string(getPreference())) + "');\" onmouseout=\"hideGwtHint();\">" +
    		getName() + "</span>";
    }
    
    public int compareTo(ExamRoomInfo room) {
        int cmp = -Double.compare(getCapacity(), room.getCapacity());
        if (cmp!=0) return cmp;
        cmp = getName().compareTo(room.getName());
        if (cmp!=0) return cmp;
        return getLocationId().compareTo(room.getLocationId());
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamRoomInfo)) return false;
        return getLocationId().equals(((ExamRoomInfo)o).getLocationId());
    }
    
    public int hashCode() {
        return getLocationId().hashCode();
    }
    
    public Double getCoordX() { return iX; }
    public Double getCoordY() { return iY; }
    
    public double getDistance(ExamRoomInfo other) {
    	if (sDistanceMetric == null) {
    		sDistanceMetric = new DistanceMetric(
    				DistanceMetric.Ellipsoid.valueOf(ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name())));
    	}
    	return sDistanceMetric.getDistanceInMeters(getLocationId(), getCoordX(), getCoordY(), other.getLocationId(), other.getCoordX(), other.getCoordY());
    }

}