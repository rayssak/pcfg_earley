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
public class _ParsedGLC {
	
	private int ruleLevelCount = 0;
	
	private boolean sentenceWordAdded = false;
	
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
				char currentLetter = line.charAt(0);
				readRules(currentLetter, 0);
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
		sentenceWordAdded = false;
		getPOSTags(currentLetter, index);
		handleEndOfRule();
	}

	private void getPOSTags(char currentLetter, int index) {

		try {
		
			for(; index<line.length(); index++) {
				
				currentLetter = getValidNextLetter(currentLetter, index);
				
				if(isOpeningBracket(currentLetter)) {
					
					ruleLevelCount++;
					
					if(lastRule.isEmpty()) 
						lastRule = currentRule;
					else if(!currentRule.isEmpty()) {
						lastRule += addComma(currentRule);
						if(lastRule.split(",").length >2 )
							fullGrammarRules.add(lastRule.split(",")[lastRule.split(",").length-2] + nextElementChar + lastRule.split(",")[lastRule.split(",").length-1]);
						else 
							fullGrammarRules.add(lastRule.replace(",", nextElementChar));
					}
					
					currentRule = "";

				} else if(isClosingBracket(currentLetter)) {
					
					ruleLevelCount--;
					
					if(lastRule.contains(",")) {
						
						String lastCurrentRule = lastRule.split(",")[lastRule.split(",").length-1];
						fullGrammarRules.add(lastCurrentRule + nextElementChar + currentRule);
						grammarRules.add(lastCurrentRule + nextElementChar + currentRule);
						getWord();
						currentRule = lastRule;
						
					} else {
						
						if(!lastRule.isEmpty()) {
							fullGrammarRules.add(lastRule + nextElementChar + currentRule);
							grammarRules.add(lastRule + nextElementChar + currentRule);
							getWord();
							currentRule = lastRule;
						}
						
					}
					
					lastRule = "";
					
				} else if(isValidChar(currentLetter) && isNextCharPOSTag(currentLetter, index)) 
					currentRule += currentLetter;
				
			}
			
			if(ruleLevelCount == 1) 
				mainRule += mainRule.isEmpty() ? currentRule : addComma(currentRule);
			else 
				lastRule += lastRule.isEmpty() ? currentRule : addComma(currentRule);
			
			getWord();
			currentRule = "";
			
		} catch(StringIndexOutOfBoundsException outOfBoundsException) {
			System.out.println("currentLine: " + line + "\ncurrentLetter: " + currentLetter);
			System.out.println(outOfBoundsException.getMessage());
		}
		
	}

	private String addComma(String text) {
		return "," + text;
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
		
		if(!hasOnlyPOS() && !sentenceWordAdded) {
			
			String lexiconRule = !line.split("\\s")[line.split("\\s").length-1].contains("(") && 
							  	  line.split("\\s")[line.split("\\s").length-1].matches("[A-Za-zÀ-Úà-ú]+.*") ? 
							  	  line.split("\\s")[line.split("\\s").length-1].replaceAll("\\s", "").replaceAll("\\)", "").toLowerCase() : "";
							  	  
		  	fullGrammarRules.add(currentRule + nextElementChar + lexiconRule);
			lexicon.add(currentRule + nextElementChar + lexiconRule);
			
			sentenceWordAdded = true;
		
		}
	}
	
	private void handleEndOfRule() {
		
		if(ruleLevelCount == 0) {
			
			String mainRules[] = mainRule.split(",");
			
			for(int i=0; i<mainRules.length; i++)
				if(i==0)
					mainRule = mainRules[i] + nextElementChar;
				else
					mainRule += addComma(mainRules[i]);
			
			fullGrammarRules.add(mainRule);
			grammarRules.add(mainRule);
			
		}
		
	}
	
}