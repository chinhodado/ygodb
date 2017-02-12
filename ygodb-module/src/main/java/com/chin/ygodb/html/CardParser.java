package com.chin.ygodb.html;

import android.util.Log;
import android.util.LruCache;

import com.chin.common.HtmlUtil;
import com.chin.ygodb.dataSource.CardStore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsing card dom
 *
 * Created by Chin on 12-Feb-17.
 */
public class CardParser {
    private Element dom;
    private String cardName;
    private static final LruCache<String, Element> cache = new LruCache<>(10);

    public CardParser(String cardName, String cardUrl) throws IOException {
        this.cardName = cardName;
        this.dom = getDocument(cardName, cardUrl);
    }

    /**
     * Use this constructor when you don't want to use the cache (to avoid waiting for the
     * synchronized cache access, or because of memory issue)
     * @param cardName The booster name
     * @param dom The dom of the booster page
     */
    public CardParser(String cardName, Document dom) {
        this.cardName = cardName;
        Element elem = dom.getElementById("mw-content-text");
        HtmlUtil.removeSupTag(elem);
        this.dom = elem;
    }

    private static Element getDocument(String cardName, String cardUrl) throws IOException {
        synchronized (cache) {
            Element elem = cache.get(cardName);
            if (elem != null) {
                return elem;
            }
            else {
                String html = Jsoup.connect(cardUrl)
                        .ignoreContentType(true).execute().body();
                Document dom = Jsoup.parse(html);
                elem = dom.getElementById("mw-content-text");
                HtmlUtil.removeSupTag(elem);
                cache.put(cardName, elem);
                return elem;
            }
        }
    }

    public List<CardStore.Pair> getCardStatus() throws Exception {
        List<CardStore.Pair> statuses = new ArrayList<>();

        Elements tableRows = dom.getElementsByClass("cardtable").first().getElementsByClass("cardtablerow");
        boolean foundStatusRow = false;
        for (int i = 0; i < tableRows.size(); i++) {
            Element row = tableRows.get(i);
            if (row.getElementsByClass("cardtablespanrow").size() > 0) {
                // we reached the end of the status rows (past the banlist status rows and
                // now is the Card descriptions row with nested table)
                break;
            }

            Element rowHeader = row.getElementsByClass("cardtablerowheader").first();
            if (rowHeader != null && rowHeader.text().equals("Statuses")) {
                foundStatusRow = true;
            }

            if (foundStatusRow) {
                String rowData = row.getElementsByClass("cardtablerowdata").first().text();
                String[] tokens = rowData.split(" \\(");
                if (tokens.length == 1) {
                    statuses.add(new CardStore.Pair("All formats", tokens[0]));
                }
                else {
                    String status = tokens[0];
                    String format = tokens[1].replace("(", "").replace(")", "");
                    statuses.add(new CardStore.Pair(format, status));
                }
            }
        }

        if (statuses.isEmpty()) {
            Log.i("ygodb", "Card banlist status not found online");
        }

        return statuses;
    }

    public List<CardStore.Pair> getCardInfo() throws Exception {
        List<CardStore.Pair> infos = new ArrayList<>();
        Elements rows = dom.getElementsByClass("cardtable").first().getElementsByClass("cardtablerow");

        // first row is "Attribute" for monster, "Type" for spell/trap and "Types" for token
        boolean foundFirstRow = false;
        for (Element row : rows) {
            Element header = row.getElementsByClass("cardtablerowheader").first();
            if (header == null) continue;
            String headerText = header.text();
            if (!foundFirstRow && !headerText.equals("Attribute") && !headerText.equals("Type") && !headerText.equals("Types")) {
                continue;
            }
            if (headerText.equals("Other card information") || header.text().equals("External links")) {
                // we have reached the end for some reasons, exit now
                break;
            }
            else {
                foundFirstRow = true;
                String data = row.getElementsByClass("cardtablerowdata").first().text();
                infos.add(new CardStore.Pair(headerText, data));
                if (headerText.equals("Card effect types") || headerText.equals("Limitation Text")) {
                    break;
                }
            }
        }

        return infos;
    }

    public String getCardLore() throws Exception {
        Element effectBox = dom.getElementsByClass("cardtablespanrow").first().getElementsByClass("navbox-list").first();
        String lore = YgoWikiaHtmlCleaner.getCleanedHtml(effectBox, null);

        // turn <dl> into <p> and <dt> into <b>
        lore = lore.replace("<dl", "<p").replace("dl>", "p>").replace("<dt", "<b").replace("dt>", "b>");
        return lore;
    }

    public String getImageLink() throws Exception {
        Element td = dom.getElementsByClass("cardtable-cardimage").first();
        String imageUrl = td.getElementsByTag("a").first().attr("href");

        // Strip out the unnecessary stuffs in the image url
        if (imageUrl.contains("/revision/")) {
            imageUrl = imageUrl.substring(0, imageUrl.indexOf("/revision/"));
        }

        return imageUrl;
    }
}
