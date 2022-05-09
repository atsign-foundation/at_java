package org.atsign.client.cli;

import java.util.Scanner;
import org.atsign.client.util.RegisterUtil;

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
        Onboard onboardCli = new Onboard();
        RegisterUtil registerUtil = new RegisterUtil();
        String atsign = registerUtil.getFreeAtsign();
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
            } else if (validationResponse.startsWith("@")) {
                cramSecret = validationResponse.split(":")[1];
                System.out.println(cramSecret);
                //call the onboarding CLI with the available cram secret
            }  else {
                System.out.println(validationResponse);
            }
        }

    }

}
