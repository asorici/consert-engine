package org.aimas.ami.contextrep.model;

import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;

public interface ContextAnnotation {
	public static enum ContextAnnotationType {
		Structured(ConsertAnnotation.STRUCTURED_ANNOTATION.getURI()), 
		Basic(ConsertAnnotation.BASIC_ANNOTATION.getURI()); 
		
		private String typeURI;
		
		ContextAnnotationType(String typeURI) {
			this.typeURI = typeURI;
		}
		
		public String getTypeURI() {
			return typeURI;
		}
	}
	
	/**
	 * Get the type of the ContextAnnotation (Structured or Unstructured)
	 * @return the type of the ContextAnnotation
	 */
	public ContextAnnotationType getAnnotationType();
	
	public boolean isStructured();
	
	/**
	 * Get the context domain ontology (annotation module) definition for this ContextAnnotation 
	 * @return The <a>com.hp.hpl.jena.ontology.OntResource</a> that defines
	 * this instance of a ContextAnnotation
	 */
	public OntResource getOntologyResource();
	
	/**
	 * Get the context domain ontology (annotation module) definition for the property that
	 * binds instances of this ContextAnnotation to the identifier named graph URI of a ContextAssertion
	 * @return The <a>com.hp.hpl.jena.ontology.OntProperty</a> that defines the binding annotation property.
	 * for instances of this ContextAnnotation
	 */
	public OntProperty getBindingProperty();
}
