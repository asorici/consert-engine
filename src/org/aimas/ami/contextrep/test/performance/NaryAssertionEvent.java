package org.aimas.ami.contextrep.test.performance;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.model.NaryContextAssertion;
import org.aimas.ami.contextrep.model.impl.NaryContextAssertionImpl;
import org.aimas.ami.contextrep.test.ContextEvent;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
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

public class NaryAssertionEvent extends ContextEvent {
	// statement
	NaryContextAssertion assertion;
	
	public NaryAssertionEvent(OntModel contextModel, OntClass assertionClass, 
			OntProperty roleProperty1, OntProperty roleProperty2, OntProperty roleProperty3,
			Resource roleEntity1, Resource roleEntity2, Resource roleEntity3,
			Calendar timestamp, int validity, double accuracy, String assertedByURI) {
		super(contextModel, timestamp, validity, accuracy, assertedByURI);
		
		Map<OntProperty, Resource> roleMap = new HashMap<>();
		roleMap.put(roleProperty1, roleEntity1);
		roleMap.put(roleProperty2, roleEntity2);
		roleMap.put(roleProperty3, roleEntity3);
		
		this.assertion = new NaryContextAssertionImpl(ContextAssertionType.Sensed, 3, assertionClass, roleMap);
		this.request = createUpdateRequest();
	}
	
	@Override
	protected UpdateRequest createUpdateRequest() {
		UpdateRequest request = UpdateFactory.create();
		Resource bnode = ResourceFactory.createResource();
		
		Node graphURINode = Node.createURI(GraphUUIDGenerator.createUUID(assertion.getOntologyResource()));
		QuadDataAcc data = new QuadDataAcc();
		
		data.addQuad(Quad.create(graphURINode, bnode.asNode(), RDF.type.asNode(), assertion.getOntologyResource().asNode()));
		
		for (OntProperty roleProp : assertion.getAssertionRoles().keySet()) {
			data.addQuad(Quad.create(graphURINode, bnode.asNode(), roleProp.asNode(), 
					assertion.getAssertionRoles().get(roleProp).asNode()));
		}
		
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
		String response = "Event :: " + assertion.getOntologyResource().getLocalName(); 
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		response += " timestamp: " + formatter.format(getTimestamp().getTime());
		return response;
	}
}
