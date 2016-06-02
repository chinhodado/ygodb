package com.chin.ygodb;

import java.util.ArrayList;

/**
 * A simple class that represents a search criterion
 * A search criterion has a subject (e.g. hp), and operator (e.g. >=) and an object (e.g. 1000)
 * @author Chin
 *
 */
public class SearchCriterion {
    public String subject;
    public String operator;
    public String object;

    public SearchCriterion(String subject, String operator, String object) {
        this.subject = subject;
        this.operator = operator;
        this.object = object;
    }

    @Override
    public String toString() {
        return subject + " " + operator + " " + object;
    }

    /**
     * Get the criteria string represented by the supplied list of SearchCriterion
     * @param list A list of SearchCriterion
     * @return A string represents the whole criteria
     */
    public static String getCriteria(ArrayList<SearchCriterion> list) {
        if (list.isEmpty()) return "";

        String string = list.get(0).toString();
        for (int i = 1; i < list.size(); i++) {
            string += (" AND " + list.get(i).toString());
        }
        return string;
    }
}
