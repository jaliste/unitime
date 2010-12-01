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
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.curricula.CurriculumEdit.EditFinishedEvent;
import org.unitime.timetable.gwt.client.curricula.CurriculumEdit.EditFinishedHandler;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.UniTimeDialogBox;
import org.unitime.timetable.gwt.resources.GwtResources;
import org.unitime.timetable.gwt.services.CurriculaService;
import org.unitime.timetable.gwt.services.CurriculaServiceAsync;
import org.unitime.timetable.gwt.shared.CurriculumInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicAreaInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicClassificationInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CourseInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumClassificationInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumCourseInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.DepartmentInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class CourseCurriculaTable extends Composite {
	public static final GwtResources RESOURCES =  GWT.create(GwtResources.class);

	private final CurriculaServiceAsync iCurriculaService = GWT.create(CurriculaService.class);

	private VerticalPanel iCurriculaPanel;
	private Image iCurriculaImage, iLoadingImage;
	private MyFlexTable iCurricula;
	private DialogBox iDialog;
	private CurriculumEdit iCurriculumEdit;
	private Label iErrorLabel, iHint;
	
	private AsyncCallback<TreeSet<CurriculumInterface>> iCourseCurriculaCallback = null;
	
	private TreeSet<AcademicClassificationInterface> iClassifications = null;
	
	private TreeSet<CourseInterface> iCourses = new TreeSet<CourseInterface>();
	private List<ChainedCommand> iRowClicks = new ArrayList<ChainedCommand>();
	private List<Integer> iRowTypes = new ArrayList<Integer>();
	private List<Long> iRowAreaId = new ArrayList<Long>();
	
	private Long iOfferingId = null;
	private String iCourseName = null;
	private boolean[] iUsed = null;
	private HashSet<Long> iExpandedAreas = new HashSet<Long>();
	private HashSet<Long> iAllAreas = new HashSet<Long>();
	private int iSelectedRow = -1;
	private boolean iEditable = true;
	
	
	public static enum Type {
		EXP ("Requested"),
		ENRL ("Current"),
		LAST ("Last-Like"),
		PROJ ("Projected by Rule"),
		EXP2ENRL ("Requested / Current"),
		EXP2LAST ("Requested / Last-Like"),
		EXP2PROJ ("Requested / Projected"),
		LAST2ENRL ("Last-Like / Current"),
		PROJ2ENRL ("Projected / Current");

		private String iName;
		
		Type(String name) { iName = name; }
		
		public String getName() { return iName; }
	}

	private static int sRowTypeHeader = 0;
	private static int sRowTypeArea = 1;
	private static int sRowTypeCurriculum = 2;
	private static int sRowTypeOtherArea = 3;
	private static int sRowTypeOther = 4;
	private static int sRowTypeTotal = 5;
	
	public CourseCurriculaTable(boolean editable, boolean showHeader) {
		iEditable = editable;
		
		iCurriculaPanel = new VerticalPanel();
		iCurriculaPanel.setWidth("100%");
		
		if (showHeader) {
			HorizontalPanel header = new HorizontalPanel();
			iCurriculaImage = new Image(CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? RESOURCES.treeOpen() : RESOURCES.treeClosed());
			iCurriculaImage.getElement().getStyle().setCursor(Cursor.POINTER);
			iCurriculaImage.setVisible(false);
			header.add(iCurriculaImage);
			Label curriculaLabel = new Label("Curricula", false);
			curriculaLabel.setStyleName("unitime3-HeaderTitle");
			curriculaLabel.getElement().getStyle().setPaddingLeft(2, Unit.PX);
			header.add(curriculaLabel);
			header.setCellWidth(curriculaLabel, "100%");
			header.setStyleName("unitime3-HeaderPanel");
			iCurriculaPanel.add(header);
			
			iCurriculaImage.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					CurriculumCookie.getInstance().setCurriculaCoursesDetails(!CurriculumCookie.getInstance().getCurriculaCoursesDetails());
					iCurriculaImage.setResource(CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? RESOURCES.treeOpen() : RESOURCES.treeClosed());
					if (iCurricula.getRowCount() > 2) {
						for (int row = 1; row < iCurricula.getRowCount() - 1; row++) {
							int rowType = iRowTypes.get(row);
							if (CurriculumCookie.getInstance().getCurriculaCoursesDetails() && (rowType == sRowTypeCurriculum || rowType == sRowTypeOtherArea)) continue;
							iCurricula.getRowFormatter().setVisible(row, CurriculumCookie.getInstance().getCurriculaCoursesDetails());
						}
						for (int col = 0; col < iClassifications.size()  + 2; col++) {
							iCurricula.getCellFormatter().setStyleName(iCurricula.getRowCount() - 1, col, CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? "unitime-TotalRow" : null );
						}
					}
				}
			});
		}

		iLoadingImage = new Image(RESOURCES.loading_small());
		iLoadingImage.setVisible(false);
		iLoadingImage.getElement().getStyle().setMarginTop(10, Unit.PX);
		iCurriculaPanel.add(iLoadingImage);
		iCurriculaPanel.setCellHorizontalAlignment(iLoadingImage, HasHorizontalAlignment.ALIGN_CENTER);
		iCurriculaPanel.setCellVerticalAlignment(iLoadingImage, HasVerticalAlignment.ALIGN_MIDDLE);
		
		VerticalPanel tableAndHint = new VerticalPanel();
		
		iCurricula = new MyFlexTable();
		tableAndHint.add(iCurricula);
		
		iHint = new Label("Showing " + CurriculumCookie.getInstance().getCourseCurriculaTableType().getName() + " Enrollment");
		iHint.setStyleName("unitime-Hint");
		iHint.setVisible(false);
		tableAndHint.add(iHint);
		tableAndHint.setCellHorizontalAlignment(iHint, HasHorizontalAlignment.ALIGN_RIGHT);
		iCurriculaPanel.add(tableAndHint);
		iHint.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				CurriculumCookie.getInstance().setCourseCurriculaTableType(Type.values()[(CurriculumCookie.getInstance().getCourseCurriculaTableType().ordinal() + 1) % Type.values().length]);
				iHint.setText("Showing " + CurriculumCookie.getInstance().getCourseCurriculaTableType().getName() + " Enrollment");
				if (iCurricula.getRowCount() > 1) {
					for (int row = 1; row < iCurricula.getRowCount(); row++) {
						for (int col = 0; col <= iClassifications.size(); col++) {
							((MyLabel)iCurricula.getWidget(row, getHeaderCols(row) + col)).refresh();
						}
					}
					//((MyLabel)iCurricula.getWidget(iCurricula.getRowCount() - 1, 1)).refresh();
					((Label)iCurricula.getWidget(iCurricula.getRowCount() - 1, 0)).setText("Total " + CurriculumCookie.getInstance().getCourseCurriculaTableType().getName() + " Enrollment");
				}
			}
		});
		
		
		iErrorLabel = new Label("Oooops, something went wrong.");
		iErrorLabel.setStyleName("unitime-ErrorMessage");
		iCurriculaPanel.add(iErrorLabel);
		iErrorLabel.setVisible(false);
				
		initWidget(iCurriculaPanel);
	}
	
	private void openDialog(final CurriculumInterface curriculum, final ConditionalCommand next) {
		if (iDialog == null) {
			iDialog = new UniTimeDialogBox(true, true);
			iCurriculumEdit = new CurriculumEdit();
			ScrollPanel panel = new ScrollPanel(iCurriculumEdit);
			// panel.setSize(Math.round(0.9 * Window.getClientWidth()) + "px", Math.round(0.9 * Window.getClientHeight()) + "px");
			panel.setStyleName("unitime-ScrollPanel");
			iDialog.setWidget(panel);
			iCurriculumEdit.addEditFinishedHandler(new CurriculumEdit.EditFinishedHandler() {
				@Override
				public void onSave(EditFinishedEvent evt) {
					iDialog.hide();
					refresh();
				}
				@Override
				public void onDelete(EditFinishedEvent evt) {
					iDialog.hide();
					refresh();
				}
				@Override
				public void onBack(EditFinishedEvent evt) {
					if (iSelectedRow >= 0) {
						iCurricula.getRowFormatter().setStyleName(iSelectedRow, null);	
					}
					iDialog.hide();
				}
			});
			iCurriculumEdit.setupClassifications(iClassifications);
			iCurriculaService.loadAcademicAreas(new AsyncCallback<TreeSet<AcademicAreaInterface>>() {
				@Override
				public void onFailure(Throwable caught) {
					setErrorMessage("Failed to load academic areas (" + caught.getMessage() + ")");
					next.executeOnFailure();
				}
				@Override
				public void onSuccess(TreeSet<AcademicAreaInterface> result) {
					iCurriculumEdit.setupAreas(result);
					iCurriculaService.loadDepartments(new AsyncCallback<TreeSet<DepartmentInterface>>() {
						@Override
						public void onFailure(Throwable caught) {
							setErrorMessage("Failed to load departments (" + caught.getMessage() + ")");
							next.executeOnFailure();
						}
						@Override
						public void onSuccess(TreeSet<DepartmentInterface> result) {
							iCurriculumEdit.setupDepartments(result);
							iDialog.setText(curriculum.getName());
							iCurriculumEdit.edit(curriculum, false);
							iCurriculumEdit.showOnlyCourses(iCourses);
							iDialog.center();
							next.executeOnSuccess();
						}
					});
				}
			});
		} else {
			iDialog.setText(curriculum.getName());
			iCurriculumEdit.edit(curriculum, false);
			iCurriculumEdit.showOnlyCourses(iCourses);
			iDialog.center();
			next.executeOnSuccess();
		}
		iCurriculumEdit.addEditFinishedHandler(new EditFinishedHandler() {
			@Override
			public void onSave(EditFinishedEvent evt) {
				refresh();
			}
			@Override
			public void onDelete(EditFinishedEvent evt) {
			}
			@Override
			public void onBack(EditFinishedEvent evt) {
			}
		});

	}
	
	private void init(final Command next) {
		iCurriculaService.loadAcademicClassifications(new AsyncCallback<TreeSet<AcademicClassificationInterface>>() {
			@Override
			public void onSuccess(TreeSet<AcademicClassificationInterface> result) {
				iClassifications = result;
				if (next != null) next.execute();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				setErrorMessage("Failed to load classifications (" + caught.getMessage() + ").");
			}
		});
	}
	
	public void clear(boolean loading) {
		for (int row = iCurricula.getRowCount() - 1; row >= 0; row--) {
			iCurricula.removeRow(row);
		}
		iCurricula.clear(true);
		iLoadingImage.setVisible(loading);
		iErrorLabel.setVisible(false);
		iHint.setVisible(false);
	}
	
	private void populate(TreeSet<CurriculumInterface> curricula) {
		// Menu
		ClickHandler menu = new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				final PopupPanel popup = new PopupPanel(true);
				MenuBar menu = new MenuBar(true);
				MenuItem showHide = new MenuItem(CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? "Hide Details" : "Show Details", true, new Command() {
					@Override
					public void execute() {
						popup.hide();
						CurriculumCookie.getInstance().setCurriculaCoursesDetails(!CurriculumCookie.getInstance().getCurriculaCoursesDetails());
						if (iCurriculaImage != null)
							iCurriculaImage.setResource(CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? RESOURCES.treeOpen() : RESOURCES.treeClosed());
						if (iCurricula.getRowCount() > 2) {
							for (int row = 1; row < iCurricula.getRowCount() - 1; row++) {
								int rowType = iRowTypes.get(row);
								if (CurriculumCookie.getInstance().getCurriculaCoursesDetails() && (rowType == sRowTypeCurriculum || rowType == sRowTypeOtherArea)) continue;
								iCurricula.getRowFormatter().setVisible(row, CurriculumCookie.getInstance().getCurriculaCoursesDetails());
							}
							for (int col = 0; col < iClassifications.size()  + 2; col++) {
								iCurricula.getCellFormatter().setStyleName(iCurricula.getRowCount() - 1, col, CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? "unitime-TotalRow" : null );
							}
						}
					}
				});
				showHide.getElement().getStyle().setCursor(Cursor.POINTER);
				menu.addItem(showHide);
				if (iCurricula.getRowCount() > 2 && CurriculumCookie.getInstance().getCurriculaCoursesDetails()) {
					boolean canExpand = false, canCollapse = false;
					for (int row = 1; row < iCurricula.getRowCount() - 1; row++) {
						int rowType = iRowTypes.get(row);
						if (rowType == sRowTypeArea || rowType == sRowTypeOther) {
							if (iCurricula.getRowFormatter().isVisible(row))
								canExpand = true;
							else 
								canCollapse = true;
						}
					}
					if (canExpand) {
						MenuItem expandAll = new MenuItem("Expand All", true, new Command() {
							@Override
							public void execute() {
								popup.hide();
								for (int row = 1; row < iCurricula.getRowCount() - 1; row++) {
									int rowType = iRowTypes.get(row);
									boolean visible = (rowType != sRowTypeArea && rowType != sRowTypeOther);
									iCurricula.getRowFormatter().setVisible(row, visible);
									iExpandedAreas.clear();
									iExpandedAreas.addAll(iAllAreas);
								}
							}
						});
						expandAll.getElement().getStyle().setCursor(Cursor.POINTER);
						menu.addItem(expandAll);
					}
					if (canCollapse) {
						MenuItem collapseAll = new MenuItem("Collapse All", true, new Command() {
							@Override
							public void execute() {
								popup.hide();
								for (int row = 1; row < iCurricula.getRowCount() - 1; row++) {
									int rowType = iRowTypes.get(row);
									boolean visible = (rowType != sRowTypeCurriculum && rowType != sRowTypeOtherArea);
									iCurricula.getRowFormatter().setVisible(row, visible);
									iExpandedAreas.clear();
								}
							}
						});
						collapseAll.getElement().getStyle().setCursor(Cursor.POINTER);
						menu.addItem(collapseAll);
					}
				}
				menu.addSeparator();
				for (final Type t : Type.values()) {
					MenuItem item = new MenuItem(
							"Show " + t.getName() + " Enrollment",
							true,
							new Command() {
								@Override
								public void execute() {
									popup.hide();
									CurriculumCookie.getInstance().setCourseCurriculaTableType(t);
									iHint.setText("Showing " + t.getName() + " Enrollment");
									if (iCurricula.getRowCount() > 1) {
										for (int row = 1; row < iCurricula.getRowCount(); row++) {
											int hc = getHeaderCols(row);
											for (int col = 0; col <= iClassifications.size(); col++) {
												((MyLabel)iCurricula.getWidget(row, hc + col)).refresh();
											}
										}
										//((MyLabel)iCurricula.getWidget(iCurricula.getRowCount() - 1, 1)).refresh();
										((Label)iCurricula.getWidget(iCurricula.getRowCount() - 1, 0)).setText("Total " + t.getName() + " Enrollment");
									}
								}
							});
					if (t == CurriculumCookie.getInstance().getCourseCurriculaTableType())
						item.getElement().getStyle().setColor("#666666");
					item.getElement().getStyle().setCursor(Cursor.POINTER);
					menu.addItem(item);
				}
				menu.addSeparator();
				MenuItem populateProjectedDemands = new MenuItem("Populate Course Projected Demands", true, new Command() {
					@Override
					public void execute() {
						popup.hide();
						LoadingWidget.getInstance().show("Populating projected demands for this offering ...");
						iCurriculaService.populateCourseProjectedDemands(false, iOfferingId, new AsyncCallback<Boolean>(){

							@Override
							public void onFailure(Throwable caught) {
								setErrorMessage("Unable to populate course projected demands (" + caught.getMessage() + ")");
								LoadingWidget.getInstance().hide();
							}

							@Override
							public void onSuccess(Boolean result) {
								ToolBox.open(GWT.getHostPageBaseURL() + "instructionalOfferingDetail.do?io=" + iOfferingId);
							}
							
						});
					}
				});
				populateProjectedDemands.getElement().getStyle().setCursor(Cursor.POINTER);
				menu.addItem(populateProjectedDemands);
				MenuItem populateProjectedDemands2 = new MenuItem("Populate Course Projected Demands (Include Other Students)", true, new Command() {
					@Override
					public void execute() {
						popup.hide();
						LoadingWidget.getInstance().show("Populating projected demands for this course ...");
						iCurriculaService.populateCourseProjectedDemands(true, iOfferingId, new AsyncCallback<Boolean>(){
							@Override
							public void onFailure(Throwable caught) {
								setErrorMessage("Unable to populate course projected demands (" + caught.getMessage() + ")");
								LoadingWidget.getInstance().hide();
							}

							@Override
							public void onSuccess(Boolean result) {
								ToolBox.open(GWT.getHostPageBaseURL() + "instructionalOfferingDetail.do?io=" + iOfferingId);
							}
							
						});
					}
				});
				populateProjectedDemands2.getElement().getStyle().setCursor(Cursor.POINTER);
				menu.addItem(populateProjectedDemands2);
				menu.setVisible(true);
				popup.add(menu);
				popup.showRelativeTo((Widget)event.getSource());
			}
		};
		
		// Create header
		int col = 0;
		final Label curriculumLabel = new Label("Curriculum", false);
		curriculumLabel.addClickHandler(menu);
		iCurricula.setWidget(0, col, curriculumLabel);
		iCurricula.getFlexCellFormatter().setStyleName(0, col, "unitime-ClickableTableHeader");
		iCurricula.getFlexCellFormatter().setWidth(0, col, "100px");
		col++;
		
		final Label areaLabel = new Label("Area", false);
		areaLabel.addClickHandler(menu);
		iCurricula.setWidget(0, col, areaLabel);
		iCurricula.getFlexCellFormatter().setStyleName(0, col, "unitime-ClickableTableHeader");
		iCurricula.getFlexCellFormatter().setWidth(0, col, "100px");
		col++;
		
		final Label majorLabel = new Label("Major(s)", false);
		majorLabel.addClickHandler(menu);
		iCurricula.setWidget(0, col, majorLabel);
		iCurricula.getFlexCellFormatter().setStyleName(0, col, "unitime-ClickableTableHeader");
		iCurricula.getFlexCellFormatter().setWidth(0, col, "100px");
		col++;
		
		for (AcademicClassificationInterface clasf: iClassifications) {
			final Label clasfLabel = new Label(clasf.getCode());
			clasfLabel.addClickHandler(menu);
			iCurricula.setWidget(0, col, clasfLabel);
			iCurricula.getFlexCellFormatter().setStyleName(0, col, "unitime-ClickableTableHeader");
			iCurricula.getFlexCellFormatter().setHorizontalAlignment(0, col, HasHorizontalAlignment.ALIGN_RIGHT);
			iCurricula.getFlexCellFormatter().setWidth(0, col, "75px");
			col++;
		}
		
		final Label totalLabel = new Label("Total", false);
		totalLabel.addClickHandler(menu);
		iCurricula.setWidget(0, col, totalLabel);
		iCurricula.getFlexCellFormatter().setStyleName(0, col, "unitime-ClickableTableHeader");
		iCurricula.getFlexCellFormatter().setHorizontalAlignment(0, col, HasHorizontalAlignment.ALIGN_RIGHT);
		iCurricula.getFlexCellFormatter().setWidth(0, col, "75px");
		col++;

		// Create body
		iCourses.clear();
		iRowClicks.clear();
		iRowClicks.add(null); // for header row
		iRowTypes.clear();
		iRowTypes.add(sRowTypeHeader);
		iRowAreaId.clear();
		iRowAreaId.add(-2l);
		
		int row = 0;
		List<CurriculumInterface> otherCurricula = new ArrayList<CurriculumInterface>();
		List<CurriculumInterface> lastArea = new ArrayList<CurriculumInterface>();
		iAllAreas.clear();
		iUsed = new boolean[iClassifications.size()];
		for (int i = 0; i < iUsed.length; i++)
			iUsed[i] = false;
		int[][] total = new int[iClassifications.size()][];
		for (int i = 0; i <total.length; i++)
			total[i] = new int[] {0, 0, 0, 0};
		int[][] totalThisArea = new int[iClassifications.size()][];
		for (int i = 0; i <totalThisArea.length; i++)
			totalThisArea[i] = new int[] {0, 0, 0, 0};
		
		for (final CurriculumInterface curriculum: curricula) {
			for (CourseInterface course: curriculum.getCourses()) {
				CourseInterface cx = new CourseInterface();
				cx.setId(course.getId()); cx.setCourseName(course.getCourseName());
				iCourses.add(cx);
			}
			if (curriculum.getId() == null) { otherCurricula.add(curriculum); continue; }
			
			iAllAreas.add(curriculum.getAcademicArea().getId());
			if (lastArea.isEmpty() || lastArea.get(0).getAcademicArea().equals(curriculum.getAcademicArea())) {
				lastArea.add(curriculum);
			} else if (!lastArea.equals(curriculum.getAcademicArea())) {
				col = 0; row++;
				iCurricula.getFlexCellFormatter().setColSpan(row, col, 3);
				//iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_CENTER);
				iCurricula.setWidget(row, col++, new HTML("<i>" + lastArea.get(0).getAcademicArea().getAbbv() + " - " + lastArea.get(0).getAcademicArea().getName() + " (" + lastArea.size() + ")</i>", false));
				int tExp = 0, tLast = 0, tEnrl = 0, tProj = 0;
				for (int clasfIdx = 0; clasfIdx < iClassifications.size(); clasfIdx++) {
					int exp = totalThisArea[clasfIdx][0];
					int last = totalThisArea[clasfIdx][1];
					int enrl = totalThisArea[clasfIdx][2];
					int proj = totalThisArea[clasfIdx][3];
					tExp += exp;
					tLast += last;
					tEnrl += enrl;
					tProj += proj;
					iCurricula.setWidget(row, col, new MyLabel(exp, enrl, last, proj));
					iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
					col++;
				}
				iCurricula.setWidget(row, col, new MyLabel(tExp, tEnrl, tLast, tProj));
				iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
				
				final int finalRow = row;
				final int lastAreas = lastArea.size();
				final Long lastAreaId = lastArea.get(0).getAcademicArea().getId();
				iRowClicks.add(new ChainedCommand() {
					@Override
					public void execute(final ConditionalCommand next) {
						iExpandedAreas.add(lastAreaId);
						iCurricula.getRowFormatter().setVisible(finalRow, false);
						for (int row = 1; row <= lastAreas; row++) {
							iCurricula.getRowFormatter().setVisible(finalRow - row, true);
						}
						if (next != null)
							next.executeOnSuccess();
					}

					@Override
					public String getLoadingMessage() {
						return null;
					}
				});
				iRowTypes.add(sRowTypeArea);
				iRowAreaId.add(lastAreaId);
				lastArea.clear();
				for (int i = 0; i <totalThisArea.length; i++)
					totalThisArea[i] = new int[] {0, 0, 0, 0};
				lastArea.add(curriculum);
			}
			col = 0; row++;
			iCurricula.setText(row, col++, curriculum.getAbbv());
			iCurricula.setText(row, col++, curriculum.getAcademicArea().getAbbv());
			iCurricula.setText(row, col++, curriculum.getMajorCodes(", "));
			int clasfIdx = 0;
			int tExp = 0, tLast = 0, tEnrl = 0, tProj = 0;
			for (AcademicClassificationInterface clasf: iClassifications) {
				CurriculumClassificationInterface f = null;
				for (CurriculumClassificationInterface x: curriculum.getClassifications()) {
					if (x.getAcademicClassification().getId().equals(clasf.getId())) { f = x; break; }
				}
				int exp = 0, last = 0, enrl = 0, proj = 0;
				for (CourseInterface course: curriculum.getCourses()) {
					CurriculumCourseInterface cx = course.getCurriculumCourse(clasfIdx);
					if (cx != null) {
						iUsed[clasfIdx] = true;
						exp += (f == null || f.getExpected() == null ? 0 : Math.round(f.getExpected() * cx.getShare()));
						last += (cx.getLastLike() == null ? 0 : cx.getLastLike());
						enrl += (cx.getEnrollment() == null ? 0 : cx.getEnrollment());
						proj += (cx.getProjection() == null ? 0 : cx.getProjection());
					}
				}
				total[clasfIdx][0] += exp;
				total[clasfIdx][1] += last;
				total[clasfIdx][2] += enrl;
				total[clasfIdx][3] += proj;
				totalThisArea[clasfIdx][0] += exp;
				totalThisArea[clasfIdx][1] += last;
				totalThisArea[clasfIdx][2] += enrl;
				totalThisArea[clasfIdx][3] += proj;
				tExp += exp;
				tLast += last;
				tEnrl += enrl;
				tProj += proj;
				iCurricula.setWidget(row, col, new MyLabel(exp, enrl, last, proj));
				iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
				col++;
				clasfIdx++;
			}
			iCurricula.setWidget(row, col, new MyLabel(tExp, tEnrl, tLast, tProj));
			iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
			if (iEditable) {
				iRowClicks.add(new ChainedCommand() {
					@Override
					public void execute(final ConditionalCommand next) {
						iCurriculaService.loadCurriculum(curriculum.getId(), new AsyncCallback<CurriculumInterface>() {
							@Override
							public void onFailure(Throwable caught) {
								setErrorMessage("Failed to load details for " + curriculum.getAbbv() + " (" + caught.getMessage() + ")");
								next.executeOnFailure();
							}
							@Override
							public void onSuccess(CurriculumInterface result) {
								openDialog(result, next);
							}
						});
					}
					@Override
					public String getLoadingMessage() {
						return "Loading details for " + curriculum.getName() + " ...";
					}
				});
			} else {
				final Long lastAreaId = curriculum.getAcademicArea().getId();
				final int finalRow = row;
				iRowClicks.add(new ChainedCommand() {
					@Override
					public void execute(final ConditionalCommand next) {
						int row = finalRow;
						while (row > 0 && iRowTypes.get(row) == sRowTypeCurriculum) {
							iCurricula.getRowFormatter().setVisible(row, false);
							row --;
						}
						row = finalRow + 1;
						while (iRowTypes.get(row) == sRowTypeCurriculum) {
							iCurricula.getRowFormatter().setVisible(row, false);
							row ++;
						}
						iCurricula.getRowFormatter().setVisible(row, true);
						iExpandedAreas.remove(lastAreaId);
						if (next != null)
							next.executeOnSuccess();
					}
					@Override
					public String getLoadingMessage() {
						return null;
					}
				});
			}
			iRowTypes.add(sRowTypeCurriculum);
			iRowAreaId.add(curriculum.getAcademicArea().getId());
		}
		if (!lastArea.isEmpty()) {
			col = 0; row++;
			iCurricula.getFlexCellFormatter().setColSpan(row, col, 3);
			//iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_CENTER);
			iCurricula.setWidget(row, col++, new HTML("<i>" + lastArea.get(0).getAcademicArea().getAbbv() + " - " + lastArea.get(0).getAcademicArea().getName() + " (" + lastArea.size() + ")</i>", false));
			int tExp = 0, tLast = 0, tEnrl = 0, tProj = 0;
			for (int clasfIdx = 0; clasfIdx < iClassifications.size(); clasfIdx++) {
				int exp = totalThisArea[clasfIdx][0];
				int last = totalThisArea[clasfIdx][1];
				int enrl = totalThisArea[clasfIdx][2];
				int proj = totalThisArea[clasfIdx][3];
				tExp += exp;
				tLast += last;
				tEnrl += enrl;
				tProj += proj;
				iCurricula.setWidget(row, col, new MyLabel(exp, enrl, last, proj));
				iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
				col++;
			}
			iCurricula.setWidget(row, col, new MyLabel(tExp, tEnrl, tLast, tProj));
			iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
			final int finalRow = row;
			final int lastAreas = lastArea.size();
			final Long lastAreaId = lastArea.get(0).getAcademicArea().getId();
			iRowClicks.add(new ChainedCommand() {
				@Override
				public void execute(final ConditionalCommand next) {
					iExpandedAreas.add(lastAreaId);
					iCurricula.getRowFormatter().setVisible(finalRow, false);
					for (int row = 1; row <= lastAreas; row++) {
						iCurricula.getRowFormatter().setVisible(finalRow - row, true);
					}
					if (next != null)
						next.executeOnSuccess();
				}
				@Override
				public String getLoadingMessage() {
					return null;
				}
			});
			iRowTypes.add(sRowTypeArea);
			iRowAreaId.add(lastAreaId);
		}
		
		// Other line
		if (!otherCurricula.isEmpty()) {
			int[][] totalOther = new int[iClassifications.size()][];
			for (int i = 0; i <totalOther.length; i++)
				totalOther[i] = new int[] {0, 0, 0, 0};
			for (CurriculumInterface other: otherCurricula) {
				col = 0; row++;
				iCurricula.getFlexCellFormatter().setColSpan(row, col, 3);
				//iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_CENTER);
				iCurricula.setHTML(row, col, "<i>" + other.getAbbv() + " - " + other.getName() + "</i>");
				iCurricula.getCellFormatter().setStyleName(row, col, "unitime-OtherRow");
				col++;
				int tExp = 0, tLast = 0, tEnrl = 0, tProj = 0;
				for (int clasfIdx = 0; clasfIdx < iClassifications.size(); clasfIdx++) {
					int exp = 0, last = 0, enrl = 0, proj = 0;;
					for (CourseInterface course: other.getCourses()) {
						CurriculumCourseInterface cx = course.getCurriculumCourse(clasfIdx);
						if (cx != null) {
							iUsed[clasfIdx] = true;
							exp += 0;
							last += (cx.getLastLike() == null ? 0 : cx.getLastLike());
							enrl += (cx.getEnrollment() == null ? 0 : cx.getEnrollment());
							proj += (cx.getProjection() == null ? 0 : cx.getProjection());
						}
					}
					total[clasfIdx][0] += exp;
					total[clasfIdx][1] += last;
					total[clasfIdx][2] += enrl;
					total[clasfIdx][3] += proj;
					totalOther[clasfIdx][0] += exp;
					totalOther[clasfIdx][1] += last;
					totalOther[clasfIdx][2] += enrl;
					totalOther[clasfIdx][3] += proj;
					tExp += exp;
					tLast += last;
					tEnrl += enrl;
					tProj += proj;
					iCurricula.setWidget(row, col, new MyLabel(exp, enrl, last, proj));
					iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
					iCurricula.getCellFormatter().setStyleName(row, col, "unitime-OtherRow");
					col++;
				}
				iCurricula.setWidget(row, col, new MyLabel(tExp, tEnrl, tLast, tProj));
				iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
				iCurricula.getCellFormatter().setStyleName(row, col, "unitime-OtherRow");
				iRowTypes.add(sRowTypeOtherArea);
				iRowAreaId.add(-1l);
				final int finalRow = row;
				iRowClicks.add(new ChainedCommand() {
					@Override
					public void execute(final ConditionalCommand next) {
						int row = finalRow;
						while (row > 0 && iRowTypes.get(row) == sRowTypeOtherArea) {
							iCurricula.getRowFormatter().setVisible(row, false);
							row --;
						}
						row = finalRow + 1;
						while (iRowTypes.get(row) == sRowTypeOtherArea) {
							iCurricula.getRowFormatter().setVisible(row, false);
							row ++;
						}
						iCurricula.getRowFormatter().setVisible(row, true);
						iExpandedAreas.remove(-1l);
						if (next != null)
							next.executeOnSuccess();
					}
					@Override
					public String getLoadingMessage() {
						return null;
					}
				});
			}
			col = 0; row++;
			iCurricula.getFlexCellFormatter().setColSpan(row, col, 3);
			//iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_CENTER);
			iCurricula.setWidget(row, col, new HTML("<i>Other Students</i>", false));
			iCurricula.getCellFormatter().setStyleName(row, col, "unitime-OtherRow");
			col++;
			int tExp = 0, tLast = 0, tEnrl = 0, tProj = 0;
			for (int clasfIdx = 0; clasfIdx < iClassifications.size(); clasfIdx++) {
				int exp = totalOther[clasfIdx][0];
				int last = totalOther[clasfIdx][1];
				int enrl = totalOther[clasfIdx][2];
				int proj = totalOther[clasfIdx][3];
				tExp += exp;
				tLast += last;
				tEnrl += enrl;
				tProj += proj;
				iCurricula.setWidget(row, col, new MyLabel(exp, enrl, last, proj));
				iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
				iCurricula.getCellFormatter().setStyleName(row, col, "unitime-OtherRow");
				col++;
			}
			iCurricula.setWidget(row, col, new MyLabel(tExp, tEnrl, tLast, tProj));
			iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
			iCurricula.getCellFormatter().setStyleName(row, col, "unitime-OtherRow");
			final int finalRow = row;
			final int lastAreas = otherCurricula.size();
			iAllAreas.add(-1l);
			iRowClicks.add(new ChainedCommand() {
				@Override
				public void execute(final ConditionalCommand next) {
					iExpandedAreas.add(-1l);
					iCurricula.getRowFormatter().setVisible(finalRow, false);
					for (int row = 1; row <= lastAreas; row++) {
						iCurricula.getRowFormatter().setVisible(finalRow - row, true);
					}
					if (next != null)
						next.executeOnSuccess();
				}
				@Override
				public String getLoadingMessage() {
					return null;
				}
			});
			iRowTypes.add(sRowTypeOther);
			iRowAreaId.add(-1l);
		}
		
		// Total line
		col = 0; row++;
		iRowClicks.add(new ChainedCommand() {
			@Override
			public void execute(ConditionalCommand next) {
				CurriculumCookie.getInstance().setCurriculaCoursesDetails(!CurriculumCookie.getInstance().getCurriculaCoursesDetails());
				if (iCurriculaImage != null)
					iCurriculaImage.setResource(CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? RESOURCES.treeOpen() : RESOURCES.treeClosed());
				if (iCurricula.getRowCount() > 2) {
					for (int row = 1; row < iCurricula.getRowCount() - 1; row++) {
						int rowType = iRowTypes.get(row);
						if (CurriculumCookie.getInstance().getCurriculaCoursesDetails() && (rowType == sRowTypeCurriculum || rowType == sRowTypeOtherArea)) continue;
						iCurricula.getRowFormatter().setVisible(row, CurriculumCookie.getInstance().getCurriculaCoursesDetails());
					}
					for (int col = 0; col < iClassifications.size()  + 2; col++) {
						iCurricula.getCellFormatter().setStyleName(iCurricula.getRowCount() - 1, col, CurriculumCookie.getInstance().getCurriculaCoursesDetails() ? "unitime-TotalRow" : null );
					}
				}
				if (next != null)
					next.executeOnSuccess();
			}
			@Override
			public String getLoadingMessage() {
				return null;
			}
		});
		iRowTypes.add(sRowTypeTotal);
		iRowAreaId.add(-3l);
		iCurricula.getFlexCellFormatter().setColSpan(row, col, 3);
		iCurricula.setWidget(row, col, new Label("Total " + CurriculumCookie.getInstance().getCourseCurriculaTableType().getName() + " Enrollment", false));
		iCurricula.getCellFormatter().setStyleName(row, col, "unitime-TotalRow");
		col++;
		for (int clasfIdx = 0; clasfIdx < iClassifications.size(); clasfIdx++) {
			int exp = total[clasfIdx][0];
			int last = total[clasfIdx][1];
			int enrl = total[clasfIdx][2];
			int proj = total[clasfIdx][3];
			iCurricula.setWidget(row, col, new MyLabel(exp, enrl, last, proj));
			iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
			iCurricula.getCellFormatter().setStyleName(row, col, "unitime-TotalRow");
			col++;
		}
		int[] tx = new int[] {0, 0, 0, 0};
		for (int i = 0; i < total.length; i ++)
			for (int j = 0; j < 4; j++)
				tx[j] += total[i][j];
		iCurricula.setWidget(row, col, new MyLabel(tx[0], tx[2], tx[1], tx[3]));
		iCurricula.getCellFormatter().setHorizontalAlignment(row, col, HasHorizontalAlignment.ALIGN_RIGHT);
		iCurricula.getCellFormatter().setStyleName(row, col, "unitime-TotalRow");
		
		// Hide all lines if requested
		if (!CurriculumCookie.getInstance().getCurriculaCoursesDetails()) {
			for (int r = 1; r < iCurricula.getRowCount() - 1; r++) {
				iCurricula.getRowFormatter().setVisible(r, false);
			}
			int r = iCurricula.getRowCount() - 1;
			int hc = getHeaderCols(r);
			for (int c = 0; c < hc + iClassifications.size(); c++) {
				iCurricula.getCellFormatter().setStyleName(r, c, null);
			}
		} else {
			// else collapse all
			for (int r = 1; r < iCurricula.getRowCount() - 1; r++) {
				int rowType = iRowTypes.get(r);
				boolean visible = (rowType != sRowTypeCurriculum && rowType != sRowTypeOtherArea);
				if (iExpandedAreas.contains(iRowAreaId.get(r)))
					visible = !visible;
				iCurricula.getRowFormatter().setVisible(r, visible);
			}
		}

		// Hide not-used classifications
		for (int i = 0; i < iUsed.length; i++) {
			for (int r = 0; r < iCurricula.getRowCount(); r++) {
				if (!iUsed[i]) {
					iCurricula.getCellFormatter().setVisible(r, getHeaderCols(r) + i, false);
				}
			}
		}
		
		boolean typeChanged = false;
		Type type = CurriculumCookie.getInstance().getCourseCurriculaTableType();
		if (type == Type.EXP && tx[0] == 0) {
			if (tx[2] > 0) {
				type = Type.ENRL;
				typeChanged = true;
			} else if (tx[1] > 0) {
				type = Type.LAST;
				typeChanged = true;
			}
		}
		if (type == Type.ENRL && tx[2] == 0) {
			if (tx[0] > 0) {
				type = Type.EXP;
				typeChanged = true;
			} else if (tx[1] > 0) {
				type = Type.LAST;
				typeChanged = true;
			}
		}
		if (type == Type.LAST && tx[1] == 0) {
			if (tx[0] > 0) {
				type = Type.EXP;
				typeChanged = true;
			} else if (tx[2] > 0) {
				type = Type.ENRL;
				typeChanged = true;
			}
		}
		if (type == Type.PROJ && tx[3] == 0) {
			if (tx[0] > 0) {
				type = Type.EXP;
				typeChanged = true;
			} else if (tx[1] > 0) {
				type = Type.ENRL;
				typeChanged = true;
			} else if (tx[2] > 0) {
				type = Type.LAST;
				typeChanged = true;
			}
		}
		if (typeChanged) {
			CurriculumCookie.getInstance().setCourseCurriculaTableType(type);
			iHint.setText("Showing " + type.getName() + " Enrollment");
			if (iCurricula.getRowCount() > 1) {
				for (int r = 1; r < iCurricula.getRowCount(); r++) {
					int hc = getHeaderCols(r);
					for (int c = 0; c <= iClassifications.size(); c++) {
						((MyLabel)iCurricula.getWidget(r, hc + c)).refresh();
					}
				}
				//((MyLabel)iCurricula.getWidget(iCurricula.getRowCount() - 1, 1)).refresh();
				((Label)iCurricula.getWidget(iCurricula.getRowCount() - 1, 0)).setText("Total " + type.getName() + " Enrollment");
			}
		}
		
		iLoadingImage.setVisible(false);
		iHint.setVisible(true);
		iCurriculaImage.setVisible(true);
	}
	
	private int getHeaderCols(int row) {
		int col = 0;
		int left = 3;
		while (left > 0) {
			left -= iCurricula.getFlexCellFormatter().getColSpan(row, col);
			col ++;
		}
		return col;
	}
	
	private void initCallbacks() {
		if (iCourseCurriculaCallback == null) {
			iCourseCurriculaCallback = new AsyncCallback<TreeSet<CurriculumInterface>>() {
				@Override
				public void onFailure(Throwable caught) {
					setErrorMessage("Failed to load curricula (" + caught.getMessage() + ").");
					iLoadingImage.setVisible(false);
				}
				@Override
				public void onSuccess(TreeSet<CurriculumInterface> result) {
					if (result.isEmpty()) {
						setMessage("The selected offering has no curricula.");
					} else {
						populate(result);
					}
					iLoadingImage.setVisible(false);
				}
			};			
		}
	}
	
	public void refresh() {
		Command populate = new Command() {
			@Override
			public void execute() {
				clear(true);
				if (iOfferingId != null)
					iCurriculaService.findCurriculaForAnInstructionalOffering(iOfferingId, iCourseCurriculaCallback);
				else
					iCurriculaService.findCurriculaForACourse(iCourseName, iCourseCurriculaCallback);
			}
		};
		if (iClassifications == null) {
			init(populate);
		} else {
			populate.execute();
		}
	}
	
	public void insert(final RootPanel panel) {
		initCallbacks();
		iOfferingId = Long.valueOf(panel.getElement().getInnerText());
		iCourseName = null;
		refresh();
		panel.getElement().setInnerText(null);
		panel.add(this);
		panel.setVisible(true);
	}
	
	public void setCourseName(String courseName) {
		initCallbacks();
		iOfferingId = null;
		iCourseName = courseName;
		refresh();
	}
	
	public class MyLabel extends HTML {
		private int iExp, iLast, iEnrl, iProj;
		
		public MyLabel(int exp, int enrl, int last, int proj) {
			//super(exp > 0 || enrl > 0 || last > 0 ? ((exp > 0 ? exp : "-") + " / " + (enrl > 0 ? enrl : "-") + " / " + (last > 0 ? last : "-")) : "", false);
			super("&nbsp;", false);
			iExp = exp;
			iLast = last;
			iEnrl = enrl;
			iProj = proj;
			refresh();
		}
		
		public void showExpected() {
			setHTML(iExp > 0 ? String.valueOf(iExp) : "&nbsp;");
		}
		
		public void showEnrolled() {
			setHTML(iEnrl > 0 ? String.valueOf(iEnrl) : "&nbsp;");
		}

		public void showLastLike() {
			setHTML(iLast > 0 ? String.valueOf(iLast) : "&nbsp;");
		}
		
		public void showProjected() {
			setHTML(iProj > 0 ? String.valueOf(iProj) : "&nbsp;");
		}

		public void showExpectedEnrolled() {
			if (iExp > 0 || iEnrl > 0)
				setHTML((iExp > 0 ? String.valueOf(iExp) : "-") + " / " + (iEnrl > 0 ? String.valueOf(iEnrl) : "-"));
			else
				setHTML("&nbsp;");
		}
		
		public void showExpectedLastLike() {
			if (iExp > 0 || iLast > 0)
				setHTML((iExp > 0 ? String.valueOf(iExp) : "-") + " / " + (iLast > 0 ? String.valueOf(iLast) : "-"));
			else
				setHTML("&nbsp;");
		}

		public void showExpectedProjected() {
			if (iExp > 0 || iProj > 0)
				setHTML((iExp > 0 ? String.valueOf(iExp) : "-") + " / " + (iProj > 0 ? String.valueOf(iProj) : "-"));
			else
				setHTML("&nbsp;");
		}

		public void showLastLikeEnrolled() {
			if (iLast > 0 || iEnrl > 0)
				setHTML((iLast > 0 ? String.valueOf(iLast) : "-") + " / " + (iEnrl > 0 ? String.valueOf(iEnrl) : "-"));
			else
				setHTML("&nbsp;");
		}
		
		public void showProjectedEnrolled() {
			if (iProj > 0 || iEnrl > 0)
				setHTML((iProj > 0 ? String.valueOf(iProj) : "-") + " / " + (iEnrl > 0 ? String.valueOf(iEnrl) : "-"));
			else
				setHTML("&nbsp;");
		}

		public void refresh() {
			switch (CurriculumCookie.getInstance().getCourseCurriculaTableType()) {
			case EXP:
				showExpected();
				break;
			case ENRL:
				showEnrolled();
				break;
			case LAST:
				showLastLike();
				break;
			case PROJ:
				showProjected();
				break;
			case EXP2LAST:
				showExpectedLastLike();
				break;
			case EXP2ENRL:
				showExpectedEnrolled();
				break;
			case EXP2PROJ:
				showExpectedProjected();
				break;
			case LAST2ENRL:
				showLastLikeEnrolled();
				break;
			case PROJ2ENRL:
				showProjectedEnrolled();
			}
		}

	}
	
	public class MyFlexTable extends FlexTable {

		public MyFlexTable() {
			super();
			sinkEvents(Event.ONMOUSEOVER);
			sinkEvents(Event.ONMOUSEOUT);
			sinkEvents(Event.ONCLICK);
			setCellPadding(2);
			setCellSpacing(0);
		}
		
		public void onBrowserEvent(Event event) {
			Element td = getEventTargetCell(event);
			if (td==null) return;
		    Element tr = DOM.getParent(td);
		    Element body = DOM.getParent(tr);
		    final int row = DOM.getChildIndex(body, tr);

		    final ChainedCommand command = iRowClicks.get(row);
		    
		    switch (DOM.eventGetType(event)) {
			case Event.ONMOUSEOVER:
				getRowFormatter().setStyleName(row, "unitime-TableRowHover");
				if (command == null) getRowFormatter().getElement(row).getStyle().setCursor(Cursor.AUTO);
				break;
			case Event.ONMOUSEOUT:
				getRowFormatter().setStyleName(row, null);	
				break;
			case Event.ONCLICK:
				if (command == null) break;
				if (command.getLoadingMessage() != null)
					LoadingWidget.getInstance().show(command.getLoadingMessage());
				getRowFormatter().setStyleName(row, "unitime-TableRowSelected");
				iSelectedRow = row;
				command.execute(new ConditionalCommand() {
					@Override
					public void executeOnSuccess() {
						//getRowFormatter().setStyleName(row, null);	
						if (command.getLoadingMessage() != null)
							LoadingWidget.getInstance().hide();
					}
					@Override
					public void executeOnFailure() {
						getRowFormatter().setStyleName(row, "unitime-TableRowHover");	
						if (command.getLoadingMessage() != null)
							LoadingWidget.getInstance().hide();
					}
				});
				break;
			}
		}
	}
	
	public static interface ChainedCommand {
		public void execute(ConditionalCommand command);
		public String getLoadingMessage();
	}

	public static interface ConditionalCommand {
		public void executeOnSuccess();
		public void executeOnFailure();
	}
	
	public void setErrorMessage(String message) {
		iErrorLabel.setStyleName("unitime-ErrorMessage");
		iErrorLabel.setText(message);
		iErrorLabel.setVisible(message != null && !message.isEmpty());
	}
	
	public void setMessage(String message) {
		iErrorLabel.setStyleName("unitime-Message");
		iErrorLabel.setText(message);
		iErrorLabel.setVisible(message != null && !message.isEmpty());
	}
	
}
