package ime.mac5725.earley.recognizer;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class Earley {
	
	private static String FIELD_SEPARATOR = "|";
	private static String NEXT_ELEMENT_CHAR = "->";
	private static String DOTTED_RULE = "*";
	private static String DUMMY_STATE = "Y" + NEXT_ELEMENT_CHAR + " " + DOTTED_RULE + " S|[0,0]";
	
	private int i = 0, j = 0;
	private int stateLevelCount = 0;
	
	private LinkedList<LinkedList<String>> chart;
	private LinkedList<String> sentenceWords;
	private LinkedHashSet<String> grammar;
	
	private String currentPOSTag;
	private String state;
	private String fullState;
	
	private enum Methods { PREDICTOR, SCANNER, COMPLETER };
	
	public LinkedList<LinkedList<String>> parse(String words, LinkedHashSet<String> fullGrammar) {
		
		state = "";
		currentPOSTag = "";
		grammar = fullGrammar;
		chart = new LinkedList<LinkedList<String>>();
		prepareSentenceWords(words);

		enqueue(DUMMY_STATE, chart.get(0));
		printHeadRule();
		
		for(; i<sentenceWords.size(); i++) {
			
			while(j<chart.get(i).size()) {
				
				getNextStateToRun();
			
				if(!isComplete(state) && !isPartOfSpeech(nextCategory(state))) 
					predictor(state);
				else if(!isComplete(state) && isPartOfSpeech(nextCategory(state)))
					scanner(state);
				else 
					completer(state);
				
			}
			
			j=0;

		}
		
		return chart;
		
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
		System.out.println("Chart[" + i + "]\t\t" + "S" + stateLevelCount + " " + chart.get(i).get(0).replace(FIELD_SEPARATOR, "") + 
						   "\t\t\t\t\tDUMMY START STATE");
	}
	
	private void printRule(String rule, String method) {
		rule = rule.contains(DOTTED_RULE) ? rule : addStateAndStartEndPointsFields(rule);
		System.out.println(method.equals(Methods.SCANNER.name()) ? "Chart[" + (i+1) + "]\t\t" + rule.replace("|", " ") + "\t\t\t" + method :
						   "Chart[" + i + "]\t\t" + rule.replace("|", " ") + "\t\t\t" + method);
	}
	
	private void getNextStateToRun() {
		state = chart.get(i).get(j);
		addStateField(state, chart.get(i));
	}
	
	private void addStateField(String state, LinkedList<String> chartEntry) {
		if(!state.split("\\" + FIELD_SEPARATOR)[0].matches("S[0-9]+")) {
			fullState = "S" + stateLevelCount + FIELD_SEPARATOR + state;
			chartEntry.set(chartEntry.indexOf(state), fullState);
		} else
			fullState = state;
	}

	private boolean isComplete(String state) {
		int dottedRule = state.lastIndexOf(DOTTED_RULE);
		int bracket = state.indexOf('[');
		return bracket-dottedRule == 2;
	}
	
	private boolean isPartOfSpeech(String nextCategory) {
		return nextCategory.matches(".*[a-z].*");
	}
	
	private String nextCategory(String state) {
		return state.substring(state.lastIndexOf(DOTTED_RULE)+2).split("\\" + FIELD_SEPARATOR)[0].split(" ")[0];
	}
	
	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		
		for(Iterator it=grammar.iterator(); it.hasNext(); ) {
			
			String rule = it.next().toString();
			
			if(getRule(rule).equals(currentPOSTag) && isPOSTag(rule) && !isRuleAlreadyInChart(rule)) {
				
				stateLevelCount++;
				enqueue(addStateAndStartEndPointsFields(rule), chart.get(i));
				printRule(rule, Methods.PREDICTOR.name());
				
			}
			
		}
		
//		markCategoryCompleted(state);
		j++;
		
	}

	private boolean isPOSTag(String posTag) {
		return posTag.matches(".*[A-Z]+.*");
	}
	
	private boolean isRuleAlreadyInChart(String rule) {
		
		boolean isRuleAlreadyInChart = false;
		rule = addStateAndStartEndPointsFields(rule).replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "");
		
		for(Iterator it=chart.get(i).iterator(); it.hasNext(); ) 
			if(it.next().toString().replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "").equals(rule)) {
				isRuleAlreadyInChart = true;
				break;
			}
		
		return isRuleAlreadyInChart;
		
	}

	private void markCategoryCompleted(String state) {
			
		if(currentPOSTag.equals(".")) 
			state = fullState.replace(DOTTED_RULE + " " + currentPOSTag, currentPOSTag + " " + DOTTED_RULE);
		else 
			state = fullState.replace(DOTTED_RULE, "").replace(" " + currentPOSTag, currentPOSTag + " " + DOTTED_RULE);
		
		state = state.replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "S" + ++stateLevelCount + FIELD_SEPARATOR);
		
		enqueue(state, chart.get(i));
		printRule(state, Methods.PREDICTOR.name());
			
		
	}

	private String getRule(String rule) {
		return rule.split(NEXT_ELEMENT_CHAR)[0];
	}

	private String addStateAndStartEndPointsFields(String rule) {
//		return "S" + stateLevelCount + FIELD_SEPARATOR + rule.replace(NEXT_ELEMENT_CHAR, NEXT_ELEMENT_CHAR + " " + DOTTED_RULE + " ") + 
//				FIELD_SEPARATOR + "[0," + i + "]";
		return "S" + stateLevelCount + FIELD_SEPARATOR + rule.replace(NEXT_ELEMENT_CHAR, NEXT_ELEMENT_CHAR + " " + DOTTED_RULE) + 
				FIELD_SEPARATOR + "[0," + i + "]";
	}

	private void scanner(String state) {
		
		LinkedList<String> terminals = new LinkedList<String>();
		String terminal = getTerminal(state);
		
		for(Iterator it=grammar.iterator(); it.hasNext(); ) {
			
			String rule = it.next().toString();
			if(rule.split(NEXT_ELEMENT_CHAR)[0].equals(terminal))
				terminals.add(rule.split(NEXT_ELEMENT_CHAR + " ")[1]);
		}
		
		if(!terminals.isEmpty())
			for(Iterator it = sentenceWords.iterator(); it.hasNext(); ) {
				String word = it.next().toString();
				for(int aux=0; aux<terminals.size(); aux++)
					if(word.equals(terminals.get(aux)) && i==sentenceWords.indexOf(word)) {
						String rule = getTerminalCompletedRule(terminal, word);
						enqueue(rule, chart.get(i+1));
						printRule(rule, Methods.SCANNER.name());
						it.remove();
					}
			}
		
		j++;
		
	}

	private String getTerminal(String state) {
		
		String terminal = "";
		String tmp[] = changeFieldSeparator(state).split(" ");
		
		for(int aux=0; aux<tmp.length; aux++)
			if(tmp[aux].matches("[A-Z]*[a-z]+")) {
				terminal = tmp[aux];
				break;
			}
		
		return terminal;
		
	}
	
	private String changeFieldSeparator(String state) {
		return state.replace(FIELD_SEPARATOR, " ");
	}
	
	private String getTerminalCompletedRule(String terminal, String word) {
		return "S" + ++stateLevelCount + FIELD_SEPARATOR + terminal + NEXT_ELEMENT_CHAR + " " + word + " " + DOTTED_RULE + FIELD_SEPARATOR + "[0," + (i+1) + "]";
	}

	private void completer(String state) {
		
		currentPOSTag = nextCompletedCategory(state);
		
		for(int count=0; count<chart.get(i).size(); count++) {
			
			String rule = chart.get(i).get(count);
			if(!chart.get(i).get(count).equals(rule.replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "S" + count + FIELD_SEPARATOR))) {
			
				String cleanRule = rule.split("\\" + FIELD_SEPARATOR)[1];
				String[] posTags = getRules(cleanRule);
				
				for(int aux=0; aux<posTags.length; aux++) 
					if(!posTags[aux].isEmpty() && posTags[aux].equals(currentPOSTag) && !equalRules(state, rule)) {
						rule = rule.replace(DOTTED_RULE, "").replace(" " + currentPOSTag, currentPOSTag + " " + DOTTED_RULE).replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "S" + ++stateLevelCount + FIELD_SEPARATOR);
						enqueue(rule, chart.get(i));
						printRule(rule, Methods.COMPLETER.name());
					}
				
			} 
		}
		j++;
		
	}

	private String nextCompletedCategory(String state) {
		return state.substring(state.lastIndexOf(DOTTED_RULE)-2).split("\\" + FIELD_SEPARATOR)[0].split(" ")[0];
	}
	
	private String[] getRules(String rule) {
		return rule.substring(rule.indexOf(NEXT_ELEMENT_CHAR)).replace(NEXT_ELEMENT_CHAR, "").replace(DOTTED_RULE, "").split(" ");
	}
	
	private boolean equalRules(String state, String rule) {
		return state.replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "").replaceAll("\\s", "").equals(rule.replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "").replaceAll("\\s", ""));
	}

}