package org.atsign.client.impl.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

class EncryptionUtilsTest {

  @Test
  void testAesEncryptionWithRandomInitialisationVector() throws Exception {
    String key = EncryptionUtils.generateAESKeyBase64();
    String text = "mary had a little lamb";
    String iv = EncryptionUtils.generateRandomIvBase64(16);

    String encrypted = EncryptionUtils.aesEncryptToBase64(text, key, iv);
    assertThat(encrypted, not(equalTo(text)));

    String decrypted = EncryptionUtils.aesDecryptFromBase64(encrypted, key, iv);
    assertThat(decrypted, equalTo(text));
  }
}
