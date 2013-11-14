package org.aimas.ami.contextrep.model.impl;

import java.util.Map;

import org.aimas.ami.contextrep.model.ContextEntity;
import org.aimas.ami.contextrep.model.NaryContextAssertion;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;

public class NaryContextAssertionImpl extends ContextAssertionImpl implements NaryContextAssertion {
	private Map<OntProperty, ContextEntity> assertionRolesMap;
	
	public NaryContextAssertionImpl(Node node, EnhGraph graph,
			ContextAssertionType assertionType, int assertionArity,
			OntResource assertionOntologyResource, Map<OntProperty, ContextEntity> assertionRolesMap) {
		
		super(node, graph, assertionType, assertionArity, assertionOntologyResource);
		this.assertionRolesMap = assertionRolesMap;
		
	}

	@Override
	public Map<OntProperty, ContextEntity> getAssertionRoles() {
		return assertionRolesMap;
	}

}
