package org.aimas.ami.contextrep.test.adhocmeeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.core.ConfigKeys;
import org.aimas.ami.contextrep.core.ContextARQFactory;
import org.aimas.ami.contextrep.core.DerivationRuleDictionary;
import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.core.Loader;
import org.aimas.ami.contextrep.core.api.ConfigException;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;
import org.aimas.ami.contextrep.vocabulary.ConsertRules;
import org.aimas.ami.contextrep.vocabulary.JenaVocabulary;
import org.apache.log4j.PropertyConfigurator;
import org.openjena.atlas.logging.Log;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.vocabulary.SPL;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.ProfileRegistry;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.tdb.TDB;

public class Test {
	
	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("etc/log4j.properties");
		String configurationFile = "etc/config.properties";
		Log.setLog4j();
		
		System.setProperty("http.proxyHost", "proxy.emse.fr");
		System.setProperty("http.proxyPort", "8080");
		
		try {
			// init configuration
			//Engine.init(configurationFile, true);
			
			Loader.parseConfigProperties(configurationFile);
			Map<String, String> contextModelURIMap = Loader.getContextModelURIs();
			Map<String, OntModel> contextModelMap = Loader.getContextModelModules(contextModelURIMap);
			
			OntDocumentManager spinDocMgr = Loader.getOntDocumentManager(ConfigKeys.SPIN_ONT_DOCMGR_FILE);
			OntModelSpec spinSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
			spinSpec.setDocumentManager(spinDocMgr);
			
			
			// Load main file
			//Model baseModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
			//baseModel.read(SPL.BASE_URI);
			
			// Create OntModel with imports
			//OntModel splModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF, baseModel);
			
			//OntModel splModel = ModelFactory.createOntologyModel(spinSpec, SPL.getModel());
			OntModel splModel = ModelFactory.createOntologyModel(spinSpec);
			splModel.read(SPL.BASE_URI, "RDF/XML");
			
			SPINModuleRegistry.get().registerAll(splModel, null);
			System.out.println(SPINModuleRegistry.get().getFunction(SPL.NS + "max", null).getURI());
			
			
//			Dataset dataset = Engine.getRuntimeContextStore();
//			OntModel basicContextModel = Engine.getCoreContextModel();
//			
//			attempSPINInference(dataset, basicContextModel);
			//Engine.close(false);
		}
		catch (ConfigException e) {
			e.printStackTrace();
		}
		finally {
			
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
		SPINInferences.run(queryModel, newTriples, cls2Query, cls2Constructor, initialTemplateBindings, null, null, true, ConsertRules.DERIVE_ASSERTION, comparator, null);
		//SPINInferences.run(queryModel, newTriples, cls2Query, cls2Constructor, null, null, true, ConsertRules.DERIVE_ASSERTION, comparator, null);
		
		System.out.println("[INFO] Ran SPIN Inference. Duration: " + 
			(System.currentTimeMillis() - timestamp) );
		
		TDB.sync(dataset);
		
		// see if it worked
		ScenarioInit.printStatements(dataset.getNamedModel(ScenarioInit.AD_HOC_MEETING_BASE + "/" + "HostsAdHocMeetingStore"));
		
	}
}
