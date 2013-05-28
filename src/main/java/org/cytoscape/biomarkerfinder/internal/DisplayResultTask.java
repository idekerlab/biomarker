package org.cytoscape.biomarkerfinder.internal;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithm;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
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
	
	private static final String SEEDNUM = "seedNum";
	private static final String SCORE = "score";

	private final BiomarkerFinderAlgorithm task;
	private final CyNetwork original;
	
	private final CyNetworkViewManager viewManager;
	private final CyNetworkViewFactory viewFactory;

	private final VisualMappingManager vmm;
	private final VisualStyleFactory vsFactory;

	private final VisualMappingFunctionFactory continousMappingFactory;
	private final VisualMappingFunctionFactory passthroughMappingFactory;
	
	private final Map<String,Object> parameters;
	private final CyNetworkManager netmgr;
	private final CyRootNetworkManager rootmgr;
	
	
//	public DisplayResultTask(final BiomarkerFinderAlgorithm algorithm,final CyNetwork originalNetwork,
//			 final CyNetworkViewManager viewManager, final CyNetworkViewFactory viewFactory, 
//			 final VisualMappingManager vmm, final VisualStyleFactory vsFactory, final VisualMappingFunctionFactory continuousMappingFactory, final VisualMappingFunctionFactory passthroughMappingFactory) {
	public DisplayResultTask(final BiomarkerFinderAlgorithm algorithm,final CyNetwork originalNetwork,
			 final CyNetworkViewManager viewManager, final CyNetworkViewFactory viewFactory, 
			 final VisualMappingManager vmm, final VisualStyleFactory vsFactory, final VisualMappingFunctionFactory continuousMappingFactory, final VisualMappingFunctionFactory passthroughMappingFactory
			 ,final Map<String,Object> parameters, final CyNetworkManager netmgr, final CyRootNetworkManager rootmgr) {		
		this.task = algorithm;
		this.original = originalNetwork;
		
		this.viewManager = viewManager;
		this.viewFactory = viewFactory;
		this.vmm = vmm;
		this.vsFactory = vsFactory;
		this.continousMappingFactory = continuousMappingFactory;
		this.passthroughMappingFactory = passthroughMappingFactory;
		
		this.parameters = parameters;
		this.netmgr = netmgr;
		this.rootmgr = rootmgr;
		
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		final CyNetwork result = task.getResult();
		coloringNodes(original);
//		CreateSubNetwork(result);
		createSubNetworkView(result);
		coloringNodes(result);
		numberingCluster(result);

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

		Double val3 = getMaximum(network.getDefaultNodeTable().getColumn("score"))*4/5;
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
	
	private void numberingCluster(CyNetwork network){
		HashMap<CyNode,Integer> clusterMap = new HashMap<CyNode, Integer>();
		int i=1;
		Collection<CyNode> nodes = network.getNodeList();
		for(CyNode node:nodes){
			if(!clusterMap.containsKey(node)){
				int size = Integer.MAX_VALUE;
				Collection<CyNode> cluster = new HashSet<CyNode>();
				cluster.add(node);
				while(cluster.size()!=size){
					size = cluster.size();
					cluster.addAll(getCluster(network,cluster));
				}
				for(CyNode n:cluster){
					clusterMap.put(n, i);
				}
				i++;
			}
		}
		if(network.getDefaultNodeTable().getColumn("cluster #")==null){
			network.getDefaultNodeTable().createColumn("cluster #", Integer.class, false);			
		}
		for(CyNode n:clusterMap.keySet()){
			network.getDefaultNodeTable().getRow(n.getSUID()).set("cluster #", clusterMap.get(n));
		}
	}
	
	private Collection<CyNode> getCluster(CyNetwork network,Collection<CyNode> seeds){
		Collection<CyNode> ans = new HashSet<CyNode>();
		for(CyNode node:seeds){
			ans.addAll(network.getNeighborList(node, CyEdge.Type.ANY));
		}
		return ans;
	}
	

	private void createSubNetworkView(CyNetwork network){
		List<CyNode> resultNodeList = network.getNodeList();
		if(!resultNodeList.isEmpty()){
			
			List<CyNode> seedList = new ArrayList<CyNode>();
			Map<CyNode,Double> resultScoreList = new HashMap<CyNode, Double>();
			
			for(Iterator<CyNode> it = resultNodeList.iterator();it.hasNext();){
				CyNode tempNode = it.next();
				resultScoreList.put(tempNode, network.getDefaultNodeTable().getRow(tempNode.getSUID()).get(SCORE, Double.class));
			}
			
			// Sort resultScoreList with its score(Value, not key)
			Map<CyNode, Double> sortedScoreList = sortNodeScore(resultScoreList);
			
			// Take seed nodes, the highest nodes
			Iterator<CyNode> itrKey = sortedScoreList.keySet().iterator();
			int seedNum;
			if(parameters.containsKey(SEEDNUM)){// If the algorithm gives parameter "seedNum", take that number of seeds
				seedNum = (Integer)parameters.get(SEEDNUM);
			}
			else{// If the algorithm doesn't give the number of seeds, it takes the highest 20% of nodes in result network's nodes
				int nodesNum = resultNodeList.size();
				seedNum = (int) (nodesNum * 0.2);
			}
			for(int i=0; i<seedNum; i++){
				seedList.add(itrKey.next());
			}
			
			// Create Modules Viewer
			CyRootNetwork rootNetwork = rootmgr.getRootNetwork(network);
			CySubNetwork modulesNetwork = rootNetwork.addSubNetwork();
			modulesNetwork.getDefaultNetworkTable().getRow(modulesNetwork.getSUID()).set("name", "Modules Viewer");
			netmgr.addNetwork(modulesNetwork);
			Map<CyNode,CyNode> moduleNodesToResultNodes = new HashMap<CyNode, CyNode>();
			
			for(Iterator<CyNode> itrModule = seedList.iterator(); itrModule.hasNext();){// Create each seed's module
				CyNode seed = itrModule.next();
				CyNode newSeed = modulesNetwork.addNode();
				moduleNodesToResultNodes.put(newSeed, seed);
				String seedName = network.getDefaultNodeTable().getRow(seed.getSUID()).get("name", String.class);
				modulesNetwork.getDefaultNodeTable().getRow(newSeed.getSUID()).set("name", seedName);
				modulesNetwork.getDefaultNodeTable().getRow(newSeed.getSUID()).set(SCORE, resultScoreList.get(seed));
				
				List<CyNode> neighbors = network.getNeighborList(seed, CyEdge.Type.ANY);
				
				for(Iterator<CyNode> itrNeighbors = neighbors.iterator(); itrNeighbors.hasNext();){// Add neighbors to new module
					CyNode neighboringNode = itrNeighbors.next();
					CyNode newNeighboringNode = modulesNetwork.addNode();
					String neighborName = network.getDefaultNodeTable().getRow(neighboringNode.getSUID()).get("name", String.class);
					modulesNetwork.getDefaultNodeTable().getRow(newNeighboringNode.getSUID()).set("name", neighborName);
					modulesNetwork.getDefaultNodeTable().getRow(newNeighboringNode.getSUID()).set(SCORE, resultScoreList.get(neighboringNode));
					
					modulesNetwork.addEdge(newSeed, newNeighboringNode, false);
					
					moduleNodesToResultNodes.put(newNeighboringNode, neighboringNode);
				}
				
				// Add edges within module
				for(Iterator<CyNode> itrModuleNodes = modulesNetwork.getNeighborList(newSeed, CyEdge.Type.ANY).iterator();
						itrModuleNodes.hasNext();){
					CyNode tmpModuleNeighboringNode = itrModuleNodes.next();
					CyNode tmpResultNeighboringNode = moduleNodesToResultNodes.get(tmpModuleNeighboringNode);
					
					// Add edges between neighbors of module seeds
					for(Iterator<CyNode> itrModuleNodes2 = modulesNetwork.getNeighborList(newSeed, CyEdge.Type.ANY).iterator();
							itrModuleNodes2.hasNext();){
						CyNode tmpModuleNeighboringNode2 = itrModuleNodes2.next();
						CyNode tmpResultNeighboringNode2 = moduleNodesToResultNodes.get(tmpModuleNeighboringNode2);
						
						if(modulesNetwork.containsEdge(tmpModuleNeighboringNode, tmpModuleNeighboringNode2) ||
								modulesNetwork.containsEdge(tmpModuleNeighboringNode2, tmpModuleNeighboringNode)){// If edge between neighbors of the seed has been already added
						}
						else if(network.containsEdge(tmpResultNeighboringNode, tmpResultNeighboringNode2) ||
									network.containsEdge(tmpResultNeighboringNode2, tmpResultNeighboringNode)){// If the edge between neighbors of the seed exists in result network, the edge is added to module networks viewer as new one
								modulesNetwork.addEdge(tmpModuleNeighboringNode, tmpModuleNeighboringNode2, false);
						}											
					}
				}
			}
		}
		else{
			return;
		}
		
		
	}
	private Map<CyNode, Double> sortNodeScore(Map<CyNode, Double> nodeScores){
		ArrayList entries = new ArrayList(nodeScores.entrySet());
		Collections.sort(entries, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				Map.Entry e1 = (Entry) o1;
				Map.Entry e2 = (Entry) o2;
				Double s1 = (Double) e1.getValue();
				Double s2 = (Double) e2.getValue();
				return s1.compareTo(s2);
			}
		});
		return nodeScores;
	}
	

}
