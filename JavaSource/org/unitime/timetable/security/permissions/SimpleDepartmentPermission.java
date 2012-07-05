package org.unitime.timetable.security.permissions;

import org.springframework.stereotype.Service;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentStatusType;
import org.unitime.timetable.security.UserAuthority;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.permissions.Permission.PermissionDepartment;
import org.unitime.timetable.security.rights.Right;

@Service("permissionDepartment")
public class SimpleDepartmentPermission implements PermissionDepartment {

	@Override
	public boolean check(UserContext user, Department department) {
		return check(user, department, new DepartmentStatusType.Status[] {}) && checkStatus(department.effectiveStatusType());
	}
	
	@Override
	public boolean check(UserContext user, Department department, DepartmentStatusType.Status... status) {
		// Not authenticated or no authority -> no permission
		if (user == null || user.getCurrentAuthority() == null || department == null) return false;
		
		UserAuthority authority = user.getCurrentAuthority();
		
		// Academic session check
		if (!authority.hasRight(Right.SessionIndependent) && !authority.hasQualifier(department.getSession()))
			return false;
		
		// Department check
		if (!authority.hasRight(Right.DepartmentIndependent) && !authority.hasQualifier(department))
			return false;

		// Check department status
		if (status.length > 0 && !authority.hasRight(Right.StatusIndependent)) {
			DepartmentStatusType type = department.effectiveStatusType();
			if (type == null) return false;
			for (DepartmentStatusType.Status s: status) {
				if (type.can(s)) return true;
			}
			return false;
		}
		
		return true;
	}

	@Override
	public Class<Department> type() {
		return Department.class;
	}
	
	public boolean checkStatus(DepartmentStatusType status) { return true; }
}
