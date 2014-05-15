package org.aimas.ami.contextrep.update;

import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.modify.UpdateEngine;
import com.hp.hpl.jena.sparql.modify.UpdateEngineFactory;
import com.hp.hpl.jena.sparql.modify.UpdateEngineMain;
import com.hp.hpl.jena.sparql.modify.UpdateEngineRegistry;
import com.hp.hpl.jena.sparql.modify.UpdateEngineWorker;
import com.hp.hpl.jena.sparql.modify.request.UpdateVisitor;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

public class ContextAssertionUpdateEngine extends UpdateEngineMain {
	
	/*
	public ContextAssertionUpdateEngine(GraphStore graphStore, UpdateRequest request, Binding inputBinding, Context context) { 
			super(graphStore, request, inputBinding, context) ; 
	}
	*/
    
	public ContextAssertionUpdateEngine(GraphStore graphStore, Binding inputBinding, Context context) { 
		super(graphStore, inputBinding, context) ; 
	}
	
	
	@Override
	protected UpdateVisitor prepareWorker() {
		return new ContextUpdateWorker(graphStore, inputBinding, context) ;
	}
	
	
	/*
    @Override
    public void execute() {
    	graphStore.startRequest(request);
        ContextUpdateWorker worker = new ContextUpdateWorker(graphStore, startBinding, context) ;
        for ( Update up : request )
            up.visit(worker) ;
        graphStore.finishRequest(request);
    }
    */
    
    // ---- Factory
	public static UpdateEngineFactory getFactory() {
		return new UpdateEngineFactory() {
			
			@Override
			public boolean accept(GraphStore graphStore, Context context) {
				// we accept everything
				return true;
			}
			
			@Override
			public UpdateEngine create(GraphStore graphStore, Binding inputBinding, Context context) {
				return new ContextAssertionUpdateEngine(graphStore, inputBinding, context);
			}
		};
	}

    public static void register() { UpdateEngineRegistry.get().add(getFactory()) ; }
	
}
