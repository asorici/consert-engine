package org.aimas.ami.contextrep.utils.spin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openjena.atlas.iterator.Iter;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.sparql.core.Quad;
//import com.hp.hpl.jena.sparql.modify.GraphStoreUtils;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateRequest;


/**
 * A GraphStore that wraps a given Dataset, so that each updateable
 * graph is wrapped with a ControlledUpdateGraph instead of the default.
 * 
 * @author Holger Knublauch
 */
class ControlledCtxUpdateGraphStore implements GraphStore {
	
	private Map<Graph, ControlledCtxUpdateGraph> cugs = new HashMap<Graph,ControlledCtxUpdateGraph>();
	
	private Dataset dataset;
	
	
	ControlledCtxUpdateGraphStore(Dataset dataset, Iterable<Graph> controlledGraphs) {
		this.dataset = dataset;
		for(Graph graph : controlledGraphs) {
			ControlledCtxUpdateGraph cug = new ControlledCtxUpdateGraph(graph);
			cugs.put(graph, cug);
		}
	}
	
	
	private Graph getControlledUpdateGraph(Graph graph) {
		Graph cug = cugs.get(graph);
		if(cug != null) {
			return cug;
		}
		else {
			return graph;
		}
	}
	
	
	public Iterable<ControlledCtxUpdateGraph> getControlledUpdateGraphs() {
		return cugs.values();
	}


	@Override
	public Graph getDefaultGraph() {
		Model defaultModel = dataset.getDefaultModel();
		if(defaultModel != null) {
			return getControlledUpdateGraph(defaultModel.getGraph());
		}
		else {
			return null;
		}
	}


	@Override
	public Graph getGraph(Node graphNode) {
		Model model = dataset.getNamedModel(graphNode.getURI());
		if(model != null) {
			return getControlledUpdateGraph(model.getGraph());
		}
		else {
			return null;
		}
	}


	@Override
	public boolean containsGraph(Node graphNode) {
		return dataset.containsNamedModel(graphNode.getURI());
	}


	@Override
	public void setDefaultGraph(Graph g) {
	}


	@Override
	public void addGraph(Node graphName, Graph graph) {
	}


	@Override
	public void removeGraph(Node graphName) {
	}


	@Override
	public Iterator<Node> listGraphNodes() {
		List<Node> results = new LinkedList<Node>();
		Iterator<String> it = dataset.listNames();
		while(it.hasNext()) {
			results.add(Node.createURI(it.next()));
		}
		return results.iterator();
	}


	@Override
	public void add(Quad quad) {
		Graph graph;
		if(quad.isDefaultGraph()) {
			graph = getDefaultGraph();
		}
		else {
			graph = getGraph(quad.getGraph());
		}
		if(graph != null) {
			graph.add(quad.asTriple());
		}
	}


	@Override
	public void delete(Quad quad) {
		Graph graph;
		if(quad.isDefaultGraph()) {
			graph = getDefaultGraph();
		}
		else {
			graph = getGraph(quad.getGraph());
		}
		if(graph != null) {
			graph.delete(quad.asTriple());
		}
	}


	@Override
	public void deleteAny(Node g, Node s, Node p, Node o) {
        Iterator<Quad> iter = find(g, s, p, o) ;
        List<Quad> list = Iter.toList(iter) ;
        for (Quad q : list) {
            delete(q);
        }
	}


	@Override
	public Iterator<Quad> find() {
		return null;
	}


	@Override
	public Iterator<Quad> find(Quad quad) {
		return null;
	}


	@Override
	public Iterator<Quad> find(Node g, Node s, Node p, Node o) {
		return null;
	}


	@Override
	public Iterator<Quad> findNG(Node g, Node s, Node p, Node o) {
		return null;
	}


	@Override
	public boolean contains(Node g, Node s, Node p, Node o) {
		Graph graph = getGraph(g);
		if(graph != null) {
			return graph.contains(s, p, o);
		}
		else {
			return false;
		}
	}


	@Override
	public boolean contains(Quad quad) {
		return false;
	}


	@Override
	public boolean isEmpty() {
		return false;
	}


	@Override
	public Lock getLock() {
		return null;
	}


	@Override
	public Context getContext() {
		return ARQ.getContext() ;
	}


	@Override
	public long size() {
		return 0;
	}


	@Override
	public void close() {
	}


	@Override
	public Dataset toDataset() {
		return null;
	}


	@Override
	public void startRequest() {
	}


	@Override
	public void finishRequest() {
		if (dataset.asDatasetGraph() instanceof DatasetGraphTDB) {
			TDB.sync(dataset);
		}
	}


	@Override
	public void add(Node g, Node s, Node p, Node o) {
		add(Quad.create(g, s, p, o));
	}


	@Override
	public void delete(Node g, Node s, Node p, Node o) {
		delete(Quad.create(g, s, p, o));
	}


	@Override
    public void finishRequest(UpdateRequest arg0) {
	    // TODO Auto-generated method stub
	    
    }


	@Override
    public void startRequest(UpdateRequest arg0) {
	    // TODO Auto-generated method stub
	    
    }
	
	
	/*
	@Override
	public void startRequest() {
		GraphStoreUtils.sendToAll(this, GraphStoreEvents.RequestStartEvent) ;
	}


	@Override
	public void finishRequest() {
		if (dataset.asDatasetGraph() instanceof DatasetGraphTDB) {
			TDB.sync(dataset);
		}
		GraphStoreUtils.sendToAll(this, GraphStoreEvents.RequestFinishEvent) ;
	}
	*/
}
