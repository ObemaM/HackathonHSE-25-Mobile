package com.example.hakaton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        requestNotificationPermission();
        startSyncService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Останавливаем сервис при закрытии приложения
        stopSyncService();
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
            List<Log> allLogs = appDao.getAllLogs();
            if (allLogs.isEmpty()) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Нет данных для отправки", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            boolean success = LogSender.sendLogs(MainActivity.this, allLogs);

            if (success) {
                // Удаляем отправленные логи
                appDao.deleteAllLogs();

                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Успешно отправлено " + allLogs.size() + " записей",
                                Toast.LENGTH_SHORT).show()
                );
            } else {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Ошибка отправки", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // Запрашивание разрешение на уведомления
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        123);
            }
        }
    }

    // Начало отсчета 10 минут на для отправки логов на фоне
    private void startSyncService() {
        Intent serviceIntent = new Intent(this, LogSyncService.class);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // Остановка работы сервиса при закрытии
    private void stopSyncService() {
        Intent serviceIntent = new Intent(this, LogSyncService.class);
        stopService(serviceIntent);
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