package org.cytoscape.biomarker.internal.algorithm;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.cytoscape.biomarker.BiomarkerFinderAlgorithm;
import org.cytoscape.biomarker.ParameterPanel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.internal.CyServiceRegistrarImpl;
import org.cytoscape.session.CyNetworkNaming;
import org.osgi.framework.BundleContext;

public class NetworkPropagationAlgorithm implements BiomarkerFinderAlgorithm {
	
	private double[] currentScore;
	private double[] originalScore;
	private double[][] originalW;
	private double[][] normalizedW;
	private double alpha;
	private double threshold;
	private HashMap<Long,Integer> nodeHash = new HashMap<Long, Integer>();
	private HashMap<Integer,Long> HashNode = new HashMap<Integer, Long>();
	private CyServiceRegistrarImpl register;
	private CyNetwork network;
	private double cutoff;
	
	public NetworkPropagationAlgorithm(BundleContext bc){
		this.register = new CyServiceRegistrarImpl(bc);
		
	}
	
	@Override
	public void run(CyNetwork network) {
		this.network = network;
		double diff = Double.MAX_VALUE;
		setNodesHash(network);
		setScore(network);
		setWeight(network);
		normalizedW = normalize(originalW);
		
		while(diff > threshold){
			double[] nextScore = propagate();
			
			RealMatrix Fn = MatrixUtils.createColumnRealMatrix(nextScore);
			RealMatrix Fc = MatrixUtils.createColumnRealMatrix(currentScore);
			diff = Fn.subtract(Fc).getFrobeniusNorm();
			currentScore = nextScore;
		}
	}

	@Override
	public void setParameter(ParameterPanel parameterPanel) {
		alpha = (Double) parameterPanel.getParameters().get("alpha");
		threshold = (Double) parameterPanel.getParameters().get("threshold");
	}

	@Override
	public CySubNetwork getResult() {
		cutoff = getMean(currentScore);
		CyRootNetworkManager rootManager = register.getService(CyRootNetworkManager.class);
		CyNetworkManager netManager = register.getService(CyNetworkManager.class);
		
		CySubNetwork result = rootManager.getRootNetwork(network).addSubNetwork();
		String resultName = network.getDefaultNetworkTable().getRow(network.getSUID()).get("name", String.class)+"_result";
		result.getDefaultNetworkTable().getRow(result.getSUID()).set("name",resultName);
		
		if(result.getDefaultNodeTable().getColumn("score")==null){
			result.getDefaultNodeTable().createColumn("score", Double.class, false);	
		}
		else{
		}
		if(result.getDefaultEdgeTable().getColumn("weight")==null){
			result.getDefaultEdgeTable().createColumn("weight", Double.class, false);	
		}
		else{
		}
		
		HashMap<Long, Long> Original2Current = new HashMap<Long, Long>();
		for(CyNode n:network.getNodeList()){
			CyNode node = result.addNode();
			Original2Current.put(n.getSUID(), node.getSUID());
			result.getDefaultNodeTable().getRow(node.getSUID()).set("name", network.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class));
//			System.out.println(currentScore[nodeHash.get(n.getSUID())]);
			result.getDefaultNodeTable().getRow(node.getSUID()).set("score", currentScore[nodeHash.get(n.getSUID())]);		
		}		
		
		for(CyEdge e:network.getEdgeList()){
			Long trg = Original2Current.get(e.getTarget().getSUID());
			Long src = Original2Current.get(e.getSource().getSUID());
			Double w = network.getDefaultEdgeTable().getRow(e.getSUID()).get("weight", Double.class);
			String name = network.getDefaultEdgeTable().getRow(e.getSUID()).get("name", String.class);
			CyEdge edge = result.addEdge(result.getNode(src),result.getNode(trg),true);
			result.getDefaultEdgeTable().getRow(edge.getSUID()).set("weight", w);
			result.getDefaultEdgeTable().getRow(edge.getSUID()).set("name",name);
		}

//		for(CyNode n:network.getNodeList()){
//			double scr = currentScore[nodeHash.get(n.getSUID())];
//			String name = network.getDefaultNodeTable().getRow(n.getSUID()).get("name",String.class);
//			if(scr>cutoff){
//				CyNode node = result.addNode();
//				Original2Current.put(n.getSUID(), node.getSUID());
//				result.getDefaultNodeTable().getRow(node.getSUID()).set("name", name);
//				result.getDefaultNodeTable().getRow(node.getSUID()).set("score", scr);
//			}
//		}
//		for(CyEdge e:network.getEdgeList()){
//			Long trg = Original2Current.get(e.getTarget().getSUID());
//			Long src = Original2Current.get(e.getSource().getSUID());
//			if(trg!=null && src !=null){
//				CyEdge edge = result.addEdge(result.getNode(src),result.getNode(trg), false);
//				Double w = network.getDefaultEdgeTable().getRow(e.getSUID()).get("weight", Double.class);
//				result.getDefaultEdgeTable().getRow(edge.getSUID()).set("weight", w);
//			}
//		}
		
		netManager.addNetwork(result);
		return result;
	}

	private double[] propagate(){
		RealMatrix Wn = MatrixUtils.createRealMatrix(normalizedW);
		RealMatrix Fm = MatrixUtils.createColumnRealMatrix(currentScore);
		RealMatrix pm = MatrixUtils.createColumnRealMatrix(originalScore);
		
		RealMatrix Fnext =  ((Wn.multiply(Fm)).scalarMultiply(alpha) ).add(pm.scalarMultiply(1-alpha));
		double x[] = Fnext.getColumn(0);
		return x;	
	}

	private double[][] normalize(double[][] originalW){
		RealMatrix W = MatrixUtils.createRealMatrix(originalW);
		double[][] x = new double[W.getRowDimension()][W.getColumnDimension()];
		for(int i=0;i<W.getRowDimension();i++){
			x[i][i]=Math.sqrt(1/sumRow(W.getRow(i)));
		}
		RealMatrix xn = MatrixUtils.createRealMatrix(x);
		RealMatrix Wn = (xn.multiply(W) ).multiply(xn);
		double[][] ans = Wn.getData();
		return ans;
	}
	
	private double sumRow(double[] row){
		double x=0;
		for(int i=0;i<row.length;i++){
			x+=row[i];
		}
		return x;
	}
	
	
	private void setNodesHash(CyNetwork network){
		List<CyNode> nodeList = network.getNodeList();
		for(int i = 0; i < nodeList.size(); i++){
			nodeHash.put(nodeList.get(i).getSUID(), i);
			HashNode.put(i, nodeList.get(i).getSUID());
		}
	}
	
	private void setScore(CyNetwork network){
		CyTable nodeTable = network.getDefaultNodeTable();
		originalScore = new double[nodeHash.size()];
		for(int i = 0; i<nodeHash.size();i++){
			originalScore[i] = Double.valueOf(nodeTable.getRow(HashNode.get(i)).get("score", Double.class)).doubleValue();
		}
		currentScore = originalScore;
	}
	
	private void setWeight(CyNetwork network){
		originalW = new double[network.getDefaultNodeTable().getRowCount()][network.getDefaultNodeTable().getRowCount()];
		CyTable edgeTable = network.getDefaultEdgeTable();
		List<CyEdge> edgeList = network.getEdgeList();
		for(int i =0; i < edgeList.size(); i++){
			long src = edgeList.get(i).getSource().getSUID();
			long trg = edgeList.get(i).getTarget().getSUID();
			int indexSrc = nodeHash.get(src);
			int indexTrg = nodeHash.get(trg);
			double w = Double.valueOf(edgeTable.getRow(edgeList.get(i).getSUID()).get("weight", Double.class)).doubleValue();
			originalW[indexSrc][indexTrg] = w;
			originalW[indexTrg][indexSrc] = w;
		}
	}
	
	private double getMean(double[] a){
		double x=0;
		for(int i=0;i<a.length;i++){
			x+=a[i];
		}
		x=x/a.length;
		return x;
	}
	
}
