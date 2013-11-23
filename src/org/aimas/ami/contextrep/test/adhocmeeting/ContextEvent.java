package org.aimas.ami.contextrep.test.adhocmeeting;

import java.util.Calendar;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.update.UpdateRequest;

public abstract class ContextEvent implements Comparable<ContextEvent> {
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
	
	public ContextEvent(OntModel contextModel, Calendar timestamp, int validity) {
		// this.dataset = dataset;
		this.contextModel = contextModel;
		this.timestamp = timestamp;
		this.validity = validity;
	}
	
	@Override
	public int compareTo(ContextEvent other) {
		return timestamp.compareTo(other.timestamp);
	}
	
	protected abstract UpdateRequest createUpdateRequest();
	
	
	public UpdateRequest getUpdateRequest() {
		return request;
	}
}
