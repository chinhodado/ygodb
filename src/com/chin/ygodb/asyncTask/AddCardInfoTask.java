package com.chin.ygodb.asyncTask;

import java.util.ArrayList;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.chin.ygodb.CardStore;
import com.chin.ygodb.CardStore.Pair;
import com.chin.common.MyTagHandler;
import com.chin.common.Util;
import com.chin.ygodb2.R;
import com.chin.ygodb.activity.CardDetailActivity;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.graphics.Point;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

/**
 * The async task that populate the information in CardDetailActivity
 * It is put into a separate file since it is too long
 */
public class AddCardInfoTask extends AsyncTask<String, Void, Void> {

    CardDetailActivity activity;
    String cardName;
    CardStore cardStore;

    public AddCardInfoTask(CardDetailActivity activity) {
        this.activity = activity;
        this.cardStore = CardStore.getInstance(activity);
    }

    @Override
    protected Void doInBackground(String... params) {
        cardName = params[0];

        try { cardStore.getCardDomReady(cardName);     } catch (Exception e) {e.printStackTrace();}
        if (isCancelled()) {return null; }; // attempt to return early
        return null;
    }

    @Override
    protected void onPostExecute(Void params) {
        // all of these should be fast
        try { addCardImage();               } catch (Exception e) {e.printStackTrace();}
        try { addCardLore();              } catch (Exception e) {e.printStackTrace();}
        try { addCardInfo();                } catch (Exception e) {e.printStackTrace();}
        try { addCardStatus();              } catch (Exception e) {e.printStackTrace();}
    }

    public void addCardImage() throws Exception {
        // remove the spinner
        ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.fragmentCardInfo_progressBar1);
        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);
        layout.removeView(pgrBar);

        ImageView imgView = (ImageView) activity.findViewById(R.id.imageView_detail_card);
        if (!Util.hasNetworkConnectivity(activity)) {
            TextView tv = new TextView(activity);
            tv.setGravity(Gravity.CENTER);
            tv.setText("(image unavailable in offline mode)");
            Util.replaceView(imgView, tv);
            return;
        }

        // calculate the width of the images to be displayed
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int scaleWidth = (int) (screenWidth * 0.8);

        // apply the width and height to the ImageView
        imgView.getLayoutParams().width = scaleWidth;
        imgView.getLayoutParams().height = (int) (scaleWidth * 1.4576); // 8.6 / 5.9
        imgView.requestLayout();

        // set the image
        String originalLink = cardStore.getImageLink(cardName);
        ImageLoader.getInstance().displayImage(Util.getScaledWikiaImageLink(originalLink, scaleWidth), imgView);
    }

    public void addCardInfo() throws Exception {
        // remove the spinner
        ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.fragmentCardInfo_progressBar2);
        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);
        layout.removeView(pgrBar);

        TableLayout infoTable = (TableLayout) activity.findViewById(R.id.infoTable);
        ArrayList<Pair> infos = cardStore.getCardInfo(cardName);
        for (Pair pair : infos) {
            Util.addRowWithTwoTextView(activity, infoTable, pair.key + "  ", pair.value, true);
        }

        Util.addBlankRow(activity, infoTable);
    }

    public void addCardLore() throws Exception {
        TextView effectTv = (TextView) activity.findViewById(R.id.textViewCardEffect);
        effectTv.setText(Html.fromHtml(cardStore.getCardLore(cardName), null, new MyTagHandler()));
    }

    public void addCardStatus() throws Exception {
        // temporary, for now
        if (!Util.hasNetworkConnectivity(activity)) {
            activity.findViewById(R.id.banlistHeader).setVisibility(View.GONE);
            return;
        }
        TableLayout statusTable = (TableLayout) activity.findViewById(R.id.banlistTable);
        Document cardDom = cardStore.getCardDom(cardName);
        Elements statusRows = cardDom.getElementsByClass("cardtablestatuses").first().getElementsByTag("tr");
        Element statusRow = null;
        for (int i = 0; i < statusRows.size(); i++) {
            if (statusRows.get(i).text().equals("TCG/OCG statuses")) {
                statusRow = statusRows.get(i + 1);
                break;
            }
        }

        if (statusRow == null) {
            Log.i("YGODB", "Card banlist status not found");
            return;
        }

        Elements th = statusRow.getElementsByTag("th");
        Elements td = statusRow.getElementsByTag("td");

        for (int i = 0; i < 3; i++) {
            Util.addRowWithTwoTextView(activity, statusTable, th.get(i).text() + "  ", td.get(i).text(), true);
        }
    }
}