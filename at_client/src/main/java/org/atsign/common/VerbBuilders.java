package org.atsign.common;

import org.apache.commons.lang3.StringUtils;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SharedKey;

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
		private String atSignStr; // the atSign that we are authenticating with (e.g. atSignStr.equals("@alice") <=> true) [required]

		public void setAtSign(String atSignStr) {
			this.atSignStr = atSignStr;
		}

		@Override
		public String build() {
			atSignStr = AtSign.formatAtSign(atSignStr);
			if(atSignStr == null || atSignStr.isEmpty()) {
				throw new IllegalArgumentException("atSignStr cannot be null or empty");
			}
			return "from:" + atSignStr;
		}
	}

	public static class CRAMVerbBuilder implements VerbBuilder {

		// chlallenge response authentication method

		private String digest; // the digest to use for authentication, encrypt the challenge (given by the from verb) to get the digest [required]

		public void setDigest(String digest) {
			this.digest = digest;
		}

		@Override
		public String build() {
			String s = "cram:" + digest;
			return s;
		}
	}

	public static class POLVerbBuilder implements VerbBuilder {

		// proof of life

		@Override
		public String build() {
			return "pol";
		}
	}

	public static class PKAMVerbBuilder implements VerbBuilder {
		
		// public key authentication method

		private String digest; // digest the challenge string given by the from verb [required]

		public void setDigest(String digest) {
			this.digest = digest;
		}

		@Override
		public String build() {
			String s = "pkam:" + digest;
			return s;
		}

	}

	public static class UpdateVerbBuilder implements VerbBuilder {

		/// Update the value (and metadata optionally) of a key.

		// =======================================
		// AtKey name details
		// =======================================
		private String key; // e.g. "test", "location", "email" [required]
		private String sharedBy; // e.g. "@alice" [required]
		private String sharedWith = null; // e.g. "@bob" 
		private Boolean isHidden = null; // if true, adds _ at the beginning of the fullKeyName
		private Boolean isPublic = null; //   /// if [isPublic] is true, then [atKey] is accessible by all atSigns, if [isPublic] is false, then [atKey] is accessible either by [sharedWith] or [sharedBy]
		private Boolean isCached = false; // if true, will add "cached:" to the fullKeyName
		private Integer ttl = null; // time to live in milliseconds (how long AtKey will exist) (0 by default)
		private Integer ttb = null; // time to birth in milliseconds (how long it will take for AtKey to exist) (0 by default)
		private Integer ttr = null; // time to refresh in milliseconds (how long it will take for AtKey to refresh)
		private Boolean ccd = null; // if true, cached keys will be deleted if the original key is deleted
		private Boolean isBinary = null; // if true, the value contains binary data
		private Boolean isEncrypted = null; // if true, the value is encrypted with some encryption key
		private String dataSignature = null; // usually public data is signed with the private key to prove that the data is authentic
		private String sharedKeyEnc = null; // will be set only when [sharedWith] is set. Will be encrypted using the public key of [sharedWith] atsign
		private String pubKeyCS = null; // checksum of the public of of [sharedWith] atSign. Will be set only when [sharedWith] is set.
		private String encoding = null; // indicates if public data is encoded. If the public data contains a new line character, the data will be encoded and the encoding will be set to given type of encoding

		private Object value; // the value to set [required]

		public void setKeyName(String keyName) {
			this.key = keyName;
		}

		public void setSharedBy(String sharedBy) {
			this.sharedBy = sharedBy;
		}

		public void setSharedWith(String sharedWith) {
			this.sharedWith = sharedWith;
		}

		public void setIsHidden(Boolean isHidden) {
			this.isHidden = isHidden;
		}

		public void setIsPublic(Boolean isPublic) {
			this.isPublic = isPublic;
		}

		public void setIsCached(Boolean isCached) {
			this.isCached = isCached;
		}

		public void setTtl(Integer ttl) {
			this.ttl = ttl;
		}

		public void setTtb(Integer ttb) {
			this.ttb = ttb;
		}

		public void setTtr(Integer ttr) {
			this.ttr = ttr;
		}

		public void setCcd(Boolean ccd) {
			this.ccd = ccd;
		}

		public void setIsBinary(boolean isBinary) {
			this.isBinary = isBinary;
		}

		public void setIsEncrypted(boolean isEncrypted) {
			this.isEncrypted = isEncrypted;
		}

		public void setDataSignature(String dataSignature) {
			this.dataSignature = dataSignature;
		}

		public void setSharedKeyEnc(String sharedKeyEnc) {
			this.sharedKeyEnc = sharedKeyEnc;
		}

		public void setPubKeyCS(String pubKeyCS) {
			this.pubKeyCS = pubKeyCS;
		}

		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public void setMetadata(Metadata metadata) {
			this.isHidden = metadata.isHidden;
			this.isPublic = metadata.isPublic;
			this.isCached = metadata.isCached;
			this.ttl = metadata.ttl;
			this.ttb = metadata.ttb;
			this.ttr = metadata.ttr;
			this.ccd = metadata.ccd;
			this.isBinary = metadata.isBinary;
			this.isEncrypted = metadata.isEncrypted;
			this.dataSignature = metadata.dataSignature;
			this.sharedKeyEnc = metadata.sharedKeyEnc;
			this.pubKeyCS = metadata.pubKeyCS;
			this.encoding = metadata.encoding;
		}

		public void with(AtKey atKey, Object value) {
			setKeyName(atKey.name);
			setSharedBy(atKey.sharedBy.toString());
			if(atKey.sharedWith != null && !atKey.sharedWith.toString().isEmpty()) setSharedWith(atKey.sharedWith.toString());
			setIsCached(atKey.metadata.isCached);
			setIsHidden(atKey.metadata.isHidden);
			setIsPublic(atKey.metadata.isPublic);
			setMetadata(atKey.metadata);
			setValue(value);
		}

		@Override
		public String build() {
			if(key == null || key.isEmpty() || sharedBy == null || sharedBy.isEmpty() || value == null || value.toString().isEmpty()) {
				throw new IllegalArgumentException("keyName, sharedBy, and value cannot be null or empty");
			}
			String fullKeyName = buildAtKeyStr();
			String metadata = buildMetadataStr();
			String s = "update" + metadata + ":" + fullKeyName + " " + value.toString();
			return s;
		}

		private String buildAtKeyStr() {
			String s = "";
			if(isHidden != null && isHidden) {
				s += "_";
			}
			if(isCached != null && isCached) {
				s += "cached:";
			}
			if(isPublic != null && isPublic) {
				s += "public:";
			}
			if(sharedWith != null && !sharedWith.isEmpty()) {
				s += AtSign.formatAtSign(sharedWith) + ":";
			}
			s += key;
			s += AtSign.formatAtSign(sharedBy);
			return s;
		}

		private String buildMetadataStr() {
			Metadata metadata = new Metadata();
			metadata.ttl = ttl;
			metadata.ttb = ttb;
			metadata.ttr = ttr;
			metadata.ccd = ccd;
			metadata.isBinary = isBinary;
			metadata.isEncrypted = isEncrypted;
			metadata.dataSignature = dataSignature;
			metadata.sharedKeyEnc = sharedKeyEnc;
			metadata.pubKeyCS = pubKeyCS;
			metadata.encoding = encoding;
			return metadata.toString();
		}
		
	}

	public static class LlookupVerbBuilder implements VerbBuilder {

		public enum Type {
			NONE, // llookup:<fullKeyName>
			METADATA, // llookup:meta:<fullKeyName>
			ALL, // llookup:all:<fullKeyName>
		}

		private String key; // e.g. "test", "location", "email" [required]
		private String sharedBy; // e.g. sharedBy atSign "@alice" [required]
		private String sharedWith = null; // e.g. sharedWith atSign "@bob"
		private Boolean isHidden = null; // if true, adds _ at the beginning of the fullKeyName
		private Boolean isPublic = null; // if [isPublic] is true, then [atKey] is accessible by all atSigns and "public:" will be added to the fullKeyName, if [isPublic] is false, then [atKey] is accessible either by [sharedWith] or [sharedBy]
		private Boolean isCached = null; // if true, will add "cached:" to the fullKeyName

		private Type type = Type.NONE;

		public void setKeyName(String key) {
			this.key = key;
		}

		public void setSharedBy(String sharedBy) {
			this.sharedBy = sharedBy;
		}

		public void setSharedWith(String sharedWith) {
			this.sharedWith = sharedWith;
		}

		public void setIsHidden(Boolean isHidden) {
			this.isHidden = isHidden;
		}

		public void setIsPublic(Boolean isPublic) {
			this.isPublic = isPublic;
		}

		public void setIsCached(Boolean isCached) {
			this.isCached = isCached;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public void with(AtKey atKey, LlookupVerbBuilder.Type type) {
			setKeyName(atKey.name);
			setSharedBy(atKey.sharedBy.toString());
			if(atKey.sharedWith != null && !atKey.sharedWith.toString().isEmpty()) setSharedWith(atKey.sharedWith.toString());
			setIsHidden(atKey.metadata.isHidden);
			setIsPublic(atKey.metadata.isPublic);
			setIsCached(atKey.metadata.isCached);
			setType(type);
		}

		@Override
		public String build() {
			if(key == null || key.isEmpty() || sharedBy == null || sharedBy.isEmpty()) {
				throw new IllegalArgumentException("keyName and sharedBy cannot be null or empty");
			}
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
			if(isHidden != null && isHidden) {
				s += "_";
			}
			if(isCached != null && isCached) {
				s += "cached:";
			}
			if(isPublic != null &&isPublic) {
				s += "public:";
			}
			if(sharedWith != null && !sharedWith.isEmpty()) {
				s += AtSign.formatAtSign(sharedWith) + ":";
			}
			s += key;
			s += AtSign.formatAtSign(sharedBy);
			return s; // eg: "llookup:meta:cached:public:test@bob"
			
		}

	}

	public static class LookupVerbBuilder implements VerbBuilder {
		
		// look up a key shared with you in another atSign's secondary
		// the sender atSign (other person) is the sharedBy atSign, while you are the sharedWith atSign.
		
		// Type of Lookup
		public enum Type {
			NONE, // lookup:<fullKeyName>
			METADATA, // lookup:meta:<fullKeyName>
			ALL, // lookup:all:<fullKeyName>
		}
		
		private String key; // key name e.g. "test", "location", "email" [required]
		private String sharedWith; // sharedBy atSign e.g. "@alice" [required] (not your atSign, the atSign of another secondary, get)

		private Type type = Type.NONE;
		
		public void setKeyName(String key) {
			this.key = key;
		}

		public void setSharedWith(String sharedWith) {
			this.sharedWith = sharedWith;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public void with(SharedKey sharedKey, LookupVerbBuilder.Type type) {
			setKeyName(sharedKey.name);
			setSharedWith(sharedKey.sharedWith.toString());
			setType(type);
		}
		
		@Override
		public String build() {
			if(key == null || key.isEmpty() || sharedWith == null || sharedWith.isEmpty()) {
				throw new IllegalArgumentException("keyName and sharedWith cannot be null or empty");
			}
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
			s += this.key;
			s += AtSign.formatAtSign(this.sharedWith);
			return s; // eg: "lookup:meta:test@bob"
		}
	}

	public static class PlookupVerbBuilder implements VerbBuilder {

		// look up a public key in another atSign's secondary
		// e.g. "plookup:publickey@bob" will return the data of the key "public:publickey@bob" in @bob's secondary server (you are @alice, @bob is not you)
		
		// Type of plookup
		public enum Type {
			NONE, // just get the data
			METADATA, // get the metadata but no data (plookup:meta:)
			ALL, // get the data and metadata (plookup:all:)
		}
		
		// AtKey details
		private String key; // key name (e.g. location, test) [required]
		private String sharedBy; // sharedBy atSign ("@bob") [required]

		private Boolean bypassCache = false; // bypass cache (plookup:bypassCache:[true/false]) [optional]

		private Type type = Type.NONE;

		public void setKeyName(String key) {
			this.key = key;
		}

		public void setSharedBy(String sharedBy) {
			this.sharedBy = sharedBy;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public void setBypassCache(Boolean bypassCache) {
			this.bypassCache = bypassCache;
		}

		public void with(PublicKey atKey, PlookupVerbBuilder.Type type) {
			setKeyName(atKey.name);
			setSharedBy(atKey.sharedBy.toString());
			setType(type);
		}

		@Override
		public String build() {
			if(this.key == null || this.key.isEmpty() || this.sharedBy == null || this.sharedBy.isEmpty()) {
				throw new IllegalArgumentException("key or sharedBy is null or empty");
			}
			String s = "plookup:";
			if(this.bypassCache != null && this.bypassCache) {
				s += "bypassCache:true:";
			}
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
			s += this.key;
			s += AtSign.formatAtSign(this.sharedBy);

			return s; // e.g: "plookup:meta:@alice:test@bob"
		}

	}

	public static class DeleteVerbBuilder implements VerbBuilder {

		private String key; // e.g. "test", "location", "email" [required]
		private String sharedBy; // e.g. sharedBy atSign "@alice" [required]
		private String sharedWith = ""; // e.g. sharedWith atSign "@bob"
		private Boolean isHidden = false;
		private Boolean isPublic = false; // if [isPublic] is true, then [atKey] is accessible by all atSigns and "public:" will be added to the fullKeyName, if [isPublic] is false, then [atKey] is accessible either by [sharedWith] or [sharedBy]
		private Boolean isCached = false; // if true, will add "cached:" to the fullKeyName

		public void setKeyName(String keyName) {
			this.key = keyName;
		}

		public void setSharedBy(String sharedBy) {
			this.sharedBy = sharedBy;
		}

		public void setSharedWith(String sharedWith) {
			this.sharedWith = sharedWith;
		}

		public void setIsHidden(Boolean isHidden) {
			this.isHidden = isHidden;
		}

		public void setIsPublic(Boolean isPublic) {
			this.isPublic = isPublic;
		}

		public void setIsCached(Boolean isCached) {
			this.isCached = isCached;
		}

		public void with(AtKey atKey) {
			setKeyName(atKey.name);
			setSharedBy(atKey.sharedBy.toString());
			if(atKey.sharedWith != null && !atKey.sharedWith.toString().isEmpty()) setSharedWith(atKey.sharedWith.toString());
			setIsHidden(atKey.metadata.isHidden);
			setIsPublic(atKey.metadata.isPublic);
			setIsCached(atKey.metadata.isCached);
		}

		@Override
		public String build() {
			if(key == null || sharedBy == null) {
				throw new IllegalArgumentException("key or sharedBy is null. These are required fields");
			}

			String s = "delete:";
			if(isHidden) {
				s += "_";
			}
			if(isCached) {
				s += "cached:";
			}
			if(isPublic) {
				s += "public:";
			}
			if(sharedWith != null && !sharedWith.isEmpty()) {
				s += AtSign.formatAtSign(sharedWith) + ":";
			}
			s += key;
			s += AtSign.formatAtSign(sharedBy);
			return s; // eg: "delete:cached:public:test@bob"
		}

	}

	public static class ScanVerbBuilder implements VerbBuilder {
		
		// Regex to filter the keys
		private String regex;
		
		// Scans the keys shared by forAtSign
		private String fromAtSign;

		// Scans for hidden keys (showHidden:true)
		private boolean showHidden = false;

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
