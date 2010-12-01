/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.dataexchange;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.ToolBox;

import org.dom4j.Document;
import org.dom4j.Element;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Curriculum;
import org.unitime.timetable.model.CurriculumClassification;
import org.unitime.timetable.model.CurriculumCourse;
import org.unitime.timetable.model.CurriculumCourseGroup;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.Session;

public class CurriculaExport extends BaseExport{
    protected static DecimalFormat sShareDF = new DecimalFormat("0.0000");


    public void saveXml(Document document, Session session, Properties parameters) throws Exception {
    	try {
    		beginTransaction();

    		List<Curriculum> curricula = getHibSession().createQuery(
    				"select c from Curriculum c where c.academicArea.session.uniqueId = :sessionId"
    				).setLong("sessionId", session.getUniqueId()).list();
    		
            boolean externalIds = "true".equals(parameters.getProperty("tmtbl.export.curricula.externalIds", "true"));
            boolean minimalistic = "true".equals(parameters.getProperty("tmtbl.export.curricula.minimalistic", "false"));

            Element root = document.addElement("curricula");
            root.addAttribute("campus", session.getAcademicInitiative());
            root.addAttribute("year", session.getAcademicYear());
            root.addAttribute("term", session.getAcademicTerm());
            root.addAttribute("created", new Date().toString());

            document.addDocType("curricula", "-//UniTime//DTD University Course Timetabling/EN", "http://www.unitime.org/interface/Curricula_3_2.dtd");

    		if (curricula.isEmpty()) throw new Exception("No curricula defined for " + session.getLabel());
    		
    		for (Curriculum curriculum: new TreeSet<Curriculum>(curricula)) {
    			Element curriculumElement = root.addElement("curriculum");
    			
    			Hashtable<Long, Integer> groupId = new Hashtable<Long, Integer>();
    			
    			if (!minimalistic && curriculum.getAbbv() != null)
        			curriculumElement.addAttribute("abbreviation", curriculum.getAbbv());
    			
    			if (!minimalistic && curriculum.getName() != null)
        			curriculumElement.addAttribute("name", curriculum.getName());
    			
    			if (curriculum.getAcademicArea() != null) {
    				Element acadAreaElement = curriculumElement.addElement("academicArea");
    				if (externalIds && curriculum.getAcademicArea().getExternalUniqueId() != null) {
    					acadAreaElement.addAttribute("externalId", curriculum.getAcademicArea().getExternalUniqueId());
    				} 
    				acadAreaElement.addAttribute("abbreviation", curriculum.getAcademicArea().getAcademicAreaAbbreviation());
    			}

    			if (!minimalistic && curriculum.getDepartment() != null) {
    				Element departmentElement = curriculumElement.addElement("department");
    				if (externalIds && curriculum.getDepartment().getExternalUniqueId() != null) {
    					departmentElement.addAttribute("externalId", curriculum.getDepartment().getExternalUniqueId());
    				}
    				departmentElement.addAttribute("code", curriculum.getDepartment().getDeptCode());
    			}
    			
    			for (PosMajor major: (Collection<PosMajor>)curriculum.getMajors()) {
    				Element majorElement = curriculumElement.addElement("major");
    				if (externalIds && major.getExternalUniqueId() != null) {
    					majorElement.addAttribute("externalId", major.getExternalUniqueId());
    				}
    				majorElement.addAttribute("code", major.getCode());
    			}
    			
    			for (CurriculumClassification clasf: new TreeSet<CurriculumClassification>(curriculum.getClassifications())) {
    				Element clasfElement = curriculumElement.addElement("classification");
    				
    				if (!minimalistic && clasf.getName() != null) {
    					clasfElement.addAttribute("name", clasf.getName());
    				}
    				
    				if (clasf.getAcademicClassification() != null) {
    					Element acadClasfElement = clasfElement.addElement("academicClassification");
    					if (externalIds && clasf.getAcademicClassification().getExternalUniqueId() != null) {
    						acadClasfElement.addAttribute("externalId", clasf.getAcademicClassification().getExternalUniqueId());
        				}
    					acadClasfElement.addAttribute("code", clasf.getAcademicClassification().getCode());
    				}
    				
    				if (clasf.getNrStudents() != null)
        				clasfElement.addAttribute("enrollment", clasf.getNrStudents().toString());
    				
    				for (CurriculumCourse course: new TreeSet<CurriculumCourse>(clasf.getCourses())) {
    					Element courseElement = clasfElement.addElement("course");
    					if (externalIds && course.getCourse().getExternalUniqueId() != null)
    						courseElement.addAttribute("externalId", course.getCourse().getExternalUniqueId());
    					courseElement.addAttribute("subject", course.getCourse().getSubjectArea().getSubjectAreaAbbreviation());
    					courseElement.addAttribute("courseNbr", course.getCourse().getCourseNbr());
    					if (!minimalistic || course.getPercShare() != 1.0f)
        					courseElement.addAttribute("share", sShareDF.format(course.getPercShare()));
    					
    					for (CurriculumCourseGroup group: (Collection<CurriculumCourseGroup>)course.getGroups()) {
    						Integer gid = groupId.get(group.getUniqueId());
    						if (gid == null) {
    							gid = groupId.size() + 1;
    							groupId.put(group.getUniqueId(), gid);
    						}
    	    				Element groupElement = courseElement.addElement("group");
    	    				groupElement.addAttribute("id", gid.toString());
    	    				if (!minimalistic && group.getName() != null)
        	    				groupElement.addAttribute("name", group.getName());
    	    				if (!minimalistic || group.getType() != 0)
        	    				groupElement.addAttribute("type", group.getType() == 1 ? "REQ" : "OPT");
    					}
    				}
    			}
    		}
    		
            commitTransaction();
        } catch (Exception e) {
            fatal("Exception: "+e.getMessage(),e);
            rollbackTransaction();
        }
    }
    
    public static void main(String[] args) {
        try {
            if (args.length==0)
                args = new String[] {
                    "curricula.xml",
                    "PWL",
                    "2010",
                    "Spring"};

            ToolBox.configureLogging();
            
            HibernateUtil.configureHibernate(ApplicationProperties.getProperties());
            
            Session session = Session.getSessionUsingInitiativeYearTerm(args[1], args[2], args[3]);
            
            if (session==null) throw new Exception("Session "+args[1]+" "+args[2]+args[3]+" not found!");
            
            new CurriculaExport().saveXml(args[0], session, ApplicationProperties.getProperties());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
