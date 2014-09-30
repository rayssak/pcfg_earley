package ime.mac5725.earley.recognizer;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class Earley {
	
	private static String FIELD_SEPARATOR = "|";
	private static String NEXT_ELEMENT_CHAR = "->";
	private static String DUMMY_STATE = "Y" + NEXT_ELEMENT_CHAR + " * S|[0,0]";
	
	private int i = 0;
	private int chartCount = 0;
	private int stateLevelCount = 0;
	
	private LinkedList<String> stack;
	
	private LinkedList<LinkedList<String>> chart;
	private LinkedList<String> sentenceWords;
	private LinkedHashSet<String> grammar;
	
	private String currentPOSTag;
	private String state;
	
	public LinkedList<LinkedList<String>> parse(String words, LinkedHashSet<String> fullGrammar) {
		
		state = "";
		currentPOSTag = "";
		grammar = fullGrammar;
		stack = new LinkedList<String>();
		chart = new LinkedList<LinkedList<String>>();
		prepareSentenceWords(words);

		enqueue(DUMMY_STATE, chart.get(0));
		printHeadRule();
		
		for(; chartCount<sentenceWords.size(); chartCount++) {
			
			while(i<chart.get(chartCount).size() || !stack.isEmpty()) {
				
				getNextStateToRun(i);
			
				if(!isComplete(state) && !isTerminal(nextCategory(state))) 
					predictor(state);
				else if(!isComplete(state) && isTerminal(nextCategory(state)))
					scanner(state);
				else 
					completer(state);
				
			}

		}
		
		return chart;
		
	}

	private String getRules(String current) {
		return current.substring(current.indexOf(NEXT_ELEMENT_CHAR)+5).split("\\" + FIELD_SEPARATOR)[0];
	}

	private void prepareSentenceWords(String words) {
		sentenceWords = new LinkedList<String>();
		String tmp[] = words.split(" ");
		for(int i=0; i<tmp.length; i++) {
			sentenceWords.add(tmp[i]);
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
		System.out.println("Chart[" + chartCount + "]\t\t" + "S" + stateLevelCount + " " + chart.get(chartCount).get(0).replace(FIELD_SEPARATOR, "") + 
						   "\t\t\t\t\tDummy start state");
	}
	
	private void printRule(String rule) {
		System.out.println("Chart[" + chartCount + "]\t\t" + addStateAndStartEndPointsFields(rule).replace("|", " ") + "\t\t\tPredictor");
	}
	
	private void getNextStateToRun(int i) {
		
		if(stack.isEmpty()) {
			state = chart.get(chartCount).get(i);
			addStateField(state, chart.get(chartCount));
		} else
			for(Iterator it = chart.get(chartCount).iterator(); it.hasNext(); ) {
				String current = it.next().toString();
				if(getRules(current).contains(stack.getFirst())) {
					state = current;
					break;
				}
			}
		
	}
	
	private void addStateField(String state, LinkedList<String> chartEntry) {
		if(!state.split("\\" + FIELD_SEPARATOR)[0].matches("S[0-9]+")) {
			chartEntry.set(chartEntry.indexOf(state), "S" + stateLevelCount + FIELD_SEPARATOR + state);
			state = "S" + stateLevelCount + FIELD_SEPARATOR + state;
		}
	}

	private boolean isComplete(String state) {
		int dottedRule = state.lastIndexOf('*');
		int bracket = state.indexOf('[');
		return bracket-dottedRule == 2;
	}
	
	private boolean isTerminal(String nextCategory) {
		return nextCategory.matches(".*[a-z].*");
	}
	
	private boolean isPOS(String posTag) {
		return !posTag.matches(".*[a-z].*");
	}
	
	private String nextCategory(String state) {
		
		if(stack.isEmpty()) {
			
			String[] categories = state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0].split(" ");
			if(categories.length>1)
				for(int i=0; i<categories.length; i++)
					if(!stack.contains(categories[i]))
						stack.add(categories[i]);
			
			return state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0].split(" ")[0];
			
		} else
			return stack.getFirst();
		
	}

	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		boolean hasAnyEntry = false;
		
		for(Iterator it=grammar.iterator(); it.hasNext(); ) {

			String rule = it.next().toString();
			if(getRule(rule).equals(currentPOSTag) && isPOS(rule)) {
				
				i++;
				stateLevelCount++;
				hasAnyEntry = true;
				enqueue(addStateAndStartEndPointsFields(rule), chart.get(chartCount));
				printRule(rule);
				
			}
				
		}
		
		pop(hasAnyEntry);
		
	}

	private String getRule(String rule) {
		return rule.split(NEXT_ELEMENT_CHAR)[0];
	}

	private void pop(boolean hasAnyEntry) {
		
		if(!hasAnyEntry) 
			stack.remove(currentPOSTag);
		else {
			for(int aux=0; aux<stack.size(); aux++) {
				String current = stack.get(aux);
				if(current.startsWith(currentPOSTag)) 
					stack.remove(current);
			}
		}
		
	}

	private String addStateAndStartEndPointsFields(String rule) {
		return "S" + stateLevelCount + FIELD_SEPARATOR + rule.replace(NEXT_ELEMENT_CHAR, NEXT_ELEMENT_CHAR + " * ") + FIELD_SEPARATOR + "[0," + chartCount + "]";
	}

	private void scanner(String state) {
		
	}
	
	private void completer(String state) {
		
	}

}