package com.chin.ygodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.chin.common.Util;
import com.chin.ygodb.activity.MainActivity;
import com.chin.ygodb.database.DatabaseQuerier;
import com.chin.ygodb.entity.Card;
import com.chin.ygodb.html.YgoWikiaHtmlCleaner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;

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
    public static class Pair {
        public String key;
        public String value;
        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public enum CardAdditionalInfoType{
        Ruling, Tips, Trivia
    }

    private static Map<String, String> columnNameMap = new Hashtable<>();
    static {
        // not all columns are in here, just those in the info section
        columnNameMap.put("attribute"      , "Attribute");
        columnNameMap.put("types"          , "Types");
        columnNameMap.put("level"          , "Level");
        columnNameMap.put("atk"            , "ATK");
        columnNameMap.put("def"            , "DEF");
        columnNameMap.put("cardnum"        , "Card Number");
        columnNameMap.put("passcode"       , "Passcode");
        columnNameMap.put("effectTypes"    , "Card effect types");
        columnNameMap.put("materials"      , "Materials");
        columnNameMap.put("fusionMaterials", "Fusion Material");
        columnNameMap.put("rank"           , "Rank");
        columnNameMap.put("ritualSpell"    , "Ritual Spell Card required");
        columnNameMap.put("pendulumScale"  , "Pendulum Scale");
        columnNameMap.put("property"       , "Property");
        columnNameMap.put("summonedBy"     , "Summoned by the effect of");
        columnNameMap.put("limitText"      , "Limitation Text");
        columnNameMap.put("synchroMaterial", "Synchro Material");
        columnNameMap.put("ritualMonster"  , "Ritual Monster required");
        columnNameMap.put("ocgStatus"      , "OCG");
        columnNameMap.put("tcgAdvStatus"   , "TCG Advanced");
        columnNameMap.put("tcgTrnStatus"   , "TCG Traditional");
    }

    // a list of all cards available, initialized in MainActivity's onCreate()
    public static List<String> cardNameList = null;

    // map a card name to its wiki page url, initialized in MainActivity's onCreate()
    private static Map<String, String[]> cardLinkTable = null;

    // a storage for cards' detail after being fetched online
    private static Map<String, Document> cardDomCache = new Hashtable<>();

    // list of Card objects, used for displaying in the ListView (the backing list for the adapter)
    public static List<Card> cardList = new ArrayList<>(8192);

    // basically a hashtable of cardList that maps a card's name to its Card object
    private static Map<String, Card> cardSet = new Hashtable<>(8192);

    // list of cards in the offline database
    private static List<String> offlineCardList = new ArrayList<>(8192);

    // flag: initialized the cardList, but not the cardLinkTable
    private static boolean initializedOffline = false;

    private static boolean initializedOnline = false;

    // we use the application context so don't worry about this
    @SuppressLint("StaticFieldLeak")
    private static volatile CardStore INSTANCE;

    private Context context;

    /**
     * Private constructor. For singleton.
     */
    private CardStore(Context context) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        this.context = context;
    }

    /**
     * Get the only instance of this class. Because of singleton.
     * @return The only instance of this class.
     */
    public static CardStore getInstance(Context context) {
        // double-checked locking
        CardStore result = INSTANCE;
        if (result == null) {
            synchronized(CardStore.class) {
                result = INSTANCE;
                if (result == null) {
                    INSTANCE = result = new CardStore(context.getApplicationContext());
                }
            }
        }

        return result;
    }

    public void initializeCardList() throws Exception {
        if (initializedOnline) return;
        if (Util.hasNetworkConnectivity(context)) {
            Log.i("ygodb", "Initializing online...");
            cardNameList = new ArrayList<String>(8192);
            cardLinkTable = new Hashtable<String, String[]>(8192);
            initializeCardListOnline(null, true);
            initializeCardListOnline(null, false);

            if (!initializedOffline) {
                addOfflineCardsToCardList(false);
            }

            // add those that are online but not offline
            List<String> onlineOfflineDiff = new ArrayList<String>(CardStore.cardNameList);
            onlineOfflineDiff.removeAll(offlineCardList);
            Log.i("ygodb", "Diff between online and offline: " + onlineOfflineDiff.size());
            for (int i = 0; i < onlineOfflineDiff.size(); i++) {
                Card card = new Card();
                card.name = onlineOfflineDiff.get(i);
                CardStore.cardList.add(card);
                CardStore.cardSet.put(card.name, card);
            }

            initializedOnline = true;
            Log.i("ygodb", "Done initializing online.");
            Log.i("ygodb", "Number of cards: " + cardNameList.size());
        }
        else if (!initializedOffline) {
            Log.i("ygodb", "Initializing offline...");
            CardStore.cardNameList = new ArrayList<String>(8192);
            initializeCardListOffline();
            initializedOffline = true;
            Log.i("ygodb", "Done initializing offline.");
            Log.i("ygodb", "Number of cards: " + cardNameList.size());
        }
    }

    private void addOfflineCardsToCardList(boolean isOffline) {
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select name, attribute, types, level, atk, def, rank, pendulumScale, property "
                                  + "from card order by name", null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                Card card = new Card();
                card.name = name;
                // note that we don't need all card info here - just those needed for displaying in the ListView
                card.attribute        = cursor.getString(cursor.getColumnIndex("attribute"));
                card.types            = cursor.getString(cursor.getColumnIndex("types"));
                card.level            = cursor.getString(cursor.getColumnIndex("level"));
                card.atk              = cursor.getString(cursor.getColumnIndex("atk"));
                card.def              = cursor.getString(cursor.getColumnIndex("def"));
                card.rank             = cursor.getString(cursor.getColumnIndex("rank"));
                card.pendulumScale    = cursor.getString(cursor.getColumnIndex("pendulumScale"));
                card.property         = cursor.getString(cursor.getColumnIndex("property"));

                CardStore.cardList.add(card);
                CardStore.cardSet.put(name, card);
                if (isOffline) {
                    cardNameList.add(name);
                }
                offlineCardList.add(name);
                cursor.moveToNext();
            }
        }

        cursor.close();
        Collections.sort(cardNameList);
    }

    private void initializeCardListOffline() {
        addOfflineCardsToCardList(true);
    }

    private void initializeCardListOnline(String offset, boolean isTcg) throws Exception {
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
        String jsonString = Jsoup.connect(url).ignoreContentType(true).execute().body();

        JSONObject myJSON = new JSONObject(jsonString);
        JSONArray myArray = myJSON.getJSONArray("items");
        for (int i = 0; i < myArray.length(); i++) {
            String cardName = myArray.getJSONObject(i).getString("title");
            if (!cardLinkTable.containsKey(cardName)) {
                cardNameList.add(cardName);
                String[] tmp = {myArray.getJSONObject(i).getString("url"),
                                myArray.getJSONObject(i).getString("id")};
                cardLinkTable.put(cardName, tmp);
            }
        }

        if (myJSON.has("offset")) {
            initializeCardListOnline((String) myJSON.get("offset"), isTcg);
        }
    }

    public void getCardDomReady(String cardName) throws Exception {
        initializeCardList();
        if (!Util.hasNetworkConnectivity(context)) {
            return; // what else can we do? switch to offline db, meh
        }
        if (cardDomCache.containsKey(cardName)) {
            return; // already cached, just return
        }

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
        cardDomCache.put(cardName, cardDOM);
    }

    public Document getCardDom(String cardName) throws Exception {
        initializeCardList();
        getCardDomReady(cardName);
        return cardDomCache.get(cardName);
    }

    public boolean hasCard(String cardName) {
        return cardSet.containsKey(cardName);
    }

    public String getImageLink(String cardName) {
        // try get from cache
        Card card = CardStore.cardSet.get(cardName);
        if (!card.thumbnailImgUrl.equals("")) {
            return card.thumbnailImgUrl;
        }

        String link = getImageLinkOffline(cardName);
        if (link != null) {
            card.thumbnailImgUrl = link;
            return link;
        }

        return null;
    }

    public String getImageLinkOnline(String cardName) throws Exception {
        getCardDomReady(cardName);
        Document dom = cardDomCache.get(cardName);

        Element td = dom.getElementsByClass("cardtable-cardimage").first();

        String imageUrl = td.getElementsByTag("a").first().attr("href");

        // Strip out the unnecessary stuffs in the image url
        if (imageUrl.contains("/revision/")) {
            imageUrl = imageUrl.substring(0, imageUrl.indexOf("/revision/"));
        }

        Card card = CardStore.cardSet.get(cardName);
        if (card.thumbnailImgUrl.equals("")) {
            card.thumbnailImgUrl = imageUrl;
        }

        return imageUrl;
    }

    public String getImageLinkOffline(String cardName) {
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select img from card where name = ?", new String[] {cardName});

        if (cursor.getCount() == 0) {
            return null;
        }

        cursor.moveToFirst();
        String img = cursor.getString(cursor.getColumnIndex("img"));
        if (img.equals("")) {
            return null;
        }

        cursor.close();

        try {
            String originalLink = "http://vignette" + img.charAt(0) + ".wikia.nocookie.net/yugioh/images/" + img.charAt(1)
                + "/" + img.charAt(1) + img.charAt(2) + "/" + img.substring(3);

            // calculate the width of the images to be displayed
            Display display = MainActivity.instance.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int screenWidth = size.x;
            int scaleWidth = (int) (screenWidth * 0.2);

            String finalLink = Util.getScaledWikiaImageLink(originalLink, scaleWidth);
            return finalLink;
        }
        catch (Exception e) {
            Log.w("ygodb", "Error parsing image link from offline database: " + cardName);
            return null;
        }
    }


    //////////////////////////////////////////////////////////////////////
    // CARD LORE
    //////////////////////////////////////////////////////////////////////

    public String getCardLore(String cardName) throws Exception {
        if (Util.hasNetworkConnectivity(context)) {
            return getCardLoreOnline(cardName);
        }
        else {
            return getCardLoreOffline(cardName);
        }
    }

    private String getCardLoreOffline(String cardName) {
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select lore from card where name = ?", new String[] {cardName});

        // assuming we always have 1 result...
        cursor.moveToFirst();

        String lore = cursor.getString(cursor.getColumnIndex("lore"));
        cursor.close();
        return lore;
    }

    private String getCardLoreOnline(String cardName) throws Exception {
        Card card = CardStore.cardSet.get(cardName);
        if (!card.lore.equals("")) {
            return card.lore;
        }

        initializeCardList();
        getCardDomReady(cardName);
        Document dom = cardDomCache.get(cardName);

        Element effectBox = dom.getElementsByClass("cardtablespanrow").first().getElementsByClass("navbox-list").first();
        String lore = YgoWikiaHtmlCleaner.getCleanedHtml(effectBox, null);

        // turn <dl> into <p> and <dt> into <b>
        lore = lore.replace("<dl", "<p").replace("dl>", "p>").replace("<dt", "<b").replace("dt>", "b>");
        card.lore = lore;
        return lore;
    }

    //////////////////////////////////////////////////////////////////////
    // CARD INFO
    //////////////////////////////////////////////////////////////////////

    public List<Pair> getCardInfo(String cardName) throws Exception {
        if (Util.hasNetworkConnectivity(context)) {
            return getCardInfoOnline(cardName);
        }
        else {
            return getCardInfoOffline(cardName);
        }
    }

    private List<Pair> getCardInfoOffline(String cardName) {
        List<Pair> array = new ArrayList<Pair>();
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select * from card where name = ?", new String[] {cardName});

        // assuming we always have 1 result...
        cursor.moveToFirst();

        // order of the columns here is important, to make it persistent between online vs offline
        String[] columns = new String[] {"attribute", "types", "property", "level", "rank", "pendulumScale",
                "atk", "def", "cardnum", "passcode", "limitText", "ritualSpell", "ritualMonster", "fusionMaterials",
                "synchroMaterial", "materials", "summonedBy", "effectTypes"};

        for (int i = 0; i < columns.length; i++) {
            String value = cursor.getString(cursor.getColumnIndex(columns[i]));
            if (!value.equals("")) {
                array.add(new Pair(columnNameMap.get(columns[i]), value));
            }
        }
        cursor.close();
        return array;
    }

    private List<Pair> getCardInfoOnline(String cardName) throws Exception {
        initializeCardList();
        List<Pair> infos = new ArrayList<CardStore.Pair>();
        getCardDomReady(cardName);
        Document dom = cardDomCache.get(cardName);
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
                infos.add(new Pair(headerText, data));
                if (headerText.equals("Card effect types") || headerText.equals("Limitation Text")) {
                    break;
                }
            }
        }

        return infos;
    }

    //////////////////////////////////////////////////////////////////////
    // CARD STATUS
    //////////////////////////////////////////////////////////////////////

    public List<Pair> getCardStatus(String cardName) throws Exception {
        if (Util.hasNetworkConnectivity(context)) {
            return getCardStatusOnline(cardName);
        }
        else {
            return getCardStatusOffline(cardName);
        }
    }

    private List<Pair> getCardStatusOffline(String cardName) {
        List<Pair> array = new ArrayList<Pair>();
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select ocgStatus, tcgAdvStatus, tcgTrnStatus from card where name = ?", new String[] {cardName});

        // assuming we always have 1 result...
        cursor.moveToFirst();

        // order of the columns here is important, to make it persistent between online vs offline
        String[] columns = new String[] {"ocgStatus", "tcgAdvStatus", "tcgTrnStatus"};

        for (int i = 0; i < columns.length; i++) {
            String value = cursor.getString(cursor.getColumnIndex(columns[i]));
            if (value.equals("")) {
                continue;
            }

            if (value.equals("U")) {
                value = "Unlimited";
            }

            array.add(new Pair(columnNameMap.get(columns[i]), value));
        }
        cursor.close();
        return array;
    }

    private List<Pair> getCardStatusOnline(String cardName) throws Exception {
        initializeCardList();
        List<Pair> statuses = new ArrayList<CardStore.Pair>();
        getCardDomReady(cardName);
        Document dom = cardDomCache.get(cardName);

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
                    statuses.add(new Pair("All formats", tokens[0]));
                }
                else {
                    String status = tokens[0];
                    String format = tokens[1].replace("(", "").replace(")", "");
                    statuses.add(new Pair(format, status));
                }
            }
        }

        if (statuses.isEmpty()) {
            Log.i("ygodb", "Card banlist status not found online");
        }

        return statuses;
    }

    //////////////////////////////////////////////////////////////////////
    // CARD RULING, TIPS AND TRIVIA
    //////////////////////////////////////////////////////////////////////

    public String getCardRuling(String cardName) throws Exception {
        return getCardGenericInfo(CardAdditionalInfoType.Ruling, cardName);
    }

    public String getCardTips(String cardName) throws Exception {
        return getCardGenericInfo(CardAdditionalInfoType.Tips, cardName);
    }

    public String getCardTrivia(String cardName) throws Exception {
        return getCardGenericInfo(CardAdditionalInfoType.Trivia, cardName);
    }

    public String getCardGenericInfo(CardAdditionalInfoType type, String cardName) throws Exception {
        if (Util.hasNetworkConnectivity(context) && cardLinkTable != null) {
            return getCardGenericInfoOnline(type, cardName);
        }
        else {
            return getCardGenericInfoOffline(type, cardName);
        }
    }

    private String getCardGenericInfoOffline(CardAdditionalInfoType type, String cardName) throws Exception {
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        String columnName = "";
        switch (type) {
            case Ruling: columnName = "ruling"; break;
            case Tips:   columnName = "tips";   break;
            case Trivia: columnName = "trivia"; break;
            default:
                throw new Exception("Unknown type of additional info!");
        }
        Cursor cursor = db.rawQuery("select " + columnName + " from card where name = ?", new String[] {cardName});

        // assuming we always have 1 result...
        cursor.moveToFirst();

        String value = cursor.getString(cursor.getColumnIndex(columnName));
        if (value.equals("")) {
            value = "Not available.";
        }
        cursor.close();
        return value;
    }

    private String getCardGenericInfoOnline(CardAdditionalInfoType type, String cardName) throws Exception {
        initializeCardList();
        String baseUrl;
        switch (type) {
            case Ruling:
                baseUrl = "http://yugioh.wikia.com/wiki/Card_Rulings:";
                break;
            case Tips:
                baseUrl = "http://yugioh.wikia.com/wiki/Card_Tips:";
                break;
            case Trivia:
                baseUrl = "http://yugioh.wikia.com/wiki/Card_Trivia:";
                break;
            default:
                throw new Exception("Unknown type of additional info!");
        }
        String url = baseUrl + CardStore.cardLinkTable.get(cardName)[0].substring(6);

        Document dom;

        try {
            dom = Jsoup.parse(Jsoup.connect(url).ignoreContentType(true).execute().body());
        } catch (Exception e) {
            Log.i("ygodb", "Error fetching " + url);
            return "Not available.";
        }

        Element content = dom.getElementById("mw-content-text");
        String info = YgoWikiaHtmlCleaner.getCleanedHtml(content, type);
        return info;
    }
}

