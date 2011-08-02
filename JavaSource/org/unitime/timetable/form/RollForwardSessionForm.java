/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008 - 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.form;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.model.dao.ClassInstructorDAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.CourseRequestDAO;
import org.unitime.timetable.model.dao.CurriculumDAO;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.model.dao.ExamPeriodDAO;
import org.unitime.timetable.model.dao.LastLikeCourseDemandDAO;
import org.unitime.timetable.model.dao.RoomFeatureDAO;
import org.unitime.timetable.model.dao.RoomGroupDAO;
import org.unitime.timetable.model.dao.StudentClassEnrollmentDAO;
import org.unitime.timetable.model.dao.TimetableManagerDAO;
import org.unitime.timetable.util.SessionRollForward;


/** 
 * MyEclipse Struts
 * Creation date: 02-27-2007
 * 
 * XDoclet definition:
 * @struts.form name="exportSessionToMsfForm"
 */
public class RollForwardSessionForm extends ActionForm {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7553214589949959977L;
	private Collection subjectAreas;
	private String[] subjectAreaIds; 
	private String buttonAction;
	private boolean isAdmin;
	private Collection toSessions;
	private Collection fromSessions;
	private Long sessionToRollForwardTo;
	private Boolean rollForwardDatePatterns;
	private Long sessionToRollDatePatternsForwardFrom;
	private Boolean rollForwardTimePatterns;
	private Long sessionToRollTimePatternsForwardFrom;
	private Boolean rollForwardDepartments;
	private Long sessionToRollDeptsFowardFrom;
	private Boolean rollForwardManagers;
	private Long sessionToRollManagersForwardFrom;
	private Boolean rollForwardRoomData;
	private Long sessionToRollRoomDataForwardFrom;
	private Boolean rollForwardSubjectAreas;
	private Long sessionToRollSubjectAreasForwardFrom;
	private Boolean rollForwardInstructorData;
	private Long sessionToRollInstructorDataForwardFrom;
	private Boolean rollForwardCourseOfferings;
	private Long sessionToRollCourseOfferingsForwardFrom;
	private Collection availableRollForwardSubjectAreas;
	private String[] rollForwardSubjectAreaIds;
	private Boolean rollForwardClassInstructors;
	private String[] rollForwardClassInstrSubjectIds;
	private Boolean addNewCourseOfferings;
	private String[] addNewCourseOfferingsSubjectIds;
	private Boolean rollForwardExamConfiguration;
	private Long sessionToRollExamConfigurationForwardFrom;
	private Boolean rollForwardMidtermExams;
	private Boolean rollForwardFinalExams;
	private Boolean rollForwardStudents;
	private Integer rollForwardStudentsMode;
	private String subpartLocationPrefsAction;
	private String subpartTimePrefsAction;
	private String classPrefsAction;
	private Boolean rollForwardCurricula;
	private Long sessionToRollCurriculaForwardFrom;
	
	/** 
	 * Method validate
	 * @param mapping
	 * @param request
	 * @return ActionErrors
	 */
	public ActionErrors validate(ActionMapping mapping,
			HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        validateSessionToRollForwardTo(errors);
        return errors;
	}
	
		
	private void validateRollForwardSessionHasNoDataOfType(ActionErrors errors, Session sessionToRollForwardTo, String rollForwardType, Collection checkCollection){
		if (checkCollection != null && !checkCollection.isEmpty()){
			errors.add("sessionHasData", new ActionMessage("errors.rollForward.sessionHasData", rollForwardType, sessionToRollForwardTo.getLabel()));			
		}		
	}
	protected void validateRollForward(ActionErrors errors, Session sessionToRollForwardTo, Long sessionIdToRollForwardFrom, String rollForwardType, Collection checkCollection){
		validateRollForwardSessionHasNoDataOfType(errors, sessionToRollForwardTo, rollForwardType,  checkCollection);
		Session sessionToRollForwardFrom = Session.getSessionById(sessionIdToRollForwardFrom);
		if (sessionToRollForwardFrom == null){
   			errors.add("mustSelectSession", new ActionMessage("errors.rollForward.missingFromSession", rollForwardType));			
		}
		if (sessionToRollForwardFrom.equals(sessionToRollForwardTo)){
			errors.add("sessionsMustBeDifferent", new ActionMessage("errors.rollForward.sessionsMustBeDifferent", rollForwardType, sessionToRollForwardTo.getLabel()));
		}	
	}

	
	public void validateDatePatternRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardDatePatterns().booleanValue()){
			validateRollForward(errors, toAcadSession, getSessionToRollDatePatternsForwardFrom(), "Date Patterns", DatePattern.findAll(toAcadSession, true, null, null));			
 		}
	}
	
	public void validateTimePatternRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardTimePatterns().booleanValue()){
			validateRollForward(errors, toAcadSession, getSessionToRollTimePatternsForwardFrom(), "Time Patterns", TimePattern.findAll(toAcadSession, null));			
 		}
	}

	public void validateDepartmentRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardDepartments().booleanValue()){
			validateRollForward(errors, toAcadSession, getSessionToRollDeptsFowardFrom(), "Departments", toAcadSession.getDepartments());			
		}
	}

	public void validateManagerRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardManagers().booleanValue()){
			TimetableManagerDAO tmDao = new TimetableManagerDAO();
			validateRollForward(errors, toAcadSession, getSessionToRollManagersForwardFrom(), "Managers", tmDao.getQuery("from TimetableManager tm inner join tm.departments d where d.session.uniqueId =" + toAcadSession.getUniqueId().toString()).list());
		}
	}
	
	public void validateBuildingAndRoomRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardRoomData().booleanValue()){
			validateRollForward(errors, toAcadSession, getSessionToRollRoomDataForwardFrom(), "Buildings", new ArrayList());
			validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Buildings", toAcadSession.getBuildings());
			validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Rooms", toAcadSession.getRooms());
			RoomFeatureDAO rfDao = new RoomFeatureDAO();
			validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Room Features", rfDao.getQuery("from RoomFeature rf where rf.department.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
			RoomGroupDAO rgDao = new RoomGroupDAO();
			validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Room Groups", rgDao.getQuery("from RoomGroup rg where rg.session.uniqueId = " + toAcadSession.getUniqueId().toString() + " and rg.global = 0").list());
		}		
	}

	public void validateSubjectAreaRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardSubjectAreas().booleanValue()){
			validateRollForward(errors, toAcadSession, getSessionToRollSubjectAreasForwardFrom(), "Subject Areas", toAcadSession.getSubjectAreas());			
		}		
	}
		
	public void validateInstructorDataRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardInstructorData().booleanValue()){
			DepartmentalInstructorDAO diDao = new DepartmentalInstructorDAO();
			validateRollForward(errors, toAcadSession, getSessionToRollInstructorDataForwardFrom(), "Instructors", diDao.getQuery("from DepartmentalInstructor di where di.department.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());			
		}		
	}

	public void validateCourseOfferingRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardCourseOfferings().booleanValue()){
			if (getSubpartLocationPrefsAction() != null 
					&& !getSubpartLocationPrefsAction().equalsIgnoreCase(SessionRollForward.DO_NOT_ROLL_ACTION)
					&& !getSubpartLocationPrefsAction().equalsIgnoreCase(SessionRollForward.ROLL_PREFS_ACTION)){
				errors.add("invalidSubpartLocationAction", new ActionMessage("errors.generic", "Invalid subpart location preference roll forward action:  " + getSubpartLocationPrefsAction()));			
			}
			if (getSubpartTimePrefsAction() != null 
					&& !getSubpartTimePrefsAction().equalsIgnoreCase(SessionRollForward.DO_NOT_ROLL_ACTION)
					&& !getSubpartTimePrefsAction().equalsIgnoreCase(SessionRollForward.ROLL_PREFS_ACTION)){
				errors.add("invalidSubpartTimeAction", new ActionMessage("errors.generic", "Invalid subpart time preference roll forward action:  " + getSubpartLocationPrefsAction()));			
			}
			if (getClassPrefsAction() != null 
					&& !getClassPrefsAction().equalsIgnoreCase(SessionRollForward.DO_NOT_ROLL_ACTION)
					&& !getClassPrefsAction().equalsIgnoreCase(SessionRollForward.PUSH_UP_ACTION)){
				errors.add("invalidClassAction", new ActionMessage("errors.generic", "Invalid class preference roll forward action:  " + getClassPrefsAction()));			
			}
			validateRollForward(errors, toAcadSession, getSessionToRollCourseOfferingsForwardFrom(), "Course Offerings", new ArrayList());
			CourseOfferingDAO coDao = new CourseOfferingDAO();
			for (int i = 0; i < getRollForwardSubjectAreaIds().length; i++){
				String queryStr = "from CourseOffering co where co.subjectArea.session.uniqueId = "
					+ toAcadSession.getUniqueId().toString()
					+ " and co.isControl = 1 and co.subjectArea.uniqueId  = '"
				    + getRollForwardSubjectAreaIds()[i] + "'";
				validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, ("Course Offerings - " + getRollForwardSubjectAreaIds()[i]), coDao.getQuery(queryStr).list());
			}			
		}
	}
	
	public void validateClassInstructorRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardClassInstructors().booleanValue()){
			validateRollForward(errors, toAcadSession, getSessionToRollCourseOfferingsForwardFrom(), "Class Instructors", new ArrayList());
			ClassInstructorDAO ciDao = new ClassInstructorDAO();
			for (int i = 0; i < getRollForwardSubjectAreaIds().length; i++){
				String queryStr = "from ClassInstructor c  inner join c.classInstructing.schedulingSubpart.instrOfferingConfig.instructionalOffering.courseOfferings as co where c.classInstructing.schedulingSubpart.instrOfferingConfig.instructionalOffering.session.uniqueId = "
					+ toAcadSession.getUniqueId().toString()
					+ " and co.isControl = 1 and co.subjectArea.uniqueId  = '"
				    + getRollForwardClassInstrSubjectIds()[i] + "'";
				validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, ("Class Instructors - " + getRollForwardClassInstrSubjectIds()[i]), ciDao.getQuery(queryStr).list());
			}			
		}
		
	}

	
	public void validateExamConfigurationRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardExamConfiguration().booleanValue()){
			ExamPeriodDAO epDao = new ExamPeriodDAO();
			validateRollForward(errors, toAcadSession, getSessionToRollExamConfigurationForwardFrom(), "Exam Configuration", epDao.getQuery("from ExamPeriod ep where ep.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());			
		}
	}

	public void validateMidtermExamRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardMidtermExams().booleanValue()){
			ExamDAO eDao = new ExamDAO();
			validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Midterm Exams", eDao.getQuery("from Exam e where e.session.uniqueId = " + toAcadSession.getUniqueId().toString() +" and e.examType = " + Exam.sExamTypeMidterm).list());			
		}
	}

	public void validateFinalExamRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardFinalExams().booleanValue()){
			ExamDAO epDao = new ExamDAO();
			validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Final Exams", epDao.getQuery("from Exam e where e.session.uniqueId = " + toAcadSession.getUniqueId().toString() +" and e.examType = " + Exam.sExamTypeFinal).list());			
		}
	}

	public void validateLastLikeDemandRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardStudents().booleanValue()) {
		    if (getRollForwardStudentsMode().intValue()==0) {
		        validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Last-like Student Course Requests", 
		                LastLikeCourseDemandDAO.getInstance().getQuery("from LastLikeCourseDemand d where d.subjectArea.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
		    } else if (getRollForwardStudentsMode().intValue()==1) {
		        validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Student Class Enrollments", 
		                StudentClassEnrollmentDAO.getInstance().getQuery("from StudentClassEnrollment d where d.courseOffering.subjectArea.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
		    } else {
                validateRollForwardSessionHasNoDataOfType(errors, toAcadSession, "Course Requests", 
                        CourseRequestDAO.getInstance().getQuery("from CourseRequest r where r.courseOffering.subjectArea.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());
		    }
		}
	}

	public void validateCurriculaRollForward(Session toAcadSession,ActionErrors errors){
		if (getRollForwardCurricula().booleanValue()){
			CurriculumDAO curDao = new CurriculumDAO();
			validateRollForward(errors, toAcadSession, getSessionToRollCurriculaForwardFrom(), "Curricula", curDao.getQuery("from Curriculum c where c.department.session.uniqueId = " + toAcadSession.getUniqueId().toString()).list());			
		}
	}

	
	public void validateSessionToRollForwardTo(ActionErrors errors){
		Session toAcadSession = Session.getSessionById(getSessionToRollForwardTo());
		if (toAcadSession == null){
   			errors.add("mustSelectSession", new ActionMessage("errors.rollForward.missingToSession"));
   			return;
		}
		
		validateDatePatternRollForward(toAcadSession, errors);
		validateTimePatternRollForward(toAcadSession, errors);
		validateDepartmentRollForward(toAcadSession, errors);
		validateManagerRollForward(toAcadSession, errors);
		validateBuildingAndRoomRollForward(toAcadSession, errors);
		validateSubjectAreaRollForward(toAcadSession, errors);
		validateInstructorDataRollForward(toAcadSession, errors);
		validateCourseOfferingRollForward(toAcadSession, errors);
		validateClassInstructorRollForward(toAcadSession, errors);
		validateExamConfigurationRollForward(toAcadSession, errors);
		validateMidtermExamRollForward(toAcadSession, errors);
		validateFinalExamRollForward(toAcadSession, errors);
		validateLastLikeDemandRollForward(toAcadSession, errors);
		validateCurriculaRollForward(toAcadSession, errors);
		
	}
	
	/** 
	 * Method init
	 */
	public void init() {
		subjectAreas = new ArrayList();
		subjectAreaIds = new String[0];
		isAdmin = false;
		fromSessions = null;
		toSessions = null;
		sessionToRollForwardTo = null;
		rollForwardDatePatterns = new Boolean(false);
		sessionToRollDatePatternsForwardFrom = null;
		rollForwardTimePatterns = new Boolean(false);
		sessionToRollTimePatternsForwardFrom = null;
		rollForwardDepartments = new Boolean(false);
		sessionToRollDeptsFowardFrom = null;
		rollForwardManagers = new Boolean(false);
		sessionToRollManagersForwardFrom = null;
		rollForwardRoomData = new Boolean(false);
		sessionToRollRoomDataForwardFrom = null;
		rollForwardSubjectAreas = new Boolean(false);
		sessionToRollSubjectAreasForwardFrom = null;
		rollForwardInstructorData = new Boolean(false);
		sessionToRollInstructorDataForwardFrom = null;
		rollForwardCourseOfferings = new Boolean(false);
		sessionToRollCourseOfferingsForwardFrom = null;
		availableRollForwardSubjectAreas = new ArrayList();
		rollForwardSubjectAreaIds = new String[0];
		rollForwardClassInstructors = new Boolean(false);
		rollForwardClassInstrSubjectIds = new String[0];
		addNewCourseOfferings = new Boolean(false);
		addNewCourseOfferingsSubjectIds = new String[0];
		rollForwardExamConfiguration = new Boolean(false);
		sessionToRollExamConfigurationForwardFrom = null;
		rollForwardMidtermExams = new Boolean(false);
		rollForwardFinalExams = new Boolean(false);
		rollForwardStudents = new Boolean(false);
		rollForwardStudentsMode = new Integer(0);
		subpartLocationPrefsAction = null;
		subpartTimePrefsAction = null;
		classPrefsAction = null;
		rollForwardCurricula = false;
		sessionToRollCurriculaForwardFrom = null;
	}

	/** 
	 * Method reset
	 * @param mapping
	 * @param request
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		init();
	}

	public String getButtonAction() {
		return buttonAction;
	}

	public void setButtonAction(String buttonAction) {
		this.buttonAction = buttonAction;
	}

	public String[] getSubjectAreaIds() {
		return subjectAreaIds;
	}

	public void setSubjectAreaIds(String[] subjectAreaIds) {
		this.subjectAreaIds = subjectAreaIds;
	}

	public Collection getSubjectAreas() {
		return subjectAreas;
	}

	public void setSubjectAreas(Collection subjectAreas) {
		this.subjectAreas = subjectAreas;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}


	public Collection getAvailableRollForwardSubjectAreas() {
		return availableRollForwardSubjectAreas;
	}

	public void setAvailableRollForwardSubjectAreas(
			Collection availableRollForwardSubjectAreas) {
		this.availableRollForwardSubjectAreas = availableRollForwardSubjectAreas;
	}

	public Boolean getRollForwardCourseOfferings() {
		return rollForwardCourseOfferings;
	}

	public void setRollForwardCourseOfferings(Boolean rollForwardCourseOfferings) {
		this.rollForwardCourseOfferings = rollForwardCourseOfferings;
	}

	public Boolean getRollForwardDatePatterns() {
		return rollForwardDatePatterns;
	}

	public void setRollForwardDatePatterns(Boolean rollForwardDatePatterns) {
		this.rollForwardDatePatterns = rollForwardDatePatterns;
	}

	public Boolean getRollForwardDepartments() {
		return rollForwardDepartments;
	}

	public void setRollForwardDepartments(Boolean rollForwardDepartments) {
		this.rollForwardDepartments = rollForwardDepartments;
	}

	public Boolean getRollForwardInstructorData() {
		return rollForwardInstructorData;
	}

	public void setRollForwardInstructorData(Boolean rollForwardInstructorData) {
		this.rollForwardInstructorData = rollForwardInstructorData;
	}

	public Boolean getRollForwardManagers() {
		return rollForwardManagers;
	}

	public void setRollForwardManagers(Boolean rollForwardManagers) {
		this.rollForwardManagers = rollForwardManagers;
	}

	public Boolean getRollForwardRoomData() {
		return rollForwardRoomData;
	}

	public void setRollForwardRoomData(Boolean rollForwardRoomData) {
		this.rollForwardRoomData = rollForwardRoomData;
	}

	public String[] getRollForwardSubjectAreaIds() {
		return rollForwardSubjectAreaIds;
	}

	public void setRollForwardSubjectAreaIds(String[] rollForwardSubjectAreaIds) {
		this.rollForwardSubjectAreaIds = rollForwardSubjectAreaIds;
	}

	public Boolean getRollForwardSubjectAreas() {
		return rollForwardSubjectAreas;
	}

	public void setRollForwardSubjectAreas(Boolean rollForwardSubjectAreas) {
		this.rollForwardSubjectAreas = rollForwardSubjectAreas;
	}

	public Long getSessionToRollCourseOfferingsForwardFrom() {
		return sessionToRollCourseOfferingsForwardFrom;
	}

	public void setSessionToRollCourseOfferingsForwardFrom(
			Long sessionToRollCourseOfferingsForwardFrom) {
		this.sessionToRollCourseOfferingsForwardFrom = sessionToRollCourseOfferingsForwardFrom;
	}

	public Long getSessionToRollDatePatternsForwardFrom() {
		return sessionToRollDatePatternsForwardFrom;
	}

	public void setSessionToRollDatePatternsForwardFrom(
			Long sessionToRollDatePatternsForwardFrom) {
		this.sessionToRollDatePatternsForwardFrom = sessionToRollDatePatternsForwardFrom;
	}

	public Long getSessionToRollDeptsFowardFrom() {
		return sessionToRollDeptsFowardFrom;
	}

	public void setSessionToRollDeptsFowardFrom(Long sessionToRollDeptsFowardFrom) {
		this.sessionToRollDeptsFowardFrom = sessionToRollDeptsFowardFrom;
	}

	public Long getSessionToRollForwardTo() {
		return sessionToRollForwardTo;
	}

	public void setSessionToRollForwardTo(Long sessionToRollForwardTo) {
		this.sessionToRollForwardTo = sessionToRollForwardTo;
	}

	public Long getSessionToRollInstructorDataForwardFrom() {
		return sessionToRollInstructorDataForwardFrom;
	}

	public void setSessionToRollInstructorDataForwardFrom(
			Long sessionToRollInstructorDataForwardFrom) {
		this.sessionToRollInstructorDataForwardFrom = sessionToRollInstructorDataForwardFrom;
	}

	public Long getSessionToRollManagersForwardFrom() {
		return sessionToRollManagersForwardFrom;
	}

	public void setSessionToRollManagersForwardFrom(
			Long sessionToRollManagersForwardFrom) {
		this.sessionToRollManagersForwardFrom = sessionToRollManagersForwardFrom;
	}

	public Long getSessionToRollRoomDataForwardFrom() {
		return sessionToRollRoomDataForwardFrom;
	}

	public void setSessionToRollRoomDataForwardFrom(
			Long sessionToRollRoomDataForwardFrom) {
		this.sessionToRollRoomDataForwardFrom = sessionToRollRoomDataForwardFrom;
	}

	public Long getSessionToRollSubjectAreasForwardFrom() {
		return sessionToRollSubjectAreasForwardFrom;
	}

	public void setSessionToRollSubjectAreasForwardFrom(
			Long sessionToRollSubjectAreasForwardFrom) {
		this.sessionToRollSubjectAreasForwardFrom = sessionToRollSubjectAreasForwardFrom;
	}

	public Collection getFromSessions() {
		return fromSessions;
	}

	public void setFromSessions(Collection fromSessions) {
		this.fromSessions = fromSessions;
	}


	public Boolean getRollForwardTimePatterns() {
		return rollForwardTimePatterns;
	}


	public void setRollForwardTimePatterns(Boolean rollForwardTimePatterns) {
		this.rollForwardTimePatterns = rollForwardTimePatterns;
	}


	public Long getSessionToRollTimePatternsForwardFrom() {
		return sessionToRollTimePatternsForwardFrom;
	}


	public void setSessionToRollTimePatternsForwardFrom(
			Long sessionToRollTimePatternsForwardFrom) {
		this.sessionToRollTimePatternsForwardFrom = sessionToRollTimePatternsForwardFrom;
	}


	public Boolean getRollForwardClassInstructors() {
		return rollForwardClassInstructors;
	}


	public void setRollForwardClassInstructors(Boolean rollForwardClassInstructors) {
		this.rollForwardClassInstructors = rollForwardClassInstructors;
	}


	public String[] getRollForwardClassInstrSubjectIds() {
		return rollForwardClassInstrSubjectIds;
	}


	public void setRollForwardClassInstrSubjectIds(
			String[] rollForwardClassInstrSubjectIds) {
		this.rollForwardClassInstrSubjectIds = rollForwardClassInstrSubjectIds;
	}


	public Collection getToSessions() {
		return toSessions;
	}


	public void setToSessions(Collection toSessions) {
		this.toSessions = toSessions;
	}


	public Boolean getAddNewCourseOfferings() {
		return addNewCourseOfferings;
	}


	public void setAddNewCourseOfferings(Boolean addNewCourseOfferings) {
		this.addNewCourseOfferings = addNewCourseOfferings;
	}


	public String[] getAddNewCourseOfferingsSubjectIds() {
		return addNewCourseOfferingsSubjectIds;
	}


	public void setAddNewCourseOfferingsSubjectIds(
			String[] addNewCourseOfferingsSubjectIds) {
		this.addNewCourseOfferingsSubjectIds = addNewCourseOfferingsSubjectIds;
	}


	public Boolean getRollForwardExamConfiguration() {
		return rollForwardExamConfiguration;
	}


	public void setRollForwardExamConfiguration(Boolean rollForwardExamConfiguration) {
		this.rollForwardExamConfiguration = rollForwardExamConfiguration;
	}


	public Boolean getRollForwardMidtermExams() {
		return rollForwardMidtermExams;
	}


	public void setRollForwardMidtermExams(Boolean rollForwardMidtermExams) {
		this.rollForwardMidtermExams = rollForwardMidtermExams;
	}


	public Boolean getRollForwardFinalExams() {
		return rollForwardFinalExams;
	}


	public void setRollForwardFinalExams(Boolean rollForwardFinalExams) {
		this.rollForwardFinalExams = rollForwardFinalExams;
	}


	public Long getSessionToRollExamConfigurationForwardFrom() {
		return sessionToRollExamConfigurationForwardFrom;
	}


	public void setSessionToRollExamConfigurationForwardFrom(
			Long sessionToRollExamConfigurationForwardFrom) {
		this.sessionToRollExamConfigurationForwardFrom = sessionToRollExamConfigurationForwardFrom;
	}
	
	public Boolean getRollForwardStudents() {
	    return rollForwardStudents;
	}
	
	public void setRollForwardStudents(Boolean rollForwardStudents) {
	    this.rollForwardStudents = rollForwardStudents;
	}
	
    public Integer getRollForwardStudentsMode() {
        return rollForwardStudentsMode;
    }
    
    public void setRollForwardStudentsMode(Integer rollForwardStudentsMode) {
        this.rollForwardStudentsMode = rollForwardStudentsMode;
    }
    
    public Boolean getRollForwardCurricula() {
    	return rollForwardCurricula;
    }
    
    public void setRollForwardCurricula(Boolean rollForwardCurricula) {
    	this.rollForwardCurricula = rollForwardCurricula;
    }
    
    public Long getSessionToRollCurriculaForwardFrom() {
    	return sessionToRollCurriculaForwardFrom;
    }
    
    public void setSessionToRollCurriculaForwardFrom(Long sessionToRollCurriculaForwardFrom) {
    	this.sessionToRollCurriculaForwardFrom = sessionToRollCurriculaForwardFrom;
    }


	/**
	 * @return the subpartLocationPrefsAction
	 */
	public String getSubpartLocationPrefsAction() {
		return subpartLocationPrefsAction;
	}


	/**
	 * @param subpartLocationPrefsAction the subpartLocationPrefsAction to set
	 */
	public void setSubpartLocationPrefsAction(String subpartLocationPrefsAction) {
		this.subpartLocationPrefsAction = subpartLocationPrefsAction;
	}


	/**
	 * @return the subpartTimePrefsAction
	 */
	public String getSubpartTimePrefsAction() {
		return subpartTimePrefsAction;
	}


	/**
	 * @param subpartTimePrefsAction the subpartTimePrefsAction to set
	 */
	public void setSubpartTimePrefsAction(String subpartTimePrefsAction) {
		this.subpartTimePrefsAction = subpartTimePrefsAction;
	}


	/**
	 * @return the classPrefsAction
	 */
	public String getClassPrefsAction() {
		return classPrefsAction;
	}


	/**
	 * @param classPrefsAction the classPrefsAction to set
	 */
	public void setClassPrefsAction(String classPrefsAction) {
		this.classPrefsAction = classPrefsAction;
	}
	
	public void copyTo(RollForwardSessionForm form) {
		form.subjectAreas = subjectAreas;
		form.subjectAreaIds = subjectAreaIds;
		form.buttonAction = buttonAction;
		form.isAdmin = isAdmin;
		form.toSessions = toSessions;
		form.fromSessions = fromSessions;
		form.sessionToRollForwardTo = sessionToRollForwardTo;
		form.rollForwardDatePatterns = rollForwardDatePatterns;
		form.sessionToRollDatePatternsForwardFrom = sessionToRollDatePatternsForwardFrom;
		form.rollForwardTimePatterns = rollForwardTimePatterns;
		form.sessionToRollTimePatternsForwardFrom = sessionToRollTimePatternsForwardFrom;
		form.rollForwardDepartments = rollForwardDepartments;
		form.sessionToRollDeptsFowardFrom = sessionToRollDeptsFowardFrom;
		form.rollForwardManagers = rollForwardManagers;
		form.sessionToRollManagersForwardFrom = sessionToRollManagersForwardFrom;
		form.rollForwardRoomData = rollForwardRoomData;
		form.sessionToRollRoomDataForwardFrom = sessionToRollRoomDataForwardFrom;
		form.rollForwardSubjectAreas = rollForwardSubjectAreas;
		form.sessionToRollSubjectAreasForwardFrom = sessionToRollSubjectAreasForwardFrom;
		form.rollForwardInstructorData = rollForwardInstructorData;
		form.sessionToRollInstructorDataForwardFrom = sessionToRollInstructorDataForwardFrom;
		form.rollForwardCourseOfferings = rollForwardCourseOfferings;
		form.sessionToRollCourseOfferingsForwardFrom = sessionToRollCourseOfferingsForwardFrom;
		form.availableRollForwardSubjectAreas = availableRollForwardSubjectAreas;
		form.rollForwardSubjectAreaIds = rollForwardSubjectAreaIds;
		form.rollForwardClassInstructors = rollForwardClassInstructors;
		form.rollForwardClassInstrSubjectIds = rollForwardClassInstrSubjectIds;
		form.addNewCourseOfferings = addNewCourseOfferings;
		form.addNewCourseOfferingsSubjectIds = addNewCourseOfferingsSubjectIds;
		form.rollForwardExamConfiguration = rollForwardExamConfiguration;
		form.sessionToRollExamConfigurationForwardFrom = sessionToRollExamConfigurationForwardFrom;
		form.rollForwardMidtermExams = rollForwardMidtermExams;
		form.rollForwardFinalExams = rollForwardFinalExams;
		form.rollForwardStudents = rollForwardStudents;
		form.rollForwardStudentsMode = rollForwardStudentsMode;
		form.subpartLocationPrefsAction = subpartLocationPrefsAction;
		form.subpartTimePrefsAction = subpartTimePrefsAction;
		form.classPrefsAction = classPrefsAction;
		form.rollForwardCurricula = rollForwardCurricula;
		form.sessionToRollCurriculaForwardFrom = sessionToRollCurriculaForwardFrom;
	}
	
	public Object clone() {
		RollForwardSessionForm form = new RollForwardSessionForm();
		copyTo(form);
		return form;
	}
}
