package org.aimas.ami.contextrep.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.ContextAssertionIndex;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.update.ContextAssertionUpdateListener;
import org.aimas.ami.contextrep.update.ContextAssertionUpdateListener.ContextUpdateHookWrapper;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;

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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

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
		OntProperty assertedByProp = contextModel.getOntProperty(ConsertAnnotation.HAS_SOURCE.getURI());
		OntProperty hasTimestampProp = contextModel.getOntProperty(ConsertAnnotation.HAS_TIMESTAMP.getURI());
		OntProperty validDuringProp = contextModel.getOntProperty(ConsertAnnotation.HAS_VALIDITY.getURI());
		OntProperty hasAccuracyProp = contextModel.getOntProperty(ConsertAnnotation.HAS_CERTAINTY.getURI());
		
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
	
	
}
