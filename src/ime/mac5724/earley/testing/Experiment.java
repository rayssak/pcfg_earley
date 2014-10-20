package ime.mac5724.earley.testing;

import ime.mac5725.earley.EarleyFinger;
import ime.mac5725.earley.util.TreeBankHandler;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

public class Experiment {
	
	private static TreeBankHandler glc;
	private static EarleyFinger earley;

	private static String time;
	
	private static boolean grammarRecognized;
	private static boolean printRules;
	
	private static LinkedHashSet<String> lexicon;
	private static LinkedHashSet<String> grammarRules;
	private static LinkedHashSet<String> grammarRulesTesting;
	private static HashMap<String, String> grammarTrees;
	private static HashMap<String, String> grammarTreesTesting;
	
	public static void main(String[] args) {
		
		printRules = Boolean.valueOf(args[1]);
		long timeRan = System.currentTimeMillis();
		initializeRequiredObjects();
		readGrammar(args);
		divideTrainningAndTestingDatasets();
			
		for(String sentence : grammarTreesTesting.values()) {
			
			grammarRecognized = earley.recognize(sentence, grammarRulesTesting, lexicon);
			handleTimeRan(timeRan);
			System.out.println("\n- SENTENCE: " + "\"" + sentence + "\"");
			System.out.println("- TIME: " + time);
			System.out.println("- SENTENCE STATUS: " + (grammarRecognized ? "recognized" : "not recognized"));
			System.out.println("- SENTENCE PRECISION: " + (earley.parse(glc.getGrammarTrees()) ? "precise" : "not precise, " + earley.getPrecision() + " % precision " + earley.getOriginalAndParsedTree()));
		
		}
		
	}
	
	private static void initializeRequiredObjects() {
		glc = new TreeBankHandler();
		earley = new EarleyFinger();
		grammarRules = new LinkedHashSet<String>();
		grammarRulesTesting = new LinkedHashSet<String>();
		grammarTreesTesting = new HashMap<String, String>();
		lexicon = new LinkedHashSet<String>();
		earley.setPrintRules(printRules);
	}
	
	private static void readGrammar(String[] args) {
		glc.readGrammar(new File(args[0]));
		grammarRules = glc.getGrammarRules();
		lexicon = glc.getLexicon();
		grammarTrees = glc.getGrammarTrees();
		divideTrainningAndTestingDatasets();
	}
	
	private static void divideTrainningAndTestingDatasets() {
		
		int aux = 1;
		double size = grammarRules.size() * 0.2;
		
		for(Entry<String, String> entry : grammarTrees.entrySet()) {
			grammarTreesTesting.put(entry.getKey(), entry.getValue());
			grammarRulesTesting.add(entry.getKey());
			aux++;
			if(aux>size)
				break;
		}
		
	}

	private static void handleTimeRan(long timeRan) {
		timeRan = System.currentTimeMillis() - timeRan;
		long seconds = (timeRan/1000) % 60;
		long minutes = (timeRan/60000) % 60;
		time = minutes + " minutes, " + seconds + " seconds e " + timeRan + " milliseconds";
	}

}