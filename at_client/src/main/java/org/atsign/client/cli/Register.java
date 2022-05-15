package org.atsign.client.cli;

import java.util.Iterator;
import java.util.Scanner;

import org.atsign.client.util.Constants;
import org.atsign.client.util.RegisterUtil;
import org.atsign.common.AtSign;

/**
 * Command line interface to claim a free atsign. Requires one-time-password
 * received on the provided email to validate.
 * Registers the free atsign to provided email
 */
public class Register {
    public static void main(String[] args) throws Exception {
        String email = args[0];
        String otp;
        String validationResponse;
        String cramSecret;
        String rootDomain = Constants.ROOT_DOMAIN_PROD;
        String rootPort = Constants.ROOT_PORT;
        String resgistrarDomain = Constants.REGISTRAR_DOMAIN_DEV;

        if (!args[0].equals("-e")) {
            System.err.println(
                    "Usage: Register -e <email@email.com> -r [optional] <Root Server Domain> -p [optional] <Root Server Port> -a [optional] <Registrar URL>");
            System.exit(1);
        }

        for (int arg = 0; arg < args.length; arg += 2) {
            switch (args[arg]) {
                case "-e":
                    email = args[arg + 1];
                    break;
                case "-r":
                    rootDomain = args[arg + 1];
                    break;
                case "-p":
                    rootPort = args[arg + 1];
                    break;
                case "-a":
                    resgistrarDomain = args[arg + 1];
                    break;
            }

        }

        Scanner scanner = new Scanner(System.in);
        RegisterUtil registerUtil = new RegisterUtil();

        System.out.println("Getting free atsign");
        AtSign atsign = new AtSign(registerUtil.getFreeAtsign(resgistrarDomain));
        System.out.println("Got atsign: " + atsign);

        System.out.println("Sending one-time-password to :" + email);
        if (registerUtil.registerAtsign(email, atsign, resgistrarDomain)) {
            System.out.println("Enter OTP received on: " + email);

            otp = scanner.nextLine();
            System.out.println("Validating one-time-password");
            validationResponse = registerUtil.validateOtp(email, atsign, otp, resgistrarDomain);

            if (validationResponse == "retry") {
                while (validationResponse == "retry") {
                    System.out.println("Incorrect OTP entered. Re-enter the OTP: ");
                    otp = scanner.nextLine();
                    validationResponse = registerUtil.validateOtp(email, atsign, otp, resgistrarDomain);
                }
                scanner.close();
            } else if (validationResponse.startsWith("@")) {
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
