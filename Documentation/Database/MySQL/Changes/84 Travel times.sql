/*
 * UniTime 3.3 (University Timetabling Application)
 * Copyright (C) 2008 - 2011, UniTime LLC
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

create table travel_time (
	uni	queid decimal(20,0) primary key not null,
		session_id decimal(20,0) not null,
		loc1_id decimal(20,0) not null,
		loc2_id decimal(20,0) not null,
		distance decimal(10,0) not null
	) engine = INNODB;

alter table travel_time add constraint fk_trvltime_session foreign key (session_id)
	references sessions (uniqueid) on delete cascade;

/*
 * Update database version
 */

update application_config set value='84' where name='tmtbl.db.version';

commit;
