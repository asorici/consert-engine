package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.rdf.model.Resource;

public class ContextValueConstraintViolation extends ContextConstraintViolation {
	
	private String assertionUUID;
	private Resource assertionViolationValue;
	private Resource annotationViolationValue;
	
	public ContextValueConstraintViolation(
            ContextAssertion constrainedAssertion, Resource constraintSource, 
            String assertionUUID, Resource assertionViolationValue, Resource annotationViolationValue) {
	    super(constrainedAssertion, constraintSource, ContextConstraintType.Value);
	    this.assertionUUID = assertionUUID;
	    this.assertionViolationValue = assertionViolationValue;
    }

	public String getAssertionUUID() {
		return assertionUUID;
	}
	
	public boolean hasAssertionViolationValue() {
		return assertionViolationValue != null;
	}
	
	public Resource getAssertionViolationValue() {
		return assertionViolationValue;
	}
	
	public boolean hasAnnotationViolationValue() {
		return annotationViolationValue != null;
	}
	
	public Resource getAnnotationViolationValue() {
		return annotationViolationValue;
	}
	
	
}
