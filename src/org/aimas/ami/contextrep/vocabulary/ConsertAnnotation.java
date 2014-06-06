package org.aimas.ami.contextrep.vocabulary;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class ConsertAnnotation {
	/** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    public final static String BASE_URI = "http://pervasive.semanticweb.org/ont/2014/05/consert/annotation";
	public final static String NS = BASE_URI + "#";
	
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    // Vocabulary classes
    /////////////////////
    
    public final static Resource CONTEXT_ANNOTATION = m_model.createResource( NS + "ContextAnnotation" );
    public final static Resource BASIC_ANNOTATION = m_model.createResource( NS + "BasicAnnotation" );
    public final static Resource STRUCTURED_ANNOTATION = m_model.createResource( NS + "StructuredAnnotation" );
    
    public final static Resource SOURCE_ANNOTATION = m_model.createResource( NS + "SourceAnnotation" );
    public final static Resource CERTAINTY_ANNOTATION = m_model.createResource( NS + "CertaintyAnnotation" );
    public final static Resource TIMESTAMP_ANNOTATION = m_model.createResource( NS + "TimestampAnnotation" );
    public final static Resource VALIDITY_ANNOTATION = m_model.createResource( NS + "ValidityAnnotation" );
    
    public final static Resource NUMERIC_VALUE_CERTAINTY = m_model.createResource( NS + "NumericValueCertainty" );
    public final static Resource DATETIME_TIMESTAMP = m_model.createResource( NS + "DatetimeTimestamp" );
    public final static Resource TEMPORAL_VALIDITY = m_model.createResource( NS + "TemporalValidity" );
    
    // Vocabulary properties
    ////////////////////////
    
    public final static Property HAS_ANNOTATION = m_model.createProperty( NS + "hasAnnotation" );
	public final static Property HAS_TIMESTAMP = m_model.createProperty( NS + "hasTimestamp" );
	public final static Property HAS_SOURCE = m_model.createProperty( NS + "hasSource" );
	public final static Property HAS_VALIDITY = m_model.createProperty( NS + "hasValidity" );
	public final static Property HAS_CERTAINTY = m_model.createProperty( NS + "hasCertainty" );
	
	public final static Property HAS_STRUCTURED_VALUE = m_model.createProperty( NS + "hasStructuredValue" );
	public final static Property HAS_UNSTRUCTURED_VALUE = m_model.createProperty( NS + "hasUnstructuredValue" );
	
	public final static Property HAS_JOIN_OP = m_model.createProperty( NS + "hasJoinOp" );
	public final static Property HAS_MEET_OP = m_model.createProperty( NS + "hasMeetOp" );
	public final static Property HAS_CONTINUITY_FUNCTION = m_model.createProperty( NS + "hasContinuityFunction" );
}
