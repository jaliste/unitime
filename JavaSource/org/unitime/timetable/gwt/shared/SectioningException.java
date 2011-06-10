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
package org.unitime.timetable.gwt.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author Tomas Muller
 */
public class SectioningException extends RuntimeException implements IsSerializable {
	private static final long serialVersionUID = 1L;

	private SectioningExceptionType iType = SectioningExceptionType.UNKNOWN;
	private String iProblem = "N/A";
	
	public SectioningException() {
		super();
	}
	
	public SectioningException(SectioningExceptionType type, String problem, Throwable cause) {
		super(cause);
		if (type != null) iType = type;
		if (problem != null) iProblem = problem;
	}
	
	public SectioningException(SectioningExceptionType type, String problem) {
		this(type, problem, null);
	}

	public SectioningException(SectioningExceptionType type, Throwable cause) {
		this(type, cause.getMessage(), cause);
	}
	
	public SectioningException(SectioningExceptionType type) {
		this(type, null, null);
	}

	public String getProblem() { return iProblem; }
	public SectioningExceptionType getType() { return iType; }
	
	public String getMessage() {
		return iType.message(iProblem);
	}
}
