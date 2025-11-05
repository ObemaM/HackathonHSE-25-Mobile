package com.example.hakaton;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetworkUtils {

    private static final String SERVER_URL = "https://your-server.com/api/events"; // Замените на свой URL

    public static boolean sendEvents(String jsonPayload) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            byte[] postData = jsonPayload.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData, 0, postData.length);
            }

            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}