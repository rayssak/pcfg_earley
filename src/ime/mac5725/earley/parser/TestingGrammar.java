package ime.mac5725.earley.parser;

import ime.mac5725.earley.util.ParsedGLC;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class TestingGrammar {
	
	public static void main(String[] args) {	
		
		long timeRan = System.currentTimeMillis();
		
		ParsedGLC glc = new ParsedGLC();
		glc.readGrammar(new File(args[0]));
		timeRan = System.currentTimeMillis() - timeRan;
		
		LinkedHashSet<String> fullGrammarRules = glc.getFullGrammarRules();
		LinkedHashSet<String> grammarRules = glc.getGrammarRules();
		LinkedHashSet<String> lexicon = glc.getLexicon();
		
		for(Iterator i = fullGrammarRules.iterator(); i.hasNext(); )
			System.out.println(i.next());
		
		System.out.println("- RULES: " + fullGrammarRules.size() + " (" + grammarRules.size() + " rules and " + lexicon.size() + " lexicons)");
		String time = timeRan > 1000 ? timeRan + " segundos." : timeRan + " millisegundos.";
		System.out.println("- TIME: " + time);
		
	}
}