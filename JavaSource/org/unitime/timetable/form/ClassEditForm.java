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
package org.unitime.timetable.form;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.model.dao.LocationDAO;
import org.unitime.timetable.util.DynamicList;
import org.unitime.timetable.util.DynamicListObjectFactory;


/** 
 * MyEclipse Struts
 * Creation date: 12-08-2005
 * 
 * XDoclet definition:
 * @struts:form name="classEditForm"
 */
public class ClassEditForm extends PreferencesForm {
	

    // --------------------------------------------------------- Class Constants

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257849883023915058L;

	// Messages
	protected final static CourseMessages MSG = Localization.create(CourseMessages.class);
	
	/** Class Start/End Date Format **/
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");    
    
    // --------------------------------------------------------- Instance Variables
    
    private Integer nbrRooms;
    private Integer expectedCapacity;
    private Long classId;
    private Long parentClassId;
    private String section;
    private Long managingDept;
    private Long subpart;
    private String className;
    private String parentClassName;
    private String itypeDesc;
    private List instrLead;
    private String managingDeptLabel;
    private String notes;
    private List instructors;
    private List instrPctShare;
    private List assignments;
    private Long datePattern;
    private String subjectAreaId;
    private String instrOfferingId;
    private String courseName;
    private String courseTitle;
    private Boolean displayInstructor;
    private String schedulePrintNote;
    private String classSuffix;
    private Boolean displayInScheduleBook;
    private Integer maxExpectedCapacity;
    private Float roomRatio;
    private Integer minRoomLimit;
    private Boolean unlimitedEnroll;
    private Integer enrollment;
    //TODO Reservations Bypass - to be removed later
    private Boolean isCrosslisted;
    private boolean iDisplayInfo; 
    //End Bypass
    
    // --------------------------------------------------------- Classes

    /** Factory to create dynamic list element for Instructors */
    protected DynamicListObjectFactory factoryInstructors = new DynamicListObjectFactory() {
        public Object create() {
            return new String(Preference.BLANK_PREF_VALUE);
        }
    };

    // --------------------------------------------------------- Methods

    /** 
     * Validate input data
     * @param mapping
     * @param request
     * @return ActionErrors
     */
    public ActionErrors validate(
        ActionMapping mapping,
        HttpServletRequest request) {
    	
        int iRoomCapacity = -1;
        ActionErrors errors = new ActionErrors();
        
        if(nbrRooms!=null && nbrRooms.intValue()<0)
            errors.add("nbrRooms", 
                    new ActionMessage("errors.generic", MSG.errorNumberOfRoomsNegative()) );
        
        if (roomRatio==null || roomRatio.floatValue()<0.0f)
            errors.add("nbrRooms", 
                    new ActionMessage("errors.generic", MSG.errorRoomRatioNegative()) );
        
        if(expectedCapacity==null || expectedCapacity.intValue()<0) 
            errors.add("expectedCapacity", 
                    new ActionMessage("errors.generic", MSG.errorMinimumExpectedCapacityNegative()) );
        
        if(maxExpectedCapacity==null || maxExpectedCapacity.intValue()<0) 
            errors.add("maxExpectedCapacity", 
                    new ActionMessage("errors.generic", MSG.errorMaximumExpectedCapacityNegative()) );
        else 
            if(maxExpectedCapacity.intValue()<expectedCapacity.intValue()) 
                errors.add("maxExpectedCapacity", 
                        new ActionMessage("errors.generic", MSG.errorMaximumExpectedCapacityLessThanMinimum()) );
            
        if( managingDept==null || managingDept.longValue()<=0) 
            errors.add("managingDept", 
                    new ActionMessage("errors.generic", MSG.errorRequiredClassOwner()) );
        
        // Schedule print note has 2000 character limit
        if(schedulePrintNote!=null && schedulePrintNote.length()>1999) 
            errors.add("notes", 
                    new ActionMessage("errors.generic", MSG.errorSchedulePrintNoteLongerThan1999()) );
        
        
        // Notes has 1000 character limit
        if(notes!=null && notes.length()>999) 
            errors.add("notes", 
                    new ActionMessage("errors.generic", MSG.errorNotesLongerThan999()) );
        
        // At least one instructor is selected
        if (instructors.size()>0) {
            
	        // Check no duplicates or blank instructors
            if(!super.checkPrefs(instructors))
                errors.add("instrPrefs", 
                        new ActionMessage(
                                "errors.generic", 
                                MSG.errorInvalidInstructorPreference()) );

            /* -- 1 lead instructor not required
            // Check Lead Instructor is set
	        if(instrLead==null 
	                || instrLead.trim().length()==0
	                || !(new LongValidator().isValid(instrLead)) ) 
	            errors.add("instrLead", 
	                    new ActionMessage("errors.required", "Lead Instructor") );
	        */
            
	        /* -- 100% percent share not required
	        // Check sum of all percent share = 100%
	        try {
	            int total = 0;
		        for (Iterator iter=instrPctShare.iterator(); iter.hasNext(); ) {	            
		            String pctShare = iter.next().toString();
		            if(Integer.parseInt(pctShare)<=0) {
			            errors.add("instrPctShare", 
			                    new ActionMessage(
			                            "errors.integerGt", "Percent Share", "0") );
			        }
		            total += Integer.parseInt(pctShare);
		        }
		        if(total!=100) {
		            errors.add("instrPctShare", 
		                    new ActionMessage(
		                            "errors.generic",
		                            "Sum of all instructor percent shares must equal 100%") );
		        }
	        }
	        catch (Exception ex) {
	            errors.add("instrPctShare", 
                    new ActionMessage(
                            "errors.generic",
                            "Invalid instructor percent shares specified.") );
	        }	   
	        */     
        }        
        
        // Check that any room with a preference required has capacity >= room capacity for the class
        if (iRoomCapacity>0) {
            List rp = this.getRoomPrefs();
            List rpl = this.getRoomPrefLevels();
            
            for (int i=0; i<rpl.size(); i++) {
                String pl = rpl.get(i).toString();
                if (pl.trim().equalsIgnoreCase("1")) {
                    String roomId = rp.get(i).toString();                    
                    Location room = new LocationDAO().get(new Long(roomId));
                    int rCap = room.getCapacity().intValue();
                    if(rCap<iRoomCapacity) {
        	            errors.add("roomPref", 
    	                    new ActionMessage(
    	                            "errors.generic",
    	                            MSG.errorRequiredRoomTooSmall(room.getLabel(), rCap, iRoomCapacity)) );
                    }
                }
            }
        }
        
        // Check Other Preferences
        errors.add(super.validate(mapping, request));
        
        return errors;        
    }

    /** 
     * Method reset
     * @param mapping
     * @param request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        
        nbrRooms = null;
        expectedCapacity = null;
        classId = null;
        section = null;
        managingDept = null;
        subpart = null;
        className = "";
        courseName = "";
        courseTitle = "";
        parentClassName = "-";
        itypeDesc = "";
        datePattern = null;
        instrLead = DynamicList.getInstance(new ArrayList(), factoryInstructors);
        managingDeptLabel = "-";
        notes="";
        displayInstructor = null;
        schedulePrintNote = null;
        classSuffix = null;
        displayInScheduleBook = null;
        maxExpectedCapacity = null;
        roomRatio = null;
        unlimitedEnroll = null;
	    //TODO Reservations Bypass - to be removed later
        isCrosslisted = null;
        // End Bypass

        instructors = DynamicList.getInstance(new ArrayList(), factoryInstructors);
        instrPctShare= DynamicList.getInstance(new ArrayList(), factoryInstructors);
        assignments = null;
        enrollment = null;
        
        iDisplayInfo = false;
        
        super.reset(mapping, request);
    }

    /**
     * @return Returns the classId.
     */
    public Long getClassId() {
        return classId;
    }
    /**
     * @param classId The classId to set.
     */
    public void setClassId(Long classId) {
        this.classId = classId;
    }
    /**
     * @return Returns the section.
     */
    public String getSection() {
        return section;
    }
    /**
     * @param section The section to set.
     */
    public void setSection(String section) {
        this.section = section;
    }
    /**
     * @return Returns the className.
     */
    public String getClassName() {
        return className;
    }
    /**
     * @param className The className to set.
     */
    public void setClassName(String className) {
        this.className = className;
    }
    /**
     * @return Returns the assignments.
     */
    public List getAssignments() {
        return assignments;
    }
    /**
     * @return Returns the assignments.
     */
    public String getAssignments(int key) {
        return assignments.get(key).toString();
    }
    /**
     * @param key The key to set.
     * @param value The value to set.
     */
    public void setAssignments(int key, Object value) {
        this.assignments.set(key, value);
    }
    /**
     * @param assignments The assignments to set.
     */
    public void setAssignments(List assignments) {
        this.assignments = assignments;
    }

    public Long getDatePattern() {
        return datePattern;
    }
    public void setDatePattern(Long datePattern) {
        this.datePattern = datePattern;
    }

    /**
     * @return Returns the expectedCapacity.
     */
    public Integer getExpectedCapacity() {
        return expectedCapacity;
    }    
    /**
     * @param expectedCapacity The expectedCapacity to set.
     */
    public void setExpectedCapacity(Integer expectedCapacity) {
        this.expectedCapacity = expectedCapacity;
    }
    
    /**
     * @return Returns the instructors.
     */
    public List getInstructors() {
        return instructors;
    }
    /**
     * @return Returns the instructors.
     */
    public String getInstructors(int key) {
        return instructors.get(key).toString();
    }
    /**
     * @param key The key to set.
     * @param value The value to set.
     */
    public void setInstructors(int key, Object value) {
        this.instructors.set(key, value);
    }
    /**
     * @param instructors The instructors to set.
     */
    public void setInstructors(List instructors) {
        this.instructors = instructors;
    }
    
    /**
     * @return Returns the instrLead.
     */
    public List getInstrLead() {
        return instrLead;
    }
    /**
     * @param instrLead The instrLead to set.
     */
    public void setInstrLead(List instrLead) {
        this.instrLead = instrLead;
    }
    
    public void addInstrLead(String instructorId) {
        instrLead.add(instructorId);
    }
    
    public String getInstrLead(int key) {
        return instrLead.get(key).toString();
    }

    public void setInstrLead(int key, Object value) {
        this.instrLead.set(key, value);
    }

    public boolean getInstrHasPref(int key) {
        if (!"true".equals(getInstrLead(key)) && !"on".equals(getInstrLead(key))) return false;
        String instructorId = getInstructors(key);
        if (instructorId==null || instructorId.trim().length()==0 || instructorId.equals("-")) return false;
        DepartmentalInstructor di = new DepartmentalInstructorDAO().get(Long.valueOf(instructorId));
        if (di!=null && di.hasPreferences()) return true;
        return false;
    }

    /**
     * @return Returns the instrPctShare.
     */
    public List getInstrPctShare() {
        return instrPctShare;
    }
    /**
     * @return Returns the instrPctShare.
     */
    public String getInstrPctShare(int key) {
        return instrPctShare.get(key).toString();
    }
    /**
     * @param key The key to set.
     * @param value The value to set.
     */
    public void setInstrPctShare(int key, Object value) {
        this.instrPctShare.set(key, value);
    }
    /**
     * @param instrPctShare The instrPctShare to set.
     */
    public void setInstrPctShare(List instrPctShare) {
        this.instrPctShare = instrPctShare;
    }
    
    /**
     * @return Returns the nbrRooms.
     */
    public Integer getNbrRooms() {
        return nbrRooms;
    }
    /**
     * @param nbrRooms The nbrRooms to set.
     */
    public void setNbrRooms(Integer nbrRooms) {
        this.nbrRooms = nbrRooms;
    }
    /**
     * @return Returns the managingDept.
     */
    public Long getManagingDept() {
        return managingDept;
    }
    /**
     * @param managingDept The managingDept to set.
     */
    public void setManagingDept(Long owner) {
        this.managingDept = owner;
    }
    /**
     * @return Returns the parent.
     */
    public String getParentClassName() {
        return parentClassName;
    }
    /**
     * @param parent The parent to set.
     */
    public void setParentClassName(String parentClassName) {
        this.parentClassName = parentClassName;
    }
    /**
     * @return Returns the subpart.
     */
    public Long getSubpart() {
        return subpart;
    }
    /**
     * @param subpart The subpart to set.
     */
    public void setSubpart(Long subpart) {
        this.subpart = subpart;
    }
    /**
     * @return Returns the itypeDesc.
     */
    public String getItypeDesc() {
        return itypeDesc;
    }
    /**
     * @param itypeDesc The itypeDesc to set.
     */
    public void setItypeDesc(String itypeDesc) {
        this.itypeDesc = itypeDesc;
    }    
    /**
     * @return Returns the notes.
     */
    public String getNotes() {
        return notes;
    }
    /**
     * @param notes The notes to set.
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }    
    /**
     * @return Returns the managingDeptLabel.
     */
    public String getManagingDeptLabel() {
        return managingDeptLabel;
    }
    /**
     * @param managingDeptLabel The managingDeptLabel to set.
     */
    public void setManagingDeptLabel(String ownerLabel) {
        this.managingDeptLabel = ownerLabel;
    }

    /**
     * @return Returns the parentClassId.
     */
    public Long getParentClassId() {
        return parentClassId;
    }
    
    public String getSubjectAreaId() {
        return subjectAreaId;
    }
    public void setSubjectAreaId(String subjectAreaId) {
        this.subjectAreaId = subjectAreaId;
    }

    public String getInstrOfferingId() {
        return instrOfferingId;
    }
    public void setInstrOfferingId(String instrOfferingId) {
        this.instrOfferingId = instrOfferingId;
    }
    
    /**
     * @param parentClassId The parentClassId to set.
     */
    public void setParentClassId(Long parentClassId) {
        this.parentClassId = parentClassId;
    }
    
    
    public String getClassSuffix() {
        return classSuffix;
    }
    public void setClassSuffix(String classSuffix) {
        this.classSuffix = classSuffix;
    }
    public Boolean getDisplayInScheduleBook() {
        return displayInScheduleBook;
    }
    public void setDisplayInScheduleBook(Boolean displayInScheduleBook) {
        this.displayInScheduleBook = displayInScheduleBook;
    }
    public Boolean getDisplayInstructor() {
        return displayInstructor;
    }
    public void setDisplayInstructor(Boolean displayInstructor) {
        this.displayInstructor = displayInstructor;
    }
    public Integer getMaxExpectedCapacity() {
        return maxExpectedCapacity;
    }
    public void setMaxExpectedCapacity(Integer maxExpectedCapacity) {
        this.maxExpectedCapacity = maxExpectedCapacity;
    }
    public Float getRoomRatio() {
        return roomRatio;
    }
    public void setRoomRatio(Float roomRatio) {
        this.roomRatio = roomRatio;
    }
    public String getSchedulePrintNote() {
        return schedulePrintNote;
    }
    public void setSchedulePrintNote(String schedulePrintNote) {
        this.schedulePrintNote = schedulePrintNote;
    }
    public Integer getMinRoomLimit() {
        return minRoomLimit;
    }
    public void setMinRoomLimit(Integer minRoomLimit) {
        this.minRoomLimit = minRoomLimit;
    }
    
    public Boolean getUnlimitedEnroll() {
        return unlimitedEnroll;
    }
    public void setUnlimitedEnroll(Boolean unlimitedEnroll) {
        this.unlimitedEnroll = unlimitedEnroll;
    }
    
    //TODO Reservations Bypass - to be removed later
    public Boolean getIsCrosslisted() {
        return isCrosslisted;
    }
    public void setIsCrosslisted(Boolean isCrosslisted) {
        this.isCrosslisted = isCrosslisted;
    }
    // End Bypass
    
    /**
     * @param date
     * @return String representation of the date formatted as mm/dd/yyyy
     */
    public String dateToStr(Date date) {
        if(date==null)
            return "";
        else
            return dateFormat.format(date);
    }
    
    /**
     * @param date String representation of the date ( mm/dd/yyyy )
     * @return java.sql.Date object for the given string
     * @throws ParseException
     */
    public java.sql.Date strToDate(String date) throws ParseException {
        java.sql.Date dt = null;
        if(date==null || date.trim().length()==0)
            return null;
        else
            dt = new java.sql.Date( dateFormat.parse(date).getTime() );
        
        return dt;
    }
    
    /**
     * Add Instructor Data to List
     * If class instructor is null, a blank row is added
     * @param classInstr
     */
    public void addToInstructors(ClassInstructor classInstr) {
        
        // Default values
        String id = "";
        String pctShare = "0";
        boolean isLead = false;
        
        // Class Instructor Specified
        if(classInstr!=null) {
	        id = classInstr.getInstructor().getUniqueId().toString();
	        pctShare = classInstr.getPercentShare().toString();
	        isLead = classInstr.isLead().booleanValue();
        }
        else {
            // If this is the only record - set 100% share and make lead
            if(this.instructors.size()==0) {
                pctShare = "100";
                isLead = true;
            }
        }
 
        // Add row
        this.instructors.add(id);
        this.instrPctShare.add(pctShare);
        this.instrLead.add(isLead?"true":"false");
    }

    /**
     * Remove Instructor from List
     * @param deleteId
     */
    public void removeInstructor(int deleteId) {
        // Remove from lists
        this.instructors.remove(deleteId);
        this.instrPctShare.remove(deleteId);
        if (this.instrLead.size()>deleteId)
        	this.instrLead.remove(deleteId);
    }

    /**
     * Clears all preference lists
     */
    public void clearPrefs() {
        this.instructors.clear();
        this.instrPctShare.clear();
        this.instrLead.clear();
    }
    
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName=courseName; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle=courseTitle; }

    public Integer getEnrollment() {
		return enrollment;
	}

	public void setEnrollment(Integer enrollment) {
		this.enrollment = enrollment;
	}
	
	public boolean getDisplayInfo() {
		return iDisplayInfo;
	}
	
	public void setDisplayInfo(boolean displayInfo) {
		iDisplayInfo = displayInfo;
	}
    
}
