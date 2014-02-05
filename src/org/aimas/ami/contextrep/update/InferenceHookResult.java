package org.aimas.ami.contextrep.update;

import java.util.List;

import org.aimas.ami.contextrep.model.ContextAssertion;

public class InferenceHookResult extends HookResult {
	private List<ContextAssertion> inferredAssertions;
	private boolean inferencePossible;
	private boolean inferred;
	
	public InferenceHookResult(long startTime, int duration, boolean error, 
			List<ContextAssertion> inferredAssertions, boolean inferencePossible, boolean inferred) {
	    super(startTime, duration, error);
	    
	    this.inferredAssertions = inferredAssertions;
	    this.inferencePossible = inferencePossible;
	    this.inferred = inferred;
    }

	public boolean hasInferencePossible() {
		return inferencePossible;
	}

	public boolean isInferred() {
		return inferred;
	}	
	
	public List<ContextAssertion> getInferredAssertions() {
		return inferredAssertions;
	}
}
