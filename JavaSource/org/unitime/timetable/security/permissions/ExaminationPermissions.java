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
package org.unitime.timetable.security.permissions;

import org.springframework.beans.factory.annotation.Autowired;
import org.unitime.timetable.model.DepartmentStatusType;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.rights.Right;

public class ExaminationPermissions {

	@PermissionForRight(Right.Examinations)
	public static class Examinations implements Permission<Session> {
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, Session source) {
			if (user.getCurrentAuthority().hasRight(Right.DepartmentIndependent))
				return permissionSession.check(user, source, DepartmentStatusType.Status.ExamTimetable);
			else
				return permissionSession.check(user, source, DepartmentStatusType.Status.ExamView);
		}

		@Override
		public Class<Session> type() { return Session.class; }
		
	}
	
	@PermissionForRight(Right.ExaminationDetail)
	public static class ExaminationDetail implements Permission<Exam> {
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, Exam source) {
			if (user.getCurrentAuthority().hasRight(Right.DepartmentIndependent))
				return permissionSession.check(user, source.getSession(), DepartmentStatusType.Status.ExamTimetable);
			else
				return permissionSession.check(user, source.getSession(), DepartmentStatusType.Status.ExamView);
		}

		@Override
		public Class<Exam> type() { return Exam.class; }
		
	}
	
	@PermissionForRight(Right.ExaminationAdd)
	public static class AddExamination implements Permission<Session> {
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, Session source) {
			if (user.getCurrentAuthority().hasRight(Right.DepartmentIndependent))
				return permissionSession.check(user, source, DepartmentStatusType.Status.ExamTimetable);
			else
				return permissionSession.check(user, source, DepartmentStatusType.Status.ExamEdit);
		}

		@Override
		public Class<Session> type() { return Session.class; }
		
	}
	
	@PermissionForRight(Right.ExaminationEdit)
	public static class EditExamination implements Permission<Exam> {
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, Exam source) {
			if (user.getCurrentAuthority().hasRight(Right.DepartmentIndependent))
				return permissionSession.check(user, source.getSession(), DepartmentStatusType.Status.ExamTimetable);
			else
				return permissionSession.check(user, source.getSession(), DepartmentStatusType.Status.ExamEdit);
		}

		@Override
		public Class<Exam> type() { return Exam.class; }
		
	}
	
	@PermissionForRight(Right.ExaminationDelete)
	public static class ExaminationDelete extends EditExamination{}
	
	@PermissionForRight(Right.ExaminationClone)
	public static class ExaminationClone extends EditExamination{}

	@PermissionForRight(Right.DistributionPreferenceExam)
	public static class DistributionPreferenceExam extends EditExamination{}
	
	@PermissionForRight(Right.ExaminationEditClearPreferences)
	public static class ExaminationEditClearPreferences extends EditExamination{}
	
	@PermissionForRight(Right.ExaminationAssignment)
	public static class ExaminationAssignment implements Permission<Exam> {
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, Exam source) {
			return user.getCurrentAuthority().hasRight(Right.DepartmentIndependent) &&
				 permissionSession.check(user, source.getSession(), DepartmentStatusType.Status.ExamTimetable);
		}

		@Override
		public Class<Exam> type() { return Exam.class; }
		
	}

	@PermissionForRight(Right.ExaminationSchedule)
	public static class ExaminationSchedule implements Permission<Session> {
		
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, Session source) {
			return permissionSession.check(user, source, DepartmentStatusType.Status.ReportExamsFinal, DepartmentStatusType.Status.ReportExamsMidterm);
		}

		@Override
		public Class<Session> type() { return Session.class; }
		
	}
}
