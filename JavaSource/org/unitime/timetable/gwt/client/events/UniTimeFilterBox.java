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
import java.util.Collection;
import java.util.List;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.widgets.FilterBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeWidget;
import org.unitime.timetable.gwt.client.widgets.FilterBox.Chip;
import org.unitime.timetable.gwt.client.widgets.FilterBox.Suggestion;
import org.unitime.timetable.gwt.client.widgets.FilterBox.SuggestionsProvider;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider.AcademicSessionChangeEvent;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider.AcademicSessionChangeHandler;
import org.unitime.timetable.gwt.shared.EventInterface.FilterRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.FilterRpcResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;

public abstract class UniTimeFilterBox extends Composite implements HasValue<String> {
	private static GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	private AcademicSessionProvider iAcademicSession;
	private UniTimeWidget<FilterBox> iFilter;
	
	public UniTimeFilterBox(AcademicSessionProvider session) {
		iFilter = new UniTimeWidget<FilterBox>(new FilterBox());
		
		iFilter.getWidget().setSuggestionsProvider(new SuggestionsProvider() {
			@Override
			public void getSuggestions(List<Chip> chips, String text, final AsyncCallback<Collection<Suggestion>> callback) {
				Long sessionId = iAcademicSession.getAcademicSessionId();
				if (sessionId == null) {
					callback.onSuccess(null);
					return;
				}
				RPC.execute(createRpcRequest(FilterRpcRequest.Command.SUGGESTIONS, iAcademicSession.getAcademicSessionId(), chips, text), new AsyncCallback<FilterRpcResponse>() {

					@Override
					public void onFailure(Throwable caught) {
						callback.onFailure(caught);
					}

					@Override
					public void onSuccess(FilterRpcResponse result) {
						List<FilterBox.Suggestion> suggestions = new ArrayList<FilterBox.Suggestion>();
						if (result.hasSuggestions()) {
							for (FilterRpcResponse.Entity s: result.getSuggestions())
								addSuggestion(suggestions, s);
						}
						callback.onSuccess(suggestions);
					}
					
				});
			}
		});
		
		initWidget(iFilter);
		
		iAcademicSession = session;
		
		iAcademicSession.addAcademicSessionChangeHandler(new AcademicSessionChangeHandler() {
			@Override
			public void onAcademicSessionChange(AcademicSessionChangeEvent event) {
				if (event.isChanged()) init(true, event.getNewAcademicSessionId(), null);
			}
		});
		
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				init(true, iAcademicSession.getAcademicSessionId(), null);
			}
		});
	}
	
	protected void addSuggestion(List<FilterBox.Suggestion> suggestions, FilterRpcResponse.Entity entity) {
		suggestions.add(new FilterBox.Suggestion(entity.getName(), entity.getAbbreviation(), entity.getProperty("hint", null)));
	}
	
	protected void initAsync() {
		setValue(getValue());
	}

	protected void init(final boolean init, Long academicSessionId, final Command onSuccess) {
		if (academicSessionId == null) {
			iFilter.setHint("No academic session is selected.");
		} else {
			if (init) iFilter.setHint("Loading data for " + iAcademicSession.getAcademicSessionName() + " ...");
			final String value = iFilter.getWidget().getValue();
			RPC.execute(createRpcRequest(FilterRpcRequest.Command.LOAD, academicSessionId, iFilter.getWidget().getChips(null), iFilter.getWidget().getText()), new AsyncCallback<FilterRpcResponse>() {
				@Override
				public void onFailure(Throwable caught) {
					iFilter.setErrorHint(caught.getMessage());
					ToolBox.checkAccess(caught);
				}
				@Override
				public void onSuccess(FilterRpcResponse result) {
					iFilter.clearHint();
					if (!value.equals(iFilter.getWidget().getValue())) return;
					for (FilterBox.Filter filter: iFilter.getWidget().getFilters())
						populateFilter(filter, result.getEntities(filter.getCommand()));
					if (onSuccess != null) onSuccess.execute();
					if (init) initAsync();
				}
			});
		}
	}
	
	protected boolean populateFilter(FilterBox.Filter filter, List<FilterRpcResponse.Entity> entities) {
		if (filter != null && filter instanceof FilterBox.StaticSimpleFilter) {
			FilterBox.StaticSimpleFilter simple = (FilterBox.StaticSimpleFilter)filter;
			List<FilterBox.Chip> chips = new ArrayList<FilterBox.Chip>();
			if (entities != null) {
				for (FilterRpcResponse.Entity entity: entities)
					chips.add(new FilterBox.Chip(filter.getCommand(), entity.getName(), entity.getCount() <= 0 ? null : "(" + entity.getCount() + ")"));
			}
			simple.setValues(chips);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
		return iFilter.getWidget().addValueChangeHandler(handler);
	}

	@Override
	public String getValue() {
		return iFilter.getWidget().getValue();
	}

	@Override
	public void setValue(String value) {
		iFilter.getWidget().setValue(value);
	}

	@Override
	public void setValue(String value, boolean fireEvents) {
		iFilter.getWidget().setValue(value, fireEvents);
		if (fireEvents)
			init(false, iAcademicSession.getAcademicSessionId(), new Command() {
				@Override
				public void execute() {
					if (iFilter.getWidget().isFilterPopupShowing())
						iFilter.getWidget().showFilterPopup();
				}
			});
	}
	
	public void clearHint() {
		iFilter.clearHint();
	}
	
	public void setErrorHint(String error) {
		iFilter.setErrorHint(error);
	}

	public void setHint(String hint) {
		iFilter.setHint(hint);
	}
	
	protected abstract FilterRpcRequest createRpcRequest();
	
	protected FilterRpcRequest createRpcRequest(FilterRpcRequest.Command command, Long sessionId, List<FilterBox.Chip> chips, String text) {
		FilterRpcRequest request = createRpcRequest();
		request.setCommand(command);
		request.setSessionId(sessionId);
		if (chips != null)
			for (Chip chip: chips)
				request.addOption(chip.getCommand(), chip.getValue());
		request.setText(text);
		return request;
	}
	
	public void getElements(final AsyncCallback<List<FilterRpcResponse.Entity>> callback) {
		RPC.execute(getElementsRequest(), new AsyncCallback<FilterRpcResponse>() {

			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(FilterRpcResponse result) {
				callback.onSuccess(result.getResults());
			}
		});
	}
	
	public FilterRpcRequest getElementsRequest() {
		return createRpcRequest(FilterRpcRequest.Command.ENUMERATE, iAcademicSession.getAcademicSessionId(), iFilter.getWidget().getChips(null), iFilter.getWidget().getText());
	}
	
	public void addFilter(FilterBox.Filter filter) {
		iFilter.getWidget().addFilter(filter);
	}
	
	public Chip getChip(String command) {
		return iFilter.getWidget().getChip(command);
	}
	
	public void addChip(FilterBox.Chip chip, boolean fireEvents) {
		iFilter.getWidget().addChip(chip, fireEvents);
	}
	
	public boolean removeChip(FilterBox.Chip chip, boolean fireEvents) {
		return iFilter.getWidget().removeChip(chip, fireEvents);
	}
	
	public boolean hasChip(FilterBox.Chip chip) {
		return iFilter.getWidget().hasChip(chip);
	}
	
	protected void fireValueChangeEvent() {
		ValueChangeEvent.fire(iFilter.getWidget(), iFilter.getWidget().getValue());
	}
	
	public boolean isFilterPopupShowing() {
		return iFilter.getWidget().isFilterPopupShowing();
	}
	
	public void showFilterPopup() {
		iFilter.getWidget().showFilterPopup();
	}
	
	protected Long getAcademicSessionId() {
		return iAcademicSession.getAcademicSessionId();
	}
}
