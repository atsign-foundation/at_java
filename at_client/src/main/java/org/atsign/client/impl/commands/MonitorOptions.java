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
   * multiplexed the server will understand that this is a connection which the client is using
   * not just for notifications but also for request-response interactions. In this case, the
   * server will only send notifications when there is no request currently being handled
   */
  @Builder.Default
  private boolean multiplexed = true;

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
