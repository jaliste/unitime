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

<tiles:importAttribute />

<tt:confirm name="confirmDelete">The room type will be deleted. Continue?</tt:confirm>

<html:form action="/roomTypeEdit">
<html:hidden property="uniqueId"/><html:errors property="uniqueId"/>
<input type='hidden' name='op2' value=''>
<html:hidden property="canEdit"/>
<logic:notEqual name="roomTypeEditForm" property="op" value="List">
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD colspan="3">
				<tt:section-header>
					<tt:section-title>
						<logic:equal name="roomTypeEditForm" property="op" value="Save">
						Add
						</logic:equal>
						<logic:notEqual name="roomTypeEditForm" property="op" value="Save">
							Edit
						</logic:notEqual>
						Status Type
					</tt:section-title>
					<logic:equal name="roomTypeEditForm" property="op" value="Save">
						<html:submit property="op" value="Save" accesskey="S" title="Save Status (Alt+S)"/>
					</logic:equal>
					<logic:notEqual name="roomTypeEditForm" property="op" value="Save">
						<html:submit property="op" value="Update" accesskey="U" title="Update Status (Alt+U)"/>
						<html:submit property="op" value="Delete" onclick="return confirmDelete();" accesskey="D" title="Delete Status (Alt+D)"/> 
					</logic:notEqual>
					<html:submit property="op" value="Back" title="Return to Status Types (Alt+B)" accesskey="B"/>
				</tt:section-header>
			</TD>
		</TR>

		<TR>
			<TD>Reference:</TD>
			<TD colspan='2'>
				<html:text property="reference" size="20" maxlength="20"/>
				&nbsp;<html:errors property="reference"/>
			</TD>
		</TR>

		<TR>
			<TD>Label:</TD>
			<TD colspan='2'>
				<html:text property="label" size="60" maxlength="60"/>
				&nbsp;<html:errors property="label"/>
			</TD>
		</TR>

		<logic:equal name="roomTypeEditForm" property="canEdit" value="true">
			<TR>
				<TD>Type:</TD>
				<TD colspan='2'>
				<html:select property="type">
					<html:option value="0">Room</html:option>
					<html:option value="1">Other Location</html:option>
				</html:select>
				&nbsp;<html:errors property="type"/>
			</TR>
		</logic:equal>
		<logic:equal name="roomTypeEditForm" property="canEdit" value="false">
			<html:hidden property="type"/>
		</logic:equal>
		
		<TR>
			<TD colspan='3'>
				<tt:section-title/>
			</TD>
		</TR>
		<TR>
			<TD align="right" colspan="3">
				<logic:equal name="roomTypeEditForm" property="op" value="Save">
					<html:submit property="op" value="Save" accesskey="S" title="Save Room Type (Alt+S)"/>
				</logic:equal>
				<logic:notEqual name="roomTypeEditForm" property="op" value="Save">
					<html:submit property="op" value="Update" accesskey="U" title="Update Room Type (Alt+U)"/>
					<logic:equal name="roomTypeEditForm" property="canEdit" value="true">
						<html:submit property="op" value="Delete" onclick="return confirmDelete();" accesskey="D" title="Delete Room Type (Alt+D)"/>
					</logic:equal> 
				</logic:notEqual>
				<html:submit property="op" value="Back" title="Return to Room Types (Alt+B)" accesskey="B"/>
			</TD>
		</TR>
	</TABLE>
</logic:notEqual>
<logic:equal name="roomTypeEditForm" property="op" value="List">
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
		<tr>
			<td colspan='5'>
				<tt:section-header>
					<tt:section-title>Room Types</tt:section-title>
					<html:submit property="op" value="Add Room Type" accesskey="A" title="Create New Room Type (Alt+A)"/>
				</tt:section-header>
			</td>
		</tr>
		<%= request.getAttribute("RoomType.table") %> 
		<tr>
			<td colspan='5'>
				<tt:section-title/>
			</td>
		</tr>
		<tr>
			<td colspan='5' align="right">
				<html:submit property="op" value="Add Room Type" accesskey="A" title="Create New Room Type (Alt+A)"/>
			</td>
		</tr>
	</TABLE>
</logic:equal>
</html:form>
