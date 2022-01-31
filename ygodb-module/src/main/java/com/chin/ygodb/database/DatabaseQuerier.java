package com.chin.ygodb.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.chin.ygodb.dataSource.CardStore;
import com.chin.ygowikitool.entity.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for making queries to our sqlite database
 * @author Chin
 */
public class DatabaseQuerier {
    private static SQLiteDatabase db;
    private final Context context;

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
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));

                    // verify that the card really exists in our database
                    if (CardStore.cardNameList.contains(name)) {
                        Card card = new Card();
                        card.setName(name);
                        // note that we don't need all card info here - just those needed for displaying in the ListView
                        card.setAttribute(cursor.getString(cursor.getColumnIndexOrThrow("attribute")));
                        card.setCardType(cursor.getString(cursor.getColumnIndexOrThrow("cardType")));
                        card.setTypes(cursor.getString(cursor.getColumnIndexOrThrow("types")));
                        card.setLevel(cursor.getString(cursor.getColumnIndexOrThrow("level")));
                        card.setAtk(cursor.getString(cursor.getColumnIndexOrThrow("atk")));
                        card.setDef(cursor.getString(cursor.getColumnIndexOrThrow("def")));
                        card.setLink(cursor.getString(cursor.getColumnIndexOrThrow("link")));
                        card.setRank(cursor.getString(cursor.getColumnIndexOrThrow("rank")));
                        card.setPendulumScale(cursor.getString(cursor.getColumnIndexOrThrow("pendulumScale")));
                        card.setProperty(cursor.getString(cursor.getColumnIndexOrThrow("property")));
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
