/*
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime LLC
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

alter table room add exam_type number(10,0) default 0;
alter table non_university_location add exam_type number(10,0) default 0;

update room set exam_type = 1 where exam_enable = 1;
update non_university_location set exam_type = 1 where exam_enable = 1;

alter table room drop column exam_enable;
alter table non_university_location drop column exam_enable;

alter table room drop column exam_pref;
alter table non_university_location drop column exam_pref;

create table exam_location_pref (
	uniqueid number(20,0) constraint nn_exam_location_pref_uniqueid not null,
	location_id number(20,0) constraint nn_exam_location_pref_owner not null,
	pref_level_id number(20,0) constraint nn_exam_location_pref_pref not null,
	period_id number(20,0) constraint nn_exam_location_pref_period not null
);

alter table exam_location_pref
  add constraint pk_exam_location_pref primary key (uniqueid);
  
alter table exam_location_pref
  add constraint fk_exam_location_pref_pref foreign key (pref_level_id)
  references preference_level (uniqueid) on delete cascade;
  
  alter table exam_location_pref
  add constraint fk_exam_location_pref_period foreign key (period_id)
  references exam_period (uniqueid) on delete cascade;
  
create index idx_exam_location_pref on exam_location_pref(location_id);
  
/*
 * Update database version
 */

update application_config set value='20' where name='tmtbl.db.version';

commit;
  
