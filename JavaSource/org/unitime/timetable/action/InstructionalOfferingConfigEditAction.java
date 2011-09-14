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
package org.unitime.timetable.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;
import org.hibernate.Transaction;
import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.InstructionalOfferingConfigEditForm;
import org.unitime.timetable.interfaces.ExternalInstrOffrConfigChangeAction;
import org.unitime.timetable.interfaces.ExternalLinkLookup;
import org.unitime.timetable.model.BuildingPref;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.Event;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.ItypeDesc;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.RoomFeaturePref;
import org.unitime.timetable.model.RoomGroup;
import org.unitime.timetable.model.RoomGroupPref;
import org.unitime.timetable.model.RoomPref;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SimpleItypeConfig;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.model.TimePref;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.comparators.SicComparator;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.InstrOfferingConfigDAO;
import org.unitime.timetable.model.dao.InstructionalOfferingDAO;
import org.unitime.timetable.model.dao.ItypeDescDAO;
import org.unitime.timetable.model.dao.TimetableManagerDAO;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.LookupTables;
import org.unitime.timetable.webutil.SchedulingSubpartTableBuilder;


/**
 * MyEclipse Struts
 * Creation date: 05-19-2005
 *
 * XDoclet definition:
 * @struts:action path="/instructionalOfferingConfigEdit" name="InstructionalOfferingConfigEditForm" input="/instructionalOfferingConfigEdit.jsp" scope="request"
 */
public class InstructionalOfferingConfigEditAction extends Action {
	
	protected final static CourseMessages MSG = Localization.create(CourseMessages.class);

    // --------------------------------------------------------- Instance Variables

    // --------------------------------------------------------- Methods

    /**
     * Method execute
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return ActionForward
     */
    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        if(!Web.isLoggedIn( request.getSession() )) {
            throw new Exception (MSG.errorAccessDenied());
        }

        HttpSession httpSession = request.getSession();
        MessageResources rsc = getResources(request);
        User user = Web.getUser(request.getSession());
        InstructionalOfferingConfigEditForm frm = (InstructionalOfferingConfigEditForm) form;

        String html = "";
        String op = (request.getParameter("op")==null)
						? (frm.getOp()==null || frm.getOp().length()==0)
						        ? (request.getAttribute("op")==null)
						                ? null
						                : request.getAttribute("op").toString()
						        : frm.getOp()
						: request.getParameter("op");

        if(op==null)
            op = request.getParameter("hdnOp");

        if(op==null || op.trim().length()==0)
            throw new Exception (MSG.errorOperationNotInterpreted() + op);

        // Set up itypes and subparts
        frm.setOp(op);
        LookupTables.setupItypes(request,true);
        LookupTables.setupExternalDepts(request, (Long)user.getAttribute(Constants.SESSION_ID_ATTR_NAME));
        TimetableManager tm = TimetableManager.getManager(user);
		if (!user.isAdmin()) {
			TreeSet ts = new TreeSet();
			for (Iterator it = ((TreeSet) request.getAttribute(Department.EXTERNAL_DEPT_ATTR_NAME)).iterator(); it.hasNext();){
				Department d = (Department) it.next();
				if (tm.isExternalManager()) {
					if (tm.getDepartments().contains(d))
						ts.add(d);
				} else {
					if (d.effectiveStatusType().canOwnerEdit() || tm.getDepartments().contains(d)) {
						ts.add(d);
					}
				}
			}
			request.setAttribute((Department.EXTERNAL_DEPT_ATTR_NAME), ts);
		}
        request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);

        // Clear previous error markers
        search(httpSession, "-1111", new Vector(), true);

        // First access to screen
        if(op.equalsIgnoreCase(rsc.getMessage("op.edit"))
                || op.equalsIgnoreCase(rsc.getMessage("button.modifyConfig"))
                || op.equalsIgnoreCase(rsc.getMessage("button.duplicateConfig")) ) {
            Long configId = null;

            try {
                configId = new Long(request.getParameter("configId"));
            }
            catch (Exception e) {
                throw new Exception (MSG.errorConfigIDNotValid() + request.getParameter("configId"));
            }

            loadDetailFromConfig(frm, configId, false);

            // load existing config from database
            Vector sp = loadOriginalConfig(user, frm.getConfigId(), frm);
            boolean createAsNew = false;
            if(op.equalsIgnoreCase(rsc.getMessage("button.duplicateConfig")))
                createAsNew = true;

            if(sp!=null && sp.size()>0) {
	            httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, sp);
	            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), configId.toString(), createAsNew, frm.getUnlimited().booleanValue());
	            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
            } else {
	            httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, null);
	            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, null);
            }

            // For duplication set configID to 0
            if(op.equalsIgnoreCase(rsc.getMessage("button.duplicateConfig"))) {
                frm.setConfigId(new Long(0));
                frm.setName(InstrOfferingConfig.getGeneratedName(
                        ( new InstrOfferingConfigDAO().get(configId)).getInstructionalOffering() ));
            }
        }

        // Add a new configuration
		if(op.equals(rsc.getMessage("button.addConfig"))) {
    		String courseOfferingId = (request.getParameter("uid")==null)
			? (request.getAttribute("uid")==null)
			        ? null
			        : request.getAttribute("uid").toString()
			: request.getParameter("uid");


			if(courseOfferingId==null || courseOfferingId.trim().length()==0)
			    throw new Exception ("Course Offering Id need for operation. ");

            loadDetailFromCourseOffering(frm, new Long(courseOfferingId), true, false);
            httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, null);
            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, "");

		}

        // Redirect after making course offered
        if (op.equalsIgnoreCase(rsc.getMessage("button.makeOffered")) ) {

    		String courseOfferingId = (request.getParameter("uid")==null)
						? (request.getAttribute("uid")==null)
						        ? null
						        : request.getAttribute("uid").toString()
						: request.getParameter("uid");


	        if(courseOfferingId==null || courseOfferingId.trim().length()==0)
	            throw new Exception ("Course Offering Id need for operation. ");

            // Get first available config
            loadDetailFromCourseOffering(frm, new Long(courseOfferingId), true, true);
            httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, null);
            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, "");

            // load existing config from database
            if (frm.getConfigId()!=null && frm.getConfigId().intValue()>0) {
	            Vector sp = loadOriginalConfig(user, frm.getConfigId(), frm);
	            if(sp!=null && sp.size()>0) {
		            httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, sp);
		            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), courseOfferingId, false, frm.getUnlimited().booleanValue());
		            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
	            } else {
		            httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, null);
		            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, null);
	            }
            }
        }

        // Add Instructional Type
        if(op.equals(MSG.actionAddInstructionalTypeToConfig())) {

            ActionMessages errors = frm.validate(mapping, request);
            if(!errors.isEmpty()) {
                html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
                request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
                saveErrors(request, errors);
                return mapping.findForward("displayForm");
            }

            addInstructionalType(httpSession, frm);
            frm.setItype(Constants.BLANK_OPTION_VALUE);

            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
        }

        // Move / Order / Delete Itypes
        if(op.indexOf(rsc.getMessage("op.shift"))>=0
                || op.equals(rsc.getMessage("op.delete")) ) {

            int limit = 0;

            try {
                limit = Integer.parseInt(request.getParameter("limit"));
            }
            catch (Exception e) {
                limit = 0;
            }

            frm.setLimit(limit);

            processShiftOrDelete(httpSession, request.getParameter("id"), op);

            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
        }

        // Multiple Limits
        if (op.equalsIgnoreCase("multipleLimits")) {

            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
        }

        // User commits changes
        if(op.equals(MSG.actionSaveConfiguration())
                || op.equals(MSG.actionUpdateConfiguration()) ) {

            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
            ActionMessages errors = frm.validate(mapping, request);
            if(!errors.isEmpty()) {
                html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
                request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
                saveErrors(request, errors);
                return mapping.findForward("displayForm");
            }

            try {
                updateConfig(request, frm);

	            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
	            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);

	            // Redirect to instr offering detail on success
	            request.setAttribute("io", frm.getInstrOfferingId());
	            request.setAttribute("op", "view");
	            return mapping.findForward("instructionalOfferingDetail");
            }
            catch (Exception e) {
                html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
                request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
                errors.add(
                        "subparts",
                        new ActionMessage(
                                "errors.generic",
                                "Configuration could not be updated. If possible, split your configuration change into 2 or more separate operations. "
                                + (e.getClass().getName().startsWith("org.hibernate.") ? "" : "Exception: " + e.getMessage()) ));
                saveErrors(request, errors);
                return mapping.findForward("displayForm");
            }
        }

        // Delete configuration
		if(op.equals(MSG.actionDeleteConfiguration())) {
            deleteConfig(request, frm);

            // Redirect to instr offering detail on success
            request.setAttribute("io", frm.getInstrOfferingId());
            request.setAttribute("cfgDelete", "1");
            request.setAttribute("op", "view");
            return mapping.findForward("instructionalOfferingDetail");
		}

        // User clicks Unlimited Enrollment
		if(op.equalsIgnoreCase("unlimitedEnrollment")) {
            html = SchedulingSubpartTableBuilder.buildSubpartsTable(request, frm.getLimit(), frm.getCourseOfferingId(), false, frm.getUnlimited().booleanValue());
            request.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, html);
            return mapping.findForward("displayForm");
		}

        return mapping.findForward("displayForm");
    }


    private void loadDetailFromConfig(
            InstructionalOfferingConfigEditForm frm,
            Long configId,
            boolean init ) throws Exception {

        // Check uniqueid
        if(configId==null)
            throw new Exception ("Config Id need for operation. ");

        InstrOfferingConfigDAO iocDao = new InstrOfferingConfigDAO();
        InstrOfferingConfig ioc = iocDao.get(configId);

        if(ioc==null)
            throw new Exception ("Congifuration not found for id: " + configId);

	    frm.setConfigId(configId);
	    frm.setName(ioc.getName());
        frm.setUnlimited(ioc.isUnlimitedEnrollment());

        Long courseOfferingId = ioc.getControllingCourseOffering().getUniqueId();
        loadDetailFromCourseOffering(frm, courseOfferingId, init, false);
    }

    /**
     * Loads course offering details into the form
     * @param courseOfferingId Course Offering Uid
     * @param frm Form
     * @throws Exception
     */
    private void loadDetailFromCourseOffering(
            InstructionalOfferingConfigEditForm frm,
            Long courseOfferingId,
            boolean init,
            boolean loadDefaultConfig ) throws Exception {

        // Check uniqueid
        if(courseOfferingId==null)
            throw new Exception ("Course Offering Id need for operation. ");

        // Load Course Offering
        CourseOfferingDAO coDao = new CourseOfferingDAO();
        CourseOffering co = coDao.get(courseOfferingId);

        if(co==null)
            throw new Exception ("Course Offering not found for id: " + courseOfferingId);

	    InstructionalOffering io = co.getInstructionalOffering();

	    // Set values
        frm.setCourseOfferingId(co.getUniqueId().toString());
        frm.setSubjectArea(co.getSubjectAreaAbbv());
        frm.setCourseNumber(co.getCourseNbr());
        frm.setInstrOfferingName(co.getCourseNameWithTitle());
        frm.setInstrOfferingId(io.getUniqueId().toString());
        frm.setNotOffered(io.isNotOffered());

	    Set configs = io.getInstrOfferingConfigs();
	    frm.setConfigCount(new Integer (configs.size()));

	    // Catalog Link
        String linkLookupClass = ApplicationProperties.getProperty("tmtbl.catalogLink.lookup.class");
        if (linkLookupClass!=null && linkLookupClass.trim().length()>0) {
        	ExternalLinkLookup lookup = (ExternalLinkLookup) (Class.forName(linkLookupClass).newInstance());
       		Map results = lookup.getLink(io);
            if (results==null)
                throw new Exception (lookup.getErrorMessage());
            
            frm.setCatalogLinkLabel((String)results.get(ExternalLinkLookup.LINK_LABEL));
            frm.setCatalogLinkLocation((String)results.get(ExternalLinkLookup.LINK_LOCATION));
        }

        if (loadDefaultConfig) {
    	    if (configs==null || configs.size()==0) {
    	        frm.setConfigId(null);
                frm.setName("1");
    	    }
    	    else {
    	        InstrOfferingConfig ioc = (InstrOfferingConfig) configs.iterator().next();
    	        frm.setConfigId(ioc.getUniqueId());

    	        if(init) {
    		        frm.setName(ioc.getName());
    		        frm.setUnlimited(ioc.isUnlimitedEnrollment());
    	        }
    	    }
        }
        else {
            if (frm.getName()==null || frm.getName().trim().length()==0)
                frm.setName(InstrOfferingConfig.getGeneratedName(io));
        }
    }


    /**
     * Loads original config from database
     * @param user User object
     * @param courseOfferingId Course Offering Uid
     * @param frm Form
     */
    private Vector loadOriginalConfig(
            User user,
            Long configId,
            InstructionalOfferingConfigEditForm frm)
    	throws Exception {

        InstrOfferingConfigDAO cfgDao = new InstrOfferingConfigDAO();
        InstrOfferingConfig config = cfgDao.get(configId);
        frm.setLimit(config.getLimit().intValue());
        Vector sp = config.toSimpleItypeConfig(user);

        if(sp!=null && sp.size()>0)
            Collections.sort(sp, new SicComparator());

        return sp;
    }


    /**
     * Add a new Inst. Type to the user defined config
     * @param httpSession Http Session object
     * @param frm Form object
     * @throws Exception
     */
    private void addInstructionalType(
            HttpSession httpSession,
            InstructionalOfferingConfigEditForm frm) throws Exception {

        // Create object
        ItypeDescDAO itypeDao = new ItypeDescDAO();
        ItypeDesc itype = itypeDao.get(new Integer(frm.getItype()));
        if(itype==null)
            throw new Exception ("Instructional Type not found");

        // Retrieve object containing user defined config from session
        Vector sp = (Vector) httpSession.getAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME);
        if(sp==null)
            sp = new Vector();

        // Create new object
        SimpleItypeConfig sic = new SimpleItypeConfig(itype);
        sic.setSubpartId(-1L);
        //sic.setLimitPerClass(frm.getLimit());
        //sic.setRoomCapacity(frm.getLimit());
        sp.addElement(sic);

        //Collections.sort(sp, new SicComparator());

        // Store back in session
        httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, sp);
    }

    /**
     * Processes arrow click and config element delete operations
     * @param httpSession Http Session object
     * @param id Id of the config element whose shift / delete arrow was clicked
     * @param op Operation to be performed
     * @throws Exception
     */
    private void processShiftOrDelete(
            HttpSession httpSession,
            String id, String op) throws Exception {

        // Read user defined config
        Vector sp = (Vector) httpSession.getAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME);

        // No subparts
        if(sp==null || sp.size()==0)
            throw new Exception ("Could not retrieve user defined configs");

        // Locate config element
        Vector indx = new Vector();
        SimpleItypeConfig result = search(httpSession, id, indx, false);
        if(result==null)
            throw new Exception ("Could not retrieve config element: " + id);

        int index = Integer.parseInt(indx.elementAt(0).toString());

        // Process ops
        if(op.equalsIgnoreCase("shiftUp"))  {
            // Get parent
           SimpleItypeConfig parent = result.getParent();

          	// Element is at the top most level
           if(parent==null) {
               // Switch elements with one above
	            int indx1 = index;
	            int indx2 = indx1-1;
	            Object tmp = sp.elementAt(indx1);
	            sp.insertElementAt(tmp, indx2);
	            sp.removeElementAt(indx1+1);
            }

         	// Element is a subpart of another element
           else {
               // Locate the element index in the subparts
               Vector v = parent.getSubparts();
               for(int i=0; i<v.size(); i++) {
                   SimpleItypeConfig subp = (SimpleItypeConfig) v.elementAt(i);

                   // Switch elements with one above
                   if(subp.getId()==Long.parseLong(id)) {
                       Object tmp = v.elementAt(i);
                       v.insertElementAt(tmp, i-1);
                       v.removeElementAt(i+1);
                       break;
                   }
               }
            }
        }

        if(op.equalsIgnoreCase("shiftDown"))  {
            // Get parent
           SimpleItypeConfig parent = result.getParent();

          	// Element is at the top most level
           if(parent==null) {
               // Switch elements with one below
	            int indx1 = index+1;
	            int indx2 = index;
	            Object tmp = sp.elementAt(indx1);
	            sp.insertElementAt(tmp, indx2);
	            sp.removeElementAt(indx1+1);
            }

          	// Element is a subpart of another element
           else {
               // Locate the element index in the subparts
               Vector v = parent.getSubparts();
               for(int i=0; i<v.size(); i++) {
                   SimpleItypeConfig subp = (SimpleItypeConfig) v.elementAt(i);

                   // Switch elements with one below
                   if(subp.getId()==Long.parseLong(id)) {
                       Object tmp = v.elementAt(i+1);
                       v.insertElementAt(tmp, i);
                       v.removeElementAt(i+2);
                       break;
                   }
               }
            }
        }

        if(op.equalsIgnoreCase("shiftLeft"))  {
            // Get parent
            SimpleItypeConfig parent = result.getParent();

            // Remove element from parent subpart
            Vector v = parent.getSubparts();
            for(int i=0; i<v.size(); i++) {
                SimpleItypeConfig subp = (SimpleItypeConfig) v.elementAt(i);
                if(subp.getId()==Long.parseLong(id)) {
                    v.removeElementAt(i);
                    break;
                }
            }

            // Get grandparent and set it as parent of element
            SimpleItypeConfig grandParent = parent.getParent();
            result.setParent(grandParent);

            // Parent is at the top most level
            if(grandParent==null) {
                sp.insertElementAt(result, index+1);
            }

            // Grandparent exists
            else {
                // Locate parent in grandparent subparts
                Vector v1 = grandParent.getSubparts();
                for(int i=0; i<v1.size(); i++) {
                    SimpleItypeConfig subp = (SimpleItypeConfig) v1.elementAt(i);
                    // Add element just after parent subpart
                    if(subp.getId()==parent.getId()) {
                        v1.insertElementAt(result, i+1);
                        break;
                    }
                }
            }
        }

        if(op.equalsIgnoreCase("shiftRight"))  {
            // Get parent
            SimpleItypeConfig parent = result.getParent();

           	// Element is at the top most level
           	if(parent==null) {
           	    // Switch elements with one below
	            SimpleItypeConfig curr = (SimpleItypeConfig) sp.elementAt(index);
	            SimpleItypeConfig prev = (SimpleItypeConfig) sp.elementAt(index-1);
	            prev.addSubpart(curr);
                sp.removeElementAt(index);
            }

           	// Element is a subpart of another element
            else {
                // Locate the element index in the subparts
                Vector v = parent.getSubparts();
                for(int i=0; i<v.size(); i++) {
                    SimpleItypeConfig subp = (SimpleItypeConfig) v.elementAt(i);

                    // Add the element to the subpart of the element above
                    if(subp.getId()==Long.parseLong(id)) {
        	            SimpleItypeConfig curr = (SimpleItypeConfig) v.elementAt(i);
        	            SimpleItypeConfig prev = (SimpleItypeConfig) v.elementAt(i-1);
        	            prev.addSubpart(curr);
                        v.removeElementAt(i);
                        break;
                    }
                }
            }
        }

        if(op.equalsIgnoreCase("delete"))  {
            // Get parent
            SimpleItypeConfig parent = result.getParent();

           	// Element is at the top most level
            if(parent==null) {
                sp.removeElementAt(index);
            }

          	// Element is a subpart of another element
            else {
                // Locate the element index in the subparts
                Vector v = parent.getSubparts();
                for(int i=0; i<v.size(); i++) {
                    SimpleItypeConfig subp = (SimpleItypeConfig) v.elementAt(i);
                    if(subp.getId()==Long.parseLong(id)) {
                        v.removeElementAt(i);
                        break;
                    }
                }
            }
        }

        httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, sp);
    }


    /**
     * Search user-defined configs for SimpleItypeConfig with the given id
     * @param httpSession Session object containing user-defined configs
     * @param id Id of the target config
     * @param indx Stores the row number of the config element that has the match
     * @return null if not found, SimpleItypeConfig object if found
     */
    private SimpleItypeConfig search(
            HttpSession httpSession, String id, Vector indx, boolean clearErrorFlags) {

        // Read user defined config
        Vector sp = (Vector) httpSession.getAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME);

        // No subparts
        if(sp==null || sp.size()==0)
            return null;

        SimpleItypeConfig result = null;

        // Loop through itypes
        for(int i=0; i<sp.size(); i++) {
            SimpleItypeConfig sic = (SimpleItypeConfig) sp.elementAt(i);

            indx.clear();
            indx.addElement(""+i);

            if (clearErrorFlags)
                sic.setHasError(false);

            // Recursively process each itype config
            result = searchR(sic, id, clearErrorFlags);
            if(result!=null) break;
        }

        if (clearErrorFlags)
            httpSession.setAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME, sp);

        return result;
    }


    /**
     * Recursive function to perform search
     * @param sic Parent Config object
     * @param id Target Id
     * @return null if not found, SimpleItypeConfig object if found
     */
    private SimpleItypeConfig searchR(SimpleItypeConfig sic, String id, boolean clearErrorFlags) {

        if(sic.getId() == Long.parseLong(id))
            return sic;

        Vector v = sic.getSubparts();
        SimpleItypeConfig result = null;

        // Loop through children sub-parts
        for(int i=0; i<v.size(); i++) {
            SimpleItypeConfig sic1 = (SimpleItypeConfig) v.elementAt(i);

            if (clearErrorFlags)
                sic1.setHasError(false);

            result = searchR(sic1, id, clearErrorFlags);
            if(result!=null) break;
        }

        return result;
    }

    /**
     * Deletes configuration
     * and associated prefs
     * @param request
     * @param frm
     * @throws Exception
     */
    private void deleteConfig(
            HttpServletRequest request,
            InstructionalOfferingConfigEditForm frm) throws Exception {

		org.hibernate.Session hibSession = null;
        Transaction tx = null;

        try {

	        InstrOfferingConfigDAO iocDao = new InstrOfferingConfigDAO();
	        hibSession = iocDao.getSession();
	        tx = hibSession.beginTransaction();

            Long configId = frm.getConfigId();
	        InstrOfferingConfig ioc = iocDao.get(configId);
	        InstructionalOffering io = ioc.getInstructionalOffering();

	        deleteSubpart(request, hibSession, ioc, new HashMap());
	        io.removeConfiguration(ioc);

	        io.computeLabels(hibSession);
	        if (!ioc.isUnlimitedEnrollment().booleanValue())
	        	io.setLimit(new Integer(io.getLimit().intValue() - ioc.getLimit().intValue()));

            ChangeLog.addChange(
                    hibSession,
                    request,
                    io,
                    io.getCourseName()+" ["+ioc.getName()+"]",
                    ChangeLog.Source.INSTR_CFG_EDIT,
                    ChangeLog.Operation.DELETE,
                    io.getControllingCourseOffering().getSubjectArea(),
                    null);
            
            Event.deleteFromEvents(hibSession, ioc);
            Exam.deleteFromExams(hibSession, ioc);
            // The following line was calling delete ioc for the second time (which is a problem for MySQL as
            // it returns zero number of deleted lines even when called in the same transaction).
            //hibSession.delete(ioc);
	        hibSession.saveOrUpdate(io);

	        hibSession.flush();
            tx.commit();
            
            hibSession.refresh(io);

        	String className = ApplicationProperties.getProperty("tmtbl.external.instr_offr_config.change_action.class");
        	if (className != null && className.trim().length() > 0){
	        	ExternalInstrOffrConfigChangeAction configChangeAction = (ExternalInstrOffrConfigChangeAction) (Class.forName(className).newInstance());
	       		configChangeAction.performExternalInstrOffrConfigChangeAction(io, hibSession);
        	}

        }
        catch (Exception e) {
            try {
	            if (tx!=null && tx.isActive())
	                tx.rollback();
            }
            catch (Exception e1) { }

			Debug.error(e);
            throw (e);
        }
    }

    /**
     * Update configuration without destroying existing subparts/classes
     * and associated prefs
     * @param request
     * @param frm
     * @throws Exception
     */
    private void updateConfig(
            HttpServletRequest request,
            InstructionalOfferingConfigEditForm frm) throws Exception {

        HttpSession httpSession = request.getSession();

        // Read user defined config
        Vector sp = (Vector) httpSession.getAttribute(SimpleItypeConfig.CONFIGS_ATTR_NAME);

        // No subparts
        if(sp==null || sp.size()==0)
            return;

        // Get Manager
        User user = Web.getUser(httpSession);
        String mgrId = (String)user.getAttribute(Constants.TMTBL_MGR_ID_ATTR_NAME);
        TimetableManagerDAO mgrDao = new TimetableManagerDAO();
        TimetableManager mgr = mgrDao.get(new Long(mgrId));
        Debug.debug("Loaded manager: " + mgrId);

		RoomGroup rg = RoomGroup.getGlobalDefaultRoomGroup(Session.getCurrentAcadSession(user));
		if (rg!=null)
		    Debug.debug("Loaded default global room group: " + rg.getName());
		else
		    Debug.error("Default Global room group not found ");


		InstrOfferingConfig ioc = null;
		InstructionalOffering io = null;

		org.hibernate.Session hibSession = null;
        Transaction tx = null;

        try {

            InstructionalOfferingDAO ioDao = new InstructionalOfferingDAO();
            InstrOfferingConfigDAO iocDao = new InstrOfferingConfigDAO();
            hibSession = iocDao.getSession();
            tx = hibSession.beginTransaction();

            io = ioDao.get(new Long(frm.getInstrOfferingId()));
            Long configId = frm.getConfigId();
            Boolean unlimitedEnroll = (frm.getUnlimited()==null) ? new Boolean(false) : frm.getUnlimited();
            int limit = (unlimitedEnroll.booleanValue()) ? 0 : frm.getLimit();

            if (configId==null || configId.intValue()==0) {
                ioc = new InstrOfferingConfig();
                ioc.setLimit(new Integer(limit));
                ioc.setName(frm.getName());
                ioc.setUnlimitedEnrollment(unlimitedEnroll);
                ioc.setInstructionalOffering(io);
                io.addToinstrOfferingConfigs(ioc);

                hibSession.saveOrUpdate(ioc);
                hibSession.saveOrUpdate(io);
            }
            else {
                ioc = iocDao.get(configId);
                ioc.setLimit(new Integer(limit));
                ioc.setName(frm.getName());
                ioc.setUnlimitedEnrollment(unlimitedEnroll);
            }

            HashMap notDeletedSubparts = new HashMap();

            // Update subparts in the modified config
            for(int i=0; i<sp.size(); i++) {
                SimpleItypeConfig sic = (SimpleItypeConfig) sp.elementAt(i);
                createOrUpdateSubpart(request, hibSession, sic, ioc, null, mgr, rg, notDeletedSubparts);
                createOrUpdateClasses(request, hibSession, sic, ioc, null);
            }

            // Update Parents
            Set s = ioc.getSchedulingSubparts();
            for (Iterator i=s.iterator(); i.hasNext(); ) {
                SchedulingSubpart subp = (SchedulingSubpart) i.next();
                if (subp.getParentSubpart()==null) {
                    Debug.debug("Setting parents for " + subp.getItypeDesc());
                    updateParentClasses(subp, null, hibSession, notDeletedSubparts);
                }
            }

            // Remove any subparts that do not exist in the modified config
            deleteSubpart(request, hibSession, ioc, notDeletedSubparts);

            hibSession.saveOrUpdate(ioc);
            hibSession.saveOrUpdate(io);

            io.computeLabels(hibSession);

            ChangeLog.addChange(
                    hibSession,
                    request,
                    ioc,
                    ChangeLog.Source.INSTR_CFG_EDIT,
                    (configId==null || configId.intValue()==0?ChangeLog.Operation.CREATE:ChangeLog.Operation.UPDATE),
                    ioc.getInstructionalOffering().getControllingCourseOffering().getSubjectArea(),
                    null);


            hibSession.flush();

            tx.commit();
            hibSession.refresh(ioc);
            hibSession.refresh(io);
            
        	String className = ApplicationProperties.getProperty("tmtbl.external.instr_offr_config.change_action.class");
        	if (className != null && className.trim().length() > 0){
	        	ExternalInstrOffrConfigChangeAction configChangeAction = (ExternalInstrOffrConfigChangeAction) (Class.forName(className).newInstance());
	       		configChangeAction.performExternalInstrOffrConfigChangeAction(io, hibSession);
        	}
        }
        catch (Exception e) {
            try {
	            if (tx!=null && tx.isActive())
	                tx.rollback();
            }
            catch (Exception e1) { }

            try {
                if (ioc!=null)
                    hibSession.refresh(ioc);
            }
            catch (Exception e2) { }

            try {
                if (io!=null)
                    hibSession.refresh(io);
            }
            catch (Exception e3) { }

            Debug.error(e);
            throw (e);
        }
    }

    private void updateParentClasses(
            SchedulingSubpart subpart,
            SchedulingSubpart parent,
            org.hibernate.Session hibSession,
            HashMap notDeletedSubparts ) {

        if (parent==null) {
            Debug.debug("Parent is null. Setting all classes to have no parent");
            Set classes = subpart.getClasses();
            for (Iterator i=classes.iterator(); i.hasNext(); ) {
                Class_ c = (Class_) i.next();
                c.setParentClass(null);
                hibSession.saveOrUpdate(c);
            }
        }
        else {

            Set classes = subpart.getClasses();
            ArrayList classesList = new ArrayList(classes);
            Collections.sort(classesList, new ClassComparator(ClassComparator.COMPARE_BY_ID));

            Set parentClasses = parent.getClasses();
            int parentNumClasses = parentClasses.size();
            if (parentNumClasses>0) {
	            Iterator cci = classesList.iterator();
	            int classPerParent = classesList.size() / parentNumClasses;
	            int classPerParentRem = classesList.size() % parentNumClasses;
	            Debug.debug("Setting " + classPerParent + " class(es) per parent");
                Debug.debug("Odd number of classes found - " + classPerParentRem + " classes ... ");

	            for (Iterator i=parentClasses.iterator(); (i.hasNext() && classPerParent!=0); ) {
	                Class_ parentClass = (Class_) i.next();
	                for (int j=0; j<classPerParent; j++) {
	                    Class_ childClass = (Class_) cci.next();

	                    if (notDeletedSubparts.get(childClass.getSchedulingSubpart().getUniqueId())!=null) {
	    	                Debug.debug("Setting class " + childClass.getClassLabel() + " to parent " + parentClass.getClassLabel());
		                    childClass.setParentClass(parentClass);
		                    parentClass.addTochildClasses(childClass);
				            hibSession.saveOrUpdate(parentClass);
				            hibSession.saveOrUpdate(childClass);

	                    }
	                    else {
	    	                Debug.debug("Deleting class " + childClass.getClassLabel());
		                    if (childClass.getParentClass()!=null) {
		                        Class_ pc = childClass.getParentClass();
		                        pc.getChildClasses().remove(childClass);
		                        hibSession.saveOrUpdate(pc);
			                    childClass.setParentClass(null);
	    			            hibSession.saveOrUpdate(childClass);
		                    }
		                    classes.remove(childClass);
		                    childClass.deleteAllDependentObjects(hibSession, false);
		                    hibSession.delete(childClass);
    			            hibSession.saveOrUpdate(subpart);
	                    }

	    	            if (classPerParentRem!=0) {
	    	                if (cci.hasNext()) {
	   	                        childClass = (Class_) cci.next();
	   		                    if (notDeletedSubparts.get(childClass.getSchedulingSubpart().getUniqueId())!=null) {
	    	    	                Debug.debug("Setting ODD class " + childClass.getClassLabel() + " to parent " + parentClass.getClassLabel());
		   	                        childClass.setParentClass(parentClass);
		    	                    parentClass.addTochildClasses(childClass);
		    			            hibSession.saveOrUpdate(parentClass);
		    			            hibSession.saveOrUpdate(childClass);
	   		                    }
	   		                    else {
	   		    	                Debug.debug("Deleting ODD class " + childClass.getClassLabel());
	   			                    if (childClass.getParentClass()!=null) {
	   			                        Class_ pc = childClass.getParentClass();
	   			                        pc.getChildClasses().remove(childClass);
	   			                        hibSession.saveOrUpdate(pc);
	   				                    childClass.setParentClass(null);
	   		    			            hibSession.saveOrUpdate(childClass);
	   			                    }
	   			                    classes.remove(childClass);
	   			                    childClass.deleteAllDependentObjects(hibSession, false);
	   			                    hibSession.delete(childClass);
		    			            hibSession.saveOrUpdate(childClass.getSchedulingSubpart());
	   		                    }

	    	                }

	    	                --classPerParentRem;
	    	            }
	                }
	            }

	            if (classPerParentRem!=0) {
	                Iterator cci2 = classesList.iterator();
    	            for (Iterator i=parentClasses.iterator(); i.hasNext(); ) {
    	                Class_ parentClass = (Class_) i.next();
    	                if (cci2.hasNext()) {
   	                        Class_ childClass = (Class_) cci2.next();
	    	                Debug.debug("Setting ODD class " + childClass.getClassLabel() + " to parent " + parentClass.getClassLabel());
   	                        childClass.setParentClass(parentClass);
    	                    parentClass.addTochildClasses(childClass);
    			            hibSession.saveOrUpdate(parentClass);
    			            hibSession.saveOrUpdate(childClass);
    	                }

    	                --classPerParentRem;
    	                if (classPerParentRem==0)
    	                    break;
    	            }
	            }

	            hibSession.saveOrUpdate(parent);
            }


        }

        // Traverse through children
        Set childSubparts = subpart.getChildSubparts();
        if (childSubparts==null || childSubparts.size()==0) return;
        for (Iterator i=childSubparts.iterator(); i.hasNext(); ) {
            SchedulingSubpart cs = (SchedulingSubpart) i.next();
            updateParentClasses(cs, subpart, hibSession, notDeletedSubparts);
        }
    }

    /**
     * Deletes all the subparts that do not exist in the modified config
     * @param request
     * @param hibSession
     * @param ioc
     * @param notDeletedSubparts
     * @throws Exception
     */
    private void deleteSubpart(
            HttpServletRequest request,
            org.hibernate.Session hibSession,
            InstrOfferingConfig ioc,
            HashMap notDeletedSubparts ) throws Exception {

        Set s = ioc.getSchedulingSubparts();
        HashMap deletedSubparts = new HashMap();

        for (Iterator i=s.iterator(); i.hasNext(); ) {
            SchedulingSubpart tmpSubpart = (SchedulingSubpart) i.next();
            if (notDeletedSubparts.get(tmpSubpart.getUniqueId())==null) {
                Debug.debug("Deleting subpart ... " + tmpSubpart.getUniqueId() + ", " + tmpSubpart.getItypeDesc() );

                // Delete classes
                Set classes = tmpSubpart.getClasses();
                for (Iterator j=classes.iterator(); j.hasNext();) {
                    Class_ c = (Class_) j.next();
                    deleteChildClasses(c, hibSession, 1);
                    Class_ pc = c.getParentClass();
                    if (pc!=null) {
                        pc.getChildClasses().remove(c);
                        if (notDeletedSubparts.get(pc.getSchedulingSubpart().getUniqueId())!=null)
                            hibSession.saveOrUpdate(pc);
                    }
                    j.remove();
                }

                // Delete from parent
                SchedulingSubpart parentSubpart = tmpSubpart.getParentSubpart();
                if (parentSubpart!=null) {
                    if (parentSubpart.getChildSubparts()!=null)
                        parentSubpart.getChildSubparts().remove(tmpSubpart);
                    tmpSubpart.setParentSubpart(null);
                    if (deletedSubparts.get(parentSubpart.getUniqueId())==null)
                        hibSession.saveOrUpdate(parentSubpart);
                }

                // Remove references from child subparts
                Set childSubparts = tmpSubpart.getChildSubparts();
                if (childSubparts!=null)
                    Debug.debug("Child subparts exist ... " + childSubparts.size());
                tmpSubpart.setChildSubparts(null);

                deletedSubparts.put(tmpSubpart.getUniqueId(), tmpSubpart.getUniqueId());
                hibSession.delete(tmpSubpart);
                i.remove();
            }
        }

        hibSession.saveOrUpdate(ioc);
        hibSession.flush();
    }


    /**
     * Recursively process the updated config subparts
     * @param request
     * @param hibSession
     * @param sic
     * @param ioc
     * @param parent
     * @param mgr
     * @param notDeletedSubparts Holds all the subpart ids encountered while recursing
     * @throws Exception
     */
    private void createOrUpdateSubpart(
            HttpServletRequest request,
            org.hibernate.Session hibSession,
            SimpleItypeConfig sic,
            InstrOfferingConfig ioc,
            SchedulingSubpart parent,
            TimetableManager mgr,
            RoomGroup rg,
            HashMap notDeletedSubparts ) throws Exception {

        // Set attributes
        String subpartId = request.getParameter("subpartId" + sic.getId());
        String minLimitPerClass = request.getParameter("mnlpc" + sic.getId());
        String maxLimitPerClass = request.getParameter("mxlpc" + sic.getId());
        String minPerWk = request.getParameter("mpw" + sic.getId());
        String numClasses = request.getParameter("nc" + sic.getId());
        String numRooms = request.getParameter("nr" + sic.getId());
        String roomRatio = request.getParameter("rr" + sic.getId());
        String managingDept = request.getParameter("md" + sic.getId());
        String disabled = request.getParameter("disabled" + sic.getId());

        if(subpartId!=null)
            sic.setSubpartId(Long.parseLong(subpartId));
        if(minLimitPerClass!=null)
            sic.setMinLimitPerClass(Constants.getPositiveInteger(minLimitPerClass, 0));
        if(maxLimitPerClass!=null)
            sic.setMaxLimitPerClass(Constants.getPositiveInteger(maxLimitPerClass, 0));
        if(minPerWk!=null)
            sic.setMinPerWeek(Integer.parseInt(minPerWk));
        if(numClasses!=null)
            sic.setNumClasses(Integer.parseInt(numClasses));
        if(numRooms!=null)
            sic.setNumRooms(Constants.getPositiveInteger(numRooms, 0));
        if(roomRatio!=null)
            sic.setRoomRatio(Constants.getPositiveFloat(roomRatio, 0.0f));
        if(managingDept!=null)
            sic.setManagingDeptId(Long.parseLong(managingDept));
        if(disabled!=null)
            sic.setDisabled(new Boolean(request.getParameter("disabled" + sic.getId())).booleanValue());

        // Read attributes
        long sid = sic.getSubpartId();
		int mnlpc = sic.getMinLimitPerClass();
		int mxlpc = sic.getMaxLimitPerClass();
        int mpw = sic.getMinPerWeek();
        int nr = sic.getNumRooms();
        float rr = sic.getRoomRatio();
        long md = sic.getManagingDeptId();
        boolean db = sic.isDisabled();

        if (ioc.isUnlimitedEnrollment().booleanValue()) {
    		mnlpc = 0;
    		mxlpc = 0;
            nr = 0;
            rr = 0;
        }

        if (request.getParameter("varLimits")==null) {
            mnlpc = mxlpc;
        }

        SchedulingSubpart subpart = null;

        // Subpart does not exist
        if (sid<0) {
            Debug.debug("Subpart does not exist ... Creating subpart - " + sic.getItype().getDesc());

            // Create subpart
            subpart = new SchedulingSubpart();
            subpart.setInstrOfferingConfig(ioc);
            subpart.setItype(sic.getItype());
            subpart.setMinutesPerWk(new Integer(mpw));
            subpart.setParentSubpart(parent);
            subpart.setAutoSpreadInTime(new Boolean(true));
            subpart.setStudentAllowOverlap(new Boolean(false));
            ioc.addToschedulingSubparts(subpart);

            if (md<0 && !ioc.isUnlimitedEnrollment().booleanValue() && rg!=null) {
	            // Add default room group pref of classroom
	            HashSet prefs = new HashSet();
	            RoomGroupPref rgp = new RoomGroupPref();

	            rgp.setPrefLevel(
	                    PreferenceLevel.getPreferenceLevel(
	                            Integer.parseInt(PreferenceLevel.PREF_LEVEL_REQUIRED)));
	            rgp.setRoomGroup(rg);
	            rgp.setOwner(subpart);
	            prefs.add(rgp);
	            subpart.setPreferences(prefs);
            }

            hibSession.saveOrUpdate(subpart);
            hibSession.flush();

            hibSession.refresh(subpart);
            sid = subpart.getUniqueId().longValue();
            Debug.debug("New subpart uniqueid: " + sid);
            sic.setSubpartId(sid);
            notDeletedSubparts.put(new Long(sid), "");
        } // End If: Subpart does not exist

        // Subpart exists
        else {
            Debug.debug("Subpart exists ... Updating");

            notDeletedSubparts.put(new Long(sid), "");

            Set s = ioc.getSchedulingSubparts();
            for (Iterator i=s.iterator(); i.hasNext(); ) {
                SchedulingSubpart tmpSubpart = (SchedulingSubpart) i.next();
                if (tmpSubpart.getUniqueId().longValue()==sid) {
                    subpart = tmpSubpart;
                    break;
                }
            }

            if (subpart==null)
                throw new Exception ("Scheduling Subpart " + sid + " was not found.");

            Debug.debug("Creating / Updating subpart - " + subpart.getItypeDesc());

            Set classes = subpart.getClasses();

            // Update only if user has permissions and does not have mixed managed classes
            //if (subpart.isEditableBy(Web.getUser(request.getSession())) && !subpart.hasMixedManagedClasses()) {
            if (!db) {

	            // If minutes per week has changed then delete time pattern and time prefs
	            if (subpart.getMinutesPerWk().intValue()!=mpw) {
	                Debug.debug("Minutes per week changed ... Deleting time prefs on subpart and classes");
		            subpart.setMinutesPerWk(new Integer(mpw));
		            Set prefs = subpart.getPreferences();
		            for (Iterator i=prefs.iterator(); i.hasNext(); ) {
		                Preference pref = (Preference) i.next();
		                if (pref instanceof TimePref) {
			                pref.setOwner(null);
			                hibSession.delete(pref);
			                i.remove();
		                }
		            }

		            for (Iterator i=classes.iterator(); i.hasNext(); ) {
		                Class_ c = (Class_) i.next();
			            Set cPrefs = c.getPreferences();
			            for (Iterator j=cPrefs.iterator(); j.hasNext(); ) {
			                Preference pref = (Preference) j.next();
			                if (pref instanceof TimePref) {
				                pref.setOwner(null);
				                hibSession.delete(pref);
				                j.remove();
			                }
			            }
		                hibSession.saveOrUpdate(c);
		            }
	            }

	            // Manager changed
	            boolean managerChanged = false;
	            long mdId = md;
	            if (md<0) {
	                mdId = subpart.getInstrOfferingConfig()
	                			.getControllingCourseOffering().getSubjectArea()
	                			.getDepartment().getUniqueId().longValue();
	            }

	            if (subpart.getManagingDept().getUniqueId().longValue()!=mdId) {
	                Debug.debug("Subpart Managing department changed ...");
	                managerChanged = true;

	                // Remove from distribution prefs
	                subpart.deleteAllDistributionPreferences(hibSession);

	                // Clear all prefs - except time & distribution
	                Set prefs = subpart.getPreferences();
	                for (Iterator prefI= prefs.iterator(); prefI.hasNext(); ) {
	                    Object a = prefI.next();

	                    if (a instanceof RoomPref || a instanceof BuildingPref
	                            || a instanceof RoomGroupPref || a instanceof RoomFeaturePref ) {
	                        prefI.remove();
	                    }

	                    //Weaken time preferences if the new manager is external
	                    if (a instanceof TimePref) {
                            Department mgDept = new DepartmentDAO().get(new Long(mdId));
                            if (mgDept.isExternalManager().booleanValue()) {
                                //weaken only when both controling and managing departments do not allow required time
                                if (subpart.getControllingDept().isAllowReqTime()==null || !subpart.getControllingDept().isAllowReqTime().booleanValue()) {
                                    if (mgDept.isAllowReqTime()==null || !mgDept.isAllowReqTime().booleanValue()) {
                                        ((TimePref)a).weakenHardPreferences();
                                    }
                                }
                            }
                            /*
                            // Set all time prefs to neutral in order to preserve time pattern
                    		TimePref tp = new TimePref();
                    		tp.setTimePattern(((TimePref) a).getTimePattern());
                    		String prefStr = tp.getTimePatternModel().getPreferences();
                    		((TimePref) a).setPreference(tp.getPreference());
                            */
	                    }
	                }

	                // Check if changed to Department and is not unlimited enroll
	                if (md<0 && !ioc.isUnlimitedEnrollment().booleanValue() && rg!=null) {
	                    // Add default room group pref of classroom
	    	            RoomGroupPref rgp = new RoomGroupPref();
	    	            rgp.setPrefLevel(
	    	                    PreferenceLevel.getPreferenceLevel(
	    	                            Integer.parseInt(PreferenceLevel.PREF_LEVEL_REQUIRED)));
	    	            rgp.setRoomGroup(rg);
	    	            rgp.setOwner(subpart);
	                    prefs.add(rgp);
	                }
	            }

	            // Update expected capacity and room capacity
	            for (Iterator i=classes.iterator(); i.hasNext(); ) {
	                Debug.debug("Updating expected capacity and room capacity on class ...");
	                Class_ c = (Class_) i.next();
	                c.setExpectedCapacity(new Integer(mnlpc));
	                c.setMaxExpectedCapacity(new Integer(mxlpc));
	                c.setRoomRatio(new Float(rr));
	                c.setNbrRooms(new Integer(nr));
	                c.setDisplayInstructor(new Boolean(true));
	                c.setDisplayInScheduleBook(new Boolean(true));

	                if (managerChanged) {
	                    if (c.getManagingDept().getUniqueId().longValue()!=mdId) {
		                    Debug.debug("Class Managing department changed ...");

                            // Update Managing Department
                            c.setManagingDept(new DepartmentDAO().get(new Long(mdId)));

		                    // Remove from distribution prefs
		                    c.deleteAllDistributionPreferences(hibSession);

		                    // Clear all prefs - except time & distribution
		                    Set prefs = c.getPreferences();
		                    for (Iterator prefI= prefs.iterator(); prefI.hasNext(); ) {
		                        Object a = prefI.next();

		                        if (a instanceof RoomPref || a instanceof BuildingPref
		                                || a instanceof RoomGroupPref || a instanceof RoomFeaturePref ) {
		                            prefI.remove();
		                        }

		                        // Weaken time preferences if the new manager is external, remove exact times
		                        if (a instanceof TimePref) {
			                    	if (((TimePref)a).getTimePattern().getType().intValue()==TimePattern.sTypeExactTime) {
			                    		prefI.remove();
			                    	} else {
                                        if (c.getManagingDept().isExternalManager().booleanValue()) {
                                            //weaken only when both controling and managing departments do not allow required time
                                            if (c.getControllingDept().isAllowReqTime()==null || !c.getControllingDept().isAllowReqTime().booleanValue()) {
                                                if (c.getManagingDept().isAllowReqTime()==null || !c.getManagingDept().isAllowReqTime().booleanValue()) {
                                                    ((TimePref)a).weakenHardPreferences();
                                                }
                                            }
                                        }
                                        /*
                                        // Set all time prefs to neutral in order to preserve time pattern
			                    		TimePref tp = new TimePref();
			                    		tp.setTimePattern(((TimePref) a).getTimePattern());
			                    		String prefStr = tp.getTimePatternModel().getPreferences();
			                    		((TimePref) a).setPreference(prefStr);
                                        */
			                    	}
		                        }
		                    }
	                    }
	                    else {
		                    Debug.debug("Class Managing department same as subpart ... ignoring");
	                    }
	                }
	                hibSession.saveOrUpdate(c);
	            }
            } // End: Update only if user has permissions and does not have mixed managed classes

            // Update Parent
            if ( (parent!=null && subpart.getParentSubpart()!=null && !subpart.getParentSubpart().equals(parent))
                    || (parent==null && subpart.getParentSubpart()!=null)
                    || (parent!=null && subpart.getParentSubpart()==null) ) {

                Debug.debug("Updating parent subparts and classes ...");
                subpart.setParentSubpart(parent);

                // Update parent for classes
    	        if (parent==null) {
    	            Debug.debug("No parent subparts ... making top level class");
    	            for (Iterator cci = subpart.getClasses().iterator(); cci.hasNext(); ) {
    	                Class_ childClass = (Class_) cci.next();
    	                childClass.setParentClass(null);
			            hibSession.saveOrUpdate(childClass);
    	            }
    	        }
    	        else {
    	            Debug.debug("Parent subpart exists ... setting parent class");

    	            ArrayList classesList = new ArrayList(classes);
    	            Collections.sort(classesList, new ClassComparator(ClassComparator.COMPARE_BY_ID));

    	            Set parentClasses = parent.getClasses();
    	            int parentNumClasses = parentClasses.size();
    	            if (parentNumClasses>0) {
	    	            Iterator cci = classesList.iterator();
	    	            int classPerParent = classesList.size() / parentNumClasses;
	    	            int classPerParentRem = classesList.size() % parentNumClasses;
	    	            Debug.debug("Setting " + classPerParent + " class(es) per parent");
    	                Debug.debug("Odd number of classes found - " + classPerParentRem + " classes ... ");

	    	            for (Iterator i=parentClasses.iterator(); (i.hasNext() && classPerParent!=0); ) {
	    	                Class_ parentClass = (Class_) i.next();
	    	                for (int j=0; j<classPerParent; j++) {
	   	                        Class_ childClass = (Class_) cci.next();
    	    	                Debug.debug("Setting class " + childClass.getClassLabel() + " to parent " + parentClass.getClassLabel());
	    	                    childClass.setParentClass(parentClass);
	    	                    parentClass.addTochildClasses(childClass);
	    			            hibSession.saveOrUpdate(parentClass);
	    			            hibSession.saveOrUpdate(childClass);

	    	    	            if (classPerParentRem!=0) {
	    	    	                if (cci.hasNext()) {
	    	   	                        childClass = (Class_) cci.next();
		    	    	                Debug.debug("Setting ODD class " + childClass.getClassLabel() + " to parent " + parentClass.getClassLabel());
	    	   	                        childClass.setParentClass(parentClass);
	    	    	                    parentClass.addTochildClasses(childClass);
	    	    			            hibSession.saveOrUpdate(parentClass);
	    	    			            hibSession.saveOrUpdate(childClass);
	    	    	                }

	    	    	                --classPerParentRem;
	    	    	            }
	    	                }
	    	            }

	    	            if (classPerParentRem!=0) {
	    	                Iterator cci2 = classesList.iterator();
		    	            for (Iterator i=parentClasses.iterator(); i.hasNext(); ) {
		    	                Class_ parentClass = (Class_) i.next();
    	    	                if (cci2.hasNext()) {
    	   	                        Class_ childClass = (Class_) cci2.next();
	    	    	                Debug.debug("Setting ODD class " + childClass.getClassLabel() + " to parent " + parentClass.getClassLabel());
    	   	                        childClass.setParentClass(parentClass);
    	    	                    parentClass.addTochildClasses(childClass);
    	    			            hibSession.saveOrUpdate(parentClass);
    	    			            hibSession.saveOrUpdate(childClass);
    	    	                }

    	    	                --classPerParentRem;
    	    	                if (classPerParentRem==0)
    	    	                    break;
		    	            }
	    	            }

	    	            hibSession.saveOrUpdate(parent);
    	            }
    	        }
           } // End If: Update Parent

            hibSession.saveOrUpdate(subpart);
            hibSession.flush();
            hibSession.refresh(subpart);
            if (parent!=null)
                hibSession.refresh(parent);

        } // End If: Subpart Exists

        // Loop through children sub-parts
        Vector v = sic.getSubparts();
        for(int i=0; i<v.size(); i++) {
            SimpleItypeConfig sic1 = (SimpleItypeConfig) v.elementAt(i);
            createOrUpdateSubpart(request, hibSession, sic1, ioc, subpart, mgr, rg, notDeletedSubparts);
        }

        hibSession.saveOrUpdate(ioc);
        hibSession.flush();
    }


    /**
     * Create or Update Classes
     * @param sic
     * @param subpart
     * @param mgr
     * @param ncfs
     * @param lfs
     */
    private void createOrUpdateClasses(
            HttpServletRequest request,
            org.hibernate.Session hibSession,
            SimpleItypeConfig sic,
            InstrOfferingConfig ioc,
            SchedulingSubpart parent) throws Exception {

		// Read attributes
        long sid = sic.getSubpartId();
		int mnlpc = sic.getMinLimitPerClass();
		int mxlpc = sic.getMaxLimitPerClass();
        int nc = sic.getNumClasses();
        int nr = sic.getNumRooms();
        float rr = sic.getRoomRatio();
        long md = sic.getManagingDeptId();
        boolean db = sic.isDisabled();

        if (ioc.isUnlimitedEnrollment().booleanValue()) {
    		mnlpc = 0;
    		mxlpc = 0;
            nr = 0;
            rr = 0;
        }

        if (request.getParameter("varLimits")==null) {
            mnlpc = mxlpc;
        }

       SchedulingSubpart subpart = null;

        // Subpart does not exist
        if (sid<0) {
            throw new Exception ("Subpart does not exist ... Cannot create classes ");
        }

        Set s = ioc.getSchedulingSubparts();
        for (Iterator i=s.iterator(); i.hasNext(); ) {
            SchedulingSubpart tmpSubpart = (SchedulingSubpart) i.next();
            if (tmpSubpart.getUniqueId().longValue()==sid) {
                subpart = tmpSubpart;
                break;
            }
        }

        if (subpart==null)
            throw new Exception ("Scheduling Subpart " + sid + " was not found.");

        Debug.debug("Creating / Updating classes for subpart - " + subpart.getItypeDesc());

        Set classes = subpart.getClasses();
        int numCls = classes.size();
        boolean readOnly = false;

        //if (!subpart.isEditableBy(Web.getUser(request.getSession())) || subpart.hasMixedManagedClasses()) {
        if (db) {
            Debug.debug("Subpart is readonly ... cannot change classes");
            readOnly = true;
        }

        // Get Number of Parent Classes
        HashMap cpClasses = new HashMap();
        if (parent!=null) {
	        for (Iterator i=classes.iterator(); i.hasNext(); ) {
	            Class_ c = (Class_) i.next();
	            if (c.getParentClass()!=null) {
		            Integer classCount = (Integer) cpClasses.get(c.getParentClass().getUniqueId());
		            if (classCount==null)
		                cpClasses.put(c.getParentClass().getUniqueId(), new Integer(1));
		            else
		                cpClasses.put(c.getParentClass().getUniqueId(), new Integer(classCount.intValue()+1));
	            }
	        }
	        int cpNumClasses = cpClasses.size();
	        int peerNumClasses = parent.getClasses().size();

            // Adjust child per parent
            if (cpNumClasses!=peerNumClasses) {

                if (readOnly)
                    throw new Exception ("Subpart " + subpart.toString() + " has read-only permissions - Number of classes cannot be changed. ");

                Debug.debug("Parents per child ( " + cpNumClasses +  " ) do not match up to - " + peerNumClasses);
                int classesPerParent = numCls/peerNumClasses;
                if (numCls>classesPerParent && cpClasses.size()>0) {
                    int diff = (numCls - classesPerParent) / cpClasses.size();
                    Debug.debug("Deleting " + diff +  " classes per current parent");

                    // Delete extra classes
                    Set parentClassKeys = cpClasses.keySet();
                    for (Iterator ci=parentClassKeys.iterator(); ci.hasNext(); ) {
                        Long parentClassId = (Long) ci.next();
                        int parentClassCount = ((Integer)cpClasses.get(parentClassId)).intValue();
                        int deleteCount = parentClassCount - classesPerParent;
                        Debug.debug("Deleting " + deleteCount + " classes for parent class: " + parentClassId.toString());

                        ArrayList uids = new ArrayList();
        	            for (Iterator i=classes.iterator(); i.hasNext(); ) {
        	                Class_ c1 = (Class_) i.next();
        	                if (c1.getParentClass().getUniqueId().equals(parentClassId))
        	                    uids.add(c1.getUniqueId());
        	            }
        	            Collections.sort(uids);

        	            for (int ct=(uids.size()-deleteCount); ct<uids.size(); ct++) {
        	                Long uid = (Long) uids.get(ct);
            	            for (Iterator i=classes.iterator(); i.hasNext(); ) {
            	                Class_ c = (Class_) i.next();
            	                if (c.getUniqueId().equals(uid)) {
            	                    deleteChildClasses(c, hibSession, 1);
            	                    Class_ pc = c.getParentClass();
            	                    if (pc!=null) {
            	                        pc.getChildClasses().remove(c);
            	                        hibSession.saveOrUpdate(pc);
            	                    }
            	                    i.remove();
    	    	                    //hibSession.delete(c);
    	    	                    //hibSession.flush();
            	                    break;
            	                }
            	            }
        	            }

        	            hibSession.saveOrUpdate(subpart);
                        hibSession.saveOrUpdate(parent);
                        hibSession.flush();
                    }
                }
                else {
                    int diff = classesPerParent - numCls;
                    Debug.debug("Adding  " + diff +  " classes");
                    // Do nothing - Will be taken care of in the code below
                }
            }
        }

       // No. of classes changed
        numCls = classes.size();
        if (numCls!= nc) {

            if (readOnly)
                throw new Exception ("Subpart " + subpart.toString() + " has read-only permissions - Number of classes cannot be changed. ");

            // Increased - create new classes
            if (nc>numCls) {
                Debug.debug("No. of classes increased ... Adding " + (nc-numCls) + " classes");
                for (int ct=0; ct<(nc-numCls); ct++) {
                    Class_ c = new Class_();
        	        c.setSchedulingSubpart(subpart);
                    c.setExpectedCapacity(new Integer(mnlpc));
                    c.setMaxExpectedCapacity(new Integer(mxlpc));
                    c.setRoomRatio(new Float(rr));
                    c.setNbrRooms(new Integer(nr));
                    c.setDisplayInstructor(new Boolean(true));
                    c.setDisplayInScheduleBook(new Boolean(true));
        	        c.setPreferences(new HashSet());
        	        if (md>0)
        	            c.setManagingDept(new DepartmentDAO().get(new Long(md)));
        	        subpart.addToclasses(c);
                }

                hibSession.saveOrUpdate(subpart);
                hibSession.flush();

                setParentClass(hibSession, subpart, parent, nc);
            }

            // Decreased - delete last class created
            else {
                Debug.debug("No. of classes decreased ... Deleting " + (numCls-nc) + " classes");
                ArrayList uids = new ArrayList();
	            for (Iterator i=classes.iterator(); i.hasNext(); ) {
	                uids.add(((Class_) i.next()).getUniqueId());
	            }
	            Collections.sort(uids);

	            // Delete last class(es) if no parent or just one class to be deleted
	            if (parent==null || (numCls-nc)==1) {
		            for (int ct=nc; ct<numCls; ct++) {
		                Long uid = (Long) uids.get(ct);
	    	            for (Iterator i=classes.iterator(); i.hasNext(); ) {
	    	                Class_ c = (Class_) i.next();
	    	                if (c.getUniqueId().equals(uid)) {
	    	                    deleteChildClasses(c, hibSession, 1);
        	                    Class_ pc = c.getParentClass();
        	                    if (pc!=null) {
        	                        pc.getChildClasses().remove(c);
        	                        hibSession.saveOrUpdate(pc);
        	                    }
	    	                    i.remove();
	    	                    //hibSession.delete(c);
	    	                    break;
	    	                }
	    	            }
		            }
	            }

	            // Delete per parent
	            else {
	                cpClasses.clear();
	    	        for (Iterator i=classes.iterator(); i.hasNext(); ) {
	    	            Class_ c = (Class_) i.next();
	    	            if (c.getParentClass()!=null) {
	    		            Integer classCount = (Integer) cpClasses.get(c.getParentClass().getUniqueId());
	    		            if (classCount==null)
	    		                cpClasses.put(c.getParentClass().getUniqueId(), new Integer(1));
	    		            else
	    		                cpClasses.put(c.getParentClass().getUniqueId(), new Integer(classCount.intValue()+1));
	    	            }
	    	        }
                    int diff = (numCls - nc) / cpClasses.size();
                    Debug.debug("Deleting " + diff +  " classes per current parent");

                    // Delete extra classes
                    Set parentClassKeys = cpClasses.keySet();
                    for (Iterator ci=parentClassKeys.iterator(); ci.hasNext(); ) {
                        Long parentClassId = (Long) ci.next();
                        Debug.debug("Deleting " + diff + " classes for parent class: " + parentClassId.toString());

                        uids.clear();
        	            for (Iterator i=classes.iterator(); i.hasNext(); ) {
        	                Class_ c1 = (Class_) i.next();
        	                if (c1.getParentClass().getUniqueId().equals(parentClassId))
        	                    uids.add(c1.getUniqueId());
        	            }
        	            Collections.sort(uids);

        	            for (int ct=(uids.size()-diff); ct<uids.size(); ct++) {
        	                Long uid = (Long) uids.get(ct);
            	            for (Iterator i=classes.iterator(); i.hasNext(); ) {
            	                Class_ c = (Class_) i.next();
            	                if (c.getUniqueId().equals(uid)) {
            	                    deleteChildClasses(c, hibSession, 1);
            	                    Class_ pc = c.getParentClass();
            	                    if (pc!=null) {
            	                        pc.getChildClasses().remove(c);
            	                        hibSession.saveOrUpdate(pc);
            	                    }
            	                    i.remove();
            	                    //hibSession.delete(c);
            	                    break;
            	                }
            	            }
        	            }

        	            hibSession.saveOrUpdate(subpart);
                        hibSession.saveOrUpdate(parent);
                        hibSession.flush();
                    }
	            }
	            hibSession.saveOrUpdate(subpart);

            }
        }

        // Loop through children sub-parts
        Vector v = sic.getSubparts();
        for(int i=0; i<v.size(); i++) {
            SimpleItypeConfig sic1 = (SimpleItypeConfig) v.elementAt(i);
            createOrUpdateClasses(request, hibSession, sic1, ioc, subpart);
        }
    }

    public void deleteChildClasses(
            Class_ c,
            org.hibernate.Session hibSession,
            int recurseLevel) {

        Set childClasses = c.getChildClasses();
        if (childClasses==null || childClasses.size()==0) {
            if (recurseLevel==1) {
                Debug.debug("Deleting class (1) ... " +  c.getClassLabel() + " - " + c.getUniqueId());
                c.deleteAllDependentObjects(hibSession, false);
                hibSession.delete(c);
            }
            return;
        }

        for (Iterator cci=childClasses.iterator(); cci.hasNext(); ) {
            Class_ cc = (Class_) cci.next();
            SchedulingSubpart ps = cc.getSchedulingSubpart();
            Set psClasses = ps.getClasses();
            for (Iterator iPs = psClasses.iterator(); iPs.hasNext(); ) {
                Class_ psCls = (Class_) iPs.next();
                if (psCls.equals(cc)) {
                    deleteChildClasses(psCls, hibSession, recurseLevel+1);
                    Debug.debug("Deleting class (2) ... " +  cc.getClassLabel() + " - " + cc.getUniqueId());
                    cc.deleteAllDependentObjects(hibSession, false);
                    hibSession.delete(cc);
                    iPs.remove();
                    hibSession.saveOrUpdate(ps);
                    hibSession.saveOrUpdate(c);
                    break;
                }
            }

            cci.remove();
        }

        Debug.debug("Deleting class (3) ... " +  c.getClassLabel() + " - " + c.getUniqueId());
        c.deleteAllDependentObjects(hibSession, false);
        hibSession.delete(c);
        //hibSession.flush();
    }

    public void setParentClass(
            org.hibernate.Session hibSession,
            SchedulingSubpart subpart,
            SchedulingSubpart parent,
            int subpartNumClasses) {

        // Set Parent Class
        if (parent!=null) {
            Set parentClasses = parent.getClasses();
            int parentNumClasses = parentClasses.size();
            int classesPerParent = subpartNumClasses / parentNumClasses;

            HashMap cpClasses = new HashMap();
	        for (Iterator i=parent.getClasses().iterator(); i.hasNext(); ) {
	            Class_ c = (Class_) i.next();
	            int childClassCount = 0;
	            Set ccl = c.getChildClasses();
	            if (ccl!=null) {
	                for (Iterator ci=ccl.iterator(); ci.hasNext(); ) {
	                    Class_ cc = (Class_) ci.next();
	                    if (cc.getSchedulingSubpart().equals(subpart))
	                        ++childClassCount;
	                }
	            }

                cpClasses.put(c.getUniqueId(), new Integer(childClassCount));
	        }

            ArrayList parentClassKeys = new ArrayList(cpClasses.keySet());
            Collections.sort(parentClassKeys);

            for (Iterator ci=parentClassKeys.iterator(); ci.hasNext(); ) {
                Long parentClassId = (Long) ci.next();
                int parentClassCount = ((Integer)cpClasses.get(parentClassId)).intValue();

                if (classesPerParent>parentClassCount) {
                    int addCount = classesPerParent - parentClassCount;
                    Debug.debug("Adding " + addCount + " classes for parent class: " + parentClassId.toString());
                    ArrayList ccList = new ArrayList(subpart.getClasses());
                    Collections.sort(ccList, new ClassComparator(ClassComparator.COMPARE_BY_ID));
                    Iterator cci = ccList.iterator();
                    for (Iterator i=parentClasses.iterator(); i.hasNext(); ) {
                        Class_ parentClass = (Class_) i.next();
                        if (parentClass.getUniqueId().equals(parentClassId)) {
                            for (int j=0; j<addCount; j++) {
                                Class_ childClass = null;
                                do {
                                    childClass = (Class_) cci.next();
                                } while (childClass.getParentClass()!=null);

                                childClass.setParentClass(parentClass);
                                parentClass.addTochildClasses(childClass);
            		            hibSession.saveOrUpdate(parentClass);
            		            hibSession.saveOrUpdate(childClass);
                            }
                        }
                    }
                }
            }

            hibSession.saveOrUpdate(parent);
        }

    }
}
