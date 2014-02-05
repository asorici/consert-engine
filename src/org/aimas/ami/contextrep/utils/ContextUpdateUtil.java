package org.aimas.ami.contextrep.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.core.Config;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.UpdateData;
import com.hp.hpl.jena.sparql.modify.request.UpdateDeleteWhere;
import com.hp.hpl.jena.sparql.modify.request.UpdateModify;
import com.hp.hpl.jena.update.Update;

public class ContextUpdateUtil {
	/**
	 * Gets all graph nodes that are potentially updated in a given Update request.
	 * @param update  the Update (UpdateData, UpdateModify and UpdateDeleteWhere are supported)
	 * @param dataset  the Dataset to get the Graphs from
	 * @return the graphs nodes
	 */
	public static Collection<Node> getUpdatedGraphs(Update update, Dataset dataset, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		Set<Node> results = new HashSet<Node>();
		
		if (update instanceof UpdateData) {
			addUpdatedGraphs(results, (UpdateData)update, dataset, templateBindings, storesOnly);
		}
		else if(update instanceof UpdateModify) {
			addUpdatedGraphs(results, (UpdateModify)update, dataset, templateBindings, storesOnly);
		}
		else if(update instanceof UpdateDeleteWhere) {
			addUpdatedGraphs(results, (UpdateDeleteWhere)update, dataset, templateBindings, storesOnly);
		}
		return results;
	}

	
	private static void addUpdatedGraphs(Set<Node> results, UpdateData update, Dataset dataset, 
			Map<String, RDFNode> templateBindings, boolean storesOnly) {
		addUpdatedGraphs(results, update.getQuads(), dataset, templateBindings, storesOnly);
    }


	private static void addUpdatedGraphs(Set<Node> results, UpdateDeleteWhere update, 
			Dataset dataset, Map<String,RDFNode> templateBindings, boolean storesOnly) {
		addUpdatedGraphs(results, update.getQuads(), dataset, templateBindings, storesOnly);
	}
	
	
	private static void addUpdatedGraphs(Set<Node> results, UpdateModify update, Dataset dataset, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		Node withIRI = update.getWithIRI();
		if(withIRI != null) {
			results.add(withIRI);
		}
		addUpdatedGraphs(results, update.getDeleteQuads(), dataset, templateBindings, storesOnly);
		addUpdatedGraphs(results, update.getInsertQuads(), dataset, templateBindings, storesOnly);
	}

	
	private static void addUpdatedGraphs(Set<Node> results, Iterable<Quad> quads, Dataset graphStore, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		for(Quad quad : quads) {
			if(quad.isDefaultGraph()) {
				results.add(quad.getGraph());
			}
			else if(quad.getGraph().isVariable()) {
				if(templateBindings != null) {
					String varName = quad.getGraph().getName();
					RDFNode binding = templateBindings.get(varName);
					if(binding != null && binding.isURIResource()) {
						if (storesOnly && Config.getContextAssertionIndex().isContextStore(binding.asNode())) {
							results.add(binding.asNode());
						}
						else if (!storesOnly) {
							results.add(binding.asNode());
						}
					}
				}
			}
			else {
				if (storesOnly && Config.getContextAssertionIndex().isContextStore(quad.getGraph())) {
					results.add(quad.getGraph());
				}
				else if (!storesOnly) {
					results.add(quad.getGraph());
				}
			}
		}
	}
}
