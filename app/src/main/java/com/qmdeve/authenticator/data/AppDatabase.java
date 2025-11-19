package com.qmdeve.authenticator.data;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.qmdeve.authenticator.data.dao.TokenDao;
import com.qmdeve.authenticator.model.Token;

@Database(entities = {Token.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract TokenDao tokenDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "qm_authenticator.db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}