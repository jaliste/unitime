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
<%@ page language="java" autoFlush="true" errorPage="../error.jsp"%>
<%@ page import="org.unitime.timetable.form.RoomDeptListForm" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%		
	// Get Form 
	String frmName = "roomDeptListForm";	
	RoomDeptListForm frm = (RoomDeptListForm) request.getAttribute(frmName);
%>

<!-- Buttons -->
<TABLE>
	<%--
	<TR>
		<TD align="right">
			<html:form action="roomList">
				<html:submit property="doit" onclick="displayLoading();" styleClass="btn" accesskey="R" title="Return to Room List">
					<bean:message key="button.returnToRoomList" />
				</html:submit>
			</html:form>
		</TD>
	</TR>
	--%>
	<TR>
		<TD colspan="2" valign="top" align="center">
			<html:errors/>
		</TD>
	</TR>
</TABLE>

<!-- room departments list -->
<TABLE width="90%" border="0" cellspacing="0" cellpadding="5">
	<% if (request.getAttribute("roomDepts") != null) {%>
		<%=request.getAttribute("roomDepts")%>
	<%}%>
</TABLE>

<!-- Buttons -->
<%--
<TABLE>
	<TR>
		<TD align="right">
			<html:form action="roomList">
				<html:submit property="doit" onclick="displayLoading();" styleClass="btn" accesskey="R" title="Return to Room List">
					<bean:message key="button.returnToRoomList" />
				</html:submit>
			</html:form>
		</TD>
	</TR>
</TABLE>
--%>