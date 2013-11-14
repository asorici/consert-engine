package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.rdf.model.RDFNode;


public interface ContextEntity {
	
	public boolean isResource();
	
	public boolean isVariable();
	
	public boolean isLiteral();
	
	public RDFNode getWrappedNode();
}
