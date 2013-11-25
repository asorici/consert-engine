package org.aimas.ami.contextrep.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.topbraid.spin.util.CommandWrapper;

import com.hp.hpl.jena.ontology.OntResource;

public class ConstraintIndex {
	private Map<OntResource, List<CommandWrapper>> assertion2ConstraintMap;
	
	public ConstraintIndex() {
		assertion2ConstraintMap = new HashMap<>();
	}
	
}
