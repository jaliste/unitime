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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.unitime.commons.web.Web;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.ManageSolversForm;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SolverGroup;
import org.unitime.timetable.model.SolverPredefinedSetting;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.SolverGroupDAO;
import org.unitime.timetable.model.dao.SolverPredefinedSettingDAO;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.remote.RemoteSolverServerProxy;
import org.unitime.timetable.solver.remote.SolverRegisterService;
import org.unitime.timetable.solver.studentsct.StudentSolverProxy;
import org.unitime.timetable.solver.ui.PropertiesInfo;
import org.unitime.timetable.util.Constants;


/** 
 * @author Tomas Muller
 */
public class ManageSolversAction extends Action {
	private static SimpleDateFormat sDF = new SimpleDateFormat("MM/dd/yy hh:mmaa");

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ManageSolversForm myForm = (ManageSolversForm) form;

        // Check Access
        if (!Web.isLoggedIn( request.getSession() )
                || !Web.hasRole(request.getSession(), Roles.getAdminRoles()) ) {
             throw new Exception ("Access Denied.");
         }

        // Read operation to be performed
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));
        
        if ("Select".equals(op) && request.getParameter("puid")!=null) {
        	String puid = request.getParameter("puid");
        	request.getSession().setAttribute("ManageSolver.puid", puid);
        	request.getSession().removeAttribute("SolverProxy");
        	return mapping.findForward("showSolver");
        }
        
        if ("Unload".equals(op) && request.getParameter("puid")!=null) {
        	String puid = request.getParameter("puid");
        	request.getSession().setAttribute("ManageSolver.puid", puid);
        	request.getSession().removeAttribute("SolverProxy");
        	WebSolver.removeSolver(request.getSession());
        }

        if ("Select".equals(op) && request.getParameter("examPuid")!=null) {
            String puid = request.getParameter("examPuid");
            request.getSession().setAttribute("ManageSolver.examPuid", puid);
            request.getSession().removeAttribute("ExamSolverProxy");
            return mapping.findForward("showExamSolver");
        }

        if ("Unload".equals(op) && request.getParameter("examPuid")!=null) {
            String puid = request.getParameter("examPuid");
            request.getSession().setAttribute("ManageSolver.examPuid", puid);
            request.getSession().removeAttribute("ExamSolverProxy");
            WebSolver.removeExamSolver(request.getSession());
        }

        if ("Select".equals(op) && request.getParameter("sectionPuid")!=null) {
            String puid = request.getParameter("sectionPuid");
            request.getSession().setAttribute("ManageSolver.sectionPuid", puid);
            request.getSession().removeAttribute("StudentSolverProxy");
            return mapping.findForward("showStudentSolver");
        }

        if ("Unload".equals(op) && request.getParameter("sectionPuid")!=null) {
            String puid = request.getParameter("sectionPuid");
            request.getSession().setAttribute("ManageSolver.sectionPuid", puid);
            request.getSession().removeAttribute("StudentSolverProxy");
            WebSolver.removeStudentSolver(request.getSession());
        }
        
        if ("Deselect".equals(op)) {
        	request.getSession().removeAttribute("ManageSolver.puid");
        	request.getSession().removeAttribute("SolverProxy");
            request.getSession().removeAttribute("ManageSolver.examPuid");
            request.getSession().removeAttribute("ExamSolverProxy");
            request.getSession().removeAttribute("ManageSolver.sectionPuid");
            request.getSession().removeAttribute("StudentSolverProxy");
        }
        
        if ("Shutdown".equals(op)) {
        	String solverName = request.getParameter("solver");
        	if (solverName!=null) {
                Set servers = SolverRegisterService.getInstance().getServers();
                synchronized (servers) {
                    for (Iterator i=servers.iterator();i.hasNext();) {
                        RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                        if (solverName.equals(server.toString())) {
                            server.shutdown();
                            break;
                        }
                    }
    			}
        	}
        }
        
        if ("Kill".equals(op)) {
            String solverName = request.getParameter("solver");
            if (solverName!=null) {
                Set servers = SolverRegisterService.getInstance().getServers();
                synchronized (servers) {
                    for (Iterator i=servers.iterator();i.hasNext();) {
                        RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                        if (solverName.equals(server.toString())) {
                            server.kill();
                            break;
                        }
                    }
                }
            }
        }

        if ("Start Using".equals(op)) {
            String solverName = request.getParameter("solver");
            if (solverName!=null) {
                Set servers = SolverRegisterService.getInstance().getServers();
                synchronized (servers) {
                    for (Iterator i=servers.iterator();i.hasNext();) {
                        RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                        if (solverName.equals(server.toString())) {
                            server.startUsing();
                            break;
                        }
                    }
                }
            }
        }

        if ("Stop Using".equals(op)) {
            String solverName = request.getParameter("solver");
            if (solverName!=null) {
                Set servers = SolverRegisterService.getInstance().getServers();
                synchronized (servers) {
                    for (Iterator i=servers.iterator();i.hasNext();) {
                        RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                        if (solverName.equals(server.toString())) {
                            server.stopUsing();
                            break;
                        }
                    }
                }
            }
        }

        if ("Disconnect".equals(op)) {
            String solverName = request.getParameter("solver");
            if (solverName!=null) {
                Set servers = SolverRegisterService.getInstance().getServers();
                synchronized (servers) {
                    for (Iterator i=servers.iterator();i.hasNext();) {
                        RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                        if (solverName.equals(server.toString())) {
                            server.disconnectProxy();
                            i.remove();
                            break;
                        }
                    }
                }
            }
        }

        getSolvers(request);
        getServers(request);
        getExamSolvers(request);
        getStudentSolvers(request);
        return mapping.findForward("showSolvers");
	}
	
	public static String getName(String puid) {
	    return getName(TimetableManager.findByExternalId(puid));
	}

	public static String getName(TimetableManager mgr) {
		if (mgr==null) return null;
	    return mgr.getShortName();
	}

	public static String getName(SolverGroup sg) {
		if (sg==null) return null;
	    return sg.getAbbv();
	}

	private void getSolvers(HttpServletRequest request) throws Exception {
		try {
			WebTable.setOrder(request.getSession(),"manageSolvers.ord",request.getParameter("ord"),1);
			
			WebTable webTable = new WebTable( 19,
					"Manage Course Solvers", "manageSolvers.do?ord=%%",
					new String[] {"Created", "Last Used", "Session", "Host", "Config", "Status", "Owner", "Assign", "Total", "Time", "Stud", "Room", "Distr", "Instr", "TooBig", "Useless", "Pert", "Note", "Operation(s)"},
					new String[] {"left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left"},
					null );
			webTable.setRowStyle("white-space:nowrap");
			
			int nrLines = 0;
			Long currentSessionId = Session.getCurrentAcadSession(Web.getUser(request.getSession())).getUniqueId();
			
            SolverProxy x = WebSolver.getSolverNoSessionCheck(request.getSession());
            String xId = (x==null?null:x.getProperties().getProperty("General.OwnerPuid"));

			HashSet solvers = new HashSet(WebSolver.getSolvers().values());
			for (Iterator i=solvers.iterator();i.hasNext();) {
				SolverProxy solver = (SolverProxy)i.next();
				if (solver==null) continue;
				DataProperties properties = solver.getProperties();
				if (properties==null) continue;
				String runnerName = getName(properties.getProperty("General.OwnerPuid","N/A"));
			   Long[] solverGroupId = properties.getPropertyLongArry("General.SolverGroupId",null);
			   String ownerName = "";
			   if (solverGroupId!=null) {
				   for (int j=0;j<solverGroupId.length;j++) {
					   if (j>0) ownerName += " & ";
					   ownerName += getName((new SolverGroupDAO()).get(solverGroupId[j]));
				   }
			   }
			   if (ownerName==null || ownerName.length()==0)
					ownerName = "N/A";
				if (runnerName==null)
					runnerName = "N/A";
				if (ownerName.equals("N/A"))
					ownerName = runnerName;
				if (runnerName.equals("N/A"))
					runnerName = ownerName;
				if (!ownerName.equals(runnerName))
					ownerName = runnerName+" as "+ownerName;
				Session session = (new SessionDAO()).get(properties.getPropertyLong("General.SessionId",new Long(-1)));
				String sessionLabel = "N/A";
				if (session!=null)
					sessionLabel = session.getLabel();
				SolverPredefinedSetting setting = (new SolverPredefinedSettingDAO()).get(properties.getPropertyLong("General.SettingsId",new Long(-1)));
				String settingLabel = properties.getProperty("Basic.Mode","N/A");
				if (setting!=null)
					settingLabel = setting.getDescription();
				String onClick = null;
				if (session!=null && session.getUniqueId().equals(currentSessionId) && properties.getProperty("General.OwnerPuid")!=null)
					onClick = "onClick=\"document.location='manageSolvers.do?op=Select&puid=" + properties.getProperty("General.OwnerPuid") + "';\"";
				String status = "N/A";
				try {
					status = (String)solver.getProgress().get("STATUS");
				} catch (Exception e) {}
				
				String note = null;
				try {
					note = solver.getNote();
				} catch (Exception e) {}
				if (note!=null) note = note.replaceAll("\n","<br>");
				PropertiesInfo globalInfo = null;
				try {
					globalInfo = solver.getGlobalInfo();
				} catch (Exception e) {}
				String assigned = (globalInfo==null?"?":globalInfo.getProperty("Assigned variables","N/A"));
				String totVal = (globalInfo==null?"?":globalInfo.getProperty("Overall solution value","N/A"));
				String timePr = (globalInfo==null?"?":globalInfo.getProperty("Time preferences","N/A"));
				String studConf = (globalInfo==null?"?":globalInfo.getProperty("Student conflicts","N/A"));
				String roomPr = (globalInfo==null?"?":globalInfo.getProperty("Room preferences","N/A"));
				String distPr = (globalInfo==null?"?":globalInfo.getProperty("Distribution preferences","N/A"));
				String instrPr = (globalInfo==null?"?":globalInfo.getProperty("Back-to-back instructor preferences","N/A"));
				String tooBig = (globalInfo==null?"?":globalInfo.getProperty("Too big rooms","N/A"));
				String useless = (globalInfo==null?"?":globalInfo.getProperty("Useless half-hours","N/A"));
				String pertPen = (globalInfo==null?"?":globalInfo.getProperty("Perturbations: Total penalty","N/A"));
				assigned = assigned.replaceAll(" of ","/");
				if (!"N/A".equals(timePr) && timePr.indexOf('/')>=0) timePr=timePr.substring(0,timePr.indexOf('/')).trim();
				if (!"N/A".equals(roomPr) && roomPr.indexOf('/')>=0) roomPr=roomPr.substring(0,roomPr.indexOf('/')).trim();
				if (!"N/A".equals(instrPr) && instrPr.indexOf('/')>=0) instrPr=instrPr.substring(0,instrPr.indexOf('/')).trim();
				if (!"N/A".equals(assigned) && assigned.indexOf(' ')>=0) assigned=assigned.substring(0,assigned.indexOf(' ')).trim();
				if (!"N/A".equals(timePr) && timePr.indexOf(' ')>=0) timePr=timePr.substring(0,timePr.indexOf(' ')).trim();
				if (!"N/A".equals(roomPr) && roomPr.indexOf(' ')>=0) roomPr=roomPr.substring(0,roomPr.indexOf(' ')).trim();
				if (!"N/A".equals(instrPr) && instrPr.indexOf(' ')>=0) instrPr=instrPr.substring(0,instrPr.indexOf(' ')).trim();
				if (!"N/A".equals(distPr) && distPr.indexOf(' ')>=0) distPr=distPr.substring(0,distPr.indexOf(' ')).trim();
				if (!"N/A".equals(tooBig) && tooBig.indexOf(' ')>=0) tooBig=tooBig.substring(0,tooBig.indexOf(' ')).trim();
				if (!"N/A".equals(useless) && useless.indexOf(' ')>=0) useless=useless.substring(0,useless.indexOf(' ')).trim();
				studConf = studConf.replaceAll(" \\[","(").replaceAll("\\]",")").replaceAll(", ",",").replaceAll("hard:","h").replaceAll("distance:","d").replaceAll("commited:","c").replaceAll("committed:","c");
				Date loaded = solver.getLoadedDate();
				Date lastUsed = solver.getLastUsed(); 
				
                String bgColor = null;
            	if (x!=null && ToolBox.equals(properties.getProperty("General.OwnerPuid"), xId))
            		bgColor = "rgb(168,187,225)";
                
                String host = solver.getHostLabel();
                
                String op = "";
                op += 
                	"<input type=\"button\" value=\"Unload\" onClick=\"" +
                	"if (confirm('Do you really want to unload this solver?')) " +
                	"document.location='manageSolvers.do?op=Unload&puid=" + properties.getProperty("General.OwnerPuid")+ "';" +
                	" event.cancelBubble=true;\">";

				webTable.addLine(onClick, new String[] {
							(loaded==null?"N/A":sDF.format(loaded)),
							(lastUsed==null?"N/A":sDF.format(lastUsed)),
							sessionLabel,
							host,
							settingLabel,
							status,
							ownerName, 
							assigned,
							totVal,
							timePr,
							studConf,
							roomPr,
							distPr,
							instrPr,
							tooBig,
							useless,
							pertPen,
							note,
							op},
						new Comparable[] {
							(loaded==null?new Date():loaded),
							(lastUsed==null?new Date():lastUsed),
							sessionLabel,
							solver.getHost(),
							settingLabel, 
							status,
							ownerName,
							assigned,
							totVal,
							timePr,
							studConf,
							roomPr,
							distPr,
							instrPr,
							tooBig,
							useless,
							pertPen,
							(solver.getNote()==null?"":solver.getNote()),
							null}).setBgColor(bgColor);
					nrLines++;
			}
			if (nrLines==0)
				webTable.addLine(null, new String[] {"<i>No solver is running.</i>"}, null, null );
			request.setAttribute("ManageSolvers.table",webTable.printTable(WebTable.getOrder(request.getSession(),"manageSolvers.ord")));
			
	    } catch (Exception e) {
	        throw new Exception(e);
	    }
	}

	private void getServers(HttpServletRequest request) throws Exception {
		try {
			WebTable.setOrder(request.getSession(),"manageSolvers.ord2",request.getParameter("ord2"),1);
			
			WebTable webTable = new WebTable( 11,
					"Available Servers", "manageSolvers.do?ord2=%%",
					new String[] {"Host", "Version", "Started", "Available Memory", "Ping", "Usage", "NrInstances", "Active", "Working", "Passivated", "Operation(s)"},
					new String[] {"left", "left", "left", "left", "left", "left", "left", "left","left","left","left"},
					null );
			webTable.setRowStyle("white-space:nowrap");
			
			DecimalFormat df = new DecimalFormat("0.00");
			
			int nrLines = 0;
			
            Set servers = SolverRegisterService.getInstance().getServers();
            synchronized (servers) {
                for (Iterator i=servers.iterator();i.hasNext();) {
                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                    if (!server.isActive()) {
                        String op="<input type=\"button\" value=\"Disconnect\" onClick=\"if (confirm('Do you really want to disconnect server "+server+"?')) document.location='manageSolvers.do?op=Disconnect&solver="+server.toString()+"';\">";
                        webTable.addLine(null, new String[] {
                                server.getAddress().getHostName()+":"+server.getPort(),
                                "<i>inactive</i>",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                op
                                },
                            new Comparable[] {
                                server.getAddress().getHostName()+":"+server.getPort(),
                                "",
                                null,
                                new Long(-1),
                                new Long(-1),
                                new Long(-1),
                                new Integer(-1),
                                new Integer(-1),
                                new Integer(-1),
                                new Integer(-1),
                                null
                        });
                        continue;
                    }
                    int nrActive = 0;
                    int nrPassivated = 0;
                    int nrWorking = 0;
                    long mem = server.getAvailableMemory();
                    long t0 = System.currentTimeMillis();
                    long usage = server.getUsage();
                    long t1 = System.currentTimeMillis();
                    for (Enumeration e=server.getSolvers().elements();e.hasMoreElements();) {
                        SolverProxy solver = (SolverProxy)e.nextElement();
                        if (solver.isPassivated()) {
                            nrPassivated++;
                        } else {
                            nrActive++;
                            if (solver.isWorking())
                                nrWorking++;
                        }
                    }
                    for (Enumeration e=server.getExamSolvers().elements();e.hasMoreElements();) {
                        ExamSolverProxy solver = (ExamSolverProxy)e.nextElement();
                        if (solver.isPassivated()) {
                            nrPassivated++;
                        } else {
                            nrActive++;
                            if (solver.isWorking())
                                nrWorking++;
                        }
                    }
                    String version = server.getVersion();
                    Date startTime = server.getStartTime();
                    String op="<input type=\"button\" value=\"Shutdown\" onClick=\"if (confirm('Do you really want to shutdown server "+server+"?')) document.location='manageSolvers.do?op=Shutdown&solver="+server.toString()+"';\">";
                    //op+="&nbsp;&nbsp;<input type=\"button\" value=\"Disconnect\" onClick=\"if (confirm('Do you really want to disconnect server "+server+"?')) document.location='manageSolvers.do?op=Disconnect&solver="+server.toString()+"';\">";
                    //op+="&nbsp;&nbsp;<input type=\"button\" value=\"Kill\" onClick=\"if (confirm('Do you really want to kill server "+server+"?') && confirm('DO YOU REALLY REALLY WANT TO KILL SERVER "+server+"? THIS FUNCTION IS ONLY FOR TESTING PURPOSES AND SHOULD NEVER BE USED IN PRODUCTION!!!')) document.location='manageSolvers.do?op=Kill&solver="+server.toString()+"';\">";
                    if (usage>=1000) {
                        op+="&nbsp;&nbsp;<input type=\"button\" value=\"Enable\" onClick=\"if (confirm('Do you really want to enable server "+server+" for the new solver instances?')) document.location='manageSolvers.do?op=Start%20Using&solver="+server.toString()+"';\">";
                    } else {
                        op+="&nbsp;&nbsp;<input type=\"button\" value=\"Disable\" onClick=\"if (confirm('Do you really want to disable server "+server+" for the new solver instances?')) document.location='manageSolvers.do?op=Stop%20Using&solver="+server.toString()+"';\">";
                    }
                    webTable.addLine(null, new String[] {
                            server.getAddress().getHostName()+":"+server.getPort(),
                            (version==null||"-1".equals(version)?"<i>N/A</i>":version),
                            (startTime==null?"<i>N/A</i>":sDF.format(startTime)),
                            df.format( ((double)mem)/1024/1024)+" MB",
                            (t1-t0)+" ms",
                            String.valueOf(usage),
                            String.valueOf(nrActive+nrPassivated),
                            String.valueOf(nrActive),
                            String.valueOf(nrWorking),
                            String.valueOf(nrPassivated),
                            op
                            },
                        new Comparable[] {
                            server.getAddress().getHostName()+":"+server.getPort(),
                            version,
                            startTime,
                            new Long(t1-t0),
                            new Long(mem),
                            new Long(usage),
                            new Integer(nrActive+nrPassivated),
                            new Integer(nrActive),
                            new Integer(nrWorking),
                            new Integer(nrPassivated),
                            null
                    });
                    nrLines++;
                }
            }
			
			if (ApplicationProperties.isLocalSolverEnabled()) {
				int nrActive = 0;
				int nrPassivated = 0;
				int nrWorking = 0;
				long mem = WebSolver.getAvailableMemory();
				long usage = WebSolver.getUsage();
				Date startTime = (SolverRegisterService.getInstance()==null?null:SolverRegisterService.getInstance().getStartTime());
				for (Enumeration e=WebSolver.getLocalSolvers().elements();e.hasMoreElements();) {
					SolverProxy solver = (SolverProxy)e.nextElement();
					if (solver.isPassivated()) {
						nrPassivated++;
					} else {
						nrActive++;
						if (solver.isWorking())
							nrWorking++;
					}
				}
                for (Enumeration e=WebSolver.getLocalExaminationSolvers().elements();e.hasMoreElements();) {
                    ExamSolverProxy solver = (ExamSolverProxy)e.nextElement();
                    if (solver.isPassivated()) {
                        nrPassivated++;
                    } else {
                        nrActive++;
                        if (solver.isWorking())
                            nrWorking++;
                    }
                }
				String version = Constants.getVersion();
				webTable.addLine(null, new String[] {
				        "local",
						(version==null||"-1".equals(version)?"<i>N/A</i>":version),
						(startTime==null?"<i>N/A</i>":sDF.format(startTime)),
						df.format(((double)mem)/1024/1024)+" MB",
						"N/A",
						String.valueOf(usage),
						String.valueOf(nrActive+nrPassivated),
						String.valueOf(nrActive),
						String.valueOf(nrWorking),
						String.valueOf(nrPassivated),
						""
						},
					new Comparable[] {
						"",
						version,
						startTime,
						new Long(mem),
						new Long(0),
						new Long(usage),
						new Integer(nrActive+nrPassivated),
						new Integer(nrActive),
						new Integer(nrWorking),
						new Integer(nrPassivated),
						null
				});
				nrLines++;
			}
			if (nrLines==0)
				webTable.addLine(null, new String[] {"<i>No solver server is running.</i>"}, null, null );

			request.setAttribute("ManageSolvers.table2",webTable.printTable(WebTable.getOrder(request.getSession(),"manageSolvers.ord2")));
			
	    } catch (Exception e) {
	        throw new Exception(e);
	    }
	}
	
	   private void getExamSolvers(HttpServletRequest request) throws Exception {
	        try {
	            WebTable.setOrder(request.getSession(),"manageSolvers.ord3",request.getParameter("ord3"),1);
	            
	            WebTable webTable = new WebTable( 20,
	                    "Manage Examination Solvers", "manageSolvers.do?ord3=%%",
	                    new String[] {"Created", "Last Used", "Session", "Host", "Config", "Status", "Owner", "Type", "Assign", "Total", "StudConf", "InstConf", "Period", "Room", "RoomSplit", "RoomSize", "Distr", "Rot", "Pert", "Operation(s)"},
	                    new String[] {"left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left"},
	                    null );
	            webTable.setRowStyle("white-space:nowrap");
	            
	            int nrLines = 0;
	            Long currentSessionId = Session.getCurrentAcadSession(Web.getUser(request.getSession())).getUniqueId();
	            
	            ExamSolverProxy x = WebSolver.getExamSolverNoSessionCheck(request.getSession());
	            String xId = (x==null?null:x.getProperties().getProperty("General.OwnerPuid"));
	            
	            for (ExamSolverProxy solver : WebSolver.getExamSolvers().values()) {
	            	if (solver==null) continue;
					DataProperties properties = solver.getProperties();
					if (properties==null) continue;
	                String runnerName = getName(properties.getProperty("General.OwnerPuid","N/A"));
	                int examType = solver.getExamType();
	                if (runnerName==null)
	                    runnerName = "N/A";
	                Session session = (new SessionDAO()).get(properties.getPropertyLong("General.SessionId",new Long(-1)));
	                String sessionLabel = "N/A";
	                if (session!=null)
	                    sessionLabel = session.getLabel();
	                SolverPredefinedSetting setting = (new SolverPredefinedSettingDAO()).get(properties.getPropertyLong("General.SettingsId",new Long(-1)));
	                String settingLabel = properties.getProperty("Basic.Mode","N/A");
	                if (setting!=null)
	                    settingLabel = setting.getDescription();
	                String onClick = null;
	                if (session.getUniqueId().equals(currentSessionId) && properties.getProperty("General.OwnerPuid")!=null)
	                    onClick = "onClick=\"document.location='manageSolvers.do?op=Select&examPuid=" + properties.getProperty("General.OwnerPuid") + "';\"";
	                String status = (String)solver.getProgress().get("STATUS");
	                
	                Map<String,String> info = null;
	                try {
	                	info = solver.currentSolutionInfo();
	                } catch (Exception e) {}
	                String assigned = (String)info.get("Assigned variables");
	                String totVal = (String)info.get("Overall solution value");
	                String dc = (String)info.get("Direct Conflicts");
	                String m2d = (String)info.get("More Than 2 A Day Conflicts");
	                String btb = (String)info.get("Back-To-Back Conflicts");
	                String conf = (dc==null?"0":dc)+", "+(m2d==null?"0":m2d)+", "+(btb==null?"0":btb);
                    String idc = (String)info.get("Instructor Direct Conflicts");
                    String im2d = (String)info.get("Instructor More Than 2 A Day Conflicts");
                    String ibtb = (String)info.get("Instructor Back-To-Back Conflicts");
                    String iconf = (idc==null?"0":idc)+", "+(im2d==null?"0":im2d)+", "+(ibtb==null?"0":ibtb);
	                String pp = (String)info.get("Period Penalty");
	                String rp = (String)info.get("Room Penalty");
	                String rsp = (String)info.get("Room Split Penalty");
	                String rsz = (String)info.get("Room Size Penalty");
	                String dp = (String)info.get("Distribution Penalty");
	                String erp = (String)info.get("Exam Rotation Penalty");
	                String pert = (String)info.get("Perturbation Penalty");
	                Date loaded = solver.getLoadedDate();
	                Date lastUsed = solver.getLastUsed(); 
	                
                    String bgColor = null;
                    if (x!=null && ToolBox.equals(properties.getProperty("General.OwnerPuid"), xId))
                        bgColor = "rgb(168,187,225)";
                    
                    String op = "";
                    op += 
                    	"<input type=\"button\" value=\"Unload\" onClick=\"" +
                    	"if (confirm('Do you really want to unload this solver?')) " +
                    	"document.location='manageSolvers.do?op=Unload&examPuid=" + properties.getProperty("General.OwnerPuid")+ "';" +
                    	" event.cancelBubble=true;\">";
	                
	                webTable.addLine(onClick, new String[] {
	                            (loaded==null?"N/A":sDF.format(loaded)),
	                            (lastUsed==null?"N/A":sDF.format(lastUsed)),
	                            sessionLabel,
	                            solver.getHostLabel(),
	                            settingLabel,
	                            status,
	                            runnerName, 
	                            Exam.sExamTypes[examType],
	                            (assigned==null?"N/A":assigned.indexOf(' ')>=0?assigned.substring(0,assigned.indexOf(' ')):assigned),
	                            (totVal==null?"N/A":totVal),
	                            (conf==null?"N/A":conf), 
	                            (iconf==null?"N/A":iconf),
	                            (pp==null?"N/A":pp.indexOf(' ')>=0?pp.substring(0,pp.indexOf(' ')):pp),
	                            (rp==null?"N/A":rp.indexOf(' ')>=0?rp.substring(0,rp.indexOf(' ')):rp),
	                            (rsp==null?"N/A":rsp.indexOf(' ')>=0?rsp.substring(0,rsp.indexOf(' ')):rsp), 
	                            (rsz==null?"N/A":rsz.indexOf(' ')>=0?rsz.substring(0,rsz.indexOf(' ')):rsz),
	                            (dp==null?"N/A":dp.indexOf(' ')>=0?dp.substring(0,dp.indexOf(' ')):dp),
	                            (erp==null?"N/A":erp.indexOf(' ')>=0?erp.substring(0,erp.indexOf(' ')):erp),
	                            (pert==null?"N/A":pert.indexOf(' ')>=0?pert.substring(0,pert.indexOf(' ')):pert),
	                            op},
	                        new Comparable[] {
	                            (loaded==null?new Date():loaded),
	                            (lastUsed==null?new Date():lastUsed),
	                            sessionLabel,
	                            solver.getHost(),
	                            settingLabel, 
	                            status,
	                            runnerName,
	                            examType,
	                            (assigned==null?"":assigned),
                                (totVal==null?"":totVal),
                                (conf==null?"":conf), 
                                (iconf==null?"":iconf),
                                (pp==null?"":pp), 
                                (rp==null?"":rp),
                                (rsp==null?"":rsp), 
                                (rsz==null?"":rsz),
                                (dp==null?"":dp), 
                                (erp==null?"":erp), 
                                (pert==null?"":pert),
                                null}).setBgColor(bgColor);
	                    nrLines++;
	            }
	            if (nrLines==0)
	                webTable.addLine(null, new String[] {"<i>No solver is running.</i>"}, null, null );
	            request.setAttribute("ManageSolvers.xtable",webTable.printTable(WebTable.getOrder(request.getSession(),"manageSolvers.ord3")));
	            
	        } catch (Exception e) {
	            throw new Exception(e);
	        }
	    }

       private void getStudentSolvers(HttpServletRequest request) throws Exception {
           try {
               WebTable.setOrder(request.getSession(),"manageSolvers.ord4",request.getParameter("ord4"),1);
               
               WebTable webTable = new WebTable( 13,
                       "Manage Student Sectioning Solvers", "manageSolvers.do?ord4=%%",
                       new String[] {"Created", "Last Used", "Session", "Host", "Config", "Status", "Owner", "Assign", "Total", "CompSched", "DistConf", "Pert", "Operation(s)"},
                       new String[] {"left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left", "left"},
                       null );
               webTable.setRowStyle("white-space:nowrap");
               
               int nrLines = 0;
               Long currentSessionId = Session.getCurrentAcadSession(Web.getUser(request.getSession())).getUniqueId();
               
               StudentSolverProxy x = WebSolver.getStudentSolverNoSessionCheck(request.getSession());
               String xId = (x==null?null:x.getProperties().getProperty("General.OwnerPuid"));
               
               for (StudentSolverProxy solver : WebSolver.getStudentSolvers().values()) {
      				if (solver==null) continue;
    				DataProperties properties = solver.getProperties();
    				if (properties==null) continue;
                   String runnerName = getName(properties.getProperty("General.OwnerPuid","N/A"));
                   if (runnerName==null)
                       runnerName = "N/A";
                   Session session = (new SessionDAO()).get(properties.getPropertyLong("General.SessionId",new Long(-1)));
                   if (session==null) continue;
                   String sessionLabel = session.getLabel();
                   SolverPredefinedSetting setting = (new SolverPredefinedSettingDAO()).get(properties.getPropertyLong("General.SettingsId",new Long(-1)));
                   String settingLabel = properties.getProperty("Basic.Mode","N/A");
                   if (setting!=null)
                       settingLabel = setting.getDescription();
                   String onClick = null;
                   if (session.getUniqueId().equals(currentSessionId) && properties.getProperty("General.OwnerPuid")!=null)
                       onClick = "onClick=\"document.location='manageSolvers.do?op=Select&sectionPuid=" + properties.getProperty("General.OwnerPuid") + "';\"";
                   String status = (String)solver.getProgress().get("STATUS");
                   
                   Map<String,String> info = null;
                   try {
                	   info = solver.currentSolutionInfo();
                   } catch (Exception e) {}
                   String assigned = (String)info.get("Assigned variables");
                   String totVal = (String)info.get("Overall solution value");
                   String compSch = (String)info.get("Students with complete schedule");
                   String distConf = (String)info.get("Student distance conflicts");
                   String pert = (String)info.get("Perturbation Penalty");
                   Date loaded = solver.getLoadedDate();
                   Date lastUsed = solver.getLastUsed(); 
                   
                   String bgColor = null;
                   if (x!=null && ToolBox.equals(properties.getProperty("General.OwnerPuid"), xId))
                       bgColor = "rgb(168,187,225)";
                   
                   String op = "";
                   op += 
                   	"<input type=\"button\" value=\"Unload\" onClick=\"" +
                   	"if (confirm('Do you really want to unload this solver?')) " +
                   	"document.location='manageSolvers.do?op=Unload&sectionPuid=" + properties.getProperty("General.OwnerPuid")+ "';" +
                   	" event.cancelBubble=true;\">";
                   
                   webTable.addLine(onClick, new String[] {
                               (loaded==null?"N/A":sDF.format(loaded)),
                               (lastUsed==null?"N/A":sDF.format(lastUsed)),
                               sessionLabel,
                               solver.getHostLabel(),
                               settingLabel,
                               status,
                               runnerName, 
                               (assigned==null?"N/A":assigned.indexOf(' ')>=0?assigned.substring(0,assigned.indexOf(' ')):assigned),
                               (totVal==null?"N/A":totVal),
                               (compSch==null?"N/A":compSch), 
                               (distConf==null?"N/A":distConf),
                               (pert==null?"N/A":pert.indexOf(' ')>=0?pert.substring(0,pert.indexOf(' ')):pert),
                               op},
                           new Comparable[] {
                               (loaded==null?new Date():loaded),
                               (lastUsed==null?new Date():lastUsed),
                               sessionLabel,
                               solver.getHost(),
                               settingLabel, 
                               status,
                               runnerName,
                               (assigned==null?"":assigned),
                               (totVal==null?"":totVal),
                               (compSch==null?"":compSch), 
                               (distConf==null?"":distConf),
                               (pert==null?"":pert),
                               null}).setBgColor(bgColor);
                       nrLines++;
               }
               if (nrLines==0)
                   webTable.addLine(null, new String[] {"<i>No solver is running.</i>"}, null, null );
               request.setAttribute("ManageSolvers.stable",webTable.printTable(WebTable.getOrder(request.getSession(),"manageSolvers.ord4")));
               
           } catch (Exception e) {
               throw new Exception(e);
           }
       }
}

