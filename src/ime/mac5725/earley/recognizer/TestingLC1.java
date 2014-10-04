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
		
		LinkedHashSet<String> fullGrammarRules = new LinkedHashSet<String>();
		fullGrammarRules.add("S-> NP VP");
		fullGrammarRules.add("S-> Aux NP VP");
		fullGrammarRules.add("S-> VP");
		fullGrammarRules.add("NP-> Pronoun");
		fullGrammarRules.add("NP-> Proper-Noun");
		fullGrammarRules.add("NP-> Det Nominal");
		fullGrammarRules.add("Nominal-> Noun");
		fullGrammarRules.add("Nominal-> Nominal PP");
		fullGrammarRules.add("VP-> Verb");
		fullGrammarRules.add("VP-> Verb NP");
		fullGrammarRules.add("VP-> Verb NP PP");
		fullGrammarRules.add("VP-> Verb PP");
		fullGrammarRules.add("VP-> VP PP");
		fullGrammarRules.add("PP-> Preposition NP");
		fullGrammarRules.add("Det-> that");
		fullGrammarRules.add("Det-> this");
		fullGrammarRules.add("Det-> a");
		fullGrammarRules.add("Noun-> book");
		fullGrammarRules.add("Noun-> flight");
		fullGrammarRules.add("Noun-> meal");
		fullGrammarRules.add("Noun-> money");
		fullGrammarRules.add("Verb-> book");
		fullGrammarRules.add("Verb-> include");
		fullGrammarRules.add("Verb-> prefer");
		fullGrammarRules.add("Pronoun-> I");
		fullGrammarRules.add("Pronoun-> she");
		fullGrammarRules.add("Pronoun-> me");
		fullGrammarRules.add("Proper-Noun-> Houston");
		fullGrammarRules.add("Proper-Noun-> NWA");
		fullGrammarRules.add("Aux-> does");
		fullGrammarRules.add("Preposition-> from");
		fullGrammarRules.add("Preposition-> to");
		fullGrammarRules.add("Preposition-> on");
		fullGrammarRules.add("Preposition-> near");
		fullGrammarRules.add("Preposition-> through");
		
//		fullGrammarRules.add("S->IP CP FRAG");
		Earley earley = new Earley();
//		earley.parse("vaidade dos homens", fullGrammarRules);
		earley.parse("book that flight", fullGrammarRules);
		
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