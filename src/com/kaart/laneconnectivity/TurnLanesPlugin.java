// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import com.kaart.laneconnectivity.gui.TurnLanesDialog;

public class TurnLanesPlugin extends Plugin {
    public TurnLanesPlugin(PluginInformation info) {
        super(info);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            // there was none before
            newFrame.addToggleDialog(new TurnLanesDialog());
        }
    }
}
