package ime.mac5725.earley;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class EarleyJurafsky extends Earley {
	
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
				else if(!isComplete(state) /*&& isPartOfSpeech(nextCategory(state))*/)
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
		
		if(!nextCategory.replace("-", "").replace("+", "").matches("[A-Z]+") &&
		    nextCategory.replace("-", "").replace("+", "").matches("[[A-Z]+[a-z]+]+")) {
			
			for(Iterator it=lexicon.iterator(); it.hasNext(); ) 
				if(it.next().toString().split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(nextCategory))
					return true;
			
		}
		
		return false;
		
	}
	
	protected void push(String state, ArrayList<String> chartEntry, int i) {
		
		String currentTerminal = getTerminal(state);
		String stateWithoutRule = state.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1];
		if(currentTerminal.isEmpty())
			chartTerminalsIndex.get(i).add(stateWithoutRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0]);
		else if(stateWithoutRule.indexOf(ConstantsUtility.DOTTED_RULE) > stateWithoutRule.indexOf(currentTerminal))
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
			if(!tmp[aux].equals(ConstantsUtility.DOTTED_RULE) && tmp[aux].replace("-", "").matches("[[A-Z]*[a-z]+]+")) {
				if(aux>0 && tmp[aux-1].equals(ConstantsUtility.DOTTED_RULE)) {
					terminal = tmp[aux];
					break;
				}
			}
		
		return terminal;
		
	}
	
}