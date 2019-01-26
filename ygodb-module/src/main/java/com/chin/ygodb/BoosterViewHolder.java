package com.chin.ygodb;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chin.ygodb.R;

/**
 * View holder for booster card
 *
 * Created by Chin on 11-Feb-17.
 */
public class BoosterViewHolder extends RecyclerView.ViewHolder {
    public TextView txtView;
    public ImageView imgView;

    public BoosterViewHolder(View itemView) {
        super(itemView);
        txtView = (TextView) itemView.findViewById(R.id.itemRowTextBooster);
        imgView = (ImageView) itemView.findViewById(R.id.itemRowImageBooster);
    }
}