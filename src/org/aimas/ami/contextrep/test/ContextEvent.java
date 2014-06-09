package org.aimas.ami.contextrep.test;

import java.util.Calendar;

import org.aimas.ami.contextrep.datatype.CalendarInterval;
import org.aimas.ami.contextrep.datatype.CalendarIntervalList;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.test.adhocmeeting.ScenarioInit;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

public abstract class ContextEvent implements Comparable<ContextEvent> {
	public static double DEFAULT_ACCURACY = 1.0;
	public static final String DEFAULT_SOURCE_URI = ScenarioInit.AD_HOC_MEETING_NS + "room-coordinator";
	
	/**
	 * timestamp of event creation
	 */
	private Calendar timestamp;

	/**
	 * validity of the event in seconds
	 */
	protected int validity;
	
	protected double accuracy;
	
	protected String assertedByURI;
	
	/**
	 * update request to be executed
	 */
	protected UpdateRequest request;
	
	// protected Dataset dataset;
	protected OntModel contextModel;
	
	public ContextEvent(OntModel contextModel, Calendar timestamp, int validity, 
			double accuracy, String assertedByURI) {
		// this.dataset = dataset;
		this.contextModel = contextModel;
		this.timestamp = timestamp;
		this.validity = validity;
		this.accuracy = accuracy;
		this.assertedByURI = assertedByURI;
	}
	
	public ContextEvent(OntModel contextModel, Calendar timestamp, int validity) {
		// this.dataset = dataset;
		this(contextModel, timestamp, validity, DEFAULT_ACCURACY, DEFAULT_SOURCE_URI);
	}
	
	
	@Override
	public int compareTo(ContextEvent other) {
		return getTimestamp().compareTo(other.getTimestamp());
	}
	
	protected abstract UpdateRequest createUpdateRequest();
	
	
	public UpdateRequest getUpdateRequest() {
		return request;
	}

	/**
	 * @return the timestamp
	 */
    public Calendar getTimestamp() {
	    return timestamp;
    }
    
    
    public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
		this.request = createUpdateRequest();
	}
    
    
    protected Update createAnnotationUpdate(Node graphURINode, ContextAssertion assertion) {
		Calendar validTo = Calendar.getInstance();
		validTo.setTimeInMillis(timestamp.getTimeInMillis() + validity * 1000);
		
		CalendarInterval interval = new CalendarInterval(timestamp, true, validTo, true);
		CalendarIntervalList intervalList = new CalendarIntervalList();
		intervalList.add(interval);
		
		// Create validity Literal
		Literal validityAnn = ResourceFactory.createTypedLiteral(intervalList);
		
		// Create timestamp Literal
		XSDDateTime xsdTimestamp = new XSDDateTime(timestamp);
		Literal timestampAnn = ResourceFactory.createTypedLiteral(xsdTimestamp);
		
		// Create accuracy Literal
		Literal accuracyAnn = ResourceFactory.createTypedLiteral(new Double(DEFAULT_ACCURACY));
		
		// Create source Literal
		Literal sourceAnn = ResourceFactory.createTypedLiteral(DEFAULT_SOURCE_URI, XSDDatatype.XSDanyURI);
		
		// create update quads
		Node storeURINode = Node.createURI(assertion.getAssertionStoreURI());
		OntProperty assertedBy = contextModel.getOntProperty(ConsertAnnotation.HAS_SOURCE.getURI());
		OntProperty hasTimestamp = contextModel.getOntProperty(ConsertAnnotation.HAS_TIMESTAMP.getURI());
		OntProperty validDuring = contextModel.getOntProperty(ConsertAnnotation.HAS_VALIDITY.getURI());
		OntProperty hasAccuracy = contextModel.getOntProperty(ConsertAnnotation.HAS_CERTAINTY.getURI());
		
		Quad q1 = Quad.create(storeURINode, graphURINode, assertedBy.asNode(), sourceAnn.asNode());
		Quad q2 = Quad.create(storeURINode, graphURINode, hasTimestamp.asNode(), timestampAnn.asNode());
		Quad q3 = Quad.create(storeURINode, graphURINode, validDuring.asNode(), validityAnn.asNode());
		Quad q4 = Quad.create(storeURINode, graphURINode, hasAccuracy.asNode(), accuracyAnn.asNode());
		
		QuadDataAcc data = new QuadDataAcc();
		data.addQuad(q1);
		data.addQuad(q2);
		data.addQuad(q3);
		data.addQuad(q4);
		
		Update annotationUpdate = new UpdateDataInsert(data);
		
		
		return annotationUpdate;
	}
}
