package org.aimas.ami.contextrep.update;

public class HookResult {
	long startTime;
	int duration;
	
	boolean error;

	public HookResult(long startTime, int duration, boolean error) {
	    this.startTime = startTime;
	    this.duration = duration;
	    this.error = error;
    }

	public long getStartTime() {
		return startTime;
	}

	public int getDuration() {
		return duration;
	}

	public boolean hasError() {
		return error;
	}
	
	public long getEnd() {
		return startTime + duration;
	}
}
