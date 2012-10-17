package org.cytoscape.Biomarker;

import javax.swing.JPanel;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

public interface BiomarkerFinderAlgorithm {
	
	public void run(CyNetwork network);
	public void setParameter(ParameterPanel parameterPanel);
	public CySubNetwork getResult();
}
