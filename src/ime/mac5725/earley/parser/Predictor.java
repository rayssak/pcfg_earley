package ime.mac5725.earley.parser;

import ime.mac5725.earley.Earley;

import java.util.ArrayList;
import java.util.Collections;

public class Predictor extends Earley implements Runnable {
	
	private int size;
	
	private String state;
	private String currentPOSTag;
	
	private ArrayList<String> partialGrammar;
	private ArrayList<String> rules;
	
	public Predictor(String state, ArrayList<String> partialGrammar, String currentPOSTag, int size) {
		this.size = size;
		this.state = state;
		this.currentPOSTag = currentPOSTag;
		this.partialGrammar = partialGrammar;
		this.rules = new ArrayList<String>();
	}

	@Override
	public void run() {
		
		int currentPOSTagCount = 1;
		currentPOSTag = nextCategory(state);
		
		while(currentPOSTagCount > 0 && partialGrammar.indexOf(currentPOSTag) >= 0) {
			currentPOSTagCount = Collections.frequency(partialGrammar, getRule(currentPOSTag));
			int ruleIndex = partialGrammar.indexOf(getRule(currentPOSTag));
			rules.add(grammar.get(ruleIndex+size));
			partialGrammar.set(partialGrammar.indexOf(currentPOSTag), "");
		}
		
		synchronized (rulesToPredict) {
			
			rulesToPredict.addAll(rules);
			rules.clear();
			threadCompletedCount++;
			
			if(threadCompletedCount >= threadCount) {
				threadRuleCompleted = true;
				rulesToPredict.notifyAll();
			}
			
		}
		
	}

}