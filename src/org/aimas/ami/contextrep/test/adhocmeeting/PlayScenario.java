package org.aimas.ami.contextrep.test.adhocmeeting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.ContextARQFactory;
import org.aimas.ami.contextrep.core.DerivationRuleDictionary;
import org.aimas.ami.contextrep.exceptions.ConfigException;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;
import org.aimas.ami.contextrep.vocabulary.JenaVocabulary;
import org.aimas.ami.contextrep.vocabulary.SPINVocabulary;
import org.openjena.atlas.logging.Log;
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
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.tdb.TDB;

public class PlayScenario {
	
	public static final int ADVANCE_MIN_SKEL = -7;
	public static final int ADVANCE_MIN_MIC = -3;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//PropertyConfigurator.configure("log4j.properties");
		String configurationFile = "src/org/aimas/ami/contextrep/test/adhocmeeting/config.properties";
		Log.setLog4j();
		
		try {
			// init configuration
			Config.init(configurationFile, true);
			Config.cleanDataset();
			
			Dataset dataset = Config.getContextStoreDataset();
			OntModel basicContextModel = Config.getBasicContextModel();
			OntModel basicScenarioModel = ScenarioInit.initScenario(dataset, basicContextModel);
			
			//attempSPINInference(dataset, basicContextModel);
			
			
			List<ProducerEvent> scenarioEvents = buildScenarioEvents(basicScenarioModel, basicContextModel, dataset);
			
			LinkedBlockingQueue<ProducerEvent> eventQueue = new LinkedBlockingQueue<>();
			EventProducerThread producer = new EventProducerThread(scenarioEvents, eventQueue);
			EventConsumerThread consumer = new EventConsumerThread(eventQueue);
			
			producer.start();
			consumer.start();
			
			try {
				producer.join();
				consumer.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("THREADS HAVE JOINED; SIMULATION OVER");
			
			
			try {
	            Thread.sleep(10000);
            }
            catch (InterruptedException e) {
	            e.printStackTrace();
            }
			
			
			System.out.println("######## DATASTORE ########");
			dataset = Config.getContextStoreDataset();
			
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
			
			//Config.close();
		} catch (ConfigException e) {
			e.printStackTrace();
		}
	}
	
	private static void attempSPINInference(Dataset dataset, OntModel basicContextModel) {
		System.out.println("#### Attempting SPIN Inference ####");
		long timestamp = System.currentTimeMillis();
		
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
		
		DerivationRuleDictionary ruleDict = Config.getDerivationRuleDictionary();
		OntProperty hasNoiseLevelProperty = basicContextModel.getOntProperty(ScenarioInit.AD_HOC_MEETING_NS + "hasNoiseLevel");
		
		//System.out.println(Config.getContextAssertionIndex().getAssertion2StoreMap());
		
		Model queryModel = dataset.getNamedModel(JenaVocabulary.UNION_GRAPH_URN);
		
		//System.out.println("Query Model as default dataset graph: should be union of all named graphs");
		//ScenarioInit.printStatements(queryModel);
		
		// Create Model for inferred triples
		Model newTriples = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
		
		List<DerivedAssertionWrapper> derivationCommands = ruleDict.getDerivationsForAssertion(hasNoiseLevelProperty);
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
		SPINInferences.run(queryModel, newTriples, cls2Query, cls2Constructor, initialTemplateBindings, null, null, true, SPINVocabulary.deriveAssertionRule, comparator, null);
		
		System.out.println("[INFO] Ran SPIN Inference. Duration: " + 
			(System.currentTimeMillis() - timestamp) );
		
		TDB.sync(dataset);
		
		// see if it worked
		ScenarioInit.printStatements(dataset.getNamedModel(ScenarioInit.AD_HOC_MEETING_BASE + "/" + "HostsAdHocMeetingStore"));
		
	}

	private static List<ProducerEvent> buildScenarioEvents(OntModel scenarioModel, OntModel contextModel, Dataset dataset) {
		List<ProducerEvent> events = new ArrayList<>();
		
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
		
		ProducerEvent event1 = new SenseSkeletonSittingEvent(contextModel, timestampSkel1, 600, cameraAlex, skeleton1);
		events.add(event1);
		
		// round 2
		gap = 5;
		Calendar timestampSkel2 = (Calendar)timestampSkel1.clone();
		timestampSkel2.add(Calendar.SECOND, gap);
		Individual skeleton2 = scenarioModel.createIndividual(ScenarioInit.SCENARIO_NS + "skeleton2", 
			contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "Skeleton"));
		ProducerEvent event2 = new SenseSkeletonSittingEvent(contextModel, timestampSkel2, 600, cameraAlex, skeleton1);
		ProducerEvent event3 = new SenseSkeletonSittingEvent(contextModel, timestampSkel2, 600, cameraAlex, skeleton2);
		
		Calendar timestampMic2 = (Calendar)timestampInit.clone();
		timestampMic2.add(Calendar.MINUTE, ADVANCE_MIN_MIC);
		timestampMic2.add(Calendar.SECOND, gap);
		ProducerEvent event4 = new HasNoiseLevelEvent(contextModel, timestampMic2, 600, mic1, 80);
		ProducerEvent event5 = new HasNoiseLevelEvent(contextModel, timestampMic2, 600, mic2, 80);
		events.add(event2); events.add(event3); events.add(event4); events.add(event5);
		
		// round 3
		gap = 5;
		Calendar timestampSkel3 = (Calendar)timestampSkel2.clone();
		timestampSkel3.add(Calendar.SECOND, gap);
		Individual skeleton3 = scenarioModel.createIndividual(ScenarioInit.SCENARIO_NS + "skeleton3", 
			contextModel.getOntClass(ScenarioInit.AD_HOC_MEETING_NS + "Skeleton"));
		ProducerEvent event6 = new SenseSkeletonSittingEvent(contextModel, timestampSkel3, 600, cameraAlex, skeleton1);
		ProducerEvent event7 = new SenseSkeletonSittingEvent(contextModel, timestampSkel3, 600, cameraAlex, skeleton2);
		ProducerEvent event8 = new SenseSkeletonSittingEvent(contextModel, timestampSkel3, 600, cameraAlex, skeleton3);
		
		Calendar timestampMic3 = (Calendar)timestampMic2.clone();
		timestampMic3.add(Calendar.SECOND, gap);
		ProducerEvent event9 = new HasNoiseLevelEvent(contextModel, timestampMic3, 600, mic1, 80);
		ProducerEvent event10 = new HasNoiseLevelEvent(contextModel, timestampMic3, 600, mic2, 80);
		
		events.add(event6); events.add(event7); events.add(event8); events.add(event9); events.add(event10);
		
		ProducerEvent stopEvent = new StopEvent(contextModel, timestampMic3, 600);
		events.add(stopEvent);
		
		return events;
	}
	
	
	private static class EventProducerThread extends Thread {
		private static final int DELAY_MILLIS = 1000;
		
		private List<ProducerEvent> events;
		private LinkedBlockingQueue<ProducerEvent> eventQueue;
		
		EventProducerThread(List<ProducerEvent> events, LinkedBlockingQueue<ProducerEvent> eventQueue) {
			this.events = events;
			this.eventQueue = eventQueue;
		}
		
		
		@Override
		public void run() {
			
			while (!events.isEmpty()) {
				Calendar nowSkel = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				Calendar nowMic = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				nowSkel.add(Calendar.MINUTE, ADVANCE_MIN_SKEL);
				nowMic.add(Calendar.MINUTE, ADVANCE_MIN_MIC);
				
				int nrEvents = events.size();
				System.out.println("Number of events: " + nrEvents);
				
				for (int i = 0; i < nrEvents; i++) {
					ProducerEvent event = events.get(i);
					if (event instanceof SenseSkeletonSittingEvent && event.timestamp.after(nowSkel)) {
						break;
					}
					else if ((event instanceof HasNoiseLevelEvent || event instanceof StopEvent) 
							&& event.timestamp.after(nowMic)) {
						break;
					}
					else {
						events.remove(i);
						i--;
						nrEvents--;
						
						//System.out.println("Enqueueing " + event);
						eventQueue.add(event);
					}
				}
				
				try {
					Thread.sleep(DELAY_MILLIS);
				} 
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			System.out.println("PRODUCER THREAD" + " stopping");
		}
	}
	
	
	private static class EventConsumerThread extends Thread {
		private LinkedBlockingQueue<ProducerEvent> eventQueue;
		private Dataset contextStoreDataset;
		
		EventConsumerThread(LinkedBlockingQueue<ProducerEvent> eventQueue) {
			this.eventQueue = eventQueue;
			contextStoreDataset = Config.getContextStoreDataset();
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					ProducerEvent event = eventQueue.take();
					
					if (event != null) {
						if (event instanceof StopEvent) {
							break;
						}
						
						handleEvent(event);
					}
					
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			System.out.println("CONSUMER THREAD" + " stopping");
		}
		
		private void handleEvent(ProducerEvent event) {
			System.out.println("Handling " + event);
			event.execute(contextStoreDataset);
		}
	}
}
