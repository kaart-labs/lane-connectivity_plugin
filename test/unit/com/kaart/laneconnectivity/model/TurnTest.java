package com.kaart.laneconnectivity.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import com.kaart.laneconnectivity.TestUtilsCustom;

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

        /* While we should discourage whitespace, enough people do it that we need to handle it */
        relation.put("connectivity", "3:1, (2) | 4 : 2,(3)");
        indices = Turn.indices(relation, "connectivity");
        Assert.assertEquals(2, indices.size());

        lane1 = indices.get(3);
        lane2 = indices.get(4);
        Assert.assertEquals(2, lane1.size());
        Assert.assertEquals(2, lane2.size());

        Assert.assertFalse(lane1.get(1));
        Assert.assertTrue(lane1.get(2));
        Assert.assertFalse(lane2.get(2));
        Assert.assertTrue(lane2.get(3));
    }

    @Test
    public void testLoadViaNode() {
        DataSet dataSet = new DataSet();
        Way way1 = TestUtils.newWay("highway=residential lanes=2", new Node(new LatLon(0, 0)),
                new Node(new LatLon(0.1, 0.1)));
        Way way2 = TestUtils.newWay("highway=residential lanes=2", new Node(new LatLon(-0.2, 0.1)),
                way1.firstNode());
        Relation relation = TestUtils.newRelation("type=connectivity connectivity=1:1",
                new RelationMember("from", way2), new RelationMember("via", way1.firstNode()),
                new RelationMember("to", way1));
        TestUtilsCustom.addPrimitivesToDataSet(dataSet, relation);
        ModelContainer container = ModelContainer.create(Collections.singleton(way1.firstNode()),
                Collections.emptyList());
        Set<Turn> turns = Turn.load(container, relation);
        Assert.assertEquals(1, turns.size());

        relation.put("connectivity", "1:1|2:2");
        way1.put("oneway", "yes");
        way2.put("oneway", "yes");
        container = container.recalculate();
        turns = Turn.load(container, relation);
        Assert.assertEquals(2, turns.size());
    }

    @Test
    public void testLoadViaWays() {

        DataSet dataSet = new DataSet();
        Way way1 = TestUtils.newWay("highway=residential lanes=2", new Node(new LatLon(0, 0)),
                new Node(new LatLon(0.1, 0.1)));
        Way way2 = TestUtils.newWay("highway=residential lanes=2", new Node(new LatLon(-0.2, 0.1)), way1.firstNode());
        Way way3 = TestUtils.newWay("highway=residential lanes=2", new Node(new LatLon(0.2, 0.2)), way1.lastNode());
        Relation relation = TestUtils.newRelation("type=connectivity connectivity=1:1",
                new RelationMember("from", way2), new RelationMember("via", way1), new RelationMember("to", way3));
        TestUtilsCustom.addPrimitivesToDataSet(dataSet, relation);
        ModelContainer container = ModelContainer.create(way1.getNodes(), Collections.singleton(way1));
        Set<Turn> turns = Turn.load(container, relation);
        Assert.assertEquals(1, turns.size());
    }
}
