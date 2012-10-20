package org.cytoscape.biomarkerfinder.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactory;
import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactoryManager;
import org.cytoscape.biomarkerfinder.event.BiomarkerFinderAlgorithmFactoryAddedEvent;
import org.cytoscape.event.CyEventHelper;

public class BiomarkerFinderAlgorithmFactoryManagerImpl implements BiomarkerFinderAlgorithmFactoryManager {

	private final Map<String, BiomarkerFinderAlgorithmFactory> factoryMap;

	private final CyEventHelper eventHelper;
	
	public BiomarkerFinderAlgorithmFactoryManagerImpl(final CyEventHelper eventHelper) {
		this.factoryMap = new HashMap<String, BiomarkerFinderAlgorithmFactory>();
		this.eventHelper = eventHelper;
	}
	
	@Override
	public BiomarkerFinderAlgorithmFactory getAlgorithm(final String algorithmID) {
		return factoryMap.get(algorithmID);
	}

	@Override
	public Collection<BiomarkerFinderAlgorithmFactory> getAllFactories() {
		return Collections.unmodifiableCollection(factoryMap.values());
	}
	
	public void registerFactory(BiomarkerFinderAlgorithmFactory factory, Map props) {
		if(factory!= null) {
			System.out.println("#### Got new Biomarker finder factory: " + factory.getAlgorithmID());
			factoryMap.put(factory.getAlgorithmID(), factory);
			eventHelper.fireEvent(new BiomarkerFinderAlgorithmFactoryAddedEvent(this, factory));
		}
	}
	
	public void unregisterFactory(BiomarkerFinderAlgorithmFactory factory, Map props) {
		// TODO: Implement this!
	}

}
