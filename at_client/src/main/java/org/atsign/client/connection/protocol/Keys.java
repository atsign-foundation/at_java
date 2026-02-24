package org.atsign.client.connection.protocol;

import static org.atsign.client.connection.protocol.Data.*;
import static org.atsign.client.connection.protocol.Error.throwExceptionIfError;
import static org.atsign.common.VerbBuilders.LookupOperation.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtKeyNames;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.AtException;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Metadata;
import org.atsign.common.VerbBuilders;

import lombok.extern.slf4j.Slf4j;

/**
 * Atsign Protocol utility code that relates to keys (the records) that are managed by an
 * atserver.
 *
 */
@Slf4j
public class Keys {

  public static void deleteKey(AtClientConnection connection, String rawKey) throws AtException {
    try {

      // send a delete command
      String deleteCommand = VerbBuilders.deleteCommandBuilder().rawKey(rawKey).build();
      String deleteResponse = connection.sendSync(deleteCommand);

      // verify command succeeded
      Data.matchDataInt(throwExceptionIfError(deleteResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void deleteKey(AtClientConnection conn, AtKey key) throws AtException {
    deleteKey(conn, key.rawKey());
  }

  public static List<AtKey> getKeys(AtClientConnection conn, String regex, boolean fetchMetadata) throws AtException {

    try {

      // send scan command and decode response
      String scanCommand = VerbBuilders.scanCommandBuilder().regex(regex).showHidden(true).build();
      String scanResponse = conn.sendSync(scanCommand);
      List<String> keyNames = matchDataJsonListOfStrings(throwExceptionIfError(scanResponse));

      // build typed key instances from each key name (optionally looking up metadata)
      List<AtKey> atKeys = new ArrayList<>();
      for (String keyName : keyNames) {
        if (!AtKeyNames.isManagementKeyName(keyName)) {
          Metadata metadata = fetchMetadata ? fetchMetadata(conn, keyName) : null;
          AtKey atKey = org.atsign.common.Keys.keyBuilder()
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

  private static Metadata fetchMetadata(AtClientConnection conn, String keyName)
      throws ExecutionException, InterruptedException, AtException {
    String llookupCommand = VerbBuilders.llookupCommandBuilder().operation(meta).rawKey(keyName).build();
    String llookupResponse = conn.sendSync(llookupCommand);
    return matchMetadata(throwExceptionIfError(llookupResponse));
  }
}
