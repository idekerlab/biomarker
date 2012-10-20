package org.cytoscape.biomarkerfinder.internal;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.biomarkerfinder.internal.ui.ControlPanel;
import org.osgi.framework.BundleContext;

public class MenuAction extends AbstractCyAction {
	
	private CySwingApplication desktopApp;
	private final CytoPanel cytoPanelWest;
	private ControlPanel myCytoPanel;

	public MenuAction(CySwingApplication desktopApp, ControlPanel myCytoPanel, BundleContext bc) {
		// Add a menu item -- Apps->sample02
		super("BioMarker");
		setPreferredMenu("Apps");

		this.desktopApp = desktopApp;

		// Note: myCytoPanel is bean we defined and registered as a service
		this.cytoPanelWest = this.desktopApp.getCytoPanel(CytoPanelName.WEST);
		this.myCytoPanel = myCytoPanel;

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// If the state of the cytoPanelWest is HIDE, show it
		if (cytoPanelWest.getState() == CytoPanelState.HIDE) {
			cytoPanelWest.setState(CytoPanelState.DOCK);
		}

		// Select my panel
		int index = cytoPanelWest.indexOfComponent(myCytoPanel);
		if (index == -1) {
			return;
		}
		cytoPanelWest.setSelectedIndex(index);
	}
}
