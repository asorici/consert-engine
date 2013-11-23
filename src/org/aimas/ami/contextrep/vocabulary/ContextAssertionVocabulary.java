package org.aimas.ami.contextrep.vocabulary;

public class ContextAssertionVocabulary {
	public final static String BASE_URI = "http://pervasive.semanticweb.org/ont/2013/05/contextassertion";
	public final static String NS = BASE_URI + "#";
	public final static String FUNCTIONS = BASE_URI + "/functions";
	public final static String FUNCTIONS_NS = FUNCTIONS + "#";
	
	public final static String CONTEXT_ASSERTION_ENTITY = NS + "ContextEntity";
	
	public final static String ENTITY_RELATION_ASSERTION = NS + "entityRelationAssertion";
	public final static String STATIC_RELATION_ASSERTION = NS + "staticRelationAssertion";
	public final static String DYNAMIC_RELATION_ASSERTION = NS + "dynamicRelationAssertion";
	public final static String SENSED_RELATION_ASSERTION = NS + "sensedRelationAssertion";
	public final static String PROFILED_RELATION_ASSERTION = NS + "profiledRelationAssertion";
	public final static String DERIVED_RELATION_ASSERTION = NS + "derivedRelationAssertion";
	
	public final static String ENTITY_DATA_ASSERTION = NS + "entityDataAssertion";
	public final static String STATIC_DATA_ASSERTION = NS + "staticDataAssertion";
	public final static String DYNAMIC_DATA_ASSERTION = NS + "dynamicDataAssertion";
	public final static String SENSED_DATA_ASSERTION = NS + "sensedDataAssertion";
	public final static String PROFILED_DATA_ASSERTION = NS + "profiledDataAssertion";
	public final static String DERIVED_DATA_ASSERTION = NS + "derivedDataAssertion";
	
	public final static String UNARY_CONTEXT_ASSERTION = NS + "UnaryContextAssertion";
	public final static String NARY_CONTEXT_ASSERTION = NS + "NaryContextAssertion";
	
	public final static String CONTEXT_ASSERTION_TYPE_CLASS = NS + "ContextAssertionType";
	public final static String CONTEXT_ASSERTION_TYPE_PROPERTY = NS + "assertionType";
	public final static String TYPE_STATIC = NS + "Static";
	public final static String TYPE_SENSED = NS + "Sensed";
	public final static String TYPE_PROFILED = NS + "Profiled";
	public final static String TYPE_DERIVED = NS + "Derived";
	
	public final static String CONTEXT_ASSERTION_ROLE = NS + "assertionRole";
	public final static String CONTEXT_ASSERTION_RESOURCE = NS + "assertionResource";
	
	public final static String CONTEXT_ANNOTATION = NS + "ContextAnnotation";
	public final static String CONTEXT_ANNOTATION_TIMESTAMP = NS + "hasTimestamp";
	public final static String CONTEXT_ANNOTATION_SOURCE = NS + "assertedBy";
	public final static String CONTEXT_ANNOTATION_VALIDITY = NS + "validDuring";
	public final static String CONTEXT_ANNOTATION_ACCURACY = NS + "hasAccuracy";
	
	public final static String CONTEXT_AGENT= NS + "ContextAgent";
	public final static String CONTEXT_AGENT_TYPE_CLASS = NS + "ContextAgentType";
	public final static String CONTEXT_AGENT_TYPE_PROPERTY = NS + "agentType";
	
	/*
	 * ############################ SPIN Functions and Templates ############################  
	 */
	public final static String CLOSE_ENOUGH_VALIDITY_TEMPLATE = FUNCTIONS_NS + "CloseEnoughValidity";
}
