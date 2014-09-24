package ime.mac5725.earley.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * @author rayssak
 * @reason Receives a GLC and a portuguese sentence in raw format to 
 * 		   pre-process and generate the formatted Grammar Tree required
 * 		   as the Parser input.
 */
public class ParsedGLC {
	
	private int ruleLevelCount = 0;
	
	private boolean sentenceWordAdded = false, hasClosingBracket = false;
	
	private String nextElementChar = "->";
	
	private String line = "", currentRule = "";
	
	private LinkedList<String> tmp;
	
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
		tmp = new LinkedList<String>();
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
		hasClosingBracket = false;
		getPOSTags(currentLetter, index);
		handleEndOfRule();
	}

	private void getPOSTags(char currentLetter, int index) {

		try {
		
			if(line.contains("P+D"))
					System.out.println("HERE");
			
			for(; index<line.length(); index++) {
				
				currentLetter = getValidNextLetter(currentLetter, index);

				if(isOpeningBracket(currentLetter)) {
					
					if(!currentRule.isEmpty()) tmp.add(ruleLevelCount + " " + currentRule);
					ruleLevelCount++;
					currentRule = "";

				} else if(isClosingBracket(currentLetter)) {
					
					hasClosingBracket = true;
					if(!currentRule.isEmpty()) tmp.add(ruleLevelCount + " " + currentRule);
					ruleLevelCount--;
					getWord();
					currentRule = "";
					
				} else if(isValidChar(currentLetter) && isNextCharPOSTag(currentLetter, index)) 
					currentRule += currentLetter;
				
				if(!tmp.isEmpty() && Integer.parseInt(tmp.getLast().split(" ")[0]) - ruleLevelCount == 2) {
					
					String currentPOSTag = "";
					String currentLevel = tmp.getLast().split(" ")[0];
					
					for(Iterator i = tmp.iterator(); i.hasNext(); ) {
						String posTag = i.next().toString();
						if(posTag.startsWith(currentLevel))
							currentPOSTag += currentPOSTag.isEmpty() ? posTag.split(" ")[1] : " " + posTag.split(" ")[1];
					}
					
					while(tmp.getLast().startsWith(currentLevel)) {
						
						String currentTargetLevel = String.valueOf(Integer.parseInt(tmp.getLast().split(" ")[0]) - 1);
						
						for(Iterator i = tmp.descendingIterator(); i.hasNext(); ) {
							
							String currentItem = i.next().toString();
							
							if(currentItem.startsWith(currentTargetLevel)) {
								String item = currentItem.replace(currentTargetLevel + " ", "");
								fullGrammarRules.add(item + nextElementChar + currentPOSTag);
								break;
							}
						}
						
						String posTagsToRemove[] = currentPOSTag.split("\\s");
						for(int i=0; i<posTagsToRemove.length; i++)
							tmp.remove(currentLevel + " " + posTagsToRemove[i]);
						
					}
					
				}
				
			}
			
			if(!hasClosingBracket) tmp.add(ruleLevelCount + " " + currentRule);
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
		boolean followedByPlusSign = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\+");
		
		// If index = line.lenght(), it's the last word and
		// there's no need to check anything else.
		return index != line.length() &&
			   String.valueOf(currentLetter).matches("[A-Z]") && 
			   (followedBySpace || followedByCapitalLetter || followedByBracket || followedByPlusSign) ||
			   (String.valueOf(currentLetter).matches("\\+") && followedByCapitalLetter);
		
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
			
			
		}
		
	}
	
}