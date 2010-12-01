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
package org.unitime.timetable.solver.curricula.students;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * @author Tomas Muller
 */
public class CurSimpleMove implements NeighbourSelection<CurVariable, CurValue>{
	private CurVariableSelection iVariableSelection;
	private CurValueSelection iValueSelection;

	protected CurSimpleMove(DataProperties	config) {
		iVariableSelection = new CurVariableSelection(config);
		iValueSelection = new CurValueSelection(config);
	}

	@Override
	public void init(Solver<CurVariable, CurValue> solver) {
	}

	@Override
	public Neighbour<CurVariable, CurValue> selectNeighbour(
			Solution<CurVariable, CurValue> solution) {
		CurVariable var = iVariableSelection.selectVariable(solution);
		if (var == null)
			return null;
		CurValue val = iValueSelection.selectValueFast(solution, var);
		return (val == null ? null : new CurSimpleAssignment(val));
	}
}
