package com.chin.ygodb.entity;

public class Card {
    // name is the title of the article, which is guaranteed to be unique, but may not be the card's true name
    // e.g. it may be missing #, have "(card)" at the end, etc.
    public String name            = "";

    // realName is the real name of the card, but may not be unique (e.g. tokens, egyptian gods, etc.)
    public String realName        = "";
    public String attribute       = "";
    public String cardType        = "";
    public String types           = "";
    public String level           = "";
    public String atk             = "";
    public String def             = "";
    public String passcode        = "";
    public String effectTypes     = "";
    public String materials       = "";
    public String fusionMaterials = "";
    public String rank            = "";
    public String ritualSpell     = "";
    public String pendulumScale   = "";
    public String linkMarkers     = "";
    public String link            = "";
    public String property        = "";
    public String summonedBy      = "";
    public String limitText       = "";
    public String synchroMaterial = "";
    public String ritualMonster   = "";
    public String ocgStatus       = "";
    public String tcgAdvStatus    = "";
    public String tcgTrnStatus    = "";
    public String lore            = "";
    public String thumbnailImgUrl = "";

    // these comes from the booster card table
    public String setNumber = "";
    public String rarity = "";
    public String category = "";

    @Override
    public String toString() {
        String tmp = "<b>" + name + "</b><br>";
        if (!category.equals("")) {
            tmp += small(category) + "<br>";
        }
        if (cardType.equals("Spell") || cardType.equals("Trap")) {
            tmp += small(property + " " + cardType);
        }
        else {
            tmp += "<small>";
            if (!level.equals("")) {
                tmp += "Level " + level;
                if (!pendulumScale.equals("")) {
                    tmp += ", Pendulum Scale " + pendulumScale;
                }
                tmp += "<br>";
            }
            else if (!rank.equals("")) {
                tmp += "Rank " + rank + "<br>";
            }
            tmp += (attribute + " " + types + "<br>" + atk + "/" + (!link.equals("")? "LINK " + link : def) + "</small>");
        }

        if (!setNumber.equals("")) {
            tmp += "<br>" + small(setNumber);
            if (!rarity.equals("")) {
                tmp += small(" (" + rarity + ")");
            }
        }

        return small(tmp);
    }

    private String small(String text) {
        return "<small>" + text + "</small>";
    }

    private String smaller(String text) {
        return "<small>" + small(text) + "</small>";
    }
}