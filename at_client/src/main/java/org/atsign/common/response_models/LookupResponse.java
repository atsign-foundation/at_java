package org.atsign.common.response_models;

import org.atsign.common.Metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LookupResponse {

  @JsonProperty
  public String key;

  @JsonProperty
  public String data;

  @JsonProperty
  public Metadata metaData;
}
