package org.unitime.timetable.reports.exam;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.ClassEvent;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.Event.MultiMeeting;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.reports.exam.InstructorExamReport.FileGenerator;
import org.unitime.timetable.solver.exam.ui.ExamAssignment;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo;
import org.unitime.timetable.solver.exam.ui.ExamRoomInfo;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.BackToBackConflict;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.DirectConflict;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.MoreThanTwoADayConflict;
import org.unitime.timetable.solver.exam.ui.ExamInfo.ExamSectionInfo;

import com.lowagie.text.DocumentException;

public class StudentExamReport extends PdfLegacyExamReport {
    protected static Logger sLog = Logger.getLogger(StudentExamReport.class);
    Hashtable<Long,Student> iStudents = null;
    Hashtable<Long,ClassEvent> iClass2event = null;
    Hashtable<Long,Location> iLocations = null;
    
    public StudentExamReport(int mode, File file, Session session, int examType, SubjectArea subjectArea, Collection<ExamAssignmentInfo> exams) throws IOException, DocumentException {
        super(mode, file, "STUDENT EXAMINATION SCHEDULE", session, examType, subjectArea, exams);
    }
    
    private void generateCache() {
        if (iStudents==null) {
            sLog.info("  Loading students...");
            iStudents = new Hashtable();
            for (Iterator i=new StudentDAO().getSession().createQuery("select s from Student s where s.session.uniqueId=:sessionId").setLong("sessionId", getSession().getUniqueId()).setCacheable(true).list().iterator();i.hasNext();) {
                Student s = (Student)i.next();
                iStudents.put(s.getUniqueId(), s);
            }
        }
        if (iClass2event==null) {
            sLog.info("  Loading class events...");
            iClass2event = new Hashtable();
            if (getSubjectArea()!=null) {
                for (Iterator i=new SessionDAO().getSession().createQuery(
                        "select c.uniqueId, e from ClassEvent e inner join e.clazz c left join fetch e.meetings m "+
                        "inner join c.schedulingSubpart.instrOfferingConfig.instructionalOffering.courseOfferings co where "+
                        "co.subjectArea.uniqueId=:subjectAreaId").
                        setLong("subjectAreaId", getSubjectArea().getUniqueId()).setCacheable(true).list().iterator();i.hasNext();) {
                    Object[] o = (Object[])i.next();
                    iClass2event.put((Long)o[0], (ClassEvent)o[1]);
                }
            } else {
                for (Iterator i=new SessionDAO().getSession().createQuery(
                        "select c.uniqueId, e from ClassEvent e inner join e.clazz c left join fetch e.meetings m "+
                        "inner join c.schedulingSubpart.instrOfferingConfig.instructionalOffering.courseOfferings co where "+
                        "co.subjectArea.session.uniqueId=:sessionId").
                        setLong("sessionId", getSession().getUniqueId()).setCacheable(true).list().iterator();i.hasNext();) {
                    Object[] o = (Object[])i.next();
                    iClass2event.put((Long)o[0], (ClassEvent)o[1]);
                }
            }
        }
        if (iLocations==null) {
            sLog.info("  Loading locations...");
            iLocations = new Hashtable();
            for (Iterator i=new SessionDAO().getSession().createQuery(
                    "select r from Room r where r.session.uniqueId=:sessionId and r.permanentId!=null").
                    setLong("sessionId", getSession().getUniqueId()).setCacheable(true).list().iterator();i.hasNext();) {
                Location location = (Location)i.next();
                iLocations.put(location.getUniqueId(), location);
            }
            for (Iterator i=new SessionDAO().getSession().createQuery(
                    "select r from NonUniversityLocation r where r.session.uniqueId=:sessionId and r.permanentId!=null").
                    setLong("sessionId", getSession().getUniqueId()).setCacheable(true).list().iterator();i.hasNext();) {
                Location location = (Location)i.next();
                iLocations.put(location.getUniqueId(), location);
            }
        }
    }
    
    public boolean isOfSubjectArea(TreeSet<ExamSectionInfo> sections) {
        if (getSubjectArea()==null) return true;
        for (ExamSectionInfo section : sections)
            if (getSubjectArea().equals(section.getOwner().getCourse().getSubjectArea())) return true;
        return false;
    }

    public void printReport() throws DocumentException {
        generateCache();
        sLog.info("  Printing report...");
        Hashtable<Student,TreeSet<ExamSectionInfo>> sections = new Hashtable();
        for (ExamAssignmentInfo exam:getExams()) {
            if (exam.getPeriod()==null) continue;
            for (ExamSectionInfo section:exam.getSectionsIncludeCrosslistedDummies()) {
                for (Long studentId : section.getStudentIds()) {
                    Student student = iStudents.get(studentId);
                    TreeSet<ExamSectionInfo> sectionsThisStudent = sections.get(student);
                    if (sectionsThisStudent==null) {
                        sectionsThisStudent = new TreeSet<ExamSectionInfo>();
                        sections.put(student, sectionsThisStudent);
                    }
                    sectionsThisStudent.add(section);
                }
            }
        }
        printHeader();
        int index = 0;
        for (Student student : new TreeSet<Student>(sections.keySet())) {
            TreeSet<ExamSectionInfo> sectionsThisStudent = sections.get(student);
            if (!isOfSubjectArea(sectionsThisStudent)) continue;
            if (iSince!=null) {
                ChangeLog last = getLastChange(sectionsThisStudent);
                if (last==null || iSince.compareTo(last.getTimeStamp())>0) {
                    sLog.debug("No change found for "+student.getName(DepartmentalInstructor.sNameFormatLastFist));
                    continue;
                }
            }
            if (index>0) newPage();
            printReport(student, sectionsThisStudent);
            index++;
            if ((index%100)==0) sLog.debug("  "+index+" students printed");
        }
        lastPage();
    }
    
    public void printReport(Long studentId) throws DocumentException {
        TreeSet<ExamSectionInfo> sections = new TreeSet();
        for (ExamAssignmentInfo exam : getExams()) {
            if (exam.getPeriod()==null) continue;
            for (ExamSectionInfo section : exam.getSectionsIncludeCrosslistedDummies())
                if (section.getStudentIds().contains(studentId)) sections.add(section);
        }
        if (sections.isEmpty()) return;
        Student student = new StudentDAO().get(studentId);
        printHeader();
        printReport(student, sections);
        lastPage();
    }
    
    public static class StudentClassComparator implements Comparator<Class_> {
    	private Student iStudent;
    	public StudentClassComparator(Student student) {
    		iStudent = student;
    	}
    	public CourseOffering getCourse(Class_ clazz) {
        	CourseOffering correctedCourse = clazz.getSchedulingSubpart().getControllingCourseOffering();
        	for (Iterator i=iStudent.getClassEnrollments().iterator();i.hasNext();) {
        		StudentClassEnrollment sce = (StudentClassEnrollment)i.next();
        		if (sce.getCourseOffering().getInstructionalOffering().equals(clazz.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering())) {
        			correctedCourse = sce.getCourseOffering();
        			break;
        		}
        	}
        	return correctedCourse;
    	}
    	public int compare(Class_ c1, Class_ c2) {
    		CourseOffering co1 = getCourse(c1);
    		CourseOffering co2 = getCourse(c2);
    		int cmp = co1.getSubjectAreaAbbv().compareTo(co2.getSubjectAreaAbbv());
    		if (cmp!=0) return cmp;
    		cmp = co1.getCourseNbr().compareTo(co2.getCourseNbr());
    		if (cmp!=0) return cmp;
    		if (isParentOf(c1, c2)) return -1;
    		if (isParentOf(c2, c1)) return 1;
    		if (cmp!=0) return cmp;
    		cmp = c1.getSchedulingSubpart().getItype().compareTo(c2.getSchedulingSubpart().getItype());
    		if (cmp!=0) return cmp;
    		return c1.getUniqueId().compareTo(c2.getUniqueId());
    	}
    	public boolean isParentOf(Class_ c1, Class_ c2) {
    		if (c2.getParentClass()!=null) {
    			if (c2.getParentClass().equals(c1)) return true;
    			return isParentOf(c1, c2.getParentClass());
    		}
    		return false;
    	}
    }

    public void printReport(Student student, TreeSet<ExamSectionInfo> sections) throws DocumentException {
        String name = student.getName(DepartmentalInstructor.sNameFormatLastFist);
        String shortName = student.getName(DepartmentalInstructor.sNameFormatLastInitial).toUpperCase();
        setPageName(shortName);
        setCont(shortName);
        println("Name:  "+name);
        if (student.getEmail()!=null)
            println("Email:       "+student.getEmail());
        Date lastChange = null;
        String changeObject = null;
        if (iClassSchedule) {
        	StudentClassComparator scc = new StudentClassComparator(student);
            TreeSet<Class_> allClasses = new TreeSet(scc);
            for (Iterator i=student.getClassEnrollments().iterator();i.hasNext();) {
                StudentClassEnrollment sce = (StudentClassEnrollment)i.next();
                allClasses.add(sce.getClazz());
            }
            if (!allClasses.isEmpty()) {
                println("");
                setHeader(new String[]{
                        "Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect   Dates                     Time            Room        Instructor",
                        "---- ------ "+(iItype?"------ ":"")+"----- ------------------------- --------------- ----------- -------------------------"});
                println(mpad("~ ~ ~ ~ ~ CLASS SECHEDULE ~ ~ ~ ~ ~",iNrChars));
                for (int i=0;i<getHeader().length;i++) println(getHeader()[i]);
                for (Class_ clazz : allClasses) {
                    String instructor = "";
                    if (clazz.isDisplayInstructor()) {
                        for (Iterator i=new TreeSet(clazz.getClassInstructors()).iterator();i.hasNext();) {
                            ClassInstructor ci = (ClassInstructor)i.next();
                            if (instructor.length()>0) instructor+=", ";
                            instructor += ci.getInstructor().getName(DepartmentalInstructor.sNameFormatLastInitial);
                        }
                    }
                	CourseOffering correctedCourse = scc.getCourse(clazz);
                    String subject = correctedCourse.getSubjectAreaAbbv(); 
                    String course = correctedCourse.getCourseNbr();
                    String itype =  getItype(clazz);
                    String section = (iUseClassSuffix && clazz.getClassSuffix()!=null?clazz.getClassSuffix():clazz.getSectionNumberString());
                    ClassEvent event = (iClass2event==null?clazz.getEvent():iClass2event.get(clazz.getUniqueId()));
                    if (event==null && iClass2event!=null && getSubjectArea()!=null && !getSubjectArea().equals(subject))
                    	event = clazz.getEvent();
                    if (event==null || event.getMeetings().isEmpty()) {
                        println(
                                rpad(subject,4)+" "+
                                rpad(course,6)+" "+
                                (iItype?rpad(itype,6)+" ":"")+
                                lpad(section,5)+" "+
                                rpad("ARRANGED HOURS",54)+
                                rpad(instructor,55)
                                );
                    } else {
                        MultiMeeting last = null;
                        String lastTime = null, lastDate = null;
                        String lastLoc = null;
                        for (MultiMeeting meeting : event.getMultiMeetings()) {
                            String line;
                            if (last==null) {
                                line = rpad(subject,4)+" "+
                                rpad(course,6)+" "+
                                (iItype?rpad(itype,6)+" ":"")+
                                lpad(section,5)+" ";
                            } else {
                                line = rpad("",18+(iItype?7:0));
                            }
                            String date = getMeetingDate(meeting);
                            String time = getMeetingTime(meeting.getMeetings().first());
                            if (last==null || !time.equals(lastTime) || !date.equals(lastDate)) {
                                line += rpad(date.equals(lastDate)?"":date,25)+" "+
                                        rpad(time.equals(lastTime)?"":time,15)+" ";
                            } else {
                                line += rpad("",39);
                            }
                            Long permId = meeting.getMeetings().first().getLocationPermanentId();
                            Location location = (permId==null?null:(iLocations==null?meeting.getMeetings().first().getLocation():iLocations.get(permId)));
                            if (location==null && iLocations!=null && getSubjectArea()!=null && !getSubjectArea().equals(subject))
                            	location = meeting.getMeetings().first().getLocation();
                            String loc = (location==null?rpad("",11):formatRoom(location.getLabel()));
                            if (last==null || !loc.equals(lastLoc)) {
                                line += loc + " ";
                            } else {
                                line += rpad("",12);
                            }
                            if (last==null)
                                line += instructor;
                            lastLoc = loc;
                            lastTime = time; lastDate = date;
                            last = meeting;
                            println(line);
                            if (iNewPage) { last=null; lastTime = null; lastDate = null; lastLoc = null; }
                        }
                    }
                }
            }
        }
        println("");
        setHeader(new String[]{
                "Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect    Meeting Times                          Date And Time                   Room      ",
                "---- ------ "+(iItype?"------ ":"")+"----- -------------------------------------- -------------------------------- -----------"});
        println(mpad("~ ~ ~ ~ ~ EXAMINATION SECHEDULE ~ ~ ~ ~ ~",iNrChars));
        for (int i=0;i<getHeader().length;i++) println(getHeader()[i]);
        iSubjectPrinted = false; String lastSubject = null;
        iCoursePrinted = false; String lastCourse = null;
        iITypePrinted = false; String lastItype = null;
        iPeriodPrinted = false; String lastSection = null;
        for (ExamSectionInfo section : sections) {
            if (iSubjectPrinted && !section.getSubject().equals(lastSubject)) { iSubjectPrinted = false; iCoursePrinted = false; iITypePrinted = false; iPeriodPrinted = false; }
            if (iCoursePrinted && !section.getCourseNbr().equals(lastCourse)) { iCoursePrinted = false; iITypePrinted = false; iPeriodPrinted = false; }
            if (iITypePrinted && !section.getItype().equals(lastItype)) { iITypePrinted = false; iPeriodPrinted = false; }
            if (iPeriodPrinted && !section.getSection().equals(lastSection)) { iPeriodPrinted = false; }
            if (section.getExamAssignment().getRooms()==null || section.getExamAssignment().getRooms().isEmpty()) {
                println(
                        rpad(iSubjectPrinted?"":section.getSubject(), 4)+" "+
                        rpad(iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                        (iItype?rpad(iITypePrinted?"":section.getItype(), 6)+" ":"")+
                        lpad(iPeriodPrinted?"":section.getSection(), 5)+" "+
                        rpad(getMeetingTime(section),38)+" "+
                        rpad((section.getExamAssignment()==null?"":section.getExamAssignment().getPeriodNameFixedLength()),32)+" "+
                        (section.getExamAssignment()==null?"":iNoRoom)
                        );
            } else {
                if (getLineNumber()+section.getExamAssignment().getRooms().size()>iNrLines) newPage();
                boolean firstRoom = true;
                for (ExamRoomInfo room : section.getExamAssignment().getRooms()) {
                    println(
                            rpad(!firstRoom || iSubjectPrinted?"":section.getSubject(), 4)+" "+
                            rpad(!firstRoom || iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                            (iItype?rpad(!firstRoom || iITypePrinted?"":section.getItype(), 6)+" ":"")+
                            lpad(!firstRoom || iPeriodPrinted?"":section.getSection(), 5)+" "+
                            rpad(!firstRoom?"":getMeetingTime(section),38)+" "+
                            rpad(!firstRoom?"":(section.getExamAssignment()==null?"":section.getExamAssignment().getPeriodNameFixedLength()),32)+" "+
                            formatRoom(room.getName()));
                    firstRoom = false;
                }
            }
            if (iNewPage) {
                iSubjectPrinted = iITypePrinted = iCoursePrinted = iPeriodPrinted = false;
                lastSubject = lastItype = lastCourse = lastSection = null;
            } else {
                iSubjectPrinted = iITypePrinted = iCoursePrinted = iPeriodPrinted = true;
                lastSubject = section.getSubject();
                lastItype = section.getItype();
                lastCourse = section.getCourseNbr();
                lastSection = section.getSection();
            }
        }
        
        boolean headerPrinted = false;
        lastSubject = null;
        for (ExamSectionInfo section : sections) {
            iSubjectPrinted = (!iNewPage && lastSubject!=null && lastSubject.equals(section.getSubject()));
            ExamAssignmentInfo exam = section.getExamAssignmentInfo();
            if (exam==null || exam.getPeriod()==null) continue;
            ExamPeriod period = exam.getPeriod();
            iCoursePrinted = false;
                if (iDirect) for (DirectConflict conflict : exam.getDirectConflicts()) {
                    if (!conflict.getStudents().contains(student.getUniqueId())) continue;
                    iPeriodPrinted = false;
                    if (conflict.getOtherExam()!=null) {
                        for (ExamSectionInfo other : conflict.getOtherExam().getSectionsIncludeCrosslistedDummies()) {
                            if (!other.getStudentIds().contains(student.getUniqueId())) continue;
                            if (!headerPrinted) {
                                if (!iNewPage) println("");
                                setHeader(null);
                                if (getLineNumber()+5>=iNrLines) newPage();
                                setHeader(new String[] {
                                        "Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Date And Time                Type   Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Time                 ",
                                        "---- ------ "+(iItype?"------ ":"")+"----- ---------------------------- ------ ---- ------ "+(iItype?"------ ":"")+"----- ---------------------"});
                                println(mpad("~ ~ ~ ~ ~ STUDENT CONFLICTS ~ ~ ~ ~ ~",iNrChars));
                                for (int i=0;i<getHeader().length;i++) println(getHeader()[i]);
                                setCont(shortName+"  STUDENT CONFLICTS");
                                headerPrinted = true;
                            }
                            println(
                                    rpad(iSubjectPrinted?"":section.getSubject(),4)+" "+
                                    rpad(iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                                    (iItype?rpad(iCoursePrinted?"":section.getItype(), 6)+" ":"")+
                                    lpad(iCoursePrinted?"":section.getSection(),5)+" "+
                                    rpad(iCoursePrinted?"":exam.getPeriodNameFixedLength(),28)+" "+
                                    rpad(iPeriodPrinted?"":"DIRECT",6)+" "+
                                    rpad(other.getSubject(),4)+" "+
                                    rpad(other.getCourseNbr(),6)+" "+
                                    (iItype?rpad(other.getItype(),6)+" ":"")+
                                    lpad(other.getSection(),5)+" "+
                                    other.getExamAssignment().getTimeFixedLength()
                                    );
                            iSubjectPrinted = iCoursePrinted = iStudentPrinted = iPeriodPrinted = !iNewPage;
                            lastSubject = section.getSubject();
                        }
                    } else if (conflict.getOtherEventId()!=null) {
                        if (!headerPrinted) {
                            if (!iNewPage) println("");
                            setHeader(null);
                            if (getLineNumber()+5>=iNrLines) newPage();
                            setHeader(new String[] {
                                    "Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Date And Time                Type   Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Time                 ",
                                    "---- ------ "+(iItype?"------ ":"")+"----- ---------------------------- ------ ---- ------ "+(iItype?"------ ":"")+"----- ---------------------"});
                            println(mpad("~ ~ ~ ~ ~ STUDENT CONFLICTS ~ ~ ~ ~ ~",iNrChars));
                            for (int i=0;i<getHeader().length;i++) println(getHeader()[i]);
                            setCont(shortName+"  STUDENT CONFLICTS");
                            headerPrinted = true;
                        }
                        if (conflict.isOtherClass()) {
                            println(
                                    rpad(iSubjectPrinted?"":section.getSubject(),4)+" "+
                                    rpad(iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                                    (iItype?rpad(iCoursePrinted?"":section.getItype(), 6)+" ":"")+
                                    lpad(iCoursePrinted?"":section.getSection(),5)+" "+
                                    rpad(iCoursePrinted?"":exam.getPeriodNameFixedLength(),28)+" "+
                                    rpad(iPeriodPrinted?"":"CLASS",6)+" "+
                                    rpad(conflict.getOtherClass().getSchedulingSubpart().getControllingCourseOffering().getSubjectAreaAbbv(),4)+" "+
                                    rpad(conflict.getOtherClass().getSchedulingSubpart().getControllingCourseOffering().getCourseNbr(),6)+" "+
                                    (iItype?rpad(iExternal?conflict.getOtherClass().getExternalUniqueId():conflict.getOtherClass().getSchedulingSubpart().getItypeDesc(),6)+" ":"")+
                                    lpad(iUseClassSuffix && conflict.getOtherClass().getClassSuffix()!=null?conflict.getOtherClass().getClassSuffix():conflict.getOtherClass().getSectionNumberString(),5)+" "+
                                    getMeetingTime(conflict.getOtherEventTime())
                                    );
                        } else {
                            println(
                                    rpad(iSubjectPrinted?"":section.getSubject(),4)+" "+
                                    rpad(iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                                    (iItype?rpad(iCoursePrinted?"":section.getItype(), 6)+" ":"")+
                                    lpad(iCoursePrinted?"":section.getSection(),5)+" "+
                                    rpad(iCoursePrinted?"":exam.getPeriodNameFixedLength(),28)+" "+
                                    rpad(iPeriodPrinted?"":"EVENT",6)+" "+
                                    rpad(conflict.getOtherEventName(),(iItype?24:17))+" "+
                                    getMeetingTime(conflict.getOtherEventTime())
                                    );
                        }
                        iSubjectPrinted = iCoursePrinted = iStudentPrinted = iPeriodPrinted = !iNewPage;
                        lastSubject = section.getSubject();
                    }
                }
                if (iM2d) for (MoreThanTwoADayConflict conflict : exam.getMoreThanTwoADaysConflicts()) {
                    if (!conflict.getStudents().contains(student.getUniqueId())) continue;
                    iPeriodPrinted = false;
                    for (ExamAssignment otherExam : conflict.getOtherExams()) {
                        for (ExamSectionInfo other : otherExam.getSectionsIncludeCrosslistedDummies()) {
                            if (!other.getStudentIds().contains(student.getUniqueId())) continue;
                            if (!headerPrinted) {
                                if (!iNewPage) println("");
                                setHeader(null);
                                if (getLineNumber()+5>=iNrLines) newPage();
                                setHeader(new String[] {
                                        "Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Date And Time                Type   Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Time                 ",
                                        "---- ------ "+(iItype?"------ ":"")+"----- ---------------------------- ------ ---- ------ "+(iItype?"------ ":"")+"----- ---------------------"});
                                println(mpad("~ ~ ~ ~ ~ STUDENT CONFLICTS ~ ~ ~ ~ ~",iNrChars));
                                for (int i=0;i<getHeader().length;i++) println(getHeader()[i]);
                                setCont(shortName+"  STUDENT CONFLICTS");
                                headerPrinted = true;
                            }
                            println(
                                    rpad(iSubjectPrinted?"":section.getSubject(),4)+" "+
                                    rpad(iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                                    (iItype?rpad(iCoursePrinted?"":section.getItype(), 6)+" ":"")+
                                    lpad(iCoursePrinted?"":section.getSection(),5)+" "+
                                    rpad(iCoursePrinted?"":exam.getPeriodNameFixedLength(),28)+" "+
                                    rpad(iPeriodPrinted?"":">2-DAY",6)+" "+
                                    rpad(other.getSubject(),4)+" "+
                                    rpad(other.getCourseNbr(),6)+" "+
                                    (iItype?rpad(other.getItype(),6)+" ":"")+
                                    lpad(other.getSection(),5)+" "+
                                    other.getExamAssignment().getTimeFixedLength()
                                    );
                            iSubjectPrinted = iCoursePrinted = iStudentPrinted = iPeriodPrinted = !iNewPage;
                            lastSubject = section.getSubject();
                        }
                    }
                }
                if (iBtb) for (BackToBackConflict conflict : exam.getBackToBackConflicts()) {
                    if (!conflict.getStudents().contains(student.getUniqueId())) continue;
                    iPeriodPrinted = false;
                    for (ExamSectionInfo other : conflict.getOtherExam().getSectionsIncludeCrosslistedDummies()) {
                        if (!other.getStudentIds().contains(student.getUniqueId())) continue;
                        if (!headerPrinted) {
                            if (!iNewPage) println("");
                            setHeader(null);
                            if (getLineNumber()+5>=iNrLines) newPage();
                            setHeader(new String[] {
                                    "Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Date And Time                Type   Subj Crsnbr "+(iItype?iExternal?"ExtnID ":"InsTyp ":"")+"Sect  Time                 ",
                                    "---- ------ "+(iItype?"------ ":"")+"----- ---------------------------- ------ ---- ------ "+(iItype?"------ ":"")+"----- ---------------------"});
                            println(mpad("~ ~ ~ ~ ~ STUDENT CONFLICTS ~ ~ ~ ~ ~",iNrChars));
                            for (int i=0;i<getHeader().length;i++) println(getHeader()[i]);
                            setCont(shortName+"  STUDENT CONFLICTS");
                            headerPrinted = true;
                        }
                        println(
                                rpad(iSubjectPrinted?"":section.getSubject(),4)+" "+
                                rpad(iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                                (iItype?rpad(iCoursePrinted?"":section.getItype(), 6)+" ":"")+
                                lpad(iCoursePrinted?"":section.getSection(),5)+" "+
                                rpad(iCoursePrinted?"":exam.getPeriodNameFixedLength(),28)+" "+
                                rpad(iPeriodPrinted?"":"BTB",6)+" "+
                                rpad(other.getSubject(),4)+" "+
                                rpad(other.getCourseNbr(),6)+" "+
                                (iItype?rpad(other.getItype(),6)+" ":"")+
                                lpad(other.getSection(),5)+" "+
                                other.getExamAssignment().getTimeFixedLength()
                                );
                        iSubjectPrinted = iCoursePrinted = iStudentPrinted = iPeriodPrinted = !iNewPage;
                        lastSubject = section.getSubject();
                    }
            }
        }
        
        setHeader(null);
        setCont(null);
    }
    
    public ChangeLog getLastChange(TreeSet<ExamSectionInfo> sections) {
        ChangeLog lastChange = null;
        for (ExamSectionInfo section : sections) {
            ChangeLog c = ChangeLog.findLastChange(section.getExam().getExam());
            if (c!=null && (lastChange==null || lastChange.getTimeStamp().compareTo(c.getTimeStamp())<0)) {
                lastChange = c;
            }
        }
        return lastChange;
    }
    
    public Hashtable<Student,File> printStudentReports(int mode, String filePrefix, FileGenerator gen) throws DocumentException, IOException {
        generateCache();
        sLog.info("Printing individual student reports...");
        Hashtable<Student,File> files = new Hashtable();
        Hashtable<Student,TreeSet<ExamSectionInfo>> sections = new Hashtable();
        for (ExamAssignmentInfo exam:getExams()) {
            for (ExamSectionInfo section:exam.getSectionsIncludeCrosslistedDummies()) {
                for (Long studentId : section.getStudentIds()) {
                    Student student = iStudents.get(studentId);
                    TreeSet<ExamSectionInfo> sectionsThisStudent = sections.get(student);
                    if (sectionsThisStudent==null) {
                        sectionsThisStudent = new TreeSet<ExamSectionInfo>();
                        sections.put(student, sectionsThisStudent);
                    }
                    sectionsThisStudent.add(section);
                }
            }
        }
        for (Student student : new TreeSet<Student>(sections.keySet())) {
            TreeSet<ExamSectionInfo> sectionsThisStudent = sections.get(student);
            if (!isOfSubjectArea(sectionsThisStudent)) continue;
            if (iSince!=null) {
                ChangeLog last = getLastChange(sectionsThisStudent);
                if (last==null || iSince.compareTo(last.getTimeStamp())>0) {
                    sLog.debug("No change found for "+student.getName(DepartmentalInstructor.sNameFormatLastFist));
                    continue;
                }
            }
            sLog.debug("  Generating file for "+student.getName(DepartmentalInstructor.sNameFormatLastFist));
            File file = gen.generate(filePrefix+"_"+
                    (student.getExternalUniqueId()!=null?student.getExternalUniqueId():student.getLastName()),
                    (mode==sModeText?"txt":"pdf")); 
                //ApplicationProperties.getTempFile(filePrefix+"_"+(instructor.getExternalUniqueId()!=null?instructor.getExternalUniqueId():instructor.getInstructor().getLastName()), (mode==sModeText?"txt":"pdf"));
            open(file, mode);
            printHeader();
            printReport(student, sectionsThisStudent);
            lastPage();
            close();
            files.put(student,file);
        }
        return files;
    }
}
