package org.atsign.client.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;



@Command(name = "keys", description = "Shows list of keys")
public class ListKeysCommand implements Callable<String>{

	@Override
	public String call() throws Exception {
		System.out.println("test keys");
		return null;
	}
	
}