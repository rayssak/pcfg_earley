package ime.mac5725.earley.parser;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class EarleyFinger extends Earley {
	
	public boolean recognize(String words, LinkedHashSet<String> grammar, LinkedHashSet<String> lexicon) {
		
		prepareVariables(grammar, lexicon);
		prepareSentenceWords(words);

		enqueue(DUMMY_STATE, chart.get(0), 0);
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
			
			resetChartControl();
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
		
		for(String current : lexicon)
			if(current.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(nextCategory))
				return true;
			
		return false;
		
	}
	
	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		sentenceHeadRule = sentenceHeadRuleCount == 0 ? currentPOSTag : sentenceHeadRule;
		sentenceHeadRuleCount = sentenceHeadRuleCount == 0 ? sentenceHeadRuleCount+1 : sentenceHeadRuleCount;
				
		int currentPOSTagCount = 1;
		ArrayList<String> tmpIndex = new ArrayList<String>(grammarIndex);
		ArrayList<String> rulesToPredict = new ArrayList<String>();
		
		while(currentPOSTagCount > 0 && tmpIndex.indexOf(currentPOSTag) >= 0) {
			currentPOSTagCount = Collections.frequency(tmpIndex, getRule(currentPOSTag));
			int ruleIndex = tmpIndex.indexOf(getRule(currentPOSTag));
			rulesToPredict.add(grammar.get(ruleIndex));
			tmpIndex.set(tmpIndex.indexOf(currentPOSTag), "");
		}
		
		for(String rule : rulesToPredict)
		
			if(!currentChartWithRules.contains(rule) && specialCase(rule))
				
				if(enqueue(addStateAndStartEndPointsFields(rule), chart.get(i), i)) {
					stateLevelCount++;
					printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i));
				
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
	
	protected void push(String state, ArrayList<String> chartEntry, int i) {
		
		String currentTerminal = getTerminal(state);
		String stateWithoutRule = state.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1];
		ArrayList<String> ruleAndNextTerminal = new ArrayList<String>(Arrays.asList(stateWithoutRule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE, " ").split(" ")));
		
		if(Collections.frequency(ruleAndNextTerminal, currentTerminal) > 1) {
			stateWithoutRule = "";
			List<String> tmp = ruleAndNextTerminal.subList(ruleAndNextTerminal.indexOf(ConstantsUtility.DOTTED_RULE), ruleAndNextTerminal.size());
			for(String element : tmp)
				stateWithoutRule += element + " ";
		}
		
		if(currentTerminal.isEmpty()) {
			String tmp = stateWithoutRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
			tmp = tmp.matches("[A-Za-z�-��-�0-9]+\\s\\*") || tmp.matches("[\\:\\;\\.\\,\\?\\!]\\s\\*") ? tmp : tmp.split(" ")[tmp.split(" ").length-2] + " " + tmp.split(" ")[tmp.split(" ").length-1];
			chartTerminalsIndex.get(i).add(tmp);
		} else if(stateWithoutRule.indexOf(ConstantsUtility.DOTTED_RULE) > stateWithoutRule.indexOf(currentTerminal))
			chartTerminalsIndex.get(i).add(currentTerminal + " " + ConstantsUtility.DOTTED_RULE);
		else
			chartTerminalsIndex.get(i).add(ConstantsUtility.DOTTED_RULE + " " + currentTerminal);
		
		super.push(state, chartEntry, i);
		
	}
	
	protected void scanner(String state) {
		super.scanner(state, getTerminal(state));
	}

	private String getTerminal(String state) {
		
		String terminal = "";
		String tmp[] = changeFieldSeparator(state).split(" ");
		
		for(int aux=0; aux<tmp.length; aux++)
			if(!tmp[aux].equals(ConstantsUtility.DOTTED_RULE) && (tmp[aux].replace("-", "").matches("[A-Z]+.*") || isPontuation(tmp[aux].charAt(0))))
				if(aux>0 && tmp[aux-1].equals(ConstantsUtility.DOTTED_RULE)) {
					terminal = tmp[aux];
					break;
				}
		
		return terminal;
		
	}
	
	private boolean isPontuation(char currentLetter) {
		return currentLetter == ':' || currentLetter == ';' || currentLetter == ',' || currentLetter == '.' || currentLetter == '!' || currentLetter == '?';
	}
	
	private void completer(String state) {
		
		int stateStart = Integer.parseInt(state.split("\\[")[1].split(",")[0]);
		currentPOSTag = nextCompletedCategory(state);
		ArrayList<String> tmp = new ArrayList<String>();
		ArrayList<String> rulesToComplete = new ArrayList<String>();
		
		if(isComplete(state) && hasCompletedSentence(state)) 
			grammarRecognized = true;
		
		int currentPOSTagCount = 1, chartIndex = 0;

		while(chartIndex < chartTerminalsIndex.size()) {

			ArrayList<String> tmpChartOnlyWithTerminals = new ArrayList<String>(chartTerminalsIndex.get(chartIndex));
			while(currentPOSTagCount > 0 && tmpChartOnlyWithTerminals.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag) >= 0) {

				currentPOSTagCount = Collections.frequency(tmpChartOnlyWithTerminals, ConstantsUtility.DOTTED_RULE + " " + currentPOSTag);
				int ruleIndex = tmpChartOnlyWithTerminals.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag);
				rulesToComplete.add(chart.get(chartIndex).get(ruleIndex));
				tmpChartOnlyWithTerminals.set(tmpChartOnlyWithTerminals.indexOf(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag), "");
				
			}
			chartIndex++;
		}
		
		for(int count=0; count<rulesToComplete.size(); count++) {
			
			String rule = rulesToComplete.get(count);
			int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].replace("]", ""));
			
			if(!isComplete(rule) && ruleEnd==stateStart && !rule.contains(DUMMY_STATE)) {
				
				String cleanNonTerminal = rule.substring(rule.indexOf(ConstantsUtility.DOTTED_RULE)+2, rule.indexOf("[")-1).split(" ")[0];
				String tmpRule = rule.substring(0, rule.indexOf('['));
				
				if(currentPOSTag.equals(cleanNonTerminal) && ruleFullyProcessedAndNotInChart(tmp, rule, cleanNonTerminal)) {
					
					previousState = state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
					int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
					tmp.add(tmpRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
					tmpRule = tmpRule.replace(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag, currentPOSTag + " " + ConstantsUtility.DOTTED_RULE)
								.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "S" + ++stateLevelCount + ConstantsUtility.FIELD_SEPARATOR) + 
								"[" + ruleStart + "," + i + "]";
					
					if(enqueue(tmpRule, chart.get(i), i)) {
						printRule(tmpRule, Methods.COMPLETER.name(), String.valueOf(i));
						finalParser.add("tmp: " + tmpRule + ConstantsUtility.FIELD_SEPARATOR + "(" + previousState + ")");
					}
					
					rule = tmpRule;
					
				}
				
			} 
			
			if(isComplete(rule) && hasCompletedSentence(rule)) 
				grammarRecognized = true;
			if(isComplete(rule) && isFinalStateToGrammarTree(rule))
				addToFinalParser(rule, "(" + previousState + ")");
			
		}
		j++;
		
	}
	
}