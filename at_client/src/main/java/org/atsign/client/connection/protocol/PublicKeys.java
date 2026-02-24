package org.atsign.client.connection.protocol;

import static org.atsign.client.connection.protocol.Data.matchDataInt;
import static org.atsign.client.connection.protocol.Data.matchLookupResponse;
import static org.atsign.client.connection.protocol.Error.throwExceptionIfError;
import static org.atsign.client.util.EncryptionUtil.signSHA256RSA;
import static org.atsign.common.VerbBuilders.LookupOperation.all;

import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Metadata;
import org.atsign.common.VerbBuilders;
import org.atsign.common.options.GetRequestOptions;
import org.atsign.common.response_models.LookupResponse;

/**
 * Atsign protocol utility code that relates to "public keys"
 *
 */
public class PublicKeys {

  public static String get(AtClientConnection conn, AtSign atSign, PublicKey key, GetRequestOptions options)
      throws AtException {
    if (atSign.equals(key.sharedBy())) {
      return getSharedByMe(conn, key);
    } else {
      return getSharedByOther(conn, key, options);
    }
  }

  public static String getSharedByMe(AtClientConnection conn, PublicKey publicKey) throws AtException {
    try {

      // send a local lookup command and decode the response
      String llookupCommand = VerbBuilders.llookupCommandBuilder()
          .key(publicKey)
          .operation(all)
          .build();
      String llookupResponse = conn.sendSync(llookupCommand);
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

  public static String getSharedByOther(AtClientConnection conn, PublicKey publicKey, GetRequestOptions options)
      throws AtException {
    try {

      // send public lookup command and decode the response
      String plookupCommand = VerbBuilders.plookupCommandBuilder()
          .key(publicKey)
          .bypassCache(options != null ? options.isBypassCache() : null)
          .operation(all)
          .build();
      String plookupResponse = conn.sendSync(plookupCommand);
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

  public static void put(AtClientConnection conn, AtKeys keys, PublicKey publicKey, String value) throws AtException {
    try {

      // add a signature to the metadata
      Metadata metadata = Metadata.builder()
          .dataSignature(signSHA256RSA(value, keys.getEncryptPrivateKey()))
          .build();
      publicKey.updateMissingMetadata(metadata);

      // send and update command
      String updateCommand = VerbBuilders.updateCommandBuilder().key(publicKey).value(value).build();
      String updateResponse = conn.sendSync(updateCommand);

      // verify the response
      matchDataInt(throwExceptionIfError(updateResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
