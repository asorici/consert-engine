package org.aimas.ami.contextrep.model;

import java.util.List;

import org.aimas.ami.contextrep.model.exceptions.ContextAssertionContentException;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionModelException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public interface ContextAssertion {
	public static enum ContextAssertionType {
		Static("http://pervasive.semanticweb.org/ont/2014/05/consert/core#Static"), 
		Sensed("http://pervasive.semanticweb.org/ont/2014/05/consert/core#Sensed"), 
		Profiled("http://pervasive.semanticweb.org/ont/2014/05/consert/core#Profiled"), 
		Derived("http://pervasive.semanticweb.org/ont/2014/05/consert/core#Derived") ;
		
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
	
	/**
	 * Get the list of statements that make up the content of the ContextAssertion instance identified by the
	 * named graph URI <code>assertionUUID</code> and stored in the <code>contextStoreDataset</code> {@link Dataset}.
	 * @param assertionUUID
	 * @param contextStoreDataset
	 * @return The list of statements that make up the content of a particular instance of a ContextAssertion
	 * in the context dataset.
	 */
	public List<Statement> getAssertionContent(Resource assertionUUID, Dataset contextStoreDataset);
	
	/**
	 * Produces the statements that represent the instantiation of the <code>ancestorAssertion</code> ContextAssertion
	 * from which the current ContextAssertion inherits. The produced statements are based on the contents of the
	 * current ContextAssertion. 
	 * 
	 * @param assertionUUID
	 * @param contextStoreDataset
	 * @param ancestorAssertion
	 * @return The statements that compose an instance of the <code>ancestorAssertion</code>.
	 * @throws ContextAssertionContentException
	 * @throws ContextAssertionModelException 
	 */
	public List<Statement> copyToAncestor(Resource assertionUUID, Dataset contextStoreDataset, 
		ContextAssertion ancestorAssertion, OntModel contextModel) 
		throws ContextAssertionContentException, ContextAssertionModelException;
	
	
	public boolean isUnary();
	
	public boolean isBinary();
	
	public boolean isNary();
}