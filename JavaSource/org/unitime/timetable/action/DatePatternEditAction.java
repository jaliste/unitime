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
package org.unitime.timetable.action;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.LabelValueBean;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.DatePatternEditForm;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.dao.DatePatternDAO;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.TimePatternDAO;
import org.unitime.timetable.util.Constants;

import net.sf.cpsolver.ifs.util.CSVFile;

/** 
 * @author Tomas Muller
 */
public class DatePatternEditAction extends Action {
	
	// --------------------------------------------------------- Instance Variables
	
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

	// --------------------------------------------------------- Methods
	
	/** 
	 * Method execute
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			DatePatternEditForm myForm = (DatePatternEditForm) form;
			
	        // Check Access
	        if (!Web.isLoggedIn(request.getSession()) || !Web.hasRole(request.getSession(),Roles.getAdminRoles())) {
	            throw new Exception ("Access Denied.");
	        }
	        
	        // Read operation to be performed
	        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));
	        
	        if (request.getParameterValues("depts")!=null) {
	        	String[] depts = request.getParameterValues("depts");
	        	for (int i=0;i<depts.length;i++)
	        		myForm.getDepartmentIds().add(new Long(depts[i]));
	        }

	        if (op==null) {
	            myForm.load(null);
	            myForm.setOp("List");
	        }
	        
	    	User user = Web.getUser(request.getSession());
	    	Long sessionId = Session.getCurrentAcadSession(user).getSessionId();

	    	List list = (new DepartmentDAO()).getSession()
						.createCriteria(Department.class)
						.add(Restrictions.eq("session.uniqueId", sessionId))
						.addOrder(Order.asc("deptCode"))
						.list();
	    	Vector availableDepts = new Vector();
	    	for (Iterator iter = list.iterator();iter.hasNext();) {
	    		Department d = (Department) iter.next();
	    		availableDepts.add(new LabelValueBean(d.getDeptCode() + "-" + d.getName(), d.getUniqueId().toString()));
	    	}
	    	request.setAttribute(Department.DEPT_ATTR_NAME, availableDepts);
	        
	        // Reset Form
	        if ("Back".equals(op)) {
	            if (myForm.getUniqueId()!=null)
	                request.setAttribute("hash", myForm.getUniqueId());
	            myForm.load(null);
	            myForm.setOp("List");
	        }
	        
            if ("Add Date Pattern".equals(op)) {
                myForm.load(null);
                myForm.setOp("Save");
            }

            if ("Add Department".equals(op)) {
	            ActionMessages errors = new ActionErrors();
				if (myForm.getDepartmentId()==null || myForm.getDepartmentId().longValue()<0)
					errors.add("department", new ActionMessage("errors.generic", "No department selected."));
				else {
					boolean contains = myForm.getDepartmentIds().contains(myForm.getDepartmentId());
					if (contains)
						errors.add("department", new ActionMessage("errors.generic", "Department already present in the list of departments."));
				}
	            if(errors.size()>0) {
	                saveErrors(request, errors);
	            } else {
	            	myForm.getDepartmentIds().add(myForm.getDepartmentId());
	            }
	            myForm.setOp(myForm.getUniqueId().longValue()<0?"Save":"Update");
	        }

	        if ("Remove Department".equals(op)) {
	            ActionMessages errors = new ActionErrors();
				if (myForm.getDepartmentId()==null || myForm.getDepartmentId().longValue()<0)
					errors.add("department", new ActionMessage("errors.generic", "No department selected."));
				else {
					boolean contains = myForm.getDepartmentIds().contains(myForm.getDepartmentId());
					if (!contains)
						errors.add("department", new ActionMessage("errors.generic", "Department not present in the list of departments."));
				}
	            if(errors.size()>0) {
	                saveErrors(request, errors);
	            } else {
	            	myForm.getDepartmentIds().remove(myForm.getDepartmentId());
	            }	
	            myForm.setOp(myForm.getUniqueId().longValue()<0?"Save":"Update");
	        }

	        // Add / Update
	        if ("Update".equals(op) || "Save".equals(op) || "Make Default".equals(op)) {
	            // Validate input
	            ActionMessages errors = myForm.validate(mapping, request);
	            if(errors.size()>0) {
	                saveErrors(request, errors);
	                myForm.setOp(myForm.getUniqueId().longValue()<0?"Save":"Update");
	            } else {
	        		Transaction tx = null;
	        		
	                try {
	                	org.hibernate.Session hibSession = (new TimePatternDAO()).getSession();
	                	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
	                		tx = hibSession.beginTransaction();
	                	
	                	DatePattern dp = myForm.saveOrUpdate(request, hibSession);
	                	if ("Make Default".equals(op)) {
	                		dp.getSession().setDefaultDatePattern(dp);
	                		hibSession.saveOrUpdate(dp.getSession());
	                		myForm.setIsDefault(true);
	                	}
	                	
                        ChangeLog.addChange(
                                hibSession, 
                                request, 
                                dp, 
                                ChangeLog.Source.DATE_PATTERN_EDIT, 
                                ("Save".equals(op)?ChangeLog.Operation.CREATE:ChangeLog.Operation.UPDATE), 
                                null, 
                                null);

                        if (tx!=null) tx.commit();
	        	    } catch (Exception e) {
	        	    	if (tx!=null) tx.rollback();
	        	    	throw e;
	        	    }

	                myForm.setOp("List");
	                if (myForm.getUniqueId()!=null)
	                    request.setAttribute("hash", myForm.getUniqueId());
	            }
	        }

	        // Edit
	        if("Edit".equals(op)) {
	            String id = request.getParameter("id");
	            ActionMessages errors = new ActionMessages();
	            if(id==null || id.trim().length()==0) {
	                errors.add("key", new ActionMessage("errors.invalid", "Unique Id : " + id));
	                saveErrors(request, errors);
	                request.setAttribute("DatePatterns.pattern", myForm.getDatePattern(request).getPatternHtml(true, myForm.getUniqueId()));
	                return mapping.findForward("list");
	            } else {
	            	DatePattern pattern = (new DatePatternDAO()).get(new Long(id));
	            	
	                if(pattern==null) {
	                    errors.add("name", new ActionMessage("errors.invalid", "Unique Id : " + id));
	                    saveErrors(request, errors);
	                    request.setAttribute("DatePatterns.pattern", myForm.getDatePattern(request).getPatternHtml(true, myForm.getUniqueId()));
	                    return mapping.findForward("list");
	                } else {
	                	myForm.load(pattern);
	                }
	            }
	        }

	        // Delete 
	        if("Delete".equals(op)) {
	    		Transaction tx = null;
	    		
	            try {
	            	org.hibernate.Session hibSession = (new TimePatternDAO()).getSession();
	            	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
	            		tx = hibSession.beginTransaction();
	            	
                    DatePattern dp = (new DatePatternDAO()).get(myForm.getUniqueId(), hibSession);
                    ChangeLog.addChange(
                            hibSession, 
                            request, 
                            dp, 
                            ChangeLog.Source.DATE_PATTERN_EDIT, 
                            ChangeLog.Operation.DELETE, 
                            null, 
                            null);

                    myForm.delete(hibSession);
	            	
	    			tx.commit();
	    	    } catch (Exception e) {
	    	    	if (tx!=null) tx.rollback();
	    	    	throw e;
	    	    }

	    	    myForm.load(null);
	            myForm.setOp("List");
	        }
	        
	        if ("Fix Generated".equals(op)) {
	    		Transaction tx = null;
	    		
            	PrintWriter out = null;

            	try {
	            	org.hibernate.Session hibSession = (new TimePatternDAO()).getSession();
	            	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
	            		tx = hibSession.beginTransaction();
	            	
	            	File file = ApplicationProperties.getTempFile("fix", "txt");
	            	out = new PrintWriter(new FileWriter(file));
	            	
	            	Session session = Session.getCurrentAcadSession(user);
	            	Vector allDatePatterns = DatePattern.findAll(session, true, null, null);
	            	for (Iterator i=allDatePatterns.iterator();i.hasNext();) {
	            		DatePattern dp = (DatePattern)i.next();
	            		if (!dp.getName().startsWith("generated")) continue;
                        
                        out.println("Checking "+dp.getName()+" ...");
	            		
	            		List classes =
           					hibSession.
           					createQuery("select distinct c from Class_ as c inner join c.datePattern as dp where dp.uniqueId=:uniqueId").
           					setLong("uniqueId", dp.getUniqueId().longValue()).list();
	            		
	            		List subparts = 
        					hibSession.
        					createQuery("select distinct s from SchedulingSubpart as s inner join s.datePattern as dp where dp.uniqueId=:uniqueId").
        					setLong("uniqueId", dp.getUniqueId().longValue()).list();
	            		
	            		Vector allClasses = new Vector(classes);
	            		
	            		for (Iterator j=subparts.iterator();j.hasNext();) {
	            			SchedulingSubpart s = (SchedulingSubpart)j.next();
	            			for (Iterator k=s.getClasses().iterator();k.hasNext();) {
	            				Class_ c = (Class_)k.next();
	            				if (c.getDatePattern()==null)
	            					allClasses.add(c);
	            			}
	            		}

	            		if (allClasses.isEmpty()) {
	            			out.println("  -- date pattern is not used -> delete");
	            			for (Iterator j=dp.getDepartments().iterator();j.hasNext();) {
	            				Department d = (Department)j.next();
	            				d.getDatePatterns().remove(dp);
	            				hibSession.saveOrUpdate(d);
	            			}
                            ChangeLog.addChange(
                                    hibSession, 
                                    request, 
                                    dp, 
                                    ChangeLog.Source.DATE_PATTERN_EDIT, 
                                    ChangeLog.Operation.DELETE, 
                                    null, 
                                    null);
	            			hibSession.delete(dp);
	            			continue;
	            		}

	            		for (Iterator j=allClasses.iterator();j.hasNext();) {
	            			Class_ clazz = (Class_)j.next();
	            			out.println("  -- used by "+clazz.getClassLabel());
	            		}
	            		
	            		TreeSet days = dp.getUsage(allClasses);

            			out.println("    -- "+days);
            			if (days.isEmpty()) {
            				int offset = dp.getPatternOffset();
        					for (int x=0;x<dp.getPattern().length();x++) {
                                if (dp.getPattern().charAt(x)!='1') continue;
       							days.add(new Integer(x+offset));
        					}
            			}

	            		DatePattern likeDp = null;
	            		int likeDiff = 0;
            			
            			for (Iterator j=allDatePatterns.iterator();j.hasNext();) {
    	            		DatePattern xdp = (DatePattern)j.next();
    	            		if (xdp.getName().startsWith("generated")) continue;
    	            		TreeSet xdays = xdp.getUsage(allClasses);
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
                                out.println("      -- like "+likeDp.getName()+", diff="+likeDiff);
                                out.println("      -- "+likeDp.getUsage(allClasses));
                                out.println("    -- transfering all classes and subparts from "+dp.getName()+" to "+likeDp.getName());
                                for (Iterator j=classes.iterator();j.hasNext();) {
                                    Class_ clazz = (Class_)j.next();
                                    clazz.setDatePattern(likeDp.isDefault()?null:likeDp);
                                    hibSession.saveOrUpdate(clazz);
                                }
                                for (Iterator j=subparts.iterator();j.hasNext();) {
                                    SchedulingSubpart subpart = (SchedulingSubpart)j.next();
                                    subpart.setDatePattern(likeDp.isDefault()?null:likeDp);
                                    hibSession.saveOrUpdate(subpart);
                                }
                                out.println("    -- deleting date pattern "+dp.getName());
                                for (Iterator j=dp.getDepartments().iterator();j.hasNext();) {
                                    Department d = (Department)j.next();
                                    d.getDatePatterns().remove(dp);
                                    hibSession.saveOrUpdate(d);
                                }
                                ChangeLog.addChange(
                                        hibSession, 
                                        request, 
                                        dp, 
                                        ChangeLog.Source.DATE_PATTERN_EDIT, 
                                        ChangeLog.Operation.DELETE, 
                                        null, 
                                        null);
                                hibSession.delete(dp);
                            } else {
                                out.println("      -- like "+likeDp.getName()+", diff="+likeDiff);
                                out.println("      -- "+likeDp.getUsage(allClasses));
                                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
                                dp.setName("generated "+sdf.format(dp.getStartDate())+" - "+sdf.format(dp.getEndDate()));
                                hibSession.saveOrUpdate(dp);
                            }
            			}
	            	}
	            	
	            	out.flush(); out.close(); out = null;
	            	request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
	        	
	    			tx.commit();
	    	    } catch (Exception e) {
	    	    	if (tx!=null) tx.rollback();
	    	    	throw e;
	    	    } finally {
	    	    	if (out!=null) out.close();
	    	    }
	    	    
	        	myForm.load(null);
	        	myForm.setOp("List");
	        }

	        if ("Push Up".equals(op)) {
	    		Transaction tx = null;
	    		
            	PrintWriter out = null;

            	try {
	            	org.hibernate.Session hibSession = (new TimePatternDAO()).getSession();
	            	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
	            		tx = hibSession.beginTransaction();
	            	
	            	File file = ApplicationProperties.getTempFile("push", "txt");
	            	out = new PrintWriter(new FileWriter(file));
	            	
	            	Session session = Session.getCurrentAcadSession(user);
	            	
	            	List subparts =
       					hibSession.
       					createQuery("select distinct c.schedulingSubpart from Class_ as c inner join c.datePattern as dp where dp.session.uniqueId=:sessionId").
       					setLong("sessionId", session.getUniqueId().longValue()).list();

	            	for (Iterator i=subparts.iterator();i.hasNext();) {
	            		SchedulingSubpart subpart = (SchedulingSubpart)i.next();
	            		
	            		out.println("Checking "+subpart.getSchedulingSubpartLabel()+" ...");
	            		
	            		boolean sameDatePattern = true;
	            		DatePattern dp = null;
	            		
	            		for (Iterator j=subpart.getClasses().iterator();j.hasNext();) {
	            			Class_ clazz = (Class_)j.next();
	            			if (clazz.getDatePattern()==null) {
	            				sameDatePattern=false; break;
	            			}
	            			if (dp==null)
	            				dp = clazz.getDatePattern();
	            			else if (!dp.equals(clazz.getDatePattern())) {
	            				sameDatePattern=false; break;
	            			}
	            		}
	            		
	            		if (!sameDatePattern) continue;
	            		
	            		out.println("  -- all classes share same date pattern "+dp.getName()+" --> pushing it to subpart");
	            		
	            		for (Iterator j=subpart.getClasses().iterator();j.hasNext();) {
	            			Class_ clazz = (Class_)j.next();
	            			clazz.setDatePattern(null);
	            			hibSession.saveOrUpdate(clazz);
	            		}
	            		subpart.setDatePattern(dp.isDefault()?null:dp);
	            		hibSession.saveOrUpdate(subpart);
                    }

	            	out.flush(); out.close(); out = null;
	            	request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
	        	
	    			tx.commit();
	    	    } catch (Exception e) {
	    	    	if (tx!=null) tx.rollback();
	    	    	throw e;
	    	    } finally {
	    	    	if (out!=null) out.close();
	    	    }
	    	    
	        	myForm.load(null);
	        	myForm.setOp("List");
	        }

	        if ("Assign Departments".equals(op)) {
	    		Transaction tx = null;
	    		
            	PrintWriter out = null;
            	HashSet refresh = new HashSet(); 

            	try {
	            	org.hibernate.Session hibSession = (new TimePatternDAO()).getSession();
	            	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
	            		tx = hibSession.beginTransaction();
	            	
	            	File file = ApplicationProperties.getTempFile("assigndept", "txt");
	            	out = new PrintWriter(new FileWriter(file));
	            	
	            	Session session = Session.getCurrentAcadSession(user);
	            	TreeSet allDatePatterns = new TreeSet(DatePattern.findAll(session, true, null, null));
	            	for (Iterator i=allDatePatterns.iterator();i.hasNext();) {
	            		DatePattern dp = (DatePattern)i.next();
	            		
	            		if (dp.getType().intValue()!=DatePattern.sTypeExtended) continue;
	            		
	            		out.println("Checking "+dp.getName()+" ...");
	            		
	            		List classes =
           					hibSession.
           					createQuery("select distinct c from Class_ as c inner join c.datePattern as dp where dp.uniqueId=:uniqueId").
           					setLong("uniqueId", dp.getUniqueId().longValue()).list();
	            		
	            		List subparts = 
        					hibSession.
        					createQuery("select distinct s from SchedulingSubpart as s inner join s.datePattern as dp where dp.uniqueId=:uniqueId").
        					setLong("uniqueId", dp.getUniqueId().longValue()).list();
	            		
	            		HashSet depts = new HashSet();
	            		
	            		for (Iterator j=classes.iterator();j.hasNext();) {
	            			Class_ c = (Class_)j.next();
	            			depts.add(c.getManagingDept());
	            		}
	            		
	            		for (Iterator j=subparts.iterator();j.hasNext();) {
	            			SchedulingSubpart s = (SchedulingSubpart)j.next();
	            			depts.add(s.getManagingDept());
	            		}
	            		
	            		out.println("  -- departments: "+depts);

	            		boolean added = false;
	            		for (Iterator j=depts.iterator();j.hasNext();) {
	            			Department d = (Department)j.next();
	            			if (d.isExternalManager().booleanValue()) {
	            				/*
	            				if (dp.getDepartments().contains(d)) {
	            					dp.getDepartments().remove(d);
	            					d.getDatePatterns().remove(dp);
	            					hibSession.saveOrUpdate(d);
	                				out.println("    -- department "+d+" removed from "+dp.getName());
	                				added=true;
	            				}*/
	            				continue;
	            			}
	            			if (!dp.getDepartments().contains(d)) {
	            				dp.getDepartments().add(d);
	            				d.getDatePatterns().add(dp);
	            				hibSession.saveOrUpdate(d);
	            				out.println("    -- department "+d+" added to "+dp.getName());
	            				added = true;
	            			}
	            		}
	            		if (added) {
	            			hibSession.saveOrUpdate(dp);
                            ChangeLog.addChange(
                                    hibSession, 
                                    request, 
                                    dp, 
                                    ChangeLog.Source.DATE_PATTERN_EDIT, 
                                    ChangeLog.Operation.UPDATE, 
                                    null, 
                                    null);
	            			refresh.add(dp);
	            		}
	            	}

	            	out.flush(); out.close(); out = null;
	            	request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
	        	
	    			tx.commit();
	    			
	    			for (Iterator i=refresh.iterator();i.hasNext();) {
	    				hibSession.refresh(i.next());
	    			}
	    			
	    	    } catch (Exception e) {
	    	    	if (tx!=null) tx.rollback();
	    	    	throw e;
	    	    } finally {
	    	    	if (out!=null) out.close();
	    	    }
	    	    
	        	myForm.load(null);
	        	myForm.setOp("List");
	        }

	        if ("Export CSV".equals(op)) {
	    		Transaction tx = null;
	    		
	    		try {
	            	org.hibernate.Session hibSession = (new TimePatternDAO()).getSession();
	            	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
	            		tx = hibSession.beginTransaction();
	            	
	            	CSVFile csv = new CSVFile();
	            	csv.setHeader(
	            			new CSVFile.CSVField[] {
	            					new CSVFile.CSVField("Name"),
	            					new CSVFile.CSVField("Type"),
	            					new CSVFile.CSVField("NrDays"),
	            					new CSVFile.CSVField("From"),
	            					new CSVFile.CSVField("To"),
	            					new CSVFile.CSVField("Dates"),
	            					new CSVFile.CSVField("Departments"),
	            					new CSVFile.CSVField("Classes")
	            			});
	            	
	            	Session session = Session.getCurrentAcadSession(user);
	            	SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
	            	TreeSet allDatePatterns = new TreeSet(DatePattern.findAll(session, true, null, null));
	            	for (Iterator i=allDatePatterns.iterator();i.hasNext();) {
	            		DatePattern dp = (DatePattern)i.next();
	            		
	            		List classes =
           					hibSession.
           					createQuery("select distinct c from Class_ as c inner join c.datePattern as dp where dp.uniqueId=:uniqueId").
           					setLong("uniqueId", dp.getUniqueId().longValue()).list();
	            		
	            		List subparts = 
        					hibSession.
        					createQuery("select distinct s from SchedulingSubpart as s inner join s.datePattern as dp where dp.uniqueId=:uniqueId").
        					setLong("uniqueId", dp.getUniqueId().longValue()).list();
	            		
	            		TreeSet allClasses = new TreeSet(new ClassComparator(ClassComparator.COMPARE_BY_HIERARCHY));
	            		allClasses.addAll(classes);
	            		
	            		for (Iterator j=subparts.iterator();j.hasNext();) {
	            			SchedulingSubpart s = (SchedulingSubpart)j.next();
	            			for (Iterator k=s.getClasses().iterator();k.hasNext();) {
	            				Class_ c = (Class_)k.next();
	            				if (c.getDatePattern()==null)
	            					allClasses.add(c);
	            			}
	            		}

	                	String deptStr = "";
	                	TreeSet depts = new TreeSet(dp.getDepartments()); 
	                	for (Iterator j=depts.iterator();j.hasNext();) {
	                		Department d = (Department)j.next();
	                		deptStr += d.getShortLabel().trim();
	                		if (j.hasNext()) { deptStr += ", "; }
	                	}
	                	
	                	String classStr = "";
	                	for (Iterator j=allClasses.iterator();j.hasNext();) {
	                		Class_ clazz = (Class_)j.next();
	                		classStr += clazz.getClassLabel();
	                		if (j.hasNext()) { classStr += ", "; }
	                	}
	                	
	                	csv.addLine(
	                			new CSVFile.CSVField[] {
		            					new CSVFile.CSVField(dp.getName()),
		            					new CSVFile.CSVField(DatePattern.sTypes[dp.getType().intValue()]),
		            					new CSVFile.CSVField(String.valueOf(dp.size())),
		            					new CSVFile.CSVField(sdf.format(dp.getStartDate())),
		            					new CSVFile.CSVField(sdf.format(dp.getEndDate())),
		            					new CSVFile.CSVField(dp.getPatternString()),
		            					new CSVFile.CSVField(deptStr),
		            					new CSVFile.CSVField(classStr)
		            			});
	                	
	                	/*
	                	System.out.println(
	                			"insert into date_pattern (uniqueid, name, pattern, offset, type, visible, session_id) values (" +
	                			"DATE_PATTERN_SEQ.Nextval, "+
	                			"'"+dp.getName()+"', "+
	                			"'"+dp.getPattern()+"', "+
	                			dp.getOffset()+", "+
	                			dp.getType()+", "+
	                			(dp.isVisible().booleanValue()?1:0)+", "+
	                			dp.getSession().getUniqueId()+");");
						*/
	            	}
	            	
	            	File file = ApplicationProperties.getTempFile("datePatterns", "csv");
	           		csv.save(file);
	           		request.setAttribute(Constants.REQUEST_OPEN_URL, "temp/"+file.getName());
	    			tx.commit();
	    	    } catch (Exception e) {
	    	    	if (tx!=null) tx.rollback();
	    	    	throw e;
	    	    }
	    	    
	        	myForm.load(null);
	        	myForm.setOp("List");
	        }

	        if ("List".equals(myForm.getOp())) {
	            // Read all existing settings and store in request
	            getDatePatterns(request);
	            return mapping.findForward("list");
	        } 
	        
	        request.setAttribute("DatePatterns.pattern", myForm.getDatePattern(request).getPatternHtml(true, myForm.getUniqueId()));
	        return mapping.findForward(myForm.getUniqueId().longValue()<0?"add":"edit");
	        
		} catch (Exception e) {
			Debug.error(e);
			throw e;
		}
	}

    private void getDatePatterns(HttpServletRequest request) throws Exception {
		WebTable.setOrder(request.getSession(),"datePatterns.ord",request.getParameter("ord"),1);
		// Create web table instance 
        WebTable webTable = new WebTable( 5,
			    null, "datePatternEdit.do?ord=%%",
			    new String[] {"Name", "Type", "Used", "Dates", "Departments"},
			    new String[] {"left", "left", "left", "left", "left"},
			    null );
        
        Vector patterns = DatePattern.findAll(request, null, null);
		if(patterns.isEmpty()) {
		    webTable.addLine(null, new String[] {"No date pattern defined for this session."}, null, null );			    
		}
		
    	User user = Web.getUser(request.getSession());
    	Session session = Session.getCurrentAcadSession(user);
		Set used = DatePattern.findAllUsed(session.getUniqueId());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MM/dd");

        for (Enumeration e=patterns.elements();e.hasMoreElements();) {
        	DatePattern pattern = (DatePattern)e.nextElement();
        	String onClick = "onClick=\"document.location='datePatternEdit.do?op=Edit&id=" + pattern.getUniqueId() + "';\"";
        	String deptStr = "";
        	String deptCmp = "";
        	TreeSet depts = new TreeSet(pattern.getDepartments()); 
        	for (Iterator i=depts.iterator();i.hasNext();) {
        		Department d = (Department)i.next();
        		deptStr += d.getManagingDeptAbbv().trim();
        		deptCmp += d.getDeptCode();
        		if (i.hasNext()) { deptStr += ", "; deptCmp += ","; }
        	}
            String pattStr = pattern.getPatternString();
            if (pattern.getName().startsWith("generated")) {
                int first = pattern.getPattern().indexOf('1') - pattern.getOffset().intValue();
                int last = pattern.getPattern().lastIndexOf('1') - pattern.getOffset().intValue();

                DatePattern likeDp = null;
                int likeDiff = 0;
                
                for (Iterator j=patterns.iterator();j.hasNext();) {
                    DatePattern xdp = (DatePattern)j.next();
                    if (xdp.getName().startsWith("generated")) continue;
                    int xfirst = xdp.getPattern().indexOf('1') - xdp.getOffset().intValue();
                    int xlast = xdp.getPattern().lastIndexOf('1') - xdp.getOffset().intValue();
                    int diff = Math.abs(first-xfirst) + Math.abs(last-xlast);
                    if (likeDp==null || likeDiff>diff || (likeDiff==diff && xdp.isDefault())) {
                        likeDp = xdp; likeDiff = diff;
                    }
                }
                
                if (likeDp!=null) {
                    int xfirst = likeDp.getPattern().indexOf('1') - likeDp.getOffset().intValue();
                    int xlast = likeDp.getPattern().lastIndexOf('1') - likeDp.getOffset().intValue();
                    int firstDiff = first - xfirst;
                    int lastDiff = last - xlast;
                    if (Math.abs(lastDiff)>3 || Math.abs(firstDiff)>3) pattStr += "<b>";
                    pattStr += "<br>Similar to "+likeDp.getName()+" (offset "+firstDiff+" and "+lastDiff+" days)";
                    pattStr += "<br>"+sdf.format(pattern.getStartDate())+"-"+sdf.format(pattern.getEndDate())+" versus "+sdf.format(likeDp.getStartDate())+"-"+sdf.format(likeDp.getEndDate());
                    if (Math.abs(lastDiff)>3 || Math.abs(firstDiff)>3) pattStr += "</b>";
                }
            }
        	boolean isUsed = used.contains(pattern) || pattern.isDefault();
        	webTable.addLine(onClick, new String[] {
        	        (pattern.isDefault()?"<B>":"")+
        	        (pattern.isVisible()?"":"<font color='gray'>")+
        	        "<a name='"+pattern.getUniqueId()+"'>"+
        	            pattern.getName().replaceAll(" ","&nbsp;")+
        	        "</a>"+
        	        (pattern.isVisible()?"":"</font>")+
        	        (pattern.isDefault()?"</B>":""),
        	        (pattern.isVisible()?"":"<font color='gray'>")+
        			    DatePattern.sTypes[pattern.getType().intValue()].replaceAll(" ","&nbsp;")+
        			(pattern.isVisible()?"":"</font>"),
        			(isUsed?"<IMG border='0' title='This date pattern is being used.' alt='Default' align='absmiddle' src='images/tick.gif'>":""),
        			(pattern.isVisible()?"":"<font color='gray'>")+pattStr+(pattern.isVisible()?"":"</font>"),
        			(pattern.isVisible()?"":"<font color='gray'>")+deptStr+(pattern.isVisible()?"":"</font>")
        		},new Comparable[] {
        			pattern.getName(),
        			pattern.getType(),
        			(isUsed?"0":"1"),
        			pattStr,
        			deptCmp
        		});
        }
        
	    request.setAttribute("DatePatterns.table", webTable.printTable(WebTable.getOrder(request.getSession(),"datePatterns.ord")));
    }	
}

