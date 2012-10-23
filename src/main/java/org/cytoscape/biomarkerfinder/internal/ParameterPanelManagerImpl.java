package org.cytoscape.biomarkerfinder.internal;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.biomarkerfinder.ParameterPanel;
import org.cytoscape.biomarkerfinder.ParameterPanelManager;

public class ParameterPanelManagerImpl implements ParameterPanelManager{

	private final Map<String, ParameterPanel> panelMap;
	
	public ParameterPanelManagerImpl() {
		panelMap = new HashMap<String, ParameterPanel>();
	}
	
	@Override
	public ParameterPanel getParameterPanel(String algorithmID) {
		return panelMap.get(algorithmID);
	}
	
	public int getPanelCount(){
		return panelMap.size();
	}
	public void registerPanel(ParameterPanel panel, Map props){
		if(panel!=null){
			panelMap.put(panel.getAlgorithmID(), panel);
		}
	}
	
	public void unregisterPanel(ParameterPanel panel,Map props){
		if(panelMap.containsKey(panel.getAlgorithmID())){
			panelMap.remove(panel.getAlgorithmID());
		}
	}
}
