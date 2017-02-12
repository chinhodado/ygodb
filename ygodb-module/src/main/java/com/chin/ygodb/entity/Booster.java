package com.chin.ygodb.entity;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a booster
 *
 * Created by Chin on 04-Feb-17.
 */
public class Booster {
    private static DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
    private static DateFormat[] DATE_FORMATS = new DateFormat[] {
            DEFAULT_DATE_FORMAT,
            new SimpleDateFormat("MMMM, yyyy", Locale.ENGLISH),
            new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH),
            new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH),
            new SimpleDateFormat("MMMM d yyyy", Locale.ENGLISH),
    };

    private String name;
    private Date enReleaseDate;
    private Date jpReleaseDate;
    private String shortenedImgSrc; // comes from the db
    private String scaledImgSrc;    // to be used in the booster list
    private String url;

    public Booster() {
        try {
            enReleaseDate = DEFAULT_DATE_FORMAT.parse("January 1, 1970");
            jpReleaseDate = DEFAULT_DATE_FORMAT.parse("January 1, 1970");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getEnReleaseDate() {
        return enReleaseDate;
    }

    /**
     * Set the English release date of the booster. If the date is not parsable,
     * the previous release date will not change (which has a default value
     * of January 1, 1970)
     *
     * @param date Release date to set
     */
    public void setEnReleaseDate(String date) {
        if (date == null) {
            return;
        }

        boolean parsed = false;
        for (DateFormat dateFormat : DATE_FORMATS) {
            try {
                this.enReleaseDate = dateFormat.parse(date);
                parsed = true;
                break;
            }
            catch (Exception e) {
                // do nothing
            }
        }

        if (!parsed) {
            Log.i("ygodb", "Unable to parse date: " + date + " for " + name);
        }
    }

    public Date getJpReleaseDate() {
        return jpReleaseDate;
    }

    /**
     * Set the Japanese release date of the booster. If the date is not parsable,
     * the previous release date will not change (which has a default value
     * of January 1, 1970)
     *
     * @param date Release date to set
     */
    public void setJpReleaseDate(String date) {
        if (date == null) {
            return;
        }

        boolean parsed = false;
        for (DateFormat dateFormat : DATE_FORMATS) {
            try {
                this.jpReleaseDate = dateFormat.parse(date);
                parsed = true;
                break;
            }
            catch (Exception e) {
                // do nothing
            }
        }

        if (!parsed) {
            Log.i("ygodb", "Unable to parse date: " + date + " for " + name);
        }
    }

    public String getScaledImgSrc() {
        return scaledImgSrc;
    }

    public void setScaledImgSrc(String scaledImgSrc) {
        this.scaledImgSrc = scaledImgSrc;
    }

    public String getShortenedImgSrc() {
        return shortenedImgSrc;
    }

    public void setShortenedImgSrc(String shortenedImgSrc) {
        this.shortenedImgSrc = shortenedImgSrc;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
