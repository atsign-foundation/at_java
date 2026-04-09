package org.atsign.client.impl.commands.builders;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;
import static org.atsign.client.impl.util.EncryptionUtils.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.atsign.client.api.Keys;
import org.atsign.client.api.Metadata;
import org.atsign.client.impl.commands.CommandBuilders;
import org.atsign.client.impl.exceptions.AtEncryptionException;
import org.atsign.client.impl.util.EncryptionUtils;

/**
 * Command builder for sending shared key update notify commands.
 */
public class NotifyUpdateSharedKeyCommandBuilder {

  private final CommandBuilders.NotifyKeyChangeCommandBuilder commandBuilder;
  private final Metadata.MetadataBuilder metadataBuilder;
  private String sharedWithPublicKey;
  private boolean reuseSharedKey;
  private String sharedKey;

  public NotifyUpdateSharedKeyCommandBuilder() {
    metadataBuilder = Metadata.builder().isEncrypted(true);
    commandBuilder = CommandBuilders.notifyKeyChangeCommandBuilder()
        .operation(CommandBuilders.NotifyOperation.update)
        .notifier("SYSTEM")
        .ttln(TimeUnit.MINUTES.toMillis(1));
  }

  public NotifyUpdateSharedKeyCommandBuilder key(Keys.SharedKey key) {
    commandBuilder.key(key);
    return this;
  }

  public NotifyUpdateSharedKeyCommandBuilder reuseSharedKey(boolean reuse) {
    this.reuseSharedKey = reuse;
    return this;
  }

  public NotifyUpdateSharedKeyCommandBuilder sharedWithPublicKey(String key) throws AtEncryptionException {
    sharedWithPublicKey = key;
    Metadata.PublicKeyHash publicKeyHash = Metadata.PublicKeyHash.builder()
        .hash(EncryptionUtils.digest(sharedWithPublicKey, HASHING_ALGO_SHA512))
        .hashingAlgo(HASHING_ALGO_SHA512)
        .build();
    metadataBuilder.pubKeyHash(publicKeyHash);
    metadataBuilder.pubKeyCS(digest(sharedWithPublicKey, MD5));
    return this;
  }

  public String build(String text) throws AtEncryptionException {
    checkNotNull(sharedWithPublicKey, "sharedWithPublicKey not set");
    if (sharedKey == null || !reuseSharedKey) {
      sharedKey = EncryptionUtils.generateAESKeyBase64();
      metadataBuilder.sharedKeyEnc(rsaEncryptToBase64(sharedKey, sharedWithPublicKey));
    }
    String iv = EncryptionUtils.generateRandomIvBase64(16);
    return commandBuilder
        .id(UUID.randomUUID().toString())
        .value(aesEncryptToBase64(text, sharedKey, iv))
        .metadata(metadataBuilder.ivNonce(iv).build())
        .build();
  }
}
