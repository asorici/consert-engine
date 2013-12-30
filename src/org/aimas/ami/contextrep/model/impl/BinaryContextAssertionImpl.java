package org.aimas.ami.contextrep.model.impl;

import org.aimas.ami.contextrep.model.BinaryContextAssertion;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class BinaryContextAssertionImpl extends ContextAssertionImpl implements
        BinaryContextAssertion {
	
	private Resource domainEntityResource;
	private Resource rangeEntityResource;
	
	public BinaryContextAssertionImpl(ContextAssertionType assertionType,
	        int assertionArity, OntResource assertionOntologyResource,
	        Resource domainEntityResource, Resource rangeEntityResource) {
		super(assertionType, assertionArity, assertionOntologyResource);
		
		this.domainEntityResource = domainEntityResource;
		this.rangeEntityResource = rangeEntityResource;
	}
	
	@Override
	public boolean isEntityRelation() {
		return assertionOntologyResource.asProperty().isObjectProperty();
	}
	
	@Override
	public boolean isDataRelation() {
		return assertionOntologyResource.asProperty().isDatatypeProperty();
	}
	
	@Override
	public Resource getDomainEntityResource() {
		return domainEntityResource;
	}
	
	@Override
	public Resource getRangeEntityResource() {
		return rangeEntityResource;
	}
	
}
