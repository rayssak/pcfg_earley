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
	
	private int ruleLevelCount = 0;
	
	private String nextElementChar = "->";
	
	private String line = "", mainRule = "", currentRule = "", lastRule = "";
	
	private HashSet<String> fullGrammarRules;
	private HashSet<String> grammarRules;
	private HashSet<String> lexicon;
	
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
				System.out.println(line);//
					if(isValidLine()) {
						char currentLetter = line.charAt(0);
						System.out.println(currentLetter);//
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
		line = line.startsWith("\t") ? line.replace("\t", "") : line;
		return line.matches(".*\\w+.*");
	}
	
	private void readRules(char currentLetter, int index) {
		getPOSTags(currentLetter, index);
		if(!hasOnlyPOS()) {	
			getWord();
//			handleEndOfRule();
		}
	}

	private void getPOSTags(char currentLetter, int index) {

		try {
		
			for(; index<line.length(); index++) {
				
				currentLetter = getValidNextLetter(currentLetter, index);
				System.out.println(currentLetter);//
				if(isOpeningBracket(currentLetter)) {
					mainRule += ruleLevelCount>0 && !mainRule.isEmpty() && !lastRule.isEmpty() ? "," : "";
					lastRule += ruleLevelCount>1 && !currentRule.isEmpty() ? currentRule : "";
					currentRule = "";
					ruleLevelCount++;
				} else if(isClosingBracket(currentLetter)) {
					if(!currentRule.isEmpty())
						grammarRules.add(lastRule.split(",")[lastRule.split(",").length-1] + nextElementChar + currentRule.split("\\s")[0]);
					currentRule = "";
					ruleLevelCount--;
				} else if(isValidChar(currentLetter) && isNextCharPOSTag(currentLetter, index)) 
					currentRule += currentLetter;
			}
			
			lastRule = currentRule;
			
			if(ruleLevelCount==1) 
				mainRule += lastRule;
			else if(ruleLevelCount>1) 
				lastRule += lastRule.isEmpty() ? currentRule : "," + currentRule;
				
			currentRule = "";
			
		} catch(StringIndexOutOfBoundsException outOfBoundsException) {
			System.out.println("currentLine: " + line + "\ncurrentLetter: " + currentLetter);
			System.out.println(outOfBoundsException.getMessage());
		}
		
	}

	// Skips first brackets
	private char getValidNextLetter(char currentLetter, int index) {
		return String.valueOf(currentLetter).matches("\\t") && index+1>line.length() ? line.charAt(index+1) : line.charAt(index);
	}
	
	private boolean isOpeningBracket(char currentLetter) {
		return String.valueOf(currentLetter).matches("\\(");
	}
	
	private boolean isClosingBracket(char currentLetter) {
		return String.valueOf(currentLetter).matches("\\)");
	}
	
	private boolean isValidChar(char currentChar) {
		return !isOpeningBracket(currentChar) && 
			   !isClosingBracket(currentChar) &&
			   !String.valueOf(currentChar).matches("\\s");
	}
	
	// Checks if it's a capital letter followed by a space,
	// another capital letter or a bracket. Otherwise, it's 
	// not a POS tag, it's the word sentence!
	private boolean isNextCharPOSTag(char currentLetter, int index) {
		
		boolean followedBySpace = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\s+");
		boolean followedByCapitalLetter = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("[A-Z�-�]|\\$");
		boolean followedByBracket = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\(");
		
		// If index = line.lenght(), it's the last word and
		// there's no need to check anything else.
		return index != line.length() &&
			   String.valueOf(currentLetter).matches("[A-Z]") && 
			   (followedBySpace || followedByCapitalLetter || followedByBracket);
		
	}
	
	private boolean hasOnlyPOS() {
		return line.matches(".*[a-z�-�]+.*") ? false : true;
	}
	
	private void getWord() {
		String lexiconRule = !line.split("\\s")[line.split("\\s").length-1].contains("(") && 
						  	  line.split("\\s")[line.split("\\s").length-1].matches("[A-Za-z�-��-�]+.*") ? 
						  	  line.split("\\s")[line.split("\\s").length-1].replaceAll("\\s", "").replaceAll("\\)", "").toLowerCase() : "";
		if(!lexicon.isEmpty())
			lexicon.add(lexiconRule);
	}
	
}