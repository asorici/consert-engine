package org.aimas.ami.contextrep.core;

import java.util.HashMap;
import java.util.Map;

import org.aimas.ami.contextrep.model.ConstraintsWrapper;
import org.aimas.ami.contextrep.model.ContextAssertion;

public class ConstraintIndex {
	private Map<ContextAssertion, ConstraintsWrapper> assertion2ConstraintMap;
	
	ConstraintIndex() {
		assertion2ConstraintMap = new HashMap<>();
	}
	
	public void addAssertionConstraint(ContextAssertion assertion, ConstraintsWrapper constraints) {
		assertion2ConstraintMap.put(assertion, constraints);
	}
	
	public ConstraintsWrapper getConstraints(ContextAssertion assertion) {
		return assertion2ConstraintMap.get(assertion);
	}
}
