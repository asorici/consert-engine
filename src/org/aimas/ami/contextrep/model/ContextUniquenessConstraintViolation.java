package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.rdf.model.Resource;

public class ContextUniquenessConstraintViolation extends ContextConstraintViolation {
	
	private String[] conflictingAssertionUUIDs;
	
	
	public ContextUniquenessConstraintViolation(ContextAssertion constrainedAssertion, Resource constraintSource, 
            String conflictingAssertionUUID1, String conflictingAssertionUUID2) {
	    super(constrainedAssertion, constraintSource, ContextConstraintType.Uniqueness);
	    conflictingAssertionUUIDs = new String[2];
	    conflictingAssertionUUIDs[0] = conflictingAssertionUUID1;
	    conflictingAssertionUUIDs[1] = conflictingAssertionUUID2;
    }
    
	public String[] getConflictingAssertionUUIDs() {
		return conflictingAssertionUUIDs;
	}
	
}
