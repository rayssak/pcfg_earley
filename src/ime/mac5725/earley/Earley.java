package ime.mac5725.earley;

import ime.mac5725.earley.parser.Completer;
import ime.mac5725.earley.parser.Predictor;
import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * @author rayssak
 * @reason Earley algorithm, a top-down search for sentence
 * 		   recognition and also adapted to parse trees.
 */
public class Earley {
	
	protected int i = 0;
	protected int j = 0;
	protected int stateLevelCount = 0;
	protected int sentenceHeadRuleCount = 0;
	protected static volatile int threadCount = 0;
	protected static volatile int threadCompletedCount = 0;
	
	protected boolean printRules;
	protected boolean grammarRecognized = false;
	protected static volatile boolean threadRuleCompleted = false;
	
	protected ArrayList<String> lexicon;
	protected ArrayList<String> grammarIndex;
	protected static volatile ArrayList<String> grammar;
	
	protected LinkedList<String> sentenceWords;
	
	protected static volatile ArrayList<ArrayList<String>> chart;
	protected ArrayList<String> chartWithRules;
	protected ArrayList<String> currentChartWithRules;
	protected ArrayList<ArrayList<String>> chartTerminalsIndex;

	protected static volatile ArrayList<String> rulesToPredict;
	protected static volatile ArrayList<String> rulesToComplete;
	
	protected String state;
	protected String fullState;
	protected String currentPOSTag;
	protected String previousState;
	protected String sentenceHeadRule;
	
	protected static String DUMMY_STATE;
	
	protected enum Methods { PREDICTOR, SCANNER, COMPLETER };
	
	protected ArrayList<String> finalParser;
	
	public void setPrintRules(boolean print) {
		this.printRules = print;
	}
	
	public ArrayList<String> parse() {
		cleanTmp();
		return finalParser;
	}
	
	protected void cleanTmp() {
		for(Iterator it=finalParser.iterator(); it.hasNext(); )
			if(it.next().toString().startsWith("tmp"))
				it.remove();
	}
	
	protected void prepareVariables(LinkedHashSet<String> grammar, LinkedHashSet<String> lexicon) {
		
		state = "";
		currentPOSTag = "";
		sentenceHeadRule = "";
		previousState = "";
		
		this.grammar = new ArrayList<String>();
		this.grammar.addAll(grammar);
		this.lexicon = new ArrayList<String>();
		this.lexicon.addAll(lexicon);
		
		startGrammarIndex();
		
		chart = new ArrayList<ArrayList<String>>();
		finalParser = new ArrayList<String>();
		chartWithRules = new ArrayList<String>();
		rulesToPredict = new ArrayList<String>();
		rulesToComplete = new ArrayList<String>();
		chartTerminalsIndex = new ArrayList<ArrayList<String>>();
		currentChartWithRules = new ArrayList<String>();
		
		DUMMY_STATE = this.getClass().getName().contains("Finger") ? ConstantsUtility.DUMMY_STATE : ConstantsUtility.DUMMY_STATE_JURAFSKY;
		
	}

	private void startGrammarIndex() {
		grammarIndex = new ArrayList<String>();
		for(String rule : grammar)
			grammarIndex.add(getRule(rule));
	}

	protected void prepareSentenceWords(String words) {
		sentenceWords = new LinkedList<String>();
		String tmp[] = words.split(" ");
		for(int aux=0; aux<tmp.length; aux++) {
			sentenceWords.add(tmp[aux]);
			addNewEntryToChart();
		}
		// Chart must have N+1 sets of states (N = sentence words)
		addNewEntryToChart();
	}

	protected void addNewEntryToChart() {
		chart.add(new ArrayList<String>());
		chartTerminalsIndex.add(new ArrayList<String>());
	}
	
	/**
	 * @author rayssak
	 * @reason PREDICTOR task of the Earley algorithm:
	 * 
	 * 		   procedure PREDICTOR((A-> d * B b, [i,j]))
	 * 		   		for each(B-> y) in GRAMMAR-RULES-FOR(B, grammar) do
	 * 					ENQUEUE((B-> y, [i,j]), chart[j])
	 * 		   end
	 * 
	 * @param String state
	 */
	protected void predictor(String state) {
		
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
		
//		// Searches for predictions according to the available grammar
//		// rules.
//		prepareAndStartThreadsToPredict(state);
//		System.gc();
			
		// Insert each grammar rule of the current state being processed
		// into the chart in case the rule is not already in the current
		// chart (not all of them).
		for(String rule : rulesToPredict)
		
			if(!currentChartWithRules.contains(rule) && specialCase(rule))
				
				if(enqueue(addStateAndStartEndPointsFields(rule), chart.get(i), i)) {
					stateLevelCount++;
					printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i));
				
				}
		
		rulesToPredict.clear();
		j++;
		
	}

	/**
	 * @author rayssak
	 * @reason In order to improve PREDICTOR performance, the grammar rules are 
	 * 		   divided in several ones. The COMPLETER task traverses the whole grammar
	 * 		   rules list data, looking for predictions corresponding to the next
	 * 		   category being processed by the current state.
	 * 		   This is also a terrible and costly performance scenario to execute sequentially,
	 * 		   although it is not so bad like COMPLETER.
	 * @param state
	 * @return int size
	 */
	private void prepareAndStartThreadsToPredict(String state) {
		
		// Initialize required variables
		threadCompletedCount = 0;
		threadCount = 0;
		threadRuleCompleted = false;
		int size = grammarIndex.size() / ConstantsUtility.THREAD_NUMBER_PREDICTOR == 0 ? 
				grammarIndex.size() : grammarIndex.size() / ConstantsUtility.THREAD_NUMBER_PREDICTOR;
		
		// Initialize partial grammar to be sent to each
		// individual thread in order to reduce what each one
		// needs to process.
		// Performance improvement.
		ArrayList<String> pC1 = new ArrayList<String>();
		ArrayList<String> pC2 = new ArrayList<String>();
		ArrayList<String> pC3 = new ArrayList<String>();
		
		// If there is not many grammar rules, there is no need
		// to gather several threads.
		if(size <= (ConstantsUtility.THREAD_NUMBER_PREDICTOR-1) && size!=0) {
			
			for (int aux=0; aux<grammarIndex.size(); aux++) 
				pC1.add(grammarIndex.get(aux));
			
			threadCount++;
			new Thread(new Predictor(state, pC1, currentPOSTag, 0)).start();
			
		// Otherwise...
		} else if(size!=0){
			
			// ...set each part of grammar to a different thread.
			for (int aux=0; aux<size; aux++) 
				pC1.add(grammarIndex.get(aux));
			for (int aux=0; aux<size; aux++) 
				pC2.add(grammarIndex.get(size + aux));
			for (int aux=0; aux<size; aux++) 
				pC3.add(grammarIndex.get((2*size) + aux));
			
			// Count control for thread synchronization.
			threadCount += ConstantsUtility.THREAD_NUMBER_PREDICTOR;
			
			// Threads initialization.
			new Thread(new Predictor(state, pC1, currentPOSTag, 0)).start();
			new Thread(new Predictor(state, pC2, currentPOSTag, size)).start();
			new Thread(new Predictor(state, pC3, currentPOSTag, (2*size))).start();
			
		}

		// Wait until all threads completes and all required grammar
		// rules are gathered.
		try {
			if(size>0)
				synchronized (rulesToPredict) {
					if(!threadRuleCompleted)
						rulesToPredict.wait();
				}
		} catch (InterruptedException interruptedException) {
			System.out.println(interruptedException.getMessage());
		}
		
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
	
	protected boolean enqueue(String state, ArrayList<String> chartEntry, int i) {
		if(!chartEntry.contains(state) && !chartWithRules.contains(state.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, ""))) {
			push(state, chartEntry, i);
			return true;
		} else
			return false;
	}

	protected void push(String state, ArrayList<String> chartEntry, int i) {
		chartEntry.add(state);
		chartWithRules.add(state.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, ""));
		currentChartWithRules.add(state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
	}
	
	protected void printHeadRule() {
		if(printRules)
			System.out.println("\tChart[" + i + "]\t\t" + "S" + stateLevelCount + " " + chart.get(i).get(0).replace(ConstantsUtility.FIELD_SEPARATOR, "") + 
							   "\t\t\t\t\t\t\t\t DUMMY START STATE");
	}
	
	protected void printRule(String rule, String method, String chartValue) {
		if(printRules) {
			rule = rule.contains(ConstantsUtility.DOTTED_RULE) ? rule : addStateAndStartEndPointsFields(rule); 
			rule += rule.length()<24 ? "\t\t\t\t\t\t\t" : (rule.length()<32 ? "\t\t\t\t\t\t" : "\t\t\t\t\t");
			System.out.println("\tChart[" + chartValue + "]\t\t" + rule.replace("|", " ") + " " + method);
		}
	}
	
	protected void getNextStateToRun() {
		state = chart.get(i).get(j);
		addStateField(state, chart.get(i));
	}
	
	protected void addStateField(String state, ArrayList<String> chartEntry) {
		if(!state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0].matches("S[0-9]+")) {
			fullState = "S" + stateLevelCount + ConstantsUtility.FIELD_SEPARATOR + state;
			chartEntry.set(chartEntry.indexOf(state), fullState);
		} else
			fullState = state;
	}

	protected boolean isComplete(String state) {
		int dottedRule = state.lastIndexOf(ConstantsUtility.DOTTED_RULE);
		int bracket = state.indexOf('[');
		return bracket-dottedRule == 2;
	}
	
	protected String nextCategory(String state) {
		return state.substring(state.lastIndexOf(ConstantsUtility.DOTTED_RULE)+2).split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0].split(" ")[0];
	}
	
	protected String getRule(String rule) {
		return rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0];
	}

	protected String addStateAndStartEndPointsFields(String rule) {
		return "S" + stateLevelCount + ConstantsUtility.FIELD_SEPARATOR + 
				rule.replace(ConstantsUtility.NEXT_ELEMENT_CHAR, ConstantsUtility.NEXT_ELEMENT_CHAR + " " + ConstantsUtility.DOTTED_RULE) + 
				ConstantsUtility.FIELD_SEPARATOR + "[" + i + "," + i + "]";
	}

	/**
	 * @author rayssak
	 * @reason SCANNER task of the Earley algorithm:
	 * 
	 * 		   procedure SCANNER((A-> d * B b, [i,j]))
	 * 		   		if B is part of PARTS-OF-SPEECH(word[j]) then
	 * 					ENQUEUE((B-> word[j], [j,j+1]), chart[j+1])
	 * 
	 * @param String state
	 * @param String terminal
	 */
	protected void scanner(String state, String terminal) {
		
		ArrayList<String> terminals = new ArrayList<String>();
		
		// Gathers all the possible words for this terminal.
		for(String rule : lexicon) 
			if(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(terminal))
				terminals.add(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR + " ")[1]);
		
		// If there is any word for this terminal...
		if(!terminals.isEmpty())
			
			// ...get each one of them...
			for(String word : sentenceWords) {
				
				for(int aux=0; aux<terminals.size(); aux++)
					// ...and even if the current word is in the input sentence,
					// check if it is the right time to add it (if the process is currently
					// in its word position in chart, the same position of the word in 
					// the input sentence).
					if(word.equals(terminals.get(aux)) && i==sentenceWords.indexOf(word)) {
						
						String rule = getTerminalCompletedRule(terminal, word, sentenceWords.indexOf(word), sentenceWords.indexOf(word)+1);
						if(enqueue(rule, chart.get(sentenceWords.indexOf(word)+1), sentenceWords.indexOf(word)+1)) {
							printRule(rule, Methods.SCANNER.name(), String.valueOf(sentenceWords.indexOf(word)+1));
							sentenceWords.set(sentenceWords.indexOf(word), "");
							addToFinalParser(rule, Methods.SCANNER.name());
							break;
						}
						
					}
			}
		
		j++;
		
	}
	
	protected String changeFieldSeparator(String state) {
		return state.replace(ConstantsUtility.FIELD_SEPARATOR, " ");
	}
	
	protected String getTerminalCompletedRule(String terminal, String word, int start, int end) {
		return "S" + ++stateLevelCount + ConstantsUtility.FIELD_SEPARATOR + terminal + ConstantsUtility.NEXT_ELEMENT_CHAR + " " + 
					word + " " + ConstantsUtility.DOTTED_RULE + ConstantsUtility.FIELD_SEPARATOR + "[" + start + "," + end + "]";
	}
	
	protected String nextCompletedCategory(String state) { //Y-> * IP CP FRAG|[0,0]
		return state.substring(state.indexOf(ConstantsUtility.FIELD_SEPARATOR)+1, state.indexOf(ConstantsUtility.NEXT_ELEMENT_CHAR));
	}
	
	/**
	 * @author rayssak
	 * @reason COMPLETER task of the Earley algorithm:
	 * 
	 * 		   procedure COMPLETER((B-> y *, [j,k]))
	 * 		   		for each(A-> d * B b, [i,j]) in chart[j] do
	 * 					ENQUEUE((A-> d B * b, [i,k]), chart[k])
	 * 		   end
	 * 
	 * @param String state
	 */
	protected void completer(String state) {
		
		int stateStart = Integer.parseInt(state.split("\\[")[1].split(",")[0]);
		currentPOSTag = nextCompletedCategory(state);
		ArrayList<String> tmp = new ArrayList<String>();
		
		// Checks if a final state entry was already processed and the
		// sentence already recognized.
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
		
//		// Searches for states with the category of the current state
//		// to update its progress.
//		prepareAndStartThreadsToComplete(state);
//		System.gc();
		
		// Checks each one of the gathered rules...
		for(int count = 0; count<rulesToComplete.size(); count++) {
			
			String rule = rulesToComplete.get(count);
			int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].replace("]", ""));
			
			// ...and check if there is any of them ending in the state state position to 
			// mark as complete.
			// e.g.: 
			// 		~ Current state: N-> virtudes [1,2]
			// 		~ Current rule: NP-> * N [0,1]
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
			
			// Handles grammar tree rules and end of the sentence
			if(isComplete(rule) && hasCompletedSentence(rule)) 
				grammarRecognized = true;
			if(isComplete(rule) && isFinalStateToGrammarTree(rule))
				addToFinalParser(rule, "(" + previousState + ")");
			
		}
		rulesToComplete.clear();
		j++;
		
	}

	/**
	 * @author rayssak
	 * @reason In order to improve COMPLETER performance, the chart states are 
	 * 		   divided in several ones. The COMPLETER task traverses all the chart,
	 * 		   in all its positions, looking for states that requires progress update.
	 * 		   This is a terrible and costly performance scenario to execute sequentially.
	 * @param state
	 * @return int size
	 */
	private void prepareAndStartThreadsToComplete(String state) {
		
		// Initialize required variables
		int size = chartTerminalsIndex.get(0).size() / ConstantsUtility.THREAD_NUMBER_COMPLETER;
		int chartIndex = 0;
		
		// Unlike the PREDICTOR task, the COMPLETER needs to process
		// all chart positions.
		while(chartIndex < chartTerminalsIndex.size()) {
			
			threadCompletedCount = 0;
			threadCount = 0;
			threadRuleCompleted = false;
			
			// Initialize partial lists of chart to be sent to each
			// individual thread in order to reduce what each one
			// needs to process.
			// Performance improvement.
			ArrayList<String> partialChart1 = new ArrayList<String>();
			ArrayList<String> partialChart2 = new ArrayList<String>();
			ArrayList<String> partialChart3 = new ArrayList<String>();
			ArrayList<String> partialChart4 = new ArrayList<String>();
			ArrayList<String> partialChart5 = new ArrayList<String>();
			size = chartTerminalsIndex.get(chartIndex).size() / ConstantsUtility.THREAD_NUMBER_COMPLETER == 0 ? 
					chartTerminalsIndex.get(chartIndex).size() : chartTerminalsIndex.get(chartIndex).size() / ConstantsUtility.THREAD_NUMBER_COMPLETER;
			
			// If there is not many chart states, there is no need
			// to gather several threads.
			if(size <= (ConstantsUtility.THREAD_NUMBER_COMPLETER-1) && size!=0) {
				
				for (int aux=0; aux<chartTerminalsIndex.get(chartIndex).size(); aux++) 
					partialChart1.add(chartTerminalsIndex.get(chartIndex).get(aux));
				
				threadCount++;
				new Thread(new Completer(chartIndex, state, partialChart1, currentPOSTag, 0)).start();
				
			// Otherwise...
			} else if(size!=0){
				
				// ...set each part of the chart to a different thread.
				for (int aux=0; aux<size; aux++) 
					partialChart1.add(chartTerminalsIndex.get(chartIndex).get(aux));
				for (int aux=0; aux<size; aux++) 
					partialChart2.add(chartTerminalsIndex.get(chartIndex).get(size + aux));
				for (int aux=0; aux<size; aux++) 
					partialChart3.add(chartTerminalsIndex.get(chartIndex).get((2*size) + aux));
				for (int aux=0; aux<size; aux++) 
					partialChart4.add(chartTerminalsIndex.get(chartIndex).get((3*size) + aux));
				for (int aux=0; aux<size; aux++) 
					partialChart5.add(chartTerminalsIndex.get(chartIndex).get((4*size) + aux));
				
				// Count control for thread synchronization.
				threadCount += ConstantsUtility.THREAD_NUMBER_COMPLETER;
				
				// Threads initialization.
				new Thread(new Completer(chartIndex, state, partialChart1, currentPOSTag, 0)).start();
				new Thread(new Completer(chartIndex, state, partialChart2, currentPOSTag, size)).start();
				new Thread(new Completer(chartIndex, state, partialChart3, currentPOSTag, (2*size))).start();
				new Thread(new Completer(chartIndex, state, partialChart4, currentPOSTag, (3*size))).start();
				new Thread(new Completer(chartIndex, state, partialChart5, currentPOSTag, (4*size))).start();

			}

			// Wait until all threads completes and all required chart
			// states are gathered.
			try {
				if(size>0)
					synchronized (rulesToComplete) {
						if(!threadRuleCompleted)
							rulesToComplete.wait();
					}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			chartIndex++;
		
		}
	}
	
	protected boolean ruleFullyProcessedAndNotInChart(ArrayList<String> tmp, String rule, String cleanNonTerminal) {
		return cleanNonTerminal.equals(currentPOSTag) && !tmp.contains(rule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]) &&
			   !rule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(ConstantsUtility.DUMMY_STATE);
	}
	
	// isStartingSentenceRule
	protected boolean hasCompletedSentence(String rule) {
		
		int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
		int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].replace("]", ""));
		String[] tmp = DUMMY_STATE.substring(0, DUMMY_STATE.indexOf(ConstantsUtility.FIELD_SEPARATOR)).split(ConstantsUtility.NEXT_ELEMENT_CHAR)[1].replace(ConstantsUtility.DOTTED_RULE, "").split(" ");
		
		if(ruleStart == 0 && ruleEnd == chart.size()-1)
			for(int aux=0; aux<tmp.length; aux++)
				if(tmp[aux].equals(getRule(rule).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "")))
					return true;
		
		return false;
		
	}

	// !isAtFinalParser
	protected boolean isFinalStateToGrammarTree(String rule){
		for(String current : finalParser) {
			String tmp = current.replace("Chart", "").replaceAll("\\[[0-9]+\\] ", "").split("\\]")[0] + "]";
			if(tmp.equals(rule))
				return false;
		}
		return true;
	}
	
	protected boolean isLastChartItem(int chartCount, String rule) {
		return rule.equals(chart.get(chartCount).get(chart.get(chartCount).size()-1));
	}
	
	protected void addToFinalParser(String rule, String method) {
		
		String states = "";
		String previousRule = "";
		
		for(int aux=finalParser.size()-1; aux>=0; aux--) {
			
			String tmp = finalParser.get(aux);
			String tmpRule = tmp.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1].replace(ConstantsUtility.DOTTED_RULE + " ", "").replace(" " + ConstantsUtility.DOTTED_RULE, "");
			
			if(ruleAlreadyInTree(tmp, rule))
				return;
			
			else if(tmpRule.equals(rule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1].replace(ConstantsUtility.DOTTED_RULE + " ", "").replace(" " + ConstantsUtility.DOTTED_RULE, ""))
					&& !previousRule.equals(tmp.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1])) {
				
				if(tmp.contains("("))
					states += states.isEmpty() ? tmp.split("\\(")[1].replace(")" + ConstantsUtility.FIELD_SEPARATOR, "").replace(")", "") :
								"," + tmp.split("\\(")[1].replace(")" + ConstantsUtility.FIELD_SEPARATOR, "").replace(")", "");
					
				previousRule = tmp.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1];
				
			}
		}
			
		int aux = i == chart.size()-1 ? i : i+1;
		finalParser.add("Chart[" + aux + "] " + rule + (method.matches(".*[A-Z]{2,}.*") ? " " + method : " (" + states + ")"));
		previousState = "";
		
	}
	
	private boolean ruleAlreadyInTree(String tmp, String rule) {
		return tmp.replace("Chart[", "").replaceFirst("[0-9]+", "").replaceFirst("] ", "").split(" \\(")[0].replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(rule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, ""));
	}
	
	protected void resetChartControl() {
		currentChartWithRules = new ArrayList<String>();
	}
	
}