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

import org.unitime.commons.Debug;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.dao.StudentClassEnrollmentDAO;

import com.lowagie.text.DocumentException;

/**
 * @author says
 *
 */
public class MultipleCourseEnrollmentsAuditReport extends PdfEnrollmentAuditReport {

    public MultipleCourseEnrollmentsAuditReport(int mode, File file, Session session, TreeSet<SubjectArea> subjectAreas, String subTitle) throws DocumentException, IOException {
        super(mode, getTitle(), file, session, subjectAreas, subTitle);
    }

    public MultipleCourseEnrollmentsAuditReport(int mode, File file, Session session) throws DocumentException, IOException {
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
        	MultipleCourseEnrollmentsAuditResult result = new MultipleCourseEnrollmentsAuditResult((Object[]) it.next());
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
	
	@SuppressWarnings("unchecked")
	@Override
	protected List getAuditResults(TreeSet<SubjectArea> subjectAreas){
		TreeSet<SubjectArea> subjects = new TreeSet<SubjectArea>();
		if (subjectAreas != null && !subjectAreas.isEmpty()){
			subjects.addAll(subjectAreas);
		} else {
			subjects.addAll(SubjectArea.getSubjectAreaList(getSession().getUniqueId()));
		}

		String query = createQueryString(subjects);
		Vector results = new Vector();
		for (SubjectArea sa : subjects){
			Debug.info(getTitle() + " - Checking Subject Area:  " + sa.getSubjectAreaAbbreviation());
			results.addAll(StudentClassEnrollmentDAO.getInstance()
				 .getQuery(query)
				 .setLong("sessId", getSession().getUniqueId().longValue())
				 .setLong("subjectId", sa.getUniqueId().longValue())
				 .list());
		}
		return(results);
	}

	public static String getTitle() {
		return ("Multiple Course Enrollments");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String createQueryString(TreeSet<SubjectArea> subjectAreas) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct s.externalUniqueId, s.lastName, s.firstName, s.middleName,")
		  .append(" sce.courseOffering.subjectArea.subjectAreaAbbreviation, sce.courseOffering.courseNbr, sce.courseOffering.title,")
		  .append(" s.uniqueId, ss.itype.abbv, ss.uniqueId,")
		  .append(" ( select count(sce1) from StudentClassEnrollment sce1")
		  .append(" where sce1.clazz.schedulingSubpart.uniqueId = ss.uniqueId and sce1.student.uniqueId = s.uniqueId ) ")
		  .append(" from Student s inner join s.classEnrollments as sce, SchedulingSubpart ss")
		  .append(" where  ss.instrOfferingConfig.uniqueId = sce.clazz.schedulingSubpart.instrOfferingConfig.uniqueId")
		  .append(" and s.session.uniqueId = :sessId")
		  .append(" and sce.courseOffering.subjectArea.uniqueId = :subjectId")
		  .append(" and 1 < ( select count(sce1) from StudentClassEnrollment sce1")
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
	
	private String buildLineString(MultipleCourseEnrollmentsAuditResult result) {
		StringBuilder sb = new StringBuilder();
		sb.append(buildBaseAuditLine(result));
		sb.append(" | ")
		  .append(" ")
		  .append(rpad(result.classesListStr(), ' ', multipleClassesLength));
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
		   .append(rpad("Multiple Classes", ' ', multipleClassesLength));
		sb1.append(" ")
		   .append(rpad("of Same Subpart", ' ', multipleClassesLength));
		sb2.append(" ")
		   .append(rpad("", '-', multipleClassesLength));

		hdr[0] = sb0.toString();
		hdr[1] = sb1.toString();
		hdr[2] = sb2.toString();
		
		return(hdr);
	}

	private class MultipleCourseEnrollmentsAuditResult extends EnrollmentAuditResult {
		private String itype;
		private Long studentUniqueId;
		private Long subpartId;
		private java.util.Vector<String> classes = new java.util.Vector<String>();


		public MultipleCourseEnrollmentsAuditResult(Object[] result) {
			super(result);
			if (result[7] != null) this.studentUniqueId = new Long(result[7].toString());
			if (result[8] != null) this.itype = result[8].toString();
			if (result[9] != null) this.subpartId = new Long(result[9].toString());
			findClasses();
		}
				
		private void findClasses(){
			StringBuilder sb = new StringBuilder();
			sb.append("select sce.clazz.schedulingSubpart.itype.abbv, sce.clazz.sectionNumberCache,  sce.clazz.schedulingSubpart.schedulingSubpartSuffixCache")
			  .append(" from StudentClassEnrollment sce where sce.student.uniqueId = :studId and sce.clazz.schedulingSubpart.uniqueId = :subpartId")
			  .append(" order by sce.clazz.sectionNumberCache,  sce.clazz.schedulingSubpart.schedulingSubpartSuffixCache");
			Iterator it = StudentClassEnrollmentDAO.getInstance()
					.getQuery(sb.toString())
					.setLong("studId", studentUniqueId.longValue())
					.setLong("subpartId", subpartId.longValue())
					.iterate();
			while (it.hasNext()){
				Object[] result = (Object[]) it.next();
				String className = createClassString(result[0].toString(), result[1].toString(), result[2].toString());
				classes.add(className);
			}
			
		}
		public String itypeString(){
			return(itype);
		}
		
		public String classesListStr(){
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String clazz : classes){
				if (first){
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(clazz);
			}
			return(sb.toString());
		}
		
	}

}
