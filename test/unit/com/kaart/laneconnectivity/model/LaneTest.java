package com.kaart.laneconnectivity.model;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

import org.junit.Assert;

public class LaneTest {
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
    public void testGetRegularCount() {
        Way way = TestUtils.newWay("highway=residential lanes=2", new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)));
        Assert.assertEquals(1, Lane.getRegularCount(way, way.firstNode()));
        Assert.assertEquals(1, Lane.getRegularCount(way, way.lastNode()));
        
        way.put("lanes", "3");
        Assert.assertEquals(1, Lane.getRegularCount(way, way.firstNode()));
        Assert.assertEquals(2, Lane.getRegularCount(way, way.lastNode()));
        
        way.put("lanes:forward", "1");
        Assert.assertEquals(2, Lane.getRegularCount(way, way.firstNode()));
        Assert.assertEquals(1, Lane.getRegularCount(way, way.lastNode()));
        way.remove("lanes:forward");
        
        way.put("lanes:backward", "1");
        Assert.assertEquals(1, Lane.getRegularCount(way, way.firstNode()));
        Assert.assertEquals(2, Lane.getRegularCount(way, way.lastNode()));
        way.remove("lanes.backward");
        
        way.put("oneway", "yes");
        Assert.assertEquals(3, Lane.getRegularCount(way, way.lastNode()));
        Assert.assertEquals(0, Lane.getRegularCount(way, way.firstNode()));
        
        way.put("oneway", "-1");
        Assert.assertEquals(0, Lane.getRegularCount(way, way.lastNode()));
        Assert.assertEquals(3, Lane.getRegularCount(way, way.firstNode()));
    }
}
