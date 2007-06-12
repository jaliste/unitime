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
package org.unitime.timetable.solver.ui;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Solution;


/**
 * @author Tomas Muller
 */
public class SolutionUnassignedClassesModel extends UnassignedClassesModel {
	
	public SolutionUnassignedClassesModel(Collection solutions, org.hibernate.Session hibSession, String instructorFormat) {
		super();
		for (Iterator i=solutions.iterator();i.hasNext();) {
			Solution solution = (Solution)i.next();
			for (Iterator j=solution.getOwner().getNotAssignedClasses(solution).iterator();j.hasNext();) {
				Class_ clazz = (Class_)j.next();
				String name = clazz.getClassLabel();
				String onClick = "window.open('suggestions.do?id="+clazz.getUniqueId()+"&op=Reset','suggestions','width=1000,height=600,resizable=yes,scrollbars=yes,toolbar=no,location=no,directories=no,status=yes,menubar=no,copyhistory=no');";
				Vector leads = clazz.getLeadInstructors();
				StringBuffer leadsSb = new StringBuffer();
				for (Enumeration e=leads.elements();e.hasMoreElements();) {
					DepartmentalInstructor instructor = (DepartmentalInstructor)e.nextElement();
					leadsSb.append(instructor.getName(instructorFormat));
					if (e.hasMoreElements()) leadsSb.append(";");
				}
				String instructorName = leadsSb.toString();
				int nrStudents = ((Number)hibSession.
						createQuery("select count(s) from StudentEnrollment as s where s.clazz.uniqueId=:classId and s.solution.uniqueId=:solutionId").
						setLong("classId",clazz.getUniqueId().longValue()).
						setInteger("solutionId",solution.getUniqueId().intValue()).
						uniqueResult()).intValue();
				rows().addElement(new UnassignedClassRow(onClick, name, instructorName, nrStudents, null, clazz));
			}
		}
	}
}
