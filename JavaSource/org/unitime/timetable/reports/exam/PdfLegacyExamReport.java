package org.unitime.timetable.reports.exam;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.sf.cpsolver.coursett.model.TimeLocation;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.ClassEvent;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseEvent;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.Event.MultiMeeting;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.reports.PdfLegacyReport;
import org.unitime.timetable.solver.exam.ui.ExamAssignment;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo.Parameters;
import org.unitime.timetable.solver.exam.ui.ExamInfo.ExamInstructorInfo;
import org.unitime.timetable.solver.exam.ui.ExamInfo.ExamSectionInfo;
import org.unitime.timetable.util.Constants;

import com.lowagie.text.DocumentException;

public abstract class PdfLegacyExamReport extends PdfLegacyReport {
    protected static Logger sLog = Logger.getLogger(PdfLegacyExamReport.class);
    
    public static Hashtable<String,Class> sRegisteredReports = new Hashtable();
    public static String sAllRegisteredReports = "";
    private Collection<ExamAssignmentInfo> iExams = null;
    private Session iSession = null;
    private SubjectArea iSubjectArea = null;
    private int iExamType = -1;
    
    protected boolean iDispRooms = true;
    protected String iNoRoom = "";
    protected boolean iDirect = true;
    protected boolean iM2d = true;
    protected boolean iBtb = false;
    protected int iLimit = -1;
    protected boolean iItype = false;
    protected boolean iClassSchedule = false;
    protected Hashtable<String,String> iRoomCodes = new Hashtable();
    protected String iRC = null;
    protected boolean iTotals = true;
    protected boolean iUseClassSuffix = false;
    protected boolean iDispLimits = true;
    protected Date iSince = null;
    protected boolean iExternal = false;
    protected boolean iDispFullTermDates = false;
    protected boolean iFullTermCheckDatePattern = true;
    
    protected static DecimalFormat sDF = new DecimalFormat("0.0");
    
    static {
        sRegisteredReports.put("crsn", ScheduleByCourseReport.class);
        sRegisteredReports.put("conf", ConflictsByCourseAndStudentReport.class);
        sRegisteredReports.put("iconf", ConflictsByCourseAndInstructorReport.class);
        sRegisteredReports.put("pern", ScheduleByPeriodReport.class);
        sRegisteredReports.put("xpern", ExamScheduleByPeriodReport.class);
        sRegisteredReports.put("room", ScheduleByRoomReport.class);
        sRegisteredReports.put("chart", PeriodChartReport.class);
        sRegisteredReports.put("ver", ExamVerificationReport.class);
        sRegisteredReports.put("abbv", AbbvScheduleByCourseReport.class);
        sRegisteredReports.put("xabbv", AbbvExamScheduleByCourseReport.class);
        sRegisteredReports.put("instr", InstructorExamReport.class);
        sRegisteredReports.put("stud", StudentExamReport.class);
        for (String report : sRegisteredReports.keySet())
            sAllRegisteredReports += (sAllRegisteredReports.length()>0?",":"") + report;
    }
    
    public PdfLegacyExamReport(int mode, File file, String title, Session session, int examType, SubjectArea subjectArea, Collection<ExamAssignmentInfo> exams) throws DocumentException, IOException {
        super(mode, file, title, 
                (examType<0?"":examType==Exam.sExamTypeFinal?ApplicationProperties.getProperty("tmtbl.exam.report.final","FINAL"):ApplicationProperties.getProperty("tmtbl.exam.report.midterm","MIDTERM"))+
                " EXAMINATIONS", 
                title + " -- " + session.getLabel(), session.getLabel());
        if (subjectArea!=null) setFooter(subjectArea.getSubjectAreaAbbreviation());
        iExams = exams;
        iSession = session;
        iExamType = examType;
        iSubjectArea = subjectArea;
        iDispRooms = "true".equals(System.getProperty("room","true"));
        iNoRoom = System.getProperty("noroom",ApplicationProperties.getProperty("tmtbl.exam.report.noroom","INSTR OFFC"));
        iDirect = "true".equals(System.getProperty("direct","true"));
        iM2d = "true".equals(System.getProperty("m2d",(examType==Exam.sExamTypeFinal?"true":"false")));
        iBtb = "true".equals(System.getProperty("btb","false"));
        iLimit = Integer.parseInt(System.getProperty("limit", "-1"));
        iItype = "true".equals(System.getProperty("itype",ApplicationProperties.getProperty("tmtbl.exam.report.itype","true")));
        iTotals = "true".equals(System.getProperty("totals","true"));
        iUseClassSuffix = "true".equals(System.getProperty("suffix",ApplicationProperties.getProperty("tmtbl.exam.report.suffix","false")));
        iExternal = "true".equals(ApplicationProperties.getProperty("tmtbl.exam.report.external","false"));
        iDispLimits = "true".equals(System.getProperty("verlimit","true"));
        iClassSchedule = "true".equals(System.getProperty("cschedule",ApplicationProperties.getProperty("tmtbl.exam.report.cschedule","true")));
        iDispFullTermDates = "true".equals(System.getProperty("fullterm","false"));
        iFullTermCheckDatePattern = "true".equals(ApplicationProperties.getProperty("tmtbl.exam.report.fullterm.checkdp","true"));
        if (System.getProperty("since")!=null) {
            try {
                iSince = new SimpleDateFormat(System.getProperty("sinceFormat","MM/dd/yy")).parse(System.getProperty("since"));
            } catch (Exception e) {
                sLog.error("Unable to parse date "+System.getProperty("since")+", reason: "+e.getMessage());
            }
        }
        setRoomCode(System.getProperty("roomcode",ApplicationProperties.getProperty("tmtbl.exam.report.roomcode")));
    }
    
    public void setDispRooms(boolean dispRooms) { iDispRooms = dispRooms; }
    public void setNoRoom(String noRoom) { iNoRoom = noRoom; }
    public void setDirect(boolean direct) { iDirect = direct; }
    public void setM2d(boolean m2d) { iM2d = m2d; }
    public void setBtb(boolean btb) { iBtb = btb; }
    public void setLimit(int limit) { iLimit = limit; }
    public void setItype(boolean itype) { iItype = itype; }
    public void setTotals(boolean totals) { iTotals = totals; }
    public void setUseClassSuffix(boolean useClassSuffix) { iUseClassSuffix = true; }
    public void setDispLimits(boolean dispLimits) { iDispLimits = dispLimits; }
    public void setClassSchedule(boolean classSchedule) { iClassSchedule = classSchedule; }
    public void setSince(Date since) { iSince = since; }
    public void setDispFullTermDates(boolean dispFullTermDates) { iDispFullTermDates = dispFullTermDates; }
    public void setRoomCode(String roomCode) {
        if (roomCode==null || roomCode.length()==0) {
            iRoomCodes = null;
            iRC = null;
            return;
        }
        iRoomCodes = new Hashtable<String, String>();
        iRC = "";
        for (StringTokenizer s = new StringTokenizer(roomCode,":;,=");s.hasMoreTokens();) {
            String room = s.nextToken(), code = s.nextToken();
            iRoomCodes.put(room, code);
            if (iRC.length()>0) iRC += ", ";
            iRC += code+":"+room;
        }
    }


    public Collection<ExamAssignmentInfo> getExams() {
        return iExams;
    }
    
    public Session getSession() {
        return iSession; 
    }
    
    public int getExamType() {
        return iExamType;
    }
    
    public SubjectArea getSubjectArea() {
        return iSubjectArea;
    }
    
    public abstract void printReport() throws DocumentException; 
    
    protected boolean iSubjectPrinted = false;
    protected boolean iITypePrinted = false;
    protected boolean iConfigPrinted = false;
    protected boolean iCoursePrinted = false;
    protected boolean iStudentPrinted = false;
    protected boolean iPeriodPrinted = false;
    protected boolean iNewPage = false;
    
    protected void headerPrinted() {
        iSubjectPrinted = false;
        iCoursePrinted = false;
        iStudentPrinted = false;
        iPeriodPrinted = false;
        iITypePrinted = false;
        iConfigPrinted = false;
        iNewPage = true;
    }
    
    protected void println(String text) throws DocumentException {
        iNewPage = false;
        super.println(text);
    }
    
    public int getDaysCode(Set meetings) {
        int daysCode = 0;
        for (Iterator i=meetings.iterator();i.hasNext();) {
            Meeting meeting = (Meeting)i.next();
            Calendar date = Calendar.getInstance(Locale.US);
            date.setTime(meeting.getMeetingDate());
            switch (date.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY : daysCode |= Constants.DAY_CODES[Constants.DAY_MON]; break;
            case Calendar.TUESDAY : daysCode |= Constants.DAY_CODES[Constants.DAY_TUE]; break;
            case Calendar.WEDNESDAY : daysCode |= Constants.DAY_CODES[Constants.DAY_WED]; break;
            case Calendar.THURSDAY : daysCode |= Constants.DAY_CODES[Constants.DAY_THU]; break;
            case Calendar.FRIDAY : daysCode |= Constants.DAY_CODES[Constants.DAY_FRI]; break;
            case Calendar.SATURDAY : daysCode |= Constants.DAY_CODES[Constants.DAY_SAT]; break;
            case Calendar.SUNDAY : daysCode |= Constants.DAY_CODES[Constants.DAY_SUN]; break;
            }
        }
        return daysCode;
    }
    
    public static String DAY_NAMES_SHORT[] = new String[] {
        "M", "T", "W", "R", "F", "S", "U"
    }; 
    
    public String getMeetingDate(MultiMeeting m) {
        if (m.getMeetings().isEmpty()) return "ARRANGED HOURS";
        SimpleDateFormat df = new SimpleDateFormat("MM/dd");
        return 
            df.format(m.getMeetings().first().getMeetingDate())+" - "+
            df.format(m.getMeetings().last().getMeetingDate())+" "+m.getDays(DAY_NAMES_SHORT,DAY_NAMES_SHORT);
    }
    
    public boolean isFullTerm(ClassEvent classEvent) {
        if (iFullTermCheckDatePattern) {
            DatePattern dp = classEvent.getClazz().effectiveDatePattern();
            if (dp!=null) return dp.isDefault();
        }
        if (classEvent!=null && !classEvent.getMeetings().isEmpty()) {
            Date first = null, last = null;
            for (Iterator i=classEvent.getMeetings().iterator();i.hasNext();) {
                Meeting m = (Meeting)i.next();
                if (first==null || first.compareTo(m.getMeetingDate())>0) first = m.getMeetingDate();
                if (last==null || last.compareTo(m.getMeetingDate())<0) last = m.getMeetingDate();
            }
            Calendar c = Calendar.getInstance(Locale.US);
            c.setTime(getSession().getSessionBeginDateTime());
            c.add(Calendar.WEEK_OF_YEAR, 2);
            if (first.compareTo(c.getTime())>=0) return false;  
            c.setTime(getSession().getClassesEndDateTime());
            c.add(Calendar.WEEK_OF_YEAR, -2);
            if (last.compareTo(c.getTime())<=0) return false;
            return true;
        }
        return false;
    }
    
    protected String getMeetingTime(ExamSectionInfo section) {
        String meetingTime = "";
        if (section.getOwner().getOwnerObject() instanceof Class_) {
            SimpleDateFormat dpf = new SimpleDateFormat("MM/dd");
            Class_ clazz = (Class_)section.getOwner().getOwnerObject();
            Assignment assignment = clazz.getCommittedAssignment();
            TreeSet meetings = (clazz.getEvent()==null?null:new TreeSet(clazz.getEvent().getMeetings()));
            if (meetings!=null && !meetings.isEmpty()) {
                int dayCode = getDaysCode(meetings);
                String days = "";
                for (int i=0;i<Constants.DAY_CODES.length;i++)
                    if ((dayCode & Constants.DAY_CODES[i])!=0) days += DAY_NAMES_SHORT[i];
                meetingTime += rpad(days,5);
                Meeting first = (Meeting)meetings.first();
                meetingTime += " "+lpad(first.startTime(),6)+" - "+lpad(first.stopTime(),6);
            } else if (assignment!=null) {
                TimeLocation t = assignment.getTimeLocation();
                meetingTime += rpad(t.getDayHeader(),5)+" "+lpad(t.getStartTimeHeader(),6)+" - "+lpad(t.getEndTimeHeader(),6);
            } else {
                meetingTime += rpad("",21);
            }
            if (meetings!=null && !meetings.isEmpty()) {
                Date first = ((Meeting)meetings.first()).getMeetingDate();
                Date last = ((Meeting)meetings.last()).getMeetingDate();
                if (!iDispFullTermDates && isFullTerm(clazz.getEvent())) {
                    meetingTime += rpad("",14);
                } else {
                    meetingTime += dpf.format(first)+" - "+dpf.format(last);
                }
            } else if (assignment!=null && assignment.getDatePattern()!=null) {
                DatePattern dp = assignment.getDatePattern();
                if (dp!=null && !dp.isDefault()) {
                    if (dp.getType().intValue()==DatePattern.sTypeAlternate)
                        meetingTime += " "+rpad(dp.getName(),13);
                    else {
                        meetingTime += " "+dpf.format(dp.getStartDate())+" - "+dpf.format(dp.getEndDate());
                    }
                }
            } else {
                meetingTime += rpad("",14);
            }
        }
        return meetingTime;
    }
    
    protected String getMeetingTime(Meeting meeting) {
        return lpad(meeting.startTime(),6)+" - "+lpad(meeting.stopTime(),6);
    }
    
    protected String getMeetingTime(String time) {
        int idx = time.indexOf('-');
        if (idx<0) return lpad(time,15);
        String start = time.substring(0,idx).trim();
        String stop = time.substring(idx+1).trim();
        return lpad(start,'0',6)+" - "+lpad(stop,'0',6);
    }

    public String formatRoom(String room) {
        String r = room.trim();
        int idx = r.lastIndexOf(' '); 
        if (idx>=0 && idx<=5 && r.length()-idx-1<=5)
            return rpad(r.substring(0, idx),5)+" "+rpad(room.substring(idx+1),5);
        return rpad(room,11);
    }
    
    public String formatPeriod(ExamPeriod period) {
        return period.getStartDateLabel()+" "+lpad(period.getStartTimeLabel(),6)+" - "+lpad(period.getEndTimeLabel(),6);
    }

    public String formatPeriod(ExamPeriod period, int length) {
        return period.getStartDateLabel()+" "+lpad(period.getStartTimeLabel(),6)+" - "+lpad(period.getEndTimeLabel(length),6);
    }
    
    public String formatPeriod(ExamAssignment assignment) {
        return assignment.getPeriod().getStartDateLabel()+" "+
            lpad(assignment.getPeriod().getStartTimeLabel(),6)+" - "+
            lpad(assignment.getPeriod().getEndTimeLabel(assignment.getLength()),6);
    }

    public String getShortDate(Date date) {
        Calendar c = Calendar.getInstance(Locale.US);
        c.setTime(date);
        String day = "";
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY : day = DAY_NAMES_SHORT[Constants.DAY_MON]; break;
            case Calendar.TUESDAY : day = DAY_NAMES_SHORT[Constants.DAY_TUE]; break;
            case Calendar.WEDNESDAY : day = DAY_NAMES_SHORT[Constants.DAY_WED]; break;
            case Calendar.THURSDAY : day = DAY_NAMES_SHORT[Constants.DAY_THU]; break;
            case Calendar.FRIDAY : day = DAY_NAMES_SHORT[Constants.DAY_FRI]; break;
            case Calendar.SATURDAY : day = DAY_NAMES_SHORT[Constants.DAY_SAT]; break;
            case Calendar.SUNDAY : day = DAY_NAMES_SHORT[Constants.DAY_SUN]; break;
        }
        return day+" "+new SimpleDateFormat("MM/dd").format(date);
    }
    
    public String formatShortPeriod(ExamPeriod period, int length) {
        return getShortDate(period.getStartDate())+" "+lpad(period.getStartTimeLabel(),6)+"-"+lpad(period.getEndTimeLabel(),6);
    }
    
    public String formatShortPeriod(ExamAssignment assignment) {
        return getShortDate(assignment.getPeriod().getStartDate())+" "+lpad(assignment.getPeriod().getStartTimeLabel(),6)+"-"+lpad(assignment.getPeriod().getEndTimeLabel(assignment.getLength()),6);
    }

    public String getItype(Class_ clazz) {
        if (iExternal) {
            String ext = clazz.getExternalUniqueId();
            return (ext==null?"":ext);
        } else
            return clazz.getSchedulingSubpart().getItypeDesc();
    }
    
    public static void sendEmails(String prefix, Hashtable<String,File> output, Hashtable<SubjectArea,Hashtable<String,File>> outputPerSubject, Hashtable<ExamInstructorInfo,File> ireports, Hashtable<Student,File> sreports) throws MessagingException, UnsupportedEncodingException {
        String managerExternalId = System.getProperty("sender");
        TimetableManager mgr = (managerExternalId==null?null:TimetableManager.findByExternalId(managerExternalId));
        InternetAddress from = null;
        if (System.getProperty("email.from")!=null)
            from = new InternetAddress(System.getProperty("email.from"), System.getProperty("email.from.name"));
        else
            from = new InternetAddress(
                            ApplicationProperties.getProperty("tmtbl.inquiry.sender",ApplicationProperties.getProperty("tmtbl.contact.email")),
                            ApplicationProperties.getProperty("tmtbl.inquiry.sender.name"));
        
        sLog.info("Sending email(s)...");
        Properties p = ApplicationProperties.getProperties();
        if (p.getProperty("mail.smtp.host")==null && p.getProperty("tmtbl.smtp.host")!=null)
            p.setProperty("mail.smtp.host", p.getProperty("tmtbl.smtp.host"));
        Authenticator a = null;
        if (ApplicationProperties.getProperty("tmtbl.mail.user")!=null && ApplicationProperties.getProperty("tmtbl.mail.pwd")!=null) {
            a = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            ApplicationProperties.getProperty("tmtbl.mail.user"),
                            ApplicationProperties.getProperty("tmtbl.mail.pwd"));
                }
            };
        }
        javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(p, a);
        if (!outputPerSubject.isEmpty() && "true".equals(System.getProperty("email.deputies","false"))) {
                Hashtable<TimetableManager,Hashtable<String,File>> files2send = new Hashtable();
                for (Map.Entry<SubjectArea, Hashtable<String,File>> entry : outputPerSubject.entrySet()) {
                    if (entry.getKey().getDepartment().getTimetableManagers().isEmpty())
                        sLog.warn("No manager associated with subject area "+entry.getKey().getSubjectAreaAbbreviation()+" ("+entry.getKey().getDepartment().getLabel()+")</font>");
                    for (Iterator i=entry.getKey().getDepartment().getTimetableManagers().iterator();i.hasNext();) {
                        TimetableManager g = (TimetableManager)i.next();
                        if (g.getEmailAddress()==null || g.getEmailAddress().length()==0) {
                            sLog.warn("Manager "+g.getName()+" has no email address.");
                        } else {
                            Hashtable<String,File> files = files2send.get(g);
                            if (files==null) { files = new Hashtable<String,File>(); files2send.put(g, files); }
                            files.putAll(entry.getValue());
                        }
                    }
                }
                if (files2send.isEmpty()) {
                    sLog.error("Nothing to send.");
                } else {
                    Set<TimetableManager> managers = files2send.keySet();
                    while (!managers.isEmpty()) {
                        TimetableManager manager = managers.iterator().next();
                        Hashtable<String,File> files = files2send.get(manager);
                        managers.remove(manager);
                        sLog.info("Sending email to "+manager.getName()+" ("+manager.getEmailAddress()+")...");
                        MimeMessage mail = new MimeMessage(mailSession);
                        mail.setSubject(System.getProperty("email.subject","Examination Report"));
                        Multipart body = new MimeMultipart();
                        BodyPart text = new MimeBodyPart();
                        String message = System.getProperty("email.body");
                        String url = System.getProperty("email.url");
                        text.setText((message==null?"":message+"\r\n\r\n")+
                                (url==null?"":"For an up-to-date report, please visit "+url+"/\r\n\r\n")+
                                "This email was automatically generated by "+
                                "UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER.replaceAll("@build.number@", "?")+
                                " (Univesity Timetabling Application, http://www.unitime.org).");
                        body.addBodyPart(text);
                        mail.addRecipient(RecipientType.TO, new InternetAddress(manager.getEmailAddress(),manager.getName()));
                        for (Iterator<TimetableManager> i=managers.iterator();i.hasNext();) {
                            TimetableManager m = (TimetableManager)i.next();
                            if (files.equals(files2send.get(m))) {
                                sLog.info("  Including "+m.getName()+" ("+m.getEmailAddress()+")");
                                mail.addRecipient(RecipientType.TO, new InternetAddress(m.getEmailAddress(),m.getName()));
                                i.remove();
                            }
                        }
                        if (System.getProperty("email.to")!=null) for (StringTokenizer s=new StringTokenizer(System.getProperty("email.to"),";,\n\r ");s.hasMoreTokens();) 
                            mail.addRecipient(RecipientType.TO, new InternetAddress(s.nextToken()));
                        if (System.getProperty("email.cc")!=null) for (StringTokenizer s=new StringTokenizer(System.getProperty("email.to"),";,\n\r ");s.hasMoreTokens();) 
                            mail.addRecipient(RecipientType.CC, new InternetAddress(s.nextToken()));
                        if (System.getProperty("email.bcc")!=null) for (StringTokenizer s=new StringTokenizer(System.getProperty("email.to"),";,\n\r ");s.hasMoreTokens();) 
                            mail.addRecipient(RecipientType.BCC, new InternetAddress(s.nextToken()));
                        if (from!=null)
                            mail.setFrom(from);
                        for (Map.Entry<String, File> entry : files.entrySet()) {
                            BodyPart attachement = new MimeBodyPart();
                            attachement.setDataHandler(new DataHandler(new FileDataSource(entry.getValue())));
                            attachement.setFileName(prefix+"_"+entry.getKey());
                            body.addBodyPart(attachement);
                            sLog.info("  Attaching <a href='temp/"+entry.getValue().getName()+"'>"+entry.getKey()+"</a>");
                        }
                        mail.setSentDate(new Date());
                        mail.setContent(body);
                        try {
                            Transport.send(mail);
                            sLog.info("Email sent.");
                        } catch (Exception e) {
                            sLog.error("Unable to send email: "+e.getMessage());
                        }
                    }
                }
            } else {
                MimeMessage mail = new MimeMessage(mailSession);
                mail.setSubject(System.getProperty("email.subject","Examination Report"));
                Multipart body = new MimeMultipart();
                BodyPart text = new MimeBodyPart();
                String message = System.getProperty("email.body");
                String url = System.getProperty("email.url");
                text.setText((message==null?"":message+"\r\n\r\n")+
                        (url==null?"":"For an up-to-date report, please visit "+url+"/\r\n\r\n")+
                        "This email was automatically generated by "+
                        "UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER.replaceAll("@build.number@", "?")+
                        " (Univesity Timetabling Application, http://www.unitime.org).");
                body.addBodyPart(text);
                if (System.getProperty("email.to")!=null) for (StringTokenizer s=new StringTokenizer(System.getProperty("email.to"),";,\n\r ");s.hasMoreTokens();) 
                    mail.addRecipient(RecipientType.TO, new InternetAddress(s.nextToken()));
                if (System.getProperty("email.cc")!=null) for (StringTokenizer s=new StringTokenizer(System.getProperty("email.to"),";,\n\r ");s.hasMoreTokens();) 
                    mail.addRecipient(RecipientType.CC, new InternetAddress(s.nextToken()));
                if (System.getProperty("email.bcc")!=null) for (StringTokenizer s=new StringTokenizer(System.getProperty("email.to"),";,\n\r ");s.hasMoreTokens();) 
                    mail.addRecipient(RecipientType.BCC, new InternetAddress(s.nextToken()));
                if (from!=null)
                    mail.setFrom(from);
                for (Map.Entry<String, File> entry : output.entrySet()) {
                    BodyPart attachement = new MimeBodyPart();
                    attachement.setDataHandler(new DataHandler(new FileDataSource(entry.getValue())));
                    attachement.setFileName(prefix+"_"+entry.getKey());
                    body.addBodyPart(attachement);
                }
                mail.setSentDate(new Date());
                mail.setContent(body);
                try {
                    Transport.send(mail);
                    sLog.info("Email sent.");
                } catch (Exception e) {
                    sLog.error("Unable to send email: "+e.getMessage());
                }
            }
            if ("true".equals(System.getProperty("email.instructors","false")) && ireports!=null && !ireports.isEmpty()) {
                sLog.info("Emailing instructors...");
                for (ExamInstructorInfo instructor : new TreeSet<ExamInstructorInfo>(ireports.keySet())) {
                    File report = ireports.get(instructor);
                    String email = instructor.getInstructor().getEmail();
                    if (email==null || email.length()==0) {
                        sLog.warn("Unable to email <a href='temp/"+report.getName()+"'>"+instructor.getName()+"</a> -- instructor has no email address.");
                        continue;
                    }
                    MimeMessage mail = new MimeMessage(mailSession);
                    mail.setSubject(System.getProperty("email.subject","Examination Report"));
                    Multipart body = new MimeMultipart();
                    BodyPart text = new MimeBodyPart();
                    String message = System.getProperty("email.body");
                    String url = System.getProperty("email.url");
                    text.setText((message==null?"":message+"\r\n\r\n")+
                            (url==null?"":"For an up-to-date report, please visit "+url+"/\r\n\r\n")+
                            "This email was automatically generated by "+
                            "UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER.replaceAll("@build.number@", "?")+
                            " (Univesity Timetabling Application, http://www.unitime.org).");
                    body.addBodyPart(text);
                    mail.addRecipient(RecipientType.TO, new InternetAddress(email));
                    if (from!=null) mail.setFrom(from);
                    BodyPart attachement = new MimeBodyPart();
                    attachement.setDataHandler(new DataHandler(new FileDataSource(report)));
                    attachement.setFileName(prefix+(report.getName().endsWith(".txt")?".txt":".pdf"));
                    mail.setSentDate(new Date());
                    mail.setContent(body);
                    try {
                        Transport.send(mail);
                        sLog.info("&nbsp;&nbsp;An email was sent to <a href='temp/"+report.getName()+"'>"+instructor.getName()+"</a>.");
                    } catch (Exception e) {
                        sLog.error("Unable to email <a href='temp/"+report.getName()+"'>"+instructor.getName()+"</a> -- "+e.getMessage());
                    }
                }
                sLog.info("Emails sent.");
            }
            if ("true".equals(System.getProperty("email.students","false")) && sreports!=null && !sreports.isEmpty()) {
                sLog.info("Emailing instructors...");
                for (Student student : new TreeSet<Student>(sreports.keySet())) {
                    File report = sreports.get(student);
                    String email = student.getEmail();
                    if (email==null || email.length()==0) {
                        sLog.warn("  Unable to email <a href='temp/"+report.getName()+"'>"+student.getName(DepartmentalInstructor.sNameFormatLastFist)+"</a> -- student has no email address.");
                        continue;
                    }
                    MimeMessage mail = new MimeMessage(mailSession);
                    mail.setSubject(System.getProperty("email.subject","Examination Report"));
                    Multipart body = new MimeMultipart();
                    BodyPart text = new MimeBodyPart();
                    String message = System.getProperty("email.body");
                    String url = System.getProperty("email.url");
                    text.setText((message==null?"":message+"\r\n\r\n")+
                            (url==null?"":"For an up-to-date report, please visit "+url+"/\r\n\r\n")+
                            "This email was automatically generated by "+
                            "UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER.replaceAll("@build.number@", "?")+
                            " (Univesity Timetabling Application, http://www.unitime.org).");
                    body.addBodyPart(text);
                    mail.addRecipient(RecipientType.TO, new InternetAddress(email));
                    if (from!=null) mail.setFrom(from);
                    BodyPart attachement = new MimeBodyPart();
                    attachement.setDataHandler(new DataHandler(new FileDataSource(report)));
                    attachement.setFileName(prefix+(report.getName().endsWith(".txt")?".txt":".pdf"));
                    mail.setSentDate(new Date());
                    mail.setContent(body);
                    try {
                        Transport.send(mail);
                        sLog.info(" An email was sent to <a href='temp/"+report.getName()+"'>"+student.getName(DepartmentalInstructor.sNameFormatLastFist)+"</a>.");
                    } catch (Exception e) {
                        sLog.error("Unable to email <a href='temp/"+report.getName()+"'>"+student.getName(DepartmentalInstructor.sNameFormatLastFist)+"</a> -- "+e.getMessage()+".");
                    }
                }
                sLog.info("Emails sent.");
            }
    }
    
    public static TreeSet<ExamAssignmentInfo> loadExams(Long sessionId, int examType, boolean assgn) throws Exception {
        sLog.info("Loading exams...");
        long t0 = System.currentTimeMillis();
        Hashtable<Long, Exam> exams = new Hashtable();
        for (Iterator i=new ExamDAO().getSession().createQuery(
                "select x from Exam x where x.session.uniqueId=:sessionId and x.examType=:examType"
                ).setLong("sessionId", sessionId).setInteger("examType", examType).setCacheable(true).list().iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            exams.put(exam.getUniqueId(), exam);
        }
        sLog.info("  Fetching related objects (class)...");
        new ExamDAO().getSession().createQuery(
                "select c from Class_ c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:classType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("classType", ExamOwner.sOwnerTypeClass).setCacheable(true).list();
        sLog.info("  Fetching related objects (config)...");
        new ExamDAO().getSession().createQuery(
                "select c from InstrOfferingConfig c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:configType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("configType", ExamOwner.sOwnerTypeConfig).setCacheable(true).list();
        sLog.info("  Fetching related objects (course)...");
        new ExamDAO().getSession().createQuery(
                "select c from CourseOffering c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:courseType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("courseType", ExamOwner.sOwnerTypeCourse).setCacheable(true).list();
        sLog.info("  Fetching related objects (offering)...");
        new ExamDAO().getSession().createQuery(
                "select c from InstructionalOffering c, ExamOwner o where o.exam.session.uniqueId=:sessionId and o.exam.examType=:examType and o.ownerType=:offeringType and c.uniqueId=o.ownerId")
                .setLong("sessionId", sessionId)
                .setInteger("examType", examType)
                .setInteger("offeringType", ExamOwner.sOwnerTypeOffering).setCacheable(true).list();
        Hashtable<Long,Set<Long>> owner2students = new Hashtable();
        Hashtable<Long,Set<Exam>> student2exams = new Hashtable();
        if (assgn) {
            sLog.info("  Loading students (class)...");
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
            sLog.info("  Loading students (config)...");
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
            sLog.info("  Loading students (course)...");
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
            sLog.info("  Loading students (offering)...");
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
        }
        Hashtable<Long, Set<Meeting>> period2meetings = new Hashtable();
        if (assgn && "true".equals(ApplicationProperties.getProperty("tmtbl.exam.eventConflicts."+(examType==Exam.sExamTypeFinal?"final":"midterm"),"true"))) {
            sLog.info("  Loading overlapping class meetings...");
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
            sLog.info("  Loading overlapping course meetings...");
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
        }
        Parameters p = new Parameters(sessionId, examType);
        sLog.info("  Creating exam assignments...");
        TreeSet<ExamAssignmentInfo> ret = new TreeSet();
        for (Enumeration<Exam> e = exams.elements(); e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            ExamAssignmentInfo info = (assgn?new ExamAssignmentInfo(exam, owner2students, student2exams, period2meetings, p):new ExamAssignmentInfo(exam, (ExamPeriod)null, null));
            ret.add(info);
        }
        long t1 = System.currentTimeMillis();
        sLog.info("Exams loaded in "+sDF.format((t1-t0)/1000.0)+"s.");
        return ret;
    }
    
    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.setProperty("log4j.rootLogger", "DEBUG, A1");
            props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
            props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
            props.setProperty("log4j.appender.A1.layout.ConversionPattern","%-5p %c{2}: %m%n");
            props.setProperty("log4j.logger.org.hibernate","INFO");
            props.setProperty("log4j.logger.org.hibernate.cfg","WARN");
            props.setProperty("log4j.logger.org.hibernate.cache.EhCacheProvider","ERROR");
            props.setProperty("log4j.logger.org.unitime.commons.hibernate","INFO");
            props.setProperty("log4j.logger.net","INFO");
            PropertyConfigurator.configure(props);
            
            HibernateUtil.configureHibernate(ApplicationProperties.getProperties());
            
            Session session = Session.getSessionUsingInitiativeYearTerm(
                    ApplicationProperties.getProperty("initiative", "puWestLafayetteTrdtn"),
                    ApplicationProperties.getProperty("year","2008"),
                    ApplicationProperties.getProperty("term","Spr")
                    );
            if (session==null) {
                sLog.error("Academic session not found, use properties initiative, year, and term to set academic session.");
                System.exit(0);
            } else {
                sLog.info("Session: "+session);
            }
            int examType = (ApplicationProperties.getProperty("type","final").equalsIgnoreCase("final")?Exam.sExamTypeFinal:Exam.sExamTypeMidterm);
            boolean assgn = "true".equals(System.getProperty("assgn","true"));
            int mode = sModeNormal;
            if ("text".equals(System.getProperty("mode"))) mode = sModeText;
            if ("ledger".equals(System.getProperty("mode"))) mode = sModeLedger;
            sLog.info("Exam type: "+Exam.sExamTypes[examType]);
            boolean perSubject = "true".equals(System.getProperty("persubject","false"));
            TreeSet<SubjectArea> subjects = null;
            if (System.getProperty("subject")!=null) {
                sLog.info("Loading subjects...");
                perSubject = true;
                subjects = new TreeSet();
                String inSubjects = "";
                for (StringTokenizer s=new StringTokenizer(System.getProperty("subject"),",");s.hasMoreTokens();)
                    inSubjects += "'"+s.nextToken()+"'"+(s.hasMoreTokens()?",":"");
                subjects.addAll(new _RootDAO().getSession().createQuery(
                        "select sa from SubjectArea sa where sa.session.uniqueId=:sessionId and sa.subjectAreaAbbreviation in ("+inSubjects+")"
                        ).setLong("sessionId", session.getUniqueId()).list());
            }
            TreeSet<ExamAssignmentInfo> exams = loadExams(session.getUniqueId(), examType, assgn);
            if (subjects==null) {
                subjects = new TreeSet();
                for (ExamAssignmentInfo exam: exams)
                    for (ExamSectionInfo section: exam.getSections())
                        subjects.add(section.getOwner().getCourse().getSubjectArea());
            }
            /*
            if (subjects==null) {
                if (perSubject) examsPerSubj = new Hashtable();
                for (Iterator i=Exam.findAll(session.getUniqueId(),examType).iterator();i.hasNext();) {
                    ExamAssignmentInfo exam = (assgn?new ExamAssignmentInfo((Exam)i.next()):new ExamAssignmentInfo((Exam)i.next(),null,null,null,null));
                    exams.add(exam);
                    if (perSubject) {
                        HashSet<SubjectArea> sas = new HashSet<SubjectArea>();
                        for (Iterator j=exam.getExam().getOwners().iterator();j.hasNext();) {
                            ExamOwner owner = (ExamOwner)j.next();
                            SubjectArea sa = owner.getCourse().getSubjectArea();
                            if (!sas.add(sa)) continue;
                            Vector<ExamAssignmentInfo> x = examsPerSubj.get(sa);
                            if (x==null) { x = new Vector(); examsPerSubj.put(sa,x); }
                            x.add(exam);
                        }
                    }
                }
            } else for (SubjectArea subject : subjects) {
                Vector<ExamAssignmentInfo> examsOfThisSubject = new Vector();
                for (Iterator i=Exam.findExamsOfSubjectArea(subject.getUniqueId(),examType).iterator();i.hasNext();) {
                    ExamAssignmentInfo exam = (assgn?new ExamAssignmentInfo((Exam)i.next()):new ExamAssignmentInfo((Exam)i.next(),null,null,null,null)); 
                    exams.add(exam);
                    examsOfThisSubject.add(exam);
                }
                examsPerSubj.put(subject, examsOfThisSubject);
            }
            */
            Hashtable<String,File> output = new Hashtable();
            Hashtable<SubjectArea,Hashtable<String,File>> outputPerSubject = new Hashtable();
            Hashtable<ExamInstructorInfo,File> ireports = null;
            Hashtable<Student,File> sreports = null;
            for (StringTokenizer stk=new StringTokenizer(ApplicationProperties.getProperty("report",sAllRegisteredReports),",");stk.hasMoreTokens();) {
                String reportName = stk.nextToken();
                Class reportClass = sRegisteredReports.get(reportName);
                if (reportClass==null) continue;
                sLog.info("Report: "+reportClass.getName().substring(reportClass.getName().lastIndexOf('.')+1));
                if (perSubject) {
                    for (SubjectArea subject : subjects) {
                        File file = new File(new File(ApplicationProperties.getProperty("output",".")),
                            session.getAcademicTerm()+session.getYear()+(examType==Exam.sExamTypeMidterm?"evn":"fin")+"_"+reportName+"_"+subject.getSubjectAreaAbbreviation()+(mode==sModeText?".txt":".pdf"));
                        long t0 = System.currentTimeMillis();
                        sLog.info("Generating report "+file+" ("+subject.getSubjectAreaAbbreviation()+") ...");
                        PdfLegacyExamReport report = (PdfLegacyExamReport)reportClass.getConstructor(int.class, File.class, Session.class, int.class, SubjectArea.class, Collection.class).newInstance(mode, file, session, examType, subject, exams);
                        report.printReport();
                        report.close();
                        output.put(subject.getSubjectAreaAbbreviation()+"_"+reportName+"."+(mode==sModeText?"txt":"pdf"),file);
                        Hashtable<String,File> files = outputPerSubject.get(subject);
                        if (files==null) {
                            files = new Hashtable(); outputPerSubject.put(subject,files);
                        }
                        files.put(subject.getSubjectAreaAbbreviation()+"_"+reportName+"."+(mode==sModeText?"txt":"pdf"),file);
                        long t1 = System.currentTimeMillis();
                        sLog.info("Report "+file+" generated in "+sDF.format((t1-t0)/1000.0)+"s.");
                        if (report instanceof InstructorExamReport && "true".equals(System.getProperty("email.instructors","false"))) {
                            ireports = ((InstructorExamReport)report).printInstructorReports(
                                    mode, session.getAcademicTerm()+session.getYear()+(examType==Exam.sExamTypeMidterm?"evn":"fin"), new InstructorExamReport.FileGenerator() {
                                        public File generate(String prefix, String ext) {
                                            int idx = 0;
                                            File file = new File(prefix+"."+ext);
                                            while (file.exists()) {
                                                idx++;
                                                file = new File(prefix+"_"+idx+"."+ext);
                                            }
                                            return file;
                                        }
                                    });
                        } else if (report instanceof StudentExamReport && "true".equals(System.getProperty("email.students","false"))) {
                            sreports = ((StudentExamReport)report).printStudentReports(
                                    mode, session.getAcademicTerm()+session.getYear()+(examType==Exam.sExamTypeMidterm?"evn":"fin"), new InstructorExamReport.FileGenerator() {
                                        public File generate(String prefix, String ext) {
                                            int idx = 0;
                                            File file = new File(prefix+"."+ext);
                                            while (file.exists()) {
                                                idx++;
                                                file = new File(prefix+"_"+idx+"."+ext);
                                            }
                                            return file;
                                        }
                                    });
                        }
                    }
                } else {
                    File file = new File(new File(ApplicationProperties.getProperty("output",".")),
                            session.getAcademicTerm()+session.getYear()+(examType==Exam.sExamTypeMidterm?"evn":"fin")+"_"+reportName+(mode==sModeText?".txt":".pdf"));
                    long t0 = System.currentTimeMillis();
                    sLog.info("Generating report "+file+" ...");
                    PdfLegacyExamReport report = (PdfLegacyExamReport)reportClass.getConstructor(int.class, File.class, Session.class, int.class, SubjectArea.class, Collection.class).newInstance(mode, file, session, examType, null, exams);
                    report.printReport();
                    report.close();
                    output.put(reportName+"."+(mode==sModeText?"txt":"pdf"),file);
                    long t1 = System.currentTimeMillis();
                    sLog.info("Report "+file.getName()+" generated in "+sDF.format((t1-t0)/1000.0)+"s.");
                    if (report instanceof InstructorExamReport && "true".equals(System.getProperty("email.instructors","false"))) {
                        ireports = ((InstructorExamReport)report).printInstructorReports(
                                mode, session.getAcademicTerm()+session.getYear()+(examType==Exam.sExamTypeMidterm?"evn":"fin"), new InstructorExamReport.FileGenerator() {
                                    public File generate(String prefix, String ext) {
                                        int idx = 0;
                                        File file = new File(prefix+"."+ext);
                                        while (file.exists()) {
                                            idx++;
                                            file = new File(prefix+"_"+idx+"."+ext);
                                        }
                                        return file;
                                    }
                                });
                    } else if (report instanceof StudentExamReport && "true".equals(System.getProperty("email.students","false"))) {
                        sreports = ((StudentExamReport)report).printStudentReports(
                                mode, session.getAcademicTerm()+session.getYear()+(examType==Exam.sExamTypeMidterm?"evn":"fin"), new InstructorExamReport.FileGenerator() {
                                    public File generate(String prefix, String ext) {
                                        int idx = 0;
                                        File file = new File(prefix+"."+ext);
                                        while (file.exists()) {
                                            idx++;
                                            file = new File(prefix+"_"+idx+"."+ext);
                                        }
                                        return file;
                                    }
                                });
                    }
                }
            }
            if ("true".equals(System.getProperty("email","false"))) {
                sendEmails(session.getAcademicTerm()+session.getYear()+(examType==Exam.sExamTypeMidterm?"evn":"fin"), output, outputPerSubject, ireports, sreports);
            }
            sLog.info("All done.");
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
        }
    }

}
