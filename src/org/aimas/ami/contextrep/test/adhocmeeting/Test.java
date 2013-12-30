package org.aimas.ami.contextrep.test.adhocmeeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.tdb.TDB;

public class Test {
	
	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// PropertyConfigurator.configure("log4j.properties");
		String configurationFile = "src/org/aimas/ami/contextrep/test/adhocmeeting/config.properties";
		Log.setLog4j();
		
		try {
			// init configuration
			Config.init(configurationFile, true);
			
			Dataset dataset = Config.getContextDataset();
			OntModel basicContextModel = Config.getBasicContextModel();
			
			attempSPINInference(dataset, basicContextModel);
			
			Config.close();
		}
		catch (ConfigException e) {
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
		
		List<DerivedAssertionWrapper> derivationCommands = 
			ruleDict.getDerivationsForAssertion(Config.getContextAssertionIndex().getAssertionFromResource(hasNoiseLevelProperty));
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
}
