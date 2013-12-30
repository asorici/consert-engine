package org.aimas.ami.contextrep.update;

import java.util.LinkedList;
import java.util.List;

import org.aimas.ami.contextrep.core.Config;
import org.aimas.ami.contextrep.core.ConstraintIndex;
import org.aimas.ami.contextrep.model.ConstraintsWrapper;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.utils.spin.ContextConstraintViolation;
import org.aimas.ami.contextrep.utils.spin.ContextSPINConstraints;
import org.topbraid.spin.statistics.SPINStatistics;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;

public class CheckConstraintHook extends ContextUpdateHook {
	
	public CheckConstraintHook(ContextAssertion contextAssertion) {
		super(contextAssertion);
	}
	
	@Override
	public ConstraintHookResult exec(Dataset contextStoreDataset) {
		long start = System.currentTimeMillis();
		
		// see if this context assertion has any constraints attached
		ConstraintIndex constraintIndex = Config.getConstraintIndex();
		ConstraintsWrapper constraints = constraintIndex.getConstraints(contextAssertion);  
		
		if (constraints != null) {
			OntModel infContextModel = Config.getRdfsContextModel();
			List<SPINStatistics> stats = new LinkedList<>();
			
			List<ContextConstraintViolation> constraintViolations = 
				ContextSPINConstraints.check(infContextModel, contextAssertion, constraints, stats);
			
			if (!constraintViolations.isEmpty()) {
				// TODO: do something useful with the detected violations
				System.out.println("[INFO] Constraint violations detected for assertion: " + contextAssertion);
			
				long end = System.currentTimeMillis();
				return new ConstraintHookResult(start, end - start, false, true, true);
			}
			else {
				long end = System.currentTimeMillis();
				return new ConstraintHookResult(start, end - start, false, true, false);
			}
		}
		
		long end = System.currentTimeMillis();
		return new ConstraintHookResult(start, end - start, false, false, false);
	}
}
