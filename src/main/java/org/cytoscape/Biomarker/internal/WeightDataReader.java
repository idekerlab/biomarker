package org.cytoscape.Biomarker.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

public class WeightDataReader {
	private HashMap<String,Long> NodeNameHash = new HashMap<String, Long>();
	
	public WeightDataReader(CyNetwork network, File weightData){
		setNodeNameHash(network);
		try {
			if(network.getDefaultEdgeTable().getColumn("weight")==null){
				network.getDefaultEdgeTable().createColumn("weight", Double.class, false,Double.MIN_VALUE);				
			}
			else{
			}
			BufferedReader br = new BufferedReader(new FileReader(weightData));
			br.readLine(); //skip attribute
			String line = "";
			String sep = "\t";
			while((line=br.readLine()) != null){
				String src = line.split(sep)[0];
				String trg = line.split(sep)[1];
				double weight = Double.valueOf(line.split(sep)[2]).doubleValue();
				weightMapping(network, src, trg, weight);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void weightMapping(CyNetwork network, String src, String trg, double weight){
		if(NodeNameHash.containsKey(src) && NodeNameHash.containsKey(trg)){
			long srcSUID = NodeNameHash.get(src);
			long trgSUID = NodeNameHash.get(trg);
			System.out.println();
			List<CyEdge> edgeList = network.getEdgeList();

			for(CyEdge edge:edgeList){
				if(srcSUID == edge.getSource().getSUID() &&
						trgSUID == edge.getTarget().getSUID()){
					network.getDefaultEdgeTable().getRow(edge.getSUID()).set("weight", weight);
				}
				else if(trgSUID == edge.getSource().getSUID() &&
						srcSUID == edge.getTarget().getSUID()){
					network.getDefaultEdgeTable().getRow(edge.getSUID()).set("weight", weight);
				}
			}
		}
		else{
			return;
		}
	}
	
	private void setNodeNameHash(CyNetwork network){
		if(network==null){
			JOptionPane.showMessageDialog(null,"network is null");
			return;
		}
		else{
			List<CyNode> nodeList = network.getNodeList();
			for(CyNode node:nodeList){
				String name = network.getDefaultNodeTable().getRow(node.getSUID()).get("name", String.class);
				NodeNameHash.put(name, node.getSUID());
			}
		}
	}

	private double logisticFunction(double t){
		double x =0;
		x=1/(1+Math.exp(-t));
		return x;
	}
}
