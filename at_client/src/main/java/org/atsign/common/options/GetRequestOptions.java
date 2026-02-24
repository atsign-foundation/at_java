package org.atsign.common.options;


import lombok.Builder;
import lombok.Value;

/**
 * Data class used to model options to the {@link org.atsign.client.api.AtClient} get methods
 */
@Value
@Builder
public class GetRequestOptions {
  boolean bypassCache;
}
