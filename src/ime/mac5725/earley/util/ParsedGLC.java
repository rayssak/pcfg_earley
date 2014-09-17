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
	
	private int endCount;
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
				
				System.out.println(line);//
				char currentLetter = line.charAt(0);
				System.out.println(currentLetter);//
				readRules(currentLetter, 1);
				
			}
			
		} catch (FileNotFoundException fileNotFoundException) {
			System.out.println(fileNotFoundException.getMessage());
		} catch (IOException ioException) {
			System.out.println(ioException.getMessage());
		}
		
		return rules;
		
	}
	
	private void readRules(char currentLetter, int index) {
		getPOSTags(currentLetter, index);
		if(!isFirstPOS()) {
			getWord();
			handleEndOfRule();
		}
	}

	private void getPOSTags(char currentLetter, int index) {

		for(; index<line.length(); index++) {
			
			// Skip first parenthesis
			currentLetter = String.valueOf(currentLetter).matches("\\t") && index+1>=line.length() ? line.charAt(index+1) : line.charAt(index);
			System.out.println(currentLetter);//
			
			// Line start
			if(currentLetter == '(')
				currentParent += ",";
			
			// End of rule (already identified)
			else if(currentLetter == ')')
				endCount++;
			
			// Checks if POS tags were already collected
			// and it's time to collect the word of the
			// sentence
//			else if(String.valueOf(currentLetter).matches("\\s+")) 
//				checkWord(currentLetter);
			
			// Gets the POS tags recursively
			else if(String.valueOf(currentLetter).matches("[A-Z]")) {
				currentParent += currentLetter;
				getPOSTags(currentLetter, ++index);
				break;
			}			
			
		}
		
	}
	
	private boolean isFirstPOS() {
		return line.contains("IP") ? true : false ;
	}

	private void getWord() {
		currentWord = !line.split("\\s")[line.split("\\s").length-1].contains("(") && 
					  line.split("\\s")[line.split("\\s").length-1].matches("[a-z]+.*") ? 
					  line.split("\\s")[line.split("\\s").length-1].replaceAll("\\s", "").replaceAll(")", "") : "";
	}

	private void handleEndOfRule() {
		rules.add(currentParent + " " + currentWord);
		currentWord = "";
		for(int i=0; i<endCount; i++) {
			String lastPosTag = currentParent.split(",")[currentParent.split(",").length];
			currentParent = currentParent.replace(lastPosTag, "");
		}
	}

}