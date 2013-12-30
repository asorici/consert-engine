package org.aimas.ami.contextrep.utils.spin;

import org.aimas.ami.contextrep.model.ContextAssertion;

import com.hp.hpl.jena.rdf.model.Resource;

public class ContextConstraintViolation {
	private ContextAssertion constrainedAssertion;
	private Resource constraintSource;
	
	private String newAssertionIdURI;
	private String conflictingAssertionIdURI;
	
	
	public ContextConstraintViolation(ContextAssertion constrainedAssertion,
            String newAssertionIdURI, String conflictingAssertionIdURI, Resource constraintSource) {
	    
		this.constrainedAssertion = constrainedAssertion;
	    this.constraintSource = constraintSource;
	    this.newAssertionIdURI = newAssertionIdURI;
	    this.conflictingAssertionIdURI = conflictingAssertionIdURI;
    }


	public ContextAssertion getConstrainedAssertion() {
		return constrainedAssertion;
	}


	public Resource getConstraintSource() {
		return constraintSource;
	}


	public String getNewAssertionIdURI() {
		return newAssertionIdURI;
	}


	public String getConflictingAssertionIdURI() {
		return conflictingAssertionIdURI;
	}
}
