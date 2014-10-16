package ime.mac5725.earley.recognizer;

import ime.mac5725.earley.util.ConstantsUtility;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * @author rayssak
 * @reason Testing Jurafsky corpus example for Earley algoritm.
 *
 */
public class TestingJurafskyCorpusExample {
	
	private static LinkedHashSet<String> grammarRules;
	private static LinkedHashSet<String> lexicon;
	private static boolean grammarRecognized;
	private static String time;
	private static EarleyJurafsky earley;
	
	private static boolean printRules = true;

	public static void main(String[] args) {
		
		/* 
		 * - SENTENCE EXAMPLES:
		 * 
		 * 		book
		 * 		book that flight
		 * 		she book that flight
		 * 		she prefer that money
		 * 		include this meal
		 * 		does she prefer
		 * 		Houston book
		 * 		she prefer me
		 * 		does she prefer me
		 * 		does she prefer that book
		 * 		does this flight include me
		 * 		does this flight include Houston
		 * 		does this flight include a meal
		 * 		include to me
		 * 
		 */
		String sentence = "book that flight";
		
		long timeRan = System.currentTimeMillis();
		
		initializeRequiredObjects();
		populateGrammarLists(grammarRules, lexicon);
		grammarRecognized = earley.recognize(sentence, grammarRules, lexicon);
		LinkedList<String> grammarTree = earley.parse();

		handleTimeRan(timeRan);
		System.out.println("\n- SENTENCE: " + "\"" + sentence + "\"");
		System.out.println("- TIME: " + time);
		
		System.out.println("- SENTENCE STATUS: " + (grammarRecognized ? "recognized" : "not recognized"));
		if(grammarRecognized) {
			System.out.println("- SYNTATIC TREE:");
			for(Iterator it = grammarTree.descendingIterator(); it.hasNext(); )
				System.out.println("\t" + it.next().toString().replace(ConstantsUtility.FIELD_SEPARATOR, " "));
			
		}
		
	}

	private static void initializeRequiredObjects() {
		earley = new EarleyJurafsky();
		grammarRules = new LinkedHashSet<String>();
		lexicon = new LinkedHashSet<String>();
		earley.setPrintRules(printRules);
	}

	private static void populateGrammarLists(LinkedHashSet<String> grammarRules, LinkedHashSet<String> lexicon) {
		
		grammarRules.add("S-> NP VP");
		grammarRules.add("S-> Aux NP VP");
		grammarRules.add("S-> VP");
		grammarRules.add("NP-> Pronoun");
		grammarRules.add("NP-> Proper-Noun");
		grammarRules.add("NP-> Det Nominal");
		grammarRules.add("Nominal-> Noun");
		grammarRules.add("Nominal-> Nominal Noun");
		grammarRules.add("Nominal-> Nominal PP");
		grammarRules.add("VP-> Verb");
		grammarRules.add("VP-> Verb NP");
		grammarRules.add("VP-> Verb NP PP");
		grammarRules.add("VP-> Verb PP");
		grammarRules.add("VP-> VP PP");
		grammarRules.add("PP-> Preposition NP");
		lexicon.add("Det-> that");
		lexicon.add("Det-> this");
		lexicon.add("Det-> a");
		lexicon.add("Noun-> book");
		lexicon.add("Noun-> flight");
		lexicon.add("Noun-> meal");
		lexicon.add("Noun-> money");
		lexicon.add("Verb-> book");
		lexicon.add("Verb-> include");
		lexicon.add("Verb-> prefer");
		lexicon.add("Pronoun-> I");
		lexicon.add("Pronoun-> she");
		lexicon.add("Pronoun-> me");
		lexicon.add("Proper-Noun-> Houston");
		lexicon.add("Proper-Noun-> NWA");
		lexicon.add("Aux-> does");
		lexicon.add("Preposition-> from");
		lexicon.add("Preposition-> to");
		lexicon.add("Preposition-> on");
		lexicon.add("Preposition-> near");
		lexicon.add("Preposition-> through");
		
	}
	
	private static void handleTimeRan(long timeRan) {
		timeRan = System.currentTimeMillis() - timeRan;
		long seconds = (timeRan/1000) % 60;
		long minutes = (timeRan/60000) % 60;
		time = minutes + " minutes, " + seconds + " seconds e " + timeRan + " milliseconds";
//		time = String.format("%03d:%04d:%05d", minutes, seconds, timeRan) + " milliseconds";
	}

}