package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.rdf.model.Resource;

public interface UnaryContextAssertion extends ContextAssertion {
	/**
 	 * Get the resource of the ContextEntity that fills the role of this ContextAssertion
	 * @return The resource of the ContextEntity that fills the role of this ContextAssertion
	 */
	public Resource getRoleEntityResource();
}
