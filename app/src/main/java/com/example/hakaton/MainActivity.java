package com.example.hakaton;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editTextBrigade;
    private Button buttonA, buttonB, buttonSend;
    private AppDao appDao; // ← Room DAO
    private Handler handler = new Handler(Looper.getMainLooper());
    private String deviceCode;
    private String appVersion = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация UI
        editTextBrigade = findViewById(R.id.editTextBrigade);
        buttonA = findViewById(R.id.buttonA);
        buttonB = findViewById(R.id.buttonB);
        buttonSend = findViewById(R.id.buttonSend);

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

    private String getUniqueDeviceId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

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

    private void sendUnsentLogsToServer() {
        new Thread(() -> {
            List<Log> unsentLogs = appDao.getUnsentLogs();
            if (unsentLogs.isEmpty()) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Нет данных для отправки", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // Реальная отправка на сервер
            String jsonPayload = convertLogsToJson(unsentLogs);
            boolean success = NetworkUtils.sendEvents(jsonPayload);

            if (success) {
                for (Log log : unsentLogs) {
                    appDao.markLogAsSent(log.getDeviceCode(), log.getDatetime());
                }
                runOnUiThread(() ->
                        Toast.makeText(this, "Отправлено " + unsentLogs.size() + " записей", Toast.LENGTH_SHORT).show()
                );
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void startBackgroundSender() {
        Runnable senderTask = new Runnable() {
            @Override
            public void run() {
                sendUnsentLogsToServer();
                handler.postDelayed(this, 10 * 60 * 1000);
            }
        };
        handler.postDelayed(senderTask, 10 * 60 * 1000);
    }

    private String convertLogsToJson(List<Log> logs) {
        // Реализация конвертации в JSON
        return "[]"; // Заглушка
    }
}