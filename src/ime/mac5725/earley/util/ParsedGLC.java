package ime.mac5725.earley.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;


/**
 * @author rayssak
 * @reason Receives a GLC and a portuguese sentence in raw format to 
 * 		   pre-process and generate the formatted Grammar Tree required
 * 		   as the Parser input.
 */
public class ParsedGLC {
	
	private HashSet<String> rules;
	private String line = "", currentParent = "", currentWord = "", currentRule = "";
	
	public HashSet<String> readGrammar(File grammar) {

		rules = new HashSet<String>();
		FileInputStream input;
		BufferedReader reader;
		
		try {
			
			input = new FileInputStream(grammar);
			reader = new BufferedReader(new InputStreamReader(input));
			
			while ((line = reader.readLine()) != null) {
				
				char currentLetter = line.charAt(0);
				readRule(currentLetter);
				
			}
			
		} catch (FileNotFoundException fileNotFoundException) {
			System.out.println(fileNotFoundException.getMessage());
		} catch (IOException ioException) {
			System.out.println(ioException.getMessage());
		}
		
		return rules;
		
	}

	private void readRule(char currentLetter) {

		for(int i=1; i<line.length(); i++) {
			
			// Skip first parenthesis
			currentLetter = line.charAt(i);
			
			// Line start
			if(currentLetter == '(')
				currentParent += ",";
			
			// End of rule (already identified)
			else if(currentLetter == ')')
				handleEndOfRule(currentLetter);
			
			// Checks if POS tags were already collected
			// and it's time to collect the word of the
			// sentence
			else if(currentLetter == ' ')
				checkWord(currentLetter);
			
			// Gets the POS tags recursively
			else if(String.valueOf(currentLetter).matches("[A-Z]")) {
				currentParent += currentLetter;
				readRule(currentLetter);
			}				
			
		}
		
	}

	private void checkWord(char currentLetter) {
		// TODO Auto-generated method stub
		
	}

	private void handleEndOfRule(char currentLetter) {
		
		
		
	}

}