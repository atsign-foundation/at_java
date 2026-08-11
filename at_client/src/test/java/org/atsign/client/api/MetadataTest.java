package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.atsign.client.api.Metadata.AppMetadata;
import org.atsign.client.api.Metadata.PublicKeyHash;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    assertThrows(RuntimeException.class, () -> Metadata.fromJson("not-json"));
  }

  @Test
  void testMergeMd1FieldsTakePriorityOverMd2() {
    AppMetadata appMetadata1 = AppMetadata.builder()
        .providerId("prov1")
        .build();
    PublicKeyHash pubKeyHash1 = PublicKeyHash.builder()
        .hash("hash1")
        .hashingAlgo("algo1")
        .build();
    Metadata md1 = Metadata.builder()
        .ttl(1L).ttb(2L).ttr(3L).ccd(true)
        .isPublic(true).isHidden(true).isCached(true)
        .isEncrypted(true).isBinary(true).namespaceAware(true)
        .dataSignature("ds1").sharedKeyStatus("sks1").sharedKeyEnc("ske1")
        .pubKeyCS("pkcs1").encoding("utf8").ivNonce("iv1")
        .pubKeyHash(pubKeyHash1)
        .encKeyName("encKeyName1").encAlgo("encAlgo1")
        .skeEncKeyName("skeEncKeyName1").skeEncAlgo("skeEncAlgo1")
        .immutable(true)
        .appMetadata(appMetadata1)
        .build();

    AppMetadata appMetadata2 = AppMetadata.builder()
        .providerId("prov2")
        .build();
    PublicKeyHash pubKeyHash2 = PublicKeyHash.builder()
        .hash("hash2")
        .hashingAlgo("algo2")
        .build();
    Metadata md2 = Metadata.builder()
        .ttl(99L).ttb(99L).ttr(99L).ccd(false)
        .isPublic(false).isHidden(false).isCached(false)
        .isEncrypted(false).isBinary(false).namespaceAware(false)
        .dataSignature("ds2").sharedKeyStatus("sks2").sharedKeyEnc("ske2")
        .pubKeyCS("pkcs2").encoding("ascii").ivNonce("iv2")
        .pubKeyHash(pubKeyHash2)
        .encKeyName("encKeyName2").encAlgo("encAlgo2")
        .skeEncKeyName("skeEncKeyName2").skeEncAlgo("skeEncAlgo2")
        .immutable(false)
        .appMetadata(appMetadata2)
        .build();

    Metadata merged = Metadata.merge(md1, md2);

    assertThat(merged.ttl(), equalTo(1L));
    assertThat(merged.ttb(), equalTo(2L));
    assertThat(merged.ttr(), equalTo(3L));
    assertThat(merged.ccd(), is(true));
    assertThat(merged.isPublic(), is(true));
    assertThat(merged.isHidden(), is(true));
    assertThat(merged.isCached(), is(true));
    assertThat(merged.isEncrypted(), is(true));
    assertThat(merged.isBinary(), is(true));
    assertThat(merged.namespaceAware(), is(true));
    assertThat(merged.dataSignature(), equalTo("ds1"));
    assertThat(merged.sharedKeyStatus(), equalTo("sks1"));
    assertThat(merged.sharedKeyEnc(), equalTo("ske1"));
    assertThat(merged.pubKeyCS(), equalTo("pkcs1"));
    assertThat(merged.encoding(), equalTo("utf8"));
    assertThat(merged.ivNonce(), equalTo("iv1"));
    assertThat(merged.pubKeyHash(), equalTo(pubKeyHash1));
    assertThat(merged.encKeyName(), equalTo("encKeyName1"));
    assertThat(merged.encAlgo(), equalTo("encAlgo1"));
    assertThat(merged.skeEncKeyName(), equalTo("skeEncKeyName1"));
    assertThat(merged.skeEncAlgo(), equalTo("skeEncAlgo1"));
    assertThat(merged.immutable(), is(true));
    assertThat(merged.appMetadata(), equalTo(appMetadata1));
  }

  @Test
  void testMergeMd2FieldsUsedWhenMd1FieldsAreNull() {
    Metadata md1 = Metadata.builder().build();

    AppMetadata appMetadata2 = AppMetadata.builder()
        .providerId("prov2")
        .build();
    PublicKeyHash pubKeyHash2 = PublicKeyHash.builder()
        .hash("hash2")
        .hashingAlgo("algo2")
        .build();
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    Metadata md2 = Metadata.builder()
        .ttl(1L).ttb(2L).ttr(3L).ccd(true)
        .isPublic(true).isHidden(true).isCached(true)
        .isEncrypted(true).isBinary(true).namespaceAware(true)
        .dataSignature("ds2").sharedKeyStatus("sks2").sharedKeyEnc("ske2")
        .pubKeyCS("pkcs2").encoding("ascii").ivNonce("iv2")
        .availableAt(now).expiresAt(now).refreshAt(now)
        .createdAt(now).updatedAt(now)
        .pubKeyHash(pubKeyHash2)
        .encKeyName("encKeyName2").encAlgo("encAlgo2")
        .skeEncKeyName("skeEncKeyName2").skeEncAlgo("skeEncAlgo2")
        .immutable(false)
        .appMetadata(appMetadata2)
        .build();

    Metadata merged = Metadata.merge(md1, md2);

    assertThat(merged.ttl(), equalTo(1L));
    assertThat(merged.ttb(), equalTo(2L));
    assertThat(merged.ttr(), equalTo(3L));
    assertThat(merged.ccd(), is(true));
    assertThat(merged.isPublic(), is(true));
    assertThat(merged.isHidden(), is(true));
    assertThat(merged.isCached(), is(true));
    assertThat(merged.isEncrypted(), is(true));
    assertThat(merged.isBinary(), is(true));
    assertThat(merged.namespaceAware(), is(true));
    assertThat(merged.dataSignature(), equalTo("ds2"));
    assertThat(merged.sharedKeyStatus(), equalTo("sks2"));
    assertThat(merged.sharedKeyEnc(), equalTo("ske2"));
    assertThat(merged.pubKeyCS(), equalTo("pkcs2"));
    assertThat(merged.encoding(), equalTo("ascii"));
    assertThat(merged.ivNonce(), equalTo("iv2"));
    assertThat(merged.availableAt(), equalTo(now));
    assertThat(merged.expiresAt(), equalTo(now));
    assertThat(merged.refreshAt(), equalTo(now));
    assertThat(merged.createdAt(), equalTo(now));
    assertThat(merged.updatedAt(), equalTo(now));
    assertThat(merged.pubKeyHash(), equalTo(pubKeyHash2));
    assertThat(merged.encKeyName(), equalTo("encKeyName2"));
    assertThat(merged.encAlgo(), equalTo("encAlgo2"));
    assertThat(merged.skeEncKeyName(), equalTo("skeEncKeyName2"));
    assertThat(merged.skeEncAlgo(), equalTo("skeEncAlgo2"));
    assertThat(merged.immutable(), is(false));
    assertThat(merged.appMetadata(), equalTo(appMetadata2));
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
    assertThat(merged.pubKeyHash(), is(nullValue()));
    assertThat(merged.encKeyName(), is(nullValue()));
    assertThat(merged.encAlgo(), is(nullValue()));
    assertThat(merged.skeEncKeyName(), is(nullValue()));
    assertThat(merged.skeEncAlgo(), is(nullValue()));
    assertThat(merged.immutable(), is(nullValue()));
    assertThat(merged.appMetadata(), is(nullValue()));
  }

  @Test
  void testMergeKeepsMd1AppMetadataWholeRatherThanMergingAdditionalEntries() {
    AppMetadata appMetadata1 = AppMetadata.builder()
        .providerId("prov1")
        .additional(Map.of("a", 1))
        .build();
    AppMetadata appMetadata2 = AppMetadata.builder()
        .providerId("prov2")
        .additional(Map.of("b", 2))
        .build();
    Metadata md1 = Metadata.builder().appMetadata(appMetadata1).build();
    Metadata md2 = Metadata.builder().appMetadata(appMetadata2).build();

    Metadata merged = Metadata.merge(md1, md2);

    // appMetadata is provider-owned and indivisible: md1 wins whole, md2's entries are not
    // folded in.
    assertThat(merged.appMetadata(), equalTo(appMetadata1));
    assertThat(merged.appMetadata().additional(), equalTo(Map.of("a", 1)));
  }

  @Test
  void testToMergedBuilderReturnsBuilderThatCanBeOverriddenBeforeBuild() {
    AppMetadata appMetadata1 = AppMetadata.builder().providerId("prov1").build();
    AppMetadata override = AppMetadata.builder().providerId("override").build();
    Metadata md1 = Metadata.builder().ttl(1L).appMetadata(appMetadata1).build();
    Metadata md2 = Metadata.builder().ttl(99L).ttb(2L).build();

    Metadata.MetadataBuilder builder = Metadata.toMergedBuilder(md1, md2);

    Metadata merged = builder.build();
    assertThat(merged.ttl(), equalTo(1L));
    assertThat(merged.ttb(), equalTo(2L));
    assertThat(merged.appMetadata(), equalTo(appMetadata1));

    Metadata overridden = builder.appMetadata(override).build();
    assertThat(overridden.appMetadata(), equalTo(override));
    assertThat(overridden.ttl(), equalTo(1L));
  }

  @Test
  void testSetAppMetadataIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    AppMetadata am = AppMetadata.builder().providerId("prov").build();
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setAppMetadataIfNotNull(b, am), is(true));
    assertThat(b.build().appMetadata(), equalTo(am));
  }

  @Test
  void testSetAppMetadataIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    AppMetadata am = AppMetadata.builder().providerId("prov").build();
    Metadata.MetadataBuilder b = Metadata.builder().appMetadata(am);
    assertThat(Metadata.setAppMetadataIfNotNull(b, null), is(false));
    assertThat(b.build().appMetadata(), equalTo(am));
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
    assertThat(b.build().ttb(), equalTo(10L));
  }

  @Test
  void testSetTtbIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ttb(10L);
    assertThat(Metadata.setTtbIfNotNull(b, null), is(false));
    assertThat(b.build().ttb(), equalTo(10L));
  }

  @Test
  void testSetTtrIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setTtrIfNotNull(b, 5L), is(true));
    assertThat(b.build().ttr(), equalTo(5L));
  }

  @Test
  void testSetTtrIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ttr(5L);
    assertThat(Metadata.setTtrIfNotNull(b, null), is(false));
    assertThat(b.build().ttr(), equalTo(5L));
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
    assertThat(b.build().dataSignature(), equalTo("sig"));
  }

  @Test
  void testSetDataSignatureIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().dataSignature("sig");
    assertThat(Metadata.setDataSignatureIfNotNull(b, null), is(false));
    assertThat(b.build().dataSignature(), equalTo("sig"));
  }

  @Test
  void testSetSharedKeyStatusIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setSharedKeyStatusIfNotNull(b, "ok"), is(true));
    assertThat(b.build().sharedKeyStatus(), equalTo("ok"));
  }

  @Test
  void testSetSharedKeyStatusIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().sharedKeyStatus("ok");
    assertThat(Metadata.setSharedKeyStatusIfNotNull(b, null), is(false));
    assertThat(b.build().sharedKeyStatus(), equalTo("ok"));
  }

  @Test
  void testSetSharedKeyEncIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setSharedKeyEncIfNotNull(b, "encKey"), is(true));
    assertThat(b.build().sharedKeyEnc(), equalTo("encKey"));
  }

  @Test
  void testSetSharedKeyEncIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().sharedKeyEnc("encKey");
    assertThat(Metadata.setSharedKeyEncIfNotNull(b, null), is(false));
    assertThat(b.build().sharedKeyEnc(), equalTo("encKey"));
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
    assertThat(b.build().pubKeyCS(), equalTo("checksum"));
  }

  @Test
  void testSetEncodingIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setEncodingIfNotNull(b, "utf8"), is(true));
    assertThat(b.build().encoding(), equalTo("utf8"));
  }

  @Test
  void testSetEncodingIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().encoding("utf8");
    assertThat(Metadata.setEncodingIfNotNull(b, null), is(false));
    assertThat(b.build().encoding(), equalTo("utf8"));
  }

  @Test
  void testSetIvNonceIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setIvNonceIfNotNull(b, "iv99"), is(true));
    assertThat(b.build().ivNonce(), equalTo("iv99"));
  }

  @Test
  void testSetIvNonceIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().ivNonce("iv99");
    assertThat(Metadata.setIvNonceIfNotNull(b, null), is(false));
    assertThat(b.build().ivNonce(), equalTo("iv99"));
  }

  @Test
  void testSetPubKeyHashIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    PublicKeyHash hash = PublicKeyHash.builder().hash("HASH").hashingAlgo("algo").build();
    assertThat(Metadata.setPubKeyHashIfNotNull(b, hash), is(true));
    assertThat(b.build().pubKeyHash(), equalTo(hash));
  }

  @Test
  void testSetPubKeyHashIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    PublicKeyHash hash = PublicKeyHash.builder().hash("HASH").hashingAlgo("algo").build();
    Metadata.MetadataBuilder b = Metadata.builder().pubKeyHash(hash);
    assertThat(Metadata.setPubKeyHashIfNotNull(b, null), is(false));
    assertThat(b.build().pubKeyHash(), equalTo(hash));
  }

  @Test
  void testSetEncKeyNameIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setEncKeyNameIfNotNull(b, "encKeyName"), is(true));
    assertThat(b.build().encKeyName(), equalTo("encKeyName"));
  }

  @Test
  void testSetEncKeyNameIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().encKeyName("encKeyName");
    assertThat(Metadata.setEncKeyNameIfNotNull(b, null), is(false));
    assertThat(b.build().encKeyName(), equalTo("encKeyName"));
  }

  @Test
  void testSetEncAlgoIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setEncAlgoIfNotNull(b, "encAlgo"), is(true));
    assertThat(b.build().encAlgo(), equalTo("encAlgo"));
  }

  @Test
  void testSetEncAlgoIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().encAlgo("encAlgo");
    assertThat(Metadata.setEncAlgoIfNotNull(b, null), is(false));
    assertThat(b.build().encAlgo(), equalTo("encAlgo"));
  }

  @Test
  void testSetSkeEncKeyNameIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setSkeEncKeyNameIfNotNull(b, "skeEncAlgo"), is(true));
    assertThat(b.build().skeEncKeyName(), equalTo("skeEncAlgo"));
  }

  @Test
  void testSetSkeEncKeyNameIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().skeEncKeyName("skeEncAlgo");
    assertThat(Metadata.setSkeEncKeyNameIfNotNull(b, null), is(false));
    assertThat(b.build().skeEncKeyName(), equalTo("skeEncAlgo"));
  }

  @Test
  void testSetSkeEncAlgoIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setSkeEncAlgoIfNotNull(b, "skeEncAlgo"), is(true));
    assertThat(b.build().skeEncAlgo(), equalTo("skeEncAlgo"));
  }

  @Test
  void testSetSkeEncAlgoIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().skeEncAlgo("skeEncAlgo");
    assertThat(Metadata.setSkeEncAlgoIfNotNull(b, null), is(false));
    assertThat(b.build().skeEncAlgo(), equalTo("skeEncAlgo"));
  }


  @Test
  void testSetImmutableIfNotNullReturnsTrueAndSetsValueWhenNotNull() {
    Metadata.MetadataBuilder b = Metadata.builder();
    assertThat(Metadata.setImmutableIfNotNull(b, false), is(true));
    assertThat(b.build().immutable(), is(false));
  }

  @Test
  void testSetImmutableIfNotNullReturnsFalseAndDoesNotOverwriteExistingValueWhenNull() {
    Metadata.MetadataBuilder b = Metadata.builder().immutable(true);
    assertThat(Metadata.setImmutableIfNotNull(b, null), is(false));
    assertThat(b.build().immutable(), is(true));
  }

  // ---- AppMetadata (pluggable-encryption provider metadata) ----

  @Test
  void testAppMetadataToJsonIsFlatProviderIdPlusAdditional() {
    AppMetadata am = AppMetadata.builder()
        .providerId("legacy")
        .additional(Map.of("v", 2))
        .build();
    Map<String, Object> json = am.toJson();
    assertThat(json.get("providerId"), equalTo("legacy"));
    assertThat(json.get("v"), equalTo(2));
    // additional is flattened, NOT nested under an "additional" key
    assertThat(json.containsKey("additional"), is(false));
  }

  @Test
  void testAppMetadataFromJsonRoutesNonProviderIdKeysToAdditional() {
    AppMetadata am = AppMetadata.fromJson(Map.of("providerId", "p1", "x", "y"));
    assertThat(am.providerId(), equalTo("p1"));
    assertThat(am.additional().get("x"), equalTo("y"));
  }

  @Test
  void testAppMetadataFromJsonWithOnlyProviderIdHasNullAdditional() {
    AppMetadata am = AppMetadata.fromJson(Map.of("providerId", "p1"));
    assertThat(am.additional(), is(nullValue()));
  }

  @Test
  void testAppMetadataFromJsonThrowsWhenProviderIdMissingOrBlank() {
    assertThrows(IllegalArgumentException.class,
                 () -> AppMetadata.fromJson(Map.of()));
    assertThrows(IllegalArgumentException.class,
                 () -> AppMetadata.fromJson(Map.of("providerId", "  ")));
  }

  @Test
  void testAppMetadataEncodeDecodeRoundTrips() {
    AppMetadata am = AppMetadata.builder().providerId("prov").build();
    String encoded = am.encode();
    // base64 of a JSON object — decode must reconstruct the same value
    assertThat(AppMetadata.decode(encoded), equalTo(am));
    // canonical parity: the static Metadata helpers delegate to encode/decode
    assertThat(Metadata.encodeAppMetadata(am), equalTo(encoded));
    assertThat(Metadata.decodeAppMetadata(encoded), equalTo(am));
  }

  @Test
  void testAppMetadataDecodeAcceptsMapAndAppMetadataAndNull() {
    AppMetadata am = AppMetadata.builder().providerId("prov").build();
    assertThat(AppMetadata.decode(am), sameInstance(am));
    assertThat(AppMetadata.decode(Map.of("providerId", "prov")), equalTo(am));
    assertThat(AppMetadata.decode(null), is(nullValue()));
    assertThat(AppMetadata.decode("null"), is(nullValue()));
  }

  @Test
  void testToStringEncodesAppMetadataAsBase64Fragment() {
    AppMetadata am = AppMetadata.builder().providerId("prov").build();
    Metadata md = Metadata.builder().appMetadata(am).build();
    assertThat(md.toString(), containsString(":appMetadata:" + am.encode()));
  }

  @Test
  void testMetadataFromJsonDeserialisesNestedAppMetadataObject() {
    // The metadata-map form carries appMetadata as a flat JSON object (not base64).
    Metadata md = Metadata.fromJson("{\"appMetadata\":{\"providerId\":\"prov\",\"v\":1}}");
    assertThat(md.appMetadata().providerId(), equalTo("prov"));
    assertThat(md.appMetadata().additional().get("v"), equalTo(1));
  }

}
