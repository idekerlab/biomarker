package org.cytoscape.biomarkerfinder.internal.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithm;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.TaskMonitor;

public class NetworkPropagationAlgorithm extends AbstractNetworkTask implements BiomarkerFinderAlgorithm {

	// Algorithm name & ID
	static final String ALGORITHM_ID = "network-propagation";
	static final String ALGORITHM_NAME = "Network Propagation";

	// Parameter names
	private static final String ALPHA = "alpha";
	private static final String THRESHOLD = "threshold";
	private static final String SEEDNUM = "seedNum";
	private static final String MODULESIZE = "moduleSize";

	private double[] currentScore;
	private double[] originalScore;
	private double[][] originalW;
	private double[][] normalizedW;

	private double alpha;
	private double threshold;
	private int seedNum;
	private int moduleSize;
	
	private final CyNetwork result;
	private final List<CyNode> seedList = new LinkedList<CyNode>();
	private final Set<CyNode> nodeList = new HashSet<CyNode>();
	private final Set<CyEdge> edgeList = new HashSet<CyEdge>();
	
	private final Map<CyNode, Integer> nodeHash = new HashMap<CyNode, Integer>();
	private final Map<Integer, CyNode> HashNode = new HashMap<Integer, CyNode>();
	private final Map<CyNode,CyNode> originalResultNode = new HashMap<CyNode, CyNode>();

	private final CyRootNetworkManager rootManager;
	private final CyNetworkManager networkManager;
	
	

	public NetworkPropagationAlgorithm(final CyNetwork network, final CyRootNetworkManager rootManager,
			final CyNetworkManager networkManager) {
		super(network);
		
		this.rootManager = rootManager;
		this.networkManager = networkManager;
		
		this.result = rootManager.getRootNetwork(network).addSubNetwork();
		String resultName = network.getDefaultNetworkTable().getRow(network.getSUID()).get("name", String.class)
		+ "_result";
		result.getDefaultNetworkTable().getRow(result.getSUID()).set("name", resultName);
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setProgress(0d);
		double diff = Double.MAX_VALUE;
		setNodesHash(network);
		setScore(network);
		taskMonitor.setProgress(0.2);
		setWeight(network);
		taskMonitor.setProgress(0.4);
		normalizedW = normalize(originalW);

		while (diff > threshold) {
			double[] nextScore = propagate();

			RealMatrix Fn = MatrixUtils.createColumnRealMatrix(nextScore);
			RealMatrix Fc = MatrixUtils.createColumnRealMatrix(currentScore);
			diff = Fn.subtract(Fc).getFrobeniusNorm();
			currentScore = nextScore;
		}
	}

	@Override
	public void setParameter(final Map<String, Object> parameters) {
		alpha = (Double) parameters.get(ALPHA);
		threshold = (Double) parameters.get(THRESHOLD);
		seedNum = (Integer) parameters.get(SEEDNUM);
		moduleSize = (Integer) parameters.get(MODULESIZE);
	}

	@Override
	public CyNetwork getResult() {
		extractModules();
		networkManager.addNetwork(result);
		return result;
	}

	private double[] propagate() {
		RealMatrix Wn = MatrixUtils.createRealMatrix(normalizedW);
		RealMatrix Fm = MatrixUtils.createColumnRealMatrix(currentScore);
		RealMatrix pm = MatrixUtils.createColumnRealMatrix(originalScore);

		RealMatrix Fnext = ((Wn.multiply(Fm)).scalarMultiply(alpha)).add(pm.scalarMultiply(1 - alpha));
		double x[] = Fnext.getColumn(0);
		return x;
	}

	private double[][] normalize(double[][] originalW) {
		try{
			RealMatrix W = MatrixUtils.createRealMatrix(originalW);
			double[][] x = new double[W.getRowDimension()][W.getColumnDimension()];
			for (int i = 0; i < W.getRowDimension(); i++) {
				x[i][i] = Math.sqrt(1 / sumRow(W.getRow(i)));
			}
			RealMatrix xn = MatrixUtils.createRealMatrix(x);
			RealMatrix Wn = (xn.multiply(W)).multiply(xn);
			double[][] ans = Wn.getData();
			return ans;
		}
		catch(OutOfMemoryError e){
			JOptionPane.showMessageDialog(null, "Out of Memory error. Too much nodes.");
			return null;
		}
	}

	private double sumRow(double[] row) {
		double x = 0;
		for (int i = 0; i < row.length; i++) {
			x += row[i];
		}
		return x;
	}

	private void setNodesHash(CyNetwork network) {
		List<CyNode> nodeList = network.getNodeList();
		for (int i = 0; i < nodeList.size(); i++) {
			nodeHash.put(nodeList.get(i), i);
			HashNode.put(i, nodeList.get(i));
		}
	}

	private void setScore(CyNetwork network) {
		CyTable nodeTable = network.getDefaultNodeTable();
		originalScore = new double[nodeHash.size()];
		for (int i = 0; i < nodeHash.size(); i++) {
			originalScore[i] = Double.valueOf(nodeTable.getRow(HashNode.get(i).getSUID()).get("score", Double.class))
					.doubleValue();
		}
		currentScore = originalScore;
	}

	private void setWeight(CyNetwork network) {
		originalW = new double[network.getDefaultNodeTable().getRowCount()][network.getDefaultNodeTable().getRowCount()];
		CyTable edgeTable = network.getDefaultEdgeTable();
		List<CyEdge> edgeList = network.getEdgeList();
		try{
			for (int i = 0; i < edgeList.size(); i++) {
				CyNode src = edgeList.get(i).getSource();
				CyNode trg = edgeList.get(i).getTarget();
				int indexSrc = nodeHash.get(src);
				int indexTrg = nodeHash.get(trg);
				double w = Double.valueOf(edgeTable.getRow(edgeList.get(i).getSUID()).get("weight", Double.class))
						.doubleValue();
				originalW[indexSrc][indexTrg] = w;
				originalW[indexTrg][indexSrc] = w;
			}
		}
		catch(OutOfMemoryError e){
			JOptionPane.showMessageDialog(null, "Out of Memory error. Too much nodes");
		}
	}

	@Override
	public String getDisplayName() {
		return ALGORITHM_NAME;
	}

	@Override
	public String getAlgorithmID() {
		return ALGORITHM_ID;
	}
	
	
	@SuppressWarnings("unchecked")
	private void extractModules(){ 
		HashMap<CyNode, Double> scoreList = new HashMap<CyNode, Double>();
		for(CyNode node:network.getNodeList()){
			scoreList.put(node, currentScore[nodeHash.get(node)]);
		}
		
		ArrayList entries = new ArrayList(scoreList.entrySet());
		//sort scoreList in descending order
		Collections.sort(entries,new Comparator() {

			@Override
			public int compare(Object o1, Object o2) {
				Entry entry1 = (Entry) o1;
				Entry entry2 = (Entry) o2;
				Double score1 = (Double) entry1.getValue();
				Double score2 = (Double) entry2.getValue();
				return score2.compareTo(score1);
			}
		});
		
		// Creating seedList and add their neighbors to nodeList of result network
		if(seedNum<entries.size()){
			Iterator it = entries.iterator();
			for(int i=0;i<seedNum;i++){
				Entry entry = (Entry) it.next();
				seedList.add((CyNode)entry.getKey());
				getNeighbors((CyNode)entry.getKey());
			}
		}
		
		addNodes2Result();
		addEdges2Result();

		numberingCluster();

	}
	
	@SuppressWarnings("unchecked")
	private void getNeighbors(CyNode seed){
		Collection<CyNode> nodes = network.getNeighborList(seed, CyEdge.Type.ANY);		
		nodes.add(seed);
		HashMap<CyNode,Double> scoreList = new HashMap<CyNode, Double>();
		
		Iterator<CyNode> it = nodes.iterator();
		while(it.hasNext()){
			CyNode node = it.next();
			scoreList.put(node, currentScore[nodeHash.get(node)]);
		}
		
		ArrayList entries = new ArrayList(scoreList.entrySet());
		//sort scoreList of neighbors in the module in descending order
		Collections.sort(entries,new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				Entry entry1 = (Entry) o1;
				Entry entry2 = (Entry) o2;
				Double score1 = (Double) entry1.getValue();
				Double score2 = (Double) entry2.getValue();
				return score2.compareTo(score1);
			}
		});

		// Extracting the highest score nodes neighboring to the seed 
		Iterator<Entry> itr = entries.iterator();
		Collection<CyNode> moduleNodes = new HashSet<CyNode>();
		Collection<CyEdge> moduleEdges = new HashSet<CyEdge>();
		
		for(int i=0; i<moduleSize;i++){
			if(itr.hasNext()){
				Entry entry = itr.next();
				moduleNodes.add((CyNode)entry.getKey());
			}
			else{
				break;
			}
		}
		
		// Add edges in the module
		for(CyNode src:moduleNodes){
			for(CyNode trg:moduleNodes){
				moduleEdges.addAll(network.getConnectingEdgeList(src, trg, CyEdge.Type.ANY));
			}
		}
		
		// Refine the module
		Collection<CyEdge> refinedEdges = refineModule(moduleEdges);
		Collection<CyNode> refinedNodes = new HashSet<CyNode>();
		for(CyEdge e:refinedEdges){
			refinedNodes.add(e.getSource());
			refinedNodes.add(e.getTarget());
		}
		
		// Filtering the module with less than 5 nodes or with >50% of overlap with seed
		int size = refinedNodes.size();
		if(size<5||countingNumOfSeedInModule(refinedNodes) > (size/2)){
			nodeList.addAll(refinedNodes);
			edgeList.addAll(refinedEdges);
		}
		
	}
	
	private void addNodes2Result(){
		Iterator<CyNode> it = nodeList.iterator();
		while(it.hasNext()){
			CyNode originalNode = it.next();
			CyNode newNode = result.addNode();
			originalResultNode.put(originalNode, newNode);
			
			String name = network.getDefaultNodeTable().getRow(originalNode.getSUID()).get("name", String.class);
			Double score = currentScore[nodeHash.get(originalNode)]; 
			result.getDefaultNodeTable().getRow(newNode.getSUID()).set("name",name);
			result.getDefaultNodeTable().getRow(newNode.getSUID()).set("score",score);
		}
	}
	
	private void addEdges2Result(){
		Iterator<CyEdge> it = edgeList.iterator();
		while(it.hasNext()){
			CyEdge originalEdge = it.next();
			CyNode src = originalResultNode.get(originalEdge.getSource());
			CyNode trg = originalResultNode.get(originalEdge.getTarget());
			CyEdge newEdge = result.addEdge(src, trg, false);
			
			Double weight = network.getDefaultEdgeTable().getRow(originalEdge.getSUID()).get("weight", Double.class);
			result.getDefaultEdgeTable().getRow(newEdge.getSUID()).set("weight", weight);
		}
	}	
	
	private int countingNumOfSeedInModule(Collection<CyNode> nodes){
		int num=0;
		for(CyNode node:nodes){
			if(seedList.contains(node)){
				num++;
			}
		}
		return num;
	}
	
	private void numberingCluster(){
		HashMap<CyNode,Integer> clusterMap = new HashMap<CyNode, Integer>();
		int i=1;
		Collection<CyNode> nodes = result.getNodeList();
		for(CyNode node:nodes){
			if(!clusterMap.containsKey(node)){
				int size = Integer.MAX_VALUE;
				Collection<CyNode> cluster = new HashSet<CyNode>();
				cluster.add(node);
				while(cluster.size()!=size){
					size = cluster.size();
					cluster.addAll(getCluster(cluster));
				}
				for(CyNode n:cluster){
					clusterMap.put(n, i);
				}
				i++;
			}
		}
		if(result.getDefaultNodeTable().getColumn("cluster #")==null){
			result.getDefaultNodeTable().createColumn("cluster #", Integer.class, false);			
		}
		for(CyNode n:clusterMap.keySet()){
			result.getDefaultNodeTable().getRow(n.getSUID()).set("cluster #", clusterMap.get(n));
		}
	}
	
	private Collection<CyNode> getCluster(Collection<CyNode> seeds){
		Collection<CyNode> ans = new HashSet<CyNode>();
		for(CyNode node:seeds){
			ans.addAll(result.getNeighborList(node, CyEdge.Type.ANY));
		}
		return ans;
	}
		
	private Collection<CyEdge> refineModule(Collection<CyEdge> moduleEdges){
		CyNode[][] edgeList = new CyNode[moduleEdges.size()][2];
		int i=0;
		for(CyEdge e:moduleEdges){
			edgeList[i][0] = e.getSource();
			edgeList[i][1] = e.getTarget();
			i++;
		}
		
		double score =0d;
		double currentScore = Double.MAX_VALUE;
		CyNode[][] tmp;
		tmp = edgeList.clone();
		while( (currentScore = calculateComplexScore(tmp)) >score){
			tmp = Maximize(tmp);
			score = calculateComplexScore(tmp);
		}
		
		Collection<CyEdge> edges = new HashSet<CyEdge>();
		for(int j=0;j<tmp.length;j++){
			CyNode src = tmp[j][0];
			CyNode trg = tmp[j][1];
			for(CyEdge e:moduleEdges){
				if(e.getSource().equals(src)&&e.getTarget().equals(trg)){
					edges.add(e);
				}
			}
		}
		return edges;
	}
	
	private double prob(CyNode[][] edgeList, CyNode src, CyNode trg) {
		double x = 0;
			x = (double)(countSrcNode(edgeList, src)*countTrgNode(edgeList, trg))/Math.pow(edgeList.length, 2);
		return x;
	}
	
	private int countSrcNode(CyNode[][] edgeList, CyNode src){
		int x = 0;
		for(int i = 0; i < edgeList.length;i++){
			if(edgeList[i][0].equals(src))
				x++;
		}
		return x;
	}
	
	private int countTrgNode(CyNode[][] edgeList, CyNode trg){
		int x = 0;
		for(int i = 0; i < edgeList.length;i++){
			if(edgeList[i][1].equals(trg))
				x++;
		}
		return x;
	}

	private int numPerfectEdge(int numNodes){
		return ( numNodes*(numNodes-1) )/2;
	}
	
	private CyNode[][] createUnexsistEdgeList(CyNode[][] edgeList,Collection<CyNode> nodeList){
		
		int numEdges = edgeList.length;
		int numPerfectEdges = numPerfectEdge(nodeList.size());
		CyNode[][] list = new CyNode[numPerfectEdges-numEdges][2];
		
		int h = 0;
			for(int i=0;i<nodeList.size();i++){
				for(int j=i+1;j<nodeList.size();j++){
					if(!hasEdge(edgeList,(CyNode)nodeList.toArray()[i], (CyNode)nodeList.toArray()[j])){
						list[h][0]=(CyNode)nodeList.toArray()[i];
						list[h][1]=(CyNode)nodeList.toArray()[j];
						h++;
					}
				}
			}
		return list;
	}
	
	private boolean hasEdge(CyNode[][] edgeList, CyNode src, CyNode trg){
		for(int i = 0; i<edgeList.length;i++){
			if(src.equals(edgeList[i][0])){
				if(trg.equals(edgeList[i][1])){
					return true;
				}
			}
			if(trg.equals(edgeList[i][0])){
				if(src.equals(edgeList[i][1])){
					return true;
				}
			}
		}
		return false;
	}
	
	private double calculateComplexScore(CyNode[][] edgeList){
		HashSet<CyNode> nodeList = new HashSet<CyNode>();
		for(int i=0;i<edgeList.length;i++){
			nodeList.add(edgeList[i][0]);
			nodeList.add(edgeList[i][1]);
		}
		
		double score = 0;
		for(int i=0;i<edgeList.length;i++){
			score = score + Math.log(0.9/prob(edgeList,edgeList[i][0],edgeList[i][1]));
		}
		CyNode[][] unexsistEdgeList =createUnexsistEdgeList(edgeList,nodeList);
		for(int i=0;i<unexsistEdgeList.length;i++){
			score = score + Math.log(0.1/(1-prob(unexsistEdgeList,unexsistEdgeList[i][0],unexsistEdgeList[i][1])));
		}
		return score;
	}
	
	private CyNode[][] removeNode(CyNode[][] edgeList,CyNode rm){
		ArrayList<CyNode> srcList = new ArrayList<CyNode>();
		ArrayList<CyNode> trgList = new ArrayList<CyNode>();
		for(int i=0;i<edgeList.length;i++){
			srcList.add(edgeList[i][0]);
			trgList.add(edgeList[i][1]);
		}
		
		while(srcList.contains(rm)){
			srcList.remove(rm);
		}
		while(trgList.contains(rm)){
			trgList.remove(rm);
		}
		int len = srcList.size()+trgList.size()-edgeList.length;
		CyNode[][] ans = new CyNode[len][2];
		int i=0;
		for(int j=0;j<edgeList.length;j++){
			if(edgeList[j][0].equals(rm)||edgeList[j][1].equals(rm)){
				
			}
			else{
				ans[i][0] = edgeList[j][0];
				ans[i][1] = edgeList[j][1];
				i++;
			}
		}
		return ans;
	}
	
	private CyNode[][] Maximize(CyNode[][] edgeList){
		
		HashSet<CyNode> nodes = new HashSet<CyNode>();
		for(int i=0;i<edgeList.length;i++){
			nodes.add(edgeList[i][0]);
			nodes.add(edgeList[i][1]);
		}
		nodes.remove("A");
		
		double score = calculateComplexScore(edgeList);
		double cur = 0d;
		CyNode r=null;
		for(CyNode rm:nodes){
			if((cur=calculateComplexScore(removeNode(edgeList, rm)))>score & !Double.isInfinite(cur)){
				score=cur;
				r=rm;
			}
		}
		if(r!=null){
			return removeNode(edgeList, r);
		}
		else{
			return edgeList;
		}
	}
	
}
