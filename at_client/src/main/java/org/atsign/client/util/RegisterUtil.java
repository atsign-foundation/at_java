package org.atsign.client.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import org.atsign.common.AtException;
import org.json.JSONObject;

public class RegisterUtil {
    public String getFreeAtsign() throws AtException, MalformedURLException, IOException {
        String getAtsignUrl = Constants.AT_DEV_DOMAIN + Constants.API_PATH + Constants.GET_FREE_ATSIGN;
        URL urlObject = new URL(getAtsignUrl);
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
            System.out.println(dataJsonObject.getString("atsign"));
            return dataJsonObject.getString("atsign");
        } else {
            throw new AtException(connection.getResponseCode() + " " + connection.getResponseMessage());
        }
    }

    public Boolean registerAtsign(String email, String atsign) throws AtException, MalformedURLException, IOException {
        String registerUrl = Constants.AT_DEV_DOMAIN + Constants.API_PATH + Constants.REGISTER_ATSIGN;
        URL urlObject = new URL(registerUrl);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) urlObject.openConnection();
        JSONObject postParams = new JSONObject();
        postParams.put("atsign", atsign);
        postParams.put("email", email);
        String params = postParams.toString().replaceAll("\"\"", "\"");
        // RequestBody body = RequestBody.create(mediaType,
        // "{\"atsign\":\"pinegreen7amateur\", \"email\":\"n150232@rguktn.ac.in\"}");
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

    public String validateOtp(String email, String atsign, String otp) throws IOException, AtException {
        URL validateOtpUrl = new URL(Constants.AT_DEV_DOMAIN, Constants.API_PATH, Constants.VALIDATE_OTP);
        HttpsURLConnection httpsConnection = (HttpsURLConnection) validateOtpUrl.openConnection();
        JSONObject postParams = new JSONObject();
        postParams.put("atsign", atsign);
        postParams.put("email", email);
        postParams.put("otp", otp);
        String params = postParams.toString().replaceAll("\"\"", "\"");
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
            JSONObject dataJsonObject = new JSONObject(response.toString());
            System.out.println(dataJsonObject.getString("atsign"));
            if (dataJsonObject.getString("atsign") == "true") {
                return dataJsonObject.getString("cramkey");
            }
            // else{
            // return "fail";
            // }
        } else {
            throw new AtException(httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage());
        }
    }

}
