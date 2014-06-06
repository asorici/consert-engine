package org.aimas.ami.contextrep.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.aimas.ami.contextrep.core.api.ConfigException;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class Loader {
	public static final String CONSERT_PERSISTENT_STORAGE_ASSEMBLER_FILE_DEFAULT = "etc/context-tdb-assembler.ttl";
	public static final String CONSERT_PERSISTENT_STORAGE_DIRECTORY_DEFAULT = "store";
	public static final String CONSERT_MEMORY_STORE_NAME_DEFAULT= "consert-store";
	public static final String CONSERT_ONT_DOCMGR_FILE_DEFAULT = "etc/consert-ont-policy.rdf";
	public static final String SPIN_ONT_DOCMGR_FILE_DEFAULT = "etc/spin-ont-policy.rdf";
	
	
	private static PropertiesConfiguration configProperties;
	private static Map<String, OntDocumentManager> ontDocumentManagers;
	
	/**
	 * Parse and validate (check for required entries) the CONSERT Engine configuration file.
	 * @param configurationFile
	 * @throws ConfigException
	 */
	public static void parseConfigProperties(String configurationFile) throws ConfigException {
		// load the properties file
		configProperties = new PropertiesConfiguration();
		
		try {
			configProperties.load(new FileInputStream(configurationFile));
			validate();
		} catch (FileNotFoundException e) {
			throw new ConfigException("configuration.properties file not found", e);
		} 
        catch (ConfigurationException e) {
        	throw new ConfigException("configuration.properties could not be loaded", e);
        }
	}
	
	private static void validate() throws ConfigException {
	    // Step 1) Check for the required entries detailing the CONSERT Ontology modules
		for (String moduleKey : ConfigKeys.CONSERT_ONT_MODULE_KEYS) {
			if (configProperties.getString(moduleKey) == null) {
				throw new ConfigException("Configuration properties has no value for key: " + moduleKey);
			}
		}
		
		// Step 2) Check for existence of Context Model Core URI (this MUST exist)
		if (configProperties.getString(ConfigKeys.DOMAIN_ONT_CORE_URI) == null) {
			throw new ConfigException("Configuration properties has no value for "
					+ "Context Domain core module key: " + ConfigKeys.DOMAIN_ONT_CORE_URI);
		}
		
		// Step 3) Check for existence of the Context Model document manager file key
		if (configProperties.getString(ConfigKeys.DOMAIN_ONT_DOCMGR_FILE) == null) {
			throw new ConfigException("Configuration properties has no value for "
					+ "Context Domain ontology document manager key: " + ConfigKeys.DOMAIN_ONT_DOCMGR_FILE);
		}
    }
	
	
	public static int getInsertThreadPoolSize() throws ConfigException {
		String sizeStr = configProperties.getString(ConfigKeys.CONSERT_ENGINE_NUM_INSERTION_THREADS, "1");
		
		try {
			return Integer.parseInt(sizeStr);
		}
		catch(NumberFormatException e) {
			throw new ConfigException("Illegal specification for integer size of insertion thread pool", e);
		}
	}
	
	
	public static int getInferenceThreadPoolSize() throws ConfigException {
		String sizeStr = configProperties.getString(ConfigKeys.CONSERT_ENGINE_NUM_INFERENCE_THREADS, "1");
		
		try {
			return Integer.parseInt(sizeStr);
		}
		catch(NumberFormatException e) {
			throw new ConfigException("Illegal specification for integer size of inference thread pool", e);
		}
	}
	
	
	public static int getQueryThreadPoolSize() throws ConfigException {
		String sizeStr = configProperties.getString(ConfigKeys.CONSERT_ENGINE_NUM_QUERY_THREADS, "1");
		
		try {
			return Integer.parseInt(sizeStr);
		}
		catch(NumberFormatException e) {
			throw new ConfigException("Illegal specification for integer size of query thread pool", e);
		}
	}
	
	
	/** Create the memory location name for the runtime contextStore that contains:
	 * <ul>
	 * 	<li> the general store: statement of the context model that refer
	 *   	 to context entities (rdf:type statements and EntityDescriptions) </li>
	 * 	<li> the named graphs that act as Stores for each TYPE of ContextAssertion </li>
	 * 	<li> the named graphs which act as identifiers of the ContextAssertions </li>
	 * </ul>
	 * 
	 *  
	 * @return The {@link Location} of the in-memory TDB backed Dataset that stores the ContextEntities, ContextAssertions and their Annotations
	 * @throws ConfigException if the configuration properties are not initialized (usually because the 
	 * <i>configuration.properties</i> was not found) 
	 */
	public static Location createRuntimeStoreLocation() throws ConfigException {
		if (configProperties != null) {
			String storeMemoryLocation = configProperties.getString(ConfigKeys.MEMORY_STORE_NAME, CONSERT_MEMORY_STORE_NAME_DEFAULT);
			
			TDB.init();
			//TDBFactory.createDataset(tdbStorageDirectory);
			
			return Location.mem(storeMemoryLocation);
			//return new Location(tdbStorageDirectory);
		}
		
		throw new ConfigException();
	}
	
	
	/** Create the directory location for the <i>persistent</i> contextStore that contains:
	 * <ul>
	 * 	<li> the general store: statement of the context model that refer
	 *   	 to context entities (rdf:type statements and EntityDescriptions) </li>
	 * 	<li> the named graphs that act as Stores for each TYPE of ContextAssertion </li>
	 * 	<li> the named graphs which act as identifiers of the ContextAssertions </li>
	 * </ul>
	 * 
	 *  
	 * @return The {@link Location} of the <i>persistent</i> TDB backed Dataset that stores the ContextEntities, ContextAssertions and their Annotations
	 * @throws ConfigException if the configuration properties are not initialized (usually because the 
	 * <i>configuration.properties</i> was not found) 
	 */
	public static Location createPersistentStoreLocation() throws ConfigException {
		if (configProperties != null) {
			String storePersistentDirectory = configProperties.getString(ConfigKeys.PERSISTENT_STORE_DIRECTORY, 
					CONSERT_PERSISTENT_STORAGE_DIRECTORY_DEFAULT);
			
			TDB.init();
			
			return new Location(storePersistentDirectory);
		}
		
		throw new ConfigException();
	}
	
	/**
	 * Create the named graph that acts as the store for ContextEntity and EntityDescription instances
	 * @param dataset The TDB-backed dataset that holds the graphs
	 */
	public static void createEntityStoreGraph(Dataset dataset) {
		Model graphStore = dataset.getNamedModel(ConsertCore.ENTITY_STORE_URI);
		TDB.sync(graphStore);
	}
	
	/**
	 * Use the configuration file to create the map of base URIs for each module of the domain Context Model:
	 * <i>core, annotation, constraints, functions, rules</i>
	 * @return A map of the base URIs for each type of module within the current domain Context Model
	 */
	public static Map<String, String> getContextModelURIs() throws ConfigException {
	    Map<String, String> contextModelURIMap = new HashMap<String, String>();
	    
	    /* build the mapping from context domain model keys to the corresponding URIs (if defined)
	     * If certain module keys are non-existent, only the default CONSERT Engine specific elements 
	     * (e.g. Annotations, Functions) will be loaded and indexed.
	     */
	    String domainCoreURI = configProperties.getString(ConfigKeys.DOMAIN_ONT_CORE_URI);
	    String domainAnnotationURI = configProperties.getString(ConfigKeys.DOMAIN_ONT_ANNOTATION_URI);
	    String domainConstraintURI = configProperties.getString(ConfigKeys.DOMAIN_ONT_CONSTRAINT_URI);
	    String domainFunctionsURI = configProperties.getString(ConfigKeys.DOMAIN_ONT_FUNCTIONS_URI);
	    String domainRulesURI = configProperties.getString(ConfigKeys.DOMAIN_ONT_RULES_URI);
	    
	    // the Context Model core URI must exist
	    contextModelURIMap.put(ConfigKeys.DOMAIN_ONT_CORE_URI, domainCoreURI);
	    
	    if (domainAnnotationURI != null) 
	    	contextModelURIMap.put(ConfigKeys.DOMAIN_ONT_ANNOTATION_URI, domainAnnotationURI);
	    
	    if (domainConstraintURI != null) 
	    	contextModelURIMap.put(ConfigKeys.DOMAIN_ONT_CONSTRAINT_URI, domainConstraintURI);
	    
	    if (domainFunctionsURI != null) 
	    	contextModelURIMap.put(ConfigKeys.DOMAIN_ONT_FUNCTIONS_URI, domainFunctionsURI);
	    
	    if (domainRulesURI != null) 
	    	contextModelURIMap.put(ConfigKeys.DOMAIN_ONT_RULES_URI, domainRulesURI);
	    
	    
	    return contextModelURIMap;
    }
	
	
	/**
	 * Setup the document managers for the CONSERT, SPIN and Context Domain ontologies 
	 * with configuration files taken from the config.properties file
	 */
	private static void setupOntologyDocManagers() throws ConfigException {
		ontDocumentManagers = new HashMap<String, OntDocumentManager>();
		
		String consertOntDocMgrFile = configProperties.getString(ConfigKeys.CONSERT_ONT_DOCMGR_FILE, CONSERT_ONT_DOCMGR_FILE_DEFAULT);
		String spinOntDocMgrFile = configProperties.getString(ConfigKeys.SPIN_ONT_DOCMGR_FILE, SPIN_ONT_DOCMGR_FILE_DEFAULT);
		String domainOntDocMgrFile = configProperties.getString(ConfigKeys.DOMAIN_ONT_DOCMGR_FILE);
		
		// ======== create a document manager configuration for the CONSERT ontology ========
        Model consertDocMgrModel = ModelFactory.createDefaultModel();
        consertDocMgrModel.read(consertOntDocMgrFile);
        OntDocumentManager consertDocManager = new OntDocumentManager(consertDocMgrModel);
        ontDocumentManagers.put(ConfigKeys.CONSERT_ONT_DOCMGR_FILE, consertDocManager);
        
        // ======== create a document manager configuration for the SPIN ontology ========
        Model spinDocMgrModel = ModelFactory.createDefaultModel();
        spinDocMgrModel.read(spinOntDocMgrFile);
        OntDocumentManager spinDocManager = new OntDocumentManager(spinDocMgrModel);
        ontDocumentManagers.put(ConfigKeys.SPIN_ONT_DOCMGR_FILE, spinDocManager);
        
		// ======== create a document manager configuration for the Context Domain ========
        Model domainDocMgrModel = ModelFactory.createDefaultModel();
        
        // read both the CONSERT and domain specific document manager config into it
        domainDocMgrModel.read(consertOntDocMgrFile);
        domainDocMgrModel.read(domainOntDocMgrFile);
        OntDocumentManager domainDocManager = new OntDocumentManager(domainDocMgrModel);
        ontDocumentManagers.put(ConfigKeys.DOMAIN_ONT_DOCMGR_FILE, domainDocManager);
	}
	
	/**
	 * Get the {@link OntDocumentManager} for the specified configuration key.
	 * @param key The configuration key identifying the document manager file for
	 * either the <b>CONSERT ontology</b>, the <b>SPIN ontologies</b> or the <b>Context Domain ontology</b>
	 * @return The OntDocumentManager for the specified configuration key or <b>null</b> if the document managers
	 * have not yet been initialized or no document manager is found for the specified key.
	 */
	public static OntDocumentManager getOntDocumentManager(String configKey) {
		if (ontDocumentManagers != null) {
			return ontDocumentManagers.get(configKey);
		}
		
		return null;
	}
	
	
	/**
	 * Use the configuration file to create the map of ontology models (<b>with applied RDFS inferencing</b>) 
	 * for each module of the domain Context Model: <i>core, annotation, constraints, functions, rules</i>
	 * @param contextModelURIMap The Context Model URI map built by calling {@code getContextModelURIs}
	 * @return A map of the models for each type of module within the current domain Context Model
	 * @see getContextModelURIs
	 */
	public static Map<String, OntModel> getContextModelModules(Map<String, String> contextModelURIMap) throws ConfigException {
		Map<String, OntModel> contextModelMap = new HashMap<String, OntModel>();
		
		// ======== setup document managers for ontology importing ========
        setupOntologyDocManagers();
		
        OntDocumentManager domainDocManager = ontDocumentManagers.get(ConfigKeys.DOMAIN_ONT_DOCMGR_FILE);
        OntModelSpec domainContextModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM_RDFS_INF);
        domainContextModelSpec.setDocumentManager(domainDocManager);
        
        // ======== now we are ready to load all context ontology modules ========
        // 1) build the core context model
        String consertCoreURI = configProperties.getString(ConfigKeys.CONSERT_ONT_CORE_URI);
        String contextModelCoreURI = contextModelURIMap.get(ConfigKeys.DOMAIN_ONT_CORE_URI);
        
        OntModel contextModelCore = ModelFactory.createOntologyModel(domainContextModelSpec);
        contextModelCore.read(consertCoreURI);
        contextModelCore.read(contextModelCoreURI);
        contextModelMap.put(ConfigKeys.DOMAIN_ONT_CORE_URI, contextModelCore);
        
        // 2) build the annotation context model
        String consertAnnotationURI = configProperties.getString(ConfigKeys.CONSERT_ONT_ANNOTATION_URI);
        String contextModelAnnotationURI = contextModelURIMap.get(ConfigKeys.DOMAIN_ONT_ANNOTATION_URI);
        OntModel contextModelAnnotations = ModelFactory.createOntologyModel(domainContextModelSpec);
        
        contextModelAnnotations.read(consertAnnotationURI);
        if (contextModelAnnotationURI != null) {
        	contextModelAnnotations.read(contextModelAnnotationURI);
        }
        contextModelMap.put(ConfigKeys.DOMAIN_ONT_ANNOTATION_URI, contextModelAnnotations);
        
        // 3) build the constraints context model
        String consertConstraintsURI = configProperties.getString(ConfigKeys.CONSERT_ONT_CONSTRAINT_URI);
        String contextModelConstraintsURI = contextModelURIMap.get(ConfigKeys.DOMAIN_ONT_CONSTRAINT_URI);
        OntModel contextModelConstraints = ModelFactory.createOntologyModel(domainContextModelSpec);
        
        contextModelConstraints.read(consertConstraintsURI);
        if (contextModelConstraintsURI != null) {
        	contextModelConstraints.read(contextModelConstraintsURI);
        }
        contextModelMap.put(ConfigKeys.DOMAIN_ONT_CONSTRAINT_URI, contextModelConstraints);
        
        // 4) build the functions context model
        String consertFunctionsURI = configProperties.getString(ConfigKeys.CONSERT_ONT_FUNCTIONS_URI);
        String contextModelFunctionsURI = contextModelURIMap.get(ConfigKeys.DOMAIN_ONT_FUNCTIONS_URI);
        OntModel contextModelFunctions = ModelFactory.createOntologyModel(domainContextModelSpec);
        
        contextModelFunctions.read(consertFunctionsURI);
        if (contextModelFunctionsURI != null) {
        	contextModelFunctions.read(contextModelFunctionsURI);
        }
        contextModelMap.put(ConfigKeys.DOMAIN_ONT_FUNCTIONS_URI, contextModelFunctions);
        
        // 5) build the rules context model
        String consertRulesURI = configProperties.getString(ConfigKeys.CONSERT_ONT_RULES_URI);
        String contextModelRulesURI = contextModelURIMap.get(ConfigKeys.DOMAIN_ONT_RULES_URI);
        OntModel contextModelRules = ModelFactory.createOntologyModel(domainContextModelSpec);
        
        contextModelRules.read(consertRulesURI);
        if (contextModelRulesURI != null) {
        	contextModelRules.read(contextModelRulesURI);
        }
        contextModelMap.put(ConfigKeys.DOMAIN_ONT_RULES_URI, contextModelRules);
        
	    return contextModelMap;
    }
	
	
	public static OntModel getTransitiveInferenceModel(OntModel basicContextModel) {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_TRANS_INF, basicContextModel);
	}
	
	public static OntModel getRDFSInferenceModel(OntModel basicContextModel) {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF, basicContextModel);
	}
	
	public static OntModel getOWLInferenceModel(OntModel basicContextModel) {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF, basicContextModel);
	}
	
	/**
	 * Return an extended {@link OntModel} context domain ontology module (with no entailement specification), 
	 * where the SPL, SPIN and SP namespaces have been added to the <code>baseContextModelModule</code>.
	 * @param baseContextModelModule The context domain ontology module to extend with the SPIN imports
	 * @return An {@link OntModel} with no entailement specification, extended with the contents of the 
	 * 		SPL, SPIN and SP ontologies
	 */
	public static OntModel ensureSPINImported(OntModel baseContextModelModule) {
		Graph baseGraph = baseContextModelModule.getGraph();
		MultiUnion spinUnion = JenaUtil.createMultiUnion();
		
		ensureImported(baseGraph, spinUnion, SP.BASE_URI, SP.getModel());
		ensureImported(baseGraph, spinUnion, SPL.BASE_URI, SPL.getModel());
		ensureImported(baseGraph, spinUnion, SPIN.BASE_URI, SPIN.getModel());
		Model unionModel = ModelFactory.createModelForGraph(spinUnion);
		
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, unionModel.union(baseContextModelModule));
	}
	
	
	/**
	 * Return an extended Jena {@link Model}, where the SPL, SPIN and SP namespaces have been added to 
	 * the <code>baseModel</code>.
	 * @param baseModel The model to extend with the SPIN imports.
	 * @return An {@link Model} extended with the contents of the SPL, SPIN and SP ontologies.
	 */
	public static Model ensureSPINImported(Model baseModel) {
		Graph baseGraph = baseModel.getGraph();
		MultiUnion spinUnion = JenaUtil.createMultiUnion();
		
		ensureImported(baseGraph, spinUnion, SP.BASE_URI, SP.getModel());
		ensureImported(baseGraph, spinUnion, SPL.BASE_URI, SPL.getModel());
		ensureImported(baseGraph, spinUnion, SPIN.BASE_URI, SPIN.getModel());
		Model unionModel = ModelFactory.createModelForGraph(spinUnion);
		
		return unionModel;
	}
	
	private static void ensureImported(Graph baseGraph, MultiUnion union, String baseURI, Model model) {
		if(!baseGraph.contains(Triple.create(NodeFactory.createURI(baseURI), RDF.type.asNode(), OWL.Ontology.asNode()))) {
			union.addGraph(model.getGraph());
		}
	}
}
