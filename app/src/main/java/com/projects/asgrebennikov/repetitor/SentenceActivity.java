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
                    Vector<Word> translations = vocabulary.Translate(item.getWord(), item.getTranslateDirection());
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

        InputStream rus_stream = getResources().openRawResource(
                getResources().getIdentifier("russian_text",
                        "raw", getPackageName()));
        InputStream eng_stream = getResources().openRawResource(
                getResources().getIdentifier("english_text",
                        "raw", getPackageName()));

        try {
            rusTextSupplier_ = new TextSupplierImpl(getFilesDir().getAbsolutePath(), rus_stream, "russian_text");
            rusTextSupplier_.SaveCursor();
            rusTextSupplier_.LoadCursor();
            engTextSupplier_ = new TextSupplierImpl(getFilesDir().getAbsolutePath(), eng_stream, "english_text");
            engTextSupplier_.SaveCursor();
            engTextSupplier_.LoadCursor();

            rusSentence_ = rusTextSupplier_.GetNextSentence();
            engSentence_ = engTextSupplier_.GetNextSentence();
            Sentence currentSentence = rusSentence_;
            currentDirection_ = Vocabulary.TranslateDirection.RU_EN;

            TextView textView = (TextView) findViewById(R.id.sentenceTextView);
            textView.setText(currentSentence.AsString());
            Vector<Word> words = currentSentence.GetWords();
            wordsList_.addAll(ToWordListItems(words, Vocabulary.TranslateDirection.RU_EN));
        } catch (Exception e) {
            finish();
        }

        Button nextSentenceButton = (Button) findViewById(R.id.nextButton);
        nextSentenceButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rusTextSupplier_.SaveCursor();
                engTextSupplier_.SaveCursor();

                Sentence currentSentence = null;

                if (currentDirection_ == Vocabulary.TranslateDirection.RU_EN) {
                    currentDirection_ = Vocabulary.TranslateDirection.EN_RU;
                    currentSentence = engSentence_;
                } else {
                    currentDirection_ = Vocabulary.TranslateDirection.RU_EN;
                    currentSentence = rusSentence_;
                }

                TextView textView = (TextView) findViewById(R.id.sentenceTextView);
                textView.setText(currentSentence.AsString());
                Vector<Word> words = currentSentence.GetWords();
                ArrayAdapter<Word> adapter = (ArrayAdapter<Word>) lv.getAdapter();
                wordsList_.clear();
                wordsList_.addAll(ToWordListItems(words, currentDirection_));
                adapter.notifyDataSetChanged();

                rusSentence_ = rusTextSupplier_.GetNextSentence();
                engSentence_ = engTextSupplier_.GetNextSentence();
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
        textView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                TextSupplier currentSupplier = null;
                Sentence currentSentence = null;

                switch (currentDirection_) {
                    case RU_EN: {
                        currentSupplier = rusTextSupplier_;
                        currentSentence = rusSentence_;
                    }
                    case EN_RU: {
                        currentSupplier = engTextSupplier_;
                        currentSentence = engSentence_;
                    }
                }

                currentSupplier.SaveCursor();
                currentSentence = currentSupplier.GetNextSentence();
                textView.setText(currentSentence.AsString());
                ArrayAdapter<Word> adapter = (ArrayAdapter<Word>) lv.getAdapter();
                wordsList_.clear();
                wordsList_.addAll(ToWordListItems(currentSentence.GetWords(), currentDirection_));
                adapter.notifyDataSetChanged();
            }
        });
    }


    private Vector<WordListItem> ToWordListItems(Vector<Word> words,
                                                 Vocabulary.TranslateDirection translateDirection) {
        Vector<WordListItem> result = new Vector<WordListItem>();

        for (Word word: words) {
            result.add(new WordListItem(word, translateDirection));
        }

        return result;
    }

    private TextSupplier rusTextSupplier_;
    private TextSupplier engTextSupplier_;
    private ArrayList<WordListItem> wordsList_;

    private Sentence rusSentence_;
    private Sentence engSentence_;
    private Vocabulary.TranslateDirection currentDirection_;

} // class SentenceActivity
