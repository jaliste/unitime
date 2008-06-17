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
package org.unitime.timetable.util;

import java.util.ArrayList;
import java.util.Iterator;

import org.unitime.timetable.model.RefTableEntry;


/**
 * @author Dagmar Murray
 *
 */
public class ReferenceList extends ArrayList {
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3760561970914801209L;

	public String labelForReference(String ref) {
		for (Iterator it = this.iterator(); it.hasNext();) {
			RefTableEntry r = (RefTableEntry) it.next();
			if (r.getReference().equals(ref)) 
				return r.getLabel();
		}
		return null;
	}
	
}
