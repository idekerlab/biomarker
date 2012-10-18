package org.cytoscape.biomarkerfinder.internal;

import java.awt.Color;
import java.awt.Paint;
import java.util.Iterator;
import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.internal.CyServiceRegistrarImpl;
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
import org.osgi.framework.BundleContext;

public class DisplayResult {
	BundleContext bc;
	CyNetwork network;
	public DisplayResult(BundleContext bc,CyNetwork network){
		this.bc=bc;
		this.network =network;
		display();
	}
	private void display(){
		
		CyServiceRegistrarImpl register = new CyServiceRegistrarImpl(bc);
		
		CyNetworkViewManager viewManager = register.getService(CyNetworkViewManager.class);
		CyNetworkViewFactory viewFactory = register.getService(CyNetworkViewFactory.class);
		CyNetworkView networkView = viewFactory.createNetworkView(network);
		viewManager.addNetworkView(networkView);
		
		VisualMappingFunctionFactory passthroughFactory = register.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
		VisualMappingFunctionFactory continuousFactory = register.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
		VisualMappingManager vmManager = register.getService(VisualMappingManager.class);
		VisualStyleFactory vsFactory = register.getService(VisualStyleFactory.class);
		VisualStyle vs = vsFactory.createVisualStyle(network.getDefaultNetworkTable().getRow(network.getSUID()).get("name", String.class));

		
		
		PassthroughMapping pMapping = (PassthroughMapping) passthroughFactory.createVisualMappingFunction("name", String.class,BasicVisualLexicon.NODE_LABEL);
		
		vs.addVisualMappingFunction(pMapping);
		vs.apply(networkView);
		
		
		ContinuousMapping mapping = (ContinuousMapping) continuousFactory.createVisualMappingFunction("score", Double.class, BasicVisualLexicon.NODE_FILL_COLOR);
		
		Double val1 = getMinimum(network.getDefaultNodeTable().getColumn("score"));
		BoundaryRangeValues<Paint> brv1 = new BoundaryRangeValues<Paint>(Color.GREEN,Color.GREEN,Color.GREEN);
		
		Double val3 = getMaximum(network.getDefaultNodeTable().getColumn("score"));
		BoundaryRangeValues<Paint> brv3 = new BoundaryRangeValues<Paint>(Color.RED,Color.RED,Color.RED);
		
		Double val2 = (val1+val3+val1+val1)/4;
		BoundaryRangeValues<Paint> brv2 = new BoundaryRangeValues<Paint>(Color.YELLOW,Color.YELLOW,Color.YELLOW);
		
		mapping.addPoint(val1, brv1);
		mapping.addPoint(val2, brv2);
		mapping.addPoint(val3, brv3);
		
		vs.addVisualMappingFunction(mapping);
		vs.apply(networkView);

		vmManager.addVisualStyle(vs);
		vmManager.setVisualStyle(vs, networkView);
		networkView.updateView();
		
		
	}
	
	private Double getMinimum(CyColumn c){
		Double x=Double.MAX_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while(it.hasNext()){
			Double y = it.next();
			if(y < x){
				x=y;
			}
		}
		return x;
	}
	
	private Double getMaximum(CyColumn c){
		double x=Double.MIN_VALUE;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while(it.hasNext()){
			Double y = it.next();
			if(y > x){
				x=y;
			}
		}		
		return x;
	}
	
	private Double getMean(CyColumn c){
		Double x=5d;
		List<Double> values = c.getValues(Double.class);
		Iterator<Double> it = values.iterator();
		while(it.hasNext()){
			Double y = it.next();
				x += y;
		}
		x = x / values.size();
		return x;
	}

}
