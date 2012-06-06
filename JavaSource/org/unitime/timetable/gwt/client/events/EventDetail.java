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
import java.util.List;

import org.unitime.timetable.gwt.client.events.EventAdd.EventPropertiesProvider;
import org.unitime.timetable.gwt.client.events.EventMeetingTable.EventMeetingRow;
import org.unitime.timetable.gwt.client.page.UniTimeNotifications;
import org.unitime.timetable.gwt.client.page.UniTimePageLabel;
import org.unitime.timetable.gwt.client.sectioning.EnrollmentTable;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.resources.GwtConstants;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.Enrollment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.EventInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ApproveEventRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.ContactInterface;
import org.unitime.timetable.gwt.shared.EventInterface.EventEnrollmentsRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;
import org.unitime.timetable.gwt.shared.EventInterface.MessageInterface;
import org.unitime.timetable.gwt.shared.EventInterface.NoteInterface;
import org.unitime.timetable.gwt.shared.EventInterface.RelatedObjectInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ResourceInterface;
import org.unitime.timetable.gwt.shared.EventInterface.SaveOrApproveEventRpcResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class EventDetail extends Composite {
	private static final GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	private static final GwtConstants CONSTANTS = GWT.create(GwtConstants.class);
	private static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	private static DateTimeFormat sTimeStampFormat = DateTimeFormat.getFormat(CONSTANTS.timeStampFormat());
	private EventInterface iEvent = null;
	
	private SimpleForm iForm;
	private UniTimeHeaderPanel iHeader, iFooter, iEnrollmentHeader;
	
	private UniTimeTable<ContactInterface> iContacts;
	private EventMeetingTable iMeetings;
	private UniTimeTable<NoteInterface> iNotes;
	private UniTimeTable<RelatedObjectInterface> iOwners;
	private EnrollmentTable iEnrollments;
	private ApproveDialog iApproveDialog;
	
	private EventPropertiesProvider iProperties;
	
	public EventDetail(EventPropertiesProvider properties) {
		iForm = new SimpleForm();
		iProperties = properties;
		
		iHeader = new UniTimeHeaderPanel();
		iHeader.addButton("edit", MESSAGES.buttonEdit(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				edit();
			}
		});
		iHeader.addButton("previous", MESSAGES.buttonPrevious(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				EventInterface prev = getPrevious(getEvent().getId());
				if (prev != null) previous(prev);
			}
		});
		iHeader.addButton("next", MESSAGES.buttonNext(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				EventInterface next = getNext(getEvent().getId());
				if (next != null) next(next);
			}
		});
		iHeader.addButton("back", MESSAGES.buttonBack(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});

		iContacts = new UniTimeTable<ContactInterface>();
		iContacts.setStyleName("unitime-EventContacts");
		
		List<Widget> contactHeader = new ArrayList<Widget>();
		contactHeader.add(new UniTimeTableHeader(MESSAGES.colName()));
		contactHeader.add(new UniTimeTableHeader(MESSAGES.colEmail()));
		contactHeader.add(new UniTimeTableHeader(MESSAGES.colPhone()));
		iContacts.addRow(null, contactHeader);
		
		iApproveDialog = new ApproveDialog() {
			@Override
			protected void onSubmit(final ApproveEventRpcRequest.Operation operation, List<EventMeetingRow> items, String message){
				switch (operation) {
				case APPROVE: LoadingWidget.getInstance().show(MESSAGES.waitForApproval(iEvent.getName())); break;
				case INQUIRE: LoadingWidget.getInstance().show(MESSAGES.waitForInquiry(iEvent.getName())); break;
				case REJECT: LoadingWidget.getInstance().show(MESSAGES.waitForRejection(iEvent.getName())); break;
				}
				List<MeetingInterface> meetings = new ArrayList<MeetingInterface>();
				for (EventMeetingRow item: items)
					meetings.add(item.getMeeting());
				RPC.execute(ApproveEventRpcRequest.createRequest(operation, iProperties.getSessionId(), iEvent, meetings, message), new AsyncCallback<SaveOrApproveEventRpcResponse>() {
					@Override
					public void onFailure(Throwable caught) {
						LoadingWidget.getInstance().hide();
						UniTimeNotifications.error(caught.getMessage());
					}

					@Override
					public void onSuccess(SaveOrApproveEventRpcResponse result) {
						LoadingWidget.getInstance().hide();
						if (result.hasMessages())
							for (MessageInterface m: result.getMessages()) {
								if (m.isError())
									UniTimeNotifications.warn(m.getMessage());
								else if (m.isWarning())
									UniTimeNotifications.error(m.getMessage());
								else
									UniTimeNotifications.info(m.getMessage());
							}
						switch (operation) {
						case APPROVE:
							onApprovalOrReject(iEvent.getId(), result.getEvent());
							setEvent(result.getEvent());
							break;
						case REJECT:
							onApprovalOrReject(iEvent.getId(), result.getEvent());
							if (result.hasEventWithId())
								setEvent(result.getEvent());
							else
								EventDetail.this.hide();
							break;
						case INQUIRE:
							setEvent(result.getEvent());
							break;
						}
					}
				});
			}
		};
		
		iMeetings = new EventMeetingTable(EventMeetingTable.Mode.MeetingsOfAnEvent, true);
		iMeetings.setOperation(EventMeetingTable.OperationType.Approve, iApproveDialog);
		iMeetings.setOperation(EventMeetingTable.OperationType.Reject, iApproveDialog);
		iMeetings.setOperation(EventMeetingTable.OperationType.Inquire, iApproveDialog);
		iMeetings.setEditable(false);
		
		iOwners = new UniTimeTable<RelatedObjectInterface>();
		iOwners.setStyleName("unitime-EventOwners");

		List<Widget> ownersHeader = new ArrayList<Widget>();
		ownersHeader.add(new UniTimeTableHeader(MESSAGES.colCourse()));
		ownersHeader.add(new UniTimeTableHeader(MESSAGES.colSection()));
		ownersHeader.add(new UniTimeTableHeader(MESSAGES.colType()));
		ownersHeader.add(new UniTimeTableHeader(MESSAGES.colDate()));
		ownersHeader.add(new UniTimeTableHeader(MESSAGES.colTime()));
		ownersHeader.add(new UniTimeTableHeader(MESSAGES.colLocation()));
		ownersHeader.add(new UniTimeTableHeader(MESSAGES.colInstructor()));
		iOwners.addRow(null, ownersHeader);
		
		iEnrollmentHeader = new UniTimeHeaderPanel(MESSAGES.sectEnrollments());
		iEnrollments = new EnrollmentTable(false, true);
		iEnrollments.getTable().setStyleName("unitime-Enrollments");
		
		iNotes = new UniTimeTable<NoteInterface>();
		iNotes.setStyleName("unitime-EventNotes");

		List<Widget> notesHeader = new ArrayList<Widget>();
		notesHeader.add(new UniTimeTableHeader(MESSAGES.colDate()));
		notesHeader.add(new UniTimeTableHeader(MESSAGES.colUser()));
		notesHeader.add(new UniTimeTableHeader(MESSAGES.colAction()));
		notesHeader.add(new UniTimeTableHeader(MESSAGES.colMeetings()));
		notesHeader.add(new UniTimeTableHeader(MESSAGES.colNote()));
		iNotes.addRow(null, notesHeader);
		
		iFooter = iHeader.clonePanel();
		
		initWidget(iForm);
	}
	
	private int iLastScrollTop, iLastScrollLeft;
	public void show() {
		UniTimePageLabel.getInstance().setPageName(MESSAGES.pageEventDetail());
		setVisible(true);
		iLastScrollLeft = Window.getScrollLeft();
		iLastScrollTop = Window.getScrollTop();
		onShow();
		Window.scrollTo(0, 0);
	}
	
	public void hide() {
		setVisible(false);
		onHide();
		Window.scrollTo(iLastScrollLeft, iLastScrollTop);
	}
	
	protected void onHide() {
	}
	
	protected void onShow() {
	}
	
	protected void edit() {
	}
	
	protected EventInterface getNext(Long eventId) { return null; }
	protected void next(EventInterface event) {}
	
	protected EventInterface getPrevious(Long eventId) { return null; }
	protected void previous(EventInterface previous) {}
	
	protected void onApprovalOrReject(Long eventId, EventInterface event) {}
	
	public void setEvent(EventInterface event) {
		iEvent = event;
		
		iApproveDialog.reset(iProperties.getProperties());
		
		iForm.clear();

		iHeader.clearMessage();
		iHeader.setHeaderTitle(iEvent.getName() + " (" + iEvent.getType().getName() + ")");
		iHeader.setEnabled("edit", iEvent.isCanEdit());
		iHeader.setEnabled("previous", getPrevious(iEvent.getId()) != null);
		iHeader.setEnabled("next", getNext(iEvent.getId()) != null);
		iForm.addHeaderRow(iHeader);
		
		iForm.addRow(MESSAGES.propEventType(), new Label(iEvent.getType().getName()));
		
		iContacts.clearTable(1);
		if (iEvent.hasContact()) {
			List<Label> row = new ArrayList<Label>();
			row.add(new Label(iEvent.getContact().getName(), false));
			row.add(new Label(iEvent.getContact().hasEmail() ? iEvent.getContact().getEmail() : "", false));
			row.add(new Label(iEvent.getContact().hasPhone() ? iEvent.getContact().getPhone() : "", false));
			int rowNum = iContacts.addRow(iEvent.getContact(), row);
			for (int col = 0; col < iContacts.getCellCount(rowNum); col++)
				iContacts.getCellFormatter().addStyleName(rowNum, col, "main-contact");
		}
		if (iEvent.hasAdditionalContacts()) {
			for (ContactInterface contact: iEvent.getAdditionalContacts()) {
				List<Label> row = new ArrayList<Label>();
				row.add(new Label(contact.getName(), false));
				row.add(new Label(contact.hasEmail() ? contact.getEmail() : "", false));
				row.add(new Label(contact.hasPhone() ? contact.getPhone() : "", false));
				int rowNum = iContacts.addRow(contact, row);
				for (int col = 0; col < iContacts.getCellCount(rowNum); col++)
					iContacts.getCellFormatter().addStyleName(rowNum, col, "additional-contact");

			}
		}
		if (iContacts.getRowCount() > 1)
			iForm.addRow(MESSAGES.propContacts(), iContacts);
		
		if (iEvent.hasEmail()) {
			iForm.addRow(MESSAGES.propAdditionalEmails(), new Label(iEvent.getEmail()));
		}

		if (iEvent.hasSponsor()) {
			iForm.addRow(MESSAGES.propSponsor(), new Label(iEvent.getSponsor().getName()));
		}

		if (iEvent.hasEnrollment()) {
			iForm.addRow(MESSAGES.propEnrollment(), new Label(String.valueOf(iEvent.getEnrollment().toString())));
			int r = iForm.addRow(MESSAGES.propStudentConflicts(), new Label(""));
			iForm.getRowFormatter().setVisible(r, false);
		}
		
		if (iEvent.hasMaxCapacity()) {
			iForm.addRow(MESSAGES.propAttendance(), new Label(iEvent.getMaxCapacity().toString()));
		}
		
		if (iEvent.hasLastChange()) {
			iForm.addRow(MESSAGES.propLastChange(), new Label(iEvent.getLastChange()));
		}
		
		iMeetings.clearTable(1);
		for (MeetingInterface meeting: iEvent.getMeetings()) {
			iMeetings.add(new EventMeetingRow(iEvent, meeting));
		}
		if (iMeetings.getRowCount() > 1) {
			iForm.addHeaderRow(MESSAGES.sectMeetings());
			iForm.addRow(iMeetings);
		}
		
		iNotes.clearTable(1);
		if (iEvent.hasNotes()) {
			for (NoteInterface note: iEvent.getNotes()) {
				List<Widget> row = new ArrayList<Widget>();
				row.add(new Label(sTimeStampFormat.format(note.getDate()), false));
				row.add(new HTML(note.getUser() == null ? "<i>N/A</i>" : note.getUser(), false));
				row.add(new Label(note.getType().getName()));
				row.add(new HTML(note.getMeetings() == null ? "<i>N/A</i>" : note.getMeetings(), false));
				row.add(new HTML(note.getNote() == null ? "" : note.getNote().replace("\n", "<br>"), true));
				int r = iNotes.addRow(note, row);
				iNotes.getRowFormatter().addStyleName(r, note.getType().getName().toLowerCase());
			}
		}
		if (iNotes.getRowCount() > 1) {
			iForm.addHeaderRow(MESSAGES.sectNotes());
			iForm.addRow(iNotes);
		}

		iOwners.clearTable(1);
		if (iEvent.hasRelatedObjects()) {
			for (RelatedObjectInterface obj: iEvent.getRelatedObjects()) {
				List<Widget> row = new ArrayList<Widget>();
				String course = "";
				if (obj.hasCourseNames()) {
					for (String cn: obj.getCourseNames()) {
						if (course.isEmpty()) {
							course += cn;
						} else {
							course += "<span class='cross-list'>" + cn + "</span>";
						}
					}
				} else {
					course = obj.getName();
				}
				row.add(new HTML(course, false));
				
				String section = "";
				if (obj.hasExternalIds()) {
					for (String ex: obj.getExternalIds()) {
						if (section.isEmpty()) {
							section += ex;
						} else {
							section += "<span class='cross-list'>" + ex + "</span>";
						}
					}
				} else if (obj.hasSectionNumber()) {
					section = obj.getSectionNumber();
				}
				row.add(new HTML(section, false));
				
				String type = (obj.hasInstruction() ? obj.getInstruction() : obj.getType().name());
				row.add(new Label(type, false));
				
				if (obj.hasDate()) {
					row.add(new Label(obj.getDate(), false));
				} else {
					row.add(new Label());
				}
				
				if (obj.hasTime()) {
					row.add(new Label(obj.getTime(), false));
				} else {
					row.add(new Label());
				}
				
				String location = "";
				if (obj.hasLocations()) {
					for (ResourceInterface loc: obj.getLocations()) {
						location += (location.isEmpty() ? "" : "<br>") + loc.getName();
					}
				}
				row.add(new HTML(location, false));

				if (obj.hasInstructors()) {
					row.add(new HTML(obj.getInstructorNames("<br>"), false));
				} else {
					row.add(new HTML());
				}
				
				int rowNumber = iOwners.addRow(obj, row);
				iOwners.getRowFormatter().addStyleName(rowNumber, "owner-row");
				for (int i = 0; i < iOwners.getCellCount(rowNumber); i++)
					iOwners.getCellFormatter().addStyleName(rowNumber, i, "owner-cell");
			}
		}
		if (iOwners.getRowCount() > 1) {
			iForm.addHeaderRow(MESSAGES.sectRelations());
			iForm.addRow(iOwners);
		}
		
		iEnrollments.clear();
		if (iEvent.hasEnrollment()) {
			final int enrollmentsRow = iForm.addHeaderRow(iEnrollmentHeader);
			iForm.addRow(iEnrollments.getTable());
			iEnrollmentHeader.showLoading();
			final Long eventId = iEvent.getId();
			RPC.execute(EventEnrollmentsRpcRequest.getEnrollmentsForEvent(eventId, iProperties.getSessionId()), new AsyncCallback<GwtRpcResponseList<ClassAssignmentInterface.Enrollment>>() {
				@Override
				public void onFailure(Throwable caught) {
					if (eventId.equals(iEvent.getId())) {
						iEnrollmentHeader.clearMessage();
						UniTimeNotifications.error(MESSAGES.failedNoEnrollments(caught.getMessage()));
						iForm.getRowFormatter().setVisible(enrollmentsRow, false);
						iForm.getRowFormatter().setVisible(enrollmentsRow + 1, false);
					}
				}

				@Override
				public void onSuccess(GwtRpcResponseList<Enrollment> result) {
					if (eventId.equals(iEvent.getId())) {
						if (result == null) result = new GwtRpcResponseList<Enrollment>();
						iEnrollmentHeader.clearMessage();
						iEnrollments.clear();
						iEnrollments.populate(result, false);
						int conf = 0;
						for (Enrollment e: result)
							if (e.hasConflict()) conf ++;
						if (conf != 0) {
							int row = iForm.getRow(MESSAGES.propStudentConflicts());
							((Label)iForm.getWidget(row, 1)).setText(String.valueOf(conf));
							iForm.getRowFormatter().setVisible(row, true);
						}
					}
				}
			});
		}

		iForm.addNotPrintableBottomRow(iFooter);
	}
	
	public EventInterface getEvent() { return iEvent; }

}
