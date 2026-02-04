package org.atsign.cucumber.steps;

import static org.atsign.cucumber.helpers.Helpers.getFirstValue;
import static org.atsign.cucumber.helpers.Helpers.toCanonicalMaps;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.client.cli.Activate;
import org.atsign.client.util.EnrollmentId;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtClientConfigException;
import org.atsign.cucumber.helpers.AtDemoData;
import org.opentest4j.TestAbortedException;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivateSteps {

  private final AtClientContext context;

  private final List<Runnable> teardownCommands = new ArrayList<>();

  private final Stack<String> otps = new Stack<>();

  private final Stack<EnrollmentId> enrollmentIds = new Stack<>();

  @After
  public void teardown() {
    teardownCommands.forEach(Runnable::run);
  }

  public ActivateSteps(AtClientContext context) {
    this.context = context;
  }

  @Given("atsign keys for {atsign} are missing")
  public void assertMissingKeys(AtSign atsign) throws Exception {
    try {
      KeysUtil.loadKeys(atsign);
    } catch (AtClientConfigException e) {
      if (e.getMessage().contains("loadKeys: No file")) {
        return;
      }
    }
    throw new TestAbortedException("skipping remainder of the scenario (virtual env needs to be reset)");
  }

  @When("{atsign} Activate.onboard with {word}.{word} from {path} in at_demo_data package")
  public void onboard(AtSign atSign, String className, String constName, File fileContainingCramSecret)
      throws Exception {
    onboard(atSign, AtDemoData.getClassConst(fileContainingCramSecret.getName(), className, constName));
  }

  @When("{atsign} Activate.onboard with CRAM secret {string}")
  public void onboard(AtSign atSign, String cramSecret) throws Exception {
    File keysFile = createAtKeysFile(atSign);
    EnrollmentId onboardEnrollmentId = createActivateUtil(atSign)
        .setCramSecret(cramSecret)
        .setKeysFile(keysFile.getAbsolutePath())
        .setNoDeleteCramKey()
        .allowOverwriteKeysFile()
        .onboard();
    assertThat(keysFile.exists(), is(true));
    teardownCommands.add(0, new ActivateTeardown(atSign, keysFile, cramSecret, onboardEnrollmentId));
  }

  @Then("{atsign} Activate.onboard fails with CRAM secret {string}")
  public void onboardExpectFail(AtSign atSign, String secret) throws Exception {
    context.assertException(() -> onboard(atSign, secret));
  }

  @Given("{atsign} Activate.otp generates an OTP")
  public void createOtp(AtSign atSign) throws Exception {
    otps.push(createActivateUtil(atSign).otp());
  }

  @Given("{atsign} Activate.otp generates {int} OTPs")
  public void createOtp(AtSign atSign, int num) throws Exception {
    for (int i = 0; i < num; i++) {
      createOtp(atSign);
    }
  }

  @Given("{atsign} Activate.enroll for app {word} and device {word} with last OTP and following namespaces")
  public void enrollWithLastOtp(AtSign atSign, String app, String device, DataTable namespaces) throws Exception {
    enroll(atSign, app, device, otps.pop(), namespaces);
  }

  @Given("{atsign} Activate.enroll for app {word} and device {word} with OTP {word} and following namespaces")
  public void enroll(AtSign atSign, String app, String device, String otp, DataTable namespaces) throws Exception {
    Activate util = new Activate()
        .setVerbose(context.isVerbose())
        .setAtSign(atSign)
        .setRootUrl(context.getRootHostAndPort())
        .setAppName(app)
        .setDeviceName(device)
        .setKeysFile(createAtKeysFile(atSign, app, device).getAbsolutePath())
        .allowOverwriteKeysFile()
        .setOtp(otp);
    for (Map<String, String> map : toCanonicalMaps(namespaces.asMaps())) {
      String ns = getFirstValue(map, "ns", "namespace");
      String ac = getFirstValue(map, "ac", "accesscontrol");
      if (ns == null || ac == null) {
        throw new IllegalArgumentException("expected table with ns (namespace) and ac (access control)");
      }
      util.addNamespace(ns, ac);
    }
    enrollmentIds.push(util.enroll());
  }

  @Given("AtClient with keys {path} for {atsign} completes enrollment")
  public void completeEnrollmentAndCreateAtClient(File keysFile, AtSign atSign) throws Exception {
    Activate util = new Activate()
        .setVerbose(context.isVerbose())
        .setAtSign(atSign)
        .setRootUrl(context.getRootHostAndPort())
        .setKeysFile(context.resolveKeysFile(keysFile).getAbsolutePath())
        .allowOverwriteKeysFile();
    util.complete();
    context.createCurrentAtClient(keysFile, atSign);
  }

  @When("{atsign} Activate.approve for enrollmentId {string}")
  public void approve(AtSign atSign, String enrollmentId) throws Exception {
    Activate util = createActivateUtil(atSign);
    util.approve(EnrollmentId.createEnrollmentId(enrollmentId));
  }

  @When("{atsign} Activate.approve for last enrollment")
  public void approve(AtSign atSign) throws Exception {
    approve(atSign, enrollmentIds.peek().toString());
  }

  @When("{atsign} Activate.approve for all enrollments")
  public void approveEnrollAll(AtSign atSign) throws Exception {
    enrollmentIds.forEach(id -> {
      try {
        approve(atSign, id.toString());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @When("{atsign} Activate.deny for enrollmentId {string}")
  public void deny(AtSign atSign, String enrollmentId) throws Exception {
    Activate util = createActivateUtil(atSign);
    util.deny(EnrollmentId.createEnrollmentId(enrollmentId));
  }

  @When("{atsign} Activate.deny for last enrollment")
  public void deny(AtSign atSign) throws Exception {
    deny(atSign, enrollmentIds.pop().toString());
  }

  @When("{atsign} Activate.revoke for enrollmentId {string}")
  public void revoke(AtSign atSign, String enrollmentId) throws Exception {
    Activate util = createActivateUtil(atSign);
    util.revoke(EnrollmentId.createEnrollmentId(enrollmentId));
  }

  @When("{atsign} Activate.revoke for last enrollment")
  public void revoke(AtSign atSign) throws Exception {
    revoke(atSign, enrollmentIds.peek().toString());
  }

  @When("{atsign} Activate.unrevoke for enrollmentId {string}")
  public void unrevoke(AtSign atSign, String enrollmentId) throws Exception {
    Activate util = createActivateUtil(atSign);
    util.unrevoke(EnrollmentId.createEnrollmentId(enrollmentId));
  }

  private Activate createActivateUtil(AtSign atSign) {
    return new Activate()
        .setVerbose(context.isVerbose())
        .setAtSign(atSign)
        .setRootUrl(context.getRootHostAndPort());
  }

  @When("{atsign} Activate.unrevoke for last enrollment")
  public void unrevoke(AtSign atSign) throws Exception {
    unrevoke(atSign, enrollmentIds.pop().toString());
  }

  private class FileDelete implements Runnable {
    private File f;

    public FileDelete(File f) {
      this.f = f;
    }

    @Override
    public void run() {
      if (!f.delete()) {
        log.warn("failed to delete {}", f);
      } else {
        log.debug("deleted {}", f);
      }
    }
  }

  private class ActivateTeardown extends Activate implements Runnable {

    private final EnrollmentId onboardEnrollmentId;

    public ActivateTeardown(AtSign atSign, File keysFile, String cramSecret, EnrollmentId onboardEnrollmentId) {
      this.onboardEnrollmentId = onboardEnrollmentId;
      setRootUrl(context.getRootHostAndPort());
      setAtSign(atSign);
      setKeysFile(keysFile.getAbsolutePath());
      setCramSecret(cramSecret);
    }

    public void run() {

      try (AtSecondaryConnection connection = createAtSecondaryConnection(atSign, rootUrl, 0)) {

        authenticateWithApkam(connection, atSign, KeysUtil.loadKeys(keysFile));

        // delete keys that have been created
        matchDataJsonListOfStrings(connection.executeCommand("scan")).stream()
            .filter(k -> !isProtectedKey(atSign, k))
            .forEach(k -> deleteKeyNoThrow(connection, k));

        // remove enrollments
        list(connection, "pending").forEach(id -> denyDeleteNoThrow(connection, id));
        list(connection, "denied").forEach(id -> deleteNoThrow(connection, id));
        list(connection, "approved").stream()
            .filter(id -> !id.equals(onboardEnrollmentId))
            .forEach(id -> revokeDeleteNoThrow(connection, id));
        revokeDeleteNoThrow(connection, onboardEnrollmentId);
      } catch (Exception e) {
        log.error("teardown for {} failed : {}", atSign, e.getMessage());
      }
    }

    private boolean isProtectedKey(AtSign atSign, String key) {
      return key.equals(atSign + ":signing_privatekey" + atSign)
          || key.equals("public:signing_publickey" + atSign)
          || key.equals("public:publickey" + atSign)
          || key.contains(("__manage@"));
    }

    protected void deleteKeyNoThrow(AtSecondaryConnection connection, String key) {
      try {
        log.debug("teardown for {} deleting key {}", connection.getAtSign(), key);
        deleteKey(connection, key);
      } catch (Exception e) {
        log.error("teardown for {} failed to delete key {} in onboarded server : {}",
                  connection.getAtSign(), key, e.getMessage());
      }
    }

    private void deleteNoThrow(AtSecondaryConnection connection, EnrollmentId id) {
      try {
        log.debug("teardown for {} deleting enroll request {}", id);
        delete(connection, id);
      } catch (Exception e) {
        log.error("teardown for {} failed to enroll delete {} in onboarded server : {}",
                  connection.getAtSign(), id, e.getMessage());
      }
    }

    private void denyDeleteNoThrow(AtSecondaryConnection connection, EnrollmentId id) {
      try {
        log.debug("teardown for {} denying enroll request {}", connection.getAtSign(), id);
        deny(connection, id);
        log.debug("teardown for {} deleting enroll request {}", connection.getAtSign(), id);
        delete(connection, id);
      } catch (Exception e) {
        log.error("teardown for {} failed to enroll deny and delete {} in onboarded server : {}",
                  connection.getAtSign(), id, e.getMessage());
      }
    }

    private void revokeDeleteNoThrow(AtSecondaryConnection connection, EnrollmentId id) {
      try {
        log.debug("teardown for {} revoking enroll request {}", connection.getAtSign(), id);
        revoke(connection, id);
        log.debug("teardown for {} deleting enroll request {}", connection.getAtSign(), id);
        delete(connection, id);
      } catch (Exception e) {
        log.error("teardown for {} failed to enroll revoke and delete {} in onboarded server : {}",
                  connection.getAtSign(), id, e.getMessage());
      }
    }
  }

  private File createAtKeysFile(AtSign atSign) {
    String filename = atSign.toString() + KeysUtil.keysFileSuffix;
    File file = new File(new File(KeysUtil.expectedKeysFilesLocation), filename);
    teardownCommands.add(new FileDelete(file));
    return file;
  }

  private File createAtKeysFile(AtSign atSign, String app, String device) {
    String filename = atSign + "-" + app + "-" + device + KeysUtil.keysFileSuffix;
    File file = new File(new File(KeysUtil.expectedKeysFilesLocation), filename);
    teardownCommands.add(new FileDelete(file));
    return file;
  }
}
