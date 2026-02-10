package org.atsign.common.response_models;

import org.atsign.common.Metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data class used to hold a response to lookup, llookup or plookup commands
 */
public class LookupResponse {

  @JsonProperty
  public String key;

  @JsonProperty
  public String data;

  @JsonProperty
  public Metadata metaData;
}
