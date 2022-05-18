package org.atsign.common;

/**
 * 
 * Contains builders that builds commands that are accepted by a secondary server
 *
 */
public class VerbBuilders {
	
	public interface VerbBuilder {
		/// Build the command to be sent to remote secondary for execution.
		String buildCommand();
	}
	
	public class ScanVerbBuilder implements VerbBuilder {
		// Regex to filter the keys
		String regex;
		
		// Scans the keys shared by <code>forAtSign</code>
		String forAtSign;
		
	    // r'^scan$|scan(:(?<forAtSign>@[^:@\s]+))?(:page:(?<page>\d+))?( (?<regex>\S+))?$';
		public String buildCommand() {
			return null;
		}
	}
	
	public class NotifyVerbBuilder implements VerbBuilder {
		//notify:((?<operation>update|delete):)?(messageType:(?<messageType>key|text):)?(priority:(?<priority>low|medium|high):)?(strategy:(?<strategy>all|latest):)?(latestN:(?<latestN>\d+):)?(notifier:(?<notifier>[^\s:]+):)?(ttln:(?<ttln>\d+):)?(ttl:(?<ttl>\d+):)?(ttb:(?<ttb>\d+):)?(ttr:(?<ttr>(-)?\d+):)?(ccd:(?<ccd>true|false):)?(@(?<forAtSign>[^@:\s]*)):(?<atKey>[^:@]((?!:{2})[^@])+)(@(?<atSign>[^@:\s]+))?(:(?<value>.+))?$	
		public String buildCommand() {
			return null;
		}
	}
	
	public class NotificationStatusVerbBuilder implements VerbBuilder {
		 //notify:status:(?<notificationId>\S+)$';
		public String buildCommand() {
			return null;
		}
	}
	
}
