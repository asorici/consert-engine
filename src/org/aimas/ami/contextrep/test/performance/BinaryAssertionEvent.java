package org.aimas.ami.contextrep.test.performance;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.aimas.ami.contextrep.model.BinaryContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.model.impl.BinaryContextAssertionImpl;
import org.aimas.ami.contextrep.test.ContextEvent;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

public class BinaryAssertionEvent extends ContextEvent {
	BinaryContextAssertion assertion;
	
	public BinaryAssertionEvent(OntModel contextModel, 
			OntProperty assertionProperty, Resource domain, Resource range,
			Calendar timestamp, int validity, double accuracy, String assertedByURI) {
		super(contextModel, timestamp, validity, accuracy, assertedByURI);
		
		this.assertion = new BinaryContextAssertionImpl(ContextAssertionType.Sensed, 2, assertionProperty, domain, range);
		this.request = createUpdateRequest();
	}
	
	@Override
	protected UpdateRequest createUpdateRequest() {
		UpdateRequest request = UpdateFactory.create();
		
		Node graphURINode = Node.createURI(GraphUUIDGenerator.createUUID(assertion.getOntologyResource()));
		Quad q = Quad.create(graphURINode, assertion.getDomainEntityResource().asNode(), 
				assertion.getOntologyResource().asNode(), assertion.getRangeEntityResource().asNode());
		
		QuadDataAcc data = new QuadDataAcc();
		data.addQuad(q);
		
		Update createUpdate = new UpdateCreate(graphURINode);
		Update assertionUpdate = new UpdateDataInsert(data);
		Update annotationUpdate = createAnnotationUpdate(graphURINode, assertion);
		
		request.add(createUpdate);
		request.add(assertionUpdate);
		request.add(annotationUpdate);
		
		return request;
	}
	
	
	@Override
	public String toString() {
		String response = "Event :: " + assertion.getOntologyResource().getLocalName() + 
				" domain: " + assertion.getDomainEntityResource().getLocalName() + "," + 
				" range: " + assertion.getRangeEntityResource().getLocalName(); 
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		response += " timestamp: " + formatter.format(getTimestamp().getTime());
		return response;
	}
}
