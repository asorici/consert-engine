package org.aimas.ami.contextrep.core.api;

public class QueryException extends Exception {
	
    private static final long serialVersionUID = 1L;

	public QueryException() {
	}
	
	public QueryException(String message) {
		super(message);
	}
	
	public QueryException(Throwable cause) {
		super(cause);
	}
	
	public QueryException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public QueryException(String message, Throwable cause,
	        boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
}
