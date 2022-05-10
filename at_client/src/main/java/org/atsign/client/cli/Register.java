package org.atsign.client.cli;

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
        if (args.length == 0) {
            System.err.println("Usage: Register <email@email.com>");
            System.exit(1);
        }

        String email = args[0];
        String otp;
        String validationResponse;
        String cramSecret;

        Scanner scanner = new Scanner(System.in);
        RegisterUtil registerUtil = new RegisterUtil();
        System.out.println("Getting free atsign");
        AtSign atsign = new AtSign(registerUtil.getFreeAtsign());
        System.out.println("Got atsign: " + atsign);
        System.out.println("Sending one-time-password to :" + email);
        if (registerUtil.registerAtsign(email, atsign)) {
            System.out.println("Enter OTP received on: " + email);
            otp = scanner.nextLine();
            System.out.println("Validating one-time-password");
            validationResponse = registerUtil.validateOtp(email, atsign, otp);
            if (validationResponse == "retry") {
                while (validationResponse == "retry") {
                    System.out.println("Incorrect OTP entered. Re-enter the OTP: ");
                    otp = scanner.nextLine();
                    validationResponse = registerUtil.validateOtp(email, atsign, otp);
                }
                scanner.close();
            } else if (validationResponse.startsWith("@")) {
                System.out.println("one-time-password verified. OK");
                cramSecret = validationResponse.split(":")[1];
                System.out.println("Got cram secret for " + atsign + " :" + cramSecret);
                String[] onboardArgs = new String[] { Constants.DEV_DOMAIN + ":" + Constants.ROOT_PORT,
                        atsign.toString(), cramSecret };
                Onboard.main(onboardArgs);
            } else {
                System.err.println(validationResponse);
            }
        } else {
            System.err.println("Error while sending OTP. Please retry");
            ;
        }

    }

}
