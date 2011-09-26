/*
 * UniTime 3.2 (University Timetabling Application)
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
package org.unitime.timetable.onlinesectioning;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.SectioningException;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.studentsct.constraint.LinkedSections;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * @author Tomas Muller
 */
public interface OnlineSectioningServer {
	public AcademicSessionInfo getAcademicSession();
	public DistanceMetric getDistanceMetric();
	public DataProperties getConfig();
	
	public ClassAssignmentInterface getAssignment(Long studentId);
	public CourseRequestInterface getRequest(Long studentId);
	public List<ClassAssignmentInterface.Enrollment> listEnrollments(Long offeringId);
	
	public Collection<CourseInfo> findCourses(String query, Integer limit);
	public List<Section> getSections(CourseInfo courseInfo);
	
	public CourseInfo getCourseInfo(Long courseId);
	public CourseInfo getCourseInfo(String course);
	
	public Student getStudent(Long studentId);
	public Section getSection(Long classId);
	public Course getCourse(Long courseId);
	public Offering getOffering(Long offeringId);
	
	public URL getSectionUrl(Long courseId, Section section);
	
	public Collection<String> checkCourses(CourseRequestInterface req);
	
	public <E> E execute(OnlineSectioningAction<E> action) throws SectioningException;
	public <E> void execute(OnlineSectioningAction<E> action, Callback<E> callback) throws SectioningException;
	
	public void remove(Student student);
	public void update(Student student);
	public void remove(Offering offering);
	public void update(Offering offering);
	public void update(CourseInfo info);
	public void clearAll();
	public void clearAllStudents();
	
	public void addLinkedSections(LinkedSections link);
	public Collection<LinkedSections> getLinkedSections(Long offeringId);
	public void removeLinkedSections(Long offeringId);
	
	public void notifyStudentChanged(Long studentId, List<Request> oldRequests, List<Request> newRequests);
	public void notifyStudentChanged(Long studentId, Request request, Enrollment oldEnrollment);
	
	public Lock readLock();
	public Lock writeLock();
	public Lock lockAll();
	public Lock lockStudent(Long studentId, Collection<Long> offeringIds, boolean excludeLockedOfferings);
	public Lock lockOffering(Long offeringId, Collection<Long> studentIds, boolean excludeLockedOffering);
	public Lock lockClass(Long classId, Collection<Long> studentIds);
	public Lock lockRequest(CourseRequestInterface request);
	
	public boolean isOfferingLocked(Long offeringId);
	public void lockOffering(Long offeringId);
	public void unlockOffering(Long offeringId);
	public Collection<Long> getLockedOfferings();
	public void releaseAllOfferingLocks();
	
	public int distance(Section s1, Section s2);
	
	public void unload();
	
	public static interface Lock {
		void release();
	}
	
	public static interface Callback<E> {
		public void onFailure(Throwable exception);
		public void onSuccess(E result);
	}
}
