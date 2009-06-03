/* 
 * UniTime 3.1 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2008-2009, UniTime LLC
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
package org.unitime.timetable.dataexchange;

import org.dom4j.Element;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.InstrOfferingConfig;

/**
 * @author says
 *
 */
public class CourseOfferingImport extends BaseCourseOfferingImport {

	/**
	 * 
	 */
	public CourseOfferingImport() {
		super();
		rootElementName = "offerings";
	}

	@Override
	protected boolean handleCustomCourseChildElements(CourseOffering courseOffering,
			Element courseOfferingElement) {
		// Core UniTime does not have any child elements for the course offering element
		return(false);
	}

	@Override
	protected boolean handleCustomClassChildElements(Element classElement,
			InstrOfferingConfig ioc, Class_ clazz) {
		// Core UniTime does not have any child elements for the class element
		return false;
	}

	@Override
	protected void postLoadAction() {
		// Core UniTime does not implement the post load action
		
	}

	@Override
	protected void preLoadAction() {
		// Core UniTime does not implement the pre load action
	}

	@Override
	protected boolean handleCustomInstrOffrConfigChildElements(
			InstrOfferingConfig instrOfferingConfig,
			Element instrOfferingConfigElement) throws Exception {
		// Core UniTime does not have any child elements for the instructional offering config element
		return false;
	}

}
