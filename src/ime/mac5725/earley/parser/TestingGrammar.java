package ime.mac5725.earley.parser;

import ime.mac5725.earley.util.ParsedGLC;

import java.io.File;

public class TestingGrammar {
	
	public static void main(String[] args) {

		ParsedGLC glc = new ParsedGLC();
		glc.readGrammar(new File(args[0]));
		
	}
}