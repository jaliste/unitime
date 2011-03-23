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
package org.unitime.timetable.gwt.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection.BranchBoundNeighbour;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;
import net.sf.cpsolver.studentsct.reservation.CourseReservation;
import net.sf.cpsolver.studentsct.reservation.Reservation;

import org.apache.log4j.Logger;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.server.custom.CustomSectionNames;
import org.unitime.timetable.gwt.server.custom.SectionLimitProvider;
import org.unitime.timetable.gwt.server.custom.SectionUrlProvider;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.gwt.shared.SectioningExceptionType;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.StudentSectioningQueue;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentDAO;

/**
 * @author Tomas Muller
 */
public class SectioningServer {
	private static Logger sLog = Logger.getLogger(SectioningServer.class);
	private static Hashtable<Long, SectioningServer> sInstances = new Hashtable<Long, SectioningServer>();
	
    public static CustomSectionNames sCustomSectionNames = null;
    public static SectionLimitProvider sSectionLimitProvider = null;
    public static SectionUrlProvider sSectionUrlProvider = null;
    public static boolean sUpdateLimitsUsingSectionLimitProvider = false;
	
	private AcademicSessionInfo iAcademicSession = null;
	private Hashtable<Long, CourseInfo> iCourseForId = new Hashtable<Long, CourseInfo>();
	private Hashtable<String, TreeSet<CourseInfo>> iCourseForName = new Hashtable<String, TreeSet<CourseInfo>>();
	private TreeSet<CourseInfo> iCourses = new TreeSet<CourseInfo>();
	private StudentSectioningModel iModel = null;
	private DistanceMetric iDistanceMetric = null;
	
	private Hashtable<Long, Course> iCourseTable = new Hashtable<Long, Course>();
	private Hashtable<Long, Section> iClassTable = new Hashtable<Long, Section>();
	private Hashtable<Long, Student> iStudentTable = new Hashtable<Long, Student>();
	
	private CourseLoader iLoader = null;
	private SectioningServerUpdater iUpdater = null;
	private ReentrantReadWriteLock iLock = new ReentrantReadWriteLock();
	private static ReentrantReadWriteLock sGlobalLock = new ReentrantReadWriteLock();
	
	private Hashtable<Long, int[]> iLastSectionLimit = new Hashtable<Long, int[]>();
	
	public static void init() {
        if (ApplicationProperties.getProperty("unitime.custom.CourseSectionNames") != null) {
        	try {
        		sCustomSectionNames = (CustomSectionNames)Class.forName(ApplicationProperties.getProperty("unitime.custom.CourseSectionNames")).newInstance();
        	} catch (Exception e) {
        		sLog.fatal("Unable to initialize custom section names, reason: "+e.getMessage(), e);
        	}
        }
        if (ApplicationProperties.getProperty("unitime.custom.SectionLimitProvider") != null) {
        	try {
        		sSectionLimitProvider = (SectionLimitProvider)Class.forName(ApplicationProperties.getProperty("unitime.custom.SectionLimitProvider")).newInstance();
        	} catch (Exception e) {
        		sLog.fatal("Unable to initialize section limit provider, reason: "+e.getMessage(), e);
        	}
        }
        if (ApplicationProperties.getProperty("unitime.custom.SectionUrlProvider") != null) {
        	try {
        		sSectionUrlProvider = (SectionUrlProvider)Class.forName(ApplicationProperties.getProperty("unitime.custom.SectionUrlProvider")).newInstance();
        	} catch (Exception e) {
        		sLog.fatal("Unable to initialize section URL provider, reason: "+e.getMessage(), e);
        	}
        }
        sUpdateLimitsUsingSectionLimitProvider = "true".equalsIgnoreCase(ApplicationProperties.getProperty("unitime.custom.SectionLimitProvider.updateLimits", "false"));
	}
	
	public static boolean isEnabled() {
		// if autostart is enabled, just check whether there are some instances already loaded in
		if ("true".equals(ApplicationProperties.getProperty("unitime.enrollment.autostart", "false")))
			return !sInstances.isEmpty();
		
		// quick check for existing instances
		if (!sInstances.isEmpty()) return true;
		
		// otherwise, look for a session that has sectioning enabled
		String year = ApplicationProperties.getProperty("unitime.enrollment.year");
		String term = ApplicationProperties.getProperty("unitime.enrollment.term");
		for (Iterator<Session> i = SessionDAO.getInstance().findAll().iterator(); i.hasNext(); ) {
			final Session session = i.next();
			
			if (year != null && !year.equals(session.getAcademicYear())) continue;
			if (term != null && !term.equals(session.getAcademicTerm())) continue;

			if (!session.getStatusType().canSectioningStudents()) continue;

			return true;
		}
		return false;
	}
	
	public static boolean isRegistrationEnabled() {
		for (Session session: SessionDAO.getInstance().findAll()) {
			if (!session.getStatusType().canSectioningStudents() && session.getStatusType().canPreRegisterStudents()) return true;
		}
		return false;
	}
	
	private SectioningServer(Long sessionId) throws SectioningException {
		org.hibernate.Session hibSession = SessionDAO.getInstance().createNewSession();
		try {
			Session session = SessionDAO.getInstance().get(sessionId, hibSession);
			if (session == null)
				throw new SectioningException(SectioningExceptionType.SESSION_NOT_EXIST, (sessionId == null ? "null" : sessionId.toString()));
			DataProperties config = new DataProperties(ApplicationProperties.getProperties());
			config.setProperty("Reservation.CanAssignOverTheLimit", "true");
			config.setProperty("Sectioning.SectionLimit", "false");
			config.setProperty("Sectioning.ConfigLimit", "false");
			config.setProperty("Sectioning.CourseLimit", "false");
			config.setProperty("Sectioning.ReservationLimit", "false");
			config.setProperty("Sectioning.StudentConflict", "false");
			iModel = new StudentSectioningModel(config);
			iAcademicSession = new AcademicSessionInfo(session);
			iLoader = new CourseLoader(iModel, iAcademicSession, iCourseTable, iClassTable, iStudentTable, iCourseForId, iCourseForName, iCourses);
			iUpdater = new SectioningServerUpdater(iAcademicSession, StudentSectioningQueue.getLastTimeStamp(hibSession, sessionId));
			iLoader.updateAll(hibSession);
			iUpdater.start();
		} catch (Throwable t) {
			if (t instanceof SectioningException) throw (SectioningException)t;
			throw new SectioningException(SectioningExceptionType.UNKNOWN, t);
		} finally {
			hibSession.close();
		}
		iDistanceMetric = new DistanceMetric(
				DistanceMetric.Ellipsoid.valueOf(ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name())));
	}
	
	public StudentSectioningModel getModel() { return iModel; }
	
	public String getSectionName(Long courseId, Section section) {
		if (sCustomSectionNames != null) {
			String name = sCustomSectionNames.getClassSuffix(getAcademicSessionId(), courseId, section.getId());
			if (name != null) return name;
		}
		return section.getName();
	}
	
	public Long getAcademicSessionId() { return iAcademicSession.getUniqueId(); }
	
	public AcademicSessionInfo getAcademicSession() { return iAcademicSession; }

	public CourseInfo getCourseInfo(String course) {
		iLock.readLock().lock();
		try {
			if (course.indexOf('-') >= 0) {
				String courseName = course.substring(0, course.indexOf('-')).trim();
				String title = course.substring(course.indexOf('-') + 1).trim();
				TreeSet<CourseInfo> infos = iCourseForName.get(courseName.toLowerCase());
				if (infos!= null && !infos.isEmpty())
					for (CourseInfo info: infos)
						if (title.equalsIgnoreCase(info.getTitle())) return info;
				return null;
			} else {
				TreeSet<CourseInfo> infos = iCourseForName.get(course.toLowerCase());
				if (infos!= null && !infos.isEmpty()) return infos.first();
				return null;
			}
		} finally {
			iLock.readLock().unlock();
		}
	}

	public CourseInfo getCourseInfo(Long courseId) {
		iLock.readLock().lock();
		try {
			return iCourseForId.get(courseId);
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	public Student getStudent(Long studentId) {
		iLock.readLock().lock();
		try {
			return iStudentTable.get(studentId);
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void reloadStudent(org.unitime.timetable.model.Student s) {
		iLock.writeLock().lock();
		try {
			Student student = iStudentTable.get(s.getUniqueId());
			if (student != null) {
				for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext();) {
					Request r = (Request)e.next();
					if (r.getAssignment() != null) r.unassign(0);
				}
				iModel.removeStudent(student);
			}
			student = iLoader.loadStudent(s);
			iModel.addStudent(student);
			iLoader.assignStudent(student, s, false);
		} finally {
			iLock.writeLock().unlock();
		}
	}

	public Course getCourse(Long courseId) {
		iLock.readLock().lock();
		try {
			return iCourseTable.get(courseId);
		} finally {
			iLock.readLock().unlock();
		}
	}

	public CourseInfo getCourseInfo(String subject, String courseNbr) {
		return getCourseInfo(subject + " " + courseNbr);
	}

	public Collection<CourseInfo> findCourses(String query, Integer limit) {
		iLock.readLock().lock();
		try {
			List<CourseInfo> ret = new ArrayList<CourseInfo>(limit == null ? 100 : limit);
			String queryInLowerCase = query.toLowerCase();
			for (CourseInfo c : iCourses) {
				if (c.matchCourseName(queryInLowerCase)) ret.add(c);
				if (limit != null && ret.size() == limit) return ret;
			}
			if (queryInLowerCase.length() > 2) {
				for (CourseInfo c : iCourses) {
					if (c.matchTitle(queryInLowerCase)) ret.add(c);
					if (limit != null && ret.size() == limit) return ret;
				}
			}
			return ret;
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	public static void createInstance(Long academicSessionId) {
		sGlobalLock.writeLock().lock();
		try {
			SectioningServer s = new SectioningServer(academicSessionId);
			sInstances.put(academicSessionId, s);
			if (SectioningServer.sCustomSectionNames != null)
				SectioningServer.sCustomSectionNames.update(s.getAcademicSession());
		} finally {
			sGlobalLock.writeLock().unlock();
		}
	}
	
	public static SectioningServer getInstance(final Long academicSessionId) throws SectioningException {
		sGlobalLock.readLock().lock();
		try {
			return sInstances.get(academicSessionId);
		} finally {
			sGlobalLock.readLock().unlock();
		}
	}
	
	public static TreeSet<AcademicSessionInfo> getAcademicSessions() {
		sGlobalLock.readLock().lock();
		try {
			TreeSet<AcademicSessionInfo> ret = new TreeSet<AcademicSessionInfo>();
			for (SectioningServer s : sInstances.values())
				ret.add(s.getAcademicSession());
			return ret;
		} finally {
			sGlobalLock.readLock().unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	private Course clone(Course course, long studentId, boolean updateFromCache) {
		Offering clonedOffering = new Offering(course.getOffering().getId(), course.getOffering().getName());
		int courseLimit = course.getLimit();
		if (courseLimit >= 0) {
			courseLimit -= course.getEnrollments().size();
			for (Iterator<Enrollment> i = course.getEnrollments().iterator(); i.hasNext();) {
				Enrollment enrollment = i.next();
				if (enrollment.getStudent().getId() == studentId) { courseLimit++; break; }
			}
			if (courseLimit < 0) courseLimit = 0;
		}
		Course clonedCourse = new Course(course.getId(), course.getSubjectArea(), course.getCourseNumber(), clonedOffering, courseLimit, course.getProjected());
		Hashtable<Config, Config> configs = new Hashtable<Config, Config>();
		Hashtable<Subpart, Subpart> subparts = new Hashtable<Subpart, Subpart>();
		Hashtable<Section, Section> sections = new Hashtable<Section, Section>();
		for (Iterator<Config> e = course.getOffering().getConfigs().iterator(); e.hasNext();) {
			Config config = e.next();
			int configLimit = config.getLimit();
			if (configLimit >= 0) {
				configLimit -= config.getEnrollments().size();
				for (Iterator<Enrollment> i = config.getEnrollments().iterator(); i.hasNext();) {
					Enrollment enrollment = i.next();
					if (enrollment.getStudent().getId() == studentId) { configLimit++; break; }
				}
				if (configLimit < 0) configLimit = 0;
			}
			Config clonedConfig = new Config(config.getId(), configLimit, config.getName(), clonedOffering);
			configs.put(config, clonedConfig);
			for (Iterator<Subpart> f = config.getSubparts().iterator(); f.hasNext();) {
				Subpart subpart = f.next();
				Subpart clonedSubpart = new Subpart(subpart.getId(), subpart.getInstructionalType(), subpart.getName(), clonedConfig,
						(subpart.getParent() == null ? null: subparts.get(subpart.getParent())));
				clonedSubpart.setAllowOverlap(subpart.isAllowOverlap());
				subparts.put(subpart, clonedSubpart);
				for (Iterator<Section> g = subpart.getSections().iterator(); g.hasNext();) {
					Section section = g.next();
					int limit = section.getLimit();
					if (limit >= 0) {
						// limited section, deduct enrollments
						limit -= section.getEnrollments().size();
						for (Iterator<Enrollment> i = section.getEnrollments().iterator(); i.hasNext();) {
							Enrollment enrollment = i.next();
							if (enrollment.getStudent().getId() == studentId) { limit++; break; }
						}
						if (limit < 0) limit = 0; // over-enrolled, but not unlimited
					}
					Section clonedSection = new Section(section.getId(), limit,
							section.getName(), clonedSubpart, section.getPlacement(),
							section.getChoice().getInstructorIds(), section.getChoice().getInstructorNames(),
							(section.getParent() == null ? null : sections.get(section.getParent())));
					clonedSection.setSpaceExpected(section.getSpaceExpected());
					clonedSection.setSpaceHeld(section.getSpaceHeld());
					clonedSection.setPenalty(section.getOnlineSectioningPenalty());
					sections.put(section, clonedSection);
				}
			}
		}
		if (course.getOffering().hasReservations()) {
			Student student = getStudent(studentId);
			for (Reservation reservation: course.getOffering().getReservations()) {
				int reservationLimit = (int)Math.round(reservation.getLimit());
				if (reservationLimit >= 0) {
					reservationLimit -= reservation.getEnrollments().size();
					for (Iterator<Enrollment> i = reservation.getEnrollments().iterator(); i.hasNext();) {
						Enrollment enrollment = i.next();
						if (enrollment.getStudent().getId() == studentId) { reservationLimit++; break; }
					}
					if (reservationLimit <= 0) continue;
				}
				boolean applicable = student != null && reservation.isApplicable(student);
				if (reservation instanceof CourseReservation)
					applicable = (course.getId() == ((CourseReservation)reservation).getCourse().getId());
				Reservation clonedReservation = new DummyReservation(reservation.getId(), clonedOffering,
						reservation.getPriority(), reservation.canAssignOverLimit(), reservationLimit, 
						applicable);
				for (Config config: reservation.getConfigs())
					clonedReservation.addConfig(configs.get(config));
				for (Map.Entry<Subpart, Set<Section>> entry: reservation.getSections().entrySet()) {
					Set<Section> clonedSections = new HashSet<Section>();
					for (Section section: entry.getValue())
						clonedSections.add(sections.get(section));
					clonedReservation.getSections().put(
							subparts.get(entry.getKey()),
							clonedSections);
				}
			}
		}
		if (sUpdateLimitsUsingSectionLimitProvider) updateLimits(clonedCourse, updateFromCache);
		return clonedCourse;
	}
	
	@SuppressWarnings("unchecked")
	private void updateLimits(Course course, boolean updateFromCache) {
		if (sSectionLimitProvider == null) return;
		ArrayList<Long> classIds = new ArrayList<Long>();
		final Hashtable<Long, String> classNames = new Hashtable<Long, String>();
		Hashtable<Long, Section> classes = new Hashtable<Long, Section>();
		for (Iterator<Config> e = course.getOffering().getConfigs().iterator(); e.hasNext();) {
			Config config = e.next();
			for (Iterator<Subpart> f = config.getSubparts().iterator(); f.hasNext();) {
				Subpart subpart = f.next();
				for (Iterator<Section> g = subpart.getSections().iterator(); g.hasNext();) {
					Section section = g.next();
					classIds.add(section.getId());
					classNames.put(section.getId(), section.getName());
					classes.put(section.getId(), section);
				}
			}
		}
		CustomSectionNames x = new CustomSectionNames() {
			public void update(AcademicSessionInfo session) {
			}

			public String getClassSuffix(Long sessionId, Long courseId, Long classId) {
				if (sCustomSectionNames != null) {
					String ret = sCustomSectionNames.getClassSuffix(sessionId, courseId, classId);
					if (ret != null) return ret;
				}
				return classNames.get(classId);
			}
		};
		Hashtable<Long, int[]> limits = (updateFromCache ? 
				SectioningServer.sSectionLimitProvider.getSectionLimitsFromCache(getAcademicSession(), course.getId(), classIds, x) :
				SectioningServer.sSectionLimitProvider.getSectionLimits(getAcademicSession(), course.getId(), classIds, x));
		for (Map.Entry<Long, int[]> entry: limits.entrySet()) {
			classes.get(entry.getKey()).setLimit(Math.max(0 , entry.getValue()[1] - entry.getValue()[0]));
			iLastSectionLimit.put(entry.getKey(), entry.getValue());
		}
	}
	
	public URL getSectionUrl(Long courseId, Section section) throws SectioningException {
		if (SectioningServer.sSectionUrlProvider == null) return null;
		return SectioningServer.sSectionUrlProvider.getSectionUrl(getAcademicSession(), courseId, section.getId(),
				getSectionName(courseId, section));
	}
	
	@SuppressWarnings("unchecked")
	public int[] getLimit(Section section, Long studentId) {
		if (sUpdateLimitsUsingSectionLimitProvider) {
			int[] limit = iLastSectionLimit.get(section.getId());
			if (limit != null) return limit;
		}
		Section original = iClassTable.get(section.getId());
		int actual = original.getEnrollments().size();
		if (studentId != null) {
			for (Iterator<Enrollment> i = original.getEnrollments().iterator(); i.hasNext();) {
				Enrollment enrollment = i.next();
				if (enrollment.getStudent().getId() == studentId) { actual--; break; }
			}
		}
		return new int[] {actual, original.getLimit()};
	}
	
	private void addRequest(StudentSectioningModel model, Student student, CourseRequestInterface.Request request, boolean alternative, boolean updateFromCache) {
		iLock.readLock().lock();
		try {
			if (request.hasRequestedFreeTime()) {
				for (CourseRequestInterface.FreeTime freeTime: request.getRequestedFreeTime()) {
					int dayCode = 0;
					for (DayCode d: DayCode.values()) {
						if (freeTime.getDays().contains(d.getIndex()))
							dayCode |= d.getCode();
					}
					TimeLocation freeTimeLoc = new TimeLocation(dayCode, freeTime.getStart(), freeTime.getLength(), 0, 0, 
							-1l, "", iAcademicSession.getFreeTimePattern(), 0);
					new FreeTimeRequest(student.getRequests().size() + 1, student.getRequests().size(), alternative, student, freeTimeLoc);
				}
			} else if (request.hasRequestedCourse()) {
				CourseInfo courseInfo = getCourseInfo(request.getRequestedCourse());
				Course course = null;
				if (courseInfo != null) course = iCourseTable.get(courseInfo.getUniqueId());
				if (course != null) {
					Vector<Course> cr = new Vector<Course>();
					cr.add(clone(course, student.getId(), updateFromCache));
					if (request.hasFirstAlternative()) {
						CourseInfo ci = getCourseInfo(request.getFirstAlternative());
						if (ci != null) {
							Course x = iCourseTable.get(ci.getUniqueId());
							if (x != null) cr.add(clone(x, student.getId(), updateFromCache));
						}
					}
					if (request.hasSecondAlternative()) {
						CourseInfo ci = getCourseInfo(request.getSecondAlternative());
						if (ci != null) {
							Course x = iCourseTable.get(ci.getUniqueId());
							if (x != null) cr.add(clone(x, student.getId(), updateFromCache));
						}
					}
					new CourseRequest(student.getRequests().size() + 1, student.getRequests().size(), alternative, student, cr, false, null);
				}
			}
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	public Collection<String> checkCourses(CourseRequestInterface req) throws SectioningException {
		ArrayList<String> notFound = new ArrayList<String>();
		for (CourseRequestInterface.Request cr: req.getCourses()) {
			if (!cr.hasRequestedFreeTime() && cr.hasRequestedCourse() && getCourseInfo(cr.getRequestedCourse()) == null)
				notFound.add(cr.getRequestedCourse());
			if (cr.hasFirstAlternative() && getCourseInfo(cr.getFirstAlternative()) == null)
				notFound.add(cr.getFirstAlternative());
			if (cr.hasSecondAlternative() && getCourseInfo(cr.getSecondAlternative()) == null)
				notFound.add(cr.getSecondAlternative());
		}
		for (CourseRequestInterface.Request cr: req.getAlternatives()) {
			if (cr.hasRequestedCourse() && getCourseInfo(cr.getRequestedCourse()) == null)
				notFound.add(cr.getRequestedCourse());
			if (cr.hasFirstAlternative() && getCourseInfo(cr.getFirstAlternative()) == null)
				notFound.add(cr.getFirstAlternative());
			if (cr.hasSecondAlternative() && getCourseInfo(cr.getSecondAlternative()) == null)
				notFound.add(cr.getSecondAlternative());
		}
		return notFound;
	}
	
	@SuppressWarnings("unchecked")
	public Set<Long> getSavedClasses(Long studentId) {
		iLock.readLock().lock();
		try {
			if (studentId == null) return null;
			Student student = (Student)iStudentTable.get(studentId);
			if (student == null) return null;
			Set<Long> ret = new HashSet<Long>();
			for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext(); ) {
				Request r = (Request)e.next();
				if (!(r instanceof CourseRequest) || r.getInitialAssignment() == null) continue;
				for (Iterator<Section> i = ((Enrollment)r.getInitialAssignment()).getSections().iterator(); i.hasNext();)
					ret.add(i.next().getId());
			}
			return ret;
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	private ClassAssignmentInterface convert(Enrollment[] enrollments,
			Hashtable<CourseRequest, Set<Section>> requiredSectionsForCourse, HashSet<FreeTimeRequest> requiredFreeTimes,
			boolean computeOverlaps,
			DistanceConflict dc, Set<Long> savedClasses) throws SectioningException {
        ClassAssignmentInterface ret = new ClassAssignmentInterface();
		int nrUnassignedCourses = 0;
		int nrAssignedAlt = 0;
		for (Enrollment enrollment: enrollments) {
			if (enrollment == null) continue;
			if (enrollment.getRequest().isAlternative() && nrAssignedAlt >= nrUnassignedCourses &&
				(enrollment.getAssignments() == null || enrollment.getAssignments().isEmpty())) continue;
			if (enrollment.getAssignments() == null || enrollment.getAssignments().isEmpty()) {
				ClassAssignmentInterface.CourseAssignment ca = new ClassAssignmentInterface.CourseAssignment();
				if (enrollment.getRequest() instanceof CourseRequest) {
					CourseRequest r = (CourseRequest)enrollment.getRequest();
					Course course = enrollment.getCourse();
					ca.setAssigned(false);
					ca.setCourseId(course.getId());
					ca.setSubject(course.getSubjectArea());
					ca.setCourseNbr(course.getCourseNumber());
					if (computeOverlaps) {
						TreeSet<Enrollment> overlap = new TreeSet<Enrollment>(new Comparator<Enrollment>() {
							public int compare(Enrollment e1, Enrollment e2) {
								return e1.getRequest().compareTo(e2.getRequest());
							}
						});
						Hashtable<CourseRequest, TreeSet<Section>> overlapingSections = new Hashtable<CourseRequest, TreeSet<Section>>();
						Collection<Enrollment> avEnrls = r.getAvaiableEnrollmentsSkipSameTime();
						for (Iterator<Enrollment> e = avEnrls.iterator(); e.hasNext();) {
							Enrollment enrl = e.next();
							overlaps: for (Enrollment x: enrollments) {
								if (x == null || x.getAssignments() == null || x.getAssignments().isEmpty()) continue;
								if (x == enrollment) continue;
						        for (Iterator<Assignment> i = x.getAssignments().iterator(); i.hasNext();) {
						        	Assignment a = i.next();
									if (a.isOverlapping(enrl.getAssignments())) {
										overlap.add(x);
										if (x.getRequest() instanceof CourseRequest) {
											CourseRequest cr = (CourseRequest)x.getRequest();
											TreeSet<Section> ss = overlapingSections.get(cr);
											if (ss == null) { ss = new TreeSet<Section>(); overlapingSections.put(cr, ss); }
											ss.add((Section)a);
										}
										break overlaps;
									}
						        }
							}
						}
						for (Enrollment q: overlap) {
							if (q.getRequest() instanceof FreeTimeRequest) {
								FreeTimeRequest f = (FreeTimeRequest)q.getRequest();
								ca.addOverlap("Free Time " +
									DayCode.toString(f.getTime().getDayCode()) + " " + 
									f.getTime().getStartTimeHeader() + " - " +
									f.getTime().getEndTimeHeader());
							} else {
								CourseRequest cr = (CourseRequest)q.getRequest();
								Course o = q.getCourse();
								String ov = o.getSubjectArea() + " " + o.getCourseNumber();
								if (overlapingSections.get(cr).size() == 1)
									for (Iterator<Section> i = overlapingSections.get(cr).iterator(); i.hasNext();) {
										Section s = i.next();
										ov += " " + s.getSubpart().getName();
										if (i.hasNext()) ov += ",";
									}
								ca.addOverlap(ov);
							}
						}
						nrUnassignedCourses++;
						int alt = nrUnassignedCourses;
						for (Enrollment x: enrollments) {
							if (x == null || x.getAssignments() == null || x.getAssignments().isEmpty()) continue;
							if (x == enrollment) continue;
							if (x.getRequest().isAlternative() && x.getRequest() instanceof CourseRequest) {
								if (--alt == 0) {
									Course o = x.getCourse();
									ca.setInstead(o.getSubjectArea() + " " +o.getCourseNumber());
									break;
								}
							}
						}
						if (avEnrls.isEmpty()) ca.setNotAvailable(true);
					}
					ret.add(ca);
				} else {
					FreeTimeRequest r = (FreeTimeRequest)enrollment.getRequest();
					ca.setAssigned(false);
					ca.setCourseId(null);
					if (computeOverlaps) {
						overlaps: for (Enrollment x: enrollments) {
							if (x == null || x.getAssignments() == null || x.getAssignments().isEmpty()) continue;
							if (x == enrollment) continue;
					        for (Iterator<Assignment> i = x.getAssignments().iterator(); i.hasNext();) {
					        	Assignment a = i.next();
								if (r.isOverlapping(a)) {
									if (x.getRequest() instanceof FreeTimeRequest) {
										FreeTimeRequest f = (FreeTimeRequest)x.getRequest();
										ca.addOverlap("Free Time " +
											DayCode.toString(f.getTime().getDayCode()) + " " + 
											f.getTime().getStartTimeHeader() + " - " +
											f.getTime().getEndTimeHeader());
									} else {
										Course o = x.getCourse();
										Section s = (Section)a;
										ca.addOverlap(o.getSubjectArea() + " " + o.getCourseNumber() + " " + s.getSubpart().getName());
									}
									break overlaps;
								}
					        }
						}
					}
					ClassAssignmentInterface.ClassAssignment a = ca.addClassAssignment();
					a.setAlternative(r.isAlternative());
					for (DayCode d : DayCode.toDayCodes(r.getTime().getDayCode()))
						a.addDay(d.getIndex());
					a.setStart(r.getTime().getStartSlot());
					a.setLength(r.getTime().getLength());
					ret.add(ca);
				}
			} else if (enrollment.getRequest() instanceof CourseRequest) {
				CourseRequest r = (CourseRequest)enrollment.getRequest();
				Set<Section> requiredSections = null;
				if (requiredSectionsForCourse != null) requiredSections = requiredSectionsForCourse.get(r);
				if (r.isAlternative() && r.isAssigned()) nrAssignedAlt++;
				TreeSet<Section> sections = new TreeSet<Section>(new EnrollmentSectionComparator());
				sections.addAll(enrollment.getSections());
				Course course = enrollment.getCourse();
				ClassAssignmentInterface.CourseAssignment ca = new ClassAssignmentInterface.CourseAssignment();
				ca.setAssigned(true);
				ca.setCourseId(course.getId());
				ca.setSubject(course.getSubjectArea());
				ca.setCourseNbr(course.getCourseNumber());
				boolean hasAlt = false;
				if (r.getCourses().size() > 1) {
					hasAlt = true;
				} else if (course.getOffering().getConfigs().size() > 1) {
					hasAlt = true;
				} else {
					for (Iterator<Subpart> i = ((Config)course.getOffering().getConfigs().get(0)).getSubparts().iterator(); i.hasNext();) {
						Subpart s = i.next();
						if (s.getSections().size() > 1) { hasAlt = true; break; }
					}
				}
				for (Iterator<Section> i = sections.iterator(); i.hasNext();) {
					Section section = (Section)i.next();
					ClassAssignmentInterface.ClassAssignment a = ca.addClassAssignment();
					a.setAlternative(r.isAlternative());
					a.setClassId(section.getId());
					a.setSubpart(section.getSubpart().getName());
					a.setSection(getSectionName(course.getId(), section));
					a.setLimit(getLimit(section, r.getStudent().getId()));
					if (section.getTime() != null) {
						for (DayCode d : DayCode.toDayCodes(section.getTime().getDayCode()))
							a.addDay(d.getIndex());
						a.setStart(section.getTime().getStartSlot());
						a.setLength(section.getTime().getLength());
						a.setBreakTime(section.getTime().getBreakTime());
						a.setDatePattern(section.getTime().getDatePatternName());
					}
					if (section.getRooms() != null) {
						for (Iterator<RoomLocation> e = section.getRooms().iterator(); e.hasNext(); ) {
							RoomLocation rm = e.next();
							a.addRoom(rm.getName());
						}
					}
					if (section.getChoice().getInstructorNames() != null && !section.getChoice().getInstructorNames().isEmpty()) {
						String[] instructors = section.getChoice().getInstructorNames().split(":");
						for (String instructor: instructors) {
							String[] nameEmail = instructor.split("\\|");
							a.addInstructor(nameEmail[0]);
							a.addInstructoEmailr(nameEmail.length < 2 ? "" : nameEmail[1]);
						}
					}
					if (section.getParent() != null)
						a.setParentSection(getSectionName(course.getId(), section.getParent()));
					if (requiredSections != null && requiredSections.contains(section)) a.setPinned(true);
					a.setSubpartId(section.getSubpart().getId());
					a.setHasAlternatives(hasAlt);
					int dist = 0;
					String from = null;
					for (Enrollment x: enrollments) {
						if (x == null || !x.isCourseRequest() || x.getAssignments() == null || x.getAssignments().isEmpty()) continue;
						for (Iterator<Section> j=x.getSections().iterator(); j.hasNext();) {
							Section s = j.next();
							if (s == section || s.getTime() == null) continue;
							int d = distance(s, section);
							if (d > dist) {
								dist = d;
								from = "";
								for (Iterator<RoomLocation> k = s.getRooms().iterator(); k.hasNext();)
									from += k.next().getName() + (k.hasNext() ? ", " : "");
							}
							if (d > s.getTime().getBreakTime()) {
								a.setDistanceConflict(true);
							}
						}
					}
					a.setBackToBackDistance(dist);
					a.setBackToBackRooms(from);
					// if (dist > 0.0) a.setDistanceConflict(true);
					if (savedClasses != null && savedClasses.contains(section.getId())) a.setSaved(true);
					if (a.getParentSection() == null)
						a.setParentSection(iCourseForId.get(course.getId()).getConsent());
					a.setExpected(section.getSpaceExpected());
				}
				ret.add(ca);
			} else {
				FreeTimeRequest r = (FreeTimeRequest)enrollment.getRequest();
				ClassAssignmentInterface.CourseAssignment ca = new ClassAssignmentInterface.CourseAssignment();
				ca.setAssigned(true);
				ca.setCourseId(null);
				ClassAssignmentInterface.ClassAssignment a = ca.addClassAssignment();
				a.setAlternative(r.isAlternative());
				for (DayCode d : DayCode.toDayCodes(r.getTime().getDayCode()))
					a.addDay(d.getIndex());
				a.setStart(r.getTime().getStartSlot());
				a.setLength(r.getTime().getLength());
				if (requiredFreeTimes != null && requiredFreeTimes.contains(r)) a.setPinned(true);
				ret.add(ca);
			}
		}
		
		return ret;	
	}
	
    private int distance(Section s1, Section s2) {
        if (s1.getPlacement()==null || s2.getPlacement()==null) return 0;
        TimeLocation t1 = s1.getTime();
        TimeLocation t2 = s2.getTime();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return 0;
        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
        if (a1+t1.getNrSlotsPerMeeting()==a2) {
            return Placement.getDistanceInMinutes(iDistanceMetric, s1.getPlacement(), s2.getPlacement());
        }
        /*
        else if (a2+t2.getNrSlotsPerMeeting()==a1) {
        	return Placement.getDistance(s1.getPlacement(), s2.getPlacement());
        }
        */
        return 0;
    }
	
    @SuppressWarnings("unchecked")
	private ClassAssignmentInterface convert(StudentSectioningModel model, Student student, BranchBoundNeighbour neighbour,
			Hashtable<CourseRequest, Set<Section>> requiredSectionsForCourse, HashSet<FreeTimeRequest> requiredFreeTimes, Set<Long> savedClasses) throws SectioningException {
        Enrollment [] enrollments = neighbour.getAssignment();
        if (enrollments == null || enrollments.length == 0)
        	throw new SectioningException(SectioningExceptionType.NO_SOLUTION);
        int idx=0;
        for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext(); idx++) {
        	Request r = e.next();
        	if (enrollments[idx] == null) {
        		Config c = null;
        		if (r instanceof CourseRequest)
        			c = (Config)((Course)((CourseRequest)r).getCourses().get(0)).getOffering().getConfigs().get(0);
        		enrollments[idx] = new Enrollment(r, 0, c, null);
        	}
        }
        
        return convert(enrollments, requiredSectionsForCourse, requiredFreeTimes, true, model.getDistanceConflict(), savedClasses);
	}

    @SuppressWarnings("unchecked")
	public ClassAssignmentInterface section(CourseRequestInterface request, ArrayList<ClassAssignmentInterface.ClassAssignment> currentAssignment) throws SectioningException {
		long t0 = System.currentTimeMillis();
		DataProperties config = new DataProperties(ApplicationProperties.getProperties());
		config.setProperty("Neighbour.BranchAndBoundTimeout", "1000");
		config.setProperty("Extensions.Classes", DistanceConflict.class.getName() + ";" + TimeOverlapsCounter.class.getName());
		config.setProperty("StudentWeights.Class", StudentSchedulingAssistantWeights.class.getName());
		config.setProperty("Distances.Ellipsoid", ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name()));
		config.setProperty("Reservation.CanAssignOverTheLimit", "true");
		StudentSectioningModel model = new StudentSectioningModel(config);
		model.addGlobalConstraint(new SectionLimit(model.getProperties()));
		Student student = new Student(request.getStudentId() == null ? -1l : request.getStudentId());
		for (CourseRequestInterface.Request c: request.getCourses())
			addRequest(model, student, c, false, false);
		if (student.getRequests().isEmpty()) throw new SectioningException(SectioningExceptionType.EMPTY_COURSE_REQUEST);
		for (CourseRequestInterface.Request c: request.getAlternatives())
			addRequest(model, student, c, true, false);
		model.addStudent(student);
		
		long t1 = System.currentTimeMillis();
		
		Hashtable<CourseRequest, Set<Section>> preferredSectionsForCourse = new Hashtable<CourseRequest, Set<Section>>();
		Hashtable<CourseRequest, Set<Section>> requiredSectionsForCourse = new Hashtable<CourseRequest, Set<Section>>();
		HashSet<FreeTimeRequest> requiredFreeTimes = new HashSet<FreeTimeRequest>();

		for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext();) {
			Request r = (Request)e.next();
			if (r instanceof CourseRequest) {
				CourseRequest cr = (CourseRequest)r;
				HashSet<Section> preferredSections = new HashSet<Section>();
				HashSet<Section> requiredSections = new HashSet<Section>();
				a: for (ClassAssignmentInterface.ClassAssignment a: currentAssignment) {
					if (!a.isFreeTime() && cr.getCourse(a.getCourseId()) != null && a.getClassId() != null) {
						Section section = cr.getSection(a.getClassId());
						if (section == null || section.getLimit() == 0) {
							continue a;
						}
						if (a.isPinned() || a.isSaved()) 
							requiredSections.add(section);
						preferredSections.add(section);
						cr.getSelectedChoices().add(section.getChoice());
					}
				}
				preferredSectionsForCourse.put(cr, preferredSections);
				requiredSectionsForCourse.put(cr, requiredSections);
			} else {
				FreeTimeRequest ft = (FreeTimeRequest)r;
				for (ClassAssignmentInterface.ClassAssignment a: currentAssignment) {
					if (a.isFreeTime() && a.isPinned() && ft.getTime() != null &&
						ft.getTime().getStartSlot() == a.getStart() &&
						ft.getTime().getLength() == a.getLength() && 
						ft.getTime().getDayCode() == DayCode.toInt(DayCode.toDayCodes(a.getDays())))
						requiredFreeTimes.add(ft);
				}
			}
		}
		
        Solution solution = new Solution(model,0,0);
        
        Solver solver = new Solver(model.getProperties());
        solver.setInitalSolution(solution);
        solver.initSolver();
        
		long t2 = System.currentTimeMillis();

        SuggestionSelection onlineSelection = new SuggestionSelection(model.getProperties(), preferredSectionsForCourse, requiredSectionsForCourse, requiredFreeTimes);
        onlineSelection.init(solver);

        BranchBoundSelection.Selection selection = onlineSelection.getSelection(student); 
        BranchBoundNeighbour neighbour = selection.select();
        neighbour.assign(0);
        sLog.info("Solution: " + neighbour);
        
		long t3 = System.currentTimeMillis();

		if (neighbour == null) throw new SectioningException(SectioningExceptionType.NO_SOLUTION);
        
		ClassAssignmentInterface ret = convert(model, student, neighbour, requiredSectionsForCourse, requiredFreeTimes, getSavedClasses(request.getStudentId()));

		long t4 = System.currentTimeMillis();
		sLog.info("Sectioning took "+(t4-t0)+"ms (model "+(t1-t0)+"ms, solver init "+(t2-t1)+"ms, sectioning "+(t3-t2)+"ms, conversion "+(t4-t3)+"ms)");

		return ret;
}
	
	@SuppressWarnings("unchecked")
	public List<Section> getSections(CourseInfo courseInfo) throws SectioningException {
		iLock.readLock().lock();
		try {
			ArrayList<Section> sections = new ArrayList<Section>();
			Course course = iCourseTable.get(courseInfo.getUniqueId());
			if (course == null) return sections;
			for (Iterator<Config> e=course.getOffering().getConfigs().iterator(); e.hasNext();) {
				Config cfg = e.next();
				for (Iterator<Subpart> f=cfg.getSubparts().iterator(); f.hasNext();) {
					Subpart subpart = f.next();
					for (Iterator<Section> g=subpart.getSections().iterator(); g.hasNext();) {
						Section section = g.next();
						sections.add(section);
					}
				}
			}
			return sections;
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Section> getSections(String courseName) throws SectioningException {
		iLock.readLock().lock();
		try {
			ArrayList<Section> sections = new ArrayList<Section>();
			Course course = iCourseTable.get(courseName);
			if (course == null) return sections;
			for (Iterator<Config> e=course.getOffering().getConfigs().iterator(); e.hasNext();) {
				Config cfg = e.next();
				for (Iterator<Subpart> f=cfg.getSubparts().iterator(); f.hasNext();) {
					Subpart subpart = f.next();
					for (Iterator<Section> g=subpart.getSections().iterator(); g.hasNext();) {
						Section section = g.next();
						sections.add(section);
					}
				}
			}
			return sections;
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public Collection<ClassAssignmentInterface> computeSuggestions(CourseRequestInterface request, Collection<ClassAssignmentInterface.ClassAssignment> currentAssignment, ClassAssignmentInterface.ClassAssignment selectedAssignment) throws SectioningException {
		long t0 = System.currentTimeMillis();

		DataProperties config = new DataProperties(ApplicationProperties.getProperties());
		config.setProperty("Neighbour.BranchAndBoundTimeout", "1000");
		config.setProperty("Extensions.Classes", DistanceConflict.class.getName() + ";" + TimeOverlapsCounter.class.getName());
		config.setProperty("StudentWeights.Class", StudentSchedulingAssistantWeights.class.getName());
		config.setProperty("Distances.Ellipsoid", ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name()));
		config.setProperty("Reservation.CanAssignOverTheLimit", "true");
		StudentSectioningModel model = new StudentSectioningModel(config);
		model.addGlobalConstraint(new SectionLimit(model.getProperties()));
		Student student = new Student(request.getStudentId() == null ? -1l : request.getStudentId());
		for (CourseRequestInterface.Request c: request.getCourses())
			addRequest(model, student, c, false, true);
		if (student.getRequests().isEmpty()) throw new SectioningException(SectioningExceptionType.EMPTY_COURSE_REQUEST);
		for (CourseRequestInterface.Request c: request.getAlternatives())
			addRequest(model, student, c, true, true);
		model.addStudent(student);
		model.setDistanceConflict(new DistanceConflict(null, model.getProperties()));
		
		long t1 = System.currentTimeMillis();

		Hashtable<CourseRequest, Set<Section>> preferredSectionsForCourse = new Hashtable<CourseRequest, Set<Section>>();
		Hashtable<CourseRequest, Set<Section>> requiredSectionsForCourse = new Hashtable<CourseRequest, Set<Section>>();
		HashSet<FreeTimeRequest> requiredFreeTimes = new HashSet<FreeTimeRequest>();
        ArrayList<ClassAssignmentInterface> ret = new ArrayList<ClassAssignmentInterface>();
        ClassAssignmentInterface messages = new ClassAssignmentInterface();
        ret.add(messages);

		Request selectedRequest = null;
		Section selectedSection = null;
		for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext();) {
			Request r = (Request)e.next();
			if (r instanceof CourseRequest) {
				CourseRequest cr = (CourseRequest)r;
				if (!selectedAssignment.isFreeTime() && cr.getCourse(selectedAssignment.getCourseId()) != null) {
					selectedRequest = r;
					if (selectedAssignment.getClassId() != null) {
						Section section = cr.getSection(selectedAssignment.getClassId());
						if (section != null) selectedSection = section;
					}
				}
				HashSet<Section> preferredSections = new HashSet<Section>();
				HashSet<Section> requiredSections = new HashSet<Section>();
				a: for (ClassAssignmentInterface.ClassAssignment a: currentAssignment) {
					if (!a.isFreeTime() && cr.getCourse(a.getCourseId()) != null && a.getClassId() != null) {
						Section section = cr.getSection(a.getClassId());
						if (section == null || section.getLimit() == 0) {
							messages.addMessage((a.isSaved() ? "Enrolled class" : a.isPinned() ? "Required class" : "Previously selected class") + a.getSubject() + " " + a.getCourseNbr() + " " + a.getSubpart() + " " + a.getSection() + " is no longer available.");
							continue a;
						}
						if (a.isPinned() || a.isSaved()) 
							requiredSections.add(section);
						preferredSections.add(section);
						cr.getSelectedChoices().add(section.getChoice());
					}
				}
				preferredSectionsForCourse.put(cr, preferredSections);
				requiredSectionsForCourse.put(cr, requiredSections);
			} else {
				FreeTimeRequest ft = (FreeTimeRequest)r;
				if (selectedAssignment.isFreeTime() && ft.getTime() != null &&
					ft.getTime().getStartSlot() == selectedAssignment.getStart() &&
					ft.getTime().getLength() == selectedAssignment.getLength() && 
					ft.getTime().getDayCode() == DayCode.toInt(DayCode.toDayCodes(selectedAssignment.getDays())))
					selectedRequest = r;
				for (ClassAssignmentInterface.ClassAssignment a: currentAssignment) {
					if (a.isFreeTime() && a.isPinned() && ft.getTime() != null &&
						ft.getTime().getStartSlot() == a.getStart() &&
						ft.getTime().getLength() == a.getLength() && 
						ft.getTime().getDayCode() == DayCode.toInt(DayCode.toDayCodes(a.getDays())))
						requiredFreeTimes.add(ft);
				}
			}
		}
		
        new Solution(model);
        
		long t2 = System.currentTimeMillis();
        
        SuggestionsBranchAndBound suggestionBaB = new SuggestionsBranchAndBound(model.getProperties(), student, requiredSectionsForCourse, requiredFreeTimes, preferredSectionsForCourse, selectedRequest, selectedSection);
        TreeSet<SuggestionsBranchAndBound.Suggestion> suggestions = suggestionBaB.computeSuggestions();
        
		long t3 = System.currentTimeMillis();
		sLog.debug("  -- suggestion B&B took "+suggestionBaB.getTime()+"ms"+(suggestionBaB.isTimeoutReached()?", timeout reached":""));

		Set<Long> savedClasses = getSavedClasses(request.getStudentId());
		for (SuggestionsBranchAndBound.Suggestion suggestion : suggestions) {
        	ret.add(convert(suggestion.getEnrollments(), requiredSectionsForCourse, requiredFreeTimes, false, model.getDistanceConflict(), savedClasses));
        }
        
		long t4 = System.currentTimeMillis();
		sLog.info("Sectioning took "+(t4-t0)+"ms (model "+(t1-t0)+"ms, solver init "+(t2-t1)+"ms, sectioning "+(t3-t2)+"ms, conversion "+(t4-t3)+"ms)");

        return ret;
	}
	
	public class EnrollmentSectionComparator implements Comparator<Section> {
	    public boolean isParent(Section s1, Section s2) {
			Section p1 = s1.getParent();
			if (p1==null) return false;
			if (p1.equals(s2)) return true;
			return isParent(p1, s2);
		}

		public int compare(Section a, Section b) {
			if (isParent(a, b)) return 1;
	        if (isParent(b, a)) return -1;

	        int cmp = a.getSubpart().getInstructionalType().compareToIgnoreCase(b.getSubpart().getInstructionalType());
			if (cmp != 0) return cmp;
			
			return Double.compare(a.getId(), b.getId());
		}
	}
	
	public Section getSection(Long classId) {
		iLock.readLock().lock();
		try {
			return iClassTable.get(classId);
		} finally {
			iLock.readLock().unlock();
		}
	}
	
	protected void studentChanged(Collection<Long> studentIds) {
		if (!"true".equals(ApplicationProperties.getProperty("unitime.enrollment.load", "true"))) return;
		sLog.info(studentIds.size() + " student schedules changed.");
		iLock.writeLock().lock();
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().createNewSession();
			try {
				for (Long studentId: studentIds) {
					// Unload student
					Student student = iStudentTable.get(studentId);
					if (student != null) {
						iStudentTable.remove(studentId);
						for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext();) {
							Request r = (Request)e.next();
							if (r.getAssignment() != null) r.unassign(0);
						}
						iModel.removeStudent(student);
					}
					
					// Load student
					org.unitime.timetable.model.Student s = StudentDAO.getInstance().get(studentId, hibSession);
					if (s != null) {
						student = iLoader.loadStudent(s);
						iModel.addStudent(student);
						iLoader.assignStudent(student, s, false);
					}
				}
			} finally {
				hibSession.close();
			}
		} finally {
			iLock.writeLock().unlock();
		}
	}
	
	protected void classChanged(Collection<Long> classIds) {
		sLog.info(classIds.size() + " class assignments changed.");
		iLock.writeLock().lock();
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().createNewSession();
			try {
				for (Long classId: classIds) {
					Section section = iClassTable.get(classId);
					Class_ clazz = Class_DAO.getInstance().get(classId, hibSession);
					if (section != null && clazz != null) {
						// class updated
						sLog.info("Reloading " + clazz.getClassLabel());
	                    org.unitime.timetable.model.Assignment a = clazz.getCommittedAssignment();
	                    Placement p = (a == null ? null : a.getPlacement());
	                    if (p != null && p.getTimeLocation() != null) {
	                    	p.getTimeLocation().setDatePattern(
	                    			p.getTimeLocation().getDatePatternId(),
	                    			iLoader.datePatternName(p.getTimeLocation()),
	                    			p.getTimeLocation().getWeekCode());
	                    }
	                    section.setPlacement(p);
						sLog.info("  -- placement: " + p);

	                    int minLimit = clazz.getExpectedCapacity();
	                	int maxLimit = clazz.getMaxExpectedCapacity();
	                	int limit = maxLimit;
	                	if (minLimit < maxLimit && p != null) {
	                		int roomLimit = Math.round((clazz.getRoomRatio() == null ? 1.0f : clazz.getRoomRatio()) * p.getRoomSize());
	                		limit = Math.min(Math.max(minLimit, roomLimit), maxLimit);
	                	}
	                    if (clazz.getSchedulingSubpart().getInstrOfferingConfig().isUnlimitedEnrollment() || limit >= 9999) limit = -1;
	                    section.setLimit(limit);
						sLog.info("  -- limit: " + limit);

	                    String instructorIds = "";
	                    String instructorNames = "";
	                    for (Iterator<ClassInstructor> k = clazz.getClassInstructors().iterator(); k.hasNext(); ) {
	                    	ClassInstructor ci = k.next();
	                    	if (!ci.isLead()) continue;
	                    	if (!instructorIds.isEmpty()) {
	                    		instructorIds += ":"; instructorNames += ":";
	                    	}
	                    	instructorIds += ci.getInstructor().getUniqueId().toString();
	                    	instructorNames += ci.getInstructor().getName(DepartmentalInstructor.sNameFormatShort) + "|"  + (ci.getInstructor().getEmail() == null ? "" : ci.getInstructor().getEmail());
	                    }
	                    section.getChoice().setInstructor(instructorIds, instructorNames);
						sLog.info("  -- instructor: " + instructorNames);

	                    section.setName(clazz.getExternalUniqueId() == null ? clazz.getClassSuffix() : clazz.getExternalUniqueId());
					} else if (section != null && clazz == null) {
						// class deleted -> unassign all enrollments, delete section
						sLog.info("Removing " + section.getLongName());
						
						iClassTable.remove(classId);
						for (Enrollment enrollment: section.getEnrollments())
							enrollment.getRequest().unassign(0);
						Subpart subpart = section.getSubpart();
						subpart.getSections().remove(section);
						if (subpart.getSections().isEmpty()) {
							Config config = subpart.getConfig();
							config.getSubparts().remove(subpart);
							if (config.getSubparts().isEmpty())
								config.getOffering().getConfigs().remove(config);
						}
					} else {
						// class added
						//TODO: Adding classes is not supported at the moment
						sLog.warn("Adding " + clazz.getClassLabel() + " not implemented");
					}
				}
			} finally {
				hibSession.close();
			}
		} finally {
			iLock.writeLock().unlock();
		}
	}
	
	protected void allStudentsChanged() {
		if (!"true".equals(ApplicationProperties.getProperty("unitime.enrollment.load", "true"))) return;
		iLock.writeLock().lock();
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().createNewSession();
			try {
				for (Student student: iStudentTable.values()) {
					for (Iterator<Request> e = student.getRequests().iterator(); e.hasNext();) {
						Request r = (Request)e.next();
						if (r.getAssignment() != null) r.unassign(0);
					}
					iModel.removeStudent(student);
				}
				iStudentTable.clear();

				List<org.unitime.timetable.model.Student> students = hibSession.createQuery(
	                    "select distinct s from Student s " +
	                    "left join fetch s.courseDemands as cd " +
	                    "left join fetch cd.courseRequests as cr " +
	                    "left join fetch s.classEnrollments as e " +
	                    "where s.session.uniqueId=:sessionId").
	                    setLong("sessionId",iAcademicSession.getUniqueId()).list();
	            for (org.unitime.timetable.model.Student student: students) {
	            	Student s = iLoader.loadStudent(student);
	            	iModel.addStudent(s);
	            	iLoader.assignStudent(s, student, true);
	            }
			} finally {
				hibSession.close();
			}
		} finally {
			iLock.writeLock().unlock();
		}
	}
	
	public void unload() {
		sGlobalLock.writeLock().lock();
		try {
			iUpdater.stopUpdating();
			sInstances.remove(getAcademicSessionId());
		} finally {
			sGlobalLock.writeLock().unlock();
		}
	}
	
	public static void unloadAll() {
		sGlobalLock.writeLock().lock();
		try {
			for (SectioningServer s: sInstances.values())
				s.iUpdater.stopUpdating();
			sInstances.clear();
		} finally {
			sGlobalLock.writeLock().unlock();
		}
	}
	
	public ReadWriteLock getLock() {
		return iLock;
	}
	
	public static class DummyReservation extends Reservation {
		private int iPriority;
		private boolean iOver;
		private int iLimit;
		private boolean iApply;
		
		public DummyReservation(long id, Offering offering, int priority, boolean over, int limit, boolean apply) {
			super(id, offering);
			iPriority = priority;
			iOver = over;
			iLimit = limit;
			iApply = apply;
		}
		
		@Override
		public boolean canAssignOverLimit() {
			return iOver;
		}

		@Override
		public double getLimit() {
			return iLimit;
		}

		@Override
		public int getPriority() {
			return iPriority;
		}

		@Override
		public boolean isApplicable(Student student) {
			return iApply;
		}
		
	}
}
