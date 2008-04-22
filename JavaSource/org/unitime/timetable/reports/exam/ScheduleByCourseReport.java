package org.unitime.timetable.reports.exam;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo;
import org.unitime.timetable.solver.exam.ui.ExamInfo;
import org.unitime.timetable.solver.exam.ui.ExamRoomInfo;
import org.unitime.timetable.solver.exam.ui.ExamInfo.ExamSectionInfo;

import com.lowagie.text.DocumentException;

public class ScheduleByCourseReport extends PdfLegacyExamReport {
    protected static Logger sLog = Logger.getLogger(ScheduleByCourseReport.class);
    
    public ScheduleByCourseReport(int mode, File file, Session session, int examType, SubjectArea subjectArea, Collection<ExamAssignmentInfo> exams) throws IOException, DocumentException {
        super(mode, file, "SCHEDULE BY COURSE", session, examType, subjectArea, exams);
    }
    
    public void printReport() throws DocumentException {
        sLog.debug("  Sorting sections ...");
        Hashtable<String,TreeSet<ExamSectionInfo>> subject2courseSections = new Hashtable();
        for (ExamInfo exam : getExams()) {
            for (ExamSectionInfo section : exam.getSections()) {
                if (getSubjectArea()!=null && !getSubjectArea().getSubjectAreaAbbreviation().equals(section.getSubject())) continue;
                TreeSet<ExamSectionInfo> sections = subject2courseSections.get(section.getSubject());
                if (sections==null) {
                    sections = new TreeSet();
                    subject2courseSections.put(section.getSubject(), sections);
                }
                sections.add(section);
            }
        }
        setHeader(new String[] {
                "Subj Crsnbr "+(iItype?"InsTyp ":"")+"Sect   Meeting Times                         Enrl    Date And Time                   Room         Cap ExCap ",
                "---- ------ "+(iItype?"------ ":"")+"---- -------------------------------------- -----  -------------------------------- ----------- ----- -----"});
        printHeader();
        for (Iterator<String> i = new TreeSet<String>(subject2courseSections.keySet()).iterator(); i.hasNext();) {
            String subject = i.next();
            TreeSet<ExamSectionInfo> sections = subject2courseSections.get(subject);
            setPageName(subject); setCont(subject);
            iSubjectPrinted = false;
            iCoursePrinted = false; String lastCourse = null;
            iITypePrinted = false; String lastItype = null;
            for (Iterator<ExamSectionInfo> j = sections.iterator(); j.hasNext();) {
                ExamSectionInfo  section = j.next();
                if (iCoursePrinted && !section.getCourseNbr().equals(lastCourse)) { iCoursePrinted = false; iITypePrinted = false; }
                if (iITypePrinted && !section.getItype().equals(lastItype)) iITypePrinted = false;
                if (section.getExamAssignment().getRooms()==null || section.getExamAssignment().getRooms().isEmpty()) {
                    println(
                            rpad(iSubjectPrinted?"":subject, 4)+" "+
                            rpad(iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                            (iItype?rpad(iITypePrinted?"":section.getItype(), 6)+" ":"")+
                            lpad(section.getSection(), 4)+" "+
                            rpad(getMeetingTime(section),38)+" "+
                            lpad(String.valueOf(section.getNrStudents()),5)+"  "+
                            rpad((section.getExamAssignment()==null?"":section.getExamAssignment().getPeriodNameFixedLength()),32)+" "+
                            (section.getExamAssignment()==null?"":iNoRoom)
                            );
                } else {
                    if (getLineNumber()+section.getExamAssignment().getRooms().size()>sNrLines) newPage();
                    boolean firstRoom = true;
                    for (ExamRoomInfo room : section.getExamAssignment().getRooms()) {
                        println(
                                rpad(!firstRoom || iSubjectPrinted?"":subject, 4)+" "+
                                rpad(!firstRoom || iCoursePrinted?"":section.getCourseNbr(), 6)+" "+
                                (iItype?rpad(!firstRoom || iITypePrinted?"":section.getItype(), 6)+" ":"")+
                                lpad(!firstRoom?"":section.getSection(), 4)+" "+
                                rpad(!firstRoom?"":getMeetingTime(section),38)+" "+
                                lpad(!firstRoom?"":String.valueOf(section.getNrStudents()),5)+"  "+
                                rpad(!firstRoom?"":(section.getExamAssignment()==null?"":section.getExamAssignment().getPeriodNameFixedLength()),32)+" "+
                                formatRoom(room.getName())+" "+
                                lpad(""+room.getCapacity(),5)+" "+
                                lpad(""+room.getExamCapacity(),5)
                                );
                        firstRoom = false;
                    }
                }
                if (iNewPage) {
                    iSubjectPrinted = iITypePrinted = iCoursePrinted = false;
                    lastItype = lastCourse = null;
                } else {
                    iSubjectPrinted = iITypePrinted = iCoursePrinted = true;
                    lastItype = section.getItype();
                    lastCourse = section.getCourseNbr();
                }
                if (j.hasNext()) { 
                    if (!iNewPage) println(""); 
                }
            }
            setCont(null);
            if (i.hasNext()) {
                newPage();
            }
        }
        lastPage();        
    }
}
