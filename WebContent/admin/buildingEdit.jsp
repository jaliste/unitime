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
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.webutil.JavascriptFunctions" %>
<%@page import="net.sf.cpsolver.ifs.util.DistanceMetric"%>
<%@page import="org.unitime.timetable.ApplicationProperties"%>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>

<tiles:importAttribute />

<SCRIPT language="javascript">
	<!--
		<%= JavascriptFunctions.getJsConfirm(Web.getUser(session)) %>
		
		function confirmDelete() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			if(confirm('The building and all its rooms will be deleted. Continue?')) {
				return true;
			}
			return false;
		}

	// -->
</SCRIPT>


<html:form action="/buildingEdit">
	<html:hidden property="uniqueId"/>

	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD colspan="3">
			<tt:section-header>
					<tt:section-title>
						<logic:equal name="buildingEditForm" property="op" value="Save">
						Add
						</logic:equal>
						<logic:equal name="buildingEditForm" property="op" value="Update">
						Edit
						</logic:equal>
						Building
					</tt:section-title>
					<logic:equal name="buildingEditForm" property="op" value="Save">
						<html:submit property="op" value="Save" title="Save (Alt+S)" accesskey="S"/> 
					</logic:equal>
					<logic:equal name="buildingEditForm" property="op" value="Update">
						<html:submit property="op" value="Update" title="Update (Alt+U)" accesskey="U"/> 
						<html:submit property="op" value="Delete" title="Delete (Alt+D)" accesskey="D" onclick="return confirmDelete();"/> 
					</logic:equal>
					<html:submit property="op" value="Back" title="Back (Alt+B)" accesskey="B"/> 
				</tt:section-header>
			</TD>
		</TR>

		<TR>
			<TD>Name:</TD>
			<TD>
				<html:text property="name" size="100" maxlength="80"/>
				&nbsp;<html:errors property="name"/>
			</TD>
			<tt:propertyEquals name="unitime.coordinates.googlemap" value="true">
				<TD rowspan="4">
					<div id="map_canvas" style="width: 600px; height: 400px; border: 1px solid #9CB0CE;"></div>
				</TD>
			</tt:propertyEquals>
		</TR>

		<TR>
			<TD>Abbreviation:</TD>
			<TD>
				<html:text property="abbreviation" size="10" maxlength="10"/>
				&nbsp;<html:errors property="abbreviation"/>
			</TD>
		</TR>

		<TR>
			<TD>External ID:</TD>
			<TD>
				<html:text property="externalId" size="40" maxlength="40"/>
				&nbsp;<html:errors property="externalId"/>
			</TD>
		</TR>

		<TR>
			<TD>Coordinates:</TD>
			<TD>
				<html:text property="coordX" size="12" maxlength="12" styleId="coordX" onchange="setMarker();"/>,
				<html:text property="coordY" size="12" maxlength="12" styleId="coordY" onchange="setMarker();"/>
				&nbsp;<html:errors property="coordX"/> <html:errors property="coordy"/>
				<% DistanceMetric.Ellipsoid ellipsoid = DistanceMetric.Ellipsoid.valueOf(ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name())); %>
				&nbsp;&nbsp;&nbsp;<i><%=ellipsoid.getEclipsoindName()%></i>
			</TD>
		</TR>

		<tr>
			<td valign="middle" colspan="3">
				<tt:section-title/>
			</td>
		<tr>
		
		<TR>
			<TD align="right" colspan="3">
				<logic:equal name="buildingEditForm" property="op" value="Save">
					<html:submit property="op" value="Save" title="Save (Alt+S)" accesskey="S"/> 
				</logic:equal>
				<logic:equal name="buildingEditForm" property="op" value="Update">
					<html:submit property="op" value="Update" title="Update (Alt+U)" accesskey="U"/> 
					<html:submit property="op" value="Delete" title="Delete (Alt+D)" accesskey="D" onclick="return confirmDelete();"/> 
				</logic:equal>
				<html:submit property="op" value="Back" title="Back (Alt+B)" accesskey="B"/> 
			</TD>
		</TR>
	</TABLE>

</html:form>

<tt:propertyEquals name="unitime.coordinates.googlemap" value="true">
<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false"></script>
<script type="text/javascript" language="javascript">
	var latlng = new google.maps.LatLng(50, -58);
	var myOptions = {
		zoom: 2,
		center: latlng,
		mapTypeId: google.maps.MapTypeId.ROADMAP
	};
	var geocoder = new google.maps.Geocoder();
	var map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);
	var marker = marker = new google.maps.Marker({
		position: latlng, 
		map: map,
		draggable: true,
		visible: false
	});

    var searchBox = createGoogleSeachControl(map);
	
	function createGoogleSeachControl(map) {
		var controlDiv = document.createElement('DIV');
	    controlDiv.index = 1;
		controlDiv.style.marginBottom = '15px';
		var controlUI = document.createElement('DIV');
		controlUI.style.backgroundColor = 'transparent';
		controlUI.style.cursor = 'pointer';
		controlUI.style.textAlign = 'center';
		controlUI.title = "Seach";
		controlDiv.appendChild(controlUI);
		var controltxtbox = document.createElement('input');
		controltxtbox.setAttribute("id", "txt_googleseach");
		controltxtbox.setAttribute("type", "text");
		controltxtbox.setAttribute("value", "");
		controltxtbox.style.height = '22px';
		controltxtbox.style.width = '450px';
		controltxtbox.style.marginRight = '2px';
		controlUI.appendChild(controltxtbox);
		var controlbtn = document.createElement('input');
		controlbtn.setAttribute("id", "btn_googleseach");
		controlbtn.setAttribute("type", "button");
		controlbtn.setAttribute("value", "Geocode");
		controlUI.appendChild(controlbtn);
		google.maps.event.addDomListener(controlbtn, 'click', function() {
			geoceodeAddress(controltxtbox.value);
		});
		controltxtbox.onkeypress = function(e) {
			var key = e.keyCode || e.which;
			if (key == 13) {
				geoceodeAddress(controltxtbox.value);
				return false;
			}
			return true;
		};
		map.controls[google.maps.ControlPosition.BOTTOM_LEFT].push(controlDiv);
		return controltxtbox;
	}
	
	function geoceodeAddress(address) {
		var address = document.getElementById("txt_googleseach").value;
		geocoder.geocode({ 'address': address }, function(results, status) {
			if (status == google.maps.GeocoderStatus.OK) {
				if (results[0]) {
					marker.setPosition(results[0].geometry.location);
					marker.setTitle(results[0].formatted_address);
					marker.setVisible(true);
					if (map.getZoom() <= 10) map.setZoom(16);
					map.panTo(results[0].geometry.location);
				} else {
					marker.setVisible(false);
				}
			} else {
				marker.setVisible(false);
			}
		});
	}
	
	function geoceodeMarker() {
		geocoder.geocode({'location': marker.getPosition()}, function(results, status) {
			if (status == google.maps.GeocoderStatus.OK) {
				if (results[0]) {
					marker.setTitle(results[0].formatted_address);
					if (searchBox != null)
						searchBox.value = results[0].formatted_address;
				} else {
					marker.setTitle(null);
					if (searchBox != null) searchBox.value = "";
				}
			} else {
				marker.setTitle(null);
				if (searchBox != null) searchBox.value = "";
			}
		});
	}
	
	var t = null;	
	
	google.maps.event.addListener(marker, 'position_changed', function() {
		document.getElementById("coordX").value = '' + marker.getPosition().lat().toFixed(6);
		document.getElementById("coordY").value = '' + marker.getPosition().lng().toFixed(6);
		if (t != null) clearTimeout(t);
		t = setTimeout("geoceodeMarker()", 500);
	});
	google.maps.event.addListener(map, 'rightclick', function(event) {
		marker.setPosition(event.latLng);
		marker.setVisible(true);
	});
	function setMarker() {
		var x = document.getElementById("coordX").value;
		var y = document.getElementById("coordY").value;
		if (x && y) {
			var pos = new google.maps.LatLng(x, y);
			marker.setPosition(pos);
			marker.setVisible(true);
			if (map.getZoom() <= 10) map.setZoom(16);
			map.panTo(pos);
		} else {
			marker.setVisible(false);
		}
	}
	setMarker();
</script>
</tt:propertyEquals>
<tt:propertyNotEquals name="unitime.coordinates.googlemap" value="true">
	<script type="text/javascript" language="javascript">
		function setMarker() {}
	</script>
</tt:propertyNotEquals>