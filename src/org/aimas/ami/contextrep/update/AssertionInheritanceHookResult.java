package org.aimas.ami.contextrep.update;

import java.util.List;

import org.aimas.ami.contextrep.model.ContextAssertion;

public class AssertionInheritanceHookResult extends HookResult {
	
	private List<ContextAssertion> ancestorAssertionList;
	
	public AssertionInheritanceHookResult(long startTime, int duration, boolean error, 
			List<ContextAssertion> ancestorAssertionList) {
	    
		super(startTime, duration, error);
	    this.ancestorAssertionList = ancestorAssertionList;
    }
	
	
	public List<ContextAssertion> getAncestorAssertionList() {
		return ancestorAssertionList;
	}
	
	
	public boolean inherits() {
		return ancestorAssertionList != null && !ancestorAssertionList.isEmpty();
	}
}
