package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.aimas.ami.contextrep.model.ContextAssertion;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphListener;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.modify.GraphStoreEvents;

public class ContextAssertionUpdateListener implements GraphListener {
	private boolean updateRunning = false;
	private ContextUpdateHookWrapper updateHookWrapper;
	
	public ContextAssertionUpdateListener() {}
	
	@Override
	public void notifyAddTriple(Graph g, Triple t) {}
	
	@Override
	public void notifyAddArray(Graph g, Triple[] triples) {}
	
	@Override
	public void notifyAddList(Graph g, List<Triple> triples) {}
	
	@Override
	public void notifyAddIterator(Graph g, Iterator<Triple> it) {}
	
	@Override
	public void notifyAddGraph(Graph g, Graph added) {}
	
	@Override
	public void notifyDeleteTriple(Graph g, Triple t) {}
	
	@Override
	public void notifyDeleteList(Graph g, List<Triple> L) {}
	
	@Override
	public void notifyDeleteArray(Graph g, Triple[] triples) {}
	
	@Override
	public void notifyDeleteIterator(Graph g, Iterator<Triple> it) {}
	
	@Override
	public void notifyDeleteGraph(Graph g, Graph removed) {}
	
	@Override
	public void notifyEvent(Graph source, Object value) {
		if (value.equals(GraphStoreEvents.RequestStartEvent)) {
			updateRunning = true;
			updateHookWrapper = new ContextUpdateHookWrapper();
		}
        else if (value.equals(GraphStoreEvents.RequestFinishEvent)) {
            updateRunning = false;
        }
        else if (value instanceof ContextAssertionEvent) {
			ContextAssertionEvent event = (ContextAssertionEvent)value;
			String eventTitle = (String)event.getTitle();
			
			if (eventTitle.equals(ContextAssertionEvent.CONTEXT_ASSERTION_ADDED) && updateRunning) {
				//updateActionList.add(new CheckConstraintAction(event.getAssertionResource()));
				//updateActionList.add(new CheckInferenceAction(event.getAssertionResource()));
				//System.out.println("ENQUEUING CHECK_CONSTRAINT AND CHECK_INFERENCE ACTIONS");
			}
			else if ((eventTitle.equals(ContextAssertionEvent.CONTEXT_ANNOTATION_ADDED) && updateRunning) || 
					eventTitle.equals(ContextAssertionEvent.CONTEXT_ASSERTION_INFERRED)) {
				Node contextAssertionUUID = (Node)event.getContent();
				
				// first set the assertion
				updateHookWrapper.setAssertion(event.getAssertion());
				
				// then check continuity
				System.out.println("============== ADDING HOOKS ==============");
				updateHookWrapper.addContinuityHook(new CheckContinuityHook(
						event.getAssertion(), contextAssertionUUID));
				
				// then check the 
				updateHookWrapper.addConstraintHook(new CheckConstraintHook(event.getAssertion()));
				
				// and lastly check if inference of a new ContextAssertion is applicable
				updateHookWrapper.addInferenceHook(new CheckInferenceHook(event.getAssertion()));
			}
		}
	}
	
	public ContextUpdateHookWrapper collectContextUpdateHooks() {
		return updateHookWrapper;
	}
	
	public static class ContextUpdateHookWrapper {
		private ContextAssertion assertion;
		
		private List<CheckContinuityHook> continuityHooks;
		private List<CheckConstraintHook> constraintHooks;
		private List<CheckInferenceHook> inferenceHooks;
		
		ContextUpdateHookWrapper() {
			continuityHooks = new ArrayList<>();
			constraintHooks = new ArrayList<>();
			inferenceHooks = new ArrayList<>();
		}
		
		ContextUpdateHookWrapper(ContextAssertion assertion) {
			this();
			this.assertion = assertion;
		}
		
		
		
		void addContinuityHook(CheckContinuityHook hook) {
			continuityHooks.add(hook);
		}
		
		void addConstraintHook(CheckConstraintHook hook) {
			constraintHooks.add(hook);
		}
		
		void addInferenceHook(CheckInferenceHook hook) {
			inferenceHooks.add(hook);
		}

		public List<CheckContinuityHook> getContinuityHooks() {
			return continuityHooks;
		}

		public List<CheckConstraintHook> getConstraintHooks() {
			return constraintHooks;
		}

		public List<CheckInferenceHook> getInferenceHooks() {
			return inferenceHooks;
		}
		
		public ContextAssertion getAssertion() {
			return assertion;
		}

		public void setAssertion(ContextAssertion assertion) {
			this.assertion = assertion;
		}

		public void extend(ContextUpdateHookWrapper other) {
			continuityHooks.addAll(other.getContinuityHooks());
			constraintHooks.addAll(other.getConstraintHooks());
			inferenceHooks.addAll(other.getInferenceHooks());
		}
	}
}
