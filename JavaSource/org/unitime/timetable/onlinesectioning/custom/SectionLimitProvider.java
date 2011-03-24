/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.onlinesectioning.custom;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.studentsct.model.Section;

import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;

/**
 * @author Tomas Muller
 */
public interface SectionLimitProvider {

	public int[] getSectionLimit(AcademicSessionInfo session, Long courseId, Section section);
	public Map<Long, int[]> getSectionLimits(AcademicSessionInfo session, Long courseId, Collection<Section> sections);
	public Map<Long, int[]> getSectionLimitsFromCache(AcademicSessionInfo session, Long courseId, Collection<Section> sections);

}
