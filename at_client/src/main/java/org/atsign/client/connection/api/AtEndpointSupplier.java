package org.atsign.client.connection.api;

import org.atsign.common.exceptions.AtSecondaryNotFoundException;

/**
 * Something that is capable of supplying an endpoint string
 */
public interface AtEndpointSupplier {

  /**
   *
   * @return host and port separated by a colon symbol
   * @throws AtSecondaryNotFoundException if it is not possible to resolve the endpoint
   */
  String get() throws AtSecondaryNotFoundException;

}
