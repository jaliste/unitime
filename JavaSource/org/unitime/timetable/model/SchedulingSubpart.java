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
package org.unitime.timetable.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpSession;

import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.timetable.model.base.BaseSchedulingSubpart;
import org.unitime.timetable.model.comparators.NavigationComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.SchedulingSubpartDAO;
import org.unitime.timetable.webutil.Navigation;


public class SchedulingSubpart extends BaseSchedulingSubpart {
	private static final long serialVersionUID = 1L;

	/** Request Parameter name for Scheduling Subpart List **/
	public static final String SCHED_SUBPART_ATTR_NAME = "schedSubpartList";
	
/*[CONSTRUCTOR MARKER BEGIN]*/
	public SchedulingSubpart () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public SchedulingSubpart (java.lang.Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/

	/*
	public String getCourseName(){
		return(this.getInstrOfferingConfig().getCourseName());
	}
	*/
	
	public String getCourseNameWithTitle(){
		return(this.getInstrOfferingConfig().getCourseNameWithTitle());
	}

	public CourseOffering getControllingCourseOffering(){
		return(this.getInstrOfferingConfig().getControllingCourseOffering());
	}
	
	public String getItypeDesc() {
		try {
			ItypeDesc itype = getItype();
			return (itype==null?null:itype.getAbbv());
		} catch (Exception e) {
			Debug.error(e);
			return null;
		}
	}
	
	public Department getManagingDept(){
		if (this.getClasses() != null){
			boolean allSame = true;
			Department d = null;
			Department d1 = null;
			Iterator it = this.getClasses().iterator();
			Class_ c = null;
			while (it.hasNext() && allSame){
				c = (Class_) it.next();
				d = c.getManagingDept();
				if (d1 == null){
					d1 = d;
				}
				if (d1 != null && (d == null || d.getUniqueId() == null || !d.getUniqueId().equals(d1.getUniqueId()))){
					allSame = false;
				}
			}
			if (d != null && allSame){
				return(d);
			} else {
				return(this.getControllingDept());
			}
		}
		return(this.getControllingDept());
	}
    
    public Department getControllingDept() {
 		return (this.getInstrOfferingConfig().getDepartment());
	}
	
    /*
	public Session getSession() {
		return (this.getInstrOfferingConfig().getSession());
	}
	
	public Long getSessionId() {
		return (this.getInstrOfferingConfig().getSessionId());
	}
	*/
    
    public Long getSessionId() {
    	return getSession().getUniqueId();
    }
	
	public String htmlLabel(){
		return(this.getItype().getDesc());
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
		return(sb.toString());
	}
	
	public String effectiveTimePatternHtml(){
		return(htmlForTimePatterns(this.effectiveTimePatterns()));
	}
	
	public String timePatternHtml(){
		return(htmlForTimePatterns(this.getTimePatterns()));
	}

	/* (non-Javadoc)
	 * @see org.unitime.timetable.model.PreferenceGroup#canUserEdit(org.unitime.commons.User)
	 * canUserEdit() - the user can edit this subpart if the user canEdit all of the
	 *      classes owned by this subpart or the user is the schedule deputy for the
	 *      subjectArea of the subpart and can edit at least one class owned by the
	 *      subpart
	 */
	protected boolean canUserEdit(User user) {
		TimetableManager tm = TimetableManager.getManager(user);
		if (tm == null) return false;
		
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
	
	protected boolean canUserView(User user){
		TimetableManager tm = TimetableManager.getManager(user);
		if (tm == null) return false;

		if (getClasses()==null || getClasses().isEmpty()) {
			if (tm.getDepartments().contains(getControllingDept())) {
				//I am owner, return true if owner can edit the class
				if (getManagingDept().effectiveStatusType().canOwnerView()) return true;
			}
			if (tm.isExternalManager() && getManagingDept().effectiveStatusType().canManagerView()) return true;
		} else {
			//can view at least one class
			for (Iterator i=getClasses().iterator();i.hasNext();) {
				Class_ clazz = (Class_)i.next();
				if (clazz.canUserView(user)) return true;
			}
		}
		
		return false;
	}
	
	public boolean isLimitedEditable(User user) {
		if (isEditableBy(user)) return true;
		if (user==null) return false;
		if (user.isAdmin()) return true;
		
		TimetableManager tm = TimetableManager.getManager(user);
		if (tm == null) return false;
		
		if (getClasses()==null || getClasses().isEmpty()) {
			if (tm.getDepartments().contains(getControllingDept())) {
				//I am owner, return true if owner can edit the class
				if (getManagingDept().effectiveStatusType().canOwnerLimitedEdit()) return true;
			}
		} else {
			//can view at least one class
			for (Iterator i=getClasses().iterator();i.hasNext();) {
				Class_ clazz = (Class_)i.next();
				if (clazz.isLimitedEditable(user)) return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Gets the minimum class limit for the sub class
	 * @return Class Limit (-1 if classes not defined, 0 if no classes set)
	 */
	public int getMinClassLimit() {
	    Set classes = this.getClasses();
        if(classes==null) return -1;
        if(classes.size()==0) return 0;
        
        int limit = 0;
        Iterator iter = classes.iterator();
        while (iter.hasNext()) {
            Class_ c = (Class_) iter.next();
            int ct = c.getExpectedCapacity().intValue();
            if(ct>limit) limit = ct;
        }

        return limit;
	}
	
	/**
	 * Gets the maximum class limit for the sub class
	 * @return Class Limit (-1 if classes not defined, 0 if no classes set)
	 */
	public int getMaxClassLimit() {
	    Set classes = this.getClasses();
        if(classes==null) return -1;
        if(classes.size()==0) return 0;
        
        int limit = 0;
        Iterator iter = classes.iterator();
        while (iter.hasNext()) {
            Class_ c = (Class_) iter.next();
            int ct = c.getMaxExpectedCapacity().intValue();
            if(ct>limit) limit = ct;
        }

        return limit;
	}
	
	/**
	 * Gets the number of classes for a subpart
	 * @return Number of classes (-1 if classes not defined)
	 */
	public int getNumClasses() {
	    Set classes = this.getClasses();
        if(classes==null) return -1;
        return classes.size();	    
	}
	
	public String getSchedulingSubpartLabel() {
		String sufix = getSchedulingSubpartSuffix();
        String cfgName = (getInstrOfferingConfig().getInstructionalOffering().hasMultipleConfigurations()?getInstrOfferingConfig().getName():null);        
		return getCourseName() + " " + this.getItypeDesc().trim() + (sufix==null || sufix.length()==0?"":" ("+sufix+")")+(cfgName==null?"":" ["+cfgName+"]");
	}
	
	/**
	 * Returns String representation of the form {Subj Area} {Crs Nbr} {Itype Desc} 
	 */
	public String toString() {
		return getSchedulingSubpartLabel();
	}
	
	/**
	 * @return Class type to distinguish the sub class in PrefGroup
	 */
	public Class getInstanceOf() {
	    return SchedulingSubpart.class;
	}

    /**
     * Gets the max room ratio among all the classes
     * belonging to the subpart
     * @return max room ratio
     */
    public float getMaxRoomRatio() {
	    Set classes = this.getClasses();
        if(classes==null) return -1;
        if(classes.size()==0) return 1.0f;
        
	    float rc = 0;
	    for (Iterator iter= classes.iterator(); iter.hasNext(); ) {
	        Class_ c = (Class_) iter.next();	        
	        Float rc1 = c.getRoomRatio();
	        if(rc1!=null && rc1.floatValue()>rc)
	            rc = rc1.floatValue();
	    }
	    
        return rc;
    }
    
    /**
     * Gets the max number of rooms among all the classes
     * belonging to the subpart
     * @return max number of rooms
     */
    public int getMaxRooms() {
	    Set classes = this.getClasses();
        if(classes==null) return -1;
        if(classes.size()==0) return -1;
        
        int numRooms = 0;
        Iterator iter = classes.iterator();
        while (iter.hasNext()) {
            Class_ c = (Class_) iter.next();
            int ct = c.getNbrRooms().intValue();
            if(ct>numRooms) numRooms = ct;
        }

        return numRooms;
        
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
    	if (getDistributionObjects()==null) return null;
    	TreeSet prefs = new TreeSet();
    	for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
    		DistributionObject distObj = (DistributionObject)i.next();
    		DistributionPref pref = distObj.getDistributionPref();
    		if (owningDept==null || owningDept.equals(pref.getOwner()))
    			prefs.add(pref);
    	}
    	return prefs;
    }

    public Set effectivePreferences(Class type) {
    	if (DistributionPref.class.equals(type)) {
    		return effectiveDistributionPreferences(getManagingDept());
    		/*
    		Department mgr = getManagingDept();
    		if (mgr!=null)
    			return mgr.getPreferences(type, this);
    		else
    			return null;
    			*/
    	}
    	
    	return super.effectivePreferences(type);
    }
    
	public DatePattern effectiveDatePattern() {
		if (getDatePattern()!=null) return getDatePattern();
		return getSession().getDefaultDatePatternNotNull();
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

        public Set getAvailableRooms() {
    	Set rooms =  new TreeSet();
        for (Iterator i=getManagingDept().getRoomDepts().iterator();i.hasNext();) {
        	RoomDept roomDept = (RoomDept)i.next();
        	rooms.add(roomDept.getRoom());
        }
        
        return rooms;
    }
    public Set getAvailableRoomFeatures() {
    	Set features = super.getAvailableRoomFeatures();
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
    
    public SchedulingSubpart getNextSchedulingSubpart(HttpSession session, User user, boolean canEdit, boolean canView) {
    	return getNextSchedulingSubpart(session, new NavigationComparator(), user, canEdit, canView);
    }
    
    public SchedulingSubpart getPreviousSchedulingSubpart(HttpSession session,User user, boolean canEdit, boolean canView) {
    	return getPreviousSchedulingSubpart(session, new NavigationComparator(), user, canEdit, canView);
    }
    
    
    public SchedulingSubpart getNextSchedulingSubpart(HttpSession session,Comparator cmp, User user, boolean canEdit, boolean canView) {
    	Long nextId = Navigation.getNext(session, Navigation.sSchedulingSubpartLevel, getUniqueId());
    	if (nextId!=null) {
    		if (nextId.longValue()<0) return null;
    		SchedulingSubpart next = (new SchedulingSubpartDAO()).get(nextId);
    		if (next==null) return null;
    		if (canEdit && !next.isEditableBy(user)) return next.getNextSchedulingSubpart(session, cmp, user, canEdit, canView); 
    		if (canView && !next.isViewableBy(user)) return next.getNextSchedulingSubpart(session, cmp, user, canEdit, canView);
    		return next;
    	}
    	SchedulingSubpart next = null;
    	InstructionalOffering offering = getInstrOfferingConfig().getInstructionalOffering();
    	while (next==null) {
    		if (offering==null) break;
    		for (Iterator i=offering.getInstrOfferingConfigs().iterator();i.hasNext();) {
    			InstrOfferingConfig c = (InstrOfferingConfig)i.next();
    			for (Iterator j=c.getSchedulingSubparts().iterator();j.hasNext();) {
    				SchedulingSubpart s = (SchedulingSubpart)j.next();
            		if (canEdit && !s.isEditableBy(user)) continue;
            		if (canView && !s.isViewableBy(user)) continue;
    				if (offering.equals(getInstrOfferingConfig().getInstructionalOffering()) && cmp.compare(this, s)>=0) continue;
    				if (next==null || cmp.compare(next,s)>0)
    					next = s;
    			}
        	}
    		offering = offering.getNextInstructionalOffering(session, cmp, user, canEdit, canView);
    	}
    	return next;
    }

    public SchedulingSubpart getPreviousSchedulingSubpart(HttpSession session,Comparator cmp, User user, boolean canEdit, boolean canView) {
    	Long previousId = Navigation.getPrevious(session, Navigation.sSchedulingSubpartLevel, getUniqueId());
    	if (previousId!=null) {
    		if (previousId.longValue()<0) return null;
    		SchedulingSubpart previous = (new SchedulingSubpartDAO()).get(previousId);
    		if (previous==null) return null;
    		if (canEdit && !previous.isEditableBy(user)) return previous.getPreviousSchedulingSubpart(session, cmp, user, canEdit, canView); 
    		if (canView && !previous.isViewableBy(user)) return previous.getPreviousSchedulingSubpart(session, cmp, user, canEdit, canView);
    		return previous;
    	}
    	SchedulingSubpart previous = null;
    	InstructionalOffering offering = getInstrOfferingConfig().getInstructionalOffering();
    	while (previous==null) {
    		if (offering==null) break;
    		for (Iterator i=offering.getInstrOfferingConfigs().iterator();i.hasNext();) {
    			InstrOfferingConfig c = (InstrOfferingConfig)i.next();
    			for (Iterator j=c.getSchedulingSubparts().iterator();j.hasNext();) {
    				SchedulingSubpart s = (SchedulingSubpart)j.next();
            		if (canEdit && !s.isEditableBy(user)) continue;
            		if (canView && !s.isViewableBy(user)) continue;
    				if (offering.equals(getInstrOfferingConfig().getInstructionalOffering()) && cmp.compare(this, s)<=0) continue;
    				if (previous==null || cmp.compare(previous,s)<0)
    					previous = s;
    			}
        	}
    		offering = offering.getPreviousInstructionalOffering(session, cmp, user, canEdit, canView);
    	}
    	return previous;
    }
    
    public String getSchedulingSubpartSuffix() {
    	return getSchedulingSubpartSuffix(true);
    }
    
    public String getSchedulingSubpartSuffix(boolean save) {
    	String suffix = getSchedulingSubpartSuffixCache();
    	if (suffix!=null) return ("-".equals(suffix)?"":suffix);
    	int nrItypes = 0;
    	int nrItypesBefore = 0;
    	
    	SchedulingSubpartComparator cmp = new SchedulingSubpartComparator();
    	
   		for (Iterator j=getInstrOfferingConfig().getSchedulingSubparts().iterator(); j.hasNext();) {
   			SchedulingSubpart ss = (SchedulingSubpart)j.next();
   			if (ss.getItype().equals(getItype())) {
   				nrItypes++;
   				if (cmp.compare(ss,this)<0) nrItypesBefore++;
   			}
    	}
    	
    	if (nrItypes<=1 || nrItypesBefore<1) 
    		suffix = "";
    	else
    		suffix = String.valueOf((char)('a'+(nrItypesBefore-1)));

    	setSchedulingSubpartSuffixCache(suffix.length()==0?"-":suffix);
    	
    	if (save) {
    		(new SchedulingSubpartDAO()).getSession().saveOrUpdate(this);
    		(new SchedulingSubpartDAO()).getSession().flush();
    	}
    	
    	return suffix;
    }
    
    public void deleteAllDistributionPreferences(org.hibernate.Session hibSession) {
		for (Iterator i3=getClasses().iterator();i3.hasNext();) {
			Class_ c = (Class_)i3.next();
			c.deleteAllDistributionPreferences(hibSession);
		}
    	boolean deleted = false;
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
				hibSession.saveOrUpdate(distributionPref);
			}
			i.remove();
    	}
    	if (deleted) hibSession.saveOrUpdate(this);
    }
    
    public int getMaxExpectedCapacity() {
    	int ret = 0;
    	for (Iterator i=getClasses().iterator();i.hasNext();) {
    		Class_ c = (Class_)i.next();
    		if (c.getMaxExpectedCapacity()!=null)
    			ret += c.getMaxExpectedCapacity().intValue();
    		else if (c.getExpectedCapacity()!=null) 
    			ret += c.getExpectedCapacity().intValue();
    	}
    	return ret;
    }
    
    public static List findAll(Long sessionId) {
    	return (new SchedulingSubpartDAO()).
    		getSession().
    		createQuery("select distinct s from SchedulingSubpart s where " +
    				"s.instrOfferingConfig.instructionalOffering.session.uniqueId=:sessionId").
    		setLong("sessionId",sessionId.longValue()).
    		list();
    }
    
    /**
     * Check if subpart has atleast two classes managed by different departments
     * @return
     */
    public boolean hasMixedManagedClasses() {
        HashMap map = new HashMap();
    	for (Iterator i=getClasses().iterator();i.hasNext();) {
    		Class_ c = (Class_)i.next();
    		map.put(c.getManagingDept().getUniqueId(), "");
    		if (map.size()>1) {
    		    map = null;
    		    return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Check if subpart has atleast one externally managed class
     * @param user
     * @return
     */
    public boolean hasExternallyManagedClasses(User user) {
    	for (Iterator i=getClasses().iterator();i.hasNext();) {
    		Class_ c = (Class_)i.next();
    		if (c.isEditableBy(user) && c.getManagingDept().isExternalManager().booleanValue())
    		    return true;
    	}
    	return false;
    }
    
    public CourseCreditUnitConfig getCredit(){
    	if(this.getCreditConfigs() == null || this.getCreditConfigs().size() != 1){
    		return(null);
    	} else {
    		return((CourseCreditUnitConfig)this.getCreditConfigs().iterator().next());
    	}
    }
    
    public void setCredit(CourseCreditUnitConfig courseCreditUnitConfig){
    	if (this.getCreditConfigs() == null || this.getCreditConfigs().size() == 0){
    		this.addTocreditConfigs(courseCreditUnitConfig);
    	} else if (!this.getCreditConfigs().contains(courseCreditUnitConfig)){
    		this.getCreditConfigs().clear();
    		this.getCreditConfigs().add(courseCreditUnitConfig);
    	} else {
    		//course already contains this config so we do not need to add it again.
    	}
    }
    
    public Object clone(){
    	SchedulingSubpart newSchedulingSubpart = new SchedulingSubpart();
    	newSchedulingSubpart.setAutoSpreadInTime(isAutoSpreadInTime());
    	if (getCreditConfigs() != null){
    		CourseCreditUnitConfig ccuc = null;
    		CourseCreditUnitConfig newCcuc = null;
    		for (Iterator credIt = getCreditConfigs().iterator(); credIt.hasNext();){
    			ccuc = (CourseCreditUnitConfig) credIt.next();
    			newCcuc = (CourseCreditUnitConfig) ccuc.clone();
    			newCcuc.setOwner(newSchedulingSubpart);
    			newSchedulingSubpart.addTocreditConfigs(newCcuc);
    		}
    	}
    	newSchedulingSubpart.setDatePattern(getDatePattern());
    	newSchedulingSubpart.setItype(getItype());
    	newSchedulingSubpart.setMinutesPerWk(getMinutesPerWk());
    	newSchedulingSubpart.setStudentAllowOverlap(isStudentAllowOverlap());
    	return(newSchedulingSubpart);
    }

    public Object cloneWithPreferences(){
    	SchedulingSubpart newSchedulingSubpart = (SchedulingSubpart)clone();
    	if (getPreferences() != null){
			Preference p = null;
			Preference newPref = null;
			for (Iterator prefIt = getPreferences().iterator(); prefIt.hasNext();){
				p = (Preference) prefIt.next();	
				if (!(p instanceof DistributionPref)) {
					newPref = (Preference)p.clone();
					newPref.setOwner(newSchedulingSubpart);
					newSchedulingSubpart.addTopreferences(newPref);
				}
			}
		}
    	return(newSchedulingSubpart);
    }
   
    public Object cloneDeep(){
    	SchedulingSubpart newSchedulingSubpart = (SchedulingSubpart)cloneWithPreferences();
    	HashMap childClassToParentClass = new HashMap();
    	if (getClasses() != null){
    		Class_ origClass = null;
    		Class_ newClass = null;
    		for (Iterator cIt = getClasses().iterator(); cIt.hasNext();){
    			origClass = (Class_) cIt.next();
    			newClass = (Class_) origClass.cloneWithPreferences();
    			newClass.setSchedulingSubpart(newSchedulingSubpart);
    			newSchedulingSubpart.addToclasses(newClass);
    			newClass.setSectionNumberCache(origClass.getSectionNumberCache());
    			newClass.setUniqueIdRolledForwardFrom(origClass.getUniqueId());
    			if (origClass.getChildClasses() != null){
    				Class_ childClass = null;
    				for (Iterator ccIt = origClass.getChildClasses().iterator(); ccIt.hasNext();){
    					childClass = (Class_) ccIt.next();
    					childClassToParentClass.put(childClass.getUniqueId(), newClass);
    				}
    			}
    		}
    	}
    	if (getChildSubparts() != null){
    		SchedulingSubpart origChildSubpart = null;
    		SchedulingSubpart newChildSubpart = null;
    		for (Iterator ssIt = getChildSubparts().iterator(); ssIt.hasNext();){
    			origChildSubpart = (SchedulingSubpart) ssIt.next();
    			newChildSubpart = (SchedulingSubpart)origChildSubpart.cloneDeep();
    			newChildSubpart.setParentSubpart(newSchedulingSubpart);
    			newSchedulingSubpart.addTochildSubparts(newChildSubpart);
    			if (newChildSubpart.getClasses() != null){
    				Class_ newChildClass = null;
    				Class_ newParentClass = null;
    				for (Iterator nccIt = newChildSubpart.getClasses().iterator(); nccIt.hasNext();){
    					newChildClass = (Class_) nccIt.next();
    					newParentClass = (Class_) childClassToParentClass.get(newChildClass.getUniqueIdRolledForwardFrom());
    					newChildClass.setParentClass(newParentClass);
    					newParentClass.addTochildClasses(newChildClass);
    					newChildClass.setUniqueIdRolledForwardFrom(null);
    				}
    			}
    		}
    	}
    	if (newSchedulingSubpart.getClasses() != null && getParentSubpart() == null){
    		Class_ newClass = null;
    		for (Iterator cIt = getClasses().iterator(); cIt.hasNext();){
    			newClass = (Class_) cIt.next();
    			newClass.setUniqueIdRolledForwardFrom(null);
    		}
    	}	
    	return(newSchedulingSubpart);
    }
    
    public static SchedulingSubpart findByIdRolledForwardFrom(Long sessionId, Long uniqueIdRolledForwardFrom) {
        return (SchedulingSubpart)new SchedulingSubpartDAO().
            getSession().
            createQuery("select ss from SchedulingSubpart ss where ss.instrOfferingConfig.instructionalOffering.session.uniqueId=:sessionId and ss.uniqueIdRolledForwardFrom=:uniqueIdRolledForwardFrom").
            setLong("sessionId", sessionId.longValue()).
            setLong("uniqueIdRolledForwardFrom", uniqueIdRolledForwardFrom.longValue()).
            setCacheable(true).
            uniqueResult();
    }

}
