// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity;

import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import com.kaart.laneconnectivity.gui.TurnLanesDialog;
import com.kaart.laneconnectivity.validation.ConnectivityRelationCheck;

/**
 *
 * @author Taylor Smock
 *
 */
public class LaneConnectivity extends Plugin {
    public static final String NAME = "Lane Connectivity";

    private static PluginInformation info;

    public static final String PLUGIN_IMAGE = "turnlanes"; // TODO get an image

    public LaneConnectivity(PluginInformation info) {
        super(info);
        setInformation(info);
        OsmValidator.addTest(ConnectivityRelationCheck.class);
    }

    /**
     * Get the version number of the plugin
     *
     * @return The version number of the plugin
     */
    public static String getVersion() {
        if (info == null) {
            return "";
        }
        return info.localversion;
    }

    private static synchronized void setInformation(PluginInformation tInfo) {
        info = tInfo;
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            // there was none before
            newFrame.addToggleDialog(new TurnLanesDialog());
        }
    }
}
