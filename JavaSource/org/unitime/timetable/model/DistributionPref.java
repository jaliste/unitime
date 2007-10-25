/*
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org, and individual contributors
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.ToolBox;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.unitime.commons.Debug;
import org.unitime.timetable.model.base.BaseDistributionPref;
import org.unitime.timetable.model.dao.DistributionPrefDAO;
import org.unitime.timetable.model.dao.PreferenceGroupDAO;
import org.unitime.timetable.model.dao._RootDAO;

public class DistributionPref extends BaseDistributionPref {
	private static final long serialVersionUID = 1L;

	/** Request Attribute name for Dist Prefs **/
	public static final String DIST_PREF_REQUEST_ATTR = "distPrefs";
	
	public static final int sGroupingNone = 0;
	public static final int sGroupingProgressive = 1;
	public static final int sGroupingByTwo = 2;
	public static final int sGroupingByThree = 3;
	public static final int sGroupingByFour = 4;
	public static final int sGroupingByFive = 5;
	public static final int sGroupingPairWise = 6;
	
	//TODO put this stuff into the database (as a some kind of DistributionPreferenceGroupingType object)
	public static String[] sGroupings = new String[] { "All Classes", "Progressive", "Groups of Two", "Groups of Three", "Groups of Four", "Groups of Five", "Pairwise"};
	public static String[] sGroupingsSufix = new String[] {""," Progressive"," Groups of Two"," Groups of Three"," Groups of Four"," Groups of Five", " Pairwise"};
	public static String[] sGroupingsSufixShort = new String[] {""," Prg"," Go2"," Go3"," Go4"," Go5", " Pair"};
	public static String[] sGroupingsDescription = new String[] {
		//All Classes
		"The constraint will apply to all classes in the selected distribution set. "+
		"For example, a Back-to-Back constraint among three classes seeks to place all three classes "+
		"sequentially in time such that there are no intervening class times (transition time between "+
		"classes is taken into account, e.g., if the first class ends at 8:20, the second has to start at 8:30).",
		//Progressive
		"The distribution constraint is created between classes in one scheduling subpart and the "+
		"appropriate class(es) in one or more other subparts. This structure links child and parent "+
		"classes together if subparts have been grouped. Otherwise the first class in one subpart is "+
		"linked to the the first class in the second subpart, etc.",
		//Groups of Two
		"The distribution constraint is applied only on subsets containing two classes in the selected "+
		"distribution set.  A constraint is posted between the first two classes (in the order listed), "+
		"then between the second two classes, etc.",
		//Groups of Three
		"The distribution constraint is applied only on subsets containing three classes in the selected "+
		"distribution set.  A constraint is posted between the first three classes (in the order listed), "+
		"then between the second three classes, etc.",
		//Groups of Four
		"The distribution constraint is applied only on subsets containing four classes in the selected "+
		"distribution set.  A constraint is posted between the first four classes (in the order listed), "+
		"then between the second four classes, etc.",
		//Groups of Five
		"The distribution constraint is applied only on subsets containing five classes in the selected "+
		"distribution set.  A constraint is posted between the first five classes (in the order listed), "+
		"then between the second five classes, etc.",
		//Pairwise
		"The distribution constraint is created between every pair of classes in the selected distribution set. "+
		"Therefore, if n classes are in the set, n(n-1)/2 constraints will be posted among the classes. "+
		"This structure should not be used with \"required\" or \"prohibited\" preferences on sets containing "+
		"more than a few classes."
	};
	
/*[CONSTRUCTOR MARKER BEGIN]*/
	public DistributionPref () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public DistributionPref (java.lang.Long uniqueId) {
		super(uniqueId);
	}

	/**
	 * Constructor for required fields
	 */
	public DistributionPref (
		java.lang.Long uniqueId,
		org.unitime.timetable.model.PreferenceGroup owner,
		org.unitime.timetable.model.PreferenceLevel prefLevel) {

		super (
			uniqueId,
			owner,
			prefLevel);
	}

/*[CONSTRUCTOR MARKER END]*/
	
	public String preferenceText() {
		return preferenceText(false, false, "<BR>", "<BR>", "");
	}

    public String preferenceText(boolean includeDistrObjects, boolean abbv, String objQuotationLeft, String objSeparator, String objQuotationRight) {
    	StringBuffer sb = new StringBuffer();
    	if (abbv) {
    		sb.append(getDistributionType().getAbbreviation().replaceAll("<","&lt;").replaceAll(">","&gt;"));
    		sb.append(sGroupingsSufixShort[getGrouping()==null?0:getGrouping().intValue()]);
    	} else {
    		sb.append(getDistributionType().getLabel());
    		sb.append(sGroupingsSufix[getGrouping()==null?0:getGrouping().intValue()]);
    	}
    	if (includeDistrObjects) {
    		if (getDistributionObjects()!=null && !getDistributionObjects().isEmpty()) {
    			sb.append(objQuotationLeft);
    			for (Iterator it=getOrderedSetOfDistributionObjects().iterator();it.hasNext();) {
    				DistributionObject distObj = (DistributionObject) it.next();;
    				sb.append(distObj.preferenceText());
    				if (it.hasNext())
    					sb.append(objSeparator);
    			}
    			sb.append(objQuotationRight);
    		} else if (getOwner() instanceof DepartmentalInstructor) {
    			sb.append(objQuotationLeft);
    			for (Iterator it=((DepartmentalInstructor)getOwner()).getClasses().iterator();it.hasNext();) {
    				ClassInstructor ci = (ClassInstructor)it.next();
    				sb.append(ci.getClassInstructing().getClassLabel());
    				if (it.hasNext())
    					sb.append(objSeparator);
    			}
    			sb.append(objQuotationRight);
    		}
    	}
    	return(sb.toString());
    }
    
    protected String preferenceHtml() {
    	StringBuffer sb = new StringBuffer();
    	String color = getPrefLevel().prefcolor();
    	if (PreferenceLevel.sNeutral.equals(getPrefLevel().getPrefProlog()))
    		color = "gray";
    	sb.append("<span style='color:"+color+";font-weight:bold;' title='"+getPrefLevel().getPrefName()+" "+preferenceText(true,false," (",", ",")")+"'>" );
    	sb.append(preferenceText(false,true, "", "", ""));
    	sb.append("</span>");
    	return sb.toString();
    }
    
	/**
	 * @param schedulingSubpart_
	 * @return
	 */
	public boolean appliesTo(SchedulingSubpart schedulingSubpart) {
		if (this.getDistributionObjects()==null) return false;
		for (Iterator it=this.getDistributionObjects().iterator();it.hasNext();) {
			DistributionObject dObj = (DistributionObject) it.next();
			
			//SchedulingSubpart check
			//no checking whether dObj.getPrefGroup() is SchedulingSubpart not needed since all PreferenceGroups have unique ids
			if (dObj.getPrefGroup().getUniqueId().equals(schedulingSubpart.getUniqueId())) return true;
		}
		return false;
	}

	/**
	 * @param aClass
	 * @return
	 */
	public boolean appliesTo(Class_ aClass) {
		if (this.getDistributionObjects()==null) return false;
		Iterator it = null;
		try {
			it = getDistributionObjects().iterator();
		} catch (ObjectNotFoundException e) {
			Debug.error("Exception "+e.getMessage()+" seen for "+this);
    		new _RootDAO().getSession().refresh(this);
   			it = getDistributionObjects().iterator();
		}
		while (it.hasNext()) {
			DistributionObject dObj = (DistributionObject) it.next();
			
			//Class_ check
			//no checking whether dObj.getPrefGroup() is Class_ not needed since all PreferenceGroups have unique ids
			if (dObj.getPrefGroup().getUniqueId().equals(aClass.getUniqueId())) return true;
			
			//SchedulingSubpart check
			SchedulingSubpart ss = null;
			if (Hibernate.isInitialized(dObj.getPrefGroup())) {
				if (dObj.getPrefGroup() instanceof SchedulingSubpart) {
					ss = (SchedulingSubpart) dObj.getPrefGroup();
				}
			} else {
				//dObj.getPrefGroup() is a proxy -> try to load it
				PreferenceGroup pg = (new PreferenceGroupDAO()).get(dObj.getPrefGroup().getUniqueId());
				if (pg!=null && pg instanceof SchedulingSubpart)
					ss = (SchedulingSubpart)pg;
			}
			if (ss!=null && ss.getClasses()!=null && ss.getClasses().size()>0) {
				for (Iterator it2 = ss.getClasses().iterator();it2.hasNext();)
					if (((Class_)it2.next()).getUniqueId().equals(aClass.getUniqueId())) return true;
			}
		}
		return false;
	}
	
	// overide default
	public boolean appliesTo(PreferenceGroup group) {
		if (group instanceof Class_)
			return appliesTo((Class_)group);
		if (group instanceof SchedulingSubpart)
			return appliesTo((SchedulingSubpart)group);
		return false;
	}

    public int compareTo(Object o) {
   		DistributionPref p = (DistributionPref)o;
   		int cmp = getDistributionType().getReference().compareTo(p.getDistributionType().getReference()); 
   		if (cmp!=0) return cmp;
   		
   		return getUniqueId().compareTo(p.getUniqueId());
   }
    
    public Object clone() {
    	DistributionPref pref = new DistributionPref();
    	pref.setPrefLevel(getPrefLevel());
    	pref.setDistributionObjects(getDistributionObjects());
    	pref.setDistributionType(getDistributionType());
    	return pref;
    }
    public boolean isSame(Preference other) {
    	if (other==null || !(other instanceof DistributionPref)) return false;
    	return ToolBox.equals(getDistributionType(),((DistributionPref)other).getDistributionType()) && ToolBox.equals(getDistributionObjects(),((DistributionPref)other).getDistributionObjects());
    }
    /** Ordered set of distribution objects */
    public Set getOrderedSetOfDistributionObjects() {
    	try {
    		return new TreeSet(getDistributionObjects());
    	} catch (ObjectNotFoundException ex) {
    		(new DistributionPrefDAO()).getSession().refresh(this);
    		return new TreeSet(getDistributionObjects());
    	}
    }
    
    public String getGroupingName() {
    	return sGroupings[getGrouping()==null?0:getGrouping().intValue()];
    }
    public String getGroupingSufix() {
    	return sGroupingsSufix[getGrouping()==null?0:getGrouping().intValue()];
    }
    
    public static String getGroupingDescription(int grouping) {
    	return sGroupingsDescription[grouping];
    }
    
    public static Collection getPreferences(Long sessionId, Long ownerId, boolean useControllingCourseOfferingManager, Long uniqueId) {
    	return getPreferences(sessionId, ownerId, useControllingCourseOfferingManager, uniqueId, null, null);
    }
    
    public static Collection getPreferences(Long sessionId, Long ownerId, boolean useControllingCourseOfferingManager, Long uniqueId, Long subjectAreaId, String courseNbr) {
    	if (sessionId==null) return null;
    	StringBuffer sb = new StringBuffer();
    	sb.append("select distinct dp ");
    	sb.append(" from ");
    	sb.append(" DistributionPref as dp ");
    	sb.append(" inner join dp.distributionObjects as do, ");
    	sb.append(" Class_ as c ");
    	sb.append(" inner join c.schedulingSubpart as ss inner join ss.instrOfferingConfig.instructionalOffering as io ");
    	if(subjectAreaId != null || ownerId != null){
    		sb.append(" inner join io.courseOfferings as co ");
    	}
    	sb.append("where ");
    	sb.append(" (c.uniqueId = do.prefGroup.uniqueId or ss.uniqueId = do.prefGroup.uniqueId) and ");
    	sb.append(" io.session.uniqueId = :sessionId ");
    	if (ownerId != null){
    		sb.append(" and (");
    		sb.append("((c.managingDept is not null and c.managingDept.uniqueId = :ownerId )");
    		sb.append(" or (c.managingDept is null ");
    		sb.append(" and co.isControl = true ");
    		sb.append("and co.subjectArea.department.uniqueId = :ownerId))");
    		if (useControllingCourseOfferingManager)	{
    			sb.append(" or (co.isControl = true");
    			sb.append(" and co.subjectArea.department.uniqueId = :ownerId)");
    		}
    		sb.append(")");
    	}
    	if (uniqueId != null){
    		sb.append(" and (c.uniqueId = :uniqueId or ss.uniqueId = :uniqueId or io.uniqueId = :uniqueId))");
    	}
    	if(subjectAreaId != null){
    	    sb.append(" and co.subjectArea.uniqueId=:subjectAreaId ");
    	    
    		if (courseNbr!=null && courseNbr.trim().length()>0) {
    		    sb.append(" and co.courseNbr ");
    		    if (courseNbr.indexOf('*')>=0) {
    	            sb.append(" like ");
    	            courseNbr = courseNbr.replace('*', '%').toUpperCase();
    		    }
    		    else {
    	            sb.append(" = ");
    		    }
                sb.append(":courseNbr");
    		}		
    	}
	
    	Query q = (new DistributionPrefDAO()).
			getSession().
			createQuery(sb.toString());
    	q.setLong("sessionId", sessionId.longValue());
    	if (ownerId!=null)
    		q.setLong("ownerId", ownerId.longValue());
    	if (uniqueId!=null)
    		q.setLong("uniqueId", uniqueId.longValue());
    	if (subjectAreaId!=null) {
    		q.setLong("subjectAreaId", subjectAreaId.longValue());
    		if (courseNbr!=null && courseNbr.trim().length()>0)
    		    q.setString("courseNbr", courseNbr.toUpperCase());
    	}
    	return q.list();
    }
    
    public static Collection getInstructorPreferences(Long sessionId, Long ownerId, Long subjectAreaId, String courseNbr) {
        if (sessionId==null) return null;
        StringBuffer sb = new StringBuffer();
        sb.append("select distinct dp ");
        sb.append(" from ");
        sb.append(" DistributionPref as dp, ");
        sb.append(" DepartmentalInstructor as di ");
        if (subjectAreaId!=null) {
            sb.append(" inner join di.classes as ci inner join ci.classInstructing.schedulingSubpart.instrOfferingConfig.instructionalOffering.courseOfferings as co ");
        }
        sb.append("where ");
        sb.append(" dp.owner = di ");
        sb.append(" and di.department.session.uniqueId = :sessionId ");
        if (subjectAreaId!=null) {
            sb.append(" and ci.lead = true ");
            sb.append(" and co.isControl = true ");
            sb.append(" and co.subjectArea.uniqueId = :subjectAreaId ");
            if (courseNbr!=null && courseNbr.trim().length()>0) {
                sb.append(" and co.courseNbr ");
                if (courseNbr.indexOf('*')>=0) {
                    sb.append(" like ");
                    courseNbr = courseNbr.replace('*', '%').toUpperCase();
                } else {
                    sb.append(" = ");
                }
                sb.append(":courseNbr");
            }       
        }
        if (ownerId != null) {
            sb.append(" and di.department.uniqueId = :ownerId ");
        }

        Query q = (new DistributionPrefDAO()).
            getSession().
            createQuery(sb.toString());
        q.setLong("sessionId", sessionId.longValue());
        if (ownerId!=null)
            q.setLong("ownerId", ownerId.longValue());
        if (subjectAreaId!=null) {
            q.setLong("subjectAreaId", subjectAreaId.longValue());
            if (courseNbr!=null && courseNbr.trim().length()>0)
                q.setString("courseNbr", courseNbr.toUpperCase());
        }
        return q.list();
    }
    
    public boolean isEditable(Session session, TimetableManager manager) {
    	if (manager == null) return false;
    	
    	Department d = null;
    	if (getOwner() instanceof DepartmentalInstructor) {
    		d = ((DepartmentalInstructor)getOwner()).getDepartment();
    	} else {
    		d = (Department) getOwner();
    	}
    	
    	if (!getDistributionType().isApplicable(d)) return false;

    	//preference owner by the given manager
    	if (manager.getDepartments().contains(d)) {
    		if (d.isExternalManager().booleanValue()) {
    			if (d.effectiveStatusType().canManagerEdit()) return true;
    		} else {
    			if (d.effectiveStatusType().canOwnerEdit()) return true;
    		}
    		return false;
    	}
    	
    	if (d.isExternalManager().booleanValue() && d.effectiveStatusType().canOwnerEdit()) {
       		for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
       			DistributionObject distrObj = (DistributionObject)i.next();
       			if (distrObj.getPrefGroup() instanceof Class_) {
       				Class_ clazz = (Class_)distrObj.getPrefGroup();
       				if (!manager.getDepartments().contains(clazz.getControllingDept())) return false;
       			} else if (distrObj.getPrefGroup() instanceof SchedulingSubpart) {
       				SchedulingSubpart subpart = (SchedulingSubpart)distrObj.getPrefGroup();
       				if (!manager.getDepartments().contains(subpart.getControllingDept())) return false;
       			}
       		}
       		return true;
    	}
        	
     	//else -> class manager of all classes / subparts
    	if (d.effectiveStatusType().canManagerEdit()) {
       		for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
       			DistributionObject distrObj = (DistributionObject)i.next();
       			PreferenceGroup pg = distrObj.getPrefGroup();
       			if (distrObj.getPrefGroup() instanceof Class_) {
       				Class_ clazz = (Class_)distrObj.getPrefGroup();
       				if (!manager.getDepartments().contains(clazz.getManagingDept())) return false;
       			} else if (distrObj.getPrefGroup() instanceof SchedulingSubpart) {
       				SchedulingSubpart subpart = (SchedulingSubpart)distrObj.getPrefGroup();
       				if (!manager.getDepartments().contains(subpart.getControllingDept())) return false;
       			}
       		}
       		
    		return true;
    	}    	
    	
    	return false;
    	
    }
    
    public boolean isVisible(Session session, Department department) {
    	if (department == null) return false;
    	
    	Department d = null;
    	if (getOwner() instanceof DepartmentalInstructor) {
    		d = ((DepartmentalInstructor)getOwner()).getDepartment();
    	} else {
    		d = (Department) getOwner();
    	}

    	if (department.isExternalManager().booleanValue() && d.effectiveStatusType().canManagerView()) return true;
    	
    	if (department.equals(d) && department.effectiveStatusType().canOwnerView()) return true;

    	//LLR/LAB manager or schedule deputy edit phase -> ownership or controlling course offering manager of one class is enough
    	if (!department.isExternalManager().booleanValue() && department.effectiveStatusType().canOwnerView()) {
       		for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
       			DistributionObject distrObj = (DistributionObject)i.next();
        		PreferenceGroup pg = distrObj.getPrefGroup();
        		if (distrObj.getPrefGroup() instanceof Class_) {
        			Class_ clazz = (Class_)distrObj.getPrefGroup();
        			if (clazz.getManagingDept().effectiveStatusType().canOwnerEdit() && department.getUniqueId().equals(clazz.getControllingDept().getUniqueId())) return true;
        			if (clazz.getManagingDept().effectiveStatusType().canManagerEdit() && department.getUniqueId().equals(clazz.getManagingDept().getUniqueId())) return true;
        		} else if (distrObj.getPrefGroup() instanceof SchedulingSubpart) {
        			SchedulingSubpart subpart = (SchedulingSubpart)distrObj.getPrefGroup();
        			if (subpart.getManagingDept().effectiveStatusType().canOwnerEdit() && department.getUniqueId().equals(subpart.getControllingDept().getUniqueId())) return true;
        			if (subpart.getManagingDept().effectiveStatusType().canManagerEdit() && department.getUniqueId().equals(subpart.getManagingDept().getUniqueId())) return true;
        		}
        	}
        	return false;
    	}
    	
    	if (department.isExternalManager().booleanValue() && department.effectiveStatusType().canManagerView()) {
        	//else -> class manager of all classes / subparts
       		for (Iterator i=getDistributionObjects().iterator();i.hasNext();) {
       			DistributionObject distrObj = (DistributionObject)i.next();
       			PreferenceGroup pg = distrObj.getPrefGroup();
       			if (distrObj.getPrefGroup() instanceof Class_) {
       				Class_ clazz = (Class_)distrObj.getPrefGroup();
       				if (!department.getUniqueId().equals(clazz.getManagingDept().getUniqueId())) return false;
       			} else if (distrObj.getPrefGroup() instanceof SchedulingSubpart) {
       				SchedulingSubpart subpart = (SchedulingSubpart)distrObj.getPrefGroup();
       				if (!department.getUniqueId().equals(subpart.getManagingDept().getUniqueId())) return false;
       			}
       		}
        	
        	return true;
    	}
    	
    	return false;
    }
    
    public boolean weakenHardPreferences() {
    	if (PreferenceLevel.sRequired.equals(getPrefLevel().getPrefProlog())) {
    		if (getDistributionType().getAllowedPref().indexOf(PreferenceLevel.sCharLevelStronglyPreferred)>=0)
    			setPrefLevel(PreferenceLevel.getPreferenceLevel(PreferenceLevel.sStronglyPreferred));
    		else
    			return false;
    	}
    	if (PreferenceLevel.sProhibited.equals(getPrefLevel().getPrefProlog())) {
    		if (getDistributionType().getAllowedPref().indexOf(PreferenceLevel.sCharLevelStronglyDiscouraged)>=0)
    			setPrefLevel(PreferenceLevel.getPreferenceLevel(PreferenceLevel.sStronglyDiscouraged));
    		else
    			return false;
    	}
    	return true;
    }
    
    public static DistributionPref findByIdRolledForwardFrom(Long uidRolledForwardFrom) {
        return (DistributionPref)new DistributionPrefDAO().
            getSession().
            createQuery(
                "select dp from DistributionPref dp where "+
                "dp.uniqueIdRolledForwardFrom=:uidRolledFrom").
            setLong("uidRolledFrom", uidRolledForwardFrom).
            setCacheable(true).
            uniqueResult(); 
    }
}