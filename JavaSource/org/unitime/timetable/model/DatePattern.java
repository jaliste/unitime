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
package org.unitime.timetable.model;

import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.model.base.BaseDatePattern;
import org.unitime.timetable.model.dao.DatePatternDAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.DateUtils;


public class DatePattern extends BaseDatePattern implements Comparable {
	private static final long serialVersionUID = 1L;

    public static final int sTypeStandard = 0;
    public static final int sTypeAlternate = 1;
    public static final int sTypeNonStandard = 2;
    public static final int sTypeExtended = 3;
    public static final String[] sTypes = new String[] {"Standard", "Alternate Weeks", "Non-standard", "Extended" };
    
    public static String DATE_PATTERN_LIST_ATTR = "datePatternList";

/*[CONSTRUCTOR MARKER BEGIN]*/
	public DatePattern () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public DatePattern (java.lang.Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/
	public int getPatternOffset() {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(getSession().getSessionBeginDateTime());
		int beginDate = cal.get(Calendar.DAY_OF_YEAR);
		return beginDate-(getOffset()==null?0:getOffset().intValue())-1;
	}

	private transient BitSet iCachedPatternBitSet = null;
	
	public void setPattern(String pattern) {
		super.setPattern(pattern); iCachedPatternBitSet = null;
	}
	
	public void setOffset(Integer offset) {
		super.setOffset(offset); iCachedPatternBitSet = null;
	}
	
	public void setPatternOffset(Date firstDate) {
	    Calendar cal = Calendar.getInstance(Locale.US);
	    cal.setTime(getSession().getSessionBeginDateTime());
	    int offset = 0;
	    while (cal.getTime().compareTo(firstDate)<0) {
	        offset--; cal.add(Calendar.DAY_OF_YEAR, 1);
	    }
	    while (cal.getTime().compareTo(firstDate)>0) {
	        offset++; cal.add(Calendar.DAY_OF_YEAR, -1);
	    }
	    setOffset(offset);
	}

	public BitSet getPatternBitSet() {
		if (iCachedPatternBitSet!=null) return iCachedPatternBitSet;
		if (getPattern()==null || getOffset()==null) return null;
		int startMonth = getSession().getPatternStartMonth();
		int endMonth = getSession().getPatternEndMonth();
		int size = getSession().getDayOfYear(0,endMonth+1)-getSession().getDayOfYear(1,startMonth);
		iCachedPatternBitSet = new BitSet(size);
		int offset = getPatternOffset() - getSession().getDayOfYear(1,startMonth);
		for (int i=0;i<getPattern().length();i++) {
			if (getPattern().charAt(i)=='1')
				iCachedPatternBitSet.set(i+offset);
		}
		return iCachedPatternBitSet;
	}
	
	public boolean isOffered(int day, int month) {
		if (getPattern()==null || getOffset()==null) return false;
		int idx = getSession().getDayOfYear(day, month)-getPatternOffset();
		if (idx<0 || idx>=getPattern().length()) return false;
		return (getPattern().charAt(idx)=='1');
	}
	
	public boolean isUsed(int day, int month, Set usage) {
		if (usage==null || getPattern()==null || getOffset()==null) return false;
		return usage.contains(new Integer(getSession().getDayOfYear(day, month)));
	}

	public String getPatternArray() {
		StringBuffer sb = new StringBuffer("[");
		int startMonth = getSession().getPatternStartMonth();
		int endMonth = getSession().getPatternEndMonth();
		int year = getSession().getSessionStartYear();
		for (int m=startMonth;m<=endMonth;m++) {
			if (m!=startMonth) sb.append(",");
			sb.append("[");
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, year);
			for (int d=1;d<=daysOfMonth;d++) {
				if (d>1) sb.append(",");
				sb.append(isOffered(d,m)?"'1'":"'0'");
			}
			sb.append("]");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public String getPatternString() {
		
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		HashMap dates = getPatternDateStringHashMaps();
		TreeSet ts = new TreeSet();
		ts.addAll(dates.keySet());
		for(Iterator it = ts.iterator(); it.hasNext();){
			Date startDate = (Date) it.next();
			Date endDate = (Date) dates.get(startDate);
			SimpleDateFormat df = new SimpleDateFormat("M/d");
			String startDateStr = df.format(startDate);
			String endDateStr = df.format(endDate);
			if (first){
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(startDateStr);
			if (!startDateStr.equals(endDateStr)){
				sb.append("-" + endDateStr);
			}
		}
		return sb.toString();
	}
	
	public HashMap getPatternDateStringHashMaps() {
		Calendar startDate = Calendar.getInstance(Locale.US);
		startDate.setTime(getStartDate());
		Calendar endDate = Calendar.getInstance(Locale.US);
		endDate.setTime(getEndDate());

		int startMonth = startDate.get(Calendar.MONTH);
		int endMonth = endDate.get(Calendar.MONTH);
		int startYear = startDate.get(Calendar.YEAR);
		int endYear = endDate.get(Calendar.YEAR);
		if (endYear > startYear){
			endMonth += (12 * (endYear - startYear));
		}
		
		HashMap mapStartToEndDate = new HashMap();
		Date first = null, previous = null;
		char[] ptrn = getPattern().toCharArray();
		int charPosition = 0;
		int dayOfWeek = startDate.get(Calendar.DAY_OF_WEEK);
		Calendar cal = Calendar.getInstance(Locale.US);

		for (int m=startMonth;m<=endMonth;m++) {
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, startYear);
			int d;
			if (m == startMonth){
				d = startDate.get(Calendar.DAY_OF_MONTH);
			} else {
				d = 1;
			}
			for (;d<=daysOfMonth && charPosition < ptrn.length ;d++) {
				if (ptrn[charPosition] == '1' || (first != null && dayOfWeek == Calendar.SUNDAY && charPosition + 1 < ptrn.length && ptrn[1 + charPosition] == '1')) {
					if (first==null){
						//first = ((m<0?12+m:m%12)+1)+"/"+d+"/"+((m>=12)?startYear+1:startYear);
						cal.setTime(getStartDate());
						cal.add(Calendar.DAY_OF_YEAR, charPosition);
						first = cal.getTime();
					}
				} else {
					if (first!=null) {
						mapStartToEndDate.put(first, previous);
						first=null;
					}
				}
				//previous = ((m<0?12+m:m%12)+1)+"/"+d+"/"+((m>=12)?startYear+1:startYear);
				cal.setTime(getStartDate());
				cal.add(Calendar.DAY_OF_YEAR, charPosition);
				previous = cal.getTime();
				
				charPosition++;
				dayOfWeek++;
				if (dayOfWeek > Calendar.SATURDAY){
					dayOfWeek = Calendar.SUNDAY;
				}
			}
		}
		if (first!=null) {
			mapStartToEndDate.put(first, previous);
			first=null;
		}
		return(mapStartToEndDate);
	}

	public TreeSet getUsage(Collection classes) {
		TreeSet days = new TreeSet();
		
		int dowOffset = Constants.getDayOfWeek(DateUtils.getDate(1, 0, getSession().getSessionStartYear()));
		int offset = getPatternOffset();
		for (Iterator j=classes.iterator();j.hasNext();) {
			Class_ clazz = (Class_)j.next();
			
			for (Iterator k=clazz.effectivePreferences(TimePref.class).iterator();k.hasNext();) {
				TimePref tp = (TimePref)k.next();
				if (tp.getTimePattern().getType().intValue()==TimePattern.sTypeExactTime) {
					//System.out.println("    -- exact time "+tp.getTimePatternModel().getExactDays());
					int dayCode = tp.getTimePatternModel().getExactDays();
					
					for (int x=0;x<getPattern().length();x++) {
						if (getPattern().charAt(x)!='1') continue;
						int dayOfWeek = (x+offset+dowOffset) % 7;
						if ((dayCode&Constants.DAY_CODES[dayOfWeek < 0 ? dayOfWeek + 7 : dayOfWeek])!=0)
							days.add(new Integer(x+offset));
					}
				} else {
					//System.out.println("    -- time pattern "+tp.getTimePattern().getName());
					TimePatternModel m = tp.getTimePatternModel();
					boolean req = false;
					for (int d=0;d<m.getNrDays();d++) {
						boolean used = false;
						for (int t=0;t<m.getNrTimes();t++) {
							if (PreferenceLevel.sRequired.equals(m.getPreference(d,t))) {
								used=true; break;
							}
						}
						if (!used) continue;
						req = true;
						int dayCode = m.getDayCode(d);
						//System.out.println("      -- required "+dayCode);
    					for (int x=0;x<getPattern().length();x++) {
    						if (getPattern().charAt(x)!='1') continue;
    						int dayOfWeek = (x+offset+dowOffset) % 7;
    						if ((dayCode&Constants.DAY_CODES[dayOfWeek < 0 ? dayOfWeek + 7 : dayOfWeek])!=0)
    							days.add(new Integer(x+offset));
    					}
					}
					if (!req) {
    					for (int d=0;d<m.getNrDays();d++) {
    						boolean used = false;
    						for (int t=0;t<m.getNrTimes();t++) {
    							if (!PreferenceLevel.sProhibited.equals(m.getPreference(d,t))) {
    								used=true; break;
    							}
    						}
    						if (!used) continue;
    						req = true;
    						int dayCode = m.getDayCode(d);
    						//System.out.println("      -- not prohibited "+dayCode);
        					for (int x=0;x<getPattern().length();x++) {
        						if (getPattern().charAt(x)!='1') continue;
        						int dayOfWeek = (x+offset+dowOffset) % 7;
        						if ((dayCode&Constants.DAY_CODES[dayOfWeek < 0 ? dayOfWeek + 7 : dayOfWeek])!=0)
        							days.add(new Integer(x+offset));
        					}
    					}
					}
				}
			}
		}

		return days;
	}
	
	public TreeSet getUsage(Long uniqueId) {
		if (uniqueId==null) return null;
		HashSet classes = new HashSet(
				new DatePatternDAO().getSession().
				createQuery("select distinct c from Class_ as c inner join c.datePattern as dp where dp.uniqueId=:uniqueId").
				setInteger("uniqueId", uniqueId.intValue()).setCacheable(true).list());
		for (Iterator i=
			new DatePatternDAO().getSession().
			createQuery("select distinct s from SchedulingSubpart as s inner join s.datePattern as dp where dp.uniqueId=:uniqueId").
			setInteger("uniqueId", uniqueId.intValue()).setCacheable(true).list().iterator();
			i.hasNext();) {
			SchedulingSubpart s = (SchedulingSubpart)i.next();
			for (Iterator k=s.getClasses().iterator();k.hasNext();) {
				Class_ c = (Class_)k.next();
				if (c.getDatePattern()==null)
					classes.add(c);
			}
		}
		return getUsage(classes);
	}
	
	public String getBorderArray(Long uniqueId) {
		int startMonth = getSession().getPatternStartMonth();
		int endMonth = getSession().getPatternEndMonth();
		int dayOfYear = 0;
		int year = getSession().getSessionStartYear();
		Set usage = (uniqueId!=null?getUsage(uniqueId):null);
		StringBuffer sb = new StringBuffer("[");
		for (int m=startMonth;m<=endMonth;m++) {
			if (m!=startMonth) sb.append(",");
			sb.append("[");
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, year);
			for (int d=1;d<=daysOfMonth;d++) {
				dayOfYear++;
				if (d>1) sb.append(",");
				String border = getSession().getBorder(d,m);
				if (isUsed(d,m,usage)) 
					border = "'green 2px solid'";
				sb.append(border);
			}
			sb.append("]");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public String getPatternHtml() {
		return getPatternHtml(true, null);
	}
	
	public String getPatternHtml(boolean editable) {
		return getPatternHtml(editable, null);
	}
    
    public String getPatternHtml(boolean editable, Long uniqueId) {
        return getPatternHtml(editable, uniqueId, true);
    }
    
	public String getPatternHtml(boolean editable, Long uniqueId, boolean includeScript) {
		StringBuffer sb = new StringBuffer(); 
        if (includeScript)
            sb.append("<script language='JavaScript' type='text/javascript' src='scripts/datepatt.js'></script>");
		sb.append("<script language='JavaScript'>");
		sb.append(
			"calGenerate("+getSession().getSessionStartYear()+","+
				(getSession().getPatternStartMonth()) +","+
				(getSession().getPatternEndMonth())+","+
				getPatternArray()+","+
				"['1','0'],"+
				"['Classes offered','Classes not offered'],"+
				"['rgb(240,240,50)','rgb(240,240,240)'],"+
				"'1',"+
				getBorderArray(uniqueId)+","+editable+","+editable+");");
		sb.append("</script>");
		return sb.toString();
	}
	
	public void setPatternAndOffset(HttpServletRequest request) {
		int startMonth = getSession().getPatternStartMonth();
		int endMonth = getSession().getPatternEndMonth();
		int firstOne = 0, lastOne = 0;
		int year = getSession().getSessionStartYear();
		StringBuffer sb = null;
		int idx = getSession().getDayOfYear(1,startMonth);
		for (int m=startMonth;m<=endMonth;m++) {
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, year);
			int yr = DateUtils.calculateActualYear(m, year);
			for (int d=1;d<=daysOfMonth;d++) {
				String offered = request.getParameter("cal_val_"+yr+"_"+((12+m)%12)+"_"+d);
				if (offered!=null) {
					if (sb!=null || !offered.equals("0")) {
						if (sb==null) {
							firstOne = idx; sb = new StringBuffer();
						}
						sb.append(offered);
					}
					if (!offered.equals("0")) lastOne=idx;
				}
				idx++;
			}
		}
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(getSession().getSessionBeginDateTime());
		if (sb!=null) {
			setPattern(sb.substring(0,lastOne-firstOne+1));
			setOffset(new Integer(cal.get(Calendar.DAY_OF_YEAR)-firstOne-1));
		} else {
			setPattern(""); setOffset(new Integer(0));
		}
	}

    public static DatePattern findByName(HttpServletRequest request, String name) throws Exception {
    	return findByName(Session.getCurrentAcadSession(Web.getUser(request.getSession())), name);
	}

    public static DatePattern findByName(Session session, String name) {
    	List list = (new DatePatternDAO()).getSession().
    		createQuery("select distinct p from DatePattern as p where p.session.uniqueId=:sessionId and p.name=:name").
    		setLong("sessionId",session.getUniqueId().longValue()).
			setText("name",name).setCacheable(true).list();
    	if (list==null || list.isEmpty()) return null;
    	return (DatePattern)list.get(0);
	}
    
    public static Vector findAll(HttpServletRequest request, Department department, DatePattern includeGiven) throws Exception {
    	User user = Web.getUser(request.getSession());
    	Session session = Session.getCurrentAcadSession(user);
    	boolean includeExtended = user.isAdmin();
    	return findAll(session, includeExtended, department, includeGiven);
    }
    
    public static Vector findAll(Session session, boolean includeExtended, Department department, DatePattern includeGiven) {
    	Vector list = new Vector((new DatePatternDAO()).getSession().
    		createQuery("select distinct p from DatePattern as p where p.session.uniqueId=:sessionId"+(!includeExtended?" and p.type!="+sTypeExtended:"")).
    		setLong("sessionId",session.getUniqueId().longValue()).
    		setCacheable(true).
			list());

    	if (!includeExtended && department!=null) {
    		for (Iterator i=department.getDatePatterns().iterator();i.hasNext();) {
    			DatePattern dp = (DatePattern)i.next();
    			if (dp.getType().intValue()!=sTypeExtended) continue;
    			list.add(dp);
    		}
    	}
    	if (includeGiven!=null && !list.contains(includeGiven))
    		list.add(includeGiven);
    	
    	Collections.sort(list);
    	return list;
	}

    public static Set findAllUsed(Long sessionId) {
    	TreeSet ret = new TreeSet(
    			(new DatePatternDAO()).
        		getSession().
        		createQuery("select distinct dp from Class_ as c inner join c.datePattern as dp where dp.session.uniqueId=:sessionId").
        		setLong("sessionId", sessionId.longValue()).
        		setCacheable(true).list());
    	ret.addAll((new DatePatternDAO()).
        		getSession().
        		createQuery("select distinct dp from SchedulingSubpart as s inner join s.datePattern as dp where dp.session.uniqueId=:sessionId").
        		setLong("sessionId", sessionId.longValue()).
        		setCacheable(true).list());
    	Session session = new SessionDAO().get(sessionId);
    	if (session.getDefaultDatePattern()!=null) ret.add(session.getDefaultDatePattern());
    	return ret;
    }

    public boolean isUsed() {
    	return findAllUsed(getSession().getUniqueId()).contains(this);
    }
    
    public boolean isDefault() {
    	return this.equals(getSession().getDefaultDatePattern());
    }

    public int size() {
		if (getPattern()==null) return 0;
		int size = 0;
		for (int i=0;i<getPattern().length();i++)
			if ('1'==getPattern().charAt(i)) size++;
		return size;
	}
	
	public int compareTo(Object o) {
    	if (o==null || !(o instanceof DatePattern)) return -1;
    	DatePattern dp = (DatePattern)o;
    	int cmp = getType().compareTo(dp.getType());
    	if (cmp!=0) return cmp;
    	if (dp.getType().intValue()==sTypeStandard) {
    		if (Math.abs(dp.size()-size())>5) {
    			cmp = Double.compare(dp.size(),size());
    			if (cmp!=0) return cmp;
    		}
        	cmp = getStartDate().compareTo(dp.getStartDate());
        	if (cmp!=0) return cmp;
    	} else {
        	cmp = getStartDate().compareTo(dp.getStartDate());
        	if (cmp!=0) return cmp;
    		if (Math.abs(dp.size()-size())>5) {
    			cmp = -Double.compare(dp.size(),size());
    			if (cmp!=0) return cmp;
    		}
    	}
    	cmp = getName().compareTo(dp.getName());
    	if (cmp!=0) return cmp;
    	return getUniqueId().compareTo(dp.getUniqueId());
    }
	
	public Object clone() {
		DatePattern dp = new DatePattern();
		dp.setSession(getSession());
		dp.setOffset(getOffset());
		dp.setPattern(getPattern());
		dp.setName(getName());
		dp.setType(getType());
		dp.setVisible(isVisible());
		return dp;
	}
	
	public Date getStartDate() {
		if (getPattern()==null || getOffset()==null) return null;
		int idx = getPattern().indexOf('1');
		if (idx<0) return null;
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(getSession().getSessionBeginDateTime());
		cal.add(Calendar.DAY_OF_YEAR, idx - getOffset().intValue());
		return cal.getTime();
	}

	public Date getEndDate() {
		if (getPattern()==null || getOffset()==null) return null;
		int idx = getPattern().lastIndexOf('1');
		if (idx<0) return null;
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(getSession().getSessionBeginDateTime());
		cal.add(Calendar.DAY_OF_YEAR, idx - getOffset().intValue());
		return cal.getTime();
	}
    
    public String toString() {
        return getName();
    }
    
    public DatePattern findCloseMatchDatePatternInSession(Session session){
    	Vector allDatePatterns = DatePattern.findAll(session, true, null, null);
 		TreeSet days = new TreeSet();

 		
		if (days.isEmpty()) {
			int offset = getPatternOffset();
			for (int x=0;x<getPattern().length();x++) {
                if (getPattern().charAt(x)!='1') continue;
					days.add(new Integer(x+offset));
			}
		}

		DatePattern likeDp = null;
		int likeDiff = 0;
		
		for (Iterator j=allDatePatterns.iterator();j.hasNext();) {
    		DatePattern xdp = (DatePattern)j.next();
    		if (xdp.getName().startsWith("generated")) continue;
    		if (getOffset().equals(xdp.getOffset()) && getPattern().equals(xdp.getPattern())) {
    			likeDp = xdp; likeDiff = 0;
    			break;
    		}
    		TreeSet xdays = new TreeSet();
			if (xdays.isEmpty()) {
				int offset = xdp.getPatternOffset();
				for (int x=0;x<xdp.getPattern().length();x++) {
					if (xdp.getPattern().charAt(x)!='1') continue;
					xdays.add(new Integer(x+offset));
				}
			}

			int diff = diff(days, xdays);
    		if (likeDp==null || likeDiff>diff || (likeDiff==diff && xdp.isDefault())) {
    			likeDp = xdp; likeDiff = diff;
    		}
		}
		
		if (likeDp!=null) {
            if (likeDiff<=5) {
                return(likeDp);
            }
		}
		return(null);
     }
    
	private int diff(Set x, Set y) {
		int diff = 0;
		for (Iterator i=x.iterator();i.hasNext();) {
			Object o = i.next();
			if (!y.contains(o)) diff++;
		}
		for (Iterator i=y.iterator();i.hasNext();) {
			Object o = i.next();
			if (!x.contains(o)) diff++;
		}
		return diff;
	}
	
	public static Date[] getBounds(Long sessionId) {
        Date startDate = null, endDate = null;
        for (Iterator i=DatePattern.findAllUsed(sessionId).iterator();i.hasNext();) {
            DatePattern dp = (DatePattern)i.next();
            if (startDate == null || startDate.compareTo(dp.getStartDate())>0)
                startDate = dp.getStartDate();
            if (endDate == null || endDate.compareTo(dp.getEndDate())<0)
                endDate = dp.getEndDate();
        }
        Calendar startDateCal = Calendar.getInstance(Locale.US);
        startDateCal.setTime(startDate);
        startDateCal.set(Calendar.HOUR_OF_DAY, 0);
        startDateCal.set(Calendar.MINUTE, 0);
        startDateCal.set(Calendar.SECOND, 0);
        Calendar endDateCal = Calendar.getInstance(Locale.US);
        endDateCal.setTime(endDate);
        endDateCal.set(Calendar.HOUR_OF_DAY, 23);
        endDateCal.set(Calendar.MINUTE, 59);
        endDateCal.set(Calendar.SECOND, 59);
        return new Date[] { startDateCal.getTime(), endDateCal.getTime()};
	}

}
