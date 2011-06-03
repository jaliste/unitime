/*
 * Copyright (C) 2011, UniTime LLC, and individual contributors
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
package org.unitime.timetable.onlinesectioning.solver;

import java.util.List;

import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Choice;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Section;

public class ResectioningWeights extends StudentSchedulingAssistantWeights {
	private double iSameChoiceFactor = 0.125;
	private double iSameRoomsFactor = 0.007;
	private double iSameTimeFactor = 0.070;
	private double iSameNameFactor = 0.014;
	private LastSectionProvider iLastSectionProvider = null;
	
	public ResectioningWeights(DataProperties properties) {
		super(properties);
		iSameChoiceFactor = properties.getPropertyDouble("StudentWeights.SameChoiceFactor", iSameChoiceFactor);
		iSameRoomsFactor = properties.getPropertyDouble("StudentWeights.SameRoomsFactor", iSameRoomsFactor);
		iSameTimeFactor = properties.getPropertyDouble("StudentWeights.SameTimeFactor", iSameTimeFactor);
		iSameNameFactor = properties.getPropertyDouble("StudentWeights.SameNameFactor", iSameNameFactor);
	}
	
	public void setLastSectionProvider(LastSectionProvider lastSectionProvider) { iLastSectionProvider = lastSectionProvider; }
	
	@Override
	public double getWeight(Enrollment enrollment) {
		double weight = super.getWeight(enrollment);
		
		if (enrollment.isCourseRequest() && enrollment.getAssignments() != null && iLastSectionProvider != null) {
			int sameChoice = 0;
			int sameTime = 0;
			int sameRooms = 0;
			int sameName = 0;
        	for (Section section: enrollment.getSections()) {
				if (iLastSectionProvider.sameLastChoice(section)) sameChoice++;
				if (iLastSectionProvider.sameLastTime(section)) sameTime++;
				if (iLastSectionProvider.sameLastRoom(section)) sameRooms++;
				if (iLastSectionProvider.sameLastName(section, enrollment.getCourse())) sameName++;
        	}
    		CourseRequest cr = (CourseRequest)enrollment.getRequest();
        	if (sameChoice == 0 && !cr.getSelectedChoices().isEmpty()) {
            	for (Section section: enrollment.getSections()) {
            		if (cr.getSelectedChoices().contains(section.getChoice())) {
            			sameChoice++; continue;
            		}
            	}
        	}
    		int size = enrollment.getAssignments().size();
    		double sameChoiceFraction = (size - sameChoice) / size;
    		double sameTimeFraction = (size - sameTime) / size;
    		double sameRoomsFraction = (size - sameRooms) / size;
    		double sameNameFraction = (size - sameName) / size;
			double base = getBaseWeight(enrollment);
			weight -= sameChoiceFraction * base * iSameChoiceFactor;
			weight -= sameTimeFraction * base * iSameTimeFactor;
			weight -= sameRoomsFraction * base * iSameRoomsFactor;
			weight -= sameNameFraction * base * iSameNameFactor;
		}
		
		return weight;
	}
	
	public static boolean sameRooms(Section s, List<RoomLocation> rooms) {
		if (s.getRooms() == null && rooms == null) return true;
		if (s.getRooms() == null || rooms == null) return false;
		return
			s.getRooms().size() == rooms.size() &&
			s.getRooms().containsAll(rooms);
	}
	
	public static boolean sameTime(Section s, TimeLocation t) {
		if (s.getTime() == null && t == null) return true;
		if (s.getTime() == null || t == null) return false;
		return
			s.getTime().getStartSlot() == t.getStartSlot() &&
			s.getTime().getLength() == t.getLength() && 
			s.getTime().getDayCode() == t.getDayCode() && 
			ToolBox.equals(s.getTime().getDatePatternName(), t.getDatePatternName());
	}
	
	public static boolean sameChoice(Section s, Choice ch) {
		return sameChoice(s, ch == null ? null : ch.getId());
	}
	
	public static boolean sameChoice(Section s, String ch) {
		if (s.getChoice() == null && ch == null) return true;
		if (s.getChoice() == null || ch == null) return false;
		return s.getChoice().getId().equals(ch);
	}

	public static boolean sameName(Long courseId, Section s1, Section s2) {
		return s1.getName(courseId).equals(s2.getName(courseId));
	}
	
	public static boolean isSame(Enrollment e1, Enrollment e2) {
		if (e1.getSections().size() != e2.getSections().size()) return false;
		s1: for (Section s1: e1.getSections()) {
			for (Section s2: e2.getSections())
				if (sameChoice(s1, s2.getChoice())) continue s1;
			return false;
		}
		return true;
	}
	
	public static boolean isVerySame(Enrollment e1, Enrollment e2) {
		if (e1.getSections().size() != e2.getSections().size()) return false;
		s1: for (Section s1: e1.getSections()) {
			for (Section s2: e2.getSections())
				if (sameName(e1.getCourse().getId(), s1, s2) && sameTime(s1, s2.getTime()) && sameRooms(s1, s2.getRooms())) continue s1;
			return false;
		}
		return true;
	}
	
	public static interface LastSectionProvider {
		public boolean sameLastChoice(Section current);
		public boolean sameLastTime(Section current);
		public boolean sameLastRoom(Section current);
		public boolean sameLastName(Section current, Course course);
	}
}
