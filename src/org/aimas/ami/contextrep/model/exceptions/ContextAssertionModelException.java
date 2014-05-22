package org.aimas.ami.contextrep.model.exceptions;

public class ContextAssertionModelException extends Exception {
	/**
	 * 
	 */
    private static final long serialVersionUID = 1L;

	public ContextAssertionModelException(Throwable cause) {
		super(cause);
	}
	
	public ContextAssertionModelException(String message) {
		super(message);
	}
	
	public ContextAssertionModelException(String message, Throwable cause) {
		super(message, cause);
	}
}
