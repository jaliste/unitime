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
package org.unitime.commons.web;



import java.util.*;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.CSVFile.CSVField;

import org.unitime.commons.NaturalOrderComparator;
import org.unitime.commons.ToolBox;


/** This class provides a web table with some sorting and filtering possibilities.
 *
 * @author Tomas Muller
 */
public class WebTable {

    /** down arrow */
	protected static String IMG_DEC = "images/listdir_top_to_bot.gif";

    /** up arrow */
    protected static String IMG_ASC = "images/listdir_bot_to_top.gif";

    /** number of columns -- for displaing header colspan */
    protected int iColumns;

    /** lines -- table content */
    protected Vector iLines;

    /** table name */
    protected String iName;

    /** column headers */
    protected String[] iHeaders;

    /** allign of columns -- left, right, center */
    protected String[] iAlign;

    /** initial ordering -- true ascendant, false descendant */
    protected boolean[] iAsc;

    /** page reference -- for link order by this column -- %% is replaced by column number */
    protected String iRef;

    /** row style - actually it is cell style */
    protected String iRowStyle = null;
    
    /** suppress row highlighting */
    protected boolean suppressRowHighlight = false;
    
    /** column filter -- hashatable <column key, Boolean> -- true if the column is filtered */
    private Hashtable iColumnFilter = null;

    /** column filter -- column keys */
    private String[]  iColumnFilterKeys = null;
    
    protected boolean iBlankWhenSame = false;
    
    protected WebTableTweakStyle iWebTableTweakStyle = null;
    
    /** creates a WebTable instance */
    public WebTable(int columns, String name, String[] headers, String[] align, boolean[] asc) {
        this(columns, name, null, headers, align, asc);
    }

    /** creates a WebTable instance */
    public WebTable(int columns, String name, String ref, String[] headers, String[] align, boolean[] asc) {
        iName = name;
        iColumns = columns;
        iName = name;
        iHeaders = headers;
        iAlign = align;
        iAsc = asc;
        iRef = ref;
        iLines = new Vector();
    }

    /** sets column filter */
    public void setColumnFilter(Hashtable filter, String[] keys) {
        iColumnFilter = filter;
        iColumnFilterKeys = keys;
    }
    
    public void setWebTableTweakStyle(WebTableTweakStyle style) {
    	iWebTableTweakStyle = style;
    }
    
    public String getStyle(WebTableLine line, WebTableLine next, int order) {
    	String style = (iRowStyle==null?"":iRowStyle+";")+(iWebTableTweakStyle==null?"":iWebTableTweakStyle.getStyleHtml(line, next, order));
    	return (style==null || style.length()==0? "" : "style=\""+style+"\"");
    }
    
    /** sets row (cell) style */
    public void setRowStyle(String style) {
        iRowStyle = (iRowStyle == null ? "" : iRowStyle + ";") + style;
    }
    
    /** sets row highlight suppression */
    public void setSuppressRowHighlight(boolean suppress) {
        suppressRowHighlight = suppress;
    }
    
    /** enable horizontal lines */
    public void enableHR() {
        setRowStyle("border-bottom: rgb(81,81,81) 1px solid");
    }
    
    /** enable horizontal lines */
    public void enableHR(String colorCode) {
        setRowStyle("border-bottom: 1px solid " + colorCode);
    }
    
    /** add line to the table */
    public WebTableLine addLine(String[] line, Comparable[] orderby) {
        WebTableLine wtline = new WebTableLine(null, line, orderby); 
        iLines.addElement(wtline);
        return wtline;
    }
    
    /** add line to the table */
    public WebTableLine addLine(String onClick, String[] line, Comparable[] orderby, String uniqueId) {
        WebTableLine wtline = new WebTableLine(onClick, line, orderby, uniqueId); 
        iLines.addElement(wtline);
        return wtline;
    }

	/** add line to the table */
	public WebTableLine addLine(String onClick, String[] line, Comparable[] orderby) {
		return addLine(onClick, line, orderby,null);
	}
   
    public WebTableLine replaceLine(int index, String onClick, String[] line, Comparable[] orderby){
    	return replaceLine(index, onClick, line, null, null);
    }
    
	public WebTableLine replaceLine(int index, String onClick, String[] line, Comparable[] orderby, String uniqueId){
		if (index >= 0 && index < iLines.size()){
			iLines.remove(index);
            WebTableLine wtline = new WebTableLine(onClick, line, orderby, uniqueId);
			iLines.add(index, wtline);
            return wtline;
		}
        return null;
	}

	/* 
	 * Return the index of the line containing uniqueId
	 * Returns -1 if not found
	 */
	public int indexOfLine (String uniqueId) {
		int indx = 0;
		boolean found = false;
		for (indx = 0; indx < iLines.size() && !found; indx++) {
			WebTableLine wtLine = (WebTableLine) iLines.get(indx);
			if (wtLine.getUniqueId().equals(uniqueId))
				found = true; 
		}
		if (found) {
			return --indx;
		} else {
			return -1;
		}
	}
	
	/* 
	 * Return  line containing uniqueId
	 * Returns null if not found
	 */
	public WebTableLine findLine (String uniqueId) {
		int indx = indexOfLine(uniqueId);
		if (indx >= 0)
			return (WebTableLine) iLines.get(indx);
		else
			return null;
	}
	
	/* 
	 * Return next uniqueId after line containing uniqueId
	 * Returns null if not found
	 */
	public String nextUniqueId (String uniqueId) {
		int indx = indexOfLine(uniqueId);
		if (indx < 0 || indx >= iLines.size() - 1) {
			return null;
		} else {
			return ((WebTableLine) iLines.get(indx + 1)).getUniqueId();
		}
	}
	
	/* 
	 * Return previous uniqueId to line containing uniqueId
	 * Returns null if not found
	 */
	public String previousUniqueId (String uniqueId) {
		int indx = indexOfLine(uniqueId);
		if (indx <= 0 ) {
			return null;
		} else {
			return ((WebTableLine) iLines.get(indx - 1)).getUniqueId();
		}
	}

    /** returns table's HTML code */
    public String printTable() {
        return printTable(-1);
    }
    
    /** is column filtered? */
    protected boolean isFiltered(int col) {
        if (iColumnFilter == null) {
            return false;
        }
        String name = iColumnFilterKeys[col];

        return (iColumnFilter.containsKey(name)
                ? ((Boolean) iColumnFilter.get(name)).booleanValue()
                : false);
    }
    
    public int getNrFilteredColumns() {
        if (iColumnFilter==null) return 0;
        int ret = 0;
        for (Enumeration e=iColumnFilter.keys();e.hasMoreElements();) {
            if (Boolean.TRUE.equals(e.nextElement())) ret++;
        }
        return ret;
    }
    
    /** returns table's HTML code, table is ordered by ordCol-th column */
    public String printTable(int ordCol) {
        String lastLine[] = new String[Math.max(iColumns,(iHeaders==null?0:iHeaders.length))];
        StringBuffer sb = new StringBuffer();

        if (iName != null && iName.trim().length()>0) {
            sb.append("<tr><td colspan=" + iColumns
                    + "><div class=WelcomeRowHead>" + iName + "</div></td></tr>");
        }
        boolean asc = (ordCol == 0 || iAsc == null
                || iAsc.length <= Math.abs(ordCol) - 1
                ? true
                : iAsc[Math.abs(ordCol) - 1]);

        if (ordCol < 0) {
            asc = !asc;
        }
        if (iHeaders != null) {
            sb.append("<tr valign='top'>");
            int last = iColumns - iHeaders.length + 1;

            for (int i = 0; i < iHeaders.length; i++) {
                if (!isFiltered(i)) {
                    if (iHeaders[i] != null) {
                        String header = ToolBox.replace((iRef == null
                                || iLines.size() == 0
                                || ((WebTableLine) iLines.elementAt(0)).getOrderBy()
                                        == null
                                || ((WebTableLine) iLines.elementAt(0)).getOrderBy()[i]
                                        == null
                                ? iHeaders[i]
                                : "<A title=\"Order by this column.\" href=\""
                                        + iRef + "\" class=\"sortHeader\">" + iHeaders[i] + "</A>"
                                        + (i == Math.abs(ordCol) - 1
                                                ? "<div class='WebTableOrderArrow'><img src='"
                                                        + (asc
                                                                ? IMG_ASC
                                                                : IMG_DEC)
                                                        + "' border='0'></div>"
                                                : "")),
                                "%%",
                                String.valueOf(i == Math.abs(ordCol) - 1
                                ? -ordCol
                                : i + 1));

                        sb.append("<td align=\""
                                + (iAlign != null ? iAlign[i] : "left") + "\""
                                + (i == iHeaders.length - 1
                                        ? " colspan=" + last + " "
                                        : "") + " class=\"WebTableHeader\">" + header + "</td>");
                    } else {
                        sb.append("<td class=\"WebTableHeader\" "
                                + (i == iHeaders.length - 1
                                        ? " colspan=" + last + " "
                                        : "")
                                + ">&nbsp;</td>");
                    }
                }
            }
            sb.append("</tr>");
        }
        if (ordCol != 0) {
            Collections.sort(iLines,
                    new WebTableComparator(Math.abs(ordCol) - 1, asc));
        }
        for (int el = 0; el < iLines.size(); el++) {
            WebTableLine wtline = (WebTableLine) iLines.elementAt(el);
            String[] line = wtline.getLine();
            String onClick = wtline.getOnClick();
            String bgColor = wtline.getBgColor();
            String style = getStyle(wtline, (el+1<iLines.size()?(WebTableLine)iLines.elementAt(el+1):null), ordCol);
            int last = iColumns - line.length + 1;
            boolean anchor = (onClick != null && onClick.startsWith("<"));

			sb.append("\n");
            sb.append((anchor ? onClick : "") + "<tr valign='top' "
                    + (onClick == null || anchor ? "" : onClick)
                    + (bgColor == null ? "" : " style='background-color:"+bgColor+"'")
                    + (!suppressRowHighlight ? " onmouseover=\"this.style.backgroundColor='rgb(223,231,242)';" : "") 
                    + "this.style.cursor='"
                    + (onClick == null ? "default" : "hand") 
                    + (onClick != null ? "';this.style.cursor='pointer" : "")
                    + "';\"" 
                    + (!suppressRowHighlight ? "onmouseout=\"this.style.backgroundColor='"+(bgColor==null?"transparent":bgColor)+"';\"" : "") 
                    + ">");
			if (wtline.getUniqueId()!=null) {
				sb.append("<A name=\""+wtline.iUniqueId+"\" />");
			}
            boolean blank = iBlankWhenSame;
            for (int i = 0; i < line.length; i++) {
                if (!isFiltered(i)) {
                    if (blank && line[i]!=null && !line[i].equals(lastLine[i]))
                        blank=false;
                    if (!blank && line[i] != null) {
                        sb.append("<td "
                                + style
                                + " align=\""
                                + (iAlign != null ? iAlign[i] : "left")
                                + "\""
                                + (i == line.length - 1
                                        ? " colspan=" + last + " "
                                        : "")
                                + ">"
                                + line[i]
                                + "</td>");
                    } else {
                        sb.append("<td "
                                + style
                                + " "
                                + (i == line.length - 1
                                        ? " colspan=" + last + " "
                                        : "")
                                + ">&nbsp;</td>");
                    }
                    if (i<lastLine.length) lastLine[i] = line[i]; 
                }
            }
            sb.append("</tr>" + (anchor ? "</a>" : ""));
        }
        return sb.toString();
    }
    
    public CSVFile toCSVFile(int ordCol) {
        CSVFile file = new CSVFile();
        if (iHeaders != null) {
            Vector header = new Vector();
            for (int i=0;i<iHeaders.length;i++)
                if (!isFiltered(i))
                    header.add(new CSVField(iHeaders[i]==null?"":iHeaders[i]));
            file.setHeader(header);
        }
        boolean asc = (ordCol == 0 || iAsc == null || iAsc.length <= Math.abs(ordCol) - 1 ? true : iAsc[Math.abs(ordCol) - 1]);
        if (ordCol < 0) asc = !asc;
        if (ordCol != 0) Collections.sort(iLines, new WebTableComparator(Math.abs(ordCol) - 1, asc));
        String lastLine[] = new String[Math.max(iColumns,(iHeaders==null?0:iHeaders.length))];
        for (Enumeration el = iLines.elements(); el.hasMoreElements();) {
            WebTableLine wtline = (WebTableLine) el.nextElement();
            String[] line = wtline.getLine();
            Vector cline = new Vector();
            boolean blank = iBlankWhenSame;
            for (int i=0; i<line.length; i++) {
                if (isFiltered(i)) continue;
                if (blank && line[i]!=null && !line[i].equals(lastLine[i])) blank=false;

                cline.add(new CSVField(blank || line[i]==null?"":line[i]));
                lastLine[i] = line[i];
            }
            file.addLine(cline);
        }
        return file;
    }
    
    public int getNrColumns() {
        return iColumns - getNrFilteredColumns();
    }
    
    public String[] getHeader() {
        return iHeaders;
    }
    
    public boolean isBlankWhenSame() {
        return iBlankWhenSame;
    }
    
    public void setBlankWhenSame(boolean blankWhenSame) {
        iBlankWhenSame = blankWhenSame;
    }

    /** This class represents a single line in the WebTable */
    public class WebTableLine {

        /** fields */
        private String[] iLine = null;

        /** filed comparators */
        private Comparable[] iOrderBy = null;

        /** onclick event */
        private String iOnClick = null;
        
        /** String uniquely identifying the line */
        private String iUniqueId = null;
        
        private String iBgColor = null;
        
        /** constructor */
        WebTableLine(String onClick, String[] line, Comparable[] orderby) {
            iOnClick = onClick;
            iLine = line;
            iOrderBy = orderby;
        }

		/** constructor */
		 WebTableLine(String onClick, String[] line, Comparable[] orderby, String uniqueId) {
		 	this(onClick, line, orderby);
			iUniqueId = uniqueId;
		 }
        
        public String getOnClick() {
            return iOnClick;
        }

        public String[] getLine() {
            return iLine;
        }
        
        public Comparable[] getOrderBy() {
            return iOrderBy;
        }
        
        /** compare two lines according to the given column and order direction */
        public int compareTo(WebTableLine another, int column, boolean asc) {
            if (column < 0 || iOrderBy == null || iOrderBy.length <= column) {
                return 0;
            }
            if (another.getOrderBy() == null
                    || another.getOrderBy().length <= column) {
                return 0;
            }
            NaturalOrderComparator noc = new NaturalOrderComparator();
            Comparable a = iOrderBy[column];
            Comparable b = another.getOrderBy()[column];
            int ret = (a == null
                    ? (b == null ? 0 : -1)
                    : b == null
                            ? 1
                            : a instanceof String && b instanceof String
                                    ? (asc ? 1 : -1)
                                            * noc.compare((String) a, (String) b)
                                    : (asc ? 1 : -1) * a.compareTo(b));

            if (ret != 0) {
                return ret;
            }
            for (int i = 0; i < iOrderBy.length; i++) {
                a = iOrderBy[i];
                b = another.getOrderBy()[i];
                ret = (a == null
                        ? (b == null ? 0 : -1)
                        : b == null
                                ? 1
                                : a instanceof String && b instanceof String
                                        ? (asc ? 1 : -1)
                                                * noc.compare((String) a, (String) b)
                                        : (asc ? 1 : -1) * a.compareTo(b));
                if (ret != 0) {
                    return ret;
                }
            }
            return ret;
        }
        
		/**
		 * @return
		 */
		public String getUniqueId() {
			return iUniqueId;
		}

		/**
		 * @param String
		 */
		public void setUniqueId(String uniqueId) {
			iUniqueId = uniqueId;
		}
        
        public void setBgColor(String bgColor) {
            iBgColor = bgColor;
        }
        
        public String getBgColor() {
            return iBgColor;
        }

    }
    

    /** Table lines comparator */
    public static class WebTableComparator implements Comparator {
        private int iColumn = 1;
        private boolean iAsc = true;
        
        /** constructor -- order column and order direction */
        public WebTableComparator(int column, boolean asc) {
            iColumn = column;
            iAsc = asc;
        }
        
        /** compares two lines */
        public int compare(Object o1, Object o2) {
            if (o1 == null || o2 == null || !(o1 instanceof WebTableLine)
                    || !(o2 instanceof WebTableLine)) {
                return 0;
            }
            WebTableLine w1 = (WebTableLine) o1;
            WebTableLine w2 = (WebTableLine) o2;

            return w1.compareTo(w2, iColumn, iAsc);
        }
        
        /** compares two lines */
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof WebTableComparator)) {
                return false;
            }
            WebTableComparator wtc = (WebTableComparator) obj;

            return (wtc.iAsc == iAsc && wtc.iColumn == iColumn);
        }
    }
    
    /** get order (index of ordered column) for given session and table 
     * @param session session
     * @param code table code
     * @return index of ordered column (negative for desc.) */
    public static int getOrder(javax.servlet.http.HttpSession session, String code) {
        Hashtable orderInfo = (Hashtable)session.getAttribute("OrderInfo");
        if (orderInfo==null) {
            orderInfo = new Hashtable();
            session.setAttribute("OrderInfo",orderInfo);
        }
        return (orderInfo.containsKey(code)?((Integer)orderInfo.get(code)).intValue():0);
    }

    /** set order (index of ordered column) for given session and table 
     * @param session session
     * @param code table code
     * @param order new order (index of ordered column, negative if desc.)
     * @param defOrder default order (if order is null) 
     */
    public static void setOrder(javax.servlet.http.HttpSession session, String code, String order, int defOrder) {
        Hashtable orderInfo = (Hashtable)session.getAttribute("OrderInfo");
        if (orderInfo==null) {
            orderInfo = new Hashtable();
            session.setAttribute("OrderInfo",orderInfo);
        } 
        if (order!=null) orderInfo.put(code,Integer.valueOf(order));
        else if (!orderInfo.containsKey(code)) orderInfo.put(code,new Integer(defOrder));
    }

    /** set order (index of ordered column) for given session and table 
     * @param session session
     * @param code table code
     * @param order new order (index of ordered column, negative if desc.)
     */
    public static void setOrder(javax.servlet.http.HttpSession session, String code, String order) {
        if (order==null) return;
        Hashtable orderInfo = (Hashtable)session.getAttribute("OrderInfo");
        if (orderInfo==null) {
            orderInfo = new Hashtable();
            session.setAttribute("OrderInfo",orderInfo);
        }
        orderInfo.put(code,Integer.valueOf(order));
    }
    
    
    public Vector getLines() {
        return iLines;
    }
    
    public void setRef(String ref) { iRef = ref; }
    public void setName(String name) { iName = name; }
    
    public static interface WebTableTweakStyle {
    	public String getStyleHtml(WebTableLine currentLine, WebTableLine nextLine, int orderBy);
    }
}
