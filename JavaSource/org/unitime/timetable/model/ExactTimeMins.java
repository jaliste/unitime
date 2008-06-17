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

import org.unitime.timetable.model.base.BaseExactTimeMins;
import org.unitime.timetable.model.dao.ExactTimeMinsDAO;
import org.unitime.timetable.util.Constants;



public class ExactTimeMins extends BaseExactTimeMins implements Comparable {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public ExactTimeMins () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public ExactTimeMins (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public ExactTimeMins (
		java.lang.Long uniqueId,
		java.lang.Integer minsPerMtgMin,
		java.lang.Integer minsPerMtgMax,
		java.lang.Integer nrSlots,
		java.lang.Integer breakTime) {

		super (
			uniqueId,
			minsPerMtgMin,
			minsPerMtgMax,
			nrSlots,
			breakTime);
	}

/*[CONSTRUCTOR MARKER END]*/

	public static ExactTimeMins findByMinPerMtg(int minPerMtg) {
		return (ExactTimeMins)
			(new ExactTimeMinsDAO()).
			getSession().
			createQuery("select m from ExactTimeMins m where m.minsPerMtgMin<=:minPerMtg and :minPerMtg<=m.minsPerMtgMax").
			setInteger("minPerMtg", minPerMtg).
			setCacheable(true).
			uniqueResult();
	}
	
	public static int getNrSlotsPerMtg(int minPerMtg) {
		ExactTimeMins ex = findByMinPerMtg(minPerMtg);
		if (ex==null) {
			int slotsPerMtg = (int)Math.round((6.0/5.0) * minPerMtg / Constants.SLOT_LENGTH_MIN);
			if (minPerMtg<30.0) slotsPerMtg = Math.min(6,slotsPerMtg);
			return slotsPerMtg;
		} else {
			return ex.getNrSlots().intValue();
		}
	}

	public static int getBreakTime(int minPerMtg) {
		ExactTimeMins ex = findByMinPerMtg(minPerMtg);
		if (ex==null) {
			int slotsPerMtg = (int)Math.round((6.0/5.0) * minPerMtg / Constants.SLOT_LENGTH_MIN);
			if (minPerMtg<30.0) slotsPerMtg = Math.min(6,slotsPerMtg);
			int breakTime = 0;
			if (slotsPerMtg%12==0) breakTime = 10;
			else if (slotsPerMtg>6) breakTime = 15;
			return breakTime;
		} else {
			return ex.getBreakTime().intValue();
		}
	}
	
    public static int getNrSlotsPerMtg(int dayCode, int minPerWeek) {
		int nrDays = 0;
		for (int i=0;i<Constants.NR_DAYS;i++)
			if ((dayCode & Constants.DAY_CODES[i])!=0) nrDays++;
		if (nrDays==0) nrDays=1;
		int minPerMtg = (int)Math.round(((double)minPerWeek) / nrDays);
		return getNrSlotsPerMtg(minPerMtg);
    }
	
    public static int getBreakTime(int dayCode, int minPerWeek) {
		int nrDays = 0;
		for (int i=0;i<Constants.NR_DAYS;i++)
			if ((dayCode & Constants.DAY_CODES[i])!=0) nrDays++;
		if (nrDays==0) nrDays=1;
		int minPerMtg = (int)Math.round(((double)minPerWeek) / nrDays);
		return getBreakTime(minPerMtg);
    }
    
    public int compareTo(Object o) {
    	if (o==null || !(o instanceof ExactTimeMins)) return -1;
    	ExactTimeMins ex = (ExactTimeMins)o;
    	int cmp = getMinsPerMtgMin().compareTo(ex.getMinsPerMtgMin());
    	if (cmp!=0) return cmp;
    	cmp = getMinsPerMtgMax().compareTo(ex.getMinsPerMtgMax());
    	if (cmp!=0) return cmp;
    	return getUniqueId().compareTo(ex.getUniqueId());
    }
}
