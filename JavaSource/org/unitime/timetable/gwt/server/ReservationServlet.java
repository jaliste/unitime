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
package org.unitime.timetable.gwt.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.gwt.services.ReservationService;
import org.unitime.timetable.gwt.shared.ReservationException;
import org.unitime.timetable.gwt.shared.ReservationInterface;
import org.unitime.timetable.model.AcademicArea;
import org.unitime.timetable.model.AcademicClassification;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.CourseReservation;
import org.unitime.timetable.model.Curriculum;
import org.unitime.timetable.model.CurriculumClassification;
import org.unitime.timetable.model.CurriculumCourse;
import org.unitime.timetable.model.CurriculumProjectionRule;
import org.unitime.timetable.model.CurriculumReservation;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.IndividualReservation;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.Reservation;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Settings;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentGroup;
import org.unitime.timetable.model.StudentGroupReservation;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.comparators.InstrOfferingConfigComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.AcademicAreaDAO;
import org.unitime.timetable.model.dao.AcademicClassificationDAO;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.CurriculumDAO;
import org.unitime.timetable.model.dao.InstrOfferingConfigDAO;
import org.unitime.timetable.model.dao.InstructionalOfferingDAO;
import org.unitime.timetable.model.dao.PosMajorDAO;
import org.unitime.timetable.model.dao.ReservationDAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentGroupDAO;
import org.unitime.timetable.util.Constants;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * @author Tomas Muller
 */
public class ReservationServlet extends RemoteServiceServlet implements ReservationService {
	private static final long serialVersionUID = -3174041940015933713L;
	private static Logger sLog = Logger.getLogger(ReservationServlet.class);
	private static DateFormat sDateFormat = new SimpleDateFormat("MM/dd/yyyy");

	public void init() throws ServletException {
	}

	@Override
	public List<ReservationInterface.Area> getAreas() throws ReservationException {
		try {
			List<ReservationInterface.Area> results = new ArrayList<ReservationInterface.Area>();
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			try {
				List<ReservationInterface.IdName> classifications = new ArrayList<ReservationInterface.IdName>();
				for (AcademicClassification classification: (List<AcademicClassification>)hibSession.createQuery(
						"select c from AcademicClassification c where c.session.uniqueId = :sessionId order by c.code, c.name")
						.setLong("sessionId", getAcademicSessionId()).setCacheable(true).list()) {
					ReservationInterface.IdName clasf = new ReservationInterface.IdName();
					clasf.setId(classification.getUniqueId());
					clasf.setName(Constants.curriculaToInitialCase(classification.getName()));
					clasf.setAbbv(classification.getCode());
					classifications.add(clasf);
				}
				for (AcademicArea area: (List<AcademicArea>)hibSession.createQuery(
						"select a from AcademicArea a where a.session.uniqueId = :sessionId order by a.academicAreaAbbreviation, a.longTitle, a.shortTitle")
						.setLong("sessionId", getAcademicSessionId()).setCacheable(true).list()) {
					ReservationInterface.Area curriculum = new ReservationInterface.Area();
					curriculum.setAbbv(area.getAcademicAreaAbbreviation());
					curriculum.setId(area.getUniqueId());
					curriculum.setName(Constants.curriculaToInitialCase(area.getLongTitle() == null ? area.getShortTitle() : area.getLongTitle()));
					for (PosMajor major: area.getPosMajors()) {
						ReservationInterface.IdName mj = new ReservationInterface.IdName();
						mj.setId(major.getUniqueId());
						mj.setAbbv(major.getCode());
						mj.setName(Constants.curriculaToInitialCase(major.getName()));
						curriculum.getMajors().add(mj);
					}
					Collections.sort(curriculum.getMajors());
					curriculum.getClassifications().addAll(classifications);
					results.add(curriculum);
				}
			} finally {
				hibSession.close();
			}
			return results;
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}
	
	private ReservationInterface.Offering convert(InstructionalOffering io, org.hibernate.Session hibSession) throws ReservationException {
		ReservationInterface.Offering offering = new ReservationInterface.Offering();
		offering.setAbbv(io.getCourseName());
		offering.setName(io.getControllingCourseOffering().getTitle());
		offering.setId(io.getUniqueId());
		for (CourseOffering co: io.getCourseOfferings()) {
			ReservationInterface.Course course = new ReservationInterface.Course();
			course.setId(co.getUniqueId());
			course.setAbbv(co.getCourseName());
			course.setName(co.getTitle());
			course.setControl(co.isIsControl());
			course.setLimit(co.getReservation());
			offering.getCourses().add(course);
		}
		for (InstrOfferingConfig ioc: io.getInstrOfferingConfigs()) {
			ReservationInterface.Config config = new ReservationInterface.Config();
			config.setId(ioc.getUniqueId());
			config.setName(ioc.getName());
			config.setAbbv(ioc.getName());
			config.setLimit(ioc.isUnlimitedEnrollment() ? null : ioc.getLimit());
			offering.getConfigs().add(config);
			TreeSet<SchedulingSubpart> subparts = new TreeSet<SchedulingSubpart>(new SchedulingSubpartComparator());
			subparts.addAll(ioc.getSchedulingSubparts());
			for (SchedulingSubpart ss: subparts) {
				ReservationInterface.Subpart subpart = new ReservationInterface.Subpart();
				subpart.setId(ss.getUniqueId());
				String suffix = ss.getSchedulingSubpartSuffix(hibSession);
				subpart.setAbbv(ss.getItypeDesc() + (suffix == null || suffix.isEmpty() ? "" : " " + suffix));
				subpart.setName(ss.getSchedulingSubpartLabel());
				subpart.setConfig(config);
				config.getSubparts().add(subpart);
				if (ss.getParentSubpart() != null)
					subpart.setParentId(ss.getParentSubpart().getUniqueId());
				List<Class_> classes = new ArrayList<Class_>(ss.getClasses());
				Collections.sort(classes, new ClassComparator(ClassComparator.COMPARE_BY_HIERARCHY));
				for (Class_ c: classes) {
					ReservationInterface.Clazz clazz = new ReservationInterface.Clazz();
					clazz.setId(c.getUniqueId());
					clazz.setAbbv(ss.getItypeDesc() + " " + c.getSectionNumberString(hibSession));
					clazz.setName(c.getClassLabel(hibSession));
					subpart.getClasses().add(clazz);
					clazz.setSubpart(subpart);
					clazz.setLimit(c.getClassLimit());
					if (c.getParentClass() != null)
						clazz.setParentId(c.getParentClass().getUniqueId());
				}
			}
		}
		return offering;
	}

	@Override
	public ReservationInterface.Offering getOffering(Long offeringId) throws ReservationException {
		try {
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			try {
				InstructionalOffering io = InstructionalOfferingDAO.getInstance().get(offeringId, hibSession);
				if (io == null) { throw new ReservationException("Offering does not exist."); }
				return convert(io, hibSession);
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}
	
	private CourseOffering getCourse(org.hibernate.Session hibSession, String courseName) {
		for (CourseOffering co: (List<CourseOffering>)hibSession.createQuery(
				"select c from CourseOffering c where " +
				"c.subjectArea.session.uniqueId = :sessionId and " +
				"lower(c.subjectArea.subjectAreaAbbreviation || ' ' || c.courseNbr) = :course")
				.setString("course", courseName.toLowerCase())
				.setLong("sessionId", getAcademicSessionId())
				.setCacheable(true).setMaxResults(1).list()) {
			return co;
		}
		return null;
	}
	public ReservationInterface.Offering getOfferingByCourseName(String courseName) throws ReservationException{
		try {
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			try {
				CourseOffering co = getCourse(hibSession, courseName);
				if (co == null) { throw new ReservationException("Course " + courseName + " does not exist."); }
				return convert(co.getInstructionalOffering(), hibSession);
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}
	
	private Hashtable<String,HashMap<String, Float>> getRules(org.hibernate.Session hibSession, Long acadAreaId) {
		Hashtable<String,HashMap<String, Float>> clasf2major2proj = new Hashtable<String, HashMap<String,Float>>();
		for (CurriculumProjectionRule rule: (List<CurriculumProjectionRule>)hibSession.createQuery(
				"select r from CurriculumProjectionRule r where r.academicArea.uniqueId=:acadAreaId")
				.setLong("acadAreaId", acadAreaId).setCacheable(true).list()) {
			String majorCode = (rule.getMajor() == null ? "" : rule.getMajor().getCode());
			String clasfCode = rule.getAcademicClassification().getCode();
			Float projection = rule.getProjection();
			HashMap<String, Float> major2proj = clasf2major2proj.get(clasfCode);
			if (major2proj == null) {
				major2proj = new HashMap<String, Float>();
				clasf2major2proj.put(clasfCode, major2proj);
			}
			major2proj.put(majorCode, projection);
		}
		return clasf2major2proj;
	}
	
	public float getProjection(Hashtable<String,HashMap<String, Float>> clasf2major2proj, String majorCode, String clasfCode) {
		if (clasf2major2proj == null || clasf2major2proj.isEmpty()) return 1.0f;
		HashMap<String, Float> major2proj = clasf2major2proj.get(clasfCode);
		if (major2proj == null) return 1.0f;
		Float projection = major2proj.get(majorCode);
		if (projection == null)
			projection = major2proj.get("");
		return (projection == null ? 1.0f : projection);
	}
	
	private ReservationInterface convert(Reservation reservation, String nameFormat, org.hibernate.Session hibSession) {
		ReservationInterface r = null;
		if (reservation instanceof CourseReservation) {
			CourseOffering co = ((CourseReservation) reservation).getCourse();
			ReservationInterface.Course course = new ReservationInterface.Course();
			course.setId(co.getUniqueId());
			course.setAbbv(co.getCourseName());
			course.setControl(co.isIsControl());
			course.setName(co.getTitle());
			course.setLimit(co.getReservation());
			r = new ReservationInterface.CourseReservation();
			((ReservationInterface.CourseReservation) r).setCourse(course);
			r.setLastLike(co.getDemand());
			r.setEnrollment(co.getEnrollment());
			r.setProjection(co.getProjectedDemand());
		} else if (reservation instanceof IndividualReservation) {
			r = new ReservationInterface.IndividualReservation();
			String sId = "";
			for (Student student: ((IndividualReservation) reservation).getStudents()) {
				ReservationInterface.IdName s = new ReservationInterface.IdName();
				s.setId(student.getUniqueId());
				s.setAbbv(student.getExternalUniqueId());
				s.setName(student.getName(nameFormat));
				((ReservationInterface.IndividualReservation) r).getStudents().add(s);
				sId += (sId.isEmpty() ? "" : ",") + student.getUniqueId();
			}
			Collections.sort(((ReservationInterface.IndividualReservation) r).getStudents(), new Comparator<ReservationInterface.IdName>() {
				@Override
				public int compare(ReservationInterface.IdName s1, ReservationInterface.IdName s2) {
					int cmp = s1.getName().compareTo(s2.getName());
					if (cmp != 0) return cmp;
					return s1.getAbbv().compareTo(s2.getAbbv());
				}
			});
			if (!sId.isEmpty()) {
				Number enrollment = (Number)hibSession.createQuery(
						"select count(distinct e.student) " +
						"from StudentClassEnrollment e where " +
						"e.courseOffering.instructionalOffering.uniqueId = :offeringId " +
						"and e.student.uniqueId in (" + sId + ")")
						.setLong("offeringId", reservation.getInstructionalOffering().getUniqueId()).uniqueResult();
				if (enrollment.intValue() > 0)
					r.setEnrollment(enrollment.intValue());
			}
		} else if (reservation instanceof CurriculumReservation) {
			CurriculumReservation cr = (CurriculumReservation) reservation;
			r = new ReservationInterface.CurriculumReservation();
			ReservationInterface.Area curriculum = new ReservationInterface.Area();
			curriculum.setId(cr.getArea().getUniqueId());
			curriculum.setAbbv(cr.getArea().getAcademicAreaAbbreviation());
			curriculum.setName(Constants.curriculaToInitialCase(cr.getArea().getLongTitle() == null ? cr.getArea().getShortTitle() : cr.getArea().getLongTitle()));
			String cfCodes = "";
			String cfIds = "";
			for (AcademicClassification classification: cr.getClassifications()) {
				ReservationInterface.IdName clasf = new ReservationInterface.IdName();
				clasf.setId(classification.getUniqueId());
				clasf.setName(Constants.curriculaToInitialCase(classification.getName()));
				clasf.setAbbv(classification.getCode());
				curriculum.getClassifications().add(clasf);
				cfCodes += (cfCodes.isEmpty() ? "" : ",") + "'" + classification.getCode() + "'";
				cfIds += (cfIds.isEmpty() ? "" : ",") + classification.getUniqueId();
			}
			String mjCodes = "";
			String mjIds = "";
			for (PosMajor major: cr.getMajors()) {
				ReservationInterface.IdName mj = new ReservationInterface.IdName();
				mj.setId(major.getUniqueId());
				mj.setAbbv(major.getCode());
				mj.setName(Constants.curriculaToInitialCase(major.getName()));
				curriculum.getMajors().add(mj);
				mjCodes += (mjCodes.isEmpty() ? "" : ",") + "'" + major.getCode() + "'";
				mjIds += (mjIds.isEmpty() ? "" : ",") + major.getUniqueId();
			}
			Collections.sort(curriculum.getMajors(), new Comparator<ReservationInterface.IdName>() {
				@Override
				public int compare(ReservationInterface.IdName s1, ReservationInterface.IdName s2) {
					int cmp = s1.getAbbv().compareTo(s2.getAbbv());
					if (cmp != 0) return cmp;
					cmp = s1.getName().compareTo(s2.getName());
					if (cmp != 0) return cmp;
					return s1.getId().compareTo(s2.getId());
				}
			});
			Collections.sort(curriculum.getClassifications(), new Comparator<ReservationInterface.IdName>() {
				@Override
				public int compare(ReservationInterface.IdName s1, ReservationInterface.IdName s2) {
					int cmp = s1.getAbbv().compareTo(s2.getAbbv());
					if (cmp != 0) return cmp;
					cmp = s1.getName().compareTo(s2.getName());
					if (cmp != 0) return cmp;
					return s1.getId().compareTo(s2.getId());
				}
			});
			((ReservationInterface.CurriculumReservation) r).setCurriculum(curriculum);
			Number enrollment = (Number)hibSession.createQuery(
					"select count(distinct e.student) " +
					"from StudentClassEnrollment e inner join e.student.academicAreaClassifications a inner join e.student.posMajors m where " +
					"e.courseOffering.instructionalOffering.uniqueId = :offeringId " +
					"and a.academicArea.uniqueId = :areaId" + 
					(mjIds.isEmpty() ? "" : " and m.uniqueId in (" + mjIds + ")") +
					(cfIds.isEmpty() ? "" : " and a.academicClassification.uniqueId in (" + cfIds + ")"))
					.setLong("offeringId", reservation.getInstructionalOffering().getUniqueId())
					.setLong("areaId", cr.getArea().getUniqueId()).uniqueResult();
			if (enrollment.intValue() > 0)
				r.setEnrollment(enrollment.intValue());
			/*
			Number lastLike = (Number)hibSession.createQuery(
					"select count(distinct s) from " +
					"LastLikeCourseDemand x inner join x.student s inner join s.academicAreaClassifications a inner join s.posMajors m " +
					"inner join a.academicClassification f inner join a.academicArea r, CourseOffering co where " +
					"x.subjectArea.session.uniqueId = :sessionId and co.instructionalOffering.uniqueId = :offeringId and "+
					"co.subjectArea.uniqueId = x.subjectArea.uniqueId and " +
					"((x.coursePermId is not null and co.permId=x.coursePermId) or (x.coursePermId is null and co.courseNbr=x.courseNbr)) " +
					"and r.academicAreaAbbreviation = :areaAbbv" +
					(mjCodes.isEmpty() ? "" : " and m.code in (" + mjCodes + ")") +
					(cfCodes.isEmpty() ? "" : " and f.code in (" + cfCodes + ")"))
					.setLong("sessionId", getAcademicSessionId())
					.setLong("offeringId", reservation.getInstructionalOffering().getUniqueId())
					.setString("areaAbbv", cr.getArea().getAcademicAreaAbbreviation()).uniqueResult();
			r.setLastLike(lastLike.intValue());
			*/
			float projection = 0f;
			int lastLike = 0;
			Hashtable<String,HashMap<String, Float>> rules = getRules(hibSession, cr.getArea().getUniqueId());
			for (Object[] o: (List<Object[]>)hibSession.createQuery(
					"select count(distinct s), m.code, f.code from " +
					"LastLikeCourseDemand x inner join x.student s inner join s.academicAreaClassifications a inner join s.posMajors m " +
					"inner join a.academicClassification f inner join a.academicArea r, CourseOffering co where " +
					"x.subjectArea.session.uniqueId = :sessionId and co.instructionalOffering.uniqueId = :offeringId and "+
					"co.subjectArea.uniqueId = x.subjectArea.uniqueId and " +
					"((x.coursePermId is not null and co.permId=x.coursePermId) or (x.coursePermId is null and co.courseNbr=x.courseNbr)) " +
					"and r.academicAreaAbbreviation = :areaAbbv" +
					(mjCodes.isEmpty() ? "" : " and m.code in (" + mjCodes + ")") +
					(cfCodes.isEmpty() ? "" : " and f.code in (" + cfCodes + ")") +
					" group by m.code, f.code")
					.setLong("sessionId", getAcademicSessionId())
					.setLong("offeringId", reservation.getInstructionalOffering().getUniqueId())
					.setString("areaAbbv", cr.getArea().getAcademicAreaAbbreviation()).list()) {
				int nrStudents = ((Number)o[0]).intValue();
				lastLike += nrStudents;
				projection += getProjection(rules, (String)o[1], (String)o[2]) * nrStudents;
			}
			if (lastLike > 0) {
				r.setLastLike(lastLike);
				r.setProjection(Math.round(projection));
			}
			
		} else if (reservation instanceof StudentGroupReservation) {
			r = new ReservationInterface.GroupReservation();
			StudentGroup sg = ((StudentGroupReservation) reservation).getGroup();
			ReservationInterface.IdName group = new ReservationInterface.IdName();
			group.setId(sg.getUniqueId());
			group.setName(sg.getGroupName());
			group.setAbbv(sg.getGroupAbbreviation());
			group.setLimit(sg.getStudents().size());
			((ReservationInterface.GroupReservation) r).setGroup(group);
			Number enrollment = (Number)hibSession.createQuery(
					"select count(distinct e.student) " +
					"from StudentClassEnrollment e inner join e.student.groups g where " +
					"e.courseOffering.instructionalOffering.uniqueId = :offeringId " +
					"and g.uniqueId = :groupId")
					.setLong("offeringId", reservation.getInstructionalOffering().getUniqueId())
					.setLong("groupId", sg.getUniqueId()).uniqueResult();
			if (enrollment.intValue() > 0)
				r.setEnrollment(enrollment.intValue());
		} else {
			throw new ReservationException("Unknown reservation " + reservation.getClass().getName());
		}
		ReservationInterface.Offering offering = new ReservationInterface.Offering();
		offering.setAbbv(reservation.getInstructionalOffering().getCourseName());
		offering.setName(reservation.getInstructionalOffering().getControllingCourseOffering().getTitle());
		offering.setId(reservation.getInstructionalOffering().getUniqueId());
		r.setOffering(offering);
		for (CourseOffering co: reservation.getInstructionalOffering().getCourseOfferings()) {
			ReservationInterface.Course course = new ReservationInterface.Course();
			course.setId(co.getUniqueId());
			course.setAbbv(co.getCourseName());
			course.setName(co.getTitle());
			course.setControl(co.isIsControl());
			course.setLimit(co.getReservation());
			offering.getCourses().add(course);
		}
		List<InstrOfferingConfig> configs = new ArrayList<InstrOfferingConfig>(reservation.getConfigurations());
		Collections.sort(configs, new InstrOfferingConfigComparator(null));
		for (InstrOfferingConfig ioc: configs) {
			ReservationInterface.Config config = new ReservationInterface.Config();
			config.setId(ioc.getUniqueId());
			config.setName(ioc.getName());
			config.setAbbv(ioc.getName());
			config.setLimit(ioc.isUnlimitedEnrollment() ? null : ioc.getLimit());
			r.getConfigs().add(config);
		}
		List<Class_> classes = new ArrayList<Class_>(reservation.getClasses());
		Collections.sort(classes, new ClassComparator(ClassComparator.COMPARE_BY_HIERARCHY));
		for (Class_ c: classes) {
			ReservationInterface.Clazz clazz = new ReservationInterface.Clazz();
			clazz.setId(c.getUniqueId());
			clazz.setAbbv(c.getSchedulingSubpart().getItypeDesc() + " " + c.getSectionNumberString(hibSession));
			clazz.setName(c.getClassLabel(hibSession));
			clazz.setLimit(c.getClassLimit());
			r.getClasses().add(clazz);
		}
		r.setExpirationDate(reservation.getExpirationDate());
		r.setLimit(reservation.getLimit());
		r.setId(reservation.getUniqueId());
		return r;
	}

	@Override
	public List<ReservationInterface> getReservations(Long offeringId) throws ReservationException {
		try {
			List<ReservationInterface> results = new ArrayList<ReservationInterface>();
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			User user = Web.getUser(getThreadLocalRequest().getSession());
			String nameFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
			try {
				for (Reservation reservation: (List<Reservation>)hibSession.createQuery(
						"select r from Reservation r where r.instructionalOffering.uniqueId = :offeringId")
						.setLong("offeringId", offeringId).setCacheable(true).list()) {
					ReservationInterface r = convert(reservation, nameFormat, hibSession);
					r.setEditable(reservation.isEditableBy(user));
					results.add(r);
				}				
			} finally {
				hibSession.close();
			}
			Collections.sort(results);
			return results;
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}

	@Override
	public List<ReservationInterface.IdName> getStudentGroups() throws ReservationException {
		try {
			List<ReservationInterface.IdName> results = new ArrayList<ReservationInterface.IdName>();
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			try {
				for (StudentGroup sg: (List<StudentGroup>)hibSession.createQuery(
						"select g from StudentGroup g where g.session.uniqueId = :sessionId order by g.groupName")
						.setLong("sessionId", getAcademicSessionId()).setCacheable(true).list()) {
					ReservationInterface.IdName group = new ReservationInterface.IdName();
					group.setId(sg.getUniqueId());
					group.setName(sg.getGroupAbbreviation());
					group.setAbbv(sg.getGroupName());
					group.setLimit(sg.getStudents().size());
					results.add(group);
				}
			} finally {
				hibSession.close();
			}
			return results;
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}

	@Override
	public ReservationInterface getReservation(Long reservationId) throws ReservationException {
		try {
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			User user = Web.getUser(getThreadLocalRequest().getSession());
			ReservationInterface r;
			try {
				Reservation reservation = ReservationDAO.getInstance().get(reservationId, hibSession);
				if (reservation == null)
					throw new ReservationException("Reservation not found.");
				r = convert(reservation, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT), hibSession);
				r.setEditable(reservation.isEditableBy(user));
			} finally {
				hibSession.close();
			}
			return r;
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}
	
	@Override
	public Long save(ReservationInterface reservation) throws ReservationException {
		try {
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			User user = Web.getUser(getThreadLocalRequest().getSession());
			try {
				InstructionalOffering offering = InstructionalOfferingDAO.getInstance().get(reservation.getOffering().getId(), hibSession);
				if (offering == null)
					throw new ReservationException("Offering " + reservation.getOffering().getName() + " does not exist.");
				if (!offering.getDepartment().isLimitedEditableBy(user))
					throw new ReservationException("You are not not authorized to create / update reservations for " + offering.getCourseName() + ".");
		    	if (offering.getSession().getStatusType().canOnlineSectionStudents() && !offering.getSession().isOfferingLocked(offering.getUniqueId()))
					throw new ReservationException("Offering " + offering.getCourseName() + " is unlocked, please lock it first.");
				Reservation r = null;
				if (reservation.getId() != null) {
					r = ReservationDAO.getInstance().get(reservation.getId(), hibSession);
				}
				if (r == null) {
					if (reservation instanceof ReservationInterface.IndividualReservation)
						r = new IndividualReservation();
					else if (reservation instanceof ReservationInterface.GroupReservation)
						r = new StudentGroupReservation();
					else if (reservation instanceof ReservationInterface.CurriculumReservation)
						r = new CurriculumReservation();
					else if (reservation instanceof ReservationInterface.CourseReservation)
						r = new CourseReservation();
					else 
						throw new ReservationException("Unknown reservation " + reservation.getClass().getName());
				}
				r.setLimit(r instanceof IndividualReservation ? null : reservation.getLimit());
				r.setExpirationDate(reservation.getExpirationDate());
				r.setInstructionalOffering(offering);
				offering.getReservations().add(r);
				if (r.getClasses() == null)
					r.setClasses(new HashSet<Class_>());
				else
					r.getClasses().clear();
				for (ReservationInterface.Clazz clazz: reservation.getClasses())
					r.getClasses().add(Class_DAO.getInstance().get(clazz.getId(), hibSession));
				if (r.getConfigurations() == null)
					r.setConfigurations(new HashSet<InstrOfferingConfig>());
				else
					r.getConfigurations().clear();
				for (ReservationInterface.Config config: reservation.getConfigs())
					r.getConfigurations().add(InstrOfferingConfigDAO.getInstance().get(config.getId(), hibSession));
				if (r instanceof IndividualReservation) {
					IndividualReservation ir = (IndividualReservation)r;
					if (ir.getStudents() == null)
						ir.setStudents(new HashSet<Student>());
					else
						ir.getStudents().clear();
					for (ReservationInterface.IdName student: ((ReservationInterface.IndividualReservation) reservation).getStudents()) {
						Student s = Student.findByExternalId(offering.getSessionId(), student.getAbbv());
						if (s != null)
							ir.getStudents().add(s);
					}
				} else if (r instanceof CourseReservation) {
					((CourseReservation)r).setCourse(CourseOfferingDAO.getInstance().get(((ReservationInterface.CourseReservation) reservation).getCourse().getId(), hibSession));
				} else if (r instanceof StudentGroupReservation) {
					((StudentGroupReservation)r).setGroup(StudentGroupDAO.getInstance().get(((ReservationInterface.GroupReservation) reservation).getGroup().getId(), hibSession));
				} else if (r instanceof CurriculumReservation) {
					ReservationInterface.Area curriculum = ((ReservationInterface.CurriculumReservation)reservation).getCurriculum();
					CurriculumReservation cr = (CurriculumReservation)r;
					cr.setArea(AcademicAreaDAO.getInstance().get(curriculum.getId(), hibSession));
					if (cr.getMajors() == null)
						cr.setMajors(new HashSet<PosMajor>());
					else
						cr.getMajors().clear();
					for (ReservationInterface.IdName mj: curriculum.getMajors()) {
						cr.getMajors().add(PosMajorDAO.getInstance().get(mj.getId(), hibSession));
					}
					if (cr.getClassifications() == null)
						cr.setClassifications(new HashSet<AcademicClassification>());
					else
						cr.getClassifications().clear();
					for (ReservationInterface.IdName clasf: curriculum.getClassifications()) {
						cr.getClassifications().add(AcademicClassificationDAO.getInstance().get(clasf.getId(), hibSession));
					}
				}
				hibSession.saveOrUpdate(r);
				hibSession.saveOrUpdate(r.getInstructionalOffering());
				hibSession.flush();
				return r.getUniqueId();
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}
	
	@Override
	public Boolean delete(Long reservationId) throws ReservationException {
		try {
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			User user = Web.getUser(getThreadLocalRequest().getSession());
			try {
				Reservation reservation = ReservationDAO.getInstance().get(reservationId, hibSession);
				if (reservation == null)
					return false;
				if (!reservation.isEditableBy(user))
					throw new ReservationException("You are not not authorized to delete reservations for " + reservation.getInstructionalOffering().getCourseName() + ".");
				InstructionalOffering offering = reservation.getInstructionalOffering();
				offering.getReservations().remove(reservation);
				hibSession.delete(reservation);
				hibSession.saveOrUpdate(offering);
				hibSession.flush();
			} finally {
				hibSession.close();
			}
			return true;
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}

	private Long getAcademicSessionId() {
		User user = Web.getUser(getThreadLocalRequest().getSession());
		if (user == null) throw new ReservationException("not authenticated");
		Long sessionId = (Long) user.getAttribute(Constants.SESSION_ID_ATTR_NAME);
		if (sessionId == null) throw new ReservationException("academic session not selected");
		return sessionId;
	}
	
	private TimetableManager getManager() {
		User user = Web.getUser(getThreadLocalRequest().getSession());
		if (user == null) throw new ReservationException("not authenticated");
		if (user.getRole() == null) throw new ReservationException("no user role");
		TimetableManager manager = TimetableManager.getManager(user);
		if (manager == null) throw new ReservationException("access denied");
		return manager;
	}

	@Override
	public Boolean canAddReservation() throws ReservationException {
		try {
			User user = Web.getUser(getThreadLocalRequest().getSession());
			if (user == null) return false;
			if (user.isAdmin()) return true;
			if (!Roles.DEPT_SCHED_MGR_ROLE.equals(user.getRole())) return false;
			if (getManager().getDepartments().isEmpty()) return false;
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			try {
				Session session = SessionDAO.getInstance().get(getAcademicSessionId(), hibSession);
				return session != null && session.getStatusType().canOwnerLimitedEdit();
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}

	@Override
	public List<ReservationInterface> findReservations(String filter) throws ReservationException {
		try {
			List<ReservationInterface> results = new ArrayList<ReservationInterface>();
			Query q = new Query(filter);
			getThreadLocalRequest().getSession().setAttribute("Reservations.LastFilter", filter);
			org.hibernate.Session hibSession = CurriculumDAO.getInstance().getSession();
			getManager();
			User user = Web.getUser(getThreadLocalRequest().getSession());
			String nameFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
			try {
				for (Reservation reservation: (List<Reservation>)hibSession.createQuery(
					"select r from Reservation r where r.instructionalOffering.session.uniqueId = :sessionId")
					.setLong("sessionId", getAcademicSessionId()).setCacheable(true).list()) {
					if (q.match(new ReservationMatcher(reservation))) {
						ReservationInterface r = convert(reservation, nameFormat, hibSession);
						r.setEditable(reservation.isEditableBy(user));
						results.add(r);
					}
				}
			} finally {
				hibSession.close();
			}
			Collections.sort(results);
			return results;
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}

	@Override
	public String lastReservationFilter() throws ReservationException {
		String filter = (String)getThreadLocalRequest().getSession().getAttribute("Reservations.LastFilter");
		if (filter == null) {
			filter = "";
			Long sessionId = getAcademicSessionId();
			for (Iterator<Department> i = getManager().getDepartments().iterator(); i.hasNext(); ) {
				Department d = i.next();
				if (d.getSession().getUniqueId().equals(sessionId)) {
					if (!filter.isEmpty()) filter += " or ";
					filter += "dept:" + d.getDeptCode();
				}
			}
			filter = (filter.isEmpty() ? "not expired" : filter.contains(" or ") ? "(" + filter + ") and not expired" : filter + " and not expired");
		}
		return filter;
	}

	private class ReservationMatcher implements Query.TermMatcher {
		private Reservation iReservation;
		private Date iExpDate;
		
		private ReservationMatcher(Reservation r) {
			iReservation = r;
			Calendar c = Calendar.getInstance(Locale.US);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			iExpDate = c.getTime();
		}
		
		public boolean match(String attr, String term) {
			if (term.isEmpty()) return true;
			if (attr == null || "course".equals(attr)) {
				for (CourseOffering co: iReservation.getInstructionalOffering().getCourseOfferings()) {
					if (eq(co.getCourseName(), term) || has(co.getCourseName(), term)) return true;
				}
			}
			if (attr == null || "dept".equals(attr)) {
				Department d = iReservation.getInstructionalOffering().getDepartment();
				if (eq(d.getDeptCode(), term) || eq(d.getAbbreviation(), term) || has(d.getName(), term)) return true;
			}
			if (attr == null || "subject".equals(attr) || "subj".equals(attr)) {
				for (CourseOffering co: iReservation.getInstructionalOffering().getCourseOfferings()) {
					if (eq(co.getSubjectAreaAbbv(), term) || has(co.getSubjectArea().getShortTitle(), term) || has(co.getSubjectArea().getLongTitle(), term)) return true;
				}
			}
			if (attr == null || "type".equals(attr)) {
				if (iReservation instanceof IndividualReservation && "individual".equals(term)) return true;
				if (iReservation instanceof StudentGroupReservation && "group".equals(term)) return true;
				if (iReservation instanceof CourseReservation && "course".equals(term)) return true;
				if (iReservation instanceof CurriculumReservation && "curriculum".equals(term)) return true;
			}
			if ("group".equals(attr)) {
				if (iReservation instanceof StudentGroupReservation) {
					StudentGroupReservation gr = (StudentGroupReservation)iReservation;
					if (eq(gr.getGroup().getGroupAbbreviation(), term) || has(gr.getGroup().getGroupName(), term)) return true;
				}
			}
			if ("student".equals(attr)) {
				if (iReservation instanceof IndividualReservation) {
					IndividualReservation ir = (IndividualReservation)iReservation;
					for (Student s: ir.getStudents()) {
						if (has(s.getName(DepartmentalInstructor.sNameFormatFirstMiddleLast), term) || eq(s.getExternalUniqueId(), term)) return true;
					}
				}
			}
			if ("area".equals(attr)) {
				if (iReservation instanceof CurriculumReservation) {
					CurriculumReservation cr = (CurriculumReservation)iReservation;
					if (eq(cr.getArea().getAcademicAreaAbbreviation(), term) || has(cr.getArea().getShortTitle(), term) || has(cr.getArea().getLongTitle(), term))
						return true;
				}
			}
			if ("class".equals(attr)) {
				for (Class_ c: iReservation.getClasses()) {
					if (eq(c.getClassLabel(), term) || has(c.getClassLabel(), term) || eq(c.getClassSuffix(), term)) return true;
				}
			}
			if ("config".equals(attr)) {
				for (InstrOfferingConfig c: iReservation.getConfigurations()) {
					if (eq(c.getName(), term) || has(c.getName(), term)) return true;
				}
			}
			if (attr == null && "expired".equalsIgnoreCase(term)) {
				if (iReservation.getExpirationDate() != null && iReservation.getExpirationDate().before(iExpDate)) {
					return true;
				}
			}
			if (attr == null || "expiration".equals(attr) || "exp".equals(attr)) {
				if (iReservation.getExpirationDate() != null && eq(sDateFormat.format(iReservation.getExpirationDate()), term)) return true;
			}
			if ("before".equals(attr)) {
				try {
					Date x = ("today".equalsIgnoreCase(term) ? iExpDate : sDateFormat.parse(term));
					if (iReservation.getExpirationDate() != null && iReservation.getExpirationDate().before(x)) return true;
				} catch (Exception e) {}
			}
			if ("after".equals(attr)) {
				try {
					Date x = ("today".equalsIgnoreCase(term) ? iExpDate : sDateFormat.parse(term));
					if (iReservation.getExpirationDate() == null || iReservation.getExpirationDate().after(x)) return true;
				} catch (Exception e) {}
			}
			return false;
		}
		
		private boolean eq(String name, String term) {
			if (name == null) return false;
			return name.equalsIgnoreCase(term);
		}

		private boolean has(String name, String term) {
			if (name == null) return false;
			for (String t: name.split(" "))
				if (t.equalsIgnoreCase(term)) return true;
			return false;
		}
	
	}

	@Override
	public List<ReservationInterface.Curriculum> getCurricula(Long offeringId) throws ReservationException {
		try {
			List<ReservationInterface.Curriculum> results = new ArrayList<ReservationInterface.Curriculum>();
			org.hibernate.Session hibSession = ReservationDAO.getInstance().getSession();
			try {
				for (Curriculum c : (List<Curriculum>)hibSession.createQuery(
						"select distinct c.classification.curriculum from CurriculumCourse c where c.course.instructionalOffering = :offeringId ")
						.setLong("offeringId", offeringId).setCacheable(true).list()) {

					ReservationInterface.Curriculum curriculum = new ReservationInterface.Curriculum();
					curriculum.setAbbv(c.getAbbv());
					curriculum.setId(c.getUniqueId());
					curriculum.setName(c.getName());
					
					ReservationInterface.IdName area = new ReservationInterface.IdName();
					area.setAbbv(c.getAcademicArea().getAcademicAreaAbbreviation());
					area.setId(c.getAcademicArea().getUniqueId());
					area.setName(Constants.curriculaToInitialCase(c.getAcademicArea().getLongTitle() == null ? c.getAcademicArea().getShortTitle() : c.getAcademicArea().getLongTitle()));
					curriculum.setArea(area);
					
					int limit = 0;
					for (CurriculumClassification cc: c.getClassifications()) {
						AcademicClassification classification = cc.getAcademicClassification();
						ReservationInterface.IdName clasf = new ReservationInterface.IdName();
						clasf.setId(classification.getUniqueId());
						clasf.setName(Constants.curriculaToInitialCase(classification.getName()));
						clasf.setAbbv(classification.getCode());
						clasf.setLimit(0);
						curriculum.getClassifications().add(clasf);
						for (CurriculumCourse cr: cc.getCourses())
							if (cr.getCourse().getInstructionalOffering().getUniqueId().equals(offeringId)) {
								limit += Math.round(cr.getPercShare() * cc.getNrStudents());
								clasf.setLimit(clasf.getLimit() + Math.round(cr.getPercShare() * cc.getNrStudents()));
							}
					}
					curriculum.setLimit(limit);
					Collections.sort(curriculum.getMajors());					
					
					for (PosMajor major: c.getMajors()) {
						ReservationInterface.IdName mj = new ReservationInterface.IdName();
						mj.setId(major.getUniqueId());
						mj.setAbbv(major.getCode());
						mj.setName(Constants.curriculaToInitialCase(major.getName()));
						curriculum.getMajors().add(mj);
					}
					Collections.sort(curriculum.getMajors());					
					
					results.add(curriculum);
				}
			} finally {
				hibSession.close();
			}
			return results;
		} catch (Exception e) {
			if (e instanceof ReservationException) throw (ReservationException)e;
			sLog.error(e.getMessage(), e);
			throw new ReservationException(e.getMessage());
		}
	}
}
