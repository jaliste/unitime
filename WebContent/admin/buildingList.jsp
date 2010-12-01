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
<%@ page import="org.unitime.commons.web.*" %>
<%@ page import="java.text.DecimalFormat" %>
<%@page import="net.sf.cpsolver.ifs.util.DistanceMetric"%>
<%@page import="org.unitime.timetable.ApplicationProperties"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<table width="100%" border="0" cellspacing="0" cellpadding="3">
	<tr><td colspan='5' nowrap>
		<tt:section-header>
			<tt:section-title>Buildings</tt:section-title>
				<table border='0'><tr><td>
				<html:form action="buildingEdit" styleClass="FormWithNoPadding">
					<html:hidden property="op" value="Add"/>
					<html:submit onclick="displayLoading();" styleClass="btn" accesskey="A" title="Add Building (Alt+B)" value="Add Building"/>
				</html:form>
				</td><td>
				<html:form action="buildingEdit" styleClass="FormWithNoPadding">
					<html:hidden property="op" value="Export PDF"/>
					<html:submit onclick="displayLoading();" styleClass="btn" accesskey="P" title="Export PDF (Alt+P)" value="Export PDF"/>
				</html:form>
				</td><td>
				<html:form action="buildingEdit" styleClass="FormWithNoPadding">
					<html:hidden property="op" value="Update Data"/>
					<html:submit onclick="displayLoading();" styleClass="btn" accesskey="U" title="Synchronize classrooms and computing labs with external rooms (Alt+U)" value="Update Data"/>
				</html:form>
				</td></tr></table>
		</tt:section-header>
	</td></tr>
<%
	DistanceMetric.Ellipsoid ellipsoid = DistanceMetric.Ellipsoid.valueOf(ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name()));
    WebTable webTable = new WebTable( 5,
    null, "buildingList.do?ord=%%",
    new String[] {"Abbreviation", "Name", "External ID", ellipsoid.getFirstCoordinateName(), ellipsoid.getSecondCoordinateName()},
    new String[] {"left", "left","left","right","right"},
    new boolean[] {true,true,true,true,true} );
    WebTable.setOrder(session, "BuildingList.ord", request.getParameter("ord"), 1);
%>

<logic:iterate name="buildingListForm" property="buildings" id="bldg" >
<%
org.unitime.timetable.model.Building b = (org.unitime.timetable.model.Building) bldg;
DecimalFormat df5 = new DecimalFormat("####0.######");
webTable.addLine(
	"onClick=\"document.location='buildingEdit.do?op=Edit&id="+b.getUniqueId()+"';\"",
	new String[] {
		b.getAbbreviation(),
		b.getName(),
		b.getExternalUniqueId()==null?"<i>N/A</i>":b.getExternalUniqueId().toString(),
		(b.getCoordinateX()==null ? "" : df5.format(b.getCoordinateX())),
		(b.getCoordinateY()==null ? "" : df5.format(b.getCoordinateY()))
		}, 
	new Comparable[] {
		b.getAbbreviation(),
		b.getName(),
		b.getExternalUniqueId()==null?"":b.getExternalUniqueId().toString(),
		b.getCoordinateX(),
		b.getCoordinateY()
		});
%>

</logic:iterate>

<%	out.println( webTable.printTable(WebTable.getOrder(session, "BuildingList.ord")) ); %>


	<TR>
		<TD colspan='5' align="right" class="WelcomeRowHead">
		&nbsp;
		</TD>
	</TR>
	<TR>
		<TD colspan='5' align="right" nowrap width="100%">
				<table border='0'><tr><td>
				<html:form action="buildingEdit" styleClass="FormWithNoPadding">
					<html:hidden property="op" value="Add"/>
					<html:submit onclick="displayLoading();" styleClass="btn" accesskey="A" title="Add Building (Alt+B)" value="Add Building"/>
				</html:form>
				</td><td>
				<html:form action="buildingEdit" styleClass="FormWithNoPadding">
					<html:hidden property="op" value="Export PDF"/>
					<html:submit onclick="displayLoading();" styleClass="btn" accesskey="P" title="Export PDF (Alt+P)" value="Export PDF"/>
				</html:form>
				</td><td>
				<html:form action="buildingEdit" styleClass="FormWithNoPadding">
					<html:hidden property="op" value="Update Data"/>
					<html:submit onclick="displayLoading();" styleClass="btn" accesskey="U" title="Synchronize classrooms and computing labs with external rooms (Alt+U)" value="Update Data"/>
				</html:form>
				</td></tr></table>
		</TD>
	</TR>
</table>
