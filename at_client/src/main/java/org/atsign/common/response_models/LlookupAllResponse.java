package org.atsign.common.response_models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LlookupAllResponse {
    
    @JsonProperty
    public String key;

    @JsonProperty
    public String data;

    @JsonProperty
    public Metadata metaData;

    public class Metadata {
        @JsonProperty
        public String createdBy;
        @JsonProperty
        public String updatedBy;
        @JsonProperty
        public String createdAt;
        @JsonProperty
        public String updatedAt;
        @JsonProperty
        public String availableAt;
        @JsonProperty
        public String expiresAt;
        @JsonProperty
        public String refreshAt;
        @JsonProperty
        public String status;
        @JsonProperty
        public Integer version;
        @JsonProperty
        public Integer ttl;
        @JsonProperty
        public Integer ttb;
        @JsonProperty
        public Integer ttr;
        @JsonProperty
        public Boolean ccd;
        @JsonProperty
        public Boolean isBinary;
        @JsonProperty
        public Boolean isEncrypted;
        @JsonProperty
        public String dataSignature;
        @JsonProperty
        public String sharedKeyEnc;
        @JsonProperty
        public String pubKeyCS;
        @JsonProperty
        public String encoding;
    }

}
