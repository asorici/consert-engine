package org.aimas.ami.contextrep.model.impl;

import java.util.Map;

import org.aimas.ami.contextrep.model.ContextEntity;
import org.aimas.ami.contextrep.model.NaryContextAssertion;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class NaryContextAssertionImpl extends ContextAssertionImpl implements
        NaryContextAssertion {
	
	private Map<OntProperty, Resource> assertionRoleMap;
	
	public NaryContextAssertionImpl(ContextAssertionType assertionType,
	        int assertionArity, OntResource assertionOntologyResource, 
	        Map<OntProperty, Resource> assertionRoleMap) {
		super(assertionType, assertionArity, assertionOntologyResource);
		this.assertionRoleMap = assertionRoleMap;
	}
	
	@Override
	public Map<OntProperty, Resource> getAssertionRoles() {
		return assertionRoleMap;
	}
	
}
