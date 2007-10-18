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
package org.unitime.timetable.form;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.model.TimePatternDays;
import org.unitime.timetable.model.TimePatternTime;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.TimePatternDAO;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.webutil.RequiredTimeTable;


/** 
 * @author Tomas Muller
 */
public class TimePatternEditForm extends ActionForm {
    private String iOp;
    private Long iUniqueId;
    private String iType;
    private String iNrMtgs;
    private String iMinPerMtg;
    private String iSlotsPerMtg;
    private String iDayCodes;
    private String iStartTimes;
    private boolean iEditable;
    private boolean iVisible;
    private String iName;
    private Vector iDepartmentIds = new Vector();
    private Long iDepartmentId;
    private String iBreakTime;

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();
        
		if(iName==null || iName.trim().length()==0)
			errors.add("name", new ActionMessage("errors.required", ""));
		else {
			try {
				TimePattern pat = TimePattern.findByName(request,iName);
				if (pat!=null && !pat.getUniqueId().equals(iUniqueId))
					errors.add("name", new ActionMessage("errors.exists", iName));
			} catch (Exception e) {
				errors.add("name", new ActionMessage("errors.generic", e.getMessage()));
			}
        }
        
		try {
			if (Integer.parseInt(getNrMtgs())<=0 && getTypeInt()!=TimePattern.sTypeExactTime)
				errors.add("nrMtgs", new ActionMessage("errors.required", ""));
		} catch (NumberFormatException e) {
			errors.add("nrMtgs", new ActionMessage("errors.required", ""));
		}
		try {
			if (Integer.parseInt(getMinPerMtg())<=0 && getTypeInt()!=TimePattern.sTypeExactTime)
				errors.add("minPerMtg", new ActionMessage("errors.required", ""));
		} catch (NumberFormatException e) {
			errors.add("minPerMtg", new ActionMessage("errors.required", ""));
		}
		try {
			if (Integer.parseInt(getSlotsPerMtg())<=0 && getTypeInt()!=TimePattern.sTypeExactTime)
				errors.add("slotsPerMtg", new ActionMessage("errors.required", ""));
		} catch (NumberFormatException e) {
			errors.add("slotsPerMtg", new ActionMessage("errors.required", ""));
		}
		if (getTypeInt()<0)
			errors.add("type", new ActionMessage("errors.required", ""));

		try {
			str2dayCodes(iDayCodes,"\n, ");
		} catch (Exception e) {
			errors.add("dayCodes", new ActionMessage("errors.generic", e.getMessage()));
		}

		try {
			str2startSlots(iStartTimes,"\n, ");
		} catch (Exception e) {
			errors.add("startTimes", new ActionMessage("errors.generic", e.getMessage()));
		}
        
		if (getTypeInt()!=TimePattern.sTypeExtended && getTypeInt()!=TimePattern.sTypeExactTime && !iDepartmentIds.isEmpty())
			errors.add("type", new ActionMessage("errors.generic", "Only extended or exact time pattern can contain relations with departments."));

		return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iOp = null; iUniqueId = new Long(-1); iType = TimePattern.sTypes[0]; iNrMtgs = ""; iMinPerMtg = ""; iSlotsPerMtg = "";
		iDayCodes = ""; iStartTimes = ""; iEditable = false; iVisible = false; iName = ""; iBreakTime = "";
		iDepartmentId = null; iDepartmentIds.clear();
	}
	
	public void load(TimePattern tp, Long sessionId) {
		if (tp==null) {
			reset(null, null);
			iOp = "Save";
			iVisible = true; iEditable = true;
		} else {
			setName(tp.getName());
			setVisible(tp.isVisible().booleanValue());
			setEditable(tp.isEditable());
			setTypeInt(tp.getType().intValue());
			setMinPerMtg(tp.getMinPerMtg().toString());
			setNrMtgs(tp.getNrMeetings().toString());
			setBreakTime(tp.getBreakTime().toString());
			setSlotsPerMtg(tp.getSlotsPerMtg().toString());
			setUniqueId(tp.getUniqueId());
			setDayCodes(dayCodes2str(tp.getDays(),(getEditable()?"\n":", ")));
			setStartTimes(startSlots2str(tp.getTimes(),(getEditable()?"\n":", ")));
			iDepartmentIds.clear();
			for (Iterator i=tp.getDepartments(sessionId).iterator();i.hasNext();) {
				Department d = (Department)i.next();
				iDepartmentIds.add(d.getUniqueId());
			}
			iOp = "Update";
		}
	}
	
	public void update(TimePattern tp, org.hibernate.Session hibSession, Long sessionId) throws Exception {
		tp.setName(getName());
		tp.setVisible(new Boolean(getVisible()));
		tp.setType(new Integer(getTypeInt()));
		tp.setBreakTime(new Integer(getBreakTime()));
		if (getEditable()) {
			tp.setMinPerMtg(new Integer(getMinPerMtg()));
			tp.setNrMeetings(new Integer(getNrMtgs()));
			tp.setSlotsPerMtg(new Integer(getSlotsPerMtg()));
			for (Iterator i=tp.getTimes().iterator();i.hasNext();) {
				TimePatternTime t = (TimePatternTime)i.next();
				hibSession.delete(t);
			}
			for (Iterator i=tp.getDays().iterator();i.hasNext();) {
				TimePatternDays d = (TimePatternDays)i.next();
				hibSession.delete(d);
			}
			tp.setTimes(str2startSlots(getStartTimes(),"\n, "));
			tp.setDays(str2dayCodes(getDayCodes(),"\n, "));
			for (Iterator i=tp.getTimes().iterator();i.hasNext();) {
				TimePatternTime t = (TimePatternTime)i.next();
				hibSession.save(t);
			}
			for (Iterator i=tp.getDays().iterator();i.hasNext();) {
				TimePatternDays d = (TimePatternDays)i.next();
				hibSession.save(d);
			}
		}
		HashSet oldDepts = new HashSet(tp.getDepartments());
		for (Enumeration e=iDepartmentIds.elements();e.hasMoreElements();) {
			Long departmentId = (Long)e.nextElement();
			Department d = (new DepartmentDAO()).get(departmentId,hibSession);
			if (d==null) continue;
			if (oldDepts.remove(d)) {
				//not changed -> do nothing
			} else {
				tp.getDepartments().add(d);
				d.getTimePatterns().add(tp);
				hibSession.saveOrUpdate(d);
			}
		}
		for (Iterator i=oldDepts.iterator();i.hasNext();) {
			Department d = (Department)i.next();
			if (!d.getSessionId().equals(sessionId)) continue;
			tp.getDepartments().remove(d);
			d.getTimePatterns().remove(tp);
			hibSession.saveOrUpdate(d);
		}
		hibSession.saveOrUpdate(tp);
	}
	
	public TimePattern create(HttpServletRequest request, org.hibernate.Session hibSession) throws Exception {
		TimePattern tp = new TimePattern();
		tp.setName(getName());
		tp.setVisible(new Boolean(getVisible()));
		tp.setType(new Integer(getTypeInt()));
		tp.setMinPerMtg(new Integer(getMinPerMtg()));
		tp.setNrMeetings(new Integer(getNrMtgs()));
		tp.setBreakTime(new Integer(getBreakTime()));
		tp.setSlotsPerMtg(new Integer(getSlotsPerMtg()));
		tp.setTimes(str2startSlots(getStartTimes(),"\n, "));
		tp.setDays(str2dayCodes(getDayCodes(),"\n, "));
		if (request!=null) {
	    	User user = Web.getUser(request.getSession());
	    	tp.setSession(Session.getCurrentAcadSession(user));
		}
		for (Iterator i=tp.getTimes().iterator();i.hasNext();) {
			TimePatternTime t = (TimePatternTime)i.next();
			hibSession.save(t);
		}
		for (Iterator i=tp.getDays().iterator();i.hasNext();) {
			TimePatternDays d = (TimePatternDays)i.next();
			hibSession.save(d);
		}
		HashSet newDepts = new HashSet();
		for (Enumeration e=iDepartmentIds.elements();e.hasMoreElements();) {
			Long departmentId = (Long)e.nextElement();
			Department d = (new DepartmentDAO()).get(departmentId,hibSession);
			if (d==null) continue;
			newDepts.add(d);
		}
		tp.setDepartments(newDepts);
		hibSession.save(tp);
		for (Iterator i=newDepts.iterator();i.hasNext();) {
			Department d = (Department)i.next();
			d.getTimePatterns().add(tp);
			hibSession.saveOrUpdate(d);
		}
		setUniqueId(tp.getUniqueId());
		return tp;
	}
	
	public TimePattern saveOrUpdate(HttpServletRequest request, org.hibernate.Session hibSession, Long sessionId) throws Exception {
		TimePattern tp = null;
		if (getUniqueId().intValue()>=0)
			tp = (new TimePatternDAO()).get(getUniqueId());
		if (tp==null) {
			tp = create(request, hibSession);
            ChangeLog.addChange(
                    hibSession, 
                    request, 
                    tp, 
                    ChangeLog.Source.TIME_PATTERN_EDIT, 
                    ChangeLog.Operation.CREATE, 
                    null, 
                    null);
        } else { 
			update(tp, hibSession, sessionId);
            ChangeLog.addChange(
                    hibSession, 
                    request, 
                    tp, 
                    ChangeLog.Source.TIME_PATTERN_EDIT, 
                    ChangeLog.Operation.UPDATE, 
                    null, 
                    null);
        }
		return tp;
	}
	
	public void delete(org.hibernate.Session hibSession, HttpServletRequest request) throws Exception {
		if (getUniqueId().intValue()<0)
			return;
		if (!getEditable())
			return;
		TimePattern tp = (new TimePatternDAO()).get(getUniqueId(), hibSession);
		for (Iterator i=tp.getDepartments().iterator();i.hasNext();) {
			Department d = (Department)i.next();
			d.getTimePatterns().remove(tp);
			hibSession.saveOrUpdate(d);
		}
        ChangeLog.addChange(
                hibSession, 
                request, 
                tp, 
                ChangeLog.Source.TIME_PATTERN_EDIT, 
                ChangeLog.Operation.DELETE, 
                null, 
                null);
		hibSession.delete(tp);
	}
	
	public String getOp() { return iOp; }
	public void setOp(String op) { iOp = op; }
	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }
	public String getType() { return iType; }
	public void setType(String type) { iType = type; }
	public String[] getTypes() { return TimePattern.sTypes; }
	public int getTypeInt() {
		for (int i=0;i<TimePattern.sTypes.length;i++)
			if (TimePattern.sTypes[i].equals(iType)) return i;
		return -1;
	}
	public void setTypeInt(int type) { iType = (type<0?"":TimePattern.sTypes[type]); }
	public String getNrMtgs() { return iNrMtgs; }
	public void setNrMtgs(String nrMtgs) { iNrMtgs = nrMtgs; }
	public String getBreakTime() { return iBreakTime; }
	public void setBreakTime(String breakTime) { iBreakTime = breakTime; }
	public String getMinPerMtg() { return iMinPerMtg; }
	public void setMinPerMtg(String minPerMtg) { iMinPerMtg = minPerMtg; }
	public String getSlotsPerMtg() { return iSlotsPerMtg; }
	public void setSlotsPerMtg(String slotsPerMtg) { iSlotsPerMtg = slotsPerMtg; }
	public String getDayCodes() { return iDayCodes; }
	public void setDayCodes(String dayCodes) { iDayCodes = dayCodes; }
	public String getStartTimes() { return iStartTimes; }
	public void setStartTimes(String startTimes) { iStartTimes = startTimes; }
	public boolean getEditable() { return iEditable; }
	public void setEditable(boolean editable) { iEditable = editable; }
	public boolean getVisible() { return iVisible; }
	public void setVisible(boolean visible) { iVisible = visible; }
	public String getName() { return iName; }
	public void setName(String name) { iName = name; }
	public Vector getDepartmentIds() { return iDepartmentIds; }
	public void setDepartmentIds(Vector departmentIds) { iDepartmentIds = departmentIds; }
	public Long getDepartmentId() { return iDepartmentId; }
	public void setDepartmentId(Long deptId) { iDepartmentId = deptId; }

	public static String dayCodes2str(Collection dayCodes, String delim) {
		StringBuffer sb = new StringBuffer();
		for (Iterator i=(new TreeSet(dayCodes)).iterator();i.hasNext();) {
			int dayCode = ((TimePatternDays)i.next()).getDayCode().intValue();
			int nrDays = 0;
			for (int j=0;j<Constants.NR_DAYS;j++)
				if ((dayCode&Constants.DAY_CODES[j])!=0) nrDays++;
			for (int j=0;j<Constants.NR_DAYS;j++) {
				if ((Constants.DAY_CODES[j]&dayCode)==0) continue;
				sb.append(nrDays==1?Constants.DAY_NAME[j]:Constants.DAY_NAMES_SHORT[j]);
			}
			if (i.hasNext())
				sb.append(delim);
		}
		return sb.toString();
	}
	public static String startSlots2str(Collection startSlots, String delim) {
		StringBuffer sb = new StringBuffer();
		for (Iterator i=(new TreeSet(startSlots)).iterator();i.hasNext();) {
			int startSlot = ((TimePatternTime)i.next()).getStartSlot().intValue();
			int min = startSlot*Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
			int time = 100*(min/60) + (min%60);
			sb.append(time);
			if (i.hasNext())
				sb.append(delim);
		}
		return sb.toString();
	}
	
	public static int getDayCode(String token, int day, int dayCode) {
		if (day==Constants.NR_DAYS) {
			if (token.length()==0) return dayCode;
			else return -1;
		}
		if (token.startsWith(Constants.DAY_NAMES_SHORT[day])) {
			int code = getDayCode(token.substring(Constants.DAY_NAMES_SHORT[day].length()),day+1,dayCode + Constants.DAY_CODES[day]);
			if (code>=0) return code;
		}
		if (token.startsWith(Constants.DAY_NAME[day])) {
			int code = getDayCode(token.substring(Constants.DAY_NAME[day].length()),day+1,dayCode + Constants.DAY_CODES[day]);
			if (code>=0) return code;
		}
		return getDayCode(token, day+1, dayCode);
	}
	
	public Set str2dayCodes(String dayCodes, String delim) throws Exception {
		Set ret = new TreeSet();
		StringTokenizer stk = new StringTokenizer(dayCodes, (delim==null?" ,\t\n\r\f":delim));
		while (stk.hasMoreTokens()) {
			String token = stk.nextToken();
			if (token.trim().length()==0) continue;
			int dayCode = getDayCode(token.trim(), 0, 0);
			if (dayCode<0)
				throw new Exception("Invalid days '"+token+"'.");
			int nrDays = 0;
			for (int i=0;i<Constants.NR_DAYS;i++)
				if ((dayCode&Constants.DAY_CODES[i])!=0) nrDays++;
			try {
				if (nrDays!=Integer.parseInt(getNrMtgs()))
					throw new Exception("Days '"+token+"' invalid -- wrong number of days.");
			} catch (NumberFormatException e) {}
			TimePatternDays days = new TimePatternDays();
			days.setDayCode(new Integer(dayCode));
			if (ret.contains(days))
				throw new Exception("Days '"+token+"' included more than once.");
			ret.add(days);
		}
		return ret;
	}

	public Set str2startSlots(String startTimes, String delim) throws Exception {
		Set ret = new TreeSet();
		StringTokenizer stk = new StringTokenizer(startTimes, (delim==null?" ,\t\n\r\f":delim));
		while (stk.hasMoreTokens()) {
			String token = stk.nextToken();
			if (token.trim().length()==0) continue;
			int slot = -1;
			try {
				int time = Integer.parseInt(token.trim());
				int hour = time/100;
				int min = time%100;
				if (hour>=24)
					throw new Exception("Invalid time '"+token+"' -- hour ("+hour+") must be between 0 and 23.");
				if (min>=60)
					throw new Exception("Invalid time '"+token+"' -- minute ("+min+") must be between 0 and 59.");
				if ((Constants.SLOT_LENGTH_MIN%5)!=0)
					throw new Exception("Invalid time '"+token+"' -- minute ("+min+") must be divisible by "+Constants.SLOT_LENGTH_MIN+".");
				try {
					int endTime = hour*60+min+Integer.parseInt(getSlotsPerMtg());
					if (endTime/Constants.SLOT_LENGTH_MIN-Constants.FIRST_SLOT_TIME_MIN >= Constants.SLOTS_PER_DAY)
						throw new Exception("Invalid time '"+token+"' -- end time ("+(100*(endTime/60)+endTime%60)+") exceeds maximum available time.");
				} catch (NumberFormatException e) {}
				slot = (hour*60+min - Constants.FIRST_SLOT_TIME_MIN)/Constants.SLOT_LENGTH_MIN;
			} catch (NumberFormatException ex) {
				throw new Exception("Invalid time '"+token+"' -- not a number.");
			}
			if (slot<0)
				throw new Exception("Invalid time '"+token+"'.");
			TimePatternTime time = new TimePatternTime();
			time.setStartSlot(new Integer(slot));
			if (ret.contains(time))
				throw new Exception("Time '"+token+"' included more than once.");
			ret.add(time);
		}
		return ret;
	}
	
	public String getExample() {
		try {
			TimePattern tp = new TimePattern(new Long(Long.MAX_VALUE));
			tp.setName(getName());
			tp.setVisible(new Boolean(getVisible()));
			tp.setType(new Integer(getTypeInt()));
			tp.setMinPerMtg(new Integer(getMinPerMtg()));
			tp.setNrMeetings(new Integer(getNrMtgs()));
			tp.setBreakTime(new Integer(getBreakTime()));
			tp.setSlotsPerMtg(new Integer(getSlotsPerMtg()));
			tp.setTimes(str2startSlots(getStartTimes(),"\n, "));
			tp.setDays(str2dayCodes(getDayCodes(),"\n, "));
			if (tp.getTimes().isEmpty() || tp.getDays().isEmpty()) return null;
			RequiredTimeTable rtt = tp.getRequiredTimeTable(true);
			return rtt.print(false,false,false,false);
		} catch (Exception e) {
			return null;
		}
	}
}

