package com.chin.ygodb.activity;

import com.chin.ygodb.YgoSqliteDatabase;
import com.chin.ygodb2.R;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Activity to show the help texts
 */
public class HelpAboutActivity extends BaseFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the help text (default)
        TextView tv = (TextView) findViewById(R.id.textView_help);

        // set the about text if specified in intent
        Intent intent = getIntent();
        if (intent != null) {
            String intentStr = intent.getStringExtra("INTENT");
            if (intentStr.equals("help")) {
                tv.setText(Html.fromHtml(getString(R.string.help_text)));
                setTitle("Help");
            }
            else if (intentStr.equals("about")) {
                String aboutText = "";

                try {
                    String appNameInfo = getString(R.string.about_text);
                    String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    aboutText = appNameInfo +
                            "\nVersion " + version +
                            "\nOffline database version: " + YgoSqliteDatabase.DATABASE_VERSION + "\n\n" +
                            getString(R.string.about_text_part2);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
                tv.setText(aboutText);
                setTitle("About");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // don't show any menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
