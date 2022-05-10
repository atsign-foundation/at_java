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
    // Calls API to get atsigns which are ready to be claimed.
    // Returns a free atsign.
    public String getFreeAtsign() throws AtException, MalformedURLException, IOException {
        URL urlObject = new URL(Constants.AT_DEV_DOMAIN + Constants.API_PATH + Constants.GET_FREE_ATSIGN);
        HttpsURLConnection connection = (HttpsURLConnection) urlObject.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", Constants.DEV_API_KEY);
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

    // Accepts email and an unpaired atsign. Method pairs free atsign with email.
    // Sends the one-time-password to the provided email.
    // Returns bool, true if OTP sent or False otherwise.
    public Boolean registerAtsign(String email, AtSign atsign) throws AtException, MalformedURLException, IOException {
        URL urlObject = new URL(Constants.AT_DEV_DOMAIN + Constants.API_PATH + Constants.REGISTER_ATSIGN);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) urlObject.openConnection();
        String params = "{\"atsign\":\"" + atsign.withoutPrefix() + "\", \"email\":\"" + email + "\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", Constants.DEV_API_KEY);
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

    // Accepts email, unpaired atsign, and the otp received on the provided email.
    // Validates the OTP against the atsign and registers it to the
    // provided email if valid.
    // Returns the CRAM secret pertaining to the atsign which is registered.
    public String validateOtp(String email, AtSign atsign, String otp) throws IOException, AtException {
        String validationUrl = Constants.AT_DEV_DOMAIN + Constants.API_PATH + Constants.VALIDATE_OTP;
        URL validateOtpUrl = new URL(validationUrl);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) validateOtpUrl.openConnection();
        String params = "{\"atsign\":\"" + atsign.withoutPrefix() + "\", \"email\":\"" + email + "\", \"otp\":\"" + otp
                + "\", \"confirmation\":\"" + "true\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", Constants.DEV_API_KEY);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        outputStream.write(params.getBytes(StandardCharsets.UTF_8), 0, params.length());
        outputStream.flush();
        outputStream.close();
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> responseData = objectMapper.readValue(response.toString(), Map.class);
            System.out.println("Got response: " + responseData.get("message"));
            if (responseData.get("message").equals("Verified")) {
                return responseData.get("cramkey");
            } else if (responseData.get("message").contains("Try again")) {
                return "retry";
            } else {
                return responseData.get("message");
            }
        } else {
            throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

}
