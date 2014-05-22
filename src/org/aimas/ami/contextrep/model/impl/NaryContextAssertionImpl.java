package org.aimas.ami.contextrep.model.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.NaryContextAssertion;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionContentException;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionModelException;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class NaryContextAssertionImpl extends ContextAssertionImpl implements
        NaryContextAssertion {
	
	private Map<OntProperty, Resource> assertionRoleMap;
	
	public NaryContextAssertionImpl(ContextAssertionType assertionType,
	        int assertionArity, OntResource assertionOntologyResource, 
	        Map<OntProperty, Resource> assertionRoleMap) {
		super(assertionType, assertionArity, assertionOntologyResource);
		this.assertionRoleMap = assertionRoleMap;
	}
	
	@Override
	public Map<OntProperty, Resource> getAssertionRoles() {
		return assertionRoleMap;
	}

	@Override
    public List<Statement> copyToAncestor(Resource assertionUUIDRes, Dataset contextStoreDataset, 
    		ContextAssertion ancestorAssertion, OntModel contextModel) 
    		throws ContextAssertionContentException, ContextAssertionModelException {
		
		List<Statement> ancestorContent = new ArrayList<Statement>();
		NaryContextAssertion naryAncestorAssertion = (NaryContextAssertion)ancestorAssertion;
		Map<OntProperty, Resource> naryAncestorRoleMap = naryAncestorAssertion.getAssertionRoles();
		
		// get the model store containing the ContextAssertion contents
		Model assertionContentStore = contextStoreDataset.getNamedModel(assertionUUIDRes.getURI());
		
		// Step 1) inspect current assertion type content 
		ResIterator resIt = assertionContentStore.listResourcesWithProperty(RDF.type, assertionOntologyResource);
		Resource assertionBNode = null;
		
		try {
			if (!resIt.hasNext()) { 
				throw new ContextAssertionContentException("Nary ContextAssertion instance " 
					+ assertionUUIDRes.getURI() + " has no " + RDF.type + " property in content.");
			}
			
			assertionBNode = resIt.next();
		} 
		finally {resIt.close();}
		
		// Step 2) create ancestor type statement
		Statement ancestorTypeStatement = contextModel.createStatement(assertionBNode, RDF.type, ancestorAssertion.getOntologyResource()); 
		ancestorContent.add(ancestorTypeStatement);
		
		// Step 3) inspect and create role statements
		for (OntProperty ancestorRoleProp : naryAncestorRoleMap.keySet()) {
			Set<? extends OntProperty> ancestorRoleSubProperties = ancestorRoleProp.listSubProperties().toSet();
			boolean found = false;
			
			for (OntProperty roleProp : assertionRoleMap.keySet()) {
				if (ancestorRoleSubProperties.contains(roleProp)) {
					Statement roleStatement = assertionContentStore.getProperty(assertionBNode, roleProp);
					if (roleStatement == null) {
						throw new ContextAssertionContentException("Nary ContextAssertion instance " 
								+ assertionUUIDRes.getURI() + " has no " + roleProp + " property in content.");
					}
					
					Statement ancestorRoleStatement = 
						contextModel.createStatement(assertionBNode, ancestorRoleProp, roleStatement.getObject());
					ancestorContent.add(ancestorRoleStatement);
					
					found = true;
					break;
				}
			}
			
			if (!found) {
				throw new ContextAssertionModelException(
					"NaryContextAssertion " + ancestorAssertion.getOntologyResource() + 
					" has no subProperty in NaryContextAssertion " + assertionOntologyResource + 
					" for role property " + ancestorRoleProp + ".");
			}
		}
		
		
		return ancestorContent;
		
    }
	
}
