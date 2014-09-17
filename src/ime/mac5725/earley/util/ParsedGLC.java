package ime.mac5725.earley.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;


/**
 * @author rayssak
 * @reason Receives a GLC and a portuguese sentence in raw format to 
 * 		   pre-process and generate the formatted Grammar Tree required
 * 		   as the Parser input.
 */
public class ParsedGLC {
	
	private HashSet<String> rules;
	private int currentRuleEndCount = 0;
	private String line = "", currentRule = "", currentWord = "";
	
	public HashSet<String> readGrammar(File grammar) {

		rules = new HashSet<String>();
		FileInputStream input;
		BufferedReader reader;
		
		try {
			
			input = new FileInputStream(grammar);
			reader = new BufferedReader(new InputStreamReader(input));
			
			while ((line = reader.readLine()) != null) {
				
				if(isValidLine()) {
					System.out.println(line);//
					char currentLetter = line.charAt(0);
					System.out.println(currentLetter);//
					readRules(currentLetter, 1);
				}
				
			}
			
		} catch (FileNotFoundException fileNotFoundException) {
			System.out.println(fileNotFoundException.getMessage());
		} catch (IOException ioException) {
			System.out.println(ioException.getMessage());
		}
		
		return rules;
		
	}

	private boolean isValidLine() {
		return line.matches(".*\\w+.*");
	}
	
	private void readRules(char currentLetter, int index) {
		getPOSTags(currentLetter, index);
		if(!hasOnlyPOS()) {
			getWord();
			handleEndOfRule();
		}
	}

	private void getPOSTags(char currentLetter, int index) {

		for(; index<line.length(); index++) {
			
			// Skip first brackets
			currentLetter = String.valueOf(currentLetter).matches("\\t") && index+1>=line.length() ? line.charAt(index+1) : line.charAt(index);
			System.out.println(currentLetter);//
			// if(String.valueOf(currentLetter).matches("[a-z]") break;
			// Line start
			if(currentLetter == '(')
				currentRule += ",";
			
			// End of rule (already identified)
			else if(currentLetter == ')')
				currentRuleEndCount++;
			
			// Checks if POS tags were already collected
			// and it's time to collect the word of the
			// sentence
//			else if(String.valueOf(currentLetter).matches("\\s+")) 
//				checkWord(currentLetter);
			
			// Gets the POS tags recursively
			else if(isNextCharPOSTag(currentLetter, index)) {
				currentRule += currentLetter;
				getPOSTags(currentLetter, ++index);
				break;
			}			
			
		}
		
	}

	// Checks if it's a capital letter followed by a space,
	// another capital letter or a bracket. Otherwise, it's 
	// not a POS tag, it's the word sentence!
	private boolean isNextCharPOSTag(char currentLetter, int index) {
		
		boolean followedBySpace = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\s+");
		boolean followedByCapitalLetter = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("[A-Z]");
		boolean followedByBracket = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\(");
		
		// If index = line.lenght(), it's the last word and
		// there's no need to check anything else.
		return index != line.length() &&
			   String.valueOf(currentLetter).matches("[A-Z]") && 
			   (followedBySpace || followedByCapitalLetter || followedByBracket);
		
	}
	
	private boolean hasOnlyPOS() {
		return line.matches(".*[a-z]+.*") ? false : true;
	}

	private void getWord() {
		currentWord = !line.split("\\s")[line.split("\\s").length-1].contains("(") && 
					  line.split("\\s")[line.split("\\s").length-1].matches("[A-Za-z]+.*") ? 
					  line.split("\\s")[line.split("\\s").length-1].replaceAll("\\s", "").replaceAll("\\)", "") : "";
	}

	private void handleEndOfRule() {
		rules.add(currentRule + " " + currentWord);
		clearVariables();
	}

	private void clearVariables() {
		
		for(int i=0; i<currentRuleEndCount; i++) {
			String lastPosTag = "," + currentRule.split(",")[currentRule.split(",").length-1];
			currentRule = currentRule.replaceAll("," + lastPosTag.replace(",", ""), "");
		}
		
		currentWord = "";
		currentRuleEndCount = 0;
		
	}

}