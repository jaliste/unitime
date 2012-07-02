/*
 * UniTime 3.4 (University Timetabling Application)
 * Copyright (C) 2012, UniTime LLC, and individual contributors
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
package org.unitime.timetable.security.authority;

import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.security.rights.Right;

public class StudentAuthority extends AbstractAuthority {
	private static final long serialVersionUID = 1L;
	public static final String TYPE = "Student";

	public StudentAuthority(Student student) {
		super(student.getUniqueId(), TYPE, student.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle));
	}
	
	@Override
	public boolean hasRight(Right right) {
		switch (right) {
		case SessionDefaultCurrent:
			return true;
		default:
			return false;
		}
	}

}
