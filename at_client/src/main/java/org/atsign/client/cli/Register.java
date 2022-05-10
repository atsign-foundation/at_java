package org.atsign.client.cli;

import java.util.Scanner;

import org.atsign.client.util.Constants;
import org.atsign.client.util.RegisterUtil;
import org.atsign.common.AtSign;

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
        AtSign atsign = new AtSign(registerUtil.getFreeAtsign());
        System.out.println("Your atsign is: " + atsign);

        if (registerUtil.registerAtsign(email, atsign)) {
            System.out.println("Enter OTP received on: " + email);
            otp = scanner.nextLine();
            validationResponse = registerUtil.validateOtp(email, atsign, otp);
            if (validationResponse == "retry") {
                while (validationResponse == "retry") {
                    System.out.println("Incorrect OTP entered. Re-enter the OTP");
                    otp = scanner.nextLine();
                    validationResponse = registerUtil.validateOtp(email, atsign, otp);
                }
                scanner.close();
            } else if (validationResponse.startsWith("@")) {
                cramSecret = validationResponse.split(":")[1];
                System.out.println(cramSecret);
                String[] onboardArgs = { Constants.DEV_DOMAIN, atsign.toString(), cramSecret };
                Onboard.main(onboardArgs);
            } else {
                System.out.println(validationResponse);
            }
        }

    }

}
