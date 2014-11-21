package com.chin.ygodb.asyncTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.chin.common.Util;
import com.chin.ygodb.activity.BoosterActivity;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;

public class PopulateBoosterAsyncTask extends AsyncTask<String, Void, Void> {
    LinearLayout layout;
    BoosterActivity activity;
    static ArrayList<String> boosterList; // a list of links to booster articles
    boolean exceptionOccurred = false;

    public PopulateBoosterAsyncTask(LinearLayout layout, BoosterActivity activity) {
        this.layout = layout;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(String... params) {
        if (boosterList != null) return null;
        try {
            String baseUrl = "http://yugioh.wikia.com/api/v1/Articles/List?category=TCG_Booster_Packs&limit=5000&namespaces=0";
            String html = Jsoup.connect(baseUrl).ignoreContentType(true).execute().body();

            JSONObject myJSON = new JSONObject(html);
            JSONArray myArray = myJSON.getJSONArray("items");
            boosterList = new ArrayList<String>();
            for (int i = 0; i < myArray.length(); i++) {
                String boosterLink = myArray.getJSONObject(i).getString("url");
                boosterList.add(boosterLink);
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

            LinearLayout tmpLayout = null;
            LinearLayout tmpLayoutName = null;

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

            final int BOOSTER_PER_ROW = 4;
            // loop through the booster list and display them
            for (int i = 0; i < boosterList.size(); i++) {
                String boosterLink = boosterList.get(i);
                String imgSrc;

                // create a new image view and add it
                final ImageView imgView = new ImageView(activity);

                if (i % BOOSTER_PER_ROW == 0) {
                    // add a new LinearLayout for a new image row
                    tmpLayout = new LinearLayout(activity);
                    tmpLayout.setLayoutParams(new LinearLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    tmpLayout.setGravity(Gravity.CENTER);
                    layout.addView(tmpLayout);

                    // and a new LinearLayout for a name row
                    tmpLayoutName = new LinearLayout(activity);
                    tmpLayoutName.setLayoutParams(new LinearLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    tmpLayoutName.setGravity(Gravity.CENTER);
                    layout.addView(tmpLayoutName);
                }
                tmpLayout.addView(imgView);

                // set the image view's dimensions
                imgView.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                // add the textview for name to the namerow
                final TextView tmpNameTv = new TextView(activity);
                tmpNameTv.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                tmpNameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                tmpNameTv.setGravity(Gravity.CENTER);
                tmpLayoutName.addView(tmpNameTv);

                if (imgLinkMap.containsKey(boosterLink)) {
                    imgSrc = imgLinkMap.get(boosterLink);
                    Log.i("foo", "Fetched " + boosterLink + " from cache");

                    // get the scaled image link and display it
                    String newScaledLink = Util.getScaledWikiaImageLink(imgSrc, scaleWidth);
                    ImageLoader.getInstance().displayImage(newScaledLink, imgView);

                    // change the name textview
                    tmpNameTv.setText(nameMap.get(boosterLink));
                }
                else {
                    new AsyncTask<String, Void, String[]>(){
                        boolean exceptionOccurred2 = false;
                        @Override
                        protected String[] doInBackground(String... params) {
                            String imgSrc = null, boosterLink = null, boosterName = null;
                            try {
                                boosterLink = params[0];
                                String html = Jsoup.connect("http://yugioh.wikia.com" + boosterLink)
                                      .ignoreContentType(true).execute().body();
                                Document dom = Jsoup.parse(html);
                                boosterName = dom.getElementById("WikiaPageHeader").getElementsByTag("h1").first().text();
                                imgSrc = dom.getElementsByClass("image-thumbnail").first().attr("href");

                                namePrefEditor.putString(boosterLink, boosterName);
                                namePrefEditor.commit();

                                imgSrcPrefEditor.putString(boosterLink, imgSrc);
                                imgSrcPrefEditor.commit();
                                Log.i("foo", "Fetched " + boosterLink + " from scratch, saved to cache");
                            } catch (Exception e) {
                                Log.w("YGODB", "Failed to fetch " + boosterLink + "'s img link");
                                e.printStackTrace();
                                // set the flag so we can do something about this in onPostExecute()
                                exceptionOccurred2 = true;
                            }

                            return new String[] {boosterName, imgSrc};
                        }

                        @Override
                        protected void onPostExecute(String[] params) {
                            if (exceptionOccurred2) return;
                            // get the scaled image link and display it
                            String newScaledLink = Util.getScaledWikiaImageLink(params[1], scaleWidth);
                            ImageLoader.getInstance().displayImage(newScaledLink, imgView);

                            // add the name to the name row
                            tmpNameTv.setText(params[0]);
                        }
                    }.execute(boosterLink);
                }
            }

            // remove the spinner
            ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.progressBar_fragment_general);
            layout.removeView(pgrBar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
