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
	private LinkedList<String> stackCurrentRuleCategories;
	
	private String currentPOSTag;
	private String state;
	private String fullState;
	
	private enum Methods { PREDICTOR, SCANNER, COMPLETER };
	
	public LinkedList<LinkedList<String>> parse(String words, LinkedHashSet<String> fullGrammar) {
		
		state = "";
		currentPOSTag = "";
		grammar = fullGrammar;
		chart = new LinkedList<LinkedList<String>>();
		stackCurrentRuleCategories = new LinkedList<String>();
		prepareSentenceWords(words);

		enqueue(DUMMY_STATE, chart.get(0));
		printHeadRule();
		
		for(; i<sentenceWords.size(); i++) {
			
			while(j<chart.get(i).size()) {
				
				getNextStateToRun();
			
				// if INCOMPLETE?(state) and NEXT-CAT(state) is not a part of speech
				if(!isComplete(state) && !isTerminal(nextCategory(state)) /*&& !stackCurrentRuleCategories.isEmpty()*/) 
					predictor(state);
				// if INCOMPLETE?(state) and NEXT-CAT(state) is a part of speech
				else if(!isComplete(state) && isTerminal(nextCategory(state)))
					scanner(state);
				else 
					completer(state);
				
			}

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
		System.out.println("Chart[" + i + "]\t\t" + rule.replace("|", " ") + "\t\t\t" + method);
	}
	
	private String getRulesText(String current) {
		return current.substring(current.indexOf(NEXT_ELEMENT_CHAR)+5).split("\\" + FIELD_SEPARATOR)[0];
	}
	
	private void getNextStateToRun() {
		
		if(stackCurrentRuleCategories.isEmpty()) {
			state = chart.get(i).get(j);
			addStateField(state, chart.get(i));
		} else
			for(Iterator it = chart.get(i).iterator(); it.hasNext(); ) {
				String current = it.next().toString();
				if(getRulesText(current).contains(stackCurrentRuleCategories.getFirst())) {
					state = current;
					break;
				}
			}
		
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
	
	private boolean isTerminal(String nextCategory) {
		return nextCategory.matches(".*[a-z].*");
	}
	
	private boolean isPOS(String posTag) {
		return posTag.matches(".*[A-Z]+.*");
	}
	
	private String nextCategory(String state) {
		
		if(stackCurrentRuleCategories.isEmpty()) {
			
			String[] categories = state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0].split(" ");
			if(categories.length>1)
				for(int aux=0; aux<categories.length; aux++)
					if(!stackCurrentRuleCategories.contains(categories[aux]))
						stackCurrentRuleCategories.add(categories[aux]);
			
			return state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0].split(" ")[0];
			
		} else
			return stackCurrentRuleCategories.getFirst();
		
//		return state.substring(state.lastIndexOf(DOTTED_RULE)+2).split("\\" + FIELD_SEPARATOR)[0].split(" ")[0];
		
	}
	
	private void predictor(String state) {
		
		boolean hasAnyEntry = false;
		currentPOSTag = nextCategory(state);
		
		for(Iterator it=grammar.iterator(); it.hasNext(); ) {
			
			String rule = it.next().toString();
			
			if(getRule(rule).equals(currentPOSTag) && isPOS(rule) && !isRuleAlreadyInChart(rule)) {
				
				stateLevelCount++;
				hasAnyEntry = true;
				enqueue(addStateAndStartEndPointsFields(rule), chart.get(i));
				printRule(rule, Methods.PREDICTOR.name());
				
			}
			
		}
		
//		markCategoryCompleted(state);
		pop(hasAnyEntry);
		j++;
		
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
	
	private void pop(boolean hasAnyEntry) {
		
		if(!hasAnyEntry) 
			stackCurrentRuleCategories.remove(currentPOSTag);
		else {
			for(int aux=0; aux<stackCurrentRuleCategories.size(); aux++) {
				String current = stackCurrentRuleCategories.get(aux);
				if(current.startsWith(currentPOSTag)) 
					stackCurrentRuleCategories.remove(current);
			}
		}
		
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
		
	}
	
	private void completer(String state) {
		
		currentPOSTag = nextCompletedCategory(state);
		
		for(int count=0; count<chart.get(i).size(); count++) {
			
			String rule = chart.get(i).get(count);
			if(!chart.get(i).get(count).equals(rule.replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "S" + count + FIELD_SEPARATOR))) {
			
				String cleanRule = rule.split("\\" + FIELD_SEPARATOR)[1];
				String[] posTags = getRulesArray(cleanRule);
				
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
	
	private String[] getRulesArray(String rule) {
		return rule.substring(rule.indexOf(NEXT_ELEMENT_CHAR)).replace(NEXT_ELEMENT_CHAR, "").replace(DOTTED_RULE, "").split(" ");
	}
	
	private boolean equalRules(String state, String rule) {
		return state.replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "").replaceAll("\\s", "").equals(rule.replaceAll("S[0-9]+" + "\\" + FIELD_SEPARATOR, "").replaceAll("\\s", ""));
	}

}