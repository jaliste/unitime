/*
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org, and individual contributors
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
package org.unitime.timetable.solver.ui;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.solver.interactive.ClassAssignmentDetails;

import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * @author Tomas Muller
 */
public class PerturbationReport implements Serializable {
	private static final long serialVersionUID = 1L;
	private HashSet iGroups = new HashSet();
	
	public PerturbationReport(Solver solver) {
		TimetableModel model = (TimetableModel)solver.currentSolution().getModel();
		for (Enumeration e=model.perturbVariables().elements();e.hasMoreElements();) {
			Lecture lecture = (Lecture)e.nextElement();
			Placement placement = (Placement)lecture.getAssignment();
			Placement initial = (Placement)lecture.getInitialAssignment();
			if (placement==null || initial==null || placement.equals(initial)) continue;
			iGroups.add(new PerturbationGroup(solver,lecture));
		}
		
	}
	
	public Set getGroups() {
		return iGroups;
	}
	
	public class PerturbationGroup implements Serializable {
		private static final long serialVersionUID = 1L;
		private ClassAssignmentDetails iDetail = null;
        public long affectedStudents=0;
        public int affectedInstructors=0;
        public long affectedStudentsByTime=0;
        public int affectedInstructorsByTime=0;
        public int differentRoom=0;
        public int affectedInstructorsByRoom=0;
        public long affectedStudentsByRoom=0;
        public int differentBuilding=0;
        public int affectedInstructorsByBldg=0;
        public long affectedStudentsByBldg=0;
        public int deltaRoomPreferences=0;
        public int differentTime=0;
        public int differentDay=0;
        public int differentHour=0;
        public int tooFarForInstructors=0;
        public int tooFarForStudents=0;
        public int deltaStudentConflicts=0;
        public int newStudentConflicts=0;
        public double deltaTimePreferences=0;
        public int deltaInstructorDistancePreferences=0;
        public double distance = 0;
        
		public PerturbationGroup(Solver solver, Lecture lecture) {
			Placement assignedPlacement = (Placement)lecture.getAssignment();
			Placement initialPlacement = (Placement)lecture.getInitialAssignment();
			iDetail = new ClassAssignmentDetails(solver, lecture, initialPlacement, false);
			iDetail.setAssigned(new AssignmentPreferenceInfo(solver, assignedPlacement, false), assignedPlacement.getRoomIds(),assignedPlacement.getTimeLocation().getDayCode(), assignedPlacement.getTimeLocation().getStartSlot());
			
	        affectedStudents = lecture.classLimit();
	        affectedInstructors= lecture.getInstructorConstraints().size();
	        affectedStudentsByTime=(initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())?0:lecture.classLimit());
	        affectedInstructorsByTime=(initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())?0:lecture.getInstructorConstraints().size());
	        
	    	differentRoom = initialPlacement.nrDifferentRooms(assignedPlacement);
	        affectedInstructorsByRoom = differentRoom*lecture.getInstructorConstraints().size();
	        affectedStudentsByRoom = differentRoom*lecture.classLimit();
	        
	        differentBuilding = initialPlacement.nrDifferentBuildings(assignedPlacement);
	        affectedInstructorsByBldg = differentBuilding*lecture.getInstructorConstraints().size();
	        affectedStudentsByBldg = differentBuilding*lecture.classLimit();
	        
	        deltaRoomPreferences = assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference();

	        differentTime=(initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())?0:1);
	        differentDay=(initialPlacement.getTimeLocation().getDayCode()!=assignedPlacement.getTimeLocation().getDayCode()?1:0);
	        differentHour=(initialPlacement.getTimeLocation().getStartSlot()!=assignedPlacement.getTimeLocation().getStartSlot()?1:0);
	        deltaStudentConflicts=lecture.countStudentConflicts(assignedPlacement)-lecture.countInitialStudentConflicts();
	        deltaTimePreferences=(assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference());
	        
	        distance = Placement.getDistance(initialPlacement, assignedPlacement);
	        if (!lecture.getInstructorConstraints().isEmpty()) {
	            if (distance>0.0 && distance<=5.0) {
	                tooFarForInstructors+=PreferenceLevel.sIntLevelDiscouraged;
	            } else if (distance>5.0 && distance<=20.0) {
	                tooFarForInstructors+=PreferenceLevel.sIntLevelStronglyDiscouraged;
	            } else if (distance>20.0) {
	                tooFarForInstructors+=PreferenceLevel.sIntLevelProhibited;
	            }
	        }
	        if (distance > ((TimetableModel)lecture.getModel()).getStudentDistanceLimit())
	        	tooFarForStudents = (int)lecture.classLimit();
	        
	        Set newStudentConflictsVect = lecture.conflictStudents(assignedPlacement);
	        Set initialStudentConflicts = lecture.initialStudentConflicts();
	        for (Iterator e=newStudentConflictsVect.iterator();e.hasNext();)
	            if (!initialStudentConflicts.contains(e.next())) newStudentConflicts++;


	       	for (Enumeration e=lecture.getInstructorConstraints().elements();e.hasMoreElements();) {
	       		InstructorConstraint ic = (InstructorConstraint)e.nextElement();
	       		for (Enumeration f=ic.variables().elements();f.hasMoreElements();) {
	       			Lecture lect = (Lecture)f.nextElement();
	       			if (lect.equals(lecture)) continue;
	       			int initialPreference = (lect.getInitialAssignment()==null?PreferenceLevel.sIntLevelNeutral:ic.getDistancePreference(initialPlacement,(Placement)lect.getInitialAssignment()));
	       			int assignedPreference = (lect.getAssignment()==null?PreferenceLevel.sIntLevelNeutral:ic.getDistancePreference(assignedPlacement,(Placement)lect.getAssignment()));
	       			deltaInstructorDistancePreferences += (assignedPreference - initialPreference);
	       		}
	        }
		}
		
		public ClassAssignmentDetails getClazz() { return iDetail; }
		
	}
	
}
