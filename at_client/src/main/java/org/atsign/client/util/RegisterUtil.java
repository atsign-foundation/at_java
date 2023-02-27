package org.atsign.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtRegistrarException;

import static java.util.stream.Collectors.toMap;
import static java.util.AbstractMap.*;

public class RegisterUtil {
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calls API to get atsigns which are ready to be claimed.
     * Returns a free atsign.
     * 
     * @param registrarUrl - URL of the atsign registrar API
     * @param apiKey       - API key to authenticate connection to atsign registrar
     * @return free atsign
     * @throws AtException thrown if HTTPS_REQUEST was not successful
     * @throws IOException if anything goes wrong while using the HttpsURLConnection
     */
    public String getFreeAtsign(String registrarUrl, String apiKey)
            throws AtException, IOException {
        URL urlObject = new URL(registrarUrl + Constants.GET_FREE_ATSIGN);
        HttpsURLConnection connection = (HttpsURLConnection) urlObject.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", apiKey);
        if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String response = bufferedReader.readLine();
            @SuppressWarnings("unchecked") Map<String, Map<String, String>> responseData = objectMapper.readValue(response, Map.class);
            Map<String, String> data = responseData.get("data");
            return data.get("atsign");
        } else {
            throw new AtRegistrarException(connection.getResponseCode() + " " + connection.getResponseMessage());
        }
    }

    /**
     *
     * @param registrarUrl - URL of the atsign registrar API
     * @param apiKey       - Super API belonging to the user that has suitable
     *                     access to the registrar API
     * @return Map containing atSign and ActivationKey as separate entries.
     *         Ex: {"atSign": "randomAtsign", "ActivationKey": "valueActivationKey"}
     * @throws AtException - thrown if invalid method parameters provided
     * @throws IOException - thrown if anything goes wrong while using the
     *                     HttpsURLConnection.
     */
    public Map<String, String> getAtsignV3(String registrarUrl, String apiKey) throws IOException, AtException {
        return getAtsignV3(registrarUrl, apiKey, "", "");
    }

    /**
     *
     * @param registrarUrl  - URL of the atsign registrar API
     * @param apiKey        - Super API belonging to the user that has suitable
     *                      access to the registrar API
     * @param atsign     - [Optional] User can choose to provide atsign. Note:
     *                      The final atsign will be in the following format:
     *                      SuperApi prefix + atsign + SuperApi postfix
     * @param activationKey - [Optional] User can choose to provide an ActivationKey
     *                      which will be set as activationKey for the corresponding
     *                      atsign.
     * @return Map containing atSign and ActivationKey as separate entries.
     *         Ex: {"atSign": "randomAtsign", "ActivationKey": "valueActivationKey"}
     * @throws AtException - thrown if invalid method parameters provided
     * @throws IOException - thrown if anything goes wrong while using the
     *                     HttpsURLConnection.
     */
    public Map<String, String> getAtsignV3(String registrarUrl, String apiKey, String atsign,
            String activationKey)
            throws AtException, IOException {
        Map<String, String> paramsMap = new HashMap<>();
        if (!atsign.isEmpty()) {
            paramsMap.put("atSign", new AtSign(atsign).withoutPrefix());
        }
        if (!activationKey.isEmpty()) {
            paramsMap.put("ActivationKey", activationKey);
        }
        String paramsJson = objectMapper.writeValueAsString(paramsMap);
        HttpsURLConnection httpsConnection = postRequestToAPI(new URL(registrarUrl + Constants.GET_ATSIGN_V3), apiKey, paramsJson);
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream()));
            String responseRaw = bufferedReader.readLine();
            @SuppressWarnings("unchecked") Map<String, String> responseData = objectMapper.readValue(responseRaw, Map.class);
            if (responseData.get("status").equals("success")) {
                @SuppressWarnings("unchecked") Map<String, Map<String, String>> responseDataMap = objectMapper.readValue(responseRaw, Map.class);
                return responseDataMap.get("value");
            } else {
                throw new AtRegistrarException("Failed getting atsign. Response from API: " + responseData.get("status"));
            }
        } else {
            throw new AtRegistrarException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

    /**
     * Accepts email and an unpaired atsign. Method pairs free atsign with email.
     * Sends the one-time-password to the provided email.
     * Returns bool, true if OTP sent or False otherwise.
     * 
     * @param email        - email initially provided to register this email
     * @param atsign       - atsign that is being registered
     * @param registrarUrl - URL of the atsign registrar API
     * @param apiKey       - API key to authenticate connection to atsign registrar
     *                     API
     * @return true if one-time-password sent successfully, false otherwise
     * @throws AtException thrown if HTTPS_REQUEST was not successful
     * @throws IOException if anything goes wrong while using the HttpsURLConnection
     */
    public Boolean registerAtsign(String email, AtSign atsign, String registrarUrl, String apiKey)
            throws AtException, IOException {
        Map<String, String> paramsMap = Stream.of(
                new SimpleEntry<>("atsign", atsign.withoutPrefix()),
                new SimpleEntry<>("email", email))
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
        String paramsJson = objectMapper.writeValueAsString(paramsMap);

        HttpsURLConnection httpsConnection = postRequestToAPI(new URL(registrarUrl + Constants.REGISTER_ATSIGN), apiKey, paramsJson);
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            String response = bufferedReader.readLine();
            @SuppressWarnings("unchecked") Map<String, String> responseData = objectMapper.readValue(response, Map.class);
            String data = responseData.get("message");
            System.out.println("Got response: " + data);
            return response.contains("Sent Successfully");
        } else {
            throw new AtRegistrarException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

    /**
     * Accepts email, unpaired atsign, and the otp received on the provided email.
     * Validates the OTP against the atsign and registers it to the provided email
     * if OTP is valid.
     * Returns the CRAM secret of the atsign which is registered.
     * 
     * @param email        - email initially provided to register this email
     * @param atsign       - atsign that is being registered
     * @param otp          - one-time-password received on provided email for
     *                     provided atsign
     * @param registrarUrl - URL of the atsign registrar API
     * @param apiKey       - API key to authenticate connection to atsign registrar
     *                     API
     * @param confirmation - Mandatory parameter for validateOTP API call. First
     *                     request to be sent with confirmation as false, in this
     *                     case API will return cram key if the user is new
     *                     otherwise will return list of already existing atsigns.
     *                     If the user already has existing atsigns user will have
     *                     to select a listed atsign old/new and place a second call
     *                     to the same API endpoint with confirmation set to true
     *                     with previously received OTP. The second follow-up call
     *                     is automated by this client using new atsign for user
     *                     simplicity
     * @return Case 1("verified") - the API has registered the atsign to
     *         provided email and CRAM key present in HTTP_RESPONSE Body.
     *         Case 2("follow-up"): User already has existing atsigns and new atsign
     *         registered successfully. To receive the CRAM key, follow-up by calling
     *         the API with one of the existing listed atsigns, with confirmation set to true.
     *         Case 3("retry"): Incorrect OTP send request again with correct OTP.
     * @throws IOException thrown if anything goes wrong while using the HttpsURLConnection.
     * @throws AtException Case 1: If user has exhausted 10 free atsign quota
     *                     Case 2: If API response is anything other than HTTP_OK/Status_200.
     */
    public String validateOtp(String email, AtSign atsign, String otp, String registrarUrl, String apiKey,
            Boolean confirmation)
            throws IOException, AtException {
        Map<String, String> paramsMap = Stream.of(
                    new SimpleEntry<>("atsign", atsign.withoutPrefix()),
                    new SimpleEntry<>("email", email),
                    new SimpleEntry<>("otp", otp),
                    new SimpleEntry<>("confirmation", confirmation.toString()))
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
        String paramsJson = objectMapper.writeValueAsString(paramsMap);

        HttpsURLConnection httpsConnection = postRequestToAPI(new URL(registrarUrl + Constants.VALIDATE_OTP), apiKey, paramsJson);

        // reading response received for the HTTP_REQUEST_POST
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            // appending HTTP_RESPONSE to the string buffer line-after-line
            String response = bufferedReader.readLine();
            @SuppressWarnings("unchecked") Map<String, String> responseDataStringObject = objectMapper.readValue(response, Map.class);
            // API in some cases returns response with a data field of Type
            // Map<String, Map<String, String>> the following if condition casts this
            // response to Map<String, String>
            if (response.startsWith("{\"data")) {
                @SuppressWarnings("unchecked") Map<String, Map<String, String>> responseDataMapObject = objectMapper.readValue(response, Map.class);
                responseDataStringObject = responseDataMapObject.get("data");
                // The following if condition logs the existing atsigns if the API response
                // contains a List<String> of atsigns.
                if (responseDataStringObject.containsKey("atsigns")
                        || responseDataStringObject.containsKey("newAtsign")) {
                    @SuppressWarnings("unchecked") Map<String, Map<String, List<String>>> responseDataArrayListObject = objectMapper.readValue(response, Map.class);
                    System.out.println("Your existing atsigns: "
                            + responseDataArrayListObject.get("data").get("atsigns").toString());
                }
            }
            if (responseDataStringObject.containsKey("message")
                    && "Verified".equals(responseDataStringObject.get("message"))) {
                return responseDataStringObject.get("cramkey");
            } else if (responseDataStringObject.containsKey("newAtsign")
                    && responseDataStringObject.get("newAtsign").equals(atsign.withoutPrefix())) {
                return "follow-up";
            } else if (responseDataStringObject.containsKey("message")
                    && responseDataStringObject.get("message").contains("Try again")) {
                return "retry";
            } else if (responseDataStringObject.containsKey("message") && responseDataStringObject.get("message")
                    .contains("You already have the maximum number of free @signs")) {
                throw new AtRegistrarException("Maximum free atsigns reached for email");
            } else {
                return responseDataStringObject.get("message");
            }
        } else {
            throw new AtRegistrarException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

    /**
     * Activates the atsign provided using activationKey provided through method
     * parameters
     *
     * @param registrarUrl  - URL of the atsign registrar API
     * @param apiKey        - Super API belonging to the user that has suitable
     *                      access to the registrar API
     * @param atsign        - atsign received as a response from GetAtsignV3
     * @param activationKey - activationKey corresponding to the atsign being
     *                      provided
     * @return cram secret for the atsign
     * @throws AtException - thrown if invalid method parameters provided
     * @throws IOException - thrown if anything goes wrong while using the
     *                     HttpsURLConnection.
     */
    public String activateAtsign(String registrarUrl, String apiKey, AtSign atsign, String activationKey)
            throws AtException, IOException {
        Map<String, String> paramsMap = Stream.of(
                new SimpleEntry<>("atSign", atsign.withoutPrefix()),
                new SimpleEntry<>("ActivationKey", activationKey))
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));

        String paramsJson = objectMapper.writeValueAsString(paramsMap);
        HttpsURLConnection httpsConnection = postRequestToAPI(new URL(registrarUrl + Constants.ACTIVATE_ATSIGN), apiKey,
                paramsJson);
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream()));
            String response = bufferedReader.readLine();
            @SuppressWarnings("unchecked") Map<String, String> responseData = objectMapper.readValue(response, Map.class);
            if (responseData.get("status").equals("success")) {
                return responseData.get("cramkey");
            } else {
                throw new AtRegistrarException(responseData.get("status"));
            }
        } else {
            throw new AtRegistrarException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }

    }

    /**
     * @deprecated method remains for backwards compatibility. will be removed in
     * future minor updates
     * <p>
     * <p>
     * This method just calls
     * {@link #validateOtp(String, AtSign, String, String, String, Boolean)
     * the new validateOtp} with confirmation set to true
     */
    @Deprecated
    public String validateOtp(String email, AtSign atsign, String otp, String registrarUrl, String apiKey)
            throws IOException, AtException {
        return validateOtp(email, atsign, otp, registrarUrl, apiKey, true);
    }

    private HttpsURLConnection postRequestToAPI(URL url, String apiKey, String paramsJson) throws IOException {
        HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();

        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", apiKey);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        outputStream.write(paramsJson.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        return httpsConnection;
    }
}
