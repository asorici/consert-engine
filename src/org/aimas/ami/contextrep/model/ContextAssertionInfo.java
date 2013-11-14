package org.aimas.ami.contextrep.model;

import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;

import com.hp.hpl.jena.ontology.OntResource;

public class ContextAssertionInfo {
	
	private ContextAssertionType assertionType;
	private int assertionArity;
	private OntResource assertionOntologyResource;
	
	public ContextAssertionInfo(ContextAssertionType assertionType, 
			int assertionArity, OntResource assertionOntologyResource) {
		
		this.assertionType = assertionType;
		this.assertionArity = assertionArity;
		this.assertionOntologyResource = assertionOntologyResource;
	}

	public ContextAssertionType getAssertionType() {
		return assertionType;
	}

	public int getAssertionArity() {
		return assertionArity;
	}

	public OntResource getAssertionOntologyResource() {
		return assertionOntologyResource;
	}
	
	@Override
	public int hashCode() {
		return assertionOntologyResource.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ContextAssertionInfo) && 
			((ContextAssertionInfo)obj).getAssertionOntologyResource().equals(assertionOntologyResource);
	}
}
