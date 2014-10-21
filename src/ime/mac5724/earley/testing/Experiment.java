package ime.mac5724.earley.testing;

import ime.mac5725.earley.EarleyFinger;
import ime.mac5725.earley.util.TreeBankHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Random;

public class Experiment {
	
	private static TreeBankHandler treeBank;

	private static String time;
	
	private static boolean grammarRecognized;
	private static boolean printRules;
	
	private static LinkedHashSet<String> lexicon;
	private static LinkedHashSet<String> grammarRules;
	private static LinkedHashSet<String> grammarRulesTesting;
	private static HashMap<String, ArrayList<String>> grammarTrees;
	private static HashMap<String, ArrayList<String>> grammarTreesTesting;
	
	public static void main(String[] args) {
		
		printRules = Boolean.valueOf(args[1]);
		long timeRan;
		initializeRequiredObjects();
		readGrammar(args);
		divideTrainningAndTestingDatasets();
		
		for (Entry<String, ArrayList<String>> entry : grammarTreesTesting.entrySet()) {
			
		    String key = entry.getKey();
		    String sentence = entry.getValue().get(0);
	    
			EarleyFinger earley = new EarleyFinger();
			earley.setPrintRules(printRules);
			grammarRecognized = false;
			
			sentence = sentence.split(" \\,")[0];
			timeRan = System.currentTimeMillis();
			System.out.println("\nStarting to recognize and parse: " + "\"" + sentence + "\"...");
			grammarRecognized = earley.recognize(sentence, grammarRulesTesting, lexicon);
			handleTimeRan(timeRan);
			
			System.out.println("- TIME: " + time);
			System.out.println("- SENTENCE STATUS: " + (grammarRecognized ? "recognized" : "not recognized"));
			System.out.println("- SENTENCE PRECISION: " + (earley.parse(treeBank.getGrammarTrees()) ? "precise" : "not precise, " + earley.getPrecision() + " % precision "));
			
		}
		
	}
	
	private static void initializeRequiredObjects() {
		treeBank = new TreeBankHandler();
		grammarRules = new LinkedHashSet<String>();
		grammarRulesTesting = new LinkedHashSet<String>();
		lexicon = new LinkedHashSet<String>();
		grammarTreesTesting = new HashMap<String, ArrayList<String>>();
	}
	
	private static void readGrammar(String[] args) {
		treeBank.readGrammar(new File(args[0]));
		lexicon = treeBank.getLexicon();
		grammarTrees = treeBank.getGrammarTrees();
		grammarRules = treeBank.getGrammarRules();
		divideTrainningAndTestingDatasets();
	}
	
	private static void divideTrainningAndTestingDatasets() {
		
		Random randomGenerator = new Random();
		double size = grammarRules.size() * 0.2;
		int random = randomGenerator.nextInt(100);
		size = size+random;
		
		ArrayList<String> keys = new ArrayList<String>(grammarTrees.keySet());
		for(int aux=random; aux<size; aux++) 
			grammarTreesTesting.put(keys.get(aux), grammarTrees.get(keys.get(aux)));
		
		random = randomGenerator.nextInt(100);
		size = grammarRules.size() * 0.8;
		size = size+random;

		ArrayList<String> rules = new ArrayList<String>(grammarRules);
		for(int aux=random; aux<size; aux++) {
			grammarRulesTesting.add(rules.get(aux));
		}
		
	}

	private static void handleTimeRan(long timeRan) {
		timeRan = System.currentTimeMillis() - timeRan;
		long seconds = (timeRan/1000) % 60;
		long minutes = (timeRan/60000) % 60;
		time = minutes + " minutes, " + seconds + " seconds e " + timeRan + " milliseconds";
	}

}