package org.aimas.ami.contextrep.update;

public class HookResult {
	long startTime;
	long duration;
	
	boolean error;

	public HookResult(long startTime, long duration, boolean error) {
	    this.startTime = startTime;
	    this.duration = duration;
	    this.error = error;
    }

	public long getStartTime() {
		return startTime;
	}

	public long getDuration() {
		return duration;
	}

	public boolean hasError() {
		return error;
	}
	
	public long getEnd() {
		return startTime + duration;
	}
}
