package org.aimas.ami.contextrep.update;

import org.aimas.ami.contextrep.core.Config;

import com.hp.hpl.jena.graph.GraphEvents;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntResource;

public class ContextAssertionEvent extends GraphEvents {
	public static final String CONTEXT_ASSERTION_ADDED = "context-assertion-added"; 
	public static final String CONTEXT_ASSERTION_REMOVED = "context-assertion-removed";
	public static final String CONTEXT_ANNOTATION_ADDED = "context-annotation-added";
	public static final String CONTEXT_ASSERTION_INFERRED = "context-assertion-inferred";
	
	public static final Object CONTEXT_ASSERTION_EXECUTE_HOOKS = new Object();
	
	public ContextAssertionEvent(String title, Node graphNode) {
	    super(title, graphNode);
    }
	
	public OntResource getAssertionResource() {
		Node graphNode = (Node)content;
		return Config.getContextAssertionIndex().getResourceFromGraphUUID(graphNode);
	}
}
