package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.DataResponses.matchDataInt;
import static org.atsign.client.impl.commands.DataResponses.matchLookupResponse;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.util.EncryptionUtils.signSHA256RSA;
import static org.atsign.client.impl.commands.CommandBuilders.LookupOperation.all;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys.PublicKey;
import org.atsign.client.api.Metadata;
import org.atsign.client.api.AtClient.GetRequestOptions;

/**
 * Atsign protocol utility code that relates to "public keys"
 *
 */
public class PublicKeyCommands {

  public static String get(AtCommandExecutor executor, AtSign atSign, PublicKey key, GetRequestOptions options)
      throws AtException {
    if (atSign.equals(key.sharedBy())) {
      return getSharedByMe(executor, key);
    } else {
      return getSharedByOther(executor, key, options);
    }
  }

  public static String getSharedByMe(AtCommandExecutor executor, PublicKey publicKey) throws AtException {
    try {

      // send a local lookup command and decode the response
      String llookupCommand = CommandBuilders.llookupCommandBuilder()
          .key(publicKey)
          .operation(all)
          .build();
      String llookupResponse = executor.sendSync(llookupCommand);
      LookupResponse response = matchLookupResponse(throwExceptionIfError(llookupResponse));

      // set isCached in metadata
      if (response.key.contains("cached:")) {
        Metadata metadata = response.metaData.toBuilder().isCached(true).build();
        publicKey.overwriteMetadata(metadata);
      }

      // return value
      return response.data;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getSharedByOther(AtCommandExecutor executor, PublicKey publicKey, GetRequestOptions options)
      throws AtException {
    try {

      // send public lookup command and decode the response
      String plookupCommand = CommandBuilders.plookupCommandBuilder()
          .key(publicKey)
          .bypassCache(options != null ? options.isBypassCache() : null)
          .operation(all)
          .build();
      String plookupResponse = executor.sendSync(plookupCommand);
      LookupResponse response = matchLookupResponse(throwExceptionIfError(plookupResponse));

      // set isCached in metadata
      if (response.key.contains("cached:")) {
        Metadata metadata = response.metaData.toBuilder().isCached(true).build();
        publicKey.overwriteMetadata(metadata);
      }

      // return value
      return response.data;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void put(AtCommandExecutor executor, AtKeys keys, PublicKey publicKey, String value)
      throws AtException {
    try {

      // add a signature to the metadata
      Metadata metadata = Metadata.builder()
          .dataSignature(signSHA256RSA(value, keys.getEncryptPrivateKey()))
          .build();
      publicKey.updateMissingMetadata(metadata);

      // send and update command
      String updateCommand = CommandBuilders.updateCommandBuilder().key(publicKey).value(value).build();
      String updateResponse = executor.sendSync(updateCommand);

      // verify the response
      matchDataInt(throwExceptionIfError(updateResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
