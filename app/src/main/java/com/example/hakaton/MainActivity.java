package com.example.hakaton;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editTextBrigade;
    private Button buttonA, buttonB, buttonSend;
    private EventDao eventDao; // ← Room DAO
    private Handler handler = new Handler(Looper.getMainLooper());

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
        EventDatabase db = EventDatabase.getDatabase(this);
        eventDao = db.eventDao();

        // Обработчики кнопок
        String deviceId = getUniqueDeviceId();
        buttonA.setOnClickListener(v -> logEvent("Кнопка A нажата", deviceId));
        buttonB.setOnClickListener(v -> logEvent("Кнопка B нажата", deviceId));
        buttonSend.setOnClickListener(v -> sendAllEventsToServer());

        // Фоновая отправка каждые 10 минут
        startBackgroundSender();
    }

    private String getUniqueDeviceId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    private void logEvent(String text, String deviceId) {
        String brigadeNumber = editTextBrigade.getText().toString().trim();
        if (brigadeNumber.isEmpty()) {
            Toast.makeText(this, "Введите номер бригады", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаём событие
        Event event = new Event(
                "REG001",
                "SMC001",
                brigadeNumber,
                deviceId,
                "Beta-test",
                text
        );

        // Room требует фонового потока!
        new Thread(() -> {
            eventDao.insertEvent(event);
            runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "Событие записано", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }

    private void sendAllEventsToServer() {
        new Thread(() -> {
            // Получаем все события
            // List<Event> events = eventDao.getAllEvents();
            // Отправляем на сервер (пока заглушка)

            runOnUiThread(() ->
                    Toast.makeText(this, "Отправка данных...", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }

    private void startBackgroundSender() {
        Runnable senderTask = new Runnable() {
            @Override
            public void run() {
                sendAllEventsToServer();
                handler.postDelayed(this, 10 * 60 * 1000);
            }
        };
        handler.postDelayed(senderTask, 10 * 60 * 1000);
    }
}