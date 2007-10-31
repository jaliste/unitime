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
package org.unitime.timetable.action.ajax;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.DistributionPref;
import org.unitime.timetable.model.DistributionType;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.Class_DAO;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.DistributionTypeDAO;
import org.unitime.timetable.model.dao.SchedulingSubpartDAO;

/**
 * 
 * @author Tomas Muller
 *
 */
public class DistributionPrefsAjax extends Action {
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        response.addHeader("Content-Type", "text/xml");
        
        //System.out.println("type:"+request.getParameter("type")); 
        //System.out.println("id:  "+request.getParameter("id"));
        
        ServletOutputStream out = response.getOutputStream();
        
        //System.out.println("response:");
        out.print("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n");
        out.print("<results>");
        coumputeSuggestionList(request, out);
        out.print("</results>");
        
        return null;        

    }
    
    protected void print(ServletOutputStream out, String id, String value) throws IOException {
        //System.out.println("  <result id=\""+id+"\" value=\""+value+"\" />");
        out.print("<result id=\""+id+"\" value=\""+value+"\" />");
    }
    
    protected void print(ServletOutputStream out, String id, String value, String extra) throws IOException {
        //System.out.println("  <result id=\""+id+"\" value=\""+value+"\" extra=\""+extra+"\" />");
        out.print("<result id=\""+id+"\" value=\""+value+"\" extra=\""+extra+"\" />");
    }

    protected void coumputeSuggestionList(HttpServletRequest request, ServletOutputStream out) throws Exception {
        if ("subjectArea".equals(request.getParameter("type"))) {
            coumputeCourseNumbers(request.getParameter("id"),out);
        } else if ("courseNbr".equals(request.getParameter("type"))) {
            coumputeSubparts(request.getParameter("id"),out);
        } else if ("itype".equals(request.getParameter("type"))) {
            coumputeClasses(request.getParameter("id"),out);
        } else if ("grouping".equals(request.getParameter("type"))) {
            coumputeGroupingDesc(request.getParameter("id"),out);
        } else if ("distType".equals(request.getParameter("type"))) {
            computePreferenceLevels(request.getParameter("id"),out);
        }
    }
    
    protected void coumputeGroupingDesc(String groupingId, ServletOutputStream out) throws Exception {
        try {
            for (int i=0;i<DistributionPref.sGroupings.length;i++)
                if (DistributionPref.sGroupings[i].equals(groupingId))
                    print(out, "desc", DistributionPref.getGroupingDescription(i).replaceAll("<", "@lt@").replaceAll(">", "@gt@").replaceAll("\"","@quot@").replaceAll("&","@amp@"));
        } catch (Exception e) {
            print(out, "desc", "");
        }
    }
    
    protected void computePreferenceLevels(String distTypeId, ServletOutputStream out) throws Exception {
        if (distTypeId==null || distTypeId.length()==0 || distTypeId.equals(Preference.BLANK_PREF_VALUE)) return;
        DistributionType dist = new DistributionTypeDAO().get(Long.valueOf(distTypeId));
        print(out, "desc", dist.getDescr().replaceAll("<", "@lt@").replaceAll(">", "@gt@").replaceAll("\"","@quot@").replaceAll("&","@amp@"));
        for (Enumeration e=PreferenceLevel.getPreferenceLevelList(false).elements();e.hasMoreElements();) {
            PreferenceLevel pref = (PreferenceLevel)e.nextElement();
            if (dist.isAllowed(pref))
                print(out, pref.getPrefId().toString(), pref.getPrefName(), pref.prefcolor());
        }
    }
    
    
    protected void coumputeCourseNumbers(String subjectAreaId, ServletOutputStream out) throws Exception {
        if (subjectAreaId==null || subjectAreaId.length()==0 || subjectAreaId.equals(Preference.BLANK_PREF_VALUE)) return;
        List courseNumbers = new CourseOfferingDAO().
            getSession().
            createQuery("select co.uniqueId, co.courseNbr from CourseOffering co "+
                    "where co.uniqueCourseNbr.subjectArea.uniqueId = :subjectAreaId "+
                    "and co.instructionalOffering.notOffered = false and co.isControl = true " +
                    "order by co.courseNbr ").
            setFetchSize(200).
            setCacheable(true).
            setLong("subjectAreaId", Long.parseLong(subjectAreaId)).
            list();
        for (Iterator i=courseNumbers.iterator();i.hasNext();) {
            Object[] o = (Object[])i.next();
            print(out, o[0].toString(), o[1].toString());
        }
    }
    
    protected void coumputeSubparts(String courseOfferingId, ServletOutputStream out) throws Exception {
        if (courseOfferingId==null || courseOfferingId.length()==0 || courseOfferingId.equals(Preference.BLANK_PREF_VALUE)) return;
        TreeSet subparts = new TreeSet(new SchedulingSubpartComparator(null));
        subparts.addAll(new SchedulingSubpartDAO().
            getSession().
            createQuery("select distinct s from " +
                    "SchedulingSubpart s inner join s.instrOfferingConfig.instructionalOffering.courseOfferings co "+
                    "where co.uniqueId = :courseOfferingId").
            setFetchSize(200).
            setCacheable(true).
            setLong("courseOfferingId", Long.parseLong(courseOfferingId)).
            list());
        for (Iterator i=subparts.iterator();i.hasNext();) {
            SchedulingSubpart s = (SchedulingSubpart)i.next();
            String id = s.getUniqueId().toString();
            String name = s.getItype().getAbbv();
            String sufix = s.getSchedulingSubpartSuffix();
            while (s.getParentSubpart()!=null) {
                name = "_"+name;
                s = s.getParentSubpart();
            }
            if (s.getInstrOfferingConfig().getInstructionalOffering().getInstrOfferingConfigs().size()>1)
                name += " ["+s.getInstrOfferingConfig().getName()+"]";
            print(out, id, name+(sufix==null || sufix.length()==0?"":" ("+sufix+")"));
        }
    }
    
    protected void coumputeClasses(String schedulingSubpartId, ServletOutputStream out) throws Exception {
        if (schedulingSubpartId==null || schedulingSubpartId.length()==0 || schedulingSubpartId.equals(Preference.BLANK_PREF_VALUE)) return;
        TreeSet classes = new TreeSet(new ClassComparator(ClassComparator.COMPARE_BY_HIERARCHY));
        classes.addAll(new Class_DAO().
            getSession().
            createQuery("select distinct c from Class_ c "+
                    "where c.schedulingSubpart.uniqueId=:schedulingSubpartId").
            setFetchSize(200).
            setCacheable(true).
            setLong("schedulingSubpartId", Long.parseLong(schedulingSubpartId)).
            list());
        print(out, "-1", "All");
        for (Iterator i=classes.iterator();i.hasNext();) {
            Class_ c = (Class_)i.next();
            print(out, c.getUniqueId().toString(), c.getSectionNumberString()); 
        }
    }
   

}
