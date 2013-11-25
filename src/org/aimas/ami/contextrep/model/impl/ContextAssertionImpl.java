package org.aimas.ami.contextrep.model.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertionInfo;
import org.aimas.ami.contextrep.model.ContextEntity;
import org.aimas.ami.contextrep.utils.ContextAssertionUtil;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;
import org.topbraid.spin.model.Element;
import org.topbraid.spin.model.NamedGraph;
import org.topbraid.spin.model.TriplePattern;
import org.topbraid.spin.model.impl.NamedGraphImpl;
import org.topbraid.spin.util.JenaUtil;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class ContextAssertionImpl extends NamedGraphImpl implements
		ContextAssertion {
	
	protected ContextAssertionType assertionType;
	protected int assertionArity;
	protected OntResource assertionOntologyResource;
	
	protected ContextAssertionImpl(Node node, EnhGraph graph, ContextAssertionType assertionType, 
			int assertionArity, OntResource assertionOntologyResource) {
		super(node, graph);
		this.assertionType = assertionType;
		this.assertionArity = assertionArity;
		this.assertionOntologyResource = assertionOntologyResource;
	}

	@Override
	public ContextAssertionType getAssertionType() {
		return assertionType;
	}

	@Override
	public int getAssertionArity() {
		return assertionArity;
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
	public ContextAssertionInfo getInfo() {
		return new ContextAssertionInfo(assertionType, assertionArity, assertionOntologyResource);
	}
	
	@Override
	public String toString() {
		String result = "{";
		result += "assertionType: " + assertionType + ", ";
		result += "assertionArity: " + assertionArity + ", ";
		result += "assertionOntologyResource: " + assertionOntologyResource;
		result += "}";
		
		return result;
	}
	
	/**
	 * Construct an appropriate instance of ContextAssertion from the given <code>namedGraph</code> instance and based on
	 * the ContextAssertion definitions available in the ontology model give by <code>assertionModel</code> 
	 * @param namedGraph The named graph instance which is supposed to contain the statements of a ContextAssertion
	 * @param assertionModel The ontology model which contains definitions for available ContextAssertions
	 * @return The appropriate instance of a ContextAssertion or null if the Named Graph does not contain statements
	 * that make up a ContextAssertion
	 */
	public static ContextAssertionImpl getFromNamedGraph(NamedGraph namedGraph, OntModel assertionModel) {
		List<Element> childElements = namedGraph.getElements();
		
		for (Element element : childElements) {
			if (element instanceof TriplePattern) {
				TriplePattern triple = element.as(TriplePattern.class);
				Resource property = triple.getPredicate();
				Resource object = triple.getObjectResource();
				
				// check if we have an assertion property
				if (property.isURIResource()) {
					OntProperty assertionProperty = assertionModel.getOntProperty(property.getURI());
					
					if (assertionProperty != null) {
						// get the entityRelationAssertion and entityDataAssertion properties
						OntProperty entityRelationAssertion = assertionModel.getOntProperty(ContextAssertionVocabulary.ENTITY_RELATION_ASSERTION);
						OntProperty entityDataAssertion = assertionModel.getOntProperty(ContextAssertionVocabulary.ENTITY_DATA_ASSERTION);
						Set<? extends OntProperty> supers = assertionProperty.listSuperProperties().toSet();
						
						if (supers.contains(entityRelationAssertion) || supers.contains(entityDataAssertion)) {
							return createBinaryContextAssertion(namedGraph, assertionModel, assertionProperty, triple);
						}
					}
					
					// check if we have an assertion class
					if (property.equals(RDF.type) && object != null && object.isURIResource()) {
						OntClass assertionClass = assertionModel.getOntClass(object.getURI());
						
						if (assertionClass != null) {
							OntClass unaryContextAssertion = assertionModel.getOntClass(ContextAssertionVocabulary.UNARY_CONTEXT_ASSERTION);
							OntClass naryContextAssertion = assertionModel.getOntClass(ContextAssertionVocabulary.NARY_CONTEXT_ASSERTION);
							Set<? extends OntClass> supers = assertionClass.listSuperClasses().toSet();
							
							if (supers.contains(unaryContextAssertion)) {
								return createUnaryContextAssertion(namedGraph, assertionModel, assertionClass, triple);
							}
							
							else if (supers.contains(naryContextAssertion)) {
								return createNaryContextAssertion(namedGraph, assertionModel, assertionClass);
							}
						}
					}
				}
				
			}
		}
		
		return null;
	}

	
	private static ContextAssertionImpl createUnaryContextAssertion( 
			NamedGraph namedGraph, OntModel assertionModel, OntClass assertionClass, 
			TriplePattern triple) {
		ContextEntity contextEntity = new ContextEntityImpl(triple.getSubject());
		ContextAssertionType assertionType = ContextAssertionUtil.getType(assertionClass, assertionModel);
		
		return new UnaryContextAssertionImpl(namedGraph.asNode(), (EnhGraph)namedGraph.getModel(), 
				assertionType, 1, assertionClass, contextEntity);
	}
	
	
	private static ContextAssertionImpl createBinaryContextAssertion(
			NamedGraph namedGraph, OntModel assertionModel, OntProperty assertionProperty, 
			TriplePattern triple) {
		
		ContextEntity subjectEntity = new ContextEntityImpl(triple.getSubject());
		ContextEntity objectEntity = new ContextEntityImpl(triple.getObject());
		
		ContextAssertionType assertionType = ContextAssertionUtil.getType(assertionProperty, assertionModel);
		return new BinaryContextAssertionImpl(namedGraph.asNode(), (EnhGraph)namedGraph.getModel(), 
				assertionType, 2, assertionProperty, subjectEntity, objectEntity);
	}
	
	
	private static ContextAssertionImpl createNaryContextAssertion(
			NamedGraph namedGraph, OntModel assertionModel, OntClass assertionClass) {
		
		ContextAssertionType assertionType = ContextAssertionUtil.getType(assertionClass, assertionModel);
		Map<OntProperty, ContextEntity> assertionRolesMap = new HashMap<OntProperty, ContextEntity>();
		
		List<Element> childElements = namedGraph.getElements();
		OntProperty assertionRoleProperty = assertionModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_ROLE);
		
		for (Element element : childElements) {
			if (element instanceof TriplePattern) {
				TriplePattern triple = element.as(TriplePattern.class);
				Resource predicate = triple.getPredicate();
				
				if (predicate.isURIResource()) {
					Property p = predicate.as(Property.class);
					if (JenaUtil.hasSuperProperty(p, assertionRoleProperty)) {
						ContextEntity contextEntity = new ContextEntityImpl(triple.getObject());
						assertionRolesMap.put(assertionModel.getOntProperty(p.getURI()), contextEntity);
					}
				}
			}
		}
		
		return new NaryContextAssertionImpl(namedGraph.asNode(), (EnhGraph)namedGraph.getModel(), 
				assertionType, 3, assertionClass, assertionRolesMap);
	}
	
}
