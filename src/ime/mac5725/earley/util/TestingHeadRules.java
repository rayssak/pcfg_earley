package ime.mac5725.earley.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestingHeadRules {
	
	public static void main(String[] args) {
		
		String line = "";
		FileInputStream input;
		BufferedReader reader;
			
		try {
			
			input = new FileInputStream(new File(args[0]));
			reader = new BufferedReader(new InputStreamReader(input));
			
			while ((line = reader.readLine()) != null) 
				if(line.startsWith("("))
					System.out.println(line);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		
	}

}