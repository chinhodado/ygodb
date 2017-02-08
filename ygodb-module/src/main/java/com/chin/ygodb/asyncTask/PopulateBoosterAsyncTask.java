package com.chin.ygodb.asyncTask;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.chin.common.Util;
import com.chin.ygodb.Booster;
import com.chin.ygodb.activity.BoosterActivity;
import com.chin.ygodb.activity.BoosterDetailActivity;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopulateBoosterAsyncTask extends AsyncTask<String, Void, Void> {
    private LinearLayout layout;
    private BoosterActivity activity;
    private static Map<String, String> boosterUrls; // a map of booster name to links its articles
    private boolean exceptionOccurred = false;
    private Toast toast = null;
    private final Object toastLock = new Object();

    public PopulateBoosterAsyncTask(LinearLayout layout, BoosterActivity activity) {
        this.layout = layout;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(String... params) {
        if (boosterUrls != null) return null;
        try {
            String baseUrl = "http://yugioh.wikia.com/api/v1/Articles/List?category=TCG_Booster_Packs&limit=5000&namespaces=0";
            String html = Jsoup.connect(baseUrl).ignoreContentType(true).execute().body();

            JSONObject myJSON = new JSONObject(html);
            JSONArray myArray = myJSON.getJSONArray("items");
            boosterUrls = new HashMap<>();
            for (int i = 0; i < myArray.length(); i++) {
                String boosterName = myArray.getJSONObject(i).getString("title");
                String boosterLink = myArray.getJSONObject(i).getString("url");
                boosterUrls.put(boosterName, boosterLink);
            }

            if (isCancelled()) {
                return null;
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
            // remove the spinner
            ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.progressBar_fragment_general);
            layout.removeView(pgrBar);
            TextView tv = new TextView(activity);
            layout.addView(tv);
            tv.setText("Something went wrong. Please restart the app and try again.");
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
            HashMap<String, String> imgLinkMap = new HashMap<String, String>((Map<String, String>) preferences.getAll());

            // get the shared preferences as a (booster link, booster release date) HashMap
            final String dateMapFileName = "boosterDate.txt";
            preferences = activity.getSharedPreferences(dateMapFileName, Context.MODE_PRIVATE);
            final Editor datePrefEditor = preferences.edit();
            @SuppressWarnings("unchecked")
            HashMap<String, String> dateMap = new HashMap<String, String>((Map<String, String>) preferences.getAll());

            final List<Booster> boosters = new ArrayList<>();

            // loop through the booster list and display them
            for (Map.Entry<String, String> entry : boosterUrls.entrySet()) {
                final Booster booster = new Booster();
                addToBoosterList(boosters, booster);
                final String boosterName = entry.getKey();
                final String boosterLink = entry.getValue();
                String imgSrc;

                // create a new image view and set its dimensions
                final ImageView imgView = new ImageView(activity);
                imgView.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                booster.setImgView(imgView);

                // create a text view for the name
                final TextView nameTv = new TextView(activity);
                nameTv.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                nameTv.setGravity(Gravity.CENTER);
                booster.setTxtView(nameTv);

                if (imgLinkMap.containsKey(boosterLink)) {
                    imgSrc = imgLinkMap.get(boosterLink);
                    Log.i("foo", "Fetched " + boosterLink + " from cache");

                    // get the scaled image link and display it
                    String newScaledLink = Util.getScaledWikiaImageLink(imgSrc, scaleWidth);
                    ImageLoader.getInstance().displayImage(newScaledLink, imgView);

                    // change the name textview
                    nameTv.setText(boosterName);
                    booster.setName(boosterName);

                    // set the booster release date and sort the booster list
                    booster.setReleaseDate(dateMap.get(boosterLink));
                    sortBoosterList(boosters);

                    // set listener for imgView
                    setImgViewListener(imgView, boosterName, boosterLink);

                    displayBoosterPage(boosters);
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
                                imgSrc = dom.getElementsByClass("image-thumbnail").first().attr("href");

                                try {
                                    Element infobox = dom.getElementsByClass("infobox").first();
                                    Elements rows = infobox.getElementsByTag("tr");

                                    for (int i = 0; i < rows.size(); i++) {
                                        Element row = rows.get(i);
                                        if (row.text().equals("Release dates")) {
                                            // right now we're only getting the first date, which can be JP, US, etc.
                                            // maybe try to parse and get the US (or JP) date if possible?
                                            date = rows.get(i+1).getElementsByTag("td").first().text();
                                            break;
                                        }
                                    }
                                }
                                catch (Exception e) {
                                    Log.i("ygodb", "Failed to get release date for: " + boosterName);
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
                            // get the scaled image link and display it
                            String newScaledLink = Util.getScaledWikiaImageLink(params[0], scaleWidth);
                            ImageLoader.getInstance().displayImage(newScaledLink, imgView);

                            // add the name to the name row
                            nameTv.setText(boosterName);
                            booster.setName(boosterName);

                            // set the booster release date and sort the booster list
                            booster.setReleaseDate(params[1]);
                            sortBoosterList(boosters);

                            // set listener for imgView
                            setImgViewListener(imgView, boosterName, boosterLink);

                            displayBoosterPage(boosters);

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

            // remove the spinner
            ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.progressBar_fragment_general);
            layout.removeView(pgrBar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setImgViewListener(ImageView imgView, final String boosterName, final String boosterUrl) {
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), BoosterDetailActivity.class);
                intent.putExtra(BoosterActivity.BOOSTER_NAME, boosterName);
                intent.putExtra(BoosterActivity.BOOSTER_URL, boosterUrl);
                activity.startActivity(intent);
            }
        });
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

    private void displayBoosterPage(List<Booster> boosters) {
        LinearLayout tmpLayout = null;
        LinearLayout tmpLayoutName = null;

        final int BOOSTER_PER_ROW = 4;

        // remove all existing rows of boosters and names
        layout.removeAllViews();

        for (int i = 0; i < boosters.size(); i++) {
            Booster booster = boosters.get(i);
            ImageView imgView = booster.getImgView();
            TextView txtView = booster.getTxtView();

            if (i % BOOSTER_PER_ROW == 0) {
                // add a new LinearLayout for a new image row
                tmpLayout = new LinearLayout(activity);
                tmpLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                tmpLayout.setGravity(Gravity.CENTER);
                layout.addView(tmpLayout);

                // and a new LinearLayout for a name row
                tmpLayoutName = new LinearLayout(activity);
                tmpLayoutName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                tmpLayoutName.setGravity(Gravity.CENTER);
                layout.addView(tmpLayoutName);
            }

            // remove the existing parent of the img view
            ViewParent parent = imgView.getParent();
            if (parent != null) {
                ((ViewGroup)parent).removeAllViews();
            }

            // remove the existing parent of the txt view
            parent = txtView.getParent();
            if (parent != null) {
                ((ViewGroup)parent).removeAllViews();
            }

            // add the imgView and txtView to their respective row
            tmpLayout.addView(imgView);
            tmpLayoutName.addView(txtView);
        }
    }
}
