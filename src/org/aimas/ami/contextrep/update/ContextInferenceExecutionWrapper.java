package org.aimas.ami.contextrep.update;

import java.util.concurrent.Callable;

import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.test.performance.RunTest;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;


public class ContextInferenceExecutionWrapper implements Callable<AssertionInferenceResult> {
	private CheckInferenceHook inferenceHook;
	private int assertionInsertID;	// the ID corresponding to the assertion insert that triggered this inference hook
	
	public ContextInferenceExecutionWrapper(CheckInferenceHook inferenceHook, int assertionInsertID) {
		this.inferenceHook = inferenceHook;
		this.assertionInsertID = assertionInsertID;
	}
	
	@Override
	public AssertionInferenceResult call() {
		long start = System.currentTimeMillis();
		
		InferenceHookResult inferenceHookResult = null;
		
		// for test purposes increment atomic counter
		RunTest.enqueuedInferenceTracker.getAndIncrement();
		
		// STEP 1: start a new READ transaction on the contextStoreDataset
		Dataset contextDataset = Engine.getRuntimeContextStore();
		contextDataset.begin(ReadWrite.READ);
		//contextDataset.getLock().enterCriticalSection(true);
		
		try {
			// STEP 2: execute inference hook
			inferenceHookResult = inferenceHook.exec(contextDataset);
			
			if (inferenceHookResult.hasError()) {
				System.out.println("Inference ERROR!");
			}
		}
		finally {
			contextDataset.end();
			//contextDataset.getLock().leaveCriticalSection();
		}
		
		long end = System.currentTimeMillis();
		return new AssertionInferenceResult(assertionInsertID, start, (int)(end - start), 
				inferenceHook.getContextAssertion(), inferenceHookResult);
	}
}
