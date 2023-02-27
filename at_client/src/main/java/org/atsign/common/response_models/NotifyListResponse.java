package org.atsign.common.response_models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotifyListResponse {
    
    @JsonProperty
    public List<Notification> notifications;

    public static class Notification {
        @JsonProperty
        public String id;

        @JsonProperty
        public String from;

        @JsonProperty
        public String to;

        @JsonProperty
        public String key;

        @JsonProperty
        public String value;

        @JsonProperty
        public String operation;

        @JsonProperty
        public Long epochMillis;

        @JsonProperty
        public String messageType;

        @JsonProperty
        public Boolean isEncrypted;
        
    }
    

}
