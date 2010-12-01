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

import org.unitime.timetable.model.base.BaseExamConflict;

public class ExamConflict extends BaseExamConflict implements Comparable<ExamConflict> {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public ExamConflict () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public ExamConflict (java.lang.Long uniqueId) {
		super(uniqueId);
	}
	
/*[CONSTRUCTOR MARKER END]*/

	
	public static final int sConflictTypeDirect = 0;
    public static final int sConflictTypeMoreThanTwoADay = 1;
	public static final int sConflictTypeBackToBackDist = 2;
	public static final int sConflictTypeBackToBack = 3;
	
	public static String[] sConflictTypes = new String[] {"Distance", ">2 A Day", "Distance Back-To-Back", "Back-To-Back"};
	
    public boolean isDirectConflict() {
        return sConflictTypeDirect==getConflictType();
    }

    public boolean isMoreThanTwoADayConflict() {
        return sConflictTypeMoreThanTwoADay==getConflictType();
    }

    public boolean isBackToBackConflict() {
        return sConflictTypeBackToBack==getConflictType() || sConflictTypeBackToBackDist==getConflictType();
    }

    public boolean isDistanceBackToBackConflict() {
        return sConflictTypeBackToBackDist==getConflictType();
    }
    
    public int compareTo(ExamConflict conflict) {
        int cmp = getConflictType().compareTo(conflict.getConflictType());
        if (cmp!=0) return cmp;
        cmp = getNrStudents().compareTo(conflict.getNrStudents());
        if (cmp!=0) return cmp;
        return getUniqueId().compareTo(conflict.getUniqueId());
    }
}
