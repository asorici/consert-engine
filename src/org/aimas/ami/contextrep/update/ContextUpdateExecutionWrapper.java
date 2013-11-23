package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.core.Config;
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

public class ContextUpdateExecutionWrapper implements Runnable {
	private UpdateRequest request;
	
	public ContextUpdateExecutionWrapper(UpdateRequest request) {
		this.request = request;
	}
	
	
	@Override
    public void run() {
		// STEP 1: start a new WRITE transaction on the contextStoreDataset
		Dataset contextStoreDataset = Config.getContextStoreDataset();
		contextStoreDataset.begin(ReadWrite.WRITE);
		
		ContextUpdateHookWrapper contextUpdateHooks = null;
		
		try {
			// STEP 2: analyze request
			List<Graph> updatedContextStores = new ArrayList<>(analyzeRequest(contextStoreDataset, null));
			
			// STEP 3: register listeners for updatedContextStores
			List<ContextAssertionUpdateListener> updateListeners = ContextAssertionUtil.registerContextAssertionStoreListeners(updatedContextStores);
			
			// STEP 3: execute updates
			GraphStore graphStore = GraphStoreFactory.create(contextStoreDataset);
			UpdateAction.execute(request, graphStore);
			
			// STEP 5: collect detected hooks and separate it according to 
			contextUpdateHooks = ContextAssertionUtil.collectContextUpdateHooks(updateListeners);
			
			// STEP 6: execute VALIDITY_CONTINUITY and CONSTRAINT HOOKS
			if (contextUpdateHooks != null) {
				execHooks(contextUpdateHooks.getContinuityHooks(), contextStoreDataset);
				execHooks(contextUpdateHooks.getConstraintHooks(), contextStoreDataset);
			}
			
			// STEP 7: commit transaction
			contextStoreDataset.commit();
		} 
		finally {
			contextStoreDataset.end();
		}
		
		// STEP 8: enqueue detected INFERENCE HOOK to assertionInferenceExecutor
		if (contextUpdateHooks != null) {
			enqueueInferenceHooks(contextUpdateHooks.getInferenceHooks());
		}
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
	
	
	protected void execHooks(List<? extends ContextUpdateHook> hooks, Dataset contextStoreDataset) {
		for (int i = 0; i < hooks.size(); i++) {
			ContextUpdateHook hook = hooks.get(i);
			
			// System.out.println("Executing context update action: " + hook);
			if (!hook.exec(contextStoreDataset)) {
				System.out.println("Action ERROR!");
			}
		}
	}
	
	private void enqueueInferenceHooks(List<CheckInferenceHook> inferenceHooks) {
	    for (CheckInferenceHook hook : inferenceHooks) {
	    	Config.assertionInferenceExecutor().execute(new ContextInferenceExecutionWrapper(hook));
	    }
    }
}
