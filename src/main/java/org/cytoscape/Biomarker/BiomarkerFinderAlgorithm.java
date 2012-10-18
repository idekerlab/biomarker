package org.cytoscape.biomarker;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

/**
 * 
 *
 */
public interface BiomarkerFinderAlgorithm {
	
	public void run(CyNetwork network);
	public void setParameter(ParameterPanel parameterPanel);
	public CySubNetwork getResult();
}
