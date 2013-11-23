package org.aimas.ami.contextrep.update;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;

public abstract class ContextUpdateHook {
	protected OntResource contextAssertionResource;
	
	
	public ContextUpdateHook(OntResource contextAssertionResource) {
		this.contextAssertionResource = contextAssertionResource;
	}
	
	public OntResource getContextAssertionResource() {
		return contextAssertionResource;
	}
	
	public abstract boolean exec(Dataset contextStoreDataset);
}
