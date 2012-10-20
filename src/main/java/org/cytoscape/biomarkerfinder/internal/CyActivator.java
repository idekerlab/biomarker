package org.cytoscape.biomarkerfinder.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactory;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactoryManager;
import org.cytoscape.biomarkerfinder.internal.algorithm.NetworkPropagationTaskFactory;
import org.cytoscape.biomarkerfinder.internal.ui.ControlPanel;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext bc) {

		// Import services
		final CySwingApplication cytoscapeDesktopService = getService(bc, CySwingApplication.class);
		final DialogTaskManager taskManager = getService(bc, DialogTaskManager.class);
		final CyNetworkManager netmgr = getService(bc, CyNetworkManager.class);
		final CyNetworkViewManager viewManager = getService(bc, CyNetworkViewManager.class);
		final CyNetworkViewFactory viewFactory = getService(bc, CyNetworkViewFactory.class);
		final CyRootNetworkManager rootManager = getService(bc, CyRootNetworkManager.class);

		final VisualMappingFunctionFactory continousMappingFactory = getService(bc, VisualMappingFunctionFactory.class,
				"(mapping.type=continuous)");
		
		final VisualMappingFunctionFactory passthroughMappingFactory = getService(bc, VisualMappingFunctionFactory.class,
				"(mapping.type=passthrough)");
		
		final CyApplicationManager applicationManager = getService(bc, CyApplicationManager.class);
		
		final CyEventHelper eventHelper = getService(bc, CyEventHelper.class);

		// Create instances
		// Manager object for Algorithms
		final BiomarkerFinderAlgorithmFactoryManager algorithmManager = new BiomarkerFinderAlgorithmFactoryManagerImpl(eventHelper);

		final NetworkPropagationTaskFactory networkPropagationTaskFactory = new NetworkPropagationTaskFactory(
				rootManager, netmgr);

		final ControlPanel bioPanel = new ControlPanel(taskManager, netmgr, algorithmManager, applicationManager);
		final MenuAction action = new MenuAction(cytoscapeDesktopService, bioPanel, bc);

		// Export OSGi Services

		// Export algorithm factory
		
		registerAllServices(bc, bioPanel, new Properties());
		
		registerAllServices(bc, algorithmManager, new Properties());
		registerServiceListener(bc, algorithmManager, "registerFactory", "unregisterFactory",
				BiomarkerFinderAlgorithmFactory.class);
		registerAllServices(bc, networkPropagationTaskFactory, new Properties());
		
		
		registerService(bc, action, CyAction.class, new Properties());
		
	}
}