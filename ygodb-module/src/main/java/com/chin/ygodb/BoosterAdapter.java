package com.chin.ygodb;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.chin.common.RegexFilterArrayAdapter;
import com.chin.ygodb.entity.Booster;
import com.chin.ygodb.entity.Card;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

/**
 * Adapter for use in the booster grid view
 * Created by Chin on 10-Feb-17.
 */
public class BoosterAdapter extends RegexFilterArrayAdapter<Booster> {
    private Context mContext;

    public BoosterAdapter(Context context, int resource, int textViewResourceId, List<Booster> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        final CardStore cardStore = CardStore.getInstance(mContext);
        TextView txtView = (TextView) view.findViewById(mFieldId);
        Booster booster = getItem(position);
        txtView.setText(Html.fromHtml("<small><small>" + booster.getName() + "</small></small>"));

        final ImageView imgView = (ImageView) view.findViewById(R.id.itemRowImageBooster);
        imgView.setImageResource(android.R.color.transparent);
//        imgView.getLayoutParams().width  = imgviewWidth;
//        imgView.getLayoutParams().height = imgviewHeight;

//        String imgLink = null;
//        try {
//            // this will fail if the card does not exist in the card store
//            imgLink = cardStore.getImageLink(card.name);
//        }
//        catch (Exception e) {
//            Log.w("frdict", "Cannot get image link for: " + card.name);
//        }
//
//        if (imgLink != null) {
//            ImageLoader.getInstance().displayImage(imgLink, imgView);
//        }
//        else if (cardStore.hasCard(card.name)){
//            // this will mostly be for cards that are online but not in the offline database
//            // (e.g. new cards since the last db update)
//            new AsyncTask<String, Void, String>() {
//                @Override
//                protected String doInBackground(String... params) {
//                    try {
//                        return cardStore.getImageLinkOnline(params[0]);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    return null;
//                }
//
//                @Override
//                protected void onPostExecute(String s) {
//                    ImageLoader.getInstance().displayImage(s, imgView);
//                }
//            }.executeOnExecutor(imageThreadPoolExecutor, card.name);
//        }

        ImageLoader.getInstance().displayImage(booster.getImgSrc(), imgView);

        return view;
    }
}
