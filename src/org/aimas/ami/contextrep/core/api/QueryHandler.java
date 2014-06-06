package org.aimas.ami.contextrep.core.api;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;

public interface QueryHandler {
	
	/**
	 * Execute a one time query with initial bindings.
	 * @param queryString The query string.
	 * @param initialBindings A map of {variable: value} pairs for initial binding. May be null.
	 * @return The results of query execution as a {@link ResultSet}
	 * @throws QueryException
	 */
	public ResultSet execQuery(String queryString, QuerySolutionMap initialBindings) throws QueryException;
	
	
	/**
	 * Execute a one time query with initial bindings.
	 * @param query The Jena query object.
	 * @param initialBindings A map of {variable: value} pairs for initial binding. May be null.
	 * @return The results of query execution as a {@link ResultSet}
	 * @throws QueryException
	 */
	public ResultSet execQuery(Query query, QuerySolutionMap initialBindings) throws QueryException;
	
	
	/**
	 * Execute a one time ask query. 
	 * @param askString The ask query string.
	 * @param initialBindings A map of {variable: value} pairs for initial binding. May be null. 
	 * @return True if the ask query returned a response, false otherwise
	 * @throws QueryException
	 */
	public boolean execAsk(String askString, QuerySolutionMap initialBindings) throws QueryException;
	
	/**
	 * Execute a one time ask query. 
	 * @param askString The Jena ask query object.
	 * @param initialBindings A map of {variable: value} pairs for initial binding. May be null. 
	 * @return True if the ask query returned a response, false otherwise
	 * @throws QueryException
	 */
	public boolean execAsk(Query askQuery, QuerySolutionMap initialBindings) throws QueryException;
	
	
	/**
	 * Subscribe for results to the given query string. 
	 * @param subscribeString The subscription query string.
	 * @param initialBindings A map of {variable: value} pairs for initial binding. May be null.
	 * @param notifier A notifier callback used to announce subscription results.
	 * @throws QueryException
	 */
	public void subscribe(String subscribeString, QuerySolutionMap initialBindings, 
			SubscribeResultNotifier notifier) throws QueryException;
	
	/**
	 * Subscribe for results to the given query object. 
	 * @param subscribeQuery The Jena subscription query object.
	 * @param initialBindings A map of {variable: value} pairs for initial binding. May be null.
	 * @param notifier A notifier callback used to announce subscription results.
	 * @throws QueryException
	 */
	public boolean subscribe(Query subscribeQuery, QuerySolutionMap initialBindings, 
			SubscribeResultNotifier notifier) throws QueryException;
}
