package com.chin.ygodb.asyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.chin.common.Util;
import com.chin.ygodb.Booster;
import com.chin.ygodb.activity.BoosterActivity;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;

public class PopulateBoosterAsyncTask extends AsyncTask<String, Void, Void> {
    private LinearLayout layout;
    private BoosterActivity activity;
    private static ArrayList<String> boosterUrls; // a list of links to booster articles
    private boolean exceptionOccurred = false;

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
            boosterUrls = new ArrayList<String>();
            for (int i = 0; i < myArray.length(); i++) {
                String boosterLink = myArray.getJSONObject(i).getString("url");
                boosterUrls.add(boosterLink);
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
    protected void onPostExecute(Void param) {

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

            // get the shared preferences as a (booster link, booster name) HashMap
            final String nameMapFileName = "boosterName.txt";
            preferences = activity.getSharedPreferences(nameMapFileName, Context.MODE_PRIVATE);
            final Editor namePrefEditor = preferences.edit();
            @SuppressWarnings("unchecked")
            HashMap<String, String> nameMap = new HashMap<String, String>((Map<String, String>) preferences.getAll());

            // get the shared preferences as a (booster link, booster release date) HashMap
            final String dateMapFileName = "boosterDate.txt";
            preferences = activity.getSharedPreferences(dateMapFileName, Context.MODE_PRIVATE);
            final Editor datePrefEditor = preferences.edit();
            @SuppressWarnings("unchecked")
            HashMap<String, String> dateMap = new HashMap<String, String>((Map<String, String>) preferences.getAll());

            final List<Booster> boosters = new ArrayList<>();

            // loop through the booster list and display them
            for (int i = 0; i < boosterUrls.size(); i++) {
                final Booster booster = new Booster();
                addToBoosterList(boosters, booster);
                String boosterLink = boosterUrls.get(i);
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
                    nameTv.setText(nameMap.get(boosterLink));
                    booster.setName(nameMap.get(boosterLink));

                    // set the booster release date and sort the booster list
                    booster.setReleaseDate(dateMap.get(boosterLink));
                    sortBoosterList(boosters);

                    displayBoosterPage(boosters);
                }
                else {
                    new AsyncTask<String, Void, String[]>(){
                        boolean exceptionOccurred2 = false;
                        @Override
                        protected String[] doInBackground(String... params) {
                            String imgSrc = null, boosterLink = null, boosterName = null;
                            String date = "January 1, 1970"; // default date
                            try {
                                boosterLink = params[0];
                                String html = Jsoup.connect("http://yugioh.wikia.com" + boosterLink)
                                      .ignoreContentType(true).execute().body();
                                Document dom = Jsoup.parse(html);
                                boosterName = dom.getElementById("WikiaPageHeader").getElementsByTag("h1").first().text();
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

                                namePrefEditor.putString(boosterLink, boosterName);
                                namePrefEditor.commit();

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

                            return new String[] {boosterName, imgSrc, date};
                        }

                        @Override
                        protected void onPostExecute(String[] params) {
                            if (exceptionOccurred2) return;
                            // get the scaled image link and display it
                            String newScaledLink = Util.getScaledWikiaImageLink(params[1], scaleWidth);
                            ImageLoader.getInstance().displayImage(newScaledLink, imgView);

                            // add the name to the name row
                            nameTv.setText(params[0]);
                            booster.setName(params[0]);

                            // set the booster release date and sort the booster list
                            booster.setReleaseDate(params[2]);
                            sortBoosterList(boosters);
                            
                            displayBoosterPage(boosters);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, boosterLink);
                }
            }

            // remove the spinner
            ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.progressBar_fragment_general);
            layout.removeView(pgrBar);
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
