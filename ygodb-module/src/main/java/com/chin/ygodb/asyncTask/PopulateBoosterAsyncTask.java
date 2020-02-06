package com.chin.ygodb.asyncTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.chin.common.HtmlUtil;
import com.chin.ygodb.activity.BoosterActivity;
import com.chin.ygodb.dataSource.BoosterStore;
import com.chin.ygodb.entity.Booster;
import com.chin.ygodb.html.BoosterParser;
import com.chin.ygodb.R;

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
            BoosterStore.getInstance(activity).init();
            String baseUrl = null;
            if (type.equals(BoosterActivity.TYPE_TCG)) {
                baseUrl = "https://yugioh.wikia.com/api/v1/Articles/List?category=TCG_Booster_Packs&limit=5000&namespaces=0";
            }
            else {
                baseUrl = "https://yugioh.wikia.com/api/v1/Articles/List?category=OCG_Booster_Packs&limit=5000&namespaces=0";
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

            // get the shared preferences as a (booster link, [enReleaseDate, jpReleaseDate]) HashMap
            final String dateMapFileName = "boosterDate.txt";
            preferences = activity.getSharedPreferences(dateMapFileName, Context.MODE_PRIVATE);
            final Editor datePrefEditor = preferences.edit();
            @SuppressWarnings("unchecked")
            HashMap<String, String> dateMap = new HashMap<>((Map<String, String>) preferences.getAll());

            final List<Booster> boosters = activity.getBoosterList();
            BoosterStore boosterStore = BoosterStore.getInstance(activity);

            // loop through the booster list and display them
            for (Map.Entry<String, String> entry : boosterUrls.entrySet()) {
                final String boosterName = entry.getKey();
                final String boosterLink = entry.getValue();

                // ignore the aggregate articles
                if (boosterName.equals("Astral Pack") || boosterName.equals("OTS Tournament Pack")) {
                    continue;
                }

                // if the booster is already in the store, use it
                if (boosterStore.hasBooster(boosterName)) {
                    Booster booster = boosterStore.getBooster(boosterName);

                    String scaledImgSrc = booster.getScaledImgSrc();
                    if (scaledImgSrc == null) {
                        try {
                            String img = booster.getShortenedImgSrc();
                            String originalLink = HtmlUtil.getFullImageLink(img);
                            scaledImgSrc = HtmlUtil.getScaledWikiaImageLink(originalLink, scaleWidth);
                            booster.setScaledImgSrc(scaledImgSrc);
                        }
                        catch (Exception e) {
                            // usually happens when the booster have no image
                            booster.setScaledImgSrc("drawable://" + R.drawable.no_image_available);
                        }
                    }
                    booster.setUrl(boosterLink);

                    addToBoosterList(boosters, booster);
                    sortBoosterList(boosters);
                    recyclerView.getAdapter().notifyDataSetChanged();
                    continue;
                }

                final Booster booster = new Booster();
                booster.setName(boosterName);
                booster.setUrl(boosterLink);
                boosterStore.addBooster(boosterName, booster);
                addToBoosterList(boosters, booster);

                // if the booster is not in the store but in shared preference (e.g. after an app restart)
                if (imgLinkMap.containsKey(boosterLink)) {
                    String imgSrc = imgLinkMap.get(boosterLink);

                    booster.setScaledImgSrc(imgSrc);
                    String[] tokens = dateMap.get(boosterLink).split("|");
                    booster.setEnReleaseDate(tokens[0]);
                    booster.setJpReleaseDate(tokens[1]);

                    sortBoosterList(boosters);
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
                else {
                    new AsyncTask<String, Void, String[]>(){
                        boolean exceptionOccurred2 = false;
                        @Override
                        protected String[] doInBackground(String... params) {
                            String imgSrc = null;
                            String enDate = "January 1, 1970", jpDate = "January 1, 1970";
                            try {
                                String html = Jsoup.connect("https://yugioh.wikia.com" + boosterLink)
                                        .ignoreContentType(true).execute().body();
                                Document dom = Jsoup.parse(html);
                                // note: we deliberately don't use the cached version of BoosterParser
                                // here so that multiple threads don't have to wait for each other,
                                // and also because we don't care about caching at this point yet
                                BoosterParser parser = new BoosterParser(boosterName, dom);

                                // get the image link
                                imgSrc = parser.getImageLink();
                                if (imgSrc != null) {
                                    // get the scaled image link
                                    imgSrc = HtmlUtil.getScaledWikiaImageLink(imgSrc, scaleWidth);
                                }
                                else {
                                    // use the placeholder image
                                    imgSrc = "drawable://" + R.drawable.no_image_available;
                                }

                                String tmpDate = parser.getEnglishReleaseDate();
                                if (tmpDate != null) {
                                    enDate = tmpDate;
                                }

                                tmpDate = parser.getJapaneseReleaseDate();
                                if (tmpDate != null) {
                                    jpDate = tmpDate;
                                }

                                imgSrcPrefEditor.putString(boosterLink, imgSrc);
                                imgSrcPrefEditor.commit();

                                datePrefEditor.putString(boosterLink, enDate + "|" + jpDate);
                                datePrefEditor.commit();

                                Log.i("foo", "Fetched " + boosterLink + " from scratch, saved to cache");
                            } catch (Exception e) {
                                Log.w("ygodb", "Failed to fetch " + boosterLink + "'s img link");
                                e.printStackTrace();
                                // set the flag so we can do something about this in onPostExecute()
                                exceptionOccurred2 = true;
                            }

                            return new String[] {imgSrc, enDate, jpDate};
                        }

                        @Override
                        protected void onPostExecute(String[] params) {
                            if (exceptionOccurred2) return;

                            booster.setScaledImgSrc(params[0]);
                            booster.setEnReleaseDate(params[1]);
                            booster.setJpReleaseDate(params[2]);

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
        final SortOrder order = type.equals(BoosterActivity.TYPE_TCG) ?
                SortOrder.EN_RELEASE_DATE_DES : SortOrder.JP_RELEASE_DATE_DES;
        Collections.sort(boosters, new Comparator<Booster>() {
            @Override
            public int compare(Booster o1, Booster o2) {
                if (order == SortOrder.EN_RELEASE_DATE_DES) {
                    return o2.getEnReleaseDate().compareTo(o1.getEnReleaseDate());
                }
                else if (order == SortOrder.JP_RELEASE_DATE_DES) {
                    return o2.getJpReleaseDate().compareTo(o1.getJpReleaseDate());
                }

                return 0;
            }
        });
    }

    private enum SortOrder {
        EN_RELEASE_DATE_DES,
        JP_RELEASE_DATE_DES,
    }
}
