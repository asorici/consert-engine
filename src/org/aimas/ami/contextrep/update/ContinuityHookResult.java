package org.aimas.ami.contextrep.update;

public class ContinuityHookResult extends HookResult {
	
	private boolean hasContinuity;
	
	public ContinuityHookResult(long startTime, long duration, boolean error, boolean hasContinuity) {
	    super(startTime, duration, error);
	    this.hasContinuity = hasContinuity;
    }
	
	public boolean hasContinuity() {
		return hasContinuity;
	}
}
