package ime.mac5725.earley.recognizer;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class Earley {
	
	protected int i = 0;
	protected int j = 0;
	protected int stateLevelCount = 0;
	protected int sentenceHeadRuleCount = 0;
	
	protected boolean printRules;
	protected boolean grammarRecognized = false;
	
	protected List<ArrayList<String>> chart = Collections.synchronizedList(new ArrayList<ArrayList<String>>());
//	protected ArrayList<ArrayList<String>> chart;
	protected LinkedList<String> sentenceWords;
	protected ArrayList<String> grammar;
	protected ArrayList<String> lexicon;
	
	protected String sentenceHeadRule;
	protected String currentPOSTag;
	protected String state;
	protected String previousState;
	protected String fullState;
	
	protected static String DUMMY_STATE;
	
	public enum Methods { PREDICTOR, SCANNER, COMPLETER };
	
	protected ArrayList<String> finalParser;
	
	public void setPrintRules(boolean print) {
		this.printRules = print;
	}
	
	public ArrayList<String> parse() {
		cleanTmp();
		return finalParser;
	}
	
	protected void cleanTmp() {
		for(int aux=finalParser.size()-1; aux>=0; aux--)
			if(finalParser.get(aux).startsWith("tmp"))
				finalParser.remove(aux);
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
		chart = new ArrayList<ArrayList<String>>();
		finalParser = new ArrayList<String>();
		DUMMY_STATE = this.getClass().getName().contains("Finger") ? ConstantsUtility.DUMMY_STATE : ConstantsUtility.DUMMY_STATE_JURAFSKY;
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
	}
	
	protected boolean enqueue(String state, ArrayList<String> chartEntry) {
		if(!chartEntry.contains(state) && !containsClanState(state)) {
			push(state, chartEntry);
			return true;
		} else
			return false;
	}

	private boolean containsClanState(String state) {
		
		if(state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0].matches("S[0-9]+")) {

			int chartCount = 0;
			String cleanStateWithPositions = state.substring(state.indexOf(ConstantsUtility.FIELD_SEPARATOR)+1);
			
			for(int aux=0; chartCount < chart.size() && aux<chart.get(chartCount).size(); aux++) {
				
				String rule = chart.get(chartCount).get(aux).toString();
				if(rule.substring(rule.indexOf(ConstantsUtility.FIELD_SEPARATOR)+1).equals(cleanStateWithPositions))
					return true;
				
				if(aux == chart.get(chartCount).size()-1) {
					aux = -1;
					chartCount++;
				}
				
			}
		}
		
		return false;
		
	}

	protected void push(String state, ArrayList<String> chartEntry) {
		chartEntry.add(state);
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
	
	protected boolean isRuleAlreadyInCurrentChart(String rule) {
		
		boolean isRuleAlreadyInChart = false;
		rule = addStateAndStartEndPointsFields(rule).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "");
		
		for(int aux=0; aux<chart.get(i).size(); aux++) 
			if(chart.get(i).get(aux).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(rule)) {
				isRuleAlreadyInChart = true;
				break;
			}
		
		return isRuleAlreadyInChart;
		
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
		
		for(int aux=lexicon.size()-1; aux>=0; aux--)
			if(lexicon.get(aux).split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals(terminal))
				terminals.add(lexicon.get(aux).split(ConstantsUtility.NEXT_ELEMENT_CHAR + " ")[1]);
		
		if(!terminals.isEmpty())
			
			for(int count=0; count<sentenceWords.size(); count++) {
				
				String word = sentenceWords.get(count);
				if(i==sentenceWords.indexOf(word)) {
				
					for(int aux=0; aux<terminals.size(); aux++)
						if(word.equals(terminals.get(aux))) {
							
							String rule = getTerminalCompletedRule(terminal, word, sentenceWords.indexOf(word), sentenceWords.indexOf(word)+1);
							if(enqueue(rule, chart.get(sentenceWords.indexOf(word)+1))) {
								printRule(rule, Methods.SCANNER.name(), String.valueOf(sentenceWords.indexOf(word)+1));
								sentenceWords.set(sentenceWords.indexOf(word), "");
								addToFinalParser(rule, Methods.SCANNER.name());
								break;
							}
							
						}
				}
			}
		
		j++;
		
	}
	
	protected boolean posAlreadyProcessed(String posTag) {
		
		int chartCount = 0;
		
		for(int aux=0; aux<chart.get(chartCount).size()-1; aux++) {
//		for(int aux=0; chartCount < chart.size() && aux<chart.get(chartCount).size(); aux++) {
			
			String rule = chart.get(chartCount).get(aux).toString();
			if(rule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").split(ConstantsUtility.NEXT_ELEMENT_CHAR_TO_REPLACE)[0].equals(posTag)) 
				return true;
			
			if(aux == chart.get(chartCount).size()-1) {
				aux = -1;
				chartCount++;
			}
			
		}
		
		return false;
		
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
	
	protected boolean ruleFullyProcessedAndNotInChart(LinkedList<String> tmp, String rule, String cleanNonTerminal) {
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
		for(int aux=0; aux<finalParser.size(); aux++) {
			String tmp = finalParser.get(aux).replace("Chart", "").replaceAll("\\[[0-9]+\\] ", "").split("\\]")[0] + "]";
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
	
	private boolean ruleAlreadyInTree(String tmp, String rule) {//IP-> IP *|[0,3] 
		return tmp.replace("Chart[", "").replaceFirst("[0-9]+", "").replaceFirst("] ", "").split(" \\(")[0].replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(rule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, ""));
	}
	
}