package org.aimas.ami.contextrep.update;

import java.util.ArrayList;
import java.util.List;

public class ContextUpdateHookExecutor extends Thread {
	private static final String NAME = "ContextUpdateHookExecutor";
	
	private List<ContextUpdateHook> hookList;
	private List<ContextUpdateHookErrorListener> updateHookErrorListeners;
	
	public ContextUpdateHookExecutor(List<ContextUpdateHook> hookList) {
		super(NAME);
		
		this.hookList = hookList;
		updateHookErrorListeners = new ArrayList<>();
	}
	
	public void registerContextUpdateErrorListeners(List<ContextUpdateHookErrorListener> listeners) {
		updateHookErrorListeners.addAll(listeners);
	}
	
	public void registerContextUpdateErrorListener(ContextUpdateHookErrorListener listener) {
		updateHookErrorListeners.add(listener);
	}
	
	public void removeContextUpdateErrorListener(ContextUpdateHookErrorListener listener) {
		updateHookErrorListeners.remove(listener);
	}
	
	@Override
	public void run() {
		if (hookList != null) {
			for (int i = 0; i < hookList.size(); i++) {
				ContextUpdateHook hook = hookList.get(i);
				
				// System.out.println("Executing context update action: " + hook);
				if (!hook.exec()) {
					System.out.println("Action ERROR!");
					for (ContextUpdateHookErrorListener listener : updateHookErrorListeners) {
						listener.notifyUpdateHookError(hook);
					}
				}
			}
		}
	}
	
}
