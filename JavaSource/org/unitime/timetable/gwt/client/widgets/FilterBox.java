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
package org.unitime.timetable.gwt.client.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class FilterBox extends AbsolutePanel implements HasValue<String>, HasValueChangeHandlers<String>, HasText {
	private static String[] sColors = new String[] {
		"blue", "green", "orange", "yellow", "pink",
		"purple", "teal", "darkpurple", "steelblue", "lightblue",
		"lightgreen", "yellowgreen", "redorange", "lightbrown", "lightpurple",
		"grey", "bluegrey", "lightteal", "yellowgrey", "brown"
	};
	
	private TextBox iFilter;
	private SimplePanel iAdd, iClear;
	private PopupPanel iFilterPopup, iSuggestionsPopup;
	private boolean iFocus = false;
	private BlurHandler iBlurHandler;
	private FocusHandler iFocusHandler;
	private SuggestionsProvider iSuggestionsProvider = new DefaultSuggestionsProvider(null);
	private SuggestionMenu iSuggestionMenu;
	
	private Parser iParser = new DefaultParser();
	private Chip2Color iChip2Color = new DefaultChip2Color();
	private List<Filter> iFilters = new ArrayList<Filter>();
	
	public FilterBox() {
		setStyleName("unitime-FilterBox");
		
		final Timer blur = new Timer() {
			@Override
			public void run() {
				if (!iFocus)
					removeStyleName("unitime-FilterBoxFocus");
			}
		};

		iFocusHandler = new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				iFocus = true;
				addStyleName("unitime-FilterBoxFocus");
				refreshSuggestions();
			}
		};
		
		iBlurHandler = new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				iFocus = false;
				blur.schedule(100);
			}
		};
		
		iFilter = new TextBox();
		iFilter.setStyleName("filter");

		
        iFilter.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (isFilterPopupShowing()) {
					hideFilterPopup();
				}
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_BACKSPACE && iFilter.getText().isEmpty()) {
					if (getWidgetCount() > 3) {
						remove(getWidgetCount()-4);
						resizeFilterIfNeeded();
						ValueChangeEvent.fire(FilterBox.this, getValue());
					}
				}
				if (isSuggestionsShowing()) {
					switch (event.getNativeEvent().getKeyCode()) {
					case KeyCodes.KEY_DOWN:
						iSuggestionMenu.selectItem(iSuggestionMenu.getSelectedItemIndex() + 1);
						break;
					case KeyCodes.KEY_UP:
						if (iSuggestionMenu.getSelectedItemIndex() == -1) {
							iSuggestionMenu.selectItem(iSuggestionMenu.getNumItems() - 1);
						} else {
							iSuggestionMenu.selectItem(iSuggestionMenu.getSelectedItemIndex() - 1);
						}
						break;
					case KeyCodes.KEY_ENTER:
						iSuggestionMenu.executeSelected();
						hideSuggestions();
						break;
					case KeyCodes.KEY_TAB:
						hideSuggestions();
						break;
					case KeyCodes.KEY_ESCAPE:
						hideSuggestions();
						break;
					}
					switch (event.getNativeEvent().getKeyCode()) {
					case KeyCodes.KEY_DOWN:
					case KeyCodes.KEY_UP:
					case KeyCodes.KEY_ENTER:
					case KeyCodes.KEY_ESCAPE:
						event.preventDefault();
						event.stopPropagation();
					}
				} else {
					if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_DOWN && (event.getNativeEvent().getAltKey() || iFilter.getCursorPos() == iFilter.getText().length())) {
						showSuggestions();
						event.preventDefault();
						event.stopPropagation();
					}
				}
			}
		});
        iFilter.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				refreshSuggestions();
			}
		});
		iFilter.addFocusHandler(iFocusHandler);
		iFilter.addBlurHandler(iBlurHandler);
		add(iFilter);
		iFilter.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				ValueChangeEvent.fire(FilterBox.this, getValue());
			}
		});
		
        iAdd = new SimplePanel();
        iAdd.getElement().setInnerHTML("+");
        iAdd.addStyleName("button");
        add(iAdd);

		iClear = new SimplePanel();
		iClear.getElement().setInnerHTML("&times;");
		iClear.addStyleName("button");
        add(iClear);
        iClear.setVisible(false);
        iFilter.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (iClear.isVisible() && getValue().isEmpty()) {
					resizeFilterIfNeeded();
				} else if (!iClear.isVisible() && !getValue().isEmpty()) {
					resizeFilterIfNeeded();
				}
			}
		});
        
        iFilterPopup = new PopupPanelKeepFocus();
        iFilterPopup.setStyleName("unitime-FilterBoxPopup");
        iSuggestionMenu = new SuggestionMenu();
        iSuggestionsPopup = new PopupPanelKeepFocus();
        iSuggestionsPopup.setWidget(iSuggestionMenu);
        iSuggestionsPopup.setStyleName("unitime-FilterBoxPopup");
        
        sinkEvents(Event.ONMOUSEDOWN);
	}
	
	public void setSuggestionsProvider(SuggestionsProvider suggestionsProvider) { iSuggestionsProvider = new DefaultSuggestionsProvider(suggestionsProvider); }
	public SuggestionsProvider getSuggestionsProvider() { return iSuggestionsProvider; }
	
	@Override
	public void setWidth(String width) {
		super.setWidth(width);
		iSuggestionsPopup.setWidth(width);
		iFilterPopup.setWidth(width);
	}
	
	public void setParser(Parser parser) { iParser = parser; }
	
	public boolean isSuggestionsShowing() {
		return iSuggestionsPopup.isShowing();
	}
	
	public boolean isFilterPopupShowing() {
		return iFilterPopup.isShowing();
	}
	
	public void hideSuggestions() {
		iSuggestionsPopup.hide();
	}
	
	public void hideFilterPopup() {
		iFilterPopup.hide();
	}
	
	public void addFilter(Filter filter) {
		iFilters.add(filter);
	}
	
	public List<Filter> getFilters() { return iFilters; }
	
	public Filter getFilter(String command) {
		for (Filter filter: getFilters())
			if (filter.getCommand().equals(command)) return filter;
		return null;
	}
	
	public void showFilterPopup() {
		iFilterPopup.setWidget(createFilterPopup());
		iFilterPopup.showRelativeTo(this);
	}
	
	protected Widget createFilterPopup() {
		final AbsolutePanel popupPanel = new AbsolutePanel();
		popupPanel.addStyleName("panel");
		
		for (final Filter filter: iFilters) {
			final AbsolutePanel filterPanel = new AbsolutePanel();
			filterPanel.addStyleName("filter");
			filterPanel.setVisible(false);
			popupPanel.add(filterPanel);
			filter.getPopupWidget(this, new AsyncCallback<Widget>() {
				@Override
				public void onFailure(Throwable caught) {
					if (filter.getCommand().length() > 0) {
						Label label = new Label(filter.getCommand(), false);
						label.addStyleName("command");
						filterPanel.add(label);
					}
					Label error = new Label(caught.getMessage(), false);
					error.addStyleName("error");
					filterPanel.add(error);
					filterPanel.setVisible(true);
				}
				@Override
				public void onSuccess(Widget widget) {
					if (widget == null) return;
					filterPanel.add(widget);
					filterPanel.setVisible(true);
				}
			});
		}
		return popupPanel;
	}
	
	
	private String iLastValue = null;
	public void showSuggestions() {
		iLastValue = null;
		refreshSuggestions();
	}
	
	public void refreshSuggestions() {
		if (getSuggestionsProvider() == null) return;
		if (isFilterPopupShowing()) return;
		String value = getValue();
		if (value.equals(iLastValue)) return;
		iLastValue = value;
		final String query = iFilter.getText();
		getSuggestionsProvider().getSuggestions(getChips(null), query, new AsyncCallback<Collection<Suggestion>>() {
			@Override
			public void onFailure(Throwable caught) {
				if (iSuggestionsPopup.isShowing()) iSuggestionsPopup.hide();
			}

			@Override
			public void onSuccess(Collection<Suggestion> result) {
				if (!query.equals(iFilter.getText())) return; // old request
				if (result != null && !result.isEmpty()) {
					updateSuggestions(result);
					iSuggestionsPopup.showRelativeTo(FilterBox.this);
				} else {
					if (iSuggestionsPopup.isShowing()) iSuggestionsPopup.hide();
				}
			}
		});
	}
	
	protected void updateSuggestions(Collection<Suggestion> suggestions) {
		iSuggestionMenu.clearItems();
		int selected = -1;
		for (final Suggestion suggestion: suggestions) {
			MenuItem item = null;
			Command command = new Command() {
				@Override
				public void execute() {
					hideSuggestions();
					iFilter.setText(suggestion.getReplacementString());
					if (suggestion.getChipToAdd() != null) {
						if (hasChip(suggestion.getChipToAdd()))
							removeChip(suggestion.getChipToAdd(), false);
						else
							addChip(suggestion.getChipToAdd(), false);
					}
					if (suggestion.getChipToRemove() != null)
						removeChip(suggestion.getChipToRemove(), false);
					iLastValue = getValue();
					ValueChangeEvent.fire(FilterBox.this, getValue());
				}
			};
			if (suggestion.getDisplayString() == null ) {
				Chip chip = (suggestion.getChipToAdd() == null ? suggestion.getChipToRemove() : suggestion.getChipToAdd()); 
				item = new MenuItem(chip.getValue() + " <span class='item-command'>" + chip.getCommand() + "</span>", true, command);
			} else {
				item = new MenuItem(SafeHtmlUtils.htmlEscape(suggestion.getDisplayString()) + (suggestion.getHint() == null ? "" : " " + suggestion.getHint()), true, command);
			}
			item.setStyleName("item");
			DOM.setStyleAttribute(item.getElement(), "whiteSpace", "nowrap");
			if (selected < 0 && suggestion.isSelected())
				selected = iSuggestionMenu.getNumItems();
			iSuggestionMenu.addItem(item);
			if (iSuggestionMenu.getNumItems() == 20) break;
		}
		if (selected >= 0)
			iSuggestionMenu.selectItem(selected);
		else
			iSuggestionMenu.selectItem(0);
	}
	
	@Override
	public void onBrowserEvent(Event event) {
    	Element target = DOM.eventGetTarget(event);

	    switch (DOM.eventGetType(event)) {
	    case Event.ONMOUSEDOWN:
	    	boolean add = iAdd.getElement().equals(target);
	    	boolean clear = iClear.getElement().equals(target);
	    	boolean filter = iFilter.getElement().equals(target);
	    	if (isFilterPopupShowing()) {
	    		hideFilterPopup();
	    	} else if (add) {
	    		hideSuggestions();
	    		showFilterPopup();
	    	}
	    	if (clear) {
				iFilter.setText("");
				removeAllChips();
				ValueChangeEvent.fire(FilterBox.this, getValue());
	    	}
	    	if (!filter) {
				event.stopPropagation();
				event.preventDefault();
		    	Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					@Override
					public void execute() {
						iFilter.setFocus(true);
					}
				});
	    	}
			break;
	    }
	}
	
	@Override
	protected void onAttach() {
		super.onAttach();
		resizeFilterIfNeeded();
	}
	
	private void resizeFilterIfNeeded() {
		if (!isAttached()) return;
		iClear.setVisible(!iFilter.getText().isEmpty() || getWidgetCount() > 3);
		if (getWidgetCount() > 3) {
			ChipPanel last = (ChipPanel)getWidget(getWidgetCount()-4);
			int width = getAbsoluteLeft() + getOffsetWidth() - last.getAbsoluteLeft() - last.getOffsetWidth()
					  - iAdd.getElement().getOffsetWidth() - iClear.getElement().getOffsetWidth() - 4;
			if (width < 100)
				width = getElement().getClientWidth()
					  - iAdd.getElement().getOffsetWidth() - iClear.getElement().getOffsetWidth() - 4;
			iFilter.getElement().getStyle().setWidth(width, Unit.PX);
		} else {
			iFilter.getElement().getStyle().setWidth(getElement().getClientWidth() - iAdd.getElement().getOffsetWidth() - iClear.getElement().getOffsetWidth() - 4, Unit.PX);
		}
		if (isSuggestionsShowing())
			iSuggestionsPopup.showRelativeTo(this);
		if (isFilterPopupShowing())
			iFilterPopup.showRelativeTo(this);
	}
	
	public void addChip(Chip chip, boolean fireEvents) {
		final ChipPanel panel = new ChipPanel(chip, iChip2Color.getColor(chip.getCommand()));
		panel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				remove(panel);
				resizeFilterIfNeeded();
				ValueChangeEvent.fire(FilterBox.this, getValue());
			}
		});
		insert(panel, getWidgetCount() - 3);
		resizeFilterIfNeeded();
		if (fireEvents)
			ValueChangeEvent.fire(this, getValue());
	}
	
	public boolean removeChip(Chip chip, boolean fireEvents) {
		for (int i = 0; i < getWidgetCount() - 3; i++) {
			ChipPanel panel = (ChipPanel)getWidget(i);
			if (panel.getChip().equals(chip)) {
				remove(i);
				resizeFilterIfNeeded();
				if (fireEvents)
					ValueChangeEvent.fire(FilterBox.this, getValue());
				return true;
			}
		}
		return false;
	}
	
	public boolean hasChip(Chip chip) {
		for (int i = 0; i < getWidgetCount() - 3; i++) {
			ChipPanel panel = (ChipPanel)getWidget(i);
			if (panel.getChip().equals(chip)) return true;
		}
		return false;
	}
	
	public Chip getChip(String command) {
		for (int i = 0; i < getWidgetCount() - 3; i++) {
			ChipPanel panel = (ChipPanel)getWidget(i);
			if (panel.getChip().getCommand().equalsIgnoreCase(command))
				return panel.getChip();
		}
		return null;
	}
	
	public List<Chip> getChips(String command) {
		List<Chip> chips = new ArrayList<Chip>();
		for (int i = 0; i < getWidgetCount() - 3; i++) {
			ChipPanel panel = (ChipPanel)getWidget(i);
			if (command == null || panel.getChip().getCommand().equalsIgnoreCase(command)) {
				chips.add(panel.getChip());
			}
		}
		return chips;
	}
	
	public void removeAllChips() {
		while (getWidgetCount() > 3) remove(0);
		resizeFilterIfNeeded();
	}
	
	public static class ChipPanel extends AbsolutePanel implements HasClickHandlers, HasText {
		private Chip iChip;
		private Label iLabel;
		private HTML iButton;
		
		public ChipPanel(Chip chip, String color) {
			iChip = chip;
			setStyleName("chip");
			addStyleName(color);
			iLabel = new Label(chip.getValue());
			iLabel.setStyleName("text");
			add(iLabel);
			iButton = new HTML("&times;");
			iButton.setStyleName("button");
			add(iButton);
			setTitle(toString());
		}
		
		@Override
		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return iButton.addClickHandler(handler);
		}

		@Override
		public String getText() {
			return iLabel.getText();
		}

		@Override
		public void setText(String text) {
			iLabel.setText(text);
		}
		
		public Chip getChip() {
			return iChip;
		}
		
		public String toString() {
			return getChip().toString();
		}
	}

	@Override
	public String getValue() {
		String ret = "";
		for (int i = 0; i < getWidgetCount() - 3; i++) {
			ChipPanel chip = (ChipPanel)getWidget(i);
			ret += chip.toString() + " ";
		}
		return ret + iFilter.getText();
	}
	
	@Override
	public void setValue(String text) {
		setValue(text, false);
	}
	
	@Override
	public void setValue(String text, final boolean fireEvents) {		
		removeAllChips();
		iParser.parse(text, getFilters(), new AsyncCallback<Parser.Results>() {
			@Override
			public void onFailure(Throwable caught) {
			}
			@Override
			public void onSuccess(Parser.Results result) {
				for (Chip chip: result.getChips())
					addChip(chip, false);
				iFilter.setText(result.getFilter());
				resizeFilterIfNeeded();
				if (fireEvents)
					ValueChangeEvent.fire(FilterBox.this, getValue());
			}
		});
	}
	
	@Override
	public String getText() {
		return iFilter.getText();
	}
	
	@Override
	public void setText(String text) {
		iFilter.setText(text);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}
	
	public static class Chip implements IsSerializable {
		private String iCommand, iValue, iHint;
		public Chip() {}
		public Chip(String command, String value, String hint) {
			iCommand = command; iValue = value; iHint = hint;
		}
		public Chip(String command, String value) {
			this(command, value, null);
		}
		public String getCommand() { return iCommand; }
		public String getValue() { return iValue; }
		public String getHint() { return iHint; }
		public void setHint(String hint) { iHint = hint; }
		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof Chip)) return false;
			Chip chip = (Chip)other;
			return (chip.getCommand() == null || chip.getCommand().equalsIgnoreCase(getCommand())) && (chip.getValue() == null || chip.getValue().equalsIgnoreCase(getValue()));
		}
		@Override
		public String toString() {
			return getCommand() + ":" + (getValue().contains(" ") ? "\"" + getValue() + "\"" : getValue());
		}
	}
	
	public interface Filter {
		public String getCommand();
		public void validate(String text, AsyncCallback<Chip> callback);
		public void getSuggestions(List<Chip> chips, String text, AsyncCallback<Collection<Suggestion>> callback);
		public void getPopupWidget(FilterBox box, AsyncCallback<Widget> callback);
	}
	
	public static interface Parser {
		public void parse(String text, Collection<Filter> filters, AsyncCallback<Parser.Results> callback);
		
		public static class Results implements IsSerializable {
			private Collection<Chip> iChips = new ArrayList<Chip>();
			private String iFilter = null;
			
			public Results(String filter, Collection<Chip> chips) {
				iChips = chips; iFilter = filter;
			}
			
			public Collection<Chip> getChips() { return iChips; }

			public String getFilter() { return iFilter; }
			
			@Override
			public String toString() {
				return "ParserResults{chips=" + getChips() + ", filter=" + getFilter() + "}";
			}
		}
	}
	
	public static class DefaultParser implements Parser {
		private static RegExp[] sRegExps = new RegExp[] {
				RegExp.compile("^(\\w+):\"([^\"]*)\"(.*)$", "i"),
				RegExp.compile("^(\\w+):([^ ]*) (.*)$", "i"),
				RegExp.compile("^(\\w+):([^ ]*)$", "i")};
		
		@Override
		public void parse(String text, Collection<Filter> filters, AsyncCallback<Parser.Results> callback) {
			parse(new ArrayList<Chip>(), text, filters, callback);
		}
		
		private void parse(final List<Chip> chips, final String text, final Collection<Filter> filters, final AsyncCallback<Parser.Results> callback) {
			if (text.isEmpty()) {
				callback.onSuccess(new Parser.Results(text, chips));
			} else {
				for (RegExp regExp: sRegExps) {
					final MatchResult r = regExp.exec(text);
					if (r == null) continue;
					for (Filter filter: filters) {
						if (filter.getCommand().equals(r.getGroup(1))) {
							filter.validate(r.getGroup(2), new AsyncCallback<Chip>() {
								@Override
								public void onFailure(Throwable caught) {
									callback.onSuccess(new Parser.Results(text, chips));
								}

								@Override
								public void onSuccess(Chip result) {
									if (result == null) {
										callback.onSuccess(new Parser.Results(text, chips));
									} else {
										chips.add(result);
										if (r.getGroupCount() > 3) {
											parse(chips, r.getGroup(3).trim(), filters, callback);
										} else {
											callback.onSuccess(new Parser.Results("", chips));
										}
									}
								}
							});
							return;
						}
					}
				}
				callback.onSuccess(new Parser.Results(text, chips));
			}
		}
	}
	
	public static abstract class SimpleFilter implements Filter {
		private String iCommand;
		private boolean iMultiple = true;
		
		public SimpleFilter(String command) {
			iCommand = command;
		}
		
		public boolean isMultipleSelection() { return iMultiple; }
		public Filter setMultipleSelection(boolean multiple) { iMultiple = multiple; return this; }
		
		@Override
		public String getCommand() {
			return iCommand;
		}
		
		public abstract void getValues(List<Chip> chips, String text, AsyncCallback<Collection<Chip>> callback);
		
		@Override
		public void getSuggestions(final List<Chip> chips, final String text, final AsyncCallback<Collection<Suggestion>> callback) {
			if (text.isEmpty()) {
				callback.onSuccess(null);
			} else {
				getValues(chips, text, new AsyncCallback<Collection<Chip>>() {

					@Override
					public void onFailure(Throwable caught) {
						callback.onFailure(caught);
					}

					@Override
					public void onSuccess(Collection<Chip> result) {
						List<Suggestion> ret = new ArrayList<FilterBox.Suggestion>();
						if (getCommand().toLowerCase().startsWith(text)) {
							for (Chip chip: result)
								if (chips.contains(chip)) { // already in there -- remove
									ret.add(new Suggestion(chip));
								} else {
									Chip old = null;
									for (Chip c: chips) { if (c.getCommand().equals(getCommand())) { old = c; break; } }
									ret.add(new Suggestion(chip, isMultipleSelection() ? null : old));
								}
						} else {
							for (Chip chip: result)
								if (chip.getValue().toLowerCase().startsWith(text.toLowerCase()) ||
									(getCommand() + " " + chip.getValue()).toLowerCase().startsWith(text.toLowerCase()) ||
									(getCommand() + ":" + chip.getValue()).toLowerCase().startsWith(text.toLowerCase())) {
									if (chips.contains(chip)) { // already in there -- remove
										ret.add(new Suggestion(chip));
									} else {
										Chip old = null;
										for (Chip c: chips) { if (c.getCommand().equals(getCommand())) { old = c; break; } }
										ret.add(new Suggestion(chip, isMultipleSelection() ? null : old));
									}
								}
						}
						callback.onSuccess(ret);
					}
					
				});
			}
		}

		@Override
		public void getPopupWidget(final FilterBox box, final AsyncCallback<Widget> callback) {
			getValues(box.getChips(null), box.getText(), new AsyncCallback<Collection<Chip>>() {
				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}

				@Override
				public void onSuccess(Collection<Chip> values) {
					if (values == null || values.isEmpty()) {
						callback.onSuccess(null);
						return;
					}
					AbsolutePanel popup = new AbsolutePanel();
					Label label = new Label(getCommand(), false);
					label.addStyleName("command");
					popup.add(label);
					for (final Chip value: values) {
						HTML item = new HTML(SafeHtmlUtils.htmlEscape(value.getValue()) +
								(value.getHint() == null ? "" : "<span class='item-hint'>" + value.getHint() + "</span>") , false);
						item.addStyleName("value");
						item.addMouseDownHandler(new MouseDownHandler() {
							@Override
							public void onMouseDown(MouseDownEvent event) {
								if (isMultipleSelection()) {
									if (!box.removeChip(value, true))
										box.addChip(value, true);
								} else {
									Chip old = box.getChip(value.getCommand());
									if (old == null) {
										box.addChip(value, true);
									} else if (!old.equals(value)) {
										box.removeChip(old, false);
										box.addChip(value, true);
									}
								}
								event.getNativeEvent().stopPropagation();
								event.getNativeEvent().preventDefault();
							}
						});
						popup.add(item);
					}
					callback.onSuccess(popup);
				}
			});
		}
		
	}
	
	public static class StaticSimpleFilter extends SimpleFilter {
		private List<Chip> iValues = new ArrayList<Chip>();
		private boolean iValidate;
		
		public StaticSimpleFilter(String command, boolean validate, String... values) {
			super(command);
			iValidate = validate;
			for (String value: values)
				iValues.add(new Chip(command, value));
		}
		
		public StaticSimpleFilter(String command, String... values) {
			this(command, values.length > 0, values);
		}
		
		public StaticSimpleFilter(String command, boolean validate, Collection<Chip> chips) {
			super(command);
			iValidate = validate;
			if (chips != null)
				iValues.addAll(chips);
		}
		
		public StaticSimpleFilter(String command, Collection<Chip> chips) {
			this(command, chips != null && !chips.isEmpty(), chips);
		}

		@Override
		public void getValues(List<Chip> chips, String text, AsyncCallback<Collection<Chip>> callback) {
			callback.onSuccess(iValues);
		}
		
		public void setValues(List<Chip> values) { iValues = values; }

		@Override
		public void validate(String text, AsyncCallback<Chip> callback) {
			if (iValidate) {
				for (Chip chip: iValues)
					if (chip.getValue().equals(text)) {
						callback.onSuccess(chip);
						return;
					}
				callback.onFailure(new Exception("Unknown value " + text + "."));
			} else {
				callback.onSuccess(new Chip(getCommand(), text));
			}
		}
	}
	
	public static class CustomFilter implements Filter {
		private String iCommand;
		private AbsolutePanel iPanel = null;
		private Widget[] iWidgets;
		
		public CustomFilter(String command, Widget... popupWidgets) {
			iCommand = command;
			iWidgets = popupWidgets;
		}

		@Override
		public String getCommand() {
			return iCommand;
		}
		
		@Override
		public void validate(String value, AsyncCallback<Chip> callback) {
			callback.onSuccess(new Chip(getCommand(), value));
		}
		
		public boolean isVisible() { return true; }

		@Override
		public void getPopupWidget(final FilterBox box, AsyncCallback<Widget> callback) {
			if (!isVisible()) {
				callback.onSuccess(null);
				return;
			}
			if (iPanel == null) {
				iPanel = new AbsolutePanel();
				iPanel.addStyleName("filter");
				Label label = new Label(getCommand(), false);
				label.addStyleName("command");
				iPanel.add(label);
				AbsolutePanel other = new AbsolutePanel();
				other.addStyleName("other");
				for (Widget w: iWidgets) {
					w.addStyleName("inline");
					if (w instanceof HasBlurHandlers)
						((HasBlurHandlers)w).addBlurHandler(box.iBlurHandler);
					if (w instanceof HasFocusHandlers)
						((HasFocusHandlers)w).addFocusHandler(box.iFocusHandler);
					if (w instanceof HasKeyDownHandlers)
						((HasKeyDownHandlers)w).addKeyDownHandler(new KeyDownHandler() {
							@Override
							public void onKeyDown(KeyDownEvent event) {
								if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
									if (box.isFilterPopupShowing()) box.hideFilterPopup();
							}
						});
					other.add(w);
				}
				iPanel.add(other);
			}
			callback.onSuccess(iPanel);
		}

		@Override
		public void getSuggestions(List<Chip> chips, String text, AsyncCallback<Collection<Suggestion>> callback) {
			callback.onSuccess(null);
		}
	}
	
	public interface Chip2Color {
		public String getColor(String command);
	}
	
	public class DefaultChip2Color implements Chip2Color {
		
		@Override
		public String getColor(String command) {
			for (int i = 0; i < iFilters.size(); i++)
				if (iFilters.get(i).getCommand().equals(command)) return sColors[i];
			return "red";
		}
		
		
	}
	
	public static interface SuggestionsProvider {
		public void getSuggestions(List<Chip> chips, String text, AsyncCallback<Collection<Suggestion>> callback);
	}
	
	public class DefaultSuggestionsProvider implements SuggestionsProvider {
		SuggestionsProvider iNext;
		
		public DefaultSuggestionsProvider(SuggestionsProvider next) {
			iNext = next;
		}

		@Override
		public void getSuggestions(List<Chip> chips, String text, AsyncCallback<Collection<Suggestion>> callback) {
			List<Suggestion> suggestions = new ArrayList<Suggestion>();
			if (!text.isEmpty())
				iterateFilters(chips, text, suggestions, getFilters().iterator(), callback);
			else
				returnSuggestions(chips, text, suggestions, callback);
		}
		
		public void iterateFilters(final List<Chip> chips, final String text, final List<Suggestion> suggestions, final Iterator<Filter> filters, final AsyncCallback<Collection<Suggestion>> callback) {
			if (filters.hasNext()) {
				Filter filter = filters.next();
				filter.getSuggestions(chips, text, new AsyncCallback<Collection<Suggestion>>() {
					@Override
					public void onFailure(Throwable caught) {
						iterateFilters(chips, text, suggestions, filters, callback);
					}
					@Override
					public void onSuccess(Collection<Suggestion> result) {
						if (result != null) suggestions.addAll(result);
						iterateFilters(chips, text, suggestions, filters, callback);
					}
				});
			} else {
				returnSuggestions(chips, text, suggestions, callback);
			}
		}
		
		public void returnSuggestions(final List<Chip> chips, final String text, final List<Suggestion> suggestions, final AsyncCallback<Collection<Suggestion>> callback) {
			if (iNext == null)
				callback.onSuccess(suggestions);
			else
				iNext.getSuggestions(chips, text, new AsyncCallback<Collection<Suggestion>>() {

					@Override
					public void onFailure(Throwable caught) {
						callback.onSuccess(suggestions);
					}

					@Override
					public void onSuccess(Collection<Suggestion> result) {
						if (result != null) suggestions.addAll(result);
						callback.onSuccess(suggestions);
					}
				});
		}
	}
	
	public static class Suggestion implements IsSerializable {
		private String iDisplay, iReplacement, iHint;
		private Chip iAdd, iRemove;
		private boolean iSelected;
		
		public Suggestion() {}
		public Suggestion(String displayString, String replacementString) {
			iDisplay = displayString;
			iReplacement = replacementString;
		}
		
		public Suggestion(String displayString, String replacementString, String hint) {
			iDisplay = displayString;
			iReplacement = replacementString;
			iHint = "<span class='item-hint'>" + hint + "</span>";
		}

		public Suggestion(Chip chip) {
			iAdd = chip; iReplacement = ""; 
		}
		
		public Suggestion(Chip add, Chip remove) {
			iAdd = add; iRemove = remove; iReplacement = ""; 
		}
		
		public Suggestion(String displayString, Chip add) {
			iDisplay = displayString; iReplacement = ""; iAdd = add; iHint = "<span class='item-command'>" + add.getCommand() + "</span>";
		}
		
		public Suggestion(String displayString, Chip add, Chip remove) {
			iDisplay = displayString; iReplacement = ""; iAdd = add; iRemove = remove;
			iHint = "<span class='item-command'>" + (add != null ? add : remove).getCommand() + "</span>";
		}
		
		public void setDisplayString(String display) { iDisplay = display; }
		public String getDisplayString() { return iDisplay; }
		
		public void setReplacementString(String replacement) { iReplacement = replacement; }
		public String getReplacementString() { return iReplacement; }
		
		public void setHint(String hint) { iHint = hint; }
		public String getHint() { return iHint; }
		
		public void setChipToAdd(Chip chip) { iAdd = chip; }
		public Chip getChipToAdd() { return iAdd; }
		
		public void setChipToRemove(Chip chip) { iRemove = chip; }
		public Chip getChipToRemove() { return iRemove; }
		
		public boolean isSelected() { return iSelected; }
		public void setSelected(boolean selected) { iSelected = selected; }
		
		@Override
		public String toString() {
			return ((getDisplayString() == null ? "" : getDisplayString()) +
				(getChipToAdd() == null ? "" : " +" + getChipToAdd()) +
				(getChipToRemove() == null ? "" : " -" + getChipToRemove())).trim();
		}
	}
	
	public class PopupPanelKeepFocus extends PopupPanel {
		public PopupPanelKeepFocus() {
			super(true, false);
			setPreviewingAllNativeEvents(true);
			sinkEvents(Event.ONMOUSEDOWN);
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			switch (DOM.eventGetType(event)) {
		    case Event.ONMOUSEDOWN:
		    	iFocus = true;
		    	break;
			}
		}
	}
	
	private class SuggestionMenu extends MenuBar {
		SuggestionMenu() {
			super(true);
			setStyleName("");
			setFocusOnHoverEnabled(false);
		}
		
		public int getNumItems() {
			return getItems().size();
		}
		
		public int getSelectedItemIndex() {
			MenuItem selectedItem = getSelectedItem();
			if (selectedItem != null)
				return getItems().indexOf(selectedItem);
			return -1;
		}
		
		public void selectItem(int index) {
			List<MenuItem> items = getItems();
			if (index > -1 && index < items.size()) {
				selectItem(items.get(index));
			}
		}
		
		public void executeSelected() {
			MenuItem selected = getSelectedItem();
			if (selected != null)
				selected.getCommand().execute();
		}
	}
	
}