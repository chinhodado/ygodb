package com.chin.ygodb;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.chin.ygodb.CardStore;
import android.content.Context;
import android.util.Log;

/**
 * A singleton class that acts as a storage for card information. Support lazy loading information.
 *
 * For now:
 * - the cardDOM is saved, so if you want the details from it you need to parse it manually
 *
 * @author Chin
 *
 */
public final class CardStore {

    public static class CardDetail {
        String name = null;
        Document cardDOM;

        CardDetail(String name) {
            this.name = name;
        }
    }

    // a list of all cards available, initialized in MainActivity's onCreate()
    public static ArrayList<String> cardList = null;

    // map a card name to its wiki page url, initialized in MainActivity's onCreate()
    public static Hashtable<String, String[]> cardLinkTable = null;

    // the heart of this class, a storage for cards' detail
    private static Hashtable<String, CardDetail> cardStore = new Hashtable<String, CardDetail>();

    private static CardStore CARDSTORE;
    private static Context context;

    /**
     * Private constructor. For singleton.
     */
    private CardStore(Context context) {
        if (CARDSTORE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        CardStore.context = context;
    }

    /**
     * Get the only instance of this class. Because of singleton.
     * @return The only instance of this class.
     */
    public static CardStore getInstance(Context context) {
        if (CARDSTORE == null) {
            CARDSTORE = new CardStore(context);
        }
        return CARDSTORE;
    }

    public void initializeCardList() throws InterruptedException, ExecutionException, JSONException {
        if (cardList != null) return;
        CardStore.cardList = new ArrayList<String>(8192);
        CardStore.cardLinkTable = new Hashtable<String, String[]>(8192);
        initializeCardList(null, true);
        initializeCardList(null, false);
        Log.i("foo", cardList.size() + "");
    }

    private void initializeCardList(String offset, boolean isTcg) throws InterruptedException, ExecutionException, JSONException {
        // this will return up to 5000 articles in the TCG_cards/OCG_cards category. Note that this is not always up-to-date,
        // as newly added articles may take a day or two before showing up in here
        String url;
        if (isTcg) {
            url = "http://yugioh.wikia.com/api/v1/Articles/List?category=TCG_cards&limit=5000&namespaces=0";
        }
        else {
            url = "http://yugioh.wikia.com/api/v1/Articles/List?category=OCG_cards&limit=5000&namespaces=0";
        }

        if (offset != null) {
            url = url + "&offset=" + offset;
        }
        String jsonString = new NetworkTask().execute(url).get();

        JSONObject myJSON = new JSONObject(jsonString);
        JSONArray myArray = myJSON.getJSONArray("items");
        for (int i = 0; i < myArray.length(); i++) {
            String cardName = myArray.getJSONObject(i).getString("title");
            if (!CardStore.cardLinkTable.containsKey(cardName)) {
                CardStore.cardList.add(cardName);
                String[] tmp = {myArray.getJSONObject(i).getString("url"),
                                myArray.getJSONObject(i).getString("id")};
                CardStore.cardLinkTable.put(cardName, tmp);
            }
        }

        if (myJSON.has("offset")) {
            initializeCardList((String) myJSON.get("offset"), isTcg);
        }
    }

    public void getGeneralInfo(String cardName) {

        CardDetail currentCard = cardStore.get(cardName);
        if (currentCard != null) {
            return; // already initialized, just return
        }

        currentCard = new CardDetail(cardName);
        cardStore.put(cardName, currentCard);

        String cardURL = "http://yugioh.wikia.com" + CardStore.cardLinkTable.get(cardName)[0];

        String cardHTML = null;
        try {
            cardHTML = Jsoup.connect(cardURL).ignoreContentType(true).execute().body();
        } catch (Exception e) {
            Log.e("CardDetail", "Error fetching the card HTML page");
            e.printStackTrace();
        }
        Document cardDOM = Jsoup.parse(cardHTML);

        // save the DOM for later use. Should look into this so that it doesn't cause huge mem usage
        currentCard.cardDOM = cardDOM;
    }

    public void getGeneralInfoReady(String cardName) {
        CardDetail currentCard = cardStore.get(cardName);
        if (currentCard == null) {
            currentCard = new CardDetail(cardName);
            cardStore.put(cardName, currentCard);
        }

        // if the cardDOM is not available yet, call getGeneralInfo(). Or maybe just fetch it directly?
        if (currentCard.cardDOM == null) getGeneralInfo(cardName);
    }

    public String getImageLink(String cardName) {
        getGeneralInfoReady(cardName);
        CardDetail currentCard = cardStore.get(cardName);

        Element td = currentCard.cardDOM.getElementsByClass("cardtable-cardimage").first();

        String imageUrl = td.getElementsByTag("a").first().attr("href");
        return imageUrl;
    }

    public String getCardEffect(String cardName) {
        getGeneralInfoReady(cardName);
        CardDetail currentCard = cardStore.get(cardName);

        Element effectBox = currentCard.cardDOM.getElementsByClass("cardtablespanrow").first().getElementsByClass("navbox-list").first();
        String effect = effectBox.html();

        // hackish, turn <a> into <span> so we don't see blue underlined text
        effect = effect.replace("<a", "<span").replace("a>", "span>");

        // turn <dl> into <p> and <dt> into <b>
        effect = effect.replace("<dl", "<p").replace("dl>", "p>").replace("<dt", "<b").replace("dt>", "b>");
        return effect;
    }

    public Document getCardDom(String cardName) {
        getGeneralInfoReady(cardName);
        CardDetail currentCard = cardStore.get(cardName);
        return currentCard.cardDOM;
    }
}

