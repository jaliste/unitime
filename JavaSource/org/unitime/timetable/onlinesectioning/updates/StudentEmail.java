/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2011, UniTime LLC, and individual contributors
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
package org.unitime.timetable.onlinesectioning.updates;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.activation.DataSource;
import javax.imageio.ImageIO;

import org.unitime.commons.Email;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.server.CalendarServlet;
import org.unitime.timetable.gwt.server.DayCode;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.gwt.shared.SectioningExceptionType;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningAction;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer.Lock;
import org.unitime.timetable.util.Constants;

import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

public class StudentEmail implements OnlineSectioningAction<Boolean> {
	private Long iStudentId = null;
	private List<Request> iOldRequests = null, iNewRequests = null;
	private Enrollment iOldEnrollment = null;
	private Date iTimeStamp = null;
	private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss aa");
	private String iSubject = "Class schedule change for %session%";
	private static Hashtable<Long, String> sLastMessage = new Hashtable<Long, String>();
	private byte[] iTimetableImage = null;
	
	public StudentEmail(Long studentId, List<Request> oldRequests, List<Request> newRequests) {
		iStudentId = studentId; iOldRequests = oldRequests; iNewRequests = newRequests;
		iTimeStamp = new Date();
	}
	
	public StudentEmail(Long studentId, Enrollment oldEnrollment, List<Request> newRequests) {
		iStudentId = studentId; iOldEnrollment = oldEnrollment; iNewRequests = newRequests;
		iTimeStamp = new Date();
	}

	public Long getStudentId() { return iStudentId; }
	public Enrollment getOldEnrollment() { return iOldEnrollment; }
	public List<Request> getOldRequests() { return iOldRequests; }
	public List<Request> getNewRequests() { return iNewRequests; }
	public Date getTimeStamp() { return iTimeStamp; }
	public String getSubject() { return iSubject; }
	public void setSubject(String subject) { iSubject = subject; }

	@Override
	public Boolean execute(final OnlineSectioningServer server, final OnlineSectioningHelper helper) {
		Lock lock = server.lockStudent(getStudentId(), null, true);
		try {
			OnlineSectioningLog.Action.Builder action = helper.getAction();
			action.setStudent(
					OnlineSectioningLog.Entity.newBuilder()
					.setUniqueId(getStudentId()));
			
			if (getOldEnrollment() != null && getOldEnrollment().getAssignments() != null) {
				OnlineSectioningLog.Enrollment.Builder enrollment = OnlineSectioningLog.Enrollment.newBuilder();
				enrollment.setType(OnlineSectioningLog.Enrollment.EnrollmentType.PREVIOUS);
				for (Assignment assignment: getOldEnrollment().getAssignments())
					enrollment.addSection(OnlineSectioningHelper.toProto(assignment, getOldEnrollment()));
				action.addEnrollment(enrollment);
			} else if (getOldRequests() != null) {
				OnlineSectioningLog.Enrollment.Builder enrollment = OnlineSectioningLog.Enrollment.newBuilder();
				enrollment.setType(OnlineSectioningLog.Enrollment.EnrollmentType.PREVIOUS);
				for (Request r: getNewRequests())
					if (r.getInitialAssignment() != null)
						for (Assignment assignment: r.getInitialAssignment().getAssignments())
							enrollment.addSection(OnlineSectioningHelper.toProto(assignment, r.getInitialAssignment()));
				action.addEnrollment(enrollment);
			}
			
			if (getNewRequests() != null) {
				OnlineSectioningLog.Enrollment.Builder enrollment = OnlineSectioningLog.Enrollment.newBuilder();
				enrollment.setType(OnlineSectioningLog.Enrollment.EnrollmentType.STORED);
				for (Request r: getNewRequests()) {
					action.addRequest(OnlineSectioningHelper.toProto(r));
					if (r.getAssignment() != null)
						for (Assignment assignment: r.getAssignment().getAssignments())
							enrollment.addSection(OnlineSectioningHelper.toProto(assignment, r.getAssignment()));
				}
				action.addEnrollment(enrollment);
			}
			
			Student student = server.getStudent(getStudentId());
			if (student == null) return false;
			action.getStudentBuilder().setUniqueId(student.getId()).setExternalId(student.getExternalId());
			
			boolean ret = false;
			
			helper.beginTransaction();
			try {
				org.unitime.timetable.model.Student dbStudent = StudentDAO.getInstance().get(getStudentId());
				if (dbStudent != null && dbStudent.getEmail() != null && !dbStudent.getEmail().isEmpty()) {
					final String html = generateMessage(dbStudent, server, helper);
					if (html != null) {
						Email email = new Email();

						email.addRecipient(dbStudent.getEmail(), dbStudent.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle));
						
						email.setSubject(getSubject().replace("%session%", server.getAcademicSession().toString()));
						
						email.addAttachement(new DataSource() {
							@Override
							public OutputStream getOutputStream() throws IOException {
								throw new IOException("No output stream.");
							}
							
							@Override
							public String getName() {
								return "message.html";
							}
							
							@Override
							public InputStream getInputStream() throws IOException {
								StringWriter buffer = new StringWriter();
								PrintWriter out = new PrintWriter(buffer);
								generateTimetable(out, server, helper);
								out.flush(); out.close();
								return new ByteArrayInputStream(
										html.replace("<img src='cid:timetable.png' border='0' alt='Timetable Image'/>", buffer.toString()).getBytes());
							}
							
							@Override
							public String getContentType() {
								return "text/html";
							}
						});
						
						if (iTimetableImage != null) {
							email.addAttachement(new DataSource() {
								@Override
								public OutputStream getOutputStream() throws IOException {
									throw new IOException("No output stream.");
								}
								
								@Override
								public String getName() {
									return "timetable.png";
								}
								
								@Override
								public InputStream getInputStream() throws IOException {
									return new ByteArrayInputStream(iTimetableImage);
								}
								
								@Override
								public String getContentType() {
									return "image/png";
								}
							});
						}
						
						try {
							final String calendar = CalendarServlet.getCalendar(server, student);
							if (calendar != null)
								email.addAttachement(new DataSource() {
									@Override
									public OutputStream getOutputStream() throws IOException {
										throw new IOException("No output stream.");
									}
									
									@Override
									public String getName() {
										return "timetable.ics";
									}
									
									@Override
									public InputStream getInputStream() throws IOException {
										return new ByteArrayInputStream(calendar.getBytes());
									}
									
									@Override
									public String getContentType() {
										return "text/calendar";
									}
								});
						} catch (IOException e) {
							helper.warn("Unable to create calendar for student " + student.getId() + ":" + e.getMessage());
						}
						
						String lastMessageId = sLastMessage.get(student.getId());
						if (lastMessageId != null)
							email.setInReplyTo(lastMessageId);
						
						email.setHTML(html);
						email.send();
						
						String messageId = email.getMessageId();
						if (messageId != null)
							sLastMessage.put(student.getId(), messageId);
						
						ret = true;
					}
				}
				helper.commitTransaction();
			} catch (Exception e) {
				helper.rollbackTransaction();
				throw e;
			}
			
			return ret;
		} catch (Exception e) {
			if (e instanceof SectioningException)
				throw (SectioningException)e;
			throw new SectioningException(SectioningExceptionType.UNKNOWN, e);
		} finally {
			lock.release();
		}
	}

	@Override
	public String name() {
		return "student-email";
	}
	
	private static String[] sColor1 = new String[] {
			"2952A3",
			//"A32929",
			"B1365F",
			"7A367A",
			"5229A3",
			"29527A",
			"1B887A",
			"28754E",
			"0D7813",
			"528800",
			"88880E",
			"AB8B00",
			"BE6D00",
			"B1440E",
			"865A5A",
			"705770",
			"4E5D6C",
			"5A6986",
			"4A716C",
			"6E6E41",
			"8D6F47"
	};
	
	private static String[] sColor2 = new String[] {
			"668CD9",
			//"D96666",
			"E67399",
			"B373B3",
			"8C66D9",
			"668CB3",
			"59BFB3",
			"65AD89",
			"4CB052",
			"8CBF40",
			"BFBF4D",
			"E0C240",
			"F2A640",
			"E6804D",
			"BE9494",
			"A992A9",
			"8997A5",
			"94A2bE",
			"85AAA5",
			"A7A77D",
			"C4A883"
	};
	
	private String generateMessage(org.unitime.timetable.model.Student student, OnlineSectioningServer server, OnlineSectioningHelper helper) {
		StringWriter buffer = new StringWriter();
		PrintWriter out = new PrintWriter(buffer);
		
		out.println("<html>");
		out.println("<head>");
		out.println("	<title>Current Class Schedule</title>");
		out.println("</head>");
		out.println("<body style=\"font-family: sans-serif, verdana, arial;\">");
		out.println("	<table style=\"border: 1px solid #9CB0CE; padding: 5px; margin-top: 10px; width: 800px;\" align=\"center\">");
		out.println("		<tr><td><table width=\"100%\">");
		out.println("			<tr>");
		out.println("				<td rowspan=\"2\"><img src=\"http://www.unitime.org/include/unitime.png\" border=\"0\" height=\"100px\"/></td>");
		out.println("				<td colspan=\"2\" style=\"font-size: x-large; font-weight: bold; color: #333333; text-align: right; padding: 20px 30px 10px 10px;\">Class Schedule</td>");
		out.println("			</tr>");
		out.println("			<tr>");
		out.println("				<td style=\"color: #333333; text-align: right; vertical-align: top; padding: 10px 5px 5px 5px;\">" + 
				student.getName(DepartmentalInstructor.sNameFormatLastFirstMiddle) + "</td>");
		out.println("				<td style=\"color: #333333; text-align: right; vertical-align: top; padding: 10px 5px 5px 5px;\">" + 
				server.getAcademicSession().getTerm() + " " + server.getAcademicSession().getYear() + " (" + server.getAcademicSession().getCampus() + ")</td>");
		out.println("			</tr>");
		out.println("		</table></td></tr>");
		generateChange(out, server, helper);
		out.println("		<tr><td " +
				"style=\"width: 100%; border-bottom: 1px solid #9CB0CE; padding-top: 5px; font-size: large; font-weight: bold; color: black; text-align: left;\">" +
				"List of Classes</td></tr>");
		out.println("		<tr><td>");
		
		generateListOfClasses(out, server, helper);
		
		out.println("		</td></tr>");
		
		if (getNewRequests() != null && !getNewRequests().isEmpty()) {

			out.println("		<tr><td " +
					"style=\"width: 100%; border-bottom: 1px solid #9CB0CE; padding-top: 5px; font-size: large; font-weight: bold; color: black; text-align: left;\">" +
					"Timetable</td></tr>");
			out.println("		<tr><td>");
			
			try {
				iTimetableImage = generateTimetableImage();
			} catch (Exception e) {
				helper.error("Unable to create PDF timetable: " + e.getMessage(), e);
				generateTimetable(out, server, helper);
			}
			if (iTimetableImage != null)
				out.println("<img src='cid:timetable.png' border='0' alt='Timetable Image'/>");
			
			out.println("		</td></tr>");
		}

		out.println("	</table>");
		out.println("	<table style=\"width: 800px; margin-top: -3px;\" align=\"center\">");
		out.println("		<tr>");
		out.println("			<td width=\"33%\" align=\"left\" style=\"font-size: 9pt; vertical-align: top; font-style: italic; color: #9CB0CE; white-space: nowrap;\">" +
				"Version " + Constants.VERSION + "." + Constants.BLD_NUMBER.replaceAll("@build.number@","?") + " built on " + Constants.REL_DATE.replaceAll("@build.date@", "?") + "</td>");
		out.println("			<td width=\"34%\" align=\"center\" style=\"font-size: 9pt; vertical-align: top; font-style: italic; color: #9CB0CE; white-space: nowrap;\">" +
				"&copy; 2008 - 2011 UniTIme LLC,<br>distributed under GNU General Public License.</td>");
		out.println("			<td width=\"33%\" align=\"right\" style=\"font-size: 9pt; vertical-align: top; font-style: italic; color: #9CB0CE; white-space: nowrap;\">" +
				sDateFormat.format(getTimeStamp()) + "</td>");
		out.println("		</tr>");
		out.println("	</table>");
		out.println("</body>");
		out.println("</html>");
		
		out.flush(); out.close();
		return buffer.getBuffer().toString();
	}
	
	private void generateListOfClassesHeader(PrintWriter out) {
		out.println("<table width=\"100%\">");
		out.println("<tr>");
		String style = "font-weight: bold; padding-top: 5px;";
		out.println("	<td style=\"" + style + "\">Subject</td>");
		out.println("	<td style=\"" + style + "\">Course</td>");
		out.println("	<td style=\"" + style + "\">Type</td>");
		out.println("	<td style=\"" + style + "\">Section</td>");
		out.println("	<td style=\"" + style + "\">Days</td>");
		out.println("	<td style=\"" + style + "\">Start</td>");
		out.println("	<td style=\"" + style + "\">End</td>");
		out.println("	<td style=\"" + style + "\">Date</td>");
		out.println("	<td style=\"" + style + "\">Room</td>");
		out.println("	<td style=\"" + style + "\">Instructor</td>");
		out.println("	<td style=\"" + style + "\">Requires</td>");
		out.println("</tr>");
	}
	
	private void generateListOfClassesFooter(PrintWriter out, boolean link) {
		if (link) {
			String url = ApplicationProperties.getProperty("unitime.url");
			if (url != null) {
				out.println("	<tr><td colspan=\"11\" style=\"font-size: 9pt; font-style: italic; color: #9CB0CE; text-align: right; margin-top: -2px; white-space: nowrap;\">");
				out.println("		For an up to date schedule, please visit " +
						"<a href='" + url + "/gwt.jsp?page=sectioning' style=\"color: inherit; background-color : transparent;\">" + url+ "</a>.");
				out.println("	</td></tr>");
			}
		}
		out.println("</table>");
	}
	
	public static boolean equals(Section a, Section b) {
		return
			ToolBox.equals(a.getName(), b.getName()) &&
			ToolBox.equals(a.getTime(), b.getTime()) &&
			ToolBox.equals(a.getRooms(), b.getRooms()) && 
			ToolBox.equals(a.getChoice(), b.getChoice()) &&
			ToolBox.equals(a.getParent() == null ? null : a.getParent().getName(), b.getParent() == null ? null : b.getParent().getName());
	}
	
	private void generateListOfClassesLine(PrintWriter out, Request request, Section section, String style) {
		out.println("<tr>");
		out.println("	<td style= \"" + style + "\">" + (request.getAssignment() == null ? request.getInitialAssignment() : request.getAssignment()).getCourse().getSubjectArea() + "</td>");
		out.println("	<td style= \"" + style + "\">" + (request.getAssignment() == null ? request.getInitialAssignment() : request.getAssignment()).getCourse().getCourseNumber() + "</td>");
		out.println("	<td style= \"" + style + "\">" + section.getSubpart().getName() + "</td>");
		out.println("	<td style= \"" + style + "\">" + section.getName() + "</td>");
		if (section.getTime() == null) {
			out.println("	<td style= \"" + style + "\" colspan=\"4\">Arrange Hours</td>");
		} else {
			out.println("	<td style= \"" + style + "\">" + DayCode.toString(section.getTime().getDayCode()) + "</td>");
			out.println("	<td style= \"" + style + "\">" + section.getTime().getStartTimeHeader() + "</td>");
			out.println("	<td style= \"" + style + "\">" + section.getTime().getEndTimeHeader() + "</td>");
			out.println("	<td style= \"" + style + "\">" + section.getTime().getDatePatternName() + "</td>");
		}
		if (section.getRooms() == null || section.getRooms().isEmpty()) {
			out.println("	<td style= \"" + style + "\">&nbsp;</td>");
		} else {
			String rooms = "";
			for (RoomLocation room: section.getRooms()) {
				if (!rooms.isEmpty()) rooms += ", ";
				rooms += room.getName();
			}
			out.println("	<td style= \"" + style + "\">" + rooms + "</td>");
		}
		if (section.getChoice().getInstructorNames() == null|| section.getChoice().getInstructorNames().isEmpty()) {
			out.println("	<td style= \"" + style + "\">&nbsp;</td>");
		} else {
			String[] instructors = section.getChoice().getInstructorNames().split(":");
			String html = "";
			for (String instructor: instructors) {
				String[] nameEmail = instructor.split("\\|");
				if (!html.isEmpty()) html += ", ";
				if (nameEmail.length < 2) {
					html += nameEmail[0];
				} else {
					html += "<a href='mailto:" + nameEmail[1] + "' style=\"color: inherit; background-color : transparent; text-decoration: none;\">" + nameEmail[0]+ "</a>";
				}
			}
			out.println("	<td style= \"" + style + "\">" + html + "</td>");
		}
		out.println("	<td style= \"" + style + "\">" + (section.getParent() == null ? "&nbsp;" : section.getParent().getName()) + "</td>");
		out.println("</tr>");
	}
	
	private String diff(String a, String b) {
		if (a == null || a.isEmpty())
			return (b == null || b.isEmpty() ? "<span style='text-decoration: none;'>&nbsp;</span>" : b);
		if (b == null || b.isEmpty())
			return "<span style='text-decoration: line-through;'>" + a + "</span>";
		if (a.equals(b))
			return a;
		return "<span style='text-decoration: line-through;'>" + a + "</span> &rarr; " + b;
	}
	
	private void generateListOfClassesDiff(PrintWriter out, Request request, Section old, Section section, String style) {
		out.println("<tr>");
		out.println("	<td style= \"" + style + "\">" + request.getAssignment().getCourse().getSubjectArea() + "</td>");
		out.println("	<td style= \"" + style + "\">" + request.getAssignment().getCourse().getCourseNumber() + "</td>");
		out.println("	<td style= \"" + style + "\">" + section.getSubpart().getName() + "</td>");
		out.println("	<td style= \"" + style + "\">" + diff(old.getName(), section.getName()) + "</td>");
		if (section.getTime() == null) {
			out.println("	<td style= \"" + style + "\" colspan=\"4\">" +
					diff(old.getTime() == null ? "Arrange Hours" : DayCode.toString(old.getTime().getDayCode()) + " " + old.getTime().getStartTimeHeader(),
					"Arrange Hours") + "</td>");
		} else {
			out.println("	<td style= \"" + style + "\">" + 
					diff(old.getTime() == null ? null : DayCode.toString(old.getTime().getDayCode()), DayCode.toString(section.getTime().getDayCode())) +
					"</td>");
			out.println("	<td style= \"" + style + "\">" + 
					diff(old.getTime() == null ? null : old.getTime().getStartTimeHeader(), section.getTime().getStartTimeHeader()) +
					"</td>");
			out.println("	<td style= \"" + style + "\">" + 
					diff(old.getTime() == null ? null : old.getTime().getEndTimeHeader(), section.getTime().getEndTimeHeader()) +
					"</td>");
			out.println("	<td style= \"" + style + "\">" +
					diff(old.getTime() == null ? null : old.getTime().getDatePatternName(), section.getTime().getDatePatternName()) +
					"</td>");
		}
	
		String oldRooms = "";
		if (old.getRooms() != null && !old.getRooms().isEmpty()) {
			for (RoomLocation room: old.getRooms()) {
				if (!oldRooms.isEmpty()) oldRooms += ", ";
				oldRooms += room.getName();
			}
		}
		
		String rooms = "";
		if (section.getRooms() != null && !section.getRooms().isEmpty()) {
			for (RoomLocation room: section.getRooms()) {
				if (!rooms.isEmpty()) rooms += ", ";
				rooms += room.getName();
			}
		}
		out.println("	<td style= \"" + style + "\">" + diff(oldRooms, rooms) + "</td>");
		
		String oldInstructors = "";
		if (old.getChoice().getInstructorNames() != null && !old.getChoice().getInstructorNames().isEmpty()) {
			for (String instructor: old.getChoice().getInstructorNames().split(":")) {
				String[] nameEmail = instructor.split("\\|");
				if (!oldInstructors.isEmpty()) oldInstructors += ", ";
				if (nameEmail.length < 2) {
					oldInstructors += nameEmail[0];
				} else {
					oldInstructors += "<a href='mailto:" + nameEmail[1] + "' style=\"color: inherit; background-color : transparent; text-decoration: none;\">" + nameEmail[0]+ "</a>";
				}
			}
		}
		String instructors = "";
		if (section.getChoice().getInstructorNames() != null && !section.getChoice().getInstructorNames().isEmpty()) {
			for (String instructor: section.getChoice().getInstructorNames().split(":")) {
				String[] nameEmail = instructor.split("\\|");
				if (!instructors.isEmpty()) instructors += ", ";
				if (nameEmail.length < 2) {
					instructors += nameEmail[0];
				} else {
					instructors += "<a href='mailto:" + nameEmail[1] + "' style=\"color: inherit; background-color : transparent; text-decoration: none;\">" + nameEmail[0]+ "</a>";
				}
			}
		}
		out.println("	<td style= \"" + style + "\">" + diff(oldInstructors, instructors) + "</td>");

		out.println("	<td style= \"" + style + "\">" + diff(old.getParent() == null ? null : old.getParent().getName(), section.getParent() == null ? null : section.getParent().getName()) + "</td>");
		out.println("</tr>");
	}
	
	private void generateListOfClassesLine(PrintWriter out, Request request) {
		if (request.getAssignment() == null) {
			if (request instanceof CourseRequest) {
				CourseRequest cr = (CourseRequest)request;
				if (!cr.getStudent().canAssign(cr)) return;
				Course course = cr.getCourses().get(0);
				out.println("<tr>");
				String style = "color: red; white-space: nowrap; border-top: 1px dashed #9CB0CE;";
				out.println("	<td style= \"" + style + "\">" + course.getSubjectArea() + "</td>");
				out.println("	<td style= \"" + style + "\">" + course.getCourseNumber() + "</td>");
				out.println("	<td style= \"" + style + "\">&nbsp;</td>");
				out.println("	<td style= \"" + style + "\">&nbsp;</td>");
				if (request.isAlternative())
					out.println("	<td style= \"" + style + "\" colspan=\"7\" align=\"center\">wait-listed alternative</td>");
				else
					out.println("	<td style= \"" + style + "\" colspan=\"7\" align=\"center\">wait-listed</td>");
				out.println("</tr>");
			}
			return;
		}
		if (request instanceof FreeTimeRequest) {
			FreeTimeRequest fr = (FreeTimeRequest)request;
			String style = "white-space: nowrap; border-top: 1px dashed #9CB0CE;";
			out.println("<tr>");
			out.println("	<td style= \"" + style + "\">Free</td>");
			out.println("	<td style= \"" + style + "\">Time</td>");
			out.println("	<td style= \"" + style + "\">&nbsp;</td>");
			out.println("	<td style= \"" + style + "\">&nbsp;</td>");
			out.println("	<td style= \"" + style + "\">" + DayCode.toString(fr.getTime().getDayCode()) + "</td>");
			out.println("	<td style= \"" + style + "\">" + fr.getTime().getStartTimeHeader() + "</td>");
			out.println("	<td style= \"" + style + "\">" + fr.getTime().getEndTimeHeader() + "</td>");
			out.println("	<td style= \"" + style + "\" colspan=\"4\">&nbsp;</td>");
			out.println("</tr>");
			return;
		}
		boolean first = true;
		for (Section section: request.getAssignment().getSections()) {
			String style = "white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
			generateListOfClassesLine(out, request, section, style);
			first = false;
		}
	}

	
	protected void generateListOfClasses(PrintWriter out, OnlineSectioningServer server, OnlineSectioningHelper helper) {
		if (getNewRequests() == null && getNewRequests().isEmpty()) {
			out.println("<table width=\"100%\"><tr><td class=\"unitime-ErrorMessage\">No class schedule.</td></tr></table>");
			return;
		}
		generateListOfClassesHeader(out);
		for (Request request: getNewRequests()) {
			generateListOfClassesLine(out, request);
		}
		generateListOfClassesFooter(out, true);
	}
	
	public void generateChange(PrintWriter out, OnlineSectioningServer server, OnlineSectioningHelper helper) {
		if (getOldEnrollment() != null) {
			Enrollment newEnrollment = null;
			Request newRequest = null;
			for (Request r: getNewRequests()) {
				if (r.equals(getOldEnrollment().getRequest())) {
					newRequest = r;
					newEnrollment = r.getAssignment();
					break;
				}
			}
			if (getOldEnrollment().getAssignments() == null && newEnrollment != null && newEnrollment.getCourse() != null) {
				setSubject("You are now enrolled in " + newEnrollment.getCourse().getSubjectArea() + " " + newEnrollment.getCourse().getCourseNumber());
				out.println("		<tr><td " +
						"style=\"width: 100%; border-bottom: 1px solid #9CB0CE; padding-top: 5px; font-size: large; font-weight: bold; color: black; text-align: left;\">" +
						newEnrollment.getCourse().getSubjectArea() + " " + newEnrollment.getCourse().getCourseNumber() + " Enrollment</td></tr>");
				out.println("		<tr><td>");
				generateListOfClassesHeader(out);
				generateListOfClassesLine(out, newEnrollment.getRequest());
				generateListOfClassesFooter(out, false);
				out.println("		</td></tr>");
			} else if (getOldEnrollment().getAssignments() != null && newEnrollment != null && newEnrollment.getCourse() != null) {
				setSubject("Enrollment changed in " + newEnrollment.getCourse().getSubjectArea() + " " + newEnrollment.getCourse().getCourseNumber());
				out.println("		<tr><td " +
						"style=\"width: 100%; border-bottom: 1px solid #9CB0CE; padding-top: 5px; font-size: large; font-weight: bold; color: black; text-align: left;\">" +
						newEnrollment.getCourse().getSubjectArea() + " " + newEnrollment.getCourse().getCourseNumber() + " Enrollment</td></tr>");
				out.println("		<tr><td>");
				generateListOfClassesHeader(out);
				boolean first = true;
				sections: for (Section section: newEnrollment.getSections()) {
					for (Section old: getOldEnrollment().getSections()) {
						if (old.getSubpart().getId() == section.getSubpart().getId()) {
							String style = "white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
							generateListOfClassesDiff(out, newEnrollment.getRequest(), old, section, style);
							first = false;
							continue sections;
						}
					}
					String style = "white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
					generateListOfClassesLine(out, newEnrollment.getRequest(), section, style);
					first = false;
				}
				sections: for (Section old: getOldEnrollment().getSections()) {
					for (Section section: newEnrollment.getSections())
						if (old.getSubpart().getId() == section.getSubpart().getId()) continue sections;
					String style = "text-decoration: line-through; white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
					generateListOfClassesLine(out, getOldEnrollment().getRequest(), old, style);
					first = false;
				}
				generateListOfClassesFooter(out, false);
				out.println("		</td></tr>");
			} else if (getOldEnrollment().getAssignments() != null && getOldEnrollment().getCourse() != null && newEnrollment == null) {
				setSubject("Course " + getOldEnrollment().getCourse().getSubjectArea() + " " + getOldEnrollment().getCourse().getCourseNumber() + " dropped due to a course change.");
				out.println("		<tr><td " +
						"style=\"width: 100%; border-bottom: 1px solid #9CB0CE; padding-top: 5px; font-size: large; font-weight: bold; color: black; text-align: left;\">" +
						getOldEnrollment().getCourse().getSubjectArea() + " " + getOldEnrollment().getCourse().getCourseNumber() + " Enrollment</td></tr>");
				out.println("		<tr><td>");
				if (newRequest !=  null && newRequest.getStudent().canAssign(newRequest))
					out.println("<table width=\"100%\"><tr><td class=\"unitime-ErrorMessage\">Course is wait-listed" + (newRequest.isAlternative() ? " alternative" : "") + ".</td></tr></table>");
				out.println("		</td></tr>");
			}
		} else if (getOldRequests() != null && !getOldRequests().isEmpty()) {
			boolean somethingWasAssigned = false;
			for (Request or: getOldRequests()) {
				if (or instanceof CourseRequest && or.getInitialAssignment() != null) {
					somethingWasAssigned = true; break;
				}
			}
			if (somethingWasAssigned) {
				out.println("		<tr><td " +
						"style=\"width: 100%; border-bottom: 1px solid #9CB0CE; padding-top: 5px; font-size: large; font-weight: bold; color: black; text-align: left;\">" +
						"Enrollment Changes</td></tr>");
				out.println("		<tr><td>");
				int nrLines = 0;
				generateListOfClassesHeader(out);
				requests: for (Request nr: getNewRequests()) {
					if (nr instanceof FreeTimeRequest) continue;
					for (Request or: getOldRequests()) {
						if (or instanceof FreeTimeRequest) continue;
						if (or.getId() == nr.getId()) {
							if (or.getInitialAssignment() == null) {
								if (nr.getAssignment() == null) continue; // bot unassigned
								// was assigned
								boolean first = true;
								for (Section section: nr.getAssignment().getSections()) {
									String style = "white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
									generateListOfClassesLine(out, nr, section, style);
									nrLines++;
									first = false;
								}
							} else if (nr.getAssignment() == null) {
								// was un-assigned
								boolean first = true;
								for (Section section: or.getInitialAssignment().getSections()) {
									String style = "text-decoration: line-through; white-space: nowrap; white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
									generateListOfClassesLine(out, or, section, style);
									nrLines++;
									first = false;
								}
							} else {
								// both assigned
								boolean first = true;
								sections: for (Section section: nr.getAssignment().getSections()) {
									for (Section old: or.getInitialAssignment().getSections()) {
										if (old.getSubpart().getId() == section.getSubpart().getId()) {
											if (equals(section, old)) continue sections;
											String style = "white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
											generateListOfClassesDiff(out, nr, old, section, style);
											nrLines++;
											first = false;
											continue sections;
										}
									}
									String style = "white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
									generateListOfClassesLine(out, nr, section, style);
									nrLines++;
									first = false;
								}
								sections: for (Section old: or.getInitialAssignment().getSections()) {
									for (Section section: nr.getAssignment().getSections())
										if (old.getSubpart().getId() == section.getSubpart().getId()) continue sections;
									String style = "text-decoration: line-through; white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
									generateListOfClassesLine(out, or, old, style);
									nrLines++;
									first = false;
								}
							}
							continue requests;
						}
					}
					// old request not found
					if (nr.getAssignment() != null) {
						boolean first = true;
						for (Section section: nr.getAssignment().getSections()) {
							String style = "white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
							generateListOfClassesLine(out, nr, section, style);
							nrLines++;
							first = false;
						}
					}
				}
				requests: for (Request or: getOldRequests()) {
					if (or instanceof FreeTimeRequest || or.getInitialAssignment() == null) continue;
					for (Request nr: getNewRequests()) {
						if (or instanceof FreeTimeRequest) continue;
						if (or.getId() == nr.getId()) continue requests;
					}
					// new request not found
					boolean first = true;
					for (Section section: or.getInitialAssignment().getSections()) {
						String style = "text-decoration: line-through; white-space: nowrap; white-space: nowrap;" + (first ? " border-top: 1px dashed #9CB0CE;" : "");
						generateListOfClassesLine(out, or, section, style);
						nrLines++;
						first = false;
					}
				}
				if (nrLines == 0)
					out.println("<tr><td colspan='11'><i>No enrollment change detected.</i></td></tr>");
				generateListOfClassesFooter(out, false);
				out.println("		</td></tr>");
			} else {
				setSubject("Class schedule notification for %session%");
			}
		} else {
			setSubject("Class schedule notification for %session%");
		}
		
	}
	
	public void generateTimetable(PrintWriter out, OnlineSectioningServer server, OnlineSectioningHelper helper) {
		int nrDays = 5, firstHour = 7, lastHour = 18;
		boolean hasSat = false, hasSun = false;
		List<Assignment> table[][] = new List[Constants.NR_DAYS][Constants.SLOTS_PER_DAY];
		for (Request request: getNewRequests()) {
			if (request.getAssignment() == null) continue;
			for (Assignment assignment: request.getAssignment().getAssignments()) {
				if (assignment.getTime() == null) continue;
				int dayCode = assignment.getTime().getDayCode();
				if ((dayCode & Constants.DAY_CODES[Constants.DAY_SAT]) != 0) hasSat = true;
				if ((dayCode & Constants.DAY_CODES[Constants.DAY_SUN]) != 0) hasSun = true;
				int startHour = (assignment.getTime().getStartSlot() * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN) / 60;
				if (startHour < firstHour) firstHour = startHour;
				int endHour = ((assignment.getTime().getStartSlot() + assignment.getTime().getLength()) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN + 59) / 60;
				if (endHour > lastHour) lastHour = endHour;
				for (Enumeration<Integer> e = assignment.getTime().getSlots(); e.hasMoreElements(); ) {
					int slot = e.nextElement();
					int day = slot / Constants.SLOTS_PER_DAY;
					int time = slot % Constants.SLOTS_PER_DAY;
					if (table[day][time] == null)
						table[day][time] = new ArrayList<Assignment>();
					table[day][time].add(assignment);
				}
			}
		}
		if (hasSat) nrDays = 6;
		if (hasSun) nrDays = 7;
		
		out.println("<table cellspacing='0' cellpadding='0'>");
		
		out.println("<tr><td align='left' style='vertical-align: top;'><table cellspacing='0' cellpadding='0'><tr>");
		
		out.println("<td align='left' style='vertical-align: top;'><div style='font-size: x-small; text-align: center; color: #6991CE; display: block; width: 30px;'></div></td>");
		
		for (int i = 0; i < nrDays; i++) {
			out.println("<td align='center' style='vertical-align: top;'><div style='font-size: x-small; text-align: center; color: #6991CE; display: block; width: 180px; '>" +
					DayCode.values()[i].getName() +  "</div></td>");
		}
		
		out.println("</tr></table></td></tr>");
		
		out.println("<tr><td align='left' style='vertical-align: top;'><div style='width: " + (35 + 180 * nrDays) + "px; '>");
		
		out.println("<table cellspacing='0' cellpadding='0'><tr>");
		
		out.println("<td align='left' style='vertical-align: top; '><div style='position: relative; overflow-x: hidden; overflow-y: hidden; width: 30px; height: " + (50 * (lastHour - firstHour)) + "px; '>");
		for (int h = firstHour; h < lastHour; h++) {
			int top = 50 * (h - firstHour);
			out.println("<div style='font-size: x-small; text-align: center; padding-right: 2px; color: #6991CE; display: block; border-top: 1px solid transparent; height: 100%; width: 28px; white-space: nowrap; " +
				"position: absolute; left: 0px; top: " + top + "px;'>" +  (h > 12 ? h - 12 : h) + (h < 12 ? "am" : "pm") + "</div>");
			out.println("<div style='font-size: x-small; text-align: center; padding-right: 2px; color: #6991CE; display: block; border-top: 1px solid transparent; height: 100%; width: 28px;" +
				"position: absolute; left: 0px; top: " + (25 + top) + "px; '></div>");
		}
		out.println("</div></td>");

		out.println("<td align='left' style='vertical-align: top; '>");
		out.println("<div style='border-bottom: 1px solid #DDDDDD; position: relative; overflow-x: hidden; overflow-y: hidden; width: " + (5 + 180 * nrDays) + "px; height: " + (50 * (lastHour - firstHour)) + "px; '>");
		
		out.println("<div style='position: relative; overflow-x: hidden; overflow-y: hidden; width: 100%; height: 100%; '>");
		
		// working hours
		out.println("<div style='background: #FFFDDD; width: " + (2 + 180 * nrDays) + "px; height: 500px; position: absolute; left: 0px; top: " + (25  + 50 * (7 - firstHour)) + "px;'></div>");
		
		for (Request request: getNewRequests()) {
			if (request instanceof FreeTimeRequest) {
				FreeTimeRequest fr = (FreeTimeRequest)request;
				for (DayCode dow: DayCode.toDayCodes(fr.getTime().getDayCode())) {
					if (dow.getIndex() >= nrDays) continue;
					if (fr.getTime().getStartSlot() + fr.getTime().getLength() < 12 * firstHour) continue;
					if (fr.getTime().getStartSlot() > 12 * lastHour) continue;
					out.println("<div style='background: #FFE1DD; width: 100%; color: #BA5353; font-size: x-small; text-align: left; white-space: nowrap; overflow: hidden;" +
							"width: 183px; height: " + (125 * fr.getTime().getLength() / 30) + "px; " +
							"position: absolute; left: " + (180 * dow.getIndex()) + "px;" +
							"top: " + (125 * fr.getTime().getStartSlot() / 30 - 50 * firstHour) + "px; '>");
					out.println("<div style='padding-left: 5px; white-space: nowrap; '>Free " +
							DayCode.toString(fr.getTime().getDayCode()) + " " + fr.getTime().getStartTimeHeader() + " - " + fr.getTime().getEndTimeHeader() + "</div>");
					out.println("</div>");
				}
			}
		}
		
		for (int h = firstHour; h < lastHour; h++) {
			int top = 50 * (h - firstHour);
			out.println("<div style='display: block; border-top: 1px solid #DDDDDD; width: 100%; position: absolute; left: 0px; top: " + top + "px; '></div>");
			out.println("<div style='display: block; border-top: 1px dotted #DDDDDD; width: 100%; position: absolute; left: 0px; top: " + (25 + top) + "px; '></div>");
		}

		for (int i = 0; i <= nrDays; i++) {
			int left = 180 * i;
			out.println("<div style='height: 100%; position: absolute; top: 0px; left: 0%; border-left: 1px solid #DDDDDD; border-right: 1px solid #DDDDDD; width: 2px; " +
					"position: absolute; left: " + left + "px; top: 0px; '></div>");
		}
		
		
		
		out.println("</div>");
		
		int color = 0;
		for (Request request: getNewRequests()) {
			if (request instanceof CourseRequest && request.getAssignment() != null) {
				for (Section section: request.getAssignment().getSections()) {
					if (section.getTime() == null) continue;
					for (DayCode dow: DayCode.toDayCodes(section.getTime().getDayCode())) {
						int col = 0;
						int index = 0;
						for (int i = 0; i < section.getTime().getLength(); i++) {
							col = Math.max(col, table[dow.getIndex()][section.getTime().getStartSlot() + i].size());
							index = Math.max(index, table[dow.getIndex()][section.getTime().getStartSlot() + i].indexOf(section));
						}
						int w =  174 / col + (index + 1 != col && col > 1 ? -3 : 0);
						int h = 125 * section.getTime().getLength() / 30 - 3;
						int l = 4 + 180 * dow.getIndex() + index * 174 / col;
						int t = 1 + 125 * section.getTime().getStartSlot() / 30 - 50 * firstHour;
						out.println("<div style='overflow-x: hidden; overflow-y: hidden; width: " + w + "px; height: " + h + "px; position: absolute; left: " + l + "px; top: " + t + "px; " +
								"position: absolute; font-size: x-small; font-family: arial; overflow: hidden; -webkit-border-radius: 6px; -moz-border-radius: 6px; color: #FFFFFF; " +
								"border: 1px solid #" + sColor1[color] + "; background: #" + sColor2[color] + ";'>");
						out.println("<table cellspacing='0' cellpadding='0' style='padding-left: 4px; padding-right: 4px; padding-bottom: 2px; padding-top: 2px; width: 100%; -webkit-border-top-left-radius: 5px; -webkit-border-top-right-radius: 5px; -moz-border-radius-topleft: 5px; -moz-border-radius-topright: 5px;" +
								"background: #" + sColor1[color] + ";'><tr><td align='left' style='vertical-align: top; '>");
						out.println("<div style='padding-left: 2px; width: 100%; font-size: x-small; white-space: nowrap; overflow: hidden; color: #FFFFFF;'>" +
								request.getAssignment().getCourse().getSubjectArea() + " " + 
								request.getAssignment().getCourse().getCourseNumber() + " " +
								section.getSubpart().getName() + "</div></td></tr></tbody></table>");
						out.println("<div style='font-size: x-small; padding-left: 4px; white-space: wrap; -webkit-border-bottom-left-radius: 5px; -webkit-border-bottom-right-radius: 5px; -moz-border-radius-bottomleft: 5px; -moz-border-radius-bottomright: 5px;'>");
						if (section.getRooms() != null)
							for (RoomLocation room: section.getRooms()) {
								out.println("<span style='white-space: nowrap'>" + room.getName() + ",</span>");
							}
						if (section.getChoice().getInstructorNames() != null && !section.getChoice().getInstructorNames().isEmpty()) {
							String[] instructors = section.getChoice().getInstructorNames().split(":");
							for (String instructor: instructors) {
								String[] nameEmail = instructor.split("\\|");
								out.println("<span style='white-space: nowrap'>" + nameEmail[0] + ",</span>");
							}
						}
						if (section.getTime().getDatePatternName() != null && !section.getTime().getDatePatternName().isEmpty()) {
							out.println("<span style='white-space: nowrap'>" + section.getTime().getDatePatternName() + "</span>");
						}
						out.println("</div></div>");
					}
				}
				color = (1 + color) % sColor1.length;
			}
		}
		
		out.println("</div></td></tr></table></div></td></tr>");
		
		/*
		out.println("	<tr><td style=\"font-size: 9pt; font-style: italic; color: #9CB0CE; text-align: right; margin-top: -2px; white-space: nowrap;\">");
		out.println("		If the timetable is not displayed correctly, please check out the attached file.");
		out.println("	</td></tr>");
		 */
		
		out.println("</table>");
	}
	
	public byte[] generateTimetableImage() throws IOException {
		int nrDays = 5, firstHour = 7, lastHour = 18;
		boolean hasSat = false, hasSun = false;
		List<Assignment> table[][] = new List[Constants.NR_DAYS][Constants.SLOTS_PER_DAY];
		for (Request request: getNewRequests()) {
			if (request.getAssignment() == null) continue;
			for (Assignment assignment: request.getAssignment().getAssignments()) {
				if (assignment.getTime() == null) continue;
				int dayCode = assignment.getTime().getDayCode();
				if ((dayCode & Constants.DAY_CODES[Constants.DAY_SAT]) != 0) hasSat = true;
				if ((dayCode & Constants.DAY_CODES[Constants.DAY_SUN]) != 0) hasSun = true;
				int startHour = (assignment.getTime().getStartSlot() * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN) / 60;
				if (startHour < firstHour) firstHour = startHour;
				int endHour = ((assignment.getTime().getStartSlot() + assignment.getTime().getLength()) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN + 59) / 60;
				if (endHour > lastHour) lastHour = endHour;
				for (Enumeration<Integer> e = assignment.getTime().getSlots(); e.hasMoreElements(); ) {
					int slot = e.nextElement();
					int day = slot / Constants.SLOTS_PER_DAY;
					int time = slot % Constants.SLOTS_PER_DAY;
					if (table[day][time] == null)
						table[day][time] = new ArrayList<Assignment>();
					table[day][time].add(assignment);
				}
			}
		}
		if (hasSat) nrDays = 6;
		if (hasSun) nrDays = 7;
		
        BufferedImage image = new BufferedImage(39 + 180 * nrDays, 21 + 50 * (lastHour - firstHour), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setFont(new Font("Sans Serif", Font.PLAIN, 11));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xff, 0xff, 0xff));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        
        int fh = g.getFontMetrics().getHeight();
        
        g.setColor(new Color(0x69, 0x91, 0xce));
		for (int i = 0; i < nrDays; i++) {
	        g.drawString(DayCode.values()[i].getName(), 40 + i * 180, 17);
		}
		
		for (int h = firstHour; h < lastHour; h++) {
			int top = 20 + 50 * (h - firstHour);
			g.drawString((h > 12 ? h - 12 : h) + (h < 12 ? "am" : "pm"), 2, top + fh);
		}
		
		g.setColor(new Color(0xff, 0xfd, 0xdd));
		g.fillRect(35, 20 + 25  + 50 * (7 - firstHour), 5 + 180 * nrDays, 501);
		
		Stroke noStroke = g.getStroke();
		Stroke dotted = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1f, new float[] {2f, 2f}, 0f);
		g.setColor(new Color(0xdd, 0xdd, 0xdd));
		for (int h = firstHour; h < lastHour; h++) {
			int top = 20 + 50 * (h - firstHour);
			g.setStroke(noStroke);
			g.drawLine(35, top, (39 + 180 * nrDays), top);
			g.setStroke(dotted);
			g.drawLine(35, top + 25, (39 + 180 * nrDays), top + 25);
		}
		g.setStroke(noStroke);
		g.drawLine(35, 20 + 50 * (lastHour - firstHour), (39 + 180 * nrDays), 20 + 50 * (lastHour - firstHour));
		
		g.setColor(new Color(0xff, 0xe1, 0xdd));
		for (Request request: getNewRequests()) {
			if (request instanceof FreeTimeRequest) {
				FreeTimeRequest fr = (FreeTimeRequest)request;
				for (DayCode dow: DayCode.toDayCodes(fr.getTime().getDayCode())) {
					g.fillRect(36 + 180 * dow.getIndex(), 21 + 125 * fr.getTime().getStartSlot() / 30 - 50 * firstHour, 182, 125 * fr.getTime().getLength() / 30 - 1);
				}
			}
		}

		g.setColor(new Color(0xdd, 0xdd, 0xdd));
		for (int i = 0; i <= nrDays; i++) {
			g.drawLine(35 + 180 * i, 20, 35 + 180 * i, 20 + 50 * (lastHour - firstHour));
			g.drawLine(38 + 180 * i, 20, 38 + 180 * i, 20 + 50 * (lastHour - firstHour));
		}
		
		g.setColor(new Color(0xba, 0x53, 0x53));
		for (Request request: getNewRequests()) {
			if (request instanceof FreeTimeRequest) {
				FreeTimeRequest fr = (FreeTimeRequest)request;
				for (DayCode dow: DayCode.toDayCodes(fr.getTime().getDayCode())) {
					g.drawString("Free " + DayCode.toString(fr.getTime().getDayCode()) + " " + fr.getTime().getStartTimeHeader() + " - " + fr.getTime().getEndTimeHeader(),
							42 + 180 * dow.getIndex(),
							20 + 125 * fr.getTime().getStartSlot() / 30 - 50 * firstHour + fh);
				}
			}
		}
		
		int color = 0;
		for (Request request: getNewRequests()) {
			if (request instanceof CourseRequest && request.getAssignment() != null) {
				for (Section section: request.getAssignment().getSections()) {
					if (section.getTime() == null) continue;
					for (DayCode dow: DayCode.toDayCodes(section.getTime().getDayCode())) {
						int col = 0;
						int index = 0;
						for (int i = 0; i < section.getTime().getLength(); i++) {
							col = Math.max(col, table[dow.getIndex()][section.getTime().getStartSlot() + i].size());
							index = Math.max(index, table[dow.getIndex()][section.getTime().getStartSlot() + i].indexOf(section));
						}
						int w = 176 / col + (index + 1 < col ? -2 : 0);
						int h = 125 * section.getTime().getLength() / 30 - 1;
						int l = 39 + 180 * dow.getIndex() + index * 174 / col;
						int t = 21 + 125 * section.getTime().getStartSlot() / 30 - 50 * firstHour;
						
						g.setColor(new Color(Integer.valueOf(sColor2[color], 16)));
						g.fillRoundRect(l, t, w, h, 6, 6);
						
						g.setColor(new Color(Integer.valueOf(sColor1[color], 16)));
						g.drawRoundRect(l, t, w, h, 6, 6);
						g.fillRoundRect(l, t, w, 2 + fh, 6, 6);
						g.fillRect(l, t + fh - 2, w, 4);
						
				        g.setColor(new Color(0xff, 0xff, 0xff));
				        String text = request.getAssignment().getCourse().getSubjectArea() + " " + 
							request.getAssignment().getCourse().getCourseNumber() + " " +
							section.getSubpart().getName();
				        while (g.getFontMetrics().stringWidth(text) > w - 10)
				        	text = text.substring(0, text.length() - 1);
				        g.drawString(text, l + 5, t + fh - 2);
				        
				        List<String> texts = new ArrayList<String>();
						if (section.getRooms() != null)
							for (RoomLocation room: section.getRooms())
								texts.add(room.getName());
						if (section.getChoice().getInstructorNames() != null && !section.getChoice().getInstructorNames().isEmpty()) {
							String[] instructors = section.getChoice().getInstructorNames().split(":");
							for (String instructor: instructors) {
								String[] nameEmail = instructor.split("\\|");
								texts.add(nameEmail[0]);
							}
						}
						if (section.getTime().getDatePatternName() != null && !section.getTime().getDatePatternName().isEmpty())
							texts.add(section.getTime().getDatePatternName());
						
						int tt = t + fh; 
						String next = "";
						int idx = 0;
						while (idx < texts.size()) {
							next += texts.get(idx);
							if (idx + 1 < texts.size()) next += ", ";
							while (g.getFontMetrics().stringWidth(next.trim()) < w - 10 && idx + 1 < texts.size()) {
								if (g.getFontMetrics().stringWidth(next + texts.get(idx + 1) + ",") < w - 10) {
									idx ++;
									next += texts.get(idx);
									if (idx + 1 < texts.size()) next += ", ";
								} else  break;
							}
							text = next; next = ""; idx ++;
					        while (g.getFontMetrics().stringWidth(text.trim()) > w - 10) {
					        	next = text.substring(text.length() - 1, text.length()) + next;
					        	text = text.substring(0, text.length() - 1);
							}
					        if (tt + fh - 2 > t + h) break;
					        g.drawString(text, l + 5, tt + fh - 2);
					        tt += fh;
						}
					}
				}
				color = (1 + color) % sColor1.length;
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		out.flush();out.close();
		return out.toByteArray();
	}
	
}
