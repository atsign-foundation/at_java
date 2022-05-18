package org.atsign.client.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;

import org.atsign.common.AtSign;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "switch", description = "Switches @sign")
public class SwitchAtSignCommand implements Callable<String> {

	private static final String currAtSignFile = System.getProperty("user.dir") + "/.currAtSign";

	@Option(names = { "-u",
			"--uSign" }, description = "Atsign to switch to", converter = AtSignConverter.class, required = true)
	private AtSign atSign;

	@Override
	public String call() throws Exception {
		
		File file = new File(currAtSignFile); //initialize File object and passing path as argument  
		if(!file.exists()) {
			 file.createNewFile();
		}
		
		try(FileOutputStream fileOutputStream = new FileOutputStream(file)){      
			
			byte byteArray[] = atSign.toString().getBytes(); //converting string into byte array      
			fileOutputStream.write(byteArray);  
		}catch(Exception exception){  
			System.out.println(exception);
		}      
		
		// Delete and recreate a file with entry for currAtSign
		System.out.println("Switched @sign to " + atSign.toString());
		return "Switched @sign to " + atSign.toString();
	}
}
