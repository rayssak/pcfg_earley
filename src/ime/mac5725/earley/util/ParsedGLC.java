package ime.mac5725.earley.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * @author rayssak
 * @reason Receives a GLC and a portuguese sentence in raw format to 
 * 		   pre-process and generate the formatted Grammar Tree required
 * 		   as the Parser input.
 */
public class ParsedGLC {
	
	// First arg: grammar (glc)
	// Second arg: sentence (pt-br)
	public static void main(String... args) {
		
		String line = null, posTag = null, word = null;
		FileInputStream input;
		BufferedReader reader;
		Set<String> symbols;
		File grammar = new File(args[0]);
		HashMap<String, String> rules = new HashMap<String, String>();
		
		try {
			
			input = new FileInputStream(grammar);
			reader = new BufferedReader(new InputStreamReader(input));
			symbols = new HashSet<String>();
			
			while ((line = reader.readLine()) != null) {
				
				line = line.replace("IP", "S");
				System.out.println(line);
				// tenho que dar replace em tudo que não for letras maiúsculas p/ POS
				posTag = line.matches(".*[A-Z].*") ? line : null;
				// tenho que tratar casos tipo esse:
				// (NP (NPR Senhor))
				// ~ tenho dois POS e WORD!
				word = line.matches("[^A-Z]") ? line : null;
//				rules.put(<pos_tag>, <word>);
				
			}
			
		} catch (FileNotFoundException fileNotFoundException) {
			System.out.println(fileNotFoundException.getMessage());
		} catch (IOException ioException) {
			System.out.println(ioException.getMessage());
		}
		
		
	}
	
}