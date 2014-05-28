package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.List;

import org.aimas.ami.contextrep.core.Engine;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.utils.CalendarInterval;
import org.aimas.ami.contextrep.utils.CalendarIntervalList;
import org.aimas.ami.contextrep.vocabulary.ConsertAnnotation;
import org.aimas.ami.contextrep.vocabulary.ConsertFunctions;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Select;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDB;

public class CheckValidityContinuityHook extends ContextUpdateHook {
	
	private Node contextAssertionUUID;
	
	public CheckValidityContinuityHook(ContextAssertion contextAssertion, Node contextAssertionUUID) {
		super(contextAssertion);
		this.contextAssertionUUID = contextAssertionUUID;
	}
	
	@Override
	public ContinuityHookResult exec(Dataset contextStoreDataset) {
		long start = System.currentTimeMillis();
		
		//System.out.println("======== CHECKING CONTINUITY AVAILABALE FOR assertion <" + contextAssertion + ">. "
		//        + "AssertionUUID: " + contextAssertionUUID);
		
		// get access to the datastore and the assertionIndex
		OntModel contextModel = Engine.getCoreContextModel();
		
		// find the property that states the validity interval
		Property validDuringProp = ConsertAnnotation.HAS_VALIDITY;
		
		// get context assertion store URI
		String assertionStoreURI = contextAssertion.getAssertionStoreURI();
		Resource newAssertionUUIDResource = ResourceFactory.createResource(contextAssertionUUID.getURI());
		
		// create a list of available (assertionUUID, assertionValidity) pairs 
		// that marks whose validity period can be extended with the current one 
		// (because the contents of the assertions is the same as that of newAssertionUUID)
		List<ContinuityWrapper> availableContinuityPairs = new ArrayList<>();
		
		
		// get the contextAssertion store model and the validityAnnotation
		Model assertionStoreModel = contextStoreDataset.getNamedModel(assertionStoreURI);
		Statement validityAnnotation = assertionStoreModel.getProperty(newAssertionUUIDResource, validDuringProp);
		
		if (validityAnnotation != null) {
			RDFNode newValidityPeriod = validityAnnotation.getObject();
			Template closeEnoughValidity = SPINModuleRegistry.get().getTemplate(ConsertFunctions.CLOSE_ENOUGH_VALIDITY_TEMPLATE.getURI(), null);
			
			// Now call the template
			com.hp.hpl.jena.query.Query arq = ARQFactory.get().createQuery((Select) closeEnoughValidity.getBody());
			
			// arq.setPrefix("contextassertion:",
			// ContextAssertionVocabulary.NS);
			// arq.setPrefix("functions:",
			// ContextAssertionVocabulary.FUNCTIONS_NS);
			// arq.setBaseURI(Config.getContextModelNamespace());
			QueryExecution qexec = ARQFactory.get().createQueryExecution(arq,
			        contextStoreDataset);
			
			// set the value of the arguments
			QuerySolutionMap arqBindings = new QuerySolutionMap();
			arqBindings.add("contextAssertionResource", ResourceFactory.createResource(contextAssertion.getOntologyResource().getURI()));
			arqBindings.add("contextAssertionStore", ResourceFactory.createResource(assertionStoreURI));
			arqBindings.add("newAssertionUUID", ResourceFactory.createResource(contextAssertionUUID.getURI()));
			arqBindings.add("newValidityPeriod", newValidityPeriod);
			
			qexec.setInitialBinding(arqBindings); // Pre-assign the required
												  // arguments
			
			// qexec.getContext().set(ARQ.symLogExec, Explain.InfoLevel.FINE) ;
			try {
				ResultSet rs = qexec.execSelect();
				
				/*
				 * we will now go through the results and make a list of the
				 * pairs (assertionUUID, assertionValidity) which can be
				 * extended with the current (newAssertionUUID,
				 * newValidityPeriod)
				 */
				while (rs.hasNext()) {
					QuerySolution qs = rs.next();
					RDFNode assertionUUID = qs.get("assertionUUID");
					RDFNode validity = qs.get("validity");
					
					//System.out.println("CONTINUITY AVAILABALE FOR assertion <" + contextAssertion + ">. "
					//        + "AssertionUUID: " + assertionUUID
					//        + ", for duration: " + validity);
					
					availableContinuityPairs.add(new ContinuityWrapper(
					        assertionUUID, validity));
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
				long end = System.currentTimeMillis();
				return new ContinuityHookResult(start, (int)(end - start), true, false);
				
			}
			finally {
				qexec.close();
			}
		}
		
		if (!availableContinuityPairs.isEmpty()) {
			// re-get the newValidityPeriod
			RDFNode newValidityPeriod = validityAnnotation.getObject();
			
			// apply extension of validity periods for identified assertions
			for (ContinuityWrapper pairWrapper : availableContinuityPairs) {
				// now get the validity of the result
				CalendarIntervalList newValidityIntervals = (CalendarIntervalList) newValidityPeriod
				        .asLiteral().getValue();
				CalendarIntervalList validityIntervals = (CalendarIntervalList) pairWrapper.assertionValidity
				        .asLiteral().getValue();
				
				CalendarIntervalList mergedValidityIntervals = validityIntervals
				        .joinCloseEnough(newValidityIntervals, CalendarInterval.MAX_GAP_MILLIS);
				Literal mergedValidityLiteral = ResourceFactory.createTypedLiteral(mergedValidityIntervals);
				
				/*
				 * Now that we have the merged intervals it's time to update the
				 * models. We have to: - update the existing assertion with the
				 * merged validity intervals - at the end of this while loop
				 * remove the newly inserted context assertion
				 */
				Resource assertionUUIDResource = ResourceFactory.createResource(pairWrapper.assertionUUID.asNode().getURI());
				assertionStoreModel.remove(assertionUUIDResource, validDuringProp, pairWrapper.assertionValidity);
				assertionStoreModel.add(assertionUUIDResource, validDuringProp, mergedValidityLiteral);
			}
			
			// we don't need to register the context store listeners here since
			// we are operating a
			// known manual event
			// Config.registerContextAssertionStoreListeners(contextStoreDataset);
			Model newAssertionStoreModel = contextStoreDataset.getNamedModel(assertionStoreURI);
			
			// now remove the newly inserted triples. we just remove the named
			// graphs altogether
			contextStoreDataset.removeNamedModel(contextAssertionUUID.getURI());
			StmtIterator newAssertionStatements = newAssertionStoreModel
					.listStatements(newAssertionUUIDResource, null, (RDFNode) null);
			newAssertionStoreModel.remove(newAssertionStatements);
			
			long end = System.currentTimeMillis();
			return new ContinuityHookResult(start, (int)(end - start), false, true);
		}
		
		// finally sync the changes
		TDB.sync(contextStoreDataset);
		
		long end = System.currentTimeMillis();
		return new ContinuityHookResult(start, (int)(end - start), false, false);
	}

	
	@Override
	public String toString() {
		String response = "";
		
		response += "Executing CHECK_VALIDITIY_CONTINUITY for contextAssertionResource: " + contextAssertion 
				 + " in graphUUID: " + contextAssertionUUID.getURI();
		
		return response;
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
