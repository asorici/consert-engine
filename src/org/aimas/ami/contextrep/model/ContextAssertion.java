package org.aimas.ami.contextrep.model;

import com.hp.hpl.jena.ontology.OntResource;

public interface ContextAssertion {
	public static enum ContextAssertionType {
		Static("http://pervasive.semanticweb.org/ont/2013/05/contextassertion#Static"), 
		Sensed("http://pervasive.semanticweb.org/ont/2013/05/contextassertion#Sensed"), 
		Profiled("http://pervasive.semanticweb.org/ont/2013/05/contextassertion#Profiled"), 
		Derived("http://pervasive.semanticweb.org/ont/2013/05/contextassertion#Derived") ;
		
		private String typeURI;
		
		ContextAssertionType(String typeURI) {
			this.typeURI = typeURI;
		}
		
		public String getTypeURI() {
			return typeURI;
		}
	}
	
	public static final int UNARY = 1;
	public static final int BINARY = 2;
	public static final int NARY = 3;
	
	/**
	 * Get the type of the ContextAssertion
	 * @return the type of the ContextAssertion
	 */
	public ContextAssertionType getAssertionType();
	
	/**
	 * Get the arity of this ContextAssertion
	 * @return 1 for UnaryContextAssertion, 2 for binary and 3 for n-ary
	 */
	public int getAssertionArity();
	
	/**
	 * Get the context domain ontology definition for this ContextAssertion 
	 * @return the <a>com.hp.hpl.jena.ontology.OntResource</a> that defines
	 * this instance of a ContextAssertion  
	 */
	public OntResource getOntologyResource();
	
	/**
	 * Get the URI of the named graph that stores annotation information about an instance of
	 * this ContextAssertion.
	 * @return The URI of the named graph that stores annotation information about an instance of
	 * this ContextAssertion.
	 */
	public String getAssertionStoreURI();
	
	
	public boolean isUnary();
	
	public boolean isBinary();
	
	public boolean isNary();
}