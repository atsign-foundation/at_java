package org.atsign.client.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.atsign.client.impl.common.TypedString;
import org.atsign.client.impl.common.TypedToken;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A piece of cryptographic key material, such as the public half of an encryption keypair or a
 * symmetric key.
 */
@Value
@Builder(toBuilder = true)
public class CryptographicMaterial {

  @NonNull
  KeyId keyId;

  EnrollmentId enrollmentId;

  @NonNull
  Role role;

  @NonNull
  Algorithm algorithm;

  @Singular
  List<Operation> operations;

  @NonNull
  BytesAsBase64 bytes;

  @NonNull
  OffsetDateTime createdAt;

  @Builder.Default
  Status status = Status.active;

  /**
   * @param status the status the copy should carry
   * @return a copy of this material with only its status replaced
   */
  public CryptographicMaterial withStatus(Status status) {
    return toBuilder().status(status).build();
  }

  /**
   * The key material itself, held as base64
   */
  public static final class BytesAsBase64 extends TypedString {

    private BytesAsBase64(String base64) {
      super(base64);
    }

    /**
     * @param base64 the base64-encoded key material
     * @return null if {@code base64} is null or blank, otherwise the corresponding value
     * @throws IllegalArgumentException if {@code base64} is not valid base64
     */
    public static BytesAsBase64 of(String base64) {
      if (isBlank(base64)) {
        return null;
      }
      java.util.Base64.getDecoder().decode(base64);
      return new BytesAsBase64(base64);
    }

    /**
     * @return the base64 value, exactly as supplied
     */
    public String base64() {
      return super.toString();
    }

    /**
     * @return the key material as bytes
     */
    public byte[] bytes() {
      return java.util.Base64.getDecoder().decode(base64());
    }

    @Override
    public String toString() {
      return "base64(" + base64().length() + " chars)";
    }
  }

  /**
   * Lifecycle state of a {@link CryptographicMaterial}.
   * <b>Do not reorder.</b> A status may only ever move forward, and {@code ordinal()} is that
   * ordering.
   */
  public enum Status {
    active, retired, dead
  }

  /**
   * What the mathematics does with the material, independent of its {@link Algorithm}.
   * Contrast {@code operations}, which is what a deployment permits
   * rather than a property of the material.
   * <p>
   * An open vocabulary: the constants are only the values this version knows about, and an
   * unrecognised token is accepted and preserved, never rejected.
   */
  public static final class Role extends TypedToken {

    private static final Map<String, Role> POOL = new ConcurrentHashMap<>();

    public static final Role symmetricEncryption = Role.of("symmetricEncryption");
    public static final Role symmetricAuthentication = Role.of("symmetricAuthentication");
    public static final Role publicEncryption = Role.of("publicEncryption");
    public static final Role privateDecryption = Role.of("privateDecryption");
    public static final Role publicVerification = Role.of("publicVerification");
    public static final Role privateSigning = Role.of("privateSigning");
    public static final Role publicEncapsulation = Role.of("publicEncapsulation");
    public static final Role privateDecapsulation = Role.of("privateDecapsulation");
    public static final Role publicKeyAgreement = Role.of("publicKeyAgreement");
    public static final Role privateKeyAgreement = Role.of("privateKeyAgreement");

    private Role(String token) {
      super(token);
    }

    public static Role of(String token) {
      return intern(token, POOL, Role::new);
    }
  }

  /**
   * The algorithm family the material belongs to, independent of its {@link Role}.
   * <p>
   * An open vocabulary on the same terms as {@link Role}, so a new algorithm needs no change here.
   */
  public static final class Algorithm extends TypedToken {

    private static final Map<String, Algorithm> POOL = new ConcurrentHashMap<>();

    public static final Algorithm aes256 = Algorithm.of("aes256");
    public static final Algorithm rsa2048 = Algorithm.of("rsa2048");
    public static final Algorithm ecc_secp256r1 = Algorithm.of("ecc_secp256r1");
    public static final Algorithm ed25519 = Algorithm.of("ed25519");
    public static final Algorithm x25519 = Algorithm.of("x25519");
    public static final Algorithm mlkem768 = Algorithm.of("mlkem768");
    public static final Algorithm mldsa65 = Algorithm.of("mldsa65");
    public static final Algorithm xwing = Algorithm.of("xwing");

    private Algorithm(String token) {
      super(token);
    }

    public static Algorithm of(String token) {
      return intern(token, POOL, Algorithm::new);
    }
  }

  /**
   * The intended usage of the material, which is policy rather than a property of the
   * material itself — contrast {@link Role}.
   */
  public static final class Operation extends TypedToken {

    private static final Map<String, Operation> POOL = new ConcurrentHashMap<>();

    public static final Operation sign = Operation.of("sign");
    public static final Operation decrypt = Operation.of("decrypt");

    private Operation(String token) {
      super(token);
    }

    public static Operation of(String token) {
      return intern(token, POOL, Operation::new);
    }
  }
}
