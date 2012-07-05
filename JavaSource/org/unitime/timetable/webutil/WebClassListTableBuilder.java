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
package org.unitime.timetable.webutil;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import javax.servlet.jsp.JspWriter;

import org.unitime.commons.web.htmlgen.TableCell;
import org.unitime.commons.web.htmlgen.TableStream;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.defaults.CommonValues;
import org.unitime.timetable.defaults.UserProperty;
import org.unitime.timetable.form.ClassListForm;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.PreferenceGroup;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.comparators.ClassCourseComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.SchedulingSubpartDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.solver.CachedClassAssignmentProxy;
import org.unitime.timetable.solver.ClassAssignmentProxy;
import org.unitime.timetable.solver.exam.ExamAssignmentProxy;


/**
 * @author Stephanie Schluttenhofer
 *
 */
public class WebClassListTableBuilder extends
		WebInstructionalOfferingTableBuilder {
	protected static CourseMessages MSG = Localization.create(CourseMessages.class);
    public static String STUDENT_SCHEDULE_NOTE = MSG.columnStudentScheduleNote();
    protected String getSchedulePrintNoteLabel(){
    	return STUDENT_SCHEDULE_NOTE;
    }

	/**
	 * 
	 */
	public WebClassListTableBuilder() {
		super();
	}
	
	protected String additionalNote(){
		return(new String());
	}
	
	protected String labelForTable(SubjectArea subjectArea){
		StringBuffer sb = new StringBuffer();
		sb.append("<p style=\"page-break-before: always\" class=\"WelcomeRowHead\"><b><font size=\"+1\">");
		sb.append(subjectArea.getSubjectAreaAbbreviation());
		sb.append(" - ");
		sb.append(subjectArea.getSession().getLabel());
		sb.append(additionalNote());
		sb.append("</font></b></p>");
		return(sb.toString());		
	}
	
	
	public void htmlTableForClasses(SessionContext context, ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, ClassListForm form, JspWriter outputStream, String backType, String backId){
        
        this.setVisibleColumns(form);
        setBackType(backType);
        setBackId(backId);
        
        TreeSet classes = (TreeSet) form.getClasses();
    	Navigation.set(context, Navigation.sClassLevel, classes);
        
    	if (isShowTimetable()) {
    		boolean hasTimetable = false;
    		if (context.hasPermission(null, "Department", Right.ClassAssignments) && classAssignment != null) {
    			try {
                	if (classAssignment instanceof CachedClassAssignmentProxy) {
                		((CachedClassAssignmentProxy)classAssignment).setCache(classes);
                	}
    				for (Iterator i=classes.iterator();i.hasNext();) {
    					Object[] o = (Object[])i.next(); Class_ clazz = (Class_)o[0];
    					if (classAssignment.getAssignment(clazz)!=null) {
        					hasTimetable = true; break;
        				}
    				}
    			}  catch (Exception e) {}
    		}
    		setDisplayTimetable(hasTimetable);
    	}
        setUserSettings(context.getUser());
        
        if (isShowExam())
            setShowExamTimetable(examAssignment!=null || Exam.hasTimetable(context.getUser().getCurrentAcademicSessionId()));

        TableStream table = null;
        int ct = 0;
        Iterator it = classes.iterator();
        SubjectArea subjectArea = null;
        String prevLabel = null;
        while (it.hasNext()){
        	Object[] o = (Object[])it.next(); Class_ c = (Class_)o[0]; CourseOffering co = (CourseOffering)o[1];
            if (subjectArea == null || !subjectArea.getUniqueId().equals(co.getSubjectArea().getUniqueId())){
            	if(table != null) {
            		table.tableComplete();
	            	try {
						outputStream.print("<br>");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
            	subjectArea = co.getSubjectArea();
            	ct = 0;
            	try {
					outputStream.print(labelForTable(subjectArea));
				} catch (IOException e) {
					e.printStackTrace();
				}
		        table = this.initTable(outputStream, context.getUser().getCurrentAcademicSessionId());
		    }		        
            this.buildClassRow(classAssignment,examAssignment, ++ct, table, co, c, "", context, prevLabel);
            prevLabel = c.getClassLabel(co);
        }  
        table.tableComplete();
    }
	
    protected TableCell buildPrefGroupLabel(CourseOffering co, PreferenceGroup prefGroup, String indentSpaces, boolean isEditable, String prevLabel){
    	if (prefGroup instanceof Class_) {
    		TableCell cell = initNormalCell(indentSpaces, isEditable);
    		Class_ aClass = (Class_) prefGroup;
	    	if(!isEditable){
	    		cell.addContent("<font color='"+disabledColor+"'>");
	    	}
	    	if ("PreferenceGroup".equals(getBackType()) && prefGroup.getUniqueId().toString().equals(getBackId()))
	    		cell.addContent("<A name=\"back\"></A>");
	    	if (co.isIsControl()) cell.addContent("<b>");
	        cell.addContent("<A name=\"A" + prefGroup.getUniqueId().toString() + "\"></A>");
	        String label = aClass.getClassLabel(co);
	        String title = aClass.getClassLabelWithTitle(co);
	        if (prevLabel != null && label.equals(prevLabel)){
	        	label = " &nbsp;";
	        }
			if (!aClass.isDisplayInScheduleBook().booleanValue()){
				title += " - Do Not Display In Schedule Book.";
				label = "<i>" + label + "</i>";
			}
	        cell.addContent(label);
	        cell.setTitle(title);
	        if (co.isIsControl()) cell.addContent("</b>");
	        cell.setNoWrap(true);
	        if(!isEditable){
	        	cell.addContent("</font>");
	        }
	        return(cell);
        } else {
        	return(super.buildPrefGroupLabel(co, prefGroup,indentSpaces, isEditable, null));
        }     
    }
	
    public void htmlTableForClasses(ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, CourseOffering co, TreeSet classes, Long subjectAreaId, SessionContext context, JspWriter outputStream){
    	String[] columns;
         if (StudentClassEnrollment.sessionHasEnrollments(context.getUser().getCurrentAcademicSessionId())) {
        	String[] tcolumns = {LABEL,
        		MSG.columnDemand(),
        		MSG.columnLimit(),
        		MSG.columnRoomRatio(),
        		MSG.columnDatePattern(),
        		MSG.columnTimePattern(),
        		MSG.columnPreferences(),
				MSG.columnInstructor(),
				MSG.columnTimetable(),
				MSG.columnSchedulePrintNote()};
        	columns = tcolumns;
         } else  {
         	String[] tcolumns = {LABEL,
         			MSG.columnLimit(),
         			MSG.columnRoomRatio(),
         			MSG.columnDatePattern(),
         			MSG.columnTimePattern(),
         			MSG.columnPreferences(),
        			MSG.columnInstructor(),
        			MSG.columnTimetable(),
        			MSG.columnSchedulePrintNote()};
            columns = tcolumns;
         };
         setVisibleColumns(columns);

        if (isShowTimetable()) {
        	boolean hasTimetable = false;
        	if (context.hasPermission(null, "Department", Right.ClassAssignments) && classAssignment != null) {
        		try {
                	if (classAssignment instanceof CachedClassAssignmentProxy) {
                		((CachedClassAssignmentProxy)classAssignment).setCache(classes);
                	}
        			for (Iterator i=classes.iterator();i.hasNext();) {
        				Class_ clazz = (Class_)i.next();
        				if (classAssignment.getAssignment(clazz)!=null) {
        					hasTimetable = true; break;
        				}
        			}
        		} catch (Exception e) {}
        	}
        	setDisplayTimetable(hasTimetable);
        	setShowDivSec(hasTimetable);
        }
        setUserSettings(context.getUser());
        
		TableStream table = this.initTable(outputStream, context.getUser().getCurrentAcademicSessionId());
        Iterator it = classes.iterator();
        Class_ cls = null;
        String prevLabel = null;
        
        int ct = 0;
        while (it.hasNext()){
            cls = (Class_) it.next();
            this.buildClassRow(classAssignment, examAssignment, ++ct, table, co, cls, "", context, prevLabel);
            prevLabel = cls.getClassLabel(co);
        }     
        table.tableComplete();
        
    }
    
    public void htmlTableForSubpartClasses(
    		SessionContext context,
    		ClassAssignmentProxy classAssignment, 
    		ExamAssignmentProxy examAssignment,
    		Long schedulingSubpartId,
    		JspWriter outputStream,
    		String backType,
    		String backId){
    	
        setBackType(backType);
        setBackId(backId);

        if (schedulingSubpartId != null) {
	    	SchedulingSubpartDAO ssDao = new SchedulingSubpartDAO();
	    	SchedulingSubpart ss = ssDao.get(schedulingSubpartId);
	        TreeSet ts = new TreeSet(new ClassComparator(ClassComparator.COMPARE_BY_HIERARCHY));
	        if (CommonValues.Yes.eq(context.getUser().getProperty(UserProperty.ClassesKeepSort))) {
	    		ts = new TreeSet(
	    			new ClassCourseComparator(
	    					context.getUser().getProperty("ClassList.sortBy",ClassCourseComparator.getName(ClassCourseComparator.SortBy.NAME)),
	    					classAssignment,
	    					"1".equals(context.getUser().getProperty("ClassList.sortByKeepSubparts", "0"))
	    			)
	    		);
	    	}
	        
	 		ts.addAll(ss.getClasses());
	 		Navigation.set(context, Navigation.sClassLevel, ts);
	        this.htmlTableForClasses(classAssignment, examAssignment, ss.getControllingCourseOffering(), ts, ss.getControllingCourseOffering().getSubjectArea().getUniqueId(), context, outputStream);
    	}
    }
    
    protected TreeSet getExams(Class_ clazz) {
        //exams directly attached to the given class
        TreeSet ret = new TreeSet(Exam.findAll(ExamOwner.sOwnerTypeClass, clazz.getUniqueId()));
        //check whether the given class is of the first subpart of the config
        SchedulingSubpart subpart = clazz.getSchedulingSubpart();
        if (subpart.getParentSubpart()!=null) return ret; 
        InstrOfferingConfig config = subpart.getInstrOfferingConfig();
        SchedulingSubpartComparator cmp = new SchedulingSubpartComparator();
        for (Iterator i=config.getSchedulingSubparts().iterator();i.hasNext();) {
            SchedulingSubpart s = (SchedulingSubpart)i.next();
            if (cmp.compare(s,subpart)<0) return ret;
        }
        InstructionalOffering offering = config.getInstructionalOffering();
        //check passed -- add config/offering/course exams to the class exams
        ret.addAll(Exam.findAll(ExamOwner.sOwnerTypeConfig, config.getUniqueId()));
        ret.addAll(Exam.findAll(ExamOwner.sOwnerTypeOffering, offering.getUniqueId()));
        for (Iterator i=offering.getCourseOfferings().iterator();i.hasNext();) {
            CourseOffering co = (CourseOffering)i.next();
            ret.addAll(Exam.findAll(ExamOwner.sOwnerTypeCourse, co.getUniqueId()));
        }
        return ret;
    }

}
