package ime.mac5725.earley.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;


/**
 * @author rayssak
 * @reason Reads a corpus file and gather its grammar rules.
 */
public class TreeBankHandler {
	
	private int ruleLevelCount = 0;
	
	private boolean sentenceWordAdded = false, hasClosingParenthesis = false;
	
	private static String NEXT_ELEMENT_CHAR = "->";
	
	private String line = "", currentRule = "";
	
	private LinkedList<String> tmp;
	
	private LinkedHashSet<String> grammarRules;
	private LinkedHashSet<String> lexicon;
	private HashMap<String, ArrayList<String>> grammarTrees;
	private LinkedHashMap<String, String> sentenceIndex;
	
	public LinkedHashSet<String> getFullGrammarRules() {
		LinkedHashSet<String> tmp = new LinkedHashSet<String>();
		tmp.addAll(grammarRules);
		tmp.addAll(lexicon);
		return tmp;
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
		grammarRules = new LinkedHashSet<String>();
		grammarTrees = new HashMap<String, ArrayList<String>>();
		lexicon = new LinkedHashSet<String>();
		sentenceIndex = new LinkedHashMap<String, String>();
		
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
		
		handleSpecialCases();
		
	}
	
	public HashMap<String, ArrayList<String>> getGrammarTrees() {
		return this.grammarTrees;
	}

	private boolean isValidLine() {
		line = line.startsWith("\t") ? line.replace("\t", "") : line;
		return line.matches(".*") && !line.isEmpty();
	}
	
	private void readRules(char currentLetter, int index) {
		sentenceWordAdded = false;
		hasClosingParenthesis = false;
		getPOSTags(currentLetter, index);
	}

	private void getPOSTags(char currentLetter, int index) {

		try {
		
			for(; index<line.length(); index++) {
				
				currentLetter = getValidNextLetter(currentLetter, index);

				if(isOpeningParenthesis(currentLetter)) {
					
					addTempRule();
					ruleLevelCount++;
					clearCurrentRule();

				} else if(isClosingParenthesis(currentLetter)) {
					
					hasClosingParenthesis = true;
					handlePontuation();
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

	// Skips first parenthesis
	private char getValidNextLetter(char currentLetter, int index) {
		return String.valueOf(currentLetter).matches("\\t") ? line.charAt(index+1) : line.charAt(index);
	}
	
	private boolean isOpeningParenthesis(char currentLetter) {
		return String.valueOf(currentLetter).matches("\\(");
	}
	
	private boolean isClosingParenthesis(char currentLetter) {
		return String.valueOf(currentLetter).matches("\\)");
	}
	
	private boolean isValidChar(char currentChar) {
		return !isOpeningParenthesis(currentChar) && 
			   !isClosingParenthesis(currentChar) &&
			   !String.valueOf(currentChar).matches("\\s");
	}
	
	// Checks if it's a capital letter followed by a space,
	// another capital letter or a parenthesis. Otherwise, it's 
	// not a POS tag, it's the word sentence!
	private boolean isNextCharPOSTag(char currentLetter, int index) {
		
		boolean followedBySpace = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\s+");
		boolean followedByCapitalLetter = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("[A-ZÀ-Ú]|\\$");
		boolean followedByParenthesis = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\(");
		boolean followedByPlusSign = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\+");
		boolean followedByBar = index+1>=line.length() ? false : String.valueOf(line.charAt(index+1)).matches("\\/");
		
		// If index = line.lenght(), it's the last word and
		// there's no need to check anything else.
		return index != line.length() &&
			   (followedBySpace || followedByCapitalLetter || followedByParenthesis || followedByPlusSign || followedByBar) ||
			   (String.valueOf(currentLetter).matches("\\+") && followedByCapitalLetter) ||
			   isPontuation(currentLetter);
		
	}

	private boolean isPontuation(char currentLetter) {
		return currentLetter == ':' || currentLetter == ';' || currentLetter == ',' || currentLetter == '.' || currentLetter == '!' || currentLetter == '?';
	}
	
	private void getWord() {
			
		String lexiconRule = !line.matches("\\s+") && !line.split("\\s")[line.split("\\s").length-1].contains("(") ?
						  	  line.split("\\s")[line.split("\\s").length-1].replaceAll("\\s", "").replaceAll("\\)", "").toLowerCase() : "";
						  	 
		if(!lexiconRule.isEmpty() && !currentRule.isEmpty()) {
			lexicon.add(currentRule + NEXT_ELEMENT_CHAR + " " + lexiconRule);
			sentenceIndex.put(tmp.toString().replaceAll("\\[", ""), lexiconRule);
		}
		
	}
	
	private void clearCurrentRule() {
		currentRule = "";
	}
	
	private boolean hasPontuation() {
		return currentRule.contains(".") || currentRule.contains(",") || currentRule.contains(":") || 
			   currentRule.contains(";") || currentRule.contains("?") || currentRule.contains("!");
	}
	
	private void handlePontuation() {
		if(hasPontuation() && currentRule.length()>1) {
			lexicon.add(currentRule.substring(0, 1) + NEXT_ELEMENT_CHAR + " " + currentRule.substring(1, 2));
			tmp.add(ruleLevelCount + " " + currentRule.substring(0, 1));
			sentenceIndex.put(tmp.toString().replaceAll("\\[", ""), currentRule.substring(0, 1));
			clearCurrentRule();
		} 
	}

	private void addTempRule() {
		if(!currentRule.isEmpty() && !currentRule.matches("\\s")) 
			tmp.add(ruleLevelCount + " " + currentRule);
	}
	
	private void addTempFirstRule() {
		if(!hasClosingParenthesis && !currentRule.isEmpty()) 
			tmp.add(ruleLevelCount + " " + currentRule);
	}
	
	private void clearPOSTagsAlreadyFinalized(String currentPOSTag, String currentLevel) {
		String posTagsToRemove[] = currentPOSTag.split("\\s");
		for(int i=0; i<posTagsToRemove.length; i++)
			tmp.remove(currentLevel + " " + posTagsToRemove[i]);
	}
	
	private void handleTempRules() {
		if(!tmp.isEmpty() && Integer.parseInt(tmp.getLast().split(" ")[0]) - ruleLevelCount == 2) {
			String currentPOSTag = "";
			String currentLevel = tmp.getLast().split(" ")[0];
			currentPOSTag = getPOSTagsWithSameLevel(currentPOSTag, currentLevel);
			addCurrentLevelRulesToRespectivePOSTag(currentPOSTag, currentLevel);
		}
	}
	
	private String getPOSTagsWithSameLevel(String currentPOSTag, String currentLevel) {
		
		for(Iterator i = tmp.iterator(); i.hasNext(); ) {
			String posTag = i.next().toString();
			if(posTag.startsWith(currentLevel))
				currentPOSTag += currentPOSTag.isEmpty() ? posTag.split(" ")[1] : " " + posTag.split(" ")[1];
		}
		
		return currentPOSTag;
		
	}
	
	private void addCurrentLevelRulesToRespectivePOSTag(String currentPOSTag, String currentLevel) {
		
		while(tmp.getLast().startsWith(currentLevel)) {
			
			String currentTargetLevel = String.valueOf(Integer.parseInt(tmp.getLast().split(" ")[0]) - 1);
			
			for(Iterator i = tmp.descendingIterator(); i.hasNext(); ) {
				
				String currentItem = i.next().toString();
				if(currentItem.startsWith(currentTargetLevel) && currentPOSTag.split(" ").length>1) {
					
					String words = "";
					boolean continueGetting = false;
					ArrayList<String> rules = new ArrayList<String>();
					String item = currentItem.replace(currentTargetLevel + " ", "");
					
					grammarRules.add(item + NEXT_ELEMENT_CHAR + " " + currentPOSTag);
					String current = currentItem + ", " + currentLevel + " " + currentPOSTag.split(" ")[0];
					
 					for(String currentRule : sentenceIndex.keySet()) {
						if(currentRule.contains(current) || continueGetting) {
							rules.add(currentRule);
							continueGetting = true;
						}
						if(currentRule.contains("\\.") || currentRule.contains("\\,") || currentRule.equals("\\;")) {
							continueGetting = false;
							break;
						}
					}
					for(String currentValue : rules)
						words += words.isEmpty() ? sentenceIndex.get(currentValue) : " " + sentenceIndex.get(currentValue);
						
					words = words.startsWith(" ") ? words.substring(1, words.length()).toLowerCase() : words.toLowerCase();
					if(words.contains(".") || words.contains(",") || words.contains(":") || words.contains(";") || words.contains("!"))
						words = words.replaceFirst("[\\.\\,] ", " ").replaceAll("\\s{2,}", " ");
					
					if(grammarTrees.get(item + NEXT_ELEMENT_CHAR + " " + currentPOSTag) != null) {
						ArrayList<String> currentValues = new ArrayList<String>();
						currentValues.add(words);
						ArrayList<String> previousValues = grammarTrees.put(item + NEXT_ELEMENT_CHAR + " " + currentPOSTag, currentValues);
						previousValues.addAll(currentValues);
						previousValues.addAll(currentValues);
						grammarTrees.put(item + NEXT_ELEMENT_CHAR + " " + currentPOSTag, previousValues);
					} else {
						ArrayList<String> sentence = new ArrayList<String>();
						sentence.add(words);
						grammarTrees.put(item + NEXT_ELEMENT_CHAR + " " + currentPOSTag, sentence);
					}

					if(currentItem.equals("1 IP")) 
						sentenceIndex.clear();
					
					break;
				}
			}
			
			clearPOSTagsAlreadyFinalized(currentPOSTag, currentLevel);
			
		}
		
	}

	private void handleSpecialCases() {
		
		// Removes recursive rules:
		// 		,-> ,
		// 		IP-> IP
		// 		.-> .
		for(Iterator it=grammarRules.iterator(); it.hasNext(); ) {
			
			String current = it.next().toString();
			String currentRule = current.split(ConstantsUtility.NEXT_ELEMENT_CHAR)[0];
			String currentTerminals = current.split(ConstantsUtility.NEXT_ELEMENT_CHAR + " ")[1];
			
			if(currentRule.equals(currentTerminals))
				it.remove();
			// Handles "(, OPEN)" and "(, CLOSE)" cases (4 rules instances):
			// 		- ",-> O"
			//      - ",-> C"
			else if(isPontuation(currentRule.charAt(0)) && !isPontuation(currentTerminals.charAt(0)))
				it.remove();
			
		}
		
		// Can not remove ALL cases that has only one POS tag (e.g.: NP-> N)!
		// The best way of overcoming the special cases is removing specifically
		// each one of them (IP-> NP, NP->PP and PP-> IP, a recursive cycle).
		grammarRules.remove("IP-> NP");
		grammarRules.remove("NP-> PP");
		grammarRules.remove("PP-> IP");
		grammarRules.remove("IP-> CP");
		grammarRules.remove("CP-> IP");
		grammarRules.remove("NP-> CP");
		grammarRules.remove("PP-> CP");
		grammarRules.remove("NP-> IP");
		grammarRules.remove("IP-");
		
		lexicon.remove("NP-> elliptical");
		lexicon.remove("NP-> nos");
		lexicon.remove("WPP-> 0");
		lexicon.remove("VB");
		
	}
	
}