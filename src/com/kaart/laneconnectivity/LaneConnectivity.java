package com.kaart.laneconnectivity;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

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
		AbstractAction laneConnectivityAction = new AbstractAction(NAME.concat(tr(" window")),
				ImageProvider.get("dialogs", PLUGIN_IMAGE, ImageProvider.ImageSizes.MENU)) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO make a window
			}
		};
		MainApplication.getMenu().dataMenu.add(laneConnectivityAction);
	}

	public static String getVersion() {
		// TODO get the version dynamically
		return "v0.0.1";
	}
}
