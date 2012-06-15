/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.gwt.server;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import net.sf.cpsolver.ifs.util.DataProperties;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.commons.web.Web;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.PageNames;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.action.PersonalizedExamReportAction;
import org.unitime.timetable.form.ListSolutionsForm;
import org.unitime.timetable.gwt.services.MenuService;
import org.unitime.timetable.gwt.shared.MenuException;
import org.unitime.timetable.gwt.shared.MenuInterface;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.SavedHQL;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SolverGroup;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.UserData;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.SolverGroupDAO;
import org.unitime.timetable.model.dao.StudentDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningService;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.studentsct.StudentSolverProxy;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.RoomAvailability;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * @author Tomas Muller
 */
public class MenuServlet extends RemoteServiceServlet implements MenuService {
	private static final long serialVersionUID = 9021169012914612488L;
	private static Logger sLog = Logger.getLogger(MenuServlet.class);
    private static Element iRoot = null;
    private static PageNames sPageNames = Localization.create(PageNames.class);

	public void init() throws ServletException {
		try {
			String menu = ApplicationProperties.getProperty("unitime.menu","menu.xml");
			Document document = null;
	        URL menuUrl = ApplicationProperties.class.getClassLoader().getResource(menu);
	        SAXReader sax = new SAXReader();
	        sax.setEntityResolver(new EntityResolver() {
	        	public InputSource resolveEntity(String publicId, String systemId) {
	        		if (publicId.equals("-//UniTime//UniTime Menu DTD/EN")) {
	        			return new InputSource(ApplicationProperties.class.getClassLoader().getResourceAsStream("menu.dtd"));
	        		}
	        		return null;
	        	}
	        });
	        if (menuUrl!=null) {
	            Debug.info("Reading menu from " + URLDecoder.decode(menuUrl.getPath(), "UTF-8") + " ...");
	            document = sax.read(menuUrl.openStream());
	        } else if (new File(menu).exists()) {
	            Debug.info("Reading menu from " + menu + " ...");
	            document = sax.read(new File(menu));
	        }
	        if (document==null)
	            throw new ServletException("Unable to create menu, reason: resource " + menu + " not found.");

	        if (!"unitime-menu".equals(document.getRootElement().getName())) throw new ServletException("Menu has an unknown format.");
	        iRoot = document.getRootElement();
	        
	        String customMenu = ApplicationProperties.getProperty("unitime.menu.custom","menu-custom.xml");
			Document customDocument = null;
	        URL customMenuUrl = ApplicationProperties.class.getClassLoader().getResource(customMenu);
	        if (customMenuUrl!=null) {
	            Debug.info("Reading custom menu from " + URLDecoder.decode(customMenuUrl.getPath(), "UTF-8") + " ...");
	            customDocument = sax.read(customMenuUrl.openStream());
	        } else if (new File(customMenu).exists()) {
	            Debug.info("Reading custom menu from " + customMenu + " ...");
	            customDocument = sax.read(new File(customMenu));
	        }
	        if (customDocument != null) {
	        	merge(iRoot, customDocument.getRootElement());
	        }
	        
		} catch (Exception e) {
			if (e instanceof ServletException) throw (ServletException)e;
			throw new ServletException("Unable to initialize, reason: "+e.getMessage(), e);
		}
	}
	
	private void merge(Element menu, Element custom) {
		if ("remove".equals(custom.getName())) {
			menu.getParent().remove(menu);
			return;
		}
		for (Iterator<Attribute> i = custom.attributeIterator(); i.hasNext();) {
			Attribute a = i.next();
			menu.addAttribute(a.getName(), a.getValue());
		}
		for (Iterator<Element> i = custom.elementIterator(); i.hasNext(); ) {
			Element e = i.next();
			if ("parameter".equals(e.getName())) {
				for (Iterator<Element> j = menu.elementIterator("parameter"); j.hasNext(); ) {
					menu.remove(j.next());
				}
				menu.add(e.createCopy());
				continue;
			}
			if ("condition".equals(e.getName())) {
				menu.add(e.createCopy());
				continue;
			}
			if ("new-condition".equals(e.getName())) {
				for (Iterator<Element> j = menu.elementIterator("condition"); j.hasNext(); ) {
					menu.remove(j.next());
				}
				Element f = e.createCopy();
				f.setName("condition");
				menu.add(f);
				continue;
			}
			String name = e.attributeValue("name");
			Element x = null;
			if (name != null) {
				for (Iterator<Element> j = menu.elementIterator(); j.hasNext(); ) {
					Element f = j.next();
					if (name.equals(f.attributeValue("name"))) { x = f; break; }
				}
			}
			if (x != null) {
				merge(x, e);
			} else {
				int pos = Integer.parseInt(e.attributeValue("position", "-1"));
				if (pos >= 0) {
					List<Element> after = new ArrayList<Element>();
					for (Iterator<Element> j = menu.elementIterator(); j.hasNext(); ) {
						Element f = j.next();
						if ("condition".equals(f.getName())) continue;
						if (pos > 0) {
							pos--;
						} else {
							after.add(f);
							menu.remove(f);
						}
					}
					menu.add(e.createCopy());
					for (Element f: after)
						menu.add(f);
				} else
					menu.add(e.createCopy());
			}
		}
	}
	
	public List<MenuInterface> getMenu() throws MenuException {
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				// init();
				
				List<MenuInterface> menu = new ArrayList<MenuInterface>();
				if (iRoot == null) throw new MenuException("menu is not configured properly");
				
				UserInfo user = new UserInfo(getThreadLocalRequest().getSession());
				
				for (Iterator<Element> i = iRoot.elementIterator(); i.hasNext(); ) {
					Element element = i.next();
					MenuInterface m = getMenu(user, element);
					if (m != null) menu.add(m);
				}

				if (menu.isEmpty())
					throw new MenuException("no menu");
				
				return menu;
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}
	
	private MenuInterface getMenu(UserInfo user, Element menuElement) {
		try {
		MenuInterface menu = new MenuInterface();
		String name = menuElement.attributeValue("name");
		String localizedName = (name == null ? null : sPageNames.translateMessage(
				name.trim().replace(' ', '_').replace("(", "").replace(")", "").replace(':', '_'), null));
		menu.setName(localizedName == null ? name : localizedName);
		menu.setTitle(menuElement.attributeValue("title"));
		menu.setTarget(menuElement.attributeValue("target"));
		menu.setPage(menuElement.attributeValue("page"));
		menu.setHash(menuElement.attributeValue("hash"));
		String type = menuElement.attributeValue("type");
		if ("gwt".equals(type)) menu.setGWT(true);
		if ("property".equals(type) && menu.getPage() != null) {
			menu.setPage(ApplicationProperties.getProperty(menu.getPage()));
			if (menu.getPage() == null) return null;
		}
		
		for (Iterator<Element> i = menuElement.elementIterator(); i.hasNext(); ) {
			Element element = i.next();
			if ("condition".equals(element.getName())) {
				if (!check(user, element)) return null;
			} else if ("parameter".equals(element.getName())) {
				menu.addParameter(element.attributeValue("name"), element.attributeValue("value", element.getText()));
			} else {
				MenuInterface m = getMenu(user, element);
				if (m != null) menu.addSubMenu(m);
			}
		}
		return menu;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean check(UserInfo userInfo, Element conditionElement) {
		String cond = conditionElement.getName();
		if ("and".equals(cond) || "condition".equals(cond)) {
			for (Iterator<Element> i = conditionElement.elementIterator(); i.hasNext(); ) {
				Element element = i.next();
				if (!check(userInfo, element)) return false;
			}
			return true;
		} else  if ("or".equals(cond)) {
			for (Iterator<Element> i = conditionElement.elementIterator(); i.hasNext(); ) {
				Element element = i.next();
				if (check(userInfo, element)) return true;
			}
			return false;
		} else if ("not".equals(cond)) {
			for (Iterator<Element> i = conditionElement.elementIterator(); i.hasNext(); ) {
				Element element = i.next();
				if (check(userInfo, element)) return false;
			}
			return true;
		} else if ("isAuthenticated".equals(cond)) {
			return userInfo.getUser() != null;
		} else if ("hasRole".equals(cond)) {
			User user = userInfo.getUser();
			if (user == null) return false;
			String role = conditionElement.attributeValue("name");
			if (role == null) return user.getRole() != null; // has any role
			return role.equalsIgnoreCase(user.getRole());
		} else if ("propertyEquals".equals(cond)) {
			return conditionElement.attributeValue("value", "true").equalsIgnoreCase(ApplicationProperties.getProperty(
					conditionElement.attributeValue("name", "dummy"),
					conditionElement.attributeValue("defaultValue", "false")));
		} else if ("hasProperty".equals(cond)) {
			return ApplicationProperties.getProperty(conditionElement.attributeValue("name", "dummy")) != null;
		} else if ("hasRight".equals(cond)) {
			String right = conditionElement.attributeValue("name", "unknown");
			if ("canSeeEvents".equals(right)) {
				return userInfo.getUser() != null && TimetableManager.canSeeEvents(userInfo.getUser());
			} else if ("hasRoomAvailability".equals(right)) {
				return RoomAvailability.getInstance() != null;
			} else if ("hasPersonalReport".equals(right)) {
				return userInfo.getUser() != null && PersonalizedExamReportAction.hasPersonalReport(userInfo.getUser());
			} else if ("isChameleon".equals(right)) {
				return getThreadLocalRequest().getSession().getAttribute("hdnAdminAlias")!=null && getThreadLocalRequest().getSession().getAttribute("hdnAdminAlias").toString().equals("1");
			} else if ("isSectioningEnabled".equals(right)) {
				return OnlineSectioningService.isEnabled();
			} else if ("isStudent".equals(right)) {
				return userInfo != null && userInfo.isStudent();
			} else if ("isInstructor".equals(right)) {
				return userInfo != null && userInfo.isInstructor();
			} else if ("isRegistrationEnabled".equals(right)) {
				return OnlineSectioningService.isRegistrationEnabled();
			} else {
				User user = userInfo.getUser();
				if (user == null) return false;
				TimetableManager manager = userInfo.getManager();
				if (manager == null) return false;
				Session session = userInfo.getSession();
				if (session == null) return false;
				if ("canSeeCourses".equals(right)) {
					return manager.canSeeCourses(session, user);
				} else if ("canSeeTimetable".equals(right)) {
					return manager.canSeeTimetable(session, user);
				} else if ("canDoTimetable".equals(right)) {
					return manager.canDoTimetable(session, user);
				} else if ("hasASolverGroup".equals(right)) {
					return manager.hasASolverGroup(session, user);
				} else if ("canSectionStudents".equals(right)) {
					return manager.canSectionStudents(session, user);
				} else if ("canSeeExams".equals(right)) {
					return manager.canSeeExams(session, user);
				} else if ("canTimetableExams".equals(right)) {
					return manager.canTimetableExams(session, user);
				} else if ("canAudit".equals(right)) {
					return manager.canAudit(session, user);
				} else if ("hasCourseReports".equals(right)) {
					return SavedHQL.hasQueries(SavedHQL.Flag.APPEARANCE_COURSES, user.isAdmin());
				} else if ("hasExamReports".equals(right)) {
					return SavedHQL.hasQueries(SavedHQL.Flag.APPEARANCE_EXAMS, user.isAdmin());
				} else if ("hasEventReports".equals(right)) {
					return SavedHQL.hasQueries(SavedHQL.Flag.APPEARANCE_EVENTS, user.isAdmin());
				} else if ("hasStudentReports".equals(right)) {
					return SavedHQL.hasQueries(SavedHQL.Flag.APPEARANCE_SECTIONING, user.isAdmin());
				}
			}
			sLog.warn("Unknown right " + right + ".");
			return true;
		}
		sLog.warn("Unknown condition " + cond + ".");
		return true;
	}
	
	public static class UserInfo {
		User iUser = null;
		Session iSession = null;
		TimetableManager iManager = null;

		public UserInfo(HttpSession session) {
			iUser = Web.getUser(session);
			if (iUser != null) {
				Long sessionId = (Long) iUser.getAttribute(Constants.SESSION_ID_ATTR_NAME);
				if (sessionId != null) {
					iSession = SessionDAO.getInstance().get(sessionId);
				}
				iManager = TimetableManager.getManager(iUser);
			}
		}
		
		public User getUser() { return iUser; }
		public Session getSession() { return iSession; }
		public TimetableManager getManager() { return iManager; }
		public boolean isStudent() {
			if (getUser() == null) return false;
			return ((Number)StudentDAO.getInstance().getSession().createQuery("select count(s) from Student s where " +
					"s.externalUniqueId = :uid")
					.setString("uid", getUser().getId()).setCacheable(true).uniqueResult()).intValue() > 0;
		}
		public boolean isInstructor() {
			if (getUser() == null) return false;
			return ((Number)StudentDAO.getInstance().getSession().createQuery("select count(s) from DepartmentalInstructor s where " +
					"s.externalUniqueId = :uid")
					.setString("uid", getUser().getId()).setCacheable(true).uniqueResult()).intValue() > 0;
		}
		
	}
	
	public HashMap<String, String> getUserInfo() throws MenuException {
		try {
			HashMap<String, String> ret = new HashMap<String, String>();
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				
				UserInfo user = new UserInfo(getThreadLocalRequest().getSession());
				if (user.getUser() == null) 
					return null;
				
		 		String userName = Constants.toInitialCase(user.getUser().getName(), "-".toCharArray());

				ret.put("0Name", userName);
				
				String dept = "";
		 		TimetableManager manager = user.getManager();
		 		if (manager!=null) {
		 			for (Iterator i=manager.getDepartments().iterator();i.hasNext();) {
		 				Department d = (Department)i.next();
		 				if (d.getSessionId().equals(user.getSession().getUniqueId())) {
		 					if (dept.length()>0) dept += ",";
		 					dept += "<span title='"+d.getHtmlTitle()+"'>"+d.getShortLabel()+"</span>";
		 				}
		 			}
		 		} else {
		 			TreeSet depts = new TreeSet(user.getUser().getDepartments());
		 			for (Iterator i=depts.iterator();i.hasNext();) {
		 				dept += i.next().toString();
		 				if (i.hasNext()) dept += ",";
		 			}
		 		}
		 		ret.put("1Dept", dept);
		 		
		 		String role = user.getUser().getRole();
		 		if (role==null) role = "No Role";
		 		ret.put("2Role", role);
		 		
		 		if (user.getUser() != null && Roles.ADMIN_ROLE.equals(user.getUser().getRole()) || 
		 			(getThreadLocalRequest().getSession().getAttribute("hdnAdminAlias")!=null && getThreadLocalRequest().getSession().getAttribute("hdnAdminAlias").toString().equals("1")))
		 			ret.put("Chameleon", "");
		 		
			} finally {
				hibSession.close();
			}
			return ret;
		} catch (Exception e) {
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}
	
	public HashMap<String, String> getSessionInfo() throws MenuException {
		try {
			HashMap<String, String> ret = new HashMap<String, String>();
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				
				UserInfo user = new UserInfo(getThreadLocalRequest().getSession());
		 		if (user.getSession() == null)
		 			return null;
		 		
	 			ret.put("0Session", user.getSession().getLabel());
	 			ret.put("1Status", user.getSession().getStatusType().getLabel());
		 		
		 		ret.put("2Database", HibernateUtil.getDatabaseName());
		 		
			} finally {
				hibSession.close();
			}
			return ret;
		} catch (Exception e) {
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}
	
	public String getVersion() throws MenuException {
 		return "Version " + Constants.getVersion() + " built on " + Constants.getReleaseDate();
	}
	
	private String getName(String puid) {
		return getName(TimetableManager.findByExternalId(puid));
	}

	private String getName(TimetableManager mgr) {
		if (mgr==null) return null;
		return mgr.getShortName();
	}

	private String getName(SolverGroup gr) {
		if (gr==null) return null;
		return gr.getAbbv();
	}
	
	public HashMap<String, String> getSolverInfo(boolean includeSolutionInfo) throws MenuException {
		try {
			HashMap<String, String> ret = new HashMap<String, String>();
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				
				SolverProxy solver = WebSolver.getSolver(getThreadLocalRequest().getSession());
				ExamSolverProxy examSolver = (solver==null?WebSolver.getExamSolverNoSessionCheck(getThreadLocalRequest().getSession()):null);
				StudentSolverProxy studentSolver = (solver==null && examSolver==null?WebSolver.getStudentSolverNoSessionCheck(getThreadLocalRequest().getSession()):null); 
				
				
				Map progress = (studentSolver!=null?studentSolver.getProgress():examSolver!=null?examSolver.getProgress():solver!=null?solver.getProgress():null);
				if (progress == null) return null;
				
				DataProperties properties = (studentSolver!=null?studentSolver.getProperties():examSolver!=null?examSolver.getProperties():solver.getProperties());
				String progressStatus = (String)progress.get("STATUS");
				String progressPhase = (String)progress.get("PHASE");
				long progressCur = ((Long)progress.get("PROGRESS")).longValue();
				long progressMax = ((Long)progress.get("MAX_PROGRESS")).longValue();
				String version = (String)progress.get("VERSION");
				if (version==null || "-1".equals(version)) version = "N/A";
				double progressPercent = 100.0*((double)(progressCur<progressMax?progressCur:progressMax))/((double)progressMax);
				String runnerName = getName(properties.getProperty("General.OwnerPuid","N/A"));
				Long[] solverGroupId = properties.getPropertyLongArry("General.SolverGroupId",null);
				String ownerName = "";
				if (solverGroupId!=null) {
					for (int i=0;i<solverGroupId.length;i++) {
						if (i>0) ownerName += " & ";
						ownerName += getName((new SolverGroupDAO()).get(solverGroupId[i]));
					}
				}
				if (examSolver!=null) ownerName = Exam.sExamTypes[examSolver.getExamType()];
				if (ownerName==null || ownerName.length()==0)
					ownerName = "N/A";
				if (ownerName.equals("N/A"))
					ownerName = runnerName;
				if (runnerName.equals("N/A"))
					runnerName = ownerName;
				if (!ownerName.equals(runnerName))
					ownerName = runnerName+" as "+ownerName;
				if (ownerName.length() > 50)
					ownerName = ownerName.substring(0,47) + "...";

				ret.put("0Type",  (studentSolver!=null?"Student Sectioning Solver":examSolver!=null?"Examinations Solver":"Course Timetabling Solver"));
				ret.put("4Owner", ownerName);
				ret.put("5Host", (studentSolver!=null?studentSolver.getHostLabel():examSolver!=null?examSolver.getHostLabel():solver.getHostLabel()));
				ret.put("1Solver", progressStatus);
				ret.put("2Phase", progressPhase);
				if (progressMax>0)
					ret.put("3Progress", (progressCur<progressMax?progressCur:progressMax) + " of " + progressMax + " (" + Web.format(progressPercent) + "%)");
				ret.put("7Version", version);
				ret.put("6Session", SessionDAO.getInstance().get(properties.getPropertyLong("General.SessionId",null)).getLabel());
				
				if (includeSolutionInfo) {
					Map<String,String> info = null;
					if (solver != null) {
						info = solver.statusSolutionInfo();
					} else if (examSolver != null) {
						info = examSolver.statusSolutionInfo();
					} else if (studentSolver != null) {
						info = studentSolver.statusSolutionInfo();
					}
					
					if (info != null) {
						TreeSet<String> keys = new TreeSet<String>(new ListSolutionsForm.InfoComparator());
						keys.addAll(info.keySet());
						int idx = 0;
						for (String key: keys) {
							ret.put((char)('A' + idx) + key, (String)info.get(key));
							idx++;
						}
					}
				}
		 		
			} finally {
				hibSession.close();
			}
			return ret;
		} catch (Exception e) {
			sLog.warn("Unable to get solver info: " + e.getMessage());
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}
	
	public String[] getHelpPageAndLocalizedTitle(String title) throws MenuException {
		String name = title.trim().replace(' ', '_').replace("(", "").replace(")", "").replace(':', '_');
		String help = null;
		if ("true".equals(ApplicationProperties.getProperty("tmtbl.wiki.help", "true")) && ApplicationProperties.getProperty("tmtbl.wiki.url") != null) {
			help = ApplicationProperties.getProperty("tmtbl.wiki.url") + name;
		}
		return new String[] {
				help,
				sPageNames.translateMessage(name, title)
				};
	}
	
	public String getUserData(String property) throws MenuException {
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				return UserData.getProperty(getThreadLocalRequest().getSession(), property);
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}
	
	public Boolean setUserData(String property, String value) throws MenuException {
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				UserData.setProperty(getThreadLocalRequest().getSession(), property, value);
				return null;
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}
	
	public HashMap<String, String> getUserData(Collection<String> property) throws MenuException {
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				return UserData.getProperties(getThreadLocalRequest().getSession(), property);
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}
	
	public Boolean setUserData(List<String[]> property2value) throws MenuException {
		try {
			org.hibernate.Session hibSession = SessionDAO.getInstance().getSession();
			try {
				for (String[] p: property2value)
					UserData.setProperty(getThreadLocalRequest().getSession(), p[0], p[1]);
				return null;
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			if (e instanceof MenuException) throw (MenuException)e;
			throw new MenuException(e.getMessage());
		}
	}


}
