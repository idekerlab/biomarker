package org.cytoscape.biomarkerfinder.internal.algorithm;

import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithmFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class NetworkPropagationTaskFactory extends AbstractTaskFactory implements NetworkTaskFactory,
		BiomarkerFinderAlgorithmFactory {

	private final CyRootNetworkManager rootManager;
	private final CyNetworkManager networkManager;

	private final String algorithmID;

	public NetworkPropagationTaskFactory(final CyRootNetworkManager rootManager, final CyNetworkManager networkManager) {
		this.networkManager = networkManager;
		this.rootManager = rootManager;
		this.algorithmID = NetworkPropagationAlgorithm.ALGORITHM_ID;
	}

	@Override
	public TaskIterator createTaskIterator(final CyNetwork network) {
		return new TaskIterator(new NetworkPropagationAlgorithm(network, rootManager, networkManager));
	}

	@Override
	public boolean isReady(CyNetwork network) {
		return false;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return null;
	}

	@Override
	public String getAlgorithmID() {
		return algorithmID;
	}

	@Override
	public String toString() {
		return algorithmID;
	}
	
}
