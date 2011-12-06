package org.unitime.timetable.gwt.client.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.client.widgets.UniTimeTextBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeWidget;
import org.unitime.timetable.gwt.services.SectioningService;
import org.unitime.timetable.gwt.services.SectioningServiceAsync;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ClassAssignment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.CourseAssignment;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public class OnlineSectioningTest extends Composite {
	private final SectioningServiceAsync iSectioningService = GWT.create(SectioningService.class);
	private DateTimeFormat iDF = DateTimeFormat.getFormat("HH:mm:ss.SSS");
	private NumberFormat iDecF = NumberFormat.getFormat("00");
	private NumberFormat iAvgF = NumberFormat.getFormat("0.000");

	private UniTimeWidget<UniTimeTextBox> iNbrThreads;
	private UniTimeWidget<TextArea> iCourses, iStudents;
	private UniTimeHeaderPanel iHeader;
	private VerticalPanel iLog;
	private Test[] iTest;
	private Label iAverage;
	
	private long iTotal = 0, iTotalSectioning = 0, iTotalSuggestions = 0, iTotalEnrollment = 0;
	private int iCount = 0;
	
	private UniTimeWidget<ListBox> iSessions;
	
	public OnlineSectioningTest() {
		SimpleForm form = new SimpleForm();

		iHeader = new UniTimeHeaderPanel("Online Student Sectioning Test");
		form.addHeaderRow(iHeader);
		
		iNbrThreads = new UniTimeWidget<UniTimeTextBox>(new UniTimeTextBox());
		iNbrThreads.getWidget().setText("10");
		iNbrThreads.setHint("Number of student simulations to be run in parallel.");
		form.addRow("Number of Threads:", iNbrThreads);
		
		iSessions = new UniTimeWidget<ListBox>(new ListBox());
		form.addRow("Academic Session:", iSessions);
		iSessions.setHint("An academic session with enabled student sectioning.");
		
		LoadingWidget.getInstance().show("Loading...");
		iSectioningService.isAdminOrAdvisor(new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
				iHeader.setErrorMessage(caught.getMessage());
				LoadingWidget.getInstance().fail(caught.getMessage());
				ToolBox.checkAccess(caught);
			}

			@Override
			public void onSuccess(Boolean result) {
				if (!result) {
					iHeader.setErrorMessage("Only administrators can use this page.");
					LoadingWidget.getInstance().fail("Only administrators can use this page.");
					return;
				}
				iSectioningService.listAcademicSessions(true, new AsyncCallback<Collection<String[]>>() {
					@Override
					public void onFailure(Throwable caught) {
						iSessions.setErrorHint(caught.getMessage());
						LoadingWidget.getInstance().fail(caught.getMessage());
					}

					@Override
					public void onSuccess(Collection<String[]> result) {
						for (String[] s: result) {
							iSessions.getWidget().addItem(s[2] + " " + s[1] + " (" + s[2] + ")", s[0]);
						}
						iSectioningService.lastAcademicSession(true, new AsyncCallback<String[]>() {
							@Override
							public void onSuccess(String[] result) {
								for (int i = 0; i < iSessions.getWidget().getItemCount(); i++) {
									if (iSessions.getWidget().getValue(i).equals(result[0]))
										iSessions.getWidget().setSelectedIndex(i);
								}
							}
							
							@Override
							public void onFailure(Throwable caught) {
								iSessions.setErrorHint(caught.getMessage());						
							}
						});
						if (!result.isEmpty())
							iHeader.setEnabled("start", true);	
						LoadingWidget.getInstance().hide();
					}
				});				
			}
		});
		
		iCourses = new UniTimeWidget<TextArea>(new TextArea());
		iCourses.getWidget().setStyleName("unitime-TextArea");
		iCourses.getWidget().setVisibleLines(10);
		iCourses.getWidget().setCharacterWidth(80);
		iCourses.getWidget().setText(
				"CS 11000\n" +
				"CS 38100\n" +
				"MA 26100\n" +
				"CHNS 20100\n" +
				"CS 54100\n" + 
				"MUS 36100\n" +
				"ENGL 10600\n" + 
				"COM 11400F\n" +
				"COM 10200\n" +
				"CNIT 26700\n" +
				"BIOL 11000\n" +
				"BIOL 11100"
				);
		iCourses.setHint("Courses to be used in the simulations.");
		form.addRow("Courses:", iCourses);

		iStudents = new UniTimeWidget<TextArea>(new TextArea());
		iStudents.getWidget().setStyleName("unitime-TextArea");
		iStudents.getWidget().setVisibleLines(10);
		iStudents.getWidget().setCharacterWidth(80);
		iStudents.getWidget().setText("131545480\n" +
				"131545487\n" +
				"131545489\n" +
				"131545491\n" +
				"131545494\n" +
				"131545496\n" +
				"131545498\n" +
				"131545501\n" +
				"131545505\n" +
				"131545507");
		iStudents.setHint("Students to be be enrolled in the simulated schedules (student unique ids), no enrollment will be made if empty.");
		form.addRow("Students:", iStudents);
		
		iAverage = new Label();
		form.addRow("Average:", iAverage);
		
		iLog = new VerticalPanel();
		form.addRow("Log:", iLog);
		
		iHeader.addButton("start", "<u>S</u>tart", 's', 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					iHeader.clearMessage();
					iHeader.setEnabled("start", false);
					iHeader.setEnabled("stop", true);
					iNbrThreads.getWidget().setReadOnly(true);
					iCourses.getWidget().setReadOnly(true);
					iStudents.getWidget().setReadOnly(true);
					startTest();
				} catch (Exception e) {
					iHeader.setErrorMessage(e.getMessage());
				}
			}
		});
		
		iHeader.addButton("stop", "Sto<u>p</u>", 'p', 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					iHeader.clearMessage();
					iHeader.setEnabled("start", true);
					iHeader.setEnabled("stop", false);
					iNbrThreads.getWidget().setReadOnly(false);
					iCourses.getWidget().setReadOnly(false);
					iStudents.getWidget().setReadOnly(false);
					stopTest();
				} catch (Exception e) {
					iHeader.setErrorMessage(e.getMessage());
				}
			}
		});
		
		iHeader.setEnabled("start", false);
		iHeader.setEnabled("stop", false);

		iHeader.addButton("clear", "<u>C</u>lear", 'c', 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				iLog.clear();
			}
		});
		
		initWidget(form);
	}
	
	public void updateAverage() {
		if (iCount == 0)
			iAverage.setText("");
		else
			iAverage.setText( iAvgF.format(0.001 * iTotal / iCount) + " s " +
					"(" + iAvgF.format(0.001 * iTotalSectioning / iCount) + " s sectioning, " +
					iAvgF.format(0.001 * iTotalSuggestions / (2 * iCount)) + " s suggestions, " +
					iAvgF.format(0.001 * iTotalEnrollment / iCount) + " s enrollment, average from " + iCount + " runs).");
	}
	
	public void startTest() {
		iTotal = 0;
		iTotalSectioning = 0;
		iTotalSuggestions = 0;
		iTotalEnrollment = 0;
		iCount = 0;
		int nbrTests = Integer.parseInt(iNbrThreads.getWidget().getText());
		iTest = new Test[nbrTests];
		String[] students = iStudents.getWidget().getText().split("\\n");
		for (int i = 0; i < nbrTests; i++) {
			iTest[i] = new Test(1 + i);
			if (i < students.length) {
				try {
					iTest[i].setStudentId(Long.parseLong(students[i]));					
				} catch (Exception e) {
				}
			}
			iTest[i].scheduleNext();
		}
	}
	
	public void stopTest() {
		for (int i = 0; i < iTest.length; i++) {
			iTest[i].stop();
		}		
	}
	
	public class Test extends Timer {
		private int iIndex = 0;
		private boolean iStopped = false;
		private String[] iChoices;
		private Long iSessionId, iStudentId;
		
		public Test(int index) {
			iIndex = index;
			iChoices = iCourses.getWidget().getText().split("\\n");
			iSessionId = Long.parseLong(iSessions.getWidget().getValue(iSessions.getWidget().getSelectedIndex()));
			iStudentId = null;
		}
		
		public void setStudentId(Long studentId) {
			iStudentId = studentId;
		}

		@Override
		public void run() {
			try {
				final long T0 = new Date().getTime();
				lookupCourses(new ArrayList<String>(), 3 + (int)(Random.nextDouble() * 5), new Callback<List<String>>() {
					@Override
					public void execute(final List<String> courses, Throwable failure) {
						final CourseRequestInterface request = new CourseRequestInterface();
						request.setAcademicSessionId(iSessionId);
						request.setStudentId(iStudentId);
						for (String course: courses) {
							CourseRequestInterface.Request r = new CourseRequestInterface.Request();
							r.setRequestedCourse(course);
							request.getCourses().add(r);
						}
						checkCourses(request, new Callback<Collection<String>>() {
							@Override
							public void execute(Collection<String> success, Throwable failure) {
								final long T1 = new Date().getTime();
								section(request, new ArrayList<ClassAssignment>(), new Callback<ClassAssignmentInterface>() {
									@Override
									public void execute(final ClassAssignmentInterface assignment, Throwable failure) {
										final long T2 = new Date().getTime();
										computeSuggestions(request, assignment, new Callback<List<ClassAssignmentInterface>>() {
											@Override
											public void execute(List<ClassAssignmentInterface> suggestions, Throwable failure) {
												computeSuggestions(request, assignment, new Callback<List<ClassAssignmentInterface>>() {
													@Override
													public void execute(List<ClassAssignmentInterface> success, Throwable failure) {
														final long T3 = new Date().getTime();
														enroll(request, assignment, new Callback<ClassAssignmentInterface>() {
															@Override
															public void execute(ClassAssignmentInterface success, Throwable failure) {
																final long T4 = new Date().getTime();
																info("Run completed in " + iAvgF.format(0.001 * (T4 - T0)) + " s " + courses + ".");
																iTotal += (T4 - T0);
																iTotalSectioning += (T2 - T1);
																iTotalSuggestions += (T3 - T2);
																iTotalEnrollment += (T4 - T3);
																iCount ++;
																updateAverage();
																scheduleNext();
															}
														});
													}
												});
											}
										});
									}
								});
							}
						});
					}
				});
			} catch (Exception e) {
				error("Failure: " + e.getMessage());
			}
		}
		
		public void enroll(final CourseRequestInterface request, final ClassAssignmentInterface assignment, final Callback<ClassAssignmentInterface> callback) {
			if (request.getStudentId() == null) {
				callback.execute(null, null);
				return;
			}
			ArrayList<ClassAssignment> assignments = new ArrayList<ClassAssignment>();
			for (CourseAssignment cx: assignment.getCourseAssignments())
				assignments.addAll(cx.getClassAssignments());
			iSectioningService.enroll(request, assignments, new AsyncCallback<ClassAssignmentInterface>() {
				@Override
				public void onFailure(Throwable caught) {
					warn("&nbsp;&nbsp;enroll(" + assignment.getCourseAssignments() + ") failed: " + caught.getMessage());
					callback.execute(null, caught);
				}

				@Override
				public void onSuccess(ClassAssignmentInterface result) {
					callback.execute(result, null);
				}
			});
		}
		
		public void computeSuggestions(final CourseRequestInterface request, final ClassAssignmentInterface assignment, final Callback<List<ClassAssignmentInterface>> callback) {
			List<ClassAssignment> assignments = new ArrayList<ClassAssignment>();
			for (CourseAssignment cx: assignment.getCourseAssignments())
				assignments.addAll(cx.getClassAssignments());
			if (assignments.isEmpty()) {
				warn("No solution has been found to " + request.getCourses() + ".");
				List<ClassAssignmentInterface> ret = new ArrayList<ClassAssignmentInterface>();
				ret.add(assignment);
				callback.execute(ret, null);
				return;
			}
			int idx = (int)(Random.nextDouble() * assignments.size());
			debug("computeSuggestions(" + request + "," + assignments + "," + idx + ")");
			iSectioningService.computeSuggestions(true, request, assignments, idx, null, new AsyncCallback<Collection<ClassAssignmentInterface>>() {
				@Override
				public void onFailure(Throwable caught) {
					warn("&nbsp;&nbsp;computeSuggestions(" + request + ") failed: " + caught.getMessage());
					List<ClassAssignmentInterface> ret = new ArrayList<ClassAssignmentInterface>();
					ret.add(assignment);
					callback.execute(ret, caught);
				}
				@Override
				public void onSuccess(Collection<ClassAssignmentInterface> result) {
					List<ClassAssignmentInterface> ret = new ArrayList<ClassAssignmentInterface>();
					ret.add(assignment);
					ret.addAll(result);
					callback.execute(ret, null);
				}
			});
		}
		
		public void section(final CourseRequestInterface request, ArrayList<ClassAssignment> assignment, final Callback<ClassAssignmentInterface> callback) {
			debug("section(" + request + "," + assignment + ")");
			iSectioningService.section(true, request, assignment, new AsyncCallback<ClassAssignmentInterface>() {
				@Override
				public void onFailure(Throwable caught) {
					warn("&nbsp;&nbsp;listClasses(" + request + ") failed: " + caught.getMessage());
					callback.execute(null, caught);
				}

				@Override
				public void onSuccess(final ClassAssignmentInterface result) {
					callback.execute(result, null);					
				}
			});
		}
		
		private void checkCourses(final CourseRequestInterface request, final Callback<Collection<String>> callback) {
			debug("checkCourses(" + request + ")");	
			iSectioningService.checkCourses(true, request, new AsyncCallback<Collection<String>>() {
				@Override
				public void onFailure(Throwable caught) {
					warn("&nbsp;&nbsp;checkCourses(" + request + ") failed: " + caught.getMessage());
					callback.execute(null, caught);
				}

				@Override
				public void onSuccess(Collection<String> result) {
					callback.execute(result, null);
				}
				
			});
		}
		
		public void lookupCourses(final List<String> courses, final int nbrCourses, final Callback<List<String>> callback) {
			if (courses.size() >= nbrCourses) {
				callback.execute(courses, null);
			} else {
				while (courses.size() < nbrCourses) {
					final String course = iChoices[(int)(Random.nextDouble() * iChoices.length)];
					if (courses.contains(course)) continue;
					courses.add(course);
					debug(courses.size() + ". " + course);					
					listCourseOfferings(course.substring(0, 1), new Callback<Collection<CourseAssignment>>() {
						@Override
						public void execute(Collection<CourseAssignment> success, Throwable failure) {
							listCourseOfferings(course.substring(0, 2), new Callback<Collection<CourseAssignment>>() {
								@Override
								public void execute(Collection<CourseAssignment> success, Throwable failure) {
									listCourseOfferings(course, new Callback<Collection<CourseAssignment>>() {
										@Override
										public void execute(Collection<CourseAssignment> success, Throwable failure) {
											retrieveCourseDetails(course, new Callback<String>() {
												@Override
												public void execute(String success, Throwable failure) {
													listClasses(course, new Callback<Collection<ClassAssignment>>() {
														@Override
														public void execute(Collection<ClassAssignment> success, Throwable failure) {
															lookupCourses(courses, nbrCourses, callback);
														}
													});
												}
											});
										}
									});
								}
							});						
						}
					});
					break;
				}				
			}			
		}
		
		private void listClasses(final String course, final Callback<Collection<ClassAssignment>> callback) {
			debug("listClasses(" + course + ")");
			iSectioningService.listClasses(iSessionId, course, new AsyncCallback<Collection<ClassAssignment>>() {
				@Override
				public void onFailure(Throwable caught) {
					warn("&nbsp;&nbsp;listClasses(" + course + ") failed: " + caught.getMessage());
					callback.execute(null, caught);					
				}

				@Override
				public void onSuccess(Collection<ClassAssignment> result) {
					callback.execute(result, null);
				}
			});
		}
		
		private void retrieveCourseDetails(final String course, final Callback<String> callback) {
			debug("retrieveCourseDetails(" + course + ")");
			iSectioningService.retrieveCourseDetails(iSessionId, course, new AsyncCallback<String>() {
				@Override
				public void onFailure(Throwable caught) {
					warn("&nbsp;&nbsp;retrieveCourseDetails(" + course + ") failed: " + caught.getMessage());
					callback.execute(null, caught);
				}

				@Override
				public void onSuccess(String result) {
					callback.execute(result, null);
				}
			});
		}
		
		private void listCourseOfferings(final String course, final Callback<Collection<CourseAssignment>> callback) {
			debug("listCourseOfferings(" + course + ")");
			iSectioningService.listCourseOfferings(iSessionId, course, 20, new AsyncCallback<Collection<CourseAssignment>>() {
				@Override
				public void onFailure(Throwable caught) {
					warn("&nbsp;&nbsp;listCourseOfferings(" + course + ") failed: " + caught.getMessage());
					callback.execute(null, caught);
				}
				@Override
				public void onSuccess(Collection<CourseAssignment> result) {
					callback.execute(result, null);
				}
			});
		}
		
		public void scheduleNext() {
			if (iStopped) return;
			// int delay = 100 + (int)(4900 * Random.nextDouble());
			// debug("Next run in " + delay + " ms.");
			schedule(iIndex * 100);			
		}
		
		public void stop() {
			iStopped = true;
		}
		
		private HTML message(String message) {
			return new HTML("[" + iDecF.format(iIndex) + " " + iDF.format(new Date()) + "] " + message);
		}
		
		private void debug(String message) {
			// iLog.add(message(message));
		}

		private void info(String message) {
			iLog.add(message(message));
		}

		private void warn(String message) {
			HTML m = message(message);
			m.getElement().getStyle().setColor("orange");
			iLog.add(m);
		}

		private void error(String message) {
			HTML m = message(message);
			m.getElement().getStyle().setColor("red");
			iLog.add(m);
		}
	}
	
	private interface Callback<T> {
		public void execute(T success, Throwable failure);
	}

}
