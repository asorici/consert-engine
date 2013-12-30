package org.aimas.ami.contextrep.model;

import org.topbraid.spin.model.NamedGraph;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public interface ContextAssertionGraph extends NamedGraph {
	
	/**
	 * Get the context domain ontology definition for this ContextAssertion 
	 * @return the <a>com.hp.hpl.jena.ontology.OntResource</a> that defines
	 * this instance of a ContextAssertion  
	 */
	public OntResource getAssertionResource();
	
	/**
	 * Gets the URI Resource or Variable that holds the identifier of this
	 * ContextAssertion.  If it's a Variable, then this method will typecast
	 * it into an instance of Variable.
	 * @return a URI Resource or Variable identifying this ContextAssertion 
	 */
	public Resource getAssertionIdentifier();
}
