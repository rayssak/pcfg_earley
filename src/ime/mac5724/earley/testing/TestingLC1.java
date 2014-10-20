package ime.mac5724.earley.testing;

import ime.mac5725.earley.EarleyFinger;
import ime.mac5725.earley.util.ConstantsUtility;
import ime.mac5725.earley.util.TreeBankHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * @author rayssak
 * @reason Receives a GLC and a portuguese sentence in raw format to 
 * 		   pre-process and generate the formatted Grammar Tree required
 * 		   as the Parser input.
 * 
 * 		   Testing real grammar from Finger's corpus.
 */
public class TestingLC1 {
	
	private static TreeBankHandler glc;
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
		 * 		senhor ofereço a vossa majestade
		 * 		todas são iguais e todas grandes
		 * 		veio até fé
		 * 		declamei contra a vaidade ,
		 * 		as reflexões sobre a vaidade dos homens
		 * 		a confissão da culpa costuma fazer menor a pena .
		 * 		e que só em vossa majestade não tem : feliz indigência
		 *		então sejam bem aceites
		 *		vem por compaixão e lástima
		 *		necessita que primeiro morra o seu autor
		 *		ficam reservadas para serem obras póstumas
		 * 
		 */
		String sentence = "um pequeno livro";
		printRules = Boolean.valueOf(args[1]);
		
		long timeRan = System.currentTimeMillis();
		
		initializeRequiredObjects();
		readGrammar(args);
		grammarRecognized = earley.recognize(sentence, grammarRules, lexicon);
		
		handleTimeRan(timeRan);
		System.out.println("\n- SENTENCE: " + "\"" + sentence + "\"");
		System.out.println("- TIME: " + time);
		System.out.println("- SENTENCE STATUS: " + (grammarRecognized ? "recognized" : "not recognized"));
		System.out.println("- SENTENCE PRECISION: " + (earley.parse(glc.getGrammarTrees()) ? "precise" : "not precise, " + earley.getPrecision() + " % precision " + earley.getOriginalAndParsedTree()));
		
		if(grammarRecognized) {
			Scanner input = new Scanner(System.in);
			System.out.println("Do you want to show grammar whole tree (all backpointers)? (y/N)");
			boolean showBackPointers = input.next().equalsIgnoreCase("y") ? true : false;
			if(showBackPointers) {
				ArrayList<String> tree = earley.getBackPointersTree();
				System.out.println("- SYNTATIC TREE (with backpointers):");
				for(int aux=tree.size()-1; aux>=0; aux--)
						System.out.println("\t" + tree.get(aux).replace(ConstantsUtility.FIELD_SEPARATOR, " "));
				
			}
		}
		
	}

	private static void handleTimeRan(long timeRan) {
		timeRan = System.currentTimeMillis() - timeRan;
		long seconds = (timeRan/1000) % 60;
		long minutes = (timeRan/60000) % 60;
		time = minutes + " minutes, " + seconds + " seconds e " + timeRan + " milliseconds";
	}

//	private static void printRules() {
//
//		PrintWriter out = null;
//		try {
//			out = new PrintWriter("C:\\rayssak\\dev\\ime\\all-sentences.txt");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		for(Entry<String, String> entry : grammarTrees.entrySet()) {
//			out.println(entry.getKey());
//			out.println(entry.getValue());
//		}
//		
////		for(String rule : tmp)
////			out.println(rule);	
////			// Print only head rules
////			if(rule.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0].equals("ADJ")) {
////				count++;
////				System.out.println(rule);
////			}
////		}
//	}

	private static void initializeRequiredObjects() {
		glc = new TreeBankHandler();
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