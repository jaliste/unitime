/*
 * UniTime 3.2 (University Timetabling Application)
 * Copyright (C) 2010, UniTime LLC
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

insert into solver_parameter_group
	(uniqueid, name, description, condition, ord, param_type) values 
	(solver_parameter_group_seq.nextval, 'StudentSctWeights', 'Student Weights', '', -1, 2);

update solver_parameter_group g set g.ord = ( select max(x.ord)+1 from solver_parameter_group x )
	where g.name='StudentSctWeights';

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.Priority' as name, 
	'0.5010' as default_value, 
	'Priority' as description, 
	'double' as type, 
	0 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.FirstAlternative' as name, 
	'0.5010' as default_value, 
	'First alternative' as description, 
	'double' as type, 
	1 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.SecondAlternative' as name, 
	'0.2510' as default_value, 
	'Second alternative' as description, 
	'double' as type, 
	2 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.DistanceConflict' as name, 
	'0.0100' as default_value, 
	'Distance conflict' as description, 
	'double' as type, 
	3 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.TimeOverlapFactor' as name, 
	'0.5000' as default_value, 
	'Time overlap' as description, 
	'double' as type, 
	4 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.TimeOverlapMaxLimit' as name, 
	'0.5000' as default_value, 
	'Time overlap limit' as description, 
	'double' as type, 
	5 as ord, 
	0 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.BalancingFactor' as name, 
	'0.0050' as default_value, 
	'Section balancing' as description, 
	'double' as type, 
	6 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.AlternativeRequestFactor' as name, 
	'0.1260' as default_value, 
	'Alternative request (equal weights)' as description, 
	'double' as type, 
	7 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.LeftoverSpread' as name, 
	'false' as default_value, 
	'Spread leftover weight equaly' as description, 
	'boolean' as type, 
	8 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctWeights');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentWeights.Mode' as name, 
	'Priority' as default_value, 
	'Student weights' as description, 
	'enum(Priority,Equal,Legacy)' as type, 
	2 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSctBasic');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'StudentSct.TimeOverlaps' as name, 
	'true' as default_value, 
	'Use time overlaps' as description, 
	'boolean' as type, 
	26 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSct');

insert into solver_parameter_def
	(select solver_parameter_def_seq.nextval as uniqueid,
	'Load.TweakLimits' as name, 
	'false' as default_value, 
	'Tweak class limits to fit all enrolled students' as description, 
	'boolean' as type, 
	27 as ord, 
	1 as visible, 
	uniqueid as solver_param_group_id from solver_parameter_group where name='StudentSct');

/**
 * Update database version
 */

update application_config set value='64' where name='tmtbl.db.version';

commit;
		