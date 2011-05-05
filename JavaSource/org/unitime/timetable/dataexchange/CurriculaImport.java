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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.cpsolver.ifs.util.ToolBox;

import org.dom4j.Element;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.AcademicArea;
import org.unitime.timetable.model.AcademicClassification;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Curriculum;
import org.unitime.timetable.model.CurriculumClassification;
import org.unitime.timetable.model.CurriculumCourse;
import org.unitime.timetable.model.CurriculumCourseGroup;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.util.Constants;

public class CurriculaImport extends BaseImport {
	
    public void loadXml(Element root) throws Exception {
        if (!root.getName().equalsIgnoreCase("curricula")) {
        	throw new Exception("Given XML file is not a Curricula load file.");
        }
        try {
            beginTransaction();

            String campus = root.attributeValue("campus");
            String year   = root.attributeValue("year");
            String term   = root.attributeValue("term");
            String created = root.attributeValue("created");

            Session session = Session.getSessionUsingInitiativeYearTerm(campus, year, term);
            if(session == null) {
                throw new Exception("No session found for the given campus, year, and term.");
            }
            if (created != null) {
                ChangeLog.addChange(getHibSession(), getManager(), session, session, created, ChangeLog.Source.DATA_IMPORT_CURRICULA, ChangeLog.Operation.UPDATE, null, null);
            }
            
        	info("Deleting existing curricula...");
        	for (Iterator<Curriculum> i = getHibSession().createQuery("select c from Curriculum c where c.department.session=:sessionId").
            	setLong("sessionId", session.getUniqueId()).list().iterator(); i.hasNext(); ) {
        		getHibSession().delete(i.next());
        	}
        	flush(false);
        	
        	info("Loading areas, departments, majors, and classifications...");
        	
        	Hashtable<String, AcademicArea> areasByAbbv = new Hashtable<String, AcademicArea>();
        	Hashtable<String, AcademicArea> areasByExtId = new Hashtable<String, AcademicArea>();
        	for (AcademicArea area: (List<AcademicArea>)getHibSession().createQuery(
        			"select a from AcademicArea a where a.session.uniqueId = :sessionId")
        			.setLong("sessionId", session.getUniqueId()).list()) {
        		areasByAbbv.put(area.getAcademicAreaAbbreviation(), area);
        		if (area.getExternalUniqueId() != null)
        			areasByExtId.put(area.getExternalUniqueId(), area);
        	}
           
        	Hashtable<String, Department> departmentsByCode = new Hashtable<String, Department>();
        	Hashtable<String, Department> departmentsByExtId = new Hashtable<String, Department>();
        	for (Department dept: (List<Department>)getHibSession().createQuery(
        			"select a from Department a where a.session.uniqueId = :sessionId")
        			.setLong("sessionId", session.getUniqueId()).list()) {
        		departmentsByCode.put(dept.getDeptCode(), dept);
        		if (dept.getExternalUniqueId() != null)
        			departmentsByExtId.put(dept.getExternalUniqueId(), dept);
        	}

        	Hashtable<String, PosMajor> majorsByCode = new Hashtable<String, PosMajor>();
        	Hashtable<String, PosMajor> majorsByExtId = new Hashtable<String, PosMajor>();
        	for (PosMajor major: (List<PosMajor>)getHibSession().createQuery(
        			"select a from PosMajor a where a.session.uniqueId = :sessionId")
        			.setLong("sessionId", session.getUniqueId()).list()) {
        		majorsByCode.put(major.getCode(), major);
        		if (major.getExternalUniqueId() != null)
        			majorsByExtId.put(major.getExternalUniqueId(), major);
        	}

        	Hashtable<String, AcademicClassification> clasfsByCode = new Hashtable<String, AcademicClassification>();
        	Hashtable<String, AcademicClassification> clasfsByExtId = new Hashtable<String, AcademicClassification>();
        	for (AcademicClassification clasf: (List<AcademicClassification>)getHibSession().createQuery(
        			"select a from AcademicClassification a where a.session.uniqueId = :sessionId")
        			.setLong("sessionId", session.getUniqueId()).list()) {
        		clasfsByCode.put(clasf.getCode(), clasf);
        		if (clasf.getExternalUniqueId() != null)
        			clasfsByExtId.put(clasf.getExternalUniqueId(), clasf);
        	}

        	info("Loading courses...");
        	Hashtable<String, CourseOffering> corusesByExtId = new Hashtable<String, CourseOffering>();
        	Hashtable<String, CourseOffering> corusesBySubjectCourseNbr = new Hashtable<String, CourseOffering>();
        	for (CourseOffering course: (List<CourseOffering>)getHibSession().createQuery(
        			"select a from CourseOffering a where a.subjectArea.session.uniqueId = :sessionId")
        			.setLong("sessionId", session.getUniqueId()).list()) {
        		corusesBySubjectCourseNbr.put(course.getSubjectArea().getSubjectAreaAbbreviation() + "|" + course.getCourseNbr(), course);
        		if (course.getExternalUniqueId() != null)
        			corusesByExtId.put(course.getExternalUniqueId(), course);
        	}
        	
        	info("Importing curricula...");
        	curricula: for (Iterator i = root.elementIterator(); i.hasNext(); ) {
                Element curriculumElement = (Element) i.next();
                
                Curriculum curriculum = new Curriculum();
                
                String abbv = curriculumElement.attributeValue("abbreviation");
                String name = curriculumElement.attributeValue("name");
                
                AcademicArea area = null;
                for (Iterator j = curriculumElement.elementIterator("academicArea"); j.hasNext(); ) {
                	Element areaElement = (Element)j.next();
                	String externalId = areaElement.attributeValue("externalId");
                	String abbreviation = areaElement.attributeValue("abbreviation");
                	area = (externalId != null ? areasByExtId.get(externalId) : areasByAbbv.get(abbreviation));
                	if (area == null) {
                		error("Academic area " + areaElement + " does not exist.");
                		continue curricula;
                	}
                }
                if (area == null) {
            		error("No academic area provided for a curriculum.");
            		continue curricula;
                }
                curriculum.setAcademicArea(area);
                
                Department dept = null;
                for (Iterator j = curriculumElement.elementIterator("department"); j.hasNext(); ) {
                	Element deptElement = (Element)j.next();
                	String externalId = deptElement.attributeValue("externalId");
                	String code = deptElement.attributeValue("code");
                	dept = (externalId != null ? departmentsByExtId.get(externalId) : departmentsByCode.get(code));
                	if (dept == null) {
                		error("Department " + deptElement + " does not exist.");
                	}
                }
                
                curriculum.setMajors(new HashSet<PosMajor>());
                List<PosMajor> majors = new ArrayList<PosMajor>();
                for (Iterator j = curriculumElement.elementIterator("major"); j.hasNext(); ) {
                	Element majorElement = (Element)j.next();
                	String externalId = majorElement.attributeValue("externalId");
                	String code = majorElement.attributeValue("code");
                	PosMajor major = (externalId != null ? majorsByExtId.get(externalId) : majorsByCode.get(code));
                	if (major == null) {
                		error("Major " + majorElement + " does not exist.");
                	} else {
                		curriculum.getMajors().add(major);
                		majors.add(major);
                	}
                }
                
                if (abbv == null) {
                	abbv = area.getAcademicAreaAbbreviation() + ( majors.isEmpty() ? "" : "/" );
                	for (PosMajor major: majors) {
                		if (!abbv.endsWith("/")) abbv += ",";
                		abbv += major.getCode();
                	}
                }
                if (abbv.length() > 20)	abbv = abbv.substring(0, 20);
                curriculum.setAbbv(abbv);
                
                if (name == null) {
                	name = Constants.curriculaToInitialCase(area.getShortTitle() == null ? area.getLongTitle() : area.getShortTitle()) + ( majors.isEmpty() ? "" : " / " );
                	for (PosMajor major: majors) {
                		if (!name.endsWith(" / ")) name += ", ";
                		name += Constants.curriculaToInitialCase(major.getName());
                	}
                }
                if (name.length() > 60) name = name.substring(0, 60);
                curriculum.setName(name);
                
                Hashtable<String, CurriculumCourseGroup> groups = new Hashtable<String, CurriculumCourseGroup>();
                
                int clasfOrd = 0;
                curriculum.setClassifications(new HashSet<CurriculumClassification>());
                classifications: for (Iterator j = curriculumElement.elementIterator("classification"); j.hasNext(); ) {
                	Element clasfElement = (Element) j.next();
                	
                	String clasfName = clasfElement.attributeValue("name");
                	
                	CurriculumClassification clasf = new CurriculumClassification();
                	
                	AcademicClassification acadClasf = null;
                	for (Iterator k = clasfElement.elementIterator("academicClassification"); k.hasNext(); ) {
                		Element acadClasfElement = (Element) k.next();
                    	String externalId = acadClasfElement.attributeValue("externalId");
                    	String code = acadClasfElement.attributeValue("code");
                    	acadClasf = (externalId != null ? clasfsByExtId.get(externalId) : clasfsByCode.get(code));
                    	if (area == null) {
                    		error("Academic classification " + acadClasfElement + " does not exist.");
                    		continue classifications;
                    	}
                    }
                    if (area == null) {
                		error("No academic classification provided for a curriculum classification.");
                		continue classifications;
                    }
                    clasf.setAcademicClassification(acadClasf);
                    
                    if (clasfName == null)
                    	clasfName = acadClasf.getCode();
                    
                    clasf.setName(clasfName);
                    clasf.setOrd(clasfOrd++);
                    curriculum.getClassifications().add(clasf);
                    clasf.setCurriculum(curriculum);
                    
                    String enrollment = clasfElement.attributeValue("enrollment", "0");
                    clasf.setNrStudents(Integer.parseInt(enrollment));

                    clasf.setCourses(new HashSet<CurriculumCourse>());
                    int courseOrd = 0;
                    for (Iterator k = clasfElement.elementIterator("course"); k.hasNext(); ) {
                    	Element courseElement = (Element) k.next();
                    	String externalId = courseElement.attributeValue("externalId");
                    	String subjectCourseNbr = courseElement.attributeValue("subject") + "|" + courseElement.attributeValue("courseNbr");
                    	CourseOffering courseOffering = (externalId != null ? corusesByExtId.get(externalId) : corusesBySubjectCourseNbr.get(subjectCourseNbr));
                    	if (courseOffering == null) {
                    		error("Course " + courseElement + " does not exist.");
                    		continue;
                    	}

                    	CurriculumCourse course = new CurriculumCourse();
                    	
                    	course.setCourse(courseOffering);
                    	course.setOrd(courseOrd++);
                    	
                    	String share = courseElement.attributeValue("share", "1.0");
                    	course.setPercShare(Float.parseFloat(share));
                    	
                    	course.setClassification(clasf);
                    	clasf.getCourses().add(course);
                    	
                    	course.setGroups(new HashSet<CurriculumCourseGroup>());
                    	for (Iterator l = courseElement.elementIterator("group"); l.hasNext(); ) {
                    		Element groupElement = (Element) l.next();
                    		CurriculumCourseGroup group = groups.get(groupElement.attributeValue("id"));
                    		if (group == null) {
                    			group = new CurriculumCourseGroup();
                    			group.setCurriculum(curriculum);
                    			groups.put(groupElement.attributeValue("id"), group);
                    			group.setType(0);
                    			group.setName(groupElement.attributeValue("id"));
                    		}
                			course.getGroups().add(group);
                    		String grName = groupElement.attributeValue("name");
                    		String grType = groupElement.attributeValue("type");
                    		if (grName != null)
                    			group.setName(grName);
                    		if (grType != null)
                    			group.setType("OPT".equalsIgnoreCase(grType) ? 0 : 1);
                    	}
                    }
                }
                
                if (curriculum.getDepartment() == null) {
                	Hashtable<Department, Float> dept2enrl = new Hashtable<Department, Float>();
                	for (Iterator j = curriculum.getClassifications().iterator(); j.hasNext(); ) {
                		CurriculumClassification clasf = (CurriculumClassification) j.next();
                		for (Iterator k = clasf.getCourses().iterator(); k.hasNext(); ) {
                			CurriculumCourse course = (CurriculumCourse) k.next();
                			Department d = course.getCourse().getSubjectArea().getDepartment();
                			Float x = dept2enrl.get(d);
                			dept2enrl.put(d, course.getPercShare() * clasf.getNrStudents() + (x == null ? 0.0f : x));
                		}
                	}
                	float best = 0.0f;
                	for (Map.Entry<Department, Float> entry: dept2enrl.entrySet()) {
                		if (dept == null || entry.getValue() > best) {
                			dept = entry.getKey(); best = entry.getValue();
                		}
                	}
                	if (dept == null) {
                		error("Unable to guess department for curriculum " + curriculum.getName() + ", it has no courses.");
                		continue;
                	}
                	curriculum.setDepartment(dept);
                }
                
                getHibSession().saveOrUpdate(curriculum);
                
                for (CurriculumCourseGroup group: groups.values()) {
                	getHibSession().saveOrUpdate(group);
                }

                flushIfNeeded(false);
            }
        	
        	info("All done.");
        	
            commitTransaction();
        } catch (Exception e) {
            fatal("Exception: " + e.getMessage(), e);
            rollbackTransaction();
            throw e;
        }
	}
    
    public static void main(String[] args) {
        try {
            if (args.length==0)
                args = new String[] {
                    "curricula.xml"};

            ToolBox.configureLogging();
            
            HibernateUtil.configureHibernate(ApplicationProperties.getProperties());
                        
            new CurriculaImport().loadXml(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
