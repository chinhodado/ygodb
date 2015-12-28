package com.chin.ygodb;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.chin.common.RegexFilterArrayAdapter;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A specialized adapter for use with Card
 */
@SuppressLint("DefaultLocale")
public class CardRegexFilterArrayAdapter extends RegexFilterArrayAdapter<Card> {

    protected CardArrayFilter mFilter;

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

        TextView text = (TextView) view.findViewById(mFieldId);
        Card card = getItem(position);
        text.setText(Html.fromHtml(card.toString()));

        ImageView imgView = (ImageView) view.findViewById(R.id.itemRowImage);
        imgView.setImageResource(android.R.color.transparent);
        ImageLoader.getInstance().displayImage(CardStore.getInstance(mContext).getImageLinkOffline(card.name), imgView);

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
                ArrayList<Card> list;
                synchronized (mLock) {
                    list = new ArrayList<Card>(mOriginalValues);
                }
                results.values = list;
                results.count = list.size();
            } else {
                String filterString = prefix.toString().toLowerCase();

                ArrayList<Card> values;
                synchronized (mLock) {
                    values = new ArrayList<Card>(mOriginalValues);
                }

                final int count = values.size();
                final ArrayList<Card> newValues = new ArrayList<Card>();

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