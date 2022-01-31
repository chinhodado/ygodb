package com.chin.ygodb.asyncTask;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chin.ygodb.R;
import com.chin.ygodb.activity.BoosterDetailActivity;
import com.chin.ygowikitool.entity.Booster;
import com.chin.ygowikitool.parser.YugipediaBoosterParser;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Async task for booster info
 *
 * Created by Chin on 06-Feb-17.
 */
public class BoosterInfoAsyncTask extends AsyncTask<String, Void, Booster> {
    private final String boosterName;
    private final String boosterUrl;
    private final View view;
    private final BoosterDetailActivity activity;
    private boolean exceptionOccurred = false;

    public BoosterInfoAsyncTask(String boosterName, String boosterUrl, View view, BoosterDetailActivity activity) {
        this.boosterName = boosterName;
        this.boosterUrl = boosterUrl;
        this.view = view;
        this.activity = activity;
    }

    @Override
    protected Booster doInBackground(String... params) {
        try {
            String html = Jsoup.connect("https://yugipedia.com/?curid=" + boosterUrl)
                    .ignoreContentType(true).execute().body();
            Document dom = Jsoup.parse(html);

            YugipediaBoosterParser parser = new YugipediaBoosterParser(boosterName, dom);
            Booster newBooster = parser.parse();

            return newBooster;
        } catch (Exception e) {
            Log.w("ygodb", "Failed to fetch " + boosterName + "'s info");
            e.printStackTrace();
            // set the flag so we can do something about this in onPostExecute()
            exceptionOccurred = true;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Booster booster) {
        if (booster == null) return;
        String jpReleaseDate = booster.getJpReleaseDate();
        if (jpReleaseDate == null) {
            jpReleaseDate = "N/A";
        }
        String enReleaseDate = booster.getEnReleaseDate();
        if (enReleaseDate == null) {
            enReleaseDate = "N/A";
        }
        TextView tvQuickInfo = view.findViewById(R.id.textView_booster_quickinfo);
        tvQuickInfo.setText(boosterName + "\n\n" +
                "Japanese release date:\n" + jpReleaseDate + "\n\n" +
                "English release date:\n" + enReleaseDate);

        TextView tv = view.findViewById(R.id.textView_booster_longinfo);
        String featureText = booster.getFeatureText();
        if (featureText == null) {
            featureText = "";
        }
        tv.setText(booster.getIntroText() + "\n\n" + featureText);

        ImageView imgView = view.findViewById(R.id.imageView_detail_booster);
        ImageLoader.getInstance().displayImage(booster.getFullImgSrc(), imgView);
    }
}
