<%--
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008 - 2010, UniTime LLC
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
--%>
<%@ page language="java" autoFlush="true"%>
<%@ page import="java.util.*" %>
<%@ page import="org.unitime.timetable.solver.WebSolver" %>
<%@ page import="org.unitime.timetable.solver.studentsct.StudentSolverProxy" %>
<%@ page import="org.hibernate.Transaction" %>
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.solver.ui.PropertiesInfo" %>
<%@ page import="net.sf.cpsolver.ifs.util.Progress" %>
<%@ page import="org.unitime.timetable.solver.ui.LogInfo" %>
<%@ page import="org.unitime.commons.User" %>
<%@ page import="org.unitime.timetable.webutil.JavascriptFunctions" %>
<%@page import="org.unitime.timetable.model.Session"%>
<%@page import="org.unitime.timetable.form.ListSolutionsForm"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<tt:back-mark back="true" clear="true" title="Student Sectioning Solver" uri="studentSolver.do"/>
<tt:confirm name="confirmUnload">Do you really want to unload your current student schedule? You may lose this student schedule if you did not save it.</tt:confirm>
<tt:confirm name="confirmClear">Do you really want to clear your current student schedule? You may lose this student schedule if you did not save it.</tt:confirm>
<tt:confirm name="confirmSave">Do you really want to save your current student schedule? This will overwrite your previous student schedule.</tt:confirm>
<tt:confirm name="confirmSaveAsNew">Do you really want to save your current student schedule?</tt:confirm>
<tiles:importAttribute />

<html:form action="/studentSolver">
<%
try {
%>
<%
	User user = Web.getUser(session); 
	StudentSolverProxy solver = WebSolver.getStudentSolver(session);
	String status = null;
	String progress = null;
	boolean hasSolution = Session.getCurrentAcadSession(user).hasStudentSchedule();
	if (solver!=null) {
		Map p = solver.getProgress();
		status = (String)p.get("STATUS");
		long progressMax = ((Long)p.get("MAX_PROGRESS")).longValue();
		if (progressMax>0) {
			progress = (String)p.get("PHASE");
			long progressCur = ((Long)p.get("PROGRESS")).longValue();
			double progressPercent = 100.0*((double)(progressCur<progressMax?progressCur:progressMax))/((double)progressMax);
			progress+=" ("+Web.format(progressPercent)+"%)";
		}
	}
	if (status==null)
		status = "Solver not started.";
%>
<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
	<TR>
		<TD colspan="2">
			<DIV class="WelcomeRowHead">
			Solver
			</DIV>
		</TD>
	</TR>
<%
	if (solver!=null && solver.getLoadedDate()!=null) {
%>
		<TR><TD>Input data loaded:</TD><TD><%=WebSolver.sDF.format(solver.getLoadedDate())%></TD></TR>
<%
	}
%>
	<TR><TD>Status:</TD><TD><%=status%> <tt:wiki>Solver Status</tt:wiki></TD></TR>
<%  if (progress!=null) { %>
		<TR><TD>Progress:</TD><TD><%=progress%></TD></TR>
<%  } %>
<%  
	boolean disabled = (solver!=null && solver.isWorking());
%>
   	<TR><TD>Solver configuration:</TD>
		<TD>
			<html:select property="setting" onchange="submit();" disabled="<%=disabled%>">
				<html:optionsCollection name="studentSolverForm" property="settings" label="value" value="id"/>
			</html:select>
			&nbsp;<html:errors property="setting"/>
		</TD>
	</TR>
	<logic:iterate id="parameter" name="studentSolverForm" property="parameters" type="org.unitime.timetable.model.SolverPredefinedSetting.IdValue">
		<TR>
			<TD><bean:write name="parameter" property="value"/>:</TD>
			<TD>
			<logic:equal name="parameter" property="type" value="boolean">
				<html:checkbox property='<%= "parameterValue["+parameter.getId()+"]"%>' disabled="<%=disabled || parameter.getDisabled()%>"/>
  				&nbsp;<html:errors property='<%= "parameterValue["+parameter.getId()+"]"%>'/>
			</logic:equal>
			<% if (parameter.getType().startsWith("enum(") && parameter.getType().endsWith(")")) { %>
				<html:select property='<%="parameterValue["+parameter.getId()+"]"%>' disabled="<%=disabled || parameter.getDisabled()%>">
					<html:options property='<%=parameter.getType()%>'/>
				</html:select>
				&nbsp;<html:errors property='<%="parameterValue["+parameter.getId()+"]"%>'/>
			<% } %>
			<logic:equal name="parameter" property="type" value="double">
				<html:text property='<%="parameterValue["+parameter.getId()+"]"%>' size="10" maxlength="10" disabled="<%=disabled || parameter.getDisabled()%>"/>
  				&nbsp;<html:errors property='<%="parameterValue["+parameter.getId()+"]"%>'/>
  			</logic:equal>
			<logic:equal name="parameter" property="type" value="integer">
				<html:text property='<%="parameterValue["+parameter.getId()+"]"%>' size="10" maxlength="10" disabled="<%=disabled || parameter.getDisabled()%>"/>
  				&nbsp;<html:errors property='<%="parameterValue["+parameter.getId()+"]"%>'/>
  			</logic:equal>
			<logic:equal name="parameter" property="type" value="long">
				<html:text property='<%="parameterValue["+parameter.getId()+"]"%>' size="10" maxlength="10" disabled="<%=disabled || parameter.getDisabled()%>"/>
  				&nbsp;<html:errors property='<%="parameterValue["+parameter.getId()+"]"%>'/>
  			</logic:equal>
			<logic:equal name="parameter" property="type" value="text">
				<html:text property='<%="parameterValue["+parameter.getId()+"]"%>' size="30" maxlength="100" disabled="<%=disabled || parameter.getDisabled()%>"/>
  				&nbsp;<html:errors property='<%="parameterValue["+parameter.getId()+"]"%>'/>
			</logic:equal>	
			<% if (disabled || parameter.getDisabled()) { %>
				<html:hidden property='<%="parameterValue["+parameter.getId()+"]"%>'/>
			<% } %>
			</TD>		
		</TR>
	</logic:iterate>
	<logic:empty name="studentSolverForm" property="hosts">
		<html:hidden property="host"/>
	</logic:empty>
	<logic:notEmpty name="studentSolverForm" property="hosts">
	   	<TR><TD>Host:</TD>
			<TD>
				<html:select property="host" disabled="<%=(solver!=null)%>">
					<html:options name="studentSolverForm" property="hosts"/>
				</html:select>
				&nbsp;<html:errors property="host"/>
			</TD>
		</TR>
	</logic:notEmpty>
	<TR>
		<TD align="right" colspan="2">
<% if (solver==null) { %>
			<html:submit onclick="displayLoading();" property="op" value="Load"/>
<% }
   if (solver==null || !solver.isWorking()) { %>
			<html:submit onclick="displayLoading();" property="op" value="Start"/>
<% }
   if (solver!=null && solver.isRunning()) { %>
			<html:submit onclick="displayLoading();" property="op" value="Stop"/>
<% }
   if (solver!=null && !solver.isWorking()) { %>
			<html:submit onclick="displayLoading();" property="op" value="Reload Input Data"/>
				<logic:equal name="studentSolverForm" property="canDo" value="true">
<%
				if (hasSolution) {
%>
						<html:submit onclick="if (!confirmSave()) return false; displayLoading();" property="op" value="Save"/>
<%
				} else {
%>
						<html:submit onclick="if (!confirmSaveAsNew()) return false; displayLoading();" property="op" value="Save As New"/>
<%
				}
%>
				</logic:equal>
			<html:submit onclick="if (!confirmClear()) return false; displayLoading();" property="op" value="Clear"/>
			<html:submit onclick="if (!confirmUnload()) return false; displayLoading();" property="op" value="Unload"/>
<% } %>
			<html:submit onclick="displayLoading();" property="op" accesskey="R" value="Refresh"/>
		</TD>
	</TR>
</TABLE>
<BR><BR>
<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
<%
	if (solver==null) {
%>
			<TR>
				<TD colspan="2">
					<DIV class="WelcomeRowHead">
					Current Student Schedule
					</DIV>
				</TD>
			</TR>
			<TR>
				<TD>
				<I>No student sectioning data loaded into the solver.</I>
				</TD>
			</TR>
<%
	} else {
		Map<String, String> info = solver.bestSolutionInfo();
		if (info!=null) {
%>
			<TR>
				<TD colspan="2">
					<DIV class="WelcomeRowHead">
					Best Student Schedule Found So Far <tt:wiki>Student Sectioning Solution Properties</tt:wiki>
					</DIV>
				</TD>
			</TR>
<%
			List<String> keys = new ArrayList<String>(info.keySet());
			Collections.sort(keys,new ListSolutionsForm.InfoComparator());
			for (String key: keys) {
				String val = info.get(key);
%>
				<TR><TD><%=key%>:</TD><TD><%=val%></TD></TR>
<%
			}
%>
		<TR><TD colspan=2>&nbsp;</TD></TR>
<%
		}
%>
		<TR>
			<TD colspan="2">
				<DIV class="WelcomeRowHead">
				Current Student Schedule <tt:wiki>Student Sectioning Solution Properties</tt:wiki>
				</DIV>
			</TD>
		</TR>
<%
		info = solver.currentSolutionInfo();
		List<String> keys = new ArrayList<String>(info.keySet());
		Collections.sort(keys,new ListSolutionsForm.InfoComparator());
		for (String key: keys) {
			String val = info.get(key);
%>
			<TR><TD><%=key%>:</TD><TD><%=val%></TD></TR>
<%
		}
		if (!solver.isWorking()) 
		{
%>
			<TR>
				<TD align="right" colspan="2">
<%
			if (solver.bestSolutionInfo()!=null) {
%>
						<html:submit onclick="displayLoading();" property="op" value="Restore From Best"/>
<%
			}
%>
					<html:submit onclick="displayLoading();" property="op" value="Store To Best"/>
				</TD>
			</TR>
<%
		}

		String log = solver.getLog(Progress.MSGLEVEL_WARN, false, "Loading input data ...");
		if (log!=null && log.length()>0) {
%>
			<TR><TD colspan=2>&nbsp;</TD></TR>
			<TR>
				<TD colspan="2">
					<DIV class="WelcomeRowHead">
					Problems <tt:wiki>Solver Warnings</tt:wiki>
					</DIV>
				</TD>
			</TR>
			<TR><TD colspan='2'><%=log%></TD></TR>
<%
		}
	}
%>
	</TABLE>
<%
} catch (Exception e) {
	e.printStackTrace();
}
%>

	<tt:propertyEquals name="tmtbl.solver.remote.allow_jnlp_exec" value="true">
		<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
			<TR>
				<TD><DIV class="WelcomeRowHeadBlank">&nbsp;</DIV></TD>
			</TR>
			<TR><TD align="right">
				<html:button onclick="document.location='solver/solver.jnlp';" property="op" value="Start Local Solver"/>
			</TD></TR>
		</TABLE>
	</tt:propertyEquals>

<logic:equal name="studentSolverForm" property="changeTab" value="true">
	<script language="javascript" type="text/javascript">
	top.frames[4].location='admin/userinfo.jsp?tab=1';
	</script>
</logic:equal>
<logic:equal name="studentSolverForm" property="changeTab" value="false">
	<script language="javascript" type="text/javascript">
	top.frames[4].location='admin/userinfo.jsp';
	</script>
</logic:equal>
</html:form>
