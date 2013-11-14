package org.aimas.ami.contextrep.test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.topbraid.spin.model.QueryOrTemplateCall;
import org.topbraid.spin.model.SPINFactory;

import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class TDBCreateTest {
	
	public static String ASSEMBLER_FILE = "etc/context-tdb-assembler.ttl";
	public static String DOCUMENT_MGR_SPEC_PATH = "etc/context-ont-policy.rdf;etc/context-ont-location-mapper.ttl";
	public static String CONTEXT_ENTITY_STORE = "http://pervasive.semanticweb.org/ont/2013/05/contextassertion/ContextEntityStore";
	
	public static String CONTEXT_ASSERTION_SOURCE = "http://pervasive.semanticweb.org/ont/2013/05/contextassertion";
	public static String CONTEXT_ASSERTION_NS = CONTEXT_ASSERTION_SOURCE + "#";
	
	// common properties and classes of ContextAssertion Ontology 
	public static String ENTITY_RELATION_ASSERTION = CONTEXT_ASSERTION_NS + "entityRelationAssertion";
	public static String ENTITY_DATA_ASSERTION = CONTEXT_ASSERTION_NS + "entityDataAssertion";
	public static String UNARY_CONTEXT_ASSERTION = CONTEXT_ASSERTION_NS + "UnaryContextAssertion";
	public static String NARY_CONTEXT_ASSERTION  = CONTEXT_ASSERTION_NS + "NaryContextAssertion";
	
	public static Map<QueryOrTemplateCall, List<OntResource>> inverseRuleDictionary;
	
	public TDBCreateTest() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("jena-log4j.properties");
		
		
		String contextModelURI = "http://pervasive.semanticweb.org/ont/2013/09/adhocmeeting/models";
		
		/* Assemble dataset that contains:
		 * - the general store: statement of the context model that refer
		 *   to context entities (rdf:type statements and EntityDescriptions)
		 * - the named graphs that act as Stores for each TYPE of ContextAssertion
		 * - the named graphs which act as identifiers of the ContextAssertions 
		 */
		//Dataset dataset = TDBFactory.assembleDataset(ASSEMBLER_FILE);
		
		/* Create the base context domain-model given for this environment. 
		 * Customize OntDocumentManager to resolve owl:imports to the local file system. 
		 */
		
		long start = System.currentTimeMillis();
		
		OntModelSpec contextModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
		OntDocumentManager globalDocMgr = OntDocumentManager.getInstance();
		globalDocMgr.setMetadataSearchPath(DOCUMENT_MGR_SPEC_PATH, true);
		OntModel contextModel = ModelFactory.createOntologyModel(contextModelSpec);
		contextModel.read(contextModelURI);
		
		System.out.println("Initialization time: " + (System.currentTimeMillis() - start) + "ms");
		start = System.currentTimeMillis();
		/*
		 * Create the named graphs for the general context entity store and 
		 * for the TYPE of each <it>asserted (not derived)</it> ContextAssertion 
		 * in the base context domain-model  
		 */
		
		//Model contextEntityStore = dataset.getNamedModel(CONTEXT_ENTITY_STORE);
		//Resource s = ResourceFactory.createResource(contextModelURI + "#" + "Camera1");
		//Property p = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		//Resource o = ResourceFactory.createResource(contextModelURI + "#" + "KinectCamera"); 
		//contextEntityStore.add(s, p, o);
		//TDB.sync(contextEntityStore);
		
		// create stores for subproperties of entityRelationAssertion and entityDataAssertion
		OntModel contextBasicInfModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF, contextModel);
		System.out.println("Create inference model time: " + (System.currentTimeMillis() - start) + "ms");
		start = System.currentTimeMillis();
		
		ExtendedIterator<? extends OntProperty> subPropIt = 
			contextBasicInfModel.getOntProperty(ENTITY_RELATION_ASSERTION).listSubProperties();
		
		/*
		for(;subPropIt.hasNext();) {
			OntProperty prop = subPropIt.next();
			if (!SPINFactory.isAbstract(prop)) {
				String propStoreURI = prop.getURI() + "Store";
				Model graphStore = dataset.getNamedModel(propStoreURI);
				TDB.sync(graphStore);
			}
		}
		
		subPropIt = contextBasicInfModel.getOntProperty(ENTITY_DATA_ASSERTION).listSubProperties();
		for(;subPropIt.hasNext();) {
			OntProperty prop = subPropIt.next();
			if (!SPINFactory.isAbstract(prop)) {
				String propStoreURI = prop.getURI() + "Store";
				Model graphStore = dataset.getNamedModel(propStoreURI);
				TDB.sync(graphStore);
			}
		}
		
		// create stores for subclasses of UnaryContextAssertion and NaryContextAssertion
		ExtendedIterator<OntClass> subClassIt = contextBasicInfModel.getOntClass(UNARY_CONTEXT_ASSERTION).listSubClasses();
		for(;subClassIt.hasNext();) {
			OntClass cls = subClassIt.next();
			if (!SPINFactory.isAbstract(cls)) {
				String clsStoreURI = cls.getURI() + "Store";
				Model graphStore = dataset.getNamedModel(clsStoreURI);
				TDB.sync(graphStore);
			}
		}
		
		subClassIt = contextBasicInfModel.getOntClass(NARY_CONTEXT_ASSERTION).listSubClasses();
		for(;subClassIt.hasNext();) {
			OntClass cls = subClassIt.next();
			if (!SPINFactory.isAbstract(cls)) {
				String clsStoreURI = cls.getURI() + "Store";
				dataset.getNamedModel(clsStoreURI);
			}
		}
		*/
		
		/*
		 * Create global inverse dictionary
		 */
		
		List<OntResource> derivedContextAssertions = new LinkedList<OntResource>();
		subPropIt = contextBasicInfModel.getOntProperty(CONTEXT_ASSERTION_NS + "derivedRelationAssertion").listSubProperties(true);
		derivedContextAssertions.addAll(subPropIt.toList());
		
		subPropIt = contextBasicInfModel.getOntProperty(CONTEXT_ASSERTION_NS + "derivedDataAssertion").listSubProperties(true);
		derivedContextAssertions.addAll(subPropIt.toList());
		
		ExtendedIterator<OntClass> assertionClassIt = contextBasicInfModel.getOntClass(UNARY_CONTEXT_ASSERTION).listSubClasses();
		assertionClassIt = assertionClassIt.andThen(contextBasicInfModel.getOntClass(NARY_CONTEXT_ASSERTION).listSubClasses());
		
		for(;assertionClassIt.hasNext();) {
			OntClass cls = assertionClassIt.next();
			if (!SPINFactory.isAbstract(cls)) {
				Iterator<OntClass> supers = cls.listSuperClasses(true);
				for(;supers.hasNext();) {
					OntClass sup = supers.next();
					if (sup.isRestriction()) {
						Restriction restriction = sup.asRestriction();
						if (restriction.isHasValueRestriction()) {
							HasValueRestriction hvr = restriction.asHasValueRestriction();
							if (hvr.onProperty(contextBasicInfModel.getProperty(CONTEXT_ASSERTION_NS + "assertionType"))
								&& hvr.hasValue(contextBasicInfModel.getIndividual(CONTEXT_ASSERTION_NS + "Derived"))) {
								derivedContextAssertions.add(cls);
								break;
							}
						}
					}
				}
			}
		}
		
		for (OntResource res : derivedContextAssertions) {
			System.out.println(res.getURI());
			/*
			if (res.isProperty()) {
				OntProperty prop = res.asProperty();
				OntResource domain = prop.getDomain();
				OntResource range = prop.getRange();
				
			}
			else {
				
			}
			*/
		}
		
		System.out.println("List derived assertions time: " + (System.currentTimeMillis() - start) + "ms");
		
		/*
		 * Close all open and models
		 */
		contextBasicInfModel.close();
		contextModel.close();
		
		
		/*
		 * Sync and close dataset
		 */
		
		
		//TDB.sync(dataset);
		//dataset.close() ;
	}
	
}