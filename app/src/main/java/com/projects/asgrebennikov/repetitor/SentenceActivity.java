package com.projects.asgrebennikov.repetitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.net.HttpURLConnection;
import java.net.URL;

public class SentenceActivity extends AppCompatActivity {

    private static String yandexKey = "https://translate.yandex.net/api/v1.5/tr.json/translate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence);

        URL url = new URL("https://translate.yandex.net/api/v1.5/tr.json/translate?" +
                          "key=" + yandexKey);
    }
}
