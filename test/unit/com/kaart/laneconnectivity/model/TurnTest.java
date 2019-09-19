package com.kaart.laneconnectivity.model;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;

public class TurnTest {
    /**
     * Setup test.
     * 
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testIndices() {
        Relation relation = TestUtils.newRelation("connectivity=1:1|2:2,(3)",
                new RelationMember("via", new Node(new LatLon(0, 0))));
        Map<Integer, Map<Integer, Boolean>> indices = Turn.indices(relation, "connectivity");
        Assert.assertEquals(2, indices.size());

        Map<Integer, Boolean> lane1 = indices.get(1);
        Map<Integer, Boolean> lane2 = indices.get(2);
        Assert.assertEquals(1, lane1.size());
        Assert.assertEquals(2, lane2.size());

        Assert.assertFalse(lane1.get(1));
        Assert.assertFalse(lane2.get(2));
        Assert.assertTrue(lane2.get(3));
    }

}
