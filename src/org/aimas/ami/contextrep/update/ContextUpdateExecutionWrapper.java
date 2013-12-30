package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.test.performance.RunTest;
import org.aimas.ami.contextrep.update.ContextAssertionUpdateListener.ContextUpdateHookWrapper;
import org.aimas.ami.contextrep.utils.ContextAssertionUtil;
import org.aimas.ami.contextrep.utils.ContextUpdateUtil;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateRequest;

public class ContextUpdateExecutionWrapper implements Callable<AssertionInsertResult> {
	private static int counter = 0;
	private int assertionInsertID = counter++;
	
	private UpdateRequest request;
	
	public ContextUpdateExecutionWrapper(UpdateRequest request) {
		this.request = request;
	}
	
	
	public int getAssertionInsertID() {
		return assertionInsertID;
	}
	
	
	@Override
    public AssertionInsertResult call() {
		long start = System.currentTimeMillis();
		
		// for testing increment the atomic counter
		RunTest.executedInsertionsTracker.getAndIncrement();
		
		// STEP 1: start a new WRITE transaction on the contextStoreDataset
		Dataset contextDataset = Config.getContextDataset();
		contextDataset.begin(ReadWrite.WRITE);
		//contextDataset.getLock().enterCriticalSection(false);
		
		ContextUpdateHookWrapper contextUpdateHooks = null;
		
		ContextAssertion contextAssertion = null;
		List<ContinuityHookResult> continuityResults = null;
		List<ConstraintHookResult> constraintResults = null;
		
		try {
			// STEP 2: analyze request
			List<Graph> updatedContextStores = new ArrayList<>(analyzeRequest(contextDataset, null));
			
			// STEP 3: register listeners for updatedContextStores
			List<ContextAssertionUpdateListener> updateListeners = ContextAssertionUtil.registerContextAssertionStoreListeners(updatedContextStores);
			
			// STEP 3: execute updates
			GraphStore graphStore = GraphStoreFactory.create(contextDataset);
			UpdateAction.execute(request, graphStore);
			
			// STEP 5: collect detected hooks and separate it according to their type to collect individual results
			// for each
			contextUpdateHooks = ContextAssertionUtil.collectContextUpdateHooks(updateListeners);
			
			// STEP 6: execute VALIDITY_CONTINUITY and CONSTRAINT HOOKS
			if (contextUpdateHooks != null) {
				contextAssertion = contextUpdateHooks.getAssertion();
				continuityResults = execContinuityHooks(contextUpdateHooks.getContinuityHooks(), contextDataset);
				constraintResults = execConstraintHooks(contextUpdateHooks.getConstraintHooks(), contextDataset);
			}
			
			// STEP 7: commit transaction
			contextDataset.commit();
		} 
		catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			contextDataset.end();
			//contextDataset.getLock().leaveCriticalSection();
		}
		
		// STEP 8: enqueue detected INFERENCE HOOK to assertionInferenceExecutor
		if (contextUpdateHooks != null) {
			enqueueInferenceHooks(contextUpdateHooks.getInferenceHooks(), assertionInsertID);
		}
		
		long end = System.currentTimeMillis();
		
		return new AssertionInsertResult(assertionInsertID, start, end - start, contextAssertion, continuityResults, constraintResults);
    }
	
	
	protected Collection<Graph> analyzeRequest(Dataset dataset, Map<String, RDFNode> templateBindings) {
		Collection<Graph> updatedContextStores = null;
		
		for (Update up : request.getOperations()) {
			if (updatedContextStores == null) {
				updatedContextStores = ContextUpdateUtil.getUpdatedGraphs(up, dataset, templateBindings, true);
			}
			else {
				updatedContextStores.addAll(ContextUpdateUtil.getUpdatedGraphs(up, dataset, templateBindings, true));
			}
		}
		
		return updatedContextStores;
	}
	
	
	protected List<ContinuityHookResult> execContinuityHooks(List<CheckValidityContinuityHook> hooks, Dataset contextStoreDataset) {
		List<ContinuityHookResult> hookResults = new LinkedList<>();
		
		for (int i = 0; i < hooks.size(); i++) {
			CheckValidityContinuityHook hook = hooks.get(i);
			
			ContinuityHookResult result = hook.exec(contextStoreDataset);
			if (result.hasError()) {
				System.out.println("Action ERROR!");
			}
			
			hookResults.add(result);
		}
		
		return hookResults;
	}
	
	
	protected List<ConstraintHookResult> execConstraintHooks(List<CheckConstraintHook> hooks, Dataset contextStoreDataset) {
		List<ConstraintHookResult> hookResults = new LinkedList<>();
		
		for (int i = 0; i < hooks.size(); i++) {
			CheckConstraintHook hook = hooks.get(i);
			
			ConstraintHookResult result = hook.exec(contextStoreDataset);
			if (result.hasError()) {
				System.out.println("Action ERROR!");
			}
			
			hookResults.add(result);
		}
		
		return hookResults;
	}
	
	
	private void enqueueInferenceHooks(List<CheckInferenceHook> inferenceHooks, int assertionInsertID) {
	    for (CheckInferenceHook hook : inferenceHooks) {
	    	Future<AssertionInferenceResult> result = Config.assertionInferenceExecutor().submit(new ContextInferenceExecutionWrapper(hook, assertionInsertID));
	    	RunTest.inferenceResults.put(assertionInsertID, result);
	    }
    }
}
