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
<%@ page import="org.unitime.timetable.model.Roles" %>
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.util.Constants" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/struts-layout.tld" prefix="layout" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<tiles:importAttribute />
<%
	String focusElement = "distType";
	if (request.getAttribute("addedClass")!=null) {
		focusElement = "subjectArea[" + request.getAttribute("addedClass").toString() + "]";
	}
	if (request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)!=null)
		focusElement = null;
%> 
<html:form action="/distributionPrefs" focus="<%= focusElement %>">
	<INPUT type="hidden" name="deleteType" id="deleteType" value="">
	<INPUT type="hidden" name="deleteId" id="deleteId" value="">
	<INPUT type="hidden" name="reloadCause" id="reloadCause" value="">
	<INPUT type="hidden" name="reloadId" id="reloadId" value="">
	<INPUT type="hidden" name="op2" value="">
	<html:hidden property="distPrefId"/>

	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<% if (request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)==null) { %>	
		
		<TR>
			<TD valign="middle" colspan='3'>
				<tt:section-header>
					<tt:section-title>
						<logic:notEmpty name="distributionPrefsForm" property="distPrefId">
							Edit
						</logic:notEmpty>
						<logic:empty name="distributionPrefsForm" property="distPrefId">
							Add
						</logic:empty>
						Distribution Preference
					</tt:section-title>
					<logic:notEmpty name="distributionPrefsForm" property="distPrefId">
						<html:submit styleClass="btn" property="op" accesskey="U" titleKey="title.updateDistPref">
							<bean:message key="button.update" />
						</html:submit>
						
						&nbsp;
						<html:submit styleClass="btn" property="op" accesskey="D" titleKey="title.removeDistPref" onclick="javascript: doDel('distPref', '-1');">
							<bean:message key="button.delete" />
						</html:submit>					
					</logic:notEmpty>
				
					<logic:empty name="distributionPrefsForm" property="distPrefId">
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
					<logic:notEmpty name="distributionPrefsForm" property="distPrefId">
						<tt:back styleClass="btn" name="Back" title="Return to %% (Alt+B)" accesskey="B" back="1" type="PreferenceGroup">
							<bean:write name="distributionPrefsForm" property="distPrefId"/>
						</tt:back>
					</logic:notEmpty>
					<logic:empty name="distributionPrefsForm" property="distPrefId">
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
				<span id='distTypeDesc' style='display:block;padding:3px; max-width: 800px;'>
					<bean:write name="distributionPrefsForm" property="description" filter="false"/>
				</span>
			</TD>
		</TR>
		
		<TR>
			<TD nowrap valign='top'>Structure: <font class="reqField">*</font></TD>
			<TD colspan='2'>
				<html:select property="grouping" onchange="javascript: groupingChanged(this.value);" > <!-- onchange="op2.value='GroupingChange';submit();" -->
					<html:option value="-">-</html:option>
					<html:options name="distributionPrefsForm" property="groupings"/>
				</html:select>
				<span id='groupingDesc' style='display:block;padding:3px; max-width: 800px;'>
					<bean:write name="distributionPrefsForm" property="groupingDescription" filter="false"/>
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
				<tt:section-header title='Classes in Distribution'>
					<html:submit styleClass="btn" property="op" accesskey="C" titleKey="title.addClass_" style="width: 100px">
						<bean:message key="button.addClass_" />
					</html:submit>
				</tt:section-header>
			</TD>
		</TR>

		<logic:iterate name="distributionPrefsForm" property="subjectArea" id="sa" indexId="ctr">
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
					<html:options collection="<%=DistributionPrefsForm.SUBJ_AREA_ATTR_LIST+ctr%>" property="value" labelProperty="label" />
				</html:select>

				<html:select style="width:470;" 
					property='<%= "courseNbr[" + ctr + "]" %>' 
					onchange="<%= \"javascript: doReload('courseNbr', '\" + ctr + \"');\" %>"
					onfocus="setUp();" 
					onkeypress="return selectSearch(event, this);" 
					onkeydown="<%= \"var y=checkKey(event, this); if(y && isModified()) { doReload('courseNbr', '\" + ctr + \"'); }return y;\" %>" 
					styleId='<%="courseNbr"+ctr%>' >
					<html:option value="-">-</html:option>
					<html:options collection="<%=DistributionPrefsForm.CRS_NUM_ATTR_LIST+ctr%>" property="value" labelProperty="label" />
				</html:select>

				<html:select style="width:150;" 
					property='<%= "itype[" + ctr + "]" %>' 
					onchange="<%= \"javascript: doReload('itype', '\" + ctr + \"');\" %>"
					styleId='<%="itype"+ctr%>' >
					<html:option value="-">-</html:option>
					<html:options collection="<%=DistributionPrefsForm.ITYPE_ATTR_LIST+ctr%>" property="value" labelProperty="label" filter="false"/>
				</html:select>

				<html:select style="width:80;" property='<%= "classNumber[" + ctr + "]" %>' styleId='<%="classNumber"+ctr%>'>
					<html:option value="-">-</html:option>
					<html:option value="-1">All</html:option>
					<html:options collection="<%=DistributionPrefsForm.CLASS_NUM_ATTR_LIST+ctr%>" property="value" labelProperty="label" />
				</html:select>
				
				<!-- Arrows -->
				<logic:greaterThan name="ctr" value="0">
					<A href="#null" onClick="javascript: doReload('moveUp', '<%=ctr%>');"><IMG border="0" src="images/arrow_u.gif" alt="Move Up" title="Move Up" align="top"></A>
				</logic:greaterThan>

				<logic:equal name="ctr" value="0">
					<IMG border="0" src="images/blank.gif" align="top">
				</logic:equal>

				<logic:lessThan name="ctr" value="<%=request.getAttribute(DistributionPrefsForm.LIST_SIZE_ATTR).toString()%>">
					<A href="#null" onClick="javascript: doReload('moveDown', '<%=ctr%>');"><IMG border="0" src="images/arrow_d.gif" alt="Move Down" title="Move Down" align="top"></A>
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
				<logic:notEmpty name="distributionPrefsForm" property="distPrefId">
					<html:submit styleClass="btn" property="op" accesskey="U" titleKey="title.updateDistPref">
						<bean:message key="button.update" />
					</html:submit>
					
					&nbsp;
					<html:submit styleClass="btn" property="op" accesskey="D" titleKey="title.removeDistPref" onclick="javascript: doDel('distPref', '-1');">
						<bean:message key="button.delete" />
					</html:submit>					
				</logic:notEmpty>
				
				<logic:empty name="distributionPrefsForm" property="distPrefId">
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
				<logic:notEmpty name="distributionPrefsForm" property="distPrefId">
					<tt:back styleClass="btn" name="Back" title="Return to %% (Alt+B)" accesskey="B" back="1" type="PreferenceGroup">
						<bean:write name="distributionPrefsForm" property="distPrefId"/>
					</tt:back>
				</logic:notEmpty>
				<logic:empty name="distributionPrefsForm" property="distPrefId">
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
					<B>Subject: </B>
					<html:select name="distributionPrefsForm" property="filterSubjectAreaId"
						onfocus="setUp();" 
						onkeypress="return selectSearch(event, this);" 
						onkeydown="return checkKey(event, this);" >
						<html:option value="<%=Constants.BLANK_OPTION_VALUE%>"><%=Constants.BLANK_OPTION_LABEL%></html:option>
						<html:option value="<%=Constants.ALL_OPTION_VALUE%>"><%=Constants.ALL_OPTION_LABEL%></html:option>
						<html:optionsCollection property="filterSubjectAreas" label="subjectAreaAbbreviation" value="uniqueId" />
					</html:select>
					<B>Course Number: </B>
					<layout:suggest 
						suggestAction="/getCourseNumbers" property="filterCourseNbr" styleId="courseNbr" 
						suggestCount="15" size="5" maxlength="5" layout="false" all="true"
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
			
			<TR>
				<TD colspan="2">
					<script language="javascript">displayLoading();</script>
					<TABLE width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:0;">
						<%=request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)%>
					</TABLE>
					<script language="javascript">displayElement('loading', false);</script>
				</TD>
			</TR>
			<TR>
				<TD colspan="2" align="right">
					<DIV class="WelcomeRowHeadBlank">&nbsp;</DIV>
				</TD>
			</TR>
			<TR>
				<% 
					if (!Web.getUser(request.getSession()).getCurrentRole().equals(Roles.VIEW_ALL_ROLE)) {
				 %>
					<TD colspan="2" align="right">
						<html:submit property="op" styleClass="btn" accesskey="A" title="Add New Distribution Preference (Alt+A)" >
							<bean:message key="button.addDistPref" />
						</html:submit>
					</TD>
				<%
					}
				 %>
			</TR>
		<% } %>

	</TABLE>

</html:form>

<SCRIPT type="text/javascript" language="javascript">
	var reload = false;

	function doDel(type, id) {
		var delType = document.distributionPrefsForm.deleteType;
		delType.value = type;

		var delId = document.distributionPrefsForm.deleteId;
		delId.value = id;
	}
	
	function doReload(type, id) {	
		if (type=='subjectArea' || type=='courseNbr' || type=='itype') {
			doAjax(type,id);
			return;
		}
		var reloadId = document.distributionPrefsForm.reloadId;
		reloadId.value = id;

		var reloadCause = document.distributionPrefsForm.reloadCause;
		reloadCause.value = type;
		
		document.distributionPrefsForm.submit();
	}

	function doAjax(type, idx) {
		var subjAreaObj = document.getElementById('subjectArea'+idx);
		var courseNbrObj = document.getElementById('courseNbr'+idx);
		var itypeObj = document.getElementById('itype'+idx);
		var classNumberObj = document.getElementById('classNumber'+idx);

		var id = null;
		var options = null;
		var next = null;
		
		if (type=='subjectArea') {
			id = subjAreaObj.value;
			options = courseNbrObj.options;
			next = 'courseNbr';
			courseNbrObj.options.length=1;
			itypeObj.options.length=1;
			classNumberObj.options.length=1;
		} else if (type=='courseNbr') {
			id = courseNbrObj.value;
			options = itypeObj.options;
			next = 'itype';
			itypeObj.options.length=1;
			classNumberObj.options.length=1;
		} else if (type=='itype') {
			id = itypeObj.value;
			options = classNumberObj.options;
			classNumberObj.options.length=1;
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
							while (optVal.indexOf('_')>=0 && type=='itype')
								optVal = optVal.replace("_",String.fromCharCode(160,160,160,160));
							options[i+1]=new Option(optVal, optId, false);
						}
					}
				}
			}
			if (options.length==2 || type=='itype') {
				options[1].selected=true;
				if (next!=null) doReload(next,idx);
			}
		};
	
		// Request
		var vars = "id="+id+"&type="+type;
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

	function groupingChanged(id) {
		var descObj = document.getElementById('groupingDesc');
		
		if (id=='-') {
			descObj.innerHTML='';
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
						var count = xmlDoc.documentElement.childNodes.length;
						for(i=0; i<count; i++) {
							var optId = xmlDoc.documentElement.childNodes[i].getAttribute("id");
							var optVal = xmlDoc.documentElement.childNodes[i].getAttribute("value");
							if (optId=='desc') {
								var desc = optVal;
								while (desc.indexOf('@lt@')>=0) desc = desc.replace('@lt@','<');
								while (desc.indexOf('@gt@')>=0) desc = desc.replace('@gt@','>');
								while (desc.indexOf('@quot@')>=0) desc = desc.replace('@quot@','"');
								while (desc.indexOf('@amp@')>=0) desc = desc.replace('@amp@','&');
								descObj.innerHTML = desc;
							}
						}
					}
				}
			}
		};
	
		// Request
		var vars = "id="+id+"&type=grouping";
		req.open( "POST", "distributionPrefsAjax.do", true );
		req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		req.setRequestHeader("Content-Length", vars.length);
		//setTimeout("try { req.send('" + vars + "') } catch(e) {}", 1000);
		req.send(vars);
	}	
</SCRIPT>
				
		
