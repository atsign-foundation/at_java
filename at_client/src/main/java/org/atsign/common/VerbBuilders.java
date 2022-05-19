package org.atsign.common;

/**
 * 
 * Contains builders that builds commands that are accepted by a secondary server
 *
 */
public class VerbBuilders {
	
	public interface VerbBuilder {
		/// Build the command to be sent to remote secondary for execution.
		String build();
	}
	
	public class ScanVerbBuilder implements VerbBuilder {
		
		// Regex to filter the keys
		private String regex;
		
		// Scans the keys shared by <code>forAtSign</code>
		private String fromAtSign;
		
		
	    public void setRegex(String regex) {
			this.regex = regex;
		}


		public void setFromAtSign(String fromAtSign) {
			this.fromAtSign = fromAtSign;
		}


		// r'^scan$|scan(:(?<forAtSign>@[^:@\s]+))?(:page:(?<page>\d+))?( (?<regex>\S+))?$';
		public String build() {
			
			String command = "scan";
			
			if(fromAtSign != null && !fromAtSign.isBlank()) {
				command += ":" + fromAtSign;
			}
			
			if(regex != null && !regex.isBlank()) {
				command += " " + regex;
			}
			
			return command;
		}
	}
	
	public class NotifyTextVerbBuilder implements VerbBuilder {
		//notify:((?<operation>update|delete):)?(messageType:(?<messageType>key|text):)?(priority:(?<priority>low|medium|high):)?(strategy:(?<strategy>all|latest):)?(latestN:(?<latestN>\d+):)?(notifier:(?<notifier>[^\s:]+):)?(ttln:(?<ttln>\d+):)?(ttl:(?<ttl>\d+):)?(ttb:(?<ttb>\d+):)?(ttr:(?<ttr>(-)?\d+):)?(ccd:(?<ccd>true|false):)?(@(?<forAtSign>[^@:\s]*)):(?<atKey>[^:@]((?!:{2})[^@])+)(@(?<atSign>[^@:\s]+))?(:(?<value>.+))?$	
		
		private String recipientAtSign;
		private String text;
		
	
		public void setRecipientAtSign(String recipientAtSign) {
			this.recipientAtSign = recipientAtSign;
		}


		public void setText(String text) {
			this.text = text;
		}


		public String build() {
			
			if(recipientAtSign == null || recipientAtSign.isEmpty()) {
				throw new IllegalArgumentException("recipientAtSign cannot be null or empty");
			}
			
			if(text == null || text.isEmpty()) {
				throw new IllegalArgumentException("text cannot be null or empty");
			}
			
			if(!recipientAtSign.startsWith("@")) {
				recipientAtSign = "@" + recipientAtSign;
			}
			
			return "notify:messageType:text:" + recipientAtSign + ":" + text;
		}
	}
	
	public class NotifyKeyChangeBuilder implements VerbBuilder {
		
		// Only allowed values are "update" or "delete"
		private String operation = "update";
		// Optional if key contains the recipient
		private String recipientAtSign;
		private String key;
		// Optional if key contains the owner
		private String senderAtSign;
		// Value to be notified when the change needs to be cached.
		private String value;
		
		// If the key has to be cached by the other @sign
		// ttr of -1 indicated cache for ever without a need to refresh the value again.
		// Any other positive value indicates time after which the value needs to be refreshed
		private final int defaultTTRValue = -2;		
		private long ttr = defaultTTRValue;
		
		
		public void setOperation(String operation) {
			this.operation = operation;
		}

		// This is optional if the key is fully formed. i.e key in the format @recipientAtSign:phone@senderAtSign
		public void setRecipientAtSign(String recipientAtSign) {
			this.recipientAtSign = recipientAtSign;
		}


		public void setKey(String key) {
			this.key = key;
		}

		// This is optional if the key is fully formed. i.e key in the format @recipientAtSign:phone@senderAtSign
		public void setSenderAtSign(String senderAtSign) {
			this.senderAtSign = senderAtSign;
		}


		public void setValue(String value) {
			this.value = value;
		}


		public void setTtr(long ttr) {
			this.ttr = ttr;
		}


		//notify:((?<operation>update|delete):)?(messageType:(?<messageType>key|text):)?(priority:(?<priority>low|medium|high):)?(strategy:(?<strategy>all|latest):)?(latestN:(?<latestN>\d+):)?(notifier:(?<notifier>[^\s:]+):)?(ttln:(?<ttln>\d+):)?(ttl:(?<ttl>\d+):)?(ttb:(?<ttb>\d+):)?(ttr:(?<ttr>(-)?\d+):)?(ccd:(?<ccd>true|false):)?(@(?<forAtSign>[^@:\s]*)):(?<atKey>[^:@]((?!:{2})[^@])+)(@(?<atSign>[^@:\s]+))?(:(?<value>.+))?$	
		public String build() {
			
			if(key == null || key.isBlank()) {
				throw new IllegalArgumentException("key cannot be null or empty");
			}
			
			
			if(!operation.equals("update") && !operation.equals("delete")) {
				throw new IllegalArgumentException("Only 'update' and 'delete' are allowed for operation");
			}
			
			if(ttr < -1 && ttr != defaultTTRValue) {
				throw new IllegalArgumentException("Invalid value for ttr. Only -1 and positive numbers are allowed");
			}
			
			if(ttr != defaultTTRValue && (value == null || value.isBlank())) {
				throw new IllegalArgumentException("When the ttr is specified value cannot be null or empty");
			}
			
			String command = "notify:" + operation + ":messageType:key:";
			
			// append ttr
			if(ttr != defaultTTRValue) {
				command += "ttr:" + ttr + ":";
			}
			
			// append recipients @sign if it not part of the key already
			if(recipientAtSign != null && !recipientAtSign.isBlank()) {
				
				if(!recipientAtSign.startsWith("@")) {
					recipientAtSign = "@" + recipientAtSign;
				}
				
				command += recipientAtSign + ":";
			}
			
			// append the key
			command += key;
			
			if(senderAtSign != null && !senderAtSign.isBlank()) {
				
				if(!senderAtSign.startsWith("@")) {
					senderAtSign = "@" + senderAtSign;
				}
				
				command += senderAtSign;
			}
			
			if(value != null && !value.isBlank()) {
				command += ":" + value;
			}
			
			return command;
		}
	}
	
	public class NotificationStatusVerbBuilder implements VerbBuilder {
		 //notify:status:(?<notificationId>\S+)$';
		public String build() {
			return null;
		}
	}
	
}
