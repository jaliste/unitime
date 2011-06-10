/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2011, UniTime LLC, and individual contributors
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
package org.unitime.timetable.gwt.client.reservations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasCellAlignment;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasColSpan;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.HasStyleName;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.MouseClickListener;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.TableEvent;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader.Operation;
import org.unitime.timetable.gwt.resources.GwtResources;
import org.unitime.timetable.gwt.services.ReservationService;
import org.unitime.timetable.gwt.services.ReservationServiceAsync;
import org.unitime.timetable.gwt.shared.ReservationInterface;
import org.unitime.timetable.gwt.shared.ReservationInterface.Clazz;
import org.unitime.timetable.gwt.shared.ReservationInterface.Config;
import org.unitime.timetable.gwt.shared.ReservationInterface.Course;
import org.unitime.timetable.gwt.shared.ReservationInterface.CourseReservation;
import org.unitime.timetable.gwt.shared.ReservationInterface.Area;
import org.unitime.timetable.gwt.shared.ReservationInterface.CurriculumReservation;
import org.unitime.timetable.gwt.shared.ReservationInterface.GroupReservation;
import org.unitime.timetable.gwt.shared.ReservationInterface.IdName;
import org.unitime.timetable.gwt.shared.ReservationInterface.IndividualReservation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class ReservationTable extends Composite {
	public static final GwtResources RESOURCES =  GWT.create(GwtResources.class);
	private final ReservationServiceAsync iReservationService = GWT.create(ReservationService.class);
	private static DateTimeFormat sDF = DateTimeFormat.getFormat("MM/dd/yyyy");
	private Long iOfferingId = null;
	
	private VerticalPanel iReservationPanel;
	private Image iOpenCloseImage, iLoadingImage;
	private UniTimeTable<ReservationInterface> iReservations;
	private Label iErrorLabel;
	
	private AsyncCallback<List<ReservationInterface>> iLoadCallback = null;
	
	private List<ReservationClickHandler> iReservationClickHandlers = new ArrayList<ReservationClickHandler>();
	
	private String iLastQuery = null;

	public ReservationTable(boolean editable, boolean showHeader) {
		iReservationPanel = new VerticalPanel();
		iReservationPanel.setWidth("100%");
		
		if (showHeader) {
			HorizontalPanel header = new HorizontalPanel();
			iOpenCloseImage = new Image(ReservationCookie.getInstance().getReservationCoursesDetails() ? RESOURCES.treeOpen() : RESOURCES.treeClosed());
			iOpenCloseImage.getElement().getStyle().setCursor(Cursor.POINTER);
			iOpenCloseImage.setVisible(false);
			header.add(iOpenCloseImage);
			Label curriculaLabel = new Label("Reservations", false);
			curriculaLabel.setStyleName("unitime3-HeaderTitle");
			curriculaLabel.getElement().getStyle().setPaddingLeft(2, Unit.PX);
			header.add(curriculaLabel);
			header.setCellWidth(curriculaLabel, "100%");
			if (editable) {
				Button add = new Button("Add&nbsp;<u>R</u>eservation");
				add.setAccessKey('r');
				add.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						ToolBox.open("gwt.jsp?page=reservation&offering=" + iOfferingId);
					}
				});
				add.getElement().getStyle().setMarginBottom(2, Unit.PX);
				header.add(add);
			}
			header.setStyleName("unitime3-HeaderPanel");
			iReservationPanel.add(header);
			
			iOpenCloseImage.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					ReservationCookie.getInstance().setReservationCoursesDetails(!ReservationCookie.getInstance().getReservationCoursesDetails());
					iOpenCloseImage.setResource(ReservationCookie.getInstance().getReservationCoursesDetails() ? RESOURCES.treeOpen() : RESOURCES.treeClosed());
					if (iReservations.getRowCount() > 2) {
						for (int row = 1; row < iReservations.getRowCount() - 1; row++) {
							iReservations.getRowFormatter().setVisible(row, ReservationCookie.getInstance().getReservationCoursesDetails());
						}
					}
				}
			});
		}

		iLoadingImage = new Image(RESOURCES.loading_small());
		iLoadingImage.setVisible(false);
		iLoadingImage.getElement().getStyle().setMarginTop(10, Unit.PX);
		iReservationPanel.add(iLoadingImage);
		iReservationPanel.setCellHorizontalAlignment(iLoadingImage, HasHorizontalAlignment.ALIGN_CENTER);
		iReservationPanel.setCellVerticalAlignment(iLoadingImage, HasVerticalAlignment.ALIGN_MIDDLE);
		
		iReservations = new UniTimeTable<ReservationInterface>();
		iReservationPanel.add(iReservations);
		
		iErrorLabel = new Label("Oooops, something went wrong.");
		iErrorLabel.setStyleName("unitime-ErrorMessage");
		iReservationPanel.add(iErrorLabel);
		iErrorLabel.setVisible(false);
				
		initWidget(iReservationPanel);
		
		if (editable) {
			iReservations.addMouseClickListener(new MouseClickListener<ReservationInterface>() {
				@Override
				public void onMouseClick(TableEvent<ReservationInterface> event) {
					if (event.getData() != null && event.getData().isEditable()) {
						ReservationClickedEvent e = new ReservationClickedEvent(event.getData());
						for (ReservationClickHandler h: iReservationClickHandlers) {
							h.onClick(e);
						}
					}
				}
			});
		}
	}
	
	private void initCallbacks() {
		if (iLoadCallback == null) {
			iLoadCallback = new AsyncCallback<List<ReservationInterface>>() {
				@Override
				public void onFailure(Throwable caught) {
					setErrorMessage("Failed to load reservations (" + caught.getMessage() + ").");
					iLoadingImage.setVisible(false);
				}
				@Override
				public void onSuccess(List<ReservationInterface> result) {
					if (result.isEmpty()) {
						setMessage("The selected offering has no reservations.");
					} else {
						populate(result);
						if (iReservations.getRowCount() > 2) {
							for (int row = 1; row < iReservations.getRowCount() - 1; row++) {
								iReservations.getRowFormatter().setVisible(row, ReservationCookie.getInstance().getReservationCoursesDetails());
							}
						}
						iLoadingImage.setVisible(false);
						iOpenCloseImage.setVisible(true);
					}
					iLoadingImage.setVisible(false);
				}
			};			
		}
	}
	
	private void refresh() {
		clear(true);
		if (iOfferingId != null) {
			iReservationService.getReservations(iOfferingId, iLoadCallback);
		} else {
			query(iLastQuery, null);
		}
	}
	
	private void clear(boolean loading) {
		for (int row = iReservations.getRowCount() - 1; row >= 0; row--) {
			iReservations.removeRow(row);
		}
		iReservations.clear(true);
		iLoadingImage.setVisible(loading);
		iErrorLabel.setVisible(false);
	}


	private void populate(List<ReservationInterface> reservations) {
		List<UniTimeTableHeader> header = new ArrayList<UniTimeTableHeader>();
		
		if (iOfferingId == null) {
			UniTimeTableHeader hOffering = new UniTimeTableHeader("Instructional<br>Offering");
			hOffering.setWidth("100px");
			header.add(hOffering);
			hOffering.addOperation(new Operation() {
				@Override
				public void execute() {
					iReservations.sort(new Comparator<ReservationInterface>() {
						@Override
						public int compare(ReservationInterface r1, ReservationInterface r2) {
							return r1.compareTo(r2);
						}
					});
				}
				@Override
				public boolean isApplicable() {
					return true;
				}
				@Override
				public boolean hasSeparator() {
					return false;
				}
				@Override
				public String getName() {
					return "Sort by Instructional Offering";
				}
			});
		}

		UniTimeTableHeader hType = new UniTimeTableHeader("Reservation<br>Type");
		hType.setWidth("100px");
		hType.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = new Integer(r1.getPriority()).compareTo(r2.getPriority());
						if (cmp != 0) return cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Reservation Type";
			}
		});
		header.add(hType);
		
		UniTimeTableHeader hOwner = new UniTimeTableHeader("Owner");
		hOwner.setWidth("250px");
		header.add(hOwner);
		hOwner.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = new Integer(r1.getPriority()).compareTo(r2.getPriority());
						if (cmp != 0) return cmp;
						cmp = r1.toString().compareTo(r2.toString());
						if (cmp != 0) return cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Owner";
			}
		});

		UniTimeTableHeader hRestrict = new UniTimeTableHeader("Restrictions");
		hRestrict.setWidth("160px");
		hRestrict.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = r1.getOffering().getAbbv().compareTo(r2.getOffering().getAbbv());
						if (cmp != 0) return cmp;
						cmp = r1.getConfigs().toString().compareTo(r2.getConfigs().toString());
						if (cmp != 0) return cmp;
						cmp = r1.getClasses().toString().compareTo(r2.getClasses().toString());
						if (cmp != 0) return cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Restrictions";
			}
		});
		header.add(hRestrict);

		UniTimeTableHeader hLimit = new UniTimeTableHeader("Reserved<br>Space");
		hLimit.setWidth("80px");
		header.add(hLimit);
		hLimit.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = new Integer(r1.getLimit() == null ? Integer.MAX_VALUE : r1.getLimit()).compareTo(r2.getLimit() == null ? Integer.MAX_VALUE : r2.getLimit());
						if (cmp != 0) return -cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Reserved Space";
			}
		});

		UniTimeTableHeader hLastLike = new UniTimeTableHeader("Last-Like<br>Enrollment");
		hLastLike.setWidth("80px");
		header.add(hLastLike);
		hLastLike.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = new Integer(r1.getLastLike() == null ? -1 : r1.getLastLike()).compareTo(r2.getLastLike() == null ? -1 : r2.getLastLike());
						if (cmp != 0) return -cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Last-Like Enrollment";
			}
		});

		UniTimeTableHeader hProjected = new UniTimeTableHeader("Projection<br>by Rule");
		hProjected.setWidth("80px");
		header.add(hProjected);
		hProjected.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = new Integer(r1.getProjection() == null ? -1 : r1.getProjection()).compareTo(r2.getProjection() == null ? -1 : r2.getProjection());
						if (cmp != 0) return -cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Projection by Rule";
			}
		});

		UniTimeTableHeader hEnrollment = new UniTimeTableHeader("Current<br>Enrollment");
		hEnrollment.setWidth("80px");
		header.add(hEnrollment);
		hEnrollment.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = new Integer(r1.getEnrollment() == null ? -1 : r1.getEnrollment()).compareTo(r2.getEnrollment() == null ? -1 : r2.getEnrollment());
						if (cmp != 0) return -cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Current Enrollment";
			}
		});
		
		UniTimeTableHeader hExpiration = new UniTimeTableHeader("Expiration<br>Date");
		hExpiration.setWidth("80px");
		header.add(hExpiration);
		hExpiration.addOperation(new Operation() {
			@Override
			public void execute() {
				iReservations.sort(new Comparator<ReservationInterface>() {
					@Override
					public int compare(ReservationInterface r1, ReservationInterface r2) {
						int cmp = new Long(r1.getExpirationDate() == null ? Long.MAX_VALUE : r1.getExpirationDate().getTime()).compareTo(r2.getExpirationDate() == null ? Long.MAX_VALUE : r2.getExpirationDate().getTime());
						if (cmp != 0) return cmp;
						return r1.compareTo(r2);
					}
				});
			}
			@Override
			public boolean isApplicable() {
				return true;
			}
			@Override
			public boolean hasSeparator() {
				return false;
			}
			@Override
			public String getName() {
				return "Sort by Expiration Date";
			}
		});
		
		iReservations.addRow(null, header);
		
		int total = 0, lastLike = 0, projection = 0, enrollment = 0;
		boolean unlimited = false;
		for (ReservationInterface reservation: reservations) {
			List<Widget> line = new ArrayList<Widget>();
			if (iOfferingId == null) {
				VerticalPanel courses = new VerticalPanel();
				courses.add(new Label(reservation.getOffering().getAbbv(), false));
				for (Course course: reservation.getOffering().getCourses()) {
					if (course.getAbbv().equals(reservation.getOffering().getAbbv())) continue;
					Label l = new Label(course.getAbbv(), false);
					l.getElement().getStyle().setMarginLeft(10, Unit.PX);
					l.getElement().getStyle().setColor("gray");
					courses.add(l);
				}
				if (!reservation.isEditable())
					courses.addStyleName("unitime-Disabled");
				line.add(courses);

			}
			
			Integer limit = reservation.getLimit();
			if (reservation instanceof CourseReservation) {
				line.add(new Label("Course"));
				Course course = ((CourseReservation) reservation).getCourse();
				limit = course.getLimit();
				line.add(new Label(course.getAbbv(), false));
			} else if (reservation instanceof IndividualReservation) {
				line.add(new Label("Individual"));
				VerticalPanel students = new VerticalPanel();
				limit = ((IndividualReservation) reservation).getStudents().size();
				for (IdName student: ((IndividualReservation) reservation).getStudents()) {
					students.add(new Label(student.getName(), false));
				}
				if (!reservation.isEditable())
					students.addStyleName("unitime-Disabled");
				line.add(students);
			} else if (reservation instanceof GroupReservation) {
				line.add(new Label("Student Group"));
				IdName group = ((GroupReservation) reservation).getGroup();
				line.add(new Label(group.getAbbv() + " - " + group.getName() + " (" + group.getLimit() + ")", false));				
			} else if (reservation instanceof CurriculumReservation) {
				line.add(new Label("Curriculum"));
				Area curriculum = ((CurriculumReservation) reservation).getCurriculum();
				VerticalPanel owner = new VerticalPanel();
				owner.add(new Label(curriculum.getAbbv() + " - " + curriculum.getName()));
				for (IdName clasf: curriculum.getClassifications()) {
					Label l = new Label(clasf.getAbbv() + " - " + clasf.getName());
					l.getElement().getStyle().setMarginLeft(10, Unit.PX);
					owner.add(l);
				}
				for (IdName major: curriculum.getMajors()) {
					Label l = new Label(major.getAbbv() + " - " + major.getName());
					l.getElement().getStyle().setMarginLeft(10, Unit.PX);
					owner.add(l);
				}
				if (!reservation.isEditable())
					owner.addStyleName("unitime-Disabled");
				line.add(owner);
			} else {
				line.add(new Label("Unknown"));
				line.add(new Label());
			}
			VerticalPanel restrictions = new VerticalPanel();
			for (Config config: reservation.getConfigs()) {
				restrictions.add(new Label("Configuration " + config.getName() + (config.getLimit() == null ? "" : " (" + config.getLimit() + ")") , false));
			}
			for (Clazz clazz: reservation.getClasses()) {
				restrictions.add(new Label(clazz.getName() + " (" + clazz.getLimit() + ")", false));
			}
			line.add(restrictions);
			if (!reservation.isEditable())
				restrictions.addStyleName("unitime-Disabled");
			line.add(new Number(limit == null ? "&infin;" : String.valueOf(limit)));
			if (limit == null)
				unlimited = true;
			else
				total += limit;
			
			if (reservation.getLastLike() != null) {
				line.add(new Number(reservation.getLastLike().toString()));
				lastLike += reservation.getLastLike();
			} else {
				line.add(new Label(""));
			}
			
			if (reservation.getProjection() != null) {
				line.add(new Number(reservation.getProjection().toString()));
				projection += reservation.getProjection();
			} else {
				line.add(new Label(""));
			}

			
			if (reservation.getEnrollment() != null) {
				line.add(new Number(reservation.getEnrollment().toString()));
				enrollment += reservation.getEnrollment();
			} else {
				line.add(new Label(""));
			}

			line.add(new Label(reservation.getExpirationDate() == null ? "" : sDF.format(reservation.getExpirationDate())));
			iReservations.addRow(reservation, line);
			iReservations.getRowFormatter().setVerticalAlign(iReservations.getRowCount() - 1, HasVerticalAlignment.ALIGN_TOP);
			if (!reservation.isEditable())
				iReservations.getRowFormatter().addStyleName(iReservations.getRowCount() - 1, "unitime-Disabled");
		}
		
		if (iOfferingId != null) {
			List<Widget> footer = new ArrayList<Widget>();
			footer.add(new TotalLabel("Total Reserved Space", 3)); 
			footer.add(new TotalNumber(unlimited ? "&infin;" : String.valueOf(total)));
			footer.add(new TotalNumber(lastLike <= 0 ? "" : String.valueOf(lastLike)));
			footer.add(new TotalNumber(projection <= 0 ? "" : String.valueOf(projection)));
			footer.add(new TotalNumber(enrollment <= 0 ? "" : String.valueOf(enrollment)));
			footer.add(new TotalLabel("&nbsp;", 1));
			iReservations.addRow(null, footer);
		} else if (reservations.isEmpty()) {
			setErrorMessage("No reservation matching the above filter found.");
		}
	}
	
	private static class Number extends HTML implements HasCellAlignment {
		public Number(String text) {
			super(text, false);
		}

		@Override
		public HorizontalAlignmentConstant getCellAlignment() {
			return HasHorizontalAlignment.ALIGN_RIGHT;
		}
	}
	
	private static class TotalNumber extends Number implements HasStyleName {
		public TotalNumber(String text) {
			super(text);
		}

		@Override
		public String getStyleName() {
			return "unitime-TotalRow";
		}
	}

	private static class TotalLabel extends HTML implements HasColSpan, HasStyleName {
		private int iColSpan;
		
		public TotalLabel(String text, int colspan) {
			super(text, false);
			iColSpan = colspan;
		}

		@Override
		public int getColSpan() {
			return iColSpan;
		}
		
		@Override
		public String getStyleName() {
			return "unitime-TotalRow";
		}
		
	}
	
	public void insert(final RootPanel panel) {
		initCallbacks();
		iOfferingId = Long.valueOf(panel.getElement().getInnerText());
		refresh();
		panel.getElement().setInnerText(null);
		panel.add(this);
		panel.setVisible(true);
		addReservationClickHandler(new ReservationClickHandler() {
			@Override
			public void onClick(ReservationClickedEvent evt) {
				ToolBox.open("gwt.jsp?page=reservation&id=" + evt.getReservation().getId());
				
			}
		});
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
	
	public void scrollIntoView(Long reservationId) {
		for (int r = 1; r < iReservations.getRowCount(); r++) {
			if (iReservations.getData(r) != null && iReservations.getData(r).getId().equals(reservationId)) {
				iReservations.getRowFormatter().getElement(r).scrollIntoView();
			}
		}
	}

	public static class ReservationClickedEvent {
		private ReservationInterface iReservation;
		
		public ReservationClickedEvent(ReservationInterface reservation) {
			iReservation = reservation;
		}
		
		public ReservationInterface getReservation() {
			return iReservation;
		}
	}
	
	public interface ReservationClickHandler {
		public void onClick(ReservationClickedEvent evt);
	}
	
	public void addReservationClickHandler(ReservationClickHandler h) {
		iReservationClickHandlers.add(h);
	}

	public void query(String filter, final Command next) {
		iLastQuery = filter;
		clear(true);
		setMessage(null);
		iReservationService.findReservations(filter, new AsyncCallback<List<ReservationInterface>>() {
			
			@Override
			public void onSuccess(List<ReservationInterface> result) {
				populate(result);
				iLoadingImage.setVisible(false);
				if (next != null)
					next.execute();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				iLoadingImage.setVisible(false);
				setErrorMessage("Unable to retrieve curricula (" + caught.getMessage() + ").");
				ToolBox.checkAccess(caught);
				if (next != null)
					next.execute();
			}
		});
	}
	
	public void select(Long curriculumId) {
		for (int i = 0; i < iReservations.getRowCount(); i++) {
			ReservationInterface r = iReservations.getData(i);
			if (r == null) continue;
			if (r.getId().equals(curriculumId))
				iReservations.getRowFormatter().setStyleName(i, "unitime-TableRowSelected");
			else if ("unitime-TableRowSelected".equals(iReservations.getRowFormatter().getStyleName(i)))
				iReservations.getRowFormatter().removeStyleName(i, "unitime-TableRowSelected");
				
		}
	}
}
