package com.projects.asgrebennikov.repetitor;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

import backend.Sentence;
import backend.TextSupplier;
import backend.TextSupplierImpl;


class MyRunnable implements Runnable {

    private static String yandexKey = "dict.1.1.20171219T092115Z.b4d251fe6793335a.ce9863aa4d1660707da362b3a1e2122fa7db659c";

    public void run() {
        try {
            URL url = new URL("https://dictionary.yandex.net/api/v1/dicservice.json/lookup?" +
                    "key=" + yandexKey + "&" +
                    "lang=ru-en" + "&" +
                    "flags=4" + "&" +
                    "text=перебежавший");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            in.read();

            Scanner s = new Scanner(in).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            result = "";

           /*
            1. Используется словарь если слово словарное (с флагом 4 - формы слова).
            2. Иначе, используется переводчик.
            3. family filter - отсекает матерные слова (не нужен)
             */

        } catch ( Exception e) {
            String what = e.getMessage();
            what = "";
        }
    }

}

public class SentenceActivity extends AppCompatActivity {

    private TextSupplier rusTextSupplier_;
    private ArrayList<String> wordsList_;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence);

        wordsList_ = new ArrayList<String>();

        ListView lv = (ListView) findViewById(R.id.wordsListView);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                wordsList_) {

                int[] colors = {
                         Color.parseColor("#8CBF26"), // lime
                         Color.parseColor("#A200FF"), // purple
                         Color.parseColor("#FF0097"), // magenta
                         Color.parseColor("#A05000"), // brown
                         Color.parseColor("#E671B8"), // pink
                         Color.parseColor("#F09609"), // orange
                         Color.parseColor("#E51400"), // red
                         Color.parseColor("#339933"), // green
                };

                @Override
                public View getView(int position, View convertView, ViewGroup parent){
                    // Get the current item from ListView
                    View view = super.getView(position,convertView,parent);

                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    textView.setTextColor(Color.WHITE);
                    textView.setGravity(Gravity.CENTER);

                    Random rand = new Random();
                    int randomIndex = rand.nextInt(((colors.length - 1) - 0) + 1) + 0;
                    while ((randomIndex % 2) != (position % 2)) {
                        randomIndex = rand.nextInt(((colors.length - 1) - 0) + 1) + 0;
                    }

                    int randomColor = colors[randomIndex];

                    view.setBackgroundColor(randomColor);

                    return view;
                }
            };

        InputStream ins = getResources().openRawResource(
                getResources().getIdentifier("russian_text",
                        "raw", getPackageName()));

        lv.setAdapter(adapter);


        try {
            rusTextSupplier_ = new TextSupplierImpl(getFilesDir().getAbsolutePath(), ins, "russian_text");
            rusTextSupplier_.SaveCursor();
            rusTextSupplier_.LoadCursor();
            TextView textView = (TextView) findViewById(R.id.sentenceTextView);
            Sentence sentence = rusTextSupplier_.GetNextSentence();
            textView.setText(sentence.AsString());
            Vector<String> words = sentence.GetWords();
            wordsList_.addAll(words);
        } catch (Exception e) {
            finish();
        }



        Button nextSentenceButton = (Button) findViewById(R.id.nextButton);
        nextSentenceButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                rusTextSupplier_.SaveCursor();
                TextView textView = (TextView) findViewById(R.id.sentenceTextView);
                Sentence sentence = rusTextSupplier_.GetNextSentence();
                textView.setText(sentence.AsString());
                Vector<String> words = sentence.GetWords();
                ListView lv = (ListView) findViewById(R.id.wordsListView);
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) lv.getAdapter();
                wordsList_.clear();
                wordsList_.addAll(words);
                wordsList_.add("some \n --------------- \n and \n more \n strings");
                adapter.notifyDataSetChanged();
            }
        });

        MyRunnable myRunnable = new MyRunnable();
        Thread t = new Thread(myRunnable);
        t.start();
    }

} // class SentenceActivity
