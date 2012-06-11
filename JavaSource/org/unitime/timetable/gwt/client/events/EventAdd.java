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

import org.unitime.timetable.gwt.client.Lookup;
import org.unitime.timetable.gwt.client.events.AcademicSessionSelectionBox.AcademicSession;
import org.unitime.timetable.gwt.client.events.EventMeetingTable.EventMeetingRow;
import org.unitime.timetable.gwt.client.events.EventMeetingTable.OperationType;
import org.unitime.timetable.gwt.client.page.UniTimeNotifications;
import org.unitime.timetable.gwt.client.page.UniTimePageLabel;
import org.unitime.timetable.gwt.client.sectioning.EnrollmentTable;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.NumberBox;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader;
import org.unitime.timetable.gwt.client.widgets.UniTimeWidget;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.resources.GwtResources;
import org.unitime.timetable.gwt.shared.AcademicSessionProvider;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.Enrollment;
import org.unitime.timetable.gwt.shared.EventInterface;
import org.unitime.timetable.gwt.shared.EventInterface.EventType;
import org.unitime.timetable.gwt.shared.EventInterface.SaveOrApproveEventRpcResponse;
import org.unitime.timetable.gwt.shared.PersonInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ContactInterface;
import org.unitime.timetable.gwt.shared.EventInterface.EventPropertiesRpcResponse;
import org.unitime.timetable.gwt.shared.EventInterface.EventRoomAvailabilityRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.EventRoomAvailabilityRpcResponse;
import org.unitime.timetable.gwt.shared.EventInterface.EventEnrollmentsRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;
import org.unitime.timetable.gwt.shared.EventInterface.MessageInterface;
import org.unitime.timetable.gwt.shared.EventInterface.RelatedObjectInterface;
import org.unitime.timetable.gwt.shared.EventInterface.RelatedObjectLookupRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.RelatedObjectLookupRpcResponse;
import org.unitime.timetable.gwt.shared.EventInterface.ResourceInterface;
import org.unitime.timetable.gwt.shared.EventInterface.SaveEventRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.SelectionInterface;
import org.unitime.timetable.gwt.shared.EventInterface.SponsoringOrganizationInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class EventAdd extends Composite implements EventMeetingTable.Implementation, AcademicSessionSelectionBox.AcademicSessionFilter {
	private static final GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	private static final GwtResources RESOURCES = GWT.create(GwtResources.class);
	private static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	
	private String iMainExternalId = null;
	private UniTimeWidget<TextBox> iName;
	private NumberBox iLimit;
	private ListBox iSponsors;
	private UniTimeWidget<ListBox> iEventType;
	private TextArea iNotes, iEmails;
	private TextBox iMainFName, iMainMName, iMainPhone;
	private UniTimeWidget<TextBox> iMainLName, iMainEmail;
	private CheckBox iReqAttendance;
	
	private SimpleForm iCoursesForm;
	
	private SimpleForm iForm;
	private UniTimeHeaderPanel iHeader, iFooter, iMeetingsHeader;
	
	private EventMeetingTable iMeetings;
	
	private CourseRelatedObjectsTable iCourses;
	
	private AddMeetingsDialog iEventAddMeetings;
	private AcademicSessionSelectionBox iSession;
	private Lookup iLookup, iAdditionalLookup;
	private UniTimeTable<ContactInterface> iContacts;
	private int iContactRow;
	
	private EnrollmentTable iEnrollments;
	private UniTimeHeaderPanel iEnrollmentHeader;
	private int iEnrollmentRow;
	private Button iLookupButton, iAdditionalLookupButton;
	
	private EventInterface iEvent, iSavedEvent;
	private EventPropertiesProvider iProperties;
	private int iSessionRow = -1;
			
	public EventAdd(AcademicSessionSelectionBox session, EventPropertiesProvider properties) {
		iSession = session;
		iProperties = properties;
		iForm = new SimpleForm();
		
		iLookup = new Lookup();
		iLookup.addValueChangeHandler(new ValueChangeHandler<PersonInterface>() {
			@Override
			public void onValueChange(ValueChangeEvent<PersonInterface> event) {
				if (event.getValue() != null) {
					iMainExternalId = event.getValue().getId();
					iMainFName.setText(event.getValue().getFirstName() == null ? "" : event.getValue().getFirstName());
					iMainMName.setText(event.getValue().getMiddleName() == null ? "" : event.getValue().getMiddleName());
					iMainLName.getWidget().setText(event.getValue().getLastName() == null ? "" : event.getValue().getLastName());
					iMainPhone.setText(event.getValue().getPhone() == null ? "" : event.getValue().getPhone());
					iMainEmail.getWidget().setText(event.getValue().getEmail() == null ? "" : event.getValue().getEmail());
				}
			}
		});
		iAdditionalLookup = new Lookup();
		iAdditionalLookup.addValueChangeHandler(new ValueChangeHandler<PersonInterface>() {
			@Override
			public void onValueChange(ValueChangeEvent<PersonInterface> event) {
				if (event.getValue() != null) {
					final ContactInterface contact = new ContactInterface(event.getValue());
					List<Widget> row = new ArrayList<Widget>();
					row.add(new Label(contact.getName(), false));
					row.add(new Label(contact.hasEmail() ? contact.getEmail() : "", false));
					row.add(new Label(contact.hasPhone() ? contact.getPhone() : "", false));
					Image remove = new Image(RESOURCES.delete());
					remove.addStyleName("remove");
					remove.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							for (int row = 1; row < iContacts.getRowCount(); row ++)
								if (contact.equals(iContacts.getData(row))) {
									iContacts.removeRow(row);
									break;
								}
							iForm.getRowFormatter().setVisible(iContactRow, iContacts.getRowCount() > 1);
						}
					});
					row.add(remove);
					int rowNum = iContacts.addRow(contact, row);
					for (int col = 0; col < iContacts.getCellCount(rowNum); col++)
						iContacts.getCellFormatter().addStyleName(rowNum, col, "main-contact");
				}
				iForm.getRowFormatter().setVisible(iContactRow, iContacts.getRowCount() > 1);
			}
		});
		iLookup.setOptions("mustHaveExternalId" + (iSession.getAcademicSessionId() == null ? "" : ",session=" + iSession.getAcademicSessionId()));
		iAdditionalLookup.setOptions("mustHaveExternalId" + (iSession.getAcademicSessionId() == null ? "" : ",session=" + iSession.getAcademicSessionId()));
		iSession.addAcademicSessionChangeHandler(new AcademicSessionProvider.AcademicSessionChangeHandler() {
			@Override
			public void onAcademicSessionChange(AcademicSessionProvider.AcademicSessionChangeEvent event) {
				iLookup.setOptions("mustHaveExternalId,session=" + event.getNewAcademicSessionId());
				iAdditionalLookup.setOptions("mustHaveExternalId,session=" + event.getNewAcademicSessionId());
			}
		});
		
		
		iHeader = new UniTimeHeaderPanel(MESSAGES.sectEvent());
		ClickHandler clickCreateOrUpdate = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				iSavedEvent = null;
				validate(new AsyncCallback<Boolean>() {
					@Override
					public void onFailure(Throwable caught) {
						UniTimeNotifications.error(MESSAGES.failedValidation(caught.getMessage()));
					}
					@Override
					public void onSuccess(Boolean result) {
						if (result) {
							final EventInterface event = getEvent();
							LoadingWidget.getInstance().show(event.getId() == null ? MESSAGES.waitCreate(event.getName()) : MESSAGES.waitUpdate(event.getName()));
							RPC.execute(SaveEventRpcRequest.saveEvent(getEvent(), iSession.getAcademicSessionId(), getMessage()), new AsyncCallback<SaveOrApproveEventRpcResponse>() {

								@Override
								public void onFailure(Throwable caught) {
									LoadingWidget.getInstance().hide();
									String message = (event.getId() == null ? MESSAGES.failedCreate(event.getName(), caught.getMessage()) : MESSAGES.failedUpdate(event.getName(), caught.getMessage()));
									iHeader.setErrorMessage(message);
									UniTimeNotifications.error(message);
								}

								@Override
								public void onSuccess(SaveOrApproveEventRpcResponse result) {
									LoadingWidget.getInstance().hide();
									iSavedEvent = result.getEvent();
									if (result.hasMessages())
										for (MessageInterface m: result.getMessages()) {
											if (m.isError())
												UniTimeNotifications.warn(m.getMessage());
											else if (m.isWarning())
												UniTimeNotifications.error(m.getMessage());
											else
												UniTimeNotifications.info(m.getMessage());
										}
									hide();
								}
							});
						}
					}
				});
			}
		};
		iHeader.addButton("create", MESSAGES.buttonCreate(), 75, clickCreateOrUpdate);
		iHeader.addButton("update", MESSAGES.buttonUpdate(), 75, clickCreateOrUpdate);
		iHeader.addButton("delete", MESSAGES.buttonDelete(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				final EventInterface event = getEvent();
				if (event.hasMeetings()) event.getMeetings().clear();
				LoadingWidget.getInstance().show(MESSAGES.waitDelete(event.getName()));
				RPC.execute(SaveEventRpcRequest.saveEvent(event, iSession.getAcademicSessionId(), getMessage()), new AsyncCallback<SaveOrApproveEventRpcResponse>() {

					@Override
					public void onFailure(Throwable caught) {
						LoadingWidget.getInstance().hide();
						iHeader.setErrorMessage(MESSAGES.failedDelete(event.getName(), caught.getMessage()));
						UniTimeNotifications.error(MESSAGES.failedDelete(event.getName(), caught.getMessage()));
					}

					@Override
					public void onSuccess(SaveOrApproveEventRpcResponse result) {
						LoadingWidget.getInstance().hide();
						iSavedEvent = result.getEvent();
						if (result.hasMessages())
							for (MessageInterface m: result.getMessages()) {
								if (m.isError())
									UniTimeNotifications.warn(m.getMessage());
								else if (m.isWarning())
									UniTimeNotifications.error(m.getMessage());
								else
									UniTimeNotifications.info(m.getMessage());
							}
						hide();
					}
				});
			}
		});
		iHeader.addButton("back", MESSAGES.buttonBack(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		
		iForm.addHeaderRow(iHeader);
		
		iSessionRow = iForm.addRow(MESSAGES.propAcademicSession(), new Label());
		
		iName = new UniTimeWidget<TextBox>(new TextBox());
		iName.getWidget().setStyleName("unitime-TextBox");
		iName.getWidget().setMaxLength(100);
		iName.getWidget().setWidth("480px");
		iName.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iName.clearHint();
				iHeader.clearMessage();
			}
		});
		iForm.addRow(MESSAGES.propEventName(), iName);
		
		iSponsors = new ListBox();
		iForm.addRow(MESSAGES.propSponsor(), iSponsors);
		
		iEventType = new UniTimeWidget<ListBox>(new ListBox());
		iEventType.getWidget().addItem(EventInterface.EventType.Special.getName());
		iForm.addRow(MESSAGES.propEventType(), iEventType);
		
		iLimit = new NumberBox();
		iLimit.setStyleName("unitime-TextBox");
		iLimit.setMaxLength(10);
		iLimit.setWidth("50px");
		iForm.addRow(MESSAGES.propAttendance(), iLimit);

		iCourses = new CourseRelatedObjectsTable(iSession);
		iCourses.addValueChangeHandler(new ValueChangeHandler<List<RelatedObjectInterface>>() {
			@Override
			public void onValueChange(ValueChangeEvent<List<RelatedObjectInterface>> event) {
				checkEnrollments(event.getValue(), iMeetings.getMeetings());
			}
		});
		
		iReqAttendance = new CheckBox(MESSAGES.checkRequiredAttendance());
						
		SimpleForm mainContact = new SimpleForm();
		mainContact.getElement().getStyle().clearWidth();
		mainContact.removeStyleName("unitime-NotPrintableBottomLine");
		
		iLookupButton = new Button(MESSAGES.buttonLookupMainContact());
		iLookupButton.setWidth("75px");
		Character lookupAccessKey = UniTimeHeaderPanel.guessAccessKey(MESSAGES.buttonLookupMainContact());
		if (lookupAccessKey != null) iLookupButton.setAccessKey(lookupAccessKey);
		iLookupButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					if (!iLookupButton.isVisible()) return;
					iLookup.setQuery((iMainFName.getText() + (iMainMName.getText().isEmpty() ? "" : " " + iMainMName.getText()) + " " + iMainLName.getWidget().getText()).trim()); 
					iLookup.center();
				}
		});
		iLookupButton.setVisible(false);
		
		iAdditionalLookupButton = new Button(MESSAGES.buttonLookupAdditionalContact());
		iAdditionalLookupButton.setWidth("125px");
		Character additionalLookupAccessKey = UniTimeHeaderPanel.guessAccessKey(MESSAGES.buttonLookupAdditionalContact());
		if (additionalLookupAccessKey != null) iAdditionalLookupButton.setAccessKey(additionalLookupAccessKey);
		iAdditionalLookupButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					if (iAdditionalLookupButton.isVisible()) iAdditionalLookup.center();
				}
		});
		iAdditionalLookupButton.setVisible(false);
		
		
		iMainFName = new TextBox();
		iMainFName.setStyleName("unitime-TextBox");
		iMainFName.setMaxLength(100);
		iMainFName.setWidth("285px");
		mainContact.addRow(MESSAGES.propFirstName(), iMainFName);
		mainContact.setWidget(0, 2, iLookupButton);
		
		iMainMName = new TextBox();
		iMainMName.setStyleName("unitime-TextBox");
		iMainMName.setMaxLength(100);
		iMainMName.setWidth("285px");
		mainContact.addRow(MESSAGES.propMiddleName(), iMainMName);
		
		iMainLName = new UniTimeWidget<TextBox>(new TextBox());
		iMainLName.getWidget().setStyleName("unitime-TextBox");
		iMainLName.getWidget().setMaxLength(100);
		iMainLName.getWidget().setWidth("285px");
		iMainLName.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iMainLName.clearHint();
				iHeader.clearMessage();
			}
		});
		mainContact.addRow(MESSAGES.propLastName(), iMainLName);
		
		iMainEmail = new UniTimeWidget<TextBox>(new TextBox());
		iMainEmail.getWidget().setStyleName("unitime-TextBox");
		iMainEmail.getWidget().setMaxLength(200);
		iMainEmail.getWidget().setWidth("285px");
		iMainEmail.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iMainEmail.clearHint();
				iHeader.clearMessage();
			}
		});
		mainContact.addRow(MESSAGES.propEmail(), iMainEmail);
		
		iMainPhone = new TextBox();
		iMainPhone.setStyleName("unitime-TextBox");
		iMainPhone.setMaxLength(35);
		iMainPhone.setWidth("285px");
		mainContact.addRow(MESSAGES.propPhone(), iMainPhone);
		mainContact.setWidget(mainContact.getRowCount() - 1, 2, iAdditionalLookupButton);
		
		iForm.addRow(MESSAGES.propMainContact(), mainContact);
		
		iContacts = new UniTimeTable<ContactInterface>();
		iContacts.setStyleName("unitime-EventContacts");
		
		List<Widget> contactHeader = new ArrayList<Widget>();
		contactHeader.add(new UniTimeTableHeader(MESSAGES.colName()));
		contactHeader.add(new UniTimeTableHeader(MESSAGES.colEmail()));
		contactHeader.add(new UniTimeTableHeader(MESSAGES.colPhone()));
		contactHeader.add(new UniTimeTableHeader("&nbsp;"));
		iContacts.addRow(null, contactHeader);
		
		iContactRow = iForm.addRow(MESSAGES.propAdditionalContacts(), iContacts);
		iForm.getRowFormatter().setVisible(iContactRow, false);
		
		iEmails = new TextArea();
		iEmails.setStyleName("unitime-TextArea");
		iEmails.setVisibleLines(3);
		iEmails.setCharacterWidth(80);
		UniTimeWidget<TextArea> emailsWithHint = new UniTimeWidget<TextArea>(iEmails);
		emailsWithHint.setHint(MESSAGES.hintAdditionalEmails());
		iForm.addRow(MESSAGES.propAdditionalEmails(), emailsWithHint);
		
		iNotes = new TextArea();
		iNotes.setStyleName("unitime-TextArea");
		iNotes.setVisibleLines(5);
		iNotes.setCharacterWidth(80);
		iForm.addRow(MESSAGES.propAdditionalInformation(), iNotes);
		
		iCoursesForm = new SimpleForm();
		iCoursesForm.addHeaderRow(MESSAGES.sectRelatedCourses());
		iCoursesForm.removeStyleName("unitime-NotPrintableBottomLine");
		iCoursesForm.addRow(iCourses);
		iCoursesForm.addRow(iReqAttendance);
		iForm.addRow(iCoursesForm);
		
		iEventType.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				int row = iForm.getRow(MESSAGES.propAttendance());
				if (iEventType.isReadOnly()) {
					iCoursesForm.setVisible(false);
					iForm.getRowFormatter().setVisible(row, false);
				} else if (iEventType.getWidget().getSelectedIndex() == 1) {
					iCoursesForm.setVisible(true);
					iForm.getRowFormatter().setVisible(row, false);
				} else {
					iCoursesForm.setVisible(false);
					iForm.getRowFormatter().setVisible(row, true);
				}
				checkEnrollments(iCourses.getValue(), iMeetings.getMeetings());
			}
		});
		
		iEventAddMeetings = new AddMeetingsDialog(session, new AsyncCallback<List<MeetingInterface>>() {
			@Override
			public void onFailure(Throwable caught) {
				UniTimeNotifications.error(MESSAGES.failedAddMeetings(caught.getMessage()));
			}

			@Override
			public void onSuccess(List<MeetingInterface> result) {
				LoadingWidget.getInstance().show(MESSAGES.waitCheckingRoomAvailability());
				RPC.execute(EventRoomAvailabilityRpcRequest.checkAvailability(result, iSession.getAcademicSessionId()), new AsyncCallback<EventRoomAvailabilityRpcResponse>() {
					@Override
					public void onFailure(Throwable caught) {
						LoadingWidget.getInstance().hide();
						UniTimeNotifications.error(MESSAGES.failedRoomAvailability(caught.getMessage()));
					}

					@Override
					public void onSuccess(EventRoomAvailabilityRpcResponse result) {
						LoadingWidget.getInstance().hide();
						addMeetings(result.getMeetings());
					}
				});
				// addMeetings(result);
			}
		});
		
		iMeetingsHeader = new UniTimeHeaderPanel(MESSAGES.sectMeetings());
		iMeetingsHeader.addButton("add", MESSAGES.buttonAddMeetings(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				iEventAddMeetings.showDialog();
			}
		});
		iForm.addHeaderRow(iMeetingsHeader);
		
		iMeetings = new EventMeetingTable(EventMeetingTable.Mode.ApprovalOfSingleEventMeetings, true); iMeetings.setEditable(true);
		iMeetings.setOperation(EventMeetingTable.OperationType.AddMeetings, this);
		iMeetings.addValueChangeHandler(new ValueChangeHandler<List<EventMeetingRow>>() {
			@Override
			public void onValueChange(ValueChangeEvent<List<EventMeetingRow>> event) {
				checkEnrollments(iCourses.getValue(), iMeetings.getMeetings());
			}
		});

		
		iForm.addRow(iMeetings);
		
		iEnrollments = new EnrollmentTable(false, true);
		iEnrollments.getTable().setStyleName("unitime-Enrollments");
		iEnrollmentHeader = new UniTimeHeaderPanel(MESSAGES.sectEnrollments());
		iEnrollmentRow = iForm.addHeaderRow(iEnrollmentHeader);
		iForm.addRow(iEnrollments.getTable());
		iForm.getRowFormatter().setVisible(iEnrollmentRow, false);
		iForm.getRowFormatter().setVisible(iEnrollmentRow + 1, false);
		
		iFooter = iHeader.clonePanel("");
		
		iForm.addNotPrintableBottomRow(iFooter);
		
		initWidget(iForm);
	}
	
	public String getMessage() {
		return iNotes.getText();
	}
	
	public EventInterface getEvent() {
		iEvent.setName(iName.getWidget().getText());
		if (!iEventType.isReadOnly()) {
			iEvent.setType(iEventType.getWidget().getSelectedIndex() == 0 ? EventType.Special : EventType.Course);
			iEvent.setMaxCapacity(iLimit.toInteger());
		}
		if (iEvent.getContact() == null) { iEvent.setContact(new ContactInterface()); }
		iEvent.getContact().setExternalId(iMainExternalId);
		iEvent.getContact().setFirstName(iMainFName.getText());
		iEvent.getContact().setMiddleName(iMainMName.getText());
		iEvent.getContact().setLastName(iMainLName.getWidget().getText());
		iEvent.getContact().setEmail(iMainEmail.getWidget().getText());
		iEvent.getContact().setPhone(iMainPhone.getText());
		
		if (iEvent.hasAdditionalContacts()) iEvent.getAdditionalContacts().clear();
		for (ContactInterface contact: iContacts.getData())
			iEvent.addAdditionalContact(contact);
		
		iEvent.setEmail(iEmails.getText());

		/*
		if (iEvent.hasNotes() && iEvent.getNotes().last().getDate() == null)
			iEvent.getNotes().remove(iEvent.getNotes().last());
		if (!iNotes.getText().isEmpty()) {
			NoteInterface note = new NoteInterface();
			note.setNote(iNotes.getText());
			note.setType(iEvent.getId() == null ? NoteType.Create : NoteType.AddMeetings);
			iEvent.addNote(note);
		}
		*/
		
		if (iEvent.hasMeetings())
			iEvent.getMeetings().clear();
		for (MeetingInterface meeting: iMeetings.getMeetings())
			iEvent.addMeeting(meeting);
		
		if (iSponsors.getSelectedIndex() > 0) {
			Long sponsorId = Long.valueOf(iSponsors.getValue(iSponsors.getSelectedIndex()));
			SponsoringOrganizationInterface sponsor = null;
			List<SponsoringOrganizationInterface> sponsors = (getProperties() == null ? null : getProperties().getSponsoringOrganizations());
			for (SponsoringOrganizationInterface s: sponsors)
				if (s.getUniqueId().equals(sponsorId)) { sponsor = s; break; }
			iEvent.setSponsor(sponsor);
		} else {
			iEvent.setSponsor(null);
		}
		
		if (iEvent.getType() == EventType.Course) {
			if (iEvent.hasRelatedObjects())
				iEvent.getRelatedObjects().clear();
			for (RelatedObjectInterface related: iCourses.getValue())
				iEvent.addRelatedObject(related);
			iEvent.setRequiredAttendance(iReqAttendance.getValue());
		} else if (iEvent.getType() == EventType.Special) {
			if (iEvent.hasRelatedObjects())
				iEvent.getRelatedObjects().clear();
			iEvent.setRequiredAttendance(false);
		}
		
		return iEvent;
	}
	
	public EventInterface getSavedEvent() {
		return iSavedEvent;
	}
	
	protected void addMeetings(List<MeetingInterface> meetings) {
		List<MeetingInterface> existingMeetings = iMeetings.getMeetings();
		if (meetings != null && !meetings.isEmpty())
			meetings: for (MeetingInterface meeting: meetings) {
				for (MeetingInterface existing: existingMeetings) {
					if (existing.inConflict(meeting)) {
						UniTimeNotifications.warn(MESSAGES.warnNewMeetingOverlaps(meeting.toString(), existing.toString()));
						continue meetings;
					}
				}
				iMeetings.add(new EventMeetingRow(iEvent, meeting));
			}
		ValueChangeEvent.fire(iMeetings, iMeetings.getValue());
	}
	
	private int iLastScrollTop, iLastScrollLeft;
	public void show() {
		UniTimePageLabel.getInstance().setPageName(iEvent.getId() == null ? MESSAGES.pageAddEvent() : MESSAGES.pageEditEvent());
		setVisible(true);
		iLastScrollLeft = Window.getScrollLeft();
		iLastScrollTop = Window.getScrollTop();
		onShow();
		Window.scrollTo(0, 0);
		if (iForm.getRowFormatter().isVisible(iSessionRow)) {
			iSession.setFilter(this);
			iForm.setWidget(iSessionRow, 1, iSession);
		}
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
	
	public void setup(EventPropertiesRpcResponse properties) {
		if (properties.hasSponsoringOrganizations()) {
			iSponsors.clear();
			iSponsors.addItem("Select...", "");
			for (SponsoringOrganizationInterface sponsor: properties.getSponsoringOrganizations())
				iSponsors.addItem(sponsor.getName(), sponsor.getUniqueId().toString());
		} else {
			iForm.getRowFormatter().setVisible(iForm.getRow(MESSAGES.propSponsor()), false);
		}
		if (isAttached()  && isVisible() && (iEvent != null && iEvent.getId() == null))
			setEvent(null);
	}
	
	public void setEvent(EventInterface event) {
		iForm.getRowFormatter().setVisible(iSessionRow, event == null);
		iEvent = (event == null ? new EventInterface() : event);
		iSavedEvent = null;
		iHeader.clearMessage();
		iName.getWidget().setText(iEvent.getName() == null ? "" : iEvent.getName());
		if (iEvent.hasSponsor()) {
			for (int i = 1; i < iSponsors.getItemCount(); i++) {
				if (iSponsors.getValue(i).equals(iEvent.getSponsor().getUniqueId().toString())) {
					iSponsors.setSelectedIndex(i); break;
				}
			}
		} else {
			iSponsors.setSelectedIndex(0);
		}
		
		boolean canAddCourseEvent = (getProperties() == null ? false : getProperties().isCanAddCourseEvent());
		if (canAddCourseEvent) {
			if (iEventType.getWidget().getItemCount() == 1)
				iEventType.getWidget().addItem(EventInterface.EventType.Course.getName());
		} else {
			if (iEventType.getWidget().getItemCount() == 2)
				iEventType.getWidget().removeItem(1);
		}
		
		if (iEvent.getType() == null) {
			iEventType.getWidget().setSelectedIndex(0);
			iEventType.setReadOnly(false);
		} else {
			iEventType.setText(iEvent.getType().getName());
			iEventType.setReadOnly(true);
		}

		iLimit.setValue(iEvent.hasMaxCapacity() ? iEvent.getMaxCapacity() : null);
		iNotes.setText("");
		iEmails.setText(iEvent.hasEmail() ? iEvent.getEmail() : "");
		if (iEvent.getType() == EventType.Course) {
			iCourses.setValue(iEvent.getRelatedObjects());
			iReqAttendance.setValue(iEvent.hasRequiredAttendance());
		} else {
			if (canAddCourseEvent) iCourses.setValue(null);
			iReqAttendance.setValue(false);
		}
		
		if (iEvent.hasMeetings()) {
			iMeetings.setMeetings(iEvent, iEvent.getMeetings());
		} else {
			iMeetings.setValue(null);
			List<MeetingInterface> meetings = new ArrayList<MeetingInterface>();
			List<SelectionInterface> selection = (iProperties == null ? null : iProperties.getSelection());
			if (selection != null && !selection.isEmpty()) {
				for (SelectionInterface s: selection) {
					for (Integer day: s.getDays()) {
						for (ResourceInterface room: s.getLocations()) {
							MeetingInterface meeting = new MeetingInterface();
							meeting.setStartSlot(s.getStartSlot());
							meeting.setEndSlot(s.getStartSlot() + s.getLength());
							meeting.setStartOffset(0);
							meeting.setEndOffset(0);
							meeting.setDayOfYear(day);
							meeting.setLocation(room);
							meetings.add(meeting);
						}
					}
				}
			}
			if (!meetings.isEmpty()) {
				LoadingWidget.getInstance().show(MESSAGES.waitCheckingRoomAvailability());
				RPC.execute(EventRoomAvailabilityRpcRequest.checkAvailability(meetings, iSession.getAcademicSessionId()), new AsyncCallback<EventRoomAvailabilityRpcResponse>() {
					@Override
					public void onFailure(Throwable caught) {
						LoadingWidget.getInstance().hide();
						UniTimeNotifications.error(MESSAGES.failedRoomAvailability(caught.getMessage()));
					}

					@Override
					public void onSuccess(EventRoomAvailabilityRpcResponse result) {
						LoadingWidget.getInstance().hide();
						iMeetings.setMeetings(iEvent, result.getMeetings());
					}
				});
			}
		}
		
		if (iEvent.hasContact()) {
			iMainExternalId = iEvent.getContact().getExternalId();
			iMainFName.setText(iEvent.getContact().hasFirstName() ? iEvent.getContact().getFirstName() : "");
			iMainMName.setText(iEvent.getContact().hasMiddleName() ? iEvent.getContact().getMiddleName() : "");
			iMainLName.getWidget().setText(iEvent.getContact().hasLastName() ? iEvent.getContact().getLastName() : "");
			iMainPhone.setText(iEvent.getContact().hasPhone() ? iEvent.getContact().getPhone() : "");
			iMainEmail.getWidget().setText(iEvent.getContact().hasEmail() ? iEvent.getContact().getEmail() : "");
		} else {
			ContactInterface mainContact = (getProperties() == null ? null : getProperties().getMainContact());
			if (mainContact != null) {
				iMainExternalId = mainContact.getExternalId();
				iMainFName.setText(mainContact.getFirstName() == null ? "" : mainContact.getFirstName());
				iMainMName.setText(mainContact.getMiddleName() == null ? "" : mainContact.getMiddleName());
				iMainLName.getWidget().setText(mainContact.getLastName() == null ? "" : mainContact.getLastName());
				iMainPhone.setText(mainContact.getPhone() == null ? "" : mainContact.getPhone());
				iMainEmail.getWidget().setText(mainContact.getEmail() == null ? "" : mainContact.getEmail());
			} else {
				iMainExternalId = null;
				iMainFName.setText("");
				iMainMName.setText("");
				iMainLName.getWidget().setText("");
				iMainPhone.setText("");
				iMainEmail.getWidget().setText("");
			}
		}
		
		iContacts.clearTable(1);
		if (iEvent.hasAdditionalContacts()) {
			for (final ContactInterface contact: iEvent.getAdditionalContacts()) {
				List<Widget> row = new ArrayList<Widget>();
				row.add(new Label(contact.getName(), false));
				row.add(new Label(contact.hasEmail() ? contact.getEmail() : "", false));
				row.add(new Label(contact.hasPhone() ? contact.getPhone() : "", false));
				Image remove = new Image(RESOURCES.delete());
				remove.addStyleName("remove");
				remove.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						for (int row = 1; row < iContacts.getRowCount(); row ++)
							if (contact.equals(iContacts.getData(row))) {
								iContacts.removeRow(row);
								break;
							}
						iForm.getRowFormatter().setVisible(iContactRow, iContacts.getRowCount() > 1);
					}
				});
				row.add(remove);
				int rowNum = iContacts.addRow(contact, row);
				for (int col = 0; col < iContacts.getCellCount(rowNum); col++)
					iContacts.getCellFormatter().addStyleName(rowNum, col, "main-contact");
			}
		}
		
		boolean canLookup = (getProperties() == null ? false : getProperties().isCanLookupContacts());
		iLookupButton.setVisible(canLookup);
		iAdditionalLookupButton.setVisible(canLookup);
		
		iEventAddMeetings.reset(iProperties == null ? null : iProperties.getRoomFilter());
		
		DomEvent.fireNativeEvent(Document.get().createChangeEvent(), iEventType.getWidget());
		
		boolean canDelete = (iEvent.getId() != null);
		if (canDelete && iEvent.hasMeetings()) {
			for (MeetingInterface meeting: iEvent.getMeetings()) {
				if (!meeting.isCanEdit()) { canDelete = false; break; }
			}
		}
		iHeader.setEnabled("delete", canDelete);
		iHeader.setEnabled("create", iEvent.getId() == null);
		iHeader.setEnabled("update", iEvent.getId() != null);
	}
	
	public static class CourseRelatedObjectLine {
		List<RelatedObjectLookupRpcResponse> iSubjects, iCourses, iSubparts, iClasses;
		
		public List<RelatedObjectLookupRpcResponse> getSubjects() { return iSubjects; }
		public void setSubjects(List<RelatedObjectLookupRpcResponse> subjects) { iSubjects = subjects; }
		public RelatedObjectLookupRpcResponse getSubject(String value) {
			if (value == null || value.isEmpty() || iSubjects == null) return null;
			for (RelatedObjectLookupRpcResponse r: iSubjects)
				if (r.getUniqueId() != null && r.getUniqueId().toString().equals(value)) return r;
			return null;
		}
		
		public List<RelatedObjectLookupRpcResponse> getCourses() { return iCourses; }
		public void setCourses(List<RelatedObjectLookupRpcResponse> courses) { iCourses = courses; }
		public RelatedObjectLookupRpcResponse getCourse(String value) {
			if (value == null || value.isEmpty() || iCourses == null) return null;
			for (RelatedObjectLookupRpcResponse r: iCourses)
				if (r.getUniqueId() != null && r.getUniqueId().toString().equals(value)) return r;
			return null;
		}

		public List<RelatedObjectLookupRpcResponse> getSubparts() { return iSubparts; }
		public void setSubparts(List<RelatedObjectLookupRpcResponse> subparts) { iSubparts = subparts; }
		public RelatedObjectLookupRpcResponse getSubpart(String value) {
			if (value == null || value.isEmpty() || iSubparts == null) return null;
			for (RelatedObjectLookupRpcResponse r: iSubparts)
				if (r.getUniqueId() != null && (r.getLevel() + ":" + r.getUniqueId()).equals(value)) return r;
			return null;
		}

		public List<RelatedObjectLookupRpcResponse> getClasses() { return iClasses; }
		public void setClasses(List<RelatedObjectLookupRpcResponse> classes) { iClasses = classes; }
		public RelatedObjectLookupRpcResponse getClass(String value) {
			if (value == null || value.isEmpty() || iClasses == null) return null;
			for (RelatedObjectLookupRpcResponse r: iClasses)
				if (r.getUniqueId() != null && r.getUniqueId().toString().equals(value)) return r;
			return null;
		}

	}
	
	private List<RelatedObjectInterface> iLastRelatedObjects = null;
	private List<MeetingInterface> iLastMeetings = null;
	public void checkEnrollments(final List<RelatedObjectInterface> relatedObjects, final List<MeetingInterface> meetings) {
		if (relatedObjects == null || relatedObjects.isEmpty() || iEventType.isReadOnly() || iEventType.getWidget().getSelectedIndex() != 1) {
			iForm.getRowFormatter().setVisible(iEnrollmentRow, false);
			iForm.getRowFormatter().setVisible(iEnrollmentRow + 1, false);
		} else {
			iForm.getRowFormatter().setVisible(iEnrollmentRow, true);
			iForm.getRowFormatter().setVisible(iEnrollmentRow + 1, true);
			if (relatedObjects.equals(iLastRelatedObjects) && meetings.equals(iLastMeetings)) return;
			iEnrollmentHeader.showLoading();
			iLastMeetings = meetings; iLastRelatedObjects = relatedObjects;
			RPC.execute(EventEnrollmentsRpcRequest.getEnrollmentsForRelatedObjects(relatedObjects, meetings, iEvent.getId(), iProperties.getSessionId()), new AsyncCallback<GwtRpcResponseList<ClassAssignmentInterface.Enrollment>>() {
				@Override
				public void onFailure(Throwable caught) {
					if (relatedObjects.equals(iLastRelatedObjects) && meetings.equals(iLastMeetings)) {
						iEnrollments.clear();
						UniTimeNotifications.error(MESSAGES.failedNoEnrollments(caught.getMessage()));
					}
				}

				@Override
				public void onSuccess(GwtRpcResponseList<Enrollment> result) {
					if (relatedObjects.equals(iLastRelatedObjects) && meetings.equals(iLastMeetings)) {
						iEnrollmentHeader.clearMessage();
						iEnrollments.clear();
						iEnrollments.populate(result, false);
					}
				}
			});
		}
	}

	public static class CourseRelatedObjectsTable extends UniTimeTable<CourseRelatedObjectLine> implements HasValue<List<RelatedObjectInterface>> {
		private Timer iChangeTimer = null;
		private static int sChangeWaitTime = 500;
		private AcademicSessionProvider iSession = null;
		private List<RelatedObjectInterface> iLastChange = null;
		
		public CourseRelatedObjectsTable(AcademicSessionProvider session) {
			iSession = session;
			setStyleName("unitime-EventOwners");
			
			List<Widget> header = new ArrayList<Widget>();
			header.add(new UniTimeTableHeader(MESSAGES.colSubject()));
			header.add(new UniTimeTableHeader(MESSAGES.colCourseNumber()));
			header.add(new UniTimeTableHeader(MESSAGES.colConfigOrSubpart()));
			header.add(new UniTimeTableHeader(MESSAGES.colClassNumber()));
			header.add(new UniTimeTableHeader("&nbsp;"));
			
			addRow(null, header);
			
			iChangeTimer = new Timer() {
				@Override
				public void run() {
					List<RelatedObjectInterface> value = getValue();
					if (iLastChange != null && iLastChange.equals(value)) return;
					iLastChange = value;
					ValueChangeEvent.fire(CourseRelatedObjectsTable.this, value);
				}
			};
		}
		
		private void addLine(final RelatedObjectInterface data) {
			List<Widget> row = new ArrayList<Widget>();
			
			final CourseRelatedObjectLine line = new CourseRelatedObjectLine();
			
			final ListBox subject = new ListBox();
			subject.addStyleName("subject");
			subject.addItem("-", "");
			subject.setSelectedIndex(0);
			RPC.execute(RelatedObjectLookupRpcRequest.getChildren(iSession.getAcademicSessionId(), RelatedObjectLookupRpcRequest.Level.SESSION, iSession.getAcademicSessionId()), new AsyncCallback<GwtRpcResponseList<RelatedObjectLookupRpcResponse>>() {

				@Override
				public void onFailure(Throwable caught) {
					UniTimeNotifications.error(MESSAGES.failedLoad(MESSAGES.colSubjects(), caught.getMessage()));
				}

				@Override
				public void onSuccess(GwtRpcResponseList<RelatedObjectLookupRpcResponse> result) {
					line.setSubjects(result);
					Long selectedId = (data != null && data.hasSelection() ? data.getSelection()[0] : null);
					int selectedIdx = -1;
					for (RelatedObjectLookupRpcResponse r: result) {
						subject.addItem(r.getLabel(), r.getUniqueId() == null ? "" : r.getUniqueId().toString());
						if (selectedId != null && selectedId.equals(r.getUniqueId())) selectedIdx = subject.getItemCount() - 1;
					}
					if (selectedIdx >= 0)
						subject.setSelectedIndex(selectedIdx);
					DomEvent.fireNativeEvent(Document.get().createChangeEvent(), subject);
				}
			});
			
			row.add(subject);
			
			final ListBox course = new ListBox();
			course.addStyleName("course");
			course.addItem(MESSAGES.itemNotApplicable(), "");
			course.setSelectedIndex(0);
			
			subject.addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					final RelatedObjectLookupRpcResponse rSubject = (subject.getSelectedIndex() < 0 ? null : line.getSubject(subject.getValue(subject.getSelectedIndex())));
					if (rSubject == null) {
						course.clear();
						course.addItem(MESSAGES.itemNotApplicable(), "");
						course.setSelectedIndex(0);
						DomEvent.fireNativeEvent(Document.get().createChangeEvent(), course);
					} else {
						course.clear();
						RPC.execute(RelatedObjectLookupRpcRequest.getChildren(iSession.getAcademicSessionId(), rSubject), new AsyncCallback<GwtRpcResponseList<RelatedObjectLookupRpcResponse>>() {
							@Override
							public void onFailure(Throwable caught) {
								UniTimeNotifications.error(MESSAGES.failedLoad(MESSAGES.colCourses(), caught.getMessage()));
							}
							@Override
							public void onSuccess(GwtRpcResponseList<RelatedObjectLookupRpcResponse> result) {
								RelatedObjectLookupRpcResponse res = (subject.getSelectedIndex() < 0 ? null : line.getSubject(subject.getValue(subject.getSelectedIndex())));
								if (!rSubject.equals(res)) return;
								course.clear();
								line.setCourses(result);
								if (result.size() > 1)
									course.addItem("-", "");
								Long selectedId = (data != null && data.hasSelection() ? data.getSelection()[1] : null);
								int selectedIdx = -1;
								for (RelatedObjectLookupRpcResponse r: result) {
									course.addItem(r.getLabel(), r.getUniqueId() == null ? "" : r.getUniqueId().toString());
									if (selectedId != null && selectedId.equals(r.getUniqueId())) selectedIdx = course.getItemCount() - 1;
								}
								if (selectedIdx >= 0)
									course.setSelectedIndex(selectedIdx);
								DomEvent.fireNativeEvent(Document.get().createChangeEvent(), course);
							}
						});
						if (line.equals(getData(getRowCount() - 1))) addLine(null);
					}

					iChangeTimer.schedule(sChangeWaitTime);
				}
			});
			
			row.add(course);
			
			final ListBox subpart = new ListBox();
			subpart.addStyleName("subpart");
			subpart.addItem(MESSAGES.itemNotApplicable(), "");
			subpart.setSelectedIndex(0);
			
			course.addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					RelatedObjectLookupRpcResponse rSubject = (subject.getSelectedIndex() < 0 ? null : line.getSubject(subject.getValue(subject.getSelectedIndex())));
					final RelatedObjectLookupRpcResponse rCourse = (course.getSelectedIndex() < 0 ? null : line.getCourse(course.getValue(course.getSelectedIndex())));
					if (rCourse == null) {
						subpart.clear();
						subpart.addItem(MESSAGES.itemNotApplicable(), "");
						DomEvent.fireNativeEvent(Document.get().createChangeEvent(), subpart);
					} else {
						subpart.clear();
						RPC.execute(RelatedObjectLookupRpcRequest.getChildren(iSession.getAcademicSessionId(), rSubject, rCourse), new AsyncCallback<GwtRpcResponseList<RelatedObjectLookupRpcResponse>>() {
							@Override
							public void onFailure(Throwable caught) {
								UniTimeNotifications.error(MESSAGES.failedLoad(MESSAGES.colConfigsOrSubparts(), caught.getMessage()));
							}
							@Override
							public void onSuccess(GwtRpcResponseList<RelatedObjectLookupRpcResponse> result) {
								RelatedObjectLookupRpcResponse res = (course.getSelectedIndex() < 0 ? null : line.getCourse(course.getValue(course.getSelectedIndex())));
								if (!rCourse.equals(res)) return;
								subpart.clear();
								line.setSubparts(result);
								Long selectedId = (data != null && data.hasSelection() && data.getSelection().length >= 3 ? data.getSelection()[2] : null);
								int selectedIdx = -1;
								RelatedObjectLookupRpcRequest.Level selectedLevel = RelatedObjectLookupRpcRequest.Level.NONE;
								if (data != null && data.hasSelection()) {
									switch (data.getType()) {
									case Config: selectedLevel = RelatedObjectLookupRpcRequest.Level.CONFIG; break;
									case Course: selectedLevel = RelatedObjectLookupRpcRequest.Level.COURSE; break;
									case Offering: selectedLevel = RelatedObjectLookupRpcRequest.Level.OFFERING; break;
									case Class: selectedLevel = RelatedObjectLookupRpcRequest.Level.SUBPART; break;
									}
								}
								for (RelatedObjectLookupRpcResponse r: result) {
									subpart.addItem(r.getLabel(), r.getUniqueId() == null ? "" : r.getLevel() + ":" + r.getUniqueId().toString());
									if (selectedLevel == r.getLevel()) {
										switch (r.getLevel()) {
										case COURSE:
										case OFFERING:
											selectedIdx = subpart.getItemCount() - 1;
											break;
										case SUBPART:
										case CONFIG:
											if (r.getUniqueId().equals(selectedId))
												selectedIdx = subpart.getItemCount() - 1;
											break;
										}
									}
								}
								if (selectedIdx >= 0)
									subpart.setSelectedIndex(selectedIdx);
								DomEvent.fireNativeEvent(Document.get().createChangeEvent(), subpart);
							}
						});
					}
					
					iChangeTimer.schedule(sChangeWaitTime);
				}
			});
			
			row.add(subpart);
			
			final ListBox clazz = new ListBox();
			clazz.addStyleName("class");
			clazz.addItem(MESSAGES.itemNotApplicable(), "");
			clazz.setSelectedIndex(0);
			
			subpart.addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					RelatedObjectLookupRpcResponse rSubject = (subject.getSelectedIndex() < 0 ? null : line.getSubject(subject.getValue(subject.getSelectedIndex())));
					RelatedObjectLookupRpcResponse rCourse = (course.getSelectedIndex() < 0 ? null : line.getCourse(course.getValue(course.getSelectedIndex())));
					final RelatedObjectLookupRpcResponse rSubpart = (subpart.getSelectedIndex() < 0 ? null : line.getSubpart(subpart.getValue(subpart.getSelectedIndex())));
					if (rSubpart == null) {
						clazz.clear();
						clazz.addItem(MESSAGES.itemNotApplicable(), "");
						clazz.setSelectedIndex(0);
					} else {
						clazz.clear();
						RPC.execute(RelatedObjectLookupRpcRequest.getChildren(iSession.getAcademicSessionId(), rSubject, rCourse, rSubpart), new AsyncCallback<GwtRpcResponseList<RelatedObjectLookupRpcResponse>>() {
							@Override
							public void onFailure(Throwable caught) {
								UniTimeNotifications.error(MESSAGES.failedLoad(MESSAGES.colClasses(), caught.getMessage()));
							}
							@Override
							public void onSuccess(GwtRpcResponseList<RelatedObjectLookupRpcResponse> result) {
								RelatedObjectLookupRpcResponse res = (subpart.getSelectedIndex() < 0 ? null : line.getSubpart(subpart.getValue(subpart.getSelectedIndex())));
								if (!rSubpart.equals(res)) return;
								clazz.clear();
								line.setClasses(result);
								if (result.size() > 1)
									clazz.addItem("-", "");
								Long selectedId = (data != null && data.hasSelection() && data.getSelection().length >= 4 ? data.getSelection()[3] : null);
								int selectedIdx = -1;
								for (int idx = 0; idx < result.size(); idx++) {
									RelatedObjectLookupRpcResponse r = result.get(idx);
									clazz.addItem(r.getLabel(), r.getUniqueId() == null ? "" : r.getUniqueId().toString());
									if (selectedId != null && selectedId.equals(r.getUniqueId())) selectedIdx = idx;
								}
								if (selectedIdx >= 0)
									clazz.setSelectedIndex(selectedIdx);
							}
						});
					}
					
					iChangeTimer.schedule(sChangeWaitTime);
				}
			});
			
			clazz.addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					iChangeTimer.schedule(sChangeWaitTime);
				}
			});
			
			row.add(clazz);
			
			Image remove = new Image(RESOURCES.delete());
			remove.addStyleName("remove");
			remove.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					for (int row = 1; row < getRowCount(); row ++)
						if (line.equals(getData(row))) {
							removeRow(row);
							break;
						}
					if (getRowCount() <= 1) addLine(null);
					iChangeTimer.schedule(sChangeWaitTime);
				}
			});
			row.add(remove);
			
			addRow(line, row);
		}

		@Override
		public HandlerRegistration addValueChangeHandler(ValueChangeHandler<List<RelatedObjectInterface>> handler) {
			return addHandler(handler, ValueChangeEvent.getType());
		}

		@Override
		public List<RelatedObjectInterface> getValue() {
			List<RelatedObjectInterface> objects = new ArrayList<RelatedObjectInterface>();
			for (int row = 1; row < getRowCount(); row ++) {
				CourseRelatedObjectLine line = getData(row);
				ListBox subject = (ListBox)getWidget(row, 0);
				RelatedObjectLookupRpcResponse rSubject = (subject.getSelectedIndex() < 0 ? null : line.getSubject(subject.getValue(subject.getSelectedIndex())));
				ListBox course = (ListBox)getWidget(row, 1);
				RelatedObjectLookupRpcResponse rCourse = (course.getSelectedIndex() < 0 ? null : line.getCourse(course.getValue(course.getSelectedIndex())));
				ListBox subpart = (ListBox)getWidget(row, 2);
				RelatedObjectLookupRpcResponse rSubpart = (subpart.getSelectedIndex() < 0 ? null : line.getSubpart(subpart.getValue(subpart.getSelectedIndex())));
				ListBox clazz = (ListBox)getWidget(row, 3);
				RelatedObjectLookupRpcResponse rClazz = (clazz.getSelectedIndex() < 0 ? null : line.getClass(clazz.getValue(clazz.getSelectedIndex())));
				if (rClazz != null && rClazz.getRelatedObject() != null) {
					objects.add(rClazz.getRelatedObject()); continue;
				}
				if (rSubpart != null && rSubpart.getRelatedObject() != null) {
					objects.add(rSubpart.getRelatedObject()); continue;
				}
				if (rCourse != null && rCourse.getRelatedObject() != null) {
					objects.add(rCourse.getRelatedObject()); continue;
				}
				if (rSubject != null && rSubject.getRelatedObject() != null) {
					objects.add(rSubject.getRelatedObject()); continue;
				}
			}
			return objects;
		}

		@Override
		public void setValue(List<RelatedObjectInterface> value) {
			setValue(value, false);
		}

		@Override
		public void setValue(List<RelatedObjectInterface> value, boolean fireEvents) {
			iLastChange = null;
			clearTable(1);
			if (value != null)
				for (RelatedObjectInterface line: value)
					addLine(line);
			addLine(null);
			addLine(null);
			if (fireEvents)
				ValueChangeEvent.fire(CourseRelatedObjectsTable.this, getValue());
		}
		
	}
	
	
	public void validate(final AsyncCallback<Boolean> callback) {
		iHeader.clearMessage();
		boolean valid = true;
		if (iName.getWidget().getText().isEmpty()) {
			iName.setErrorHint(MESSAGES.reqEventName());
			UniTimeNotifications.error(MESSAGES.reqEventName());
			iHeader.setErrorMessage(MESSAGES.reqEventName());
			valid = false;
		} else {
			iName.clearHint();
		}
		if (iMainLName.getWidget().getText().isEmpty()) {
			UniTimeNotifications.error(MESSAGES.reqMainContactLastName());
			if (valid)
				iHeader.setErrorMessage(MESSAGES.reqMainContactLastName());
			valid = false;
		} else {
			iMainLName.clearHint();
		}
		if (iMainEmail.getWidget().getText().isEmpty()) {
			UniTimeNotifications.error(MESSAGES.reqMainContactEmail());
			if (valid)
				iHeader.setErrorMessage(MESSAGES.reqMainContactEmail());
			valid = false;
		} else {
			iMainEmail.clearHint();
		}
		if (iMeetings.getValue().isEmpty() && iEvent.getId() == null) {
			UniTimeNotifications.error(MESSAGES.reqMeetings());
			if (valid)
				iHeader.setErrorMessage(MESSAGES.reqMeetings());
			valid = false;
		}
		callback.onSuccess(valid);
	}
	
	protected EventPropertiesRpcResponse getProperties() {
		return iProperties == null ? null : iProperties.getProperties();
	}

	public static interface EventPropertiesProvider {
		public Long getSessionId();
		public EventPropertiesRpcResponse getProperties();
		public List<SelectionInterface> getSelection(); 
		public String getRoomFilter();
		public ContactInterface getMainContact();
	}


	@Override
	public void execute(EventMeetingTable source, OperationType operation, List<EventMeetingRow> selection) {
		switch (operation) {
		case AddMeetings:
			iEventAddMeetings.showDialog();
			break;
		}
	}

	@Override
	public boolean accept(AcademicSession session) {
		return session.has(AcademicSession.Flag.CanAddEvents);
	}

}
