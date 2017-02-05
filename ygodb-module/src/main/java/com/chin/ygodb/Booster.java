package com.chin.ygodb;

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
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
    private static DateFormat DATE_FORMAT_2 = new SimpleDateFormat("MMMM, yyyy", Locale.ENGLISH);
    private static DateFormat DATE_FORMAT_3 = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

    private ImageView imgView;
    private TextView txtView;
    private String name;
    private Date releaseDate;

    public Booster() {
        try {
            releaseDate = DATE_FORMAT.parse("January 1, 1970");
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
        try {
            this.releaseDate = DATE_FORMAT.parse(releaseDate);
            parsed = true;
        } catch (ParseException e) {
            // do nothing
        }

        if (!parsed) {
            try {
                this.releaseDate = DATE_FORMAT_2.parse(releaseDate);
                parsed = true;
            } catch (ParseException e) {
                // do nothing
            }
        }

        if (!parsed) {
            try {
                this.releaseDate = DATE_FORMAT_3.parse(releaseDate);
            } catch (ParseException e) {
                Log.i("ygodb", "Unable to parse date: " + releaseDate + " for " + name);
            }
        }
    }
}
