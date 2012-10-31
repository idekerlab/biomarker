package org.cytoscape.biomarkerfinder.internal;

import java.awt.Color;
import java.awt.Paint;
import java.util.Iterator;
import java.util.List;

import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithm;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class DisplayResultTask extends AbstractTask {

	private final BiomarkerFinderAlgorithm task;
	private final CyNetwork original;
	
	private final CyNetworkViewManager viewManager;
	private final CyNetworkViewFactory viewFactory;

	private final VisualMappingManager vmm;
	private final VisualStyleFactory vsFactory;

	private final VisualMappingFunctionFactory continousMappingFactory;
	private final VisualMappingFunctionFactory passthroughMappingFactory;
	
	public DisplayResultTask(final BiomarkerFinderAlgorithm algorithm,final CyNetwork originalNetwork,
			 final CyNetworkViewManager viewManager, final CyNetworkViewFactory viewFactory, 
			 final VisualMappingManager vmm, final VisualStyleFactory vsFactory, final VisualMappingFunctionFactory continuousMappingFactory, final VisualMappingFunctionFactory passthroughMappingFactory) {
		
		this.task = algorithm;
		this.original = originalNetwork;
		
		this.viewManager = viewManager;
		this.viewFactory = viewFactory;
		this.vmm = vmm;
		this.vsFactory = vsFactory;
		this.continousMappingFactory = continuousMappingFactory;
		this.passthroughMappingFactory = passthroughMappingFactory;
		
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		final CyNetwork result = task.getResult();
		coloringNodes(original);
		coloringNodes(result);

	}
	
	private void coloringNodes(final CyNetwork network) {

		final CyNetworkView networkView;
		
		if(viewManager.viewExists(network)){
			networkView = viewManager.getNetworkViews(network).iterator().next();
		}
		else{
			networkView = viewFactory.createNetworkView(network);
			viewManager.addNetworkView(networkView);
		}

		final VisualStyle vs = vsFactory.createVisualStyle(network.getDefaultNetworkTable().getRow(network.getSUID())
				.get(CyNetwork.NAME, String.class));

		PassthroughMapping pMapping = (PassthroughMapping) passthroughMappingFactory.createVisualMappingFunction(
				CyNetwork.NAME, String.class, BasicVisualLexicon.NODE_LABEL);

		vs.addVisualMappingFunction(pMapping);
		vs.apply(networkView);

		ContinuousMapping mapping = (ContinuousMapping) continousMappingFactory.createVisualMappingFunction("score",
				Double.class, BasicVisualLexicon.NODE_FILL_COLOR);

		Double val1 = getMinimum(network.getDefaultNodeTable().getColumn("score"));
		BoundaryRangeValues<Paint> brv1 = new BoundaryRangeValues<Paint>(Color.GREEN, Color.GREEN, Color.GREEN);

		Double val3 = getMaximum(network.getDefaultNodeTable().getColumn("score"));
		BoundaryRangeValues<Paint> brv3 = new BoundaryRangeValues<Paint>(Color.RED, Color.RED, Color.RED);

		Double val2 = (val1 + val3 + val1 + val1) / 10;
		BoundaryRangeValues<Paint> brv2 = new BoundaryRangeValues<Paint>(Color.YELLOW, Color.YELLOW, Color.YELLOW);

		mapping.addPoint(val1, brv1);
		mapping.addPoint(val2, brv2);
		mapping.addPoint(val3, brv3);

		vs.addVisualMappingFunction(mapping);
		vs.apply(networkView);

		vmm.addVisualStyle(vs);
		vmm.setVisualStyle(vs, networkView);
		networkView.updateView();
	}

	private Double getMinimum(CyColumn c) {
		Double x = Double.MAX_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while (it.hasNext()) {
			Double y = it.next();
			if (y < x) {
				x = y;
			}
		}
		return x;
	}

	private Double getMaximum(CyColumn c) {
		double x = Double.MIN_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while (it.hasNext()) {
			Double y = it.next();
			if (y > x) {
				x = y;
			}
		}
		return x;
	}

}
