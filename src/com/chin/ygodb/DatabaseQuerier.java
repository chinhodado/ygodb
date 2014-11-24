package com.chin.ygodb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * A class for making queries to our sqlite database
 * @author Chin
 */
public class DatabaseQuerier {
    private static SQLiteDatabase db;
    Context context;

    public DatabaseQuerier(Context context) {
        this.context = context;
    }

    public SQLiteDatabase getDatabase() {
        if (db == null) {
            YGOSqliteDatabase dbHelper = new YGOSqliteDatabase(context);
            db = dbHelper.getReadableDatabase();
        }
        return db;
    }
}
