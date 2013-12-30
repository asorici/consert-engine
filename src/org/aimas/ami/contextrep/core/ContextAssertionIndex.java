package org.aimas.ami.contextrep.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.model.ContextAssertion;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntResource;

public class ContextAssertionIndex {
	
	private List<ContextAssertion> staticContextAssertions;
	private List<ContextAssertion> profiledContextAssertions;
	private List<ContextAssertion> sensedContextAssertions;
	private List<ContextAssertion> derivedContextAssertions;
	
	private Map<OntResource, ContextAssertion> assertionInfoMap;
	private Map<String, ContextAssertion> graphURIBase2AssertionMap;
	
	ContextAssertionIndex() {
		staticContextAssertions = new ArrayList<>();
		profiledContextAssertions = new ArrayList<>();
		sensedContextAssertions = new ArrayList<>();
		derivedContextAssertions = new ArrayList<>();
		
		assertionInfoMap = new HashMap<>();
		graphURIBase2AssertionMap = new HashMap<>();
	}

	public List<ContextAssertion> getStaticContextAssertions() {
		return staticContextAssertions;
	}

	public List<ContextAssertion> getProfiledContextAssertions() {
		return profiledContextAssertions;
	}

	public List<ContextAssertion> getSensedContextAssertions() {
		return sensedContextAssertions;
	}

	public List<ContextAssertion> getDerivedContextAssertions() {
		return derivedContextAssertions;
	}
	
	public List<ContextAssertion> getContextAssertions() {
		List<ContextAssertion> allAssertions = new ArrayList<>();
		allAssertions.addAll(staticContextAssertions);
		allAssertions.addAll(sensedContextAssertions);
		allAssertions.addAll(profiledContextAssertions);
		allAssertions.addAll(derivedContextAssertions);
		
		return allAssertions;
	}
	
	public Map<OntResource, ContextAssertion> getAssertionInfoMap() {
		return assertionInfoMap;
	}
	
	public Map<String, ContextAssertion> getGraphURIBase2AssertionMap() {
		return graphURIBase2AssertionMap;
	}
	
	/*
	public String getStoreForAssertion(OntResource assertionResource) {
		if (assertionResource != null) {
			return assertionInfoMap.get(assertionResource);
		}
		
		return null;
	}
	*/
	
	public ContextAssertion getAssertionFromResource(OntResource assertionResource) {
		return assertionInfoMap.get(assertionResource);
	}
	
	public ContextAssertion getAssertionFromGraphStore(Node graphNode) {
		// this works because of the way in which we create the 
		// named graph stores for a particular ContextAssertion
		String graphURI = graphNode.getURI();
		int storeSuffixIndex = graphURI.lastIndexOf("Store");
		
		if (storeSuffixIndex > 0) {
			String graphStoreBase = graphURI.substring(0, storeSuffixIndex);
			return graphURIBase2AssertionMap.get(graphStoreBase);
		}
		
		return null;
	}
	
	
	public ContextAssertion getAssertionFromGraphUUID(Node graphUUIDNode) {
		// this works because of the way in which we generate the assertion 
		// named graph UUIDs
		
		String graphUUID = graphUUIDNode.getURI();
		int firstHyphenIndex = graphUUID.indexOf("-");
		
		if (firstHyphenIndex > 0) {
			String graphUUIDBase = graphUUID.substring(0, firstHyphenIndex);
			return graphURIBase2AssertionMap.get(graphUUIDBase);
		}
		
		return null;
	}
	
	
	public boolean isContextAssertionUUID(Node graphNode) {
		return getAssertionFromGraphUUID(graphNode) != null;
	}
	
	
	public boolean isContextStore(Node graphNode) {
		String graphURI = graphNode.getURI();
		
		if (graphURI.equalsIgnoreCase(Config.getEntityStoreURI())) {
			return true;
		}
		
		return isContextAssertionStore(graphNode);
	}
	
	
	public boolean isContextAssertionStore(Node graphNode) {
		String graphURI = graphNode.getURI();
		int storeSuffixIndex = graphURI.lastIndexOf("Store");
		
		if (storeSuffixIndex > 0) {
			String graphStoreBase = graphURI.substring(0, storeSuffixIndex);
			return graphURIBase2AssertionMap.containsKey(graphStoreBase);
		}
		
		return false;
	}
	
	
	public boolean containsAssertion(OntResource assertion) {
		return assertionInfoMap.containsKey(assertion);
	}
	
	
	public boolean containsAssertion(ContextAssertion assertion) {
		if (staticContextAssertions.contains(assertion)) {
			return true;
		}
		
		if (profiledContextAssertions.contains(assertion)) {
			return true;
		}
		
		if (sensedContextAssertions.contains(assertion)) {
			return true;
		}
		
		if (derivedContextAssertions.contains(assertion)) {
			return true;
		}
		
		return false;
	}
	
	
	public void addStaticContextAssertion(ContextAssertion staticAssertion) {
		staticContextAssertions.add(staticAssertion);
		assertionInfoMap.put(staticAssertion.getOntologyResource(), staticAssertion);
	}
	
	public void addProfiledContextAssertion(ContextAssertion profiledAssertion) {
		profiledContextAssertions.add(profiledAssertion);
		assertionInfoMap.put(profiledAssertion.getOntologyResource(), profiledAssertion);
	}
	
	public void addSensedContextAssertion(ContextAssertion sensedAssertion) {
		sensedContextAssertions.add(sensedAssertion);
		assertionInfoMap.put(sensedAssertion.getOntologyResource(), sensedAssertion);
	}
	
	public void addDerivedContextAssertion(ContextAssertion derivedAssertion) {
		derivedContextAssertions.add(derivedAssertion);
		assertionInfoMap.put(derivedAssertion.getOntologyResource(), derivedAssertion);
	}
	
	
	public void mapAssertionStorage(ContextAssertion assertion) {
		String assertionURI = assertion.getOntologyResource().getURI();
		assertionURI = assertionURI.replaceAll("#", "/");
		
		//String assertionStoreURI = assertionURI + "Store";
		//assertionInfoMap.put(assertion, assertionStoreURI);
		
		graphURIBase2AssertionMap.put(assertionURI, assertion);
	}
	
}
