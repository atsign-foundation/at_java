package org.atsign.client.impl.exceptions;

/**
 * Occurs when attempt to encrypt fails
 */
public class AtEncryptionException extends AtException {
  public AtEncryptionException(String message) {
    super(message);
  }

  public AtEncryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
