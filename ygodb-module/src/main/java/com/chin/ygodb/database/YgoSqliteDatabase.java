package com.chin.ygodb.database;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import android.content.Context;

/**
 * Helper class for database provisioning
 * @author Chin
 *
 */
public class YgoSqliteDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "ygo.db";
    public static final int DATABASE_VERSION = 20200419;

    public YgoSqliteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
    }
}