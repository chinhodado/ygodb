package com.chin.ygodb.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chin.ygodb.BoosterRecyclerViewAdapter;
import com.chin.ygodb.PagerSlidingTabStrip;
import com.chin.ygodb.asyncTask.PopulateBoosterAsyncTask;
import com.chin.ygodb.entity.Booster;
import com.chin.ygodb.R;

import java.util.ArrayList;
import java.util.List;

public abstract class BoosterActivity extends BaseFragmentActivity {
    public static final String BOOSTER_NAME = "BOOSTER_NAME";
    public static final String BOOSTER_URL = "BOOSTER_URL";
    public static final String TYPE_TCG = "TCG";
    public static final String TYPE_OCG = "OCG";
    private final List<Booster> boosters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);

        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setViewPager(pager);
        tabs.setIndicatorColor(getResources().getColor(R.color.red));
    }

    /**
     * Get the type of this activity
     * @return either BoosterActivity.TYPE_TCG or BoosterActivity.TYPE_OCG
     */
    public abstract String getType();

    public List<Booster> getBoosterList() {
        return boosters;
    }

    public static class BoosterListFragment extends Fragment {
        PopulateBoosterAsyncTask myTask;

        public static BoosterListFragment newInstance() {
            BoosterListFragment f = new BoosterListFragment();
            Bundle b = new Bundle();
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
            View view = inflater.inflate(R.layout.fragment_booster_grid, container, false);
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.booster_recycler_view);
            recyclerView.setHasFixedSize(false);
            StaggeredGridLayoutManager gridLayoutManager = new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(gridLayoutManager);
            List<Booster> boosters = ((BoosterActivity) getActivity()).getBoosterList();
            BoosterRecyclerViewAdapter rcAdapter = new BoosterRecyclerViewAdapter(getContext(), boosters);
            recyclerView.setAdapter(rcAdapter);

            myTask = (PopulateBoosterAsyncTask) new PopulateBoosterAsyncTask(recyclerView, (BoosterActivity) getActivity())
                            .execute();

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
        private final String[] TITLES = { "Boosters" };

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
            return BoosterListFragment.newInstance();
        }
    }
}
