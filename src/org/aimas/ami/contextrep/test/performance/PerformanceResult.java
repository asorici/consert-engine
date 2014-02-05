package org.aimas.ami.contextrep.test.performance;

import java.util.HashMap;
import java.util.Map;

public class PerformanceResult {
	public static int INF = 10000000;
	
	public int minInsertionDelay = INF;
	public int averageInsertionDelay;
	public int maxInsertionDelay = -INF;
	
	public int minInferenceDelay = INF;
	public int averageInferenceDelay;
	public int maxInferenceDelay = -INF;
	
	public int minDeductionCycleDuration = INF;
	public int averageDeductionCycleDuration;
	public int maxDeductionCycleDuration = -INF;
	
	public int minInsertionDuration = INF;
	public int averageInsertionDuration;
	public int maxInsertionDuration = -INF;
	
	public int minInferenceCheckDuration = INF;
	public int averageInferenceCheckDuration;
	public int maxInferenceCheckDuration = -INF;
	
	public int minContinuityCheckDuration = INF;
	public int averageContinuityCheckDuration;
	public int maxContinuityCheckDuration = -INF;
	
	public int minConstraintCheckDuration = INF;
	public int averageConstraintCheckDuration;
	public int maxConstraintCheckDuration = -INF;
	
	public Map<Integer, Integer> insertionDelayHistory = new HashMap<>();
	public Map<Integer, Integer> inferenceDelayHistory = new HashMap<>();
	
	public Map<Integer, Integer> insertionDurationHistory = new HashMap<>();
	public Map<Integer, Integer> inferenceDurationHistory = new HashMap<>();
	
	public Map<Integer, Integer> deductionCycleHistory = new HashMap<>();
}
