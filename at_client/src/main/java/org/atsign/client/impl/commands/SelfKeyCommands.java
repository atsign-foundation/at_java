package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.CommandBuilders.LookupOperation.all;
import static org.atsign.client.impl.commands.DataResponses.matchDataInt;
import static org.atsign.client.impl.commands.DataResponses.matchLookupResponse;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;
import static org.atsign.client.impl.util.EncryptionUtils.*;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.*;
import org.atsign.client.api.Keys.SelfKey;
import org.atsign.client.impl.exceptions.AtException;

/**
 * At Protocol utility code that relates to "self keys"
 *
 */
public class SelfKeyCommands {

  /**
   * Get the String value associated with a self key. The value will be decrypted with the AtSign's
   * Self Encryption Key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link Keys.SelfKey}
   * @return The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static String get(AtCommandExecutor executor, AtSign atSign, AtKeys keys, SelfKey key) throws AtException {
    checkAtSignCanGet(atSign, key);
    try {

      // send local lookup command and decode response
      String llookupCommand = CommandBuilders.llookupCommandBuilder().key(key).operation(all).build();
      String llookupResponse = executor.sendSync(llookupCommand);
      LookupResponse response = matchLookupResponse(throwExceptionIfError(llookupResponse));

      // decrypt with my self encrypt key
      String selfEncryptionKey = keys.getSelfEncryptKey();
      String iv = checkNotNull(response.metaData.ivNonce(), "ivNonce is null");
      String decrypted = aesDecryptFromBase64(response.data, selfEncryptionKey, iv);

      // overwrite metadata
      key.overwriteMetadata(response.metaData);

      return decrypted;
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set a String value to be associated with a self key. The value will be encrypted with the
   * AtSign's
   * Self Encryption Key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link Keys.SelfKey}
   * @param value The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static void put(AtCommandExecutor executor, AtSign atSign, AtKeys keys, SelfKey key, String value)
      throws AtException {
    checkAtSignCanPut(atSign, key);
    try {

      // add signature and iv
      Metadata metadata = Metadata.builder()
          .dataSignature(signSHA256RSA(value, keys.getEncryptPrivateKey()))
          .ivNonce(generateRandomIvBase64(16))
          .build();
      key.updateMissingMetadata(metadata);

      // encrypt with my self encrypt key
      String encrypted = aesEncryptToBase64(value, keys.getSelfEncryptKey(), metadata.ivNonce());

      // send update command to store encrypted value
      String updateCommand = CommandBuilders.updateCommandBuilder()
          .key(key)
          .value(encrypted)
          .build();
      String updateResponse = executor.sendSync(updateCommand);

      // verify response
      matchDataInt(throwExceptionIfError(updateResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void checkAtSignCanGet(AtSign atSign, SelfKey key) {
    if (!key.sharedBy().equals(atSign)) {
      throw new IllegalArgumentException(atSign + " is not the sharedBy of " + key);
    }
  }

  private static void checkAtSignCanPut(AtSign atSign, SelfKey key) {
    if (!key.sharedBy().equals(atSign)) {
      throw new IllegalArgumentException(atSign + " is not the sharedBy of " + key);
    }
  }

}
