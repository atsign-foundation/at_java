package org.atsign.client.impl.commands;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.Keys.SelfKey;
import org.atsign.client.api.Metadata;

import java.util.concurrent.ExecutionException;

import static org.atsign.client.impl.commands.DataResponses.matchDataInt;
import static org.atsign.client.impl.commands.DataResponses.matchLookupResponse;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.util.EncryptionUtils.*;
import static org.atsign.client.impl.util.EncryptionUtils.aesEncryptToBase64;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;
import static org.atsign.client.impl.commands.CommandBuilders.LookupOperation.all;

/**
 * Atsign protocol utility code that relates to "self keys"
 *
 */
public class SelfKeyCommands {

  public static String get(AtCommandExecutor executor, AtKeys keys, SelfKey key) throws AtException {
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

  public static void put(AtCommandExecutor executor, AtKeys keys, SelfKey key, String value) throws AtException {
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

}
