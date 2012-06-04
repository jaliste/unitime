<%--
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008-2009, UniTime LLC
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
<%@ page import="org.unitime.timetable.model.DepartmentalInstructor" %>
<%@ page import="org.unitime.timetable.form.ClassInstructorAssignmentForm" %>
<%@ page import="org.unitime.commons.web.Web" %>
<%@ page import="org.unitime.timetable.webutil.JavascriptFunctions" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/tld/timetable.tld" prefix="tt" %>
<%@ taglib uri="/WEB-INF/tld/localization.tld" prefix="loc" %>
<tiles:importAttribute />

<loc:bundle name="CourseMessages">

<% 
	String frmName = "classInstructorAssignmentForm";
	ClassInstructorAssignmentForm frm = (ClassInstructorAssignmentForm)request.getAttribute(frmName);
	String crsNbr = "";
	if (session.getAttribute(Constants.CRS_NBR_ATTR_NAME)!=null )
		crsNbr = session.getAttribute(Constants.CRS_NBR_ATTR_NAME).toString();
%>

<html:form action="/classInstructorAssignment">
<html:hidden name="<%=frmName%>" property="instrOffrConfigId"/>
<html:hidden property="instrOfferingId"/>	
<INPUT type="hidden" name="deletedInstrRowNum" value = "">
<INPUT type="hidden" name="addInstructorId" value = "">
<INPUT type="hidden" name="hdnOp" value = "">

<SCRIPT language="javascript">
	<!--
		<%= JavascriptFunctions.getJsConfirm(Web.getUser(session)) %>
		
		function confirmUnassignAll() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			if (!confirm('<%=MSG.confirmUnassignAllInstructors() %>')) {
				return false;
			}

			return true;
		}

	// -->
</SCRIPT>

	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD colspan="2" valign="middle">
				 <tt:section-header>
					<tt:section-title>
							<A  title="<%=MSG.titleBackToIOList(MSG.accessBackToIOList()) %>" 
								accesskey="<%=MSG.accessBackToIOList() %>"
								class="l8" 
								href="instructionalOfferingShowSearch.do?doit=Search&subjectAreaId=<bean:write name="<%=frmName%>" 
										property="subjectAreaId" />&courseNbr=<%=crsNbr%>#A<bean:write name="<%=frmName%>" property="instrOfferingId" />"
							><bean:write name="<%=frmName%>" property="instrOfferingName" /></A>
					</tt:section-title>						

				<html:submit property="op"
					onclick="return confirmUnassignAll();"
					styleClass="btn" 
					title="<%=MSG.titleUnassignAllInstructorsFromConfig() %>">
					<loc:message name="actionUnassignAllInstructorsFromConfig" />
				</html:submit>
				 
				&nbsp;
				<html:submit property="op"
					styleClass="btn" 
					accesskey="<%=MSG.accessUpdateClassInstructorsAssignment() %>" 
					title="<%=MSG.titleUpdateClassInstructorsAssignment(MSG.accessUpdateClassInstructorsAssignment()) %>">
					<loc:message name="actionUpdateClassInstructorsAssignment" />
				</html:submit>
			
				<bean:define id="instrOfferingId">
					<bean:write name="<%=frmName%>" property="instrOfferingId" />				
				</bean:define>

				<logic:notEmpty name="<%=frmName%>" property="previousId">
					<html:hidden name="<%=frmName%>" property="previousId"/>
					&nbsp;
					<html:submit property="op" 
						styleClass="btn" 
						accesskey="<%=MSG.accessPreviousIO() %>" 
						title="<%=MSG.titlePreviousIOWithUpdate(MSG.accessPreviousIO()) %>">
						<loc:message name="actionPreviousIO" />
					</html:submit> 
				</logic:notEmpty>
				<logic:notEmpty name="<%=frmName%>" property="nextId">
					<html:hidden name="<%=frmName%>" property="nextId"/>
					&nbsp;
					<html:submit property="op" 
						styleClass="btn" 
						accesskey="<%=MSG.accessNextIO() %>" 
						title="<%=MSG.titleNextIOWithUpdate(MSG.accessNextIO()) %>">
						<loc:message name="actionNextIO" />
					</html:submit> 
				</logic:notEmpty>
				 
				&nbsp;
				<html:button property="op" 
					styleClass="btn" 
					accesskey="<%=MSG.accessBackToIODetail() %>" 
					title="<%=MSG.titleBackToIODetail(MSG.accessBackToIODetail()) %>" 
					onclick="document.location.href='instructionalOfferingDetail.do?op=view&io=${instrOfferingId}';">
					<loc:message name="actionBackToIODetail" />
				</html:button>		
				</tt:section-header>					
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
			<TD colspan="2" align="left">
				<TABLE align="left" border="0" cellspacing="0" cellpadding="1">
					<TR>
						<TD align="center" valign="bottom" rowspan="2"> &nbsp;</TD>
						<TD align="center" valign="bottom" rowspan="2"> &nbsp;</TD>
						<TD align="center" valign="bottom" rowspan="2"> &nbsp;</TD>
						<logic:equal name="<%=frmName%>" property="displayExternalId" value="true" >
							<TD rowspan="2">&nbsp;</TD>
							<TD align="center" valign="bottom" rowspan="2"><i><loc:message name="columnExternalId"/></i></TD>
						</logic:equal>
						<TD rowspan="2">&nbsp;</TD>
						<TD rowspan="2">&nbsp;</TD>
						<TD rowspan="2">&nbsp;</TD>
						<TD rowspan="2">&nbsp;</TD>
						<TD align="center" valign="bottom" rowspan="2"><I><loc:message name="columnInstructorName"/></I></TD>
						<TD align="center" valign="bottom" rowspan="2"><I><loc:message name="columnInstructorShare"/></I></TD>
						<TD align="center" valign="bottom" rowspan="2"><I><loc:message name="columnInstructorCheckConflictsBr"/>&nbsp;&nbsp;</I></TD>
						<TD align="center" valign="bottom"><I><loc:message name="columnDisplay"/>&nbsp;</I></TD>
						<TD rowspan="2">&nbsp;</TD>
						<TD align="center" valign="bottom" rowspan="2"><I><loc:message name="columnAssignedTime"/></I></TD>
						<TD rowspan="2">&nbsp;</TD>
						<TD align="center" valign="bottom" rowspan="2"><I><loc:message name="columnAssignedRoom"/></I></TD>
						<TD rowspan="2">&nbsp;</TD>
					</TR>
					<TR>
						<TD align="left" valign="bottom">
							(<loc:message name="propertyAll"/>
							<input type='checkbox' checked='checked' 
									onclick='resetAllDisplayFlags(this.checked);' value='test'>)
						</TD>
					</TR>

					<logic:iterate name="<%=frmName%>" property="classIds" id="c" indexId="ctr">
						<TR onmouseover="this.style.backgroundColor='rgb(223,231,242)';this.style.cursor='default';" onmouseout="this.style.backgroundColor='transparent';">
							<TD nowrap valign="top">
								<html:hidden property='<%= "classIds[" + ctr + "]" %>'/>
								<html:hidden property='<%= "classLabels[" + ctr + "]" %>'/>
								<html:hidden property='<%= "classLabelIndents[" + ctr + "]" %>'/>
								<html:hidden property='<%= "rooms[" + ctr + "]" %>'/>
								<html:hidden property='<%= "times[" + ctr + "]" %>'/>
								<html:hidden property='<%= "allowDeletes[" + ctr + "]" %>'/>
								<html:hidden property='<%= "readOnlyClasses[" + ctr + "]" %>'/>
								<html:hidden property='<%= "classHasErrors[" + ctr + "]" %>'/>
								<html:hidden name="<%=frmName%>" property='<%= "showDisplay[" + ctr + "]" %>' />
								&nbsp;
							</TD>
							<TD nowrap valign="top">
								<logic:equal name="<%=frmName%>" property='<%= "classHasErrors[" + ctr + "]" %>' value="true" >
									<IMG src="images/Error16.jpg">
								</logic:equal>
								<logic:equal name="<%=frmName%>" property='<%= "classHasErrors[" + ctr + "]" %>' value="false" >
									&nbsp;
								</logic:equal>
							</TD>
							<TD nowrap valign="top">
								<%=frm.getClassLabelIndents().get(ctr.intValue()).toString()%>
								<bean:write name="<%=frmName%>" property='<%= "classLabels[" + ctr + "]" %>'/> 
								&nbsp;
							</TD>
	
							<logic:equal name="<%=frmName%>" property="displayExternalId" value="true" >
								<TD>&nbsp;</TD>
								<TD align="left" valign="top" nowrap><%= frm.getExternalIds().get(ctr)%></TD>
							</logic:equal>
							<TD>&nbsp;</TD>
							<TD align="center" valign="top" nowrap>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="false" >
									<logic:equal name="<%=frmName%>" property='<%= "allowDeletes[" + ctr + "]" %>' value="true" >
										<IMG border="0" src="images/Delete16.gif" title="<%=MSG.titleRemoveInstructorFromClass() %>"
											onmouseover="this.style.cursor='hand';this.style.cursor='pointer';"
											onclick="document.forms[0].elements['hdnOp'].value='Delete';
													document.forms[0].elements['deletedInstrRowNum'].value='<%= ctr.toString() %>';
													document.forms[0].submit();">
										</logic:equal>
								</logic:equal>
							</TD>
							<TD align="center" valign="top" nowrap> &nbsp;
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="false" >
									<IMG border="0" src="images/Add16.gif" title="<%=MSG.titleAddInstructorToClass() %>"
										onmouseover="this.style.cursor='hand';this.style.cursor='pointer';"
										onclick="document.forms[0].elements['hdnOp'].value='Add Instructor';
												document.forms[0].elements['addInstructorId'].value='<%= ctr.toString() %>';
												document.forms[0].submit();">
								</logic:equal>
							</TD>
							<TD>&nbsp;<html:hidden property='<%= "externalIds[" + ctr + "]" %>'/></TD>
							<TD align="left" valign="top" nowrap>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="false" >
									<html:select style="width:200;" property='<%= "instructorUids[" + ctr + "]" %>' tabindex="<%=java.lang.Integer.toString(10000 + ctr.intValue())%>">
										<html:option value="<%= Constants.BLANK_OPTION_VALUE%>"><%=Constants.BLANK_OPTION_LABEL%></html:option>
										<html:options collection="<%=DepartmentalInstructor.INSTR_LIST_ATTR_NAME%>" property="uniqueId" labelProperty="nameLastFirst" />
									</html:select>
								</logic:equal>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="true" >
									<logic:iterate scope="request" name="<%=DepartmentalInstructor.INSTR_LIST_ATTR_NAME%>" id="instr">
										<logic:equal name="<%=frmName%>" property='<%= "instructorUids[" + ctr + "]" %>' value="<%=((DepartmentalInstructor)instr).getUniqueId().toString()%>">
											<%=((DepartmentalInstructor)instr).getName(Web.getUser(session))%>
										</logic:equal>
									</logic:iterate>
									<html:hidden property='<%= "instructorUids[" + ctr + "]" %>'/>
								</logic:equal>
							</TD>
							
							<TD align="left" valign="top" nowrap>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="false" >
									<html:text name="<%=frmName%>" property='<%= "percentShares[" + ctr + "]" %>' 
											tabindex="<%=java.lang.Integer.toString(4000 + ctr.intValue())%>" maxlength="5" size="5"/>
								</logic:equal>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="true" >
									<html:hidden property='<%= "percentShares[" + ctr + "]" %>'/>
									<logic:notEmpty name="<%=frmName%>" property='<%= "instructorUids[" + ctr + "]" %>'>
										<bean:write name="<%=frmName%>" property='<%= "percentShares[" + ctr + "]" %>'/>
									</logic:notEmpty>
								</logic:equal>
							</TD>
							<TD align="center" valign="top" nowrap>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="false" >
									<html:checkbox name="<%=frmName%>" property='<%= "leadFlags[" + ctr + "]" %>' 
											tabindex="<%=java.lang.Integer.toString(6000 + ctr.intValue())%>"/>
								</logic:equal>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="true" >
									<html:hidden property='<%= "leadFlags[" + ctr + "]" %>'/>
									<logic:notEmpty name="<%=frmName%>" property='<%= "instructorUids[" + ctr + "]" %>'>
										<bean:write name="<%=frmName%>" property='<%= "leadFlags[" + ctr + "]" %>'/>
									</logic:notEmpty>
								</logic:equal>
							</TD>
							<TD align="center" valign="top" nowrap>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="false" >
									<logic:equal name="<%=frmName%>" property='<%= "showDisplay[" + ctr + "]" %>' value="true" >
										<html:checkbox name="<%=frmName%>" property='<%= "displayFlags[" + ctr + "]" %>' 
												tabindex="<%=java.lang.Integer.toString(8000 + ctr.intValue())%>"/>
									</logic:equal>
								</logic:equal>
								<logic:equal name="<%=frmName%>" property='<%= "readOnlyClasses[" + ctr + "]" %>' value="true" >
									<html:hidden property='<%= "displayFlags[" + ctr + "]" %>'/>
										<bean:write name="<%=frmName%>" property='<%= "displayFlags[" + ctr + "]" %>'/>
								</logic:equal></TD>
							<TD>&nbsp;</TD>
							<TD align="left" valign="top" nowrap>
								&nbsp;
								<bean:write name="<%=frmName%>" property='<%= "times[" + ctr + "]" %>'/>
							</TD>
							<TD>&nbsp;</TD>
							<TD align="left" valign="top" nowrap><%= frm.getRooms().get(ctr)%></TD>
							<TD>&nbsp;</TD>
						</TR>
					</logic:iterate>
				</TABLE>
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
				<html:submit property="op"
					onclick="return confirmUnassignAll();"
					styleClass="btn" 
					title="<%=MSG.titleUnassignAllInstructorsFromConfig() %>">
					<loc:message name="actionUnassignAllInstructorsFromConfig" />
				</html:submit>
			 
				&nbsp;
				<html:submit property="op"
					styleClass="btn" 
					accesskey="<%=MSG.accessUpdateClassInstructorsAssignment() %>" 
					title="<%=MSG.titleUpdateClassInstructorsAssignment(MSG.accessUpdateClassInstructorsAssignment()) %>">
					<loc:message name="actionUpdateClassInstructorsAssignment" />
				</html:submit>
			
				<bean:define id="instrOfferingId">
					<bean:write name="<%=frmName%>" property="instrOfferingId" />				
				</bean:define>

				<logic:notEmpty name="<%=frmName%>" property="previousId">
					<html:hidden name="<%=frmName%>" property="previousId"/>
					&nbsp;
					<html:submit property="op" 
						styleClass="btn" 
						accesskey="<%=MSG.accessPreviousIO() %>" 
						title="<%=MSG.titlePreviousIOWithUpdate(MSG.accessPreviousIO()) %>">
						<loc:message name="actionPreviousIO" />
					</html:submit> 
				</logic:notEmpty>
				<logic:notEmpty name="<%=frmName%>" property="nextId">
					<html:hidden name="<%=frmName%>" property="nextId"/>
					&nbsp;
					<html:submit property="op" 
						styleClass="btn" 
						accesskey="<%=MSG.accessNextIO() %>" 
						title="<%=MSG.titleNextIOWithUpdate(MSG.accessNextIO()) %>">
						<loc:message name="actionNextIO" />
					</html:submit> 
				</logic:notEmpty>

				&nbsp;
				<html:button property="op" 
					styleClass="btn" 
					accesskey="<%=MSG.accessBackToIODetail() %>" 
					title="<%=MSG.titleBackToIODetail(MSG.accessBackToIODetail()) %>" 
					onclick="document.location.href='instructionalOfferingDetail.do?op=view&io=${instrOfferingId}';">
					<loc:message name="actionBackToIODetail" />
				</html:button>		
					
			</TD>
		</TR>

	</TABLE>

</html:form>

<script language='JavaScript'>
      function resetAllDisplayFlags(value) {
            for (var i=0;i<<%=frm.getClassIds().size()%>;i++) {
                  var chbox = document.getElementsByName('displayFlags['+i+']');
                  if (chbox!=null && chbox.length>0)
                        chbox[0].checked = value;
            }
      }
</script>

</loc:bundle>