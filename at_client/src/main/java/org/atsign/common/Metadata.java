package org.atsign.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Metadata {
    static final ObjectMapper mapper = new ObjectMapper();

    public Integer ttl;
    public Integer ttb;
    public Integer ttr;
    public Boolean ccd;
    public String createdBy;
    public String updatedBy;
    @JsonDeserialize(using = AtStringDateTimeDeserializer.class)
    public OffsetDateTime availableAt;
    @JsonDeserialize(using = AtStringDateTimeDeserializer.class)
    public OffsetDateTime expiresAt;
    @JsonDeserialize(using = AtStringDateTimeDeserializer.class)
    public OffsetDateTime refreshAt;
    @JsonDeserialize(using = AtStringDateTimeDeserializer.class)
    public OffsetDateTime createdAt;
    @JsonDeserialize(using = AtStringDateTimeDeserializer.class)
    public OffsetDateTime updatedAt;
    public String status;
    public Integer version;
    public String dataSignature;
    public String sharedKeyStatus;
    public Boolean isPublic = false;
    public Boolean isEncrypted = true;
    public Boolean isHidden = false;
    public Boolean namespaceAware = true;
    public Boolean isBinary = false;
    public Boolean isCached = false;
    public String sharedKeyEnc;
    public String pubKeyCS;
    public String encoding;

    public static Metadata fromJson(String json) throws JsonProcessingException {
        return mapper.readValue(json, Metadata.class);
    }

    @Override
    public String toString() {
        String s = "";
        if (ttl != null) s += ":ttl:" + ttl;
        if (ttb != null) s += ":ttb:" + ttb;
        if (ttr != null) s += ":ttr:" + ttr;
        if (ccd != null) s += ":ccd:" + ccd;
        if (dataSignature != null) s += ":dataSignature:" + dataSignature;
        if (sharedKeyStatus != null) s += ":sharedKeyStatus:" + sharedKeyStatus;
        if (sharedKeyEnc != null) s += ":sharedKeyEnc:" + sharedKeyEnc;
        if (pubKeyCS != null) s += ":pubKeyCS:" + pubKeyCS;
        if (isBinary != null) s += ":isBinary:" + isBinary;
        if (isEncrypted != null) s += ":isEncrypted:" + isEncrypted;
        if (encoding != null) s += ":encoding:" + encoding;
        return s;
    }

    /**
     * Squashes the two metadatas into one metadata.
     *
     * @param firstMetadata  has priority
     * @param secondMetadata use fields from here if not in firstMetadata
     * @return One merged metadata object
     */
    public static Metadata squash(Metadata firstMetadata, Metadata secondMetadata) {
        Metadata metadata = new Metadata();
        if (firstMetadata.ttl != null) metadata.ttl = firstMetadata.ttl;
        else if (secondMetadata.ttl != null) metadata.ttl = secondMetadata.ttl;

        if (firstMetadata.ttb != null) metadata.ttb = firstMetadata.ttb;
        else if (secondMetadata.ttb != null) metadata.ttb = secondMetadata.ttb;

        if (firstMetadata.ttr != null) metadata.ttr = firstMetadata.ttr;
        else if (secondMetadata.ttr != null) metadata.ttr = secondMetadata.ttr;

        if (firstMetadata.ccd != null) metadata.ccd = firstMetadata.ccd;
        else if (secondMetadata.ccd != null) metadata.ccd = secondMetadata.ccd;

        if (firstMetadata.availableAt != null) metadata.availableAt = firstMetadata.availableAt;
        else if (secondMetadata.availableAt != null) metadata.availableAt = secondMetadata.availableAt;

        if (firstMetadata.expiresAt != null) metadata.expiresAt = firstMetadata.expiresAt;
        else if (secondMetadata.expiresAt != null) metadata.expiresAt = secondMetadata.expiresAt;

        if (firstMetadata.refreshAt != null) metadata.refreshAt = firstMetadata.refreshAt;
        else if (secondMetadata.refreshAt != null) metadata.refreshAt = secondMetadata.refreshAt;

        if (firstMetadata.createdAt != null) metadata.createdAt = firstMetadata.createdAt;
        else if (secondMetadata.createdAt != null) metadata.createdAt = secondMetadata.createdAt;

        if (firstMetadata.updatedAt != null) metadata.updatedAt = firstMetadata.updatedAt;
        else if (secondMetadata.updatedAt != null) metadata.updatedAt = secondMetadata.updatedAt;

        if (firstMetadata.dataSignature != null) metadata.dataSignature = firstMetadata.dataSignature;
        else if (secondMetadata.dataSignature != null) metadata.dataSignature = secondMetadata.dataSignature;

        if (firstMetadata.sharedKeyStatus != null) metadata.sharedKeyStatus = firstMetadata.sharedKeyStatus;
        else if (secondMetadata.sharedKeyStatus != null) metadata.sharedKeyStatus = secondMetadata.sharedKeyStatus;

        if (firstMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = firstMetadata.sharedKeyEnc;
        else if (secondMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = secondMetadata.sharedKeyEnc;

        if (firstMetadata.isPublic != null) metadata.isPublic = firstMetadata.isPublic;
        else if (secondMetadata.isPublic != null) metadata.isPublic = secondMetadata.isPublic;

        if (firstMetadata.isEncrypted != null) metadata.isEncrypted = firstMetadata.isEncrypted;
        else if (secondMetadata.isEncrypted != null) metadata.isEncrypted = secondMetadata.isEncrypted;

        if (firstMetadata.isHidden != null) metadata.isHidden = firstMetadata.isHidden;
        else if (secondMetadata.isHidden != null) metadata.isHidden = secondMetadata.isHidden;

        if (firstMetadata.namespaceAware != null) metadata.namespaceAware = firstMetadata.namespaceAware;
        else if (secondMetadata.namespaceAware != null) metadata.namespaceAware = secondMetadata.namespaceAware;

        if (firstMetadata.isBinary != null) metadata.isBinary = firstMetadata.isBinary;
        else if (secondMetadata.isBinary != null) metadata.isBinary = secondMetadata.isBinary;

        if (firstMetadata.isCached != null) metadata.isCached = firstMetadata.isCached;
        else if (secondMetadata.isCached != null) metadata.isCached = secondMetadata.isCached;

        if (firstMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = firstMetadata.sharedKeyEnc;
        else if (secondMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = secondMetadata.sharedKeyEnc;

        if (firstMetadata.pubKeyCS != null) metadata.pubKeyCS = firstMetadata.pubKeyCS;
        else if (secondMetadata.pubKeyCS != null) metadata.pubKeyCS = secondMetadata.pubKeyCS;

        if (firstMetadata.encoding != null) metadata.encoding = firstMetadata.encoding;
        else if (secondMetadata.encoding != null) metadata.encoding = secondMetadata.encoding;

        return metadata;
    }

    @SuppressWarnings("unused")
    public static class AtStringDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {
        public AtStringDateTimeDeserializer() {
            this((Class<?>) null);
        }
        protected AtStringDateTimeDeserializer(Class<?> vc) {
            super(vc);
        }

        protected AtStringDateTimeDeserializer(JavaType valueType) {
            super(valueType);
        }

        protected AtStringDateTimeDeserializer(StdDeserializer<?> src) {
            super(src);
        }

        static final DateTimeFormatter dateTimeFormatter
                = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

        @Override
        public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            String dateString = jsonParser.getText();
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, dateTimeFormatter);
            return zonedDateTime.toOffsetDateTime();
        }
    }
}
