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
package org.unitime.timetable.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class PdfEventHandler extends PdfPageEventHelper {

	private BaseFont baseFont;
	private float fontSize;
	
	private Date dateTime = null;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy hh:mmaa");
	
    /**
     * Constructor for PdfEventHandler
     * 
     */
    public PdfEventHandler() throws DocumentException, IOException {

    	super();
	    
    	Font font = PdfFont.getFont();
		setBaseFont(font.getBaseFont());
		setFontSize(font.getSize());
		
        return;
     }

    /**
     * Initialize Pdf footer
     * @param document
     * @param outputStream
     * @return PdfWriter
     */
    public static PdfWriter initFooter(Document document, FileOutputStream outputStream) 
    		throws DocumentException, IOException {
    	
		PdfWriter iWriter = PdfWriter.getInstance(document, outputStream);
		iWriter.setPageEvent(new PdfEventHandler());
    	
		return iWriter;
    }
    /**
     * Print footer string on each page
     * @param writer
     * @param document
     */
    public void onEndPage(PdfWriter writer, Document document) {
	    
    	if(getDateTime() == null) {
    		setDateTime(new Date());
    	}
    	
		PdfContentByte cb = writer.getDirectContent();
		cb.beginText();
		cb.setFontAndSize(getBaseFont(), getFontSize());
		cb.showTextAligned(PdfContentByte.ALIGN_LEFT, getDateFormat().format(getDateTime()), 
			    document.left(), 20, 0);
		cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, String.valueOf(document.getPageNumber()), 
			    document.right(), 20, 0);
		cb.endText();
			
        return;
    }

	private BaseFont getBaseFont() {
		return baseFont;
	}

	private void setBaseFont(BaseFont baseFont) {
		this.baseFont = baseFont;
	}

	private float getFontSize() {
		return fontSize;
	}

	private void setFontSize(float fontSize) {
		this.fontSize = fontSize;
	}

	private Date getDateTime() {
		return dateTime;
	}

	private void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}

	private SimpleDateFormat getDateFormat() {
		return dateFormat;
	}

}
