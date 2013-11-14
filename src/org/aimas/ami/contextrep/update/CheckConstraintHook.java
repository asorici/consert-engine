package org.aimas.ami.contextrep.update;

import com.hp.hpl.jena.ontology.OntResource;

public class CheckConstraintHook implements ContextUpdateHook {
	
	private OntResource contextAssertionResource;
	
	public CheckConstraintHook(OntResource contextAssertionResource) {
		this.contextAssertionResource = contextAssertionResource;
	}
	
	@Override
	public boolean exec() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
    public OntResource getContextAssertionResource() {
	    return contextAssertionResource;
    }
}
