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

import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningTestFwk;
import org.unitime.timetable.onlinesectioning.updates.SaveStudentRequests;

public class SaveRequestsTest extends OnlineSectioningTestFwk {
	
	public List<Operation> operations() {
		org.hibernate.Session hibSession = new _RootDAO().getSession();
		
		List<Operation> saveRequests = new ArrayList<Operation>();
		
		for (final Long studentId: (List<Long>)hibSession.createQuery(
				"select s.uniqueId from Student s where s.session.uniqueId = :sessionId")
				.setLong("sessionId", getServer().getAcademicSession().getUniqueId()).list()) {
			saveRequests.add(new Operation() {
				@Override
				public double execute(OnlineSectioningServer s) {
					CourseRequestInterface request = s.getRequest(studentId);
					if (request != null)
						s.execute(new SaveStudentRequests(studentId, request, true), 
								user());
					return 1.0;
				}
			});
		}
		
		hibSession.close();
		
		return saveRequests;
	}
	
	public static void main(String args[]) {
		new SaveRequestsTest().test(-1, 100);
	}

}
