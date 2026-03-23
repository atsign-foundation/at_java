package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.CommandBuilders.LookupOperation.all;
import static org.atsign.client.impl.commands.DataResponses.matchDataInt;
import static org.atsign.client.impl.commands.DataResponses.matchLookupResponse;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.common.Preconditions.checkTrue;
import static org.atsign.client.impl.util.EncryptionUtils.signSHA256RSA;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient.GetRequestOptions;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys.PublicKey;
import org.atsign.client.api.Metadata;
import org.atsign.client.impl.exceptions.AtException;

/**
 * At Protocol utility code that relates to "public keys".
 *
 */
public class PublicKeyCommands {

  /**
   * Get the String value associated with a public key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link PublicKey}
   * @param options If set then can be used to bypass caches.
   * @return The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static String get(AtCommandExecutor executor, AtSign atSign, PublicKey key, GetRequestOptions options)
      throws AtException {
    return get(executor, atSign, key, false, options);
  }

  /**
   * Get the String value associated with a public key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link PublicKey}
   * @param options If set then can be used to bypass caches.
   * @return The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static String get(AtCommandExecutor executor,
                           AtSign atSign,
                           PublicKey key,
                           boolean expectBinary,
                           GetRequestOptions options)
      throws AtException {
    if (atSign.equals(key.sharedBy())) {
      return getSharedByMe(executor, key, expectBinary);
    } else {
      return getSharedByOther(executor, key, expectBinary, options);
    }
  }

  /**
   * Get the String value associated with a public key that has been shared by the {@link AtSign}
   * that the {@link AtCommandExecutor} has authenticated with. This will use a llookup command.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param key The {@link PublicKey}
   * @param expectBinary If true then metadata will be checked
   * @return The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static String getSharedByMe(AtCommandExecutor executor,
                                     PublicKey key,
                                     boolean expectBinary)
      throws AtException {
    try {

      // send a local lookup command and decode the response
      String llookupCommand = CommandBuilders.llookupCommandBuilder()
          .key(key)
          .operation(all)
          .build();
      String llookupResponse = executor.sendSync(llookupCommand);
      LookupResponse response = matchLookupResponse(throwExceptionIfError(llookupResponse));

      if (expectBinary) {
        checkTrue(Metadata.isBinary(response.metaData), "metadata.isBinary not set to true");
      }

      // set isCached in metadata
      if (response.key.contains("cached:")) {
        Metadata metadata = response.metaData.toBuilder().isCached(true).build();
        key.overwriteMetadata(metadata);
      }

      // return value
      return response.data;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the String value associated with a public key that has been shared by somebody other than
   * the {@link AtSign} that the {@link AtCommandExecutor} has authenticated with. This will use
   * a plookup command.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param key The {@link PublicKey}
   * @param expectBinary If true then metadata will be checked
   * @param options If set then can be used to bypass caches.
   * @return The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static String getSharedByOther(AtCommandExecutor executor,
                                        PublicKey key,
                                        boolean expectBinary,
                                        GetRequestOptions options)
      throws AtException {
    try {

      // send public lookup command and decode the response
      String plookupCommand = CommandBuilders.plookupCommandBuilder()
          .key(key)
          .bypassCache(options != null ? options.isBypassCache() : null)
          .operation(all)
          .build();
      String plookupResponse = executor.sendSync(plookupCommand);
      LookupResponse response = matchLookupResponse(throwExceptionIfError(plookupResponse));

      if (expectBinary) {
        checkTrue(Metadata.isBinary(response.metaData), "isBinary not set to true");
      }

      // set isCached in metadata
      if (response.key.contains("cached:")) {
        Metadata metadata = response.metaData.toBuilder().isCached(true).build();
        key.overwriteMetadata(metadata);
      }

      // return value
      return response.data;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set a String value to be associated with a public key. A signature will be added to the key
   * metadata using the AtSign's Private Encryption Key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param atSign The AtSign that corresponds to the executor.
   * @param key The {@link PublicKey}
   * @param value The associated value.
   * @throws AtException If any of the commands fail or the key does not exist.
   */
  public static void put(AtCommandExecutor executor, AtSign atSign, AtKeys keys, PublicKey key, String value)
      throws AtException {
    checkAtSignCanPut(atSign, key);
    try {

      // add a signature to the metadata
      Metadata metadata = Metadata.builder()
          .dataSignature(signSHA256RSA(value, keys.getEncryptPrivateKey()))
          .build();
      key.updateMissingMetadata(metadata);

      // send and update command
      String updateCommand = CommandBuilders.updateCommandBuilder().key(key).value(value).build();
      String updateResponse = executor.sendSync(updateCommand);

      // verify the response
      matchDataInt(throwExceptionIfError(updateResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void checkAtSignCanPut(AtSign atSign, PublicKey key) {
    if (!key.sharedBy().equals(atSign)) {
      throw new IllegalArgumentException(atSign + " is not the sharedBy of " + key);
    }
  }

}
