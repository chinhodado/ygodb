package com.chin.ygodb;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.chin.ygodb.CardStore;
import com.chin.common.MyTagHandler;
import com.chin.common.Util;
import com.chin.ygodb2.R;
import com.chin.ygodb.activity.CardDetailActivity;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.graphics.Point;
import android.os.AsyncTask;
import android.text.Html;
import android.view.Display;
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
        this.cardName = params[0];

        try { cardStore.getGeneralInfo(this.cardName);     } catch (Exception e) {e.printStackTrace();}
        if (isCancelled()) {return null; }; // attempt to return early
        return null;
    }

    @Override
    protected void onPostExecute(Void params) {
        // all of these should be fast
        try { addCardImage();               } catch (Exception e) {e.printStackTrace();}
        try { addCardEffect();              } catch (Exception e) {e.printStackTrace();}
        try { addCardInfo();                } catch (Exception e) {e.printStackTrace();}
    }

    public void addCardImage() {
        // remove the spinner
        ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.fragmentCardInfo_progressBar1);
        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);
        layout.removeView(pgrBar);

        // calculate the width of the images to be displayed
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int scaleWidth = (int) (screenWidth * 0.8);

        // apply the width and height to the ImageView
        ImageView imgView = (ImageView) activity.findViewById(R.id.imageView_detail_card);
        imgView.getLayoutParams().width = scaleWidth;
        imgView.getLayoutParams().height = (int) (scaleWidth * 1.4576); // 8.6 / 5.9
        imgView.requestLayout();

        // set the image
        String originalLink = cardStore.getImageLink(cardName);
        ImageLoader.getInstance().displayImage(Util.getScaledWikiaImageLink(originalLink, scaleWidth), imgView);
    }

    public void addCardInfo() {
        Document cardDom = cardStore.getCardDom(cardName);
        Elements rows = cardDom.getElementsByClass("cardtable").first().getElementsByClass("cardtablerow");

        // remove the spinner
        ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.fragmentCardInfo_progressBar2);
        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);
        layout.removeView(pgrBar);

        TableLayout infoTable = (TableLayout) activity.findViewById(R.id.infoTable);

        // first row is "Attribute" for monster, "Type" for spell/trap and "Types" for token
        boolean foundFirstRow = false;
        for (Element row : rows) {
            Element header = row.getElementsByClass("cardtablerowheader").first();
            if (header == null) continue;
            String headerText = header.text();
            if (!foundFirstRow && !headerText.equals("Attribute") && !headerText.equals("Type") && !headerText.equals("Types")) {
                continue;
            }
            if (headerText.equals("Other card information") || header.equals("External links")) {
                // we have reached the end for some reasons, exit now
                break;
            }
            else {
                foundFirstRow = true;
                String data = row.getElementsByClass("cardtablerowdata").first().text();
                Util.addRowWithTwoTextView(activity, infoTable, headerText + " ", data, true);
                if (headerText.equals("Card effect types") || headerText.equals("Limitation Text")) {
                    break;
                }
            }
        }

        Util.addBlankRow(activity, infoTable);
    }

    public void addCardEffect() {
        TextView effectTv = (TextView) activity.findViewById(R.id.textViewCardEffect);
        effectTv.setText(Html.fromHtml(cardStore.getCardEffect(cardName), null, new MyTagHandler()));
    }
}