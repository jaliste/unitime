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
package org.unitime.timetable.form;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.ExamPeriod;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.dao.ExamPeriodDAO;
import org.unitime.timetable.model.dao.PreferenceLevelDAO;
import org.unitime.timetable.util.CalendarUtils;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.DateUtils;


/** 
 * @author Tomas Muller
 */
public class ExamPeriodEditForm extends ActionForm {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8152697759737310033L;
	private Long iUniqueId;
    private String iOp;
    private String iDate;
    private Integer iStart;
    private Integer iLength;
    private Integer iStartOffset;
    private Integer iStopOffset;
    private Integer iStart2;
    private Integer iLength2;
    private Integer iStartOffset2;
    private Integer iStopOffset2;
    private Integer iStart3;
    private Integer iLength3;
    private Integer iStartOffset3;
    private Integer iStopOffset3;
    private Integer iStart4;
    private Integer iLength4;
    private Integer iStartOffset4;
    private Integer iStopOffset4;
    private Integer iStart5;
    private Integer iLength5;
    private Integer iStartOffset5;
    private Integer iStopOffset5;
    private String iType;
    private Long iPrefLevel;
    private boolean iAutoSetup;
    private Session iSession;
    private Integer iDefaultMidtermStartOffset;
    private Integer iDefaultMidtermStopOffset;
    private Integer iDefaultFinalStartOffset;
    private Integer iDefaultFinalStopOffset;
    
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
	    ActionErrors errors = new ActionErrors();
	    
	    if (!iAutoSetup && !CalendarUtils.isValidDate(iDate, "MM/dd/yyyy"))
	        errors.add("date", new ActionMessage("errors.invalidDate", "Examination Date"));
	    
        if (iStart==null || iStart<=0) {
            if (!iAutoSetup) errors.add("start", new ActionMessage("errors.required", ""));
        } else {
            int hour = iStart/100;
            int min = iStart%100;
            if (hour>=24)
                errors.add("start", new ActionMessage("errors.generic","Invalid start time -- hour ("+hour+") must be between 0 and 23."));
            if (min>=60)
                errors.add("start", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be between 0 and 59."));
            if ((min%Constants.SLOT_LENGTH_MIN)!=0)
                errors.add("start", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));

            if (iLength==null || iLength<=0)
                errors.add("length", new ActionMessage("errors.required", ""));
            else if ((iLength%Constants.SLOT_LENGTH_MIN)!=0)
                errors.add("length", new ActionMessage("errors.generic","Invalid length -- period length must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
            if (iStartOffset != null) {
            	if(iStartOffset.intValue() < 0){
                  	errors.add("start offset", new ActionMessage("errors.generic", "Invalid event start offset -- value must be a positive integer."));    
            	}
            	if ((iStartOffset.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                    errors.add("start offset", new ActionMessage("errors.generic","Invalid event start offset -- value ("+iStartOffset.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
            	}
            }
            if (iStopOffset != null) {
            	if(iStopOffset.intValue() < 0){
                  	errors.add("stop offset", new ActionMessage("errors.generic", "Invalid event stop offset -- value must be a positive integer."));    
            	}
            	if ((iStopOffset.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                    errors.add("stop offset", new ActionMessage("errors.generic","Invalid event stop offset -- value ("+iStopOffset.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
            	}
            }
       }
	    
	    if (iAutoSetup) {
	        if (iStart2!=null && iStart2>0) {
	            int hour = iStart2/100;
	            int min = iStart2%100;
	            if (hour>=24)
	                errors.add("start2", new ActionMessage("errors.generic","Invalid start time -- hour ("+hour+") must be between 0 and 23."));
	            if (min>=60)
	                errors.add("start2", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be between 0 and 59."));
	            if ((min%Constants.SLOT_LENGTH_MIN)!=0)
	                errors.add("start2", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
	            if (iLength2==null || iLength2<=0)
	                errors.add("length2", new ActionMessage("errors.required", ""));
	            else if ((iLength2%Constants.SLOT_LENGTH_MIN)!=0)
	                errors.add("length2", new ActionMessage("errors.generic","Invalid length -- period length must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
	            if (iStartOffset2 != null) {
	            	if(iStartOffset2.intValue() < 0){
	                  	errors.add("start offset2", new ActionMessage("errors.generic", "Invalid event start offset -- value must be a positive integer."));    
	            	}
	            	if ((iStartOffset2.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
	                    errors.add("start offset2", new ActionMessage("errors.generic","Invalid event start offset -- value ("+iStartOffset2.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
	            	}
	            }
	            if (iStopOffset2 != null) {
	            	if(iStopOffset2.intValue() < 0){
	                  	errors.add("stop offset2", new ActionMessage("errors.generic", "Invalid event stop offset -- value must be a positive integer."));    
	            	}
	            	if ((iStopOffset2.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
	                    errors.add("stop offset2", new ActionMessage("errors.generic","Invalid event stop offset -- value ("+iStopOffset2.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
	            	}
	            }
	        }
	        
            if (iStart3!=null && iStart3>0) {
                int hour = iStart3/100;
                int min = iStart3%100;
                if (hour>=24)
                    errors.add("start3", new ActionMessage("errors.generic","Invalid start time -- hour ("+hour+") must be between 0 and 23."));
                if (min>=60)
                    errors.add("start3", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be between 0 and 59."));
                if ((min%Constants.SLOT_LENGTH_MIN)!=0)
                    errors.add("start3", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
                if (iLength3==null || iLength3<=0)
                    errors.add("length3", new ActionMessage("errors.required", ""));
                else if ((iLength3%Constants.SLOT_LENGTH_MIN)!=0)
                    errors.add("length3", new ActionMessage("errors.generic","Invalid length -- period length must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
                if (iStartOffset3 != null) {
                	if(iStartOffset3.intValue() < 0){
                      	errors.add("start offset3", new ActionMessage("errors.generic", "Invalid event start offset -- value must be a positive integer."));    
                	}
                	if ((iStartOffset3.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                        errors.add("start offset3", new ActionMessage("errors.generic","Invalid event start offset -- value ("+iStartOffset3.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
                	}
                }
                if (iStopOffset3 != null) {
                	if(iStopOffset3.intValue() < 0){
                      	errors.add("stop offset3", new ActionMessage("errors.generic", "Invalid event stop offset -- value must be a positive integer."));    
                	}
                	if ((iStopOffset3.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                        errors.add("stop offset3", new ActionMessage("errors.generic","Invalid event stop offset -- value ("+iStopOffset3.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
                	}
                }
           }

            if (iStart4!=null && iStart4>0) {
                int hour = iStart4/100;
                int min = iStart4%100;
                if (hour>=24)
                    errors.add("start4", new ActionMessage("errors.generic","Invalid start time -- hour ("+hour+") must be between 0 and 23."));
                if (min>=60)
                    errors.add("start4", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be between 0 and 59."));
                if ((min%Constants.SLOT_LENGTH_MIN)!=0)
                    errors.add("start4", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
                if (iLength4==null || iLength4<=0)
                    errors.add("length4", new ActionMessage("errors.required", ""));
                else if ((iLength4%Constants.SLOT_LENGTH_MIN)!=0)
                    errors.add("length4", new ActionMessage("errors.generic","Invalid length -- period length must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
                if (iStartOffset4 != null) {
                	if(iStartOffset4.intValue() < 0){
                      	errors.add("start offset4", new ActionMessage("errors.generic", "Invalid event start offset -- value must be a positive integer."));    
                	}
                	if ((iStartOffset4.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                        errors.add("start offset4", new ActionMessage("errors.generic","Invalid event start offset -- value ("+iStartOffset4.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
                	}
                }
                if (iStopOffset4 != null) {
                	if(iStopOffset4.intValue() < 0){
                      	errors.add("stop offset4", new ActionMessage("errors.generic", "Invalid event stop offset -- value must be a positive integer."));    
                	}
                	if ((iStopOffset4.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                        errors.add("stop offset4", new ActionMessage("errors.generic","Invalid event stop offset -- value ("+iStopOffset4.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
                	}
                }
            }

            if (iStart5!=null && iStart5>0) {
                int hour = iStart5/100;
                int min = iStart5%100;
                if (hour>=24)
                    errors.add("start5", new ActionMessage("errors.generic","Invalid start time -- hour ("+hour+") must be between 0 and 23."));
                if (min>=60)
                    errors.add("start5", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be between 0 and 59."));
                if ((min%Constants.SLOT_LENGTH_MIN)!=0)
                    errors.add("start5", new ActionMessage("errors.generic","Invalid start time -- minute ("+min+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
                if (iLength5==null || iLength5<=0)
                    errors.add("length5", new ActionMessage("errors.required", ""));
                else if ((iLength5%Constants.SLOT_LENGTH_MIN)!=0)
                    errors.add("length5", new ActionMessage("errors.generic","Invalid length -- period length must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));
                if (iStartOffset5 != null) {
                	if(iStartOffset5.intValue() < 0){
                      	errors.add("start offset5", new ActionMessage("errors.generic", "Invalid event start offset -- value must be a positive integer."));    
                	}
                	if ((iStartOffset5.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                        errors.add("start offset5", new ActionMessage("errors.generic","Invalid event start offset -- value ("+iStartOffset5.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
                	}
                }
                if (iStopOffset5 != null) {
                	if(iStopOffset5.intValue() < 0){
                      	errors.add("stop offset5", new ActionMessage("errors.generic", "Invalid event stop offset -- value must be a positive integer."));    
                	}
                	if ((iStopOffset5.intValue()%Constants.SLOT_LENGTH_MIN)!=0){
                        errors.add("stop offset5", new ActionMessage("errors.generic","Invalid event stop offset -- value ("+iStopOffset5.intValue()+") must be divisible by "+Constants.SLOT_LENGTH_MIN+"."));            		
                	}
                }
            }
	    }
	    
	    try {
	        if (errors.isEmpty()) {
	            User user = Web.getUser(request.getSession());
	            Session session = Session.getCurrentAcadSession(user);
	            Date startDate = new SimpleDateFormat("MM/dd/yyyy").parse(iDate);
	            long diff = startDate.getTime()-session.getExamBeginDate().getTime();
	            int dateOffset = (int)Math.round(diff/(1000.0 * 60 * 60 * 24)); 
	            int hour = iStart / 100;
	            int min = iStart % 100;
	            int slot = (hour*60 + min - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
	            ExamPeriod period = ExamPeriod.findByDateStart(session.getUniqueId(), dateOffset, slot);
	            if (period!=null && !period.getUniqueId().equals(getUniqueId())) {
	                errors.add("date", new ActionMessage("errors.exists", "An examination period with given date and start time"));
	            }
	        }
	    } catch (Exception e) {}
	    
	    return errors;
	}
	
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iOp = null; iUniqueId = new Long(-1); iDate = null; 
		iStart = null; iLength = null; iStartOffset = null; iStopOffset = null;
		iStart2 = null; iLength2 = null; iStartOffset2 = null; iStopOffset2 = null;
		iStart3 = null; iLength3 = null; iStartOffset3 = null; iStopOffset3 = null;
		iStart4 = null; iLength4 = null; iStartOffset4 = null; iStopOffset4 = null;
		iStart5 = null; iLength5 = null; iStartOffset5 = null; iStopOffset5 = null;
		iDefaultMidtermStartOffset = Constants.getDefaultExamStartOffset(Exam.sExamTypeMidterm);
		iDefaultMidtermStopOffset = Constants.getDefaultExamStopOffset(Exam.sExamTypeMidterm);
		iDefaultFinalStartOffset = Constants.getDefaultExamStartOffset(Exam.sExamTypeFinal);
		iDefaultFinalStopOffset = Constants.getDefaultExamStopOffset(Exam.sExamTypeFinal);
		iPrefLevel = PreferenceLevel.getPreferenceLevel(PreferenceLevel.sNeutral).getUniqueId();
		iType = Exam.sExamTypes[Exam.sExamTypeFinal];
		iAutoSetup = false;
		try {
			iSession = Session.getCurrentAcadSession(Web.getUser(request.getSession()));
		} catch (Exception e) {}
	}
	
	public void load(ExamPeriod ep, HttpServletRequest request) throws Exception {
		if (ep==null) {
			reset(null, null);
	        User user = Web.getUser(request.getSession());
	        Session session = Session.getCurrentAcadSession(user);
			iDate = new SimpleDateFormat("MM/dd/yyyy").format(session.getExamBeginDate());
			iLength = 120;
			TreeSet periods = ExamPeriod.findAll(request, null);
			int maxType = 0;
			if (!periods.isEmpty()) {
			    TreeSet times = new TreeSet();
			    for (Iterator i=periods.iterator();i.hasNext();) {
			        ExamPeriod p = (ExamPeriod)i.next();
			        times.add(p.getStartSlot());
			        maxType = Math.max(maxType, p.getExamType());
			    }
			    for (Iterator i=times.iterator();i.hasNext();) {
			        Integer start = (Integer)i.next();
			        if (start.equals(((ExamPeriod)periods.last()).getStartSlot()) && i.hasNext()) {
			            int time = Constants.SLOT_LENGTH_MIN*(Integer)i.next()+Constants.FIRST_SLOT_TIME_MIN;
			            iStart = 100*(time/60)+(time%60);
			            break;
			        }
			    }
			    iLength = ((ExamPeriod)periods.last()).getLength()*Constants.SLOT_LENGTH_MIN;
			    iDate = new SimpleDateFormat("MM/dd/yyyy").format(((ExamPeriod)periods.last()).getStartDate());
			    
			}

			iPrefLevel = PreferenceLevel.getPreferenceLevel(PreferenceLevel.sNeutral).getUniqueId();
			iType = Exam.sExamTypes[maxType];
			if (iType != null && iType.equals(Exam.sExamTypes[Exam.sExamTypeFinal])){
		    	iStartOffset = iDefaultFinalStartOffset;
		    	iStopOffset = iDefaultFinalStopOffset;
		    } else {
		    	iStartOffset = iDefaultMidtermStartOffset;
		    	iStopOffset = iDefaultMidtermStopOffset;
		    }	
			iOp = "Save";
		} else {
		    iUniqueId = ep.getUniqueId();
			iDate = new SimpleDateFormat("MM/dd/yyyy").format(ep.getStartDate());
			iStart = ep.getStartHour()*100 + ep.getStartMinute();
			iLength = ep.getLength() * Constants.SLOT_LENGTH_MIN;
			iStartOffset = ep.getEventStartOffset() * Constants.SLOT_LENGTH_MIN;
			iStopOffset = ep.getEventStopOffset() * Constants.SLOT_LENGTH_MIN;
			iPrefLevel = ep.getPrefLevel().getUniqueId();
			iType = Exam.sExamTypes[ep.getExamType()];
			iOp = "Update";
		}
	}
	
	public void update(ExamPeriod ep, HttpServletRequest request, org.hibernate.Session hibSession) throws Exception {
	    ep.setStartDate(new SimpleDateFormat("MM/dd/yyyy").parse(iDate));
	    int hour = iStart / 100;
	    int min = iStart % 100;
	    int slot = (hour*60 + min - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
	    ep.setStartSlot(slot);
	    ep.setLength(iLength / Constants.SLOT_LENGTH_MIN);
	    ep.setPrefLevel(new PreferenceLevelDAO().get(iPrefLevel));
	    ep.setExamType(getExamTypeIdx());
	    ep.setEventStartOffset(iStartOffset == null?new Integer(0):new Integer(iStartOffset.intValue()/Constants.SLOT_LENGTH_MIN));
	    ep.setEventStopOffset(iStopOffset == null?new Integer(0):new Integer(iStopOffset.intValue()/Constants.SLOT_LENGTH_MIN));
		hibSession.saveOrUpdate(ep);
	}
	
	public ExamPeriod create(HttpServletRequest request, org.hibernate.Session hibSession) throws Exception {
	    ExamPeriod ep = new ExamPeriod();
        User user = Web.getUser(request.getSession());
        Session session = Session.getCurrentAcadSession(user);
        ep.setSession(session);
        ep.setStartDate(new SimpleDateFormat("MM/dd/yyyy").parse(iDate));
        int hour = iStart / 100;
        int min = iStart % 100;
        int slot = (hour*60 + min - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
        ep.setStartSlot(slot);
        ep.setLength(iLength / Constants.SLOT_LENGTH_MIN);
        ep.setPrefLevel(new PreferenceLevelDAO().get(iPrefLevel));
        ep.setExamType(getExamTypeIdx());
        ep.setEventStartOffset(iStartOffset == null?new Integer(0):new Integer(iStartOffset.intValue() / Constants.SLOT_LENGTH_MIN));
        ep.setEventStopOffset(iStopOffset == null?new Integer(0):new Integer(iStopOffset.intValue() / Constants.SLOT_LENGTH_MIN));
        hibSession.saveOrUpdate(ep);
		setUniqueId(ep.getUniqueId());
		return ep;
	}
	
	public ExamPeriod saveOrUpdate(HttpServletRequest request, org.hibernate.Session hibSession) throws Exception {
		if (iSession==null) iSession = Session.getCurrentAcadSession(Web.getUser(request.getSession()));
		if (getAutoSetup()) {
			setDays(request);
			TreeSet periods = ExamPeriod.findAll(request, Exam.sExamTypeMidterm);
			TreeSet<Integer> slots = new TreeSet();
			TreeSet oldDays = new TreeSet();
			for (Iterator i=periods.iterator();i.hasNext();) {
				ExamPeriod period = (ExamPeriod)i.next();
				slots.add(period.getStartSlot());
				if (!iDays.contains(period.getDateOffset())) {
				    for (Iterator j=hibSession.createQuery(
                    "select x from Exam x where x.assignedPeriod.uniqueId=:periodId")
                    .setLong("periodId", period.getUniqueId())
                    .iterate();j.hasNext();) {
				        Exam exam = (Exam)j.next();
				        exam.unassign(Web.getUser(request.getSession()).getId(), hibSession);
				    }
					hibSession.delete(period);
					i.remove();
				} else {
					oldDays.add(period.getDateOffset());
				}
			}
			Hashtable<Integer,Integer> length = new Hashtable();
			Hashtable<Integer,Integer> translate = new Hashtable();
			Hashtable<Integer,Integer> eventStartOffsets = new Hashtable();
			Hashtable<Integer,Integer> eventStopOffsets = new Hashtable();
			
			TreeSet<Integer> newStarts = new TreeSet();
			Iterator<Integer> it = slots.iterator();
            if (iStart!=null && iStart>0) {
                int slot = ((iStart/100)*60 + (iStart%100) - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN; 
                length.put(slot, iLength);
                eventStartOffsets.put(slot, iStartOffset);
                eventStopOffsets.put(slot, iStopOffset);
                if (it.hasNext()) translate.put(it.next(), slot); else newStarts.add(slot);
            } else if (it.hasNext()) it.next();
            if (iStart2!=null && iStart2>0) {
                int slot = ((iStart2/100)*60 + (iStart2%100) - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
                length.put(slot, iLength2);
                eventStartOffsets.put(slot, iStartOffset2);
                eventStopOffsets.put(slot, iStopOffset2);
                if (it.hasNext()) translate.put(it.next(), slot); else newStarts.add(slot);
            } else if (it.hasNext()) it.next();
            if (iStart3!=null && iStart3>0) {
                int slot = ((iStart3/100)*60 + (iStart3%100) - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
                length.put(slot, iLength3);
                eventStartOffsets.put(slot, iStartOffset3);
                eventStopOffsets.put(slot, iStopOffset3);
               if (it.hasNext()) translate.put(it.next(), slot); else newStarts.add(slot);
            } else if (it.hasNext()) it.next();
            if (iStart4!=null && iStart4>0) {
                int slot = ((iStart4/100)*60 + (iStart4%100) - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
                length.put(slot, iLength4);
                eventStartOffsets.put(slot, iStartOffset4);
                eventStopOffsets.put(slot, iStopOffset4);
                if (it.hasNext()) translate.put(it.next(), slot); else newStarts.add(slot);
            } else if (it.hasNext()) it.next();
            if (iStart5!=null && iStart5>0) {
                int slot = ((iStart5/100)*60 + (iStart5%100) - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
                length.put(slot, iLength5);
                eventStartOffsets.put(slot, iStartOffset5);
                eventStopOffsets.put(slot, iStopOffset5);
                if (it.hasNext()) translate.put(it.next(), slot); else newStarts.add(slot);
            } else if (it.hasNext()) it.next();
			for (Iterator i=periods.iterator();i.hasNext();) {
				ExamPeriod period = (ExamPeriod)i.next();
				Integer start = translate.get(period.getStartSlot());
				if (start==null) {
				    for (Iterator j=hibSession.createQuery(
				            "select x from Exam x where x.assignedPeriod.uniqueId=:periodId")
				            .setLong("periodId", period.getUniqueId())
				            .iterate();j.hasNext();) {
				        Exam exam = (Exam)j.next();
				        exam.unassign(Web.getUser(request.getSession()).getId(), hibSession);
				    }
				    hibSession.delete(period);
				    i.remove();
				} else {
				    period.setStartSlot(start);
				    period.setLength(length.get(start) / Constants.SLOT_LENGTH_MIN);
				    period.setEventStartOffset(eventStartOffsets.get(start) == null?new Integer(null):new Integer(eventStartOffsets.get(start)/Constants.SLOT_LENGTH_MIN));
				    period.setEventStopOffset(eventStopOffsets.get(start) == null?new Integer(null):new Integer(eventStopOffsets.get(start)/Constants.SLOT_LENGTH_MIN));
				    hibSession.update(period);
				}
			}
			for (Iterator i=iDays.iterator();i.hasNext();) {
				Integer day = (Integer)i.next();
				for (int start : new TreeSet<Integer>(length.keySet())) {
				    if (oldDays.contains(day) && !newStarts.contains(start)) continue;
				    ExamPeriod ep = new ExamPeriod();
				    ep.setSession(iSession);
				    ep.setDateOffset(day);
				    ep.setStartSlot(start);
				    ep.setLength(length.get(start) / Constants.SLOT_LENGTH_MIN);
				    ep.setEventStartOffset(eventStartOffsets.get(start) == null?new Integer(null):new Integer(eventStartOffsets.get(start)/Constants.SLOT_LENGTH_MIN));
				    ep.setEventStopOffset(eventStopOffsets.get(start) == null?new Integer(null):new Integer(eventStopOffsets.get(start)/Constants.SLOT_LENGTH_MIN));
				    ep.setExamType(Exam.sExamTypeMidterm);
				    ep.setPrefLevel(PreferenceLevel.getPreferenceLevel(PreferenceLevel.sNeutral));
				    hibSession.save(ep);
				}
			}
			return null;
		} else {
			ExamPeriod ep = null;
			if (getUniqueId().longValue()>=0)
				ep = (new ExamPeriodDAO()).get(getUniqueId());
			if (ep==null)
				ep = create(request, hibSession);
			else 
				update(ep, request, hibSession);
			return ep;
		}
	}
	
	public void delete(HttpServletRequest request, org.hibernate.Session hibSession) throws Exception {
		if (getUniqueId().longValue()<0) return;
		ExamPeriod ep = (new ExamPeriodDAO()).get(getUniqueId(), hibSession);
		for (Iterator j=hibSession.createQuery(
		        "select x from Exam x where x.assignedPeriod.uniqueId=:periodId")
		        .setLong("periodId", ep.getUniqueId())
		        .iterate();j.hasNext();) {
		    Exam exam = (Exam)j.next();
            exam.unassign(Web.getUser(request.getSession()).getId(), hibSession);
		}
		hibSession.delete(ep);
	}
	
	public String getOp() { return iOp; }
	public void setOp(String op) { iOp = op; }
	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }
	public String getDate() { return iDate; }
	public void setDate(String date) { iDate = date; }
	public Integer getStart() { return iStart; }
	public void setStart(Integer start) { iStart = start; }
    public Integer getLength() { return iLength; }
    public void setLength(Integer length) { iLength = length; }
    public Integer getStart2() { return iStart2; }
    public void setStart2(Integer start2) { iStart2 = start2; }
    public Integer getLength2() { return iLength2; }
    public void setLength2(Integer length2) { iLength2 = length2; }
    public Integer getStart3() { return iStart3; }
    public void setStart3(Integer start3) { iStart3 = start3; }
    public Integer getLength3() { return iLength3; }
    public void setLength3(Integer length3) { iLength3 = length3; }
    public Integer getStart4() { return iStart4; }
    public void setStart4(Integer start4) { iStart4 = start4; }
    public Integer getLength4() { return iLength4; }
    public void setLength4(Integer length4) { iLength4 = length4; }
    public Integer getStart5() { return iStart5; }
    public void setStart5(Integer start5) { iStart5 = start5; }
    public Integer getLength5() { return iLength5; }
    public void setLength5(Integer length5) { iLength5 = length5; }
    public Long getPrefLevel() { return iPrefLevel; }
    public void setPrefLevel(Long prefLevel) { iPrefLevel = prefLevel; }
	public Integer getStartOffset() {
		return iStartOffset;
	}

	public void setStartOffset(Integer startOffset) {
		iStartOffset = startOffset;
	}

	public Integer getStopOffset() {
		return iStopOffset;
	}

	public void setStopOffset(Integer stopOffset) {
		iStopOffset = stopOffset;
	}

	public Integer getStartOffset2() {
		return iStartOffset2;
	}

	public void setStartOffset2(Integer startOffset2) {
		iStartOffset2 = startOffset2;
	}

	public Integer getStopOffset2() {
		return iStopOffset2;
	}

	public void setStopOffset2(Integer stopOffset2) {
		iStopOffset2 = stopOffset2;
	}

	public Integer getStartOffset3() {
		return iStartOffset3;
	}

	public void setStartOffset3(Integer startOffset3) {
		iStartOffset3 = startOffset3;
	}

	public Integer getStopOffset3() {
		return iStopOffset3;
	}

	public void setStopOffset3(Integer stopOffset3) {
		iStopOffset3 = stopOffset3;
	}

	public Integer getStartOffset4() {
		return iStartOffset4;
	}

	public void setStartOffset4(Integer startOffset4) {
		iStartOffset4 = startOffset4;
	}

	public Integer getStopOffset4() {
		return iStopOffset4;
	}

	public void setStopOffset4(Integer stopOffset4) {
		iStopOffset4 = stopOffset4;
	}

	public Integer getStartOffset5() {
		return iStartOffset5;
	}

	public void setStartOffset5(Integer startOffset5) {
		iStartOffset5 = startOffset5;
	}

	public Integer getStopOffset5() {
		return iStopOffset5;
	}

	public void setStopOffset5(Integer stopOffset5) {
		iStopOffset5 = stopOffset5;
	}
    
    public Vector getPrefLevels() {
        Vector ret = new Vector();
        for (Enumeration e=PreferenceLevel.getPreferenceLevelList(false).elements();e.hasMoreElements();) {
            PreferenceLevel level = (PreferenceLevel)e.nextElement();
            if (PreferenceLevel.sRequired.equals(level.getPrefProlog())) continue;
            ret.addElement(level);
        }
        return ret;
    }
    public String getExamType() { return iType; }
    public void setExamType(String type) { iType = type; }
    public String[] getExamTypes() { return Exam.sExamTypes; }
    public int getExamTypeIdx() {
    	for (int i=0;i<Exam.sExamTypes.length;i++) {
    		if (Exam.sExamTypes[i].equals(getExamType())) return i;
    	}
    	return Exam.sExamTypeFinal;
    }
    public boolean getAutoSetup() { return iAutoSetup; }
    public void setAutoSetup(boolean autoSetup) { iAutoSetup = autoSetup; }
    
	public String getBorder(int day, int month) {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(iSession.getSessionBeginDateTime());
		if (day==cal.get(Calendar.DAY_OF_MONTH) && month==cal.get(Calendar.MONTH))
			return "'blue 2px solid'";
		cal.setTime(iSession.getSessionEndDateTime());
		if (day==cal.get(Calendar.DAY_OF_MONTH) && month==cal.get(Calendar.MONTH))
			return "'blue 2px solid'";
		cal.setTime(iSession.getExamBeginDate());
		if (day==cal.get(Calendar.DAY_OF_MONTH) && month==cal.get(Calendar.MONTH))
			return "'green 2px solid'";
		int holiday = iSession.getHoliday(day, month);
		if (holiday!=Session.sHolidayTypeNone)
			return "'"+Session.sHolidayTypeColors[holiday]+" 2px solid'";
		return "null";
	}
	
	TreeSet<Integer> iDays = null;
	public boolean getCanAutoSetup() {
		iDays = new TreeSet<Integer>(); 
		TreeSet<Integer> times = new TreeSet<Integer>(); 
		Hashtable<Integer, Integer> lengths = new Hashtable<Integer, Integer>(); 
		Hashtable<Integer, Integer> eventStartOffsets = new Hashtable<Integer, Integer>(); 
		Hashtable<Integer, Integer> eventStopOffsets = new Hashtable<Integer, Integer>(); 
		TreeSet periods = ExamPeriod.findAll(iSession.getUniqueId(),Exam.sExamTypeMidterm);
		iType = Exam.sSeatingTypes[Exam.sExamTypeMidterm];
		for (Iterator i=periods.iterator();i.hasNext();) {
			ExamPeriod period = (ExamPeriod)i.next();
			iDays.add(period.getDateOffset());
			times.add(period.getStartSlot());
			Integer length = lengths.get(period.getStartSlot());
			if (length==null)
				lengths.put(period.getStartSlot(),period.getLength());
			else if (!length.equals(period.getLength())) {
				return false;
			}
			Integer eventStartOffset = eventStartOffsets.get(period.getStartSlot());
			if (eventStartOffset == null){
				eventStartOffsets.put(period.getStartSlot(), period.getEventStartOffset());
			} else if (!eventStartOffset.equals(period.getEventStartOffset())){
				return(false);
			}
			Integer eventStopOffset = eventStopOffsets.get(period.getStartSlot());
			if (eventStopOffset == null){
				eventStopOffsets.put(period.getStartSlot(), period.getEventStopOffset());
			} else if (!eventStopOffset.equals(period.getEventStopOffset())){
				return(false);
			}
		}
		if (periods.size()!=iDays.size()*times.size() || times.size()>5) return false;
		if (times.isEmpty()) {
		    iStart = 1830; iLength = 60; iStartOffset = iDefaultMidtermStartOffset; iStopOffset = iDefaultMidtermStopOffset;
		    iStart2 = 2000; iLength2 = 120; iStartOffset2 = iDefaultMidtermStartOffset; iStopOffset2 = iDefaultMidtermStopOffset;
		    iStart3 = null; iLength3 = null; iStartOffset3 = iDefaultMidtermStartOffset; iStopOffset3 = iDefaultMidtermStopOffset;
		    iStart4 = null; iLength4 = null; iStartOffset4 = iDefaultMidtermStartOffset; iStopOffset4 = iDefaultMidtermStopOffset;
		    iStart5 = null; iLength5 = null; iStartOffset5 = iDefaultMidtermStartOffset; iStopOffset5 = iDefaultMidtermStopOffset;
		}
		Iterator<Integer> it = times.iterator();
        if (it.hasNext()) {
            int slot = it.next();
            int min = Constants.SLOT_LENGTH_MIN*times.first()+Constants.FIRST_SLOT_TIME_MIN;
            iStart = 100 * (min / 60) + (min % 60);
            iLength = Constants.SLOT_LENGTH_MIN * lengths.get(slot);
            iStartOffset = Constants.SLOT_LENGTH_MIN * eventStartOffsets.get(slot);
            iStopOffset = Constants.SLOT_LENGTH_MIN * eventStopOffsets.get(slot);
       } else {
            iStart = null; iLength = null; 
            if (iType.equals(Exam.sSeatingTypes[Exam.sExamTypeMidterm])){
            	iStartOffset = iDefaultMidtermStartOffset; iStopOffset = iDefaultMidtermStopOffset;
            } else {
            	iStartOffset = iDefaultFinalStartOffset; iStopOffset = iDefaultFinalStopOffset;
            }
        }
        if (it.hasNext()) {
            int slot = it.next();
            int min = Constants.SLOT_LENGTH_MIN*slot+Constants.FIRST_SLOT_TIME_MIN;
            iStart2 = 100 * (min / 60) + (min % 60);
            iLength2 = Constants.SLOT_LENGTH_MIN * lengths.get(slot);
            iStartOffset2 = Constants.SLOT_LENGTH_MIN * eventStartOffsets.get(slot);
            iStopOffset2 = Constants.SLOT_LENGTH_MIN * eventStopOffsets.get(slot);
        } else {
            iStart2 = null; iLength2 = null;
            if (iType.equals(Exam.sSeatingTypes[Exam.sExamTypeMidterm])){
            	iStartOffset2 = iDefaultMidtermStartOffset; iStopOffset2 = iDefaultMidtermStopOffset;
            } else {
            	iStartOffset2 = iDefaultFinalStartOffset; iStopOffset2 = iDefaultFinalStopOffset;
            }
        }
        if (it.hasNext()) {
            int slot = it.next();
            int min = Constants.SLOT_LENGTH_MIN*slot+Constants.FIRST_SLOT_TIME_MIN;
            iStart3 = 100 * (min / 60) + (min % 60);
            iLength3 = Constants.SLOT_LENGTH_MIN * lengths.get(slot);
            iStartOffset3 = Constants.SLOT_LENGTH_MIN * eventStartOffsets.get(slot);
            iStopOffset3 = Constants.SLOT_LENGTH_MIN * eventStopOffsets.get(slot);
        } else {
            iStart3 = null; iLength3 = null; 
            if (iType.equals(Exam.sSeatingTypes[Exam.sExamTypeMidterm])){
            	iStartOffset3 = iDefaultMidtermStartOffset; iStopOffset3 = iDefaultMidtermStopOffset;
            } else {
            	iStartOffset3 = iDefaultFinalStartOffset; iStopOffset3 = iDefaultFinalStopOffset;
            }
        }
        if (it.hasNext()) {
            int slot = it.next();
            int min = Constants.SLOT_LENGTH_MIN*slot+Constants.FIRST_SLOT_TIME_MIN;
            iStart4 = 100 * (min / 60) + (min % 60);
            iLength4 = Constants.SLOT_LENGTH_MIN * lengths.get(slot);
            iStartOffset4 = Constants.SLOT_LENGTH_MIN * eventStartOffsets.get(slot);
            iStopOffset4 = Constants.SLOT_LENGTH_MIN * eventStopOffsets.get(slot);
        } else {
            iStart4 = null; iLength4 = null;
            if (iType.equals(Exam.sSeatingTypes[Exam.sExamTypeMidterm])){
            	iStartOffset4 = iDefaultMidtermStartOffset; iStopOffset4 = iDefaultMidtermStopOffset;
            } else {
            	iStartOffset4 = iDefaultFinalStartOffset; iStopOffset4 = iDefaultFinalStopOffset;
            }
        }
        if (it.hasNext()) {
            int slot = it.next();
            int min = Constants.SLOT_LENGTH_MIN*slot+Constants.FIRST_SLOT_TIME_MIN;
            iStart5 = 100 * (min / 60) + (min % 60);
            iLength5 = Constants.SLOT_LENGTH_MIN * lengths.get(slot);
            iStartOffset5 = Constants.SLOT_LENGTH_MIN * eventStartOffsets.get(slot);
            iStopOffset5 = Constants.SLOT_LENGTH_MIN * eventStopOffsets.get(slot);
        } else {
            iStart5 = null; iLength5 = null;
            if (iType.equals(Exam.sSeatingTypes[Exam.sExamTypeMidterm])){
            	iStartOffset5 = iDefaultMidtermStartOffset; iStopOffset5 = iDefaultMidtermStopOffset;
            } else {
            	iStartOffset5 = iDefaultFinalStartOffset; iStopOffset5 = iDefaultFinalStopOffset;
            }
        }
		return true;
	}
	
	public int getExamOffset() {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(iSession.getExamBeginDate());
		return(DateUtils.getDayOfYear(cal.get(Calendar.DAY_OF_MONTH), DateUtils.getStartMonth(iSession.getExamBeginDate(), iSession.getSessionStartYear(), 0), iSession.getSessionStartYear()) + 1);
	}
	
	public boolean hasExam(int day, int month) {
		return iDays.contains(1+iSession.getDayOfYear(day, month)-getExamOffset());
	}

	
	public String getPatternHtml() {
		try {
		int startMonth = iSession.getStartMonth();
		int endMonth = iSession.getEndMonth();
		StringBuffer border = new StringBuffer("[");
		StringBuffer pattern = new StringBuffer("[");
		for (int m=startMonth;m<=endMonth;m++) {
			if (m!=startMonth) { border.append(","); pattern.append(","); }
			border.append("["); pattern.append("[");
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, iSession.getSessionStartYear());
			for (int d=1;d<=daysOfMonth;d++) {
				if (d>1) { border.append(","); pattern.append(","); }
				border.append(getBorder(d,m));
				pattern.append(hasExam(d,m)?"'1'":"'0'");
			}
			border.append("]");
			pattern.append("]");
		}
		border.append("]");
		pattern.append("]");
		StringBuffer sb = new StringBuffer(); 
        sb.append("<script language='JavaScript' type='text/javascript' src='scripts/datepatt.js'></script>");
		sb.append("<script language='JavaScript'>");
		sb.append(
			"calGenerate("+iSession.getSessionStartYear()+","+
				iSession.getStartMonth()+","+
				iSession.getEndMonth()+","+
				pattern+","+
				"['1','0'],"+
				"['Midterm exams offered','Midterm exams not offered'],"+
				"['rgb(240,240,50)','rgb(240,240,240)'],"+
				"'1',"+
				border+","+true+","+true+");");
		sb.append("</script>");
		return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void setDays(HttpServletRequest request) {
		int startMonth = iSession.getStartMonth();
		int endMonth = iSession.getEndMonth();
		iDays = new TreeSet<Integer>();
		for (int m=startMonth;m<=endMonth;m++) {
		    int yr = DateUtils.calculateActualYear(m, iSession.getSessionStartYear());
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, iSession.getSessionStartYear());
			for (int d=1;d<=daysOfMonth;d++) {
				String exam = request.getParameter("cal_val_"+yr+"_"+((12+m)%12)+"_"+d);
				if ("1".equals(exam)) {
					iDays.add(1+iSession.getDayOfYear(d, m)-getExamOffset());
				}
			}
		}
	}

	public Integer getDefaultMidtermStartOffset() {
		return iDefaultMidtermStartOffset;
	}

	public void setDefaultMidtermStartOffset(Integer defaultMidtermStartOffset) {
		iDefaultMidtermStartOffset = defaultMidtermStartOffset;
	}

	public Integer getDefaultMidtermStopOffset() {
		return iDefaultMidtermStopOffset;
	}

	public void setDefaultMidtermStopOffset(Integer defaultMidtermStopOffset) {
		iDefaultMidtermStopOffset = defaultMidtermStopOffset;
	}

	public Integer getDefaultFinalStartOffset() {
		return iDefaultFinalStartOffset;
	}

	public void setDefaultFinalStartOffset(Integer defaultFinalStartOffset) {
		iDefaultFinalStartOffset = defaultFinalStartOffset;
	}

	public Integer getDefaultFinalStopOffset() {
		return iDefaultFinalStopOffset;
	}

	public void setDefaultFinalStopOffset(Integer defaultFinalStopOffset) {
		iDefaultFinalStopOffset = defaultFinalStopOffset;
	}
}
