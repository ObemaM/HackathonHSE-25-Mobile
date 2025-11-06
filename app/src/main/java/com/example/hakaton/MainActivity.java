package com.example.hakaton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editTextBrigade;
    private Button buttonA, buttonB, buttonSend;
    private AppDao appDao; // ← Room DAO
    private Handler handler = new Handler(Looper.getMainLooper());
    private String deviceCode;
    private String appVersion = "1";
    private TextView textBottomInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFirstRun()) {
            // Если первый запуск - переходим к настройке
            startActivity(new Intent(this, SetUpActivity.class));
            finish(); // Закрываем MainActivity
            return;   // Не продолжаем загрузку
        }

        setContentView(R.layout.activity_main);

        // Инициализация UI
        editTextBrigade = findViewById(R.id.editTextBrigade);
        buttonA = findViewById(R.id.buttonA);
        buttonB = findViewById(R.id.buttonB);
        buttonSend = findViewById(R.id.buttonSend);
        textBottomInfo = findViewById(R.id.textBottomInfo);
        updateBottomText();

        // Инициализация Room
        AppDatabase db = AppDatabase.getDatabase(this);
        appDao = db.appDao();

        // Получаем device_code (постоянный для устройства)
        deviceCode = getUniqueDeviceId();

        // Обработчики кнопок
        buttonA.setOnClickListener(v -> logAction("1", "Кнопка A нажата"));
        buttonB.setOnClickListener(v -> logAction("2", "Кнопка B нажата"));
        buttonSend.setOnClickListener(v -> sendUnsentLogsToServer());

        // Предварительное создание действий в базе
        initializeActions();

        // Фоновая отправка каждые 10 минут
        startBackgroundSender();
    }

    // Получение уникального Id девайса
    private String getUniqueDeviceId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    // Инициализация действий
    private void initializeActions() {
        new Thread(() -> {
            try {
                appDao.insertAction(new Action("Кнопка A нажата", "1", appVersion));
            } catch (Exception e) {

            }

            try {
                appDao.insertAction(new Action("Кнопка B нажата", "2", appVersion));
            } catch (Exception e) {

            }
        }).start();
    }

    // Запись лога в локальную БД
    private void logAction(String actionCode, String actionText) {
        String brigadeNumber = editTextBrigade.getText().toString().trim();
        if (brigadeNumber.isEmpty()) {
            Toast.makeText(this, "Введите номер бригады", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            // Создаем лог
            Log log = new Log(actionCode, appVersion, deviceCode);
            appDao.insertLog(log);

            runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "Событие записано", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }

    // Отправка логов на сервер
    private void sendUnsentLogsToServer() {
        new Thread(() -> {
            appDao.getAllLogs(); // Обновляем кэш
            List<Log> unsentLogs = appDao.getUnsentLogs();
            if (unsentLogs.isEmpty()) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Нет данных для отправки", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            try {
                // Конвертируем логи в JSON
                String jsonPayload = convertLogsToJson(unsentLogs);

                // Отправляем на сервер
                boolean success = sendToServer(jsonPayload);

                if (success) {
                    // Помечаем логи как отправленные
                    for (Log log : unsentLogs) {
                        appDao.markLogAsSent(log.getDeviceCode(), log.getDatetime());
                    }

                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Успешно отправлено " + unsentLogs.size() + " записей",
                                    Toast.LENGTH_SHORT).show()
                    );
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Ошибка отправки", Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // Конвертация логов в JSON
    private String convertLogsToJson(List<Log> logs) {
        JSONArray jsonArray = new JSONArray();
        try {
            SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
            String regionCode = prefs.getString("region_code", "");
            String smcCode = prefs.getString("smc_code", "");
            String brigadeNumber = editTextBrigade.getText().toString().trim();

            for (Log log : logs) {
                JSONObject jsonLog = new JSONObject();
                jsonLog.put("region_code", regionCode);
                jsonLog.put("smp_code", smcCode);
                jsonLog.put("team_number", brigadeNumber);
                jsonLog.put("action_code", log.getActionCode());
                jsonLog.put("app_version", log.getAppVersion());
                jsonLog.put("device_code", log.getDeviceCode());
                jsonLog.put("datetime", log.getDatetime());
                jsonArray.put(jsonLog);
            }
            return jsonArray.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "[]";
        }
    }

    // Отправка конвертированных в JSON логов на сервер
    private boolean sendToServer(String jsonPayload) {
        try {
            URL url = new URL("http://10.0.2.2:8080/api/logs");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            // Отправляем данные
            OutputStream os = connection.getOutputStream();
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            os.close();

            // Проверяем что ответ 200
            int responseCode = connection.getResponseCode();
            return responseCode == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Начало отсчета 10 минут на для отправки логов на фоне
    private void startBackgroundSender() {
        Runnable senderTask = new Runnable() {
            @Override
            public void run() {
                sendUnsentLogsToServer();
                handler.postDelayed(this, 10 * 60 * 1000);
            }
        };
        handler.postDelayed(senderTask,  10 * 60 * 1000);
    }

    // Проверка на первый запуск приложения
    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        return prefs.getBoolean("is_first_run", true);
    }

    // Отображение информации о версии, коде региона и коде СМП
    private void updateBottomText() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String regionCode = prefs.getString("region_code", "");
        String smcCode = prefs.getString("smc_code", "");

        // Используем appVersion из поля класса и данные из настроек
        String bottomText = "Версия: " + appVersion + " | Код региона: " + regionCode + " | Код СМП: " + smcCode;
        textBottomInfo.setText(bottomText);
    }
}