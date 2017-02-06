package com.chin.ygodb.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.chin.ygodb.Card;
import com.chin.ygodb.PagerSlidingTabStrip;
import com.chin.ygodb.asyncTask.BoosterCardListAsyncTask;
import com.chin.ygodb2.R;

/**
 * Activity for booster detail
 *
 * Created by Chin on 05-Feb-17.
 */
public class BoosterDetailActivity extends BaseFragmentActivity {

    private String boosterName = null;
    private String boosterUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            boosterName = savedInstanceState.getString(BoosterActivity.BOOSTER_NAME);
            boosterUrl = savedInstanceState.getString(BoosterActivity.BOOSTER_URL);
        }
        else {
            Intent intent = getIntent(); // careful, this intent may not be the intent from MainActivity...
            String tmpName = intent.getStringExtra(BoosterActivity.BOOSTER_NAME);
            String tmpUrl = intent.getStringExtra(BoosterActivity.BOOSTER_URL);
            if (tmpName != null) {
                boosterName = tmpName; // needed since we may come back from other activity, not just the main one
            }

            if (tmpUrl != null) {
                boosterUrl = tmpUrl; // needed since we may come back from other activity, not just the main one
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

        getActionBar().setTitle(boosterName);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(BoosterActivity.BOOSTER_NAME, boosterName);
        bundle.putString(BoosterActivity.BOOSTER_URL, boosterUrl);
    }

    public static class CardListFragment extends Fragment {
        BoosterCardListAsyncTask myTask;

        public static CardListFragment newInstance(String boosterName, String boosterUrl) {
            CardListFragment f = new CardListFragment();
            Bundle b = new Bundle();
            b.putString(BoosterActivity.BOOSTER_NAME, boosterName);
            b.putString(BoosterActivity.BOOSTER_URL, boosterUrl);
            f.setArguments(b);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @SuppressLint("RtlHardcoded")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_search_result, container, false);
            ListView famListView = (ListView) view.findViewById(R.id.resultListView);

            String boosterName = getArguments().getString(BoosterActivity.BOOSTER_NAME);
            String boosterUrl = getArguments().getString(BoosterActivity.BOOSTER_URL);
            myTask = (BoosterCardListAsyncTask) new BoosterCardListAsyncTask(boosterName, boosterUrl,
                    famListView, (BoosterDetailActivity) getActivity())
                    .execute();

            // go to a card's detail page when click on its name on the list
            famListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                    String cardName = ((Card)arg0.getItemAtPosition(position)).name;
                    Intent intent = new Intent(v.getContext(), CardDetailActivity.class);
                    intent.putExtra(MainActivity.CARD_NAME, cardName);
                    startActivity(intent);
                }
            });

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

    private class MyPagerAdapter extends FragmentPagerAdapter {
        private final String[] TITLES = { "Card list" };

        private MyPagerAdapter(FragmentManager fm) {
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
            return CardListFragment.newInstance(boosterName, boosterUrl);
        }
    }
}
