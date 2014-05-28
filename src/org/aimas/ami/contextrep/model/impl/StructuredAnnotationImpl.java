package org.aimas.ami.contextrep.model.impl;

import org.aimas.ami.contextrep.model.StructuredAnnotation;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;

public class StructuredAnnotationImpl extends ContextAnnotationImpl implements StructuredAnnotation {
	
	private OntClass annotationCategoryClass;
	
	// structure elements
	private String meetOperatorURI;
	private String joinOperatorURI;
	private String permitsContinuityFuncURI;
	private String valueRestrictionURI;
	
	public StructuredAnnotationImpl(ContextAnnotationType annotationType, 
			OntClass annotationOntClass, OntProperty bindingOntProperty,
			OntClass annCategoryClass, String meetOperatorURI, String joinOperatorURI, 
			String continuityFuncURI, String valueRestrictionURI) {
		super(annotationType, annotationOntClass, bindingOntProperty);
		
		this.annotationCategoryClass = annCategoryClass;
		this.meetOperatorURI = meetOperatorURI;
		this.joinOperatorURI = joinOperatorURI;
		this.permitsContinuityFuncURI = continuityFuncURI;
		this.valueRestrictionURI = valueRestrictionURI;
	}
	
	@Override
    public OntClass getAnnotationCategoryClass() {
	    return annotationCategoryClass;
    }
	
	@Override
	public String getMeetOperatorURI() {
		return meetOperatorURI;
	}
	
	@Override
	public String getJoinOperatorURI() {
		return joinOperatorURI;
	}
	
	@Override
	public String getPermitsContinuityFunctionURI() {
		return permitsContinuityFuncURI;
	}

	@Override
    public String getValueRestrictionURI() {
	    return valueRestrictionURI;
    }
	
}
