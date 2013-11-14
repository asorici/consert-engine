package org.aimas.ami.contextrep.model.impl;

import org.aimas.ami.contextrep.model.ContextEntity;
import org.aimas.ami.contextrep.model.UnaryContextAssertion;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntResource;

public class UnaryContextAssertionImpl extends ContextAssertionImpl implements UnaryContextAssertion {
	
	private ContextEntity contextEntity;
	
	
	protected UnaryContextAssertionImpl(Node node, EnhGraph graph,
			ContextAssertionType assertionType, int assertionArity,
			OntResource assertionOntologyResource, ContextEntity contextEntity) {
		super(node, graph, assertionType, assertionArity, assertionOntologyResource);
		this.contextEntity = contextEntity;
	}


	@Override
	public ContextEntity getEntity() {
		return contextEntity;
	}


}
