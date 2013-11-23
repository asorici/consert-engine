package org.aimas.ami.contextrep.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aimas.ami.contextrep.core.Config;

import com.hp.hpl.jena.graph.Graph;
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
	 * Gets all Graphs that are potentially updated in a given Update request.
	 * @param update  the Update (UpdateData, UpdateModify and UpdateDeleteWhere are supported)
	 * @param dataset  the Dataset to get the Graphs from
	 * @return the Graphs
	 */
	public static Collection<Graph> getUpdatedGraphs(Update update, Dataset dataset, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		Set<Graph> results = new HashSet<Graph>();
		
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

	
	private static void addUpdatedGraphs(Set<Graph> results, UpdateData update, Dataset dataset, 
			Map<String, RDFNode> templateBindings, boolean storesOnly) {
		addUpdatedGraphs(results, update.getQuads(), dataset, templateBindings, storesOnly);
    }


	private static void addUpdatedGraphs(Set<Graph> results, UpdateDeleteWhere update, 
			Dataset dataset, Map<String,RDFNode> templateBindings, boolean storesOnly) {
		addUpdatedGraphs(results, update.getQuads(), dataset, templateBindings, storesOnly);
	}
	
	
	private static void addUpdatedGraphs(Set<Graph> results, UpdateModify update, Dataset dataset, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		Node withIRI = update.getWithIRI();
		if(withIRI != null) {
			results.add(dataset.getNamedModel(withIRI.getURI()).getGraph());
		}
		addUpdatedGraphs(results, update.getDeleteQuads(), dataset, templateBindings, storesOnly);
		addUpdatedGraphs(results, update.getInsertQuads(), dataset, templateBindings, storesOnly);
	}

	
	private static void addUpdatedGraphs(Set<Graph> results, Iterable<Quad> quads, Dataset graphStore, 
			Map<String,RDFNode> templateBindings, boolean storesOnly) {
		for(Quad quad : quads) {
			if(quad.isDefaultGraph()) {
				results.add(graphStore.getDefaultModel().getGraph());
			}
			else if(quad.getGraph().isVariable()) {
				if(templateBindings != null) {
					String varName = quad.getGraph().getName();
					RDFNode binding = templateBindings.get(varName);
					if(binding != null && binding.isURIResource()) {
						if (storesOnly && Config.getContextAssertionIndex().isContextStore(binding.asNode())) {
							results.add(graphStore.getNamedModel(binding.asNode().getURI()).getGraph());
						}
						else if (!storesOnly) {
							results.add(graphStore.getNamedModel(binding.asNode().getURI()).getGraph());
						}
					}
				}
			}
			else {
				if (storesOnly && Config.getContextAssertionIndex().isContextStore(quad.getGraph())) {
					results.add(graphStore.getNamedModel(quad.getGraph().getURI()).getGraph());
				}
				else if (!storesOnly) {
					results.add(graphStore.getNamedModel(quad.getGraph().getURI()).getGraph());
				}
			}
		}
	}
}
