package org.atsign.client.cli;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

import picocli.CommandLine.Option;

/**
 * 
 * Remote command is a command that requires a connection to the remote secondary.
 */
public abstract class OtherAtSignCommand {
	
	@Option(names = {"-u", "--uSign"}, description = "Your @sign", converter = AtSignConverter.class, required = true)
	protected AtSign yourAtSign;
	
	@Option(names = {"-o", "--oSign"}, description = "The other @sign to whom the notification needs to be sent", converter = AtSignConverter.class, required = true)
	protected AtSign otherAtSign;
	
	@Option(names = {"-r", "--root"}, description = "URL of the the root server in the host:port format. Ex: vip.ve.atsign.zone:64", required = false)
	protected String rootUrl = "root.atsign.wtf:64";
	
	protected AtClient atClient;
	
	public void initialize() {
		
		// Let's also look up the other one before we do anything, just in case
        try {
            new AtRootConnection(rootUrl).lookupAtSign(otherAtSign);
        } catch (Exception e) {
            System.err.println("Failed to look up remote secondary for " + otherAtSign + " : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }

        AtClient atClient = null;
        
        try {
            atClient = AtClient.withRemoteSecondary(rootUrl, yourAtSign);
        } catch (AtException e) {
            System.err.println("Failed to create AtClientImpl : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
        
        this.atClient = atClient;
        
	}
	
	
}
