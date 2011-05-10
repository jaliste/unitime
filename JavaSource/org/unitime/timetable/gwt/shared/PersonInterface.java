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
package org.unitime.timetable.gwt.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author Tomas Muller
 */
public class PersonInterface implements Comparable<PersonInterface>, IsSerializable {
    private String iId, iFName, iMName, iLName, iEmail, iPhone, iDept, iPos, iSource;
    
    public PersonInterface() {
    }
    public PersonInterface(String id, String fname, String mname, String lname, String email, String phone, String dept, String pos, String source) {
        iId = id; iSource = source;
        iFName = fname; iMName = mname; iLName = lname;
        if (iMName!=null && iFName!=null && iMName.indexOf(iFName)>=0) iMName = iMName.replaceAll(iFName+" ?", "");
        if (iMName!=null && iLName!=null && iMName.indexOf(iLName)>=0) iMName = iMName.replaceAll(" ?"+iLName, "");
        iEmail = email; iPhone = phone; iDept = dept; iPos = pos;
    }

    public String getId() { return iId; }
    public void setId(String id) { iId = id; }
    
    public String getSource() { return iSource; }
    public void setSource(String source) { iSource = source; }
    
    public String getFirstName() { return iFName; }
    public void setFirstName(String fname) { iFName = fname; }
    
    public String getMiddleName() { return iMName; }
    public void setMiddleName(String mname) { iMName = mname; }
    
    public String getLastName() { return iLName; }
    public void setLastName(String lname) { iLName = lname; }
    
    public String getName() {
    	return (
    			(iLName == null || iLName.isEmpty() ? "" : iLName) +
    			(iFName == null || iFName.isEmpty() ? "" : ", " + iFName) +
    			(iMName == null || iMName.isEmpty() ? "" : " " + iMName)).trim();
    }
    
    public String getPhone() { return iPhone; }
    public void setPhone(String phone) { iPhone = phone; }
    
    public String getEmail() { return iEmail; }
    public void setEmail(String email) { iEmail = email; }
    
    public String getDepartment() { return iDept; }
    public void setDepartment(String dept) { iDept = dept; }
    
    public String getPosition() { return iPos; }
    public void setPosition(String pos) { iPos = pos; }
    
    public int compareTo(PersonInterface p) {
        int cmp = (getLastName()==null?"":getLastName()).compareToIgnoreCase(p.getLastName()==null?"":p.getLastName());
        if (cmp!=0) return cmp;
        cmp = (getFirstName()==null?"":getFirstName()).compareToIgnoreCase(p.getFirstName()==null?"":p.getFirstName());
        if (cmp!=0) return cmp;
        cmp = (getMiddleName()==null?"":getMiddleName()).compareToIgnoreCase(p.getMiddleName()==null?"":p.getMiddleName());
        if (cmp!=0) return cmp;
        cmp = getSource().compareToIgnoreCase(p.getSource());
        if (cmp!=0) return cmp;
        if (getId() != null) return getId().compareTo(p.getId());
        if (getId() == null && p.getId() == null) return(0);
        return(1);
    }
    
    public void merge(PersonInterface person) {
    	if (iId == null || iId.isEmpty()) iId = person.getId();
    	if (iFName == null || iFName.isEmpty()) iFName = person.getFirstName();
    	if (iMName == null || iMName.isEmpty()) iMName = person.getMiddleName();
    	if (iLName == null || iLName.isEmpty()) iLName = person.getLastName();
    	if (iEmail == null || iEmail.isEmpty()) iEmail = person.getEmail();
    	if (iPhone == null || iPhone.isEmpty()) iPhone = person.getPhone();
    	if (iDept == null || iDept.isEmpty()) iDept = person.getDepartment();
    	if (!iSource.contains(person.getSource()))
    		iSource += ", " + person.getSource();
    }
}
