package ime.mac5725.earley.recognizer;

import ime.mac5725.earley.util.ConstantsUtility;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * @author rayssak
 * @reason Testing real grammar from Finger's corpus.
 */
public class TestingLC1 {
	
	private static LinkedHashSet<String> lexicon;
	private static LinkedHashSet<String> fullGrammarRules;
	private static LinkedHashSet<String> grammarRules;
	private static boolean grammarRecognized;
	private static String time;
	private static Earley earley;
	private static ParsedGLC glc;
	
	public static void main(String[] args) {
		
//		String sentence = new File(args[1]);
		String sentence = "include to me";
		long timeRan = System.currentTimeMillis();
		
		initializeRequiredObjects();
		readGrammar(args);
//		printRules(timeRan);
		grammarRecognized = earley.recognize(sentence, grammarRules, lexicon);
		LinkedList<String> grammarTree = earley.getParse();

		timeRan = System.currentTimeMillis() - timeRan;
		time = timeRan > 1000 ? timeRan + " segundos" : timeRan + " millisegundos";
		System.out.println("\n- SENTENCE: " + "\"" + sentence + "\"");
		System.out.println("- TIME: " + time);
		
		System.out.println("- SENTENCE STATUS: " + (grammarRecognized ? "recognized" : "not recognized"));
		if(grammarRecognized) {
			System.out.println("- SYNTATIC TREE:");
			for(Iterator it = grammarTree.descendingIterator(); it.hasNext(); )
				System.out.println("\t" + it.next().toString().replace(ConstantsUtility.FIELD_SEPARATOR, " "));
			
		}
		
	}

	private static void printRules(long timeRan) {
		fullGrammarRules = glc.getFullGrammarRules();
		
		for(Iterator i = fullGrammarRules.iterator(); i.hasNext(); ) {
			String rule = i.next().toString();
			// Print only head rules
//			if(rule.startsWith("IP"))
				System.out.println(rule);
		}
	
		System.out.println("- RULES: " + fullGrammarRules.size() + " (" + grammarRules.size() + " rules and " + lexicon.size() + " lexicons)");
		timeRan = System.currentTimeMillis() - timeRan;
		time = timeRan > 1000 ? timeRan + " segundos." : timeRan + " millisegundos.";
		System.out.println("- TIME: " + time);
		
	}

	private static void initializeRequiredObjects() {
		glc = new ParsedGLC();
		earley = new Earley();
		fullGrammarRules = new LinkedHashSet<String>();
		grammarRules = new LinkedHashSet<String>();
		lexicon = new LinkedHashSet<String>();
	}
	
	private static void readGrammar(String[] args) {
		glc.readGrammar(new File(args[0]));
		grammarRules = glc.getGrammarRules();
		lexicon = glc.getLexicon();
	}
	
}