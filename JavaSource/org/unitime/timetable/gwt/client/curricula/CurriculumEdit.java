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
package org.unitime.timetable.gwt.client.curricula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.unitime.timetable.gwt.client.curricula.CurriculaClassifications.ExpectedChangedEvent;
import org.unitime.timetable.gwt.client.curricula.CurriculaClassifications.NameChangedEvent;
import org.unitime.timetable.gwt.client.page.UniTimePageLabel;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.client.widgets.UniTimeTextBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeWidget;
import org.unitime.timetable.gwt.services.CurriculaService;
import org.unitime.timetable.gwt.services.CurriculaServiceAsync;
import org.unitime.timetable.gwt.shared.CurriculumInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicAreaInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicClassificationInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CourseInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumClassificationInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumStudentsInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.DepartmentInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.MajorInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;

/**
 * @author Tomas Muller
 */
public class CurriculumEdit extends Composite {
	private final CurriculaServiceAsync iService = GWT.create(CurriculaService.class);

	private SimpleForm iCurriculaTable;
	
	private UniTimeHeaderPanel iTitleAndButtons;
	
	private UniTimeWidget<TextBox> iCurriculumAbbv, iCurriculumName;
	private UniTimeWidget<ListBox> iCurriculumMajors, iCurriculumArea, iCurriculumDept;
	private CurriculaClassificationsPanel iCurriculumClasfTable = null;
	
	private boolean iDefaultAbbv = false, iDefaultName = false;
	
	private List<AcademicAreaInterface> iAreas = new ArrayList<AcademicAreaInterface>();
	private List<DepartmentInterface> iDepts = new ArrayList<DepartmentInterface>();
	private List<MajorInterface> iMajors = new ArrayList<MajorInterface>();
	private List<AcademicClassificationInterface> iClassifications = new ArrayList<AcademicClassificationInterface>();
	private CurriculumInterface iCurriculum = null;
	
	private CurriculaCourses iCurriculumCourses;
	
	private List<EditFinishedHandler> iEditFinishedHandlers = new ArrayList<EditFinishedHandler>();
	
	private boolean iAreaHasNoMajors = false;
	
	private Mode iMode;
	
	private boolean iSaved = false;
	
	private NavigationProvider iNavigation = null;
	
	public static enum Mode {
		ADD("Add Curriculum", true, true),
		EDIT("Edit Curriculum", true, true),
		DETAILS("Curriculum Detail", false, false),
		DIALOG(null, true, false);
		
		private String iTitle;
		private boolean iEditable, iEditableDetails;
		Mode(String title, boolean editable, boolean details) { iTitle = title; iEditable = editable; iEditableDetails = details; }
		public boolean hasTitle() { return iTitle != null; }
		public String getTitle() { return iTitle; }
		public boolean isEditable() { return iEditable; }
		public boolean areDetailsEditable() { return iEditableDetails; }
	}
	
	public CurriculumEdit(NavigationProvider navigation) {
		iNavigation = navigation;

		ClickHandler backHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (iMode == Mode.EDIT) { // back to detail screen
					loadCurriculum(Mode.DETAILS);
				} else {
					EditFinishedEvent e = new EditFinishedEvent();
					for (EditFinishedHandler h: iEditFinishedHandlers) {
						if (iSaved)
							h.onSave(e);
						else
							h.onBack(e);
					}
				}
			}
		};
		
		ClickHandler saveHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (saveCurriculum()) {
					showLoading("Saving curriculum " + iCurriculum.getName() + " ...");
					iService.saveCurriculum(iCurriculum, new AsyncCallback<Long>() {
						@Override
						public void onFailure(Throwable caught) {
							hideLoading();
							iTitleAndButtons.setErrorMessage("Validation failed (" + caught.getMessage() + ").");
						}
						@Override
						public void onSuccess(Long result) {
							if (iMode == Mode.EDIT) { // back to details page
								iCurriculum.setId(result);
								reload(Mode.DETAILS);
								iSaved = true;
							} else {
								EditFinishedEvent e = new EditFinishedEvent();
								for (EditFinishedHandler h: iEditFinishedHandlers) {
									h.onSave(e);
								}
							}
							hideLoading();
						}
					});
				} else {
					iTitleAndButtons.setErrorMessage("Validation failed, see errors below.");
				}
			}
		};
		
		ClickHandler deleteHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (!Window.confirm("Do you realy want to delete this curriculum?")) return;
				showLoading("Deleting curriculum " + iCurriculum.getName() + " ...");
				iService.deleteCurriculum(iCurriculum.getId(), new AsyncCallback<Boolean>() {
					@Override
					public void onFailure(Throwable caught) {
						iTitleAndButtons.setErrorMessage("Delete failed (" + caught.getMessage() + ").");
						hideLoading();
					}
					@Override
					public void onSuccess(Boolean result) {
						EditFinishedEvent e = new EditFinishedEvent();
						for (EditFinishedHandler h: iEditFinishedHandlers) {
							h.onDelete(e);
						}
						hideLoading();
					}
				});
			}
		};
		
		ClickHandler printHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Window.print();
			}
		};
		
		ClickHandler editHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				loadCurriculum(Mode.EDIT);
			}
		};
		
		ClickHandler nextHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final CurriculumInterface next = (iNavigation == null ? null : iNavigation.next(iCurriculum));
				if (next == null) {
					iTitleAndButtons.setErrorMessage("Next curriculum not provided.");
					return;
				}
				if (getMode().isEditable()) {
					if (saveCurriculum()) {
						showLoading("Saving curriculum " + iCurriculum.getName() + " ...");
						iService.saveCurriculum(iCurriculum, new AsyncCallback<Long>() {
							@Override
							public void onFailure(Throwable caught) {
								hideLoading();
								iTitleAndButtons.setErrorMessage("Validation failed (" + caught.getMessage() + ").");
							}
							@Override
							public void onSuccess(Long result) {
								iSaved = false;
								iCurriculum = next;
								reload(iMode);
								iNavigation.onChange(iCurriculum);
								hideLoading();
							}
						});
					} else {
						iTitleAndButtons.setErrorMessage("Validation failed, see errors below.");
					}
				} else {
					iSaved = false;
					iCurriculum = next;
					reload(iMode);
					iNavigation.onChange(iCurriculum);
				}
			}
		};
		
		ClickHandler previousHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final CurriculumInterface previous = (iNavigation == null ? null : iNavigation.previous(iCurriculum));
				if (previous == null) {
					iTitleAndButtons.setErrorMessage("Previous curriculum not provided.");
					return;
				}
				if (getMode().isEditable()) {
					if (saveCurriculum()) {
						showLoading("Saving curriculum " + iCurriculum.getName() + " ...");
						iService.saveCurriculum(iCurriculum, new AsyncCallback<Long>() {
							@Override
							public void onFailure(Throwable caught) {
								hideLoading();
								iTitleAndButtons.setErrorMessage("Validation failed (" + caught.getMessage() + ").");
							}
							@Override
							public void onSuccess(Long result) {
								iSaved = false;
								iCurriculum = previous;
								reload(iMode);
								iNavigation.onChange(iCurriculum);
								hideLoading();
							}
						});
					} else {
						iTitleAndButtons.setErrorMessage("Validation failed, see errors below.");
					}
				} else {
					iSaved = false;
					iCurriculum = previous;
					reload(iMode);
					iNavigation.onChange(iCurriculum);
				}
			}
		};

		iCurriculaTable = new SimpleForm();

		iTitleAndButtons = new UniTimeHeaderPanel("Curriculum Details");
		iTitleAndButtons.addButton("edit", "<u>E</u>dit", 75, editHandler);
		iTitleAndButtons.addButton("save", "<u>S</u>ave", 75, saveHandler);
		iTitleAndButtons.addButton("previous", "<u>P</u>revious", 75, previousHandler);
		iTitleAndButtons.addButton("next", "<u>N</u>ext", 75, nextHandler);
		iTitleAndButtons.addButton("delete", "<u>D</u>elete", 75, deleteHandler);
		iTitleAndButtons.addButton("print", "Prin<u>t</u>", 75, printHandler);
		iTitleAndButtons.addButton("back", "<u>B</u>ack", 75, backHandler);
		
		
		iCurriculaTable.addHeaderRow(iTitleAndButtons);
		
		iCurriculumAbbv = new UniTimeWidget<TextBox>(new UniTimeTextBox(20, ValueBoxBase.TextAlignment.LEFT));
		iCurriculaTable.addRow("Abbreviation:", iCurriculumAbbv);
		iCurriculumAbbv.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iDefaultAbbv = false;
				iCurriculumAbbv.clearHint();
			}
		});

		iCurriculumName = new UniTimeWidget<TextBox>(new UniTimeTextBox(60, 500));
		iCurriculaTable.addRow("Name:", iCurriculumName);
		iCurriculumName.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iDefaultName = false;
				iCurriculumName.clearHint();
			}
		});

		iCurriculumArea = new UniTimeWidget<ListBox>(new ListBox(false));
		iCurriculumArea.getWidget().setWidth("300px");
		iCurriculumArea.getWidget().setStyleName("unitime-TextBox");
		iCurriculumArea.getWidget().setVisibleItemCount(1);
		iCurriculaTable.addRow("Academic Area:", iCurriculumArea);
		
		iCurriculumArea.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				if (iDefaultName || iDefaultAbbv) {
					try {
						if (iCurriculumArea.getWidget().getSelectedIndex() == 0) {
							if (iDefaultAbbv) iCurriculumAbbv.getWidget().setText("");
							if (iDefaultName) iCurriculumName.getWidget().setText("");
						} else {
							AcademicAreaInterface area = iAreas.get(iCurriculumArea.getWidget().getSelectedIndex() - 1);
							if (iDefaultAbbv) iCurriculumAbbv.getWidget().setText(area.getAbbv());
							if (iDefaultName) iCurriculumName.getWidget().setText(area.getName());
						}
					} catch (Exception e) {}
				}
				iCurriculumArea.clearHint();
				loadMajors(true);
			}
		});

		iCurriculumMajors = new UniTimeWidget<ListBox>(new ListBox(true));
		iCurriculumMajors.getWidget().setWidth("300px");
		iCurriculumMajors.getWidget().setStyleName("unitime-TextBox");
		iCurriculumMajors.getWidget().setVisibleItemCount(3);
		iCurriculumMajors.getWidget().setHeight("100px");
		iCurriculaTable.addRow("Major(s):", iCurriculumMajors);
		
		iCurriculumMajors.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				try {
					String defaultAbbv = "", defaultName = "";
					AcademicAreaInterface area = iAreas.get(iCurriculumArea.getWidget().getSelectedIndex() - 1);
					defaultAbbv = area.getAbbv();
					defaultName = area.getName();
					String majors = "";
					for (int i = 0; i < iCurriculumMajors.getWidget().getItemCount(); i++) {
						if (iCurriculumMajors.getWidget().isItemSelected(i)) {
							MajorInterface m = iMajors.get(i);
							if (!defaultAbbv.contains("/")) { defaultAbbv += "/"; defaultName += " / "; }
							else { defaultAbbv += ","; defaultName += ", "; }
							defaultAbbv += m.getCode();
							defaultName += m.getName();
							if (!majors.isEmpty()) majors += "<br>";
							majors += m.getName();
						}
					}
					if (defaultName.length() > 60) defaultName = defaultName.substring(0, 60);
					if (defaultAbbv.length() > 20) defaultAbbv = defaultAbbv.substring(0, 20);
					if (iDefaultAbbv) iCurriculumAbbv.getWidget().setText(defaultAbbv);
					if (iDefaultName) iCurriculumName.getWidget().setText(defaultName);
					iCurriculumMajors.setPrintText(majors);
				} catch (Exception e) {}
				loadEnrollments(true);
			}
		});

		iCurriculumDept = new UniTimeWidget<ListBox>(new ListBox(false));
		iCurriculumDept.getWidget().setWidth("300px");
		iCurriculumDept.getWidget().setStyleName("unitime-TextBox");
		iCurriculumDept.getWidget().setVisibleItemCount(1);
		iCurriculaTable.addRow("Department:", iCurriculumDept);
		
		iCurriculaTable.addRow("Last Change:", new Label("",false));
		iCurriculaTable.getRowFormatter().setVisible(6, false);
		
		iCurriculumDept.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iCurriculumDept.clearHint();
			}
		});
		
		iCurriculaTable.addHeaderRow("Curriculum Classifications");
		
		iCurriculumClasfTable = new CurriculaClassificationsPanel(new CurriculaClassifications());
		
		iCurriculaTable.addRow(iCurriculumClasfTable);
		

		iCurriculumCourses = new CurriculaCourses();

		iCurriculaTable.addHeaderRow("Course Projections");
	
		iCurriculaTable.addRow(iCurriculumCourses);
		
		iCurriculaTable.addNotPrintableBottomRow(iTitleAndButtons.clonePanel(null));
		
		initWidget(iCurriculaTable);
	}
	
	public Mode getMode() { return iMode; }
	
	private void loadCurriculum(Mode mode) {
		iMode = mode;

		if (iMode.hasTitle())
			UniTimePageLabel.getInstance().setPageName(iMode.getTitle());
		
		if (iCurriculum.getId() == null) {
			iDefaultAbbv = true; iDefaultName = true;
		} else {
			iDefaultAbbv = false; iDefaultName = false;
		}
		if (iDepts.isEmpty() || iAreas.isEmpty())
			iCurriculum.setEditable(false);
		iCurriculumAbbv.clearHint();
		iCurriculumName.clearHint();
		iCurriculumArea.clearHint();
		iCurriculumDept.clearHint();
		iCurriculumClasfTable.clearHint();

		iTitleAndButtons.clearMessage();
		iTitleAndButtons.setEnabled("delete", iMode == Mode.DETAILS && iCurriculum.getId() != null && iCurriculum.isEditable());
		iTitleAndButtons.setEnabled("save", iCurriculum.isEditable() && iMode.isEditable());
		iTitleAndButtons.setEnabled("edit", iCurriculum.isEditable() && !iMode.isEditable());
		iTitleAndButtons.setEnabled("print", iMode == Mode.DETAILS);
		iTitleAndButtons.setEnabled("previous", iNavigation != null && iNavigation.previous(iCurriculum) != null);
		iTitleAndButtons.setEnabled("next", iNavigation != null && iNavigation.next(iCurriculum) != null);

		if (iCurriculum.hasLastChange() && iMode == Mode.DETAILS) {
			((Label)iCurriculaTable.getWidget(6, 1)).setText(iCurriculum.getLastChange());
			iCurriculaTable.getRowFormatter().setVisible(6, true);
		} else {
			iCurriculaTable.getRowFormatter().setVisible(6, false);
		}

		iCurriculumAbbv.getWidget().setText(iCurriculum.getAbbv());
		iCurriculumAbbv.getWidget().setReadOnly(!iCurriculum.isEditable() || !iMode.areDetailsEditable() || !iMode.isEditable());
		iCurriculumName.getWidget().setText(iCurriculum.getName());
		iCurriculumName.getWidget().setReadOnly(!iCurriculum.isEditable() || !iMode.areDetailsEditable() || !iMode.isEditable());
		iCurriculumArea.getWidget().setSelectedIndex(0);
		if (iCurriculum.getAcademicArea() != null) {
			for (int i = 0; i < iAreas.size(); i++)
				if (iAreas.get(i).getId().equals(iCurriculum.getAcademicArea().getId()))
					iCurriculumArea.getWidget().setSelectedIndex(1 + i);
		}
		iCurriculumArea.setText(iCurriculum.getAcademicArea() == null ? "" : iCurriculum.getAcademicArea().getName());
		iCurriculumArea.setReadOnly(!iCurriculum.isEditable() || !iMode.areDetailsEditable() || !iMode.isEditable());
		iCurriculumDept.getWidget().setSelectedIndex(0);
		if (iCurriculum.getDepartment() != null) {
			for (int i = 0; i < iDepts.size(); i++)
				if (iDepts.get(i).getId().equals(iCurriculum.getDepartment().getId()))
					iCurriculumDept.getWidget().setSelectedIndex(1 + i);
		}
		iCurriculumDept.setText(iCurriculum.getDepartment() == null ? "" : iCurriculum.getDepartment().getLabel());
		iCurriculumDept.setReadOnly(!iCurriculum.isEditable() || !iMode.areDetailsEditable() || !iMode.isEditable());

		iCurriculumMajors.setReadOnly(!iCurriculum.isEditable() || !iMode.areDetailsEditable() || !iMode.isEditable());
		iCurriculumMajors.setText(iCurriculum.getCodeMajorNames("<br>"));
		iCurriculumMajors.setPrintText(iCurriculum.getCodeMajorNames("<br>"));
		loadMajors(iMode.areDetailsEditable());
		iCurriculumClasfTable.populate(iCurriculum.getClassifications());
		iCurriculumClasfTable.setReadOnly(!iCurriculum.isEditable() || !iMode.isEditable());
		iCurriculumCourses.populate(iCurriculum, iMode.isEditable());
		for (int col = 0; col < iClassifications.size(); col++) {
			if (iCurriculumClasfTable.getWidget().getExpected(col) == null)
				iCurriculumCourses.setVisible(col, false);
		}
	}
	
	public boolean saveCurriculum() {
		boolean ret = true;

		iCurriculum.setAbbv(iCurriculumAbbv.getWidget().getText());
		if (iCurriculum.getAbbv().isEmpty()) {
			iCurriculumAbbv.setErrorHint("Curriculum abbreviation must be filled in.");
			ret = false;
		}

		iCurriculum.setName(iCurriculumName.getWidget().getText());
		if (iCurriculum.getName().isEmpty()) {
			iCurriculumName.setErrorHint("Curriculum name must be filled in.");
			ret = false;
		}
		
		if (iCurriculumArea.getWidget().getSelectedIndex() <= 0) {
			iCurriculumArea.setErrorHint("An academic area must be selected.");
			ret = false;
		} else {
			AcademicAreaInterface a = new AcademicAreaInterface();
			a.setId(Long.valueOf(iCurriculumArea.getWidget().getValue(iCurriculumArea.getWidget().getSelectedIndex())));
			iCurriculum.setAcademicArea(a);
		}
		
		if (iCurriculum.hasMajors()) { iCurriculum.getMajors().clear(); }
		for (int i = 0; i < iCurriculumMajors.getWidget().getItemCount(); i++) {
			if (iCurriculumMajors.getWidget().isItemSelected(i)) {
				MajorInterface m = new MajorInterface();
				m.setId(Long.valueOf(iCurriculumMajors.getWidget().getValue(i)));
				iCurriculum.addMajor(m);
			}
		}
		if (!iCurriculum.hasMajors())
			for (int i = 0; i < iCurriculumMajors.getWidget().getItemCount(); i++) {
				MajorInterface m = new MajorInterface();
				m.setId(Long.valueOf(iCurriculumMajors.getWidget().getValue(i)));
				iCurriculum.addMajor(m);
			}
		
		if (iCurriculumMajors.getWidget().getItemCount() == 0 && iCurriculumArea.getWidget().getSelectedIndex() > 0 && !iAreaHasNoMajors) {
			iCurriculumArea.setErrorHint("Selected academic area has no majors without a curriculum.");
			ret = false;
		}
		
		if (iCurriculumDept.getWidget().getSelectedIndex() <= 0) {
			iCurriculumDept.setErrorHint("A controlling department must be selected.");
			ret = false;
		} else {
			DepartmentInterface d = new DepartmentInterface();
			d.setId(Long.valueOf(iCurriculumDept.getWidget().getValue(iCurriculumDept.getWidget().getSelectedIndex())));
			iCurriculum.setDepartment(d);
		}
		
		if (!iCurriculumClasfTable.getWidget().saveCurriculum(iCurriculum)) {
			ret = false;
		}
		if (!iCurriculum.hasClassifications()) {
			iCurriculumClasfTable.setErrorHint("At least some students must be expected.");
			ret = false;
		}
		
		if (!iCurriculumCourses.saveCurriculum(iCurriculum)) {
			ret = false;
		}
		
		return ret;
	}

	private void loadMajors(final boolean showEmptyCourses) {
		if (iCurriculumArea.getWidget().getSelectedIndex() > 0) {
			showLoading("Loading majors ...");
			iService.loadMajors(iCurriculum.getId(), Long.valueOf(iCurriculumArea.getWidget().getValue(iCurriculumArea.getWidget().getSelectedIndex())),
					new AsyncCallback<TreeSet<MajorInterface>>() {

						@Override
						public void onFailure(Throwable caught) {
							hideLoading();
						}

						@Override
						public void onSuccess(TreeSet<MajorInterface> result) {
							if (result == null) {
								iAreaHasNoMajors = true;
								result = new TreeSet<MajorInterface>();
							} else {
								iAreaHasNoMajors = false;
							}
							String defaultAbbv = "", defaultName = "";
							AcademicAreaInterface area = null;
							try {
								if (iCurriculumArea.getWidget().getSelectedIndex() > 0) {
									area = iAreas.get(iCurriculumArea.getWidget().getSelectedIndex() - 1);
									defaultAbbv = area.getAbbv();
									defaultName = area.getName();
								}
							} catch (Exception e) {}
							
							iMajors.clear(); iMajors.addAll(result);
							iCurriculumMajors.getWidget().clear();
							int idx = 0;
							boolean allSelected = true;
							CurriculumCookie.getInstance().getCurriculaDisplayMode();
							for (MajorInterface m: result) {
								iCurriculumMajors.getWidget().addItem(m.getCode() + " - " + m.getName(), m.getId().toString());
								if (iCurriculum != null && iCurriculum.hasMajors()) {
									iCurriculumMajors.getWidget().setItemSelected(idx, iCurriculum.getMajors().contains(m));
									if (iCurriculum.getMajors().contains(m)) {
										if (!defaultAbbv.contains("/")) { defaultAbbv += "/"; defaultName += " / "; }
										else { defaultAbbv += ","; defaultName += ", "; }
										defaultAbbv += m.getCode();
										defaultName += m.getName();
									} else {
										allSelected = false;
									}
								}
								idx++;
							}
							if (defaultName.length() > 60) defaultName = defaultName.substring(0, 60);
							if (defaultAbbv.length() > 20) defaultAbbv = defaultAbbv.substring(0, 20);
							iDefaultAbbv = defaultAbbv.equals(iCurriculumAbbv.getWidget().getText());
							iDefaultName = defaultName.equalsIgnoreCase(iCurriculumName.getWidget().getText());
							if (!iDefaultAbbv && allSelected && area != null && area.getAbbv().equals(iCurriculumAbbv.getWidget().getText()))
								iDefaultAbbv = true;
							if (!iDefaultName && allSelected && area != null && area.getName().equalsIgnoreCase(iCurriculumName.getWidget().getText()))
								iDefaultName = true;
							iCurriculumMajors.getWidget().setVisibleItemCount(iCurriculumMajors.getWidget().getItemCount() <= 3 ? 3 : iCurriculumMajors.getWidget().getItemCount() > 10 ? 10 : iCurriculumMajors.getWidget().getItemCount());
							loadEnrollments(showEmptyCourses);
							hideLoading();
						}
					});
		} else {
			iCurriculumMajors.getWidget().clear();
		}
	}
	
	private void loadEnrollments(final boolean showEmptyCourses) {
		if (iCurriculumArea.getWidget().getSelectedIndex() >= 0) {
			final Long areaId = Long.valueOf(iCurriculumArea.getWidget().getValue(iCurriculumArea.getWidget().getSelectedIndex()));
			final List<Long> majorIds = new ArrayList<Long>();
			for (int i = 0; i < iCurriculumMajors.getWidget().getItemCount(); i++) {
				if (iCurriculumMajors.getWidget().isItemSelected(i)) majorIds.add(Long.valueOf(iCurriculumMajors.getWidget().getValue(i)));
			}
			
			if (majorIds.isEmpty()) {
				for (int i = 0; i < iCurriculumMajors.getWidget().getItemCount(); i++) {
					majorIds.add(Long.valueOf(iCurriculumMajors.getWidget().getValue(i)));
				}
			}
			if (majorIds.isEmpty() && !iAreaHasNoMajors) return;
			
			showLoading("Loading course enrollments ...");
			iService.computeEnrollmentsAndLastLikes(areaId, majorIds, new AsyncCallback<HashMap<String,CurriculumStudentsInterface[]>>() {

				@Override
				public void onFailure(Throwable caught) {
					hideLoading();
				}

				@Override
				public void onSuccess(HashMap<String, CurriculumStudentsInterface[]> result) {
					CurriculumStudentsInterface[] x = result.get("");
					for (int col = 0; col < iClassifications.size(); col++) {
						iCurriculumClasfTable.getWidget().setEnrollment(col, x == null || x[col] == null ? null : x[col].getEnrollment());
						iCurriculumClasfTable.getWidget().setLastLike(col, x == null || x[col] == null ? null : x[col].getLastLike());
						iCurriculumClasfTable.getWidget().setProjection(col, x == null || x[col] == null ? null : x[col].getProjection());
					}
					iCurriculumCourses.updateEnrollmentsAndLastLike(result, showEmptyCourses);
					if (iCurriculumClasfTable.isShowingAllColumns())
						iCurriculumClasfTable.getWidget().showAllColumns();
					else
						iCurriculumClasfTable.getWidget().hideEmptyColumns();
					hideLoading();
				}
			});
		}
	}
	
	public void showLoading(String message) { LoadingWidget.getInstance().show(message); }
	
	public void hideLoading() { LoadingWidget.getInstance().hide(); }
	
	public static class EditFinishedEvent {
		
	}
	
	public static interface EditFinishedHandler {
		public void onBack(EditFinishedEvent evt);
		public void onDelete(EditFinishedEvent evt);
		public void onSave(EditFinishedEvent evt);
	}
	
	public void addEditFinishedHandler(EditFinishedHandler h) {
		iEditFinishedHandlers.add(h);
	}
	
	public void addNew() {
		iSaved = false;
		iCurriculum = new CurriculumInterface();
		iCurriculum.setEditable(true);
		if (iDepts.size() == 1) {
			DepartmentInterface d = iDepts.get(0);
			iCurriculumDept.getWidget().setSelectedIndex(1);
			iCurriculum.setDepartment(d);
		}
		loadCurriculum(Mode.ADD);
	}
	
	public void edit(CurriculumInterface curriculum, boolean detailsEditable) {
		iSaved = false;
		iCurriculum = curriculum;
		loadCurriculum(detailsEditable ? Mode.DETAILS : Mode.DIALOG);
	}

	public void setupAreas(TreeSet<AcademicAreaInterface> result) {
		iAreas.clear(); iAreas.addAll(result);
		iCurriculumArea.getWidget().clear();
		iCurriculumArea.getWidget().addItem("Select ...", "");
		for (AcademicAreaInterface area: result) {
			iCurriculumArea.getWidget().addItem(area.getAbbv() + " - " + area.getName(), area.getId().toString());
		}
	}
	
	public void setupDepartments(TreeSet<DepartmentInterface> result) {
		iDepts.clear(); iDepts.addAll(result);
		iCurriculumDept.getWidget().clear();
		iCurriculumDept.getWidget().addItem("Select ...", "");
		for (DepartmentInterface dept: result) {
			iCurriculumDept.getWidget().addItem(dept.getLabel(), dept.getId().toString());
		}
	}
	
	public void setupClassifications(TreeSet<AcademicClassificationInterface> result) {
		iClassifications.clear(); iClassifications.addAll(result);
		iCurriculumClasfTable.getWidget().setup(iClassifications);
		iCurriculumCourses.link(iCurriculumClasfTable.getWidget());
	}
	
	public void showOnlyCourses(TreeSet<CourseInterface> courses) {
		iCurriculumCourses.showOnlyCourses(courses);
	}
	
	private static class CurriculaClassificationsPanel extends UniTimeWidget<CurriculaClassifications> {
		private Label iHint;
		
		public CurriculaClassificationsPanel(CurriculaClassifications classifications) {
			super(classifications);
			
			iHint = new Label();
			iHint.setStyleName("unitime-Hint");
			iHint.setVisible(true);
			iHint.addStyleName("unitime-NoPrint");
			getPanel().insert(iHint, 1);
			
			iHint.setText("Show all columns.");
			iHint.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					if (isShowingAllColumns()) {
						getWidget().hideEmptyColumns();
						iHint.setText("Show all columns.");
					} else {
						getWidget().showAllColumns();
						iHint.setText("Hide empty columns.");
					}
				}
			});
			
			getWidget().addExpectedChangedHandler(new CurriculaClassifications.ExpectedChangedHandler() {
				@Override
				public void expectedChanged(ExpectedChangedEvent e) {
					clearHint();
				}
			});
			
			getWidget().addNameChangedHandler(new CurriculaClassifications.NameChangedHandler() {
				@Override
				public void nameChanged(NameChangedEvent e) {
					clearHint();
				}
			});
		}
		
		public boolean isShowingAllColumns() { return iHint.getText().equals("Hide empty columns."); }
		
		public void populate(TreeSet<CurriculumClassificationInterface> classifications) {
			getWidget().populate(classifications);
			if (isShowingAllColumns())
				getWidget().showAllColumns();
			else
				getWidget().hideEmptyColumns();
		}
		
		public void setReadOnly(boolean readOnly) {
			getWidget().setEnabled(!readOnly);
			iHint.setVisible(!readOnly);
		}
	}
	
	public void reload(final Mode mode) {
		showLoading("Loading curriculum " + iCurriculum.getName() + " ...");
		iService.loadCurriculum(iCurriculum.getId(), new AsyncCallback<CurriculumInterface>() {
			@Override
			public void onFailure(Throwable caught) {
				hideLoading();
			}
			@Override
			public void onSuccess(CurriculumInterface result) {
				iCurriculum = result;
				loadCurriculum(mode);
				hideLoading();
			}
		});
	}
	
	public static interface NavigationProvider {
		public CurriculumInterface previous(CurriculumInterface curriculum);
		public CurriculumInterface next(CurriculumInterface curriculum);
		public void onChange(CurriculumInterface curriculum);
	}
	
	public CurriculumInterface getCurriculum() {
		return iCurriculum;
	}
}
