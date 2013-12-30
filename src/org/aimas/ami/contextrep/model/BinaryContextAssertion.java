package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.rdf.model.Resource;

public interface BinaryContextAssertion extends ContextAssertion {
	/**
	 * Specifies whether this ContextAssertion binds two ContextEntities
	 * @return true if the ContextAssertion binds two ContextEntities 
	 * and false otherwise
	 */
	public boolean isEntityRelation();
	
	/**
	 * Specifies whether this ContextAssertion binds a ContextEntity and a Literal
	 * @return true if the ContextAssertion binds a ContextEntity and a Literal  
	 * and false otherwise
	 */
	public boolean isDataRelation();
	
	/**
	 * Get the ContextEntity that is the subject of this binary ContextAssertion
	 * @return the ContextEntity that is the subject of this binary ContextAssertion
	 */
	public Resource getDomainEntityResource();
	
	/**
	 * Get the ContextEntity that is the object of this binary ContextAssertion
	 * @return the ContextEntity that is the object of this binary ContextAssertion
	 */
	public Resource getRangeEntityResource();
}
