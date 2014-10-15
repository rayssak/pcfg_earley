package ime.mac5725.earley.recognizer;

import ime.mac5725.earley.Predictor;
import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class EarleyFinger extends Earley {
	
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
				else if(!isComplete(state))
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
		
		for(int aux=0; aux<lexicon.size(); aux++)
			if(lexicon.get(aux).split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(nextCategory))
				return true;
			
		return false;
		
	}
	
	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		sentenceHeadRule = sentenceHeadRuleCount == 0 ? currentPOSTag : sentenceHeadRule;
		sentenceHeadRuleCount = sentenceHeadRuleCount == 0 ? sentenceHeadRuleCount+1 : sentenceHeadRuleCount;
		
		long timeRan = System.currentTimeMillis();
		
		int chartByThreadSize = grammar.size()/3;
		ArrayList<String> grammarFirstThread = new ArrayList<String>();
		ArrayList<String> grammarSecondThread = new ArrayList<String>();
		ArrayList<String> grammarThirdThread = new ArrayList<String>();
		
		for (int aux=0; aux<chartByThreadSize; aux++) 
			grammarFirstThread.add(grammar.get(aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			grammarSecondThread.add(grammar.get(chartByThreadSize + aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			grammarThirdThread.add(grammar.get(chartByThreadSize + chartByThreadSize + aux));
		
		Predictor predictor = new Predictor(chart.get(i), i, stateLevelCount, grammarFirstThread, currentPOSTag);
		Thread firstThread = new Thread(predictor);
		firstThread.start();
		Predictor predictor2 = new Predictor(chart.get(i), i, stateLevelCount, grammarSecondThread, currentPOSTag);
		Thread secondThread = new Thread(predictor2);
		secondThread.start();
		Predictor predictor3 = new Predictor(chart.get(i), i, stateLevelCount, grammarThirdThread, currentPOSTag);
		Thread thirdThread = new Thread(predictor3);
		thirdThread.start();
		
//		for(int aux=0; aux<grammar.size(); aux++) {
//			
//			String rule = grammar.get(aux);
//			if(getRule(rule).equals(currentPOSTag) && !isRuleAlreadyInCurrentChart(rule) && specialCase(rule)) {
//				
//				stateLevelCount++;
//				if(enqueue(addStateAndStartEndPointsFields(rule), chart.get(i)))
//					printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i));
//				
//			}
//			
//		}
		
		while(firstThread.isAlive() || secondThread.isAlive() || thirdThread.isAlive())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		timeRan = System.currentTimeMillis() - timeRan;
		System.out.println(timeRan);
		
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
			if(!tmp[aux].equals(ConstantsUtility.DOTTED_RULE) && (tmp[aux].replace("-", "").matches("[A-Z]+.*") || isPontuation(tmp[aux].charAt(0))))
				if(aux>0 && tmp[aux-1].equals(ConstantsUtility.DOTTED_RULE) && !posAlreadyProcessed(tmp[aux])) {
					terminal = tmp[aux];
					break;
				}
		
		return terminal;
		
	}
	
	private boolean isPontuation(char currentLetter) {
		return currentLetter == ':' || currentLetter == ';' || currentLetter == ',' || currentLetter == '.' || currentLetter == '!' || currentLetter == '?';
	}
	
	private void completer(String state) {
		
		int chartCount = 0;
		int stateStart = Integer.parseInt(state.split("\\[")[1].split(",")[0]);
		currentPOSTag = nextCompletedCategory(state);
		LinkedList<String> tmp = new LinkedList<String>();
		
		if(isComplete(state) && hasCompletedSentence(state)) 
			grammarRecognized = true;
		
		int chartByThreadSize = chart.get(chartCount).size()/3;
		ArrayList<String> chartFirstThread = new ArrayList<String>();
		ArrayList<String> chartSecondThread = new ArrayList<String>();
		ArrayList<String> chartThirdThread = new ArrayList<String>();
		
		for (int aux=0; aux<chartByThreadSize; aux++) 
			chartFirstThread.add(chart.get(chartCount).get(aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			chartSecondThread.add(chart.get(chartCount).get(chartByThreadSize + aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			chartThirdThread.add(chart.get(chartCount).get(chartByThreadSize + chartByThreadSize + aux));
		
		
		for(int count=0; count<chart.get(chartCount).size(); count++) {
			
			String rule = chart.get(chartCount).get(count);
			int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].substring(0, 1));
			
			if(!isComplete(rule) /*&& ruleEnd==stateStart*/) {
				
				String cleanNonTerminal = rule.substring(rule.indexOf(ConstantsUtility.DOTTED_RULE)+2, rule.indexOf("[")-1).split(" ")[0];
				String tmpRule = rule.substring(0, rule.indexOf('['));
				
				if(currentPOSTag.equals(cleanNonTerminal) && ruleFullyProcessedAndNotInChart(tmp, rule, cleanNonTerminal)) {
					
					previousState = state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
					int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
					tmp.add(tmpRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
					tmpRule = tmpRule.replace(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag, currentPOSTag + " " + ConstantsUtility.DOTTED_RULE)
								.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "S" + ++stateLevelCount + ConstantsUtility.FIELD_SEPARATOR) + 
								"[" + ruleStart + "," + i + "]";
					
					if(enqueue(tmpRule, chart.get(i))) {
						printRule(tmpRule, Methods.COMPLETER.name(), String.valueOf(i));
						finalParser.add("tmp: " + tmpRule + ConstantsUtility.FIELD_SEPARATOR + "(" + previousState + ")");
					}
					
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
			
			if(grammarRecognized)
				return;
			
		}
		j++;
		
	}
	
}