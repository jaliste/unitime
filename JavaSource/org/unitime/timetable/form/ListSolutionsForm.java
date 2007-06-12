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

import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.form.SolverForm.LongIdValue;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Solution;
import org.unitime.timetable.model.SolverGroup;
import org.unitime.timetable.model.SolverPredefinedSetting;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.dao.SolutionDAO;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.WebSolver;
import org.unitime.timetable.solver.remote.RemoteSolverServerProxy;
import org.unitime.timetable.solver.remote.SolverRegisterService;
import org.unitime.timetable.solver.ui.LogInfo;
import org.unitime.timetable.solver.ui.PropertiesInfo;

import net.sf.cpsolver.ifs.util.Progress;

/** 
 * @author Tomas Muller
 */
public class ListSolutionsForm extends ActionForm {
	private static SimpleDateFormat sDF = new SimpleDateFormat("MM/dd/yy hh:mmaa");
	private Vector iMessages = new Vector();
	private String iOp = null;
	private Long iEmptySetting = null;
	private Vector iSettings = null;
	private SolverProxy iSolver = null;
	private Vector iHosts = new Vector();
	private String iHost = null;
	private String iHostEmpty = null;
	private boolean iCanDo = true;
	private Vector iOwners = null;
	private boolean iSelectOwner = false;
	private Long iOwnerId = null;
	private Vector iSolutionBeans = new Vector();
	private int iSelectedSolutionBean = -1;
	private Long iSetting = null;
	private String iNote = null;
	private boolean iChangeTab = false;
	private boolean iViewOnly = false;
	
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        
        if (iSelectedSolutionBean<0 || iSelectedSolutionBean>=iSolutionBeans.size()) {
        	errors.add("selectedSolutionBean", new ActionMessage("errors.general", "No solution selected"));
        }
        
        return errors;
	}
	
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		iCanDo = false;
		iChangeTab = false;
		iOp = null; iNote = null;
		iSolutionBeans.clear(); iSelectedSolutionBean = -1;
		iSettings = SolverPredefinedSetting.getIdValueList(SolverPredefinedSetting.APPEARANCE_TIMETABLES);
		iEmptySetting = null;
		iSetting = null;
		if (iSettings!=null && !iSettings.isEmpty()) {
			iEmptySetting = ((SolverPredefinedSetting.IdValue)iSettings.firstElement()).getId();
			iSetting = iEmptySetting; 
		}
		iSolver = WebSolver.getSolver(request.getSession());
		iMessages.clear();
		SolverProxy solver = WebSolver.getSolver(request.getSession());
		iHost = (solver==null?"auto":solver.getHost());
		iHostEmpty = iHost;
		iHosts.clear();
		User user = Web.getUser(request.getSession());
		iViewOnly = user.getCurrentRole().equals(Roles.VIEW_ALL_ROLE);
		if (user.isAdmin()) {
            Set servers = SolverRegisterService.getInstance().getServers();
            synchronized (servers) {
                for (Iterator i=servers.iterator();i.hasNext();) {
                    RemoteSolverServerProxy server = (RemoteSolverServerProxy)i.next();
                    if (server.isActive())
                        iHosts.addElement(server.getAddress().getHostName()+":"+server.getPort());
                }
            }
			Collections.sort(iHosts);
			if (ApplicationProperties.isLocalSolverEnabled())
				iHosts.insertElementAt("local",0);
			iHosts.insertElementAt("auto",0);
		}
		
		iSelectOwner = true;
		iOwnerId = null; iOwners = null;
		try {
			TimetableManager manager = (user==null?null:TimetableManager.getManager(user)); 
			Session acadSession = (user==null?null:Session.getCurrentAcadSession(user));
			iCanDo = manager.canDoTimetable(acadSession, user);

			iOwners = new Vector();
			if (Web.hasRole(request.getSession(), Roles.getAdminRoles())) {
				for (Iterator i=SolverGroup.findBySessionId(acadSession.getUniqueId()).iterator();i.hasNext();) {
					SolverGroup owner = (SolverGroup)i.next();
					iOwners.add(new LongIdValue(owner.getUniqueId(),owner.getName()));
				}
			} else {
				if (manager.canDoTimetable(acadSession, user)) {
					for (Iterator i=manager.getSolverGroups(acadSession).iterator();i.hasNext();) {
						SolverGroup owner = (SolverGroup)i.next();
						if (owner.canTimetable())
							iOwners.add(new LongIdValue(owner.getUniqueId(),owner.getName()));
					}
				} else {
					for (Iterator i=manager.getSolverGroups(acadSession).iterator();i.hasNext();) {
						SolverGroup owner = (SolverGroup)i.next();
						if (owner.canAudit())
							iOwners.add(new LongIdValue(owner.getUniqueId(),owner.getName()));
					}
				}
			}
			Collections.sort(iOwners);
		} catch (Exception e) {
			Debug.error(e);
		}
		iSelectOwner = (iOwners!=null && iOwners.size()>1);
	}
	
	public void setOp(String op) { iOp = op; }
	public String getOp() { return iOp; }
	public void setNote(String note) { iNote = note; }
	public String getNote() { return iNote; }
	public Vector getSettings() { return iSettings; }
	public void setSettings(Vector settings) { iSettings = settings; }
	public Long getEmptySetting() { return iEmptySetting; }
	public void setEmptySetting(Long setting) { iEmptySetting = setting; }
	public boolean getHasSettings() { return (iSettings!=null && !iSettings.isEmpty()); }
	public void setSolver(SolverProxy solver) { iSolver = solver; }
	public SolverProxy getSolver() { return iSolver; }
	public String getSolverNote() throws Exception {
		if (iSolver==null) return null;
		return iSolver.getNote();
	}
	public void setSolverNote(String note) throws Exception {
		if (iSolver!=null) iSolver.setNote(note);
	}

	public static class InfoComparator implements Comparator {
		private static Vector sInfoKeys = null;
		static {
			sInfoKeys = new Vector();
			sInfoKeys.add("Assigned variables");
			sInfoKeys.add("Overall solution value");
			sInfoKeys.add("Time preferences");
			sInfoKeys.add("Student conflicts");
			sInfoKeys.add("Room preferences");
			sInfoKeys.add("Distribution preferences");
			sInfoKeys.add("Back-to-back instructor preferences");
			sInfoKeys.add("Too big rooms");
			sInfoKeys.add("Useless half-hours");
			sInfoKeys.add("Same subpart balancing penalty");
			sInfoKeys.add("Department balancing penalty");
			sInfoKeys.add("Perturbation penalty");
			sInfoKeys.add("Perturbation variables");
			sInfoKeys.add("Time");
			sInfoKeys.add("Iteration");
			sInfoKeys.add("Memory usage");
			sInfoKeys.add("Speed");
		}
		public int compare(Object o1, Object o2) {
			String key1 = (String)o1;
			String key2 = (String)o2;
			int i1 = sInfoKeys.indexOf(key1);
			int i2 = sInfoKeys.indexOf(key2);
			if (i1<0) {
				if (i2<0) return key1.compareTo(key2);
				else return 1;
			} else if (i2<0) return -1;
			return (i1<i2?-1:1);
		}
	}
	public Vector getMessages() { return iMessages; }
	public Collection getHosts() {
		return iHosts;
	}
	public String getHost() {
		return iHost;
	}
	public void setHost(String host) {
		iHost = host;
	}
	public String getHostEmpty() {
		return iHostEmpty;
	}
	public void setHostEmpty(String hostEmpty) {
		iHostEmpty = hostEmpty;
	}
	public boolean getCanDo() { return iCanDo; }
	
	public Long getOwnerId() { return iOwnerId; }
	public void setOwnerId(Long ownerId) { iOwnerId = ownerId; }
	public boolean getSelectOwner() { return iSelectOwner; }
	public Vector getOwners() { return iOwners; }
	public Long getSetting() { return iSetting; }
	public void setSetting(Long setting) { iSetting = setting; }
	
	public Vector getSolutionBeans() { return iSolutionBeans; }
	public void setSolutionBeans(Vector solutionBeans) { iSolutionBeans = solutionBeans; }
	public int getSelectedSolutionBean() { return iSelectedSolutionBean; }
	public void setSelectedSolutionBean(int selectedSolutionBean) { iSelectedSolutionBean = selectedSolutionBean; }
	public void addSolution(Solution solution, User user) throws Exception {
		SolutionBean solutionBean = null;
		for (Enumeration e=iSolutionBeans.elements();e.hasMoreElements();) {
			SolutionBean sb = (SolutionBean)e.nextElement();
			if (sb.getOwnerId().equals(solution.getOwner().getUniqueId())) {
				solutionBean = sb; break;
			}
		}
		if (solutionBean!=null) iSolutionBeans.remove(solutionBean);
		iSolutionBeans.addElement(new SolutionBean(solution, user));
	}
	public String getSolutionId() {
		String solutionId = "";
		for (Enumeration e=iSolutionBeans.elements();e.hasMoreElements();) {
			SolutionBean sb = (SolutionBean)e.nextElement();
			solutionId += sb.getUniqueId().toString();
			if (e.hasMoreElements()) solutionId += ",";
		}
		return solutionId;
	}
	public void setSolutionId(String solutionId, User user) throws Exception {
		iSolutionBeans.clear();
		if (solutionId==null || solutionId.length()==0) return;
		for (StringTokenizer s=new StringTokenizer(solutionId,",");s.hasMoreTokens();) {
			Long id = Long.valueOf(s.nextToken());
			Solution solution = (new SolutionDAO()).get(id);
			if (solution!=null)
				iSolutionBeans.addElement(new SolutionBean(solution, user));
		}
	}
	public void removeSolution(Long solutionId) {
		SolutionBean solutionBean = null;
		for (Enumeration e=iSolutionBeans.elements();e.hasMoreElements();) {
			SolutionBean sb = (SolutionBean)e.nextElement();
			if (sb.getUniqueId().equals(solutionId)) {
				solutionBean = sb; break;
			}
		}
		if (solutionBean!=null) iSolutionBeans.remove(solutionBean);
	}
	public SolutionBean getSolutionBean() {
		if (iSelectedSolutionBean<0) return null;
		try {
			return (SolutionBean)iSolutionBeans.elementAt(iSelectedSolutionBean);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	public SolutionBean getSolutionBean(Long solutionId) {
		for (Enumeration e=iSolutionBeans.elements();e.hasMoreElements();) {
			SolutionBean sb = (SolutionBean)e.nextElement();
			if (sb.getUniqueId().equals(solutionId))return sb;
		}
		return null;
	}
	public Long[] getOwnerIds() {
		Long[] ret = new Long[iSolutionBeans.size()];
		for (int i=0;i<iSolutionBeans.size();i++)
			ret[i] = ((SolutionBean)iSolutionBeans.elementAt(i)).getOwnerId();
		return ret;
	}
	
	public static class SolutionBean {
		private Long iUniqueId = null;
		private String iCreated = null;
		private String iCommited = null;
		private String iOwner = null;
		private String iNote = null;
		private boolean iValid = false;
		private String iLog = null;
		private Properties iGlobalInfo = null;
		private Long iOwnerId = null;
		private boolean iCanCommit = true;
		
		public SolutionBean()  {}
		public SolutionBean(Solution solution, User user) throws Exception {
			this();
			setSolution(solution, user);
		}
		
		public void setSolution(Solution solution, User user) throws Exception {
			iUniqueId = solution.getUniqueId();
			iGlobalInfo = (PropertiesInfo)solution.getInfo("GlobalInfo");
			LogInfo logInfo = (LogInfo)solution.getInfo("LogInfo");
			iLog = (logInfo==null?null:logInfo.getHtmlLog(Progress.MSGLEVEL_WARN, false, "Loading input data ..."));
			iCreated = sDF.format(solution.getCreated());
			iCommited = (solution.isCommited().booleanValue()?sDF.format(solution.getCommitDate()):"");
			iNote = solution.getNote();
			iOwner = solution.getOwner().getName();
			iOwnerId = solution.getOwner().getUniqueId();
			iValid = solution.isValid().booleanValue();
			iCanCommit = solution.getOwner().canCommit(user);
		}
		
		public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }
		public Long getUniqueId() { return iUniqueId; }
		public void setCreated(String created) { iCreated = created; }
		public String getCreated() { return iCreated; }
		public void setCommited(String commited) { iCommited = commited; }
		public String getCommited() { return iCommited; }
		public void setOwner(String owner) { iOwner = owner; }
		public String getOwner() { return iOwner; }
		public void setOwnerId(Long ownerId) { iOwnerId = ownerId; }
		public Long getOwnerId() { return iOwnerId; }
		public void setNote(String note) { iNote = note; }
		public String getNote() { return iNote; }
		public void setValid(boolean valid) { iValid = valid; }
		public boolean getValid() { return iValid; }
		public String getLog() { return iLog; }
		public void setLog(String log) { iLog = log; }
		public void setGlobalInfo(Properties globalInfo) {
			iGlobalInfo = globalInfo; 
		}
		public Properties getGlobalInfo() { return iGlobalInfo; }
		public String getInfo(String key) {
			try {
				return iGlobalInfo.getProperty(key);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		public void setInfo(String key, String value) { iGlobalInfo.setProperty(key, value); }
		public Collection getInfos() {
			if (iGlobalInfo==null) return new Vector();
			Vector infos = new Vector(iGlobalInfo.keySet());
			Collections.sort(infos, new InfoComparator());
			return infos;
		}
		public boolean getCanCommit() { return iCanCommit; }
		public void setCanCommit(boolean canCommit) { iCanCommit = canCommit; }
		public int hashCode() { return iUniqueId.hashCode(); }
		public boolean equals(Object o) {
			if (o==null || !(o instanceof SolutionBean)) return false;
			return getUniqueId().equals(((SolutionBean)o).getUniqueId());
		}
	}
	public boolean isChangeTab() { return iChangeTab;}
	public void setChangeTab(boolean changeTab) { iChangeTab = changeTab; }
	public boolean isViewOnly() { return iViewOnly; }
}

