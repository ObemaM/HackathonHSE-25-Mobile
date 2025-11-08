package com.example.hakaton;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LogSender {
    private static final String SERVER_URL = "http://46.146.248.104:10880/api/mobile/logs/batch";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    // Основной метод для отправки логов
    public static boolean sendLogs(Context context, List<Log> logs) {
        if (logs == null || logs.isEmpty()) {
            return false;
        }

        try {
            String jsonPayload = convertLogsToJson(context, logs);
            return sendToServer(jsonPayload);
        } catch (Exception e) {
            return false;
        }
    }

    // Конвертация логов в JSON
    private static String convertLogsToJson(Context context, List<Log> logs) {
        JSONArray jsonArray = new JSONArray();
        try {
            SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            String regionCode = prefs.getString("region_code", "");
            String smcCode = prefs.getString("smc_code", "");

            for (Log log : logs) {
                JSONObject jsonLog = new JSONObject();
                jsonLog.put("region_code", regionCode);
                jsonLog.put("smp_code", smcCode);
                jsonLog.put("team_number", log.getTeamNumber());
                jsonLog.put("action_code", log.getActionCode());
                jsonLog.put("app_version", log.getAppVersion());
                jsonLog.put("device_code", log.getDeviceCode());
                jsonLog.put("datetime", log.getDatetime());
                jsonArray.put(jsonLog);
            }
            return jsonArray.toString();
        } catch (JSONException e) {
            return "[]";
        }
    }

    // Отправка на сервер
    private static boolean sendToServer(String jsonPayload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(SERVER_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // Отправляем данные
            OutputStream os = connection.getOutputStream();
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            os.close();

            // Проверяем ответ
            if (connection.getResponseCode() == 200) {
                String response = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                        .readLine();
                return new JSONObject(response).getBoolean("success");
            }
            return false;

        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}