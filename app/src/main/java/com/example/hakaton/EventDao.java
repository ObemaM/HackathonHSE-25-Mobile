package com.example.hakaton;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EventDao {
    @Insert
    void insertEvent(Event event);

    @Query("SELECT * FROM events ORDER BY eventTime DESC")
    List<Event> getAllEvents();

    @Query("DELETE FROM events")
    void deleteAllEvents();
}