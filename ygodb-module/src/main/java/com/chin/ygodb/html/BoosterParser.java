package com.chin.ygodb.html;

import android.content.Context;
import android.database.DatabaseUtils;
import android.util.Log;
import android.util.LruCache;

import com.chin.ygodb.database.DatabaseQuerier;
import com.chin.ygodb.entity.Card;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsing booster dom
 *
 * Created by Chin on 06-Feb-17.
 */
public class BoosterParser {
    private Context context;
    private Element dom;
    private String boosterName;
    private static final LruCache<String, Element> cache = new LruCache<>(5);

    public BoosterParser(Context context, String boosterName, String boosterUrl) throws IOException {
        this.context = context;
        this.boosterName = boosterName;
        this.dom = getDocument(boosterName, boosterUrl);
    }

    /**
     * Use this constructor when you don't want to use the cache (to avoid waiting for the
     * synchronized cache access, or because of memory issue)
     * @param context A context
     * @param boosterName The booster name
     * @param dom The dom of the booster page
     */
    public BoosterParser(Context context, String boosterName, Document dom) {
        this.context = context;
        this.boosterName = boosterName;
        Element elem = dom.getElementById("mw-content-text");
        removeSupTag(elem);
        this.dom = elem;
    }

    private static Element getDocument(String boosterName, String boosterUrl) throws IOException {
        synchronized (cache) {
            Element elem = cache.get(boosterName);
            if (elem != null) {
                return elem;
            }
            else {
                String html = Jsoup.connect("http://yugioh.wikia.com" + boosterUrl)
                        .ignoreContentType(true).execute().body();
                Document dom = Jsoup.parse(html);
                elem = dom.getElementById("mw-content-text");
                removeSupTag(elem);
                cache.put(boosterName, elem);
                return elem;
            }
        }
    }

    public String getJapaneseReleaseDate() {
        try {
            Elements rows = dom.getElementsByClass("infobox").first().getElementsByTag("tr");
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                if (row.text().equals("Release dates")) {
                    for (int j = i + 1; j < rows.size(); j++) {
                        Elements headers = rows.get(j).getElementsByTag("th");
                        if (headers.size() > 0 && headers.get(0).text().equals("Japanese")) {
                            String date = rows.get(j).getElementsByTag("td").first().text();
                            return date;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Log.i("ygodb", "Unable to get Japanese release date");
        }

        return null;
    }

    public String getEnglishReleaseDate() {
        try {
            Elements rows = dom.getElementsByClass("infobox").first().getElementsByTag("tr");
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                if (row.text().equals("Release dates")) {
                    String date = null;
                    for (int j = i + 1; j < rows.size(); j++) {
                        Elements headers = rows.get(j).getElementsByTag("th");
                        if (headers.size() > 0) {
                            Element header = headers.get(0);

                            if (header.text().startsWith("English")) {
                                date = rows.get(j).getElementsByTag("td").first().text();
                            }

                            if (header.text().equals("English (na)")){
                                return date;
                            }
                        }
                    }

                    return date;
                }
            }
        }
        catch (Exception e) {
            Log.i("ygodb", "Unable to get Japanese release date");
        }

        return null;
    }

    public String getImageLink() {
        try {
            return dom.getElementsByClass("image-thumbnail").first().attr("href");
        }
        catch (Exception e) {
            return null;
        }
    }

    public String getIntroText() {
        try {
            return dom.select("#mw-content-text > p").first().text();
        }
        catch (Exception e) {
            return null;
        }
    }

    public String getFeatureText() {
        try {
            return dom.select("#mw-content-text > ul").first().text();
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the list of cards in this booster
     * @return list of cards
     */
    public List<Card> getCardList() {
        List<Card> cards = new ArrayList<>();
        try {
            Elements rows = dom.getElementsByClass("wikitable").first()
                    .getElementsByTag("tbody").first()
                    .getElementsByTag("tr");
            for (Element row : rows) {
                try {
                    Elements cells = row.getElementsByTag("td");
                    String setNumber = cells.get(0).text();
                    String cardName = cells.get(1).text();
                    String rarity = "", category = "";
                    if (cells.size() == 4) {
                        // table without Japanese name column
                        rarity = cells.get(2).text();
                        category = cells.get(3).text();
                    }
                    else if (cells.size() == 5) {
                        // table with Japanese name column
                        // String jpName = cells.get(2).text();
                        rarity = cells.get(3).text();
                        category = cells.get(4).text();
                    }

                    // TODO: can I use the card store here??? Need to review this
                    DatabaseQuerier querier = new DatabaseQuerier(context);
                    String criteria = "name = " + DatabaseUtils.sqlEscapeString(cardName);
                    List<Card> res = querier.executeQuery(criteria);

                    if (res.size() > 0) {
                        Card card = res.get(0);
                        card.setNumber = setNumber;
                        card.rarity = rarity;
                        cards.add(card);
                    }
                    else {
                        Card card = new Card();
                        card.name = cardName;
                        card.setNumber = setNumber;
                        card.rarity = rarity;
                        card.category = category;
                        cards.add(card);
                    }
                }
                catch (Exception e) {
                    // do nothing
                }
            }
        }
        catch (Exception e) {
            Log.i("ygodb", "Unable to get card list for booster: " + boosterName);
        }

        return cards;
    }

    private static void removeSupTag(Element elem) {
        Elements sups = elem.getElementsByTag("sup");
        for (Element e : sups) {
            e.remove();
        }
    }
}
