/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2008 - 2010, UniTime LLC
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

update `timetable`.`solver_info_def` set `implementation`=replace(`implementation`,'edu.purdue.smas','org.unitime');
update `timetable`.`solver_parameter_def` set `default_value`=replace(`default_value`,'edu.purdue.smas','org.unitime') where `default_value` like 'edu.purdue.smas%';
update `timetable`.`change_log` set `obj_type`=replace(`obj_type`,'edu.purdue.smas','org.unitime');

commit;
