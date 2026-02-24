package org.atsign.client.connection.protocol;

import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.AtException;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Metadata;
import org.atsign.common.VerbBuilders;
import org.atsign.common.response_models.LookupResponse;

import java.util.concurrent.ExecutionException;

import static org.atsign.client.connection.protocol.Data.matchDataInt;
import static org.atsign.client.connection.protocol.Data.matchLookupResponse;
import static org.atsign.client.connection.protocol.Error.throwExceptionIfError;
import static org.atsign.client.util.EncryptionUtil.*;
import static org.atsign.client.util.EncryptionUtil.aesEncryptToBase64;
import static org.atsign.client.util.Preconditions.checkNotNull;
import static org.atsign.common.VerbBuilders.LookupOperation.all;

/**
 * Atsign protocol utility code that relates to "self keys"
 *
 */
public class SelfKeys {

  public static String get(AtClientConnection conn, AtKeys keys, SelfKey key) throws AtException {
    try {

      // send local lookup command and decode response
      String llookupCommand = VerbBuilders.llookupCommandBuilder().key(key).operation(all).build();
      String llookupResponse = conn.sendSync(llookupCommand);
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

  public static void put(AtClientConnection conn, AtKeys keys, SelfKey key, String value) throws AtException {
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
      String updateCommand = VerbBuilders.updateCommandBuilder()
          .key(key)
          .value(encrypted)
          .build();
      String updateResponse = conn.sendSync(updateCommand);

      // verify response
      matchDataInt(throwExceptionIfError(updateResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
