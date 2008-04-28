/*
 * UniTime 3.0 (University Course Timetabling & Student Sectioning Application)
 * Copyright (C) 2007, UniTime.org
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

var CAL_MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
var CAL_WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

function calGenVariables(name, year, startMonth, endMonth, cal, prefTable, prefColors, defPreference, showLegend) {
	if (showLegend) document.writeln("<INPUT id='cal_select' type='hidden' value='"+defPreference+"' name='"+name+"_select'>");
	for (var m=startMonth;m<=endMonth;m++) {
		var d=new Date(year,m,1);
		do {
			document.writeln("<INPUT id='"+name+"_val_"+d.getMonth()+"_"+d.getDate()+"' name='"+name+"_val_"+d.getMonth()+"_"+d.getDate()+"' type='hidden' value='"+(cal==null?0:cal[m-startMonth][d.getDate()-1])+"'>");
			d.setDate(d.getDate()+1);
		} while (d.getMonth()==((12+m)%12));
	}
	document.writeln("<script language='javascript'>");
	document.writeln("function calPref2Color(pref) {");
	for (var i=0;i<prefTable.length;i++)
		document.writeln("if (pref=='"+prefTable[i]+"') return '"+prefColors[i]+"';");
		document.writeln("return 'rgb(240,240,240)';");
	document.writeln("}");
	document.writeln("<"+"/script>");
}

function calGetCurrentPreference(name) {
	return document.getElementById("cal_select").value;
}
function calPrefSelected(name, pref) {
	document.getElementById(name+'_pref'+document.getElementById("cal_select").value).style.border='rgb(0,0,0) 2px solid';
	document.getElementById('cal_select').value=pref;
	document.getElementById(name+'_pref'+pref).style.border='rgb(0,0,240) 2px solid';
}
function calGetPreference(name, date) {
	return document.getElementById(name+"_val_"+date.getMonth()+"_"+date.getDate()).value;
}
function calSetPreference(name, date, pref) {
	document.getElementById(name+"_val_"+date.getMonth()+"_"+date.getDate()).value=pref;
	document.getElementById(name+"_"+date.getMonth()+"_"+date.getDate()).style.backgroundColor=calPref2Color(pref);
}

function calGetWeekNumber(date) {
	var w = 1;
	var d=new Date(date.getFullYear(),0,1);
	while (d.getDay()!=6)
		d.setDate(d.getDate()+1);
	while (date>d) {
		w++; d.setDate(d.getDate()+7);
	}
	return w;
}

function calGenDayHeader(name, year, month, day, editable) {
	if (editable) {
		var onclick = "";
		var d = new Date(year,month,1);
		while (d.getDay()!=day) {
			d.setDate(d.getDate()+1);
		}
		do {
			if (calGetPreference(name, d)!='@')
				onclick += "calSetPreference('"+name+"', new Date("+d.getFullYear()+","+d.getMonth()+","+d.getDate()+"),calGetCurrentPreference('"+name+"'));";
			d.setDate(d.getDate()+7);
		} while (d.getMonth()==month);
		if (onclick.length>0) {
			document.writeln("<th width='20' height='20' "+
 	  		"style=\"border:rgb(100,100,100) 1px solid;background-color:"+(day==0||day==6?"rgb(200,200,200);":"rgb(220,220,220);")+"\" "+
 				"onmouseover=\"this.style.border='rgb(0,0,242) 1px solid';this.style.cursor='pointer';\" "+
 				"onmouseout=\"this.style.border='rgb(100,100,100) 1px solid';\" "+
 				"onclick=\""+onclick+"\">"+
 				"<font size=1>"+CAL_WEEKDAYS[day]+"</font>"+
 				"</th>");
 		} else {
			document.writeln("<th width='20' height='20' "+
 	  		"style=\"border:rgb(100,100,100) 1px solid;background-color:"+(day==0||day==6?"rgb(200,200,200);":"rgb(220,220,220);")+"\" "+
 				"<font size=1>"+CAL_WEEKDAYS[day]+"</font>"+
	 			"</th>");
 		}
 	} else {
		document.writeln("<th width='20' height='20' "+
 	  	"style=\"border:rgb(100,100,100) 1px solid;background-color:"+(day==0||day==6?"rgb(200,200,200);":"rgb(220,220,220);")+"\" "+
 			"<font size=1>"+CAL_WEEKDAYS[day]+"</font>"+
 			"</th>");
 	}
}

function calGenDayHeaderBlank(name, year, month, editable) {
	if (editable) {
		var onclick = "";
		var d = new Date(year,month,1);
		do {
			if (calGetPreference(name, d)!='@')
				onclick += "calSetPreference('"+name+"', new Date("+d.getFullYear()+","+d.getMonth()+","+d.getDate()+"),calGetCurrentPreference('"+name+"'));";
			d.setDate(d.getDate()+1);
		} while (d.getMonth()==month);
		if (onclick.length>0) {
			document.writeln("<th width='20' height='20' "+
 	  		"style=\"border:rgb(100,100,100) 1px solid;background-color:rgb(220,220,220);\" "+
	 			"onmouseover=\"this.style.border='rgb(0,0,242) 1px solid';this.style.cursor='pointer';\" "+
 				"onmouseout=\"this.style.border='rgb(100,100,100) 1px solid';\" "+
 				"onclick=\""+onclick+"\">"+
 				"<font size=1>&nbsp;</font>"+
 				"</th>");
 		} else {
			document.writeln("<th width='20' height='20' "+
	 	  	"style=\"border:rgb(100,100,100) 1px solid;background-color:rgb(220,220,220);\" "+
 				"<font size=1>&nbsp;</font>"+
	 			"</th>");
 		}
 	} else {
		document.writeln("<th width='20' height='20' "+
 	  	"style=\"border:rgb(100,100,100) 1px solid;background-color:rgb(220,220,220);\" "+
 			"<font size=1>&nbsp;</font>"+
 			"</th>");
 	}
}

function calGenWeekHeader(name, date, editable) {
	if (editable) {
		var onclick = "";
		var d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
		do {
			if (calGetPreference(name, d)!='@')
				onclick += "calSetPreference('"+name+"', new Date("+d.getFullYear()+","+d.getMonth()+","+d.getDate()+"),calGetCurrentPreference('"+name+"'));";
			d.setDate(d.getDate()+1);
		} while (d.getDay()!=0 && d.getMonth()==date.getMonth());
		if (onclick.length>0) {
			document.writeln("<th width='20' height='20' "+
 	  		"style=\"border:rgb(100,100,100) 1px solid;background-color:rgb(220,220,220);\" "+
 				"onmouseover=\"this.style.border='rgb(0,0,242) 1px solid';this.style.cursor='pointer';\" "+
 				"onmouseout=\"this.style.border='rgb(100,100,100) 1px solid';\" "+
 				"onclick=\""+onclick+"\">"+
 				"<font size=1>"+calGetWeekNumber(date)+"</font>"+
 				"</th>");
 		} else {
			document.writeln("<th width='20' height='20' "+
 		  	"style=\"border:rgb(100,100,100) 1px solid;background-color:rgb(220,220,220);\" "+
 				"<font size=1>"+calGetWeekNumber(date)+"</font>"+
	 			"</th>");
 		}
 	} else {
		document.writeln("<th width='20' height='20' "+
 	  	"style=\"border:rgb(100,100,100) 1px solid;background-color:rgb(220,220,220);\" "+
 			"<font size=1>"+calGetWeekNumber(date)+"</font>"+
 			"</th>");
 	}
}

function calGenHeader(name, year, month, editable) {
	document.writeln("<tr>");
	calGenDayHeaderBlank(name, year, month, editable);
	for (var i=0;i<7;i++)
		calGenDayHeader(name, year, month, i, editable);
	document.writeln("</tr>");
}

function calGenField(name, monthIdx, date, highlight, editable) {
	var border = (highlight==null || highlight[monthIdx][date.getDate()-1]==null?'rgb(100,100,100) 1px solid':highlight[monthIdx][date.getDate()-1]);
	var borderSelect = (highlight==null || highlight[monthIdx][date.getDate()-1]==null?'rgb(0,0,242) 1px solid':'rgb(0,0,242) 2px solid');
	if (editable && calGetPreference(name, date)!='@')
		document.writeln("<td align='center' width='20' height='20' id='"+name+"_"+date.getMonth()+"_"+date.getDate()+"' "+
 		  	"style=\"border:"+border+";background-color:"+calPref2Color(calGetPreference(name, date))+";\" "+
 			"onmouseover=\"this.style.border='"+borderSelect+"';this.style.cursor='pointer';\" "+
 			"onmouseout=\"this.style.border='"+border+"';\" "+
 			"onclick=\"calSetPreference('"+name+"', new Date("+date.getFullYear()+","+date.getMonth()+","+date.getDate()+"),calGetCurrentPreference('"+name+"'));\">"+
 			"<font size=1>"+date.getDate()+"</font>"+
 			"</td>");
 	else
		document.writeln("<td align='center' width='20' height='20' id='"+name+"_"+date.getMonth()+"_"+date.getDate()+"' "+
 		  "style=\"border:"+border+";background-color:"+calPref2Color(calGetPreference(name, date))+";\" "+
 			"<font size=1>"+date.getDate()+"</font>"+
 			"</td>");
}
function calGenFieldBlank() {
	document.writeln("<td width='20' height='20' style=\"border:rgb(100,100,100) 1px solid;\" >&nbsp;</td>");
}

function calGenFieldBlankTL() {
	document.writeln("<td width='20' height='20' style=\"border-top:rgb(100,100,100) 1px solid;border-left:rgb(100,100,100) 1px solid;\" >&nbsp;</td>");
}

function calGenFieldBlankT() {
	document.writeln("<td width='20' height='20' style=\"border-top:rgb(100,100,100) 1px solid;\" >&nbsp;</td>");
}

function calGenFieldBlankTB() {
	document.writeln("<td width='20' height='20' style=\"border-top:rgb(100,100,100) 1px solid;border-bottom:rgb(100,100,100) 1px solid;\" >&nbsp;</td>");
}

function calGenFieldBlankTBL() {
	document.writeln("<td width='20' height='20' style=\"border-top:rgb(100,100,100) 1px solid;border-bottom:rgb(100,100,100) 1px solid;border-left:rgb(100,100,100) 1px solid;\" >&nbsp;</td>");
}

function calGenFieldBlankTBR() {
	document.writeln("<td width='20' height='20' style=\"border-top:rgb(100,100,100) 1px solid;border-bottom:rgb(100,100,100) 1px solid;border-right:rgb(100,100,100) 1px solid;\" >&nbsp;</td>");
}

function calGetMonth(name, year, month, monthIdx, highlight, editable, nameSuffix) {
	document.writeln("<table style='font-size:10px;' cellSpacing='0' cellPadding='1' border='0'>");
	var xmonth=month; var xyear = year;
	if (xmonth<0) { xmonth+=12; xyear--; }
	if (xmonth>=12) { xmonth-=12; xyear++; }
	document.writeln("<tr><th colspan='8' align='center'>"+CAL_MONTHS[xmonth]+" "+xyear+" "+nameSuffix+"</th></tr>");
	calGenHeader(name,xyear,xmonth,editable);
	document.writeln("<tr>");
	var x = new Date(xyear,xmonth,1);
	calGenWeekHeader(name, x, editable);
	for (var i=0;i<x.getDay();i++) {
		if (i==0 && i+1==x.getDay())
		calGenFieldBlank();
		else if (i==0)
			calGenFieldBlankTBL();
		else if (i+1==x.getDay())
			calGenFieldBlankTBR();
		else
			calGenFieldBlankTB();
	}
	var f = true;
	do {
		if (x.getDay()==0 && !f) {
			document.writeln("</tr><tr>");
			calGenWeekHeader(name, x, editable);
		}
		f = false;
		calGenField(name, monthIdx, x, highlight, editable);
		x.setDate(x.getDate()+1);
	} while (x.getMonth()==xmonth);
	if (x.getDay()!=0) {
		for (var i=x.getDay();i<7;i++) {
			if (i==x.getDay())
				calGenFieldBlankTL();
			else
				calGenFieldBlankT();
		}
	}
	document.writeln("</tr>");
	document.writeln("</table>");
}

function calGenPreference(name, pref, title, editable) {
	if (editable && pref!='@')
		document.writeln("<tr align=left "+
			"onmouseover=\"this.style.backgroundColor='rgb(223,231,242)';this.style.cursor='pointer';\" "+
			"onclick=\"calPrefSelected('"+name+"', '"+pref+"');\" "+
			"onmouseout=\"this.style.backgroundColor='rgb(255,255,255)';\">"+
			"<td id='"+name+"_pref"+pref+"' width='30' height='20' "+
			"style=\"border:"+(calGetCurrentPreference(name)==pref?"rgb(0,0,240)":"rgb(0,0,0)")+" 2px solid;background-color:"+calPref2Color(pref)+";\">"+
			"&nbsp;</td>"+
			"<th nowrap>"+title+"</th></tr>");
	else
		document.writeln("<tr align=left>"+
			"<td id='"+name+"_pref"+pref+"' width='30' height='20' "+
			"style=\"border:rgb(0,0,0) 2px solid;background-color:"+calPref2Color(pref)+";\">"+
			"&nbsp;</td>"+
			"<th nowrap>"+title+"</th></tr>");
}

function calGenLegend(name, prefTable, prefNames, editable) {
	document.writeln("<table style='font-size:12px' cellSpacing='2' cellPadding='2' border='0'>");
	for (var i=0;i<prefTable.length;i++)
		calGenPreference(name, prefTable[i],prefNames[i],editable);
	document.writeln("</table>");
}

function calGenerate(year, startMonth, endMonth, cal, prefTable, prefNames, prefColors, defPref, highlight, editable, showLegend, name, nameSuffix, cols, ts, te) {
 	cols = (typeof(cols) != 'undefined' ? cols : 4);
 	name = (typeof(name) != 'undefined' ? name : 'cal');
 	nameSuffix = (typeof(nameSuffix) != 'undefined' ? nameSuffix : '');
 	ts = (typeof(ts) != 'undefined' ? ts : true);
 	te = (typeof(te) != 'undefined' ? te : true); 
	calGenVariables(name, year, startMonth, endMonth, cal, prefTable, prefColors, defPref, showLegend);
	if (ts) document.writeln("<table cellSpacing='10' cellPadding='2' border='0'>");
	for (var m=startMonth;m<=endMonth;m++) {
		if ((m-startMonth)%cols==0) document.writeln("<tr>");
		document.writeln("<td valign='top'>");
			calGetMonth(name, year,m,m-startMonth,highlight, editable, nameSuffix);
		document.writeln("</td>");
		if ((m-startMonth)%cols==cols-1) document.writeln("</tr>");
	}
	if (showLegend) {
		if ((m-startMonth)%cols!=0) {
			document.writeln("<td valign='center' align='center'"+((m-startMonth)%cols==1?" colspan='2'":"")+">");
				calGenLegend(name, prefTable, prefNames, editable);
			document.writeln("</td></tr>");
		} else {
			document.writeln("<tr><td colspan='"+cols+"' valign='top' align='center'>");
				calGenLegend(name, prefTable, prefNames, editable);
			document.writeln("</td></tr>");
		}
	} else {
		if ((m-startMonth)%cols!=0)
			document.writeln("</tr>");
	}
	if (te) document.writeln("</table>");
}