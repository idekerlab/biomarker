package org.cytoscape.biomarkerfinder.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

public class ScoreDataReader {
	private HashMap<String,Long> NodeNameHash = new HashMap<String, Long>();
	
	public ScoreDataReader(CyNetwork network,File scoreFile){
		setNodeNameHash(network);
		
		try {
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(scoreFile));
			String line = "";
			String sep = "\t"; //tsv file
			br.readLine();	//skip attribute row
			if(network.getDefaultNodeTable().getColumn("score")==null){
				network.getDefaultNodeTable().createColumn("score",Double.class, false,(double)0);				
			}
			else{
			}
			while( (line = br.readLine() )!=null){
				double score = 0d;
				String name = line.split(sep)[1];	//second column "NAME"
//				double score = Double.valueOf(line.split(sep)[2]).doubleValue();	//third column "RNA Change" 
				if(!line.split(sep)[3].isEmpty()){
					score = Double.valueOf(line.split(sep)[3]).doubleValue();	//fourth column "Protein Change" including a lot of NA
				}
				scoreMapping(network, name, score);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
//	private void scoreMapping(CyNetwork network, String name, double score){
//		if(NodeNameHash.containsKey(name)){
//			network.getDefaultNodeTable().getRow(NodeNameHash.get(name)).set("score", score);
//		}
//		else{
//			return;
//		}	
//	}
	private void scoreMapping(CyNetwork network, String name, double score){
		for(CyNode n:network.getNodeList()){
			if(name.matches(".*"+network.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class)+".*")){
				network.getDefaultNodeTable().getRow(n.getSUID()).set("score", score);
				return;
			}
		}
	}
}

