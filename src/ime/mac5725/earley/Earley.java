package ime.mac5725.earley;

import ime.mac5725.earley.util.ConstantsUtility;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author rayssak
 * @reason Earley algorithm, a top-down search for sentence
 * 		   recognition and also adapted to parse trees.
 */
public class Earley {
	
	protected static volatile PrintWriter out;
	
	protected static volatile int i;
	protected static volatile int j;
	protected static volatile int stateLevelCount;
	protected static volatile int sentenceHeadRuleCount;
	protected static volatile int threadCount;
	protected static volatile int threadCompletedCount;
	
	protected static double precision;
	
	protected static volatile boolean printRules;
	protected static volatile boolean grammarRecognized;
	protected static volatile boolean threadRuleCompleted;
	
	protected static volatile ArrayList<String> lexicon;
	protected static volatile ArrayList<String> grammarIndex;
	protected static volatile ArrayList<String> grammar;
	
	protected LinkedList<String> sentenceWords;
	
	protected static volatile ArrayList<ArrayList<String>> chart;
	protected static volatile ArrayList<String> chartWithRules;
	protected static volatile ArrayList<String> currentChartWithRules;
	protected static volatile ArrayList<ArrayList<String>> chartTerminalsIndex;

	protected static volatile ArrayList<String> rulesToPredict;
	protected static volatile ArrayList<String> rulesToComplete;
	
	protected String sentence;
	protected String state;
	protected String fullState;
	protected static volatile String currentPOSTag;
	protected String previousState;
	protected static volatile String sentenceHeadRule;
	protected static volatile String finalState;
	
	protected static String DUMMY_STATE;
	
	public enum Methods { PREDICTOR, SCANNER, COMPLETER };
	
	protected static volatile ArrayList<String> finalParser;
	
	public void setPrintRules(boolean print) {
		this.printRules = print;
	}
	
	protected void prepareVariables(LinkedHashSet<String> grammar, LinkedHashSet<String> lexicon) {
		
		i = 0;
		j = 0;
		stateLevelCount = 0;
		sentenceHeadRuleCount = 0;
		threadCount = 0;
		threadCompletedCount = 0;
		precision = 0d;
		
		sentence = "";
		state = "";
		fullState = "";
		currentPOSTag = "";
		sentenceHeadRule = "";
		previousState = "";
		finalState = "";
		
		grammarRecognized = false;
		threadRuleCompleted = false;
		
		this.grammar = new ArrayList<String>();
		this.grammar.addAll(grammar);
		this.lexicon = new ArrayList<String>();
		this.lexicon.addAll(lexicon);
		Collections.sort(this.lexicon);
		
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
		sentence = words;
		sentenceWords = new LinkedList<String>();
		String tmp[] = words.split(" ");
		for(int aux=0; aux<tmp.length; aux++) {
			sentenceWords.add(tmp[aux]);
			addNewEntryToChart();
		}
		// Chart must have N+1 sets of states (N = sentence words)
		addNewEntryToChart();
		checkPossibleTerminalsToPredict();
	}
	
	private void checkPossibleTerminalsToPredict() {
		for(String rule : lexicon) {
			for(int aux=0; aux<sentenceWords.size(); aux++) {
				if(rule.contains(sentenceWords.get(aux)))
					if(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1].equals(sentenceWords.get(aux)))
						rulesToPredict.add(rule);
			}
		}
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
		
		// Searches for predictions according to the available grammar
		// rules.
		prepareAndStartThreadsToPredict(state);
		System.gc();
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
	protected boolean specialCase(String rule) {
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
			
		String currentTerminal = getTerminal(state);
		String stateWithoutRule = state.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1];
		ArrayList<String> ruleAndNextTerminal = new ArrayList<String>(Arrays.asList(stateWithoutRule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE, " ").split(" ")));
		
		if(Collections.frequency(ruleAndNextTerminal, currentTerminal) > 1) {
			stateWithoutRule = "";
			List<String> tmp = ruleAndNextTerminal.subList(ruleAndNextTerminal.indexOf(ConstantsUtility.DOTTED_RULE), ruleAndNextTerminal.size());
			for(String element : tmp)
				stateWithoutRule += element + " ";
		}
		
		synchronized (chartTerminalsIndex) {
			if(currentTerminal.isEmpty()) {
				String tmp = stateWithoutRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
				tmp = tmp.matches("[A-Za-z�-��-�0-9]+\\s\\*") || tmp.matches("[\\:\\;\\.\\,\\?\\!]\\s\\*") ? tmp : tmp.split(" ")[tmp.split(" ").length-2] + " " + tmp.split(" ")[tmp.split(" ").length-1];
				chartTerminalsIndex.get(i).add(tmp);
			} else if(stateWithoutRule.indexOf(ConstantsUtility.DOTTED_RULE) > stateWithoutRule.indexOf(currentTerminal))
				chartTerminalsIndex.get(i).add(currentTerminal + " " + ConstantsUtility.DOTTED_RULE);
			else
				chartTerminalsIndex.get(i).add(ConstantsUtility.DOTTED_RULE + " " + currentTerminal);
		}
		
		synchronized (chartEntry) {
			chartEntry.add(state);
			chartWithRules.add(state.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, ""));
			currentChartWithRules.add(state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
		}
			
		
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
	
	protected void printHeadRule() {
		if(printRules)
			System.out.println("\tChart[" + i + "]\t\t" + "S" + stateLevelCount + " " + DUMMY_STATE.replace(ConstantsUtility.FIELD_SEPARATOR, "") + 
							   "\t\t\t\t\t\t\t\t DUMMY START STATE");
	}
	
	protected void printRule(String rule, String method, String chartValue, String state) {
		if(printRules) {
			rule = rule.contains(ConstantsUtility.DOTTED_RULE) ? rule : addStateAndStartEndPointsFields(rule); 
			rule += rule.length()<24 ? "\t\t\t\t\t\t\t" : (rule.length()<32 ? "\t\t\t\t\t\t" : "\t\t\t\t\t");
			System.out.println("\tChart[" + chartValue + "]\t\t" + rule.replace("|", " ") + " " + method);
		}
	}
	
	protected void getNextStateToRun() {
		synchronized (chart) {
			state = chart.get(i).get(j);
			addStateField(state, chart.get(i));
		}
	}
	
	protected synchronized void addStateField(String state, ArrayList<String> chartEntry) {
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
	protected void scanner(String state) {
		
		String terminal = getTerminal(state);
		ArrayList<String> terminals = new ArrayList<String>();
		
		// Gathers all the possible words for this terminal.
		for(String rule : lexicon) 
			if(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(terminal) &&
			  !rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE)[1].matches("\\s+"))
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
							printRule(rule, Methods.SCANNER.name(), String.valueOf(sentenceWords.indexOf(word)+1), state);
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
		
		// Searches for states with the category of the current state
		// to update its progress.
		prepareAndStartThreadsToComplete(state);
		System.gc();
		
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
				new Thread(new Completer(chartIndex, state, partialChart1, 0)).start();
				
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
				new Thread(new Completer(chartIndex, state, partialChart1, 0)).start();
				new Thread(new Completer(chartIndex, state, partialChart2, size)).start();
				new Thread(new Completer(chartIndex, state, partialChart3, (2*size))).start();
				new Thread(new Completer(chartIndex, state, partialChart4, (3*size))).start();
				new Thread(new Completer(chartIndex, state, partialChart5, (4*size))).start();

			}

			// Wait until all threads completes and all required chart
			// states are gathered.
			try {
				if(size>0)
					synchronized (rulesToComplete) {
						if(grammarRecognized)
							break;
						if(!threadRuleCompleted)
							rulesToComplete.wait();
					}
			} catch (InterruptedException interruptedException) {
				System.out.println(interruptedException.getMessage());
			}
			
			chartIndex++;
		
		}
		j++;
		
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
//			for(int aux=0; aux<tmp.length; aux++)
//				if(tmp[aux].equals(getRule(rule).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "")))
					return true;
		
		return false;
		
	}

	// !isAtFinalParser
	protected boolean isFinalStateToGrammarTree(String rule){
		synchronized (finalParser) {
			for(String current : finalParser) {
				String tmp = current.replace("Chart", "").replaceAll("\\[[0-9]+\\] ", "").split("\\]")[0] + "]";
				if(tmp.equals(rule))
					return false;
			}
		}
		return true;
	}
	
	protected boolean isLastChartItem(int chartCount, String rule) {
		return rule.equals(chart.get(chartCount).get(chart.get(chartCount).size()-1));
	}
	
	protected void addToFinalParser(String rule, String method) {
		
		synchronized (finalParser) {
			
			String states = "";
			String previousRule = "";
			
			if(finalParser.size() > 0) {
			
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
						
				previousState = "";
				int aux = i == chart.size()-1 ? i : i+1;
				finalParser.add("Chart[" + aux + "] " + rule + (method.matches(".*[A-Z]{2,}.*") ? " " + method : " (" + states + ")"));
			
			}
		}
		
	}
	
	private boolean ruleAlreadyInTree(String tmp, String rule) {
		return tmp.replace("Chart[", "").replaceFirst("[0-9]+", "").replaceFirst("] ", "").split(" \\(")[0].replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(rule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, ""));
	}
	
	protected void resetChartControl() {
		currentChartWithRules = new ArrayList<String>();
	}
	
	/**
	 * @author rayssak
	 * @reason Check wheter the parsed tree is exactly the same original
	 * 		   one from the grammar tree.
	 * 		   The precision will be measured by number of sentences with
	 * 		   same tree by the number of recognized sentences.
	 * 		   Also, precision measure will be detailed in percentage of
	 * 		   correctness ("out of all brackets found by the parser, how 
	 * 		   many are also present in the gold standard?" 
	 * 							[Stymne, Sara. 2013. Uppsala Universitet]).
	 * 
	 * @param grammarTrees
	 * @return
	 */
	public boolean parse(HashMap<String,ArrayList<String>> grammarTrees) {
		
		if(!finalState.isEmpty()) {
			String finalParserState = finalState.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "")
												.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0]
												.replace(" " + ConstantsUtility.DOTTED_RULE, "");

			for(ArrayList<String> values : grammarTrees.values()) {
				for(Entry<String, ArrayList<String>> rule : grammarTrees.entrySet()) {
				
					if(!values.contains(sentence) && rule.getKey().equals(finalParserState)) {
						
						String tmpParserState[] = finalParserState.split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1].split(" ");

						int count = 0;
						for(int aux=0; aux<tmpParserState.length; aux++)
							if(rule.getKey().contains(tmpParserState[aux]))
								count++;
						
						precision = (count*100)/tmpParserState.length;
						return true;
								
					}
				}
			}
				
		}
		
		return false;
		
	}
	
	public ArrayList<String> getBackPointersTree() {
		for(Iterator it=finalParser.iterator(); it.hasNext(); )
			if(it.next().toString().startsWith("tmp"))
				it.remove();
		return finalParser;
	}
	
	public double getPrecision() {
		return precision;
	}
	
}