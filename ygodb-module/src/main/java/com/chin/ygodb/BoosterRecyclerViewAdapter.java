package com.chin.ygodb;

import android.content.Context;
import android.content.Intent;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chin.ygodb.activity.BoosterActivity;
import com.chin.ygodb.activity.BoosterDetailActivity;
import com.chin.ygowikitool.entity.Booster;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

/**
 * Recycler view adapter for booster staggered grid
 * <p>
 * Created by Chin on 11-Feb-17.
 */
public class BoosterRecyclerViewAdapter extends RecyclerView.Adapter<BoosterViewHolder> {
    private final List<Booster> itemList;
    private final Context context;

    public BoosterRecyclerViewAdapter(Context context, List<Booster> itemList) {
        this.itemList = itemList;
        this.context = context;
    }

    @Override
    public BoosterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item_booster, null);
        final BoosterViewHolder holder = new BoosterViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Booster booster = itemList.get(position);
                    Intent intent = new Intent(v.getContext(), BoosterDetailActivity.class);
                    intent.putExtra(BoosterActivity.BOOSTER_NAME, booster.getBoosterName());
                    intent.putExtra(BoosterActivity.BOOSTER_URL, booster.getUrl());
                    v.getContext().startActivity(intent);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(BoosterViewHolder holder, int position) {
        Booster booster = itemList.get(position);
        holder.txtView.setText(Html.fromHtml("<small><small>" + booster.getBoosterName() + "</small></small>"));
        // reset the imgView initially to avoid flickering (e.g. when the old image is still there and
        // the new image has not loaded yet)
        holder.imgView.setImageResource(android.R.color.transparent);
        ImageLoader.getInstance().displayImage(booster.getFullImgSrc(), holder.imgView);
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }
}
