package org.aimas.ami.contextrep.core.api;

import org.aimas.ami.contextrep.query.QueryResult;

public interface QueryResultNotifier {
	public void notifyQueryResult(QueryResult result);
	
	public void notifyAskResult(QueryResult result);
}
