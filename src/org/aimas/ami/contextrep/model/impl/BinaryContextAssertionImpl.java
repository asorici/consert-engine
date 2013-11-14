package org.aimas.ami.contextrep.model.impl;

import org.aimas.ami.contextrep.model.BinaryContextAssertion;
import org.aimas.ami.contextrep.model.ContextEntity;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;

public class BinaryContextAssertionImpl extends ContextAssertionImpl implements BinaryContextAssertion {
	private ContextEntity subjectEntity;
	private ContextEntity objectEntity;
	private OntProperty assertionProperty;
	
	public BinaryContextAssertionImpl(Node node, EnhGraph graph,
			ContextAssertionType assertionType, int assertionArity,
			OntResource assertionOntologyResource, ContextEntity subjectEntity, ContextEntity objectEntity) {
		
		super(node, graph, assertionType, assertionArity, assertionOntologyResource);
		
		this.subjectEntity = subjectEntity;
		this.objectEntity = objectEntity;
		this.assertionProperty = assertionOntologyResource.asProperty();
	}

	@Override
	public boolean isEntityRelation() {
		return assertionProperty.isObjectProperty();
	}

	@Override
	public boolean isDataRelation() {
		return assertionProperty.isDatatypeProperty();
	}

	@Override
	public ContextEntity getSubjectEntity() {
		return subjectEntity;
	}

	@Override
	public ContextEntity getObjectEntity() {
		return objectEntity;
	}

}
