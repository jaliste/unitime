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
--%>
<%@ page language="java" autoFlush="true" errorPage="../error.jsp" %>
<%@ page import="org.unitime.timetable.form.DistributionPrefsForm" %>
<%@ page import="org.unitime.timetable.model.DistributionPref" %>
<%@ page import="org.unitime.timetable.model.DistributionType" %>
<%@ page import="org.unitime.timetable.model.PreferenceLevel" %>
<%@ page import="org.unitime.timetable.util.Constants" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/struts-layout.tld" prefix="layout" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<tiles:importAttribute />
<html:form action="/examDistributionPrefs" >
	<INPUT type="hidden" name="deleteType" id="deleteType" value="">
	<INPUT type="hidden" name="deleteId" id="deleteId" value="">
	<INPUT type="hidden" name="reloadCause" id="reloadCause" value="">
	<INPUT type="hidden" name="reloadId" id="reloadId" value="">
	<INPUT type="hidden" name="op2" value="">
	<html:hidden property="distPrefId"/>

	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<% if (request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)==null) { %>	
		<html:hidden property="examType"/>
		<TR>
			<TD valign="middle" colspan='3'>
				<tt:section-header>
					<tt:section-title>
						<logic:notEmpty name="examDistributionPrefsForm" property="distPrefId">
							Edit
						</logic:notEmpty>
						<logic:empty name="examDistributionPrefsForm" property="distPrefId">
							Add
						</logic:empty>
						Examination Distribution Preference
					</tt:section-title>
					<logic:notEmpty name="examDistributionPrefsForm" property="distPrefId">
						<html:submit styleClass="btn" property="op" accesskey="U" titleKey="title.updateDistPref">
							<bean:message key="button.update" />
						</html:submit>
						
						&nbsp;
						<html:submit styleClass="btn" property="op" accesskey="D" titleKey="title.removeDistPref" onclick="javascript: doDel('distPref', '-1');">
							<bean:message key="button.delete" />
						</html:submit>					
					</logic:notEmpty>
				
					<logic:empty name="examDistributionPrefsForm" property="distPrefId">
						&nbsp;
						<html:submit styleClass="btn" property="op" accesskey="S" titleKey="title.addNewDistPref">
							<bean:message key="button.save" />
						</html:submit>
					</logic:empty>

					<!-- 
					<html:submit property="op" accesskey="C">
						<bean:message key="button.cancel" />
					</html:submit>
					-->

					&nbsp;
					<logic:notEmpty name="examDistributionPrefsForm" property="distPrefId">
						<tt:back styleClass="btn" name="Back" title="Return to %% (Alt+B)" accesskey="B" back="1" type="PreferenceGroup">
							<bean:write name="examDistributionPrefsForm" property="distPrefId"/>
						</tt:back>
					</logic:notEmpty>
					<logic:empty name="examDistributionPrefsForm" property="distPrefId">
						<tt:back styleClass="btn" name="Back" title="Return to %% (Alt+B)" accesskey="B" back="1"/>
					</logic:empty>
					
				</tt:section-header>
			</TD>
		</TR>
				
		<logic:messagesPresent>
		<TR>
			<TD colspan="3" align="left" class="errorCell">
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
			<TD nowrap valign='top'>Distribution Type: <font class="reqField">*</font></TD>
			<TD colspan='2' width='100%'>
				<html:select style="width:300;" property="distType" onchange="javascript: distTypeChanged(this.value);"> <!-- op2.value='DistTypeChange';submit(); -->
					<html:option value="-">-</html:option>
					<html:options collection="<%=DistributionType.DIST_TYPE_ATTR_NAME%>" property="uniqueId" labelProperty="label" />
				</html:select>
				<span id='distTypeDesc' style='display:block;padding:3px'>
					<bean:write name="examDistributionPrefsForm" property="description" filter="false"/>
				</span>
			</TD>
		</TR>
		
		<TR>
			<TD>Preference: <font class="reqField">*</font></TD>
			<TD>
				<html:select style="width:200;" property="prefLevel">					
					<html:option value="-">-</html:option>
					<logic:iterate scope="request" name="<%=PreferenceLevel.PREF_LEVEL_ATTR_NAME%>" id="prLevel">
					<% PreferenceLevel pr = (PreferenceLevel)prLevel; %>			
					<html:option
						style='<%="background-color:" + pr.prefcolor() + ";"%>'
						value="<%=pr.getUniqueId().toString()%>" ><%=
						pr.getPrefName()
					%></html:option>
				   	</logic:iterate>
				</html:select>
			</TD>
			<TD>&nbsp;</TD>
		</TR>

		<TR><TD colspan='3'>&nbsp;</TD></TR>
		<TR>
			<TD valign="middle" colspan='3'>
				<tt:section-header>
					<tt:section-title>
						<logic:iterate name="examDistributionPrefsForm" property="examTypes" id="et">
							<bean:define name="et" property="value" id="examType"/>
							<logic:equal name="examDistributionPrefsForm" property="examType" value="<%=(String)examType%>">
								<bean:write name="et" property="label"/>			
							</logic:equal>
						</logic:iterate>
						Examinations in Distribution
					</tt:section-title>
					<html:submit styleClass="btn" property="op" accesskey="A" titleKey="title.addExam" >
						<bean:message key="button.addExam" />
					</html:submit>
				</tt:section-header>
			</TD>
		</TR>

		<logic:iterate name="examDistributionPrefsForm" property="subjectAreaList" id="sa" indexId="ctr">
		<TR>
			<TD colspan="3">
			
				<!-- Class / Subpart -->
				<html:select style="width:90;" 
					property='<%= "subjectArea[" + ctr + "]" %>' 
					onchange="<%= \"javascript: doReload('subjectArea', '\" + ctr + \"');\" %>"
					onfocus="setUp();" 
					onkeypress="return selectSearch(event, this);" 
					onkeydown="<%= \"var y=checkKey(event, this); if(y && isModified()) { doReload('subjectArea', '\" + ctr + \"'); }return y;\" %>" 
					styleId='<%="subjectArea"+ctr%>' >
					<html:option value="-">-</html:option>
					<html:optionsCollection property="filterSubjectAreas" label="subjectAreaAbbreviation" value="uniqueId"/>
				</html:select>

				<html:select style="width:290;" 
					property='<%= "courseNbr[" + ctr + "]" %>' 
					onchange="<%= \"javascript: doReload('courseNbr[', '\" + ctr + \"]');\" %>"
					onfocus='setUp();'
					onkeypress="return selectSearch(event, this);" 
					onkeydown="<%= \"var y=checkKey(event, this); if(y && isModified()) { doReload('exam', '\" + ctr + \"'); }return y;\" %>" 
					styleId='<%="courseNbr"+ctr%>' >
					<html:optionsCollection property='<%="courseNbrs["+ctr+"]"%>' label="value" value="id"/>
				</html:select>

				<html:select style="width:300;" 
					property='<%= "exam[" + ctr + "]" %>' 
					styleId='<%="exam"+ctr%>' >
					<html:optionsCollection property='<%="exams["+ctr+"]"%>' label="value" value="id"/>
				</html:select>
				
				<!-- Arrows -->
				<logic:greaterThan name="ctr" value="0">
					<IMG border="0" src="images/arrow_u.gif" alt="Move Up" title="Move Up" align="top"
						onMouseOver="this.style.cursor='hand';this.style.cursor='pointer';"
						onClick="javascript: doReload('moveUp', '<%=ctr%>');">
				</logic:greaterThan>

				<logic:equal name="ctr" value="0">
					<IMG border="0" src="images/blank.gif" align="top">
				</logic:equal>

				<logic:lessThan name="ctr" value="<%=request.getAttribute(DistributionPrefsForm.LIST_SIZE_ATTR).toString()%>">
					<IMG border="0" src="images/arrow_d.gif" alt="Move Down" title="Move Down" align="top"
						onMouseOver="this.style.cursor='hand';this.style.cursor='pointer';"
						onClick="javascript: doReload('moveDown', '<%=ctr%>');">
				</logic:lessThan>

				<logic:equal name="ctr" value="<%=request.getAttribute(DistributionPrefsForm.LIST_SIZE_ATTR).toString()%>">
					<IMG border="0" src="images/blank.gif" align="top">
				</logic:equal>

				<!-- Delete button -->
				&nbsp;&nbsp;				
				<html:submit styleClass="btn" property="op" onclick="<%= \"javascript: doDel('distObject', '\" + ctr + \"');\" %>">
					<bean:message key="button.delete" />
				</html:submit> 			
				<!--
				<IMG src="images/Delete16.gif" border="0" align="middle">	
				-->
			</TD>
		</TR>
		</logic:iterate>
		
		<TR>
			<TD colspan="3">
				<tt:section-title/>
			</TD>
		</TR>
			
		<TR>
			<TD colspan="3" align="right">
				<logic:notEmpty name="examDistributionPrefsForm" property="distPrefId">
					<html:submit styleClass="btn" property="op" accesskey="U" titleKey="title.updateDistPref">
						<bean:message key="button.update" />
					</html:submit>
					
					&nbsp;
					<html:submit styleClass="btn" property="op" accesskey="D" titleKey="title.removeDistPref" onclick="javascript: doDel('distPref', '-1');">
						<bean:message key="button.delete" />
					</html:submit>					
				</logic:notEmpty>
				
				<logic:empty name="examDistributionPrefsForm" property="distPrefId">
					&nbsp;
					<html:submit styleClass="btn" property="op" accesskey="S" titleKey="title.addNewDistPref">
						<bean:message key="button.save" />
					</html:submit>
				</logic:empty>
				
				<!-- 
				<html:submit property="op" accesskey="C">
					<bean:message key="button.cancel" />
				</html:submit>
				-->
				
				&nbsp;
				<logic:notEmpty name="examDistributionPrefsForm" property="distPrefId">
					<tt:back styleClass="btn" name="Back" title="Return to %% (Alt+B)" accesskey="B" back="1" type="PreferenceGroup">
						<bean:write name="examDistributionPrefsForm" property="distPrefId"/>
					</tt:back>
				</logic:notEmpty>
				<logic:empty name="examDistributionPrefsForm" property="distPrefId">
					<tt:back styleClass="btn" name="Back" title="Return to %% (Alt+B)" accesskey="B" back="1"/>
				</logic:empty>
			</TD>
		</TR>
			
		<!-- 
		<TR>
			<TD colspan="2" class="font8Gray">
				To edit/delete any preference from the list below, simply click on the line
				<br>data will be shown in the form above with the buttons to update/delete
			</TD>
		</TR>
		-->

		<% } else { %>
			<TR>
				<TD colspan="2">
					<B>Type: </B>
					<html:select name="examDistributionPrefsForm" property="examType">
						<html:optionsCollection property="examTypes" label="label" value="value" />
					</html:select>
					<B>Subject: </B>
					<html:select name="examDistributionPrefsForm" property="filterSubjectAreaId"
						onfocus="setUp();" 
						onkeypress="return selectSearch(event, this);" 
						onkeydown="return checkKey(event, this);" >
						<html:option value="<%=Constants.BLANK_OPTION_VALUE%>"><%=Constants.BLANK_OPTION_LABEL%></html:option>
						<logic:equal name="examDistributionPrefsForm" property="canSeeAll" value="true">
							<html:option value="<%=Constants.ALL_OPTION_VALUE%>"><%=Constants.ALL_OPTION_LABEL%></html:option>
						</logic:equal>
						<html:optionsCollection property="filterSubjectAreas" label="subjectAreaAbbreviation" value="uniqueId" />
					</html:select>
					<B>Course Number: </B>
					<layout:suggest 
						suggestAction="/getCourseNumbers" property="filterCourseNbr" styleId="courseNbr" 
						suggestCount="15" size="10" maxlength="10" layout="false" all="true"
						minWordLength="2"
						onblur="hideSuggestionList('filterCourseNbr');" />
					&nbsp;&nbsp;&nbsp;
					<html:submit property="op" 
						onclick="displayLoading();"
						accesskey="S" styleClass="btn" titleKey="title.search">
						<bean:message key="button.search" />
					</html:submit> 
					&nbsp;&nbsp;&nbsp;
					<html:submit property="op" 
						accesskey="S" styleClass="btn" titleKey="title.exportPDF">
						<bean:message key="button.exportPDF" />
					</html:submit> 
				</TD>
			</TR>		
		
			<TR>
				<TD colspan="2">
					&nbsp;
				</TD>
			</TR>		
			
		<logic:notEmpty scope="request" name="<%=DistributionPref.DIST_PREF_REQUEST_ATTR%>">
			<TR>
				<TD colspan="2">
					<tt:section-header>
						<tt:section-title>
							<logic:iterate name="examDistributionPrefsForm" property="examTypes" id="et">
								<bean:define name="et" property="value" id="examType"/>
								<logic:equal name="examDistributionPrefsForm" property="examType" value="<%=(String)examType%>">
									<bean:write name="et" property="label"/>			
								</logic:equal>
							</logic:iterate>
							Examination Distribution Preferences
						</tt:section-title>
						<logic:equal name="examDistributionPrefsForm" property="canAdd" value="true">
							<TD colspan="2" align="right">
								<html:submit property="op" styleClass="btn" accesskey="A" title="Add New Distribution Preference (Alt+A)" >
									<bean:message key="button.addDistPref" />
								</html:submit>
							</TD>
						</logic:equal>
					</tt:section-header>
				</TD>
			</TR>		

			<TR>
				<TD colspan="2">
					<script language="javascript">displayLoading();</script>
					<TABLE width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:0;">
						<%=request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)%>
					</TABLE>
					<script language="javascript">displayElement('loading', false);</script>
				</TD>
			</TR>
		</logic:notEmpty>
			<TR>
				<TD colspan="2" align="right">
					<DIV class="WelcomeRowHeadBlank">&nbsp;</DIV>
				</TD>
			</TR>
			<TR>
				<TD colspan="2" align="right">
					<logic:equal name="examDistributionPrefsForm" property="canAdd" value="true">
						<html:submit property="op" styleClass="btn" accesskey="A" title="Add New Distribution Preference (Alt+A)" >
							<bean:message key="button.addDistPref" />
						</html:submit>
					</logic:equal>
				</TD>
			</TR>
		<% } %>

	</TABLE>

</html:form>

<SCRIPT type="text/javascript" language="javascript">
	var reload = false;

	function doDel(type, id) {
		var delType = document.examDistributionPrefsForm.deleteType;
		delType.value = type;

		var delId = document.examDistributionPrefsForm.deleteId;
		delId.value = id;
	}
	
	function doReload(type, id) {	
		if (type=='subjectArea' || type=='exam') {
			doAjax(type,id);
			return;
		}
		var reloadId = document.examDistributionPrefsForm.reloadId;
		reloadId.value = id;

		var reloadCause = document.examDistributionPrefsForm.reloadCause;
		reloadCause.value = type;
		
		document.examDistributionPrefsForm.submit();
	}

	function doAjax(type, idx) {
		var subjAreaObj = document.getElementById('subjectArea'+idx);
		var courseNbrObj = document.getElementById('courseNbr'+idx);
		var examObj = document.getElementById('exam'+idx);
		var examType = document.getElementsByName('examType')[0].value;

		var id = null;
		var options = null;
		var next = null;
		
		if (type=='subjectArea') {
			id = subjAreaObj.value;
			options = courseNbrObj.options;
			next = 'exam';
			courseNbrObj.options.length=1;
			examObj.options.length=1;
			examObj.options.length=1;
			examObj.options[0]=new Option('-', '-1', false);
		} else if (type=='exam') {
			id = courseNbrObj.value;
			options = examObj.options;
			examObj.options.length=0;
		}
		if (id=='-') return;
		
		// Request initialization
		if (window.XMLHttpRequest) req = new XMLHttpRequest();
		else if (window.ActiveXObject) req = new ActiveXObject( "Microsoft.XMLHTTP" );

		// Response
		req.onreadystatechange = function() {
			options.length=1;
			if (req.readyState == 4) {
				if (req.status == 200) {
					// Response
					var xmlDoc = req.responseXML;
					if (xmlDoc && xmlDoc.documentElement && xmlDoc.documentElement.childNodes && xmlDoc.documentElement.childNodes.length > 0) {
						// Course numbers options creation
						var count = xmlDoc.documentElement.childNodes.length;
						for(i=0; i<count; i++) {
							var optId = xmlDoc.documentElement.childNodes[i].getAttribute("id");
							var optVal = xmlDoc.documentElement.childNodes[i].getAttribute("value");
							while (optVal.indexOf('@amp@')>=0) optVal = optVal.replace('@amp@','&');
							options[(type=='exam'?i:i+1)]=new Option(optVal, optId, false);
						}
					}
				}
			}
		};
	
		// Request
		var vars = "id="+id+"&examType="+examType+"&type="+type;
		req.open( "POST", "distributionPrefsAjax.do", true );
		req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		req.setRequestHeader("Content-Length", vars.length);
		//setTimeout("try { req.send('" + vars + "') } catch(e) {}", 1000);
		req.send(vars);
	}
	
	function distTypeChanged(id) {
		var descObj = document.getElementById('distTypeDesc');
		var prefLevObj = document.getElementsByName('prefLevel')[0];
		var options = prefLevObj.options;
		var prefId = prefLevObj.options[prefLevObj.selectedIndex].value;
		
		if (id=='-') {
			descObj.innerHTML='';
			options.length=1;
			return;
		}
		
		// Request initialization
		if (window.XMLHttpRequest) req = new XMLHttpRequest();
		else if (window.ActiveXObject) req = new ActiveXObject( "Microsoft.XMLHTTP" );

		// Response
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200) {
					// Response
					var xmlDoc = req.responseXML;
					if (xmlDoc && xmlDoc.documentElement && xmlDoc.documentElement.childNodes && xmlDoc.documentElement.childNodes.length > 0) {
						options.length=1;
						var count = xmlDoc.documentElement.childNodes.length;
						if (count>0) {
							var desc = xmlDoc.documentElement.childNodes[0].getAttribute("value");
							while (desc.indexOf('@lt@')>=0) desc = desc.replace('@lt@','<');
							while (desc.indexOf('@gt@')>=0) desc = desc.replace('@gt@','>');
							while (desc.indexOf('@quot@')>=0) desc = desc.replace('@quot@','"');
							while (desc.indexOf('@amp@')>=0) desc = desc.replace('@amp@','&');
							descObj.innerHTML=desc;
						}
						for(i=1; i<count; i++) {
							var optId = xmlDoc.documentElement.childNodes[i].getAttribute("id");
							var optVal = xmlDoc.documentElement.childNodes[i].getAttribute("value");
							var optExt = xmlDoc.documentElement.childNodes[i].getAttribute("extra");
							options[i] = new Option(optVal, optId, (prefId==optId));
							options[i].style.backgroundColor=optExt;
						}
					}
				}
			}
		};
	
		// Request
		var vars = "id="+id+"&type=distType";
		req.open( "POST", "distributionPrefsAjax.do", true );
		req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		req.setRequestHeader("Content-Length", vars.length);
		//setTimeout("try { req.send('" + vars + "') } catch(e) {}", 1000);
		req.send(vars);
	}	

</SCRIPT>
				
		
