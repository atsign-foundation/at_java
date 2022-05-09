package org.atsign.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.atsign.common.AtException;
import org.json.JSONObject;

public class RegisterUtil {
    //Calls the API to receive atsigns that are ready to be claimed. Returns a free atsign.
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
            JSONObject dataJsonObject = new JSONObject(response.toString());
            dataJsonObject = dataJsonObject.getJSONObject("data");
            return dataJsonObject.getString("atsign");
        } else {
            throw new AtException(connection.getResponseCode() + " " + connection.getResponseMessage());
        }
    }

    //Accepts your email and an unpaired atsign. This method will pair the free atsign with your email.
    //Sends the one-time-password to the provided email. Returns bool, true if OTP sent or False otherwise.
    public Boolean registerAtsign(String email, String atsign) throws AtException, MalformedURLException, IOException {
        URL urlObject = new URL(Constants.AT_DEV_DOMAIN + Constants.API_PATH + Constants.REGISTER_ATSIGN);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) urlObject.openConnection();
        String params = "{\"atsign\":\"" + atsign + "\", \"email\":\"" + email + "\"}";
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", Constants.DEV_API_KEY);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        outputStream.write(params.getBytes(StandardCharsets.UTF_8), 0, params.length());
        outputStream.flush();
        outputStream.close();
        System.out.println(httpsConnection.getResponseCode());
        System.out.println(httpsConnection.getResponseMessage());
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            if (response.toString().contains("Sent Successfully")) {
                return true;
            }
            return false;
        }
        throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
    }

    //Accepts your email, unpaired atsign, and the otp received on the provided email.
    //Validates the one-time-password against the atsign and registers it to the provided email if valid.
    //Returns the CRAM secret pertaining to the atsign which is registered.
    public String validateOtp(String email, String atsign, String otp) throws IOException, AtException {
        String validationUrl = Constants.AT_DEV_DOMAIN + Constants.API_PATH + Constants.VALIDATE_OTP;
        URL validateOtpUrl = new URL(validationUrl);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) validateOtpUrl.openConnection();
        String params = "{\"atsign\":\"" + atsign + "\", \"email\":\"" + email + "\", \"otp\":\"" + otp
                + "\", \"confirmation\":\"" + "true\"}";
        System.out.println(params);
        httpsConnection.setRequestMethod("POST");
        httpsConnection.setRequestProperty("Content-Type", "application/json");
        httpsConnection.setRequestProperty("Authorization", Constants.DEV_API_KEY);
        httpsConnection.setDoOutput(true);
        OutputStream outputStream = httpsConnection.getOutputStream();
        outputStream.write(params.getBytes(StandardCharsets.UTF_8), 0, params.length());
        outputStream.flush();
        outputStream.close();
        System.out.println(httpsConnection.getResponseCode());
        System.out.println(httpsConnection.getResponseMessage());
        if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    httpsConnection.getInputStream()));
            StringBuffer response = new StringBuffer();
            response.append(bufferedReader.readLine());
            JSONObject dataJsonObject = new JSONObject(response.toString());
            System.out.println(dataJsonObject);
            System.out.println("test" + dataJsonObject.getString("message"));
            System.out.println(dataJsonObject.getString("message")=="Verified");
            if (dataJsonObject.getString("message") == "Verified") {
                System.out.println("Success " + dataJsonObject.getString("cramkey").split(":")[1]);
                return dataJsonObject.getString("cramkey");
            } else if (dataJsonObject.getString("message").contains("Try again")) {
                return "retry";
            } else {
                return "could not validate. Something went wrong";
            }
        } else {
            throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

}
