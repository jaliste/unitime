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
 * <bean:write name="eventDetailForm" property="additionalInfo"/> 
--%>

<%@ page language="java" autoFlush="true" errorPage="../error.jsp" %>
<%@page import="org.unitime.timetable.form.EventGridForm"%>
<%@page import="org.unitime.timetable.webutil.timegrid.EventGridTable"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<script language="JavaScript" type="text/javascript" src="scripts/block.js"></script>

<tiles:importAttribute />
<tt:back-mark back="true" clear="true" title="Event Room Availability" uri="eventGrid.do"/>
<html:form action="/eventGrid">
	<input type="hidden" name="op2" value="">
	<script language="JavaScript">blToggleHeader('Filter','dispFilter');blStart('dispFilter');</script>
		<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD nowrap>Academic Session: </TD>
			<TD>
				<html:select name="eventGridForm" property="sessionId"  
					onfocus="setUp();" 
    				onkeypress="return selectSearch(event, this);" 
					onkeydown="return checkKey(event, this);"
					onchange="op2.value='SessionChanged'; submit();" > 
 					<html:optionsCollection property="academicSessions"	label="label" value="value" />
				</html:select>
			</TD>
		</TR>

		<TR>
			<TD>
				Dates:
			</TD>
			<TD>
				<bean:write name="eventGridForm" property="datesTable" filter="false"/>
				<tt:displayPrefLevelLegend prefs="false" dpBackgrounds="true" separator=""/>
			</TD>
		</TR>
		<TR>
			<TD nowrap>Time: </TD>
			<TD> Start:&nbsp;
				<html:select name="eventGridForm" property="startTime"
					onfocus="setUp();" 
    				onkeypress="return selectSearchTime(event, this);" 
					onkeydown="return checkKey(event, this);">
					<html:optionsCollection name="eventGridForm" property="times"/>
				</html:select>
				
				&nbsp;&nbsp;
				Stop: 
				<html:select name="eventGridForm" property="stopTime"
					onfocus="setUp();" 
    				onkeypress="return selectSearchTime(event, this);" 
					onkeydown="return checkKey(event, this);">
					<html:optionsCollection name="eventGridForm" property="stopTimes"/>
				</html:select> 
		</TR>

		<TR>
			<TD valign='top' rowspan='2'> 
				Location:
			</TD>
			<TD>
				<table border='0'>
					<tr>
						<logic:iterate name="eventGridForm" property="allRoomTypes" id="rf" indexId="rfIdx">
							<td nowrap>
								<logic:equal name="rf" property="room" value="true">
									<html:multibox property="roomTypes">
										<bean:write name="rf" property="uniqueId"/>
									</html:multibox>
									<bean:write name="rf" property="label"/>&nbsp;&nbsp;&nbsp;
								</logic:equal>
								<logic:equal name="rf" property="room" value="false">
								    <bean:define id="rfId" name="rf" property="uniqueId"/>
								    <% String onChange = "document.getElementById('nul" + rfId + "').style.display = (this.checked ? null : 'none');"; %>
									<html:multibox property="roomTypes" onchange="<%=onChange%>" styleId='<%="chnul" + rfId%>'>
										<bean:write name="rf" property="uniqueId"/>
									</html:multibox>
									<bean:write name="rf" property="label"/><span id="<%="nul"+rfId%>">:&nbsp;
									<html:select name="eventGridForm" property='<%="nonUniversityLocation[" + rfId + "]"%>'
										onfocus="setUp();" 
    									onkeypress="return selectSearch(event, this);" 
										onkeydown="return checkKey(event, this);">
										<html:option value="-1">Select...</html:option>
										<html:optionsCollection name="eventGridForm" property='<%="nonUniversityLocations[" + rfId + "]"%>' label="label" value="uniqueId"/>
									</html:select>
									</span>
									<script>document.getElementById('nul<%=rfId%>').style.display = (document.getElementById('chnul<%=rfId%>').checked ? null : 'none');</script>
								</logic:equal>
							</td>
							<% if (rfIdx%4==3) { %>
								</tr><tr>
							<% } %>
						</logic:iterate>
					</tr>
				</table>
			</TD>
		</TR>

		<TR>
			<TD>Building:
				<html:select name="eventGridForm" property="buildingId"
					onfocus="setUp();" 
    				onkeypress="return selectSearch(event, this);" 
					onkeydown="return checkKey(event, this);">
					<html:option value="-1">Select...</html:option>
					<html:optionsCollection name="eventGridForm" property="buildings" label="abbrName" value="uniqueId"/>
				</html:select> 			
				&nbsp; Room Number:&nbsp; <html:text property="roomNumber" maxlength="20" size="10"/>
				&nbsp;&nbsp;&nbsp; <html:checkbox property="lookAtNearLocations"/> Include close by locations
			</TD>
		</TR>
		<TR>
			<TD> 
				Room Capacity:
			</TD>
			<TD>
				Min: <html:text property="minCapacity" maxlength="5" size="5"/> &nbsp; Max: <html:text property="maxCapacity" maxlength="5" size="5"/>
			</TD>
		</TR>

		<TR>
			<TD valign='top'> 
				Room Groups:
			</TD>
			<TD>
				<table border='0'>
					<tr>
						<logic:iterate name="eventGridForm" property="allRoomGroups" id="rf" indexId="rfIdx">
							<td nowrap>
								<html:multibox property="roomGroups">
								<bean:write name="rf" property="uniqueId"/>
								</html:multibox>
								<bean:write name="rf" property="name"/>&nbsp;&nbsp;&nbsp;
							</td>
							<% if (rfIdx%4==3) { %>
								</tr><tr>
							<% } %>
						</logic:iterate>
					</tr>
				</table>
			</TD>
		</TR>

		<TR>
			<TD valign='top'> 
				Room Features:
			</TD>
			<TD>
				<table border='0'>
					<tr>
						<logic:iterate name="eventGridForm" property="allRoomFeatures" id="rf" indexId="rfIdx">
							<td nowrap>
								<html:multibox property="roomFeatures">
								<bean:write name="rf" property="uniqueId"/>
								</html:multibox>
								<bean:write name="rf" property="label"/>&nbsp;&nbsp;&nbsp;
							</td>
							<% if (rfIdx%4==3) { %>
								</tr><tr>
							<% } %>
						</logic:iterate>
					</tr>
				</table>
			</TD>
		</TR>

		<TR>
			<TD valign='top'> 
				Mode:
			</TD>
			<TD>
				<html:select property="mode">
					<html:options property="modes"/>
				</html:select>
			</TD>
		</TR>
		<TR>
			<TD colspan='2' align='right'>
				<html:submit onclick="displayLoading();" property="op" accesskey="N" value="Show Availability" title="Show Location Availability (Alt+N)"/>
				<html:submit onclick="displayLoading();" property="op" accesskey="C" value="Clear" title="Clear Filter (Alt+C)"/>
				<html:submit onclick="displayLoading();" property="op" accesskey="A" value="Add Event" title="Add Event (Alt+A)"/>
				<html:submit onclick="displayLoading();" property="op" accesskey="P" value="Export PDF" title="Export PDF (Alt+P)"/>
				<html:submit onclick="displayLoading();" property="op" accesskey="R" value="Refresh" title="Refresh Page (Alt+R)"/>
			</TD>
		</TR>
	</TABLE>
	<script language="JavaScript">blEnd('dispFilter');blStartCollapsed('dispFilter');</script>
		<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
			<TR>
				<TD colspan='2' align='right'>
					<html:submit onclick="displayLoading();" property="op" accesskey="A" value="Add Event" title="Add Event (Alt+A)"/>
					<html:submit onclick="displayLoading();" property="op" accesskey="P" value="Export PDF" title="Export PDF (Alt+P)"/>
					<html:submit onclick="displayLoading();" property="op" accesskey="R" value="Refresh" title="Refresh Page (Alt+R)"/>
				</TD>
			</TR>
		</TABLE>
	<script language="JavaScript">blEndCollapsed('dispFilter');</script>
	<logic:messagesPresent>
		<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
	    <html:messages id="error">
			<TR><TD class="errorCell">
				${error}
			</TD></TR>
		</html:messages>
		</TABLE>
	</logic:messagesPresent>
	<br>
	<% 
	EventGridForm myForm = (EventGridForm)request.getAttribute("eventGridForm");
	if (myForm.getPossibleLocations() != null && myForm.getMeetingDates() != null && (myForm.getStartTime() < myForm.getStopTime())) {
	  	if (15000 > (myForm.getPossibleLocations().size() * ((myForm.getStopTime() - myForm.getStartTime())/12) * myForm.getMeetingDates().size())){
			new EventGridTable(myForm).printTable(out);
		} else {
		%>
		<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
			<TR><TD class="errorCell">
				<b>Please refine your Room Availablity query to return fewer rooms or dates or try generating a PDF.</b>
			</TD></TR>
		</TABLE>
		<%
		}
	}
	 %>
	<logic:notEmpty scope="request" name="hash">
		<SCRIPT type="text/javascript" language="javascript">
			location.hash = '<%=request.getAttribute("hash")%>';
		</SCRIPT>
	</logic:notEmpty>
<script language="JavaScript">
	function evOver(source, event, eventId, meetingId) {
		source.style.backgroundColor='rgb(223,231,242)';
		source.style.cursor='hand';
		source.style.cursor='pointer';
	}
	function evOut(source, event, eventId, meetingId) {
		source.style.backgroundColor='transparent';
	}
	function evClick(source, event, eventId, meetingId) {
		document.location='eventDetail.do?id='+eventId;
	}   
	var lastTableId = -1, lastRow = -1, lastCol = -1, lastCheck = false;
	function avOver(source, event, tableId, row, col) {
		source.style.backgroundColor='rgb(223,231,242)';
		document.getElementById('a'+tableId+'.'+col).style.backgroundColor='rgb(223,231,242)';
		document.getElementById('b'+tableId+'.'+row).style.backgroundColor='rgb(223,231,242)';
		source.style.cursor='hand';
		source.style.cursor='pointer';
		if (lastTableId==tableId) {
			var c0 = (lastCol<col?lastCol:col);
			var c1 = (lastCol<col?col:lastCol);
			var r0 = (lastRow<row?lastRow:row);
			var r1 = (lastRow<row?row:lastRow);
			for (var c=c0;c<=c1;c++) {
				for (var r=r0;r<=r1;r++) {
					var d = document.getElementById('d'+tableId+'.'+r+'.'+c);
					if ((c!=col || r!=row) && d!=null) {
						d.style.backgroundColor = 'rgb(223,231,242)';
					}
				}
			}
		}
	}
	function avOut(source, event, tableId, row, col) {
		var ch = document.getElementById('c'+tableId+'.'+row+'.'+col);
		source.style.backgroundColor=(ch.checked?'rgb(168,187,225)':'transparent');
		document.getElementById('a'+tableId+'.'+col).style.backgroundColor='transparent';
		document.getElementById('b'+tableId+'.'+row).style.backgroundColor='transparent';
		if (lastTableId==tableId) {
			var c0 = (lastCol<col?lastCol:col);
			var c1 = (lastCol<col?col:lastCol);
			var r0 = (lastRow<row?lastRow:row);
			var r1 = (lastRow<row?row:lastRow);
			for (var c=c0;c<=c1;c++) {
				for (var r=r0;r<=r1;r++) {
					var d = document.getElementById('d'+tableId+'.'+r+'.'+c);
					if ((c!=col || r!=row) && d!=null) {
						d.style.backgroundColor = (document.getElementById('c'+tableId+'.'+r+'.'+c).checked?'rgb(168,187,225)':'transparent');
					}
				}
			}
		}
	}
	
	function avDown(source, event, tableId, row, col) {
		lastTableId = tableId; 
		lastRow = row;
		lastCol = col;
		lastCheck = !document.getElementById('c'+tableId+'.'+row+'.'+col).checked;
	}
	
	function avUp(source, event, tableId, row, col) {
		if (lastTableId==tableId && (lastRow!=row || col!=lastCol)) {
			var c0 = (lastCol<col?lastCol:col);
			var c1 = (lastCol<col?col:lastCol);
			var r0 = (lastRow<row?lastRow:row);
			var r1 = (lastRow<row?row:lastRow);
			for (var c=c0;c<=c1;c++) {
				for (var r=r0;r<=r1;r++) {
					var ch = document.getElementById('c'+tableId+'.'+r+'.'+c);
					if (ch!=null) {
						ch.checked = lastCheck;//!ch.checked;
						if (c!=col || r!=row) {
							var d = document.getElementById('d'+tableId+'.'+r+'.'+c);
							d.style.backgroundColor = (ch.checked?'rgb(168,187,225)':'transparent');
						}
					}
				}
			}
		}
		lastTableId = -1;
	}

	function avClick(source, event, tableId, row, col) {
		var ch = document.getElementById('c'+tableId+'.'+row+'.'+col);
		ch.checked = !ch.checked;
		lastTableId = -1;
	}   
	
</script>
</html:form>