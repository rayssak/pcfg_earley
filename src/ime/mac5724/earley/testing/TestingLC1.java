package ime.mac5724.earley.testing;

import ime.mac5725.earley.EarleyFinger;
import ime.mac5725.earley.util.ConstantsUtility;
import ime.mac5725.earley.util.ParsedGLC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * @author rayssak
 * @reason Receives a GLC and a portuguese sentence in raw format to 
 * 		   pre-process and generate the formatted Grammar Tree required
 * 		   as the Parser input.
 * 
 * 		   Testing real grammar from Finger's corpus.
 */
public class TestingLC1 {
	
	private static ParsedGLC glc;
	private static EarleyFinger earley;

	private static String time;
	
	private static boolean grammarRecognized;
	private static boolean printRules;
	
	private static LinkedHashSet<String> lexicon;
	private static LinkedHashSet<String> fullGrammarRules;
	private static LinkedHashSet<String> grammarRules;
	private static HashMap<String, String> grammarTrees;
	
	public static void main(String[] args) {
		
		/* 
		 * - SENTENCE EXAMPLES:
		 * 
		 * 		pequeno livro
		 * 		senhor ofereço a vossa majestade
		 * 		todas são iguais e todas grandes
		 * 		declamei virtudes
		 * 		veio até fé
		 * 		senhor : ofereço a vossa majestade as reflexões sobre a vaidade dos homens ;
		 * 		declamei contra a vaidade ,
		 * 		a confissão da culpa costuma fazer menor a pena .
		 * 		e que só em vossa majestade não tem : feliz indigência
		 *		então sejam bem aceites
		 *		mas vem por compaixão e lástima
		 *		necessita que primeiro morra o seu autor
		 *		ficam reservadas para serem obras póstumas
		 * 
		 */
		String sentence = "então sejam bem aceites";
//		String sentence = new File(args[2]);
		printRules = Boolean.valueOf(args[1]);
		
		long timeRan = System.currentTimeMillis();
		
		initializeRequiredObjects();
		readGrammar(args);
//		printRules();
		grammarRecognized = earley.recognize(sentence, grammarRules, lexicon);
		ArrayList<String> grammarTree = earley.parse();
		
		handleTimeRan(timeRan);
		System.out.println("\n- SENTENCE: " + "\"" + sentence + "\"");
		System.out.println("- TIME: " + time);
		System.out.println("- SENTENCE STATUS: " + (grammarRecognized ? "recognized" : "not recognized"));
		
//		if(grammarRecognized) {
//			System.out.println("- SYNTATIC TREE (the whole one):");
//			for(int aux=grammarTree.size()-1; aux>=0; aux--)
//					System.out.println("\t" + grammarTree.get(aux).replace(ConstantsUtility.FIELD_SEPARATOR, " "));
//			
//		}
		
	}

	private static void handleTimeRan(long timeRan) {
		timeRan = System.currentTimeMillis() - timeRan;
		long seconds = (timeRan/1000) % 60;
		long minutes = (timeRan/60000) % 60;
		time = minutes + " minutes, " + seconds + " seconds e " + timeRan + " milliseconds";
	}

	private static void printRules() {
		ArrayList<String> tmp = new ArrayList<String>();
		tmp.addAll(grammarRules);
		Collections.sort(tmp);
		
		int count=0;
		PrintWriter out = null;
		try {
			out = new PrintWriter("C:\\rayssak\\dev\\ime\\all-rules_sorted.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(String rule : tmp)
			out.println(rule);	
			// Print only head rules
//			if(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals("ADJ")) {
//				count++;
//				System.out.println(rule);
//			}
//		}
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
		grammarTrees = glc.getGrammarTrees();
	}
	
}