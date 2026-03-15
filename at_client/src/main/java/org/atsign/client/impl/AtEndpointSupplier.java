package org.atsign.client.impl;

import org.atsign.client.impl.exceptions.AtSecondaryNotFoundException;

/**
 * Something that is capable of supplying an endpoint string (e.g. tcp://host:port).
 */
public interface AtEndpointSupplier {

  /**
   *
   * @return host and port separated by a colon symbol
   * @throws AtSecondaryNotFoundException if it is not possible to resolve the endpoint
   */
  String get() throws AtSecondaryNotFoundException;

}
