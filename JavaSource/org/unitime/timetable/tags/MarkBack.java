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
import javax.servlet.jsp.tagext.TagSupport;

import org.unitime.timetable.webutil.BackTracker;


/**
 * @author Tomas Muller
 */
public class MarkBack extends TagSupport {
	boolean iBack = true;
	boolean iClear = false;
	String iUri = null;
	String iTitle = null;
	
	public boolean getBack() { return iBack; }
	public void setBack(boolean back) { iBack = back; }
	public boolean getClear() { return iClear; }
	public void setClear(boolean clear) { iClear = clear; }
	public String getUri() { return iUri; }
	public void setUri(String uri) { iUri = uri; }
	public String getTitle() { return iTitle; }
	public void setTitle(String title) { iTitle = title; }
	
	public int doStartTag() throws JspException {
		BackTracker.markForBack((HttpServletRequest)pageContext.getRequest(), getUri(), getTitle(), getBack(), getClear());
		return SKIP_BODY;
	}
	
	public int doEndTag() {
		return EVAL_PAGE;
	}
	
}
