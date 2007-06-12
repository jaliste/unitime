<%-- 
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org
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
<%@ page import="org.unitime.timetable.form.EditRoomGroupForm" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<%
	// Get Form 
	String frmName = "editRoomGroupForm";	
	EditRoomGroupForm frm = (EditRoomGroupForm) request.getAttribute(frmName);
%>	

<tiles:importAttribute />

<html:form action="/editRoomGroup">
	<html:hidden property="id"/>
	<TABLE width="93%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD valign="middle" colspan='2'>
				<tt:section-header>
					<tt:section-title><%=frm.getName()%></tt:section-title>
					<html:submit property="doit" onclick="displayLoading();" styleClass="btn" accesskey="U" titleKey="title.updateRoomGroups">
						<bean:message key="button.update" />
					</html:submit>
					&nbsp;
					<html:submit property="doit" onclick="displayLoading();" styleClass="btn" accesskey="B" titleKey="title.returnToRoomList">
						<bean:message key="button.returnToRoomDetail" />
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
	
		<logic:iterate name="<%=frmName%>" property="globalRoomGroupIds" id="globalRoomGroupId" indexId="ctr">
			<TR>
				<TD nowrap>
					<logic:equal name="ctr" value="0">
						Global Groups:
					</logic:equal>
					<logic:notEqual name="ctr" value="0">
						&nbsp;
					</logic:notEqual>
				</TD>
				<TD width='100%'>
				<% 
					boolean disabled1 = true;
					if (frm.getGlobalRoomGroupsEditable(ctr.intValue()).equalsIgnoreCase("true")) {
						disabled1 = false;
					}			
				%> 
					<% if (disabled1) { %>
						<html:hidden property="<%= "globalRoomGroupsAssigned[" + ctr + "]" %>"/>
					<% } %>
					<html:checkbox property="<%= "globalRoomGroupsAssigned[" + ctr + "]" %>" 
									disabled="<%=disabled1%>" >
					</html:checkbox>
					<html:hidden property="<%= "globalRoomGroupIds[" + ctr + "]" %>"/>
					<html:hidden property="<%= "globalRoomGroupNames[" + ctr + "]" %>"/>
					&nbsp;&nbsp;
					<bean:write name="<%=frmName%>" property="<%= "globalRoomGroupNames[" + ctr + "]" %>"/> &nbsp;	
				</TD>
			</TR>
		</logic:iterate>
			
		<logic:iterate name="<%=frmName%>" property="managerRoomGroupIds" id="managerRoomGroupId" indexId="ctr">
			<TR>
				<TD nowrap>
					<logic:equal name="ctr" value="0">
						Manager Groups:
					</logic:equal>
					<logic:notEqual name="ctr" value="0">
						&nbsp;
					</logic:notEqual>
				</TD>
				<TD align='left' width='100%'>	
				<% 
					boolean disabled2 = true;
					if (frm.getManagerRoomGroupsEditable(ctr.intValue()).equalsIgnoreCase("true")) {
						disabled2 = false;
					}			
				%> 
					<% if (disabled2) { %>
						<html:hidden property="<%= "managerRoomGroupsAssigned[" + ctr + "]" %>"/>
					<% } %>
					<html:checkbox property="<%= "managerRoomGroupsAssigned[" + ctr + "]" %>" 
									disabled="<%=disabled2%>" >
					</html:checkbox>
					<html:hidden property="<%= "managerRoomGroupIds[" + ctr + "]" %>"/>
					<html:hidden property="<%= "managerRoomGroupNames[" + ctr + "]" %>"/>
					&nbsp;&nbsp;
					<bean:write name="<%=frmName%>" property="<%= "managerRoomGroupNames[" + ctr + "]" %>"/> &nbsp;	
				</TD>
			</TR>
		</logic:iterate>

		<TR>
			<TD colspan='2'>
				<tt:section-title/>
			</TD>
		</TR>
		
		<TR>
			<TD colspan='2' align='right'>
				<html:submit property="doit" onclick="displayLoading();" styleClass="btn" accesskey="U" titleKey="title.updateRoomGroups">
					<bean:message key="button.update" />
				</html:submit>
				&nbsp;
				<html:submit property="doit" onclick="displayLoading();" styleClass="btn" accesskey="B" titleKey="title.returnToRoomList">
					<bean:message key="button.returnToRoomDetail" />
				</html:submit>
			</TD>
		</TR>
	</TABLE>
	
</html:form>