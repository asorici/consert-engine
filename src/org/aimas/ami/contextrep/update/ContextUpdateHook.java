package org.aimas.ami.contextrep.update;

import com.hp.hpl.jena.ontology.OntResource;

public interface ContextUpdateHook {
	public boolean exec();
	public OntResource getContextAssertionResource();
}
