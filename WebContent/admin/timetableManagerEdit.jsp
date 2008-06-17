<%-- 
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime LLC
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
 --%>
<%@ page language="java" autoFlush="true" errorPage="../error.jsp" %>
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.model.Roles" %>
<%@ page import="org.unitime.timetable.model.Department" %>
<%@ page import="org.unitime.timetable.util.Constants" %>
<%@ page import="org.unitime.timetable.webutil.JavascriptFunctions" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<SCRIPT language="javascript">
	<!--
		<%= JavascriptFunctions.getJsConfirm(Web.getUser(session)) %>
		
		function confirmDelete() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			if(confirm('The manager and all associated settings will be deleted. Continue?')) {
				doDel('manager', '');
				return true;
			}
			return false;
		}

		function doDel(type, id) {
			var delType = document.mgrForm.deleteType;
			delType.value = type;
	
			var delId = document.mgrForm.deleteId;
			delId.value = id;
		}
		
	// -->
</SCRIPT>
<tiles:importAttribute />

	<html:form method="post" action="timetableManagerEdit.do">
	<html:hidden name="mgrForm" property="uniqueId" />
	<html:hidden name="mgrForm" property="op1" />
	<html:hidden name="mgrForm" property="isExternalManager" />
	<INPUT type="hidden" name="deleteType" id="deleteType" value="">
	<INPUT type="hidden" name="deleteId" id="deleteId" value="">
	
	<TABLE width="93%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD valign="middle" colspan='2'>
				<tt:section-header>
					<tt:section-title>
						<bean:write name="mgrForm" property="firstName"/>
						<bean:write name="mgrForm" property="middleName"/>
						<bean:write name="mgrForm" property="lastName"/>
					</tt:section-title>
					
					<logic:equal name="mgrForm" property="op1" value="1">
						<html:submit property="op" 
							styleClass="btn" accesskey="S" titleKey="title.insertTimetableManager">
							<bean:message key="button.insertTimetableManager" />
						</html:submit>
					</logic:equal>
					
					<logic:equal name="mgrForm" property="op1" value="2">
						<html:submit property="op" 
							styleClass="btn" accesskey="U" titleKey="title.updateTimetableManager">
							<bean:message key="button.updateTimetableManager" />
						</html:submit>
						<html:submit property="op" 
							styleClass="btn" accesskey="D" titleKey="title.deleteTimetableManager"
							onclick="return (confirmDelete());" >
							<bean:message key="button.deleteTimetableManager" />
						</html:submit>
					</logic:equal>
					
					<html:submit property="op" 
						styleClass="btn" accesskey="B" titleKey="title.backToManagerList">
						<bean:message key="button.backToManagerList" />
					</html:submit>
				</tt:section-header>
			</TD>
		</TR>


		<logic:messagesPresent>
		<TR>
			<TD colspan="2" align="left" class="errorCell">
					<B><U>ERRORS</U></B><BR>
				<BLOCKQUOTE>
				<UL>
				    <html:messages id="error">
				      <LI>
						${error}
				      </LI>
				    </html:messages>
			    </UL>
			    </BLOCKQUOTE>
			</TD>
		</TR>
		</logic:messagesPresent>

		<TR>
			<TD>Academic Session: </TD>
			<TD><%= Web.getUser(session).getAttribute(Constants.ACAD_YRTERM_LABEL_ATTR_NAME) %></TD>
		</TR>
		
		<logic:equal name="mgrForm" property="lookupEnabled" value="false">
			<TR>
				<TD>First Name:</TD>
				<TD>
					<html:text name="mgrForm" property="firstName" size="20" maxlength="20"></html:text>
				</TD>
			</TR>
			<TR>
				<TD>Middle Name:</TD>
				<TD>
					<html:text name="mgrForm" property="middleName" size="20" maxlength="20"></html:text>
				</TD>
			</TR>
			<TR>
				<TD>Last Name:</TD>
				<TD>
					<html:text name="mgrForm" property="lastName" size="30" maxlength="30"></html:text>
				</TD>
			</TR>
		</logic:equal>

		<TR>
			<TD>External ID: </TD>
			<TD>
				<html:text name="mgrForm" property="externalId" size="12" maxlength="12"/>
				&nbsp; <bean:write name="mgrForm" property="lookupResult"/>
				&nbsp;
				<html:hidden name="mgrForm" property="lookupEnabled"/>
				<logic:equal name="mgrForm" property="lookupEnabled" value="true">
					<html:submit property="op" 
						styleClass="btn" accesskey="L" titleKey="title.lookupManager">
						<bean:message key="button.lookupManager" />
					</html:submit>
				</logic:equal>
			</TD>
		</TR>

		<TR>
			<TD>Email Address: </TD>
			<TD>
				<html:text name="mgrForm" property="email" size="30" maxlength="100"/>
			</TD>
		</TR>

		<%--
		<TR>
			<TD>External Manager: </TD>
			<TD>
				<logic:equal name="mgrForm" property="isExternalManager" value="true">
					<img src="images/tick.gif" border="0" title="External Manager">
				</logic:equal>
				<logic:notEqual name="mgrForm" property="isExternalManager" value="true">
					<img src="images/delete.gif" border="0" title="Not External Manager">
				</logic:notEqual>
			</TD>
		</TR>
		--%>

<!-- Departments -->
		<TR>
			<TD colspan="2">
				<tt:section-title>&nbsp;<br>Departments</tt:section-title>
			</TD>
		</TR>

		<TR>
			<TD>&nbsp;</TD>
			<TD>
				<html:select property="dept"									
					onfocus="setUp();" 
					onkeypress="return selectSearch(event, this);" 
					onkeydown="return checkKey(event, this);" >														
					<html:option value="<%=Constants.BLANK_OPTION_VALUE%>"><%=Constants.BLANK_OPTION_LABEL%></html:option>
					<html:options collection="<%=Department.DEPT_ATTR_NAME%>" property="uniqueId" labelProperty="label" />
				</html:select>
				&nbsp;
				<html:submit property="op" 
					styleClass="btn" accesskey="D" titleKey="title.addDepartment" >
					<bean:message key="button.addDepartment" />
				</html:submit>
			</TD>
		</TR>

		<TR>
			<TD>&nbsp;</TD>
			<TD>
				<TABLE width="100%">
				<logic:iterate name="mgrForm" property="depts" id="dept" indexId="ctr">
					<TR>
						<TD>
							<html:hidden property="<%= "depts[" + ctr + "]" %>" />
							<html:hidden property="<%= "deptLabels[" + ctr + "]" %>" />
							<bean:write name="mgrForm" property="<%= "deptLabels[" + ctr + "]" %>" />
						</TD>
						<TD align="right">							
							&nbsp; 
							<html:submit property="op" 
								styleClass="btn"
								onclick="<%= "javascript: doDel('dept', '" + ctr + "');"%>">
								<bean:message key="button.delete" />
							</html:submit> 			
						</TD>
					</TR>
				</logic:iterate>
				</TABLE>
			</TD>
		</TR>

<!-- Solver Groups -->
		<TR>
			<TD colspan="2">
				<tt:section-title>&nbsp;<br>Solver Groups</tt:section-title>
			</TD>
		</TR>

		<TR>
			<TD>&nbsp;</TD>
			<TD>
				<html:select property="solverGr"									
					onfocus="setUp();" 
					onkeypress="return selectSearch(event, this);" 
					onkeydown="return checkKey(event, this);" >														
					<html:option value="<%=Constants.BLANK_OPTION_VALUE%>"><%=Constants.BLANK_OPTION_LABEL%></html:option>
					<html:options collection="solverGroupList" property="uniqueId" labelProperty="name" />
				</html:select>
				&nbsp;
				<html:submit property="op" 
					styleClass="btn" accesskey="S" titleKey="title.addSolverGroup" >
					<bean:message key="button.addSolverGroup" />
				</html:submit>
			</TD>
		</TR>

		<TR>
			<TD>&nbsp;</TD>
			<TD>
				<TABLE width="100%">
				<logic:iterate name="mgrForm" property="solverGrs" id="solverGr" indexId="ctr">
					<TR>
						<TD>
							<html:hidden property="<%= "solverGrs[" + ctr + "]" %>" />
							<html:hidden property="<%= "solverGrLabels[" + ctr + "]" %>" />
							<bean:write name="mgrForm" property="<%= "solverGrLabels[" + ctr + "]" %>" />
						</TD>
						<TD align="right">							
							&nbsp; 
							<html:submit property="op" 
								styleClass="btn"
								onclick="<%= "javascript: doDel('solverGr', '" + ctr + "');"%>">
								<bean:message key="button.delete" />
							</html:submit> 			
						</TD>
					</TR>
				</logic:iterate>
				</TABLE>
			</TD>
		</TR>

<!-- Roles -->
		<TR>
			<TD colspan="2">
				<tt:section-title>&nbsp;<br>Roles</tt:section-title>
			</TD>
		</TR>

		<TR>
			<TD>&nbsp;</TD>
			<TD>
				<html:select property="role">					
					<html:option value="<%=Constants.BLANK_OPTION_VALUE%>"><%=Constants.BLANK_OPTION_LABEL%></html:option>
					<html:options collection="<%=Roles.ROLES_ATTR_NAME%>" property="roleId" labelProperty="abbv" />
				</html:select>
				&nbsp;
				<html:submit property="op" 
					styleClass="btn" accesskey="R" titleKey="title.addRole" >
					<bean:message key="button.addRole" />
				</html:submit>
			</TD>
		</TR>

		<TR>
			<TD>&nbsp;</TD>
			<TD>
				<TABLE width="100%">
					<TR>
						<TD align="left" width="100">
							<I>Primary</I>
						</TD>
						<TD>
							&nbsp;
						</TD>
						<TD>
							&nbsp;
						</TD>
					</TR>
				
							
				<logic:iterate name="mgrForm" property="roles" id="role" indexId="ctr">
					<bean:define id="roleRef" name="mgrForm" property="<%= "roleRefs[" + ctr + "]" %>" />
					<TR>
						<TD align="left" width="100">
							<html:radio name="mgrForm" property="primaryRole" value="<%=role.toString()%>"
								styleId="<%= "primaryRole" + ctr %>"
								onclick="<%= "if(document.getElementById('primaryRole" + ctr + "').checked) { document.getElementById('primaryRole" + ctr + "').value=document.getElementById('role" + ctr + "').value; }; "%>" />
						</TD>
						<TD align="left">
							<%--
								<IMG src="images/<%= Roles.getRoleIcon(roleRef.toString()) %>" border="0" align="middle">&nbsp;
							--%>
							<html:hidden styleId="<%= "role" + ctr %>" property="<%= "roles[" + ctr + "]" %>" />
							<html:hidden property="<%= "roleRefs[" + ctr + "]" %>" />
							<bean:write name="mgrForm" property="<%= "roleRefs[" + ctr + "]" %>" />
						</TD>
						<TD align="right">
							&nbsp; 
							<html:submit property="op" 
								styleClass="btn"
								onclick="<%= "javascript: doDel('role', '" + ctr + "');"%>">
								<bean:message key="button.delete" />
							</html:submit> 			
						</TD>
					</TR>
				</logic:iterate>
				</TABLE>
			</TD>
		</TR>

		<TR>
			<TD colspan="2">
				<tt:section-title/>
			</TD>
		</TR>

		<TR>
			<TD colspan="2" align="right">
				<logic:equal name="mgrForm" property="op1" value="1">
					<html:submit property="op" 
						styleClass="btn" accesskey="S" titleKey="title.insertTimetableManager">
						<bean:message key="button.insertTimetableManager" />
					</html:submit>
				</logic:equal>
				
				<logic:equal name="mgrForm" property="op1" value="2">
					<html:submit property="op" 
						styleClass="btn" accesskey="U" titleKey="title.updateTimetableManager">
						<bean:message key="button.updateTimetableManager" />
					</html:submit>
					<html:submit property="op" 
						styleClass="btn" accesskey="D" titleKey="title.deleteTimetableManager"
						onclick="return (confirmDelete());" >
						<bean:message key="button.deleteTimetableManager" />
					</html:submit>
				</logic:equal>
				
				<html:submit property="op" 
					styleClass="btn" accesskey="B" titleKey="title.backToManagerList">
					<bean:message key="button.backToManagerList" />
				</html:submit>
			</TD>
		</TR>
	</TABLE>
	</html:form>
