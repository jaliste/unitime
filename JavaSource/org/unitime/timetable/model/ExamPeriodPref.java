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

import org.unitime.timetable.model.base.BaseExamPeriodPref;

import net.sf.cpsolver.ifs.util.ToolBox;



public class ExamPeriodPref extends BaseExamPeriodPref {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public ExamPeriodPref () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public ExamPeriodPref (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public ExamPeriodPref (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.PreferenceGroup owner,
		org.unitime.timetable.model.PreferenceLevel prefLevel) {

		super (
			uniqueId,
			owner,
			prefLevel);
	}

/*[CONSTRUCTOR MARKER END]*/
	
	public String preferenceText() { 
		return (this.getExamPeriod().getName());
    }

    public String preferenceAbbv() { 
        return (this.getExamPeriod().getAbbreviation());
    }

    public Object clone() {
        ExamPeriodPref pref = new ExamPeriodPref();
    	pref.setPrefLevel(getPrefLevel());
    	pref.setExamPeriod(getExamPeriod());
    	return pref;
    }
    public boolean isSame(Preference other) {
    	if (other==null || !(other instanceof ExamPeriodPref)) return false;
    	return ToolBox.equals(getExamPeriod(),((ExamPeriodPref)other).getExamPeriod());
    }

	public String preferenceTitle() {
		return getPrefLevel().getPrefName()+" "+getExamPeriod().getName();
	}
	
	public int compareTo(Object o) {
	    if (o==null || !(o instanceof ExamPeriodPref)) return super.compareTo(o);
	    ExamPeriodPref p = (ExamPeriodPref)o;
	    int cmp = getExamPeriod().compareTo(p.getExamPeriod());
	    if (cmp!=0) return cmp;
	    return super.compareTo(o);
	}
}