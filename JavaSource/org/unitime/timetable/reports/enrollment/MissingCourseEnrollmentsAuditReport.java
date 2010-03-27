/*
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC, and individual contributors
 * as indicated by the @authors tag.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package org.unitime.timetable.reports.enrollment;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SubjectArea;

import com.lowagie.text.DocumentException;

/**
 * @author says
 *
 */
public class MissingCourseEnrollmentsAuditReport extends PdfEnrollmentAuditReport {

    public MissingCourseEnrollmentsAuditReport(int mode, File file, Session session, TreeSet<SubjectArea> subjectAreas, String subTitle) throws DocumentException, IOException {
        super(mode, getTitle(), file, session, subjectAreas, subTitle);
    }

    public MissingCourseEnrollmentsAuditReport(int mode, File file, Session session) throws DocumentException, IOException {
    	super(mode, getTitle(), file, session);
    }


	@Override
	public void printReport() throws DocumentException {
        int n = iNrLines - 3;
        setHeader(buildHeaderString());
        List results = getAuditResults(getSubjectAreas());
        Vector<String> lines = new Vector<String>();
        Iterator it = results.iterator();
        while(it.hasNext()) {
        	MissingCourseEnrollmentsAuditResult result = new MissingCourseEnrollmentsAuditResult((Object[]) it.next());
        	lines.add(buildLineString(result));
        }
        printHeader();
        for (String str : lines) {
                println(str);
        }
        if (!lines.isEmpty()){
        	lastPage();
        }

	}

	public static String getTitle() {
		return ("Missing Course Enrollments");
	}
	
	private String buildLineString(MissingCourseEnrollmentsAuditResult result) {
		StringBuilder sb = new StringBuilder();
		sb.append(buildBaseAuditLine(result));
		sb.append(" | ")
		  .append(" ")
		  .append(rpad(result.itypeString(), ' ', itypeLength));
		return(sb.toString());
	}

	private String[] buildHeaderString(){
		String[] hdr = new String[3];
		StringBuilder sb0 = new StringBuilder();
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		
		String[] baseHdr = getBaseHeader();
		sb0.append(baseHdr[0]);
		sb1.append(baseHdr[1]);
		sb2.append(baseHdr[2]);
				
		sb0.append(" | ");
		sb1.append(" | ");
		sb2.append(" | ");
		
		sb0.append(" ")
		   .append(rpad("Missing", ' ', itypeLength));
		sb1.append(" ")
		   .append(rpad("Subpart", ' ', itypeLength));
		sb2.append(" ")
		   .append(rpad("", '-', itypeLength));

		hdr[0] = sb0.toString();
		hdr[1] = sb1.toString();
		hdr[2] = sb2.toString();
		
		return(hdr);
	}
	
	protected String createQueryString(TreeSet<SubjectArea> subjectAreas){
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct s.externalUniqueId, s.lastName, s.firstName, s.middleName,")
		  .append(" sce.courseOffering.subjectArea.subjectAreaAbbreviation, sce.courseOffering.courseNbr, sce.courseOffering.title,")
		  .append(" ss.itype.abbv,")
		  .append(" ( select count(sce1) from StudentClassEnrollment sce1")
		  .append(" where sce1.clazz.schedulingSubpart.uniqueId = ss.uniqueId and sce1.student.uniqueId = s.uniqueId ) ")
		  .append(" from Student s inner join s.classEnrollments as sce, SchedulingSubpart ss")
		  .append(" where  ss.instrOfferingConfig.uniqueId = sce.clazz.schedulingSubpart.instrOfferingConfig.uniqueId")
		  .append(" and s.session.uniqueId = :sessId");

		if(subjectAreas != null && !subjectAreas.isEmpty()){
			sb.append(" and sce.courseOffering.subjectArea.uniqueId in (");
			boolean first = true;
			for (SubjectArea sa : subjectAreas){
				if (first){
					first = false;
				} else {
					sb.append(",");
				}
				sb.append(" ");
				sb.append(sa.getUniqueId().toString());
			}
			sb.append(" ) ");
		}

		sb.append(" and 0 = ( select count(sce1) from StudentClassEnrollment sce1")
		  .append(" where sce1.clazz.schedulingSubpart.uniqueId = ss.uniqueId and sce1.student.uniqueId = s.uniqueId )")
		  .append(" order by sce.courseOffering.subjectArea.subjectAreaAbbreviation, sce.courseOffering.courseNbr,")
		  .append(" sce.courseOffering.title, ss.itype.abbv");

		if (isShowId()){
			sb.append(", s.externalUniqueId");
		} else if (isShowName()) {
			sb.append(", s.lastName, s.firstName, s.middleName");
		}

		
		return(sb.toString());
	}

	private class MissingCourseEnrollmentsAuditResult extends EnrollmentAuditResult {
		private String itype;


		public MissingCourseEnrollmentsAuditResult(Object[] result) {
			super(result);
			if (result[7] != null) this.itype = result[7].toString();
		}
				
		public String itypeString(){
			return(itype);
		}
		
	}

}
