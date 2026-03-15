package org.atsign.client.impl.cli.register;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.cli.Activate;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command line interface to claim a free atsign. Requires one-time-password
 * received on the provided email to validate.
 * Registers the free atsign to provided email
 */
@Command(name = "register", description = "Get an atsign and register")
public class Register implements Callable<String> {
  @Option(names = {"-e", "--email"}, description = "email to register a free atsign using otp-auth")
  static String email = "";

  @Option(names = {"-k", "--api-key"}, description = "register an atsign using super-API key")
  static String apiKey = "";

  Map<String, String> params = new HashMap<>();
  boolean superApiKeyMode = false;

  public static void main(String[] args) throws AtException {
    int status = new CommandLine(new Register()).execute(args);
    System.exit(status);
  }

  /**
   * contains actual register logic.
   * main() calls this method with args passed through CLI
   */
  @Override
  public String call() throws Exception {

    readParameters();
    if (superApiKeyMode) {
      new RegistrationFlow(params).add(new GetFreeAtsignWithSuperApiKey()).add(new ActivateAtsignWithSuperApiKey())
          .start();

    } else {
      // parameter confirmation needs to be manually inserted into the params map
      params.put("confirmation", "false");
      new RegistrationFlow(params).add(new GetFreeAtsign()).add(new RegisterAtsign()).add(new ValidateOtp()).start();
    }

    String[] onboardArgs = new String[] {
        "onboard",
        "-r", params.get("rootDomain") + ":" + params.get("rootPort"),
        "-a", params.get("atSign"),
        "-c", params.get("cram")};
    Activate.main(onboardArgs);

    return "Done.";
  }

  void readParameters() throws IOException {

    // checks to ensure only either of email or super-API key are provided as args.
    // if super-API key is provided sets superApiKeyMode to true
    if ("".equals(email) && !"".equals(apiKey)) {
      superApiKeyMode = true;
    } else if ("".equals(apiKey) && !"".equals(email)) {
      superApiKeyMode = false;
    } else {
      System.err.println(
                         "Usage: Register -e <email@email.com> (or)\nRegister -k <Your API Key>"
                             + "\nNOTE: Use email if you prefer activating using verification code."
                             + " Use API key option if you have a SuperAPI key. You can NOT use both.");
      System.exit(1);
    }

    params.put("rootDomain", ConfigReader.getProperty("rootServer", "domain"));
    if (params.get("rootDomain") == null) {
      // reading config from older configuration syntax for backwards compatibility
      params.put("rootDomain", ConfigReader.getProperty("ROOT_DOMAIN"));
    }

    params.put("rootPort", ConfigReader.getProperty("rootServer", "port"));
    if (params.get("rootPort") == null) {
      // reading config from older configuration syntax for backwards compatibility
      params.put("rootPort", ConfigReader.getProperty("ROOT_PORT"));
    }
    System.out.println("RootServer is " + params.get("rootDomain") + ":" + params.get("rootPort"));

    params.put("registrarUrl", ConfigReader.getProperty("registrarV3", "url"));
    if (params.get("registrarUrl") == null) {
      // reading config from older configuration syntax for backwards compatibility
      params.put("registrarUrl", ConfigReader.getProperty("REGISTRAR_URL"));
    }

    if (!superApiKeyMode && "".equals(apiKey)) {
      params.put("apiKey", ConfigReader.getProperty("registrar", "apiKey"));
      if (params.get("apiKey") == null) {
        // reading config from older configuration syntax for backwards compatibility
        params.put("apiKey", ConfigReader.getProperty("API_KEY"));
      }
    }

    // adding email/apiKey to params whichever is passed through command line args
    if (!superApiKeyMode) {
      params.put("email", email);
    } else {
      params.put("apiKey", apiKey);
    }

    // ensure all required params have been set
    if (!params.containsKey("rootDomain") || !params.containsKey("rootPort") || !params.containsKey("registrarUrl")
        || !params.containsKey("apiKey")) {
      System.err.println(
                         "Please make sure to set all relevant configuration in src/main/resources/config.yaml");
      System.exit(1);
    }
  }
}
