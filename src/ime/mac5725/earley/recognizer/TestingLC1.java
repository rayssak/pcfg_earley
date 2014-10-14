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
	private static EarleyFinger earley;
	private static ParsedGLC glc;
	private static boolean printRules;
	
	public static void main(String[] args) {
		
		/* 
		 * - SENTENCE EXAMPLES:
		 * 
		 * 		senhor
		 * 		vaidade
		 * 		ofereço
		 * 		vaidade homens
		 * 		vaidade ofereço
		 * 		vaidade ofereço
		 * 		pequeno livro
		 * 		senhor ofereço a vossa majestade
		 * 		todas são iguais e todas grandes
		 * 		declamei virtudes
		 * 		veio até fé
		 * 
		 */
		String sentence = "senhor ofereço a vossa majestade";
//		String sentence = new File(args[2]);
		printRules = Boolean.valueOf(args[1]);
		
		long timeRan = System.currentTimeMillis();
		
		initializeRequiredObjects();
		readGrammar(args);
		printRules();
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

	private static void handleTimeRan(long timeRan) {
		timeRan = System.currentTimeMillis() - timeRan;
		long seconds = (timeRan/1000) % 60;
		long minutes = (timeRan/60000) % 60;
		time = minutes + " minutes, " + seconds + " seconds e " + timeRan + " milliseconds";
	}

	private static void printRules() {
		int count=0;
		for(Iterator i = lexicon.iterator(); i.hasNext(); ) {
			String rule = i.next().toString();
			// Print only head rules
			if(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals("P")) {
				count++;
				System.out.println(rule);
			}
		}
	}

	private static void initializeRequiredObjects() {
		glc = new ParsedGLC();
		earley = new EarleyFinger();
		fullGrammarRules = new LinkedHashSet<String>();
		grammarRules = new LinkedHashSet<String>();
		lexicon = new LinkedHashSet<String>();
		earley.setPrintRules(printRules);
	}
	
	private static void readGrammar(String[] args) {
		glc.readGrammar(new File(args[0]));
		grammarRules = glc.getGrammarRules();
		lexicon = glc.getLexicon();
	}
	
}