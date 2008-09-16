/*
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime LLC, and individual contributors
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
package org.unitime.timetable.action;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.unitime.commons.MultiComparable;
import org.unitime.commons.User;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.commons.web.Web;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.ExamAssignmentReportForm;
import org.unitime.timetable.model.ClassEvent;
import org.unitime.timetable.model.CourseEvent;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Settings;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.exam.ui.ExamAssignment;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo;
import org.unitime.timetable.solver.exam.ui.ExamInfo;
import org.unitime.timetable.solver.exam.ui.ExamRoomInfo;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.BackToBackConflict;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.DirectConflict;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.DistributionConflict;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.MoreThanTwoADayConflict;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.Parameters;
import org.unitime.timetable.solver.exam.ui.ExamInfo.ExamInstructorInfo;
import org.unitime.timetable.solver.exam.ui.ExamInfo.ExamSectionInfo;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.RoomAvailability;
import org.unitime.timetable.webutil.PdfWebTable;


/** 
 * @author Tomas Muller
 */
public class ExamAssignmentReportAction extends Action {
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
	    ExamAssignmentReportForm myForm = (ExamAssignmentReportForm) form;

        // Check Access
        if (!Web.isLoggedIn( request.getSession() )) {
            throw new Exception ("Access Denied.");
        }
        
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));

        if ("Export PDF".equals(op) || "Apply".equals(op)) {
            myForm.save(request.getSession());
        } else if ("Refresh".equals(op)) {
            myForm.reset(mapping, request);
        }
        
        Session session = Session.getCurrentAcadSession(Web.getUser(request.getSession()));
        RoomAvailability.setAvailabilityWarning(request, session, myForm.getExamType(), true, false);
        
        myForm.load(request.getSession());
        
        ExamSolverProxy solver = WebSolver.getExamSolver(request.getSession());
        Collection<ExamAssignmentInfo> assignedExams = null;
        if (myForm.getSubjectArea()!=null && myForm.getSubjectArea()!=0) {
            if (solver!=null && solver.getExamType()==myForm.getExamType())
                assignedExams = solver.getAssignedExams(myForm.getSubjectArea());
            else {
                if ("true".equals(ApplicationProperties.getProperty("tmtbl.exams.conflicts.cache","true")))
                    assignedExams = Exam.findAssignedExams(session.getUniqueId(),myForm.getSubjectArea(),myForm.getExamType());
                else
                    assignedExams = findAssignedExams(session.getUniqueId(),myForm.getSubjectArea(),myForm.getExamType());
            }
        }
        
        WebTable.setOrder(request.getSession(),"examAssignmentReport["+myForm.getReport()+"].ord",request.getParameter("ord"),1);
        
        WebTable table = getTable(session.getUniqueId(), Web.getUser(request.getSession()), true, myForm, assignedExams);
        
        if ("Export PDF".equals(op) && table!=null) {
            PdfWebTable pdfTable = getTable(session.getUniqueId(), Web.getUser(request.getSession()), false, myForm, assignedExams);
            File file = ApplicationProperties.getTempFile("xreport", "pdf");
            pdfTable.exportPdf(file, WebTable.getOrder(request.getSession(),"examAssignmentReport["+myForm.getReport()+"].ord"));
        	request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
        }
        
        if ("Export CSV".equals(op) && table!=null) {
            WebTable csvTable = getTable(session.getUniqueId(), Web.getUser(request.getSession()), false, myForm, assignedExams);
            File file = ApplicationProperties.getTempFile("xreport", "csv");
            csvTable.toCSVFile(WebTable.getOrder(request.getSession(),"examAssignmentReport["+myForm.getReport()+"].ord")).save(file);
            request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
        }

        if (table!=null)
            myForm.setTable(table.printTable(WebTable.getOrder(request.getSession(),"examAssignmentReport["+myForm.getReport()+"].ord")), table.getNrColumns(), assignedExams.size());
		
        if (request.getParameter("backId")!=null)
            request.setAttribute("hash", request.getParameter("backId"));

        return mapping.findForward("show");
	}
	
    public static TreeSet<ExamAssignmentInfo> findAssignedExams(Long sessionId, Long subjectAreaId, int examType) throws Exception {
        Hashtable<Long, Exam> exams = new Hashtable();
        for (Iterator i=new ExamDAO().getSession().createQuery(
                "select x from Exam x where x.session.uniqueId=:sessionId and x.examType=:examType"
                ).setLong("sessionId", sessionId).setInteger("examType", examType).setCacheable(true).list().iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            exams.put(exam.getUniqueId(), exam);
        }
        new ExamDAO().getSession().createQuery(
                "select c from Class_ c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:classType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("classType", ExamOwner.sOwnerTypeClass).setCacheable(true).list();
        new ExamDAO().getSession().createQuery(
                "select c from InstrOfferingConfig c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:configType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("configType", ExamOwner.sOwnerTypeConfig).setCacheable(true).list();
        new ExamDAO().getSession().createQuery(
                "select c from CourseOffering c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:courseType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("courseType", ExamOwner.sOwnerTypeCourse).setCacheable(true).list();
        new ExamDAO().getSession().createQuery(
                "select c from InstructionalOffering c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:offeringType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("offeringType", ExamOwner.sOwnerTypeOffering).setCacheable(true).list();
        Hashtable<Long,Set<Long>> owner2students = new Hashtable();
        Hashtable<Long,Set<Exam>> student2exams = new Hashtable();
            for (Iterator i=
                new ExamDAO().getSession().createQuery(
                "select x.uniqueId, o.uniqueId, e.student.uniqueId from "+
                "Exam x inner join x.owners o, "+
                "StudentClassEnrollment e inner join e.clazz c "+
                "where x.session.uniqueId=:sessionId and x.examType=:examType and "+
                "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeClass+" and "+
                "o.ownerId=c.uniqueId").setLong("sessionId", sessionId).setInteger("examType", examType).setCacheable(true).list().iterator();i.hasNext();) {
                    Object[] o = (Object[])i.next();
                    Long examId = (Long)o[0];
                    Long ownerId = (Long)o[1];
                    Long studentId = (Long)o[2];
                    Set<Long> studentsOfOwner = owner2students.get(ownerId);
                    if (studentsOfOwner==null) {
                        studentsOfOwner = new HashSet<Long>();
                        owner2students.put(ownerId, studentsOfOwner);
                    }
                    studentsOfOwner.add(studentId);
                    Set<Exam> examsOfStudent = student2exams.get(studentId);
                    if (examsOfStudent==null) { 
                        examsOfStudent = new HashSet<Exam>();
                        student2exams.put(studentId, examsOfStudent);
                    }
                    examsOfStudent.add(exams.get(examId));
                }
            for (Iterator i=
                new ExamDAO().getSession().createQuery(
                        "select x.uniqueId, o.uniqueId, e.student.uniqueId from "+
                        "Exam x inner join x.owners o, "+
                        "StudentClassEnrollment e inner join e.clazz c " +
                        "inner join c.schedulingSubpart.instrOfferingConfig ioc " +
                        "where x.session.uniqueId=:sessionId and x.examType=:examType and "+
                        "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeConfig+" and "+
                        "o.ownerId=ioc.uniqueId").setLong("sessionId", sessionId).setInteger("examType", examType).setCacheable(true).list().iterator();i.hasNext();) {
                Object[] o = (Object[])i.next();
                Long examId = (Long)o[0];
                Long ownerId = (Long)o[1];
                Long studentId = (Long)o[2];
                Set<Long> studentsOfOwner = owner2students.get(ownerId);
                if (studentsOfOwner==null) {
                    studentsOfOwner = new HashSet<Long>();
                    owner2students.put(ownerId, studentsOfOwner);
                }
                studentsOfOwner.add(studentId);
                Set<Exam> examsOfStudent = student2exams.get(studentId);
                if (examsOfStudent==null) { 
                    examsOfStudent = new HashSet<Exam>();
                    student2exams.put(studentId, examsOfStudent);
                }
                examsOfStudent.add(exams.get(examId));
            }
            for (Iterator i=
                new ExamDAO().getSession().createQuery(
                        "select x.uniqueId, o.uniqueId, e.student.uniqueId from "+
                        "Exam x inner join x.owners o, "+
                        "StudentClassEnrollment e inner join e.courseOffering co " +
                        "where x.session.uniqueId=:sessionId and x.examType=:examType and "+
                        "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeCourse+" and "+
                        "o.ownerId=co.uniqueId").setLong("sessionId", sessionId).setInteger("examType", examType).setCacheable(true).list().iterator();i.hasNext();) {
                Object[] o = (Object[])i.next();
                Long examId = (Long)o[0];
                Long ownerId = (Long)o[1];
                Long studentId = (Long)o[2];
                Set<Long> studentsOfOwner = owner2students.get(ownerId);
                if (studentsOfOwner==null) {
                    studentsOfOwner = new HashSet<Long>();
                    owner2students.put(ownerId, studentsOfOwner);
                }
                studentsOfOwner.add(studentId);
                Set<Exam> examsOfStudent = student2exams.get(studentId);
                if (examsOfStudent==null) { 
                    examsOfStudent = new HashSet<Exam>();
                    student2exams.put(studentId, examsOfStudent);
                }
                examsOfStudent.add(exams.get(examId));
            }
            for (Iterator i=
                new ExamDAO().getSession().createQuery(
                        "select x.uniqueId, o.uniqueId, e.student.uniqueId from "+
                        "Exam x inner join x.owners o, "+
                        "StudentClassEnrollment e inner join e.courseOffering.instructionalOffering io " +
                        "where x.session.uniqueId=:sessionId and x.examType=:examType and "+
                        "o.ownerType="+org.unitime.timetable.model.ExamOwner.sOwnerTypeOffering+" and "+
                        "o.ownerId=io.uniqueId").setLong("sessionId", sessionId).setInteger("examType", examType).setCacheable(true).list().iterator();i.hasNext();) {
                Object[] o = (Object[])i.next();
                Long examId = (Long)o[0];
                Long ownerId = (Long)o[1];
                Long studentId = (Long)o[2];
                Set<Long> studentsOfOwner = owner2students.get(ownerId);
                if (studentsOfOwner==null) {
                    studentsOfOwner = new HashSet<Long>();
                    owner2students.put(ownerId, studentsOfOwner);
                }
                studentsOfOwner.add(studentId);
                Set<Exam> examsOfStudent = student2exams.get(studentId);
                if (examsOfStudent==null) { 
                    examsOfStudent = new HashSet<Exam>();
                    student2exams.put(studentId, examsOfStudent);
                }
                examsOfStudent.add(exams.get(examId));
            }
            Hashtable<Long, Set<Meeting>> period2meetings = new Hashtable();
            for (Iterator i=new ExamDAO().getSession().createQuery(
                    "select p.uniqueId, ce, m from ClassEvent ce inner join ce.meetings m, ExamPeriod p " +
                    "where p.startSlot - :travelTime < m.stopPeriod and m.startPeriod < p.startSlot + p.length + :travelTime and "+
                    HibernateUtil.addDate("p.session.examBeginDate","p.dateOffset")+" = m.meetingDate and p.session.uniqueId=:sessionId and p.examType=:examType")
                    .setInteger("travelTime", Integer.parseInt(ApplicationProperties.getProperty("tmtbl.exam.eventConflicts.travelTime.classEvent","6")))
                    .setLong("sessionId", sessionId).setInteger("examType", examType)
                    .setCacheable(true).iterate(); i.hasNext();) {
                Object[] o = (Object[])i.next();
                Long periodId = (Long)o[0];
                ClassEvent event = (ClassEvent)o[1];
                Meeting meeting = (Meeting)o[2];
                Set<Meeting> meetings  = period2meetings.get(periodId);
                if (meetings==null) {
                    meetings = new HashSet(); period2meetings.put(periodId, meetings);
                }
                meetings.add(meeting);
            }
            for (Iterator i=new ExamDAO().getSession().createQuery(
                    "select p.uniqueId, ce, m from CourseEvent ce inner join ce.meetings m, ExamPeriod p " +
                    "where ce.reqAttendance=true and p.startSlot - :travelTime < m.stopPeriod and m.startPeriod < p.startSlot + p.length + :travelTime and "+
                    HibernateUtil.addDate("p.session.examBeginDate","p.dateOffset")+" = m.meetingDate and p.session.uniqueId=:sessionId and p.examType=:examType")
                    .setInteger("travelTime", Integer.parseInt(ApplicationProperties.getProperty("tmtbl.exam.eventConflicts.travelTime.courseEvent","0")))
                    .setLong("sessionId", sessionId).setInteger("examType", examType)
                    .setCacheable(true).iterate(); i.hasNext();) {
                Object[] o = (Object[])i.next();
                Long periodId = (Long)o[0];
                CourseEvent event = (CourseEvent)o[1];
                Meeting meeting = (Meeting)o[2];
                Set<Meeting> meetings  = period2meetings.get(periodId);
                if (meetings==null) {
                    meetings = new HashSet(); period2meetings.put(periodId, meetings);
                }
                meetings.add(meeting);
            }
            Parameters p = new Parameters(sessionId, examType);
        TreeSet<ExamAssignmentInfo> ret = new TreeSet();
        if (subjectAreaId==null || subjectAreaId<0) {
            for (Iterator i = new ExamDAO().getSession().createQuery(
                    "select x from Exam x where " +
                    "x.examType=:examType and "+
                    "x.session.uniqueId=:sessionId and x.assignedPeriod!=null").
                    setLong("sessionId", sessionId).
                    setLong("examType", examType).
                    setCacheable(true).list().iterator();i.hasNext();) {
                Exam exam = (Exam)i.next();
                ExamAssignmentInfo info = new ExamAssignmentInfo(exam, owner2students, student2exams, period2meetings, p);
                ret.add(info);
            }
        } else {
            for (Iterator i = new ExamDAO().getSession().createQuery(
                    "select distinct x from Exam x inner join x.owners o where " +
                    "o.course.subjectArea.uniqueId=:subjectAreaId and "+
                    "x.examType=:examType and "+
                    "x.session.uniqueId=:sessionId and x.assignedPeriod!=null").
                    setLong("sessionId", sessionId).
                    setLong("examType", examType).
                    setLong("subjectAreaId", subjectAreaId).
                    setCacheable(true).list().iterator();i.hasNext();) {
                Exam exam = (Exam)i.next();
                ExamAssignmentInfo info = new ExamAssignmentInfo(exam, owner2students, student2exams, period2meetings, p);
                ret.add(info);
            }
        }
        return ret;
    }
	
	public boolean match(ExamAssignmentReportForm form, String name) {
	    if (form.getFilter()==null || form.getFilter().trim().length()==0) return true;
	    String n = (name==null?"":name).toUpperCase();
        StringTokenizer stk1 = new StringTokenizer(form.getFilter().toUpperCase(),";");
        while (stk1.hasMoreTokens()) {
            StringTokenizer stk2 = new StringTokenizer(stk1.nextToken()," ,");
            boolean match = true;
            while (match && stk2.hasMoreTokens()) {
                String token = stk2.nextToken().trim();
                if (token.length()==0) continue;
                if (token.indexOf('*')>=0 || token.indexOf('?')>=0) {
                    try {
                        String tokenRegExp = "\\s+"+token.replaceAll("\\.", "\\.").replaceAll("\\?", ".+").replaceAll("\\*", ".*")+"\\s";
                        if (!Pattern.compile(tokenRegExp).matcher(" "+n+" ").find()) match = false;
                    } catch (PatternSyntaxException e) { match = false; }
                } else if (n.indexOf(token)<0) match = false;
            }
            if (match) return true;
        }
        return false;
	}
	
	public boolean match(ExamAssignmentReportForm form, ExamAssignment exam) {
	    if (exam==null) return false;
	    if (form.getShowSections()) {
	        for (ExamSectionInfo section : exam.getSections())
	            if (match(form, section.getName())) return true;
	        return false;
	    } else {
	        return match(form, exam.getExamName());
	    }
	}
	
	public PdfWebTable getTable(Long sessionId, User user, boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams) {
        if (exams==null || exams.isEmpty()) return null;
		if (ExamAssignmentReportForm.sExamAssignmentReport.equals(form.getReport())) {
		    return generateAssignmentReport(html, form, exams);
		}  else if (ExamAssignmentReportForm.sRoomAssignmentReport.equals(form.getReport())) {
		    return generateRoomReport(html, form, exams);
		}  else if (ExamAssignmentReportForm.sPeriodUsage.equals(form.getReport())) {
            return generatePeriodUsageReport(html, form, exams, sessionId);
		}  else if (ExamAssignmentReportForm.sNrExamsADay.equals(form.getReport())) {
		    return generateNrExamsADayReport(html, form, exams);
		} else if (ExamAssignmentReportForm.sRoomSplits.equals(form.getReport())) {
		    return generateRoomSplitReport(html, form, exams);
		} else if (ExamAssignmentReportForm.sViolatedDistributions.equals(form.getReport())) {
		    return generateViolatedDistributionsReport(html, form, exams);
        } else if (ExamAssignmentReportForm.sIndividualStudentConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, true, true, true, true, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualDirectStudentConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, true, true, false, false, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualBackToBackStudentConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, true, false, false, true, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualMore2ADayStudentConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, true, false, true, false, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualInstructorConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, false, true, true, true, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualInstructorConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, false, true, false, false, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualBackToBackInstructorConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, false, false, false, true, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualMore2ADayInstructorConflicts.equals(form.getReport())) {
            return generateIndividualConflictsReport(html, sessionId, form, exams, false, false, true, false, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sDirectStudentConflicts.equals(form.getReport())) {
            return generateDirectConflictsReport(html, form, exams, true);
        } else if (ExamAssignmentReportForm.sBackToBackStudentConflicts.equals(form.getReport())) {
            return generateBackToBackConflictsReport(html, form, exams, true);
        } else if (ExamAssignmentReportForm.sMore2ADayStudentConflicts.equals(form.getReport())) {
            return generate2MoreADayConflictsReport(html, form, exams, true);
        } else if (ExamAssignmentReportForm.sDirectInstructorConflicts.equals(form.getReport())) {
            return generateDirectConflictsReport(html, form, exams, false);
        } else if (ExamAssignmentReportForm.sBackToBackInstructorConflicts.equals(form.getReport())) {
            return generateBackToBackConflictsReport(html, form, exams, false);
        } else if (ExamAssignmentReportForm.sMore2ADayInstructorConflicts.equals(form.getReport())) {
            return generate2MoreADayConflictsReport(html, form, exams, false);
        } else if (ExamAssignmentReportForm.sIndividualStudentSchedule.equals(form.getReport())) {
            return generateIndividualAssignmentReport(html, sessionId, form, exams, true, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else if (ExamAssignmentReportForm.sIndividualInstructorSchedule.equals(form.getReport())) {
            return generateIndividualAssignmentReport(html, sessionId, form, exams, false, Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT));
        } else  return null;
    }
	
	private PdfWebTable generateAssignmentReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams) {
	    String nl = (html?"<br>":"\n");
        PdfWebTable table = new PdfWebTable( 10,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    (form.getShowSections()?"Class / Course":"Examination"),
                    "Enrollment",
                    "Seating"+nl+"Type",
                    "Date",
                    "Time",
                    "Room",
                    "Capacity",
                    "Instructor",
                    "Student"+nl+"Conflicts",
                    "Instructor"+nl+"Conflicts"},
                new String[] {"left", "right", "center", "left", "left", "left", "right", "left", "center", "center"},
                new boolean[] {true, true, true, true, true, true, true, true, true, true} );
        table.setRowStyle("white-space:nowrap");
        for (ExamAssignmentInfo exam : exams) {
            if (form.getShowSections()) {
                boolean firstSection = true; 
                for (ExamSectionInfo section : exam.getSections()) {
                    if (!match(form, section.getName())) continue;
                    int idc = exam.getNrInstructorDirectConflicts(section);
                    String idcStr = (idc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+idc+"</font>":String.valueOf(idc));
                    int im2d = exam.getNrInstructorMoreThanTwoConflicts(section);
                    String im2dStr = (im2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+im2d+"</font>":String.valueOf(im2d));
                    int ibtb = exam.getNrInstructorBackToBackConflicts(section);
                    int idbtb = exam.getNrInstructorDistanceBackToBackConflicts(section);
                    String ibtbStr = (ibtb<=0 && idbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+ibtb+(idbtb>0?" (d:"+idbtb+")":"")+"</font>":ibtb+(idbtb>0?" (d:"+idbtb+")":""));

                    int dc = exam.getNrDirectConflicts(section);
                    String dcStr = (dc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+dc+"</font>":String.valueOf(dc));
                    int m2d = exam.getNrMoreThanTwoConflicts(section);
                    String m2dStr = (m2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+m2d+"</font>":String.valueOf(m2d));
                    int btb = exam.getNrBackToBackConflicts(section);
                    int dbtb = exam.getNrDistanceBackToBackConflicts(section);
                    String btbStr = (btb<=0 && dbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+btb+(dbtb>0?" (d:"+dbtb+")":"")+"</font>":btb+(dbtb>0?" (d:"+dbtb+")":""));

                    table.addLine(
                            "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                            new String[] {
                                (html?"<a name='"+exam.getExamId()+"'>":"")+section.getName()+(html?"</a>":""),
                                String.valueOf(section.getNrStudents()),
                                exam.getSeatingTypeLabel(),
                                exam.getDate(html),
                                exam.getTime(html),
                                exam.getRoomsName(html,", "),
                                exam.getRoomsCapacity(html, ", "),
                                exam.getInstructorName("; "),
                                (dc==0&&m2d==0&&btb==0&&dbtb==0?"":dcStr+", "+m2dStr+", "+btbStr),
                                (idc==0&&im2d==0&&ibtb==0&&idbtb==0?"":idcStr+", "+im2dStr+", "+ibtbStr),
                            },
                            new Comparable[] {
                                new MultiComparable(section.getName(), exam),
                                new MultiComparable(-section.getNrStudents(), section.getName(), exam),
                                new MultiComparable(exam.getSeatingType(), section.getName(), exam),
                                new MultiComparable(exam.getPeriodOrd(), section.getName(), exam),
                                new MultiComparable(exam.getPeriod()==null?-1:exam.getPeriod().getStartSlot(), section.getName(), exam),
                                new MultiComparable(exam.getRoomsName(":"), section.getName(), exam),
                                new MultiComparable(-exam.getRoomsCapacity(), section.getName(), exam),
                                new MultiComparable(exam.getInstructorName(":"), section.getName(), exam),
                                new MultiComparable(-dc,-m2d,-btb,-dbtb,section.getName(),exam),
                                new MultiComparable(-idc,-im2d,-ibtb,-idbtb,section.getName(),exam)
                            },
                            (firstSection?exam.getExamId().toString():null));
                    firstSection = false;
                }
            } else {
                if (!match(form, exam.getExamName())) continue;
                int idc = exam.getNrInstructorDirectConflicts();
                String idcStr = (idc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+idc+"</font>":String.valueOf(idc));
                int im2d = exam.getNrInstructorMoreThanTwoConflicts();
                String im2dStr = (im2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+im2d+"</font>":String.valueOf(im2d));
                int ibtb = exam.getNrInstructorBackToBackConflicts();
                int idbtb = exam.getNrInstructorDistanceBackToBackConflicts();
                String ibtbStr = (ibtb<=0 && idbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+ibtb+(idbtb>0?" (d:"+idbtb+")":"")+"</font>":ibtb+(idbtb>0?" (d:"+idbtb+")":""));

                int dc = exam.getNrDirectConflicts();
                String dcStr = (dc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+dc+"</font>":String.valueOf(dc));
                int m2d = exam.getNrMoreThanTwoConflicts();
                String m2dStr = (m2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+m2d+"</font>":String.valueOf(m2d));
                int btb = exam.getNrBackToBackConflicts();
                int dbtb = exam.getNrDistanceBackToBackConflicts();
                String btbStr = (btb<=0 && dbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+btb+(dbtb>0?" (d:"+dbtb+")":"")+"</font>":btb+(dbtb>0?" (d:"+dbtb+")":""));
                
                table.addLine(
                        "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                        new String[] {
                            (html?"<a name='"+exam.getExamId()+"'>":"")+exam.getExamName()+(html?"</a>":""),
                            String.valueOf(exam.getNrStudents()),
                            exam.getSeatingTypeLabel(),
                            exam.getDate(html),
                            exam.getTime(html),
                            exam.getRoomsName(html,", "),
                            exam.getRoomsCapacity(html, ", "),
                            exam.getInstructorName("; "),
                            (dc==0&&m2d==0&&btb==0&&dbtb==0?"":dcStr+", "+m2dStr+", "+btbStr),
                            (idc==0&&im2d==0&&ibtb==0&&idbtb==0?"":idcStr+", "+im2dStr+", "+ibtbStr),
                        },
                        new Comparable[] {
                            exam,
                            new MultiComparable(exam.getNrStudents(), exam),
                            new MultiComparable(exam.getSeatingType(), exam),
                            new MultiComparable(exam.getPeriodOrd(), exam),
                            new MultiComparable(exam.getPeriod().getStartSlot(), exam),
                            new MultiComparable(exam.getRoomsName(":"), exam),
                            new MultiComparable(-exam.getRoomsCapacity(), exam),
                            new MultiComparable(exam.getInstructorName(":"), exam),
                            new MultiComparable(-dc,-m2d,-btb,-dbtb,exam),
                            new MultiComparable(-idc,-im2d,-ibtb,-idbtb,exam)
                        },
                        exam.getExamId().toString());
            }
        }
        return table;	    
	}
	
	private PdfWebTable generateRoomReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams) {
        String nl = (html?"<br>":"\n");
        PdfWebTable table = new PdfWebTable( 11,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    "Room",
                    "Capacity",
                    "Exam"+nl+"Capacity",
                    "Date",
                    "Time",
                    (form.getShowSections()?"Class / Course":"Examination"),
                    "Enrollment",
                    "Seating"+nl+"Type",
                    "Instructor",
                    "Student"+nl+"Conflicts",
                    "Instructor"+nl+"Conflicts"},
                new String[] {"left", "right", "right", "left", "left", "left", "right", "center", "left", "center", "center"},
                new boolean[] {true, true, true, true, true, true, true, true, true, true, true} );
        table.setRowStyle("white-space:nowrap");
        table.setBlankWhenSame(true);
        for (ExamAssignmentInfo exam : exams) {
            boolean match = false;
            for (ExamRoomInfo room : exam.getRooms()) {
                if (match(form,room.getName())) { match = true; break; }
            }
            if (!match) continue;
            if (form.getShowSections()) {
                for (ExamSectionInfo section : exam.getSections()) {
                    int idc = exam.getNrInstructorDirectConflicts(section);
                    String idcStr = (idc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+idc+"</font>":String.valueOf(idc));
                    int im2d = exam.getNrInstructorMoreThanTwoConflicts(section);
                    String im2dStr = (im2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+im2d+"</font>":String.valueOf(im2d));
                    int ibtb = exam.getNrInstructorBackToBackConflicts(section);
                    int idbtb = exam.getNrInstructorDistanceBackToBackConflicts(section);
                    String ibtbStr = (ibtb<=0 && idbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+ibtb+(idbtb>0?" (d:"+idbtb+")":"")+"</font>":ibtb+(idbtb>0?" (d:"+idbtb+")":""));

                    int dc = exam.getNrDirectConflicts(section);
                    String dcStr = (dc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+dc+"</font>":String.valueOf(dc));
                    int m2d = exam.getNrMoreThanTwoConflicts(section);
                    String m2dStr = (m2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+m2d+"</font>":String.valueOf(m2d));
                    int btb = exam.getNrBackToBackConflicts(section);
                    int dbtb = exam.getNrDistanceBackToBackConflicts(section);
                    String btbStr = (btb<=0 && dbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+btb+(dbtb>0?" (d:"+dbtb+")":"")+"</font>":btb+(dbtb>0?" (d:"+dbtb+")":""));
                    
                    boolean firstRoom = true;
                    for (ExamRoomInfo room : exam.getRooms()) {
                        if (!match(form,room.getName())) continue;
                        table.addLine(
                                "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                new String[] {
                                    room.getName(),
                                    String.valueOf(room.getCapacity()),
                                    String.valueOf(room.getExamCapacity()),
                                    exam.getDate(html),
                                    exam.getTime(html),
                                    (html?"<a name='"+exam.getExamId()+"'>":"")+section.getName()+(html?"</a>":""),
                                    String.valueOf(section.getNrStudents()),
                                    exam.getSeatingTypeLabel(),
                                    exam.getInstructorName("; "),
                                    (dc==0&&m2d==0&&btb==0&&dbtb==0?"":dcStr+", "+m2dStr+", "+btbStr),
                                    (idc==0&&im2d==0&&ibtb==0&&idbtb==0?"":idcStr+", "+im2dStr+", "+ibtbStr),
                                },
                                new Comparable[] {
                                    new MultiComparable(room.getName()),
                                    new MultiComparable(-room.getCapacity(), room.getName()),
                                    new MultiComparable(-room.getExamCapacity(), room.getName()),
                                    new MultiComparable(room.getName(), exam.getPeriodOrd(), section.getName(), exam),
                                    new MultiComparable(room.getName(), exam.getPeriod().getStartSlot(), section.getName(), exam),
                                    new MultiComparable(room.getName(), section.getName(), exam),
                                    new MultiComparable(room.getName(), -section.getNrStudents(), section.getName(), exam),
                                    new MultiComparable(room.getName(), exam.getSeatingType(), section.getName(), exam),
                                    new MultiComparable(room.getName(), exam.getInstructorName(":"), section.getName(), exam),
                                    new MultiComparable(room.getName(), -dc,-m2d,-btb,-dbtb,section.getName(),exam),
                                    new MultiComparable(room.getName(), -idc,-im2d,-ibtb,-idbtb,section.getName(),exam)
                                },
                                (firstRoom?exam.getExamId().toString():null));
                        firstRoom = false;
                    }
                }
            } else {
                int idc = exam.getNrInstructorDirectConflicts();
                String idcStr = (idc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+idc+"</font>":String.valueOf(idc));
                int im2d = exam.getNrInstructorMoreThanTwoConflicts();
                String im2dStr = (im2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+im2d+"</font>":String.valueOf(im2d));
                int ibtb = exam.getNrInstructorBackToBackConflicts();
                int idbtb = exam.getNrInstructorDistanceBackToBackConflicts();
                String ibtbStr = (ibtb<=0 && idbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+ibtb+(idbtb>0?" (d:"+idbtb+")":"")+"</font>":ibtb+(idbtb>0?" (d:"+idbtb+")":""));

                int dc = exam.getNrDirectConflicts();
                String dcStr = (dc<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>"+dc+"</font>":String.valueOf(dc));
                int m2d = exam.getNrMoreThanTwoConflicts();
                String m2dStr = (m2d<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>"+m2d+"</font>":String.valueOf(m2d));
                int btb = exam.getNrBackToBackConflicts();
                int dbtb = exam.getNrDistanceBackToBackConflicts();
                String btbStr = (btb<=0 && dbtb<=0?"0":html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>"+btb+(dbtb>0?" (d:"+dbtb+")":"")+"</font>":btb+(dbtb>0?" (d:"+dbtb+")":""));
                
                boolean firstRoom = true;
                for (ExamRoomInfo room : exam.getRooms()) {
                    if (!match(form,room.getName())) continue;
                    table.addLine(
                            "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                            new String[] {
                                room.getName(),
                                String.valueOf(room.getCapacity()),
                                String.valueOf(room.getExamCapacity()),
                                exam.getDate(html),
                                exam.getTime(html),
                                (html?"<a name='"+exam.getExamId()+"'>":"")+exam.getExamName()+(html?"</a>":""),
                                String.valueOf(exam.getNrStudents()),
                                exam.getSeatingTypeLabel(),
                                exam.getInstructorName("; "),
                                (dc==0&&m2d==0&&btb==0&&dbtb==0?"":dcStr+", "+m2dStr+", "+btbStr),
                                (idc==0&&im2d==0&&ibtb==0&&idbtb==0?"":idcStr+", "+im2dStr+", "+ibtbStr),
                            },
                            new Comparable[] {
                                new MultiComparable(room.getName()),
                                new MultiComparable(-room.getCapacity(), room.getName()),
                                new MultiComparable(-room.getExamCapacity(), room.getName()),
                                new MultiComparable(room.getName(), exam.getPeriodOrd(), exam),
                                new MultiComparable(room.getName(), exam.getPeriod().getStartSlot(), exam),
                                new MultiComparable(room.getName(), exam),
                                new MultiComparable(room.getName(), -exam.getNrStudents(), exam),
                                new MultiComparable(room.getName(), exam.getSeatingType(), exam),
                                new MultiComparable(room.getName(), exam.getInstructorName(":"), exam),
                                new MultiComparable(room.getName(), -dc,-m2d,-btb,-dbtb,exam),
                                new MultiComparable(room.getName(), -idc,-im2d,-ibtb,-idbtb,exam)
                            },
                            (firstRoom?exam.getExamId().toString():null));
                    firstRoom = false;
                }
            }
        }
        return table;	    
	}
	
	private PdfWebTable generatePeriodUsageReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams, Long sessionId) {
	    String nl = (html?"<br>":"\n");
	    PdfWebTable table = new PdfWebTable( 7,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    "Date",
                    "Time",
                    (form.getShowSections()?"Classes / Courses":"Examinations"),
                    "Size",
                    (form.getShowSections()?"Classes / Courses":"Examinations")+nl+"with 10+ students",
                    (form.getShowSections()?"Classes / Courses":"Examinations")+nl+"with 50+ students",
                    (form.getShowSections()?"Classes / Courses":"Examinations")+nl+"with 100+ students"},
                new String[] {"left","left","right","right","right","right","right"},
                new boolean[] {true, true, true, true, true, true, true} );
        table.setRowStyle("white-space:nowrap");
        int tnrExams = 0, tnrStudents = 0, tnrExams10=0, tnrExams50=0, tnrExams100=0;
        for (Iterator i=ExamPeriod.findAll(sessionId, form.getExamType()).iterator();i.hasNext();) {
            ExamPeriod period = (ExamPeriod)i.next();
            String periodDate = period.getStartDateLabel();
            String periodTime = period.getStartTimeLabel()+" - "+period.getEndTimeLabel();
            if (html && period.getPrefLevel()!=null && !PreferenceLevel.sNeutral.equals(period.getPrefLevel().getPrefProlog())) {
                periodDate = "<font color='"+PreferenceLevel.prolog2color(period.getPrefLevel().getPrefProlog())+"'>"+periodDate+"</font>";
                periodTime = "<font color='"+PreferenceLevel.prolog2color(period.getPrefLevel().getPrefProlog())+"'>"+periodTime+"</font>";
            }
            int nrExams = 0, nrStudents = 0, nrExams10=0, nrExams50=0, nrExams100=0;
            for (ExamAssignmentInfo exam : exams) {
                if (!period.getUniqueId().equals(exam.getPeriodId())) continue;
                if (form.getShowSections()) {
                    for (ExamSectionInfo section : exam.getSections()) {
                        if (!match(form,section.getName())) continue;
                        nrExams++;
                        nrStudents+=section.getNrStudents();
                        if (section.getNrStudents()>10) nrExams10++;
                        if (section.getNrStudents()>50) nrExams50++;
                        if (section.getNrStudents()>100) nrExams100++;
                    }
                } else {
                    if (!match(form,exam.getExamName())) continue;
                    nrExams++;
                    nrStudents+=exam.getNrStudents();
                    if (exam.getNrStudents()>10) nrExams10++;
                    if (exam.getNrStudents()>50) nrExams50++;
                    if (exam.getNrStudents()>100) nrExams100++;
                }
            }
            if (nrExams==0) continue;
            table.addLine(
                    new String[] {
                            periodDate,
                            periodTime,
                            String.valueOf(nrExams),
                            String.valueOf(nrStudents),
                            String.valueOf(nrExams10),
                            String.valueOf(nrExams50),
                            String.valueOf(nrExams100)},
                    new Comparable[] {
                            new MultiComparable(0,period),
                            new MultiComparable(0,period.getStartSlot(), period.getDateOffset(), period),
                            new MultiComparable(0,nrExams),
                            new MultiComparable(0,nrStudents),
                            new MultiComparable(0,nrExams10),
                            new MultiComparable(0,nrExams50),
                            new MultiComparable(0,nrExams100)
                    });
            tnrExams += nrExams;
            tnrExams10 += nrExams10;
            tnrExams50 += nrExams50;
            tnrExams100 += nrExams100;
            tnrStudents += nrStudents;
        }
        table.addLine(
                new String[] {
                        (html?"<b>Totals</b>":"Totals"),
                        "",
                        (html?"<b>"+tnrExams+"</b>":String.valueOf(tnrExams)),
                        (html?"<b>"+tnrStudents+"</b>":String.valueOf(tnrStudents)),
                        (html?"<b>"+tnrExams10+"</b>":String.valueOf(tnrExams10)),
                        (html?"<b>"+tnrExams50+"</b>":String.valueOf(tnrExams50)),
                        (html?"<b>"+tnrExams100+"</b>":String.valueOf(tnrExams100))},
                new Comparable[] {
                        new MultiComparable(1,null),
                        new MultiComparable(1,0,0, null),
                        new MultiComparable(1,tnrExams),
                        new MultiComparable(1,tnrStudents),
                        new MultiComparable(1,tnrExams10),
                        new MultiComparable(1,tnrExams50),
                        new MultiComparable(1,tnrExams100)
                });
        return table;	    
	}
	
	private PdfWebTable generateNrExamsADayReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams) {
        SimpleDateFormat df = new SimpleDateFormat("EEE, MM/dd");
        String nl = (html?"<br>":"\n");
        PdfWebTable table = new PdfWebTable( 8,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    "Date",
                    "Students with"+nl+"No Exam",
                    "Students with"+nl+"One Exam",
                    "Students with"+nl+"Two Exams",
                    "Students with"+nl+"Three Exams",
                    "Students with"+nl+"Four or More Exams",
                    "Student "+nl+"Back-To-Back Exams",
                    "Student Distance"+nl+"Back-To-Back Exams"},
                new String[] {"left", "right", "right", "right", "right", "right", "right", "right", "right"},
                new boolean[] {true, true, true, true, true, true, true, true} );
        table.setRowStyle("white-space:nowrap");
        HashSet<Long> studentIds = new HashSet<Long>();
        Hashtable<Date,Hashtable<Long,Integer>> date2students = new Hashtable();
        Hashtable<Date,Integer> date2btb = new Hashtable();
        Hashtable<Date,Integer> date2dbtb = new Hashtable();
        for (ExamAssignmentInfo exam : exams) {
            if (!form.getShowSections() && !match(form,exam.getExamName())) continue;
            Hashtable<Long,Integer> students = date2students.get(exam.getPeriod().getStartDate());
            if (students==null) {
                students = new Hashtable<Long, Integer>(); date2students.put(exam.getPeriod().getStartDate(),students);
            }
            for (ExamSectionInfo section : exam.getSections()) {
                if (form.getShowSections() && !match(form,section.getName())) continue;
                studentIds.addAll(section.getStudentIds());
                for (Long studentId : section.getStudentIds()) {
                    Integer nrExamsThisDay = students.get(studentId);
                    students.put(studentId, 1+(nrExamsThisDay==null?0:nrExamsThisDay));
                }
                int btb = 0, dbtb = 0;
                for (Iterator i=exam.getBackToBackConflicts().iterator();i.hasNext();) {
                    BackToBackConflict conf = (BackToBackConflict)i.next();
                    if (exam.getPeriod().compareTo(conf.getOtherExam().getPeriod())>=0) continue;
                    if (form.getShowSections() && form.getFilter()!=null && form.getFilter().trim().length()>0) {
                        for (Enumeration e=conf.getStudents().elements();e.hasMoreElements();) {
                            Long studentId = (Long)e.nextElement();
                            if (section.getStudentIds().contains(studentId)) {
                                btb++;
                                if (conf.isDistance()) dbtb++;
                            }
                        }
                    } else {
                        btb += conf.getNrStudents();
                        if (conf.isDistance()) dbtb += conf.getNrStudents(); 
                    }
                }
                if (btb>0)
                    date2btb.put(exam.getPeriod().getStartDate(), btb + (date2btb.get(exam.getPeriod().getStartDate())==null?0:date2btb.get(exam.getPeriod().getStartDate())));
                if (dbtb>0)
                    date2dbtb.put(exam.getPeriod().getStartDate(), dbtb + (date2dbtb.get(exam.getPeriod().getStartDate())==null?0:date2dbtb.get(exam.getPeriod().getStartDate())));
            }
        }
        int tNoExam = 0, tOneExam = 0, tTwoExams = 0, tThreeExams = 0, tFourExams = 0, tBtb = 0, tDistBtb = 0;
        for (Map.Entry<Date,Hashtable<Long,Integer>> entry : date2students.entrySet()) {
            int noExam = 0, oneExam = 0, twoExams = 0, threeExams = 0, fourExams = 0, btb = 0, dbtb = 0;
            for (Map.Entry<Long, Integer> student : entry.getValue().entrySet()) {
                if (student.getValue()==1) oneExam ++;
                else if (student.getValue()==2) twoExams ++;
                else if (student.getValue()==3) threeExams ++;
                else if (student.getValue()>=4) fourExams ++;
            }
            noExam = studentIds.size() - oneExam - twoExams - threeExams - fourExams;
            btb = (date2btb.get(entry.getKey())==null?0:date2btb.get(entry.getKey()));
            dbtb = (date2dbtb.get(entry.getKey())==null?0:date2dbtb.get(entry.getKey()));
            table.addLine(
                    new String[] {
                            df.format(entry.getKey()),
                            String.valueOf(noExam),
                            String.valueOf(oneExam),
                            String.valueOf(twoExams),
                            String.valueOf(threeExams),
                            String.valueOf(fourExams),
                            String.valueOf(btb),
                            String.valueOf(dbtb)},
                    new Comparable[] {
                            new MultiComparable(0,entry.getKey()),
                            new MultiComparable(0,noExam),
                            new MultiComparable(0,oneExam),
                            new MultiComparable(0,twoExams),
                            new MultiComparable(0,threeExams),
                            new MultiComparable(0,fourExams),
                            new MultiComparable(0,btb),
                            new MultiComparable(0,dbtb)
                    });
            tNoExam += noExam;
            tOneExam += oneExam;
            tTwoExams += twoExams;
            tThreeExams += threeExams;
            tFourExams += fourExams;
            tBtb += btb;
            tDistBtb += dbtb;
        }
        table.addLine(
                new String[] {
                        (html?"<b>Totals</b>":"Totals"),
                        (html?"<b>"+tNoExam+"</b>":String.valueOf(tNoExam)),
                        (html?"<b>"+tOneExam+"</b>":String.valueOf(tOneExam)),
                        (html?"<b>"+tTwoExams+"</b>":String.valueOf(tTwoExams)),
                        (html?"<b>"+tThreeExams+"</b>":String.valueOf(tThreeExams)),
                        (html?"<b>"+tFourExams+"</b>":String.valueOf(tFourExams)),
                        (html?"<b>"+tBtb+"</b>":String.valueOf(tBtb)),
                        (html?"<b>"+tDistBtb+"</b>":String.valueOf(tDistBtb))},
                new Comparable[] {
                        new MultiComparable(1,null),
                        new MultiComparable(1,tNoExam),
                        new MultiComparable(1,tOneExam),
                        new MultiComparable(1,tTwoExams),
                        new MultiComparable(1,tThreeExams),
                        new MultiComparable(1,tFourExams),
                        new MultiComparable(1,tBtb),
                        new MultiComparable(1,tDistBtb)
                });
        return table;	    
	}
	
	private PdfWebTable generateRoomSplitReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams) {
        String nl = (html?"<br>":"\n");
        PdfWebTable table = new PdfWebTable( 14,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    (form.getShowSections()?"Class / Course":"Examination"),
                    "Enrollment",
                    "Seating"+nl+"Type",
                    "Date",
                    "Time",
                    "Average"+nl+"Distance",
                    "1st Room",
                    "1st Room"+nl+"Capacity",
                    "2nd Room",
                    "2nd Room"+nl+"Capacity",
                    "3rd Room",
                    "3rd Room"+nl+"Capacity",
                    "4th Room",
                    "4th Room"+nl+"Capacity"},
                new String[] {"left","left","center","left","left","left","left","left","left","left","left","left","left","left"},
                new boolean[] {true, true, true, true, true, true, true, true, true, true, true, true, true, true} );
        table.setRowStyle("white-space:nowrap");
        for (ExamAssignmentInfo exam : exams) {
            if (exam.getRooms()==null || exam.getRooms().size()<=1) continue;
            if (form.getShowSections()) {
                boolean firstSection = true; 
                for (ExamSectionInfo section : exam.getSections()) {
                    if (!match(form, section.getName())) continue;
                    ExamRoomInfo[] rooms = new ExamRoomInfo[Math.max(4,exam.getRooms().size())];
                    int idx = 0;
                    for (ExamRoomInfo room : exam.getRooms()) rooms[idx++] = room;
                    double distance = 0;
                    for (ExamRoomInfo r1 : exam.getRooms())
                        for (ExamRoomInfo r2 : exam.getRooms())
                            if (r1.getLocationId().compareTo(r2.getLocationId())<0) distance += r1.getDistance(r2);
                    distance /= exam.getRooms().size() * (exam.getRooms().size() - 1) / 2;
                    table.addLine(
                            "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                            new String[] {
                                (html?"<a name='"+exam.getExamId()+"'>":"")+section.getName()+(html?"</a>":""),
                                String.valueOf(section.getNrStudents()),
                                exam.getSeatingTypeLabel(),
                                exam.getDate(html),
                                exam.getTime(html),
                                ((int)(distance*10.0)==0?"":(int)(distance*10.0)+" m"),
                                (rooms[0]==null?"":html?rooms[0].toString():rooms[0].getName()),
                                (rooms[0]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[0].getPreference())+"'>"+rooms[0].getCapacity(exam)+"</font>":String.valueOf(rooms[0].getCapacity(exam))),
                                (rooms[1]==null?"":html?rooms[1].toString():rooms[1].getName()),
                                (rooms[1]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[1].getPreference())+"'>"+rooms[1].getCapacity(exam)+"</font>":String.valueOf(rooms[1].getCapacity(exam))),
                                (rooms[2]==null?"":html?rooms[2].toString():rooms[2].getName()),
                                (rooms[2]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[2].getPreference())+"'>"+rooms[2].getCapacity(exam)+"</font>":String.valueOf(rooms[2].getCapacity(exam))),
                                (rooms[3]==null?"":html?rooms[3].toString():rooms[2].getName()),
                                (rooms[3]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[3].getPreference())+"'>"+rooms[3].getCapacity(exam)+"</font>":String.valueOf(rooms[3].getCapacity(exam)))
                            },
                            new Comparable[] {
                                new MultiComparable(section.getName(), exam),
                                new MultiComparable(exam.getNrStudents(), section.getName(), exam),
                                new MultiComparable(exam.getSeatingType(), section.getName(), exam),
                                new MultiComparable(exam.getPeriodOrd(), section.getName(), exam),
                                new MultiComparable(exam.getPeriod().getStartSlot(), section.getName(), exam),
                                new MultiComparable(-distance, section.getName(), exam),
                                new MultiComparable((rooms[0]==null?"":rooms[0].getName()), section.getName(), exam),
                                new MultiComparable((rooms[0]==null?0:rooms[0].getCapacity(exam)), section.getName(), exam),
                                new MultiComparable((rooms[1]==null?"":rooms[1].getName()), section.getName(), exam),
                                new MultiComparable((rooms[1]==null?0:rooms[1].getCapacity(exam)), section.getName(), exam),
                                new MultiComparable((rooms[2]==null?"":rooms[2].getName()), section.getName(), exam),
                                new MultiComparable((rooms[2]==null?0:rooms[2].getCapacity(exam)), section.getName(), exam),
                                new MultiComparable((rooms[3]==null?"":rooms[3].getName()), section.getName(), exam),
                                new MultiComparable((rooms[3]==null?0:rooms[3].getCapacity(exam)), section.getName(), exam)
                            },
                            (firstSection?exam.getExamId().toString():null));
                    firstSection = false;
                }
            } else {
                if (!match(form, exam.getExamName())) continue;
                ExamRoomInfo[] rooms = new ExamRoomInfo[Math.max(4,exam.getRooms().size())];
                int idx = 0;
                for (ExamRoomInfo room : exam.getRooms()) rooms[idx++] = room;
                double distance = 0;
                for (ExamRoomInfo r1 : exam.getRooms())
                    for (ExamRoomInfo r2 : exam.getRooms())
                        if (r1.getLocationId().compareTo(r2.getLocationId())<0) distance += r1.getDistance(r2);
                distance /= exam.getRooms().size() * (exam.getRooms().size() - 1) / 2;
                table.addLine(
                        "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                        new String[] {
                            (html?"<a name='"+exam.getExamId()+"'>":"")+exam.getExamName()+(html?"</a>":""),
                            String.valueOf(exam.getNrStudents()),
                            exam.getSeatingTypeLabel(),
                            exam.getDate(html),
                            exam.getTime(html),
                            ((int)(distance*10.0)==0?"":(int)(distance*10.0)+" m"),
                            (rooms[0]==null?"":html?rooms[0].toString():rooms[0].getName()),
                            (rooms[0]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[0].getPreference())+"'>"+rooms[0].getCapacity(exam)+"</font>":String.valueOf(rooms[0].getCapacity(exam))),
                            (rooms[1]==null?"":html?rooms[1].toString():rooms[1].getName()),
                            (rooms[1]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[1].getPreference())+"'>"+rooms[1].getCapacity(exam)+"</font>":String.valueOf(rooms[1].getCapacity(exam))),
                            (rooms[2]==null?"":html?rooms[2].toString():rooms[2].getName()),
                            (rooms[2]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[2].getPreference())+"'>"+rooms[2].getCapacity(exam)+"</font>":String.valueOf(rooms[2].getCapacity(exam))),
                            (rooms[3]==null?"":html?rooms[3].toString():rooms[2].getName()),
                            (rooms[3]==null?"":html?"<font color='"+PreferenceLevel.int2color(rooms[3].getPreference())+"'>"+rooms[3].getCapacity(exam)+"</font>":String.valueOf(rooms[3].getCapacity(exam)))
                        },
                        new Comparable[] {
                            new MultiComparable(exam),
                            new MultiComparable(exam.getNrStudents(), exam),
                            new MultiComparable(exam.getSeatingType(), exam),
                            new MultiComparable(exam.getPeriodOrd(), exam),
                            new MultiComparable(exam.getPeriod().getStartSlot(), exam),
                            new MultiComparable(-distance, exam),
                            new MultiComparable((rooms[0]==null?"":rooms[0].getName()), exam),
                            new MultiComparable((rooms[0]==null?0:rooms[0].getCapacity(exam)), exam),
                            new MultiComparable((rooms[1]==null?"":rooms[1].getName()), exam),
                            new MultiComparable((rooms[1]==null?0:rooms[1].getCapacity(exam)), exam),
                            new MultiComparable((rooms[2]==null?"":rooms[2].getName()), exam),
                            new MultiComparable((rooms[2]==null?0:rooms[2].getCapacity(exam)), exam),
                            new MultiComparable((rooms[3]==null?"":rooms[3].getName()), exam),
                            new MultiComparable((rooms[3]==null?0:rooms[3].getCapacity(exam)), exam)
                        },
                        exam.getExamId().toString());
            }
        }
        return table;	    
	}
	
	private PdfWebTable generateViolatedDistributionsReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams) {
        String nl = (html?"<br>":"\n");
        PdfWebTable table = new PdfWebTable( 8,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    "Preference",
                    "Distribution",
                    (form.getShowSections()?"Class / Course":"Examination"),
                    "Enrollment",
                    "Seating"+nl+"Type",
                    "Date",
                    "Time",
                    "Room"},
                new String[] {"left","left","left","right","center","left","left","left"},
                new boolean[] {true, true, true, true, true, true, true, true} );
        table.setRowStyle("white-space:nowrap");
        HashSet<DistributionConflict> conflicts = new HashSet();
        for (ExamAssignmentInfo exam : exams) {
            if (!match(form, exam)) continue;
            for (DistributionConflict conf : exam.getDistributionConflicts()) {
                if (conflicts.contains(conf)) continue;
                conf.getOtherExams().add(exam); conflicts.add(conf);
            }
        }
        for (DistributionConflict conf : conflicts) {
            String classes = "", enrollment = "", seating = "";
            String date = "", time = "", room = "";
            int idx = 0;
            Date[] dates = new Date[conf.getOtherExams().size()]; 
            Integer[] times = new Integer[conf.getOtherExams().size()];
            int enrl = 0;
            for (ExamInfo exam:conf.getOtherExams()) {
                enrl += exam.getNrStudents();
                if (form.getShowSections()) {
                    if (exam instanceof ExamAssignment) {
                        ExamAssignment ea = (ExamAssignment)exam;
                        dates[idx] = ea.getPeriod().getStartTime();
                        times[idx] = ea.getPeriod().getStartSlot();
                    } else {
                        dates[idx] = new Date(0);
                        times[idx] = -1;
                    }
                    idx++;
                    boolean firstSection = true;
                    for (ExamSectionInfo section : exam.getSections()) {
                        if (classes.length()>0) {
                            classes += nl; enrollment += nl; seating += nl;
                            date += nl; time += nl; room += nl;
                        }
                        classes += section.getName();
                        enrollment += section.getNrStudents();
                        if (firstSection) {
                            seating += exam.getSeatingTypeLabel();
                            if (exam instanceof ExamAssignment) {
                                ExamAssignment ea = (ExamAssignment)exam;
                                date += ea.getDate(html);
                                time += ea.getTime(html);
                                room += ea.getRoomsName(html, ", ");
                            }
                        }
                        firstSection = false;
                    }
                } else {
                    if (classes.length()>0) {
                        classes += nl; enrollment += nl; seating += nl;
                        date += nl; time += nl; room += nl;
                    }
                    classes += exam.getExamName();
                    enrollment += exam.getNrStudents();
                    seating += exam.getSeatingTypeLabel();
                    if (exam instanceof ExamAssignment) {
                        ExamAssignment ea = (ExamAssignment)exam;
                        date += ea.getDate(html);
                        time += ea.getTime(html);
                        room += ea.getRoomsName(html, ", ");
                    }
                }
            }
            table.addLine(
                    new String[] {
                        (html?"<font color='"+PreferenceLevel.prolog2color(conf.getPreference())+"'>":"")+PreferenceLevel.prolog2string(conf.getPreference())+(html?"</font>":""),
                        (html?"<font color='"+PreferenceLevel.prolog2color(conf.getPreference())+"'>":"")+conf.getType()+(html?"</font>":""),
                        classes,
                        enrollment,
                        seating,
                        date,
                        time,
                        room
                    },
                    new Comparable[] {
                        new MultiComparable(conf),
                        new MultiComparable(conf.getType(), conf),
                        new MultiComparable(classes, conf),
                        new MultiComparable(-enrl, conf),
                        new MultiComparable(seating, conf),
                        new MultiComparable(dates),
                        new MultiComparable(times),
                        new MultiComparable(room, conf)
                    });
        }
        return table;
	}
	
	private PdfWebTable generateIndividualConflictsReport(boolean html, Long sessionId, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams, boolean studentConf, boolean direct, boolean m2d, boolean btb, String nameFormat) {
	    Hashtable<Long, Student> students = new Hashtable();
	    if (studentConf) {
            HashSet<Long> allStudentIds = new HashSet();
            for (ExamAssignmentInfo exam : exams) {
                if (direct) for (DirectConflict conflict : exam.getDirectConflicts()) {
                    allStudentIds.addAll(conflict.getStudents());
                }
                if (btb) for (BackToBackConflict conflict : exam.getBackToBackConflicts()) {
                    allStudentIds.addAll(conflict.getStudents());
                }
                if (m2d) for (MoreThanTwoADayConflict conflict : exam.getMoreThanTwoADaysConflicts()) {
                    allStudentIds.addAll(conflict.getStudents());
                }
            }
            String inSet = null; int idx = 0;
            for (Iterator i=allStudentIds.iterator();i.hasNext();idx++) {
                if (idx==1000) {
                    for (Iterator j=StudentDAO.getInstance().getSession().createQuery("select s from Student s where s.uniqueId in ("+inSet+")").iterate();j.hasNext();) {
                        Student s = (Student)j.next();
                        students.put(s.getUniqueId(), s);
                    }
                    idx = 0; inSet = null;
                }
                if (inSet==null)
                    inSet = i.next().toString();
                else
                    inSet += ","+i.next();
            }
            if (inSet!=null) {
                for (Iterator j=StudentDAO.getInstance().getSession().createQuery("select s from Student s where s.uniqueId in ("+inSet+")").iterate();j.hasNext();) {
                    Student s = (Student)j.next();
                    students.put(s.getUniqueId(), s);
                }
            }
	    }
        String nl = (html?"<br>":"\n");
        PdfWebTable table = new PdfWebTable( 10,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    (studentConf?"Student Id":"Instructor Id"),
                    "Name",
                    "Type",
                    (form.getShowSections()?"Class / Course":"Examination"),
                    "Enrollment",
                    "Seating"+nl+"Type",
                    "Date",
                    "Time",
                    "Room",
                    "Distance"},
                new String[] {"left","left","left","left","right","center","left","left","left","left"},
                new boolean[] {true, true, true, true, true, true, true, true, true, true} );
        table.setRowStyle("white-space:nowrap");
        table.setBlankWhenSame(true);
        for (ExamAssignmentInfo exam : exams) {
            if (direct)
                for (DirectConflict conflict : (studentConf?exam.getDirectConflicts():exam.getInstructorDirectConflicts())) {
                    if (conflict.getOtherExam()!=null && exam.compareTo(conflict.getOtherExam())>=0 && exams.contains(conflict.getOtherExam())) continue;
                    for (Long studentId : conflict.getStudents()) {
                        String id = "", name = "";
                        if (studentConf) {
                            Student student = students.get(studentId);
                            id = student.getExternalUniqueId();
                            name = student.getName(nameFormat);
                        } else {
                            DepartmentalInstructor instructor = new DepartmentalInstructorDAO().get(studentId);
                            id = instructor.getExternalUniqueId();
                            name = instructor.getName(nameFormat);
                        }
                        if (!match(form,id) && !match(form,name)) continue;
                        if (form.getShowSections()) {
                            String classes = "", enrollment = "", seating = "", date = "", time = "", room = "", distance = "", blank="";
                            boolean firstSection = true;
                            for (ExamSectionInfo section : exam.getSections()) {
                                if (studentConf && !section.getStudentIds().contains(studentId)) continue;
                                if (classes.length()>0) {
                                    blank+=nl; classes += nl; enrollment += nl; seating += nl; date += nl; time += nl; room += nl; distance += nl;
                                }
                                classes += section.getName();
                                enrollment += String.valueOf(section.getNrStudents());
                                if (firstSection) {
                                    seating += exam.getSeatingTypeLabel();
                                    date += exam.getDate(html);
                                    time += exam.getTime(html);
                                    room += exam.getRoomsName(html, ", ");
                                }
                                firstSection = false;
                            }
                            firstSection = true;
                            if (conflict.getOtherExam()!=null) {
                                for (ExamSectionInfo section : conflict.getOtherExam().getSections()) {
                                    if (studentConf && !section.getStudentIds().contains(studentId)) continue;
                                    if (classes.length()>0) {
                                        blank+=nl; classes += nl; enrollment += nl; seating += nl; date += nl; time += nl; room += nl; distance += nl;
                                    }
                                    classes += section.getName();
                                    enrollment += String.valueOf(section.getNrStudents());
                                    if (firstSection) {
                                        seating += conflict.getOtherExam().getSeatingTypeLabel();
                                        room += conflict.getOtherExam().getRoomsName(html, ", ");
                                    }
                                    firstSection = false;
                                }
                            } else if (conflict.getOtherEventId()!=null) {
                                blank+=nl; classes += nl; enrollment += nl; seating += nl; date += nl; time += nl; room += nl; distance += nl;
                                classes += conflict.getOtherEventName();
                                enrollment += conflict.getOtherEventSize();
                                seating += (conflict.isOtherClass()?"Class":"Event");
                                room += conflict.getOtherEventRoom();
                                //date += conflict.getOtherEventDate();
                                time += conflict.getOtherEventTime(); 
                            }
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        id,
                                        name,
                                        (html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>":"")+"Direct"+(html?"</font>":""),
                                        classes,
                                        enrollment,
                                        seating,
                                        date,
                                        time,
                                        room,
                                        ""
                                    }, new Comparable[] {
                                        new MultiComparable(id, exam, 0),
                                        new MultiComparable(name,id, exam, 0),
                                        new MultiComparable(0, exam, 0),
                                        new MultiComparable(exam, exam, 0),
                                        new MultiComparable(-exam.getNrStudents()-(conflict.getOtherExam()==null?0:conflict.getOtherExam().getNrStudents()), exam, 0),
                                        new MultiComparable(exam.getSeatingType(), exam, 0),
                                        new MultiComparable(exam.getPeriodOrd(), exam, 0),
                                        new MultiComparable(exam.getPeriod().getStartSlot(), exam, 0),
                                        new MultiComparable(exam.getRoomsName(":"), exam, 0),
                                        new MultiComparable(-1.0, exam, 0)
                                    },
                                    exam.getExamId().toString());
                        } else {
                            if (conflict.getOtherExam()!=null) {
                                table.addLine(
                                        "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                        new String[] {
                                            id,
                                            name,
                                            (html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>":"")+"Direct"+(html?"</font>":""),
                                            exam.getExamName()+nl+conflict.getOtherExam().getExamName(),
                                            exam.getNrStudents()+nl+conflict.getOtherExam().getNrStudents(),
                                            exam.getSeatingTypeLabel()+nl+conflict.getOtherExam().getSeatingTypeLabel(),
                                            exam.getDate(html)+nl,
                                            exam.getTime(html)+nl,
                                            exam.getRoomsName(html, ", ")+nl+conflict.getOtherExam().getRoomsName(html, ", "),
                                            ""
                                        }, new Comparable[] {
                                            new MultiComparable(id, exam, 0),
                                            new MultiComparable(name, id, exam, 0),
                                            new MultiComparable(0, exam, 0),
                                            new MultiComparable(exam, exam, 0),
                                            new MultiComparable(-exam.getNrStudents()-conflict.getOtherExam().getNrStudents(), exam, 0),
                                            new MultiComparable(exam.getSeatingType(), exam, 0),
                                            new MultiComparable(exam.getPeriodOrd(), exam, 0),
                                            new MultiComparable(exam.getPeriod().getStartSlot(), exam, 0),
                                            new MultiComparable(exam.getRoomsName(":"), exam, 0),
                                            new MultiComparable(-1.0, exam, 0)
                                        },
                                        exam.getExamId().toString());                                
                            } else if (conflict.getOtherEventId()!=null) {
                                table.addLine(
                                        "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                        new String[] {
                                            id,
                                            name,
                                            (html?"<font color='"+PreferenceLevel.prolog2color("P")+"'>":"")+"Direct"+(html?"</font>":""),
                                            exam.getExamName()+nl+conflict.getOtherEventName(),
                                            String.valueOf(exam.getNrStudents())+nl+conflict.getOtherEventSize(),
                                            exam.getSeatingTypeLabel()+nl+(conflict.isOtherClass()?"Class":"Event"),
                                            exam.getDate(html)+nl,//+conflict.getOtherEventDate(),
                                            exam.getTime(html)+nl+conflict.getOtherEventTime(),
                                            exam.getRoomsName(html, ", ")+nl+conflict.getOtherEventRoom(),
                                            ""
                                        }, new Comparable[] {
                                            new MultiComparable(id, exam, 0),
                                            new MultiComparable(name, id, exam, 0),
                                            new MultiComparable(0, exam, 0),
                                            new MultiComparable(exam, exam, 0),
                                            new MultiComparable(-exam.getNrStudents()-conflict.getOtherEventSize(), exam, 0),
                                            new MultiComparable(exam.getSeatingType(), exam, 0),
                                            new MultiComparable(exam.getPeriodOrd(), exam, 0),
                                            new MultiComparable(exam.getPeriod().getStartSlot(), exam, 0),
                                            new MultiComparable(exam.getRoomsName(":"), exam, 0),
                                            new MultiComparable(-1.0, exam, 0)
                                        },
                                        exam.getExamId().toString());                                
                            }

                        }
                    }
                }
            if (btb)
                for (BackToBackConflict conflict : (studentConf?exam.getBackToBackConflicts():exam.getInstructorBackToBackConflicts())) {
                    if (exam.compareTo(conflict.getOtherExam())>=0 && exams.contains(conflict.getOtherExam())) continue;
                    for (Long studentId : conflict.getStudents()) {
                        String id = "", name = "";
                        if (studentConf) {
                            Student student = students.get(studentId);
                            id = student.getExternalUniqueId();
                            name = student.getName(nameFormat);
                        } else {
                            DepartmentalInstructor instructor = new DepartmentalInstructorDAO().get(studentId);
                            id = instructor.getExternalUniqueId();
                            name = instructor.getName(nameFormat);
                        }
                        if (!match(form,id) && !match(form,name)) continue;
                        if (form.getShowSections()) {
                            String classes = "", enrollment = "", seating = "", date = "", time = "", room = "", distance = "", blank="";
                            boolean firstSection = true;
                            for (ExamSectionInfo section : exam.getSections()) {
                                if (studentConf && !section.getStudentIds().contains(studentId)) continue;
                                if (classes.length()>0) {
                                    blank+=nl; classes += nl; enrollment += nl; seating += nl; date += nl; time += nl; room += nl; distance += nl;
                                }
                                classes += section.getName();
                                enrollment += String.valueOf(section.getNrStudents());
                                if (firstSection) {
                                    seating += exam.getSeatingTypeLabel();
                                    date += exam.getDate(html);
                                    time += exam.getTime(html);
                                    room += exam.getRoomsName(html, ", ");
                                }
                                firstSection = false;
                            }
                            firstSection = true;
                            for (ExamSectionInfo section : conflict.getOtherExam().getSections()) {
                                if (studentConf && !section.getStudentIds().contains(studentId)) continue;
                                if (classes.length()>0) {
                                    blank+=nl; classes += nl; enrollment += nl; seating += nl; date += nl; time += nl; room += nl; distance += nl;
                                }
                                classes += section.getName();
                                enrollment += String.valueOf(section.getNrStudents());
                                if (firstSection) {
                                    seating += exam.getSeatingTypeLabel();
                                    time += conflict.getOtherExam().getTime(html);
                                    room += conflict.getOtherExam().getRoomsName(html, ", ");
                                }
                                firstSection = false;
                            }
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        id,
                                        name,
                                        (html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>":"")+"Back-To-Back"+(html?"</font>":""),
                                        classes,
                                        enrollment,
                                        seating,
                                        date,
                                        time,
                                        room,
                                        (int)(conflict.getDistance()*10.0)+" m"
                                    }, new Comparable[] {
                                        new MultiComparable(id, exam, 0),
                                        new MultiComparable(name, id, exam, 0),
                                        new MultiComparable(2, exam, 0),
                                        new MultiComparable(exam, exam, 0),
                                        new MultiComparable(-exam.getNrStudents()-conflict.getOtherExam().getNrStudents(), exam, 0),
                                        new MultiComparable(exam.getSeatingType(), exam, 0),
                                        new MultiComparable(exam.getPeriodOrd(), exam, 0),
                                        new MultiComparable(exam.getPeriod().getStartSlot(), exam, 0),
                                        new MultiComparable(exam.getRoomsName(":"), exam, 0),
                                        new MultiComparable(conflict.getDistance(), exam, 0)
                                    },
                                    exam.getExamId().toString());
                        } else {
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        id,
                                        name,
                                        (html?"<font color='"+PreferenceLevel.prolog2color("1")+"'>":"")+"Back-To-Back"+(html?"</font>":""),
                                        exam.getExamName()+nl+conflict.getOtherExam().getExamName(),
                                        exam.getNrStudents()+nl+conflict.getOtherExam().getNrStudents(),
                                        exam.getSeatingTypeLabel()+nl+conflict.getOtherExam().getSeatingTypeLabel(),
                                        exam.getDate(html)+nl,
                                        exam.getTime(html)+nl+conflict.getOtherExam().getTime(html),
                                        exam.getRoomsName(html, ", ")+nl+conflict.getOtherExam().getRoomsName(html, ", "),
                                        (int)(conflict.getDistance()*10.0)+" m"
                                    }, new Comparable[] {
                                        new MultiComparable(id, exam, 0),
                                        new MultiComparable(name, id, exam, 0),
                                        new MultiComparable(2, exam, 0),
                                        new MultiComparable(exam, exam, 0),
                                        new MultiComparable(-exam.getNrStudents()-conflict.getOtherExam().getNrStudents(), exam, 0),
                                        new MultiComparable(exam.getSeatingType(), exam, 0),
                                        new MultiComparable(exam.getPeriodOrd(), exam, 0),
                                        new MultiComparable(exam.getPeriod().getStartSlot(), exam, 0),
                                        new MultiComparable(exam.getRoomsName(":"), exam, 0),
                                        new MultiComparable(conflict.getDistance(), exam, 0)
                                    },
                                    exam.getExamId().toString());
                        }
                    }
                }
            if (m2d)
                conflicts: for (MoreThanTwoADayConflict conflict : (studentConf?exam.getMoreThanTwoADaysConflicts():exam.getInstructorMoreThanTwoADaysConflicts())) {
                    for (ExamAssignment other : conflict.getOtherExams())
                        if (exam.compareTo(other)>=0 && exams.contains(other)) continue conflicts;
                    for (Long studentId : conflict.getStudents()) {
                        String id = "", name = "";
                        if (studentConf) {
                            Student student = students.get(studentId);
                            id = student.getExternalUniqueId();
                            name = student.getName(nameFormat);
                        } else {
                            DepartmentalInstructor instructor = new DepartmentalInstructorDAO().get(studentId);
                            id = instructor.getExternalUniqueId();
                            name = instructor.getName(nameFormat);
                        }
                        if (!match(form,id) && !match(form,name)) continue;
                        if (form.getShowSections()) {
                            String classes = "", enrollment = "", seating = "", date = "", time = "", room = "", distance = "", blank="";
                            int nrStudents = exam.getNrStudents();
                            boolean firstSection = true;
                            for (ExamSectionInfo section : exam.getSections()) {
                                if (studentConf && !section.getStudentIds().contains(studentId)) continue;
                                if (classes.length()>0) {
                                    blank+=nl; classes += nl; enrollment += nl; seating += nl; date += nl; time += nl; room += nl; distance += nl;
                                }
                                classes += section.getName();
                                enrollment += String.valueOf(section.getNrStudents());
                                if (firstSection) {
                                    seating += exam.getSeatingTypeLabel();
                                    date += exam.getDate(html);
                                    time += exam.getTime(html);
                                    room += exam.getRoomsName(html, ", ");
                                }
                                firstSection = false;
                            }
                            for (ExamAssignment other : conflict.getOtherExams()) {
                                firstSection = true;
                                nrStudents += other.getNrStudents();
                                for (ExamSectionInfo section : other.getSections()) {
                                    if (studentConf && !section.getStudentIds().contains(studentId)) continue;
                                    if (classes.length()>0) {
                                        blank+=nl; classes += nl; enrollment += nl; seating += nl; date += nl; time += nl; room += nl; distance += nl;
                                    }
                                    classes += section.getName();
                                    enrollment += String.valueOf(section.getNrStudents());
                                    if (firstSection) {
                                        seating += other.getSeatingTypeLabel();
                                        time += other.getTime(html);
                                        room += other.getRoomsName(html, ", ");
                                    }
                                    firstSection = false;
                                }
                            }
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        id,
                                        name,
                                        (html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>":"")+(html?"&gt;":"")+"2 A Day"+(html?"</font>":""),
                                        classes,
                                        enrollment,
                                        seating,
                                        date,
                                        time,
                                        room,
                                        ""
                                    }, new Comparable[] {
                                        new MultiComparable(id, exam, 0),
                                        new MultiComparable(name, id, exam, 0),
                                        new MultiComparable(1, exam, 0),
                                        new MultiComparable(exam, exam, 0),
                                        new MultiComparable(-nrStudents, exam, 0),
                                        new MultiComparable(exam.getSeatingType(), exam, 0),
                                        new MultiComparable(exam.getPeriodOrd(), exam, 0),
                                        new MultiComparable(exam.getPeriod().getStartSlot(), exam, 0),
                                        new MultiComparable(exam.getRoomsName(":"), exam, 0),
                                        new MultiComparable(-1.0, exam, 0)
                                    },
                                    exam.getExamId().toString());
                        } else {
                            String classes = exam.getExamName(), enrollment = ""+exam.getNrStudents(), seating = exam.getSeatingTypeLabel();
                            String date = exam.getDate(html), time = exam.getTime(html), room = exam.getRoomsName(html, ", "), distance = "", blank="";
                            int nrStudents = exam.getNrStudents();
                            for (ExamAssignment other : conflict.getOtherExams()) {
                                classes += nl+other.getExamName();
                                enrollment += nl+other.getNrStudents();
                                seating += nl+other.getSeatingTypeLabel();
                                time += nl+other.getTime(html);
                                room += nl+other.getRoomsName(html, ", ");
                                blank += nl; distance += nl;
                            }
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        id,
                                        name,
                                        (html?"<font color='"+PreferenceLevel.prolog2color("2")+"'>":"")+(html?"&gt;":"")+"2 A Day"+(html?"</font>":""),
                                        classes,
                                        enrollment,
                                        seating,
                                        date,
                                        time,
                                        room,
                                        ""
                                    }, new Comparable[] {
                                        new MultiComparable(id, exam, 0),
                                        new MultiComparable(name, id, exam, 0),
                                        new MultiComparable(1, exam, 0),
                                        new MultiComparable(exam, exam, 0),
                                        new MultiComparable(-nrStudents, exam, 0),
                                        new MultiComparable(exam.getSeatingType(), exam, 0),
                                        new MultiComparable(exam.getPeriodOrd(), exam, 0),
                                        new MultiComparable(exam.getPeriod().getStartSlot(), exam, 0),
                                        new MultiComparable(exam.getRoomsName(":"), exam, 0),
                                        new MultiComparable(-1.0, exam, 0)
                                    },
                                    exam.getExamId().toString());
                        }
                    }
                }
        }
        
        return table;	    
	}
	
	private PdfWebTable generateDirectConflictsReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams, boolean studentConf) {
        String nl = (html?"<br>":"\n");
        DecimalFormat df = new DecimalFormat("0.0");
        PdfWebTable table = new PdfWebTable( 10,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    "1st "+(form.getShowSections()?"Class / Course":"Examination"),
                    "1st Enrollment",
                    "1st Seating"+nl+"Type",
                    "2nd "+(form.getShowSections()?"Class / Course":"Examination"),
                    "2nd Enrollment",
                    "2nd Seating"+nl+"Type",
                    "Date",
                    "Time",
                    "Direct",
                    "Direct [%]"},
                new String[] {"left","right","center","left","right","center","left","left","right","right"},
                new boolean[] {true, true, true, true, true, true, true, true, true, true} );
        
        table.setRowStyle("white-space:nowrap");
        for (ExamAssignmentInfo exam : exams) {
            if (!match(form, exam)) continue;
            for (DirectConflict conflict : (studentConf?exam.getDirectConflicts():exam.getInstructorDirectConflicts())) {
                if (match(form, conflict.getOtherExam()) && exam.compareTo(conflict.getOtherExam())>=0) continue;
                ExamAssignment other = conflict.getOtherExam();
                if (form.getShowSections()) {
                    for (ExamSectionInfo section1 : exam.getSections()) {
                        if (other!=null) {
                            for (ExamSectionInfo section2 : conflict.getOtherExam().getSections()) {
                                if (!match(form, section1.getName()) && !match(form, section2.getName())) continue;
                                int nrStudents = 0;
                                if (studentConf) for (Long studentId : section1.getStudentIds()) {
                                    if (section2.getStudentIds().contains(studentId)) nrStudents++;
                                } else nrStudents = conflict.getNrStudents();
                                if (nrStudents==0) continue;
                                table.addLine(
                                        "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                        new String[] {
                                            section1.getName(),
                                            String.valueOf(section1.getNrStudents()),
                                            exam.getSeatingTypeLabel(),
                                            section2.getName(),
                                            String.valueOf(section2.getNrStudents()),
                                            conflict.getOtherExam().getSeatingTypeLabel(),
                                            exam.getDate(html),
                                            exam.getTime(html),
                                            String.valueOf(nrStudents),
                                            df.format(100.0*nrStudents/Math.min(section1.getNrStudents(), section2.getNrStudents()))
                                        }, new Comparable[] {
                                            new MultiComparable(section1.getName(), section2.getName(), exam, other),
                                            new MultiComparable(-section1.getNrStudents(), -section2.getNrStudents(), section1.getName(), section2.getName(), exam, other),
                                            new MultiComparable(exam.getSeatingType(), other.getSeatingType(), section1.getName(), section2.getName(), exam, other),
                                            new MultiComparable(section2.getName(), section1.getName(), other, exam),
                                            new MultiComparable(-section2.getNrStudents(), -section1.getNrStudents(), section2.getName(), section1.getName(), other, exam),
                                            new MultiComparable(other.getSeatingType(), exam.getSeatingType(), section2.getName(), section1.getName(), other, exam),
                                            new MultiComparable(exam.getPeriodOrd(), exam, other),
                                            new MultiComparable(exam.getPeriod().getStartSlot(), exam, other),
                                            new MultiComparable(-nrStudents, exam, other),
                                            new MultiComparable(-100.0*nrStudents/Math.min(section1.getNrStudents(), section2.getNrStudents()), exam, other)
                                        },
                                        exam.getExamId().toString());
                            }                        
                        } else if (conflict.getOtherEventId()!=null) {
                            if (!match(form, section1.getName())) continue;
                            int nrStudents = 0;
                            for (Long studentId : section1.getStudentIds())
                                if (conflict.getStudents().contains(studentId)) nrStudents++;
                            if (nrStudents==0) continue;
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        section1.getName(),
                                        String.valueOf(section1.getNrStudents()),
                                        exam.getSeatingTypeLabel(),
                                        conflict.getOtherEventName(),
                                        String.valueOf(conflict.getOtherEventSize()),
                                        (conflict.isOtherClass()?"Class":"Event"),
                                        exam.getDate(html),
                                        exam.getTime(html),
                                        String.valueOf(nrStudents),
                                        df.format(100.0*nrStudents/Math.min(section1.getNrStudents(), conflict.getOtherEventSize()))
                                    }, new Comparable[] {
                                        new MultiComparable(section1.getName(), conflict.getOtherEventName(), exam, other),
                                        new MultiComparable(-section1.getNrStudents(), -conflict.getOtherEventSize(), section1.getName(), conflict.getOtherEventName(), exam, other),
                                        new MultiComparable(exam.getSeatingType(), -1, section1.getName(), conflict.getOtherEventName(), exam, other),
                                        new MultiComparable(conflict.getOtherEventName(), section1.getName(), other, exam),
                                        new MultiComparable(-conflict.getOtherEventSize(), -section1.getNrStudents(), conflict.getOtherEventName(), section1.getName(), other, exam),
                                        new MultiComparable(-1, exam.getSeatingType(), conflict.getOtherEventName(), section1.getName(), other, exam),
                                        new MultiComparable(exam.getPeriodOrd(), exam, other),
                                        new MultiComparable(exam.getPeriod().getStartSlot(), exam, other),
                                        new MultiComparable(-nrStudents, exam, other),
                                        new MultiComparable(-100.0*nrStudents/Math.min(section1.getNrStudents(), conflict.getOtherEventSize()), exam, other)
                                    },
                                    exam.getExamId().toString());                            
                        }
                    }
                } else {
                    if (other!=null) {
                        table.addLine(
                                "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                new String[] {
                                    exam.getExamName(),
                                    String.valueOf(exam.getNrStudents()),
                                    exam.getSeatingTypeLabel(),
                                    other.getExamName(),
                                    String.valueOf(other.getNrStudents()),
                                    conflict.getOtherExam().getSeatingTypeLabel(),
                                    exam.getDate(html),
                                    exam.getTime(html),
                                    String.valueOf(conflict.getNrStudents()),
                                    df.format(100.0*conflict.getNrStudents()/Math.min(exam.getNrStudents(), other.getNrStudents()))
                                }, new Comparable[] {
                                    new MultiComparable(exam, other),
                                    new MultiComparable(-exam.getNrStudents(), -other.getNrStudents(), exam, other),
                                    new MultiComparable(exam.getSeatingType(), other.getSeatingType(), exam, other),
                                    new MultiComparable(other, exam),
                                    new MultiComparable(-other.getNrStudents(), -exam.getNrStudents(), other, exam),
                                    new MultiComparable(other.getSeatingType(), exam.getSeatingType(), other, exam),
                                    new MultiComparable(exam.getPeriodOrd(), exam, other),
                                    new MultiComparable(exam.getPeriod().getStartSlot(), exam, other),
                                    new MultiComparable(-conflict.getNrStudents(), exam, other),
                                    new MultiComparable(-100.0*conflict.getNrStudents()/Math.min(exam.getNrStudents(), other.getNrStudents()), exam, other)
                                },
                                exam.getExamId().toString());
                    } else if (conflict.getOtherEventId()!=null) {
                        table.addLine(
                                "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                new String[] {
                                    exam.getExamName(),
                                    String.valueOf(exam.getNrStudents()),
                                    exam.getSeatingTypeLabel(),
                                    conflict.getOtherEventName(),
                                    String.valueOf(conflict.getOtherEventSize()),
                                    (conflict.isOtherClass()?"Class":"Event"),
                                    exam.getDate(html),
                                    exam.getTime(html),
                                    String.valueOf(conflict.getNrStudents()),
                                    df.format(100.0*conflict.getNrStudents()/Math.min(exam.getNrStudents(), conflict.getOtherEventSize()))
                                }, new Comparable[] {
                                    new MultiComparable(exam, other),
                                    new MultiComparable(-exam.getNrStudents(), -conflict.getOtherEventSize(), exam, other),
                                    new MultiComparable(exam.getSeatingType(), -1, exam, other),
                                    new MultiComparable(other, exam),
                                    new MultiComparable(-conflict.getOtherEventSize(), -exam.getNrStudents(), other, exam),
                                    new MultiComparable(-1, exam.getSeatingType(), other, exam),
                                    new MultiComparable(exam.getPeriodOrd(), exam, other),
                                    new MultiComparable(exam.getPeriod().getStartSlot(), exam, other),
                                    new MultiComparable(-conflict.getNrStudents(), exam, other),
                                    new MultiComparable(-100.0*conflict.getNrStudents()/Math.min(exam.getNrStudents(), conflict.getOtherEventSize()), exam, other)
                                },
                                exam.getExamId().toString());
                    }
                }
            }
        }
        return table;
	}
    
    private PdfWebTable generateBackToBackConflictsReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams, boolean studentConf) {
         String nl = (html?"<br>":"\n");
         DecimalFormat df = new DecimalFormat("0.0");
         PdfWebTable table = new PdfWebTable( 11,
                 form.getReport(), "examAssignmentReport.do?ord=%%",
                 new String[] {
                     "1st "+(form.getShowSections()?"Class / Course":"Examination"),
                     "1st Enrollment",
                     "1st Seating"+nl+"Type",
                     "2nd "+(form.getShowSections()?"Class / Course":"Examination"),
                     "2nd Enrollment",
                     "2nd Seating"+nl+"Type",
                     "Date",
                     "Time",
                     "Back-To-Back",
                     "Back-To-Back [%]",
                     "Distance [m]"},
                 new String[] {"left","right","center","left","right","center","left","left","right","right","right"},
                 new boolean[] {true, true, true, true, true, true, true, true, true, true, true} );
         
         table.setRowStyle("white-space:nowrap");
         for (ExamAssignmentInfo exam : exams) {
             if (!match(form, exam)) continue;
             for (BackToBackConflict conflict : (studentConf?exam.getBackToBackConflicts():exam.getInstructorBackToBackConflicts())) {
                 if (match(form, conflict.getOtherExam()) && exam.getPeriod().compareTo(conflict.getOtherExam().getPeriod())>=0) continue;
                 ExamAssignment other = conflict.getOtherExam();
                 if (form.getShowSections()) {
                     for (ExamSectionInfo section1 : exam.getSections()) {
                         for (ExamSectionInfo section2 : conflict.getOtherExam().getSections()) {
                             int nrStudents = 0;
                             if (!match(form, section1.getName()) && !match(form, section2.getName())) continue;
                             if (studentConf) for (Long studentId : section1.getStudentIds()) {
                                 if (section2.getStudentIds().contains(studentId)) nrStudents++;
                             } else
                                 nrStudents = conflict.getNrStudents();
                             if (nrStudents==0) continue;
                             table.addLine(
                                     "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                     new String[] {
                                         section1.getName(),
                                         String.valueOf(section1.getNrStudents()),
                                         exam.getSeatingTypeLabel(),
                                         section2.getName(),
                                         String.valueOf(section2.getNrStudents()),
                                         conflict.getOtherExam().getSeatingTypeLabel(),
                                         exam.getDate(html),
                                         exam.getTime(html),
                                         String.valueOf(nrStudents),
                                         df.format(100.0*nrStudents/Math.min(section1.getNrStudents(), section2.getNrStudents())),
                                         String.valueOf((int)(10.0*conflict.getDistance()))
                                     }, new Comparable[] {
                                         new MultiComparable(section1.getName(), section2.getName(), exam, other),
                                         new MultiComparable(-section1.getNrStudents(), -section2.getNrStudents(), section1.getName(), section2.getName(), exam, other),
                                         new MultiComparable(exam.getSeatingType(), other.getSeatingType(), section1.getName(), section2.getName(), exam, other),
                                         new MultiComparable(section2.getName(), section1.getName(), other, exam),
                                         new MultiComparable(-section2.getNrStudents(), -section1.getNrStudents(), section2.getName(), section1.getName(), other, exam),
                                         new MultiComparable(other.getSeatingType(), exam.getSeatingType(), section2.getName(), section1.getName(), other, exam),
                                         new MultiComparable(exam.getPeriodOrd(), exam, other),
                                         new MultiComparable(exam.getPeriod().getStartSlot(), exam, other),
                                         new MultiComparable(-nrStudents, exam, other),
                                         new MultiComparable(-100.0*nrStudents/Math.min(section1.getNrStudents(), section2.getNrStudents()), exam, other),
                                         new MultiComparable(-conflict.getDistance(), exam, other)
                                     },
                                     exam.getExamId().toString());
                         }
                     }
                 } else {
                     table.addLine(
                             "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                             new String[] {
                                 exam.getExamName(),
                                 String.valueOf(exam.getNrStudents()),
                                 exam.getSeatingTypeLabel(),
                                 other.getExamName(),
                                 String.valueOf(other.getNrStudents()),
                                 conflict.getOtherExam().getSeatingTypeLabel(),
                                 exam.getDate(html),
                                 exam.getTime(html),
                                 String.valueOf(conflict.getNrStudents()),
                                 df.format(100.0*conflict.getNrStudents()/Math.min(exam.getNrStudents(), other.getNrStudents())),
                                 String.valueOf((int)(10.0*conflict.getDistance()))
                             }, new Comparable[] {
                                 new MultiComparable(exam, other),
                                 new MultiComparable(-exam.getNrStudents(), -other.getNrStudents(), exam, other),
                                 new MultiComparable(exam.getSeatingType(), other.getSeatingType(), exam, other),
                                 new MultiComparable(other, exam),
                                 new MultiComparable(-other.getNrStudents(), -exam.getNrStudents(), other, exam),
                                 new MultiComparable(other.getSeatingType(), exam.getSeatingType(), other, exam),
                                 new MultiComparable(exam.getPeriodOrd(), exam, other),
                                 new MultiComparable(exam.getPeriod().getStartSlot(), exam, other),
                                 new MultiComparable(-conflict.getNrStudents(), exam, other),
                                 new MultiComparable(-100.0*conflict.getNrStudents()/Math.min(exam.getNrStudents(), other.getNrStudents()), exam, other),
                                 new MultiComparable(-conflict.getDistance(), exam, other)
                             },
                             exam.getExamId().toString());
                 }
             }
         }
         return table;
    }
    
    private PdfWebTable generate2MoreADayConflictsReport(boolean html, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams, boolean studentConf) {
        String nl = (html?"<br>":"\n");
        DecimalFormat df = new DecimalFormat("0.0");
        int max = 0;
        for (ExamAssignmentInfo exam : exams) {
            if (!match(form, exam)) continue;
            conflicts: for (MoreThanTwoADayConflict conflict : (studentConf?exam.getMoreThanTwoADaysConflicts():exam.getInstructorMoreThanTwoADaysConflicts())) {
                for (ExamAssignment other : conflict.getOtherExams()) 
                    if (match(form, other) && exam.compareTo(other)>=0) continue conflicts;
                max = Math.max(max,conflict.getOtherExams().size()+1);
            }
        }
        if (max<=2) return null;
        
        String[] colName = new String[3+3*max];
        String[] colAlign = new String[3+3*max];
        boolean[] colOrd = new boolean[3+3*max];
        int idx = 0;
        colName[idx] = "Date"; colAlign[idx] = "left"; colOrd[idx++] = true;
        for (int i=0;i<max;i++) {
            String th = (i==0?"1st":i==1?"2nd":i==2?"3rd":(i+1)+"th");
            colName[idx] = th+" "+(form.getShowSections()?"Class / Course":"Examination"); colAlign[idx] = "left"; colOrd[idx++] = true;
            colName[idx] = th+" Enrollment"; colAlign[idx] = "right"; colOrd[idx++] = true;
            colName[idx] = th+" Time"; colAlign[idx] = "left"; colOrd[idx++] = true;
        }
        colName[idx] = (html?"&gt;":"")+"2 A Day"; colAlign[idx] = "left"; colOrd[idx++] = true;
        colName[idx] = (html?"&gt;":"")+"2 A Day [%]"; colAlign[idx] = "left"; colOrd[idx++] = true;
        PdfWebTable table = new PdfWebTable( 3+4*max, form.getReport(), "examAssignmentReport.do?ord=%%", colName, colAlign, colOrd);
        table.setRowStyle("white-space:nowrap");
        for (ExamAssignmentInfo exam : exams) {
            if (!match(form, exam)) continue;
            conflicts: for (MoreThanTwoADayConflict conflict : (studentConf?exam.getMoreThanTwoADaysConflicts():exam.getInstructorMoreThanTwoADaysConflicts())) {
                for (ExamAssignment other : conflict.getOtherExams())
                    if (match(form, other) && exam.compareTo(other)>=0) continue conflicts;
                Vector<ExamAssignment> examsThisConf = new Vector<ExamAssignment>(max);
                examsThisConf.add(exam);
                examsThisConf.addAll(conflict.getOtherExams());
                Collections.sort(examsThisConf, new Comparator<ExamAssignment>() {
                    public int compare(ExamAssignment a1, ExamAssignment a2) {
                        //int cmp = a1.getPeriod().compareTo(a2.getPeriod());
                        //if (cmp!=0) return cmp;
                        return a1.compareTo(a2);
                    }
                });
                if (form.getShowSections()) {
                    idx = 0;
                    String[] line = new String[3+3*max];
                    Comparable[] cmp = new Comparable[3+3*max];
                    line[idx] = exam.getDate(false); cmp[idx] = new MultiComparable(exam.getPeriodOrd(), new MultiComparable(examsThisConf));
                    m2dReportAddLines(form, html, studentConf, table, max, examsThisConf, 0, line, cmp, 1, exam.getNrStudents(), null, false);  
                } else {
                    idx = 0;
                    String[] line = new String[3+3*max];
                    Comparable[] cmp = new Comparable[3+3*max];
                    line[idx] = exam.getDate(false); cmp[idx++] = new MultiComparable(exam.getPeriodOrd(), new MultiComparable(examsThisConf));
                    int minStudents = exam.getNrStudents();
                    for (ExamAssignment x : examsThisConf) {
                        line[idx] = x.getExamName();
                        cmp[idx++] = new MultiComparable(x, new MultiComparable(examsThisConf));
                        line[idx] = String.valueOf(x.getNrStudents());
                        cmp[idx++] = new MultiComparable(-x.getNrStudents(), x, new MultiComparable(examsThisConf));
                        line[idx] = x.getTime(html);
                        cmp[idx++] = new MultiComparable(x.getPeriod().getStartSlot(), x, new MultiComparable(examsThisConf));
                        minStudents = Math.min(minStudents, x.getNrStudents());
                    }
                    for (int i=examsThisConf.size();i<max;i++) {
                        line[idx] = "";
                        cmp[idx++] = new MultiComparable(null, new MultiComparable(examsThisConf));
                        line[idx] = "";
                        cmp[idx++] = new MultiComparable(1, null, new MultiComparable(examsThisConf));
                        line[idx] = "";
                        cmp[idx++] = new MultiComparable(-1, null, new MultiComparable(examsThisConf));
                    }
                    line[idx] = String.valueOf(conflict.getNrStudents());
                    cmp[idx++] = new MultiComparable(-conflict.getNrStudents(), new MultiComparable(examsThisConf));
                    line[idx] = df.format(100.0*conflict.getNrStudents()/minStudents);
                    cmp[idx++] = new MultiComparable(-100.0*conflict.getNrStudents()/minStudents, new MultiComparable(examsThisConf));
                    table.addLine(
                            "onClick=\"document.location='examDetail.do?examId="+examsThisConf.firstElement().getExamId()+"';\"",
                            line, cmp, examsThisConf.firstElement().getExamId().toString());
                }
            }
        }
        return table;
    }
    
    private void m2dReportAddLines(ExamAssignmentReportForm form, boolean html, boolean studentConf, PdfWebTable table, int max, Vector<ExamAssignment> exams, int pos, String[] line, Comparable[] cmp, int idx, int minStudents, Set<Long> students, boolean match) {
        if (students!=null && students.isEmpty()) return;
        if (pos==max) {
            if (!match) return;
            line[idx] = String.valueOf(students.size());
            cmp[idx++] = new MultiComparable(-students.size(), new MultiComparable(exams));
            line[idx] = new DecimalFormat("0.0").format(100.0*students.size()/minStudents);
            cmp[idx++] = new MultiComparable(-100.0*students.size()/minStudents, new MultiComparable(exams));
            
            table.addLine("onClick=\"document.location='examDetail.do?examId="+exams.firstElement().getExamId()+"';\"", (String[])line.clone(), (Comparable[])cmp.clone(), exams.firstElement().getExamId().toString());
            return;
        }
        if (pos<exams.size()) {
            ExamAssignment exam = exams.elementAt(pos);
            Set<Long> newStudents = null;
            for (ExamSectionInfo section : exam.getSections()) {
                if (students == null) {
                    newStudents = new HashSet(section.getStudentIds());
                } else {
                    newStudents = new HashSet<Long>();
                    for (Long studentId : students) {
                        if (studentConf && section.getStudentIds().contains(studentId)) newStudents.add(studentId);
                        if (!studentConf && section.getExam().hasInstructor(studentId)) newStudents.add(studentId);
                    }
                }
                if (newStudents.isEmpty()) continue;
                line[idx] = section.getName();
                cmp[idx] = new MultiComparable(section.getName(), new MultiComparable(exams));
                line[idx+1] = String.valueOf(section.getNrStudents());
                cmp[idx+1] = new MultiComparable(-section.getNrStudents(), section.getName(), new MultiComparable(exams));
                line[idx+2] = exam.getTime(html);
                cmp[idx+2] = new MultiComparable(exam.getPeriod().getStartSlot(), section.getName(), new MultiComparable(exams));
                m2dReportAddLines(form, html, studentConf, table, max, exams, pos+1, line, cmp, idx+3, Math.min(section.getNrStudents(),minStudents), newStudents, match || match(form,section.getName()));
            }
        } else {
            line[idx] = "";
            cmp[idx++] = new MultiComparable(null, new MultiComparable(exams));
            line[idx] = "";
            cmp[idx++] = new MultiComparable(1, null, new MultiComparable(exams));
            line[idx] = "";
            cmp[idx++] = new MultiComparable(-1, null, new MultiComparable(exams));
            m2dReportAddLines(form, html, studentConf, table, max, exams, pos+1, line, cmp, idx, minStudents, students, match);
        }
    }
    
    private PdfWebTable generateIndividualAssignmentReport(boolean html, Long sessionId, ExamAssignmentReportForm form, Collection<ExamAssignmentInfo> exams, boolean student, String nameFormat) {
        Hashtable<Long, Student> students = new Hashtable();
        if (student) {
            HashSet<Long> allStudentIds = new HashSet();
            for (ExamAssignmentInfo exam : exams)
                for (ExamSectionInfo section : exam.getSections())
                    allStudentIds.addAll(section.getStudentIds());
            String inSet = null; int idx = 0;
            for (Iterator i=allStudentIds.iterator();i.hasNext();idx++) {
                if (idx==1000) {
                    for (Iterator j=StudentDAO.getInstance().getSession().createQuery("select s from Student s where s.uniqueId in ("+inSet+")").iterate();j.hasNext();) {
                        Student s = (Student)j.next();
                        students.put(s.getUniqueId(), s);
                    }
                    idx = 0; inSet = null;
                }
                if (inSet==null)
                    inSet = i.next().toString();
                else
                    inSet += ","+i.next();
            }
            if (inSet!=null) {
                for (Iterator j=StudentDAO.getInstance().getSession().createQuery("select s from Student s where s.uniqueId in ("+inSet+")").iterate();j.hasNext();) {
                    Student s = (Student)j.next();
                    students.put(s.getUniqueId(), s);
                }
            }
        }
        String nl = (html?"<br>":"\n");
        PdfWebTable table =
            (student?
            new PdfWebTable( 7,
                form.getReport(), "examAssignmentReport.do?ord=%%",
                new String[] {
                    (student?"Student Id":"Instructor Id"),
                    "Name",
                    (form.getShowSections()?"Class / Course":"Examination"),
                    "Date",
                    "Time",
                    "Room",
                    "Instuctor"},
                new String[] {"left","left","left","left", "left", "left", "left"},
                new boolean[] {true, true, true, true, true, true, true} ):
           new PdfWebTable( 6,
                   form.getReport(), "examAssignmentReport.do?ord=%%",
                   new String[] {
                       (student?"Student Id":"Instructor Id"),
                       "Name",
                       (form.getShowSections()?"Class / Course":"Examination"),
                       "Date",
                       "Time",
                       "Room"},
                       new String[] {"left","left","left","left", "left", "left"},
                       new boolean[] {true, true, true, true, true, true} ));
                    
        table.setRowStyle("white-space:nowrap");
        table.setBlankWhenSame(true);
        for (ExamAssignmentInfo exam : exams) {
            if (form.getShowSections()) {
                for (ExamSectionInfo section : exam.getSections()) {
                    if (student) {
                        for (Long studentId : section.getStudentIds()) {
                            Student s = students.get(studentId);
                            if (s==null) continue;
                            if (!match(form, s.getExternalUniqueId()) && !match(form, s.getName(nameFormat))) continue;
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        s.getExternalUniqueId(),
                                        s.getName(nameFormat),
                                        (html?"<a name='"+exam.getExamId()+"'>":"")+section.getName()+(html?"</a>":""),
                                        exam.getDate(html),
                                        exam.getTime(html),
                                        exam.getRoomsName(html,", "),
                                        exam.getInstructorName("; ")
                                    },
                                    new Comparable[] {
                                        new MultiComparable(s.getExternalUniqueId(), section.getName(), exam),
                                        new MultiComparable(s.getName(nameFormat), s.getExternalUniqueId(), section.getName(), exam),
                                        new MultiComparable(section.getName(), exam),
                                        new MultiComparable(exam.getPeriodOrd(), section.getName(), exam),
                                        new MultiComparable(exam.getPeriod()==null?-1:exam.getPeriod().getStartSlot(), section.getName(), exam),
                                        new MultiComparable(exam.getRoomsName(":"), section.getName(), exam),
                                        new MultiComparable(exam.getInstructorName(":"), section.getName(), exam)
                                    });
                        }
                    } else {
                        for (ExamInstructorInfo instructor : section.getExam().getInstructors()) {
                            if (!match(form, instructor.getExternalUniqueId()) && !match(form, instructor.getName())) continue;
                            table.addLine(
                                    "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                    new String[] {
                                        instructor.getExternalUniqueId(),
                                        instructor.getName(),
                                        (html?"<a name='"+exam.getExamId()+"'>":"")+section.getName()+(html?"</a>":""),
                                        exam.getDate(html),
                                        exam.getTime(html),
                                        exam.getRoomsName(html,", ")
                                    },
                                    new Comparable[] {
                                        new MultiComparable(instructor.getExternalUniqueId(), section.getName(), exam),
                                        new MultiComparable(instructor.getName(), instructor.getExternalUniqueId(), section.getName(), exam),
                                        new MultiComparable(section.getName(), exam),
                                        new MultiComparable(exam.getPeriodOrd(), section.getName(), exam),
                                        new MultiComparable(exam.getPeriod()==null?-1:exam.getPeriod().getStartSlot(), section.getName(), exam),
                                        new MultiComparable(exam.getRoomsName(":"), section.getName(), exam)
                                    });
                        }
                    }
                }
            } else {
                if (student) {
                    HashSet<Long> studentIds = new HashSet();
                    for (ExamSectionInfo section : exam.getSections()) {
                        studentIds.addAll(section.getStudentIds());
                    }
                    for (Long studentId : studentIds) {
                        Student s = students.get(studentId);
                        if (s==null) continue;
                        if (!match(form, s.getExternalUniqueId()) && !match(form, s.getName(nameFormat))) continue;
                        table.addLine(
                                "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                new String[] {
                                    s.getExternalUniqueId(),
                                    s.getName(nameFormat),
                                    (html?"<a name='"+exam.getExamId()+"'>":"")+exam.getExamName()+(html?"</a>":""),
                                    exam.getDate(html),
                                    exam.getTime(html),
                                    exam.getRoomsName(html,", "),
                                    exam.getInstructorName("; ")
                                },
                                new Comparable[] {
                                    new MultiComparable(s.getExternalUniqueId(), exam),
                                    new MultiComparable(s.getName(nameFormat), s.getExternalUniqueId(), exam),
                                    new MultiComparable(exam),
                                    new MultiComparable(exam.getPeriodOrd(), exam),
                                    new MultiComparable(exam.getPeriod()==null?-1:exam.getPeriod().getStartSlot(), exam),
                                    new MultiComparable(exam.getRoomsName(":"), exam),
                                    new MultiComparable(exam.getInstructorName(":"), exam)
                                });
                    }                        
                } else {
                    for (ExamInstructorInfo instructor : exam.getInstructors()) {
                        if (!match(form, instructor.getExternalUniqueId()) && !match(form, instructor.getName())) continue;
                        table.addLine(
                                "onClick=\"document.location='examDetail.do?examId="+exam.getExamId()+"';\"",
                                new String[] {
                                    instructor.getExternalUniqueId(),
                                    instructor.getName(),
                                    (html?"<a name='"+exam.getExamId()+"'>":"")+exam.getExamName()+(html?"</a>":""),
                                    exam.getDate(html),
                                    exam.getTime(html),
                                    exam.getRoomsName(html,", ")
                                },
                                new Comparable[] {
                                    new MultiComparable(instructor.getExternalUniqueId(), exam),
                                    new MultiComparable(instructor.getName(), instructor.getExternalUniqueId(), exam),
                                    new MultiComparable(exam),
                                    new MultiComparable(exam.getPeriodOrd(), exam),
                                    new MultiComparable(exam.getPeriod()==null?-1:exam.getPeriod().getStartSlot(), exam),
                                    new MultiComparable(exam.getRoomsName(":"), exam)
                                });
                    }
                }
            }
        }
        return table;       
    } 
}

