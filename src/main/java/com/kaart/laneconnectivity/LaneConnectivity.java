// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import com.kaart.laneconnectivity.gui.TurnLanesDialog;

/**
 *
 * @author Taylor Smock
 *
 */
public class LaneConnectivity extends Plugin {
	public static final String NAME = "Lane Connectivity";

	public static final String PLUGIN_IMAGE = "turnlanes"; // TODO get an image

	public LaneConnectivity(PluginInformation info) {
		super(info);
	}

	public static String getVersion() {
		// TODO get the version dynamically
		return "v0.0.1";
	}

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            // there was none before
            newFrame.addToggleDialog(new TurnLanesDialog());
        }
    }
}
