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
package org.unitime.timetable.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.unitime.timetable.webutil.BackTracker;


/**
 * @author Tomas Muller
 */
public class BackTree extends TagSupport {
	
	public int doStartTag() throws JspException {
		try {
			pageContext.getOut().print(
					BackTracker.getBackTree((HttpServletRequest)pageContext.getRequest())
				);
		} catch (Exception e) {
			throw new JspTagException(e.getMessage());
		}
		return SKIP_BODY;
	}
	
	public int doEndTag() {
		return EVAL_PAGE;
	}

}
