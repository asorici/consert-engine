package org.aimas.ami.contextrep.test.adhocmeeting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.aimas.ami.contextrep.update.ContextAssertionUpdateListener;
import org.aimas.ami.contextrep.update.ContextUpdateHook;
import org.aimas.ami.contextrep.update.ContextUpdateHookErrorListener;
import org.aimas.ami.contextrep.update.ContextUpdateHookExecutor;
import org.aimas.ami.contextrep.utils.ContextAssertionUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateRequest;

public abstract class ProducerEvent implements Comparable<ProducerEvent> {
	public static double DEFAULT_ACCURACY = 1.0;
	public static final String DEFAULT_SOURCE_URI = ScenarioInit.AD_HOC_MEETING_NS + "room-coordinator";
	
	/**
	 * timestamp of event creation
	 */
	protected Calendar timestamp;
	
	/**
	 * validity of the event in seconds
	 */
	protected int validity;
	
	/**
	 * update request to be executed
	 */
	protected UpdateRequest request;
	
	// protected Dataset dataset;
	protected OntModel contextModel;
	
	public ProducerEvent(OntModel contextModel, Calendar timestamp, int validity) {
		// this.dataset = dataset;
		this.contextModel = contextModel;
		this.timestamp = timestamp;
		this.validity = validity;
	}
	
	public void execute(Dataset dataset) {
		dataset.begin(ReadWrite.WRITE);
		
		List<ContextAssertionUpdateListener> registeredContextStoreListeners = 
			ContextAssertionUtils.registerContextAssertionStoreListeners(dataset);
		GraphStore graphStore = GraphStoreFactory.create(dataset);
		List<ContextUpdateHook> contextUpdateHooks = null;
		
		try {
			UpdateAction.execute(request, graphStore);
			//ContextAssertionUtils.executeContextUpdateHooks(dataset);
			
			contextUpdateHooks = ContextAssertionUtils.collectContextUpdateHooks(registeredContextStoreListeners);
			dataset.commit();
		}
		finally {
			dataset.end();
		}
		
		if (contextUpdateHooks != null) {
			List<ContextUpdateHookErrorListener> updateHookErrorListeners = new ArrayList<>();
			ContextAssertionUtils.executeContextUpdateHooks(contextUpdateHooks, updateHookErrorListeners);
		}
	}
	
	
	@Override
	public int compareTo(ProducerEvent other) {
		return timestamp.compareTo(other.timestamp);
	}
	
	protected abstract UpdateRequest createUpdateRequest();
}
