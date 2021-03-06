package ime.mac5725.earley;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.LinkedHashSet;

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
			
			System.out.println("Reading chart[" + i + "]...");
			
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
	
}