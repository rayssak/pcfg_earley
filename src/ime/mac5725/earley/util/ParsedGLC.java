package ime.mac5725.earley.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


/**
 * @author rayssak
 * @reason Receives a GLC and a portuguese sentence in raw format to 
 * 		   pre-process and generate the formatted Grammar Tree required
 * 		   as the Parser input.
 */
public class ParsedGLC {
	
	private HashSet<String> fullGrammarRules;
	private HashSet<String> grammarRules;
	private HashSet<String> lexicon;
	
	private int currentRuleEndCount = 0;
	private String line = "", currentRule = "", currentWord = "";
	
	public HashSet<String> getFullGrammarRules() {
		return fullGrammarRules;
	}

	public HashSet<String> getGrammarRules() {
		return grammarRules;
	}
	
	public HashSet<String> getLexicon() {
		return lexicon;
	}

	public void readGrammar(File grammar) {

		FileInputStream input;
		BufferedReader reader;
		fullGrammarRules = new HashSet<String>();
		grammarRules = new HashSet<String>();
		lexicon = new HashSet<String>();
		
		try {
			
			input = new FileInputStream(grammar);
			reader = new BufferedReader(new InputStreamReader(input));
			
			while ((line = reader.readLine()) != null) {
					if(isValidLine()) {
						char currentLetter = line.charAt(0);
						readRules(currentLetter, 0);
					}
			}
			
		} catch (FileNotFoundException fileNotFoundException) {
			System.out.println(fileNotFoundException.getMessage());
		} catch (IOException ioException) {
			System.out.println(ioException.getMessage());
		}
		
	}

	private boolean isValidLine() {
		return line.matches(".*\\w+.*");
	}
	
	private void readRules(char currentLetter, int index) {
		if(isNewRule())
			currentRule = "";
		getPOSTags(currentLetter, index);
		if(!hasOnlyPOS()) {	
			getWord();
			handleEndOfRule();
		}
	}

	private boolean isNewRule() {
		return line.matches("\\([A-Z].*") ? true : false;
	}

	private void getPOSTags(char currentLetter, int index) {

		try {
		
			for(; index<line.length(); index++) {
				
				// Skip first brackets
				currentLetter = String.valueOf(currentLetter).matches("\\t") && index+1>line.length() ? line.charAt(index+1) : line.charAt(index);
				// Line start
				if(currentLetter == '(')
					currentRule += ",";
				
				// End of rule (already identified)
				else if(currentLetter == ')')
					currentRuleEndCount++;
				
				// Gets the POS tags recursively
				else if(isNextCharPOSTag(currentLetter, index)) {
					currentRule += currentLetter;
					getPOSTags(currentLetter, ++index);
					break;
				}			
				
			}
			
		} catch(StringIndexOutOfBoundsException outOfBoundsException) {
			System.out.println("currentLine: " + line + "\ncurrentLetter: " + currentLetter);
			System.out.println(outOfBoundsException.getMessage());
		}
		
	}
	
	// Checks if it's a capital letter followed by a space,
	// another capital letter or a bracket. Otherwise, it's 
	// not a POS tag, it's the word sentence!
	private boolean isNextCharPOSTag(char currentLetter, int index) {
		
		boolean followedBySpace = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\s+");
		boolean followedByCapitalLetter = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("[A-ZÀ-Ú]|\\$");
		boolean followedByBracket = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\(");
		
		// If index = line.lenght(), it's the last word and
		// there's no need to check anything else.
		return index != line.length() &&
			   String.valueOf(currentLetter).matches("[A-Z]") && 
			   (followedBySpace || followedByCapitalLetter || followedByBracket);
		
	}
	
	private boolean hasOnlyPOS() {
		return line.matches(".*[a-zà-ú]+.*") ? false : true;
	}

	private void getWord() {
		currentWord = !line.split("\\s")[line.split("\\s").length-1].contains("(") && 
					  line.split("\\s")[line.split("\\s").length-1].matches("[A-Za-zÀ-Úà-ú]+.*") ? 
					  line.split("\\s")[line.split("\\s").length-1].replaceAll("\\s", "").replaceAll("\\)", "") : "";
	}

	private void handleEndOfRule() {
		
		currentRule = currentRule.charAt(0) == ',' ? currentRule.substring(1, currentRule.length()) : currentRule;
		
		// Get grammar rule (POS tags)
		grammarRules.add(currentRule);
		
		// Get full grammar rule (POS tags + sentence word)
		fullGrammarRules.add(currentRule + " " + currentWord);
		
		// Get lexicon rule (last POS tag + current sentence word)
		lexicon.add(currentRule.split(",")[currentRule.split(",").length-1] + " " + currentWord);
		
		clearVariables();
		
	}

	private void clearVariables() {
		
		ArrayList<String> tmpPOSTags = new ArrayList<String>();
		String tmp[] = currentRule.split(",");
		currentWord = "";
		currentRule = "";
		
		for(int i=0; i<tmp.length; i++) 
			tmpPOSTags.add(tmp[i]);
		
		while(currentRuleEndCount > 0) {
			tmpPOSTags.remove(tmpPOSTags.size()-1);
			currentRuleEndCount--;
		}
		
		for(Iterator i=tmpPOSTags.iterator(); i.hasNext(); ) {
			currentRule += i.next().toString();
			if(i.hasNext())
				currentRule += ",";
		}
		
	}

}