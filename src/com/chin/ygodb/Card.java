package com.chin.ygodb;

public class Card {
    public String name            = "";
    public String attribute      = "";
    public String types           = "";
    public String level           = "";
    public String atk             = "";
    public String def             = "";
    public String cardnum         = "";
    public String passcode        = "";
    public String effectTypes     = "";
    public String materials       = "";
    public String fusionMaterials = "";
    public String rank            = "";
    public String ritualSpell     = "";
    public String pendulumScale   = "";
    public String property        = "";
    public String summonedBy      = "";
    public String limitText       = "";
    public String synchroMaterial = "";
    public String ritualMonster   = "";
    public String ocgStatus       = "";
    public String tcgAdvStatus    = "";
    public String tcgTrnStatus    = "";

    @Override
    public String toString() {
        String tmp = "<b>" + name + "</b><br>";
        if (types.equals("Spell Card") || types.equals("Trap Card")) {
            tmp += ("<small>" + property + " " + types + "</small>");
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
            tmp += (attribute + " " + types + "<br>" + atk + "/" + def + "</small>");
        }
        return tmp;
    }
}