package ime.mac5725.earley;

import ime.mac5725.earley.recognizer.Earley.Methods;
import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;

public class Predictor implements Runnable {
	
	private int i = 0;
	private int stateLevelCount = 0;
	private String currentPOSTag = "";
	private ArrayList<String> grammar = new ArrayList<String>();
	private ArrayList<String> chartEntry = new ArrayList<String>();

	public Predictor(ArrayList<String> chartEntry, int i, int stateLevelCount, ArrayList<String> grammar, String currentPOSTag) {
		this.i = i;
		this.grammar = grammar;
		this.chartEntry = chartEntry;
		this.currentPOSTag = currentPOSTag;
		this.stateLevelCount = stateLevelCount;
	}

	/**
	 * 
	 * 
	    long timeRan = System.currentTimeMillis();
		
		int chartByThreadSize = grammar.size()/3;
		ArrayList<String> grammarFirstThread = new ArrayList<String>();
		ArrayList<String> grammarSecondThread = new ArrayList<String>();
		ArrayList<String> grammarThirdThread = new ArrayList<String>();
		
		for (int aux=0; aux<chartByThreadSize; aux++) 
			grammarFirstThread.add(grammar.get(aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			grammarSecondThread.add(grammar.get(chartByThreadSize + aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			grammarThirdThread.add(grammar.get(chartByThreadSize + chartByThreadSize + aux));
		
		Predictor predictor = new Predictor(chart.get(i), i, stateLevelCount, grammarFirstThread, currentPOSTag);
		Thread firstThread = new Thread(predictor);
		firstThread.start();
		Predictor predictor2 = new Predictor(chart.get(i), i, stateLevelCount, grammarSecondThread, currentPOSTag);
		Thread secondThread = new Thread(predictor2);
		secondThread.start();
		Predictor predictor3 = new Predictor(chart.get(i), i, stateLevelCount, grammarThirdThread, currentPOSTag);
		Thread thirdThread = new Thread(predictor3);
		thirdThread.start();
		
		
		
		while(firstThread.isAlive() || secondThread.isAlive() || thirdThread.isAlive())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		timeRan = System.currentTimeMillis() - timeRan;
		System.out.println(timeRan);
		
	 * 
	 * 
	 */
	
	@Override
	public void run() {
		
		for(int aux=0; aux<grammar.size(); aux++) {
				
			String rule = grammar.get(aux);
			if(getRule(rule).equals(currentPOSTag) && !isRuleAlreadyInCurrentChart(rule) && specialCase(rule)) {
				stateLevelCount++;
				if(enqueue(addStateAndStartEndPointsFields(rule), chartEntry))
					printRule(rule, Methods.PREDICTOR.name(), String.valueOf(i));
			}
				
		}
			
	}
	
	protected String getRule(String rule) {
		return rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0];
	}
	
	protected boolean isRuleAlreadyInCurrentChart(String rule) {
		
		boolean isRuleAlreadyInChart = false;
		rule = addStateAndStartEndPointsFields(rule).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "");
		
		for(int aux=0; aux<chartEntry.size(); aux++) 
			if(chartEntry.get(aux).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(rule)) {
				isRuleAlreadyInChart = true;
				break;
			}
		
		return isRuleAlreadyInChart;
		
	}
	
	private boolean specialCase(String rule) {
		   		// Removing VB rules that are not part of lexicon 
		return !getRule(rule).equals("VB");
			   // Removing the only one WPP lexicon rule
			   // (this is not required since the PREDICTOR
			   //  only run through the GRAMMAR rules list, not the
		       //  FULLGRAMMAR rules that includes LEXICON)
		//		   && (getRule(rule).equals("WPP") && !getTerminal(state).matches("[0-9]+"));
	}
	
	protected String addStateAndStartEndPointsFields(String rule) {
		return "S" + stateLevelCount + ConstantsUtility.FIELD_SEPARATOR + 
				rule.replace(ConstantsUtility.NEXT_ELEMENT_CHAR, ConstantsUtility.NEXT_ELEMENT_CHAR + " " + ConstantsUtility.DOTTED_RULE) + 
				ConstantsUtility.FIELD_SEPARATOR + "[" + i + "," + i + "]";
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
			
			for(int aux=0; chartCount < chartEntry.size() && aux<chartEntry.size(); aux++) {
				
				String rule = chartEntry.get(aux);
				if(rule!=null && rule.substring(rule.indexOf(ConstantsUtility.FIELD_SEPARATOR)+1).equals(cleanStateWithPositions))
					return true;
				
				if(aux == chartEntry.size()-1) {
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
	
	protected void printRule(String rule, String method, String chartValue) {
//		if(printRules) {
			rule = rule.contains(ConstantsUtility.DOTTED_RULE) ? rule : addStateAndStartEndPointsFields(rule); 
			rule += rule.length()<24 ? "\t\t\t\t\t\t\t" : (rule.length()<32 ? "\t\t\t\t\t\t" : "\t\t\t\t\t");
			System.out.println("\tChart[" + chartValue + "]\t\t" + rule.replace("|", " ") + " " + method);
//		}
	}

}