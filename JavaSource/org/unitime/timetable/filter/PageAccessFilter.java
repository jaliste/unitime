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
package org.unitime.timetable.filter;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.TilesUtil;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.unitime.commons.User;
import org.unitime.commons.web.Web;
import org.unitime.timetable.util.Constants;


public class PageAccessFilter implements Filter {
	private static Log sLog = LogFactory.getLog(PageAccessFilter.class);
	private static DecimalFormat sDF = new DecimalFormat("0.00");
	private ServletContext iContext;
	private Hashtable<String, String> iPath2Tile = new Hashtable<String, String>();
	private long debugTime = 30000; // Print info about the page if the page load took at least this time.
	private long dumpTime = 300000; // Print debug info about the page if the page load took at least this time.
	private boolean dumpSessionAttribues = false; // Include session attributes in the dump.
	
	public void init(FilterConfig cfg) throws ServletException {
		iContext = cfg.getServletContext();
		try {
			Document config = (new SAXReader()).read(cfg.getServletContext().getResource(cfg.getInitParameter("config")));
			for (Iterator i=config.getRootElement().element("action-mappings").elementIterator("action"); i.hasNext();) {
				Element action = (Element)i.next();
				String path = action.attributeValue("path");
				String input = action.attributeValue("input");
				if (path!=null && input!=null) {
					iPath2Tile.put(path+".do", input);
				}
			}
		} catch (Exception e) {
			sLog.error("Unable to read config "+cfg.getInitParameter("config")+", reason: "+e.getMessage());
		}
		if (cfg.getInitParameter("debug-time")!=null) {
			debugTime = Long.parseLong(cfg.getInitParameter("debug-time"));
		}
		if (cfg.getInitParameter("dump-time")!=null) {
			dumpTime = Long.parseLong(cfg.getInitParameter("dump-time"));
		}
		if (cfg.getInitParameter("session-attributes")!=null) {
			dumpSessionAttribues = Boolean.parseBoolean(cfg.getInitParameter("session-attributes"));
		}
	}
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {
		long t0 = System.currentTimeMillis();
		
		if (request instanceof HttpServletRequest) {
			HttpServletRequest r = (HttpServletRequest)request;
			if (r.getRequestURI().endsWith(".do")) {
				HttpServletResponse x = (HttpServletResponse)response;
				String def = r.getRequestURI().substring(r.getContextPath().length());
				try {
					if (iPath2Tile.containsKey(def)) {
						String tile = iPath2Tile.get(def);
						ComponentDefinition c = TilesUtil.getDefinition(tile, request, iContext);
						HttpSession s = r.getSession();
						if (c!=null && "true".equals(c.getAttribute("checkLogin"))) {
							if (!Web.isLoggedIn(s)) {
								sLog.warn("Page "+r.getRequestURI()+" denied: user not logged in");
								if (s.isNew()) 
									x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=Your+timetabling+session+has+expired.+Please+log+in+again."));
								else
									x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=Login+is+required+to+use+timetabling+application."));
								return;
							}
						}
						if (c!=null && "true".equals(c.getAttribute("checkRole"))) {
							User user = Web.getUser(s);
							if (user==null || user.getCurrentRole()==null) {
								sLog.warn("Page "+r.getRequestURI()+" denined: no role");
								x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=Insufficient+user+privileges."));
								return;
							}
						}
						if (c!=null && "true".equals(c.getAttribute("checkAdmin"))) {
							User user = Web.getUser(s);
							if (user==null || !user.isAdmin()) {
								sLog.warn("Page "+r.getRequestURI()+" denied: user not admin");
								x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=Insufficient+user+privileges."));
								return;
							}
						}
						if (c!=null && "true".equals(c.getAttribute("checkAccessLevel"))) {
							String appAccess = (String) s.getAttribute(Constants.SESSION_APP_ACCESS_LEVEL);
							if (appAccess!=null && !"true".equalsIgnoreCase(appAccess)) {
								sLog.warn("Page "+r.getRequestURI()+" denied: application access disabled");
								x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=The+application+is+temporarily+unavailable.+Please+try+again+after+some+time."));
								return;
							}
						}
					}
				} catch (Exception e) {
					sLog.warn("Unable to check page access for "+r.getRequestURI()+", reason: "+e.getMessage(), e);
				}
			}
		}
		
		// Process request
		Throwable exception = null;
		try {
			chain.doFilter(request,response);
		} catch (Throwable t) {
			exception = t;
		}
		
		long t1 = System.currentTimeMillis(); 
		if (request instanceof HttpServletRequest && ((t1-t0)>debugTime || exception!=null)) {
			HttpServletRequest r = (HttpServletRequest)request;
			String message = "Page "+r.getRequestURI()+" took "+sDF.format((t1-t0)/1000.0)+" s.";
			if (exception!=null) {
				message = exception+" seen on page "+r.getRequestURI()+" (page took "+sDF.format((t1-t0)/1000.0)+" s).";
			}
			if (exception!=null || (t1-t0)>dumpTime) {
				User u = null;
				try {
					u = (r.getSession() == null ? null : Web.getUser(r.getSession()));
				} catch (IllegalStateException e) {}
				if (u==null) {
					message += "\n  User: no user";
				} else {
					message += "\n  User: "+u.getLogin()+(u.getCurrentRole()!=null?" ("+u.getCurrentRole()+")":u.isAdmin()?"(admin)":"");
				}
				message += "\n  Request parameters:";
				for (Enumeration e=r.getParameterNames(); e.hasMoreElements();) {
					String n = (String)e.nextElement();
					if ("password".equals(n)) continue;
					message+="\n    "+n+"="+r.getParameter(n);
				}
				try {
					if (dumpSessionAttribues && r.getSession() != null) {
						message += "\n  Session attributes:";
						for (Enumeration e=r.getSession().getAttributeNames(); e.hasMoreElements();) {
							String n = (String)e.nextElement();
							if (n.equals("userTrace")) continue;
							message+="\n    "+n+"="+r.getSession().getAttribute(n);
						}
					}
				} catch (IllegalStateException e) {
					message += "\n    INVALID SESSION";
				}
			} else {
				User u = Web.getUser(r.getSession());
				if (u==null) {
					message += "  (User: no user)";
				} else {
					message += "  (User: "+u.getLogin()+(u.getCurrentRole()!=null?", "+u.getCurrentRole():u.isAdmin()?", admin":"")+")";
				}
			}
			if (exception!=null)
				sLog.warn(message);
			else
				sLog.info(message);
		}		

		if (exception!=null) {
			if (exception instanceof ServletException)
				throw (ServletException)exception;
			if (exception instanceof IOException)
				throw (IOException)exception;
			if (exception instanceof RuntimeException)
				throw (RuntimeException)exception;
			throw new ServletException(exception);
		}
	}

	public void destroy() {
	}
}
