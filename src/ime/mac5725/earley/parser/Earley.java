package ime.mac5725.earley.parser;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class Earley {
	
	protected int i = 0;
	protected int j = 0;
	protected int stateLevelCount = 0;
	protected int sentenceHeadRuleCount = 0;
	
	protected boolean printRules;
	protected boolean grammarRecognized = false;
	
	protected ArrayList<ArrayList<String>> chart;
	protected ArrayList<String> chartWithRules;
	protected ArrayList<String> currentChartWithRules;
	protected ArrayList<ArrayList<String>> chartTerminalsIndex;
	protected LinkedList<String> sentenceWords;
	protected ArrayList<String> grammar;
	protected ArrayList<String> grammarIndex;
	protected ArrayList<String> lexicon;
	
	protected String sentenceHeadRule;
	protected String currentPOSTag;
	protected String state;
	protected String previousState;
	protected String fullState;
	
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

	protected void scanner(String state, String terminal) {
		
		ArrayList<String> terminals = new ArrayList<String>();
		
		for(String rule : lexicon) 
			if(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(terminal))
				terminals.add(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR + " ")[1]);
		
		if(!terminals.isEmpty())
			
			for(String word : sentenceWords) {
				
				for(int aux=0; aux<terminals.size(); aux++)
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
	
	protected String nextCompletedCategory(String state) {
		return state.substring(state.indexOf(ConstantsUtility.FIELD_SEPARATOR)+1, state.indexOf(ConstantsUtility.NEXT_ELEMENT_CHAR));
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