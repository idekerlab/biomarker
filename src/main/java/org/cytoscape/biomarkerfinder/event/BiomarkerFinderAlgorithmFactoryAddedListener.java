package org.cytoscape.biomarkerfinder.event;

import org.cytoscape.event.CyListener;

public interface BiomarkerFinderAlgorithmFactoryAddedListener extends CyListener {
	
	public void handleEvent(BiomarkerFinderAlgorithmFactoryAddedEvent e);

}
