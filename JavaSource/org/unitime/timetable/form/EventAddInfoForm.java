package org.unitime.timetable.form;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.form.EventDetailForm.ContactBean;
import org.unitime.timetable.form.EventRoomAvailabilityForm.DateLocation;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseEvent;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Event;
import org.unitime.timetable.model.EventContact;
import org.unitime.timetable.model.EventNote;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.RelatedCourseInfo;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.SpecialEvent;
import org.unitime.timetable.model.comparators.InstrOfferingConfigComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.InstrOfferingConfigDAO;
import org.unitime.timetable.model.dao.SchedulingSubpartDAO;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.util.DynamicList;
import org.unitime.timetable.util.DynamicListObjectFactory;
import org.unitime.timetable.util.IdValue;

public class EventAddInfoForm extends ActionForm {

	private TreeSet<DateLocation> iDateLocations = new TreeSet();
	private int iStartTime;
	private int iStopTime;
	private String iOp;
	private String iStartTimeString;
	private String iStopTimeString;
	private String iEventName;
	private ContactBean iMainContact;
	private String iAdditionalInfo;
	private String iMainContactEmail;
	private String iMainContactPhone;
	private String iMainContactFirstName;	
	private String iMainContactLastName;
	private String iEventType;
	private List iSubjectArea;
	private Collection iSubjectAreas;
	private List iCourseNbr;
	private List iItype;
	private List iClassNumber;
	private boolean iAttendanceRequired;
	
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		
		ActionErrors errors = new ActionErrors();

		if (iEventName==null || iEventName.length()==0) {
			errors.add("eventName", new ActionMessage("errors.generic", "The event name is mandatory."));
		}

		if (iMainContactEmail==null || iMainContactEmail.length()==0) {
			errors.add("mcEmail", new ActionMessage("errors.generic", "The contact email is mandatory."));
		}

		if (iMainContactPhone==null || iMainContactPhone.length()==0) {
			errors.add("mcPhone", new ActionMessage("errors.generic", "The contact phone number is mandatory."));
		}

		if (iAdditionalInfo.length()>999) {
			errors.add("note", new ActionMessage("errors.generic", "Additional information is too long. Please, limit it to no more than 1000 characters."));
		}
		
		return errors;
	}
	
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iDateLocations.clear();
		iStartTime = 90;
		iStopTime = 210;
		iOp = null;
		iStartTimeString = null;
		iStopTimeString = null;
		iEventName = null;
		iMainContact = null;
		iAdditionalInfo = "";
		iMainContactEmail = null;
		iMainContactPhone = null;
		iMainContactFirstName = null;
		iMainContactLastName = null;
		iEventType = null;
        iSubjectArea = DynamicList.getInstance(new ArrayList(), idfactory);
        iCourseNbr = DynamicList.getInstance(new ArrayList(), idfactory);
        iItype = DynamicList.getInstance(new ArrayList(), idfactory);
        iClassNumber = DynamicList.getInstance(new ArrayList(), idfactory);
		iAttendanceRequired = false;
		load(request.getSession());
	}
	
	public void load(HttpSession session) {
		iDateLocations = (TreeSet<DateLocation>) session.getAttribute("Event.DateLocations");
		iStartTime = (Integer) session.getAttribute("Event.StartTime");
		iStopTime = (Integer) session.getAttribute("Event.StopTime");
		iEventType = (String) session.getAttribute("Event.EventType");		
		if (session.getAttribute("Event.SubjectArea")!=null) {
			iSubjectArea = (List) session.getAttribute("Event.SubjectArea");
			iSubjectAreas = (Collection) iSubjectArea;
			iCourseNbr = (List) session.getAttribute("Event.CourseNbr");
			iItype = (List) session.getAttribute("Event.SubjectItype");
			iClassNumber = (List) session.getAttribute("Event.ClassNumber");
			iAttendanceRequired = (Boolean) session.getAttribute("Event.AttendanceRequired");
		}
	}
	
	public void save(HttpSession session) {
		
	}
	
	public void submit(HttpSession session) {

		Transaction tx = null;
		try {
			Session hibSession = new _RootDAO().getSession();
			tx = hibSession.beginTransaction();

			// search database for a contact with this e-mail
			// if not in db, create a new contact
			// update information from non-empty fields
			EventContact mainContact = EventContact.findByEmail(iMainContactEmail); 
			if (mainContact==null) mainContact = new EventContact();
			if (iMainContactFirstName!=null && iMainContactFirstName.length()>0) 
				mainContact.setFirstName(iMainContactFirstName);
			if (iMainContactLastName!=null && iMainContactLastName.length()>0)
				mainContact.setLastName(iMainContactLastName);
			if (iMainContactEmail!=null && iMainContactEmail.length()>0)
				mainContact.setEmailAddress(iMainContactEmail);
			if (iMainContactPhone!=null && iMainContactPhone.length()>0)
				mainContact.setPhone(iMainContactPhone);
			hibSession.saveOrUpdate(mainContact);
			
			// create event
			Event event = null;
			if ("Course Event".equals(iEventType)) {
				event = new CourseEvent();
				((CourseEvent) event).setReqAttendance(iAttendanceRequired);
				setRelatedCourseInfos((CourseEvent)event);
			} else {
				event = new SpecialEvent();
			}
			event.setEventName(iEventName);
			event.setMainContact(mainContact);
			
			hibSession.saveOrUpdate(event);
			
			// create event meetings
			event.setMeetings(new HashSet());
			for (Iterator i=iDateLocations.iterator();i.hasNext();) {
				DateLocation dl = (DateLocation) i.next();
				Meeting m = new Meeting();
				m.setMeetingDate(dl.getDate());
				m.setStartPeriod(iStartTime);
				m.setStopPeriod(iStopTime);
				m.setLocationPermanentId(dl.getLocation());
				m.setClassCanOverride(true);
				m.setEvent(event);
				hibSession.saveOrUpdate(m); // save each meeting to db
				event.getMeetings().add(m); // link each meeting with event
			}
			hibSession.saveOrUpdate(event);
			
			// add event note (additional info)
			EventNote en = new EventNote();
			en.setEvent(event);
			en.setTextNote(iAdditionalInfo);
			hibSession.saveOrUpdate(en);
			// attach the note to event
			event.setNotes(new HashSet());
			event.getNotes().add(en);
			hibSession.saveOrUpdate(event);

			tx.commit();
		} catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		}
		
	}
	
	public int getStartTime() {return iStartTime;	}
	public int getStopTime() {return iStopTime;}
	
	public String getOp() {return iOp;}
	public void setOp(String op) {iOp = op;}
	
	public boolean getAttendanceRequired() {return iAttendanceRequired;}
	public void setAttendanceRequired(boolean save) {iAttendanceRequired = save;}
	
	public TreeSet<DateLocation> getDateLocations() {
		return iDateLocations;
	}
	
	public String getTimeString(int time) {
	    int hour = (time/12)%12;
	    if (hour==0) hour = 12;
    	int minute = time%12*5;
    	String ampm = (time/144==0?"am":"pm");
		return hour+":"+(minute<10?"0":"")+minute+" "+ampm;
	}
	
	public String getStartTimeString() {
		return getTimeString(iStartTime);
	}
	
	public String getStopTimeString() {
		return getTimeString(iStopTime);
	}
	
	public String getEventType() {return iEventType;}
	public void setEventType(String type) {iEventType = type;}

	public String getEventName() {return iEventName;}
	public void setEventName(String name) {iEventName = name;}
	
	public ContactBean getMainContact() {return iMainContact;}
	public void setMainContact (ContactBean contact) {iMainContact = contact;}	
	
	public String getMainContactEmail() {return iMainContactEmail;}
	public void setMainContactEmail(String email) {iMainContactEmail = email;}

	public String getMainContactPhone() {return iMainContactPhone;}
	public void setMainContactPhone(String phone) {iMainContactPhone = phone;}
	
	public String getAdditionalInfo() {return iAdditionalInfo;}
	public void setAdditionalInfo(String info) {iAdditionalInfo = info;}

	public String getMainContactFirstName() {return iMainContactFirstName;}
	public void setMainContactFirstName(String firstName) {iMainContactFirstName = firstName;}
	
	public String getMainContactLastName() {return iMainContactLastName;}
	public void setMainContactLastName(String lastName) {iMainContactLastName = lastName;}

	public void cleanSessionAttributes(HttpSession session) {
		session.removeAttribute("Event.DateLocations");
		session.removeAttribute("Event.StartTime");
		session.removeAttribute("Event.StopTime");
		session.removeAttribute("Event.MeetingDates");
		session.removeAttribute("Event.MinCapacity");
		session.removeAttribute("Event.MaxCapacity");
		session.removeAttribute("Event.BuildingId");
		session.removeAttribute("Event.RoomNumber");
		session.removeAttribute("Event.SessionId");
		session.removeAttribute("back");
		session.removeAttribute("Event.LookAtNearLocations");
		session.removeAttribute("Event.SubjectArea");
		session.removeAttribute("Event.CourseNbr");
		session.removeAttribute("Event.SubjectItype");
		session.removeAttribute("Event.ClassNumber");
	}

    protected DynamicListObjectFactory idfactory = new DynamicListObjectFactory() {
        public Object create() {
            return new Long(-1);
        }
    };
	
    public Collection getSubjectAreas() { return iSubjectAreas; }
    public void setSubjectAreas(Collection subjectAreas) { this.iSubjectAreas = subjectAreas; }
    public List getSubjectAreaList() { return iSubjectArea; }
    public List getSubjectArea() { return iSubjectArea; }
    public Long getSubjectArea(int key) { return (Long)iSubjectArea.get(key); }
    public void setSubjectArea(int key, Long value) { this.iSubjectArea.set(key, value); }
    public void setSubjectArea(List subjectArea) { this.iSubjectArea = subjectArea; }
    public List getCourseNbr() { return iCourseNbr; }
    public Long getCourseNbr(int key) { return (Long)iCourseNbr.get(key); }
    public void setCourseNbr(int key, Long value) { this.iCourseNbr.set(key, value); }
    public void setCourseNbr(List courseNbr) { this.iCourseNbr = courseNbr; }
    public List getItype() { return iItype; }
    public Long getItype(int key) { return (Long)iItype.get(key); }
    public void setItype(int key, Long value) { this.iItype.set(key, value); }
    public void setItype(List itype) { this.iItype = itype; }
    public List getClassNumber() { return iClassNumber; }
    public Long getClassNumber(int key) { return (Long)iClassNumber.get(key); }
    public void setClassNumber(int key, Long value) { this.iClassNumber.set(key, value); }
    public void setClassNumber(List classNumber) { this.iClassNumber = classNumber; }
    
    
    public Collection getCourseNbrs(int idx) { 
        Vector ret = new Vector();
        boolean contains = false;
        if (getSubjectArea(idx)>=0) {
            for (Iterator i= new CourseOfferingDAO().
                    getSession().
                    createQuery("select co.uniqueId, co.courseNbr from CourseOffering co "+
                            "where co.uniqueCourseNbr.subjectArea.uniqueId = :subjectAreaId "+
                            "and co.instructionalOffering.notOffered = false "+
                            "order by co.courseNbr ").
                    setFetchSize(200).
                    setCacheable(true).
                    setLong("subjectAreaId", getSubjectArea(idx)).iterate();i.hasNext();) {
                Object[] o = (Object[])i.next();
                ret.add(new IdValue((Long)o[0],(String)o[1]));
                if (o[0].equals(getCourseNbr(idx))) contains = true;
            }
        }
        if (!contains) setCourseNbr(idx, -1L);
        if (ret.size()==1) setCourseNbr(idx, ((IdValue)ret.firstElement()).getId());
        else ret.insertElementAt(new IdValue(-1L,"-"), 0);
        return ret;
    }
    
    public Collection getItypes(int idx) { 
        Vector ret = new Vector();
        boolean contains = false;
        if (getCourseNbr(idx)>=0) {
            CourseOffering course = new CourseOfferingDAO().get(getCourseNbr(idx));
            if (course.isIsControl())
                ret.add(new IdValue(Long.MIN_VALUE+1,"Offering"));
            ret.add(new IdValue(Long.MIN_VALUE,"Course"));
            if (!course.isIsControl()) {
                setItype(idx, Long.MIN_VALUE);
                return ret;
            }
            TreeSet configs = new TreeSet(new InstrOfferingConfigComparator(null));
            configs.addAll(new InstrOfferingConfigDAO().
                getSession().
                createQuery("select distinct c from " +
                        "InstrOfferingConfig c inner join c.instructionalOffering.courseOfferings co "+
                        "where co.uniqueId = :courseOfferingId").
                setFetchSize(200).
                setCacheable(true).
                setLong("courseOfferingId", course.getUniqueId()).
                list());
            if (!configs.isEmpty()) {
                ret.add(new IdValue(Long.MIN_VALUE+2,"-- Configurations --"));
                for (Iterator i=configs.iterator();i.hasNext();) {
                    InstrOfferingConfig c = (InstrOfferingConfig)i.next();
                    if (c.getUniqueId().equals(getItype(idx))) contains = true;
                    ret.add(new IdValue(-c.getUniqueId(), c.getName()));
                }
            }
            TreeSet subparts = new TreeSet(new SchedulingSubpartComparator(null));
            subparts.addAll(new SchedulingSubpartDAO().
                getSession().
                createQuery("select distinct s from " +
                        "SchedulingSubpart s inner join s.instrOfferingConfig.instructionalOffering.courseOfferings co "+
                        "where co.uniqueId = :courseOfferingId").
                setFetchSize(200).
                setCacheable(true).
                setLong("courseOfferingId", course.getUniqueId()).
                list());
            if (!configs.isEmpty() && !subparts.isEmpty())
                ret.add(new IdValue(Long.MIN_VALUE+2,"-- Subparts --"));
            for (Iterator i=subparts.iterator();i.hasNext();) {
                SchedulingSubpart s = (SchedulingSubpart)i.next();
                Long sid = s.getUniqueId();
                String name = s.getItype().getAbbv();
                String sufix = s.getSchedulingSubpartSuffix();
                while (s.getParentSubpart()!=null) {
                    name = "&nbsp;&nbsp;&nbsp;&nbsp;"+name;
                    s = s.getParentSubpart();
                }
                if (s.getInstrOfferingConfig().getInstructionalOffering().getInstrOfferingConfigs().size()>1)
                    name += " ["+s.getInstrOfferingConfig().getName()+"]";
                if (sid.equals(getItype(idx))) contains = true;
                ret.add(new IdValue(sid, name+(sufix==null || sufix.length()==0?"":" ("+sufix+")")));
            }
        } else {
            ret.addElement(new IdValue(0L,"N/A"));
        }
        if (!contains) setItype(idx, ((IdValue)ret.firstElement()).getId());
        return ret;
    }
	
	
    public RelatedCourseInfo getRelatedCourseInfo(int idx) {
        if (getSubjectArea(idx)<0 || getCourseNbr(idx)<0) return null;
        CourseOffering course = new CourseOfferingDAO().get(getCourseNbr(idx));
        if (course==null) return null;
        if (getItype(idx)==Long.MIN_VALUE) { //course
            RelatedCourseInfo owner = new RelatedCourseInfo();
            owner.setOwner(course);
            return owner;
        } else if (getItype(idx)==Long.MIN_VALUE+1 || getItype(idx)==Long.MIN_VALUE+2) { //offering
            RelatedCourseInfo owner = new RelatedCourseInfo();
            owner.setOwner(course.getInstructionalOffering());
            return owner;
        } else if (getItype(idx)<0) { //config
            InstrOfferingConfig config = new InstrOfferingConfigDAO().get(-getItype(idx));
            if (config==null) return null;
            RelatedCourseInfo owner = new RelatedCourseInfo();
            owner.setOwner(config);
            return owner;
        } else if (getClassNumber(idx)>=0) { //class
            Class_ clazz = new Class_DAO().get(getClassNumber(idx));
            if (clazz==null) return null;
            RelatedCourseInfo owner = new RelatedCourseInfo();
            owner.setOwner(clazz);
            return owner;
        }
        return null;
    }
    
    public void setRelatedCourseInfos(CourseEvent event) {
        if (event.getRelatedCourses()==null) event.setRelatedCourses(new HashSet());
        event.getRelatedCourses().clear();
        for (int idx=0;idx<getSubjectArea().size();idx++) {
            RelatedCourseInfo course = getRelatedCourseInfo(idx);
            if (course!=null) {
                event.getRelatedCourses().add(course);
                course.setEvent(event);
            }
        }
    }

    public String getRelatedCoursesTable() {
        WebTable table = new WebTable(3, null, new String[] {"Object", "Type", "Title"}, new String[] {"left", "left", "left"}, new boolean[] {true, true, true});
        for (int idx=0;idx<iSubjectArea.size();idx++) {
            RelatedCourseInfo rci = getRelatedCourseInfo(idx);
            if (rci==null) continue;
            String onclick = null, name = null, type = null, students = String.valueOf(rci.countStudents()), limit = /*String.valueOf(rci.getLimit())*/ "no limit", manager = null, assignment = null, title = null;
            switch (rci.getOwnerType()) {
                case ExamOwner.sOwnerTypeClass :
                    Class_ clazz = (Class_)rci.getOwnerObject();
            //        if (clazz.isViewableBy(user))
            //            onclick = "onClick=\"document.location='classDetail.do?cid="+clazz.getUniqueId()+"';\"";
                    name = rci.getLabel();//clazz.getClassLabel();
                    type = "Class";
                    manager = clazz.getManagingDept().getShortLabel();
                    if (clazz.getCommittedAssignment()!=null)
                        assignment = clazz.getCommittedAssignment().getPlacement().getLongName();
                    title = clazz.getSchedulePrintNote();
                    if (title==null || title.length()==0) title=clazz.getSchedulingSubpart().getControllingCourseOffering().getTitle();
                    break;
                case ExamOwner.sOwnerTypeConfig :
                    InstrOfferingConfig config = (InstrOfferingConfig)rci.getOwnerObject();
            //        if (config.isViewableBy(user))
            //            onclick = "onClick=\"document.location='instructionalOfferingDetail.do?io="+config.getInstructionalOffering().getUniqueId()+"';\"";;
                    name = rci.getLabel();//config.getCourseName()+" ["+config.getName()+"]";
                    type = "Configuration";
                    manager = config.getInstructionalOffering().getControllingCourseOffering().getDepartment().getShortLabel();
                    title = config.getControllingCourseOffering().getTitle();
                    break;
                case ExamOwner.sOwnerTypeOffering :
                    InstructionalOffering offering = (InstructionalOffering)rci.getOwnerObject();
            //        if (offering.isViewableBy(user))
            //            onclick = "onClick=\"document.location='instructionalOfferingDetail.do?io="+offering.getUniqueId()+"';\"";;
                    name = rci.getLabel();//offering.getCourseName();
                    type = "Offering";
                    manager = offering.getControllingCourseOffering().getDepartment().getShortLabel();
                    title = offering.getControllingCourseOffering().getTitle();
                    break;
                case ExamOwner.sOwnerTypeCourse :
                    CourseOffering course = (CourseOffering)rci.getOwnerObject();
             //       if (course.isViewableBy(user))
             //           onclick = "onClick=\"document.location='instructionalOfferingDetail.do?io="+course.getInstructionalOffering().getUniqueId()+"';\"";;
                    name = rci.getLabel();//course.getCourseName();
                    type = "Course";
                    manager = course.getDepartment().getShortLabel();
                    title = course.getTitle();
                    break;
                        
            }
            table.addLine(onclick, new String[] { name, type, title}, null);
        }
        return (table.getLines().isEmpty()?"":table.printTable());
    }
    
}