package org.aimas.ami.contextrep.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.model.ContextAssertionGraph;
import org.aimas.ami.contextrep.utils.ContextAssertionFinder;
import org.apache.log4j.PropertyConfigurator;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Aggregation;
import org.topbraid.spin.model.Bind;
import org.topbraid.spin.model.CommandWithWhere;
import org.topbraid.spin.model.ElementList;
import org.topbraid.spin.model.Exists;
import org.topbraid.spin.model.Filter;
import org.topbraid.spin.model.FunctionCall;
import org.topbraid.spin.model.Minus;
import org.topbraid.spin.model.NamedGraph;
import org.topbraid.spin.model.NotExists;
import org.topbraid.spin.model.Optional;
import org.topbraid.spin.model.QueryOrTemplateCall;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Service;
import org.topbraid.spin.model.SubQuery;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.model.TriplePath;
import org.topbraid.spin.model.TriplePattern;
import org.topbraid.spin.model.Union;
import org.topbraid.spin.model.Values;
import org.topbraid.spin.model.Variable;
import org.topbraid.spin.model.visitor.ElementVisitor;
import org.topbraid.spin.model.visitor.ExpressionVisitor;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.SPINQueryFinder;
import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class CollectSpinRulesTest {

	public static String ASSEMBLER_FILE = "etc/context-tdb-assembler.ttl";
	public static String DOCUMENT_MGR_SPEC_PATH = "etc/context-ont-policy.rdf;etc/context-ont-location-mapper.ttl";
	public static String CONTEXT_ENTITY_STORE = "http://pervasive.semanticweb.org/ont/2013/05/contextassertion/ContextEntityStore";
	
	public static String CONTEXT_ASSERTION_SOURCE = "http://pervasive.semanticweb.org/ont/2013/05/contextassertion";
	public static String CONTEXT_ASSERTION_NS = CONTEXT_ASSERTION_SOURCE + "#";
	
	// common properties and classes of ContextAssertion Ontology 
	public static String ENTITY_RELATION_ASSERTION = CONTEXT_ASSERTION_NS + "entityRelationAssertion";
	public static String ENTITY_DATA_ASSERTION = CONTEXT_ASSERTION_NS + "entityDataAssertion";
	public static String UNARY_CONTEXT_ASSERTION = CONTEXT_ASSERTION_NS + "UnaryContextAssertion";
	public static String NARY_CONTEXT_ASSERTION  = CONTEXT_ASSERTION_NS + "NaryContextAssertion";
	
	public static Map<QueryOrTemplateCall, List<OntResource>> inverseRuleDictionary;
	
	public CollectSpinRulesTest() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("jena-log4j.properties");
		
		String contextModelURI = "http://pervasive.semanticweb.org/ont/2013/09/adhocmeeting/models";
		
		long start = System.currentTimeMillis();
		
		//OntModelSpec contextModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM_TRANS_INF);
		OntModelSpec contextModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
		OntDocumentManager globalDocMgr = OntDocumentManager.getInstance();
		globalDocMgr.setMetadataSearchPath(DOCUMENT_MGR_SPEC_PATH, true);
		OntModel contextModel = ModelFactory.createOntologyModel(contextModelSpec);
		contextModel.read(contextModelURI);
		
		System.out.println("Initialization time: " + (System.currentTimeMillis() - start) + "ms");
		start = System.currentTimeMillis();
		
		/*
		 * Collect spin:deriveassertion rules in contextModel
		 */
		Property deriveAssertionProp = ResourceFactory.createProperty(SPIN.NS + "deriveassertion");
		Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings = new HashMap<CommandWrapper, Map<String,RDFNode>>();
		Map<Resource,List<CommandWrapper>> cls2Query = SPINQueryFinder.getClass2QueryMap(
				contextModel, contextModel, deriveAssertionProp, false, false);
		
		for (Resource res : cls2Query.keySet()) {
			
			System.out.println(res.getURI() + ": ");
			System.out.println(res.getClass().getName() + ": ");
			List<CommandWrapper> cmds = cls2Query.get(res);
			
			for (CommandWrapper cmd : cmds) {
				org.topbraid.spin.model.Command spinCommand = null;
				Template template = null;
				
				TemplateCall templateCall = SPINFactory.asTemplateCall(cmd.getSource());
				if (templateCall != null) {
					template = templateCall.getTemplate();
					if(template != null) {
						spinCommand = template.getBody();
					}
				}
				else {
					spinCommand = SPINFactory.asCommand(cmd.getSource());
				}
				
				CommandWithWhere whereCommand =  spinCommand.as(CommandWithWhere.class);
				ElementList elements = whereCommand.getWhere();
				System.out.println(elements.getElements().size());
				
				//ElementWalker myElemWalker = new ElementWalker(new ContextAssertionVisitor(), new ContextExpressionVisitor());
				//myElemWalker.visit(elements);
				
				OntModel contextBasicInfModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_TRANS_INF, contextModel);
				ContextAssertionFinder finder = new ContextAssertionFinder(elements, contextBasicInfModel, initialTemplateBindings.get(cmd));
				finder.run();
				List<ContextAssertionGraph> contextAssertions = finder.getResult();
				
				System.out.println("Found " + contextAssertions.size() + " ContextAssertions!!!");
				for (ContextAssertionGraph assertion : contextAssertions) {
					System.out.println(assertion.getAssertionResource().getURI());
				}
				
				/*
				for (Element element : elements.getElements()) {
					ContextAssertionVisitor assertionVisitor = new ContextAssertionVisitor(element, new HashMap<Property, RDFNode>()); 
					assertionVisitor.run();
				}
				*/
				
			}
			System.out.println("===================================================");
			System.out.println();
		}
		
		System.out.println("Computation time: " + (System.currentTimeMillis() - start) + "ms");
		
		/*
		 * Close all open and models
		 */
		contextModel.close();
	}
	
	
	static class ContextAssertionVisitor implements ElementVisitor {

		@Override
		public void visit(Bind bind) {
			System.out.println("A binding: " + bind.getVariable().getName());
			System.out.println();
		}

		@Override
		public void visit(ElementList elementList) {
			System.out.println("Recursing into element list of size: " + elementList.size());
			System.out.println();
		}

		@Override
		public void visit(Exists exists) {
			System.out.println("An EXISTS statement: " + exists.getElements().size() + " subelements");
			System.out.println();
		}

		@Override
		public void visit(Filter filter) {
			System.out.println("An FILTER statement: " + filter.getExpression().getClass().getName());
			System.out.println();
		}

		@Override
		public void visit(Minus minus) {
			System.out.println("An MINUS statement: " + minus.getElements().size() + " subelements");
			System.out.println();
		}

		@Override
		public void visit(NamedGraph namedGraph) {
			System.out.println("A NAMED GRAPH statement: " + namedGraph.getElements().size() + " subelements");
			System.out.println();
		}

		@Override
		public void visit(NotExists notExists) {
			System.out.println("A NOT EXISTS statement: " + notExists.getElements().size() + " subelements");
			System.out.println();
		}

		@Override
		public void visit(Optional optional) {
			System.out.println("An OPTIONAL statement: " + optional.getElements().size() + " subelements");
			System.out.println();
		}

		@Override
		public void visit(Service service) {
			System.out.println("A SERVICE statement: " + service.getElements().size() + " subelements");
			System.out.println();
		}

		@Override
		public void visit(SubQuery subQuery) {
			System.out.println("A SUBQUERY statement: " + ARQFactory.get().createCommandString(subQuery.getQuery()));
			System.out.println();
		}

		@Override
		public void visit(TriplePath triplePath) {
			System.out.println("A TriplePath statement: " + triplePath.getSubject().getURI() + " " + 
					triplePath.getObject().asResource().getURI());
			System.out.println();
		}

		@Override
		public void visit(TriplePattern triplePattern) {
			System.out.println("A TripplePattern statement: " + triplePattern.getSubject().getURI() + " " + 
					triplePattern.getPredicate().getURI() + " " +
					triplePattern.getObject().asResource().getURI());
			System.out.println();
		}

		@Override
		public void visit(Union union) {
			System.out.println("A UNION statement: " + union.getElements().size() + " subelements");
			System.out.println();
		}

		@Override
        public void visit(Values values) {
			System.out.println("A VALUES statement: " + values.getVarNames().size() + " variables");
			System.out.println();
	        
        }
	}
	
	static class ContextExpressionVisitor implements ExpressionVisitor {

		@Override
		public void visit(Aggregation aggregation) {
			//System.out.println("An AGGREGATION expression ");
		}

		@Override
		public void visit(FunctionCall functionCall) {
			//System.out.println("An FunctionCall expression ");
		}

		@Override
		public void visit(RDFNode node) {
			System.out.println("An RDFNode expression ");
		}

		@Override
		public void visit(Variable variable) {
			//System.out.println("A VARIABLE expression: " + variable.getName());
		}
	}
}
