package org.aimas.ami.contextrep.test.performance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.exceptions.ConfigException;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.test.ContextEvent;
import org.aimas.ami.contextrep.test.adhocmeeting.ScenarioInit;
import org.aimas.ami.contextrep.update.AssertionInferenceResult;
import org.aimas.ami.contextrep.update.AssertionInsertResult;
import org.aimas.ami.contextrep.update.ConstraintHookResult;
import org.aimas.ami.contextrep.update.ContextUpdateExecutionWrapper;
import org.aimas.ami.contextrep.update.ContinuityHookResult;
import org.aimas.ami.contextrep.utils.ContextAssertionUtil;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;
import org.openjena.atlas.logging.Log;
import org.topbraid.spin.statistics.SPINStatistics;

import com.google.gson.Gson;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class RunTest {
	private static final int DELAY_MILLIS = 200;
	public static AtomicInteger numInferredAssertions = new AtomicInteger(0);
	public static AtomicInteger executedInsertionsTracker = new AtomicInteger(0);
	public static AtomicInteger enqueuedInferenceTracker = new AtomicInteger(0);
	
	public static Map<Integer, Future<AssertionInsertResult>> insertionResults = new HashMap<>();
	public static Map<Integer, Future<AssertionInferenceResult>> inferenceResults = new HashMap<>();
	
	public static LinkedList<SPINStatistics> hookApplicationStats = new LinkedList<>();
	
	public static void main(String[] args) {
		String configurationFile = "src/org/aimas/ami/contextrep/test/performance/config.properties";
		Log.setLog4j();
		
		try {
			// read performance setup configuration
			Gson gson = new Gson();
			String jsonConfig = readFile(ScenarioSetup.CONFIG_FILE_PATH);
			PerformanceConfig configuration = gson.fromJson(jsonConfig, PerformanceConfig.class);
			
			// init generated context model configuration
			Config.init(configurationFile, true);
			Config.cleanDataset();
			
			Dataset dataset = Config.getContextDataset();
			OntModel basicContextModel = Config.getBasicContextModel();
			
			List<ContextEvent> scenarioEvents = buildScenarioEvents(configuration, basicContextModel, dataset);
			int numEvents = scenarioEvents.size();
			
			// ============================= start event generation =============================
			while (!scenarioEvents.isEmpty()) {
				Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				
				int nrEvents = scenarioEvents.size();
				System.out.println("Number of events: " + nrEvents);
				
				for (int i = 0; i < nrEvents; i++) {
					ContextEvent event = scenarioEvents.get(i);
					if (event.getTimestamp().after(now)) {
						break;
					}
					else {
						scenarioEvents.remove(i);
						i--;
						nrEvents--;
						
						// wrap event for execution and send it to insert
						// executor
						System.out.println("GENERATING EVENT: " + event);
						ContextUpdateExecutionWrapper insertTask = new ContextUpdateExecutionWrapper(event.getUpdateRequest()); 
						Future<AssertionInsertResult> taskResult = Config.assertionInsertExecutor().submit(insertTask);
						insertionResults.put(insertTask.getAssertionInsertID(), taskResult);
					}
				}
				
				try {
					Thread.sleep(DELAY_MILLIS);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			System.out.println("SCENARIO GENERATOR " + " stopping");
			
			// ============================== end event generation ==============================
			try {
				Thread.sleep(10000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("######## DATASTORE ########");
			dataset = Config.getContextDataset();
			
			dataset.begin(ReadWrite.READ);
			Iterator<String> dataStoreNameIt = dataset.listNames();
			for (; dataStoreNameIt.hasNext();) {
				System.out.println(dataStoreNameIt.next());
			}
			
			System.out.println();
			
			Property assertionResourceProp = basicContextModel.getProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_RESOURCE);
			
			// unary check
			OntResource unaryResource = basicContextModel.getOntClass(ScenarioSetup.CONTEXT_GEN_MODEL_NS + "UnaryA2");
			ContextAssertion unaryAssertion = Config.getContextAssertionIndex().getAssertionFromResource(unaryResource);
			Model unaryA2StoreModel = dataset.getNamedModel(unaryAssertion.getAssertionStoreURI());
			ScenarioInit.printStatements(unaryA2StoreModel);
			
			ResIterator derivedUnaries = unaryA2StoreModel.listResourcesWithProperty(assertionResourceProp, unaryResource);
			for (; derivedUnaries.hasNext(); ) {
				Resource unaryIDGraph = derivedUnaries.next();
				System.out.println("======================= Contents of " + unaryIDGraph + " ======================= ");
				
				Model derivedUnaryModel = dataset.getNamedModel(unaryIDGraph.getURI());
				ScenarioInit.printStatements(derivedUnaryModel);
				System.out.println();
			}
			
			// binary check
			OntResource binaryResource = basicContextModel.getOntProperty(ScenarioSetup.CONTEXT_GEN_MODEL_NS + "BinaryA2");
			ContextAssertion binaryAssertion = Config.getContextAssertionIndex().getAssertionFromResource(binaryResource);
			Model binaryA2StoreModel = dataset.getNamedModel(binaryAssertion.getAssertionStoreURI());
			
			ResIterator derivedBinaries = binaryA2StoreModel.listResourcesWithProperty(assertionResourceProp, binaryResource);
			for (; derivedBinaries.hasNext(); ) {
				Resource binaryIDGraph = derivedBinaries.next();
				System.out.println("======================= Contents of " + binaryIDGraph + " ======================= ");
				
				Model derivedBinaryModel = dataset.getNamedModel(binaryIDGraph.getURI());
				ScenarioInit.printStatements(derivedBinaryModel);
				System.out.println();
			}
			
			// nary check
			OntResource naryResource = basicContextModel.getOntClass(ScenarioSetup.CONTEXT_GEN_MODEL_NS + "NaryA2");
			ContextAssertion naryAssertion = Config.getContextAssertionIndex().getAssertionFromResource(naryResource);
			Model naryA2StoreModel = dataset.getNamedModel(naryAssertion.getAssertionStoreURI());
			
			ResIterator derivedNaries = naryA2StoreModel.listResourcesWithProperty(assertionResourceProp, naryResource);
			for (; derivedNaries.hasNext(); ) {
				Resource naryIDGraph = derivedNaries.next();
				System.out.println("======================= Contents of " + naryIDGraph + " ======================= ");
				
				Model derivedNaryModel = dataset.getNamedModel(naryIDGraph.getURI());
				ScenarioInit.printStatements(derivedNaryModel);
				System.out.println();
			}
			
			dataset.end();
			
			
			// sort statistics
			List<Integer> insertionExecIDs = new LinkedList<Integer>(insertionResults.keySet());
			Collections.sort(insertionExecIDs);
			
			for (Integer insertExecID : insertionExecIDs) {
				try {
					System.out.println("Assertion Insert " + insertExecID);
	                AssertionInsertResult insertRes = insertionResults.get(insertExecID).get(5, TimeUnit.SECONDS);
	                
	                if(insertRes != null) {
	                	System.out.println("	timestamp: " + insertRes.getStartTime());
	                	System.out.println("	duration: " + insertRes.getDuration() + "ms");
	                	
	                	ContextAssertion assertion = insertRes.getAssertion();
	                	List<ContinuityHookResult> continuityResults = insertRes.continuityResults();
	                	List<ConstraintHookResult> constraintResults = insertRes.constraintResults();
	                	
	                	if (assertion != null) {
	                		System.out.println("	Assertion: " + 
	                		insertRes != null ? insertRes.getAssertion().getOntologyResource().getLocalName() : "UNKNOWN");
	                	}
	                	
	                	System.out.println("	continuity results - ");
	                	if (continuityResults != null) {
		                	for (ContinuityHookResult continuityRes : continuityResults) {
		                		System.out.println("		available: " + continuityRes.hasContinuity());
		                		System.out.println("		timestamp: " + continuityRes.getStartTime());
		                		System.out.println("		duration: " + continuityRes.getDuration() + "ms");
		                	}
	                	}
	                	
	                	System.out.println("	constraint results - ");
	                	if (constraintResults != null) {
		                	for (ConstraintHookResult constraintRes : constraintResults) {
		                		System.out.println("		available: " + constraintRes.hasConstraint());
		                		System.out.println("		violation: " + constraintRes.hasViolation());
		                		System.out.println("		timestamp: " + constraintRes.getStartTime());
		                		System.out.println("		duration: " + constraintRes.getDuration() + "ms");
		                	}
	                	}
	                	
	                	// check triggered inference execution if available
	                	if (inferenceResults.get(insertExecID) != null) {
		                	AssertionInferenceResult inferenceRes = inferenceResults.get(insertExecID).get(5, TimeUnit.SECONDS);
		                	if (inferenceRes != null) {
		                		System.out.println("	inference results - ");
		                		System.out.println("		available: " + inferenceRes.inferenceHookResult().hasInferencePossible());
		                		System.out.println("		timestamp: " + inferenceRes.getStartTime());
		                		System.out.println("		duration: " + inferenceRes.getDuration());
		                	}
	                	}
	                }
	                
                }
                catch (InterruptedException e) {
	                e.printStackTrace();
                }
                catch (ExecutionException e) {
	                e.printStackTrace();
                }
                catch (TimeoutException e) {
	                e.printStackTrace();
                }
			}
			
			long enqueuedInsertions = Config.assertionInsertExecutor().getTaskCount();
			long enqueuedInferences = Config.assertionInferenceExecutor().getTaskCount();
			
			long completedInsertions = Config.assertionInsertExecutor().getCompletedTaskCount();
			long completedInferences = Config.assertionInferenceExecutor().getCompletedTaskCount();
			
			System.out.println("=========== We tracked " + executedInsertionsTracker.get() + " insertions");
			System.out.println("=========== We tracked " + enqueuedInferenceTracker.get() + " inference attempts");
			
			System.out.println("=========== We had " + numEvents + " generated events");
			System.out.println("=========== We had " + numInferredAssertions.get() + " inferred assertions");
			
			System.out.println("=========== Enqueued insertions: " + enqueuedInsertions + ". Completed: " + completedInsertions + " ===========");
            System.out.println("=========== Enqueued inferences: " + enqueuedInferences + ". Completed: " + completedInferences + " ===========");
			
			Config.close();
		}
        catch (ConfigException e) {
	        e.printStackTrace();
        }
	}
	
	private static String readFile(String filename) {
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
	
	private static List<ContextEvent> buildScenarioEvents(PerformanceConfig configuration, 
			OntModel basicContextModel, Dataset dataset) {
	    
		System.out.println("BUILDING THE SCENARIO");
		
		// STEP 1 - populate the entityStore with the ContextEntity instances
		String entityStoreURI = Config.getEntityStoreURI();
		Model entityStoreModel = dataset.getNamedModel(entityStoreURI);
		
		for (int i = 0; i < configuration.contextEntities.nrTypes; i++) {
			String entityURI = ScenarioSetup.CONTEXT_GEN_MODEL_NS + "E" + (i + 1);
			OntClass entityClass = basicContextModel.getOntClass(entityURI);
			
			for (int j = 0; j < configuration.contextEntities.nrInstances; j++) {
				String entityInstanceURI = entityURI + "_I" + (j + 1);
				entityStoreModel.add(ResourceFactory.createResource(entityInstanceURI), RDF.type, entityClass);
			}
		}
		
		// write to TDB
		// TDB.sync(entityStoreModel);
		
		// STEP 2 - create the sequence of context assertion insertion events
		List<ContextEvent> events = new ArrayList<>();
		
		// STEP 2a - generate sequence for all unary context assertions
		OntProperty assertionRole = basicContextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_ROLE);
		String assertedByURI = configuration.contextAssertions.unary.annotation.assertedBy;
		int validity = configuration.contextAssertions.unary.annotation.duration;  	// get the validity duration
																					// this is also the gap 
																					// between two successive insertions
		
		for (int i = 0; i < configuration.contextAssertions.unary.nrTypes; i++) {
			if (i >= configuration.contextAssertions.unary.nrDerived) {
				// get a timestamp
				Calendar timestampInit = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				
				for (int j = 0; j < configuration.contextAssertions.unary.nrInstances; j++) {
					String unaryAssertionURI = ScenarioSetup.CONTEXT_GEN_MODEL_NS + "UnaryA" + (i + 1);
					OntClass unaryAssertion = basicContextModel.getOntClass(unaryAssertionURI);
					Resource unaryRoleEntityClass = ContextAssertionUtil
							.getAssertionRoleEntity(basicContextModel, unaryAssertion, assertionRole);
					
					// select random roleEntityinstance
					int randIdx = ScenarioSetup.randGen.nextInt(configuration.contextEntities.nrInstances);
					String roleInstanceURI = unaryRoleEntityClass.getURI() + "_I" + (randIdx + 1);
					Resource roleEntityInstance = entityStoreModel.getResource(roleInstanceURI);
					
					// set timestamp
					Calendar timestamp = (Calendar)timestampInit.clone();
					timestamp.add(Calendar.SECOND, j * validity);
					
					// set random accuracy
					double accuracy = Math.round(ScenarioSetup.randGen.nextDouble() * 100) / 100.0;
					
					ContextEvent event = new UnaryAssertionEvent(basicContextModel, unaryAssertion, roleEntityInstance, 
							timestamp, validity, accuracy, assertedByURI);
					events.add(event);
				}
			}
		}
		
		
		// STEP 2b - generate sequence for all binary assertions
		for (int i = 0; i < configuration.contextAssertions.binary.nrTypes; i++) {
			if (i >= configuration.contextAssertions.binary.nrDerived) {
				// get a timestamp
				Calendar timestampInit = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				
				for (int j = 0; j < configuration.contextAssertions.binary.nrInstances; j++) {
					String binaryAssertionURI = ScenarioSetup.CONTEXT_GEN_MODEL_NS + "BinaryA" + (i + 1);
					OntProperty binaryAssertion = basicContextModel.getOntProperty(binaryAssertionURI);
					Resource domainClass = binaryAssertion.getDomain();
					Resource rangeClass = binaryAssertion.getRange();
					
					// select random domainEntity and rangeEntity
					int randIdx = ScenarioSetup.randGen.nextInt(configuration.contextEntities.nrInstances);
					String domainInstanceURI = domainClass.getURI() + "_I" + (randIdx + 1);
					Resource domainEntityInstance = entityStoreModel.getResource(domainInstanceURI);
					
					randIdx = ScenarioSetup.randGen.nextInt(configuration.contextEntities.nrInstances);
					String rangeInstanceURI = rangeClass.getURI() + "_I" + (randIdx + 1);
					Resource rangeEntityInstance = entityStoreModel.getResource(rangeInstanceURI);
					
					// set timestamp
					Calendar timestamp = (Calendar)timestampInit.clone();
					timestamp.add(Calendar.SECOND, j * validity);
					
					// set random accuracy
					double accuracy = Math.round(ScenarioSetup.randGen.nextDouble() * 100) / 100.0;
					
					ContextEvent event = new BinaryAssertionEvent(basicContextModel, binaryAssertion, 
							domainEntityInstance, rangeEntityInstance, 
							timestamp, validity, accuracy, assertedByURI);
					events.add(event);
				}
			}
		}
		
		// STEP 2c - generate sequence for all binary assertions
		for (int i = 0; i < configuration.contextAssertions.nary.nrTypes; i++) {
			if (i >= configuration.contextAssertions.nary.nrDerived) {
				// get a timestamp
				Calendar timestampInit = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				
				for (int j = 0; j < configuration.contextAssertions.nary.nrInstances; j++) {
					String naryAssertionURI = ScenarioSetup.CONTEXT_GEN_MODEL_NS + "NaryA" + (i + 1);
					OntClass naryAssertion = basicContextModel.getOntClass(naryAssertionURI);
					OntProperty naryAssertion_role1 = basicContextModel.getOntProperty(naryAssertion.getURI() + "_R1");
					OntProperty naryAssertion_role2 = basicContextModel.getOntProperty(naryAssertion.getURI() + "_R2");
					OntProperty naryAssertion_role3 = basicContextModel.getOntProperty(naryAssertion.getURI() + "_R3");
					
					Resource naryRoleClass1 = naryAssertion_role1.getRange();
					Resource naryRoleClass2 = naryAssertion_role2.getRange();
					Resource naryRoleClass3 = naryAssertion_role3.getRange();
					
					// select random roleEntity instances
					int randIdx = ScenarioSetup.randGen.nextInt(configuration.contextEntities.nrInstances);
					String roleInstanceURI1 = naryRoleClass1.getURI() + "_I" + (randIdx + 1);
					Resource roleEntityInstance1 = entityStoreModel.getResource(roleInstanceURI1);
					
					randIdx = ScenarioSetup.randGen.nextInt(configuration.contextEntities.nrInstances);
					String roleInstanceURI2 = naryRoleClass2.getURI() + "_I" + (randIdx + 1);
					Resource roleEntityInstance2 = entityStoreModel.getResource(roleInstanceURI2);
							
					randIdx = ScenarioSetup.randGen.nextInt(configuration.contextEntities.nrInstances);
					String roleInstanceURI3 = naryRoleClass3.getURI() + "_I" + (randIdx + 1);
					Resource roleEntityInstance3 = entityStoreModel.getResource(roleInstanceURI3);
					
					// set timestamp
					Calendar timestamp = (Calendar) timestampInit.clone();
					timestamp.add(Calendar.SECOND, j * validity);
					
					// set random accuracy
					double accuracy = Math.round(ScenarioSetup.randGen
					        .nextDouble() * 100) / 100.0;
					
					ContextEvent event = new NaryAssertionEvent(
					        basicContextModel, naryAssertion,
					        naryAssertion_role1, naryAssertion_role2, naryAssertion_role3,  
					        roleEntityInstance1, roleEntityInstance2, roleEntityInstance3,  
					        timestamp, validity, accuracy, assertedByURI);
					events.add(event);
				}
			}
		}
		
		Collections.sort(events);
		
		return events;
    }
}
