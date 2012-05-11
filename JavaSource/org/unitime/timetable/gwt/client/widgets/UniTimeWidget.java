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

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class UniTimeWidget<T extends Widget> extends Composite {
	private T iWidget;
	private HTML iReadOnly = null, iPrint = null;
	private Label iHint;
	private VerticalPanel iPanel;
	
	public UniTimeWidget(T widget, String hint) {
		iPanel = new VerticalPanel();
		
		iWidget = widget;
		iPanel.add(iWidget);
		
		iHint = new Label(hint == null ? "" : hint, false);
		iHint.setStyleName("unitime-NotClickableHint");
		if (hint == null || hint.isEmpty())
			iHint.setVisible(false);
		iPanel.add(iHint);
		
		initWidget(iPanel);
	}
	
	public void setText(String html) {
		if (iReadOnly == null) {
			iReadOnly = new HTML(html, false);
			iReadOnly.setStyleName("unitime-LabelInsteadEdit");
			iReadOnly.setVisible(!getWidget().isVisible());
			iReadOnly.addStyleName("unitime-NoPrint");
			iPanel.insert(iReadOnly, 1);
		} else {
			iReadOnly.setHTML(html);
		}
	}

	public void setPrintText(String html) {
		if (iPrint == null) {
			iPrint = new HTML(html, true);
			iPrint.setStyleName("unitime-LabelInsteadEdit");
			iPrint.addStyleName("unitime-Print");
			getWidget().addStyleName("unitime-NoPrint");
			iPanel.insert(iPrint, 1);
		} else {
			iPrint.setHTML(html);
		}
	}

	public boolean showReadOnly() {
		return iWidget instanceof ListBox;
	}
	
	public UniTimeWidget(T widget) {
		this(widget, null);
	}
	
	public T getWidget() {
		return iWidget;
	}
	
	public void clearHint() {
		iHint.setText("");
		iHint.setVisible(false);
	}
	
	public void setErrorHint(String error) {
		if (error == null || error.isEmpty()) {
			clearHint();
		} else {
			iHint.setStyleName("unitime-ErrorHint");
			iHint.setText(error);
			iHint.setVisible(true);
		}
	}

	public void setHint(String hint) {
		if (hint == null || hint.isEmpty()) {
			clearHint();
		} else {
			iHint.setStyleName("unitime-NotClickableHint");
			iHint.setText(hint);
			iHint.setVisible(true);
		}
	}
	
	@Deprecated
	public void setVisible(boolean visible) {
		getWidget().setVisible(visible);
		if (iReadOnly != null)
			iReadOnly.setVisible(!visible);
	}
	
	@Deprecated
	public void clear() {
		if (getWidget() instanceof ListBox)
			((ListBox)getWidget()).clear();
		if (getWidget() instanceof Panel)
			((Panel)getWidget()).clear();
	}
	
	public void setReadOnly(boolean readOnly) {
		if (getWidget() instanceof UniTimeTextBox) {
			((UniTimeTextBox)getWidget()).setReadOnly(readOnly);
		} else {
			getWidget().setVisible(!readOnly);
			if (iReadOnly != null)
				iReadOnly.setVisible(readOnly);
		}
	}
	
	public boolean isReadOnly() {
		if (getWidget() instanceof UniTimeTextBox) {
			return ((UniTimeTextBox)getWidget()).isReadOnly();
		} else {
			return getWidget().isVisible();
		}
	}
	
	protected VerticalPanel getPanel() { return  iPanel; }
}
