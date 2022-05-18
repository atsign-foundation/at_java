package org.atsign.client.cli;

import java.time.OffsetDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.atsign.common.Keys;
import org.atsign.common.builders.KeyBuilders.SharedKeyBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * A command-line interface half-example half-utility to share something with
 * another atSign
 */
@Command(name = "share", description = "Shares a key with the other @sign", requiredOptionMarker = '*')
public class Share extends OtherAtSignCommand implements Callable<String> {

	@Option(names = { "-k", "--key" }, description = "Key to share", required = true)
	String keyName;

	@Option(names = { "-v", "--value" }, description = "Value share", required = true)
	String toShare;

	@Option(names = {
			"-ttr" }, description = "Value in milliseconds. Specifying a value for this makes the key cacheable on the other @sign. Cached value will be refreshed with in the value specified for the ttr.", required = false)
	int ttr;

	@Override
	public String call() throws Exception {
		share();
		return "Value has been shared successfully";

	}

	void share() {
		super.initialize();

		try {
			SharedKeyBuilder sharedKeyBuilder = new SharedKeyBuilder(yourAtSign, otherAtSign);
			sharedKeyBuilder = sharedKeyBuilder.cache(ttr, true);
			sharedKeyBuilder = sharedKeyBuilder.key(keyName);
			Keys.SharedKey sharedKey = sharedKeyBuilder.build();

			System.out.println(OffsetDateTime.now() + " | calling atClient.put()");
			String putResponse = atClient.put(sharedKey, toShare).get();
			System.out.println(OffsetDateTime.now() + " | put response : " + putResponse);
		} catch (InterruptedException | ExecutionException e) {
			System.err.println("Failed to share : " + e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Share()).execute(args);
		System.exit(exitCode);
	}

}
