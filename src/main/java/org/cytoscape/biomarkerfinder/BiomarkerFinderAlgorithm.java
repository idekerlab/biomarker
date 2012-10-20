package org.cytoscape.biomarkerfinder;

import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.Task;

/**
 * 
 *
 */
public interface BiomarkerFinderAlgorithm extends Task {
	
	String getDisplayName();
	
	String getAlgorithmID();
	
	CyNetwork getResult();
	
	void setParameter(final Map<String, Object> parameters);
}
