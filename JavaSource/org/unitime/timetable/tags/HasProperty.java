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

import javax.servlet.jsp.tagext.BodyTagSupport;

import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.ManagerSettings;


/**
 * @author Tomas Muller
 */
public class HasProperty extends BodyTagSupport {
	private static final long serialVersionUID = 5400060812896606098L;
	private String iName;
	public String getName() { return iName; }
	public void setName(String name) { iName = name; }
	
	private boolean iUser = false;
	public boolean isUser() { return iUser; }
	public void setUser(boolean user) { iUser = user; }
	
	protected String getProperty() {
		if (isUser()) {
			return ManagerSettings.getValue(pageContext.getSession(), getName(),
					ApplicationProperties.getProperty(getName()));
		} else {
			return ApplicationProperties.getProperty(getName());
		}
	}

	public int doStartTag() {
		return EVAL_BODY_BUFFERED;
	}
	
	public int doEndTag() {
        try {
            String body = (getBodyContent()==null?null:getBodyContent().getString());
            String value = getProperty();
            if (value!=null && value.length()>0 && body!=null) 
                pageContext.getOut().println(body.replaceAll("%"+getName()+"%", value));
        } catch (Exception e) {}
		return EVAL_PAGE;
	}
}
