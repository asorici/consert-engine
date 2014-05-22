package org.aimas.ami.contextrep.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class ConsertFunctions {
	/** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    public final static String BASE_URI = "http://pervasive.semanticweb.org/ont/2014/05/consert/functions";
	public final static String NS = BASE_URI + "#";
	
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    public final static Resource CLOSE_ENOUGH_VALIDITY_TEMPLATE = m_model.createResource( NS + "CloseEnoughValidity" );
}
