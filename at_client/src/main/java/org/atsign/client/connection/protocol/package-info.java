/**
 * Utilities which given an {@link org.atsign.client.connection.api.AtClientConnection}
 * can perform common functions such as authentication, atsign onboarding and
 * enrollment. In some case these are simply sending the appropriate At Protocol command
 * and verifying the response. Other scenarios such as those relating shared keys
 * require some workflow.
 */
package org.atsign.client.connection.protocol;
