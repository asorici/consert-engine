package org.aimas.ami.contextrep.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.contextrep.exceptions.ConfigException;
import org.aimas.ami.contextrep.functions.datetimeDelay;
import org.aimas.ami.contextrep.functions.getCurrentAgent;
import org.aimas.ami.contextrep.functions.makeValidityInterval;
import org.aimas.ami.contextrep.functions.newGraphUUID;
import org.aimas.ami.contextrep.functions.now;
import org.aimas.ami.contextrep.functions.timeValidityJoinOp;
import org.aimas.ami.contextrep.functions.timeValidityMeetOp;
import org.aimas.ami.contextrep.functions.validityIntervalsCloseEnough;
import org.aimas.ami.contextrep.functions.validityIntervalsInclude;
import org.aimas.ami.contextrep.functions.validityIntervalsOverlap;
import org.aimas.ami.contextrep.update.ContextAssertionUpdateEngine;
import org.aimas.ami.contextrep.utils.CalendarIntervalListType;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.function.FunctionRegistry;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;

public class Config {
	// Path to context store for persistance
	private static Location contextStoreLocation;
	
	// In memory dataset 
	//private static Dataset contextDataset;
	
	// URI for the entity store 
	private static String entityStoreURI;
	
	// Domain Context Model
	private static String contextModelBaseURI;
	private static OntModel basicContextModel;
	private static OntModel transitiveContextModel;
	private static OntModel rdfsContextModel;
	private static OntModel owlContextModel;
	
	// ContextAssertion Derivation Rule Dictionary
	private static DerivationRuleDictionary derivationRuleDictionary;
	
	// ContextAssertion Constraints Dictionary
	private static ConstraintIndex constraintIndex;
	
	// ContextAssertion index
	private static ContextAssertionIndex contextAssertionIndex;
	
	// Execution: ContextAssertion insertion and ContextAssertion inference-hook execution
	private static int assertionInsertThreadPoolSize;
	private static int assertionInferenceThreadPoolSize; 

	private static ThreadPoolExecutor assertionInsertExecutor;
	private static ThreadPoolExecutor assertionInferenceExecutor;
	
	/**
	 * Do configuration setup:
	 * <ul>
	 * 	<li>parse configuration properties</li>
	 * 	<li>create/open storage dataset</li>
	 * 	<li>load basic context model</li>
	 * 	<li>build transitive, rdfs and mini-owl inference model from basic context model</li>
	 * 	<li>compute derivationRuleDictionary</li>
	 * </ul>
	 */
	public static void init(String configurationFile, boolean printDurations) throws ConfigException {
		long timestamp = System.currentTimeMillis();
		
		// parse configuration properties
		Loader.parseConfigProperties(configurationFile);
		if (printDurations) {
			System.out.println("Task: parse configuration properties. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		// create the in-memory TDB contextStore dataset
		contextStoreLocation = Loader.createOrOpenTDB();
		
		//contextDataset = GraphStoreFactory.create().toDataset();
		Dataset contextDataset = TDBFactory.createDataset(contextStoreLocation);
		
		if (printDurations) {
			System.out.println("Task: create the contextStore. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		// setup ontology document manager paths and retrieve the base URI of the context model domain ontology
		contextModelBaseURI = Loader.setupOntologyDocManager();
		
		// load context model
		basicContextModel = Loader.getBasicContextModel();
		if (printDurations) {
			System.out.println("Task: load context model " + contextModelBaseURI +  ". Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();

		// build inference models
		transitiveContextModel = Loader.getTransitiveInferenceModel(basicContextModel);
		if (printDurations) {
			System.out.println("Task: build transitive inference model. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		
		rdfsContextModel = Loader.getRDFSInferenceModel(basicContextModel);
		if (printDurations) {
			System.out.println("Task: build RDFS inference model. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		
		owlContextModel = Loader.getOWLInferenceModel(basicContextModel);
		if (printDurations) {
			System.out.println("Task: build OWL-MINI inference model. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		// register custom RDF datatypes
		registerCustomRDFDatatypes();
		if (printDurations) {
			System.out.println("Task: register custom RDF datatypes. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		// register custom SPARQL functions
		registerCustomSPARQLFunctions();
		
		if (printDurations) {
			System.out.println("Task: register custom SPARQL functions. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		
		// create the named graph for ContextEntities and EntityDescriptions
		entityStoreURI = Loader.createEntityStoreGraph(contextDataset);
		
		// create the named graph ContextAssertion Stores
		contextAssertionIndex = Loader.createContextAssertionIndex(transitiveContextModel);
		if (printDurations) {
			System.out.println("Task: create the named graph ContextAssertion Stores. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		timestamp = System.currentTimeMillis();
		
		// compute derivation rule dictionary
		derivationRuleDictionary = Loader.buildDerivationRuleDictionary(transitiveContextModel);
		if (printDurations) {
			System.out.println("Task: compute derivation rule dictionary. Duration: " + 
				(System.currentTimeMillis() - timestamp) + " ms");
		}
		//System.out.println("#### Derivation Rule Map : ");
		//System.out.println(derivationRuleDictionary.getAssertion2QueryMap());
		timestamp = System.currentTimeMillis();
		
		// compute constraint dictionary
		constraintIndex = Loader.buildConstraintIndex(contextAssertionIndex, rdfsContextModel);
		
		// register custom TDB UpdateEgine to listen for ContextAssertion insertions
		ContextAssertionUpdateEngine.register();
		
		// lastly create the assertion execution services
		assertionInsertExecutor = createInsertionExecutor();
		assertionInferenceExecutor = createInferenceExecutor();
	}
	

	/**
	 * Clean out all statements from the named graphs in the dataset
	 */
	public static void cleanDataset() {
		Dataset contextStoreDataset = TDBFactory.createDataset(contextStoreLocation); 
		
		Iterator<String> graphNameIt = contextStoreDataset.listNames();
		if (graphNameIt != null) {
			List<String> namedGraphs = new ArrayList<>();
			for ( ; graphNameIt.hasNext() ; ) {
				namedGraphs.add(graphNameIt.next());
			}
			
			for (String graphName : namedGraphs ) {
				contextStoreDataset.removeNamedModel(graphName);
			}
			
			TDB.sync(contextStoreDataset);
		}
	}
	
	
	/**
	 * Close context model and sync TDB-backed dataset before closing
	 */
	public static void close() {
		
		//Dataset contextStoreDataset = TDBFactory.createDataset(contextStoreLocation); 
		// shutdown the executors and await their task termination
		assertionInsertExecutor.shutdown();
		assertionInferenceExecutor.shutdown();
		
		try {
	        assertionInsertExecutor.awaitTermination(10, TimeUnit.SECONDS);
	        assertionInferenceExecutor.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
	        e.printStackTrace();
        }
		
		assertionInsertExecutor.shutdownNow();
		assertionInferenceExecutor.shutdownNow();
		
		owlContextModel.close();
		rdfsContextModel.close();
		transitiveContextModel.close();
		basicContextModel.close();
		
		//contextStoreDataset.close();
	}
	
	private static ThreadPoolExecutor createInsertionExecutor() {
		try {
	        assertionInsertThreadPoolSize = Loader.getInsertThreadPoolSize();
        }
        catch (ConfigException e) {
	        assertionInsertThreadPoolSize = 1;
        }
		
		return (ThreadPoolExecutor)Executors.newFixedThreadPool(assertionInsertThreadPoolSize, 
				new ContextInsertThreadFactory());
	}
	
	private static ThreadPoolExecutor createInferenceExecutor() {
		try {
	        assertionInferenceThreadPoolSize = Loader.getInferenceThreadPoolSize();
        }
        catch (ConfigException e) {
        	assertionInferenceThreadPoolSize = 1;
        }
		return (ThreadPoolExecutor)Executors.newFixedThreadPool(assertionInferenceThreadPoolSize, 
				new ContextInferenceThreadFactory());
	}
	
	
	public static ThreadPoolExecutor assertionInsertExecutor() {
		if (assertionInsertExecutor == null) {
			assertionInsertExecutor = createInsertionExecutor();
		}
		
		return assertionInsertExecutor;
	}
	
	
	public static ThreadPoolExecutor assertionInferenceExecutor() {
		if (assertionInferenceExecutor == null) {
			assertionInferenceExecutor = createInferenceExecutor();
		}
		
		return assertionInferenceExecutor;
	}
	
	
	public static int getAssertionInsertThreadPoolSize() {
		return assertionInsertThreadPoolSize;
	}


	public static int getAssertionInferenceThreadPoolSize() {
		return assertionInferenceThreadPoolSize;
	}

	
	public static Dataset getPersistentContextStore() {
		return TDBFactory.createDataset(contextStoreLocation);
	}
	
	
	public static Dataset getContextDataset() {
		/*
		 *  We're doing it like this such that every thread that asks for the dataset will
		 *  get its own object. Synchronization happens on TDB.sync()
		 */
		return TDBFactory.createDataset(contextStoreLocation);
		//return contextDataset;
	}
	
	public static String getContextModelBaseURI() {
		return contextModelBaseURI;
	}
	
	public static String getContextModelNamespace() {
		return contextModelBaseURI + "#";
	}
	
	public static OntModel getBasicContextModel() {
		return basicContextModel;
	}

	public static OntModel getTransitiveInfContextModel() {
		return transitiveContextModel;
	}

	public static OntModel getRdfsContextModel() {
		return rdfsContextModel;
	}

	public static OntModel getOwlContextModel() {
		return owlContextModel;
	}

	public static DerivationRuleDictionary getDerivationRuleDictionary() {
		return derivationRuleDictionary;
	}
	
	public static ContextAssertionIndex getContextAssertionIndex() {
		return contextAssertionIndex;
	}
	
	public static ConstraintIndex getConstraintIndex() {
		return constraintIndex;
	}
	
	
	public static String getEntityStoreURI() {
		return entityStoreURI;
	}
	
	
	private static void registerCustomRDFDatatypes() {
		RDFDatatype rtype = CalendarIntervalListType.intervalListType;
		TypeMapper.getInstance().registerDatatype(rtype);
	}
	
	
	private static void registerCustomSPARQLFunctions() {
		// register SPIN system functions and templates 
		SPINModuleRegistry.get().init();
		
		// load SPIN custom function module and register the functions and templates it defines
		OntModelSpec functionModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
		OntModel functionModel = ModelFactory.createOntologyModel(functionModelSpec);
		functionModel.read(ContextAssertionVocabulary.FUNCTIONS);
		
		SPINModuleRegistry.get().registerAll(functionModel, null);
		
		// load SPIN elements from attached to the basic context model
		// SPINModuleRegistry.get().registerAll(basicContextModel, null);
		
		// register annotation operator functions
		registerAnnotationOperators();
		
		// register custom filter functions
		registerCustomFilterFunctions();
	}
	
	
	private static void registerAnnotationOperators() {
		// register time validity operators
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "timeValidityMeetOp", 
				timeValidityMeetOp.class) ;
		
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "timeValidityJoinOp", 
				timeValidityJoinOp.class) ;
	}
	
	
	private static void registerCustomFilterFunctions() {
		// register now function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "now", now.class) ;
		
		// register datetimeDelay function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "datetimeDelay", datetimeDelay.class) ;
		
		// register makeValidityInterval function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "makeValidityInterval", 
				makeValidityInterval.class) ;
		
		// register validityIntervalsInclude function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "validityIntervalsInclude", 
				validityIntervalsInclude.class) ;
		
		// register validityIntervalsOverlap function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "validityIntervalsOverlap", 
				validityIntervalsOverlap.class) ;
		
		// register validityIntervalsOverlap function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "validityIntervalsCloseEnough", 
				validityIntervalsCloseEnough.class) ;
		
		// register getCurrentAgent function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "getCurrentAgent", 
				getCurrentAgent.class) ;
		
		// register newGraphUUID function
		FunctionRegistry.get().put(ContextAssertionVocabulary.FUNCTIONS_NS + "newGraphUUID", 
				newGraphUUID.class) ;
	}
	
}
