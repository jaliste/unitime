/*
 * UniTime 3.4 (University Timetabling Application)
 * Copyright (C) 2012, UniTime LLC, and individual contributors
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
package org.unitime.timetable.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.server.GwtRpcHelper;
import org.unitime.timetable.gwt.command.server.GwtRpcImplementation;
import org.unitime.timetable.gwt.shared.EventException;
import org.unitime.timetable.gwt.shared.PageAccessException;
import org.unitime.timetable.gwt.shared.EventInterface.ResourceLookupRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.ResourceInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ResourceType;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Curriculum;
import org.unitime.timetable.model.CurriculumClassification;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.EventContact;
import org.unitime.timetable.model.NonUniversityLocation;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.Room;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.dao.EventDAO;
import org.unitime.timetable.model.dao.SessionDAO;

public class ResourceLookupBackend implements GwtRpcImplementation<ResourceLookupRpcRequest, GwtRpcResponseList<ResourceInterface>> {

	@Override
	public GwtRpcResponseList<ResourceInterface> execute(ResourceLookupRpcRequest request, GwtRpcHelper helper) {
		checkAccess(request, helper);
		
		GwtRpcResponseList<ResourceInterface> response = new GwtRpcResponseList<ResourceInterface>();
		if (request.hasLimit() && request.getLimit() == 1) {
			response.add(findResource(request.getSessionId(), request.getResourceType(), request.getName()));
		} else {
			response.addAll(findResources(request.getSessionId(), request.getResourceType(), request.getName(), request.getLimit()));
		}
		return response;
	}

	public void checkAccess(ResourceLookupRpcRequest request, GwtRpcHelper helper) throws PageAccessException {
		if ((request.getResourceType() == ResourceType.PERSON || "true".equals(ApplicationProperties.getProperty("unitime.event_timetable.requires_authentication", "true")))
				&& helper.getUser() == null)
			throw new PageAccessException(request.getResourceType().getPageTitle().substring(0, 1).toUpperCase() +
					request.getResourceType().getPageTitle().substring(1).toLowerCase() + " is only available to authenticated users.");

		if (request.getResourceType() == ResourceType.PERSON) {
			if (!request.hasName()) {
				request.setName(helper.getUser().getId());
			} else {
				if (!request.getName().equals(helper.getUser().getId()) && !(
						Roles.ADMIN_ROLE.equals(helper.getUser().getRole()) ||
						Roles.STUDENT_ADVISOR.equals(helper.getUser().getRole()) ||
						Roles.DEPT_SCHED_MGR_ROLE.equals(helper.getUser().getRole())
						)) {
					if (request.getName() != null && !request.getName().isEmpty() && !request.getName().equals(helper.getUser().getId()))
						throw new EventException("It is not allowed to access a timetable of someone else.");
				}
			}
		}
	}
	
	
	public ResourceInterface findResource(Long sessionId, ResourceType type, String name) throws EventException {
		try {
			org.hibernate.Session hibSession = EventDAO.getInstance().getSession();
			try {
				Session academicSession = SessionDAO.getInstance().get(sessionId);
				switch (type) {
				case ROOM:
					if ("true".equals(ApplicationProperties.getProperty("unitime.event_timetable.event_rooms_only", "true"))) {
						List<Room> rooms = hibSession.createQuery("select distinct r from Room r " +
								"inner join r.roomDepts rd inner join rd.department.timetableManagers m inner join m.managerRoles mr " +
								"where r.session.uniqueId = :sessionId and rd.control=true and mr.role.reference=:eventMgr and (" +
								"r.buildingAbbv || ' ' || r.roomNumber = :name or r.buildingAbbv || r.roomNumber = :name)")
								.setString("name", name)
								.setLong("sessionId", academicSession.getUniqueId())
								.setString("eventMgr", Roles.EVENT_MGR_ROLE)
								.list();
						if (!rooms.isEmpty()) {
							Room room = rooms.get(0);
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(room.getUniqueId());
							ret.setAbbreviation(room.getLabel());
							ret.setName(room.getLabel());
							ret.setHint(room.getHtmlHint());
							return ret;
						}
						List<NonUniversityLocation> locations = hibSession.createQuery("select distinct l from NonUniversityLocation l " +
								"inner join l.roomDepts rd inner join rd.department.timetableManagers m inner join m.managerRoles mr " +
								"where l.session.uniqueId = :sessionId and l.name = :name and " + 
								"rd.control=true and mr.role.reference=:eventMgr"
								)
								.setString("name", name)
								.setLong("sessionId", academicSession.getUniqueId())
								.setString("eventMgr", Roles.EVENT_MGR_ROLE)
								.list();
						if (!locations.isEmpty()) {
							NonUniversityLocation location = locations.get(0);
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(location.getUniqueId());
							ret.setAbbreviation(location.getLabel());
							ret.setName(location.getLabel());
							ret.setHint(location.getHtmlHint());
							return ret;
						}
					} else {
						List<Room> rooms = hibSession.createQuery("select distinct r from Room r " +
								"where r.session.uniqueId = :sessionId and (" +
								"r.buildingAbbv || ' ' || r.roomNumber = :name or r.buildingAbbv || r.roomNumber = :name)")
								.setString("name", name)
								.setLong("sessionId", academicSession.getUniqueId())
								.list();
						if (!rooms.isEmpty()) {
							Room room = rooms.get(0);
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(room.getUniqueId());
							ret.setAbbreviation(room.getLabel());
							ret.setName(room.getLabel());
							ret.setHint(room.getHtmlHint());
							return ret;
						}
						List<NonUniversityLocation> locations = hibSession.createQuery("select distinct l from NonUniversityLocation l " +
								"where l.session.uniqueId = :sessionId and l.name = :name"
								)
								.setString("name", name)
								.setLong("sessionId", academicSession.getUniqueId())
								.list();
						if (!locations.isEmpty()) {
							NonUniversityLocation location = locations.get(0);
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(location.getUniqueId());
							ret.setAbbreviation(location.getLabel());
							ret.setName(location.getLabel());
							ret.setHint(location.getHtmlHint());
							return ret;
						}
					}
					throw new EventException("Unable to find a " + type.getLabel() + " named " + name + ".");
				case SUBJECT:
					List<SubjectArea> subjects = hibSession.createQuery("select s from SubjectArea s where s.session.uniqueId = :sessionId and " +
							"lower(s.subjectAreaAbbreviation) = :name")
							.setString("name", name.toLowerCase()).setLong("sessionId", academicSession.getUniqueId()).list();
					if (!subjects.isEmpty()) {
						SubjectArea subject = subjects.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.SUBJECT);
						ret.setId(subject.getUniqueId());
						ret.setAbbreviation(subject.getSubjectAreaAbbreviation());
						ret.setName(subject.getLongTitle() == null ? subject.getShortTitle() : subject.getLongTitle());
						return ret;
					}
				case COURSE:
					List<CourseOffering> courses = hibSession.createQuery("select c from CourseOffering c inner join c.subjectArea s where s.session.uniqueId = :sessionId and " +
							"lower(s.subjectAreaAbbreviation || ' ' || c.courseNbr) = :name and c.instructionalOffering.notOffered = false")
							.setString("name", name.toLowerCase()).setLong("sessionId", academicSession.getUniqueId()).list();
					if (!courses.isEmpty()) {
						CourseOffering course = courses.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.COURSE);
						ret.setId(course.getUniqueId());
						ret.setAbbreviation(course.getCourseName());
						ret.setName(course.getTitle() == null ? course.getCourseName() : course.getTitle());
						return ret;
					}
					throw new EventException("Unable to find a " + type.getLabel() + " named " + name + ".");
				case CURRICULUM:
					List<Curriculum> curricula = hibSession.createQuery("select c from Curriculum c where c.department.session.uniqueId = :sessionId and " +
							"lower(c.abbv) = :name or lower(c.name) = :name")
							.setString("name", name.toLowerCase()).setLong("sessionId", academicSession.getUniqueId()).list();
					if (!curricula.isEmpty()) {
						Curriculum curriculum = curricula.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.CURRICULUM);
						ret.setId(curriculum.getUniqueId());
						ret.setAbbreviation(curriculum.getAbbv());
						ret.setName(curriculum.getName());
						return ret;
					}
					List<CurriculumClassification> classifications = hibSession.createQuery("select f from CurriculumClassification f inner join f.curriculum c where " +
							"c.department.session.uniqueId = :sessionId and (" +
							"lower(c.abbv || '/' || f.name) = :name or lower(c.name || '/' || f.name) = :name or " +
							"lower(c.abbv || '/' || f.academicClassification.code) = :name or lower(c.name || '/' || f.academicClassification.code) = :name or " + 
							"lower(c.abbv || '/' || f.academicClassification.name) = :name or lower(c.name || '/' || f.academicClassification.name) = :name or " +
							"lower(c.abbv || ' ' || f.name) = :name or lower(c.name || ' ' || f.name) = :name or " +
							"lower(c.abbv || ' ' || f.academicClassification.code) = :name or lower(c.name || ' ' || f.academicClassification.code) = :name or " + 
							"lower(c.abbv || ' ' || f.academicClassification.name) = :name or lower(c.name || ' ' || f.academicClassification.name) = :name or " +
							"lower(c.abbv || f.name) = :name or lower(c.name || f.name) = :name or " +
							"lower(c.abbv || f.academicClassification.code) = :name or lower(c.name || f.academicClassification.code) = :name or " + 
							"lower(c.abbv || f.academicClassification.name) = :name or lower(c.name || f.academicClassification.name) = :name)")
							.setString("name", name.toLowerCase()).setLong("sessionId", academicSession.getUniqueId()).list();
					if (!classifications.isEmpty()) {
						CurriculumClassification classification = classifications.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.CURRICULUM);
						ret.setId(classification.getUniqueId());
						ret.setAbbreviation(classification.getCurriculum().getAbbv() + " " + classification.getAcademicClassification().getCode());
						ret.setName(classification.getCurriculum().getName() + " " + classification.getAcademicClassification().getName());
						return ret;
					}
					throw new EventException("Unable to find a " + type.getLabel() + " named " + name + ".");
				case DEPARTMENT:
					List<Department> departments = hibSession.createQuery("select d from Department d where d.session.uniqueId = :sessionId and " +
							"(lower(d.deptCode) = :name or lower(d.abbreviation) = :name)")
							.setString("name", name.toLowerCase()).setLong("sessionId", academicSession.getUniqueId()).list();
					if (!departments.isEmpty()) {
						Department department = departments.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.DEPARTMENT);
						ret.setId(department.getUniqueId());
						ret.setAbbreviation(department.getAbbreviation() == null ? department.getDeptCode() : department.getAbbreviation());
						ret.setName(department.getName());
						return ret;
					}
					throw new EventException("Unable to find a " + type.getLabel() + " named " + name + ".");
				case PERSON:
					List<Student> students = hibSession.createQuery("select s from Student s where s.session.uniqueId = :sessionId and " +
							"s.externalUniqueId = :name or lower(s.email) = lower(:name)")
							.setString("name", name).setLong("sessionId", academicSession.getUniqueId()).list();
					if (!students.isEmpty()) {
						Student student = students.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.PERSON);
						ret.setId(student.getUniqueId());
						ret.setAbbreviation(student.getName(DepartmentalInstructor.sNameFormatShort));
						ret.setName(student.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle));
						ret.setExternalId(student.getExternalUniqueId());
						return ret;
					}
					List<DepartmentalInstructor> instructors = hibSession.createQuery("select i from DepartmentalInstructor i where i.department.session.uniqueId = :sessionId and " +
							"i.externalUniqueId = :name or lower(i.careerAcct) = lower(:name) or lower(i.email) = lower(:name)")
							.setString("name", name).setLong("sessionId", academicSession.getUniqueId()).list();
					if (!instructors.isEmpty()) {
						DepartmentalInstructor instructor = instructors.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.PERSON);
						ret.setId(instructor.getUniqueId());
						ret.setAbbreviation(instructor.getName(DepartmentalInstructor.sNameFormatShort));
						ret.setName(instructor.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle));
						ret.setExternalId(instructor.getExternalUniqueId());
						return ret;
					}
					List<EventContact> contacts = hibSession.createQuery("select c from EventContact c where " +
							"c.externalUniqueId = :name or lower(c.emailAddress) = lower(:name)")
							.setString("name", name).list();
					if (!contacts.isEmpty()) {
						EventContact contact = contacts.get(0);
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.PERSON);
						ret.setId(contact.getUniqueId());
						ret.setAbbreviation(contact.getName());
						ret.setName(contact.getName());
						ret.setExternalId(contact.getExternalUniqueId());
						return ret;
					}
					throw new EventException("No events found in " + academicSession.getLabel() + ".");
				default:
					throw new EventException("Resource type " + type.getLabel() + " not supported.");
				}
			} finally {
				hibSession.close();
			}
		} catch (EventException e) {
			throw e;
		} catch (Exception e) {
			throw new EventException("Unable to find a " + type.getLabel() + " named " + name + ": " + e.getMessage());
		}
	}
	
	public List<ResourceInterface> findResources(Long sessionId, ResourceType type, String query, int limit) throws EventException {
		try {
			if (query == null) query = "";
			org.hibernate.Session hibSession = EventDAO.getInstance().getSession();
			try {
				Session academicSession = SessionDAO.getInstance().get(sessionId);
				
				List<ResourceInterface> resources = new ArrayList<ResourceInterface>();
				switch (type) {
				case ROOM:
					if ("true".equals(ApplicationProperties.getProperty("unitime.event_timetable.event_rooms_only", "true"))) {
						List<Room> rooms = hibSession.createQuery("select distinct r from Room r " +
								"inner join r.roomDepts rd inner join rd.department.timetableManagers m inner join m.managerRoles mr, " +
								"RoomTypeOption o where r.session.uniqueId = :sessionId and " +
								"rd.control=true and mr.role.reference=:eventMgr and " + 
								"o.status = 1 and o.roomType = r.roomType and o.session = r.session and (" +
								"lower(r.roomNumber) like :name or lower(r.buildingAbbv || ' ' || r.roomNumber) like :name or lower(r.buildingAbbv || r.roomNumber) like :name) " +
								"order by r.buildingAbbv, r.roomNumber")
								.setString("name", query.toLowerCase() + "%")
								.setLong("sessionId", academicSession.getUniqueId())
								.setString("eventMgr", Roles.EVENT_MGR_ROLE)
								.setMaxResults(limit).list();
						for (Room room: rooms) {
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(room.getUniqueId());
							ret.setAbbreviation(room.getLabel());
							ret.setName(room.getLabel());
							if (room.getDisplayName() != null && !room.getDisplayName().isEmpty()) {
								ret.setTitle(room.getLabel() + " - " + room.getDisplayName());
							} else {
								ret.setTitle(room.getLabel() + " - " + room.getRoomTypeLabel() + (room.getCapacity() > 1 ? " (" + room.getCapacity() + " seats)" : ""));
							}
							resources.add(ret);
						}
						List<NonUniversityLocation> locations = hibSession.createQuery("select distinct l from NonUniversityLocation l " +
								"inner join l.roomDepts rd inner join rd.department.timetableManagers m inner join m.managerRoles mr, " +
								"RoomTypeOption o where " +
								"rd.control=true and mr.role.reference=:eventMgr and " + 
								"l.session.uniqueId = :sessionId and o.status = 1 and o.roomType = l.roomType and o.session = l.session and lower(l.name) like :name " +
								"order by l.name")
								.setString("name", query.toLowerCase() + "%")
								.setLong("sessionId", academicSession.getUniqueId())
								.setString("eventMgr", Roles.EVENT_MGR_ROLE)
								.setMaxResults(limit).list();
						for (NonUniversityLocation location: locations) {
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(location.getUniqueId());
							ret.setAbbreviation(location.getLabel());
							ret.setName(location.getLabel());
							if (location.getDisplayName() != null && !location.getDisplayName().isEmpty()) {
								ret.setTitle(location.getLabel() + " - " + location.getDisplayName());
							} else {
								ret.setTitle(location.getLabel() + " - " + location.getRoomTypeLabel() + (location.getCapacity() > 1 ? " (" + location.getCapacity() + " seats)" : ""));
							}
							resources.add(ret);
						}
						Collections.sort(resources);
						if (limit > 0 && resources.size() > limit) {
							resources = new ArrayList<ResourceInterface>(resources.subList(0, limit));
						}
					} else {
						List<Room> rooms = hibSession.createQuery("select distinct r from Room r " +
								"where r.session.uniqueId = :sessionId and (" +
								"lower(r.roomNumber) like :name or lower(r.buildingAbbv || ' ' || r.roomNumber) like :name or lower(r.buildingAbbv || r.roomNumber) like :name) " +
								"order by r.buildingAbbv, r.roomNumber")
								.setString("name", query.toLowerCase() + "%")
								.setLong("sessionId", academicSession.getUniqueId())
								.setMaxResults(limit).list();
						for (Room room: rooms) {
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(room.getUniqueId());
							ret.setAbbreviation(room.getLabel());
							ret.setName(room.getLabel());
							if (room.getDisplayName() != null && !room.getDisplayName().isEmpty()) {
								ret.setTitle(room.getLabel() + " - " + room.getDisplayName());
							} else {
								ret.setTitle(room.getLabel() + " - " + room.getRoomTypeLabel() + (room.getCapacity() > 1 ? " (" + room.getCapacity() + " seats)" : ""));
							}
							resources.add(ret);
						}
						List<NonUniversityLocation> locations = hibSession.createQuery("select distinct l from NonUniversityLocation l where " +
								"l.session.uniqueId = :sessionId and lower(l.name) like :name " +
								"order by l.name")
								.setString("name", query.toLowerCase() + "%")
								.setLong("sessionId", academicSession.getUniqueId())
								.setMaxResults(limit).list();
						for (NonUniversityLocation location: locations) {
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.ROOM);
							ret.setId(location.getUniqueId());
							ret.setAbbreviation(location.getLabel());
							ret.setName(location.getLabel());
							if (location.getDisplayName() != null && !location.getDisplayName().isEmpty()) {
								ret.setTitle(location.getLabel() + " - " + location.getDisplayName());
							} else {
								ret.setTitle(location.getLabel() + " - " + location.getRoomTypeLabel() + (location.getCapacity() > 1 ? " (" + location.getCapacity() + " seats)" : ""));
							}
							resources.add(ret);
						}
						Collections.sort(resources);
						if (limit > 0 && resources.size() > limit) {
							resources = new ArrayList<ResourceInterface>(resources.subList(0, limit));
						}
					}
					break;
				case SUBJECT:
					List<SubjectArea> subjects = hibSession.createQuery("select s from SubjectArea s where s.session.uniqueId = :sessionId and (" +
							"lower(s.subjectAreaAbbreviation) like :name or lower(' ' || s.shortTitle) like :title or lower(' ' || s.longTitle) like :title) " +
							"order by s.subjectAreaAbbreviation")
							.setString("name", query.toLowerCase() + "%").setString("title", "% " + query.toLowerCase() + "%")
							.setLong("sessionId", academicSession.getUniqueId()).setMaxResults(limit).list();
					for (SubjectArea subject: subjects) {
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.SUBJECT);
						ret.setId(subject.getUniqueId());
						ret.setAbbreviation(subject.getSubjectAreaAbbreviation());
						ret.setName(subject.getLongTitle() == null ? subject.getShortTitle() : subject.getLongTitle());
						resources.add(ret);
					}
					if (subjects.size() == 1) {
						for (CourseOffering course: new TreeSet<CourseOffering>(subjects.get(0).getCourseOfferings())) {
							if (course.getInstructionalOffering().isNotOffered()) continue;
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.COURSE);
							ret.setId(course.getUniqueId());
							ret.setAbbreviation(course.getCourseName());
							ret.setName(course.getTitle() == null ? course.getCourseName() : course.getTitle());
							ret.setTitle("&nbsp;&nbsp;&nbsp;&nbsp;" + course.getCourseName() + (course.getTitle() == null ? "" : " - " + course.getTitle()));
							resources.add(ret);
						}
					} else if (subjects.isEmpty()) {
						List<CourseOffering> courses = hibSession.createQuery("select c from CourseOffering c inner join c.subjectArea s where s.session.uniqueId = :sessionId and (" +
								"lower(s.subjectAreaAbbreviation || ' ' || c.courseNbr) like :name or lower(' ' || c.title) like :title) and c.instructionalOffering.notOffered = false " +
								"order by s.subjectAreaAbbreviation, c.courseNbr")
								.setString("name", query.toLowerCase() + "%").setString("title", "% " + query.toLowerCase() + "%")
								.setLong("sessionId", academicSession.getUniqueId()).setMaxResults(limit).list();
						for (CourseOffering course: courses) {
							if (course.getInstructionalOffering().isNotOffered()) continue;
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.COURSE);
							ret.setId(course.getUniqueId());
							ret.setAbbreviation(course.getCourseName());
							ret.setName(course.getTitle() == null ? course.getCourseName() : course.getTitle());
							resources.add(ret);
						}
					}
					break;
				case COURSE:
					List<CourseOffering> courses = hibSession.createQuery("select c from CourseOffering c inner join c.subjectArea s where s.session.uniqueId = :sessionId and (" +
							"lower(s.subjectAreaAbbreviation || ' ' || c.courseNbr) like :name or lower(' ' || c.title) like :title) and c.instructionalOffering.notOffered = false " +
							"order by s.subjectAreaAbbreviation, c.courseNbr")
							.setString("name", query.toLowerCase() + "%").setString("title", "% " + query.toLowerCase() + "%")
							.setLong("sessionId", academicSession.getUniqueId()).setMaxResults(limit).list();
					for (CourseOffering course: courses) {
						if (course.getInstructionalOffering().isNotOffered()) continue;
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.COURSE);
						ret.setId(course.getUniqueId());
						ret.setAbbreviation(course.getCourseName());
						ret.setName(course.getTitle() == null ? course.getCourseName() : course.getTitle());
						resources.add(ret);
					}
					break;
				case CURRICULUM:
					List<Curriculum> curricula = hibSession.createQuery("select c from Curriculum c where c.department.session.uniqueId = :sessionId and (" +
							"lower(c.abbv) like :name or lower(c.name) like :title) order by c.abbv")
							.setString("name", query.toLowerCase() + "%").setString("title", "%" + query.toLowerCase() + "%")
							.setLong("sessionId", academicSession.getUniqueId()).setMaxResults(limit).list();
					for (Curriculum curriculum: curricula) {
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.CURRICULUM);
						ret.setId(curriculum.getUniqueId());
						ret.setAbbreviation(curriculum.getAbbv());
						ret.setName(curriculum.getName());
						resources.add(ret);
					}
					if (curricula.size() == 1) {
						for (CurriculumClassification classification: new TreeSet<CurriculumClassification>(curricula.get(0).getClassifications())) {
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.CURRICULUM);
							ret.setId(classification.getUniqueId());
							ret.setAbbreviation(classification.getCurriculum().getAbbv() + " " + classification.getAcademicClassification().getCode());
							ret.setName(classification.getCurriculum().getName() + " " + classification.getAcademicClassification().getName());
							ret.setTitle("&nbsp;&nbsp;&nbsp;&nbsp;" + classification.getAcademicClassification().getCode() + " " + classification.getAcademicClassification().getName());
							resources.add(ret);
						}
					} else if (curricula.isEmpty()) {
						List<CurriculumClassification> classifications = hibSession.createQuery("select f from CurriculumClassification f inner join f.curriculum c where " +
								"c.department.session.uniqueId = :sessionId and (" +
								"lower(c.abbv || '/' || f.name) like :name or lower(c.name || '/' || f.name) like :title or " +
								"lower(c.abbv || '/' || f.academicClassification.code) like :name or lower(c.name || '/' || f.academicClassification.code) like :title or " + 
								"lower(c.abbv || '/' || f.academicClassification.name) like :name or lower(c.name || '/' || f.academicClassification.name) like :title or " +
								"lower(c.abbv || ' ' || f.name) like :name or lower(c.name || ' ' || f.name) like :title or " +
								"lower(c.abbv || ' ' || f.academicClassification.code) like :name or lower(c.name || ' ' || f.academicClassification.code) like :title or " + 
								"lower(c.abbv || ' ' || f.academicClassification.name) like :name or lower(c.name || ' ' || f.academicClassification.name) like :title or " +
								"lower(c.abbv || f.name) like :name or lower(c.name || f.name) like :title or " +
								"lower(c.abbv || f.academicClassification.code) like :name or lower(c.name || f.academicClassification.code) like :title or " + 
								"lower(c.abbv || f.academicClassification.name) like :name or lower(c.name || f.academicClassification.name) like :title) " +
								"order by c.abbv, f.academicClassification.code")
								.setString("name", query.toLowerCase() + "%").setString("title", "%" + query.toLowerCase() + "%")
								.setLong("sessionId", academicSession.getUniqueId())
								.setMaxResults(limit - resources.size()).list();
						for (CurriculumClassification classification: classifications) {
							ResourceInterface ret = new ResourceInterface();
							ret.setType(ResourceType.CURRICULUM);
							ret.setId(classification.getUniqueId());
							ret.setAbbreviation(classification.getCurriculum().getAbbv() + " " + classification.getAcademicClassification().getCode());
							ret.setName(classification.getCurriculum().getName() + " " + classification.getAcademicClassification().getName());
							resources.add(ret);
						}
					}
					if (limit > 0 && resources.size() > limit) {
						resources = new ArrayList<ResourceInterface>(resources.subList(0, limit));
					}
					break;
				case DEPARTMENT:
					List<Department> departments = hibSession.createQuery("select d from Department d where d.session.uniqueId = :sessionId and (" +
							"lower(d.deptCode) like :name or lower(d.abbreviation) like :name or lower(d.name) like :title) " +
							"order by d.abbreviation, d.deptCode")
							.setString("name", query.toLowerCase() + "%").setString("title", "%" + query.toLowerCase() + "%")
							.setLong("sessionId", academicSession.getUniqueId()).setMaxResults(limit).list();
					for (Department department: departments) {
						ResourceInterface ret = new ResourceInterface();
						ret.setType(ResourceType.DEPARTMENT);
						ret.setId(department.getUniqueId());
						ret.setAbbreviation(department.getAbbreviation() == null ? department.getDeptCode() : department.getAbbreviation());
						ret.setName(department.getName());
						resources.add(ret);
					}
					break;
				default:
					throw new EventException("Resource type " + type.getLabel() + " not supported.");
				}
				if (resources.isEmpty())
					throw new EventException("No " + type.getLabel() + " " + query + " found.");
				return resources;
			} finally {
				hibSession.close();
			}
		} catch (EventException e) {
			throw e;
		} catch (Exception e) {
			throw new EventException("Failed to find resources: " + e.getMessage());
		}
	}

}
