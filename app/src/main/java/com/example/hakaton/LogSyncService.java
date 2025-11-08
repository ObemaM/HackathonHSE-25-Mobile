package com.example.hakaton;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogSyncService extends Service {
    private static final int NOTIFICATION_ID = 123;
    private ScheduledExecutorService scheduler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Создаем канал уведомлений
        createNotificationChannel();

        // Запускаем как Foreground Service
        startForeground(NOTIFICATION_ID, createSyncNotification());

        // Запускаем отправку логов каждые 10 минут
        startPeriodicSync();

        return START_STICKY;
    }

    private void startPeriodicSync() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::performSync, 10, 10, TimeUnit.MINUTES);
    }

    private void performSync() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(this);
                AppDao appDao = db.appDao();
                List<Log> logs = appDao.getAllLogs();

                if (logs.isEmpty()) {
                    return;
                }

                boolean success = LogSender.sendLogs(this, logs);

                if (success) {
                    appDao.deleteAllLogs();
                    showToast("Отправлено " + logs.size() + " записей");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Notification createSyncNotification() {
        return new NotificationCompat.Builder(this, "sync_channel")
                .setContentTitle("Синхронизация логов")
                .setContentText("Синхронизация активна")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "sync_channel",
                    "Синхронизация",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}