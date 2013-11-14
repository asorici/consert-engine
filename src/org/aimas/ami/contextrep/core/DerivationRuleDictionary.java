package org.aimas.ami.contextrep.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.topbraid.spin.util.CommandWrapper;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class DerivationRuleDictionary {
	private Map<Resource, List<DerivedAssertionWrapper>> entity2RuleMap;
	private Map<OntResource, List<DerivedAssertionWrapper>> assertion2RuleMap;
	private Map<DerivedAssertionWrapper, Resource> rule2EntityMap;
	
	public DerivationRuleDictionary() {
		entity2RuleMap = new HashMap<>();
		assertion2RuleMap = new HashMap<>();
		rule2EntityMap = new HashMap<>();
	}
	
	public Map<OntResource, List<DerivedAssertionWrapper>> getAssertion2QueryMap() {
		return assertion2RuleMap;
	}
	
	public Map<Resource, List<DerivedAssertionWrapper>> getEntity2QueryMap() {
		return entity2RuleMap;
	}
	
	public List<DerivedAssertionWrapper> getDerivationsForEntity(Resource entity) {
		return entity2RuleMap.get(entity);
	}
	
	public List<DerivedAssertionWrapper> getDerivationsForAssertion(OntResource assertion) {
		return assertion2RuleMap.get(assertion);
	}
	
	public Resource getEntityForDerivation(DerivedAssertionWrapper derivationWrapper) {
		return rule2EntityMap.get(derivationWrapper);
	}
	
	public void addCommandForEntity(Resource entityResource, DerivedAssertionWrapper derivationWrapper) {
		List<DerivedAssertionWrapper> entityCommands = entity2RuleMap.get(entityResource);
		if (entityCommands == null) {
			entityCommands = new ArrayList<DerivedAssertionWrapper>();
			entityCommands.add(derivationWrapper);
			entity2RuleMap.put(entityResource, entityCommands);
		}
		else {
			entityCommands.add(derivationWrapper);
		}
	}
	
	public void setEntityForDerivation(DerivedAssertionWrapper derivationWrapper, Resource entityResource) {
		rule2EntityMap.put(derivationWrapper, entityResource);
	}
	
	public void addDerivationForAssertion(OntResource assertion, DerivedAssertionWrapper derivedWrapper) {
		List<DerivedAssertionWrapper> assertionCommands = assertion2RuleMap.get(assertion);
		if (assertionCommands == null) {
			assertionCommands = new ArrayList<DerivedAssertionWrapper>();
			assertionCommands.add(derivedWrapper);
			assertion2RuleMap.put(assertion, assertionCommands);
		}
		else {
			assertionCommands.add(derivedWrapper);
		}
	}
	
	public void appendEntityQueryMap(Map<Resource, List<DerivedAssertionWrapper>> map) {
		entity2RuleMap.putAll(map);
	}
	
	public void appendAssertionQueryMap(Map<OntResource, List<DerivedAssertionWrapper>> map) {
		assertion2RuleMap.putAll(map);
	}
	
}
