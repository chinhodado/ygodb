package com.chin.ygodb.asyncTask;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;

import com.chin.ygodb.Card;
import com.chin.ygodb.CardRegexFilterArrayAdapter;
import com.chin.ygodb.DatabaseQuerier;
import com.chin.ygodb.activity.BoosterDetailActivity;
import com.chin.ygodb2.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
            String html = Jsoup.connect("http://yugioh.wikia.com" + boosterUrl)
                    .ignoreContentType(true).execute().body();
            Document dom = Jsoup.parse(html);
            Elements rows = dom.getElementsByClass("wikitable").first()
                               .getElementsByTag("tbody").first()
                               .getElementsByTag("tr");
            for (Element row : rows) {
                try {
                    Elements cells = row.getElementsByTag("td");
                    String setNumber = cells.get(0).text();
                    String cardName = cells.get(1).text();
                    String rarity = cells.get(2).text();

                    DatabaseQuerier querier = new DatabaseQuerier(activity);
                    String criteria = "name = '" + cardName + "'";
                    List<Card> res = querier.executeQuery(criteria);

                    if (res.size() > 0) {
                        cards.add(res.get(0));
                    }
                    else {
                        // TODO: card not in offline db, do something
                    }
                }
                catch (Exception e) {
                    // do nothing
                }
            }

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
