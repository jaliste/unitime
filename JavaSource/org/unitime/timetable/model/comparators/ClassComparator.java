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
package org.unitime.timetable.model.comparators;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.Vector;

import org.unitime.timetable.form.ClassListForm;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.solver.ClassAssignmentProxy;

import net.sf.cpsolver.coursett.model.TimeLocation;


/**
 * Compare Classes based on specified parameter
 * Choices are - ID (default), Label and Itype
 * 
 * @author Heston Fernandes, Tomas Muller
 */
public class ClassComparator implements Comparator {
	private Long subjectUID = null;
	
    /** Compare 2 classes on UniqueId - Default **/
    public static final short COMPARE_BY_ID = 1;
    
    /** Compare 2 classes on Class Label **/
    public static final short COMPARE_BY_LABEL = 2;
    
    /** Compare 2 classes on Subpart / Itype **/
    public static final short COMPARE_BY_ITYPE = 3;
    
    /** Compare 2 classes on SubjArea, CourseNumber, Itype, Section **/
    public static final short COMPARE_BY_SUBJ_NBR_ITYP_SEC = 4;
    
    public static final short COMPARE_BY_HIERARCHY = 5;
    
    // Decides method to compare 
    private short compareBy;
    private String classListFormSortBy = null;
    private ClassAssignmentProxy classAssignmentProxy = null;
    private boolean keepSubparts = true;

    public ClassComparator (Long subjectUID, short compareBy) {
		this.subjectUID = subjectUID;
		this.compareBy = compareBy;
	}
    
    public ClassComparator (short compareBy) {
    	this(null, compareBy);
    }
    
    public ClassComparator (String classListFormSortBy, ClassAssignmentProxy classAssignmentProxy, boolean keepSubparts) {
    	this(null, COMPARE_BY_HIERARCHY);
    	this.classListFormSortBy = classListFormSortBy;
    	this.classAssignmentProxy = classAssignmentProxy;
    	this.keepSubparts = keepSubparts;
    }
    
    public static int compare(Comparable c1, Comparable c2) {
    	return (c1==null?(c2==null?0:-1):(c2==null?1:c1.compareTo(c2)));
    }
    
    public static int compareInstructors(Vector i1, Vector i2) {
    	if (i1.isEmpty() || i2.isEmpty())
    		return Double.compare(i1.size(),i2.size());
    	if (i1.size()>1) Collections.sort(i1);
    	if (i2.size()>1) Collections.sort(i2);
    	for (int i=0;i<Math.min(i1.size(),i2.size());i++) {
    		int cmp = compare((Comparable)i1.elementAt(i),(Comparable)i2.elementAt(i));
    		if (cmp!=0) return cmp;
    	}
    	return Double.compare(i1.size(),i2.size());
    }
    
    public boolean isParentSameIType(SchedulingSubpart s1, SchedulingSubpart s2) {
		SchedulingSubpart p1 = s1.getParentSubpart();
		if (p1==null) return false;
		if (p1.equals(s2)) return true;
		if (!p1.getItype().equals(s2.getItype())) return false;
		return isParentSameIType(p1, s2);
	}

    public int compareByParentChildSameIType(Class_ c1, Class_ c2) {
    	SchedulingSubpart s1 = c1.getSchedulingSubpart();
    	SchedulingSubpart s2 = c2.getSchedulingSubpart();
    	
    	if (s1.equals(s2)) {
			while (s1.getParentSubpart()!=null && s1.getParentSubpart().getItype().equals(s1.getItype()) && !c1.getParentClass().equals(c2.getParentClass())) {
				s1 = s1.getParentSubpart(); c1 = c1.getParentClass();
				s2 = s2.getParentSubpart(); c2 = c2.getParentClass();
			}
    		return compareByClassListFormSortBy(c1,c2);
    	}
    	
    	if (s1.getItype().equals(s2.getItype())) {
    		if (isParentSameIType(s1,s2)) {
    			while (!s1.equals(s2)) {
    				s1 = s1.getParentSubpart(); c1 = c1.getParentClass();
    			}
    			while (s1.getParentSubpart()!=null && s1.getParentSubpart().getItype().equals(s1.getItype())) {
    				s1 = s1.getParentSubpart(); c1 = c1.getParentClass();
    				s2 = s2.getParentSubpart(); c2 = c2.getParentClass();
    			}
    			int cmp = compareByClassListFormSortBy(c1,c2);
    			if (cmp!=0) return cmp;
    			return 1;
    		} else if (isParentSameIType(s2,s1)) {
    			while (!s2.equals(s1)) {
    				s2 = s2.getParentSubpart(); c2 = c2.getParentClass();
    			}
    			while (s1.getParentSubpart()!=null && s1.getParentSubpart().getItype().equals(s1.getItype())) {
    				s1 = s1.getParentSubpart(); c1 = c1.getParentClass();
    				s2 = s2.getParentSubpart(); c2 = c2.getParentClass();
    			}
    			int cmp = compareByClassListFormSortBy(c1,c2);
    			if (cmp!=0) return cmp;
    			return -1;
    		}
    	}
    	
    	Comparator comparator = new SchedulingSubpartComparator(subjectUID);
    	return comparator.compare(s1,s2);
    }
    
    public int compareByClassListFormSortBy(Class_ c1, Class_ c2) {
    	int cmp = 0;
    	if (cmp!=0) return cmp;
    	try {
    		if (ClassListForm.sSortByName.equals(classListFormSortBy)) {
        		if (!c1.getSchedulingSubpart().equals(c2.getSchedulingSubpart())) {
        			Comparator comparator = new SchedulingSubpartComparator(subjectUID);
        			cmp = comparator.compare(c1.getSchedulingSubpart(), c2.getSchedulingSubpart());
        		} else
        			cmp = c1.getSectionNumber().compareTo(c2.getSectionNumber());
    		} else if (ClassListForm.sSortByDivSec.equals(classListFormSortBy)) {
            		if (!c1.getSchedulingSubpart().equals(c2.getSchedulingSubpart())) {
            			Comparator comparator = new SchedulingSubpartComparator(subjectUID);
            			cmp = comparator.compare(c1.getSchedulingSubpart(), c2.getSchedulingSubpart());
            		} else {
            			String sx1 = c1.getClassSuffix();
            			String sx2 = c2.getClassSuffix();
            			if (sx1==null) {
            				if (sx2==null) {
            					cmp = c1.getSectionNumber().compareTo(c2.getSectionNumber());
            				} else {
            					return -1;
            				}
            			} else {
            				if (sx2==null) {
            					return 1;
            				} else {
            					cmp = sx1.compareTo(sx2);
            					if (cmp!=0) return cmp;
            					cmp = c1.getSectionNumber().compareTo(c2.getSectionNumber());
            				}
            			}
            		}
    		} else if (ClassListForm.sSortByTimePattern.equals(classListFormSortBy)) {
        		Set t1s = c1.effectiveTimePatterns();
        		Set t2s = c2.effectiveTimePatterns();
        		TimePattern t1 = (t1s==null || t1s.isEmpty() ? null : (TimePattern)t1s.iterator().next());
        		TimePattern t2 = (t2s==null || t2s.isEmpty() ? null : (TimePattern)t2s.iterator().next());
        		cmp = compare(t1,t2);
        	} else if (ClassListForm.sSortByLimit.equals(classListFormSortBy)) {
        		cmp = compare(c1.getExpectedCapacity(),c2.getExpectedCapacity());
        	} else if (ClassListForm.sSortByRoomSize.equals(classListFormSortBy)) {
        		cmp = compare(c1.getMinRoomLimit(),c2.getMinRoomLimit());
        	} else if (ClassListForm.sSortByDatePattern.equals(classListFormSortBy)) {
        		cmp = compare(c1.effectiveDatePattern(),c2.effectiveDatePattern());
        	} else if (ClassListForm.sSortByInstructor.equals(classListFormSortBy)) {
        		cmp = compareInstructors(c1.getLeadInstructors(),c2.getLeadInstructors());
        	} else if (ClassListForm.sSortByAssignedTime.equals(classListFormSortBy)) {
        		Assignment a1 = (classAssignmentProxy==null?null:classAssignmentProxy.getAssignment(c1));
        		Assignment a2 = (classAssignmentProxy==null?null:classAssignmentProxy.getAssignment(c2));
        		if (a1==null) {
        			cmp = (a2==null?0:-1);
        		} else {
        			if (a2==null)
        				cmp = 1;
        			else {
        				TimeLocation t1 = a1.getPlacement().getTimeLocation();
        				TimeLocation t2 = a2.getPlacement().getTimeLocation();
        				cmp = Double.compare(t1.getStartSlots().nextInt(), t2.getStartSlots().nextInt());
        				if (cmp==0)
        					cmp = Double.compare(t1.getDayCode(), t2.getDayCode());
        				if (cmp==0)
        					cmp = Double.compare(t1.getLength(), t2.getLength());
        			}
        		}
        	} else if (ClassListForm.sSortByAssignedRoom.equals(classListFormSortBy)) {
        		Assignment a1 = (classAssignmentProxy==null?null:classAssignmentProxy.getAssignment(c1));
        		Assignment a2 = (classAssignmentProxy==null?null:classAssignmentProxy.getAssignment(c2));
        		if (a1==null) {
        			cmp = (a2==null?0:-1);
        		} else {
        			if (a2==null)
        				cmp = 1;
        			else
        				cmp = a1.getPlacement().getRoomName(",").compareTo(a2.getPlacement().getRoomName(","));
        		}
        	} else if (ClassListForm.sSortByAssignedRoomCap.equals(classListFormSortBy)) {
        		Assignment a1 = (classAssignmentProxy==null?null:classAssignmentProxy.getAssignment(c1));
        		Assignment a2 = (classAssignmentProxy==null?null:classAssignmentProxy.getAssignment(c2));
        		if (a1==null) {
        			cmp = (a2==null?0:-1);
        		} else {
        			if (a2==null)
        				cmp = 1;
        			else
        				cmp = Double.compare(a1.getPlacement().getRoomSize(),a2.getPlacement().getRoomSize());
        		}
        	}
    	} catch (Exception e) {}
    	if (cmp!=0) return cmp;
		if (!c1.getSchedulingSubpart().equals(c2.getSchedulingSubpart())) {
			Comparator comparator = new SchedulingSubpartComparator(subjectUID);
			cmp = comparator.compare(c1.getSchedulingSubpart(), c2.getSchedulingSubpart());
		}
    	if (cmp!=0) return cmp;
    	cmp = c1.getSectionNumber().compareTo(c2.getSectionNumber());
    	if (cmp!=0) return cmp;
    	return c1.getUniqueId().compareTo(c2.getUniqueId());
    }
    
    public int compare(Object o1, Object o2) {
        Class_ c1 = (Class_) o1;
        Class_ c2 = (Class_) o2;
        if (classListFormSortBy!=null) {
        	if (keepSubparts) {
        		return compareByParentChildSameIType(c1, c2);
        	} else {
        		return compareByClassListFormSortBy(c1, c2);
        	}
        }
        int cmp = 0;
        switch (compareBy) {
        	case COMPARE_BY_LABEL :
        		cmp = c1.getSchedulingSubpart().getSchedulingSubpartLabel().compareTo(c1.getSchedulingSubpart().getSchedulingSubpartLabel());
        		if (cmp!=0) return cmp;
        		cmp = c1.getClassLabel().compareTo(c2.getClassLabel());
        		if (cmp!=0) return cmp;
        	case COMPARE_BY_ITYPE :
        		cmp = c1.getSchedulingSubpart().getItype().getItype().compareTo(
        				c2.getSchedulingSubpart().getItype().getItype());
        		if (cmp!=0) return cmp;
        	case COMPARE_BY_HIERARCHY :
        		if (!c1.getSchedulingSubpart().equals(c2.getSchedulingSubpart())) {
        			Comparator comparator = new SchedulingSubpartComparator(subjectUID);
        			return comparator.compare(c1.getSchedulingSubpart(), c2.getSchedulingSubpart());
        		}
        		cmp = c1.getSectionNumber().compareTo(c2.getSectionNumber());
        		if (cmp!=0) return cmp;
        	case COMPARE_BY_SUBJ_NBR_ITYP_SEC :
        		cmp = c1.getSchedulingSubpart().getControllingCourseOffering().getCourseName().compareTo(
        				c2.getSchedulingSubpart().getControllingCourseOffering().getCourseName());
        		if (cmp!=0) return cmp;
        		cmp = c1.getSchedulingSubpart().getItype().getItype().compareTo(c2.getSchedulingSubpart().getItype().getItype());
        		if (cmp!=0) return cmp;
        		cmp = c1.getSectionNumber().compareTo(c2.getSectionNumber());
        		if (cmp!=0) return cmp;
        		cmp = c1.getSchedulingSubpart().getSchedulingSubpartSuffix().compareTo(
        				c2.getSchedulingSubpart().getSchedulingSubpartSuffix());
        		if (cmp!=0) return cmp;
        	case COMPARE_BY_ID :
        	default :
        		return c1.getUniqueId().compareTo(c2.getUniqueId());
        }
    }
    
    public Long getSubjectUID() {
        return subjectUID;
    }

    public void setSubjectUID(Long subjectUID) {
        this.subjectUID = subjectUID;
    }
}
