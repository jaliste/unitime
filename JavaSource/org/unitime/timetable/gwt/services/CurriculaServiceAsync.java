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
package org.unitime.timetable.gwt.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CurriculaException;
import org.unitime.timetable.gwt.shared.CurriculumInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicAreaInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicClassificationInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.MajorInterface;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Tomas Muller
 */
public interface CurriculaServiceAsync {
	public void findCurricula(String filter, AsyncCallback<TreeSet<CurriculumInterface>> callback) throws CurriculaException;
	public void loadClassifications(List<Long> curriculumIds, AsyncCallback<List<CurriculumInterface.CurriculumClassificationInterface>> callback) throws CurriculaException;
	public void computeEnrollmentsAndLastLikes(Long acadAreaId, List<Long> majors, AsyncCallback<HashMap<String, CurriculumInterface.CurriculumStudentsInterface[]>> callback) throws CurriculaException;
	public void loadAcademicAreas(AsyncCallback<TreeSet<CurriculumInterface.AcademicAreaInterface>> callback) throws CurriculaException;
	public void loadAcademicClassifications(AsyncCallback<TreeSet<CurriculumInterface.AcademicClassificationInterface>> callback) throws CurriculaException;
	public void loadDepartments(AsyncCallback<TreeSet<CurriculumInterface.DepartmentInterface>> callback) throws CurriculaException;
	public void loadMajors(Long curriculumId, Long academicAreaId, AsyncCallback<TreeSet<CurriculumInterface.MajorInterface>> callback) throws CurriculaException;
	public void lastCurriculaFilter(AsyncCallback<String> callback) throws CurriculaException;
	public void loadCurriculum(Long curriculumId, AsyncCallback<CurriculumInterface> callback) throws CurriculaException;
	public void saveCurriculum(CurriculumInterface curriculum, AsyncCallback<Long> callback) throws CurriculaException;
	public void deleteCurriculum(Long curriculumId, AsyncCallback<Boolean> callback) throws CurriculaException;
	public void deleteCurricula(Set<Long> curriculumIds, AsyncCallback<Boolean> callback) throws CurriculaException;
	public void mergeCurricula(Set<Long> curriculumIds, AsyncCallback<Boolean> callback) throws CurriculaException;
	public void findCurriculaForACourse(String courseName, AsyncCallback<TreeSet<CurriculumInterface>> callback) throws CurriculaException;
	public void findCurriculaForAnInstructionalOffering(Long offeringId, AsyncCallback<TreeSet<CurriculumInterface>> callback) throws CurriculaException;
	public void saveClassifications(List<CurriculumInterface> curricula, AsyncCallback<Boolean> callback) throws CurriculaException;
	
	public void listCourseOfferings(String query, Integer limit, AsyncCallback<Collection<ClassAssignmentInterface.CourseAssignment>> callback) throws CurriculaException;
	public void retrieveCourseDetails(String course, AsyncCallback<String> callback) throws CurriculaException;
	public void listClasses(String course, AsyncCallback<Collection<ClassAssignmentInterface.ClassAssignment>> callback) throws CurriculaException;
	public void getApplicationProperty(String[] name, AsyncCallback<String[]> callback) throws CurriculaException;
	public void canAddCurriculum(AsyncCallback<Boolean> callback) throws CurriculaException;
	public void isAdmin(AsyncCallback<Boolean> callback) throws CurriculaException;
	public void makeupCurriculaFromLastLikeDemands(boolean lastLike, AsyncCallback<Boolean> callback) throws CurriculaException;
	public void updateCurriculaByProjections(Set<Long> curriculumIds, boolean updateCurriculumCourses,  AsyncCallback<Boolean> callback) throws CurriculaException;
	public void populateCourseProjectedDemands(boolean includeOtherStudents, AsyncCallback<Boolean> callback) throws CurriculaException;
	public void populateCourseProjectedDemands(boolean includeOtherStudents, Long offeringId, AsyncCallback<Boolean> callback) throws CurriculaException;

	
	public void loadProjectionRules(AsyncCallback<HashMap<AcademicAreaInterface, HashMap<MajorInterface, HashMap<AcademicClassificationInterface, Number[]>>>> callback) throws CurriculaException;
	public void saveProjectionRules(HashMap<AcademicAreaInterface, HashMap<MajorInterface, HashMap<AcademicClassificationInterface, Number[]>>> rules, AsyncCallback<Boolean> callback) throws CurriculaException;
	public void canEditProjectionRules(AsyncCallback<Boolean> callback) throws CurriculaException;
}
