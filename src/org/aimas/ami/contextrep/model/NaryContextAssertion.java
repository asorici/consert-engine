package org.aimas.ami.contextrep.model;

import java.util.Map;

import com.hp.hpl.jena.ontology.OntProperty;

public interface NaryContextAssertion extends ContextAssertion {
	
	/**
	 * Get the map of OntProperties defining the types of ContextEntity 
	 * playing a role in this ContextAssertion associated with the ContextEntity
	 * instances that play that role. 
	 * @return a map of OntProperty to ContextEntity that contains the assertion roles
	 * and the ContextEntity instance that fill them
	 */
	public Map<OntProperty, ContextEntity> getAssertionRoles();
}
