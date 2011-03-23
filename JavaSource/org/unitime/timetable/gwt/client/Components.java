/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
 * as indicated by the @authors tag.
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
*/
package org.unitime.timetable.gwt.client;

import org.unitime.timetable.gwt.client.curricula.CourseCurriculaTable;
import org.unitime.timetable.gwt.client.page.UniTimePageLabel;
import org.unitime.timetable.gwt.client.page.UniTimeBack;
import org.unitime.timetable.gwt.client.page.UniTimePageHeader;
import org.unitime.timetable.gwt.client.page.UniTimeMenuBar;
import org.unitime.timetable.gwt.client.page.UniTimeSideBar;
import org.unitime.timetable.gwt.client.page.UniTimeVersion;
import org.unitime.timetable.gwt.client.reservations.ReservationTable;

import com.google.gwt.user.client.ui.RootPanel;

/**
 * Register GWT components here.
 * @author Tomas Muller
 *
 */
public enum Components {
	courseCurricula("UniTimeGWT:CourseCurricula", new ComponentFactory() { public void insert(RootPanel panel) { new CourseCurriculaTable(true, true).insert(panel); } }),
	title("UniTimeGWT:Title", new ComponentFactory() { public void insert(RootPanel panel) { UniTimePageLabel.getInstance().insert(panel); } }),
	sidebar_stack("UniTimeGWT:SideStackMenu", new ComponentFactory() { public void insert(RootPanel panel) { new UniTimeSideBar(true).insert(panel); } }),
	sidebar_tree("UniTimeGWT:SideTreeMenu", new ComponentFactory() { public void insert(RootPanel panel) { new UniTimeSideBar(false).insert(panel); } }),
	menubar_static("UniTimeGWT:TopMenu", new ComponentFactory() { public void insert(RootPanel panel) { new UniTimeMenuBar(false).insert(panel); } }),
	menubar_dynamic("UniTimeGWT:DynamicTopMenu", new ComponentFactory() { public void insert(RootPanel panel) { new UniTimeMenuBar(true).insert(panel); } }),
	header("UniTimeGWT:Header", new ComponentFactory() { public void insert(RootPanel panel) { new UniTimePageHeader().insert(panel); } }),
	version("UniTimeGWT:Version", new ComponentFactory() { public void insert(RootPanel panel) { new UniTimeVersion().insert(panel); } }),
	back("UniTimeGWT:Back", new ComponentFactory() { public void insert(RootPanel panel) { new UniTimeBack().insert(panel); } }),
	offeringReservations("UniTimeGWT:OfferingReservations", new ComponentFactory() { public void insert(RootPanel panel) { new ReservationTable(true, true).insert(panel); } }),
	offeringReservationsReadOnly("UniTimeGWT:OfferingReservationsRO", new ComponentFactory() { public void insert(RootPanel panel) { new ReservationTable(false, true).insert(panel); } });
	
	private String iId;
	private ComponentFactory iFactory;
	
	Components(String id, ComponentFactory factory) { iId = id; iFactory = factory; }
	public String id() { return iId; }
	public void insert(RootPanel panel) { iFactory.insert(panel); }
	
	public interface ComponentFactory {
		void insert(RootPanel panel);
	}
}
