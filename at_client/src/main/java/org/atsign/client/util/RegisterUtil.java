package org.atsign.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

public class RegisterUtil {
    /**
     * Calls API to get atsigns which are ready to be claimed.
     * Returns a free atsign.
     * 
     * @param registrarUrl - URL of the atsign registrar API
     * @param apiKey       - API key to authenticate connection to atsign registrar
     *                     API
     * @return free atsign
     * @throws AtException           thrown if HTTPS_REQUEST was not successful
     * @throws MalformedURLException thrown in case of improper URL params provided
     * @throws IOException           if anything goes wrong while handling I/O
     *                               streams from
     *                               HttpsURLConnection
     */

    public String getFreeAtsign(String registrarUrl, String apiKey)
            throws AtException, MalformedURLException, IOException {
        URL urlObject = new URL(registrarUrl + Constants.GET_FREE_ATSIGN);
        HttpsURLConnection connection = (HttpsURLConnection) urlObject.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", apiKey);
        if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Map<String, String>> responseData = new HashMap<>();
            responseData = objectMapper.readValue(response.toString(), Map.class);
            Map<String, String> data = responseData.get("data");
            return data.get("atsign");
        } else {
            throw new AtException(connection.getResponseCode() + " " + connection.getResponseMessage());
        }
    }

    /**
     * Accepts email and an unpaired atsign. Method pairs free atsign with email.
     * Sends the one-time-password to the provided email.
     * Returns bool, true if OTP sent or False otherwise.
     * 
     * @param email        - email initially provided to register this email
     * @param atsign       - atsign that is being registered
     * @param otp          - one-time-password received on provided email for
     *                     provided atsign
     * @param registrarUrl - URL of the atsign registrar API
     * @param apiKey       - API key to authenticate connection to atsign registrar
     *                     API
     * @return true if one-time-password sent successfully, false otherwise
     * @throws AtException           thrown if HTTPS_REQUEST was not successfull
     * @throws MalformedURLException thrown in case of improper URL params provided
     * @throws IOException           if anything goes wrong while handling I/O
     *                               streams from
     *                               HttpsURLConnection
     */
    public Boolean registerAtsign(String email, AtSign atsign, String registrarUrl, String apiKey)
            throws AtException, MalformedURLException, IOException {
        URL urlObject = new URL(registrarUrl + Constants.REGISTER_ATSIGN);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) urlObject.openConnection();
        String params = "{\"atsign\":\"" + atsign.withoutPrefix() + "\", \"email\":\"" + email + "\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", apiKey);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        outputStream.write(params.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> responseData = new HashMap<>();
            responseData = objectMapper.readValue(response.toString(), Map.class);
            String data = responseData.get("message");
            System.out.println("Got response: " + data);
            if (response.toString().contains("Sent Successfully")) {
                return true;
            }
            return false;
        }
        throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
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
     *         provided email and cramkey present in HTTP_RESPONSE Body.
     *         Case 2("follow-up"): User already has existing atsigns and new atsign
     *         registered usccessfully. To receive cramkey follow-up with API with
     *         one
     *         of exsting listed atsigns with confirmation set to true.
     *         Case 3("retry"): Incorrect OTP send request again with correct OTP.
     * @throws IOException           thrown if anything goes wrong while handling I/O
     *                               streams from
     *                               HttpsURLConnection.
     * @throws AtException           Case 1: If user has exhausted 10 free atsign
     *                               quota
     *                               Case 2: If API response is anything other than
     *                               HTTP_OK/Status_200.
     * @throws MalformedURLException thrown in case of improper URL params provided
     */
    public String validateOtp(String email, AtSign atsign, String otp, String registrarUrl, String apiKey,
            Boolean confirmation)
            throws IOException, AtException, MalformedURLException {
        // creation of a new URL object from provided URL parameters
        URL validateOtpUrl = new URL(registrarUrl + Constants.VALIDATE_OTP);
        // opens a stream type connetion to the above URL
        HttpsURLConnection httpsConnection = (HttpsURLConnection) validateOtpUrl.openConnection();
        String params = "{\"atsign\":\"" + atsign.withoutPrefix() + "\", \"email\":\"" + email + "\", \"otp\":\"" + otp
                + "\", \"confirmation\":\"" + confirmation + "\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", apiKey);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        // writing POST Body on the output stream stands equal to sending a body with
        // HTTP_POST
        outputStream.write(params.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        // reading response received for the HTTP_REQUEST_POST
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            // appending HTTP_RESPONSE to the string buffer line-after-line
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> responseDataStringObject = objectMapper.readValue(response.toString(), Map.class);
            // API in some cases returns response with a data field of Type
            // Map<String, Map<String, String>> the following if condition casts this
            // response to Map<String, String>
            if (response.toString().startsWith("{\"data")) {
                Map<String, Map<String, String>> responseDataMapObject = objectMapper.readValue(response.toString(),
                        Map.class);
                responseDataStringObject = responseDataMapObject.get("data");
                // The following if condition logs the existing atsigns if the API response
                // contains a List<String> of atsigns.
                if (responseDataStringObject.containsKey("atsigns")
                        || responseDataStringObject.containsKey("newAtsign")) {
                    Map<String, Map<String, List<String>>> responseDataArrayListObject = objectMapper
                            .readValue(response.toString(), Map.class);
                    System.out.println("Your existing atsigns: "
                            + responseDataArrayListObject.get("data").get("atsigns").toString());
                }
            }
            System.out.println("Got response: " + responseDataStringObject.get("message"));
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
                throw new AtException("Maximum free atsigns reached for email");
            } else {
                return responseDataStringObject.get("message");
            }
        } else {
            throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

    // method added for backwards compatability. will be removed in further updates
    public String validateOtp(String email, AtSign atsign, String otp, String registrarUrl, String apiKey)
            throws IOException, AtException {
        return validateOtp(email, atsign, otp, registrarUrl, apiKey, true);
    }

}
