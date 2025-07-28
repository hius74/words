package com.hius74.words.data_source;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Card.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CardDao getWordDao();
}
