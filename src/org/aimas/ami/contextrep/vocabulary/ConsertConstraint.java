package org.aimas.ami.contextrep.vocabulary;

import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class ConsertConstraint {
	/** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    public final static String BASE_URI = "http://pervasive.semanticweb.org/ont/2014/05/consert/constraint";
	public final static String NS = BASE_URI + "#";
	
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    // Vocabulary classes
    /////////////////////
    
    public final static Resource CONTEXT_CONSTRAINT_VIOLATION = m_model.createResource( NS + "ContextConstraintViolation" );
    public final static Resource UNIQUENESS_CONSTRAINT_VIOLATION = m_model.createResource( NS + "UniquenessConstraintViolation" );
    public final static Resource VALUE_CONSTRAINT_VIOLATION = m_model.createResource( NS + "ValueConstraintViolation" );
    
    public final static Resource CONTEXT_CONSTRAINT_TEMPLATE = m_model.createResource( NS + "ContextConstraintTemplate" );
    public final static Resource UNIQUENESS_CONSTRAINT_TEMPLATE = m_model.createResource( NS + "UniquenessConstraintTemplate" );
    public final static Resource VALUE_CONSTRAINT_TEMPLATE = m_model.createResource( NS + "ValueConstraintTemplate" );
    
    public final static Resource NARY_INHERITANCE_CONSTRAINT = m_model.createResource( NS + "NaryAssertionInheritanceConstraint" );
    public final static Resource ADD_ASSERTION_ROLE_SUBPROPERTY = m_model.createResource( NS + "AddAssertionRoleSubProperty" );
    
    
    // Vocabulary properties
    ////////////////////////
    
    public final static Property CONSTRAINT = m_model.createProperty( SPIN.NS + "contextconstraint" );
    public final static Property HAS_SOURCE_TEMPLATE = m_model.createProperty( NS + "hasSourceTemplate" );
    public final static Property ON_CONTEXT_ASSERTION = m_model.createProperty( NS + "onContextAssertion" );
    public final static Property HAS_CONFLICTING_ASSERTION = m_model.createProperty( NS + "hasConflictingAssertion" );
    public final static Property HAS_CONFLICT_ASSERTION_VALUE = m_model.createProperty( NS + "hasConflictAssertionValue" );
    public final static Property HAS_CONFLICT_ANNOTATION_VALUE = m_model.createProperty( NS + "hasConflictAnnotationValue" );
}
