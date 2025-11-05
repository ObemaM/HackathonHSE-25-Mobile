package com.example.hakaton;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {Event.class}, version = 1, exportSchema = false)
public abstract class EventDatabase extends RoomDatabase {
    public abstract EventDao eventDao();

    private static volatile EventDatabase INSTANCE;

    public static EventDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (EventDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            EventDatabase.class,
                            "events_database"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}