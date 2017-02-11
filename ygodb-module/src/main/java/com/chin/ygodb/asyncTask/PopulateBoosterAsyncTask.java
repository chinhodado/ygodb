package com.chin.ygodb.asyncTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.chin.common.Util;
import com.chin.ygodb.activity.BoosterActivity;
import com.chin.ygodb.entity.Booster;
import com.chin.ygodb.html.BoosterParser;
import com.chin.ygodb2.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopulateBoosterAsyncTask extends AsyncTask<String, Void, Void> {
    private RecyclerView recyclerView;
    private BoosterActivity activity;
    private static Map<String, String> boosterUrls; // a map of booster name to links its articles
    private boolean exceptionOccurred = false;
    private Toast toast = null;
    private final Object toastLock = new Object();
    private String type; // TCG or OCG

    public PopulateBoosterAsyncTask(RecyclerView recyclerView, BoosterActivity activity) {
        this.recyclerView = recyclerView;
        this.activity = activity;
        this.type = activity.getType();
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            String baseUrl = null;
            if (type.equals(BoosterActivity.TYPE_TCG)) {
                baseUrl = "http://yugioh.wikia.com/api/v1/Articles/List?category=TCG_Booster_Packs&limit=5000&namespaces=0";
            }
            else {
                baseUrl = "http://yugioh.wikia.com/api/v1/Articles/List?category=OCG_Booster_Packs&limit=5000&namespaces=0";
            }

            String html = Jsoup.connect(baseUrl).ignoreContentType(true).execute().body();

            JSONObject myJSON = new JSONObject(html);
            JSONArray myArray = myJSON.getJSONArray("items");
            boosterUrls = new HashMap<>();
            for (int i = 0; i < myArray.length(); i++) {
                String boosterName = myArray.getJSONObject(i).getString("title");
                String boosterLink = myArray.getJSONObject(i).getString("url");
                boosterUrls.put(boosterName, boosterLink);
            }
        } catch (Exception e) {
            e.printStackTrace();

            // set the flag so we can do something about this in onPostExecute()
            exceptionOccurred = true;
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Void param) {
        if (exceptionOccurred) {
            Toast.makeText(activity, "Something went wrong. Please restart the app and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // calculate the width of the images to be displayed later on
            DisplayMetrics displaymetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = displaymetrics.widthPixels;
            final int scaleWidth = width / 5; // set it to be 1/5 of the screen width

            // get the shared preferences as a (booster link, booster img link) HashMap
            final String imgLinkMapFileName = "boosterImgLink.txt";
            SharedPreferences preferences = activity.getSharedPreferences(imgLinkMapFileName, Context.MODE_PRIVATE);
            final Editor imgSrcPrefEditor = preferences.edit();
            @SuppressWarnings("unchecked")
            HashMap<String, String> imgLinkMap = new HashMap<>((Map<String, String>) preferences.getAll());

            // get the shared preferences as a (booster link, booster release date) HashMap
            final String dateMapFileName = "boosterDate.txt";
            preferences = activity.getSharedPreferences(dateMapFileName, Context.MODE_PRIVATE);
            final Editor datePrefEditor = preferences.edit();
            @SuppressWarnings("unchecked")
            HashMap<String, String> dateMap = new HashMap<>((Map<String, String>) preferences.getAll());

            final List<Booster> boosters = activity.getBoosterList();

            // loop through the booster list and display them
            for (Map.Entry<String, String> entry : boosterUrls.entrySet()) {
                final String boosterName = entry.getKey();
                final String boosterLink = entry.getValue();

                // ignore the aggregate articles
                if (boosterName.equals("Astral Pack") || boosterName.equals("OTS Tournament Pack")) {
                    continue;
                }

                final Booster booster = new Booster();
                booster.setName(boosterName);
                booster.setUrl(boosterLink);
                addToBoosterList(boosters, booster);

                if (imgLinkMap.containsKey(boosterLink)) {
                    String imgSrc = imgLinkMap.get(boosterLink);

                    booster.setImgSrc(imgSrc);
                    booster.setReleaseDate(dateMap.get(boosterLink));

                    sortBoosterList(boosters);
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
                else {
                    new AsyncTask<String, Void, String[]>(){
                        boolean exceptionOccurred2 = false;
                        @Override
                        protected String[] doInBackground(String... params) {
                            String imgSrc = null;
                            String date = "January 1, 1970"; // default date
                            try {
                                String html = Jsoup.connect("http://yugioh.wikia.com" + boosterLink)
                                        .ignoreContentType(true).execute().body();
                                Document dom = Jsoup.parse(html);
                                // note: we deliberately don't use the cached version of BoosterParser
                                // here so that multiple threads don't have to wait for each other,
                                // and also because we don't care about caching at this point yet
                                BoosterParser parser = new BoosterParser(activity, boosterName, dom);

                                // get the image link
                                imgSrc = parser.getImageLink();
                                if (imgSrc != null) {
                                    // get the scaled image link
                                    imgSrc = Util.getScaledWikiaImageLink(imgSrc, scaleWidth);
                                }
                                else {
                                    // use the placeholder image
                                    imgSrc = "drawable://" + R.drawable.no_image_available;
                                }

                                String tmpDate = parser.getEnglishReleaseDate();
                                if (tmpDate == null) {
                                    tmpDate = parser.getJapaneseReleaseDate();
                                }

                                if (tmpDate != null) {
                                    date = tmpDate;
                                }

                                imgSrcPrefEditor.putString(boosterLink, imgSrc);
                                imgSrcPrefEditor.commit();

                                datePrefEditor.putString(boosterLink, date);
                                datePrefEditor.commit();

                                Log.i("foo", "Fetched " + boosterLink + " from scratch, saved to cache");
                            } catch (Exception e) {
                                Log.w("ygodb", "Failed to fetch " + boosterLink + "'s img link");
                                e.printStackTrace();
                                // set the flag so we can do something about this in onPostExecute()
                                exceptionOccurred2 = true;
                            }

                            return new String[] {imgSrc, date};
                        }

                        @Override
                        protected void onPostExecute(String[] params) {
                            if (exceptionOccurred2) return;

                            booster.setImgSrc(params[0]);
                            booster.setReleaseDate(params[1]);

                            sortBoosterList(boosters);
                            recyclerView.getAdapter().notifyDataSetChanged();

                            synchronized (toastLock) {
                                // cancel the currently showing toast if any
                                if (toast != null) {
                                    toast.cancel();
                                }

                                // show the new toast for the current booster
                                toast = Toast.makeText(activity, "Fetched info: " + boosterName, Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void addToBoosterList(List<Booster> boosters, Booster booster) {
        boosters.add(booster);
    }

    private synchronized void sortBoosterList(List<Booster> boosters) {
        Collections.sort(boosters, new Comparator<Booster>() {
            @Override
            public int compare(Booster o1, Booster o2) {
                return o2.getReleaseDate().compareTo(o1.getReleaseDate());
            }
        });
    }
}
