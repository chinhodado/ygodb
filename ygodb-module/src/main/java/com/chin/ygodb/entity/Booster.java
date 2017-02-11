package com.chin.ygodb.entity;

import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

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

    private ImageView imgView;
    private TextView txtView;
    private String name;
    private Date releaseDate;

    public Booster() {
        try {
            releaseDate = DEFAULT_DATE_FORMAT.parse("January 1, 1970");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public ImageView getImgView() {
        return imgView;
    }

    public void setImgView(ImageView imgView) {
        this.imgView = imgView;
    }

    public TextView getTxtView() {
        return txtView;
    }

    public void setTxtView(TextView txtView) {
        this.txtView = txtView;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        boolean parsed = false;
        for (DateFormat dateFormat : DATE_FORMATS) {
            try {
                this.releaseDate = dateFormat.parse(releaseDate);
                parsed = true;
                break;
            }
            catch (ParseException e) {
                // do nothing
            }
        }

        if (!parsed) {
            Log.i("ygodb", "Unable to parse date: " + releaseDate + " for " + name);
        }
    }
}
