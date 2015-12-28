package com.chin.ygodb.asyncTask;

import java.util.ArrayList;

import com.chin.common.MyTagHandler;
import com.chin.common.Util;
import com.chin.ygodb.CardStore;
import com.chin.ygodb.CardStore.Pair;
import com.chin.ygodb.activity.CardDetailActivity;
import com.chin.ygodb2.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.text.Html;
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
        try { addCardLore();                } catch (Exception e) {e.printStackTrace();}
        try { addCardInfo();                } catch (Exception e) {e.printStackTrace();}
        try { addCardStatus();              } catch (Exception e) {e.printStackTrace();}
    }

    public void addCardImage() throws Exception {
        final LinearLayout pgrbarWrapper = (LinearLayout) activity.findViewById(R.id.progressbar_wrapper);
        final LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);

        final ImageView imgView = (ImageView) activity.findViewById(R.id.imageView_detail_card);
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
        final int scaleWidth = (int) (screenWidth * 0.8);

        // apply the width and height to the progress bar wrapper. We do this so that the placeholder
        // for the image is expanded initially, with the progress bar at the center. We don't want the
        // card info text to jump down while the user is reading it.
        pgrbarWrapper.getLayoutParams().width = scaleWidth;
        pgrbarWrapper.getLayoutParams().height = (int) (scaleWidth * 1.4576); // 8.6 / 5.9
        pgrbarWrapper.requestLayout();

        // set the image
        String originalLink = cardStore.getImageLinkOnline(cardName);
        ImageLoader.getInstance().displayImage(Util.getScaledWikiaImageLink(originalLink, scaleWidth), imgView, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                // remove the spinner
                layout.removeView(pgrbarWrapper);

                // apply the width and height to the ImageView
                imgView.getLayoutParams().width = scaleWidth;
                imgView.getLayoutParams().height = (int) (scaleWidth * 1.4576); // 8.6 / 5.9
                imgView.requestLayout();
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                // remove the spinner
                layout.removeView(pgrbarWrapper);
            }
        });
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
        TableLayout statusTable = (TableLayout) activity.findViewById(R.id.banlistTable);

        ArrayList<Pair> statuses = cardStore.getCardStatus(cardName);
        for (Pair pair : statuses) {
            Util.addRowWithTwoTextView(activity, statusTable, pair.key + "  ", pair.value, true);
        }
    }
}