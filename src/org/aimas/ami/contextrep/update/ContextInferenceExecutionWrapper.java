package org.aimas.ami.contextrep.update;

import org.aimas.ami.contextrep.core.Config;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;


public class ContextInferenceExecutionWrapper implements Runnable {
	private CheckInferenceHook inferenceHook;
	
	public ContextInferenceExecutionWrapper(CheckInferenceHook inferenceHook) {
		this.inferenceHook = inferenceHook;
	}
	
	@Override
	public void run() {
		// STEP 1: start a new READ transaction on the contextStoreDataset
		Dataset contextStoreDataset = Config.getContextStoreDataset();
		contextStoreDataset.begin(ReadWrite.READ);
		
		try {
			// STEP 2: execute inference hook
			if (!inferenceHook.exec(contextStoreDataset)) {
				System.out.println("Inference ERROR!");
			}
		}
		finally {
			contextStoreDataset.end();
		}
	}
}
