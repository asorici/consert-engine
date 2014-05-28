package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.ontology.OntClass;

public interface StructuredAnnotation extends ContextAnnotation {
	
	/**
	 * Get the context ontology resource that defines the broader category of
	 * ContextAnnotations to which this particular structured annotation belongs 
	 * (following an owl:subClassOf property). The <i>ContextAnnotation categories</i>
	 * are defined by OWL classes which are direct subclasses of <code>contextannotation:StructuredAnnotation</code> 
	 * (the root class for all structure ContextAnnotations). 
	 * <p>
	 * As such, the annotations which are used at runtime, like for example, <code>contextannotation:NumericValueCertainty</code>,
	 * belong to an annotation category (in this case <code>contextannotation:CertaintyAnnotation</code>, since 
	 * <code>contextannotation:NumericValueCertainty</code> is a subclass of it).
	 * @return The context domain ontology resource from the annotation module which defines the annotation category. 
	 */
	public OntClass getAnnotationCategoryClass();
	
	/**
	 * Get the URI that represents the <i>meet</i> operator for this ContextAnnotation
	 * @return The URI that represents the <i>meet</i> operator for this ContextAnnotation
	 */
	public String getMeetOperatorURI();
	
	/**
	 * Get the URI that represents the <i>join</i> operator for this ContextAnnotation
	 * @return The URI that represents the <i>join</i> operator for this ContextAnnotation
	 */
	public String getJoinOperatorURI();
	
	/**
	 * Get the URI that identifies the function implementing the <i>permits continuity</i> functionality 
	 * during a <i>CheckContinuityHook</i> performed at runtime. 
	 * @return The URI that identifies the <i>permits continuity</i> function for this ContextAnnotation
	 */
	public String getPermitsContinuityFunctionURI();
	
	/**
	 * Get the URI of the data type that represents the owl:allValuesFrom restriction defined for instances
	 * of this ContextAnnotation.
	 * @return The URI that represents the data type owl:allValuesFrom restriction for instances of this ContextAnnotation 
	 */
	public String getValueRestrictionURI();
}
