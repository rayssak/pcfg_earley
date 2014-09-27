package ime.mac5725.earley.recognizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

public class Earley {
	
	private static String FIELD_SEPARATOR = "|";
	private static String NEXT_ELEMENT_CHAR = "->";
	private static String DUMMY_STATE = "Y" + NEXT_ELEMENT_CHAR + " * S|[0,0]";
	
	private int chartCount = 0;
	private int stateLevelCount = 0;
	
	private ArrayList<String> nextPOSTags;
	
	private LinkedList<LinkedList<String>> chart;
	private LinkedList<String> sentenceWords;
	private LinkedHashSet<String> grammar;
	
	private String currentPOSTag;
	
	public LinkedList<LinkedList<String>> parse(String words, LinkedHashSet<String> fullGrammar) {
		
		currentPOSTag = "";
		grammar = fullGrammar;
		nextPOSTags = new ArrayList<String>();
		chart = new LinkedList<LinkedList<String>>();
		prepareSentenceWords(words);

		enqueue(DUMMY_STATE, chart.get(0));
		printHeadRule();
		
		for(; chartCount<sentenceWords.size(); chartCount++) {
			
			for(int i=0; i<chart.get(chartCount).size(); i++) {
				
				String state = chart.get(chartCount).get(i);
				addStateField(state, chart.get(chartCount));;
				
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

	private void printHeadRule() {
		System.out.println("Chart[" + chartCount + "]\t" + "S" + stateLevelCount + " " + chart.get(chartCount).get(0).replace(FIELD_SEPARATOR, "") + 
						   "\tDummy start state");
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
		String[] categories = state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0].split(" ");
		if(categories.length>1 && !nextPOSTags.contains(state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0]))
			nextPOSTags.add(state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0]);
		return state.substring(state.lastIndexOf('*')+2).split("\\" + FIELD_SEPARATOR)[0].split(" ")[0];
	}

	private void predictor(String state) {
		
		currentPOSTag = nextCategory(state);
		
		for(Iterator i=grammar.iterator(); i.hasNext(); ) {

			String rule = i.next().toString();
			if(rule.startsWith(currentPOSTag) && isPOS(rule)) {
				
				stateLevelCount++;
				enqueue(addStateAndStartEndPointsFields(rule), chart.get(chartCount));
				System.out.println("Chart[" + chartCount + "]\t" + addStateAndStartEndPointsFields(rule).replace("|", " ") + "\tPredictor");
				
				// Se eu fizer isso, só vou pegar 1 regra de cada POS!
				// Peguei  IP-> * . VB NP .
				// se remover IP
				// não pego nenhum outro IP -> !!!!!!!!!
				for(int aux=0; aux<nextPOSTags.size(); aux++) {
					String current = nextPOSTags.get(aux);
					if(current.startsWith(currentPOSTag)) {
						nextPOSTags.remove(aux);
						nextPOSTags.add(current.replace(currentPOSTag, ""));
					}
				}
				
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