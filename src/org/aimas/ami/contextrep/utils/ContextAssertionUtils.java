package org.aimas.ami.contextrep.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.ContextAssertionIndex;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.update.ContextAssertionEvent;
import org.aimas.ami.contextrep.update.ContextAssertionUpdateListener;
import org.aimas.ami.contextrep.update.ContextUpdateHook;
import org.aimas.ami.contextrep.update.ContextUpdateHookErrorListener;
import org.aimas.ami.contextrep.update.ContextUpdateHookExecutor;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Graph;
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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class ContextAssertionUtils {
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
			if (supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.STATIC_RELATION_ASSERTION))
				|| supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.STATIC_DATA_ASSERTION))) {
				return ContextAssertionType.Static;
			}
			
			else if (supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.SENSED_RELATION_ASSERTION))
				|| supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.SENSED_DATA_ASSERTION))) {
				return ContextAssertionType.Sensed;
			}
			
			else if (supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.PROFILED_RELATION_ASSERTION))
				|| supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.PROFILED_DATA_ASSERTION))) {
				return ContextAssertionType.Profiled;
			}
			
			else if (supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.DERIVED_RELATION_ASSERTION))
				|| supers.contains(contextModel.getOntProperty(ContextAssertionVocabulary.DERIVED_DATA_ASSERTION))) {
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
						if (hvr.onProperty(contextModel.getProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_TYPE_PROPERTY))) {
							
							if (hvr.hasValue(contextModel.getIndividual(ContextAssertionVocabulary.TYPE_STATIC))) {
								return ContextAssertionType.Static;
							}
							
							else if (hvr.hasValue(contextModel.getIndividual(ContextAssertionVocabulary.TYPE_SENSED))) {
								return ContextAssertionType.Sensed;
							}
							
							else if (hvr.hasValue(contextModel.getIndividual(ContextAssertionVocabulary.TYPE_PROFILED))) {
								return ContextAssertionType.Profiled;
							}
							
							else if (hvr.hasValue(contextModel.getIndividual(ContextAssertionVocabulary.TYPE_DERIVED))) {
								return ContextAssertionType.Derived;
							}
						}
					}
				}
			}
		}
		
		return ContextAssertionType.Static;
	}
	
	
	public static List<Statement> createAnnotationStatements(String graphURI, OntModel contextModel, 
			ContextAssertionType assertionType, Calendar timestamp, CalendarIntervalList validity, 
			double accuracy, String sourceURI) {
		List<Statement> annotationStatements = new ArrayList<>();
		
		Resource idGraph = ResourceFactory.createResource(graphURI);
		
		OntProperty assertionTypeProp = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_TYPE_PROPERTY);
		OntProperty assertedByProp = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_SOURCE);
		OntProperty hasTimestampProp = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_TIMESTAMP);
		OntProperty validDuringProp = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_VALIDITY);
		OntProperty hasAccuracyProp = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_ACCURACY);
		
		// Create type statement
		Individual typeIndividual = contextModel.getIndividual(assertionType.getTypeURI());
		Statement typeStatement = ResourceFactory.createStatement(idGraph, assertionTypeProp, typeIndividual);
		annotationStatements.add(typeStatement);
		
		// Create validity statement
		Literal validityAnn = ResourceFactory.createTypedLiteral(validity);
		Statement validityStatement = ResourceFactory.createStatement(idGraph, validDuringProp, validityAnn);
		annotationStatements.add(validityStatement);
		
		// Create timestamp Literal
		XSDDateTime xsdTimestamp = new XSDDateTime(timestamp);
		Literal timestampAnn = ResourceFactory.createTypedLiteral(xsdTimestamp);
		Statement timestampStatement = ResourceFactory.createStatement(idGraph, hasTimestampProp, timestampAnn);
		annotationStatements.add(timestampStatement);
				
		// Create accuracy Literal
		Literal accuracyAnn = ResourceFactory.createTypedLiteral(new Double(accuracy));
		Statement accuracyStatement = ResourceFactory.createStatement(idGraph, hasAccuracyProp, accuracyAnn);
		annotationStatements.add(accuracyStatement);
		
		// Create source Literal
		Literal sourceAnn = ResourceFactory.createTypedLiteral(sourceURI, XSDDatatype.XSDanyURI);
		Statement sourceStatement = ResourceFactory.createStatement(idGraph, assertedByProp, sourceAnn);
		annotationStatements.add(sourceStatement);
		
		return annotationStatements;
	}
	
	
	public static List<ContextAssertionUpdateListener> registerContextAssertionStoreListeners(Dataset dataset) {
		List<ContextAssertionUpdateListener> registeredContextStoreListeners = new ArrayList<>();
		
		ContextAssertionIndex contextAssertionIndex = Config.getContextAssertionIndex();
		Map<OntResource, String> assertion2StoreMap = contextAssertionIndex.getAssertion2StoreMap();
		
		for (OntResource res : assertion2StoreMap.keySet()) {
			String assertionStoreURI = assertion2StoreMap.get(res);
			
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

	/*
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
	*/

	public static List<ContextUpdateHook> collectContextUpdateHooks(
			List<ContextAssertionUpdateListener> registeredContextStoreListeners) {
		
		List<ContextUpdateHook> contextUpdateHooks = new ArrayList<>();
		
		for (ContextAssertionUpdateListener listener : registeredContextStoreListeners) {
			List<ContextUpdateHook> updateHooks = listener.collectContextUpdateHooks();
			if (updateHooks != null) {
				contextUpdateHooks.addAll(updateHooks);
			}
		}
		
		return contextUpdateHooks;
    }
	
	
	public static void executeContextUpdateHooks(List<ContextUpdateHook> contextUpdateHooks, 
			List<ContextUpdateHookErrorListener> updateHookErrorListeners) {
		
		ContextUpdateHookExecutor updateHookExecutor = new ContextUpdateHookExecutor(contextUpdateHooks);
		updateHookExecutor.registerContextUpdateErrorListeners(updateHookErrorListeners);
		updateHookExecutor.start();
	}
}
