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
<%@ page language="java" autoFlush="true"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<script language="JavaScript" type="text/javascript" src="scripts/block.js"></script>
<tt:back-mark back="true" clear="true" title="Examination Reports" uri="examAssignmentReport.do"/>
<tiles:importAttribute />
<html:form action="/examAssignmentReport">
	<script language="JavaScript">blToggleHeader('Filter','dispFilter');blStart('dispFilter');</script>
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD width="10%" nowrap>Show classes/courses:</TD>
			<TD>
				<html:checkbox property="showSections"/>
			</TD>
		</TR>
	</TABLE>
	<script language="JavaScript">blEnd('dispFilter');blStartCollapsed('dispFilter');</script>
	<script language="JavaScript">blEndCollapsed('dispFilter');</script>
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
	<TR>
  		<TD width="10%" nowrap>Examination Problem:</TD>
		<TD>
			<html:select property="examType">
				<html:optionsCollection property="examTypes" label="label" value="value"/>
			</html:select>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Report:</TD>
		<TD>
			<html:select property="report">
				<html:options property="reports"/>
			</html:select>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Filter:</TD>
		<TD>
			<html:text property="filter" size="50" title="Hint: use comma for conjunctions, semicolon for disjunctions, e.g., 'a,b;c means (a and b) or c'."/>
		</TD>
	</TR>
	<TR>
		<TD width="10%" nowrap>Subject Areas:</TD>
		<TD>
			<html:select name="examAssignmentReportForm" property="subjectArea"
				onfocus="setUp();" 
				onkeypress="return selectSearch(event, this);" 
				onkeydown="return checkKey(event, this);" >
				<html:option value="">Select...</html:option>
				<html:option value="-1">All</html:option>
				<html:optionsCollection property="subjectAreas"	label="subjectAreaAbbreviation" value="uniqueId" />
			</html:select>
		</TD>
	</TR>
	<TR>
		<TD colspan='2' align='right'>
			<html:submit onclick="displayLoading();" accesskey="A" property="op" value="Apply"/>
			<logic:notEmpty name="examAssignmentReportForm" property="table">
				<html:submit onclick="displayLoading();" property="op" value="Export PDF"/>
			</logic:notEmpty>
			<logic:notEmpty name="examAssignmentReportForm" property="table">
				<html:submit onclick="displayLoading();" property="op" value="Export CSV"/>
			</logic:notEmpty>
			<html:submit onclick="displayLoading();" accesskey="R" property="op" value="Refresh"/>
		</TD>
	</TR>
	</TABLE>

	<BR><BR>
	<logic:empty name="examAssignmentReportForm" property="table">
		<table width='95%' border='0' cellspacing='0' cellpadding='3'>
			<tr><td><i>
				No exams matching the above criteria found.
			</i></td></tr>
		</table>
	</logic:empty>
	<logic:notEmpty name="examAssignmentReportForm" property="table">
		<table width='95%' border='0' cellspacing='0' cellpadding='3'>
			<bean:define id="colspan" name="examAssignmentReportForm" property="nrColumns"/>
			<bean:write name="examAssignmentReportForm" property="table" filter="false"/>
			<tr><td colspan='<%=colspan%>'><tt:displayPrefLevelLegend/></td></tr>
		</table>
	</logic:notEmpty>
	<logic:notEmpty scope="request" name="hash">
		<SCRIPT type="text/javascript" language="javascript">
			location.hash = '<%=request.getAttribute("hash")%>';
		</SCRIPT>
	</logic:notEmpty>
</html:form>