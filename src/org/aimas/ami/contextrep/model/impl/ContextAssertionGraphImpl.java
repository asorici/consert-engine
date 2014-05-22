package org.aimas.ami.contextrep.model.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.model.ContextAssertionGraph;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.topbraid.spin.model.Element;
import org.topbraid.spin.model.NamedGraph;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.TriplePattern;
import org.topbraid.spin.model.impl.NamedGraphImpl;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class ContextAssertionGraphImpl extends NamedGraphImpl implements
		ContextAssertionGraph {
	
	protected OntResource assertionOntologyResource;
	
	protected ContextAssertionGraphImpl(Node node, EnhGraph graph, OntResource assertionOntologyResource) {
		super(node, graph);
		this.assertionOntologyResource = assertionOntologyResource;
	}

	@Override
	public OntResource getAssertionResource() {
		return assertionOntologyResource;
	}
	
	@Override
	public Resource getAssertionIdentifier() {
		return getNameNode();
	}
	
	
	@Override
	public String toString() {
		return assertionOntologyResource.toString();
	}
	
	/**
	 * Construct an appropriate instance of ContextAssertionGraph from the given <code>namedGraph</code> instance and based on
	 * the ContextAssertion definitions available in the ontology model give by <code>assertionModel</code> 
	 * @param namedGraph The named graph instance which is supposed to contain the statements of a ContextAssertion
	 * @param assertionModel The ontology model which contains definitions for available ContextAssertions
	 * @param templateBindings A map containing mappings of possible variable resources contained in 
	 * the statements of the named graph
	 * @return The appropriate instance of a ContextAssertion or null if the Named Graph does not contain statements
	 * that make up a ContextAssertion
	 */
	public static ContextAssertionGraphImpl getFromNamedGraph(NamedGraph namedGraph, OntModel assertionModel, Map<String, RDFNode> templateBindings) {
		List<Element> childElements = namedGraph.getElements();
		
		for (Element element : childElements) {
			if (element instanceof TriplePattern) {
				TriplePattern triple = element.as(TriplePattern.class);
				RDFNode property = triple.getPredicate();
				RDFNode object = triple.getObjectResource();
				
				if (SPINFactory.isVariable(property)) {
					String varName = SPINFactory.asVariable(property).getName();
					if (templateBindings != null && templateBindings.get(varName) != null) { 
						property = templateBindings.get(varName);
					}
				}
				
				if (SPINFactory.isVariable(object)) {
					String varName = SPINFactory.asVariable(object).getName();;
					
					if (templateBindings != null && templateBindings.get(varName) != null) {
						object = templateBindings.get(varName);
					}
				}
				
				// check if we have an assertion property
				if (property.isURIResource()) {
					OntProperty assertionProperty = assertionModel.getOntProperty(property.asResource().getURI());
					
					if (assertionProperty != null) {
						// get the entityRelationAssertion and entityDataAssertion properties
						OntProperty entityRelationAssertion = assertionModel.getOntProperty(ConsertCore.ENTITY_RELATION_ASSERTION.getURI());
						OntProperty entityDataAssertion = assertionModel.getOntProperty(ConsertCore.ENTITY_DATA_ASSERTION.getURI());
						Set<? extends OntProperty> supers = assertionProperty.listSuperProperties().toSet();
						
						if (supers.contains(entityRelationAssertion) || supers.contains(entityDataAssertion)) {
							return new ContextAssertionGraphImpl(namedGraph.asNode(), (EnhGraph)namedGraph.getModel(), assertionProperty);
						}
					}
					
					// check if we have an assertion class
					if (property.equals(RDF.type) && object != null && object.isURIResource()) {
						OntClass assertionClass = assertionModel.getOntClass(object.asResource().getURI());
						
						if (assertionClass != null) {
							OntClass unaryContextAssertion = assertionModel.getOntClass(ConsertCore.UNARY_CONTEXT_ASSERTION.getURI());
							OntClass naryContextAssertion = assertionModel.getOntClass(ConsertCore.NARY_CONTEXT_ASSERTION.getURI());
							Set<? extends OntClass> supers = assertionClass.listSuperClasses().toSet();
							
							if (supers.contains(unaryContextAssertion) || supers.contains(naryContextAssertion)) {
								return new ContextAssertionGraphImpl(namedGraph.asNode(), (EnhGraph)namedGraph.getModel(), assertionClass);
							}
						}
					}
				}
				
			}
		}
		
		return null;
	}
}
