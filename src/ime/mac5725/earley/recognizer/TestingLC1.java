package ime.mac5725.earley.recognizer;

import java.io.File;
import java.util.LinkedHashSet;

public class TestingLC1 {
	
	public static void main(String[] args) {	
		
////		long timeRan = System.currentTimeMillis();
////		
////		ParsedGLC glc = new ParsedGLC();
////		glc.readGrammar(new File(args[0]));
////		timeRan = System.currentTimeMillis() - timeRan;
//		
//		LinkedHashSet<String> fullGrammarRules = glc.getFullGrammarRules();
//		LinkedHashSet<String> grammarRules = glc.getGrammarRules();
//		LinkedHashSet<String> lexicon = glc.getLexicon();
		
		LinkedHashSet<String> grammarRules = new LinkedHashSet<String>();
		LinkedHashSet<String> lexicon = new LinkedHashSet<String>();
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
		
//		fullGrammarRules.add("S->IP CP FRAG");
		Earley earley = new Earley();
//		earley.parse("vaidade dos homens", fullGrammarRules);
		earley.parse("book that flight", grammarRules, lexicon);
		
//		for(Iterator i = fullGrammarRules.iterator(); i.hasNext(); ) {
//			String rule = i.next().toString();
//			if(rule.startsWith("IP"))
//				System.out.println(rule);
//		}
//		
//		System.out.println("- RULES: " + fullGrammarRules.size() + " (" + grammarRules.size() + " rules and " + lexicon.size() + " lexicons)");
//		String time = timeRan > 1000 ? timeRan + " segundos." : timeRan + " millisegundos.";
//		System.out.println("- TIME: " + time);
		
	}
}