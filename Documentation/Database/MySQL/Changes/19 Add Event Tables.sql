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

-- create event_type table
create table EVENT_TYPE
(
  UNIQUEID  DECIMAL(20,0) primary key not null,
  REFERENCE VARCHAR(20) not null,
  LABEL     VARCHAR(60) not null
);

select `next_hi` into @id from `timetable`.`hibernate_unique_key`;

insert into event_type values(@id, 'class', 'Class');
insert into event_type values(@id+1, 'final', 'Final Exam');
insert into event_type values(@id+2, 'evening', 'Evening Exam');
insert into event_type values(@id+3, 'otherWithConflict', 'Other Course Event with Conflict Checking');
insert into event_type values(@id+4, 'otherNoConflict', 'Other Course Event with No Conflict Checking');
insert into event_type values(@id+5, 'special', 'Special Event');

update `timetable`.`hibernate_unique_key` set `next_hi`=`next_hi`+6; 

 
 -- Create table event_contact
create table event_contact
(
  uniqueid    decimal(20,0) primary key not null,
  external_id varchar(40),
  email       varchar(100)  not null,
  phone       varchar(10)  not null,
  firstName   varchar(20),
  middleName  varchar(20),
  lastName    varchar(30)
);

-- Create event table
create table event
(
  uniqueid       decimal(20,0) not null primary key,
  event_type           decimal(20,0) not null,
  event_name           varchar(100),
  min_capacity   bigint(10),
  max_capacity   bigint(10),
  sponsoring_org decimal(20,0),
  main_contact_id          decimal(20,0)
);

-- Create/Recreate primary, unique and foreign key constraints 
alter table event
  add constraint fk_event_event_type foreign key (EVENT_TYPE)
  references event_type (UNIQUEID) on delete cascade;
alter table event
  add constraint fk_event_main_contact foreign key (MAIN_CONTACT_ID)
  references event_contact (UNIQUEID) on delete  set null;

-- Create table
create table event_join_event_contact
(
  event_id         decimal(20,0) not null,
  event_contact_id decimal(20,0) not null
);

-- Create/Recreate primary, unique and foreign key constraints 
alter table event_join_event_contact
  add constraint fk_event_id_join foreign key (EVENT_ID)
  references event (UNIQUEID) on delete cascade;
alter table event_join_event_contact
  add constraint fk_event_contact_join foreign key (EVENT_CONTACT_ID)
  references event_contact (UNIQUEID) on delete cascade;
  

create table related_course_info (
  uniqueid decimal(20,0) primary key not null,
  event_id decimal(20,0) not null,
  owner_id decimal(20,0) not null,
  owner_type decimal(10,0) not null,
  course_id decimal (20,0) not null
);

alter table related_course_info add constraint fk_event_owner_event foreign key (event_id) references event (uniqueid) on delete cascade;
alter table related_course_info add constraint fk_event_owner_course foreign key (course_id) references course_offering (uniqueid) on delete cascade;

create index idx_event_owner_event on related_course_info(event_id);
create index idx_event_owner_owner on related_course_info(owner_id, owner_type);

-- Create table
create table meeting
(
  uniqueid           decimal(20,0) primary key not null,
  event_id           decimal(20,0)  not null,
  event_type         decimal(20,0) not null,
  meeting_date       date not null,
  start_period       bigint(10) not null,
  start_offset       bigint(10),
  stop_period        bigint(10) not null,
  stop_offset        bigint(10),
  location_perm_id   decimal(20,0),
  class_can_override int(1) not null,
  approved_date      date
)
;
-- Create/Recreate primary, unique and foreign key constraints 
alter table meeting
  add constraint fk_meeting_event foreign key (EVENT_ID)
  references event (UNIQUEID) on delete cascade;
alter table meeting
  add constraint fk_meeting_event_type foreign key (EVENT_TYPE)
  references event_type (UNIQUEID) on delete cascade;
  
 -- create event_note table
create table STANDARD_EVENT_NOTE
(
  UNIQUEID  DECIMAL(20,0) primary key not null,
  REFERENCE VARCHAR(20) not null,
  NOTE     VARCHAR(1000) not null
);
 
-- Create table event_note
create table event_note
(
  uniqueid decimal(20,0) primary key not null,
  event_id  decimal(20,0) not null,
  note_id   decimal(20,0),
  text_note varchar(1000)
);

-- Create/Recreate primary, unique and foreign key constraints 
alter table event_note
  add constraint fk_event_note_event foreign key (EVENT_ID)
  references event (UNIQUEID) on delete cascade;
alter table event_note
  add constraint fk_event_note_std_note foreign key (NOTE_ID)
  references standard_event_note (UNIQUEID) on delete set null;
  
/*
 * Update database version
 */

update application_config set value='19' where name='tmtbl.db.version';


commit;
