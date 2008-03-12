/*
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime.org, and individual contributors
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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.cpsolver.exam.model.ExamCourseSection;
import net.sf.cpsolver.exam.model.ExamInstructor;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.ifs.model.Constraint;

import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.model.dao.ExamOwnerDAO;

/**
 * @author Tomas Muller
 */
public class ExamInfo implements Serializable, Comparable {
    protected String iExamLabel = null;
    protected Long iExamId = null;
    protected transient Exam iExam = null;
    protected int iExamType;
    protected int iNrStudents;
    protected int iLength;
    protected int iMaxRooms;
    protected int iSeatingType;
    protected Vector iSections = null;
    protected Vector iInstructors = null;
    
    public ExamInfo(net.sf.cpsolver.exam.model.Exam exam) {
    	iExamType = ((ExamModel)exam.getModel()).getProperties().getPropertyInt("Exam.Type", Exam.sExamTypeFinal);
        iExamId = exam.getId();
        iExamLabel = exam.getName();
        iLength = exam.getLength();
        iMaxRooms = exam.getMaxRooms();
        iNrStudents = exam.getStudents().size();
        iSeatingType = (exam.hasAltSeating()?Exam.sSeatingTypeExam:Exam.sSeatingTypeNormal);
        if (!exam.getCourseSections().isEmpty()) {
            iSections = new Vector();
            for (Enumeration e=exam.getCourseSections().elements();e.hasMoreElements();) {
                ExamCourseSection ecs = (ExamCourseSection)e.nextElement();
                iSections.add(new ExamSectionInfo(ecs.getId(), ecs.getName(), ecs.getStudents().size()));
            }
        }
        iInstructors = new Vector();
        for (Enumeration e=exam.constraints().elements();e.hasMoreElements();) {
            Constraint c = (Constraint)e.nextElement();
            if (c instanceof ExamInstructor) {
                ExamInstructor instructor = (ExamInstructor)c;
                iInstructors.add(new ExamInstructorInfo(instructor.getId(), instructor.getName()));
            }
        }
    }

    public ExamInfo(Exam exam) {
    	iExamType = exam.getExamType();
        iExamId = exam.getUniqueId();
        iExamLabel = exam.getLabel();
        iMaxRooms = exam.getMaxNbrRooms();
        iExam = exam;
        iLength = exam.getLength();
        iNrStudents = exam.countStudents();
        iSeatingType = exam.getSeatingType().intValue();
    }
    
    public int getExamType() {
    	return iExamType;
    }
    
    public String getExamTypeLabel() {
        return Exam.sExamTypes[iExamType];
    }
    
    public Long getExamId() {
        return iExamId;
    }
    
    public Exam getExam() {
        if (iExam==null)
            iExam = new ExamDAO().get(iExamId);
        return iExam;
    }
    
    public Exam getExam(org.hibernate.Session hibSession) {
        return new ExamDAO().get(iExamId, hibSession);
    }
    
    public String getExamName() {
        return (iExamLabel==null?getExam().getLabel():iExamLabel);
    }
    
    public String getExamNameHtml() {
        String name = getExamName();
        if (name.length()>50)
            return "<span title='"+name+"'>"+name.substring(0,50)+"...</span>";
        else
            return name;
    }

    public int getNrStudents() {
        return iNrStudents;
    }
    
    public int getSeatingType() {
        return iSeatingType;
    }
    
    public String getSeatingTypeLabel() {
        return Exam.sSeatingTypes[iSeatingType];
    }

    public int getLength() {
        return iLength;
    }

    public int getMaxRooms() {
        return iMaxRooms;
    }

    public Vector getSections() {
        if (iSections==null) {
            iSections = new Vector();
            for (Iterator i=new TreeSet(getExam().getOwners()).iterator();i.hasNext();)
                iSections.add(new ExamSectionInfo((ExamOwner)i.next()));
        }
        return iSections;
    }
    
    public String getSectionName(String delim) {
        String name = "";
        for (Enumeration e=getSections().elements();e.hasMoreElements();) {
            ExamSectionInfo info = (ExamSectionInfo)e.nextElement();
            name += info.getName();
            if (e.hasMoreElements()) name += delim;
        }
        return name;
    }

    public Vector getInstructors() {
        if (iInstructors==null) {
            iInstructors = new Vector();
            for (Iterator i=new TreeSet(getExam().getInstructors()).iterator();i.hasNext();)
                iInstructors.add(new ExamInstructorInfo((DepartmentalInstructor)i.next()));
        }
        return iInstructors;
    }
    
    public String getInstructorName(String delim) {
        String name = "";
        for (Enumeration e=getInstructors().elements();e.hasMoreElements();) {
            ExamInstructorInfo info = (ExamInstructorInfo)e.nextElement();
            name += info.getName();
            if (e.hasMoreElements()) name += delim;
        }
        return name;
    }

    public int hashCode() {
        return getExamId().hashCode();
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamInfo)) return false;
        return ((ExamInfo)o).getExamId().equals(getExamId());
    }
    
    public int compareTo(Object o) {
        if (o==null || !(o instanceof ExamInfo)) return -1;
        ExamInfo a = (ExamInfo)o;
        int cmp = getExamName().compareTo(a.getExamName());
        if (cmp!=0) return cmp;
        return getExamId().compareTo(a.getExamId());
    }
    
    public String toString() {
        return getExamName();
    }
    
    public class ExamSectionInfo implements Serializable {
        protected Long iId;
        protected String iName;
        protected int iNrStudents = -1;
        protected transient ExamOwner iOwner = null;
        public ExamSectionInfo(Long id, String name, int nrStudents) {
            iId = id;
            iName = name;
            iNrStudents = nrStudents;
        }
        public ExamSectionInfo(ExamOwner owner) {
            iOwner = owner;
            iId = owner.getUniqueId();
            iName = owner.getLabel();
        }
        public Long getId() { return iId; }
        public Long getOwnerId() { return getOwner().getOwnerId(); }
        public Integer getOwnerType() { return getOwner().getOwnerType(); }
        public ExamOwner getOwner() {
            if (iOwner==null)
                iOwner = new ExamOwnerDAO().get(getId());
            return iOwner;
        }
        public String getName() { return iName; }
        public int getNrStudents() {
            if (iNrStudents<0)
                iNrStudents = getOwner().countStudents();
            return iNrStudents;
        }
        public ExamInfo getExam() {
            return ExamInfo.this;
        }
    }
    
    public class ExamInstructorInfo implements Serializable {
        protected Long iId;
        protected String iName = null;
        protected transient DepartmentalInstructor iInstructor;
        public ExamInstructorInfo(Long id, String name) {
            iId = id;
            iName = name;
        }
        public ExamInstructorInfo(DepartmentalInstructor instructor) {
            iId = instructor.getUniqueId();
            iName = instructor.getNameLastFirst();
            iInstructor = instructor;
        }
        public Long getId() { return iId; }
        public String getName() { return iName; }
        public DepartmentalInstructor getInstructor() {
            if (iInstructor==null)
                iInstructor = new DepartmentalInstructorDAO().get(getId());
            return iInstructor;
        }
        public ExamInfo getExam() {
            return ExamInfo.this;
        }
    }
}