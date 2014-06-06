package org.aimas.ami.contextrep.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.aimas.ami.contextrep.update.performance.AssertionInferenceResult;
import org.aimas.ami.contextrep.update.performance.AssertionInsertResult;

public class PerformanceRunner {
	private static int insertionCounter = 0;
	private static int inferenceCounter = 0;
	
	public synchronized int getInsertCounterID() {
		return insertionCounter++;
	}
	
	public synchronized int getInferenceCounterID() {
		return inferenceCounter++;
	}
	
	public PerformanceRunner() {
		// TODO Auto-generated constructor stub
	}
	
	public AtomicInteger executedInsertionsTracker = new AtomicInteger(0);
	public AtomicInteger enqueuedInferenceTracker = new AtomicInteger(0);
	public AtomicInteger inferredAssertionsTracker = new AtomicInteger(0);
	
	public Map<Integer, Long> insertionTaskEnqueueTime = new HashMap<>();
	public Map<Integer, Long> inferenceTaskEnqueueTime = new HashMap<>();
	public Map<Integer, Integer> numNamedGraphs = new HashMap<>();
	
	public Map<Integer, Future<AssertionInsertResult>> insertionResults = new HashMap<>();
	public Map<Integer, Future<AssertionInferenceResult>> inferenceResults = new HashMap<>();
	
	public void incInferredAssertions() {
		inferredAssertionsTracker.getAndIncrement();
	}
	
	public void incExecInsertions() {
		executedInsertionsTracker.getAndIncrement();
	}
	
	public void incEnqueuedInferences() {
		enqueuedInferenceTracker.getAndIncrement();
	}
	
	public void markInsertionTime(int assertionID, long timestamp) {
		insertionTaskEnqueueTime.put(assertionID, timestamp);
	}
	
	public void markInferenceTime(int assertionID, long timestamp) {
		inferenceTaskEnqueueTime.put(assertionID, timestamp);
	}
}
