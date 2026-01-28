package org.atsign.client.cli;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.atsign.client.util.EncryptionUtil.aesDecryptFromBase64;
import static org.atsign.client.util.EncryptionUtil.aesEncryptToBase64;
import static org.atsign.client.util.EncryptionUtil.generateAESKeyBase64;
import static org.atsign.client.util.EncryptionUtil.generateRSAKeyPair;
import static org.atsign.client.util.EncryptionUtil.generateRandomIvBase64;
import static org.atsign.client.util.EncryptionUtil.rsaDecryptFromBase64;
import static org.atsign.client.util.EncryptionUtil.rsaEncryptToBase64;
import static org.atsign.client.util.EnrollmentId.createEnrollmentId;
import static org.atsign.client.util.KeysUtil.saveKeys;
import static org.atsign.client.util.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.client.util.AuthUtil;
import org.atsign.client.util.EnrollmentId;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtUnauthenticatedException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Utility (and CommandLineInterface) for onboarding and enrolling atSigns and AtSign application
 * devices
 */
@Command(
    mixinStandardHelpOptions = true)
public class Activate extends AbstractCli<Activate> implements Callable<Integer> {

  enum Action {
    onboard, enroll, otp, list, approve, deny, revoke, unrevoke
  };

  public static final String DEFAULT_FIRST_APP = "firstApp";
  public static final String DEFAULT_FIRST_DEVICE = "firstDevice";

  @Parameters(index = "0", description = "onboard action to perform")
  private Action action;

  private String appName;
  private String deviceName;
  private boolean overwriteKeysFile = false;
  private String cramSecret;
  private boolean deleteCramKey = true;
  private AtKeys keys;
  private EnrollmentId enrollmentId;
  private String requestStatus = "pending";
  private String otp;
  private Map<String, String> namespaces = new LinkedHashMap<>();
  private int completionRetries = 5;


  public static void main(String[] args) {
    System.exit(execute(args));
  }

  public static int execute(String[] args) {
    return new CommandLine(new Activate())
        .setUsageHelpWidth(80)
        .setAllowOptionsAsOptionParameters(true)
        .execute(args);
  }

  public Activate() {
    // TODO replace with stack check after JDK upgrade
    deleteCramKey = !System.getProperty("test.mode", "false").equalsIgnoreCase("true");
  }

  @Override
  public Integer call() throws Exception {
    switch (action) {
      case onboard:
        System.out.println(onboard());
        break;
      case otp:
        System.out.println(otp());
        break;
      case enroll:
        enroll();
        complete(completionRetries, 1, TimeUnit.SECONDS);
        break;
      case list:
        list().forEach(System.out::println);
        break;
      case approve:
        approve();
        break;
      case deny:
        deny();
        break;
      case revoke:
        revoke();
        break;
      case unrevoke:
        unrevoke();
        break;
      default:
        throw new Exception("no action");
    }
    return 0;
  }

  @Override
  protected Activate self() {
    return this;
  }

  @Option(names = {"-p", "--app"}, description = "The name of the app being enrolled")
  public Activate setAppName(String appName) {
    this.appName = appName;
    return self();
  }

  @Option(names = {"-d", "--device"}, description = " A name for the device on which this app is running")
  public Activate setDeviceName(String deviceName) {
    this.deviceName = deviceName;
    return self();
  }

  public Activate allowOverwriteKeysFile() {
    this.overwriteKeysFile = true;
    return self();
  }

  @Option(names = {"-c", "--cramkey"}, description = "CRAM key")
  public Activate setCramSecret(String cramSecret) {
    this.cramSecret = cramSecret;
    return self();
  }

  public Activate setNoDeleteCramKey() {
    this.deleteCramKey = false;
    return self();
  }

  @Option(names = {"-i", "--enrollmentId"}, description = "the ID of the enrollment request")
  public void setEnrollmentId(String s) {
    this.enrollmentId = EnrollmentId.createEnrollmentId(s);
  }

  @Option(names = {"-es", " --enrollmentStatus"}, description = "A specific status to filter by",
      defaultValue = "pending")
  public void setRequestStatus(String s) {
    this.requestStatus = s;
  }

  @Option(names = {"-s", "--passcode"}, description = "passcode to present with this enrollment request (OTP)")
  public Activate setOtp(String otp) {
    this.otp = otp;
    return self();
  }

  @Option(names = {"-n", "--namespaces"}, description = "the namespace access list as comma-separated list " +
      "of name:value pairs e.g. \"ns:rw,contacts:rw,__manage:rw\"")
  public Activate setNamespaces(String namespaces) {
    for (String namespace : namespaces.split(",")) {
      String[] parts = namespace.split(":");
      addNamespace(parts[0], parts[1]);
    }
    return self();
  }

  @Option(names = {"--max-retries"}, defaultValue = "5",
      description = " number of times to check for approval before giving up")
  public Activate setCompletionRetries(int completionRetries) {
    this.completionRetries = completionRetries;
    return self();
  }

  public Activate addNamespace(String namespace, String accessControl) {
    this.namespaces.put(namespace, accessControl);
    return self();
  }

  public EnrollmentId onboard() throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      return onboard(connection);
    }
  }

  public EnrollmentId onboard(AtSecondaryConnection connection) throws Exception {
    File file = getAtKeysFile(keysFile, atSign);
    if (!overwriteKeysFile) {
      checkNotExists(file);
    }
    checkAtServerMatchesAtSign(connection, atSign);
    authenticateWithCram(connection, atSign, cramSecret);
    AtKeys keys = generateAtKeys(true);
    EnrollmentId enrollmentId = enroll(connection,
                                       keys,
                                       ensureNotNull(appName, DEFAULT_FIRST_APP),
                                       ensureNotNull(deviceName, DEFAULT_FIRST_DEVICE));
    keys.setEnrollmentId(enrollmentId);
    authenticateWithApkam(connection, atSign, keys);
    saveKeys(keys, file);
    storeEncryptPublicKey(connection, atSign, keys);
    if (deleteCramKey) {
      deleteCramSecret(connection);
    }
    return enrollmentId;
  }

  public Activate authenticate(AtSecondaryConnection connection) throws Exception {
    File file = checkExists(getAtKeysFile(keysFile, atSign));
    keys = KeysUtil.loadKeys(file);
    authenticateWithApkam(connection, atSign, keys);
    return this;
  }

  public List<EnrollmentId> list() throws Exception {
    return list(requestStatus);
  }

  public List<EnrollmentId> list(String status) throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      return authenticate(connection).list(connection, status);
    }
  }

  public List<EnrollmentId> list(AtSecondaryConnection connection, String status) throws Exception {
    String command = "enroll:list:" + encodeAsJson(singletonMap("enrollmentStatusFilter", singletonList(status)));
    return matchDataJsonMapOfObjects(connection.executeCommand(command), true).keySet().stream()
        .map(Activate::inferEnrollmentId)
        .collect(Collectors.toList());
  }

  private static EnrollmentId inferEnrollmentId(String key) {
    int endIndex = key.indexOf('.');
    if (key.contains("__manage") && endIndex > 0) {
      return EnrollmentId.createEnrollmentId(key.substring(0, endIndex));
    } else {
      throw new RuntimeException(key + " doesn't match expected enrollment key pattern");
    }
  }

  public void approve() throws Exception {
    approve(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void approve(EnrollmentId enrollmentId) throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      authenticate(connection).approve(connection, enrollmentId);
    }
  }

  public void approve(AtSecondaryConnection connection, EnrollmentId enrollmentId) throws Exception {
    String key = fetchApkamSymmetricKey(connection, enrollmentId);
    String privateKeyIv = generateRandomIvBase64(16);
    String encryptPrivateKey = aesEncryptToBase64(keys.getEncryptPrivateKey(), key, privateKeyIv);
    String selfKeyIv = generateRandomIvBase64(16);
    String selfEncryptKey = aesEncryptToBase64(keys.getSelfEncryptKey(), key, selfKeyIv);
    String json = encodeKeyValuesAsJson("enrollmentId", enrollmentId,
                                        "encryptedDefaultEncryptionPrivateKey", encryptPrivateKey,
                                        "encPrivateKeyIV", privateKeyIv,
                                        "encryptedDefaultSelfEncryptionKey", selfEncryptKey,
                                        "selfEncKeyIV", selfKeyIv);

    Map<String, String> response = matchDataJsonMapOfStrings(connection.executeCommand("enroll:approve:" + json));
    if (!"approved".equals(response.get("status"))) {
      throw new RuntimeException("status is not approved : " + response.get("status"));
    }
  }

  private String fetchApkamSymmetricKey(AtSecondaryConnection connection, EnrollmentId enrollmentId) throws Exception {
    String command = "enroll:fetch:" + encodeKeyValuesAsJson("enrollmentId", enrollmentId);
    Map<String, Object> request = matchDataJsonMapOfObjects(connection.executeCommand(command));
    if (!"pending".equals(request.get("status"))) {
      throw new RuntimeException("status is not pending : " + request.get("status"));
    }
    String encryptedApkamSymmetricKey = (String) request.get("encryptedAPKAMSymmetricKey");
    return rsaDecryptFromBase64(encryptedApkamSymmetricKey, keys.getEncryptPrivateKey());
  }

  public void deny() throws Exception {
    deny(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void deny(EnrollmentId enrollmentId) throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      authenticate(connection).deny(connection, enrollmentId);
    }
  }

  public void revoke() throws Exception {
    revoke(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void revoke(EnrollmentId enrollmentId) throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      authenticate(connection).revoke(connection, enrollmentId);
    }
  }

  public void unrevoke() throws Exception {
    unrevoke(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void unrevoke(EnrollmentId enrollmentId) throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      authenticate(connection).unrevoke(connection, enrollmentId);
    }
  }

  public void delete(EnrollmentId enrollmentId) throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      authenticate(connection).delete(connection, enrollmentId);
    }
  }

  public void deny(AtSecondaryConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "deny", enrollmentId, "denied");
  }

  public void revoke(AtSecondaryConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "revoke", enrollmentId, "revoked");
  }

  public void unrevoke(AtSecondaryConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "unrevoke", enrollmentId, "approved");
  }

  public void delete(AtSecondaryConnection connection, EnrollmentId enrollmentId) throws Exception {
    singleArgEnrollAction(connection, "delete", enrollmentId, "deleted");
  }

  public String otp() throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      return otp(connection);
    }
  }

  public String otp(AtSecondaryConnection connection) throws IOException, AtException {
    File file = checkExists(getAtKeysFile(keysFile, atSign));
    AtKeys keys = KeysUtil.loadKeys(file);
    authenticateWithApkam(connection, atSign, keys);
    return match(connection.executeCommand("otp:get"), DATA_NON_WHITESPACE);
  }

  public List<String> scan() throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      return scan(connection);
    }
  }

  public List<String> scan(AtSecondaryConnection connection) throws Exception {
    return matchDataJsonListOfStrings(connection.executeCommand("scan:showHidden:true .*"));
  }

  private void singleArgEnrollAction(AtSecondaryConnection connection,
                                     String action,
                                     EnrollmentId enrollmentId,
                                     String expectedStatus)
      throws Exception {
    String command = "enroll:" + action + ":" + encodeKeyValuesAsJson("enrollmentId", enrollmentId);
    Map<String, String> map = matchDataJsonMapOfStrings(connection.executeCommand(command));
    if (!expectedStatus.equals(map.get("status"))) {
      throw new RuntimeException("status is not " + expectedStatus + " : " + map.get("status"));
    }
  }

  protected static void authenticateWithCram(AtSecondaryConnection connection, AtSign atSign, String cramSecret)
      throws AtException, IOException {
    checkNotNull(cramSecret, "CRAM secret not set");
    new AuthUtil().authenticateWithCram(connection, atSign, cramSecret);
  }

  private static EnrollmentId enroll(AtSecondaryConnection connection,
                                     AtKeys keys,
                                     String appName,
                                     String deviceName)
      throws Exception {
    String json = encodeKeyValuesAsJson(
                                        "appName", appName,
                                        "deviceName", deviceName,
                                        "apkamPublicKey", keys.getApkamPublicKey());
    Map<String, String> response = matchDataJsonMapOfStrings(connection.executeCommand("enroll:request:" + json));
    if (!response.get("status").equals("approved")) {
      throw new RuntimeException("enroll request failed, expected status approved : " + response);
    }
    return createEnrollmentId(response.get("enrollmentId"));
  }

  protected static void storeEncryptPublicKey(AtSecondaryConnection connection, AtSign atSign, AtKeys keys)
      throws IOException {
    match(connection.executeCommand("update:public:publickey" + atSign + " " + keys.getEncryptPublicKey()), DATA_INT);
  }

  protected static void deleteCramSecret(AtSecondaryConnection connection) {
    deleteKey(connection, "privatekey:at_secret");
  }

  protected static AtKeys generateAtKeys(boolean generateEncryptionKeyPair) throws NoSuchAlgorithmException {
    AtKeys keys = new AtKeys()
        .setSelfEncryptKey(generateAESKeyBase64())
        .setApkamKeyPair(generateRSAKeyPair())
        .setApkamSymmetricKey(generateAESKeyBase64());
    if (generateEncryptionKeyPair) {
      keys.setEncryptKeyPair(generateRSAKeyPair());
    }
    return keys;
  }

  public EnrollmentId enroll() throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      return enroll(connection);
    }
  }

  public EnrollmentId enroll(AtSecondaryConnection connection) throws Exception {
    String publicKey = matchDataString(connection.executeCommand("lookup:publickey" + atSign));
    File file = keysFile;
    if (!overwriteKeysFile) {
      checkNotExists(file);
    }
    AtKeys keys = generateAtKeys(false);
    keys.setEncryptPublicKey(publicKey);
    keys.setEnrollmentId(enroll(connection, keys));
    KeysUtil.saveKeys(keys, keysFile);
    return keys.getEnrollmentId();
  }

  private EnrollmentId enroll(AtSecondaryConnection connection, AtKeys keys) throws Exception {
    System.out.println(keys.getApkamSymmetricKey());
    Map<String, Object> args = toObjectMap("appName", appName,
                                           "deviceName", deviceName,
                                           "apkamPublicKey", keys.getApkamPublicKey(),
                                           "encryptedAPKAMSymmetricKey",
                                           rsaEncryptToBase64(keys.getApkamSymmetricKey(), keys.getEncryptPublicKey()),
                                           "otp", otp,
                                           "namespaces", namespaces,
                                           "apkamKeysExpiryInMillis", 0);
    String command = "enroll:request:" + encodeAsJson(args);
    Map<String, String> response = matchDataJsonMapOfStrings(connection.executeCommand(command));
    if ("pending".equals(response.get("status"))) {
      return EnrollmentId.createEnrollmentId(response.get("enrollmentId"));
    } else {
      throw new RuntimeException("expected status pending : " + response);
    }
  }

  public void complete() throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      complete(connection);
    }
  }

  public void complete(AtSecondaryConnection connection) throws Exception {
    AtKeys keys = KeysUtil.loadKeys(keysFile);
    authenticate(connection);
    keys.setSelfEncryptKey(keysGetDecrypted(connection, atSign, keys, "default_self_enc_key"));
    keys.setEncryptPrivateKey(keysGetDecrypted(connection, atSign, keys, "default_enc_private_key"));
    KeysUtil.saveKeys(keys, keysFile);
  }

  public void complete(int retries, long sleepDuration, TimeUnit sleepUnit) throws Exception {
    try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, connectionRetries)) {
      complete(connection, retries, sleepDuration, sleepUnit);
    }
  }

  public void complete(AtSecondaryConnection connection, int retries, long sleepDuration, TimeUnit sleepUnit)
      throws Exception {
    Exception exception;
    int remainingRetries = retries;
    do {
      Thread.sleep(sleepUnit.toMillis(sleepDuration));
      try {
        complete(connection);
        return;
      } catch (AtUnauthenticatedException e) {
        exception = e.getMessage().contains("is pending") ? null : e;
      } catch (Exception e) {
        exception = e;
      }
    } while (exception == null && remainingRetries-- > 0);

    throw exception != null ? exception : new IllegalArgumentException();
  }

  private static String keysGetDecrypted(AtSecondaryConnection connection,
                                         AtSign atSign,
                                         AtKeys keys,
                                         String keyConstant)
      throws Exception {
    EnrollmentId enrollmentId = keys.getEnrollmentId();
    String command = "keys:get:keyName:" + enrollmentId + "." + keyConstant + ".__manage" + atSign;
    return decryptEncryptedKey(connection.executeCommand(command), keys.getApkamSymmetricKey());
  }

  protected static String decryptEncryptedKey(String json, String keyBase64) throws Exception {
    Map<String, String> map = matchDataJsonMapOfStrings(json);
    String encryptedKey = map.get("value");
    String iv = map.get("iv");
    return aesDecryptFromBase64(encryptedKey, keyBase64, iv);
  }
}
