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
package org.unitime.timetable.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.unitime.commons.Debug;


/**
 *  Servlet Filter to allow only certain characters in the request
 *  object to prevent Cross-Site Scripting and SQL Injection attacks.  
 * 
 *  The filter accepts the following optional paramters:
 *  
 *   replace_string   : This is character / string that replaces all characters 
 * 				  		not in the allowed list. replaceStr defaults to underscore (_)
 *   additional_chars : These are additional characters that should be allowed in addition
 * 				  		to the default characters. RegEx patterns may also be supplied.
 * 
 * @author Heston Fernandes
 */

public class XSSFilter implements Filter {

    // ------------------------------------------------------ Properties
    
    /** Filter Configuration object **/
    private FilterConfig filterConfig;
    
    /** The character that replaces the filtered character **/
    private String replaceStr ="_";
    
    /** Additional characters that should be allowed **/
    private String addlChars = "";
    
    /** Pattern of allowed characters **/
    //private String charsAllowed = "A-Za-z0-9@.' _+&%=/\\-";
    private String charsAllowed = "A-Za-z0-9@. _+&%/\\-";

    
    // ------------------------------------------------------ Methods
    
    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;        
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter( ServletRequest request, 
            			  ServletResponse response,
            			  FilterChain chain ) throws IOException, ServletException {
        
        // Get Filter Init Parameters
        String replaceStrParam = filterConfig.getInitParameter("replace_string");
        String addlCharsParam = filterConfig.getInitParameter("additional_chars");
        
        // Set variables
        if(replaceStrParam!=null)
            replaceStr = replaceStrParam;
        if(addlCharsParam!=null)
            addlChars = addlCharsParam;
        
        // Construct allowed characters pattern
        String charPattern = "([^" + charsAllowed + addlChars + "]+)(%0A)";        
        
        // Instantiate actual filter
        RequestXSSFilter rxs = new RequestXSSFilter( 
                (HttpServletRequest) request, replaceStr, charPattern);

		// Process request
		chain.doFilter(rxs,response);

    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        this.filterConfig = null;
    }
 

    /**
     * Testing function
     * @param args
     */
    public static void main(String[] args) {
        
        String arg = "jhfsdhfds/=+;-%0Adsa";
        if(args[0]!=null && args[0].trim().length()>0)
            arg = args[0];
        
        String result = arg.trim().replaceAll("([^A-Za-z0-9@.' _+&%/=\\-]+)(%0A)", "_");
        
        if(!result.equals(arg)) {
	        Debug.debug("Before Filtering: " + result);
	        Debug.debug("After Filtering: " + result);
        }
    }
    
}

/**
 * Class that wraps HttpServletRequest and filters the request parameters
 * This logic is contrary to conventional logic where only certain tags and
 * words are replaced. As there are new hacks everyday, the safest way is to 
 * allow only certain characters: A-Z a-z 0-9 @ . ' space _ - (by default)
 * @note This logic is yet to be tested so do not use it yet. Heston 
 */
final class RequestXSSFilter extends HttpServletRequestWrapper {
    
    /** The character that replaces the filtered character **/
    private String replaceStr ="_";
    
    /** Pattern of allowed characters **/
    private String charsAllowed = "";
    
	/**
     * Constructor with filter parameters
     * @param servletRequest HttpServletRequest object
     * @param replaceStr Character/String that replaces all characters not allowed
     * @param charsAllowed Pattern of allowed characters
     */
    public RequestXSSFilter(HttpServletRequest servletRequest, 
            				String replaceStr, 
            				String charsAllowed) {
        super(servletRequest);
        this.charsAllowed = charsAllowed;
        this.replaceStr = replaceStr;
    }

    /**
     * @param parameter Parameter name
     * @return array of filtered request parameters
     */
    public String[] getParameterValues(String parameter) {
        String[] results = super.getParameterValues(parameter);
        
        if (results == null)
            return results;
        
        int count = results.length;
        String[] params = new String[count];
        
        for (int i = 0; i < count; i++) {
            if(results[i]!=null) {
                // Filter
                params[i] = results[i].trim().replaceAll(charsAllowed, replaceStr);
                
                // Characters were filtered out
                if(!results[i].equals(params[i])) {
                    Debug.debug("parameterValues: " + parameter);
	                Debug.debug("   - before Filtering: " + results[i]);
	                Debug.debug("   - after Filtering: " + params[i]);
                }
            }
            else
                params[i] = null;
        }
        
        return params;
    }
    
    
    /**
     * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
     */
    public String getParameter(String parameter) {
        String result = super.getParameter(parameter);
        if (result == null) 
            return result;
    
        String bf = result;
        
        // Filter
        result = result.trim().replaceAll(charsAllowed, replaceStr);
        
        // Characters were filtered out
        if(!bf.equals(result)) {
            Debug.debug("parameter: " + parameter);
            Debug.debug("   - before Filtering: " + bf);
            Debug.debug("   - after Filtering: " + result);
        }

        return result;
    }
}
