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
package org.unitime.timetable.test;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;
import org.unitime.timetable.model.AcademicAreaClassification;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.ClassWaitList;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.CourseOfferingReservation;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.LastLikeCourseDemand;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.SessionDAO;

import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.studentsct.StudentSectioningLoader;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
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

/**
 * @author Tomas Muller
 */
public class BatchStudentSectioningLoader extends StudentSectioningLoader {
    private static Log sLog = LogFactory.getLog(BatchStudentSectioningLoader.class);
    private boolean iIncludeCourseDemands = true;
    private boolean iIncludeLastLikeStudents = true;
    private boolean iIncludeUseCommittedAssignments = false;
    private String iInitiative = null;
    private String iTerm = null;
    private String iYear = null;

    public BatchStudentSectioningLoader(StudentSectioningModel model) {
        super(model);
        iIncludeCourseDemands = model.getProperties().getPropertyBoolean("Load.IncludeCourseDemands", iIncludeCourseDemands);
        iIncludeLastLikeStudents = model.getProperties().getPropertyBoolean("Load.IncludeLastLikeStudents", iIncludeLastLikeStudents);
        iIncludeUseCommittedAssignments = model.getProperties().getPropertyBoolean("Load.IncludeUseCommittedAssignments", iIncludeUseCommittedAssignments);
        iInitiative = model.getProperties().getProperty("Data.Initiative");
        iYear = model.getProperties().getProperty("Data.Year");
        iTerm = model.getProperties().getProperty("Data.Term");
    }
    
    public void load() throws Exception {
        Session session = Session.getSessionUsingInitiativeYearTerm(iInitiative, iYear, iTerm);
        
        if (session==null) throw new Exception("Session "+iInitiative+" "+iTerm+iYear+" not found!");
        
        load(session);
    }
    
    private static String getInstructorIds(Class_ clazz) {
        if (!clazz.isDisplayInstructor().booleanValue()) return null;
        String ret = null;
        TreeSet ts = new TreeSet(clazz.getClassInstructors());
        for (Iterator i=ts.iterator();i.hasNext();) {
            ClassInstructor ci = (ClassInstructor)i.next();
            if (!ci.isLead().booleanValue()) continue;
            if (ret==null)
                ret = ci.getInstructor().getUniqueId().toString();
            else
                ret += ":"+ci.getInstructor().getUniqueId().toString();
        }
        return ret;
    }
    
    private static String getInstructorNames(Class_ clazz) {
        if (!clazz.isDisplayInstructor().booleanValue()) return null;
        String ret = null;
        TreeSet ts = new TreeSet(clazz.getClassInstructors());
        for (Iterator i=ts.iterator();i.hasNext();) {
            ClassInstructor ci = (ClassInstructor)i.next();
            if (!ci.isLead().booleanValue()) continue;
            if (ret==null)
                ret = ci.getInstructor().nameShort();
            else
                ret += ":"+ci.getInstructor().nameShort();
        }
        return ret;
    }

    private static Offering loadOffering(InstructionalOffering io, Hashtable courseTable, Hashtable classTable) {
        sLog.debug("Loading offering "+io.getCourseName());
        if (!io.hasClasses()) {
            sLog.debug("  -- offering "+io.getCourseName()+" has no class");
            return null;
        }
        Offering offering = new Offering(io.getUniqueId().longValue(), io.getCourseName());
        for (Iterator i=io.getCourseOfferings().iterator();i.hasNext();) {
            CourseOffering co = (CourseOffering)i.next();
            int projected = (co.getProjectedDemand()==null?0:co.getProjectedDemand().intValue());
            int limit = co.getInstructionalOffering().getLimit().intValue();
            for (Iterator j=co.getInstructionalOffering().getCourseReservations().iterator();j.hasNext();) {
                CourseOfferingReservation reservation = (CourseOfferingReservation)j.next();
                if (reservation.getCourseOffering().equals(co) && reservation.getReserved()!=null)
                    limit = reservation.getReserved().intValue();
            }
            Course course = new Course(co.getUniqueId().longValue(), co.getSubjectAreaAbbv(), co.getCourseNbr(), offering, limit, projected);
            courseTable.put(co.getUniqueId(), course);
            sLog.debug("  -- created course "+course);
        }
        Hashtable class2section = new Hashtable();
        Hashtable ss2subpart = new Hashtable();
        for (Iterator i=io.getInstrOfferingConfigs().iterator();i.hasNext();) {
            InstrOfferingConfig ioc = (InstrOfferingConfig)i.next();
            if (!ioc.hasClasses()) {
                sLog.debug("  -- config "+ioc.getName()+" has no class");
                continue;
            }
            Config config = new Config(ioc.getUniqueId().longValue(), ioc.getCourseName()+" ["+ioc.getName()+"]", offering);
            sLog.debug("  -- created config "+config);
            TreeSet subparts = new TreeSet(new SchedulingSubpartComparator());
            subparts.addAll(ioc.getSchedulingSubparts());
            for (Iterator j=subparts.iterator();j.hasNext();) {
                SchedulingSubpart ss = (SchedulingSubpart)j.next();
                String sufix = ss.getSchedulingSubpartSuffix();
                Subpart parentSubpart = (ss.getParentSubpart()==null?null:(Subpart)ss2subpart.get(ss.getParentSubpart()));
                if (ss.getParentSubpart()!=null && parentSubpart==null) {
                    sLog.error("    -- subpart "+ss.getSchedulingSubpartLabel()+" has parent "+ss.getParentSubpart().getSchedulingSubpartLabel()+", but the appropriate parent subpart is not loaded.");
                }
                Subpart subpart = new Subpart(ss.getUniqueId().longValue(), ss.getItype().getItype().toString()+sufix, ss.getItypeDesc().trim()+(sufix==null || sufix.length()==0?"":" ("+sufix+")"), config, parentSubpart);
                ss2subpart.put(ss, subpart);
                sLog.debug("    -- created subpart "+subpart);
                for (Iterator k=ss.getClasses().iterator();k.hasNext();) {
                    Class_ c = (Class_)k.next();
                    Assignment a = c.getCommittedAssignment();
                    int limit = c.getClassLimit();
                    if (ioc.isUnlimitedEnrollment().booleanValue()) limit = -1;
                    Section parentSection = (c.getParentClass()==null?null:(Section)class2section.get(c.getParentClass()));
                    if (c.getParentClass()!=null && parentSection==null) {
                        sLog.error("    -- class "+c.getClassLabel()+" has parent "+c.getParentClass().getClassLabel()+", but the appropriate parent section is not loaded.");
                    }
                    Section section = new Section(c.getUniqueId().longValue(), limit, c.getClassLabel(), subpart, (a==null?null:a.getPlacement()), getInstructorIds(c), getInstructorNames(c), parentSection);
                    if (section.getTime()!=null && section.getTime().getDatePatternId().equals(c.getSession().getDefaultDatePattern().getUniqueId()))
                        section.getTime().setDatePattern(section.getTime().getDatePatternId(),"",section.getTime().getWeekCode());
                    class2section.put(c, section);
                    classTable.put(c.getUniqueId(), section);
                    sLog.debug("      -- created section "+section);
                }
            }
        }
        return offering;
    }
    
    public static Student loadStudent(org.unitime.timetable.model.Student s, Hashtable courseTable, Hashtable classTable) {
        sLog.debug("Loading student "+s.getUniqueId()+" (id="+s.getExternalUniqueId()+", name="+s.getFirstName()+" "+s.getMiddleName()+" "+s.getLastName()+")");
        Student student = new Student(s.getUniqueId().longValue());
        loadStudentInfo(student,s);
        int priority = 0;
        for (Iterator i=new TreeSet(s.getCourseDemands()).iterator();i.hasNext();) {
            CourseDemand cd = (CourseDemand)i.next();
            if (cd.getFreeTime()!=null) {
                Request request = new FreeTimeRequest(
                        cd.getUniqueId().longValue(),
                        priority++,
                        cd.isAlternative().booleanValue(),
                        student,
                        new TimeLocation(
                                cd.getFreeTime().getDayCode().intValue(),
                                cd.getFreeTime().getStartSlot().intValue(),
                                cd.getFreeTime().getLength().intValue(),
                                0, 0, 
                                s.getSession().getDefaultDatePattern().getUniqueId(),
                                "",
                                s.getSession().getDefaultDatePattern().getPatternBitSet(),
                                0)
                        );
                sLog.debug("  -- added request "+request);
            } else if (!cd.getCourseRequests().isEmpty()) {
                Vector courses = new Vector();
                HashSet selChoices = new HashSet();
                HashSet wlChoices = new HashSet();
                HashSet assignedSections = new HashSet();
                Config assignedConfig = null;
                for (Iterator j=new TreeSet(cd.getCourseRequests()).iterator();j.hasNext();) {
                    org.unitime.timetable.model.CourseRequest cr = (org.unitime.timetable.model.CourseRequest)j.next();
                    Course course = (Course)courseTable.get(cr.getCourseOffering().getUniqueId());
                    if (course==null) {
                        sLog.warn("  -- course "+cr.getCourseOffering().getCourseName()+" not loaded");
                        continue;
                    }
                    for (Iterator k=cr.getClassWaitLists().iterator();k.hasNext();) {
                        ClassWaitList cwl = (ClassWaitList)k.next();
                        Section section = course.getOffering().getSection(cwl.getClazz().getUniqueId().longValue());
                        if (section!=null) {
                            if (cwl.getType().equals(ClassWaitList.TYPE_SELECTION))
                                selChoices.add(section.getChoice());
                            else if (cwl.getType().equals(ClassWaitList.TYPE_WAITLIST))
                                wlChoices.add(section.getChoice());
                        }
                    }
                    if (assignedConfig==null) {
                        for (Iterator k=cr.getClassEnrollments().iterator();k.hasNext();) {
                            StudentClassEnrollment sce = (StudentClassEnrollment)k.next();
                            Section section = course.getOffering().getSection(sce.getClazz().getUniqueId().longValue());
                            if (section!=null) {
                                assignedSections.add(section);
                                assignedConfig = section.getSubpart().getConfig();
                            }
                        }
                    }
                    courses.addElement(course);
                }
                if (courses.isEmpty()) continue;
                CourseRequest request = new CourseRequest(
                        cd.getUniqueId().longValue(),
                        priority++,
                        cd.isAlternative().booleanValue(),
                        student,
                        courses,
                        cd.isWaitlist().booleanValue());
                request.getSelectedChoices().addAll(selChoices);
                request.getWaitlistedChoices().addAll(wlChoices);
                if (assignedConfig!=null && assignedSections.size()==assignedConfig.getSubparts().size()) {
                    Enrollment enrollment = new Enrollment(request, 0, assignedConfig, assignedSections);
                    request.setInitialAssignment(enrollment);
                }
                sLog.debug("  -- added request "+request);
            } else {
                sLog.warn("  -- course demand "+cd.getUniqueId()+" has no course requests");
            }
        }
        
        return student;
    }
    
    public static double getLastLikeStudentWeight(Course course, Hashtable demands) {
        int projected = course.getProjected();
        int limit = course.getLimit();
        if (projected<=0) {
            sLog.warn("  -- No projected demand for course "+course.getName()+", using course limit ("+limit+")");
            projected = limit;
        } else if (limit<projected) {
            sLog.warn("  -- Projected number of students is over course limit for course "+course.getName()+" ("+Math.round(projected)+">"+limit+")");
            projected = limit;
        }
        Number lastLike = (Number)demands.get(new Long(course.getId()));
        if (lastLike==null) {
            sLog.warn("  -- No last like info for course "+course.getName());
            return 1.0;
        }
        double weight = ((double)projected) / lastLike.doubleValue(); 
        sLog.debug("  -- last like student weight for "+course.getName()+" is "+weight+" (lastLike="+Math.round(lastLike.doubleValue())+", projected="+projected+")");
        return weight;
    }
    
    public static void loadLastLikeStudent(org.hibernate.Session hibSession, LastLikeCourseDemand d, org.unitime.timetable.model.Student s, CourseOffering co, Hashtable studentTable, Hashtable courseTable, Hashtable classTable, Hashtable demands, Hashtable classAssignments) {
        sLog.debug("Loading last like demand of student "+s.getUniqueId()+" (id="+s.getExternalUniqueId()+", name="+s.getFirstName()+" "+s.getMiddleName()+" "+s.getLastName()+") for "+co.getCourseName());
        Student student = (Student)studentTable.get(s.getUniqueId());
        if (student==null) {
            student = new Student(s.getUniqueId().longValue(),true);
            loadStudentInfo(student,s);
            studentTable.put(s.getUniqueId(),student);
        }
        int priority = student.getRequests().size();
        Vector courses = new Vector();
        Course course = (Course)courseTable.get(co.getUniqueId());
        if (course==null) {
            sLog.warn("  -- course "+co.getCourseName()+" not loaded");
            return;
        }
        courses.addElement(course);
        CourseRequest request = new CourseRequest(
                d.getUniqueId().longValue(),
                priority++,
                false,
                student,
                courses,
                false);
        request.setWeight(getLastLikeStudentWeight(course, demands));
        sLog.debug("  -- added request "+request);
        if (classAssignments!=null && !classAssignments.isEmpty()) {
            HashSet assignedSections = new HashSet();
            HashSet classIds = (HashSet)classAssignments.get(s.getUniqueId());
            if (classIds!=null)
                for (Iterator i=classIds.iterator();i.hasNext();) {
                    Long classId = (Long)i.next();
                    Section section = (Section)request.getSection(classId.longValue());
                    if (section!=null) assignedSections.add(section);
                }
            if (!assignedSections.isEmpty()) {
                sLog.debug("    -- committed assignment: "+assignedSections);
                for (Enumeration e=request.values().elements();e.hasMoreElements();) {
                    Enrollment enrollment = (Enrollment)e.nextElement();
                    if (enrollment.getAssignments().containsAll(assignedSections)) {
                        request.setInitialAssignment(enrollment);
                        sLog.debug("      -- found: "+enrollment);
                        break;
                    }
                }
            }
        }
    }
    
    public static void loadStudentInfo(Student student, org.unitime.timetable.model.Student s) {
        for (Iterator i=s.getAcademicAreaClassifications().iterator();i.hasNext();) {
            AcademicAreaClassification aac = (AcademicAreaClassification)i.next();
            student.getAcademicAreaClasiffications().add(aac.getAcademicArea().getAcademicAreaAbbreviation()+":"+aac.getAcademicClassification().getCode());
            sLog.debug("  -- aac: "+aac.getAcademicArea().getAcademicAreaAbbreviation()+":"+aac.getAcademicClassification().getCode());
        }
        for (Iterator i=s.getPosMajors().iterator();i.hasNext();) {
            PosMajor major = (PosMajor)i.next();
            student.getMajors().add(major.getCode());
            sLog.debug("  -- mj: "+major.getCode());
        }
    }

    public void load(Session session) {
        org.hibernate.Session hibSession = new SessionDAO().getSession();
        Transaction tx = hibSession.beginTransaction();
        
        try {
            
            Hashtable courseTable = new Hashtable();
            Hashtable classTable = new Hashtable();
            List offerings = hibSession.createQuery(
                    "select distinct io from InstructionalOffering io " +
                    "left join fetch io.courseOfferings as co "+
                    "left join fetch io.instrOfferingConfigs as ioc "+
                    "left join fetch ioc.schedulingSubparts as ss "+
                    "left join fetch ss.classes as c "+
                    "where " +
                    "io.session.uniqueId=:sessionId and io.notOffered=false").
                    setLong("sessionId",session.getUniqueId().longValue()).
                    setFetchSize(1000).list();
            for (Iterator i=offerings.iterator();i.hasNext();) {
                InstructionalOffering io = (InstructionalOffering)i.next(); 
                Offering offering = loadOffering(io, courseTable, classTable);
                if (offering!=null) getModel().addOffering(offering);
            }
            
            HashSet loadedStudentIds = new HashSet();
            if (iIncludeCourseDemands) {
                List students = hibSession.createQuery(
                        "select distinct s from Student s " +
                        "left join fetch s.courseDemands as cd "+
                        "left join fetch cd.courseRequests as cr "+
                        "where s.session.uniqueId=:sessionId").
                        setLong("sessionId",session.getUniqueId().longValue()).
                        setFetchSize(1000).list();
                for (Iterator i=students.iterator();i.hasNext();) {
                    org.unitime.timetable.model.Student s = (org.unitime.timetable.model.Student)i.next();
                    if (s.getCourseDemands().isEmpty()) continue;
                    Student student = loadStudent(s, courseTable, classTable);
                    if (student!=null)
                        getModel().addStudent(student);
                    if (s.getExternalUniqueId()!=null)
                        loadedStudentIds.add(s.getExternalUniqueId());
                }
            }
            
            if (iIncludeLastLikeStudents) {
                Hashtable demands = new Hashtable();
                for (Iterator i=hibSession.createQuery("select co.uniqueId, count(d.uniqueId) from "+
                        "CourseOffering co left join co.demandOffering cx, LastLikeCourseDemand d where co.subjectArea.session.uniqueId=:sessionId and "+
                        "(( d.subjectArea=co.subjectArea and d.courseNbr=co.courseNbr ) or "+
                        " ( d.subjectArea=cx.subjectArea and d.courseNbr=cx.courseNbr )) "+
                        "group by co.uniqueId").setLong("sessionId",session.getUniqueId().longValue()).iterate();i.hasNext();) {
                    Object[] o = (Object[])i.next();
                    Long courceOfferingId = (Long)o[0];
                    Number demand = (Number)o[1];
                    demands.put(courceOfferingId, demand);
                }
                Hashtable classAssignments = null;
                if (iIncludeUseCommittedAssignments) {
                    classAssignments = new Hashtable();
                    for (Iterator i=hibSession.createQuery("select distinct se.studentId, se.clazz.uniqueId from StudentEnrollment se where "+
                            "se.solution.commited=true and se.solution.owner.session.uniqueId=:sessionId").
                            setLong("sessionId",session.getUniqueId().longValue()).iterate();i.hasNext();) {
                        Object[] o = (Object[])i.next();
                        Long studentId = (Long)o[0];
                        Long classId = (Long)o[1];
                        HashSet classIds = (HashSet)classAssignments.get(studentId);
                        if (classIds==null) {
                            classIds = new HashSet();
                            classAssignments.put(studentId, classIds);
                        }
                        classIds.add(classId);
                    }

                }
            
                Hashtable lastLikeStudentTable = new Hashtable();
                for (Iterator i=hibSession.createQuery(
                    "select d, s, c from LastLikeCourseDemand d inner join d.student s, CourseOffering c left join c.demandOffering cx " +
                    "where d.subjectArea.session.uniqueId=:sessionId and " +
                    "((d.subjectArea=c.subjectArea and d.courseNbr=c.courseNbr ) or "+
                    " (d.subjectArea=cx.subjectArea and d.courseNbr=cx.courseNbr)) "+
                    "order by s.uniqueId, d.priority, d.uniqueId").
                    setLong("sessionId",session.getUniqueId().longValue()).iterate();i.hasNext();) {
                    Object[] o = (Object[])i.next();
                    LastLikeCourseDemand d = (LastLikeCourseDemand)o[0];
                    org.unitime.timetable.model.Student s = (org.unitime.timetable.model.Student)o[1];
                    CourseOffering co = (CourseOffering)o[2];
                    if (s.getExternalUniqueId()!=null && loadedStudentIds.contains(s.getExternalUniqueId())) continue;
                    loadLastLikeStudent(hibSession, d, s, co, lastLikeStudentTable, courseTable, classTable, demands, classAssignments);
                }
                for (Enumeration e=lastLikeStudentTable.elements();e.hasMoreElements();) {
                    Student student = (Student)e.nextElement();
                    getModel().addStudent(student);
                }
                if (classAssignments!=null && !classAssignments.isEmpty()) {
                    for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
                        Request request = (Request)e.nextElement();
                        if (request.getInitialAssignment()==null) continue;
                        Set conflicts = getModel().conflictValues(request.getInitialAssignment());
                        if (conflicts.isEmpty())
                            request.assign(0, request.getInitialAssignment());
                        else
                            sLog.debug("Unable to assign "+request.getInitialAssignment()+", conflicts: "+conflicts);
                    }
                }
            }
            
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e);
        } finally {
            hibSession.close();
        }
    }
    
    
}
