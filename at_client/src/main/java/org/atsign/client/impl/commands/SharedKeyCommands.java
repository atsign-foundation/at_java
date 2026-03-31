package org.atsign.client.impl.commands;

import static org.atsign.client.api.AtKeyNames.toSharedByMeKeyName;
import static org.atsign.client.impl.commands.CommandBuilders.LookupOperation.all;
import static org.atsign.client.impl.commands.DataResponses.*;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;
import static org.atsign.client.impl.common.Preconditions.checkTrue;
import static org.atsign.client.impl.util.EncryptionUtils.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.atsign.client.api.*;
import org.atsign.client.api.Keys.SharedKey;
import org.atsign.client.impl.exceptions.*;
import org.atsign.client.impl.util.EncryptionUtils;

/**
 * At Protocol utility code that relates to "shared keys".
 *
 */

public class SharedKeyCommands {

  /**
   * Get the String value associated with a shared key. The value will be encrypted with a specific
   * key for the sharedBy-sharedWith relationship.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link Keys.SharedKey}
   * @return The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */

  public static String get(AtCommandExecutor executor, AtSign atSign, AtKeys keys, SharedKey key) throws AtException {
    return get(executor, atSign, keys, key, false);
  }

  /**
   * Get the String value associated with a shared key. The value will be encrypted with a specific
   * key for the sharedBy-sharedWith relationship.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link Keys.SharedKey}
   * @param expectedBinary If true then lookup metadata will be checked
   * @return The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */

  public static String get(AtCommandExecutor executor, AtSign atSign, AtKeys keys, SharedKey key,
                           boolean expectedBinary)
      throws AtException {
    checkAtSignCanGet(atSign, key);
    if (key.sharedBy().equals(atSign)) {
      return getSharedByMe(executor, keys, key, expectedBinary);
    } else if (key.sharedWith().equals(atSign)) {
      return getSharedByOther(executor, keys, key, expectedBinary);
    } else {
      throw new IllegalArgumentException("the client atsign is neither the sharedBy or sharedWith");
    }
  }

  /**
   * Set a String value to be associated with a shared key. The value will be decrypted with a
   * specific
   * key for the sharedBy-sharedWith relationship.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link Keys.SharedKey}
   * @param value The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static void put(AtCommandExecutor executor, AtSign atSign, AtKeys keys, SharedKey key, String value)
      throws AtException {
    checkAtSignCanPut(atSign, key);
    try {

      // get or create key for sharedBy - sharedWith
      String aesKey = lookupEncryptKeySharedByMe(executor, keys, key);
      if (aesKey == null) {
        aesKey = createEncryptKey(executor, keys, key);
      }

      // encrypt the value
      String iv = EncryptionUtils.generateRandomIvBase64(16);
      key.updateMissingMetadata(Metadata.builder().ivNonce(iv).build());
      String encrypted = aesEncryptToBase64(value, aesKey, iv);

      // send an update command
      String updateCommand = CommandBuilders.updateCommandBuilder().key(key).value(encrypted).build();
      String updateResponse = executor.sendSync(updateCommand);

      // verify the response
      matchDataInt(throwExceptionIfError(updateResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getSharedByMe(AtCommandExecutor executor, AtKeys keys, SharedKey key, boolean expectBinary)
      throws AtException {
    try {

      // send local lookup command and decode
      String llookupCommand = CommandBuilders.llookupCommandBuilder().key(key).operation(all).build();
      LookupResponse llookupResponse = matchLookupResponse(throwExceptionIfError(executor.sendSync(llookupCommand)));

      if (expectBinary) {
        checkTrue(Metadata.isBinary(llookupResponse.metaData), "metadata.isBinary not set to true");
      }

      // get the encryption key that was previously created by "me"
      String aesKey = checkNotNull(lookupEncryptKeySharedByMe(executor, keys, key), key + " not found");

      // return decrypted value
      return aesDecryptFromBase64(llookupResponse.data, aesKey, llookupResponse.metaData.ivNonce());

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getSharedByOther(AtCommandExecutor executor, AtKeys keys, SharedKey key, boolean expectBinary)
      throws AtException {
    try {

      // send lookup command and decode
      String lookupCommand = CommandBuilders.lookupCommandBuilder().key(key).operation(all).build();
      LookupResponse lookupResponse = matchLookupResponse(throwExceptionIfError(executor.sendSync(lookupCommand)));

      if (expectBinary) {
        checkTrue(Metadata.isBinary(lookupResponse.metaData), "isBinary not set to true");
      }

      // get the encryption key that was created by the "other"
      String sharedEncryptionKey;
      if (lookupResponse.metaData.sharedKeyEnc() != null) {
        sharedEncryptionKey = extractEncryptKeySharedByOther(lookupResponse, keys);
      } else {
        sharedEncryptionKey = lookupEncryptKeySharedByOther(executor, keys, key);
      }

      // return decrypted value
      return aesDecryptFromBase64(lookupResponse.data, sharedEncryptionKey, lookupResponse.metaData.ivNonce());

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static String extractEncryptKeySharedByOther(LookupResponse lookupResponse, AtKeys keys)
      throws AtException {
    String encryptedShareEncryptionKey = lookupResponse.metaData.sharedKeyEnc();
    if (lookupResponse.metaData.pubKeyHash() != null) {
      String algo = lookupResponse.metaData.pubKeyHash().hashingAlgo();
      String hash = digest(keys.getEncryptPublicKey(), algo);
      if (!hash.equals(lookupResponse.metaData.pubKeyHash().hash())) {
        throw new AtPublicKeyChangeException("pubKeyHash mis-match");
      }
    }
    return rsaDecryptFromBase64(encryptedShareEncryptionKey, keys.getEncryptPrivateKey());
  }

  /**
   * Get the specific encryption key which needs to be used for a sharedBy - sharedWith relationship
   * where the AtSign that the {@link AtCommandExecutor} has authenticated with is the sharedBy
   * AtSign. This will automatically decrypt the value with the AtKeys Private Encryption Key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param keys The {@link AtKeys} for the {@link AtSign} that is the sharedBy in the relationship.
   * @param key The {@link Keys.SharedKey}
   * @return The symmetric encryption key (in base64).
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static String lookupEncryptKeySharedByMe(AtCommandExecutor executor, AtKeys keys, SharedKey key)
      throws AtException {
    try {

      String keyName = AtKeyNames.toSharedByMeKeyName(key.sharedWith());
      String aesKey = keys.get(keyName);
      if (aesKey != null) {
        return aesKey;
      }

      // send local lookup command for sharedKey
      String llookupCommand = CommandBuilders.llookupCommandBuilder()
          .keyName(toSharedByMeKeyName(key.sharedWith()))
          .sharedBy(key.sharedBy())
          .build();
      String llookupResponse = executor.sendSync(llookupCommand);

      try {
        // decrypt the key
        String encrypted = DataResponses.matchDataStringNoWhitespace(throwExceptionIfError(llookupResponse));
        aesKey = rsaDecryptFromBase64(encrypted, keys.getEncryptPrivateKey());

        // store in keys
        keys.put(keyName, aesKey);

        return aesKey;
      } catch (AtKeyNotFoundException e) {
        return null;
      }
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the specific encryption key which needs to be used for a sharedBy - sharedWith relationship
   * where the AtSign that the {@link AtCommandExecutor} has authenticated with is the sharedWith
   * AtSign. This will automatically decrypt the value with the AtKeys Private Encryption Key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param keys The {@link AtKeys} for the {@link AtSign} that is the sharedWith in the relationship.
   * @param key The {@link Keys.SharedKey}
   * @return The symmetric encryption key (in base64).
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static String lookupEncryptKeySharedByOther(AtCommandExecutor executor, AtKeys keys, SharedKey key)
      throws AtException {
    try {

      // check in Keys cache
      String keyName = AtKeyNames.toSharedWithMeKeyName(key.sharedBy(), key.sharedWith());
      String aesKey = keys.get(keyName);
      if (aesKey != null) {
        return aesKey;
      }

      // otherwise send lookup
      String lookupCommand = CommandBuilders.lookupCommandBuilder()
          .keyName(AtKeyNames.SHARED_KEY)
          .sharedBy(key.sharedBy())
          .build();
      String lookupResponse = executor.sendSync(lookupCommand);

      // decrypt with my private key
      String encrypted = matchDataStringNoWhitespace(throwExceptionIfError(lookupResponse));
      aesKey = rsaDecryptFromBase64(encrypted, keys.getEncryptPrivateKey());

      // store in keys
      keys.put(keyName, aesKey);

      return aesKey;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static String createEncryptKey(AtCommandExecutor executor, AtKeys keys, SharedKey key) throws AtException {
    try {

      // generate a new encrypt key
      String aesKey = EncryptionUtils.generateAESKeyBase64();

      // compose an update command to store this key encrypted with this (sharedBy) atsign's public key
      String encryptedForMe = rsaEncryptToBase64(aesKey, keys.getEncryptPublicKey());
      String updateForUsCommand = CommandBuilders.updateCommandBuilder()
          .keyName(toSharedByMeKeyName(key.sharedWith()))
          .sharedBy(key.sharedBy())
          .value(encryptedForMe)
          .build();
      executor.sendSync(updateForUsCommand);

      // get the other (sharedWith) atsign's public key (and compute the hash of that key)
      String otherPublicKey = getEncryptKey(executor, key.sharedWith());
      Metadata.PublicKeyHash hash = Metadata.PublicKeyHash.builder()
          .hash(EncryptionUtils.digest(otherPublicKey, HASHING_ALGO_SHA512))
          .hashingAlgo(HASHING_ALGO_SHA512)
          .build();
      String checksum = digest(otherPublicKey, MD5);

      // compose an update command to store this key encrypted with the other (sharedWith) atsign's public key
      String encryptedForOther = rsaEncryptToBase64(aesKey, otherPublicKey);
      String updateForOtherCommand = CommandBuilders.updateCommandBuilder()
          .keyName(AtKeyNames.SHARED_KEY)
          .sharedBy(key.sharedBy())
          .sharedWith(key.sharedWith())
          .ttr(TimeUnit.HOURS.toMillis(24))
          .value(encryptedForOther)
          .build();
      executor.sendSync(updateForOtherCommand);

      // update the key metadata to include the shared encryption key encrypted with the other (sharedWith)
      // atsign's public key plus the hash of that key to accommodate race conditions on public key changes
      Metadata modifiedMetadata = key.metadata().toBuilder()
          .sharedKeyEnc(encryptedForOther)
          .pubKeyHash(hash)
          .pubKeyCS(checksum)
          .build();
      key.overwriteMetadata(modifiedMetadata);

      // store in my cache
      keys.put(AtKeyNames.toSharedByMeKeyName(key.sharedWith()), aesKey);

      // return the new
      return aesKey;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getEncryptKey(AtCommandExecutor executor, AtSign atSign) throws AtException {
    try {

      // send plookup for atsign's public encryption key
      String plookupCommand = CommandBuilders.plookupCommandBuilder()
          .keyName(AtKeyNames.PUBLIC_ENCRYPT)
          .sharedBy(atSign)
          .build();
      String plookupResponse = executor.sendSync(plookupCommand);

      // return key
      return matchDataStringNoWhitespace(throwExceptionIfError(plookupResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void checkAtSignCanGet(AtSign atSign, SharedKey key) {
    if (!key.sharedBy().equals(atSign) && !key.sharedWith().equals(atSign)) {
      throw new IllegalArgumentException(atSign + " is neither the sharedBy or sharedWith of " + key);
    }
  }

  private static void checkAtSignCanPut(AtSign atSign, SharedKey key) {
    if (!key.sharedBy().equals(atSign)) {
      throw new IllegalArgumentException(atSign + " is not the sharedBy of " + key);
    }
  }


}
