package org.aimas.ami.contextrep.test.performance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
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
import org.apache.jena.atlas.logging.LogCtl;
import org.topbraid.spin.statistics.SPINStatistics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
	private static final int DELAY_MILLIS = 100;
	private static final String TEST_DIR = "tests" + File.separator + "performance";
	
	public static AtomicInteger numInferredAssertions = new AtomicInteger(0);
	public static AtomicInteger executedInsertionsTracker = new AtomicInteger(0);
	public static AtomicInteger enqueuedInferenceTracker = new AtomicInteger(0);
	
	public static Map<Integer, Long> insertionTaskEnqueueTime = new HashMap<>();
	public static Map<Integer, Long> inferenceTaskEnqueueTime = new HashMap<>();
	public static Map<Integer, Integer> numNamedGraphs = new HashMap<>();
	
	public static Map<Integer, Future<AssertionInsertResult>> insertionResults = new HashMap<>();
	public static Map<Integer, Future<AssertionInferenceResult>> inferenceResults = new HashMap<>();
	
	public static LinkedList<SPINStatistics> hookApplicationStats = new LinkedList<>();
	
	public static void main(String[] args) {
		String configurationFile = "src/org/aimas/ami/contextrep/test/performance/config.properties";
		LogCtl.setLog4j();
		
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
			
			// ============================= build scenario events ============================== 
			List<ContextEvent> scenarioEvents = buildScenarioEvents(configuration, basicContextModel, dataset);
			
			// ============================= start event generation =============================
			runScenarioEvents(scenarioEvents, configuration, basicContextModel);
			
			// ======================== wait out remaining event processing =====================
			try {
				Thread.sleep(10000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// ======================== show partial context dataset content ====================
			showDatasetContent(basicContextModel);
			
			
			// =============================== collect statistics ===============================
			PerformanceMeasure measureCollecter = collectRunStatistics(configuration);
			
			// ========================= store statistics and graphs ============================
			storeRunStatistics(configuration, measureCollecter);
			
			Config.close();
		}
        catch (ConfigException e) {
	        e.printStackTrace();
        }
	}
	
	
	private static void storeRunStatistics(PerformanceConfig config, PerformanceMeasure measureCollecter) {
		// get JSON output of run statistics
		Gson gsonOut = new GsonBuilder().setPrettyPrinting().create();
		String jsonOutput = gsonOut.toJson(measureCollecter, PerformanceMeasure.class);
		
		// create test output directory if it does not exist 
		File globalTestDir = new File(TEST_DIR);
		if (!globalTestDir.exists()) {
			globalTestDir.mkdirs();
		}
		
		// get the current quantification step: how many events per second are inserted
		int quantificationStep = config.contextAssertions.unary.pushrate
		        + config.contextAssertions.binary.pushrate
		        + config.contextAssertions.nary.pushrate;
		
		/* determine name of current test configuration output directory and create it
		   name is composed of: test_<nrTypes x nrInstances unary>_<nrTypes x nrInstance binary>_
		   						<nrTypes x nrInstances nary>_quantificationStep_assertionThreadNum_inferenceThreadNum;
		*/
		String outputFolderName = TEST_DIR + File.separator + 
				"test" + "_" + 
				config.contextAssertions.unary.nrTypes + "x" + config.contextAssertions.unary.nrInstances + "_" +
				config.contextAssertions.binary.nrTypes + "x" + config.contextAssertions.binary.nrInstances + "_" +
				config.contextAssertions.nary.nrTypes + "x" + config.contextAssertions.nary.nrInstances + "_" +
				quantificationStep + "_" +
				Config.getAssertionInsertThreadPoolSize() + "_" + 
				Config.getAssertionInferenceThreadPoolSize();
		
		File outputFolder = new File(outputFolderName);
		if (!outputFolder.exists()) {
			outputFolder.mkdirs();
		}
		
		// json output filename is timestamp pretty printed
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		String jsonFileName = outputFolderName + File.separator + "test_" + sdf.format(now.getTime()) + ".json";
		
		
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
			        new FileOutputStream(jsonFileName), "utf-8"));
			writer.write(jsonOutput);
		}
		catch (IOException ex) {
		}
		finally {
			try {
				writer.close();
			}
			catch (Exception ex) {
			}
		}
		
		/*
		 * long enqueuedInsertions =
		 * Config.assertionInsertExecutor().getTaskCount(); long
		 * enqueuedInferences =
		 * Config.assertionInferenceExecutor().getTaskCount();
		 * 
		 * long completedInsertions =
		 * Config.assertionInsertExecutor().getCompletedTaskCount(); long
		 * completedInferences =
		 * Config.assertionInferenceExecutor().getCompletedTaskCount();
		 * 
		 * System.out.println("=========== We tracked " +
		 * executedInsertionsTracker.get() + " insertions");
		 * System.out.println("=========== We tracked " +
		 * enqueuedInferenceTracker.get() + " inference attempts");
		 * 
		 * System.out.println("=========== We had " + numEvents +
		 * " generated events"); System.out.println("=========== We had " +
		 * numInferredAssertions.get() + " inferred assertions");
		 * 
		 * System.out.println("=========== Enqueued insertions: " +
		 * enqueuedInsertions + ". Completed: " + completedInsertions +
		 * " ===========");
		 * System.out.println("=========== Enqueued inferences: " +
		 * enqueuedInferences + ". Completed: " + completedInferences +
		 * " ===========");
		 */
    }
	
	
	private static PerformanceMeasure collectRunStatistics(PerformanceConfig configuration) {
	    
		// sort statistics indexes and collect log data
		List<Integer> insertionExecIDs = new LinkedList<Integer>(insertionResults.keySet());
		Collections.sort(insertionExecIDs);
		
		PerformanceMeasure measureCollecter = new PerformanceMeasure();
		measureCollecter.performanceConfig = configuration;
		
		int countInsertion = 0;
		int countContinuityChecks = 0;
		int countConstraintChecks = 0;
		int countInferenceChecks = 0;
		int countDeductionCycleChecks = 0;
		
		int timedOutTasks = 0;
		int MAX_TIMEOUTS = 30;
		
		for (Integer insertExecID : insertionExecIDs) {
			if (timedOutTasks >= MAX_TIMEOUTS)
				break;
			
			try {
				// System.out.println("Assertion Insert " + insertExecID);
				AssertionInsertResult insertRes = insertionResults.get(insertExecID).get(5, TimeUnit.SECONDS);
				
				if (insertRes != null) {
					long enqTime = insertionTaskEnqueueTime.get(insertExecID);
					int insertDelay = (int) (insertRes.getStartTime() - enqTime);
					if (insertDelay < 0)
						insertDelay = 0;
					
					int insertDuration = insertRes.getDuration();
					
					measureCollecter.performanceResult.averageInsertionDelay += insertDelay;
					measureCollecter.performanceResult.averageInsertionDuration += insertDuration;
					
					countInsertion++;
					
					if (insertDelay > measureCollecter.performanceResult.maxInsertionDelay)
						measureCollecter.performanceResult.maxInsertionDelay = insertDelay;
					if (insertDelay < measureCollecter.performanceResult.minInsertionDelay)
						measureCollecter.performanceResult.minInsertionDelay = insertDelay;
					
					if (insertDuration > measureCollecter.performanceResult.maxInsertionDuration)
						measureCollecter.performanceResult.maxInsertionDuration = insertDuration;
					if (insertDuration < measureCollecter.performanceResult.minInsertionDuration)
						measureCollecter.performanceResult.minInsertionDuration = insertDuration;
					
					measureCollecter.performanceResult.insertionDelayHistory.put(insertExecID, insertDelay);
					measureCollecter.performanceResult.insertionDurationHistory.put(insertExecID, insertDuration);
					
					// List<ContextAssertion> insertedAssertions =
					// insertRes.getInsertedAssertions();
					List<ContinuityHookResult> continuityResults = insertRes.continuityResults();
					List<ConstraintHookResult> constraintResults = insertRes.constraintResults();
					
					// System.out.println("	Inserted assertions - ");
					// System.out.println("	continuity results - ");
					if (continuityResults != null) {
						for (ContinuityHookResult continuityRes : continuityResults) {
							int continuityDuration = continuityRes.getDuration();
							
							measureCollecter.performanceResult.averageContinuityCheckDuration += continuityDuration;
							countContinuityChecks++;
							
							if (continuityDuration > measureCollecter.performanceResult.maxContinuityCheckDuration)
								measureCollecter.performanceResult.maxContinuityCheckDuration = continuityDuration;
							if (continuityDuration < measureCollecter.performanceResult.minContinuityCheckDuration)
								measureCollecter.performanceResult.minContinuityCheckDuration = continuityDuration;
						}
					}
					
					// System.out.println("	constraint results - ");
					if (constraintResults != null) {
						for (ConstraintHookResult constraintRes : constraintResults) {
							if (constraintRes.hasConstraint()) {
								int constraintDuration = constraintRes.getDuration();
								
								measureCollecter.performanceResult.averageConstraintCheckDuration += constraintDuration;
								countConstraintChecks++;
								
								if (constraintDuration > measureCollecter.performanceResult.maxConstraintCheckDuration)
									measureCollecter.performanceResult.maxConstraintCheckDuration = constraintDuration;
								if (constraintDuration < measureCollecter.performanceResult.minConstraintCheckDuration)
									measureCollecter.performanceResult.minConstraintCheckDuration = constraintDuration;
							}
						}
					}
				}
				
				// check triggered inference execution if available
				if (inferenceResults.get(insertExecID) != null) {
					AssertionInferenceResult inferenceRes = inferenceResults.get(insertExecID).get(5, TimeUnit.SECONDS);
					if (inferenceRes != null && inferenceRes.inferenceHookResult().hasInferencePossible()) {
						// System.out.println("	inference results - ");
						
						long infEnqTime = inferenceTaskEnqueueTime.get(insertExecID);
						int inferenceDelay = (int) (inferenceRes.getStartTime() - infEnqTime);
						if (inferenceDelay < 0)
							inferenceDelay = 0;
						
						int inferenceDuration = inferenceRes.getDuration();
						
						measureCollecter.performanceResult.averageInferenceDelay += inferenceDelay;
						measureCollecter.performanceResult.averageInferenceCheckDuration += inferenceDuration;
						countInferenceChecks++;
						
						if (inferenceDelay > measureCollecter.performanceResult.maxInferenceDelay)
							measureCollecter.performanceResult.maxInferenceDelay = inferenceDelay;
						if (inferenceDelay < measureCollecter.performanceResult.minInferenceDelay)
							measureCollecter.performanceResult.minInferenceDelay = inferenceDelay;
						
						if (inferenceDuration > measureCollecter.performanceResult.maxInferenceCheckDuration)
							measureCollecter.performanceResult.maxInferenceCheckDuration = inferenceDuration;
						if (inferenceDuration < measureCollecter.performanceResult.minInferenceCheckDuration)
							measureCollecter.performanceResult.minInferenceCheckDuration = inferenceDuration;
						
						measureCollecter.performanceResult.inferenceDelayHistory.put(insertExecID, inferenceDelay);
						measureCollecter.performanceResult.inferenceDurationHistory.put(insertExecID, inferenceDuration);
						
						if (insertRes != null) {
							int countComputed = computeDeductionCycle(insertExecID, insertRes, inferenceRes, measureCollecter);
							countDeductionCycleChecks += countComputed;
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
				timedOutTasks++;
			}
		}
		
		if (countInsertion != 0) {
			measureCollecter.performanceResult.averageInsertionDelay /= countInsertion;
			measureCollecter.performanceResult.averageInsertionDuration /= countInsertion;
		}
		
		if (countContinuityChecks != 0) {
			measureCollecter.performanceResult.averageContinuityCheckDuration /= countContinuityChecks;
		}
		
		if (countConstraintChecks != 0) {
			measureCollecter.performanceResult.averageConstraintCheckDuration /= countConstraintChecks;
		}
		
		if (countInferenceChecks != 0) {
			measureCollecter.performanceResult.averageInferenceCheckDuration /= countInferenceChecks;
			measureCollecter.performanceResult.averageInferenceDelay /= countInferenceChecks;
		}
		
		if (countDeductionCycleChecks != 0) {
			measureCollecter.performanceResult.averageDeductionCycleDuration /= countDeductionCycleChecks;
		}
		
		return measureCollecter;
    }
	
	
	private static int computeDeductionCycle(Integer insertExecID, AssertionInsertResult insertRes,
            AssertionInferenceResult inferenceRes, PerformanceMeasure measureCollecter) {
		int countComputed = 0;
		
		List<ContextAssertion> inferredAssertions = inferenceRes.inferenceHookResult().getInferredAssertions();
		
		if (inferredAssertions != null) {
			for (ContextAssertion inferredAssertion : inferredAssertions) {
				for (int id = insertExecID + 1; id < insertionResults.size(); id++) {
					try {
			            AssertionInsertResult res = insertionResults.get(id).get(5, TimeUnit.SECONDS);
			            if (res != null && res.getInsertedAssertions().contains(inferredAssertion)) {
			            	// found what we were looking for -> compute time
			            	long enqTime = insertionTaskEnqueueTime.get(insertExecID);
			            	long deducedExecTime = res.getStartTime();
			            	int deducedInsertionDuration = res.getDuration();
			            	
			            	int deductionCycleDuration = (int)(deducedExecTime - enqTime) + deducedInsertionDuration;
			            	countComputed++;
			            	
							measureCollecter.performanceResult.averageDeductionCycleDuration += deductionCycleDuration;
							if (deductionCycleDuration > measureCollecter.performanceResult.maxDeductionCycleDuration)
								measureCollecter.performanceResult.maxDeductionCycleDuration = deductionCycleDuration;
							if (deductionCycleDuration < measureCollecter.performanceResult.minDeductionCycleDuration)
								measureCollecter.performanceResult.minDeductionCycleDuration = deductionCycleDuration;
							
							measureCollecter.performanceResult.deductionCycleHistory.put(insertExecID, deductionCycleDuration);
							
							break;
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
			}
		}
		return countComputed;
    }


	private static void showDatasetContent(OntModel basicContextModel) {
		System.out.println("######## DATASTORE ########");
		Dataset dataset = Config.getContextDataset();
		
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
	
	
	private static void runScenarioEvents(List<ContextEvent> scenarioEvents, PerformanceConfig configuration, OntModel basicContextModel) {
		// ============================= start event generation =============================
		while (!scenarioEvents.isEmpty()) {
			Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			
			int nrEvents = scenarioEvents.size();
			//System.out.println("Number of events: " + nrEvents);
			
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
					//System.out.println("GENERATING EVENT: " + event);
					ContextUpdateExecutionWrapper insertTask = new ContextUpdateExecutionWrapper(event.getUpdateRequest());
					Future<AssertionInsertResult> taskResult = Config.assertionInsertExecutor().submit(insertTask);
					
					insertionTaskEnqueueTime.put(insertTask.getAssertionInsertID(), System.currentTimeMillis());
					insertionResults.put(insertTask.getAssertionInsertID(), taskResult);
					
					//Dataset dataset = Config.getContextDataset();
					//numNamedGraphs.put(insertTask.getAssertionInsertID(), dataset.)
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
		
		// ======== STEP 2a - generate all unary context assertions ========
		OntProperty assertionRole = basicContextModel.getOntProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_ROLE);
		String assertedByURI = configuration.contextAssertions.unary.annotation.assertedBy;
		int validity = configuration.contextAssertions.unary.annotation.duration;  	// get the validity duration
																					// this is also the gap 
																					// between two successive insertions
		
		List<UnaryAssertionEvent> unaryEvents = new LinkedList<>();
		
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
					timestamp.add(Calendar.MILLISECOND, j * validity);
					
					// set random accuracy
					double accuracy = Math.round(ScenarioSetup.randGen.nextDouble() * 100) / 100.0;
					
					UnaryAssertionEvent event = new UnaryAssertionEvent(basicContextModel, unaryAssertion, roleEntityInstance, 
							timestamp, validity, accuracy, assertedByURI);
					unaryEvents.add(event);
				}
			}
		}
		
		distributeEventsInTime(unaryEvents, configuration.contextAssertions.unary.annotation.duration, 
				configuration.contextAssertions.unary.pushrate);
		
		
		// ======== STEP 2b - generate all binary assertions ========
		validity = configuration.contextAssertions.binary.annotation.duration;
		List<BinaryAssertionEvent> binaryEvents = new LinkedList<>();
		
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
					timestamp.add(Calendar.MILLISECOND, j * validity);
					
					// set random accuracy
					double accuracy = Math.round(ScenarioSetup.randGen.nextDouble() * 100) / 100.0;
					
					BinaryAssertionEvent event = new BinaryAssertionEvent(basicContextModel, binaryAssertion, 
							domainEntityInstance, rangeEntityInstance, 
							timestamp, validity, accuracy, assertedByURI);
					binaryEvents.add(event);
				}
			}
		}
		
		distributeEventsInTime(binaryEvents, configuration.contextAssertions.binary.annotation.duration, 
				configuration.contextAssertions.binary.pushrate);
		
		// ======== STEP 2c - generate all nary assertions ========
		validity = configuration.contextAssertions.nary.annotation.duration;
		List<NaryAssertionEvent> naryEvents = new LinkedList<>();
		
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
					timestamp.add(Calendar.MILLISECOND, j * validity);
					
					// set random accuracy
					double accuracy = Math.round(ScenarioSetup.randGen.nextDouble() * 100) / 100.0;
					
					NaryAssertionEvent event = new NaryAssertionEvent(
					        basicContextModel, naryAssertion,
					        naryAssertion_role1, naryAssertion_role2, naryAssertion_role3,  
					        roleEntityInstance1, roleEntityInstance2, roleEntityInstance3,  
					        timestamp, validity, accuracy, assertedByURI);
					naryEvents.add(event);
				}
			}
		}		
		
		distributeEventsInTime(naryEvents, configuration.contextAssertions.nary.annotation.duration, 
				configuration.contextAssertions.nary.pushrate);
		
		
		// STEP 3 - add all event types into one big list and sort it by the timestamp of the events
		events.addAll(unaryEvents);
		events.addAll(binaryEvents);
		events.addAll(naryEvents);
		
		Collections.sort(events);
		
		return events;
    }
	
	
	private static void distributeEventsInTime(List<? extends ContextEvent> events, int validity, int pushrate) {
		// form a list of indexes
		List<Integer> eventIndexes = new LinkedList<>();
		for (int i = 0; i < events.size(); i++) {
			eventIndexes.add(i);
		}
		
		Calendar timestampInit = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		int step = 0;
		while (!eventIndexes.isEmpty()) {
			
			if (eventIndexes.size() >= pushrate) {
				for (int i = 0; i < pushrate; i++) {
					int rIdx = ScenarioSetup.randGen.nextInt(eventIndexes.size());
					int index = eventIndexes.remove(rIdx);
					
					ContextEvent event = events.get(index);
					
					// set timestamp
					Calendar timestamp = (Calendar) timestampInit.clone();
					timestamp.add(Calendar.MILLISECOND, step * validity);
					event.setTimestamp(timestamp);
				}
			}
			else {
				for (int i = 0; i < eventIndexes.size(); i++) {
					int index = eventIndexes.remove(i);
					ContextEvent event = events.get(index);
					
					// set timestamp
					Calendar timestamp = (Calendar) timestampInit.clone();
					timestamp.add(Calendar.MILLISECOND, step * validity);
					event.setTimestamp(timestamp);
				}
			}
			
			step++;
		}
	}
}
