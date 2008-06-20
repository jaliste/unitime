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
package org.unitime.timetable.solver.exam.ui;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.ClassEvent;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.DistributionObject;
import org.unitime.timetable.model.DistributionPref;
import org.unitime.timetable.model.Event;
import org.unitime.timetable.model.ExamConflict;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.SolverParameterDef;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.dao.EventDAO;
import org.unitime.timetable.model.dao.ExamPeriodDAO;
import org.unitime.timetable.solver.exam.ExamModel;
import org.unitime.timetable.solver.exam.ExamResourceUnavailability;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamDistributionConstraint;
import net.sf.cpsolver.exam.model.ExamInstructor;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;

/**
 * @author Tomas Muller
 */
public class ExamAssignmentInfo extends ExamAssignment implements Serializable  {
    private TreeSet<DirectConflict> iDirects = new TreeSet();
    private TreeSet<BackToBackConflict> iBackToBacks = new TreeSet();
    private TreeSet<MoreThanTwoADayConflict> iMoreThanTwoADays = new TreeSet();
    private TreeSet<DirectConflict> iInstructorDirects = new TreeSet();
    private TreeSet<BackToBackConflict> iInstructorBackToBacks = new TreeSet();
    private TreeSet<MoreThanTwoADayConflict> iInstructorMoreThanTwoADays = new TreeSet();
    private TreeSet<DistributionConflict> iDistributions = new TreeSet();
    
    public ExamAssignmentInfo(ExamPlacement placement) {
        this((Exam)placement.variable(),placement);
    }

    public ExamAssignmentInfo(Exam exam, ExamPlacement placement) {
        super(exam, placement);
        if (placement!=null) {
            ExamModel model = (ExamModel)exam.getModel();
            Hashtable<Exam,DirectConflict> directs = new Hashtable();
            for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                for (Iterator i=student.getExams(placement.getPeriod()).iterator();i.hasNext();) {
                    Exam other = (Exam)i.next();
                    if (other.equals(exam)) continue;
                    DirectConflict dc = directs.get(other);
                    if (dc==null) {
                        dc = new DirectConflict(new ExamAssignment((ExamPlacement)other.getAssignment()));
                        directs.put(other, dc);
                    } else dc.incNrStudents();
                    dc.getStudents().add(student.getId());
                }
            }
            iDirects.addAll(directs.values());
            int btbDist = model.getBackToBackDistance();
            Hashtable<Exam,BackToBackConflict> backToBacks = new Hashtable();
            for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                if (placement.getPeriod().prev()!=null) {
                    if (model.isDayBreakBackToBack() || placement.getPeriod().prev().getDay()==placement.getPeriod().getDay()) {
                        Set exams = student.getExams(placement.getPeriod().prev());
                        for (Iterator i=exams.iterator();i.hasNext();) {
                            Exam other = (Exam)i.next();
                            double distance = placement.getDistance((ExamPlacement)other.getAssignment());
                            BackToBackConflict btb = backToBacks.get(other);
                            if (btb==null) {
                                btb = new BackToBackConflict(new ExamAssignment((ExamPlacement)other.getAssignment()),
                                        (btbDist<0?false:distance>btbDist), distance);
                                backToBacks.put(other, btb);
                            } else btb.incNrStudents();
                            btb.getStudents().add(student.getId());
                        }
                    }
                }
                if (placement.getPeriod().next()!=null) {
                    if (model.isDayBreakBackToBack() || placement.getPeriod().next().getDay()==placement.getPeriod().getDay()) {
                        Set exams = student.getExams(placement.getPeriod().next());
                        for (Iterator i=exams.iterator();i.hasNext();) {
                            Exam other = (Exam)i.next();
                            BackToBackConflict btb = backToBacks.get(other);
                            double distance = placement.getDistance((ExamPlacement)other.getAssignment());
                            if (btb==null) {
                                btb = new BackToBackConflict(new ExamAssignment((ExamPlacement)other.getAssignment()),
                                        (btbDist<0?false:distance>btbDist), distance);
                                backToBacks.put(other, btb);
                            } else btb.incNrStudents();
                            btb.getStudents().add(student.getId());
                        }
                    }
                }
            }
            iBackToBacks.addAll(backToBacks.values());
            Hashtable<String,MoreThanTwoADayConflict> m2ds = new Hashtable();
            for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                Set exams = student.getExamsADay(placement.getPeriod());
                int nrExams = exams.size() + (exams.contains(exam)?0:1);
                if (nrExams<=2) continue;
                TreeSet examIds = new TreeSet();
                TreeSet otherExams = new TreeSet();
                for (Iterator i=exams.iterator();i.hasNext();) {
                    Exam other = (Exam)i.next();
                    if (other.equals(exam)) continue;
                    examIds.add(other.getId());
                    otherExams.add(new ExamAssignment((ExamPlacement)other.getAssignment()));
                }
                MoreThanTwoADayConflict m2d = m2ds.get(examIds.toString());
                if (m2d==null) {
                    m2d = new MoreThanTwoADayConflict(otherExams);
                    m2ds.put(examIds.toString(), m2d);
                } else m2d.incNrStudents();
                m2d.getStudents().add(student.getId());
            }
            iMoreThanTwoADays.addAll(m2ds.values());

            Hashtable<Exam,DirectConflict> idirects = new Hashtable();
            for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
                ExamInstructor instructor = (ExamInstructor)e.nextElement();
                for (Iterator i=instructor.getExams(placement.getPeriod()).iterator();i.hasNext();) {
                    Exam other = (Exam)i.next();
                    if (other.equals(exam)) continue;
                    DirectConflict dc = idirects.get(other);
                    if (dc==null) {
                        dc = new DirectConflict(new ExamAssignment((ExamPlacement)other.getAssignment()));
                        idirects.put(other, dc);
                    } else dc.incNrStudents();
                    dc.getStudents().add(instructor.getId());
                }
            }
            iInstructorDirects.addAll(idirects.values());

            Hashtable<Exam,BackToBackConflict> ibackToBacks = new Hashtable();
            for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
                ExamInstructor instructor = (ExamInstructor)e.nextElement();
                if (placement.getPeriod().prev()!=null) {
                    if (model.isDayBreakBackToBack() || placement.getPeriod().prev().getDay()==placement.getPeriod().getDay()) {
                        Set exams = instructor.getExams(placement.getPeriod().prev());
                        for (Iterator i=exams.iterator();i.hasNext();) {
                            Exam other = (Exam)i.next();
                            double distance = placement.getDistance((ExamPlacement)other.getAssignment());
                            BackToBackConflict btb = ibackToBacks.get(other);
                            if (btb==null) {
                                btb = new BackToBackConflict(new ExamAssignment((ExamPlacement)other.getAssignment()),
                                        (btbDist<0?false:distance>btbDist), distance);
                                ibackToBacks.put(other, btb);
                            } else btb.incNrStudents();
                            btb.getStudents().add(instructor.getId());
                        }
                    }
                }
                if (placement.getPeriod().next()!=null) {
                    if (model.isDayBreakBackToBack() || placement.getPeriod().next().getDay()==placement.getPeriod().getDay()) {
                        Set exams = instructor.getExams(placement.getPeriod().next());
                        for (Iterator i=exams.iterator();i.hasNext();) {
                            Exam other = (Exam)i.next();
                            BackToBackConflict btb = ibackToBacks.get(other);
                            double distance = placement.getDistance((ExamPlacement)other.getAssignment());
                            if (btb==null) {
                                btb = new BackToBackConflict(new ExamAssignment((ExamPlacement)other.getAssignment()),
                                        (btbDist<0?false:distance>btbDist), distance);
                                ibackToBacks.put(other, btb);
                            } else btb.incNrStudents();
                            btb.getStudents().add(instructor.getId());
                        }
                    }
                }
            }
            iInstructorBackToBacks.addAll(ibackToBacks.values());
            Hashtable<String,MoreThanTwoADayConflict> im2ds = new Hashtable();
            for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
                ExamInstructor instructor = (ExamInstructor)e.nextElement();
                Set exams = instructor.getExamsADay(placement.getPeriod());
                int nrExams = exams.size() + (exams.contains(exam)?0:1);
                if (nrExams<=2) continue;
                TreeSet examIds = new TreeSet();
                TreeSet otherExams = new TreeSet();
                for (Iterator i=exams.iterator();i.hasNext();) {
                    Exam other = (Exam)i.next();
                    if (other.equals(exam)) continue;
                    examIds.add(other.getId());
                    otherExams.add(new ExamAssignment((ExamPlacement)other.getAssignment()));
                }
                MoreThanTwoADayConflict m2d = im2ds.get(examIds.toString());
                if (m2d==null) {
                    m2d = new MoreThanTwoADayConflict(otherExams);
                    im2ds.put(examIds.toString(), m2d);
                } else m2d.incNrStudents();
                m2d.getStudents().add(instructor.getId());
            }
            iInstructorMoreThanTwoADays.addAll(im2ds.values());
            computeUnavailablility(exam, model.getUnavailabilities(placement.getPeriod()));
            for (Enumeration e=exam.getDistributionConstraints().elements();e.hasMoreElements();) {
                ExamDistributionConstraint dc = (ExamDistributionConstraint)e.nextElement();
                if (dc.isHard()) {
                    if (dc.inConflict(placement))
                        iDistributions.add(new DistributionConflict(dc,exam));
                } else {
                    if (!dc.isSatisfied(placement))
                        iDistributions.add(new DistributionConflict(dc,exam));
                }
            }
        }
    }
    
    public ExamAssignmentInfo(org.unitime.timetable.model.Exam exam) {
        this(exam, "true".equals(ApplicationProperties.getProperty("tmtbl.exams.conflicts.cache","true")));
    }
    
    public ExamAssignmentInfo(org.unitime.timetable.model.Exam exam, Hashtable<Long,Set<Long>> owner2students, Hashtable<Long, Set<org.unitime.timetable.model.Exam>> studentExams, Hashtable<Long, Set<Meeting>> period2meetings, Parameters p) {
        super(exam, owner2students);
        Hashtable<Long,Set<org.unitime.timetable.model.Exam>> examStudents = new Hashtable();
        for (ExamSectionInfo section: getSections())
            for (Long studentId : section.getStudentIds())
                examStudents.put(studentId, studentExams.get(studentId));
        generateConflicts(exam, examStudents, null, period2meetings, p);
    }
    
    public ExamAssignmentInfo(org.unitime.timetable.model.Exam exam, boolean useCache) {
        super(exam);
        if (!useCache) {
            generateConflicts(exam, exam.getStudentExams(), null); 
            return;
        }
        if (exam.getConflicts()!=null && !exam.getConflicts().isEmpty()) {
            for (Iterator i=exam.getConflicts().iterator();i.hasNext();) {
                ExamConflict conf = (ExamConflict)i.next();
                if (conf.isDirectConflict()) {
                    ExamAssignment other = null;
                    for (Iterator j=conf.getExams().iterator();j.hasNext();) {
                        org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)j.next();
                        if (x.equals(exam)) continue;
                        if (x.getAssignedPeriod()!=null) other = new ExamAssignment(x);
                    }
                    if (conf.getNrStudents()>0) {
                        iDirects.add(new DirectConflict(other, conf, true));
                        iNrDirectConflicts += conf.getNrStudents();
                    }
                    if (conf.getNrInstructors()>0) {
                        iInstructorDirects.add(new DirectConflict(other, conf, false));
                        iNrInstructorDirectConflicts += conf.getNrInstructors();
                    }
                } else if (conf.isBackToBackConflict()) {
                    ExamAssignment other = null;
                    for (Iterator j=conf.getExams().iterator();j.hasNext();) {
                        org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)j.next();
                        if (x.equals(exam)) continue;
                        if (x.getAssignedPeriod()!=null) other = new ExamAssignment(x);
                    }
                    if (other==null) continue;
                    if (conf.getNrStudents()>0) {
                        iBackToBacks.add(new BackToBackConflict(other, conf, true));
                        iNrBackToBackConflicts += conf.getNrStudents();
                        if (conf.isDistanceBackToBackConflict()) iNrDistanceBackToBackConflicts += conf.getNrStudents();
                    }
                    if (conf.getNrInstructors()>0) {
                        iInstructorBackToBacks.add(new BackToBackConflict(other, conf, false));
                        iNrInstructorBackToBackConflicts += conf.getNrInstructors();
                        if (conf.isDistanceBackToBackConflict()) iNrInstructorDistanceBackToBackConflicts += conf.getNrInstructors();
                    }
                } else if (conf.isMoreThanTwoADayConflict()) {
                    TreeSet other = new TreeSet();
                    for (Iterator j=conf.getExams().iterator();j.hasNext();) {
                        org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)j.next();
                        if (x.equals(exam)) continue;
                        if (x.getAssignedPeriod()!=null) other.add(new ExamAssignment(x));
                    }
                    if (other.size()<2) continue;
                    if (conf.getNrStudents()>0) {
                        iMoreThanTwoADays.add(new MoreThanTwoADayConflict(other, conf, true));
                        iNrMoreThanTwoADayConflicts += conf.getNrStudents();
                    }
                    if (conf.getNrInstructors()>0) {
                        iInstructorMoreThanTwoADays.add(new MoreThanTwoADayConflict(other, conf, false));
                        iNrInstructorMoreThanTwoADayConflicts += conf.getNrInstructors();
                    }
                }
            }
        }
        for (Iterator i=exam.getDistributionObjects().iterator();i.hasNext();) {
            DistributionObject dObj = (DistributionObject)i.next();
            DistributionPref pref = dObj.getDistributionPref();
            if (!check(pref, exam, getPeriod(), getRooms(), null))
                iDistributions.add(new DistributionConflict(pref, exam));
        }
        if (exam.getAssignedPeriod()!=null &&
                "true".equals(ApplicationProperties.getProperty("tmtbl.exam.eventConflicts."+(iExamType==org.unitime.timetable.model.Exam.sExamTypeFinal?"final":"midterm"),"true"))) {
            computeUnavailablility(exam, exam.getAssignedPeriod().getUniqueId());
            for (Iterator i=exam.getInstructors().iterator();i.hasNext();)
                computeUnavailablility((DepartmentalInstructor)i.next(), exam.getAssignedPeriod());
        }
    }
    
    private void computeUnavailablility(Exam exam, Vector<ExamResourceUnavailability> unavailabilities) {
        if (unavailabilities==null || unavailabilities.isEmpty()) return;
        for (ExamResourceUnavailability unavailability : unavailabilities) {
            Vector<Long> commonStudents = new Vector();
            for (Enumeration e=exam.getStudents().elements();e.hasMoreElements();) {
                ExamStudent student = (ExamStudent)e.nextElement();
                if (unavailability.getStudentIds().contains(student.getId())) commonStudents.add(student.getId());
            }
            if (!commonStudents.isEmpty())
                iDirects.add(new DirectConflict(unavailability, commonStudents));
            Vector<Long> commonInstructors = new Vector();
            for (Enumeration e=exam.getInstructors().elements();e.hasMoreElements();) {
                ExamInstructor instructor = (ExamInstructor)e.nextElement();
                if (unavailability.getInstructorIds().contains(instructor.getId())) commonInstructors.add(instructor.getId());
            }
            if (!commonInstructors.isEmpty())
                iInstructorDirects.add(new DirectConflict(unavailability, commonInstructors));
        }
    }
    
    /*
    private void computeUnavailablility(Hashtable<Assignment, Set<Long>> studentAssignments, ExamPeriod period) {
        for (Map.Entry<Assignment, Set<Long>> entry : studentAssignments.entrySet()) {
            if (!period.overlap(entry.getKey())) continue;
            iDirects.add(new DirectConflict(entry.getKey(), entry.getValue()));
        }
    }
    */
    
    private void computeUnavailablility(org.unitime.timetable.model.Exam exam, Long periodId, Hashtable<Long, Set<Meeting>> period2meetings) {
        if (period2meetings==null) {
            computeUnavailablility(exam, periodId);
        } else {
            Set<Meeting> meetings = period2meetings.get(periodId);
            if (meetings!=null) {
                meetings: for (Meeting meeting: meetings) {
                    for (Iterator i=iDirects.iterator();i.hasNext();) {
                        DirectConflict dc = (DirectConflict)i.next();
                        if (meeting.getEvent().getUniqueId().equals(dc.getOtherEventId())) {
                            dc.addMeeting(meeting);
                            continue meetings;
                        }
                    }
                    HashSet<Long> students = new HashSet();
                    for (Iterator i=meeting.getEvent().getStudentIds().iterator();i.hasNext();) {
                        Long studentId = (Long)i.next();
                        for (ExamSectionInfo section: getSections()) 
                            if (section.getStudentIds().contains(studentId)) {
                                students.add(studentId); break;
                            }
                    }
                    iDirects.add(new DirectConflict(meeting, students));
                }
            }
        }
    }

    private void computeUnavailablility(org.unitime.timetable.model.Exam exam, Long periodId) {
        meetings: for (Map.Entry<Meeting, Set<Long>> entry : exam.getOverlappingStudentMeetings(periodId).entrySet()) {
            for (Iterator i=iDirects.iterator();i.hasNext();) {
                DirectConflict dc = (DirectConflict)i.next();
                if (entry.getKey().getEvent().getUniqueId().equals(dc.getOtherEventId())) {
                    dc.addMeeting(entry.getKey());
                    continue meetings;
                }
            }
            iDirects.add(new DirectConflict(entry.getKey(), entry.getValue()));
        }
        meetings: for (Map.Entry<Meeting, Set<Long>> entry : ExamPeriodDAO.getInstance().get(periodId).findOverlappingCourseMeetingsWithReqAttendence(getStudentIds()).entrySet()) {
            for (Iterator i=iDirects.iterator();i.hasNext();) {
                DirectConflict dc = (DirectConflict)i.next();
                if (entry.getKey().getEvent().getUniqueId().equals(dc.getOtherEventId())) {
                    dc.addMeeting(entry.getKey());
                    continue meetings;
                }
            }
            iDirects.add(new DirectConflict(entry.getKey(), entry.getValue()));
        }
    }
    
    private void computeUnavailablility(DepartmentalInstructor instructor, ExamPeriod period, Hashtable<Long, Set<Meeting>> period2meetings) {
        if (period2meetings==null) {
            computeUnavailablility(instructor, period);
        } else {
            Set<Meeting> meetings = period2meetings.get(period.getUniqueId());
            if (meetings!=null) {
                meetings: for (Meeting meeting: meetings) {
                    if (!(meeting.getEvent() instanceof ClassEvent)) continue;
                    Class_ clazz = ((ClassEvent)meeting.getEvent()).getClazz();
                    for (Iterator i=clazz.getClassInstructors().iterator();i.hasNext();) {
                        ClassInstructor ci = (ClassInstructor)i.next();
                        if (ci.isLead() && (ci.getInstructor().getUniqueId().equals(instructor.getUniqueId()) ||
                           (ci.getInstructor().getExternalUniqueId()!=null && ci.getInstructor().getExternalUniqueId().equals(instructor.getExternalUniqueId())))) {
                            for (Iterator j=iInstructorDirects.iterator();j.hasNext();) {
                                DirectConflict dc = (DirectConflict)j.next();
                                if (meeting.getEvent().getUniqueId().equals(dc.getOtherEventId())) {
                                    dc.incNrStudents();
                                    dc.getStudents().add(instructor.getUniqueId());
                                    dc.addMeeting(meeting);
                                    continue meetings;
                                }
                            }
                            DirectConflict dc = new DirectConflict(meeting);
                            dc.getStudents().add(instructor.getUniqueId());
                            iInstructorDirects.add(dc);
                            break;
                        }
                    }
                }
            }
        }

    }
    
    private void computeUnavailablility(DepartmentalInstructor instructor, ExamPeriod period) {
        for (Iterator j=instructor.getClasses().iterator();j.hasNext();) {
            ClassInstructor ci = (ClassInstructor)j.next();
            if (!ci.isLead()) continue;
            meetings: for (Iterator k=period.findOverlappingClassMeetings(ci.getClassInstructing().getUniqueId()).iterator();k.hasNext();) {
                Meeting meeting = (Meeting)k.next();
                for (Iterator i=iInstructorDirects.iterator();i.hasNext();) {
                    DirectConflict dc = (DirectConflict)i.next();
                    if (meeting.getEvent().getUniqueId().equals(dc.getOtherEventId())) {
                        dc.incNrStudents();
                        dc.getStudents().add(instructor.getUniqueId());
                        dc.addMeeting(meeting);
                        continue meetings;
                    }
                }
                DirectConflict dc = new DirectConflict(meeting);
                dc.getStudents().add(instructor.getUniqueId());
                iInstructorDirects.add(dc);
            }
        }
    }

    public boolean check(DistributionPref pref, org.unitime.timetable.model.Exam exam, ExamPeriod assignedPeriod, Collection<ExamRoomInfo> assignedRooms, Hashtable<Long,ExamAssignment> table) {
        if (PreferenceLevel.sNeutral.equals(pref.getPrefLevel().getPrefProlog())) return true;
        boolean positive = 
            PreferenceLevel.sRequired.equals(pref.getPrefLevel().getPrefProlog()) ||
            PreferenceLevel.sStronglyPreferred.equals(pref.getPrefLevel().getPrefProlog()) ||
            PreferenceLevel.sPreferred.equals(pref.getPrefLevel().getPrefProlog());
        if ("EX_SAME_PER".equals(pref.getDistributionType().getReference())) {
            if (positive) { //same period
                ExamPeriod period = null;
                for (Iterator i=pref.getDistributionObjects().iterator();i.hasNext();) {
                    org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)((DistributionObject)i.next()).getPrefGroup();
                    ExamPeriod p = (x.equals(exam)?assignedPeriod:getAssignedPeriod(x,table));
                    if (p==null) continue;
                    if (period==null) period = p;
                    else if (!period.equals(p)) return false;
                }
                return true;
            } else { //different period
                HashSet periods = new HashSet();
                for (Iterator i=pref.getDistributionObjects().iterator();i.hasNext();) {
                    org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)((DistributionObject)i.next()).getPrefGroup();
                    ExamPeriod p = (x.equals(exam)?assignedPeriod:getAssignedPeriod(x,table));
                    if (p==null) continue;
                    if (!periods.add(p)) return false;
                }
                return true;
            }
        } else if ("EX_PRECEDENCE".equals(pref.getDistributionType().getReference())) {
            TreeSet distObjects = new TreeSet(
                    positive?new Comparator<DistributionObject>() {
                        public int compare(DistributionObject d1, DistributionObject d2) {
                            return d1.getSequenceNumber().compareTo(d2.getSequenceNumber());
                        }
                    }:new Comparator<DistributionObject>() {
                        public int compare(DistributionObject d1, DistributionObject d2) {
                            return d2.getSequenceNumber().compareTo(d1.getSequenceNumber());
                        }
                    });
            distObjects.addAll(pref.getDistributionObjects());
            ExamPeriod prev = null;
            for (Iterator i=distObjects.iterator();i.hasNext();) {
                org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)((DistributionObject)i.next()).getPrefGroup();
                ExamPeriod p = (x.equals(exam)?assignedPeriod:getAssignedPeriod(x,table));
                if (p==null) continue;
                if (prev!=null && prev.compareTo(p)>=0) return false;
                prev = p;
            }
            return true;
        } else if ("EX_SAME_ROOM".equals(pref.getDistributionType().getReference())) {
            if (positive) { //same room
                Collection<ExamRoomInfo> rooms = null;
                for (Iterator i=pref.getDistributionObjects().iterator();i.hasNext();) {
                    org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)((DistributionObject)i.next()).getPrefGroup();
                    Collection<ExamRoomInfo> r = (x.equals(exam)?assignedRooms:getAssignedRooms(x, table));
                    if (r==null) continue;
                    if (rooms==null) rooms = r;
                    else if (!rooms.containsAll(r) && !r.containsAll(rooms)) return false;
                }
                return true;
            } else { //different room
                Collection<ExamRoomInfo> allRooms = new HashSet();
                for (Iterator i=pref.getDistributionObjects().iterator();i.hasNext();) {
                    org.unitime.timetable.model.Exam x = (org.unitime.timetable.model.Exam)((DistributionObject)i.next()).getPrefGroup();
                    Collection<ExamRoomInfo> r = (x.equals(exam)?assignedRooms:getAssignedRooms(x, table));
                    if (r==null) continue;
                    for (ExamRoomInfo room : r) {
                        if (!allRooms.add(room)) return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    public static ExamPeriod getAssignedPeriod(org.unitime.timetable.model.Exam exam, Hashtable<Long, ExamAssignment> table) {
        ExamAssignment assignment = (table==null?null:table.get(exam.getUniqueId()));
        return (assignment==null?exam.getAssignedPeriod():assignment.getPeriod());
    }
    
    public static TreeSet<ExamRoomInfo> getAssignedRooms(org.unitime.timetable.model.Exam exam, Hashtable<Long, ExamAssignment> table) {
        ExamAssignment assignment = (table==null?null:table.get(exam.getUniqueId()));
        if (assignment!=null) return assignment.getRooms();
        TreeSet<ExamRoomInfo> rooms = new TreeSet();
        for (Iterator i=exam.getAssignedRooms().iterator();i.hasNext();) {
            Location location = (Location)i.next();
            rooms.add(new ExamRoomInfo(location,0));
        }
        return rooms;
    }
    
    public static ExamAssignment getAssignment(org.unitime.timetable.model.Exam exam, Hashtable<Long, ExamAssignment> table) {
        ExamAssignment assignment = (table==null?null:table.get(exam.getUniqueId()));
        return (assignment==null?new ExamAssignment(exam):assignment);
    }

    public ExamAssignmentInfo(org.unitime.timetable.model.Exam exam, ExamPeriod period, Collection<ExamRoomInfo> rooms) throws Exception {
        this(exam, period, rooms, (period==null?null:exam.getStudentExams()), null);
    }
    
    public ExamAssignmentInfo(org.unitime.timetable.model.Exam exam, ExamPeriod period, Collection<ExamRoomInfo> rooms, Hashtable<Long, ExamAssignment> table) throws Exception {
        this(exam, period, rooms, exam.getStudentExams(), table);
    }
    
    
    public ExamAssignmentInfo(org.unitime.timetable.model.Exam exam, ExamPeriod period, Collection<ExamRoomInfo> rooms, Hashtable<Long, Set<org.unitime.timetable.model.Exam>> examStudents, Hashtable<Long, ExamAssignment> table) throws Exception {
        super(exam, period, rooms);
        if (period!=null) generateConflicts(exam, examStudents, table);
    }
   
    public ExamAssignmentInfo(org.unitime.timetable.model.Exam exam, Hashtable<Long, ExamAssignment> table) {
        super(exam);
        generateConflicts(exam, exam.getStudentExams(), table);
    }
    
    public void generateConflicts(org.unitime.timetable.model.Exam exam, Hashtable<Long, Set<org.unitime.timetable.model.Exam>> examStudents, Hashtable<Long, ExamAssignment> table) {
        generateConflicts(exam, examStudents, table, null, new Parameters(exam.getSession().getUniqueId(), exam.getExamType()));
    }
    
    public void generateConflicts(org.unitime.timetable.model.Exam exam, Hashtable<Long, Set<org.unitime.timetable.model.Exam>> examStudents, Hashtable<Long, ExamAssignment> table, Hashtable<Long, Set<Meeting>> period2meetings, Parameters p) {
        if (getPeriod()==null) return;
        
        Hashtable<org.unitime.timetable.model.Exam,DirectConflict> directs = new Hashtable();
        Hashtable<org.unitime.timetable.model.Exam,BackToBackConflict> backToBacks = new Hashtable();
        Hashtable<String,MoreThanTwoADayConflict> m2ds = new Hashtable();
        for (Entry<Long,Set<org.unitime.timetable.model.Exam>> studentExams : examStudents.entrySet()) {
            TreeSet sameDateExams = new TreeSet();
            for (org.unitime.timetable.model.Exam other : studentExams.getValue()) {
                if (other.equals(getExam())) continue;
                ExamPeriod otherPeriod = getAssignedPeriod(other, table);
                if (otherPeriod==null) continue;
                if (getPeriod().equals(otherPeriod)) { //direct conflict
                    DirectConflict dc = directs.get(other);
                    if (dc==null) {
                        dc = new DirectConflict(getAssignment(other, table));
                        directs.put(other, dc);
                    } else dc.incNrStudents();
                    dc.getStudents().add(studentExams.getKey());
                    iNrDirectConflicts++;
                } else if (p.isBackToBack(getPeriod(),otherPeriod)) {
                    BackToBackConflict btb = backToBacks.get(other);
                    double distance = Location.getDistance(getRooms(), getAssignedRooms(other, table));
                    if (btb==null) {
                        btb = new BackToBackConflict(getAssignment(other, table), (p.getBackToBackDistance()<0?false:distance>p.getBackToBackDistance()), distance);
                        backToBacks.put(other, btb);
                    } else btb.incNrStudents();
                    btb.getStudents().add(studentExams.getKey());
                    iNrBackToBackConflicts++;
                    if (btb.isDistance()) iNrDistanceBackToBackConflicts++;
                }
                if (getPeriod().getDateOffset().equals(otherPeriod.getDateOffset()))
                    sameDateExams.add(other);
            }
            if (sameDateExams.size()>=2) {
                TreeSet examIds = new TreeSet();
                TreeSet otherExams = new TreeSet();
                for (Iterator j=sameDateExams.iterator();j.hasNext();) {
                    org.unitime.timetable.model.Exam other = (org.unitime.timetable.model.Exam)j.next();
                    examIds.add(other.getUniqueId());
                    otherExams.add(getAssignment(other, table));
                }
                MoreThanTwoADayConflict m2d = m2ds.get(examIds.toString());
                if (m2d==null) {
                    m2d = new MoreThanTwoADayConflict(otherExams);
                    m2ds.put(examIds.toString(), m2d);
                } else m2d.incNrStudents();
                iNrMoreThanTwoADayConflicts++;
                m2d.getStudents().add(studentExams.getKey());
            }
        }
        iDirects.addAll(directs.values());
        iBackToBacks.addAll(backToBacks.values());
        iMoreThanTwoADays.addAll(m2ds.values());
        
        if ("true".equals(ApplicationProperties.getProperty("tmtbl.exam.eventConflicts."+(iExamType==org.unitime.timetable.model.Exam.sExamTypeFinal?"final":"midterm"), "true")))
            computeUnavailablility(exam,getPeriodId(),period2meetings);
            
        Hashtable<org.unitime.timetable.model.Exam,DirectConflict> idirects = new Hashtable();
        Hashtable<org.unitime.timetable.model.Exam,BackToBackConflict> ibackToBacks = new Hashtable();
        Hashtable<String,MoreThanTwoADayConflict> im2ds = new Hashtable();
        for (Iterator i=getExam().getInstructors().iterator();i.hasNext();) {
            DepartmentalInstructor instructor = (DepartmentalInstructor)i.next();
            TreeSet sameDateExams = new TreeSet();
            for (Iterator j=instructor.getExams(getExam().getExamType()).iterator();j.hasNext();) {
                org.unitime.timetable.model.Exam other = (org.unitime.timetable.model.Exam)j.next();
                if (other.equals(getExam())) continue;
                ExamPeriod otherPeriod = getAssignedPeriod(other, table);
                if (otherPeriod==null) continue;
                if (getPeriod().equals(otherPeriod)) { //direct conflict
                    DirectConflict dc = idirects.get(other);
                    if (dc==null) {
                        dc = new DirectConflict(getAssignment(other, table));
                        idirects.put(other, dc);
                    } else dc.incNrStudents();
                    iNrInstructorDirectConflicts++;
                    dc.getStudents().add(instructor.getUniqueId());
                } else if (p.isBackToBack(getPeriod(),otherPeriod)) {
                    BackToBackConflict btb = ibackToBacks.get(other);
                    double distance = Location.getDistance(getRooms(), getAssignedRooms(other, table));
                    if (btb==null) {
                        btb = new BackToBackConflict(getAssignment(other, table), (p.getBackToBackDistance()<0?false:distance>p.getBackToBackDistance()), distance);
                        ibackToBacks.put(other, btb);
                    } else btb.incNrStudents();
                    iNrInstructorBackToBackConflicts++;
                    if (btb.isDistance()) iNrInstructorDistanceBackToBackConflicts++;
                    btb.getStudents().add(instructor.getUniqueId());
                }
                if (getPeriod().getDateOffset().equals(otherPeriod.getDateOffset()))
                    sameDateExams.add(other);
            }
            if ("true".equals(ApplicationProperties.getProperty("tmtbl.exam.eventConflicts."+(iExamType==org.unitime.timetable.model.Exam.sExamTypeFinal?"final":"midterm"), "true")))
                computeUnavailablility(instructor, getPeriod(), period2meetings);
            if (sameDateExams.size()>=2) {
                TreeSet examIds = new TreeSet();
                TreeSet otherExams = new TreeSet();
                for (Iterator j=sameDateExams.iterator();j.hasNext();) {
                    org.unitime.timetable.model.Exam other = (org.unitime.timetable.model.Exam)j.next();
                    examIds.add(other.getUniqueId());
                    otherExams.add(getAssignment(other, table));
                }
                MoreThanTwoADayConflict m2d = im2ds.get(examIds.toString());
                if (m2d==null) {
                    m2d = new MoreThanTwoADayConflict(otherExams);
                    im2ds.put(examIds.toString(), m2d);
                } else m2d.incNrStudents();
                iNrInstructorMoreThanTwoADayConflicts++;
                m2d.getStudents().add(instructor.getUniqueId());
            }
        }
        iInstructorDirects.addAll(idirects.values());
        iInstructorBackToBacks.addAll(ibackToBacks.values());
        iInstructorMoreThanTwoADays.addAll(im2ds.values());   

        for (Iterator i=getExam().getDistributionObjects().iterator();i.hasNext();) {
            DistributionObject dObj = (DistributionObject)i.next();
            DistributionPref pref = dObj.getDistributionPref();
            if (!check(pref, getExam(), getPeriod(), getRooms(), table))
                iDistributions.add(new DistributionConflict(pref, getExam()));
        }
    }
    
    public TreeSet<DirectConflict> getDirectConflicts() {
        return iDirects;
    }

    public TreeSet<BackToBackConflict> getBackToBackConflicts() {
        return iBackToBacks;
    }
    
    public TreeSet<MoreThanTwoADayConflict> getMoreThanTwoADaysConflicts() {
        return iMoreThanTwoADays;
    }
    
    public int getNrDirectConflicts() {
        int ret = 0;
        for (Iterator i=iDirects.iterator();i.hasNext();) {
            DirectConflict dc = (DirectConflict)i.next();
            ret += dc.getNrStudents();
        }
        return ret;
    }

    public int getNrBackToBackConflicts() {
        int ret = 0;
        for (Iterator i=iBackToBacks.iterator();i.hasNext();) {
            BackToBackConflict btb = (BackToBackConflict)i.next();
            ret += btb.getNrStudents();
        }
        return ret;
    }
    
    public int getNrDistanceBackToBackConflicts() {
        int ret = 0;
        for (Iterator i=iBackToBacks.iterator();i.hasNext();) {
            BackToBackConflict btb = (BackToBackConflict)i.next();
            if (btb.isDistance())
                ret += btb.getNrStudents();
        }
        return ret;
    }

    public int getNrMoreThanTwoConflicts() {
        int ret = 0;
        for (Iterator i=iMoreThanTwoADays.iterator();i.hasNext();) {
            MoreThanTwoADayConflict m2d = (MoreThanTwoADayConflict)i.next();
            ret += m2d.getNrStudents();
        }
        return ret;
    }
    
    public int getNrDirectConflicts(ExamSectionInfo section) {
        int ret = 0;
        for (Iterator i=iDirects.iterator();i.hasNext();) {
            DirectConflict dc = (DirectConflict)i.next();
            for (Enumeration f=dc.getStudents().elements();f.hasMoreElements();)
                if (section.getStudentIds().contains(f.nextElement())) ret++;
        }
        return ret;
    }

    public int getNrBackToBackConflicts(ExamSectionInfo section) {
        int ret = 0;
        for (Iterator i=iBackToBacks.iterator();i.hasNext();) {
            BackToBackConflict btb = (BackToBackConflict)i.next();
            for (Enumeration f=btb.getStudents().elements();f.hasMoreElements();)
                if (section.getStudentIds().contains(f.nextElement())) ret++;
        }
        return ret;
    }
    
    public int getNrDistanceBackToBackConflicts(ExamSectionInfo section) {
        int ret = 0;
        for (Iterator i=iBackToBacks.iterator();i.hasNext();) {
            BackToBackConflict btb = (BackToBackConflict)i.next();
            if (btb.isDistance())
                for (Enumeration f=btb.getStudents().elements();f.hasMoreElements();)
                    if (section.getStudentIds().contains(f.nextElement())) ret++;
        }
        return ret;
    }

    public int getNrMoreThanTwoConflicts(ExamSectionInfo section) {
        int ret = 0;
        for (Iterator i=iMoreThanTwoADays.iterator();i.hasNext();) {
            MoreThanTwoADayConflict m2d = (MoreThanTwoADayConflict)i.next();
            for (Enumeration f=m2d.getStudents().elements();f.hasMoreElements();)
                if (section.getStudentIds().contains(f.nextElement())) ret++;
        }
        return ret;
    }
    
    public TreeSet<DistributionConflict> getDistributionConflicts() {
        return iDistributions;
    }
    
    public String getDistributionConflictsHtml(String delim) {
        String ret = "";
        for (Iterator i=iDistributions.iterator();i.hasNext();) {
            DistributionConflict dc = (DistributionConflict)i.next();
            if (ret.length()>0) ret+=delim;
            ret+=dc.getTypeHtml();
        }
        return ret;
    }
    
    public String getDistributionConflictsList(String delim) {
        String ret = "";
        for (Iterator i=iDistributions.iterator();i.hasNext();) {
            DistributionConflict dc = (DistributionConflict)i.next();
            if (ret.length()>0) ret+=delim;
            ret+=PreferenceLevel.prolog2abbv(dc.getPreference())+" "+dc.getType();
        }
        return ret;
    }

    public int getNrDistributionConflicts() {
        return iDistributions.size();
    }
    
    public boolean getHasConflicts() {
        return !getDirectConflicts().isEmpty() || !getBackToBackConflicts().isEmpty() || !getMoreThanTwoADaysConflicts().isEmpty();
    }
    
    public String getConflictTable() {
        return getConflictTable(true);
    }
    
    public String getConflictTable(boolean header) {
        String ret = "<table border='0' width='95%' cellspacing='0' cellpadding='3'>";
        if (header) {
            ret += "<tr>";
            ret += "<td><i>Students</i></td>";
            ret += "<td><i>Conflict</i></td>";
            ret += "<td><i>Exam</i></td>";
            ret += "<td><i>Period</i></td>";
            ret += "<td><i>Room</i></td>";
            ret += "</tr>";
        }
        for (Iterator i=getDirectConflicts().iterator();i.hasNext();)
            ret += i.next().toString();
        for (Iterator i=getMoreThanTwoADaysConflicts().iterator();i.hasNext();)
            ret += i.next().toString();
        for (Iterator i=getBackToBackConflicts().iterator();i.hasNext();)
            ret += i.next().toString();
        ret += "</table>";
        return ret;
    }
    
    public String getConflictInfoTable() {
        String ret = "<table border='0' width='95%' cellspacing='0' cellpadding='3'>";
        ret += "<tr>";
        ret += "<td><i>Students</i></td>";
        ret += "<td><i>Conflict</i></td>";
        ret += "<td><i>Exam</i></td>";
        ret += "<td><i>Period</i></td>";
        ret += "<td><i>Room</i></td>";
        ret += "</tr>";
        for (DirectConflict dc : getDirectConflicts())
            ret += dc.toString(true);
        for (MoreThanTwoADayConflict m2d : getMoreThanTwoADaysConflicts())
            ret += m2d.toString(true);
        for (BackToBackConflict btb : getBackToBackConflicts()) 
            ret += btb.toString(true);
        ret += "</table>";
        return ret;
    }
    
    public String getDistributionConflictTable() {
        return getDistributionConflictTable(true);
    }
    
    public String getDistributionConflictTable(boolean header) {
        String ret = "<table border='0' width='95%' cellspacing='0' cellpadding='3'>";
        if (header) {
            ret += "<tr>";
            ret += "<td><i>Preference</i></td>";
            ret += "<td><i>Distribution</i></td>";
            ret += "<td><i>Exam</i></td>";
            ret += "<td><i>Period</i></td>";
            ret += "<td><i>Room</i></td>";
            ret += "</tr>";
        }
        for (Iterator i=getDistributionConflicts().iterator();i.hasNext();)
            ret += i.next().toString();
        ret += "</table>";
        return ret;
    }
    
    public String getDistributionInfoConflictTable() {
        String ret = "<table border='0' width='95%' cellspacing='0' cellpadding='3'>";
        ret += "<tr>";
        ret += "<td><i>Preference</i></td>";
        ret += "<td><i>Distribution</i></td>";
        ret += "<td><i>Exam</i></td>";
        ret += "<td><i>Period</i></td>";
        ret += "<td><i>Room</i></td>";
        ret += "</tr>";
        for (DistributionConflict dc : getDistributionConflicts())
            ret += dc.toString(true);
        ret += "</table>";
        return ret;
    }
    
    public TreeSet<DirectConflict> getInstructorDirectConflicts() {
        return iInstructorDirects;
    }

    public TreeSet<BackToBackConflict> getInstructorBackToBackConflicts() {
        return iInstructorBackToBacks;
    }
    
    public TreeSet<MoreThanTwoADayConflict> getInstructorMoreThanTwoADaysConflicts() {
        return iInstructorMoreThanTwoADays;
    }
    
    public int getNrInstructorDirectConflicts() {
        int ret = 0;
        for (Iterator i=iInstructorDirects.iterator();i.hasNext();) {
            DirectConflict dc = (DirectConflict)i.next();
            ret += dc.getNrStudents();
        }
        return ret;
    }

    public int getNrInstructorBackToBackConflicts() {
        int ret = 0;
        for (Iterator i=iInstructorBackToBacks.iterator();i.hasNext();) {
            BackToBackConflict btb = (BackToBackConflict)i.next();
            ret += btb.getNrStudents();
        }
        return ret;
    }
    
    public int getNrInstructorDistanceBackToBackConflicts() {
        int ret = 0;
        for (Iterator i=iInstructorBackToBacks.iterator();i.hasNext();) {
            BackToBackConflict btb = (BackToBackConflict)i.next();
            if (btb.isDistance())
                ret += btb.getNrStudents();
        }
        return ret;
    }

    public int getNrInstructorMoreThanTwoConflicts() {
        int ret = 0;
        for (Iterator i=iInstructorMoreThanTwoADays.iterator();i.hasNext();) {
            MoreThanTwoADayConflict m2d = (MoreThanTwoADayConflict)i.next();
            ret += m2d.getNrStudents();
        }
        return ret;
    }
    
    public int getNrInstructorDirectConflicts(ExamSectionInfo section) {
        int ret = 0;
        for (Iterator i=iInstructorDirects.iterator();i.hasNext();) {
            DirectConflict dc = (DirectConflict)i.next();
            for (Enumeration f=dc.getStudents().elements();f.hasMoreElements();)
                if (dc.getOtherEventId()!=null) {
                    if (section.getStudentIds().contains(f.nextElement())) ret++;
                } else ret++;
        }
        return ret;
    }

    public int getNrInstructorBackToBackConflicts(ExamSectionInfo section) {
        return getNrInstructorBackToBackConflicts();
    }
    
    public int getNrInstructorDistanceBackToBackConflicts(ExamSectionInfo section) {
        return getNrInstructorDistanceBackToBackConflicts();
    }

    public int getNrInstructorMoreThanTwoConflicts(ExamSectionInfo section) {
        return getNrInstructorMoreThanTwoConflicts();
    }

    public boolean getHasInstructorConflicts() {
        return !getInstructorDirectConflicts().isEmpty() || !getInstructorBackToBackConflicts().isEmpty() || !getInstructorMoreThanTwoADaysConflicts().isEmpty();
    }
    
    public String getInstructorConflictTable() {
        return getInstructorConflictTable(true);
    }
    
    public String getInstructorConflictTable(boolean header) {
        String ret = "<table border='0' width='95%' cellspacing='0' cellpadding='3'>";
        if (header) {
            ret += "<tr>";
            ret += "<td><i>Instructors</i></td>";
            ret += "<td><i>Conflict</i></td>";
            ret += "<td><i>Exam</i></td>";
            ret += "<td><i>Period</i></td>";
            ret += "<td><i>Room</i></td>";
            ret += "</tr>";
        }
        for (Iterator i=getInstructorDirectConflicts().iterator();i.hasNext();)
            ret += i.next().toString();
        for (Iterator i=getInstructorMoreThanTwoADaysConflicts().iterator();i.hasNext();)
            ret += i.next().toString();
        for (Iterator i=getInstructorBackToBackConflicts().iterator();i.hasNext();)
            ret += i.next().toString();
        ret += "</table>";
        return ret;
    }
    
    public String getInstructorConflictInfoTable() {
        String ret = "<table border='0' width='95%' cellspacing='0' cellpadding='3'>";
        ret += "<tr>";
        ret += "<td><i>Students</i></td>";
        ret += "<td><i>Conflict</i></td>";
        ret += "<td><i>Exam</i></td>";
        ret += "<td><i>Period</i></td>";
        ret += "<td><i>Room</i></td>";
        ret += "</tr>";
        for (DirectConflict dc : getInstructorDirectConflicts())
            ret += dc.toString(true);
        for (MoreThanTwoADayConflict m2d : getInstructorMoreThanTwoADaysConflicts())
            ret += m2d.toString(true);
        for (BackToBackConflict btb : getInstructorBackToBackConflicts()) 
            ret += btb.toString(true);
        ret += "</table>";
        return ret;
    }

    
    
    public static class DirectConflict implements Serializable, Comparable<DirectConflict> {
        protected ExamAssignment iOtherExam = null;
        protected int iNrStudents = 1;
        protected Vector<Long> iStudents = new Vector();
        protected String iOtherEventName = null;
        protected String iOtherEventTime = null;
        protected String iOtherEventDate = null;
        protected String iOtherEventRoom = null;
        protected int iOtherEventSize = 0;
        protected Long iOtherEventId;
        protected transient Event iOtherEvent = null;
        
        protected DirectConflict(ExamAssignment otherExam) {
            iOtherExam = otherExam;
        }
        protected DirectConflict(ExamAssignment otherExam, ExamConflict conflict, boolean students) {
            iOtherExam = otherExam;
            if (students) {
                iNrStudents = conflict.getStudents().size();
                for (Iterator i=conflict.getStudents().iterator();i.hasNext();) {
                    Student student = (Student)i.next();
                    iStudents.add(student.getUniqueId());
                }
            } else {
                iNrStudents = conflict.getInstructors().size();
                for (Iterator i=conflict.getInstructors().iterator();i.hasNext();) {
                    DepartmentalInstructor instructor = (DepartmentalInstructor)i.next();
                    iStudents.add(instructor.getUniqueId());
                }
            }
        }
        protected DirectConflict(Meeting otherMeeting) {
            iOtherEvent = otherMeeting.getEvent();
            iOtherEventSize = otherMeeting.getEvent().getStudentIds().size();
            iOtherEventId = otherMeeting.getEvent().getUniqueId();
            iOtherEventName = otherMeeting.getEvent().getEventName();
            iOtherEventDate = otherMeeting.dateStr();
            iOtherEventTime = otherMeeting.startTime()+" - "+otherMeeting.stopTime();
            iOtherEventRoom = otherMeeting.getRoomLabel();
        }
        protected void addMeeting(Meeting otherMeeting) {
            if (otherMeeting.getLocation()!=null)
                iOtherEventRoom += (iOtherEventRoom!=null && iOtherEventRoom.length()>0?", ":"")+otherMeeting.getRoomLabel();
        }
        protected DirectConflict(Meeting otherMeeting,Collection<Long> studentIds) {
            this(otherMeeting);
            iNrStudents = studentIds.size();
            iStudents.addAll(studentIds);
        }
        protected DirectConflict(ExamResourceUnavailability unavailability, Vector<Long> studentIds) {
            iOtherEventId = unavailability.getId();
            iOtherEventSize = unavailability.getSize();
            iOtherEventName = unavailability.getName();
            iOtherEventTime = unavailability.getTime();
            iOtherEventDate = unavailability.getDate();
            iOtherEventRoom = unavailability.getRoom();
            iNrStudents = studentIds.size();
            iStudents = studentIds;
        }
        protected void incNrStudents() {
            iNrStudents++;
        }
        public int getNrStudents() {
            return iNrStudents;
        }
        public Vector<Long> getStudents() {
            return iStudents;
        }
        public ExamAssignment getOtherExam() {
            return iOtherExam;
        }
        public Long getOtherEventId() {
            return iOtherEventId;
        }
        public Event getOtherEvent() {
            if (iOtherEvent!=null) return iOtherEvent;
            if (iOtherEventId==null) return null;
            iOtherEvent = new EventDAO().get(iOtherEventId);
            return iOtherEvent;
        }
        public String getOtherEventName() {
            return iOtherEventName;
        }
        public String getOtherEventRoom() {
            return iOtherEventRoom;
        }
        public String getOtherEventDate() {
            return iOtherEventDate;
        }
        public String getOtherEventTime() {
            return iOtherEventTime;
        }
        public int getOtherEventSize() {
            return iOtherEventSize;
        }
        public boolean isOtherClass() {
            return getOtherEvent()!=null && (getOtherEvent() instanceof ClassEvent);
        }
        public Class_ getOtherClass() {
            return (getOtherEvent()==null || ((getOtherEvent() instanceof ClassEvent))?null:((ClassEvent)getOtherEvent()).getClazz());
        }
        public int compareTo(DirectConflict c) {
            int cmp = -Double.compare(getNrStudents(), c.getNrStudents());
            if (cmp!=0) return cmp;
            if (getOtherExam()==null) return (c.getOtherExam()==null?0:-1);
            if (c.getOtherExam()==null) return 1;
            return getOtherExam().compareTo(c.getOtherExam());
        }
        public String toString() {
            return toString(false);
        }
        public String toString(boolean links) {
            String ret = "";
            if (links && getOtherExam()!=null)
                ret += "<tr onmouseover=\"this.style.backgroundColor='rgb(223,231,242)';this.style.cursor='hand';this.style.cursor='pointer';\" onmouseout=\"this.style.backgroundColor='transparent';\" onclick=\"document.location='examInfo.do?examId="+getOtherExam().getExamId()+"&op=Select';\">";
            else
                ret += "<tr onmouseover=\"this.style.backgroundColor='rgb(223,231,242)';\" onmouseout=\"this.style.backgroundColor='transparent';\">";
            ret += "<td style='font-weight:bold;color:"+PreferenceLevel.prolog2color("P")+";'>";
            ret += String.valueOf(getNrStudents());
            ret += "</td>";
            ret += "<td style='font-weight:bold;color:"+PreferenceLevel.prolog2color("P")+";'>";
            ret += "Direct";
            ret += "</td>";
            if (getOtherExam()==null) {
                if (iOtherEventName!=null) {
                    ret += "<td>"+iOtherEventName+"</td>";
                    ret += "<td>"+iOtherEventDate+" "+iOtherEventTime+"</td>";
                    ret += "<td>"+iOtherEventRoom+"</td>";
                } else {
                    ret += "<td colspan='3'>Student/instructor not available for unknown reason.</td>";
                }
            } else {
                ret += "<td>"+getOtherExam().getExamNameHtml()+"</td>";
                ret += "<td>"+getOtherExam().getPeriodAbbreviationWithPref()+"</td>";
                ret += "<td>"+getOtherExam().getRoomsNameWithPref(", ")+"</td>";
            }
            ret += "</tr>";
            return ret;
        }
    }
    
    public static class BackToBackConflict implements Serializable, Comparable<BackToBackConflict> {
        protected ExamAssignment iOtherExam;
        protected int iNrStudents = 1;
        protected boolean iIsDistance = false; 
        protected Vector<Long> iStudents = new Vector();
        protected double iDistance = 0;
        
        protected BackToBackConflict(ExamAssignment otherExam, boolean isDistance, double distance) {
            iOtherExam = otherExam;
            iIsDistance = isDistance;
            iDistance = distance;
        }
        protected BackToBackConflict(ExamAssignment otherExam, ExamConflict conflict, boolean students) {
            iOtherExam = otherExam;
            if (students) {
                iNrStudents = conflict.getStudents().size();
                for (Iterator i=conflict.getStudents().iterator();i.hasNext();) {
                    Student student = (Student)i.next();
                    iStudents.add(student.getUniqueId());
                }
            } else {
                iNrStudents = conflict.getInstructors().size();
                for (Iterator i=conflict.getInstructors().iterator();i.hasNext();) {
                    DepartmentalInstructor instructor = (DepartmentalInstructor)i.next();
                    iStudents.add(instructor.getUniqueId());
                }
            }
            iIsDistance = conflict.isDistanceBackToBackConflict();
            iDistance = conflict.getDistance();
        }
        protected void incNrStudents() {
            iNrStudents++;
        }
        public int getNrStudents() {
            return iNrStudents;
        }
        public boolean isDistance() {
            return iIsDistance;
        }
        public ExamAssignment getOtherExam() {
            return iOtherExam;
        }
        public Vector<Long> getStudents() {
            return iStudents;
        }
        public double getDistance() {
            return iDistance;
        }
        public int compareTo(BackToBackConflict c) {
            int cmp = -Double.compare(getNrStudents(), c.getNrStudents());
            if (cmp!=0) return cmp;
            if (isDistance()!=c.isDistance()) return (isDistance()?-1:1);
            return getOtherExam().compareTo(c.getOtherExam());
        }
        public String toString() {
            return toString(false);
        }
        public String toString(boolean links) {
            String ret = "";
            if (links && getOtherExam()!=null)
                ret += "<tr onmouseover=\"this.style.backgroundColor='rgb(223,231,242)';this.style.cursor='hand';this.style.cursor='pointer';\" onmouseout=\"this.style.backgroundColor='transparent';\" onclick=\"document.location='examInfo.do?examId="+getOtherExam().getExamId()+"&op=Select';\">";
            else
                ret += "<tr onmouseover=\"this.style.backgroundColor='rgb(223,231,242)';\" onmouseout=\"this.style.backgroundColor='transparent';\">";
            ret += "<td style='font-weight:bold;color:"+PreferenceLevel.prolog2color("1")+";'>";
            ret += String.valueOf(getNrStudents());
            ret += "</td>";
            ret += "<td style='font-weight:bold;color:"+PreferenceLevel.prolog2color("1")+";'>";
            ret += "Back-To-Back";
            if (isDistance()) ret+="<br>("+Math.round(10.0*getDistance())+" m)";
            ret += "</td>";
            ret += "<td>"+getOtherExam().getExamNameHtml()+"</td>";
            ret += "<td>"+getOtherExam().getPeriodAbbreviationWithPref()+"</td>";
            ret += "<td>"+getOtherExam().getRoomsNameWithPref(", ")+"</td>";
            ret += "</tr>";
            return ret;
        }
    }

    public static class MoreThanTwoADayConflict implements Serializable, Comparable<MoreThanTwoADayConflict> {
        protected TreeSet<ExamAssignment> iOtherExams;
        protected int iNrStudents = 1;
        protected Vector<Long> iStudents = new Vector();
        
        protected MoreThanTwoADayConflict(TreeSet<ExamAssignment> otherExams) {
            iOtherExams = otherExams;
        }
        protected MoreThanTwoADayConflict(TreeSet<ExamAssignment> otherExams, ExamConflict conflict, boolean students) {
            iOtherExams = otherExams;
            if (students) {
                iNrStudents = conflict.getStudents().size();
                for (Iterator i=conflict.getStudents().iterator();i.hasNext();) {
                    Student student = (Student)i.next();
                    iStudents.add(student.getUniqueId());
                }
            } else {
                iNrStudents = conflict.getInstructors().size();
                for (Iterator i=conflict.getInstructors().iterator();i.hasNext();) {
                    DepartmentalInstructor instructor = (DepartmentalInstructor)i.next();
                    iStudents.add(instructor.getUniqueId());
                }
            }
        }
        protected void incNrStudents() {
            iNrStudents++;
        }
        public int getNrStudents() {
            return iNrStudents;
        }
        public Vector<Long> getStudents() {
            return iStudents;
        }
        public TreeSet<ExamAssignment> getOtherExams() {
            return iOtherExams;
        }
        public int compareTo(MoreThanTwoADayConflict c) {
            int cmp = -Double.compare(getNrStudents(), c.getNrStudents());
            if (cmp!=0) return cmp;
            cmp = -Double.compare(getOtherExams().size(), c.getOtherExams().size());
            if (cmp!=0) return cmp;
            Iterator i1 = getOtherExams().iterator(), i2 = c.getOtherExams().iterator();
            while (i1.hasNext()) {
                ExamAssignment a1 = (ExamAssignment)i1.next();
                ExamAssignment a2 = (ExamAssignment)i2.next();
                if (!a1.equals(a2)) return a1.compareTo(a2);
            }
            return 0;
        }
        public String toString() {
            return toString(false);
        }
        public String toString(boolean links) {
            String ret = "";
            String mouseOver = "";
            String mouseOut = "";
            String id = "";
            for (Iterator i=getOtherExams().iterator();i.hasNext();) {
                ExamAssignment a = (ExamAssignment)i.next();
                id+=a.getExamId(); 
                if (i.hasNext()) id+=":";
            }
            int idx = 0;
            Vector<Long> ids = new Vector();
            for (Iterator i=getOtherExams().iterator();i.hasNext();idx++) {
                ExamAssignment a = (ExamAssignment)i.next();
                ids.add(a.getExamId());
                mouseOver += "document.getElementById('"+id+":"+idx+"').style.backgroundColor='rgb(223,231,242)';";
                if (links)
                    mouseOver += "this.style.cursor='hand';this.style.cursor='pointer';";
                mouseOut += "document.getElementById('"+id+":"+idx+"').style.backgroundColor='transparent';";
            }
            idx = 0;
            if (links)
                ret += "<tr id='"+id+":"+idx+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\" onclick=\"document.location='examInfo.do?examId="+ids.elementAt(idx)+"&op=Select';\">";
            else
                ret += "<tr id='"+id+":"+idx+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\">";
            ret += "<td valign='top' rowspan='"+getOtherExams().size()+"' style='font-weight:bold;color:"+PreferenceLevel.prolog2color("2")+";'>";
            ret += String.valueOf(getNrStudents());
            ret += "</td>";
            ret += "<td valign='top' rowspan='"+getOtherExams().size()+"' style='font-weight:bold;color:"+PreferenceLevel.prolog2color("2")+";'>";
            ret += "&gt;2 A Day";
            ret += "</td>";
            for (Iterator i=getOtherExams().iterator();i.hasNext();idx++) {
                ExamAssignment a = (ExamAssignment)i.next();
                ret += "<td>"+a.getExamNameHtml()+"</td>";
                ret += "<td>"+a.getPeriodAbbreviationWithPref()+"</td>";
                ret += "<td>"+a.getRoomsNameWithPref(", ")+"</td>";
                ret += "</tr>";
                if (i.hasNext()) {
                    if (links)
                        ret += "<tr id='"+id+":"+(1+idx)+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\" onclick=\"document.location='examInfo.do?examId="+ids.elementAt(1+idx)+"&op=Select';\">";
                    else
                        ret += "<tr id='"+id+":"+(1+idx)+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\">";
                }
            }
            return ret;
        }
    }
    
    public static class DistributionConflict implements Serializable, Comparable<DistributionConflict> {
        protected TreeSet<ExamInfo> iOtherExams;
        protected String iPreference;
        protected Long iId;
        protected String iType;
        protected transient DistributionPref iPref = null;
        protected DistributionConflict(Long id, String type, TreeSet<ExamInfo> otherExams, String preference) {
            iId = id;
            iType = type;
            iOtherExams = otherExams;
            iPreference = preference;
        }
        protected DistributionConflict(ExamDistributionConstraint dc, Exam exclude) {
            iId = dc.getId();
            iType = dc.getTypeString();
            iOtherExams = new TreeSet();
            for (Enumeration e=dc.variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                if (exam.equals(exclude)) continue;
                iOtherExams.add(exam.getAssignment()==null?new ExamInfo(exam):new ExamAssignment(exam,(ExamPlacement)exam.getAssignment()));
            }
            iPreference = (dc.isHard()?"R":dc.getWeight()>=2?"-2":"-1");
        }
        protected DistributionConflict(DistributionPref pref, org.unitime.timetable.model.Exam exclude) {
            iPref = pref;
            iId = pref.getUniqueId();
            iType = pref.getDistributionType().getLabel();
            iOtherExams = new TreeSet();
            for (Iterator i=pref.getDistributionObjects().iterator();i.hasNext();) {
                DistributionObject dObj = (DistributionObject)i.next();
                org.unitime.timetable.model.Exam exam = (org.unitime.timetable.model.Exam)dObj.getPrefGroup();
                if (exam.equals(exclude)) continue;
                iOtherExams.add(exam.getAssignedPeriod()==null?new ExamInfo(exam):new ExamAssignment(exam));
            }
            iPreference = pref.getPrefLevel().getPrefProlog(); 
        }
        public Long getId() {
            return iId;
        }
        public String getType() {
            return iType;
        }
        public String getTypeHtml() {
            String title = PreferenceLevel.prolog2string(getPreference())+" "+getType()+" with ";
            for (Iterator i=getOtherExams().iterator();i.hasNext();) {
                ExamInfo a = (ExamInfo)i.next();
                title += a.getExamName();
                if (i.hasNext()) title += " and ";
            }
            return "<span style='font-weight:bold;color:"+PreferenceLevel.prolog2color(getPreference())+";' title='"+title+"'>"+iType+"</span>";
        }
        public String getPreference() {
            return iPreference;
        }
        public TreeSet<ExamInfo> getOtherExams() {
            return iOtherExams;
        }
        public int hashCode() {
            return getId().hashCode();
        }
        public boolean equals(Object o) {
            if (o==null || !(o instanceof DistributionConflict)) return false;
            DistributionConflict c = (DistributionConflict)o;
            return getId().equals(c.getId());
        }
        public int compareTo(DistributionConflict c) {
            Iterator i1 = getOtherExams().iterator(), i2 = c.getOtherExams().iterator();
            while (i1.hasNext()) {
                ExamInfo a1 = (ExamInfo)i1.next();
                ExamInfo a2 = (ExamInfo)i2.next();
                if (!a1.equals(a2)) return a1.compareTo(a2);
            }
            return getId().compareTo(c.getId());
        }
        public String toString() {
            return toString(false);
        }
        public String toString(boolean links) {
            String ret = "";
            String mouseOver = "";
            String mouseOut = "";
            String id = "";
            for (Iterator i=getOtherExams().iterator();i.hasNext();) {
                ExamInfo a = (ExamInfo)i.next();
                id+=a.getExamId(); 
                if (i.hasNext()) id+=":";
            }
            int idx = 0;
            Vector<Long> ids = new Vector();
            for (Iterator i=getOtherExams().iterator();i.hasNext();idx++) {
                ExamInfo a = (ExamInfo)i.next();
                ids.add(a.getExamId());
                mouseOver += "document.getElementById('"+id+":"+idx+"').style.backgroundColor='rgb(223,231,242)';";
                if (links)
                    mouseOver += "this.style.cursor='hand';this.style.cursor='pointer';";
                mouseOut += "document.getElementById('"+id+":"+idx+"').style.backgroundColor='transparent';";
            }
            idx = 0;
            if (links)
                ret += "<tr id='"+id+":"+idx+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\" onclick=\"document.location='examInfo.do?examId="+ids.elementAt(idx)+"&op=Select';\">";
            else
                ret += "<tr id='"+id+":"+idx+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\">";
            ret += "<td valign='top' rowspan='"+getOtherExams().size()+"' style='font-weight:bold;color:"+PreferenceLevel.prolog2color(getPreference())+";'>";
            ret += PreferenceLevel.prolog2string(getPreference());
            ret += "</td>";
            ret += "<td valign='top' rowspan='"+getOtherExams().size()+"' style='font-weight:bold;color:"+PreferenceLevel.prolog2color(getPreference())+";'>";
            ret += getType();
            ret += "</td>";
            for (Iterator i=getOtherExams().iterator();i.hasNext();idx++) {
                ExamInfo a = (ExamInfo)i.next();
                ret += "<td>"+a.getExamNameHtml()+"</td>";
                if (a instanceof ExamAssignment) {
                    ExamAssignment ea = (ExamAssignment)a;
                    ret += "<td>"+ea.getPeriodAbbreviationWithPref()+"</td>";
                    ret += "<td>"+ea.getRoomsNameWithPref(", ")+"</td>";
                } else {
                    ret += "<td></td>";
                    ret += "<td></td>";
                }
                ret += "</tr>";
                if (i.hasNext()) {
                    if (links)
                        ret += "<tr id='"+id+":"+(1+idx)+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\" onclick=\"document.location='examInfo.do?examId="+ids.elementAt(1+idx)+"&op=Select';\">";
                    else
                        ret += "<tr id='"+id+":"+(1+idx)+"' onmouseover=\""+mouseOver+"\" onmouseout=\""+mouseOut+"\">";
                }
            }
            return ret;
        }
        
    }
    
    public static class Parameters {
        private int iBtbDistance = -1;
        private boolean iBtbDayBreak = false;
        private Set iPeriods;
        
        public Parameters(Long sessionId, int examType) {
            iPeriods = ExamPeriod.findAll(sessionId, examType); 
            
            boolean btbDayBreak = false;
            SolverParameterDef btbDistDef = SolverParameterDef.findByName("Exams.BackToBackDistance");
            if (btbDistDef!=null && btbDistDef.getDefault()!=null)
                iBtbDistance = Integer.parseInt(btbDistDef.getDefault());
        
            SolverParameterDef btbDayBreakDef = SolverParameterDef.findByName("Exams.IsDayBreakBackToBack");
            if (btbDayBreakDef!=null && btbDayBreakDef.getDefault()!=null)
                iBtbDayBreak = "true".equals(btbDayBreakDef.getDefault());
        }
        
        public int getBackToBackDistance() { return iBtbDistance; }
        public boolean isDayBreakBackToBack() { return iBtbDayBreak; }

        public boolean isBackToBack(ExamPeriod p1, ExamPeriod p2) {
            if (!isDayBreakBackToBack() && !p1.getDateOffset().equals(p2.getDateOffset())) return false;
            for (Iterator i=iPeriods.iterator();i.hasNext();) {
                ExamPeriod p = (ExamPeriod)i.next();
                if (p1.compareTo(p)<0 && p.compareTo(p2)<0) return false;
                if (p1.compareTo(p)>0 && p.compareTo(p2)>0) return false;
            }
            return true;
        }
    }
}
