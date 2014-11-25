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
    private static final int DATABASE_VERSION = 20141125;

    public YGOSqliteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
    }
}