/*
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2007, UniTime.org, and individual contributors
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.ManagerRole;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.TimetableManager;

/**
 * @author Tomas Muller
 */
public class LdapAuthenticateModule extends AuthenticateModule {
	private String iExternalUid;
	
	/**
	 * Abort authentication (when overall authentication fails)
	 */
	public boolean abort() throws LoginException {
		if (!isAuthSucceeded()) return false;
		if (isAuthSucceeded() && !isCommitSucceeded()) reset();
		else logout();
		return true;
	}

	/**
	 * Commit phase of login
	 */
	public boolean commit() throws LoginException {
		
		// Check if authentication succeeded
		if (isAuthSucceeded()) {		
			
			// External UID must exist in order to get manager info
			if (iExternalUid==null || iExternalUid.trim().length()==0)
				throw new LoginException ("External Uid not found");
			
			// Get manager info			
			String name = "";
			String role = "";
			String dept = "";
			
			try {
				TimetableManager tm = TimetableManager.findByExternalId(iExternalUid);
				
				name = tm.getName();
				
				// Get roles
				if (tm.getManagerRoles() != null){					
					if (isDebug()) System.out.println("User roles found ");					
					ManagerRole mr = null;
					
					Iterator it2 = tm.getManagerRoles().iterator();
					while (it2.hasNext()){
						mr = (ManagerRole) it2.next();
					    Roles roles = mr.getRole();
					    role = role + roles.getReference() + "-";
					}
				}
				
				// Get departments
				if (tm.getDepartments()!=null) {
					if (isDebug()) System.out.println("User departments found ");					
					Set ds = tm.getDepartments();
					for (Iterator it3=ds.iterator(); it3.hasNext(); ) {
						Department d = (Department) it3.next();
						String deptCode = d.getDeptCode();
						if (deptCode!=null && dept.indexOf(deptCode)<0) dept += d.getDeptCode() + " ";
					}
				}
			} catch(Exception e) {
				throw new LoginException(e.getMessage());
			}
			
			// Check at least one role is found
			if ( role==null || role.trim().length()==0 ) {
				if (isDebug()) System.out.println("Role not found. Access Denied to User: " + getUser());
				throw new LoginException ("Role not found. Access Denied to User: " + getUser());
			}

			if(role.endsWith("-"))
			    role = role.substring(0, role.length()-1);

			// Create user principal
			if (isDebug()) System.out.println("User Roles: " + role);
			if (isDebug()) System.out.println("User Depts: " + dept);

			Vector roles = new Vector();
			Vector depts = new Vector();

			org.unitime.commons.User p = new org.unitime.commons.User();
			
		    p.setLogin(getUser());
		    p.setId(iExternalUid);
		    p.setName(name);
		    p.setAdmin(false);
		    
			StringTokenizer strTok = new StringTokenizer(dept, " ");
			while(strTok.hasMoreElements())
			    depts.addElement(strTok.nextElement().toString());

			strTok = new StringTokenizer(role, "-");
			while(strTok.hasMoreElements()) {
				String role1 = strTok.nextElement().toString();
				roles.addElement(role1);
			    if(role1.equals(Roles.ADMIN_ROLE)) {
					if (isDebug()) System.out.println("User is admin ");
			        p.setAdmin(true);
			    }
			}

			p.setDepartments(depts);
			p.setRoles(roles);

			// Add user object to subjects public credentials
			getSubject().getPublicCredentials().add(p);
			
		    setCommitSucceeded(true);
			return true;
		}
		
		// Authentication failed - do not commit 
		else {
			reset();
			return false;
		}
	}

	/**
	 * Initialize
	 */
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options ) {
		
		super.initialize(subject, callbackHandler, sharedState, options);
		iExternalUid = null;
	}

	/**
	 * Authenticate the user
	 */
	public boolean login() throws LoginException {

		if (isDebug()) System.out.println("Performing ldap authentication ... ");

		// Get callback parameters
		if (getCallbackHandler() == null)
		    throw new LoginException("Error: no CallbackHandler available ");
		
		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("User Name: ");
		callbacks[1] = new PasswordCallback("Password: ", true);

		
		try {
			getCallbackHandler().handle(callbacks);
			String n = ((NameCallback) callbacks[0]).getName();
			String p = String.valueOf(((PasswordCallback) callbacks[1]).getPassword());

			HashMap userProps = new HashMap();
			userProps.put("username", n);
			userProps.put("password", p);
			
			if (doAuthenticate(userProps)) return true;
			
			// Authentication failed
			if (isDebug()) System.out.println("Ldap authentication failed ... ");
			setAuthSucceeded(false);
			return false;
		} 
		catch (Exception ex) {
			if (isDebug()) System.out.println("Ldap authentication failed ... " + ex.getMessage());
			setAuthSucceeded(false);
			return false;
		}
	}

	/**
	 * Logs the user out
	 */
	public boolean logout() throws LoginException {
		reset();
		return true;
	}

	/**
	 * Resets user attributes and status flags
	 */
	public void reset() {
		iExternalUid = null;
		super.reset();
	}

	/**
	 * Perform actual authentication the user
	 */
	public boolean doAuthenticate(HashMap userProps) throws Exception {
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.provider")==null) throw new Exception("Ldap provider is not set.");
        String principal = ApplicationProperties.getProperty("tmtbl.authenticate.ldap.principal");
        if (principal==null) throw new Exception("Ldap principal is not set.");
        String query = ApplicationProperties.getProperty("tmtbl.authenticate.ldap.query");
        if (query==null) throw new Exception("Ldap query is not set.");

        String n = (String) userProps.get("username");
		String p = (String) userProps.get("password");
		
		Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ctxFactory","com.sun.jndi.ldap.LdapCtxFactory"));
        env.put(Context.PROVIDER_URL, ApplicationProperties.getProperty("tmtbl.authenticate.ldap.provider"));
        env.put(Context.REFERRAL, ApplicationProperties.getProperty("tmtbl.authenticate.ldap.referral","ignore"));
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.version")!=null)
            env.put("java.naming.ldap.version", ApplicationProperties.getProperty("tmtbl.authenticate.ldap.version"));
        env.put(Context.SECURITY_AUTHENTICATION, ApplicationProperties.getProperty("tmtbl.authenticate.ldap.security","simple"));
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.socketFactory")!=null)
            env.put("java.naming.ldap.factory.socket",ApplicationProperties.getProperty("tmtbl.authenticate.ldap.socketFactory"));
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.keyStore")!=null)
            System.setProperty("javax.net.ssl.keyStore", ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.keyStore"));
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.trustStore")!=null)
            System.setProperty("javax.net.ssl.trustStore", ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.trustStore"));
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.trustStorePassword")!=null)
            System.setProperty("javax.net.ssl.keyStorePassword", ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.keyStorePassword"));
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.trustStorePassword")!=null)
            System.setProperty("javax.net.ssl.trustStorePassword", ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.trustStorePassword"));
        if (ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.trustStoreType")!=null)
            System.setProperty("javax.net.ssl.trustStoreType", ApplicationProperties.getProperty("tmtbl.authenticate.ldap.ssl.trustStoreType"));
        env.put(Context.SECURITY_PRINCIPAL, principal.replaceAll("%", n));
        //if (isDebug()) System.out.println("env:"+ToolBox.dict2string(env, 2));
        env.put(Context.SECURITY_CREDENTIALS, p);
		InitialDirContext cx = new InitialDirContext(env);
		//if (isDebug()) System.out.println("cx:"+cx);
		String idAttributeName = ApplicationProperties.getProperty("tmtbl.authenticate.ldap.externalId","uid");
		Attributes attributes = cx.getAttributes(query.replaceAll("%", n),new String[] {idAttributeName});
		//if (isDebug()) System.out.println("attr:"+attributes);
        Attribute idAttribute = attributes.get(idAttributeName);
        if (idAttribute!=null) {
            if (isDebug()) System.out.println("Ldap authentication passed ... ");
            setAuthSucceeded(true);
            iExternalUid = (String)idAttribute.get();
            setUser(n);
            return true;
        }

        return false;
	}
}
