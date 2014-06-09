package org.aimas.ami.contextrep.core;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.contextrep.core.api.CommandException;
import org.aimas.ami.contextrep.core.api.CommandHandler;
import org.aimas.ami.contextrep.core.api.InsertResult;
import org.aimas.ami.contextrep.core.api.InsertionHandler;
import org.aimas.ami.contextrep.core.api.QueryHandler;
import org.aimas.ami.contextrep.core.api.QueryResultNotifier;
import org.aimas.ami.contextrep.core.api.StatsHandler;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.query.ContextQueryTask;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

public class EngineFrontend implements InsertionHandler, QueryHandler,
        CommandHandler, StatsHandler {
	
	public EngineFrontend() {
		// TODO Auto-generated constructor stub
	}
	
	// =============================== INSERT HANDLING =============================== //
	
	@Override
	public InsertResult insert(UpdateRequest insertionRequest) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public InsertResult insert(Update assertionContents,
	        Update assertionAnnotations) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	// =============================== QUERY HANDLING =============================== //

	@Override
	public void execQuery(Query query, QuerySolutionMap initialBindings, QueryResultNotifier notifier) {
		Engine.assertionQueryExecutor().submit(new ContextQueryTask(query, initialBindings, notifier));
	}
	
	@Override
	public void execAsk(Query askQuery, QuerySolutionMap initialBindings, QueryResultNotifier notifier) {
		Engine.assertionQueryExecutor().submit(new ContextQueryTask(askQuery, initialBindings, notifier));
	}
	
	@Override
	public void subscribe(Query subscribeQuery, QuerySolutionMap initialBindings, QueryResultNotifier notifier) {
		Engine.subscriptionMonitor().newSubscription(subscribeQuery, initialBindings, notifier);
	}
	
	
	// =============================== COMMAND HANDLING =============================== //

	@Override
	public ValidityReport triggerOntReasoning(ContextAssertion assertion)
	        throws CommandException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public ValidityReport triggerOntReasoning(OntResource assertionResource)
	        throws CommandException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void cleanRuntimeContextStore(ContextAssertion assertion,
	        Calendar timeThreshold) throws CommandException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void cleanRuntimeContextStore(OntResource assertionResource,
	        Calendar timeThreshold) throws CommandException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void cleanRuntimeContextStore(Calendar timeThreshold)
	        throws CommandException {
		// TODO Auto-generated method stub
		
	}
	
	
	// =============================== STATS HANDLING =============================== // 
	
	@Override
	public int updatesPerTimeUnit(ContextAssertion assertion, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int updatesPerTimeUnit(OntResource assertionResource, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int timeSinceLastUpdate(ContextAssertion assertion, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int timeSinceLastUpdate(OntResource assertionResource, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int requestsPerTimeUnit(ContextAssertion assertion, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int requestsPerTimeUnit(OntResource assertionResource, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int subscriptionCount(ContextAssertion assertion) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int subscriptionCount(OntResource assertionResource) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int timeSinceLastQuery(ContextAssertion assertion, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int timeSinceLastQuery(OntResource assertionResource, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int timeSinceReasoning(ContextAssertion assertion, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int timeSinceReasoning(OntResource assertionResource, TimeUnit measureUnit) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
