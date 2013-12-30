package org.aimas.ami.contextrep.test.adhocmeeting;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.test.ContextEvent;
import org.aimas.ami.contextrep.utils.CalendarInterval;
import org.aimas.ami.contextrep.utils.CalendarIntervalList;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

public class HasNoiseLevelEvent extends ContextEvent {
	private static final String LOCAL_NAME = "hasNoiseLevel";
	private static final String STORE_NAME = LOCAL_NAME + "Store";
	
	private Individual microphone;
	private int level;

	public HasNoiseLevelEvent(OntModel contextModel,
			Calendar timestamp, int validity, Individual microphone, int level) {
		super(contextModel, timestamp, validity);
		this.microphone = microphone;
		this.level = level;
		
		this.request = createUpdateRequest();
	}

	@Override
	public UpdateRequest createUpdateRequest() {
		UpdateRequest request = UpdateFactory.create() ;
		
		OntProperty assertionProperty = contextModel.getOntProperty(ScenarioInit.AD_HOC_MEETING_NS + LOCAL_NAME);
		Literal noiseLevel = ResourceFactory.createTypedLiteral(new Integer(level));
		
		Node graphURINode = Node.createURI(GraphUUIDGenerator.createUUID(assertionProperty));
		
		Quad q = Quad.create(graphURINode, microphone.asNode(), assertionProperty.asNode(), noiseLevel.asNode());
		
		QuadDataAcc data = new QuadDataAcc();
		data.addQuad(q);
		
		Update createUpdate = new UpdateCreate(graphURINode);
		Update assertionUpdate = new UpdateDataInsert(data);
		Update annotationUpdate = createAnnotationUpdate(graphURINode, getTimestamp(), validity);
		
		request.add(createUpdate);
		request.add(assertionUpdate);
		request.add(annotationUpdate);
		
		return request;
	}
	
	
	/**
	 * Create mock annotations for a ContextAssertion identified by <code>graphURINode</code>
	 * @param graphURINode Graph URI identifying the ContextAssertion
	 * @param timestamp Timestamp of the ContextAssertion
	 * @param validity Time validity in seconds
	 * @return an Update request inserting the mock annotations
	 */
	private Update createAnnotationUpdate(Node graphURINode, Calendar timestamp, int validity) {
		OntProperty assertionProperty = contextModel.getOntProperty(ScenarioInit.AD_HOC_MEETING_NS + LOCAL_NAME);
		
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
		Node storeURINode = Node.createURI(
				Config.getContextAssertionIndex().getAssertionFromResource(assertionProperty).getAssertionStoreURI());
		OntProperty assertedBy = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_SOURCE);
		OntProperty hasTimestamp = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_TIMESTAMP);
		OntProperty validDuring = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_VALIDITY);
		OntProperty hasAccuracy = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ANNOTATION_ACCURACY);
		
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
	
	@Override
	public String toString() {
		String response = "Event :: " + "hasNoiseLevel " + "mic: " + microphone + "," + "level: " + level; 
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		response += " timestamp: " + formatter.format(getTimestamp().getTime());
		return response;
	}
}
