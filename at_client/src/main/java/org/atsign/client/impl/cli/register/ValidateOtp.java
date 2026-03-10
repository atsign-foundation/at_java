package org.atsign.client.impl.cli.register;

import org.atsign.client.api.AtSign;
import org.atsign.client.impl.exceptions.AtRegistrarException;

import java.util.Map;
import java.util.Scanner;

class ValidateOtp extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {
  Scanner scanner = new Scanner(System.in);

  @Override
  public RegisterApiResult<Map<String, String>> run() {
    try {
      // only ask for user input the first time. use the otp entry in params map in
      // subsequent api requests
      if (!params.containsKey("otp")) {
        System.out.println("Enter verification code received on " + params.get("email")
            + " [verification code is case sensitive]");
        params.put("otp", scanner.nextLine());
        System.out.println("Validating verification code ...");
      }
      String apiResponse = registerUtil.validateOtp(params.get("email"), new AtSign(params.get("atSign")),
                                                    params.get("otp"), params.get("registrarUrl"), params.get("apiKey"),
                                                    Boolean.parseBoolean(params.get("confirmation")));
      if ("retry".equals(apiResponse)) {
        System.err.println("Incorrect OTP!!! Please re-enter your OTP");
        params.put("otp", scanner.nextLine());
        result.apiCallStatus = ApiCallStatus.retry;
        result.atException = new AtRegistrarException("Only 3 retries allowed to re-enter OTP - Incorrect OTP entered");
      } else if ("follow-up".equals(apiResponse)) {
        params.put("confirmation", "true");
        result.apiCallStatus = ApiCallStatus.retry;
      } else if (apiResponse.startsWith("@")) {
        result.data.put("cram", apiResponse.split(":")[1]);
        System.out.println("\tRCVD cram: " + result.data.get("cram"));
        System.out.println("Done.");
        result.apiCallStatus = ApiCallStatus.success;
        scanner.close();
      }
    } catch (AtRegistrarException e) {
      result.atException = e;
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    } catch (Exception e) {
      result.atException = new AtRegistrarException("Failed while validating OTP", e);
      result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
    }
    return result;
  }
}
