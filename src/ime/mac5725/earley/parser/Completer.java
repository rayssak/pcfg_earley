package ime.mac5725.earley.parser;

import ime.mac5725.earley.Earley;
import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Collections;

public class Completer extends Earley implements Runnable {
	
	private int i;
	private int size;
	
	private String currentPOSTag;
	private String state;
	
	private ArrayList<String> partialChart;
	private ArrayList<String> rules;
	
	public Completer(int i, String state, ArrayList<String> partialChart, String currentPOSTag, int size) {
		this.i = i;
		this.size = size;
		this.state = state;
		this.partialChart = partialChart;
		this.currentPOSTag = currentPOSTag;
		this.rules = new ArrayList<String>();
	}

	@Override
	public void run() {
		
		int currentPOSTagCount = 1;
		currentPOSTag = nextCompletedCategory(state);

		while(currentPOSTagCount > 0 && partialChart.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag) >= 0) {

			currentPOSTagCount = Collections.frequency(partialChart, ConstantsUtility.DOTTED_RULE + " " + currentPOSTag);
			int ruleIndex = partialChart.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag);
			rules.add(chart.get(i).get(ruleIndex+size));
			partialChart.set(partialChart.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag), "");
				
		}
		
		synchronized (rulesToComplete) {
			
			rulesToComplete.addAll(rules);
			rules.clear();
			threadCompletedCount++;
			
			if(threadCompletedCount >= threadCount) {
				threadRuleCompleted = true;
				rulesToComplete.notifyAll();
			}
			
		}
		
	}
	
}