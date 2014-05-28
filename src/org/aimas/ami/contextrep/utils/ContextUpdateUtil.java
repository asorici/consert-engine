package org.aimas.ami.contextrep.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.topbraid.spin.util.JenaUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.UpdateData;
import com.hp.hpl.jena.sparql.modify.request.UpdateDeleteWhere;
import com.hp.hpl.jena.sparql.modify.request.UpdateModify;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ContextUpdateUtil {
	/**
	 * Gets all graph nodes that are potentially updated in a given Update request.
	 * @param update  the Update (UpdateData, UpdateModify and UpdateDeleteWhere are supported)
	 * @param dataset  the Dataset to get the Graphs from
	 * @return the graphs nodes
	 */
	public static Collection<Node> getUpdatedGraphs(Update update, Dataset dataset, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		Set<Node> results = new HashSet<Node>();
		
		if (update instanceof UpdateData) {
			addUpdatedGraphs(results, (UpdateData)update, dataset, templateBindings, storesOnly);
		}
		else if(update instanceof UpdateModify) {
			addUpdatedGraphs(results, (UpdateModify)update, dataset, templateBindings, storesOnly);
		}
		else if(update instanceof UpdateDeleteWhere) {
			addUpdatedGraphs(results, (UpdateDeleteWhere)update, dataset, templateBindings, storesOnly);
		}
		return results;
	}

	
	private static void addUpdatedGraphs(Set<Node> results, UpdateData update, Dataset dataset, 
			Map<String, RDFNode> templateBindings, boolean storesOnly) {
		addUpdatedGraphs(results, update.getQuads(), dataset, templateBindings, storesOnly);
    }


	private static void addUpdatedGraphs(Set<Node> results, UpdateDeleteWhere update, 
			Dataset dataset, Map<String,RDFNode> templateBindings, boolean storesOnly) {
		addUpdatedGraphs(results, update.getQuads(), dataset, templateBindings, storesOnly);
	}
	
	
	private static void addUpdatedGraphs(Set<Node> results, UpdateModify update, Dataset dataset, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		Node withIRI = update.getWithIRI();
		if(withIRI != null) {
			results.add(withIRI);
		}
		addUpdatedGraphs(results, update.getDeleteQuads(), dataset, templateBindings, storesOnly);
		addUpdatedGraphs(results, update.getInsertQuads(), dataset, templateBindings, storesOnly);
	}

	
	private static void addUpdatedGraphs(Set<Node> results, Iterable<Quad> quads, Dataset graphStore, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		for(Quad quad : quads) {
			if(quad.isDefaultGraph()) {
				results.add(quad.getGraph());
			}
			else if(quad.getGraph().isVariable()) {
				if(templateBindings != null) {
					String varName = quad.getGraph().getName();
					RDFNode binding = templateBindings.get(varName);
					if(binding != null && binding.isURIResource()) {
						if (storesOnly && Engine.getContextAssertionIndex().isContextStore(binding.asNode())) {
							results.add(binding.asNode());
						}
						else if (!storesOnly) {
							results.add(binding.asNode());
						}
					}
				}
			}
			else {
				if (storesOnly && Engine.getContextAssertionIndex().isContextStore(quad.getGraph())) {
					results.add(quad.getGraph());
				}
				else if (!storesOnly) {
					results.add(quad.getGraph());
				}
			}
		}
	}
	
	/**
	 * Returns the {@link ContextAssertion} that is a direct parent of the <code>contextAssertion</code> 
	 * given as input, within the <code>contextModel</code>. As per the modeling recommendations of a 
	 * CONSERT-based context model, a <i>ContextAssertion</i> can have at most a parent assertion. 
	 * The method returns <code>null</code> if the input <code>contextAssertion</code> has no parent to inherit from.
	 * 
	 * @param contextAssertion
	 * @param contextModel
	 * @return the {@link ContextAssertion} that is a direct parent or null if no parent exists
	 */
	public static ContextAssertion getContextAssertionParent(ContextAssertion contextAssertion, OntModel contextModel) {
		OntResource assertionRes = contextAssertion.getOntologyResource();
		Resource directAssertionParent = null;
		
		// we must make the distinction between assertion arity
		if (contextAssertion.isUnary()) {
			Collection<Resource> directAssertionParents = JenaUtil.getSuperClasses(assertionRes);
			for (Resource res : directAssertionParents) {
				if (res.isURIResource() && !res.equals(ConsertCore.UNARY_CONTEXT_ASSERTION)) {
					directAssertionParent = res;
					break;
				}
			}
		}
		else if (contextAssertion.isBinary()) {
			directAssertionParent = JenaUtil.getResourceProperty(assertionRes, RDFS.subPropertyOf);
			if (ConsertCore.ROOT_BINARY_RELATION_ASSERTION_SET.contains(directAssertionParent) 
				|| ConsertCore.ROOT_BINARY_DATA_ASSERTION_SET.contains(directAssertionParent)) {
				directAssertionParent = null;
			}
		}
		else {
			Collection<Resource> directAssertionParents = JenaUtil.getSuperClasses(assertionRes);
			for (Resource res : directAssertionParents) {
				if (res.isURIResource() && !res.equals(ConsertCore.NARY_CONTEXT_ASSERTION)) {
					directAssertionParent = res;
					break;
				}
			}
		}
		
		if (directAssertionParent != null) {
			OntResource assertionOntRes = contextModel.getOntResource(directAssertionParent);
			return Engine.getContextAssertionIndex().getAssertionFromResource(assertionOntRes);
		}
		
		return null;
	}
	
	/**
	 * Returns the entire chain of {@link ContextAssertion} ancestor assertions up to the base ontology resources that
	 * define a <i>ContextAssertion</i> in the CONSERT ontology. 
	 * @param contextAssertion
	 * @param contextModel
	 * @return The list of {@link ContextAssertion} ancestor assertions from nearest to farthest.
	 */
	public static List<ContextAssertion> getContextAssertionAncestors(ContextAssertion contextAssertion, OntModel contextModel) {
		List<ContextAssertion> assertionAncestorList = new ArrayList<ContextAssertion>();
		ContextAssertion currentAssertion = contextAssertion;
		
		while( (currentAssertion = getContextAssertionParent(currentAssertion, contextModel)) != null ) {
			assertionAncestorList.add(currentAssertion);
		}
		
		return assertionAncestorList;
	}
	
	/**
	 * Get all the annotations of a <i>ContextAssertion</i> instance identified by <code>assertionUUID</code>, 
	 * grouped by the Statements which bind the <i>ContextAssertion</i> to its <i>ContextAnnotations</i>.
	 * @param contextAssertion
	 * @param assertionUUID
	 * @param contextModel
	 * @param contextStoreDataset
	 * @return A mapping from the <i>ContextAnnotation</i> {@link Statement} to the list of statements that define the annotations. 
	 */
	public static Map<Statement, Set<Statement>> getAnnotationsFor(ContextAssertion contextAssertion, Resource assertionUUID, 
			OntModel contextModel, Dataset contextStoreDataset) {
		Map<Statement, Set<Statement>> annotationsMap = new HashMap<Statement, Set<Statement>>();
		
		// get the all annotation properties defined in the Context Model
		OntProperty hasAnnotation = contextModel.getOntProperty(ConsertAnnotation.HAS_ANNOTATION.getURI());
		Set<? extends OntProperty> annotationProperties = hasAnnotation.listSubProperties(true).toSet();
		
		// get the assertion store for the ContextAssertion
		String assertionStoreURI = contextAssertion.getAssertionStoreURI();
		Model assertionStoreModel = contextStoreDataset.getNamedModel(assertionStoreURI);
		
		for(OntProperty annProp : annotationProperties) {
			// by definition there can be only one instance of each annotation type attached to an assertion
			Statement annStatement = assertionStoreModel.getProperty(assertionUUID, annProp);
			Resource annResource = annStatement.getResource();
			
			// get all statement within the assertion store that start out from the annotation resource
			Set<Statement> collectedAnnotationStatements = new HashSet<Statement>();
			Set<Resource> reached = new HashSet<Resource>();
			collectAnnotationStatements(annResource, assertionStoreModel, collectedAnnotationStatements, reached);
			
			annotationsMap.put(annStatement, collectedAnnotationStatements);
		}
		
		return annotationsMap;
	}
	
	
	private static void collectAnnotationStatements(Resource annRelatedsubject, Model assertionStoreModel, 
			Set<Statement> collectedAnnotationStatements, Set<Resource> reached) {
		
		reached.add(annRelatedsubject);
		StmtIterator statementIt = assertionStoreModel.listStatements(annRelatedsubject, (Property)null, (RDFNode)null);
		
		Set<Statement> statements = statementIt.toSet();
		collectedAnnotationStatements.addAll(statements);
		
		/* Walk through statements and recurse through those that have a resource object, 
		   if that object has not already been visited */ 
		for (Statement s : statements) {
			RDFNode obj = s.getObject();
			if (obj.isResource()) {
				Resource objRes = obj.asResource();
				if (!reached.contains(objRes)) {
					collectAnnotationStatements(objRes, assertionStoreModel, collectedAnnotationStatements, reached);
				}
			}
		}
	}
}
