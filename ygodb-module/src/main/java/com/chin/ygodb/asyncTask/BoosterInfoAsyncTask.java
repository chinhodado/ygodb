package com.chin.ygodb.asyncTask;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chin.ygodb.html.BoosterParser;
import com.chin.ygodb.activity.BoosterDetailActivity;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * Async task for booster info
 *
 * Created by Chin on 06-Feb-17.
 */
public class BoosterInfoAsyncTask extends AsyncTask<String, Void, BoosterParser> {
    private String boosterName;
    private String boosterUrl;
    private View view;
    private BoosterDetailActivity activity;
    private boolean exceptionOccurred = false;

    public BoosterInfoAsyncTask(String boosterName, String boosterUrl, View view, BoosterDetailActivity activity) {
        this.boosterName = boosterName;
        this.boosterUrl = boosterUrl;
        this.view = view;
        this.activity = activity;
    }

    @Override
    protected BoosterParser doInBackground(String... params) {
        try {
            return new BoosterParser(boosterName, boosterUrl);
        } catch (Exception e) {
            Log.w("ygodb", "Failed to fetch " + boosterName + "'s info");
            e.printStackTrace();
            // set the flag so we can do something about this in onPostExecute()
            exceptionOccurred = true;
        }
        return null;
    }

    @Override
    protected void onPostExecute(BoosterParser parser) {
        if (parser == null) return;
        String jpReleaseDate = parser.getJapaneseReleaseDate();
        if (jpReleaseDate == null) {
            jpReleaseDate = "N/A";
        }
        String enReleaseDate = parser.getEnglishReleaseDate();
        if (enReleaseDate == null) {
            enReleaseDate = "N/A";
        }
        TextView tvQuickInfo = (TextView) view.findViewById(R.id.textView_booster_quickinfo);
        tvQuickInfo.setText(boosterName + "\n\n" +
                "Japanese release date:\n" + jpReleaseDate + "\n\n" +
                "English release date:\n" + enReleaseDate);

        TextView tv = (TextView) view.findViewById(R.id.textView_booster_longinfo);
        String featureText = parser.getFeatureText();
        if (featureText == null) {
            featureText = "";
        }
        tv.setText(parser.getIntroText() + "\n\n" + featureText);

        ImageView imgView = (ImageView) view.findViewById(R.id.imageView_detail_booster);
        ImageLoader.getInstance().displayImage(parser.getImageLink(), imgView);
    }
}
