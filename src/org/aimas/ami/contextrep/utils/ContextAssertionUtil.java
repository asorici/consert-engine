package org.aimas.ami.contextrep.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.core.ContextAssertionIndex;
import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.datatype.CalendarIntervalList;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.update.CheckInferenceHook;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.topbraid.spin.util.JenaUtil;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ContextAssertionUtil {
	/**
	 * Determine the type of a ContextAssertion (Static, Sensed, Profiled or Derived)
	 * @param resource The ontology resource defining the ContextAssertion.
	 * @param contextModel	The ontology model where the resource is defined.
	 * @return The {@link ContextAssertionType} instance giving the type of the 
	 * ContextAssertion defined by <i>resource</i> or null if no appropriate type can be found. 
	 */
	public static ContextAssertionType getType(OntResource resource, OntModel contextModel) {
		if (resource.isProperty()) {
			OntProperty property = resource.asProperty();
			Set<? extends OntProperty> supers = property.listSuperProperties(true).toSet();
			
			// if-else statements for each type of ContextAssertion
			if (supers.contains(ConsertCore.STATIC_RELATION_ASSERTION)
				|| supers.contains(ConsertCore.STATIC_DATA_ASSERTION)) {
				return ContextAssertionType.Static;
			}
			
			else if (supers.contains(ConsertCore.SENSED_RELATION_ASSERTION)
				|| supers.contains(ConsertCore.SENSED_DATA_ASSERTION)) {
				return ContextAssertionType.Sensed;
			}
			
			else if (supers.contains(ConsertCore.PROFILED_RELATION_ASSERTION)
				|| supers.contains(ConsertCore.PROFILED_DATA_ASSERTION)) {
				return ContextAssertionType.Profiled;
			}
			
			else if (supers.contains(ConsertCore.DERIVED_RELATION_ASSERTION)
				|| supers.contains(ConsertCore.DERIVED_DATA_ASSERTION)) {
				return ContextAssertionType.Derived;
			}
		}
		
		else if (resource.isClass() && resource.isURIResource()) {
			OntClass cls = resource.asClass();
			
			Iterator<OntClass> supers = cls.listSuperClasses(true);
			for(;supers.hasNext();) {
				OntClass sup = supers.next();
				if (sup.isRestriction()) {
					Restriction restriction = sup.asRestriction();
					if (restriction.isHasValueRestriction()) {
						HasValueRestriction hvr = restriction.asHasValueRestriction();
						if (hvr.onProperty(ConsertCore.CONTEXT_ASSERTION_TYPE_PROPERTY)) {
							
							if (hvr.hasValue(ConsertCore.TYPE_STATIC)) {
								return ContextAssertionType.Static;
							}
							
							else if (hvr.hasValue(ConsertCore.TYPE_SENSED)) {
								return ContextAssertionType.Sensed;
							}
							
							else if (hvr.hasValue(ConsertCore.TYPE_PROFILED)) {
								return ContextAssertionType.Profiled;
							}
							
							else if (hvr.hasValue(ConsertCore.TYPE_DERIVED)) {
								return ContextAssertionType.Derived;
							}
						}
					}
				}
			}
		}
		
		return ContextAssertionType.Static;
	}
	
	
	public static Resource getAssertionRoleEntity(OntModel contextModel, OntClass unaryAssertion,
			OntProperty assertionRoleProperty) {
		
		Iterator<OntClass> supers = unaryAssertion.listSuperClasses(true);
		for(;supers.hasNext();) {
			OntClass sup = supers.next();
			if (sup.isRestriction()) {
				Restriction restriction = sup.asRestriction();
				if (restriction.isAllValuesFromRestriction()) {
					AllValuesFromRestriction avfr = restriction.asAllValuesFromRestriction();
					if (avfr.onProperty(assertionRoleProperty)) {
						return avfr.getAllValuesFrom();
					}
				}
			}
		}
		
		return null;
	}
	
	
	public static List<Statement> createAnnotationStatements(String graphURI, OntModel contextModel, 
			ContextAssertionType assertionType, Calendar timestamp, CalendarIntervalList validity, 
			double accuracy, String sourceURI) {
		List<Statement> annotationStatements = new ArrayList<>();
		
		Resource idGraph = ResourceFactory.createResource(graphURI);
		
		OntProperty assertionTypeProp = contextModel.getOntProperty(ConsertCore.CONTEXT_ASSERTION_TYPE_PROPERTY.getURI());
		OntProperty hasSourceProp = contextModel.getOntProperty(ConsertAnnotation.HAS_SOURCE.getURI());
		OntProperty hasTimestampProp = contextModel.getOntProperty(ConsertAnnotation.HAS_TIMESTAMP.getURI());
		OntProperty hasValidityProp = contextModel.getOntProperty(ConsertAnnotation.HAS_VALIDITY.getURI());
		OntProperty hasCertaintyProp = contextModel.getOntProperty(ConsertAnnotation.HAS_CERTAINTY.getURI());
		
		// Create type statement
		Individual typeIndividual = contextModel.getIndividual(assertionType.getTypeURI());
		Statement typeStatement = ResourceFactory.createStatement(idGraph, assertionTypeProp, typeIndividual);
		annotationStatements.add(typeStatement);
		
		// Create validity statement
		Literal validityAnnVal = ResourceFactory.createTypedLiteral(validity);
		Resource validityAnn = ResourceFactory.createResource();
		Statement validityStatement = ResourceFactory.createStatement(idGraph, hasValidityProp, validityAnn);
		Statement validityTypeStatement = ResourceFactory.createStatement(validityAnn, RDF.type, ConsertAnnotation.TEMPORAL_VALIDITY);
		Statement validityValStatement = ResourceFactory.createStatement(validityAnn, ConsertAnnotation.HAS_STRUCTURED_VALUE, validityAnnVal);
		annotationStatements.add(validityStatement);
		annotationStatements.add(validityTypeStatement);
		annotationStatements.add(validityValStatement);
		
		// Create timestamp Literal
		XSDDateTime xsdTimestamp = new XSDDateTime(timestamp);
		Literal timestampValAnn = ResourceFactory.createTypedLiteral(xsdTimestamp);
		Resource timestampAnn = ResourceFactory.createResource();
		Statement timestampStatement = ResourceFactory.createStatement(idGraph, hasTimestampProp, timestampAnn);
		Statement timestampTypeStatement = ResourceFactory.createStatement(timestampAnn, RDF.type, ConsertAnnotation.DATETIME_TIMESTAMP);
		Statement timestampValStatement = ResourceFactory.createStatement(timestampAnn, ConsertAnnotation.HAS_STRUCTURED_VALUE, timestampValAnn);
		annotationStatements.add(timestampStatement);
		annotationStatements.add(timestampTypeStatement);
		annotationStatements.add(timestampValStatement);
				
		// Create certainty Literal
		Literal certaintyAnnVal = ResourceFactory.createTypedLiteral(new Double(accuracy));
		Resource certaintyAnn = ResourceFactory.createResource();
		Statement certaintyStatement = ResourceFactory.createStatement(idGraph, hasCertaintyProp, certaintyAnn);
		Statement certaintyTypeStatement = ResourceFactory.createStatement(certaintyAnn, RDF.type, ConsertAnnotation.NUMERIC_VALUE_CERTAINTY);
		Statement certaintyValStatement = ResourceFactory.createStatement(certaintyAnn, ConsertAnnotation.HAS_STRUCTURED_VALUE, certaintyAnnVal);
		annotationStatements.add(certaintyStatement);
		annotationStatements.add(certaintyTypeStatement);
		annotationStatements.add(certaintyValStatement);
		
		// Create source Literal
		Literal sourceAnnVal = ResourceFactory.createTypedLiteral(sourceURI, XSDDatatype.XSDanyURI);
		Resource sourceAnn = ResourceFactory.createResource();
		Statement sourceStatement = ResourceFactory.createStatement(idGraph, hasSourceProp, sourceAnn);
		Statement sourceTypeStatement = ResourceFactory.createStatement(sourceAnn, RDF.type, ConsertAnnotation.SOURCE_ANNOTATION);
		Statement sourceValStatement = ResourceFactory.createStatement(sourceAnn, ConsertAnnotation.HAS_UNSTRUCTURED_VALUE, sourceAnnVal);
		annotationStatements.add(sourceStatement);
		annotationStatements.add(sourceTypeStatement);
		annotationStatements.add(sourceValStatement);
		
		return annotationStatements;
	}
	
	/*
	public static List<ContextAssertionUpdateListener> registerContextAssertionStoreListeners(Dataset dataset) {
		List<ContextAssertionUpdateListener> registeredContextStoreListeners = new ArrayList<>();
		
		ContextAssertionIndex contextAssertionIndex = Engine.getContextAssertionIndex();
		Map<OntResource, ContextAssertion> assertionInfoMap = contextAssertionIndex.getAssertionInfoMap();
		
		for (OntResource res : assertionInfoMap.keySet()) {
			String assertionStoreURI = assertionInfoMap.get(res).getAssertionStoreURI();
			
			// set a listener for the named graph store corresponding to this
			// ContextAssertion
			Model model = dataset.getNamedModel(assertionStoreURI);
			if (model != null) {
				ContextAssertionUpdateListener listener = new ContextAssertionUpdateListener();
				registeredContextStoreListeners.add(listener);
				model.getGraph().getEventManager().register(listener);
			}
		}
		
		return registeredContextStoreListeners;
	}
	
	
	public static List<ContextAssertionUpdateListener> registerContextAssertionStoreListeners(List<Graph> assertionStores) {
		List<ContextAssertionUpdateListener> registeredContextStoreListeners = new ArrayList<>();
		
		for (Graph graph : assertionStores) {
			ContextAssertionUpdateListener listener = new ContextAssertionUpdateListener();
			registeredContextStoreListeners.add(listener);
			graph.getEventManager().register(listener);
		}
		
		return registeredContextStoreListeners;
	}
	
	
	public static void executeContextUpdateHooks(Dataset dataset) {
		ContextAssertionIndex contextAssertionIndex = Config.getContextAssertionIndex();
		Map<OntResource, String> assertion2StoreMap = contextAssertionIndex.getAssertion2StoreMap();
		
		for (OntResource res : assertion2StoreMap.keySet()) {
			String assertionStoreURI = assertion2StoreMap.get(res);
			
			// set a listener for the named graph store corresponding to this
			// ContextAssertion
			Model model = dataset.getNamedModel(assertionStoreURI);
			if (model != null) {
				Graph graph = model.getGraph();
				graph.getEventManager().notifyEvent(graph, ContextAssertionEvent.CONTEXT_ASSERTION_EXECUTE_HOOKS);
			}
		}
    }
	

	public static ContextUpdateHookWrapper collectContextUpdateHooks(
		List<ContextAssertionUpdateListener> registeredContextStoreListeners) {
		
		ContextUpdateHookWrapper combinedHookWrapper = null;
		
		for (ContextAssertionUpdateListener listener : registeredContextStoreListeners) {
			if (combinedHookWrapper == null) {
				combinedHookWrapper = listener.collectContextUpdateHooks();
			}
			else {
				combinedHookWrapper.extend(listener.collectContextUpdateHooks());
			}
		}
		
		return combinedHookWrapper;
    }
	*/
	
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
	 * Get the contents of a ContextAssertion that has been derived following the trigger of a Context Derivation Rule. 
	 * The content of the ContextAssertion instance we are looking for is initially hinted towards by a root <code>contentStatement</code> 
	 * the object of which is a ReificationStatement that describes the <i>seed</i> statement of the ContextAssertion instance. 
	 * This <i>seed</i> statement depends on the arity of the assertion and allows to retrieve its entire content.
	 * <p>
	 * This method is reserved for internal usage by the {@link CheckInferenceHook} mechanism during CONSERT Engine Derivation Rule reasoning.
	 * @param assertionResource The ontology resource that defines the type of this ContextAssertion
	 * @param contentStatement	The statement that describes the ContextAssertion instance whose contents we are trying to retrieve.
	 * @param contentModel	The model containing all the inferred triples as result of applying a Context Derivation Rule.
	 * @return The set of statements that make up the contents of the derived ContextAssertion instance.
	 * 		The method returns null, if the seed statement cannot be found or it does not correspond to the expected format.
	 */
	public static Set<Statement> getDerivedAssertionContents(OntResource assertionResource, Statement contentStatement, Model contentModel) {
		Set<Statement> assertionInstanceContents = null;
		
		// The contentStatement object is a reified statement describing the seed statement
		RDFNode seedStatementNode = contentStatement.getObject();
		if (!seedStatementNode.canAs(ReifiedStatement.class)) 
			return null;
		
		Statement seedEqualStatement = seedStatementNode.as(ReifiedStatement.class).getStatement();
		
		// Since the above transformation produces an .equals() statement from the reification we want to get the exact
		// statement in the contentModel so we can further use it to recover the rest of the ContextAssertion contents
		Statement seedStatement = contentModel.listStatements(new EqualsStatementSelector(seedEqualStatement)).next();
		
		// Now switch according to assertion arity
		ContextAssertion assertion = Engine.getContextAssertionIndex().getAssertionFromResource(assertionResource);
		if (assertion.isUnary()) {
			assertionInstanceContents = collectUnaryAssertionStatements(assertion, seedStatement, contentModel);
		}
		else if (assertion.isBinary()) {
			assertionInstanceContents = collectBinaryAssertionStatements(assertion, seedStatement, contentModel);
		}
		else {
			assertionInstanceContents = collectNaryAssertionStatements(assertion, seedStatement, contentModel);
		}
		
		return assertionInstanceContents;
	}
	
	
	private static Set<Statement> collectUnaryAssertionStatements(ContextAssertion derivedAssertion, Statement seedStatement, Model contentModel) {
	    // The seed statement for a unary assertion is the a triple of the form
		// _:bnode a <derivedAssertion>
		if (!seedStatement.getObject().isURIResource())
			return null;
		
		if (!seedStatement.getPredicate().equals(RDF.type) && !seedStatement.getResource().equals(derivedAssertion.getOntologyResource()))
			return null;
	    
		// In the case of the unary assertion we only need to select all the statements that have the same subject as that
		// of the seed statement
		Set<Statement> unaryAssertionContents = contentModel.listStatements(seedStatement.getSubject(), (Property)null, (RDFNode)null).toSet();
		
		return unaryAssertionContents;
    }
	
	
	private static Set<Statement> collectBinaryAssertionStatements(ContextAssertion derivedAssertion, Statement seedStatement, Model contentModel) {
		// The seed statement for a binary assertion is the a triple of the form
		// <some context entity> <derivedAssertion> <some context entity or literal>
		// It is currently in itself, the whole content of a binary assertion
		if (!seedStatement.getPredicate().equals(derivedAssertion.getOntologyResource()))
			return null;
		
		Set<Statement> binaryAssertionContents = new HashSet<Statement>();
		binaryAssertionContents.add(seedStatement);
		
		return binaryAssertionContents;
    }

	
	private static Set<Statement> collectNaryAssertionStatements(ContextAssertion derivedAssertion, Statement seedStatement, Model contentModel) {
		// The seed statement for an nary assertion is the a triple of the form
		// _:bnode a <derivedAssertion>
		if (!seedStatement.getObject().isURIResource())
			return null;
		
		if (!seedStatement.getPredicate().equals(RDF.type) && !seedStatement.getResource().equals(derivedAssertion.getOntologyResource()))
			return null;
		
		// As in the case of the unary assertion, we only need to select all the statements that have the same subject as that
		// of the seed statement (because unary and nary assertions are basically an extension of the reification mechanism)
		Set<Statement> naryAssertionContents = contentModel.listStatements(seedStatement.getSubject(), (Property)null, (RDFNode)null).toSet();
		return naryAssertionContents;
    }
	
	
	private static class EqualsStatementSelector implements Selector {
		
		private Statement statement;
		
		EqualsStatementSelector(Statement statement) {
	        this.statement = statement;
        }
		
		@Override
        public boolean test(Statement s) {
	        return statement.equals(s);
        }

		@Override
        public boolean isSimple() {
	        return false;
        }

		@Override
        public Resource getSubject() {
	        return null;
        }

		@Override
        public Property getPredicate() {
	        return null;
        }

		@Override
        public RDFNode getObject() {
	        return null;
        }
		
	}
}
