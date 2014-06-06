package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.rdf.model.Resource;

public class ContextConstraintViolation {
	public enum ContextConstraintType { Uniqueness, Value }
	
	/*
	 * The context assertion for which the constraint is expressed
	 */
	private ContextAssertion constrainedAssertion;
	
	/*
	 * The resource identifying the direct query or template call that expresses the constraint
	 */
	private Resource constraintSource;
	
	private ContextConstraintType constraintType;
	
	public ContextConstraintViolation(ContextAssertion constrainedAssertion, Resource constraintSource, ContextConstraintType constraintType) {
	    
		this.constrainedAssertion = constrainedAssertion;
	    this.constraintSource = constraintSource;
	    this.constraintType = constraintType;
    }


	public ContextAssertion getConstrainedAssertion() {
		return constrainedAssertion;
	}


	public Resource getConstraintSource() {
		return constraintSource;
	}
	
	
	public ContextConstraintType getType() {
		return constraintType;
	}
	
	public boolean isUniquenessConstraint() {
		return constraintType == ContextConstraintType.Uniqueness;
	}
	
	
	public boolean isValueConstraint() {
		return constraintType == ContextConstraintType.Value;
	}
}
