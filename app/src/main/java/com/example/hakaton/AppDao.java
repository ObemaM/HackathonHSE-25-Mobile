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

    // Операции для Logs
    @Insert
    void insertLog(Log log);

    @Query("SELECT * FROM logs ORDER BY datetime DESC")
    List<Log> getAllLogs();

    @Query("DELETE FROM logs")
    void deleteAllLogs();
}