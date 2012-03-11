/*
 * UniTime 3.4 (University Timetabling Application)
 * Copyright (C) 2012, UniTime LLC, and individual contributors
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
package org.unitime.timetable.events;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.DistanceMetric;

import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.client.events.UniTimeFilterBox.FilterRpcRequest;
import org.unitime.timetable.gwt.client.events.UniTimeFilterBox.FilterRpcResponse;
import org.unitime.timetable.gwt.client.events.UniTimeFilterBox.FilterRpcResponse.Entity;
import org.unitime.timetable.gwt.server.Query;
import org.unitime.timetable.gwt.server.Query.TermMatcher;
import org.unitime.timetable.model.Building;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentRoomFeature;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.GlobalRoomFeature;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.Room;
import org.unitime.timetable.model.RoomDept;
import org.unitime.timetable.model.RoomFeature;
import org.unitime.timetable.model.RoomGroup;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.TravelTime;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.RoomDAO;

public class RoomFilterBackend extends FilterBoxBackend {
	private DistanceMetric iMetrics;
	private static double EPSILON = 0.000001;
	private static DecimalFormat sCDF = new DecimalFormat("0.000000");
	
	private static enum Size {
		eq, lt, gt, le, ge
	};

	@Override
	public void load(FilterRpcRequest request, FilterRpcResponse response) {
		Set<String> eventDepts = new HashSet<String>(
				DepartmentDAO.getInstance().getSession().createQuery(
						"select distinct d.deptCode from Department d inner join d.timetableManagers m inner join m.managerRoles mr " +
						"where d.session.uniqueId = :sessionId and mr.role.reference=:eventMgr"
				)
				.setLong("sessionId", request.getSessionId())
				.setString("eventMgr", Roles.EVENT_MGR_ROLE)
				.setCacheable(true)
				.list());

		Set<String> eventRoomTypes = new HashSet<String>(
				DepartmentDAO.getInstance().getSession().createQuery(
						"select o.roomType.label from RoomTypeOption o where o.status = 1 and o.session.uniqueId = :sessionId"
				)
				.setLong("sessionId", request.getSessionId())
				.setCacheable(true)
				.list());
		
		Set<String> departments = request.getOptions("department");
		
		Set<String> userDepts = null;
		if (request.hasOption("user")) {
			TimetableManager m = TimetableManager.findByExternalId(request.getOption("user"));
			if (m != null) {
				userDepts = new HashSet<String>();
				for (Department d: m.getDepartments())
					if (d.getSessionId().equals(request.getSessionId())) userDepts.add(d.getDeptCode());
			}
		}
		
		Map<Long, Entity> types = new HashMap<Long, Entity>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), null, -1, null, "type")) {
			Entity type = types.get(location.getRoomType().getUniqueId());
			if (type == null) {
				type = new Entity(location.getRoomType().getUniqueId(), location.getRoomType().getReference(), location.getRoomType().getLabel());
				types.put(type.getUniqueId(), type);
			}
			type.incCount();
		}
		response.add("type", new TreeSet<Entity>(types.values()));
		
		Map<Long, Entity> features = new HashMap<Long, Entity>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), null, -1, null, null)) {
			for (RoomFeature rf: location.getFeatures()) {
				if (rf instanceof GlobalRoomFeature || (rf instanceof DepartmentRoomFeature && departments != null && departments.contains(((DepartmentRoomFeature)rf).getDepartment().getDeptCode()))) {
					Entity feature = features.get(rf.getUniqueId());
					if (feature == null) {
						feature = new Entity(rf.getUniqueId(), rf.getAbbv(), rf.getLabel());
						features.put(feature.getUniqueId(), feature);
					}
					feature.incCount();
				}
			}
		}
		response.add("feature", new TreeSet<Entity>(features.values()));
		
		Map<Long, Entity> groups = new HashMap<Long, Entity>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), null, -1, null, "group")) {
			for (RoomGroup rg: location.getRoomGroups()) {
				if (rg.isGlobal() || (departments != null && departments.contains(rg.getDepartment().getDeptCode()))) {
					Entity group = groups.get(rg.getUniqueId());
					if (group == null) {
						group = new Entity(rg.getUniqueId(), rg.getAbbv(), rg.getName());
						groups.put(group.getUniqueId(), group);
					}
					group.incCount();
				}
			}
		}
		response.add("group", new TreeSet<Entity>(groups.values()));
		
		Map<Long, Entity> buildings = new HashMap<Long, Entity>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), null, -1, null, "building")) {
			if (location instanceof Room) {
				Room room = (Room)location;
				Entity building = buildings.get(room.getBuilding().getUniqueId());
				if (building == null) {
					building = new Entity(room.getBuilding().getUniqueId(), room.getBuilding().getAbbreviation(), room.getBuilding().getAbbrName());
					buildings.put(building.getUniqueId(), building);
				}
				building.incCount();
			}
		}
		response.add("building", new TreeSet<Entity>(buildings.values()));

		Entity event = new Entity(-1l, "Event", "Event Rooms");
		Entity managed = new Entity(-2l, "Managed", "Managed Rooms");
		Entity examFinal = new Entity(-3l, "Final", "Final Examination Rooms");
		Entity examMidterm = new Entity(-4l, "Midterm", "Midterm Examination Rooms");
		Map<Long, Entity> depts = new HashMap<Long, Entity>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), null, -1, null, "department")) {
			boolean isManaged = false;
			for (RoomDept rd: location.getRoomDepts()) {
				if (rd.isControl() && eventRoomTypes.contains(location.getRoomType().getLabel()) && eventDepts.contains(rd.getDepartment().getDeptCode()))
					event.incCount();
				Entity department = depts.get(rd.getDepartment().getUniqueId());
				if (department == null) {
					department = new Entity(rd.getDepartment().getUniqueId(), rd.getDepartment().getDeptCode(),
							rd.getDepartment().getDeptCode() + " - " + rd.getDepartment().getName() + (rd.getDepartment().isExternalManager() ? " (" + rd.getDepartment().getExternalMgrLabel() + ")" : ""));
					depts.put(department.getUniqueId(), department);
				}
				department.incCount();
				if (userDepts != null && userDepts.contains(rd.getDepartment().getDeptCode())) isManaged = true;
			}
			if (location.isExamEnabled(Exam.sExamTypeFinal))
				examFinal.incCount();
			if (location.isExamEnabled(Exam.sExamTypeMidterm))
				examMidterm.incCount();
			if (isManaged)
				managed.incCount();
		}
		if (event.getCount() > 0)
			response.add("department", event);
		if (managed.getCount() > 0)
			response.add("department",managed);
		if (examFinal.getCount() > 0)
			response.add("department",examFinal);
		if (examMidterm.getCount() > 0)
			response.add("department", examMidterm);
		response.add("department", new TreeSet<Entity>(depts.values()));
	}
	
	public List<Location> locations(Long sessionId, Map<String, Set<String>> options, Query query, int limit, Map<Long, Double> room2distance, String ignoreCommand) {
		org.hibernate.Session hibSession = RoomDAO.getInstance().getSession();

		Set<String> type = (options == null || "type".equals(ignoreCommand) ? null : options.get("type"));
		Set<String> feature = (options == null || "feature".equals(ignoreCommand) ? null : options.get("feature"));
		Set<String> group = (options == null || "group".equals(ignoreCommand) ? null : options.get("group"));
		Set<String> building = (options == null || "building".equals(ignoreCommand) ? null : options.get("building"));
		Set<String> department = (options == null || "department".equals(ignoreCommand) ? null : options.get("department"));
		Set<String> groupFeaturedept = (options == null ? null : options.get("department"));
		Set<String> size = (options == null || "size".equals(ignoreCommand) ? null : options.get("size"));
		Set<String> flag = (options == null || "flag".equals(ignoreCommand) ? null : options.get("flag"));
		Set<String> user = (options == null || "user".equals(ignoreCommand) ? null : options.get("user"));
		
		int min = 0, max = Integer.MAX_VALUE;
		boolean nearby = (flag != null && flag.contains("nearby"));
		if (size != null && !size.isEmpty()) {
			Size prefix = Size.eq;
			String number = size.iterator().next();
			if (number.startsWith("<=")) { prefix = Size.le; number = number.substring(2); }
			else if (number.startsWith(">=")) { prefix = Size.ge; number = number.substring(2); }
			else if (number.startsWith("<")) { prefix = Size.lt; number = number.substring(1); }
			else if (number.startsWith(">")) { prefix = Size.gt; number = number.substring(1); }
			else if (number.startsWith("=")) { prefix = Size.eq; number = number.substring(1); }
			try {
				int a = Integer.parseInt(number);
				switch (prefix) {
					case eq: min = max = a; break; // = a
					case le: max = a; break; // <= a
					case ge: min = a; break; // >= a
					case lt: max = a - 1; break; // < a
					case gt: min = a + 1; break; // > a
				}
			} catch (NumberFormatException e) {}
			if (number.contains("..")) {
				try {
					String a = number.substring(0, number.indexOf('.'));
					String b = number.substring(number.indexOf("..") + 2);
					min = Integer.parseInt(a); max = Integer.parseInt(b);
				} catch (NumberFormatException e) {}
			}
		}
		
		List<Location> locations = (department != null && department.contains("Event") ? (List<Location>)hibSession.createQuery("select distinct l from" +
				" Location l inner join l.roomDepts rd" +
				" inner join rd.department.timetableManagers m" +
				" inner join m.managerRoles mr " +
				" left outer join l.roomType t " +
				" left outer join l.roomGroups g " +
				" left outer join l.features f " +
				" ,RoomTypeOption o" +
				" where" +
				" l.session.uniqueId = :sessionId and" +
				" rd.control=true and mr.role.reference=:eventMgr and" +
				" o.status = 1 and o.roomType = l.roomType and o.session = l.session")
				.setLong("sessionId", sessionId)
				.setString("eventMgr", Roles.EVENT_MGR_ROLE)
				.setCacheable(true)
				.list() : department != null && department.contains("Managed") && user != null && !user.isEmpty() ?
				(List<Location>)hibSession.createQuery("select distinct l from" +
				" Location l inner join l.roomDepts rd" +
				" left outer join l.roomType t " +
				" left outer join l.roomGroups g " +
				" left outer join l.features f " +
				" inner join rd.department.timetableManagers m" +
				" left outer join m.managerRoles mr " +
				" where" +
				" l.session.uniqueId = :sessionId and m.externalUniqueId = :user")
				.setLong("sessionId", sessionId)
				.setString("user", user.iterator().next())
				.setCacheable(true)
				.list() :
				(List<Location>)hibSession.createQuery("select distinct l from" +
				" Location l left outer join l.roomDepts rd" +
				" left outer join l.roomType t " +
				" left outer join l.roomGroups g " +
				" left outer join l.features f " +
				" left outer join rd.department.timetableManagers m" +
				" left outer join m.managerRoles mr " +
				" where" +
				" l.session.uniqueId = :sessionId")
				.setLong("sessionId", sessionId)
				.setCacheable(true)
				.list());
		
		List<Location> ret = new ArrayList<Location>();
		locations: for (Location location: locations) {
			if (size != null && !size.isEmpty() && (location.getCapacity() < min || location.getCapacity() > max)) continue;
			if (query != null && !query.match(new LocationMatcher(location))) continue;
			if (type != null && !type.isEmpty() && !type.contains(location.getRoomType().getLabel())) continue;
			if (building != null && !building.isEmpty() && (!(location instanceof Room) || !building.contains(((Room)location).getBuilding().getAbbreviation()))) continue;
			if (feature != null && !feature.isEmpty()) {
				for (String f: feature) {
					boolean found = false;
					for (RoomFeature rf: location.getFeatures())
						if (rf instanceof GlobalRoomFeature) {
							if (f.equals(rf.getLabel())) { found = true; break; }
						} else if (rf instanceof DepartmentRoomFeature && groupFeaturedept != null && !groupFeaturedept.isEmpty()) {
							if (groupFeaturedept.contains(((DepartmentRoomFeature)rf).getDepartment().getDeptCode()) && f.equals(rf.getLabel())) { found = true; break; }
						}
					if (!found) continue locations;
				}
			}
			if (group != null && !group.isEmpty()) {
				boolean found = false;
				for (RoomGroup rg: location.getRoomGroups()) {
					if (rg.isGlobal()) {
						if (group.contains(rg.getName())) { found = true; break; }
					} else if (groupFeaturedept != null && !groupFeaturedept.isEmpty() && groupFeaturedept.contains(rg.getDepartment().getDeptCode())) {
						if (group.contains(rg.getName())) { found = true; break; }	
					}
				}
				if (!found) continue;
			}
			if (department == null || department.isEmpty()) {
			} else if (department.contains("Final")) {
				if (!location.isExamEnabled(Exam.sExamTypeFinal)) continue;
			} else if (department.contains("Midterm")) {
				if (!location.isExamEnabled(Exam.sExamTypeMidterm)) continue;
			} else if (!department.contains("Event") && !department.contains("Managed")) {
				boolean found = false;
				for (RoomDept rd: location.getRoomDepts())
					if (department.contains(rd.getDepartment().getDeptCode())) { found = true; break; }
				if (!found) continue;
			}
			ret.add(location);
		}
		
		final Map<Long, Double> distances = (room2distance == null ? new Hashtable<Long, Double>() : room2distance);
		if (nearby && building != null && !building.isEmpty() && (limit <= 0 || ret.size() < limit)) {
			double allowedDistance = Double.parseDouble(ApplicationProperties.getProperty("tmtbl.events.nearByDistance", "670")); // 670m by default
			Set<Coordinates> coord = new HashSet<Coordinates>();
			for (Location location: ret)
				coord.add(new Coordinates(location));

			String buildingOnlyFilter = "";
			for (String b: building)
				buildingOnlyFilter += (buildingOnlyFilter.isEmpty() ? "" : " or ") + "b.abbreviation = '" + b + "'";
			
			if (coord.isEmpty()) {
				for (Building b: (List<Building>)hibSession.createQuery("select b from Building b where" +
						" b.session.uniqueId = :sessionId and (" + buildingOnlyFilter + ")").setLong("sessionId", sessionId).list()) {
					coord.add(new Coordinates(-b.getUniqueId(), b.getCoordinateX(), b.getCoordinateY()));
				}
			}
			
			if (!coord.isEmpty()) {
				locations: for (Location location: locations) {
					if (size != null && !size.isEmpty() && (location.getCapacity() < min || location.getCapacity() > max)) continue;
					if (query != null && !query.match(new LocationMatcher(location))) continue;
					if (type != null && !type.isEmpty() && !type.contains(location.getRoomType().getLabel())) continue;
					if (feature != null && !feature.isEmpty()) {
						for (String f: feature) {
							boolean found = false;
							for (RoomFeature rf: location.getFeatures())
								if (rf instanceof GlobalRoomFeature) {
									if (f.equals(rf.getLabel())) { found = true; break; }
								} else if (rf instanceof DepartmentRoomFeature && groupFeaturedept != null && !groupFeaturedept.isEmpty()) {
									if (groupFeaturedept.contains(((DepartmentRoomFeature)rf).getDepartment().getDeptCode()) && f.equals(rf.getLabel())) { found = true; break; }
								}
							if (!found) continue locations;
						}
					}
					if (group != null && !group.isEmpty()) {
						boolean found = false;
						for (RoomGroup rg: location.getRoomGroups()) {
							if (rg.isGlobal()) {
								if (group.contains(rg.getName())) { found = true; break; }
							} else if (groupFeaturedept != null && !groupFeaturedept.isEmpty() && groupFeaturedept.contains(rg.getDepartment().getDeptCode())) {
								if (group.contains(rg.getName())) { found = true; break; }	
							}
						}
						if (!found) continue;
					}
					if (department == null || department.isEmpty()) {
					} else if (department.contains("Final")) {
						if (!location.isExamEnabled(Exam.sExamTypeFinal)) continue;
					} else if (department.contains("Midterm")) {
						if (!location.isExamEnabled(Exam.sExamTypeMidterm)) continue;
					} else if (!department.contains("Event") && !department.contains("Managed")) {
						boolean found = false;
						for (RoomDept rd: location.getRoomDepts())
							if (department.contains(rd.getDepartment().getDeptCode())) { found = true; break; }
						if (!found) continue;
					}
					Coordinates c = new Coordinates(location);
					Double distance = null;
					for (Coordinates x: coord) {
						double d = c.distance(x);
						if (distance == null || distance > d) distance = d;
					}
					if (distance != null && distance <= allowedDistance) {
						ret.add(location);
						if (distances != null) distances.put(location.getUniqueId(), distance);
					}
				}
			}
		}
		
		final boolean sortBySize = (size != null && !size.isEmpty());
		Collections.sort(ret, new Comparator<Location>() {
			@Override
			public int compare(Location l1, Location l2) {
				Double d1 = distances.get(l1.getUniqueId());
				Double d2 = distances.get(l2.getUniqueId());
				if (d1 == null && d2 != null) return -1;
				if (d1 != null && d2 == null) return 1;
				if (d1 != null) {
					int cmp = new Long(Math.round(d1)).compareTo(Math.round(d2));
					if (cmp != 0) return cmp;
				}
				if (sortBySize) {
					int cmp = new Integer(l1.getCapacity() != null ? l1.getCapacity() : Integer.MAX_VALUE).compareTo(l2.getCapacity() != null ? l2.getCapacity() : Integer.MAX_VALUE);
					if (cmp != 0) return cmp;
				}
				return l1.getLabel().compareTo(l2.getLabel());
			}
		});
		
		return (limit <= 0 || ret.size() < limit ? ret : ret.subList(0, limit));
	}
	
	@Override
	public void suggestions(FilterRpcRequest request, FilterRpcResponse response) {
		Map<Long, Double> distances = new HashMap<Long, Double>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), new Query(request.getText()), 20, distances, null)) {
			String hint = location.getRoomTypeLabel() + ", " + location.getCapacity() + " seats";
			Double dist = distances.get(location.getUniqueId());
			if (dist != null) hint += ", " + Math.round(dist) + " m";
			response.addSuggestion(location.getLabel(), location.getLabel(), "(" + hint + ")");
		}
	}
	
	@Override
	public void enumarate(FilterRpcRequest request, FilterRpcResponse response) {
		Map<Long, Double> distances = new HashMap<Long, Double>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), new Query(request.getText()), -1, distances, null)) {
			Double dist = distances.get(location.getUniqueId());
			String hint = location.getHtmlHint().replace("\\'", "'");
			if (dist != null)
				hint = hint.replace("</table>", "<tr><td>Distance:</td><td>" + Math.round(dist) + " m</td></tr></table>");
			response.addResult(new Entity(
					location.getUniqueId(),
					location.getDisplayName(),
					location.getLabel(),
					"type", location.getRoomType().getLabel(),
					"capacity", location.getCapacity().toString(),
					"distance", String.valueOf(dist == null ? 0l : Math.round(dist)),
					"mouseOver", hint));
		}
	}

	public DistanceMetric getDistanceMetric() {
		if (iMetrics == null) {
			DataProperties config = new DataProperties();
			config.setProperty("Distances.Ellipsoid", ApplicationProperties.getProperty("unitime.distance.ellipsoid", DistanceMetric.Ellipsoid.LEGACY.name()));
			iMetrics = new DistanceMetric(new DataProperties(config));
			TravelTime.populateTravelTimes(iMetrics);
		}
		return iMetrics;
	}
	
	public class Coordinates {
		Long iId;
		Double iX, iY;
		public Coordinates(Long id, Double x, Double y) { iId = id; iX = x; iY = y; }
		public Coordinates(Location location) { iId = location.getUniqueId(); iX = location.getCoordinateX(); iY = location.getCoordinateY(); }
		
		public Long id() { return iId; }
		public Double x() { return iX; }
		public Double y() { return iY; }
		public boolean hasCoordinates() { return iX != null && iY != null; }
		
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Coordinates)) return false;
			Coordinates c = (Coordinates)o;
			if (!hasCoordinates()) return !c.hasCoordinates();
			else if (!c.hasCoordinates()) return false;
			return Math.abs(c.x() - x()) < EPSILON && Math.abs(c.y() - y()) < EPSILON;
		}
		
		public double distance(Coordinates c) {
			return (hasCoordinates() && c.hasCoordinates() ? getDistanceMetric().getDistanceInMeters(id(), x(), y(), c.id(), c.x(), c.y()) : Double.POSITIVE_INFINITY); 
		}
		
		public double distance(Location location) {
			return distance(new Coordinates(location));
		}
		
		public String toString() {
			return (hasCoordinates() ? sCDF.format(iX) + "," + sCDF.format(iY) : "");
		}
		
		public int hashCode() {
			return toString().hashCode();
		}
	}
	
	public class LocationMatcher implements TermMatcher {
		private Location iLocation;
		
		LocationMatcher(Location location) {
			iLocation = location;
		}
		
		public Location getLocation() { return iLocation; }

		@Override
		public boolean match(String attr, String term) {
			if (attr == null || attr.isEmpty()) {
				return (has(getLocation().getLabel(), term) || has(getLocation().getDisplayName(), term) ||
						getLocation().getLabel().toLowerCase().startsWith(term.toLowerCase()) || 
						(getLocation() instanceof Room && ((Room)getLocation()).getRoomNumber().toLowerCase().startsWith(term.toLowerCase())));
			} else if ("feature".equals(attr)) {
				for (RoomFeature rf: getLocation().getFeatures())
					if (rf instanceof GlobalRoomFeature && (eq(rf.getAbbv(), term) || has(rf.getLabel(), term))) return true;
				return false;
			} else if ("group".equals(attr)) {
				for (RoomGroup rg: getLocation().getRoomGroups())
					if (rg.isGlobal() && (eq(rg.getAbbv(), term) || has(rg.getName(), term))) return true;
				return false;
			} else if ("type".equals(attr)) {
				return eq(getLocation().getRoomType().getReference(), term) || has(getLocation().getRoomType().getLabel(), term);
			} else if ("building".equals(attr)) {
				if (getLocation() instanceof Room) {
					Building building = ((Room)getLocation()).getBuilding();
					return eq(building.getAbbreviation(), term) || has(building.getName(), term);
				}
				return false;
			} else if ("size".equals(attr)) {
				int min = 0, max = Integer.MAX_VALUE;
				Size prefix = Size.eq;
				String number = term;
				if (number.startsWith("<=")) { prefix = Size.le; number = number.substring(2); }
				else if (number.startsWith(">=")) { prefix = Size.ge; number = number.substring(2); }
				else if (number.startsWith("<")) { prefix = Size.lt; number = number.substring(1); }
				else if (number.startsWith(">")) { prefix = Size.gt; number = number.substring(1); }
				else if (number.startsWith("=")) { prefix = Size.eq; number = number.substring(1); }
				try {
					int a = Integer.parseInt(number);
					switch (prefix) {
						case eq: min = max = a; break; // = a
						case le: max = a; break; // <= a
						case ge: min = a; break; // >= a
						case lt: max = a - 1; break; // < a
						case gt: min = a + 1; break; // > a
					}
				} catch (NumberFormatException e) {}
				if (term.contains("..")) {
					try {
						String a = term.substring(0, term.indexOf('.'));
						String b = term.substring(term.indexOf("..") + 2);
						min = Integer.parseInt(a); max = Integer.parseInt(b);
					} catch (NumberFormatException e) {}
				}
				return min <= getLocation().getCapacity() && getLocation().getCapacity() <= max;
			} else {
				return true;
			}
		}
		
		private boolean eq(String name, String term) {
			if (name == null) return false;
			return name.equalsIgnoreCase(term);
		}

		private boolean has(String name, String term) {
			if (name == null) return false;
			if (eq(name, term)) return true;
			for (String t: name.split(" |,"))
				if (t.equalsIgnoreCase(term)) return true;
			return false;
		}

		
	}
}
