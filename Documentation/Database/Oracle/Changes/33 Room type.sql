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


/**
 * Add is_room attribute to room_type table
 **/
 
alter table room_type add is_room number(1) default 1 constraint nn_room_type_room not null;

/**
 * Add non-university location room type
 **/

insert into room_type(uniqueid, reference, label, ord, is_room) values (ref_table_seq.nextval, 'nonUniversity', 'Non-University Locations', 4, 0);

/**
 * Create room_type attribute of non_university_location table
 **/
 
alter table non_university_location add room_type number(20,0);
 
update non_university_location r set r.room_type = (select t.uniqueid from room_type t where t.reference='nonUniversity');
 
alter table non_university_location add constraint nn_location_type check  (room_type is not null);
 
alter table non_university_location add constraint fk_location_type foreign key (room_type)
  references room_type (uniqueid) on delete cascade;
  
/**
 * Update database version
 */

update application_config set value='33' where name='tmtbl.db.version';

commit;
