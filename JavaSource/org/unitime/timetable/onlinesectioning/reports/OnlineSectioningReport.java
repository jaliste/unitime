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
package org.unitime.timetable.onlinesectioning.reports;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author Tomas Muller
 */
public class OnlineSectioningReport implements Runnable {
	protected static Logger sLog = Logger.getLogger(OnlineSectioningReport.class);
	protected static DecimalFormat sDF = new DecimalFormat("0.00000");
	private Report iReport = null;
	
	private Map<String, Counter> iCounters = new Hashtable<String, Counter>();
	private Map<String, Map<String, Map<String, Counter>>> iReports = new Hashtable<String, Map<String,Map<String,Counter>>>();
	
	public OnlineSectioningReport(Report report) throws Exception {
		iReport = report;
	}
	
	public void run() {
        Properties props = new Properties();
        props.setProperty("log4j.rootLogger", "DEBUG, A1");
        props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.A1.layout.ConversionPattern","%-5p %c{2}: %m%n");
        props.setProperty("log4j.logger.org.hibernate","INFO");
        props.setProperty("log4j.logger.org.hibernate.cfg","WARN");
        props.setProperty("log4j.logger.org.hibernate.cache.EhCacheProvider","ERROR");
        props.setProperty("log4j.logger.org.unitime.commons.hibernate","INFO");
        props.setProperty("log4j.logger.net","INFO");
        PropertyConfigurator.configure(props);
        
        try {
        	HibernateUtil.configureHibernate(ApplicationProperties.getProperties());
        } catch (Exception e) {
        	sLog.fatal("Unable to initialize hibernate: " + e.getMessage(), e);
        	return;
        }

        org.hibernate.Session hibSession = new _RootDAO().getSession();
        
        Session session = Session.getSessionUsingInitiativeYearTerm(
                iReport.getCampus(),
                iReport.getYear(),
                iReport.getTerm()
                );
        if (session==null) {
        	sLog.fatal("Academic session not found:" + iReport.getTerm() + " " + iReport.getYear() + " (" + iReport.getCampus() + ")");
        	return;
        } else {
            sLog.info("Session: "+session);
        }
        
        Long sessionId = session.getUniqueId();
        
		String student = null;
		String filter = null;
		String[] operations = iReport.getOperations();
		if (operations == null) {
			filter = "";
		} else if (operations.length == 1) {
			filter = "and l.operation = '" + operations[0] + "'";
		} else {
			filter = "and l.operation in (";
			for (int i = 0; i < operations.length; i++) {
				filter += (i > 0 ? "," : "") + "'" + operations[i] + "'";
			}
			filter += ")";
		}
		
		List<OnlineSectioningLog.Action> actions = new ArrayList<OnlineSectioningLog.Action>();
		for (Iterator<org.unitime.timetable.model.OnlineSectioningLog> i = hibSession.createQuery(
				"select l from OnlineSectioningLog l where " +
				"l.session.uniqueId = :sessionId " + filter +
				" order by l.student, l.timeStamp")
				.setLong("sessionId", sessionId).iterate(); i.hasNext(); ) {
			org.unitime.timetable.model.OnlineSectioningLog l = i.next();
			if (student == null) {
				student = l.getStudent();
			} else if (!student.equals(l.getStudent())) {
				if (!actions.isEmpty())
					iReport.process(this, student, actions);
				actions.clear(); student = l.getStudent();
			}
			try {
				actions.add(OnlineSectioningLog.Action.parseFrom(l.getAction()));
			} catch (InvalidProtocolBufferException e) {
				sLog.error("Unable to parse action for " + student + " (op=" + l.getOperation() + ", ts=" + l.getTimeStamp() + "): " + e.getMessage());
			}
		}
		if (!actions.isEmpty())
			iReport.process(this, student, actions);
		
		// Write counters
		for (String name: new TreeSet<String>(iCounters.keySet())) {
			sLog.info(name + ": " + iCounters.get(name));
		}
		
		// Write reports
		for (Map.Entry<String, Map<String, Map<String, Counter>>> report: iReports.entrySet()) {
			TreeSet<String> rows = new TreeSet<String>(report.getValue().keySet());
			TreeSet<String> cols = new TreeSet<String>();
			CSVFile csv = new CSVFile();
			for (Map.Entry<String, Map<String, Counter>> record: report.getValue().entrySet()) {
				cols.addAll(record.getValue().keySet());
			}
			List<CSVField> header = new ArrayList<CSVField>();
			header.add(new CSVField(report.getKey()));
			for (String col: cols)
				header.add(new CSVField(col));
			csv.setHeader(header);
			for (String row: rows) {
				List<CSVField> line = new ArrayList<CSVField>();
				line.add(new CSVField(row));
				Map<String, Counter> table = report.getValue().get(row);
				for (String col: cols) {
					Counter counter = table.get(col);
					line.add(new CSVField(counter == null ? "" : String.valueOf(counter.sum())));
				}
				csv.addLine(line);
			}
			try {
				File output = new File(iReport.getReportFolder(), report.getKey() + ".csv");
				sLog.info("Writing " + output + " ...");
				csv.save(output);
			} catch (IOException e) {
				sLog.error("Unable to write report " + report.getKey() + ": " + e.getMessage(), e);
			}
		}

		hibSession.close();
		HibernateUtil.closeHibernate();
	}

	public void inc(String counter, double value) {
		Counter cnt = iCounters.get(counter);
		if (cnt == null) {
			cnt = new Counter();
			iCounters.put(counter, cnt);
		}
		cnt.inc(value);
	}
	
	public void inc(String report, String record, String property, double value) {
		Map<String, Map<String, Counter>> table = iReports.get(report);
		if (table == null) {
			table = new Hashtable<String, Map<String,Counter>>();
			iReports.put(report, table);
		}
		Map<String, Counter> line = table.get(record);
		if (line == null) {
			line = new Hashtable<String, Counter>();
			table.put(record, line);
		}
		Counter counter = line.get(property);
		if (counter == null) {
			counter = new Counter();
			line.put(property, counter);
		}
		counter.inc(value);
	}

	public static interface Report {
		public File getReportFolder();
		public String getYear();
		public String getTerm();
		public String getCampus();
		public String[] getOperations();
		public void process(OnlineSectioningReport report, String student, List<OnlineSectioningLog.Action> actions);
	}
	
	public static class Counter {
		private double iTotal = 0.0, iMin = 0.0, iMax = 0.0, iTotalSquare = 0.0;
		private int iCount = 0;
		
		public Counter() {
		}
		
		public void inc(double value) {
			if (iCount == 0) {
				iTotal = value;
				iMin = value;
				iMax = value;
				iTotalSquare = value * value;
			} else {
				iTotal += value;
				iMin = Math.min(iMin, value);
				iMax = Math.max(iMax, value);
				iTotalSquare += value * value;
			}
			iCount ++;
		}
		
		public int count() { return iCount; }
		public double sum() { return iTotal; }
		public double min() { return iMin; }
		public double max() { return iMax; }
		public double rms() { return (iCount == 0 ? 0.0 : Math.sqrt(iTotalSquare / iCount)); }
		public double avg() { return (iCount == 0 ? 0.0 : iTotal / iCount); }
		
		public String toString() {
			return sDF.format(sum()) +
			" (min: " + sDF.format(min()) +
			", max: " + sDF.format(max()) +
			", avg: " + sDF.format(avg()) +
			", rms: " + sDF.format(rms()) +
			", cnt: " + count() + ")";
		}
	}
	
}
