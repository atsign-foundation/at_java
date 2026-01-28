package org.atsign.client.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

class EncryptionUtilTest {

  @Test
  void testAesEncryptionWithRandomInitialisationVector() throws Exception {
    String key = EncryptionUtil.generateAESKeyBase64();
    String text = "mary had a little lamb";
    String iv = EncryptionUtil.generateRandomIvBase64(16);

    String encrypted = EncryptionUtil.aesEncryptToBase64(text, key, iv);
    assertThat(encrypted, not(equalTo(text)));

    String decrypted = EncryptionUtil.aesDecryptFromBase64(encrypted, key, iv);
    assertThat(decrypted, equalTo(text));
  }
}
