package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.ontology.OntProperty;

public interface UnaryContextAssertion extends ContextAssertion {
	
	
	/**
 	 * Get the ContextEntity that fills the role of this ContextAssertion
	 * @return the ContextEntity that fills the role of this ContextAssertion
	 */
	public ContextEntity getEntity();
}
