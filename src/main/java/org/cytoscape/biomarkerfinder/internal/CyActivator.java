package org.cytoscape.biomarkerfinder.internal;

import java.util.Properties;

import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.biomarkerfinder.internal.ui.ControlPanel;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;


public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext bc) {
		CySwingApplication cytoscapeDesktopService = getService(bc,CySwingApplication.class);
		ControlPanel bioPanel = new ControlPanel(bc);
		MenuAction action = new MenuAction(cytoscapeDesktopService,bioPanel,bc);
		
		registerService(bc,bioPanel,CytoPanelComponent.class, new Properties());
		registerService(bc,action,CyAction.class, new Properties());
	}

}
