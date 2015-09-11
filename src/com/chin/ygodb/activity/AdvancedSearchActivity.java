package com.chin.ygodb.activity;

import java.util.ArrayList;
import java.util.Collections;

import com.chin.common.TabListener;
import com.chin.ygodb.DatabaseQuerier;
import com.chin.ygodb.SearchCriterion;
import com.chin.ygodb2.R;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * The activity for advanced search function
 * @author Chin
 *
 */
public class AdvancedSearchActivity extends BaseFragmentActivity {
    public static String[] mainCategoryList = {"(All)", "Monster", "Spell", "Trap"};
    public static String[] subCategoryMonsterList = {"(All)", "Normal", "Effect", "Fusion", "Ritual", "Synchro", "Xyz",
    		"Pendulum", "Tuner", "Gemini", "Union", "Spirit", "Flip", "Toon"};
    public static String[] subCategorySpellList = {"(All)", "Normal", "Quick-Play", "Continuous", "Ritual", "Equip", "Field"};
    public static String[] subCategoryTrapList = {"(All)", "Normal", "Continuous", "Counter"};
    public static String[] attributeList = {"(All)", "Earth", "Water", "Fire", "Wind", "Light", "Dark", "Divine"};
    public static String[] typeList = {"(All)", "Warrior", "Spellcaster", "Fairy", "Fiend", "Zombie", "Machine", "Aqua",
    		"Pyro", "Rock", "Winged Beast", "Plant", "Insect", "Thunder", "Dragon", "Beast", "Beast-Warrior", "Dinosaur",
    		"Fish", "Sea Serpent", "Reptile", "Psychic", "Divine-Beast", "Creator God", "Wyrm"};
    public static String[] limitList = {"(All)", "Banned", "Limited", "Semi-limited"};

    static ArrayAdapter<String> subCategoryMonsterAdapter;
    static ArrayAdapter<String> subCategorySpellAdapter;
    static ArrayAdapter<String> subCategoryTrapAdapter;
    static int lastSelectedSubCategoryPosition = -1;
    static String lastSelectedMainCategory = "(All)";

    // store the result set after each search
    static ArrayList<String> resultSet = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add the tabs
        ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        bar.addTab(bar.newTab().setText("Search")
                .setTabListener(new TabListener<SearchCriteriaFragment>(this, "search", SearchCriteriaFragment.class, null, R.id.tab_viewgroup)));
        bar.addTab(bar.newTab().setText("Results")
                .setTabListener(new TabListener<SearchResultFragment>(this, "result", SearchResultFragment.class, null, R.id.tab_viewgroup)));

        // if we're resuming the activity, re-select the tab that was selected before
        if (savedInstanceState != null) {
            // Select the tab that was selected before orientation change
            int index = savedInstanceState.getInt("TAB_INDEX");
            bar.setSelectedNavigationItem(index);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
      super.onSaveInstanceState(bundle);
      // Save the index of the currently selected tab
      bundle.putInt("TAB_INDEX", getActionBar().getSelectedTab().getPosition());
    }

    /**
     * Fragment for the search criteria
     */
    public static class SearchCriteriaFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            final View view = inflater.inflate(R.layout.fragment_advanced_search_criteria, container, false);
            LinearLayout layout = (LinearLayout) view.findViewById(R.id.fragment_layout);
            initializeSkillSearchUI(layout);

            Button searchButton = (Button) layout.findViewById(R.id.searchButton);
            searchButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // make the list of criteria
                    ArrayList<SearchCriterion> criteriaList = new ArrayList<SearchCriterion>();
                    EditText levelRankEdit = (EditText) view.findViewById(R.id.criteriaLevelRank);
                    String levelRankInput = levelRankEdit.getText().toString().trim();
                    String levelRankSql = null;
                    if (!levelRankInput.equals("")) {
                    	// this has to be done manually since out query is complex...
                    	String levelRankOperator = ((Spinner) view.findViewById(R.id.spinnerOperatorLevelRank)).getSelectedItem().toString();
                    	levelRankSql = "((level <> \"\" and level " + levelRankOperator + " " + levelRankInput +
                    			") or (rank <> \"\" and rank " + levelRankOperator + " " + levelRankInput + "))";
                    }

                    // atk
                    EditText atkEdit = (EditText) view.findViewById(R.id.criteriaAtk);
                    String atkInput = atkEdit.getText().toString().trim();
                    String atkOperator = ((Spinner) view.findViewById(R.id.spinnerOperatorAtk)).getSelectedItem().toString();
                    if (!atkInput.equals("")) {
                    	criteriaList.add(new SearchCriterion("atk", atkOperator, atkInput));
                    	// hacky, since we have values like "?", etc.
                    	// TODO: handle "?", "X000", "????"
                    	criteriaList.add(new SearchCriterion("atk", ">=", "0"));
                    	criteriaList.add(new SearchCriterion("atk", "<=", "9999999"));
                    	criteriaList.add(new SearchCriterion("atk", "<>", "\"\""));
                    }

                    // def
                    EditText defEdit = (EditText) view.findViewById(R.id.criteriaDef);
                    String defInput = defEdit.getText().toString().trim();
                    String defOperator = ((Spinner) view.findViewById(R.id.spinnerOperatorDef)).getSelectedItem().toString();
                    if (!defInput.equals("")) {
                    	criteriaList.add(new SearchCriterion("def", defOperator, defInput));
                    	// hacky, since we have values like "?", etc.
                    	// TODO: handle "?", "X000", "????"
                    	criteriaList.add(new SearchCriterion("def", ">=", "0"));
                    	criteriaList.add(new SearchCriterion("def", "<=", "9999999"));
                    	criteriaList.add(new SearchCriterion("def", "<>", "\"\""));
                    }

                    // main category
                    String mainCategoryInput = ((Spinner) view.findViewById(R.id.spinnerMainCategory)).getSelectedItem().toString();
                    String mainCategorySql = null;
                    if (!mainCategoryInput.equals("(All)")) {
                        if (mainCategoryInput.equals("Monster")) {
                        	mainCategorySql = "atk <> \"\""; // relies on the fact that all monster have atk
                        }
                        else if (mainCategoryInput.equals("Spell")) {
                        	mainCategorySql = "types = \"Spell Card\"";
                        }
                        else if (mainCategoryInput.equals("Trap")) {
                        	mainCategorySql = "types = \"Trap Card\"";
                        }
                    }

                    // sub category
                    String subCategoryInput = ((Spinner) view.findViewById(R.id.spinnerSubCategory)).getSelectedItem().toString();
                    String subCategorySql = null;
                    if (!subCategoryInput.equals("(All)")) {
                        if (mainCategoryInput.equals("Monster")) {
                        	subCategorySql = "types like \"%" + subCategoryInput + "%\"";
                        }
                        else if (mainCategoryInput.equals("Spell") || mainCategoryInput.equals("Trap")) {
                        	subCategorySql = "property = \"" + subCategoryInput + "\"";
                        }
                    }

                    // attribute
                    String attributeInput = ((Spinner) view.findViewById(R.id.spinnerAttribute)).getSelectedItem().toString();
                    String attributeSql = null;
                    if (!attributeInput.equals("(All)")) {
                    	attributeSql = "attribute = \"" + attributeInput.toUpperCase() + "\"";
                    }

                    // type
                    String typeInput = ((Spinner) view.findViewById(R.id.spinnerType)).getSelectedItem().toString();
                    String typeSql = null;
                    if (!typeInput.equals("(All)")) {
                    	typeSql = "types like \"%" + typeInput + "%\"";
                    }

                    // limit
                    String limitInput = ((Spinner) view.findViewById(R.id.spinnerLimit)).getSelectedItem().toString();
                    String limitSql = null;
                    if (!limitInput.equals("(All)")) {
                    	if (limitInput.equals("Banned")) {
                    		limitSql = "(tcgAdvStatus = \"Illegal\" or tcgAdvStatus = \"Forbidden\")";
                        }
                        else if (limitInput.equals("Limited")) {
                        	limitSql = "tcgAdvStatus = \"Limited\"";
                        }
                        else if (limitInput.equals("Semi-limited")) {
                        	limitSql = "tcgAdvStatus = \"Semi-Limited\"";
                        }
                    }

                    // get the SQL where clause
                    String criteria = SearchCriterion.getCriteria(criteriaList);
                    if (levelRankSql != null) {
                    	if (!criteria.equals("")) criteria += " AND ";
                    	criteria += levelRankSql;
                    }

                    if (mainCategorySql != null) {
                    	if (!criteria.equals("")) criteria += " AND ";
                    	criteria += mainCategorySql;
                    }

                    if (subCategorySql != null) {
                    	if (!criteria.equals("")) criteria += " AND ";
                    	criteria += subCategorySql;
                    }

                    if (attributeSql != null) {
                    	if (!criteria.equals("")) criteria += " AND ";
                    	criteria += attributeSql;
                    }

                    if (typeSql != null) {
                    	if (!criteria.equals("")) criteria += " AND ";
                    	criteria += typeSql;
                    }

                    if (limitSql != null) {
                    	if (!criteria.equals("")) criteria += " AND ";
                    	criteria += limitSql;
                    }

                    // then execute the query and get the result
                    DatabaseQuerier querier = new DatabaseQuerier(v.getContext());
                    resultSet = querier.executeQuery(criteria);
                    Collections.sort(resultSet);

                    // now display the results
                    if (!resultSet.isEmpty()) {
                        // switch to the result tab (with index 1)
                        getActivity().getActionBar().setSelectedNavigationItem(1);
                        Toast toast = Toast.makeText(getActivity(), "Found " + resultSet.size() + " results.", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    else {
                        Toast toast = Toast.makeText(getActivity(), "No results found.", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            });

            return view;
        }

        private void initializeSkillSearchUI(View view) {
            int[] spinnerOperatorIdList = new int[] {
                R.id.spinnerOperatorAtk, R.id.spinnerOperatorDef, R.id.spinnerOperatorLevelRank,
            };

            ArrayAdapter<String> operatorAdapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_item, new String[] {
                    ">", ">=", "<", "<=", "="
            });

            // Specify the layout to use when the list of choices appears
            operatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            for (int i = 0; i < spinnerOperatorIdList.length; i++) {
                final Spinner operatorSpin = (Spinner) view.findViewById(spinnerOperatorIdList[i]);
                operatorSpin.setAdapter(operatorAdapter);
            }

            // main category
            Spinner mainCategorySpinner = (Spinner) view.findViewById(R.id.spinnerMainCategory);
            ArrayAdapter<String> mainCategoryAdapter = new ArrayAdapter<String>(getActivity(),
            		android.R.layout.simple_spinner_item, mainCategoryList);
            mainCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mainCategorySpinner.setAdapter(mainCategoryAdapter);

            // attribute
            Spinner attributeSpinner = (Spinner) view.findViewById(R.id.spinnerAttribute);
            ArrayAdapter<String> attributeAdapter = new ArrayAdapter<String>(getActivity(),
            		android.R.layout.simple_spinner_item, attributeList);
            attributeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            attributeSpinner.setAdapter(attributeAdapter);

            // type
            Spinner typeSpinner = (Spinner) view.findViewById(R.id.spinnerType);
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(getActivity(),
            		android.R.layout.simple_spinner_item, typeList);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeSpinner.setAdapter(typeAdapter);

            // limit
            Spinner limitSpinner = (Spinner) view.findViewById(R.id.spinnerLimit);
            ArrayAdapter<String> limitAdapter = new ArrayAdapter<String>(getActivity(),
            		android.R.layout.simple_spinner_item, limitList);
            limitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            limitSpinner.setAdapter(limitAdapter);

            // sub-categories
            final Spinner subCategorySpinner = (Spinner) view.findViewById(R.id.spinnerSubCategory);
            if (subCategoryMonsterAdapter == null) {
            	subCategoryMonsterAdapter = new ArrayAdapter<String>(getActivity(),
            		android.R.layout.simple_spinner_item, subCategoryMonsterList);
            	subCategoryMonsterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        	}

            if (subCategorySpellAdapter == null) {
            	subCategorySpellAdapter = new ArrayAdapter<String>(getActivity(),
            		android.R.layout.simple_spinner_item, subCategorySpellList);
            	subCategorySpellAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }

            if (subCategoryTrapAdapter == null) {
            	subCategoryTrapAdapter = new ArrayAdapter<String>(getActivity(),
            		android.R.layout.simple_spinner_item, subCategoryTrapList);
            	subCategoryTrapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }
			mainCategorySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					String selected = (String) parent.getItemAtPosition(position);
					if (selected == "(All)") {
						subCategorySpinner.setEnabled(false);
						subCategorySpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
								android.R.layout.simple_spinner_item, new String[] { "(All)" }));
						if (!lastSelectedMainCategory.equals("(All)")) {
							lastSelectedSubCategoryPosition = -1;
						}
						lastSelectedMainCategory = "(All)";
					} else if (selected == "Monster") {
						subCategorySpinner.setEnabled(true);
						subCategorySpinner.setAdapter(subCategoryMonsterAdapter);
						// if we have just come from another main category, allow the subcategory spinner to reset
						// to the first item in the list. Same thing below.
						if (!lastSelectedMainCategory.equals("Monster")) {
							lastSelectedSubCategoryPosition = -1;
						}
						lastSelectedMainCategory = "Monster";
					} else if (selected == "Spell") {
						subCategorySpinner.setEnabled(true);
						subCategorySpinner.setAdapter(subCategorySpellAdapter);
						if (!lastSelectedMainCategory.equals("Spell")) {
							lastSelectedSubCategoryPosition = -1;
						}
						lastSelectedMainCategory = "Spell";
					} else if (selected == "Trap") {
						subCategorySpinner.setEnabled(true);
						subCategorySpinner.setAdapter(subCategoryTrapAdapter);
						if (!lastSelectedMainCategory.equals("Trap")) {
							lastSelectedSubCategoryPosition = -1;
						}
						lastSelectedMainCategory = "Trap";
					}

					// re-select the last selected item in the sub category spinner. This is needed so that
					// the spinner doesn't reset to "(All)" every time we come back to the search panel
					if (lastSelectedSubCategoryPosition != -1) { // -1 is our "don't do it" value
						subCategorySpinner.setSelection(lastSelectedSubCategoryPosition);
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});

			subCategorySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					lastSelectedSubCategoryPosition = position;
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
        }
    }

    /**
     * Fragment for the search result
     */
    public static class SearchResultFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View view = inflater.inflate(R.layout.fragment_search_result, container, false);
            ListView famListView = (ListView) view.findViewById(R.id.resultListView);

            // set the result set of the previous search as the adapter for the list
            famListView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, resultSet));

            // go to a card's detail page when click on its name on the list
            famListView.setOnItemClickListener(new OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                        String cardName = (String)arg0.getItemAtPosition(position);
                        Intent intent = new Intent(v.getContext(), CardDetailActivity.class);
                        intent.putExtra(MainActivity.CARD_NAME, cardName);
                        startActivity(intent);
                }
            });
            return view;
        }
    }
}
