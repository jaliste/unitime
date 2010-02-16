<%--
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008-2009, UniTime LLC
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
<%@page import="org.unitime.timetable.solver.exam.ui.ExamConflictStatisticsInfo"%>
<%@ page import="org.unitime.timetable.form.ExamInfoForm"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<script language="JavaScript" type="text/javascript" src="scripts/block.js"></script>
<%
	// Get Form 
	String frmName = "examInfoForm";
	ExamInfoForm frm = (ExamInfoForm) request.getAttribute(frmName);

%>
<tiles:importAttribute />
<html:form action="/examInfo">
	<html:submit onclick="displayLoading();" property="op" value="Apply" style="display:none;"/>
	<html:hidden property="depth"/>
	<html:hidden property="timeout"/>
	<bean:define id="model" name="examInfoForm" property="model"/>
	<bean:define id="exam" name="model" property="exam"/>
	<bean:define id="examId" name="exam" property="examId"/>
	<logic:notEmpty name="examInfoForm" property="message">
		<bean:define id="message" name="examInfoForm" property="message"/>
		<script language="JavaScript" type="text/javascript">
			alert('<%=message%>');
		</script>
	</logic:notEmpty>
	<logic:equal name="examInfoForm" property="op" value="Close">
		<script language="JavaScript" type="text/javascript">
			if (document.parentWindow && document.parentWindow.frames[5]) { 
				document.parentWindow.frames[5].location = document.parentWindow.frames[5].location+'?backId=<%=examId%>&backType=Exam';
			}
			window.close();
		</script>
	</logic:equal>
	<tt:confirm name="confirmAssign"><bean:write name="model" property="assignConfirm"/></tt:confirm>
	<table border='0' width='95%'>
		<tr><td colspan='2'>
			<tt:section-header>
				<tt:section-title>Examination <bean:write name="exam" property="examName"/></tt:section-title>
			</tt:section-header>
		</td></tr>
		<tr><td>Courses / Classes:</td><td><bean:write name="exam" property="sectionName(<br>)" filter="false"/></td></tr>
		<tr><td>Type:</td><td><bean:write name="exam" property="examTypeLabel"/></td></tr>
		<tr><td>Length:</td><td><bean:write name="exam" property="length"/> minutes</td></tr>
		<tr><td>Size:</td><td><bean:write name="exam" property="nrStudents"/></td></tr>
		<tr><td>Seating Type:</td><td><bean:write name="exam" property="seatingTypeLabel"/></td></tr>
		<tr><td>Maximum Number of Rooms:</td><td><bean:write name="exam" property="maxRooms"/></td></tr>
		<logic:notEmpty name="exam" property="instructors">
			<tr><td valign="top">Instructor(s):</td><td><%= frm.getModel().getExam().getInstructorName("<br>") %></td></tr>
		</logic:notEmpty>
		<logic:notEmpty name="model" property="change">
			<logic:notEmpty name="model" property="examOldAssignment">
				<bean:define id="assignment" name="model" property="examOldAssignment"/>
				<tr><td>Assigned Period:</td><td><bean:write name="assignment" property="periodNameWithPref" filter="false"/></td></tr>
				<logic:notEmpty name="assignment" property="rooms">
					<tr><td>Assigned Room:</td><td><bean:write name="assignment" property="roomsNameWithPref(, )" filter="false"/></td></tr>
				</logic:notEmpty>
			</logic:notEmpty>
			<logic:notEmpty name="model" property="selectedAssignment">
				<bean:define id="assignment" name="model" property="selectedAssignment"/>
				<tr><td>Selected Period:</td><td><bean:write name="assignment" property="periodNameWithPref" filter="false"/></td></tr>
				<logic:notEmpty name="assignment" property="rooms">
					<tr><td>Selected Room:</td><td><bean:write name="assignment" property="roomsNameWithPref(, )" filter="false"/></td></tr>
				</logic:notEmpty>
			</logic:notEmpty>
			<bean:define name="model" property="change" id="change"/>
			<tr><td colspan='2'><tt:section-title><br>New Assignment(s)</tt:section-title></td></tr>
			<tr><td colspan='2'><bean:write name="change" property="htmlTable" filter="false"/></td></tr>
			<logic:equal name="model" property="canAssign" value="true">
				<tr><td colspan='2' align="right">
					<html:submit onclick="displayLoading();" property="op" value="Assign" onclick="return confirmAssign();" />
				</td></tr>
			</logic:equal>
			<logic:notEmpty name="model" property="selectedAssignment">
				<logic:greaterThan name="assignment" property="nrDistributionConflicts" value="0">
					<tr><td colspan='2'><tt:section-title><br>Violated Distribution Preferences for <bean:write name="exam" property="examName"/> (<bean:write name="assignment" property="periodAbbreviation" filter="false"/>)</tt:section-title></td></tr>
					<tr><td colspan='2'><bean:write name="assignment" property="distributionInfoConflictTable" filter="false"/></td></tr>
				</logic:greaterThan>
				<logic:equal name="assignment" property="hasConflicts" value="true">
					<tr><td colspan='2'><tt:section-title><br>Student Conflicts for <bean:write name="exam" property="examName"/> (<bean:write name="assignment" property="periodAbbreviation" filter="false"/>)</tt:section-title></td></tr>
					<tr><td colspan='2'><bean:write name="assignment" property="conflictInfoTable" filter="false"/></td></tr>
				</logic:equal>
				<logic:equal name="assignment" property="hasInstructorConflicts" value="true">
					<tr><td colspan='2'><tt:section-title><br>Instructor Conflicts for <bean:write name="exam" property="examName"/> (<bean:write name="assignment" property="periodAbbreviation" filter="false"/>)</tt:section-title></td></tr>
					<tr><td colspan='2'><bean:write name="assignment" property="instructorConflictInfoTable" filter="false"/></td></tr>
				</logic:equal>
			</logic:notEmpty>
		</logic:notEmpty>
		<logic:empty name="model" property="change">
			<logic:notEmpty name="model" property="examAssignment">
				<bean:define id="assignment" name="model" property="examAssignment"/>
				<tr><td>Period:</td><td><bean:write name="assignment" property="periodNameWithPref" filter="false"/></td></tr>
				<logic:notEmpty name="assignment" property="rooms">
					<tr><td>Room:</td><td><bean:write name="assignment" property="roomsNameWithPref(, )" filter="false"/></td></tr>
				</logic:notEmpty>
				<logic:greaterThan name="assignment" property="nrDistributionConflicts" value="0">
					<tr><td colspan='2'><tt:section-title><br>Violated Distribution Preferences</tt:section-title></td></tr>
					<tr><td colspan='2'><bean:write name="assignment" property="distributionConflictTable" filter="false"/></td></tr>
				</logic:greaterThan>
				<logic:equal name="assignment" property="hasConflicts" value="true">
					<tr><td colspan='2'><tt:section-title><br>Student Conflicts</tt:section-title></td></tr>
					<tr><td colspan='2'><bean:write name="assignment" property="conflictTable" filter="false"/></td></tr>
				</logic:equal>
				<logic:equal name="assignment" property="hasInstructorConflicts" value="true">
					<tr><td colspan='2'><tt:section-title><br>Instructor Conflicts</tt:section-title></td></tr>
					<tr><td colspan='2'><bean:write name="assignment" property="instructorConflictTable" filter="false"/></td></tr>
				</logic:equal>
			</logic:notEmpty>
		</logic:empty>
		<logic:notEmpty name="model" property="periods">
			<tr><td colspan='2'><br><table border='0' width='100%' cellspacing='0' cellpadding='3'>
				<bean:write name="model" property="periodsTable" filter="false"/>
			</table></td></tr>
		</logic:notEmpty>
		<logic:notEmpty name="model" property="selectedAssignment">
		<logic:greaterThan name="exam" property="maxRooms" value="0">
			<tr><td colspan='2'><tt:section-title>
				<bean:define id="nrStudents" name="exam" property="nrStudents"/>
				<br>Available Rooms for <bean:write name="exam" property="examName"/> &nbsp;&nbsp;
				( selected size: <span id='roomCapacityCounter'>
					<logic:lessThan name="model" property="roomSize" value="<%=String.valueOf(nrStudents)%>">
						<font color='red'><bean:write name="model" property="roomSize"/></font>
					</logic:lessThan>
					<logic:greaterEqual name="model" property="roomSize" value="<%=String.valueOf(nrStudents)%>">
						<bean:write name="model" property="roomSize"/>
					</logic:greaterEqual>
					</span> of <bean:write name="exam" property="nrStudents"/> ) 
			</tt:section-title></td></tr>
			<tr><td colspan='2'>
				<table border='0' width='100%'>
					<tr><td>
						Room size:
							<html:text property="minRoomSize" size="5" maxlength="5"/> - <html:text property="maxRoomSize" size="5" maxlength="5"/>
					</td><td>
						Filter:
							<html:text property="roomFilter" size="25" maxlength="100"/>
					</td><td>
						Allow conflicts:
							<html:checkbox property="allowRoomConflict"/>
					</td><td>
						Order:
							<html:select property="roomOrder">
								<html:options property="roomOrders"/>
							</html:select>
					</td><td align="right">
						<html:submit onclick="displayLoading();" property="op" value="Apply"/>
					</td></tr>
				</table>
			</td></tr>
			<tr><td colspan='2'>
				<table border='0' width="100%"><tr>
					<td nowrap>Room Types:</td>
					<logic:iterate name="examInfoForm" property="allRoomTypes" id="rf" indexId="rfIdx">
						<td nowrap>
							<html:multibox property="roomTypes">
								<bean:write name="rf" property="uniqueId"/>
							</html:multibox>
							<bean:write name="rf" property="label"/>&nbsp;&nbsp;&nbsp;
						</td>
						<% if (rfIdx%3==2) { %>
							</tr><tr><td></td>
						<% } %>
					</logic:iterate>
				</tr><tr>
					<td nowrap>Room Groups:</td>
					<logic:iterate name="examInfoForm" property="allRoomGroups" id="rf" indexId="rfIdx">
						<td nowrap>
							<html:multibox property="roomGroups">
								<bean:write name="rf" property="uniqueId"/>
							</html:multibox>
							<bean:write name="rf" property="name"/>&nbsp;&nbsp;&nbsp;
						</td>
						<% if (rfIdx%3==2) { %>
							</tr><tr><td></td>
						<% } %>
					</logic:iterate>
				</tr><tr>
					<td nowrap>Room Features:</td>
					<logic:iterate name="examInfoForm" property="allRoomFeatures" id="rf" indexId="rfIdx">
						<td nowrap>
							<html:multibox property="roomFeatures">
								<bean:write name="rf" property="uniqueId"/>
							</html:multibox>
							<bean:write name="rf" property="label"/>&nbsp;&nbsp;&nbsp;
						</td>
						<% if (rfIdx%3==2) { %>
							</tr><tr><td></td>
						<% } %>
					</logic:iterate>
				</tr></table>
			</td></tr>
			<logic:empty name="model" property="roomTable">
				<tr><td colspan='2'><i>No room matching the above criteria was found.</td></tr>
			</logic:empty>
			<logic:notEmpty name="model" property="roomTable">
				<tr><td colspan='2'>
					<bean:write name="model" property="roomTable" filter="false"/>
				</td></tr>
			</logic:notEmpty>
		</logic:greaterThan>
		</logic:notEmpty>
		<logic:equal name="model" property="canComputeSuggestions" value="true">
			<tr><td colspan='2'><tt:section-title><br><html:checkbox property="computeSuggestions" onclick="displayLoading();submit();"/> Suggestions</tt:section-title></td></tr>
			<logic:equal name="examInfoForm" property="computeSuggestions" value="true">
				<tr><td colspan='2'>
					<table border='0' width='100%'>
						<tr><td>
							Filter:
								<html:text property="filter" size="50" maxlength="100"/>
						</td><td>
							Maximal Number of Suggestions:
								<html:text property="limit" size="5" maxlength="5"/>
						</td><td align="right">
						</td><td align="right">
							<html:submit onclick="displayLoading();" property="op" value="Apply"/>
							<html:submit onclick="displayLoading();" property="op" value="Search Deeper"/>
							<logic:equal name="model" property="suggestionsTimeoutReached" value="true">
								<html:submit onclick="displayLoading();" property="op" value="Search Longer"/>
							</logic:equal>
						</td></tr>
					</table>
				</td></tr>
				<logic:notEmpty name="model" property="suggestions">
					<tr><td colspan='2'>
						<bean:write name="model" property="suggestionTable" filter="false"/>
					</td></tr>
				</logic:notEmpty>
			</logic:equal>
		</logic:equal>
		<logic:notEmpty name="model" property="cbs">
			<% ExamConflictStatisticsInfo.printHtmlHeader(out); %>
			<tr><td colspan='2'><tt:section-title><br>Conflict-based Statistics</tt:section-title></td></tr>
			<tr><td colspan='2'>
				<bean:define name="model" property="cbs" id="cbs"/>
				<font size='2'>
				<% ((ExamConflictStatisticsInfo)cbs).printHtml(out, (Long)examId, 1.0, ExamConflictStatisticsInfo.TYPE_CONSTRAINT_BASED, true); %>
				</font>
			</td></tr>
		</logic:notEmpty>
		<tr><td colspan='2'><tt:section-title><br></tt:section-title></td></tr>
	</table>
</html:form>
