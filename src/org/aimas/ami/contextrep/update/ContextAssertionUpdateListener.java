package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphListener;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.modify.GraphStoreEvents;

public class ContextAssertionUpdateListener implements GraphListener {
	private boolean updateRunning = false;
	private List<ContextUpdateHook> updateActionHooks;
	
	public ContextAssertionUpdateListener() {
		updateActionHooks = new ArrayList<>();
	}
	
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
			updateActionHooks = new ArrayList<>();
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
				
				// first check continuity 
				updateActionHooks.add(new CheckValidityContinuityHook(
						event.getAssertionResource(), contextAssertionUUID));
				
				// then check the 
				updateActionHooks.add(new CheckConstraintHook(event.getAssertionResource()));
				
				// and lastly check if inference of a new ContextAssertion is applicable
				updateActionHooks.add(new CheckInferenceHook(event.getAssertionResource()));
			}
		}
	}
	
	public List<ContextUpdateHook> collectContextUpdateHooks() {
		return updateActionHooks;
	}
}
