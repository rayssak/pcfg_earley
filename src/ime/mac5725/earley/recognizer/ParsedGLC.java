package ime.mac5725.earley.recognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
	
	private LinkedHashSet<String> fullGrammarRules;
	private LinkedHashSet<String> grammarRules;
	private LinkedHashSet<String> lexicon;
	
	public LinkedHashSet<String> getFullGrammarRules() {
		return fullGrammarRules;
	}

	public LinkedHashSet<String> getGrammarRules() {
		return grammarRules;
	}
	
	public LinkedHashSet<String> getLexicon() {
		return lexicon;
	}

	public void readGrammar(File grammar) {

		FileInputStream input;
		BufferedReader reader;
		tmp = new LinkedList<String>();
		fullGrammarRules = new LinkedHashSet<String>();
		grammarRules = new LinkedHashSet<String>();
		lexicon = new LinkedHashSet<String>();
		
		try {
			
			input = new FileInputStream(grammar);
			reader = new BufferedReader(new InputStreamReader(input));
			
			while ((line = reader.readLine()) != null) 
				if(isValidLine()) {
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
		return line.matches(".*") && !line.isEmpty();
	}
	
	private void readRules(char currentLetter, int index) {
		sentenceWordAdded = false;
		hasClosingBracket = false;
		getPOSTags(currentLetter, index);
	}

	private void getPOSTags(char currentLetter, int index) {

		try {
		
			for(; index<line.length(); index++) {
				
				currentLetter = getValidNextLetter(currentLetter, index);

				if(isOpeningBracket(currentLetter)) {
					
					addTempRule();
					ruleLevelCount++;
					clearCurrentRule();

				} else if(isClosingBracket(currentLetter)) {
					
					hasClosingBracket = true;
					addTempRule();
					ruleLevelCount--;
					getWord();
					clearCurrentRule();
					
				} else if(isValidChar(currentLetter) && isNextCharPOSTag(currentLetter, index)) 
					currentRule += currentLetter;
				
				handleTempRules();
				
			}
			
			addTempFirstRule();
			getWord();
			clearCurrentRule();
			
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
	
	private void clearCurrentRule() {
		currentRule = "";
	}

	private void addTempRule() {
		if(!currentRule.isEmpty() && !currentRule.matches("\\s")) 
			tmp.add(ruleLevelCount + " " + currentRule);
	}
	
	private void addTempFirstRule() {
		if(!hasClosingBracket && !currentRule.isEmpty()) 
			tmp.add(ruleLevelCount + " " + currentRule);
	}
	
	private void clearPOSTagsAlreadyFinalized(String currentPOSTag, String currentLevel) {
		String posTagsToRemove[] = currentPOSTag.split("\\s");
		for(int i=0; i<posTagsToRemove.length; i++)
			tmp.remove(currentLevel + " " + posTagsToRemove[i]);
	}
	
	private String getPOSTagsWithSameLevel(String currentPOSTag, String currentLevel) {
		
		for(Iterator i = tmp.iterator(); i.hasNext(); ) {
			String posTag = i.next().toString();
			if(posTag.startsWith(currentLevel))
				currentPOSTag += currentPOSTag.isEmpty() ? posTag.split(" ")[1] : " " + posTag.split(" ")[1];
		}
		
		return currentPOSTag;
		
	}
	
	private void handleTempRules() {
		if(!tmp.isEmpty() && Integer.parseInt(tmp.getLast().split(" ")[0]) - ruleLevelCount == 2) {
			String currentPOSTag = "";
			String currentLevel = tmp.getLast().split(" ")[0];
			currentPOSTag = getPOSTagsWithSameLevel(currentPOSTag, currentLevel);
			addCurrentLevelRulesToRespectivePOSTag(currentPOSTag, currentLevel);
		}
	}

	private void addCurrentLevelRulesToRespectivePOSTag(String currentPOSTag, String currentLevel) {
		
		while(tmp.getLast().startsWith(currentLevel)) {
			
			String currentTargetLevel = String.valueOf(Integer.parseInt(tmp.getLast().split(" ")[0]) - 1);
			
			for(Iterator i = tmp.descendingIterator(); i.hasNext(); ) {
				
				String currentItem = i.next().toString();
				
				if(currentItem.startsWith(currentTargetLevel)) {
					String item = currentItem.replace(currentTargetLevel + " ", "");
					fullGrammarRules.add(item + nextElementChar + currentPOSTag);
					grammarRules.add(item + nextElementChar + currentPOSTag);
					break;
				}
			}
			
			clearPOSTagsAlreadyFinalized(currentPOSTag, currentLevel);
			
		}
		
	}
	
}