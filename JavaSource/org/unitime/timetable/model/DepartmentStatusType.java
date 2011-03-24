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
import java.util.TreeSet;

import org.hibernate.criterion.Restrictions;
import org.unitime.timetable.model.base.BaseDepartmentStatusType;
import org.unitime.timetable.model.dao.DepartmentStatusTypeDAO;


public class DepartmentStatusType extends BaseDepartmentStatusType implements Comparable{
	private static final long serialVersionUID = 1L;
	
	public static final int sCanManagerView				= 1;
	public static final int sCanManagerEdit				= 2;
	public static final int sCanManagerLimitedEdit		= 4; //e.g., assign instructors
	public static final int sCanOwnerView				= 8;
	public static final int sCanOwnerEdit				= 16;
	public static final int sCanOwnerLimitedEdit		= 32;
	public static final int sCanAudit					= 64;
	public static final int sCanTimetable				= 128;
	public static final int sCanCommit					= 256;
	public static final int sCanExamView				= 512;
	public static final int sCanExamEdit				= 1024;
	public static final int sCanExamTimetable			= 2048;
	public static final int sCanNoRoleReportExamFin		= 4096;
	public static final int sCanNoRoleReportExamMid		= 8192;
	public static final int sCanNoRoleReportClass		= 16384;
	public static final int sCanSectAssistStudents		= 32768;
	public static final int sCanPreRegisterStudents		= 65536; 
	public static final int sCanOnlineSectionStudents	= 131072;
	
	public static final int sApplySession    = 1;
	public static final int sApplyDepartment = 2;
	
/*[CONSTRUCTOR MARKER BEGIN]*/
	public DepartmentStatusType () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public DepartmentStatusType (Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/
	
	/**
	 * 
	 */
	public static DepartmentStatusType findById(Integer uid) throws Exception {
		if (uid==null) return null;
		return (DepartmentStatusType)
			(new DepartmentStatusTypeDAO()).
			getSession().
			createCriteria(DepartmentStatusType.class).
			add(Restrictions.eq("uniqueId", uid)).
			uniqueResult();
	}
	
	public static TreeSet findAll() {
		return new TreeSet((new DepartmentStatusTypeDAO().findAll()));
	}
	
	public static TreeSet findAll(int apply) {
		TreeSet ret = findAll();
		for (Iterator i=ret.iterator();i.hasNext();) {
			DepartmentStatusType t = (DepartmentStatusType)i.next();
			if (!t.apply(apply)) i.remove();
		}
		return ret;
	}
	
	public static TreeSet findAllForSession() {
		return findAll(sApplySession);
	}

	public static TreeSet findAllForDepartment() {
		return findAll(sApplyDepartment);
	}

	public static DepartmentStatusType findByRef(String ref) {
		if (ref==null) return null;
		return (DepartmentStatusType)
			(new DepartmentStatusTypeDAO()).
			getSession().
			createCriteria(DepartmentStatusType.class).
			add(Restrictions.eq("reference", ref)).
			uniqueResult();
	}

	public int compareTo(Object o) {
        if (o==null || !(o instanceof DepartmentStatusType)) return -1;
        DepartmentStatusType t = (DepartmentStatusType) o;
        return getOrd().compareTo(t.getOrd());
	}
	
	public boolean can(int operation) {
		return (getStatus().intValue() & operation) == operation;
	}
	
	public boolean canManagerEdit() {
		return can(sCanManagerEdit);
	}
	
	public boolean canManagerLimitedEdit() {
		return can(sCanManagerLimitedEdit);
	}

	public boolean canManagerView() {
		return can(sCanManagerView);
	}

	public boolean canOwnerEdit() {
		return can(sCanOwnerEdit);
	}

	public boolean canOwnerLimitedEdit() {
		return can(sCanOwnerLimitedEdit);
	}

	public boolean canOwnerView() {
		return can(sCanOwnerView);
	}

	public boolean canAudit() {
		return can(sCanAudit);
	}

	public boolean canTimetable() {
		return can(sCanTimetable);
	}

	public boolean canCommit() {
		return can(sCanCommit);
	}
	
	public boolean canExamView() {
	    return can(sCanExamView);
	}
	
    public boolean canExamEdit() {
        return can(sCanExamEdit);
    }

    public boolean canExamTimetable() {
        return can(sCanExamTimetable);
    }
    
    public boolean canNoRoleReportExamFinal() {
        return can(sCanNoRoleReportExamFin);
    }

    public boolean canNoRoleReportExamMidterm() {
        return can(sCanNoRoleReportExamMid);
    }

    public boolean canNoRoleReportClass() {
        return can(sCanNoRoleReportClass);
    }
    
    public boolean canSectionAssistStudents() {
        return can(sCanSectAssistStudents);
    }
    
    public boolean canPreRegisterStudents() {
    	return can(sCanPreRegisterStudents);
    }

    public boolean canOnlineSectionStudents() {
    	return can(sCanOnlineSectionStudents);
    }

    public boolean canNoRoleReportExam() {
        return canNoRoleReportExamFinal() || canNoRoleReportExamMidterm();
    }

    public boolean canNoRoleReport() {
        return canNoRoleReportClass() || canNoRoleReportExam();
    }
    
    public boolean apply(int apply) {
		return (getApply().intValue() & apply) == apply;
	}
	
	public boolean applySession() {
		return apply(sApplySession);
	}

	public boolean applyDepartment() {
		return apply(sApplyDepartment);
	}
	
	/** Status is active when someone can edit, timetable or commit*/
	public boolean isActive() {
	    return canTimetable() || canCommit() || canManagerEdit() || canOwnerEdit() || canManagerLimitedEdit() || canOwnerLimitedEdit() || canExamEdit() || canExamTimetable();
	}
}
