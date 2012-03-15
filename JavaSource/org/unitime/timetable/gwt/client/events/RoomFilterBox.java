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

import org.unitime.timetable.gwt.client.widgets.FilterBox;
import org.unitime.timetable.gwt.client.widgets.FilterBox.Chip;
import org.unitime.timetable.gwt.client.widgets.FilterBox.Suggestion;
import org.unitime.timetable.gwt.command.client.GwtRpcImplementedBy;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class RoomFilterBox extends UniTimeFilterBox {
	private ListBox iBuildings, iDepartments;
	
	public RoomFilterBox(AcademicSessionProvider session) {
		super(session);
		
		iDepartments = new ListBox(false);
		iDepartments.setWidth("100%");
		
		addFilter(new FilterBox.CustomFilter("department", iDepartments) {
			@Override
			public void getSuggestions(List<Chip> chips, String text, AsyncCallback<Collection<Suggestion>> callback) {
				if (text.isEmpty()) {
					callback.onSuccess(null);
				} else {
					Chip oldChip = getChip("department");
					List<Suggestion> suggestions = new ArrayList<Suggestion>();
					for (int i = 0; i < iDepartments.getItemCount(); i++) {
						Chip chip = new Chip("department", iDepartments.getValue(i));
						String name = iDepartments.getItemText(i);
						if (iDepartments.getValue(i).toLowerCase().startsWith(text.toLowerCase())) {
							suggestions.add(new Suggestion(name, chip, oldChip));
						} else if (text.length() > 2 && (name.toLowerCase().contains(" " + text.toLowerCase()) || name.toLowerCase().contains(" (" + text.toLowerCase()))) {
							suggestions.add(new Suggestion(name, chip, oldChip));
						}
					}
					callback.onSuccess(suggestions);
				}
			}
		});
		iDepartments.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				Chip oldChip = getChip("department");
				Chip newChip = (iDepartments.getSelectedIndex() <= 0 ? null : new Chip("department", iDepartments.getValue(iDepartments.getSelectedIndex())));
				if (oldChip != null) {
					if (newChip == null) {
						removeChip(oldChip, true);
					} else {
						if (!oldChip.getValue().equals(newChip.getValue())) {
							removeChip(oldChip, false);
							addChip(newChip, true);
						}
					}
				} else {
					if (newChip != null)
						addChip(newChip, true);
				}
			}
		});
		
		addFilter(new FilterBox.StaticSimpleFilter("type", true));
		addFilter(new FilterBox.StaticSimpleFilter("feature", true));
		addFilter(new FilterBox.StaticSimpleFilter("group", true));
		addFilter(new FilterBox.StaticSimpleFilter("size"));
		addFilter(new FilterBox.StaticSimpleFilter("flag"));
		
		iBuildings = new ListBox(true);
		iBuildings.setWidth("100%"); iBuildings.setVisibleItemCount(3);
		
		addFilter(new FilterBox.CustomFilter("building", iBuildings) {
			@Override
			public void getSuggestions(List<Chip> chips, String text, AsyncCallback<Collection<Suggestion>> callback) {
				if (text.isEmpty()) {
					callback.onSuccess(null);
				} else {
					List<Suggestion> suggestions = new ArrayList<Suggestion>();
					for (int i = 0; i < iBuildings.getItemCount(); i++) {
						Chip chip = new Chip("building", iBuildings.getValue(i));
						String name = iBuildings.getItemText(i);
						if (iBuildings.getValue(i).toLowerCase().startsWith(text.toLowerCase())) {
							suggestions.add(new Suggestion(name, chip));
						} else if (text.length() > 2 && name.toLowerCase().contains(" " + text.toLowerCase())) {
							suggestions.add(new Suggestion(name, chip));
						}
					}
					callback.onSuccess(suggestions);
				}
			}
			@Override
			public boolean isVisible() {
				return iBuildings.getItemCount() > 0;
			}
		});
		iBuildings.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				boolean changed = false;
				for (int i = 0; i < iBuildings.getItemCount(); i++) {
					Chip chip = new Chip("building", iBuildings.getValue(i));
					if (iBuildings.isItemSelected(i)) {
						if (!hasChip(chip)) {
							addChip(chip, false); changed = true;
						}
					} else {
						if (hasChip(chip)) {
							removeChip(chip, false); changed = true;
						}
					}
				}
				if (changed)
					fireValueChangeEvent();
			}
		});
		
		Label l1 = new Label("Min:");

		final TextBox min = new TextBox();
		min.setStyleName("gwt-SuggestBox");
		min.setMaxLength(10); min.setWidth("50");
		
		Label l2 = new Label("Max:");
		l2.getElement().getStyle().setMarginLeft(10, Unit.PX);

		final TextBox max = new TextBox();
		max.setMaxLength(10); max.setWidth("50");
		max.setStyleName("gwt-SuggestBox");
		
		final CheckBox nearby = new CheckBox("Include close by locations");
		nearby.getElement().getStyle().setMarginLeft(10, Unit.PX);
		
		addFilter(new FilterBox.CustomFilter("other", l1, min, l2, max, nearby) {
			@Override
			public void getSuggestions(final List<Chip> chips, final String text, AsyncCallback<Collection<FilterBox.Suggestion>> callback) {
				if (text.isEmpty()) {
					callback.onSuccess(null);
				} else {
					List<FilterBox.Suggestion> suggestions = new ArrayList<FilterBox.Suggestion>();
					if ("nearby".startsWith(text.toLowerCase()) || "include close by locations".startsWith(text.toLowerCase())) {
						suggestions.add(new Suggestion("Include close by locations", new Chip("flag", "nearby")));
					} else {
						Chip old = null;
						for (Chip c: chips) { if (c.getCommand().equals("size")) { old = c; break; } }
						try {
							String number = text;
							String prefix = "";
							if (text.startsWith("<=") || text.startsWith(">=")) { number = number.substring(2); prefix = text.substring(0, 2); }
							else if (text.startsWith("<") || text.startsWith(">")) { number = number.substring(1); prefix = text.substring(0, 1); }
							Integer.parseInt(number);
							suggestions.add(new Suggestion(new Chip("size", text), old));
							if (prefix.isEmpty()) {
								suggestions.add(new Suggestion(new Chip("size", "<=" + text), old));
								suggestions.add(new Suggestion(new Chip("size", ">=" + text), old));
							}
						} catch (Exception e) {}
						if (text.contains("..")) {
							try {
								String first = text.substring(0, text.indexOf('.'));
								String second = text.substring(text.indexOf("..") + 2);
								Integer.parseInt(first); Integer.parseInt(second);
								suggestions.add(new Suggestion(new Chip("size", text), old));
							} catch (Exception e) {}
						}
					}
					callback.onSuccess(suggestions);
				}
			}

		}); 

		ChangeHandler ch = new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				boolean removed = removeChip(new Chip("size", null), false);
				if (min.getText().isEmpty()) {
					if (max.getText().isEmpty()) {
						if (removed)
							fireValueChangeEvent();
					} else {
						addChip(new Chip("size", "<=" + max.getText()), true);
					}
				} else {
					if (max.getText().isEmpty()) {
						addChip(new Chip("size", ">=" + min.getText()), true);
					} else if (max.getText().equals(min.getText())) {
						addChip(new Chip("size", max.getText()), true);
					} else {
						addChip(new Chip("size", min.getText() + ".." + max.getText()), true);
					}
				}
			}
		};
		min.addChangeHandler(ch);
		max.addChangeHandler(ch);
		
		nearby.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				Chip chip = new Chip("flag", "nearby");
				if (event.getValue()) {
					if (!hasChip(chip)) addChip(chip, true);
				} else {
					if (hasChip(chip)) removeChip(chip, true);
				}
			}
		});
		nearby.addMouseDownHandler(new MouseDownHandler() {
			@Override
			public void onMouseDown(MouseDownEvent event) {
				event.getNativeEvent().stopPropagation();
				event.getNativeEvent().preventDefault();
			}
		});

		addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				if (!isFilterPopupShowing()) {
					nearby.setValue(hasChip(new Chip("flag", "nearby")));
					Chip size = getChip("size");
					if (size != null) {
						if (size.getValue().startsWith("<=")) {
							min.setText(""); max.setText(size.getValue().substring(2));
						} else if (size.getValue().startsWith("<")) {
							try {
								max.setText(String.valueOf(Integer.parseInt(size.getValue().substring(1)) - 1)); min.setText("");							
							} catch (Exception e) {}
						} else if (size.getValue().startsWith(">=")) {
							min.setText(size.getValue().substring(2)); max.setText("");
						} else if (size.getValue().startsWith(">")) {
							try {
								min.setText(String.valueOf(Integer.parseInt(size.getValue().substring(1)) + 1)); max.setText("");							
							} catch (Exception e) {}
						} else if (size.getValue().contains("..")) {
							min.setText(size.getValue().substring(0, size.getValue().indexOf(".."))); max.setText(size.getValue().substring(size.getValue().indexOf("..") + 2));
						} else {
							min.setText(size.getValue()); max.setText(size.getValue());
						}
					}
					for (int i = 0; i < iBuildings.getItemCount(); i++) {
						String value = iBuildings.getValue(i);
						iBuildings.setItemSelected(i, hasChip(new Chip("building", value)));
					}
					iDepartments.setSelectedIndex(0);
					for (int i = 1; i < iDepartments.getItemCount(); i++) {
						String value = iDepartments.getValue(i);
						if (hasChip(new Chip("department", value))) {
							iDepartments.setSelectedIndex(i);
							break;
						}
					}
				}
				if (getAcademicSessionId() != null)
					init(false, getAcademicSessionId(), new Command() {
						@Override
						public void execute() {
							if (isFilterPopupShowing())
								showFilterPopup();
						}
					});
			}
		});
	}
	
	@Override
	protected boolean populateFilter(FilterBox.Filter filter, List<FilterRpcResponse.Entity> entities) {
		if ("building".equals(filter.getCommand())) {
			iBuildings.clear();
			if (entities != null)
				for (FilterRpcResponse.Entity entity: entities)
					iBuildings.addItem(entity.getName() + " (" + entity.getCount() + ")", entity.getAbbreviation());
			for (int i = 0; i < iBuildings.getItemCount(); i++) {
				String value = iBuildings.getValue(i);
				iBuildings.setItemSelected(i, hasChip(new Chip("building", value)));
			}
			return true;
		} else if ("department".equals(filter.getCommand())) {
			iDepartments.clear();
			iDepartments.addItem("All Departments", "");
			if (entities != null)
				for (FilterRpcResponse.Entity entity: entities)
					iDepartments.addItem(entity.getName() + " (" + entity.getCount() + ")", entity.getAbbreviation());
			
			iDepartments.setSelectedIndex(0);
			Chip dept = getChip("department");
			if (dept != null)
				for (int i = 1; i < iDepartments.getItemCount(); i++)
					if (dept.getValue().equals(iDepartments.getValue(i))) {
						iDepartments.setSelectedIndex(i);
						break;
					}
			return true;
		} else return super.populateFilter(filter, entities);
	}
	
	@GwtRpcImplementedBy("org.unitime.timetable.events.RoomFilterBackend")
	public static class RoomFilterRpcRequest extends FilterRpcRequest {
		public RoomFilterRpcRequest() {}
	}

	@Override
	public FilterRpcRequest createRpcRequest() {
		return new RoomFilterRpcRequest();
	}
}
