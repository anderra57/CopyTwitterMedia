package com.anderpri.copytwittermedia;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class CopyToClipboard extends Activity {

    String url = "";
    Context context = null;
    boolean error = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            CopyToClipboard.this.moveTaskToBack(true);
            handleSharedText(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVisible(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action)) CopyToClipboard.this.moveTaskToBack(true);
    }

    private void handleSharedText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null && sharedText.contains("twitter.com")) {
            String jsonLink = "https://publish.twitter.com/oembed?dnt=true&omit_script=true&url=https://mobile.twitter.com/i/status/";
            String tweetId = sharedText.split("/status/")[1].split("\\?")[0];

            url = jsonLink + tweetId;

            Content content = new Content();
            content.execute();
        } else {
            error = true;
            quitApp();
        }
    }

    private void quitApp() {
        finishAndRemoveTask();
    }

    @SuppressLint("StaticFieldLeak")
    private class Content extends AsyncTask<Void, Void, Void> {

        private String tweetContent;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        protected Void doInBackground(Void... voids) {
            try {

                String tweetJson = Jsoup.connect(url).ignoreContentType(true).execute().body();
                JSONObject jObject = new JSONObject(tweetJson);
                String html = jObject.getString("html");

                Document parsedHtml = Jsoup.parse(html);
                Elements hrefElems = parsedHtml.getElementsByAttribute("href");

                int hrefElemsSize = hrefElems.size();

                if (hrefElemsSize == 2 || hrefElemsSize == 3)
                    tweetContent = hrefElems.get(hrefElemsSize - 2).text();
                else {
                    cancel(true);
                    error = true;
                    quitApp();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(null, tweetContent);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Enlace multimedia copiado al portapapeles", Toast.LENGTH_SHORT).show();

            finishAndRemoveTask();
        }
    }
}