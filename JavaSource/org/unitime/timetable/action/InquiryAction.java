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
package org.unitime.timetable.action;

import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;
import org.springframework.stereotype.Service;
import org.unitime.commons.Debug;
import org.unitime.commons.Email;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.InquiryForm;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.EventContact;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SolverGroup;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.LookupTables;


/** 
 * @author Tomas Muller
 */
@Service("/inquiry")
public class InquiryAction extends Action {

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			
			InquiryForm myForm = (InquiryForm) form;
	        MessageResources rsc = getResources(request);
			
	        // Check Access
	        if (!Web.isLoggedIn( request.getSession())) {
	            throw new Exception ("Access Denied.");
	        }
			
	        // Read operation to be performed
	        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));
	        
            User user = Web.getUser(request.getSession());
            TimetableManager mgr = TimetableManager.getManager(user);
            myForm.setNoRole(mgr==null);
	        
	        if ("Cancel".equals(op) || "Back".equals(op)) {
	        	return mapping.findForward("submit");
	        }
	        
    		ActionMessages errors = null;
    		
	        if (op!=null && op.equals(rsc.getMessage("button.insertAddress"))) {
	        	if (myForm.getPuid()!=null && myForm.getPuid().length()>0) {
	        		myForm.addToCarbonCopy(myForm.getPuid());
	        		myForm.setPuid(null);
	        	}
	        	else {
	        		errors = new ActionMessages();
	        		errors.add("puid", 
	        					new ActionMessage("errors.generic",	"Recipient has an invalid email address."));
	                saveErrors(request, errors);
	        	}
	        }
	        
	        if (request.getParameter("deleteId")!=null && request.getParameter("deleteId").length()>0) {
	        	try {
	        		int deleteId = Integer.parseInt(request.getParameter("deleteId"));
	        		myForm.removeCarbonCopy(deleteId);
	        		myForm.setPuid(null);
	        	}
	        	catch(Exception e) {
	        		errors = new ActionMessages();
	        		errors.add("puid", 
	        					new ActionMessage("errors.generic",	"Invalid email address."));
	                saveErrors(request, errors);
	        	}
	        }
	        
	        if ("Submit".equals(op)) {
	            errors = myForm.validate(mapping, request);
	            if(errors.size()>0) {
	                saveErrors(request, errors);
	            } else {
	            	Session session = Session.getCurrentAcadSession(user);
	            	
	            	String mail = myForm.getMessage();;
	            	mail += "\r\n";
	            	mail += "\r\n";
	            	mail += "User info -------------- \r\n";
	            	mail += "User: "+user.getName()+"\r\n";
	            	mail += "Login: "+user.getLogin()+"\r\n";
	            	//mail += "PUID: "+user.getId()+"\r\n";
	            	mail += "Role: "+user.getCurrentRole()+"\r\n";
	            	mail += "Departments: "+user.getDepartments()+"\r\n";
	            	mail += "\r\n";
	            	if (mgr!=null) {
	            	    mail += "Manager info -------------- \r\n";
	            	    mail += "Name: "+mgr.getName()+"\r\n";
	            	//    mail += "PUID: "+mgr.getPuid()+"\r\n";
	            	    mail += "Email: "+mgr.getEmailAddress()+"\r\n";
	            	    mail += "\r\n";
	            	}
	            	if (session!=null) {
	            	    mail += "Session info -------------- \r\n";
	            	    mail += "Session Term: "+session.getAcademicYearTerm()+"\r\n";
	            	    mail += "Session Initiative: "+session.getAcademicInitiative()+"\r\n";
	            	    if (mgr!=null) {
	                        mail += "Departments: \r\n";
	                        for (Iterator i=mgr.getDepartments().iterator();i.hasNext();) {
	                            Department d = (Department)i.next();
	                            if (!session.equals(d.getSession())) continue;
	                            mail += "  "+d.getLabel()+"\r\n";
	                        }
	                        mail += "Solver Groups: \r\n";
	                        for (Iterator i=mgr.getSolverGroups().iterator();i.hasNext();) {
	                            SolverGroup g = (SolverGroup)i.next();
	                            if (!session.equals(g.getSession())) continue;
	                            mail += "  "+g.getName()+"\r\n";
	                        }
	                        mail += "Subject Areas: \r\n";
	                        for (Iterator i=TimetableManager.getSubjectAreas(user).iterator();i.hasNext();) {
	                            SubjectArea sa = (SubjectArea)i.next();
	                            if (!session.equals(sa.getSession())) continue;
	                            mail += "  "+sa.getSubjectAreaAbbreviation()+"\r\n";
	                        }
	            	    }
	            	}
	            	mail += "\r\n";
	            	mail += "Application info -------------- \r\n";
	            	mail += "Version: "+Constants.getVersion()+" ("+Constants.getReleaseDate()+")\r\n";
	            	mail += "TimeStamp: "+(new Date());
	            	
	            	
                    EventContact c = EventContact.findByExternalUniqueId(user.getId());
	            	
                    Email email = new Email();
                    email.setSubject("UniTime ("+myForm.getTypeMsg(myForm.getType())+"): "+myForm.getSubject());
                    email.setText(mail);
                    
	            	if (!myForm.getCarbonCopy().isEmpty()) {
	            		for (Iterator i=myForm.getCarbonCopy().iterator(); i.hasNext(); ) {
	            			email.addRecipientCC((String)i.next(), null);
	            		}
	            	}
                    
                    if (ApplicationProperties.getProperty("unitime.email.inquiry") != null)
                    	email.addRecipient(ApplicationProperties.getProperty("unitime.email.inquiry"), ApplicationProperties.getProperty("unitime.email.inquiry.name"));
                    else
                    	email.addNotify();
                    
                    boolean autoreply = "true".equals(ApplicationProperties.getProperty("unitime.email.inquiry.autoreply", ApplicationProperties.getProperty("tmtbl.inquiry.autoreply", "false")));
                    
                    if (!autoreply) {
                        if (mgr != null && mgr.getEmailAddress() != null) {
                            email.addRecipientCC(mgr.getEmailAddress(), mgr.getName());
                        } else if (c != null && c.getEmailAddress() != null) {
                        	email.addRecipientCC(c.getEmailAddress(), c.getName());
                        } else {
                        	email.addRecipientCC(user.getLogin() + ApplicationProperties.getProperty("unitime.email.inquiry.suffix", ApplicationProperties.getProperty("tmtbl.inquiry.email.suffix","@unitime.org")), null);
                        }
                        email.send();
                    }
                    
                    if (autoreply) {
            		
                    	try {
    	            		mail = "The following inquiry was submitted on your behalf. " +
	    	            		"We will contact you soon. "+
	            				"This email was automatically generated, please do not reply.\n\n";
                        
    	            		if (ApplicationProperties.getProperty("tmtbl.inquiry.sender.name")!=null) {
                                mail += "Thank you, \n\n"+ApplicationProperties.getProperty("tmtbl.inquiry.sender.name")+"\n\n";
                            }
                            
                            mail +=
                                "-- INQUIRY ("+myForm.getTypeMsg(myForm.getType())+"): "+myForm.getSubject()+" ---------- \n\n"+
                				myForm.getMessage()+"\n"+
                				"-- END INQUIRY -------------------------------------------";
                            
                            email = new Email();
                            email.setSubject("RE: UniTime ("+myForm.getTypeMsg(myForm.getType())+"): "+myForm.getSubject());
                            email.setText(mail);
                            
                            if (mgr != null && mgr.getEmailAddress() != null) {
                                email.addRecipient(mgr.getEmailAddress(), mgr.getName());
                            } else if (c != null && c.getEmailAddress() != null) {
                            	email.addRecipient(c.getEmailAddress(), c.getName());
                            } else {
                            	email.addRecipient(user.getLogin() + ApplicationProperties.getProperty("unitime.email.inquiry.suffix", ApplicationProperties.getProperty("tmtbl.inquiry.email.suffix","@unitime.org")), null);
                            }
                            
                            email.send();
                    	} catch (Exception e) {}
	            	}
	            	
	            	myForm.setOp("Sent");
	            	return mapping.findForward("display");
	            }
	        }
	        
            LookupTables.setupTimetableManagers(request);
	        myForm.updateMessage();
	        return mapping.findForward("display");
		} catch (Exception e) {
			Debug.error(e);
			throw e;
		}
	}
	
}
