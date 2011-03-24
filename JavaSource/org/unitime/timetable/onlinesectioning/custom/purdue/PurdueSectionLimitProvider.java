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
package org.unitime.timetable.onlinesectioning.custom.purdue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.cpsolver.studentsct.model.Section;

import org.apache.log4j.Logger;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.gwt.shared.SectioningExceptionType;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.custom.SectionLimitProvider;
import org.unitime.timetable.onlinesectioning.custom.SectionUrlProvider;

/**
 * @author Tomas Muller
 */
public class PurdueSectionLimitProvider implements SectionLimitProvider, SectionUrlProvider {
    private static Logger sLog = Logger.getLogger(PurdueSectionLimitProvider.class);

	public static String sUrl = "https://esa-oas-prod-wl.itap.purdue.edu/prod/bzwsrch.p_schedule_detail?term=:year:term&crn=:crn";
	public static String sDummyUrl = "https://esa-oas-prod-wl.itap.purdue.edu/prod/bzwsrch.p_schedule_detail?term=201010&crn=10001";
	public static String sContentRE = "(<table [ ]*class=\"[a-z]*\" summary=\"This layout table is used to present the seating numbers.\" .*</table>)";
	public static String sTableRE = "<td class=\"dddefault\">(\\-?[0-9]*)</td>";
	private Pattern iContentRE = Pattern.compile(sContentRE, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNIX_LINES);
	private Pattern iTableRE = Pattern.compile(sTableRE, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNIX_LINES);
	public static int sConcurrencyLimit = 10;
	
	private Hashtable<Long, int[]> iCache = new Hashtable<Long, int[]>();

	private String getTerm(AcademicSessionInfo session) throws SectioningException {
		if (session.getTerm().toLowerCase().startsWith("spr")) return "20";
		if (session.getTerm().toLowerCase().startsWith("sum")) return "30";
		if (session.getTerm().toLowerCase().startsWith("fal")) return "10";
		throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "academic term "+session.getTerm()+" not known");
	}
	
	private String getYear(AcademicSessionInfo session) throws SectioningException {
		if (session.getTerm().toLowerCase().startsWith("fal"))
			return String.valueOf(Integer.parseInt(session.getYear()) + 1);
		return session.getYear();
	}
	
	@Override
	public URL getSectionUrl(AcademicSessionInfo session, Long courseId, Section section) {
		return getSectionUrl(session, courseId, section.getId(), section.getName(courseId));
	}
	
	protected URL getSectionUrl(AcademicSessionInfo session, Long courseId, Long classId, String className) {
		try {
			if (className == null || className.isEmpty()) throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "class CRN not provided");
			String crn = className;
			if (className.indexOf('-') >= 0)
				crn = className.substring(0, className.indexOf('-'));
			URL url = new URL(sUrl
				.replace(":year", getYear(session))
				.replace(":term", getTerm(session))
				.replace(":initiative", session.getCampus())
				.replace(":crn", crn));
			return url;
		} catch (MalformedURLException e) {
			throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "course detail url is wrong");
		}
	}
	
	@Override
	public int[] getSectionLimit(AcademicSessionInfo session, Long courseId, Section section) throws SectioningException {
		return getSectionLimit(session, courseId, section.getId(), section.getName(courseId));
	}
	
	protected int[] getSectionLimit(AcademicSessionInfo session, Long courseId, Long classId, String className) throws SectioningException {
		int[] ret = getSectionLimit(getSectionUrl(session, courseId, classId, className));
		iCache.put(classId, ret);
		return ret;
	}

	protected int[] getSectionLimit(URL secionUrl) throws SectioningException {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(secionUrl.openStream()));
			StringBuffer content = new StringBuffer();
			String line;
			while ((line = in.readLine()) != null)
				content.append(line);
			in.close();
			
			Matcher match = iContentRE.matcher(content);
			if (!match.find()) throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "unable to parse <a href='"+secionUrl+"'>class detial page</a>");
			String table = match.group(1);

			match = iTableRE.matcher(table);
			if (!match.find()) throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "unable to parse <a href='"+secionUrl+"'>class detial page</a>");
			int capacity = Integer.parseInt(match.group(1));
			if (!match.find()) throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "unable to parse <a href='"+secionUrl+"'>class detial page</a>");
			int actual = Integer.parseInt(match.group(1));
			if (!match.find()) throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "unable to parse <a href='"+secionUrl+"'>class detial page</a>");
//			int remaning = Integer.parseInt(match.group(1));
			
			return new int[] {actual, capacity};
		} catch (IOException e) {
			throw new SectioningException(SectioningExceptionType.CUSTOM_SECTION_LIMITS_FAILURE, "unable to read <a href='"+secionUrl+"'>class detial page</a>");
		}
	}
	
	@Override
	public Map<Long, int[]> getSectionLimits(AcademicSessionInfo session, Long courseId, Collection<Section> sections) {
		Hashtable<Long, int[]> ret = new Hashtable<Long, int[]>();
		ThreadPool pool = new ThreadPool();
		for (Section section: sections) {
			pool.retrieveLimit(session, courseId, section.getId(), section.getName(courseId), ret);
		}
		pool.waitForAll();
		return ret;
	}

	@Override
	public Map<Long, int[]> getSectionLimitsFromCache(AcademicSessionInfo session, Long courseId, Collection<Section> sections) {
		Hashtable<Long, int[]> ret = new Hashtable<Long, int[]>();
		ThreadPool pool = new ThreadPool();
		for (Section section: sections) {
			int[] limits = iCache.get(section.getId());
			if (limits != null) {
				ret.put(section.getId(), limits);
			} else {
				pool.retrieveLimit(session, courseId, section.getId(), section.getName(courseId), ret);
			}
		}
		pool.waitForAll();
		return ret;
	}

	private class ThreadPool {
		private Set<Worker> iWorkers = new HashSet<Worker>();
		
		private void retrieveLimit(AcademicSessionInfo session, Long courseId, Long classId, String customClassSuffix, Hashtable<Long, int[]> ret) {
			synchronized (iWorkers) {
				while (iWorkers.size() > sConcurrencyLimit) {
					try {
						iWorkers.wait();
					} catch (InterruptedException e) {}
				}
				Worker w = new Worker(session, courseId, classId, customClassSuffix, ret);
				iWorkers.add(w);
				w.start();
			}
		}
		
		private void done(Worker w) {
			synchronized (iWorkers) {
				iWorkers.remove(w);
				iWorkers.notify();
			}
		}
		
		private void waitForAll() {
			synchronized (iWorkers) {
				while (!iWorkers.isEmpty()) {
					try {
						iWorkers.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
	
		private class Worker extends Thread {
			private AcademicSessionInfo iSession;
			private Long iCourseId, iClassId;
			private String iClassName;
			private Hashtable<Long, int[]> iResults;
			
			private Worker(AcademicSessionInfo session, Long courseId, Long classId, String className, Hashtable<Long, int[]> ret) {
				iSession = session;
				iCourseId = courseId;
				iClassId = classId;
				iClassName = className;
				iResults = ret;
				setName("PuSectLimitP-" + classId);
			}
			
			@Override
			public void run() {
				try {
					int[] limit = getSectionLimit(iSession, iCourseId, iClassId, iClassName);
					iResults.put(iClassId, limit);
				} catch (SectioningException e) {
					sLog.warn("Failed to retrieve section limit for "+iClassName+" ("+iSession.getTerm()+" "+iSession.getYear()+"): "+e.getMessage());
				} finally {
					done(this);
				}
			}
		}
	}

}
