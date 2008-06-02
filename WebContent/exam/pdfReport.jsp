<%--
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime.org
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
<html:form action="/examPdfReport">
	<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
	<logic:messagesPresent>
		<TR>
			<TD colspan='2'>
				<tt:section-header>
					<tt:section-title><font color='red'>Errors</font></tt:section-title>
					<logic:empty name="examPdfReportForm" property="report">
						<html:submit onclick="displayLoading();" accesskey="G" property="op" value="Generate" title="Generate Report (Alt+G)"/>
					</logic:empty>
					<logic:notEmpty name="examPdfReportForm" property="report">
						<html:submit onclick="displayLoading();" accesskey="B" property="op" value="Back" title="Back (Alt+B)"/>
					</logic:notEmpty>
				</tt:section-header>
			</TD>
		</TR>
		<TR>
			<TD colspan="2" align="left" class="errorCell">
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
		<TR><TD>&nbsp;</TD></TR>
	</logic:messagesPresent>
	<logic:notEmpty name="examPdfReportForm" property="report">
		<TR>
			<TD colspan='2'>
				<tt:section-header>
					<tt:section-title>Log</tt:section-title>
					<logic:messagesNotPresent>
						<html:submit onclick="displayLoading();" accesskey="B" property="op" value="Back" title="Back (Alt+B)"/>
					</logic:messagesNotPresent>
				</tt:section-header>
			</TD>
		</TR>
		<TR>
  			<TD colspan='2'>
  				<blockquote>
  					<bean:write name="examPdfReportForm" property="report" filter="false"/>
  				</blockquote>
  			</TD>
		</TR>
		<TR>
			<TD colspan='2'>
				<tt:section-title>&nbsp;</tt:section-title>
			</TD>
		</TR>
		<TR>
			<TD colspan='2' align='right'>
				<html:submit onclick="displayLoading();" accesskey="B" property="op" value="Back" title="Back (Alt+B)"/>
			</TD>
		</TR>
	</logic:notEmpty>
	<logic:empty name="examPdfReportForm" property="report">
	<TR>
		<TD colspan='2'>
			<tt:section-header>
				<tt:section-title>Input Data</tt:section-title>
				<logic:messagesNotPresent>
					<html:submit onclick="displayLoading();" accesskey="G" property="op" value="Generate" title="Generate Report (Alt+G)"/>
				</logic:messagesNotPresent>
			</tt:section-header>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Examination Problem:</TD>
		<TD>
			<html:select property="examType">
				<html:optionsCollection property="examTypes" label="label" value="value"/>
			</html:select>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top'>Subject Areas:</TD>
		<TD>
			<html:checkbox property="all" onclick="selectionChanged();"/>All Subject Areas (on one report)<br>
			<html:select property="subjects" multiple="true" size="7"
				onfocus="setUp();" onkeypress="return selectSearch(event, this);" onkeydown="return checkKey(event, this);">
				<html:optionsCollection property="subjectAreas"	label="subjectAreaAbbreviation" value="uniqueId" />
			</html:select>
		</TD>
	</TR>
	<TR>
		<TD colspan='2'>
			<tt:section-title><br>Report</tt:section-title>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top'>Report:</TD>
		<TD>
			<logic:iterate name="examPdfReportForm" property="allReports" id="report">
				<html:multibox property="reports" onclick="selectionChanged();">
					<bean:write name="report"/>
				</html:multibox>
				<bean:write name="report"/><br>
			</logic:iterate>
		</TD>
	</TR>
	<TR>
		<TD colspan='2'>
			<tt:section-title><br>Parameters</tt:section-title>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top'>All Reports:</TD>
		<TD><html:checkbox property="itype"/>Display Instructional Type</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top'>Conflicts Reports:</TD>
		<TD>
			<html:checkbox property="direct"/>Display Direct Conflicts<br>
			<html:checkbox property="m2d"/>Display More Than 2 Exams A Day Conflicts<br>
			<html:checkbox property="btb"/>Display Back-To-Back Conflicts
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top' rowspan='2'>Reports with Rooms:</TD>
		<TD><html:checkbox property="dispRooms"/>Display Rooms</TD>
	</TR>
	<TR>
		<TD>No Room: <html:text property="noRoom" size="11" maxlength="11"/></TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top' rowspan='3'>Period Chart:</TD>
		<TD><html:checkbox property="totals"/>Display Totals</TD>
	</TR>
	<TR>
		<TD>Limit: <html:text property="limit" size="4" maxlength="4"/></TD>
	</TR>
	<TR>
		<TD>Room Codes: <html:text property="roomCodes" size="70" maxlength="200"/></TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top'>Verification Report:</TD>
		<TD><html:checkbox property="dispLimit"/>Display Limits &amp; Enrollments</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap valign='top' rowspan='2'>Individual Reports:</TD>
  		<TD><html:checkbox property="classSchedule"/>Include Class Schedule</TD>
  	</TR>
  	<TR>
		<TD>
			Date: <html:text property="since" maxlength="10" size="10" styleId="since_date"/> 
				<img style="cursor: pointer;" src="scripts/jscalendar/calendar_1.gif" 
				border="0" id="show_since_date">
			<i>(Only email instructors/students that have a change in their schedule since this date, email all when empty)</i>
		</TD>
	</TR>
	<TR>
		<TD colspan='2' valign='top'>
			<tt:section-title><br>Output</tt:section-title>
		</TD>
	</TR>
	<TR>
  		<TD width="10%" nowrap>Format:</TD>
		<TD>
			<html:select property="mode">
				<html:options property="modes"/>
			</html:select>
		</TD>
	</TR>
	<logic:equal name="examPdfReportForm" property="canEmail" value="false">
		<html:hidden property="delivery"/>
	</logic:equal>
	<logic:equal name="examPdfReportForm" property="canEmail" value="true">
	<TR>
		<TD rowspan='1' valign='top'>Delivery:</TD>
		<TD>
			<html:checkbox property="email" onclick="document.getElementById('eml').style.display=(this.checked?'block':'none');"/> Email
			<bean:define name="examPdfReportForm" property="email" id="email"/>
			<table border='0' id='eml' style='display:<%=(Boolean)email?"block":"none"%>;'>
				<tr>
					<td rowspan='4' valign='top'>Address:</td>
					<td><html:textarea property="address" rows="3" cols="70"/></td>
				</tr>
				<tr><td>
					<html:checkbox property="emailDeputies" styleId="ed"/> All Involved Department Schedule Managers
				</td></tr>
				<tr><td>
					<html:checkbox property="emailInstructors" styleId="ed"/> Send Individual Instructor Schedule Reports to All Involved Instructors
				</td></tr>
				<tr><td>
					<html:checkbox property="emailStudents" styleId="ed"/> Send Individual Student Schedule Reports to All Involved Students
				</td></tr>
				<tr><td valign='top'>CC:</td><td>
					<html:textarea property="cc" rows="2" cols="70"/>
				</td></tr>
				<tr><td valign='top'>BCC:</td><td>
					<html:textarea property="bcc" rows="2" cols="70"/>
				</td></tr>
				<tr><td valign='top' style='border-top: black 1px dashed;'>Subject:</td><td style='border-top: black 1px dashed;'>
					<html:text property="subject" size="70" style="margin-top:2px;"/>
				</td></tr>
				<tr><td valign='top'>Message:</td><td>
					<html:textarea property="message" rows="10" cols="70"/>
				</td></tr>
			</table>
		</TD>
	</TR>
	</logic:equal>
	<TR>
		<TD colspan='2'>
			<tt:section-title><br>&nbsp;</tt:section-title>
		</TD>
	</TR>
	<TR>
		<TD colspan='2' align='right'>
			<html:submit onclick="displayLoading();" accesskey="G" property="op" value="Generate" title="Generate Report (Alt+G)"/>
		</TD>
	</TR>
	</logic:empty>
	</TABLE>
<script type="text/javascript" language="javascript">
	function selectionChanged() {
		if (document.getElementsByName('all')==null || document.getElementsByName('all').length==0) return;
		var allSubjects = document.getElementsByName('all')[0].checked;
		var objSubjects = document.getElementsByName('subjects')[0];
		var objEmailDeputies = document.getElementsByName('emailDeputies')[0];
		var objEmailInstructors = document.getElementsByName('emailInstructors')[0];
		var objEmailStudents = document.getElementsByName('emailStudents')[0];
		var objReports = document.getElementsByName('reports');
		var objSince = document.getElementsByName('since')[0];
		var studentSchedule = false;
		var instructorSchedule = false;
		for (var i=0;i<objReports.length;i++) {
			if ('Individual Student Schedule'==objReports[i].value) studentSchedule = objReports[i].checked;
			if ('Individual Instructor Schedule'==objReports[i].value) instructorSchedule = objReports[i].checked;
		}
		objSubjects.disabled=allSubjects;
		objEmailDeputies.disabled=allSubjects; 
		objEmailInstructors.disabled=!instructorSchedule;
		objEmailStudents.disabled=!studentSchedule;
		if (allSubjects) {
			objEmailDeputies.checked=false;
		}
		if (!studentSchedule) objEmailStudents.checked=false;
		if (!instructorSchedule) objEmailInstructors.checked=false;
		objSince.disabled=objEmailInstructors.disabled && objEmailStudents.disabled;
	}
</script>
<script type="text/javascript" language="javascript">
 
 var objSinceDate = document.getElementById('since_date');
 if (objSinceDate!=null) {
 Calendar.setup( {
  cache      : true,      // Single object used for all calendars
  electric   : false,     // Changes date only when calendar is closed
  inputField : "since_date",  // ID of the input field
     ifFormat   : "%m/%d/%Y",    // Format of the input field
     showOthers : true,     // Show overlap of dates from other months     
     <% if (request.getParameter("since")!=null && request.getParameter("since").length()>=10) { %>
     date  : <%=request.getParameter("since")%>,
     <% }%>
  button     : "show_since_date" // ID of the button
 } );
 }
 
 selectionChanged();
</script>
</html:form>