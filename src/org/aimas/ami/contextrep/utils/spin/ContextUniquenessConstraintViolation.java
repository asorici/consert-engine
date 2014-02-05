package org.aimas.ami.contextrep.utils.spin;

import org.aimas.ami.contextrep.model.ContextAssertion;

import com.hp.hpl.jena.rdf.model.Resource;

public class ContextUniquenessConstraintViolation extends ContextConstraintViolation {
	
	private String newAssertionIdURI;
	private String conflictingAssertionIdURI;
	
	
	public ContextUniquenessConstraintViolation(ContextAssertion constrainedAssertion, Resource constraintSource, 
            String newAssertionIdURI, String conflictingAssertionIdURI) {
	    super(constrainedAssertion, constraintSource);
	    this.newAssertionIdURI = newAssertionIdURI;
	    this.conflictingAssertionIdURI = conflictingAssertionIdURI;
    }
    
	
	public String getNewAssertionIdURI() {
		return newAssertionIdURI;
	}


	public String getConflictingAssertionIdURI() {
		return conflictingAssertionIdURI;
	}
}
