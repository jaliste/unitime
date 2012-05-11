/*
 * UniTime 3.4 (University Timetabling Application)
 * Copyright (C) 2012, UniTime LLC, and individual contributors
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
package org.unitime.timetable.gwt.client.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.unitime.timetable.gwt.client.events.SessionDatesSelector.RequestSessionDetails;
import org.unitime.timetable.gwt.client.events.SessionDatesSelector.SessionMonth;
import org.unitime.timetable.gwt.client.widgets.UniTimeWidget;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.resources.GwtConstants;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider.AcademicSessionChangeEvent;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider.AcademicSessionChangeHandler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.CalendarUtil;

public class SingleDateSelector extends Composite implements HasValue<Date> {
	private static final GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	private static final GwtConstants CONSTANTS = GWT.create(GwtConstants.class);
	private static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	
	private PopupPanel iPopup;
	private SingleMonth iMonth;
	private DateTimeFormat iFormat = DateTimeFormat.getFormat(CONSTANTS.eventDateFormat());
	private AcademicSessionProvider iAcademicSession;
	
	UniTimeWidget<TextBox> iPicker;
	
	public SingleDateSelector() {
		this(null);
	}
	
	public SingleDateSelector(AcademicSessionProvider session) {
		iAcademicSession = session;

		iPicker = new UniTimeWidget<TextBox>(new TextBox());
		iPicker.setHint(iFormat.getPattern().toUpperCase());
		iPicker.getWidget().setStyleName("gwt-SuggestBox");
		iPicker.getWidget().addStyleName("unitime-DateSelectionBox");
		
		
		iMonth = new SingleMonth(new Date());
		AbsolutePanel panel = new AbsolutePanel();
		panel.setStyleName("unitime-DateSelector");
		panel.add(iMonth);
		
		iPopup = new PopupPanel(true, false);
		iPopup.setPreviewingAllNativeEvents(true);
		iPopup.setStyleName("unitime-DateSelectionBoxPopup");
		iPopup.setWidget(panel);
		
		iPicker.getWidget().addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				iPopup.showRelativeTo(iPicker.getWidget());
			}
		});
		
		iPicker.getWidget().addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (iPopup.isShowing()) iPopup.hide();
			}
		});
		
		iPicker.getWidget().addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (iPopup.isShowing()) {
					switch (event.getNativeEvent().getKeyCode()) {
					case KeyCodes.KEY_UP:
						iMonth.addDays(-7);
						event.preventDefault();
						event.stopPropagation();
						break;
					case KeyCodes.KEY_DOWN:
						iMonth.addDays(+7);
						event.preventDefault();
						event.stopPropagation();
						break;
					case KeyCodes.KEY_RIGHT:
						if (iPicker.getWidget().getCursorPos() == iPicker.getWidget().getText().length()) {
							iMonth.addDays(+1);
							event.preventDefault();
							event.stopPropagation();
						}
						break;
					case KeyCodes.KEY_LEFT:
						if (iPicker.getWidget().getCursorPos() == 0) {
							iMonth.addDays(-1);
							event.preventDefault();
							event.stopPropagation();
						}
						break;
					case KeyCodes.KEY_ESCAPE:
						event.preventDefault();
						event.stopPropagation();
						iPopup.hide();
						break;
					case KeyCodes.KEY_ENTER:
						if (iMonth.getValue() != null)
							iPicker.getWidget().setText(iFormat.format(iMonth.getValue()));
						event.preventDefault();
						event.stopPropagation();
						iPopup.hide();
						break;
					}
				} else {
					if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_DOWN && (event.getNativeEvent().getAltKey() || iPicker.getWidget().getCursorPos() == iPicker.getWidget().getText().length())) {
						try {
							iMonth.setDate(iFormat.parse(iPicker.getWidget().getText()));
						} catch (Exception e) {}
						iPopup.showRelativeTo(iPicker.getWidget());
						event.preventDefault();
						event.stopPropagation();
					}
				}
			}
		});
		
		iMonth.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null) {
					iPicker.getWidget().setText(iFormat.format(iMonth.getValue()));
					if (iPopup.isShowing()) iPopup.hide();
				}
			}
		});
		
		iPicker.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				try {
					iMonth.setDate(iFormat.parse(iPicker.getWidget().getText()));
				} catch (Exception e) {
					iMonth.setDate(null);
				}
			}
		});
		
		initWidget(iPicker);
		
		if (iAcademicSession != null) {
			iAcademicSession.addAcademicSessionChangeHandler(new AcademicSessionChangeHandler() {
				@Override
				public void onAcademicSessionChange(AcademicSessionChangeEvent event) {
					if (event.isChanged()) init(event.getNewAcademicSessionId());
				}
			});
			
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					init(iAcademicSession.getAcademicSessionId());
				}
			});
		}
	}
	
	public void init(Long sessionId) {
		if (sessionId == null) {
			iPicker.setHint(MESSAGES.hintNoSession());
		} else {
			iPicker.setHint(MESSAGES.waitLoadingData(iAcademicSession.getAcademicSessionName()));
			RPC.execute(new RequestSessionDetails(sessionId), new AsyncCallback<GwtRpcResponseList<SessionMonth>>() {

				@Override
				public void onFailure(Throwable caught) {
					iPicker.setErrorHint(caught.getMessage());
				}

				@Override
				public void onSuccess(GwtRpcResponseList<SessionMonth> result) {
					iPicker.setHint(iFormat.getPattern().toUpperCase());
					iMonth.setMonths(result);
				}
			});
		}
	}
	
	public static class P extends AbsolutePanel implements HasMouseDownHandlers {
		private String iCaption;
		
		private P(String caption, String... styles) {
			iCaption = caption;
			if (caption != null)
				getElement().setInnerHTML(caption);
			for (String style: styles)
				if (style != null && !style.isEmpty())
					addStyleName(style);
			sinkEvents(Event.ONMOUSEDOWN);
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			switch (DOM.eventGetType(event)) {
		    case Event.ONMOUSEDOWN:
		    	MouseDownEvent.fireNativeEvent(event, this);
		    	event.stopPropagation();
		    	event.preventDefault();
		    	break;
			}
		}
		
		@Override
		public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
			return addHandler(handler, MouseDownEvent.getType());
		}

		public String getCaption() { return iCaption; }
	}
	
	public static class D extends AbsolutePanel implements HasMouseDownHandlers {
		private int iNumber;
		
		private D(int number, String... styles) {
			iNumber = number;
			getElement().setInnerHTML(String.valueOf(number));
			for (String style: styles)
				if (style != null && !style.isEmpty())
					addStyleName(style);
			sinkEvents(Event.ONMOUSEDOWN);
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			switch (DOM.eventGetType(event)) {
		    case Event.ONMOUSEDOWN:
		    	MouseDownEvent.fireNativeEvent(event, this);
		    	event.stopPropagation();
		    	event.preventDefault();
		    	break;
			}
		}
		
		public int getNumber() { return iNumber; }
		
		public String toString() {
			return String.valueOf(1 + getNumber());
		}

		@Override
		public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
			return addHandler(handler, MouseDownEvent.getType());
		}
	}
	
	static int startingDayOfWeek() {
		return (6 + CalendarUtil.getStartingDayOfWeek()) % 7;
	}
	
	static Date toDate(int year, int month, int day) {
		return DateTimeFormat.getFormat("yyyy/MM/dd").parse(year + "/" + month + "/" + day);
	}
	
	@SuppressWarnings("deprecation")
	static int firstDayOfWeek(int year, int month) {
		return (6 + new Date(year - 1900, month - 1, 1).getDay()) % 7;
	}
	
	@SuppressWarnings("deprecation")
	static int daysInMonth(int year, int month) {
		return new Date(year + (month == 12 ? 1 : 0) - 1900, (month == 12 ? 1 : month + 1) - 1, 0).getDate();
	}
	
	@SuppressWarnings("deprecation")
	static int weekNumber(int year, int month) {
		Date d = new Date(year - 1900, month - 1, 1);
		while (d.getDay() != CalendarUtil.getStartingDayOfWeek()) d.setDate(d.getDate() - 1);
		int y = d.getYear();
		int week = 0;
		while (d.getYear() == y) { d.setDate(d.getDate() - 7); week += 1; }
		return week;
	}
	
	@SuppressWarnings("deprecation")
	static int dayOfYear(int year, int month, int day) {
		Date d = new Date(year - 1900, month - 1, day);
		int doy = 0, y = d.getYear();
		while (d.getYear() == y) { d.setDate(d.getDate() - 1); doy ++; }
		return doy;
	}
	
	@SuppressWarnings("deprecation")
	static Date dayOfYear(int year, int dayOfYear) {
		Date d = new Date(year - 1900, 0, 1); dayOfYear--;
		while (dayOfYear < 0) { d.setDate(d.getDate() - 1); dayOfYear ++; }
		while (dayOfYear > 0) { d.setDate(d.getDate() + 1); dayOfYear --; }
		return d;
	}

	
	static String monthName(int year, int month) {
		return DateTimeFormat.getFormat("MMMM yyyy").format(toDate(year, month, 1));
	}
	
	public static class SingleMonth extends AbsolutePanel implements HasValue<Date> {
		List<D> iDays = new ArrayList<D>();
		int iYear, iMonth, iDay;
		String iTitle = null;
		List<SessionMonth> iMonths = null;
		private boolean iAllowDeselect;
		
		public SingleMonth() {
			this(null, Integer.parseInt(DateTimeFormat.getFormat("yyyy").format(new Date())),
					 Integer.parseInt(DateTimeFormat.getFormat("MM").format(new Date())),
					 0);
		}
		
		public SingleMonth(String title) {
			this(title, Integer.parseInt(DateTimeFormat.getFormat("yyyy").format(new Date())),
				 Integer.parseInt(DateTimeFormat.getFormat("MM").format(new Date())),
				 0);
		}
		
		public SingleMonth(Date date) {
			this(null, Integer.parseInt(DateTimeFormat.getFormat("yyyy").format(date)),
				 Integer.parseInt(DateTimeFormat.getFormat("MM").format(date)),
				 Integer.parseInt(DateTimeFormat.getFormat("dd").format(date)));
		}
		
		public SingleMonth(String title, int year, int month, int day) {
			iTitle = title;
			iYear = year;
			iMonth = month;
			iDay = day;
			init();
		}
		
		public boolean isAllowDeselect() { return iAllowDeselect; }
		public void setAllowDeselect(boolean allowDeselect) { iAllowDeselect = allowDeselect; }
		
		public void setMonths(List<SessionMonth> months) {
			iMonths = months;
			init();
		}
		
		private void init() {
			clear(); iDays.clear();
			
			SessionMonth sessionMonth = null;
			boolean hasPrev = false, hasNext = false;
			if (iMonths != null && !iMonths.isEmpty()) {
				for (int i = 0; i < iMonths.size(); i++) {
					SessionMonth m = iMonths.get(i);
					if (m.getMonth() + 1 == iMonth && m.getYear() == iYear) {
						sessionMonth = m;
						if (i > 0) hasPrev = true;
						if (i + 1 < iMonths.size()) hasNext = true;
					}
				}
				if (sessionMonth == null) {
					if (iYear < iMonths.get(0).getYear() || (iYear == iMonths.get(0).getYear() && iMonth <= iMonths.get(0).getMonth())) {
						sessionMonth = iMonths.get(0);
						iYear = sessionMonth.getYear();
						iMonth = sessionMonth.getMonth() + 1;
						hasPrev = false;
						hasNext = iMonths.size() > 1;
						if (iDay > 0) iDay = 1;
					} else {
						sessionMonth = iMonths.get(iMonths.size() - 1);
						iYear = sessionMonth.getYear();
						iMonth = sessionMonth.getMonth() + 1;
						hasPrev = iMonths.size() > 1;
						hasNext = false;
						if (iDay > 0) iDay = daysInMonth(iYear, iMonth);
					}
				}
			}
			if (sessionMonth != null && iDay > 0) {
				while (iDay > 0 && sessionMonth.hasFlag(iDay - 1, SessionMonth.Flag.DISABLED)) iDay --;
				if (iDay == 0) {
					iDay = 1;
					while (iDay <= daysInMonth(iYear, iMonth) && sessionMonth.hasFlag(iDay - 1, SessionMonth.Flag.DISABLED)) iDay ++;
					if (iDay > daysInMonth(iYear, iMonth)) iDay = 0; // all disabled
				}
			}
			
			int firstDayOfWeek = firstDayOfWeek(iYear, iMonth);
			int nrDays = daysInMonth(iYear, iMonth);
			int firstWeekNumber = weekNumber(iYear, iMonth);
			
			addStyleName("month");
						
			if (iTitle != null) {
				P box = new P(null, "box");
				add(box);
				P row = new P(null, "row");
				box.add(row);
				row.add(new P(iTitle, "command"));
			}
			
			P box = new P(null, "box");
			add(box);

			P top = new P(null, "row");
			if (sessionMonth == null) {
				P py = new P("&laquo;", "cell", "left", "clickable");
				py.addMouseDownHandler(new MouseDownHandler() {
					@Override
					public void onMouseDown(MouseDownEvent event) {
						addMonths(-12);
					}
				});
				top.add(py);
				P pm = new P("&lsaquo;", "cell", "left", "clickable");
				top.add(pm);
				pm.addMouseDownHandler(new MouseDownHandler() {
					@Override
					public void onMouseDown(MouseDownEvent event) {
						addMonths(-1);
					}
				});
				P m = new P(monthName(iYear, iMonth), "cell", "label", "middle", "clickable");
				m.addMouseDownHandler(new MouseDownHandler() {
					@Override
					public void onMouseDown(MouseDownEvent event) {
						setDate(new Date());
					}
				});
				top.add(m);
				P nm = new P("&rsaquo;", "cell", "right", "clickable");
				nm.addMouseDownHandler(new MouseDownHandler() {
					@Override
					public void onMouseDown(MouseDownEvent event) {
						addMonths(+1);
					}
				});
				top.add(nm);
				P ny = new P("&raquo;", "cell", "right", "clickable");
				ny.addMouseDownHandler(new MouseDownHandler() {
					@Override
					public void onMouseDown(MouseDownEvent event) {
						addMonths(+12);
					}
				});
				top.add(ny);				
			} else {
				top.add(new P(null, "cell", "left"));
				if (hasPrev) {
					P pm = new P("&lsaquo;", "cell", "left", "clickable");
					top.add(pm);
					pm.addMouseDownHandler(new MouseDownHandler() {
						@Override
						public void onMouseDown(MouseDownEvent event) {
							addMonths(-1);
						}
					});
				} else {
					top.add(new P(null, "cell", "left"));
				}
				P m = new P(monthName(iYear, iMonth), "cell", "label", "middle", "clickable");
				m.addMouseDownHandler(new MouseDownHandler() {
					@Override
					public void onMouseDown(MouseDownEvent event) {
						setDate(new Date());
					}
				});
				top.add(m);
				if (hasNext) {
					P nm = new P("&rsaquo;", "cell", "right", "clickable");
					nm.addMouseDownHandler(new MouseDownHandler() {
						@Override
						public void onMouseDown(MouseDownEvent event) {
							addMonths(+1);
						}
					});
					top.add(nm);
				} else {
					top.add(new P(null, "cell", "right"));
				}
				top.add(new P(null, "cell", "right"));
			}

			box.add(top);
			
			box = new P(null, "box");
			add(box);
			
			P header = new P(null, "row");
			box.add(header);
			P corner = new P(null, "cell", "corner");
			header.add(corner);
			
			for (int i = 0; i < 7; i++) {
				header.add(new P(CONSTANTS.days()[(i + startingDayOfWeek()) % 7], "cell", "dow"));
			}

			int weekNumber = firstWeekNumber;
			P line = new P(null, "row");
			box.add(line);
			P week = new P(String.valueOf(weekNumber ++), "cell", "week");
			line.add(week);
			
			int idx = 0;
			int blanks = (firstDayOfWeek + 7 - startingDayOfWeek()) % 7;
			for (int i = 0; i < blanks; i++) {
				line.add(new P(null, "cell", (i + 1 == blanks ? "last-blank": "blank")));
				idx++;
			}
			
			MouseDownHandler onClick = new MouseDownHandler() {
				@Override
				public void onMouseDown(MouseDownEvent event) {
					if (iDay == ((D)event.getSource()).getNumber() && iAllowDeselect) {
						D old = (iDay <= 0 || iDay > iDays.size() ? null : iDays.get(iDay - 1));
						if (old != null) old.removeStyleName("selected");
						iDay = 0;
						ValueChangeEvent.fire(SingleMonth.this, getValue());
					} else {
						D old = (iDay <= 0 || iDay > iDays.size() ? null : iDays.get(iDay - 1));
						if (old != null) old.removeStyleName("selected");
						iDay = ((D)event.getSource()).getNumber();
						iDays.get(iDay - 1).addStyleName("selected");
						ValueChangeEvent.fire(SingleMonth.this, getValue());
					}
				}
			};
			
			int today = -1;
			if (iYear == Integer.parseInt(DateTimeFormat.getFormat("yyyy").format(new Date())) &&
				iMonth == Integer.parseInt(DateTimeFormat.getFormat("MM").format(new Date())))
				today = Integer.parseInt(DateTimeFormat.getFormat("dd").format(new Date()));
			for (int i = 1; i <= nrDays; i++) {
				if (i > 1 && idx % 7 == 0) {
					if (idx == 7 && iMonth == 1 && weekNumber > 50) weekNumber = 1;
					line = new P(null, "row");
					box.add(line);
					week = new P(String.valueOf(weekNumber ++), "cell", "week");
					line.add(week);
				}
				D d = new D(i, "cell", (((idx + startingDayOfWeek()) % 7) < 5 ? "day" : "weekend"), "clickable", (iDay == i ? "selected" : null));
				line.add(d);
				iDays.add(d);
				idx++;
				boolean enabled = true;
				if (i == today)
					d.addStyleName("today");
				if (sessionMonth != null) {
					if (sessionMonth.hasFlag(i - 1, SessionMonth.Flag.START))
						d.addStyleName("start");
					else if (sessionMonth.hasFlag(i - 1, SessionMonth.Flag.END))
						d.addStyleName("start");
					else if (sessionMonth.hasFlag(i - 1, SessionMonth.Flag.EXAM_START))
						d.addStyleName("exam");
					else if (sessionMonth.hasFlag(i - 1, SessionMonth.Flag.HOLIDAY))
						d.addStyleName("holiday");
					else if (sessionMonth.hasFlag(i - 1, SessionMonth.Flag.BREAK))
						d.addStyleName("break");
					if (sessionMonth.hasFlag(i - 1, SessionMonth.Flag.DISABLED)) {
						d.removeStyleName("clickable");
						d.addStyleName("disabled");
						enabled = false;
					}
				}
				if (enabled)
					d.addMouseDownHandler(onClick);
			}
		}
		
		public void addMonths(int months) {
			iMonth += months;
			if (iMonth <= 0) { iYear -= 1; iMonth += 12; }
			if (iMonth > 12) { iYear += 1; iMonth -= 12; }
			init();
		}
		
		@SuppressWarnings("deprecation")
		public void addDays(int days) {
			Date d = getValue();
			if (d == null) {
				setDate(new Date());
			} else {
				d.setDate(d.getDate() + days);
				setDate(d);
			}
		}
		
		public void clearSelection() {
			D old = (iDay <= 0 || iDay > iDays.size() ? null : iDays.get(iDay - 1));
			if (old != null) {
				old.removeStyleName("selected");
				iDay = 0;
			}
		}
		
		public void setDate(Date date) {
			iYear =  Integer.parseInt(DateTimeFormat.getFormat("yyyy").format(date));
			iMonth = Integer.parseInt(DateTimeFormat.getFormat("MM").format(date));
			iDay = Integer.parseInt(DateTimeFormat.getFormat("dd").format(date));
			init();
		}
		
		@Override
		public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Date> handler) {
			return addHandler(handler, ValueChangeEvent.getType());
		}

		@Override
		public Date getValue() {
			if (iDay < 0 || iDay > iDays.size()) return null;
			return DateTimeFormat.getFormat("yyyy/MM/dd").parse(iYear + "/" + iMonth + "/" + iDay);
		}

		@Override
		public void setValue(Date value) {
			setValue(value, false);
		}

		@Override
		public void setValue(Date value, boolean fireEvents) {
			iYear =  Integer.parseInt(DateTimeFormat.getFormat("yyyy").format(value == null ? new Date() : value));
			iMonth = Integer.parseInt(DateTimeFormat.getFormat("MM").format(value == null ? new Date() : value));
			iDay = (value == null ? 0 : Integer.parseInt(DateTimeFormat.getFormat("dd").format(value)));
			init();
			if (fireEvents)
				ValueChangeEvent.fire(this, value);				
		}
		
		public String toString() {
			return (getValue() == null ? "" : DateTimeFormat.getFormat(CONSTANTS.eventDateFormat()).format(getValue()));
		}
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Date> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public Date getValue() {
		try {
			return iFormat.parse(iPicker.getWidget().getText());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void setValue(Date value) {
		setValue(value, false);
	}

	@Override
	public void setValue(Date value, boolean fireEvents) {
		if (value == null) {
			iPicker.getWidget().setText("");
			iMonth.setValue(null);
		} else {
			iPicker.getWidget().setText(iFormat.format(value));
			iMonth.setValue(value);
		}
		if (fireEvents)
			ValueChangeEvent.fire(this, value);
	}

}
