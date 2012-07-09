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
<%@ page language="java" autoFlush="true" errorPage="../error.jsp"%>
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.model.Roles" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<%@ taglib uri="/WEB-INF/tld/localization.tld" prefix="loc" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>

<loc:bundle name="CourseMessages">
		
	<logic:notEmpty name="instructorList" scope="request">
		<table width="100%" border="0" cellspacing="0" cellpadding="3"><tr>
			<bean:write name="instructorList" scope="request" filter="false"/>
		</tr></table>
		
		<table width="100%" border="0" cellspacing="0" cellpadding="3">
		<tr><td align="right">
				<TABLE border="0" cellspacing="3" cellpadding="0" align="right"><TR>
					<sec:authorize access="hasPermission(#deptUniqueId, 'Department', 'InstructorsExportPdf')">
					<TD>
					<html:form action="instructorList" styleClass="FormWithNoPadding">			
						<html:submit property="op" onclick="displayLoading();" styleClass="btn" 
							accesskey="<%=MSG.accessExportPdf() %>" 
							title="<%=MSG.titleExportPdf(MSG.accessExportPdf()) %>">
							<loc:message name="actionExportPdf" />
						</html:submit>
					</html:form>
					</TD>
				</sec:authorize>
				<sec:authorize access="hasPermission(#deptUniqueId, 'Department', 'ManageInstructors')">
					<TD>
					<html:form action="instructorListUpdate" styleClass="FormWithNoPadding">
						<html:submit onclick="displayLoading();" styleClass="btn"
							accesskey="<%=MSG.accessManageInstructorList() %>" 
							title="<%=MSG.titleManageInstructorList(MSG.accessManageInstructorList()) %>">
							<loc:message name="actionManageInstructorList" />
						</html:submit>
					</html:form>
					</TD>
				</sec:authorize>
				<sec:authorize access="hasPermission(#deptUniqueId, 'Department', 'InstructorAdd')">
					<TD>
					<html:form action="instructorAdd" styleClass="FormWithNoPadding">
						<html:submit onclick="displayLoading();" styleClass="btn"
							accesskey="<%=MSG.accessAddNewInstructor() %>" 
							title="<%=MSG.titleAddNewInstructor(MSG.accessAddNewInstructor()) %>">
							<loc:message name="actionAddNewInstructor" />
						</html:submit>
					</html:form>
					</TD>
				</sec:authorize>
			</TR></TABLE>
		</td></tr>
		</table>
	</logic:notEmpty>
</loc:bundle>

