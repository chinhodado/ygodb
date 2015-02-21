package com.chin.ygodb;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Helper class for database provisioning
 * @author Chin
 *
 */
public class YGOSqliteDatabase extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "ygo.db";
    public static final int DATABASE_VERSION = 20150221;

    public YGOSqliteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
    }
}