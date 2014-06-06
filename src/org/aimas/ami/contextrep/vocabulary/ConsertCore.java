package org.aimas.ami.contextrep.vocabulary;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class ConsertCore {
	/** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
	
    private static final String CONSERT_ONT_ROOT_URI = "http://pervasive.semanticweb.org/ont/2014/05/consert/";
    
	public final static String BASE_URI = CONSERT_ONT_ROOT_URI + "core";
	public final static String NS = BASE_URI + "#";
	
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
	
    // Vocabulary classes
    /////////////////////
    
    public final static Resource CONTEXT_ENTITY = m_model.createResource( NS + "ContextEntity" );
    public final static Resource UNARY_CONTEXT_ASSERTION = m_model.createResource( NS + "UnaryContextAssertion" );
	public final static Resource NARY_CONTEXT_ASSERTION = m_model.createResource( NS + "NaryContextAssertion" );
	
	public final static Resource CONTEXT_ASSERTION_TYPE_CLASS = m_model.createResource( NS + "ContextAssertionType" );
	
	public final static Resource CONTEXT_AGENT = m_model.createResource( NS + "ContextAgent" );
	public final static Resource CONTEXT_AGENT_TYPE_CLASS = m_model.createResource( NS + "ContextAgentType" );
	
    
	// Vocabulary properties
	////////////////////////
	
	public final static Property ENTITY_RELATION_ASSERTION = m_model.createProperty( NS + "entityRelationAssertion" );
	public final static Property STATIC_RELATION_ASSERTION = m_model.createProperty( NS + "staticRelationAssertion" );
	public final static Property DYNAMIC_RELATION_ASSERTION = m_model.createProperty( NS + "dynamicRelationAssertion" );
	public final static Property SENSED_RELATION_ASSERTION = m_model.createProperty( NS + "sensedRelationAssertion" );
	public final static Property PROFILED_RELATION_ASSERTION = m_model.createProperty( NS + "profiledRelationAssertion" );
	public final static Property DERIVED_RELATION_ASSERTION = m_model.createProperty( NS + "derivedRelationAssertion" );
	
	public final static Set<Property> ROOT_BINARY_RELATION_ASSERTION_SET = new HashSet<Property>();
	static {
		ROOT_BINARY_RELATION_ASSERTION_SET.add(ENTITY_RELATION_ASSERTION);
		ROOT_BINARY_RELATION_ASSERTION_SET.add(STATIC_RELATION_ASSERTION);
		ROOT_BINARY_RELATION_ASSERTION_SET.add(DYNAMIC_RELATION_ASSERTION);
		ROOT_BINARY_RELATION_ASSERTION_SET.add(SENSED_RELATION_ASSERTION);
		ROOT_BINARY_RELATION_ASSERTION_SET.add(PROFILED_RELATION_ASSERTION);
		ROOT_BINARY_RELATION_ASSERTION_SET.add(DERIVED_RELATION_ASSERTION);
	}
	
	public final static Property ENTITY_DATA_ASSERTION = m_model.createProperty( NS + "entityDataAssertion" );
	public final static Property STATIC_DATA_ASSERTION = m_model.createProperty( NS + "staticDataAssertion" );
	public final static Property DYNAMIC_DATA_ASSERTION = m_model.createProperty( NS + "dynamicDataAssertion" );
	public final static Property SENSED_DATA_ASSERTION = m_model.createProperty( NS + "sensedDataAssertion" );
	public final static Property PROFILED_DATA_ASSERTION = m_model.createProperty( NS + "profiledDataAssertion" );
	public final static Property DERIVED_DATA_ASSERTION = m_model.createProperty( NS + "derivedDataAssertion" );
	
	public final static Set<Property> ROOT_BINARY_DATA_ASSERTION_SET = new HashSet<Property>();
	static {
		ROOT_BINARY_DATA_ASSERTION_SET.add(ENTITY_DATA_ASSERTION);
		ROOT_BINARY_DATA_ASSERTION_SET.add(STATIC_DATA_ASSERTION);
		ROOT_BINARY_DATA_ASSERTION_SET.add(DYNAMIC_DATA_ASSERTION);
		ROOT_BINARY_DATA_ASSERTION_SET.add(SENSED_DATA_ASSERTION);
		ROOT_BINARY_DATA_ASSERTION_SET.add(PROFILED_DATA_ASSERTION);
		ROOT_BINARY_DATA_ASSERTION_SET.add(DERIVED_DATA_ASSERTION);
	}
	
	public final static Property CONTEXT_ASSERTION_ROLE = m_model.createProperty( NS + "assertionRole" );
	public final static Property CONTEXT_ASSERTION_TYPE_PROPERTY = m_model.createProperty( NS + "assertionType" );
	
	public final static Property CONTEXT_ASSERTION_RESOURCE = m_model.createProperty( NS + "assertionResource" );
	public final static Property CONTEXT_ASSERTION_CONTENT = m_model.createProperty( NS + "assertionContent" );
	
	public final static Property CONTEXT_AGENT_TYPE_PROPERTY = m_model.createProperty( NS + "agentType" );
	
	
	// Vocabulary Individuals
	/////////////////////////
	
	public final static Resource TYPE_STATIC = m_model.createResource( NS + "Static", CONTEXT_ASSERTION_TYPE_CLASS);
	public final static Resource TYPE_SENSED = m_model.createResource( NS + "Sensed", CONTEXT_ASSERTION_TYPE_CLASS );
	public final static Resource TYPE_PROFILED = m_model.createResource( NS + "Profiled", CONTEXT_ASSERTION_TYPE_CLASS );
	public final static Resource TYPE_DERIVED = m_model.createResource( NS + "Derived", CONTEXT_ASSERTION_TYPE_CLASS );
	
	public final static Resource ORG_MGR = m_model.createResource( NS + "OrgMgr", CONTEXT_AGENT_TYPE_CLASS);
	public final static Resource CTX_COORD = m_model.createResource( NS + "CtxCoord", CONTEXT_AGENT_TYPE_CLASS);
	public final static Resource CTX_SENSOR = m_model.createResource( NS + "CtxSensor", CONTEXT_AGENT_TYPE_CLASS);
	public final static Resource CTX_QUERY_HANDLER = m_model.createResource( NS + "CtxQueryHandler", CONTEXT_AGENT_TYPE_CLASS);
	public final static Resource CTX_USER = m_model.createResource( NS + "CtxUser", CONTEXT_AGENT_TYPE_CLASS);
	public final static Resource CTX_AGGREGATOR = m_model.createResource( NS + "CtxAggregator", CONTEXT_AGENT_TYPE_CLASS);
	public final static Resource CTX_HISTORIAN = m_model.createResource( NS + "CtxHistorian", CONTEXT_AGENT_TYPE_CLASS);
	
	// Miscellaneous 
	////////////////
	public static final String ENTITY_STORE_URI = CONSERT_ONT_ROOT_URI + "entityStore"; 
}
