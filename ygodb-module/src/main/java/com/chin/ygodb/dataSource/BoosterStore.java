package com.chin.ygodb.dataSource;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.chin.ygodb.database.DatabaseQuerier;
import com.chin.ygowikitool.entity.Booster;
import com.chin.ygowikitool.parser.YugiohWikiUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provide access to booster data
 *
 * Created by Chin on 11-Feb-17.
 */
public class BoosterStore {
    // we use the application context so don't worry about this
    @SuppressLint("StaticFieldLeak")
    private static BoosterStore INSTANCE;
    private final Context context;
    private boolean doneInit;

    // list of boosters in the offline database
    private static final Set<String> offlineBoosterSet = new HashSet<>(8192);

    // maps a booster name to its Booster object, for both online and offline
    private static final Map<String, Booster> allBoosterMap = new ConcurrentHashMap<>(8192);

    private BoosterStore(Context context) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        this.context = context;
    }

    public static synchronized BoosterStore getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new BoosterStore(context.getApplicationContext());
        }

        return INSTANCE;
    }

    /**
     * Only call this once! Will do heavy work like accessing the db so call this
     * not on the UI thread!
     */
    public synchronized void init() {
        if (doneInit) return;
        Log.i("ygodb", "Initializing BoosterStore");

        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select name, enReleaseDate, jpReleaseDate, imgSrc from booster", null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                Booster booster = new Booster();
                booster.setBoosterName(name);
                booster.setEnReleaseDate(cursor.getString(cursor.getColumnIndexOrThrow("enReleaseDate")));
                booster.setJpReleaseDate(cursor.getString(cursor.getColumnIndexOrThrow("jpReleaseDate")));

                String shortenedImgSrc = cursor.getString(cursor.getColumnIndexOrThrow("imgSrc"));
                booster.setShortenedImgSrc(shortenedImgSrc);
                String fullImgSrc = YugiohWikiUtil.getFullYugipediaImageLink(shortenedImgSrc);
                booster.setFullImgSrc(fullImgSrc);

                booster.parseEnReleaseDate();
                booster.parseJpReleaseDate();

                offlineBoosterSet.add(name);
                allBoosterMap.put(name, booster);
                cursor.moveToNext();
            }
        }

        cursor.close();

        doneInit = true;
    }

    public Booster getBooster(String name) {
        return allBoosterMap.get(name);
    }

    public boolean hasBooster(String name) {
        return allBoosterMap.containsKey(name);
    }

    public void addBooster(String name, Booster booster) {
        allBoosterMap.put(name, booster);
    }
}
