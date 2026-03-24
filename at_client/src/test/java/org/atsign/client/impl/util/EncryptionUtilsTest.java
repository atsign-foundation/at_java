package org.atsign.client.impl.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.Signature;
import java.util.Base64;

import org.atsign.client.impl.exceptions.AtDecryptionException;
import org.atsign.client.impl.exceptions.AtEncryptionException;
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

  @Test
  void testAesEncryptToBase64ThrowsExpectedExceptions() throws Exception {
    String key = EncryptionUtils.generateAESKeyBase64();
    String text = "mary had a little lamb";
    String iv = EncryptionUtils.generateRandomIvBase64(16);

    AtEncryptionException ex = assertThrows(AtEncryptionException.class,
                                            () -> EncryptionUtils.aesEncryptToBase64(null, key, iv));
    assertThat(ex.getMessage(), containsString("AES encryption failed : input is blank"));

    ex = assertThrows(AtEncryptionException.class,
                      () -> EncryptionUtils.aesEncryptToBase64(text, null, iv));
    assertThat(ex.getMessage(), containsString("AES encryption failed : key is blank"));

    ex = assertThrows(AtEncryptionException.class,
                      () -> EncryptionUtils.aesEncryptToBase64(text, key, null));
    assertThat(ex.getMessage(), containsString("AES encryption failed : iv is blank"));
  }

  @Test
  void testAesDecryptFromBase64ThrowsExpectedExceptions() throws Exception {
    String key = EncryptionUtils.generateAESKeyBase64();
    String text = "mary had a little lamb";
    String iv = EncryptionUtils.generateRandomIvBase64(16);
    String encrypted = EncryptionUtils.aesEncryptToBase64(text, key, iv);

    AtDecryptionException ex = assertThrows(AtDecryptionException.class,
                                            () -> EncryptionUtils.aesDecryptFromBase64(null, key, iv));
    assertThat(ex.getMessage(), containsString("AES decryption failed : input is blank"));

    ex = assertThrows(AtDecryptionException.class,
                      () -> EncryptionUtils.aesDecryptFromBase64(encrypted, null, iv));
    assertThat(ex.getMessage(), containsString("AES decryption failed : key is blank"));

    ex = assertThrows(AtDecryptionException.class,
                      () -> EncryptionUtils.aesDecryptFromBase64(encrypted, key, null));
    assertThat(ex.getMessage(), containsString("AES decryption failed : iv is blank"));
  }

  @Test
  void testRsaEncryptToBase64AndRsaDecryptFromBase64() throws Exception {
    KeyPair keyPair = EncryptionUtils.generateRSAKeyPair();
    String publicKeyBase64 = EncryptionUtils.toStringBase64(keyPair.getPublic());
    String privateKeyBase64 = EncryptionUtils.toStringBase64(keyPair.getPrivate());
    String text = "mary had a little lamb";

    String encrypted = EncryptionUtils.rsaEncryptToBase64(text, publicKeyBase64);
    assertThat(encrypted, not(equalTo(text)));

    String decrypted = EncryptionUtils.rsaDecryptFromBase64(encrypted, privateKeyBase64);
    assertThat(decrypted, equalTo(text));
  }

  @Test
  void testRsaEncryptToBase64ThrowExpectedExceptions() throws Exception {
    KeyPair keyPair = EncryptionUtils.generateRSAKeyPair();
    String publicKeyBase64 = EncryptionUtils.toStringBase64(keyPair.getPublic());
    String text = "mary had a little lamb";

    AtEncryptionException ex = assertThrows(AtEncryptionException.class,
                                            () -> EncryptionUtils.rsaEncryptToBase64(null, publicKeyBase64));
    assertThat(ex.getMessage(), containsString("RSA encryption failed : input is blank"));

    ex = assertThrows(AtEncryptionException.class,
                      () -> EncryptionUtils.rsaEncryptToBase64(text, null));
    assertThat(ex.getMessage(), containsString("RSA encryption failed : key is blank"));
  }

  @Test
  void testRsaDecryptFromBase64ThrowExpectedExceptions() throws Exception {
    KeyPair keyPair = EncryptionUtils.generateRSAKeyPair();
    String publicKeyBase64 = EncryptionUtils.toStringBase64(keyPair.getPublic());
    String privateKeyBase64 = EncryptionUtils.toStringBase64(keyPair.getPrivate());
    String text = "mary had a little lamb";
    String encrypted = EncryptionUtils.rsaEncryptToBase64(text, publicKeyBase64);

    AtDecryptionException ex = assertThrows(AtDecryptionException.class,
                                            () -> EncryptionUtils.rsaDecryptFromBase64(null, privateKeyBase64));
    assertThat(ex.getMessage(), containsString("RSA decryption failed : input is blank"));

    ex = assertThrows(AtDecryptionException.class,
                      () -> EncryptionUtils.rsaDecryptFromBase64(encrypted, null));
    assertThat(ex.getMessage(), containsString("RSA decryption failed : key is blank"));
  }

  @Test
  void testSignSHA256RSA() throws Exception {
    KeyPair keyPair = EncryptionUtils.generateRSAKeyPair();
    String privateKeyBase64 = EncryptionUtils.toStringBase64(keyPair.getPrivate());
    String text = "mary had a little lamb";

    String signature = EncryptionUtils.signSHA256RSA(text, privateKeyBase64);
    assertThat(signature, not(equalTo(text)));

    Signature verifier = Signature.getInstance("SHA256withRSA");
    verifier.initVerify(keyPair.getPublic());
    verifier.update(text.getBytes(UTF_8));

    assertThat(verifier.verify(Base64.getDecoder().decode(signature)), is(true));
  }

  @Test
  void testSignSHA256RSAThrowExpectedExceptions() throws Exception {
    KeyPair keyPair = EncryptionUtils.generateRSAKeyPair();
    String privateKeyBase64 = EncryptionUtils.toStringBase64(keyPair.getPrivate());
    String text = "mary had a little lamb";

    AtEncryptionException ex = assertThrows(AtEncryptionException.class,
                                            () -> EncryptionUtils.signSHA256RSA(null, privateKeyBase64));
    assertThat(ex.getMessage(), containsString("SHA256 sign failed : input is blank"));

    ex = assertThrows(AtEncryptionException.class,
                      () -> EncryptionUtils.signSHA256RSA(text, null));
    assertThat(ex.getMessage(), containsString("SHA256 sign failed : key is blank"));
  }

  @Test
  void testDigest() throws Exception {
    String text = "mary had a little lamb";

    String digest = EncryptionUtils.digest(text, "MD5");
    assertThat(digest, not(equalTo(text)));
  }

  @Test
  void testDigestThrowsExpectedExceptions() throws Exception {
    String text = "mary had a little lamb";

    AtEncryptionException ex = assertThrows(AtEncryptionException.class, () -> EncryptionUtils.digest(text, "XXX"));
    assertThat(ex.getMessage(), containsString("failed to hash : XXX MessageDigest not available"));

    ex = assertThrows(AtEncryptionException.class, () -> EncryptionUtils.digest(null, "MD5"));
    assertThat(ex.getMessage(), containsString("failed to hash : input blank"));

    ex = assertThrows(AtEncryptionException.class, () -> EncryptionUtils.digest("text", null));
    assertThat(ex.getMessage(), containsString("failed to hash : algo blank"));
  }

  @Test
  public void testBytesToHex() {
    byte[] bytes = new byte[] {(byte) 0x00, (byte) 0x0f, (byte) 0xff};
    assertThat(EncryptionUtils.bytesToHex(bytes), is("000fff"));
  }

}
