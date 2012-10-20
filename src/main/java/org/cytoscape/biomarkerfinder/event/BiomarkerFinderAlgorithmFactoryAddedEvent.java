package org.cytoscape.biomarkerfinder.event;

import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactory;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactoryManager;
import org.cytoscape.event.AbstractCyEvent;

public final class BiomarkerFinderAlgorithmFactoryAddedEvent extends
		AbstractCyEvent<BiomarkerFinderAlgorithmFactoryManager> {

	private final BiomarkerFinderAlgorithmFactory newFactory;

	public BiomarkerFinderAlgorithmFactoryAddedEvent(BiomarkerFinderAlgorithmFactoryManager source,
			final BiomarkerFinderAlgorithmFactory newFactory) {
		super(source, BiomarkerFinderAlgorithmFactoryAddedListener.class);
		
		this.newFactory = newFactory;
	}

	public BiomarkerFinderAlgorithmFactory getFacotry() {
		return this.newFactory;
	}
	
}
