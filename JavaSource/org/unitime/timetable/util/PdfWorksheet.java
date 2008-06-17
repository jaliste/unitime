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
package org.unitime.timetable.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.AcadAreaReservation;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.BuildingPref;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.CourseOfferingReservation;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.ExactTimeMins;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.ItypeDesc;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.RoomFeaturePref;
import org.unitime.timetable.model.RoomGroupPref;
import org.unitime.timetable.model.RoomPref;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimePatternModel;
import org.unitime.timetable.model.TimePref;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.comparators.InstrOfferingConfigComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.solver.TimetableDatabaseLoader;
import org.unitime.timetable.util.Constants;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;

/**
 * @author Tomas Muller
 */
public class PdfWorksheet {
    private boolean iUseCommitedAssignments = true;
    private static int sNrChars = 133;
    private static int sNrLines = 50;
    private File iFolder = null;
    private File iFile = null;
    private FileOutputStream iOut = null;
    private Document iDoc = null;
    private PdfWriter iWriter = null;
    private SubjectArea iSubjectArea = null;
    private String iCourseNumber = null;
    private int iPageNo = 0;
    private int iLineNo = 0;
    private StringBuffer iBuffer = new StringBuffer();
    private CourseOffering iCourseOffering = null;
    
    private PdfWorksheet(File file, SubjectArea sa, String courseNumber) throws IOException, DocumentException  {
        iUseCommitedAssignments = "true".equals(ApplicationProperties.getProperty("tmtbl.pdf.worksheet.useCommitedAssignments","true"));
        iSubjectArea = sa;
        iCourseNumber = courseNumber;
        iFile = file;
        if (iCourseNumber!=null && (iCourseNumber.trim().length()==0 || "*".equals(iCourseNumber.trim().length())))
            iCourseNumber = null;
        iDoc = new Document(PageSize.LETTER.rotate());

        iOut = new FileOutputStream(file);
        iWriter = PdfWriter.getInstance(iDoc, iOut);

        iDoc.addTitle(sa.getSubjectAreaAbbreviation()+(iCourseNumber==null?"":" "+iCourseNumber)+" Worksheet");
        iDoc.addAuthor(ApplicationProperties.getProperty("tmtbl.pdf.worksheet.author","UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER+", www.unitime.org"));
        iDoc.addSubject(sa.getSubjectAreaAbbreviation()+" -- "+sa.getSession());
        iDoc.addCreator("UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER+", www.unitime.org");

        iDoc.open();
        
        printHeader();
    }
    
    public static boolean print(File file, SubjectArea sa) throws IOException, DocumentException {
        TreeSet courses = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                CourseOffering co1 = (CourseOffering)o1;
                CourseOffering co2 = (CourseOffering)o2;
                int cmp = co1.getCourseNbr().compareTo(co2.getCourseNbr());
                if (cmp!=0) return cmp;
                return co1.getUniqueId().compareTo(co2.getUniqueId());
            }
        });
        courses.addAll(new SessionDAO().getSession().
                createQuery("select co from CourseOffering co where  co.subjectArea.uniqueId=:subjectAreaId").
                setLong("subjectAreaId", sa.getUniqueId()).list());
        if (courses.isEmpty()) return false;
        PdfWorksheet w = new PdfWorksheet(file,sa,null);
        for (Iterator i=courses.iterator();i.hasNext();) {
            w.print((CourseOffering)i.next());
        }
        w.lastPage();
        w.close();
        return true;
    }
    
    public static boolean print(File file, SubjectArea sa, String courseNumber) throws IOException, DocumentException {
        TreeSet courses = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                CourseOffering co1 = (CourseOffering)o1;
                CourseOffering co2 = (CourseOffering)o2;
                int cmp = co1.getCourseNbr().compareTo(co2.getCourseNbr());
                if (cmp!=0) return cmp;
                return co1.getUniqueId().compareTo(co2.getUniqueId());
            }
        });
        String query = "select co from CourseOffering co where  co.subjectArea.uniqueId=:subjectAreaId";
        if (courseNumber!=null && courseNumber.trim().length()>0) {
            query += " and co.courseNbr ";
            if (courseNumber.indexOf('*')>=0)
                query += " like '"+courseNumber.trim().replace('*', '%').toUpperCase()+"'";
            else 
                query += " = '"+courseNumber.trim().toUpperCase()+"'";
        }
        courses.addAll(new SessionDAO().getSession().createQuery(query).setLong("subjectAreaId", sa.getUniqueId()).list());
        if (courses.isEmpty()) return false;
        PdfWorksheet w = new PdfWorksheet(file,sa,courseNumber);
        for (Iterator i=courses.iterator();i.hasNext();) {
            w.print((CourseOffering)i.next());
        }
        w.lastPage();
        w.close();
        return true;
    }
    
    private String[] time(Class_ clazz) {
        String dpat = "";
        DatePattern dp = clazz.effectiveDatePattern();
        if (dp!=null && !dp.isDefault()) {
            if (dp.getType().intValue()==DatePattern.sTypeAlternate)
                dpat = " "+dp.getName();
            else {
                SimpleDateFormat dpf = new SimpleDateFormat("MM/dd");
                dpat = ", "+dpf.format(dp.getStartDate())+" - "+dpf.format(dp.getEndDate());
            }
        }
        Assignment assgn = (iUseCommitedAssignments?clazz.getCommittedAssignment():null);
        if (assgn==null) {
            Set timePrefs = clazz.getEffectiveTimePreferences();
            if (timePrefs.isEmpty()) {
                if (clazz.getSchedulingSubpart().getMinutesPerWk().intValue()>0)
                    return new String[]{"Arr "+((clazz.getSchedulingSubpart().getMinutesPerWk().intValue()+59)/60)+" Hrs"+dpat};
                else
                    return new String[]{"Arr Hrs"+dpat};
            }
            boolean onlyOneReq = true;
            TimeLocation req = null;
            for (Iterator x=timePrefs.iterator();onlyOneReq && x.hasNext();) {
                TimePref tp = (TimePref)x.next();
                TimePatternModel model = tp.getTimePatternModel();
                if (model.isExactTime()) {
                    if (req!=null) onlyOneReq=false;
                    else {
                        int length = ExactTimeMins.getNrSlotsPerMtg(model.getExactDays(),clazz.getSchedulingSubpart().getMinutesPerWk().intValue());
                        int breakTime = ExactTimeMins.getBreakTime(model.getExactDays(),clazz.getSchedulingSubpart().getMinutesPerWk().intValue()); 
                        req = new TimeLocation(model.getExactDays(), model.getExactStartSlot(), length,PreferenceLevel.sIntLevelNeutral,0,dp.getUniqueId(),dp.getName(),dp.getPatternBitSet(),breakTime);
                    }
                } else {
                    for (int d=0;d<model.getNrDays();d++)
                        for (int t=0;onlyOneReq && t<model.getNrTimes();t++) {
                            if (PreferenceLevel.sRequired.equals(model.getPreference(d,t))) {
                                if (req!=null) onlyOneReq=false;
                                else {
                                    req = new TimeLocation(
                                            model.getDayCode(d),
                                            model.getStartSlot(t),
                                            model.getSlotsPerMtg(),
                                            PreferenceLevel.prolog2int(model.getPreference(d, t)),
                                            0,
                                            dp.getUniqueId(),
                                            dp.getName(),
                                            dp.getPatternBitSet(),
                                            model.getBreakTime());                                                
                                }
                            }
                        }
                }
            }
            if (onlyOneReq && req!=null)
                return new String[] {req.getDayHeader()+" "+req.getStartTimeHeader()+" - "+req.getEndTimeHeader()+dpat};
            Vector t = new Vector();
            for (Iterator x=timePrefs.iterator();x.hasNext();) {
                TimePref tp = (TimePref)x.next();
                String tx = tp.getTimePatternModel().toString();
                for (StringTokenizer s=new StringTokenizer(tx,",");s.hasMoreTokens();)
                    t.add(s.nextToken().trim());
            }
            String[] time = new String[t.size()];
            for (int x=0;x<time.length;x++)
                time[x]=t.elementAt(x)+dpat;
            return time;
        }
        TimeLocation t = assgn.getTimeLocation();
        return new String[] {t.getDayHeader()+" "+t.getStartTimeHeader()+" - "+t.getEndTimeHeader()+dpat};
    }
    
    private String[] room(Class_ clazz) {
        Assignment assgn = (iUseCommitedAssignments?clazz.getCommittedAssignment():null);
        if (assgn==null || assgn.getRoomLocations().isEmpty()) {
            Vector roomLocations = TimetableDatabaseLoader.computeRoomLocations(clazz);
            if (roomLocations.size()==clazz.getNbrRooms().intValue()) {
                String[] rooms = new String[roomLocations.size()];
                for (int x=0;x<roomLocations.size();x++) {
                    RoomLocation r = (RoomLocation)roomLocations.elementAt(x); 
                    rooms[x] = r.getName();
                }
                return rooms;
            }
            Vector roomPrefs = new Vector();
            boolean allRoomReq = true;
            for (Iterator i=clazz.effectivePreferences(BuildingPref.class).iterator();i.hasNext();) {
                Preference pref = (Preference)i.next();
                roomPrefs.add(PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText());
                allRoomReq=false;
            }
            for (Iterator i=clazz.effectivePreferences(RoomPref.class).iterator();i.hasNext();) {
                Preference pref = (Preference)i.next();
                roomPrefs.add(PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText());
                if (!PreferenceLevel.sRequired.equals(pref.getPrefLevel().getPrefProlog())) allRoomReq=false;
            }
            for (Iterator i=clazz.effectivePreferences(RoomFeaturePref.class).iterator();i.hasNext();) {
                Preference pref = (Preference)i.next();
                roomPrefs.add(PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText());
                allRoomReq=false;
            }
            for (Iterator i=clazz.effectivePreferences(RoomGroupPref.class).iterator();i.hasNext();) {
                Preference pref = (Preference)i.next();
                roomPrefs.add(PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText());
                allRoomReq=false;
            }
            if (allRoomReq) {
                roomPrefs.clear();
                for (Iterator i=clazz.effectivePreferences(RoomPref.class).iterator();i.hasNext();) {
                    Preference pref = (Preference)i.next();
                    roomPrefs.add(pref.preferenceText());
                }
            }
            String[] rooms = new String[roomPrefs.size()];
            for (int x=0;x<roomPrefs.size();x++) {
                rooms[x] = roomPrefs.elementAt(x).toString();
            }
            return rooms;
        }
        String[] rooms = new String[assgn.getRoomLocations().size()];
        for (int x=0;x<assgn.getRoomLocations().size();x++) {
            RoomLocation r = (RoomLocation)assgn.getRoomLocations().elementAt(x); 
            rooms[x] = r.getName();
        }
        return rooms;
    }
    
    private String[] instructor(Class_ clazz) {
        Vector leads = clazz.getLeadInstructors();
        String[] instr = new String[leads.size()];
        for (int x=0;x<clazz.getLeadInstructors().size();x++) {
            DepartmentalInstructor in = (DepartmentalInstructor)leads.elementAt(x); 
            instr[x] = in.nameShort();
        }
        return instr;
    }
    
    protected void print(CourseOffering co) throws DocumentException {
        //System.out.println("  Printing "+co.getCourseName()+" ...");
        if (iLineNo+5>=sNrLines) newPage();
        iCourseOffering = co;
        String session = lpad(co.getSubjectArea().getSession().getAcademicTerm()+" "+co.getSubjectArea().getSession().getAcademicYear(),17);
        int courseLimit = -1;
        InstructionalOffering offering = co.getInstructionalOffering();
        for (Iterator i=offering.getCourseReservations().iterator();i.hasNext();) {
            CourseOfferingReservation r = (CourseOfferingReservation)i.next();
            if (r.getCourseOffering().equals(co))
                courseLimit = r.getReserved().intValue();
        }
        if (courseLimit<0) {
            if (offering.getCourseOfferings().size()==1 && offering.getLimit()!=null)
                courseLimit = offering.getLimit().intValue();
        }
        boolean unlimited = false;
        String courseOrg = "";
        for (Iterator i=offering.getInstrOfferingConfigs().iterator();i.hasNext();) {
            InstrOfferingConfig config = (InstrOfferingConfig)i.next();
            if (config.isUnlimitedEnrollment().booleanValue()) unlimited=true;
            Hashtable creditPerIType = new Hashtable();
            for (Iterator j=config.getSchedulingSubparts().iterator();j.hasNext();) {
                SchedulingSubpart subpart = (SchedulingSubpart)j.next();
                if (subpart.getMinutesPerWk().intValue()<=0) continue;
                Integer credit = (Integer)creditPerIType.get(subpart.getItype());
                creditPerIType.put(subpart.getItype(), new Integer((credit==null?0:credit.intValue())+subpart.getMinutesPerWk().intValue()));
            }
            TreeSet itypes = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    ItypeDesc i1 = (ItypeDesc)o1;
                    ItypeDesc i2 = (ItypeDesc)o2;
                    return i1.getItype().compareTo(i2.getItype());
                }
            });
            itypes.addAll(creditPerIType.keySet());
            for (Iterator j=itypes.iterator();j.hasNext();) {
                ItypeDesc itype = (ItypeDesc)j.next();
                int minPerWeek = ((Integer)creditPerIType.get(itype)).intValue();
                if (courseOrg.length()>0) courseOrg+=", ";
                courseOrg+=itype.getAbbv().trim()+" "+((minPerWeek+49)/50);
            }
            break;
        }
        int enrl = -1;
        String s1 = co.getSubjectArea().getSession().getAcademicTerm().substring(0,1) + co.getSubjectArea().getSession().getAcademicYear().substring(2);
        String s2 = co.getSubjectArea().getSession().getAcademicTerm().substring(0,1) + 
            new DecimalFormat("00").format(Integer.parseInt(co.getSubjectArea().getSession().getAcademicYear().substring(2))-1);
        if (co.getProjectedDemand()!=null) enrl = co.getProjectedDemand().intValue();
        int lastLikeEnrl = co.getCourseOfferingDemands().size();
        String title = co.getTitle();
        if (title==null) title="*** Title not set";
        println("                                                                                              Proj  "+s2+"   Desig                   ");
        println("Course     Title/Notes                           Credit Course Organization             Limit Enrl  Enrl  Reqd  Consent    Cross List");
        println("---------- ------------------------------------- ------ ------------------------------- ----- ----- ----- ----- ---------- ----------");
        println(rpad(co.getCourseName(),10)+" "+
                rpad(title,37)+(title.length()>37?"-":" ")+" "+
                rpad(offering.getCredit()==null?"":offering.getCredit().creditAbbv(),5)+" "+
                rpad(courseOrg,31)+" "+
                lpad(courseLimit<=0?unlimited?"  inf":"":String.valueOf(courseLimit),5)+" "+
                lpad(enrl<=0?"":String.valueOf(enrl),5)+" "+
                lpad(lastLikeEnrl<=0?"":String.valueOf(lastLikeEnrl),5)+" "+
                rpad(offering.isDesignatorRequired()==null?"":offering.isDesignatorRequired().booleanValue()?"yes":"no",5)+" "+
                rpad(offering.getConsentType()==null?"":offering.getConsentType().getAbbv(),10)+" "+
                rpad(offering.getCourseOfferings().size()>1?offering.getCourseName():"",10)
                );
        while (title.length()>37) {
            title = title.substring(37);
            println("           "+rpad(title,37)+(title.length()>37?"-":" "));
        }
        if (co.getScheduleBookNote()!=null && co.getScheduleBookNote().trim().length()>0) {
            String note = co.getScheduleBookNote();
            note = note.replaceAll("\\. ", "\\.\n");
            for (StringTokenizer s=new StringTokenizer(note,"\n\r");s.hasMoreTokens();) {
                String line = s.nextToken().trim();
                while (line.length()>sNrChars-7) {
                    println("   "+line.substring(0,sNrChars-7)+"-");
                    line = line.substring(sNrChars-7);
                }
                println("   "+line);
            }
        }
        if (iLineNo+5>=sNrLines) newPage();
        else println("");
        println("        "+s1+"   "+s2+"  Proj | Type");
        println("Curr  Reqst  Enrl  Enrl | Instr Number Time                                     Limit Bldg-Room          Instructor            Mgr");
        println("----  -----  ----  ---- | ----- ------ ---------------------------------------- ----- ------------------ --------------------- ------");

        Vector rTable = new Vector();
        Vector cTable = new Vector();
        int a=0,b=0,c=0;
        for (Iterator i=co.getAcadAreaReservations().iterator();i.hasNext();) {
            AcadAreaReservation ar = (AcadAreaReservation)i.next();
            rTable.add(
                    lpad(ar.getAcademicArea().getAcademicAreaAbbreviation(),4)+"  "+
                    lpad(ar.getRequested()==null?"":ar.getRequested().toString(),5)+" "+
                    lpad(ar.getPriorEnrollment()==null?"":ar.getPriorEnrollment().toString(),5)+" "+
                    lpad(ar.getProjectedEnrollment()==null?"":ar.getProjectedEnrollment().toString(),5));
            if (ar.getRequested()!=null) a+=ar.getRequested().intValue();
            if (ar.getPriorEnrollment()!=null) b+=ar.getPriorEnrollment().intValue();
            if (ar.getProjectedEnrollment()!=null) c+=ar.getProjectedEnrollment().intValue();
        }
        if (rTable.isEmpty()) {
            rTable.add(" *** No Request Data   ");
        } else {
            rTable.add(
                    " Tot  "+
                    lpad(String.valueOf(a),5)+" "+
                    lpad(String.valueOf(b),5)+" "+
                    lpad(String.valueOf(c),5));
            rTable.add("                       ");
            rTable.add(" *Please check requests");
        }
        if (offering.isNotOffered().booleanValue())
            cTable.add(" ** Course not offered");
        Vector gTable = new Vector();
        TreeSet configs = new TreeSet(new InstrOfferingConfigComparator(null));
        configs.addAll(offering.getInstrOfferingConfigs());
        for (Iterator i=configs.iterator();i.hasNext();) {
            InstrOfferingConfig config = (InstrOfferingConfig)i.next();
            if (offering.getInstrOfferingConfigs().size()>1)
                cTable.add("** Configuration "+config.getName());
            TreeSet subparts = new TreeSet(new SchedulingSubpartComparator());
            subparts.addAll(config.getSchedulingSubparts());
            for (Iterator j=subparts.iterator();j.hasNext();) {
                SchedulingSubpart subpart = (SchedulingSubpart)j.next();
                TreeSet classes = new TreeSet(new ClassComparator(ClassComparator.COMPARE_BY_HIERARCHY));
                classes.addAll(subpart.getClasses());
                String subpartLabel = subpart.getItype().getAbbv();
                boolean same = false;
                for (Iterator k=classes.iterator();k.hasNext();) {
                    Class_ clazz = (Class_)k.next();
                    Assignment assgn = (Assignment)clazz.getCommittedAssignment();
                    String[] time = time(clazz);
                    String[] rooms = room(clazz);
                    String[] instr = instructor(clazz);
                    for (int x=0;x<Math.max(Math.max(1,time.length),Math.max(instr.length,rooms.length));x++) {
                        cTable.add(
                                rpad(same?"":x==0?subpartLabel:"",5)+" "+
                                lpad(x==0?clazz.getSectionNumberString():"",6)+" "+
                                rpad(time!=null && x<time.length?time[x]:"",40)+" "+
                                lpad(x==0 && clazz.getClassLimit()>0 && clazz.getNbrRooms().intValue()>0?(clazz.getNbrRooms().intValue()>1?clazz.getNbrRooms()+"x":"")+String.valueOf(clazz.getClassLimit()):"",5)+" "+
                                rpad(rooms!=null && x<rooms.length?rooms[x]:"",18)+" "+
                                rpad(instr!=null && x<instr.length?instr[x]:"",21)+" "+
                                rpad(x==0?clazz.getManagingDept().getShortLabel():"",6)
                                );
                    }
                    same=true;
                    if (clazz.getParentClass()!=null && clazz.getChildClasses().isEmpty()) {
                        String gr = clazz.getSchedulingSubpart().getItype().getAbbv().trim()+
                                    lpad(clazz.getSectionNumberString(),4);
                        Class_ parent = clazz.getParentClass();
                        while (parent!=null) {
                            gr = parent.getSchedulingSubpart().getItype().getAbbv().trim()+
                                lpad(parent.getSectionNumberString(),4)+
                                ", "+gr;
                            parent = parent.getParentClass();
                        }
                        gTable.add(gr);
                    }
                }
            }
        }
        for (int i=0;i<1+Math.max(rTable.size(), cTable.size());i++) {
            String res = null;
            String cl = null;
            if (i<rTable.size()) res = (String)rTable.elementAt(i);
            if (i<cTable.size()) cl = (String)cTable.elementAt(i);
            println(rpad(res,23)+" | "+(cl==null?"":cl));
        }
        if (!gTable.isEmpty()) {
            println(rep('-',sNrChars));
            println("     Course groups:");
            int half = (gTable.size()+1)/2;
            for (int i=0;i<half;i++) {
                String gr1 = (String)gTable.elementAt(i);
                String gr2 = (half+i<gTable.size()?(String)gTable.elementAt(half+i):"");
                println("     "+rpad(gr1,60)+" | "+rpad(gr2,60));
            }
        }
        println(rep('=',sNrChars));
        iCourseOffering = null;
    }
    
    private void out(String text) throws DocumentException {
        if (iBuffer.length()>0) iBuffer.append("\n");
        iBuffer.append(text);
    }
    
    private static String rep(char ch, int cnt) {
        String ret = "";
        for (int i=0;i<cnt;i++) ret+=ch;
        return ret;
    }
    
    private void outln(char ch) throws DocumentException {
        out(rep(ch,sNrChars));
    }
    
    private String lpad(String s, char ch, int len) {
        while (s.length()<len) s = ch + s;
        return s;
    }
    
    private String lpad(String s, int len) {
        if (s==null) s="";
        if (s.length()>len) return s.substring(0,len);
        return lpad(s,' ',len);
    }

    private String rpad(String s, char ch, int len) {
        while (s.length()<len) s = s + ch;
        return s;
    }
    
    private String rpad(String s, int len) {
        if (s==null) s="";
        if (s.length()>len) return s.substring(0,len);
        return rpad(s,' ',len);
    }
    
    private String mpad(String s, char ch, int len) {
        if (s==null) s="";
        if (s.length()>len) return s.substring(0,len);
        while (s.length()<len) 
            if (s.length()%2==0) s = s + ch; else s = ch + s;
        return s;
    }

    private String mpad(String s, int len) {
        return mpad(s,' ',len);
    }
    
    private String mpad(String s1, String s2, char ch, int len) {
        String m = "";
        while ((s1+m+s2).length()<len) m += ch;
        return s1+m+s2;
    }
    
    private String render(String line, String s, int idx) {
        String a = (line.length()<=idx?rpad(line,' ',idx):line.substring(0,idx));
        String b = (line.length()<=idx+s.length()?"":line.substring(idx+s.length()));
        return a + s + b;
    }

    private String renderMiddle(String line, String s) {
        return render(line, s, (sNrChars - s.length())/2);
    }

    private String renderEnd(String line, String s) {
        return render(line, s, sNrChars-s.length());
    }
    
    protected void printHeader() throws DocumentException {
        out(renderMiddle(
                ApplicationProperties.getProperty("tmtbl.pdf.worksheet.author","UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER),
                ApplicationProperties.getProperty("tmtbl.pdf.worksheet.title","PDF WORKSHEET")
                ));
        out(mpad(
                new SimpleDateFormat("EEE MMM dd, yyyy").format(new Date()),
                iSubjectArea.getSession().getAcademicInitiative()+" "+
                iSubjectArea.getSession().getAcademicTerm()+" "+
                iSubjectArea.getSession().getAcademicYear(),' ',sNrChars));
        outln('=');
        iLineNo=0;
        if (iCourseOffering!=null)
            println("("+iCourseOffering.getCourseName()+" Continued)");
    }
    
    protected void printFooter() throws DocumentException {
        out("");
        out(renderEnd(renderMiddle("","Page "+(iPageNo+1)),"<"+iSubjectArea.getSubjectAreaAbbreviation()+(iCourseNumber!=null?" "+iCourseNumber:"")+">  "));
        Paragraph p = new Paragraph(iBuffer.toString(), FontFactory.getFont(FontFactory.COURIER, 9));
        p.setLeading(9.5f); //was 13.5f
        iDoc.add(p);
        iBuffer = new StringBuffer();
        iPageNo++;
    }
    protected void lastPage() throws DocumentException {
        while (iLineNo<sNrLines) {
            out(""); iLineNo++;
        }
        printFooter();
    }
    
    protected void newPage() throws DocumentException {
        while (iLineNo<sNrLines) {
            out(""); iLineNo++;
        }
        printFooter();
        iDoc.newPage();
        printHeader();
    }
    
    protected void println(String text) throws DocumentException {
        out(text);
        iLineNo++;
        if (iLineNo>=sNrLines) newPage();
    }
    
    private void close() throws IOException {
        iDoc.close();
        iOut.close();
    }

    public static void main(String[] args) {
        try {
            HibernateUtil.configureHibernate(ApplicationProperties.getProperties());
            
            Long sessionId = Long.valueOf(ApplicationProperties.getProperty("tmtbl.pdf.worksheet.session", "165924"));
            Session session = new SessionDAO().get(sessionId);
            if (session==null) {
                System.err.println("Academic session "+sessionId+" not found, use property tmtbl.pdf.worksheet.session to set academic session.");
                System.exit(0);
            } else {
                System.out.println("Session: "+session);
            }
            TreeSet subjectAreas = null;
            if (args.length>0) {
                subjectAreas = new TreeSet();
                for (int i=0;i<args.length;i++) {
                    SubjectArea sa = SubjectArea.findByAbbv(sessionId, args[i]);
                    if (sa==null)
                        System.err.println("Subject area "+args[i]+" not found.");
                    else
                        subjectAreas.add(sa);
                }
            } else {
                subjectAreas = new TreeSet(SubjectArea.getSubjectAreaList(sessionId));
            }
            
            for (Iterator i=subjectAreas.iterator();i.hasNext();) {
                SubjectArea sa = (SubjectArea)i.next();
                System.out.println("Printing subject area "+sa.getSubjectAreaAbbreviation()+" ...");
                PdfWorksheet.print(new File(sa.getSubjectAreaAbbreviation()+".pdf"),sa);
            }
            
            HibernateUtil.closeHibernate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
