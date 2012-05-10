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
<%@ page import="org.unitime.commons.web.*"%>
<%@ page import="java.util.*"%>
<%@ page import="org.unitime.timetable.util.Constants"%>
<%@ page import="org.unitime.timetable.model.Settings"%>
<%@ page import="org.unitime.commons.web.Web"%>
<%@ page import="org.unitime.timetable.model.Department"%>
<%@ page import="org.unitime.timetable.model.ChangeLog"%>
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="org.unitime.timetable.model.TimetableManager"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld"	prefix="bean"%>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld"	prefix="html"%>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld"	prefix="logic"%>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt"%>

<html:form action="subjectAreaEdit.do">

	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">

		<TR>
			<TD>

				<tt:section-header>
					<tt:section-title>
					Subject Area List - <%= Web.getUser(session).getAttribute(Constants.ACAD_YRTERM_LABEL_ATTR_NAME) %>
					</tt:section-title>

					<html:submit property="op" styleClass="btn" accesskey="S" titleKey="title.addSubjectArea">
						<bean:message key="button.addSubjectArea" />
					</html:submit>

					<input type='button' onclick="document.location='subjectList.do?op=Export%20PDF';" title='Export PDF (Alt+P)' accesskey="P" class="btn" value="Export PDF">
				</tt:section-header>
			</TD>
		</TR>

	</TABLE>

	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
<%
	boolean dispLastChanges = (!"no".equals(Settings.getSettingValue(Web.getUser(session), Constants.SETTINGS_DISP_LAST_CHANGES)));
	
    WebTable webTable = new WebTable( 
	    (dispLastChanges?7:6),
	    "",
	    "subjectList.do?ord=%%",
	    (dispLastChanges?
		    new String[] {"Abbv", "Title", "Department", "Managers", "Sched Book Only", "Pseudo","Last Change"}:
		    new String[] {"Abbv", "Title", "Department", "Managers", "Sched Book Only", "Pseudo"}),
	    new String[] {"left", "left","left","left","left","left","right"},
	    new boolean[] {true, true, true, true, true, true, false} );
    webTable.enableHR("#9CB0CE");
    webTable.setRowStyle("white-space: nowrap");
    WebTable.setOrder(session,"SubjectList.ord",request.getParameter("ord"),1);
%>

		<logic:iterate name="subjectListForm" property="subjects" id="subj">
<%
	org.unitime.timetable.model.SubjectArea s = (org.unitime.timetable.model.SubjectArea) subj;
	DecimalFormat df5 = new DecimalFormat("####0");
	Department d = s.getDepartment();
	String sdName = "";
	for (Iterator it = s.getManagers().iterator(); it.hasNext();) {
		TimetableManager mgr = (TimetableManager) it.next();
		if (sdName.length() > 0)
			sdName = sdName + "<BR>";
		sdName = sdName + mgr.getFirstName() + " " + mgr.getLastName();
	}

	String lastChangeStr = null;
	Long lastChangeCmp = null;
	if (dispLastChanges) {
		List changes = ChangeLog.findLastNChanges(
			d.getSession().getUniqueId(), null, null, d.getUniqueId(), 1);
		ChangeLog lastChange = 
			(changes == null || changes.isEmpty() ? null : (ChangeLog) changes.get(0));
		lastChangeStr = 
		(lastChange == null 
			? "&nbsp;"
			: "<span title='"
				+ lastChange.getLabel()
				+ "'>"
				+ ChangeLog.sDFdate.format(lastChange
						.getTimeStamp()) + " by "
				+ lastChange.getManager().getShortName()
				+ "</span>");
		lastChangeCmp = new Long(
			lastChange == null ? 0 : lastChange.getTimeStamp().getTime());
	}

	webTable.addLine(
		"onClick=\"document.location.href='subjectAreaEdit.do?op=edit&id=" + s.getUniqueId() + "'\"",
		new String[] { 
			"<A name='" + s.getUniqueId() + "'>" + s.getSubjectAreaAbbreviation() + "</A>",
			s.getLongTitle(),
			(d == null) ? "&nbsp;" : "<span title='"+d.getHtmlTitle()+"'>"+
                            d.getDeptCode()+(d.getAbbreviation()==null?"":": "+d.getAbbreviation().trim())+
                            "</span>",
			(sdName == null || sdName.trim().length()==0) ? "&nbsp;" : sdName,
			s.isScheduleBookOnly().booleanValue() ? "<IMG src='images/tick.gif' border='0' title='Schedule Book Only' alt='Schedule Book Only'>" : "&nbsp;",
			s.isPseudoSubjectArea().booleanValue() ? "<IMG src='images/tick.gif' border='0' title='Pseudo' alt='Pseudo'>" : "&nbsp;", 
			lastChangeStr },
		new Comparable[] { 
			s.getSubjectAreaAbbreviation(),
			s.getLongTitle(),
			(d == null) ? "" : d.getDeptCode(),
			sdName,
			s.isScheduleBookOnly().toString(),
			s.isPseudoSubjectArea().toString(),
			lastChangeCmp });
%>

		</logic:iterate>

<%
	out.println(webTable.printTable(WebTable.getOrder(session, "SubjectList.ord")));
%>

		<%-- print out the refresh link --%>

	</TABLE>
	
	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">

		<TR>
			<TD class="WelcomeRowHead">
				&nbsp;
			</TD>
		</TR>
		<TR>
			<TD align="right">
				<html:submit property="op" styleClass="btn" accesskey="S" titleKey="title.addSubjectArea">
					<bean:message key="button.addSubjectArea" />
				</html:submit>

				<input type='button' onclick="document.location='subjectList.do?op=Export%20PDF';" title='Export PDF (Alt+P)' accesskey="P" class="btn" value="Export PDF">
			</TD>
		</TR>

	</TABLE>

	
</html:form>

<SCRIPT type="text/javascript" language="javascript" src="scripts/smooth_scroll.js"></SCRIPT>
<SCRIPT type="text/javascript" language="javascript">
	
	function jumpToAnchor() {
    <% if (request.getAttribute(Constants.JUMP_TO_ATTR_NAME) != null) { %>
  		//location.hash = "<%=request.getAttribute(Constants.JUMP_TO_ATTR_NAME)%>";  		
  		ss.STEPS=100;
  		ss.INTERVAL=50;
  		ss.smoothScroll("<%=request.getAttribute(Constants.JUMP_TO_ATTR_NAME)%>");
	<% } %>
	    self.focus();
  	}
</SCRIPT>	
