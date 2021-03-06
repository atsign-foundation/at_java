package org.atsign.client.cli;

import java.util.Scanner;

import org.atsign.client.util.RegisterUtil;
import org.atsign.common.AtSign;
import org.atsign.config.ConfigReader;

/**
 * Command line interface to claim a free atsign. Requires one-time-password
 * received on the provided email to validate.
 * Registers the free atsign to provided email
 */
public class Register {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Usage: Register <email@email.com>");
            System.exit(1);
        }

        ConfigReader configReader = new ConfigReader();
        String email = args[0];
        String otp;
        String validationResponse;
        String cramSecret;
        String rootDomain = configReader.getProperty("rootServer", "domain");
        if (rootDomain == null) {
            //reading config from older configuration syntax for backwards compatability
            rootDomain = configReader.getProperty("ROOT_DOMAIN");
        }
        String rootPort = configReader.getProperty("rootServer", "port");
        if (rootPort == null) {
            //reading config from older configuration syntax for backwards compatability
            rootPort = configReader.getProperty("ROOT_PORT");
        }
        String registrarUrl = configReader.getProperty("registrar", "url");
        if (registrarUrl == null) {
            //reading config from older configuration syntax for backwards compatability
            registrarUrl = configReader.getProperty("REGISTRAR_URL");
        }
        String apiKey = configReader.getProperty("registrar", "apiKey");
        if (apiKey == null) {
            //reading config from older configuration syntax for backwards compatability
            apiKey = configReader.getProperty("API_KEY");
        }

        if (rootDomain == null || rootPort == null || registrarUrl == null || apiKey == null) {
            System.err.println("Please make sure to set all relevant configuration in src/main/resources/config.yaml");
            System.exit(1);
        }

        Scanner scanner = new Scanner(System.in);
        RegisterUtil registerUtil = new RegisterUtil();

        System.out.println("Getting free atsign");
        AtSign atsign = new AtSign(registerUtil.getFreeAtsign(registrarUrl, apiKey));
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
            }
        } else {
            System.err.println("Error while sending OTP. Please retry the process");
        }

    }

}
