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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;

/**
 * @author Tomas Muller
 */
public class UniTimeTable<T> extends FlexTable {
	
	private List<MouseOverListener<T>> iMouseOverListeners = new ArrayList<MouseOverListener<T>>();
	private List<MouseOutListener<T>> iMouseOutListeners = new ArrayList<MouseOutListener<T>>();
	private List<MouseClickListener<T>> iMouseClickListeners = new ArrayList<MouseClickListener<T>>();
	private List<DataChangedListener<T>> iDataChangedListeners = new ArrayList<DataChangedListener<T>>();
	
	private PopupPanel iHintPanel = null;
	private HintProvider<T> iHintProvider = null;
	
	private int iLastHoverRow = -1;
	private String iLastHoverBackgroundColor = null;
	private boolean iAllowSelection = false;
	
	public UniTimeTable() {
		setCellPadding(2);
		setCellSpacing(0);
		sinkEvents(Event.ONMOUSEOVER);
		sinkEvents(Event.ONMOUSEOUT);
		sinkEvents(Event.ONCLICK);
		sinkEvents(Event.ONKEYDOWN);
		setStylePrimaryName("unitime-MainTable");
		iHintPanel = new PopupPanel();
		iHintPanel.setStyleName("unitime-PopupHint");
	}
	
	public void setAllowSelection(boolean allow) { iAllowSelection = allow; }
	public boolean isAllowSelection() { return iAllowSelection; }

	public void clearTable(int headerRows) {
		for (int row = getRowCount() - 1; row >= headerRows; row--)
			removeRow(row);
	}
	
	public void clearTable() {
		clearTable(0);
	}
	
	public void addRow(T data, Widget... widgets) {
		List<Widget> list = new ArrayList<Widget>();
		for (Widget widget: widgets)
			list.add(widget);
		addRow(data, list);
	}
	
	public int addRow(T data, List<? extends Widget> widgets) {
		int row = getRowCount();
		setRow(row, data, widgets);
		return row;
	}
	
	public void setRow(int row, T data, List<? extends Widget> widgets) {
		SmartTableRow<T> oldRow = getSmartRow(row);
		if (oldRow != null && oldRow.getData() != null) {
			DataChangedEvent<T> event = new DataChangedEvent<T>(oldRow.getData(), row);
			for (DataChangedListener<T> listener: iDataChangedListeners)
				listener.onDataRemoved(event);
		}
		SmartTableRow smartRow = new SmartTableRow(data);
		int col = 0;
		for (Widget widget: widgets) {
			SmartTableCell cell = new SmartTableCell(smartRow, widget);
			if (widget instanceof HasColSpan)
				getFlexCellFormatter().setColSpan(row, col, ((HasColSpan)widget).getColSpan());
			if (widget instanceof HasStyleName && ((HasStyleName)widget).getStyleName() != null)
				getFlexCellFormatter().setStyleName(row, col, ((HasStyleName)widget).getStyleName());
			if (widget instanceof HasAdditionalStyleNames) {
				List<String> styleNames = ((HasAdditionalStyleNames)widget).getAdditionalStyleNames();
				if (styleNames != null)
					for (String styleName: styleNames)
						getFlexCellFormatter().addStyleName(row, col, styleName);
			}
			if (widget instanceof  HasCellAlignment)
				getFlexCellFormatter().setHorizontalAlignment(row, col, ((HasCellAlignment)widget).getCellAlignment());
			if (widget instanceof  HasVerticalCellAlignment)
				getFlexCellFormatter().setVerticalAlignment(row, col, ((HasVerticalCellAlignment)widget).getVerticalCellAlignment());
			setWidget(row, col, cell);
			if (row > 0 && col < getCellCount(0))
				getCellFormatter().setVisible(row, col, getCellFormatter().isVisible(0, col));
			col++;
		}
		if (data != null) {
			DataChangedEvent<T> event = new DataChangedEvent<T>(data, row);
			for (DataChangedListener<T> listener: iDataChangedListeners)
				listener.onDataInserted(event);
		}
	}
	
	public boolean isColumnVisible(int col) {
		return getCellFormatter().isVisible(0, col);
	}
	
	public void setColumnVisible(int col, boolean visible) {
		for (int r = 0; r < getRowCount(); r++)
			getCellFormatter().setVisible(r, col, visible);
	}
	
	public static class SmartTableRow<T> {
		private List<SmartTableCell> iCells = new ArrayList<SmartTableCell>();
		private T iData = null;
		
		public SmartTableRow(T data) {
			iData = data;
		}
		
		public T getData() {
			return iData;
		}
		
		public boolean hasData() {
			return iData != null;
		}
		
		public List<SmartTableCell> getCells() { return iCells; }
		
		public Comparator<SmartTableRow<T>> getComparator(final Comparator<T> cmp) {
			return new Comparator<SmartTableRow<T>>() {
				public int compare(SmartTableRow<T> a, SmartTableRow<T> b) {
					return cmp.compare(a.getData(), b.getData());
				}
			};
		}
	}
	
	public static class SmartTableCell extends Composite {
		SmartTableRow iRow;
		
		public SmartTableCell(SmartTableRow row, Widget widget) {
			iRow = row;
			row.getCells().add(this);
			initWidget(widget);
		}
		
		public SmartTableRow getRow() { return iRow; }
		
		public boolean focus() {
			if (getWidget() instanceof HasFocus) {
				return ((HasFocus)getWidget()).focus();
			} else if (getWidget() instanceof Focusable) {
				((Focusable)getWidget()).setFocus(true);
				if (getWidget() instanceof TextBoxBase)
					((TextBoxBase)getWidget()).selectAll();
				return true;
			}
			return false;
		}
		
		public Widget getInnerWidget() {
			return getWidget();
		}
	}
	
	public Widget getWidget(int row, int col) {
		Widget w = super.getWidget(row, col);
		if (w == null) return w;
		if (w instanceof SmartTableCell)
			return ((SmartTableCell)w).getInnerWidget();
		return w;
	}
	
	public Widget replaceWidget(int row, int col, Widget widget) {
		Widget w = super.getWidget(row, col);
		if (w == null)
			super.setWidget(row, col, widget);
		else if (w instanceof SmartTableCell)
			super.setWidget(row, col, new SmartTableCell(((SmartTableCell)w).getRow(), widget));
		else
			super.setWidget(row, col, widget);
		return w;
	}

	private boolean focus(int row, int col) {
		if (!getRowFormatter().isVisible(row) || col >= getCellCount(row)) return false;
		Widget w = super.getWidget(row, col);
		if (w == null || !w.isVisible()) return false;
		if (w instanceof SmartTableCell) {
			return ((SmartTableCell)w).focus();
		} else if (w instanceof HasFocus) {
			return ((HasFocus)w).focus();
		} else if (w instanceof Focusable) {
			((Focusable)w).setFocus(true);
			if (w instanceof TextBoxBase)
				((TextBoxBase)w).selectAll();
			return true;
		}
		return false;
	}
	
	public T getData(int row) {
		SmartTableRow<T> r = getSmartRow(row);
		return (r == null ? null : r.getData());
	}
	
	public List<T> getData() {
		List<T> ret = new ArrayList<T>();
		for (int row = 0; row < getRowCount(); row++) {
			T data = getData(row);
			if (data != null) ret.add(data);
		}
		return ret;
	}

	
	public SmartTableRow<T> getSmartRow(int row) {
		if (row < 0 || row >= getRowCount()) return null;
		for (int col = 0; col < getCellCount(row); col++) {
			Widget w = super.getWidget(row, col);
			if (w != null && w instanceof SmartTableCell)
				return ((SmartTableCell)w).getRow();
		}
		return null;
	}
	
	private void swapRows(int r0, int r1) {
		if (r0 == r1) return;
		if (r0 > r1) {
			swapRows(r1, r0);
		} else { // r0 < r1
			Element body = getBodyElement();
			Element a = DOM.getChild(body, r0);
			Element b = DOM.getChild(body, r1);
			DOM.removeChild(body, a);
			DOM.removeChild(body, b);
			DOM.insertChild(body, b, r0);
			DOM.insertChild(body, a, r1);
		}
	}
	
	public void sort(final Comparator<T> rowComparator) {
		Element body = getBodyElement();
		ArrayList<Object[]> rows = new ArrayList<Object[]>();
		for (int row = 0; row < getRowCount(); row++) {
			SmartTableRow<T> r = getSmartRow(row);
			if (r != null && r.hasData()) {
				rows.add(new Object[] {r, getRowFormatter().getElement(row)});
			}
		}
		Collections.sort(rows,new Comparator<Object[]>() {
			public int compare(Object[] a, Object[] b) {
				return rowComparator.compare(((SmartTableRow<T>)a[0]).getData(), ((SmartTableRow<T>)b[0]).getData());
			}
		});
		int idx = 0;
		List<DataChangedEvent<T>> changeEvents = new ArrayList<DataChangedEvent<T>>();
		for (int row = 0; row < getRowCount(); row++) {
			SmartTableRow<T> a = getSmartRow(row);
			if (a != null && a.hasData()) {
				Object[] o = rows.get(idx++);
				int otherRow = DOM.getChildIndex(body, (Element)o[1]);
				swapRows(row, otherRow);
				changeEvents.add(new DataChangedEvent<T>(((SmartTableRow<T>)o[0]).getData(), row));
			}
		}
		for (DataChangedListener<T> listener: iDataChangedListeners) 
			listener.onDataSorted(changeEvents);
	}
	
	public void sortByRow(final Comparator<Integer> rowComparator) {
		Element body = getBodyElement();
		ArrayList<Object[]> rows = new ArrayList<Object[]>();
		for (int row = 0; row < getRowCount(); row++) {
			SmartTableRow<T> r = getSmartRow(row);
			if (r != null && r.hasData()) {
				rows.add(new Object[] {r, getRowFormatter().getElement(row), row});
			}
		}
		Collections.sort(rows,new Comparator<Object[]>() {
			public int compare(Object[] a, Object[] b) {
				return rowComparator.compare((Integer)a[2], (Integer)b[2]);
			}
		});
		int idx = 0;
		List<DataChangedEvent<T>> changeEvents = new ArrayList<DataChangedEvent<T>>();
		for (int row = 0; row < getRowCount(); row++) {
			SmartTableRow<T> a = getSmartRow(row);
			if (a != null && a.hasData()) {
				Object[] o = rows.get(idx++);
				int otherRow = DOM.getChildIndex(body, (Element)o[1]);
				swapRows(row, otherRow);
				changeEvents.add(new DataChangedEvent<T>(((SmartTableRow<T>)o[0]).getData(), row));
			}
		}
		for (DataChangedListener<T> listener: iDataChangedListeners) 
			listener.onDataSorted(changeEvents);
	}
	
	public boolean canSwapRows(T a, T b) {
		return true;
	}
	
	public void onBrowserEvent(final Event event) {
		Element td = getEventTargetCell(event);
		if (td==null) return;
	    final Element tr = DOM.getParent(td);
		int col = DOM.getChildIndex(tr, td);
	    Element body = DOM.getParent(tr);
	    int row = DOM.getChildIndex(body, tr);
	    
	    Widget widget = getWidget(row, col);
	    SmartTableRow<T> r = getSmartRow(row);
	    boolean hasData = (r != null && r.getData() != null);
	    
	    TableEvent<T> tableEvent = new TableEvent<T>(event, row, col, tr, td, hasData ? r.getData() : null);

	    Widget hint = null;
		if (widget instanceof HasHint) {
			String html = ((HasHint)widget).getHint();
			if (html != null && !html.isEmpty())
				hint = new HTML(html, false);
		}
		if (hint == null && iHintProvider != null)
			hint = iHintProvider.getHint(tableEvent);

		String style = getRowFormatter().getStyleName(row);

		switch (DOM.eventGetType(event)) {
		case Event.ONMOUSEOVER:
			if (hasData) {
				if (!iMouseClickListeners.isEmpty())
					getRowFormatter().getElement(row).getStyle().setCursor(Cursor.POINTER);
				if (isAllowSelection()) {
					if ("unitime-TableRowSelected".equals(style))
						getRowFormatter().setStyleName(row, "unitime-TableRowSelectedHover");	
					else
						getRowFormatter().setStyleName(row, "unitime-TableRowHover");
				} else {
					getRowFormatter().addStyleName(row, "unitime-TableRowHover");
				}
				iLastHoverRow = row;
				iLastHoverBackgroundColor = getRowFormatter().getElement(row).getStyle().getBackgroundColor();
				if (iLastHoverBackgroundColor != null && !iLastHoverBackgroundColor.isEmpty())
					getRowFormatter().getElement(row).getStyle().clearBackgroundColor();
			}
			if (!iHintPanel.isShowing() && hint != null) {
				iHintPanel.setWidget(hint);
				iHintPanel.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
					@Override
					public void setPosition(int offsetWidth, int offsetHeight) {
						boolean top = (tr.getAbsoluteBottom() - Window.getScrollTop() + 15 + offsetHeight > Window.getClientHeight());
						iHintPanel.setPopupPosition(
								Math.max(Math.min(event.getClientX(), tr.getAbsoluteRight() - offsetWidth - 15), tr.getAbsoluteLeft() + 15),
								top ? tr.getAbsoluteTop() - offsetHeight - 15 : tr.getAbsoluteBottom() + 15);
					}
				});
			}
			for (MouseOverListener<T> listener: iMouseOverListeners)
				listener.onMouseOver(tableEvent);
			break;
		case Event.ONMOUSEOUT:
			if (hasData) {
				if (!iMouseClickListeners.isEmpty())
					getRowFormatter().getElement(row).getStyle().clearCursor();
				if (isAllowSelection()) {
					if ("unitime-TableRowHover".equals(style))
						getRowFormatter().setStyleName(row, null);	
					else if ("unitime-TableRowSelectedHover".equals(style))
						getRowFormatter().setStyleName(row, "unitime-TableRowSelected");
				} else {
					getRowFormatter().removeStyleName(row, "unitime-TableRowHover");
				}
				if (iLastHoverBackgroundColor != null && !iLastHoverBackgroundColor.isEmpty())
					getRowFormatter().getElement(row).getStyle().setBackgroundColor(iLastHoverBackgroundColor);
				iLastHoverRow = -1;
				iLastHoverBackgroundColor = null;
			}
			if (iHintPanel.isShowing()) iHintPanel.hide();
			for (MouseOutListener<T> listener: iMouseOutListeners)
				listener.onMouseOut(tableEvent);
			break;
		case Event.ONMOUSEMOVE:
			if (iHintPanel.isShowing()) {
				boolean top = (tr.getAbsoluteBottom() - Window.getScrollTop() + 15 + iHintPanel.getOffsetHeight() > Window.getClientHeight());
				iHintPanel.setPopupPosition(
						Math.max(Math.min(event.getClientX(), tr.getAbsoluteRight() - iHintPanel.getOffsetWidth() - 15), tr.getAbsoluteLeft() + 15),
						top ? tr.getAbsoluteTop() - iHintPanel.getOffsetHeight() - 15 : tr.getAbsoluteBottom() + 15);
			}
			break;
		case Event.ONCLICK:
			if (isAllowSelection()) {
				Element element = DOM.eventGetTarget(event);
				while (DOM.getElementProperty(element, "tagName").equalsIgnoreCase("div"))
					element = DOM.getParent(element);
				if (DOM.getElementProperty(element, "tagName").equalsIgnoreCase("td")) {
					boolean hover = ("unitime-TableRowHover".equals(style) || "unitime-TableRowSelectedHover".equals(style));
					boolean selected = !("unitime-TableRowSelected".equals(style) || "unitime-TableRowSelectedHover".equals(style));
					getRowFormatter().setStyleName(row, "unitime-TableRow" + (selected ? "Selected" : "") + (hover ? "Hover" : ""));
				}
			}
			for (MouseClickListener<T> listener: iMouseClickListeners)
				listener.onMouseClick(tableEvent);
			break;
		case Event.ONKEYDOWN:
			if (event.getKeyCode() == KeyCodes.KEY_RIGHT && (event.getAltKey() || event.getMetaKey())) {
				do {
					col++;
					if (col >= getCellCount(row)) break;
				} while (!focus(row, col));
				event.stopPropagation();
		    	event.preventDefault();
			}
			if (event.getKeyCode() == KeyCodes.KEY_LEFT && (event.getAltKey() || event.getMetaKey())) {
				do {
					col--;
					if (col < 0) break;
				} while (!focus(row, col));
				event.stopPropagation();
		    	event.preventDefault();
			}
			if (event.getKeyCode() == KeyCodes.KEY_UP && (event.getAltKey() || event.getMetaKey())) {
				do {
					row--;
					if (row < 0) break;
				} while (!focus(row, col));
				event.stopPropagation();
		    	event.preventDefault();
			}
			if (event.getKeyCode() == KeyCodes.KEY_DOWN && (event.getAltKey() || event.getMetaKey())) {
				do {
					row++;
					if (row >= getRowCount()) break;
				} while (!focus(row, col));
				event.stopPropagation();
		    	event.preventDefault();
			}
			if (hasData && event.getKeyCode() == KeyCodes.KEY_UP && event.getCtrlKey()) {
				SmartTableRow<T> up = getSmartRow(row - 1);
				if (up != null && up.getData() != null && canSwapRows(r.getData(), up.getData())) {
					getRowFormatter().removeStyleName(row, "unitime-TableRowHover");
					getRowFormatter().removeStyleName(row - 1, "unitime-TableRowHover");
					swapRows(row - 1, row);
					focus(row - 1, col);
					if (!iDataChangedListeners.isEmpty()) {
						List<DataChangedEvent<T>> e = new ArrayList<DataChangedEvent<T>>();
						e.add(new DataChangedEvent<T>(up.getData(), row));
						e.add(new DataChangedEvent<T>(r.getData(), row - 1));
						for (DataChangedListener<T> listener: iDataChangedListeners) {
							listener.onDataMoved(e);
						}
					}
				}
				event.stopPropagation();
		    	event.preventDefault();
			}
			if (hasData && event.getKeyCode() == KeyCodes.KEY_DOWN && event.getCtrlKey()) {
				SmartTableRow<T> dn = getSmartRow(row + 1);
				if (dn != null && dn.getData() != null && canSwapRows(r.getData(), dn.getData())) {
					getRowFormatter().removeStyleName(row, "unitime-TableRowHover");
					getRowFormatter().removeStyleName(row + 1, "unitime-TableRowHover");
					swapRows(row + 1, row);
					focus(row + 1, col);
					if (!iDataChangedListeners.isEmpty()) {
						List<DataChangedEvent<T>> e = new ArrayList<DataChangedEvent<T>>();
						e.add(new DataChangedEvent<T>(dn.getData(), row));
						e.add(new DataChangedEvent<T>(r.getData(), row + 1));
						for (DataChangedListener<T> listener: iDataChangedListeners) {
							listener.onDataMoved(e);
						}
					}
				}
				event.stopPropagation();
		    	event.preventDefault();
			}
			break;
	    }
	}
	
	public void clearHover() {
		if (iLastHoverRow >= 0 && iLastHoverRow < getRowCount()) {
			if (isAllowSelection()) {
				String style = getRowFormatter().getStyleName(iLastHoverRow);
				boolean selected = ("unitime-TableRowSelected".equals(style) || "unitime-TableRowSelectedHover".equals(style));
				getRowFormatter().setStyleName(iLastHoverRow, "unitime-TableRow" + (selected ? "Selected" : ""));
			} else {
				getRowFormatter().removeStyleName(iLastHoverRow, "unitime-TableRowHover");
			}
			if (iLastHoverBackgroundColor != null && !iLastHoverBackgroundColor.isEmpty())
				getRowFormatter().getElement(iLastHoverRow).getStyle().setBackgroundColor(iLastHoverBackgroundColor);
		}
		iLastHoverRow = -1;
		iLastHoverBackgroundColor = null;
	}
	
	public boolean isSelected(int row) {
		if (isAllowSelection()) {
			String style = getRowFormatter().getStyleName(row);
			return "unitime-TableRowSelected".equals(style) || "unitime-TableRowSelectedHover".equals(style);
		} else {
			return false;
		}
	}
	
	public void setSelected(int row, boolean selected) {
		if (isAllowSelection()) {
			String style = getRowFormatter().getStyleName(row);
			boolean hover = ("unitime-TableRowHover".equals(style) || "unitime-TableRowSelectedHover".equals(style));
			getRowFormatter().setStyleName(row, "unitime-TableRow" + (selected ? "Selected" : "") + (hover ? "Hover" : ""));
		}
	}
	
	public int getSelectedCount() {
		int selected = 0;
		for (int row = 0; row < getRowCount(); row ++)
			if (isSelected(row)) selected ++;
		return selected;
	}
	
	public int getSelectedRow() {
		for (int row = 0; row < getRowCount(); row ++)
			if (isSelected(row)) return row;
		return -1;
	}
	
	public static class TableEvent<T> {
		private Event iSourceEvent;
		private int iRow;
		private int iCol;
		private Element iTD;
		private Element iTR;
		private T iData;
		
		public TableEvent(Event sourceEvent, int row, int col, Element tr, Element td, T data) {
			iRow = row;
			iCol = col;
			iTR = tr;
			iTD = td;
			iData = data;
			iSourceEvent = sourceEvent;
		}
		
		public int getRow() { return iRow; }
		public int getCol() { return iCol; }
		public T getData() { return iData; }
		public Element getRowElement() { return iTR; }
		public Element getCellElement() { return iTD; }
		public Event getSourceEvent() { return iSourceEvent; }
	}
	
	public static interface MouseOverListener<T> {
		public void onMouseOver(TableEvent<T> event);
	}
	
	public void addMouseOverListener(MouseOverListener<T> mouseOverListener) {
		iMouseOverListeners.add(mouseOverListener);
	}

	public static interface MouseOutListener<T> {
		public void onMouseOut(TableEvent<T> event);
	}
	
	public void addMouseOutListener(MouseOutListener<T> mouseOutListener) {
		iMouseOutListeners.add(mouseOutListener);
	}

	public static interface MouseClickListener<T> {
		public void onMouseClick(TableEvent<T> event);
	}
	
	public void addMouseClickListener(MouseClickListener<T> mouseClickListener) {
		iMouseClickListeners.add(mouseClickListener);
	}

	public static class DataChangedEvent<T> {
		private T iData;
		private int iRow;
		public DataChangedEvent(T data, int row) {
			iData = data; iRow = row;
		}
		public T getData() { return iData; }
		public int getRow() { return iRow; }
	}
	
	public interface DataChangedListener<T> {
		public void onDataInserted(DataChangedEvent<T> event);
		public void onDataRemoved(DataChangedEvent<T> event);
		public void onDataMoved(List<DataChangedEvent<T>> events);
		public void onDataSorted(List<DataChangedEvent<T>> events);
	}
	
	public void addDataChangedListener(DataChangedListener<T> listener) {
		iDataChangedListeners.add(listener);
	}
	
	public static interface HasFocus {
		public boolean focus();
	}

	public static interface HasHint {
		public String getHint();
	}
	
	public static interface HasColSpan {
		public int getColSpan();
	}
	
	public static interface HasCellAlignment {
		public HorizontalAlignmentConstant getCellAlignment();
	}
	
	public static interface HasVerticalCellAlignment {
		public VerticalAlignmentConstant getVerticalCellAlignment();
	}
	
	public static interface HasStyleName {
		public String getStyleName();
	}
	
	public static interface HasAdditionalStyleNames {
		public List<String> getAdditionalStyleNames();
	}
	
	public static interface HasDataUpdate {
		public void update();
	}
	
	public static interface HintProvider<T> {
		Widget getHint(TableEvent<T> event);
	}
	
	public void setHintProvider(HintProvider<T> hintProvider) {
		iHintProvider = hintProvider;
	}
	
}
