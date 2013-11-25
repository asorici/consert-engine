package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.ContextARQFactory;
import org.aimas.ami.contextrep.core.DerivationRuleDictionary;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.utils.GraphUUIDGenerator;
import org.aimas.ami.contextrep.utils.spin.ContextSPINInferences;
import org.aimas.ami.contextrep.utils.spin.ContextSPINInferences.ContextInferenceResult;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;
import org.aimas.ami.contextrep.vocabulary.JenaVocabulary;
import org.aimas.ami.contextrep.vocabulary.SPINVocabulary;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.util.CommandWrapper;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
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
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

public class CheckInferenceHook extends ContextUpdateHook {
	
	public CheckInferenceHook(OntResource contextAssertionResource) {
		super(contextAssertionResource);
	}
	
	@Override
	public boolean exec(Dataset contextStoreDataset) {
		// get the context model
		OntModel basicContextModel = Config.getBasicContextModel();
		
		return attemptContextSPINInference(contextStoreDataset, basicContextModel);
	}
	
	private boolean attemptContextSPINInference(Dataset contextStoreDataset, OntModel basicContextModel) {
		//System.out.println("#### Attempting SPIN Inference ####");
		long timestamp = System.currentTimeMillis();
		DerivationRuleDictionary ruleDict = Config.getDerivationRuleDictionary();
		ContextInferenceResult inferenceResult = null;
		
		// get the query model as the union of the named graphs in our TDB store
		// dataset
		Model queryModel = contextStoreDataset.getNamedModel(JenaVocabulary.UNION_GRAPH_URN);
		
		// Create Model for inferred triples
		Model newTriples = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
		
		List<DerivedAssertionWrapper> derivationCommands = ruleDict.getDerivationsForAssertion(contextAssertionResource);
		
		Map<Resource, List<CommandWrapper>> cls2Query = new HashMap<>();
		Map<Resource, List<CommandWrapper>> cls2Constructor = new HashMap<>();
		Map<CommandWrapper, Map<String, RDFNode>> initialTemplateBindings = new HashMap<CommandWrapper, Map<String, RDFNode>>();
		SPINRuleComparator comparator = new DefaultSPINRuleComparator(queryModel);
		
		if (derivationCommands != null) {
			for (DerivedAssertionWrapper derivationWrapper : derivationCommands) {
				Resource entityRes = ruleDict.getEntityForDerivation(derivationWrapper);
				CommandWrapper cmd = derivationWrapper.getDerivationCommand();
				//OntResource derivedAssertionResource = derivationWrapper.getDerivedResource();
				
				/*
				Map<String, RDFNode> binding = new HashMap<>();
				String graphUUID = GraphUUIDGenerator.createUUID(derivedAssertionResource);
				
				RDFNode graphUUIDNode = ResourceFactory.createResource(graphUUID);
				binding.put("graphUUID", graphUUIDNode);
				
				cmd2GraphUUIDMap.put(cmd, graphUUIDNode);
				initialTemplateBindings.put(cmd, binding);
				*/
				
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
			inferenceResult = ContextSPINInferences.run(queryModel, newTriples,
			        cls2Query, cls2Constructor, initialTemplateBindings, null,
			        null, true, SPINVocabulary.deriveAssertionRule, comparator,
			        null);
		}
		
		
		if (inferenceResult != null && inferenceResult.isInferred()) {
			System.out.println("[INFO] WE HAVE DEDUCED A NEW CONTEXT-ASSERTION following insertion of " + contextAssertionResource 
				+ ". Duration: " + (System.currentTimeMillis() - timestamp) );
			
			// If there was a deduction the CONSTRUCTED result is in the newTriples model.
			// Use it to create a new UpdateRequest to be executed by the assertionInsertExecutor
			
			// step 1: identify the blank node that asserts the annotations
			ResIterator annotationSubjectIt = newTriples.listSubjectsWithProperty(RDF.type, 
				basicContextModel.getResource(ContextAssertionVocabulary.CONTEXT_ANNOTATION));
			Resource annotationSubject = annotationSubjectIt.next();
			
			// step 2: identify the derived ContextAssertion resource type
			NodeIterator nodeIt = newTriples.listObjectsOfProperty(annotationSubject, 
				ResourceFactory.createProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_RESOURCE));
			OntResource derivedAssertionResource = basicContextModel.getOntResource(nodeIt.next().asResource());
			
			// step 3: identify all statements having that node as subject - those are annotations
			StmtIterator annotationIt = 
				newTriples.listStatements(new AnnotationStatementSelector(annotationSubject));
			List<Statement> annotationStatements = annotationIt.toList();
			
			// step 4: identify all statements that are not annotations - those are assertion statements
			StmtIterator assertionStmtIt = 
				newTriples.listStatements(new AssertionStatementSelector(annotationStatements, annotationSubject));
			List<Statement> assertionStatements = assertionStmtIt.toList();
			
			// step 5: create the identifier named graph URI for the new ContextAssertion
			Node graphUUIDNode = Node.createURI(GraphUUIDGenerator.createUUID(derivedAssertionResource));
			
			// step 6: create the UpdateRequest based on these statement groups
			// create the new assertion graph
			Update createUpdate = new UpdateCreate(graphUUIDNode);
			
			// create the context assertion quads
			QuadDataAcc assertionData = new QuadDataAcc();
			for (Statement s : assertionStatements) {
				assertionData.addQuad(Quad.create(graphUUIDNode, s.asTriple()));
			}
			Update assertionUpdate = new UpdateDataInsert(assertionData);
			
			// create the context annotation quads, while keeping in mind to replace the
			// annotationSubject with the created graphUUIDNode identifier of the ContextAssertion
			Node derivedAssertionStore = Node.createURI(Config.getStoreForAssertion(derivedAssertionResource));
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
			
			Config.assertionInsertExecutor().execute(new ContextUpdateExecutionWrapper(insertInferredRequest));
		}
		
		return true;
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
