package ime.mac5725.earley.parallel;

import ime.mac5725.earley.Earley;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author rayssak
 * @reason Thread to implement PREDICTOR's task parallel.
 */
public class Predictor extends Earley implements Runnable {
	
	private int size;
	
	private String state;
	private String currentPOSTag;
	
	private ArrayList<String> partialGrammar;
	
	public Predictor(String state, ArrayList<String> partialGrammar, String currentPOSTag, int size) {
		this.state = state;
		this.size = size;
		this.currentPOSTag = currentPOSTag;
		this.partialGrammar = partialGrammar;
	}

	@Override
	public void run() {
		
		int currentPOSTagCount = 1;
		currentPOSTag = nextCategory(state);
		sentenceHeadRule = sentenceHeadRuleCount == 0 ? currentPOSTag : sentenceHeadRule;
		sentenceHeadRuleCount = sentenceHeadRuleCount == 0 ? sentenceHeadRuleCount+1 : sentenceHeadRuleCount;

		while(currentPOSTagCount > 0 && partialGrammar.indexOf(currentPOSTag) >= 0) {
			
			currentPOSTagCount = Collections.frequency(partialGrammar, getRule(currentPOSTag));
			int ruleIndex = partialGrammar.indexOf(getRule(currentPOSTag));
			
			// Insert each grammar rule of the current state being processed
			// into the chart in case the rule is not already in the current
			// chart (not all of them).
			String rule = grammar.get(ruleIndex+size);
			if(!currentChartWithRules.contains(rule) && specialCase(rule))
				
				if(enqueue(addStateAndStartEndPointsFields(rule), chart.get(i), i)) {
					stateLevelCount++;
					printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i), state);
				
				}
			
			partialGrammar.set(partialGrammar.indexOf(currentPOSTag), "");
			
		}
		
		synchronized (rulesToPredict) {
			
			threadCompletedCount++;
			
			if(threadCompletedCount >= threadCount) {
				threadRuleCompleted = true;
				rulesToPredict.notifyAll();
			}
			
		}
		
	}
	
}