package org.atsign.client.impl.cli;

import static org.atsign.client.impl.util.EncryptionUtils.generateAESKeyBase64;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.atsign.client.impl.util.KeysUtils.saveKeys;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.impl.commands.EnrollCommands;
import org.atsign.client.impl.commands.ScanCommands;
import org.atsign.client.impl.common.EnrollmentId;
import org.atsign.client.impl.util.KeysUtils;
import org.atsign.client.impl.exceptions.AtEncryptionException;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;

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
    AtCommandExecutorContext context = newConnectionContext();
    try (AtCommandExecutor executor = createConnection(context, connectionRetries)) {
      return onboard(executor, context);
    }
  }

  public EnrollmentId onboard(AtCommandExecutor executor, AtCommandExecutorContext context) throws Exception {
    File file = getAtKeysFile(keysFile, atSign);
    if (!overwriteKeysFile) {
      checkNotExists(file);
    }
    AtKeys keys = generateAtKeys(true);
    keys = EnrollCommands.onboard(executor,
                                  context,
                                  keys,
                                  cramSecret,
                                  ensureNotNull(appName, DEFAULT_FIRST_APP),
                                  ensureNotNull(deviceName, DEFAULT_FIRST_DEVICE),
                                  deleteCramKey);
    saveKeys(keys, file);
    return enrollmentId;
  }

  public List<EnrollmentId> list() throws Exception {
    return list(requestStatus);
  }

  public List<EnrollmentId> list(String status) throws Exception {
    try (AtCommandExecutor executor = createAuthenticatedConnection(rootUrl, atSign, connectionRetries)) {
      return EnrollCommands.list(executor, status);
    }
  }

  public void approve() throws Exception {
    approve(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void approve(EnrollmentId enrollmentId) throws Exception {
    try (AtCommandExecutor executor = createAuthenticatedConnection(rootUrl, atSign, connectionRetries)) {
      EnrollCommands.approve(executor, getKeys(), enrollmentId);
    }
  }

  public void deny() throws Exception {
    deny(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void deny(EnrollmentId enrollmentId) throws Exception {
    try (AtCommandExecutor executor = createAuthenticatedConnection(rootUrl, atSign, connectionRetries)) {
      EnrollCommands.deny(executor, enrollmentId);
    }
  }

  public void revoke() throws Exception {
    revoke(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void revoke(EnrollmentId enrollmentId) throws Exception {
    try (AtCommandExecutor executor = createAuthenticatedConnection(rootUrl, atSign, connectionRetries)) {
      EnrollCommands.revoke(executor, enrollmentId);
    }
  }

  public void unrevoke() throws Exception {
    unrevoke(checkNotNull(enrollmentId, "enrollment id not set"));
  }

  public void unrevoke(EnrollmentId enrollmentId) throws Exception {
    try (AtCommandExecutor executor = createAuthenticatedConnection(rootUrl, atSign, connectionRetries)) {
      EnrollCommands.unrevoke(executor, enrollmentId);
    }
  }

  public void delete(EnrollmentId enrollmentId) throws Exception {
    try (AtCommandExecutor executor = createAuthenticatedConnection(rootUrl, atSign, connectionRetries)) {
      delete(executor, enrollmentId);
    }
  }

  public static void delete(AtCommandExecutor executor, EnrollmentId enrollmentId) throws Exception {
    EnrollCommands.delete(executor, enrollmentId);
  }

  public String otp() throws Exception {
    try (AtCommandExecutor executor = createAuthenticatedConnection(rootUrl, atSign, connectionRetries)) {
      return EnrollCommands.otp(executor);
    }
  }

  public List<String> scan() throws Exception {
    try (AtCommandExecutor executor = createConnection(rootUrl, atSign, connectionRetries)) {
      return ScanCommands.scan(executor, true, ".*");
    }
  }

  public EnrollmentId enroll() throws Exception {
    try (AtCommandExecutor executor = createConnection(rootUrl, atSign, connectionRetries)) {
      return enroll(executor);
    }
  }

  public EnrollmentId enroll(AtCommandExecutor executor) throws Exception {
    File file = keysFile;
    if (!overwriteKeysFile) {
      checkNotExists(file);
    }
    AtKeys keys = generateAtKeys(false);
    keys = EnrollCommands.enroll(executor, atSign, keys, otp, appName, deviceName, namespaces);
    KeysUtils.saveKeys(keys, keysFile);
    return keys.getEnrollmentId();
  }

  public void complete() throws Exception {
    // complete authenticates imperatively (and retries on "pending"), so it issues its own from:
    // rather than reusing a connect-time challenge that could go stale before the retried PKAM
    try (AtCommandExecutor executor = createConnectionForImperativeAuth(rootUrl, atSign, connectionRetries)) {
      complete(executor);
    }
  }

  public void complete(AtCommandExecutor executor) throws Exception {
    AtKeys keys = KeysUtils.loadKeys(keysFile);
    keys = EnrollCommands.complete(executor, atSign, keys);
    KeysUtils.saveKeys(keys, keysFile);
  }

  public void complete(int retries, long sleepDuration, TimeUnit sleepUnit) throws Exception {
    // see complete(): imperative, retried PKAM issues its own from:
    try (AtCommandExecutor executor = createConnectionForImperativeAuth(rootUrl, atSign, connectionRetries)) {
      complete(executor, retries, sleepDuration, sleepUnit);
    }
  }

  public void complete(AtCommandExecutor executor, int retries, long sleepDuration, TimeUnit sleepUnit)
      throws Exception {
    Exception exception;
    int remainingRetries = retries;
    do {
      Thread.sleep(sleepUnit.toMillis(sleepDuration));
      try {
        complete(executor);
        return;
      } catch (AtUnauthenticatedException e) {
        exception = e.getMessage().contains("is pending") ? null : e;
      } catch (Exception e) {
        exception = e;
      }
    } while (exception == null && remainingRetries-- > 0);

    throw exception != null ? exception : new IllegalArgumentException();
  }

  protected static AtKeys generateAtKeys(boolean generateEncryptionKeyPair) throws AtEncryptionException {
    AtKeys.AtKeysBuilder builder = AtKeys.builder()
        .selfEncryptKey(generateAESKeyBase64())
        .apkamKeyPair(generateRSAKeyPair())
        .apkamSymmetricKey(generateAESKeyBase64());
    if (generateEncryptionKeyPair) {
      builder.encryptKeyPair(generateRSAKeyPair());
    }
    return builder.build();
  }

}
