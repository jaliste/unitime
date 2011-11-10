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
package org.unitime.timetable.gwt.resources;

import com.google.gwt.i18n.client.Messages;

/**
 * @author Tomas Muller
 */
public interface StudentSectioningMessages extends Messages {
	/*  General messages
	 */
	@DefaultMessage("{0} {1}")
	String courseName(String subject, String courseNbr);
	
	@DefaultMessage("{0} {1} - {2}")
	String courseNameWithTitle(String subject, String courseNbr, String title);

	
	/*  Common column names
	 */
	@DefaultMessage("Lock")
	String colLock();
	
	@DefaultMessage("Subject")
	String colSubject();
	
	@DefaultMessage("Course")
	String colCourse();
	
	@DefaultMessage("Type")
	String colSubpart();
	
	@DefaultMessage("Class")
	String colClass();
	
	@DefaultMessage("Avail")
	String colLimit();
	
	@DefaultMessage("Days")
	String colDays();
	
	@DefaultMessage("Time")
	String colTime();

	@DefaultMessage("Start")
	String colStart();
	
	@DefaultMessage("End")
	String colEnd();
	
	@DefaultMessage("Date")
	String colDate();
	
	@DefaultMessage("Room")
	String colRoom();
	
	@DefaultMessage("Instructor")
	String colInstructor();
	
	@DefaultMessage("Requires")
	String colParent();
	
	@DefaultMessage("&nbsp;")
	String colSaved();
	
	@DefaultMessage("&nbsp;")
	String colHighDemand();

	@DefaultMessage("Title")
	String colTitle();
	
	@DefaultMessage("Note")
	String colNote();

	@DefaultMessage("Year")
	String colYear();
	
	@DefaultMessage("Term")
	String colTerm();
	
	@DefaultMessage("Campus")
	String colCampus();

	@DefaultMessage("&nbsp;")
	String colNoteIcon();

	/* Academic Session Selector messages
	 */
	@DefaultMessage("No academic session is selected.")
	String sessionSelectorNoSession();
	
	@DefaultMessage("Click here to change the session.")
	String sessionSelectorHint();
	
	@DefaultMessage("Select Academic Session ...")
	String sessionSelectorSelect();
	
	@DefaultMessage("Loading academic sessions ...")
	String sessionSelectorLoading();
	
	@DefaultMessage("Session: {1} {0} ({2})")
	String sessionSelectorLabel(String year, String term, String campus);
	
	@DefaultMessage("{1} {0} ({2})")
	String sessionName(String year, String term, String campus);
	
	/* Course Requests Table messages
	 */
	@DefaultMessage("Validating...")
	String courseRequestsValidating();

	@DefaultMessage("Scheduling...")
	String courseRequestsScheduling();
	
	@DefaultMessage("Saving...")
	String courseRequestsSaving();

	@DefaultMessage("Loading...")
	String courseRequestsLoading();

	@DefaultMessage("Validation failed, see above for errors.")
	String validationFailed();

	@DefaultMessage("Course {0} used multiple times.")
	String validationMultiple(String course);
	
	@DefaultMessage("No course provided.")
	String validationNoCourse();
	
	@DefaultMessage("No alternative for a free time.")
	String validationFreeTimeWithAlt();
	
	@DefaultMessage("No free time alternative.")
	String validationAltFreeTime();
	
	@DefaultMessage("No first alternative provided.")
	String validationSecondAltWithoutFirst();
	
	@DefaultMessage("Course {0} does not exist.")
	String validationCourseNotExists(String course);

	@DefaultMessage("Course does not exist.")
	String validationUnknownCourseNotExists();
	
	@DefaultMessage("Courses")
	String courseRequestsCourses();

	@DefaultMessage("{0}. Priority")
	String courseRequestsPriority(int i);
	
	@DefaultMessage("Alternatives")
	String courseRequestsAlternatives();
	
	@DefaultMessage("{0}. Alternative")
	String courseRequestsAlternative(int i);
	
	@DefaultMessage("Alternative to {0}")
	String courseRequestsHintAlt(String course);
	
	@DefaultMessage("Alt. to {0} & {1}")
	String courseRequestsHintAlt2(String course, String altCourse);
	
	@DefaultMessage("Course with the second highest priority.")
	String courseRequestsHint1();
	
	@DefaultMessage("Enter a course name, e.g., ENG 10600")
	String courseRequestsHint3();

	@DefaultMessage("or a free time, e.g., Free MWF 7:30 - 8:30")
	String courseRequestsHint4();
	
	@DefaultMessage("Course with the lowest priority.")
	String courseRequestsHint8();
	
	@DefaultMessage("Alternative(s) to all the courses above.")
	String courseRequestsHintA0();
	
	/* Course Selection Box messages
	 */
	@DefaultMessage("Course Finder")
	String courseSelectionDialog();
	
	@DefaultMessage("No course selected.")
	String courseSelectionNoCourseSelected();
	
	@DefaultMessage("<u>D</u>etails")
	String courseSelectionDetails();
	
	@DefaultMessage("<u>L</u>ist of classes")
	String courseSelectionClasses();
	
	@DefaultMessage("<u>C</u>ourses")
	String courseSelectionCourses();
	
	@DefaultMessage("Free <u>T</u>ime")
	String courseSelectionFreeTime();
		
	@DefaultMessage("No course filter set.")
	String courseSelectionNoCourseFilter();
	
	@DefaultMessage("Looking for courses ...")
	String courseSelectionLoadingCourses();
	
	@DefaultMessage("Course {0} has no classes.")
	String courseSelectionNoClasses(String course);
	
	@DefaultMessage("Loading classes ...")
	String courseSelectionLoadingClasses();
	
	@DefaultMessage("Loading course details ...")
	String courseSelectionLoadingDetails();
	
	@DefaultMessage("Invalid free time.")
	String invalidFreeTime();
	
	@DefaultMessage("No free time entered.")
	String courseSelectionNoFreeTime();
	
	@DefaultMessage("Unable to interpret {0} as free time (error at position {1}).")
	String invalidFreeTimeGeneric(String text, int pos);
	
	@DefaultMessage("Unable to interpret {0} as free time (expected a day or a number at position {1}).")
	String invalidFreeTimeExpectedDayOrNumber(String text, int pos);
	
	@DefaultMessage("Unable to interpret {0} as free time (expected a number at position {1}).")
	String invalidFreeTimeExpectedNumber(String text, int pos);
	
	@DefaultMessage("Unable to interpret {0} as free time (start time before {1}).")
	String invalidFreeTimeStartBeforeFirst(String text, String first);
	
	@DefaultMessage("Unable to interpret {0} as free time (start time after {1}).")
	String invalidFreeTimeStartAfterLast(String text, String last);

	@DefaultMessage("Unable to interpret {0} as free time (end time before {1}).")
	String invalidFreeTimeEndBeforeFirst(String text, String first);
	
	@DefaultMessage("Unable to interpret {0} as free time (end time after {1}).")
	String invalidFreeTimeEndAfterLast(String text, String last);

	@DefaultMessage("Unable to interpret {0} as free time (start time is not before end time).")
	String invalidFreeTimeStartNotBeforeEnd(String text);
	
	@DefaultMessage("Unable to interpret {0} as free time (invalid start time).")
	String invalidFreeTimeInvalidStartTime(String text);

	@DefaultMessage("Unable to interpret {0} as free time (invalid end time).")
	String invalidFreeTimeInvalidEndTime(String text);

	/* Suggestion Box messages
	 */
	@DefaultMessage("Waiting for alternatives ...")
	String suggestionsLoading();
	
	@DefaultMessage("Alternatives for {0}")
	String suggestionsAlternatives(String source);

	@DefaultMessage("There are no alternatives for {0}.")
	String suggestionsNoAlternative(String source);

	@DefaultMessage("There are no alternatives for {0} matching {1}.")
	String suggestionsNoAlternativeWithFilter(String source, String filter);

	@DefaultMessage("No alternative for {0} is matching {1}.")
	String suggestionsNoMatch(String source, String filter);

	@DefaultMessage("Free Time {0} {1} - {2}")
	String freeTime(String days, String start, String end);

	@DefaultMessage("{0} {1}")
	String course(String subject, String course);

	@DefaultMessage("{0} {1} {2} {3}")
	String clazz(String subject, String course, String subpart, String section);
	
	/* Time Grid messages
	 */
	@DefaultMessage("Send {0} an email.")
	String sendEmail(String name);
	
	@DefaultMessage("(~{0} min)")
	String distanceConflict(int distanceInMinutes);
	
	/* Student Sectioning widget messags
	 */
	@DefaultMessage("<u>R</u>equests")
	String buttonRequests();
	
	@DefaultMessage("Re-schedule")
	String buttonReset();

	@DefaultMessage("<u>S</u>chedule")
	String buttonSchedule();
	
	@DefaultMessage("<u>E</u>nroll")
	String buttonEnroll();
	
	@DefaultMessage("<u>P</u>rint")
	String buttonPrint();
	
	@DefaultMessage("E<u>x</u>port")
	String buttonExport();

	@DefaultMessage("<u>S</u>ave")
	String buttonSave();

	@DefaultMessage("<u>L</u>ist of Classes")
	String tabClasses();
	
	@DefaultMessage("<u>T</u>imetable")
	String tabTimetable();
	
	@DefaultMessage("Requests stored.")
	String saveRequestsOK();
	
	@DefaultMessage("Unable to store requests: {0}")
	String saveRequestsFail(String reason);

	@DefaultMessage("Success!")
	String enrollOK();
	
	@DefaultMessage("Enrollment failed: {0}")
	String enrollFailed(String reason);
	
	@DefaultMessage("Student Schedule")
	String studentSchedule();

	@DefaultMessage("Preliminary Student Schedule")
	String studentScheduleNotEnrolled();

	@DefaultMessage("Free")
	String freeTimeSubject();

	@DefaultMessage("Time")
	String freeTimeCourse();
	
	@DefaultMessage("Computed schedule is empty.")
	String noSchedule();
	
	/* User Authentication messages
	 */
	@DefaultMessage("User: Not authenticated")
	String userNotAuthenticated();
	
	@DefaultMessage("Click here to authenticate.")
	String userHint();
	
	@DefaultMessage("Click here to log in.")
	String userHintLogin();

	@DefaultMessage("Click here to log out.")
	String userHintLogout();
	
	@DefaultMessage("You can close the window now.")
	String userHintClose();

	@DefaultMessage("Please Log In ...")
	String dialogAuthenticate();
	
	@DefaultMessage("Username:")
	String username();
	
	@DefaultMessage("Password:")
	String password();
	
	@DefaultMessage("Log In")
	String buttonUserLogin();
	
	@DefaultMessage("Guest")
	String buttonUserSkip();
	
	@DefaultMessage("Lookup")
	String buttonUserLookup();

	@DefaultMessage("Guest")
	String userGuest();
	
	@DefaultMessage("User: {0}")
	String userLabel(String user);
	
	/* Validation Error messages
	 */
	@DefaultMessage("Please wait ...")
	String pleaseWait();
	
	/* Web Table messages
	 */
	@DefaultMessage("No data.")
	String tableEmpty();
	
	/* Interface messages
	 */
	@DefaultMessage("Distance to travel from {0} is approx. {1} minutes.")
	String backToBackDistance(String rooms, int distanceInMinutes);
	
	@DefaultMessage("&infin;")
	String unlimited();
	
	@DefaultMessage("Not available.")
	String classNotAvailable();
	
	@DefaultMessage("Not assigned.")
	String courseNotAssigned();

	@DefaultMessage("Not Enrolled")
	String courseWaitListed();

	@DefaultMessage("Conflicts with {0}")
	String conflictWithFirst(String course);

	@DefaultMessage(", {0}")
	String conflictWithMiddle(String course);

	@DefaultMessage(" or {0}")
	String conflictWithLast(String course);
	
	@DefaultMessage(", assigned {0} instead")
	String conflictAssignedAlternative(String alt);
	
	@DefaultMessage("Failed to load the application ({0}).")
	String failedToLoadTheApp(String message);
	
	@DefaultMessage("Expected {0} students, but only {1} spaces are available, please try to avoid this class.")
	String highDemand(int expected, int available);

	@DefaultMessage("Course {0} is undergoing maintenance / changes.")
	String courseLocked(String course);
	
	@DefaultMessage("You are currently enrolled in {0}.")
	String saved(String clazz);

	@DefaultMessage("You are currently enrolled in {0}, this enrollment will get dropped.")
	String unassignment(String clazz);

	@DefaultMessage("You are currently not enrolled in {0}.")
	String assignment(String clazz);
	
	@DefaultMessage("Show unassignments")
	String showUnassignments();
	
	@DefaultMessage("Free time is not allowed.")
	String freeTimeNotAllowed();
	
	@DefaultMessage("{0} is a course.")
	String notFreeTimeIsCourse(String text);
	
	/* Enrollment table messages
	 */
	
	@DefaultMessage("Failed to load enrollments: {0}")
	String failedToLoadEnrollments(String message);

	@DefaultMessage("The selected offering has no students enrolled.")
	String offeringHasNoEnrollments();
	
	@DefaultMessage("The selected class has no students enrolled.")
	String classHasNoEnrollments();

	@DefaultMessage("Sort by {0}")
	String sortBy(String column);
	
	@DefaultMessage("Student")
	String colStudent();

	@DefaultMessage("Area")
	String colArea();

	@DefaultMessage("Clasf")
	String colClassification();

	@DefaultMessage("Major")
	String colMajor();

	@DefaultMessage("Requested")
	String colRequestTimeStamp();
	
	@DefaultMessage("Enrolled")
	String colEnrollmentTimeStamp();

	@DefaultMessage("Approved")
	String colApproved();

	@DefaultMessage("Priority")
	String colPriority();
	
	@DefaultMessage("Alternative")
	String colAlternative();

	@DefaultMessage("Reservation")
	String colReservation();

	@DefaultMessage("{0}.")
	String priority(int priority);
	
	@DefaultMessage("Total Enrolled: {0}")
	String totalEnrolled(int count);

	@DefaultMessage("Total Requested: {0}")
	String totalRequested(int count);

	@DefaultMessage("Total Not Enrolled: {0}")
	String totalWaitListed(int count);
	
	@DefaultMessage("{0} by {1}")
	String approval(String approvedDate, String approvedBy);
	
	@DefaultMessage("Select All")
	String selectAll();

	@DefaultMessage("Clear All")
	String clearAll();
	
	@DefaultMessage("Approve Selected Enrollments")
	String approveSelectedEnrollments();
	
	@DefaultMessage("<u>A</u>pprove")
	String buttonApproveSelectedEnrollments();
	
	@DefaultMessage("Failed to approve enrollments: {0}")
	String failedToApproveEnrollments(String error);

	@DefaultMessage("Reject Selected Enrollments")
	String rejectSelectedEnrollments();
	
	@DefaultMessage("<u>R</u>eject")
	String buttonRejectSelectedEnrollments();

	@DefaultMessage("Failed to reject enrollments: {0}")
	String failedToRejectEnrollments(String error);
	
	@DefaultMessage("Consent approved on {0}")
	String consentApproved(String approvedDate);
	
	@DefaultMessage("Waiting for {0}")
	String consentWaiting(String consent);

	@DefaultMessage("Group")
	String reservationGroup();

	@DefaultMessage("Individual")
	String reservationIndividual();

	@DefaultMessage("Course")
	String reservationCourse();

	@DefaultMessage("Curriculum")
	String reservationCurriculum();

	/* Enrollment dialog messages (opened from Enrollments table)
	 */
	
	@DefaultMessage("Loading classes for {0}...")
	String loadingEnrollment(String student);

	@DefaultMessage("Classes for {0}")
	String dialogEnrollments(String student);
	
	@DefaultMessage("Show External Ids")
	String showExternalIds();
	
	@DefaultMessage("Show Class Numbers")
	String showClassNumbers();
	
	@DefaultMessage("Export in iCalendar format.")
	String exportICalendar();
	
	@DefaultMessage("Enrollments")
	String enrollmentsTable();
	
	/* Sectioning exceptions
	 */
	@DefaultMessage("Course {0} does not exist.")
	String exceptionCourseDoesNotExist(String course);
	
	@DefaultMessage("Academic session {0} does not exist.")
	String exceptionSessionDoesNotExist(String session);
	
	@DefaultMessage("Academic session not selected.")
	String exceptionNoAcademicSession();
	
	@DefaultMessage("No suitable academic sessions found.")
	String exceptionNoSuitableAcademicSessions();
	
	@DefaultMessage("No classes found for {0}.")
	String exceptionNoClassesForCourse(String course);
	
	@DefaultMessage("Unable to compute a schedule ({0}).")
	String exceptionSectioningFailed(String message);
	
	@DefaultMessage("Too many bad attempts, login disabled.")
	String exceptionTooManyLoginAttempts();
	
	@DefaultMessage("User name not provided.")
	String exceptionLoginNoUsername();
	
	@DefaultMessage("Wrong username and/or password.")
	String exceptionLoginFailed();
	
	@DefaultMessage("Login failed ({0}).")
	String exceptionLoginFailedUnknown(String message);
	
	@DefaultMessage("User is not logged in.")
	String exceptionUserNotLoggedIn();

	@DefaultMessage("Unable to load section information ({0}).")
	String exceptionCustomSectionNamesFailed(String reason);
	
	@DefaultMessage("Unable to retrive course details ({0}).")
	String exceptionCustomCourseDetailsFailed(String reason);
	
	@DefaultMessage("Unable to retrive class details ({0}).")
	String exceptionCustomSectionLimitsFailed(String reason);
	
	@DefaultMessage("Course detail interface not provided.")
	String exceptionNoCustomCourseDetails();
	
	@DefaultMessage("Last academic session failed ({0}).")
	String exceptionLastAcademicSessionFailed(String message);
	
	@DefaultMessage("Not a student.")
	String exceptionNoStudent();
	
	@DefaultMessage("Wrong student id.")
	String exceptionBadStudentId();
	
	@DefaultMessage("No requests stored for the student.")
	String exceptionNoRequests();
	
	@DefaultMessage("Online student scheduling is not available for this academic session.")
	String exceptionBadSession();
	
	@DefaultMessage("Your are not authenticated, please log in first.")
	String exceptionEnrollNotAuthenticated();
	
	@DefaultMessage("Your are not registered as a student in {0}.")
	String exceptionEnrollNotStudent(String session);
	
	@DefaultMessage("Unable to enroll into {0}, the class is no longer available.")
	String exceptionEnrollNotAvailable(String clazz);
	
	@DefaultMessage("This feature is not supported in the current environment.")
	String exceptionNotSupportedFeature();
	
	@DefaultMessage("No schedule stored for the student.")
	String exceptionNoSchedule();
	
	@DefaultMessage("No courses provided.")
	String exceptionNoCourse();
	
	@DefaultMessage("Unable to compute a schedule (no solution found).")
	String exceptionNoSolution();

	@DefaultMessage("{0}")
	String exceptionUnknown(String reason);
	
	@DefaultMessage("Academic session is not available for student scheduling.")
	String exceptionNoServerForSession();

	@DefaultMessage("Wrong class or instructional offering.")
	String exceptionBadClassOrOffering();
	
	@DefaultMessage("Wrong instructional offering.")
	String exceptionBadOffering();
	
	@DefaultMessage("Wrong course offering.")
	String exceptionBadCourse();

	@DefaultMessage("Insufficient user privileges.")
	String exceptionInsufficientPrivileges();
	
	@DefaultMessage("Your timetabling session has expired. Please log in again.")
	String exceptionHttpSessionExpired();
	
	@DefaultMessage("Login is required to use this page.")
	String exceptionLoginRequired();
	
	@DefaultMessage("Filter assignments of the selected class by name, day, start time, date, room or instructor." +
			"<br><br>You can also use the following tags:" +
			"<ul>" +
			"<li><i>name:</i> class name" + 
			"<li><i>day:</i> class must meet on this day or days (e.g., monday, MWF)" + 
			"<li><i>time:</i> class must start at this time (e.g., 730)" +
			"<li><i>before:</i> class must end before or by this time" +
			"<li><i>after:</i> class must start on or after this time" +
			"<li><i>date:</i> class must meet on this date" +
			"<li><i>room:</i> class must use this room or building" +
			"<li><i>instructor:</i> class must have this instructor" +
			"</ul>Use <i>or</i>, <i>and</i>, <i>not</i>, and brackets to build a boolean query." +
			"<br><br>Example: day: monday and (time: 730 or time: 830)")
	String suggestionsFilterHint();
	
	@DefaultMessage("<u>S</u>earch")
	String buttonSearch();
	
	@DefaultMessage("Overlaps with {0}")
	String noteAllowedOverlapFirst(String classOrCourse);
	
	@DefaultMessage(", {0}")
	String noteAllowedOverlapMiddle(String classOrCourse);

	@DefaultMessage(" and {0}")
	String noteAllowedOverlapLast(String classOrCourse);
	
	@DefaultMessage("Filter:")
	String filter();
	
	@DefaultMessage("Loading data...")
	String loadingData();
	
	@DefaultMessage("Available")
	String colAvailable();
	
	@DefaultMessage("Projection")
	String colProjection();
	
	@DefaultMessage("Enrollment")
	String colEnrollment();
	
	@DefaultMessage("Wait-Listed")
	String colWaitListed();
	
	@DefaultMessage("Reservation")
	String colReserved();
	
	@DefaultMessage("Consent")
	String colConsent();

	@DefaultMessage("Enrollments of {0}")
	String titleEnrollments(String courseOrClass);
	
	@DefaultMessage("Total")
	String total();
	
	@DefaultMessage("Limit not defined.")
	String availableNoLimit();
	
	@DefaultMessage("Unlimited, reservation required")
	String availableUnlimitedWithReservation();
	
	@DefaultMessage("Unlimited, reservation not needed")
	String availableUnlimited();
	
	@DefaultMessage("No space available, limit of {0} was spaces reached")
	String availableNot(int limit);
	
	@DefaultMessage("Available {0} out of {1} spaces, reservation not needed")
	String available(int available, int limit);
	
	@DefaultMessage("Available {0} out of {1} spaces, reservation required")
	String availableWithReservation(int available, int limit);
	
	@DefaultMessage("Available {0} out of {1} spaces, reservation required for {2} of them")
	String availableSomeReservation(int available, int limit, int availableWithReservation);
	
	@DefaultMessage("<sup><font color='#9CB0CE'>r)</font></sup>")
	String htmlReservationSign();
	
	@DefaultMessage("No results matching filter {0} found.")
	String exceptionNoMatchingResultsFound(String filter);
	
	@DefaultMessage("Filter course, enrollments, and wait-listed course requests by any word<br>from the course name or title." +
			"<br><br>You can also use the following tags:" +
			"<ul>" +
			"<li><i>area:</i> academic area abbreviation" + 
			"<li><i>classification:</i> academic classification code" +
			"<li><i>consent:</i> offering consent" +
			"<li><i>course:</i> course offering name" +
			"<li><i>department:</i> course controling department code or abbreviation" +
			"<li><i>group:</i> student group abbreviation" +
			"<li><i>major:</i> academic major code" +
			"<li><i>reserved:</i> enrollments with a reservation" +
			"<li><i>student:</i> student name or external id" +
			"<li><i>subject:</i> subject area abbreviation" +
			"<li><i>waitlist:</i> wait-listed course requests" +
			"</ul>Use <i>or</i>, <i>and</i>, <i>not</i>, and brackets to build a boolean query." +
			"<br><br>Example: subject:AAE and (waitlist:true or consent:waiting)")
	String sectioningStatusFilterHint();
	
	@DefaultMessage("Loading enrollments for {0}...")
	String loadingEnrollments(String classOrCourse);
	
	@DefaultMessage("Scheduling <u>A</u>ssistant")
	String buttonAssistant();

	@DefaultMessage("Student Scheduling Assistant for {0}")
	String dialogAssistant(String student);
	
	@DefaultMessage("Close")
	String buttonClose();
	
	@DefaultMessage("Loading scheduling assistant for {0}...")
	String loadingAssistant(String student);
		
	@DefaultMessage("Need<br>Consent")
	String colNeedConsent();
	
	@DefaultMessage("<u>E</u>nrollments")
	String tabEnrollments();

	@DefaultMessage("<u>S</u>tudents")
	String tabStudents();
	
	@DefaultMessage("<sup><font color='#9CB0CE'>({0}p)</font></sup>")
	String firstWaitListedPrioritySign(int priority);
	
	@DefaultMessage("r) Space available only with a reservation.")
	String sectioningStatusReservationHint();
	
	@DefaultMessage("(p) denotes priority of the first wait-listed course request.")
	String sectioningStatusPriorityHint();
}