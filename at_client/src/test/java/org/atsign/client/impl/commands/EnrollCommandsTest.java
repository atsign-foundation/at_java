package org.atsign.client.impl.commands;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.atsign.client.impl.util.EncryptionUtils.*;
import static org.atsign.client.impl.common.EnrollmentId.createEnrollmentId;
import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.impl.common.EnrollmentId;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.exceptions.AtServerRuntimeException;
import org.junit.jupiter.api.Test;

public class EnrollCommandsTest {

  public static final String RSA_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi0ZpMHagomCnC6MX" +
      "GIRbG9our0NsmPsSPLKSpL/BbJU8nMqwbsSVlDoW9LX8h8yyLeMDC/AsrHjF3jviXWEYNQhZTZohPP" +
      "ioFSEMUHv9N1OC67KoMvZNRU1hEKe2kOaXUQl/Bud257QKl4I9bxr7L1qZD+TPrzclKGKgkKtWDB6M" +
      "+nlql3wFUGRny3RWEjNvLdv0XsIfQwQPO16dZ9vYSXHTiFueqRuTu+HjBVjWudnV2eERpdMq5Hg+Mst" +
      "TYE1uCPuLR5+dcn6UaW1b2H6X9o4ps46BMKLr+xa+/E0EBdAQIhd0QP5O3/ZNLbhJnn7jB+QokmNKoKVN55p7yu0QdwIDAQAB";

  public static final String AES_KEY = "U1xSuG5yjRTolsqoXFcHaSw4al0UJAiWF9Ae3wQ20uM=";

  public static final String IV = "9OD1f3XTbZjz0fWJcw/5Ww==";


  @Test
  public void testDeleteCramSecretDoesNotThrowException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:privatekey:at_secret", "data:1")
        .build();

    EnrollCommands.deleteCramSecret(executor);
  }

  @Test
  public void testDeleteCramSecretThrowsException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:privatekey:at_secret", "error:AT0001:deliberate")
        .build();

    assertThrows(AtServerRuntimeException.class, () -> EnrollCommands.deleteCramSecret(executor));
  }

  @Test
  public void testOnboardThrowsExceptionIfSigningPublicKeyIsMissing() throws Exception {
    AtSign atSign = createAtSign("@alice");
    AtKeys keys = AtKeys.builder()
        .apkamKeyPair(generateRSAKeyPair())
        .encryptKeyPair(generateRSAKeyPair())
        .build();

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("scan", "data:[\"signing_publickey@gary\"]")
        .build();

    Exception ex = assertThrows(Exception.class,
                                () -> EnrollCommands.onboard(executor, new AtCommandExecutorContext(atSign, keys, null),
                                                             "secret", "app", "device", false));
    assertThat(ex.getMessage(), containsString("not connected to the atsign's at server"));
  }

  @Test
  void testOtpSendsExpectedCommandAndMatchesExpectedResponse() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("otp:get", "data:ABC123")
        .build();

    assertThat(EnrollCommands.otp(executor), equalTo("ABC123"));
  }

  @Test
  void testOtpThrowsExceptionOnError() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("otp:get", "error:AT0013:deliberate")
        .build();

    assertThrows(AtException.class, () -> EnrollCommands.otp(executor));
  }

  @Test
  public void testOnboard() throws Exception {
    AtSign atSign = createAtSign("@alice");
    AtKeys keys = AtKeys.builder()
        .apkamKeyPair(generateRSAKeyPair())
        .encryptKeyPair(generateRSAKeyPair())
        .build();

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("scan", "data:[\"signing_publickey@alice\"]")
        .stub("from:@alice", "data:challenge")
        .stub("cram:7e91508d5.+", "data:success")
        .stub("enroll:request.+", "data:{\"enrollmentId\":\"904dcbf7\",\"status\":\"approved\"}")
        .stub("pkam:[^{].+", "data:success")
        .stub("update:public:publickey@alice .+", "data:1")
        .build();

    AtKeys newKeys = EnrollCommands.onboard(executor, new AtCommandExecutorContext(atSign, keys, null), "secret",
                                            "app", "device", false);

    assertThat(newKeys, is(not(sameInstance(keys))));
    assertThat(newKeys.getEnrollmentId(), equalTo(createEnrollmentId("904dcbf7")));
  }

  @Test
  public void testOnboardReusesTheConnectionFromChallengeForCram() throws Exception {
    AtSign atSign = createAtSign("@alice");
    AtKeys keys = AtKeys.builder()
        .apkamKeyPair(generateRSAKeyPair())
        .encryptKeyPair(generateRSAKeyPair())
        .build();

    // simulate the connection having issued from: on connect (sendFrom) and retained the challenge
    AtCommandExecutorContext context = new AtCommandExecutorContext(atSign, keys, null);
    context.setChallenge("challenge");

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("scan", "data:[\"signing_publickey@alice\"]")
        .stub("cram:7e91508d5.+", "data:success")
        .stub("enroll:request.+", "data:{\"enrollmentId\":\"904dcbf7\",\"status\":\"approved\"}")
        .stub("from:@alice", "data:challenge2")
        .stub("pkam:[^{].+", "data:success")
        .stub("update:public:publickey@alice .+", "data:1")
        .build();

    EnrollCommands.onboard(executor, context, "secret", "app", "device", false);

    // CRAM reused the retained challenge, so exactly ONE from: reaches the wire — PKAM's own (the
    // challenge is single-use and PKAM authenticates with the freshly-enrolled keys)
    verify(executor, times(1)).sendSync(matches("from:.*"));
    verify(executor, times(1)).sendSync(matches("cram:.*"));
    assertThat(context.consumeChallenge(), is(nullValue()));
  }

  @Test
  public void testEnroll() throws Exception {
    AtSign atSign = createAtSign("@alice");
    AtKeys keys = AtKeys.builder()
        .apkamKeyPair(generateRSAKeyPair())
        .apkamSymmetricKey(generateAESKeyBase64())
        .build();

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("lookup:publickey@alice", "data:" + RSA_KEY)
        .stub("enroll:request\\{.+}", "data:{\"enrollmentId\":\"759acb09\",\"status\":\"pending\"}")
        .build();

    AtKeys newKeys = EnrollCommands.enroll(executor, atSign, keys, "OTP123", "app", "device", singletonMap("ns", "rw"));

    assertThat(newKeys, is(not(sameInstance(keys))));
    assertThat(newKeys.getEnrollmentId(), equalTo(createEnrollmentId("759acb09")));
    assertThat(newKeys.getEncryptPublicKey(), notNullValue());
  }


  @Test
  public void testComplete() throws Exception {
    AtSign atSign = createAtSign("@alice");
    AtKeys keys = AtKeys.builder()
        .apkamKeyPair(generateRSAKeyPair())
        .apkamSymmetricKey(generateAESKeyBase64())
        .enrollmentId(createEnrollmentId("12345"))
        .build();

    String selfEncryptKeysGetResponse = format("data:{\"value\":\"%s\",\"iv\":\"%s\"}",
                                               aesEncryptToBase64(AES_KEY, keys.getApkamSymmetricKey(), IV), IV);
    String privateEncryptKeysGetResponse = format("data:{\"value\":\"%s\",\"iv\":\"%s\"}",
                                                  aesEncryptToBase64(RSA_KEY, keys.getApkamSymmetricKey(), IV), IV);

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .stub("keys:get:keyName:12345.default_self_enc_key.__manage@alice", selfEncryptKeysGetResponse)
        .stub("keys:get:keyName:12345.default_enc_private_key.__manage@alice", privateEncryptKeysGetResponse)
        .build();

    AtKeys newKeys = EnrollCommands.complete(executor, atSign, keys);

    assertThat(newKeys, is(not(sameInstance(keys))));
    assertThat(newKeys.getEncryptPrivateKey(), notNullValue());
    assertThat(newKeys.getSelfEncryptKey(), notNullValue());
  }

  @Test
  public void testApprove() throws Exception {
    AtKeys keys = AtKeys.builder()
        .encryptKeyPair(generateRSAKeyPair())
        .selfEncryptKey(generateAESKeyBase64())
        .build();

    String fetchResponse = format("data:{\"appName\":\"app1\",\"deviceName\":\"device1\"," +
        "\"namespace\":{\"ns\":\"rw\"},\"encryptedAPKAMSymmetricKey\":\"%s\",\"status\":\"pending\"}",
                                  rsaEncryptToBase64(AES_KEY, keys.getEncryptPublicKey()));

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("enroll:fetch\\{\"enrollmentId\":\"12345\"}", fetchResponse)
        .stub("enroll:approve\\{\"enrollmentId\":\"12345\".+",
              "data:{\"status\":\"approved\",\"enrollmentId\":\"12345\"}")
        .build();

    EnrollCommands.approve(executor, keys, createEnrollmentId("12345"));
  }

  @Test
  public void testDeny() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("enroll:deny\\{\"enrollmentId\":\"12345\".+",
              "data:{\"status\":\"denied\",\"enrollmentId\":\"12345\"}")
        .build();

    EnrollCommands.deny(executor, createEnrollmentId("12345"));
  }

  @Test
  public void testRevoke() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("enroll:revoke\\{\"enrollmentId\":\"12345\".+",
              "data:{\"status\":\"revoked\",\"enrollmentId\":\"12345\"}")
        .build();

    EnrollCommands.revoke(executor, createEnrollmentId("12345"));
  }

  @Test
  public void testUnrevoke() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("enroll:unrevoke\\{\"enrollmentId\":\"12345\".+",
              "data:{\"status\":\"approved\",\"enrollmentId\":\"12345\"}")
        .build();

    EnrollCommands.unrevoke(executor, createEnrollmentId("12345"));
  }

  @Test
  public void testDelete() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("enroll:delete\\{\"enrollmentId\":\"12345\".+",
              "data:{\"status\":\"deleted\",\"enrollmentId\":\"12345\"}")
        .build();

    EnrollCommands.delete(executor, createEnrollmentId("12345"));
  }

  @Test
  public void testList() throws Exception {
    String response = "data:{" +
        "  \"bc8bfdf3-eadd-4373-b0cc-a9c8a52c96c5.new.enrollments.__manage@alice\": {" +
        "    \"sessionId\": \"_e425ba99-88d7-4382-b40d-aeb61ec77ee6\"," +
        "    \"appName\": \"app\"," +
        "    \"deviceName\": \"device\"," +
        "    \"namespaces\": {" +
        "      \"fredns\": \"rw\"" +
        "    }," +
        "    \"apkamPublicKey\": \"\"," +
        "    \"requestType\": \"newEnrollment\"," +
        "    \"approval\": {" +
        "      \"state\": \"pending\"" +
        "    }," +
        "    \"encryptedAPKAMSymmetricKey\": \"\"," +
        "    \"apkamKeysExpiryInMillis\": 0," +
        "    \"status\": \"pending\"," +
        "    \"namespace\": {" +
        "      \"fredns\": \"rw\"" +
        "    }}}";

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("enroll:list\\{\"enrollmentStatusFilter\":\\[\"pending\"]}", response)
        .build();

    List<EnrollmentId> ids = EnrollCommands.list(executor, "pending");
    assertThat(ids, contains(createEnrollmentId("bc8bfdf3-eadd-4373-b0cc-a9c8a52c96c5")));
  }

}
