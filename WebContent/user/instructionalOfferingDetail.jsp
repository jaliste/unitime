<%--
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008 - 2010, UniTime LLC
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
--%>
<%@ page language="java" autoFlush="true" errorPage="../error.jsp" %>
<%@ page import="org.unitime.timetable.util.Constants" %>
<%@ page import="org.unitime.timetable.model.DistributionPref" %>
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.webutil.JavascriptFunctions" %>
<%@ page import="org.unitime.timetable.webutil.WebInstrOfferingConfigTableBuilder"%>
<%@ page import="org.unitime.timetable.form.InstructionalOfferingDetailForm"%>
<%@ page import="org.unitime.timetable.solver.WebSolver"%>
<%@ page import="org.unitime.timetable.model.CourseOffering" %>
<%@ page import="org.unitime.timetable.model.Reservation" %>
<%@ page import="org.unitime.timetable.model.Roles" %>
<%@ page import="org.unitime.commons.User" %>

<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<%@ taglib uri="/WEB-INF/tld/localization.tld" prefix="loc" %> 
<tiles:importAttribute />
<% 
	User user = Web.getUser(session);
	String frmName = "instructionalOfferingDetailForm";
	InstructionalOfferingDetailForm frm = (InstructionalOfferingDetailForm) request.getAttribute(frmName);

	String crsNbr = "";
	if (session.getAttribute(Constants.CRS_NBR_ATTR_NAME)!=null )
		crsNbr = session.getAttribute(Constants.CRS_NBR_ATTR_NAME).toString();
%>
<loc:bundle name="CourseMessages">
<SCRIPT language="javascript">
	<!--
		<%= JavascriptFunctions.getJsConfirm(Web.getUser(session)) %>
		
		function confirmMakeOffered() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			if (!confirm('<%=MSG.confirmMakeOffered() %>')) {
				return false;
			}

			return true;
		}

		function confirmMakeNotOffered() {
			if (jsConfirm!=null && !jsConfirm)
				return true;
				
			if (!confirm('<%=MSG.confirmMakeNotOffered() %>')) {
				return false;
			}
			
			return true;
		}
		
		function confirmDelete() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			if (!confirm('<%=MSG.confirmDeleteIO() %>')) {
				return false;
			}

			return true;
		}

	// -->
</SCRIPT>

	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD valign="middle" colspan='2'>
				<html:form action="/instructionalOfferingDetail" styleClass="FormWithNoPadding">
					<input type='hidden' name='confirm' value='y'/>
					<html:hidden property="instrOfferingId"/>	
					<html:hidden property="nextId"/>
					<html:hidden property="previousId"/>
					<html:hidden property="catalogLinkLabel"/>
					<html:hidden property="catalogLinkLocation"/>
					
				<tt:section-header>
					<tt:section-title>
							<A  title="<%=MSG.titleBackToIOList(MSG.accessBackToIOList()) %>" 
								accesskey="<%=MSG.accessBackToIOList() %>"
								class="l8" 
								href="instructionalOfferingShowSearch.do?doit=Search&subjectAreaId=<bean:write name="instructionalOfferingDetailForm" property="subjectAreaId" />&courseNbr=<%=crsNbr%>#A<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" />"
							><bean:write name="instructionalOfferingDetailForm" property="instrOfferingName" /></A> 
					</tt:section-title>						
					<bean:define id="instrOfferingId">
						<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" />				
					</bean:define>
					<bean:define id="subjectAreaId">
						<bean:write name="instructionalOfferingDetailForm" property="subjectAreaId" />				
					</bean:define>
				 
					<!-- Display buttons only if editable by current user -->
					<logic:equal name="instructionalOfferingDetailForm" property="isEditable" value="true">
					
						<!-- Do not display buttons if offered -->
						<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="false">
					
							<html:submit property="op" 
									styleClass="btn" 
									accesskey="<%=MSG.accessAddConfiguration() %>" 
									title="<%=MSG.titleAddConfiguration(MSG.accessAddConfiguration()) %>">
								<loc:message name="actionAddConfiguration" />
							</html:submit>
						</logic:equal>
						
					</logic:equal>
	
					<!-- Display buttons only if managed by current user -->
					<logic:equal name="instructionalOfferingDetailForm" property="isManager" value="true">

						<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="false">
							<html:submit property="op" 
									styleClass="btn" 
									accesskey="<%=MSG.accessCrossLists() %>" 
									title="<%=MSG.titleCrossLists(MSG.accessCrossLists()) %>">
								<loc:message name="actionCrossLists" />
							</html:submit>
						</logic:equal>
					</logic:equal>

					<logic:equal name="instructionalOfferingDetailForm" property="isFullyEditable" value="true">
						<!-- Display 'Make Offered' if offering is currently 'Not Offered' -->
						<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="true">
							<html:submit property="op" 
									onclick="return confirmMakeOffered();"
									styleClass="btn" 
									accesskey="<%=MSG.accessMakeOffered() %>" 
									title="<%=MSG.titleMakeOffered(MSG.accessMakeOffered()) %>">
								<loc:message name="actionMakeOffered" />
							</html:submit>
							
						<% if (user!=null
								&& user.getRole().equals(Roles.ADMIN_ROLE)) { %>
							<html:submit property="op" 
									onclick="return confirmDelete();"
									styleClass="btn" 
									accesskey="<%=MSG.accessDeleteIO() %>" 
									title="<%=MSG.titleDeleteIO(MSG.accessDeleteIO()) %>">
								<loc:message name="actionDeleteIO" />
							</html:submit>
						<% } %>
						
						</logic:equal>
	
						<!-- Display 'Make NOT Offered' if offering is currently 'Offered' -->
						<logic:notEqual name="instructionalOfferingDetailForm" property="notOffered" value="true">
							<html:submit property="op" 
									onclick="return confirmMakeNotOffered();"
									styleClass="btn" 
									accesskey="<%=MSG.accessMakeNotOffered() %>"
									title="<%=MSG.titleMakeNotOffered(MSG.accessMakeNotOffered()) %>">
								<loc:message name="actionMakeNotOffered" />
							</html:submit>
						</logic:notEqual>
			
					</logic:equal>
					
					<logic:equal name="instructionalOfferingDetailForm" property="canLock" value="true">
						<html:submit property="op" styleClass="btn" 
								accesskey="<%=MSG.accessLockIO() %>" 
								title="<%=MSG.titleLockIO(MSG.accessLockIO()) %>">
							<loc:message name="actionLockIO"/>
						</html:submit> 
					</logic:equal>
					<logic:equal name="instructionalOfferingDetailForm" property="canUnlock" value="true">
						<html:submit property="op" styleClass="btn" 
								accesskey="<%=MSG.accessUnlockIO() %>" 
								title="<%=MSG.titleUnlockIO(MSG.accessUnlockIO()) %>">
							<loc:message name="actionUnlockIO"/>
						</html:submit> 
					</logic:equal>
				
					<logic:notEmpty name="instructionalOfferingDetailForm" property="previousId">
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessPreviousIO() %>" 
								title="<%=MSG.titlePreviousIO(MSG.accessPreviousIO()) %>">
							<loc:message name="actionPreviousIO" />
						</html:submit> 
					</logic:notEmpty>
					<logic:notEmpty name="instructionalOfferingDetailForm" property="nextId">
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessNextIO() %>" 
								title="<%=MSG.titleNextIO(MSG.accessNextIO()) %>">
							<loc:message name="actionNextIO" />
						</html:submit> 
					</logic:notEmpty>

					<tt:back styleClass="btn" 
							name="<%=MSG.actionBackIODetail() %>" 
							title="<%=MSG.titleBackIODetail(MSG.accessBackIODetail()) %>" 
							accesskey="<%=MSG.accessBackIODetail() %>" 
							type="InstructionalOffering">
						<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId"/>
					</tt:back>
				</tt:section-header>					
				
				</html:form>
			</TD>
		</TR>		

		<logic:messagesPresent>
		<TR>
			<TD colspan="2" align="left" class="errorCell">
					<B><U><loc:message name="errors"/></U></B><BR>
				<BLOCKQUOTE>
				<UL>
				    <html:messages id="error">
				      <LI>
						${error}
				      </LI>
				    </html:messages>
			    </UL>
			    </BLOCKQUOTE>
			</TD>
		</TR>
		</logic:messagesPresent>

		<TR>
			<TD width="20%" valign="top"><loc:message name="propertyCourseOfferings"/></TD>
			<TD>
				<TABLE border="0" width="100%" cellspacing="0" cellpadding="2">
					<TR>
						<TD align="center" class="WebTableHeader">&nbsp;</TD>
						<TD align="left" class="WebTableHeader"><loc:message name="columnTitle"/></TD>
						<TD align="left" class="WebTableHeader"><loc:message name="columnReserved"/></TD>
						<TD align="left" class="WebTableHeader"><loc:message name="columnScheduleOfClassesNote"/></TD>
						<logic:equal name="instructionalOfferingDetailForm" property="hasDemandOfferings" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnDemandsFrom"/></TD>
						</logic:equal>
						<TD align="center" class="WebTableHeader">&nbsp;</TD>
					</TR>
				<logic:iterate id="co" name="instructionalOfferingDetailForm" property="courseOfferings" >
					<TR>
						<TD align="center" class="BottomBorderGray">
							&nbsp;
							<logic:equal name="co" property="isControl" value="true">
								<IMG src="images/tick.gif" alt="<%=MSG.altControllingCourse() %>" title="<%=MSG.titleControllingCourse() %>" border="0">
							</logic:equal>
							&nbsp;
						</TD>
						<TD class="BottomBorderGray"><bean:write name="co" property="courseNameWithTitle"/></TD>
						<TD class="BottomBorderGray">
							<logic:notEmpty name="co" property="reservation">
								<bean:write name="co" property="reservation"/>
							</logic:notEmpty>
						</TD>
						<TD class="BottomBorderGray">&nbsp;<bean:write name="co" property="scheduleBookNote"/></TD>
						<logic:equal name="instructionalOfferingDetailForm" property="hasDemandOfferings" value="true">
							<TD class="BottomBorderGray">&nbsp;
							<%
								CourseOffering cod = ((CourseOffering)co).getDemandOffering();
								if (cod!=null) out.write(cod.getCourseName()); 
							 %>
							</TD>
						</logic:equal>
						<TD align="right" class="BottomBorderGray">
							<!-- Display buttons if course offering is owned by current user -->
							<% 
								String courseOfferingId = ((CourseOffering)co).getUniqueId().toString();
								boolean isEditableBy = ((CourseOffering)co).isEditableBy(Web.getUser(session));
								boolean isLimitEditableBy = ((CourseOffering)co).isLimitedEditableBy(Web.getUser(session));
								if (isEditableBy || isLimitEditableBy) {
							%>
							
							<html:form action="/courseOfferingEdit" styleClass="FormWithNoPadding">
								<html:hidden property="courseOfferingId" value="<%= courseOfferingId %>" />

								<% if (isEditableBy) { %>
								<html:submit property="op" 
										styleClass="btn" 
										title="<%=MSG.titleEditCourseOffering() %>">
									<loc:message name="actionEditCourseOffering" />
								</html:submit>
								<% } %>
							</html:form>
							<%
								}
							%>
						</TD>
					</TR>
				</logic:iterate>
				</TABLE>
			</TD>
		</TR>
		
		<TR>
			<TD><loc:message name="propertyEnrollment"/> </TD>
			<TD>
				<bean:write name="instructionalOfferingDetailForm" property="enrollment" /> 
			</TD>
		</TR>

		<TR>
			<TD><loc:message name="propertyLastEnrollment"/> </TD>
			<TD>
				<logic:equal name="instructionalOfferingDetailForm" property="demand" value="0">
					-
				</logic:equal>
				<logic:notEqual name="instructionalOfferingDetailForm" property="demand" value="0">
					<bean:write name="instructionalOfferingDetailForm" property="demand" /> 
				</logic:notEqual>
			</TD>
		</TR>

		<logic:notEqual name="instructionalOfferingDetailForm" property="projectedDemand" value="0">
			<TR>
				<TD><loc:message name="propertyProjectedDemand"/> </TD>
				<TD>
					<bean:write name="instructionalOfferingDetailForm" property="projectedDemand" /> 
				</TD>
			</TR>
		</logic:notEqual>

		<TR>
			<TD><loc:message name="propertyOfferingLimit"/> </TD>
			<TD>
				<logic:equal name="instructionalOfferingDetailForm" property="unlimited" value="false">
					<bean:write name="instructionalOfferingDetailForm" property="limit" /> 
					<% if (request.getAttribute("limitsDoNotMatch")!=null) { %>
						&nbsp;
						<img src='images/Error16.jpg' alt='<%=MSG.altLimitsDoNotMatch() %>' title='<%=MSG.titleLimitsDoNotMatch() %>' border='0' align='top'> &nbsp;
						<font color="#FF0000"><%= MSG.errorReservedSpacesForOfferingsTotal(request.getAttribute("limitsDoNotMatch").toString()) %></font>
					<% } %>
				</logic:equal>
				<logic:equal name="instructionalOfferingDetailForm" property="unlimited" value="true">
					<span title="<%=MSG.titleUnlimitedEnrollment() %>"><font size="+1">&infin;</font></span>
				</logic:equal>
			</TD>
		</TR>

		<TR>
			<TD><loc:message name="propertyConsent"/> </TD>
			<TD>
				<bean:write name="instructionalOfferingDetailForm" property="consentType" /> 
			</TD>
		</TR>

		<TR>
			<TD><loc:message name="propertyDesignatorRequired"/></TD>
			<TD>
				<logic:equal name="instructionalOfferingDetailForm" property="designatorRequired" value="true">
					<IMG src="images/tick.gif" alt="<%=MSG.altDesignatorRequired() %>" title="<%=MSG.titleDesignatorRequired() %>" border="0">
				</logic:equal>
				<logic:equal name="instructionalOfferingDetailForm" property="designatorRequired" value="false">
					<loc:message name="no"/>
				</logic:equal>&nbsp;
			</TD>
		</TR>
		<TR>
			<TD><loc:message name="propertyCredit"/></TD>
			<TD>
				<bean:write name="instructionalOfferingDetailForm" property="creditText" />
			</TD>
		</TR>
		
		<logic:equal name="instructionalOfferingDetailForm" property="byReservationOnly" value="true">
			<TR>
				<TD><loc:message name="propertyByReservationOnly"/></TD>
				<TD>
					<IMG src="images/tick.gif" alt="ENABLED" title="<%=MSG.descriptionByReservationOnly2() %>" border="0">
					<i><loc:message name="descriptionByReservationOnly2"/></i>
				</TD>
			</TR>
		</logic:equal>
		
		<logic:notEmpty name="instructionalOfferingDetailForm" property="coordinators">
			<TR>
				<TD valign="top"><loc:message name="propertyCoordinators"/></TD>
				<TD>
					<bean:write name="instructionalOfferingDetailForm" property="coordinators" filter="false"/>
				</TD>
			</TR>
		</logic:notEmpty>
		
		<logic:notEmpty name="instructionalOfferingDetailForm" property="catalogLinkLabel">
		<TR>
			<TD><loc:message name="propertyCourseCatalog"/> </TD>
			<TD>
				<A href="<bean:write name="instructionalOfferingDetailForm" property="catalogLinkLocation" />" 
						target="_blank"><bean:write name="instructionalOfferingDetailForm" property="catalogLinkLabel" /></A>
			</TD>
		</TR>
		</logic:notEmpty>
		
		<TR>
			<TD colspan="2">
				<div id='UniTimeGWT:CourseCurricula' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
			</TD>
		</TR>
		
		<TR>
			<TD colspan="2">
				<a name="reservations"></a>
				<logic:equal name="instructionalOfferingDetailForm" property="isEditable" value="true">
					<div id='UniTimeGWT:OfferingReservations' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
				</logic:equal>
				<logic:notEqual name="instructionalOfferingDetailForm" property="isEditable" value="true">
					<div id='UniTimeGWT:OfferingReservationsRO' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
				</logic:notEqual>
			</TD>
		</TR>

		<TR>
			<TD colspan="2" >&nbsp;</TD>
		</TR>

<!-- Configuration -->
		<TR>
			<TD colspan="2" valign="middle">
	<% //output configuration
	if (frm.getInstrOfferingId() != null){
		WebInstrOfferingConfigTableBuilder ioTableBuilder = new WebInstrOfferingConfigTableBuilder();
		ioTableBuilder.setDisplayDistributionPrefs(false);
		ioTableBuilder.setDisplayConfigOpButtons(true);
		ioTableBuilder.htmlConfigTablesForInstructionalOffering(
									session,
				    		        WebSolver.getClassAssignmentProxy(session),
				    		        WebSolver.getExamSolver(session),
				    		        frm.getInstrOfferingId(), 
				    		        Web.getUser(session), out,
				    		        request.getParameter("backType"),
				    		        request.getParameter("backId"));
	}
	%>
			</TD>
		</TR>

		<TR>
			<TD valign="middle" colspan='3' align='left'>
				<tt:displayPrefLevelLegend/>
			</TD>
		</TR>
		
		<% if (request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)!=null) { %>
			<TR>
				<TD colspan="2" >&nbsp;</TD>
			</TR>
	
			<TR>
				<TD colspan="2">
					<TABLE width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:0;">
						<%=request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)%>
					</TABLE>
				</TD>
			</TR>
		<% } %>
		

		<TR>
			<TD colspan="2">
				<tt:exams type='InstructionalOffering' add='true'>
					<bean:write name="<%=frmName%>" property="instrOfferingId"/>
				</tt:exams>
			</TD>
		</TR>
		
		<tt:last-change type='InstructionalOffering'>
			<bean:write name="<%=frmName%>" property="instrOfferingId"/>
		</tt:last-change>		

		<TR>
			<TD colspan="2">
				<div id='UniTimeGWT:OfferingEnrollments' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
			</TD>
		</TR>

<!-- Buttons -->
		<TR>
			<TD colspan="2" valign="middle">
				<DIV class="WelcomeRowHeadBlank">&nbsp;</DIV>
			</TD>
		</TR>

		<TR>
			<TD colspan="2" align="right">
			
				<html:form action="/instructionalOfferingDetail" styleClass="FormWithNoPadding">
					<input type='hidden' name='confirm' value='y'/>
					<html:hidden property="instrOfferingId"/>	
					<html:hidden property="nextId"/>
					<html:hidden property="previousId"/>
					<html:hidden property="canLock"/>
					<html:hidden property="canUnlock"/>
					
				<!-- Display buttons only if editable by current user -->
				<logic:equal name="instructionalOfferingDetailForm" property="isEditable" value="true">
				
					<!-- Do not display buttons if offered -->
					<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="false">
				
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessAddConfiguration() %>" 
								title="<%=MSG.titleAddConfiguration(MSG.accessAddConfiguration()) %>">
							<loc:message name="actionAddConfiguration" />
						</html:submit>
					</logic:equal>
					
				</logic:equal>

				<!-- Display buttons only if managed by current user -->
				<logic:equal name="instructionalOfferingDetailForm" property="isManager" value="true">

					<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="false">
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessCrossLists() %>" 
								title="<%=MSG.titleCrossLists(MSG.accessCrossLists()) %>">
							<loc:message name="actionCrossLists" />
						</html:submit>
					</logic:equal>

				</logic:equal>

				<logic:equal name="instructionalOfferingDetailForm" property="isFullyEditable" value="true">
					<!-- Display 'Make Offered' if offering is currently 'Not Offered' -->
					<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="true">
						<html:submit property="op" 
								onclick="return confirmMakeOffered();"
								styleClass="btn" 
								accesskey="<%=MSG.accessMakeOffered() %>" 
								title="<%=MSG.titleMakeOffered(MSG.accessMakeOffered()) %>">
							<loc:message name="actionMakeOffered" />
						</html:submit>

						<% if (user!=null
								&& user.getRole().equals(Roles.ADMIN_ROLE)) { %>
						<html:submit property="op" 
								onclick="return confirmDelete();"
								styleClass="btn" 
								accesskey="<%=MSG.accessDeleteIO() %>" 
								title="<%=MSG.titleDeleteIO(MSG.accessDeleteIO()) %>">
							<loc:message name="actionDeleteIO" />
						</html:submit>
						<% } %>
						
					</logic:equal>
	
					<!-- Display 'Make NOT Offered' if offering is currently 'Offered' -->
					<logic:notEqual name="instructionalOfferingDetailForm" property="notOffered" value="true">
						<html:submit property="op" 
								onclick="return confirmMakeNotOffered();"
								styleClass="btn" 
								accesskey="<%=MSG.accessMakeNotOffered() %>"
								title="<%=MSG.titleMakeNotOffered(MSG.accessMakeNotOffered()) %>">
							<loc:message name="actionMakeNotOffered" />
						</html:submit>
					</logic:notEqual>
		
				</logic:equal>


				<logic:equal name="instructionalOfferingDetailForm" property="canLock" value="true">
					<html:submit property="op" styleClass="btn" 
							accesskey="<%=MSG.accessLockIO() %>" 
							title="<%=MSG.titleLockIO(MSG.accessLockIO()) %>">
						<loc:message name="actionLockIO"/>
					</html:submit> 
				</logic:equal>
				<logic:equal name="instructionalOfferingDetailForm" property="canUnlock" value="true">
					<html:submit property="op" styleClass="btn" 
							accesskey="<%=MSG.accessUnlockIO() %>" 
							title="<%=MSG.titleUnlockIO(MSG.accessUnlockIO()) %>">
						<loc:message name="actionUnlockIO"/>
					</html:submit> 
				</logic:equal>

				<logic:notEmpty name="instructionalOfferingDetailForm" property="previousId">
					<html:submit property="op" 
							styleClass="btn" 
							accesskey="<%=MSG.accessPreviousIO() %>" 
							title="<%=MSG.titlePreviousIO(MSG.accessPreviousIO()) %>">
						<loc:message name="actionPreviousIO" />
					</html:submit> 
				</logic:notEmpty>
				<logic:notEmpty name="instructionalOfferingDetailForm" property="nextId">
					<html:submit property="op" 
							styleClass="btn" 
							accesskey="<%=MSG.accessNextIO() %>" 
							title="<%=MSG.titleNextIO(MSG.accessNextIO()) %>">
						<loc:message name="actionNextIO" />
					</html:submit> 
				</logic:notEmpty>

				<tt:back styleClass="btn" 
						name="<%=MSG.actionBackIODetail() %>" 
						title="<%=MSG.titleBackIODetail(MSG.accessBackIODetail()) %>" 
						accesskey="<%=MSG.accessBackIODetail() %>" 
						type="InstructionalOffering">
					<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId"/>
				</tt:back>				

				</html:form>					
			</TD>
		</TR>

	</TABLE>
</loc:bundle>
