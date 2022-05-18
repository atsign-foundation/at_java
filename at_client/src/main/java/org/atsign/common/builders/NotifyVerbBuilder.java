package org.atsign.common.builders;

import java.util.UUID;

public class NotifyVerbBuilder extends KeyBuilders {
	  /// id for each notification.
	  String id = UUID.randomUUID().toString();

	  /// Key that represents a user's information. e.g phone, location, email etc.,
	  String atKey;

	  /// Value of the key typically in string format. Images, files, etc.,
	  /// must be converted to unicode string before storing.
	  String value;

	  /// AtSign to whom [atKey] has to be shared.
	  String sharedWith;

	  /// AtSign of the client user calling this builder.
	  String sharedBy;

	  /// if [isPublic] is true, then [atKey] is accessible by all atSigns.
	  /// if [isPublic] is false, then [atKey] is accessible either by [sharedWith] or [sharedBy]
	  boolean isPublic = false;

	  /// time in milliseconds after which [atKey] expires.
	  int ttl;

	  /// time in milliseconds after which a notification expires.
	  int ttln;

	  /// time in milliseconds after which [atKey] becomes active.
	  int ttb;

	  /// time in milliseconds to refresh [atKey].
	  int ttr;

	  OperationEnum operation;

	  /// priority of the notification
	  PriorityEnum priority;

	  /// strategy in processing the notification
	  StrategyEnum strategy;

	  /// type of notification
	  MessageTypeEnum messageType = MessageTypeEnum.text;

	  /// The notifier of the notification. Defaults to system.
	  String notifier = "SYSTEM";

	  /// Latest N notifications to notify. Defaults to 1
	  int latestN;

	  boolean ccd;

	  /// Will be set only when [sharedWith] is set. Will be encrypted using the public key of [sharedWith] atsign
	  String sharedKeyEncrypted;

	  /// checksum of the the public key of [sharedWith] atsign. Will be set only when [sharedWith] is set.
	  String pubKeyChecksum;

	 
	  
	  public void setId(String id) {
		this.id = id;
	}


	public void setAtKey(String atKey) {
		this.atKey = atKey;
	}


	public void setValue(String value) {
		this.value = value;
	}


	public void setSharedWith(String sharedWith) {
		this.sharedWith = sharedWith;
	}


	public void setSharedBy(String sharedBy) {
		this.sharedBy = sharedBy;
	}


	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}


	public void setTtl(int ttl) {
		this.ttl = ttl;
	}


	public void setTtln(int ttln) {
		this.ttln = ttln;
	}


	public void setTtb(int ttb) {
		this.ttb = ttb;
	}


	public void setTtr(int ttr) {
		this.ttr = ttr;
	}


	public void setOperation(OperationEnum operation) {
		this.operation = operation;
	}


	public void setPriority(PriorityEnum priority) {
		this.priority = priority;
	}


	public void setStrategy(StrategyEnum strategy) {
		this.strategy = strategy;
	}


	public void setMessageType(MessageTypeEnum messageType) {
		this.messageType = messageType;
	}


	public void setNotifier(String notifier) {
		this.notifier = notifier;
	}


	public void setLatestN(int latestN) {
		this.latestN = latestN;
	}


	public void setCcd(boolean ccd) {
		this.ccd = ccd;
	}


	public void setSharedKeyEncrypted(String sharedKeyEncrypted) {
		this.sharedKeyEncrypted = sharedKeyEncrypted;
	}


	public void setPubKeyChecksum(String pubKeyChecksum) {
		this.pubKeyChecksum = pubKeyChecksum;
	}


	public String build() {
	    String command = String.format("notify:id:%s:",id);

	    if (operation != null) {
	      command += String.format("%s:", operation);
	    }
	    if (messageType != null) {
	      command += String.format("messageType:%s:", messageType);
	    }
	    if (priority != null) {
	      command += String.format("priority:%s:", priority);
	    }
	    if (strategy != null) {
	    	command += String.format("strategy:%s:", strategy);
	    }
	    if (latestN != 0) {
	      command += String.format("latestN:%s:", latestN);
	    }
	    command += String.format("notifier:%s:", notifier);
	    

	    if (sharedWith != null) {
	      command += String.format("%s:", sharedWith);
	    }

	    if (isPublic) {
	      command += "public:";
	    }
	    command += atKey;

	    if (sharedBy != null) {
	      command += String.format("%s", sharedBy);
	    }
	    if (value != null) {
	    	command += String.format(":%s", value);
	    }

	    return command + "\n";
	  }

	 
	  boolean checkParams() {
	    boolean isValid = true;
	    if ((atKey == null) || (isPublic == true && sharedWith != null)) {
	      isValid = false;
	    }
	    return isValid;
	  }
	}

enum OperationEnum { update, delete, append, remove };
enum PriorityEnum { low, medium, high };
enum StrategyEnum { all, latest };
enum MessageTypeEnum { key, text };
