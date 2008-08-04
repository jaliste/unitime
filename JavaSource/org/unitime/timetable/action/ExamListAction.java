package org.unitime.timetable.action;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.ExamListForm;
import org.unitime.timetable.model.BuildingPref;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.DistributionPref;
import org.unitime.timetable.model.MidtermPeriodPreferenceModel;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.ExamPeriodPref;
import org.unitime.timetable.model.PeriodPreferenceModel;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.RoomFeaturePref;
import org.unitime.timetable.model.RoomGroupPref;
import org.unitime.timetable.model.RoomPref;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Settings;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.SubjectAreaDAO;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.exam.ExamAssignmentProxy;
import org.unitime.timetable.solver.exam.ui.ExamAssignment;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.webutil.BackTracker;
import org.unitime.timetable.webutil.Navigation;
import org.unitime.timetable.webutil.PdfWebTable;
import org.unitime.timetable.webutil.RequiredTimeTable;

public class ExamListAction extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ExamListForm myForm = (ExamListForm) form;
        
        User user = Web.getUser(request.getSession()); 
        TimetableManager manager = (user==null?null:TimetableManager.getManager(user)); 
        Session session = (user==null?null:Session.getCurrentAcadSession(user));
        if (user==null || session==null || !manager.canSeeExams(session, user)) throw new Exception ("Access Denied.");
        myForm.setCanAddExam(manager!=null && manager.canEditExams(session, user));
        
        // Read operation to be performed
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));
        
        if (op==null && request.getSession().getAttribute(Constants.SUBJ_AREA_ID_ATTR_NAME)!=null) {
            myForm.setSubjectAreaId((String)request.getSession().getAttribute(Constants.SUBJ_AREA_ID_ATTR_NAME));
            myForm.setCourseNbr((String)request.getSession().getAttribute(Constants.CRS_NBR_ATTR_NAME));
        }
        if (op==null && request.getSession().getAttribute("Exam.Type")!=null) {
        	myForm.setExamType((Integer)request.getSession().getAttribute("Exam.Type"));
        }
        
        WebTable.setOrder(request.getSession(), "ExamList.ord", request.getParameter("ord"), 1);

        if ("Search".equals(op) || "Export PDF".equals(op)) {
            if (myForm.getSubjectAreaId()!=null) {
                request.getSession().setAttribute(Constants.SUBJ_AREA_ID_ATTR_NAME, myForm.getSubjectAreaId());
                request.getSession().setAttribute(Constants.CRS_NBR_ATTR_NAME, myForm.getCourseNbr());
                request.getSession().setAttribute("Exam.Type", myForm.getExamType());
            }
            
            if ("Export PDF".equals(op)) {
                PdfWebTable table = getExamTable(WebSolver.getExamSolver(request.getSession()), user, manager, session, myForm, false);
                if (table!=null) {
                    File file = ApplicationProperties.getTempFile("exams", "pdf");
                    table.exportPdf(file, WebTable.getOrder(request.getSession(), "ExamList.ord"));
                    request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
                }

            }
        }
        
        if ("Add Examination".equals(op)) {
            return mapping.findForward("addExam");
        }
        
        myForm.setSubjectAreas(TimetableManager.getSubjectAreas(user));
        if (myForm.getSubjectAreas().size()==1) {
            SubjectArea firstSubjectArea = (SubjectArea)myForm.getSubjectAreas().iterator().next();
            myForm.setSubjectAreaId(firstSubjectArea.getUniqueId().toString());
        }
        
        if (myForm.getSubjectAreaId()!=null && myForm.getSubjectAreaId().length()!=0) {
            PdfWebTable table = getExamTable(WebSolver.getExamSolver(request.getSession()), user, manager, session, myForm, true);
            if (table!=null) {
                request.setAttribute("ExamList.table", table.printTable(WebTable.getOrder(request.getSession(), "ExamList.ord")));
                Vector ids = new Vector();
                for (Enumeration e=table.getLines().elements();e.hasMoreElements();) {
                    WebTable.WebTableLine line = (WebTable.WebTableLine)e.nextElement();
                    ids.add(Long.parseLong(line.getUniqueId()));
                }
                Navigation.set(request.getSession(), Navigation.sInstructionalOfferingLevel, ids);
            } else {
                ActionMessages errors = new ActionMessages();
                errors.add("exams", new ActionMessage("errors.generic", "No examination matching the above criteria was found."));
                saveErrors(request, errors);
            }
        }
        
        String subjectAreaName = "";
        try {
            subjectAreaName = new SubjectAreaDAO().get(new Long(myForm.getSubjectAreaId())).getSubjectAreaAbbreviation();
        } catch (Exception e) {}
        
        if (request.getParameter("backId")!=null)
            request.setAttribute("hash", request.getParameter("backId"));
        
        BackTracker.markForBack(
                request, 
                "examList.do?op=Search&examType="+myForm.getExamType()+"&subjectAreaId="+myForm.getSubjectAreaId()+"&courseNbr="+myForm.getCourseNbr(),
                Exam.sExamTypes[myForm.getExamType()]+" Exams ("+(Constants.ALL_OPTION_VALUE.equals(myForm.getSubjectAreaId())?"All":subjectAreaName+
                    (myForm.getCourseNbr()==null || myForm.getCourseNbr().length()==0?"":" "+myForm.getCourseNbr()))+
                    ")", 
                true, true);

        return mapping.findForward("list");
    }
    
    public PdfWebTable getExamTable(ExamAssignmentProxy examAssignment, User user, TimetableManager manager, Session session, ExamListForm form, boolean html) {
        Collection exams = (form.getSubjectAreaId()==null || form.getSubjectAreaId().trim().length()==0 || "null".equals(form.getSubjectAreaId())?null:Constants.ALL_OPTION_VALUE.equals(form.getSubjectAreaId())?Exam.findAll(session.getUniqueId(),form.getExamType()):Exam.findExamsOfCourse(Long.valueOf(form.getSubjectAreaId()), form.getCourseNbr(),form.getExamType()));
        
        if (exams==null || exams.isEmpty()) return null;
        
        if (examAssignment!=null && examAssignment.getExamType()!=form.getExamType()) examAssignment = null;
        
        String nl = (html?"<br>":"\n");
        
        boolean timeVertical = RequiredTimeTable.getTimeGridVertical(user);
        boolean timeText = RequiredTimeTable.getTimeGridAsText(user);
        String instructorNameFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
        
        PdfWebTable table = new PdfWebTable(
                11,
                Exam.sExamTypes[form.getExamType()]+" Examinations", "examList.do?ord=%%",
                new String[] {"Classes / Courses", "Length", "Seating"+nl+"Type", "Size", "Max"+nl+"Rooms", 
                        "Instructor", "Period"+nl+"Preferences", "Room"+nl+"Preferences", "Distribution"+nl+"Preferences",
                        "Assigned"+nl+"Period", "Assigned"+nl+"Room"},
                new String[] {"left", "right", "center", "right", "right", "left", 
                        "left", "left", "left", "left", "left"},
                new boolean[] {true, true, true, true, true, true, true, true, true, true}
                );
        
        
        for (Iterator i=exams.iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            String objects = "", perPref = "", roomPref = "", distPref = "", per = "", rooms = "";
            
            for (Iterator j=new TreeSet(exam.getOwners()).iterator();j.hasNext();) {
                ExamOwner owner = (ExamOwner)j.next();
                if (objects.length()>0) objects+=nl;
                objects += owner.getLabel();
            }
            
            ExamAssignment ea = (examAssignment!=null?examAssignment.getAssignment(exam.getUniqueId()):exam.getAssignedPeriod()!=null?new ExamAssignment(exam):null);
            if (ea!=null) {
                per = (html?ea.getPeriodAbbreviationWithPref():ea.getPeriodAbbreviation());
                rooms = (html?ea.getRoomsNameWithPref(nl):ea.getRoomsName(nl));
            }
            
            if (html) {
                roomPref += exam.getEffectivePrefHtmlForPrefType(RoomPref.class);
                if (roomPref.length()>0 && !roomPref.endsWith(nl)) roomPref+=nl;
                roomPref += exam.getEffectivePrefHtmlForPrefType(BuildingPref.class);
                if (roomPref.length()>0 && !roomPref.endsWith(nl)) roomPref+=nl;
                roomPref += exam.getEffectivePrefHtmlForPrefType(RoomFeaturePref.class);
                if (roomPref.length()>0 && !roomPref.endsWith(nl)) roomPref+=nl;
                roomPref += exam.getEffectivePrefHtmlForPrefType(RoomGroupPref.class);
                if (roomPref.endsWith(nl)) roomPref = roomPref.substring(0, roomPref.length()-nl.length());
                if (timeText || Exam.sExamTypeMidterm==exam.getExamType()) {
                	if (Exam.sExamTypeMidterm==exam.getExamType()) {
                    	MidtermPeriodPreferenceModel epx = new MidtermPeriodPreferenceModel(exam.getSession(), null);
                    	epx.load(exam);
                    	perPref+=epx.toString(true);
                	} else {
                		perPref += exam.getEffectivePrefHtmlForPrefType(ExamPeriodPref.class);
                	}
                } else {
                    PeriodPreferenceModel px = new PeriodPreferenceModel(exam.getSession(), ea, exam.getExamType());
                    px.load(exam);
                    RequiredTimeTable rtt = new RequiredTimeTable(px);
                    File imageFileName = null;
                    try {
                        imageFileName = rtt.createImage(timeVertical);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    String title = rtt.getModel().toString();
                    if (imageFileName!=null)
                        perPref = "<img border='0' src='temp/"+(imageFileName.getName())+"' title='"+title+"'>";
                    else
                        perPref += exam.getEffectivePrefHtmlForPrefType(ExamPeriodPref.class);
                }
                distPref += exam.getEffectivePrefHtmlForPrefType(DistributionPref.class);
            } else {
                for (Iterator j=exam.effectivePreferences(RoomPref.class).iterator();j.hasNext();) {
                    Preference pref = (Preference)j.next();
                    if (roomPref.length()>0) roomPref+=nl;
                    roomPref += PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText();
                }
                for (Iterator j=exam.effectivePreferences(BuildingPref.class).iterator();j.hasNext();) {
                    Preference pref = (Preference)j.next();
                    if (roomPref.length()>0) roomPref+=nl;
                    roomPref += PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText();
                }
                for (Iterator j=exam.effectivePreferences(RoomFeaturePref.class).iterator();j.hasNext();) {
                    Preference pref = (Preference)j.next();
                    if (roomPref.length()>0) roomPref+=nl;
                    roomPref += PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText();
                }
                for (Iterator j=exam.effectivePreferences(RoomGroupPref.class).iterator();j.hasNext();) {
                    Preference pref = (Preference)j.next();
                    if (roomPref.length()>0) roomPref+=nl;
                    roomPref += PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText();
                }
                boolean prefPrinted = false;
                if (Exam.sExamTypeMidterm==exam.getExamType()) {
                    MidtermPeriodPreferenceModel epx = new MidtermPeriodPreferenceModel(exam.getSession(), null);
                    epx.load(exam);
                    perPref+=epx.toString(false);
                } else {
                    for (Iterator j=exam.effectivePreferences(ExamPeriodPref.class).iterator();j.hasNext();) {
                        Preference pref = (Preference)j.next();
                        if (perPref.length()>0) perPref+=nl;
                        perPref += PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText();
                    }
                }
                for (Iterator j=exam.effectivePreferences(DistributionPref.class).iterator();j.hasNext();) {
                    DistributionPref pref = (DistributionPref)j.next();
                    if (distPref.length()>0) distPref+=nl;
                    distPref += PreferenceLevel.prolog2abbv(pref.getPrefLevel().getPrefProlog())+" "+pref.preferenceText(true, true, " (", ", ",")").replaceAll("&lt;","<").replaceAll("&gt;",">");
                }
            }
            
            int nrStudents = exam.getSize();
            String instructors = "";
            for (Iterator j=new TreeSet(exam.getInstructors()).iterator();j.hasNext();) {
                DepartmentalInstructor instructor = (DepartmentalInstructor)j.next();
                if (instructors.length()>0) instructors+=nl;
                instructors+=instructor.getName(instructorNameFormat);
            }
            
            table.addLine(
                    "onClick=\"document.location='examDetail.do?examId="+exam.getUniqueId()+"';\"",
                    new String[] {
                        (html?"<a name='"+exam.getUniqueId()+"'>":"")+objects+(html?"</a>":""),
                        exam.getLength().toString(),
                        (Exam.sSeatingTypeNormal==exam.getSeatingType()?"Normal":"Exam"),
                        String.valueOf(nrStudents),
                        exam.getMaxNbrRooms().toString(),
                        instructors,
                        perPref,
                        roomPref,
                        distPref,
                        per,
                        rooms
                    },
                    new Comparable[] {
                        exam.firstOwner(),
                        exam.getLength(),
                        exam.getSeatingType(),
                        nrStudents,
                        exam.getMaxNbrRooms(),
                        instructors,
                        perPref,
                        roomPref,
                        distPref,
                        (exam.getAssignedPeriod()==null?new Date(0):exam.getAssignedPeriod().getStartTime()),
                        rooms
                    },
                    exam.getUniqueId().toString());
        }
        
        return table;
                
    }
}
