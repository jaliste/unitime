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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unitime.timetable.gwt.client.curricula.CurriculaClassifications.NameChangedEvent;
import org.unitime.timetable.gwt.client.widgets.UniTimeDialogBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader;
import org.unitime.timetable.gwt.client.widgets.UniTimeTextBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasCellAlignment;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HintProvider;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.TableEvent;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader.Operation;
import org.unitime.timetable.gwt.shared.CurriculumInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicClassificationInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CourseInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumCourseGroupInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumCourseInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumStudentsInterface;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class CurriculaCourses extends Composite {
	private UniTimeTable<String> iTable = null;
	
	private static NumberFormat NF = NumberFormat.getFormat("##0.0");
	
	public static enum Mode {
		LAST ("Last", "Last-Like Enrollment"),
		PROJ ("Proj", "Projection by Rule"),
		ENRL ("Curr", "Current Enrollment"),
		NONE ("&nbsp;", "NONE");

		private String iAbbv, iName;
		
		Mode(String abbv, String name) { iAbbv = abbv; iName = name; }
		
		public String getAbbv() { return iAbbv; }
		public String getName() { return iName; }
	}
	
	private List<Group> iGroups = new ArrayList<Group>();
	
	private CurriculaClassifications iClassifications;
	
	private CurriculaCourseSelectionBox.CourseSelectionChangeHandler iCourseChangedHandler = null;
	
	private GroupDialogBox iNewGroupDialog;
	private boolean iEditable = true;
	
	private static String[] sColors = new String[] {
		"red", "blue", "green", "orange", "yellow", "pink",
		"purple", "teal", "darkpurple", "steelblue", "lightblue",
		"lightgreen", "yellowgreen", "redorange", "lightbrown", "lightpurple",
		"grey", "bluegrey", "lightteal", "yellowgrey", "brown"
	};
	
	private TreeSet<String> iVisibleCourses = null;
	private HashMap<String, CurriculumStudentsInterface[]> iLastCourses = null;
	
	public CurriculaCourses() {
		iTable = new UniTimeTable<String>();
		initWidget(iTable);
		iCourseChangedHandler = new CurriculaCourseSelectionBox.CourseSelectionChangeHandler() {
			@Override
			public void onChange(CurriculaCourseSelectionBox.CourseSelectionChangeEvent evt) {
				CurriculumStudentsInterface[] c = (iLastCourses == null ? null : iLastCourses.get(evt.getCourse()));
				for (int col = 0; col < iClassifications.getClassifications().size(); col ++) {
					setEnrollmentAndLastLike(evt.getCourse(), col,
							c == null || c[col] == null ? null : c[col].getEnrollment(), 
							c == null || c[col] == null ? null : c[col].getLastLike(),
							c == null || c[col] == null ? null : c[col].getProjection());
				}
				Element td = evt.getSource().getElement();
				while (td != null && !DOM.getElementProperty(td, "tagName").equalsIgnoreCase("td")) {
					td = DOM.getParent(td);
				}
				Element tr = DOM.getParent(td);
			    Element body = DOM.getParent(tr);
			    int row = DOM.getChildIndex(body, tr);
			    if (evt.getCourse().isEmpty()) {
					iTable.getRowFormatter().addStyleName(row, "unitime-NoPrint");
			    } else {
					iTable.getRowFormatter().removeStyleName(row, "unitime-NoPrint");
			    }
			    if (row + 1 == iTable.getRowCount() && !evt.getCourse().isEmpty())
					addBlankLine();
			}
		};
		
		iNewGroupDialog = new GroupDialogBox();
		
		iTable.setHintProvider(new HintProvider<String>() {
			@Override
			public Widget getHint(TableEvent<String> event) {
				if (!canShowStudentsTable(event.getRow())) return null;
				StudentsTable studentsTable = new StudentsTable(event.getRow());
				if (studentsTable.canShow()) return studentsTable;
				return null;
			}
		});
	}
	
	public void link(CurriculaClassifications cx) {
		iClassifications = cx;
		iClassifications.addExpectedChangedHandler(new CurriculaClassifications.ExpectedChangedHandler() {
			@Override
			public void expectedChanged(CurriculaClassifications.ExpectedChangedEvent e) {
				setVisible(e.getColumn(), e.getExpected() != null);
				if (e.getExpected() != null)
					CurriculaCourses.this.expectedChanged(e.getColumn(), e.getExpected());
			}
		});
		iClassifications.addNameChangedHandler(new CurriculaClassifications.NameChangedHandler() {
			@Override
			public void nameChanged(NameChangedEvent e) {
				((Label)iTable.getWidget(0, 2 + 2 * e.getColumn())).setText(e.getName());
			}
		});
	}
	
	public void populate(CurriculumInterface curriculum, boolean editable) {
		iEditable = curriculum.isEditable() && editable;
		iTable.setAllowSelection(iEditable);
		iTable.clearTable();
		// iTable.clear(true);
		iGroups.clear();
		
		// header
		List<UniTimeTableHeader> header = new ArrayList<UniTimeTableHeader>();
		UniTimeTableHeader hGroup = new UniTimeTableHeader("Group") {
			@Override
			public List<Operation> getOperations() {
				List<Operation> ret = new ArrayList<Operation>();
				for (CurriculaCourses.Group g: getGroups()) {
					ret.add(g.getOperation());
				}
				ret.addAll(super.getOperations());
				return ret;
			}
		};
		header.add(hGroup);
		hGroup.addOperation(new Operation() {
			@Override
			public String getName() {
				return "New group...";
			}
			@Override
			public boolean hasSeparator() {
				return true;
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null && iEditable && iTable.getSelectedCount() > 0;
			}
			@Override
			public void execute() {
				iTable.clearHover();
				iNewGroupDialog.openNew();
			}
		});
		hGroup.addOperation(new Operation() {
			@Override
			public String getName() {
				return "Sort by Group";
			}
			@Override
			public boolean hasSeparator() {
				return true;
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null;
			}
			@Override
			public void execute() {
				iTable.sortByRow(new Comparator<Integer>() {
					public int compare(Integer a, Integer b) {
						return compareTwoRows(0, a, b);
					}
				});
			}
		});
		
		UniTimeTableHeader hCourse = new UniTimeTableHeader("Course");
		header.add(hCourse);
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				showAllCourses();
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses != null;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Show All Courses";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				for (int i = 1; i < iTable.getRowCount(); i++)
					iTable.setSelected(i, !((CurriculaCourseSelectionBox)iTable.getWidget(i, 1)).getCourse().isEmpty());				
			}
			@Override
			public boolean isApplicable() {
				return iEditable;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Select All Courses";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				for (int row = iTable.getRowCount() - 1; row > 0; row --) {
					if (!iTable.isSelected(row)) continue;
					String course = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
					if (course.isEmpty() && row + 1 == iTable.getRowCount()) {
						iTable.setSelected(row, false);
						continue;
					}
					iTable.removeRow(row);
				}
			}
			@Override
			public boolean isApplicable() {
				return iEditable && iTable.getSelectedCount() > 0 && iVisibleCourses == null;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Remove Selected Courses";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				for (int i = 1; i < iTable.getRowCount(); i++)
					iTable.setSelected(i, false);
			}
			@Override
			public boolean isApplicable() {
				return iEditable && iTable.getSelectedCount() > 0;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Clear Selection";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				setPercent(!CurriculumCookie.getInstance().getCurriculaCoursesPercent());
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return true;
			}
			@Override
			public String getName() {
				return "Show " + (CurriculumCookie.getInstance().getCurriculaCoursesPercent() ? "Numbers" : "Percentages");
			}
		});
		for (final Mode m: Mode.values()) {
			hCourse.addOperation(new Operation() {
				@Override
				public void execute() {
					setMode(m);
				}
				@Override
				public boolean isApplicable() {
					return CurriculumCookie.getInstance().getCurriculaCoursesMode() != m;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return (m == Mode.NONE ? "Hide " + CurriculumCookie.getInstance().getCurriculaCoursesMode().getName() : "Show " + m.getName());
				}
			});
		}
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				updateEnrollmentsAndLastLike(iLastCourses, true);
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null && iLastCourses != null;
			}
			@Override
			public boolean hasSeparator() {
				return true;
			}
			@Override
			public String getName() {
				return "Show Empty Courses";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				// boolean selectedOnly = (iTable.getSelectedCount() > 0);
				rows: for (int row = iTable.getRowCount() - 1; row > 0; row --) {
					String course = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
					if (course.isEmpty() && row + 1 == iTable.getRowCount()) continue;
					/*
					if (selectedOnly && !iTable.isSelected(row)) {
						iTable.setSelected(row, false);
						continue;
					}
					*/
					for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
						int x = 2 + 2 * c;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, x);
						if (text.getShare() != null) continue rows;
					}
					iTable.removeRow(row);
				}
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null;
			}
			@Override
			public boolean hasSeparator() {
				return iLastCourses == null;
			}
			@Override
			public String getName() {
				return "Hide Empty Courses";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				boolean selectedOnly = (iTable.getSelectedCount() > 0);
				for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
					int x = 2 + 2 * c;
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, x);
						text.setShare(null);
					}
				}
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null && iEditable;
			}
			@Override
			public boolean hasSeparator() {
				return true;
			}
			@Override
			public String getName() {
				return "Clear Requested Enrollments (All Classifications" + (iTable.getSelectedCount() > 0 ? ", Selected Courses Only" : "") + ")";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				boolean selectedOnly = (iTable.getSelectedCount() > 0);
				for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
					int x = 2 + 2 * c;
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, x);
						EnrollmentLabel label = (EnrollmentLabel)iTable.getWidget(row, x + 1);
						if (CurriculumCookie.getInstance().getCurriculaCoursesPercent())
							text.setShare(label.getLastLikePercent());
						else
							text.setExpected(label.getLastLike());
					}
				}
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null && iEditable && CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.LAST;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Copy Last-Like &rarr; Requested (All Classifications" + (iTable.getSelectedCount() > 0 ? ", Selected Courses Only" : "") + ")";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				boolean selectedOnly = (iTable.getSelectedCount() > 0);
				for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
					int x = 2 + 2 * c;
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, x);
						EnrollmentLabel label = (EnrollmentLabel)iTable.getWidget(row, x + 1);
						if (CurriculumCookie.getInstance().getCurriculaCoursesPercent())
							text.setShare(label.getEnrollmentPercent());
						else
							text.setExpected(label.getEnrollment());
					}
				}
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null && iEditable && CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.ENRL;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Copy Current &rarr; Requested (All Classifications" + (iTable.getSelectedCount() > 0 ? ", Selected Courses Only" : "") + ")";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public void execute() {
				boolean selectedOnly = (iTable.getSelectedCount() > 0);
				for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
					int x = 2 + 2 * c;
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, x);
						EnrollmentLabel label = (EnrollmentLabel)iTable.getWidget(row, x + 1);
						if (CurriculumCookie.getInstance().getCurriculaCoursesPercent())
							text.setShare(label.getProjectionPercent());
						else
							text.setExpected(label.getProjection());
					}
				}
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null && iEditable && CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.PROJ;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Copy Projection &rarr; Requested (All Classifications" + (iTable.getSelectedCount() > 0 ? ", Selected Courses Only" : "") + ")";
			}
		});
		hCourse.addOperation(new Operation() {
			@Override
			public String getName() {
				return "Sort by Course";
			}
			@Override
			public boolean hasSeparator() {
				return true;
			}
			@Override
			public boolean isApplicable() {
				return iVisibleCourses == null;
			}
			@Override
			public void execute() {
				iTable.sortByRow(new Comparator<Integer>() {
					public int compare(Integer a, Integer b) {
						return compareTwoRows(1, a, b);
					}
				});
			}
		});
		
		CurriculaCourseSelectionBox.CourseFinderDialogHandler fx = new CurriculaCourseSelectionBox.CourseFinderDialogHandler() {
			@Override
			public void onOpen(CurriculaCourseSelectionBox.CourseFinderDialogEvent e) {
				iTable.clearHover();
			}
		};
		
		int col = 2;
		for (final AcademicClassificationInterface clasf: iClassifications.getClassifications()) {
			UniTimeTableHeader hExp = new UniTimeTableHeader(clasf.getCode(), HasHorizontalAlignment.ALIGN_RIGHT);
			header.add(hExp);
			final int expCol = col++;
			hExp.addOperation(new Operation() {
				@Override
				public void execute() {
					for (int i = 1; i < iTable.getRowCount(); i++)
						iTable.setSelected(i, !((ShareTextBox)iTable.getWidget(i, expCol)).getText().isEmpty());
				}
				@Override
				public boolean isApplicable() {
					return iEditable;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Select All";
				}
			});
			hExp.addOperation(new Operation() {
				@Override
				public void execute() {
					for (int i = 1; i < iTable.getRowCount(); i++)
						iTable.setSelected(i, false);
				}
				@Override
				public boolean isApplicable() {
					return iEditable && iTable.getSelectedCount() > 0;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Clear Selection";
				}
			});
			hExp.addOperation(new Operation() {
				@Override
				public void execute() {
					setPercent(!CurriculumCookie.getInstance().getCurriculaCoursesPercent());
				}
				@Override
				public boolean isApplicable() {
					return true;
				}
				@Override
				public boolean hasSeparator() {
					return true;
				}
				@Override
				public String getName() {
					return "Show " + (CurriculumCookie.getInstance().getCurriculaCoursesPercent() ? "Numbers" : "Percentages");
				}
			});
			for (final Mode m: Mode.values()) {
				hExp.addOperation(new Operation() {
					@Override
					public void execute() {
						setMode(m);
					}
					@Override
					public boolean isApplicable() {
						return CurriculumCookie.getInstance().getCurriculaCoursesMode() != m;
					}
					@Override
					public boolean hasSeparator() {
						return false;
					}
					@Override
					public String getName() {
						return (m == Mode.NONE ? "Hide " + CurriculumCookie.getInstance().getCurriculaCoursesMode().getName() : "Show " + m.getName());
					}
				});
			}
			hExp.addOperation(new Operation() {
				@Override
				public void execute() {
					boolean selectedOnly = (iTable.getSelectedCount() > 0);
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, expCol);
						text.setShare(null);
					}
				}
				@Override
				public boolean isApplicable() {
					return iVisibleCourses == null && iEditable;
				}
				@Override
				public boolean hasSeparator() {
					return true;
				}
				@Override
				public String getName() {
					return "Clear Requested Enrollments" + (iTable.getSelectedCount() > 0 ? " (Selected Courses Only)" : "");
				}
			});
			hExp.addOperation(new Operation() {
				@Override
				public void execute() {
					boolean selectedOnly = (iTable.getSelectedCount() > 0);
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, expCol);
						EnrollmentLabel label = (EnrollmentLabel)iTable.getWidget(row, expCol + 1);
						if (CurriculumCookie.getInstance().getCurriculaCoursesPercent())
							text.setShare(label.getLastLikePercent());
						else
							text.setExpected(label.getLastLike());
					}
				}
				@Override
				public boolean isApplicable() {
					return iVisibleCourses == null && iEditable && CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.LAST;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Copy Last-Like &rarr; Requested" + (iTable.getSelectedCount() > 0 ? " (Selected Courses Only)" : "");
				}
			});
			hExp.addOperation(new Operation() {
				@Override
				public void execute() {
					boolean selectedOnly = (iTable.getSelectedCount() > 0);
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, expCol);
						EnrollmentLabel label = (EnrollmentLabel)iTable.getWidget(row, expCol + 1);
						if (CurriculumCookie.getInstance().getCurriculaCoursesPercent())
							text.setShare(label.getEnrollmentPercent());
						else
							text.setExpected(label.getEnrollment());
					}
				}
				@Override
				public boolean isApplicable() {
					return iVisibleCourses == null && iEditable && CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.ENRL;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Copy Current &rarr; Requested" + (iTable.getSelectedCount() > 0 ? " (Selected Courses Only)" : "");
				}
			});
			hExp.addOperation(new Operation() {
				@Override
				public void execute() {
					boolean selectedOnly = (iTable.getSelectedCount() > 0);
					for (int row = 1; row < iTable.getRowCount(); row ++) {
						if (selectedOnly && !iTable.isSelected(row)) continue;
						ShareTextBox text = (ShareTextBox)iTable.getWidget(row, expCol);
						EnrollmentLabel label = (EnrollmentLabel)iTable.getWidget(row, expCol + 1);
						if (CurriculumCookie.getInstance().getCurriculaCoursesPercent())
							text.setShare(label.getProjectionPercent());
						else
							text.setExpected(label.getProjection());
					}
				}
				@Override
				public boolean isApplicable() {
					return iVisibleCourses == null && iEditable && CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.PROJ;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Copy Projection &rarr; Requested" + (iTable.getSelectedCount() > 0 ? " (Selected Courses Only)" : "");
				}
			});
			hExp.addOperation(new Operation() {
				@Override
				public String getName() {
					return "Sort by " + clasf.getCode();
				}
				@Override
				public boolean hasSeparator() {
					return true;
				}
				@Override
				public boolean isApplicable() {
					return iVisibleCourses == null;
				}
				@Override
				public void execute() {
					iTable.sortByRow(new Comparator<Integer>() {
						public int compare(Integer a, Integer b) {
							return compareTwoRows(expCol, a, b);
						}
					});
				}
			});
			
			UniTimeTableHeader hCmp = new UniTimeTableHeader(CurriculumCookie.getInstance().getCurriculaCoursesMode().getAbbv(), HasHorizontalAlignment.ALIGN_CENTER);
			header.add(hCmp);
			final int cmpCol = col++;
			hCmp.addOperation(new Operation() {
				@Override
				public void execute() {
					for (int i = 1; i < iTable.getRowCount(); i++)
						iTable.setSelected(i, !((EnrollmentLabel)iTable.getWidget(i, cmpCol)).getText().isEmpty());
				}
				@Override
				public boolean isApplicable() {
					return iEditable;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Select All";
				}
			});
			hCmp.addOperation(new Operation() {
				@Override
				public void execute() {
					for (int i = 1; i < iTable.getRowCount(); i++)
						iTable.setSelected(i, false);
				}
				@Override
				public boolean isApplicable() {
					return iEditable && iTable.getSelectedCount() > 0;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Clear Selection";
				}
			});
			hCmp.addOperation(new Operation() {
				@Override
				public void execute() {
					setPercent(!CurriculumCookie.getInstance().getCurriculaCoursesPercent());
				}
				@Override
				public boolean isApplicable() {
					return true;
				}
				@Override
				public boolean hasSeparator() {
					return true;
				}
				@Override
				public String getName() {
					return "Show " + (CurriculumCookie.getInstance().getCurriculaCoursesPercent() ? "Numbers" : "Percentages");
				}
			});
			for (final Mode m: Mode.values()) {
				hCmp.addOperation(new Operation() {
					@Override
					public void execute() {
						setMode(m);
					}
					@Override
					public boolean isApplicable() {
						return CurriculumCookie.getInstance().getCurriculaCoursesMode() != m;
					}
					@Override
					public boolean hasSeparator() {
						return false;
					}
					@Override
					public String getName() {
						return (m == Mode.NONE ? "Hide " + CurriculumCookie.getInstance().getCurriculaCoursesMode().getName() : "Show " + m.getName());
					}
				});
			}
			hCmp.addOperation(new Operation() {
				@Override
				public String getName() {
					return "Sort by " + clasf.getCode() + " " + CurriculumCookie.getInstance().getCurriculaCoursesMode().getName();
				}
				@Override
				public boolean hasSeparator() {
					return true;
				}
				@Override
				public boolean isApplicable() {
					return iVisibleCourses == null;
				}
				@Override
				public void execute() {
					iTable.sortByRow(new Comparator<Integer>() {
						public int compare(Integer a, Integer b) {
							return compareTwoRows(cmpCol, a, b);
						}
					});
				}
			});
		}
		iTable.addRow(null, header);
		
		// body
		if (curriculum.hasCourses()) {
			for (CourseInterface course: curriculum.getCourses()) {
				List<Widget> line = new ArrayList<Widget>();
				HorizontalPanel hp = new HorizontalPanel();
				line.add(hp);
				
				if (course.hasGroups()) {
					for (CurriculumCourseGroupInterface g: course.getGroups()) {
						Group gr = null;
						for (Group x: iGroups) {
							if (x.getName().equals(g.getName())) { gr = x; break; }
						}
						if (gr == null) {
							gr = new Group(g.getName(), g.getType());
							if (g.getColor() != null) {
								gr.setColor(g.getColor());
							} else {
								colors: for (String c: sColors) {
									for (Group x: iGroups) {
										if (x.getColor().equals(c)) continue colors;
									}
									gr.setColor(c);
									break;
								}
								if (gr.getColor() == null) gr.setColor(sColors[0]);
							}
							iGroups.add(gr);
						}
						hp.add(gr.cloneGroup());
					}
				}
				
				CurriculaCourseSelectionBox cx = new CurriculaCourseSelectionBox(course.getId().toString());
				cx.setCourse(course.getCourseName(), false);
				cx.setWidth("130px");
				cx.addCourseFinderDialogHandler(fx);
				cx.addCourseSelectionChangeHandler(iCourseChangedHandler);
				if (!iEditable) cx.setEnabled(false);
				line.add(cx);
				
				for (col = 0; col < iClassifications.getClassifications().size(); col++) {
					CurriculumCourseInterface cci = course.getCurriculumCourse(col);
					ShareTextBox ex = new ShareTextBox(col, cci == null ? null : cci.getShare());
					if (!iEditable) ex.setReadOnly(true);
					line.add(ex);
					EnrollmentLabel note = new EnrollmentLabel(col, cci == null ? null : cci.getEnrollment(), cci == null ? null : cci.getLastLike(), cci == null ? null : cci.getProjection());
					line.add(note);
				}
				iTable.addRow(course.getCourseName(), line);
			}
		}
		if (iEditable) addBlankLine();
	}
	
	public boolean saveCurriculum(CurriculumInterface c) {
		boolean ret = true;
		HashSet<String> courses = new HashSet<String>();
		HashMap<String, CurriculumCourseGroupInterface> groups = new HashMap<String, CurriculumCourseGroupInterface>();
		if (c.hasCourses()) c.getCourses().clear();
		for (int row = 1; row < iTable.getRowCount(); row++) {
			String course = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
			if (course.isEmpty()) continue;
			if (!courses.add(course)) {
				((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).setError("Duplicate course " + course);
				ret = false;
				continue;
			}
			CourseInterface cr = new CourseInterface();
			cr.setCourseName(course);
			for (int i = 0; i < iClassifications.getClassifications().size(); i++) {
				Float share = ((ShareTextBox)iTable.getWidget(row, 2 + 2 * i)).getShare();
				if (share == null) continue;
				Integer lastLike = ((EnrollmentLabel)iTable.getWidget(row, 3 + 2 * i)).iLastLike;
				CurriculumCourseInterface cx = new CurriculumCourseInterface();
				cx.setShare(share);
				cx.setLastLike(lastLike);
				cx.setCurriculumClassificationId(iClassifications.getClassifications().get(i).getId());
				cr.setCurriculumCourse(i, cx);
			}
			if (!cr.hasCurriculumCourses()) continue;
			HorizontalPanel hp = (HorizontalPanel)iTable.getWidget(row, 0);
			for (int i = 0; i < hp.getWidgetCount(); i++) {
				Group g = (Group)hp.getWidget(i);
				CurriculumCourseGroupInterface gr = groups.get(g.getName());
				if (gr == null) {
					gr = new CurriculumCourseGroupInterface();
					gr.setName(g.getName());
					gr.setType(g.getType());
					gr.setColor(g.getColor());
					groups.put(g.getName(), gr);
				}
				cr.addGroup(gr);
			}
			c.addCourse(cr);
		}
		return ret;
	}
	
	public void addBlankLine() {
		List<Widget> line = new ArrayList<Widget>();

		HorizontalPanel hp = new HorizontalPanel();
		line.add(hp);

		CurriculaCourseSelectionBox cx = new CurriculaCourseSelectionBox(null);
		cx.setWidth("130px");
		cx.addCourseSelectionChangeHandler(iCourseChangedHandler);
		cx.addCourseFinderDialogHandler(new CurriculaCourseSelectionBox.CourseFinderDialogHandler() {
			@Override
			public void onOpen(CurriculaCourseSelectionBox.CourseFinderDialogEvent e) {
				iTable.clearHover();
			}
		});
		if (!iEditable) cx.setEnabled(false);
		line.add(cx);
		
		for (int col = 0; col < iClassifications.getClassifications().size(); col++) {
			ShareTextBox ex = new ShareTextBox(col, null);
			if (!iEditable) ex.setReadOnly(true);
			line.add(ex);
			EnrollmentLabel note = new EnrollmentLabel(col, null, null, null);
			line.add(note);
		}
		
		int row = iTable.addRow("", line);
		iTable.getRowFormatter().addStyleName(row, "unitime-NoPrint");
		if (iVisibleCourses != null) iTable.getRowFormatter().setVisible(row, false);
		for (int col = 0; col < line.size(); col++)
			if (!iTable.getCellFormatter().isVisible(0, col))
				iTable.getCellFormatter().setVisible(row, col, false);
	}
	
	private int compareTwoRows(int column, int r0, int r1) {
		boolean e1 = ((CurriculaCourseSelectionBox)iTable.getWidget(r0, 1)).getCourse().isEmpty();
		boolean e2 = ((CurriculaCourseSelectionBox)iTable.getWidget(r1, 1)).getCourse().isEmpty();
		if (e1 && !e2) return 1;
		if (e2 && !e1) return -1;
		if (column == 0) {
			HorizontalPanel p0 = (HorizontalPanel)iTable.getWidget(r0, 0);
			HorizontalPanel p1 = (HorizontalPanel)iTable.getWidget(r1, 0);
			TreeSet<Group> g0 = new TreeSet<Group>();
			TreeSet<Group> g1 = new TreeSet<Group>();
			for (int i = 0; i < p0.getWidgetCount(); i++) g0.add((Group)p0.getWidget(i));
			for (int i = 0; i < p1.getWidgetCount(); i++) g1.add((Group)p1.getWidget(i));
			Iterator<Group> i0 = g0.iterator();
			Iterator<Group> i1 = g1.iterator();
			while (i0.hasNext() || i1.hasNext()) {
				if (!i0.hasNext()) return 1;
				if (!i1.hasNext()) return -1;
				int cmp = i0.next().compareTo(i1.next());
				if (cmp != 0) return cmp;
			}
			return compareTwoRows(2, r0, r1);
		}
		if (column == 1)
			return ((CurriculaCourseSelectionBox)iTable.getWidget(r0, 1)).getCourse().compareTo(((CurriculaCourseSelectionBox)iTable.getWidget(r1, 1)).getCourse());
		if (column % 2 == 0) {
			Float s0 = ((ShareTextBox)iTable.getWidget(r0, column)).getShare();
			Float s1 = ((ShareTextBox)iTable.getWidget(r1, column)).getShare();
			return - (s0 == null ? new Float(0) : s0).compareTo(s1 == null ? new Float(0) : s1);
		} else {
			EnrollmentLabel l0 = ((EnrollmentLabel)iTable.getWidget(r0, column));
			EnrollmentLabel l1 = ((EnrollmentLabel)iTable.getWidget(r1, column));
			Mode mode = CurriculumCookie.getInstance().getCurriculaCoursesMode();
			Integer i0 = (mode == Mode.ENRL ? l0.iEnrollment : mode == Mode.LAST ? l0.iLastLike : l0.iProjection);
			Integer i1 = (mode == Mode.ENRL ? l1.iEnrollment : mode == Mode.LAST ? l1.iLastLike : l1.iProjection);
			return - (i0 == null ? new Integer(0) : i0).compareTo(i1 == null ? new Integer(0) : i1);
		}
	}
	
	public int getCourseIndex(String course) {
		for (int row = 1; row < iTable.getRowCount(); row++) {
			String c = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
			if (course.equals(c)) return row - 1;
		}
		return -1;
	}
	
	public boolean setEnrollmentAndLastLike(String course, int clasf, Integer enrollment, Integer lastLike, Integer projection) {
		boolean changed = false;
		for (int row = 1; row < iTable.getRowCount(); row++) {
			String c = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
			if (!course.equals(c)) continue;
			EnrollmentLabel note = ((EnrollmentLabel)iTable.getWidget(row, 3 + 2 * clasf));
			note.iEnrollment = enrollment;
			note.iLastLike = lastLike;
			note.iProjection = projection;
			note.update();
			changed = true;
		}
		return changed;
	}
	
	public void updateEnrollmentsAndLastLike(HashMap<String, CurriculumStudentsInterface[]> courses, boolean showEmptyCourses) {
		iLastCourses = courses;
		rows: for (int row = 1; row < iTable.getRowCount() - 1; ) {
			for (int col = 0; col < iClassifications.getClassifications().size(); col ++) {
				ShareTextBox text = (ShareTextBox)iTable.getWidget(row, 2 + 2 * col);
				if (!text.getText().isEmpty()) {
					row ++;
					continue rows;
				}
			}
			iTable.removeRow(row);
		}
		HashSet<String> updated = new HashSet<String>();
		for (int row = 1; row < iTable.getRowCount(); row++) {
			String c = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
			if (c.isEmpty()) continue;
			updated.add(c);
			CurriculumStudentsInterface[] cc = courses.get(c);
			for (int col = 0; col < iClassifications.getClassifications().size(); col++) {
				EnrollmentLabel note = ((EnrollmentLabel)iTable.getWidget(row, 3 + 2 * col));
				note.iEnrollment = (cc == null || cc[col] == null ? null : cc[col].getEnrollment());
				note.iLastLike = (cc == null || cc[col] == null ? null : cc[col].getLastLike());
				note.iProjection = (cc == null || cc[col] == null ? null : cc[col].getProjection());
				note.update();
			}
		}
		CurriculumStudentsInterface[] total = courses.get("");
		if (total == null) return;
		int totalEnrollment = 0, totalLastLike = 0;
		for (int i = 0; i < total.length; i++) {
			if (total[i] != null) totalEnrollment += total[i].getEnrollment();
			if (total[i] != null) totalLastLike += total[i].getLastLike();
		}
		TreeSet<Map.Entry<String, CurriculumStudentsInterface[]>> include = new TreeSet<Map.Entry<String,CurriculumStudentsInterface[]>>(new Comparator<Map.Entry<String,CurriculumStudentsInterface[]>>() {
			/*
			private int highestClassification(CurriculumStudentsInterface[] a) {
				int best = a.length;
				int bestVal = -1;
				for (int i = 0; i < a.length; i++) {
					if (a[i] == null) continue;
					if (a[i].getEnrollment() > bestVal) {
						bestVal = a[i].getEnrollment(); best = i;
					}
					if (a[i].getLastLike() > bestVal) {
						bestVal = a[i].getLastLike(); best = i;
					}
				}
				return best;
			}
			*/
			private int firstClassification(CurriculumStudentsInterface[] a) {
				for (int i = 0; i < a.length; i++) {
					if (a[i] == null) continue;
					if (a[i].getEnrollment() > 0) return i;
					if (a[i].getLastLike() > 0) return i;
					if (a[i].getProjection() > 0) return i;
				}
				return a.length;
			}
			public int compare(Map.Entry<String,CurriculumStudentsInterface[]> c0, Map.Entry<String,CurriculumStudentsInterface[]> c1) {
				/*
				int a0 = highestClassification(c0.getValue());
				int a1 = highestClassification(c1.getValue());
				if (a0 < a1) return -1;
				if (a0 > a1) return 1;
				if (a0 < c0.getValue().length) {
					int v0 = (c0.getValue()[a0][0] == null ? 0 : c0.getValue()[a0][0]);
					int v1 = (c1.getValue()[a0][0] == null ? 0 : c1.getValue()[a0][0]);
					int w0 = (c0.getValue()[a0][1] == null ? 0 : c0.getValue()[a0][1]);
					int w1 = (c1.getValue()[a0][1] == null ? 0 : c1.getValue()[a0][1]);
					if (v0 < v1 || w0 < w1) return -1;
					if (v0 > v1 || w0 > w1) return 1;
				}
				*/
				int b0 = firstClassification(c0.getValue());
				int b1 = firstClassification(c1.getValue());
				if (b0 < b1) return -1;
				if (b0 > b1) return 1;
				while (b0 < c0.getValue().length) {
					int v0 = (c0.getValue()[b0] == null ? 0 : c0.getValue()[b0].getEnrollment());
					int v1 = (c1.getValue()[b0] == null ? 0 : c1.getValue()[b0].getEnrollment());
					int w0 = (c0.getValue()[b0] == null ? 0 : c0.getValue()[b0].getLastLike());
					int w1 = (c1.getValue()[b0] == null ? 0 : c1.getValue()[b0].getLastLike());
					if (v0 > v1 || w0 > w1) return -1;
					if (v0 < v1 || w0 < w1) return 1;
					b0++;
				}
				return c0.getKey().compareTo(c1.getKey());
			}
		});
		for (Map.Entry<String, CurriculumStudentsInterface[]> course: courses.entrySet()) {
			if (updated.contains(course.getKey()) || course.getKey().isEmpty()) continue;
			CurriculumStudentsInterface[] cc = course.getValue();
			int enrollment = 0, lastLike = 0;
			for (int i = 0; i < cc.length; i++) {
				if (cc[i] != null) enrollment += cc[i].getEnrollment();
				if (cc[i] != null) lastLike += cc[i].getLastLike();
			}
			if ((totalEnrollment > 0 && 100.0f * enrollment / totalEnrollment > 3.0f) ||
				(totalLastLike > 0 && 100.0f * lastLike / totalLastLike > 3.0f)) {
				include.add(course);
			}
		}
		if (showEmptyCourses)
			for (Map.Entry<String, CurriculumStudentsInterface[]> course: include) {
				CurriculumStudentsInterface[] cc = course.getValue();
				int row = iTable.getRowCount() - 1;
				if (!iEditable) row++;
				addBlankLine();
				CurriculaCourseSelectionBox c = (CurriculaCourseSelectionBox)iTable.getWidget(row, 1);
				c.setCourse(course.getKey(), false);
				iTable.getRowFormatter().removeStyleName(row, "unitime-NoPrint");
				for (int col = 0; col < iClassifications.getClassifications().size(); col++) {
					EnrollmentLabel note = ((EnrollmentLabel)iTable.getWidget(row, 3 + 2 * col));
					note.iEnrollment = (cc == null || cc[col] == null ? null : cc[col].getEnrollment());
					note.iLastLike = (cc == null || cc[col] == null ? null : cc[col].getLastLike());
					note.iProjection = (cc == null || cc[col] == null ? null : cc[col].getProjection());
					note.update();
				}
				if (iVisibleCourses!=null) {
					if (iVisibleCourses.contains(course.getKey())) {
						((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).setEnabled(false);
						iTable.getRowFormatter().setVisible(row, true);
					} else {
						iTable.getRowFormatter().setVisible(row, false);
					}
				}
			}
	}
	
	public void expectedChanged(int col, int expected) {
		if (!CurriculumCookie.getInstance().getCurriculaCoursesPercent()) {
			for (int row = 1; row < iTable.getRowCount(); row++) {
				((ShareTextBox)iTable.getWidget(row, 2 + 2 * col)).update();
			}
		}
	}
	
	private void setPercent(boolean percent) {
		if (CurriculumCookie.getInstance().getCurriculaCoursesPercent() == percent) return;
		CurriculumCookie.getInstance().setCurriculaCoursesPercent(percent);
		for (int row = 1; row < iTable.getRowCount(); row++) {
			for (int col = 0; col < iClassifications.getClassifications().size(); col++) {
				((ShareTextBox)iTable.getWidget(row, 2 + 2 * col)).update();
				((EnrollmentLabel)iTable.getWidget(row, 3 + 2 * col)).update();
			}
		}
	}
	
	private void setMode(Mode mode) {
		CurriculumCookie.getInstance().setCurriculaCoursesMode(mode);
		for (int col = 0; col < iClassifications.getClassifications().size(); col++) {
			((HTML)iTable.getWidget(0, 3 + 2 * col)).setHTML(mode.getAbbv());
		}
		for (int row = 1; row < iTable.getRowCount(); row++) {
			for (int col = 0; col < iClassifications.getClassifications().size(); col++) {
				((EnrollmentLabel)iTable.getWidget(row, 3 + 2 * col)).update();
			}
		}
	}
	
	public void setVisible(int col, boolean visible) {
		for (int row = 0; row < iTable.getRowCount(); row++) {
			iTable.getFlexCellFormatter().setVisible(row, 2 + 2 * col, visible);
			iTable.getFlexCellFormatter().setVisible(row, 3 + 2 * col, visible);
		}
	}
	
	public class EnrollmentLabel extends Label implements HasCellAlignment {
		private int iColumn;
		private Integer iEnrollment, iLastLike, iProjection;
		
		public EnrollmentLabel(int column, Integer enrollment, Integer lastLike, Integer projection) {
			super();
			setStyleName("unitime-Label");
			iColumn = column;
			iEnrollment = enrollment;
			iLastLike = lastLike;
			iProjection = projection;
			update();
		}
		
		public void update() {
			switch (CurriculumCookie.getInstance().getCurriculaCoursesMode()) {
			case NONE: // None
				setText("");
				break;
			case ENRL: // Enrollment
				if (iEnrollment == null || iEnrollment == 0) {
					setText("");
				} else if (CurriculumCookie.getInstance().getCurriculaCoursesPercent()) {
					Integer total = iClassifications.getEnrollment(iColumn);
					setText(total == null ? "N/A" : NF.format(100.0 * iEnrollment / total) + "%");
				} else {
					setText(iEnrollment.toString());
				}
				break;
			case LAST: // Last-like
				if (iLastLike == null || iLastLike == 0) {
					setText("");
				} else if (CurriculumCookie.getInstance().getCurriculaCoursesPercent()) {
					Integer total = iClassifications.getLastLike(iColumn);
					setText(total == null ? "N/A" : NF.format(100.0 * iLastLike / total) + "%");
				} else {
					setText(iLastLike.toString());
				}
				break;
			case PROJ: // Projection
				if (iProjection == null || iProjection == 0) {
					setText("");
				} else if (CurriculumCookie.getInstance().getCurriculaCoursesPercent()) {
					Integer total = iClassifications.getProjection(iColumn);
					setText(total == null ? "N/A" : NF.format(100.0 * iProjection / total) + "%");
				} else {
					setText(iProjection.toString());
				}
				break;
			}
		}
		
		public Integer getLastLike() { return (iLastLike == null || iLastLike == 0 ? null : iLastLike); }
		
		public Integer getEnrollment() { return (iEnrollment == null || iEnrollment == 0 ? null : iEnrollment); }

		public Integer getProjection() { return (iProjection == null || iProjection == 0 ? null : iProjection); }

		public Float getLastLikePercent() { 
			if (iLastLike == null || iLastLike == 0) return null;
			Integer total = iClassifications.getLastLike(iColumn);
			if (total == null) return null;
			return ((float)iLastLike) / total;
		}
		
		public Float getEnrollmentPercent() { 
			if (iEnrollment == null || iEnrollment == 0) return null;
			Integer total = iClassifications.getEnrollment(iColumn);
			if (total == null) return null;
			return ((float)iEnrollment) / total;
		}

		public Float getProjectionPercent() { 
			if (iProjection == null || iProjection == 0) return null;
			Integer total = iClassifications.getProjection(iColumn);
			if (total == null) return null;
			return ((float)iProjection) / total;
		}

		@Override
		public HorizontalAlignmentConstant getCellAlignment() {
			return HasHorizontalAlignment.ALIGN_RIGHT;
		}
	}

	public class ShareTextBox extends UniTimeTextBox {
		private int iColumn;
		private Float iShare = null;
		
		public ShareTextBox(int column, Float share) {
			super(6, ValueBoxBase.TextAlignment.RIGHT);
			iColumn = column;
			iShare = share;
			addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					try {
						if (getText().isEmpty()) {
							iShare = null;
						} else if (getText().endsWith("%")) {
							iShare = Float.valueOf(getText().substring(0, getText().length() - 1)) / 100.0f;
							if (iShare > 1.0f) iShare = 1.0f;
							if (iShare <= 0.0f) iShare = null;
						} else {
							iShare = Float.valueOf(getText()) / iClassifications.getExpected(iColumn);
							if (iShare > 1.0f) iShare = 1.0f;
							if (iShare <= 0.0f) iShare = null;
						}
					} catch (Exception e) {
						iShare = null;
					}
					update();
				}
			});
			update();
		}
		
		public void setShare(Float share) {
			iShare = share;
			update();
		}
		
		public void setExpected(Integer expected) {
			if (expected == null) {
				iShare = null;
			} else {
				Integer total = iClassifications.getExpected(iColumn);
				if (total == null) {
					iShare = null;
				} else {
					iShare = ((float)expected) / total;
				}
			}
			update();
		}
		
		public Float getShare() {
			return iShare;
		}
		
		public void update() {
			if (iShare == null) 
				setText("");
			else if (CurriculumCookie.getInstance().getCurriculaCoursesPercent())
				setText(NF.format(100.0 * iShare) + "%");
			else {
				Integer exp = iClassifications.getExpected(iColumn);
				setText(exp == null ? "N/A" : String.valueOf(Math.round(exp * iShare)));	
			}
		}
	}
	
	public static class CourseChangedEvent {
		private String iCourseName = null;
		public CourseChangedEvent(String courseName) {
			iCourseName = courseName;
		}
		public String getCourseName() { return iCourseName; }
	}
	
	public class Group extends Label implements Comparable<Group> {
		private String iName;
		private int iType;
		private String iColor;
		private Operation iOperation;
		
		public Group(String name, int type) {
			super(name, false);
			iName = name;
			iType = type;
			setStylePrimaryName("unitime-TinyLabel" + (iType == 1 ? "White" : ""));
			if (iEditable) {
				addClickHandler(iNewGroupDialog.getClickHandler());
				getElement().getStyle().setCursor(Cursor.POINTER);
			}
			iOperation = new Operation() {
				@Override
				public String getName() {
					return DOM.toString(getElement());
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}

				@Override
				public boolean isApplicable() {
					return iEditable && iVisibleCourses == null;
				}

				@Override
				public void execute() {
					assignGroup(null, iName, iType);
				}
			};
		}
		public String getName() { return iName; }
		public int getType() { return iType; }
		public void setType(int type) {
			iType = type;
			setStylePrimaryName("unitime-TinyLabel" + (iType == 1 ? "White" : ""));
		}
		public void setName(String name) {
			iName = name;
			setText(name);
		}
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Group)) return false;
			return getName().equals(((Group)o).getName());
		}
		public Group cloneGroup() {
			Group g = new Group(iName, iType);
			g.setColor(getColor());
			return g;
		}
		public String getColor() {
			return iColor;
		}
		public void setColor(String color) {
			iColor = color;
			addStyleName(color);
		}
		public int compareTo(Group g) {
			return getName().compareTo(g.getName());
		}
		
		public Operation getOperation() { return iOperation; }
	}
	
	public void assignGroup(String oldName, String name, int type) {
		Group g = null;
		for (Group x: iGroups) {
			if (x.getName().equals(oldName == null ? name : oldName)) { g = x; break; }
		}
		if (g == null) {
			if (name == null || name.isEmpty()) return;
			g = new Group(name, type);
			colors: for (String c: sColors) {
				for (Group x: iGroups) {
					if (x.getColor().equals(c)) continue colors;
				}
				g.setColor(c);
				break;
			}
			iGroups.add(g);
		} else {
			rows: for (int row = 1; row < iTable.getRowCount(); row++ ) {
				HorizontalPanel p = (HorizontalPanel)iTable.getWidget(row, 0);
				for (int i = 0; i < p.getWidgetCount(); i++) {
					Group x = (Group)p.getWidget(i);
					if (x.equals(g)) {
						if (name == null || name.isEmpty()) {
							p.remove(i);
							continue rows;
						} else {
							x.setName(name); x.setType(type);
						}
					}
				}
			}
			if (name == null || name.isEmpty()) {
				iGroups.remove(g);
				return;
			} else {
				g.setName(name);
				g.setType(type);
			}
		}
		if (oldName != null) return;
		boolean nothing = true;
		boolean hasNoGroup = false;
		rows: for (int row = 1; row < iTable.getRowCount(); row++ ) {
			if (!iTable.isSelected(row)) continue;
			nothing = false;
			HorizontalPanel p = (HorizontalPanel)iTable.getWidget(row, 0);
			for (int i = 0; i < p.getWidgetCount(); i++) {
				Group x = (Group)p.getWidget(i);
				if (x.equals(g)) continue rows;
			}
			hasNoGroup = true;
			break;
		}
		if (nothing) {
			boolean select = false;
			for (int row = 1; row < iTable.getRowCount(); row++ ) {
				HorizontalPanel p = (HorizontalPanel)iTable.getWidget(row, 0);
				for (int i = 0; i < p.getWidgetCount(); i++) {
					Group x = (Group)p.getWidget(i);
					if (x.equals(g)) {
						iTable.setSelected(row, true);
						select = true;
					}
				}
			}
			if (select) return;
		}
		rows: for (int row = 1; row < iTable.getRowCount(); row++ ) {
			if (!iTable.isSelected(row)) continue;
			iTable.setSelected(row, false);
			HorizontalPanel p = (HorizontalPanel)iTable.getWidget(row, 0);
			for (int i = 0; i < p.getWidgetCount(); i++) {
				Group x = (Group)p.getWidget(i);
				if (x.equals(g)) {
					if (!hasNoGroup) p.remove(i);
					continue rows;
				}
			}
			p.add(g.cloneGroup());
		}
		boolean found = false;
		rows: for (int row = 1; row < iTable.getRowCount(); row++ ) {
			HorizontalPanel p = (HorizontalPanel)iTable.getWidget(row, 0);
			for (int i = 0; i < p.getWidgetCount(); i++) {
				Group x = (Group)p.getWidget(i);
				if (x.equals(g)) {
					found = true; break rows;
				}
			}
		}
		if (!found) iGroups.remove(g);
	}
	
	public List<Group> getGroups() { return iGroups; }
	
	public void showOnlyCourses(TreeSet<CourseInterface> courses) {
		iVisibleCourses = new TreeSet<String>();
		for (CourseInterface c: courses) iVisibleCourses.add(c.getCourseName());
		for (int row = 1; row < iTable.getRowCount(); row++) {
			String courseName = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
			if (iVisibleCourses.contains(courseName)) {
				((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).setEnabled(false);
				iTable.getRowFormatter().setVisible(row, true);
			} else {
				iTable.getRowFormatter().setVisible(row, false);
			}
		}
	}
	
	public void showAllCourses() {
		if (iVisibleCourses != null) {
			for (int i = 1; i < iTable.getRowCount(); i++) {
				String courseName = ((CurriculaCourseSelectionBox)iTable.getWidget(i, 1)).getCourse();
				iTable.setSelected(i, iVisibleCourses.contains(courseName));
			}
		}
		iVisibleCourses = null;
		for (int row = 1; row < iTable.getRowCount(); row++) {
			((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).setEnabled(true);
			iTable.getRowFormatter().setVisible(row, true);
		}
	}
	
	public boolean canShowStudentsTable(int row) {
		if (CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.NONE) return false;
		if (row < 1 || row >= iTable.getRowCount()) return false;
		String course = ((CurriculaCourseSelectionBox)iTable.getWidget(row, 1)).getCourse();
		if (iLastCourses == null || !iLastCourses.containsKey(course)) return false;
		int nrOther = 0;
		for (int r = 1; r < iTable.getRowCount(); r ++) {
			if (r == row || !iTable.isSelected(r)) continue;
			nrOther ++;
		}
		return (nrOther > 0);
	}
	
	public class StudentsTable extends Composite {
		private FlexTable iT = new FlexTable();
		private VerticalPanel iP = new VerticalPanel();
		private boolean iCanShow = false;
		
		private int count(CurriculumStudentsInterface c, Set<Long> students) {
			if (CurriculumCookie.getInstance().getCurriculaCoursesMode() != Mode.PROJ || c == null) return students.size();
			return c.countProjectedStudents(students);
		}
		
		private StudentsTable(int currentRow) {
			super();
			
			String course = ((CurriculaCourseSelectionBox)iTable.getWidget(currentRow, 1)).getCourse();
			
			iP.add(new Label("Comparing " + course + " " + CurriculumCookie.getInstance().getCurriculaCoursesMode().getName().toLowerCase().replace(" enrollment", "") + " students with the other selected courses:"));
			iP.add(iT);
			initWidget(iP);
			
			if (iLastCourses == null) return;
			CurriculumStudentsInterface[] thisCourse = iLastCourses.get(course);
			CurriculumStudentsInterface[] totals = iLastCourses.get("");
			if (thisCourse == null) return;
			
			int column = 0;
			for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
				if (iClassifications.getExpected(c) == null) continue;
				iT.setText(0, 1 + column, iClassifications.getName(c));
				iT.getCellFormatter().setWidth(0, 1 + column, "50px");
				iT.getCellFormatter().setStyleName(0, 1 + column, "unitime-DashedBottom");
				column++;
			}
			
			iT.setText(1, 0, "Students in at least 1 other course");
			iT.setText(2, 0, "Students in at least 2 other courses");
			iT.setText(3, 0, "Students in at least 3 other courses");
			iT.setText(4, 0, "Students in all other courses");
			iT.setText(5, 0, "Students not in any other course");
			int row = 0;
			List<CurriculumStudentsInterface[]> other = new ArrayList<CurriculumStudentsInterface[]>();
			for (int r = 1; r < iTable.getRowCount(); r ++) {
				if (r == currentRow || !iTable.isSelected(r)) continue;
				String c = ((CurriculaCourseSelectionBox)iTable.getWidget(r, 1)).getCourse();
				if (c.isEmpty()) continue;
				other.add(iLastCourses.get(c));
				iT.setText(6 + row, 0, "Students shared with " + c);
				row++;
			}

			column = 0;
			int total = 0;
			int totalC[] = new int [other.size()];
			for (int i = 0; i < totalC.length; i++)
				totalC[i] = 0;
			boolean has1 = false, has2 = false, has3 = false, hasAll = false, hasNone = false;
			for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
				CurriculumStudentsInterface tc = totals[c];
				if (iClassifications.getExpected(c) == null) continue;
				Set<Long> thisEnrollment = (thisCourse[c] == null ? null : (CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.ENRL ? thisCourse[c].getEnrolledStudents() : thisCourse[c].getLastLikeStudents()));
				if (thisEnrollment != null && count(tc,thisEnrollment) != 0) {
					total += thisEnrollment.size();
					Set<Long> sharedWithOneOther = new HashSet<Long>();
					Set<Long> sharedWithTwoOther = new HashSet<Long>();
					Set<Long> sharedWithThreeOther = new HashSet<Long>();
					Set<Long> sharedWithAll = new HashSet<Long>(thisEnrollment);
					Set<Long> notShared = new HashSet<Long>(thisEnrollment);
					row = 0;
					for (CurriculumStudentsInterface[] o: other) {
						Set<Long> enrl = (o == null || o[c] == null ? null : CurriculumCookie.getInstance().getCurriculaCoursesMode() == Mode.ENRL  ? o[c].getEnrolledStudents() : o[c].getLastLikeStudents());
						if (enrl == null) {
							sharedWithAll.clear();
							row++;
							continue;
						}
						Set<Long> share = new HashSet<Long>();
						for (Long s: thisEnrollment) {
							if (enrl.contains(s)) {
								if (!sharedWithOneOther.add(s))
									if (!sharedWithTwoOther.add(s))
										sharedWithThreeOther.add(s);
								share.add(s);
							}
						}
						for (Iterator<Long> i = sharedWithAll.iterator(); i.hasNext(); )
							if (!enrl.contains(i.next())) i.remove();
						for (Iterator<Long> i = notShared.iterator(); i.hasNext(); )
							if (enrl.contains(i.next())) i.remove();
						if (!share.isEmpty() && count(tc, share) != 0) {
							totalC[row] += share.size();
							iT.setText(6 + row, 1 + column, (CurriculumCookie.getInstance().getCurriculaCoursesPercent() ? NF.format(100.0 * count(tc, share) / count(tc,thisEnrollment)) + "%" : "" + count(tc,share)));
						}
						row++;
					}
					boolean percent = CurriculumCookie.getInstance().getCurriculaCoursesPercent();
					if (!sharedWithOneOther.isEmpty() && count(tc,sharedWithOneOther) != 0) {
						iT.setText(1, 1 + column, (percent ? NF.format(100.0 * count(tc,sharedWithOneOther) / count(tc,thisEnrollment)) + "%" : "" + count(tc,sharedWithOneOther)));
						has1 = true;
					}
					if (!sharedWithTwoOther.isEmpty() && count(tc,sharedWithTwoOther) != 0) {
						iT.setText(2, 1 + column, (percent ? NF.format(100.0 * count(tc,sharedWithTwoOther) / count(tc,thisEnrollment)) + "%" : "" + count(tc,sharedWithTwoOther)));
						has2 = true;
					}
					if (!sharedWithThreeOther.isEmpty() && count(tc,sharedWithThreeOther) != 0) {
						iT.setText(3, 1 + column, (percent ? NF.format(100.0 * count(tc,sharedWithThreeOther) / count(tc,thisEnrollment)) + "%" : "" + count(tc,sharedWithThreeOther)));
						has3 = true;
					}
					if (!sharedWithAll.isEmpty() && count(tc,sharedWithAll) != 0) {
						iT.setText(4, 1 + column, (percent ? NF.format(100.0 * count(tc,sharedWithAll) / count(tc,thisEnrollment)) + "%" : "" + count(tc,sharedWithAll)));
						hasAll = true;
					}
					if (!notShared.isEmpty() && count(tc,notShared) != 0) {
						iT.setText(5, 1 + column, (percent ? NF.format(100.0 * count(tc,notShared) / count(tc,thisEnrollment)) + "%" : "" + count(tc,notShared)));
						hasNone = true;
					}
				}
				column ++;
			}
			if (!has1 || other.size() == 1) iT.getRowFormatter().setVisible(1, false);
			if (!has2 || other.size() == 1) iT.getRowFormatter().setVisible(2, false);
			if (!has3 || other.size() == 1) iT.getRowFormatter().setVisible(3, false);
			if (!hasAll || other.size() <= 3) iT.getRowFormatter().setVisible(4, false);
			if (!hasNone || other.size() == 1) iT.getRowFormatter().setVisible(5, false);
			if (other.size() > 1) {
				int minTotal = -1;
				List<Integer> visible = new ArrayList<Integer>();
				for (row = other.size() - 1; row >= 0; row--) {
					if (totalC[row] < 1)
						iT.getRowFormatter().setVisible(6 + row, false);
					else {
						visible.add(row);
						if (minTotal < 0 || minTotal < totalC[row])
							minTotal = totalC[row];
					}
				}
				while (visible.size() > 10) {
					int limit = minTotal; minTotal = -1;
					for (Iterator<Integer> i = visible.iterator(); i.hasNext() && visible.size() > 10; ) {
						row = i.next();
						if (totalC[row] <= limit) {
							iT.getRowFormatter().setVisible(6 + row, false);
							i.remove();
						} else {
							if (minTotal < 0 || minTotal < totalC[row])
								minTotal = totalC[row];
						}
					}
				}
				if (!visible.isEmpty()) {
					int r = 6 + visible.get(visible.size() - 1);
					int col = 1;
					for (int c = 0; c < iClassifications.getClassifications().size(); c++) {
						if (iClassifications.getExpected(c) == null) continue;
						if (iT.getCellCount(r) <= col || iT.getText(r, col) == null || iT.getText(r, col).isEmpty()) iT.setHTML(r, col, "&nbsp;");
						iT.getCellFormatter().setStyleName(r, col, "unitime-DashedTop");
						col++;
					}
				}

			}
						
			iCanShow = has1 || has2 || hasAll || hasNone;
		}
		
		public boolean canShow() { return iCanShow; }
		
	}
	
	private class GroupDialogBox extends UniTimeDialogBox {
		private TextBox iGrName;
		private ListBox iGrType;
		private Button iGrAssign, iGrDelete, iGrUpdate;
		private String iGrOldName = null;
		private ClickHandler iGrHandler;

		private GroupDialogBox() {
			super(true, true);
			FlexTable groupTable = new FlexTable();
			groupTable.setCellSpacing(2);
			groupTable.setText(0, 0, "Name:");
			iGrName = new UniTimeTextBox();
			groupTable.setWidget(0, 1, iGrName);
			groupTable.setText(1, 0, "Type:");
			iGrType = new ListBox();
			iGrType.addItem("No conflict (different students)");
			iGrType.addItem("Conflict (same students)");
			iGrType.setSelectedIndex(0);
			groupTable.setWidget(1, 1, iGrType);
			HorizontalPanel grButtons = new HorizontalPanel();
			grButtons.setSpacing(2);
			iGrAssign = new Button("Assign");
			grButtons.add(iGrAssign);
			iGrUpdate = new Button("Update");
			grButtons.add(iGrUpdate);
			iGrDelete = new Button("Delete");
			grButtons.add(iGrDelete);
			groupTable.setWidget(2, 1, grButtons);
			groupTable.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasHorizontalAlignment.ALIGN_RIGHT);
			setWidget(groupTable);
			
			iGrAssign.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					hide();
					assignGroup(iGrOldName, iGrName.getText(), iGrType.getSelectedIndex());
				}
			});
			
			setEscapeToHide(true);
			setEnterToSubmit(new Command() {
				@Override
				public void execute() {
					hide();
					assignGroup(iGrOldName, iGrName.getText(), iGrType.getSelectedIndex());
				}
			});
			/*
			iGrName.addKeyUpHandler(new KeyUpHandler() {
				@Override
				public void onKeyUp(KeyUpEvent event) {
					if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
						hide();
						assignGroup(iGrOldName, iGrName.getText(), iGrType.getSelectedIndex());
					}
					if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
						hide();
					}
				}
			});
			*/
			
			iGrUpdate.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					hide();
					assignGroup(iGrOldName, iGrName.getText(), iGrType.getSelectedIndex());
				}
			});
			
			iGrDelete.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					hide();
					assignGroup(iGrOldName, null, iGrType.getSelectedIndex());
				}
			});
			
			iGrHandler = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					setText("Edit group");
					iGrOldName = ((Group)event.getSource()).getName();
					iGrName.setText(((Group)event.getSource()).getText());
					iGrType.setSelectedIndex(((Group)event.getSource()).getType());
					iGrAssign.setVisible(false);
					iGrDelete.setVisible(true);
					iGrUpdate.setVisible(true);
					Scheduler.get().scheduleDeferred(new ScheduledCommand() {
						@Override
						public void execute() {
							iGrName.setFocus(true);
							iGrName.selectAll();
						}
					});
					iTable.clearHover();
					event.stopPropagation();
					center();
				}
			};
		}
		
		public ClickHandler getClickHandler() {
			return iGrHandler;
		}
		
		public void openNew() {
			setText("New group");
			iGrOldName = null;
			iGrName.setText(String.valueOf((char)('A' + getGroups().size())));
			iGrType.setSelectedIndex(0);
			iGrAssign.setVisible(true);
			iGrDelete.setVisible(false);
			iGrUpdate.setVisible(false);
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					iGrName.setFocus(true);
					iGrName.selectAll();
				}
			});
			center();
		}
	}
}
