package org.atsign.client.connection.protocol;

import static org.atsign.client.connection.protocol.Data.*;
import static org.atsign.client.connection.protocol.Error.throwExceptionIfError;
import static org.atsign.client.util.EncryptionUtil.*;
import static org.atsign.client.util.EnrollmentId.createEnrollmentId;
import static org.atsign.client.util.Preconditions.checkNotNull;
import static org.atsign.common.VerbBuilders.EnrollParameters.ENCRYPTED_APKAM_SYMMETRIC_KEY;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.atsign.client.api.AtKeyNames;
import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.util.EnrollmentId;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.VerbBuilders;

/**
 * Atsign Protocol utilitiy code that relates to onboarding and enrolling atsigns.
 */
public class Enroll {

  /**
   * Performs the onboarding workflow which sets up the manage keys for an atserver.
   *
   * @param connection A connection to an atserver command interface
   * @param atSign The AtSign that corresponds to the connection
   * @param keys The {@link AtKeys}
   * @param cramSecret
   * @param appName
   * @param deviceName
   * @param deleteCramKey
   * @return a new {@link AtKeys} instance that has the enrollment id set
   * @throws AtException
   */
  public static AtKeys onboard(AtClientConnection connection,
                               AtSign atSign,
                               AtKeys keys,
                               String cramSecret,
                               String appName,
                               String deviceName,
                               boolean deleteCramKey)
      throws AtException {
    try {

      // verify that the connection is connected to the atsigns atserver
      String scanCommand = VerbBuilders.scanCommandBuilder().build();
      String scanResponse = throwExceptionIfError(connection.sendSync(scanCommand));
      List<String> rawKeys = matchDataJsonListOfStrings(scanResponse);
      if (!rawKeys.contains("signing_publickey" + atSign)) {
        throw new IllegalStateException("not connected to the atsign's at server");
      }

      // authenticate with CRAM
      Authentication.authenticateWithCram(connection, atSign, cramSecret);

      // send an enroll request which should automatically be approved after CRAM authentication
      String requestCommand = VerbBuilders.enrollCommandBuilder()
          .operation(VerbBuilders.EnrollOperation.request)
          .appName(appName)
          .deviceName(deviceName)
          .apkamPublicKey(keys.getApkamPublicKey())
          .build();
      String requestResponse = connection.sendSync(requestCommand);
      Map<String, String> response = matchDataJsonMapOfStrings(throwExceptionIfError(requestResponse));
      checkStatus(response, "approved");

      // update the AtKeys with the enrollment id
      keys = keys.toBuilder()
          .enrollmentId(createEnrollmentId(response.get("enrollmentId")))
          .build();

      // authenticate with PKAM
      Authentication.authenticateWithPkam(connection, atSign, keys);

      // explicitly store the public encryption key in the atserver
      String updateCommand = VerbBuilders.updateCommandBuilder()
          .sharedBy(atSign)
          .keyName(AtKeyNames.PUBLIC_ENCRYPT)
          .isPublic(true)
          .value(keys.getEncryptPublicKey())
          .build();
      String updateResponse = connection.sendSync(updateCommand);
      matchDataInt(throwExceptionIfError(updateResponse));

      if (deleteCramKey) {
        deleteCramSecret(connection);
      }

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    return keys;
  }

  public static void deleteCramSecret(AtClientConnection connection) throws AtException {
    Keys.deleteKey(connection, AtKeyNames.PRIVATE_AT_SECRET);
  }

  public static String otp(AtClientConnection connection) throws AtException {
    try {

      // send otp command
      String otpCommand = VerbBuilders.otpCommandBuilder().build();
      String otpResponse = connection.sendSync(otpCommand);

      // verify that response is an OTP
      return Data.matchDataStringNoWhitespace(throwExceptionIfError(otpResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static AtKeys enroll(AtClientConnection connection,
                              AtSign atSign,
                              AtKeys keys,
                              String otp,
                              String appName,
                              String deviceName,
                              Map<String, String> namespaces)
      throws Exception {

    checkNotNull(keys.getApkamPublicKey(), "apkam public key not set");
    checkNotNull(keys.getApkamSymmetricKey(), "apkam symmetric key not set");

    // lookup the public encryption key for the atserver's atsign, this will have been set during onboarding
    String lookupCommand = VerbBuilders.lookupCommandBuilder()
        .keyName(AtKeyNames.PUBLIC_ENCRYPT)
        .sharedBy(atSign)
        .build();
    String lookupResponse = connection.sendSync(lookupCommand);
    String publicKey = matchDataStringNoWhitespace(throwExceptionIfError(lookupResponse));

    // send an enroll request and verify that the status is pending
    String requestCommand = VerbBuilders.enrollCommandBuilder()
        .operation(VerbBuilders.EnrollOperation.request)
        .appName(appName)
        .deviceName(deviceName)
        .apkamPublicKey(keys.getApkamPublicKey())
        .apkamSymmetricKey(rsaEncryptToBase64(keys.getApkamSymmetricKey(), publicKey))
        .otp(otp)
        .namespaces(namespaces)
        .build();
    String requestResponse = connection.sendSync(requestCommand);
    Map<String, String> map = matchDataJsonMapOfStrings(throwExceptionIfError(requestResponse));
    checkStatus(map, "pending");

    // return a copy of the provided AtKeys with the public encryption key and enrollment id set
    return keys.toBuilder()
        .encryptPublicKey(publicKey)
        .enrollmentId(EnrollmentId.createEnrollmentId(map.get("enrollmentId")))
        .build();
  }

  public static AtKeys complete(AtClientConnection connection, AtSign atSign, AtKeys keys) throws AtException {

    // attempt to authenticate with PKAM, this will succeed once the enroll request is approved
    Authentication.authenticateWithPkam(connection, atSign, keys);

    // Use the keys:get command to obtain the private encryption key and self encryption key
    String selfEncryptKey = keysGetSelfEncryptKey(connection, atSign, keys);
    String encryptPrivateKey = keysGetEncryptPrivateKey(connection, atSign, keys);

    // return a copy of the provided AtKeys with the private encryption key and self encryption key set
    return keys.toBuilder()
        .selfEncryptKey(selfEncryptKey)
        .encryptPrivateKey(encryptPrivateKey)
        .build();

  }

  public static List<EnrollmentId> list(AtClientConnection connection, String status) throws AtException {
    try {

      // get the list of enrollment requests that have the status
      String listCommand = VerbBuilders.enrollCommandBuilder()
          .operation(VerbBuilders.EnrollOperation.list)
          .status(status)
          .build();
      String listResponse = connection.sendSync(listCommand);
      Map<String, Object> map = matchDataJsonMapOfObjects(throwExceptionIfError(listResponse), true);

      // return the list enrollment ids, extracted from the keys
      return map.keySet().stream()
          .map(Enroll::inferEnrollmentId)
          .collect(Collectors.toList());

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void approve(AtClientConnection connection, AtKeys keys, EnrollmentId enrollmentId) throws AtException {
    try {

      // fetch the request and decrypt the apkam symmetric key that will be used encrypt the shared keys
      String fetchCommand = VerbBuilders.enrollCommandBuilder()
          .operation(VerbBuilders.EnrollOperation.fetch)
          .enrollmentId(enrollmentId)
          .build();
      String fetchResponse = connection.sendSync(fetchCommand);
      Map<String, Object> request = matchDataJsonMapOfObjects(throwExceptionIfError(fetchResponse));
      checkStatus(request, "pending");
      String encryptedApkamSymmetricKey = (String) request.get(ENCRYPTED_APKAM_SYMMETRIC_KEY);
      String key = rsaDecryptFromBase64(encryptedApkamSymmetricKey, keys.getEncryptPrivateKey());

      // encrypt the private encryption key and the self encryption key with the apkam symmetric key
      String privateKeyIv = generateRandomIvBase64(16);
      String encryptPrivateKey = aesEncryptToBase64(keys.getEncryptPrivateKey(), key, privateKeyIv);
      String selfEncryptKeyIv = generateRandomIvBase64(16);
      String selfEncryptKey = aesEncryptToBase64(keys.getSelfEncryptKey(), key, selfEncryptKeyIv);

      // approve the request
      String approveCommand = VerbBuilders.enrollCommandBuilder()
          .operation(VerbBuilders.EnrollOperation.approve)
          .enrollmentId(enrollmentId)
          .encryptPrivateKey(encryptPrivateKey)
          .encryptPrivateKeyIv(privateKeyIv)
          .selfEncryptKey(selfEncryptKey)
          .selfEncryptKeyIv(selfEncryptKeyIv)
          .build();
      String approveResponse = connection.sendSync(approveCommand);

      // check the response
      Map<String, String> response = matchDataJsonMapOfStrings(throwExceptionIfError(approveResponse));
      checkStatus(response, "approved");

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }

  }

  public static void deny(AtClientConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "deny", enrollmentId, "denied");
  }

  public static void revoke(AtClientConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "revoke", enrollmentId, "revoked");
  }

  public static void unrevoke(AtClientConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "unrevoke", enrollmentId, "approved");
  }

  public static void delete(AtClientConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "delete", enrollmentId, "deleted");
  }

  private static void singleArgEnrollAction(AtClientConnection connection,
                                            String action,
                                            EnrollmentId enrollmentId,
                                            String expectedStatus)
      throws Exception {
    String actionCommand = VerbBuilders.enrollCommandBuilder()
        .operation(VerbBuilders.EnrollOperation.valueOf(action))
        .enrollmentId(enrollmentId)
        .build();
    String actionResponse = connection.sendSync(actionCommand);
    Map<String, String> map = matchDataJsonMapOfStrings(actionResponse);
    checkStatus(map, expectedStatus);
  }

  private static EnrollmentId inferEnrollmentId(String key) {
    int endIndex = key.indexOf('.');
    if (key.contains("__manage") && endIndex > 0) {
      return EnrollmentId.createEnrollmentId(key.substring(0, endIndex));
    } else {
      throw new RuntimeException(key + " doesn't match expected enrollment key pattern");
    }
  }

  private static void checkStatus(Map<String, ? extends Object> map, String expectedStatus) {
    String actualStatus = (String) map.get("status");
    if (!Objects.equals(actualStatus, expectedStatus)) {
      throw new RuntimeException("status is " + actualStatus);
    }
  }

  private static String keysGetSelfEncryptKey(AtClientConnection connection, AtSign atSign, AtKeys keys)
      throws AtException {
    String keyName = keys.getEnrollmentId() + "." + "default_self_enc_key" + ".__manage" + atSign;
    return keysGet(connection, keyName, keys.getApkamSymmetricKey());
  }

  private static String keysGetEncryptPrivateKey(AtClientConnection connection, AtSign atSign, AtKeys keys)
      throws AtException {
    String keyName = keys.getEnrollmentId() + "." + "default_enc_private_key" + ".__manage" + atSign;
    return keysGet(connection, keyName, keys.getApkamSymmetricKey());
  }

  private static String keysGet(AtClientConnection connection, String keyName, String keyBase64) throws AtException {
    try {

      // send a key:get command
      String keysGetCommand = VerbBuilders.keysCommandBuilder()
          .operation(VerbBuilders.KeysOperation.get)
          .keyName(keyName)
          .build();
      String keyGetResponse = connection.sendSync(keysGetCommand);

      // unmarshall the encrypted value and the encryption iv that was used
      Map<String, String> map = matchDataJsonMapOfStrings(Error.throwExceptionIfError(keyGetResponse));
      String encryptedKey = map.get("value");
      String iv = map.get("iv");

      // return the decrypted value
      return aesDecryptFromBase64(encryptedKey, keyBase64, iv);

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
