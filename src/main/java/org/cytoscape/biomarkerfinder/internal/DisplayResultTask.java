package org.cytoscape.biomarkerfinder.internal;

import org.cytoscape.biomarkerfinder.BiomarkerFinderAlgorithm;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class DisplayResultTask extends AbstractTask {

	private final BiomarkerFinderAlgorithm task;
	
	public DisplayResultTask(final BiomarkerFinderAlgorithm algorithm) {
		this.task = algorithm;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		final CyNetwork result = task.getResult();
		//final DisplayResult dr = new DisplayResult(result);
	}

}
