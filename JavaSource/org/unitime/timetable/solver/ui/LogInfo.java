/*
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime LLC, and individual contributors
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
package org.unitime.timetable.solver.ui;

import java.io.Serializable;
import java.util.*;

import org.dom4j.Element;

import net.sf.cpsolver.ifs.util.Progress;

/**
 * @author Tomas Muller
 */
public class LogInfo implements TimetableInfo, Serializable {
	private static final long serialVersionUID = 1L;
	public static int sVersion = 1; // to be able to do some changes in the future
	public static int sNoSaveThreshold = Progress.MSGLEVEL_DEBUG;
	private Vector iLog = new Vector();
	
	public void setLog(Vector log) { iLog = log; }
	public Vector getLog() { return iLog; }
	
    public String getLog(int level) {
    	StringBuffer sb = new StringBuffer(); 
    	for (Enumeration e=iLog.elements();e.hasMoreElements();) {
    		Progress.Message m = (Progress.Message)e.nextElement();
    		String s = m.toString(level);
    		if (s!=null) sb.append(s+"\n");
    	}
    	return sb.toString();
    }
    
    public String getHtmlLog(int level, boolean includeDate) {
    	StringBuffer sb = new StringBuffer(); 
    	for (Enumeration e=iLog.elements();e.hasMoreElements();) {
    		Progress.Message m = (Progress.Message)e.nextElement();
    		String s = m.toHtmlString(level, includeDate);
    		if (s!=null) sb.append(s+"<br>");
    	}
    	return sb.toString();
    }
	
    public String getHtmlLog(int level, boolean includeDate, String fromStage) {
    	StringBuffer sb = new StringBuffer(); 
    	for (Enumeration e=iLog.elements();e.hasMoreElements();) {
    		Progress.Message m = (Progress.Message)e.nextElement();
    		if (m.getLevel()==Progress.MSGLEVEL_STAGE && m.getMessage().equals(fromStage))
    			sb = new StringBuffer();
    		String s = m.toHtmlString(level, includeDate);
    		if (s!=null) sb.append(s+"<br>");
    	}
    	return sb.toString();
    }

	public void load(Element root) throws Exception {
		/*
		XMLWriter writer = new XMLWriter(System.out, OutputFormat.createPrettyPrint());
		writer.write(root.getDocument());
		writer.flush();
		*/
		iLog.clear();
		int version = Integer.parseInt(root.attributeValue("version"));
		if (version==1) {
			for (Iterator i=root.elementIterator("msg");i.hasNext();)
				iLog.add(new Progress.Message((Element)i.next()));
		}		
	}
	public void save(Element root) throws Exception {
		root.addAttribute("version", String.valueOf(sVersion));
		for (Enumeration e=iLog.elements();e.hasMoreElements();) {
			Progress.Message msg = (Progress.Message)e.nextElement();
			if (msg.getLevel()<=sNoSaveThreshold) continue;
			msg.save(root.addElement("msg"));
		}
	}

	public boolean saveToFile() {
		return false;
	}
}
