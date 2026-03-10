package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.DataResponses.*;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.util.EncryptionUtils.*;
import static org.atsign.client.impl.common.EnrollmentId.createEnrollmentId;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;
import static org.atsign.client.impl.commands.CommandBuilders.EnrollParameters.ENCRYPTED_APKAM_SYMMETRIC_KEY;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.atsign.client.api.AtKeyNames;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.common.EnrollmentId;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.AtSign;

/**
 * Atsign Protocol utilitiy code that relates to onboarding and enrolling atsigns.
 */
public class EnrollCommands {

  /**
   * Performs the onboarding workflow which sets up the manage keys for an atserver.
   *
   * @param executor A executor to an atserver command interface.
   * @param atSign The AtSign that corresponds to the executor.
   * @param keys The {@link AtKeys} for the {@link AtSign}.
   * @param cramSecret The CRAM secret.
   * @param appName The app name for this first enrollment.
   * @param deviceName The device name for this first enrollment.
   * @param deleteCramKey Whether the CRAM key should be removed from the AtServer.
   * @return a new {@link AtKeys} instance that has the enrollment id set
   * @throws AtException If the enrollment fails.
   */
  public static AtKeys onboard(AtCommandExecutor executor,
                               AtSign atSign,
                               AtKeys keys,
                               String cramSecret,
                               String appName,
                               String deviceName,
                               boolean deleteCramKey)
      throws AtException {
    try {

      // verify that the executor is connected to the atsigns atserver
      String scanCommand = CommandBuilders.scanCommandBuilder().build();
      String scanResponse = throwExceptionIfError(executor.sendSync(scanCommand));
      List<String> rawKeys = matchDataJsonListOfStrings(scanResponse);
      if (!rawKeys.contains("signing_publickey" + atSign)) {
        throw new IllegalStateException("not connected to the atsign's at server");
      }

      // authenticate with CRAM
      AuthenticationCommands.authenticateWithCram(executor, atSign, cramSecret);

      // send an enroll request which should automatically be approved after CRAM authentication
      String requestCommand = CommandBuilders.enrollCommandBuilder()
          .operation(CommandBuilders.EnrollOperation.request)
          .appName(appName)
          .deviceName(deviceName)
          .apkamPublicKey(keys.getApkamPublicKey())
          .build();
      String requestResponse = executor.sendSync(requestCommand);
      Map<String, String> response = matchDataJsonMapOfStrings(throwExceptionIfError(requestResponse));
      checkStatus(response, "approved");

      // update the AtKeys with the enrollment id
      keys = keys.toBuilder()
          .enrollmentId(createEnrollmentId(response.get("enrollmentId")))
          .build();

      // authenticate with PKAM
      AuthenticationCommands.authenticateWithPkam(executor, atSign, keys);

      // explicitly store the public encryption key in the atserver
      String updateCommand = CommandBuilders.updateCommandBuilder()
          .sharedBy(atSign)
          .keyName(AtKeyNames.PUBLIC_ENCRYPT)
          .isPublic(true)
          .value(keys.getEncryptPublicKey())
          .build();
      String updateResponse = executor.sendSync(updateCommand);
      matchDataInt(throwExceptionIfError(updateResponse));

      if (deleteCramKey) {
        deleteCramSecret(executor);
      }

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    return keys;
  }

  public static void deleteCramSecret(AtCommandExecutor executor) throws AtException {
    KeyCommands.deleteKey(executor, AtKeyNames.PRIVATE_AT_SECRET);
  }

  public static String otp(AtCommandExecutor executor) throws AtException {
    try {

      // send otp command
      String otpCommand = CommandBuilders.otpCommandBuilder().build();
      String otpResponse = executor.sendSync(otpCommand);

      // verify that response is an OTP
      return matchDataStringNoWhitespace(throwExceptionIfError(otpResponse));

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static AtKeys enroll(AtCommandExecutor executor,
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
    String lookupCommand = CommandBuilders.lookupCommandBuilder()
        .keyName(AtKeyNames.PUBLIC_ENCRYPT)
        .sharedBy(atSign)
        .build();
    String lookupResponse = executor.sendSync(lookupCommand);
    String publicKey = matchDataStringNoWhitespace(throwExceptionIfError(lookupResponse));

    // send an enroll request and verify that the status is pending
    String requestCommand = CommandBuilders.enrollCommandBuilder()
        .operation(CommandBuilders.EnrollOperation.request)
        .appName(appName)
        .deviceName(deviceName)
        .apkamPublicKey(keys.getApkamPublicKey())
        .apkamSymmetricKey(rsaEncryptToBase64(keys.getApkamSymmetricKey(), publicKey))
        .otp(otp)
        .namespaces(namespaces)
        .build();
    String requestResponse = executor.sendSync(requestCommand);
    Map<String, String> map = matchDataJsonMapOfStrings(throwExceptionIfError(requestResponse));
    checkStatus(map, "pending");

    // return a copy of the provided AtKeys with the public encryption key and enrollment id set
    return keys.toBuilder()
        .encryptPublicKey(publicKey)
        .enrollmentId(EnrollmentId.createEnrollmentId(map.get("enrollmentId")))
        .build();
  }

  public static AtKeys complete(AtCommandExecutor executor, AtSign atSign, AtKeys keys) throws AtException {

    // attempt to authenticate with PKAM, this will succeed once the enroll request is approved
    AuthenticationCommands.authenticateWithPkam(executor, atSign, keys);

    // Use the keys:get command to obtain the private encryption key and self encryption key
    String selfEncryptKey = keysGetSelfEncryptKey(executor, atSign, keys);
    String encryptPrivateKey = keysGetEncryptPrivateKey(executor, atSign, keys);

    // return a copy of the provided AtKeys with the private encryption key and self encryption key set
    return keys.toBuilder()
        .selfEncryptKey(selfEncryptKey)
        .encryptPrivateKey(encryptPrivateKey)
        .build();

  }

  public static List<EnrollmentId> list(AtCommandExecutor executor, String status) throws AtException {
    try {

      // get the list of enrollment requests that have the status
      String listCommand = CommandBuilders.enrollCommandBuilder()
          .operation(CommandBuilders.EnrollOperation.list)
          .status(status)
          .build();
      String listResponse = executor.sendSync(listCommand);
      Map<String, Object> map = matchDataJsonMapOfObjects(throwExceptionIfError(listResponse), true);

      // return the list enrollment ids, extracted from the keys
      return map.keySet().stream()
          .map(EnrollCommands::inferEnrollmentId)
          .collect(Collectors.toList());

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void approve(AtCommandExecutor executor, AtKeys keys, EnrollmentId enrollmentId) throws AtException {
    try {

      // fetch the request and decrypt the apkam symmetric key that will be used encrypt the shared keys
      String fetchCommand = CommandBuilders.enrollCommandBuilder()
          .operation(CommandBuilders.EnrollOperation.fetch)
          .enrollmentId(enrollmentId)
          .build();
      String fetchResponse = executor.sendSync(fetchCommand);
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
      String approveCommand = CommandBuilders.enrollCommandBuilder()
          .operation(CommandBuilders.EnrollOperation.approve)
          .enrollmentId(enrollmentId)
          .encryptPrivateKey(encryptPrivateKey)
          .encryptPrivateKeyIv(privateKeyIv)
          .selfEncryptKey(selfEncryptKey)
          .selfEncryptKeyIv(selfEncryptKeyIv)
          .build();
      String approveResponse = executor.sendSync(approveCommand);

      // check the response
      Map<String, String> response = matchDataJsonMapOfStrings(throwExceptionIfError(approveResponse));
      checkStatus(response, "approved");

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }

  }

  public static void deny(AtCommandExecutor executor, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(executor, "deny", enrollmentId, "denied");
  }

  public static void revoke(AtCommandExecutor executor, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(executor, "revoke", enrollmentId, "revoked");
  }

  public static void unrevoke(AtCommandExecutor executor, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(executor, "unrevoke", enrollmentId, "approved");
  }

  public static void delete(AtCommandExecutor executor, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(executor, "delete", enrollmentId, "deleted");
  }

  private static void singleArgEnrollAction(AtCommandExecutor executor,
                                            String action,
                                            EnrollmentId enrollmentId,
                                            String expectedStatus)
      throws Exception {
    String actionCommand = CommandBuilders.enrollCommandBuilder()
        .operation(CommandBuilders.EnrollOperation.valueOf(action))
        .enrollmentId(enrollmentId)
        .build();
    String actionResponse = executor.sendSync(actionCommand);
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

  private static String keysGetSelfEncryptKey(AtCommandExecutor executor, AtSign atSign, AtKeys keys)
      throws AtException {
    String keyName = keys.getEnrollmentId() + "." + "default_self_enc_key" + ".__manage" + atSign;
    return keysGet(executor, keyName, keys.getApkamSymmetricKey());
  }

  private static String keysGetEncryptPrivateKey(AtCommandExecutor executor, AtSign atSign, AtKeys keys)
      throws AtException {
    String keyName = keys.getEnrollmentId() + "." + "default_enc_private_key" + ".__manage" + atSign;
    return keysGet(executor, keyName, keys.getApkamSymmetricKey());
  }

  private static String keysGet(AtCommandExecutor executor, String keyName, String keyBase64) throws AtException {
    try {

      // send a key:get command
      String keysGetCommand = CommandBuilders.keysCommandBuilder()
          .operation(CommandBuilders.KeysOperation.get)
          .keyName(keyName)
          .build();
      String keyGetResponse = executor.sendSync(keysGetCommand);

      // unmarshall the encrypted value and the encryption iv that was used
      Map<String, String> map = matchDataJsonMapOfStrings(throwExceptionIfError(keyGetResponse));
      String encryptedKey = map.get("value");
      String iv = map.get("iv");

      // return the decrypted value
      return aesDecryptFromBase64(encryptedKey, keyBase64, iv);

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
