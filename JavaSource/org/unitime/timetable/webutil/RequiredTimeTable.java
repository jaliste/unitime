/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008 - 2010, UniTime LLC, and individual contributors
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
package org.unitime.timetable.webutil;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.servlet.ServletRequest;

import org.unitime.commons.Debug;
import org.unitime.commons.User;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.Settings;
import org.unitime.timetable.util.Constants;



/** This class provides a time pattern HTML table.
 *
 * @author Tomas Muller
 */
public class RequiredTimeTable {
    public static String getTimeGridSize(User user) {
    	return Settings.getSettingValue(user, Constants.SETTINGS_TIME_GRID_SIZE);
    }

    public static boolean getTimeGridVertical(User user) {
    	return Constants.SETTINGS_TIME_GRID_ORIENTATION_VERTICAL.equalsIgnoreCase(Settings.getSettingValue(user, Constants.SETTINGS_TIME_GRID_ORIENTATION));
    }

    public static boolean getTimeGridAsText(User user) {
    	return Constants.SETTINGS_TIME_GRID_TEXT.equalsIgnoreCase(Settings.getSettingValue(user, Constants.SETTINGS_TIME_GRID_ORIENTATION));
    }

    /** model */
    private RequiredTimeTableModel iModel = null;

    /** table name -- for distinguish two tables in one page */
    private String iName = "rtt";

    /** Constructor from time pattern */
    public RequiredTimeTable(RequiredTimeTableModel model) {
    	iModel = model;
    }

    /** sets table's name */
    public void setName(String name) {
        iName = name;
    }
    
    public String print(boolean timeVertical) throws java.io.IOException {
        return print(true, timeVertical);
    }

    public String exactTime(boolean editable) {
    	StringBuffer sb = new StringBuffer();
    	
    	int days = 0;
    	int hour = -1;
    	int min = -1;
    	int morn = -1;
    	
    	try {
    		days = getModel().getExactDays();
    		int startSlot = getModel().getExactStartSlot();
    		int startMin = startSlot * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
    		if (startMin>=0) {
    			min = startMin % 60;
    			int startHour = startMin / 60;
    			morn = (startHour<12?1:0);
    			hour = startHour % 12;
    			if (hour==0) hour = 12;
    		}
    	} catch (NumberFormatException e) {}
    	
    	if (editable) {
        	for (int i=0;i<Constants.DAY_CODES.length;i++) {
        		sb.append("<input type='checkbox' name='"+iName+"_d"+i+"' "+((days&Constants.DAY_CODES[i])!=0?"checked":"")+" "+"/>"+Constants.DAY_NAME[i]+"&nbsp;&nbsp;\n");
        	}
        	
        	sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n");
        	
        	sb.append("<select name='"+iName+"_hour' "+">\n");
        	sb.append("<option value=''></option>\n");
        	for (int i=1;i<=12;i++)
        		sb.append("<option value='"+i+"' "+(i==hour?"selected":"")+">"+i+"</option>\n");
        	sb.append("</select> : <select name='"+iName+"_min' "+">\n");
        	sb.append("<option value=''></option>\n");
        	for (int i=0;i<60;i+=5)
        		sb.append("<option value='"+i+"' "+(i==min?"selected":"")+">"+(i<10?"0":"")+i+"</option>\n");
        	sb.append("</select> <select name='"+iName+"_morn' "+">\n");
        	sb.append("<option value=''></option>\n");
        	sb.append("<option value='1' "+(morn==1?"selected":"")+">am</option>\n");
        	sb.append("<option value='0' "+(morn==0?"selected":"")+">pm</option>\n");
        	sb.append("</select>");
        } else {
        	int nrDays = 0;
    		for (int i=0;i<Constants.DAY_CODES.length;i++) {
    			if ((days&Constants.DAY_CODES[i])!=0) nrDays++;
    		}
    		for (int i=0;i<Constants.DAY_CODES.length;i++) {
    			if ((days&Constants.DAY_CODES[i])!=0) 
    				sb.append(nrDays==1?Constants.DAY_NAME[i]:Constants.DAY_NAMES_SHORT[i]);
    		}
    		sb.append(" "+hour+":"+(min<10?"0":"")+min+(morn==1?"a":"p"));
    	}

    	return sb.toString();
    }

    public String print(boolean editable, boolean timeVertical) throws java.io.IOException {
    	return print(editable, timeVertical, true, false);
    }
    
    public String getDays() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getNrDays();i++) {
    		if (i>0) sb.append(",");
    		sb.append("'"+getModel().getDayHeader(i)+"'");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getStartTimes() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getNrTimes();i++) {
    		if (i>0) sb.append(",");
    		sb.append("'"+getModel().getStartTime(i)+"'");
    	}
    	sb.append("]");
    	return sb.toString();
    }

    public String getEndTimes() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getNrTimes();i++) {
    		if (i>0) sb.append(",");
    		sb.append("'"+getModel().getEndTime(i)+"'");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getPreferences() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int d=0;d<getModel().getNrDays();d++) {
    		if (d>0) sb.append(",");
    		sb.append("[");
    		for (int t=0; t<getModel().getNrTimes();t++) {
    			if (t>0) sb.append(",");
    			sb.append("'"+getModel().getPreference(d,t)+"'");
    		}
    		sb.append("]");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getTexts() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int d=0;d<getModel().getNrDays();d++) {
    		if (d>0) sb.append(",");
    		sb.append("[");
    		for (int t=0; t<getModel().getNrTimes();t++) {
    			if (t>0) sb.append(",");
    			sb.append("'"+getModel().getFieldText(d,t)+"'");
    		}
    		sb.append("]");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getBorders() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int d=0;d<getModel().getNrDays();d++) {
    		if (d>0) sb.append(",");
    		sb.append("[");
    		for (int t=0; t<getModel().getNrTimes();t++) {
    			if (t>0) sb.append(",");
    			Color borderColor = getModel().getBorder(d,t);
    			if (borderColor==null)
    				sb.append("null");
    			else
    				sb.append("'rgb("+borderColor.getRed()+","+borderColor.getGreen()+","+borderColor.getBlue()+") 2px solid'");
    		}
    		sb.append("]");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getEditables(boolean editable) {
    	StringBuffer sb = new StringBuffer("[");
    	for (int d=0;d<getModel().getNrDays();d++) {
    		if (d>0) sb.append(",");
    		sb.append("[");
    		for (int t=0; t<getModel().getNrTimes();t++) {
    			if (t>0) sb.append(",");
    			sb.append(editable && getModel().isEditable(d,t)?"true":"false");
    		}
    		sb.append("]");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getPreferenceNames() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getPreferenceNames().length;i++) {
    		if (i>0) sb.append(",");
    		sb.append("'"+getModel().getPreferenceNames()[i]+"'");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getPreferenceColors() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getPreferenceNames().length;i++) {
    		if (i>0) sb.append(",");
    		Color color = getModel().getPreferenceColor(getModel().getPreferenceNames()[i]);
    		sb.append("'rgb("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")'");
    	}
    	sb.append("]");
    	return sb.toString();
    }

    public String getPreferenceTexts() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getPreferenceNames().length;i++) {
    		if (i>0) sb.append(",");
    		sb.append("'"+getModel().getPreferenceText(getModel().getPreferenceNames()[i]).replaceAll("'", "&#39;")+"'");
    	}
    	sb.append("]");
    	return sb.toString();
    }
    
    public String getPreferenceEnables() {
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getPreferenceNames().length;i++) {
    		if (i>0) sb.append(",");
    		sb.append(getModel().isPreferenceEnabled(getModel().getPreferenceNames()[i])?"true":"false");
    	}
    	sb.append("]");
    	return sb.toString();
    }

    public String getSelections() {
    	if (getModel().getNrSelections()==0) return "null";
    	StringBuffer sb = new StringBuffer("[");
    	for (int i=0;i<getModel().getNrSelections();i++) {
    		if (i>0) sb.append(",");
    		sb.append("[");
    		int[] limits = getModel().getSelectionLimits(i);
    		sb.append("'"+getModel().getSelectionName(i)+"',[");
    		sb.append(limits[0]+","+limits[1]+","+limits[2]+","+limits[3]);
    		sb.append("]]");
    	}
    	sb.append("]");
    	return sb.toString();
    }

    public String print(boolean editable, boolean timeVertical, boolean showLegend, boolean showTexts) throws java.io.IOException {
        if (getModel().isExactTime()) {
        	return exactTime(editable);
        } else {
        	return
        		"<script language=\"javascript\">\n"+
        		"document.write(tpGenerate(\n\t"+
        		"'"+iName+"',\n\t"+
        		(timeVertical?"false":"true")+",\n\t"+
        		(getModel().getName()==null?"null":"'"+getModel().getName()+"'")+",\n\t"+
        		getModel().getNrTimes()+",\n\t"+
        		getModel().getNrDays()+",\n\t"+
        		getDays()+",\n\t"+
        		getStartTimes()+",\n\t"+
        		getEndTimes()+",\n\t"+
        		(showTexts?getTexts():"null")+",\n\t"+
        		getPreferences()+",\n\t"+
        		getBorders()+",\n\t"+
        		getEditables(editable)+",\n\t"+
        		getPreferenceNames()+",\n\t"+
        		getPreferenceColors()+",\n\t"+
        		getPreferenceTexts()+",\n\t"+
        		getPreferenceEnables()+",\n\t"+
        		getSelections()+",\n\t"+
        		getModel().getDefaultSelection()+",\n\t"+
        		"'"+getModel().getDefaultPreference()+"',\n\t"+
        		(getModel().getPreferenceCheck()==null?"null":"\""+getModel().getPreferenceCheck()+"\"")+", \n\t"+
        		showLegend+"));\n"+
        		"</script>";
        }
    }

    /** update table content from given request */
    public void update(ServletRequest request) {
    	if (getModel().isExactTime()) {
    		int dayCode = 0;
    		for (int i=0;i<Constants.DAY_CODES.length;i++) {
    			if (request.getParameter(iName+"_d"+i)!=null)
    				dayCode += Constants.DAY_CODES[i];
    		}
    		int startSlot = -1;
    		try {
    			if (request.getParameter(iName+"_hour")!=null) {
    				int hour = Integer.parseInt(request.getParameter(iName+"_hour"));
    				if (hour==12) hour=0;
    				int min = Integer.parseInt(request.getParameter(iName+"_min")); 
    				boolean morn = (Integer.parseInt(request.getParameter(iName+"_morn"))==1);
    				int startTime = ((hour+(morn?0:12))%24)*60 + min;
    				startSlot = (startTime - Constants.FIRST_SLOT_TIME_MIN) / Constants.SLOT_LENGTH_MIN;
    			}
    		} catch (Exception e) {}
    		getModel().setExactDays(dayCode);
    		getModel().setExactStartSlot(startSlot);
    		return;
    	} else {
        	for (int d=0;d<getModel().getNrDays();d++) {
        		for (int t=0; t<getModel().getNrTimes();t++) {
        			String prefStr = request.getParameter(iName + "_req_" + d + "_" + t);
        			iModel.setPreference(d, t, (prefStr==null?getModel().getDefaultPreference():prefStr));
        		}
        	}
    	}
    }
    
    public RequiredTimeTableModel getModel() { return iModel; }

    /** put pixel */
    private void putPixel(WritableRaster raster, int x, int y, Color color) {
        //Debug.log("setPixel("+x+","+y+","+color+")");
        raster.setPixel(
            x, y,
            new int[] { color.getRed(), color.getGreen(), color.getBlue() });
    }

    /** draw horizontal line */
    private void drawHline(
        WritableRaster raster, int x, int y, int width, Color color) {
        for (int i = 0; i < width; i++)
            putPixel(raster, x + i, y, color);
    }

    /** draw vertical line */
    private void drawVline(
        WritableRaster raster, int x, int y, int width, Color color) {
        for (int i = 0; i < width; i++)
            putPixel(raster, x, y + i, color);
    }

    /** fill rectangle  */
    private void fillRect(
        WritableRaster raster, int x, int y, int width, int height, Color color) {
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                putPixel(raster, x + i, y + j, color);
    }
    
    public BufferedImage createBufferedImage(boolean timeVertical) {
    	if (getModel().isExactTime()) return null;
    	int[] limit = getModel().getSelectionLimits(getModel().getDefaultSelection());
    	int minTime=limit[0], maxTime=limit[1], minDay=limit[2], maxDay=limit[3];
        int lineWidth = 1;
        int cellWidth = 5;
        int cellsAcross;
        int cellsDown;
        int cellX = 0;
        int cellY = 0;

        if (timeVertical) {
            cellsAcross = maxDay-minDay+1;
            cellsDown = maxTime-minTime+1;
        } else {
            cellsAcross = maxTime-minTime+1;
            cellsDown = maxDay-minDay+1;
        }

        BufferedImage image = new BufferedImage(
                ((cellsAcross * cellWidth) + (cellsAcross) + 1) * lineWidth,
                (cellsDown * cellWidth) + ((cellsDown + 1) * lineWidth),
                BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = image.getRaster();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int cellY2 = 0; cellY2 < (cellsDown + 1); cellY2++) {
            drawHline(raster, 0, cellY2 * (cellWidth + lineWidth), width, Color.darkGray);
            for (int cellX2 = 0; cellX2 < (cellsAcross + 1); cellX2++) {
                drawVline(raster, cellX2 * (cellWidth + lineWidth), 0, height, Color.darkGray);
            }
        }

        for (int day = minDay; day <= maxDay; day++) {
            if (timeVertical)
                cellX = day-minDay;
            else
                cellY = day-minDay;

            for (int time = minTime; time <= maxTime; time++) {
                if (timeVertical)
                    cellY = time-minTime;
                else
                    cellX = time-minTime;

                String pref = iModel.getPreference(day, time);
                if (pref==null) pref = PreferenceLevel.sNeutral;
                Color color = iModel.getPreferenceColor(pref);
                Color borderColor=iModel.getBorder(day,time);
                if (borderColor!=null) {
                	fillRect(raster, cellX * (cellWidth + lineWidth), cellY * (cellWidth + lineWidth), cellWidth + 2, cellWidth + 2, borderColor);
                	fillRect(raster, (cellX * (cellWidth + lineWidth)) + 2, (cellY * (cellWidth + lineWidth)) + 2, cellWidth - 2, cellWidth - 2, color);
                } else {
                	fillRect(raster, (cellX * (cellWidth + lineWidth)) + 1, (cellY * (cellWidth + lineWidth)) + 1, cellWidth, cellWidth, color);
                }
            }
        }
        return image;
    }
    

    /** create a table image -- returns appropriate file (table is not created when the file already exists -- cache) */
    public File createImage(boolean timeVertical) throws java.io.IOException {
    	if (getModel().isExactTime()) return null;

    	char axisId;
        if (timeVertical) {
            axisId = 'V';
        } else {
            axisId = 'H';
        }

        File file = new File(ApplicationProperties.getTempFolder(),getModel().getFileName()+axisId+".png");

        if (file.exists()) {
            return file;
        }
        
        file.getParentFile().mkdirs();
        
        try {
        	if (!file.createNewFile()) {
        		Debug.info("Unable to write a file "+file);
        		file = File.createTempFile("temp",".PNG",ApplicationProperties.getTempFolder());
        	}
        } catch (IOException e) {
    		Debug.info("Unable to write a file "+file);
    		file = File.createTempFile("temp",".PNG",ApplicationProperties.getTempFolder());
        }

 //       Debug.log("Writing image " + file + " ...");


       	javax.imageio.ImageIO.write(createBufferedImage(timeVertical), "PNG", file);
        
        return file;
    }
}
