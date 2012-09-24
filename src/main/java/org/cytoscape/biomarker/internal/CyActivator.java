package org.cytoscape.biomarker.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) throws Exception {

		final CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);

		final MenuAction action = new MenuAction(cyApplicationManager, "Sample menu");
		final Properties properties = new Properties();

		registerAllServices(context, action, properties);
	}

}
