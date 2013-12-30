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
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

public class SenseSkeletonSittingEvent extends ContextEvent {
	private static final String LOCAL_NAME = "SensesSkelInPosition";
	private static final String STORE_NAME = LOCAL_NAME + "Store";
	
	private Individual camera;
	private Individual skeleton;
	
	public SenseSkeletonSittingEvent(OntModel contextModel, 
			Calendar timestamp, int validity, Individual camera, Individual skeleton) {
		super(contextModel, timestamp, validity);
		this.camera = camera;
		this.skeleton = skeleton;
		
		this.request = createUpdateRequest();
	}

	@Override
	public UpdateRequest createUpdateRequest() {
		UpdateRequest request = UpdateFactory.create() ;
		
		Resource bnode = ResourceFactory.createResource();
		OntClass assertionClass = contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + LOCAL_NAME);
		
		Node graphURINode = Node.createURI(GraphUUIDGenerator.createUUID(assertionClass));
		
		Individual sittingPosition = contextModel.getIndividual(ScenarioInit.AD_HOC_MEETING_NS + "sitting");
		OntProperty cameraRole = contextModel.getOntProperty(ScenarioInit.AD_HOC_MEETING_NS + "kinectCameraRole");
		OntProperty skeletonRole = contextModel.getOntProperty(ScenarioInit.AD_HOC_MEETING_NS + "skeletonRole");
		OntProperty skelPositionRole = contextModel.getOntProperty(ScenarioInit.AD_HOC_MEETING_NS + "skelPositionRole");
		
		Quad q1 = Quad.create(graphURINode, bnode.asNode(), RDF.type.asNode(), assertionClass.asNode());
		Quad q2 = Quad.create(graphURINode, bnode.asNode(), cameraRole.asNode(), camera.asNode());
		Quad q3 = Quad.create(graphURINode, bnode.asNode(), skeletonRole.asNode(), skeleton.asNode());
		Quad q4 = Quad.create(graphURINode, bnode.asNode(), skelPositionRole.asNode(), sittingPosition.asNode());
		
		QuadDataAcc data = new QuadDataAcc();
		data.addQuad(q1);
		data.addQuad(q2);
		data.addQuad(q3);
		data.addQuad(q4);
		
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
		OntClass assertionClass = contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + LOCAL_NAME);
		
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
			Config.getContextAssertionIndex().getAssertionFromResource(assertionClass).getAssertionStoreURI());
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
		String response = "Event :: " + "SensesSkeleton " + "camera: " + camera + "," + "skeleton: " + skeleton; 
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		response += " timestamp: " + formatter.format(getTimestamp().getTime());
		return response;
	}
}
