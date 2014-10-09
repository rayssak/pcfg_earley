package ime.mac5725.earley.recognizer;

import ime.mac5725.earley.util.EarleyConstantsUtility;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class Earley {
	
	private int i = 0, j = 0;
	private int stateLevelCount = 0;
	private int sentenceHeadRuleCount = 0;
	
	private boolean grammarRecognized = false;
	
	private LinkedList<LinkedList<String>> chart;
	private LinkedList<String> sentenceWords;
	private LinkedHashSet<String> grammar;
	private LinkedHashSet<String> lexicon;
	
	private String sentenceHeadRule;
	private String currentPOSTag;
	private String state;
	private String previousState;
	private String fullState;
	
	private enum Methods { PREDICTOR, SCANNER, COMPLETER };
	
	private LinkedList<String> finalParser;
	
	public LinkedList<String> parse() {
		return finalParser;
	}
	
	public boolean recognize(String words, LinkedHashSet<String> grammar, LinkedHashSet<String> lexicon) {
		
		prepareVariables(grammar, lexicon);
		prepareSentenceWords(words);

		enqueue(EarleyConstantsUtility.DUMMY_STATE, chart.get(0));
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

	private void prepareVariables(LinkedHashSet<String> grammar, LinkedHashSet<String> lexicon) {
		state = "";
		currentPOSTag = "";
		sentenceHeadRule = "";
		previousState = "";
		this.grammar = grammar;
		this.lexicon = lexicon;
		chart = new LinkedList<LinkedList<String>>();
		finalParser = new LinkedList<String>();
	}

	private void prepareSentenceWords(String words) {
		sentenceWords = new LinkedList<String>();
		String tmp[] = words.split(" ");
		for(int aux=0; aux<tmp.length; aux++) {
			sentenceWords.add(tmp[aux]);
			addNewEntryToChart();
		}
		// Chart must have N+1 sets of states (N = sentence words)
		addNewEntryToChart();
	}

	private void addNewEntryToChart() {
		chart.add(new LinkedList<String>());
	}
	
	private void enqueue(String state, LinkedList<String> chartEntry) {
		if(!chartEntry.contains(state))
			push(state, chartEntry);
	}

	private void push(String state, LinkedList<String> chartEntry) {
		chartEntry.addLast(state);
	}
	
	private void printHeadRule() {
		System.out.println("\tChart[" + i + "]\t\t" + "S" + stateLevelCount + " " + chart.get(i).get(0).replace(EarleyConstantsUtility.FIELD_SEPARATOR, "") + 
						   "\t\t\t\t\t\t\t\t DUMMY START STATE");
	}
	
	private void printRule(String rule, String method, String chartValue) {
		rule = rule.contains(EarleyConstantsUtility.DOTTED_RULE) ? rule : addStateAndStartEndPointsFields(rule); 
		rule += rule.length()<24 ? "\t\t\t\t\t\t\t" : (rule.length()<32 ? "\t\t\t\t\t\t" : "\t\t\t\t\t");
		System.out.println("\tChart[" + chartValue + "]\t\t" + rule.replace("|", " ") + " " + method);
		
	}
	
	private void getNextStateToRun() {
		state = chart.get(i).get(j);
		addStateField(state, chart.get(i));
	}
	
	private void addStateField(String state, LinkedList<String> chartEntry) {
		if(!state.split(EarleyConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0].matches("S[0-9]+")) {
			fullState = "S" + stateLevelCount + EarleyConstantsUtility.FIELD_SEPARATOR + state;
			chartEntry.set(chartEntry.indexOf(state), fullState);
		} else
			fullState = state;
	}

	private boolean isComplete(String state) {
		int dottedRule = state.lastIndexOf(EarleyConstantsUtility.DOTTED_RULE);
		int bracket = state.indexOf('[');
		return bracket-dottedRule == 2;
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
		
		boolean isLexicon = false;
		
		if(!nextCategory.replace("-", "").matches("[A-Z]+") &&
		    nextCategory.replace("-", "").matches("[[A-Z]+[a-z]+]+")) {
			
			for(Iterator it=lexicon.iterator(); it.hasNext(); ) 
				if(it.next().toString().split(EarleyConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(nextCategory))
					isLexicon = true;
			
		}
		
		return isLexicon;
		
	}
	
	private String nextCategory(String state) {
		return state.substring(state.lastIndexOf(EarleyConstantsUtility.DOTTED_RULE)+2).split(EarleyConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0].split(" ")[0];
	}
	
	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		sentenceHeadRule = sentenceHeadRuleCount == 0 ? currentPOSTag : sentenceHeadRule;
		sentenceHeadRuleCount = sentenceHeadRuleCount == 0 ? sentenceHeadRuleCount+1 : sentenceHeadRuleCount;
		
		for(Iterator it=grammar.iterator(); it.hasNext(); ) {
			
			String rule = it.next().toString();
			
			if(getRule(rule).equals(currentPOSTag) && isPOSTag(rule) && 
			  !isRuleAlreadyInCurrentChart(rule) && !chartHasCurretRuleAlreadyCompleted(rule)) {
				
				stateLevelCount++;
				enqueue(addStateAndStartEndPointsFields(rule), chart.get(i));
				printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i));
				
			}
			
		}
		
		j++;
		
	}

	private boolean isPOSTag(String posTag) {
		return posTag.matches(".*[A-Z]+.*");
	}
	
	private boolean isRuleAlreadyInCurrentChart(String rule) {
		
		boolean isRuleAlreadyInChart = false;
		rule = addStateAndStartEndPointsFields(rule).replaceAll(EarleyConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "");
		
		for(Iterator it=chart.get(i).iterator(); it.hasNext(); ) 
			if(it.next().toString().replaceAll(EarleyConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(rule)) {
				isRuleAlreadyInChart = true;
				break;
			}
		
		return isRuleAlreadyInChart;
		
	}
	
	private boolean chartHasCurretRuleAlreadyCompleted(String rule) {

		int chartCount = 0;
		String terminalAfterDottedRule = rule.split(EarleyConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1].split(" ")[0];
		
		for(int count=0; count<chart.get(chartCount).size(); count++) {
			
			String currentTerminals = chart.get(chartCount).get(count).replaceAll(EarleyConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "")
										.split(EarleyConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0]
										.split(EarleyConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE + " ")[1];
			
			if(currentTerminals.contains(terminalAfterDottedRule) && 
			   !currentTerminals.substring(0, currentTerminals.indexOf(terminalAfterDottedRule)).contains(EarleyConstantsUtility.DOTTED_RULE))
					return true;
			
			if(isLastChartItem(chartCount, chart.get(chartCount).get(count)) && chartCount < chart.size()-1) {
				count = -1;
				chartCount++;
			}
			
		}
		
		return false;
		
	}

	private String getRule(String rule) {
		return rule.split(EarleyConstantsUtility.NEXT_ELEMENT_CHAR)[0];
	}

	private String addStateAndStartEndPointsFields(String rule) {
		return "S" + stateLevelCount + EarleyConstantsUtility.FIELD_SEPARATOR + 
				rule.replace(EarleyConstantsUtility.NEXT_ELEMENT_CHAR, EarleyConstantsUtility.NEXT_ELEMENT_CHAR + " " + EarleyConstantsUtility.DOTTED_RULE) + 
				EarleyConstantsUtility.FIELD_SEPARATOR + "[" + i + "," + i + "]";
	}

	private void scanner(String state) {
		
		LinkedList<String> terminals = new LinkedList<String>();
		String terminal = getTerminal(state);
		
		for(Iterator it=lexicon.iterator(); it.hasNext(); ) {
			String rule = it.next().toString();
			if(rule.split(EarleyConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(terminal))
				terminals.add(rule.split(EarleyConstantsUtility.NEXT_ELEMENT_CHAR + " ")[1]);
		}
		
		if(!terminals.isEmpty())
			
			for(Iterator it = sentenceWords.iterator(); it.hasNext(); ) {
				
				String word = it.next().toString();
				for(int aux=0; aux<terminals.size(); aux++)
					if(word.equals(terminals.get(aux)) && i==sentenceWords.indexOf(word)) {
						
						String rule = getTerminalCompletedRule(terminal, word);
						enqueue(rule, chart.get(i+1));
						printRule(rule, Methods.SCANNER.name(), String.valueOf(i+1));
						sentenceWords.set(sentenceWords.indexOf(word), "");
						
						addToFinalParser(rule, Methods.SCANNER.name());
						
					}
			}
		
		j++;
		
	}

	private String getTerminal(String state) {
		
		String terminal = "";
		String tmp[] = changeFieldSeparator(state).split(" ");
		
		for(int aux=0; aux<tmp.length; aux++)
			if(tmp[aux].matches("[A-Z]*[a-z]+")) 
				if(!posAlreadyProcessed(tmp[aux])) {
					terminal = tmp[aux];
					break;
				}
		
		return terminal;
		
	}
	
	private boolean posAlreadyProcessed(String posTag) {
		
		int chartCount = 0;
		
		for(int aux=0; aux<chart.get(chartCount).size()-1; aux++) {
			
			String rule = chart.get(chartCount).get(aux).toString();
			if(rule.replaceAll(EarleyConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").startsWith(posTag)) 
				return true;
			
			if(aux == chart.get(chartCount).size()-1) {
				aux = -1;
				chartCount++;
			}
			
		}
		
		return false;
		
	}

	private String changeFieldSeparator(String state) {
		return state.replace(EarleyConstantsUtility.FIELD_SEPARATOR, " ");
	}
	
	private String getTerminalCompletedRule(String terminal, String word) {
		return "S" + ++stateLevelCount + EarleyConstantsUtility.FIELD_SEPARATOR + terminal + EarleyConstantsUtility.NEXT_ELEMENT_CHAR + " " + 
					word + " " + EarleyConstantsUtility.DOTTED_RULE + EarleyConstantsUtility.FIELD_SEPARATOR + "[" + i + "," + (i+1) + "]";
	}
	
	private void completer(String state) {
		
		int chartCount = 0;
		int stateStart = Integer.parseInt(state.split("\\[")[1].split(",")[0]);
		currentPOSTag = nextCompletedCategory(state);
		LinkedList<String> tmp = new LinkedList<String>();
		
		for(int count=0; count<chart.get(chartCount).size(); count++) {
			
			String rule = chart.get(chartCount).get(count);
			int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].replace("]", ""));
			
			if(!isComplete(rule) && ruleEnd==stateStart) {
				
				String cleanNonTerminal = rule.substring(rule.indexOf(EarleyConstantsUtility.DOTTED_RULE)+2, rule.indexOf("[")-1).split(" ")[0];
				String tmpRule = rule.substring(0, rule.indexOf('['));
				
				if(ruleFullyProcessedAndNotInChart(tmp, rule, cleanNonTerminal)) {
					
					previousState = state.split(EarleyConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
					int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
					tmp.add(tmpRule.split(EarleyConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
					tmpRule = tmpRule.replace(EarleyConstantsUtility.DOTTED_RULE, "")
								.replace(" " + currentPOSTag, currentPOSTag + " " + EarleyConstantsUtility.DOTTED_RULE)
								.replaceAll(EarleyConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "S" + ++stateLevelCount + EarleyConstantsUtility.FIELD_SEPARATOR) + 
								"[" + ruleStart + "," + i + "]";
					
					enqueue(tmpRule, chart.get(i));
					printRule(tmpRule, Methods.COMPLETER.name(), String.valueOf(i));
					
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
			
		}
		
		j++;
		
	}

	private String nextCompletedCategory(String state) {
		return state.substring(state.indexOf(EarleyConstantsUtility.FIELD_SEPARATOR)+1, state.indexOf(EarleyConstantsUtility.NEXT_ELEMENT_CHAR));
	}
	
	private boolean ruleFullyProcessedAndNotInChart(LinkedList<String> tmp,
			String rule, String cleanNonTerminal) {
		return cleanNonTerminal.equals(currentPOSTag) && !tmp.contains(rule.split(EarleyConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]) &&
		   !rule.replaceAll(EarleyConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(EarleyConstantsUtility.DUMMY_STATE);
	}
	
	private boolean sentenceCompleted(String rule) {
		return i == chart.size()-1 && 
				Integer.parseInt(rule.split("\\[")[1].split(",")[1].replace("]", "")) == chart.size()-1;
	}
	
	// isStartingSentenceRule
	private boolean hasCompletedSentence(String rule) {
		return rule.replaceAll(EarleyConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").startsWith(sentenceHeadRule);
	}

	// !isAtFinalParser
	private boolean isFinalStateToGrammarTree(String rule){
		for(Iterator it=finalParser.iterator(); it.hasNext(); ) {
			String tmp = it.next().toString().replace("Chart", "").replaceAll("\\[[0-9]+\\] ", "").split("\\]")[0] + "]";//
			if(tmp.equals(rule))
				return false;
		}
		return true;
	}
	
	private boolean isLastChartItem(int chartCount, String rule) {
		return rule.equals(chart.get(chartCount).getLast());
	}
	
	private void addToFinalParser(String rule, String method) {
		int aux = i == chart.size()-1 ? i : i+1;
		finalParser.add("Chart[" + aux + "] " + rule + " " + method);
		previousState = "";
	}

}