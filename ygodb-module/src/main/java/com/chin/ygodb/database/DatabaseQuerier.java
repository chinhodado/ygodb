package com.chin.ygodb.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.chin.ygodb.dataSource.CardStore;
import com.chin.ygodb.entity.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for making queries to our sqlite database
 * @author Chin
 */
public class DatabaseQuerier {
    private static SQLiteDatabase db;
    private Context context;

    public DatabaseQuerier(Context context) {
        this.context = context;
    }

    public SQLiteDatabase getDatabase() {
        if (db == null) {
            YgoSqliteDatabase dbHelper = new YgoSqliteDatabase(context);
            db = dbHelper.getReadableDatabase();
        }
        return db;
    }

    /**
     * Execute a search query and return the result
     * @param criteria The where clause that represents the criteria of the search
     * @return List of cards that satisfy the criteria
     */
    public List<Card> executeQuery(String criteria) {
    	Log.i("Search", "Criteria: " + criteria);
        List<Card> resultSet = new ArrayList<>();
        try {
            SQLiteDatabase db = getDatabase();

            Cursor cursor = db.rawQuery("Select name, attribute, cardType, types, level, atk, def, link, rank, pendulumScale, property "
                                      + "from card where " + criteria + " order by name", null);
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    String name = cursor.getString(cursor.getColumnIndex("name"));

                    // verify that the card really exists in our database
                    if (CardStore.cardNameList.contains(name)) {
                        Card card = new Card();
                        card.name = name;
                        // note that we don't need all card info here - just those needed for displaying in the ListView
                        card.attribute        = cursor.getString(cursor.getColumnIndex("attribute"));
                        card.cardType         = cursor.getString(cursor.getColumnIndex("cardType"));
                        card.types            = cursor.getString(cursor.getColumnIndex("types"));
                        card.level            = cursor.getString(cursor.getColumnIndex("level"));
                        card.atk              = cursor.getString(cursor.getColumnIndex("atk"));
                        card.def              = cursor.getString(cursor.getColumnIndex("def"));
                        card.link             = cursor.getString(cursor.getColumnIndex("link"));
                        card.rank             = cursor.getString(cursor.getColumnIndex("rank"));
                        card.pendulumScale    = cursor.getString(cursor.getColumnIndex("pendulumScale"));
                        card.property         = cursor.getString(cursor.getColumnIndex("property"));
                        resultSet.add(card);
                    }
                    else {
                        Log.i("Search", "Not found: " + name);
                    }

                    cursor.moveToNext();
                }
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(context, "An error occurred while searching.", Toast.LENGTH_SHORT);
            toast.show();
        }

        return resultSet;
    }
}
