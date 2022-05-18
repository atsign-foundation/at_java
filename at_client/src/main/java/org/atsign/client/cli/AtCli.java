package org.atsign.client.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;


@Command(
		name = "@",	
		  subcommands = {
			  ListAtSignsCommand.class,
		      SwitchAtSignCommand.class,
		      ShowCurruntAtSignCommand.class
		  }
		)
public class AtCli implements Callable<Integer>{
	
	@Override
	public Integer call() throws Exception {
		System.out.println("Subcommand needed: 'list', 'switch' or 'whoami'");
		return 0;
	}
	
	public static void main(String[] args) {
		 new CommandLine(new AtCli()).execute(args);
    }
}