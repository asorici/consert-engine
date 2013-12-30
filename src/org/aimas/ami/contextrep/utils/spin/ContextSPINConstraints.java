package org.aimas.ami.contextrep.utils.spin;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.model.ConstraintsWrapper;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.statistics.SPINStatistics;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.util.QueryWrapper;
import org.topbraid.spin.util.SPINUtil;
import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class ContextSPINConstraints {
	
	public static List<ContextConstraintViolation> check(OntModel infContextModel, ContextAssertion assertion, ConstraintsWrapper constraints, 
			List<SPINStatistics> stats) {
		
		Resource anchorResource = constraints.getAnchorResource();
		List<CommandWrapper> constraintCommands = constraints.getConstraintCommands();
		Map<CommandWrapper, Map<String, RDFNode>> templateBindings = constraints.getConstraintTemplateBindings();
		
		return run(infContextModel, assertion, anchorResource, constraintCommands, templateBindings, stats);
	}
	
	private static List<ContextConstraintViolation> run(OntModel infContextModel, ContextAssertion assertion, 
			Resource anchorResource, List<CommandWrapper> constraintCommands,
			Map<CommandWrapper, Map<String, RDFNode>> templateBindings,
			List<SPINStatistics> stats) {
		
		List<ContextConstraintViolation> results = new LinkedList<>();
		
		for(CommandWrapper arqConstraint : constraintCommands) {
			QueryWrapper queryConstraintWrapper = (QueryWrapper) arqConstraint;
			Map<String,RDFNode> initialBindings = templateBindings.get(arqConstraint);
			
			Query arq = queryConstraintWrapper.getQuery();
			String label = arqConstraint.getLabel();
			
			runQueryOnClass(results, arq, queryConstraintWrapper.getSPINQuery(), label, infContextModel, assertion, anchorResource, initialBindings, arqConstraint.isThisUnbound(), arqConstraint.isThisDeep(), arqConstraint.getSource(), stats);
			
			if(!arqConstraint.isThisUnbound()) {
				Set<Resource> subClasses = JenaUtil.getAllSubClasses(anchorResource);
				for(Resource subClass : subClasses) {
					runQueryOnClass(results, arq, queryConstraintWrapper.getSPINQuery(), label, infContextModel, assertion, subClass, initialBindings, arqConstraint.isThisUnbound(), arqConstraint.isThisDeep(), arqConstraint.getSource(), stats);
				}
			}
			
		}
		
		return results;
	}

	private static void runQueryOnClass(
            List<ContextConstraintViolation> results, Query arq,
            org.topbraid.spin.model.Query spinQuery, String label,
            OntModel infContextModel, ContextAssertion assertion, Resource anchorResource,
            Map<String, RDFNode> initialBindings, boolean thisUnbound,
            boolean thisDeep, Resource source, List<SPINStatistics> stats) {
	    
		if(thisUnbound || SPINUtil.isRootClass(anchorResource) || infContextModel.contains(null, RDF.type, anchorResource)) {
			QuerySolutionMap arqBindings = new QuerySolutionMap();
			if(!thisUnbound) {
				arqBindings.add(SPINUtil.TYPE_CLASS_VAR_NAME, anchorResource);
			}
			if(initialBindings != null) {
				for(String varName : initialBindings.keySet()) {
					RDFNode value = initialBindings.get(varName);
					arqBindings.add(varName, value);
				}
			}
			
			long startTime = System.currentTimeMillis();
			Model cm = JenaUtil.createDefaultModel();
			
			if(thisDeep && !thisUnbound) {
				StmtIterator it = infContextModel.listStatements(null, RDF.type, anchorResource);
				while(it.hasNext()) {
					Resource instance = it.next().getSubject();
					arqBindings.add(SPIN.THIS_VAR_NAME, instance);
					QueryExecution qexec = ARQFactory.get().createQueryExecution(arq, infContextModel, arqBindings);
					qexec.execConstruct(cm);
					qexec.close();
				}
			}
			else {
				QueryExecution qexec = ARQFactory.get().createQueryExecution(arq, infContextModel, arqBindings);
				qexec.execConstruct(cm);
				qexec.close();
			}
			
			long endTime = System.currentTimeMillis();
			if(stats != null) {
				long duration = endTime - startTime;
				String queryText = SPINLabels.get().getLabel(spinQuery);
				if(label == null) {
					label = queryText;
				}
				stats.add(new SPINStatistics(label, queryText, duration, startTime, anchorResource.asNode()));
			}
			
			addConstructedProblemReports(cm, results, infContextModel, assertion, anchorResource, null, label, source);
		}
	    
    }

	private static void addConstructedProblemReports(Model cm,
            List<ContextConstraintViolation> results, OntModel infContextModel,
            ContextAssertion assertion, Resource anchorResource, Resource matchRoot, String label,
            Resource source) {
		
		StmtIterator it = cm.listStatements(null, RDF.type, SPIN.ConstraintViolation);
		while(it.hasNext()) {
			Statement s = it.nextStatement();
			Resource vio = s.getSubject();
			
			// this is misused to indicate the newly inserted context assertion
			Statement rootS = vio.getProperty(SPIN.violationRoot);
			String newAssertionIdURI = rootS.getResource().getURI();
			
			// this is misused to indicate the conflicting existing context assertion
			Statement rootP = vio.getProperty(SPIN.violationPath);
			String conflictingAssertionIdURI = rootP.getResource().getURI();
			
			results.add(createConstraintViolation(assertion, newAssertionIdURI, conflictingAssertionIdURI, source));
		}
    }
	
	
	private static ContextConstraintViolation createConstraintViolation(
            ContextAssertion assertion, String newAssertionIdURI,
            String conflictingAssertionIdURI, Resource source) {
	    
		return new ContextConstraintViolation(assertion, newAssertionIdURI, conflictingAssertionIdURI, source);
    }
}
