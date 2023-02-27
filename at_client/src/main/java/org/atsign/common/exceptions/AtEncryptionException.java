package org.atsign.common.exceptions;

import org.atsign.common.AtException;

public class AtEncryptionException extends AtException {
    public AtEncryptionException(String message) {
        super(message);
    }

    public AtEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
