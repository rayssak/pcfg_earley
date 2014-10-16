package ime.mac5725.earley;

import ime.mac5725.earley.recognizer.Earley;
import ime.mac5725.earley.recognizer.Earley.Methods;
import ime.mac5725.earley.util.ConstantsUtility;

import java.util.ArrayList;
import java.util.LinkedList;

public class Completer implements Runnable {

	private int i;
	private int chartCount;
	private int stateLevelCount;
	
	private boolean grammarRecognized;
	
	private String state;
	private String previousState;
	private String currentPOSTag;
	
	private ArrayList<ArrayList<String>> chart = new ArrayList<ArrayList<String>>();
	
	public Completer(String state, ArrayList<ArrayList<String>> chart, int i, int stateLevelCount, int chartCount, String currentPOSTag) {
		
		this.i = i;
		this.state = state;
		this.previousState = "";
		grammarRecognized = false;
		this.chartCount = chartCount;
		this.chart = chart;
		this.currentPOSTag = currentPOSTag;
		this.stateLevelCount = stateLevelCount;
		
	}
	
	/**
	 * 
	 * 
	    long timeRan = System.currentTimeMillis();
		int chartByThreadSize = chart.get(chartCount).size()/3;
		ArrayList<String> chartFirstThread = new ArrayList<String>();
		ArrayList<String> chartSecondThread = new ArrayList<String>();
		ArrayList<String> chartThirdThread = new ArrayList<String>();
		
		for (int aux=0; aux<chartByThreadSize; aux++) 
			chartFirstThread.add(chart.get(chartCount).get(aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			chartSecondThread.add(chart.get(chartCount).get(chartByThreadSize + aux));
		for (int aux=0; aux<chartByThreadSize; aux++) 
			chartThirdThread.add(chart.get(chartCount).get(chartByThreadSize + chartByThreadSize + aux));
		
		Completer completer = new Completer(state, chart, i, stateLevelCount, chartCount, currentPOSTag);
		Thread firstThread = new Thread(completer);
		firstThread.start();
		Completer completer2 = new Completer(state, chart, i, stateLevelCount, chartCount, currentPOSTag);
		Thread secondThread = new Thread(completer2);
		secondThread.start();
		Completer completer3 = new Completer(state, chart, i, stateLevelCount, chartCount, currentPOSTag);
		Thread thirdThread = new Thread(completer3);
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
	 * 
	 */

	@Override
	public void run() {
		
		int stateStart = Integer.parseInt(state.split("\\[")[1].split(",")[0]);
		LinkedList<String> tmp = new LinkedList<String>();
		
		for(int count=0; count<chart.get(chartCount).size(); count++) {
			
			String rule = chart.get(chartCount).get(count);
			int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].substring(0, 1));
			
			if(!isComplete(rule) /*&& ruleEnd==stateStart*/) {
				
				String cleanNonTerminal = rule.substring(rule.indexOf(ConstantsUtility.DOTTED_RULE)+2, rule.indexOf("[")-1).split(" ")[0];
				String tmpRule = rule.substring(0, rule.indexOf('['));
				
				if(currentPOSTag.equals(cleanNonTerminal) && ruleFullyProcessedAndNotInChart(tmp, rule, cleanNonTerminal)) {
					
					previousState = state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0];
					int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
					tmp.add(tmpRule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]);
					tmpRule = tmpRule.replace(ConstantsUtility.DOTTED_RULE + " " + currentPOSTag, currentPOSTag + " " + ConstantsUtility.DOTTED_RULE)
								.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "S" + ++stateLevelCount + ConstantsUtility.FIELD_SEPARATOR) + 
								"[" + ruleStart + "," + i + "]";
					
					if(enqueue(tmpRule, chart.get(chartCount))) {
						printRule(tmpRule, Methods.COMPLETER.name(), String.valueOf(i));
//						finalParser.add("tmp: " + tmpRule + ConstantsUtility.FIELD_SEPARATOR + "(" + previousState + ")");
					}
					
				}
				
			} 
			
			if(isComplete(rule) && hasCompletedSentence(rule)) 
				grammarRecognized = true;
//			if(isComplete(rule) && isFinalStateToGrammarTree(rule))
//				addToFinalParser(rule, "(" + previousState + ")");
			
			if(isLastChartItem(chartCount, rule) && chartCount < chart.size()-1) {
				count = -1;
				chartCount++;
			}
			
			if(grammarRecognized)
				return;
			
		}
		
	}
	
	protected boolean isComplete(String state) {
		int dottedRule = state.lastIndexOf(ConstantsUtility.DOTTED_RULE);
		int bracket = state.indexOf('[');
		return bracket-dottedRule == 2;
	}
	
	protected boolean ruleFullyProcessedAndNotInChart(LinkedList<String> tmp, String rule, String cleanNonTerminal) {
		return cleanNonTerminal.equals(currentPOSTag) && !tmp.contains(rule.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[1]) &&
			   !rule.replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "").equals(ConstantsUtility.DUMMY_STATE);
	}
	
	protected boolean enqueue(String state, ArrayList<String> chartEntry) {
		if(!chartEntry.contains(state) && !containsClanState(state, chartEntry)) {
			push(state, chartEntry);
			return true;
		} else
			return false;
	}
	
	private boolean containsClanState(String state, ArrayList<String> chartEntry) {
		
		if(state.split(ConstantsUtility.FIELD_SEPARATOR_TO_REPLACE)[0].matches("S[0-9]+")) {

			int chartCount = 0;
			String cleanStateWithPositions = state.substring(state.indexOf(ConstantsUtility.FIELD_SEPARATOR)+1);
			
			for(int aux=0; chartCount < chart.size() && aux<chartEntry.size(); aux++) {
				
				String rule = chartEntry.get(aux).toString();
				if(rule.substring(rule.indexOf(ConstantsUtility.FIELD_SEPARATOR)+1).equals(cleanStateWithPositions))
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
	
	protected String addStateAndStartEndPointsFields(String rule) {
		return "S" + stateLevelCount + ConstantsUtility.FIELD_SEPARATOR + 
				rule.replace(ConstantsUtility.NEXT_ELEMENT_CHAR, ConstantsUtility.NEXT_ELEMENT_CHAR + " " + ConstantsUtility.DOTTED_RULE) + 
				ConstantsUtility.FIELD_SEPARATOR + "[" + i + "," + i + "]";
	}
	
	protected boolean hasCompletedSentence(String rule) {
		
		int ruleStart = Integer.parseInt(rule.split("\\[")[1].split(",")[0]);
		int ruleEnd = Integer.parseInt(rule.split("\\[")[1].split(",")[1].replace("]", ""));
		String[] tmp = Earley.DUMMY_STATE.substring(0, Earley.DUMMY_STATE.indexOf(ConstantsUtility.FIELD_SEPARATOR)).split(ConstantsUtility.NEXT_ELEMENT_CHAR)[1].replace(ConstantsUtility.DOTTED_RULE, "").split(" ");
		
		if(ruleStart == 0 && ruleEnd == chart.size()-1)
			for(int aux=0; aux<tmp.length; aux++)
				if(tmp[aux].equals(getRule(rule).replaceAll(ConstantsUtility.FIELD_SEPARATOR_WITH_STATE_LEVEL, "")))
					return true;
		
		return false;
		
	}

	protected boolean isLastChartItem(int chartCount, String rule) {
		return rule.equals(chart.get(chartCount).get(chart.get(chartCount).size()-1));
	}
	
	protected String getRule(String rule) {
		return rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0];
	}
	
}