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
<%@ page import="org.unitime.timetable.util.Constants" %>

<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<tiles:importAttribute />

<html:form action="/designatorList">
	<TABLE border="0" cellspacing="1" cellpadding="3">
		<TR>
			<TD valign="top"><B>Subject: </B></TD>
			<TD valign="top">
				<html:select name="designatorListForm" property="subjectAreaId"
					onfocus="setUp();" 
					onkeypress="return selectSearch(event, this);" 
					onkeydown="return checkKey(event, this);">
					<html:option value="<%=Constants.BLANK_OPTION_VALUE%>"><%=Constants.BLANK_OPTION_LABEL%></html:option>
					<html:optionsCollection property="subjectAreas"	label="subjectAreaAbbreviation" value="uniqueId" />
				</html:select>
			</TD>
			<TD valign="top">
				&nbsp;&nbsp;&nbsp;
				<html:submit property="op" 
					accesskey="S" styleClass="btn" titleKey="title.displayDesignatorList"
					onclick="displayLoading();">
					<bean:message key="button.displayDesignatorList" />
				</html:submit> 
			</TD>
			<TD valign="top">
				<html:submit property="op" 
					accesskey="P" styleClass="btn" titleKey="title.exportPDF">
					<bean:message key="button.exportPDF" />
				</html:submit> 
			</TD>
			<logic:equal name="designatorListForm" property="editable" value="true">
				<TD valign="top">
					<html:submit property="op" 
						accesskey="A" styleClass="btn" titleKey="title.addDesignator">
						<bean:message key="button.addDesignator" />
					</html:submit> 
				</TD>
			</logic:equal>
		</TR>
		<TR>
			<TD colspan="4" align="center">
				<html:errors />
			</TD>
		</TR>
	</TABLE>	
</html:form>

<%
	if (request.getAttribute("designatorList")!=null) {
%>
	<script language="javascript">displayLoading();</script>
	<TABLE border="0" cellspacing="1" cellpadding="3">
		<%=request.getAttribute("designatorList")%>
	</TABLE>	
	<script language="javascript">displayElement('loading', false);</script>
<%		
	}
%>
		
