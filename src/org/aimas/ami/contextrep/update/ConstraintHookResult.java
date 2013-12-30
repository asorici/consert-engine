package org.aimas.ami.contextrep.update;

public class ConstraintHookResult extends HookResult {
	
	private boolean hasConstraint;
	private boolean hasViolation;
	
	public ConstraintHookResult(long startTime, long duration, boolean error, 
			boolean hasConstraint, boolean hasViolation) {
	    super(startTime, duration, error);
	    this.hasConstraint = hasConstraint;
	    this.hasViolation = hasViolation;
    }

	public boolean hasConstraint() {
		return hasConstraint;
	}
	
	public boolean hasViolation() {
		return hasViolation;
	}
	
}
