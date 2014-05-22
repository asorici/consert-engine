package org.aimas.ami.contextrep.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.aimas.ami.contextrep.model.BinaryContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionContentException;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionModelException;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class BinaryContextAssertionImpl extends ContextAssertionImpl implements
        BinaryContextAssertion {
	
	private Resource domainEntityResource;
	private Resource rangeEntityResource;
	
	public BinaryContextAssertionImpl(ContextAssertionType assertionType,
	        int assertionArity, OntResource assertionOntologyResource,
	        Resource domainEntityResource, Resource rangeEntityResource) {
		super(assertionType, assertionArity, assertionOntologyResource);
		
		this.domainEntityResource = domainEntityResource;
		this.rangeEntityResource = rangeEntityResource;
	}
	
	@Override
	public boolean isEntityRelation() {
		return assertionOntologyResource.asProperty().isObjectProperty();
	}
	
	@Override
	public boolean isDataRelation() {
		return assertionOntologyResource.asProperty().isDatatypeProperty();
	}
	
	@Override
	public Resource getDomainEntityResource() {
		return domainEntityResource;
	}
	
	@Override
	public Resource getRangeEntityResource() {
		return rangeEntityResource;
	}

	@Override
    public List<Statement> copyToAncestor(Resource assertionUUIDRes, Dataset contextStoreDataset, 
    	ContextAssertion ancestorAssertion, OntModel contextModel) 
    		throws ContextAssertionContentException, ContextAssertionModelException {
		
		List<Statement> ancestorContent = new ArrayList<Statement>();
		
		// get the model store containing the ContextAssertion contents
		Model assertionContentStore = contextStoreDataset.getNamedModel(assertionUUIDRes.getURI());
		
		// ======== inspect the current assertion contents ========
		Statement contentStatement = assertionContentStore.getProperty(null, assertionOntologyResource.asProperty());
		if (contentStatement == null) {
			throw new ContextAssertionContentException("Binary ContextAssertion instance " 
				+ assertionUUIDRes.getURI() + " has no " + assertionOntologyResource + " statement in its content.");
		}
		
		// ======== create the ancestor statements ========
		Statement ancestorContentStatement = contextModel.createStatement(contentStatement.getSubject(), 
				ancestorAssertion.getOntologyResource().asProperty(), contentStatement.getObject());
		ancestorContent.add(ancestorContentStatement);
		
		return ancestorContent;
    }
	
}
