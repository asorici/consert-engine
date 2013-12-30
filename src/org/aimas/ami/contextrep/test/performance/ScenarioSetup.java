package org.aimas.ami.contextrep.test.performance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

import org.aimas.ami.contextrep.core.Loader;
import org.aimas.ami.contextrep.test.performance.PerformanceConfig.ContextAssertionConfig;
import org.aimas.ami.contextrep.test.performance.PerformanceConfig.ContextEntityConfig;
import org.aimas.ami.contextrep.utils.ContextAssertionUtil;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;
import org.openjena.atlas.logging.Log;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SPIN;

import com.google.gson.Gson;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

public class ScenarioSetup {
	public static final String CONFIG_FILE_PATH = "src/org/aimas/ami/contextrep/test/performance/performance-config.json";
	public static final String CONTEXT_GEN_MODEL_FILE_PATH = "etc/ontologies/PerformanceTest/performanceGen.rdf";
	public static final String CONTEXT_GEN_MODEL_URI = "http://pervasive.semanticweb.org/ont/2013/11/test/performance";
	public static final String CONTEXT_GEN_MODEL_NS = CONTEXT_GEN_MODEL_URI + "#";
	
	public static final String CONTEXT_SEED_MODEL_URI = "http://pervasive.semanticweb.org/ont/2013/11/test/performanceSeed";
	public static final String CONTEXT_DERIVATION_MODEL_URI = "http://pervasive.semanticweb.org/ont/2013/11/test/performanceTpl";
	
	
	public static final String UNARY_DERIVATION_SCHEME_URI = CONTEXT_DERIVATION_MODEL_URI + "#" + "SchemeUnary";
	public static final String BINARY_DERIVATION_SCHEME_URI = CONTEXT_DERIVATION_MODEL_URI + "#" + "SchemeBinary";
	public static final String NARY_DERIVATION_SCHEME_URI = CONTEXT_DERIVATION_MODEL_URI + "#" + "SchemeNary";
	
	public static final Random randGen = new Random();
	
	public PerformanceConfig configuration;
	
	public ScenarioSetup() {
		readTestConfiguration();
		prepareTestConfiguration();
	}
	
	private void prepareTestConfiguration() { 
	    // STEP 1: load the model containing the derivation rule schemas
		OntDocumentManager globalDocMgr = OntDocumentManager.getInstance();
		globalDocMgr.setMetadataSearchPath(Loader.DEFAULT_DOC_MGR_PATH, true);
		
		
		OntModelSpec genContextModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
		OntModel genContextModel = ModelFactory.createOntologyModel(genContextModelSpec);
		genContextModel.read(CONTEXT_SEED_MODEL_URI);
		genContextModel.setNsPrefix("", CONTEXT_GEN_MODEL_NS);
		
		// STEP 2: start creating the actual test model : entities, assertions and attached derivation rules.
		createModelEntities(genContextModel);
		createModelAssertions(genContextModel);
		createModelDerivationRules(genContextModel);
		
		// STEP 3: save the file
		FileWriter out = null;
		try {
			out = new FileWriter( CONTEXT_GEN_MODEL_FILE_PATH );
			genContextModel.write( out, "RDF/XML");
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		finally {
		   try {
		       if (out != null) {
		    	   out.close();
		       }
		   }
		   catch (IOException closeException) {}
		}
    }

	
	private void readTestConfiguration() {
	    Gson gson = new Gson();
		String jsonConfig = readFile(CONFIG_FILE_PATH);
	    configuration = gson.fromJson(jsonConfig, PerformanceConfig.class);
    }

	private String readFile(String filename) {
        File f = new File(filename);
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            return new String(bytes,"UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
	}
	
	
	private void createModelEntities(OntModel contextModel) {
		ContextEntityConfig contextEntities = configuration.contextEntities;
		
	    for (int i = 0; i < contextEntities.nrTypes; i++) {
	    	String entityURI = CONTEXT_GEN_MODEL_NS + "E" + (i + 1);
	    	OntClass entityClass = contextModel.createClass(entityURI);
	    	contextModel.add(entityClass, RDFS.subClassOf, 
	    		contextModel.getOntClass(ContextAssertionVocabulary.CONTEXT_ENTITY));
	    }
    }

	
	private void createModelAssertions(OntModel contextModel) {
		ContextAssertionConfig contextAssertions = configuration.contextAssertions;
		OntProperty assertionRole = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_ROLE);
		
		// ATTENTION: THE DERIVED ASSERTIONS ARE HARDCODED AS ARE THE DERIVATION RULES THAT GO ALONG
		
		// ======================== first the unary assertions ========================
		for (int i = contextAssertions.unary.nrDerived; i < contextAssertions.unary.nrTypes; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "UnaryA" + (i + 1);
	    	OntClass assertionClass = contextModel.createClass(assertionURI);
	    	
	    	// set the restriction for the assertionRole property
	    	// select randomly a ContextEntity
	    	int contextEntityIndex = randGen.nextInt(configuration.contextEntities.nrTypes) + 1;
	    	String roleEntityURI = CONTEXT_GEN_MODEL_NS + "E" + contextEntityIndex;
	    	OntClass roleEntityClass = contextModel.getOntClass(roleEntityURI);
	    	
	    	Restriction assertionRoleRestriction = contextModel.createAllValuesFromRestriction(null,
		    	assertionRole, roleEntityClass);
	    	
	    	// set the restriction for the assertionType property
	    	Restriction assertionTypeRestriction = contextModel.createHasValueRestriction(null,
		        contextModel.getProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_TYPE_PROPERTY),
		        contextModel.getIndividual(ContextAssertionVocabulary.TYPE_SENSED));
	    	
	    	// set the subClassOf properties for this new assertion
	    	contextModel.add(assertionClass, RDFS.subClassOf, 
		    	contextModel.getOntClass(ContextAssertionVocabulary.UNARY_CONTEXT_ASSERTION));
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionTypeRestriction);
		}
		
		for (int i = 0; i < contextAssertions.unary.nrDerived; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "UnaryA" + (i + 1);
	    	OntClass assertionClass = contextModel.createClass(assertionURI);
	    	
	    	// the assertion Role restriction is the same one as for UnaryA4
	    	String inspiringUnaryURI = CONTEXT_GEN_MODEL_NS + "UnaryA4";
	    	OntClass inspiringUnaryAssertion = contextModel.getOntClass(inspiringUnaryURI);
	    	Resource roleEntityClass = 
	    		ContextAssertionUtil.getAssertionRoleEntity(contextModel, inspiringUnaryAssertion, assertionRole);
	    	
	    	Restriction assertionRoleRestriction = contextModel.createAllValuesFromRestriction(null, 
	    			assertionRole, roleEntityClass);
	    	Restriction assertionTypeRestriction = contextModel.createHasValueRestriction(null,
	    	    	contextModel.getProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_TYPE_PROPERTY),
	    	    	contextModel.getIndividual(ContextAssertionVocabulary.TYPE_DERIVED));
	    	
	    	// set the subClassOf properties for this new assertion
	    	contextModel.add(assertionClass, RDFS.subClassOf, 
		    	contextModel.getOntClass(ContextAssertionVocabulary.UNARY_CONTEXT_ASSERTION));
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionTypeRestriction);
		}
		
		// ======================== then for the binary context assertion ========================
		for (int i = contextAssertions.binary.nrDerived; i < contextAssertions.binary.nrTypes; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "BinaryA" + (i + 1);
	    	OntProperty assertionProp = contextModel.createOntProperty(assertionURI);
	    	
	    	// select randomly a domain and range class
	    	int indexDomain = randGen.nextInt(configuration.contextEntities.nrTypes) + 1;
	    	int indexRange = randGen.nextInt(configuration.contextEntities.nrTypes) + 1;
	    	
	    	String domainEntityURI = CONTEXT_GEN_MODEL_NS + "E" + indexDomain;
	    	String rangeEntityURI = CONTEXT_GEN_MODEL_NS + "E" + indexRange;
	    	
	    	OntClass domainEntityClass = contextModel.getOntClass(domainEntityURI);
	    	OntClass rangeEntityClass = contextModel.getOntClass(rangeEntityURI);
	    	
	    	// set domain and range limits
	    	assertionProp.setDomain(domainEntityClass);
	    	assertionProp.setRange(rangeEntityClass);
	    	
	    	// mark its type by subPropertyOf
	    	contextModel.add(assertionProp, RDFS.subPropertyOf, 
		    	contextModel.getOntProperty(ContextAssertionVocabulary.SENSED_RELATION_ASSERTION));
		}
		
		for (int i = 0; i < contextAssertions.binary.nrDerived; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "BinaryA" + (i + 1);
	    	OntProperty assertionProp = contextModel.createOntProperty(assertionURI);
			
	    	// the inspiring property takes after BinaryA6
	    	String inspiringBinaryURI = CONTEXT_GEN_MODEL_NS + "BinaryA6";
	    	OntProperty inspiringBinaryAssertion = contextModel.getOntProperty(inspiringBinaryURI);
	    	Resource domainEntityClass = inspiringBinaryAssertion.getDomain();
	    	Resource rangeEntityClass = inspiringBinaryAssertion.getRange();
	    	
	    	// set domain and range limits
	    	assertionProp.setDomain(domainEntityClass);
	    	assertionProp.setRange(rangeEntityClass);
	    	
	    	// the assertionProp has the same domain and range as BinaryA6
			contextModel.add(assertionProp, RDFS.subPropertyOf, 
	    		contextModel.getOntProperty(ContextAssertionVocabulary.DERIVED_RELATION_ASSERTION));
		}
		
		
		// ======================== finally the nary context assertions ========================
		for (int i = contextAssertions.nary.nrDerived; i < contextAssertions.nary.nrTypes; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "NaryA" + (i + 1);
	    	OntClass assertionClass = contextModel.createClass(assertionURI);
	    	
	    	// set the restriction for the assertionRole property
	    	// select randomly 3 ContextEntities
	    	int contextEntityIndex1 = randGen.nextInt(configuration.contextEntities.nrTypes) + 1;
	    	int contextEntityIndex2 = randGen.nextInt(configuration.contextEntities.nrTypes) + 1;
	    	int contextEntityIndex3 = randGen.nextInt(configuration.contextEntities.nrTypes) + 1;
	    	
	    	String roleEntityURI1 = CONTEXT_GEN_MODEL_NS + "E" + contextEntityIndex1;
	    	String roleEntityURI2 = CONTEXT_GEN_MODEL_NS + "E" + contextEntityIndex2;
	    	String roleEntityURI3 = CONTEXT_GEN_MODEL_NS + "E" + contextEntityIndex3;
	    	
	    	OntClass roleEntityClass1 = contextModel.getOntClass(roleEntityURI1);
	    	OntClass roleEntityClass2 = contextModel.getOntClass(roleEntityURI2);
	    	OntClass roleEntityClass3 = contextModel.getOntClass(roleEntityURI3);
	    	
	    	// create 3 subProperties of assertionRole
	    	String assertionRoleURI1 = assertionURI + "_R1";
	    	String assertionRoleURI2 = assertionURI + "_R2";
	    	String assertionRoleURI3 = assertionURI + "_R3";
	    	
	    	OntProperty assertionRoleProp1 = contextModel.createOntProperty(assertionRoleURI1);
	    	assertionRoleProp1.setDomain(assertionClass);
	    	assertionRoleProp1.setRange(roleEntityClass1);
	    	
	    	OntProperty assertionRoleProp2 = contextModel.createOntProperty(assertionRoleURI2);
	    	assertionRoleProp2.setDomain(assertionClass);
	    	assertionRoleProp2.setRange(roleEntityClass2);
	    	
	    	OntProperty assertionRoleProp3 = contextModel.createOntProperty(assertionRoleURI3);
	    	assertionRoleProp3.setDomain(assertionClass);
	    	assertionRoleProp3.setRange(roleEntityClass3);
	    	
	    	// set the role restrictions for this assertion
	    	Restriction assertionRoleRestriction1 = 
	    		contextModel.createAllValuesFromRestriction(null, assertionRoleProp1, roleEntityClass1);
	    	Restriction assertionRoleRestriction2 = 
	    		contextModel.createAllValuesFromRestriction(null, assertionRoleProp2, roleEntityClass2);
	    	Restriction assertionRoleRestriction3 = 
	    		contextModel.createAllValuesFromRestriction(null, assertionRoleProp3, roleEntityClass3);
	    	
	    	// set the restriction for the assertionType property
	    	Restriction assertionTypeRestriction = contextModel.createHasValueRestriction(null,
		    	    contextModel.getProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_TYPE_PROPERTY),
		    	    contextModel.getIndividual(ContextAssertionVocabulary.TYPE_SENSED));
	    		    	
	    	// set the subClassOf properties for this new assertion
	    	contextModel.add(assertionClass, RDFS.subClassOf, 
		    	contextModel.getOntClass(ContextAssertionVocabulary.NARY_CONTEXT_ASSERTION));
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionTypeRestriction);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction1);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction2);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction3);
		}
		
		for (int i = 0; i < contextAssertions.nary.nrDerived; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "NaryA" + (i + 1);
	    	OntClass assertionClass = contextModel.createClass(assertionURI);
			
	    	// create 3 subProperties of assertionRole
	    	String assertionRoleURI1 = assertionURI + "_R1";
	    	String assertionRoleURI2 = assertionURI + "_R2";
	    	String assertionRoleURI3 = assertionURI + "_R3";
	    	
	    	// the inspiring class takes after NaryA8
	    	String inspiringNaryURI_R1 = CONTEXT_GEN_MODEL_NS + "NaryA8_R1";
	    	String inspiringNaryURI_R2 = CONTEXT_GEN_MODEL_NS + "NaryA8_R2";
	    	String inspiringNaryURI_R3 = CONTEXT_GEN_MODEL_NS + "NaryA8_R3";
	    	OntProperty inspiringRoleProperty1 = contextModel.getOntProperty(inspiringNaryURI_R1);
	    	OntProperty inspiringRoleProperty2 = contextModel.getOntProperty(inspiringNaryURI_R2);
	    	OntProperty inspiringRoleProperty3 = contextModel.getOntProperty(inspiringNaryURI_R3);
	    	
	    	OntProperty assertionRoleProp1 = contextModel.createOntProperty(assertionRoleURI1);
	    	assertionRoleProp1.setDomain(assertionClass);
	    	assertionRoleProp1.setRange(inspiringRoleProperty1.getDomain());
	    	
	    	OntProperty assertionRoleProp2 = contextModel.createOntProperty(assertionRoleURI2);
	    	assertionRoleProp2.setDomain(assertionClass);
	    	assertionRoleProp2.setRange(inspiringRoleProperty2.getDomain());
	    	
	    	OntProperty assertionRoleProp3 = contextModel.createOntProperty(assertionRoleURI3);
	    	assertionRoleProp3.setDomain(assertionClass);
	    	assertionRoleProp3.setRange(inspiringRoleProperty3.getDomain());
	    	
	    	// set the role restrictions for this assertion
	    	Restriction assertionRoleRestriction1 = 
	    		contextModel.createAllValuesFromRestriction(null, assertionRoleProp1, inspiringRoleProperty1.getDomain());
	    	Restriction assertionRoleRestriction2 = 
	    		contextModel.createAllValuesFromRestriction(null, assertionRoleProp2, inspiringRoleProperty2.getDomain());
	    	Restriction assertionRoleRestriction3 = 
	    		contextModel.createAllValuesFromRestriction(null, assertionRoleProp3, inspiringRoleProperty3.getDomain());
	    	
	    	Restriction assertionTypeRestriction = contextModel.createHasValueRestriction(null,
	    	    	contextModel.getProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_TYPE_PROPERTY),
	    	    	contextModel.getIndividual(ContextAssertionVocabulary.TYPE_DERIVED));
	    	
			// set the subClassOf properties for this new assertion
	    	contextModel.add(assertionClass, RDFS.subClassOf, 
		    	contextModel.getOntClass(ContextAssertionVocabulary.NARY_CONTEXT_ASSERTION));
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionTypeRestriction);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction1);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction2);
	    	contextModel.add(assertionClass, RDFS.subClassOf, assertionRoleRestriction3);
		}
    }

	
	private void createModelDerivationRules(OntModel contextModel) {
	    // first get some usefull properties
		OntProperty assertionRole = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_ROLE);
		
		// then read in all the derivation templates
		SPINModuleRegistry.get().init();
		OntModelSpec derivationModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
		OntModel derivationModel = ModelFactory.createOntologyModel(derivationModelSpec);
		derivationModel.read(CONTEXT_DERIVATION_MODEL_URI);
		SPINModuleRegistry.get().registerTemplates(derivationModel);
		
		// STEP 1 : retrieve all derived UnaryContextAssertions		
		for (int i = 0; i < configuration.contextAssertions.unary.nrDerived; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "UnaryA" + (i + 1);
	    	OntClass unaryAssertion = contextModel.getOntClass(assertionURI);
	    	//OntClass derivedEntityClass = unaryAssertion.getPropertyResourceValue(assertionRole).as(OntClass.class);
	    	Resource derivedEntityClass = ContextAssertionUtil.getAssertionRoleEntity(contextModel, unaryAssertion, assertionRole);
	    	
	    	// load the SchemeUnary
	    	Template unaryScheme = SPINModuleRegistry.get().getTemplate(UNARY_DERIVATION_SCHEME_URI, null);
	    	TemplateCall unarySchemeCall = SPINFactory.createTemplateCall(contextModel, unaryScheme);
	    	
	    	// now start assigning values to the required arguments
	    	instantiateUnarySchemeCall(unarySchemeCall, contextModel, derivationModel, unaryAssertion, 
	    		derivedEntityClass.as(OntClass.class));
	    	
	    	// and attach it using the spin:deriveassertion property to the derivedEntityClass
	    	derivedEntityClass.addProperty(ResourceFactory.createProperty(SPIN.NS + "deriveassertion"), unarySchemeCall);
		}
		
		// STEP 2 : retrieve all derived BinaryContextAssertions		
		for (int i = 0; i < configuration.contextAssertions.binary.nrDerived; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "BinaryA" + (i + 1);
			OntProperty binaryAssertion = contextModel.getOntProperty(assertionURI);
			OntResource domainClass = binaryAssertion.getDomain();
			OntResource rangeClass = binaryAssertion.getRange();
			
			// load the SchemeBinary
			Template binaryScheme = SPINModuleRegistry.get().getTemplate(BINARY_DERIVATION_SCHEME_URI, null);
			TemplateCall binarySchemeCall = SPINFactory.createTemplateCall(contextModel, binaryScheme);
			
			// now start assigning values to the required arguments
			instantiateBinarySchemeCall(binarySchemeCall, contextModel, derivationModel, binaryAssertion, domainClass, rangeClass);
			
			// and attach it using the spin:deriveassertion property to the domainClass
			domainClass.addProperty(ResourceFactory.createProperty(SPIN.NS + "deriveassertion"), binarySchemeCall);
		}
		
		// STEP 3 : retrieve all derived NaryContextAssertions		
		for (int i = 0; i < configuration.contextAssertions.nary.nrDerived; i++) {
			String assertionURI = CONTEXT_GEN_MODEL_NS + "NaryA" + (i + 1);
			OntClass naryAssertion = contextModel.getOntClass(assertionURI);
			
			// load the SchemeNary
			Template naryScheme = SPINModuleRegistry.get().getTemplate(NARY_DERIVATION_SCHEME_URI, null);
			TemplateCall narySchemeCall = SPINFactory.createTemplateCall(contextModel, naryScheme);
			
			// now start assigning values to the required arguments
			instantiateNarySchemeCall(narySchemeCall, contextModel, derivationModel, naryAssertion);
			
			// and attach it using the spin:deriveassertion property to the first roleEntity
			OntProperty naryAssertion_role1 = contextModel.getOntProperty(naryAssertion.getURI() + "_R1");
			OntResource entityR1Class = naryAssertion_role1.getRange();
			entityR1Class.addProperty(ResourceFactory.createProperty(SPIN.NS + "deriveassertion"), narySchemeCall);
		}

    }
	
	
	private void instantiateUnarySchemeCall(TemplateCall unarySchemeCall, OntModel contextModel, 
			OntModel derivationModel, OntClass unaryAssertion, OntClass derivedEntityClass) {
		OntProperty assertionRole = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_ROLE);
		
		// unaryAssertion
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "unaryAssertion"), unaryAssertion);
		
		// entityClass		
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "entityClass"), derivedEntityClass);
		
		// unary_a1, unary_e1
		OntClass unary_a1 = selectRandomUnaryAssertion(contextModel, false);
		OntClass unary_e1 = 
			ContextAssertionUtil.getAssertionRoleEntity(contextModel, unary_a1, assertionRole).as(OntClass.class);
		
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "unary_a1"), unary_a1);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "unary_e1"), unary_e1);
		
		// binary_a1, binary_e1, binary_e2
		OntProperty binary_a1 = selectRandomBinaryAssertion(contextModel, false);
		OntClass binary_e1 = binary_a1.getPropertyResourceValue(RDFS.domain).as(OntClass.class);
		OntClass binary_e2 = binary_a1.getPropertyResourceValue(RDFS.range).as(OntClass.class);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binary_a1"), binary_a1);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binary_e1"), binary_e1);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binary_e2"), binary_e2);
		
		// nary_a1, nary_a1_r1, nary_a1_r2, nary_a1_r3 nary_e2, nary_e3
		OntClass nary_a1 = selectRandomNaryAssertion(contextModel, false);
		OntProperty nary_a1_r1 = contextModel.getOntProperty(nary_a1.getURI() + "_R1");
		OntProperty nary_a1_r2 = contextModel.getOntProperty(nary_a1.getURI() + "_R2");
		OntProperty nary_a1_r3 = contextModel.getOntProperty(nary_a1.getURI() + "_R3");
		OntResource nary_e2 = nary_a1_r2.getRange();
		OntResource nary_e3 = nary_a1_r3.getRange();
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1"), nary_a1);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r1"), nary_a1_r1);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r2"), nary_a1_r2);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r3"), nary_a1_r3);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_e2"), nary_e2);
		unarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_e3"), nary_e3);
	}
	
	
	private void instantiateBinarySchemeCall(TemplateCall binarySchemeCall, OntModel contextModel, 
			OntModel derivationModel, OntProperty binaryAssertion, OntResource domainClass, OntResource rangeClass) {
		OntProperty assertionRole = contextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_ROLE);
		
		// binaryAssertion, domainClass, rangeClass
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binaryAssertion"), binaryAssertion);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "domainClass"), domainClass);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "rangeClass"), rangeClass);
		
		// unary_a1, unary_e1
		OntClass unary_a1 = selectRandomUnaryAssertion(contextModel, false);
		OntClass unary_e1 = 
			ContextAssertionUtil.getAssertionRoleEntity(contextModel, unary_a1, assertionRole).as(OntClass.class);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "unary_a1"), unary_a1);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "unary_e1"), unary_e1);
		
		// binary_a1, binary_e1, binary_e2
		OntProperty binary_a1 = selectRandomBinaryAssertion(contextModel, false);
		OntClass binary_e1 = binary_a1.getPropertyResourceValue(RDFS.domain).as(OntClass.class);
		OntClass binary_e2 = binary_a1.getPropertyResourceValue(RDFS.range).as(OntClass.class);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binary_a1"), binary_a1);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binary_e1"), binary_e1);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binary_e2"), binary_e2);
		
		// nary_a1, nary_a1_r1, nary_a1_r2, nary_a1_r3 nary_e2, nary_e3
		OntClass nary_a1 = selectRandomNaryAssertion(contextModel, false);
		OntProperty nary_a1_r1 = contextModel.getOntProperty(nary_a1.getURI() + "_R1");
		OntProperty nary_a1_r2 = contextModel.getOntProperty(nary_a1.getURI() + "_R2");
		OntProperty nary_a1_r3 = contextModel.getOntProperty(nary_a1.getURI() + "_R3");
		OntResource nary_e1 = nary_a1_r1.getRange();
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1"), nary_a1);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r1"), nary_a1_r1);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r2"), nary_a1_r2);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r3"), nary_a1_r3);
		binarySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_e1"), nary_e1);
	}
	
	
	private void instantiateNarySchemeCall(TemplateCall narySchemeCall, OntModel contextModel, 
			OntModel derivationModel, OntClass naryAssertion) {
		
		// naryAssertion, naryAssertion_role1, naryAssertion_role2, naryAssertion_role3
		// entityR1Class, entityR2Class, entityR3Class
		OntProperty naryAssertion_role1 = contextModel.getOntProperty(naryAssertion.getURI() + "_R1");
		OntProperty naryAssertion_role2 = contextModel.getOntProperty(naryAssertion.getURI() + "_R2");
		OntProperty naryAssertion_role3 = contextModel.getOntProperty(naryAssertion.getURI() + "_R3");
		OntResource entityR1Class = naryAssertion_role1.getRange();
		OntResource entityR2Class = naryAssertion_role2.getRange();
		OntResource entityR3Class = naryAssertion_role3.getRange();
		
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "naryAssertion"), naryAssertion);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "naryAssertion_role1"), naryAssertion_role1);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "naryAssertion_role2"), naryAssertion_role2);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "naryAssertion_role3"), naryAssertion_role3);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "entityR1Class"), entityR1Class);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "entityR2Class"), entityR2Class);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "entityR3Class"), entityR3Class);
		
		// unary_a1
		OntClass unary_a1 = selectRandomUnaryAssertion(contextModel, false);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "unary_a1"), unary_a1);
		
		// binary_a1
		OntProperty binary_a1 = selectRandomBinaryAssertion(contextModel, false);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "binary_a1"), binary_a1);
		
		// nary_a1, nary_a1_r1, nary_a1_r2, nary_a1_r3, nary_e1, nary_e2, nary_e3
		OntClass nary_a1 = selectRandomNaryAssertion(contextModel, false);
		OntProperty nary_a1_r1 = contextModel.getOntProperty(nary_a1.getURI() + "_R1");
		OntProperty nary_a1_r2 = contextModel.getOntProperty(nary_a1.getURI() + "_R2");
		OntProperty nary_a1_r3 = contextModel.getOntProperty(nary_a1.getURI() + "_R3");
		OntResource nary_e1 = nary_a1_r1.getRange();
		OntResource nary_e2 = nary_a1_r2.getRange();
		OntResource nary_e3 = nary_a1_r3.getRange();
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1"), nary_a1);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r1"), nary_a1_r1);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r2"), nary_a1_r2);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_a1_r3"), nary_a1_r3);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_e1"), nary_e1);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_e2"), nary_e2);
		narySchemeCall.addProperty(derivationModel.getProperty(ARG.NS + "nary_e3"), nary_e3);
	}
	
	private OntClass selectRandomEntity(OntModel contextModel) {
		int contextEntityIndex = randGen.nextInt(configuration.contextEntities.nrTypes) + 1;
		String entityURI = CONTEXT_GEN_MODEL_NS + "E" + contextEntityIndex;
    	OntClass entityClass = contextModel.getOntClass(entityURI);
    	
    	return entityClass;
	}
	
	
	private OntClass selectRandomUnaryAssertion(OntModel contextModel, boolean derivedAllowed) {
		int assertionIndex = 1;
		
		if (derivedAllowed) {
			assertionIndex = randGen.nextInt(configuration.contextAssertions.unary.nrTypes) + 1;
		}
		else {
			int n = configuration.contextAssertions.unary.nrTypes - configuration.contextAssertions.unary.nrDerived;
			assertionIndex = randGen.nextInt(n) + configuration.contextAssertions.unary.nrDerived + 1;
		}
		String assertionURI = CONTEXT_GEN_MODEL_NS + "UnaryA" + assertionIndex;
    	OntClass assertionClass = contextModel.getOntClass(assertionURI);
    	
    	return assertionClass;
	}
	
	
	private OntProperty selectRandomBinaryAssertion(OntModel contextModel, boolean derivedAllowed) {
		int assertionIndex = 1;
		
		if (derivedAllowed) {
			assertionIndex = randGen.nextInt(configuration.contextAssertions.binary.nrTypes) + 1;
		}
		else {
			int n = configuration.contextAssertions.binary.nrTypes - configuration.contextAssertions.binary.nrDerived;
			assertionIndex = randGen.nextInt(n) + configuration.contextAssertions.binary.nrDerived + 1;
		}
		String assertionURI = CONTEXT_GEN_MODEL_NS + "BinaryA" + assertionIndex;
		OntProperty assertionProperty = contextModel.getOntProperty(assertionURI);
    	
    	return assertionProperty;
	}
	
	
	private OntClass selectRandomNaryAssertion(OntModel contextModel, boolean derivedAllowed) {
		int assertionIndex = 1;
		
		if (derivedAllowed) {
			assertionIndex = randGen.nextInt(configuration.contextAssertions.nary.nrTypes) + 1;
		}
		else {
			int n = configuration.contextAssertions.nary.nrTypes - configuration.contextAssertions.nary.nrDerived;
			assertionIndex = randGen.nextInt(n) + configuration.contextAssertions.nary.nrDerived + 1;
		}
		String assertionURI = CONTEXT_GEN_MODEL_NS + "NaryA" + assertionIndex;
    	OntClass assertionClass = contextModel.getOntClass(assertionURI);
    	
    	return assertionClass;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configurationFile = "src/org/aimas/ami/contextrep/test/performance/config.properties";
		Log.setLog4j();
		
		ScenarioSetup scenarioSetup = new ScenarioSetup();
		
		//Gson gson = new Gson();
		//System.out.println(gson.toJson(scenarioSetup.configuration));
	}
	
}
