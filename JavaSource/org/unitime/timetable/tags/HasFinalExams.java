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
package org.unitime.timetable.tags;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.security.SessionContext;


/**
 * @author Tomas Muller
 */
public class HasFinalExams extends HasMidtermExams {
	private static final long serialVersionUID = -2181817157444320749L;
	
    public SessionContext getSessionContext() {
    	return (SessionContext) WebApplicationContextUtils.getWebApplicationContext(pageContext.getServletContext()).getBean("sessionContext");
    }

	public boolean includeContent() {
        try {
            return getSessionContext().isAuthenticated() && Exam.hasFinalExams(getSessionContext().getUser().getCurrentAcademicSessionId());
        } catch (Exception e) {}
        return false;
    }
}
