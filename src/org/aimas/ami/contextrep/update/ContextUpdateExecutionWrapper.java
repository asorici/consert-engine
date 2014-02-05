package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.DerivationRuleDictionary;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.test.performance.RunTest;
import org.aimas.ami.contextrep.update.ContextAssertionUpdateListener.ContextUpdateHookWrapper;
import org.aimas.ami.contextrep.utils.ContextUpdateUtil;

import com.hp.hpl.jena.graph.Node;
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
		
		List<ContextAssertion> insertedContextAssertions = new LinkedList<>();
		List<CheckValidityContinuityHook> continuityChecks = new LinkedList<>();
		List<CheckConstraintHook> constraintChecks = new LinkedList<>();
		List<CheckInferenceHook> inferenceChecks = new LinkedList<>();
		
		List<ContinuityHookResult> continuityResults = null;
		List<ConstraintHookResult> constraintResults = null;
		
		// STEP 2: analyze request
		List<Node> updatedContextStores = new ArrayList<>(analyzeRequest(contextDataset, null));
		
		try {
			// STEP 3: determine the inserted ContextAssertion based on the request analysis - the updates context stores
			// 		   since for each update there is only one corresponding ContextAssertion we can break at the first
			//		   match
			for (Node graphNode : updatedContextStores) {
				if (Config.getContextAssertionIndex().isContextAssertionUUID(graphNode)) {
					// get the inserted assertion
					ContextAssertion assertion = Config.getContextAssertionIndex().getAssertionFromGraphUUID(graphNode);
					insertedContextAssertions.add(assertion);
					
					// add continuity checks for it
					continuityChecks.add(new CheckValidityContinuityHook(assertion, graphNode));
					
					// add constraint checks for it
					constraintChecks.add(new CheckConstraintHook(assertion));
					
					// add inference checks for it
					DerivationRuleDictionary ruleDict = Config.getDerivationRuleDictionary();
					if (ruleDict.getDerivationsForAssertion(assertion) != null) {
						inferenceChecks.add(new CheckInferenceHook(assertion));
					}
				}
			}
			
			//for (Node graphNode : updatedContextStores) {
			//	contextDataset.asDatasetGraph().getGraph(graphNode).getTransactionHandler().begin();
			//}
			
			// STEP 4: execute updates
			GraphStore graphStore = GraphStoreFactory.create(contextDataset);
			UpdateAction.execute(request, graphStore);
			
			// STEP 5: execute VALIDITY_CONTINUITY and CONSTRAINT HOOKS
			continuityResults = execContinuityHooks(continuityChecks, contextDataset);
			constraintResults = execConstraintHooks(constraintChecks, contextDataset);
			
			// STEP 6: commit transaction
			contextDataset.commit();
		} 
		catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			contextDataset.end();
			//contextDataset.getLock().leaveCriticalSection();
		}
		
		// STEP 7: enqueue detected INFERENCE HOOK to assertionInferenceExecutor
		enqueueInferenceHooks(inferenceChecks, assertionInsertID);
		
		
		long end = System.currentTimeMillis();
		
		return new AssertionInsertResult(assertionInsertID, start, (int)(end - start), insertedContextAssertions, 
				continuityResults, constraintResults);
    }
	
	
	protected Collection<Node> analyzeRequest(Dataset dataset, Map<String, RDFNode> templateBindings) {
		Collection<Node> updatedContextStores = null;
		
		for (Update up : request.getOperations()) {
			if (updatedContextStores == null) {
				updatedContextStores = ContextUpdateUtil.getUpdatedGraphs(up, dataset, templateBindings, false);
			}
			else {
				updatedContextStores.addAll(ContextUpdateUtil.getUpdatedGraphs(up, dataset, templateBindings, false));
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
	    	
	    	RunTest.inferenceTaskEnqueueTime.put(assertionInsertID, System.currentTimeMillis());
	    	RunTest.inferenceResults.put(assertionInsertID, result);
	    }
    }
}
