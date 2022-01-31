package com.chin.ygodb.dataSource;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.chin.common.Util;
import com.chin.ygodb.database.DatabaseQuerier;
import com.chin.ygowikitool.api.YugipediaApi;
import com.chin.ygowikitool.entity.Card;
import com.chin.ygowikitool.parser.YugiohWikiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final Map<String, String> columnNameMap = new HashMap<>();
    static {
        // not all columns are in here, just those in the info section
        columnNameMap.put("attribute"      , "Attribute");
        columnNameMap.put("types"          , "Types");
        columnNameMap.put("level"          , "Level");
        columnNameMap.put("atk"            , "ATK");
        columnNameMap.put("def"            , "DEF");
        columnNameMap.put("link"           , "Link");
        columnNameMap.put("linkMarkers"    , "Link Arrows");
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

    // List of all cards names, initialized in MainActivity's onCreate()
    public static List<String> cardNameList = null;

    // list of Card objects, used for displaying in the ListView (the backing list for the adapter)
    public static List<Card> cardList = new ArrayList<>(8192);

    // Map of a card's name to its wiki page url, initialized in MainActivity's onCreate()
    private static Map<String, String> cardLinkMap = null;

    // Map of a card's name to its Card object
    private static final Map<String, Card> cardMap = new ConcurrentHashMap<>(8192);

    // Set of card names in the offline database
    private static final Set<String> offlineCardSet = new HashSet<>(8192);

    // Map of real card name to article name
    private static final Map<String, String> realNameMap = new HashMap<>();

    // flag: initialized the cardList, but not the cardLinkMap
    private static boolean initializedOffline = false;

    private static boolean initializedOnline = false;

    // we use the application context so don't worry about this
    @SuppressLint("StaticFieldLeak")
    private static volatile CardStore INSTANCE;

    private final Context context;

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
            cardNameList = new ArrayList<>(8192);
            cardLinkMap = new HashMap<>(8192);
            initializeCardListOnline(true);
            initializeCardListOnline(false);

            if (!initializedOffline) {
                addOfflineCardsToCardList(false);
            }

            // add those that are online but not offline
            List<String> onlineOfflineDiff = new ArrayList<>(CardStore.cardNameList);
            onlineOfflineDiff.removeAll(offlineCardSet);
            Log.i("ygodb", "Diff between online and offline: " + onlineOfflineDiff.size());
            for (int i = 0; i < onlineOfflineDiff.size(); i++) {
                Card card = new Card();
                card.setName(onlineOfflineDiff.get(i));
                CardStore.cardList.add(card);
                CardStore.cardMap.put(card.getName(), card);
            }

            initializedOnline = true;
            Log.i("ygodb", "Done initializing online.");
            Log.i("ygodb", "Number of cards: " + cardNameList.size());
        }
        else if (!initializedOffline) {
            Log.i("ygodb", "Initializing offline...");
            CardStore.cardNameList = new ArrayList<>(8192);
            initializeCardListOffline();
            initializedOffline = true;
            Log.i("ygodb", "Done initializing offline.");
            Log.i("ygodb", "Number of cards: " + cardNameList.size());
        }
    }

    private void addOfflineCardsToCardList(boolean isOffline) {
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select name, realName, attribute, cardType, types, level, atk, " +
                                    "def, link, rank, pendulumScale, property " +
                                    "from card order by name", null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                Card card = new Card();
                card.setName(name);
                // note that we don't need all card info here - just those needed for displaying in the ListView
                card.setRealName(cursor.getString(cursor.getColumnIndexOrThrow("realName")));
                card.setAttribute(cursor.getString(cursor.getColumnIndexOrThrow("attribute")));
                card.setCardType(cursor.getString(cursor.getColumnIndexOrThrow("cardType")));
                card.setTypes(cursor.getString(cursor.getColumnIndexOrThrow("types")));
                card.setLevel(cursor.getString(cursor.getColumnIndexOrThrow("level")));
                card.setAtk(cursor.getString(cursor.getColumnIndexOrThrow("atk")));
                card.setDef(cursor.getString(cursor.getColumnIndexOrThrow("def")));
                card.setLink(cursor.getString(cursor.getColumnIndexOrThrow("link")));
                card.setRank(cursor.getString(cursor.getColumnIndexOrThrow("rank")));
                card.setPendulumScale(cursor.getString(cursor.getColumnIndexOrThrow("pendulumScale")));
                card.setProperty(cursor.getString(cursor.getColumnIndexOrThrow("property")));

                CardStore.cardList.add(card);
                CardStore.cardMap.put(name, card);
                if (isOffline) {
                    cardNameList.add(name);
                }

                // If the card has a real name, we also map that real name to the card object
                // However, this real name is not guaranteed to be unique and so the value of
                // the mapping may be incorrect.
                if (!card.getRealName().equals("")) {
                    realNameMap.put(card.getRealName(), card.getName());
                    CardStore.cardMap.put(card.getRealName(), card);
                }

                offlineCardSet.add(name);
                cursor.moveToNext();
            }
        }

        cursor.close();
        Collections.sort(cardNameList);
    }

    private void initializeCardListOffline() {
        addOfflineCardsToCardList(true);
    }

    private void initializeCardListOnline(boolean isTcg) throws Exception {
        YugipediaApi api = new YugipediaApi();
        Map<String, String> cardMap = api.getCardMap(isTcg);

        for (Map.Entry<String, String> entry : cardMap.entrySet()) {
            String cardName = entry.getKey();
            String articleId = entry.getValue();

            if (!cardLinkMap.containsKey(cardName)) {
                cardNameList.add(cardName);
                cardLinkMap.put(cardName, articleId);
            }
        }
    }

    public boolean hasCard(String cardName) {
        // technically the second check inside realNameMap is not needed since we already add the
        // mapping into cardMap
        return cardMap.containsKey(cardName) || realNameMap.containsKey(cardName);
    }

    public boolean hasCardOffline(String cardName) {
        // technically the offlineCardSet.contains(realNameMap.get(cardName)) check is not needed
        // since all our real names come from the db, so if our realNameMap contains it, it must be
        // in the db
        return offlineCardSet.contains(cardName) ||
                (realNameMap.containsKey(cardName) && offlineCardSet.contains(realNameMap.get(cardName)));
    }

    public Card getCard(String cardName) {
        return cardMap.get(cardName);
    }

    public String getImageLink(String cardName) {
        // try get from cache
        Card card = CardStore.cardMap.get(cardName);
        if (!card.getFullImgLink().equals("")) {
            return card.getFullImgLink();
        }

        String link = getImageLinkOffline(cardName);
        if (link != null) {
            card.setFullImgLink(link);
            return link;
        }

        return null;
    }

    public String getImageLinkOnline(String cardName) {
        Card card = cardMap.get(cardName);
        String imageUrl = YugiohWikiUtil.getFullYugipediaImageLink(card.getImg());
        if (card.getFullImgLink().equals("")) {
            card.setFullImgLink(imageUrl);
        }

        return imageUrl;
    }

    private String getImageLinkOffline(String cardName) {
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select img from card where name = ?", new String[] {cardName});

        if (cursor.getCount() == 0) {
            return null;
        }

        cursor.moveToFirst();
        String img = cursor.getString(cursor.getColumnIndexOrThrow("img"));
        if (img.equals("")) {
            return null;
        }

        cursor.close();

        try {
            String originalLink = YugiohWikiUtil.getFullYugipediaImageLink(img);

            // calculate the width of the images to be displayed
//            Display display = MainActivity.instance.getWindowManager().getDefaultDisplay();
//            Point size = new Point();
//            display.getSize(size);
//            int screenWidth = size.x;
//            int scaleWidth = (int) (screenWidth * 0.2);

//            String finalLink = com.chin.ygowikitool.parser.Util.getScaledYugipediaImageLink(originalLink, scaleWidth);
//            return finalLink;

            // Yugipedia doesn't seem to support on the fly scaled image generation
            return originalLink;
        }
        catch (Exception e) {
            Log.w("ygodb", "Error parsing image link from offline database: " + cardName);
            return null;
        }
    }


    //////////////////////////////////////////////////////////////////////
    // CARD LORE
    //////////////////////////////////////////////////////////////////////

    public String getCardLore(String cardName) {
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

        String lore = cursor.getString(cursor.getColumnIndexOrThrow("lore"));
        cursor.close();
        return lore;
    }

    private String getCardLoreOnline(String cardName) {
        Card card = CardStore.cardMap.get(cardName);
        return card.getLore();
    }

    //////////////////////////////////////////////////////////////////////
    // CARD INFO
    //////////////////////////////////////////////////////////////////////

    public List<Pair> getCardInfo(String cardName) {
        if (Util.hasNetworkConnectivity(context)) {
            return getCardInfoOnline(cardName);
        }
        else {
            return getCardInfoOffline(cardName);
        }
    }

    private List<Pair> getCardInfoOffline(String cardName) {
        List<Pair> array = new ArrayList<>();
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select * from card where name = ?", new String[] {cardName});

        // assuming we always have 1 result...
        cursor.moveToFirst();

        // order of the columns here is important, to make it persistent between online vs offline
        String[] columns = new String[] {"attribute", "types", "property", "level", "rank", "pendulumScale",
                "atk", "def", "link", "linkMarkers", "passcode", "limitText", "ritualSpell", "ritualMonster",
                "fusionMaterials", "synchroMaterial", "materials", "summonedBy", "effectTypes"};

        for (String column : columns) {
            String value = cursor.getString(cursor.getColumnIndexOrThrow(column));
            if (!value.equals("")) {
                if (column.equals("linkMarkers")) {
                    value = value.replaceAll("\\|", "");
                }
                array.add(new Pair(columnNameMap.get(column), value));
            }
        }
        cursor.close();

        cursor = db.rawQuery("select archetype.name from archetype, card_archetype, card " +
                "where archetype.id = card_archetype.archetypeId " +
                "and card_archetype.cardId = card.id " +
                "and card.name = ?", new String[] {cardName});

        String archetypes = "";
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (!archetypes.equals("")) archetypes += ", ";
                archetypes += name;

                cursor.moveToNext();
            }
        }
        cursor.close();

        if (!"".equals(archetypes)) {
            array.add(new Pair("Archetypes and series", archetypes));
        }

        return array;
    }

    private List<Pair> getCardInfoOnline(String cardName) {
        Card card = cardMap.get(cardName);

        // order of the columns here is important, to make it persistent between online vs offline
        String[] columns = new String[] {"attribute", "types", "property", "level", "rank", "pendulumScale",
                "atk", "def", "link", "linkMarkers", "passcode", "limitText", "ritualSpell", "ritualMonster",
                "fusionMaterials", "synchroMaterial", "materials", "summonedBy", "effectTypes"};

        String[] values = new String[] {
                card.getAttribute(), card.getTypes(), card.getProperty(), card.getLevel(), card.getRank(), card.getPendulumScale(),
                card.getAtk(), card.getDef(), card.getLink(), card.getLinkMarkers(), card.getPasscode(), card.getLimitText(), card.getRitualSpell(), card.getRitualMonster(),
                card.getFusionMaterials(), card.getSynchroMaterial(), card.getMaterials(), card.getSummonedBy(), card.getEffectTypes()
        };

        List<Pair> array = new ArrayList<>();

        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            String value = values[i];
            if (!value.equals("")) {
                if (column.equals("linkMarkers")) {
                    value = value.replaceAll("\\|", "");
                }
                array.add(new Pair(columnNameMap.get(column), value));
            }
        }

        return array;
    }

    //////////////////////////////////////////////////////////////////////
    // CARD STATUS
    //////////////////////////////////////////////////////////////////////

    public List<Pair> getCardStatus(String cardName) {
        if (Util.hasNetworkConnectivity(context)) {
            return getCardStatusOnline(cardName);
        }
        else {
            return getCardStatusOffline(cardName);
        }
    }

    private List<Pair> getCardStatusOffline(String cardName) {
        List<Pair> array = new ArrayList<>();
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        Cursor cursor = db.rawQuery("select ocgStatus, tcgAdvStatus, tcgTrnStatus from card where name = ?", new String[] {cardName});

        // assuming we always have 1 result...
        cursor.moveToFirst();

        // order of the columns here is important, to make it persistent between online vs offline
        String[] columns = new String[] {"ocgStatus", "tcgAdvStatus", "tcgTrnStatus"};

        for (String column : columns) {
            String value = cursor.getString(cursor.getColumnIndexOrThrow(column));
            if (value.equals("")) {
                continue;
            }

            if (value.equals("U")) {
                value = "Unlimited";
            }

            array.add(new Pair(columnNameMap.get(column), value));
        }
        cursor.close();
        return array;
    }

    private List<Pair> getCardStatusOnline(String cardName) {
        Card card = cardMap.get(cardName);

        String[] columns = new String[] {"ocgStatus", "tcgAdvStatus", "tcgTrnStatus"};
        String[] values = new String[] {
                card.getOcgStatus(), card.getTcgAdvStatus(), card.getTcgTrnStatus()
        };

        List<Pair> array = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            String value = values[i];
            if (value.equals("")) {
                continue;
            }

            if (value.equals("U")) {
                value = "Unlimited";
            }

            array.add(new Pair(columnNameMap.get(column), value));
        }

        return array;
    }

    //////////////////////////////////////////////////////////////////////
    // CARD RULING, TIPS AND TRIVIA
    //////////////////////////////////////////////////////////////////////

    public String getCardGenericInfo(CardAdditionalInfoType type, String cardName) throws Exception {
        if (Util.hasNetworkConnectivity(context) && cardLinkMap != null) {
            return getCardGenericInfoOnline(type, cardName);
        }
        else {
            return getCardGenericInfoOffline(type, cardName);
        }
    }

    private String getCardGenericInfoOffline(CardAdditionalInfoType type, String cardName) throws Exception {
        DatabaseQuerier dbq = new DatabaseQuerier(context);
        SQLiteDatabase db = dbq.getDatabase();
        String columnName;
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

        String value = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        if (value.equals("")) {
            value = "Not available.";
        }
        cursor.close();
        return value;
    }

    private String getCardGenericInfoOnline(CardAdditionalInfoType type, String cardName) throws Exception {
        initializeCardList();

        YugipediaApi api = new YugipediaApi();
        if (type == CardAdditionalInfoType.Ruling) {
            String content = api.getCardRulingByCardName(cardName);
            if (content != null) {
                return content;
            }

            Log.i("ygodb", "Error fetching " + cardName + " Yugipedia ruling");
        }
        else if (type == CardAdditionalInfoType.Tips) {
            String content = api.getCardTipsByCardName(cardName);
            if (content != null) {
                return content;
            }

            Log.i("ygodb", "Error fetching " + cardName + " Yugipedia tips");
        }
        else if (type == CardAdditionalInfoType.Trivia) {
            String ruling = api.getCardTriviaByCardName(cardName);
            if (ruling != null) {
                return ruling;
            }

            Log.i("ygodb", "Error fetching " + cardName + " Yugipedia trivia");
        }

        return "Not available.";
    }

    public String getCardPageId(String cardName) {
        return cardLinkMap.get(cardName);
    }

    public void setCard(String cardName, Card card) {
        cardMap.put(cardName, card);
    }
}

