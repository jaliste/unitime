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
package org.unitime.timetable.test;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.ClassEvent;
import org.unitime.timetable.model.Solution;
import org.unitime.timetable.model.dao._RootDAO;

import net.sf.cpsolver.ifs.util.ToolBox;

public class MakeEventsForAllCommitedAssignments {

    public static void main(String args[]) {
        try {
            ToolBox.configureLogging();
            HibernateUtil.configureHibernate(new Properties());
            
            Session hibSession = new _RootDAO().getSession();
            List commitedSolutions = hibSession.createQuery("select s from Solution s where s.commited = true").list();
            
            int idx = 0;
            
            for (Iterator i=commitedSolutions.iterator();i.hasNext();) {
                Solution s = (Solution)i.next();
            
                idx++;
                
                System.out.println("Procession solution "+idx+"/"+commitedSolutions.size()+" ("+s.getOwner().getName()+" of "+s.getSession().getLabel()+", committed "+s.getCommitDate()+")");
                
                Transaction tx = null;
                try {
                    tx = hibSession.beginTransaction();

                    Hashtable<Long,ClassEvent> classEvents = new Hashtable();
                    for (Iterator j=hibSession.createQuery(
                            "select e from Solution s inner join s.assignments a, ClassEvent e where e.clazz=a.clazz and s.uniqueId=:solutionId")
                            .setLong("solutionId",s.getUniqueId())
                            .iterate(); j.hasNext();) {
                        ClassEvent e = (ClassEvent)j.next();
                        classEvents.put(e.getClazz().getUniqueId(),e);
                    }
                    for (Iterator j=hibSession.createQuery(
                            "select a from Assignment a "+
                            "where a.solution.uniqueId = :solutionId")
                            .setLong("solutionId", s.getUniqueId())
                            .iterate();
                        j.hasNext();) {
                        Assignment a = (Assignment)j.next();
                        ClassEvent event = a.generateCommittedEvent(classEvents.get(a.getUniqueId()),true);
                        if (event!=null) {
                            System.out.println("  "+a.getClassName()+" "+a.getPlacement().getLongName());
                            hibSession.saveOrUpdate(event);
                        }
                    }
                    
                    tx.commit();
                } catch (Exception e) {
                    if (tx!=null) tx.rollback();
                    throw e;
                }
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
