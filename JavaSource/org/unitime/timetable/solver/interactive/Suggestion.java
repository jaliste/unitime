/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008 - 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.solver.interactive;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpSession;

import org.dom4j.Element;
import org.unitime.commons.Debug;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.interactive.Hint.HintComparator;
import org.unitime.timetable.solver.ui.GroupConstraintInfo;
import org.unitime.timetable.solver.ui.JenrlInfo;

import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.criteria.BackToBackInstructorPreferences;
import net.sf.cpsolver.coursett.criteria.BrokenTimePatterns;
import net.sf.cpsolver.coursett.criteria.DepartmentBalancingPenalty;
import net.sf.cpsolver.coursett.criteria.DistributionPreferences;
import net.sf.cpsolver.coursett.criteria.Perturbations;
import net.sf.cpsolver.coursett.criteria.RoomPreferences;
import net.sf.cpsolver.coursett.criteria.SameSubpartBalancingPenalty;
import net.sf.cpsolver.coursett.criteria.StudentCommittedConflict;
import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.criteria.StudentDistanceConflict;
import net.sf.cpsolver.coursett.criteria.StudentHardConflict;
import net.sf.cpsolver.coursett.criteria.TimePreferences;
import net.sf.cpsolver.coursett.criteria.TooBigRooms;
import net.sf.cpsolver.coursett.criteria.UselessHalfHours;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * @author Tomas Muller
 */
public class Suggestion implements Serializable, Comparable {
	private static final long serialVersionUID = 1L;
    private static java.text.DecimalFormat sDF = new java.text.DecimalFormat("0.000",new java.text.DecimalFormatSymbols(Locale.US));
    private double iValue = 0;
    private Vector iDifferentAssignments = null;
    private int iTooBigRooms = 0;
    private long iUselessSlots = 0;
    private double iGlobalTimePreference = 0;
    private long iGlobalRoomPreference = 0;
    private long iGlobalGroupConstraintPreference = 0;
    private long iViolatedStudentConflicts = 0;
    private long iHardStudentConflicts = 0;
    private long iDistanceStudentConflicts = 0;
    private long iCommitedStudentConflicts = 0;
    private long iInstructorDistancePreference = 0;
    private int iDepartmentSpreadPenalty = 0;
    private int iUnassignedVariables = 0;
    private double iPerturbationPenalty = 0;
    private int iSpreadPenalty = 0;
    private HashSet iUnresolvedConflicts = null;
    private Hint iHint = null;
    private Vector iStudentConflictInfos = null;
    private Vector iGroupConstraintInfos = null;
    private Vector iBtbInstructorInfos = null;
    
    public Suggestion(Solver solver) {
    	this(solver, null, null, null);
    }
    
    public Suggestion() {
    	
    }
    
    public Suggestion(Solver<Lecture, Placement> solver, Hashtable<Lecture, Placement> initialAssignments, Vector order, Collection unresolvedConflicts) {
    	if (unresolvedConflicts!=null) {
    		iUnresolvedConflicts = new HashSet();
    		for (Iterator i=unresolvedConflicts.iterator();i.hasNext();)
    			iUnresolvedConflicts.add(new Hint(solver, (Placement)i.next()));
    	}
        if (initialAssignments!=null) {
            iDifferentAssignments = new Vector();
            iBtbInstructorInfos = new Vector();
        	HashSet jenrls = new HashSet();
        	HashSet gcs = new HashSet();
        	Hashtable committed = new Hashtable();
        	for (Lecture lecture: solver.currentSolution().getModel().assignedVariables()) {
        		Placement p = lecture.getAssignment();
        		Placement ini = (Placement)initialAssignments.get(p.variable());
        		if (ini==null || !ini.equals(p)) {
        			iDifferentAssignments.add(new Hint(solver, p));  
        			jenrls.addAll(lecture.activeJenrls());
        			if (p.getCommitedConflicts()>0) {
        				Hashtable x = new Hashtable();
        				for (Iterator i=lecture.students().iterator();i.hasNext();) {
        					Student s = (Student)i.next();
        					Set confs = s.conflictPlacements(p);
        					if (confs==null) continue;
        					for (Iterator j=confs.iterator();j.hasNext();) {
        						Placement commitedPlacement = (Placement)j.next();
        						Integer current = (Integer)x.get(commitedPlacement);
        						x.put(commitedPlacement, new Integer(1+(current==null?0:current.intValue())));
        					}
        				}
        				committed.put(p,x);
        			}
        			gcs.addAll(lecture.groupConstraints());
        			for (InstructorConstraint ic: lecture.getInstructorConstraints()) {
        			    for (Lecture other: ic.variables()) {
        			        if (other.equals(lecture) || other.getAssignment()==null) continue;
        			        int pref = ic.getDistancePreference(p, (Placement)other.getAssignment());
        			        if (pref==PreferenceLevel.sIntLevelNeutral) continue;
        	            	Hint h1 = new Hint(solver, p);
        	            	Hint h2 = new Hint(solver, (Placement)other.getAssignment());
        			        iBtbInstructorInfos.add(new BtbInstructorInfo(h1,h2,lecture.getInstructorName(),pref));
        			    }
        			}
        		}
        	}
            if (order!=null)
            	Collections.sort(iDifferentAssignments,new HintComparator(order));
            iStudentConflictInfos = new Vector(jenrls.size());
            for (Iterator i=jenrls.iterator();i.hasNext();) {
            	JenrlConstraint jenrl = (JenrlConstraint)i.next();
            	Hint h1 = new Hint(solver, (Placement)jenrl.first().getAssignment());
            	Hint h2 = new Hint(solver, (Placement)jenrl.second().getAssignment());
            	int i1 = iDifferentAssignments.indexOf(h1);
            	int i2 = iDifferentAssignments.indexOf(h2);
            	if (i2<0 || (i1>=0 && i1<i2))
            		iStudentConflictInfos.add(new StudentConflictInfo(h1,h2,new JenrlInfo(jenrl)));
            	else
            		iStudentConflictInfos.add(new StudentConflictInfo(h2,h1,new JenrlInfo(jenrl)));
            }
            for (Iterator i=committed.entrySet().iterator();i.hasNext();) {
            	Map.Entry x = (Map.Entry)i.next();
            	Placement p1 = (Placement)x.getKey();
            	Lecture l1 = (Lecture)p1.variable();
            	for (Iterator j=((Hashtable)x.getValue()).entrySet().iterator();j.hasNext();) {
            		Map.Entry y = (Map.Entry)j.next();
            		Placement p2 = (Placement)y.getKey();
            		Integer cnt = (Integer)y.getValue();
                	Hint h1 = new Hint(solver, p1);
                	Hint h2 = new Hint(solver, p2);
                	JenrlInfo jenrl = new JenrlInfo();
                	jenrl.setIsCommited(true);
                	jenrl.setJenrl(cnt.intValue());
                	jenrl.setIsFixed(l1.nrTimeLocations()==1);
                	jenrl.setIsHard(l1.isSingleSection());
                	jenrl.setIsDistance(!p1.getTimeLocation().hasIntersection(p2.getTimeLocation()));
                	iStudentConflictInfos.add(new StudentConflictInfo(h1,h2,jenrl));
            	}
            }
            iGroupConstraintInfos = new Vector();
            for (Iterator i=gcs.iterator();i.hasNext();) {
            	GroupConstraint gc = (GroupConstraint)i.next();
            	if (gc.isSatisfied()) continue;
				DistributionInfo dist = new DistributionInfo(new GroupConstraintInfo(gc));
				for (Lecture another: gc.variables()) {
					if (another.getAssignment()!=null)
						dist.addHint(new Hint(solver, (Placement)another.getAssignment()));
				}
				iGroupConstraintInfos.addElement(dist);
            }
        }
        iValue = solver.currentSolution().getModel().getTotalValue();
        TimetableModel m = (TimetableModel)solver.currentSolution().getModel();
        iTooBigRooms = (int)Math.round(m.getCriterion(TooBigRooms.class).getValue());
        iUselessSlots = Math.round(m.getCriterion(UselessHalfHours.class).getValue() + m.getCriterion(BrokenTimePatterns.class).getValue());
        iGlobalTimePreference = m.getCriterion(TimePreferences.class).getValue();
        iGlobalRoomPreference = Math.round(m.getCriterion(RoomPreferences.class).getValue());
        iGlobalGroupConstraintPreference = Math.round(m.getCriterion(DistributionPreferences.class).getValue());
        iViolatedStudentConflicts = Math.round(m.getCriterion(StudentConflict.class).getValue() + m.getCriterion(StudentCommittedConflict.class).getValue());
        iHardStudentConflicts = Math.round(m.getCriterion(StudentHardConflict.class).getValue());
        iDistanceStudentConflicts = Math.round(m.getCriterion(StudentDistanceConflict.class).getValue());
        iCommitedStudentConflicts = Math.round(m.getCriterion(StudentCommittedConflict.class).getValue());
        iInstructorDistancePreference = Math.round(m.getCriterion(BackToBackInstructorPreferences.class).getValue());
        iDepartmentSpreadPenalty = (int)Math.round(m.getCriterion(DepartmentBalancingPenalty.class).getValue());
        iSpreadPenalty = (int)Math.round(m.getCriterion(SameSubpartBalancingPenalty.class).getValue());
        iUnassignedVariables = m.unassignedVariables().size();
        iPerturbationPenalty = m.getCriterion(Perturbations.class).getValue();
    }
    
    public Suggestion(Solver solver, Lecture lecture, TimeLocation time) {
    	iStudentConflictInfos = new Vector();
    	Placement currentPlacement = (Placement)lecture.getAssignment();
    	if (currentPlacement==null)
    		currentPlacement = (lecture.values().isEmpty()?null:lecture.values().get(0));
    	
    	Hashtable committed = new Hashtable();
    	if (currentPlacement!=null) {
        	Placement dummyPlacement = null;
    		if (currentPlacement.isMultiRoom())
    			dummyPlacement = new Placement(lecture,time,currentPlacement.getRoomLocations());
    		else
    			dummyPlacement = new Placement(lecture,time,currentPlacement.getRoomLocation());
    		
        	iDifferentAssignments = new Vector();
        	iDifferentAssignments.addElement(new Hint(solver, dummyPlacement));
        	
			if (dummyPlacement.getCommitedConflicts()>0) {
				for (Iterator i=lecture.students().iterator();i.hasNext();) {
					Student s = (Student)i.next();
					Set confs = s.conflictPlacements(dummyPlacement);
					if (confs==null) continue;
					for (Iterator j=confs.iterator();j.hasNext();) {
						Placement commitedPlacement = (Placement)j.next();
						Integer current = (Integer)committed.get(commitedPlacement);
						committed.put(commitedPlacement, new Integer(1+(current==null?0:current.intValue())));
					}
				}
			}

			for (JenrlConstraint jenrl: lecture.jenrlConstraints()) {
        		long j = jenrl.jenrl(lecture, dummyPlacement);
        		if (j>0) {
        			//if (lecture.getAssignment()==null && jenrl.areStudentConflictsDistance(dummyPlacement)) continue;
        			if (jenrl.areStudentConflictsDistance(dummyPlacement)) continue;
        			JenrlInfo jInfo = new JenrlInfo();
        			jInfo.setJenrl(j);
        			iViolatedStudentConflicts += j;
        			if (jenrl.areStudentConflictsHard()) {
        				iHardStudentConflicts += j;
        				jInfo.setIsHard(true);
        			}
        			if (jenrl.areStudentConflictsDistance(dummyPlacement)) {
        				iDistanceStudentConflicts += j;
        				jInfo.setIsDistance(true);
        			}
        			if (jenrl.first().equals(lecture)) {
        				Hint h = new Hint(solver, (Placement)jenrl.second().getAssignment());
                    	iStudentConflictInfos.add(new StudentConflictInfo(h,null,jInfo));
        			} else {
        				Hint h = new Hint(solver, (Placement)jenrl.first().getAssignment());
                    	iStudentConflictInfos.add(new StudentConflictInfo(h,null,jInfo));
        			}
        		}
        	}
			
			for (Iterator i=committed.entrySet().iterator();i.hasNext();) {
				Map.Entry x = (Map.Entry)i.next();
				Placement p = (Placement)x.getKey();
				Integer cnt = (Integer)x.getValue();
            	Hint h = new Hint(solver, p);
            	JenrlInfo jenrl = new JenrlInfo();
            	jenrl.setIsCommited(true);
            	jenrl.setJenrl(cnt.intValue());
            	iViolatedStudentConflicts += cnt.intValue();
            	jenrl.setIsFixed(lecture.nrTimeLocations()==1);
            	jenrl.setIsHard(lecture.isSingleSection());
            	jenrl.setIsDistance(!dummyPlacement.getTimeLocation().hasIntersection(p.getTimeLocation()));
            	iStudentConflictInfos.add(new StudentConflictInfo(h,null,jenrl));
			}
        	iGroupConstraintInfos = new Vector();
        	for (Iterator i=lecture.groupConstraints().iterator();i.hasNext();) {
        		GroupConstraint gc = (GroupConstraint)i.next();
        		if (gc.getType() == GroupConstraint.ConstraintType.SAME_ROOM) continue;
        		int curPref = gc.getCurrentPreference(dummyPlacement);
        		if (gc.getType() == GroupConstraint.ConstraintType.BTB) {
        			gc.setType(GroupConstraint.ConstraintType.BTB_TIME);
        			curPref = gc.getCurrentPreference(dummyPlacement);
        			gc.setType(GroupConstraint.ConstraintType.BTB);
        		}
        		if (gc.getType() == GroupConstraint.ConstraintType.SAME_STUDENTS) {
        			gc.setType(GroupConstraint.ConstraintType.DIFF_TIME);
        			curPref = gc.getCurrentPreference(dummyPlacement);
        			gc.setType(GroupConstraint.ConstraintType.SAME_STUDENTS);
        		}
        		boolean sat = (gc.getPreference()<0 && curPref<0) || gc.getPreference()==0 || (gc.getPreference()>0 && curPref==0);
        		if (sat) continue;
        		iGlobalGroupConstraintPreference += Math.abs(curPref==0?gc.getPreference():curPref);
    			DistributionInfo dist = new DistributionInfo(new GroupConstraintInfo(gc));
    			for (Lecture another: gc.variables()) {
    				if (another.equals(lecture)) {
    					//dist.addHint(new Hint(solver, dummyPlacement));
    				} else if (another.getAssignment()!=null)
    					dist.addHint(new Hint(solver, another.getAssignment()));
    			}
    			iGroupConstraintInfos.addElement(dist);
        	}
            iCommitedStudentConflicts = dummyPlacement.getCommitedConflicts();
    	}
    }

    public void setHint(Hint hint) { iHint = hint; }
    public Hint getHint() { return iHint; }
    public Vector getDifferentAssignments() { return iDifferentAssignments; }
    public void assign(HttpSession session) throws Exception {
    	SolverProxy solver = WebSolver.getSolver(session);
    	if (solver==null) return;
    	assign(solver);
    }
    public void assign(SolverProxy solver) throws Exception {
    	solver.assign(iDifferentAssignments);
    }
    public Hashtable conflictInfo(HttpSession session) throws Exception {
    	SolverProxy solver = WebSolver.getSolver(session);
    	if (solver==null) return null;
    	return conflictInfo(solver);
    }
    public Hashtable conflictInfo(SolverProxy solver) throws Exception {
    	return solver.conflictInfo(iDifferentAssignments);
    }
    public int compareTo(Object o) {
        if (o==null || !(o instanceof Suggestion)) return -1;
        int cmp = Double.compare(iValue, ((Suggestion)o).iValue);
        if (cmp!=0) return cmp;
        return iDifferentAssignments.toString().compareTo(((Suggestion)o).iDifferentAssignments.toString());
    }
    public boolean isBetter(Solver solver) {
    	return (iValue < solver.currentSolution().getModel().getTotalValue());
    }
    
    public double getValue() { return iValue; }
    public int getTooBigRooms() { return iTooBigRooms;}
    public long getUselessSlots() { return iUselessSlots;}
    public double getGlobalTimePreference() { return iGlobalTimePreference;}
    public long getGlobalRoomPreference() { return iGlobalRoomPreference;}
    public long getGlobalGroupConstraintPreference() { return iGlobalGroupConstraintPreference;}
    public long getViolatedStudentConflicts() { return iViolatedStudentConflicts;}
    public long getHardStudentConflicts() { return iHardStudentConflicts;}
    public long getCommitedStudentConflicts() { return iCommitedStudentConflicts;}
    public long getDistanceStudentConflicts() { return iDistanceStudentConflicts;}
    public long getInstructorDistancePreference() { return iInstructorDistancePreference;}
    public double getDepartmentSpreadPenalty() { return ((double)iDepartmentSpreadPenalty)/12.0;}
    public double getSpreadPenalty() { return ((double)iSpreadPenalty)/12.0;}
    public int getUnassignedVariables() { return iUnassignedVariables;}
    public double getPerturbationPenalty() { return iPerturbationPenalty;}
    public Set getUnresolvedConflicts() { return iUnresolvedConflicts; }
    
    public boolean hasStudentConflictInfo() {
    	return (iStudentConflictInfos!=null && !iStudentConflictInfos.isEmpty());
    }
    
    public String getStudentConflictInfosAsHtml(HttpSession session, boolean link, int id, int spanLimit) {
    	Collections.sort(iStudentConflictInfos,new StudentConflictInfoComparator(session));
    	StringBuffer sb = new StringBuffer();
    	int idx = 0; boolean span = false;
    	for (Enumeration e=iStudentConflictInfos.elements();e.hasMoreElements();idx++) {
    		StudentConflictInfo info = (StudentConflictInfo)e.nextElement();
    		if (idx==spanLimit) {
    			sb.append("<span id='hint_conf_dots"+id+"' onMouseOver=\"this.style.cursor='hand';this.style.cursor='pointer';\" style='display:inline'><a onClick=\"document.getElementById('hint_conf_dots"+id+"').style.display='none';document.getElementById('hint_conf_rest"+id+"').style.display='inline';\">...</a></span><span id='hint_conf_rest"+id+"' style='display:none'>");
    			span = true;
    		}
    		sb.append(info.toHtml(session, link));
    	}
    	if (span) sb.append("</span>");
    	return sb.toString();
    }
    
    public boolean hasBtbInstructorInfo() {
    	return (iBtbInstructorInfos!=null && !iBtbInstructorInfos.isEmpty());
    }
    
    public String getBtbInstructorInfosAsHtml(HttpSession session, boolean link) {
    	Collections.sort(iBtbInstructorInfos,new BtbInstructorInfoComparator(session));
    	StringBuffer sb = new StringBuffer();
    	int idx = 0; 
    	for (Enumeration e=iBtbInstructorInfos.elements();e.hasMoreElements();idx++) {
    		BtbInstructorInfo info = (BtbInstructorInfo)e.nextElement();
    		sb.append(info.toHtml(session, link));
    	}
    	return sb.toString();
    }

    public boolean hasDistributionConstraintInfo() {
    	return (iGroupConstraintInfos!=null && !iGroupConstraintInfos.isEmpty());
    }
    
    public String getDistributionConstraintInfoAsHtml(HttpSession session, boolean link, int id, int spanLimit) {
    	StringBuffer sb = new StringBuffer();
    	int idx = 0; 
    	boolean span = false;
    	for (Enumeration e=iGroupConstraintInfos.elements();e.hasMoreElements();idx++) {
    		DistributionInfo info = (DistributionInfo)e.nextElement();
    		if (idx==spanLimit) {
    			sb.append("<span id='hint_dist_dots"+id+"' onMouseOver=\"this.style.cursor='hand';this.style.cursor='pointer';\" style='display:inline'><a onClick=\"document.getElementById('hint_dist_dots"+id+"').style.display='none';document.getElementById('hint_dist_rest"+id+"').style.display='inline';\">...</a></span><span id='hint_dist_rest"+id+"' style='display:none'>");
    			span = true;
    		}
    		sb.append(info.toHtml(session, link));
    	}
    	if (span) sb.append("</span>");
    	return sb.toString();
    }
    
    public static class BtbInstructorInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		private Hint iFirst, iSecond;
		private transient ClassAssignmentDetails iFirstInfo = null, iSecondInfo = null;
		private int iPref;
		private String iInsturctor;
		public BtbInstructorInfo() {
		}
		public BtbInstructorInfo(Hint first, Hint second, String instructor, int pref) {
			iFirst = first; iSecond = second;
			iPref = pref; iInsturctor = instructor;
		}
		public void createInfo(HttpSession session) throws Exception {
			if (iFirstInfo==null)
				iFirstInfo = iFirst.getDetails(session,false);
			if (iSecondInfo==null)
				iSecondInfo = iSecond.getDetails(session,false);
		}
		public boolean hasInfo() {
			return (iFirstInfo!=null && iSecondInfo!=null);
		}
		public int getPreference() { return iPref; }
		public String getInstructor() { return iInsturctor; }
		public String toHtml(HttpSession session, boolean link) {
			try {
				createInfo(session);
		        StringBuffer sb = new StringBuffer("<table border='0'>");
		        sb.append("<tr><td nowrap align='center'>");
		        sb.append("<font color='"+PreferenceLevel.int2color(getPreference())+"'>");
		        sb.append(PreferenceLevel.getPreferenceLevel(PreferenceLevel.int2prolog(getPreference())).getPrefName());
		        sb.append("</font><br>");
		        sb.append(iInsturctor);
		        sb.append("</font>");
		        sb.append("</td><td nowrap>");
		        sb.append(iFirstInfo.getClazz().toHtml(link)+" ");
		        if (iFirstInfo.getAssignedTime()!=null) {
		        	sb.append(iFirstInfo.getAssignedTime().toHtml(false,false,true)+" ");
		        	for (int i=0;i<iFirstInfo.getAssignedRoom().length;i++) {
		        		if (i>0) sb.append(", ");
		        		sb.append(iFirstInfo.getAssignedRoom()[i].toHtml(false,false));
		        	}
		        } else {
		        	sb.append(iFirstInfo.getTime().toHtml(false,false,true)+" ");
		        	for (int i=0;i<iFirstInfo.getRoom().length;i++) {
		        		if (i>0) sb.append(", ");
		        		sb.append(iFirstInfo.getRoom()[i].toHtml(false,false));
		        	}
		        }
		        sb.append("<br>");
		        sb.append(iSecondInfo.getClazz().toHtml(link)+" ");
		        if (iSecondInfo.getAssignedTime()!=null) {
		        	sb.append(iSecondInfo.getAssignedTime().toHtml(false,false,true)+" ");
		        	for (int i=0;i<iSecondInfo.getAssignedRoom().length;i++) {
		        		if (i>0) sb.append(", ");
		        		sb.append(iSecondInfo.getAssignedRoom()[i].toHtml(false,false));
		        	}
		        } else {
		        	sb.append(iSecondInfo.getTime().toHtml(false,false,true)+" ");
		        	for (int i=0;i<iSecondInfo.getRoom().length;i++) {
		        		if (i>0) sb.append(", ");
		        		sb.append(iSecondInfo.getRoom()[i].toHtml(false,false));
		        	}
		        }
		        sb.append("</td></tr></table>");
		        return sb.toString();
			} catch (Exception e) {
				Debug.error(e);
				return "<font color='red'>ERROR:"+e.getMessage()+"</font>";
			}
		}    	
		public void toXml(Element element) throws Exception {
			if (iFirst!=null)
				iFirst.toXml(element.addElement("first"));
			if (iSecond!=null)
				iSecond.toXml(element.addElement("second"));
			element.addAttribute("pref", String.valueOf(iPref));
			if (iInsturctor!=null)
				element.addAttribute("instructor", iInsturctor);
		}
		public static BtbInstructorInfo fromXml(Element element) {
			BtbInstructorInfo s = new BtbInstructorInfo();
			if (element.element("first")!=null)
				s.iFirst = Hint.fromXml(element.element("first"));
			if (element.element("second")!=null)
				s.iSecond = Hint.fromXml(element.element("second"));
			s.iPref = Integer.parseInt(element.attributeValue("pref"));
			s.iInsturctor = element.attributeValue("instructor");
			return s;
		}
    }
    
	public class BtbInstructorInfoComparator implements Comparator {
		HttpSession iSession = null;
		public BtbInstructorInfoComparator(HttpSession session) {
			iSession = session;
		}
		public int compare(Object o1, Object o2) {
			try {
				BtbInstructorInfo i1 = (BtbInstructorInfo)o1;
				BtbInstructorInfo i2 = (BtbInstructorInfo)o2;
				int cmp = i1.getInstructor().compareTo(i2.getInstructor());
				if (cmp!=0) return cmp;
				if (!i1.hasInfo()) i1.createInfo(iSession);
				if (!i2.hasInfo()) i2.createInfo(iSession);
				cmp = i1.iFirstInfo.compareTo(i2.iFirstInfo);
				if (cmp!=0) return cmp;
				return i1.iSecondInfo.compareTo(i2.iSecondInfo);
			} catch (Exception e) {
				return 0;
			}
		}
	}    
    
    public static class StudentConflictInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		private JenrlInfo iInfo;
		private Hint iFirst, iSecond;
		private transient ClassAssignmentDetails iFirstInfo = null, iSecondInfo = null;
		public StudentConflictInfo() {}
		public StudentConflictInfo(Hint first, Hint second, JenrlInfo info) {
			iInfo = info; iFirst = first; iSecond = second;
		}
		public JenrlInfo getInfo() { return iInfo; }
		public void createInfo(HttpSession session) throws Exception {
			if (iFirstInfo==null)
				iFirstInfo = iFirst.getDetails(session,false);
			if (iSecondInfo==null && iSecond!=null)
				iSecondInfo = iSecond.getDetails(session,false);
		}
		public boolean hasInfo() {
			return (iFirstInfo!=null);
		}
		
		public String toHtml(HttpSession session, boolean link) {
			try {
				createInfo(session);
				Vector props = new Vector();
				if (iInfo.isCommited()) props.add("committed");
		        if (iInfo.isFixed()) props.add("fixed");
		        else if (iInfo.isHard()) props.add("hard");
		        if (iInfo.isDistance()) props.add("distance");
		        StringBuffer sb = new StringBuffer("<table border='0'>");
		        sb.append("<tr>");
		        sb.append("<td "+(iSecondInfo==null?"":"rowspan='2'")+" nowrap>");
		        sb.append(ClassAssignmentDetails.sJenrDF.format(iInfo.getJenrl()));
		        sb.append("&times; ");
		        sb.append("</td><td nowrap>");
		        sb.append(iFirstInfo.getClazz().toHtml(link && !iInfo.isCommited())+" ");
		        if (iFirstInfo.getAssignedTime()!=null) {
		        	sb.append(iFirstInfo.getAssignedTime().toHtml(false,false,true)+" ");
		        	for (int i=0;i<iFirstInfo.getAssignedRoom().length;i++) {
		        		if (i>0) sb.append(", ");
		        		sb.append(iFirstInfo.getAssignedRoom()[i].toHtml(false,false));
		        	}
		        } else {
		        	sb.append(iFirstInfo.getTime().toHtml(false,false,true)+" ");
		        	for (int i=0;i<iFirstInfo.getRoom().length;i++) {
		        		if (i>0) sb.append(", ");
		        		sb.append(iFirstInfo.getRoom()[i].toHtml(false,false));
		        	}
		        }
		        sb.append("</td><td "+(iSecondInfo==null?"":"rowspan='2'")+" nowrap>");
		        sb.append(props.isEmpty()?"":" <i>"+props+"</i>");
		        sb.append(" <i>" + iInfo.getCurriculumText()+"</i>");
		        if (iSecondInfo!=null) {
		        	sb.append("</td></tr><tr><td nowrap>");
		        	sb.append(iSecondInfo.getClazz().toHtml(link && !iInfo.isCommited())+" ");
		        	if (iSecondInfo.getAssignedTime()!=null) {
		        		sb.append(iSecondInfo.getAssignedTime().toHtml(false,false,true)+" ");
		        		for (int i=0;i<iSecondInfo.getAssignedRoom().length;i++) {
		        			if (i>0) sb.append(", ");
		        			sb.append(iSecondInfo.getAssignedRoom()[i].toHtml(false,false));
		        		}
		        	} else {
		        		sb.append(iSecondInfo.getTime().toHtml(false,false,true)+" ");
		        		for (int i=0;i<iSecondInfo.getRoom().length;i++) {
		        			if (i>0) sb.append(", ");
		        			sb.append(iSecondInfo.getRoom()[i].toHtml(false,false));
		        		}
		        	}
		        }
		        sb.append("</td></tr></table>");
		        return sb.toString();
			} catch (Exception e) {
				Debug.error(e);
				return "<font color='red'>ERROR:"+e.getMessage()+"</font>";
			}
		}
		
		public void toXml(Element element) throws Exception {
			if (iInfo!=null)
				iInfo.save(element.addElement("jenrl"));
			if (iFirst!=null)
				iFirst.toXml(element.addElement("first"));
			if (iSecond!=null)
				iSecond.toXml(element.addElement("second"));
		}
		
		public static StudentConflictInfo fromXml(Element element) throws Exception {
			StudentConflictInfo s = new StudentConflictInfo();
			if (element.element("first")!=null)
				s.iFirst = Hint.fromXml(element.element("first"));
			if (element.element("second")!=null)
				s.iSecond = Hint.fromXml(element.element("second"));
			if (element.element("jenrl")!=null) {
				s.iInfo = new JenrlInfo();
				s.iInfo.load(element.element("jenrl"));
			}
			return s;
		}
	}
    
	public class StudentConflictInfoComparator implements Comparator {
		HttpSession iSession = null;
		public StudentConflictInfoComparator(HttpSession session) {
			iSession = session;
		}
		public int compare(Object o1, Object o2) {
			try {
				StudentConflictInfo i1 = (StudentConflictInfo)o1;
				StudentConflictInfo i2 = (StudentConflictInfo)o2;
				int cmp = Double.compare(i1.getInfo().getJenrl(),i2.getInfo().getJenrl());
				if (cmp!=0) return -cmp;
				if (!i1.hasInfo()) i1.createInfo(iSession);
				if (!i2.hasInfo()) i2.createInfo(iSession);
				cmp = i1.iFirstInfo.getClassName().compareTo(i2.iFirstInfo.getClassName());
				if (cmp!=0 || i1.iSecondInfo==null) return cmp;
				return i1.iSecondInfo.getClassName().compareTo(i2.iSecondInfo.getClassName());
			} catch (Exception e) {
				return 0;
			}
		}
	}
	
	public static class DistributionInfo implements Serializable {
		private static final long serialVersionUID = 1L;
		private GroupConstraintInfo iInfo;
		private Vector iClassIds = new Vector();
		public DistributionInfo() {
		}
		public DistributionInfo(GroupConstraintInfo info) {
			iInfo = info;
		}
		public void addHint(Hint hint) {
			iClassIds.add(hint);
		}
		public Vector getClassIds() { return iClassIds; }
		public GroupConstraintInfo getInfo() { return iInfo; }
		public String toHtml(HttpSession session, boolean link) {
	        StringBuffer sb = new StringBuffer("<table border='0'>");
	        sb.append("<tr><td nowrap align='center'>");
	        sb.append("<font color='"+PreferenceLevel.prolog2color(iInfo.getPreference())+"'>");
	        sb.append(PreferenceLevel.getPreferenceLevel(iInfo.getPreference()).getPrefName());
	        sb.append("</font><br>");
	        sb.append(iInfo.getName());
	        sb.append("</font>");
	        sb.append("</td><td nowrap>");
			try {
				for (Enumeration e=iClassIds.elements();e.hasMoreElements();) {
					Hint hint = (Hint)e.nextElement();
					ClassAssignmentDetails other = hint.getDetails(session,false);
					if (other==null) continue;
					sb.append(other.getClazz().toHtml(link)+" ");
					if (other.getTime()!=null)
						sb.append(other.getTime().toHtml(false,false,true)+" ");
					if (other.getRoom()!=null)
						for (int i=0;i<other.getRoom().length;i++) {
							if (i>0) sb.append(", ");
							sb.append(other.getRoom()[i].toHtml(false,false));
						}
			        if (e.hasMoreElements()) sb.append("<BR>");
				}
			} catch (Exception e) {
				Debug.error(e);
				sb.append("<font color='red'>ERROR:"+e.getMessage()+"</font>");
			}
			sb.append("</td></tr></table>");
			return sb.toString();
		}
		public void toXml(Element element) throws Exception {
			if (iInfo!=null)
				iInfo.save(element.addElement("groupConstraintInfo"));
			if (iClassIds!=null) {
				for (Enumeration e=iClassIds.elements();e.hasMoreElements();) {
					Hint h = (Hint)e.nextElement();
					h.toXml(element.addElement("class"));
				}
			}
		}
		public static DistributionInfo fromXml(Element element) throws Exception {
			DistributionInfo s = new DistributionInfo();
			s.iClassIds = new Vector();
			for (Iterator i=element.elementIterator("class");i.hasNext();) {
				s.iClassIds.add(Hint.fromXml((Element)i.next()));
			}
			if (element.element("groupConstraintInfo")!=null) {
				s.iInfo = new GroupConstraintInfo();
				s.iInfo.load(element.element("groupConstraintInfo"));
			}
			return s;
		}
		
	}
	
	public void toXml(Element element) throws Exception {
		element.addAttribute("value", String.valueOf(iValue));
		element.addAttribute("tooBigRooms", String.valueOf(iTooBigRooms));
		element.addAttribute("uselessSlots", String.valueOf(iUselessSlots));
		element.addAttribute("timePreference", String.valueOf(iGlobalTimePreference));
		element.addAttribute("roomPreference", String.valueOf(iGlobalRoomPreference));
		element.addAttribute("groupConstraintPreference", String.valueOf(iGlobalGroupConstraintPreference));
		element.addAttribute("studentConflicts", String.valueOf(iViolatedStudentConflicts));
		element.addAttribute("hardStudentConflicts", String.valueOf(iHardStudentConflicts));
		element.addAttribute("distanceStudentConflicts", String.valueOf(iDistanceStudentConflicts));
		element.addAttribute("commitedStudentConflicts", String.valueOf(iCommitedStudentConflicts));
		element.addAttribute("instructorDistancePreference", String.valueOf(iInstructorDistancePreference));
		element.addAttribute("departmentSpreadPenalty", String.valueOf(iDepartmentSpreadPenalty));
		element.addAttribute("unassignedVariables", String.valueOf(iUnassignedVariables));
		element.addAttribute("perturbationPenalty", String.valueOf(iPerturbationPenalty));
		element.addAttribute("spreadPenalty", String.valueOf(iSpreadPenalty));
		if (iHint!=null)
			iHint.toXml(element.addElement("hint"));
		if (iDifferentAssignments!=null) {
			for (Enumeration e=iDifferentAssignments.elements();e.hasMoreElements();) {
				Hint h = (Hint)e.nextElement();
				h.toXml(element.addElement("differentAssignment"));
			}
		}
		if (iUnresolvedConflicts!=null) {
			for (Iterator i=iUnresolvedConflicts.iterator();i.hasNext();) {
				Hint h = (Hint)i.next();
				h.toXml(element.addElement("unresolvedConflict"));
			}
		}
		if (iStudentConflictInfos!=null) {
			for (Enumeration e=iStudentConflictInfos.elements();e.hasMoreElements();) {
				StudentConflictInfo sci = (StudentConflictInfo)e.nextElement();
				sci.toXml(element.addElement("studentConflictInfo"));
			}
		}
		if (iGroupConstraintInfos!=null) {
			for (Enumeration e=iGroupConstraintInfos.elements();e.hasMoreElements();) {
				DistributionInfo di = (DistributionInfo)e.nextElement();
				di.toXml(element.addElement("groupConstraintInfo"));
			}
		}
		if (iBtbInstructorInfos!=null) {
			for (Enumeration e=iBtbInstructorInfos.elements();e.hasMoreElements();) {
				BtbInstructorInfo bii = (BtbInstructorInfo)e.nextElement();
				bii.toXml(element.addElement("btbInstructorInfo"));
			}
		}
	}
	
	public static Suggestion fromXml(Element element) throws Exception {
		Suggestion s = new Suggestion();
		s.iValue = Double.parseDouble(element.attributeValue("value"));
		s.iTooBigRooms = Integer.parseInt(element.attributeValue("tooBigRooms"));
		s.iUselessSlots = Long.parseLong(element.attributeValue("uselessSlots"));
		s.iGlobalTimePreference = Double.parseDouble(element.attributeValue("timePreference"));
		s.iGlobalRoomPreference = Long.parseLong(element.attributeValue("roomPreference"));
		s.iGlobalGroupConstraintPreference = Long.parseLong(element.attributeValue("groupConstraintPreference"));
		s.iViolatedStudentConflicts = Long.parseLong(element.attributeValue("studentConflicts"));
		s.iHardStudentConflicts = Long.parseLong(element.attributeValue("hardStudentConflicts"));
		s.iDistanceStudentConflicts = Long.parseLong(element.attributeValue("distanceStudentConflicts"));;
		s.iCommitedStudentConflicts = Long.parseLong(element.attributeValue("commitedStudentConflicts"));
		s.iInstructorDistancePreference = Long.parseLong(element.attributeValue("instructorDistancePreference"));
		s.iDepartmentSpreadPenalty = Integer.parseInt(element.attributeValue("departmentSpreadPenalty"));
		s.iUnassignedVariables = Integer.parseInt(element.attributeValue("unassignedVariables"));
		s.iPerturbationPenalty = Double.parseDouble(element.attributeValue("perturbationPenalty"));
		s.iSpreadPenalty = Integer.parseInt(element.attributeValue("spreadPenalty"));
		if (element.element("hint")!=null)
			s.iHint = Hint.fromXml(element.element("hint"));
		for (Iterator i=element.elementIterator("differentAssignment"); i.hasNext();) {
			if (s.iDifferentAssignments==null) s.iDifferentAssignments = new Vector();
			s.iDifferentAssignments.add(Hint.fromXml((Element)i.next()));
		}
		for (Iterator i=element.elementIterator("unresolvedConflict"); i.hasNext();) {
			if (s.iUnresolvedConflicts==null) s.iUnresolvedConflicts = new HashSet();
			s.iUnresolvedConflicts.add(Hint.fromXml((Element)i.next()));
		}
		for (Iterator i=element.elementIterator("studentConflictInfo"); i.hasNext();) {
			if (s.iStudentConflictInfos==null) s.iStudentConflictInfos = new Vector();
			s.iStudentConflictInfos.add(StudentConflictInfo.fromXml((Element)i.next()));
		}
		for (Iterator i=element.elementIterator("groupConstraintInfo"); i.hasNext();) {
			if (s.iGroupConstraintInfos==null) s.iGroupConstraintInfos = new Vector();
			s.iGroupConstraintInfos.add(DistributionInfo.fromXml((Element)i.next()));
		}
		for (Iterator i=element.elementIterator("btbInstructorInfo"); i.hasNext();) {
			if (s.iBtbInstructorInfos==null) s.iBtbInstructorInfos = new Vector();
			s.iBtbInstructorInfos.add(BtbInstructorInfo.fromXml((Element)i.next()));
		}
		return s;
	}
    
    public String toString() {
        return "Suggestion{value = "+sDF.format(iValue)+"\n"+
                (iDifferentAssignments==null || iDifferentAssignments.isEmpty()?"":"  differentAssignments = "+iDifferentAssignments+"\n")+
                (iUnresolvedConflicts==null || iUnresolvedConflicts.isEmpty()?"":"  unresolvedConflicts = "+iUnresolvedConflicts+"\n")+
                (iHint==null?"":"  hint = "+iHint+"\n")+
                "}";
    }
}
