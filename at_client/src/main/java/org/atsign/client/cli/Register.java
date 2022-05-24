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
        String rootPort = configReader.getProperty("rootServer", "port");
        String registrarUrl = configReader.getProperty("registrar", "url");
        String apiKey = configReader.getProperty("registrar", "apiKey");

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

            if ("retry".equals(validationResponse)) {
                while ("retry".equals(validationResponse)) {
                    System.out.println("Incorrect OTP entered. Re-enter the OTP: ");
                    otp = scanner.nextLine();
                    validationResponse = registerUtil.validateOtp(email, atsign, otp, registrarUrl, apiKey, false);
                }
                scanner.close();
            }

            if (validationResponse.startsWith("@")) {
                System.out.println("One-time-password verified. OK");
                cramSecret = validationResponse.split(":")[1];
                System.out.println("Got cram secret for " + atsign + " :" + cramSecret);

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
