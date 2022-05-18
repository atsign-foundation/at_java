package org.atsign.client.cli;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "whoami", description = "Shows currunt @sign")
public class ShowCurruntAtSignCommand implements Callable<String>{
	
	private static final String currAtSignFile = System.getProperty("user.dir") + "/.currAtSign";
	
	@Override
	public String call() throws Exception {
		File file = new File(currAtSignFile); //initialize File object and passing path as argument  
		
		try(FileInputStream fileInputStream = new FileInputStream(file)){      
			
			byte byteArray[] = fileInputStream.readAllBytes(); //converting string into byte array 
			String currAtSign = new String(byteArray);
			System.out.println(currAtSign);
			return currAtSign;
		}catch(Exception exception){  
			System.out.println(exception);
		}
		return null;     
		
	}
}
