package com.kaart.laneconnectivity.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

public class ConnectivityRelationCheckTest {
    private ConnectivityRelationCheck check;
    private static final String CONNECTIVITY = "connectivity";
    /**
     * Setup test.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
        check = new ConnectivityRelationCheck();
    }

    private Relation createDefaultTestRelation() {
        Node connection = new Node(new LatLon(0, 0));
        return TestUtils.newRelation("type=connectivity connectivity=1:1",
                new RelationMember("from", TestUtils.newWay("lanes=4", new Node(new LatLon(-0.1, -0.1)), connection)),
                new RelationMember("via", connection),
                new RelationMember("to", TestUtils.newWay("lanes=4", connection, new Node(new LatLon(0.1, 0.1)))));
    }

    @Test
    public void testNoTypeTag() {
        Relation relation = createDefaultTestRelation();
        check.visit(relation);

        Assert.assertEquals(0, check.getErrors().size());

        relation.remove(CONNECTIVITY);
        check.visit(relation);
        Assert.assertEquals(1, check.getErrors().size());
    }

    @Test
    public void testMisMatchedLanes() {
        Relation relation = createDefaultTestRelation();
        check.visit(relation);
        int expectedFailures = 0;

        Assert.assertEquals(expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "45000:1");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:45000");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:1,2");
        check.visit(relation);
        Assert.assertEquals(expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:1,(2)");
        check.visit(relation);
        Assert.assertEquals(expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:1,(20000)");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());
    }

    @Test
    public void testForBadRole() {
        Relation relation = createDefaultTestRelation();
        check.visit(relation);
        int expectedFailures = 0;

        Assert.assertEquals(expectedFailures, check.getErrors().size());

        for (int i = 0; i < relation.getMembers().size(); i++) {
            String tRole = replaceMember(relation, i, "badRole");
            check.visit(relation);
            Assert.assertEquals(++expectedFailures, check.getErrors().size());
            replaceMember(relation, i, tRole);
            check.visit(relation);
            Assert.assertEquals(expectedFailures, check.getErrors().size());
        }
    }

    private String replaceMember(Relation relation, int index, String replacementRole) {
        RelationMember relationMember = relation.getMember(index);
        String currentRole = relationMember.getRole();
        relation.removeMember(index);
        relation.addMember(index, new RelationMember(replacementRole, relationMember.getMember()));
        return currentRole;
    }
}
