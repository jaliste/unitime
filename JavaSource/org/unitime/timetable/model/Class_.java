/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008-2009, UniTime LLC, and individual contributors
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
package org.unitime.timetable.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.cpsolver.coursett.preference.MinMaxPreferenceCombination;
import net.sf.cpsolver.coursett.preference.PreferenceCombination;

import org.hibernate.FlushMode;
import org.hibernate.Transaction;
import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.interfaces.ExternalClassEditAction;
import org.unitime.timetable.interfaces.ExternalClassNameHelperInterface;
import org.unitime.timetable.model.base.BaseClass_;
import org.unitime.timetable.model.comparators.InstructorComparator;
import org.unitime.timetable.model.comparators.NavigationComparator;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.model.dao.DatePatternDAO;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.model.dao.SectioningInfoDAO;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.solver.ClassAssignmentProxy;
import org.unitime.timetable.solver.CommitedClassAssignmentProxy;
import org.unitime.timetable.solver.course.ui.ClassAssignmentInfo;
import org.unitime.timetable.solver.course.ui.ClassInstructorInfo;
import org.unitime.timetable.solver.course.ui.ClassRoomInfo;
import org.unitime.timetable.solver.ui.AssignmentPreferenceInfo;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.DefaultExternalClassNameHelper;
import org.unitime.timetable.webutil.Navigation;


public class Class_ extends BaseClass_ {
    private static final long serialVersionUID = 1L;
    private static ExternalClassNameHelperInterface externalClassNameHelper = null;

	/* [CONSTRUCTOR MARKER BEGIN] */
	public Class_ () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Class_ (java.lang.Long uniqueId) {
		super(uniqueId);
	}

    /* [CONSTRUCTOR MARKER END] */

    public String getCourseName(){
		return getSchedulingSubpart().getCourseName();
	}

    public String getCourseNameWithTitle(){
		return getSchedulingSubpart().getCourseNameWithTitle();
	}

    /**
     *
     * @return
     */
    public String getItypeDesc() {
   		return getSchedulingSubpart().getItypeDesc();
    }

    public Department getManagingDept(){
    	if (super.getManagingDept()==null) return getControllingDept();
        return super.getManagingDept();
    }

    public void setManagingDept(Department dept) {
        Department oldDept = getManagingDept();
        super.setManagingDept(dept);
        if (dept==null) return;
        if (oldDept!=null && !oldDept.equals(dept) && getAssignments()!=null && !getAssignments().isEmpty()) {
            for (Iterator i=getAssignments().iterator();i.hasNext();) {
                Assignment a = (Assignment)i.next();
                if (!a.getSolution().getOwner().getDepartments().contains(dept)) {
                	Class_DAO.getInstance().getSession().delete(a);
                	i.remove();
                }
            }
            ClassEvent event = getEvent();
            if (event!=null) Class_DAO.getInstance().getSession().delete(event);
        }
    }

    /**
     * Retrieves the department for the subject area of the
     * controlling course offering for the class
     * @return Department object
     */
    public Department getDepartmentForSubjectArea() {
        Department dept = this.getSchedulingSubpart()
			        		.getInstrOfferingConfig()
			        		.getControllingCourseOffering()
			        		.getSubjectArea()
			        		.getDepartment();
        return dept;
    }

    public Session getSession(){
    	return (this.getSchedulingSubpart().getSession());
    }

	public Long getSessionId() {
		return (this.getSchedulingSubpart().getSessionId());
	}

    private Set classInstructorPrefsOfType(Class type) {
    	List<DepartmentalInstructor> instructors = getLeadInstructors();
    	if (instructors.isEmpty()) return null;
    	Set ret = null;
    	for (DepartmentalInstructor instructor: instructors) {
    		if (ret == null)
    			ret = instructor.getPreferences(type);
    		else
    			ret = combinePreferences(ret, instructor.getPreferences(type));
    	}
    	return ret;
    }

    private Set weakenHardPreferences(Set prefs) {
    	if (prefs==null || prefs.isEmpty()) return prefs;
    	Set ret = new TreeSet();
		for (Iterator i=prefs.iterator();i.hasNext();) {
			Preference pref = (Preference)((Preference)i.next()).clone();
			if (pref.weakenHardPreferences())
				ret.add(pref);
		}
    	return ret;
    }

    private Set removeNeutralPreferences(Set prefs) {
    	if (prefs==null) return new TreeSet();
    	if (prefs.isEmpty()) return prefs;
    	Set ret = new TreeSet(prefs);
		for (Iterator i=ret.iterator();i.hasNext();) {
			Preference pref = (Preference)i.next();
			if (PreferenceLevel.sNeutral.equals(pref.getPrefLevel().getPrefProlog())) i.remove();
		}
		return ret;
    }

    private Set<Preference> combinePreferences(Set<Preference> instrPrefs1, Set<Preference> instrPrefs2) {
    	if (instrPrefs1==null || instrPrefs1.isEmpty()) return instrPrefs2;
    	if (instrPrefs2==null || instrPrefs2.isEmpty()) return instrPrefs1;

    	Set<Preference> ret = new TreeSet<Preference>();

    	TimePref tp = null;
    	for (Iterator<Preference> i=instrPrefs1.iterator();i.hasNext();) {
    		Preference p1 = i.next();
    		if (p1 instanceof TimePref) {
    			if (tp==null) {
    				tp = (TimePref)p1.clone();
    			} else tp.combineWith((TimePref)p1,false);
    		} else ret.add(p1);
    	}
    	for (Iterator<Preference> i=instrPrefs2.iterator();i.hasNext();) {
    		Preference p2 = i.next();
    		if (p2 instanceof TimePref) {
    			if (tp==null) {
    				tp = (TimePref)p2.clone();
    			} else tp.combineWith((TimePref)p2,false);
    		}
    	}

    	for (Iterator<Preference> i=instrPrefs2.iterator();i.hasNext();) {
    		Preference p2 = i.next();
    		Preference p1 = null;
			for (Iterator<Preference> j=ret.iterator();j.hasNext();) {
				Preference p = j.next();
				if (p.isSame(p2)) {
					p1 = p; j.remove(); break;
				}
			}
			if (p1==null) {
				ret.add(p2);
			} else {
				Preference combPref = (Preference)p1.clone();
				PreferenceCombination com = new MinMaxPreferenceCombination();
				com.addPreferenceProlog(p1.getPrefLevel().getPrefProlog());
				com.addPreferenceProlog(p2.getPrefLevel().getPrefProlog());
				combPref.setPrefLevel(PreferenceLevel.getPreferenceLevel(com.getPreferenceProlog()));
				ret.add(combPref);
			}
		}

    	if (tp!=null) ret.add(tp);

    	return ret;
    }

    private Set combinePreferences(Class type, Set subpartPrefs, Set instrPrefs) {
		if (TimePref.class.equals(type)) {
			if (subpartPrefs==null || subpartPrefs.isEmpty() || instrPrefs==null || instrPrefs.isEmpty()) return subpartPrefs;
			TimePref instrPref = (TimePref)instrPrefs.iterator().next(); //there has to be only one TimePref for instructor/department/session

			Set ret = new TreeSet();
			for (Iterator i=subpartPrefs.iterator();i.hasNext();) {
				TimePref pref = (TimePref)((TimePref)i.next()).clone();
				pref.combineWith(instrPref, false);
				ret.add(pref);
			}

			return ret;
		}

		if (subpartPrefs==null || subpartPrefs.isEmpty()) return instrPrefs;
		if (instrPrefs==null || instrPrefs.isEmpty()) return subpartPrefs;

		Set ret = new TreeSet(subpartPrefs);
		for (Iterator i=instrPrefs.iterator();i.hasNext();) {
			Preference instrPref = (Preference)i.next();
			Preference subpartPref = null;
			for (Iterator j=ret.iterator();j.hasNext();) {
				Preference p = (Preference)j.next();
				if (p.isSame(instrPref)) {
					subpartPref = p; j.remove(); break;
				}
			}
			if (subpartPref==null) {
				ret.add(instrPref);
			} else {
				Preference combPref = (Preference)subpartPref.clone();
				PreferenceCombination com = new MinMaxPreferenceCombination();
				com.addPreferenceProlog(instrPref.getPrefLevel().getPrefProlog());
				com.addPreferenceProlog(subpartPref.getPrefLevel().getPrefProlog());
				combPref.setPrefLevel(PreferenceLevel.getPreferenceLevel(com.getPreferenceProlog()));
				ret.add(combPref);
			}
		}
		return ret;
    }

    private Set combinePreferences(Class type, Set classPrefs, Set subpartPrefs, Set instrPrefs) {
		Set ret = new TreeSet(classPrefs);
		Set combined = combinePreferences(type, subpartPrefs, instrPrefs);

		if (combined==null) return ret;

		for (Iterator i=combined.iterator();i.hasNext();) {
			Preference combPref = (Preference)i.next();
			Preference classPref = null;
			for (Iterator j=classPrefs.iterator();j.hasNext();) {
				Preference p = (Preference)j.next();
				if (p.isSame(combPref)) {
					classPref = p; break;
				}
			}
			if (classPref==null) ret.add(combPref);
		}

		return ret;
    }

    public Set getDistributionPreferences() {
    	TreeSet prefs = new TreeSet();
    	if (getDistributionObjects()!=null) {
    		for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
    			DistributionObject distObj = (DistributionObject)i.next();
    				prefs.add(distObj.getDistributionPref());
    		}
    	}
    	return prefs;
    }

    public Set effectiveDistributionPreferences(Department owningDept) {
    	TreeSet prefs = new TreeSet();
    	if (getDistributionObjects()!=null) {
    		for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
    			DistributionObject distObj = (DistributionObject)i.next();
    			DistributionPref pref = distObj.getDistributionPref();
    			if (owningDept==null || owningDept.equals(pref.getOwner()))
    				prefs.add(pref);
    		}
    	}
    	if (getSchedulingSubpart().getDistributionObjects()!=null) {
    		for (Iterator i=getSchedulingSubpart().getDistributionObjects().iterator();i.hasNext();) {
    			DistributionObject distObj = (DistributionObject)i.next();
    			DistributionPref pref = distObj.getDistributionPref();
    			if (owningDept==null || owningDept.equals(pref.getOwner()))
    				prefs.add(pref);
    		}
    	}
    	return prefs;
    }

    public Set effectivePreferences(Class type, Vector leadInstructors) {
    	Department mngDept = getManagingDept();
    	if (DistributionPref.class.equals(type)) {
    		return effectiveDistributionPreferences(mngDept);
    		//return (mngDept==null?null:mngDept.getPreferences(type, this));
    	}

    	if (leadInstructors==null || leadInstructors.isEmpty()) return effectivePreferences(type);

    	Set instrPrefs = null;
    	for (Enumeration e=leadInstructors.elements();e.hasMoreElements();) {
    		DepartmentalInstructor leadInstructor = (DepartmentalInstructor)e.nextElement();
    		if (!mngDept.isExternalManager().booleanValue()) { // departmental class -> take instructor preferences as they are
    			instrPrefs = combinePreferences(instrPrefs,leadInstructor.prefsOfTypeForDepartment(type, getControllingDept()));
    		} else {
    			//LLR/LAB class take weaken form of time instructor preferences
    			if (TimePref.class.equals(type))
    				instrPrefs = combinePreferences(instrPrefs,weakenHardPreferences(leadInstructor.prefsOfTypeForDepartment(type, getControllingDept())));
    		}
    	}

    	if (getSchedulingSubpart().getManagingDept().getUniqueId().equals(mngDept.getUniqueId())) {
    		//subpart of the same owner -> take subpart preferences
    		Set subpartPrefs = getSchedulingSubpart().getPreferences(type);
    		return removeNeutralPreferences(combinePreferences(type, subpartPrefs, instrPrefs));
    	} else {
    		//subpart of different owner -> take only time pattern
    		Set subpartPrefs = new TreeSet();
    		if (TimePref.class.equals(type)) {
    			Set subpartTimePrefs = getSchedulingSubpart().getPreferences(type);
    			if (subpartTimePrefs!=null) {
    				for (Iterator i=subpartTimePrefs.iterator();i.hasNext();) {
    					TimePref tp = (TimePref)((TimePref)i.next()).clone();
    					TimePatternModel m = tp.getTimePatternModel();
    					if (mngDept.isExternalManager().booleanValue())
    						m.weakenHardPreferences();
    					tp.setTimePatternModel(m);
    					subpartPrefs.add(tp);
    				}
    			}
    		}
    		return removeNeutralPreferences(combinePreferences(type, subpartPrefs, instrPrefs));
    	}

    }

    public Set effectivePreferences(Class type) {
    	Department mngDept = getManagingDept();
    	// special handling of distribution preferences
    	if (DistributionPref.class.equals(type)) {
    		Set prefs = effectiveDistributionPreferences(mngDept);
    			//(mngDept==null?new TreeSet():mngDept.getPreferences(type, this));
    		if (!mngDept.isExternalManager().booleanValue()) {
    			Set instPref = classInstructorPrefsOfType(type);
    			if (instPref!=null) prefs.addAll(instPref);
    		}
    		return prefs;
    	}

    	Set classPrefs = getPreferences(type, this);

    	Set instrPrefs = null;
    	if (mngDept == null || !mngDept.isExternalManager().booleanValue()) { // departmental class -> take instructor preferences
    		instrPrefs = classInstructorPrefsOfType(type);
        	if (instrPrefs==null || instrPrefs.isEmpty()){
        		if (!RoomPref.class.equals(type)  && getManagingDept() != null){ //Department Room Prefs are not used in this way
        			instrPrefs = getManagingDept().getPreferences(type); //take department preference if there is no instructor pref
        		}
        	}
        	if (instrPrefs==null || instrPrefs.isEmpty())
        		instrPrefs = getSession().getPreferences(type); // get session preference if there is no instructor or dept. pref
    	}

    	boolean hasExactTimePattern = false;
		if (TimePref.class.equals(type)) {
    		if (classPrefs!=null && !classPrefs.isEmpty()) {
    			for (Iterator i=classPrefs.iterator();i.hasNext();) {
    				TimePref tp = (TimePref)i.next();
    				if (tp.getTimePattern()!=null && tp.getTimePattern().getType().intValue()==TimePattern.sTypeExactTime) {
    					hasExactTimePattern = true; break;
    				}
    			}
    		}
		}

    	if (mngDept!=null && mngDept.getUniqueId().equals(getSchedulingSubpart().getManagingDept().getUniqueId())) {
    		//subpart with the same owner -> take subpart preferences
        	Set subpartPrefs = (hasExactTimePattern?null:getSchedulingSubpart().getPreferences(type, this));

        	return removeNeutralPreferences(combinePreferences(type, classPrefs, subpartPrefs, instrPrefs));
    	} else {
    		//subpart with different owner -> take only time pattern from subpart preferences
    		if (TimePref.class.equals(type)) {
    			Set subpartPrefs = (hasExactTimePattern?null:getSchedulingSubpart().getPreferences(type));
    			Set clearedSubpartPrefs = new TreeSet();
    			if (subpartPrefs!=null) {
    				for (Iterator i=subpartPrefs.iterator();i.hasNext();) {
    					TimePref tp = (TimePref)((TimePref)i.next()).clone();
    					TimePatternModel m = tp.getTimePatternModel();
    					if (mngDept.isExternalManager().booleanValue())
    						m.weakenHardPreferences();
    					tp.setTimePatternModel(m);
    					clearedSubpartPrefs.add(tp);
    				}
    			}
    			return removeNeutralPreferences(combinePreferences(type, classPrefs, clearedSubpartPrefs, instrPrefs));
    		} else
    			return removeNeutralPreferences(classPrefs);
    	}
    }

    public String instructorHtml(String instructorNameFormat){
    	StringBuffer sb = new StringBuffer();
    	if (this.getClassInstructors()==null) return "";
    	TreeSet sortedInstructors = new TreeSet(new InstructorComparator());
    	sortedInstructors.addAll(this.getClassInstructors());

    	Iterator it = sortedInstructors.iterator();
    	ClassInstructor ci = null;
    	while (it.hasNext()){
    		ci = (ClassInstructor) it.next();
    		String title = ci.getInstructor().getNameLastFirst();
    		title += " ("+ci.getPercentShare()+"%"+(ci.isLead().booleanValue()?", lead":"")+")";
    		if (!isDisplayInstructor().booleanValue()){
    			title += " - Do Not Display Instructor.";
    		}
    		if (ci.isLead().booleanValue()){
    			sb.append("<span style='font-weight:bold;"+(isDisplayInstructor().booleanValue()?"":"font-style:italic;")+"' title='"+title+"'>");
    		} else {
    			sb.append("<span title='"+title+"'>");
    		}
    		sb.append(ci.getInstructor().getName(instructorNameFormat));
    		sb.append("</span>");
    		if (it.hasNext()) sb.append("<br>");
    		sb.append("\n");
    	}
    	return (sb.toString());
    }


	private String htmlForTimePatterns(Set patterns){
		StringBuffer sb = new StringBuffer();
		if (patterns != null){
			Iterator it = patterns.iterator();
			TimePattern t = null;
			while (it.hasNext()){
				t = (TimePattern) it.next();
				sb.append(t.getName());
				if (it.hasNext()) {
					sb.append("<BR>");
				}
			}
		}
        if (patterns == null || patterns.isEmpty()) {
            if (getSchedulingSubpart().getMinutesPerWk().intValue()<=0) {
                sb.append("<span title='Arrange Hours'>Arr Hrs</span>");
            } else {
                int nrHours = Math.round(getSchedulingSubpart().getMinutesPerWk().intValue()/50.0f);
                sb.append("<span title='Arrange "+nrHours+" Hours'>Arr "+nrHours+" Hrs</span>");
            }
        }
		return(sb.toString());
	}

	public String effectiveTimePatternHtml(){
		return(htmlForTimePatterns(this.effectiveTimePatterns()));
	}

	public String timePatternHtml(){
		return(htmlForTimePatterns(this.getTimePatterns()));
	}

	public Integer getSectionNumber() {
		return getSectionNumber(null, true);
	}
	
	public Integer getSectionNumber(org.hibernate.Session hibSession) {
		return getSectionNumber(hibSession, true);
	}

	public Integer getSectionNumber(boolean save) {
		return getSectionNumber(null, save);
	}

    public Integer getSectionNumber(org.hibernate.Session hibSession, boolean save) {
    	Integer sectionNumber = getSectionNumberCache();
    	if (sectionNumber!=null) return sectionNumber;

    	Comparator cmp = new Comparator() {
    		public int compare(Object o1, Object o2) {
    			Class_ c1 = (Class_)o1;
    			Class_ c2 = (Class_)o2;
    			if (c1.getParentClass()==null || c2.getParentClass()==null || c1.getParentClass().equals(c2.getParentClass()))
    				return c1.getUniqueId().compareTo(c2.getUniqueId());
    			else
    				return compare(c1.getParentClass(),c2.getParentClass());
    		}
    	};

    	int idx = 0;
    	for (Iterator i=getSchedulingSubpart().getClasses().iterator();i.hasNext();) {
    		Class_ clazz = (Class_)i.next();
    		if (cmp.compare(clazz, this)<0) idx++;
    	}

    	sectionNumber = new Integer(getSchedulingSubpart().getInstrOfferingConfig().getFirstSectionNumber(getSchedulingSubpart().getItype())+idx);
		setSectionNumberCache(sectionNumber);

    	if (save) {
    		if (hibSession != null) {
    			hibSession.saveOrUpdate(this);
    		} else {
        		(new Class_DAO()).getSession().saveOrUpdate(this);
        		(new Class_DAO()).getSession().flush();
    		}
    	}

    	return sectionNumber;
    }

    public String getSectionNumberString(){
    	return getSectionNumber()+getSchedulingSubpart().getSchedulingSubpartSuffix();
    }

    public String getSectionNumberString(org.hibernate.Session hibSession){
    	return getSectionNumber(hibSession)+getSchedulingSubpart().getSchedulingSubpartSuffix(hibSession);
    }

    public List<DepartmentalInstructor> getLeadInstructors() {
    	List<DepartmentalInstructor> ret = new ArrayList<DepartmentalInstructor>();
    	if (getClassInstructors() == null) {
    		return ret;
    	}
    	for (ClassInstructor classInstructor: getClassInstructors()) {
    		if (classInstructor.isLead().booleanValue()) ret.add(classInstructor.getInstructor());
    	}
    	return ret;
    }

    /**
     * @return Class Label of the form {CourseName} {Itype} {[config]} {Section No.}
     */
    public String getClassLabel() {
        /*
        SchedulingSubpart ss = getSchedulingSubpart();
    	String itypeDesc = ss.getItypeDesc();
    	if (ss.getInstrOfferingConfig().getInstructionalOffering().hasMultipleConfigurations())
    		itypeDesc += " [" + ss.getInstrOfferingConfig().getName() + "]";
        */
    	return getCourseName()+" "+getItypeDesc().trim()+" "+getSectionNumberString();
//    	return(getClassLabel(getSchedulingSubpart().getControllingCourseOffering()));
    }

    public String getClassLabel(org.hibernate.Session hibSession) {
    	return getCourseName()+" "+getItypeDesc().trim()+" "+getSectionNumberString(hibSession);
    }

    public String getClassLabelWithTitle() {
    	return getCourseNameWithTitle()+" "+getItypeDesc().trim()+" "+getSectionNumberString();
//    	return(getClassLabelWithTitle(getSchedulingSubpart().getControllingCourseOffering()));
    }

    /**
     * @see getClassLabel()
     */
    public String toString() {
        return getClassLabel();
    }

	/**
	 * @return Class type to distinguish the sub class in PrefGroup
	 */
	public Class getInstanceOf() {
	    return Class_.class;
	}

 	/* (non-Javadoc)
	 * @see org.unitime.timetable.model.PreferenceGroup#canUserEdit(org.unitime.commons.User)
	 */
	protected boolean canUserEdit(User user) {
		TimetableManager tm = TimetableManager.getManager(user);
		if (tm == null) return false;

		if (!Roles.DEPT_SCHED_MGR_ROLE.equals(user.getRole())) return false;

		if (tm.getDepartments().contains(getManagingDept())) {
			//I am manager, return true if manager can edit the class
			if (getManagingDept().effectiveStatusType().canManagerEdit()) return true;
		}

		if (tm.getDepartments().contains(getControllingDept())) {
			//I am owner, return true if owner can edit the class
			if (getManagingDept().effectiveStatusType().canOwnerEdit()) return true;
		}

		return false;
	}

	/*
	 * canUserEdit() - if the user can edit any of the classes in the same
	 * 	   SchedulingSubpart as this class then the user can view this class
	 */
	protected boolean canUserView(User user) {
		TimetableManager tm = TimetableManager.getManager(user);
		if (tm == null) return false;

		if (tm.getDepartments().contains(getManagingDept())) {
			//I am manager, return true if manager can view the class
			if (getManagingDept().effectiveStatusType().canManagerView()) return true;
		}

		if (tm.getDepartments().contains(getControllingDept())) {
			//I am owner, return true if owner can view the class
			if (getManagingDept().effectiveStatusType().canOwnerView()) return true;
		}

		if (tm.isExternalManager() && getManagingDept().effectiveStatusType().canManagerView()) return true;

		return false;
	}

	@Override
	public boolean isEditableBy(User user) {
    	if (getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering().getSession().isOfferingLockNeeded(
    			getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering().getUniqueId())) {
    		return false;
        }
    	return super.isEditableBy(user);
	}

	public boolean isLimitedEditable(User user) {
		if (isEditableBy(user)) return true;
		if (user==null) return false;
		if (user.isAdmin()) return true;

		TimetableManager tm = TimetableManager.getManager(user);
		if (tm == null) return false;

        if (!Roles.DEPT_SCHED_MGR_ROLE.equals(user.getRole())) return false;

        if (tm.getDepartments().contains(getManagingDept())) {
			//I am manager, return true if manager can view the class
			if (getManagingDept().effectiveStatusType().canManagerLimitedEdit()) return true;
		}

		if (tm.getDepartments().contains(getControllingDept())) {
			//I am owner, return true if owner can view the class
			if (getManagingDept().effectiveStatusType().canOwnerLimitedEdit()) return true;
		}

		return false;
	}

	public String htmlLabel(){
		return(getItypeDesc()+" "+getSectionNumberString());
	}

	public boolean canBeDeleted(){
		if (this.getChildClasses() != null && this.getChildClasses().size() > 0){
			return(false);
		}
		if (this.getParentClass() != null){
			int totalCapacity = 0;
			Class_ c = null;
			for(Iterator it = this.getParentClass().getChildClasses().iterator(); it.hasNext();){
				c = (Class_) it.next();
				totalCapacity += c.getExpectedCapacity().intValue();
			}
			totalCapacity -= this.getExpectedCapacity().intValue();
			if (totalCapacity < this.getParentClass().getExpectedCapacity().intValue()){
				return(false);
			}
		}
		return(true);
	}

    /**
     * Remove class from instructor list
     * @param ci
     */
    public void removeClassInstructor(ClassInstructor classInstr) {
        Set s = this.getClassInstructors();
        for( Iterator iter=s.iterator(); iter.hasNext(); ) {
            ClassInstructor ci = (ClassInstructor) iter.next();
            if(ci.getUniqueId().intValue()==classInstr.getUniqueId().intValue()) {
                s.remove(ci);
                break;
            }
        }
    }

    public DatePattern effectiveDatePattern() {
    	if (getDatePattern()!=null) return getDatePattern();
    	return getSchedulingSubpart().effectiveDatePattern();
    }

    public boolean canUseHardTimePreferences(User user) {
    	if (user.isAdmin()) return true;
        TimetableManager tm = TimetableManager.getManager(user);
        if (tm.getDepartments().contains(getManagingDept())) return true;
        if (getControllingDept().isAllowReqTime()!=null && getControllingDept().isAllowReqTime().booleanValue()) return true;
        if (getManagingDept().isAllowReqTime()!=null && getManagingDept().isAllowReqTime().booleanValue()) return true;
        return(false);
    }

    public boolean canUseHardRoomPreferences(User user) {
        if (user.isAdmin()) return true;
        TimetableManager tm = TimetableManager.getManager(user);
        if (tm.getDepartments().contains(getManagingDept())) return true;
        if (getControllingDept().isAllowReqRoom()!=null && getControllingDept().isAllowReqRoom().booleanValue()) return true;
        if (getManagingDept().isAllowReqRoom()!=null && getManagingDept().isAllowReqRoom().booleanValue()) return true;
        return(false);
    }

    public Set<Location> getAvailableRooms() {

    	Set<Location> rooms =  new TreeSet<Location>();
        for (Iterator i=getManagingDept().getRoomDepts().iterator();i.hasNext();) {
        	RoomDept roomDept = (RoomDept)i.next();
        	rooms.add(roomDept.getRoom());
        }

        return rooms;
    }

    public Set getAvailableRoomFeatures() {
    	Set features = new TreeSet(GlobalRoomFeature.getAllGlobalRoomFeatures(getSession()));
    	Department dept = getManagingDept();
    	if (dept!=null)
    		features.addAll(DepartmentRoomFeature.getAllDepartmentRoomFeatures(dept));
    	return features;

    }

    public Set getAvailableRoomGroups() {
    	Set groups = super.getAvailableRoomGroups();
    	Department dept = getManagingDept();
    	if (dept!=null)
    		groups.addAll(RoomGroup.getAllDepartmentRoomGroups(dept));
    	return groups;
    }

    public Class_ getNextClass(SessionContext context, Right right) {
    	return getNextClass(context, new NavigationComparator(), right);
    }

    public Class_ getPreviousClass(SessionContext context, Right right) {
    	return getPreviousClass(context, new NavigationComparator(), right);
    }

    public Class_ getNextClass(SessionContext context, Comparator cmp, Right right) {
    	Long nextId = Navigation.getNext(context, Navigation.sClassLevel, getUniqueId());
    	if (nextId!=null) {
    		if (nextId.longValue()<0) return null;
    		Class_ next = (new Class_DAO()).get(nextId);
    		if (next==null) return null;
    		if (right != null && !context.hasPermission(next, right)) return next.getNextClass(context, cmp, right);
    		return next;
    	}
    	Class_ next = null;
    	SchedulingSubpart subpart = getSchedulingSubpart();
    	while (next==null) {
    		if (subpart==null) break;
        	for (Iterator i=subpart.getClasses().iterator();i.hasNext();) {
        		Class_ c = (Class_)i.next();
        		if (right != null && !context.hasPermission(c, right)) continue;
        		if (subpart.equals(getSchedulingSubpart()) && cmp.compare(this, c)>=0) continue;
        		if (next==null || cmp.compare(next,c)>0)
        			next = c;
        	}
    		subpart = subpart.getNextSchedulingSubpart(context, cmp, null);
    	}
    	return next;
    }

    public Class_ getPreviousClass(SessionContext context, Comparator cmp, Right right) {
    	Long previosId = Navigation.getPrevious(context, Navigation.sClassLevel, getUniqueId());
    	if (previosId!=null) {
    		if (previosId.longValue()<0) return null;
    		Class_ previos = (new Class_DAO()).get(previosId);
    		if (previos==null) return null;
    		if (right != null && !context.hasPermission(previos, right)) return previos.getPreviousClass(context, cmp, right);
    		return previos;
    	}
    	Class_ previous = null;
    	SchedulingSubpart subpart = getSchedulingSubpart();
    	while (previous==null) {
    		if (subpart==null) break;
        	for (Iterator i=subpart.getClasses().iterator();i.hasNext();) {
        		Class_ c = (Class_)i.next();
        		if (right != null && !context.hasPermission(c, right)) continue;
        		if (subpart.equals(getSchedulingSubpart()) && cmp.compare(this, c)<=0) continue;
        		if (previous==null || cmp.compare(previous,c)<0)
        			previous = c;
        	}
    		subpart = subpart.getPreviousSchedulingSubpart(context, cmp, null);
    	}
    	return previous;
    }

    /**
     * Deletes all distribution prefs and updates the class_ objects
     * @param hibSession
     */
    public void deleteAllDistributionPreferences(org.hibernate.Session hibSession) {
    	deleteAllDistributionPreferences(hibSession, true);
    }

    /**
     * Deletes all distribution prefs
     * @param hibSession
     * @param updateClass If true then class_ object is updated
     */
    public void deleteAllDistributionPreferences(org.hibernate.Session hibSession, boolean updateClass) {
    	boolean deleted = false;
    	if (getDistributionObjects()==null) return;
    	for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
    		DistributionObject relatedObject = (DistributionObject)i.next();
    		DistributionPref distributionPref = relatedObject.getDistributionPref();
    		distributionPref.getDistributionObjects().remove(relatedObject);
    		Integer seqNo = relatedObject.getSequenceNumber();
			hibSession.delete(relatedObject);
			deleted = true;
			if (distributionPref.getDistributionObjects().isEmpty()) {
				PreferenceGroup owner = distributionPref.getOwner();
				owner.getPreferences().remove(distributionPref);
				getPreferences().remove(distributionPref);
				hibSession.saveOrUpdate(owner);
				hibSession.delete(distributionPref);
			} else {
				if (seqNo!=null) {
					for (Iterator j=distributionPref.getDistributionObjects().iterator();j.hasNext();) {
						DistributionObject dObj = (DistributionObject)j.next();
						if (seqNo.compareTo(dObj.getSequenceNumber())<0) {
							dObj.setSequenceNumber(new Integer(dObj.getSequenceNumber().intValue()-1));
							hibSession.saveOrUpdate(dObj);
						}
					}
				}

				if (updateClass)
					hibSession.saveOrUpdate(distributionPref);
			}
			i.remove();
    	}

    	if (deleted && updateClass)
    		hibSession.saveOrUpdate(this);
    }

    public Integer getMinRoomLimit() {
    	int expCap = (getExpectedCapacity()==null?0:getExpectedCapacity().intValue());
    	float roomRatio = (getRoomRatio()==null?0.0f:getRoomRatio().floatValue());
    	return new Integer((int)Math.ceil(expCap<=0?roomRatio:expCap*roomRatio));
    }

    public static List findAll(Long sessionId) {
    	return findAll(Class_DAO.getInstance().getSession(), sessionId);
    }
    
    public static List findAll(org.hibernate.Session hibSession, Long sessionId) {
    	return hibSession.
    		createQuery("select distinct c from Class_ c where " +
    				"c.schedulingSubpart.instrOfferingConfig.instructionalOffering.session.uniqueId=:sessionId").
    		setLong("sessionId",sessionId.longValue()).
    		list();
    }

    public static List findAllForControllingSubjectArea(String subjectAreaAbbv, Long sessionId) {
    	return(findAllForControllingSubjectArea(subjectAreaAbbv, sessionId, (new Class_DAO()).getSession()));
    }

    public static List findAllForControllingSubjectArea(String subjectAreaAbbv, Long sessionId, org.hibernate.Session hibSession) {
    	return hibSession.
    		createQuery("select distinct c from Class_ c inner join c.schedulingSubpart.instrOfferingConfig.instructionalOffering.courseOfferings as co where " +
    				"co.subjectArea.subjectAreaAbbreviation=:subjectAreaAbbv and c.schedulingSubpart.instrOfferingConfig.instructionalOffering.session.uniqueId=:sessionId and co.isControl=1").
    		setString("subjectAreaAbbv",subjectAreaAbbv).
    		setLong("sessionId",sessionId.longValue()).
			setFlushMode(FlushMode.MANUAL).
    		list();
    }

    public String getDivSecNumber() {
    	if (getParentClass()!=null && getSchedulingSubpart().getItype().equals(getParentClass().getSchedulingSubpart().getItype())) {
    		return getParentClass().getDivSecNumber();
    	}
    	String suffix = getClassSuffix();
    	if (suffix!=null && suffix.length()==6) return suffix.substring(0,3)+"-"+suffix.substring(3,6);
    	return suffix;
    }

    public int getClassLimit() {
    	return getClassLimit(new CommitedClassAssignmentProxy());
    }

    public int getClassLimit(ClassAssignmentProxy proxy) {
    	// MinClassLimit == MaxClassLimit == ClassLimit
    	if (getExpectedCapacity().equals(getMaxExpectedCapacity())) return getExpectedCapacity().intValue();

    	// No assignment -> take MaxClassLimit
    	int maxClassLimit = getMaxExpectedCapacity().intValue();
    	Assignment assignment = null;
    	try {
    		assignment = (proxy==null?null:proxy.getAssignment(this));
    	} catch (Exception e) {
    		Debug.error(e);
    	}
    	if (assignment!=null) {
    		// Assignment -> take smaller from MaxClassLimit and RoomSize / RoomRatio
    		maxClassLimit = Math.min(
    				getMaxExpectedCapacity().intValue(),
    				(int)Math.floor(assignment.getPlacement().minRoomSize()/getRoomRatio().floatValue()));
    	}

    	if (getChildClasses().isEmpty()) return maxClassLimit;

    	// if there are children classes ...
    	for (Iterator i=getSchedulingSubpart().getChildSubparts().iterator();i.hasNext();) {
    		SchedulingSubpart childSubpart = (SchedulingSubpart)i.next();
    		// take all children classes of the same subpart, sum their class limit
    		int maxChildrenLimit = 0;
    		for (Iterator j=getChildClasses().iterator();j.hasNext();) {
    			Class_ childClass = (Class_)j.next();
    			if (!childClass.getSchedulingSubpart().equals(childSubpart)) continue;
    			maxChildrenLimit += childClass.getClassLimit(proxy);
    		}
    		// children class limit cannot be exceeded
    		maxClassLimit = Math.min(maxClassLimit, maxChildrenLimit);
    	}

    	return maxClassLimit;
    }

    public int getClassLimit(CourseOffering offering) {
        return getClassLimit(new CommitedClassAssignmentProxy(), offering);
    }
    
    private boolean hasChildClass(Reservation r) {
    	if (r.getClasses().contains(this)) return true;
    	for (Class_ child: getChildClasses())
    		if (child.hasChildClass(r)) return true;
    	return false;
    }
    
    public boolean hasClass(Reservation r) {
    	if (r.getClasses().isEmpty()) return false;
    	Class_ c = this;
    	while (c != null) {
    		if (r.getClasses().contains(c)) return true;
    		c = c.getParentClass();
    	}
    	for (Class_ child: getChildClasses())
    		if (child.hasChildClass(r)) return true;
    	return false;
    }

    @Deprecated
    public int getClassLimit(ClassAssignmentProxy proxy, CourseOffering offering) {
    	return getClassLimit(proxy);
    	/*
        int limit = getClassLimit(proxy);

        if (limit==0 || offering.getInstructionalOffering().getCourseOfferings().size()==1) {
            //CASE 0: no cross-listing or zero class limit -> return the class limit as it is
            return limit;
        }

        Long solutionId = null;
        if (proxy!=null) {
            try {
                Assignment a = proxy.getAssignment(this);
                if (a!=null) solutionId = a.getSolution().getUniqueId();
            } catch (Exception e) {}
        }
        if (solutionId==null && getCommittedAssignment()!=null) {
            solutionId = getCommittedAssignment().getSolution().getUniqueId();
        }

        float nrReservedStudents = 0;
        float nrReservedStudentsThisOffering = (offering.getReservation() == null ? 0 : offering.getReservation());
        for (CourseOffering course:offering.getInstructionalOffering().getCourseOfferings()) {
        	if (course.getReservation() != null)
        		nrReservedStudents += course.getReservation();
        }

        float nrLastLikeStudents = (float)(offering.getInstructionalOffering().getDemand()==null?0:offering.getInstructionalOffering().getDemand().intValue());
        float nrLastLikeStudentsThisOffering = (float)offering.getDemand().intValue();
        boolean canUseLastLikeData = (solutionId!=null && nrLastLikeStudentsThisOffering>0);
        if (canUseLastLikeData) {
            for (Iterator i=offering.getInstructionalOffering().getCourseOfferings().iterator();i.hasNext();) {
                CourseOffering co = (CourseOffering)i.next();
                if (co.getDemand().intValue()==0) {
                    canUseLastLikeData = false; break;
                }
            }
        }

        if (canUseLastLikeData) {
            //CASE 1: last-like term student course demands are there

            float nrStudents =
                ((Number)(new Class_DAO()).
                getSession().
                createQuery("select count (distinct cod.student.uniqueId) from LastLikeCourseDemand cod, "+
                    "StudentEnrollment se, "+
                    "CourseOffering co left outer join co.demandOffering cox " +
                    "where co.instructionalOffering.uniqueId=:instructionalOfferingId and "+
                    "se.studentId=cod.student.uniqueId and se.solution.uniqueId=:solutionId and se.clazz.uniqueId=:classId and "+
                    "((cod.subjectArea = co.subjectArea and cod.courseNbr=co.courseNbr) or "+
                    "(cod.subjectArea = cox.subjectArea and cod.courseNbr=cox.courseNbr))").
                setLong("instructionalOfferingId", offering.getInstructionalOffering().getUniqueId().longValue()).
                setLong("solutionId", solutionId.longValue()).
                setLong("classId",getUniqueId().longValue()).
                uniqueResult()).floatValue();

            if (nrStudents==0) return 0;

            float nrStudentsThisOffering =
                ((Number)(new Class_DAO()).
                getSession().
                createQuery("select count (distinct cod.student.uniqueId) from LastLikeCourseDemand cod, "+
                    "StudentEnrollment se, "+
                    "CourseOffering co left outer join co.demandOffering cox " +
                    "where co.uniqueId=:offeringId and "+
                    "se.studentId=cod.student.uniqueId and se.solution.uniqueId=:solutionId and se.clazz.uniqueId=:classId and "+
                    "((cod.subjectArea = co.subjectArea and cod.courseNbr=co.courseNbr) or "+
                    "(cod.subjectArea = cox.subjectArea and cod.courseNbr=cox.courseNbr))").
                setLong("offeringId", offering.getUniqueId().longValue()).
                setLong("solutionId", solutionId.longValue()).
                setLong("classId",getUniqueId().longValue()).
                uniqueResult()).floatValue();

            if (nrStudentsThisOffering==0) return 0;

            // return number proportional to the number of enrolled students
            float studentWeightThisOffering = nrReservedStudentsThisOffering / nrLastLikeStudentsThisOffering;
            float otherStudentWeight = (nrReservedStudents - nrReservedStudentsThisOffering) / (nrLastLikeStudents - nrLastLikeStudentsThisOffering);
            float ratio = (studentWeightThisOffering * nrStudentsThisOffering) /
                (studentWeightThisOffering * nrStudentsThisOffering + (nrStudents - nrStudentsThisOffering)*otherStudentWeight);
            return Math.round(ratio*limit);
        }

        if (nrReservedStudents>0) {
            //CASE 2: no last-like term student course demands, but there are course reservations

            if (!getChildClasses().isEmpty()) {
                //CASE 2a: base the class limit on the limits of children
                float limitThisOffering = -1;
                for (Iterator i=getSchedulingSubpart().getChildSubparts().iterator();i.hasNext();) {
                    SchedulingSubpart subpart = (SchedulingSubpart)i.next();
                    float limitThisSubpart = 0;
                    float limitThisSubpartThisOffering = 0;
                    boolean hasCourseReservation = false;
                    for (Iterator j=getChildClasses().iterator();j.hasNext();) {
                        Class_ childClass = (Class_)j.next();
                        if (childClass.hasCourseReservation(offering)) hasCourseReservation = true;
                        if (!childClass.getSchedulingSubpart().equals(subpart)) continue;
                        limitThisSubpart += childClass.getClassLimit(proxy);
                        limitThisSubpartThisOffering += childClass.getClassLimit(proxy, offering);
                    }
                    if (limitThisOffering<0f) {
                        limitThisOffering = limitThisSubpartThisOffering * (limit/limitThisSubpart);
                    } else if (hasCourseReservation) {
                        limitThisOffering = limitThisSubpartThisOffering * (limit/limitThisSubpart);
                        break;
                    } else {
                        limitThisOffering = Math.min(limitThisOffering, limitThisSubpartThisOffering * (limit/limitThisSubpart));
                    }
                }
                if (Math.round(limitThisOffering)>=0)
                    return Math.round(limitThisOffering);
            } else {
                //CASE 2b: compute limit on bottom most classes
                Hashtable reservedClasses = new Hashtable();
                for (Iterator i=getSchedulingSubpart().getClasses().iterator();i.hasNext();) {
                    Class_ clazz = (Class_)i.next();
                    HashSet courseOfferingsThisClass = new HashSet();
                	for (Reservation r: offering.getInstructionalOffering().getReservations()) {
                		if (r instanceof CourseReservation && clazz.hasClass(r)) {
                			courseOfferingsThisClass.add(((CourseReservation)r).getCourse());
                		}
                	}
                    for (Iterator j=courseOfferingsThisClass.iterator();j.hasNext();) {
                        CourseOffering co = (CourseOffering)j.next();
                        HashSet reservedClassesThisCO = (HashSet)reservedClasses.get(co);
                        if (reservedClassesThisCO==null) {
                            reservedClassesThisCO = new HashSet();
                            reservedClasses.put(co, reservedClassesThisCO);
                        }
                        reservedClassesThisCO.add(clazz);
                    }
                }
                if (reservedClasses.get(offering)!=null) {
                    HashSet reservedClassesThisCO = (HashSet)reservedClasses.get(offering);
                    if (!reservedClassesThisCO.contains(this)) return 0;
                    if (reservedClassesThisCO.size()==1) return Math.round(nrReservedStudentsThisOffering);
                    float totalLimit = 0;
                    for (Iterator j=reservedClassesThisCO.iterator();j.hasNext();) {
                        Class_ clazz = (Class_)j.next();
                        totalLimit += clazz.getClassLimit(proxy);
                    }
                    return Math.round(nrReservedStudentsThisOffering * (limit / totalLimit));
                } else {
                    //all classes but the ones reserved
                    int updatedLimit = limit;
                    float updatedNrReservedStudents = nrReservedStudents;
                    for (Iterator i=reservedClasses.entrySet().iterator();i.hasNext();) {
                        Map.Entry entry = (Map.Entry)i.next();
                        CourseOffering co = (CourseOffering)entry.getKey();
                        HashSet classes = (HashSet)entry.getValue();
                        float nrReservedStudentsCO = (co.getReservation() == null ? 0 : co.getReservation());
                        updatedNrReservedStudents -= nrReservedStudentsCO;
                        if (!classes.contains(this)) continue;
                        float totalLimit = 0;
                        for (Iterator j=classes.iterator();j.hasNext();) {
                            Class_ clazz = (Class_)j.next();
                            totalLimit += clazz.getClassLimit(proxy);
                        }
                        updatedLimit -= Math.round(nrReservedStudentsCO * (limit / totalLimit));
                    }
                    float ratio = nrReservedStudentsThisOffering/updatedNrReservedStudents;
                    return Math.round(ratio * updatedLimit);
                }
            }

            // return number proportional to the reserved spaces
            float ratio = nrReservedStudentsThisOffering/nrReservedStudents;
            return Math.round(ratio*limit);
        }

        //CASE 3: no last-like term student course demands, no course reservations
        // return the class limit as it is
        return limit;
        */
    }

    /**
     * Delete all objects that have an identifying relationship with a class
     * Usually use this method when one needs to delete a class
     * (in which case set updateClass to false because the class will eventually be deleted)
     * @param hibSession
     * @param updateClass Updates class when all dependent objects are deleted
     */
	public void deleteAllDependentObjects(org.hibernate.Session hibSession, boolean updateClass) {

		// Call individual methods to delete specific collections
	    deleteAllDistributionPreferences(hibSession, updateClass);
		deleteClassInstructors(hibSession);
		deleteAssignments(hibSession);
		Exam.deleteFromExams(hibSession, this);
		Event.deleteFromEvents(hibSession, this);

		// Add more collection deletes if needed

		if (updateClass)
			hibSession.saveOrUpdate(this);
	}

	/**
	 * Delete all class instructors
	 * @param hibSession
	 */
	public void deleteClassInstructors(org.hibernate.Session hibSession) {
		Set s = getClassInstructors();
		//deleteObjectsFromCollection(hibSession, s);
		if (s==null || s.size()==0) return;

		for (Iterator i=s.iterator(); i.hasNext(); ) {
			ClassInstructor ci = (ClassInstructor) (i.next());

			DepartmentalInstructor di = new DepartmentalInstructorDAO().get(ci.getInstructor().getUniqueId());
			di.getClasses().remove(ci);
			ci.setInstructor(null);
			ci.setClassInstructing(null);
			hibSession.saveOrUpdate(di);
			hibSession.delete(ci);
			i.remove();
		}
	}

	/**
	 * Delete all class assignments
	 * @param hibSession
	 */
	public void deleteAssignments(org.hibernate.Session hibSession) {
		Set s = getAssignments();
		deleteObjectsFromCollection(hibSession, s);
	}

	/**
	 * Common method to delete objects from acollection
	 * @param hibSession
	 * @param s Collection from which objects have to be deleted
	 */
	private void deleteObjectsFromCollection(org.hibernate.Session hibSession, Collection s) {
		if (s==null || s.size()==0) return;

		for (Iterator i=s.iterator(); i.hasNext(); ) {
			hibSession.delete(i.next());
			i.remove();
		}
	}
	
	public boolean isOddOrEvenWeeksOnly(){
		if (effectiveDatePattern() != null && effectiveDatePattern().getType().equals(new Integer(DatePattern.sTypeAlternate))){
			return(true);
		}
		return(false);
			
	}
	
	public Object clone(){
		Class_ newClass = new Class_();
		newClass.setDatePattern(getDatePattern());
		newClass.setDisplayInScheduleBook(isDisplayInScheduleBook());
		newClass.setDisplayInstructor(isDisplayInstructor());
		newClass.setExpectedCapacity(getExpectedCapacity());
		newClass.setManagingDept(getManagingDept());
		newClass.setMaxExpectedCapacity(getMaxExpectedCapacity());
		newClass.setNbrRooms(getNbrRooms());
		newClass.setNotes(getNotes());
		newClass.setRoomRatio(getRoomRatio());
		newClass.setSchedulePrintNote(getSchedulePrintNote());
		newClass.setSchedulingSubpart(getSchedulingSubpart());
		return(newClass);
	}
	
	public Object cloneWithPreferences(){
		Class_ newClass = (Class_) this.clone();
		if (getPreferences() != null){
			Preference origPref = null;
			Preference newPref = null;
			for (Iterator prefIt = getPreferences().iterator(); prefIt.hasNext();){
				origPref = (Preference) prefIt.next();	
				if (!(origPref instanceof DistributionPref)) {
					newPref = (Preference)origPref.clone();
					newPref.setOwner(newClass);
					newClass.addTopreferences(newPref);
				}
			}
		}
		if (getClassInstructors() != null && !getClassInstructors().isEmpty()){
			ClassInstructor ci = null;
			ClassInstructor newCi = null;
			for (Iterator ciIt = getClassInstructors().iterator(); ciIt.hasNext();){
				ci = (ClassInstructor) ciIt.next();
				newCi = new ClassInstructor();
				newCi.setClassInstructing(newClass);
				newCi.setInstructor(ci.getInstructor());
				newCi.setLead(ci.isLead());
				newCi.setPercentShare(ci.getPercentShare());
				ci.getInstructor().addToclasses(newCi);
				newClass.addToclassInstructors(newCi);
			}
		}
		return(newClass);
	}
	
    public static Class_ findByExternalId(Long sessionId, String externalId) {
        return (Class_)new Class_DAO().
            getSession().
            createQuery("select c from Class_ c where c.schedulingSubpart.instrOfferingConfig.instructionalOffering.session.uniqueId=:sessionId and c.externalUniqueId=:externalId").
            setLong("sessionId", sessionId.longValue()).
            setString("externalId", externalId).
            setCacheable(true).
            uniqueResult();
    }
    
    public static Class_ findByIdRolledForwardFrom(Long sessionId, Long uniqueIdRolledForwardFrom) {
        return (Class_)new Class_DAO().
            getSession().
            createQuery("select c from Class_ c where c.schedulingSubpart.instrOfferingConfig.instructionalOffering.session.uniqueId=:sessionId and c.uniqueIdRolledForwardFrom=:uniqueIdRolledForwardFrom").
            setLong("sessionId", sessionId.longValue()).
            setLong("uniqueIdRolledForwardFrom", uniqueIdRolledForwardFrom.longValue()).
            setCacheable(true).
            uniqueResult();
    }
    
    private ClassEvent iEvent = null;
    public ClassEvent getEvent() {
        if (iEvent==null) 
            iEvent = (ClassEvent)new Class_DAO().getSession().createQuery(
                "select e from ClassEvent e left join fetch e.meetings m where e.clazz.uniqueId=:classId").
                setLong("classId", getUniqueId()).
                setCacheable(true).uniqueResult();
        return iEvent;
    }
    public void setEvent(ClassEvent event) {
        iEvent = event;
    }
    public ClassEvent getCachedEvent() {
    	return iEvent;
    }
    
    @Deprecated
    public static void updateClassEnrollmentForSession(Session acadSession, org.hibernate.Session hibSession) throws Exception{
    	/*
       Transaction trans = null;
       try {
    	trans = hibSession.beginTransaction();
    	List<Long> classIds = (List<Long>)hibSession.createQuery(
    			"select c.uniqueId from Class_ c inner join c.schedulingSubpart.instrOfferingConfig.instructionalOffering as io " +
    			"where io.session.uniqueId = :sessionId)").
                setLong("sessionId", acadSession.getUniqueId()).list();
    	String ids = "";
    	int count = 0;
    	for (Long id: classIds) {
    		if (count > 0) ids += ",";
    		ids += id;
    		count ++;
    		if (count == 1000) {
    			hibSession.createQuery("update Class_  c " +
    	        		"set c.enrollment=(select count(distinct d.student) " +
    	                " from StudentClassEnrollment d " +
    	                " where d.clazz.uniqueId =c.uniqueId) " + 
    	                " where c.uniqueId in (" + ids + ")").executeUpdate();
    			ids = ""; count = 0;
    		}
    	}
    	if (count > 0) {
			hibSession.createQuery("update Class_  c " +
	        		"set c.enrollment=(select count(distinct d.student) " +
	                " from StudentClassEnrollment d " +
	                " where d.clazz.uniqueId =c.uniqueId) " + 
	                " where c.uniqueId in (" + ids + ")").executeUpdate();
    	}
        trans.commit();
       } catch (Exception e) {
    	   if (trans != null){
    		   trans.rollback();
    	   }
		   throw(e);    	   
       }
       */
    }
    
    public String unassignCommited(String managerExternalId, org.hibernate.Session hibSession) {
        Transaction tx = null;
        try {
            if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
                tx = hibSession.beginTransaction();

            Assignment oldAssignment = getCommittedAssignment();
            
            if (oldAssignment==null)
            	throw new RuntimeException("Class "+getClassLabel()+" does not have an assignment.");
            
            ClassEvent event = getEvent();
            if (event!=null) hibSession.delete(event);

            String old = oldAssignment.getPlacement().getName();
            
            oldAssignment.getSolution().getAssignments().remove(oldAssignment);
            
            // Remove all related constraint infos to avoid hibernate cache issues 
            // when an orphaned constraint info is automatically deleted
            for (ConstraintInfo ci: oldAssignment.getConstraintInfo()) {
            	for (Assignment a: ci.getAssignments()) {
            		if (!a.equals(oldAssignment)) {
            			a.getConstraintInfo().remove(ci);
            		}
            	}
            	hibSession.delete(ci);
            }
            
        	hibSession.delete(oldAssignment);
        	
        	setCommittedAssignment(null);
        	hibSession.update(this);
        	
            ChangeLog.addChange(hibSession,
                    TimetableManager.findByExternalId(managerExternalId),
                    getSession(),
                    this,
                    getClassLabel()+" ("+
                    old+
                    " &rarr; N/A)",
                    ChangeLog.Source.CLASS_INFO,
                    ChangeLog.Operation.UNASSIGN,
                    getSchedulingSubpart().getControllingCourseOffering().getSubjectArea(),
                    getManagingDept());

            if (tx!=null) tx.commit();
            
            new _RootDAO().getSession().refresh(this);
            String className = ApplicationProperties.getProperty("tmtbl.external.class.edit_action.class");
        	if (className != null && className.trim().length() > 0){
            	ExternalClassEditAction editAction = (ExternalClassEditAction) (Class.forName(className).newInstance());
           		editAction.performExternalClassEditAction(this, hibSession);
        	}

            return null;
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
            return "Unassignment of "+getClassLabel()+" failed, reason: "+e.getMessage();
        }   
    }

    public String assignCommited(ClassAssignmentInfo assignment, String managerExternalId, org.hibernate.Session hibSession) {
        Transaction tx = null;
        try {
            if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
                tx = hibSession.beginTransaction();

            String old = "N/A";
            
            Assignment oldAssignment = getCommittedAssignment();
            if (oldAssignment!=null) {
                old = oldAssignment.getPlacement().getName();
                
                oldAssignment.getSolution().getAssignments().remove(oldAssignment);
                
                // Remove all related constraint infos to avoid hibernate cache issues 
                // when an orphaned constraint info is automatically deleted
                for (ConstraintInfo ci: oldAssignment.getConstraintInfo()) {
                	for (Assignment a: ci.getAssignments()) {
                		if (!a.equals(oldAssignment)) {
                			a.getConstraintInfo().remove(ci);
                		}
                	}
                	hibSession.delete(ci);
                }
                
            	hibSession.delete(oldAssignment);
            }
            
            SolverGroup group = getManagingDept().getSolverGroup();
            if (group==null) throw new RuntimeException("Department "+getManagingDept().getLabel()+" has no solver group.");
            Solution solution = group.getCommittedSolution();
            if (solution==null) throw new RuntimeException("Solver group "+group.getName()+" has no commited solution.");
            
            DatePattern dp = DatePatternDAO.getInstance().get(assignment.getDate().getId(), hibSession);
            
            Assignment a = new Assignment();
            a.setSolution(solution);
            a.setSlotsPerMtg(assignment.getTime().getNrSlotsPerMeeting());
            a.setBreakTime(assignment.getTime().getBreakTime());
            a.setClazz(this);
            a.setClassName(getClassLabel());
            a.setClassId(getUniqueId());
            a.setDays(assignment.getTime().getDayCode());
            a.setRooms(new HashSet());
            a.setInstructors(new HashSet());
            a.setStartSlot(assignment.getTime().getStartSlot());
            a.setTimePattern(assignment.getTime().getTimePattern(hibSession));
            a.setDatePattern(dp != null ? dp : effectiveDatePattern());
            a.setAssignmentInfo(new HashSet());
            
            for (ClassRoomInfo room: assignment.getRooms())
            	a.getRooms().add(room.getLocation(hibSession));
            for (ClassInstructorInfo inst: assignment.getInstructors())
            	if (inst.isLead()) a.getInstructors().add(inst.getInstructor(hibSession).getInstructor());
            
            hibSession.save(a);

            //TODO: More information should be gathered about the assignment.
            AssignmentPreferenceInfo pref = new AssignmentPreferenceInfo();
            pref.setTimePreference(assignment.getTime().getPreference());
            for (ClassRoomInfo room:assignment.getRooms())
            	pref.setRoomPreference(room.getLocationId(), room.getPreference());
            AssignmentInfo ai = new AssignmentInfo();
			ai.setAssignment(a);
			ai.setDefinition(SolverInfoDef.findByName(hibSession,"AssignmentInfo"));
			ai.setOpt(null);
			ai.setInfo(pref);
			hibSession.save(ai);
            
			a.getAssignmentInfo().add(ai);
			a.cleastAssignmentInfoCache();
            
            ClassEvent event = getEvent();
            event = a.generateCommittedEvent(event, true);
            if (event != null)
            	hibSession.saveOrUpdate(event);

            setCommittedAssignment(a);
            hibSession.update(this);

            ChangeLog.addChange(hibSession,
                    TimetableManager.findByExternalId(managerExternalId),
                    getSession(),
                    this,
                    getClassLabel()+" ("+
                    old+
                    " &rarr; "+assignment.getTime().getName()+" "+assignment.getRoomNames(", ")+")",
                    ChangeLog.Source.CLASS_INFO,
                    ChangeLog.Operation.ASSIGN,
                    getSchedulingSubpart().getControllingCourseOffering().getSubjectArea(),
                    getManagingDept());
            
            if (tx!=null) tx.commit();
            
            new _RootDAO().getSession().refresh(this);
            String className = ApplicationProperties.getProperty("tmtbl.external.class.edit_action.class");
        	if (className != null && className.trim().length() > 0){
            	ExternalClassEditAction editAction = (ExternalClassEditAction) (Class.forName(className).newInstance());
           		editAction.performExternalClassEditAction(this, hibSession);
        	}
            
            return null;
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            e.printStackTrace();
            return "Assignment of "+getClassLabel()+" failed, reason: "+e.getMessage();
        }   
    }
    
    public Collection<Long> getEnrolledStudentIds() {
        return Class_DAO.getInstance().getSession().createQuery(
                "select e.student.uniqueId from StudentClassEnrollment e where "+
                "e.clazz.uniqueId=:classId")
                .setLong("classId",getUniqueId())
                .setCacheable(true).list();

    }
    
    public String buildAssignedTimeHtml(ClassAssignmentProxy proxy){
		Assignment a = null;
		StringBuffer sb = new StringBuffer();
		try {
			a = proxy.getAssignment(this);
		} catch (Exception e) {
			Debug.error(e);
		}
		if (a!=null) {
				Enumeration<Integer> e = a.getTimeLocation().getDays();
				while (e.hasMoreElements()){
					sb.append(Constants.DAY_NAMES_SHORT[e.nextElement()]);
				}
				sb.append(" ");
				sb.append(a.getTimeLocation().getStartTimeHeader());
				sb.append("-");
				sb.append(a.getTimeLocation().getEndTimeHeader());
		} else {
			if (getEffectiveTimePreferences().isEmpty()){
	            if (getSchedulingSubpart().getMinutesPerWk().intValue()<=0) {
	                sb.append("Arr Hrs");
	            } else {
	                int nrHours = Math.round(getSchedulingSubpart().getMinutesPerWk().intValue()/50.0f);
	                sb.append("Arr "+nrHours+" Hrs");
	            }	
			}
		}
		if (sb.length() == 0){
			sb.append(" ");
		}
	    return(sb.toString());
	}

	public String buildAssignedRoomHtml(ClassAssignmentProxy proxy){
		Assignment a = null;
		StringBuffer sb = new StringBuffer();
		try {
			a= proxy.getAssignment(this);
		} catch (Exception e) {
			Debug.error(e);
		}
		if (a!=null) {
			Iterator it2 = a.getRooms().iterator();
			while (it2.hasNext()){
				Location room = (Location)it2.next();
				sb.append(room.getLabel());
			}	
		} else {
			if (getEffectiveTimePreferences().isEmpty()){
	            boolean first = true;
	            for(Iterator it = getEffectiveRoomPreferences().iterator(); it.hasNext();){
	            	RoomPref rp = (RoomPref) it.next();
	            	if (rp.getPrefLevel().getPrefId().toString().equals(PreferenceLevel.PREF_LEVEL_REQUIRED)){
	            		if (first) {
	            			first = false;
	            		} else {
	            			sb.append("<br>");
	            		}
	            		sb.append(rp.getRoom().getLabel());
	            	}
	            }
			}
		}
		if (sb.length() == 0){
			sb.append(" ");
		}
	    return(sb.toString());
	}

	public String buildInstructorHtml(String nameFormat){
		StringBuffer sb = new StringBuffer();
		if (getClassInstructors() != null && !getClassInstructors().isEmpty()){
			boolean first = true;
			for(ClassInstructor ci : (Set<ClassInstructor>) getClassInstructors()){
				if (first){
					first = false;
				} else {
					sb.append("<br>");
				}
				sb.append(ci.getInstructor().getName(nameFormat));
			}
		} else {
			sb.append(" ");
		}
	    return(sb.toString());
	}

	public static ExternalClassNameHelperInterface getExternalClassNameHelper() {
		if (externalClassNameHelper == null){
            String className = ApplicationProperties.getProperty("tmtbl.class.naming.helper", "org.unitime.timetable.util.DefaultExternalClassNameHelper");
        	if (className != null && className.trim().length() > 0){
        		try {
					externalClassNameHelper = (ExternalClassNameHelperInterface) (Class.forName(className).newInstance());
				} catch (InstantiationException e) {
					Debug.error("Failed to instantiate instance of: " + className + " using the default class name helper.");
					e.printStackTrace();
	        		externalClassNameHelper = new DefaultExternalClassNameHelper();
				} catch (IllegalAccessException e) {
					Debug.error("Illegal Access Exception on: " + className + " using the default class name helper.");
					e.printStackTrace();
	        		externalClassNameHelper = new DefaultExternalClassNameHelper();
				} catch (ClassNotFoundException e) {
					Debug.error("Failed to find class: " + className + " using the default class name helper.");
					e.printStackTrace();
	        		externalClassNameHelper = new DefaultExternalClassNameHelper();
				}
        	} else {
        		externalClassNameHelper = new DefaultExternalClassNameHelper();
        	}
		}
		return externalClassNameHelper;
	}
    
	public String getClassLabel(CourseOffering courseOffering) {
		return(getExternalClassNameHelper().getClassLabel(this, courseOffering));
	}

	public String getClassSuffix(CourseOffering courseOffering) {
		return(getExternalClassNameHelper().getClassSuffix(this, courseOffering));
	}

	public String getClassLabelWithTitle(CourseOffering courseOffering) {
		return(getExternalClassNameHelper().getClassLabelWithTitle(this,courseOffering));
	}

	public String getExternalId(CourseOffering courseOffering) {
		return(getExternalClassNameHelper().getExternalId(this, courseOffering));
	}
	
	public SectioningInfo getSectioningInfo() {
		return (SectioningInfo) SectioningInfoDAO.getInstance().getSession().createQuery(
				"select i from SectioningInfo i where i.clazz.uniqueId = :classId")
				.setLong("classId", getUniqueId()).setCacheable(true).uniqueResult();
	}

//	/* (non-Javadoc)
//	 * @see org.unitime.timetable.model.base.BaseClass_#getClassSuffix()
//	 */
//	@Override
//	public String getClassSuffix() {
//		return(getClassSuffix(getSchedulingSubpart().getControllingCourseOffering()));
//	}


	@Override
	public Department getDepartment() { return getManagingDept(); }

}
