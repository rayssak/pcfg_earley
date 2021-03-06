package ime.mac5725.earley.util;

/**
 * @author rayssak
 * @reason Utility constants.
 */
public class ConstantsUtility {
	
	public static int THREAD_NUMBER_PREDICTOR = 3;
	
	public static int THREAD_NUMBER_COMPLETER = 5;
	
	public static String SYSTEM_SCAPE = "\\";

	public static String FIELD_SEPARATOR = "|";
	
	public static String FIELD_SEPARATOR_TO_REPLACE = SYSTEM_SCAPE + FIELD_SEPARATOR;
	
	public static String FIELD_SEPARATOR_WITH_STATE_LEVEL = "S[0-9]+" + FIELD_SEPARATOR_TO_REPLACE;
	
	public static String NEXT_ELEMENT_CHAR = "->";
	
	public static String NEXT_ELEMENT_CHAR_TO_REPLACE = SYSTEM_SCAPE + NEXT_ELEMENT_CHAR;
	
	public static String DOTTED_RULE = "*";
	
	public static String DUMMY_STATE_JURAFSKY = "Y" + NEXT_ELEMENT_CHAR + " " + DOTTED_RULE + " S|[0,0]";
	
	public static String DUMMY_STATE = "Y" + NEXT_ELEMENT_CHAR + " " + DOTTED_RULE + " IP CP FRAG|[0,0]";
	
}