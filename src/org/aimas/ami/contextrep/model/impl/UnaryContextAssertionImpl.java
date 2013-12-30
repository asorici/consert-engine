package org.aimas.ami.contextrep.model.impl;

import org.aimas.ami.contextrep.model.UnaryContextAssertion;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class UnaryContextAssertionImpl extends ContextAssertionImpl implements UnaryContextAssertion {
	private Resource roleEntityResource;
	
	public UnaryContextAssertionImpl(ContextAssertionType assertionType,
	        int assertionArity, OntResource assertionOntologyResource, Resource roleEntityResource) {
		super(assertionType, assertionArity, assertionOntologyResource);
		this.roleEntityResource = roleEntityResource;
	}

	@Override
    public Resource getRoleEntityResource() {
	    return roleEntityResource;
    }
	
}
