package com.example.hakaton;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AppDao {
    // Операции для Actions
    @Insert
    void insertAction(Action action);

    @Query("SELECT * FROM actions WHERE action_code = :actionCode AND app_version = :appVersion")
    Action getAction(String actionCode, String appVersion);

    @Query("SELECT * FROM actions")
    List<Action> getAllActions();

    // Операции для Logs
    @Insert
    void insertLog(Log log);

    @Query("SELECT * FROM logs WHERE is_sent = 0 ORDER BY datetime DESC")
    List<Log> getUnsentLogs();

    @Query("SELECT * FROM logs ORDER BY datetime DESC")
    List<Log> getAllLogs();

    @Query("UPDATE logs SET is_sent = 1 WHERE device_code = :deviceCode AND datetime = :datetime")
    void markLogAsSent(String deviceCode, String datetime);

    @Query("DELETE FROM logs WHERE is_sent = 1 AND datetime < :olderThan")
    void deleteOldSentLogs(String olderThan);

    @Query("SELECT * FROM logs WHERE device_code = :deviceCode ORDER BY datetime DESC")
    List<Log> getLogsByDevice(String deviceCode);
}