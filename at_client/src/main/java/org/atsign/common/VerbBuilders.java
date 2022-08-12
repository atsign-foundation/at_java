package org.atsign.common;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * Contains builders that build commands that are accepted by a secondary server
 *
 */
public class VerbBuilders {
	
	public interface VerbBuilder {
		/// Build the command to be sent to remote secondary for execution.
		String build();
	}

	public static class FromVerbBuilder implements VerbBuilder {
		private String atSignStr;

		public FromVerbBuilder(String atSignStr) {
			this.atSignStr = atSignStr;
		}

		@Override
		public String build() {
			return "from:" + atSignStr;
		}
	}

	public static class CRAMVerbBuilder implements VerbBuilder {

		private String digest;

		public CRAMVerbBuilder(String digest) {
			this.digest = digest;
		}

		@Override
		public String build() {
			String s = "cram:" + digest;
			return s;
		}
	}

	public static class PKAMVerbBuilder implements VerbBuilder {

		private String digest; // digest the challenge string given by the from verb

		public PKAMVerbBuilder(String digest) {
			this.digest = digest;
		}

		@Override
		public String build() {
			String s = "pkam:" + digest;
			return s;
		}

	}

	public static class LlookupVerbBuilder implements VerbBuilder {

		public enum Type {
			NONE, // llookup:<fullKeyName>
			METADATA, // llookup:meta:<fullKeyName>
			ALL, // llookup:all:<fullKeyName>
		}

		private String fullKeyName;

		private Type type;

		public LlookupVerbBuilder(String fullKeyName) {
			this(fullKeyName, Type.NONE);
		}

		public LlookupVerbBuilder(String fullKeyName, Type type) {
			this.fullKeyName = fullKeyName; // eg: "cached:public:test@bob"
			this.type = type;	
		}

		public void setFullKeyName(String fullKeyName) {
			this.fullKeyName = fullKeyName;
		}

		public void setType(Type type) {
			this.type = type;
		}

		@Override
		public String build() {
			String s = "llookup:";
			switch (type) {
				case METADATA:
					s += "meta:";
					break;
				case ALL:
					s += "all:";
					break;
				default:
					break;
			}
			s += this.fullKeyName;
			return s; // eg: "llookup:meta:cached:public:test@bob"
			
		}

	}

	public static class LookupVerbBuilder implements VerbBuilder {
		
		public enum Type {
			NONE, // lookup:<fullKeyName>
			METADATA, // lookup:meta:<fullKeyName>
			ALL, // lookup:all:<fullKeyName>
		}
		
		private String fullKeyName; // eg: "cached:public:test@bob"
		private Type type;
		
		public LookupVerbBuilder(String fullKeyName) {
			this(fullKeyName, Type.NONE);
		}
		
		public LookupVerbBuilder(String fullKeyName, Type type) {
			this.fullKeyName = fullKeyName;
			this.type = type;	
		}
		
		public void setFullKeyName(String fullKeyName) {
			this.fullKeyName = fullKeyName;
		}
		
		public void setType(Type type) {
			this.type = type;
		}
		
		@Override
		public String build() {
			String s = "lookup:";
			switch (type) {
				case METADATA:
					s += "meta:";
					break;
				case ALL:
					s += "all:";
					break;
				default:
					break;
			}
			s += this.fullKeyName;
			return s; // eg: "lookup:meta:cached:public:test@bob"
		}
	}

	public static class PlookupVerbBuilder implements VerbBuilder {

		public enum Type {
			NONE, // just get the data
			METADATA, // get the metadata but no data
			ALL, // get the data and metadata
		}
		
		private String fullKeyName;
		private Type type;

		public PlookupVerbBuilder(String fullKeyName) {
			this(fullKeyName, Type.NONE);
		}

		public PlookupVerbBuilder(String fullKeyName, Type type) {
			this.fullKeyName = fullKeyName;
			this.type = type;
		}

		public void setFullKeyName(String fullKeyName) {
			this.fullKeyName = fullKeyName;
		}

		public void setType(Type type) {
			this.type = type;
		}

		@Override
		public String build() {
			String s = "plookup:";
			switch(type) {
				case METADATA:
					s += "meta:";
					break;
				case ALL:
					s += "all:";
					break;
				default:
					break;
			}
			s += this.fullKeyName;
			return s; // e.g: "plookup:meta:@alice:test@bob"
		}

	}

	public static class ScanVerbBuilder implements VerbBuilder {
		
		// Regex to filter the keys
		private String regex;
		
		// Scans the keys shared by <code>forAtSign</code>
		private String fromAtSign;

		// Scans for hidden keys (showHidden:true)
		private boolean showHidden;

		// regex: no
		// fromAtSign: no
		// showHidden: no
		public ScanVerbBuilder() {
			this("", "", false);
		}

		// regex: yes
		// fromAtSign: yes
		// showHidden: no
		public ScanVerbBuilder(String regex, String fromAtSign) {
			this(regex, fromAtSign, false);
		}

		// regex: yes
		// fromAtSign: no
		// showHidden: yes
		public ScanVerbBuilder(String regex, boolean showHidden) {
			this(regex, "", showHidden);
		}

		// regex: no
		// fromAtSign: no
		// showHidden: yes
		public ScanVerbBuilder(boolean showHidden) {
			this("", "", showHidden);
		}

		// regex: yes
		// fromAtSign: yes
		// showHidden: yes
		public ScanVerbBuilder(String regex, String fromAtSign, boolean showHidden) {
			this.regex = regex;
			this.fromAtSign = fromAtSign;
			this.showHidden = showHidden;
		}

	    public void setRegex(String regex) {
			this.regex = regex;
		}

		public void setFromAtSign(String fromAtSign) {
			this.fromAtSign = fromAtSign;
		}

		public void setShowHidden(boolean showHidden) {
			this.showHidden = showHidden;
		}

		// r'^scan$|scan(:(?<forAtSign>@[^:@\s]+))?(:page:(?<page>\d+))?( (?<regex>\S+))?$';
		public String build() {
			
			String command = "scan";
			
			if(showHidden) {
				command += ":showHidden:true";
			}

			if(fromAtSign != null && !StringUtils.isBlank(fromAtSign)) {
				command += ":" + fromAtSign;
			}
			
			if(regex != null && !StringUtils.isBlank(regex)) {
				command += " " + regex;
			}
			
			return command;
		}
	}
	
	public static class NotifyTextVerbBuilder implements VerbBuilder {
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
	
	public static class NotifyKeyChangeBuilder implements VerbBuilder {
		
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
		// ttr of -1 indicated cache forever without a need to refresh the value again.
		// Any other positive value indicates time after which the value needs to be refreshed
		private final int defaultTTRValue = -2;		
		private long ttr = defaultTTRValue;
		
		
		public void setOperation(String operation) {
			this.operation = operation;
		}

		// This is optional if the key is fully formed. i.e. key in the format @recipientAtSign:phone@senderAtSign
		public void setRecipientAtSign(String recipientAtSign) {
			this.recipientAtSign = recipientAtSign;
		}


		public void setKey(String key) {
			this.key = key;
		}

		// This is optional if the key is fully formed. i.e. key in the format @recipientAtSign:phone@senderAtSign
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
			
			if(key == null || StringUtils.isBlank(key)) {
				throw new IllegalArgumentException("key cannot be null or empty");
			}
			
			
			if(!operation.equals("update") && !operation.equals("delete")) {
				throw new IllegalArgumentException("Only 'update' and 'delete' are allowed for operation");
			}
			
			if(ttr < -1 && ttr != defaultTTRValue) {
				throw new IllegalArgumentException("Invalid value for ttr. Only -1 and positive numbers are allowed");
			}
			
			if(ttr != defaultTTRValue && (value == null || StringUtils.isBlank(value))) {
				throw new IllegalArgumentException("When the ttr is specified value cannot be null or empty");
			}
			
			String command = "notify:" + operation + ":messageType:key:";
			
			// append ttr
			if(ttr != defaultTTRValue) {
				command += "ttr:" + ttr + ":";
			}
			
			// append recipients @sign if it is not part of the key already
			if(recipientAtSign != null && !StringUtils.isBlank(recipientAtSign)) {
				
				if(!recipientAtSign.startsWith("@")) {
					recipientAtSign = "@" + recipientAtSign;
				}
				
				command += recipientAtSign + ":";
			}
			
			// append the key
			command += key;
			
			if(senderAtSign != null && !StringUtils.isBlank(senderAtSign)) {
				
				if(!senderAtSign.startsWith("@")) {
					senderAtSign = "@" + senderAtSign;
				}
				
				command += senderAtSign;
			}
			
			if(value != null && !StringUtils.isBlank(value)) {
				command += ":" + value;
			}
			
			return command;
		}
	}
	
	public static class NotificationStatusVerbBuilder implements VerbBuilder {
		
		private String notificationId;
		
		 public void setNotificationId(String notificationId) {
			this.notificationId = notificationId;
		}

		//notify:status:(?<notificationId>\S+)$';
		public String build() {
			
			if(notificationId == null || StringUtils.isBlank(notificationId)) {
				throw new IllegalArgumentException("notificationId cannot be null or empty");
			}
			
			return "notify:status:" + notificationId;
		}
	}
	
}
