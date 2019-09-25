// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Check for inconsistencies in lane information between relation and members.
 */
public class ConnectivityRelationCheck extends Test {

    protected static final int INCONSISTENT_LANE_COUNT = 9200;

    protected static final int UNKNOWN_CONNECTIVITY_ROLE = INCONSISTENT_LANE_COUNT + 1;

    protected static final int NO_CONNECTIVITY_TAG = INCONSISTENT_LANE_COUNT + 2;

    protected static final int TOO_MANY_ROLES = INCONSISTENT_LANE_COUNT + 3;

    private static final String CONNECTIVITY_TAG = "connectivity";
    private static final String VIA = "via";
    private static final String TO = "to";
    private static final String FROM = "from";

    /**
    * Constructor
    */
    public ConnectivityRelationCheck() {
        super(tr("Connectivity Relation Check"), tr("Checks that lane count of relation matches with lanes of members"));
    }

    //Get lane count of members from the 'connectivity' tag
    /**
     * Convert the connectivity tag into a map of values
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @return A Map in the form of {@code Map<Lane From, Map<Lane To, Optional>>}
     */
    private static Map<Integer, Map<Integer, Boolean>> parseConnectivityTag(Relation relation) {
        final String joined = relation.get(CONNECTIVITY_TAG);

        if (joined == null) {
            return new TreeMap<>();
        }

        final Map<Integer, Map<Integer, Boolean>> result = new HashMap<>();
        String[] lanes = joined.split("\\|", -1);
        for (int i = 0; i < lanes.length; i++) {
            String[] lane = lanes[i].split(":", -1);
            int laneNumber = Integer.parseInt(lane[0].trim());
            Map<Integer, Boolean> connections = new HashMap<>();
            String[] toLanes = Pattern.compile("\\p{Zs}*[,:;]\\p{Zs}*").split(lane[1]);
            for (int j = 0; j < toLanes.length; j++) {
                String toLane = toLanes[j].trim();
                if (Pattern.compile("\\([0-9]+\\)").matcher(toLane).matches()) {
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

    @Override
    public void visit(Relation r) {
        if (r.hasTag("type", CONNECTIVITY_TAG)) {
            if (!r.hasKey(CONNECTIVITY_TAG)) {
                errors.add(TestError.builder(this, Severity.WARNING, NO_CONNECTIVITY_TAG)
                        .message(tr("No connectivity tag in connectivity relation")).primitives(r).build());
            } else if (!r.hasIncompleteMembers()) {
                boolean badRole = checkForBadRole(r);
                if (!badRole)
                    checkForInconsistentLanes(r);
            }
        }
    }

    private void checkForInconsistentLanes(Relation relation) {
        // Lane count from connectivity tag
        Map<Integer, Map<Integer, Boolean>> connTagLanes = parseConnectivityTag(relation);
        // Lane count from member tags
        Map<String, Integer> roleLanes = new HashMap<>();

        for (RelationMember rM : relation.getMembers()) {
            // Check lanes
            if (rM.getType() == OsmPrimitiveType.WAY) {
                OsmPrimitive prim = rM.getMember();
                if (prim.hasKey("lanes") && !rM.getRole().equals(VIA)) {
                    roleLanes.put(rM.getRole(), Integer.parseInt(prim.get("lanes")));
                }
            }
        }
        boolean fromCheck = roleLanes.get(FROM) < Collections
                .max(connTagLanes.entrySet(), Comparator.comparingInt(Map.Entry::getKey)).getKey();
        boolean toCheck = false;
        for (Entry<Integer, Map<Integer, Boolean>> to : connTagLanes.entrySet()) {
            toCheck = roleLanes.get(TO) < Collections
                    .max(to.getValue().entrySet(), Comparator.comparingInt(Map.Entry::getKey)).getKey();
        }
        if (fromCheck || toCheck) {
            errors.add(TestError.builder(this, Severity.WARNING, INCONSISTENT_LANE_COUNT)
                    .message(tr("Inconsistent lane numbering between relation and members")).primitives(relation)
                    .build());
        }
    }

    private boolean checkForBadRole(Relation relation) {
        // Check role names
        int viaWays = 0;
        int viaNodes = 0;
        int toWays = 0;
        int fromWays = 0;
        for (RelationMember relationMember : relation.getMembers()) {
            if (relationMember.getMember() instanceof Way) {
                if (relationMember.hasRole(FROM))
                    fromWays++;
                else if (relationMember.hasRole(TO))
                    toWays++;
                else if (relationMember.hasRole(VIA))
                    viaWays++;
                else {
                    createUnknownRole(relation, relationMember.getMember());
                    return true;
                }
            } else if (relationMember.getMember() instanceof Node) {
                if (!relationMember.hasRole(VIA)) {
                    createUnknownRole(relation, relationMember.getMember());
                    return true;
                }
                viaNodes++;
            }
        }
        return mixedViaNodeAndWay(relation, viaWays, viaNodes, toWays, fromWays);
    }

    private boolean mixedViaNodeAndWay(Relation relation, int viaWays, int viaNodes, int toWays, int fromWays) {
        String message = "";
        if ((viaWays != 0 && viaNodes != 0) || viaNodes > 1) {
            message = tr("Relation contains {1} {0} roles.", VIA, viaWays + viaNodes);
        } else if (toWays != 1) {
            message = tr("Relation contains too many {0} roles", TO);
        } else if (fromWays != 1) {
            message = tr("Relation contains too many {0} roles", FROM);
        }
        if (message.isEmpty()) {
            return false;
        } else {
            errors.add(TestError.builder(this, Severity.WARNING, TOO_MANY_ROLES)
                    .message(message).primitives(relation).build());
            return true;
        }
    }

    private void createUnknownRole(Relation relation, OsmPrimitive primitive) {
        errors.add(TestError.builder(this, Severity.WARNING, UNKNOWN_CONNECTIVITY_ROLE)
                .message(tr("Unkown role in connectivity relation")).primitives(relation).highlight(primitive).build());
    }
}
