package ime.mac5725.earley.recognizer;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class EarleyFinger extends Earley {
	
	private LinkedList<String> recursiveCycle = new LinkedList<String>();
	
	public boolean recognize(String words, LinkedHashSet<String> grammar, LinkedHashSet<String> lexicon) {
		
		prepareVariables(grammar, lexicon);
		prepareSentenceWords(words);

		enqueue(DUMMY_STATE, chart.get(0));
		printHeadRule();
		
		for(; i<=sentenceWords.size(); i++) {
			
			while(j<chart.get(i).size()) {
				
				getNextStateToRun();
			
//				System.out.println("\n-STATE: " + state);//
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
		
		for(Iterator it=lexicon.iterator(); it.hasNext(); ) 
			if(it.next().toString().split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(nextCategory))
				return true;
			
		return false;
		
	}
	
	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		sentenceHeadRule = sentenceHeadRuleCount == 0 ? currentPOSTag : sentenceHeadRule;
		sentenceHeadRuleCount = sentenceHeadRuleCount == 0 ? sentenceHeadRuleCount+1 : sentenceHeadRuleCount;
		
		for(Iterator it=grammar.iterator(); it.hasNext(); ) {
			
			String rule = it.next().toString();
			
			if(getRule(rule).equals(currentPOSTag) && !isRuleAlreadyInCurrentChart(rule) && specialCase(rule)) {
				
				stateLevelCount++;
				enqueue(addStateAndStartEndPointsFields(rule), chart.get(i));
				printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i));
				
			}
			
		}
		
		j++;
		
	}
	
	/**
	 * @author rayssak
	 * @param rule
	 * @return Improvements due to corpus properties)
	 */
	private boolean specialCase(String rule) {
			   // Removing VB rules that are not part of lexicon 
		return !getRule(rule).equals("VB");
			   // Removing the only one WPP lexicon rule
			   // (this is not required since the PREDICTOR
			   //  only run through the GRAMMAR rules list, not the
		       //  FULLGRAMMAR rules that includes LEXICON)
//			   && (getRule(rule).equals("WPP") && !getTerminal(state).matches("[0-9]+"));
	}
	
	protected void scanner(String state) {
		super.scanner(state, getTerminal(state));
	}

	private String getTerminal(String state) {
		
		String terminal = "";
		String tmp[] = changeFieldSeparator(state).split(" ");
		
		for(int aux=0; aux<tmp.length; aux++)
			if(!tmp[aux].equals(ConstantsUtility.DOTTED_RULE) && tmp[aux].replace("-", "").matches("[A-Z]+"))
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
		System.out.println(state);
		
		if(state.contains("PP *"))
			System.out.println("here");

		if(isRecursiveRule(state)) {
			j++;
			return;
		}
		
		for(int count=0; count<chart.get(chartCount).size(); count++) {
			
			String rule = chart.get(chartCount).get(count);
			int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].substring(0, 1));
			
			if(!isComplete(rule) && ruleEnd==stateStart) {
				
				String cleanNonTerminal = rule.substring(rule.indexOf(ConstantsUtility.DOTTED_RULE)+2, rule.indexOf("[")-1).split(" ")[0];
				String tmpRule = rule.substring(0, rule.indexOf('['));
				if(rule.contains("S536"))
					System.out.println("here");
				
				if(ruleFullyProcessedAndNotInChart(tmp, rule, cleanNonTerminal)) {
					
					previousState = state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
					int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
					tmp.add(tmpRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
					tmpRule = tmpRule.replace(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag, currentPOSTag + " " + ConstantsUtility.DOTTED_RULE)
								.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "S" + ++stateLevelCount + ConstantsUtility.FIELD_SEPARATOR) + 
								"[" + ruleStart + "," + i + "]";
					finalParser.add("tmp: " + tmpRule + ConstantsUtility.FIELD_SEPARATOR + "(" + previousState + ")");
					
					enqueue(tmpRule, chart.get(i));
					System.out.println("----- " + state);//
					printRule(tmpRule, Methods.COMPLETER.name(), String.valueOf(i));
					
				}
				
			} 
			
			if(isComplete(rule) && hasCompletedSentence(rule))
				grammarRecognized = true;
			if(isComplete(rule) && isFinalStateToGrammarTree(rule))
				addToFinalParser(rule, "(" + previousState + ")");
			
			if(isLastChartItem(chartCount, rule) && chartCount < chart.size()-1) {
				count = -1;
				chartCount++;
			}
			
		}
		
		handleRecursiveCycle(state);
		j++;
		
	}

	private boolean isRecursiveRule(String state) {
		return currentPOSTag.equals(sentenceHeadRule) && getRule(state).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(
				state.split(ConstantsUtility.NEXT_ELEMENT_CHAR + " ")[1].split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0].replace(" " + ConstantsUtility.DOTTED_RULE, "").replace(ConstantsUtility.DOTTED_RULE, ""));
	}
	
	private void handleRecursiveCycle(String state) {
		if(recursiveCycle.size()>4) 
			recursiveCycle.removeFirst();
		recursiveCycle.add(state);
	}
	
}