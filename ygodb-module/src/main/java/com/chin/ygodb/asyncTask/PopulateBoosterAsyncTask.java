package com.chin.ygodb.asyncTask;

import static com.chin.ygowikitool.parser.YugiohWikiUtil.jsoupGet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.chin.ygodb.R;
import com.chin.ygodb.activity.BoosterActivity;
import com.chin.ygodb.dataSource.BoosterStore;
import com.chin.ygowikitool.api.YugipediaApi;
import com.chin.ygowikitool.entity.Booster;
import com.chin.ygowikitool.parser.YugipediaBoosterParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PopulateBoosterAsyncTask extends AsyncTask<String, Void, Void> {
    public static final Executor THREAD_POOL_EXECUTOR;
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 3; // Try not to flood the site with requests
    private static final int KEEP_ALIVE_SECONDS = 3;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    static {
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<>(), sThreadFactory);
    }

    private final RecyclerView recyclerView;
    private final BoosterActivity activity;

    private static Map<String, String> boosterUrls; // a map of booster name to links its articles
    private boolean exceptionOccurred = false;
    private Toast toast = null;
    private final Object toastLock = new Object();
    private final String type; // TCG or OCG

    public PopulateBoosterAsyncTask(RecyclerView recyclerView, BoosterActivity activity) {
        this.recyclerView = recyclerView;
        this.activity = activity;
        this.type = activity.getType();
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            BoosterStore.getInstance(activity).init();

            YugipediaApi api = new YugipediaApi();
            boosterUrls = api.getBoosterMap(type.equals(BoosterActivity.TYPE_TCG));
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

                    String fullImgSrc = booster.getFullImgSrc();
                    if (fullImgSrc == null) {
                        booster.setFullImgSrc("drawable://" + R.drawable.no_image_available);
                    }
                    booster.setUrl(boosterLink);

                    addToBoosterList(boosters, booster);
                    sortBoosterList(boosters);
                    recyclerView.getAdapter().notifyDataSetChanged();
                    continue;
                }

                final Booster booster = new Booster();
                booster.setBoosterName(boosterName);
                booster.setUrl(boosterLink);
                boosterStore.addBooster(boosterName, booster);
                addToBoosterList(boosters, booster);

                // if the booster is not in the store but in shared preference (e.g. after an app restart)
                if (imgLinkMap.containsKey(boosterLink)) {
                    String imgSrc = imgLinkMap.get(boosterLink);

                    booster.setFullImgSrc(imgSrc);
                    String[] tokens = dateMap.get(boosterLink).split("\\|");
                    booster.setEnReleaseDate(tokens[0]);
                    booster.setJpReleaseDate(tokens[1]);

                    booster.parseEnReleaseDate();
                    booster.parseJpReleaseDate();

                    sortBoosterList(boosters);
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
                else {
                    FetchBoosterInfoAsyncTask fetchBoosterInfoAsyncTask = new FetchBoosterInfoAsyncTask(boosterLink, boosterName,
                            scaleWidth, imgSrcPrefEditor, datePrefEditor, booster, boosters);
                    fetchBoosterInfoAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        Collections.sort(boosters, (o1, o2) -> {
            if (order == SortOrder.EN_RELEASE_DATE_DES) {
                return o2.getEnReleaseDateObject().compareTo(o1.getEnReleaseDateObject());
            }
            else if (order == SortOrder.JP_RELEASE_DATE_DES) {
                return o2.getJpReleaseDateObject().compareTo(o1.getJpReleaseDateObject());
            }

            return 0;
        });
    }

    private enum SortOrder {
        EN_RELEASE_DATE_DES,
        JP_RELEASE_DATE_DES,
    }

    private class FetchBoosterInfoAsyncTask extends AsyncTask<String, Void, String[]> {
        private final String boosterLink;
        private final String boosterName;
        private final int scaleWidth;
        private final Editor imgSrcPrefEditor;
        private final Editor datePrefEditor;
        private final Booster booster;
        private final List<Booster> boosters;
        boolean exceptionOccurred;

        public FetchBoosterInfoAsyncTask(String boosterLink, String boosterName, int scaleWidth, Editor imgSrcPrefEditor, Editor datePrefEditor, Booster booster, List<Booster> boosters) {
            this.boosterLink = boosterLink;
            this.boosterName = boosterName;
            this.scaleWidth = scaleWidth;
            this.imgSrcPrefEditor = imgSrcPrefEditor;
            this.datePrefEditor = datePrefEditor;
            this.booster = booster;
            this.boosters = boosters;
            exceptionOccurred = false;
        }

        @Override
        protected String[] doInBackground(String... params) {
            try {
                String html = jsoupGet("https://yugipedia.com/?curid=" + boosterLink);
                Document dom = Jsoup.parse(html);

                YugipediaBoosterParser parser = new YugipediaBoosterParser(boosterName, dom);
                Booster newBooster = parser.parse();

                String imgSrc = newBooster.getFullImgSrc();
                if (imgSrc == null) {
                    // use the placeholder image
                    imgSrc = "drawable://" + R.drawable.no_image_available;
                }

                booster.setFullImgSrc(imgSrc);

                booster.setEnReleaseDate(newBooster.getEnReleaseDate());
                booster.parseEnReleaseDate();

                booster.setJpReleaseDate(newBooster.getJpReleaseDate());
                booster.parseJpReleaseDate();

                booster.setCardMap(newBooster.getCardMap());

                imgSrcPrefEditor.putString(boosterLink, imgSrc);
                imgSrcPrefEditor.commit();

                String toStore = Booster.DEFAULT_DATE_FORMAT.format(booster.getEnReleaseDateObject()) + "|" + Booster.DEFAULT_DATE_FORMAT.format(booster.getJpReleaseDateObject());
                datePrefEditor.putString(boosterLink, toStore);
                datePrefEditor.commit();

                Log.i("foo", "Fetched " + boosterName + " from scratch, saved to cache");
            }
            catch (Exception e) {
                Log.w("ygodb", "Failed to fetch " + boosterName + "'s info, pageid=" + boosterLink);
                e.printStackTrace();
                // set the flag so we can do something about this in onPostExecute()
                exceptionOccurred = true;
            }

            return new String[] {};
        }

        @Override
        protected void onPostExecute(String[] params) {
            if (exceptionOccurred) return;

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
    }
}
