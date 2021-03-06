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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentStatusType;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.PreferenceGroup;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningService;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.rights.Right;

public class CoursePermissions {
	
	@Service("permissionOfferingLockNeeded")
	public static class OfferingLockNeeded implements Permission<InstructionalOffering> {
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (source.isNotOffered()) return false;
			
			if (!permissionSession.check(
					user,
					source.getSession(),
					DepartmentStatusType.Status.StudentsAssistant, DepartmentStatusType.Status.StudentsOnline))
				return false;
			
			OnlineSectioningServer server = OnlineSectioningService.getInstance(user.getCurrentAcademicSessionId());
			
			return server != null && !server.isOfferingLocked(source.getUniqueId());
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@Service("permissionOfferingLockNeededLimitedEdit")
	public static class OfferingLockNeededLimitedEdit implements Permission<InstructionalOffering> {
		@Autowired PermissionSession permissionSession;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (source.isNotOffered()) return false;
			
			if (!permissionSession.check(
					user,
					source.getSession(),
					DepartmentStatusType.Status.StudentsOnline))
				return false;
			
			OnlineSectioningServer server = OnlineSectioningService.getInstance(user.getCurrentAcademicSessionId());
			
			return server != null && server.getAcademicSession().isSectioningEnabled() && !server.isOfferingLocked(source.getUniqueId());
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
	}
	
	@Service("permissionOfferingEdit")
	public static class OfferingEdit implements Permission<InstructionalOffering> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			// Owner can edit one of the course offerings
			for (CourseOffering course: source.getCourseOfferings()) {
				if (permissionDepartment.check(user, course.getDepartment(), DepartmentStatusType.Status.OwnerEdit))
					return true;
			}
			
			// Manager can edit external department
			Set<Department> externals = new HashSet<Department>();
			for (InstrOfferingConfig config: source.getInstrOfferingConfigs()) {
				for (SchedulingSubpart subpart: config.getSchedulingSubparts()) {
					for (Class_ clazz: subpart.getClasses()) {
						if (clazz.getManagingDept() != null && clazz.getManagingDept().isExternalManager()) {
							if (externals.add(clazz.getManagingDept()) &&
								permissionDepartment.check(user, clazz.getManagingDept(), DepartmentStatusType.Status.ManagerEdit))
								return true;
						}
					}
				}
			}
			
			return false;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.OfferingCanLock)
	public static class OfferingCanLock implements Permission<InstructionalOffering> {
		@Autowired Permission<InstructionalOffering> permissionOfferingEdit;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (!permissionOfferingLockNeeded.check(user, source))
				return false; // locking not need (e.g., bad status or already locked)
			
			if (!permissionOfferingEdit.check(user, source))
				return false; // user is not able to edit the offering -> no need to lock

			return true;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.OfferingCanUnlock)
	public static class OfferingCanUnlock implements Permission<InstructionalOffering> {
		@Autowired Permission<InstructionalOffering> permissionOfferingEdit;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (!permissionOfferingEdit.check(user, source))
				return false; // user is not able to edit the offering -> no need to lock

			OnlineSectioningServer server = OnlineSectioningService.getInstance(user.getCurrentAcademicSessionId());
			
			return user.getCurrentAuthority().hasRight(Right.OfferingCanUnlock) && server != null && server.isOfferingLocked(source.getUniqueId());
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}

	@PermissionForRight(Right.AddCourseOffering)
	public static class AddCourseOffering implements Permission<SubjectArea> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, SubjectArea source) {
			return permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit);
		}

		@Override
		public Class<SubjectArea> type() { return SubjectArea.class; }
		
	}
	
	@PermissionForRight(Right.InstructionalOfferingDetail)
	public static class InstructionalOfferingDetail implements Permission<InstructionalOffering> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			// Owner can view one of the course offerings
			for (CourseOffering course: source.getCourseOfferings()) {
				if (permissionDepartment.check(user, course.getDepartment(), DepartmentStatusType.Status.OwnerView))
					return true;
			}
			
			/*
			for (Department dept: source.getSession().getDepartments()) {
				if (dept.isExternalManager() && permissionDepartment.check(user, dept, DepartmentStatusType.Status.ManagerView))
					return true;
			}
			*/

			// Manager can view one of the classes
			Set<Department> externals = new HashSet<Department>();
			for (InstrOfferingConfig config: source.getInstrOfferingConfigs()) {
				for (SchedulingSubpart subpart: config.getSchedulingSubparts()) {
					for (Class_ clazz: subpart.getClasses()) {
						if (clazz.getManagingDept() != null && clazz.getManagingDept().isExternalManager()) {
							if (externals.add(clazz.getManagingDept()) &&
								permissionDepartment.check(user, clazz.getManagingDept(), DepartmentStatusType.Status.ManagerView))
								return true;
						}
					}
				}
			}
			
			return false;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.SchedulingSubpartDetail)
	public static class SchedulingSubpartDetail implements Permission<SchedulingSubpart> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, SchedulingSubpart source) {
			return permissionDepartment.check(user, source.getControllingDept(), DepartmentStatusType.Status.OwnerView,
					source.getManagingDept(), DepartmentStatusType.Status.ManagerView);
		}

		@Override
		public Class<SchedulingSubpart> type() { return SchedulingSubpart.class; }
	}
	
	@PermissionForRight(Right.ClassDetail)
	public static class ClassDetail implements Permission<Class_> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, Class_ source) {
			return permissionDepartment.check(user, source.getControllingDept(), DepartmentStatusType.Status.OwnerView,
					source.getManagingDept(), DepartmentStatusType.Status.ManagerView);
		}

		@Override
		public Class<Class_> type() { return Class_.class; }
	}
	
	@PermissionForRight(Right.ClassEdit)
	public static class ClassEdit implements Permission<Class_> {
		@Autowired PermissionDepartment permissionDepartment;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, Class_ source) {
			return !permissionOfferingLockNeeded.check(user, source.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering()) &&
					permissionDepartment.check(user, source.getControllingDept(), DepartmentStatusType.Status.OwnerEdit,
							source.getManagingDept(), DepartmentStatusType.Status.ManagerEdit);
		}

		@Override
		public Class<Class_> type() { return Class_.class; }
	}
	
	@PermissionForRight(Right.SchedulingSubpartEdit)
	public static class SchedulingSubpartEdit implements Permission<SchedulingSubpart> {
		@Autowired PermissionDepartment permissionDepartment;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, SchedulingSubpart source) {
			return !permissionOfferingLockNeeded.check(user, source.getInstrOfferingConfig().getInstructionalOffering()) &&
					permissionDepartment.check(user, source.getControllingDept(), DepartmentStatusType.Status.OwnerEdit,
							source.getManagingDept(), DepartmentStatusType.Status.ManagerEdit);
		}

		@Override
		public Class<SchedulingSubpart> type() { return SchedulingSubpart.class; }
	}

	@PermissionForRight(Right.MultipleClassSetup)
	public static class MultipleClassSetup implements Permission<InstrOfferingConfig> {
		@Autowired PermissionDepartment permissionDepartment;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, InstrOfferingConfig source) {
			if (source.getInstructionalOffering().isNotOffered()) return false;
			
			if (permissionOfferingLockNeeded.check(user, source.getInstructionalOffering())) return false;
			
			if (permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit))
				return true;
			
			// Manager can edit external department
			Set<Department> externals = new HashSet<Department>();
			for (SchedulingSubpart subpart: source.getSchedulingSubparts()) {
				for (Class_ clazz: subpart.getClasses()) {
					if (clazz.getManagingDept() != null && clazz.getManagingDept().isExternalManager()) {
						if (externals.add(clazz.getManagingDept()) &&
							permissionDepartment.check(user, clazz.getManagingDept(), DepartmentStatusType.Status.ManagerEdit))
							return true;
					}
				}
			}
			
			return false;
		}

		@Override
		public Class<InstrOfferingConfig> type() { return InstrOfferingConfig.class; }
	}
	
	@PermissionForRight(Right.InstrOfferingConfigEdit)
	public static class InstrOfferingConfigEdit extends MultipleClassSetup {
	}
	
	@PermissionForRight(Right.InstrOfferingConfigAdd)
	public static class InstrOfferingConfigAdd implements Permission<InstructionalOffering> {
		@Autowired PermissionDepartment permissionDepartment;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (permissionOfferingLockNeeded.check(user, source)) return false;
			
			if (source.isNotOffered()) return false;
			
			if (permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit)) return true;
			
			return false;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.InstrOfferingConfigDelete)
	public static class InstrOfferingConfigDelete implements Permission<InstrOfferingConfig> {
		@Autowired PermissionDepartment permissionDepartment;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, InstrOfferingConfig source) {
			if (source.getInstructionalOffering().isNotOffered()) return false;
			
			if (source.getInstructionalOffering().getInstrOfferingConfigs().size() <= 1) return false;

			if (permissionOfferingLockNeeded.check(user, source.getInstructionalOffering())) return false;
			
			if (source.getInstructionalOffering().isNotOffered()) return false;
			
			// Manager can edit external department
			Set<Department> externals = new HashSet<Department>();
			for (SchedulingSubpart subpart: source.getSchedulingSubparts()) {
				for (Class_ clazz: subpart.getClasses()) {
					if (clazz.getManagingDept() != null && clazz.getManagingDept().isExternalManager()) {
						if (externals.add(clazz.getManagingDept()) &&
							!permissionDepartment.check(user, clazz.getManagingDept(), DepartmentStatusType.Status.ManagerEdit) &&
							!clazz.getManagingDept().effectiveStatusType().can(DepartmentStatusType.Status.OwnerEdit))
							return false;
					}
				}
			}
			
			return permissionDepartment.check(user, source.getInstructionalOffering().getDepartment(), DepartmentStatusType.Status.OwnerEdit);
		}

		@Override
		public Class<InstrOfferingConfig> type() { return InstrOfferingConfig.class; }
		
	}

	@PermissionForRight(Right.InstrOfferingConfigEditDepartment)
	public static class InstrOfferingConfigEditDepartment implements Permission<Department> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, Department source) {
			if (source.isExternalManager() && permissionDepartment.check(user, source, DepartmentStatusType.Status.ManagerEdit))
				return true;
			
			if (!source.isExternalManager() && permissionDepartment.check(user, source, DepartmentStatusType.Status.OwnerEdit))
				return true;
			
			if (source.isExternalManager() && source.effectiveStatusType().can(DepartmentStatusType.Status.OwnerEdit))
				return true;
			
			return false;
		}

		@Override
		public Class<Department> type() { return Department.class; }
		
	}
	
	@PermissionForRight(Right.InstrOfferingConfigEditSubpart)
	public static class InstrOfferingConfigEditSubpart extends SchedulingSubpartEdit {}
	
	@PermissionForRight(Right.InstructionalOfferingCrossLists)
	public static class InstructionalOfferingCrossLists implements Permission<InstructionalOffering> {
		@Autowired PermissionDepartment permissionDepartment;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (permissionOfferingLockNeeded.check(user, source)) return false;
			
			if (permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit)) return true;
			
			return false;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.MultipleClassSetupDepartment)
	public static class MultipleClassSetupDepartment extends InstrOfferingConfigEditDepartment {}
	
	@PermissionForRight(Right.MultipleClassSetupClass)
	public static class MultipleClassSetupClassEdit extends ClassEdit {}

	@PermissionForRight(Right.OfferingMakeOffered)
	public static class OfferingMakeOffered implements Permission<InstructionalOffering> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (!source.isNotOffered()) return false;
			
			if (permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit)) return true;
			
			return false;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.OfferingMakeNotOffered)
	public static class OfferingMakeNotOffered implements Permission<InstructionalOffering> {
		@Autowired PermissionDepartment permissionDepartment;
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (permissionOfferingLockNeeded.check(user, source)) return false;
			
			if (source.isNotOffered()) return false;
			
			if (permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit)) return true;
			
			return false;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.OfferingDelete)
	public static class OfferingDelete implements Permission<InstructionalOffering> {
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, InstructionalOffering source) {
			if (!source.isNotOffered()) return false;
			
			if (permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit)) return true;
			
			return false;
		}

		@Override
		public Class<InstructionalOffering> type() { return InstructionalOffering.class; }
		
	}
	
	@PermissionForRight(Right.EditCourseOffering) 
	public static class EditCourseOffering implements Permission<CourseOffering> {
		@Autowired Permission<InstructionalOffering> permissionOfferingLockNeeded;
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, CourseOffering source) {
			if (permissionOfferingLockNeeded.check(user, source.getInstructionalOffering())) return false;

			return permissionDepartment.check(user, source.getDepartment(), DepartmentStatusType.Status.OwnerEdit);
		}

		@Override
		public Class<CourseOffering> type() { return CourseOffering.class; }
		
	}
	
	@PermissionForRight(Right.CanUseHardPeriodPrefs)
	public static class CanUseHardPeriodPrefs implements Permission<PreferenceGroup> {

		@Autowired PermissionDepartment permissionDepartment;
		
		@Override
		public boolean check(UserContext user, PreferenceGroup source) {
			return user.getCurrentAuthority().hasRight(Right.DepartmentIndependent);
		}

		@Override
		public Class<PreferenceGroup> type() { return PreferenceGroup.class; }
	}
	
	@PermissionForRight(Right.CanUseHardTimePrefs)
	public static class CanUseHardTimePrefs implements Permission<PreferenceGroup> {

		@Autowired PermissionDepartment permissionDepartment;
		
		@Override
		public boolean check(UserContext user, PreferenceGroup source) {
			if (user.getCurrentAuthority().hasRight(Right.DepartmentIndependent) || source.getDepartment() == null) return true;
			
			if (Boolean.FALSE.equals(source.getDepartment().getAllowReqTime())) return false;
			
			return permissionDepartment.check(user, source.getDepartment());
		}

		@Override
		public Class<PreferenceGroup> type() { return PreferenceGroup.class; }
	}
	
	@PermissionForRight(Right.CanUseHardRoomPrefs)
	public static class CanUseHardRoomPrefs implements Permission<PreferenceGroup> {

		@Autowired PermissionDepartment permissionDepartment;
		
		@Override
		public boolean check(UserContext user, PreferenceGroup source) {
			if (user.getCurrentAuthority().hasRight(Right.DepartmentIndependent) || source.getDepartment() == null) return true;
			
			if (Boolean.FALSE.equals(source.getDepartment().getAllowReqRoom())) return false;
			
			return permissionDepartment.check(user, source.getDepartment());
		}

		@Override
		public Class<PreferenceGroup> type() { return PreferenceGroup.class; }
	}
	
	@PermissionForRight(Right.CanUseHardDistributionPrefs)
	public static class CanUseHardDistributionPrefs implements Permission<PreferenceGroup> {

		@Autowired PermissionDepartment permissionDepartment;
		
		@Override
		public boolean check(UserContext user, PreferenceGroup source) {
			if (user.getCurrentAuthority().hasRight(Right.DepartmentIndependent) || source.getDepartment() == null) return true;
			
			if (Boolean.FALSE.equals(source.getDepartment().getAllowReqDistribution())) return false;
			
			return permissionDepartment.check(user, source.getDepartment());
		}

		@Override
		public Class<PreferenceGroup> type() { return PreferenceGroup.class; }
	}
		
	@PermissionForRight(Right.InstructionalOfferings)
	public static class InstructionalOfferings implements Permission<Department> {
		
		@Autowired PermissionDepartment permissionDepartment;

		@Override
		public boolean check(UserContext user, Department source) {
			return permissionDepartment.check(user, source, DepartmentStatusType.Status.OwnerView, DepartmentStatusType.Status.ManagerView);
		}

		@Override
		public Class<Department> type() { return Department.class; }
		
	}
	
	@PermissionForRight(Right.InstructionalOfferingsExportPDF)
	public static class InstructionalOfferingsExportPDF extends InstructionalOfferings {}

	@PermissionForRight(Right.InstructionalOfferingsWorksheetPDF)
	public static class InstructionalOfferingsWorksheetPDF extends InstructionalOfferings {}

	@PermissionForRight(Right.Classes)
	public static class Classes extends InstructionalOfferings {}

	@PermissionForRight(Right.ClassesExportPDF)
	public static class ClassesExportPDF extends InstructionalOfferings {}
			
	@PermissionForRight(Right.DistributionPreferenceClass)
	public static class DistributionPreferenceClass extends ClassEdit {}
	
	@PermissionForRight(Right.ClassEditClearPreferences)
	public static class ClassEditClearPreferences extends ClassEdit {}
	
	@PermissionForRight(Right.DistributionPreferenceSubpart)
	public static class DistributionPreferenceSubpart extends SchedulingSubpartEdit {}
	
	@PermissionForRight(Right.SchedulingSubpartDetailClearClassPreferences)
	public static class SchedulingSubpartDetailClearClassPreferences extends SchedulingSubpartEdit {}
	
	@PermissionForRight(Right.SchedulingSubpartEditClearPreferences)
	public static class SchedulingSubpartEditClearPreferences extends SchedulingSubpartEdit {}
	
}
