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
<%@ page language="java" autoFlush="true"%>
<%@ page import="org.unitime.timetable.model.QueryLog"%>
<%@ page import="org.unitime.commons.web.WebTable"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<tiles:importAttribute />

<table width="100%" cellpadding="10" cellspacing="0">
	<% for (QueryLog.ChartWindow ch: QueryLog.ChartWindow.values()) { %>
		<tr><td colspan="<%=QueryLog.ChartType.values().length%>">
			<tt:section-title><%=ch.getName()%></tt:section-title>
		</td></tr>
		<tr>
		<% for (QueryLog.ChartType t: QueryLog.ChartType.values()) { %>
			<td><img src="<%=QueryLog.getChart(ch, t)%>" border="0"/></td>
		<% } %>
		</tr>
	<% } %>
</table>
<table width="100%" cellpadding="2" cellspacing="0">
	<% WebTable.setOrder(session,"pageStats.ord",request.getParameter("ord"), 1); %>
	<%=QueryLog.getTopQueries(7).printTable(WebTable.getOrder(session, "pageStats.ord"))%>
</table>
