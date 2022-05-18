package org.atsign.client.api.impl.clients;

import org.atsign.client.api.Secondary;
import org.atsign.client.api.Secondary.Response;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.NotificationResult;
import org.atsign.common.builders.NotifyVerbBuilder;

public class NotificationImpl {
	
	private Secondary secondary;

	public NotificationImpl(Secondary secondary) {
		this.secondary = secondary;
	}
	
	public NotificationResult notifyText(String text, AtSign self, AtSign other) {
		NotifyVerbBuilder builder = new NotifyVerbBuilder();
		builder.setAtKey(text);
		builder.setSharedBy(self.toString());
		builder.setSharedWith(other.toString());
		String command = builder.build();
		System.out.println(command);
		
		try {
			Response response = secondary.executeCommand(command, true);
			return new NotificationResult(response.data);	
		} catch (AtException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
}
