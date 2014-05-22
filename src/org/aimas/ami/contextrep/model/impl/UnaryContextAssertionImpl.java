package org.aimas.ami.contextrep.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.UnaryContextAssertion;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionContentException;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionModelException;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class UnaryContextAssertionImpl extends ContextAssertionImpl implements UnaryContextAssertion {
	private Resource roleEntityResource;
	
	public UnaryContextAssertionImpl(ContextAssertionType assertionType,
	        int assertionArity, OntResource assertionOntologyResource, Resource roleEntityResource) {
		super(assertionType, assertionArity, assertionOntologyResource);
		this.roleEntityResource = roleEntityResource;
	}

	@Override
    public Resource getRoleEntityResource() {
	    return roleEntityResource;
    }


	@Override
    public List<Statement> copyToAncestor(Resource assertionUUIDRes, Dataset contextStoreDataset, 
    	ContextAssertion ancestorAssertion, OntModel contextModel) 
    	throws ContextAssertionContentException, ContextAssertionModelException {
		List<Statement> ancestorContent = new ArrayList<Statement>();
		
		// get the model store containing the ContextAssertion contents
		Model assertionContentStore = contextStoreDataset.getNamedModel(assertionUUIDRes.getURI());
		
		// ======== inspect the current assertion contents ========
		ResIterator resIt = assertionContentStore.listResourcesWithProperty(RDF.type, assertionOntologyResource);
		Resource assertionBNode = null;
		
		try {
			if (!resIt.hasNext()) { 
				throw new ContextAssertionContentException("Unary ContextAssertion instance " 
					+ assertionUUIDRes.getURI() + " has no " + RDF.type + " property in content.");
			}
			
			assertionBNode = resIt.next();
		} 
		finally {resIt.close();}
		
		Statement roleStatement = assertionContentStore.getProperty(assertionBNode, ConsertCore.CONTEXT_ASSERTION_ROLE);
		if (roleStatement == null) {
			throw new ContextAssertionContentException("Unary ContextAssertion instance " 
				+ assertionUUIDRes.getURI() + " has no " + ConsertCore.CONTEXT_ASSERTION_ROLE + " property in content.");
		}
		
		// ======== create the ancestor statements ========
		Statement ancestorTypeStatement = 
			contextModel.createStatement(assertionBNode, RDF.type, ancestorAssertion.getOntologyResource()); 
		Statement ancestorRoleStatement = 
			contextModel.createStatement(assertionBNode, ConsertCore.CONTEXT_ASSERTION_ROLE, roleStatement.getObject());
		
		ancestorContent.add(ancestorTypeStatement);
		ancestorContent.add(ancestorRoleStatement);
		
		return ancestorContent;
    }
	
}
