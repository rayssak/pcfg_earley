package ime.mac5725.earley.recognizer;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class EarleyJurafsky extends Earley {
	
	public boolean recognize(String words, LinkedHashSet<String> grammar, LinkedHashSet<String> lexicon) {
		
		prepareVariables(grammar, lexicon);
		prepareSentenceWords(words);

		enqueue(DUMMY_STATE, chart.get(0));
		printHeadRule();
		
		for(; i<=sentenceWords.size(); i++) {
			
			while(!grammarRecognized && j<chart.get(i).size()) {
				
				getNextStateToRun();
			
				if(!isComplete(state) && !isPartOfSpeech(nextCategory(state))) 
					predictor(state);
				else if(!isComplete(state) /*&& isPartOfSpeech(nextCategory(state))*/)
					scanner(state);
				else 
					completer(state);
				
			}
			
			j=0;

		}
		
		return grammarRecognized;
		
	}
	
	/**
	 * @author rayssak
	 * @reason Verifies if the next category is a part-of-speech according
	 * 		   to Jurafsky definition:
	 * 		   		~ 	  POS: Noun, Nominal, Verb, etc (points to another POS);
	 * 				~ Not POS: Aux (points to a lexicon).
	 * @param nextCategory
	 * @return boolean isPOS
	 */
	private boolean isPartOfSpeech(String nextCategory) {
		
		if(!nextCategory.replace("-", "").replace("+", "").matches("[A-Z]+") &&
		    nextCategory.replace("-", "").replace("+", "").matches("[[A-Z]+[a-z]+]+")) {
			
			for(Iterator it=lexicon.iterator(); it.hasNext(); ) 
				if(it.next().toString().split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(nextCategory))
					return true;
			
		}
		
		return false;
		
	}
	
	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		sentenceHeadRule = sentenceHeadRuleCount == 0 ? currentPOSTag : sentenceHeadRule;
		sentenceHeadRuleCount = sentenceHeadRuleCount == 0 ? sentenceHeadRuleCount+1 : sentenceHeadRuleCount;
		
		for(Iterator it=grammar.iterator(); it.hasNext(); ) {
			
			String rule = it.next().toString();
			
			if(getRule(rule).equals(currentPOSTag) && isPOSTag(rule) && 
			  !isRuleAlreadyInCurrentChart(rule) /*&& !chartHasCurretRuleAlreadyCompleted(rule)*/) {
				
				stateLevelCount++;
				if(enqueue(addStateAndStartEndPointsFields(rule), chart.get(i)))
					printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i));
				
			}
			
		}
		
		j++;
		
	}
	
	protected void scanner(String state) {
		super.scanner(state, getTerminal(state));
	}

	private boolean isPOSTag(String posTag) {
		return posTag.matches(".*[A-Z]+.*");
	}
	
//	private boolean chartHasCurretRuleAlreadyCompleted(String rule) {
//
//		int chartCount = 0;
//		String terminalAfterDottedRule = rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1].split(" ")[0];
//		
//		for(int count=0; count<chart.get(chartCount).size(); count++) {
//			
//			String currentTerminals = chart.get(chartCount).get(count).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "")
//										.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0]
//										.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1];
//			
//			if(currentTerminals.contains(terminalAfterDottedRule) && 
//			   !currentTerminals.substring(0, currentTerminals.indexOf(terminalAfterDottedRule)).contains(ConstantsUtility.DOTTED_RULE))
//					return true;
//			
//			if(isLastChartItem(chartCount, chart.get(chartCount).get(count)) && chartCount < chart.size()-1) {
//				count = -1;
//				chartCount++;
//			}
//			
//		}
//		
//		return false;
//		
//	}

	private String getTerminal(String state) {
		
		String terminal = "";
		String tmp[] = changeFieldSeparator(state).split(" ");
		
		for(int aux=0; aux<tmp.length; aux++)
			if(!tmp[aux].equals(ConstantsUtility.DOTTED_RULE) && tmp[aux].replace("-", "").matches("[[A-Z]*[a-z]+]+"))
				if(!posAlreadyProcessed(tmp[aux])) {
					terminal = tmp[aux];
					break;
				}
		
		return terminal;
		
	}
	
	private void completer(String state) {
		
		int chartCount = 0;
		int stateStart = Integer.parseInt(state.split("\\[")[1].split(",")[0]);
		currentPOSTag = nextCompletedCategory(state);
		LinkedList<String> tmp = new LinkedList<String>();
		
		if(isComplete(state) && hasCompletedSentence(state)) 
			grammarRecognized = true;
		
		for(int count=0; count<chart.get(chartCount).size(); count++) {
			
			String rule = chart.get(chartCount).get(count);
			int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].replace("]", ""));
			
			if(!isComplete(rule) && ruleEnd==stateStart && !rule.contains(DUMMY_STATE)) {
				
				String cleanNonTerminal = rule.substring(rule.indexOf(ConstantsUtility.DOTTED_RULE)+2, rule.indexOf("[")-1).split(" ")[0];
				String tmpRule = rule.substring(0, rule.indexOf('['));
				
				if(ruleFullyProcessedAndNotInChart(tmp, rule, cleanNonTerminal)) {
					
					previousState = state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
					int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
					tmp.add(tmpRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
					tmpRule = tmpRule.replace(ConstantsUtility.DOTTED_RULE, "")
								.replace(" " + currentPOSTag, currentPOSTag + " " + ConstantsUtility.DOTTED_RULE)
								.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "S" + ++stateLevelCount + ConstantsUtility.FIELD_SEPARATOR) + 
								"[" + ruleStart + "," + i + "]";
					
					if(enqueue(tmpRule, chart.get(i))) {
						printRule(tmpRule, Methods.COMPLETER.name(), String.valueOf(i));
						finalParser.add("tmp: " + tmpRule + ConstantsUtility.FIELD_SEPARATOR + "(" + previousState + ")");
					}
					
				}
				
			} 
			
			if(sentenceCompleted(rule) && isComplete(rule)) {
				if(hasCompletedSentence(rule)) 
					grammarRecognized = true;
				if(isFinalStateToGrammarTree(rule))
					addToFinalParser(rule, "(" + previousState + ")");
			}
			
			if(isLastChartItem(chartCount, rule) && chartCount < chart.size()-1) {
				count = -1;
				chartCount++;
			}
			
			if(grammarRecognized)
				return;
			
		}
		j++;
		
	}

	private boolean sentenceCompleted(String rule) {
		return i == chart.size()-1 && 
				Integer.parseInt(rule.split("\\[")[1].split(",")[1].substring(0, 1)) == chart.size()-1;
	}
	
}