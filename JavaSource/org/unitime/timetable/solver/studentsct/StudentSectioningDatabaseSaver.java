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
package org.unitime.timetable.solver.studentsct;

import java.util.Date;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Transaction;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.SectioningInfo;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.StudentSectioningQueue;
import org.unitime.timetable.model.WaitList;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentDAO;

import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.studentsct.StudentSectioningSaver;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * @author Tomas Muller
 */
public class StudentSectioningDatabaseSaver extends StudentSectioningSaver {
    private static Log sLog = LogFactory.getLog(StudentSectioningDatabaseSaver.class);
    private boolean iIncludeCourseDemands = true;
    private boolean iIncludeLastLikeStudents = true;
    private String iInitiative = null;
    private String iTerm = null;
    private String iYear = null;
    private Hashtable<Long,org.unitime.timetable.model.Student> iStudents = null;
    private Hashtable<Long,CourseOffering> iCourses = null;
    private Hashtable<Long,Class_> iClasses = null;
    private Hashtable<String,org.unitime.timetable.model.CourseRequest> iRequests = null;
    
    private int iInsert = 0;
    
    private Progress iProgress = null;

    public StudentSectioningDatabaseSaver(Solver solver) {
        super(solver);
        iIncludeCourseDemands = solver.getProperties().getPropertyBoolean("Load.IncludeCourseDemands", iIncludeCourseDemands);
        iIncludeLastLikeStudents = solver.getProperties().getPropertyBoolean("Load.IncludeLastLikeStudents", iIncludeLastLikeStudents);
        iInitiative = solver.getProperties().getProperty("Data.Initiative");
        iYear = solver.getProperties().getProperty("Data.Year");
        iTerm = solver.getProperties().getProperty("Data.Term");
        iProgress = Progress.getInstance(getModel());
    }
    
    public void save() {
        iProgress.setStatus("Saving solution ...");
        org.hibernate.Session hibSession = null;
        Transaction tx = null;
        try {
            hibSession = SessionDAO.getInstance().getSession();
            hibSession.setFlushMode(FlushMode.MANUAL);
            
            tx = hibSession.beginTransaction(); 

            Session session = Session.getSessionUsingInitiativeYearTerm(iInitiative, iYear, iTerm);
            
            if (session==null) throw new Exception("Session "+iInitiative+" "+iTerm+iYear+" not found!");
            
            save(session, hibSession);
            
            StudentSectioningQueue.allStudentsChanged(hibSession, session.getUniqueId());
            
            tx.commit(); tx = null;
            
        } catch (Exception e) {
            iProgress.fatal("Unable to save student schedule, reason: "+e.getMessage(),e);
            sLog.error(e.getMessage(),e);
            if (tx != null) tx.rollback();
        } finally {
            // here we need to close the session since this code may run in a separate thread
            if (hibSession!=null && hibSession.isOpen()) hibSession.close();
        }
    }
    
    public void flushIfNeeded(org.hibernate.Session hibSession) {
        iInsert++;
        if ((iInsert%1000)==0) {
            hibSession.flush(); hibSession.clear();
        }
    }
    
    public void flush(org.hibernate.Session hibSession) {
        hibSession.flush(); hibSession.clear();
        iInsert=0;
    }

    
    public void saveStudent(org.hibernate.Session hibSession, Student student) {
        org.unitime.timetable.model.Student s = iStudents.get(student.getId());
        if (s==null)
        	s = StudentDAO.getInstance().get(student.getId(), hibSession);
        if (s==null) {
            iProgress.warn("Student "+student.getId()+" not found.");
            return;
        }
        for (Iterator<StudentClassEnrollment> i = s.getClassEnrollments().iterator(); i.hasNext(); ) {
            StudentClassEnrollment sce = i.next();
            sce.getClazz().getStudentEnrollments().remove(sce);
            if (sce.getCourseRequest() != null)
            	sce.getCourseRequest().getClassEnrollments().remove(sce);
            hibSession.delete(sce); i.remove();
        }
        for (Iterator<WaitList> i = s.getWaitlists().iterator(); i.hasNext(); ) {
            WaitList wl = i.next();
            hibSession.delete(wl); i.remove();
        }
        for (Iterator e=student.getRequests().iterator();e.hasNext();) {
            Request request = (Request)e.next();
            Enrollment enrollment = (Enrollment)request.getAssignment();
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest)request;
                if (enrollment==null) {
                    if (courseRequest.isWaitlist() && student.canAssign(courseRequest)) {
                        CourseOffering co = iCourses.get(courseRequest.getCourses().get(0).getId());
                        if (co == null) {
                        	iProgress.warn("Course offering " + courseRequest.getCourses().get(0).getId() + " not found.");
                        	continue;
                        }
                        WaitList wl = new WaitList();
                        wl.setStudent(s);
                        wl.setCourseOffering(co);
                        wl.setTimestamp(new Date());
                        wl.setType(new Integer(0));
                        s.getWaitlists().add(wl);
                        hibSession.save(wl);
                    }
                } else {
                    org.unitime.timetable.model.CourseRequest cr = iRequests.get(request.getId()+":"+enrollment.getOffering().getId());
                    if (cr != null)
                    	cr.getClassEnrollments().clear();
                    for (Iterator j=enrollment.getAssignments().iterator();j.hasNext();) {
                        Section section = (Section)j.next();
                        Class_ clazz = iClasses.get(section.getId());
                        if (clazz == null) {
                        	iProgress.warn("Class " + section.getId() + " not found.");
                        	continue;
                        }
                        StudentClassEnrollment sce = new StudentClassEnrollment();
                        sce.setStudent(s);
                        sce.setClazz(clazz);
                        if (cr == null) {
                        	CourseOffering co = iCourses.get(enrollment.getCourse().getId());
                        	if (co == null)
                        		co = clazz.getSchedulingSubpart().getControllingCourseOffering();
                        	sce.setCourseOffering(co);
                        } else {
                            sce.setCourseRequest(cr);
                            sce.setCourseOffering(cr.getCourseOffering());
                        	cr.getClassEnrollments().add(sce);
                        }
                        sce.setTimestamp(new Date());
                        s.getClassEnrollments().add(sce);
                        hibSession.save(sce);
                    }
                    if (cr != null)
                    	hibSession.saveOrUpdate(cr);
                }
            }
        }
        hibSession.saveOrUpdate(s);
    }    
    
    public void save(Session session, org.hibernate.Session hibSession) {
        iClasses = new Hashtable<Long, Class_>();
        iProgress.setPhase("Loading classes...", 1);
        for (Iterator i=Class_.findAll(hibSession, session.getUniqueId()).iterator();i.hasNext();) {
            Class_ clazz = (Class_)i.next();
            iClasses.put(clazz.getUniqueId(),clazz);
        }
        iProgress.incProgress();
        
        if (iIncludeCourseDemands) {
            iStudents = new Hashtable();
            iCourses = new Hashtable();
            iRequests = new Hashtable();
            List courseDemands = CourseDemand.findAll(hibSession, session.getUniqueId());
            iProgress.setPhase("Loading course demands...", courseDemands.size());
            for (Iterator i=courseDemands.iterator();i.hasNext();) {
                CourseDemand demand = (CourseDemand)i.next(); iProgress.incProgress();
                iStudents.put(demand.getStudent().getUniqueId(), demand.getStudent());
                for (Iterator j=demand.getCourseRequests().iterator();j.hasNext();) {
                    org.unitime.timetable.model.CourseRequest request = (org.unitime.timetable.model.CourseRequest)j.next();
                    iRequests.put(demand.getUniqueId()+":"+request.getCourseOffering().getInstructionalOffering().getUniqueId(), request);
                    iCourses.put(request.getCourseOffering().getUniqueId(), request.getCourseOffering());
                }
            }
            iProgress.setPhase("Saving student enrollments...", getModel().getStudents().size());
            for (Iterator e=getModel().getStudents().iterator();e.hasNext();) {
                Student student = (Student)e.next(); iProgress.incProgress();
                if (student.isDummy()) continue;
                saveStudent(hibSession, student);
            }
            flush(hibSession);
        }
        
        if (iIncludeLastLikeStudents) {
            iProgress.setPhase("Computing expected/held space for online sectioning...", 0);
            getModel().computeOnlineSectioningInfos();
            iProgress.incProgress();
            
        	Hashtable<Long, SectioningInfo> infoTable = new Hashtable<Long, SectioningInfo>();
        	List<SectioningInfo> infos = hibSession.createQuery(
        			"select i from SectioningInfo i where i.clazz.schedulingSubpart.instrOfferingConfig.instructionalOffering.session.uniqueId = :sessionId")
        			.setLong("sessionId", session.getUniqueId())
        			.list();
        	for (SectioningInfo info : infos)
        		infoTable.put(info.getClazz().getUniqueId(), info);
            
            iProgress.setPhase("Saving expected/held space for online sectioning...", getModel().getOfferings().size());
            for (Iterator e=getModel().getOfferings().iterator();e.hasNext();) {
                Offering offering = (Offering)e.next(); iProgress.incProgress();
                for (Iterator f=offering.getConfigs().iterator();f.hasNext();) {
                    Config config = (Config)f.next();
                    for (Iterator g=config.getSubparts().iterator();g.hasNext();) {
                        Subpart subpart = (Subpart)g.next();
                        for (Iterator h=subpart.getSections().iterator();h.hasNext();) {
                            Section section = (Section)h.next();
                            Class_ clazz = iClasses.get(section.getId());
                            if (clazz==null) continue;
                            SectioningInfo info = infoTable.get(section.getId());
                            if (info==null) {
                                info = new SectioningInfo();
                                info.setClazz(clazz);
                            }
                            info.setNbrExpectedStudents(section.getSpaceExpected());
                            info.setNbrHoldingStudents(section.getSpaceHeld());
                            hibSession.saveOrUpdate(info);
                            flushIfNeeded(hibSession);
                        }
                    }
                }
            }
        }
        
        // Update class enrollments
        iProgress.setPhase("Updating class enrollments...", getModel().getOfferings().size());
        for (Iterator e=getModel().getOfferings().iterator();e.hasNext();) {
            Offering offering = (Offering)e.next(); iProgress.incProgress();
            for (Iterator f=offering.getConfigs().iterator();f.hasNext();) {
                Config config = (Config)f.next();
                for (Iterator g=config.getSubparts().iterator();g.hasNext();) {
                    Subpart subpart = (Subpart)g.next();
                    for (Iterator h=subpart.getSections().iterator();h.hasNext();) {
                        Section section = (Section)h.next();
                        Class_ clazz = iClasses.get(section.getId());
                        if (clazz==null) continue;
                        int enrl = 0;
                        for (Iterator i=section.getEnrollments().iterator();i.hasNext();) {
                        	Enrollment en = (Enrollment)i.next();
                        	if (!en.getStudent().isDummy()) enrl++;
                        }
                        clazz.setEnrollment(enrl);
                        iProgress.debug(section.getName()+" has an enrollment of "+enrl);
                        hibSession.saveOrUpdate(clazz);
                        flushIfNeeded(hibSession);
                    }
                }
            }
        }
        flush(hibSession);
        
        iProgress.setPhase("Done",1);iProgress.incProgress();
    }

}
