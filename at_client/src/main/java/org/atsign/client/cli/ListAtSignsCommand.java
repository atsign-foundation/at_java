package org.atsign.client.cli;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "list", description = "Shows list of @signs", subcommands = {
	      ListKeysCommand.class,
	  })
public class ListAtSignsCommand implements Callable<String>{
	
	private static final String rootFolder = System.getProperty("user.dir") +  "/keys/";
	
	@Override
	public String call() throws Exception {
		File file = new File(rootFolder);
		File[] keyFiles = file.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".atKeys");
			}
		});
		
		List<String> atSigns = new LinkedList<String>();
		for (File keyFile : keyFiles) {
			atSigns.add(keyFile.getName().substring(0, keyFile.getName().indexOf("_")));
		}
		
		System.out.println(atSigns.toString());
		return atSigns.toString();
	}
	
	public static void main(String[] args) {
		 new CommandLine(new ListAtSignsCommand()).execute(args);
   }
}
