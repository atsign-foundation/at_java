package org.atsign.client.cli;

import org.atsign.client.util.RegisterUtil;
import org.atsign.common.ApiCallStatus;
import org.atsign.common.AtSign;
import org.atsign.common.RegisterApiResult;
import org.atsign.common.RegisterApiTask;
import org.atsign.common.AtException;
import org.atsign.common.exceptions.AtRegistrarException;
import org.atsign.config.ConfigReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

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

    Map<String, String> params = new HashMap<>();
    boolean isRegistrarV3 = false;

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
        if (isRegistrarV3) {
            new RegistrationFlow(params).add(new GetAtsignV3()).add(new ActivateAtsignV3()).start();

        } else {
            // parameter confirmation needs to be manually inserted into the params map
            params.put("confirmation", "false");
            new RegistrationFlow(params).add(new GetFreeAtsign()).add(new RegisterAtsign()).add(new ValidateOtp()).start();
        }

        String[] onboardArgs = new String[] {
                params.get("rootDomain") + ":" + params.get("rootPort"),
                params.get("atSign"), params.get("cram") };
        Onboard.main(onboardArgs);

        return "Done.";
    }

    void readParameters() throws IOException {

        // checks to ensure only either of email or super-API key are provided as args.
        // if super-API key is provided uses registrar v3, otherwise uses registrar v2.
        if ("".equals(email) && !"".equals(apiKey)) {
            isRegistrarV3 = true;
        } else if ("".equals(apiKey) && !"".equals(email)) {
            isRegistrarV3 = false;
        } else {
            System.err.println(
                    "Usage: Register -e <email@email.com> (or)\nRegister -k <Your API Key>\nNOTE: Use email if you prefer activating using OTP. Go for API key option if you have your own SuperAPI key. You cannot use both.");
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

        params.put("registrarUrl", isRegistrarV3 ? ConfigReader.getProperty("registrarV3", "url")
                : ConfigReader.getProperty("registrar", "url"));
        if (params.get("registrarUrl") == null) {
            // reading config from older configuration syntax for backwards compatibility
            params.put("registrarUrl", ConfigReader.getProperty("REGISTRAR_URL"));
        }

        if (!isRegistrarV3 && "".equals(apiKey)) {
            params.put("apiKey", ConfigReader.getProperty("registrar", "apiKey"));
            if (params.get("apiKey") == null) {
                // reading config from older configuration syntax for backwards compatibility
                params.put("apiKey", ConfigReader.getProperty("API_KEY"));
            }
        }

        // adding email/apiKey to params whichever is passed through command line args
        if (!isRegistrarV3) {
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

class RegistrationFlow {
    List<RegisterApiTask<RegisterApiResult<Map<String, String>>>> processFlow = new ArrayList<>();
    RegisterApiResult<Map<String, String>> result;
    Map<String, String> params;
    RegisterUtil registerUtil = new RegisterUtil();

    RegistrationFlow(Map<String, String> params) {
        this.params = params;
    }

    RegistrationFlow add(RegisterApiTask<RegisterApiResult<Map<String, String>>> task) {
        processFlow.add(task);
        return this;
    }

    void start() throws Exception {
        for (RegisterApiTask<RegisterApiResult<Map<String, String>>> task : processFlow) {
            // initialize each task by passing params to init()
            task.init(params, registerUtil);
            result = task.run();
            if (result.apiCallStatus.equals(ApiCallStatus.retry)) {
                while (task.shouldRetry()
                        && result.apiCallStatus.equals(ApiCallStatus.retry)) {
                    result = task.run();
                    task.retryCount++;
                }
            }
            if (result.apiCallStatus.equals(ApiCallStatus.success)) {
                params.putAll(result.data);
            } else {
                throw result.atException;
            }
        }
    }
}

class GetFreeAtsign extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {

    @Override
    public RegisterApiResult<Map<String, String>> run() {
        System.out.println("Getting free atsign ...");
        try {
            result.data.put("atSign",
                    registerUtil.getFreeAtsign(params.get("registrarUrl"), params.get("apiKey")));
            result.apiCallStatus = ApiCallStatus.success;
            System.out.println("Got atsign: " + result.data.get("atSign"));
        } catch (AtRegistrarException e) {
            result.atException = e;
        } catch (Exception e) {
            result.atException = new AtRegistrarException("error while getting free atsign", e);
        }
        result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
        return result;
    }
}

class RegisterAtsign extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {

    @Override
    public RegisterApiResult<Map<String, String>> run() {
        System.out.println("Sending one-time-password to :" + params.get("email"));
        try {
            result.data.put("otpSent",
                    registerUtil.registerAtsign(params.get("email"), new AtSign(params.get("atSign")),
                            params.get("registrarUrl"), params.get("apiKey")).toString());
            result.apiCallStatus = ApiCallStatus.success;
        } catch (Exception e) {
            result.atException = new AtRegistrarException(e.getMessage(), e.getCause());
            result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
        }
        return result;
    }
}

class ValidateOtp extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {
    Scanner scanner = new Scanner(System.in);

    @Override
    public RegisterApiResult<Map<String, String>> run() {
        System.out.println("Enter OTP received on " + params.get("email") + " [note: otp is case sensitive]");
        try {
            // only ask for user input the first time. use the otp entry in params map in
            // subsequent api requests
            if (!params.containsKey("otp")) {
                params.put("otp", scanner.nextLine());
            }
            System.out.println("Validating OTP ...");
            String apiResponse = registerUtil.validateOtp(params.get("email"), new AtSign(params.get("atSign")),
                    params.get("otp"), params.get("registrarUrl"), params.get("apiKey"),
                    Boolean.parseBoolean(params.get("confirmation")));
            if (apiResponse.equals("retry")) {
                System.out.println("Incorrect OTP!!! Please re-enter your OTP");
                params.put("otp", scanner.nextLine());
                result.apiCallStatus = ApiCallStatus.retry;
                result.atException = new AtRegistrarException("Only 3 retries allowed to re-enter OTP - Incorrect OTP entered");
            } else if (apiResponse.equals("follow-up")) {
                params.put("confirmation", "true");
                result.apiCallStatus = ApiCallStatus.retry;
            } else if (apiResponse.startsWith("@")) {
                result.data.put("cram", apiResponse.split(":")[1]);
                System.out.println("your cram secret: " + result.data.get("cram"));
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

class GetAtsignV3 extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {
    @Override
    public RegisterApiResult<Map<String, String>> run() {
        System.out.println("Getting atSign ...");
        try {
            result.data.putAll(registerUtil.getAtsignV3(params.get("registrarUrl"), params.get("apiKey")));
            System.out.println("Got atsign: " + result.data.get("atSign"));
            result.apiCallStatus = ApiCallStatus.success;
        } catch (AtRegistrarException e) {
            result.atException = e;
            result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
        } catch (Exception e) {
            result.atException = new AtRegistrarException("Failed while getting atSign", e);
            result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
        }
        return result;
    }
}

class ActivateAtsignV3 extends RegisterApiTask<RegisterApiResult<Map<String, String>>> {
    @Override
    public RegisterApiResult<Map<String, String>> run() {
        try {
            result.data.put("cram", registerUtil.activateAtsign(params.get("registrarUrl"), params.get("apiKey"),
                    new AtSign(params.get("atSign")), params.get("ActivationKey")).split(":")[1]);
            result.apiCallStatus = ApiCallStatus.success;
            System.out.println("Your cram secret: " + result.data.get("cram"));
        } catch (AtRegistrarException e) {
            result.atException = e;
            result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
        } catch (Exception e) {
            result.atException = new AtRegistrarException("Failed while activating atSign", e);
            result.apiCallStatus = retryCount < maxRetries ? ApiCallStatus.retry : ApiCallStatus.failure;
        }
        return result;
    }
}
