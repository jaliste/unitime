/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2011, UniTime LLC, and individual contributors
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
package org.unitime.timetable.onlinesectioning.test;

import java.util.ArrayList;
import java.util.List;

import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningTestFwk;
import org.unitime.timetable.onlinesectioning.solver.FindAssignmentAction;

public class FindAssignmentsTest extends OnlineSectioningTestFwk {
		
	public List<Operation> operations() {

		org.hibernate.Session hibSession = new _RootDAO().getSession();
		
		List<Operation> operations = new ArrayList<Operation>();
		
		for (final Long studentId: (List<Long>)hibSession.createQuery(
				"select s.uniqueId from Student s where s.session.uniqueId = :sessionId")
				.setLong("sessionId", getServer().getAcademicSession().getUniqueId()).list()) {
			
			CourseRequestInterface request = getServer().getRequest(studentId);
			if (request == null || request.getCourses().isEmpty()) continue;
			
			operations.add(new Operation() {
				@Override
				public double execute(OnlineSectioningServer s) {
					CourseRequestInterface request = s.getRequest(studentId);
					if (request != null && !request.getCourses().isEmpty()) {
						FindAssignmentAction action = new FindAssignmentAction(request, new ArrayList<ClassAssignmentInterface.ClassAssignment>()); 
						s.execute(action, user());
						return action.value();
					} else {
						return 1.0;
					}
				}
			});
		}
		
		hibSession.close();

		return operations;
	}
	
	public static void main(String[] args) {
		new FindAssignmentsTest().test(-1, 1, 2, 5, 10, 20, 50, 100, 250, 1000);
	}
}
