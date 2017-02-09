package com.chin.ygodb.asyncTask;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;

import com.chin.ygodb.html.BoosterParser;
import com.chin.ygodb.entity.Card;
import com.chin.ygodb.CardRegexFilterArrayAdapter;
import com.chin.ygodb.activity.BoosterDetailActivity;
import com.chin.ygodb2.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Task for making the booster card list page
 *
 * Created by Chin on 05-Feb-17.
 */
public class BoosterCardListAsyncTask extends AsyncTask<String, Void, List<Card>> {
    private String boosterName;
    private String boosterUrl;
    private ListView cardListView;
    private BoosterDetailActivity activity;
    private boolean exceptionOccurred = false;

    public BoosterCardListAsyncTask(String boosterName, String boosterUrl, ListView cardListView, BoosterDetailActivity activity) {
        this.boosterName = boosterName;
        this.boosterUrl = boosterUrl;
        this.cardListView = cardListView;
        this.activity = activity;
    }

    @Override
    protected List<Card> doInBackground(String... params) {
        List<Card> cards = new ArrayList<>();
        try {
            BoosterParser parser = new BoosterParser(activity, boosterName, boosterUrl);
            return parser.getCardList();
        } catch (Exception e) {
            Log.w("ygodb", "Failed to fetch " + boosterName + "'s card list");
            e.printStackTrace();
            // set the flag so we can do something about this in onPostExecute()
            exceptionOccurred = true;
        }

        return cards;
    }

    @Override
    protected void onPostExecute(List<Card> resultSet) {
        cardListView.setAdapter(new CardRegexFilterArrayAdapter(activity, R.layout.list_item_card, R.id.itemRowText, resultSet));
    }
}
