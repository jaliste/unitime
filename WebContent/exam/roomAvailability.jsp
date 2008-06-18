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
<tt:back-mark back="true" clear="true" title="Room Availability" uri="roomAvailability.do"/>
<tiles:importAttribute />
<html:form action="/roomAvailability">
	<html:hidden property="showSections"/>
	<html:hidden property="subjectArea" />
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
	<TR>
		<TD colspan='2'>
			<tt:section-header>
				<tt:section-title>Filter</tt:section-title>
				<html:submit onclick="displayLoading();" accesskey="A" property="op" value="Apply"/>
				<logic:notEmpty name="roomAvailabilityForm" property="table">
					<html:submit onclick="displayLoading();" property="op" value="Export PDF"/>
				</logic:notEmpty>
				<html:submit onclick="displayLoading();" accesskey="R" property="op" value="Refresh"/>
			</tt:section-header>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Examination Problem:</TD>
		<TD>
			<html:select property="examType">
				<html:option value="-1">Select...</html:option>
				<html:optionsCollection property="examTypes" label="label" value="value"/>
			</html:select>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Room Filter:</TD>
		<TD>
			<html:text property="filter" size="80"/>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Display Examinations:</TD>
		<TD>
			<html:checkbox property="includeExams"/>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Compare Examinations:</TD>
		<TD>
			<html:checkbox property="compare"/>
		</TD>
	</TR>
	<logic:notEmpty scope="request" name="timestamp">
		<TR>
  			<TD width="10%" nowrap>Last Update:</TD>
			<TD>
				<bean:write scope="request" name="timestamp"/>
			</TD>
		</TR>
	</logic:notEmpty>
	</TABLE>

	<BR><BR>
	<logic:empty name="roomAvailabilityForm" property="table">
		<table width='95%' border='0' cellspacing='0' cellpadding='3'>
			<tr><td><i>
				<logic:equal name="roomAvailabilityForm" property="examType" value="-1">
					Examination problem not selected.
				</logic:equal>
				<logic:notEqual name="roomAvailabilityForm" property="examType" value="-1">
					Nothing to display.
				</logic:notEqual>
			</i></td></tr>
		</table>
	</logic:empty>
	<logic:notEmpty name="roomAvailabilityForm" property="table">
		<table width='95%' border='0' cellspacing='0' cellpadding='3'>
			<bean:write name="roomAvailabilityForm" property="table" filter="false"/>
		</table>
	</logic:notEmpty>
	
	<logic:notEmpty scope="request" name="hash">
		<SCRIPT type="text/javascript" language="javascript">
			location.hash = '<%=request.getAttribute("hash")%>';
		</SCRIPT>
	</logic:notEmpty>
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
	<TR>
		<TD>
			<tt:section-title/>
		</TD>
	</TR>
	<TR>
		<TD align="right">
			<html:submit onclick="displayLoading();" accesskey="A" property="op" value="Apply"/>
			<logic:notEmpty name="roomAvailabilityForm" property="table">
				<html:submit onclick="displayLoading();" property="op" value="Export PDF"/>
			</logic:notEmpty>
			<html:submit onclick="displayLoading();" accesskey="R" property="op" value="Refresh"/>
		</TD>
	</TR>
	</TABLE>	
</html:form>
