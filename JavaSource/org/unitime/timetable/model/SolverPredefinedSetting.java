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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.unitime.timetable.model.base.BaseSolverPredefinedSetting;
import org.unitime.timetable.model.dao.SolverPredefinedSettingDAO;



public class SolverPredefinedSetting extends BaseSolverPredefinedSetting {
	private static final long serialVersionUID = 1L;
	public static String[] sAppearances = new String[] {"Timetables","Solver","Examination Solver","Student Sectioning Solver"};
	public static Integer APPEARANCE_TIMETABLES = new Integer(0);
	public static Integer APPEARANCE_SOLVER = new Integer(1);
	public static Integer APPEARANCE_EXAM_SOLVER = new Integer(2);
	public static Integer APPEARANCE_STUDENT_SOLVER = new Integer(3);


/*[CONSTRUCTOR MARKER BEGIN]*/
	public SolverPredefinedSetting () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public SolverPredefinedSetting (java.lang.Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/


	/**
	 * Get the default value for a given key
	 * @param key Setting key
	 * @return Default value if found, null otherwise
	 */
	public static SolverPredefinedSetting findByName(String name) {
		List list = (new SolverPredefinedSettingDAO()).getSession().
			createCriteria(SolverPredefinedSetting.class).add(Restrictions.eq("name", name)).setCacheable(true).list();
		
		if (list.isEmpty()) return null;

		return (SolverPredefinedSetting)list.get(0);
	}
	
	public static String[] getNames(Integer appearance) {
		List list = (new SolverPredefinedSettingDAO()).getSession().
			createCriteria(SolverPredefinedSetting.class).add(Restrictions.eq("appearance", appearance)).addOrder(Order.asc("name")).setCacheable(true).list();

		if (list.isEmpty())
			return new String[] {};
		
		String[] names = new String[list.size()];
		for (int i=0;i<list.size();i++) {
			SolverPredefinedSetting set = (SolverPredefinedSetting)list.get(i);
			names[i]=set.getName();
		}
		return names;
	}	

	public static Vector getIdValueList(Integer appearance) {
		List list = (new SolverPredefinedSettingDAO()).getSession().
			createCriteria(SolverPredefinedSetting.class).add(Restrictions.eq("appearance", appearance)).addOrder(Order.asc("name")).setCacheable(true).list();
		
		Vector idValueList = new Vector();

		for (Iterator i=list.iterator();i.hasNext();) {
			SolverPredefinedSetting set = (SolverPredefinedSetting)i.next();
			idValueList.add(new IdValue(set.getUniqueId(),set.getDescription()));
		}
		
		return idValueList;
	}	
	
	public static class IdValue {
		private Long iId;
		private String iValue;
		private String iType;
		private boolean iEnabled;
		public IdValue(Long id, String value) {
			this(id,value,null,true);
		}
		public IdValue(Long id, String value, String type) {
			this(id,value,type,true);
		}
		public IdValue(Long id, String value, String type, boolean enabled) {
			iId = id; iValue = value; iType = type; iEnabled = enabled;
		}
		public Long getId() { return iId; }
		public String getValue() { return iValue; }
		public String getType() { return iType;}
		public boolean getEnabled() { return iEnabled; }
		public boolean getDisabled() { return !iEnabled; }
	}	
}
