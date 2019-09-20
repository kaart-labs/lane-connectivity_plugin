// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity;

import java.util.Arrays;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

public final class TestUtilsCustom {
    private TestUtilsCustom() {
        // Hide the constructor
    }

    /**
     * Add primitives to a dataset with their children
     *
     * @param dataSet    The dataset to add the primitives to
     * @param primitives The primitives to add to the dataset
     */
    public static void addPrimitivesToDataSet(DataSet dataSet, OsmPrimitive... primitives) {
        addPrimitivesToDataSet(dataSet, Arrays.asList(primitives));
    }

    /**
     * Add primitives to a dataset with their children
     *
     * @param dataSet    The dataset to add the primitives to
     * @param primitives The primitives to add to the dataset
     */
    public static void addPrimitivesToDataSet(DataSet dataSet, Collection<OsmPrimitive> primitives) {
        for (OsmPrimitive primitive : primitives) {
            if (primitive instanceof Relation) {
                addPrimitivesToDataSet(dataSet, ((Relation) primitive).getMemberPrimitives());
            } else if (primitive instanceof Way) {
                for (Node node : ((Way) primitive).getNodes()) {
                    if (!dataSet.containsNode(node))
                        dataSet.addPrimitive(node);
                }
            }
            dataSet.addPrimitive(primitive);
        }
    }
}
