package org.atsign.common.response_models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.atsign.common.Metadata;

public class LookupResponse {
    
    @JsonProperty
    public String key;

    @JsonProperty
    public String data;

    @JsonProperty
    public Metadata metaData;
}
