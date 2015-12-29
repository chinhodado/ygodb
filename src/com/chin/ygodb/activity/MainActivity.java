package com.chin.ygodb.activity;

import com.chin.common.CustomDialogFragment;
import com.chin.common.Util;
import com.chin.ygodb.Card;
import com.chin.ygodb.CardRegexFilterArrayAdapter;
import com.chin.ygodb.CardStore;
import com.chin.ygodb2.R;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * The main activity, entry point of the app. It consists of the card search list.
 */
public class MainActivity extends BaseFragmentActivity {
    static boolean hasJustBeenStarted = true; // flag to determine if the app has just been started
    public final static String CARD_LINK = "com.chin.ygodb.LINK";
    public final static String CARD_NAME = "com.chin.ygodb.NAME";

    public static CardRegexFilterArrayAdapter adapter = null;
    public static MainActivity instance;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasJustBeenStarted) {
            Util.checkNewVersion(this, "https://api.github.com/repos/chinhodado/ygodb/releases/latest",
                    "https://github.com/chinhodado/ygodb/releases", false);
        }

        final ProgressBar initializingSpin = (ProgressBar)findViewById(R.id.progressBarInitializaing);
        final TextView initializingTv = (TextView)findViewById(R.id.textViewInitializing);

        // get the card list and their wiki url
        if (CardStore.cardNameList == null) {
            try {
                new AsyncTask<Context, Void, Void>() {
                    @Override
                    protected Void doInBackground(Context... params) {
                        try {
                            CardStore.getInstance(params[0]).initializeCardList();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void foo) {
                        initializingSpin.setVisibility(View.GONE);
                        initializingTv.setVisibility(View.GONE);
                        if (savedInstanceState == null) {
                            SearchCardFragment newFragment = new SearchCardFragment();
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.add(R.id.tab_viewgroup, newFragment).commit();
                        }
                    }
                }.execute(this);
            } catch (Exception e) {
                e.printStackTrace();
                CustomDialogFragment newFragment = new CustomDialogFragment(
                        "Something went horribly wrong. Please send me an email at chinho.dev@gmail.com if this persists.");
                newFragment.setCancelable(false);
                newFragment.show(getFragmentManager(), "no net");
                return;
            }
        }

        // for our purposes, consider the app already opened at this point
        hasJustBeenStarted = false;
        instance = this;
    }

    /**
     * Fragment for the search card view
     */
    public static class SearchCardFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View view = inflater.inflate(R.layout.fragment_search_card, container, false);

            try {
                if (adapter == null) {
                    adapter = new CardRegexFilterArrayAdapter(getActivity(), R.layout.list_item_card, R.id.itemRowText, CardStore.cardList);
                }

                EditText cardEditText = (EditText) view.findViewById(R.id.cardEditText);

                cardEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        //adapter.getFilter().filter(s);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void afterTextChanged(Editable s) {
                        adapter.getFilter().filter(s);
                    }
                });

                ListView cardListView = (ListView) view.findViewById(R.id.cardListView);
                cardListView.setAdapter(adapter);
                cardListView.setOnItemClickListener(new OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                            String cardName = ((Card)arg0.getItemAtPosition(position)).name;
                            Intent intent = new Intent(v.getContext(), CardDetailActivity.class);
                            intent.putExtra(CARD_NAME, cardName);
                            startActivity(intent);
                    }
                });
            }
            catch (Exception e) {
                Log.e("MainActivity", "Error setting up the card list");
                e.printStackTrace();
            }

            return view;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }
}
