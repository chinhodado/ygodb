package com.chin.ygodb;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.chin.ygodb.CardStore.CardAdditionalInfoType;

public class YgoWikiaHtmlCleaner {
    static String getCleanedHtml(Element content, CardAdditionalInfoType type) {
        Elements navboxes = content.select("table.navbox");
        if (!navboxes.isEmpty()) {navboxes.first().remove();} // remove the navigation box

        content.select("script").remove();               // remove <script> tags
        content.select("noscript").remove();             // remove <noscript> tags
        content.select("#toc").remove();                 // remove the table of content
        content.select("sup").remove();                  // remove the sup tags
        content.select("#References").remove();          // remove the reference header
        content.select(".references").remove();          // remove the references section
        content.select(".mbox-image").remove();          // remove the image in the "previously official ruling" box

        // remove the "Previously Official Rulings" notice
        Elements tables = content.getElementsByTag("table");
        for (Element table : tables) {
            if (table.text().startsWith("These TCG rulings were issued by Upper Deck Entertainment")) {
                // TODO: may want to put a placeholder here so we know to put it back in later
                table.remove();
            }
        }

        if (type == CardAdditionalInfoType.Tips) {
            // remove the "lists" tables
            boolean foundListsHeader = false;
            Elements children = content.select("#mw-content-text").first().children();
            for (Element child : children) {
                if ((child.tagName().equals("h2") || child.tagName().equals("h3")) && child.text().contains("List")) {
                    foundListsHeader = true;
                    child.remove();
                    continue;
                }
                else if (foundListsHeader && (child.tagName().equals("h2") || child.tagName().equals("h3"))) {
                    break;
                }
                else if (foundListsHeader) {
                    child.remove();
                }
            }
        }

        if (type == CardAdditionalInfoType.Trivia) {
            Elements children = content.select("#mw-content-text").first().children();
            for (Element child : children) {
                if (child.text().contains("For trivia pertaining to this monster's Number")) {
                    child.remove();
                    break;
                }
            }
        }

        if (type == CardAdditionalInfoType.Ruling) {
            Elements children = content.select("#mw-content-text").first().children();
            for (Element child : children) {
                if (child.text().equals("Notes") && (child.tagName().equals("h2") || child.tagName().equals("h3"))) {
                    child.remove();
                    break;
                }
            }
        }

        removeComments(content);                         // remove comments
        removeAttributes(content);                       // remove all attributes. Has to be at the end, otherwise can't grab id, etc.
        removeEmptyTags(content);                        // remove all empty tags

        // convert to text
        String text = content.html();

        // remove useless tags
        text = text.replace("<span>", "").replace("</span>", "").replace("<a>", "").replace("</a>", "");
        return text;
    }

    private static void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }

    private static void removeAttributes(Element doc) {
        Elements el = doc.getAllElements();
        for (Element e : el) {
            Attributes at = e.attributes();
            for (Attribute a : at) {
                e.removeAttr(a.getKey());
            }
        }
    }

    private static void removeEmptyTags(Element doc) {
        for (Element element : doc.select("*")) {
            if (!element.hasText() && element.isBlock()) {
                element.remove();
            }
        }
    }
}
