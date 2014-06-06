package org.aimas.ami.contextrep.core.api;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;

public interface SubscribeResultNotifier {
	public void notifyResult(String subscriptionString, ResultSet result);
	
	public void notifyResult(Query subscriptionQuery, ResultSet result);
}
