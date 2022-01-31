package com.chin.ygodb.asyncTask;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;

import com.chin.ygodb.CardRegexFilterArrayAdapter;
import com.chin.ygodb.R;
import com.chin.ygodb.activity.BoosterDetailActivity;
import com.chin.ygodb.dataSource.CardStore;
import com.chin.ygowikitool.entity.Booster;
import com.chin.ygowikitool.entity.Card;
import com.chin.ygowikitool.parser.YugipediaBoosterParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Task for making the booster card list page
 *
 * Created by Chin on 05-Feb-17.
 */
public class BoosterCardListAsyncTask extends AsyncTask<String, Void, List<Card>> {
    private final String boosterName;
    private final String boosterUrl;
    private final ListView cardListView;
    private final BoosterDetailActivity activity;
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
            String html = Jsoup.connect("https://yugipedia.com/?curid=" + boosterUrl)
                    .ignoreContentType(true).execute().body();
            Document dom = Jsoup.parse(html);

            YugipediaBoosterParser parser = new YugipediaBoosterParser(boosterName, dom);
            Booster newBooster = parser.parse();

            Map<String, Card> map = newBooster.getCardMap();
            CardStore cardStore = CardStore.getInstance(activity);
            for (Map.Entry<String, Card> entry : map.entrySet()) {
                String cardName = entry.getKey();
                Card cardNew = entry.getValue();
                if (cardStore.hasCardOffline(cardName)) {
                    // if we have the card in the offline db then we know all its info already
                    Card card = cardStore.getCard(cardName);
                    card.setSetNumber(cardNew.getSetNumber());
                    card.setRarity(cardNew.getRarity());
                    cards.add(card);
                }
                else {
                    // otherwise, the card is either online and not in our db, or does not exist
                    cards.add(cardNew);
                }
            }
        }
        catch (Exception e) {
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
