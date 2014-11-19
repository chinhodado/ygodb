package com.chin.ygodb.activity;

import com.chin.ygodb.activity.BaseFragmentActivity;
import com.chin.ygodb.CardStore;
import com.chin.common.NetworkDialogFragment;
import com.chin.ygodb2.R;
import com.chin.common.RegexFilterArrayAdapter;

import android.app.DialogFragment;
import android.content.Intent;
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

/**
 * The main activity, entry point of the app. It consists of the card search list.
 */
public class MainActivity extends BaseFragmentActivity {

    public final static String CARD_LINK = "com.chin.ygodb.LINK";
    public final static String CARD_NAME = "com.chin.ygodb.NAME";

    public static RegexFilterArrayAdapter<String> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the card list and their wiki url
        if (CardStore.cardList == null) {
            try {
                CardStore.getInstance(this).initializeCardList();
            } catch (Exception e) {
                DialogFragment newFragment = new NetworkDialogFragment();
                newFragment.setCancelable(false);
                newFragment.show(getFragmentManager(), "no net");
                e.printStackTrace();
                return;
            }
        }

        if (savedInstanceState == null) {
            SearchCardFragment newFragment = new SearchCardFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.tab_viewgroup, newFragment).commit();
        }
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
                    adapter = new RegexFilterArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, CardStore.cardList);
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
                            String cardName = (String)arg0.getItemAtPosition(position);
                            Intent intent = new Intent(v.getContext(), CardDetailActivity.class);
                            intent.putExtra(CARD_NAME, cardName);
                            intent.putExtra(CARD_LINK, CardStore.cardLinkTable.get(cardName));
                            startActivity(intent);
                    }
                });

            } catch (Exception e) {
                Log.e("MainActivity", "Error setting up the card list");
                e.printStackTrace();
            }

            return view;
        }
    }
}
