package org.aimas.ami.contextrep.utils;

public class IntervalFormatException extends Exception {

	private static final long serialVersionUID = 1L;

	
	public IntervalFormatException(String intervalStr) {
		super("String " + intervalStr + " does not respect [Calendar,Calendar] format");
	}

	public IntervalFormatException(String intervalStr, Throwable cause) {
		super("String " + intervalStr + " does not respect [Calendar,Calendar] format", cause);
	}
}
