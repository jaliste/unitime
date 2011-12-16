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

select 32767 * next_hi into @id from hibernate_unique_key;

insert into distribution_type (uniqueid, reference, label, sequencing_required, req_id, allowed_pref, description, abbreviation, instructor_pref, exam_pref) values
	(@id, 'MAX_HRS_DAY(5)', 'At Most 5 Hours A Day', 0, 43, '210R',
		'Classes are to be placed in a way that there is no more than five hours in any day.',
		'At Most 5 Hrs', 1, 0),
	(@id + 1, 'BTB_PRECEDENCE', 'Back-To-Back Precedence', 0, 44, 'P43210R',
		'Given classes have to be taught in the given order, on the same days, and in adjacent time segments.<br>When prohibited or (strongly) discouraged: Given classes have to be taught in the given order, on the same days, but cannot be back-to-back.',
		'BTB Precede', 0, 0),
	(@id + 2, 'SAME_D_T', 'Same Days-Time', 0, 45, 'P43210R',
		'Given classes must be taught at the same time of day and on the same days.<br>This constraint combines Same Days and Same Time distribution preferences.<br>When prohibited or (strongly) discouraged: Any pair of classes classes cannot be taught on the same days during the same time.',
		'Same Days-Time', 0, 0),
	(@id + 3, 'SAME_D_R_T', 'Same Days-Room-Time', 0, 46, 'P43210R',
		'Given classes must be taught at the same time of day, on the same days and in the same room.<br>Note that this constraint is the same as Meet Together constraint, except it does not allow for room sharing. In other words, it is only useful when these classes are taught during non-overlapping date patterns.<br>When prohibited or (strongly) discouraged: Any pair of classes classes cannot be taught on the same days during the same time in the same room.',
		'Same Days-Room-Time', 0, 0),
	(@id + 4, 'SAME_WEEKS', 'Same Weeks', 0, 47, 'P43210R',
		'Given classes must be taught during the same weeks (i.e., must have the same date pattern).<br>When prohibited or (strongly) discouraged: any two classes must have non overlapping date patterns.',
		'Same Weeks', 0, 0);

update hibernate_unique_key set next_hi=next_hi+1;

/*
 * Update database version
 */

update application_config set value='83' where name='tmtbl.db.version';

commit;
