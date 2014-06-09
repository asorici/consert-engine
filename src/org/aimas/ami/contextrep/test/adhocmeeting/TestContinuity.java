package org.aimas.ami.contextrep.test.adhocmeeting;

import org.aimas.ami.contextrep.core.ContextAssertionIndex;
import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.core.api.ConfigException;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertFunctions;
import org.openjena.atlas.logging.Log;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.mgt.Explain;

public class TestContinuity {
	private static final String contextAssertionResourceURI = ScenarioInit.AD_HOC_MEETING_NS + "hasNoiseLevel";
	private static final String contextAssertionUUID = "http://pervasive.semanticweb.org/ont/2013/09/adhocmeeting/models/hasNoiseLevel-01cf6354-a99a-4a7c-8ae9-32dcf724832a";
	
	public TestContinuity() {
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
			Engine.init(configurationFile, true);
			
			Dataset dataset = Engine.getRuntimeContextStore();
			OntModel basicContextModel = Engine.getCoreContextModel();
			
			attemptContinuityDeduction(dataset, basicContextModel);
			
			Engine.close(false);
		}
		catch (ConfigException e) {
			e.printStackTrace();
		}
	}

	private static void attemptContinuityDeduction(Dataset contextStoreDataset, OntModel contextModel) {
		ContextAssertionIndex assertionIndex = Engine.getContextAssertionIndex();
		OntResource contextAssertionResource = contextModel.getOntResource(contextAssertionResourceURI);
		
		// find the property that states the validity interval
		Property validDuringProp = ConsertAnnotation.HAS_VALIDITY;
		
		// get context assertion store URI
		String assertionStoreURI = 
			assertionIndex.getAssertionFromResource(contextAssertionResource).getAssertionStoreURI();
		Resource newAssertionUUIDResource = ResourceFactory.createResource(contextAssertionUUID);
		
		// create a list of available (assertionUUID, assertionValidity) pairs 
		// that marks whose validity period can be extended with the current one 
		// (because the contents of the assertions is the same as that of newAssertionUUID)
		//List<ContinuityWrapper> availableContinuityPairs = new ArrayList<>();
		
		// enter a READ transaction to determine whether continuity is available
		contextStoreDataset.begin(ReadWrite.READ);
		try {
			// get the contextAssertion store model and the validityAnnotation
			Model assertionStoreModel = contextStoreDataset.getNamedModel(assertionStoreURI);
			Statement validityAnnotation = assertionStoreModel.getProperty(newAssertionUUIDResource, validDuringProp);
			
			if (validityAnnotation != null) {
				RDFNode newValidityPeriod = validityAnnotation.getObject();
				Template closeEnoughValidity = SPINModuleRegistry.get().getTemplate(ConsertFunctions.CLOSE_ENOUGH_VALIDITY_TEMPLATE.getURI(), null); 
				
				// Now call the template
				com.hp.hpl.jena.query.Query arq = ARQFactory.get().createQuery((Select)closeEnoughValidity.getBody());
				
				//arq.setPrefix("contextassertion:", ContextAssertionVocabulary.NS);
				//arq.setPrefix("functions:", ContextAssertionVocabulary.FUNCTIONS_NS);
				//arq.setBaseURI(Config.getContextModelNamespace());
				QueryExecution qexec = ARQFactory.get().createQueryExecution(arq, contextStoreDataset);
				
				// set the value of the arguments
				QuerySolutionMap arqBindings = new QuerySolutionMap();
				arqBindings.add("contextAssertionResource", ResourceFactory.createResource(contextAssertionResource.getURI()));
				arqBindings.add("contextAssertionStore", ResourceFactory.createResource(assertionStoreURI));
				arqBindings.add("newAssertionUUID", ResourceFactory.createResource(contextAssertionUUID));
				arqBindings.add("newValidityPeriod", newValidityPeriod);
				
				qexec.setInitialBinding(arqBindings); // Pre-assign the required arguments
				
				qexec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.FINE) ;
				try {
					ResultSet rs = qexec.execSelect();
					
					/*
					 * we will now go through the results and make a list of
					 * the pairs (assertionUUID, assertionValidity) which can be extended with
					 * the current (newAssertionUUID, newValidityPeriod)
					 */
					while (rs.hasNext()) {
						QuerySolution qs = rs.next();
						RDFNode assertionUUID = qs.get("assertionUUID");
						RDFNode validity = qs.get("validity");
						
						System.out.println("CONTINUITY AVAILABALE FOR assertion <"
						        + contextAssertionResource + ">. "
						        + "AssertionUUID: " + assertionUUID
						        + ", for duration: " + validity);
						
						//availableContinuityPairs.add(new ContinuityWrapper(assertionUUID, validity));
					}
				} 
				catch (Exception ex) {
					ex.printStackTrace();
				}
				finally {
					qexec.close();
				}
			}
		}  
		finally {
			contextStoreDataset.end();
		}
    }
	
	
	private static class ContinuityWrapper {
		RDFNode assertionUUID; 
		RDFNode assertionValidity;
		
		ContinuityWrapper(RDFNode assertionUUID, RDFNode assertionValidity) {
	        this.assertionUUID = assertionUUID;
	        this.assertionValidity = assertionValidity;
        }
	}
}
