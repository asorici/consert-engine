package org.aimas.ami.contextrep.test.adhocmeeting;

import java.util.Calendar;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.update.UpdateRequest;

public class StopEvent extends ProducerEvent {
	
	public StopEvent(OntModel contextModel, Calendar timestamp, int validity) {
		super(contextModel, timestamp, validity);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected UpdateRequest createUpdateRequest() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString() {
		return "StopEvent";
	}
}
