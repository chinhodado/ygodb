package com.chin.ygodb.asyncTask;

import java.util.List;

import com.chin.common.MyTagHandler;
import com.chin.common.Util;
import com.chin.ygodb.dataSource.CardStore;
import com.chin.ygodb.dataSource.CardStore.Pair;
import com.chin.ygodb.activity.CardDetailActivity;
import com.chin.ygodb.R;
import com.chin.ygowikitool.api.YugipediaApi;
import com.chin.ygowikitool.entity.Card;
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

    private final CardDetailActivity activity;
    private String cardName;
    private final CardStore cardStore;

    public AddCardInfoTask(CardDetailActivity activity) {
        this.activity = activity;
        this.cardStore = CardStore.getInstance(activity);
    }

    @Override
    protected Void doInBackground(String... params) {
        cardName = params[0];

        try {
            String pageid = cardStore.getCardPageId(cardName);
            YugipediaApi api = new YugipediaApi();
            Card card = api.getCard(cardName, pageid);
            cardStore.setCard(cardName, card);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

    private void addCardImage() throws Exception {
        final LinearLayout pgrbarWrapper = (LinearLayout) activity.findViewById(R.id.progressbar_wrapper);
        final LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);

        final ImageView imgView = (ImageView) activity.findViewById(R.id.imageView_detail_card);
        if (!Util.hasNetworkConnectivity(activity)) {
            // remove the spinner
            layout.removeView(pgrbarWrapper);
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
        String shortenedLink = cardStore.getCard(cardName).getImg();
        String originalLink = com.chin.ygowikitool.parser.Util.getFullYugipediaImageLink(shortenedLink);
        // Yugipedia doesn't seem to support on the fly scaled image generation
//        String scaledImageLink = com.chin.ygowikitool.parser.Util.getScaledYugipediaImageLink(originalLink, scaleWidth);
        ImageLoader.getInstance().displayImage(originalLink, imgView, new SimpleImageLoadingListener() {
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

    private void addCardInfo() throws Exception {
        // remove the spinner
        ProgressBar pgrBar = (ProgressBar) activity.findViewById(R.id.fragmentCardInfo_progressBar2);
        LinearLayout layout = (LinearLayout) activity.findViewById(R.id.fragmentCardInfo_mainLinearLayout);
        layout.removeView(pgrBar);

        TableLayout infoTable = (TableLayout) activity.findViewById(R.id.infoTable);
        List<Pair> infos = cardStore.getCardInfo(cardName);
        for (Pair pair : infos) {
            Util.addRowWithTwoTextView(activity, infoTable, pair.key + "     ", pair.value, true);
        }

        Util.addBlankRow(activity, infoTable);
    }

    private void addCardLore() throws Exception {
        TextView effectTv = (TextView) activity.findViewById(R.id.textViewCardEffect);
        effectTv.setText(Html.fromHtml(cardStore.getCardLore(cardName), null, new MyTagHandler()));
    }

    private void addCardStatus() throws Exception {
        TableLayout statusTable = (TableLayout) activity.findViewById(R.id.banlistTable);

        List<Pair> statuses = cardStore.getCardStatus(cardName);
        for (Pair pair : statuses) {
            Util.addRowWithTwoTextView(activity, statusTable, pair.key + "          ", pair.value, true);
        }
    }
}