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

import java.util.Iterator;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentStatusType;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.dao.DepartmentStatusTypeDAO;
import org.unitime.timetable.util.IdValue;


/** 
 * @author Tomas Muller
 */
public class DeptStatusTypeEditForm extends ActionForm {
    private String iOp;
    private Long iUniqueId;
    private String iReference;
    private String iLabel;
    private int iApply = 0;
    private int iOrder = -1;
    private boolean iCanManagerView         = false;
    private boolean iCanManagerEdit         = false;
    private boolean iCanManagerLimitedEdit  = false;
    private boolean iCanOwnerView           = false;
    private boolean iCanOwnerEdit           = false;
    private boolean iCanOwnerLimitedEdit    = false;
    private boolean iCanAudit               = false;
    private boolean iCanTimetable           = false;
    private boolean iCanCommit              = false;
    

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		ActionErrors errors = new ActionErrors();
        
        if(iReference==null || iReference.trim().length()==0)
            errors.add("reference", new ActionMessage("errors.required", ""));
		else {
			try {
				DepartmentStatusType ds = DepartmentStatusType.findByRef(iReference);
				if (ds!=null && !ds.getUniqueId().equals(iUniqueId))
					errors.add("reference", new ActionMessage("errors.exists", iReference));
			} catch (Exception e) {
				errors.add("reference", new ActionMessage("errors.generic", e.getMessage()));
			}
        }
        
        if(iLabel==null || iLabel.trim().length()==0)
            errors.add("label", new ActionMessage("errors.required", ""));
        
        if (iApply<0)
            errors.add("apply", new ActionMessage("errors.required", ""));

		return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iOp = null; iUniqueId = new Long(-1);
        iReference = null; iLabel = null;
        iApply = 0; iOrder = DepartmentStatusType.findAll().size();
        iCanManagerView         = false;
        iCanManagerEdit         = false;
        iCanManagerLimitedEdit  = false;
        iCanOwnerView           = false;
        iCanOwnerEdit           = false;
        iCanOwnerLimitedEdit    = false;
        iCanAudit               = false;
        iCanTimetable           = false;
        iCanCommit              = false;
	}
    
    public void setOp(String op) { iOp = op; }
    public String getOp() { return iOp; }
    public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }
    public Long getUniqueId() { return iUniqueId; }
    public void setReference(String reference) { iReference = reference; }
    public String getReference() { return iReference; }
    public void setLabel(String label) { iLabel = label; }
    public String getLabel() { return iLabel; }
    public void setOrder(int order) { iOrder = order; }
    public int getOrder() { return iOrder; }
    public int getApply() { return iApply; }
    public void setApply(int apply) { iApply = apply; }
    public void setApply(Long apply) { iApply = (apply==null?-1:(int)apply.longValue()); }
    public Vector getApplyOptions() {
        Vector options = new Vector();
        options.add(new IdValue(new Long(DepartmentStatusType.sApplySession), "Session"));
        options.add(new IdValue(new Long(DepartmentStatusType.sApplyDepartment), "Department"));
        options.add(new IdValue(new Long(DepartmentStatusType.sApplySession | DepartmentStatusType.sApplyDepartment), "Both"));
        return options;
    }
    public void setCanManagerView(boolean canManagerView) { iCanManagerView = canManagerView; }
    public boolean getCanManagerView() { return iCanManagerView; }
    public void setCanManagerEdit(boolean canManagerEdit) { iCanManagerEdit = canManagerEdit; }
    public boolean getCanManagerEdit() { return iCanManagerEdit; }
    public void setCanManagerLimitedEdit(boolean canManagerLimitedEdit) { iCanManagerLimitedEdit = canManagerLimitedEdit; }
    public boolean getCanManagerLimitedEdit() { return iCanManagerLimitedEdit; }
    public void setCanOwnerView(boolean canOwnerView) { iCanOwnerView = canOwnerView; }
    public boolean getCanOwnerView() { return iCanOwnerView; }
    public void setCanOwnerEdit(boolean canOwnerEdit) { iCanOwnerEdit = canOwnerEdit; }
    public boolean getCanOwnerEdit() { return iCanOwnerEdit; }
    public void setCanOwnerLimitedEdit(boolean canOwnerLimitedEdit) { iCanOwnerLimitedEdit = canOwnerLimitedEdit; }
    public boolean getCanOwnerLimitedEdit() { return iCanOwnerLimitedEdit; }
    public void setCanAudit(boolean canAudit) { iCanAudit = canAudit; }
    public boolean getCanAudit() { return iCanAudit; }
    public void setCanTimetable(boolean canTimetable) { iCanTimetable = canTimetable; }
    public boolean getCanTimetable() { return iCanTimetable; }
    public void setCanCommit(boolean canCommit) { iCanCommit = canCommit; }
    public boolean getCanCommit() { return iCanCommit; }
    public int getRights() {
        int rights = 0;
        if (getCanManagerView()) rights += DepartmentStatusType.sCanManagerView;
        if (getCanManagerEdit()) rights += DepartmentStatusType.sCanManagerEdit;
        if (getCanManagerLimitedEdit()) rights += DepartmentStatusType.sCanManagerLimitedEdit;
        if (getCanOwnerView()) rights += DepartmentStatusType.sCanOwnerView;
        if (getCanOwnerEdit()) rights += DepartmentStatusType.sCanOwnerEdit;
        if (getCanOwnerLimitedEdit()) rights += DepartmentStatusType.sCanOwnerLimitedEdit;
        if (getCanAudit()) rights += DepartmentStatusType.sCanAudit;
        if (getCanTimetable()) rights += DepartmentStatusType.sCanTimetable;
        if (getCanCommit()) rights += DepartmentStatusType.sCanCommit;
        return rights;
    }
    public void setRights(int rights) {
        setCanManagerView((rights&DepartmentStatusType.sCanManagerView)==DepartmentStatusType.sCanManagerView);
        setCanManagerEdit((rights&DepartmentStatusType.sCanManagerEdit)==DepartmentStatusType.sCanManagerEdit);
        setCanManagerLimitedEdit((rights&DepartmentStatusType.sCanManagerLimitedEdit)==DepartmentStatusType.sCanManagerLimitedEdit);
        setCanOwnerView((rights&DepartmentStatusType.sCanOwnerView)==DepartmentStatusType.sCanOwnerView);
        setCanOwnerEdit((rights&DepartmentStatusType.sCanOwnerEdit)==DepartmentStatusType.sCanOwnerEdit);
        setCanOwnerLimitedEdit((rights&DepartmentStatusType.sCanOwnerLimitedEdit)==DepartmentStatusType.sCanOwnerLimitedEdit);
        setCanAudit((rights&DepartmentStatusType.sCanAudit)==DepartmentStatusType.sCanAudit);
        setCanTimetable((rights&DepartmentStatusType.sCanTimetable)==DepartmentStatusType.sCanTimetable);
        setCanCommit((rights&DepartmentStatusType.sCanCommit)==DepartmentStatusType.sCanCommit);
    }
	
	public void load(DepartmentStatusType s) {
		if (s==null) {
			reset(null, null);
			setOp("Add New");
		} else {
            setUniqueId(s.getUniqueId());
            setReference(s.getReference());
            setLabel(s.getLabel());
            setApply(s.getApply());
            setRights(s.getStatus().intValue());
            setOrder(s.getOrd());
            setOp("Update");
		}
	}
	
	public DepartmentStatusType saveOrUpdate(org.hibernate.Session hibSession) throws Exception {
        DepartmentStatusType s = null;
		if (getUniqueId().intValue()>=0)
			s = (new DepartmentStatusTypeDAO()).get(getUniqueId());
		if (s==null) 
            s = new DepartmentStatusType();
        s.setReference(getReference());
        s.setLabel(getLabel());
        s.setApply(getApply());
        if (s.getOrd()==null) s.setOrd(DepartmentStatusType.findAll().size());
        s.setStatus(getRights());
        hibSession.saveOrUpdate(s);
        setUniqueId(s.getUniqueId());
        return s;
	}
	
	public void delete(org.hibernate.Session hibSession) throws Exception {
		if (getUniqueId().intValue()<0) return;
        DepartmentStatusType s = (new DepartmentStatusTypeDAO()).get(getUniqueId());
        for (Iterator i=hibSession.createQuery(
                "select s from Session s where s.statusType.uniqueId=:id").
                setLong("id", s.getUniqueId()).iterate();i.hasNext();) {
            Session session = (Session)i.next();
            DepartmentStatusType other = null;
            for (Iterator j=DepartmentStatusType.findAll().iterator();j.hasNext();) {
                DepartmentStatusType x = (DepartmentStatusType)j.next();
                if (!x.getUniqueId().equals(s.getUniqueId()) && x.applySession()) {
                    other = x; break;
                }
            }
            if (other==null)
                throw new RuntimeException("Unable to delete session status "+getReference()+", no other session status available.");
            session.setStatusType(other);
            hibSession.saveOrUpdate(session);
        }
        for (Iterator i=hibSession.createQuery(
                "select d from Department d where d.statusType.uniqueId=:id").
                setLong("id", s.getUniqueId()).iterate();i.hasNext();) {
            Department dept = (Department)i.next();
            dept.setStatusType(null);
            hibSession.saveOrUpdate(dept);
        }        
        for (Iterator i=DepartmentStatusType.findAll().iterator();i.hasNext();) {
            DepartmentStatusType x = (DepartmentStatusType)i.next();
            if (x.getOrd()>s.getOrd()) {
                x.setOrd(x.getOrd()-1); 
                hibSession.saveOrUpdate(x);
            }
        }
        if (s!=null) hibSession.delete(s);
	}
}

