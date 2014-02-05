package org.aimas.ami.contextrep.utils.spin;

import org.aimas.ami.contextrep.model.ContextAssertion;

import com.hp.hpl.jena.rdf.model.Resource;

public class ContextConstraintViolation {
	/*
	 * The context assertion for which the constraint is expressed
	 */
	private ContextAssertion constrainedAssertion;
	
	/*
	 * The resource identifying the direct query or template call that expresses the constraint
	 */
	private Resource constraintSource;
	
	public ContextConstraintViolation(ContextAssertion constrainedAssertion, Resource constraintSource) {
	    
		this.constrainedAssertion = constrainedAssertion;
	    this.constraintSource = constraintSource;
    }


	public ContextAssertion getConstrainedAssertion() {
		return constrainedAssertion;
	}


	public Resource getConstraintSource() {
		return constraintSource;
	}
}
