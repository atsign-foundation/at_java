package org.atsign.client.impl.commands;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * Models optional parameters to the monitor command.
 */
@Data
@Builder
@Accessors(fluent = true)
public class MonitorOptions {

  /**
   * strict server will only send notifications which match the regex; no other 'control'
   * notifications such as statsNotifications will be sent on this connection unless they match
   * the regex
   */
  private boolean strict;

  /**
   * server will only send notifications received at or after that timestamp
   */
  private long epochMillis;

  /**
   * server will only send notifications for the atsign that sends this command
   */
  private boolean selfNotification;

  /**
   * server will send notifications which match the regex. If 'strict' is set, then only
   * those regex-matching notifications will be sent. If 'strict' is not set, then other
   * 'control' notifications (e.g. the statsNotification) which don't necessarily match the
   * regex will also be sent
   */
  String regex;
}
