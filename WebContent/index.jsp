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
<%@ page import="org.unitime.timetable.ApplicationProperties" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
	<HEAD>
		<META http-equiv="Content-Type"	content="text/html; charset=ISO-8859-1">
		<META http-equiv="Refresh" content="1; url=login.do">		
		<LINK rel="stylesheet" type="text/css" href="styles/timetabling.css">
		<TITLE>Timetabling</TITLE>
	</HEAD>
	<BODY>
		<% if (ApplicationProperties.getProperty("tmtbl.header.external", "").trim().length()>0) { %>
			<jsp:include flush="true" page='<%=ApplicationProperties.getProperty("tmtbl.header.external")%>' />
		<% } %>
		<BR>
	</BODY>
</HTML>
