package org.aimas.ami.contextrep.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.model.ContextAssertionInfo;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntResource;

public class ContextAssertionIndex {
	
	private List<ContextAssertionInfo> staticContextAssertions;
	private List<ContextAssertionInfo> profiledContextAssertions;
	private List<ContextAssertionInfo> sensedContextAssertions;
	private List<ContextAssertionInfo> derivedContextAssertions;
	
	private Map<OntResource, String> assertion2StoreMap;
	private Map<String, OntResource> graphURIBase2AssertionMap;
	
	public ContextAssertionIndex() {
		staticContextAssertions = new ArrayList<>();
		profiledContextAssertions = new ArrayList<>();
		sensedContextAssertions = new ArrayList<>();
		derivedContextAssertions = new ArrayList<>();
		
		assertion2StoreMap = new HashMap<>();
		graphURIBase2AssertionMap = new HashMap<>();
	}

	public List<ContextAssertionInfo> getStaticContextAssertions() {
		return staticContextAssertions;
	}

	public List<ContextAssertionInfo> getProfiledContextAssertions() {
		return profiledContextAssertions;
	}

	public List<ContextAssertionInfo> getSensedContextAssertions() {
		return sensedContextAssertions;
	}

	public List<ContextAssertionInfo> getDerivedContextAssertions() {
		return derivedContextAssertions;
	}
	
	public List<ContextAssertionInfo> getContextAssertions() {
		List<ContextAssertionInfo> allAssertions = new ArrayList<>();
		allAssertions.addAll(staticContextAssertions);
		allAssertions.addAll(sensedContextAssertions);
		allAssertions.addAll(profiledContextAssertions);
		allAssertions.addAll(derivedContextAssertions);
		
		return allAssertions;
	}
	
	public Map<OntResource, String> getAssertion2StoreMap() {
		return assertion2StoreMap;
	}
	
	public Map<String, OntResource> getGraphURIBase2AssertionMap() {
		return graphURIBase2AssertionMap;
	}
	
	public String getStoreForAssertion(OntResource assertionResource) {
		if (assertionResource != null) {
			return assertion2StoreMap.get(assertionResource);
		}
		
		return null;
	}
	
	
	public OntResource getResourceFromGraphUUID(Node graphUUIDNode) {
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
		return getResourceFromGraphUUID(graphNode) != null;
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
		return assertion2StoreMap.containsKey(assertion);
	}
	
	
	public boolean containsAssertion(ContextAssertionInfo assertion) {
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
	
	
	public void addStaticContextAssertion(ContextAssertionInfo staticAssertion) {
		staticContextAssertions.add(staticAssertion);
	}
	
	public void addProfiledContextAssertion(ContextAssertionInfo profiledAssertion) {
		staticContextAssertions.add(profiledAssertion);
	}
	
	public void addSensedContextAssertion(ContextAssertionInfo sensedAssertion) {
		staticContextAssertions.add(sensedAssertion);
	}
	
	public void addDerivedContextAssertion(ContextAssertionInfo derivedAssertion) {
		staticContextAssertions.add(derivedAssertion);
	}
	
	public void mapAssertionStorage(OntResource assertion) {
		String assertionURI = assertion.getURI();
		assertionURI = assertionURI.replaceAll("#", "/");
		
		String assertionStoreURI = assertionURI + "Store";
		
		assertion2StoreMap.put(assertion, assertionStoreURI);
		graphURIBase2AssertionMap.put(assertionURI, assertion);
	}
	
}
