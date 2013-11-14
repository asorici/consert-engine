package org.aimas.ami.contextrep.vocabulary;

import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class SPINVocabulary {
	public static final Property deriveAssertionRule = ResourceFactory.createProperty(SPIN.NS + "deriveassertion"); 
			
}
