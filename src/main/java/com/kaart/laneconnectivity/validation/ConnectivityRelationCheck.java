// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.validation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Check for inconsistencies in lane information between relation and members.
 */
public class ConnectivityRelationCheck extends Test {

    protected static final int INCONSISTENT_LANE_COUNT = 3800;

    protected static final int UNKNOWN_CONNECTIVITY_ROLE = 3801;

    protected static final int NO_CONNECTIVITY_TAG = 3802;

    /**
    * Constructor
    */
    public ConnectivityRelationCheck() {
        super(tr("Connectivity Relation Check"), tr("Checks that lane count of relation matches with lanes of members"));
    }

    //Get lane count of members from the 'connectivity' tag
    private Map<String, Integer> countLanesFromConnTag(Relation r) {
        Map<String, Integer> laneInfo = new HashMap<String, Integer>();
        if (r.hasKey("connectivity")) {
            String connectivityString = r.get("connectivity");
            laneInfo.put("from", 0);
            laneInfo.put("to", 0);
            String roleString = "from";
            for (char ch : connectivityString.toCharArray()) {
                if (Character.isDigit(ch)) {
                    int laneNum = Character.getNumericValue(ch);
                    if (laneNum > laneInfo.get(roleString)) {
                        laneInfo.put(roleString, laneNum);
                    }
                } else if (ch == ':') {
                    roleString = "to";
                } else if (ch == '|') {
                    roleString = "from";
                }
            }
        }
        return laneInfo;
    }

    @Override
    public void visit(Relation r) {
        if (r.hasTag("type", "connectivity")) {
            //No Connectivity, member outside
            boolean incompleteMember = false, badRole = false;
            boolean noConnectivityTag = !r.hasKey("connectivity");
            //Lane count from connectivity tag
            Map<String, Integer> connTagLanes = countLanesFromConnTag(r);
            //Lane count from member tags
            Map<String, Integer> roleLanes = new HashMap<String, Integer>();
            for (RelationMember rM : r.getMembers()) {
                if (!incompleteMember) {
                    incompleteMember = rM.toString().contains("incomplete");
                }
                //Check role names
                String role = rM.getRole();
                if (!badRole) {
                    badRole = (!role.equals("from") && !role.equals("to") && !role.equals("via"));
                }
                //Check lanes
                if (rM.getType() == OsmPrimitiveType.WAY) {
                    OsmPrimitive prim = rM.getMember();
                    if (prim.hasKey("lanes")) {
                        if (!rM.getRole().equals("via")) {
                            roleLanes.put(rM.getRole(), Integer.parseInt(prim.get("lanes")));
                        }
                    }
                }
            }
            if (!incompleteMember) {
                if (noConnectivityTag) {
                    errors.add(TestError
                            .builder(this, Severity.WARNING, NO_CONNECTIVITY_TAG)
                            .message(tr("kaart"), tr("No connectivity tag in connectivity relation"))
                            .primitives(r)
                            .build());
                } else {
                    boolean fromCheck = roleLanes.get("from") < connTagLanes.get("from");
                    boolean toCheck = roleLanes.get("to") < connTagLanes.get("to");
                    if (fromCheck || toCheck) {
                        errors.add(TestError
                                .builder(this, Severity.WARNING, INCONSISTENT_LANE_COUNT)
                                .message(tr("kaart"), tr("Inconsistent lane numbering between relation and members"))
                                .primitives(r)
                                .build());
                    }
                }
                if (badRole) {
                    errors.add(TestError
                            .builder(this, Severity.WARNING, UNKNOWN_CONNECTIVITY_ROLE)
                            .message(tr("kaart"), tr("Unkown role in connectivity relation"))
                            .primitives(r)
                            .build());
                }
            }
        }
    }
}
