// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.model;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import com.kaart.laneconnectivity.CollectionUtils;

public final class Turn {
    /**
     * Get the relations that affect the primitive
     * @param c The {@code ModelContainer} which contains a model? TODO rephrase later
     * @param role The role that the primitive must have in the relation
     * @param primitive The OsmPrimitive that may have relations
     * @return A set of relations that contain the primitive with the specified role and match {@link Constants#TYPE_CONNECTION}
     */
    static Set<Turn> load(ModelContainer c, String role, OsmPrimitive primitive) {
        return load(c, role, primitive, Constants.TYPE_CONNECTIVITY);
    }

    /**
     * Get the relations that affect the primitive
     * @param c The {@code ModelContainer} which contains a model? TODO rephrase later
     * @param role The role that the primitive must have in the relation
     * @param primitive The OsmPrimitive that may have relations
     * @param relationType The relation type to load
     * @return A set of relations that contain the primitive with the specified role and match the relation type.
     */
    static Set<Turn> load(ModelContainer c, String role, OsmPrimitive primitive, String relationType) {
        final Set<Turn> result = new HashSet<>();

        for (Relation r : org.openstreetmap.josm.tools.Utils.filteredCollection(primitive.getReferrers(), Relation.class)) {
            if (!r.isUsable() || !r.get("type").equals(relationType)) {
                continue;
            }

            for (RelationMember m : r.getMembers()) {
                if (m.getRole().equals(role) && m.getMember().equals(primitive)) {
                    result.addAll(load(c, r));
                }
            }
        }

        return result;
    }

    static Set<Turn> load(ModelContainer c, Relation r) {
        for (RelationMember m : r.getMembers()) {
            if (m.getRole().equals(Constants.ROLE_VIA)) {
                if (m.isNode()) {
                    return loadWithViaNode(c, r);
                } else if (m.isWay()) {
                    return loadWithViaWays(c, r);
                }
            }
        }

        throw new IllegalArgumentException("No via node or way(s).");
    }

    private static Set<Turn> loadWithViaWays(ModelContainer c, Relation r) {
        final Way from = TurnlanesUtils.getMemberWay(r, Constants.ROLE_FROM);
        final Way to = TurnlanesUtils.getMemberWay(r, Constants.ROLE_TO);

        if (!c.hasRoad(from) || !c.hasRoad(to)) {
            return Collections.emptySet();
        }

        final List<Way> tmp = TurnlanesUtils.getMemberWays(r, Constants.ROLE_VIA);
        final LinkedList<Road> via = new LinkedList<>();

        final Road.End fromRoadEnd = c.getJunction(TurnlanesUtils.lineUp(from, tmp.get(0))).getRoadEnd(from);

        Node n = fromRoadEnd.getJunction().getNode();
        final Iterator<Way> it = tmp.iterator();
        while (it.hasNext()) {
            final Way w = it.next();
            if (!c.hasRoad(w)) {
                return Collections.emptySet();
            }

            final Road v = c.getRoad(w);
            via.add(v);
            n = TurnlanesUtils.getOppositeEnd(w, n);

            if (!v.isPrimary()) {
                throw new IllegalStateException("The road is not part of the junction.");
            }

            final Iterator<Route.Segment> it2 = (v.getRoute().getFirstSegment().getWay().equals(w) ? v.getRoute()
                    .getSegments() : CollectionUtils.reverse(v.getRoute().getSegments())).iterator();
            it2.next(); // first is done

            while (it2.hasNext()) {
                final Way w2 = it2.next().getWay();
                n = TurnlanesUtils.getOppositeEnd(w2, n);

                if (!it.hasNext() || !w2.equals(it.next())) {
                    throw new IllegalStateException("The via ways of the relation do not form a road.");
                }
            }
        }
        final Road.End toRoadEnd = c.getJunction(n).getRoadEnd(to);
        n = TurnlanesUtils.getOppositeEnd(to, n);

        final Set<Turn> result = new HashSet<>();
        // TODO don't use keySet, use all of the information.
        for (int i : indices(r, Constants.TURN_KEY_LANES).keySet()) {
            result.add(new Turn(r, fromRoadEnd.getLane(Lane.Kind.REGULAR, i), via, toRoadEnd));
        }
        return result;
    }

    /**
     * Splits a key based off of a split pattern
     * @param r The relation with the key-value to split
     * @param key The key that needs to be split
     * @return A map of a map of Integers (<Lane From, <Lane To, Optional>>). Lanes counts start at 1.
     */
    static Map<Integer, Map<Integer, Boolean>> indices(Relation r, String key) {
        final String joined = r.get(key);

        if (joined == null) {
            return Collections.emptyMap();
        }

        final Map<Integer, Map<Integer, Boolean>> result = new HashMap<>();
        String[] lanes = joined.split(Constants.LANE_SEPARATOR);
        for (int i = 0; i < lanes.length; i++) {
            String[] lane = lanes[i].split(Constants.CONNECTIVITY_TO_FROM_SEPARATOR);
            int laneNumber = Integer.parseInt(lane[0].trim());
            Map<Integer, Boolean> connections = new HashMap<>();
            String[] toLanes = Constants.SPLIT_PATTERN.split(lane[1]);
            for (int j = 0; j < toLanes.length; j++) {
                String toLane = toLanes[j].trim();
                if (Constants.CONNECTIVITY_OPTIONAL_LANES_PATTERN.matcher(toLane).matches()) {
                    toLane = toLane.replace("(", "").replace(")", "").trim();
                    connections.put(Integer.parseInt(toLane), true);
                } else {
                    connections.put(Integer.parseInt(toLane), false);
                }
            }
            result.put(laneNumber, connections);
        }
        return result;
    }

    private static Set<Turn> loadWithViaNode(ModelContainer c, Relation r) {
        final Way from = TurnlanesUtils.getMemberWay(r, Constants.ROLE_FROM);
        final Node via = TurnlanesUtils.getMemberNode(r, Constants.ROLE_VIA);
        final Way to = TurnlanesUtils.getMemberWay(r, Constants.ROLE_TO);

        if (!c.hasRoad(from) || !c.hasJunction(via) || !c.hasRoad(to)) {
            return Collections.emptySet();
        }

        final Junction j = c.getJunction(via);

        final Road.End fromRoadEnd = j.getRoadEnd(from);
        final Road.End toRoadEnd = j.getRoadEnd(to);

        final Set<Turn> result = new HashSet<>();
        // TODO don't use keySet, use all of the information.
        for (int i : indices(r, Constants.TYPE_CONNECTIVITY).keySet()) {
            result.add(new Turn(r, fromRoadEnd.getLane(Lane.Kind.REGULAR, i), Collections.<Road>emptyList(), toRoadEnd));
        }
        return result;
    }

    static String join(List<Integer> list) {
        if (list.isEmpty()) {
            return null;
        }

        final StringBuilder builder = new StringBuilder(list.size() * (2 + Constants.SEPARATOR.length()));

        for (int e : list) {
            builder.append(e).append(Constants.SEPARATOR);
        }

        builder.setLength(builder.length() - Constants.SEPARATOR.length());
        return builder.toString();
    }

    private final Relation relation;

    private final Lane from;
    private final List<Road> via;
    private final Road.End to;

    // TODO replace Road.End to with a Lane to
    public Turn(Relation relation, Lane from, List<Road> via, Road.End to) {
        this.relation = relation;
        this.from = from;
        this.via = via;
        this.to = to;
    }

    public Lane getFrom() {
        return from;
    }

    public List<Road> getVia() {
        return via;
    }

    public Road.End getTo() {
        return to;
    }

    Relation getRelation() {
        return relation;
    }

    public void remove() {
        final GenericCommand cmd = new GenericCommand(relation.getDataSet(), tr("Delete turn."));

        remove(cmd);

        UndoRedoHandler.getInstance().add(cmd);
    }

    void remove(GenericCommand cmd) {
        // TODO don't use keySet, use all of the information.
        final List<Integer> lanes = new ArrayList<>(indices(relation, Constants.TURN_KEY_LANES).keySet());
        final List<Integer> extraLanes = new ArrayList<>(indices(relation, Constants.TURN_KEY_EXTRA_LANES).keySet());

        // TODO understand & document
        if (lanes.size() + extraLanes.size() == 1 && (from.isExtra() ^ !lanes.isEmpty())) {
            cmd.backup(relation).setDeleted(true);
            // relation.getDataSet().removePrimitive(relation.getPrimitiveId());
        } else if (from.isExtra()) {
            extraLanes.remove(Integer.valueOf(from.getIndex()));
        } else {
            lanes.remove(Integer.valueOf(from.getIndex()));
        }

        cmd.backup(relation).put(Constants.TURN_KEY_LANES, lanes.isEmpty() ? null : join(lanes));
        cmd.backup(relation).put(Constants.TURN_KEY_EXTRA_LANES, extraLanes.isEmpty() ? null : join(extraLanes));
    }

    void fixReferences(GenericCommand cmd, boolean left, int index) {
        final List<Integer> fixed = new ArrayList<>();
        // TODO don't use keySet, use all of the information.
        for (int i : indices(relation, Constants.TURN_KEY_EXTRA_LANES).keySet()) {
            if (left ? i < index : i > index) {
                fixed.add(left ? i + 1 : i - 1);
            } else {
                fixed.add(i);
            }
        }

        cmd.backup(relation).put(Constants.TURN_KEY_EXTRA_LANES, join(fixed));
    }
}
