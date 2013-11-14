package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.ContextARQFactory;
import org.aimas.ami.contextrep.core.ContextAssertionIndex;
import org.aimas.ami.contextrep.core.DerivationRuleDictionary;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.utils.ContextAssertionUtils;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;
import org.aimas.ami.contextrep.utils.spin.ContextSPINInferences;
import org.aimas.ami.contextrep.utils.spin.ContextSPINInferences.ContextInferenceResult;
import org.aimas.ami.contextrep.vocabulary.JenaVocabulary;
import org.aimas.ami.contextrep.vocabulary.SPINVocabulary;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.UpdateWrapper;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.UpdateModify;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.Update;

public class CheckInferenceHook implements ContextUpdateHook {
	private OntResource contextAssertionResource;
	
	public CheckInferenceHook(OntResource contextAssertionResource) {
		this.contextAssertionResource = contextAssertionResource;
	}
	
	@Override
	public boolean exec() {
		// get the dataset and the context model
		Dataset contextStoreDataset = Config.getContextStoreDataset();
		OntModel basicContextModel = Config.getBasicContextModel();
		
		return attemptContextSPINInference(contextStoreDataset, basicContextModel);
	}
	
	private boolean attemptContextSPINInference(Dataset contextStoreDataset, OntModel basicContextModel) {
		//System.out.println("#### Attempting SPIN Inference ####");
		long timestamp = System.currentTimeMillis();
		DerivationRuleDictionary ruleDict = Config.getDerivationRuleDictionary();
		ContextInferenceResult inferenceResult = null;
		
		Map<CommandWrapper, RDFNode> cmd2GraphUUIDMap = new HashMap<>();
		
		// start a WRITE dataset transaction		
		contextStoreDataset.begin(ReadWrite.WRITE);
		try {
			// get the query model as the union of the named graphs in our TDB store dataset
			Model queryModel = contextStoreDataset.getNamedModel(JenaVocabulary.UNION_GRAPH_URN);
			
			// Create Model for inferred triples
			Model newTriples = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
			
			List<DerivedAssertionWrapper> derivationCommands = ruleDict.getDerivationsForAssertion(contextAssertionResource);
			
			Map<Resource, List<CommandWrapper>> cls2Query = new HashMap<>();
			Map<Resource, List<CommandWrapper>> cls2Constructor = new HashMap<>();
			Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings = 
					new HashMap<CommandWrapper, Map<String,RDFNode>>();
			SPINRuleComparator comparator = new DefaultSPINRuleComparator(queryModel);
			
			if (derivationCommands != null) {
				for (DerivedAssertionWrapper derivationWrapper : derivationCommands) {
					Resource entityRes = ruleDict.getEntityForDerivation(derivationWrapper);
					CommandWrapper cmd = derivationWrapper.getDerivationCommand();
					OntResource derivedAssertionResource = derivationWrapper.getDerivedResource();
					
					Map<String, RDFNode> binding = new HashMap<>();
					String graphUUID = GraphUUIDGenerator.createUUID(derivedAssertionResource);
					
					RDFNode graphUUIDNode = ResourceFactory.createResource(graphUUID);
					binding.put("graphUUID", graphUUIDNode);
					
					cmd2GraphUUIDMap.put(cmd, graphUUIDNode);
					
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
				
				ARQFactory.set(new ContextARQFactory(contextStoreDataset));
				inferenceResult = ContextSPINInferences.run(queryModel, newTriples, cls2Query, cls2Constructor, initialTemplateBindings, null, null, true, SPINVocabulary.deriveAssertionRule, comparator, null);
			}
			
			// commit the transaction if anything was added
			if (inferenceResult != null && inferenceResult.isInferred()) {
				contextStoreDataset.commit();
			}
			else {
				contextStoreDataset.abort();
			}
		}
		finally {
			contextStoreDataset.end();
		}
		
		//TDB.sync(dataset);
		
		if (inferenceResult != null && inferenceResult.isInferred()) {
			System.out.println("[INFO] WE HAVE DEDUCED A NEW CONTEXT-ASSERTION following insertion of " + contextAssertionResource 
				+ ". Duration: " + (System.currentTimeMillis() - timestamp) );
			
			
			// check now again for chaining of inferences
			ContextAssertionIndex assertionIndex = Config.getContextAssertionIndex();
			List<DerivedAssertionWrapper> derivationCommands = ruleDict.getDerivationsForAssertion(contextAssertionResource);
			
			contextStoreDataset.begin(ReadWrite.READ);
			
			// attach the context assertion listeners because we might infer additional assertions
			// which can trigger the inference process again
			List<ContextAssertionUpdateListener> registeredContextStoreListeners = 
				ContextAssertionUtils.registerContextAssertionStoreListeners(contextStoreDataset);
			
			GraphStore graphStore = GraphStoreFactory.create(contextStoreDataset);
			for (DerivedAssertionWrapper derivationWrapper : derivationCommands) {
				CommandWrapper cmd = derivationWrapper.getDerivationCommand();
				
				if (cmd instanceof UpdateWrapper) {
					UpdateWrapper updateWrapper = (UpdateWrapper) cmd;
					Update update = updateWrapper.getUpdate();
					
					if(update instanceof UpdateModify) {
						UpdateModify updateModify = (UpdateModify)update;
						List<Quad> quads = updateModify.getInsertQuads();
					
						List<Node> updatedGraphNodes = new ArrayList<>();
				        Map<Node, Quad> updatedGraphNodeMap = new HashMap<>();
				        
				        for (Quad q : quads) {
				            // add the graphNode to the list of updated graph Nodes if it doesn't exist yet
				            if (!updatedGraphNodes.contains(q.getGraph())) {
				            	updatedGraphNodes.add(q.getGraph());
				            	updatedGraphNodeMap.put(q.getGraph(), q);
				            }
				        }
				        
				        for (Node graphNode : updatedGraphNodes) {
							// inspect the graph node to see if it is a
							// ContextAssertion named graph UUID. If it is, then we are dealing with a 
							// ContextAssertion insertion
							
							// alternatively, test to see if we have a ContextAnnotation insertion
							if (graphNode.isURI() && assertionIndex.isContextAssertionStore(graphNode)) {
								RDFNode assertionUUID = cmd2GraphUUIDMap.get(cmd);
								OntResource assertionResource = assertionIndex.getResourceFromGraphUUID(assertionUUID.asNode());
								
								if (assertionResource != null) {
						        	Graph assertionStoreGraph = graphStore.getGraph(graphNode);
						        	
						        	// signal the context annotation insertion
						        	ContextAssertionEvent inferredContextAssertion = 
						        		new ContextAssertionEvent(ContextAssertionEvent.CONTEXT_ASSERTION_INFERRED, assertionUUID.asNode());
					            	assertionStoreGraph.getEventManager().notifyEvent(assertionStoreGraph, inferredContextAssertion);
								}
							}
						}
					}
				}
				
				contextStoreDataset.end();
			}
			
			List<ContextUpdateHook> contextUpdateHooks = ContextAssertionUtils.collectContextUpdateHooks(registeredContextStoreListeners);
			if (contextUpdateHooks != null) {
				List<ContextUpdateHookErrorListener> updateHookErrorListeners = new ArrayList<>();
				ContextAssertionUtils.executeContextUpdateHooks(contextUpdateHooks, updateHookErrorListeners);
			}
		}
		
		return true;
	}

	@Override
    public OntResource getContextAssertionResource() {
	    return contextAssertionResource;
    }
}
