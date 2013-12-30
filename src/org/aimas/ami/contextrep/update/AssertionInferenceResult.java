package org.aimas.ami.contextrep.update;

import java.util.List;

import org.aimas.ami.contextrep.model.ContextAssertion;

public class AssertionInferenceResult {
	private int referenceID;
	private long startTime;
	private long duration;
	
	private ContextAssertion assertion;
	private InferenceHookResult inferenceHookResult;
	
	public AssertionInferenceResult(int referenceID, long startTime, long duration, 
			ContextAssertion assertion, InferenceHookResult inferenceHookResult) {
	    this.referenceID = referenceID;
	    this.startTime = startTime;
	    this.duration = duration;
	    this.assertion = assertion;
	    this.inferenceHookResult = inferenceHookResult;
    }

	public int getReferenceID() {
		return referenceID;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getDuration() {
		return duration;
	}

	public ContextAssertion getAssertion() {
		return assertion;
	}

	public InferenceHookResult inferenceHookResult() {
		return inferenceHookResult;
	}
}
