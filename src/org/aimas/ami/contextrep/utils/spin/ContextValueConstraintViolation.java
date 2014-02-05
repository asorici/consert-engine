package org.aimas.ami.contextrep.utils.spin;

import org.aimas.ami.contextrep.model.ContextAssertion;

import com.hp.hpl.jena.rdf.model.Resource;

public class ContextValueConstraintViolation extends ContextConstraintViolation {
	
	private String newAssertionIdURI;
	private Resource violationValue;
	
	public ContextValueConstraintViolation(
            ContextAssertion constrainedAssertion, Resource constraintSource, 
            String newAssertionIdURI, Resource violationValue) {
	    super(constrainedAssertion, constraintSource);
	    this.newAssertionIdURI = newAssertionIdURI;
	    this.violationValue = violationValue;
    }
	
}
