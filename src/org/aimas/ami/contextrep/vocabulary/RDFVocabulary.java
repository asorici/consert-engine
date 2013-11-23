package org.aimas.ami.contextrep.vocabulary;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class RDFVocabulary {
	public static final String BASE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
	public static final String NS = BASE_URI + "#";
	
	public static final Property TYPE = ResourceFactory.createProperty(NS + "type");
}
