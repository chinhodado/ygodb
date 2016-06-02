package com.chin.ygodb;

import org.jsoup.Jsoup;

import android.os.AsyncTask;

/**
 * An AsyncTask that performs a network request in the backgound
 */
public class NetworkTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {

        String json = null;
        try {
            json = Jsoup.connect(params[0]).ignoreContentType(true).execute().body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
}
