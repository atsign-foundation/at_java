package org.atsign.client.impl.cli;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.commands.TestExecutorBuilder;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;
import org.atsign.client.impl.util.KeysUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.commands.EnrollCommandsTest.*;
import static org.atsign.client.impl.common.EnrollmentId.createEnrollmentId;
import static org.atsign.client.impl.util.EncryptionUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivateTest {

  @TempDir
  File keysDir;
  private AtKeys keys;
  private File keysFile;
  private String selfKey;
  private String privateKey;

  @BeforeEach
  public void setUp() throws Exception {
    keys = AtKeys.builder()
        .apkamKeyPair(generateRSAKeyPair())
        .apkamSymmetricKey(generateAESKeyBase64())
        .selfEncryptKey(generateAESKeyBase64())
        .enrollmentId(createEnrollmentId("12345"))
        .build();
    keysFile = new File(keysDir, "@alice_key.atKeys");
    KeysUtils.saveKeys(keys, keysFile);

    selfKey = aesEncryptToBase64(AES_KEY, keys.getApkamSymmetricKey(), IV);
    privateKey = aesEncryptToBase64(RSA_KEY, keys.getApkamSymmetricKey(), IV);
  }

  @Test
  void testCompleteRetryEventuallySucceeds() throws Exception {

    List<String> commands = new ArrayList<>();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice:clientConfig:.+", "data:challenge")
        .stub("pkam:[^{].+", createPkamResponse(4))
        .stub("keys:get:keyName:12345.default_self_enc_key.__manage@alice", createGetKeyResponse(selfKey))
        .stub("keys:get:keyName:12345.default_enc_private_key.__manage@alice", createGetKeyResponse(privateKey))
        .record(commands::add)
        .build();

    Activate activate = new Activate().setAtSign(createAtSign("@alice"));
    activate.setKeysFile(keysFile.getPath());

    activate.complete(executor, 3, 0, MILLISECONDS);

    List<String> fromCommands = commands.stream().filter(x -> x.startsWith("from:")).collect(Collectors.toList());
    assertThat(fromCommands.size(), equalTo(4));
    assertThat(fromCommands.stream().distinct().count(), equalTo(1L));

    AtKeys newKeys = KeysUtils.loadKeys(keysFile);
    assertThat(newKeys.getEncryptPrivateKey(), notNullValue());
  }

  @Test
  void testCompleteRetryEventuallyGivesUp() throws Exception {

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice:clientConfig:.+", "data:challenge")
        .stub("pkam:[^{].+", createPkamResponse(4))
        .stub("keys:get:keyName:12345.default_self_enc_key.__manage@alice", createGetKeyResponse(selfKey))
        .stub("keys:get:keyName:12345.default_enc_private_key.__manage@alice", createGetKeyResponse(privateKey))
        .build();

    Activate activate = new Activate().setAtSign(createAtSign("@alice"));
    activate.setKeysFile(keysFile.getPath());

    assertThrows(AtUnauthenticatedException.class, () -> activate.complete(executor, 2, 0, MILLISECONDS));
  }

  private static Function<Matcher, String> createPkamResponse(int after) {
    AtomicInteger invocations = new AtomicInteger();
    return x -> {
      if (invocations.incrementAndGet() == after) {
        return "data:success";
      } else {
        return "error:AT0401:enrollment_id:12345 is pending";
      }
    };
  }

  private static Function<Matcher, String> createGetKeyResponse(String key) {
    return x -> format("data:{\"value\":\"%s\",\"iv\":\"%s\"}", key, IV);
  }
}
