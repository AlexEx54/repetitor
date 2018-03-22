package com.projects.asgrebennikov.repetitor;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import backend.Sentence;
import backend.TextSupplier;
import backend.TextSupplierImpl;
import backend.Vocabulary;
import backend.Word;
import backend.YandexVocabularyImpl;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sentence);

        wordsList_ = new ArrayList<WordListItem>();
        ListView lv = (ListView) findViewById(R.id.wordsListView);
        ArrayAdapter<WordListItem> adapter = new WordsArrayAdapter<WordListItem>(this,
                                                                      android.R.layout.simple_list_item_1,
                                                                      wordsList_);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                WordListItem item = (WordListItem) parent.getItemAtPosition(position);
                if (item.getTranslations() != null) {
                    item.setFolded(!item.isFolded());
                    adapter.notifyDataSetChanged();
                    return;
                }

                Flowable.fromCallable(() -> {
                    YandexVocabularyImpl vocabulary = new YandexVocabularyImpl();
                    Vector<Word> translations = vocabulary.Translate(item.getWord(), Vocabulary.TranslateDirection.RU_EN);
                    return translations;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((Vector<Word> words) -> {
                            item.setTranslations(words);
                            item.setFolded(!item.isFolded());
                            adapter.notifyDataSetChanged();
                        }, Throwable::printStackTrace);
            }
        });

        InputStream ins = getResources().openRawResource(
                getResources().getIdentifier("russian_text",
                        "raw", getPackageName()));

        try {
            rusTextSupplier_ = new TextSupplierImpl(getFilesDir().getAbsolutePath(), ins, "russian_text");
            rusTextSupplier_.SaveCursor();
            rusTextSupplier_.LoadCursor();
            TextView textView = (TextView) findViewById(R.id.sentenceTextView);
            Sentence sentence = rusTextSupplier_.GetNextSentence();
            textView.setText(sentence.AsString());
            Vector<Word> words = sentence.GetWords();
            wordsList_.addAll(ToWordListItems(words));
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
                Vector<Word> words = sentence.GetWords();
                ListView lv = (ListView) findViewById(R.id.wordsListView);
                ArrayAdapter<Word> adapter = (ArrayAdapter<Word>) lv.getAdapter();
                wordsList_.clear();
                wordsList_.addAll(ToWordListItems(words));
                adapter.notifyDataSetChanged();
            }
        });

        TextView textView = (TextView) findViewById(R.id.sentenceTextView);
        textView.setGravity(Gravity.CENTER);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Guideline guideLine = (Guideline) findViewById(R.id.guideline2);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                if (params.guidePercent < 0.4f) {
                    params.guidePercent = 0.5f;
                } else {
                    params.guidePercent = 0.2f;
                }
                guideLine.setLayoutParams(params);
            }
            });

        MyRunnable myRunnable = new MyRunnable();
        Thread t = new Thread(myRunnable);
        t.start();
    }


    private Vector<WordListItem> ToWordListItems(Vector<Word> words) {
        Vector<WordListItem> result = new Vector<WordListItem>();

        for (Word word: words) {
            result.add(new WordListItem(word));
        }

        return result;
    }


    private TextSupplier rusTextSupplier_;
    private ArrayList<WordListItem> wordsList_;

} // class SentenceActivity
