package org.atsign.client.cli;

import java.time.OffsetDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.atsign.common.AtSign;
import org.atsign.common.NotificationResult;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;

/**
 * A command-line utility to notify an @sign of a text or a Change
 */

@Command(name = "notify", description = "Sends the text given as a notification to an @sign", requiredOptionMarker = '*')
public class Notify extends OtherAtSignCommand implements Callable<NotificationResult>{
	
	@Option(names = {"-t", "--text"}, description = "Text to be notified", required = true)
    private String text;
	

	@Override
	public NotificationResult call() throws Exception {
		assert(text != null);
		assert(yourAtSign != null);
		assert(otherAtSign != null);
		assert(rootUrl != null);
		System.out.println(text + "-" + yourAtSign + "-" + otherAtSign + "-" + rootUrl);
		return notifyText();
		
	}
	
	NotificationResult notifyText() {
		
		super.initialize();
        try {
        	System.out.println(OffsetDateTime.now() + " | calling atClient.notify()");
        	NotificationResult result = atClient.notifyText(text, otherAtSign).get();
            System.out.println(OffsetDateTime.now() + " | notify response : " + result);
            return result;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to share : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
		return null;
	}
	
	

	// this example implements Callable, so parsing, error handling and handling user
    // requests for usage help or version help can be done with one line of code.
    public static void main(String... args) {
        int exitCode = new CommandLine(new Notify()).execute(args);
        System.exit(exitCode);
    }
	
	
}


class AtSignConverter implements ITypeConverter<AtSign> {
    public AtSign convert(String value) throws Exception {
        return new AtSign(value);
    }
}



