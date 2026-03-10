package org.atsign.client.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetadataTest {

  @Test
  void testToStringReturnsEmptyStringWhenAllNull() {
    Metadata md = Metadata.builder().build();
    assertThat(md.toString(), is(""));
  }

  @Test
  void testToStringIncludesTtlWhenSet() {
    Metadata md = Metadata.builder().ttl(500L).build();
    assertThat(md.toString(), containsString(":ttl:500"));
  }

  @Test
  void testToStringIncludesTtbWhenSet() {
    Metadata md = Metadata.builder().ttb(200L).build();
    assertThat(md.toString(), containsString(":ttb:200"));
  }

  @Test
  void testToStringIncludesTtrWhenSet() {
    Metadata md = Metadata.builder().ttr(100L).build();
    assertThat(md.toString(), containsString(":ttr:100"));
  }

  @Test
  void testToStringIncludesCcdWhenSet() {
    Metadata md = Metadata.builder().ccd(true).build();
    assertThat(md.toString(), containsString(":ccd:true"));
  }

  @Test
  void testToStringIncludesDataSignatureWhenSet() {
    Metadata md = Metadata.builder().dataSignature("sig123").build();
    assertThat(md.toString(), containsString(":dataSignature:sig123"));
  }

  @Test
  void testToStringIncludesSharedKeyStatusWhenSet() {
    Metadata md = Metadata.builder().sharedKeyStatus("cached").build();
    assertThat(md.toString(), containsString(":sharedKeyStatus:cached"));
  }

  @Test
  void testToStringIncludesSharedKeyEncWhenSet() {
    Metadata md = Metadata.builder().sharedKeyEnc("encKey").build();
    assertThat(md.toString(), containsString(":sharedKeyEnc:encKey"));
  }

  @Test
  void testToStringIncludesPubKeyCSWhenSet() {
    Metadata md = Metadata.builder().pubKeyCS("checksum").build();
    assertThat(md.toString(), containsString(":pubKeyCS:checksum"));
  }

  @Test
  void testToStringIncludesIsBinaryWhenSet() {
    Metadata md = Metadata.builder().isBinary(false).build();
    assertThat(md.toString(), containsString(":isBinary:false"));
  }

  @Test
  void testToStringIncludesIsEncryptedWhenSet() {
    Metadata md = Metadata.builder().isEncrypted(true).build();
    assertThat(md.toString(), containsString(":isEncrypted:true"));
  }

  @Test
  void testToStringIncludesEncodingWhenSet() {
    Metadata md = Metadata.builder().encoding("base64").build();
    assertThat(md.toString(), containsString(":encoding:base64"));
  }

  @Test
  void testToStringIncludesIvNonceWhenSet() {
    Metadata md = Metadata.builder().ivNonce("nonce42").build();
    assertThat(md.toString(), containsString(":ivNonce:nonce42"));
  }

  @Test
  void testToStringOmitsFieldsNotSet() {
    Metadata md = Metadata.builder().ttl(1L).build();
    String s = md.toString();
    assertThat(s, not(containsString(":ttb:")));
    assertThat(s, not(containsString(":ttr:")));
    assertThat(s, not(containsString(":encoding:")));
  }

  @Test
  void testFromJsonParsesBasicFields() throws JsonProcessingException {
    String json = "{\"ttl\":300,\"isPublic\":true,\"encoding\":\"base64\"}";
    Metadata md = Metadata.fromJson(json);

    assertThat(md.ttl(), is(300L));
    assertThat(md.isPublic(), is(true));
    assertThat(md.encoding(), is("base64"));
  }

  @Test
  void testFromJsonEmptyObjectProducesAllNullFields() throws JsonProcessingException {
    Metadata md = Metadata.fromJson("{}");
    assertThat(md.ttl(), is(nullValue()));
    assertThat(md.isPublic(), is(nullValue()));
    assertThat(md.encoding(), is(nullValue()));
  }

  @Test
  void testFromJsonInvalidJsonThrowsException() {
    assertThrows(JsonProcessingException.class, () -> Metadata.fromJson("not-json"));
  }

  @Test
  void testMergeMd1FieldsTakePriorityOverMd2() {
    Metadata md1 = Metadata.builder()
        .ttl(1L).ttb(2L).ttr(3L).ccd(true)
        .isPublic(true).isHidden(true).isCached(true)
        .isEncrypted(true).isBinary(true).namespaceAware(true)
        .dataSignature("ds1").sharedKeyStatus("sks1").sharedKeyEnc("ske1")
        .pubKeyCS("pkcs1").encoding("utf8").ivNonce("iv1")
        .build();

    Metadata md2 = Metadata.builder()
        .ttl(99L).ttb(99L).ttr(99L).ccd(false)
        .isPublic(false).isHidden(false).isCached(false)
        .isEncrypted(false).isBinary(false).namespaceAware(false)
        .dataSignature("ds2").sharedKeyStatus("sks2").sharedKeyEnc("ske2")
        .pubKeyCS("pkcs2").encoding("ascii").ivNonce("iv2")
        .build();

    Metadata merged = Metadata.merge(md1, md2);

    assertThat(merged.ttl(), is(1L));
    assertThat(merged.ttb(), is(2L));
    assertThat(merged.ttr(), is(3L));
    assertThat(merged.ccd(), is(true));
    assertThat(merged.isPublic(), is(true));
    assertThat(merged.isHidden(), is(true));
    assertThat(merged.isCached(), is(true));
    assertThat(merged.isEncrypted(), is(true));
    assertThat(merged.isBinary(), is(true));
    assertThat(merged.namespaceAware(), is(true));
    assertThat(merged.dataSignature(), is("ds1"));
    assertThat(merged.sharedKeyStatus(), is("sks1"));
    assertThat(merged.sharedKeyEnc(), is("ske1"));
    assertThat(merged.pubKeyCS(), is("pkcs1"));
    assertThat(merged.encoding(), is("utf8"));
    assertThat(merged.ivNonce(), is("iv1"));
  }

  @Test
  void testMergeMd2FieldsUsedWhenMd1FieldsAreNull() {
    Metadata md1 = Metadata.builder().build();

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Metadata md2 = Metadata.builder()
        .ttl(1L).ttb(2L).ttr(3L).ccd(true)
        .isPublic(true).isHidden(true).isCached(true)
        .isEncrypted(true).isBinary(true).namespaceAware(true)
        .dataSignature("ds2").sharedKeyStatus("sks2").sharedKeyEnc("ske2")
        .pubKeyCS("pkcs2").encoding("ascii").ivNonce("iv2")
        .availableAt(now).expiresAt(now).refreshAt(now)
        .createdAt(now).updatedAt(now)
        .build();

    Metadata merged = Metadata.merge(md1, md2);

    assertThat(merged.ttl(), is(1L));
    assertThat(merged.ttb(), is(2L));
    assertThat(merged.ttr(), is(3L));
    assertThat(merged.ccd(), is(true));
    assertThat(merged.isPublic(), is(true));
    assertThat(merged.isHidden(), is(true));
    assertThat(merged.isCached(), is(true));
    assertThat(merged.isEncrypted(), is(true));
    assertThat(merged.isBinary(), is(true));
    assertThat(merged.namespaceAware(), is(true));
    assertThat(merged.dataSignature(), is("ds2"));
    assertThat(merged.sharedKeyStatus(), is("sks2"));
    assertThat(merged.sharedKeyEnc(), is("ske2"));
    assertThat(merged.pubKeyCS(), is("pkcs2"));
    assertThat(merged.encoding(), is("ascii"));
    assertThat(merged.ivNonce(), is("iv2"));
    assertThat(merged.availableAt(), is(now));
    assertThat(merged.expiresAt(), is(now));
    assertThat(merged.refreshAt(), is(now));
    assertThat(merged.createdAt(), is(now));
    assertThat(merged.updatedAt(), is(now));
  }

  @Test
  void testMergeFieldRemainsNullWhenBothMd1AndMd2AreNull() {
    Metadata merged = Metadata.merge(Metadata.builder().build(), Metadata.builder().build());

    assertThat(merged.ttl(), is(nullValue()));
    assertThat(merged.ttb(), is(nullValue()));
    assertThat(merged.ttr(), is(nullValue()));
    assertThat(merged.ccd(), is(nullValue()));
    assertThat(merged.isPublic(), is(nullValue()));
    assertThat(merged.isHidden(), is(nullValue()));
    assertThat(merged.isCached(), is(nullValue()));
    assertThat(merged.isEncrypted(), is(nullValue()));
    assertThat(merged.isBinary(), is(nullValue()));
    assertThat(merged.namespaceAware(), is(nullValue()));
    assertThat(merged.dataSignature(), is(nullValue()));
    assertThat(merged.sharedKeyStatus(), is(nullValue()));
    assertThat(merged.sharedKeyEnc(), is(nullValue()));
    assertThat(merged.pubKeyCS(), is(nullValue()));
    assertThat(merged.encoding(), is(nullValue()));
    assertThat(merged.ivNonce(), is(nullValue()));
    assertThat(merged.availableAt(), is(nullValue()));
    assertThat(merged.expiresAt(), is(nullValue()));
    assertThat(merged.refreshAt(), is(nullValue()));
    assertThat(merged.createdAt(), is(nullValue()));
    assertThat(merged.updatedAt(), is(nullValue()));
  }

  @Test
  void testSetTtlIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setTtlIfNotNull(b, 42L), is(true));
    assertThat(b.build().ttl(), is(42L));
  }

  @Test
  void testSetTtlIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ttl(42L);
    assertThat(Metadata.setTtlIfNotNull(b, null), is(false));
    assertThat(b.build().ttl(), is(42L));
  }

  @Test
  void testSetTtbIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setTtbIfNotNull(b, 10L), is(true));
    assertThat(b.build().ttb(), is(10L));
  }

  @Test
  void testSetTtbIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ttb(10L);
    assertThat(Metadata.setTtbIfNotNull(b, null), is(false));
    assertThat(b.build().ttb(), is(10L));
  }

  @Test
  void testSetTtrIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setTtrIfNotNull(b, 5L), is(true));
    assertThat(b.build().ttr(), is(5L));
  }

  @Test
  void testSetTtrIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ttr(5L);
    assertThat(Metadata.setTtrIfNotNull(b, null), is(false));
    assertThat(b.build().ttr(), is(5L));
  }

  @Test
  void testSetCcdIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setCcdIfNotNull(b, false), is(true));
    assertThat(b.build().ccd(), is(false));
  }

  @Test
  void testSetCcdIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ccd(true);
    assertThat(Metadata.setCcdIfNotNull(b, null), is(false));
    assertThat(b.build().ccd(), is(true));
  }

  @Test
  void testSetIsPublicIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setIsPublicIfNotNull(b, true), is(true));
    assertThat(b.build().isPublic(), is(true));
  }

  @Test
  void testSetIsPublicIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().isPublic(true);
    assertThat(Metadata.setIsPublicIfNotNull(b, null), is(false));
    assertThat(b.build().isPublic(), is(true));
  }

  @Test
  void testSetIsHiddenIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setIsHiddenIfNotNull(b, true), is(true));
    assertThat(b.build().isHidden(), is(true));
  }

  @Test
  void testSetIsHiddenIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().isHidden(true);
    assertThat(Metadata.setIsHiddenIfNotNull(b, null), is(false));
    assertThat(b.build().isHidden(), is(true));
  }

  @Test
  void testSetIsCachedIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setIsCachedIfNotNull(b, false), is(true));
    assertThat(b.build().isCached(), is(false));
  }

  @Test
  void testSetIsCachedIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().isCached(true);
    assertThat(Metadata.setIsCachedIfNotNull(b, null), is(false));
    assertThat(b.build().isCached(), is(true));
  }

  @Test
  void testSetIsBinaryIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setIsBinaryIfNotNull(b, true), is(true));
    assertThat(b.build().isBinary(), is(true));
  }

  @Test
  void testSetIsBinaryIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().isBinary(true);
    assertThat(Metadata.setIsBinaryIfNotNull(b, null), is(false));
    assertThat(b.build().isBinary(), is(true));
  }

  @Test
  void testSetIsEncryptedIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setIsEncryptedIfNotNull(b, false), is(true));
    assertThat(b.build().isEncrypted(), is(false));
  }

  @Test
  void testSetIsEncryptedIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().isEncrypted(true);
    assertThat(Metadata.setIsEncryptedIfNotNull(b, null), is(false));
    assertThat(b.build().isEncrypted(), is(true));
  }

  @Test
  void testSetNamespaceAwareIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setNamespaceAwareIfNotNull(b, true), is(true));
    assertThat(b.build().namespaceAware(), is(true));
  }

  @Test
  void testSetNamespaceAwareIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().namespaceAware(true);
    assertThat(Metadata.setNamespaceAwareIfNotNull(b, null), is(false));
    assertThat(b.build().namespaceAware(), is(true));
  }

  @Test
  void testSetDataSignatureIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setDataSignatureIfNotNull(b, "sig"), is(true));
    assertThat(b.build().dataSignature(), is("sig"));
  }

  @Test
  void testSetDataSignatureIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().dataSignature("sig");
    assertThat(Metadata.setDataSignatureIfNotNull(b, null), is(false));
    assertThat(b.build().dataSignature(), is("sig"));
  }

  @Test
  void testSetSharedKeyStatusIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setSharedKeyStatusIfNotNull(b, "ok"), is(true));
    assertThat(b.build().sharedKeyStatus(), is("ok"));
  }

  @Test
  void testSetSharedKeyStatusIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().sharedKeyStatus("ok");
    assertThat(Metadata.setSharedKeyStatusIfNotNull(b, null), is(false));
    assertThat(b.build().sharedKeyStatus(), is("ok"));
  }

  @Test
  void testSetSharedKeyEncIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setSharedKeyEncIfNotNull(b, "encKey"), is(true));
    assertThat(b.build().sharedKeyEnc(), is("encKey"));
  }

  @Test
  void testSetSharedKeyEncIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().sharedKeyEnc("encKey");
    assertThat(Metadata.setSharedKeyEncIfNotNull(b, null), is(false));
    assertThat(b.build().sharedKeyEnc(), is("encKey"));
  }

  @Test
  void testSetPubKeyCSIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setPubKeyCSIfNotNull(b, "checksum"), is(true));
    assertThat(b.build().pubKeyCS(), is("checksum"));
  }

  @Test
  void testSetPubKeyCSIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().pubKeyCS("checksum");
    assertThat(Metadata.setPubKeyCSIfNotNull(b, null), is(false));
    assertThat(b.build().pubKeyCS(), is("checksum"));
  }

  @Test
  void testSetEncodingIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setEncodingIfNotNull(b, "utf8"), is(true));
    assertThat(b.build().encoding(), is("utf8"));
  }

  @Test
  void testSetEncodingIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().encoding("utf8");
    assertThat(Metadata.setEncodingIfNotNull(b, null), is(false));
    assertThat(b.build().encoding(), is("utf8"));
  }

  @Test
  void testSetIvNonceIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setIvNonceIfNotNull(b, "iv99"), is(true));
    assertThat(b.build().ivNonce(), is("iv99"));
  }

  @Test
  void testSetIvNonceIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ivNonce("iv99");
    assertThat(Metadata.setIvNonceIfNotNull(b, null), is(false));
    assertThat(b.build().ivNonce(), is("iv99"));
  }
}
