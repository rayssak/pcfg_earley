package ime.mac5725.earley;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author rayssak
 * @reason Thread to implement COMPLETER's task parallel.
 */
public class Completer extends Earley implements Runnable {
	
	private int chartCount;
	private int size;
	
	private String state;
	
	private ArrayList<String> partialChart;
	
	public Completer(int chartCount, String state, ArrayList<String> partialChart, int size) {
		this.chartCount = chartCount;
		this.state = state;
		this.size = size;
		this.partialChart = partialChart;
	}

	@Override
	public void run() {
		
		int currentPOSTagCount = 1;
		int stateStart = Integer.parseInt(state.split("\\[")[1].split(",")[0]);
		currentPOSTag = nextCompletedCategory(state);
		ArrayList<String> tmp = new ArrayList<String>();
		
		// Checks if a final state entry was already processed and the
		// sentence already recognized.
		if(isComplete(state) && hasCompletedSentence(state)) 
			grammarRecognized = true;
		
		// Checks each one of the gathered rules...
		while(!grammarRecognized && currentPOSTagCount > 0 && partialChart.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag) >= 0) {
			
			currentPOSTagCount = Collections.frequency(partialChart, ConstantsUtility.DOTTED_RULE + " " + currentPOSTag);
			int ruleIndex = partialChart.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag);
			String currentRule = "";
			
			synchronized (chart.get(chartCount)) {
				currentRule = chart.get(chartCount).get(ruleIndex+size);
			}
			
			int ruleEnd = Integer.parseInt(currentRule.split("\\[")[1].split(",")[1].replace("]", ""));
			if(ruleEnd==stateStart) {
				
				// ...and check if there is any of them ending in the state state position to 
				// mark as complete.
				// e.g.: 
				// 		~ Current state: N-> virtudes [1,2]
				// 		~ Current rule: NP-> * N [0,1]
				if(!isComplete(currentRule) && ruleEnd==stateStart && !currentRule.contains(DUMMY_STATE)) {
					
					String cleanNonTerminal = currentRule.substring(currentRule.indexOf(ConstantsUtility.DOTTED_RULE)+2, currentRule.indexOf("[")-1).split(" ")[0];
					String tmpRule = currentRule.substring(0, currentRule.indexOf('['));
					
					if(cleanNonTerminal.equals(currentPOSTag)) {
						
						previousState = state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
						int ruleStart = Integer.parseInt(currentRule.split("\\[")[1].split(",")[0]);
						tmp.add(tmpRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
						tmpRule = tmpRule.replace(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag, currentPOSTag + " " + ConstantsUtility.DOTTED_RULE)
									.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "S" + ++stateLevelCount + ConstantsUtility.FIELD_SEPARATOR) + 
									"[" + ruleStart + "," + i + "]";
						
						if(enqueue(tmpRule, chart.get(i), i)) {
							printRule(tmpRule, Methods.COMPLETER.name(), String.valueOf(i), state);
							synchronized (finalParser) {
								finalParser.add("tmp: " + tmpRule + ConstantsUtility.FIELD_SEPARATOR + "(" + previousState + ")");
							}
						}
						
						currentRule = tmpRule;
						
					}
					
				}
				
				// Handles grammar tree rules and end of the sentence
				if(isComplete(currentRule) && hasCompletedSentence(currentRule)) {
					finalState = currentRule;
					grammarRecognized = true;
				}
				if(isComplete(currentRule) && isFinalStateToGrammarTree(currentRule))
					addToFinalParser(currentRule, "(" + previousState + ")");
				
			}
			
			if(partialChart.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag) > 0)
				partialChart.set(partialChart.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag), "");
			if(grammarRecognized)
				break;
		}
		
		synchronized (rulesToComplete) {
			
			threadCompletedCount++;
			
			if(threadCompletedCount >= threadCount || grammarRecognized) {
				threadRuleCompleted = true;
				rulesToComplete.notifyAll();
			}
			
		}
		
	}
	
}