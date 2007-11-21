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
<%@ page language="java" %>
<%@ page errorPage="../error.jsp" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<html:form action="/hibernateQueryTest">

	<TABLE align="left" width="95%">
		<TR>
			<TD colspan='2'>
				<tt:section-header>
					<tt:section-title>HQL</tt:section-title>
					<html:submit property="op" accesskey="S" titleKey="button.submit">
						<bean:message key="button.submit"/>
					</html:submit>
				</tt:section-header>
			</TD>
		</TR>
		
		<logic:messagesPresent>
		<TR>
			<TD valign="top">
				Errors:
			</TD>
			<TD class="errorCell">
				<html:messages id="error">
					${error}<br>
			    </html:messages>
			</TD>
		</TR>
		</logic:messagesPresent>

		<TR>
			<TD valign="top">
				Query:
			</TD>
			<TD>
				<html:textarea property="query" rows="12" cols="120"></html:textarea>
			</TD>
		</TR>
		
		<TR>
			<TD colspan='2'>
				&nbsp;
			</TD>
		</TR>

		<logic:notEmpty name="hibernateQueryTestForm" property="listSize">		
			<TR>
				<TD colspan='2'>
					<tt:section-title>Result (<bean:write name="hibernateQueryTestForm" property="listSize" /> lines)</tt:section-title>
				</TD>
			</TR>
		
			<logic:notEmpty scope="request" name="result">
				<TR>
					<TD colspan='2'>
						<bean:write scope="request" name="result" filter="false"/>
					</TD>
				</TR>
			</logic:notEmpty>
		</logic:notEmpty>
		
		<TR>
			<TD colspan='2'>
				<tt:section-title/>
			</TD>
		</TR>
		
		<TR>
			<TD colspan='2' align="right">
				<html:submit property="op" accesskey="S" titleKey="button.submit">
					<bean:message key="button.submit"/>
				</html:submit>
			</TD>
		</TR>

	</TABLE>

</html:form>	