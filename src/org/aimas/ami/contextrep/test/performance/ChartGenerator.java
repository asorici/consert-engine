package org.aimas.ami.contextrep.test.performance;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;
import org.jfree.util.PaintUtilities;
import org.jfree.util.ShapeUtilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ChartGenerator {
	private static final String TEST_DIR = "tests" + File.separator + "performance";
	
	
	private static void createCharts() throws IOException {
		// list all folders in the TEST_DIR - each one corresponds to a different configuration setting
		File testsFolder = new File(TEST_DIR);
		File[] testConfigurations = testsFolder.listFiles();
		
		for (int i = 0; i < testConfigurations.length; i++) {
			File testConfigFolder = testConfigurations[i];
			if (testConfigFolder.isDirectory()) {
				// list all collected json result files
				File[] resultFiles = testConfigFolder.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.getPath().endsWith(".json"); 
					}
				});
				
				System.out.println("## Generating charts for config: " + testConfigFolder.getName());
				
				Map<String, PerformanceMeasure> collectedResults = getCollectedResults(resultFiles);
				//createGeneralCharts(collectedResults, testConfigFolder);
				createHistoryCharts(collectedResults, testConfigFolder);
			}
		}
	}
	

	private static Map<String, PerformanceMeasure> getCollectedResults(File[] resultFiles) {
		Map<String, PerformanceMeasure> collectedResults = new HashMap<>();
		for (int i = 0; i < resultFiles.length; i++) {
			File resultFile = resultFiles[i];
			String jsonContent = readFile(resultFile); 
			
			Gson gson = new Gson();
			PerformanceMeasure measure = gson.fromJson(jsonContent, PerformanceMeasure.class);
			
			collectedResults.put(resultFile.getName(), measure);
		}
		
	    return collectedResults;
    }
	
	
	private static PerformanceMeasure getCumulatedMeasure(Map<String, PerformanceMeasure> collectedResults) {
		List<PerformanceMeasure> resultMeasures = new LinkedList<>(collectedResults.values());
	    
	    PerformanceMeasure cumulatedMeasure = new PerformanceMeasure();
	    cumulatedMeasure.performanceConfig = resultMeasures.get(0).performanceConfig;
	    
	    for (PerformanceMeasure pm : resultMeasures) {
	    	cumulatedMeasure.performanceResult.minInsertionDuration = 0;
	    	cumulatedMeasure.performanceResult.averageInsertionDuration = 0;
	    	cumulatedMeasure.performanceResult.maxInsertionDuration = 0;
	    	
	    	cumulatedMeasure.performanceResult.minInsertionDelay = 0;
	    	cumulatedMeasure.performanceResult.averageInsertionDelay = 0;
	    	cumulatedMeasure.performanceResult.maxInsertionDelay = 0;
	    	
	    	cumulatedMeasure.performanceResult.minInferenceDelay = 0;
	    	cumulatedMeasure.performanceResult.averageInferenceDelay = 0;
	    	cumulatedMeasure.performanceResult.maxInferenceDelay = 0;
	    	
	    	cumulatedMeasure.performanceResult.minInferenceCheckDuration = 0;
	    	cumulatedMeasure.performanceResult.averageInferenceCheckDuration = 0;
	    	cumulatedMeasure.performanceResult.maxInferenceCheckDuration = 0;
	    	
	    	cumulatedMeasure.performanceResult.minContinuityCheckDuration = 0;
	    	cumulatedMeasure.performanceResult.averageContinuityCheckDuration = 0;
	    	cumulatedMeasure.performanceResult.maxContinuityCheckDuration = 0;
	    	
	    	cumulatedMeasure.performanceResult.minConstraintCheckDuration = 0;
	    	cumulatedMeasure.performanceResult.averageConstraintCheckDuration = 0;
	    	cumulatedMeasure.performanceResult.maxConstraintCheckDuration = 0;
	    	
	    	cumulatedMeasure.performanceResult.minDeductionCycleDuration = 0;
	    	cumulatedMeasure.performanceResult.averageDeductionCycleDuration = 0;
	    	cumulatedMeasure.performanceResult.maxDeductionCycleDuration = 0;
	    }
	    
	    for (PerformanceMeasure pm : resultMeasures) {
	    	cumulatedMeasure.performanceResult.minInsertionDuration += pm.performanceResult.minInsertionDuration;
	    	cumulatedMeasure.performanceResult.averageInsertionDuration += pm.performanceResult.averageInsertionDuration;
	    	cumulatedMeasure.performanceResult.maxInsertionDuration += pm.performanceResult.maxInsertionDuration;
	    	
	    	cumulatedMeasure.performanceResult.minInsertionDelay += pm.performanceResult.minInsertionDelay;
	    	cumulatedMeasure.performanceResult.averageInsertionDelay += pm.performanceResult.averageInsertionDelay;
	    	cumulatedMeasure.performanceResult.maxInsertionDelay += pm.performanceResult.maxInsertionDelay;
	    	
	    	cumulatedMeasure.performanceResult.minInferenceDelay += pm.performanceResult.minInferenceDelay;
	    	cumulatedMeasure.performanceResult.averageInferenceDelay += pm.performanceResult.averageInferenceDelay;
	    	cumulatedMeasure.performanceResult.maxInferenceDelay += pm.performanceResult.maxInferenceDelay;
	    	
	    	cumulatedMeasure.performanceResult.minInferenceCheckDuration += pm.performanceResult.minInferenceCheckDuration;
	    	cumulatedMeasure.performanceResult.averageInferenceCheckDuration += pm.performanceResult.averageInferenceCheckDuration;
	    	cumulatedMeasure.performanceResult.maxInferenceCheckDuration += pm.performanceResult.maxInferenceCheckDuration;
	    	
	    	cumulatedMeasure.performanceResult.minContinuityCheckDuration += pm.performanceResult.minContinuityCheckDuration;
	    	cumulatedMeasure.performanceResult.averageContinuityCheckDuration += pm.performanceResult.averageContinuityCheckDuration;
	    	cumulatedMeasure.performanceResult.maxContinuityCheckDuration += pm.performanceResult.maxContinuityCheckDuration;
	    	
	    	cumulatedMeasure.performanceResult.minConstraintCheckDuration += pm.performanceResult.minConstraintCheckDuration;
	    	cumulatedMeasure.performanceResult.averageConstraintCheckDuration += pm.performanceResult.averageConstraintCheckDuration;
	    	cumulatedMeasure.performanceResult.maxConstraintCheckDuration += pm.performanceResult.maxConstraintCheckDuration;
	    	
	    	cumulatedMeasure.performanceResult.minDeductionCycleDuration += pm.performanceResult.minDeductionCycleDuration;
	    	cumulatedMeasure.performanceResult.averageDeductionCycleDuration += pm.performanceResult.averageDeductionCycleDuration;
	    	cumulatedMeasure.performanceResult.maxDeductionCycleDuration += pm.performanceResult.maxDeductionCycleDuration;
	    }
	    
	    int numResults = resultMeasures.size();
	    
	    cumulatedMeasure.performanceResult.minInsertionDuration /= numResults;
    	cumulatedMeasure.performanceResult.averageInsertionDuration /= numResults;
    	cumulatedMeasure.performanceResult.maxInsertionDuration /= numResults;
    	
    	cumulatedMeasure.performanceResult.minInsertionDelay /= numResults;
    	cumulatedMeasure.performanceResult.averageInsertionDelay /= numResults;
    	cumulatedMeasure.performanceResult.maxInsertionDelay /= numResults;
    	
    	cumulatedMeasure.performanceResult.minInferenceDelay /= numResults;
    	cumulatedMeasure.performanceResult.averageInferenceDelay /= numResults;
    	cumulatedMeasure.performanceResult.maxInferenceDelay /= numResults;
    	
    	cumulatedMeasure.performanceResult.minInferenceCheckDuration /= numResults;
    	cumulatedMeasure.performanceResult.averageInferenceCheckDuration /= numResults;
    	cumulatedMeasure.performanceResult.maxInferenceCheckDuration /= numResults;
    	
    	cumulatedMeasure.performanceResult.minContinuityCheckDuration /= numResults;
    	cumulatedMeasure.performanceResult.averageContinuityCheckDuration /= numResults;
    	cumulatedMeasure.performanceResult.maxContinuityCheckDuration /= numResults;
    	
    	cumulatedMeasure.performanceResult.minConstraintCheckDuration /= numResults;
    	cumulatedMeasure.performanceResult.averageConstraintCheckDuration /= numResults;
    	cumulatedMeasure.performanceResult.maxConstraintCheckDuration /= numResults;
    	
    	cumulatedMeasure.performanceResult.minDeductionCycleDuration /= numResults;
    	cumulatedMeasure.performanceResult.averageDeductionCycleDuration /= numResults;
    	cumulatedMeasure.performanceResult.maxDeductionCycleDuration /= numResults;
    	
    	return cumulatedMeasure;
	}
	
	
	private static void createGeneralCharts(Map<String, PerformanceMeasure> collectedResults, File configFolder) throws IOException {
	    PerformanceMeasure cumulatedMeasure = getCumulatedMeasure(collectedResults);
	    
	    // get JSON output of cumulated run statistics
	 	Gson gsonOut = new GsonBuilder().setPrettyPrinting().create();
	 	String jsonOutput = gsonOut.toJson(cumulatedMeasure, PerformanceMeasure.class);
	 	
	 	// output cumulated measures
	 	File cumulatedMeasureFile = new File(configFolder.getAbsolutePath() + File.separator + "cumulated.json");
	 	writeFile(cumulatedMeasureFile, jsonOutput);
	 	
	    File plot = null;
	    
	    // ================ create insertion duration bar chart ================
	    DefaultCategoryDataset insertionDurationDataset = new DefaultCategoryDataset();
	    insertionDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.minInsertionDuration, MinMaxAverageDisplay.min(), "InsertionDuration");
	    insertionDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.averageInsertionDuration, MinMaxAverageDisplay.average(), "InsertionDuration");
	    insertionDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.maxInsertionDuration, MinMaxAverageDisplay.max(), "InsertionDuration");
	    
	    JFreeChart insertionDurationChart = setupGeneralChart("Insertion Duration", "statistics", "ms", insertionDurationDataset);
	    plot = new File(configFolder.getAbsolutePath() + File.separator + "InsertionDuration.png"); 
	    ChartUtilities.saveChartAsPNG(plot, insertionDurationChart, 400, 300);
	    
	    // ================ create insertion delay bar chart ================
	    DefaultCategoryDataset insertionDelayDataset = new DefaultCategoryDataset();
	    insertionDelayDataset.addValue(
	    	cumulatedMeasure.performanceResult.minInsertionDelay, MinMaxAverageDisplay.min(), "InsertionDelay");
	    insertionDelayDataset.addValue(
	    	cumulatedMeasure.performanceResult.averageInsertionDelay, MinMaxAverageDisplay.average(), "InsertionDelay");
	    insertionDelayDataset.addValue(
	    	cumulatedMeasure.performanceResult.maxInsertionDelay, MinMaxAverageDisplay.max(), "InsertionDelay");
	    
	    JFreeChart insertionDelayChart = setupGeneralChart("Insertion Delay", "statistics", "ms", insertionDelayDataset);
	    plot = new File(configFolder.getAbsolutePath() + File.separator + "InsertionDelay.png"); 
	    ChartUtilities.saveChartAsPNG(plot, insertionDelayChart, 400, 300);
	    
	    
	    // ================ create inference duration bar chart ================
	    DefaultCategoryDataset inferenceDurationDataset = new DefaultCategoryDataset();
	    inferenceDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.minInferenceCheckDuration, MinMaxAverageDisplay.min(), "InferenceDuration");
	    inferenceDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.averageInferenceCheckDuration, MinMaxAverageDisplay.average(), "InferenceDuration");
	    inferenceDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.maxInferenceCheckDuration, MinMaxAverageDisplay.max(), "InferenceDuration");
	    
	    JFreeChart inferenceDurationChart = setupGeneralChart("Inference Duration", "statistics", "ms", inferenceDurationDataset);
	    plot = new File(configFolder.getAbsolutePath() + File.separator + "InferenceDuration.png"); 
	    ChartUtilities.saveChartAsPNG(plot, inferenceDurationChart, 400, 300);
	    
	    
	    // ================ create inference delay bar chart ================
	    DefaultCategoryDataset inferenceDelayDataset = new DefaultCategoryDataset();
	    inferenceDelayDataset.addValue(
	    	cumulatedMeasure.performanceResult.minInferenceDelay, MinMaxAverageDisplay.min(), "inferenceDelay");
	    inferenceDelayDataset.addValue(
	    	cumulatedMeasure.performanceResult.averageInferenceDelay, MinMaxAverageDisplay.average(), "inferenceDelay");
	    inferenceDelayDataset.addValue(
	    	cumulatedMeasure.performanceResult.maxInferenceDelay, MinMaxAverageDisplay.max(), "inferenceDelay");
	    
	    JFreeChart inferenceDelayChart = setupGeneralChart("Inference Delay", "statistics", "ms", inferenceDelayDataset);
	    plot = new File(configFolder.getAbsolutePath() + File.separator + "InferenceDelay.png"); 
	    ChartUtilities.saveChartAsPNG(plot, inferenceDelayChart, 400, 300);
	    
	    
	    // ================ create deductin cycle duration bar chart ================
	    DefaultCategoryDataset deductionCycleDurationDataset = new DefaultCategoryDataset();
	    deductionCycleDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.minDeductionCycleDuration, MinMaxAverageDisplay.min(), "deductionCycleDuration");
	    deductionCycleDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.averageDeductionCycleDuration, MinMaxAverageDisplay.average(), "deductionCycleDuration");
	    deductionCycleDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.maxDeductionCycleDuration, MinMaxAverageDisplay.max(), "deductionCycleDuration");
	    
	    JFreeChart deductionCycleDurationChart = setupGeneralChart("Deduction Cycle Duration", "statistics", "ms", 
	    	deductionCycleDurationDataset);
	    plot = new File(configFolder.getAbsolutePath() + File.separator + "DeductionCycleDuration.png"); 
	    ChartUtilities.saveChartAsPNG(plot, deductionCycleDurationChart, 400, 300);
	    
	    
	    // ================ create continuity check duration bar chart ================
	    DefaultCategoryDataset continuityCheckDurationDataset = new DefaultCategoryDataset();
	    continuityCheckDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.minContinuityCheckDuration, MinMaxAverageDisplay.min(), "ContinuityCheckDuration");
	    continuityCheckDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.averageContinuityCheckDuration, MinMaxAverageDisplay.average(), "ContinuityCheckDuration");
	    continuityCheckDurationDataset.addValue(
	    	cumulatedMeasure.performanceResult.maxContinuityCheckDuration, MinMaxAverageDisplay.max(), "ContinuityCheckDuration");
	    
	    JFreeChart continuityCheckDurationChart = setupGeneralChart("Continuity Check Duration", "statistics", "ms", 
	    	continuityCheckDurationDataset);
	    plot = new File(configFolder.getAbsolutePath() + File.separator + "ContinuityCheckDuration.png"); 
	    ChartUtilities.saveChartAsPNG(plot, continuityCheckDurationChart, 400, 300);
    }
	
	
	private static void createHistoryCharts(Map<String, PerformanceMeasure> collectedResults, File configFolder) throws IOException {
	    int skipRate = 20;
		int phase = 0;
	    
		for (String resultName : collectedResults.keySet()) {
	    	PerformanceMeasure measure = collectedResults.get(resultName);
	    	
	    	XYSeriesCollection insertionDatasetCollection = new XYSeriesCollection();
	    	XYSeriesCollection inferenceDatasetCollection = new XYSeriesCollection();
	    	XYBarDataset inferenceBarDataset = new XYBarDataset(inferenceDatasetCollection, 16);
	    	//CategoryDataset infereceBarDataset = new DefaultCategoryDataset();
	    	
	    	XYSeriesCollection deductionCycleDatasetCollection = new XYSeriesCollection();
	    	XYBarDataset deductionBarDataset = new XYBarDataset(deductionCycleDatasetCollection, 8);
	    	
	    	// ============ create the insertion series ============
	    	XYSeries insertionDelaySeries = new XYSeries("Insert Delay", true);
	    	XYSeries insertionDurationSeries = new XYSeries("Insert Processing", true);
	    	
	    	List<Integer> sortedInsertionIDs = new ArrayList<Integer>(measure.performanceResult.insertionDelayHistory.keySet());  
	    	Collections.sort(sortedInsertionIDs);
	    	
	    	for (int insertionID : sortedInsertionIDs) {
	    		if ((insertionID + phase) % skipRate == 0) {
	    			int insertionDelayValue = measure.performanceResult.insertionDelayHistory.get(insertionID);
	    			int insertionDurationValue = measure.performanceResult.insertionDurationHistory.get(insertionID);
    		
	    			insertionDelaySeries.add(insertionID, insertionDelayValue);
	    			insertionDurationSeries.add(insertionID, insertionDurationValue);
	    		}
    		}
	    	
	    	insertionDatasetCollection.addSeries(insertionDelaySeries);
	    	insertionDatasetCollection.addSeries(insertionDurationSeries);
	    	
	    	// ============ create the inference series ============
	    	XYSeries inferenceDelaySeries = new XYSeries("Inference Delay", true);
	    	XYSeries inferenceDurationSeries = new XYSeries("Inference Processing", true);
	    	
	    	
	    	for (Integer insertionID : measure.performanceResult.insertionDelayHistory.keySet()) {
	    		Integer inferenceDelayValue = measure.performanceResult.inferenceDelayHistory.get(insertionID);
	    		Integer inferenceDurationValue = measure.performanceResult.inferenceDurationHistory.get(insertionID);
	    		
	    		if (inferenceDelayValue != null) {
	    			inferenceDelaySeries.add(insertionID, inferenceDelayValue);
	    		}
	    		
	    		if (inferenceDurationValue != null) {
	    			inferenceDurationSeries.add(insertionID, inferenceDurationValue);
	    		}
	    	}
	    	
	    	inferenceDatasetCollection.addSeries(inferenceDelaySeries);
	    	inferenceDatasetCollection.addSeries(inferenceDurationSeries);
	    	
	    	// ============ create the deduction series ============
	    	XYSeries deductionSeries = new XYSeries("Deduction Duration", true);
	    	for (Integer insertionID : measure.performanceResult.insertionDelayHistory.keySet()) {
	    		Integer deductionDurationValue = measure.performanceResult.deductionCycleHistory.get(insertionID);
	    		if (deductionDurationValue != null) {
	    			deductionSeries.add(insertionID, deductionDurationValue);
	    		}
	    	}
	    	
	    	deductionCycleDatasetCollection.addSeries(deductionSeries);
	    	
	    	JFreeChart historyChart = setupHistoryChart(insertionDatasetCollection, inferenceBarDataset, deductionBarDataset);
	    	File historyGraph = new File(configFolder.getAbsolutePath() + File.separator + resultName + "_graph.png"); 
		    ChartUtilities.saveChartAsPNG(historyGraph, historyChart, 800, 600);
	    }
    }
	
	
	private static JFreeChart setupGeneralChart(String title, String domainAxisLabel, String rangeAxisLabel, 
			DefaultCategoryDataset dataset) {
		JFreeChart chart = ChartFactory.createBarChart(
		        title, domainAxisLabel, rangeAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		
		chart.setBackgroundPaint(Color.white);
		BarRenderer br = (BarRenderer) chart.getCategoryPlot().getRenderer();
		br.setItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		br.setBasePositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.HALF_ASCENT_CENTER));
		br.setItemLabelsVisible(true);
		chart.getCategoryPlot().setRenderer(br);
		
		return chart;
	}
	
	
	private static JFreeChart setupHistoryChart(
			XYDataset insertionDataset, XYDataset inferenceDataset, XYDataset deductionDataset) {
		
		NumberAxis insertionRangeAxis = new NumberAxis("ms");
		insertionRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYItemRenderer insertionRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES);
		XYPlot insertionPlot = new XYPlot(insertionDataset, null, insertionRangeAxis, insertionRenderer);
		insertionPlot.setDomainGridlinesVisible(true);
		insertionRenderer.setSeriesStroke(0, new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, 
				new float[] {4, 4}, 0));
		insertionRenderer.setSeriesStroke(1, new BasicStroke(1));
		insertionRenderer.setSeriesShape(0, ShapeUtilities.createRegularCross(2, 2));
		insertionRenderer.setSeriesShape(1, ShapeUtilities.createUpTriangle(4));
		
		NumberAxis inferenceRangeAxis = new NumberAxis("ms");
		inferenceRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		ClusteredXYBarRenderer inferenceRenderer = new ClusteredXYBarRenderer();
		inferenceRenderer.setShadowVisible(false);
		inferenceRenderer.setSeriesPaint(0, PaintUtilities.stringToColor("orange"));
		inferenceRenderer.setSeriesPaint(1, PaintUtilities.stringToColor("black"));
		
		//inferenceRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
        XYPlot inferencePlot = new XYPlot(inferenceDataset, null, inferenceRangeAxis, inferenceRenderer);
        inferencePlot.setDomainGridlinesVisible(true);
        
        NumberAxis deductionRangeAxis = new NumberAxis("ms");
		deductionRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYBarRenderer deductionRenderer = new XYBarRenderer();
		deductionRenderer.setSeriesShape(0, ShapeUtilities.createDiamond(2));
		deductionRenderer.setShadowVisible(false);
		deductionRenderer.setSeriesPaint(0, PaintUtilities.stringToColor("magenta"));
		//deductionRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
        XYPlot deductionPlot = new XYPlot(deductionDataset, null, deductionRangeAxis, deductionRenderer);
        deductionPlot.setDomainGridlinesVisible(true);
        
        NumberAxis domainAxis = new NumberAxis("Insert Event");
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
        plot.add(insertionPlot, 3);
        plot.add(inferencePlot, 2);
        plot.add(deductionPlot, 1);
        
        JFreeChart result = new JFreeChart(
        		"ContextAssertion Runtime Statistics",
        		new Font("SansSerif", Font.BOLD, 12), plot, true);
        
        // result.getLegend().setAnchor(Legend.SOUTH);
        return result;
	}
	
	
	private static String readFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes,"UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
	}
	
	private static void writeFile(File file, String content) {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
			        new FileOutputStream(file), "utf-8"));
			writer.write(content);
		}
		catch (IOException ex) {
		}
		finally {
			try {
				writer.close();
			}
			catch (Exception ex) {
			}
		}
	}
	
	
	private static class MinMaxAverageDisplay implements Comparable<MinMaxAverageDisplay> {
		public static final String[] displayOrder = {"min", "average", "max"}; 
		
		int displayIndex = 0;
		
		private MinMaxAverageDisplay(int displayIndex) {
			this.displayIndex = displayIndex;
        }
		
		static MinMaxAverageDisplay min() {
			return new MinMaxAverageDisplay(0);
		}
		
		static MinMaxAverageDisplay average() {
			return new MinMaxAverageDisplay(1);
		}
		
		static MinMaxAverageDisplay max() {
			return new MinMaxAverageDisplay(2);
		}
		
		@Override
        public int compareTo(MinMaxAverageDisplay o) {
	        return displayIndex - o.displayIndex;
        }
		
		@Override
		public String toString() {
			return displayOrder[displayIndex];
		}
	}
	
	
	public static void main(String[] args) {
		try {
	        createCharts();
        }
        catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}
}
