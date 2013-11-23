package org.aimas.ami.contextrep.update;

import static com.hp.hpl.jena.sparql.modify.TemplateLib.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.ContextAssertionIndex;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.modify.UpdateEngineWorker;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.update.GraphStore;

public class ContextUpdateWorker extends UpdateEngineWorker {
	
	public ContextUpdateWorker(GraphStore graphStore, Binding initialBinding, Context context) {
		super(graphStore, initialBinding, context);
	}
	
	/*
	@Override
    public void visit(UpdateCreate update) {
        Node g = update.getGraph() ;
        
        if ( g == null )
            return ;
        
        if ( graphStore.containsGraph(g) ) {
            if ( ! alwaysSilent && ! update.isSilent() ) {
                error("Graph store already contains graph : "+g);
            }
            return;
        }
        
        // In-memory specific 
        graphStore.addGraph(g, GraphFactory.createDefaultGraph()) ;
        
        // notify Context Assertion creation as ContextAssertionEvents instance
        ContextAssertionIndex assertionIndex = Config.getContextAssertionIndex();
        OntResource assertionResource = assertionIndex.getResourceFromGraphUUID(g);
        
        if (assertionResource != null) {
        	String assertionStoreURI = assertionIndex.getStoreForAssertion(assertionResource);
        	Graph assertionStoreGraph = graphStore.getGraph(Node.createURI(assertionStoreURI));
        	
        	ContextAssertionEvent addContextAssertion = 
        		new ContextAssertionEvent(ContextAssertionEvent.CONTEXT_ASSERTION_ADDED, update.getGraph());
        	assertionStoreGraph.getEventManager().notifyEvent(graphStore.getGraph(update.getGraph()), addContextAssertion);
        }
    }
	*/
	
	@Override
	public void visit(UpdateDataInsert update) {
		super.visit(update);
		
		ContextAssertionIndex assertionIndex = Config.getContextAssertionIndex();
		List<Quad> quads = update.getQuads();
		Quad firstQuad = quads.get(0);
		Node graphNode = firstQuad.getGraph();
		
		// first, take the first quad and inspect the graph node to see if it is a
		// ContextAssertion named graph UUID. If it is, then we are dealing with a 
		// ContextAssertion insertion
		if (assertionIndex.isContextAssertionUUID(graphNode)) {
			OntResource assertionResource = assertionIndex.getResourceFromGraphUUID(graphNode);
			String assertionStoreURI = assertionIndex.getStoreForAssertion(assertionResource);
        	Graph assertionStoreGraph = graphStore.getGraph(Node.createURI(assertionStoreURI));
        	
        	// signal the context assertion insertion
        	ContextAssertionEvent addContextAssertion = 
        		new ContextAssertionEvent(ContextAssertionEvent.CONTEXT_ASSERTION_ADDED, graphNode);
            assertionStoreGraph.getEventManager().notifyEvent(graphStore.getGraph(graphNode), addContextAssertion);
		}
		
		// alternatively, test to see if we have a ContextAnnotation insertion
		if (assertionIndex.isContextAssertionStore(graphNode)) {
			Node assertionUUID = firstQuad.getSubject();
			OntResource assertionResource = assertionIndex.getResourceFromGraphUUID(assertionUUID);
			
			if (assertionResource != null) {
	        	Graph assertionStoreGraph = graphStore.getGraph(graphNode);
	        	
	        	// signal the context annotation insertion
	        	ContextAssertionEvent addContextAnn = 
	        		new ContextAssertionEvent(ContextAssertionEvent.CONTEXT_ANNOTATION_ADDED, assertionUUID);
            	assertionStoreGraph.getEventManager().notifyEvent(assertionStoreGraph, addContextAnn);
			}
		}
	}
	
	
	@Override
	protected void execInsert(List<Quad> quads, Node dftGraph, Iterator<Binding> bindings) {
		// We are only interested in the insert quads because we know that for Context SPIN
     	// Inferences we are only going to add assertions, not remove them.
     	// Removal from the ACTIVE contextStoreDataset will be done either based on continuity checks 
     	// or based on the internal counter that signals the end of ACTIVE time-to-live for an assertion
		
		Iterator<Quad> it = template(quads, dftGraph, bindings) ;
        if ( it == null ) return ;
        
        List<Node> updatedGraphNodes = new ArrayList<>();
        Map<Node, Quad> updatedGraphNodeMap = new HashMap<>();
        
        while (it.hasNext()) {
            Quad q = it.next();
            
            // add the graphNode to the list of updated graph Nodes if it doesn't exist yet
            if (!updatedGraphNodes.contains(q.getGraph())) {
            	updatedGraphNodes.add(q.getGraph());
            	updatedGraphNodeMap.put(q.getGraph(), q);
            }
            
            // insert the quad in the context graph store
            addToGraphStore(graphStore, q);
        }
		
		ContextAssertionIndex assertionIndex = Config.getContextAssertionIndex();
		for (Node graphNode : updatedGraphNodes) {
			// inspect the graph node to see if it is a
			// ContextAssertion named graph UUID. If it is, then we are dealing with a 
			// ContextAssertion insertion
			if (assertionIndex.isContextAssertionUUID(graphNode)) {
				OntResource assertionResource = assertionIndex.getResourceFromGraphUUID(graphNode);
				String assertionStoreURI = assertionIndex.getStoreForAssertion(assertionResource);
	        	Graph assertionStoreGraph = graphStore.getGraph(Node.createURI(assertionStoreURI));
	        	
	        	// signal the context assertion insertion
	        	ContextAssertionEvent addContextAssertion = 
	        		new ContextAssertionEvent(ContextAssertionEvent.CONTEXT_ASSERTION_ADDED, graphNode);
	            assertionStoreGraph.getEventManager().notifyEvent(graphStore.getGraph(graphNode), addContextAssertion);
			}
			
			// alternatively, test to see if we have a ContextAnnotation insertion
			if (assertionIndex.isContextAssertionStore(graphNode)) {
				Quad firstQuad = updatedGraphNodeMap.get(graphNode);
				Node assertionUUID = firstQuad.getSubject();
				OntResource assertionResource = assertionIndex.getResourceFromGraphUUID(assertionUUID);
				
				if (assertionResource != null) {
		        	Graph assertionStoreGraph = graphStore.getGraph(graphNode);
		        	
		        	// signal the context annotation insertion
		        	ContextAssertionEvent addContextAnn = 
		        		new ContextAssertionEvent(ContextAssertionEvent.CONTEXT_ANNOTATION_ADDED, assertionUUID);
	            	assertionStoreGraph.getEventManager().notifyEvent(assertionStoreGraph, addContextAnn);
				}
			}
		}
    }
	
	// Catch all individual adds of quads (and deletes - mainly for symmetry). 
    private static void addToGraphStore(GraphStore graphStore, Quad quad) 
    {
        // Check legal triple.
        if ( quad.isLegalAsData() )
            graphStore.add(quad);
        // Else drop.
        //Log.warn(UpdateEngineWorker.class, "Bad quad as data: "+quad) ;
    }
}
