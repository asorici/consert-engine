package org.aimas.ami.contextrep.model.impl;

import java.util.Iterator;

import org.aimas.ami.contextrep.model.ContextAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class ContextAnnotationImpl implements ContextAnnotation {
	
	protected ContextAnnotationType annotationType;
	protected OntClass annotationOntClass;
	protected OntProperty bindingOntProperty;
	
	
	public ContextAnnotationImpl(ContextAnnotationType annotationType, OntClass annotationOntClass, 
			OntProperty bindingOntProperty) {
	    this.annotationType = annotationType;
	    this.annotationOntClass = annotationOntClass;
	    this.bindingOntProperty = bindingOntProperty;
    }

	@Override
	public ContextAnnotationType getAnnotationType() {
		return annotationType;
	}
	
	@Override
	public boolean isStructured() {
		return annotationType == ContextAnnotationType.Structured;
	}
	
	@Override
	public OntResource getOntologyResource() {
		return annotationOntClass;
	}
	
	@Override
    public OntProperty getBindingProperty() {
	    return bindingOntProperty;
    }
	
	@Override
    public int hashCode() {
	    return annotationOntClass.hashCode();
    }

	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    ContextAnnotationImpl other = (ContextAnnotationImpl) obj;
	    if (annotationOntClass == null) {
		    if (other.annotationOntClass != null)
			    return false;
	    }
	    else if (!annotationOntClass.equals(other.annotationOntClass))
		    return false;
	    return true;
    }
	
	@Override
	public String toString() {
		String result = "{";
		result += "annotation class: " + annotationOntClass + ", ";
		result += "binding prop.: " + bindingOntProperty;
		result += "}";
		
		return result;
	}
	
	
	/**
	 * Return an array containing the 4 elements defining a structured annotation: meet operator,
	 * join operator, permits continuity function, structured value data type.
	 * The URIs are returned in this order.
	 * @param annotationClass The structured annotation ontology class for which to extract this information
	 * @param annotationModel The ontology model that holds the annotation definitions
	 * @return A String array containing the structured annotation element URIs in this order: meet operator,
	 * 		join operator, permits continuity function, structured value data type. 
	 * 		<p>
	 * 		The method return null if the <code>annotationClass</code> does not provide all the required elements. 
	 */
	public static String[] getStructureElementURIs(OntClass annotationClass, OntModel annotationModel) {
		String[] elementURIs = new String[4];
		
		Iterator<OntClass> supers = annotationClass.listSuperClasses(true);
		for(;supers.hasNext();) {
			OntClass sup = supers.next();
			if (sup.isRestriction()) {
				Restriction restriction = sup.asRestriction();
				if (restriction.isHasValueRestriction()) {
					HasValueRestriction valueRestriction = restriction.asHasValueRestriction();
					
					// check for meet operator
					if (valueRestriction.onProperty(ConsertAnnotation.HAS_MEET_OP)) {
						RDFNode value = valueRestriction.getHasValue();
						if (value.isURIResource()) {
							elementURIs[0] = value.asResource().getURI();
						}
					}
					// check for join operator
					else if (valueRestriction.onProperty(ConsertAnnotation.HAS_JOIN_OP)) {
						RDFNode value = valueRestriction.getHasValue();
						if (value.isURIResource()) {
							elementURIs[1] = value.asResource().getURI();
						}
					} 
					// check for permits continuity function 
					else if (valueRestriction.onProperty(ConsertAnnotation.HAS_CONTINUITY_FUNCTION)) {
						RDFNode value = valueRestriction.getHasValue();
						if (value.isURIResource()) {
							elementURIs[2] = value.asResource().getURI();
						}
					}
				}
				else if (restriction.isAllValuesFromRestriction()) {
					AllValuesFromRestriction avr = restriction.asAllValuesFromRestriction();
					if (avr.onProperty(ConsertAnnotation.HAS_STRUCTURED_VALUE)) {
						Resource fromRes = avr.getAllValuesFrom();
						if (fromRes.isURIResource()) {
							elementURIs[3] = fromRes.getURI();
						}
					}
				}
			}
		}
		
		// Inspect elementURIs to check for null values. If there are any, it means that not all
		// required elements were found, so we don't return anything (this is an all or nothing deal).
		
		for (String element : elementURIs) {
			if (element == null) { 
				return null;
			}
		}
		
		// we're all ok, so return the elements
		return elementURIs;
	}
}
