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
package org.unitime.timetable.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public interface SessionContext {
	
	public boolean isAuthenticated();
	public UserContext getUser();
    
    @Deprecated
    public HttpSession getHttpSession();
    public boolean isHttpSessionNew();
    public String getHttpSessionId();
    
    public Object getAttribute(String name);
    public void removeAttribute(String name);
    public void setAttribute(String name, Object value);

    @Deprecated
    public HttpServletRequest getHttpServletRequest();
    
}