package org.atsign.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
     * @param registrarUrl
     * @return
     * @throws AtException
     * @throws MalformedURLException
     * @throws IOException
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
     * @param email
     * @param atsign
     * @param registrarUrl
     * @return
     * @throws AtException
     * @throws MalformedURLException
     * @throws IOException
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
     * @param email
     * @param atsign
     * @param otp
     * @param registrarUrl
     * @return
     * @throws IOException
     * @throws AtException
     */
    public String validateOtp(String email, AtSign atsign, String otp, String registrarUrl, String apiKey,
            boolean confirmation)
            throws IOException, AtException {
        //creation of a new URL object from provided URL parameters
        URL validateOtpUrl = new URL(registrarUrl + Constants.VALIDATE_OTP);
        //opens a stream type connetion to the above URL
        HttpsURLConnection httpsConnection = (HttpsURLConnection) validateOtpUrl.openConnection();
        String params = "{\"atsign\":\"" + atsign.withoutPrefix() + "\", \"email\":\"" + email + "\", \"otp\":\"" + otp
                + "\", \"confirmation\":\"" + confirmation + "\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", apiKey);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        //writing POST Body on the output stream stands equal to sending a body with HTTP_POST
        outputStream.write(params.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        //reading response received for the HTTP_REQUEST_POST
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            //appending HTTP_RESPONSE to the string buffer line-after-line
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> responseDataStringObject = objectMapper.readValue(response.toString(), Map.class);
            // API some cases returns response of Type Map<String, Map<String, String>> the
            // following if condition casts this response to Map<String, String>
            if (response.toString().startsWith("{\"data")) {
                Map<String, Map<String, String>> responseDataMapObject = objectMapper.readValue(response.toString(),
                        Map.class);
                responseDataStringObject = responseDataMapObject.get("data");
            }
            System.out.println("Got response: " + responseDataStringObject.get("message"));
            if ("Verified".equals(responseDataStringObject.get("message"))) {
                return responseDataStringObject.get("cramkey");
            } else if (responseDataStringObject.containsKey("newAtsign")
                    && responseDataStringObject.get("newAtsign").equals(atsign.withoutPrefix())) {
                return "follow-up";
            } else if (responseDataStringObject.get("message").contains("Try again")) {
                return "retry";
            } else if (responseDataStringObject.get("message")
                    .contains("You already have the maximum number of free @signs")) {
                throw new AtException("Maximum free atsigns reached for email");
            } else {
                return responseDataStringObject.get("message");
            }
        } else {
            throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

}
