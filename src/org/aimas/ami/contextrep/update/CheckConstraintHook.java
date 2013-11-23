package org.aimas.ami.contextrep.update;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;

public class CheckConstraintHook extends ContextUpdateHook {
	
	public CheckConstraintHook(OntResource contextAssertionResource) {
		super(contextAssertionResource);
	}
	
	@Override
	public boolean exec(Dataset contextStoreDataset) {
		// TODO Auto-generated method stub
		return true;
	}
}
