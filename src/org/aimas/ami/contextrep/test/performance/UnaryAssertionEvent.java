package org.aimas.ami.contextrep.test.performance;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.model.UnaryContextAssertion;
import org.aimas.ami.contextrep.model.impl.UnaryContextAssertionImpl;
import org.aimas.ami.contextrep.test.ContextEvent;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

public class UnaryAssertionEvent extends ContextEvent {
	// statement
	UnaryContextAssertion assertion;
	
	
	public UnaryAssertionEvent(OntModel contextModel, OntClass assertionClass, Resource roleEntity, 
			Calendar timestamp, int validity, double accuracy, String assertedByURI) {
		super(contextModel, timestamp, validity, accuracy, assertedByURI);
		
		this.assertion = new UnaryContextAssertionImpl(ContextAssertionType.Sensed, 1, assertionClass, roleEntity);
		this.request = createUpdateRequest();
	}
	
	
	@Override
	protected UpdateRequest createUpdateRequest() {
		UpdateRequest request = UpdateFactory.create() ;
		
		Node graphURINode = Node.createURI(GraphUUIDGenerator.createUUID(assertion.getOntologyResource()));
		Quad q = Quad.create(graphURINode, assertion.getRoleEntityResource().asNode(), RDF.type.asNode(), assertion.getOntologyResource().asNode());
		
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
				" roleEntity: " + assertion.getRoleEntityResource().getLocalName(); 
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		response += " timestamp: " + formatter.format(getTimestamp().getTime());
		return response;
	}
}
