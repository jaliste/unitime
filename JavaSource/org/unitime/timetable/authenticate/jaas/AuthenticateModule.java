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
package org.unitime.timetable.authenticate.jaas;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.spi.LoginModule;

/**
 * 
 * @author Heston Fernandes
 *
 */
public abstract class AuthenticateModule implements LoginModule {

    // --------------------------------------------------------- Instance Variables

	// Initialization Parameters
	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	private Map sharedState;
	private Map options;
	
	// Config option
	private boolean debug;
	
	// Flags
	private boolean authSucceeded;
	private boolean commitSucceeded;
	
	// User attributes
	private String user;

    // --------------------------------------------------------- Abstract Methods
	
	/**
	 * Actual method that does the authentication
	 * @param userProps All user properties necessary for authentication
	 */
	public abstract boolean doAuthenticate(HashMap userProps) throws Exception;
	
    // --------------------------------------------------------- Methods

	/**
	 * Initialize
	 */
	public void initialize(
			Subject subject, 
			CallbackHandler callbackHandler,
			Map sharedState, 
			Map options ) {
		
		setSubject(subject);
		setCallbackHandler(callbackHandler);
		setSharedState(sharedState);
		setOptions(options);
		
		setUser(null);
		setAuthSucceeded(false);
		setCommitSucceeded(false);
		setDebug(false);
		
		// Check if debug option is set in jaas config
		String d = (String)options.get("debug");
		if (d!=null && d.equalsIgnoreCase("true"))
			setDebug(true);
	}
	
	/**
	 * Resets user attributes and status flags
	 */
    protected void reset() {
		setUser(null);
		setAuthSucceeded(false);
		setCommitSucceeded(false);
	}

	
	public boolean isAuthSucceeded() {
		return authSucceeded;
	}
	public void setAuthSucceeded(boolean authSucceeded) {
		this.authSucceeded = authSucceeded;
	}
	
	public CallbackHandler getCallbackHandler() {
		return callbackHandler;
	}
	public void setCallbackHandler(CallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}
	
	public boolean isCommitSucceeded() {
		return commitSucceeded;
	}
	public void setCommitSucceeded(boolean commitSucceeded) {
		this.commitSucceeded = commitSucceeded;
	}
	
	public boolean isDebug() {
		return debug;
	}
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public Map getOptions() {
		return options;
	}
	public void setOptions(Map options) {
		this.options = options;
	}
	
	public Map getSharedState() {
		return sharedState;
	}
	public void setSharedState(Map sharedState) {
		this.sharedState = sharedState;
	}
	
	public Subject getSubject() {
		return subject;
	}
	public void setSubject(Subject subject) {
		this.subject = subject;
	}
	
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}	
}
