package org.aimas.ami.contextrep.test.adhocmeeting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.core.ContextARQFactory;
import org.aimas.ami.contextrep.core.DerivationRuleDictionary;
import org.aimas.ami.contextrep.core.api.ConfigException;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.test.ContextEvent;
import org.aimas.ami.contextrep.update.ContextUpdateExecutionWrapper;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;
import org.aimas.ami.contextrep.vocabulary.JenaVocabulary;
import org.aimas.ami.contextrep.vocabulary.ConsertRules;
import org.apache.jena.atlas.logging.LogCtl;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.CommandWrapper;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.tdb.TDB;

public class PlayScenario {
	
	public static final int ADVANCE_MIN_SKEL = -7;
	public static final int ADVANCE_MIN_MIC = -3;
	private static final int DELAY_MILLIS = 1000;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//PropertyConfigurator.configure("log4j.properties");
		String configurationFile = "src/org/aimas/ami/contextrep/test/adhocmeeting/config.properties";
		LogCtl.setLog4j();
		
		try {
			// init configuration
			Engine.init(configurationFile, true);
			Engine.cleanPersistentContextStore();
			
			Dataset dataset = Engine.getRuntimeContextStore();
			OntModel basicContextModel = Engine.getCoreContextModel();
			OntModel basicScenarioModel = ScenarioInit.initScenario(dataset, basicContextModel);
			
			//attempSPINInference(dataset, basicContextModel);
			List<ContextEvent> scenarioEvents = buildScenarioEvents(basicScenarioModel, basicContextModel, dataset);
			
			
			// ============================= start event generation =============================
			while (!scenarioEvents.isEmpty()) {
				Calendar nowSkel = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				Calendar nowMic = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				nowSkel.add(Calendar.MINUTE, ADVANCE_MIN_SKEL);
				nowMic.add(Calendar.MINUTE, ADVANCE_MIN_MIC);
				
				int nrEvents = scenarioEvents.size();
				//System.out.println("Number of events: " + nrEvents);
				
				for (int i = 0; i < nrEvents; i++) {
					ContextEvent event = scenarioEvents.get(i);
					if (event instanceof SenseSkeletonSittingEvent && event.getTimestamp().after(nowSkel)) {
						break;
					}
					else if (event instanceof HasNoiseLevelEvent && event.getTimestamp().after(nowMic)) {
						break;
					}
					else {
						scenarioEvents.remove(i);
						i--;
						nrEvents--;
						
						// wrap event for execution and send it to insert executor
						System.out.println("GENERATING EVENT: " + event);
						Engine.assertionInsertExecutor().submit(new ContextUpdateExecutionWrapper(event.getUpdateRequest()));
					}
				}
				
				try {
					Thread.sleep(DELAY_MILLIS);
				} 
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			System.out.println("SCENARIO GENERATOR THREAD" + " stopping");
			
			// ============================== end event generation ==============================
			
			try {
	            Thread.sleep(5000);
            }
            catch (InterruptedException e) {
	            e.printStackTrace();
            }
			
			
			System.out.println("######## DATASTORE ########");
			dataset = Engine.getRuntimeContextStore();
			
			dataset.begin(ReadWrite.READ);
			Iterator<String> dataStoreNameIt = dataset.listNames();
			for ( ; dataStoreNameIt.hasNext(); ) {
				System.out.println(dataStoreNameIt.next());
			}
			
			/*
			OntResource sensesSkelAssertion = basicContextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "SensesSkelInPosition");
			Model sensesSkeletonStore = dataset.getNamedModel(Config.getStoreForAssertion(sensesSkelAssertion)); 
			ScenarioInit.printStatements(sensesSkeletonStore);
			*/
			
			dataset.end();
			
			Engine.close(false);
		} catch (ConfigException e) {
			e.printStackTrace();
		}
	}
	
	private static void attempSPINInference(Dataset dataset, OntModel basicContextModel) {
		System.out.println("#### Attempting SPIN Inference ####");
		long timestamp = System.currentTimeMillis();
		
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
		
		DerivationRuleDictionary ruleDict = Engine.getDerivationRuleDictionary();
		OntProperty hasNoiseLevelProperty = basicContextModel.getOntProperty(ScenarioInit.AD_HOC_MEETING_NS + "hasNoiseLevel");
		
		//System.out.println(Config.getContextAssertionIndex().getAssertion2StoreMap());
		
		Model queryModel = dataset.getNamedModel(JenaVocabulary.UNION_GRAPH_URN);
		
		//System.out.println("Query Model as default dataset graph: should be union of all named graphs");
		//ScenarioInit.printStatements(queryModel);
		
		// Create Model for inferred triples
		//Model newTriples = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
		Model newTriples = ModelFactory.createDefaultModel();
		
		List<DerivedAssertionWrapper> derivationCommands = 
			ruleDict.getDerivationsForAssertion(Engine.getContextAssertionIndex().getAssertionFromResource(hasNoiseLevelProperty));
		Map<Resource, List<CommandWrapper>> cls2Query = new HashMap<>();
		Map<Resource, List<CommandWrapper>> cls2Constructor = new HashMap<>();
		Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings = 
				new HashMap<CommandWrapper, Map<String,RDFNode>>();
		SPINRuleComparator comparator = new DefaultSPINRuleComparator(queryModel);
		
		for (DerivedAssertionWrapper derivationWrapper : derivationCommands) {
			Resource entityRes = ruleDict.getEntityForDerivation(derivationWrapper);
			CommandWrapper cmd = derivationWrapper.getDerivationCommand();
			
			Map<String, RDFNode> binding = new HashMap<>();
			String graphUUID = GraphUUIDGenerator.createUUID(ScenarioInit.AD_HOC_MEETING_BASE, "HostsAdHocMeeting");
			RDFNode graphUUIDNode = ResourceFactory.createResource(graphUUID);
			binding.put("graphUUID", graphUUIDNode);
			
			initialTemplateBindings.put(cmd, binding);
			
			if (entityRes != null) {
				List<CommandWrapper> entityCommandWrappers = cls2Query.get(entityRes); 
				
				if (entityCommandWrappers == null) {
					entityCommandWrappers = new ArrayList<>();
					entityCommandWrappers.add(cmd);
					cls2Query.put(entityRes, entityCommandWrappers);
				}
				else {
					entityCommandWrappers.add(cmd);
				}
			}
		}
		//cls2Query.put(hasNoiseLevelProperty, commands);
		
		//ARQ.setExecutionLogging(Explain.InfoLevel.FINE);
		ARQFactory.set(new ContextARQFactory());
		//SPINInferences.run(queryModel, newTriples, cls2Query, cls2Constructor, initialTemplateBindings, null, null, true, SPINVocabulary.deriveAssertionRule, comparator, null);
		SPINInferences.run(queryModel, newTriples, cls2Query, cls2Constructor, null, null, true, ConsertRules.DERIVE_ASSERTION, comparator, null);
		
		System.out.println("[INFO] Ran SPIN Inference. Duration: " + 
			(System.currentTimeMillis() - timestamp) );
		
		TDB.sync(dataset);
		
		// see if it worked
		ScenarioInit.printStatements(dataset.getNamedModel(ScenarioInit.AD_HOC_MEETING_BASE + "/" + "HostsAdHocMeetingStore"));
		
	}

	private static List<ContextEvent> buildScenarioEvents(OntModel scenarioModel, OntModel contextModel, Dataset dataset) {
		List<ContextEvent> events = new ArrayList<>();
		
		//ScenarioInit.printStatements(scenarioModel);
		OntClass kinectCamera = contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "KinectCamera");
		OntClass microphone = contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "Microphone");
		
		Individual cameraAlex = scenarioModel.createIndividual(ScenarioInit.CAMERA1_URI, kinectCamera);
		Individual mic1 = scenarioModel.createIndividual(ScenarioInit.MIC1_URI, microphone);
		Individual mic2 = scenarioModel.createIndividual(ScenarioInit.MIC2_URI, microphone);
		
		int gap = 2;
		Calendar timestampInit = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		// round 1
		Calendar timestampSkel1 = (Calendar)timestampInit.clone();
		timestampSkel1.add(Calendar.MINUTE, ADVANCE_MIN_SKEL);
		timestampSkel1.add(Calendar.SECOND, gap);
		Individual skeleton1 = scenarioModel.createIndividual(ScenarioInit.SCENARIO_NS + "skeleton1", 
			contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "Skeleton"));
		
		ContextEvent event1 = new SenseSkeletonSittingEvent(contextModel, timestampSkel1, 600, cameraAlex, skeleton1);
		events.add(event1);
		
		// round 2
		gap = 5;
		Calendar timestampSkel2 = (Calendar)timestampSkel1.clone();
		timestampSkel2.add(Calendar.SECOND, gap);
		Individual skeleton2 = scenarioModel.createIndividual(ScenarioInit.SCENARIO_NS + "skeleton2", 
			contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "Skeleton"));
		ContextEvent event2 = new SenseSkeletonSittingEvent(contextModel, timestampSkel2, 600, cameraAlex, skeleton1);
		ContextEvent event3 = new SenseSkeletonSittingEvent(contextModel, timestampSkel2, 600, cameraAlex, skeleton2);
		
		Calendar timestampMic2 = (Calendar)timestampInit.clone();
		timestampMic2.add(Calendar.MINUTE, ADVANCE_MIN_MIC);
		timestampMic2.add(Calendar.SECOND, gap);
		ContextEvent event4 = new HasNoiseLevelEvent(contextModel, timestampMic2, 600, mic1, 80);
		ContextEvent event5 = new HasNoiseLevelEvent(contextModel, timestampMic2, 600, mic2, 80);
		events.add(event2); events.add(event3); events.add(event4); events.add(event5);
		
		// round 3
		gap = 5;
		Calendar timestampSkel3 = (Calendar)timestampSkel2.clone();
		timestampSkel3.add(Calendar.SECOND, gap);
		Individual skeleton3 = scenarioModel.createIndividual(ScenarioInit.SCENARIO_NS + "skeleton3", 
			contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "Skeleton"));
		ContextEvent event6 = new SenseSkeletonSittingEvent(contextModel, timestampSkel3, 600, cameraAlex, skeleton1);
		ContextEvent event7 = new SenseSkeletonSittingEvent(contextModel, timestampSkel3, 600, cameraAlex, skeleton2);
		ContextEvent event8 = new SenseSkeletonSittingEvent(contextModel, timestampSkel3, 600, cameraAlex, skeleton3);
		
		Calendar timestampMic3 = (Calendar)timestampMic2.clone();
		timestampMic3.add(Calendar.SECOND, gap);
		ContextEvent event9 = new HasNoiseLevelEvent(contextModel, timestampMic3, 600, mic1, 80);
		ContextEvent event10 = new HasNoiseLevelEvent(contextModel, timestampMic3, 600, mic2, 80);
		
		events.add(event6); events.add(event7); events.add(event8); events.add(event9); events.add(event10);
		
		//ContextEvent stopEvent = new StopEvent(contextModel, timestampMic3, 600);
		//events.add(stopEvent);
		
		return events;
	}
}
