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
package org.unitime.timetable.webutil;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.upload.FormFile;
import org.unitime.commons.Email;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Event;
import org.unitime.timetable.model.EventNote;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.Event.MultiMeeting;
import org.unitime.timetable.util.Constants;

public class EventEmail {
    private static Log sLog = LogFactory.getLog(EventEmail.class);
    private Event iEvent = null;
    private TreeSet<MultiMeeting> iMeetings = null;
    private String iNote = null;
    private int iAction = sActionCreate;
    private FormFile iAttachement = null;
    
    public static final int sActionCreate = 0;
    public static final int sActionApprove = 1;
    public static final int sActionReject = 2;
    public static final int sActionAddMeeting = 3;
    public static final int sActionUpdate = 4;
    public static final int sActionDelete = 5;
    public static final int sActionInquire = 6;
    
    public EventEmail(Event event, int action, TreeSet<MultiMeeting> meetings, String note, FormFile attachement) {
        iEvent = event;
        iAction = action;
        iMeetings = meetings;
        iNote = note;
        iAttachement = attachement;
    }
    
    public void send(HttpServletRequest request) {
        String subject = null;
        File conf = null;
        try {
            User user = Web.getUser(request.getSession());
            if (Roles.ADMIN_ROLE.equals(user.getRole()) || Roles.EVENT_MGR_ROLE.equals(user.getRole())) {
                if (iAction!=sActionReject && iAction!=sActionApprove && iAction!=sActionInquire) return;
            }
            
            switch (iAction) {
            case sActionCreate : 
                subject = "Event "+iEvent.getEventName()+" created.";
                break;
            case sActionApprove :
                subject = "Event "+iEvent.getEventName()+" approved.";
                break;
            case sActionReject :
                subject = "Event "+iEvent.getEventName()+" rejected.";
                break;
            case sActionUpdate :
                subject = "Event "+iEvent.getEventName()+" updated.";
                break;
            case sActionAddMeeting :
                subject = "Event "+iEvent.getEventName()+" updated (one or more meetings added).";
                break;
            case sActionDelete : 
                subject = "Event "+iEvent.getEventName()+" updated (one or more meetings deleted).";
                break;
            case sActionInquire : 
                subject = "Event "+iEvent.getEventName()+" inquiry.";
                break;
            }

            if (!"true".equals(ApplicationProperties.getProperty("unitime.email.confirm.event", ApplicationProperties.getProperty("tmtbl.event.confirmationEmail","true")))) {
                request.getSession().setAttribute(Constants.REQUEST_MSSG, "Confirmation emails are disabled.");
                return;
            }
            
            String message = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">";
            message += "<html><head>";
            message += "<title>"+subject+"</title>";
            message += "<meta http-equiv='Content-Type' content='text/html; charset=windows-1250'>";
            message += "<meta name='Author' content='UniTime LLC'>";
            message += "<style type='text/css'>";
            message += "<!--" +
            		"A:link     { color: blue; text-decoration: none; }" +
            		"A:visited  { color: blue; text-decoration: none; }" +
            		"A:active   { color: blue; text-decoration: none; }" +
            		"A:hover    { color: blue; text-decoration: none; }" +
            		"-->";
            message += "</style></head><body bgcolor='#ffffff' style='font-size: 10pt; font-family: arial;'>";
            message += "<table border='0' width='100%' align='center' cellspacing='10'>";
            
            message += "<tr><td colspan='2' style='border-bottom: 2px #2020FF solid;'><font size='+2'>";
            message += iEvent.getEventName();
            message += "</td></tr>";
            message += "<tr><td>Event Type</td><td>"+iEvent.getEventTypeLabel()+"</td></tr>";
            if (iEvent.getMinCapacity()!=null) {
                message += "<tr><td>"+(iEvent.getEventType()==Event.sEventTypeSpecial?"Expected Size":"Event Capacity")+":</td>";
                if (iEvent.getMaxCapacity()==null || iEvent.getMinCapacity().equals(iEvent.getMaxCapacity())) {
                    message += "<td>"+iEvent.getMinCapacity()+"</td>";
                } else {
                    message += "<td>"+iEvent.getMinCapacity()+" - "+iEvent.getMaxCapacity()+"</td>";
                }
            }
            if (iEvent.getSponsoringOrganization()!=null) {
                message += "<tr><td>Sponsoring Organization</td><td>"+iEvent.getSponsoringOrganization().getName()+"</td></tr>";
            }
            if (iEvent.getMainContact()!=null) {
                message += "<tr><td>Main Contact</td><td>";
                message += "<a href='mailto:"+iEvent.getMainContact().getEmailAddress()+"'>";
                if (iEvent.getMainContact().getLastName()!=null)
                    message += iEvent.getMainContact().getLastName();
                if (iEvent.getMainContact().getFirstName()!=null)
                    message += ", "+iEvent.getMainContact().getFirstName();
                if (iEvent.getMainContact().getMiddleName()!=null && iEvent.getMainContact().getMiddleName().length()>0)
                    message += ", "+iEvent.getMainContact().getMiddleName();
                message += "</a></td></tr>";
            }
            
            if (!iMeetings.isEmpty()) {
                message += "<tr><td colspan='2' style='border-bottom: 1px #2020FF solid; font-variant:small-caps;'>";
                message += "<br><font size='+1'>";
                switch (iAction) {
                case sActionCreate : 
                    message += "Following meetings were requested by you or on your behalf, confirmation will follow";
                    break;
                case sActionApprove :
                    message += "Following meetings were approved";
                    break;
                case sActionReject :
                    message += "Following meetings were rejected";
                    if (iNote!=null && iNote.length()>0) message += " (see the note below for more details)";
                    break;
                case sActionAddMeeting :
                    message += "Following meetings were added by you or on your behalf, confirmation will follow";
                    break;
                case sActionDelete :
                    message += "Following meetings were deleted by you or on your behalf";
                    break;
                case sActionInquire :
                    message += "Following meetings are in question";
                    break;
                }
                message += "</font>";
                message += "</td></tr><tr><td colspan='2'>";
                message += "<table border='0' width='100%'>";
                message += "<tr><td><i>Date</i></td><td><i>Time</i></td><td><i>Location</i></td></tr>";
                for (MultiMeeting m : iMeetings) {
                    message += "<tr><td>";
                    message += m.getDays()+" "+new SimpleDateFormat("MM/dd/yyyy").format(m.getMeetings().first().getMeetingDate());
                    message += (m.getMeetings().size()>1?" - "+new SimpleDateFormat("MM/dd/yyyy").format(m.getMeetings().last().getMeetingDate()):"");
                    message += "</td><td>";
                    message += m.getMeetings().first().startTime()+" - "+m.getMeetings().first().stopTime();
                    message += "</td><td>";
                    message += (m.getMeetings().first().getLocation()==null?"":" "+m.getMeetings().first().getLocation().getLabel());
                    message += "</td></tr>";
                }
                message += "</table></td></tr>";
            }
            
            if (iNote!=null && iNote.length()>0) {
                message += "<tr><td colspan='2' style='border-bottom: 1px #2020FF solid; font-variant:small-caps;'>";
                message += "<br><font size='+1'>" + (iAction == sActionInquire ? "Inquiry" : "Notes" ) + "</font>";
                message += "</td></tr><tr><td colspan='2' >";
                message += iNote.replaceAll("\n", "<br>");
                message += "</td></tr>";
            }
            
            if (iAction!=sActionCreate) {
                message += "<tr><td colspan='2' style='border-bottom: 1px #2020FF solid; font-variant:small-caps;'>";
                message += "<br><font size='+1'>History of "+iEvent.getEventName()+"</font>";
                message += "</td></tr>";
                if (iEvent.getMeetings().isEmpty()) {
                    message += "<tr><td colspan='2' style='background-color:';>";
                    message += "No meeting left, the event "+iEvent.getEventName()+" was deleted as well.";
                    message += "</td></tr>";
                } else {
                    message += "<tr><td colspan='2'>";
                    message += "<table border='0' width='100%'>";
                    message += "<tr><td><i>Date</i></td><td><i>Time</i></td><td><i>Location</i></td><td><i>Capacity</i></td><td><i>Approved</i></td></tr>";
                    for (MultiMeeting m : iEvent.getMultiMeetings()) {
                        message += "<tr><td>";
                        message += m.getDays()+" "+new SimpleDateFormat("MM/dd/yyyy").format(m.getMeetings().first().getMeetingDate());
                        message += (m.getMeetings().size()>1?" - "+new SimpleDateFormat("MM/dd/yyyy").format(m.getMeetings().last().getMeetingDate()):"");
                        message += "</td><td>";
                        message += m.getMeetings().first().startTime()+" - "+m.getMeetings().first().stopTime();
                        message += "</td><td>";
                        message += (m.getMeetings().first().getLocation()==null?"":" "+m.getMeetings().first().getLocation().getLabel());
                        message += "</td><td>";
                        message += (m.getMeetings().first().getLocation()==null?"":" "+m.getMeetings().first().getLocation().getCapacity());
                        message += "</td><td>";
                        if (m.isPast()) {
                            message += "";
                        } else if (m.getMeetings().first().getApprovedDate()==null) {
                            message += "<i>Waiting Approval</i>";
                        } else {
                            message += new SimpleDateFormat("MM/dd/yyyy").format(m.getMeetings().first().getApprovedDate());
                        }
                        message += "</td></tr>";
                    }
                    message += "</table></td></tr>";
                }
            
                message += "<tr><td colspan='2' style='border-bottom: 1px #2020FF solid; font-variant:small-caps;'>";
                message += "<br><font size='+1'>All Notes of "+iEvent.getEventName()+"</font>";
                message += "</td></tr><tr><td colspan='2'>";
                message += "<table border='0' width='100%' cellspacing='0' cellpadding='3'>";
                message += "<tr><td><i>Date</i></td><td><i>Action</i></td><td><i>Meetings</i></td><td><i>Note</i></td></tr>";
                for (EventNote note : new TreeSet<EventNote>(iEvent.getNotes())) {
                    message += "<tr style=\"background-color:"+EventNote.sEventNoteTypeBgColor[note.getNoteType()]+";\" valign='top'>";
                    message += "<td>"+new SimpleDateFormat("MM/dd/yyyy hh:mmaa").format(note.getTimeStamp())+"</td>";
                    message += "<td>"+EventNote.sEventNoteTypeName[note.getNoteType()]+"</td>";
                    message += "<td>"+note.getMeetingsHtml()+"</td>";
                    message += "<td>"+(note.getTextNote()==null?"":note.getTextNote().replaceAll("\n", "<br>"))+"</td>";
                    message += "</tr>";
                }
                message += "</table></td></tr>";
            }
            
            message += "<tr><td colspan='2'>&nbsp;</td></tr>";
            message += "<tr><td colspan='2' style='border-top: 1px #2020FF solid;' align='center'>";
            message += "This email was automatically generated at ";
            message += request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();
            message += ",<br>by UniTime "+Constants.VERSION+"."+Constants.BLD_NUMBER.replaceAll("@build.number@", "?");
            message += " (University Timetabling Application, http://www.unitime.org).";
            message += "</td></tr></table>";
            message += "</body></html>";
            
            Email mail = new Email();
            mail.setSubject(subject);
            
            mail.setHTML(message);

            conf = ApplicationProperties.getTempFile("email", "html");
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new FileWriter(conf));
                pw.println(message);
                pw.flush(); pw.close(); pw=null;
            } catch (Exception e) {}
            finally { if (pw!=null) pw.close(); }
            
            String to = "";
            if (iEvent.getMainContact()!=null && iEvent.getMainContact().getEmailAddress()!=null) {
                mail.addRecipient(iEvent.getMainContact().getEmailAddress(),iEvent.getMainContact().getName());
                to = "<a href='mailto:"+iEvent.getMainContact().getEmailAddress()+"'>"+iEvent.getMainContact().getShortName()+"</a>";
            }
            if (iEvent.getEmail()!=null && iEvent.getEmail().length()>0) {
                for (StringTokenizer stk = new StringTokenizer(iEvent.getEmail(),";:,\n\r\t");stk.hasMoreTokens();) {
                    String email = stk.nextToken();
                    mail.addRecipientCC(email, null);
                    if (to.length()>0) to+=", ";
                    to += email;
                }
            }
            if (iEvent.getSponsoringOrganization()!=null && iEvent.getSponsoringOrganization().getEmail()!=null && iEvent.getSponsoringOrganization().getEmail().length()>0) {
                mail.addRecipient(iEvent.getSponsoringOrganization().getEmail(),iEvent.getSponsoringOrganization().getName());
                if (to.length()>0) to+=", ";
                to += "<a href='mailto:"+iEvent.getSponsoringOrganization().getEmail()+"'>"+iEvent.getSponsoringOrganization().getName()+"</a>";
            }
            
            if (iAttachement != null && iAttachement.getFileSize() > 0)
            	mail.addAttachement(iAttachement);
            
            mail.send();
            
            request.getSession().setAttribute(Constants.REQUEST_MSSG, 
                    (conf==null || !conf.exists()?"":"<a class='noFancyLinks' href='temp/"+conf.getName()+"'>")+
                    subject+" Confirmation email sent to "+to+"."+
                    (conf==null || !conf.exists()?"":"</a>"));
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            request.getSession().setAttribute(Constants.REQUEST_WARN,
                    (conf==null || !conf.exists()?"":"<a class='noFancyLinks' href='temp/"+conf.getName()+"'>")+
                    (subject==null?"":subject+" ")+"Unable to send confirmation email, reason: "+e.getMessage()+
                    (conf==null || !conf.exists()?"":"</a>"));
        }
    }
}
