package com.chin.ygodb;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.chin.common.RegexFilterArrayAdapter;
import com.chin.ygodb.entity.Card;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A specialized adapter for use with Card
 */
@SuppressLint("DefaultLocale")
public class CardRegexFilterArrayAdapter extends RegexFilterArrayAdapter<Card> {

    protected CardArrayFilter mFilter;
    private int imgviewWidth;
    private int imgviewHeight;

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     * @param objects The objects to represent in the ListView.
     */
    public CardRegexFilterArrayAdapter(Context context, int resource, int textViewResourceId, List<Card> objects) {
        super(context, resource, textViewResourceId, objects);

        // calculate the width and height of the imageview for the card thumbnail
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        imgviewWidth = (int) (screenWidth * 0.2);
        imgviewHeight = (int) (imgviewWidth * 1.4576);
    }

    /**
     * {@inheritDoc}
     */
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
        TextView text = (TextView) view.findViewById(mFieldId);
        Card card = getItem(position);
        text.setText(Html.fromHtml(card.toString()));

        final ImageView imgView = (ImageView) view.findViewById(R.id.itemRowImage);
        imgView.setImageResource(android.R.color.transparent);
        imgView.getLayoutParams().width  = imgviewWidth;
        imgView.getLayoutParams().height = imgviewHeight;

        String imgLink = null;
        try {
            // this will fail if the card does not exist in the card store
            imgLink = cardStore.getImageLink(card.name);
        }
        catch (Exception e) {
            Log.w("frdict", "Cannot get image link for: " + card.name);
        }

        if (imgLink != null) {
            ImageLoader.getInstance().displayImage(imgLink, imgView);
        }
        else if (cardStore.hasCard(card.name)){
            // this will mostly be for cards that are online but not in the offline database
            // (e.g. new cards since the last db update)
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    try {
                        return cardStore.getImageLinkOnline(params[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String s) {
                    ImageLoader.getInstance().displayImage(s, imgView);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, card.name);
        }

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new CardArrayFilter();
        }
        return mFilter;
    }

    /**
     * <p>An array filter constrains the content of the array adapter with
     * a regex. Each item that is not matched by the regex
     * is removed from the list.</p>
     */
    private class CardArrayFilter extends Filter {
        @SuppressLint("DefaultLocale")
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<Card>(mObjects);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                List<Card> list;
                synchronized (mLock) {
                    list = new ArrayList<Card>(mOriginalValues);
                }
                results.values = list;
                results.count = list.size();
            } else {
                String filterString = prefix.toString().toLowerCase();

                List<Card> values;
                synchronized (mLock) {
                    values = new ArrayList<Card>(mOriginalValues);
                }

                final int count = values.size();
                final List<Card> newValues = new ArrayList<Card>();

                for (int i = 0; i < count; i++) {
                    final Card card = values.get(i);
                    final String valueText = card.name.toLowerCase();

                    // Filter using the filterString as the regex pattern
                    Pattern r = Pattern.compile(filterString);
                    Matcher m = r.matcher(valueText);
                    if (m.find()) {
                        newValues.add(card);
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            mObjects = (List<Card>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}