package org.cytoscape.biomarkerfinder;

import java.util.Collection;

public interface BiomarkerFinderAlgorithmFactoryManager {

	BiomarkerFinderAlgorithmFactory getAlgorithm(final String algorithmID);
	
	Collection<BiomarkerFinderAlgorithmFactory> getAllFactories();
}
