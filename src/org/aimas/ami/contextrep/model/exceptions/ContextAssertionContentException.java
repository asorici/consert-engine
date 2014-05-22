package org.aimas.ami.contextrep.model.exceptions;

public class ContextAssertionContentException extends Exception {
	/**
	 * 
	 */
    private static final long serialVersionUID = 1L;

	public ContextAssertionContentException(Throwable cause) {
		super(cause);
	}
	
	public ContextAssertionContentException(String message) {
		super(message);
	}
	
	public ContextAssertionContentException(String message, Throwable cause) {
		super(message, cause);
	}
}
