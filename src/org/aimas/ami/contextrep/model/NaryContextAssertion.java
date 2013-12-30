package org.aimas.ami.contextrep.model;

import java.util.Map;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Resource;

public interface NaryContextAssertion extends ContextAssertion {
	/**
	 * Get the map of OntProperties defining the types of ContextEntity 
	 * playing a role in this ContextAssertion associated with the {@link Resource} of the corresponding
	 * ContextEntity instances that play that role. 
	 * @return a map of {@link OntProperty} to ContextEntity {@link Resource} that contains the assertion roles
	 * and the ContextEntity Resource instances that fill them
	 */
	public Map<OntProperty, Resource> getAssertionRoles();
}
