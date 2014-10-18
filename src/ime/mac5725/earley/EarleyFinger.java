package ime.mac5725.earley;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author rayssak
 * @reason Earley algorithm with a few details adapted to
 * 		   the grammar of Finger's corpus. 
 */
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
			tmp = tmp.matches("[A-Za-zÀ-Úà-ú0-9]+\\s\\*") || tmp.matches("[\\:\\;\\.\\,\\?\\!]\\s\\*") ? tmp : tmp.split(" ")[tmp.split(" ").length-2] + " " + tmp.split(" ")[tmp.split(" ").length-1];
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
	
}