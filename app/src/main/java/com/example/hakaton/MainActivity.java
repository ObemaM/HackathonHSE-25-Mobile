package com.example.hakaton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class MainActivity extends AppCompatActivity {

    private EditText editTextBrigade;
    private AppDao appDao;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String deviceCode;
    private final String appVersion = AppConfig.APP_VERSION;
    private TextView textBottomInfo;
    private RecyclerView recyclerViewActions;
    private EditText editTextSearch;
    private ActionsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFirstRun()) {
            startActivity(new Intent(this, SetUpActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Инициализация UI
        editTextBrigade = findViewById(R.id.editTextBrigade);
        setupBrigadeField();
        Button buttonSend = findViewById(R.id.buttonSend);
        recyclerViewActions = findViewById(R.id.recyclerViewActions);
        editTextSearch = findViewById(R.id.editTextSearch);
        textBottomInfo = findViewById(R.id.textBottomInfo);

        // Инициализация Room
        AppDatabase db = AppDatabase.getDatabase(this);
        appDao = db.appDao();

        // Получаем device_code (постоянный для устройства)
        deviceCode = getUniqueDeviceId();

        // Инициализация данных
        initializeAppData();

        // Обработчики кнопок
        setupAntiSpamButton(buttonSend, this::sendUnsentLogsToServer);

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

    // Метод инициализации данных
    private void initializeAppData() {
        new Thread(() -> {
            // Инициализируем действия в БД
            initializeActionsInDatabase();

            // Настраиваем UI
            runOnUiThread(() -> {
                setupActionsUI();
                setupSearch();
                updateBottomText();
            });
        }).start();
    }

    // Инициализация действий в БД
    private void initializeActionsInDatabase() {
        for (ActionItem actionItem : AppConfig.ACTIONS) {
            try {
                Action action = new Action(
                        actionItem.getActionText(),
                        actionItem.getActionCode(),
                        appVersion
                );
                appDao.insertAction(action);
            } catch (Exception e) {

            }
        }
    }

    // Настройка списка действий
    private void setupActionsUI() {
        List<ActionItem> actionItems = AppConfig.ACTIONS;

        recyclerViewActions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActionsAdapter(actionItems, this::onActionClick);
        recyclerViewActions.setAdapter(adapter);
    }

    // Настройка поиска
    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void onActionClick(ActionItem action) {
        logAction(action.getActionCode());
    }

    // Запись лога в локальную БД
    private void logAction(String actionCode) {
        String teamNumber = editTextBrigade.getText().toString().trim();
        if (teamNumber.isEmpty()) {
            Toast.makeText(this, "Введите номер бригады", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            // Создаем лог
            Log log = new Log(teamNumber, actionCode, appVersion, deviceCode);
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
            e.printStackTrace();
            return "[]";
        }
    }

    // Отправка конвертированных в JSON логов на сервер
    private boolean sendToServer(String jsonPayload) {
        try {
            URL url = new URL("http://46.146.248.104:10880/api/mobile/logs/batch");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

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

    // Метод для времени восстановления кнопки
    void setupAntiSpamButton(Button button, Runnable action) {
        button.setOnClickListener(v -> {
            button.setEnabled(false);

            // Действие кнопки
            action.run();

            handler.postDelayed(() -> button.setEnabled(true), 1500);
        });
    }

    // Сохранение номера бригады в памяти
    private void setupBrigadeField() {
        // Восстанавливаем сохраненный номер
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String savedTeamNumber = prefs.getString("team_number", "");
        editTextBrigade.setText(savedTeamNumber);

        // Сохраняем при любом изменении
        editTextBrigade.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String teamNumber = s.toString().trim();
                prefs.edit().putString("team_number", teamNumber).apply();
            }
        });
    }
}