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
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean"%> 
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<TABLE width="90%" border="0" cellspacing="0" cellpadding="3">
	<TR>
		<TD colspan='7'>
			<tt:section-header>
				<tt:section-title>Instructional Types</tt:section-title>
				<TABLE align="right" cellspacing="0" cellpadding="2" class="FormWithNoPadding">
					<TR><TD nowrap>
						<html:form action="itypeDescEdit" styleClass="FormWithNoPadding">
							<html:submit property="op" onclick="displayLoading();" styleClass="btn" accesskey="I" titleKey="title.addIType">
								<bean:message key="button.addIType" />
							</html:submit>
						</html:form>
					</TD><TD nowrap>
						<input type='button' onclick="document.location='itypeDescList.do?op=Export%20PDF';" title='Export PDF (Alt+P)' accesskey="P" class="btn" value="Export PDF">
					</TD></TR>
				</TABLE>
			</tt:section-header>
		</TD>
	</TR>
	<%=request.getAttribute("itypeDescList")%>
	<TR>
		<TD colspan='7'>
			<tt:section-title/>
		</TD>
	</TR>
	<TR>
		<TD colspan='7' align="right">
			<TABLE align="right" cellspacing="0" cellpadding="2" class="FormWithNoPadding">
				<TR><TD nowrap>
					<html:form action="itypeDescEdit" styleClass="FormWithNoPadding">
						<html:submit property="op" onclick="displayLoading();" styleClass="btn" accesskey="I" titleKey="title.addIType">
							<bean:message key="button.addIType" />
						</html:submit>
					</html:form>
				</TD><TD nowrap>
					<input type='button' onclick="document.location='itypeDescList.do?op=Export%20PDF';" title='Export PDF (Alt+P)' accesskey="P" class="btn" value="Export PDF">
				</TD></TR>
			</TABLE>
		</TD>
	</TR>
</TABLE>
