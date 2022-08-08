package org.atsign.client.cli;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.atsign.client.util.RegisterUtil;
import org.atsign.common.AtSign;
import org.atsign.config.ConfigReader;

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
    @Option(names = { "-e", "--email" }, description = "email to register a free atsign using otp-auth")
    static String email = "";

    @Option(names = { "-k", "--api-key" }, description = "register an atsign using super-API key")
    static String apiKey = "";

    @Override
    public String call() throws Exception {
        boolean isRegistrarV3 = false;
        // checks to ensure only either of email or super-API key are provided as args.
        // if super-API key is provided uses registrar v3, otherwise uses registrar v2.
        if (email.equals("") && !apiKey.equals("")) {
            isRegistrarV3 = true;
        } else if (apiKey.equals("") && !email.equals("")) {
        } else {
            System.err.println(
                    "Usage: Register -e <email@email.com> (or)\nRegister -k <Your API Key>\nNOTE: Use email if you prefer activating using OTP. Go for API key option if you have your own SuperAPI key. You cannot use both.");
            System.exit(1);
        }

        ConfigReader configReader = new ConfigReader();
        RegisterUtil registerUtil = new RegisterUtil();
        String cramSecret;
        String rootDomain;
        String rootPort;
        String registrarUrl;
        String otp;
        String validationResponse;
        AtSign atsign;

        rootDomain = configReader.getProperty("rootServer", "domain");
        if (rootDomain == null) {
            // reading config from older configuration syntax for backwards compatability
            rootDomain = configReader.getProperty("ROOT_DOMAIN");
        }

        rootPort = configReader.getProperty("rootServer", "port");
        if (rootPort == null) {
            // reading config from older configuration syntax for backwards compatability
            rootPort = configReader.getProperty("ROOT_PORT");
        }

        registrarUrl = isRegistrarV3 ? configReader.getProperty("registrarV3", "url")
                : configReader.getProperty("registrar", "url");
        if (registrarUrl == null) {
            // reading config from older configuration syntax for backwards compatability
            registrarUrl = configReader.getProperty("REGISTRAR_URL");
        }

        if (!isRegistrarV3 && apiKey.equals("")) {
            try {
                apiKey = configReader.getProperty("registrar", "apiKey");
            } catch (Exception e) {
                // reading config from older configuration syntax for backwards compatability
                apiKey = configReader.getProperty("API_KEY");
            }
        }

        if (rootDomain == null || rootPort == null || registrarUrl == null || apiKey == null) {
            System.err.println(
                    "Please make sure to set all relevant configuration in src/main/resources/config.yaml");
            System.exit(1);
        }

        if (!isRegistrarV3) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Getting free atsign");
            atsign = new AtSign(registerUtil.getFreeAtsign(registrarUrl, apiKey));
            System.out.println("Got atsign: " + atsign);

            System.out.println("Sending one-time-password to :" + email);
            if (registerUtil.registerAtsign(email, atsign, registrarUrl, apiKey)) {
                System.out.println("Enter OTP received on: " + email);

                otp = scanner.nextLine();
                System.out.println("Validating one-time-password");
                validationResponse = registerUtil.validateOtp(email, atsign, otp, registrarUrl, apiKey, false);
                // if validationResponse is retry, the OTP entered is incorrect. Ask user to
                // re-enter correct OTP
                if ("retry".equals(validationResponse)) {
                    while ("retry".equals(validationResponse)) {
                        System.out.println("Incorrect OTP entered. Re-enter the OTP: ");
                        otp = scanner.nextLine();
                        validationResponse = registerUtil.validateOtp(email, atsign, otp, registrarUrl, apiKey, false);
                    }
                    scanner.close();
                }
                // if validationResponse is follow-up, the atsign has been regstered to email.
                // Again call the API with "confirmation"=true to get the cram key
                if ("follow-up".equals(validationResponse)) {
                    validationResponse = registerUtil.validateOtp(email, atsign, otp, registrarUrl, apiKey, true);
                }
                // if validation response starts with @, that represents that validationResponse
                // contains cram
                if (validationResponse.startsWith("@")) {
                    System.out.println("One-time-password verified. OK");
                    // extract cram from response
                    cramSecret = validationResponse.split(":")[1];
                    System.out.println("Got cram secret for " + atsign + ": " + cramSecret);

                    String[] onboardArgs = new String[] { rootDomain + ":" + rootPort,
                            atsign.toString(), cramSecret };
                    Onboard.main(onboardArgs);
                } else {
                    System.err.println(validationResponse);
                    return "success";
                }
            } else {

                System.err.println("Error while sending OTP. Please retry the process");
                scanner.close();
                return "unsuccessful";
            }
        } else {
            String activationKey;
            Map<String, String> responseMap;
            Map.Entry<String, String> mapEntry;
            System.out.println("Getting AtSign...");
            responseMap = registerUtil.getAtsignV3(registrarUrl, apiKey);
            mapEntry = responseMap.entrySet().iterator().next();
            atsign = new AtSign(mapEntry.getKey());
            System.out.println("Got AtSign: " + atsign);
            activationKey = mapEntry.getValue();
            System.out.println("Activating atsign using activationKey...");
            cramSecret = registerUtil.activateAtsign(registrarUrl, apiKey, atsign, activationKey);
            cramSecret = cramSecret.split(":")[1];
            System.out.println("Your cramSecret is: " + cramSecret);
            System.out.println("Do you want to activate the atsign? [y/n] " + atsign);
            Scanner scanner = new Scanner(System.in);
            if (scanner.next() == "y") {
                String[] onboardArgs = new String[] { rootDomain + ":" + rootPort,
                        atsign.toString(), cramSecret };
                Onboard.main(onboardArgs);
            }
            scanner.close();
            System.out.println("Done.");
            return "success";
        }
        return "";
    }

    public static void main(String[] args) {
        int status = new CommandLine(new Register()).execute(args);
        System.exit(status);
    }
}
