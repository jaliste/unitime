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
<%@ page language="java" autoFlush="true" errorPage="../error.jsp"%>
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.form.InstructorEditForm" %>
<%@ page import="org.unitime.timetable.webutil.JavascriptFunctions" %>
<%@ page import="org.unitime.timetable.model.PositionType" %>
<%@ page import="org.unitime.timetable.util.Constants" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<%
	// Get Form 
	String operation = "update";	
	String frmName = "instructorEditForm";	
	InstructorEditForm frm = (InstructorEditForm) request.getAttribute(frmName);	
%>	

<SCRIPT language="javascript">
	<!--
		<%= JavascriptFunctions.getJsConfirm(Web.getUser(session)) %>
		function confirmDelete() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			return ( confirm('Any instructor class assignments will be deleted as well. Continue?'));
		}
		
	// -->
</SCRIPT>

<html:form action="instructorInfoEdit">
	<html:hidden property="instructorId"/>
	<html:hidden property="nextId"/>
	<html:hidden property="previousId"/>
	<html:hidden property="lookupEnabled"/>
	
	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<%@include file="instructorLookup.jspf" %>
		<% 
		if (frm.getMatchFound()==null || !frm.getMatchFound().booleanValue()) {
		%>
		<%@include file="instructor.jspf" %>
	<% } %>
		
	</TABLE>
</html:form>

