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
package org.unitime.timetable.solver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ProgressListener;
import net.sf.cpsolver.studentsct.weights.StudentWeights;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Settings;
import org.unitime.timetable.model.SolverParameter;
import org.unitime.timetable.model.SolverParameterDef;
import org.unitime.timetable.model.SolverParameterGroup;
import org.unitime.timetable.model.SolverPredefinedSetting;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.SolverPredefinedSettingDAO;
import org.unitime.timetable.solver.exam.ExamSolver;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.exam.RemoteExamSolverProxy;
import org.unitime.timetable.solver.exam.ExamSolver.ExamSolverDisposeListener;
import org.unitime.timetable.solver.remote.BackupFileFilter;
import org.unitime.timetable.solver.remote.RemoteSolverProxy;
import org.unitime.timetable.solver.remote.RemoteSolverServerProxy;
import org.unitime.timetable.solver.remote.SolverRegisterService;
import org.unitime.timetable.solver.studentsct.StudentSolver;
import org.unitime.timetable.solver.studentsct.StudentSolverProxy;
import org.unitime.timetable.solver.studentsct.StudentSolver.StudentSolverDisposeListener;
import org.unitime.timetable.tags.SolverWarnings;
import org.unitime.timetable.util.Constants;

/**
 * @author Tomas Muller
 */
public class WebSolver extends TimetableSolver implements ProgressListener {
	protected static Log sLog = LogFactory.getLog(WebSolver.class);
	public static SimpleDateFormat sDF = new SimpleDateFormat("MM/dd/yy hh:mmaa");
	private JspWriter iJspWriter;
	private static Hashtable<String,SolverProxy> sSolvers = new Hashtable();
	private static Hashtable<String,ExamSolverProxy> sExamSolvers = new Hashtable();
	private static Hashtable<String,StudentSolverProxy> sStudentSolvers = new Hashtable();
	private static SolverPassivationThread sSolverPasivationThread = null;
	private static long sMemoryLimit = Integer.parseInt(ApplicationProperties.getProperty("tmtbl.solver.mem_limit","200"))*1024*1024; //200 MB
	private static boolean sBackupWhenDone = false;
	
	public WebSolver(DataProperties properties) {
		super(properties);
	}
	
	public static ExamSolverProxy getExamSolver(String puid, Long sessionId) {
        try {
            ExamSolverProxy solver = sExamSolvers.get(puid);
            if (solver!=null) {
                if (sessionId!=null && !sessionId.equals(solver.getProperties().getPropertyLong("General.SessionId",null))) 
                    return null;
                return solver;
            }
            Set servers = SolverRegisterService.getInstance().getServers();
            synchronized (servers) {
                for (Iterator i=servers.iterator();i.hasNext();) {
                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                    if (!server.isActive()) continue;
                    ExamSolverProxy proxy = server.getExamSolver(puid);
                    if (proxy!=null) {
                        if (sessionId!=null && !sessionId.equals(proxy.getProperties().getPropertyLong("General.SessionId",null))) 
                            return null;
                        return proxy;
                    }
                }
            }
        } catch (Exception e) {
            sLog.error("Unable to retrieve solver, reason:"+e.getMessage(),e);
        }
        return null;
    }
	
	public static ExamSolverProxy getExamSolver(javax.servlet.http.HttpSession session) {
	    ExamSolverProxy solver = (ExamSolverProxy)session.getAttribute("ExamSolverProxy");
        if (solver!=null) {
            try {
                if (solver instanceof RemoteExamSolverProxy && ((RemoteExamSolverProxy)solver).exists())
                    return solver;
                else
                    session.removeAttribute("ExamSolverProxy");
            } catch (Exception e) {
                session.removeAttribute("ExamSolverProxy");
            };
        }
        User user = Web.getUser(session);
        if (user==null) return null;
        Session acadSession = null;
        try {
            acadSession = Session.getCurrentAcadSession(user);
        } catch (Exception e) {}
        if (acadSession==null) return null;
        TimetableManager mgr = TimetableManager.getManager(user);
        if (!mgr.canTimetableExams(acadSession, user)) return null;
        String puid = (String)session.getAttribute("ManageSolver.examPuid");
        if (puid!=null) {
            solver = getExamSolver(puid, acadSession.getUniqueId());
            if (solver!=null) {
                session.setAttribute("ExamSolverProxy", solver);
                return solver;
            }
        }
        solver = getExamSolver(user.getId(), acadSession.getUniqueId());
        if (solver==null) return null;
        session.setAttribute("ExamSolverProxy", solver);
        return solver;
    }
	
	public static ExamSolverProxy getExamSolverNoSessionCheck(javax.servlet.http.HttpSession session) {
        try {
            User user = Web.getUser(session);
            if (user==null) return null;
            String puid = (String)session.getAttribute("ManageSolver.examPuid");
            if (puid!=null) {
                ExamSolverProxy solver = getExamSolver(puid, null);
                if (solver!=null) return solver;
            }
            return getExamSolver(user.getId(), null);
        } catch (Exception e) {
            sLog.error("Unable to retrieve solver, reason:"+e.getMessage(),e);
        }
        return null;
    }
	
	   public static StudentSolverProxy getStudentSolver(String puid, Long sessionId) {
	        try {
	            StudentSolverProxy solver = sStudentSolvers.get(puid);
	            if (solver!=null) {
	                if (sessionId!=null && !sessionId.equals(solver.getProperties().getPropertyLong("General.SessionId",null))) 
	                    return null;
	                return solver;
	            }
	            Set servers = SolverRegisterService.getInstance().getServers();
	            synchronized (servers) {
	                for (Iterator i=servers.iterator();i.hasNext();) {
	                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
	                    if (!server.isActive()) continue;
	                    StudentSolverProxy proxy = server.getStudentSolver(puid);
	                    if (proxy!=null) {
	                        if (sessionId!=null && !sessionId.equals(proxy.getProperties().getPropertyLong("General.SessionId",null))) 
	                            return null;
	                        return proxy;
	                    }
	                }
	            }
	        } catch (Exception e) {
	            sLog.error("Unable to retrieve solver, reason:"+e.getMessage(),e);
	        }
	        return null;
	    }
	    
	    public static StudentSolverProxy getStudentSolver(javax.servlet.http.HttpSession session) {
	        StudentSolverProxy solver = (StudentSolverProxy)session.getAttribute("StudentSolverProxy");
	        if (solver!=null) {
	            try {
	                if (solver instanceof RemoteExamSolverProxy && ((RemoteExamSolverProxy)solver).exists())
	                    return solver;
	                else
	                    session.removeAttribute("StudentSolverProxy");
	            } catch (Exception e) {
	                session.removeAttribute("StudentSolverProxy");
	            };
	        }
	        User user = Web.getUser(session);
	        if (user==null) return null;
	        Session acadSession = null;
	        try {
	            acadSession = Session.getCurrentAcadSession(user);
	        } catch (Exception e) {}
	        if (acadSession==null) return null;
	        TimetableManager mgr = TimetableManager.getManager(user);
	        if (!mgr.canSectionStudents(acadSession, user)) return null;
	        String puid = (String)session.getAttribute("ManageSolver.sectionPuid");
	        if (puid!=null) {
	            solver = getStudentSolver(puid, acadSession.getUniqueId());
	            if (solver!=null) {
	                session.setAttribute("StudentSolverProxy", solver);
	                return solver;
	            }
	        }
	        solver = getStudentSolver(user.getId(), acadSession.getUniqueId());
	        if (solver==null) return null;
	        session.setAttribute("StudentSolverProxy", solver);
	        return solver;
	    }
	    
	    public static StudentSolverProxy getStudentSolverNoSessionCheck(javax.servlet.http.HttpSession session) {
	        try {
	            User user = Web.getUser(session);
	            if (user==null) return null;
	            String puid = (String)session.getAttribute("ManageSolver.sectionPuid");
	            if (puid!=null) {
	                StudentSolverProxy solver = getStudentSolver(puid, null);
	                if (solver!=null) return solver;
	            }
	            return getStudentSolver(user.getId(), null);
	        } catch (Exception e) {
	            sLog.error("Unable to retrieve solver, reason:"+e.getMessage(),e);
	        }
	        return null;
	    }

	public static SolverProxy getSolver(String puid, Long sessionId) {
		try {
			SolverProxy proxy = (SolverProxy)sSolvers.get(puid);
			if (proxy!=null) {
				if (sessionId!=null && !sessionId.equals(proxy.getProperties().getPropertyLong("General.SessionId",null))) 
					return null;
				return proxy;
			}
            Set servers = SolverRegisterService.getInstance().getServers();
            synchronized (servers) {
                for (Iterator i=servers.iterator();i.hasNext();) {
                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                    if (!server.isActive()) continue;
                    proxy = server.getSolver(puid);
                    if (proxy!=null) {
                        if (sessionId!=null && !sessionId.equals(proxy.getProperties().getPropertyLong("General.SessionId",null))) 
                            return null;
                        return proxy;
                    }
				}
			}
		} catch (Exception e) {
			sLog.error("Unable to retrieve solver, reason:"+e.getMessage(),e);
		}
		return null;
	}
	
	public static SolverProxy getSolver(javax.servlet.http.HttpSession session) {
		SolverProxy solver = (SolverProxy)session.getAttribute("SolverProxy");
		if (solver!=null) {
			try {
				if (solver instanceof RemoteSolverProxy && ((RemoteSolverProxy)solver).exists())
					return solver;
				else
					session.removeAttribute("SolverProxy");
			} catch (Exception e) {
				session.removeAttribute("SolverProxy");
			};
		}
		User user = Web.getUser(session);
		if (user==null) return null;
		Long sessionId = null;
		try {
			sessionId = Session.getCurrentAcadSession(user).getUniqueId();
		} catch (Exception e) {}
		String puid = (String)session.getAttribute("ManageSolver.puid");
		if (puid!=null) {
			solver = getSolver(puid, sessionId);
			if (solver!=null) {
				session.setAttribute("SolverProxy", solver);
				return solver;
			}
		}
		solver = getSolver(user.getId(), sessionId);
		if (solver!=null)
			session.setAttribute("SolverProxy", solver);
		return solver;
	}
	
	public static SolverProxy getSolverNoSessionCheck(javax.servlet.http.HttpSession session) {
		User user = Web.getUser(session);
		if (user==null) return null;
		String puid = (String)session.getAttribute("ManageSolver.puid");
		if (puid!=null) {
			SolverProxy solver = getSolver(puid, null);
			if (solver!=null) return solver;
		}
		return getSolver(user.getId(), null);
	}	
	
	public static DataProperties createProperties(Long settingsId, Hashtable extraParams, int type) {
		DataProperties properties = new DataProperties();
		Transaction tx = null;
		try {
			SolverPredefinedSettingDAO dao = new SolverPredefinedSettingDAO();
			org.hibernate.Session hibSession = dao.getSession();
			if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
				tx = hibSession.beginTransaction();
			List defaultParams = hibSession.createCriteria(SolverParameterDef.class).list();
			for (Iterator i=defaultParams.iterator();i.hasNext();) {
				SolverParameterDef def = (SolverParameterDef)i.next();
				if (def.getGroup().getType()!=type) continue;
				if (def.getDefault()!=null)
					properties.put(def.getName(),def.getDefault());
				if (extraParams!=null && extraParams.containsKey(def.getUniqueId()))
					properties.put(def.getName(), (String)extraParams.get(def.getUniqueId()));
			}
			SolverPredefinedSetting settings = dao.get(settingsId);
			for (Iterator i=settings.getParameters().iterator();i.hasNext();) {
				SolverParameter param = (SolverParameter)i.next();
				if (!param.getDefinition().isVisible().booleanValue()) continue;
				if (param.getDefinition().getGroup().getType()!=type) continue;
				properties.put(param.getDefinition().getName(),param.getValue());
				if (extraParams!=null && extraParams.containsKey(param.getDefinition().getUniqueId()))
					properties.put(param.getDefinition().getName(), (String)extraParams.get(param.getDefinition().getUniqueId()));
			}
			properties.setProperty("General.SettingsId", settings.getUniqueId().toString());
			if (tx!=null) tx.commit();
		} catch (Exception e) {
			if (tx!=null) tx.rollback();
			sLog.error(e);
		}
		StringBuffer ext = new StringBuffer();
		if (properties.getPropertyBoolean("General.SearchIntensification",type==SolverParameterGroup.sTypeCourse)) {
        	if (ext.length()>0) ext.append(";");
        	ext.append("net.sf.cpsolver.ifs.extension.SearchIntensification");
        }
        if (properties.getPropertyBoolean("General.CBS",type==SolverParameterGroup.sTypeCourse)) {
        	if (ext.length()>0) ext.append(";");
        	ext.append("net.sf.cpsolver.ifs.extension.ConflictStatistics");
        } else if (properties.getPropertyBoolean("ExamGeneral.CBS",type==SolverParameterGroup.sTypeExam)) {
            if (ext.length()>0) ext.append(";");
            ext.append("net.sf.cpsolver.ifs.extension.ConflictStatistics");
            properties.setProperty("ConflictStatistics.Print","true");
        }
        if (type==SolverParameterGroup.sTypeCourse) {
            String mode = properties.getProperty("Basic.Mode","Initial");
            if ("MPP".equals(mode)) {
                properties.setProperty("General.MPP","true");
                if (ext.length()>0) ext.append(";");
                ext.append("net.sf.cpsolver.ifs.extension.ViolatedInitials");
            }
        } else if (type==SolverParameterGroup.sTypeExam) {
            String mode = properties.getProperty("ExamBasic.Mode","Initial");
            if ("MPP".equals(mode)) {
                properties.setProperty("General.MPP","true");
                /*
                if (ext.length()>0) ext.append(";");
                ext.append("net.sf.cpsolver.ifs.extension.ViolatedInitials");
                */
            }
        } else if (type==SolverParameterGroup.sTypeStudent) {
            String mode = properties.getProperty("StudentSctBasic.Mode","Initial");
            if ("MPP".equals(mode))
                properties.setProperty("General.MPP","true");
            if (properties.getPropertyBoolean("StudentSct.CBS",true)) {
                if (ext.length()>0) ext.append(";");
                ext.append("net.sf.cpsolver.studentsct.extension.StudentConflictStatistics");
                properties.setProperty("ConflictStatistics.Print","true");
            }
            if (properties.getPropertyBoolean("StudentSct.StudentDist",true)) {
                if (ext.length()>0) ext.append(";");
                ext.append("net.sf.cpsolver.studentsct.extension.DistanceConflict");
            }
            if (properties.getPropertyBoolean("StudentSct.TimeOverlaps",true)) {
                if (ext.length()>0) ext.append(";");
                ext.append("net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter");
            }
            if (!properties.getProperty("StudentWeights.Mode","").isEmpty()) {
                StudentWeights.Implementation studentWeights = StudentWeights.Implementation.valueOf(properties.getProperty("StudentWeights.Mode"));
                if (studentWeights != null) {
                	properties.setProperty("StudentWeights.Class", studentWeights.getImplementation().getName());
                	properties.setProperty("Comparator.Class", studentWeights.getImplementation().getName());
                }
            }
        }
        properties.setProperty("Extensions.Classes",ext.toString());
        if (properties.getPropertyBoolean("Basic.DisobeyHard",false)) {
        	properties.setProperty("General.InteractiveMode", "true");
        }
        if ("No Action".equals(properties.getProperty("Basic.WhenFinished")) || "No Action".equals(properties.getProperty("ExamBasic.WhenFinished")) || "No Action".equals(properties.getProperty("StudentSctBasic.WhenFinished"))) {
            properties.setProperty("General.Save","false");
            properties.setProperty("General.CreateNewSolution","false");
            properties.setProperty("General.Unload","false");
        } else if ("Save".equals(properties.getProperty("Basic.WhenFinished")) || "Save".equals(properties.getProperty("ExamBasic.WhenFinished")) || "Save".equals(properties.getProperty("StudentSctBasic.WhenFinished"))) {
            properties.setProperty("General.Save","true");
            properties.setProperty("General.CreateNewSolution","false");
            properties.setProperty("General.Unload","false");
        } else if ("Save as New".equals(properties.getProperty("Basic.WhenFinished"))) {
            properties.setProperty("General.Save","true");
            properties.setProperty("General.CreateNewSolution","true");
            properties.setProperty("General.Unload","false");
        } else if ("Save and Unload".equals(properties.getProperty("Basic.WhenFinished")) || "Save and Unload".equals(properties.getProperty("ExamBasic.WhenFinished")) || "Save and Unload".equals(properties.getProperty("StudentSctBasic.WhenFinished"))) {
            properties.setProperty("General.Save","true");
            properties.setProperty("General.CreateNewSolution","false");
            properties.setProperty("General.Unload","true");
        } else if ("Save as New and Unload".equals(properties.getProperty("Basic.WhenFinished"))) {
            properties.setProperty("General.Save","true");
            properties.setProperty("General.CreateNewSolution","true");
            properties.setProperty("General.Unload","true");
        }
        properties.setProperty("Xml.ShowNames","true");
        if (type==SolverParameterGroup.sTypeCourse)
            properties.setProperty("Xml.ExportStudentSectioning", "true");
        if (type==SolverParameterGroup.sTypeExam) {
            properties.setProperty("Exam.GreatDeluge", ("Great Deluge".equals(properties.getProperty("Exam.Algorithm","Great Deluge"))?"true":"false"));
            if (extraParams!=null && extraParams.get("Exam.Type")!=null)
                properties.setProperty("Exam.Type", extraParams.get("Exam.Type").toString());
        }
        if (properties.getProperty("Distances.Ellipsoid") == null || properties.getProperty("Distances.Ellipsoid").equals("DEFAULT"))
            properties.setProperty("Distances.Ellipsoid", ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name()));
        properties.expand();
        return properties;
	}
	
	public static SolverProxy createSolver(Long sessionId, javax.servlet.http.HttpSession session, Long[] ownerId, String solutionIds, Long settingsId, Hashtable extraParams, boolean startSolver, String host) throws Exception {
		try {
		    System.out.println("Memory limit is "+sMemoryLimit);
		    
		User user = Web.getUser(session);
		if (user==null) return null;
		
		removeSolver(session);
			
		DataProperties properties = createProperties(settingsId, extraParams, SolverParameterGroup.sTypeCourse);
		
        String warn = SolverWarnings.getSolverWarning(session, ownerId);
        if (warn!=null) 
        	properties.setProperty("General.SolverWarnings",warn);
        else
        	properties.remove("General.SolverWarnings");
		
		properties.setProperty("General.SessionId",sessionId.toString());
		properties.setProperty("General.SolverGroupId",ownerId);
		properties.setProperty("General.OwnerPuid", user.getId());
		if (solutionIds!=null)
			properties.setProperty("General.SolutionId",solutionIds);
		properties.setProperty("General.StartTime", String.valueOf((new Date()).getTime()));
	    properties.setProperty("General.StartSolver",Boolean.toString(startSolver));
	    String instructorFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
	    if (instructorFormat!=null)
	    	properties.setProperty("General.InstructorFormat",instructorFormat);
	    
	    if (host!=null) {
            Set servers = SolverRegisterService.getInstance().getServers();
            synchronized (servers) {
                for (Iterator i=servers.iterator();i.hasNext();) {
                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                    if (!server.isActive()) continue;
                    if (host.equals(server.getAddress().getHostName()+":"+server.getPort())) {
                        SolverProxy solver = server.createSolver(user.getId(),properties);
                        solver.load(properties);
                        return solver;
                    }
                }
            }
	    }
	    
	    if (!"local".equals(host) && !SolverRegisterService.getInstance().getServers().isEmpty()) {
	    	RemoteSolverServerProxy bestServer = null;
            Set servers = SolverRegisterService.getInstance().getServers();
            synchronized (servers) {
                for (Iterator i=servers.iterator();i.hasNext();) {
                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                    if (!server.isActive()) continue;
                    if (server.getAvailableMemory()<sMemoryLimit) continue;
                    if (bestServer==null) {
                        bestServer = server;
                    } else if (bestServer.getUsage()>server.getUsage()) {
                        bestServer = server;
                    }
                }
            }
			if (bestServer!=null) {
				SolverProxy solver = bestServer.createSolver(user.getId(),properties);
				solver.load(properties);
				return solver;
			}
	    }
	    
	    if (getAvailableMemory()<sMemoryLimit)
	    	throw new Exception("Not enough resources to create a solver instance, please try again later.");
    	WebSolver solver = new WebSolver(properties);
    	solver.load(properties);
    	Progress.getInstance(solver.currentSolution().getModel()).addProgressListener(solver);
    	sSolvers.put(user.getId(),solver);
    	return solver;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	   public static ExamSolverProxy createExamSolver(Long sessionId, javax.servlet.http.HttpSession session, Long settingsId, Hashtable extraParams, boolean startSolver, String host) throws Exception {
	        try {
	            
	        User user = Web.getUser(session);
	        if (user==null) return null;
	        
	        removeExamSolver(session);
	            
	        DataProperties properties = createProperties(settingsId, extraParams, SolverParameterGroup.sTypeExam);
	        
	        properties.setProperty("General.SessionId",sessionId.toString());
	        properties.setProperty("General.OwnerPuid", user.getId());
	        properties.setProperty("General.StartTime", String.valueOf((new Date()).getTime()));
	        properties.setProperty("General.StartSolver",Boolean.toString(startSolver));
	        String instructorFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
	        if (instructorFormat!=null)
	            properties.setProperty("General.InstructorFormat",instructorFormat);
	        
	        if (host!=null) {
	            Set servers = SolverRegisterService.getInstance().getServers();
	            synchronized (servers) {
	                for (Iterator i=servers.iterator();i.hasNext();) {
	                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
	                    if (!server.isActive()) continue;
	                    if (host.equals(server.getAddress().getHostName()+":"+server.getPort())) {
	                        ExamSolverProxy solver = server.createExamSolver(user.getId(), properties);
	                        solver.load(properties);
	                        return solver;
	                    }
	                }
	            }
	        }
	        
	        if (!"local".equals(host) && !SolverRegisterService.getInstance().getServers().isEmpty()) {
	            RemoteSolverServerProxy bestServer = null;
	            Set servers = SolverRegisterService.getInstance().getServers();
	            synchronized (servers) {
	                for (Iterator i=servers.iterator();i.hasNext();) {
	                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
	                    if (!server.isActive()) continue;
	                    if (server.getAvailableMemory()<sMemoryLimit) continue;
	                    if (bestServer==null) {
	                        bestServer = server;
	                    } else if (bestServer.getUsage()>server.getUsage()) {
	                        bestServer = server;
	                    }
	                }
	            }
	            if (bestServer!=null) {
	                ExamSolverProxy solver = bestServer.createExamSolver(user.getId(), properties);
	                solver.load(properties);
	                return solver;
	            }
	        }
	        
	        if (getAvailableMemory()<sMemoryLimit)
	            throw new Exception("Not enough resources to create a solver instance, please try again later.");
	        ExamSolver solver = new ExamSolver(properties, new ExamSolverOnDispose(user.getId()));
	        sExamSolvers.put(user.getId(), solver);
	        solver.load(properties);
	        //Progress.getInstance(sExamSolver.currentSolution().getModel()).addProgressListener(sExamSolver);
	        return solver;
	        } catch (Exception e) {
	            e.printStackTrace();
	            throw e;
	        }
	    }
	   
       public static StudentSolverProxy createStudentSolver(Long sessionId, javax.servlet.http.HttpSession session, Long settingsId, Hashtable extraParams, boolean startSolver, String host) throws Exception {
           try {
               
           User user = Web.getUser(session);
           if (user==null) return null;
           
           removeExamSolver(session);
               
           DataProperties properties = createProperties(settingsId, extraParams, SolverParameterGroup.sTypeStudent);
           
           properties.setProperty("General.SessionId",sessionId.toString());
           properties.setProperty("General.OwnerPuid", user.getId());
           properties.setProperty("General.StartTime", String.valueOf((new Date()).getTime()));
           properties.setProperty("General.StartSolver",Boolean.toString(startSolver));
           String instructorFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
           if (instructorFormat!=null)
               properties.setProperty("General.InstructorFormat",instructorFormat);
           
           if (host!=null) {
               Set servers = SolverRegisterService.getInstance().getServers();
               synchronized (servers) {
                   for (Iterator i=servers.iterator();i.hasNext();) {
                       RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                       if (!server.isActive()) continue;
                       if (host.equals(server.getAddress().getHostName()+":"+server.getPort())) {
                           StudentSolverProxy solver = server.createStudentSolver(user.getId(), properties);
                           solver.load(properties);
                           return solver;
                       }
                   }
               }
           }
           
           if (!"local".equals(host) && !SolverRegisterService.getInstance().getServers().isEmpty()) {
               RemoteSolverServerProxy bestServer = null;
               Set servers = SolverRegisterService.getInstance().getServers();
               synchronized (servers) {
                   for (Iterator i=servers.iterator();i.hasNext();) {
                       RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                       if (!server.isActive()) continue;
                       if (server.getAvailableMemory()<sMemoryLimit) continue;
                       if (bestServer==null) {
                           bestServer = server;
                       } else if (bestServer.getUsage()>server.getUsage()) {
                           bestServer = server;
                       }
                   }
               }
               if (bestServer!=null) {
                   StudentSolverProxy solver = bestServer.createStudentSolver(user.getId(), properties);
                   solver.load(properties);
                   return solver;
               }
           }
           
           if (getAvailableMemory()<sMemoryLimit)
               throw new Exception("Not enough resources to create a solver instance, please try again later.");
           StudentSolverProxy solver = new StudentSolver(properties, new StudentSolverOnDispose(user.getId()));
           sStudentSolvers.put(user.getId(), solver);
           solver.load(properties);
           //Progress.getInstance(sExamSolver.currentSolution().getModel()).addProgressListener(sExamSolver);
           return solver;
           } catch (Exception e) {
               e.printStackTrace();
               throw e;
           }
       }
	
	public static SolverProxy reload(javax.servlet.http.HttpSession session, Long settingsId, Hashtable extraParams) throws Exception {
		User user = Web.getUser(session);
		if (user==null) return null;
		
		SolverProxy solver = getSolver(session);
		if (solver==null) return null;
		DataProperties oldProperties = solver.getProperties();
		
		if (settingsId==null)
			settingsId = oldProperties.getPropertyLong("General.SettingsId", null);
		
		DataProperties properties = createProperties(settingsId, extraParams, SolverParameterGroup.sTypeCourse);
		
        String warn = SolverWarnings.getSolverWarning(session, oldProperties.getPropertyLongArry("General.SolverGroupId", null));
        if (warn!=null) properties.setProperty("General.SolverWarnings",warn);
		
		properties.setProperty("General.SessionId",oldProperties.getProperty("General.SessionId"));
		properties.setProperty("General.SolverGroupId",oldProperties.getProperty("General.SolverGroupId"));
		properties.setProperty("General.OwnerPuid", oldProperties.getProperty("General.OwnerPuid"));
		properties.setProperty("General.StartTime", String.valueOf((new Date()).getTime()));
	    String instructorFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
	    if (instructorFormat!=null)
	    	properties.setProperty("General.InstructorFormat",instructorFormat);
	    
    	solver.reload(properties);
    	
    	if (solver instanceof WebSolver) {
    		Progress p = Progress.getInstance(((WebSolver)solver).currentSolution().getModel());
    		p.clearProgressListeners();
    		p.addProgressListener((WebSolver)solver);
    		sSolvers.put(user.getId(),solver);
    	}
    	
    	return solver;
	}
	
    public static ExamSolverProxy reloadExamSolver(javax.servlet.http.HttpSession session, Long settingsId, Hashtable extraParams) throws Exception {
        User user = Web.getUser(session);
        if (user==null) return null;
        
        ExamSolverProxy solver = getExamSolver(session);
        if (solver==null) return null;
        DataProperties oldProperties = solver.getProperties();
        
        if (settingsId==null)
            settingsId = oldProperties.getPropertyLong("General.SettingsId", null);
        
        DataProperties properties = createProperties(settingsId, extraParams, SolverParameterGroup.sTypeExam);
        
        String warn = SolverWarnings.getSolverWarning(session, oldProperties.getPropertyLongArry("General.SolverGroupId", null));
        if (warn!=null) properties.setProperty("General.SolverWarnings",warn);
        
        properties.setProperty("General.SessionId",oldProperties.getProperty("General.SessionId"));
        properties.setProperty("General.SolverGroupId",oldProperties.getProperty("General.SolverGroupId"));
        properties.setProperty("General.OwnerPuid", oldProperties.getProperty("General.OwnerPuid"));
        properties.setProperty("General.StartTime", String.valueOf((new Date()).getTime()));
        String instructorFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
        if (instructorFormat!=null)
            properties.setProperty("General.InstructorFormat",instructorFormat);
        
        solver.reload(properties);
        
        return solver;
    }
    
    public static StudentSolverProxy reloadStudentSolver(javax.servlet.http.HttpSession session, Long settingsId, Hashtable extraParams) throws Exception {
        User user = Web.getUser(session);
        if (user==null) return null;
        
        StudentSolverProxy solver = getStudentSolver(session);
        if (solver==null) return null;
        DataProperties oldProperties = solver.getProperties();
        
        if (settingsId==null)
            settingsId = oldProperties.getPropertyLong("General.SettingsId", null);
        
        DataProperties properties = createProperties(settingsId, extraParams, SolverParameterGroup.sTypeStudent);
        
        String warn = SolverWarnings.getSolverWarning(session, oldProperties.getPropertyLongArry("General.SolverGroupId", null));
        if (warn!=null) properties.setProperty("General.SolverWarnings",warn);
        
        properties.setProperty("General.SessionId",oldProperties.getProperty("General.SessionId"));
        properties.setProperty("General.SolverGroupId",oldProperties.getProperty("General.SolverGroupId"));
        properties.setProperty("General.OwnerPuid", oldProperties.getProperty("General.OwnerPuid"));
        properties.setProperty("General.StartTime", String.valueOf((new Date()).getTime()));
        String instructorFormat = Settings.getSettingValue(user, Constants.SETTINGS_INSTRUCTOR_NAME_FORMAT);
        if (instructorFormat!=null)
            properties.setProperty("General.InstructorFormat",instructorFormat);
        
        solver.reload(properties);
        
        return solver;
    }
	
	public void dispose() {
		super.dispose();
		if (iJspWriter!=null) {
			try {
				iJspWriter.println("<I>Solver finished.</I>");
				iJspWriter.flush();
			} catch (Exception e) {}
			iJspWriter = null;
		}
		String puid = getProperties().getProperty("General.OwnerPuid");
		if (puid!=null)
			sSolvers.remove(puid);
	}
	
	public static void saveSolution(javax.servlet.http.HttpSession session, boolean createNewSolution, boolean commitSolution) throws Exception {
		SolverProxy solver = getSolver(session);
		if (solver==null) return;
		solver.save(createNewSolution, commitSolution);
	}
	
    public static void removeSolver(javax.servlet.http.HttpSession session) throws Exception {
		session.removeAttribute("SolverProxy");
		session.removeAttribute("Suggestions.model");
		session.removeAttribute("Timetable.table");
		SolverProxy solver = getSolverNoSessionCheck(session);
		if (solver!=null) {
			solver.interrupt();
			solver.dispose();
		}
		session.removeAttribute("ManageSolver.puid");
	}
	
    public static void removeExamSolver(javax.servlet.http.HttpSession session) throws Exception {
        session.removeAttribute("ExamSolverProxy");
        ExamSolverProxy solver = getExamSolverNoSessionCheck(session);
        if (solver!=null) {
			solver.interrupt();
            solver.dispose();
        }
        session.removeAttribute("ManageSolver.examPuid");
    }

    public static void removeStudentSolver(javax.servlet.http.HttpSession session) throws Exception {
        session.removeAttribute("StudentSolverProxy");
        StudentSolverProxy solver = getStudentSolverNoSessionCheck(session);
        if (solver!=null) {
			solver.interrupt();
            solver.dispose();
        }
        session.removeAttribute("ManageSolver.sectionPuid");
    }

    public static Hashtable<String,SolverProxy> getSolvers() throws Exception {
		Hashtable<String,SolverProxy> solvers = new Hashtable(sSolvers);
        Set servers = SolverRegisterService.getInstance().getServers();
        synchronized (servers) {
            for (Iterator i=servers.iterator();i.hasNext();) {
                RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                if (!server.isActive()) continue;
                Hashtable serverSolvers = server.getSolvers();
                if (serverSolvers!=null)
                    solvers.putAll(serverSolvers);
            }
		}
		return solvers; 
	}
	
    public static Hashtable<String,ExamSolverProxy> getExamSolvers() throws Exception {
        Hashtable<String,ExamSolverProxy> solvers = new Hashtable(sExamSolvers);
        Set servers = SolverRegisterService.getInstance().getServers();
        synchronized (servers) {
            for (Iterator i=servers.iterator();i.hasNext();) {
                RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                if (!server.isActive()) continue;
                Hashtable serverSolvers = server.getExamSolvers();
                if (serverSolvers!=null)
                    solvers.putAll(serverSolvers);
            }
        }
        return solvers; 
    }

    public static Hashtable<String,StudentSolverProxy> getStudentSolvers() throws Exception {
        Hashtable<String,StudentSolverProxy> solvers = new Hashtable(sStudentSolvers);
        Set servers = SolverRegisterService.getInstance().getServers();
        synchronized (servers) {
            for (Iterator i=servers.iterator();i.hasNext();) {
                RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                if (!server.isActive()) continue;
                Hashtable serverSolvers = server.getStudentSolvers();
                if (serverSolvers!=null)
                    solvers.putAll(serverSolvers);
            }
        }
        return solvers; 
    }

    public static Hashtable getLocalSolvers() throws Exception {
		return sSolvers;
	}

    public static Hashtable getLocalExaminationSolvers() throws Exception {
        return sExamSolvers;
    }

    public static Hashtable getLocalStudentSolvers() throws Exception {
        return sStudentSolvers;
    }

    public void setHtmlMessageWriter(JspWriter out) {
		if (iJspWriter!=null && !iJspWriter.equals(out)) {
			try {
				iJspWriter.println("<I>Thread ended.</I>");
			} catch (Exception e) {}
		}
		iJspWriter = out;
		while (out.equals(iJspWriter)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("STOP: "+e.getMessage());
				break;
			}
		}
	}
	
	//Progress listener
    public void statusChanged(String status) {}
    public void phaseChanged(String phase) {}
    public void progressChanged(long currentProgress, long maxProgress) {}
    public void progressSaved() {}
    public void progressRestored() {}
    public void progressMessagePrinted(Progress.Message message) {
    	try {
    		if (iJspWriter!=null) {
    			String m = message.toHtmlString(getDebugLevel());
    			if (m!=null)  {
    				iJspWriter.println(m+"<br>");
    				iJspWriter.flush();
    			}
    		}
    	} catch (IOException e) {
    		System.out.println("STOP: "+e.getMessage());
    		iJspWriter = null;
    	}
    }
    
    public String getHost() {
    	return "local";
    }
    
    public String getHostLabel() {
    	return getHost();
    }
    
    private void backup() {
    	if (!sBackupWhenDone) return;
    	String puid = getProperties().getProperty("General.OwnerPuid");
    	if (puid!=null)
    		backup(SolverRegisterService.sBackupDir,puid);
    }
    
    protected void onFinish() {
    	super.onFinish();
    	backup();
    }
    
    protected void onStop() {
    	super.onStop();
    	backup();
    }

    protected void afterLoad() {
    	super.afterLoad();
    	backup();
    }
    
    protected void afterFinalSectioning() {
    	super.afterFinalSectioning();
    	backup();
    }
    
    public void restoreBest() {
    	super.restoreBest();
    	backup();
    }

    public void saveBest() {
    	super.saveBest();
    	backup();
    }

    public static void backup(File folder) {
        if (folder.exists() && !folder.isDirectory()) return;
        folder.mkdirs();
        File[] old = folder.listFiles(new BackupFileFilter(true,true));
        for (int i=0;i<old.length;i++)
            old[i].delete();
		synchronized (sSolvers) {
			for (Iterator i=sSolvers.entrySet().iterator();i.hasNext();) {
				Map.Entry entry = (Map.Entry)i.next();
				String puid = (String)entry.getKey();
				WebSolver solver =(WebSolver)entry.getValue();
				solver.backup(folder, puid);
			}
		}
        synchronized (sExamSolvers) {
            for (Iterator i=sExamSolvers.entrySet().iterator();i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String puid = (String)entry.getKey();
                ExamSolver solver =(ExamSolver)entry.getValue();
                solver.backup(folder, puid);
            }
        }
        synchronized (sStudentSolvers) {
            for (Iterator i=sStudentSolvers.entrySet().iterator();i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String puid = (String)entry.getKey();
                StudentSolver solver =(StudentSolver)entry.getValue();
                solver.backup(folder, puid);
            }
        }
	}
	
	public static void restore(File folder, File passivateFolder) {
		if (!folder.exists() || !folder.isDirectory()) return;
		synchronized (sSolvers) {
			for (Iterator i=sSolvers.values().iterator();i.hasNext();) {
				WebSolver solver =(WebSolver)i.next();
				solver.dispose();
			}
			sSolvers.clear();
			File[] files = folder.listFiles(new BackupFileFilter(true,false));
			for (int i=0;i<files.length;i++) {
				File file = files[i];
				String puid = file.getName().substring(0,file.getName().indexOf('.'));
				
                if (puid.startsWith("exam_")) {
                    String exPuid = puid.substring("exam_".length());
                    ExamSolver solver = new ExamSolver(new DataProperties(), new ExamSolverOnDispose(exPuid));
                    if (solver.restore(folder, exPuid)) {
                        if (passivateFolder!=null)
                            solver.passivate(passivateFolder,puid);
                        sExamSolvers.put(exPuid, solver);
                    }
                    continue;
                }
                
                if (puid.startsWith("sct_")) {
                    String exPuid = puid.substring("sct_".length());
                    StudentSolver solver = new StudentSolver(new DataProperties(), new StudentSolverOnDispose(exPuid));
                    if (solver.restore(folder, exPuid)) {
                        if (passivateFolder!=null)
                            solver.passivate(passivateFolder,puid);
                        sStudentSolvers.put(exPuid, solver);
                    }
                    continue;
                }
                
				WebSolver solver = new WebSolver(new DataProperties());
				if (solver.restore(folder,puid)) {
					if (passivateFolder!=null)
						solver.passivate(passivateFolder,puid);
					sSolvers.put(puid,solver);
				} 
			}
		}
	}
	
	public static void startSolverPasivationThread(File folder) {
		if (sSolverPasivationThread!=null && sSolverPasivationThread.isAlive()) return;
		sSolverPasivationThread = new SolverPassivationThread(folder, sSolvers, sExamSolvers, sStudentSolvers);
		sSolverPasivationThread.start();
	}
	
    public static void stopSolverPasivationThread() {
        if (sSolverPasivationThread!=null && sSolverPasivationThread.isAlive()) {
            sSolverPasivationThread.interrupt();
        }
    }

    public static ClassAssignmentProxy getClassAssignmentProxy(HttpSession session) {
		SolverProxy solver = getSolver(session);
		if (solver!=null) return new CachedClassAssignmentProxy(solver);
		String solutionIdsStr = (String)session.getAttribute("Solver.selectedSolutionId");
		Set solutionIds = new HashSet();
		if (solutionIdsStr!=null) {
			for (StringTokenizer s = new StringTokenizer(solutionIdsStr, ",");s.hasMoreTokens();) {
                Long solutionId = Long.valueOf(s.nextToken());
				solutionIds.add(solutionId);
			}
		}
		SolutionClassAssignmentProxy cachedProxy = (SolutionClassAssignmentProxy)session.getAttribute("LastSolutionClassAssignmentProxy");
		if (cachedProxy!=null && cachedProxy.equals(solutionIds)) {
			return cachedProxy;
		}
		SolutionClassAssignmentProxy newProxy = new SolutionClassAssignmentProxy(solutionIds);
		session.setAttribute("LastSolutionClassAssignmentProxy",newProxy);
		return newProxy;
	}
	
    public static long getAvailableMemory() {
		System.gc();
		return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory(); 
	}

	public static long getUsage() {
		int ret = 0;
		for (Iterator i=sSolvers.entrySet().iterator();i.hasNext();) {
			Map.Entry entry = (Map.Entry)i.next();
			SolverProxy solver = (SolverProxy)entry.getValue();
			ret++;
			if (!solver.isPassivated()) ret++;
			try {
				if (solver.isWorking()) ret++;
			} catch (Exception e) {};
		}
        for (Iterator i=sExamSolvers.entrySet().iterator();i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            ExamSolverProxy solver = (ExamSolverProxy)entry.getValue();
            ret++;
            if (!solver.isPassivated()) ret++;
            try {
                if (solver.isWorking()) ret++;
            } catch (Exception e) {};
        }
		return ret;
	}
	
    private static class ExamSolverOnDispose implements ExamSolverDisposeListener {
        String iOwnerId = null;
        public ExamSolverOnDispose(String ownerId) {
            iOwnerId = ownerId;
        }
        public void onDispose() {
            sExamSolvers.remove(iOwnerId);
        }
    }

    private static class StudentSolverOnDispose implements StudentSolverDisposeListener {
        String iOwnerId = null;
        public StudentSolverOnDispose(String ownerId) {
            iOwnerId = ownerId;
        }
        public void onDispose() {
            sStudentSolvers.remove(iOwnerId);
        }
    }
}
