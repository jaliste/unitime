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
package org.unitime.timetable.gwt.client.widgets;

import java.util.ArrayList;
import java.util.List;

import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasAdditionalStyleNames;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasCellAlignment;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasColSpan;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasColumn;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasStyleName;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class UniTimeTableHeader extends HTML implements HasStyleName, HasCellAlignment, HasColSpan, HasAdditionalStyleNames, HasColumn {
	private int iColSpan = 1, iColumn = -1;
	private HorizontalAlignmentConstant iAlign;
	private List<Operation> iOperations = new ArrayList<Operation>();
	private List<String> iStyleNames = new ArrayList<String>();
	
	public UniTimeTableHeader(String title, int colSpan, HorizontalAlignmentConstant align) {
		super(title, false);
		iColSpan = colSpan;
		iAlign = align;
		
		addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final PopupPanel popup = new PopupPanel(true);
				popup.addStyleName("unitime-Menu");
				if (!setMenu(popup)) return;
				popup.showRelativeTo((Widget)event.getSource());
			}
		});
	}
	
	public boolean setMenu(final PopupPanel popup) {
		List<Operation> operations = getOperations();
		if (operations.isEmpty()) return false;
		boolean first = true;
		MenuBar menu = new MenuBar(true);
		for (final Operation op: operations) {
			if (!op.isApplicable()) continue;
			if (op.hasSeparator() && !first)
				menu.addSeparator();
			first = false;
			MenuItem item = new MenuItem(op.getName(), true, new Command() {
				@Override
				public void execute() {
					popup.hide();
					op.execute();
				}
			});
			item.getElement().getStyle().setCursor(Cursor.POINTER);
			menu.addItem(item);
		}
		if (first) return false;
		menu.setVisible(true);
		menu.setFocusOnHoverEnabled(true);
		popup.add(menu);
		return true;
	}
	
	public UniTimeTableHeader(String title) {
		this(title, 1, HasHorizontalAlignment.ALIGN_LEFT);
	}
	
	public UniTimeTableHeader(String title, int colSpan) {
		this(title, colSpan, HasHorizontalAlignment.ALIGN_LEFT);
	}

	
	public UniTimeTableHeader(String title, HorizontalAlignmentConstant align) {
		this(title, 1, align);
	}

	public int getColSpan() {
		return iColSpan;
	}
	
	public HorizontalAlignmentConstant getCellAlignment() {
		return iAlign;
	}
	
	public String getStyleName() {
		return (iOperations.isEmpty() ? "unitime-TableHeader" : "unitime-ClickableTableHeader");
	}
	

	public void addOperation(Operation operation) {
		iOperations.add(operation);
	}
	
	public List<Operation> getOperations() {
		return iOperations;
	}
	
	public static interface Operation extends Command {
		public String getName();
		public boolean isApplicable();
		public boolean hasSeparator();
	}
	
	public String getHint() {
		return null;
	}
	
	public List<String> getAdditionalStyleNames() {
		return iStyleNames;
	}
	
	public void addAdditionalStyleName(String styleName) {
		iStyleNames.add(styleName);
	}

	@Override
	public int getColumn() {
		return iColumn;
	}

	@Override
	public void setColumn(int column) {
		iColumn = column;
	}
}
