package org.atsign.client.connection.protocol;

import static org.atsign.client.api.AtKeyNames.toSharedByMeKeyName;
import static org.atsign.client.connection.protocol.Data.*;
import static org.atsign.client.connection.protocol.Error.throwExceptionIfError;
import static org.atsign.client.util.EncryptionUtil.*;
import static org.atsign.client.util.Preconditions.checkNotNull;
import static org.atsign.client.util.Preconditions.checkTrue;
import static org.atsign.common.VerbBuilders.LookupOperation.all;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.atsign.client.api.AtKeyNames;
import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.util.EncryptionUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.Metadata;
import org.atsign.common.VerbBuilders;
import org.atsign.common.exceptions.AtKeyNotFoundException;
import org.atsign.common.response_models.LookupResponse;

/**
 * Atsign protocol utility code that relates to "shared keys"
 *
 */

public class SharedKeys {

  public static String get(AtClientConnection conn, AtSign atSign, AtKeys keys, SharedKey key)
      throws AtException {
    if (key.sharedBy().equals(atSign)) {
      return getSharedByMe(conn, keys, key);
    } else if (key.sharedWith().equals(atSign)) {
      return getSharedByOther(conn, keys, key);
    } else {
      throw new IllegalArgumentException("the client atsign is neither the sharedBy or sharedWith");
    }
  }

  public static void put(AtClientConnection conn, AtSign atSign, AtKeys keys, SharedKey key, String value)
      throws AtException {
    checkTrue(key.sharedBy().equals(atSign), "sharedBy does not match this client's atsign");
    try {

      // get or create key for sharedBy - sharedWith
      String aesKey = getEncryptKeySharedByMe(conn, keys, key);
      if (aesKey == null) {
        aesKey = createEncryptKey(conn, keys, key);
      }

      // encrypt the value
      String iv = EncryptionUtil.generateRandomIvBase64(16);
      key.updateMissingMetadata(Metadata.builder().ivNonce(iv).build());
      String encrypted = aesEncryptToBase64(value, aesKey, iv);

      // send an update command
      String updateCommand = VerbBuilders.updateCommandBuilder().key(key).value(encrypted).build();
      String updateResponse = conn.sendSync(updateCommand);

      // verify the response
      matchDataInt(throwExceptionIfError(updateResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getSharedByMe(AtClientConnection conn, AtKeys keys, SharedKey key) throws AtException {
    try {

      // send local lookup command and decode
      String llookupCommand = VerbBuilders.llookupCommandBuilder().key(key).operation(all).build();
      LookupResponse llookupResponse = matchLookupResponse(throwExceptionIfError(conn.sendSync(llookupCommand)));

      // get my encrypt key for sharedBy sharedWith
      String aesKey = checkNotNull(getEncryptKeySharedByMe(conn, keys, key), key + " not found");

      // return decrypted value
      return aesDecryptFromBase64(llookupResponse.data, aesKey, llookupResponse.metaData.ivNonce());

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getSharedByOther(AtClientConnection conn, AtKeys keys, SharedKey key) throws AtException {
    try {

      // send lookup command and decode
      String lookupCommand = VerbBuilders.lookupCommandBuilder().key(key).operation(all).build();
      LookupResponse lookupResponse = matchLookupResponse(throwExceptionIfError(conn.sendSync(lookupCommand)));

      // get my encrypt key for sharedBy sharedWith
      String shareEncryptionKey = getEncryptKeySharedByOther(conn, keys, key);

      // return decrypted value
      return aesDecryptFromBase64(lookupResponse.data, shareEncryptionKey, lookupResponse.metaData.ivNonce());

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getEncryptKeySharedByMe(AtClientConnection conn, AtKeys keys, SharedKey key) throws AtException {
    try {

      String keyName = AtKeyNames.toSharedByMeKeyName(key.sharedWith());
      String aesKey = keys.get(keyName);
      if (aesKey != null) {
        return aesKey;
      }

      // send local lookup command for sharedKey
      String llookupCommand = VerbBuilders.llookupCommandBuilder()
          .keyName(toSharedByMeKeyName(key.sharedWith()))
          .sharedBy(key.sharedBy())
          .build();
      String llookupResponse = conn.sendSync(llookupCommand);

      try {
        // decrypt the key
        String encrypted = Data.matchDataStringNoWhitespace(throwExceptionIfError(llookupResponse));
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

  public static String getEncryptKeySharedByOther(AtClientConnection conn, AtKeys keys, SharedKey key)
      throws AtException {
    try {

      // check in Keys cache
      String keyName = AtKeyNames.toSharedWithMeKeyName(key.sharedBy(), key.sharedWith());
      String aesKey = keys.get(keyName);
      if (aesKey != null) {
        return aesKey;
      }

      // otherwise send lookup
      String lookupCommand = VerbBuilders.lookupCommandBuilder()
          .keyName(AtKeyNames.SHARED_KEY)
          .sharedBy(key.sharedBy())
          .build();
      String lookupResponse = conn.sendSync(lookupCommand);

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

  public static String createEncryptKey(AtClientConnection connection, AtKeys keys, SharedKey key) throws AtException {
    try {

      // generate a new encrypt key
      String aesKey = EncryptionUtil.generateAESKeyBase64();

      // compose an update command to store this key encrypted with this (sharedBy) atsign's public key
      String encryptedForMe = rsaEncryptToBase64(aesKey, keys.getEncryptPublicKey());
      String updateForUsCommand = VerbBuilders.updateCommandBuilder()
          .keyName(toSharedByMeKeyName(key.sharedWith()))
          .sharedBy(key.sharedBy())
          .value(encryptedForMe)
          .build();

      // get the other (sharedWith) atsign's public key
      String otherPublicKey = getEncryptKey(connection, key.sharedWith());

      // compose an update command to store this key encrypted with the other (sharedWith) atsign's public key
      String encryptedForOther = rsaEncryptToBase64(aesKey, otherPublicKey);
      String updateForOtherCommand = VerbBuilders.updateCommandBuilder()
          .keyName(AtKeyNames.SHARED_KEY)
          .sharedBy(key.sharedBy())
          .sharedWith(key.sharedWith())
          .ttr(TimeUnit.HOURS.toMillis(24))
          .value(encryptedForOther)
          .build();

      // send the update commands
      connection.sendSync(updateForUsCommand);
      connection.sendSync(updateForOtherCommand);

      keys.put(AtKeyNames.toSharedByMeKeyName(key.sharedWith()), aesKey);

      // return the new
      return aesKey;

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getEncryptKey(AtClientConnection connection, AtSign sharedBy) throws AtException {
    try {

      // send plookup for atsign's public encryption key
      String plookupCommand = VerbBuilders.plookupCommandBuilder()
          .keyName(AtKeyNames.PUBLIC_ENCRYPT)
          .sharedBy(sharedBy)
          .build();
      String plookupResponse = connection.sendSync(plookupCommand);

      // return key
      return matchDataStringNoWhitespace(throwExceptionIfError(plookupResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
