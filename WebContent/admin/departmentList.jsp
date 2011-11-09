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
<%@ page import="java.text.DecimalFormat"%>
<%@ page import="java.util.List"%>
<%@ page import="org.unitime.timetable.util.Constants"%>
<%@ page import="org.unitime.timetable.model.Settings"%>
<%@ page import="org.unitime.commons.web.Web"%>
<%@ page import="org.unitime.timetable.model.ChangeLog"%>
<%@ page import="org.unitime.timetable.form.DepartmentListForm" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld"	prefix="bean"%>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld"	prefix="html"%>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
	<TR>
		<TD align="right">
			<tt:section-header>
				<tt:section-title>
				 Department List - <%= Web.getUser(session).getAttribute(Constants.ACAD_YRTERM_LABEL_ATTR_NAME) %>
				</tt:section-title>

				<TABLE align="right" cellspacing="0" cellpadding="2" class="FormWithNoPadding">
					<TR><TD nowrap>
							<html:form action="departmentEdit" styleClass="FormWithNoPadding">
								<html:submit property="op" onclick="displayLoading();" styleClass="btn" accesskey="D" titleKey="title.addDepartment">
								<bean:message key="button.addDepartment" />
							</html:submit>
						</html:form>
					</TD><TD nowrap>
						<input type='button' onclick="document.location='departmentList.do?op=Export%20PDF';" title='Export PDF (Alt+P)' accesskey="P" class="btn" value="Export PDF">
					</TD></TR>
				</TABLE>

			</tt:section-header>
		</TD>
	</TR>
</TABLE>

<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">

	<%
		DepartmentListForm frm = (DepartmentListForm) request.getAttribute("departmentListForm");
		boolean dispLastChanges = (
				!"no".equals(Settings.getSettingValue(Web.getUser(session),
					Constants.SETTINGS_DISP_LAST_CHANGES)));

		WebTable webTable = new WebTable((dispLastChanges ? 10 : 9), "",
				"departmentList.do?ord=%%",
				(dispLastChanges 
					? new String[] { "Number", "Abbv", "Name", "External<br>Manager", 
									 "Subjects", "Rooms", "Status", "Dist&nbsp;Pref Priority", 
									 "Allow Required", "Last Change" } 
					: new String[] { "Number", "Abbreviation", "Name", "External Manager",
									 "Subjects", "Rooms", "Status", "Dist Pref Priority", 
									 "Allow Required" }),
				new String[] { "left", "left", "left", "left", "right",	"right", "left", "right", "left", "left" },
				new boolean[] { true, true, true, true, true, true, true, true, true, false });
		WebTable.setOrder(session, "DepartmentList.ord", request.getParameter("ord"), 1);
        webTable.enableHR("#9CB0CE");
        webTable.setRowStyle("white-space: nowrap");
	%>

	<logic:iterate name="departmentListForm" property="departments"
		id="bldg">
		<%
		org.unitime.timetable.model.Department d = (org.unitime.timetable.model.Department) bldg;
		if (frm.getShowUnusedDepts() || !d.getSubjectAreas().isEmpty()
			|| !d.getTimetableManagers().isEmpty()
			|| d.isExternalManager().booleanValue()) {
				
			DecimalFormat df5 = new DecimalFormat("####0");

			String lastChangeStr = null;
			Long lastChangeCmp = null;
			if (dispLastChanges) {
					List changes = ChangeLog.findLastNChanges(d
							.getSession().getUniqueId(), null, null, d
							.getUniqueId(), 1);
					ChangeLog lastChange = (changes == null
							|| changes.isEmpty() ? null
							: (ChangeLog) changes.get(0));
					lastChangeStr = (lastChange == null ? "&nbsp;"
							: "<span title='"
							+ lastChange.getLabel()
							+ "'>"
							+ ChangeLog.sDFdate.format(lastChange
							.getTimeStamp())
							+ " by "
							+ lastChange.getManager()
							.getShortName() + "</span>");
					lastChangeCmp = new Long(lastChange == null ? 0
							: lastChange.getTimeStamp().getTime());
			}
			
                    String allowReq = "";
                    int allowReqOrd = 0;
                    if (d.isAllowReqRoom() != null && d.isAllowReqRoom().booleanValue()) {
                    	if (!allowReq.isEmpty()) allowReq += ", ";
                    	allowReq += "room";
                    	allowReqOrd += 1;
                    }
                    if (d.isAllowReqTime() != null && d.isAllowReqTime().booleanValue()) {
                    	if (!allowReq.isEmpty()) allowReq += ", ";
                    	allowReq += "time";
                    	allowReqOrd += 2;
                    }
                    if (d.isAllowReqDistribution() != null && d.isAllowReqDistribution().booleanValue()) {
                    	if (!allowReq.isEmpty()) allowReq += ", ";
                    	allowReq += "distribution";
                    	allowReqOrd += 4;
                    }
                    if (allowReqOrd == 7) allowReq = "all";
                    if (allowReqOrd == 0) allowReq = "&nbsp;";

			webTable.addLine(
				"onClick=\"document.location='departmentEdit.do?op=Edit&id=" + d.getUniqueId() + "';\"",
				new String[] {
						d.getDeptCode(),
						d.getAbbreviation()==null ? "&nbsp;" : d.getAbbreviation(),
						"<A name='" + d.getUniqueId() + "'>" + d.getName() + "</A>",
						(d.isExternalManager().booleanValue() 
							? "<span title='" + d.getExternalMgrLabel()	+ "'>" + d.getExternalMgrAbbv()	+ "</span>"
							: "&nbsp;"),
						df5.format(d.getSubjectAreas().size()),
						df5.format(d.getRoomDepts().size()),
						(d.getStatusType() == null ? "<i>" : "&nbsp;")
							+ d.effectiveStatusType().getLabel()
							+ (d.getStatusType() == null ? "</i>" : ""),
						(d.getDistributionPrefPriority() == null && d.getDistributionPrefPriority().intValue() != 0 
							? "&nbsp;" : d.getDistributionPrefPriority().toString()),
						allowReq, lastChangeStr },
				new Comparable[] {
						d.getDeptCode(),
						d.getAbbreviation()==null ? "&nbsp;" : d.getAbbreviation(),
						d.getName(),
						(d.isExternalManager()
						.booleanValue() ? d
						.getExternalMgrAbbv() : ""),
						new Integer(d.getSubjectAreas()
						.size()),
						new Integer(d.getRoomDepts().size()),
						d.effectiveStatusType().getOrd(),
						d.getDistributionPrefPriority(),
						new Integer(allowReqOrd),
						lastChangeCmp });
		}
		%>

	</logic:iterate>

	<%
		out.println(webTable.printTable(WebTable.getOrder(session, "DepartmentList.ord")));
	%>

</TABLE>

<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
	<TR>
		<TD align="right" colspan="2">
			<tt:section-header/>
		</TD>
	</TR>
	<TR>
		<TD align="left">
			<html:form action="/departmentList">
				Show all departments (including departments with no manager and no subject area):
				<html:hidden property="op" value="Apply"/>
				<html:checkbox property="showUnusedDepts" onchange="submit()"/>
			</html:form>
		</TD>
		<TD align="right">
				<TABLE align="right" cellspacing="0" cellpadding="2" class="FormWithNoPadding">
					<TR><TD nowrap>
							<html:form action="departmentEdit" styleClass="FormWithNoPadding">
								<html:submit property="op" onclick="displayLoading();" styleClass="btn" accesskey="D" titleKey="title.addDepartment">
								<bean:message key="button.addDepartment" />
							</html:submit>
						</html:form>
					</TD><TD nowrap>
						<html:submit onclick="displayLoading();" property="op" value="Export PDF" title='Export PDF (Alt+P)' accesskey="P" styleClass="btn"/>
					</TD></TR>
				</TABLE>
		</TD>
	</TR>
</TABLE>				

<SCRIPT type="text/javascript" language="javascript">
	function jumpToAnchor() {
    <% if (request.getAttribute(Constants.JUMP_TO_ATTR_NAME) != null) { %>
  		location.hash = "<%=request.getAttribute(Constants.JUMP_TO_ATTR_NAME)%>";
	<% } %>
	    self.focus();
  	}
</SCRIPT>
