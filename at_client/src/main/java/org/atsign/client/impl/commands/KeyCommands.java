package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.CommandBuilders.LookupOperation.meta;
import static org.atsign.client.impl.commands.DataResponses.matchDataJsonListOfStrings;
import static org.atsign.client.impl.commands.DataResponses.matchMetadata;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeyNames;
import org.atsign.client.api.Keys.AtKey;
import org.atsign.client.api.Metadata;
import org.atsign.client.impl.exceptions.AtException;

import lombok.extern.slf4j.Slf4j;

/**
 * At Protocol utility code that relates to keys (the records) that can be stored in an
 * At Server.
 *
 */
@Slf4j
public class KeyCommands {

  /**
   * Sends the delete command and verifies the response.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param rawKey The key to delete.
   * @throws AtException If any of the commands fail.
   */
  public static void deleteKey(AtCommandExecutor executor, String rawKey) throws AtException {
    try {

      // send a delete command
      String deleteCommand = CommandBuilders.deleteCommandBuilder().rawKey(rawKey).build();
      String deleteResponse = executor.sendSync(deleteCommand);

      // verify command succeeded
      DataResponses.matchDataInt(throwExceptionIfError(deleteResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends the delete command and verifies the response for an {@link AtKey}
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param key The key to delete.
   * @throws AtException If any of the commands fail.
   */
  public static void deleteKey(AtCommandExecutor executor, AtKey key) throws AtException {
    deleteKey(executor, key.rawKey());
  }

  /**
   * Sends the scan command to list the matching keys. And optionally then does a lookup
   * to obtain the metadata for each key.
   *
   * @param executor The {@link AtCommandExecutor} to use.
   * @param regex A regular expression to match the key names.
   * @param fetchMetadata If true then utility will lookup the metadata for each key.
   * @return A list of {@link AtKey} subclasses that correspond to the scan results.
   * @throws AtException If any of the commands fail.
   */
  public static List<AtKey> getKeys(AtCommandExecutor executor, String regex, boolean fetchMetadata)
      throws AtException {

    try {

      // send scan command and decode response
      String scanCommand = CommandBuilders.scanCommandBuilder().regex(regex).showHidden(true).build();
      String scanResponse = executor.sendSync(scanCommand);
      List<String> keyNames = matchDataJsonListOfStrings(throwExceptionIfError(scanResponse));

      // build typed key instances from each key name (optionally looking up metadata)
      List<AtKey> atKeys = new ArrayList<>();
      for (String keyName : keyNames) {
        if (!AtKeyNames.isManagementKeyName(keyName)) {
          Metadata metadata = fetchMetadata ? fetchMetadata(executor, keyName) : null;
          AtKey atKey = org.atsign.client.api.Keys.keyBuilder()
              .rawKey(keyName)
              .metadata(metadata)
              .build();
          atKeys.add(atKey);
        }
      }

      return atKeys;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static Metadata fetchMetadata(AtCommandExecutor executor, String keyName)
      throws ExecutionException, InterruptedException, AtException {
    String llookupCommand = CommandBuilders.llookupCommandBuilder().operation(meta).rawKey(keyName).build();
    String llookupResponse = executor.sendSync(llookupCommand);
    return matchMetadata(throwExceptionIfError(llookupResponse));
  }
}
