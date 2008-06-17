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
<%@ page language="java" autoFlush="true"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<script language="JavaScript" type="text/javascript" src="scripts/block.js"></script>
<tiles:importAttribute />
<html:form action="/lastChanges">
	<script language="JavaScript">blToggleHeader('Filter','dispFilter');blStart('dispFilter');</script>
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
	<TR>
		<TD>Department:</TD>
		<TD>
			<html:select property="departmentId"
				onfocus="setUp();" 
				onkeypress="return selectSearch(event, this);" 
				onkeydown="return checkKey(event, this);">
				<html:option value="-1">All Departments</html:option>
				<html:options collection="departments" labelProperty="label" property="uniqueId" />
			</html:select>
		</TD>
	</TR>
	<TR>
		<TD>Subject Area:</TD>
		<TD>
			<html:select property="subjAreaId"
				onfocus="setUp();" 
				onkeypress="return selectSearch(event, this);" 
				onkeydown="return checkKey(event, this);">
				<html:option value="-1">All Subjects</html:option>
				<html:options collection="subjAreas" labelProperty="subjectAreaAbbreviation" property="uniqueId" />
			</html:select>
		</TD>
	</TR>
	<TR>
		<TD>Manager:</TD>
		<TD>
			<html:select property="managerId"
				onfocus="setUp();" 
				onkeypress="return selectSearch(event, this);" 
				onkeydown="return checkKey(event, this);">
				<html:option value="-1">All Managers</html:option>
				<html:options collection="managers" labelProperty="name" property="uniqueId" />
			</html:select>
		</TD>
	</TR>
	<TR>
		<TD>Number of Changes:</TD>
		<TD>
			<html:text property="n" maxlength="5" size="8" />
		</TD>
	</TR>
	<TR>
		<TD colspan='2' align='right'>
			<html:submit onclick="displayLoading();" property="op" value="Apply"/>
			<html:submit onclick="displayLoading();" property="op" value="Export PDF"/>
			<html:submit onclick="displayLoading();" accesskey="R" property="op" value="Refresh"/>
		</TD>
	</TR>
	</TABLE>
	<script language="JavaScript">blEnd('dispFilter');blStartCollapsed('dispFilter');</script>
		<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
			<TR>
				<TD colspan='2' align='right'>
					<html:submit onclick="displayLoading();" accesskey="R" property="op" value="Refresh"/>
					<html:submit onclick="displayLoading();" property="op" value="Export PDF"/>
				</TD>
			</TR>
		</TABLE>
	<script language="JavaScript">blEndCollapsed('dispFilter');</script>

	<BR><BR>

	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
		<%=request.getAttribute("table")%>
	</TABLE>
</html:form>
