package com.chin.ygodb.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chin.common.MyTagHandler;
import com.chin.ygodb.PagerSlidingTabStrip;
import com.chin.ygodb.asyncTask.AddCardInfoTask;
import com.chin.ygodb.dataSource.CardStore;
import com.chin.ygodb.dataSource.CardStore.CardAdditionalInfoType;
import com.chin.ygodb.R;

/**
 * Activity to show all details about a card
 */
public class CardDetailActivity extends BaseFragmentActivity {

    public String cardName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            cardName = savedInstanceState.getString("CARDNAME");

            // When system is low on mem, it kills this app in background
            // (so hasJustBeenStarted is true). So the whole CardStore is also toasted,
            // which means we can't do anything and need to go back to MainActivity
            // so that it can initialize the CardStore and everything else in onCreate()
            // TODO: come back to this activity with the current card after
            // reinitialization is done
            if (MainActivity.hasJustBeenStarted) {
                finish();
                return;
            }
        }
        else {
            Intent intent = getIntent(); // careful, this intent may not be the intent from MainActivity...
            String tmpName = intent.getStringExtra(MainActivity.CARD_NAME);
            if (tmpName != null) {
                cardName = tmpName; // needed since we may come back from other activity, not just the main one
            }
        }

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);

        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true); // note: has to be before setViewPager()
        tabs.setViewPager(pager);
        tabs.setIndicatorColor(ContextCompat.getColor(this, R.color.red));

        getActionBar().setTitle(CardStore.getInstance(this).getCard(cardName).getDisplayName());
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("CARDNAME", cardName);
    }

    /**
     * Fragment for the card info view
     */
    public static class CardInfoFragment extends Fragment {

        AsyncTask<?, ?, ?> myTask = null;
        static String cardName;

        public static CardInfoFragment newInstance(String cardName) {
            CardInfoFragment f = new CardInfoFragment();
            CardInfoFragment.cardName = cardName;
            return f;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View view = inflater.inflate(R.layout.fragment_card_info, container, false);
            myTask = new AddCardInfoTask((CardDetailActivity) getActivity()).execute(cardName);
            return view;
        }

        @Override
        public void onPause() {
            super.onPause();
            if (myTask != null) {
                myTask.cancel(true);
                myTask = null;
            }
        }
    }

    public static class CardGenericDetailFragment extends Fragment {
        PopulateAdditionalInfoAsyncTask myTask;

        private static final String TYPE = "TYPE";
        private static final String CARD_NAME = "CARD_NAME";

        CardAdditionalInfoType type;
        String cardName;

        public static CardGenericDetailFragment newInstance(CardAdditionalInfoType type, String cardName) {
            CardGenericDetailFragment f = new CardGenericDetailFragment();
            Bundle b = new Bundle();
            b.putSerializable(TYPE, type);
            b.putString(CARD_NAME, cardName);
            f.setArguments(b);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            cardName = getArguments().getString(CARD_NAME);
            type = (CardAdditionalInfoType) getArguments().getSerializable(TYPE);
            setRetainInstance(true);
        }

        @SuppressLint("RtlHardcoded")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_general_linear, container, false);
            LinearLayout layout = (LinearLayout) view.findViewById(R.id.fragment_layout);
            layout.setGravity(Gravity.RIGHT);

            myTask = (PopulateAdditionalInfoAsyncTask) new PopulateAdditionalInfoAsyncTask(layout, (CardDetailActivity) getActivity(), type, cardName).execute();

            return view;
        }

        @Override
        public void onPause() {
            super.onPause();
            if (myTask != null) {
                myTask.cancel(true);
                myTask = null;
            }
        }
    }

    private static class PopulateAdditionalInfoAsyncTask extends AsyncTask<String, Void, String> {
        LinearLayout layout;
        CardDetailActivity activity;
        CardAdditionalInfoType type;
        String cardName;
        boolean exceptionOccurred = false;

        PopulateAdditionalInfoAsyncTask(LinearLayout layout, CardDetailActivity activity, CardAdditionalInfoType type, String cardName) {
            this.layout = layout;
            this.activity = activity;
            this.type = type;
            this.cardName = cardName;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                String info = CardStore.getInstance(activity).getCardGenericInfo(type, cardName);
                return info;
            } catch (Exception e) {
                e.printStackTrace();

                // set the flag so we can do something about this in onPostExecute()
                exceptionOccurred = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String param) {

            if (exceptionOccurred) {
                // remove the spinner
                ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.progressBar_fragment_general);
                layout.removeView(pgrBar);
                TextView tv = new TextView(activity);
                layout.addView(tv);
                tv.setText("Not available");
                return;
            }

            TextView tv = new TextView(activity);
            layout.addView(tv);
            tv.setText(Html.fromHtml(param, null, new MyTagHandler()));

            // remove the spinner
            ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.progressBar_fragment_general);
            layout.removeView(pgrBar);
        }
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = { "Detail", "Ruling", "Tips", "Trivia"};

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return CardInfoFragment.newInstance(cardName);
            }
            else if (position == 1){
                return CardGenericDetailFragment.newInstance(CardAdditionalInfoType.Ruling, cardName);
            }
            else if (position == 2) {
                return CardGenericDetailFragment.newInstance(CardAdditionalInfoType.Tips, cardName);
            }
            else {
                return CardGenericDetailFragment.newInstance(CardAdditionalInfoType.Trivia, cardName);
            }
        }
    }
}
