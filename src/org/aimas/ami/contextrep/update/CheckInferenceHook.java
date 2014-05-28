package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.core.ContextARQFactory;
import org.aimas.ami.contextrep.core.DerivationRuleDictionary;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.test.performance.RunTest;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;
import org.aimas.ami.contextrep.utils.spin.ContextSPINInferences;
import org.aimas.ami.contextrep.utils.spin.ContextSPINInferences.ContextInferenceResult;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.aimas.ami.contextrep.vocabulary.ConsertRules;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.JenaUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

public class CheckInferenceHook extends ContextUpdateHook {
	
	public CheckInferenceHook(ContextAssertion contextAssertion) {
		super(contextAssertion);
	}
	
	@Override
	public InferenceHookResult exec(Dataset contextDataset) {
		//System.out.println("======== CHECKING INFERENCE FOR assertion <" + contextAssertion + ">. ");
		
		// get the context model
		OntModel basicContextModel = Engine.getCoreContextModel();
		
		return attemptContextSPINInference(contextDataset, basicContextModel);
	}
	
	private InferenceHookResult attemptContextSPINInference(Dataset contextDataset, OntModel basicContextModel) {
		long start = System.currentTimeMillis();
		
		DerivationRuleDictionary ruleDict = Engine.getDerivationRuleDictionary();
		List<DerivedAssertionWrapper> derivationCommands = ruleDict.getDerivationsForAssertion(contextAssertion);
		
		if (derivationCommands != null) {
			
			// Create a list to collect inference results
			List<ContextUpdateExecutionWrapper> inferredContext = new ArrayList<>();
			List<ContextAssertion> inferredContextAssertions = new ArrayList<>();
			
			
			// get the query model as the union of the named graphs in our dataset
			//Model queryModel = contextDataset.getNamedModel(JenaVocabulary.UNION_GRAPH_URN);
			MultiUnion union = new MultiUnion();
			Iterator<String> namedModels = contextDataset.listNames();
			for (; namedModels.hasNext();) {
				union.addGraph(contextDataset.getNamedModel(namedModels.next()).getGraph());
			}
			Model queryModel = ModelFactory.createModelForGraph(union);
			
			// for each derivation rule do a separate inference procedure
			for (DerivedAssertionWrapper derivationWrapper : derivationCommands) {
				List<ContextUpdateExecutionWrapper> inferred = attemptDerivationRule(derivationWrapper, queryModel, basicContextModel, contextDataset);
				
				if (!inferred.isEmpty()) {
					inferredContextAssertions.add(derivationWrapper.getDerivedResource());
					inferredContext.addAll(inferred);
				}
			}
			
			if (!inferredContext.isEmpty()) {
				for (ContextUpdateExecutionWrapper inferredAssertion : inferredContext) {
					Future<AssertionInsertResult> result = Engine.assertionInsertExecutor().submit(inferredAssertion);
					
					RunTest.insertionTaskEnqueueTime.put(inferredAssertion.getAssertionInsertID(), System.currentTimeMillis());
					RunTest.insertionResults.put(inferredAssertion.getAssertionInsertID(), result);
				}
				
				long end = System.currentTimeMillis();
				return new InferenceHookResult(start, (int)(end - start), false, inferredContextAssertions, true, true);
			}
			else {
				long end = System.currentTimeMillis();
				return new InferenceHookResult(start, (int)(end - start), false, null, true, false);
			}
		}
		
		long end = System.currentTimeMillis();
		return new InferenceHookResult(start, (int)(end - start), false, null, false, false);
	}
	
	
	private List<ContextUpdateExecutionWrapper> attemptDerivationRule(DerivedAssertionWrapper derivationWrapper, 
			Model queryModel,OntModel basicContextModel, Dataset contextDataset) {
		List<ContextUpdateExecutionWrapper> inferred = new ArrayList<>();
		
		DerivationRuleDictionary ruleDict = Engine.getDerivationRuleDictionary();
		
		// Create Model for inferred triples
		//Model newTriples = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
		Model newTriples = ModelFactory.createDefaultModel();
		
		Map<Resource, List<CommandWrapper>> cls2Query = new HashMap<>();
		Map<Resource, List<CommandWrapper>> cls2Constructor = new HashMap<>();
		SPINRuleComparator comparator = new DefaultSPINRuleComparator(queryModel);
		
		Resource entityRes = ruleDict.getEntityForDerivation(derivationWrapper);
		CommandWrapper cmd = derivationWrapper.getDerivationCommand();
		
		//Map<CommandWrapper, Map<String, RDFNode>> initialTemplateBindings = new HashMap<CommandWrapper, Map<String, RDFNode>>();
		//initialTemplateBindings.put(cmd, derivationWrapper.getCommandBindings());
		
		// create entityCommandWrappers required for SPIN inference API call
		List<CommandWrapper> entityCommandWrappers = new ArrayList<>();
		entityCommandWrappers.add(cmd);
		cls2Query.put(entityRes, entityCommandWrappers);
		
		// perform inference
		//long timestamp = System.currentTimeMillis();
		ARQFactory.set(new ContextARQFactory(contextDataset));
		
		//ContextInferenceResult inferenceResult = ContextSPINInferences.runContextInference(queryModel, newTriples,
		//	cls2Query, cls2Constructor, initialTemplateBindings, null, SPINVocabulary.deriveAssertionRule, comparator);
		ContextInferenceResult inferenceResult = ContextSPINInferences.runContextInference(queryModel, newTriples,
			cls2Query, cls2Constructor, null, ConsertRules.DERIVE_ASSERTION, comparator);
		
		if (inferenceResult != null && inferenceResult.isInferred()) {
			//System.out.println("[INFO] WE HAVE DEDUCED A NEW CONTEXT-ASSERTION following insertion of " + contextAssertion 
			//+ ". Duration: " + (System.currentTimeMillis() - timestamp) );
			
			//ScenarioInit.printStatements(newTriples);
			
			// for testing purpose only count number of inferred assertions
			RunTest.numInferredAssertions.getAndIncrement();
			
			// If there was a deduction the CONSTRUCTED result is in the newTriples model.
			// Use it to create a new UpdateRequest to be executed by the assertionInsertExecutor
			
			// step 1: identify the blank nodes that assert the annotations - there may be several deductions
			ResIterator annotationSubjectIt = newTriples.listResourcesWithProperty(RDF.type, ConsertAnnotation.CONTEXT_ANNOTATION);
			List<Resource> annotationSubjects = annotationSubjectIt.toList();
			
			// There may be more inference instances - the rule applied to several entities and assertions.
			// Each of them will have the same annotations (from rule construction), so we only need to inspect
			// one annotation instance.
			// But we have to identify the individual assertions - for this it suffices to know the type of the
			// derived assertion and the number of inference instances (which is the same as that of annotation
			// instances).
			//int nrInferenceInstances = annotationSubjects.size();
			Resource annotationSubject = annotationSubjects.get(0);
			NodeIterator nodeIt = newTriples.listObjectsOfProperty(annotationSubject, ConsertCore.CONTEXT_ASSERTION_RESOURCE);
			
			OntResource derivedAssertionResource = basicContextModel.getOntResource(nodeIt.next().asResource());
			ContextAssertion derivedAssertion = Engine.getContextAssertionIndex().getAssertionFromResource(derivedAssertionResource);
				
			// step 2: identify all statements having that node as subject - those are annotations
			StmtIterator annotationIt = newTriples.listStatements(new AnnotationStatementSelector(annotationSubject));
			List<Statement> annotationStatements = annotationIt.toList();
			
			Map<Node, Update> assertionUpdates = null;
			
			// step 3: depending on the type of derived resource identify the assertion statements
			if (derivedAssertionResource.canAs(OntProperty.class)) {
				// we have a binary assertion
				 assertionUpdates = getBinaryAssertionUpdates(newTriples, derivedAssertionResource.as(OntProperty.class));
			}
			else {
				if (JenaUtil.hasSuperClass(derivedAssertionResource, basicContextModel.getOntClass(ConsertCore.UNARY_CONTEXT_ASSERTION.getURI()))) {
					// we have a unary assertion
					assertionUpdates = getUnaryAssertionUpdates(newTriples, derivedAssertionResource);
				}
				else {
					// we have a nary assertion
					assertionUpdates = getNaryAssertionUpdates(newTriples, derivedAssertionResource);
				}
			}
			
			// step 4: for each assertion instance: create the new identifier graph and add the
			// annotation statements (which are the same for every instance)
			for (Node graphUUIDNode : assertionUpdates.keySet()) {
				// create the new assertion graph
				Update createUpdate = new UpdateCreate(graphUUIDNode);
				Update assertionUpdate = assertionUpdates.get(graphUUIDNode);
				
				// create the context annotation quads, while keeping in mind to replace the
				// annotationSubject with the created graphUUIDNode identifier of the ContextAssertion
				Node derivedAssertionStore = NodeFactory.createURI(derivedAssertion.getAssertionStoreURI());
				
				QuadDataAcc annotationData = new QuadDataAcc();
				for (Statement s : annotationStatements) {
					annotationData.addQuad(Quad.create(derivedAssertionStore,
							graphUUIDNode, s.getPredicate().asNode(), s.getObject().asNode()));
				}
				Update annotationUpdate = new UpdateDataInsert(annotationData);
				
				// create the update request and enqueue it
				UpdateRequest insertInferredRequest = UpdateFactory.create();
				insertInferredRequest.add(createUpdate);
				insertInferredRequest.add(assertionUpdate);
				insertInferredRequest.add(annotationUpdate);
				
				inferred.add(new ContextUpdateExecutionWrapper(insertInferredRequest));
			}
		}
		else if (inferenceResult != null && !inferenceResult.isInferred()) {
			//System.out.println("[INFO] NO INFERENCE RESULT ");
		}
		
		
		return inferred;
    }


	private Map<Node, Update> getNaryAssertionUpdates(Model newTriples, OntResource derivedAssertionResource) {
		Map<Node, Update> naryAssertionInstances = new HashMap<>();
		
		// Identify all statements that are not annotations - those are assertion statements
		StmtIterator assertionInstanceIt = newTriples.listStatements(null, RDF.type, derivedAssertionResource);
		List<Statement> assertionInstances = assertionInstanceIt.toList();
		
		for (Statement assertionInstance : assertionInstances) {
			Resource assertionSubject = assertionInstance.getSubject();
			
			// For Nary assertions the statements are those that have the blank node that defines the assertion
			// as a subject
			StmtIterator assertionStmtIt = newTriples.listStatements(assertionSubject, null, (RDFNode)null);
			List<Statement> assertionStatements = assertionStmtIt.toList();
			
			// Create the identifier named graph URI for the new ContextAssertion
			//Node graphUUIDNode = Node.createURI(GraphUUIDGenerator.createUUID(derivedAssertionResource));
			Node graphUUIDNode = NodeFactory.createURI(GraphUUIDGenerator.createUUID(derivedAssertionResource));
			
			// Create the context assertion quads
			QuadDataAcc assertionData = new QuadDataAcc();
			for (Statement s : assertionStatements) {
				assertionData.addQuad(Quad.create(graphUUIDNode, s.asTriple()));
			}
			Update assertionUpdate = new UpdateDataInsert(assertionData);
			
			naryAssertionInstances.put(graphUUIDNode, assertionUpdate);
		}
		
		return naryAssertionInstances;
    }

	private Map<Node, Update> getBinaryAssertionUpdates(Model newTriples, OntProperty derivedAssertionProp) {
		Map<Node, Update> binaryAssertionInstances = new HashMap<>();
		
		// Identify all statements that are not annotations - those are assertion statements
		StmtIterator assertionInstanceIt = newTriples.listStatements((Resource)null, derivedAssertionProp, (RDFNode)null);
		List<Statement> assertionInstances = assertionInstanceIt.toList();
		
		for (Statement assertionInstance : assertionInstances) {
			// For Binary assertions the statements are those that have the derivedAssertionProp as property =>
			// the exact assertionInstance statement as selected above
			
			// Create the identifier named graph URI for the new ContextAssertion
			//Node graphUUIDNode = Node.createURI(GraphUUIDGenerator.createUUID(derivedAssertionProp));
			Node graphUUIDNode = NodeFactory.createURI(GraphUUIDGenerator.createUUID(derivedAssertionProp));
			
			// Create the context assertion quads
			QuadDataAcc assertionData = new QuadDataAcc();
			assertionData.addQuad(Quad.create(graphUUIDNode, assertionInstance.asTriple()));
			
			Update assertionUpdate = new UpdateDataInsert(assertionData);
			
			binaryAssertionInstances.put(graphUUIDNode, assertionUpdate);
		}
		
		return binaryAssertionInstances;
    }

	private Map<Node, Update> getUnaryAssertionUpdates(Model newTriples, OntResource derivedAssertionResource) {
		Map<Node, Update> unaryAssertionInstances = new HashMap<>();
		
		// Identify all statements that are not annotations - those are assertion statements
		StmtIterator assertionInstanceIt = newTriples.listStatements(null, RDF.type, derivedAssertionResource);
		List<Statement> assertionInstances = assertionInstanceIt.toList();
		
		for (Statement assertionInstance : assertionInstances) {
			Resource assertionSubject = assertionInstance.getSubject();
			
			// For Unary assertions the statements are those that have the ContextEntity that plays the assertionRole
			// as a subject
			StmtIterator assertionStmtIt = newTriples.listStatements(assertionSubject, null, (RDFNode)null);
			List<Statement> assertionStatements = assertionStmtIt.toList();
			
			// Create the identifier named graph URI for the new ContextAssertion
			//Node graphUUIDNode = Node.createURI(GraphUUIDGenerator.createUUID(derivedAssertionResource));
			Node graphUUIDNode = NodeFactory.createURI(GraphUUIDGenerator.createUUID(derivedAssertionResource));
			
			// Create the context assertion quads
			QuadDataAcc assertionData = new QuadDataAcc();
			for (Statement s : assertionStatements) {
				assertionData.addQuad(Quad.create(graphUUIDNode, s.asTriple()));
			}
			Update assertionUpdate = new UpdateDataInsert(assertionData);
			
			unaryAssertionInstances.put(graphUUIDNode, assertionUpdate);
		}
		
		return unaryAssertionInstances;
    }


	private class AnnotationStatementSelector implements Selector {
		private Resource annotationSubject;
		
		AnnotationStatementSelector(Resource annotationSubject) {
			this.annotationSubject = annotationSubject;
		}
		
		@Override
        public boolean test(Statement s) {
			if (s.getSubject().equals(annotationSubject) && !s.getPredicate().equals(RDF.type)) {
				return true;
			}
			
			return false;
        }

		@Override
        public boolean isSimple() {
	        return false;
        }

		@Override
        public Resource getSubject() {
	        return annotationSubject;
        }

		@Override
        public Property getPredicate() {
	        return null;
        }

		@Override
        public RDFNode getObject() {
	        return null;
        }
	}
	
	private class AssertionStatementSelector implements Selector {
		private List<Statement> annotationStatements;
		private Resource annotationSubject;

		public AssertionStatementSelector(List<Statement> annotationStatements, Resource annotationSubject) {
	        this.annotationStatements = annotationStatements;
	        this.annotationSubject = annotationSubject;
        }
		
		@Override
        public boolean test(Statement s) {
	        if (annotationStatements.contains(s) || s.getSubject().equals(annotationSubject)) {
	        	return false;
	        }
	        
	        return true;
        }

		@Override
        public boolean isSimple() {
	        return false;
        }

		@Override
        public Resource getSubject() {
	        return null;
        }

		@Override
        public Property getPredicate() {
	        return null;
        }

		@Override
        public RDFNode getObject() {
	        return null;
        }
	}
}
